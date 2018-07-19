/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
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

import backend.support.MachineFunctionPass;
import tools.Util;
import backend.pass.AnalysisResolver;
import backend.support.DepthFirstOrder;

import java.util.ArrayList;
import java.util.HashSet;

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

    private void collapseMBB(MachineBasicBlock mbb)
    {
        if (mbb.getNumSuccessors() > 1) return;
        MachineBasicBlock succ = mbb.suxAt(0);
        mbb.removeSuccessor(succ);

        // Avoiding cocurrentModificationException
        HashSet<MachineBasicBlock> handled = new HashSet<>();
        handled.addAll(mbb.getPredecessors());
        for (MachineBasicBlock pred : handled)
        {
            pred.removeSuccessor(mbb);
            pred.addSuccessor(succ);
        }

        MachineFunction mf = mbb.getParent();
        // Replace all mbb operand reference to mbb with succ.
        for (MachineBasicBlock mb : mf.getBasicBlocks())
        {
            for (MachineInstr mi : mb.getInsts())
            {
                for (int i = 0, e = mi.getNumOperands(); i < e; i++)
                {
                    MachineOperand mo = mi.getOperand(i);
                    if (mo.isMBB() && mo.getMBB() == mbb)
                        mo.setMBB(succ);
                }
            }
        }
        mbb.eraseFromParent();
        mf.renumberBlocks();
    }

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
            // merge the empty mbb into it's successor.
            if (mbb.isEmpty())
            {
                collapseMBB(mbb);
                continue;
            }
            for (int i = 0, e = mbb.size(); i < e; i++)
            {
                MachineInstr mi = mbb.getInstAt(i);
                if (!mi.getDesc().isTerminator())
                    continue;

                // Case 1: the mi is unconditional branch
                if (mi.getDesc().isUnconditionalBranch())
                {
                    MachineBasicBlock target = mi.getOperand(0).getMBB();
                    Util.assertion(target != null, "Target mbb shouldn't be null!");
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
            // merge the empty mbb into it's successor.
            if (mbb.isEmpty())
                collapseMBB(mbb);
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
