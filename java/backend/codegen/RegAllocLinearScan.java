package backend.codegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
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

import backend.analysis.MachineDomTree;
import backend.analysis.MachineLoop;
import backend.pass.AnalysisUsage;
import backend.support.EquivalenceClass;
import backend.target.TargetRegisterClass;
import backend.target.TargetRegisterInfo;
import gnu.trove.set.hash.TIntHashSet;
import tools.Util;

import java.util.*;

import static backend.target.TargetRegisterInfo.isPhysicalRegister;
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
    private TreeSet<LiveInterval> unhandled;
    private ArrayList<LiveInterval> fixed;

    private ArrayList<LiveInterval> active;
    private ArrayList<LiveInterval> inactive;
    private LinkedList<LiveInterval> handled;

    private LiveIntervalAnalysis li;
    private PhysRegTracker prt;
    private TargetRegisterInfo tri;
    private MachineRegisterInfo mri;
    private VirtRegMap vrm;
    private MachineFunction mf;
    private float[] spillWeights;
    private VirtRegRewriter rewriter;
    private LiveStackSlot ls;
    /**
     * This structure is built the first time a function is
     * compiled, and keeps track of which register classes have registers that
     * belong to multiple classes or have aliases that are in other classes.
     */
    private EquivalenceClass<TargetRegisterClass> relatedRegisterClasses;

    private HashMap<Integer, TargetRegisterClass> oneClassForEachPhysReg;

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.setPreservesCFG();
        au.addRequired(LiveIntervalAnalysis.class);
        au.addRequired(LiveIntervalCoalescing.class);

        au.addRequired(LiveStackSlot.class);
        au.addPreserved(LiveStackSlot.class);

        au.addRequired(MachineLoop.class);
        au.addPreserved(MachineLoop.class);
        au.addPreserved(MachineDomTree.class);
        super.getAnalysisUsage(au);
    }

    @Override
    public String getPassName()
    {
        return "Linear scan register allocator";
    }

    private void initIntervalSet()
    {
        Util.assertion( unhandled.isEmpty() && fixed.isEmpty()
                && active.isEmpty() && inactive.isEmpty());

        for (LiveInterval interval : li.getReg2LiveInterval().values())
        {
            // Add live interval of physical register to fixed set.
            if (TargetRegisterInfo.isPhysicalRegister(interval.register))
            {
                fixed.add(interval);
                mri.setPhysRegUsed(interval.register);
            }
            else
            {
                unhandled.add(interval);
            }
        }
    }

    /**
     * Create a live Interval for a stack slot if the specified live interval has
     * been spilled.
     * @param cur
     * @param ls
     * @param li
     * @param mri
     * @param vrm
     */
    private static void addStackInterval(
            LiveInterval cur,
            LiveStackSlot ls,
            LiveIntervalAnalysis li,
            MachineRegisterInfo mri,
            VirtRegMap vrm)
    {
        if (!vrm.hasStackSlot(cur.register))
            return;
        int ss = vrm.getStackSlot(cur.register);
        TargetRegisterClass rc = mri.getRegClass(cur.register);
        LiveInterval slotInterval = ls.getOrCreateInterval(ss, rc);
        int valNumber;
        if (slotInterval.hasAtLeastOneValue())
            valNumber = slotInterval.getRange(0).valId;
        else
            valNumber = slotInterval.getNextValue();

        LiveInterval regInterval = li.getInterval(cur.register);
        slotInterval.mergeRangesInAsValue(regInterval, valNumber);
    }

    private void linearScan()
    {
        while (!unhandled.isEmpty())
        {
            // remove and obtains the first live interval whose start is first.
            LiveInterval cur = unhandled.pollFirst();
            Util.assertion( cur != null);

            for (int i = 0; i < active.size(); i++)
            {
                LiveInterval li = active.get(i);
                if (li.expiredAt(cur.beginNumber()))
                {
                    active.remove(i);
                    --i;
                    prt.delRegUse(vrm.getPhys(li.register));
                }
                else if (!li.isLiveAt(cur.beginNumber()))
                {
                    active.remove(i);
                    --i;
                    inactive.add(li);

                    prt.delRegUse(vrm.getPhys(li.register));
                }
            }

            for (int i = 0; i < inactive.size(); i++)
            {
                LiveInterval li = inactive.get(i);
                if (li.expiredAt(cur.beginNumber()))
                {
                    inactive.remove(i);
                    --i;
                }
                else if (li.isLiveAt(cur.beginNumber()))
                {
                    inactive.remove(i);
                    --i;
                    active.add(li);
                    prt.addRegUse(vrm.getPhys(li.register));
                }
            }

            // if this register is fixed we are done
            if (TargetRegisterInfo.isPhysicalRegister(cur.register))
            {
                prt.addRegUse(cur.register);
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

    private void assignRegOrStackSlot(LiveInterval cur)
    {
        spillWeights = new float[tri.getNumRegs()];
        // The register class for current live interval.
        TargetRegisterClass rc1 = mri.getRegClass(cur.register);

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
        EquivalenceClass.ECNode<TargetRegisterClass> node =
                relatedRegisterClasses.findLeading(rc1), node2;
        TargetRegisterClass leadingRC = node != null?node.getValue():null;
        for (LiveInterval li : inactive)
        {
            node2 = relatedRegisterClasses.findLeading(mri.getRegClass(li.register));
            TargetRegisterClass rcs = node2 != null ?node2.getValue():null;
            if (leadingRC == rcs && leadingRC != null && cur.overlaps(li))
            {
                int reg = li.register;
                if (isVirtualRegister(reg))
                    reg = vrm.getPhys(reg);
                updateSpillWeights(reg, li.weight);
                prt.addRegUse(reg);
            }
        }

        // for every interval in fixed we overlap with,
        // mark the register as not free and update spill weights
        node = relatedRegisterClasses.findLeading(rc1);
        leadingRC = node != null?node.getValue():null;
        for (LiveInterval li : fixed)
        {
            Util.assertion( oneClassForEachPhysReg.containsKey(li.register));
            node2 = relatedRegisterClasses.findLeading(oneClassForEachPhysReg.get(li.register));
            TargetRegisterClass rcs = node2 != null ?node2.getValue():null;
            if (leadingRC != null && leadingRC == rcs && cur.overlaps(li))
            {
                int reg = li.register;
                updateSpillWeights(reg, li.weight);
                prt.addRegUse(reg);
            }
        }

        // if we find a free register, we are done: assign this virtual to
        // the free physical register and add this interval to the active
        // list.
        int phyReg = getFreePhysReg(cur);
        if (phyReg != 0)
        {
            vrm.assignVirt2Phys(cur.register, phyReg);
            prt.addRegUse(phyReg);
            active.add(cur);
            handled.add(cur);
            return;
        }

        if (Util.DEBUG)
        {
            System.err.print("no free register\n");
            System.err.print("\tassigning stack slot at interval");
            cur.print(System.err, tri);
            System.err.println(":");
        }
        float minWeigth = Float.MAX_VALUE;
        int minReg = 0;
        TargetRegisterClass rc = mri.getRegClass(cur.register);
        for (int reg : rc.getAllocableRegs(mf))
        {
            if (spillWeights[reg] <= minWeigth)
            {
                minWeigth = spillWeights[reg];
                minReg = reg;
            }
        }

        if (Util.DEBUG)
        {
            System.err.printf("\tregister with min weight: %s (%f)\n",
                    tri.getName(minReg),
                    minWeigth);
        }

        // if the current has the minimum weight, we need to spill it and
        // add any added intervals back to unhandled, and restart
        // linearscan.
        if (cur.weight < minWeigth)
        {
            if (Util.DEBUG)
            {
                System.err.print("\t\t\tspilling(c):");
                cur.print(System.err, tri);
                System.err.println();
            }

            int slot = vrm.assignVirt2StackSlot(cur.register);
            ArrayList<LiveInterval> added = li.addIntervalsForSpills(cur, vrm, slot);
            addStackInterval(cur, ls, li, mri, vrm);
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


        ArrayList<LiveInterval> added = new ArrayList<>();
        Util.assertion(isPhysicalRegister(minReg), "didn't choose a register to spill?");

        boolean[] toSpill = new boolean[tri.getNumRegs()];
        toSpill[minReg] = true;

        for (int alias : tri.getAliasSet(minReg))
            toSpill[alias] = true;

        int earliestStart = cur.beginNumber();

        // set of spilled vregs(used later to rollback properly).
        TIntHashSet spilled = new TIntHashSet();

        // spill live intervals of virtual regs mapped to the physical
        // register we want to clear (and its aliases). we only spill
        // those that overlap with the current interval as the rest do not
        // affect its allocation. we also keep track of the earliest start
        // of all spilled live intervals since this will mark our rollback
        // point
        for (LiveInterval interval : active)
        {
            int reg = interval.register;
            if (isVirtualRegister(reg) && toSpill[vrm.getPhys(reg)]
                    && cur.overlaps(interval))
            {
                if (Util.DEBUG)
                {
                    System.err.print("\t\t\tspilling(a): ");
                    interval.print(System.err, tri);
                    System.err.println();
                }
                earliestStart = Math.min(earliestStart, interval.beginNumber());
                int slot = vrm.assignVirt2StackSlot(reg);
                ArrayList<LiveInterval> newIS = li.addIntervalsForSpills(interval, vrm, slot);
                addStackInterval(interval, ls, li, mri, vrm);
                added.addAll(newIS);
                spilled.add(reg);
            }
        }

        for (LiveInterval interval : inactive)
        {
            int reg = interval.register;
            if (isVirtualRegister(reg) && toSpill[vrm.getPhys(reg)]
                    && cur.overlaps(interval))
            {
                if (Util.DEBUG)
                {
                    System.err.print("\t\t\tspilling(a): ");
                    interval.print(System.err, tri);
                    System.err.println();
                }
                earliestStart = Math.min(earliestStart, interval.beginNumber());
                int slot = vrm.assignVirt2StackSlot(reg);
                ArrayList<LiveInterval> newIS = li.addIntervalsForSpills(interval, vrm, slot);
                addStackInterval(interval, ls, li, mri, vrm);
                added.addAll(newIS);
                spilled.add(reg);
            }
        }

        // Starting to rollback.
        if (Util.DEBUG)
            System.err.printf("\t\trolling back to: %d\n", earliestStart);

        /**
         * Scan handled in reverse order up to the earliest start of a spilled live
         * interval and undo each one, restore the state of unhandled.
         */
        while (!handled.isEmpty())
        {
            LiveInterval interval = handled.getLast();

            // If the begining number of interval is less than
            // earliest start, just break out.
            if (interval.beginNumber() < earliestStart)
                break;

            // Remove it from the handled list.
            handled.removeLast();
            int idx = -1;
            // when undoing a live interval allocation we must know if it
            // is active or inactive to properly update the PhysRegTracker
            // and the virtRegMap
            if ((idx = active.indexOf(interval)) != -1)
            {
                active.remove(idx);
                int reg = interval.register;
                if(isPhysicalRegister(reg))
                {
                    prt.delRegUse(reg);
                    unhandled.add(interval);
                }
                else
                {
                    if (!spilled.contains(reg))
                        unhandled.add(interval);

                    prt.delRegUse(vrm.getPhys(reg));
                    vrm.clearVirt(reg);
                }
            }
            else if ((idx = inactive.indexOf(interval)) != -1)
            {
                inactive.remove(idx);
                int reg = interval.register;
                if(isPhysicalRegister(reg))
                {
                    prt.delRegUse(reg);
                    unhandled.add(interval);
                }
                else
                {
                    if (!spilled.contains(reg))
                        unhandled.add(interval);

                    // FIXME prt.delRegUse(vrm.getPhys(reg)); why?
                    vrm.clearVirt(reg);
                }
            }
            else
            {
                int reg = interval.register;
                if (isVirtualRegister(reg))
                    vrm.clearVirt(reg);

                unhandled.add(interval);
            }
        }

        for (Iterator<LiveInterval> itr = handled.iterator();
                itr.hasNext();)
        {
            LiveInterval interval = itr.next();
            if (!interval.expiredAt(earliestStart) &&
                    interval.expiredAt(cur.beginNumber()))
            {
                active.add(interval);
                int reg = interval.register;
                if (Util.DEBUG)
                    System.err.printf("\t\t\tundo register: %s\n",
                            li.getRegisterName(reg));
                if (isPhysicalRegister(reg))
                    prt.addRegUse(reg);
                else
                    prt.addRegUse(vrm.getPhys(reg));
            }
        }

        // Add all of live intervals that are caused by
        // spilling code.
        unhandled.addAll(added);
    }

    private int getFreePhysReg(LiveInterval cur)
    {
        TargetRegisterClass rc = mri.getRegClass(cur.register);
        for (int reg : rc.getAllocableRegs(mf))
        {
            if (prt.isRegAvail(reg))
                return reg;
        }
        return 0;
    }

    private RegAllocLinearScan()
    {
        unhandled = new TreeSet<>(
                Comparator.comparingInt(LiveInterval::beginNumber));
        fixed = new ArrayList<>();

        active = new ArrayList<>();
        inactive = new ArrayList<>();
        handled = new LinkedList<>();
        relatedRegisterClasses = new EquivalenceClass<>();
        oneClassForEachPhysReg = new HashMap<>();
    }

    /**
     * Build related register class equivalence classes for checking exactly
     * overlapping between different live interval.
     */
    private void buildRelatedRegClasses()
    {
        if (tri.getRegClasses() == null || tri.getRegClasses().length <= 0)
            return;

        boolean hasAlias = false;
        for (TargetRegisterClass rc : tri.getRegClasses())
        {
            if (rc.getRegs() == null || rc.getRegs().length <= 0)
                continue;
            relatedRegisterClasses.insert(rc);
            for (int reg : rc.getRegs())
            {
                hasAlias = hasAlias || tri.getAliasSet(reg) != null
                        && tri.getAliasSet(reg).length > 0;
                if (!oneClassForEachPhysReg.containsKey(reg))
                    oneClassForEachPhysReg.put(reg, rc);
                else
                    relatedRegisterClasses.union(oneClassForEachPhysReg.get(reg), rc);
            }
        }
        if (hasAlias)
        {
            for (int reg : oneClassForEachPhysReg.keySet())
            {
                TargetRegisterClass rc = oneClassForEachPhysReg.get(reg);
                int[] alias = tri.getAliasSet(reg);
                if (alias != null && alias.length > 0)
                {
                    for (int aliasReg : alias)
                        relatedRegisterClasses.union(rc, oneClassForEachPhysReg.get(aliasReg));
                }
            }
        }
    }

    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        this.mf = mf;
        li = (LiveIntervalAnalysis) getAnalysisToUpDate(LiveIntervalAnalysis.class);
        ls = (LiveStackSlot)getAnalysisToUpDate(LiveStackSlot.class);

        tri = mf.getTarget().getRegisterInfo();
        mri = mf.getMachineRegisterInfo();
        prt = new PhysRegTracker(tri);
        if (relatedRegisterClasses.isEmpty())
            buildRelatedRegClasses();

        // Step#1: Initialize interval set.
        initIntervalSet();

        vrm = new VirtRegMap(mf);

        // Step#2:
        linearScan();

        if (rewriter == null)
            rewriter = VirtRegRewriter.createVirtRegRewriter();

        // Step#3: Inserts load code for loading data from memory before use, or
        // store data to memory after define it.
        rewriter.runOnMachineFunction(mf, vrm);
        unhandled.clear();
        fixed.clear();
        active.clear();
        inactive.clear();
        handled.clear();
        relatedRegisterClasses.clear();
        oneClassForEachPhysReg.clear();
        return true;
    }

    public static RegAllocLinearScan createLinearScanRegAllocator()
    {
        return new RegAllocLinearScan();
    }
}
