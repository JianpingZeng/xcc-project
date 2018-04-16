/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package utils.tablegen;

import backend.codegen.MVT;
import com.sun.javafx.binding.StringFormatter;
import tools.OutParamWrapper;
import tools.Pair;
import tools.Util;
import utils.tablegen.Init.DefInit;
import utils.tablegen.Init.IntInit;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.TreeSet;

import static backend.codegen.MVT.getName;
import static utils.tablegen.ComplexPattern.CPAttr.CPAttrParentAsRoot;
import static utils.tablegen.EEVT.isExtFloatingPointInVTs;
import static utils.tablegen.EEVT.isExtIntegerInVTs;
import static utils.tablegen.SDNP.*;

/**
 * This class takes responsibility of generating matching code for each LLVM td pattern.
 * @author xlous
 */
public class PatternCodeEmitter
{
    private CodeGenDAGPatterns cgp;

    /**
     * Predicates
     */
    private String predicatCheck;
    /**
     * Cost of this pattern selection.
     */
    private int cost;
    /**
     * The pattern matches the input target-independent DAG.
     */
    private TreePatternNode pattern;
    /**
     * The machine instruction matches the given target-independent DAG.
     */
    private TreePatternNode instruction;

    private TreeMap<String, String> variableMap;

    private TreeMap<String, Record> operatorMap;
    private Pair<String, Integer> foldedFlag;
    private ArrayList<Pair<String, Integer>> foldedChains;
    private ArrayList<Pair<String, String>> originChains;
    private TreeSet<String> duplicates;

    private ArrayList<String> lsi;
    enum GeneratedCodeKind
    {
        Normal,
        ExitPredicate,
        Init;
    }
    private ArrayList<Pair<GeneratedCodeKind, String>> generatedCodes;

    private TreeSet<String> generatedDecls;

    private ArrayList<String> targetOpcodes;

    private ArrayList<String> targetVTs;

    private String chainName;
    private int tmpNo;
    private int opcNo;
    private int vtNo;

    private boolean outputIsVariadic;
    private int numInputRootOps;

    public void emitCheck(String code)
    {
        if (code != null && !code.isEmpty())
            generatedCodes.add(Pair.get(GeneratedCodeKind.ExitPredicate, code));
    }

    public void emitCode(String code)
    {
        if (code != null && !code.isEmpty())
        {
            generatedCodes.add(Pair.get(GeneratedCodeKind.Normal, code));
        }
    }

    public void emitInit(String code)
    {
        if (code != null && !code.isEmpty())
            generatedCodes.add(Pair.get(GeneratedCodeKind.Init, code));
    }

    public void emitDecl(String decl)
    {
        assert decl!= null && !decl.isEmpty():"Invalid declaration";
        generatedDecls.add(decl);
    }

    public void emitOpcode(String opc)
    {
        assert opc != null && !opc.isEmpty():"Invalid opcode!";
        targetVTs.add(opc);
        opcNo++;
    }

    public void emitVT(String vt)
    {
        assert vt != null && !vt.isEmpty():"Invalid vt!";
        targetVTs.add(vt);
        vtNo++;
    }

    public PatternCodeEmitter(CodeGenDAGPatterns cgp,
                              String predCheck,
                              TreePatternNode pattern,
                              TreePatternNode inst,
                              ArrayList<Pair<GeneratedCodeKind, String>> genCodes,
                              TreeSet<String> genDecls,
                              ArrayList<String> targetOpcs,
                              ArrayList<String> targetVTs)
    {
        this.cgp = cgp;
        this.predicatCheck = predCheck;
        this.pattern = pattern;
        this.instruction = inst;
        this.generatedCodes = genCodes;
        this.generatedDecls = genDecls;
        this.targetOpcodes = targetOpcs;
        this.targetVTs = targetVTs;
    }

    public boolean isOutputIsVariadic()
    {
        return outputIsVariadic;
    }

    public int getNumInputRootOps()
    {
        return numInputRootOps;
    }

