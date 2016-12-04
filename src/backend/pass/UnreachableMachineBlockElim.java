package backend.pass;
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

import backend.analysis.MachineDominatorTree;
import backend.analysis.MachineLoopInfo;
import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineFunction;
import backend.codegen.MachineFunctionPass;
import backend.codegen.MachineInstr;
import backend.support.DepthFirstOrder;
import backend.target.x86.X86InstrSets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * This pass used for eliminating the unreachable machine basic block from
 * machine function.
 * It must be performed after LoopInfo and DominatorTree.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class UnreachableMachineBlockElim extends MachineFunctionPass
{
    @Override
    public String getPassName()
    {
        return "Removing the unreachable machine block";
    }

    /**
     * This method must be overridded by concrete subclass for performing
     * desired machine code transformation or analysis.
     *
     * @param mf
     * @return
     */
    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        HashSet<MachineBasicBlock> reachable;

        MachineDominatorTree mdt = getAnalysisToUpDate(MachineDominatorTree.class);
        MachineLoopInfo mli = getAnalysisToUpDate(MachineLoopInfo.class);

        // mark all reachable machine block.
        reachable =  DepthFirstOrder.reversePostOrder(mf.getEntryBlock());

        // loop all dead blocks, remembering them and then deleting all instr
        // in them.
        LinkedList<MachineBasicBlock> deaded = new LinkedList<>();
        for (MachineBasicBlock mbb : mf.getBasicBlocks())
        {
            if (!reachable.contains(mbb))
            {
                deaded.addLast(mbb);

                // update dominator and loop info.
                if (mli != null)
                    mli.removeBlock(mbb);
                if (mdt != null && mdt.getNode(mbb) != null)
                    mdt.eraseNode(mbb);

                for (int i = 0; i < mbb.getNumSucc(); i++)
                {
                    MachineBasicBlock succ = mbb.getSucc(i);

                    for (ListIterator<MachineInstr> itr = succ.getInsts().listIterator();
                            itr.hasNext(); )
                    {
                        MachineInstr mi = itr.next();
                        if (mi.getOpCode() != X86InstrSets.PHI)
                            break;

                        for (int j = mi.getNumOperands() - 1; j >= 2; j-=2)
                        {
                            if (mi.getOperand(j).isMachineBasicBlock()
                                    && mi.getOperand(j).getMBB() == mbb)
                            {
                                mi.removeOperand(j);
                                mi.removeOperand(j-1);
                            }
                        }
                    }
                    mbb.removeSuccessor(0);
                }
            }
        }

        for (MachineBasicBlock dead : deaded)
        {
            // TODO add MachineModuleInfo pass 2016.12.01.
            dead.eraseFromParent();
        }

        // cleanup phi node.
        for (int i = 0, e = mf.getNumBlockIDs(); i < e; i++)
        {
            // Prune the unneeded PHI nodes.
            MachineBasicBlock mbb = mf.getMBBAt(i);
            ArrayList<MachineBasicBlock> pred = mbb.getPredecessors();
            for (int j = 0; j <  mbb.size(); )
            {
                MachineInstr phi = mbb.getInstAt(j);
                if (phi.getOpCode() != X86InstrSets.PHI)
                    continue;
                for (int k = phi.getNumOperands() - 1; k >= 2; k-=2)
                {
                    if (!pred.contains(phi.getOperand(k).getMBB()))
                    {
                        phi.removeOperand(k);
                        phi.removeOperand(k-1);
                    }
                }

                // If this phi have only one input argument, remove it from MBB.
                if (phi.getNumOperands() == 3)
                {
                    int input = phi.getOperand(1).getRegNum();
                    int output = phi.getOperand(0).getRegNum();
                    int phiPos = j;
                    // advance to next inst.
                    j++;

                    // remove this phi inst.
                    mbb.erase(phiPos);
                    if (input != output)
                        mf.replaceRegWith(output, input);
                    continue;
                }
                j++;
            }
        }

        // re-number all blocks in machine function.
        mf.renumberBlocks();

        return !deaded.isEmpty();
    }

    /**
     * Add all required MachineFunctionPasses for UnreachableMachineBlockElim
     * to the PassManager, like MachineLoopInfo and MachineDominatorTree.
     * @param au
     */
    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.addPreserved(MachineLoopInfo.class);
        au.addPreserved(MachineDominatorTree.class);
        super.getAnalysisUsage(au);
    }
}
