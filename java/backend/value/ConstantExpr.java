package backend.value;
/*
 * Xlous C language CompilerInstance
 * Copyright (c) 2015-2016, Xlous
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

import backend.transform.scalars.ConstantFolder;
import backend.type.Type;
import backend.value.Instruction.CmpInst.Predicate;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;

import static backend.value.Instruction.CmpInst.Predicate.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class ConstantExpr extends Constant
{
    protected Operator opcode;
    /**
     * Constructs a new instruction representing the specified constants.
     *
     * @param ty
     */
    protected ConstantExpr(Type ty, Operator opcode)
    {
        super(ty, ValueKind.ConstantExprVal);
        this.opcode = opcode;
    }

    @Override
    public boolean isNullValue()
    {
        return false;
    }

    public static Constant getCast(Operator op, Constant c, Type ty)
    {
        assert op.isComparison():"opcode out of range";
        assert c!=null && ty!=null:"Null arguments to getCast";
        assert ty.isFirstClassType():"Cannot cast to an aggregate type!";

        switch (op)
        {
            default:
                Util.shouldNotReachHere("Invalid cast opcode");
                break;
            case Trunc:
                return getTrunc(c, ty);
            case ZExt:
                return getZExt(c, ty);
            case SExt:
                return getSExt(c, ty);
            case FPTrunc:
                return getFPTrunc(c, ty);
            case FPExt:
                return getFPExt(c, ty);
            case UIToFP:
                return getUIToFP(c, ty);
            case SIToFP:
                return getSIToFP(c, ty);
            case FPToSI:
                return getFPToSI(c, ty);
            case FPToUI:
                return getFPToUI(c, ty);
            case PtrToInt:
                return getPtrToInt(c, ty);
            case IntToPtr:
                return getIntToPtr(c, ty);
            case BitCast:
                return getBitCast(c, ty);
        }
        return null;
    }

    public static Constant getBitCast(Constant c, Type ty)
    {
        assert c.getType().getScalarSizeBits() == ty.getScalarSizeBits()
                :"BitCast requires types of same width";

        // It is common to ask for a bitcast of a value to its own type, handle this
        // speedily.
        if (c.getType() == ty)
            return c;

        return getFoldedCast(Operator.BitCast, c, ty);
    }

    public static Constant getIntToPtr(Constant c, Type ty)
    {
        assert ty.isPointerType():"PtrToInt destination must be pointer";
        assert c.getType().isIntegerType():"PtrToInt source must be integral";
        return getFoldedCast(Operator.IntToPtr, c, ty);
    }

    public static Constant getPtrToInt(Constant c, Type ty)
    {
        assert c.getType().isPointerType():"PtrToInt source must be pointer";
        assert ty.isIntegerType():"PtrToInt destination must be integral";
        return getFoldedCast(Operator.PtrToInt, c, ty);
    }

    public static Constant getFPToUI(Constant c, Type ty)
    {
        assert c.getType().isFloatingPointType()&&ty.isIntegerType()
                : "This is an illegal floating point to uint cast!";
        return getFoldedCast(Operator.FPToUI, c, ty);
    }

    public static Constant getFPToSI(Constant c, Type ty)
    {
        assert c.getType().isFloatingPointType()&&ty.isIntegerType()
                : "This is an illegal floating point to sint cast!";
        return getFoldedCast(Operator.FPToSI, c, ty);
    }

    public static Constant getSIToFP(Constant c, Type ty)
    {
        assert c.getType().isIntegerType()&&ty.isFloatingPointType()
                :"This is an illegal uint to floating point cast!";
        return getFoldedCast(Operator.SIToFP, c, ty);
    }

    public static Constant getUIToFP(Constant c, Type ty)
    {
        assert c.getType().isIntegerType()&&ty.isFloatingPointType()
                :"This is an illegal uint to floating point cast!";
        return getFoldedCast(Operator.UIToFP, c, ty);
    }

    public static Constant getFPExt(Constant c, Type ty)
    {
        assert c.getType().isFloatingPointType()
                && ty.isFloatingPointType()
                && c.getType().getScalarSizeBits()<ty.getScalarSizeBits()
                :"This is an illegal floating point extension!";

        return getFoldedCast(Operator.FPExt, c, ty);
    }

    public static Constant getFPTrunc(Constant c, Type ty)
    {
        assert c.getType().isFloatingPointType()
                && ty.isFloatingPointType()
                && c.getType().getScalarSizeBits()>ty.getScalarSizeBits()
                :"This is an illegal floating point truncation!";

        return getFoldedCast(Operator.FPTrunc, c, ty);
    }

    public static Constant getSExt(Constant c, Type ty)
    {
        assert c.getType().isIntegerType():"SExt operand must be integer";
        assert ty.isIntegerType():"SExt produces only integral";
        assert c.getType().getScalarSizeBits() < ty.getScalarSizeBits()
                :"SrcTy must be smaller than DestTy for Trunc!";

        return getFoldedCast(Operator.SExt, c, ty);
    }

    public static Constant getZExt(Constant c, Type ty)
    {
        assert c.getType().isIntegerType():"ZExt operand must be integer";
        assert ty.isIntegerType():"ZExt produces only integral";
        assert c.getType().getScalarSizeBits() < ty.getScalarSizeBits()
                :"SrcTy must be smaller than DestTy for Trunc!";

        return getFoldedCast(Operator.ZExt, c, ty);
    }

    public static Constant getTrunc(Constant c, Type ty)
    {
        assert c.getType().isIntegerType():"Trunc operand must be integer";
        assert ty.isIntegerType():"Trunc produces only integral";
        assert c.getType().getScalarSizeBits()> ty.getScalarSizeBits()
                :"SrcTy must be larger than DestTy for Trunc!";

        return getFoldedCast(Operator.Trunc, c, ty);
    }

    /**
     * This is an utility function to handle folding of casts and lookup for the
     * cast in the ExprConstantMaps.
     * @param op
     * @param c
     * @param ty
     * @return
     */
    static Constant getFoldedCast(Operator op, Constant c, Type ty)
    {
        assert ty.isFirstClassType():"Cannot cast to an aggregate type!";
        // TODO fold a few common cases.

        ExprMapKeyType key = new ExprMapKeyType(op, c);
        Constant val = exprConstanMaps.get(key);
        if (val != null)
            return val;
        val = new UnaryConstExpr(op, c, ty);
        return exprConstanMaps.put(key, val);
    }

    private static HashMap<ExprMapKeyType, Constant> exprConstanMaps
            = new HashMap<>();

    public static Constant getElementPtr(Constant base, ArrayList<Constant> operands)
    {
        // TODO
        return null;
    }

    public static Constant getElementPtr(Constant base, Constant offset, int i)
    {
        // TODO
        return null;
    }

    public Operator getOpCode()
    {
        return opcode;
    }

    public Constant operand(int index)
    {
        return (Constant) super.operand(index);
    }

    public static Constant getNeg(Constant c)
    {
        if (c.getType().isFloatingPointType())
            return getFNeg(c);
        assert c.getType().isIntegerType():"Cann't NEG a non integral value!";
        return get(Operator.Sub, ConstantInt.getNullValue(c.getType()), c);
    }

    public static Constant getFNeg(Constant c)
    {
        assert c.getType().isFloatingPointType()
                : "Can not NEG a non floating point value!";
        return get(Operator.FSub, ConstantFP.getNullValue(c.getType()), c);
    }

    public static Constant get(Operator op, Constant c1, Constant c2)
    {
        if (c1.getType().isFloatingPointType())
        {
            if (op == Operator.Add) op = Operator.FAdd;
            else if (op == Operator.Sub) op = Operator.FSub;
            else if (op == Operator.Mul) op = Operator.FMul;
        }
        ArrayList<Constant> list = new ArrayList<>(2);
        list.add(c1);
        list.add(c2);
        ExprMapKeyType key = new ExprMapKeyType(op, list);
        Constant val = exprConstanMaps.get(key);
        if (val != null) return val;
        val = new BinaryConstantExpr(op, c1, c2);
        exprConstanMaps.put(key, val);
        return val;
    }

    public static Constant getAdd(Constant lhs, Constant rhs)
    {
        return get(Operator.Add, lhs, rhs);
    }

    public static Constant getSub(Constant lhs, Constant rhs)
    {
        return get(Operator.Sub, lhs, rhs);
    }

    public static Constant getMul(Constant lhs, Constant rhs)
    {
        return get(Operator.Mul, lhs, rhs);
    }

    public static Constant getSDiv(Constant lhs, Constant rhs)
    {
        return get(Operator.SDiv, lhs, rhs);
    }

    public static Constant getUDiv(Constant lhs, Constant rhs)
    {
        return get(Operator.UDiv, lhs, rhs);
    }

    public static Constant getShl(Constant lhs, Constant rhs)
    {
        return get(Operator.Shl, lhs, rhs);
    }

    public static Constant getLShr(Constant lhs, Constant rhs)
    {
        return get(Operator.LShr, lhs, rhs);
    }

    public static Constant getAShr(Constant lhs, Constant rhs)
    {
        return get(Operator.AShr, lhs, rhs);
    }

    public static Constant getICmp(Predicate predicate,
            Constant lhs, Constant rhs)
    {
        assert lhs.getType().equals(rhs.getType());
        assert predicate.ordinal() >= FIRST_ICMP_PREDICATE.ordinal()
                && predicate.ordinal() <= LAST_ICMP_PREDICATE.ordinal();
        Constant res = ConstantFolder.constantFoldCompareInstruction(predicate, lhs, rhs);
        if (res != null)
            return res;

        ArrayList<Constant> ops = new ArrayList<>();
        ops.add(lhs);
        ops.add(rhs);
        ExprMapKeyType key = new ExprMapKeyType(Operator.ICmp, ops, predicate);
        return getOrCreate(Type.Int1Ty, key);
    }

    public static Constant getFCmp(Predicate predicate,
            Constant lhs, Constant rhs)
    {
        assert lhs.getType().equals(rhs.getType());
        assert predicate.compareTo(FIRST_FCMP_PREDICATE)>= 0 &&
                predicate.compareTo(LAST_FCMP_PREDICATE) >= 0;

        Constant res = ConstantFolder.constantFoldCompareInstruction(predicate, lhs, rhs);
        if (res != null)
            return res;

        ArrayList<Constant> ops = new ArrayList<>();
        ops.add(lhs);
        ops.add(rhs);
        ExprMapKeyType key = new ExprMapKeyType(Operator.FCmp, ops, predicate);
        return getOrCreate(Type.Int1Ty, key);
    }

    private static Constant getOrCreate(Type reqTy, ExprMapKeyType key)
    {
        if (exprConstanMaps == null)
            exprConstanMaps = new HashMap<>();
        if (!exprConstanMaps.containsKey(key))
        {

        }
        // TODO finish getOrCreate method().
        return null;
    }

    public static Constant getNot(ConstantInt value)
    {
        assert value.getType().isIntegerType();
        return get(Operator.Xor, value, Constant.getAllOnesValue(value.getType()));
    }

    public static Constant getCompareTy(
            Predicate predicate,
            Constant lhs,
            Constant rhs)
    {
        switch (predicate)
        {
            case FCMP_FALSE:  /// 0 0 0 0    Always false (always folded)
            case FCMP_OEQ:  /// 0 0 0 1    True if ordered and equal
            case FCMP_OGT:  /// 0 0 1 0    True if ordered and greater than
            case FCMP_OGE:  /// 0 0 1 1    True if ordered and greater than or equal
            case FCMP_OLT:  /// 0 1 0 0    True if ordered and less than
            case FCMP_OLE:
            case FCMP_ONE:
            case FCMP_ORD:
            case FCMP_UNO:
            case FCMP_UEQ:
            case FCMP_UGT:
            case FCMP_UGE:
            case FCMP_ULT:
            case FCMP_ULE:
            case FCMP_UNE:
            case FCMP_TRUE:
                return getFCmp(predicate, lhs, rhs);
            case ICMP_EQ:  /// equal
            case ICMP_NE:  /// not equal
            case ICMP_UGT:  /// unsigned greater than
            case ICMP_UGE:  /// unsigned greater or equal
            case ICMP_ULT:  /// unsigned less than
            case ICMP_ULE:  /// unsigned less or equal
            case ICMP_SGT:  /// signed greater than
            case ICMP_SGE:  /// signed greater or equal
            case ICMP_SLT:  /// signed less than
            case ICMP_SLE:
                return getICmp(predicate, lhs, rhs);
            default:
                assert false:"Unknown comparison instruction";
                return null;
        }
    }

    public static Constant getCompare(
            Predicate predicate,
            Constant lhs,
            Constant rhs)
    {
        assert lhs.getType().equals(rhs.getType()):"Compare must have same type of operand";
        return getCompareTy(predicate, lhs, rhs);
    }

    public boolean isCast()
    {
        return Instruction.isCast(getOpcode());
    }

    public boolean isCompare()
    {
        return getOpCode() == Operator.ICmp || getOpCode() == Operator.FCmp;
    }

    public Predicate getPredicate()
    {
        assert isCompare();
        return ((CompareConstantExpr)this).predicate;
    }

    static class ExprMapKeyType
    {
        Operator op;
        ArrayList<Constant> constants;
        Predicate predicate;
        ExprMapKeyType(Operator opcode, ArrayList<Constant> ops)
        {
            op = opcode;
            constants = new ArrayList<>(ops.size());
            constants.addAll(ops);
        }

        ExprMapKeyType(Operator opcode, Constant c)
        {
            op = opcode;
            constants = new ArrayList<>(1);
            constants.add(c);
        }

        ExprMapKeyType(Operator opcode, ArrayList<Constant> ops, Predicate pred)
        {
            op = opcode;
            constants = new ArrayList<>(ops.size());
            constants.addAll(ops);
            predicate = pred;
        }
    }
}