    public boolean emitMatchCode(TreePatternNode node, TreePatternNode parent,
                              String rootName,
                              String chainSuffix,
                                 boolean foundChain)
    {
        if (!node.isLeaf() && node.getName().isEmpty())
        {
            if (nodeHasProperty(node, SDNPMemOperand, cgp))
            {
                lsi.add(rootName);
            }
        }

        boolean isRoot = parent == null;
        if (isRoot)
        {
            numInputRootOps = node.getNumChildren();
            emitCheck(predicatCheck);
        }

        if (node.isLeaf())
        {
            IntInit ii = node.getLeafValue() instanceof IntInit ? (IntInit)node.getLeafValue() : null;
            if (ii != null)
            {
                emitCheck(StringFormatter.format("(ConstantSDNode(%s)).getSExtValue() == %d",
                        rootName, ii.getValue()).getValue());
                return false;
            }
            else if (!nodeIsComplexPattern(node))
            {
                assert false:"Must be complex pattern for leaf value!";
                System.exit(-1);
            }
        }

        if (!node.getName().isEmpty())
        {
            String varMapEntry = null;
            if (!variableMap.containsKey(node.getName()))
            {
                variableMap.put(node.getName(), rootName);
                varMapEntry = rootName;
            }
            else
            {
                emitCheck(varMapEntry + " == " + rootName);
                return false;
            }
        }
        // Emit code to load the child nodes and match their contents recursively.
        int opNo = 0;
        boolean nodeHasChain = nodeHasProperty(node, SDNPHasChain, cgp);
        boolean hasChain = patternHasProperty(node ,SDNPHasChain, cgp);
        boolean emittedUseCheck = false;
        if (hasChain)
        {
            if (nodeHasChain)
                opNo = 1;
            if (!isRoot)
            {
                emitCheck(rootName + ".hasOneUse()");
                emittedUseCheck = true;
                if (nodeHasChain)
                {
                    // If the immediate use can somehow reach this node through another
                    // path, then can't fold it either or it will create a cycle.
                    // e.g. In the following diagram, XX can reach ld through YY. If
                    // ld is folded into XX, then YY is both a predecessor and a successor
                    // of XX.
                    //
                    //         [ld]
                    //         ^  ^
                    //         |  |
                    //        /   \---
                    //      /        [YY]
                    //      |         ^
                    //     [XX]-------|
                    boolean needCheck = parent != pattern;
                    if (!needCheck)
                    {
                        SDNodeInfo info = cgp.getSDNodeInfo(parent.getOperator());
                        needCheck = parent.getOperator() == cgp.getIntrinsicVoidSDNode()
                                || parent.getOperator() == cgp.getIntrinsicWChainSDNode()
                                || parent.getOperator() == cgp.getIntrinsicWOChainSDNode()
                                || info.getNumOperands() > 1
                                || info.hasProperty(SDNPHasChain)
                                || info.hasProperty(SDNPInFlag)
                                || info.hasProperty(SDNPOptInFlag);
                    }
                    if (needCheck)
                    {
                        String parentName = rootName.substring(0, rootName.length()-1);
                        emitCheck(StringFormatter.format("isLegalAndProfitableToFold(%.getNode(), %s.getNode(), n.getNode())",
                                rootName, parentName).getValue());
                    }
                }
            }
            if (nodeHasChain)
            {
                if (foundChain)
                {
                    emitCheck(StringFormatter.format("(%s.getNode() == %s.getNode() || isChainCompatible(%s.getNode(), %s.getNode()))",
                            chainName, rootName, chainName, rootName).getValue());
                    originChains.add(Pair.get(chainName, rootName));
                }
                else
                {
                    foundChain = true;
                }
                chainName = "chain" + chainSuffix;
                emitInit(StringFormatter.format("SDValue %s = %s.getOperand(0)", chainName, rootName).getValue());
            }
        }

        // Don't fold any node which reads or writes a flag and has multiple uses.
        // FIXME: We really need to separate the concepts of flag and "glue". Those
        // real flag results, e.g. X86CMP output, can have multiple uses.
        // FIXME: If the optional incoming flag does not exist. Then it is ok to
        // fold it.
        if (!isRoot && (patternHasProperty(node, SDNPInFlag, cgp) ||
                        patternHasProperty(node, SDNPOptInFlag, cgp) ||
                        patternHasProperty(node, SDNPOutFlag, cgp)))
        {
            if (!emittedUseCheck)
                emitCheck(rootName + ".hasOneUse()");
        }

        for (String pred : node.getPredicateFns())
        {
            emitCheck(pred + "(" +rootName + ".getNode())");
        }

        // If this is an 'and R, 1234' where the operation is AND/OR and the RHS is
        // a constant without a predicate fn that has more that one bit set, handle
        // this as a special case.  This is usually for targets that have special
        // handling of certain large constants (e.g. alpha with it's 8/16/32-bit
        // handling stuff).  Using these instructions is often far more efficient
        // than materializing the constant.  Unfortunately, both the instcombiner
        // and the dag combiner can often infer that bits are dead, and thus drop
        // them from the mask in the dag.  For example, it might turn 'AND X, 255'
        // into 'AND X, 254' if it knows the low bit is set.  Emit code that checks
        // to handle this.
        String opName = node.getOperator().getName();
        if (!node.isLeaf() && (opName.equals("and") || opName.equals("or")) &&
                node.getChild(1).isLeaf() && node.getChild(1).getPredicateFns().isEmpty())
        {
            Init i = node.getChild(1).getLeafValue();
            IntInit ii = i instanceof IntInit ? (IntInit)i : null;
            if(ii != null)
            {
                if (!Util.isPowerOf2(ii.getValue()))
                {
                    emitInit(StringFormatter.format("SDValue %s0 = %s.geOperand(0);",
                            rootName, rootName).getValue());
                    emitInit(StringFormatter.format("SDValue %s1 = %s.geOperand(1);",
                            rootName, rootName).getValue());

                    int nTemp = tmpNo++;
                    emitCheck(StringFormatter.format
                            ("ConstantSDNode tmp%d = %s1 instanceof ConstantSDNode?(ConstantSDNode)%s1:null;", nTemp,
                                    rootName, rootName).getValue());
                    emitCheck("tmp != null");
                    String maskPredicate = opName.equals("or")?"checkOrMask(":"checkAndMask(";
                    emitCheck(maskPredicate + rootName+"0, tmp" + nTemp +
                    ", " + ii.getValue() + ")");
                    return emitChildMatchNode(node.getChild(0), node, rootName+"0", rootName,
                            chainSuffix + "0", foundChain);
                }
            }
        }

        for (int i = 0,  e = node.getNumChildren(); i < e; i++, ++opNo)
        {
            emitInit(StringFormatter.format("SDValue %s%d = %s.getOperand(%d);", rootName, opNo,
                    rootName, opNo).getValue());
            foundChain = emitChildMatchNode(node.getChild(i), node, rootName+opNo,
                    rootName, chainSuffix + opNo, foundChain);
        }

        ComplexPattern cp;
        if (isRoot && node.isLeaf() && (cp = nodeGetComplexPattern(node, cgp)) != null)
        {
            String fn = cp.getSelectFunc();
            int numOps = cp.getNumOperands();
            for (int i = 0; i < numOps; i++)
            {
                emitDecl("cpTmp" + rootName + i);
                emitCode("SDValue cpTmp"+ rootName + i);
            }

            if (cp.hasProperty(SDNPHasChain))
            {
                emitDecl("cpInChain");
                emitDecl("chain" + chainSuffix);
                emitCode("SDValue cpInChian;");
                emitCode("SDValue chain" + chainSuffix + ";");
            }

            StringBuilder code = new StringBuilder(fn + "(" + rootName + ", " + rootName);
            for(int i = 0; i < numOps; i++)
                code.append(", cpTmp").append(rootName).append(i);
            if (cp.hasProperty(SDNPHasChain))
            {
                chainName = "chain" + chainSuffix;
                code.append(", cpInChain, chain").append(chainSuffix);
            }
            emitCode(code.append(")").toString());
        }
        return foundChain;
    }

