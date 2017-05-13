package jlang.type;
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

import jlang.basic.PrintingPolicy;

import java.util.ArrayList;

/**
 * Represents a prototype with argument number and jlang.type info,
 * e.g. <pre>int foo(int)</pre> or <pre>int foo(void)</pre>.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class FunctionProtoType extends FunctionType
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
    public FunctionProtoType(QualType returnType, ArrayList<QualType> paramTypes,
            boolean isVarArgs, QualType canonical, boolean noReturn)
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
        assert idx >= 0 && idx < getNumArgs();
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

        inner += "()";
        String temp = "";
        PrintingPolicy pp = new PrintingPolicy(policy.opts);
        pp.suppressSpecifiers = false;
        for (int i = 0, e = getNumArgs(); i < e; i++)
        {
            if (i != 0) inner += ", ";
            inner += getArgType(i).getAsStringInternal(temp, pp);
        }

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
}
