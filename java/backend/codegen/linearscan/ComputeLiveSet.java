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

package backend.codegen.linearscan;

import backend.analysis.MachineDomTree;
import backend.analysis.MachineLoop;
import backend.analysis.MachineLoopInfo;
import backend.codegen.*;
import backend.pass.AnalysisUsage;
import tools.Util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class ComputeLiveSet extends MachineFunctionPass
{
    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        // Obtains the loop information used for assigning a spilling weight to
        // each live interval. The more nested, the more weight.
        au.addPreserved(MachineDomTree.class);
        au.addRequired(MachineDomTree.class);
        au.addPreserved(MachineLoop.class);
        au.addRequired(MachineLoop.class);
        // Eliminate phi node.
        au.addPreserved(PhiElimination.class);
        au.addRequired(PhiElimination.class);

        // Converts the RISC-like MachineInstr to two addr instruction in some
        // target, for example, X86.
        au.addRequired(TwoAddrInstructionPass.class);

        super.getAnalysisUsage(au);
    }

    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        int size = mf.getNumBlockIDs();
        int[] numIncomingBranches = new int[size];
        MachineLoop ml = (MachineLoop) getAnalysisToUpDate(MachineLoop.class);

        ArrayList<MachineBasicBlock> mbbs = mf.getBasicBlocks();
        for (int i = 0; i < size; i++)
        {
            MachineBasicBlock mbb = mbbs.get(i);
            int num = mbb.getNumPredecessors();
            MachineLoopInfo loopInfo = ml.getLoopFor(mbb);
            if (loopInfo != null)
            {
                for (int j = 0, e = mbb.getNumPredecessors(); j < e; j++)
                {
                    if (loopInfo.contains(mbb.predAt(j)))
                        num--;
                }
            }
            numIncomingBranches[i] = num;
        }

        // Step #1: compute the block order
        ArrayList<MachineBasicBlock> sequence = new ArrayList<>();

        LinkedList<MachineBasicBlock> worklist = new LinkedList<>();
        worklist.add(mf.getEntryBlock());
        while (!worklist.isEmpty())
        {
            MachineBasicBlock cur = worklist.getFirst();
            worklist.removeFirst();
            sequence.add(cur);

            for (Iterator<MachineBasicBlock> itr = cur.succIterator(); itr.hasNext(); )
            {
                MachineBasicBlock succ = itr.next();
                --numIncomingBranches[succ.getNumber()];
                if (numIncomingBranches[succ.getNumber()] == 0)
                {
                    worklist.add(succ);
                }
            }
        }

        if (Util.DEBUG)
        {
            for (MachineBasicBlock bb : sequence)
            {
                System.err.printf("[%s, %d]\n", bb.getName(), bb.getNumber());
            }
        }
        // Step #2: compute local live set.
        return false;
    }

    @Override
    public String getPassName()
    {
        return "Computing live set for each virtual register";
    }
}