    private boolean emitChildMatchNode(TreePatternNode child, TreePatternNode parent, String rootName,
                                       String parentRootName,
                                       String chainSuffix,
                                       boolean foundChain)
    {
        if (!child.isLeaf())
        {
            // if this node is not a leaf, just matchers recursively.
            SDNodeInfo info = cgp.getSDNodeInfo(child.getOperator());
            emitCheck(rootName+".getOpcode() == " + info.getEnumName());
            foundChain = emitMatchCode(child, parent, rootName, chainSuffix, foundChain);
            int hasChain = 0;
            if (nodeHasProperty(child, SDNPHasChain, cgp))
            {
                hasChain = 1;
                foldedChains.add(Pair.get(rootName, info.getNumResults()));
            }
            if (nodeHasProperty(child, SDNPOutFlag, cgp))
            {
                assert foldedFlag.first.equals("") && foldedFlag.second == 0;
                foldedFlag = Pair.get(rootName, info.getNumResults() + hasChain);
            }
        }
        else
        {
            if (!child.getName().isEmpty())
            {
                String varMapEntry;
                if (!variableMap.containsKey(child.getName()))
                {
                    varMapEntry = rootName;
                    variableMap.put(child.getName(), rootName);
                }
                else
                {
                    varMapEntry = variableMap.get(child.getName());
                    emitCheck(varMapEntry + ".equlas(" + rootName + ")");
                    duplicates.add(rootName);
                    return foundChain;
                }
            }

            // handle leaves of various tyoe.
            DefInit di = (child.getLeafValue() instanceof DefInit)?(DefInit)child.getLeafValue():null;
            if (di != null)
            {
                Record leafRec = di.getDef();
                if (leafRec.isSubClassOf("RegisterClass") ||
                        leafRec.isSubClassOf("PointerLikeRegClass")) {
                    // nothing to do here.
                }
                else if (leafRec.isSubClassOf("Register")) {
                    // nothing to do here.
                }
                else if (leafRec.isSubClassOf("ComplexPattern"))
                {
                    // handle complex pattern.
                    ComplexPattern cp = nodeGetComplexPattern(child, cgp);
                    String fn = cp.getSelectFunc();
                    int numOps = cp.getNumOperands();
                    for (int i = 0; i < numOps; i++)
                    {
                        emitDecl("cpTmp" + rootName+i);
                        emitCode("SDValue cpTmp" + rootName+i + ";");
                    }

                    if (cp.hasProperty(SDNPHasChain))
                    {
                        SDNodeInfo info = cgp.getSDNodeInfo(parent.getOperator());
                        foldedChains.add(Pair.get("cpInChain", info.getNumResults()));
                        chainName = "chain" + chainSuffix;
                        emitDecl("cpInChain");
                        emitDecl(chainName);
                        emitCode("SDValue cpInChain;");
                        emitCode("SDValue " + chainName + ";");
                    }

                    StringBuilder code = new StringBuilder(fn);
                    code.append("(");
                    if (cp.hasAttribute(CPAttrParentAsRoot))
                    {
                        code.append(parentRootName).append(", ");
                    }
                    else
                    {
                        code.append("n, ");
                    }
                    if (cp.hasProperty(SDNPHasChain))
                    {
                        code.append(rootName.substring(0, rootName.length()-1)).append(", ");
                    }
                    code.append(rootName);
                    for (int i = 0; i < numOps; i++)
                        code.append(", cpTmp").append(rootName).append(i);
                    if (cp.hasProperty(SDNPHasChain))
                        code.append(", cpInChain, chain").append(chainSuffix);
                    emitCheck(code.append(")").toString());
                }
                else if (leafRec.getName().equals("srcvalue"))
                {
                    // place holder for SRCVALEU nodes.
                    // nothing to do here.
                }
                else if (leafRec.getName().equals("ValueType"))
                {
                    emitCheck(StringFormatter.format("((VTSDNode)%s).getVT().getSimpleVT().simpleVT == ",
                            rootName, leafRec.getName()).getValue());
                }
                else if (leafRec.isSubClassOf("condCode"))
                {
                    emitCheck(StringFormatter.format("((CondCodeSDNode)%s).get() == ",
                            rootName, leafRec.getName()).getValue());
                }
                else
                    assert false:"Unkown leaf value!";
                child.getPredicateFns().forEach(pred->
                {
                    emitCheck(pred + "("+rootName+".getNode()");
                });
            }
            else if (child.getLeafValue() instanceof IntInit)
            {
                IntInit ii = (IntInit)child.getLeafValue();
                int nTmp = tmpNo++;
                emitCheck(rootName + " instanceof ConstantSDNode");
                emitCode("ConstantSDNode tmp" + nTmp + " = (ConstantSDNode)" + rootName+";");
                int cTmp = tmpNo++;
                emitCode("long cn"+cTmp + " = tmp"+ nTmp + ".getSExtValue();");
                emitCheck("cn"+cTmp+" == " + ii.getValue());
            }
            else
                assert false:"Unknown leaf value!";
        }
        return foundChain;
    }

