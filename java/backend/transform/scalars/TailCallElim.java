package backend.transform.scalars;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

import backend.pass.FunctionPass;
import backend.support.IntStatistic;
import backend.value.BasicBlock;
import backend.value.Function;
import backend.value.Instruction;
import backend.value.Instruction.*;
import backend.value.Value;

import java.util.ArrayList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class TailCallElim implements FunctionPass
{
    public final static IntStatistic NumTailElim =
            new IntStatistic("NumTailElim", "The number of tail call elimination");
    @Override
    public boolean runOnFunction(Function f)
    {
        boolean madeChange = false;

        // Skip the variadic function.
        if (f.isVarArg())
            return false;

        ArrayList<PhiNode> argumentPhis = new ArrayList<>();
        BasicBlock oldEntry = null;
        for (BasicBlock bb : f.getBasicBlockList())
        {
            TerminatorInst ti = bb.getTerminator();
            ReturnInst ri = ti instanceof ReturnInst ? (ReturnInst)bb.getTerminator() : null;
            if (ri != null)
            {
                int riIdx = ri.getIndexToBB();
                // The return instr can not be first.
                if (riIdx >= 1)
                {
                    // The proceeding instruction must be CallInst.
                    if (bb.getInstAt(riIdx - 1) instanceof CallInst)
                    {
                        CallInst ci = (CallInst)bb.getInstAt(riIdx - 1);
                        // The returned value must stem from function calling
                        // unless there is no returned value.
                        if (ci.getCalledFunction().equals(f) &&
                                (ci.getNumOfOperands() == 0 || ri.getReturnValue().equals(ci)))
                        {
                            if (oldEntry == null)
                            {
                                oldEntry = f.getEntryBlock();
                                BasicBlock newEntry = BasicBlock.createBasicBlock("tailrecur", oldEntry);
                                newEntry.appendInst(new BranchInst(oldEntry));

                                Instruction insertBefore = oldEntry.getFirstInst();
                                for (Value arg : f.getArgumentList())
                                {
                                    PhiNode phi = new PhiNode(arg.getType(), arg.getName() + ".phi", insertBefore);
                                    phi.addIncoming(arg, newEntry);
                                    argumentPhis.add(phi);

                                    arg.replaceAllUsesWith(phi);
                                }
                            }

                            for (int i = 0, e = f.getNumOfArgs(); i != e; i++)
                            {
                                argumentPhis.get(i).addIncoming(ci.argumentAt(i), bb);
                            }

                            // Insert a unconditional branch to oldEntry before
                            // the call instruction.
                            new BranchInst(oldEntry, ci);
                            ci.eraseFromParent();
                            ri.eraseFromParent();

                            NumTailElim.inc();
                            madeChange = true;
                        }
                    }
                }
            }
        }
        return madeChange;
    }

    @Override
    public String getPassName()
    {
        return "Tail calling elimination pass";
    }

    public static TailCallElim createTailCallElimination()
    {
        return new TailCallElim();
    }
}
