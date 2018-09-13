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
import tools.OutRef;
import tools.Pair;
import tools.Util;
import utils.tablegen.Init.DefInit;
import utils.tablegen.Init.IntInit;

import java.util.*;

import static backend.codegen.MVT.getEnumName;
import static backend.codegen.MVT.getName;
import static utils.tablegen.CodeGenHwModes.DefaultMode;
import static utils.tablegen.ComplexPattern.CPAttr.CPAttrParentAsRoot;
import static utils.tablegen.SDNP.*;

/**
 * This class takes responsibility of generating matching code for each LLVM td pattern.
 *
 * @author xlous
 */
public class PatternCodeEmitter {
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

  private TreeMap<String, String> variableMap = new TreeMap<>();

  private TreeMap<String, Record> operatorMap = new TreeMap<>();
  private Pair<String, Integer> foldedFlag = new Pair<>("", 0);
  private ArrayList<Pair<String, Integer>> foldedChains = new ArrayList<>();
  private ArrayList<Pair<String, String>> originChains = new ArrayList<>();
  private TreeSet<String> duplicates = new TreeSet<>();

  private ArrayList<String> lsi = new ArrayList<>();

  enum GeneratedCodeKind {
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

  public void emitCheck(String code) {
    if (code != null && !code.isEmpty())
      generatedCodes.add(Pair.get(GeneratedCodeKind.ExitPredicate, code));
  }

  public void emitCode(String code) {
    if (code != null && !code.isEmpty()) {
      generatedCodes.add(Pair.get(GeneratedCodeKind.Normal, code));
    }
  }

  public void emitInit(String code) {
    if (code != null && !code.isEmpty())
      generatedCodes.add(Pair.get(GeneratedCodeKind.Init, code));
  }

  public void emitDecl(String decl) {
    Util.assertion(decl != null && !decl.isEmpty(), "Invalid declaration");
    generatedDecls.add(decl);
  }

  public void emitOpcode(String opc) {
    Util.assertion(opc != null && !opc.isEmpty(), "Invalid opcode!");
    targetOpcodes.add(opc);
    opcNo++;
  }

  public void emitVT(String vt) {
    Util.assertion(vt != null && !vt.isEmpty(), "Invalid vt!");
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
                            ArrayList<String> targetVTs) {
    this.cgp = cgp;
    this.predicatCheck = predCheck;
    this.pattern = pattern;
    this.instruction = inst;
    this.generatedCodes = genCodes;
    this.generatedDecls = genDecls;
    this.targetOpcodes = targetOpcs;
    this.targetVTs = targetVTs;
  }

  public boolean isOutputIsVariadic() {
    return outputIsVariadic;
  }

  public int getNumInputRootOps() {
    return numInputRootOps;
  }

  private static boolean disableComplexPatternForNotOpt(TreePatternNode node, CodeGenDAGPatterns cgp) {
    boolean isStore = !node.isLeaf() && getOpcodeName(node.getOperator(), cgp).equals("ISD.STORE");
    if (!isStore && nodeHasProperty(node, SDNPHasChain, cgp))
      return false;

    for (int i = 0, e = node.getNumChildren(); i < e; i++) {
      if (patternHasProperty(node.getChild(i), SDNPHasChain, cgp))
        return true;
    }
    return false;
  }

