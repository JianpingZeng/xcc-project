package backend.opt;
/*
 * Xlous C language Compiler
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

import backend.hir.BasicBlock;
import backend.hir.Operator;
import backend.type.Type;
import backend.value.Constant;
import backend.value.Instruction;
import backend.value.Value;
import backend.value.Value.UndefValue;
import gnu.trove.list.array.TIntArrayList;

/**
 * Performs some local constant folding optimizaiton.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class ConstantFolder
{
    public static boolean constantFoldTerminator(BasicBlock bb)
    {
        return false;
    }

    public static Constant constantFoldCastInstruction(
            Operator opocde,
            Constant val,
            Type destTy)
    {
        return null;
    }

    public static Constant constantFoldBinaryInstruction(
            Operator opcode,
            Constant op1,
            Constant op2)
    {
        return null;
    }

    public static Constant constantFoldCompareInstruction(
            Operator opcode,
            Constant c1,
            Constant c2)
    {
        return null;
    }

    public static Constant constantFoldGetElementPtr(
            Constant c,
            TIntArrayList idxs)
    {
        return null;
    }

    /**
     * This method attempts to fold specified expression to constant.
     * If successfully, the constant value returned, otherwise, null returned.
     *
     * @param inst The instruction to be folded.
     * @return The constant returned if successfully, otherwise, null returned.
     */
    public static Constant constantFoldInstruction(Instruction inst)
    {
        // handle phi nodes here
        if (inst instanceof Instruction.PhiNode)
        {
            Constant commonValue = null;
            Instruction.PhiNode PH = (Instruction.PhiNode) inst;
            for (int i = 0; i < PH.getNumberIncomingValues(); i++)
            {
                Value incomingValue = PH.getIncomingValue(i);

                // if the incoming value is undef and then skip it.
                // Note that while we could skip the valeu if th is equal to the
                // phi node itself because that would break the rules that constant
                // folding only applied if all reservedOperands are constant.
                if (incomingValue instanceof UndefValue)
                    continue;

                // get rid of it, if the incoming value is not a constant
                if (!(incomingValue instanceof Constant))
                    return null;

                Constant constant = (Constant) incomingValue;
                // folds the phi's reservedOperands
                if (commonValue != null && constant != commonValue)
                    return null;
                commonValue = constant;
            }

            return commonValue != null ?
                    commonValue :
                    UndefValue.get(PH.getType());
        }

        // handles other instruction here.
        // inst.accept(this);
        return null;
    }
}
