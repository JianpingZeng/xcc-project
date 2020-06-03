package backend.codegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
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

import backend.mc.MCRegisterClass;
import backend.target.TargetInstrInfo;
import backend.target.TargetRegisterInfo;
import backend.target.TargetSubtarget;
import tools.BitMap;
import tools.OutRef;
import tools.Util;

import java.util.BitSet;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class RegScavenger {
  private TargetRegisterInfo tri;
  private TargetInstrInfo tii;
  private MachineBasicBlock mbb;
  private int mbbi;
  private int numPhysRegs;

  /**
   * True if RegScavenger is currently tracking the liveness of registers.
   */
  private boolean tracking;
  /**
   * Special spill slot used for scavenging a register post register allocation.
   */
  private int scavengingFrameIndex;

  /**
   * If none zero, the specific register is currently being
   * scavenged. That is, it is spilled to the special scavenging stack slot.
   */
  private int scavengedReg;

  /**
   * Register class of the scavenged register.
   */
  private MCRegisterClass scavengedRC;

  /**
   * Instruction that restores the scavenged register from stack.
   */
  private MachineInstr scavengeRestore;

  /**
   * A bitset of callee saved registers for the target.
   */
  private BitSet calleeSavedRegs;

  /**
   * A bitset of reserved registers.
   */
  private BitSet reservedRegs;

  /**
   * The current state of all the physical registers immediately
   * before {@linkplain #mbbi}. One bit per physical register.
   * If bit is set that means it's available, unset means the
   * register is currently being used.
   */
  private BitSet regsAvailable;

  /**
   * Those bit set are only used for {@linkplain #forward()}. They are
   * members to avoid frequent reallocation.
   */
  private BitSet killRegs, defRegs;

  public RegScavenger() {
    mbb = null;
    numPhysRegs = 0;
    tracking = false;
    scavengingFrameIndex = -1;
    scavengedReg = (0);
    scavengedRC = null;
    killRegs = new BitMap();
    defRegs = new BitMap();
    regsAvailable = new BitSet();
    calleeSavedRegs = new BitSet();
    reservedRegs = new BitSet();
  }

  /**
   * Start tracking liveness from the start of the specific basic block.
   * @param mbb
   */
  public void enterBasicBlock(MachineBasicBlock mbb) {
    MachineFunction mf = mbb.getParent();
    TargetSubtarget subtarget = mf.getSubtarget();
    tii = subtarget.getInstrInfo();
    tri = subtarget.getRegisterInfo();
    Util.assertion(numPhysRegs == 0 || numPhysRegs == tri.getNumRegs(),
            "target changed?");

    killRegs.clear();
    defRegs.clear();
    regsAvailable.clear();
    calleeSavedRegs.clear();
    reservedRegs.clear();
    this.mbb = mbb;

    numPhysRegs = tri.getNumRegs();
    reservedRegs = tri.getReservedRegs(mf);
    int[] csregs = tri.getCalleeSavedRegs(mf);
    if (csregs != null) {
      for (int reg : csregs)
        calleeSavedRegs.set(reg);
    }
    initRegState();
    tracking = false;
  }

  /**
   * Allow resetting register state info for multiple passes over/within the same function.
   */
  public void initRegState() {
    scavengedReg = 0;
    scavengedRC = null;
    scavengeRestore = null;

    // all physical registers are free at the beginning.
    regsAvailable.set(0, numPhysRegs);

    // set reserved registers as used.
    for (int reg = reservedRegs.nextSetBit(1); reg > 0; reg = reservedRegs.nextSetBit(reg+1))
      regsAvailable.clear(reg);

    if (mbb == null)
      return;

    // live-in registers are in use
    for (int i = 0, e = mbb.getLiveIns().size(); i < e; ++i)
      setUsed(mbb.getLiveIns().get(i));

    // set callee saved registers as used.
    BitMap regs = mbb.getParent().getFrameInfo().getPristineRegs(mbb);
    for (int reg = regs.findFirst(); reg > 0; reg = regs.findNext(reg+1))
      setUsed(reg);
  }

  /**
   * Move the internal MBB iterator and update register states.
   */
  public void forward() {
    if (!tracking) {
      mbbi = 0;
      tracking = true;
    }
    else {
      Util.assertion(mbbi != mbb.size(), "already past the end of mbb");
      ++mbbi;
    }
    Util.assertion(mbbi != mbb.size(), "already past the end of mbb");

    MachineInstr mi = mbb.getInstAt(mbbi);
    if (mi == scavengeRestore) {
      scavengedReg = 0;
      scavengedRC = null;
      scavengeRestore = null;
    }

    // Find out which registers are early clobbered, killed, defined, and marked
    // def-dead in this instruction.
    boolean isPred = tii.isPredicated(mi);
    killRegs.clear();
    defRegs.clear();
    for (int i = 0, e = mi.getNumOperands(); i < e; ++i) {
      MachineOperand mo = mi.getOperand(i);
      if (!mo.isRegister()) continue;
      int reg = mo.getReg();
      if (reg == 0 || isReserved(reg))
        continue;

      if (mo.isUse()) {
        // ignore undef uses.
        if (mo.isUndef())
          continue;
        if (!isPred && mo.isKill())
          addRegWithSubRegs(killRegs, reg);
      }
      else {
        Util.assertion(mo.isDef());
        if (!isPred && mo.isDead())
          addRegWithSubRegs(killRegs, reg);
        else
          addRegWithSubRegs(defRegs, reg);
      }
    }

    setUnused(killRegs);
    setUsed(defRegs);
  }

  /**
   * Move the internal MBB iterator and update register states until
   * it has processed the specific iterator.
   * @param itr
   */
  public void forward(int itr) {
    if (!tracking && itr != 0)
      forward();
    while (mbbi != itr)
      forward();
  }

  /**
   * Move the internal MBB iterator but do not update register states.
   * @param itr
   */
  public void skipTo(int itr) {
    mbbi = itr;
  }

  /// getRegsUsed - return all registers currently in use in used.
  public void getRegsUsed(BitSet used, boolean includeReserved) {
    used.clear();
    used.xor(regsAvailable);
    if (!includeReserved)
      used.or(reservedRegs);

    used.flip(0, used.size());
  }

  /**
   * Find a unused register of the specified register class. Return 0 if none is found.
   * @param regClass
   * @return
   */
  public int findUnusedReg(MCRegisterClass regClass) {
    int[] regs = regClass.getRegs();
    if (regs != null) {
      for (int reg : regs) {
        if (!isAliasUsed(reg))
          return reg;
      }
    }
    return 0;
  }

  private BitSet getRegsAvailable(MCRegisterClass rc) {
    BitSet res = new BitMap(tri.getNumRegs());
    int[] regs = rc.getRegs();
    if (regs != null) {
      for (int reg : regs)
        if (!isAliasUsed(reg))
          res.set(reg);
    }
    return res;
  }

  public void setScavengingFrameIndex(int FI) {
    scavengingFrameIndex = FI;
  }

  public int getScavengingFrameIndex() {
    return scavengingFrameIndex;
  }

  /**
   * Make a register of the specific register class available and do the
   * appropriate bookkeeping. spAdj is the stack adjustment due to call
   * frame, it's passed along to eliminateFrameIndex(). Returns the
   * scavenged register.
   * @param rc
   * @param itr
   * @param spAdj
   * @return
   */
  public int scavengeRegister(MCRegisterClass rc, int itr, int spAdj) {
    BitSet candidates = tri.getAllocatableSet(mbb.getParent(), rc);
    clearRegWithAliases(candidates, reservedRegs);

    // exclude all the registers being used by the instruction.
    MachineInstr mi = mbb.getInstAt(itr);
    for (int i = 0, e = mi.getNumOperands(); i < e; ++i) {
      MachineOperand mo = mi.getOperand(i);
      if (mo.isRegister() && mo.getReg() != 0 &&
              TargetRegisterInfo.isPhysicalRegister(mo.getReg())) {
        candidates.clear(mo.getReg());
      }
    }

    // Try to find a register that's unused if there is one, as then we won't
    // have to spill. Search explicitly rather than masking out based on
    // RegsAvailable, as RegsAvailable does not take aliases into account.
    // That's what getRegsAvailable() is for.
    BitSet available = getRegsAvailable(rc);
    available.and(candidates);
    if (!available.isEmpty())
      candidates = available;

    // Find the register whose use is furthest away.
    OutRef<MachineInstr> useMI = new OutRef<>(null);
    int sreg = findSurvivorReg(itr, candidates, 25, useMI);

    // if we found an unused register there is no reason to spill it.
    if (!isAliasUsed(sreg)) {
      return sreg;
    }

    Util.assertion(scavengedReg == 0,
            "scavenger slot is live, unable to scavenge another register!");
    scavengedReg = sreg;
    // If the target knows how to save/restore the register, let it do so;
    // otherwise, use the emergency stack spill slot.
    if (!tri.saveScavengerRegister(mbb, itr, useMI, rc, sreg)) {
      Util.assertion(scavengingFrameIndex >= 0,
              "can't scavenge register without an emergency spill slot!");
      tii.storeRegToStackSlot(mbb, itr, sreg, true, scavengingFrameIndex, rc);
      int ii = itr;
      tri.eliminateFrameIndex(mbb.getParent(), spAdj, mbb.getInstAt(ii), this);
      if (useMI.get() == null)
        ii = mbb.size();
      else
        ii = useMI.get().getIndexInMBB();
      tii.loadRegFromStackSlot(mbb, ii, sreg, scavengingFrameIndex, rc);
      if (useMI.get() == null)
        ii = mbb.size();
      else
        ii = useMI.get().getIndexInMBB();
      ii -= 1;
      tri.eliminateFrameIndex(mbb.getParent(), spAdj, mbb.getInstAt(ii), this);
    }
    int ii = useMI.get() == null ? mbb.size() : useMI.get().getIndexInMBB();
    scavengeRestore = mbb.getInstAt(ii - 1);
    scavengedRC = rc;
    return sreg;
  }

  public int scavengeRegister(MCRegisterClass regClass, int spAdj) {
    return scavengeRegister(regClass, mbbi, spAdj);
  }

  /**
   * Returns true if a register is reserved. It is never "unused".
   * @param Reg
   * @return
   */
  private boolean isReserved(int Reg) {
    return reservedRegs.get(Reg);
  }

  /**
   * Test if a register is currently being used.
   * @param Reg
   * @return
   */
  private boolean isUsed(int Reg) {
    return !regsAvailable.get(Reg);
  }

  private boolean isUnused(int Reg) {
    return regsAvailable.get(Reg);
  }

  /**
   * Is reg or an alias currently in use?
   * @param reg
   * @return
   */
  private boolean isAliasUsed(int reg) {
    if (isUsed(reg))
      return true;

    int[] subregs = tri.getAliasSet(reg);
    if (subregs == null) return false;
    for (int subreg : subregs)
      if (isUsed(subreg))
        return true;

    return false;
  }

  /**
   * Mark the state of one or a number of registers.
   * @param reg
   */
  private void setUsed(int reg) {
    regsAvailable.clear(reg);

    int[] subregs = tri.getSubRegisters(reg);
    if (subregs == null) return;
    for (int subreg : subregs)
      regsAvailable.clear(subreg);
  }

  private void setUsed(BitSet regs) {
    for (int idx = regs.nextSetBit(1); idx > 0; idx = regs.nextSetBit(idx+1))
      regsAvailable.clear(idx);
  }

  private void setUnused(BitSet regs) {
    for (int idx = regs.nextSetBit(1); idx > 0; idx = regs.nextSetBit(idx+1))
      regsAvailable.set(idx);
  }

  /**
   * Add reg and all its sub-registers to res.
   * @param res
   * @param reg
   */
  private void addRegWithSubRegs(BitSet res, int reg) {
    res.set(reg);
    int[] subregs = tri.getSubRegisters(reg);
    if (subregs == null) return;
    for (int subreg : subregs)
      res.set(subreg);
  }

  /**
   * Add reg and its aliases to res.
   * @param res
   * @param reg
   */
  private void addRegWithAliases(BitSet res, int reg) {
    res.set(reg);
    int[] aliasRegs = tri.getAliasSet(reg);
    if (aliasRegs == null) return;
    for (int ar : aliasRegs)
      res.set(ar);
  }

  private void clearRegWithAliases(BitSet res, int reg) {
    res.clear(reg);
    int[] aliasRegs = tri.getAliasSet(reg);
    if (aliasRegs == null) return;
    for (int ar : aliasRegs)
      res.clear(ar);
  }

  private void clearRegWithAliases(BitSet res, BitSet mask) {
    for (int reg = mask.nextSetBit(1); reg > 0; reg = mask.nextSetBit(reg+1)) {
      res.clear(reg);
      int[] aliasRegs = tri.getAliasSet(reg);
      if (aliasRegs == null) return;
      for (int ar : aliasRegs)
        res.clear(ar);
    }
  }

  /**
   * Return the candidate register that is unused for the longest after startMI.
   * useMI is the index of using machine instruction where searching stop.
   *
   * No more than instrLimit instructions are inspected.
   * @param startMI
   * @param candidates
   * @param instrLimit
   * @param useMI
   * @return
   */
  private int findSurvivorReg(int startMI,
                              BitSet candidates,
                              int instrLimit,
                              OutRef<MachineInstr> useMI) {
    int survivor = candidates.nextSetBit(0);
    Util.assertion(survivor > 0, "no candidates for scavenging!");

    int termIdx = mbb.getFirstTerminator();
    Util.assertion(startMI != termIdx, "mi already at terminator");
    int restorePointMI = startMI;
    int mi = startMI;
    boolean inVirtLiveRange = false;
    for (++mi; instrLimit > 0 && mi != termIdx; ++mi, --instrLimit) {
      boolean isVirtKillInst = false;
      boolean isVirtDefInst = false;
      MachineInstr inst = mbb.getInstAt(mi);
      for (int i = 0, e = inst.getNumOperands(); i < e; ++i) {
        MachineOperand mo = inst.getOperand(i);
        if (!mo.isRegister() || mo.isUndef() || mo.getReg() == 0)
          continue;;
          int reg = mo.getReg();
          if (TargetRegisterInfo.isVirtualRegister(reg)) {
            if (mo.isDef())
              isVirtDefInst = true;
            else if (mo.isKill())
              isVirtKillInst = true;
            continue;
          }
          clearRegWithAliases(candidates, reg);
      }
      // if we are not in a virtual register live range, this is a valid
      // restore point.
      if (!inVirtLiveRange)
        restorePointMI = mi;

      if (isVirtKillInst) inVirtLiveRange = false;
      if (isVirtDefInst) inVirtLiveRange = true;

      // was our survivor untouched by this instruction?
      if (candidates.get(survivor))
        continue;

      // all candidates gone?
      if (candidates.isEmpty())
        break;

      survivor = candidates.nextSetBit(0);
    }

    // if we ran off the end, this is where we want to restore.
    if (mi == termIdx) restorePointMI = termIdx;
    Util.assertion(restorePointMI != startMI, "no available scavenger restore localtion!");
    useMI.set(restorePointMI == mbb.size() ? null : mbb.getInstAt(restorePointMI));
    return survivor;
  }
}
