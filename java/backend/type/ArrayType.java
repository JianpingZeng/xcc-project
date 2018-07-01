package backend.type;
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
import tools.TypeMap;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class ArrayType extends SequentialType
{
    static class ArrayValType
    {
        final Type valType;
        final int numElts;
        ArrayValType(Type val, int numElts)
        {
            valType = val;
            this.numElts = numElts;
        }

        @Override
        public int hashCode()
        {
            return numElts << 23 + valType.hashCode() << 11;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null) return false;
            if (this == obj) return true;
            if (getClass() != obj.getClass())
                return false;

            ArrayValType avt = (ArrayValType)obj;
            return avt.valType.equals(valType) && avt.numElts == numElts;
        }
    }

    private static TypeMap<ArrayValType, ArrayType> arrayTypes;
    static
    {
        arrayTypes = new TypeMap<>();
    }

    protected ArrayType(Type elemType, int numElts)
    {
        super(ArrayTyID, elemType);
        this.numElts = numElts;
        setAbstract(elemType.isAbstract());
    }

    public static ArrayType get(Type elemType, long numElements)
    {
        Util.assertion(elemType != null, "Can't get array of null types!");
        ArrayValType avt = new ArrayValType(elemType, (int)numElements);
        ArrayType at = arrayTypes.get(avt);
        if (at != null)
            return at;

        at = new ArrayType(elemType, (int)numElements);

        // Value not found.  Derive a new type!
        arrayTypes.put(avt, at);
        return at;
    }

    public long getNumElements()
    {
        return numElts;
    }

    public static boolean isValidElementType(Type eleTy)
    {
        return !(eleTy == LLVMContext.VoidTy || eleTy == LLVMContext.LabelTy);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;

        if (getClass() != obj.getClass())
            return false;
        ArrayType at = (ArrayType)obj;
        return getNumElements() == at.getNumElements() &&
                getElementType().equals(at.getElementType());
    }
}
