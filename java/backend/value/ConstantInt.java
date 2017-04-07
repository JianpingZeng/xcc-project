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

import backend.type.IntegerType;
import backend.type.Type;
import jlang.sema.APInt;

import java.util.HashMap;

/**
 * This is an abstract base class of all bool and integral constants.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class ConstantInt extends Constant
{
    static class APIntKeyInfo
    {
        APInt val;
        Type type;

        APIntKeyInfo(APInt v, Type ty)
        {
            val = v;
            type = ty;
        }
    }

    private APInt val;

    private static ConstantInt TRUE, FALSE;

    private static HashMap<APIntKeyInfo, ConstantInt> intConstants;
    static
    {
        intConstants = new HashMap<>();
    }

    /**
     * Constructs a new instruction representing the specified constants.
     *
     * @param ty
     */
    private ConstantInt(IntegerType ty, APInt v)
    {
        super(ty, ValueKind.ConstantIntVal);
        val = v;
        assert v.getBitWidth() == ty.getBitWidth():"Invalid constants for type";
    }

    public static ConstantInt get(IntegerType ty, long val, boolean isSigned)
    {
        return get(ty, new APInt(ty.getBitWidth(), val, isSigned));
    }

    public static ConstantInt get(IntegerType ty, long val)
    {
        return get(ty, new APInt(ty.getBitWidth(), val, false));
    }

    public static ConstantInt get(IntegerType ty, APInt val)
    {
        IntegerType ity = IntegerType.get(ty.getBitWidth());
        APIntKeyInfo key = new APIntKeyInfo(val, ity);
        ConstantInt slot = intConstants.get(key);
        if (slot != null)
            return slot;
        slot = new ConstantInt(ity, val);
        intConstants.put(key, slot);
        return slot;
    }

    public static ConstantInt get(Type ty, long val)
    {return get(ty, val, false);}

    public static ConstantInt get(Type ty, long val, boolean isSigned)
    {
        ConstantInt c = get((IntegerType)ty, val, isSigned);
        return c;
    }

    public static ConstantInt get(APInt val)
    {
        IntegerType ity = IntegerType.get(val.getBitWidth());
        APIntKeyInfo key = new APIntKeyInfo(val, ity);
        ConstantInt slot = intConstants.get(key);
        if (slot != null)
            return slot;
        slot = new ConstantInt(ity, val);
        intConstants.put(key, slot);
        return slot;
    }

    public static ConstantInt getTrue()
    {
        if (TRUE != null)
            return TRUE;
        TRUE = get(Type.Int1Ty, 1, false);
        return TRUE;
    }

    public static ConstantInt getFalse()
    {
        if (FALSE != null)
            return FALSE;
        return (FALSE = get(Type.Int1Ty, 1, false));
    }

    public boolean isMaxValue(boolean isSigned)
    {
        if (isSigned)
            return val.isMaxSignedValue();
        else
            return val.isMaxValue();
    }

    public boolean isMinValue(boolean isSigned)
    {
        if (isSigned)
            return val.isMinSignedValue();
        else
            return val.isMinValue();
    }

    public int getBitsWidth() { return val.getBitWidth();}

    public long getZExtValue() { return val.getZExtValue();}

    public long getSExtValue() { return val.getSExtValue();}

    public boolean equalsInt(long v) { return val.eq(v);}

    public IntegerType getType() { return (IntegerType)super.getType();}

    public boolean isZero() {return val.eq(0);}
    public boolean isOne() {return val.eq(1);}

    @Override
    public boolean isNullValue()
    {
        return val.eq(0);
    }

    public boolean isAllOnesValue(){return val.isAllOnesValue();}

    public APInt getValue() {return val;}
}
