package backend.value;
/*
 * Xlous C language Compiler
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

import backend.hir.Operator;
import backend.type.Type;
import tools.Util;

import java.util.HashMap;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class ConstantExpr extends Constant
{
    protected Operator opcode;
    /**
     * Constructs a new instruction representing the specified constant.
     *
     * @param ty
     */
    public ConstantExpr(Type ty, Operator opcode)
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

    static HashMap<ExprMapKeyType, Constant> exprConstanMaps;
    static
    {
        exprConstanMaps = new HashMap<>();
    }

    public static Constant getElementPtr(Constant casted, Constant offset, int i)
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

    static class ExprMapKeyType
    {
        Operator op;
        Constant constant;
        ExprMapKeyType(Operator opcode, Constant c)
        {
            op = opcode;
            constant = c;
        }
    }
}
