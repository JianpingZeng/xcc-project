package backend.type;
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

import tools.TypeMap;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class PointerType extends SequentialType
{
    private static TypeMap<Type, PointerType> pointerTypes;
    static {
        pointerTypes = new TypeMap<>();
    }

    private int addressSpace;

    protected PointerType(Type elemType, int addrSpace)
    {
        super(PointerTyID, elemType);
        addressSpace = addrSpace;
    }

    public static PointerType get(final Type valueType, int addrSpace)
    {
        assert valueType != null:"Can't get a pointer to <null> type";
        PointerType pt = pointerTypes.get(valueType);
        if (pt != null)
            return pt;
        pointerTypes.put(valueType, pt);
        return pt;
    }

    /**
     * This constructs a pointer to an object of the
     * specified type in the generic address space (address space zero).
     * @param elemType
     * @return
     */
    public static PointerType getUnqual(Type elemType)
    {
        return get(elemType, 0);
    }

    public int getAddressSpace()
    {
        return addressSpace;
    }
}