    private boolean insertOneTypesCheck(TreePatternNode pat,
                                        TreePatternNode other,
                                        String prefix,
                                        boolean isRoot)
    {
        if (!pat.getExtTypes().equals(other.getExtTypes()))
        {
            pat.setTypes(other.getExtTypes());
            if (!isRoot)
                emitCheck(prefix+".getNode().getValueType(0).getSimpleVT().simpleVT == "
                + getName(pat.getTypeNum(0)));
            return true;
        }
        int opNo = nodeHasProperty(pat, SDNPHasChain, cgp)?1:0;
        for (int i = 0, e = pat.getNumChildren(); i < e; i++, ++opNo)
        {
            if (insertOneTypesCheck(pat.getChild(i), other.getChild(i),
                    prefix+opNo, false))
                return true;
        }
        return false;
    }

    private void emitIntFlagSelectCode(TreePatternNode node,
                                       String rootName,
                                       OutParamWrapper<Boolean> chainEmitted,
                                       OutParamWrapper<Boolean> inFlagDecled,
                                       OutParamWrapper<Boolean> resNodeDecled,
                                       boolean isRoot)
    {
        CodeGenTarget target = cgp.getTarget();
        int opNo = nodeHasProperty(node, SDNPHasChain, cgp)?1:0;
        boolean hasInFlag = nodeHasProperty(node, SDNPInFlag, cgp);
        for (int i = 0, e = node.getNumChildren(); i < e; i++, ++opNo)
        {
            TreePatternNode child = node.getChild(i);
            if (!child.isLeaf())
                emitIntFlagSelectCode(child, rootName+opNo,
                        chainEmitted, inFlagDecled, resNodeDecled, false);
            else
            {
                DefInit di = child.getLeafValue() instanceof DefInit ?(DefInit)child.getLeafValue():null;
                if (di != null)
                {
                    if (!child.getName().isEmpty())
                    {
                        String name = rootName + opNo;
                        if (duplicates.contains(name))
                            continue;
                    }

                    Record rec = di.getDef();
                    if (rec.isSubClassOf("Register"))
                    {
                        int vt = getRegisterValueType(rec, target);
                    }
                }
            }
        }

        if (hasInFlag)
        {
            if (!inFlagDecled.get())
            {
                emitCode("SDValue inFlag = " + rootName + ".getOperand("+opNo+");");
                inFlagDecled.set(true);
            }
            else
            {
                emitCode("inFlag = "+rootName+".getOperand(" + opNo + ");");
            }
        }
    }
    private static ComplexPattern nodeGetComplexPattern(TreePatternNode node, CodeGenDAGPatterns cgp)
    {
        if (node.isLeaf() && (node.getLeafValue() instanceof DefInit)
                && ((DefInit)node.getLeafValue()).getDef().isSubClassOf("ComplexPattern"))
        {
            return cgp.getComplexPattern(((DefInit)node.getLeafValue()).getDef());
        }
        return null;
    }


