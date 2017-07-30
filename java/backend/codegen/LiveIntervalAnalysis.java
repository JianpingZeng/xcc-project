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

import backend.analysis.LiveVariable;
import backend.analysis.LoopInfo;
import backend.pass.AnalysisUsage;
import backend.support.IntStatistic;
import backend.target.TargetInstrInfo;
import backend.target.TargetMachine;
import backend.target.TargetRegisterClass;
import backend.target.TargetRegisterInfo;
import backend.transform.scalars.PhiElimination;
import backend.transform.scalars.TwoAddrInstruction;
import backend.value.Module;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;
import tools.BitMap;
import tools.OutParamWrapper;
import tools.Pair;
import tools.commandline.BooleanOpt;
import tools.commandline.Initializer;
import tools.commandline.OptionNameApplicator;

import java.io.PrintStream;
import java.util.*;

import static backend.target.TargetRegisterInfo.isPhysicalRegister;
import static backend.target.TargetRegisterInfo.isVirtualRegister;
import static tools.commandline.Desc.desc;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class LiveIntervalAnalysis extends MachineFunctionPass
{
    public static IntStatistic numIntervals =
            new IntStatistic("liveIntervals", "Number of original intervals");

    public static IntStatistic numIntervalsAfter =
            new IntStatistic("liveIntervals", "Number of intervals after coalescing");

    public static IntStatistic numJoins =
            new IntStatistic("liveIntervals", "Number of intervals joins performed");

    public static IntStatistic numPeep =
            new IntStatistic("liveIntervals", "Number of identit moves eliminated after coalescing");

    public static IntStatistic numFolded =
            new IntStatistic("liveIntervals", "Number of loads/stores folded into instructions");

    public static BooleanOpt EnableJoining = new BooleanOpt(
            OptionNameApplicator.optionName("join-liveintervals"),
            desc("Join compatible live interval"),
            Initializer.init(true));

    public interface InstrSlots
    {
        int LOAD = 0;
        int USE = 1;
        int DEF = 2;
        int STORE = 3;
        int NUM = 4;
    }

    /**
     * A mapping from instruction number to itself.
     */
    private TreeMap<Integer, MachineInstr> idx2MI;

    /**
     * A mapping from MachineInstr to its number.
     */
    private TreeMap<MachineInstr, Integer> mi2Idx;

    private TreeMap<Integer, LiveInterval> reg2LiveInterval;
    private TreeMap<LiveInterval, Integer> liveInterval2Reg;

    private LiveVariable lv;
    private MachineFunction mf;
    private TargetMachine tm;
    private TargetRegisterInfo tri;
    private BitMap allocatableRegs;
    private TargetInstrInfo tii;
    private MachineRegisterInfo mri;
    private TIntIntHashMap r2rMap = new TIntIntHashMap();

    public LiveIntervalAnalysis()
    {
        idx2MI = new TreeMap<>();
        mi2Idx = new TreeMap<>();
        reg2LiveInterval = new TreeMap<>();
        liveInterval2Reg = new TreeMap<>();
    }

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        // Compute dead set and kill set for each machine instr.
        au.addRequired(LiveVariable.class);
        // Eliminate phi node.
        au.addRequired(PhiElimination.class);
        // Converts the RISC-like MachineInstr to two addr instruction in some
        // target, for example, X86.
        au.addRequired(TwoAddrInstruction.class);
        // Obtains the loop information used for assigning a spilling weight to
        // each live interval. The more nested, the more weight.
        au.addRequired(LoopInfo.class);
        super.getAnalysisUsage(au);
    }

    @Override
    public String getPassName()
    {
        return "Computes Live Intervals for each virtual register";
    }

    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        System.err.println("***********Linear Scan Register Allocator**********");
        System.err.printf("***********Function: %s\n", mf.getFunction().getName());

        this.mf = mf;
        lv = getAnalysisToUpDate(LiveVariable.class);
        tm = mf.getTarget();
        tri = tm.getRegisterInfo();
        tii = tm.getInstrInfo();
        allocatableRegs = tri.getAllocatableSet(mf);
        mri = mf.getMachineRegisterInfo();

        // Step#1: Handle live-in regs of mf.
        // Step#2: Numbering each MachineInstr in each MachineBasicBlock.

        int idx = 0;
        // Step#3: Walk through each MachineInstr to compute live interval.
        for (MachineBasicBlock mbb : mf.getBasicBlocks())
        {
            for (int i = 0, e = mbb.size(); i != e; i++)
            {
                MachineInstr mi = mbb.getInstAt(i);
                assert !(mi2Idx.containsKey(mi)):"Duplicate mi2Idx entry";
                mi2Idx.put(mi, idx);
                idx2MI.put(idx, mi);
                idx += InstrSlots.NUM;
            }
        }

        // Step#4: Compute live interval.
        computeLiveIntervals();

        numIntervals.add(getNumIntervals());

        System.err.printf("*********** Intervals *************\n");
        for (LiveInterval interval : reg2LiveInterval.values())
        {
            interval.print(System.err, tri);
        }

        // If acquired by command line argument, join intervals.
        if (EnableJoining.value)
            joinIntervals();

        numIntervalsAfter.add(getNumIntervals());

        // perform a final pass over the instructions and compute spill
        // weights, coalesce virtual registers and remove identity moves
        LoopInfo loopInfo = getAnalysisToUpDate(LoopInfo.class);
        TargetInstrInfo tii = tm.getInstrInfo();

        for (MachineBasicBlock mbb : mf.getBasicBlocks())
        {
            int loopDepth = loopInfo.getLoopDepth(mbb.getBasicBlock());

            for (Iterator<MachineInstr> itr = mbb.getInsts().iterator(); itr.hasNext(); )
            {
                MachineInstr mi = itr.next();
                OutParamWrapper<Integer> srcReg = new OutParamWrapper<>(0);
                OutParamWrapper<Integer> dstReg = new OutParamWrapper<>();
                int regRep;

                // If the move will be an identify move delete it.
                if (tii.isMoveInstr(mi, srcReg, dstReg, null, null)
                        && (regRep = rep(srcReg.get())) == rep(dstReg.get()))
                {
                    // Remove from def list.
                    // LiveInterval interval = getOrCreateInterval(regRep);
                    if (mi2Idx.containsKey(mi))
                    {
                        idx2MI.put(mi2Idx.get(mi)/InstrSlots.NUM, null);
                        mi2Idx.remove(mi);
                    }
                    itr.remove();
                    numPeep.inc();
                }
                else
                {
                    for (int i = 0, e = mi.getNumOperands(); i < e; i++)
                    {
                        MachineOperand mo = mi.getOperand(i);
                        if (mo.isRegister() && mo.getReg() != 0 && isVirtualRegister(mo.getReg()))
                        {
                            // Replace register with representative register.
                            int reg = rep(mo.getReg());
                            mi.setMachineOperandReg(i, reg);

                            LiveInterval interval = getInterval(reg);
                            interval.weight += ((mo.isUse() ? 1:0) + (mo.isDef()?1:0)) + Math.pow(10, loopDepth);
                        }
                    }

                }
            }
        }

        System.err.printf("************ Intervals *************\n");
        reg2LiveInterval.values().forEach(System.err::println);

        System.err.printf("************ MachineInstrs *************\n");
        for (MachineBasicBlock mbb : mf.getBasicBlocks())
        {
            System.err.println(mbb.getBasicBlock().getName() + ":");
            for (MachineInstr mi : mbb.getInsts())
            {
                System.err.printf("%d\t", getInstructionIndex(mi));
                mi.print(System.err, tm);
            }
        }
        return true;
    }

    /**
     * Returns the representative of this register.
     * @param reg
     * @return
     */
    private int rep(int reg)
    {
        if (r2rMap.containsKey(reg))
            return r2rMap.get(reg);

        return reg;
    }

    private void joinIntervals()
    {
        System.err.printf("************ Joining Intervals *************\n");

        LoopInfo loopInfo = getAnalysisToUpDate(LoopInfo.class);
        if (loopInfo.isEmpty())
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
            mf.getBasicBlocks().forEach(mbb->
            {
                mbbs.add(Pair.get(loopInfo.getLoopDepth(mbb.getBasicBlock()), mbb));
            });

            // Sort mbb by loop depth.
            mbbs.sort((lhs, rhs) ->
            {
                if (lhs.first > rhs.first)
                    return -1;
                if (lhs.first.equals(rhs.first)
                        && lhs.second.getNumber() < rhs.second.getNumber())
                    return -1;
                return 1;
            });

            // Finally, joi intervals in loop nest order.
            for (Pair<Integer, MachineBasicBlock> pair : mbbs)
            {
                joinIntervalsInMachineBB(pair.second);
            }
        }

        System.err.printf("**** Register mapping ***\n");
        r2rMap.forEachEntry(new TIntIntProcedure()
        {
            @Override
            public boolean execute(int i, int i1)
            {
                System.err.printf(" reg %d -> reg %d\n", i, i1);
                return false;
            }
        });
    }

    private void joinIntervalsInMachineBB(MachineBasicBlock mbb)
    {
        System.err.printf("%s:\n", mbb.getBasicBlock().getName());
        TargetInstrInfo tii = tm.getInstrInfo();

        for (MachineInstr mi : mbb.getInsts())
        {
            System.err.printf("%d\t", getInstructionIndex(mi));
            mi.print(System.err, tm);

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
                // Get representaive register.
                int regA = rep(srcReg.get());
                int regB = rep(dstReg.get());

                if (regA == regB)
                    continue;

                // If there are both physical register, we can not join them.
                if (isPhysicalRegister(regA) && isPhysicalRegister(regB))
                {
                    continue;
                }

                // If they are not of the same register class, we cannot join them.
                if (differingRegisterClasses(regA, regB))
                {
                    continue;
                }

                LiveInterval intervalA = getInterval(regA);
                LiveInterval intervalB = getInterval(regB);
                assert intervalA.register == regA && intervalB.register == regB
                        :"Regiser mapping is horribly borken!";

                System.err.printf("\t\tInspecting ");
                intervalA.print(System.err, tri);
                System.err.print(" and ");
                intervalB.print(System.err, tri);
                System.err.print(": ");

                // If two intervals contain a single value and are joined by a copy, it
                // does not matter if the intervals overlap, they can always be joined.
                boolean triviallyJoinable = intervalA.containsOneValue() &&
                        intervalB.containsOneValue();

                int midDefIdx = getDefIndex(getInstructionIndex(mi));
                if (triviallyJoinable || intervalB.joinable(intervalA, midDefIdx)
                        && !overlapsAliases(intervalA, intervalB))
                {
                    intervalB.join(intervalA, midDefIdx);

                    if (!isPhysicalRegister(regA))
                    {
                        r2rMap.remove(regA);
                        r2rMap.put(regA, regB);
                    }
                    else
                    {
                        r2rMap.put(regB, regA);
                        intervalB.register = regA;
                        intervalA.swap(intervalB);
                        reg2LiveInterval.remove(regB);
                    }
                    numJoins.inc();
                }
                else
                {
                    System.err.println("Interference!");
                }
            }
        }
    }

    /**
     *
     * @param lhs
     * @param rhs
     * @return
     */
    private boolean overlapsAliases(LiveInterval lhs, LiveInterval rhs)
    {
        if (!isPhysicalRegister(lhs.register))
        {
            if (!isPhysicalRegister(rhs.register))
                return false;       // Virtual register never aliased.
            LiveInterval temp = lhs;
            lhs = rhs;
            rhs = temp;
        }

        assert isPhysicalRegister(lhs.register)
                : "First interval describe a physical register";

        for (int alias : tri.getAliasSet(lhs.register))
        {
            if (rhs.overlaps(getInterval(alias)))
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
        TargetRegisterClass rc = null;
        if (isVirtualRegister(regA))
            rc = mri.getRegClass(regA);
        else
            rc = tri.getRegClass(regA);

        // Compare against the rc for the second reg.
        if (isVirtualRegister(regB))
            return !rc.equals(mri.getRegClass(regB));
        else
            return !rc.equals(tri.getRegClass(regB));
    }

    private int getNumIntervals()
    {
        return reg2LiveInterval.size();
    }

    public TreeMap<Integer, LiveInterval> getReg2LiveInterval()
    {
        return reg2LiveInterval;
    }

    private LiveInterval createLiveInterval(int reg)
    {
        float weight = isPhysicalRegister(reg) ? Float.MAX_VALUE : 0;
        return new LiveInterval(reg, weight);
    }

    /**
     * Obtains the live interval corresponding to the reg. Creating a new one if
     * there is no live interval as yet.
     * @param reg
     * @return
     */
    private LiveInterval getOrCreateInterval(int reg)
    {
        if(reg2LiveInterval.containsKey(reg))
            return reg2LiveInterval.get(reg);

        LiveInterval newInterval = createLiveInterval(reg);
        reg2LiveInterval.put(reg, newInterval);
        liveInterval2Reg.put(newInterval, reg);
        return newInterval;
    }

    private LiveInterval getInterval(int reg)
    {
        assert reg2LiveInterval.containsKey(reg);
        return reg2LiveInterval.get(reg);
    }

    public String getRegisterName(int register)
    {
        if (isPhysicalRegister(register))
            return tri.getName(register);
        else
            return "%reg" + register;
    }

    private int getInstructionIndex(MachineInstr mi)
    {
        assert mi2Idx.containsKey(mi);
        return mi2Idx.get(mi);
    }

    private static int getBaseIndex(int index)
    {
        return (index / InstrSlots.NUM) * InstrSlots.NUM;
        //return index - index % InstrSlots.NUM;
    }

    private static int getDefIndex(int index)
    {
        return getBaseIndex(index) + InstrSlots.DEF;
    }

    private static int getUseIndex(int index)
    {
        return getBaseIndex(index) + InstrSlots.USE;
    }

    private static int getLoadIndex(int index)
    {
        return getBaseIndex(index) + InstrSlots.LOAD;
    }

    private static int getStoreIndex(int index)
    {
        return getBaseIndex(index) + InstrSlots.STORE;
    }

    /**
     *
     * @param mbb
     * @param index
     * @param li
     */
    private void handleVirtualRegisterDef(
            MachineBasicBlock mbb,
            int index,
            LiveInterval li)
    {
        System.err.printf("\t\tregister: %s\n", getRegisterName(li.register));
        LiveVariable.VarInfo vi = lv.getVarInfo(li.register);

        // Virtual registers may be defined multiple times (due to phi
        // elimination and 2-addr elimination).  Much of what we do only has to be
        // done once for the vreg.  We use an empty interval to detect the first
        // time we see a vreg.
        if (li.isEmpty())
        {
            int defIdx = getDefIndex(getInstructionIndex(mbb.getInstAt(index)));

            // If the only use instruction is lives in block as same as mbb.
            if (vi.kills.size() == 1 && vi.kills.get(0).getParent().equals(mbb))
            {
                int killIdx;
                if (!vi.kills.get(0).equals(mbb.getInstAt(index)))
                    killIdx = getUseIndex(getInstructionIndex(mbb.getInstAt(index)));
                else
                    killIdx = defIdx + 1;

                // If the kill happens after the definition, we have an intra-block
                // live range.
                if (killIdx > defIdx)
                {
                    assert vi.aliveBlocks.isEmpty():"Shouldn't be alive across any block";
                    LiveRange range = new LiveRange(defIdx, killIdx, li.getNextValue());
                    li.addRange(range);
                }
            }

            // The other case we handle is when a virtual register lives to the end
            // of the defining block, potentially live across some blocks, then is
            // live into some number of blocks, but gets killed.  Start by adding a
            // range that goes from this definition to the end of the defining block.
            int lastIdx = getInstructionIndex(mbb.getInsts().getLast());
            LiveRange liveThrough = new LiveRange(defIdx, lastIdx + InstrSlots.NUM, li.getNextValue());
            li.addRange(liveThrough);

            for (int i = 0, e = vi.aliveBlocks.size(); i != e; i++)
            {
                // The index to block where the def reg is alive to out.
                // add [getInstructionIndex(getFirst(bb)), getInstructionIndex(getLast(bb)) + 4)
                // LiveRange to the LiveInterval.
                int bbIdx = vi.aliveBlocks.get(i);
                MachineBasicBlock block = mf.getMBBAt(bbIdx);
                if (block != null)
                {
                    int begin = getInstructionIndex(block.getInsts().getFirst());
                    int end = getInstructionIndex(block.getInsts().getLast()) + InstrSlots.NUM;
                    li.addRange(new LiveRange(begin, end, li.getNextValue()));
                }
            }

            // Finally, this virtual register is live from the start of any killing
            // block to the 'use' slot of the killing instruction.
            for (MachineInstr killMI : vi.kills)
            {
                int begin = getInstructionIndex(killMI.getParent().getInsts().getFirst());
                int end = getInstructionIndex(killMI) + InstrSlots.USE;
                li.addRange(new LiveRange(begin, end, li.getNextValue()));
            }
        }
        else
        {
            // It is the second time we have seen it. This caused by two-address
            // instruction pass or phi elimination pass.

            // If it is the result of 2-addr instruction elimination, the first
            // operand must be register and it is def-use.
            MachineInstr mi = mbb.getInstAt(index);
            if (mi.getOperand(0).isRegister()
                    && mi.getOperand(0).getReg() == li.register
                    && mi.getOperand(0).isDef()
                    && mi.getOperand(0).isUse())
            {
                int defIndex = getDefIndex(getInstructionIndex(vi.defInst));
                int redefIndex = getDefIndex(getInstructionIndex(mi));

                // remove other live range that intersects with the [defIndex, redefIndex).
                li.removeRange(defIndex, redefIndex);

                LiveRange lr = new LiveRange(defIndex, redefIndex, li.getNextValue());
                li.addRange(lr);

                // If this redefinition is dead, we need to add a dummy unit live
                // range covering the def slot.
                if (lv.registerDefIsDeaded(mi, li.register))
                    li.addRange(new LiveRange(redefIndex, redefIndex + 1, li.getNextValue()));
            }
            else
            {
                // Reaching here, it must be caused by phi elimination.
                if (li.containsOneValue())
                {
                    // true.BB:
                    //    %a1 = 0
                    //    br end
                    // false.BB:
                    //    %a2 = 1
                    //    br end
                    // end:
                    //    %res = phi[%a1, %a2]   // The solely use.
                    //    ret %res.
                    //
                    // After phi elimination.
                    // true.BB:
                    //    %a1 = 0
                    //    %res = %a1
                    //    br end
                    // false.BB:
                    //    %a2 = 1
                    //    %res = %a2
                    //    br end
                    // end:
                    //    ret %res.
                    assert vi.kills.size() == 1:"PHI elimination vreg should have one kill, the PHI itself";

                    MachineInstr killer = vi.kills.get(0);
                    int start = getInstructionIndex(killer.getParent().getInsts().getFirst());
                    int end = getUseIndex(getInstructionIndex(killer)) + 1;

                    li.removeRange(start, end);

                    li.addRange(new LiveRange(start, end, li.getNextValue()));
                }

                // In the case of PHI elimination, each variable definition is only
                // live until the end of the block.  We've already taken care of the
                // rest of the live range.
                int defIndex = getDefIndex(getInstructionIndex(mi));
                LiveRange r = new LiveRange(defIndex,
                        getInstructionIndex(mbb.getLastInst()) + InstrSlots.NUM,
                        li.getNextValue());
                li.addRange(r);
            }
        }
    }

    private void handleRegisterDef(MachineBasicBlock mbb, int index, int reg)
    {
        if (TargetRegisterInfo.isVirtualRegister(reg))
        {
            handleVirtualRegisterDef(mbb, index, getOrCreateInterval(reg));
        }
        else if (allocatableRegs.get(reg))
        {
            // If the reg is physical, checking on if it is allocable or not.
            OutParamWrapper<Integer> srcReg = new OutParamWrapper<>();
            OutParamWrapper<Integer> destReg = new OutParamWrapper<>();
            if (!tii.isMoveInstr(mbb.getInstAt(index), srcReg, destReg, null, null))
            {
                srcReg.set(0);
                destReg.set(0);
            }

            handlePhysicalRegisterDef(mbb, index,
                    getOrCreateInterval(reg),
                    srcReg.get(), destReg.get(),
                    false);
            for (int alias : tri.getAliasSet(reg))
            {
                handlePhysicalRegisterDef(mbb, index, getOrCreateInterval(alias),
                        srcReg.get(), destReg.get(), false);
            }
        }
    }

    private MachineInstr getInstructionFromIndex(int idx)
    {
        assert idx2MI.containsKey(idx);
        return idx2MI.get(idx);
    }

    private void handlePhysicalRegisterDef(
            MachineBasicBlock mbb,
            int index,
            LiveInterval interval,
            int srcReg,
            int destReg,
            boolean isLiveIn)
    {
        MachineInstr mi = mbb.getInstAt(index);
       int baseIndex = getInstructionIndex(mi);
       int start = getDefIndex(baseIndex);
       int end = start;

        exit:
        {
            // If it is not used after definition, it is considered dead at
            // the instruction defining it. Hence its interval is:
            // [defSlot(def), defSlot(def)+1)
            if (lv.registerDefIsDeaded(mi, interval.register))
            {
                end = getDefIndex(start) + 1;
                break exit;
            }

            // If it is not dead on definition, it must be killed by a
            // subsequent instruction. Hence its interval is:
            // [defSlot(def), useSlot(kill)+1)
            while (++index != mbb.size())
            {
                baseIndex += InstrSlots.NUM;
                if (lv.killRegister(mbb.getInstAt(index), interval.register))
                {
                    end = getUseIndex(baseIndex) + 1;
                    break exit;
                }
            }

            assert isLiveIn :"phyreg was not killed in defining block!";
            end = getDefIndex(start) + 1;
        }

        assert start < end :"did not find end of intervals";

        // Finally, if this is defining a new range for the physical register, and if
        // that physreg is just a copy from a vreg, and if THAT vreg was a copy from
        // the physreg, then the new fragment has the same value as the one copied
        // into the vreg.
        if (interval.register == destReg
                && !interval.isEmpty()
                && isVirtualRegister(srcReg))
        {
            // Get the live interval for the vreg, see if it is defined by a copy.
            LiveInterval srcInterval = getOrCreateInterval(srcReg);

            if (srcInterval.containsOneValue())
            {
                assert !srcInterval.isEmpty() :"Can't contain a value and be empty";

                int startIdx = srcInterval.getRange(0).start;
                MachineInstr srcDefMI = getInstructionFromIndex(startIdx);

                OutParamWrapper<Integer> vregSrcSrc = new OutParamWrapper<>();
                OutParamWrapper<Integer> vregSrcDest = new OutParamWrapper<>();
                if (tii.isMoveInstr(srcDefMI, vregSrcSrc, vregSrcDest, null, null)
                        && srcReg == vregSrcDest.get()
                        && destReg == vregSrcSrc.get())
                {
                    LiveRange range = interval.getLiveRangeContaining(startIdx-1);
                    if (range != null)
                    {
                        LiveRange lr = new LiveRange(startIdx, end, range.valId);
                        interval.addRange(lr);
                        return;
                    }
                }
            }
        }

        LiveRange lr = new LiveRange(start, end, interval.getNextValue());
        interval.addRange(lr);
    }

    private void computeLiveIntervals()
    {
        // Process each def operand for each machine instr, including implicitly
        // definition and explicitly definition.
        // Just walk instruction from start to end since advantage caused by
        // MachineInstr's SSA property, definition dominates all uses.
        // So avoiding iterative dataflow analysis to compute local liveIn and
        // liveOuts.
        for (MachineBasicBlock mbb : mf.getBasicBlocks())
        {
            for (int i = 0, e = mbb.size(); i != e; i++)
            {
                MachineInstr mi = mbb.getInstAt(i);

                int[] implDefs = mi.getDesc().implicitDefs;
                if (implDefs != null && implDefs.length > 0)
                {
                    // Process implicitly def operand.
                    for (int def : implDefs)
                        handleRegisterDef(mbb, i, def);
                }

                // Process explicitly defs.
                for (int j = 0, sz = mi.getNumOperands(); j < sz; ++j)
                {
                    MachineOperand mo = mi.getOperand(j);
                    if (mo.isRegister() && mo.getReg() != 0 && mo.isDef())
                        handleRegisterDef(mbb, i, mo.getReg());
                }
            }
        }
    }
    public ArrayList<LiveInterval> addIntervalsForSpills(
            LiveInterval interval,
            VirtRegMap vrm,
            int slot)
    {
        ArrayList<LiveInterval> added = new ArrayList<>();

        assert interval.weight != Float.MAX_VALUE
                : "attempt to spill already spilled interval";

        System.err.printf("\t\t\tadding interals for spills for interval: ");
        interval.print(System.err, tri);
        System.err.println();

        TargetRegisterClass rc = mri.getRegClass(interval.register);

        for (LiveRange lr : interval.ranges)
        {
            int index = getBaseIndex(lr.start);
            int end = getBaseIndex(lr.end -1 ) + InstrSlots.NUM;

            for (; index != end; index += InstrSlots.NUM)
            {
                // Skip deleted instructions.
                while ( index != end && getInstructionFromIndex(index) == null)
                {
                    index += InstrSlots.NUM;
                }

                if (index == end)
                    break;

                MachineInstr mi = getInstructionFromIndex(index);

                boolean forOperand = false;
                do
                {
                    for (int i = 0, e = mi.getNumOperands(); i != e; i++)
                    {
                        MachineOperand mo = mi.getOperand(i);
                        if (mo.isRegister() && mo.getReg() == interval.register)
                        {
                            MachineInstr fmi = tii.foldMemoryOperand(mf, mi, i, slot);
                            if (fmi != null)
                            {
                                lv.instructionChanged(mi, fmi);
                                vrm.virtFolded(interval.register, mi, fmi);
                                mi2Idx.remove(mi);
                                idx2MI.put(index/InstrSlots.NUM, fmi);
                                mi2Idx.put(fmi, index);
                                MachineBasicBlock mbb = mi.getParent();
                                mbb.insert(mbb.remove(mi), fmi);
                                numFolded.inc();
                                forOperand = true;
                            }
                            else
                            {
                                // This is tricky. We need to add information in
                                // the interval about the spill code so we have to
                                // use our extra load/store slots.
                                //
                                // If we have a use we are going to have a load so
                                // we start the interval from the load slot
                                // onwards. Otherwise we start from the def slot.
                                int start = (mo.isUse() ? getLoadIndex(index) : getDefIndex(index));

                                // If we have a def we are going to have a store
                                // right after it so we end the interval after the
                                // use of the next instruction. Otherwise we end
                                // after the use of this instruction.
                                int stop = 1 + (mo.isDef() ? getStoreIndex(index) : getUseIndex(index));

                                int nReg = mri.createVirtualRegister(rc);
                                mi.setMachineOperandReg(i, nReg);
                                vrm.assignVirt2StackSlot(nReg, slot);
                                LiveInterval ni = getOrCreateInterval(nReg);
                                assert ni.isEmpty();

                                ni.weight = Float.MAX_VALUE;
                                LiveRange r = new LiveRange(start, end, ni.getNextValue());
                                ni.addRange(r);
                                added.add(ni);

                                // Update viriable.
                                lv.addVirtualRegisterKilled(nReg, mi);
                            }
                        }
                    }
                }while (forOperand);
            }
        }

        return added;
    }

    public void print(PrintStream os, Module m)
    {
        os.printf("****************intervals*****************\n");
        for (LiveInterval interval : reg2LiveInterval.values())
        {
            interval.print(os, tri);
            os.println();
        }

        os.printf("****************machineinstrs**************\n");
        for (MachineBasicBlock mbb : mf.getBasicBlocks())
        {
            os.printf(mbb.getBasicBlock().getName());
            for (MachineInstr mi : mbb.getInsts())
            {
                os.printf("%d\t", getInstructionIndex(mi));
                mi.print(os, null);
            }
        }
    }
}
