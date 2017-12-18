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

package backend.codegen;

import backend.pass.AnalysisResolver;
import backend.support.DepthFirstOrder;

import java.util.ArrayList;

/**
 * This file defines a class used for reorder the basic blocks listed in Function
 * for reducing redundant branch instruction in the process of instruction
 * selection.
 * @author Xlous.zeng
 * @version 0.1
 */
public class RearrangementMBB extends MachineFunctionPass
{
    private AnalysisResolver resolver;

    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        if (mf == null) return false;
        ArrayList<MachineBasicBlock> mbbs = DepthFirstOrder.dfs(mf.getEntryBlock());
        mf.setBasicBlocks(mbbs);

        // Loop over the blocks list, reduce useless branch instr
        for (int j = 0, sz = mbbs.size(); j < sz; j++)
        {
            MachineBasicBlock mbb = mbbs.get(j);
            for (int i = 0, e = mbb.size(); i < e; i++)
            {
                MachineInstr mi = mbb.getInstAt(i);
                if (!mi.getDesc().isTerminator())
                    continue;

                // Case 1: the mi is unconditional branch
                if (mi.getDesc().isUnconditionalBranch())
                {
                    MachineBasicBlock target = mi.getOperand(0).getMBB();
                    assert target != null:"Target mbb shouldn't be null!";
                    // if the specified target is a lyaout successor of mbb,
                    // just remove it
                    if (mbb.isLayoutSuccessor(target))
                    {
                        mbb.remove(i);
                        i--;
                        e--;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public String getPassName()
    {
        return "Rearragement Basic Block Pass";
    }

    @Override
    public AnalysisResolver getAnalysisResolver()
    {
        return resolver;
    }

    @Override
    public void setAnalysisResolver(AnalysisResolver resolver)
    {
        this.resolver = resolver;
    }

    public static RearrangementMBB createRearrangeemntPass()
    {
        return new RearrangementMBB();
    }
}
