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

import backend.analysis.LiveVariables;
import backend.analysis.MachineLoop;
import backend.codegen.*;
import backend.pass.AnalysisUsage;
import backend.support.IntStatistic;
import backend.target.TargetInstrInfo;
import backend.target.TargetMachine;
import backend.target.TargetRegisterClass;
import backend.target.TargetRegisterInfo;
import gnu.trove.map.hash.TIntIntHashMap;
import tools.OutParamWrapper;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;

import static backend.codegen.PrintMachineFunctionPass.createMachineFunctionPrinterPass;
import static backend.target.TargetOptions.PrintMachineCode;
import static backend.target.TargetRegisterInfo.isPhysicalRegister;
import static backend.target.TargetRegisterInfo.isVirtualRegister;

/**
 * This file defines a class takes responsibility for performing register coalescing
 * on live interval to eliminate redundant move instruction.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class SimpleRegisterCoalescer extends MachineFunctionPass
{
    public static IntStatistic numJoins =
            new IntStatistic("liveIntervals", "Number of intervals joins performed");

    public static IntStatistic numIntervalsAfter =
            new IntStatistic("liveIntervals", "Number of intervals after coalescing");

    public static IntStatistic numPeep =
            new IntStatistic("liveIntervals", "Number of identity moves eliminated after coalescing");

    private MachineFunction mf;
    private TargetMachine tm;
    private LiveIntervalAnalysis li;
    private TargetRegisterInfo tri;
    private MachineRegisterInfo mri;
    private LiveVariables lv;
    public TIntIntHashMap r2rMap = new TIntIntHashMap();

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        Util.assertion( au != null);
        au.addPreserved(MachineLoop.class);
        au.addPreserved(LiveIntervalAnalysis.class);
        au.addPreserved(LiveVariables.class);
        //au.addPreserved(PhiElimination.class);
        //au.addPreserved(TwoAddrInstructionPass.class);
        super.getAnalysisUsage(au);
    }
    /**
     * Returns the representative of this register.
     * @param reg
     * @return
     */
    public int rep(int reg)
    {
        if (r2rMap.containsKey(reg))
            return rep(r2rMap.get(reg));

        return reg;
    }

    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        this.mf = mf;
        tm = mf.getTarget();
        li = (LiveIntervalAnalysis) getAnalysisToUpDate(LiveIntervalAnalysis.class);
        tri = tm.getRegisterInfo();
        mri = mf.getMachineRegisterInfo();
        lv = (LiveVariables) getAnalysisToUpDate(LiveVariables.class);

        joinIntervals();

        numIntervalsAfter.add(li.getNumIntervals());

        // perform a final pass over the instructions and compute spill
        // weights, coalesce virtual registers and remove identity moves
        MachineLoop loopInfo = (MachineLoop) getAnalysisToUpDate(MachineLoop.class);
        TargetInstrInfo tii = tm.getInstrInfo();

        for (MachineBasicBlock mbb : mf.getBasicBlocks())
        {
            for (int i = 0; i < mbb.size(); i++)
            {
                MachineInstr mi = mbb.getInstAt(i);
                OutParamWrapper<Integer> srcReg = new OutParamWrapper<>(0);
                OutParamWrapper<Integer> dstReg = new OutParamWrapper<>(0);
                int regRep;

                // If the move will be an identify move delete it.
                if (tii.isMoveInstr(mi, srcReg, dstReg, null, null)
                        && (regRep = rep(srcReg.get())) == rep(dstReg.get())
                        && regRep != 0)
                {
                    // Remove from def list.
                    // LiveInterval interval = getOrCreateInterval(regRep);
                    if (li.mi2Idx.containsKey(mi))
                    {
                        li.idx2MI[li.mi2Idx.get(mi)/LiveIntervalAnalysis.InstrSlots.NUM] = null;
                        li.mi2Idx.remove(mi);
                    }
                    mi.removeFromParent();
                    --i;
                    numPeep.inc();
                }
                else
                {
                    for (int j = 0, e = mi.getNumOperands(); j < e; j++)
                    {
                        MachineOperand mo = mi.getOperand(j);
                        int reg;
                        if (mo.isRegister() && (reg = mo.getReg()) != 0 && isVirtualRegister(reg))
                        {
                            // Replace register with representative register.
                            int repReg = rep(reg);
                            if (reg != repReg) mi.setMachineOperandReg(j, reg);
                        }
                    }
                }
            }
        }

        if (PrintMachineCode.value)
        {
            createMachineFunctionPrinterPass(System.err,
                    "# *** IR dump after register coalescing ***:\n")
                    .runOnMachineFunction(mf);
        }
        if (Util.DEBUG)
        {
            System.err.print("Number of joined interval: ");
            numJoins.printValue(System.err);
            System.err.println();
            System.err.print("Number of removed move instrs: ");
            numPeep.printValue(System.err);
            System.err.println();
        }
        // The r2rMap is not cleared after this pass be run which causes performancing
        // incorrect interval coalescing on %vreg1027 and %vreg1029.
        r2rMap.clear();
        return true;
    }

    @Override
    public String getPassName()
    {
        return "Simple Register Coalescing Pass";
    }

    private void joinIntervals()
    {
        if (Util.DEBUG)
            System.err.println("************ Joining Intervals *************");

        MachineLoop loopInfo = (MachineLoop) getAnalysisToUpDate(MachineLoop.class);
        if (loopInfo != null)
        {
            if (loopInfo.isNoTopLevelLoop())
            {
                // If there are no loops in the function, join intervals in function
                // order.
                for (MachineBasicBlock mbb : mf.getBasicBlocks())
                    joinIntervalsInMachineBB(mbb);
            }
            else
            {
                // Otherwise, join intervals in inner loops before other intervals.
                // Unfortunately we can't just iterate over loop hierarchy here because
                // there may be more MBB's than BB's.  Collect MBB's for sorting.
                ArrayList<Pair<Integer, MachineBasicBlock>> mbbs = new ArrayList<>();
                mf.getBasicBlocks().forEach(mbb -> {
                    mbbs.add(Pair.get(loopInfo.getLoopDepth(mbb),
                            mbb));
                });

                // Sort mbb by loop depth.
                mbbs.sort((lhs, rhs) ->
                {
                    if (lhs.first > rhs.first)
                        return -1;
                    if (lhs.first.equals(rhs.first) && lhs.second.getNumber() < rhs.second.getNumber())
                        return 0;
                    return 1;
                });

                // Finally, joi intervals in loop nest order.
                for (Pair<Integer, MachineBasicBlock> pair : mbbs)
                {
                    joinIntervalsInMachineBB(pair.second);
                }
            }
        }

        if (Util.DEBUG)
        {
            System.err.println("**** Register mapping ***");
            for (int key : r2rMap.keys())
            {
                System.err.printf(" reg %d -> reg %d\n", key, r2rMap.get(key));
            }
            System.err.println("\n**** After Register coalescing ****");
            li.print(System.err, mf.getFunction().getParent());
        }
    }

    private void joinIntervalsInMachineBB(MachineBasicBlock mbb)
    {
        if (Util.DEBUG)
            System.err.printf("%s:\n", mbb.getBasicBlock().getName());
        TargetInstrInfo tii = tm.getInstrInfo();

        for (MachineInstr mi : mbb.getInsts())
        {
            if (Util.DEBUG)
            {
                System.err.printf("%d\t", li.mi2Idx.get(mi));
                mi.print(System.err, tm);
            }

            // we only join virtual registers with allocatable
            // physical registers since we do not have liveness information
            // on not allocatable physical registers
            OutParamWrapper<Integer> srcReg = new OutParamWrapper<>(0);
            OutParamWrapper<Integer> dstReg = new OutParamWrapper<>(0);
            if (tii.isMoveInstr(mi, srcReg, dstReg, null, null)
                    && (isVirtualRegister(srcReg.get())
                    || lv.getAllocatablePhyRegs().get(srcReg.get()))
                    && (isVirtualRegister(dstReg.get())
                    || lv.getAllocatablePhyRegs().get(dstReg.get())))
            {
                // Get representative register.
                int regA = rep(srcReg.get());
                int regB = rep(dstReg.get());

                if (regA == regB)
                    continue;

                // If there are both physical register, we can not join them.
                if (isPhysicalRegister(regA) && isPhysicalRegister(regB))
                    continue;

                // If they are not of the same register class, we cannot join them.
                if (differingRegisterClasses(regA, regB))
                {
                    continue;
                }

                LiveInterval intervalA = li.getInterval(regA);
                LiveInterval intervalB = li.getInterval(regB);

                Util.assertion(intervalA.register == regA && intervalB.register == regB,
                        "Register mapping is horribly broken!");

                if (Util.DEBUG)
                {
                    System.err.print("Inspecting ");
                    intervalA.print(System.err, tri);
                    System.err.print(" and ");
                    intervalB.print(System.err, tri);
                    System.err.print(": ");
                }

                int midDefIdx = li.getIndex(mi);
                if (intervalB.joinable(intervalA, midDefIdx) &&
                        !overlapsAliases(intervalA, intervalB))
                {
                    intervalB.join(intervalA);

                    if (!isPhysicalRegister(regA))
                    {
                        r2rMap.remove(regA);
                        r2rMap.put(regA, regB);
                        li.intervals.remove(regA, li.intervals.get(regA));
                    }
                    else
                    {
                        r2rMap.put(regB, regA);
                        intervalB.register = regA;
                        intervalA.swap(intervalB);
                        li.intervals.remove(regB, li.intervals.get(regB));
                    }
                    numJoins.inc();
                }
                else
                {
                    if (Util.DEBUG)
                        System.err.println("Interference!");
                }
            }
        }
    }


    /**
     *
     * @param src
     * @param dest
     * @return
     */
    private boolean overlapsAliases(LiveInterval src, LiveInterval dest)
    {
        if (!isPhysicalRegister(src.register))
        {
            if (!isPhysicalRegister(dest.register))
                return false;       // Virtual register never aliased.
            LiveInterval temp = src;
            src = dest;
            dest = temp;
        }

        Util.assertion(isPhysicalRegister(src.register),
                "First interval describe a physical register");

        for (int alias : tri.getAliasSet(src.register))
        {
            LiveInterval aliasLI = li.getInterval(alias);
            if (aliasLI == null)
                continue;
            if (dest.intersect(aliasLI))
                return true;
        }
        return false;
    }

    /**
     * Return true if the two specified registers belong to different register
     * classes.  The registers may be either phys or virt regs.
     * @param regA
     * @param regB
     * @return
     */
    private boolean differingRegisterClasses(int regA, int regB)
    {
        TargetRegisterClass rc;
        if (isPhysicalRegister(regA))
        {
            Util.assertion(isVirtualRegister(regB), "Can't consider two physical register");
            rc = mri.getRegClass(regB);
            return !rc.contains(regA);
        }

        // Compare against the rc for the second reg.
        rc = mri.getRegClass(regA);
        if (isVirtualRegister(regB))
            return !rc.equals(mri.getRegClass(regB));
        else
            return !rc.contains(regB);
    }
}
