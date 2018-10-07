package backend.codegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng
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

import backend.target.TargetRegisterClass;
import backend.target.TargetRegisterInfo;
import gnu.trove.list.array.TIntArrayList;
import tools.BitMap;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;

import static backend.target.TargetRegisterInfo.FirstVirtualRegister;
import static backend.target.TargetRegisterInfo.NoRegister;

/**
 * Maps register number to register classes which used to assist register allocation.
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public final class MachineRegisterInfo {
  public static class DefUseChainIterator {
    private MachineOperand op;
    private boolean returnUses;
    private boolean returnDefs;

    public DefUseChainIterator(MachineOperand mo, boolean uses, boolean defs) {
      op = mo;
      returnUses = uses;
      returnDefs = defs;
      if (op != null) {
        if ((!returnUses && op.isUse())
            || (!returnDefs && op.isDef()))
          next();
      }
    }

    public void next() {
      Util.assertion(op != null, "Can not increment end iterator");
      op = op.getNextOperandForReg();

      // Skip those machine operand that We don't care about.
      while (op != null && ((!returnUses && op.isUse())
          || (!returnDefs && op.isDef())))
        op = op.getNextOperandForReg();
    }

    public boolean atEnd() {
      return op != null;
    }

    public MachineOperand getOpearnd() {
      Util.assertion(op != null, "Can not derefence end iterator!");
      return op;
    }

    public int getOperandNo() {
      Util.assertion(op != null, "Can not derefence end iterator!");
      return getMachineInstr().getIndexOf(op);
    }

    public MachineInstr getMachineInstr() {
      Util.assertion(op != null, "Can not derefence end iterator!");
      return op.getParent();
    }

    public boolean hasNext() {
      return op != null;
    }
  }

  /**
   * Mapping from virtual register number to its attached register class and
   * define machine operand.
   */
  private ArrayList<Pair<TargetRegisterClass, MachineOperand>> vregInfo;

  private ArrayList<TIntArrayList> regClass2VRegMap;

  private ArrayList<Pair<Integer, Integer>> regAllocHints;

  /**
   * This is an array of the head of the use/def list for
   * physical registers.
   */
  private MachineOperand[] physRegUseDefLists;

  private BitMap usedPhysRegs;

  /**
   * Keeps track which physical register is live in to this function, a live-in register usually means
   * incoming argument. the first field of pair represents the live-in physical register and second one means
   * it's corresponding virtual register assigned by MachineRegisterInfo class.
   */
  private ArrayList<Pair<Integer, Integer>> liveIns;
  /**
   * Keeps track of live-out register which usually means the return value by register.
   */
  private TIntArrayList liveOuts;

  public MachineRegisterInfo(TargetRegisterInfo tri) {
    vregInfo = new ArrayList<>();
    regClass2VRegMap = new ArrayList<>();
    regAllocHints = new ArrayList<>();

    // Create physical register def/use chain.
    physRegUseDefLists = new MachineOperand[tri.getNumRegs()];

    usedPhysRegs = new BitMap();
    liveIns = new ArrayList<>();
    liveOuts = new TIntArrayList();
  }

  private int rescale(int reg) {
    return reg - FirstVirtualRegister;
  }

  /**
   * Obatins the target register class for the given virtual register.
   *
   * @param reg
   * @return
   */
  public TargetRegisterClass getRegClass(int reg) {
    int actualReg = rescale(reg);
    Util.assertion(actualReg < vregInfo.size(), "Register out of bound!");
    return vregInfo.get(actualReg).first;
  }

  /**
   * Creates and returns a new virtual register in the current function with
   * specified target register class.
   *
   * @param regClass
   * @return
   */
  public int createVirtualRegister(TargetRegisterClass regClass) {
    vregInfo.add(new Pair<>(regClass, null));
    return vregInfo.size() - 1 + FirstVirtualRegister;
  }

  public void clear() {
    vregInfo.clear();
  }

  /**
   * Gets the definition machine operand of the specified virtual register.
   *
   * @param regNo
   * @return
   */
  public MachineOperand getDefMO(int regNo) {
    Util.assertion(regNo >= FirstVirtualRegister, "the regNo is not a virtual register");

    int actualReg = rescale(regNo);
    Util.assertion(actualReg < vregInfo.size(), "Register out of bound!");
    return vregInfo.get(actualReg).second;
  }

  public MachineInstr getDefMI(int regNo) {
    return getDefMO(regNo).getParent();
  }

  public void setDefMO(int regNo, MachineOperand mo) {
    Util.assertion(regNo >= FirstVirtualRegister, "the regNo is not a virtual register");

    int actualReg = rescale(regNo);
    Util.assertion(actualReg < vregInfo.size(), "Register out of bound!");
    vregInfo.get(regNo).second = mo;

  }

  /**
   * Checks to see if the specified register is a physical register or not.
   *
   * @param regNo
   * @return
   */
  public boolean isPhysicalReg(int regNo) {
    return regNo >= NoRegister && regNo < FirstVirtualRegister;
  }

  /**
   * Checks to see if the specified register is a virtual register or not.
   *
   * @param regNo
   * @return
   */
  public boolean isVirtualReg(int regNo) {
    return regNo >= FirstVirtualRegister;
  }

  public int getLastVirReg() {
    return vregInfo.size() + FirstVirtualRegister;
  }

  /**
   * Return the head pointer for the register use/def
   * list for the specified virtual or physical register.
   *
   * @param regNo
   * @return
   */
  public MachineOperand getRegUseDefListHead(int regNo) {
    if (regNo < FirstVirtualRegister)
      return physRegUseDefLists[regNo];
    regNo -= FirstVirtualRegister;
    return vregInfo.get(regNo).second;
  }

  /**
   * Update the head of def/use list for specified physical or virtual register
   * with head.
   *
   * @param regNo Physical or virtual register number
   * @param head  The head of def/use list of specified register number.
   */
  public void updateRegUseDefListHead(int regNo, MachineOperand head) {
    if (regNo < FirstVirtualRegister)
      physRegUseDefLists[regNo] = head;
    else {
      regNo -= FirstVirtualRegister;
      vregInfo.get(regNo).second = head;
    }
  }

  /**
   * Walk all defs and uses of the specified register.
   *
   * @param regNo
   * @return
   */
  public DefUseChainIterator getRegIterator(int regNo) {
    return new DefUseChainIterator(getRegUseDefListHead(regNo), true, true);
  }

  /**
   * Checks if there is machine operand uses or defines the specified register.
   *
   * @param regNo
   * @return Return true if there is have def or uses of the spcified reg.
   */
  public boolean hasDefUseOperand(int regNo) {
    return !new DefUseChainIterator(getRegUseDefListHead(regNo), true, true).
        atEnd();
  }

  /**
   * Walk all defs of the specified register.
   *
   * @param regNo
   * @return
   */
  public DefUseChainIterator getDefIterator(int regNo) {
    return new DefUseChainIterator(getRegUseDefListHead(regNo), false, true);
  }

  /**
   * Checks if it have any defs of specified register.
   *
   * @param regNo
   * @return Return true if have.
   */
  public boolean hasDefOperand(int regNo) {
    return !new DefUseChainIterator(getRegUseDefListHead(regNo), false, true).atEnd();
  }

  public DefUseChainIterator getUseIterator(int regNo) {
    return new DefUseChainIterator(getRegUseDefListHead(regNo), true, false);
  }

  /**
   * Checks if it have any uses of specified register.
   *
   * @param regNo
   * @return Return true if have.
   */
  public boolean hasUseOperand(int regNo) {
    return !new DefUseChainIterator(getRegUseDefListHead(regNo), true, false).atEnd();
  }

  /**
   * Return the machine instr that defines the specified virtual
   * register or null if none is found.  This assumes that the code is in SSA
   * form, so there should only be one definition.
   *
   * @param reg
   * @return
   */
  public MachineInstr getVRegDef(int reg) {
    DefUseChainIterator itr = getDefIterator(reg);
    while (itr.hasNext()) {
      if (itr.getOpearnd().getReg() == reg)
        return itr.getMachineInstr();
      itr.next();
    }
    return null;
  }


  /**
   * Replace all instances of FromReg with ToReg in the
   * machine function.  This is like llvm-level X->replaceAllUsesWith(Y),
   * except that it also changes any definitions of the register as well.
   *
   * @param oldReg
   * @param newReg
   */
  public void replaceRegWith(int oldReg, int newReg) {
    Util.assertion(oldReg != newReg, "It is not needed to replace the same reg");
    DefUseChainIterator itr = getRegIterator(oldReg);
    while (itr.hasNext()) {
      MachineOperand mo = itr.getOpearnd();
      mo.setReg(newReg);
      itr.next();
    }
  }

  /**
   * Return true if the specified register is used in this function. This only
   * works after register allocation.
   *
   * @param reg
   * @return
   */
  public boolean isPhysRegUsed(int reg) {
    return usedPhysRegs.get(reg);
  }

  /**
   * Mark the specified register used in this function. This should only be
   * called during and after register allocation.
   *
   * @param reg
   */
  public void setPhysRegUsed(int reg) {
    // FIXME: 17-8-5  This method is not called by Register Allocator.
    usedPhysRegs.set(reg);
  }

  /**
   * Mark the specified register unused in this function.
   * This should only be called during and after register allocation.
   *
   * @param reg
   */
  public void setPhysRegUnused(int reg) {
    // FIXME: 17-8-5  This method is not called by Register Allocator.
    usedPhysRegs.clear(reg);
  }

  public void addLiveIn(int reg) {
    addLiveIn(reg, 0);
  }

  public void addLiveIn(int reg, int vreg) {
    liveIns.add(Pair.get(reg, vreg));
  }

  public void addLiveOut(int reg) {
    liveOuts.add(reg);
  }

  public boolean isLiveInEmpty() {
    return liveIns.isEmpty();
  }

  public ArrayList<Pair<Integer, Integer>> getLiveIns() {
    return liveIns;
  }

  public boolean isLiveOutEmpty() {
    return liveOuts.isEmpty();
  }

  public TIntArrayList getLiveOuts() {
    return liveOuts;
  }

  public boolean isLiveIn(int reg) {
    for (Pair<Integer, Integer> itr : liveIns) {
      if (itr.first == reg || itr.second == reg)
        return true;
    }
    return false;
  }

  /**
   * Determines the virtual register set for the specified Target register class.
   *
   * @param rc
   * @return
   */
  public TIntArrayList getRegClassVirReg(TargetRegisterClass rc) {
    if (rc == null || !regClass2VRegMap.contains(rc.getID()))
      return new TIntArrayList();

    return regClass2VRegMap.get(rc.getID());
  }

  public void replaceDefRegInfo(int defReg, MachineInstr oldMI, MachineInstr newMI) {
    Util.assertion(oldMI.getOperand(0).isRegister() && newMI.getOperand(0).isRegister());
    Util.assertion(oldMI.getOperand(0).getReg() == newMI.getOperand(0).getReg());
    Util.assertion(oldMI.getOperand(0).getReg() > 0);

    MachineOperand newDef = newMI.getOperand(0);
    DefUseChainIterator itr = getDefIterator(defReg);
    while (itr.hasNext()) {
      MachineInstr mi = itr.getMachineInstr();
      if (mi.equals(oldMI)) {
        MachineOperand mo = itr.getOpearnd();
        // unlink this def reg and link the new def register into def/use chain.
        newDef.reg.next = mo.reg.next;
        if (mo.reg.next != null)
          newDef.reg.next.reg.prev = newDef;

        if (mo.reg.prev != null)
          mo.reg.prev.reg.next = newDef;
        newDef.reg.prev = mo.reg.prev;
        mo.reg.clear();
      }
      itr.next();
    }
    updateRegUseDefListHead(defReg, newDef);
  }
}
