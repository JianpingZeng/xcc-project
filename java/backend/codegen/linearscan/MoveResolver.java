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

import backend.codegen.MachineBasicBlock;
import backend.target.TargetInstrInfo;
import backend.target.TargetRegisterClass;
import backend.target.TargetRegisterInfo;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;

import static backend.target.TargetRegisterInfo.isPhysicalRegister;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class MoveResolver
{
    private ArrayList<Pair<LiveInterval, LiveInterval>> mappings;
    private boolean multipleReadsAllowed;
    private int[] registerBlocked;
    private MachineBasicBlock insertedMBB;
    private int insertedIndex;
    private IntervalLocKeeper ilk;
    private TargetInstrInfo tii;
    private TargetRegisterInfo tri;

    public MoveResolver(WimmerLinearScanRegAllocator allocator)
    {
        mappings = new ArrayList<>();
        multipleReadsAllowed = false;
        registerBlocked = new int[allocator.getRegisterInfo().getNumRegs()];
        ilk = allocator.getIntervalLocKeeper();
        tii = allocator.getInstrInfo();
        tri = allocator.getRegisterInfo();
    }

    public void clear()
    {
        mappings.clear();
        multipleReadsAllowed = false;
        registerBlocked = null;
        insertedIndex = -1;
        ilk = null;
    }

    // mark assignedReg and assignedRegHi of the interval as blocked
    private void blockRegisters(LiveInterval it)
    {
        if (ilk.isAssignedPhyReg(it))
        {
            int reg = ilk.getPhyReg(it);
            assert multipleReadsAllowed || registerBlocked(reg)
                    == 0 : "register already marked as used";
            setRegisterBlocked(reg, 1);
        }
    }

    private void setRegisterBlocked(int reg, int direction)
    {
        Util.assertion(direction == -1 || direction == 1);
        Util.assertion(isPhysicalRegister(reg));
        registerBlocked[reg] += direction;
    }

    private int registerBlocked(int reg)
    {
        return registerBlocked[reg];
    }

    public void setMultipleReadsAllowed(boolean val)
    {
        multipleReadsAllowed = val;
    }

    public boolean hasMappings()
    {
        return mappings.isEmpty();
    }

    // mark assignedReg and assignedRegHi of the it as unblocked
    private void unblockRegisters(LiveInterval it)
    {
        if (ilk.isAssignedPhyReg(it))
        {
            int reg = ilk.getPhyReg(it);
            assert registerBlocked(reg) > 0 : "register already marked as unused";
            setRegisterBlocked(reg, -1);
        }
    }

    /**
     * Checks if the assigned location is not blocked or is only blocked by {@code from}.
     */
    private boolean isSafeToProcessMove(LiveInterval from, LiveInterval to)
    {
        Util.assertion(ilk.isAssignedPhyReg(from));
        int fromReg = ilk.getPhyReg(from);

        if (ilk.isAssignedPhyReg(to))
        {
            int toReg = ilk.getPhyReg(to);
            if (registerBlocked(toReg) > 1 ||
                    (registerBlocked(toReg) == 1 && toReg != fromReg))
            {
                return false;
            }
        }

        return true;
    }

    private void insertMove(LiveInterval srcIt, LiveInterval dstIt)
    {
        Util.assertion(srcIt != null && dstIt != null);
        // move is not needed if srcIt is equal to dstIt.
        if (srcIt.equals(dstIt))
            return;

        if (ilk.isAssignedPhyReg(srcIt) && ilk.isAssignedPhyReg(dstIt))
        {
            int srcReg = ilk.getPhyReg(srcIt), dstReg = ilk.getPhyReg(dstIt);
            TargetRegisterClass srcRC = tri.getRegClass(srcReg);
            TargetRegisterClass dstRC = tri.getRegClass(dstReg);
            boolean emitted = tii.copyRegToReg(insertedMBB, insertedIndex, dstReg, srcReg, dstRC, srcRC);
            Util.assertion(emitted, "Can't emit a copy from reg to reg");
        }
        else if (ilk.isAssignedStackSlot(srcIt) && ilk.isAssignedPhyReg(dstIt))
        {
            int srcSlot = ilk.getStackSlot(srcIt), dstReg = ilk.getPhyReg(dstIt);
            TargetRegisterClass dstRC = tri.getRegClass(dstReg);
            tii.loadRegFromStackSlot(insertedMBB, insertedIndex, dstReg, srcSlot, dstRC);
        }
        else if (ilk.isAssignedPhyReg(srcIt) && ilk.isAssignedStackSlot(dstIt))
        {
            int srcReg = ilk.getPhyReg(srcIt);
            int dstFI = ilk.getStackSlot(dstIt);
            TargetRegisterClass rc = tri.getRegClass(srcReg);
            tii.storeRegToStackSlot(insertedMBB, insertedIndex, srcReg, true, dstFI, rc);
        }
        else
        {
            Util.shouldNotReachHere("Can't insert move between two stack slots");
        }
    }

    private void  resolveMappings()
    {
        // Block all registers that are used as input operands of a move.
        // When a register is blocked, no move to this register is emitted.
        // This is necessary for detecting cycles in moves.
        for (int i = mappings.size() - 1; i >= 0; i--)
        {
            LiveInterval srcIt = mappings.get(i).first;
            if (srcIt != null) blockRegisters(srcIt);
        }

        int spillCandidate = -1;
        while (!mappings.isEmpty())
        {
            boolean processedInterval = false;
            for (int i = mappings.size() - 1; i >= 0; i--)
            {
                LiveInterval srcIt = mappings.get(i).first;
                LiveInterval dstIt = mappings.get(i).second;
                Util.assertion(srcIt != null);
                if (isSafeToProcessMove(srcIt, dstIt))
                {
                    insertMove(srcIt, dstIt);
                    unblockRegisters(srcIt);
                    mappings.remove(i);
                    processedInterval = true;
                }
                else if (ilk.isAssignedPhyReg(srcIt))
                {
                    // this interval cannot be processed now because target is not free
                    // it starts in a register, so it is a possible candidate for spilling
                    spillCandidate = i;
                }
            }

            if (!processedInterval)
            {
                // no move could be processed because there is a cycle in the move list
                // (e.g. r1 . r2, r2 . r1), so one interval must be spilled to memory
                Util.assertion(spillCandidate != -1);
                LiveInterval srcIt = mappings.get(spillCandidate).first;

                LiveInterval spillInterval = new LiveInterval();
                // add pseduo-range
                // add a dummy range because real position is difficult to calculate
                // Note: this range is a special case when the integrity of the allocation is checked
                spillInterval.addRange(1, 2);

                ilk.assignInterval2StackSlot(spillInterval);
                insertMove(srcIt, spillInterval);
                mappings.get(spillCandidate).first = spillInterval;
                unblockRegisters(srcIt);
            }
        }
    }

    public void insertMoveInstr(MachineBasicBlock mbb, int index)
    {
        if (this.insertedMBB != null && (insertedMBB != mbb ||
            insertedIndex != index))
        {
            // insertion position has been changed, resolve mappings.
            resolveMappings();
        }
        insertedMBB = mbb;
        insertedIndex = index;
    }

    public void addMapping(LiveInterval srcIt, LiveInterval dstIt)
    {
        Util.assertion(srcIt != null && dstIt != null);
        Util.assertion(!srcIt.equals(dstIt));
        mappings.add(Pair.get(srcIt, dstIt));
    }
}
