package jlang.type;
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
import jlang.support.PrintingPolicy;
import tools.FoldingSetNode;
import tools.FoldingSetNodeID;

import java.util.ArrayList;

/**
 * Represents a prototype with argument number and jlang.type info,
 * e.g. <pre>int foo(int)</pre> or <pre>int foo(void)</pre>.
 * @author Jianping Zeng
 * @version 0.1
 */
public final class FunctionProtoType extends FunctionType implements
        FoldingSetNode
{
    private boolean isVariadic;
    private QualType[] argInfo;
    /**
     * Constructor with one parameter which represents the kind of jlang.type
     * for reason of comparison convenient.
     *
     * @param returnType indicates what jlang.type would be returned.
     * @param paramTypes indicates the parameters jlang.type list would be passed into
     *                   function body.
     * @param isVarArgs  indicates if it is variable parameter list.
     */
    public FunctionProtoType(
            QualType returnType,
            ArrayList<QualType> paramTypes,
            boolean isVarArgs,
            QualType canonical,
            boolean noReturn)
    {
        super(FunctionProto, returnType, canonical, noReturn);
        isVariadic = isVarArgs;
        argInfo = new QualType[paramTypes.size()];
        paramTypes.toArray(argInfo);
    }

    public int getNumArgs()
    {
        return argInfo.length;
    }

    public QualType getArgType(int idx)
    {
        Util.assertion( idx >= 0 && idx < getNumArgs());
        return argInfo[idx];
    }

    public boolean isVariadic()
    {
        return isVariadic;
    }

    @Override
    public String getAsStringInternal(String inner, PrintingPolicy policy)
    {
        if (!inner.isEmpty())
            inner  = "(" + inner + ")";

        inner += "(";
        String temp = "";
        PrintingPolicy pp = new PrintingPolicy(policy.opts);
        pp.suppressSpecifiers = false;
        StringBuilder innerBuilder = new StringBuilder(inner);
        for (int i = 0, e = getNumArgs(); i < e; i++)
        {
            if (i != 0) innerBuilder.append(", ");
            innerBuilder.append(getArgType(i).getAsStringInternal(temp, pp));
        }
        inner = innerBuilder.toString();

        if (isVariadic)
        {
            if (getNumArgs() != 0)
                inner += ", ";
            inner += "...";
        }
        else if (getNumArgs() == 0)
        {
            // Do not emit int() if we have a proto, emit 'int(void)'.
            inner += "void";
        }
        inner += ')';
        if (getNoReturnAttr())
            inner += "__attribute__((noreturn))";
        return getResultType().getAsStringInternal(inner, policy);
    }

    @Override
    public void profile(FoldingSetNodeID id)
    {
        id.addInteger(getResultType().hashCode());
        for (int i = 0, e = getNumArgs(); i != e; i++)
            id.addInteger(getArgType(i).hashCode());
        id.addInteger(isVariadic?1:0);
        id.addInteger(getNoReturnAttr()?1:0);
    }

    public static void profile(FoldingSetNodeID id,
            QualType resultTy,
            ArrayList<QualType> argTys,
            boolean isVariadic,
            boolean noReturn)
    {
        id.addInteger(resultTy.hashCode());
        for (QualType argTy : argTys)
            id.addInteger(argTy.hashCode());
        id.addInteger(isVariadic?1:0);
        id.addInteger(noReturn?1:0);
    }

    @Override
    public int hashCode()
    {
        FoldingSetNodeID id = new FoldingSetNodeID();
        profile(id);
        return id.computeHash();
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

        FunctionProtoType ft = (FunctionProtoType)obj;
        return ft.hashCode() == hashCode();
    }

    public int getTypeQuals()
    {
        return super.getTypeQuals();
    }
}