    static boolean patternHasProperty(TreePatternNode node, int prop, CodeGenDAGPatterns cgp)
    {
        if (nodeHasProperty(node, prop, cgp))
            return true;

        for (int i = 0, e = node.getNumChildren(); i < e; i++)
        {
            TreePatternNode child = node.getChild(i);
            if (patternHasProperty(child, prop, cgp))
                return true;
        }
        return false;
    }

    static boolean nodeIsComplexPattern(TreePatternNode node)
    {
        return (node.isLeaf() && (node.getLeafValue() instanceof DefInit)
                && ((DefInit)node.getLeafValue()).getDef().isSubClassOf("ComplexPattern"));
    }

    static boolean nodeHasProperty(TreePatternNode node, int prop, CodeGenDAGPatterns cgp)
    {
        if (node.isLeaf())
        {
            ComplexPattern cp = nodeGetComplexPattern(node, cgp);
            if (cp != null)
                return cp.hasProperty(prop);
            return false;
        }
        Record rec = node.getOperator();
        if (!rec.isSubClassOf("SDNode")) return false;
        return cgp.getSDNodeInfo(rec).hasProperty(prop);
    }

    static String getOpcodeName(Record opc, CodeGenDAGPatterns cgp)
    {
        return cgp.getSDNodeInfo(opc).getEnumName();
    }

