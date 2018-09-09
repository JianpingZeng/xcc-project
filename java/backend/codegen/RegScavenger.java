package backend.codegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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
 * @author Jianping Zeng
 * @version 0.1
 */
public class RegScavenger {
  TargetRegisterInfo TRI;
  TargetInstrInfo TII;
  MachineRegisterInfo MRI;
  MachineBasicBlock MBB;
  int MBBI;
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

  public RegScavenger() {
    MBB = null;
    NumPhysRegs = 0;
    Tracking = false;
    ScavengingFrameIndex = -1;
    ScavengedReg = (0);
    ScavengedRC = null;
  }

  /// enterBasicBlock - Start tracking liveness from the start of the specific
  /// basic block.
  public void enterBasicBlock(MachineBasicBlock mbb) {
  }

  /// initRegState - allow resetting register state info for multiple
  /// passes over/within the same function.
  public void initRegState() {
  }

  /// forward - Move the internal MBB iterator and update register states.
  public void forward() {
    // TODO: 17-8-3
  }

  /// forward - Move the internal MBB iterator and update register states until
  /// it has processed the specific iterator.
  public void forward(int I) {
    if (!Tracking && I != 0)
      forward();
    while (MBBI != I)
      forward();
  }

  /// skipTo - Move the internal MBB iterator but do not update register states.
  ///
  public void skipTo(int I) {
    MBBI = I;
  }

  /// getRegsUsed - return all registers currently in use in used.
  public void getRegsUsed(BitSet used, boolean includeReserved) {
  }

  /// FindUnusedReg - Find a unused register of the specified register class.
  /// Return 0 if none is found.
  public int FindUnusedReg(TargetRegisterClass RegClass) {
    // TODO: 17-8-3
    return 0;
  }

  /// setScavengingFrameIndex / getScavengingFrameIndex - accessor and setter of
  /// ScavengingFrameIndex.
  public void setScavengingFrameIndex(int FI) {
    ScavengingFrameIndex = FI;
  }

  public int getScavengingFrameIndex() {
    return ScavengingFrameIndex;
  }

  /// scavengeRegister - Make a register of the specific register class
  /// available and do the appropriate bookkeeping. SPAdj is the stack
  /// adjustment due to call frame, it's passed along to eliminateFrameIndex().
  /// Returns the scavenged register.
  public int scavengeRegister(TargetRegisterClass RegClass, int I, int SPAdj) {
    // TODO: 17-8-3
    return 0;
  }

  public int scavengeRegister(TargetRegisterClass RegClass, int SPAdj) {
    return scavengeRegister(RegClass, MBBI, SPAdj);
  }

  /// isReserved - Returns true if a register is reserved. It is never "unused".
  private boolean isReserved(int Reg) {
    return ReservedRegs.get(Reg);
  }

  /// isUsed / isUsed - Test if a register is currently being used.
  ///
  private boolean isUsed(int Reg) {
    return !RegsAvailable.get(Reg);
  }

  private boolean isUnused(int Reg) {
    return RegsAvailable.get(Reg);
  }

  /// isAliasUsed - Is Reg or an alias currently in use?
  private boolean isAliasUsed(int Reg) {
    // TODO: 17-8-3
    return false;
  }

  /// setUsed / setUnused - Mark the state of one or a number of registers.
  ///
  private void setUsed(int Reg) {
  }

  private void setUsed(BitSet Regs) {
    RegsAvailable.andNot(Regs);
  }

  private void setUnused(BitSet Regs) {
    RegsAvailable.or(Regs);
  }

  /// Add Reg and all its sub-registers to BV.
  private void addRegWithSubRegs(BitSet BV, int Reg) {
  }

  /// Add Reg and its aliases to BV.
  private void addRegWithAliases(BitSet BV, int Reg) {
  }

  private int findSurvivorReg(int MI, BitSet Candidates, int InstrLimit,
                              int UseMI) {
    // TODO: 17-8-3
    return 0;
  }
}
