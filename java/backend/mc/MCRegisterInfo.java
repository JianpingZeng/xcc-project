/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2015-2019, Jianping Zeng.
 * All rights reserved.
 *
 * Please refer the LICENSE for detail.
 */

package backend.mc;

import backend.codegen.EVT;
import backend.codegen.MachineFunction;
import backend.target.RegClassInfo;
import tools.BitMap;
import tools.Util;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class MCRegisterInfo {
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
    Util.assertion(reg != 0, "this is not a register");
    return reg < FirstVirtualRegister;
  }

  public static boolean isVirtualRegister(int reg) {
    Util.assertion(reg != 0, "this is not a register");
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
    for (int reg : rc.getAllocableRegs(mf))
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
    if (rc != null) {
      getAllocatableSetForRC(mf, rc, allocatable);
      return allocatable;
    }
    for (MCRegisterClass _rc : regClasses)
      getAllocatableSetForRC(mf, _rc, allocatable);

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
    Util.assertion(i >= 0 && i <= regClasses.length);
    return i != 0 ? regClasses[i - 1] : null;
  }
  /**
   * This method should return the register where the return
   * address can be found.
   *
   * @return
   */
  public int getRARegister() {
    return getRARegister();
  }

  public RegClassInfo getRegClassInfo(MCRegisterClass rc) {
    return regClassInfo[hwMode * getNumRegClasses() + rc.getID() - 1];
  }

  public boolean hasType(MCRegisterClass rc, EVT vt) {
    for (int v : getRegClassInfo(rc).vts) {
      if (v == vt.getSimpleVT().simpleVT)
        return true;
    }
    return false;
  }
}
