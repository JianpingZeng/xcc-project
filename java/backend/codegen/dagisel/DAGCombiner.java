/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.codegen.dagisel;

import backend.analysis.aa.AliasAnalysis;
import backend.codegen.EVT;
import backend.codegen.MVT;
import backend.codegen.MachineFrameInfo;
import backend.codegen.PseudoSourceValue;
import backend.codegen.dagisel.SDNode.*;
import backend.target.TargetData;
import backend.target.TargetLowering;
import backend.target.TargetLowering.TargetLoweringOpt;
import backend.target.TargetMachine;
import backend.type.ArrayType;
import backend.type.Type;
import backend.value.Constant;
import backend.value.ConstantArray;
import backend.value.ConstantFP;
import tools.APInt;
import tools.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;

import static backend.codegen.dagisel.SelectionDAG.isCommutativeBinOp;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class DAGCombiner
{
    private SelectionDAG dag;
    private CombineLevel level;
    private TargetMachine.CodeGenOpt optLevel;
    private boolean legalOprations;
    private boolean legalTypes;
    private LinkedList<SDNode> workList;
    private AliasAnalysis aa;
    private TargetLowering tli;

    public DAGCombiner(SelectionDAG dag, AliasAnalysis aa,
            TargetMachine.CodeGenOpt optLevel)
    {
        this.dag = dag;
        this.aa = aa;
        this.optLevel = optLevel;
        workList = new LinkedList<>();
        tli = dag.getTargetLoweringInfo();
    }

    public void run(CombineLevel level)
    {
        this.level = level;
        legalOprations = level.compareTo(CombineLevel.NoIllegalOperations) >= 0;
        legalTypes = level.compareTo(CombineLevel.NoIllegalTypes) >= 0;

        workList.addAll(dag.allNodes);

        // create a dummy node that adds a reference to the root node, preventing
        // it from beging deleted, and tracking any change of root.
        SDNode.HandleSDNode dummy = new SDNode.HandleSDNode(dag.getRoot());

        dag.setRoot(new SDValue());

        while (!workList.isEmpty())
        {
            SDNode n = workList.pop();
            if (n.isUseEmpty() && !n.equals(dummy))
            {
                // if this node has no uses, so it is dead.
                for (int i = 0, e = n.getNumOperands(); i < e; i++)
                    addToWorkList(n.getOperand(i).getNode());

                dag.deleteNode(n);
                continue;
            }
            SDValue rv = combine(n);
            if (rv.getNode() == null || rv.getNode().equals(n))
                continue;

            assert n.getOpcode() != ISD.DELETED_NODE &&
                    rv.getNode().getOpcode() != ISD.DELETED_NODE:
                    "Node was deleted but visit returned new node!";

            WorklistRemover remover = new WorklistRemover(this);
            if (n.getNumValues() == rv.getNode().getNumValues())
                dag.replaceAllUsesWith(n, rv.getNode(), remover);
            else
            {
                assert n.getValueType(0).equals(rv.getValueType()) &&
                        n.getNumValues() == 1:"Type mismatch!";
                dag.replaceAllUsesWith(n, rv, remover);
            }

            addToWorkList(rv.getNode());
            addUsersToWorklist(rv.getNode());

            for (int i = 0, e = n.getNumOperands(); i < e; i++)
                addToWorkList(n.getOperand(i).getNode());

            if (n.isUseEmpty())
            {
                removeFromWorkList(n);
                dag.deleteNode(n);
            }
        }
        dag.setRoot(dummy.getValue());
        dummy.dropOperands();
        dag.removeDeadNodes();
    }

    private SDValue combine(SDNode n)
    {
        SDValue rv = visit(n);
        if (rv.getNode() == null)
        {
            assert n.getOpcode() != ISD.DELETED_NODE:"Node was deleted but visit returned null!";

            if (n.getOpcode() >= ISD.BUILTIN_OP_END ||
                    tli.hasTargetDAGCombine(n.getOpcode()))
            {
                DAGCombinerInfo combineInfo = new DAGCombinerInfo(dag,
                        !legalTypes, !legalOprations, false, this);
                rv = tli.performDAGCombine(n, combineInfo);
            }
        }

        if (rv.getNode() == null && isCommutativeBinOp(n.getOpcode()) &&
                n.getNumValues() == 1)
        {
            SDValue n0 = n.getOperand(0);
            SDValue n1 = n.getOperand(1);
            if (n0.getNode() instanceof ConstantSDNode ||
                    !(n1.getNode() instanceof ConstantSDNode))
            {
                SDValue[] ops = {n1, n0};
                SDNode cseNode = dag.getNodeIfExists(n.getOpcode(), n.getValueList(),
                        ops);
                if (cseNode != null)
                    return new SDValue(cseNode, 0);
            }
        }
        return rv;
    }

    private SDValue visit(SDNode n)
    {
        switch(n.getOpcode()) 
        {
            default: break;
            case ISD.TokenFactor:        return visitTokenFactor(n);
            case ISD.MERGE_VALUES:       return visitMERGE_VALUES(n);
            case ISD.ADD:                return visitADD(n);
            case ISD.SUB:                return visitSUB(n);
            case ISD.ADDC:               return visitADDC(n);
            case ISD.ADDE:               return visitADDE(n);
            case ISD.MUL:                return visitMUL(n);
            case ISD.SDIV:               return visitSDIV(n);
            case ISD.UDIV:               return visitUDIV(n);
            case ISD.SREM:               return visitSREM(n);
            case ISD.UREM:               return visitUREM(n);
            case ISD.MULHU:              return visitMULHU(n);
            case ISD.MULHS:              return visitMULHS(n);
            case ISD.SMUL_LOHI:          return visitSMUL_LOHI(n);
            case ISD.UMUL_LOHI:          return visitUMUL_LOHI(n);
            case ISD.SDIVREM:            return visitSDIVREM(n);
            case ISD.UDIVREM:            return visitUDIVREM(n);
            case ISD.AND:                return visitAND(n);
            case ISD.OR:                 return visitOR(n);
            case ISD.XOR:                return visitXOR(n);
            case ISD.SHL:                return visitSHL(n);
            case ISD.SRA:                return visitSRA(n);
            case ISD.SRL:                return visitSRL(n);
            case ISD.CTLZ:               return visitCTLZ(n);
            case ISD.CTTZ:               return visitCTTZ(n);
            case ISD.CTPOP:              return visitCTPOP(n);
            case ISD.SELECT:             return visitSELECT(n);
            case ISD.SELECT_CC:          return visitSELECT_CC(n);
            case ISD.SETCC:              return visitSETCC(n);
            case ISD.SIGN_EXTEND:        return visitSIGN_EXTEND(n);
            case ISD.ZERO_EXTEND:        return visitZERO_EXTEND(n);
            case ISD.ANY_EXTEND:         return visitANY_EXTEND(n);
            case ISD.SIGN_EXTEND_INREG:  return visitSIGN_EXTEND_INREG(n);
            case ISD.TRUNCATE:           return visitTRUNCATE(n);
            case ISD.BIT_CONVERT:        return visitBIT_CONVERT(n);
            case ISD.BUILD_PAIR:         return visitBUILD_PAIR(n);
            case ISD.FADD:               return visitFADD(n);
            case ISD.FSUB:               return visitFSUB(n);
            case ISD.FMUL:               return visitFMUL(n);
            case ISD.FDIV:               return visitFDIV(n);
            case ISD.FREM:               return visitFREM(n);
            case ISD.FCOPYSIGN:          return visitFCOPYSIGN(n);
            case ISD.SINT_TO_FP:         return visitSINT_TO_FP(n);
            case ISD.UINT_TO_FP:         return visitUINT_TO_FP(n);
            case ISD.FP_TO_SINT:         return visitFP_TO_SINT(n);
            case ISD.FP_TO_UINT:         return visitFP_TO_UINT(n);
            case ISD.FP_ROUND:           return visitFP_ROUND(n);
            case ISD.FP_ROUND_INREG:     return visitFP_ROUND_INREG(n);
            case ISD.FP_EXTEND:          return visitFP_EXTEND(n);
            case ISD.FNEG:               return visitFNEG(n);
            case ISD.FABS:               return visitFABS(n);
            case ISD.BRCOND:             return visitBRCOND(n);
            case ISD.BR_CC:              return visitBR_CC(n);
            case ISD.LOAD:               return visitLOAD(n);
            case ISD.STORE:              return visitSTORE(n);
            case ISD.INSERT_VECTOR_ELT:  return visitINSERT_VECTOR_ELT(n);
            case ISD.EXTRACT_VECTOR_ELT: return visitEXTRACT_VECTOR_ELT(n);
            case ISD.BUILD_VECTOR:       return visitBUILD_VECTOR(n);
            case ISD.CONCAT_VECTORS:     return visitCONCAT_VECTORS(n);
            case ISD.VECTOR_SHUFFLE:     return visitVECTOR_SHUFFLE(n);
        }
        return new SDValue();
    }

    private SDValue visitVECTOR_SHUFFLE(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitCONCAT_VECTORS(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitBUILD_VECTOR(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitEXTRACT_VECTOR_ELT(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitINSERT_VECTOR_ELT(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitSTORE(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitLOAD(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitBR_CC(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitBRCOND(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitFABS(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitFNEG(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitFP_EXTEND(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitFP_ROUND_INREG(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitFP_ROUND(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitFP_TO_UINT(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitFP_TO_SINT(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitUINT_TO_FP(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitSINT_TO_FP(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitFCOPYSIGN(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitFREM(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitFDIV(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitFMUL(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitFSUB(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitFADD(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitBUILD_PAIR(SDNode n)
    {
        EVT vt = n.getValueType(0);
        return combineConsecutiveLoads(n, vt);
    }
    private SDNode getBuildPairElt(SDNode n, int i)
    {
        SDValue elt = n.getOperand(i);
        if (elt.getOpcode() != ISD.MERGE_VALUES)
            return elt.getNode();
        return elt.getOperand(elt.getResNo()).getNode();
    }

    private SDValue combineConsecutiveLoads(SDNode n, EVT vt)
    {
        assert n.getOpcode() == ISD.BUILD_PAIR;
        SDNode t = getBuildPairElt(n, 0);
        LoadSDNode ld1 = t instanceof LoadSDNode ?
                (LoadSDNode)t : null;
        t = getBuildPairElt(n, 1);
        LoadSDNode ld2 = t instanceof LoadSDNode ?
                (LoadSDNode)t : null;
        if (ld1 == null || ld2 == null || !ld1.isNONExtLoad() ||
                !ld1.hasOneUse())
            return new SDValue();

        EVT ld1VT = ld1.getValueType(0);
        MachineFrameInfo mfi = dag.getMachineFunction().getFrameInfo();
        if (ld2.isNONExtLoad() && ld2.hasOneUse() &&
                !ld1.isVolatile() &&
                !ld2.isVolatile() &&
                tli.isConsecutiveLoad(ld2, ld1, ld1VT.getSizeInBits()/8, 1, mfi))
        {
            int align = ld1.getAlignment();
            int newAlign = tli.getTargetData().getABITypeAlignment(
                    vt.getTypeForEVT());
            if (newAlign <= align &&
                    (!legalOprations ||
                    tli.isOperationLegal(ISD.LOAD, vt)))
            {
                return dag.getLoad(vt, ld1.getChain(),
                        ld1.getBasePtr(),
                        ld1.getSrcValue(),
                        ld1.getSrcValueOffset(),
                        false, align);
            }
        }
        return new SDValue();
    }

    private SDValue visitBIT_CONVERT(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitTRUNCATE(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitSIGN_EXTEND_INREG(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitANY_EXTEND(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitZERO_EXTEND(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitSIGN_EXTEND(SDNode n)
    {
        return new SDValue();
    }

    private SDValue simplifySetCC(EVT vt, SDValue op0,
                                  SDValue op1, CondCode cond)
    {
        return simplifySetCC(vt, op0, op1, cond, true);
    }

    private SDValue simplifySetCC(EVT vt,
                                  SDValue op0,
                                  SDValue op1,
                                  CondCode cond,
                                  boolean foldBooleans)
    {
        DAGCombinerInfo dci = new DAGCombinerInfo(dag, !legalTypes,
                !legalOprations, false, this);
        return tli.simplifySetCC(vt, op0, op1, cond, foldBooleans, dci);
    }

    private SDValue visitSETCC(SDNode n)
    {
        return simplifySetCC(n.getValueType(0), n.getOperand(0), n.getOperand(1),
                ((CondCodeSDNode)n.getOperand(2).getNode()).getCondition());
    }

    private SDValue visitSELECT_CC(SDNode n)
    {
        SDValue n0 = n.getOperand(0);
        SDValue n1 = n.getOperand(1);
        SDValue n2 = n.getOperand(2);
        SDValue n3 = n.getOperand(3);
        SDValue n4 = n.getOperand(4);
        CondCode cc = ((CondCodeSDNode)n4.getNode()).getCondition();

        if (n2.equals(n3)) return n2;
        SDValue scc = simplifySetCC(
                new EVT(tli.getSetCCResultType(n0.getValueType())),
                n0, n1, cc, false);
        if (scc.getNode() != null)
            addToWorkList(scc.getNode());

        if (scc.getNode() instanceof ConstantSDNode)
        {
            ConstantSDNode ssd = (ConstantSDNode)scc.getNode();
            if (!ssd.isNullValue())
                return n2;
            return n3;
        }

        if (scc.getNode() != null && scc.getOpcode() == ISD.SETCC)
        {
            return dag.getNode(ISD.SELECT_CC, n2.getValueType(),
                    scc.getOperand(0), scc.getOperand(1), n2, n3,
                    scc.getOperand(2));
        }
        if (simplifySelectOps(n, n2, n3))
            return new SDValue(n, 0);
        return simplifySelectCC(n0, n1, n2, n3, cc);
    }

    private SDValue visitSELECT(SDNode n)
    {
        SDValue n0 = n.getOperand(0); // condition
        SDValue n1 = n.getOperand(1); // lhs
        SDValue n2 = n.getOperand(2); // rhs
        ConstantSDNode c0 = n0.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n0.getNode() : null;
        ConstantSDNode c1 = n1.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n1.getNode() : null;
        ConstantSDNode c2 = n2.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n2.getNode() : null;
        EVT vt = n.getValueType(0);
        EVT vt0 = n0.getValueType();

        // fold (select C, X, X) -> X
        if (n1.equals(n2)) return n1;

        // fold (select true, X, Y) -> X
        if (n0.getNode() instanceof ConstantSDNode &&
                !((ConstantSDNode)n0.getNode()).isNullValue())
        {
            return n1;
        }

        // fold (select false, X, Y) -> Y
        if (n0.getNode() instanceof ConstantSDNode &&
                ((ConstantSDNode)n0.getNode()).isNullValue())
        {
            return n2;
        }
        // fold (select C, 1, X) -> (or C, X)
        if (n0.getNode() instanceof ConstantSDNode &&
                n1.getNode() instanceof ConstantSDNode &&
                ((ConstantSDNode)n1.getNode()).getAPIntValue().eq(1))
        {
            return dag.getNode(ISD.OR, vt, n0, n2);
        }
        // fold (select C, 0, 1) -> (xor C, 1)
        if (n0.getNode() instanceof ConstantSDNode &&
                n1.getNode() instanceof ConstantSDNode &&
                ((ConstantSDNode)n1.getNode()).getAPIntValue().eq(0) &&
                n2.getNode() instanceof ConstantSDNode &&
                ((ConstantSDNode)n2.getNode()).getAPIntValue().eq(1))
        {
            return dag.getNode(ISD.XOR, vt, n0, n2);
        }
        // fold (select C, 0, X) -> (and (not C), X)
        if (n0.getNode() instanceof ConstantSDNode &&
                n1.getNode() instanceof ConstantSDNode &&
                ((ConstantSDNode)n1.getNode()).getAPIntValue().eq(0))
        {
            APInt cc = ((ConstantSDNode)n0.getNode()).getAPIntValue();
            return dag.getNode(ISD.AND, vt, dag.getConstant(cc.not(), vt0, false), n2);
        }
        // fold (select C, X, 1) -> (or (not C), X)
        if (n0.getNode() instanceof ConstantSDNode &&
                n2.getNode() instanceof ConstantSDNode &&
                ((ConstantSDNode)n2.getNode()).getAPIntValue().eq(1))
        {
            APInt cc = ((ConstantSDNode)n0.getNode()).getAPIntValue();
            return dag.getNode(ISD.OR, vt, dag.getConstant(cc.not(), vt0, false), n1);
        }
        // fold (select C, X, 0) -> (and C, X)
        if (n0.getNode() instanceof ConstantSDNode &&
                n2.getNode() instanceof ConstantSDNode &&
                ((ConstantSDNode)n2.getNode()).getAPIntValue().eq(0))
        {
            return dag.getNode(ISD.AND, vt, n0, n1);
        }

        // fold (select X, X, Y) -> (or X, Y)
        if (n0.equals(n1))
        {
            return dag.getNode(ISD.OR, vt, n0, n2);
        }
        // fold (select X, 1, Y) -> (or X, Y)
        if (n1.getNode() instanceof ConstantSDNode &&
                ((ConstantSDNode)n1.getNode()).getAPIntValue().eq(1))
        {
            return dag.getNode(ISD.OR, vt, n0, n2);
        }

        // fold (select X, Y, X) -> (and X, Y)
        if (n0.equals(n2))
        {
            return dag.getNode(ISD.AND, vt, n0, n1);
        }
        // fold (select X, Y, 0) -> (and X, Y)
        if (n2.getNode() instanceof ConstantSDNode &&
                ((ConstantSDNode)n2.getNode()).isNullValue())
        {
            return dag.getNode(ISD.AND, vt, n0, n1);
        }

        // If we can fold this based on the true/false value, do so.
        if (simplifySelectOps(n, n1, n2))
            return new SDValue(n, 0);

        // fold selects based on a setcc into other things, such as min/max/abs
        if (n0.getOpcode() == ISD.SETCC)
        {
            if (tli.isOperationLegalOrCustom(ISD.SELECT_CC, new EVT(MVT.Other)) &&
                    tli.isOperationLegalOrCustom(ISD.SELECT, vt))
            {
                return dag.getNode(ISD.SELECT_CC, vt,
                        n0.getOperand(0),
                        n0.getOperand(1),
                        n1, n2, n0.getOperand(2));
            }
            return simplifySelect(n0, n1, n2);
        }
        return new SDValue();
    }

    private boolean simplifySelectOps(SDNode sel, SDValue lhs, SDValue rhs)
    {
        if(lhs.getOpcode() == rhs.getOpcode() &&
                lhs.hasOneUse() && rhs.hasOneUse())
        {
            if (lhs.getOpcode() == ISD.LOAD &&
                    !((LoadSDNode)lhs.getNode()).isVolatile() &&
                    !((LoadSDNode)rhs.getNode()).isVolatile() &&
                    lhs.getOperand(0).equals(rhs.getOperand(0)))
            {
                LoadSDNode ld1 = (LoadSDNode)lhs.getNode();
                LoadSDNode ld2 = (LoadSDNode)rhs.getNode();
                if (ld1.getMemoryVT().equals(ld2.getMemoryVT()))
                {
                    SDValue addr = new SDValue();
                    if (sel.getOpcode() == ISD.SELECT)
                    {
                        if (!ld1.isPredecessorOf(sel.getOperand(0).getNode()) &&
                                !ld2.isPredecessorOf(sel.getOperand(0).getNode()))
                        {
                            addr = dag.getNode(ISD.SELECT,
                                    ld1.getBasePtr().getValueType(),
                                    sel.getOperand(0),
                                    ld1.getBasePtr(),
                                    ld2.getBasePtr());
                        }
                    }
                    else
                    {
                        if (!ld1.isPredecessorOf(sel.getOperand(0).getNode()) &&
                                !ld2.isPredecessorOf(sel.getOperand(0).getNode()) &&
                                !ld1.isPredecessorOf(sel.getOperand(1).getNode()) &&
                                !ld2.isPredecessorOf(sel.getOperand(1).getNode()))
                        {
                            addr = dag.getNode(ISD.SELECT_CC,
                                    ld1.getBasePtr().getValueType(),
                                    sel.getOperand(0), sel.getOperand(1),
                                    ld1.getBasePtr(), ld2.getBasePtr(),
                                    sel.getOperand(4));
                        }
                    }

                    if (addr.getNode() != null)
                    {
                        SDValue load = new SDValue();
                        if (ld1.getExtensionType() == LoadExtType.NON_EXTLOAD)
                        {
                            load = dag.getLoad(sel.getValueType(0),
                                    ld1.getChain(), addr, ld1.getSrcValue(),
                                    ld1.getSrcValueOffset(), ld1.isVolatile(),
                                    ld1.getAlignment());
                        }
                        else
                        {
                            load = dag.getExtLoad(ld1.getExtensionType(),
                                    sel.getValueType(0),
                                    ld1.getChain(),
                                    addr,
                                    ld1.getSrcValue(),
                                    ld1.getSrcValueOffset(),
                                    ld1.getMemoryVT(),
                                    ld1.isVolatile(),
                                    ld1.getAlignment());
                        }
                        combineTo(sel, load, true);
                        combineTo(lhs.getNode(), load.getValue(0), load.getValue(1), true);
                        combineTo(rhs.getNode(), load.getValue(0), load.getValue(1), true);
                    }
                }
            }
        }
        return false;
    }

    private SDValue simplifySelect(SDValue n0, SDValue n1, SDValue n2)
    {
        assert n0.getOpcode() == ISD.SETCC:
                "First argument must be a SetCC node!";;
        SDValue scc = simplifySelectCC(n0.getOperand(0),
                n0.getOperand(1), n1, n2,
                ((CondCodeSDNode)n0.getOperand(2).getNode()).getCondition());
        if (scc.getNode() != null)
        {
            if (scc.getOpcode() == ISD.SELECT_CC)
            {
                SDValue setcc = dag.getNode(ISD.SETCC, n0.getValueType(),
                        scc.getOperand(0), scc.getOperand(1),
                        scc.getOperand(4));
                addToWorkList(setcc.getNode());
                return dag.getNode(ISD.SELECT, scc.getValueType(),
                        scc.getOperand(2), scc.getOperand(3), setcc);
            }
            return new SDValue();
        }
        return new SDValue();
    }
    private SDValue simplifySelectCC(SDValue n0, SDValue n1, SDValue n2, SDValue n3,
                                     CondCode cc)
    {
        return simplifySelectCC(n0, n1, n2, n3, cc, false);
    }

    private SDValue simplifySelectCC(SDValue n0, SDValue n1, SDValue n2, SDValue n3,
                                     CondCode cc, boolean notExtCompare)
    {
        if (n2.equals(n3)) return n2;
        EVT vt = n2.getValueType();
        ConstantSDNode c1 = n1.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n1.getNode() : null;
        ConstantSDNode c2 = n2.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n2.getNode() : null;
        ConstantSDNode c3 = n3.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n3.getNode() : null;
        SDValue scc = simplifySetCC(
                new EVT(tli.getSetCCResultType(n0.getValueType())),
                n0, n1, cc, false);
        if (scc.getNode() != null)
            addToWorkList(scc.getNode());
        ConstantSDNode sccc = scc.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)scc.getNode() : null;

        if (sccc != null && !sccc.isNullValue())
            return n2;
        if (sccc != null && sccc.isNullValue())
            return n3;

        if (n1.getNode() instanceof ConstantFPSDNode)
        {
            ConstantFPSDNode fps = (ConstantFPSDNode)n1.getNode();
            // Allow either -0.0 or 0.0
            if (fps.getValueAPF().isZero())
            {
                if ((cc == CondCode.SETGE || cc == CondCode.SETGT) &&
                        n0.equals(n2) && n3.getOpcode() == ISD.FNEG &&
                        n2.equals(n3.getOperand(0)))
                {
                    return dag.getNode(ISD.FABS, vt, n0);
                }

                if ((cc == CondCode.SETLT || cc == CondCode.SETLE) &&
                        n0.equals(n3) && n2.getOpcode() == ISD.FNEG &&
                        n2.getOperand(0).equals(n3))
                {
                    return dag.getNode(ISD.FABS, vt, n3);
                }
            }
        }

        if (n2.getNode() instanceof ConstantFPSDNode)
        {
            ConstantFPSDNode tv = (ConstantFPSDNode)n2.getNode();
            if (n3.getNode() instanceof ConstantFPSDNode)
            {
                ConstantFPSDNode fv = (ConstantFPSDNode)n3.getNode();
                if (tli.isTypeLegal(n2.getValueType()) &&
                        (tli.getOperationAction(ISD.ConstantFP, n2.getValueType()) !=
                        TargetLowering.LegalizeAction.Legal) &&
                        (tv.hasOneUse() || fv.hasOneUse()))
                {
                    Constant[] elts = {
                            fv.getConstantFPValue(),
                            tv.getConstantFPValue()
                    };
                    Type fpTy = elts[0].getType();
                    TargetData td = tli.getTargetData();

                    Constant ca = ConstantArray.get(ArrayType.get(fpTy, 2), elts);
                    SDValue cpIdx = dag.getConstantPool(ca, new EVT(tli.getPointerTy()),
                            td.getPrefTypeAlignment(fpTy), 0, false, 0);
                    int alignment = ((ConstantPoolSDNode)cpIdx.getNode()).getAlign();

                    SDValue zero = dag.getIntPtrConstant(0);
                    long eltSize = td.getTypeAllocSize(elts[0].getType());
                    SDValue one = dag.getIntPtrConstant(1);
                    SDValue cond = dag.getSetCC(new EVT(tli.getSetCCResultType(n0.getValueType())),
                            n0, n1, cc);

                    SDValue cstOffset = dag.getNode(ISD.SELECT, zero.getValueType(),
                            cond, one, zero);
                    cpIdx = dag.getNode(ISD.ADD, new EVT(tli.getPointerTy()), cpIdx, cstOffset);
                    return dag.getLoad(tv.getValueType(0),
                            dag.getEntryNode(), cpIdx,
                            PseudoSourceValue.getConstantPool(), 0,
                            false, alignment);
                }
            }
        }

        if (c1 != null && c3 != null && c3.isNullValue() &
                cc == CondCode.SETLT &&
                n0.getValueType().isInteger() &&
                n2.getValueType().isInteger() &&
                (c1.isNullValue() || (c1.getAPIntValue().eq(1) &&
                n0.equals(n2))))
        {
            EVT xTy = n0.getValueType();
            EVT aTy = n2.getValueType();
            if (xTy.bitsGT(aTy))
            {
                if (c2 != null && c2.getAPIntValue().and(c2.getAPIntValue().sub(1)).eq(0))
                {
                    long shCtv = c2.getAPIntValue().logBase2();
                    SDValue shCt = dag.getConstant(shCtv, new EVT(tli.getShiftAmountTy()), false);
                    SDValue shift = dag.getNode(ISD.SRL,
                            xTy, n0, shCt);
                    addToWorkList(shift.getNode());

                    if (xTy.bitsGT(aTy))
                    {
                        shift = dag.getNode(ISD.TRUNCATE, aTy, shift);
                        addToWorkList(shift.getNode());
                    }
                    return dag.getNode(ISD.AND, aTy, shift, n2);
                }

                SDValue shift = dag.getNode(ISD.SRA, xTy, n0,
                        dag.getConstant(xTy.getSizeInBits() -1,
                                new EVT(tli.getShiftAmountTy()), false));
                addToWorkList(shift.getNode());
                if (xTy.bitsGT(aTy))
                {
                    shift = dag.getNode(ISD.TRUNCATE, aTy, shift);
                    addToWorkList(shift.getNode());
                }
                return dag.getNode(ISD.AND, aTy, shift, n2);
            }
        }

        if (c2 != null && c3 != null && c3.isNullValue() &&
                c2.getAPIntValue().isPowerOf2() &&
                tli.getBooleanContents() == TargetLowering.BooleanContent.ZeroOrOneBooleanContent)
        {
            if (notExtCompare && c2.getAPIntValue().eq(1))
                return new SDValue();

            SDValue temp, sc;
            if (legalTypes)
            {
                sc = dag.getSetCC(new EVT(tli.getSetCCResultType(
                        n0.getValueType())), n0, n1, cc);
                if (n2.getValueType().bitsGT(sc.getValueType()))
                    temp = dag.getZeroExtendInReg(sc, n2.getValueType());
                else
                    temp = dag.getNode(ISD.ZERO_EXTEND, n2.getValueType(), sc);
            }
            else
            {
                sc = dag.getSetCC(new EVT(MVT.i1), n0, n1, cc);
                temp = dag.getNode(ISD.ZERO_EXTEND, n2.getValueType(), sc);
            }

            addToWorkList(sc.getNode());
            addToWorkList(temp.getNode());

            if (c2.getAPIntValue().eq(1))
                return temp;

            return dag.getNode(ISD.SHL, n2.getValueType(), temp,
                    dag.getConstant(c2.getAPIntValue().logBase2(),
                            new EVT(tli.getShiftAmountTy()), false));
        }

        if (c1 != null && c1.isNullValue() && (cc == CondCode.SETLT ||
            cc == CondCode.SETLE) && n0.equals(n3) &&
                n2.getOpcode() == ISD.SUB && n0.equals(n2.getOperand(1)) &&
                n2.getOperand(0).equals(n1) && n0.getValueType().isInteger())
        {
            EVT xType = n0.getValueType();
            SDValue shift = dag.getNode(ISD.SRA, xType, n0,
                    dag.getConstant(xType.getSizeInBits()-1,
                            new EVT(tli.getShiftAmountTy()), false));
            SDValue add = dag.getNode(ISD.ADD, xType, n0, shift);
            addToWorkList(shift.getNode());;
            addToWorkList(add.getNode());
            return dag.getNode(ISD.XOR, xType, add, shift);
        }

        if (c1 != null && c1.isAllOnesValue() && cc == CondCode.SETGT &&
                n0.equals(n2) && n3.getOpcode() == ISD.SUB &&
                n0.equals(n3.getOperand(1)))
        {
            if (n3.getOperand(0).getNode() instanceof ConstantSDNode)
            {
                ConstantSDNode csd = (ConstantSDNode)n3.getOperand(0).getNode();
                EVT xType = n0.getValueType();
                if (csd.isNullValue() && xType.isInteger())
                {
                    SDValue shift = dag.getNode(ISD.SRA, xType, n0,
                            dag.getConstant(xType.getSizeInBits()-1,
                                    new EVT(tli.getShiftAmountTy()), false));
                    SDValue add = dag.getNode(ISD.ADD, xType, n0, shift);
                    addToWorkList(shift.getNode());
                    addToWorkList(add.getNode());
                    return dag.getNode(ISD.XOR, xType, add, shift);
                }
            }
        }
        return new SDValue();
    }

    private SDValue visitCTPOP(SDNode n)
    {
        SDValue n0 = n.getOperand(0);
        EVT vt = n.getValueType(0);
        if (n0.getNode() instanceof ConstantSDNode)
            return dag.getNode(ISD.CTPOP, vt, n0);

        return new SDValue();
    }

    private SDValue visitCTTZ(SDNode n)
    {
        SDValue n0 = n.getOperand(0);
        EVT vt = n.getValueType(0);
        if (n0.getNode() instanceof ConstantSDNode)
            return dag.getNode(ISD.CTTZ, vt, n0);

        return new SDValue();
    }

    private SDValue visitCTLZ(SDNode n)
    {
        SDValue n0 = n.getOperand(0);
        EVT vt = n.getValueType(0);
        if (n0.getNode() instanceof ConstantSDNode)
            return dag.getNode(ISD.CTLZ, vt, n0);

        return new SDValue();
    }

    private SDValue visitSRL(SDNode n)
    {
        SDValue n0 = n.getOperand(0);
        SDValue n1 = n.getOperand(1);
        ConstantSDNode c0 = n0.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n0.getNode() : null;
        ConstantSDNode c1 = n1.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n1.getNode() : null;
        EVT vt = n0.getValueType();
        int opSizeInBits = vt.getSizeInBits();

        if (c0 != null && c1 != null)
        {
            return dag.foldConstantArithmetic(ISD.SRL, vt, c0, c1);
        }
        if (c0 != null && c0.isNullValue())
            return n0;
        if (c1 != null && c1.getZExtValue() >= opSizeInBits)
            return dag.getUNDEF(vt);
        if (c1 != null && c1.isNullValue())
            return n0;
        if (c1 != null && dag.maskedValueIsZero(new SDValue(n, 0),
                APInt.getAllOnesValue(opSizeInBits)))
            return dag.getConstant(0, vt, false);

        if (c1 != null && n0.getOpcode() == ISD.SRL &&
                n0.getOperand(1).getOpcode() == ISD.Constant)
        {
            long t1 = ((ConstantSDNode)n0.getOperand(1).getNode()).getZExtValue();
            long t2 = c1.getZExtValue();
            if ((t1 + t2) > opSizeInBits)
                return dag.getConstant(0, vt, false);
            return dag.getNode(ISD.SRL, vt, n0.getOperand(0),
                    dag.getConstant(t1+t2, n1.getValueType(), false));
        }

        if (c1 != null && n0.getOpcode() == ISD.ANY_EXTEND)
        {
            EVT smallVT = n0.getOperand(0).getValueType();
            if (c1.getZExtValue() >= smallVT.getSizeInBits())
                return dag.getUNDEF(vt);

            SDValue smallShift = dag.getNode(ISD.SRL, smallVT, n0.getOperand(0), n1);
            addToWorkList(smallShift.getNode());
            return dag.getNode(ISD.ANY_EXTEND, vt, smallShift);
        }

        if (c1 != null && c1.getZExtValue() + 1 == vt.getSizeInBits() &&
                n0.getOpcode() == ISD.SRA)
        {
            return dag.getNode(ISD.SRL, vt, n0.getOperand(0), n1);
        }

        if (c1 != null && n0.getOpcode() == ISD.CTLZ &&
                c1.getAPIntValue().eq(Util.log2(vt.getSizeInBits())))
        {
            APInt[] knownVals = new APInt[2];
            APInt mask = APInt.getAllOnesValue(vt.getSizeInBits());
            dag.computeMaskedBits(n0.getOperand(0), mask, knownVals, 0);
            APInt knownZero = knownVals[0];
            APInt knownOne = knownVals[1];
            if (knownOne.getBoolValue()) return dag.getConstant(0, vt, false);

            APInt unknownBits = knownZero.not().and(mask);
            if (unknownBits.eq(0)) return dag.getConstant(1, vt, false);

            if (unknownBits.and(unknownBits.sub(1)).eq(0))
            {
                int shAmt = unknownBits.countTrailingZeros();
                SDValue op = n0.getOperand(0);
                if (shAmt != 0)
                {
                    op = dag.getNode(ISD.SRL, vt, op,
                            dag.getConstant(shAmt, new EVT(tli.getShiftAmountTy()), false));
                    addToWorkList(op.getNode());
                }
                return dag.getNode(ISD.XOR, vt, op, dag.getConstant(1, vt, false));
            }
        }

        if (n1.getOpcode() == ISD.TRUNCATE &&
                n1.getOperand(0).getOpcode() == ISD.AND &&
                n1.hasOneUse() && n1.getOperand(0).hasOneUse())
        {
            SDValue n101 = n1.getOperand(0).getOperand(1);
            if (n101.getNode() instanceof ConstantSDNode)
            {
                ConstantSDNode cst = (ConstantSDNode)n101.getNode();
                EVT truncVT = n1.getValueType();
                SDValue n100 = n1.getOperand(0).getOperand(0);
                APInt trunc = cst.getAPIntValue();
                trunc.trunc(truncVT.getSizeInBits());
                return dag.getNode(ISD.SRL, vt, n0, dag.getNode(ISD.AND, truncVT,
                        dag.getNode(ISD.TRUNCATE, truncVT, n100),
                        dag.getConstant(trunc, truncVT, false)));
            }
        }

        if (c1 != null && simplifyDemandedBits(new SDValue(n, 0)))
            return new SDValue(n, 0);
        return c1 != null ? visitShiftByConstant(n, c1.getZExtValue()) : new SDValue();
    }

    private SDValue visitSRA(SDNode n)
    {

        return new SDValue();
    }

    private SDValue visitSHL(SDNode n)
    {
        SDValue n0 = n.getOperand(0);
        SDValue n1 = n.getOperand(1);
        ConstantSDNode c0 = n0.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n0.getNode() : null;
        ConstantSDNode c1 = n1.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n1.getNode() : null;
        EVT vt = n0.getValueType();
        int opSizeInBits = vt.getSizeInBits();

        if (c0 != null && c1 != null)
        {
            return dag.foldConstantArithmetic(ISD.SHL, vt, c0, c1);
        }
        if (c0 != null && c0.isNullValue())
            return n0;
        if (c1 != null && c1.getZExtValue() >= opSizeInBits)
            return dag.getUNDEF(vt);
        if (c1 != null && c1.isNullValue())
            return n0;
        if (dag.maskedValueIsZero(new SDValue(n, 0), APInt.getAllOnesValue(vt.getSizeInBits())))
        {
            return dag.getConstant(0, vt, false);
        }
        if (n1.getOpcode() == ISD.TRUNCATE &&
                n1.getOperand(0).getOpcode() == ISD.AND &&
                n1.hasOneUse() && n1.getOperand(0).hasOneUse())
        {
            SDValue n101 = n1.getOperand(0).getOperand(1);
            if (n101.getNode() instanceof ConstantSDNode)
            {
                ConstantSDNode n101C = (ConstantSDNode)n101.getNode();
                EVT truncVT = n1.getValueType();
                SDValue n100 = n1.getOperand(0).getOperand(0);
                APInt trunc = n101C.getAPIntValue();
                trunc.trunc(truncVT.getSizeInBits());
                return dag.getNode(ISD.SHL, vt, n0,
                        dag.getNode(ISD.AND, truncVT, dag.getNode(ISD.TRUNCATE, truncVT, n100),
                                dag.getConstant(trunc, truncVT, false)));
            }
        }

        if (c1 != null && n0.getOpcode() == ISD.SHL &&
                n0.getOperand(1).getOpcode() == ISD.Constant)
        {
            long t1 = ((ConstantSDNode)n0.getOperand(1).getNode()).getZExtValue();
            long t2 = c1.getZExtValue();
            if ((t1 + t2) > opSizeInBits)
                return dag.getConstant(0, vt, false);
            return dag.getNode(ISD.SHL, vt, n0.getOperand(0),
                    dag.getConstant(t1+t2, n1.getValueType(), false));
        }

        if (c1 != null && n0.getOpcode() == ISD.SRL &&
                n0.getOperand(1).getOpcode() == ISD.Constant)
        {
            long t1 = ((ConstantSDNode)n0.getOperand(1).getNode()).getZExtValue();
            if (t1 < vt.getSizeInBits())
            {
                long t2 = c1.getZExtValue();
                SDValue hiBitsMask = dag.getConstant(APInt.getHighBitsSet(vt.getSizeInBits(),
                        (int) (vt.getSizeInBits() - t1)), vt, false);
                SDValue mask = dag.getNode(ISD.AND, vt, n0.getOperand(0), hiBitsMask);
                if (t2 > t1)
                    return dag.getNode(ISD.SHL, vt, mask, dag.getConstant(t2-t1, n1.getValueType(), false));
                else
                    return dag.getNode(ISD.SRL, vt, mask, dag.getConstant(t1-t2, n1.getValueType(), false));
            }
        }

        if (c1 != null && n0.getOpcode() == ISD.SRA && n1.equals(n0.getOperand(1)))
        {
            SDValue hiBitsMask = dag.getConstant(APInt.getHighBitsSet(vt.getSizeInBits(),
                    (int) (vt.getSizeInBits() - c1.getZExtValue())), vt, false);
            return dag.getNode(ISD.AND, vt, n0.getOperand(0), hiBitsMask);
        }

        return c1 != null ? visitShiftByConstant(n, c1.getZExtValue()) : new SDValue();
    }

    private SDValue visitShiftByConstant(SDNode n, long amt)
    {
        SDNode lhs = n.getOperand(0).getNode();
        if (!lhs.hasOneUse()) return new SDValue();

        boolean highBits = false;
        switch (lhs.getOpcode())
        {
            default: return new SDValue();
            case ISD.OR:
            case ISD.XOR:
                highBits = false;
                break;
            case ISD.AND:
                highBits = true;
                break;
            case ISD.ADD:
                if (n.getOpcode() != ISD.SHL)
                    return new SDValue();
                highBits = false;
                break;
        }

        if (!(lhs.getOperand(1).getNode() instanceof ConstantSDNode))
            return new SDValue();

        ConstantSDNode binOpCst = (ConstantSDNode)lhs.getOperand(1).getNode();
        SDNode binOpLhsVal = lhs.getOperand(0).getNode();
        int binOpc = binOpLhsVal.getOpcode();
        if ((binOpc != ISD.SHL && binOpc != ISD.SRA && binOpc != ISD.SRL) ||
                !(binOpLhsVal.getOperand(1).getNode() instanceof ConstantSDNode))
            return new SDValue();

        EVT vt = n.getValueType(0);
        if (n.getOpcode() == ISD.SRA)
        {
            boolean binOpRhsSignSet = binOpCst.getAPIntValue().isNegative();
            if (binOpRhsSignSet != highBits)
                return new SDValue();
        }

        SDValue newRhs = dag.getNode(n.getOpcode(), n.getValueType(0), lhs.getOperand(1), n.getOperand(1));
        SDValue newShift = dag.getNode(n.getOpcode(), vt, lhs.getOperand(0), n.getOperand(1));
        return dag.getNode(lhs.getOpcode(), vt, newShift, newRhs);
    }

    private SDValue visitXOR(SDNode n)
    {
        SDValue op0 = n.getOperand(0);
        SDValue op1 = n.getOperand(1);
        ConstantSDNode op0C = op0.getNode() instanceof ConstantSDNode
                ? (ConstantSDNode)op0.getNode():null;
        ConstantSDNode op1C = op1.getNode() instanceof ConstantSDNode
                ? (ConstantSDNode)op1.getNode():null;
        EVT vt = op0.getValueType();
        if (vt.isVector())
        {
            SDValue foldedOp = simplifyVBinOp(n);
            if (foldedOp.getNode() != null)
                return foldedOp;
        }

        if (op0.getOpcode() == ISD.UNDEF && op1.getOpcode() == ISD.UNDEF)
            return dag.getConstant(0, vt, false);
        if (op0.getOpcode() == ISD.UNDEF)
            return op0;
        if (op1.getOpcode() == ISD.UNDEF)
            return op1;
        // fold (xor c1, c2) -> c1 ^ c2.
        if (op0C != null && op1C != null)
            return dag.foldConstantArithmetic(ISD.XOR, vt, op0C, op1C);
        if (op0C != null && op1C == null)
            return dag.getNode(ISD.XOR, vt, op1, op0);
        if (op1C != null && op1C.isNullValue())
            return op0;
        SDValue rxor = reassociateOps(ISD.XOR, op0, op1);
        if (rxor.getNode() != null)
            return rxor;

        // fold !(x cc y) -> (x !cc y)
        SDValue[] res = new SDValue[3];
        if (op1C != null && op1C.getAPIntValue().eq(1) && isSetCCEquivalent(op0, res))
        {
            boolean isInt = res[0].getValueType().isInteger();
            CondCode notCC = ISD.getSetCCInverse(((CondCodeSDNode)res[2].getNode()).getCondition(),
                    isInt);
            if (!legalOprations || tli.isCondCodeLegal(notCC, res[0].getValueType()))
            {
                switch (op0.getOpcode())
                {
                    default:
                        Util.shouldNotReachHere("Unhandled SetCC Equivalent");
                        break;
                    case ISD.SETCC:
                        return dag.getSetCC(vt, res[0], res[1], notCC);
                    case ISD.SELECT_CC:
                        return dag.getSelectCC(res[0], res[1], op0.getOperand(2),
                                op0.getOperand(3), notCC);
                }
            }
        }

        // fold (not (zext (setcc x, y))) -> (zext (not (setcc x, y)))
        if (op1C != null && op1C.getAPIntValue().eq(1) &&
                op0.getOpcode() == ISD.ZERO_EXTEND &&
                op0.hasOneUse() && isSetCCEquivalent(op0.getOperand(0), res))
        {
            SDValue v = op0.getOperand(0);
            v = dag.getNode(ISD.XOR, v.getValueType(), v,
                    dag.getConstant(1, v.getValueType(), false));
            addToWorkList(v.getNode());
            return dag.getNode(ISD.ZERO_EXTEND, vt, v);
        }

        // fold (not (or x, y)) -> (and (not x), (not y)) iff x or y are setcc
        if (op1C != null && op1C.getAPIntValue().eq(1) && vt.getSimpleVT().simpleVT == MVT.i1 &&
                (op0.getOpcode() == ISD.OR || op0.getOpcode() == ISD.AND))
        {
            SDValue lhs = op0.getOperand(0), rhs = op0.getOperand(1);
            if (isOneUseSetCC(rhs) || isOneUseSetCC(rhs))
            {
                int newOpc = op0.getOpcode() == ISD.AND ? ISD.OR : ISD.AND;
                lhs = dag.getNode(ISD.XOR, vt, lhs, op1);
                rhs = dag.getNode(ISD.XOR, vt, rhs, op1);
                addToWorkList(lhs.getNode());
                addToWorkList(rhs.getNode());
                return dag.getNode(newOpc, vt, lhs, rhs);
            }
        }

        // fold (not (or x, y)) -> (and (not x), (not y)) iff x or y are constants
        if (op1C != null && op1C.isAllOnesValue() &&
                (op0.getOpcode() == ISD.OR || op1.getOpcode() == ISD.AND))
        {
            SDValue lhs = op0.getOperand(0), rhs = op0.getOperand(1);
            if (rhs.getNode() instanceof ConstantSDNode ||
                    lhs.getNode() instanceof ConstantSDNode)
            {
                int newOpc = op0.getOpcode() == ISD.AND? ISD.OR : ISD.AND;
                lhs = dag.getNode(ISD.XOR, vt, lhs, op1);
                rhs = dag.getNode(ISD.XOR, vt, rhs, op1);
                addToWorkList(lhs.getNode());
                addToWorkList(rhs.getNode());
                return dag.getNode(newOpc, vt, lhs, rhs);
            }
        }
        // fold (xor (xor x, c1), c2) -> (xor x, (xor c1, c2))
        if (op1C != null && op0.getOpcode() == ISD.XOR)
        {
            ConstantSDNode op00C = op0.getOperand(0).getNode() instanceof ConstantSDNode
                    ? (ConstantSDNode)op0.getOperand(0).getNode() : null;
            ConstantSDNode op01C = op0.getOperand(1).getNode() instanceof ConstantSDNode
                    ? (ConstantSDNode)op0.getOperand(1).getNode() : null;
            if (op00C != null)
            {
                dag.getNode(ISD.XOR, vt, op0.getOperand(1),
                        dag.getConstant(op1C.getAPIntValue().xor(op00C.getAPIntValue()),
                                vt, false));
            }
            if (op01C != null)
            {
                dag.getNode(ISD.XOR, vt, op0.getOperand(0),
                        dag.getConstant(op1C.getAPIntValue().xor(op01C.getAPIntValue()),
                                vt, false));
            }
        }
        // fold (xor x, x) -> 0
        if (op0.equals(op1))
        {
            if (!vt.isVector())
                return dag.getConstant(0, vt, false);
            else if (!legalOprations || tli.isOperationLegal(ISD.BUILD_VECTOR, vt))
            {
                SDValue el = dag.getConstant(0, vt.getVectorElementType(), false);
                SDValue[] ops = new SDValue[vt.getVectorNumElements()];
                Arrays.fill(ops, el);
                return dag.getNode(ISD.BUILD_VECTOR, vt, ops);
            }
        }

        // Simplify: xor (op x...), (op y...)  -> (op (xor x, y))
        if (op0.getOpcode() == op1.getOpcode())
        {
            SDValue tmp = simplifyBinOpWithSamOpcodeHands(n);
            if (tmp.getNode() != null)
                return tmp;
        }
        if (!vt.isVector() && simplifyDemandedBits(new SDValue(n, 0)))
            return new SDValue(n, 0);

        return new SDValue();
    }

    private boolean simplifyDemandedBits(SDValue op)
    {
        APInt demanded = APInt.getAllOnesValue(op.getValueSizeInBits());
        return simplifyDemandedBits(op, demanded);
    }

    private boolean simplifyDemandedBits(SDValue op, APInt demanded)
    {
        TargetLoweringOpt tlo = new TargetLoweringOpt(dag);
        APInt[] res = new APInt[2];
        if (!tli.simplifyDemandedBits(op, demanded, tlo, res, 0))
            return false;

        addToWorkList(op.getNode());
        commitTargetLoweringOpt(tlo);
        return true;
    }

    private SDValue simplifyBinOpWithSamOpcodeHands(SDNode n)
    {
        SDValue n0 = n.getOperand(0), n1 = n.getOperand(1);
        EVT vt = n0.getValueType();
        assert n0.getOpcode() == n1.getOpcode():"Bad input!";

        // For each of OP in AND/OR/XOR:
        // fold (OP (zext x), (zext y)) -> (zext (OP x, y))
        // fold (OP (sext x), (sext y)) -> (sext (OP x, y))
        // fold (OP (aext x), (aext y)) -> (aext (OP x, y))
        // fold (OP (trunc x), (trunc y)) -> (trunc (OP x, y)) (if trunc isn't free)
        if ((n0.getOpcode() == ISD.ZERO_EXTEND ||
                n0.getOpcode() == ISD.ANY_EXTEND ||
                n0.getOpcode() == ISD.SIGN_EXTEND ||
                (n0.getOpcode() == ISD.TRUNCATE &&
                !tli.isTruncateFree(n0.getOperand(0).getValueType(), vt))) &&
                n0.getOperand(0).getValueType().equals(n1.getOperand(0).getValueType()) &&
                (!legalOprations || tli.isOperationLegal(n.getOpcode(),
                        n0.getOperand(0).getValueType())))
        {
            SDValue orNode = dag.getNode(n.getOpcode(), n0.getOperand(0).getValueType(),
                    n0.getOperand(0), n1.getOperand(0));
            addToWorkList(orNode.getNode());
            return dag.getNode(n0.getOpcode(), vt, orNode);
        }

        // For each of OP in SHL/SRL/SRA/AND...
        //   fold (and (OP x, z), (OP y, z)) -> (OP (and x, y), z)
        //   fold (or  (OP x, z), (OP y, z)) -> (OP (or  x, y), z)
        //   fold (xor (OP x, z), (OP y, z)) -> (OP (xor x, y), z)
        if ((n0.getOpcode() == ISD.SHL || n0.getOpcode() == ISD.SRL ||
                n0.getOpcode() == ISD.SRA || n0.getOpcode() == ISD.AND) &&
                n0.getOperand(1).equals(n1.getOperand(1)))
        {
            SDValue orNode = dag.getNode(n.getOpcode(), n0.getOperand(0).getValueType(),
                    n0.getOperand(0), n1.getOperand(0));
            addToWorkList(orNode.getNode());
            return dag.getNode(n0.getOpcode(), vt, orNode, n0.getOperand(1));
        }
        return new SDValue();
    }

    private static boolean isOneUseSetCC(SDValue n)
    {
        SDValue[] res = new SDValue[3];
        return isSetCCEquivalent(n, res) && n.hasOneUse();
    }

    private SDValue reassociateOps(int opc, SDValue n0, SDValue n1)
    {
        EVT vt = n0.getValueType();
        if (n0.getOpcode() == opc && n0.getOperand(1).getNode() instanceof ConstantSDNode)
        {
            if (n1.getNode() instanceof ConstantSDNode)
            {
                // reassoc. (op (op x, c1), c2) -> (op x, (op c1, c2))
                SDValue opNode = dag.foldArithmetic(opc, vt,
                        (ConstantSDNode)n0.getOperand(1).getNode(),
                        (ConstantSDNode)n1.getNode());
                return dag.getNode(opc, vt, n0.getOperand(0), opNode);
            }
            else if (n0.hasOneUse())
            {
                // reassoc. (op (op x, c1), y) -> (op (op x, y), c1) iff x+c1 has one use
                SDValue opNode = dag.getNode(opc, vt, n0.getOperand(0), n1);
                addToWorkList(opNode.getNode());
                return dag.getNode(opc, vt, opNode, n0.getOperand(1));
            }
        }

        if (n1.getOpcode() == opc && n1.getOperand(1).getNode() instanceof ConstantSDNode)
        {
            if (n0.getNode() instanceof ConstantSDNode)
            {
                // reassoc. (op c2, (op x, c1)) -> (op x, (op c1, c2))
                SDValue opNode = dag.foldConstantArithmetic(opc, vt,
                        (ConstantSDNode)n1.getOperand(1).getNode(),
                        (ConstantSDNode)n0.getNode());
                return dag.getNode(opc, vt, n1.getOperand(0), opNode);
            }
            else if (n1.hasOneUse())
            {
                // reassoc. (op y, (op x, c1)) -> (op (op x, y), c1) iff x+c1 has one use
                SDValue opNode = dag.getNode(opc, vt, n1.getOperand(0), n0);
                addToWorkList(opNode.getNode());
                return dag.getNode(opc, vt, opNode, n1.getOperand(1));
            }
        }
        return new SDValue();
    }

    private static boolean isSetCCEquivalent(SDValue n, SDValue[] res)
    {
        if (n.getOpcode() == ISD.SETCC)
        {
            for (int i = 0; i < 3; i++)
                res[i] = n.getOperand(i);
            return true;
        }
        if (n.getOpcode() == ISD.SELECT_CC &&
                n.getOperand(2).getOpcode() == ISD.Constant &&
                n.getOperand(3).getOpcode() == ISD.Constant &&
                ((ConstantSDNode)n.getOperand(2).getNode()).getAPIntValue().eq(1) &&
                ((ConstantSDNode)n.getOperand(3).getNode()).isNullValue())
        {
            res[0] = n.getOperand(0);
            res[1] = n.getOperand(1);
            res[2] = n.getOperand(4);
            return true;
        }
        return false;
    }

    private SDValue visitOR(SDNode n)
    {
        SDValue n0 = n.getOperand(0);
        SDValue n1 = n.getOperand(1);
        ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n0.getNode():null;
        ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n1.getNode():null;
        EVT vt = n1.getValueType();
        int bitwidth = vt.getSizeInBits();

        if (vt.isVector())
        {
            SDValue foldedVOp = simplifyVBinOp(n);
            if (foldedVOp.getNode() != null) return foldedVOp;
        }
        // fold (or x, undef) -> -1
        if (n0.getOpcode() == ISD.UNDEF || n1.getOpcode() == ISD.UNDEF)
            return dag.getConstant(APInt.getAllOnesValue(bitwidth), vt, false);
        // fold (or c0, c1) -> c0|c1
        if (c1 != null && c2 != null)
            return dag.foldConstantArithmetic(ISD.OR, vt, c1, c2);
        // fold (or x, 0) -> x
        if (c2 != null && c2.isNullValue())
            return n0;

        // fold (or x, -1) -> -1
        if (c2 != null && c2.isAllOnesValue())
            return n1;

        // fold (or x, c) -> c iff (x & ~c) == 0
        if (c2 != null && dag.maskedValueIsZero(n0, c2.getAPIntValue().not()))
            return n1;
        // reassociate or
        SDValue res = reassociateOps(ISD.OR, n0, n1);
        if (res.getNode() != null)
            return res;

        // Canonicalize (or (and X, c1), c2) -> (and (or X, c2), c1|c2)
        if (c2 != null && n0.getOpcode() == ISD.AND &&
                n0.getOperand(1).getNode() instanceof ConstantSDNode)
        {
            ConstantSDNode c = (ConstantSDNode)n0.getOperand(1).getNode();
            SDValue or = dag.getNode(ISD.OR, vt, n0.getOperand(0), n1);
            return dag.getNode(ISD.AND, vt, or,
                    dag.foldConstantArithmetic(ISD.OR, vt, c, c2));
        }

        // fold (or (setcc x), (setcc y)) -> (setcc (or x, y))
        SDValue[] setcc1 = new SDValue[3];
        SDValue[] setcc2 = new SDValue[3];
        if (isSetCCEquivalent(n0, setcc1) && isSetCCEquivalent(n1, setcc2))
        {
            CondCode cc1 = ((CondCodeSDNode)setcc1[2].getNode()).getCondition();
            CondCode cc2 = ((CondCodeSDNode)setcc2[2].getNode()).getCondition();
            SDValue ll = setcc1[0];
            SDValue lr = setcc1[1];
            SDValue rl = setcc2[0];
            SDValue rr = setcc2[1];
            if (lr.equals(rr) && lr.getNode() instanceof ConstantSDNode &&
                    cc1.equals(cc2) &&
                    ll.getValueType().isInteger())
            {
                // fold (or (setne X, 0), (setne Y, 0)) -> (setne (or X, Y), 0)
                // fold (or (setlt X, 0), (setlt Y, 0)) -> (setne (or X, Y), 0)
                if (((ConstantSDNode)lr.getNode()).isNullValue() &&
                        (cc2 == CondCode.SETNE || cc2 == CondCode.SETLT))
                {
                    SDValue or = dag.getNode(ISD.OR, lr.getValueType(), ll, rl);
                    addToWorkList(or.getNode());
                    return dag.foldSetCC(vt, or, lr, cc2);
                }
                // fold (or (setne X, -1), (setne Y, -1)) -> (setne (and X, Y), -1)
                // fold (or (setgt X, -1), (setgt Y  -1)) -> (setgt (and X, Y), -1)
                if (((ConstantSDNode)lr.getNode()).isAllOnesValue() &&
                        (cc2 == CondCode.SETNE || cc2 == CondCode.SETGT))
                {
                    SDValue and = dag.getNode(ISD.AND, lr.getValueType(), ll, rl);
                    addToWorkList(and.getNode());
                    return dag.getSetCC(vt, and, lr, cc2);
                }
            }
            if (ll.equals(rr) && lr.equals(rl))
            {
                cc2 = ISD.getSetCCSwappedOperands(cc2);
                SDValue t = rl;
                rl = rr;
                rr = t;
            }
            if (ll.equals(rl) && lr.equals(rr))
            {
                boolean isInteger = ll.getValueType().isInteger();
                CondCode result = ISD.getSetCCOrOperation(cc1, cc2, isInteger);
                if (result != CondCode.SETCC_INVALID &&
                        (!legalOprations || tli.isCondCodeLegal(result, ll.getValueType())))
                {
                    return dag.getSetCC(n0.getValueType(), ll, lr, result);
                }
            }
        }
        // Simplify: (or (op x...), (op y...))  -> (op (or x, y))
        if (n0.getOpcode() == n1.getOpcode())
        {
            SDValue tmp = simplifyBinOpWithSamOpcodeHands(n);
            if (tmp.getNode() != null) return tmp;
        }
        // (or (and X, C1), (and Y, C2))  -> (and (or X, Y), C3) if possible.
        if (n0.getOpcode() == n1.getOpcode() && n0.getOpcode() == ISD.AND &&
                n0.getOperand(1).getNode() instanceof ConstantSDNode &&
                n1.getOperand(1).getNode() instanceof ConstantSDNode &&
                (n0.hasOneUse() || n1.hasOneUse()))
        {
            APInt lhsMask = ((ConstantSDNode) n0.getOperand(1).getNode()).getAPIntValue();
            APInt rhsMask = ((ConstantSDNode) n1.getOperand(1).getNode()).getAPIntValue();
            if (dag.maskedValueIsZero(n0.getOperand(0), rhsMask.and(lhsMask.not())) &&
                    dag.maskedValueIsZero(n1.getOperand(0), lhsMask.and(rhsMask.not())))
            {
                SDValue x = dag.getNode(ISD.OR, vt, n0.getOperand(0), n1.getOperand(0));
                return dag.getNode(ISD.AND, vt, x, dag.getConstant(lhsMask.or(rhsMask), vt, false));
            }
        }
        // See if this is some rotate idiom.
        SDNode t = matchRotate(n0, n1);
        if (t != null)
            return new SDValue(t, 0);
        return new SDValue();
    }

    private SDNode matchRotate(SDValue lhs, SDValue rhs)
    {
        EVT vt = lhs.getValueType();
        if (!tli.isTypeLegal(vt)) return null;

        boolean hasROTL = tli.isOperationLegalOrCustom(ISD.ROTL, vt);
        boolean hasROTR = tli.isOperationLegalOrCustom(ISD.ROTR, vt);
        if (!hasROTL && !hasROTR) return null;

        SDValue[] lhsRes = new SDValue[2];
        if (!matchRotateHalf(lhs, lhsRes))
            return null;

        SDValue[] rhsRes = new SDValue[2];
        if (matchRotateHalf(rhs, rhsRes))
            return null;

        if (!lhsRes[0].getOperand(0).equals(rhsRes[0].getOperand(0)))
            return null;

        if (lhsRes[0].getOpcode() == rhsRes[0].getOpcode())
            return null;

        if (rhsRes[0].getOpcode() == ISD.SHL)
        {
            SDValue t = lhs;
            lhs = rhs;
            rhs = t;
            t = lhsRes[0];
            lhsRes[0] = rhsRes[0];
            lhsRes[0] = t;

            t = lhsRes[1];
            lhsRes[1] = rhsRes[1];
            rhsRes[1] = t;
        }

        int opSizeInBits = vt.getSizeInBits();
        SDValue lhsShiftArg = lhsRes[0].getOperand(0);
        SDValue lhsShiftAmt = lhsRes[0].getOperand(1);
        SDValue rhsShiftAmt = rhsRes[0].getOperand(1);

        // fold (or (shl x, C1), (srl x, C2)) -> (rotl x, C1)
        // fold (or (shl x, C1), (srl x, C2)) -> (rotr x, C2)
        if (lhsShiftAmt.getOpcode() == ISD.Constant &&
                rhsShiftAmt.getOpcode() == ISD.Constant)
        {
            long lshVal = ((ConstantSDNode)lhsShiftAmt.getNode()).getZExtValue();
            long rshVal = ((ConstantSDNode)rhsShiftAmt.getNode()).getZExtValue();
            if ((lshVal + rshVal) != opSizeInBits)
                return null;

            SDValue rot;
            if (hasROTL)
                rot = dag.getNode(ISD.ROTL, vt, lhsShiftArg, lhsShiftAmt);
            else
                rot = dag.getNode(ISD.ROTR, vt, lhsShiftArg, rhsShiftAmt);

            if (lhsRes[1].getNode() != null || rhsRes[1].getNode() != null)
            {
                APInt mask = APInt.getAllOnesValue(opSizeInBits);
                if (lhsRes[1].getNode() != null)
                {
                    APInt rhsBits = APInt.getLowBitsSet(opSizeInBits, (int) lshVal);
                    mask.andAssign(((ConstantSDNode)lhsRes[1].getNode()).getAPIntValue().or(rhsBits));
                }
                if (rhsRes[1].getNode() != null)
                {
                    APInt lhsBits = APInt.getHighBitsSet(opSizeInBits, (int) rshVal);
                    mask.andAssign(((ConstantSDNode)rhsRes[1].getNode()).getAPIntValue().or(lhsBits));
                }
                rot = dag.getNode(ISD.AND, vt, rot, dag.getConstant(mask, vt, false));
            }
            return rot.getNode();
        }

        // If there is a mask here, and we have a variable shift, we can't be sure
        // that we're masking out the right stuff.
        if (lhsRes[1].getNode() != null || rhsRes[1].getNode() != null)
            return null;

        // fold (or (shl x, y), (srl x, (sub 32, y))) -> (rotl x, y)
        // fold (or (shl x, y), (srl x, (sub 32, y))) -> (rotr x, (sub 32, y))
        if (rhsShiftAmt.getOpcode() == ISD.SUB &&
                lhsShiftAmt.equals(rhsShiftAmt.getOperand(1)))
        {
            if (rhsShiftAmt.getOperand(0).getNode() instanceof ConstantSDNode)
            {
                ConstantSDNode c = (ConstantSDNode)rhsShiftAmt.getOperand(0).getNode();
                if (c.getAPIntValue().eq(opSizeInBits))
                {
                    if (hasROTL)
                        return dag.getNode(ISD.ROTL, vt, lhsShiftArg, lhsShiftAmt).getNode();
                    else
                        return dag.getNode(ISD.ROTR, vt, lhsShiftArg, rhsShiftAmt).getNode();
                }
            }
        }

        // fold (or (shl x, (sub 32, y)), (srl x, r)) -> (rotr x, y)
        // fold (or (shl x, (sub 32, y)), (srl x, r)) -> (rotl x, (sub 32, y))
        if (lhsShiftAmt.getOpcode() == ISD.SUB &&
                rhsShiftAmt.equals(lhsShiftAmt.getOperand(1)))
        {
            if (lhsShiftAmt.getOperand(0).getNode() instanceof ConstantSDNode)
            {
                ConstantSDNode c = (ConstantSDNode)lhsShiftAmt.getOperand(0).getNode();
                if (c.getAPIntValue().eq(opSizeInBits))
                {
                    if (hasROTR)
                        return dag.getNode(ISD.ROTR, vt, lhsShiftArg, rhsShiftAmt).getNode();
                    else
                        return dag.getNode(ISD.ROTR, vt, lhsShiftArg, lhsShiftAmt).getNode();
                }
            }
        }

        // Look for sign/zext/any-extended or truncate cases:
        if ((lhsShiftAmt.getOpcode() == ISD.SIGN_EXTEND ||
                lhsShiftAmt.getOpcode() == ISD.ZERO_EXTEND ||
                lhsShiftAmt.getOpcode() == ISD.ANY_EXTEND ||
                lhsShiftAmt.getOpcode() == ISD.TRUNCATE) &&
                (rhsShiftAmt.getOpcode() == ISD.SIGN_EXTEND ||
                rhsShiftAmt.getOpcode() == ISD.ZERO_EXTEND ||
                rhsShiftAmt.getOpcode() == ISD.ANY_EXTEND ||
                rhsShiftAmt.getOpcode() == ISD.TRUNCATE))
        {
            SDValue lextOp0 = lhsShiftAmt.getOperand(0);
            SDValue rextOp0 = rhsShiftAmt.getOperand(0);
            if (rextOp0.getOpcode() == ISD.SUB &&
                    rextOp0.getOperand(1).equals(lextOp0))
            {
                // fold (or (shl x, (*ext y)), (srl x, (*ext (sub 32, y)))) ->
                //   (rotl x, y)
                // fold (or (shl x, (*ext y)), (srl x, (*ext (sub 32, y)))) ->
                //   (rotr x, (sub 32, y))
                if (rextOp0.getOperand(0).getNode() instanceof ConstantSDNode)
                {
                    ConstantSDNode c = (ConstantSDNode)rextOp0.getOperand(0).getNode();
                    if (c.getAPIntValue().eq(opSizeInBits))
                    {
                        return dag.getNode(hasROTL ? ISD.ROTL : ISD.ROTR, vt, lhsShiftArg,
                                hasROTL ? lhsShiftAmt:rhsShiftAmt).getNode();
                    }
                }
            }
            else if (lextOp0.getOpcode() == ISD.SUB &&
                    rextOp0.equals(lextOp0.getOperand(1)))
            {
                // fold (or (shl x, (*ext (sub 32, y))), (srl x, (*ext y))) ->
                //   (rotr x, y)
                // fold (or (shl x, (*ext (sub 32, y))), (srl x, (*ext y))) ->
                //   (rotl x, (sub 32, y))
                if (lextOp0.getOperand(0).getNode() instanceof ConstantSDNode)
                {
                    ConstantSDNode c = (ConstantSDNode)lextOp0.getOperand(0).getNode();
                    if (c.getAPIntValue().eq(opSizeInBits))
                    {
                        return dag.getNode(hasROTR ? ISD.ROTR : ISD.ROTL, vt, lhsShiftArg,
                                hasROTR ? rhsShiftAmt:lhsShiftAmt).getNode();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Match "(X shl/srl V1) & V2" where V2 may not be present.
     * @param op
     * @param res   res[0] represents shift and res[1] represents mask.
     * @return
     */
    private static boolean matchRotateHalf(SDValue op, SDValue[] res)
    {
        assert res != null && res.length == 2;
        if (op.getOpcode() == ISD.AND)
        {
            SDValue rhs = op.getOperand(1);
            if (rhs.getNode() instanceof ConstantSDNode)
            {
                res[1] = rhs;
                op = op.getOperand(0);
            }
            else
                return false;
        }
        if (op.getOpcode() == ISD.SHL || op.getOpcode() == ISD.SRL)
        {
            res[0] = op;
            return true;
        }
        return false;
    }

    private SDValue visitAND(SDNode n)
    {
        SDValue op0 = n.getOperand(0);
        SDValue op1 = n.getOperand(1);
        ConstantSDNode c0 = op0.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)op0.getNode():null;
        ConstantSDNode c1 = op1.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)op1.getNode():null;
        EVT vt = op1.getValueType();
        int bitwidth = vt.getSizeInBits();

        if (vt.isVector())
        {
            SDValue foldedVOp = simplifyVBinOp(n);
            if (foldedVOp.getNode() != null) return foldedVOp;
        }

        if (op0.getOpcode() == ISD.UNDEF || op1.getOpcode() == ISD.UNDEF)
        {
            return dag.getConstant(0, vt, false);
        }
        if (c0 != null && c1 != null)
            return dag.foldConstantArithmetic(ISD.AND, vt, c0, c1);
        if (c0 != null && c1 == null)
            return dag.getNode(ISD.AND, vt, op1, op0);
        if (c1 != null && c1.isAllOnesValue())
            return op0;
        if (c1 != null && dag.maskedValueIsZero(new SDValue(n, 0),
                APInt.getAllOnesValue(bitwidth)))
            return dag.getConstant(0, vt, false);
        SDValue rand = reassociateOps(ISD.AND, op0, op1);
        if (rand.getNode() != null)
            return rand;
        // fold (and (or x, 0xFFFF), 0xFF) -> 0xFF
        if (c1 != null && op0.getOpcode() == ISD.OR)
        {
            if (op0.getOperand(1).getNode() instanceof ConstantSDNode)
            {
                ConstantSDNode op01C = (ConstantSDNode)op0.getOperand(1).getNode();
                if (op01C.getAPIntValue().and(c1.getAPIntValue()).eq(c1.getAPIntValue()))
                    return op1;
            }
        }

        // fold (and (any_ext V), c) -> (zero_ext V) if 'and' only clears top bits.
        if (c1 != null && op0.getOpcode() == ISD.ANY_EXTEND)
        {
            SDValue op00 = op0.getOperand(0);
            APInt mask = c1.getAPIntValue().not();
            mask.trunc(op00.getValueSizeInBits());
            if (dag.maskedValueIsZero(op00, mask))
            {
                SDValue zext = dag.getNode(ISD.ZERO_EXTEND, op0.getValueType(),
                        op00);
                combineTo(n, zext, true);
                combineTo(op0.getNode(), zext, true);
                return new SDValue(n, 0);
            }
        }
        // fold (and (setcc x), (setcc y)) -> (setcc (and x, y))
        SDValue[] setccRes = new SDValue[3], setccRes2 = new SDValue[3];
        if (isSetCCEquivalent(op0, setccRes) && isSetCCEquivalent(op1, setccRes2))
        {
            CondCode cc1 = ((CondCodeSDNode)setccRes[2].getNode()).getCondition();
            CondCode cc2 = ((CondCodeSDNode)setccRes2[2].getNode()).getCondition();
            if (setccRes[1].equals(setccRes2[1]) &&
                    setccRes[1].getNode() instanceof ConstantSDNode &&
                    setccRes[0].getValueType().isInteger())
            {
                // fold (and (seteq X, 0), (seteq Y, 0)) -> (seteq (or X, Y), 0)
                if (((ConstantSDNode)setccRes[1].getNode()).isNullValue() &&
                        cc2 == CondCode.SETEQ)
                {
                    SDValue orNode = dag.getNode(ISD.OR, setccRes[1].getValueType(),
                            setccRes[0], setccRes2[0]);
                    addToWorkList(orNode.getNode());
                    return dag.getSetCC(vt, orNode, setccRes[1], cc2);
                }
                // fold (and (seteq X, -1), (seteq Y, -1)) -> (seteq (and X, Y), -1)
                if (((ConstantSDNode)setccRes[1].getNode()).isAllOnesValue() &&
                        cc2 == CondCode.SETEQ)
                {
                    SDValue andNode = dag.getNode(ISD.AND, setccRes[1].getValueType(),
                            setccRes[0], setccRes2[0]);
                    addToWorkList(andNode.getNode());
                    return dag.getSetCC(vt, andNode, setccRes[1], cc2);
                }
                // fold (and (setgt X,  -1), (setgt Y,  -1)) -> (setgt (or X, Y), -1)
                if (((ConstantSDNode)setccRes[1].getNode()).isAllOnesValue() &&
                        cc2 == CondCode.SETGT)
                {
                    SDValue orNode = dag.getNode(ISD.OR, setccRes[1].getValueType(),
                            setccRes[0], setccRes2[0]);
                    addToWorkList(orNode.getNode());
                    return dag.getSetCC(vt, orNode, setccRes[1], cc2);
                }
            }

            // canonicalize equivalent to ll == rl
            if (setccRes[0].equals(setccRes2[1]) && setccRes[1].equals(setccRes2[0]))
            {
                cc2 = ISD.getSetCCSwappedOperands(cc2);
                SDValue t = setccRes2[0];
                setccRes2[0] = setccRes2[1];
                setccRes2[1] = t;
            }
            if (setccRes[0].equals(setccRes2[0]) && setccRes[1].equals(setccRes2[1]))
            {
                boolean isInteger = setccRes[0].getValueType().isInteger();
                CondCode result = ISD.getSetCCAndOperation(cc1,cc2, isInteger);
                if (result != CondCode.SETCC_INVALID &&
                        (!legalOprations || tli.isCondCodeLegal(result, setccRes[0].getValueType())))
                    return dag.getSetCC(op0.getValueType(), setccRes[0], setccRes[1], result);
            }
        }

        // Simplify: (and (op x...), (op y...))  -> (op (and x, y))
        if (op0.getOpcode() == op1.getOpcode())
        {
            SDValue tmp = simplifyBinOpWithSamOpcodeHands(n);
            if (tmp.getNode() != null) return tmp;
        }

        if (!vt.isVector() && simplifyDemandedBits(new SDValue(n, 0)))
            return new SDValue(n, 0);

        // fold (zext_inreg (extload x)) -> (zextload x)
        if (op0.getNode().isExtLoad() && op0.getNode().isUNINDEXEDLoad())
        {
            LoadSDNode ld = (LoadSDNode)op0.getNode();
            EVT evt = ld.getMemoryVT();
            bitwidth = op1.getValueSizeInBits();
            if (dag.maskedValueIsZero(op1, APInt.getHighBitsSet(bitwidth,
                    bitwidth - evt.getSizeInBits())) &&
                    ((!legalOprations && !ld.isVolatile()) ||
                    tli.isLoadExtLegal(LoadExtType.ZEXTLOAD, evt)))
            {
                SDValue extLd = dag.getExtLoad(LoadExtType.ZEXTLOAD,
                        vt, ld.getChain(), ld.getBasePtr(),
                        ld.getSrcValue(), ld.getSrcValueOffset(),
                        evt, ld.isVolatile(), ld.getAlignment());
                addToWorkList(n);
                combineTo(op0.getNode(), extLd, extLd.getValue(1), true);
                return new SDValue(n, 0);
            }
        }

        // fold (and (load x), 255) -> (zextload x, i8)
        // fold (and (extload x, i16), 255) -> (zextload x, i8)
        if (c1 != null && op0.getOpcode() == ISD.LOAD)
        {
            LoadSDNode ld = (LoadSDNode)op0.getNode();
            if (ld.getExtensionType() == LoadExtType.SEXTLOAD &&
                    ld.isUnindexed() && op0.hasOneUse() &&
                    !ld.isVolatile())
            {
                EVT extVT = new EVT(MVT.Other);
                int activeBits = c1.getAPIntValue().getActiveBits();
                if (activeBits > 0 && APInt.isMask(activeBits, c1.getAPIntValue()))
                {
                    extVT = EVT.getIntegerVT(activeBits);
                }
                EVT loadedVT = ld.getMemoryVT();
                if (!extVT.equals(new EVT(MVT.Other)) && loadedVT.bitsGT(extVT) &&
                        extVT.isRound() && (!legalOprations ||
                        tli.isLoadExtLegal(LoadExtType.ZEXTLOAD, extVT)))
                {
                    EVT ptrTy = op0.getOperand(1).getValueType();
                    int lvtStoreBytes = loadedVT.getStoreSizeInBits()/8;
                    int evtStoreBytes = extVT.getStoreSizeInBits()/8;
                    int ptrOff = lvtStoreBytes - evtStoreBytes;
                    int alignment = ld.getAlignment();
                    SDValue newPtr = ld.getBasePtr();
                    if (tli.isBigEndian())
                    {
                        newPtr = dag.getNode(ISD.ADD, ptrTy, newPtr,
                                dag.getConstant(ptrOff, ptrTy, false));
                        alignment = Util.minAlign(alignment, ptrOff);
                    }
                    addToWorkList(newPtr.getNode());
                    SDValue load = dag.getExtLoad(LoadExtType.ZEXTLOAD,
                            vt, ld.getChain(), newPtr, ld.getSrcValue(),
                            ld.getSrcValueOffset(), extVT, ld.isVolatile(),
                            alignment);
                    addToWorkList(n);
                    combineTo(op0.getNode(), load, load.getValue(1),true);
                    return new SDValue(n, 0);
                }
            }
        }
        return new SDValue();
    }

    private SDValue visitUDIVREM(SDNode n)
    {
        SDValue res = simplifyNodeWithTwoResults(n, ISD.UDIV, ISD.UREM);
        if (res.getNode() != null) return res;
        return new SDValue();
    }

    private SDValue visitSDIVREM(SDNode n)
    {
        SDValue res = simplifyNodeWithTwoResults(n, ISD.SDIV, ISD.SREM);
        if (res.getNode() != null) return res;
        return new SDValue();
    }

    private SDValue visitUMUL_LOHI(SDNode n)
    {
        SDValue res = simplifyNodeWithTwoResults(n, ISD.MUL, ISD.MULHU);
        if (res.getNode() != null) return res;
        return new SDValue();
    }

    private SDValue simplifyNodeWithTwoResults(SDNode n, int loOp, int hiOp)
    {
        boolean hiExists = n.hasAnyUseOfValue(1);
        if (!hiExists && (!legalOprations || tli.isOperationLegal(loOp, n.getValueType(0))))
        {
            SDValue res = dag.getNode(loOp, n.getValueType(0), n.getOperandList());
            return combineTo(n ,res, res, true);
        }

        boolean loExists = n.hasAnyUseOfValue(0);
        if (!loExists && (!legalOprations || tli.isOperationLegal(hiOp, n.getValueType(1))))
        {
            SDValue res = dag.getNode(hiOp, n.getValueType(1), n.getOperandList());
            return combineTo(n, res, res, true);
        }

        if (loExists && hiExists)
            return new SDValue();

        if (loExists)
        {
            SDValue lo = dag.getNode(loOp, n.getValueType(0), n.getOperandList());
            addToWorkList(lo.getNode());
            SDValue loOpt = combine(lo.getNode());
            if (loOpt.getNode() != null && !loOpt.getNode().equals(lo.getNode()) &&
                    (!legalOprations || tli.isOperationLegal(loOpt.getOpcode(), loOpt.getValueType())))
                return combineTo(n, loOpt, loOpt, true);
        }

        if (hiExists)
        {
            SDValue hi = dag.getNode(hiOp, n.getValueType(1), n.getOperandList());
            addToWorkList(hi.getNode());
            SDValue hiOpt = combine(hi.getNode());
            if (hiOpt.getNode() != null && !hiOpt.equals(hi) &&
                    (!legalOprations || tli.isOperationLegal(hiOpt.getOpcode(), hiOpt.getValueType())))
                return combineTo(n, hiOpt, hiOpt, true);
        }
        return new SDValue();
    }

    private SDValue visitSMUL_LOHI(SDNode n)
    {
        SDValue res = simplifyNodeWithTwoResults(n, ISD.MUL, ISD.MULHS);
        if (res.getNode() != null) return res;
        return new SDValue();
    }

    private SDValue visitMULHS(SDNode n)
    {
        SDValue n0 = n.getOperand(0);
        SDValue n1 = n.getOperand(1);
        ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n0.getNode() : null;
        ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n1.getNode() : null;
        EVT vt = n0.getValueType();

        if (c2 != null && c2.isNullValue())
            return n1;
        if (c2 != null && c2.getAPIntValue().eq(1))
            return dag.getNode(ISD.SRA, n0.getValueType(), n0,
                    dag.getConstant(n0.getValueType().getSizeInBits()-1,
                            new EVT(tli.getShiftAmountTy()), false));

        if (n0.getOpcode() == ISD.UNDEF || n1.getOpcode() == ISD.UNDEF)
            return dag.getConstant(0, vt, false);

        return new SDValue();
    }

    private SDValue visitMULHU(SDNode n)
    {
        SDValue n0 = n.getOperand(0);
        SDValue n1 = n.getOperand(1);
        ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n0.getNode() : null;
        ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n1.getNode() : null;
        EVT vt = n0.getValueType();

        if (c2 != null && c2.isNullValue())
            return n1;
        if (c2 != null && c2.getAPIntValue().eq(1))
            return dag.getConstant(0, n0.getValueType(), false);

        if (n0.getOpcode() == ISD.UNDEF || n1.getOpcode() == ISD.UNDEF)
            return dag.getConstant(0, vt, false);

        return new SDValue();
    }

    private SDValue visitUREM(SDNode n)
    {
        SDValue n0 = n.getOperand(0);
        SDValue n1 = n.getOperand(1);
        ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n0.getNode() : null;
        ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n1.getNode() : null;
        EVT vt = n0.getValueType();

        if (c1 != null && c2 != null && !c2.isNullValue())
        {
            return dag.foldConstantArithmetic(ISD.UREM, vt, c1, c2);
        }
        if (c2 != null && !c2.isNullValue() && !c2.getAPIntValue().isPowerOf2())
            return dag.getNode(ISD.AND, vt, n0, dag.getConstant(c2.getAPIntValue().sub(1), vt, false));

        if (n1.getOpcode() == ISD.SHL && n1.getOperand(0).getNode() instanceof ConstantSDNode)
        {
            ConstantSDNode shc = (ConstantSDNode)n1.getOperand(0).getNode();
            if (shc.getAPIntValue().isPowerOf2())
            {
                SDValue add = dag.getNode(ISD.ADD, vt, n1,
                        dag.getConstant(APInt.getAllOnesValue(vt.getSizeInBits()), vt, false));
                addToWorkList(add.getNode());
                return dag.getNode(ISD.AND, vt, n0, add);
            }
        }

        if (c2 != null && !c2.isNullValue())
        {
            SDValue div = dag.getNode(ISD.UDIV, vt, n0, n1);
            addToWorkList(div.getNode());
            SDValue optimizedDiv = combine(div.getNode());
            if (optimizedDiv.getNode() != null &&
                    !optimizedDiv.getNode().equals(div.getNode()))
            {
                SDValue mul = dag.getNode(ISD.MUL, vt, optimizedDiv, n1);
                SDValue sub = dag.getNode(ISD.SUB, vt, n0, mul);
                addToWorkList(mul.getNode());
                return sub;
            }
        }

        if (n0.getOpcode() == ISD.UNDEF)
            return dag.getConstant(0, vt, false);
        if (n1.getOpcode() == ISD.UNDEF)
            return n1;
        return new SDValue();
    }

    private SDValue visitSREM(SDNode n)
    {
        SDValue n0 = n.getOperand(0);
        SDValue n1 = n.getOperand(1);
        ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n0.getNode() : null;
        ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n1.getNode() : null;
        EVT vt = n0.getValueType();
        if (c1 != null && c2 != null && !c2.isNullValue())
        {
            return dag.foldConstantArithmetic(ISD.SREM, vt, c1, c2);
        }
        if (!vt.isVector() && dag.signBitIsZero(n1, 0) && dag.signBitIsZero(n0, 0))
        {
            return dag.getNode(ISD.UREM, vt, n0, n1);
        }

        if (c2 != null && !c2.isNullValue())
        {
            SDValue div = dag.getNode(ISD.SDIV, vt, n0, n1);
            addToWorkList(div.getNode());
            SDValue optimizedDiv = combine(div.getNode());
            if (optimizedDiv.getNode() != null &&
                    !optimizedDiv.getNode().equals(div.getNode()))
            {
                SDValue mul = dag.getNode(ISD.MUL, vt, optimizedDiv, n1);
                SDValue sub = dag.getNode(ISD.SUB, vt, n0, mul);
                addToWorkList(mul.getNode());
                return sub;
            }
        }

        if (n0.getOpcode() == ISD.UNDEF)
            return dag.getConstant(0, vt, false);
        if (n1.getOpcode() == ISD.UNDEF)
            return n1;
        return new SDValue();
    }

    private SDValue visitUDIV(SDNode n)
    {
        SDValue n0 = n.getOperand(0);
        SDValue n1 = n.getOperand(1);
        ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n0.getNode() : null;
        ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n1.getNode() : null;
        EVT vt = n0.getValueType();

        if (vt.isVector())
        {
            SDValue foldedOp = simplifyVBinOp(n);
            if (foldedOp.getNode() != null) return foldedOp;
            return new SDValue();
        }
        // fold (udiv c1, c2) -> c1/c2
        if (c1 != null && c2 != null && !c2.isNullValue())
        {
            return dag.foldConstantArithmetic(ISD.UDIV, vt, c1, c2);
        }
        // fold (udiv x, (1 << c)) -> x >>u c
        if (c2 != null && c2.getAPIntValue().isPowerOf2())
        {
            return dag.getNode(ISD.SRL, vt, n0,
                    dag.getConstant(c2.getAPIntValue().logBase2(),
                            new EVT(tli.getShiftAmountTy()), false));
        }
        // fold (udiv x, (shl c, y)) -> x >>u (log2(c)+y) iff c is power of 2
        if (n1.getOpcode() == ISD.SHL &&
                n1.getOperand(0).getNode() instanceof ConstantSDNode)
        {
            ConstantSDNode c = (ConstantSDNode)n1.getOperand(0).getNode();
            if (c.getAPIntValue().isPowerOf2())
            {
                EVT addVT = n1.getOperand(1).getValueType();
                SDValue add = dag.getNode(ISD.ADD, addVT, n1.getOperand(1),
                        dag.getConstant(c.getAPIntValue().logBase2(),
                                addVT, false));
                return dag.getNode(ISD.SRL, vt, n0, add);
            }
        }

        // fold (udiv x, c) -> alternate
        if (c2 != null && !c2.isNullValue() && !tli.isIntDivCheap())
        {
            SDValue op = buildUDIV(n);
            if (op.getNode() != null) return op;
        }

        if (n0.getOpcode() == ISD.UNDEF)
            return dag.getConstant(0, vt, false);
        if (n1.getOpcode() == ISD.UNDEF)
            return n1;

        return new SDValue();
    }

    private SDValue buildUDIV(SDNode n)
    {
        ArrayList<SDNode> built = new ArrayList<>();
        SDValue s = tli.buildUDIV(n, dag, built);
        built.forEach(this::addToWorkList);
        return s;
    }

    private SDValue visitSDIV(SDNode n)
    {
        SDValue n0 = n.getOperand(0);
        SDValue n1 = n.getOperand(1);
        ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n0.getNode() : null;
        ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n1.getNode() : null;
        EVT vt = n0.getValueType();

        if (vt.isVector())
        {
            SDValue foldedOp = simplifyVBinOp(n);
            if (foldedOp.getNode() != null) return foldedOp;
            return new SDValue();
        }
        // fold (sdiv c1, c2) -> c1/c2
        if (c1 != null && !c2.isNullValue())
        {
            return dag.foldConstantArithmetic(ISD.SDIV, vt, c1, c2);
        }
        // fold (sdiv c1, 1) --> c1
        if (c2 != null && c2.getSExtValue() == 1)
            return n0;
        // fold (sdiv X, -1) -> 0-X
        if (c2 != null && c2.isAllOnesValue())
            return dag.getNode(ISD.SUB, vt, dag.getConstant(0, vt, false),
                    n0);
        // If we know the sign bits of both operands are zero, strength reduce to a
        // udiv instead.  Handles (X&15) /s 4 -> X&15 >> 2
        if (!vt.isVector())
        {
            if (dag.signBitIsZero(n1, 0) && dag.signBitIsZero(n0, 0))
                return dag.getNode(ISD.UDIV, n1.getValueType(), n0, n1);
        }
        // fold (sdiv X, pow2) -> simple ops after legalize
        if (c2 != null && !c2.isNullValue() && !tli.isIntDivCheap() &&
                (Util.isPowerOf2(c2.getSExtValue()) || Util.isPowerOf2(-c2.getSExtValue())))
        {
            if (tli.isIntDivCheap())
                 return new SDValue();

            long pow2 = c2.getSExtValue();
            long abs2 = pow2 > 0 ? pow2 : -pow2;
            int lg2 = Util.log2(abs2);
            SDValue sgn = dag.getNode(ISD.SRA, vt, n0, dag.getConstant(vt.getSizeInBits()-1,
                    new EVT(tli.getShiftAmountTy()), false));
            addToWorkList(sgn.getNode());

            // add (n0 < 0) ? abs2 - 1 : 0
            SDValue srl = dag.getNode(ISD.SRL, vt, sgn,
                    dag.getConstant(vt.getSizeInBits() - lg2,
                            new EVT(tli.getShiftAmountTy()), false));
            SDValue add = dag.getNode(ISD.ADD, vt, n0, srl);
            addToWorkList(srl.getNode());
            addToWorkList(add.getNode());
            SDValue sra = dag.getNode(ISD.SRA, vt, add,
                    dag.getConstant(lg2, new EVT(tli.getShiftAmountTy()), false));
            if (pow2 > 0)
                return sra;
            addToWorkList(sra.getNode());
            return dag.getNode(ISD.SUB, vt, dag.getConstant(0, vt, false), sra);
        }

        if (c2 != null && (c2.getSExtValue() < -1 || c2.getSExtValue() > 1) &&
                !tli.isIntDivCheap())
        {
            SDValue op = buildSDIV(n);
            if (op.getNode() != null) return op;
        }
        if (n0.getOpcode() == ISD.UNDEF)
            return dag.getConstant(0, vt, false);
        if (n1.getOpcode() == ISD.UNDEF)
            return n1;

        return new SDValue();
    }

    private SDValue buildSDIV(SDNode n)
    {
        ArrayList<SDNode> nodes = new ArrayList<>();
        SDValue s = tli.buildSDIV(n, dag, nodes);

        nodes.stream().forEach(op -> addToWorkList(op));
        return s;
    }

    private SDValue visitMUL(SDNode n)
    {
        SDValue n0 = n.getOperand(0);
        SDValue n1 = n.getOperand(1);
        ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n0.getNode() : null;
        ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n1.getNode() : null;
        EVT vt = n0.getValueType();

        if (vt.isVector())
        {
            SDValue foldedOp = simplifyVBinOp(n);
            if (foldedOp.getNode() != null) return foldedOp;
            return new SDValue();
        }

        // (mul x, undef) or (mul undef, x) --> 0
        if (n0.getOpcode() == ISD.UNDEF || n1.getOpcode() == ISD.UNDEF)
            return dag.getConstant(0, vt, false);
        // fold (mul c1, c2) --> c1*c2
        if (c1 != null && c2 != null)
            return dag.foldConstantArithmetic(ISD.MUL, vt, c1, c2);
        // reassociate the two operand and move the constant to the right.
        if (c1 != null && c2 == null)
            return dag.getNode(ISD.MUL, vt, n1, n0);
        // fold (mul x, 0) --> 0
        if (c2 != null && c2.isNullValue())
            return n1;
        // fold (mul x, -1) -> 0-x
        if (c2 != null && c2.isAllOnesValue())
            return dag.getNode(ISD.SUB, vt, dag.getConstant(0, vt, false), n0);
        if (c2 != null && c2.getAPIntValue().isPowerOf2())
        {
            // fold (mul x, (1 << c)) -> x << c
            return dag.getNode(ISD.MUL, vt, n0, dag.getConstant(c2.getAPIntValue().logBase2(),
                    new EVT(tli.getShiftAmountTy()), false));
        }
        if (c2 != null && c2.getAPIntValue().negative().isPowerOf2())
        {
            // fold (mul x, -(1 << c)) -> -(x << c) or (-x) << c
            long log2Val = c2.getAPIntValue().negative().logBase2();
            return dag.getNode(ISD.SUB, vt,
                    dag.getNode(ISD.SHL, vt, n0,
                            dag.getConstant(log2Val, vt, false)));
        }
        // (mul (shl X, c1), c2) -> (mul X, c2 << c1)
        if (c2 != null && n0.getOpcode() == ISD.SHL &&
                n0.getOperand(1).getNode() instanceof ConstantSDNode)
        {
            ConstantSDNode n01C = (ConstantSDNode)n0.getOperand(1).getNode();
            return dag.getNode(ISD.MUL, vt, n0.getOperand(0),
                    dag.getConstant(c2.getAPIntValue().shl(n01C.getAPIntValue()), vt, false));
        }
        // Change (mul (shl X, C), Y) -> (shl (mul X, Y), C) when the shift has one
        // use.
        SDValue x = new SDValue(), y = new SDValue();
        if (n0.getOpcode() == ISD.SHL && n0.getOperand(1).getNode() instanceof ConstantSDNode &&
                n0.hasOneUse())
        {
            x = n0;
            y = n1;
        }
        else if (n1.getOpcode() == ISD.SHL && n1.getOperand(1).getNode() instanceof ConstantSDNode &&
                n1.hasOneUse())
        {
            x = n1;
            y = n0;
        }
        if (x.getNode() != null)
        {
            return dag.getNode(ISD.SHL, vt, dag.getNode(ISD.MUL, vt, x.getOperand(0), y),
                    x.getOperand(1));
        }
        // fold (mul (add x, c1), c2) -> (add (mul x, c2), c1*c2)
        if (c2 != null && n0.getOpcode() == ISD.ADD && n0.hasOneUse() &&
                n0.getOperand(1).getNode() instanceof ConstantSDNode)
        {
            return dag.getNode(ISD.ADD, vt,
                    dag.getNode(ISD.MUL, vt, n0.getOperand(0), n1),
                    dag.getNode(ISD.MUL, vt, n0.getOperand(1), n1));
        }
        SDValue rmul = reassociateOps(ISD.MUL, n0, n1);
        return rmul.getNode() != null ? rmul : new SDValue();
    }

    private SDValue visitADDE(SDNode n)
    {
        SDValue n0 = n.getOperand(0);
        SDValue n1 = n.getOperand(1);
        ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n0.getNode() : null;
        ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n1.getNode() : null;
        EVT vt = n0.getValueType();
        SDValue carryIn = n.getOperand(2);

        if (c1 != null && c2 == null)
        {
            return dag.getNode(ISD.ADDE, n.getValueList(), n1, n0, carryIn);
        }
        // fold (adde X, Y, false) --> (addc  X, Y).
        if (carryIn.getOpcode() == ISD.CARRY_FALSE)
            return dag.getNode(ISD.ADDC, n.getValueList(), n0, n1);
        return new SDValue();
    }

    private SDValue visitADDC(SDNode n)
    {
        SDValue n0 = n.getOperand(0);
        SDValue n1 = n.getOperand(1);
        ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n0.getNode() : null;
        ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n1.getNode() : null;
        EVT vt = n0.getValueType();
        if (n.hasNumUsesOfValue(0, 1))
        {
            return combineTo(n, dag.getNode(ISD.ADD, vt, n1, n0),
                    dag.getNode(ISD.CARRY_FALSE, new EVT(MVT.Flag)), true);
        }
        if (c1 != null && c2 == null)
        {
            return dag.getNode(ISD.ADDC, n.getValueList(), n1, n0);
        }
        APInt[] lhs = new APInt[2];
        APInt[] rhs = new APInt[2];
        APInt mask = APInt.getAllOnesValue(vt.getSizeInBits());
        dag.computeMaskedBits(n0, mask, lhs, 0);
        if (lhs[0].getBoolValue())
        {
            dag.computeMaskedBits(n1, mask, rhs, 0);
            if (rhs[0].and(lhs[0].not().and(mask)).eq(lhs[0].not().and(mask)) ||
                    lhs[0].and(rhs[0].not().and(mask)).eq(rhs[0].not().and(mask)))
            {
                return combineTo(n, dag.getNode(ISD.OR, vt, n0, n1),
                        dag.getNode(ISD.CARRY_FALSE, new EVT(MVT.Flag)), true);
            }
        }

        return new SDValue();
    }

    private SDValue visitSUB(SDNode n)
    {
        SDValue n0 = n.getOperand(0);
        SDValue n1 = n.getOperand(1);
        ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n0.getNode() : null;
        ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n1.getNode() : null;
        EVT vt = n0.getValueType();

        if (vt.isVector())
        {
            SDValue foldedOp = simplifyVBinOp(n);
            if (foldedOp.getNode() != null)
                return foldedOp;
        }

        // fold (sub x, x) --> 0
        if (n0.equals(n1))
            return dag.getConstant(0, n.getValueType(0), false);
        if (c1 != null && c2 != null)
            return dag.foldConstantArithmetic(ISD.SUB, vt, c1, c2);
        if (c2 != null)
            return dag.getNode(ISD.ADD, vt, n0,
                    dag.getConstant(c2.getAPIntValue().negative(), vt, false));
        // fold (A+B)-A -> B
        if (n0.getOpcode() == ISD.ADD && n0.getOperand(0).equals(n1))
            return n0.getOperand(1);

        // fold (A+B)-B -> A
        if (n0.getOpcode() == ISD.ADD && n0.getOperand(1).equals(n1))
            return n0.getOperand(0);
        // fold ((A+(B+or-C))-B) -> A+or-C
        if (n0.getOpcode() == ISD.ADD)
        {
            int opc = n0.getOperand(1).getOpcode();
            if ((opc == ISD.ADD || opc == ISD.SUB) &&
                    n1.equals(n0.getOperand(1).getOperand(0)))
                return dag.getNode(opc, vt, n0.getOperand(0),
                        n0.getOperand(1).getOperand(1));
        }
        // fold ((A+(C+B))-B) -> A+C
        if (n0.getOpcode() == ISD.ADD &&
                n0.getOperand(1).getOpcode() == ISD.ADD &&
                n1.equals(n0.getOperand(1).getOperand(1)))
        {
            return dag.getNode(ISD.ADD, vt, n0.getOperand(0),
                    n0.getOperand(1).getOperand(0));
        }
        // fold ((A-(B-C))-C) -> A-B
        if (n0.getOpcode() == ISD.SUB &&
                n0.getOperand(1).getOpcode() == ISD.SUB &&
                n0.getOperand(1).getOperand(1).equals(n1))
        {
            return dag.getNode(ISD.SUB, vt, n0.getOperand(0),
                    n0.getOperand(1).getOperand(0));
        }
        // If either operand of a sub is undef, the result is undef
        if (n0.getOpcode() == ISD.UNDEF)
            return n0;
        if (n1.getOpcode() == ISD.UNDEF)
            return n1;
        // If the relocation model supports it, consider symbol offsets.
        if (n0.getNode() instanceof GlobalAddressSDNode)
        {
            GlobalAddressSDNode ga = (GlobalAddressSDNode)n0.getNode();
            if (!legalOprations && tli.isOffsetFoldingLegal(ga))
            {
                if (c2 != null && ga.getOpcode() == ISD.GlobalAddress)
                    return dag.getGlobalAddress(ga.getGlobalValue(),
                            vt, ga.getOffset() - c2.getSExtValue(), false, 0);

                if (n1.getNode() instanceof GlobalAddressSDNode)
                {
                    GlobalAddressSDNode gad = (GlobalAddressSDNode)n1.getNode();
                    if (ga.getGlobalValue().equals(gad.getGlobalValue()))
                        return dag.getConstant(ga.getOffset() -
                         - gad.getOffset(), vt, false);
                }
            }
        }
        return new SDValue();
    }

    private SDValue visitADD(SDNode n)
    {
        SDValue n0 = n.getOperand(0);
        SDValue n1 = n.getOperand(1);
        ConstantSDNode c1 = n0.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n0.getNode() : null;
        ConstantSDNode c2 = n1.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n1.getNode() : null;
        EVT vt = n0.getValueType();

        // fold vector ops.
        if (vt.isVector())
        {
            SDValue foldedOp = simplifyVBinOp(n);
            if (foldedOp.getNode() != null)
                return foldedOp;
        }

        // (add x, undef) --> undef
        if (n0.getOpcode() == ISD.UNDEF)
            return n0;
        if (n1.getOpcode() == ISD.UNDEF)
            return n1;
        if (c1 != null && c2 != null)
        {
            return dag.foldConstantArithmetic(ISD.ADD, vt, c1, c2);
        }
        if (c1 != null && c2 == null)
        {
            return dag.getNode(ISD.ADD, vt, n1, n0);
        }
        if (c2 != null && c2.isNullValue())
            return n0;
        if (n0.getNode() instanceof GlobalAddressSDNode)
        {
            // (add sym, c) --> sym + c
            GlobalAddressSDNode ga = (GlobalAddressSDNode)n0.getNode();
            if (!legalOprations && tli.isOffsetFoldingLegal(ga) &&
                    c2 != null && ga.getOpcode() == ISD.GlobalAddress)
            {
                return dag.getGlobalAddress(ga.getGlobalValue(),
                        vt, ga.getOffset() + c2.getSExtValue(),false, 0);
            }
        }
        // fold ((c1-A)+c2) -> (c1+c2)-A
        if (c2 != null && n0.getOpcode() == ISD.SUB)
        {
            if (n0.getOperand(0).getNode() instanceof ConstantSDNode)
            {
                ConstantSDNode lhsOp0C = (ConstantSDNode)n0.getOperand(0).getNode();
                return dag.getNode(ISD.SUB, vt, dag.getConstant(c2.getAPIntValue().
                        add(lhsOp0C.getAPIntValue()), vt, false), n0.getOperand(1));
            }
        }
        // reassociate add
        SDValue radd = reassociateOps(ISD.ADD, n0, n1);
        if (radd.getNode() != null)
            return radd;
        // fold ((0-A) + B) -> B-A
        if (n0.getOpcode() == ISD.SUB &&
                n0.getOperand(0).getNode() instanceof ConstantSDNode)
        {
            ConstantSDNode csd = (ConstantSDNode)n0.getOperand(0).getNode();
            if (csd.isNullValue())
                return dag.getNode(ISD.SUB, vt, n1, n0.getOperand(1));
        }
        // fold (A + (0-B)) -> A-B
        if (n1.getOpcode() == ISD.SUB &&
                n1.getOperand(0).getNode() instanceof ConstantSDNode)
        {
            ConstantSDNode csd = (ConstantSDNode)n1.getOperand(0).getNode();
            if (csd.isNullValue())
                return dag.getNode(ISD.SUB, vt, n0, n1.getOperand(1));
        }
        // fold (A+(B-A)) -> B
        if (n1.getOpcode() == ISD.SUB &&
                n1.getOperand(1).equals(n0))
            return n1.getOperand(1);
        // fold ((B-A)+A) -> B
        if (n0.getOpcode() == ISD.SUB &&
                n0.getOperand(1).equals(n1))
            return n0.getOperand(0);
        // fold (A+(B-(A+C))) to (B-C)
        if (n1.getOpcode() == ISD.SUB &&
                n1.getOperand(1).getOpcode() == ISD.ADD &&
                n1.getOperand(1).getOperand(0).equals(n0))
        {
            return dag.getNode(ISD.SUB, vt, n1.getOperand(0),
                    n1.getOperand(1).getOperand(1));
        }
        // fold (A+(B-(C+A))) to (B-C)
        if (n1.getOpcode() == ISD.SUB &&
                n1.getOperand(1).getOpcode() == ISD.ADD &&
                n1.getOperand(1).getOperand(1).equals(n0))
        {
            return dag.getNode(ISD.SUB, vt, n1.getOperand(0),
                    n1.getOperand(1).getOperand(0));
        }
        // fold (A+((B-A)+or-C)) to (B+or-C)
        int opc = n1.getOpcode();
        if ((opc == ISD.ADD || opc == ISD.SUB) &&
                n1.getOperand(0).getOpcode() == ISD.SUB)
        {
            if (n1.getOperand(0).getOperand(1).equals(n0))
                return dag.getNode(opc, vt, n1.getOperand(0).getOperand(0),
                        n1.getOperand(1));
        }
        // fold (A-B)+(C-D) to (A+C)-(B+D) when A or C is constant
        if (n0.getOpcode() == ISD.SUB && n1.getOpcode() == ISD.SUB)
        {
            if (n0.getOperand(0).getNode() instanceof ConstantSDNode ||
                    n1.getOperand(0).getNode() instanceof ConstantSDNode)
            {
                SDValue t0 = n0.getOperand(0);
                SDValue t1 = n0.getOperand(1);
                SDValue t2 = n1.getOperand(0);
                SDValue t3 = n1.getOperand(1);
                return dag.getNode(ISD.SUB, vt,
                        dag.getNode(ISD.ADD, vt, t0, t2),
                        dag.getNode(ISD.ADD, vt, t1, t3));
            }
        }
        if (!vt.isVector() && simplifyDemandedBits(new SDValue(n, 0)))
            return new SDValue(n, 0);

        // fold (a+b) -> (a|b) iff a and b share no bits.
        if (vt.isInteger() && !vt.isVector())
        {
            APInt mask = APInt.getAllOnesValue(vt.getSizeInBits());
            APInt[] lhs = new APInt[2];
            APInt[] rhs = new APInt[2];
            dag.computeMaskedBits(n0, mask, lhs, 0);
            if (lhs[0].getBoolValue())
            {
                dag.computeMaskedBits(n1, mask, rhs, 0);
                if (rhs[0].and(lhs[0].not().and(mask)).eq(lhs[0].not().and(mask)) ||
                        lhs[0].and(rhs[0].not().and(mask)).eq(rhs[0].not().and(mask)))
                {
                    return dag.getNode(ISD.OR, vt, n0, n1);
                }
            }
        }

        // fold (add (shl (add x, c1), c2), ) -> (add (add (shl x, c2), c1<<c2), )
        if (n0.getOpcode() == ISD.SHL && n0.getNode().hasOneUse())
        {
            SDValue result = combineShlAndConstant(n0, n1);
            if (result.getNode() != null) return result;
        }
        if (n1.getOpcode() == ISD.SHL && n1.getNode().hasOneUse())
        {
            SDValue result = combineShlAndConstant(n1, n0);
            if (result.getNode() != null) return result;
        }
        return new SDValue();
    }

    private SDValue combineShlAndConstant(SDValue n0, SDValue n1)
    {
        EVT vt = n0.getValueType();
        SDValue n00 = n0.getOperand(0);
        SDValue n01 = n0.getOperand(1);
        ConstantSDNode n01C = n01.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode)n01.getNode() : null;
        if (n01C != null && n00.getOpcode() == ISD.ADD &&
                n00.getNode().hasOneUse() &&
                n00.getOperand(1).getNode() instanceof ConstantSDNode)
        {
            n0 = dag.getNode(ISD.ADD, vt,
                    dag.getNode(ISD.SHL, vt, n00.getOperand(0), n01),
                    dag.getNode(ISD.SHL, vt, n00.getOperand(1), n01));
            return dag.getNode(ISD.ADD, vt, n0, n1);
        }
        return new SDValue();
    }

    private SDValue visitMERGE_VALUES(SDNode n)
    {
        WorklistRemover remover = new WorklistRemover(this);
        do
        {
            for (int i = 0, e = n.getNumOperands(); i < e; i++)
                dag.replaceAllUsesOfValueWith(new SDValue(n, i),
                        n.getOperand(i), remover);
        }while (!n.isUseEmpty());
        removeFromWorkList(n);
        dag.deleteNode(n);
        return new SDValue(n, 0);
    }

    private static SDValue getInputChainForNode(SDNode n)
    {
        int numOps = n.getNumOperands();
        if (numOps > 0)
        {
            if (n.getOperand(0).getValueType().getSimpleVT().simpleVT == MVT.Other)
                return n.getOperand(0);
            else if (n.getOperand(numOps-1).getValueType().getSimpleVT().simpleVT == MVT.Other)
                return n.getOperand(numOps-1);
            for (int i = 1; i < numOps-1; i++)
                if (n.getOperand(i).getValueType().getSimpleVT().simpleVT == MVT.Other)
                    return n.getOperand(i);
        }
        return new SDValue();
    }

    private SDValue visitTokenFactor(SDNode n)
    {
        // If n has two operands, where one has on input
        // chain equal to the other, so 'other' is redundant.
        if (n.getNumOperands() == 2)
        {
            if (getInputChainForNode(n.getOperand(0).getNode()).equals(n.getOperand(1)))
                return n.getOperand(0);
            if (getInputChainForNode(n.getOperand(1).getNode()).equals(n.getOperand(1)))
                return n.getOperand(1);
        }

        ArrayList<SDNode> tfs = new ArrayList<>();
        ArrayList<SDValue> ops = new ArrayList<>();
        HashSet<SDNode> seenOps = new HashSet<>();
        boolean changed = false;

        tfs.add(n);
        // Iterate through token factors.  The TFs grows when new token factors are
        // encountered.
        for (int i = 0, e = tfs.size(); i < e; i++)
        {
            SDNode tf = tfs.get(i);
            for (int j = 0, sz = tf.getNumOperands(); j < sz; j++)
            {
                SDValue op = tf.getOperand(j);
                switch (op.getOpcode())
                {
                    case ISD.EntryToken:
                        changed = true;
                        break;
                    case ISD.TokenFactor:
                    default:
                        if (seenOps.add(op.getNode()))
                            ops.add(op);
                        else
                            changed = true;
                        break;
                }
            }
        }
        SDValue result = new SDValue();
        if (changed)
        {
            if (ops.isEmpty())
                result = dag.getEntryNode();
            else
                result = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other),
                        ops);
            return combineTo(n, result, false);
        }
        return result;
    }

    private SDValue simplifyVBinOp(SDNode n)
    {
        // TODO: 18-6-11
        return new SDValue();
    }
    public void addToWorkList(SDNode n)
    {
        if (n == null) return;
        removeFromWorkList(n);
        workList.add(n);
    }

    public void removeFromWorkList(SDNode n)
    {
        workList.remove(n);
    }
    public SDValue combineTo(SDNode n, SDValue[] to, boolean addTo)
    {
        assert n.getNumValues() == to.length;
        WorklistRemover remover = new WorklistRemover(this);
        dag.replaceAllUsesWith(n, to[0], remover);
        if (addTo)
        {
            for (SDValue v : to)
            {
                if (v.getNode() != null)
                {
                    addToWorkList(v.getNode());
                    addUsersToWorklist(v.getNode());
                }
            }
        }

        if (n.isUseEmpty())
        {
            removeFromWorkList(n);
            dag.deleteNode(n);
        }
        return new SDValue(n, 0);
    }

    public SDValue combineTo(SDNode n, ArrayList<SDValue> to, boolean addTo)
    {
        SDValue[] temp = new SDValue[to.size()];
        to.toArray(temp);
        return combineTo(n, temp, addTo);
    }

    public SDValue combineTo(SDNode n, SDValue res, boolean addTo)
    {
        ArrayList<SDValue> vals = new ArrayList<>();
        vals.add(res);
        return combineTo(n, vals, addTo);
    }

    public SDValue combineTo(SDNode n, SDValue res0, SDValue res1, boolean addTo)
    {
        ArrayList<SDValue> vals = new ArrayList<>();
        vals.add(res0);
        vals.add(res1);
        return combineTo(n, vals, addTo);
    }

    public static class WorklistRemover implements DAGUpdateListener
    {
        private DAGCombiner combiner;
        public WorklistRemover(DAGCombiner cmb)
        {
            combiner = cmb;
        }
        @Override
        public void nodeDeleted(SDNode node, SDNode e)
        {
            combiner.removeFromWorkList(node);
        }

        @Override
        public void nodeUpdated(SDNode node)
        {
            // ignore updates.
        }
    }

    public void commitTargetLoweringOpt(TargetLoweringOpt tlo)
    {
        WorklistRemover remover = new WorklistRemover(this);
        dag.replaceAllUsesOfValueWith(tlo.oldVal, tlo.newVal, remover);

        addToWorkList(tlo.newVal.getNode());
        addUsersToWorklist(tlo.newVal.getNode());

        if (tlo.oldVal.getNode().isUseEmpty())
        {
            removeFromWorkList(tlo.oldVal.getNode());

            for (int i = 0, e = tlo.oldVal.getNode().getNumOperands(); i < e; i++)
            {
                if (tlo.oldVal.getNode().getOperand(i).getNode().hasOneUse())
                    addToWorkList(tlo.oldVal.getNode().getOperand(i).getNode());
            }
            dag.deleteNode(tlo.oldVal.getNode());
        }
    }

    private void addUsersToWorklist(SDNode node)
    {
        for (SDUse u : node.useList)
        {
            addToWorkList(u.getNode());
        }
    }
}
