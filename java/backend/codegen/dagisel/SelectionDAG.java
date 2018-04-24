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

import backend.codegen.*;
import backend.codegen.dagisel.SDNode.*;
import backend.codegen.fastISel.ISD;
import backend.target.TargetLowering;
import backend.target.TargetMachine;
import backend.value.*;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import tools.*;

import java.util.*;

import static backend.codegen.dagisel.SDNode.*;
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
    private FunctionLoweringInfo fli;
    private MachineFunction mf;
    private MachineModuleInfo mmi;
    private SDNode entryNode;
    private SDValue root;
    public ArrayList<SDNode> allNodes;
    private ArrayList<SDVTList> vtlist;
    private ArrayList<CondCodeSDNode> condCodeNodes;
    private TIntObjectHashMap<SDNode> cseMap;

    private HashMap<Pair<String, Integer>, SDNode> targetExternalSymbols = new HashMap<>();
    private HashMap<EVT, SDNode> extendedValueTypeNodes = new HashMap<>();
    private ArrayList<SDNode> valueTypeNodes = new ArrayList<>();
    private HashMap<String, SDNode> externalSymbols = new HashMap<>();

    public SelectionDAG(TargetLowering tl, FunctionLoweringInfo fli)
    {
        target = tl.getTargetMachine();
        tli = tl;
        this.fli = fli;
        mmi = null;
        allNodes = new ArrayList<>();
        vtlist = new ArrayList<>();
        condCodeNodes = new ArrayList<>();
        cseMap = new TIntObjectHashMap<>();
        entryNode = new SDNode(ISD.EntryToken, getVTList(new EVT(MVT.Other)));
        root = getRoot();
        allNodes.add(entryNode);
    }

    public void init(MachineFunction mf, MachineModuleInfo mmi)
    {
        this.mmi = mmi;
        this.mf = mf;
    }

    public TargetMachine getTarget()
    {
        return target;
    }

    public SDValue getNode(int opc, SDVTList vtList, SDValue... ops)
    {
        if (vtList.numVTs == 1)
            return getNode(opc, vtList.vts[0], ops);

        SDNode node;
        if (vtList.vts[vtList.numVTs - 1].getSimpleVT().simpleVT != MVT.Flag)
        {
            FoldingSetNodeID calc = new FoldingSetNodeID();
            addNodeToIDNode(calc, opc, vtList, ops, ops.length);
            int id = calc.computeHash();
            if (cseMap.containsKey(id))
                return new SDValue(cseMap.get(id), 0);

            switch (ops.length)
            {
                default:
                    node = new SDNode(opc, vtList, ops);
                    break;
                case 1:
                    node = new SDNode.UnarySDNode(opc, vtList, ops[0]);
                    break;
                case 2:
                    node = new SDNode.BinarySDNode(opc, vtList, ops[0], ops[1]);
                    break;
                case 3:
                    node = new SDNode.TernarySDNode(opc, vtList, ops[0], ops[1],
                            ops[2]);
                    break;
            }
            cseMap.put(id, node);
        }
        else
        {
            switch (ops.length)
            {
                default:
                    node = new SDNode(opc, vtList, ops);
                    break;
                case 1:
                    node = new SDNode.UnarySDNode(opc, vtList, ops[0]);
                    break;
                case 2:
                    node = new SDNode.BinarySDNode(opc, vtList, ops[0], ops[1]);
                    break;
                case 3:
                    node = new SDNode.TernarySDNode(opc, vtList, ops[0], ops[1],
                            ops[2]);
                    break;
            }
        }
        return new SDValue(node, 0);
    }

    public SDValue getNode(int opc, ArrayList<EVT> vts)
    {
        // TODO: 18-4-21
        return null;
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
            default:
                break;
            case ISD.SELECT_CC:
                assert numOps == 5 : "SELECT_CC takes five operands!";
                assert ops[0].getValueType().equals(ops[1]
                        .getValueType()) : "LHS and RHS of condition must have same type!";
                assert ops[2].getValueType().equals(ops[3]
                        .getValueType()) : "True and False parts of SELECT_CC must have same type!";
                assert ops[2].getValueType()
                        .equals(vt) : "SELECT_CC node must be same type as true/false part!";
                break;
            case ISD.BR_CC:
                assert numOps == 5 : "BR_CC takes five operands!";
                assert ops[2].getValueType().equals(ops[3]
                        .getValueType()) : "LHS/RHS of comparison should match types!";
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

    public SDValue getNode(int opc, EVT vt, ArrayList<SDValue> ops)
    {
        SDValue[] temps = new SDValue[ops.size()];
        ops.toArray(temps);
        return getNode(opc, vt, temps);
    }

    public SDValue getNode(int opc, EVT vt)
    {
        FoldingSetNodeID calc = new FoldingSetNodeID();
        addNodeToIDNode(calc, opc, getVTList(vt), null, 0);
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
            ConstantSDNode cn1 = (ConstantSDNode) op0.getNode();
            APInt val = cn1.getAPIntValue();
            int bitwidth = vt.getSizeInBits();
            switch (opc)
            {
                default:
                    break;
                case ISD.SIGN_EXTEND:
                    return getConstant(new APInt(val).sextOrTrunc(bitwidth), vt,
                            false);
                case ISD.ANY_EXTEND:
                case ISD.ZERO_EXTEND:
                case ISD.TRUNCATE:
                    return getConstant(new APInt(val).zextOrTrunc(bitwidth), vt,
                            false);
                case ISD.UINT_TO_FP:
                case ISD.SINT_TO_FP:
                {
                    long[] zeros = { 0, 0 };
                    if (vt.equals(new EVT(MVT.ppcf128)))
                        break;
                    APFloat apf = new APFloat(new APInt(bitwidth, 2, zeros));
                    apf.convertFromAPInt(val, opc == ISD.SINT_TO_FP,
                            rmNearestTiesToEven);
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
            ConstantFPSDNode fp1 = (ConstantFPSDNode) op0.getNode();
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
                        OutParamWrapper<Boolean> ignored = new OutParamWrapper<>(
                                false);
                        val.convert(EVTToAPFloatSemantics(vt),
                                rmNearestTiesToEven, ignored);
                        return getConstantFP(val, vt, false);
                    }
                    case ISD.FP_TO_SINT:
                    case ISD.FP_TO_UINT:
                    {
                        long[] x = { 0, 0 };
                        OutParamWrapper<Boolean> ignored = new OutParamWrapper<>(
                                false);
                        int opStatus = val
                                .convertToInteger(x, vt.getSizeInBits(), opc == ISD.FP_TO_SINT,
                                        rmTowardZero, ignored);
                        if (opStatus == opInvalidOp)
                            break;
                        APInt res = new APInt(vt.getSizeInBits(), 2, x);
                        return getConstant(res, vt, false);
                    }
                    case ISD.BIT_CONVERT:
                    {
                        if (vt.equals(new EVT(MVT.i32)) && fp1.getValueType(0).equals(new EVT(MVT.f32)))
                            return getConstant((int) (val.bitcastToAPInt().getZExtValue()),
                                    vt, false);
                        if (vt.equals(new EVT(MVT.i64)) && fp1.getValueType(0).equals(new EVT(MVT.f64)))
                            return getConstant(val.bitcastToAPInt().getZExtValue(), vt,
                                    false);
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
                assert vt.isFloatingPoint() && op0.getValueType()
                        .isFloatingPoint() : "Invalid FP cast!";
                if (op0.getValueType().equals(vt))
                    return op0;
                if (op0.getOpcode() == ISD.UNDEF)
                    return getUNDEF(vt);
                break;
            case ISD.SIGN_EXTEND:
                assert vt.isInteger() && op0.getValueType()
                        .isInteger() : "Invalid Integer cast!";
                if (op0.getValueType().equals(vt))
                    return op0;
                assert op0.getValueType()
                        .bitsLT(vt) : "Invalid sext node, dest < src!";
                if (opOpcode == ISD.SIGN_EXTEND || opOpcode == ISD.ZERO_EXTEND)
                    return getNode(opOpcode, vt, op0.getNode().getOperand(0));
                break;
            case ISD.ANY_EXTEND:
            case ISD.ZERO_EXTEND:
                assert vt.isInteger() && op0.getValueType()
                        .isInteger() : "Invalid Integer cast!";
                if (op0.getValueType().equals(vt))
                    return op0;
                assert op0.getValueType()
                        .bitsLT(vt) : "Invalid zext node, dest < src!";
                if (opOpcode == ISD.SIGN_EXTEND || opOpcode == ISD.ZERO_EXTEND)
                    return getNode(opOpcode, vt, op0.getNode().getOperand(0));
                break;

            case ISD.TRUNCATE:
                assert vt.isInteger() && op0.getValueType()
                        .isInteger() : "Invalid Integer cast!";
                if (op0.getValueType().equals(vt))
                    return op0;
                assert op0.getValueType()
                        .bitsGT(vt) : "Invalid truncate node, dest > src!";
                if (opOpcode == ISD.TRUNCATE)
                    return getNode(ISD.TRUNCATE, vt, op0.getNode().getOperand(0));
                else if (opOpcode == ISD.ZERO_EXTEND || opOpcode == ISD.SIGN_EXTEND
                        || opOpcode == ISD.ANY_EXTEND)
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
                assert vt.getSizeInBits() == op0.getValueType()
                        .getSizeInBits() : "Can't perform bit conversion between different size!";
                if (vt.equals(op0.getValueType()))
                    return op0;
                if (opOpcode == ISD.BIT_CONVERT)
                    return getNode(ISD.BIT_CONVERT, vt, op0.getOperand(0));
                if (opOpcode == ISD.UNDEF)
                    return getUNDEF(vt);
                break;
            case ISD.SCALAR_TO_VECTOR:
                assert vt.isVector() && !op0.getValueType().isVector() && (
                        vt.getVectorElementType().equals(op0.getValueType())
                                || (vt.getVectorElementType().isInteger() && op0
                                .getValueType().isInteger() && vt.getVectorElementType().bitsLE(op0
                                .getValueType()))) : "Illegal SCALAR_TO_VECTOR node!";
                if (opOpcode == ISD.UNDEF)
                    return getUNDEF(vt);

                if (opOpcode == ISD.EXTRACT_VECTOR_ELT && (op0.getOperand(0).getNode() instanceof ConstantSDNode)
                        && op0.getConstantOperandVal(1) == 0 && op0.getOperand(0).getValueType().equals(vt))
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

    public SDValue getUNDEF(EVT vt)
    {
        return getNode(ISD.UNDEF, vt);
    }

    public SDValue foldArithmetic(int opcode, EVT vt, ConstantSDNode cst1,
            ConstantSDNode cst2)
    {
        APInt c1 = cst1.getAPIntValue(), c2 = cst2.getAPIntValue();
        switch (opcode)
        {
            case ISD.ADD:
                return getConstant(c1.add(c2), vt, false);
            case ISD.SUB:
                return getConstant(c1.sub(c2), vt, false);
            case ISD.MUL:
                return getConstant(c1.mul(c2), vt, false);
            case ISD.SDIV:
                return getConstant(c1.sdiv(c2), vt, false);
            case ISD.UDIV:
                return getConstant(c1.udiv(c2), vt, false);
            case ISD.UREM:
                return getConstant(c1.urem(c2), vt, false);
            case ISD.SREM:
                return getConstant(c1.srem(c2), vt, false);
            case ISD.AND:
                return getConstant(c1.and(c2), vt, false);
            case ISD.OR:
                return getConstant(c1.or(c2), vt, false);
            case ISD.XOR:
                return getConstant(c1.xor(c2), vt, false);
            //todo case ISD.SRA: return getConstant(c1.sra(c2), vt, false);
            case ISD.SHL:
                return getConstant(c1.shl(c2), vt, false);
        }
        return new SDValue();
    }

    public SDValue getNode(int opc, EVT vt, SDValue op0, SDValue op1)
    {
        ConstantSDNode cn0 = op0.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode) op0.getNode() :
                null;
        ConstantSDNode cn1 = op1.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode) op1.getNode() :
                null;

        switch (opc)
        {
            default:
                break;
            case ISD.TokenFactor:
                assert vt.equals(new EVT(MVT.Other)) && op0.getValueType().equals(new EVT(MVT.Other))
                        && op1.getValueType().equals(new EVT(MVT.Other));
                // fold trivial token factors.
                if (op0.getOpcode() == ISD.EntryToken)
                    return op1;
                if (op1.getOpcode() == ISD.EntryToken)
                    return op0;

                if (op1.equals(op0))
                    return op0;
                break;

            case ISD.CONCAT_VECTORS:
                if (op0.getOpcode() == ISD.BUILD_VECTOR && op1.getOpcode() == ISD.BUILD_VECTOR)
                {
                    SDValue[] vals = new SDValue[op0.getNumOperands() + op1.getNumOperands()];
                    int i = 0;
                    for (int j = 0, e = op0.getNumOperands(); j < e; j++)
                        vals[i++] = op0.getOperand(j);
                    for (int j = 0, e = op1.getNumOperands(); j < e; j++)
                        vals[i++] = op1.getOperand(j);

                    return getNode(ISD.BUILD_VECTOR, vt, vals);
                }
                break;
            case ISD.AND:
                assert vt.isInteger() && op0.getValueType().isInteger() && op1.getValueType()
                        .isInteger() : "Binary operator types must match!";
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
                assert vt.isInteger() && op0.getValueType().isInteger() && op1.getValueType()
                        .isInteger() : "Binary operator types must match!";
                // (X ^|+- 0) = X
                if (cn1 != null && cn1.isNullValue())
                    return op0;
                break;
            case ISD.MUL:
                assert vt.isInteger() && op0.getValueType().isInteger() && op1.getValueType()
                        .isInteger() : "Binary operator types must match!";
                // X * 0 == 0
                if (cn1 != null && cn1.isNullValue())
                    return op0;
                break;
            case ISD.SHL:
            case ISD.SRA:
            case ISD.SRL:
            case ISD.ROTL:
            case ISD.ROTR:
                assert vt.equals(op0
                        .getValueType()) : "Shift operators return tyep must be the same as their arg";
                assert vt.isInteger() && op0.getValueType().equals(op1
                        .getValueType()) : "Shift operator only works on integer type!";

                if (vt.equals(new EVT(MVT.i1)))
                    return op0;
                break;
        }

        if (cn0 != null)
        {
            if (cn1 != null)
            {
                SDValue res = foldArithmetic(opc, vt, cn0, cn1);
                if (res.getNode() != null)
                    return res;
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

        ConstantFPSDNode fp0 = op0.getNode() instanceof ConstantFPSDNode ?
                (ConstantFPSDNode) op0.getNode() :
                null;
        ConstantFPSDNode fp1 = op1.getNode() instanceof ConstantFPSDNode ?
                (ConstantFPSDNode) op1.getNode() :
                null;

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
                    default:
                        break;
                }
            }
        }

        if (op0.getOpcode() == ISD.UNDEF)
        {
            if (isCommutativeBinOp(opc))
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
                        return getConstant(APInt.getAllOnesValue(vt.getSizeInBits()), vt,
                                false);
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

    private static boolean isCommutativeBinOp(int opc)
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
            case ISD.ADDE:
                return true;
            default:
                return false;
        }
    }

    public SDValue getNode(int opc, EVT vt, SDValue op0, SDValue op1, SDValue op2)
    {
        ConstantSDNode cn0 = op0.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode) op0.getNode() :
                null;
        ConstantSDNode cn1 = op1.getNode() instanceof ConstantSDNode ?
                (ConstantSDNode) op1.getNode() :
                null;

        switch (opc)
        {
            case ISD.CONCAT_VECTORS:
            {
                if (op0.getOpcode() == ISD.BUILD_VECTOR && op1.getOpcode() == ISD.BUILD_VECTOR
                        && op2.getOpcode() == ISD.BUILD_VECTOR)
                {
                    SDValue[] vals = new SDValue[op0.getNumOperands() + op1.getNumOperands()
                            + op2.getNumOperands()];
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
                if (op1.equals(op2))
                    return op1;
            }
            case ISD.BRCOND:
                if (cn1 != null)
                {
                    return cn1.getZExtValue() != 0 ?
                            getNode(ISD.BR, new EVT(MVT.Other), op0, op2) :
                            op0;
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
        EVT eltVt = vt.isVector() ? vt.getVectorElementType() : vt;
        assert eltVt.getSizeInBits() >= 64 || (val >> eltVt.getSizeInBits()) + 1
                < 2 : "getConstant with a long value that doesn't fit in type!";
        return getConstant(new APInt(eltVt.getSizeInBits(), val), vt, isTarget);
    }

    public SDValue getConstant(APInt val, EVT vt, boolean isTarget)
    {
        return getConstant(ConstantInt.get(val), vt, isTarget);
    }

    public SDValue getTargetConstant(long val, EVT vt)
    {
        return getConstant(val, vt, true);
    }

    public SDValue getTargetConstant(APInt val, EVT vt)
    {
        return getConstant(val, vt, true);
    }

    public SDValue getTargetConstant(ConstantInt val, EVT vt)
    {
        return getConstant(val, vt, true);
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
        assert vt.isInteger() : "Can't create FP integer constant";
        EVT eltVT = vt.isVector() ? vt.getVectorElementType() : vt;
        assert ci.getBitsWidth() == eltVT
                .getSizeInBits() : "APInt size doesn't match type size!";

        int opc = isTarget ? ISD.TargetConstant : ISD.Constant;
        FoldingSetNodeID id = new FoldingSetNodeID();
        addNodeToIDNode(id, opc, getVTList(eltVT), null, 0);
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
                ops[i] = res;

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
        assert vt
                .isFloatingPoint() : "Can't calling getConstantFP method on non-floating";
        EVT eltVT = vt.isVector() ? vt.getVectorElementType() : vt;

        int opc = isTarget ? ISD.TargetConstantFP : ISD.ConstantFP;
        FoldingSetNodeID id = new FoldingSetNodeID();
        addNodeToIDNode(id, opc, getVTList(eltVT), null, 0);
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
                ops[i] = res;

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

    public SDValue getTargetConstantFP(APFloat val, EVT vt)
    {
        return getConstantFP(val, vt, true);
    }

    public SDValue getTargetConstantFP(float val, EVT vt)
    {
        return getConstantFP(val, vt, true);
    }

    public SDValue getTargetConstantFP(double val, EVT vt)
    {
        return getConstantFP(val, vt, true);
    }

    public SDValue getTargetConstantFP(ConstantFP val, EVT vt)
    {
        return getConstantFP(val, vt, true);
    }

    public static void addNodeToID(FoldingSetNodeID id, SDNode node)
    {
        addNodeToIDNode(id, node.getOpcode(), node.getValueList(), null, 0);
        for (SDUse use : node.getOperandList())
        {
            id.addInteger(use.getNode().hashCode());
            id.addInteger(use.getResNo());
        }
        addCustomToIDNode(id, node);
    }

    private static void addCustomToIDNode(FoldingSetNodeID id, SDNode node)
    {
        switch (node.getOpcode())
        {
            case ISD.TargetExternalSymbol:
            case ISD.ExternalSymbol:
                Util.shouldNotReachHere(
                        "Should only be used on nodes with operands!");
                break;
            default:
                break;
            case ISD.TargetConstant:
            case ISD.Constant:
                id.addInteger(((ConstantSDNode) node).getConstantIntValue().hashCode());
                break;
            case ISD.TargetConstantFP:
            case ISD.ConstantFP:
                id.addInteger(((ConstantFPSDNode) node).getConstantFPValue().hashCode());
                break;
            case ISD.TargetGlobalAddress:
            case ISD.GlobalAddress:
            case ISD.TargetGlobalTLSAddress:
            case ISD.GlobalTLSAddress:
            {
                SDNode.GlobalAddressSDNode addrNode = (SDNode.GlobalAddressSDNode) node;
                id.addInteger(addrNode.getGlobalValue().hashCode());
                id.addInteger(addrNode.getOffset());
                id.addInteger(addrNode.getTargetFlags());
                break;
            }
            case ISD.BasicBlock:
                id.addInteger(((SDNode.BasicBlockSDNode) node).getBasicBlock().hashCode());
                break;
            case ISD.Register:
                id.addInteger(((SDNode.RegisterSDNode) node).getReg());
                break;
            case ISD.MEMOPERAND:
                MachineMemOperand mo = ((SDNode.MemOperandSDNode) node).getMachineMemOperand();
                mo.profile(id);
                break;
            case ISD.FrameIndex:
            case ISD.TargetFrameIndex:
                id.addInteger(((SDNode.FrameIndexSDNode) node).getFrameIndex());
                break;
            case ISD.JumpTable:
            case ISD.TargetJumpTable:
                id.addInteger(((SDNode.JumpTableSDNode) node).getJumpTableIndex());
                break;
            case ISD.ConstantPool:
            case ISD.TargetConstantPool:
            {
                SDNode.ConstantPoolSDNode pool = (SDNode.ConstantPoolSDNode) node;
                id.addInteger(pool.getAlign());
                id.addInteger(pool.getOffset());
                if (pool.isMachineConstantPoolValue())
                    pool.getMachineConstantPoolValue().addSelectionDAGCSEId(id);
                else
                    id.addInteger(pool.getConstantValue().hashCode());
                id.addInteger(pool.getTargetFlags());
                break;
            }
            case ISD.LOAD:
            {
                SDNode.LoadSDNode load = (SDNode.LoadSDNode) node;
                id.addInteger(load.getMemoryVT().getRawBits().hashCode());
                id.addInteger(load.getRawSubclassData());
                break;
            }
            case ISD.STORE:
            {
                SDNode.StoreSDNode store = (SDNode.StoreSDNode) node;
                id.addInteger(store.getMemoryVT().getRawBits().hashCode());
                id.addInteger(store.getRawSubclassData());
                break;
            }
            case ISD.ATOMIC_CMP_SWAP:
            case ISD.ATOMIC_SWAP:
            case ISD.ATOMIC_LOAD_ADD:
            case ISD.ATOMIC_LOAD_SUB:
            case ISD.ATOMIC_LOAD_AND:
            case ISD.ATOMIC_LOAD_OR:
            case ISD.ATOMIC_LOAD_XOR:
            case ISD.ATOMIC_LOAD_NAND:
            case ISD.ATOMIC_LOAD_MIN:
            case ISD.ATOMIC_LOAD_MAX:
            case ISD.ATOMIC_LOAD_UMIN:
            case ISD.ATOMIC_LOAD_UMAX:
            {
                SDNode.AtomicSDNode atomNode = (SDNode.AtomicSDNode) node;
                id.addInteger(atomNode.getMemoryVT().getRawBits().hashCode());
                id.addInteger(atomNode.getRawSubclassData());
                break;
            }
        }
    }

    private static void addNodeToIDNode(
            FoldingSetNodeID id, int opc,
            SDVTList vtList,
            SDValue... ops)
    {
        addNodeToIDNode(id, opc, vtList, ops, ops.length);
    }

    private static void addNodeToIDNode(
            FoldingSetNodeID id,
            int opc, SDVTList vtList,
            SDValue[] ops,
            int numOps)
    {
        id.addInteger(opc);
        id.addInteger(vtList.vts.length);
        for (int i = 0, e = vtList.vts.length; i < e; i++)
            id.addInteger(vtList.vts[i].hashCode());
        for (int i = 0; i < numOps; i++)
            id.addInteger(ops[i].hashCode());
    }

    public void clear()
    {
        allNodes.clear();
        cseMap.clear();
        Collections.fill(condCodeNodes, null);
        entryNode.useList = null;
        allNodes.add(entryNode);
        root = getEntryNode();
    }

    public SDValue getEntryNode()
    {
        return new SDValue(entryNode, 0);
    }

    public SDValue getRoot()
    {
        return root;
    }

    public void setRoot(SDValue root)
    {
        assert root.getNode() == null || root.getValueType().equals(new EVT(MVT.Other)):"Not a legal root!";
        this.root = root;
    }

    public SDValue getMergeValues(ArrayList<SDValue> ops)
    {
        if(ops.size() == 1) return ops.get(0);

        EVT[] vts = ops.stream().map(op->op.getValueType()).toArray(EVT[]::new);
        SDValue[] ops_ = ops.stream().toArray(SDValue[]::new);
        return getNode(ISD.MERGE_VALUES, getVTList(vts), ops_);
    }

    public void removeDeadNodes()
    {
        HandleSDNode dummy = new HandleSDNode(getRoot());

        ArrayList<SDNode> deadNodes = new ArrayList<>();
        for (SDNode node : allNodes)
        {
            if (node.isUseEmpty())
                deadNodes.add(node);
        }

        removeDeadNodes(deadNodes, null);
        setRoot(dummy.getValue());
    }

    public void removeDeadNode(SDNode node, DAGUpdateListener listener)
    {
        ArrayList<SDNode> nodes = new ArrayList<>();
        nodes.add(node);
        removeDeadNodes(nodes, listener);
    }

    public void removeDeadNodes(ArrayList<SDNode> deadNodes, DAGUpdateListener listener)
    {
        for (ListIterator<SDNode> itr = deadNodes.listIterator(); itr.hasNext(); )
        {
            SDNode node = itr.next();
            itr.remove();

            if (listener != null)
                listener.nodeDeleted(node, null);

            removeNodeFromCSEMaps(node);
            for (SDUse use : node.operandList)
            {
                SDNode operand = use.getNode();
                use.set(new SDValue());

                if (operand.isUseEmpty())
                    deadNodes.add(operand);
            }
        }
    }

    private boolean removeNodeFromCSEMaps(SDNode node)
    {
        boolean erased = false;
        switch (node.getOpcode())
        {
            case ISD.EntryToken:
                Util.shouldNotReachHere("EntryToken should not be in cseMap!");
                return false;
            case ISD.HANDLENODE: return false;
            case ISD.CONDCODE:
                assert condCodeNodes.get(((CondCodeSDNode)node).getCondition().ordinal()) != null
                        :"Cond code doesn't exist!";
                erased = true;
                condCodeNodes.set(((CondCodeSDNode)node).getCondition().ordinal(), null);
                break;
            case ISD.TargetExternalSymbol:
                ExternalSymbolSDNode sym = (ExternalSymbolSDNode)node;
                erased = targetExternalSymbols.remove(Pair.get(sym.getExtSymol(),
                        sym.getTargetFlags())) != null;
                break;
            case ISD.VALUETYPE:
                EVT vt = ((VTSDNode)node).getVT();
                if (vt.isExtended())
                    erased = extendedValueTypeNodes.remove(vt) != null;
                else
                {
                    erased = valueTypeNodes.get(vt.getSimpleVT().simpleVT) != null;
                    valueTypeNodes.set(vt.getSimpleVT().simpleVT, null);
                }
                break;
            default:
                // remove it from cseMap.
                for (TIntObjectIterator<SDNode> itr = cseMap.iterator(); itr.hasNext();)
                {
                    if (itr.value().equals(node))
                    {
                        itr.remove();
                        erased = true;
                    }
                }
                break;
        }
        return erased;
    }

    /**
     * Replace any uses of {@code oldNode} with {@code newNode}, leaving uses
     * of other values produced by {@code oldNode.getNode()} alone.
     * @param oldNode
     * @param newNode
     * @param listener
     */
    public void replaceAllUsesOfValueWith(
            SDValue oldNode,
            SDValue newNode,
            DAGUpdateListener listener)
    {
        if (Objects.equals(oldNode, newNode)) return;
        if (oldNode.getNode().getNumValues() == 1)
        {
            // handle trivial case
            replaceAllUsesWith(oldNode.getNode(), newNode.getNode(), listener);
            return;
        }

        ArrayList<SDUse> useList = oldNode.getNode().useList;
        int i = 0, e = useList.size();

        while (i < e)
        {
            SDUse u = useList.get(i);
            SDNode user = u.User;
            boolean userRemovedFromCSEMaps = false;

            do
            {
                u = useList.get(i);
                if (u.getResNo() != oldNode.getResNo())
                {
                    ++i;
                    continue;
                }
                if (!userRemovedFromCSEMaps)
                {
                    removeNodeFromCSEMaps(user);
                    userRemovedFromCSEMaps = true;
                }
                ++i;
                u.set(newNode);
            }while (i < e && useList.get(i).User.equals(user));

            if (!userRemovedFromCSEMaps)
                continue;

            addModifiedNodeToCSEMaps(user, listener);
        }
    }
    private static boolean doNotCSE(SDNode node)
    {
        if (node.getValueType(0).getSimpleVT().simpleVT == MVT.Flag)
            return true;

        switch (node.getOpcode())
        {
            default: break;
            case ISD.HANDLENODE:
            case ISD.DBG_LABEL:
            case ISD.DBG_STOPPOINT:
            case ISD.EH_LABEL:
            case ISD.DECLARE:
                return true;
        }

        for (int i = 0,e = node.getNumValues(); i < e; i++)
        {
            if (node.getValueType(i).getSimpleVT().simpleVT == MVT.Flag)
                return true;
        }
        return false;
    }

    private void addModifiedNodeToCSEMaps(SDNode node, DAGUpdateListener listener)
    {
        if (!doNotCSE(node))
        {
            FoldingSetNodeID compute = new FoldingSetNodeID();
            addNodeToID(compute, node);
            int id = compute.computeHash();
            if (cseMap.containsKey(id))
            {
                SDNode existing = cseMap.get(id);
                replaceAllUsesWith(node, existing, listener);

                if (listener != null)
                    listener.nodeDeleted(node, existing);
                deleteNodeNotInCSEMap(node);
            }
        }
        if (listener != null)
            listener.nodeUpdated(node);
    }

    private void deleteNodeNotInCSEMap(SDNode node)
    {
        assert !Objects.equals(node,allNodes.get(0));
        assert node.isUseEmpty();

        node.dropOperands();
        markNodeAsDeallocated(node);
    }

    private void markNodeAsDeallocated(SDNode node)
    {
        // Mark the node as Deleted for capturing bug.
        node.nodeID = ISD.DELETED_NODE;
    }

    public void replaceAllUsesWith(
            SDNode oldNode,
            SDNode newNode,
            DAGUpdateListener listener)
    {
        if (Objects.equals(oldNode, newNode))
            return;

        ArrayList<SDUse> useList = oldNode.useList;
        int i = 0, e = useList.size();

        while (i < e)
        {
            SDUse u = useList.get(i);
            SDNode user = u.User;
            removeNodeFromCSEMaps(user);

            do
            {
                u = useList.get(i);
                ++i;
                u.setNode(newNode);
            }while (i < e && useList.get(i).User.equals(user));
            addModifiedNodeToCSEMaps(user, listener);
        }
    }

    public void replaceAllUsesWith(
            SDNode from, SDValue to,
            DAGUpdateListener listener)
    {
        if (from.getNumValues() == 1)
        {
            replaceAllUsesWith(new SDValue(from, 0), to, listener);
            return;
        }

        ArrayList<SDUse> useList = from.useList;
        int i = 0, e = useList.size();

        while (i < e)
        {
            SDUse u = useList.get(i);
            SDNode user = u.User;
            removeNodeFromCSEMaps(user);

            do
            {
                u = useList.get(i);
                ++i;
                u.set(to);
            }while (i < e && useList.get(i).User.equals(user));
            addModifiedNodeToCSEMaps(user, listener);
        }
    }

    public void replaceAllUsesWith(
            SDValue oldNode, SDValue newNode,
            DAGUpdateListener listener)
    {
        SDNode from = oldNode.getNode();
        assert from.getNumValues() == 1 && oldNode.getResNo() == 0:
                "Can't replace with this method!";
        assert !Objects.equals(from, newNode.getNode()) :"Can't replace uses of with self!";

        ArrayList<SDUse> useList = from.useList;
        int i = 0, e = useList.size();

        while (i < e)
        {
            SDUse u = useList.get(i);
            SDNode user = u.User;
            removeNodeFromCSEMaps(user);

            do
            {
                u = useList.get(i);
                ++i;
                u.set(newNode);
            }while (i < e && useList.get(i).User.equals(user));
            addModifiedNodeToCSEMaps(user, listener);
        }
    }

    public SDValue getRegister(int reg, EVT ty)
    {
        FoldingSetNodeID calc = new FoldingSetNodeID();
        addNodeToIDNode(calc, ISD.Register, getVTList(ty), null, 0);
        int id = calc.computeHash();
        if (cseMap.containsKey(id))
            return new SDValue(cseMap.get(id), 0);

        SDNode node = new RegisterSDNode(ty, reg);
        cseMap.put(id, node);
        allNodes.add(node);
        return new SDValue(node, 0);
    }

    /**
     * Assign an unique ID for each SDNode in {@linkplain this#allNodes} in
     * the topological order.
     * @return
     */
    public int assignTopologicalOrder()
    {
        int dagSize = 0;
        int insertPos = 0;
        for (int i = 0, e = allNodes.size(); i < e; i++)
        {
            SDNode node = allNodes.get(i);
            int degree = node.getNumOperands();
            if (degree == 0)
            {
                allNodes.add(insertPos++, node);
                allNodes.remove(i);
                node.setNodeID(dagSize++);
            }
            else
            {
                // set the temporary Node ID as the in degree.
                allNodes.get(i).setNodeID(degree);
            }
        }

        for (int i = 0, e = allNodes.size(); i < e; i++)
        {
            SDNode node = allNodes.get(i);
            for (SDUse use : node.useList)
            {
                SDNode user = use.getUser();
                int degree = user.getNodeID();
                --degree;
                if (degree == 0)
                {
                    allNodes.add(insertPos++, node);
                    allNodes.remove(i);
                    node.setNodeID(dagSize++);
                }
                else
                {
                    user.setNodeID(degree);
                }
            }
        }
        SDNode firstNode = allNodes.get(0);
        SDNode lastNode = allNodes.get(allNodes.size()-1);
        assert insertPos == allNodes.size():"Topological incomplete!";
        assert firstNode.getOpcode() == ISD.EntryToken:
                "First node in allNode is not a entry token!";
        assert firstNode.getNodeID() == 0:"First node in allNode does't have zero id!";
        assert lastNode.getNodeID() == allNodes.size()-1:
                "Last node in topological doesn't have "+(allNodes.size()-1) + " id!";
        assert lastNode.isUseEmpty():"Last node in topological shouldn't have use";
        assert dagSize == allNodes.size():"Node count mismatch!";
        return dagSize;
    }

    public static boolean isBuildVectorAllOnes(SDNode n)
    {
        if (n.getOpcode() == ISD.BIT_CONVERT)
            n = n.getOperand(0).getNode();

        if (n.getOpcode() != ISD.BUILD_VECTOR)
            return false;

        int i = 0, e = n.getNumOperands();
        while (i < e && n.getOperand(i).getOpcode() == ISD.UNDEF)
            ++i;

        if (i == e) return false;

        SDValue notZero = n.getOperand(i);
        if (notZero.getNode() instanceof ConstantSDNode)
        {
            if (!((ConstantSDNode)notZero.getNode()).isAllOnesValue())
                return false;
        }
        else if (notZero.getNode() instanceof ConstantFPSDNode)
        {
            if (((ConstantFPSDNode)notZero.getNode()).getValueAPF().bitcastToAPInt().isAllOnesValue())
                return false;
        }
        else
            return false;

        for (++i; i < e; i++)
        {
            if (!n.getOperand(i).equals(notZero) &&
                    n.getOperand(i).getOpcode() != ISD.UNDEF)
                return false;
        }
        return true;
    }

    public static boolean isBuildVectorAllZeros(SDNode n)
    {
        if (n.getOpcode() == ISD.BIT_CONVERT)
            n = n.getOperand(0).getNode();

        if (n.getOpcode() != ISD.BUILD_VECTOR)
            return false;

        int i = 0, e = n.getNumOperands();
        while (i < e && n.getOperand(i).getOpcode() == ISD.UNDEF)
            ++i;

        if (i == e) return false;

        SDValue notZero = n.getOperand(i);
        if (notZero.getNode() instanceof ConstantSDNode)
        {
            if (!((ConstantSDNode)notZero.getNode()).isNullValue())
                return false;
        }
        else if (notZero.getNode() instanceof ConstantFPSDNode)
        {
            if (((ConstantFPSDNode)notZero.getNode()).getValueAPF().isPosZero())
                return false;
        }
        else
            return false;

        for (++i; i < e; i++)
        {
            if (!n.getOperand(i).equals(notZero) &&
                    n.getOperand(i).getOpcode() != ISD.UNDEF)
                return false;
        }
        return true;
    }

    public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt)
    {
        SDVTList vts = getVTList(vt);
        return selectNodeTo(n, targetOpc, vts, null, 0);
    }

    public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt, SDValue op1)
    {
        SDVTList vts = getVTList(vt);
        SDValue[] ops = {op1};
        return selectNodeTo(n, targetOpc, vts, ops, 1);
    }

    public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt, SDValue op1, SDValue op2)
    {
        SDVTList vts = getVTList(vt);
        SDValue[] ops = {op1, op2};
        return selectNodeTo(n, targetOpc, vts, ops, 2);
    }

    public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt, SDValue op1, SDValue op2, SDValue op3)
    {
        SDVTList vts = getVTList(vt);
        SDValue[] ops = {op1, op2, op3};
        return selectNodeTo(n, targetOpc, vts, ops, 3);
    }

    public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt, ArrayList<SDValue> ops)
    {
        SDValue[] temps = new SDValue[ops.size()];
        ops.toArray(temps);
        return selectNodeTo(n, targetOpc, vt, temps, temps.length);
    }

    public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt, SDValue[] ops)
    {
        return selectNodeTo(n, targetOpc, vt, ops, ops.length);
    }

    public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt, SDValue[] ops, int numOps)
    {
        SDVTList vts = getVTList(vt);
        return selectNodeTo(n, targetOpc, vts, ops, numOps);
    }

    public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt1, EVT vt2)
    {
        SDVTList vts = getVTList(vt1, vt2);
        return selectNodeTo(n, targetOpc, vts, null, 0);
    }

    public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt1, EVT vt2, ArrayList<SDValue> ops)
    {
        SDValue[] temps = new SDValue[ops.size()];
        ops.toArray(temps);
        return selectNodeTo(n, targetOpc, vt1, vt2, temps, temps.length);
    }

    public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt1, EVT vt2, SDValue[] ops)
    {
        return selectNodeTo(n, targetOpc, vt1, vt2, ops, ops.length);
    }

    public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt1,
            EVT vt2, SDValue[] ops, int numOps)
    {
        SDVTList vts = getVTList(vt1, vt2);
        return selectNodeTo(n, targetOpc, vts, ops, numOps);
    }

    public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt1, EVT vt2,
            EVT vt3, SDValue[] ops, int numOps)
    {
        SDVTList vts = getVTList(vt1, vt2, vt3);
        return selectNodeTo(n, targetOpc, vts, ops, numOps);
    }

    public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt1,
            EVT vt2, EVT vt3, EVT vt4, SDValue[] ops, int numOps)
    {
        SDVTList vts = getVTList(vt1, vt2, vt3, vt4);
        return selectNodeTo(n, targetOpc, vts, ops, numOps);
    }

    public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt1, EVT vt2,
            SDValue op1)
    {
        SDVTList vts = getVTList(vt1, vt2);
        SDValue[] ops = {op1};
        return selectNodeTo(n, targetOpc, vts, ops, 1);
    }

    public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt1, EVT vt2,
            SDValue op1, SDValue op2)
    {
        SDVTList vts = getVTList(vt1, vt2);
        SDValue[] ops = {op1, op2};
        return selectNodeTo(n, targetOpc, vts, ops, 2);
    }

    public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt1, EVT vt2,
            SDValue op1, SDValue op2, SDValue op3)
    {
        SDVTList vts = getVTList(vt1, vt2);
        SDValue[] ops = {op1, op2, op3};
        return selectNodeTo(n, targetOpc, vts, ops, 3);
    }

    public SDNode selectNodeTo(SDNode n, int targetOpc, EVT vt1, EVT vt2,
            EVT vt3, SDValue op1, SDValue op2, SDValue op3)
    {
        SDVTList vts = getVTList(vt1, vt2, vt3);
        SDValue[] ops = {op1, op2, op3};
        return selectNodeTo(n, targetOpc, vts, ops, 3);
    }

    public SDNode selectNodeTo(SDNode n, int targetOpc, SDVTList vts,
            SDValue[] ops, int numOps)
    {
        return morphNodeTo(n, ~targetOpc, vts, ops, numOps);
    }

    public SDNode morphNodeTo(SDNode n, int opc, SDVTList vts, SDValue[] ops, int numOps)
    {
        int id = 0;
        if (vts.vts[vts.numVTs-1].getSimpleVT().simpleVT == MVT.Flag)
        {
            FoldingSetNodeID compute = new FoldingSetNodeID();
            addNodeToIDNode(compute, opc, vts, ops, numOps);
            id = compute.computeHash();
            if (cseMap.containsKey(id))
                return cseMap.get(id);
        }

        if (!removeNodeFromCSEMaps(n))
        {
            id = 0;
        }

        n.setNodeID(opc);
        n.valueList = vts.vts;

        HashSet<SDNode> deadNodeSet = new HashSet<>();
        for (SDUse use : n.operandList)
        {
            SDNode used = use.getNode();
            use.set(new SDValue());
            if (used.isUseEmpty())
                deadNodeSet.add(used);
        }

        if (numOps > n.getNumOperands())
        {
            n.operandList = new SDUse[numOps];
            Arrays.fill(n.operandList, new SDUse());
        }
        for (int i = 0; i < numOps; i++)
        {
            n.operandList[i].setUser(n);
            n.operandList[i].setInitial(ops[i]);
        }

        ArrayList<SDNode> deadNodes = new ArrayList<>();
        for (SDNode node : deadNodeSet)
        {
            if (node.isUseEmpty())
                deadNodes.add(node);
        }

        removeDeadNodes(deadNodes, null);
        if (id != 0)
            cseMap.put(id, n);
        return n;
    }

	public SDValue getMemOperand(MachineMemOperand memOperand)
	{
		FoldingSetNodeID compute = new FoldingSetNodeID();
		addNodeToIDNode(compute, ISD.MEMOPERAND, getVTList(new EVT(MVT.Other)), null, 0);
		int id = compute.computeHash();
		if (cseMap.containsKey(id))
		    return new SDValue(cseMap.get(id), 0);

        SDNode n = new MemOperandSDNode(memOperand);
        cseMap.put(id, n);
        allNodes.add(n);
        return new SDValue(n, 0);
	}

    public SDNode getTargetNode(int opc, EVT vt)
    {
        return getNode(~opc, vt).getNode();
    }

    public SDNode getTargetNode(int opc, EVT vt, SDValue op1)
    {
        return getNode(~opc, vt, op1).getNode();
    }

    public SDNode getTargetNode(int opc, EVT vt, SDValue op1, SDValue op2)
    {
        return getNode(~opc, vt, op1, op2).getNode();
    }

    public SDNode getTargetNode(int opc, EVT vt, SDValue op1, SDValue op2, SDValue op3)
    {
        return getNode(~opc, vt, op1, op2, op3).getNode();
    }

    public SDNode getTargetNode(int opc, EVT vt, SDValue[] ops)
    {
        return getNode(~opc, vt, ops).getNode();
    }

    public SDNode getTargetNode(int opc, EVT vt1, EVT vt2)
    {
        return getNode(~opc, getVTList(vt1, vt2)).getNode();
    }

    public SDNode getTargetNode(int opc, EVT vt1, EVT vt2, SDValue op1)
    {
        return getNode(~opc, getVTList(vt1, vt2), op1).getNode();
    }

    public SDNode getTargetNode(int opc, EVT vt1, EVT vt2, SDValue op1, SDValue op2)
    {
        return getNode(~opc, getVTList(vt1, vt2), op1, op2).getNode();
    }

    public SDNode getTargetNode(int opc, EVT vt1, EVT vt2, SDValue op1, SDValue op2, SDValue op3)
    {
        return getNode(~opc, getVTList(vt1, vt2), op1, op2, op3).getNode();
    }

    public SDNode getTargetNode(int opc, EVT vt1, EVT vt2, SDValue[] ops)
    {
        return getNode(~opc, getVTList(vt1, vt2), ops).getNode();
    }

    public SDNode getTargetNode(int opc, EVT vt1, EVT vt2, SDValue[] ops, int len)
    {
        assert len >= 0 && len <= ops.length;
        if (ops.length == len)
        {
            SDValue[] temp = new SDValue[len];
            System.arraycopy(ops, 0, temp, 0, len);
            ops = temp;
        }

        return getNode(~opc, getVTList(vt1, vt2), ops).getNode();
    }

    public SDNode getTargetNode(int opc, EVT vt1, EVT vt2, EVT vt3,
            SDValue op1, SDValue op2)
    {
        return getNode(~opc, getVTList(vt1, vt2, vt3), op1, op2).getNode();
    }

    public SDNode getTargetNode(int opc, EVT vt1, EVT vt2, EVT vt3,
            SDValue op1, SDValue op2, SDValue op3)
    {
        return getNode(~opc, getVTList(vt1, vt2, vt3), op1, op2, op3).getNode();
    }

    public SDNode getTargetNode(int opc, EVT vt1, EVT vt2, EVT vt3, SDValue[] ops)
    {
        return getNode(~opc, getVTList(vt1, vt2, vt3), ops).getNode();
    }

    public SDNode getTargetNode(int opc, EVT vt1, EVT vt2, EVT vt3, EVT vt4, SDValue[] ops)
    {
        return getNode(~opc, getVTList(vt1, vt2, vt3, vt4), ops).getNode();
    }

    public SDNode getTargetNode(int opc, ArrayList<EVT> resultTys, SDValue[] ops)
    {
        EVT[] vts = new EVT[resultTys.size()];
        resultTys.toArray(vts);
        return getNode(~opc, getVTList(vts), ops).getNode();
    }

    public SDNode getTargetNode(int opc, EVT[] resultTys, SDValue[] ops)
    {
        return getNode(~opc, getVTList(resultTys), ops).getNode();
    }

    public SDValue getTargetFrameIndex(int fi, EVT vt)
    {
        return getFrameIndex(fi, vt, true);
    }

    public SDValue getGlobalAddress(GlobalValue gv,
            EVT vt, long offset,
            boolean isTargetGA,
            int targetFlags)
    {
        assert targetFlags == 0 || isTargetGA:
                "Can't set target flags on target-independent globals!";
        EVT ptr = new EVT(tli.getPointerTy());
        int bitwidth = ptr.getSizeInBits();
        if (bitwidth < 64)
            offset = (offset << (64-bitwidth)) >> (64 - bitwidth);
        GlobalVariable gvar = gv instanceof GlobalVariable?(GlobalVariable)gv:null;
        int opc;
        if (gvar != null && gvar.isThreadLocal())
            opc = isTargetGA ? ISD.TargetGlobalTLSAddress:ISD.GlobalTLSAddress;
        else
            opc = isTargetGA ? ISD.TargetGlobalAddress:ISD.GlobalAddress;

        FoldingSetNodeID compute = new FoldingSetNodeID();
        addNodeToIDNode(compute, opc, getVTList(vt), null, 0);
        compute.addInteger(gv.hashCode());
        compute.addInteger(offset);
        compute.addInteger(targetFlags);

        int id = compute.computeHash();
        if (cseMap.containsKey(id))
            return new SDValue(cseMap.get(id), 0);
        SDNode n = new GlobalAddressSDNode(opc, vt, gv, offset, targetFlags);
        cseMap.put(id, n);
        allNodes.add(n);
        return new SDValue(n, 0);
    }

    public SDValue getTargetGlobalAddress(GlobalValue gv, EVT vt, long offset, int targetFlags)
    {
        return getGlobalAddress(gv, vt, offset, true, targetFlags);
    }

    public SDValue getFrameIndex(int fi, EVT vt, boolean isTarget)
    {
        int opc = isTarget ? ISD.TargetFrameIndex : ISD.FrameIndex;
        FoldingSetNodeID compute = new FoldingSetNodeID();
        addNodeToIDNode(compute, opc, getVTList(vt), null, 0);
        compute.addInteger(fi);

        int id = compute.computeHash();
        if (cseMap.containsKey(id))
            return new SDValue(cseMap.get(id), 0);
        SDNode n = new FrameIndexSDNode(fi, vt, isTarget);
        cseMap.put(id, n);
        allNodes.add(n);
        return new SDValue(n, 0);
    }

    public SDValue getJumpTable(int jti, EVT vt, boolean isTarget, int targetFlags)
    {
        assert targetFlags == 0 || isTarget:
                "Can't set target flags on target-independent jump table";
        int opc = isTarget ? ISD.TargetJumpTable:ISD.JumpTable;
        FoldingSetNodeID compute = new FoldingSetNodeID();
        addNodeToIDNode(compute, opc, getVTList(vt), null, 0);
        compute.addInteger(jti);
        compute.addInteger(targetFlags);

        int id = compute.computeHash();
        if (cseMap.containsKey(id))
            return new SDValue(cseMap.get(id), 0);
        SDNode n = new JumpTableSDNode(jti, vt, isTarget);
        cseMap.put(id, n);
        allNodes.add(n);
        return new SDValue(n, 0);
    }

    public SDValue getTargetJumpTable(int jti, EVT vt,int targetFlags)
    {
        return getJumpTable(jti, vt, true, targetFlags);
    }

    public SDValue getConstantPool(Constant c, EVT vt,
            int align, int offset,
            boolean isTarget, int targetFlags)
    {
        assert targetFlags == 0 || isTarget:
                "Can't set target flags on target-independent constant pool";
        int opc = isTarget ? ISD.TargetConstantPool:ISD.ConstantPool;
        FoldingSetNodeID compute = new FoldingSetNodeID();
        addNodeToIDNode(compute, opc, getVTList(vt), null, 0);
        compute.addInteger(c.hashCode());
        compute.addInteger(align);
        compute.addInteger(offset);
        compute.addInteger(targetFlags);

        int id = compute.computeHash();
        if (cseMap.containsKey(id))
            return new SDValue(cseMap.get(id), 0);
        SDNode n = new ConstantPoolSDNode(isTarget, c, vt, offset, align, targetFlags);
        cseMap.put(id, n);
        allNodes.add(n);
        return new SDValue(n, 0);
    }

    public SDValue getTargetConstantPool(
            Constant c, EVT vt,
            int align, int offset,
            int targetFlags)
    {
        return getConstantPool(c, vt, align, offset, true, targetFlags);
    }

    public SDValue getBasicBlock(MachineBasicBlock mbb)
    {
        FoldingSetNodeID compute = new FoldingSetNodeID();
        addNodeToIDNode(compute, ISD.BasicBlock, getVTList(new EVT(MVT.Other)), null, 0);
        compute.addInteger(mbb.hashCode());

        int id = compute.computeHash();
        if (cseMap.containsKey(id))
            return new SDValue(cseMap.get(id), 0);
        SDNode n = new BasicBlockSDNode(mbb);
        cseMap.put(id, n);
        allNodes.add(n);
        return new SDValue(n, 0);
    }

    public SDValue getValueType(EVT vt)
    {
        if (vt.isSimple() && vt.getSimpleVT().simpleVT>= valueTypeNodes.size())
        {
            for (int i = valueTypeNodes.size(); i < vt.getSimpleVT().simpleVT; i++)
                valueTypeNodes.add(null);
        }

        SDNode n = vt.isExtended() ? extendedValueTypeNodes.get(vt):
                valueTypeNodes.get(vt.getSimpleVT().simpleVT);
        if (n != null) return new SDValue(n, 0);
        n = new VTSDNode(vt);
        if (vt.isExtended())
            extendedValueTypeNodes.put(vt, n);
        else
            valueTypeNodes.add(vt.getSimpleVT().simpleVT, n);
        allNodes.add(n);
        return new SDValue(n, 0);
    }

    public SDValue getExternalSymbol(String sym, EVT vt)
    {
        if (externalSymbols.containsKey(sym))
            return new SDValue(externalSymbols.get(sym), 0);
        SDNode n = new ExternalSymbolSDNode(false, vt, sym, 0);
        externalSymbols.put(sym, n);
        allNodes.add(n);
        return new SDValue(n, 0);
    }

    public SDValue getTargetExternalSymbol(String sym, EVT vt, int targetFlags)
    {
        Pair<String, Integer> key = Pair.get(sym, targetFlags);
        if (targetExternalSymbols.containsKey(key))
            return new SDValue(targetExternalSymbols.get(key), 0);
        SDNode n = new ExternalSymbolSDNode(true, vt, sym, targetFlags);
        targetExternalSymbols.put(key, n);
        allNodes.add(n);
        return new SDValue(n, 0);
    }

    public SDValue getLabel(int opc, SDValue root, int labelID)
    {
        FoldingSetNodeID compute = new FoldingSetNodeID();
        SDValue[] ops = {root};
        addNodeToIDNode(compute, ISD.BasicBlock, getVTList(new EVT(MVT.Other)), ops);
        compute.addInteger(labelID);

        int id = compute.computeHash();
        if (cseMap.containsKey(id))
            return new SDValue(cseMap.get(id), 0);
        SDNode n = new LabelSDNode(opc, root, labelID);
        cseMap.put(id, n);
        allNodes.add(n);
        return new SDValue(n, 0);
    }

    public SDValue getSrcValue(Value val)
    {
        assert val == null || val.getType() instanceof backend.type.PointerType:
                "SrcValue is not a pointer!";
        FoldingSetNodeID compute = new FoldingSetNodeID();
        addNodeToIDNode(compute, ISD.SRCVALUE, getVTList(new EVT(MVT.Other)), null, 0);
        compute.addInteger(val == null?0:val.hashCode());

        int id = compute.computeHash();
        if (cseMap.containsKey(id))
            return new SDValue(cseMap.get(id), 0);
        SDNode n = new SrcValueSDNode(val);
        cseMap.put(id, n);
        allNodes.add(n);
        return new SDValue(n, 0);
    }
}

