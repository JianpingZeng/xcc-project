package backend.value;
/*
 * Extremely C language CompilerInstance
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

import tools.Util;
import backend.support.LLVMContext;
import backend.type.IntegerType;
import backend.type.Type;
import backend.value.UniqueConstantValueImpl.APIntKeyType;
import tools.FoldingSetNodeID;
import tools.APInt;

/**
 * This is an abstract base class of all bool and integral constants.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class ConstantInt extends Constant
{
    private APInt val;

    private static ConstantInt TRUE, FALSE;

    /**
     * Constructs a new instruction representing the specified constants.
     *
     * @param ty
     */
    ConstantInt(IntegerType ty, APInt v)
    {
        super(ty, ValueKind.ConstantIntVal);
        val = v.clone();
        Util.assertion(v.getBitWidth() == ty.getBitWidth(), "Invalid constants for type");
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
        APIntKeyType key = new APIntKeyType(val, ity);
        return UniqueConstantValueImpl.getUniqueImpl().getOrCreate(key);
    }

    public static ConstantInt get(Type ty, long val)
    {
        return get(ty, val, false);
    }

    public static ConstantInt get(Type ty, long val, boolean isSigned)
    {
        return get((IntegerType)ty, val, isSigned);
    }

    public static ConstantInt get(APInt val)
    {
        IntegerType ity = IntegerType.get(val.getBitWidth());
        APIntKeyType key = new APIntKeyType(val, ity);
        return UniqueConstantValueImpl.getUniqueImpl().getOrCreate(key);
    }

    public static ConstantInt getTrue()
    {
        if (TRUE != null)
            return TRUE;
        TRUE = get(LLVMContext.Int1Ty, 1, false);
        return TRUE;
    }

    public static ConstantInt getFalse()
    {
        if (FALSE != null)
            return FALSE;
        return (FALSE = get(LLVMContext.Int1Ty, 1, false));
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

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;

        ConstantInt ci = (ConstantInt)obj;
        return val.eq(ci.getValue());
    }

    @Override
    public int hashCode()
    {
        FoldingSetNodeID id = new FoldingSetNodeID();
        for (long v : val.getRawData())
            id.addInteger(v);

        return id.computeHash();
    }
}
