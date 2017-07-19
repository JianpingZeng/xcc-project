package backend.codegen;
/*
 * Xlous C language Compiler
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

import backend.target.TargetInstrInfo;
import backend.target.TargetRegisterClass;
import backend.target.TargetRegisterInfo;

import java.util.BitSet;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class RegScavenger
{
      TargetRegisterInfo TRI;
   TargetInstrInfo TII;
    MachineRegisterInfo MRI;
    MachineBasicBlock MBB;
    MachineBasicBlock::iterator MBBI;
    int NumPhysRegs;

    /// Tracking - True if RegScavenger is currently tracking the liveness of
    /// registers.
    boolean Tracking;

    /// ScavengingFrameIndex - Special spill slot used for scavenging a register
    /// post register allocation.
    int ScavengingFrameIndex;

    /// ScavengedReg - If none zero, the specific register is currently being
    /// scavenged. That is, it is spilled to the special scavenging stack slot.
    int ScavengedReg;

    /// ScavengedRC - Register class of the scavenged register.
    ///
   TargetRegisterClass ScavengedRC;

    /// ScavengeRestore - Instruction that restores the scavenged register from
    /// stack.
   MachineInstr ScavengeRestore;

    /// CalleeSavedrRegs - A bitvector of callee saved registers for the target.
    ///
    BitSet CalleeSavedRegs;

    /// ReservedRegs - A bitvector of reserved registers.
    ///
    BitSet ReservedRegs;

    /// RegsAvailable - The current state of all the physical registers immediately
    /// before MBBI. One bit per physical register. If bit is set that means it's
    /// available, unset means the register is currently being used.
    BitSet RegsAvailable;

    RegScavenger()
    {
    MBB = null;
    NumPhysRegs = 0;
    Tracking = false;
    ScavengingFrameIndex = -1;
    ScavengedReg = (0);
    ScavengedRC = null;
    }

    /// enterBasicBlock - Start tracking liveness from the begin of the specific
    /// basic block.
    void enterBasicBlock(MachineBasicBlock mbb) {}

    /// initRegState - allow resetting register state info for multiple
    /// passes over/within the same function.
    void initRegState(){}

    /// forward - Move the internal MBB iterator and update register states.
    void forward() {}

    /// forward - Move the internal MBB iterator and update register states until
    /// it has processed the specific iterator.
    void forward(MachineBasicBlock::iterator I)
    {
        if (!Tracking && MBB->begin() != I) forward();
        while (MBBI != I) forward();
    }

    /// skipTo - Move the internal MBB iterator but do not update register states.
    ///
    void skipTo(MachineBasicBlock::iterator I) { MBBI = I; }

    /// getRegsUsed - return all registers currently in use in used.
    void getRegsUsed(BitSet used, boolean includeReserved)
    {}

    /// FindUnusedReg - Find a unused register of the specified register class.
    /// Return 0 if none is found.
    int FindUnusedReg( TargetRegisterClass RegClass) {}

    /// setScavengingFrameIndex / getScavengingFrameIndex - accessor and setter of
    /// ScavengingFrameIndex.
    void setScavengingFrameIndex(int FI) { ScavengingFrameIndex = FI; }
    int getScavengingFrameIndex()  { return ScavengingFrameIndex; }

    /// scavengeRegister - Make a register of the specific register class
    /// available and do the appropriate bookkeeping. SPAdj is the stack
    /// adjustment due to call frame, it's passed along to eliminateFrameIndex().
    /// Returns the scavenged register.
    int scavengeRegister( TargetRegisterClass RegClass,
            MachineBasicBlock::iterator I, int SPAdj)
    {}

    int scavengeRegister( TargetRegisterClass RegClass, int SPAdj)
    {
        return scavengeRegister(RegClass, MBBI, SPAdj);
    }

    private:
    /// isReserved - Returns true if a register is reserved. It is never "unused".
    boolean isReserved(int Reg)  { return ReservedRegs.get(Reg); }

    /// isUsed / isUsed - Test if a register is currently being used.
    ///
    boolean isUsed(int Reg)    { return !RegsAvailable.get(Reg); }
    boolean isUnused(int Reg)  { return RegsAvailable.get(Reg); }

    /// isAliasUsed - Is Reg or an alias currently in use?
    boolean isAliasUsed(int Reg)
    {

    }

    /// setUsed / setUnused - Mark the state of one or a number of registers.
    ///
    void setUsed(int Reg){}
    void setUsed(BitSet Regs)
    {
        RegsAvailable.andNot(Regs);
    }

    void setUnused(BitSet Regs)
    {
        RegsAvailable.or(Regs);
    }

    /// Add Reg and all its sub-registers to BV.
    void addRegWithSubRegs(BitSet BV, int Reg){}

    /// Add Reg and its aliases to BV.
    void addRegWithAliases(BitSet BV, int Reg){}

    int findSurvivorReg(MachineBasicBlock::iterator MI,
            BitSet Candidates,
            int InstrLimit,
            MachineBasicBlock::iterator &UseMI)
    {

    }
}
