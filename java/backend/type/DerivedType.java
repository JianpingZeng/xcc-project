package backend.type;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
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

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class DerivedType extends Type
{
    protected DerivedType(int typeID)
    {
        super(typeID);
    }

    protected void notifyUsesThatTypeBecameConcrete()
    {
        int oldSize = abstractTypeUsers.size();
        while (!abstractTypeUsers.isEmpty())
        {
            AbstractTypeUser user = abstractTypeUsers.getLast();
            user.typeBecameConcrete(this);

            assert abstractTypeUsers.size() < oldSize-- :
                    "AbstractTypeUser did not remove ifself";
        }
    }

    protected void unlockRefineAbstractTypeTo(Type newType)
    {
        assert isAbstract():"refinedAbstractTypeto: Current type is not abstract";
        assert this !=newType:"Can not refine to itself!";

        while (!abstractTypeUsers.isEmpty() && newType != this)
        {
            AbstractTypeUser user = abstractTypeUsers.getLast();
            int oldSize = abstractTypeUsers.size();
            user.refineAbstractType(this, newType);

            assert abstractTypeUsers.size() != oldSize :
                    "AbstractTypeUser did not remove ifself from user list!";
        }
    }

    public void refineAbstractTypeTo(Type newType)
    {
        unlockRefineAbstractTypeTo(newType);
    }

    public void dump()
    {
        super.dump();
    }
}
