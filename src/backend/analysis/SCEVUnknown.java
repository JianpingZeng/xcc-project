package backend.analysis;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous
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

import backend.type.Type;
import backend.value.Constant;
import backend.value.ConstantFP;
import backend.value.ConstantInt;
import backend.value.Value;

import java.io.PrintStream;
import java.util.HashMap;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class SCEVUnknown extends SCEV
{
    /**
     * A cache for ensuring that there is only one SCEVUnknown
     * instance for a value.
     */
    private static final HashMap<Value, SCEVUnknown>
            scevUnknowns = new HashMap<>();

    private Value val;
    private SCEVUnknown(Value val)
    {
        super(SCEVType.scUnknown);
        this.val = val;
    }

    public static SCEV get(Value val)
    {
        if (val instanceof ConstantInt)
            return SCEVConstant.get((ConstantInt)val);
        SCEVUnknown res = new SCEVUnknown(val);
        if (!scevUnknowns.containsKey(val))
        {
            scevUnknowns.put(val, res);
            return res;
        }
        return scevUnknowns.get(val);
    }

    public static SCEV getIntegerSCEV(int val, Type ty)
    {
        Constant c;
        if (val == 0)
            c = Constant.getNullValue(ty);
        else if (ty.isFloatingPointType())
            c = ConstantFP.get(ty, val);
        else
        {
            assert ty.isIntegral():"Integral type is required.";
            c = ConstantInt.get(ty, val);
        }
        return get(c);
    }

    public Value getValue()
    {
        return val;
    }

    @Override
    public boolean isLoopInvariant(Loop loop)
    {
        return false;
    }

    @Override
    public boolean hasComputableLoopEvolution(Loop loop)
    {
        return false;
    }

    @Override
    public SCEV replaceSymbolicValuesWithConcrete(SCEV sym, SCEV concrete)
    {
        if(sym.equals(this)) return concrete;
        return this;
    }

    @Override
    public Type getType()
    {
        return val.getType();
    }

    @Override public void print(PrintStream os)
    {

    }
}
