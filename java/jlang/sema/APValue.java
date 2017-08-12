package jlang.sema;
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

import jlang.ast.Tree;
import backend.support.APSInt;

import java.math.BigDecimal;
import static jlang.sema.APValue.ValueKind.*;

/**
 * This class implements a functionality of [Uninitialized], [APSInt], [APFloat]
 * , [Complex APSInt], [Complex APSFloat].
 * @author Xlous.zeng
 * @version 0.1
 */
public class APValue
{
    public enum ValueKind
    {
        Uninitialized,
        Int,
        Float,
        ComplexInt,
        ComplexFloat,
        LValue,
    }
    private ValueKind kind;

    static class ComplexAPSInt
    {
        APSInt real, imag;
        ComplexAPSInt()
        {
            real = new APSInt(1);
            imag = new APSInt(1);
        }
        ComplexAPSInt(APSInt r, APSInt i)
        {
            real = r;
            imag = i;
        }
    }

    static class ComplexAPFloat
    {
        BigDecimal real, imag;
        ComplexAPFloat()
        {
            real = BigDecimal.ZERO;
            imag = BigDecimal.ZERO;
        }
        ComplexAPFloat(BigDecimal r, BigDecimal i)
        {
            real = r;
            imag = i;
        }
    }

    static class LV
    {
        Tree.Expr base;
        long offset;
    }

    /**
     * A data of jlang.type Object for storing several data of different jlang.type.
     */
    private Object data;

    public APValue()
    {
        kind = Uninitialized;
    }

    public APValue(final APSInt i)
    {
        init(Int, i);
    }
    public APValue(final BigDecimal f)
    {
        init(Float, f);
    }
    public APValue(final APSInt r, final APSInt i)
    {
        init(ComplexInt, new ComplexAPSInt(r, i));
    }
    public APValue(final BigDecimal r, final BigDecimal i)
    {
        init(ComplexFloat, new ComplexAPFloat(r, i));
    }
    public APValue(final Tree.Expr base, final long offset)
    {
        LV lv = new LV();
        lv.base = base;
        lv.offset = offset;
        init(LValue, lv);
    }

    private void init(ValueKind kind, Object x)
    {
        assert isUninit():"Bad status change";
        this.kind = kind;
        data = x;
    }
    public boolean isUninit()
    {
        return kind == Uninitialized;
    }

    public ValueKind getKind() { return kind; }
    public boolean isInt() { return kind == Int; }
    public boolean isFloat() { return kind == Float; }
    public boolean isComplexInt() { return kind == ComplexInt; }
    public boolean isComplexFloat() { return kind == ComplexFloat;}
    public boolean isLValue() { return kind == LValue; }

    public APSInt getInt()
    {
        assert isInt():"Invalid accessor.";
        return ((APSInt) data);
    }

    public BigDecimal getFloat()
    {
        assert isFloat():"Invalid accessor";
        return ((BigDecimal)data);
    }

    public APSInt getComplexIntReal()
    {
        assert isComplexInt():"Invalid accessor";
        return ((ComplexAPSInt)data).real;
    }

    public APSInt getComplexIntImag()
    {
        assert isComplexInt():"Invalid accessor";
        return ((ComplexAPSInt)data).imag;
    }

    public BigDecimal getComplexFloatReal()
    {
        assert isComplexFloat():"Invalid accessor";
        return ((ComplexAPFloat)data).real;
    }

    public BigDecimal getComplexFloatImag()
    {
        assert isComplexFloat():"Invalid accessor";
        return ((ComplexAPFloat)data).imag;
    }

    public Tree.Expr getLValueBase()
    {
        assert isLValue():"Invalid accessor";
        return ((LV)data).base;
    }

    public long getLValueOffset()
    {
        assert isLValue():"Invalid accessor";
        return ((LV)data).offset;
    }

    public void setInt(APSInt i)
    {
        assert isInt():"Invalid accessor.";
        data = i;
    }

    public void setFloat(BigDecimal f)
    {
        assert isFloat():"Invalid accessor.";
        data = f;
    }

    public void setComplexInt(APSInt r, APSInt i)
    {
        assert isComplexInt():"Invalid accessor";
        ((ComplexAPSInt)data).real = r;
        ((ComplexAPSInt)data).imag = i;
    }

    public void setComplexFloat(BigDecimal r, BigDecimal i)
    {
        assert isComplexFloat():"Invalid accessor";
        ((ComplexAPFloat)data).imag = i;
        ((ComplexAPFloat)data).real = r;
    }

    public void setLValue(Tree.Expr base, long offset)
    {
        assert isLValue():"Invalid accessor";
        ((LV)data).base = base;
        ((LV)data).offset = offset;
    }

    @Override
    public boolean equals(Object obj)
    {
        return super.equals(obj);
    }
}