    static void removeAllTypes(TreePatternNode node)
    {
        node.removeTypes();
        if (!node.isLeaf())
        {
            for (int i = 0, e = node.getNumChildren(); i < e; i++)
                removeAllTypes(node.getChild(i));
        }
    }

    static int getRegisterValueType(Record r, CodeGenTarget target)
    {
        boolean foundRC = false;
        int vt = MVT.Other;
        ArrayList<CodeGenRegisterClass> rcs = target.getRegisterClasses();
        for (CodeGenRegisterClass regClass : rcs)
        {
            if (regClass.elts.contains(r))
            {
                if (!foundRC)
                {
                    foundRC = true;
                    vt = regClass.getValueTypeAt(0);
                }
                else
                {
                    // Multiple RCs
                    if (vt != regClass.getValueTypeAt(0))
                        return MVT.Other;
                }
            }
        }
        return vt;
    }

    static int getPatternSize(TreePatternNode pat, CodeGenDAGPatterns cgp)
    {
        int extVT = pat.getExtTypeNum(0);
        assert isExtIntegerInVTs(pat.getExtTypes()) ||
                isExtFloatingPointInVTs(pat.getExtTypes())||
                extVT == MVT.isVoid ||
                extVT == MVT.Flag ||
                extVT == MVT.iPTR ||
                extVT == MVT.iPTRAny:"Not a valid pattern node to size";
        int size = 3;
        if (pat.isLeaf() && pat.getLeafValue() instanceof IntInit)
            size += 2;

        ComplexPattern cp = nodeGetComplexPattern(pat, cgp);
        if (cp != null)
            size += cp.getNumOperands() * 3;

        if (!pat.getPredicateFns().isEmpty())
            ++size;

        for (int i = 0, e = pat.getNumChildren(); i < e; i++)
        {
            TreePatternNode child = pat.getChild(i);
            if (!child.isLeaf() && child.getExtTypeNum(0) != MVT.Other)
                size += getPatternSize(child, cgp);
            else if (child.isLeaf())
            {
                if (child.getLeafValue() instanceof IntInit)
                    size += 5;
                else if (nodeIsComplexPattern(child))
                    size += getPatternSize(child, cgp);
                else if (!child.getPredicateFns().isEmpty())
                    size++;
            }
        }
        return size;
    }

    static int getResultPatternCost(TreePatternNode inst, CodeGenDAGPatterns cgp) throws Exception
    {
        if (inst.isLeaf()) return 0;

        int cost = 0;
        Record opc = inst.getOperator();
        if (opc.isSubClassOf("Instruction"))
        {
            ++cost;
            CodeGenInstruction cgInst = cgp.getTarget().getInstruction(opc.getName());
            if (cgInst.usesCustomDAGSchedInserter)
                cost += 10;
        }
        for (int i = 0, e = inst.getNumChildren(); i < e; i++)
            cost += getResultPatternCost(inst.getChild(i), cgp);
        return cost;
    }

    static int getResultPatternSize(TreePatternNode node, CodeGenDAGPatterns cgp) throws Exception
    {
        if (node.isLeaf()) return 0;

        int size = 0;
        Record opc = node.getOperator();
        if (opc.isSubClassOf("Instruction"))
            size += opc.getValueAsInt("CodeSize");
        for (int i = 0, e = node.getNumChildren(); i < e; i++)
            size += getResultPatternSize(node.getChild(i), cgp);
        return size;
    }

