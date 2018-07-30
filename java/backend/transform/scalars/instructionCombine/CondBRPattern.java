package backend.transform.scalars.instructionCombine;

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

import backend.value.BasicBlock;
import backend.value.Instruction.BranchInst;
import backend.value.Value;

public class CondBRPattern implements Pattern
{
    private Pattern cond;
    private BasicBlock trueBB;
    private BasicBlock falseBB;
    public CondBRPattern(Pattern cond)
    {
        this.cond = cond;
    }
    @Override
    public boolean match(Value valueToMatch)
    {
        if (valueToMatch instanceof BranchInst)
        {
            BranchInst br = (BranchInst) valueToMatch;
            if (br.isConditional() && cond.match(br.getCondition()))
            {
                trueBB = br.getSuccessor(0);
                falseBB = br.getSuccessor(1);
                return true;
            }
        }
        return false;
    }

    public BasicBlock getFalseBB()
    {
        return falseBB;
    }

    public BasicBlock getTrueBB()
    {
        return trueBB;
    }

    public static Pattern mBr(Pattern cond)
    {
        return new CondBRPattern(cond);
    }
}
