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
import backend.codegen.*;
import backend.pass.AnalysisUsage;
import backend.target.TargetRegisterClass;
import backend.target.TargetRegisterInfo;
import tools.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;

/**
 * This class designed for implementing an advancing linear scan register allocator
 * with live internal splitting.
 *
 * If you want to understand more details about this allocator, please
 * refer to the following papers:
 * <pre>
 * 1. Linear Scan Register Allocation for the Java HotSpot™ Client Compiler, Christian Wimmer.
 * 2. Kotzmann, Thomas, et al. "Design of the Java HotSpot™ client compiler for Java 6." ACM
 *    Transactions on Architecture and Code Optimization (TACO) 5.1 (2008): 7.
 * 3. Wimmer, Christian, and Hanspeter Mössenböck. "Optimized interval splitting in a linear scan register allocator."
 *    Proceedings of the 1st ACM/USENIX international conference on Virtual execution environments. ACM, 2005.
 * 4. Wimmer, Christian, and Michael Franz. "Linear scan register allocation on SSA form."
 *    Proceedings of the 8th annual IEEE/ACM international symposium on Code generation and optimization. ACM, 2010.
 * </pre>
 * @author Xlous.zeng
 * @version 0.1
 */
public final class WimmerLinearScanRegAllocator extends MachineFunctionPass
{
    public static WimmerLinearScanRegAllocator createWimmerLinearScanRegAlloc()
    {
        return new WimmerLinearScanRegAllocator();
    }

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.addRequired(LiveIntervalAnalysis.class);
        // Obtains the loop information used for assigning a spilling weight to
        // each live interval. The more nested, the more weight.
        au.addPreserved(MachineDomTree.class);
        au.addRequired(MachineDomTree.class);
        au.addPreserved(MachineLoop.class);
        au.addRequired(MachineLoop.class);

