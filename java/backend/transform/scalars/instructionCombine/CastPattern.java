package backend.transform.scalars.instructionCombine;

import backend.value.Instruction;
import backend.value.Operator;
import backend.value.Value;
import tools.Util;

/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Jianping Zeng.
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
public class CastPattern implements Pattern
{
    private Operator opc;
    private Pattern op;
    private CastPattern(Operator opc, Pattern op)
    {
        this.opc = opc;
        this.op = op;
        Util.assertion(opc.isCastOps(), "Non casting opc for CastPattern.");
    }

    @Override
    public boolean match(Value valueToMatch)
    {
        if (valueToMatch instanceof Instruction)
        {
            Instruction inst = (Instruction) valueToMatch;
            if (inst.getOpcode() == opc)
            {
                return op.match(inst.operand(0));
            }
        }
        return false;
    }
    public static Pattern mTrunc(Pattern op)
    {
        return new CastPattern(Operator.Trunc, op);
    }
    public static Pattern mZExt(Pattern op)
    {
        return new CastPattern(Operator.ZExt, op);
    }
    public static Pattern mSExt(Pattern op)
    {
        return new CastPattern(Operator.SExt, op);
    }
    public static Pattern mFPToSI(Pattern op)
    {
        return new CastPattern(Operator.FPToSI, op);
    }
    public static Pattern mFPToUI(Pattern op)
    {
        return new CastPattern(Operator.FPToUI, op);
    }
    public static Pattern mSIToFP(Pattern op)
    {
        return new CastPattern(Operator.SIToFP, op);
    }
    public static Pattern mUIToFP(Pattern op)
    {
        return new CastPattern(Operator.UIToFP, op);
    }
    public static Pattern mFPTrunc(Pattern op)
    {
        return new CastPattern(Operator.FPTrunc, op);
    }
    public static Pattern mFPExt(Pattern op)
    {
        return new CastPattern(Operator.FPExt, op);
    }
    public static Pattern mIntToPtr(Pattern op)
    {
        return new CastPattern(Operator.IntToPtr, op);
    }
    public static Pattern mPtrToInt(Pattern op)
    {
        return new CastPattern(Operator.PtrToInt, op);
    }
    public static Pattern mBitCast(Pattern op)
    {
        return new CastPattern(Operator.BitCast, op);
    }
}
