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

import backend.value.Instruction;
import backend.value.Instruction.CmpInst.Predicate;
import backend.value.Instruction.FCmpInst;
import backend.value.Instruction.ICmpInst;
import backend.value.Value;

public abstract class CmpPattern implements Pattern
{
    protected Predicate pred;
    protected Pattern lhs;
    protected Pattern rhs;
    private CmpPattern(Predicate pred, Pattern lhs, Pattern rhs)
    {
        this.pred = pred;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public static final class ICmpPattern extends CmpPattern
    {
        private ICmpPattern(Predicate pred, Pattern lhs, Pattern rhs)
        {
            super(pred, lhs, rhs);
        }

        @Override
        public boolean match(Value valueToMatch)
        {
            if (valueToMatch instanceof ICmpInst)
            {
                ICmpInst ic = (ICmpInst) valueToMatch;
                return pred == ic.getPredicate() &&
                        lhs.match(ic.operand(0)) &&
                        rhs.match(ic.operand(1));
            }
            return false;
        }
    }

    public static final class FCmpPattern extends CmpPattern
    {
        private FCmpPattern(Predicate pred, Pattern lhs, Pattern rhs)
        {
            super(pred, lhs, rhs);
        }

        @Override
        public boolean match(Value valueToMatch)
        {
            if (valueToMatch instanceof FCmpInst)
            {
                FCmpInst ic = (FCmpInst) valueToMatch;
                return pred == ic.getPredicate() &&
                        lhs.match(ic.operand(0)) &&
                        rhs.match(ic.operand(1));
            }
            return false;
        }
    }

    public static Pattern mICmp(Predicate pred, Pattern lhs, Pattern rhs)
    {
        return new ICmpPattern(pred, lhs, rhs);
    }
    public static Pattern mFCmp(Predicate pred, Pattern lhs, Pattern rhs)
    {
        return new FCmpPattern(pred, lhs, rhs);
    }
}
