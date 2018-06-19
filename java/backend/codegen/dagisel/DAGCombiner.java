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
import backend.codegen.dagisel.SDNode.CondCodeSDNode;
import backend.codegen.dagisel.SDNode.ConstantSDNode;
import backend.codegen.dagisel.SDNode.GlobalAddressSDNode;
import backend.codegen.dagisel.SDNode.LoadSDNode;
import backend.target.TargetLowering;
import backend.target.TargetLowering.TargetLoweringOpt;
import backend.target.TargetMachine;
import jdk.nashorn.internal.objects.Global;
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
        return new SDValue();
    }

    private SDValue visitSELECT(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitCTPOP(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitCTTZ(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitCTLZ(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitSRL(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitSRA(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitSHL(SDNode n)
    {
        return new SDValue();
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
        return new SDValue();
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
        return new SDValue();
    }

    private SDValue visitSDIVREM(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitUMUL_LOHI(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitSMUL_LOHI(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitMULHS(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitMULHU(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitUREM(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitSREM(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitUDIV(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitSDIV(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitMUL(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitADDE(SDNode n)
    {
        return new SDValue();
    }

    private SDValue visitADDC(SDNode n)
    {
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
