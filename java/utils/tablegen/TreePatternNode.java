package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
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

import backend.codegen.EVT;
import backend.codegen.MVT;
import gnu.trove.list.array.TIntArrayList;
import utils.tablegen.Init.DefInit;
import utils.tablegen.Init.IntInit;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static backend.codegen.MVT.*;
import static utils.tablegen.CodeGenDAGPatterns.*;
import static utils.tablegen.CodeGenTarget.getValueType;
import static utils.tablegen.EEVT.*;
import static utils.tablegen.SDNP.SDNPCommutative;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class TreePatternNode implements Cloneable
{
    private TIntArrayList types;

    private Record operator;

    private Init val;

    private String name = "";

    /**
     * The predicate functions to execute on this node to check
     * for a match.  If this list is empty, no predicate is involved.
     */
    private ArrayList<String> predicateFns;

    private Record transformFn;

    private ArrayList<TreePatternNode> children;

    public TreePatternNode(Record op, ArrayList<TreePatternNode> chs)
    {
        types = new TIntArrayList();
        predicateFns = new ArrayList<>();
        operator = op;
        children = new ArrayList<>();
        children.addAll(chs);
        types.add(EEVT.isUnknown);
    }

    public TreePatternNode(Init leaf)
    {
        types = new TIntArrayList();
        predicateFns = new ArrayList<>();
        val = leaf;
        children = new ArrayList<>();
        types.add(EEVT.isUnknown);
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean isLeaf()
    {
        return val != null;
    }

    public boolean hasTypeSet()
    {
        int vt = types.get(0);
        return vt < LAST_VALUETYPE
                || vt == iPTR || vt == iPTRAny;
    }

    public boolean isTypeCompleteUnknown()
    {
        return types.get(0) == EEVT.isUnknown;
    }

    public boolean isTypeDynamicallyResolved()
    {
        int val = types.get(0);
        return val == iPTR || val == iPTRAny;
    }

    public int getTypeNum(int num)
    {
        assert hasTypeSet():"Doesn't have a type yet!";
        assert types.size() > num : "Type out of range!";
        return types.get(num);
    }

    public int getExtTypeNum(int num)
    {
        assert types.size() > num:"Extended type num out of range!";
        return types.get(num);
    }

    public TIntArrayList getExtTypes()
    {
        return types;
    }

    public void setTypes(TIntArrayList list)
    {
        types = list;
    }

    public void removeTypes()
    {
        types = new TIntArrayList();
        types.add(EEVT.isUnknown);
    }

    public Init getLeafValue()
    {
        assert isLeaf();
        return val;
    }

    public Record getOperator()
    {
        assert !isLeaf();
        return operator;
    }

    public int getNumChildren()
    {
        return children.size();
    }

    public TreePatternNode getChild(int idx)
    {
        return children.get(idx);
    }

    public void setChild(int idx, TreePatternNode node)
    {
        children.set(idx, node);
    }

    public ArrayList<String> getPredicateFns()
    {
        return predicateFns;
    }

    public void clearPredicateFns()
    {
        predicateFns.clear();
    }

    public void setPredicateFns(ArrayList<String> p)
    {
        assert predicateFns.isEmpty();
        this.predicateFns = p;
    }

    public void addPredicateFn(String fn)
    {
        assert !fn.isEmpty():"Empty predicate string!";
        if (!predicateFns.contains(fn))
            predicateFns.add(fn);
    }

    public Record getTransformFn()
    {
        return transformFn;
    }

    public void setTransformFn(Record transformFn)
    {
        this.transformFn = transformFn;
    }

    public CodeGenIntrinsic getIntrinsicInfo(CodeGenDAGPatterns cdp)
    {
        Record operator = getOperator();
        if (operator != cdp.getIntrinsicVoidSDNode() &&
                operator != cdp.getIntrinsicWChainSDNode() &&
                operator != cdp.getIntrinsicWOChainSDNode())
            return null;

        int iid = (int) ((IntInit)getChild(0).getLeafValue()).getValue();
        return cdp.getIntrinsicInfo(iid);
    }

    public boolean isCommutativeIntrinsic(CodeGenDAGPatterns cdp)
    {
        CodeGenIntrinsic intrinsic = getIntrinsicInfo(cdp);
        return intrinsic != null && intrinsic.isCommutative;
    }

    public void print(PrintStream os)
    {
        if (isLeaf())
            getLeafValue().print(os);
        else
            os.printf("(%s", getOperator().getName());

        int val = getExtTypeNum(0);
        switch (val)
        {
            case Other:
                os.print(":Other");
                break;
            case isInt:
                os.print(":isInt");
                break;
            case isFP:
                os.print(":isFP");
                break;
            case isVec:
                os.print(":isVec");
                break;
            case isUnknown:
                //os.print(":?");
                break;
            case iPTR:
                os.print(":iPTR");
                break;
            case iPTRAny:
                os.print(":iPTRAny");
                break;
            default:
            {
                String vtName = MVT.getName(val);
                if (vtName.substring(0, 4).equals("MVT."))
                    vtName = vtName.substring(4);
                os.printf(":%s", vtName);
            }
        }

        if (!isLeaf())
        {
            if (getNumChildren() != 0)
            {
                os.print(" ");
                getChild(0).print(os);
                for (int i = 1, e = getNumChildren(); i != e; i++)
                {
                    os.print(", ");
                    getChild(i).print(os);
                }
            }
            os.print(")");
        }

        for (int i = 0, e = predicateFns.size(); i < e; i++)
            os.printf("<<P:%s>>", predicateFns.get(i));
        if (transformFn != null)
            os.printf("<<X:%s>>", transformFn.getName());
        if (!getName().isEmpty())
            os.printf(":$%s", getName());
    }

    public void dump()
    {
        print(System.err);
    }

    /**
     * Apply all of the type constraints relevant to
     * this node and its children in the tree.  This returns true if it makes a
     * change, false otherwise.  If a type contradiction is found, throw an
     * exception.
     * @param tp
     * @param notRegisters
     * @return
     */
    public boolean applyTypeConstraints(TreePattern tp, boolean notRegisters)
            throws Exception
    {
        CodeGenDAGPatterns cdp = tp.getDAGPatterns();
        if (isLeaf())
        {
            DefInit di = getLeafValue() instanceof DefInit ? (DefInit)getLeafValue() : null;
            if (di != null)
            {
                return updateNodeType(getImplicitType(di.getDef(), notRegisters, tp), tp);
            }
            else if (getLeafValue() instanceof IntInit)
            {
                IntInit ii = (IntInit)getLeafValue();
                boolean madeChanged = updateNodeType(isInt, tp);

                if (hasTypeSet())
                {
                    assert getExtTypes().size() >= 1:"TreePattern doesn't have a type!";
                    int svt = getTypeNum(0);
                    for (int i = 1, e = getExtTypes().size(); i != e; i++)
                        assert getTypeNum(i) == svt:"TreePattern has too many types!";

                    svt = getTypeNum(0);
                    if (svt != iPTR && svt != iPTRAny)
                    {
                        int size = new EVT(svt).getSizeInBits();
                        if (size < 32)
                        {
                            int val = (int) ((ii.getValue() << (32 - size)) >> (32 -size));
                            if (val != ii.getValue())
                            {
                                int valueMask, unsignedVal;
                                valueMask = (~0) >> (32 - size);
                                unsignedVal = (int) ii.getValue();
                                if ((valueMask & unsignedVal) != unsignedVal)
                                {
                                    tp.error("Integer value '" + ii.getValue() +
                                            "' is out of range of type '" +
                                            MVT.getEnumName(getTypeNum(0)) + "'!");
                                }
                            }
                        }
                    }
                }
                return madeChanged;
            }
            return false;
        }

        CodeGenIntrinsic intrinsic;
        if (getOperator().getName().equals("set"))
        {
            assert getNumChildren() >= 2:"Missing RHS of a set?";
            int nc = getNumChildren();
            boolean madeChanged = false;
            for (int i = 0; i < nc - 1; i++)
            {
                madeChanged = getChild(i).applyTypeConstraints(tp, notRegisters);
                madeChanged |= getChild(nc - 1).applyTypeConstraints(tp, notRegisters);

                madeChanged |= getChild(i).updateNodeType(getChild(nc - 1).getExtTypes(), tp);
                madeChanged |= getChild(nc - 1).updateNodeType(getChild(i).getExtTypes(), tp);
                madeChanged |= updateNodeType(isVoid, tp);
            }
            return madeChanged;
        }
        else if (getOperator().getName().equals("implicit") ||
                getOperator().getName().equals("parallel"))
        {
            boolean madeChanged = false;
            for (int i = 0; i < getNumChildren(); i++)
            {
                madeChanged = getChild(i).applyTypeConstraints(tp, notRegisters);
            }
            madeChanged |= updateNodeType(isVoid, tp);
            return madeChanged;
        }
        else if (getOperator().getName().equals("COPY_TO_REGCLASS"))
        {
            boolean madeChanged = false;
            madeChanged |= getChild(0).applyTypeConstraints(tp, notRegisters);
            madeChanged |= getChild(1).applyTypeConstraints(tp, notRegisters);
            madeChanged |= updateNodeType(getChild(1).getTypeNum(0), tp);
            return madeChanged;
        }
        else if ((intrinsic = getIntrinsicInfo(cdp)) != null)
        {
            boolean madeChange = false;

            int numRetVTs = intrinsic.is.retVTs.size();
            int numParamVTs = intrinsic.is.paramVTs.size();

            for (int i = 0; i != numRetVTs; i++)
                madeChange |= updateNodeType(intrinsic.is.retVTs.get(i), tp);

            if (getNumChildren() != numParamVTs + numRetVTs)
            {
                tp.error("Intrinsic '" + intrinsic.name + "' expects " + (
                        numParamVTs + numRetVTs - 1) + " operands, not " + (
                        getNumChildren() - 1) + " operands!");
            }

            madeChange |= getChild(0).updateNodeType(iPTR, tp);

            for (int i = numRetVTs, e = getNumChildren(); i != e; i++)
            {
                int opVT = intrinsic.is.paramVTs.get(i - numRetVTs);
                madeChange |= getChild(i).updateNodeType(opVT, tp);
                madeChange |= getChild(i).applyTypeConstraints(tp, notRegisters);
            }

            return madeChange;
        }
        else if (getOperator().isSubClassOf("SDNode"))
        {
            SDNodeInfo ni = cdp.getSDNodeInfo(getOperator());

            boolean madeChanged = ni.applyTypeConstraints(this, tp);
            for (int i = 0, e = getNumChildren(); i != e; ++i)
            {
                madeChanged |= getChild(i).applyTypeConstraints(tp, notRegisters);
            }

            if (ni.getNumResults() == 0)
            {
                madeChanged |= updateNodeType(isVoid, tp);
            }
            return madeChanged;
        }
        else if (getOperator().isSubClassOf("Instruction"))
        {
            DAGInstruction instr = cdp.getInstruction(getOperator());
            boolean madeChanged = false;
            int numResults = instr.getNumResults();

            assert numResults <= 1:"Only supports zero or one result instrs!";
            CodeGenInstruction instInfo = cdp.getTarget().getInstruction(getOperator().getName());
            if (numResults == 0 || instInfo.numDefs == 0)
            {
                madeChanged = updateNodeType(isVoid, tp);
            }
            else
            {
                Record resultNode = instr.getResult(0);

                if (resultNode.isSubClassOf("PointerLikeRegClass"))
                {
                    TIntArrayList vts = new TIntArrayList();
                    vts.add(iPTR);
                    madeChanged = updateNodeType(vts, tp);
                }
                else if (resultNode.getName().equals("unknown"))
                {
                    TIntArrayList vts = new TIntArrayList();
                    vts.add(isUnknown);
                    madeChanged = updateNodeType(vts, tp);
                }
                else
                {
                    assert resultNode.isSubClassOf("RegisterClass")
                            :"Operands should be register class";

                    CodeGenRegisterClass rc = cdp.getTarget().getRegisterClass(resultNode);
                    madeChanged = updateNodeType(convertVTs(rc.getValueTypes()), tp);
                }
            }

            int childNo = 0;
            for (int i = 0, e = instr.getNumOperands(); i != e; i++)
            {
                Record operandNode = instr.getOperand(i);

                // If the instruction expects a predicate or optional def operand, we
                // codegen this by setting the operand to it's default value if it has a
                // non-empty DefaultOps field.
                if ((operandNode.isSubClassOf("PredicateOperand") ||
                        operandNode.isSubClassOf("OptionalDefOperand")) &&
                        !cdp.getDefaultOperand(operandNode).defaultOps.isEmpty())
                {
                    continue;
                }

                if (childNo >= getNumChildren())
                {
                    tp.error("Instruction '" + getOperator().getName() +
                            "' expects more operands than were provided.");
                }

                int vt;
                TreePatternNode child = getChild(childNo++);
                if (operandNode.isSubClassOf("RegisterClass"))
                {
                    CodeGenRegisterClass rc = cdp.getTarget().getRegisterClass(operandNode);
                    madeChanged |= child.updateNodeType(convertVTs(rc.getValueTypes()), tp);
                }
                else if (operandNode.isSubClassOf("Operand"))
                {
                    vt = getValueType(operandNode.getValueAsDef("Type"));
                    madeChanged |= child.updateNodeType(vt, tp);
                }
                else if (operandNode.isSubClassOf("PointerLikeRegClass"))
                {
                    madeChanged |= child.updateNodeType(iPTR, tp);
                }
                else if (operandNode.getName().equals("unknown"))
                {
                    madeChanged |= child.updateNodeType(isUnknown, tp);
                }
                else
                {
                    assert false:"Undefined operand type!";
                    System.exit(0);
                }
                madeChanged |= child.applyTypeConstraints(tp, notRegisters);
            }

            if (childNo != getNumChildren())
                tp.error("Instruction '" + getOperator().getName() +
                        "' was provided too many operands!");

            return madeChanged;
        }
        else
        {
            //System.out.println(getOperator());
            assert getOperator().isSubClassOf("SDNodeXForm"):"Undefined node type!";

            if (getNumChildren() != 1)
            {
                tp.error("Node transform '" + getOperator().getName() +
                            "' requires one operand!");
            }

            if (!hasTypeSet() || !getChild(0).hasTypeSet())
            {
                boolean madeChanged = updateNodeType(getChild(0).getExtTypes(), tp);
                madeChanged |= getChild(0).updateNodeType(getExtTypes(), tp);
                return madeChanged;
            }
            return false;
        }
    }

    /**
     * Check to see if the specified record has an implicit
     * type which should be applied to it.  This will infer the type of register
     * references from the register file information, for example.
     * @param r
     * @param notRegisters
     * @param tp
     * @return
     */
    private TIntArrayList getImplicitType(Record r, boolean notRegisters,
            TreePattern tp) throws Exception
    {
        TIntArrayList unknown = new TIntArrayList();
        unknown.add(isUnknown);
        TIntArrayList other = new TIntArrayList();
        other.add(Other);

        if (r.isSubClassOf("RegisterClass"))
        {
            if (notRegisters)
                return unknown;
            CodeGenRegisterClass rc = tp.getDAGPatterns().getTarget().getRegisterClass(r);
            return convertVTs(rc.getValueTypes());
        }
        else if (r.isSubClassOf("PatFlag"))
        {
            // Pattern fragment types will be resolved when they are inlined.
            return unknown;
        }
        else if (r.isSubClassOf("Register"))
        {
            if (notRegisters)
                return unknown;
            CodeGenTarget target = tp.getDAGPatterns().getTarget();
            return target.getRegisterVTs(r);
        }
        else if (r.isSubClassOf("ValueType") || r.isSubClassOf("CondCode"))
        {
            // Using a VTSDNode or CondCodeSDNode.
            return other;
        }
        else if (r.isSubClassOf("ComplexPattern"))
        {
            if (notRegisters)
            {
                return unknown;
            }
            TIntArrayList complexPat = new TIntArrayList();
            complexPat.add(tp.getDAGPatterns().getComplexPattern(r).getValueType());
            return complexPat;
        }
        else if (r.isSubClassOf("PointerLikeRegClass"))
        {
            other.set(0, iPTR);
            return other;
        }
        else if (r.getName().equals("node") || r.getName().equals("srcvalue")
                || r.getName().equals("zero_reg"))
        {
            return unknown;
        }

        tp.error("Undefined node flavour used in pattern: " + r.getName());
        return other;
    }

    public boolean containsUnresolvedType()
    {
        if (!hasTypeSet() && !isTypeDynamicallyResolved()) return true;
        for (TreePatternNode node : children)
            if (node.containsUnresolvedType())
                return true;
        return false;
    }

    @Override
    public TreePatternNode clone()
    {
        TreePatternNode res;
        if (isLeaf())
            res = new TreePatternNode(getLeafValue());
        else
        {
            ArrayList<TreePatternNode> childs = new ArrayList<>();
            children.forEach(ch->childs.add(ch.clone()));
            res = new TreePatternNode(getOperator(), childs);
        }
        res.setName(getName());
        res.setTypes(getExtTypes());
        res.setPredicateFns(getPredicateFns());
        res.setTransformFn(getTransformFn());
        return res;
    }

    /**
     * If it is impossible for this pattern to match on this
     * target, fill in Reason and return false.  Otherwise, return true.  This is
     * used as a sanity check for .td files (to prevent people from writing stuff
     * that can never possibly work), and to prevent the pattern permuter from
     * generating stuff that is useless.
     * @param reason
     * @param cdp
     * @return
     */
    public boolean canPatternMatch(StringBuilder reason,
            CodeGenDAGPatterns cdp)
    {
        if (isLeaf()) return true;

        for (TreePatternNode node : children)
        {
            if (!node.canPatternMatch(reason, cdp))
                return false;
        }

        // If this is an intrinsic, handle cases that would make it not match.  For
        // example, if an operand is required to be an immediate.
        if (getOperator().isSubClassOf("Intrinsic"))
            return true;

        SDNodeInfo nodeInfo = cdp.getSDNodeInfo(getOperator());
        boolean isCommIntrinsic = isCommutativeIntrinsic(cdp);
        if (nodeInfo.hasProperty(SDNPCommutative) || isCommIntrinsic)
        {
            // Scan all of the operands of the node and make sure that only the last one
            // is a constant node, unless the RHS also is.
            if (!onlyOnRHSOfCommutative(getChild(getNumChildren() - 1)))
            {
                int skip = isCommIntrinsic ?1:0;
                for (int i = skip; i < getNumChildren() - 1; i++)
                {
                    if (onlyOnRHSOfCommutative(getChild(i)))
                    {
                        reason.append("Immediate value must be on the RHS of commutative operators!");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Return true if this value is only allowed on the
     * RHS of a commutative operation, not the on LHS.
     * @param node
     * @return
     */
    private static boolean onlyOnRHSOfCommutative(TreePatternNode node)
    {
        if (!node.isLeaf() && node.getOperator().getName().equals("imm"))
            return true;
        if (node.isLeaf() && (node.getLeafValue() instanceof IntInit))
            return true;

        return false;
    }

    /**
     * Return true if this node is recursively
     * isomorphic to the specified node.  For this comparison, the node's
     * entire state is considered. The assigned name is ignored, since
     * nodes with differing names are considered isomorphic. However, if
     * the assigned name is present in the dependent variable set, then
     * the assigned name is considered significant and the node is
     * isomorphic if the names match.
     * @param node
     * @param depVars
     * @return
     */
    public boolean isIsomorphicTo(TreePatternNode node,
            HashSet<String> depVars)
    {
        if (node == this) return true;
        if (node.isLeaf() != isLeaf() || getExtTypes() != node.getExtTypes()
                || getPredicateFns() != node.getPredicateFns()
                | getTransformFn() != node.getTransformFn())
            return false;

        if (isLeaf())
        {
            DefInit di = getLeafValue() instanceof DefInit ? (DefInit)getLeafValue() : null;
            if (di != null)
            {
                DefInit ndi = node.getLeafValue() instanceof DefInit ? (DefInit)node.getLeafValue() : null;
                if (ndi != null)
                {
                    return di.getDef().equals(ndi.getDef()) && (!depVars.contains(getName())
                            || getName().equals(node.getName()));
                }
            }
            return getLeafValue().equals(node.getLeafValue());
        }

        if (node.getOperator() != getOperator() || node.getNumChildren() !=getNumChildren())
            return false;

        for (int i = 0, e = getNumChildren(); i != e; i++)
            if (!getChild(i).isIsomorphicTo(node.getChild(i), depVars))
                return false;

        return true;
    }

    /**
     * Set the node type of node to VT if VT contains
     * information.  If node already contains a conflicting type, then throw an
     * exception.  This returns true if any information was updated.
     * @param extTypes
     * @param tp
     * @return
     */
    public boolean updateNodeType(TIntArrayList extTypes,
            TreePattern tp) throws Exception
    {
        assert !extTypes.isEmpty():"Cannot update node type with empty type vector!";

        if (extTypes.get(0) == EEVT.isUnknown
                || lhsIsSubsetOfRHS(getExtTypes(), extTypes))
            return false;

        if (isTypeCompleteUnknown() || lhsIsSubsetOfRHS(extTypes, getExtTypes()))
        {
            setTypes(extTypes);
            return true;
        }
        int v = getExtTypeNum(0);
        if (v == iPTR || v == iPTRAny)
        {
            if (extTypes.get(0) == iPTR || extTypes.get(0) == iPTRAny
                    || extTypes.get(0) == isInt)
                return false;

            if (EEVT.isExtIntegerInVTs(extTypes))
            {
                TIntArrayList fvts = filterEVTs(extTypes, isInteger);
                if (!fvts.isEmpty())
                {
                    setTypes(extTypes);
                    return true;
                }
            }
        }

        if ((extTypes.get(0) == isInt || extTypes.get(0) == iAny)
                && EEVT.isExtIntegerInVTs(getExtTypes()))
        {
            assert hasTypeSet() :"should be handled above!";
            TIntArrayList fvts = filterEVTs(getExtTypes(), isInteger);
            if (getExtTypes().equals(fvts))
                return false;
            setTypes(fvts);
            return true;
        }

        if ((extTypes.get(0) == iPTR || extTypes.get(0) == iPTRAny)
                && EEVT.isExtIntegerInVTs(getExtTypes()))
        {
            TIntArrayList fvts = filterEVTs(getExtTypes(), isInteger);
            if (getExtTypes().equals(fvts))
                return false;
            if (!fvts.isEmpty())
            {
                setTypes(fvts);
                return true;
            }
        }

        if ((extTypes.get(0) == isFP || extTypes.get(0) == fAny)
                && EEVT.isExtFloatingPointInVTs(getExtTypes()))
        {
            assert hasTypeSet(): "should be handled above!";
            TIntArrayList fvts = filterEVTs(getExtTypes(), isFloatingPoint);
            if (fvts.equals(getExtTypes()))
                return false;

            setTypes(fvts);
            return true;
        }

        if ((extTypes.get(0) == isVec || extTypes.get(0) == vAny)
                && EEVT.isExtVectorVTs(getExtTypes()))
        {
            assert hasTypeSet():"should be handled above!";
            TIntArrayList fvts = filterEVTs(getExtTypes(), isVector);
            if (fvts.equals(getExtTypes()))
                return false;
            setTypes(fvts);
            return true;
        }

        // If we know this is an int, FP, or vector type, and we are told it is a
        // specific one, take the advice.
        //
        // Similarly, we should probably set the type here to the intersection of
        // {isInt|isFP|isVec} and ExtVTs
        v = getExtTypeNum(0);
        if ((getExtTypeNum(0) == isInt || v == iAny) &&
                EEVT.isExtIntegerInVTs(extTypes) ||
                ((getExtTypeNum(0) == isFP || v == fAny) &&
                EEVT.isExtFloatingPointInVTs(extTypes)) ||
                ((getExtTypeNum(0) == isVec || v == vAny) &&
                        EEVT.isExtVectorVTs(extTypes)))
        {
            setTypes(extTypes);
            return true;
        }

        if (getExtTypeNum(0) == isInt &&
                (extTypes.get(0) == iPTR || extTypes.get(0) == iPTRAny))
        {
            setTypes(extTypes);
            return true;
        }

        if (isLeaf())
        {
            dump();
            System.err.printf(" ");
            tp.error("Type inference contradiction found in node!");
        }
        else
        {
            tp.error("Type inference contradiction found in node " +
                getOperator().getName() + "!");
        }

        // Unreachable.
        return true;
    }

    public boolean updateNodeType(int extVT, TreePattern pattern)
            throws Exception
    {
        TIntArrayList list = new TIntArrayList();
        list.add(extVT);
        return updateNodeType(list, pattern);
    }

    private boolean lhsIsSubsetOfRHS(
            TIntArrayList lhs,
            TIntArrayList rhs)
    {
        if (lhs.size() > rhs.size()) return false;
        for (int i = 0, e = lhs.size(); i != e; i++)
            if (!rhs.contains(lhs.get(i)))
                return false;

        return true;
    }

    /**
     * If this pattern refers to any pattern fragments, inline them into place,
     * giving us a pattern without any PatFrag references.
     * @param pattern
     * @return
     */
    public TreePatternNode inlinePatternFragments(TreePattern pattern)
            throws Exception
    {
        // nothing to de.
        if (isLeaf()) return this;

        Record op = getOperator();

        if (!op.isSubClassOf("PatFrag"))
        {
            // Just recursively inline children nodes.
            for (int i = 0, e = getNumChildren(); i != e; i++)
            {
                TreePatternNode child = getChild(i);
                TreePatternNode newChild = child.inlinePatternFragments(pattern);

                assert (child.getPredicateFns().isEmpty()
                    || newChild.getPredicateFns().equals(child.getPredicateFns()))
                        :"Non-empty child predicate clobbered!";
                setChild(i, newChild);
            }
            return this;
        }

        // Otherwise, we found a reference to a fragment. First, look up to the
        // TreePattern record.
        TreePattern frag = pattern.getDAGPatterns().getPatternFragment(op);

        // Verify that we are passing the right number of operands.
        if (frag.getNumArgs() != children.size())
        {
            pattern.error("'" + op.getName() + "' fragment requires " +
                frag.getNumArgs() + " operands!");
        }

        TreePatternNode fragTree = frag.getOnlyTree().clone();
        String code = op.getValueAsCode("Predicate");
        if (code != null && !code.isEmpty())
            fragTree.addPredicateFn("Predicate_" + op.getName());

        // Resolve formal arguments to their actual value.
        if (frag.getNumArgs() != 0)
        {
            // Compute the map of formal to actual arguments.
            HashMap<String, TreePatternNode> argMap = new HashMap<>();
            for (int i = 0, e = frag.getNumArgs(); i != e; i++)
                argMap.put(frag.getArgName(i), getChild(i).inlinePatternFragments(pattern));

            fragTree.substituteFromalArguments(argMap);
        }

        fragTree.setName(getName());
        fragTree.updateNodeType(getExtTypes(), pattern);

        // Transfer in the old predicateFns.
        for (String fn : getPredicateFns())
            fragTree.addPredicateFn(fn);

        // Get a new copy of this fragment to stitch into here.

        // The fragment we inlined could have recursive inlining that is needed.  See
        // if there are any pattern fragments in it and inline them as needed.
        return fragTree.inlinePatternFragments(pattern);
    }

    /**
     * Replace the formal arguments in this tree with actual values specified
     * by ArgMap.
     * @param argMap
     */
    private void substituteFromalArguments(
            HashMap<String, TreePatternNode> argMap)
    {
        if (isLeaf())
            return;

        for (int i = 0, e = getNumChildren(); i != e; i++)
        {
            TreePatternNode child = getChild(i);
            if (child.isLeaf())
            {
                Init val = child.getLeafValue();
                if (val instanceof DefInit &&
                        ((DefInit)val).getDef().getName().equals("node"))
                {
                    // We found a use of a formal argument, replace it with its
                    // value.
                    assert argMap.containsKey(child.getName()) :"Couldn't find formal argument!";
                    TreePatternNode newChild = argMap.get(child.getName());
                    assert child.getPredicateFns().isEmpty() ||
                            newChild.getPredicateFns().equals(child.getPredicateFns())
                            :"Non empty child predicate clobered!";
                    setChild(i, newChild);
                }
            }
            else
            {
                getChild(i).substituteFromalArguments(argMap);
            }
        }
    }
}
