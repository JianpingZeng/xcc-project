/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.type;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class PATypeHandle
{
    private Type ty;
    private AbstractTypeUser user;

    private void addUser()
    {
        assert ty != null:"Type handle has a null type";
        if (ty.isAbstract())
            ty.addAbstractTypeUser(user);
    }

    private void removeUser()
    {
        if (ty.isAbstract())
            ty.removeAbstractTypeUser(user);
    }

    public PATypeHandle(Type ty, AbstractTypeUser user)
    {
        this.ty = ty;
        this.user = user;
        addUser();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        if (this == obj)
            return true;

        if (getClass() != obj.getClass())
            return false;

        PATypeHandle handle = (PATypeHandle)obj;
        return ty.equals(handle.ty);
    }

    public Type getType()
    {
        return ty;
    }
}
