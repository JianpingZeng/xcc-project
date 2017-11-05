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

import backend.support.LLVMContext;
import backend.value.Value;

/**
 * This is the superclass of the array and pointer type
 * classes.  Both of these represent "arrays" in memory.  The array type
 * represents a specifically sized array, pointer types are unsized/unknown getNumOfSubLoop
 * arrays.  SequentialType holds the common features of both, which stem from
 * the fact that both lay their components out in memory identically.
 * @author Xlous.zeng
 * @version 0.1
 */
public class SequentialType extends CompositeType
{
    protected int numElts;
    protected SequentialType(int primitiveID, Type elemType)
    {
        super(primitiveID);
        containedTys = new PATypeHandle[1];
        containedTys[0] = new PATypeHandle(elemType, this);
        numElts = 1;
    }

    public Type getElementType()
    {
        return containedTys[0].getType();
    }

    /**
     * Returns an element type at the specified position.
     * There is only one subtype for sequential type.
     * @param v
     * @return
     */
    @Override
    public Type getTypeAtIndex(Value v)
    {
        return getElementType();
    }

    @Override
    public boolean indexValid(Value v)
    {
        // must be a 'long' index.
        return v.getType() == LLVMContext.Int64Ty;
    }

    @Override
    public Type getIndexType()
    {
        return LLVMContext.Int64Ty;
    }
}
