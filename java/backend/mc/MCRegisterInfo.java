/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 *
 * Please refer the LICENSE for detail.
 */

package backend.mc;

import backend.codegen.*;
import backend.target.RegClassInfo;
import backend.target.TargetFrameLowering;
import backend.target.TargetRegisterInfo;
import tools.BitMap;
import tools.FormattedOutputStream;
import tools.OutRef;
import tools.Util;

import java.util.ArrayList;
import java.util.BitSet;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public abstract class MCRegisterInfo {
  /**
   * This is used as the destination register for instructions that do not
   * produce a value.
   */
  public static final int NoRegister = 0;

  /**
   * This is the first register number that is
   * considered to be a 'public abstract' register, which is part of the SSA
   * namespace.  This must be the same for all targets, which means that each
   * target is limited to 1024 registers.
   */
  public static final int FirstVirtualRegister = 1024;

  protected MCRegisterDesc[] desc;

  /**
   * Register classes of target machine.
   */
  protected MCRegisterClass[] regClasses;

  protected int[] subregHash;
  protected int[] superregHash;
  protected int[] aliasesHash;
  protected int subregHashSize;
  protected int superregHashSize;
  protected int aliasHashSize;
  /**
   * The register class information, such as register size, spill size
   * and spilling alignment, by hardware mode.
   */
  protected RegClassInfo[] regClassInfo;
  protected int hwMode;
  protected int ra;

  public void initMCRegisterInfo(MCRegisterDesc[] desc,
                                 MCRegisterClass[] regClasses,
                                 int[] subregs, int subregHashSize,
                                 int[] superregs, int superregHashSize,
                                 int[] aliases, int aliasHashSize,
                                 RegClassInfo[] rcInfo,
                                 int mode) {
    this.desc = desc;
    this.regClasses = regClasses;
    subregHash = subregs;
    this.subregHashSize = subregHashSize;
    superregHash = superregs;
    this.superregHashSize = superregHashSize;
    aliasesHash = aliases;
    this.aliasHashSize = aliasHashSize;
    this.regClassInfo = rcInfo;
    this.hwMode = mode;
  }

  public static boolean isPhysicalRegister(int reg) {
    //Util.assertion(reg != 0, "this is not a register");
    return reg < FirstVirtualRegister && reg > 0;
  }

  public static boolean isVirtualRegister(int reg) {
    //Util.assertion(reg != 0, "this is not a register");
    return reg >= FirstVirtualRegister;
  }

  /**
   * Toggle the bits that represent allocatable
   * registers for the specific register class.
   *
   * @param mf
   * @param rc
   * @param r
   */
  public static void getAllocatableSetForRC(MachineFunction mf,
                                            MCRegisterClass rc, BitMap r) {
    for (int reg : rc.getRawAllocationOrder(mf))
      r.set(reg);
  }

  /**
   * Returns a bitset indexed by register number
   * indicating if a register is allocatable or not. If a register class is
   * specified, returns the subset for the class.
   *
   * @param mf
   * @param rc
   * @return
   */
  public BitMap getAllocatableSet(MachineFunction mf, MCRegisterClass rc) {
    BitMap allocatable = new BitMap(desc.length);
    if (rc != null)
      getAllocatableSetForRC(mf, rc, allocatable);
    else {
      for (MCRegisterClass _rc : regClasses)
        getAllocatableSetForRC(mf, _rc, allocatable);
    }
    BitMap temp = getReservedRegs(mf);
    allocatable.diff(temp);
    return allocatable;
  }

  public BitMap getAllocatableSet(MachineFunction mf) {
    return getAllocatableSet(mf, null);
  }

  /**
   * Obtains the register information indexed with given register number.
   *
   * @param regNo
   * @return
   */
  public MCRegisterDesc get(int regNo) {
    Util.assertion(regNo >= 0 && regNo < desc.length);
    return desc[regNo];
  }

  public int[] getAliasSet(int regNo) {
    return get(regNo).aliasSet;
  }

  public int[] getSubRegisters(int regNo) {
    return get(regNo).subRegs;
  }

  public int[] getSuperRegisters(int regNo) {
    return get(regNo).superRegs;
  }

  public String getAsmName(int regNo) {
    return get(regNo).asmName;
  }

  /**
   * Note that the returned register name of this method can't used as
   * AsmPrinter. If you want to do that, just use {@linkplain #getAsmName(int)}
   * instead.
   *
   * @param regNo
   * @return
   */
  public String getName(int regNo) {
    return get(regNo).name;
  }

  public int getNumRegs() {
    return desc.length;
  }

  /**
   * Returns true if the two registers are equal or alias each
   * other. The registers may be virtual register.
   *
   * @param regA
   * @param regB
   * @return
   */
  public boolean regsOverlap(int regA, int regB) {
    if (regA == regB)
      return true;

    if (isVirtualRegister(regA) || isVirtualRegister(regB))
      return false;

    int index = (regA + regB * 37) & (aliasHashSize - 1);

    int probeAmt = 0;
    while (aliasesHash[index * 2] != 0 && aliasesHash[index * 2 + 1] != 0) {
      if (aliasesHash[index * 2] == regA && aliasesHash[index * 2 + 1] == regB)
        return true;

      index = (index + probeAmt) & (aliasHashSize - 1);
      probeAmt += 2;
    }
    return false;
  }

  /**
   * Return true if regB is a sub-register of regA.
   *
   * @param regA
   * @param regB
   * @return
   */
  public boolean isSubRegister(int regA, int regB) {
    int index = (regA + regB * 37) & (subregHashSize - 1);

    int probeAmt = 2;
    while (subregHash[index * 2] != 0 && subregHash[index * 2 + 1] != 0) {
      if (subregHash[index * 2] == regA && subregHash[index * 2 + 1] == regB)
        return true;

      index = (index + probeAmt) & (subregHashSize - 1);
      probeAmt += 2;
    }
    return false;
  }

  /**
   * Returns true if regB is a super-register of regA.
   *
   * @param regA
   * @param regB
   * @return
   */
  public boolean isSuperRegister(int regA, int regB) {
    //System.err.printf("%d, %d. %s, %s%n", regA, regB, getName(regA), getName(regB));
    int index = (regA + regB * 37) & (superregHashSize - 1);

    int probeAmt = 2;
    while (superregHash[index * 2] != 0 && superregHash[index * 2 + 1] != 0) {
      if (superregHash[index * 2] == regA
          && superregHash[index * 2 + 1] == regB)
        return true;

      index = (index + probeAmt) & (superregHashSize - 1);
      probeAmt += 2;
    }
    return false;
  }


  public MCRegisterClass[] getRegClasses() {
    return regClasses;
  }

  public int getNumRegClasses() {
    return regClasses.length;
  }

  /**
   * Returns the register class associated with the enumeration
   * value.  See class MCOperandInfo.
   *
   * @param i
   * @return
   */
  public MCRegisterClass getRegClass(int i) {
    Util.assertion(i >= 0 && i < regClasses.length);
    return regClasses[i];
  }
  /**
   * This method should return the register where the return
   * address can be found.
   *
   * @return
   */
  public int getRARegister() {
    return ra;
  }

  public RegClassInfo getRegClassInfo(MCRegisterClass rc) {
    return regClassInfo[hwMode * getNumRegClasses() + rc.getID()];
  }

  public boolean hasType(MCRegisterClass rc, EVT vt) {
    for (int v : getRegClassInfo(rc).vts) {
      if (v == vt.getSimpleVT().simpleVT)
        return true;
    }
    return false;
  }

  public abstract int[] getCalleeSavedRegs(MachineFunction mf);

  public abstract MCRegisterClass[] getCalleeSavedRegClasses(
          MachineFunction mf);

  public abstract BitMap getReservedRegs(MachineFunction mf);

  /**
   * Returns the physical register number of sub-register "Index"
   * for physical register RegNo. Return zero if the sub-register does not
   * exist.
   *
   * @param regNo
   * @param index
   * @return
   */
  public abstract int getSubReg(int regNo, int index);
  public abstract MCRegisterClass getPhysicalRegisterRegClass(int reg);
  public abstract MCRegisterClass getPhysicalRegisterRegClass(int reg, EVT vt);
  public abstract int getMatchingSuperReg(int reg, int subIdx, MCRegisterClass rc);
  public abstract MCRegisterClass getMatchingSuperRegClass(MCRegisterClass a,
                                                           MCRegisterClass b);

  /**
   * Returns a MCRegisterClass used for pointer
   * values.  If a target supports multiple different pointer register classes,
   * kind specifies which one is indicated.
   *
   * @param kind
   * @return
   */
  public abstract MCRegisterClass getPointerRegClass(int kind);

  /**
   * Returns a legal register class to copy a register
   * in the specified class to or from. Returns NULL if it is possible to copy
   * between a two registers of the specified class.
   *
   * @param rc
   * @return
   */
  public abstract MCRegisterClass getCrossCopyRegClass(MCRegisterClass rc);

  /**
   * Resolves the specified register allocation hint
   * to a physical register. Returns the physical register if it is successful.
   *
   * @param type
   * @param reg
   * @param mf
   * @return
   */
  public abstract int resolveRegAllocHint(int type, int reg, MachineFunction mf);

  /**
   * A callback to allow target a chance to update
   * register allocation hints when a register is "changed" (e.g. coalesced)
   * to another register. e.g. On ARM, some virtual registers should target
   * register pairs, if one of pair is coalesced to another register, the
   * allocation hint of the other half of the pair should be changed to point
   * to the new register.
   *
   * @param reg
   * @param newReg
   * @param mf
   */
  public abstract void updateRegAllocHint(int reg, int newReg, MachineFunction mf);

  public abstract boolean targetHandlessStackFrameRounding();

  /**
   * Return true if the target needs register scavenging for some application
   * scenario, such as cope with virtual register for virtual register after
   * frame index elimination of PrologEpilogInserter
   * after
   * @param mf
   * @return
   */
  public abstract boolean requiresRegisterScavenging(MachineFunction mf);

  /**
   * returns true if the target requires post scavenging of registers
   * for materializing frame index constants, e.g. PrologEpilogInserter.
   * @return
   */
  public abstract boolean requiresFrameIndexScavenging(MachineFunction mf);

  public abstract boolean hasReservedSpillSlot(MachineFunction mf, int reg,
                                               OutRef<Integer> frameIdx);

  public abstract boolean needsStackRealignment(MachineFunction mf);

  /*
   * eliminateFrameIndex - This method must be overridden to eliminate abstract
   * frame indices from instructions which may use them.  The instruction
   * referenced by the iterator contains an MO_FrameIndex operand which must be
   * eliminated by this method.  This method may modify or replace the
   * specified instruction, as long as it keeps the iterator pointing the the
   * finished product.
   */
  public abstract void eliminateFrameIndex(MachineFunction mf,
                                           int spAdj,
                                           MachineInstr mi);

  /**
   * This function is used to replace the abstract frame index with a
   * pair of stack offset and stack pointer.
   * @param mf
   * @param mi
   * @param rs
   */
  public abstract void eliminateFrameIndex(MachineFunction mf,
                                           int spAdj,
                                           MachineInstr mi,
                                           RegScavenger rs);

  /**
   * Spill the register so it can be used by the register scavenger.
   * Return true if the register was spilled, false otherwise.
   * If this function does not spill the register, the scavenger
   * will instead spill it to the emergency spill slot.
   * @param mbb
   * @param itr
   * @param useMI
   * @param rc
   * @param reg
   * @return
   */
  public abstract boolean saveScavengerRegister(MachineBasicBlock mbb,
                                                int itr,
                                                OutRef<MachineInstr> useMI,
                                                MCRegisterClass rc,
                                                int reg);

  public abstract int getFrameRegister(MachineFunction mf);

  public abstract int getFrameIndexOffset(MachineFunction mf, int fi);

  /**
   * Returns a list of machine moves that are assumed
   * on entry to all functions.  Note that LabelID is ignored (assumed to be
   * the beginning of the function.)
   *
   * @param moves
   */
  public abstract void getInitializeFrameState(ArrayList<MachineMove> moves);
  /**
   * Checks if the specified machine instr is a move instr or not.
   * if it is, return true and store the srcReg, destReg, srcSubReg,
   * destSubReg into regs in the mentioned order.
   *
   * @param mi
   * @param regs
   * @return
   */
  public abstract boolean isMoveInstr(MachineInstr mi, int[] regs);
  public abstract int getSpillSize(MCRegisterClass rc);
  public abstract int getRegSizeInBit(MCRegisterClass rc);
  public abstract int getRegSize(MCRegisterClass rc);
  public abstract int getSpillAlignmentInBit(MCRegisterClass rc);
  public abstract int getSpillAlignment(MCRegisterClass rc);
  public abstract int[] getRegisterClassVTs(MCRegisterClass rc);

  public abstract MCRegisterClass getCommonSubClass(MCRegisterClass rc1, MCRegisterClass rc2);
  public static void printReg(FormattedOutputStream os, int reg) {
    printReg(os, reg, null);
  }
  public static void printReg(FormattedOutputStream os, int reg, TargetRegisterInfo tri) {
    printReg(os, reg, tri, 0);
  }
  public static void printReg(FormattedOutputStream os, int reg, TargetRegisterInfo tri, int subIdx) {
    if (reg == 0) {
      os.print("%noreg");
    }
    else if (TargetRegisterInfo.isVirtualRegister(reg))
      os.print(String.format("%%reg%d", reg));
    else if (tri != null && reg < tri.getNumRegs())
      os.print(String.format("%%%s", tri.getName(reg)));
    else
      os.print(String.format("%%physreg%d", reg));
    if (subIdx != 0)
      os.print(String.format(":sub(%d)", subIdx));
  }
}
