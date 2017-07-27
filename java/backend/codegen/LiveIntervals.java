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
import backend.target.TargetRegisterInfo;
import backend.transform.scalars.PhiElimination;
import backend.transform.scalars.TwoAddrInstruction;
import tools.BitMap;
import tools.OutParamWrapper;

import java.util.TreeMap;

import static backend.target.TargetRegisterInfo.isPhysicalRegister;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class LiveIntervals extends MachineFunctionPass
{
    public static IntStatistic numIntervals =
            new IntStatistic("liveIntervals", "Number of original intervals");

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

    public LiveIntervals()
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
        return false;
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
            if (mi.getOperand(0).isReg()
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

            // todo handlePhysicalRegisterDef();
        }
    }

    private void computeLiveIntervals()
    {
        // Process each def operand for each machine instr, including implicitly
        // definition and explicitly definition.
        // Just walk instruction from begin to end since advantage caused by
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
                    if (mo.isReg() && mo.getReg() != 0 && mo.isDef())
                        handleRegisterDef(mbb, i, mo.getReg());
                }
            }
        }
    }
}