  public boolean emitMatchCode(TreePatternNode node,
                               TreePatternNode parent,
                               String rootName,
                               String chainSuffix,
                               boolean foundChain) {
    if (!node.isLeaf() && node.getName().isEmpty()) {
      if (nodeHasProperty(node, SDNPMemOperand, cgp)) {
        lsi.add(rootName);
      }
    }

    boolean isRoot = parent == null;
    if (isRoot) {
      numInputRootOps = node.getNumChildren();
      if (disableComplexPatternForNotOpt(node, cgp))
        emitCheck("optLevel != CodeGenOpt.None");

      emitCheck(predicatCheck);
    }

    if (node.isLeaf()) {
      IntInit ii = node.getLeafValue() instanceof IntInit ? (IntInit) node.getLeafValue() : null;
      if (ii != null) {
        emitCheck(String.format("((ConstantSDNode)%s.getNode()).getSExtValue() == %dL",
            rootName, ii.getValue()));
        return false;
      } else if (!nodeIsComplexPattern(node)) {
        Util.assertion(false, "Must be complex pattern for leaf value!");
        System.exit(-1);
      }
    }

    if (!node.getName().isEmpty()) {
      String varMapEntry = null;
      if (!variableMap.containsKey(node.getName())) {
        variableMap.put(node.getName(), rootName);
        varMapEntry = rootName;
      } else {
        varMapEntry = variableMap.get(node.getName());
        emitCheck(varMapEntry + " == " + rootName);
        return false;
      }
    }
    // Emit code to load the child nodes and match their contents recursively.
    int opNo = 0;
    boolean nodeHasChain = nodeHasProperty(node, SDNPHasChain, cgp);
    boolean hasChain = patternHasProperty(node, SDNPHasChain, cgp);
    boolean emittedUseCheck = false;
    if (hasChain) {
      if (nodeHasChain)
        opNo = 1;
      if (!isRoot) {
        emitCheck(rootName + ".hasOneUse()");
        emittedUseCheck = true;
        if (nodeHasChain) {
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
          if (!needCheck) {
            SDNodeInfo info = cgp.getSDNodeInfo(parent.getOperator());
            needCheck = parent.getOperator() == cgp.getIntrinsicVoidSDNode()
                || parent.getOperator() == cgp.getIntrinsicWChainSDNode()
                || parent.getOperator() == cgp.getIntrinsicWOChainSDNode()
                || info.getNumOperands() > 1
                || info.hasProperty(SDNPHasChain)
                || info.hasProperty(SDNPInFlag)
                || info.hasProperty(SDNPOptInFlag);
          }
          if (needCheck) {
            String parentName = rootName.substring(0, rootName.length() - 1);
            emitCheck(String.format("isLegalAndProfitableToFold(%s.getNode(), %s.getNode(), n.getNode())",
                rootName, parentName));
          }
        }
      }
      if (nodeHasChain) {
        if (foundChain) {
          emitCheck(String.format("(%s.getNode() == %s.getNode() || isChainCompatible(%s.getNode(), %s.getNode()))",
              chainName, rootName, chainName, rootName));
          originChains.add(Pair.get(chainName, rootName));
        } else {
          foundChain = true;
        }
        chainName = "chain" + chainSuffix;
        emitInit(String.format("SDValue %s = %s.getOperand(0);", chainName, rootName));
      }
    }

    // Don't fold any node which reads or writes a flag and has multiple uses.
    // FIXME: We really need to separate the concepts of flag and "glue". Those
    // real flag results, e.g. X86CMP output, can have multiple uses.
    // FIXME: If the optional incoming flag does not exist. Then it is ok to
    // fold it.
    if (!isRoot && (patternHasProperty(node, SDNPInFlag, cgp) ||
        patternHasProperty(node, SDNPOptInFlag, cgp) ||
        patternHasProperty(node, SDNPOutFlag, cgp))) {
      if (!emittedUseCheck)
        emitCheck(rootName + ".hasOneUse()");
    }

    for (String pred : node.getPredicateFns()) {
      emitCheck(pred + "(" + rootName + ".getNode())");
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
    String opName;
    if (!node.isLeaf() && (opName = node.getOperator().getName()) != null &&
        (opName.equals("and") || opName.equals("or")) &&
        node.getChild(1).isLeaf() && node.getChild(1).getPredicateFns().isEmpty()) {
      Init i = node.getChild(1).getLeafValue();
      IntInit ii = i instanceof IntInit ? (IntInit) i : null;
      if (ii != null) {
        if (!Util.isPowerOf2(ii.getValue())) {
          emitInit(String.format("SDValue %s0 = %s.getOperand(0);",
              rootName, rootName));
          emitInit(String.format("SDValue %s1 = %s.getOperand(1);",
              rootName, rootName));

          int nTemp = tmpNo++;
          emitCode(String.format
              ("ConstantSDNode tmp%d = %s1.getNode() instanceof ConstantSDNode?(ConstantSDNode)%s1.getNode():null;", nTemp,
                  rootName, rootName));
          emitCheck("tmp" + nTemp + " != null");
          String maskPredicate = opName.equals("or") ? "checkOrMask(" : "checkAndMask(";
          emitCheck(maskPredicate + rootName + "0, tmp" + nTemp +
              ", " + ii + "L)");
          return emitChildMatchNode(node.getChild(0), node, rootName + "0", rootName,
              chainSuffix + "0", foundChain);
        }
      }
    }

    for (int i = 0, e = node.getNumChildren(); i < e; i++, ++opNo) {
      emitInit(String.format("SDValue %s%d = %s.getOperand(%d);", rootName, opNo,
          rootName, opNo));
      foundChain = emitChildMatchNode(node.getChild(i), node, rootName + opNo,
          rootName, chainSuffix + opNo, foundChain);
    }

    ComplexPattern cp;
    if (isRoot && node.isLeaf() && (cp = nodeGetComplexPattern(node, cgp)) != null) {
      String fn = cp.getSelectFunc();
      int numOps = cp.getNumOperands();

      if (cp.hasProperty(SDNPHasChain)) {
        numOps += 2;
      }

      for (int i = 0; i < numOps; i++) {
        emitDecl("cpTmp" + rootName + i);
      }

      emitCode(String.format("// The last two elements for "
          + "cpTmp.%nSDValue[] cpTmp%s = new SDValue[%d];", rootName, numOps));

      StringBuilder code = new StringBuilder(fn + "(" + rootName + ", " + rootName);
      code.append(", cpTmp").append(rootName);
      emitCheck(code.append(")").toString());
    }
    return foundChain;
  }

  private boolean emitChildMatchNode(TreePatternNode child, TreePatternNode parent, String rootName,
                                     String parentRootName,
                                     String chainSuffix,
                                     boolean foundChain) {
    if (!child.isLeaf()) {
      // if this node is not a leaf, just matchers recursively.
      SDNodeInfo info = cgp.getSDNodeInfo(child.getOperator());
      emitCheck(rootName + ".getOpcode() == " + info.getEnumName());
      foundChain = emitMatchCode(child, parent, rootName, chainSuffix, foundChain);
      int hasChain = 0;
      if (nodeHasProperty(child, SDNPHasChain, cgp)) {
        hasChain = 1;
        foldedChains.add(Pair.get(rootName, info.getNumResults()));
      }
      if (nodeHasProperty(child, SDNPOutFlag, cgp)) {
        Util.assertion((foldedFlag.first == null || foldedFlag.first.isEmpty()) && foldedFlag.second == 0);

        foldedFlag = Pair.get(rootName, info.getNumResults() + hasChain);
      }
    } else {
      if (!child.getName().isEmpty()) {
        String varMapEntry;
        if (!variableMap.containsKey(child.getName())) {
          varMapEntry = rootName;
          variableMap.put(child.getName(), rootName);
        } else {
          varMapEntry = variableMap.get(child.getName());
          emitCheck(varMapEntry + ".equals(" + rootName + ")");
          duplicates.add(rootName);
          return foundChain;
        }
      }

      // handle leaves of various tyoe.
      DefInit di = (child.getLeafValue() instanceof DefInit) ? (DefInit) child.getLeafValue() : null;
      if (di != null) {
        Record leafRec = di.getDef();
        if (leafRec.isSubClassOf("RegisterClass") ||
            leafRec.isSubClassOf("PointerLikeRegClass")) {
          // nothing to do here.
        } else if (leafRec.isSubClassOf("Register")) {
          // nothing to do here.
        } else if (leafRec.isSubClassOf("ComplexPattern")) {
          // handle complex pattern.
          ComplexPattern cp = nodeGetComplexPattern(child, cgp);
          Util.assertion(cp != null);
          String fn = cp.getSelectFunc();

          int numOps = cp.getNumOperands();
          if (cp.hasProperty(SDNPHasChain))
            numOps += 2;

          for (int i = 0; i < numOps; i++) {
            emitDecl("cpTmp" + rootName + i);
          }

          emitCode(String.format("SDValue[] cpTmp%s = new SDValue[%d];", rootName, numOps));

          StringBuilder code = new StringBuilder(fn);
          code.append("(");
          if (cp.hasAttribute(CPAttrParentAsRoot)) {
            code.append(parentRootName).append(", ");
          } else {
            code.append("n, ");
          }
          if (cp.hasProperty(SDNPHasChain)) {
            code.append(rootName.substring(0, rootName.length() - 1)).append(", ");
          }

          code.append(rootName);
          code.append(", cpTmp").append(rootName);
          emitCheck(code.append(")").toString());
        } else if (leafRec.getName().equals("srcvalue")) {
          // place holder for SRCVALEU nodes.
          // nothing to do here.
        } else if (leafRec.isSubClassOf("ValueType")) {
          emitCheck(String.format("((VTSDNode)%s.getNode()).getVT().getSimpleVT().simpleVT == MVT.%s",
              rootName, leafRec.getName()));
        } else if (leafRec.isSubClassOf("CondCode")) {
          emitCheck(String.format("((CondCodeSDNode)%s).getCondition() == dagisel.CondCode.%s",
              rootName, leafRec.getName()));
        } else
          Util.assertion(false, "Unknown leaf value!");
        child.getPredicateFns().forEach(pred ->
        {
          emitCheck(pred + "(" + rootName + ".getNode())");
        });
      } else if (child.getLeafValue() instanceof IntInit) {
        IntInit ii = (IntInit) child.getLeafValue();
        int nTmp = tmpNo++;
        emitCheck(rootName + ".getNode() instanceof ConstantSDNode");
        emitCode("ConstantSDNode tmp" + nTmp + " = (ConstantSDNode)" + rootName + ".getNode();");
        int cTmp = tmpNo++;
        emitCode("long cn" + cTmp + " = tmp" + nTmp + ".getSExtValue();");
        emitCheck("cn" + cTmp + " == " + ii + "L");
      } else
        Util.assertion(false, "Unknown leaf value!");
    }
    return foundChain;
  }

  private boolean insertOneTypesCheck(TreePatternNode pat,
                                      TreePatternNode other,
                                      String prefix,
                                      boolean isRoot) {
    if (!pat.getExtTypes().equals(other.getExtTypes())) {
      pat.setTypes(other.getExtTypes());
      if (!isRoot)
        emitCheck(prefix + ".getNode().getValueType(0).getSimpleVT().simpleVT == " +
            pat.getExtType(0).toString());
      return true;
    }
    int opNo = nodeHasProperty(pat, SDNPHasChain, cgp) ? 1 : 0;
    for (int i = 0, e = pat.getNumChildren(); i < e; i++, ++opNo) {
      if (insertOneTypesCheck(pat.getChild(i), other.getChild(i),
          prefix + opNo, false))
        return true;
    }
    return false;
  }

  private void emitIntFlagSelectCode(TreePatternNode node,
                                     String rootName,
                                     OutRef<Boolean> chainEmitted,
                                     OutRef<Boolean> inFlagDecled,
                                     OutRef<Boolean> resNodeDecled,
                                     boolean isRoot) {
    CodeGenTarget target = cgp.getTarget();
    int opNo = nodeHasProperty(node, SDNPHasChain, cgp) ? 1 : 0;
    boolean hasInFlag = nodeHasProperty(node, SDNPInFlag, cgp);
    for (int i = 0, e = node.getNumChildren(); i < e; i++, ++opNo) {
      TreePatternNode child = node.getChild(i);
      if (!child.isLeaf())
        emitIntFlagSelectCode(child, rootName + opNo,
            chainEmitted, inFlagDecled, resNodeDecled, false);
      else {
        DefInit di = child.getLeafValue() instanceof DefInit ? (DefInit) child.getLeafValue() : null;
        if (di != null) {
          if (!child.getName().isEmpty()) {
            String name = rootName + opNo;
            if (duplicates.contains(name))
              continue;
          }

          Record rec = di.getDef();
          if (rec.isSubClassOf("Register")) {
            int vt = getRegisterValueType(rec, target).getSimple().simpleVT;
            int rvt = getRegisterValueType(rec, target).getSimple().simpleVT;
            if (rvt == MVT.Flag) {
              if (!inFlagDecled.get()) {
                emitCode("SDValue inFlag = " + rootName + opNo + ";");
                inFlagDecled.set(true);
              } else
                emitCode("inFlag = " + rootName + opNo + ";");
            } else {
              if (!chainEmitted.get()) {
                emitCode("SDValue chain = curDAG.getEntryNode();");
                chainName = "chain";
                chainEmitted.set(true);
              }
              if (!inFlagDecled.get()) {
                emitCode("SDValue inFlag = new SDValue();");
                inFlagDecled.set(true);
              }
              String decl = !resNodeDecled.get() ? "SDNode " : "";
              emitCode(String.format("%sresNode = curDAG.getCopyToReg(%s, X86GenRegisterNames.%s, %s%d, inFlag).getNode();",
                  decl, chainName, rec.getName(), rootName,
                  opNo));
              resNodeDecled.set(true);
              emitCode(chainName + " = new SDValue(resNode, 0);");
              emitCode("inFlag = new SDValue(resNode, 1);");
            }
          }
        }
      }
    }

    if (hasInFlag) {
      if (!inFlagDecled.get()) {
        emitCode("SDValue inFlag = " + rootName + ".getOperand(" + opNo + ");");
        inFlagDecled.set(true);
      } else {
        emitCode("inFlag = " + rootName + ".getOperand(" + opNo + ");");
      }
    }
  }

  protected static ComplexPattern nodeGetComplexPattern(TreePatternNode node,
                                                        CodeGenDAGPatterns cgp) {
    if (node.isLeaf() && (node.getLeafValue() instanceof DefInit)
        && ((DefInit) node.getLeafValue()).getDef().isSubClassOf("ComplexPattern")) {
      return cgp.getComplexPattern(((DefInit) node.getLeafValue()).getDef());
    }
    return null;
  }


  static boolean patternHasProperty(TreePatternNode node, int prop, CodeGenDAGPatterns cgp) {
    if (nodeHasProperty(node, prop, cgp))
      return true;

    for (int i = 0, e = node.getNumChildren(); i < e; i++) {
      TreePatternNode child = node.getChild(i);
      if (patternHasProperty(child, prop, cgp))
        return true;
    }
    return false;
  }

  static boolean nodeIsComplexPattern(TreePatternNode node) {
    return (node.isLeaf() && (node.getLeafValue() instanceof DefInit)
        && ((DefInit) node.getLeafValue()).getDef().isSubClassOf("ComplexPattern"));
  }

  static boolean nodeHasProperty(TreePatternNode node, int prop, CodeGenDAGPatterns cgp) {
    if (node.isLeaf()) {
      ComplexPattern cp = nodeGetComplexPattern(node, cgp);
      if (cp != null)
        return cp.hasProperty(prop);
      return false;
    }
    Record rec = node.getOperator();
    if (!rec.isSubClassOf("SDNode")) return false;
    return cgp.getSDNodeInfo(rec).hasProperty(prop);
  }

  static String getOpcodeName(Record opc, CodeGenDAGPatterns cgp) {
    return cgp.getSDNodeInfo(opc).getEnumName();
  }

  static void removeAllTypes(TreePatternNode node) {
    node.removeTypes();
    if (!node.isLeaf()) {
      for (int i = 0, e = node.getNumChildren(); i < e; i++)
        removeAllTypes(node.getChild(i));
    }
  }

  static ValueTypeByHwMode getRegisterValueType(Record r, CodeGenTarget target) {
    boolean foundRC = false;
    ValueTypeByHwMode vt = new ValueTypeByHwMode();
    vt.getOrCreateTypeForMode(DefaultMode, new MVT(MVT.Other));
    ArrayList<CodeGenRegisterClass> rcs = target.getRegisterClasses();
    for (CodeGenRegisterClass regClass : rcs) {
      if (regClass.contains(r)) {
        if (!foundRC) {
          foundRC = true;
          regClass.getValueTypeAt(0);
          vt = regClass.getValueTypeAt(0);
        } else {
          // Multiple RCs
          if (vt != regClass.getValueTypeAt(0))
            return vt;
        }
      }
    }
    return vt;
  }

  static int getPatternSize(TreePatternNode pat, CodeGenDAGPatterns cgp) {
    int size = 3;
    if (pat.isLeaf() && pat.getLeafValue() instanceof IntInit)
      size += 2;

    ComplexPattern cp = nodeGetComplexPattern(pat, cgp);
    if (cp != null)
      size += cp.getNumOperands() * 3;

    if (!pat.getPredicateFns().isEmpty())
      ++size;

    for (int i = 0, e = pat.getNumChildren(); i < e; i++) {
      TreePatternNode child = pat.getChild(i);
      TypeSetByHwMode c0 = child.getExtType(0);
      if (!child.isLeaf() && c0.getMachineValueType().simpleVT != MVT.Other)
        size += getPatternSize(child, cgp);
      else if (child.isLeaf()) {
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

  static int getResultPatternCost(TreePatternNode inst, CodeGenDAGPatterns cgp) {
    if (inst.isLeaf()) return 0;

    int cost = 0;
    Record opc = inst.getOperator();
    if (opc.isSubClassOf("Instruction")) {
      ++cost;
      CodeGenInstruction cgInst = cgp.getTarget().getInstruction(opc.getName());
      if (cgInst.usesCustomDAGSchedInserter)
        cost += 10;
    }
    for (int i = 0, e = inst.getNumChildren(); i < e; i++)
      cost += getResultPatternCost(inst.getChild(i), cgp);
    return cost;
  }

  static int getResultPatternSize(TreePatternNode node, CodeGenDAGPatterns cgp) {
    if (node.isLeaf()) return 0;

    int size = 0;
    Record opc = node.getOperator();
    if (opc.isSubClassOf("Instruction"))
      size += opc.getValueAsInt("CodeSize");
    for (int i = 0, e = node.getNumChildren(); i < e; i++)
      size += getResultPatternSize(node.getChild(i), cgp);
    return size;
  }

  static class PatternSortingPredicate implements Comparator<Pair<PatternToMatch,
      ArrayList<Pair<GeneratedCodeKind, String>>>> {
    private CodeGenDAGPatterns cgp;

    PatternSortingPredicate(CodeGenDAGPatterns cgp) {
      this.cgp = cgp;
    }

    @Override
    public int compare(Pair<PatternToMatch, ArrayList<Pair<GeneratedCodeKind, String>>> o1,
                       Pair<PatternToMatch, ArrayList<Pair<GeneratedCodeKind, String>>> o2) {
      try {
        PatternToMatch lhs = o1.first, rhs = o2.first;
        int lhsSize = getPatternSize(lhs.getSrcPattern(), cgp);
        int rhsSize = getPatternSize(rhs.getSrcPattern(), cgp);
        lhsSize += lhs.getAddedComplexity();
        rhsSize += rhs.getAddedComplexity();
        if (lhsSize > rhsSize) return -1;
        if (lhsSize < rhsSize) return 1;

        int lhsCost = getResultPatternCost(lhs.getDstPattern(), cgp);
        int rhsCost = getResultPatternCost(rhs.getDstPattern(), cgp);
        if (lhsCost > rhsCost) return -1;
        if (lhsCost < rhsCost) return 1;

        return getResultPatternSize(rhs.getDstPattern(), cgp) -
            getResultPatternSize(lhs.getDstPattern(), cgp);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return 0;
    }
  }

  public ArrayList<String> emitResultCode(TreePatternNode node, ArrayList<Record> destRegs,
                                          boolean inFlagDecled, boolean resNodeDecled,
                                          boolean likeLeaf, boolean isRoot) {
    TypeSetByHwMode res0T = node.getExtType(0);
    ArrayList<String> nodeOps = new ArrayList<>();
    if (node.getName() != null && !node.getName().isEmpty()) {
      String varName = node.getName();
      boolean modifiedVal = false;
      if (!variableMap.containsKey(varName)) {
        System.err.printf("Variable '%s' referenced but not defined and not caught earlier!%n",
            varName);
        System.exit(-1);
      }
      String val = variableMap.get(varName);
      if (val.equals("Tmp")) {
        nodeOps.add("tmp");
        return nodeOps;
      }

      ComplexPattern cp;
      int resNo = tmpNo++;
      if (!node.isLeaf() && node.getOperator().getName().equals("imm")) {
        Util.assertion(node.getExtTypes().size() == 1, "Multiple types not handled!");
        String castType = null, tmpVar = "tmp" + resNo;
        switch (res0T.getMachineValueType().simpleVT) {
          default:
            System.err.printf("Can't handle %s type as an immediate constant, Aborting!%n",
                getEnumName(res0T.getMachineValueType().simpleVT));
            System.exit(-1);
            break;
          case MVT.i1:
            castType = "boolean";
            break;
          case MVT.i8:
            castType = "byte";
            break;
          case MVT.i16:
            castType = "short";
            break;
          case MVT.i32:
            castType = "int";
            break;
          case MVT.i64:
            castType = "long";
            break;
        }
        String temp = castType.equals("boolean") ? "((ConstantSDNode)" + val + ".getNode()).getZExtValue() != 0" :
            "((" + castType + ")" + "((ConstantSDNode)" + val + ".getNode()).getZExtValue())";
        emitCode(String.format("SDValue %s = curDAG.getTargetConstant(%s, new EVT(%s));", tmpVar, temp,
            getEnumName(res0T.getMachineValueType().simpleVT)));
        val = tmpVar;
        modifiedVal = true;
        nodeOps.add(val);
      } else if (!node.isLeaf() && node.getOperator().getName().equals("fpimm")) {
        Util.assertion(node.getExtTypes().size() == 1, "Multiple types not be handled yet!");
        String tmpVar = "tmp" + resNo;
        emitCode(String.format("SDValue %s = curDAG.getTargetConstantFP(((ConstantFPSDNode)%s)"
                + ".getConstantFPValue(), ((ConstantFPSDNode)%s).getValueType(0));",
            tmpVar, val, val));
        val = tmpVar;
        modifiedVal = true;
        nodeOps.add(val);
      } else if (!node.isLeaf() && node.getOperator().getName().equals("texternalsym")) {
        Record op;
        if (((op = operatorMap.get(node.getName())) != null && op.getName().equals("externalsym"))) {
          String tmpVar = "tmp" + resNo;
          emitCode(String.format("SDValue %s = curDAG.getTargetExternalSymbol"
                  + "(((ExternalSymbolSDNode)%s).getSymbol(), %s);", tmpVar, val,
              getEnumName(res0T.getMachineValueType().simpleVT)));
          val = tmpVar;
          modifiedVal = true;
        }
        nodeOps.add(val);
      } else if (!node.isLeaf() && (node.getOperator().getName().equals("tglobaladdr") ||
          node.getOperator().getName().equals("tglobaltlsaddr"))) {
        Record op;
        if ((op = operatorMap.get(node.getName())) != null && (op.getName().equals("globaladdr")
            || op.getName().equals("globaltlsaddr"))) {
          String tmpVar = "tmp" + resNo;
          emitCode(String.format("SDValue %s = curDAG.getTargetGlobalAddress"
                  + "(((GlobalAddressSDNode)%s).getGlobal(), %s);", tmpVar, val,
              getEnumName(res0T.getMachineValueType().simpleVT)));
          val = tmpVar;
          modifiedVal = true;
        }
        nodeOps.add(val);
      } else if (!node.isLeaf() && (node.getOperator().getName().equals("texternalsym")
          || node.getOperator().getName().equals("tconstpool"))) {
        nodeOps.add(val);
      } else if (node.isLeaf() && (cp = nodeGetComplexPattern(node, cgp)) != null) {
        for (int i = 0, e = cp.getNumOperands(); i < e; i++) {
          nodeOps.add("cpTmp" + val + i);
        }
      } else {
        if (!likeLeaf) {
          if (isRoot && node.isLeaf()) {
            emitCode("replaceUses(n, " + val + ");");
            emitCode("return null;");
          }
        }
        nodeOps.add(val);
      }

      if (modifiedVal)
        variableMap.put(varName, val);
      return nodeOps;
    }

    if (node.isLeaf()) {
      DefInit di = node.getLeafValue() instanceof DefInit ? (DefInit) node.getLeafValue() : null;
      if (di != null) {
        int resNo = tmpNo++;
        if (di.getDef().isSubClassOf("Register")) {
          String targetRegInfoClassName = cgp.getTarget().getName() + "GenRegisterNames";
          emitCode(String.format("SDValue tmp%d = curDAG.getRegister(%s.%s, %s);",
              resNo, targetRegInfoClassName, di.getDef().getName(),
              getEnumName(res0T.getMachineValueType().simpleVT)));
          nodeOps.add("tmp" + resNo);
          return nodeOps;
        } else if (di.getDef().getName().equals("zero_reg")) {
          emitCode(String.format("SDValue tmp%d = curDAG.getRegister(0, %s);", resNo,
              getEnumName(res0T.getMachineValueType().simpleVT)));
          nodeOps.add("tmp" + resNo);
          return nodeOps;
        } else if (di.getDef().isSubClassOf("RegisterClass")) {
          String targetRegInfoClassName = cgp.getTarget().getName() + "GenRegisterInfo";

          emitCode(String.format("SDValue tmp%d = curDAG.getTargetConstant(%s.%sRegClassID, new EVT(MVT.i32));",
              resNo, targetRegInfoClassName, di.getDef().getName()));
          nodeOps.add("tmp" + resNo);
          return nodeOps;
        }
      } else if (node.getLeafValue() instanceof IntInit) {
        IntInit ii = (IntInit) node.getLeafValue();
        int resNo = tmpNo++;
        Util.assertion(node.getExtTypes().size() == 1, "Multiple types are not handled yet!");
        emitCode(String.format("SDValue tmp%d = curDAG.getTargetConstant(%dL, new EVT(%s));",
            resNo, ii.getValue(), getEnumName(res0T.getMachineValueType().simpleVT)));
        nodeOps.add("tmp" + resNo);
        return nodeOps;
      }

      Util.assertion(false, "Unknown leaf value!");
      return nodeOps;
    }

    // handle the operator.
    Record op = node.getOperator();
    if (op.isSubClassOf("Instruction")) {
      CodeGenTarget cgt = cgp.getTarget();
      CodeGenInstruction cgInst = cgt.getInstruction(op.getName());
      DAGInstruction inst = cgp.getInstruction(op);
      TreePattern pat = inst.getPattern();

      TreePatternNode patNode = isRoot ?
          (pat != null ? pat.getTree(0) : pattern) :
          (pat != null ? pat.getTree(0) : null);
      if (patNode != null && !patNode.isLeaf() && patNode.getOperator().getName().equals("set")) {
        patNode = patNode.getChild(patNode.getNumChildren() - 1);
      }

      boolean isVariadic = isRoot && cgInst.isVariadic;
      boolean hasImpInputs = isRoot && inst.getNumImpOperands() > 0;
      boolean hasImpResults = isRoot && !destRegs.isEmpty();
      boolean nodeHasOptInFlag =
          isRoot && patternHasProperty(pattern, SDNPOptInFlag, cgp);
      boolean nodeHasInFlag =
          isRoot && patternHasProperty(pattern, SDNPInFlag, cgp);
      boolean nodeHasOutFlag =
          isRoot && patternHasProperty(pattern, SDNPOutFlag, cgp);
      boolean nodeHasChain =
          patNode != null && patternHasProperty(patNode, SDNPHasChain,
              cgp);
      boolean inputHasChain =
          isRoot && nodeHasProperty(pattern, SDNPHasChain, cgp);
      int numResults = inst.getNumResults();
      int numDestRegs = hasImpResults ? destRegs.size() : 0;

      // record output varargs info.
      outputIsVariadic = isVariadic;

      if (nodeHasOptInFlag) {
        emitCode(
            "boolean hasInFlag = (n.getOperand(n.getNumOperands()-1)"
                + ".getValueType().getSimpleVT().simpleVT == MVT.Flag);");
      }
      if (isVariadic) {
        emitCode("ArrayList<SDValue> ops" + opcNo + " = new ArrayList<>();");
      }

      int numPatResults = 0;
      for (int i = 0, e = pattern.getExtTypes().size(); i < e; i++) {
        int vt = pattern.getExtType(0).getMachineValueType().simpleVT;
        if (vt != MVT.isVoid && vt != MVT.Flag)
          ++numPatResults;
      }

      if (!originChains.isEmpty()) {
        emitCode("ArrayList<SDValue> inChains = new ArrayList<>();");
        for (int i = 0, e = originChains.size(); i < e; i++) {
          emitCode(String.format("if (%s.getNode() != %s.getNode()) {",
              originChains.get(i).first, originChains.get(i).second));
          emitCode(String
              .format("  inChains.add(%s);", originChains.get(i).first));
          emitCode("}");
        }
        emitCode(String.format("  inChains.add(%s);", chainName));
        emitCode(String.format("%s = curDAG.getNode(ISD.TokenFactor, new EVT(MVT.Other), "
            + "inChains);", chainName));
      }

      ArrayList<String> allOps = new ArrayList<>();
      for (int childNo = 0, instOpNo = numResults; instOpNo != cgInst.operandList.size(); ++instOpNo) {
        ArrayList<String> ops;

        Record operandNode = cgInst.operandList.get(instOpNo).rec;
        if ((operandNode.isSubClassOf("PredicateOperand") || operandNode
            .isSubClassOf("OptionalDefOperand")) && !cgp.getDefaultOperand(operandNode).defaultOps.isEmpty()) {
          DAGDefaultOperand defaultOp = cgp.getDefaultOperand(cgInst.operandList.get(instOpNo).rec);
          for (int i = 0, e = defaultOp.defaultOps.size(); i < e; i++) {
            ops = emitResultCode(defaultOp.defaultOps.get(i),
                destRegs, inFlagDecled, resNodeDecled, false,
                false);
            allOps.addAll(ops);
          }
        } else {
          ops = emitResultCode(node.getChild(childNo), destRegs,
              inFlagDecled, resNodeDecled, false, false);
          allOps.addAll(ops);
          ++childNo;
        }
      }
      boolean chainEmitted = nodeHasChain;
      if (nodeHasInFlag || hasImpInputs) {
        OutRef<Boolean> x = new OutRef<>(chainEmitted);
        OutRef<Boolean> y = new OutRef<>(inFlagDecled);
        OutRef<Boolean> z = new OutRef<>(resNodeDecled);
        emitIntFlagSelectCode(pattern, "n", x, y, z, true);
        chainEmitted = x.get();
        inFlagDecled = y.get();
        resNodeDecled = z.get();
      }
      if (nodeHasOptInFlag || nodeHasInFlag || hasImpInputs) {
        if (!inFlagDecled) {
          emitCode("SDValue inFlag = new SDValue(null, 0);");
          inFlagDecled = true;
        }
        if (nodeHasOptInFlag) {
          emitCode("if (hasInFlag) {");
          emitCode("  inFlag = n.getOperand(n.getNumOperands()-1);");
          emitCode("}");
        }
      }

      int resNo = tmpNo++;
      int opsNo = opcNo;
      String codePrefix = "";
      boolean chainAssignmentNeeded = nodeHasChain && !isRoot;
      LinkedList<String> after = new LinkedList<>();
      String nodeName;
      if (!isRoot) {
        nodeName = "tmp" + resNo;
        codePrefix = "SDValue " + nodeName + " = new SDValue(";
      } else {
        nodeName = "resNode";
        if (!resNodeDecled) {
          codePrefix = "SDNode " + nodeName + " = ";
          resNodeDecled = true;
        } else
          codePrefix = nodeName + " = ";
      }

      StringBuilder code = new StringBuilder("opc" + opcNo);

      Util.assertion(!cgInst.theDef.getName().isEmpty());
      emitOpcode(cgp.getTarget().getName() + "GenInstrNames." + cgInst.theDef.getName());

      if (numResults > 0 && node.getExtType(0).getMachineValueType().simpleVT != MVT.isVoid) {
        code.append(", vt").append(vtNo);
        emitVT("new EVT(" + getEnumName(res0T.getMachineValueType().simpleVT) + ")");
      }

      for (int i = 0; i < numDestRegs; i++) {
        Record rec = destRegs.get(i);
        if (rec.isSubClassOf("Register")) {
          ValueTypeByHwMode rvt = getRegisterValueType(rec, cgt);
          code.append(", new EVT(").append(getEnumName(rvt.getSimple().simpleVT)).append(")");
        }
      }

      if (nodeHasChain)
        code.append(", new EVT(MVT.Other)");
      if (nodeHasOutFlag)
        code.append(", new EVT(MVT.Flag)");

      if (isVariadic) {
        for (int i = 0, e = allOps.size(); i < e; i++) {
          emitCode("ops" + opsNo + ".add(" + allOps.get(i) + ");");
        }
        allOps.clear();

        String endAdjust = "";
        if (nodeHasInFlag || hasImpInputs) {
          endAdjust = "-1";
        } else if (nodeHasOptInFlag)
          endAdjust = "-(hasInFlag?1:0)";

        emitCode("for (int i = numInputRootOps" + (nodeHasChain ? " + 1" : "")
            + ", e = n.getNumOperands()" + endAdjust + "; i != e; i++) {");
        emitCode("  ops" + opsNo + ".add(n.getOperand(i));");
        emitCode("}");
      }

      if (cgInst.mayLoad || cgInst.mayStore) {
        for (String name : lsi) {
          String lsiName = "lsi" + name.toUpperCase();
          emitCode(String.format("SDValue %s = curDAG."
                  + "getMemOperand(((MemSDNode)%s.getNode()).getMemOperand());",
              lsiName, name));

          if (isVariadic)
            emitCode("ops" + opsNo + ".add(" + lsiName + ");");
          else
            allOps.add(lsiName);
        }
      }

      if (nodeHasChain) {
        if (isVariadic)
          emitCode("ops" + opsNo + ".add(" + chainName + ");");
        else
          allOps.add(chainName);
      }

      if (isVariadic) {
        if (nodeHasInFlag || hasImpResults)
          emitCode("ops" + opsNo + ".add(inFlag);");
        else if (nodeHasOptInFlag) {
          emitCode("if (hasInFlag)");
          emitCode("  ops" + opsNo + ".add(inFlag);");
        }
        code.append(", ops").append(opsNo);
      } else if (nodeHasInFlag || nodeHasOptInFlag || hasImpInputs)
        allOps.add("inFlag");

      int numOps = allOps.size();
      if (numOps > 0) {
        if (!nodeHasOptInFlag && numOps < 4) {
          for (int i = 0; i < numOps; i++)
            code.append(", ").append(allOps.get(i));
        } else {
          StringBuilder opsCode = new StringBuilder(
              "SDValue[] ops" + opsNo + " = {");
          for (int i = 0; i < numOps; i++) {
            opsCode.append(allOps.get(i));
            if (i < numOps - 1)
              opsCode.append(", ");
          }
          emitCode(opsCode.toString() + "};");
          code.append(", ops").append(opsNo).append(",");
          if (nodeHasOptInFlag) {
            code.append("hasInFlag ? ");
            code.append(numOps).append(":").append(numOps - 1);
          } else
            code.append(numOps);
        }
      }
      if (!isRoot)
        code.append("), 0");

      ArrayList<String> replaceFroms = new ArrayList<>();
      ArrayList<String> replaceTos = new ArrayList<>();
      if (!isRoot)
        nodeOps.add("tmp" + resNo);
      else {
        if (nodeHasOutFlag) {
          if (!inFlagDecled) {
            after.add("SDValue inFlag = new SDValue(resNode, " + (
                numResults + numDestRegs + (nodeHasChain ? 1 : 0)) + ");");
            inFlagDecled = true;
          } else {
            after.add("inFlag = new SDValue(resNode, " + (numResults
                + numDestRegs + (nodeHasChain ? 1 : 0)) + ");");
          }
        }

        for (int j = 0, e = foldedChains.size(); j < e; j++) {
          replaceFroms.add(String.format("new SDValue(%s.getNode(), %d)",
              foldedChains.get(j).first, foldedChains.get(j).second));
          replaceTos.add(String
              .format("new SDValue(resNode, %d)", numResults + numDestRegs));
        }

        if (nodeHasOutFlag) {
          if (!foldedFlag.first.isEmpty()) {
            replaceFroms.add(String.format("new SDValue(%s.getNode(), %d)",
                foldedFlag.first, foldedFlag.second));
            replaceTos.add("inFlag");
          } else {
            Util.assertion(nodeHasProperty(pattern, SDNPOutFlag, cgp));
            replaceFroms.add(String.format("new SDValue(n.getNode(), %d)",
                numPatResults + (inputHasChain ? 1 : 0)));
            replaceTos.add("inFlag");
          }
        }

        if (!replaceFroms.isEmpty() && inputHasChain) {
          replaceFroms.add(String.format("new SDValue(n.getNode(), %d)",
              numPatResults));
          replaceTos.add(String.format("new SDValue(%s.getNode(), %s.getResNo())",
              chainName, chainName));
          chainAssignmentNeeded |= nodeHasChain;
        }

        if (!inputHasChain && nodeHasChain && nodeHasOutFlag) {
          // noting to do!
        } else if (inputHasChain && !nodeHasChain) {
          if (nodeHasOutFlag) {
            replaceFroms.add(String.format("new SDValue(n.getNode(), %d)",
                numPatResults + 1));
            replaceTos.add("new SDValue(resNode, n.getResNo()-1)");
          }
          replaceFroms.add(String.format("new SDValue(n.getNode(), %d)",
              numPatResults));
          replaceTos.add(chainName);
        }
      }


      if (chainAssignmentNeeded) {
        String chainAssign;
        if (!isRoot)
          chainAssign = chainName + " = new SDValue(" + nodeName
              + ".getNode(), " + (numResults + numDestRegs) + ");";
        else
          chainAssign =
              chainName + " = new SDValue(" + nodeName + ", " + (
                  numResults + numDestRegs) + ");";

        after.addFirst(chainAssign);
      }

      if (replaceFroms.size() == 1) {
        after.add(String
            .format("replaceUses(%s, %s);", replaceFroms.get(0),
                replaceTos.get(0)));
      } else if (!replaceFroms.isEmpty()) {
        after.add("SDValue[] froms = {");
        for (int i = 0, e = replaceFroms.size(); i < e; i++)
          after.add(" " + replaceFroms.get(i) + (i + 1 < e ? "," : ""));
        after.add("};");
        after.add("SDValue[] tos = {");
        for (int i = 0, e = replaceTos.size(); i < e; i++)
          after.add(" " + replaceTos.get(i) + (i + 1 < e ? "," : ""));
        after.add("};");
        after.add("replaceUses(froms, tos);");
      }

      if (!isRoot || (inputHasChain && !nodeHasChain))
        code.insert(0, "curDAG.getTargetNode(");
      else
        code.insert(0, "curDAG.selectNodeTo(n.getNode(), ");

      if (isRoot) {
        if (after.isEmpty())
          codePrefix = "return ";
        else
          after.add("return resNode;");
      }

      emitCode(codePrefix + code.toString() + ");");
      after.forEach(this::emitCode);
      return nodeOps;
    }

    if (op.isSubClassOf("SDNodeXForm")) {
      Util.assertion(node.getNumChildren() == 1, "node xform should have one child!");

      ArrayList<String> ops = emitResultCode(node.getChild(0), destRegs,
          inFlagDecled, resNodeDecled, true, false);
      int resNo = tmpNo++;
      emitCode("SDValue tmp" + resNo + " = transform_" + op.getName() +
          "(" + ops.get(ops.size() - 1) + ".getNode());");
      nodeOps.add("tmp" + resNo);
      if (isRoot)
        emitCode("return tmp" + resNo + ".getNode();");
      return nodeOps;
    }

    Util.shouldNotReachHere("Unknown node in result pattern!");
    return null;
  }

  public boolean insertOneTypeCheck(TreePatternNode pat,
                                    TreePatternNode other,
                                    String prefix,
                                    boolean isRoot) {
    if (!pat.getExtTypes().equals(other.getExtTypes())) {
      pat.setTypes(other.getExtTypes());
      if (!isRoot)
        emitCheck(prefix + ".getNode().getValueType(0) == " +
            getName(pat.getExtType(0).getMachineValueType().simpleVT));
      return true;
    }
    int opNo = nodeHasProperty(pat, SDNPHasChain, cgp) ? 1 : 0;
    for (int i = 0, e = pat.getNumChildren(); i < e; i++, ++opNo) {
      if (insertOneTypesCheck(pat.getChild(i), other.getChild(i), prefix + opNo, false))
        return true;
    }
    return false;
  }
}