        super.getAnalysisUsage(au);
    }

    private TreeSet<LiveInterval> unhandled;
    private ArrayList<LiveInterval> handled;
    private ArrayList<LiveInterval> active;
    private ArrayList<LiveInterval> inactive;
    private LiveIntervalAnalysis li;
    private PhysRegTracker prt;
    private TargetRegisterInfo tri;
    private MachineRegisterInfo mri;
    private VirtRegMap vrm;
    private MachineFunction mf;
    private MachineLoop ml;

    public WimmerLinearScanRegAllocator()
    {
        unhandled = new TreeSet<>(Comparator.comparingInt(o -> o.first.start));
        handled = new ArrayList<>();
        active = new ArrayList<>();
        inactive = new ArrayList<>();
    }

    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        unhandled.clear();
        handled.clear();
        active.clear();
        inactive.clear();
        this.mf = mf;
        tri = mf.getTarget().getRegisterInfo();
        prt = new PhysRegTracker(tri);
        mri = mf.getMachineRegisterInfo();
        vrm = new VirtRegMap(mf);

        li = (LiveIntervalAnalysis) getAnalysisToUpDate(LiveIntervalAnalysis.class);
        ml = (MachineLoop) getAnalysisToUpDate(MachineLoop.class);
        Util.assertion(ml != null);
        Util.assertion(li != null);

        // Step #1: initialize the interval list.
        initIntervalSet();

        // walk the intervals
        linearScan();
        Util.assertion(false, "Following waiting to be finished");
        return false;
    }

    private void prehandled(int position)
    {
        // check for intervals in active that are expired or inactive.
        for (int i = 0, e = active.size(); i < e; i++)
        {
            LiveInterval it = active.get(i);
            if (it.isExpiredAt(position))
            {
                active.remove(i);
                --i;
                --e;

                handled.add(it);
            }
            else if (!it.isLiveAt(position))
            {
                active.remove(i);
                --i;
                --e;
                inactive.add(it);
            }
        }

        // checks for intervals in inactive that are expired or active.
        for (int i = 0, e = inactive.size(); i < e; i++)
        {
            LiveInterval it = inactive.get(i);
            if (it.isExpiredAt(position))
            {
                inactive.remove(i);
                --i;
                --e;
                handled.add(it);
            }
            else if (it.isLiveAt(position))
            {
                inactive.remove(i);
                --i;
                --e;
                active.add(it);
            }
        }
    }
    private LiveInterval cur;
    private int position;

    private void linearScan()
    {
        while (!unhandled.isEmpty())
        {
            cur = unhandled.pollFirst();
            Util.assertion(cur != null);

            position = cur.getFirst().start;
            // pre-handling, like move expired interval from active to handled list.
            prehandled(position);

            // if we find a free register, we are done: assign this virtual to
            // the free physical register and add this interval to the active
            // list.
            int phyReg = getFreePhysReg(cur);
            if (phyReg != 0)
            {
                vrm.assignVirt2Phys(cur.register, phyReg);
                prt.addRegUse(phyReg);
                active.add(cur);
                return;
            }

            if (Util.DEBUG)
            {
                System.err.print("no free register\n");
                System.err.print("\ttry to assign a blocked physical register");
                cur.print(System.err, tri);
                System.err.println(":");
            }

            phyReg = allocateBlockedRegister(cur);
            Util.assertion(phyReg != 0, "No register to be assigned!");
        }
    }

    private int allocateBlockedRegister(LiveInterval cur)
    {
        TargetRegisterClass rc = mri.getRegClass(cur.register);
        int[] allocatableRegs = rc.getAllocatableRegs(mf);
        int[] nextUsePos = new int[rc.getNumRegs()];
        // set the free position of all free physical register as
        // integral max value.
        for (int reg : allocatableRegs)
        {
            nextUsePos[reg] = Integer.MAX_VALUE;
        }
        // TODO: 18-7-6
        return -1;
    }

    private int getFreePhysReg(LiveInterval cur)
    {
        TargetRegisterClass rc = mri.getRegClass(cur.register);
        int[] allocatableRegs = rc.getAllocatableRegs(mf);
        int[] freeUntilPos = new int[rc.getNumRegs()];
        // set the free position of all free physical register as
        // integral max value.
        for (int reg : allocatableRegs)
        {
            freeUntilPos[reg] = Integer.MAX_VALUE;
        }
        // set the free position of that register occupied by active
        // as 0.
        for (LiveInterval it : active)
            freeUntilPos[it.register] = 0;

        for (LiveInterval it : inactive)
        {
            if (it.intersect(cur))
                freeUntilPos[it.register] = it.intersectAt(cur);
        }

        int reg = -1;
        int max = -1;
        for (int i = 0; i < freeUntilPos.length; i++)
        {
            if (freeUntilPos[i] > max)
            {
                max = freeUntilPos[i];
                reg = i;
            }
        }
        Util.assertion(reg != -1, "No physical register found!");

        if (freeUntilPos[reg] == 0)
            // allocation failed.
            return 0;
        else if (freeUntilPos[reg] > cur.getLast().end)
        {
            // assign this reg to current interval.
            return reg;
        }
        else
        {
            // register available for first part of current interval.
            // split current at optimal position before freePos[reg].
            splitIntervalWhenPartialAvailable(cur, freeUntilPos[reg]);
            return reg;
        }
    }

    /**
     * Split the it at an optimal position before regAvaliableUntil.
     * @param it
     * @param regAvaliableUntil
     */
    private void splitIntervalWhenPartialAvailable(LiveInterval it, int regAvaliableUntil)
    {
        int minSplitPos = Math.max(it.getUsePointBefore(regAvaliableUntil), it.beginNumber());
        splitBeforeUsage(it, minSplitPos, regAvaliableUntil);
    }

    /**
     * <pre>
     * Find an optimal position, between minSplitPos and maxSplitPos, can be used
     * for splitting the specified LiveInterval.
     *
     * Then left part of it will be assigned a physical register and
     * right part will be inserted into unhandled list for following alloacted.
     * </pre>
     * @param it
     * @param minSplitPos
     * @param maxSplitPos
     */
    private void splitBeforeUsage(LiveInterval it, int minSplitPos, int maxSplitPos)
    {
        Util.assertion(minSplitPos < maxSplitPos);
        Util.assertion(minSplitPos > cur.beginNumber());
        Util.assertion(maxSplitPos < cur.endNumber());
        Util.assertion(minSplitPos > cur.beginNumber());

        int optimalSplitPos = findOptimalSplitPos(it, minSplitPos, maxSplitPos);
        Util.assertion(optimalSplitPos >= minSplitPos &&
                        optimalSplitPos <= maxSplitPos);
        if (optimalSplitPos == cur.endNumber())
        {
            // If the optimal split position is at the end of current interval,
            // so splitting is not at all necessary.
            return;
        }
        LiveInterval rightPart = it.split(optimalSplitPos, this);
        rightPart.setInsertedMove();

        unhandled.add(rightPart);
    }

    private int findOptimalSplitPos(
            MachineBasicBlock minBlock,
            MachineBasicBlock maxBlock,
            int maxSplitPos)
    {
        // Try to split at end of maxBlock. If this would be after
        // maxSplitPos, then use the begin of maxBlock
        int optimalSplitPos = li.mi2Idx.get(maxBlock.getLastInst()) + 2;
        if (optimalSplitPos > maxSplitPos)
        {
            optimalSplitPos = li.mi2Idx.get(maxBlock.getFirstInst());
        }

        int fromBlockId = minBlock.getNumber();
        int toBlockId = maxBlock.getNumber();
        int minLoopDepth = ml.getLoopDepth(maxBlock);

        for (int i = toBlockId - 1; i >= fromBlockId; i--)
        {
            MachineBasicBlock curBB = mf.getMBBAt(i);
            int depth = ml.getLoopDepth(curBB);
            if (depth < minLoopDepth)
            {
                minLoopDepth = depth;
                optimalSplitPos = li.mi2Idx.get(curBB.getLastInst()) + 2;
            }
        }
        return optimalSplitPos;
    }

    private int findOptimalSplitPos(LiveInterval it, int minSplitPos, int maxSplitPos)
    {
        if (minSplitPos == maxSplitPos)
            return minSplitPos;
        MachineBasicBlock minBlock = li.getBlockAtId(minSplitPos-1);
        MachineBasicBlock maxBlock = li.getBlockAtId(maxSplitPos-1);

        if (minBlock.equals(maxBlock))
        {
            return maxSplitPos;
        }
        if (it.hasHoleBetween(maxSplitPos-1, maxSplitPos) &&
                !li.isBlockBegin(maxSplitPos))
        {
            // Do not move split position if the interval has a hole before
            // maxSplitPos. Intervals resulting from Phi-Functions have
            // more than one definition with a hole before each definition.
            // When the register is needed for the second definition, an
            // earlier reloading is unnecessary.
            return maxSplitPos;
        }
        else
        {
            return findOptimalSplitPos(minBlock, maxBlock, maxSplitPos);
        }
    }

    private void initIntervalSet()
    {
        unhandled.addAll(li.intervals.values());
    }

    @Override
    public String getPassName()
    {
        return "Wimmer-Style Linear scan register allocator";
    }
}
