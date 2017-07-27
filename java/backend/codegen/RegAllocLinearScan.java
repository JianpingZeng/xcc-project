package backend.codegen;
/*
 * Extremely C language Compiler
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

import backend.pass.AnalysisUsage;
import backend.target.TargetRegisterClass;
import backend.target.TargetRegisterInfo;
import gnu.trove.map.hash.TIntFloatHashMap;

import java.util.ArrayList;
import java.util.PriorityQueue;

import static backend.target.TargetRegisterInfo.isVirtualRegister;

/**
 * This class implements a MachineFunctionPass used for performing linear scan
 * register allocation on each MachineFunction.
 * <pre>
 * If you want to learn more information, consult the paper
 * Poletto, Massimiliano, and Vivek Sarkar. "Linear scan register allocation.".
 * </pre>
 * @author Xlous.zeng
 * @version 0.1
 */
public class RegAllocLinearScan extends MachineFunctionPass
{
    private PriorityQueue<LiveInterval> unhandled = new PriorityQueue<>();
    private PriorityQueue<LiveInterval> fixed = new PriorityQueue<>();

    private ArrayList<LiveInterval> active = new ArrayList<>();
    private ArrayList<LiveInterval> inactive = new ArrayList<>();
    private ArrayList<LiveInterval> handled = new ArrayList<>();

    private LiveIntervalAnalysis li;
    private int[] physRegUsed;
    private TargetRegisterInfo tri;

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.addRequired(LiveIntervalAnalysis.class);
        super.getAnalysisUsage(au);
    }

    @Override
    public String getPassName()
    {
        return "Linear scan register allocator";
    }

    private void initIntervalSet()
    {
        assert unhandled.isEmpty() && fixed.isEmpty() &&
                active.isEmpty() && inactive.isEmpty();
        for (LiveInterval interval : li.getReg2LiveInterval().values())
        {
            // Add live interval of physical register to fixed set.
            if (TargetRegisterInfo.isPhysicalRegister(interval.register))
            {
                fixed.add(interval);
                physRegUsed[interval.register]++;
            }
            else
            {
                unhandled.add(interval);
            }
        }
    }

    private void linearScan()
    {
        while (!unhandled.isEmpty())
        {
            // remove and obtains the first live interval whose start is first.
            LiveInterval cur = unhandled.poll();

            for (int i = 0; i < active.size(); i++)
            {
                LiveInterval li = active.get(i);
                if (li.expireAt(cur.beginNumber()))
                {
                    active.remove(i);
                    --i;
                    physRegUsed[li.register]--;
                }
                else if (!li.isLiveAt(cur.beginNumber()))
                {
                    active.remove(i);
                    --i;
                    inactive.add(li);
                    physRegUsed[li.register]--;
                }
            }

            for (int i = 0; i < inactive.size(); i++)
            {
                LiveInterval li = inactive.get(i);
                if (li.expireAt(cur.beginNumber()))
                {
                    inactive.remove(i);
                    --i;
                }
                else if (li.isLiveAt(cur.beginNumber()))
                {
                    inactive.remove(i);
                    --i;
                    active.add(li);
                    physRegUsed[li.register]--;
                }
            }

            // if this register is fixed we are done
            if (TargetRegisterInfo.isPhysicalRegister(cur.register))
            {
                physRegUsed[cur.register]++;
                active.add(cur);
                handled.add(cur);
            }
            else
            {
                // otherwise we are allocating a virtual register. try to find
                // a free physical register or spill an interval in order to
                // assign it one (we could spill the current though).
                assignRegOrStackSlot(cur);
            }
        }
    }

    private void updateSpillWeights(int reg, float weight)
    {
        spillWeights[reg] += weight;
        for (int alias : tri.getAliasSet(reg))
            spillWeights[alias] += weight;
    }

    private float[] spillWeights;

    private void assignRegOrStackSlot(LiveInterval cur)
    {
        spillWeights = new float[tri.getNumRegs()];

        // Update spill weight.
        for (LiveInterval li : active)
        {
            int reg = li.register;
            if (isVirtualRegister(reg))
                reg = vrm.getPhys(reg);
            updateSpillWeights(reg, li.weight);
        }

        // for every interval in inactive we overlap with, mark the
        // register as not free and update spill weights.
        for (LiveInterval li : inactive)
        {
            if (cur.overlaps(li))
            {
                int reg = li.register;
                if (isVirtualRegister(reg))
                    reg = vrm.getPhys(reg);
                updateSpillWeights(reg, li.weight);
                physRegUsed[reg]++;
            }
        }

        // for every interval in fixed we overlap with,
        // mark the register as not free and update spill weights
        for (LiveInterval li : fixed)
        {
            if (li.overlaps(cur))
            {
                int reg = li.register;
                updateSpillWeights(reg, li.weight);
                physRegUsed[reg]++;
            }
        }

        // if we find a free register, we are done: assign this virtual to
        // the free physical register and add this interval to the active
        // list.
        int phyReg = getFreePhysReg(cur);
        if (phyReg != 0)
        {
            vrm.assignVirt2Phys(cur.register, phyReg);
            physRegUsed[cur.register]++;
            active.add(cur);
            handled.add(cur);
            return;
        }

        System.err.print("no free register\n");
        System.err.print("\tassigning stack slot at interval");
        cur.print(System.err, tri);
        System.err.println(":");

        float minWeigth = Float.MAX_VALUE;
        int minReg = 0;
        TargetRegisterClass rc = tri.getRegClass(cur.register);
        for (int reg : rc.getAllocableRegs(mf))
        {
            if (spillWeights[reg] <= minWeigth)
            {
                minWeigth = spillWeights[reg];
                minReg = reg;
            }
        }

        System.err.printf("\tregister with min weight: %s (%d)\n",
                tri.getName(minReg),
                minWeigth);

        // if the current has the minimum weight, we need to spill it and
        // add any added intervals back to unhandled, and restart
        // linearscan.
        if (cur.weight <= minWeigth)
        {
            System.err.print("\t\t\tspilling(c):");
            cur.print(System.err, tri);
            System.err.println();

            int slot = vrm.assignVirt2StackSlot(cur.register);
            ArrayList<LiveInterval> added = li.addIntervalsForSpills(cur, vrm, slot);
            if (added.isEmpty())
                return;     // Early exit if all spills were folded.

            // Merge added with unhandled.  Note that we know that
            // addIntervalsForSpills returns intervals sorted by their starting
            // point.
            unhandled.addAll(added);
            return;
        }

        // push the current interval back to unhandled since we are going
        // to re-run at least this iteration. Since we didn't modify it it
        // should go back right in the front of the list
        unhandled.add(cur);


    }

    private int getFreePhysReg(LiveInterval cur)
    {
        TargetRegisterClass rc = tri.getRegClass(cur.register);
        for (int reg : rc.getAllocableRegs(mf))
        {
            if (physRegUsed[reg] == 0)
                return reg;
        }
        return 0;
    }

    private VirtRegMap vrm;
    private MachineFunction mf;
    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        this.mf = mf;
        li = getAnalysisToUpDate(LiveIntervalAnalysis.class);
        physRegUsed = new int[tri.getNumRegs()];

        // Step#1: Initialize interval set.
        initIntervalSet();

        vrm = new VirtRegMap(mf);
        // Step#2:
        linearScan();
        return false;
    }
}