    static class PatternSortingPredicate implements Comparator<Pair<PatternToMatch, ArrayList<Pair<GeneratedCodeKind, String>>>>
    {
        private CodeGenDAGPatterns cgp;

        public PatternSortingPredicate(CodeGenDAGPatterns cgp)
        {
            this.cgp = cgp;
        }

        @Override
        public int compare(Pair<PatternToMatch, ArrayList<Pair<GeneratedCodeKind, String>>> o1, Pair<PatternToMatch, ArrayList<Pair<GeneratedCodeKind, String>>> o2)
        {
            try
            {
                PatternToMatch lhs = o1.first, rhs = o2.first;
                int lhsSize = getPatternSize(lhs.getSrcPattern(), cgp);
                int rhsSize = getPatternSize(rhs.getSrcPattern(), cgp);
                lhsSize += lhs.getAddedComplexity();
                rhsSize += rhs.getAddedComplexity();
                if (lhsSize > rhsSize) return 1;
                if (lhsSize < rhsSize) return -1;

                int lhsCost = getResultPatternCost(lhs.getDstPattern(), cgp);
                int rhsCost = getResultPatternCost(rhs.getDstPattern(), cgp);
                if (lhsCost > rhsCost) return 1;
                if (lhsCost < rhsCost) return -1;

                return getResultPatternSize(lhs.getDstPattern(), cgp) -
                        getResultPatternSize(rhs.getDstPattern(), cgp);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return 0;
        }
    }

