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

import backend.type.Type;

/**
 * This is an abstract base class of all bool and integral constant.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class ConstantInteger extends Constant
{
    /**
     * Constructs a new instruction representing the specified constant.
     *
     * @param ty
     */
    public ConstantInteger(Type ty)
    {
        super(ty, ValueKind.ConstantIntVal);
    }

    public abstract boolean isNullValue();

    public abstract boolean isMaxValue();

    public abstract boolean isMinValue();

    /**
     * Returns true if every bit is set to true.
     * @return
     */
    public abstract boolean isAllOnesValue();

    /**
     * Static constructor to create the maximum constant of an integral frontend.type...
     * @param ty
     * @return
     */
    public static ConstantInteger getMaxValue(Type ty)
    {
        switch (ty.getPrimitiveID())
        {
            case Type.Int1TyID:
                return ConstantBool.True;
            case Type.SByteTyID:
            case Type.ShortTyID:
            case Type.IntTyID:
            case Type.LongTyID:
            {
                // Calculate 011111111111111.
                long typeBits = ty.getPrimitiveSize() << 3;
                long val = -1;
                val <<= typeBits - 1;
                return ConstantInt.ConstantSInt.get(ty, val);
            }
            case Type.Int8TyID:
            case Type.Int16TyID:
            case Type.Int32TyID:
            case Type.Int64TyID:
            {
                return getAllOnesValue(ty);
            }
            default:
                return null;
        }
    }

    /**
     * Static constructor to create the minimum constant for an integral frontend.type...
     * @param ty
     * @return
     */
    public static ConstantInteger getMinValue(Type ty)
    {
        switch (ty.getPrimitiveID())
        {
            case Type.Int1TyID:
                return ConstantBool.False;
            case Type.SByteTyID:
            case Type.ShortTyID:
            case Type.IntTyID:
            case Type.LongTyID:
            {
                // Calculate 1111111111000000000000
                long typeBits = ty.getPrimitiveSize()<<3;
                long val = Long.MAX_VALUE;
                val >>>= 64 - typeBits;
                return ConstantInt.ConstantSInt.get(ty, val);
            }
            case Type.Int8TyID:
            case Type.Int16TyID:
            case Type.Int32TyID:
            case Type.Int64TyID:
            {
                return ConstantInt.ConstantUInt.get(ty, 0);
            }
            default:
                return null;
        }
    }

    public static ConstantInteger getAllOnesValue(Type ty)
    {
        switch (ty.getPrimitiveID())
        {
            case Type.Int1TyID:
                return ConstantBool.True;
            case Type.SByteTyID:
            case Type.ShortTyID:
            case Type.IntTyID:
            case Type.LongTyID:
            {
                return ConstantInt.ConstantSInt.get(ty, -1);
            }
            case Type.Int8TyID:
            case Type.Int16TyID:
            case Type.Int32TyID:
            case Type.Int64TyID:
            {
                long typeBits = ty.getPrimitiveSize()<<3;
                long val = ~0L;
                val >>>= 64 - typeBits;
                return ConstantInt.ConstantUInt.get(ty, val);
            }
            default:
                return null;
        }
    }
}
