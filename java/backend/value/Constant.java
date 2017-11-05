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

import backend.support.LLVMContext;
import backend.type.IntegerType;
import backend.type.PointerType;
import backend.type.Type;
import backend.utils.InstVisitor;
import tools.APInt;

/**
 * The {@code Constant} instruction represents a constants such as an integer
 * inst, long, float, object reference, address, etc.
 */
public abstract class Constant extends User
{
    /**
     * Constructs a new instruction representing the specified constants.
     */
    public Constant(Type ty, int valueKind)
    {
        super(ty, valueKind);
    }

    public void accept(InstVisitor visitor){}

    public static Constant getNullValue(Type type)
    {
        switch (type.getTypeID())
        {
            case Type.IntegerTyID:
                return ConstantInt.get((IntegerType) type, 0);

            case Type.FloatTyID:
                return ConstantFP.get(LLVMContext.FloatTy, 0);
            case Type.DoubleTyID:
                return ConstantFP.get(LLVMContext.DoubleTy, 0);
            case Type.PointerTyID:
                return ConstantPointerNull.get((PointerType)type);
            case Type.StructTyID:
            case Type.ArrayTyID:
                return ConstantAggregateZero.get(type);
            default:
                return null;
        }
    }

    public abstract boolean isNullValue();

    public static Constant getAllOnesValue(Type ty)
    {
        if (ty instanceof IntegerType)
        {
            APInt val = APInt.getAllOnesValue(((IntegerType) ty).getBitWidth());
            return ConstantInt.get(val);
        }
        return null;
    }

    @Override
    public Constant operand(int index)
    {
        return (Constant) super.operand(index);
    }

    public boolean containsRelocations()
    {
        if (this instanceof GlobalValue)
            return true;
        for (int i = 0, e = getNumOfOperands(); i < e; i++)
            if (operand(i).containsRelocations())
                return true;
        return false;
    }
}
