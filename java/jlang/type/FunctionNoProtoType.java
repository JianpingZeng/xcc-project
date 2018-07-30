package jlang.type;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import jlang.support.PrintingPolicy;
import tools.FoldingSetNode;
import tools.FoldingSetNodeID;

/**
 * Represents a K&R-style 'int foo()' function, which has
 * no information available about its arguments.
 * @author Jianping Zeng
 * @version 0.1
 */
public final class FunctionNoProtoType extends FunctionType implements
        FoldingSetNode
{
    public FunctionNoProtoType(QualType returnType, QualType canonical)
    {
        this(returnType, canonical, false);
    }

    /**
     * Constructor with one parameter which represents the kind of jlang.type
     * for reason of comparison convenient.
     *
     * @param returnType indicates what jlang.type would be returned.
      *
     */
    public FunctionNoProtoType(QualType returnType, QualType canonical, boolean noReturn)
    {
        super(FunctionNoProto, returnType, null, false);
    }

    @Override
    public String getAsStringInternal(String inner, PrintingPolicy policy)
    {
        if (!inner.isEmpty())
            inner  = "(" + inner + ")";

        inner += "()";
        if (getNoReturnAttr())
            inner += "__attribute__((noreturn))";
        return getResultType().getAsStringInternal(inner, policy);
    }

    @Override
    public int hashCode()
    {
        FoldingSetNodeID id = new FoldingSetNodeID();
        profile(id);
        return id.computeHash();
    }

    @Override
    public void profile(FoldingSetNodeID id)
    {
        id.addInteger(getNoReturnAttr()?1:0);
        id.addInteger(getResultType().hashCode());
    }

    public static void profile(
            FoldingSetNodeID id,
            QualType resultTy,
            boolean noReturn)
    {
        id.addBoolean(noReturn);
        id.addInteger(resultTy.hashCode());
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
        FunctionNoProtoType ft = (FunctionNoProtoType)obj;
        return ft.hashCode() == hashCode();
    }
}
