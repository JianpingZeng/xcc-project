/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.transform.scalars;

import backend.pass.AnalysisUsage;
import backend.pass.FunctionPass;
import backend.support.IntStatistic;
import backend.value.BasicBlock;
import backend.value.Function;

import java.util.LinkedList;

/**
 * This pass propagates information about conditional expressions through the
 * program, allowing it to eliminate conditional branches in some cases.
 * @author Xlous.zeng
 * @version 0.1
 */
public class ConditionalPropagate implements FunctionPass
{
    public static IntStatistic NumBrThread =
            new IntStatistic("NumBrThread", "Number of CFG edges threaded through program");
    public static IntStatistic NumSwThread =
            new IntStatistic("NumSwThread", "Number of CFG edges threaded through program");

    private boolean madeChange;
    private LinkedList<BasicBlock> deadBlocks;

    private ConditionalPropagate()
    {
        super();
        madeChange = false;
        deadBlocks = new LinkedList<>();
    }

    /**
     * A static factory method to create an instance of {@linkplain ConditionalPropagate}.
     * @return
     */
    public static ConditionalPropagate createCondPropagatePass()
    {
        return new ConditionalPropagate();
    }

    @Override
    public String getPassName()
    {
        return "Sparse Conditional constant propagate";
    }

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.addRequired(BreakCriticalEdge.class);
    }

    @Override
    public boolean runOnFunction(Function f)
    {
        boolean everMadeChange = false;
        deadBlocks.clear();

        // Keep iterating while the CFG has been changed.
        do
        {
            madeChange = false;
            for (BasicBlock bb : f.getBasicBlockList())
            {
                simplifyBasicBlock(bb);
            }
            everMadeChange |= madeChange;

        }while (madeChange);

        if (everMadeChange)
        {
            while (!deadBlocks.isEmpty())
            {
                deleteDeadBlock(deadBlocks.removeLast());
            }
        }
        return everMadeChange;
    }

    private void simplifyBasicBlock(BasicBlock bb)
    {

    }

    /**
     * Delete the specified dead basic block.
     * @param bb    A dead basic block to be removed from CFG.
     */
    private void deleteDeadBlock(BasicBlock bb)
    {}
}
