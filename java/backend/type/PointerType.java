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
 * @author Jianping Zeng
 * @version 0.1
 */
public class PointerType extends SequentialType
{
    private static TypeMap<Type, PointerType> pointerTypes;
    static
    {
        pointerTypes = new TypeMap<>();
    }

    private int addressSpace;

    protected PointerType(Type elemType, int addrSpace)
    {
        super(PointerTyID, elemType);
        addressSpace = addrSpace;

        setAbstract(elemType.isAbstract());
    }

    public static PointerType get(Type valueType, int addrSpace)
    {
        Util.assertion(valueType != null, "Can't get a pointer to <null> type");
        PointerType pt = pointerTypes.get(valueType);
        if (pt != null)
            return pt;

        pt = new PointerType(valueType, addrSpace);
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

    public static boolean isValidElementType(Type eleTy)
    {
        return !(eleTy == LLVMContext.VoidTy || eleTy == LLVMContext.LabelTy);
    }

    public int getAddressSpace()
    {
        return addressSpace;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) return false;
        if (this == obj) return true;
        if (getClass() != obj.getClass())
            return false;
        PointerType ty = (PointerType) obj;
        return numElts == ty.getNumContainedTypes() && isAbstract == ty.isAbstract
                && getAddressSpace() == ty.getAddressSpace() &&
                (getElementType() == ty.getElementType() || getElementType().equals(ty.getElementType()));
    }

    @Override
    public void refineAbstractType(DerivedType oldTy, Type newTy)
    {
        Util.assertion( oldTy != newTy);
        for (int i = 0, e = getNumContainedTypes(); i < e; i++)
        {
            if (containedTys[i].getType() == oldTy)
                containedTys[i].setType(newTy);
        }
        oldTy.abstractTypeUsers.remove(this);
        isAbstract = false;
    }
}
