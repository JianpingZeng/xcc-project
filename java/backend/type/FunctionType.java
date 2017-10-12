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
public class FunctionType extends DerivedType
{
    static class FunctionValType
    {
        Type retTy;
        ArrayList<Type> argTypes;
        boolean isVarArg;

        public FunctionValType(Type resultType, ArrayList<Type> params,
                boolean isVarArg)
        {
            retTy = resultType;
            argTypes = new ArrayList<>(params.size());
            params.forEach(x -> argTypes.add(x));
            this.isVarArg = isVarArg;
        }

        @Override public int hashCode()
        {
            return retTy.hashCode() << 23 + argTypes.hashCode() << 11 + (
                    isVarArg ?
                            1 :
                            0);
        }

        @Override public boolean equals(Object obj)
        {
            if (obj == null)
                return false;
            if (this == obj)
                return true;
            if (getClass() != obj.getClass())
                return false;

            FunctionValType fvt = (FunctionValType) obj;
            return retTy.equals(fvt.retTy) && argTypes.equals(fvt.argTypes)
                    && isVarArg == fvt.isVarArg;
        }
    }

    private boolean isVarArgs;

    private static TypeMap<FunctionValType, FunctionType> functionTypes = new TypeMap<>();

    private FunctionType(Type retType, final ArrayList<Type> argsType,
            boolean isVarArgs)
    {
        super(FunctionTyID);
        containedTys = new PATypeHandle[argsType.size() + 1];
        containedTys[0] = new PATypeHandle(retType, this);
        this.isVarArgs = isVarArgs;
        isAbstract = retType.isAbstract();
        int i = 1;
        for (Type argTy : argsType)
        {
            assert isValidArgumentType(argTy)
                    : "Not a valid type for function argument";
            containedTys[i++] = new PATypeHandle(argTy, this);
            isAbstract |= argTy.isAbstract();
        }

        setAbstract(isAbstract);
    }

    public static FunctionType get(Type result, ArrayList<Type> params,
            boolean isVarArgs)
    {
        FunctionValType fvt = new FunctionValType(result, params, isVarArgs);
        FunctionType ft = functionTypes.get(fvt);
        if (ft != null)
            return ft;
        ft = new FunctionType(result, params, isVarArgs);
        functionTypes.put(fvt, ft);
        return ft;
    }

    public static FunctionType get(Type resultType, boolean isVarArgs)
    {
        return get(resultType, new ArrayList<>(), isVarArgs);
    }

    public static boolean isValidArgumentType(Type argTy)
    {
        return argTy.isFirstClassType() || (argTy instanceof OpaqueType);
    }

    public boolean isValidReturnType(Type retType)
    {
        if (retType.isFirstClassType())
        {
            return true;
        }

        if (retType == Type.VoidTy || retType instanceof OpaqueType)
            return true;

        if (!(retType instanceof StructType) || ((StructType)retType).getNumOfElements() == 0)
            return false;

        StructType st = (StructType)retType;
        for (int i = 0, e = st.getNumOfElements(); i < e; i++)
            if (!st.getElementType(i).isFirstClassType())
                return false;
        return true;
    }

    public boolean isVarArg()
    {
        return isVarArgs;
    }

    public Type getReturnType()
    {
        return containedTys[0].getType();
    }

    public Type getParamType(int index)
    {
        assert index >= 0 && index < getNumParams();
        return containedTys[index+1].getType();
    }

    public int getNumParams()
    {
        return containedTys.length - 1;
    }
}