    public ArrayList<String> emitResultCode(TreePatternNode *node, ArrayList<Record> destRegs,
                                           boolean inFlagDecled, boolean resNodeDecled,
                                           boolean likeLeaf, boolean isRoot)
    {
        ArrayList<String> nodeOps = new ArrayList<>();
        if (node.getName() != null && !node.getName().isEmpty()
        {
            String varName = node.getName();
            boolean modifiedVal = false;
            if (!variableMap.containsKey(varName))
            {
                System.err.printf("Variable '%s' referenced but not defined and not caught earlier!%n", 
                    varName);
                System.exit(-1);
            }
            String val = variableMap.get(varName);
            if (var.equals("Tmp"))
            {
                nodeOps.add("tmp");
                return nodeOps;
            }

            ComplexPattern cp;
            int resNo = tmpNo++;
            if (!node.isLeaf() && node.getOperator().getName().equals("imm"))
            {
                assert node.getExtTypes().size() == 1 :"Multiple types not handled!";
                String castType, tmpVar = "tmp" + resNo;
                switch(node.getTypeNum(0))
                {
                    default:
                      System.err.printf("Can't handle %s type as an immediate constant, Aborting!%n", 
                          getEnumName(node.getTypeNum(0)));
                      System.exit(-1);
                      break;
                    case MVT.i1:
                      castType = "boolean"; break;
                    case MVT.i8:
                      castType = "byte"; break;
                    case MVT.i16:
                      castType ="short"; break;
                    case MVT.i32:
                      castType = "int"; break;
                    case MVT.i64:
                      castType = "long"; break;
                }
                String temp = castType.equals("boolean") ? "((ConstantSDNode)"+val+").getZExtValue() != 0":
                                                           "((" + castType +")"+"((ConstantSDNode)"+val+").getZExtValue())";
                emitCode(StringFormatter.format("SDValue %s = curDAG.getTargetConstant(%s, %s);", temp, 
                      getEnumName(node.getTypeNum(0))).getValue());
                val = tmpVar;
                modifiedVal = true;
                nodeOps.add(val);
            }
            else if (!node.isLeaf() && node.getOperator().getName().equals("fpimm"))
            {
                assert node.getExtTypes().size() == 1:"Multiple types not be handled yet!";
                String tmpVar = "tmp" + resNo;
                emtiCode(StringFormatter.format("SDValue %s = curDAG.getTargetConstantFP(((ConstantFPSDNode)%s).getConstantFPValue(), ((ConstantFPSDNode)%s).getValueType(0));", tmpVar, val, val).getValue());
                val = tmpVar;
                modifiedVal = true;
                nodeOps.add(val);
            }
            else if (!node.isLeaf() && node.getOperator().getName().equals("texternalsym"))
            {
                Record op;
                if ((op = operatorMap.get(node.getName()) != null && op.getName().equals("extenralsym"))
                {
                    String tmpVar = "tmp" + resNo;
                    emitCode(StringFormatter.format("SDValue %s = curDAG.getTargetExternalSymbol(((ExternalSymbolSDNode)%s).getSymbol(), %s);", tmpVar, val, getEnumName(node.getTypeNum(0)).getValue());
                    val = tmpVar;
                    modifiedVal = true;
                }
                nodeOps.add(val);
            }
            else if (!node.isLeaf() && (node.getOperator().getName().equals("tglobaladdr")|| 
                node.getOperator().getName().equals("tglobaltlsaddr")))
            {
                Record op;
                if ((op = operatorMap.get(node.getName()) != null && (op.getName().equals("globaladdr"))
                      || op.getName().equals("globaltlsaddr"))
                {
                    String tmpVar = "tmp" + resNo;
                    emitCode(StringFormatter.format("SDValue %s = curDAG.getTargetGlobalAddress(((GlobalAddressSDNode)%s).getGlobal(), %s);", tmpVar, val, getEnumName(node.getTypeNum(0)).getValue());
                    val = tmpVar;
                    modifiedVal = true;
                }
                nodeOps.add(val);
            }
            else if (!node.isLeaf() && (node.getOperator().getName().equals("texternalsym") || 
                    || op.getName().equals("tconstpool")))
            {
                nodeOps.add(val);
            }
            else if (node.isLeaf() && (cp = nodeGetComplexPattern(node, cgp)) != null)
            {
                for (int i = 0, e = cp.getNumOperands(); i < e; i++)
                {
                    nodeOps.add("cpTmp" + val + i);
                }
            }
            else
            {
                if (!likeLeaf)
                {
                      if (isRoot && node.isLeaf())
                      {
                          emitCode("replaceUses(node, " + val + ");");
                          emitCode("return null;");
                      }
                }
                nodeOps.add(val);
            }

            if (modifiedVal)
              variableMap.put(varName, val);
            return nodeOps;
        }
        
        if (node.isLeaf())
        {
            DefInit di = node.getLeafValue() instanceof DefInit ? (DefInit)node.getLeafValue() : null;
            if (di != null)
            {
                int resNo = tmpNo++;
                if (di.getDef().isSubClassOf("Regiser"))
                {
                    emitCode("SDValue tmp" + resNo + " = curDAG.getRegister("+getQualifiedName(di.getDef())
                        + ", " + getEnumName(node.getTypeNum(0)+");"));
                    nodeOps.add("tmp" + resNo);
                    return nodeOps;
                }
                else if (di.getDef().getName().equals("zero_reg"))
                {
                    emitCode(StringFormatter.format("SDValue tmp%d = curDAG.getRegister(0, %s);", resNo, 
                          getEnumName(node.getTypeNum(0))).getValue());
                    nodeOps.add("tmp"+resNo);
                    return nodeOps;
                }
                else if (di.getDef().isSubClassOf("RegisterClass"))
                {
                    emitCode(StringFormatter.format("SDValue tmp%d = curDAG.getTargetConstant(%sRegisterID, MVT.i32);", resNo, getQualifiedName(di.getDef())).getValue());
                    nodeOps.add("tmp"+resNo);
                    return nodeOps;
                }
            }
            else if (node.getLeafValue() instanceof IntInit)
            {
                IntInit ii = (IntInit)node.getLeafValue();
                int resNo = tmpNo++;
                assert node.getExtTypes().size() == 1:"Multiple types are not handled yet!";
                emitCode(StringFormatter.format("SDValue tmp%d = curDAG.getTargetConstant(%dL, %s);",
                      ii.getValue(), getEnumName(node.getTypeNum(0))).getValue());
                nodeOps.add("tmp"+resNo);
                return nodeOps;
            }

            assert false :"Unknown leaf value!";
            return nodeOps;
        }
    }
}
