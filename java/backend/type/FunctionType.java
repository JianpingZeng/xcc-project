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

import java.util.ArrayList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class FunctionType extends Type
{
    static class FunctionValType
    {
        Type retTy;
        ArrayList<Type> argTypes;
        boolean isVarArg;

        public FunctionValType(Type resultType,
                ArrayList<Type> params,
                boolean isVarArg)
        {
            retTy = resultType;
            argTypes = new ArrayList<>(params.size());
            params.forEach(x -> argTypes.add(x));
            this.isVarArg = isVarArg;
        }
    }

    private Type resultType;
    private ArrayList<Type> paramTypes;
    private boolean isVarArgs;

    private static TypeMap<FunctionValType, FunctionType> functionTypes;
    static
    {
        functionTypes = new TypeMap<>();
    }

    private FunctionType(final Type retType,
            final ArrayList<Type> argsType,
            boolean isVarArgs)
    {
        super("",  FunctionTyID);
        resultType = retType;
        this.isVarArgs = isVarArgs;

        paramTypes = new ArrayList<>(argsType.size());
        argsType.forEach((x)->paramTypes.add(x));
    }

    public static FunctionType get(Type result,
            ArrayList<Type> params,
            boolean isVarArgs)
    {
        FunctionValType fvt = new FunctionValType(result, params, isVarArgs);
        FunctionType ft = functionTypes.get(fvt);
        if (ft != null)
            return ft;
        functionTypes.put(fvt, ft);
        return ft;
    }

    public boolean isVarArgs()
    {
        return isVarArgs;
    }

    public Type getReturnType()
    {
        return resultType;
    }

    public Type getParamType(int index)
    {
        assert index>=0 && index< paramTypes.size();
        return paramTypes.get(index);
    }

    public int getNumParams() {return paramTypes.size();}

    public ArrayList<Type> getParamTypes() { return paramTypes;}
}
