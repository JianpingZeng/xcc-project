package frontend.type;

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

import frontend.sema.Decl.EnumDecl;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class EnumType extends TagType
{
    /**
     * Constructor with one parameter which represents the kind of frontend.type
     * for reason of comparison convenient.
     *
     * @param decl
     */
    public EnumType(EnumDecl decl)
    {
        super(Enum, decl);
    }

    @Override
    public EnumDecl getDecl() { return (EnumDecl) decl;}

    @Override
    public long getTypeSize()
    {
        return 0;
    }

    @Override
    public boolean isSameType(Type other)
    {
        return false;
    }


    /**
     * Indicates if this frontend.type can be casted into TargetData frontend.type.
     *
     * @param target
     * @return
     */
    @Override
    public boolean isCastableTo(Type target)
    {
        return false;
    }
}
