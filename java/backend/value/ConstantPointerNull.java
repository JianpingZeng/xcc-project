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

import backend.type.PointerType;
import backend.type.Type;

import static backend.value.UniqueConstantValueImpl.getUniqueImpl;

/**
 * A constant pointer value that points to null.
 * @author Xlous.zeng
 * @version 0.1
 */
public class ConstantPointerNull extends Constant
{
    /**
     * Constructs a new instruction representing the specified constant.
     *
     * @param ty
     */
    ConstantPointerNull(Type ty)
    {
        super(ty, ValueKind.ConstantPointerNullVal);
    }

    public static ConstantPointerNull get(Type ty)
    {
        return (ConstantPointerNull) getUniqueImpl().getOrCreate(ty);
    }

    public static void removeConstant(Type ty)
    {
        getUniqueImpl().remove(ty);
    }

    @Override
    public boolean isNullValue()
    {
        return true;
    }

    @Override
    public PointerType getType()
    {
        return (PointerType)super.getType();
    }
}
