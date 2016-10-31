package backend.type;
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
        final int size;

        ArrayValType(final Type val, int sz)
        {
            valType = val;
            size = sz;
        }
    }

    private int numElements;
    private static TypeMap<ArrayValType, ArrayType> arrayTypes;
    static {
        arrayTypes = new TypeMap<>();
    }
    protected ArrayType(Type elemType, int numElts)
    {
        super(ArrayTyID, elemType);
        numElements = numElts;
    }

    public static ArrayType get(Type elemType, int numElts)
    {
        assert elemType!=null:"Can't get array of null types!";
        ArrayValType avt = new ArrayValType(elemType, numElts);
        ArrayType at = arrayTypes.get(avt);
        if (at != null)
            return at;

        // Value not found.  Derive a new type!
        arrayTypes.put(avt, at);
        return at;
    }

    public int getNumElements() { return numElements;}
}
