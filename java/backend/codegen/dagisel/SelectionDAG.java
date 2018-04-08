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

import backend.codegen.EVT;
import backend.codegen.MVT;
import backend.codegen.MachineFunction;
import backend.codegen.MachineModuleInfo;
import backend.codegen.dagisel.SDNode.CondCodeSDNode;
import backend.codegen.dagisel.SDNode.ConstantFPSDNode;
import backend.codegen.dagisel.SDNode.ConstantSDNode;
import backend.codegen.dagisel.SDNode.SDVTList;
import backend.codegen.fastISel.ISD;
import backend.target.TargetLowering;
import backend.target.TargetMachine;
import backend.value.ConstantFP;
import backend.value.ConstantInt;
import gnu.trove.map.hash.TIntObjectHashMap;
import tools.APFloat;
import tools.APInt;
import tools.FoldingSetNodeID;
import tools.OutParamWrapper;

import java.util.ArrayList;

import static backend.codegen.dagisel.SDNode.EVTToAPFloatSemantics;
import static backend.support.BackendCmdOptions.EnableUnsafeFPMath;
import static tools.APFloat.OpStatus.opDivByZero;
import static tools.APFloat.OpStatus.opInvalidOp;
import static tools.APFloat.RoundingMode.rmNearestTiesToEven;
import static tools.APFloat.RoundingMode.rmTowardZero;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class SelectionDAG
{
    private TargetMachine target;
    private TargetLowering tli;
    private MachineFunction mf;
    private FunctionLoweringInfo fli;
    private MachineModuleInfo mmi;
    private SDNode entryNode;
    private SDValue root;
    private ArrayList<SDNode> allNodes;
    private ArrayList<SDVTList> vtlist;
    private ArrayList<CondCodeSDNode> condCodeNodes;
    private TIntObjectHashMap<SDNode> cseMap;
    public SelectionDAG()
    {
        allNodes = new ArrayList<>();
        vtlist = new ArrayList<>();
        condCodeNodes = new ArrayList<>();
        cseMap = new TIntObjectHashMap<>();
    }

    public TargetMachine getTarget()
    {
        return target;
    }

    public SDValue getNode(int opc, EVT vt, SDValue... ops)
    {
        int numOps = ops.length;
        switch (numOps)
        {
            case 0:
                return getNode(opc, vt);
            case 1:
                return getNode(opc, vt, ops[0]);
            case 2:
                return getNode(opc, vt, ops[0], ops[1]);
            case 3:
                return getNode(opc, vt, ops[0], ops[1], ops[2]);
        }
        switch (opc)
        {
            default: break;
            case ISD.SELECT_CC:
                assert numOps == 5:"SELECT_CC takes five operands!";
                assert ops[0].getValueType().equals(ops[1].getValueType()):
                        "LHS and RHS of condition must have same type!";
                assert ops[2].getValueType().equals(ops[3].getValueType()):
                        "True and False parts of SELECT_CC must have same type!";
                assert ops[2].getValueType().equals(vt):"SELECT_CC node must be same type as true/false part!";
                break;
            case ISD.BR_CC:
                assert numOps == 5:"BR_CC takes five operands!";
                assert ops[2].getValueType().equals(ops[3].getValueType()):
                        "LHS/RHS of comparison should match types!";
                break;
        }

        SDNode node;
        SDVTList vts = getVTList(vt);
        if (!vt.equals(new EVT(MVT.Flag)))
        {
            FoldingSetNodeID calc = new FoldingSetNodeID();
            addNodeToIDNode(calc, opc, vts, ops);
            int id = calc.computeHash();
            if (cseMap.containsKey(id))
            {
                node = cseMap.get(id);
                return new SDValue(node, 0);
            }
            node = new SDNode(opc, vts, ops);
            cseMap.put(id, node);
        }
        else
        {
            node = new SDNode(opc, vts, ops);
        }
        allNodes.add(node);
        return new SDValue(node, 0);
    }

    public SDValue getNode(int opc, EVT vt)
    {
        FoldingSetNodeID calc = new FoldingSetNodeID();
        addNodeToIDNode(calc, opc, getVTList(vt));
        int id = calc.computeHash();
        if (cseMap.containsKey(id))
            return new SDValue(cseMap.get(id), 0);
        SDNode node = new SDNode(opc, getVTList(vt));
        cseMap.put(id, node);
        allNodes.add(node);
        return new SDValue(node, 0);
    }

    public SDValue getNode(int opc, EVT vt, SDValue op0)
    {
        if (op0.getNode() instanceof ConstantSDNode)
        {
            ConstantSDNode cn1 = (ConstantSDNode)op0.getNode();
            APInt val = cn1.getAPIntValue();
            int bitwidth = vt.getSizeInBits();
            switch (opc)
            {
                default: break;
                case ISD.SIGN_EXTEND:
                    return getConstant(new APInt(val).sextOrTrunc(bitwidth), vt, false);
                case ISD.ANY_EXTEND:
                case ISD.ZERO_EXTEND:
                case ISD.TRUNCATE:
                    return getConstant(new APInt(val).zextOrTrunc(bitwidth), vt, false);
                case ISD.UINT_TO_FP:
                case ISD.SINT_TO_FP:
                {
                    long[] zeros = {0, 0};
                    if (vt.equals(new EVT(MVT.ppcf128)))
                        break;
                    APFloat apf = new APFloat(new APInt(bitwidth, 2, zeros));
                    apf.convertFromAPInt(val, opc == ISD.SINT_TO_FP, rmNearestTiesToEven);
                    return getConstantFP(apf, vt, false);
                }
                case ISD.BIT_CONVERT:
                {
                    if (vt.equals(new EVT(MVT.f32)) && cn1.getValueType(0).equals(new EVT(MVT.i32)))
                        return getConstantFP(val.bitsToFloat(), vt, false);
                    else if (vt.equals(new EVT(MVT.f64)) && cn1.getValueType(0).equals(new EVT(MVT.i64)))
                        return getConstantFP(val.bitsToDouble(), vt, false);
                    break;
                }
                case ISD.BSWAP:
                    return getConstant(val.byteSwap(), vt, false);
                case ISD.CTPOP:
                    return getConstant(val.countPopulation(), vt, false);
                case ISD.CTLZ:
                    return getConstant(val.countLeadingZeros(), vt, false);
                case ISD.CTTZ:
                    return getConstant(val.countTrailingZeros(), vt, false);
            }
        }

        if (op0.getNode() instanceof ConstantFPSDNode)
        {
            ConstantFPSDNode fp1 = (ConstantFPSDNode)op0.getNode();
            APFloat val = fp1.getValueAPF();
            if (!vt.equals(new EVT(MVT.ppcf128)) && !op0.getValueType().equals(new EVT(MVT.ppcf128)))
            {
                switch (opc)
                {
                    case ISD.FNEG:
                        val.changeSign();
                        return getConstantFP(val, vt, false);
                    case ISD.FABS:
                        val.clearSign();
                        return getConstantFP(val, vt, false);
                    case ISD.FP_ROUND:
                    case ISD.FP_EXTEND:
                    {
                        OutParamWrapper<Boolean> ignored = new OutParamWrapper<>(false);
                        val.convert(EVTToAPFloatSemantics(vt), rmNearestTiesToEven, ignored);
                        return getConstantFP(val, vt, false);
                    }
                    case ISD.FP_TO_SINT:
                    case ISD.FP_TO_UINT:
                    {
                        long[] x = {0, 0};
                        OutParamWrapper<Boolean> ignored = new OutParamWrapper<>(false);
                        int opStatus = val.convertToInteger(x, vt.getSizeInBits(),
                                opc == ISD.FP_TO_SINT,
                                rmTowardZero,
                                ignored);
                        if (opStatus == opInvalidOp)
                            break;
                        APInt res = new APInt(vt.getSizeInBits(), 2, x);
                        return getConstant(res, vt, false);
                    }
                    case ISD.BIT_CONVERT:
                    {
                        if (vt.equals(new EVT(MVT.i32)) && fp1.getValueType(0).equals(new EVT(MVT.f32)))
                            return getConstant((int)(val.bitcastToAPInt().getZExtValue()), vt, false);
                        if (vt.equals(new EVT(MVT.i64)) && fp1.getValueType(0).equals(new EVT(MVT.f64)))
                            return getConstant(val.bitcastToAPInt().getZExtValue(), vt, false);
                        break;
                    }
                }
            }
        }

        int opOpcode = op0.getNode().getOpcode();
        switch (opc)
        {
            case ISD.TokenFactor:
            case ISD.MERGE_VALUES:
            case ISD.CONCAT_VECTORS:
                return op0;
            case ISD.FP_ROUND:
            case ISD.FP_EXTEND:
                assert vt.isFloatingPoint() && op0.getValueType().isFloatingPoint():
                        "Invalid FP cast!";
                if (op0.getValueType().equals(vt)) return op0;
                if (op0.getOpcode() == ISD.UNDEF)
                    return getUNDEF(vt);
                break;
            case ISD.SIGN_EXTEND:
                assert vt.isInteger() && op0.getValueType().isInteger()
                        :"Invalid Integer cast!";
                if (op0.getValueType().equals(vt)) return op0;
                assert op0.getValueType().bitsLT(vt):"Invalid sext node, dest < src!";
                if (opOpcode == ISD.SIGN_EXTEND || opOpcode == ISD.ZERO_EXTEND)
                    return getNode(opOpcode, vt, op0.getNode().getOperand(0));
                break;
            case ISD.ANY_EXTEND:
            case ISD.ZERO_EXTEND:
                assert vt.isInteger() && op0.getValueType().isInteger()
                        :"Invalid Integer cast!";
                if (op0.getValueType().equals(vt)) return op0;
                assert op0.getValueType().bitsLT(vt):"Invalid zext node, dest < src!";
                if (opOpcode == ISD.SIGN_EXTEND || opOpcode == ISD.ZERO_EXTEND)
                    return getNode(opOpcode, vt, op0.getNode().getOperand(0));
                break;

            case ISD.TRUNCATE:
                assert vt.isInteger() && op0.getValueType().isInteger()
                        :"Invalid Integer cast!";
                if (op0.getValueType().equals(vt)) return op0;
                assert op0.getValueType().bitsGT(vt):"Invalid truncate node, dest > src!";
                if (opOpcode == ISD.TRUNCATE)
                    return getNode(ISD.TRUNCATE, vt, op0.getNode().getOperand(0));
                else if (opOpcode == ISD.ZERO_EXTEND || opOpcode == ISD.SIGN_EXTEND ||
                        opOpcode == ISD.ANY_EXTEND)
                {
                    // If the source is smaller than the dest, we still need an extend.
                    if (op0.getOperand(0).getValueType().bitsLT(vt))
                        return getNode(opOpcode, vt, op0.getOperand(0));
                    else if (op0.getOperand(0).getValueType().bitsGT(vt))
                        return getNode(ISD.TRUNCATE, vt, op0.getOperand(0));
                    else
                        return op0.getOperand(0);
                }
                break;
            case ISD.BIT_CONVERT:
                assert vt.getSizeInBits() == op0.getValueType().getSizeInBits():
                        "Can't perform bit conversion between different size!";
                if (vt.equals(op0.getValueType())) return op0;
                if (opOpcode == ISD.BIT_CONVERT)
                    return getNode(ISD.BIT_CONVERT, vt, op0.getOperand(0));
                if (opOpcode == ISD.UNDEF)
                    return getUNDEF(vt);
                break;
            case ISD.SCALAR_TO_VECTOR:
                assert vt.isVector() && !op0.getValueType().isVector() &&
                        (vt.getVectorElementType().equals(op0.getValueType()) ||
                                (vt.getVectorElementType().isInteger() &&
                                op0.getValueType().isInteger() &&
                                vt.getVectorElementType().bitsLE(op0.getValueType())))
                        :"Illegal SCALAR_TO_VECTOR node!";
                if (opOpcode == ISD.UNDEF)
                    return getUNDEF(vt);

                if (opOpcode == ISD.EXTRACT_VECTOR_ELT &&
                        (op0.getOperand(0).getNode() instanceof ConstantSDNode) &&
                        op0.getConstantOperandVal(1) == 0 &&
                        op0.getOperand(0).getValueType().equals(vt))
                    return op0.getOperand(0);
                break;
            case ISD.FNEG:
                // -(X-Y) ==> Y-X is unsafe because -0.0 ！= +0.0 when X equals Y.
                if (EnableUnsafeFPMath.value && op0.getOpcode() == ISD.FSUB)
                {
                    return getNode(ISD.FSUB, vt, op0.getOperand(1), op0.getOperand(0));
                }
                if (opOpcode == ISD.FNEG)   // -(-X) ==> X
                    return op0.getOperand(0);
                break;
            case ISD.FABS:
                // abs(-X) ==> abs(X)
                if (opOpcode == ISD.FNEG)
                {
                    return getNode(ISD.FABS, vt, op0.getOperand(0));
                }
                break;
        }

        SDNode node;
        SDVTList vts = getVTList(vt);
        if (!vt.equals(new EVT(MVT.Flag)))
        {
            FoldingSetNodeID calc = new FoldingSetNodeID();
            addNodeToIDNode(calc, opc, vts, op0);
            int id = calc.computeHash();
            if (cseMap.containsKey(id))
                return new SDValue(cseMap.get(id), 0);
            node = new SDNode.UnarySDNode(opc, vts, op0);
            cseMap.put(id, node);
        }
        else
            node = new SDNode.UnarySDNode(opc, vts, op0);
        allNodes.add(node);
        return new SDValue(node, 0);
    }

    private SDValue getUNDEF(EVT vt)
    {
        return getNode(ISD.UNDEF, vt);
    }

    public SDValue foldArithmetic(int opcode, EVT vt,
            ConstantSDNode cst1, ConstantSDNode cst2)
    {
        APInt c1 = cst1.getAPIntValue(), c2 = cst2.getAPIntValue();
        switch (opcode)
        {
            case ISD.ADD: return getConstant(c1.add(c2), vt, false);
            case ISD.SUB: return getConstant(c1.sub(c2), vt, false);
            case ISD.MUL: return getConstant(c1.mul(c2), vt, false);
            case ISD.SDIV: return getConstant(c1.sdiv(c2), vt, false);
            case ISD.UDIV: return getConstant(c1.udiv(c2), vt, false);
            case ISD.UREM: return getConstant(c1.urem(c2), vt, false);
            case ISD.SREM: return getConstant(c1.srem(c2), vt, false);
            case ISD.AND:  return getConstant(c1.and(c2), vt, false);
            case ISD.OR:   return getConstant(c1.or(c2), vt, false);
            case ISD.XOR:  return getConstant(c1.xor(c2), vt, false);
            //todo case ISD.SRA: return getConstant(c1.sra(c2), vt, false);
            case ISD.SHL: return getConstant(c1.shl(c2), vt, false);
        }
        return new SDValue();
    }

    public SDValue getNode(int opc, EVT vt, SDValue op0, SDValue op1)
    {
        ConstantSDNode cn0 = op0.getNode() instanceof ConstantSDNode ? (ConstantSDNode)op0.getNode() : null;
        ConstantSDNode cn1 = op1.getNode() instanceof ConstantSDNode ? (ConstantSDNode)op1.getNode() : null;

        switch (opc)
        {
            default:break;
            case ISD.TokenFactor:
                assert vt.equals(new EVT(MVT.Other))  && op0.getValueType().equals(new EVT(MVT.Other))
                        && op1.getValueType().equals(new EVT(MVT.Other));
                // fold trivial token factors.
                if (op0.getOpcode() == ISD.EntryToken) return op1;
                if (op1.getOpcode() == ISD.EntryToken) return op0;

                if (op1.equals(op0)) return op0;
                break;

            case ISD.CONCAT_VECTORS:
                if (op0.getOpcode() == ISD.BUILD_VECTOR &&
                        op1.getOpcode() == ISD.BUILD_VECTOR)
                {
                    SDValue[] vals = new SDValue[op0.getNumOperands()+op1.getNumOperands()];
                    int i = 0;
                    for (int j = 0, e = op0.getNumOperands(); j < e; j++)
                        vals[i++] = op0.getOperand(j);
                    for (int j = 0, e = op1.getNumOperands(); j < e; j++)
                        vals[i++] = op1.getOperand(j);

                    return getNode(ISD.BUILD_VECTOR, vt, vals);
                }
                break;
            case ISD.AND:
                assert vt.isInteger() && op0.getValueType().isInteger()
                        && op1.getValueType().isInteger():"Binary operator types must match!";
                // X & 0 ==> 0
                if (cn1 != null && cn1.isNullValue())
                    return op1;
                // X & -1 ==> X
                if (cn1 != null && cn1.isAllOnesValue())
                    return op0;
                break;
            case ISD.OR:
            case ISD.XOR:
            case ISD.ADD:
            case ISD.SUB:
                assert vt.isInteger() && op0.getValueType().isInteger()
                        && op1.getValueType().isInteger():"Binary operator types must match!";
                // (X ^|+- 0) = X
                if (cn1 != null && cn1.isNullValue())
                    return op0;
                break;
            case ISD.MUL:
                assert vt.isInteger() && op0.getValueType().isInteger()
                        && op1.getValueType().isInteger():"Binary operator types must match!";
                // X * 0 == 0
                if (cn1 != null && cn1.isNullValue())
                    return op0;
                break;
            case ISD.SHL:
            case ISD.SRA:
            case ISD.SRL:
            case ISD.ROTL:
            case ISD.ROTR:
                assert vt.equals(op0.getValueType()):
                        "Shift operators return tyep must be the same as their arg";
                assert vt.isInteger() && op0.getValueType().equals(op1.getValueType()):
                        "Shift operator only works on integer type!";

                if (vt.equals(new EVT(MVT.i1)))
                    return op0;
                break;
        }

        if (cn0 != null)
        {
            if (cn1 != null)
            {
                SDValue res = foldArithmetic(opc, vt, cn0, cn1);
                if (res.getNode() != null) return res;
            }
            else
            {
                if (isCommutativeBinOp(opc))
                {
                    ConstantSDNode t = cn0;
                    cn0 = cn1;
                    cn1 = t;
                    SDValue temp = op0;
                    op0 = op1;
                    op1 = temp;
                }
            }
        }

        ConstantFPSDNode fp0 = op0.getNode() instanceof ConstantFPSDNode ?(ConstantFPSDNode)op0.getNode():null;
        ConstantFPSDNode fp1 = op1.getNode() instanceof ConstantFPSDNode ?(ConstantFPSDNode)op1.getNode():null;

        if (fp0 != null)
        {
            if (fp1 == null && isCommutativeBinOp(opc))
            {
                ConstantFPSDNode t = fp0;
                fp0 = fp1;
                fp1 = t;
                SDValue temp = op0;
                op0 = op1;
                op1 = temp;
            }
            else if (fp1 != null && !vt.equals(new EVT(MVT.ppcf128)))
            {
                APFloat v0 = fp0.getValueAPF(), v1 = fp1.getValueAPF();
                int opStatus;
                switch (opc)
                {
                    case ISD.FADD:
                        opStatus = v0.add(v1, rmNearestTiesToEven);
                        if (opStatus != opInvalidOp)
                            return getConstantFP(v1, vt, false);
                        break;
                    case ISD.FSUB:
                        opStatus = v0.subtract(v1, rmNearestTiesToEven);
                        if (opStatus != opInvalidOp)
                            return getConstantFP(v1, vt, false);
                        break;
                    case ISD.FMUL:
                        opStatus = v0.multiply(v1, rmNearestTiesToEven);
                        if (opStatus != opInvalidOp)
                            return getConstantFP(v1, vt, false);
                        break;
                    case ISD.FDIV:
                        opStatus = v0.divide(v1, rmNearestTiesToEven);
                        if (opStatus != opInvalidOp && opStatus != opDivByZero)
                            return getConstantFP(v1, vt, false);
                        break;
                    case ISD.FREM:
                        opStatus = v0.mod(v1, rmNearestTiesToEven);
                        if (opStatus != opInvalidOp && opStatus != opDivByZero)
                            return getConstantFP(v1, vt, false);
                        break;
                    case ISD.FCOPYSIGN:
                        v0.copySign(v1);
                        return getConstantFP(v0, vt, false);
                    default:break;
                }
            }
        }

        if (op0.getOpcode() == ISD.UNDEF)
        {
            if(isCommutativeBinOp(opc))
            {
                SDValue temp = op0;
                op0 = op1;
                op1 = temp;
            }
            else
            {
                switch (opc)
                {
                    case ISD.FP_ROUND_INREG:
                    case ISD.SIGN_EXTEND_INREG:
                    case ISD.SUB:
                    case ISD.FSUB:
                    case ISD.FDIV:
                    case ISD.FREM:
                    case ISD.SRA:
                        return op0;     // fold op(undef, arg2) -> undef
                    case ISD.UDIV:
                    case ISD.SDIV:
                    case ISD.UREM:
                    case ISD.SREM:
                    case ISD.SRL:
                    case ISD.SHL:
                    {
                        if (vt.isVector())
                            return getConstant(0, vt, false);
                        return op1;
                    }
                }
            }
        }

        if (op1.getOpcode() == ISD.UNDEF)
        {
            switch (opc)
            {
                case ISD.XOR:
                    if (op0.getOpcode() == ISD.UNDEF)
                        return getConstant(0, vt, false);
                    // fallthrough
                case ISD.ADD:
                case ISD.ADDC:
                case ISD.ADDE:
                case ISD.SUB:
                case ISD.UDIV:
                case ISD.SDIV:
                case ISD.UREM:
                case ISD.SREM:
                    return op1;       // fold op(arg1, undef) -> undef
                case ISD.FADD:
                case ISD.FSUB:
                case ISD.FMUL:
                case ISD.FDIV:
                case ISD.FREM:
                    if (EnableUnsafeFPMath.value)
                        return op1;
                    break;
                case ISD.MUL:
                case ISD.AND:
                case ISD.SRL:
                case ISD.SHL:
                    if (!vt.isVector())
                        return getConstant(0, vt, false);
                    return op0;
                case ISD.OR:
                    if (!vt.isVector())
                        return getConstant(APInt.getAllOnesValue(vt.getSizeInBits()), vt, false);
                    return op0;
                case ISD.SRA:
                    return op0;
            }
        }

        SDNode node;
        SDVTList vts = getVTList(vt);
        if (!vt.equals(new EVT(MVT.Flag)))
        {
            FoldingSetNodeID calc = new FoldingSetNodeID();
            addNodeToIDNode(calc, opc, vts, op0, op1);
            int id = calc.computeHash();
            if (cseMap.containsKey(id))
                return new SDValue(cseMap.get(id), 0);
            node = new SDNode.BinarySDNode(opc, vts, op0, op1);
            cseMap.put(id, node);
        }
        else
            node = new SDNode.BinarySDNode(opc, vts, op0, op1);
        allNodes.add(node);
        return new SDValue(node, 0);
    }

    static boolean isCommutativeBinOp(int opc)
    {
        switch (opc)
        {
            case ISD.ADD:
            case ISD.MUL:
            case ISD.MULHU:
            case ISD.MULHS:
            case ISD.SMUL_LOHI:
            case ISD.UMUL_LOHI:
            case ISD.FADD:
            case ISD.FMUL:
            case ISD.AND:
            case ISD.OR:
            case ISD.XOR:
            case ISD.SADDO:
            case ISD.UADDO:
            case ISD.ADDC:
            case ISD.ADDE: return true;
            default: return false;
        }
    }

    public SDValue getNode(int opc, EVT vt, SDValue op0, SDValue op1, SDValue op2)
    {
        ConstantSDNode cn0 = op0.getNode() instanceof ConstantSDNode ?(ConstantSDNode)op0.getNode():null;
        ConstantSDNode cn1 = op1.getNode() instanceof ConstantSDNode ?(ConstantSDNode)op1.getNode():null;

        switch (opc)
        {
            case ISD.CONCAT_VECTORS:
            {
                if (op0.getOpcode() == ISD.BUILD_VECTOR &&
                        op1.getOpcode() == ISD.BUILD_VECTOR &&
                        op2.getOpcode() == ISD.BUILD_VECTOR)
                {
                    SDValue[] vals = new SDValue[op0.getNumOperands()+op1.getNumOperands()+op2.getNumOperands()];
                    int i = 0;
                    for (int j = 0, e = op0.getNumOperands(); j < e; j++)
                        vals[i++] = op0.getOperand(j);
                    for (int j = 0, e = op1.getNumOperands(); j < e; j++)
                        vals[i++] = op1.getOperand(j);
                    for (int j = 0, e = op2.getNumOperands(); j < e; j++)
                        vals[i++] = op2.getOperand(j);

                    return getNode(ISD.BUILD_VECTOR, vt, vals);
                }
                break;
            }
            case ISD.SELECT:
            {
                // true, X, Y ==> X
                // false, X, Y ==> Y
                if (cn0 != null)
                {
                    return cn0.getZExtValue() != 0 ? op1 : op2;
                }
                if (op1.equals(op2)) return op1;
            }
            case ISD.BRCOND:
                if (cn1 != null)
                {
                    return cn1.getZExtValue() != 0 ? getNode(ISD.BR, new EVT(MVT.Other), op0, op2):op0;
                }
                break;
            case ISD.BIT_CONVERT:
                if (op0.getValueType().equals(vt))
                    return op0;
                break;
        }

        SDNode node;
        SDVTList vts = getVTList(vt);
        if (!vt.equals(new EVT(MVT.Flag)))
        {
            FoldingSetNodeID calc = new FoldingSetNodeID();
            addNodeToIDNode(calc, opc, vts, op0, op1, op2);
            int id = calc.computeHash();
            if (cseMap.containsKey(id))
                return new SDValue(cseMap.get(id), 0);
            node = new SDNode.TernarySDNode(opc, vts, op0, op1, op2);
            cseMap.put(id, node);
        }
        else
            node = new SDNode.TernarySDNode(opc, vts, op0, op1, op2);
        allNodes.add(node);
        return new SDValue(node, 0);
    }

    public SDValue getCondCode(CondCode cond)
    {
        int idx = cond.ordinal();
        if (idx < condCodeNodes.size())
        {
            return new SDValue(condCodeNodes.get(idx), 0);
        }
        for (int i = condCodeNodes.size(); i < idx; i++)
            condCodeNodes.add(null);
        CondCodeSDNode node = new CondCodeSDNode(cond);
        condCodeNodes.set(idx, node);
        allNodes.add(node);
        return new SDValue(node, 0);
    }

    public SDValue getSetCC(EVT vt, SDValue op1, SDValue op2, CondCode cond)
    {
        return getNode(ISD.SETCC, vt, op1, op2, getCondCode(cond));
    }

    public SDValue getIntPtrConstant(long amt)
    {
        return getIntPtrConstant(amt, false);
    }

    public SDValue getIntPtrConstant(long amt, boolean isTarget)
    {
        return getConstant(amt, new EVT(tli.getPointerTy()), isTarget);
    }

    public SDValue getConstant(long val, EVT vt, boolean isTarget)
    {
        EVT eltVt = vt.isVector()?vt.getVectorElementType() : vt;
        assert eltVt.getSizeInBits() >= 64 || (val >> eltVt.getSizeInBits()) + 1 < 2
                :"getConstant with a long value that doesn't fit in type!";
        return getConstant(new APInt(eltVt.getSizeInBits(), val), vt, isTarget);
    }

    public SDValue getConstant(APInt val, EVT vt, boolean isTarget)
    {
        return getConstant(ConstantInt.get(val), vt, isTarget);
    }

    public SDVTList getVTList(EVT... vts)
    {
        assert vts != null && vts.length > 0 : "Can't have an emtpy list!";
        for (int i = vtlist.size() - 1; i >= 0; i--)
        {
            SDVTList list = vtlist.get(i);
            if (list.vts.length != vts.length)
                continue;
            int j = vts.length - 1;
            for (; j >= 0; j--)
            {
                if (!vts[j].equals(list.vts[j]))
                    break;
            }
            if (j == -1)
                return list;
        }

        SDVTList list = makeVTList(vts);
        vtlist.add(list);
        return list;
    }

    public SDVTList makeVTList(EVT[] vts)
    {
        SDVTList list = new SDVTList();
        list.vts = vts;
        list.numVTs = vts.length;
        return list;
    }

    public SDValue getConstant(ConstantInt ci, EVT vt, boolean isTarget)
    {
        assert vt.isInteger():"Can't create FP integer constant";
        EVT eltVT = vt.isVector()?vt.getVectorElementType() : vt;
        assert ci.getBitsWidth() == eltVT.getSizeInBits():"APInt size doesn't match type size!";

        int opc = isTarget?ISD.TargetConstant : ISD.Constant;
        FoldingSetNodeID id = new FoldingSetNodeID();
        addNodeToIDNode(id, opc, getVTList(eltVT));
        id.addInteger(ci.hashCode());
        int hash = id.computeHash();
        SDNode n = null;
        if (cseMap.containsKey(hash))
        {
            n = cseMap.get(hash);
            if (!vt.isVector())
                return new SDValue(cseMap.get(hash), 0);
        }
        if (n == null)
        {
            n = new ConstantSDNode(isTarget, ci, eltVT);
            cseMap.put(hash, n);
            allNodes.add(n);
        }

        SDValue res = new SDValue(n, 0);
        if (vt.isVector())
        {
            SDValue[] ops = new SDValue[vt.getVectorNumElements()];
            for (int i = 0; i < ops.length; i++)
                ops[i]  = res;

            res = getNode(ISD.BUILD_VECTOR, vt, ops);
        }
        return res;
    }

    public SDValue getConstantFP(APFloat apf, EVT vt, boolean isTarget)
    {
        return getConstantFP(ConstantFP.get(apf), vt, isTarget);
    }

    public SDValue getConstantFP(ConstantFP val, EVT vt, boolean isTarget)
    {
        assert vt.isFloatingPoint():"Can't calling getConstantFP method on non-floating";
        EVT eltVT = vt.isVector()?vt.getVectorElementType() : vt;

        int opc = isTarget?ISD.TargetConstantFP : ISD.ConstantFP;
        FoldingSetNodeID id = new FoldingSetNodeID();
        addNodeToIDNode(id, opc, getVTList(eltVT));
        id.addInteger(val.hashCode());
        int hash = id.computeHash();
        SDNode n = null;
        if (cseMap.containsKey(hash))
        {
            n = cseMap.get(hash);
            if (!vt.isVector())
                return new SDValue(cseMap.get(hash), 0);
        }
        if (n == null)
        {
            n = new ConstantFPSDNode(isTarget, val, eltVT);
            cseMap.put(hash, n);
            allNodes.add(n);
        }

        SDValue res = new SDValue(n, 0);
        if (vt.isVector())
        {
            SDValue[] ops = new SDValue[vt.getVectorNumElements()];
            for (int i = 0; i < ops.length; i++)
                ops[i]  = res;

            res = getNode(ISD.BUILD_VECTOR, vt, ops);
        }
        return res;
    }

    public SDValue getConstantFP(float val, EVT vt, boolean isTarget)
    {
        return getConstantFP(new APFloat(val), vt, isTarget);
    }

    public SDValue getConstantFP(double val, EVT vt, boolean isTarget)
    {
        return getConstantFP(new APFloat(val), vt, isTarget);
    }

    private void addNodeToIDNode(FoldingSetNodeID id,
            int opc, SDVTList vtList,
            SDValue... ops)
    {
        id.addInteger(opc);
        id.addInteger(vtList.vts.length);
        for (int i = 0, e = vtList.vts.length; i < e; i++)
            id.addInteger(vtList.vts[i].hashCode());
        for (SDValue op : ops)
            id.addInteger(op.hashCode());
    }
}
