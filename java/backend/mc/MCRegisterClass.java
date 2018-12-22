/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2015-2018, Jianping Zeng.
 * All rights reserved.
 *
 * Please refer the LICENSE for detail.
 */

package backend.mc;

import backend.codegen.EVT;
import backend.codegen.MachineFunction;
import backend.target.TargetRegisterInfo;
import gnu.trove.set.hash.TIntHashSet;
import tools.Util;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public abstract class MCRegisterClass {
  /**
   * The id of this {@linkplain MCRegisterClass}.
   */
  private int id;
  /**
   * The name of this {@linkplain MCRegisterClass}.
   */
  private String name;
  private TIntHashSet regSet;
  private MCRegisterClass[] subClasses;
  private MCRegisterClass[] superClasses;
  private MCRegisterClass[] subRegClasses;
  private MCRegisterClass[] superRegClasses;
  private int copyCost;

  /**
   * The register list contained in this {@linkplain MCRegisterClass}.
   */
  private int[] regs;

  protected MCRegisterClass(int id,
                            String name,
                            MCRegisterClass[] subcs,
                            MCRegisterClass[] supercs,
                            MCRegisterClass[] subregcs,
                            MCRegisterClass[] superregcs,
                            int copyCost,
                            int[] regs) {
    this.id = id;
    this.name = name;
    subClasses = subcs;
    superClasses = supercs;
    subRegClasses = subregcs;
    superRegClasses = superregcs;
    this.regs = regs;
    this.copyCost = copyCost;
    regSet = new TIntHashSet();
    regSet.addAll(regs);
  }

  public void setSubClasses(MCRegisterClass[] subClasses) {
    this.subClasses = subClasses;
  }

  public void setSubRegClasses(MCRegisterClass[] subRegClasses) {
    this.subRegClasses = subRegClasses;
  }

  public void setSuperClasses(MCRegisterClass[] superClasses) {
    this.superClasses = superClasses;
  }

  public void setSuperRegClasses(MCRegisterClass[] superRegClasses) {
    this.superRegClasses = superRegClasses;
  }

  public int getID() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getNumRegs() {
    return regs.length;
  }

  public int getRegister(int i) {
    Util.assertion(i >= 0 && i < regs.length);
    return regs[i];
  }

  /**
   * Returns all of register in current target machine, and contains unavailable
   * register. If want to obtain all available registers, just consulting by
   * {@linkplain #getAllocableRegs(MachineFunction)}.
   *
   * @return
   */
  public int[] getRegs() {
    return regs;
  }

  /**
   * Obtains the allocatable registers of type array.
   * Default, returned array is as same as registers array contained in this
   * MCRegisterClass. But it is may be altered for concrete sub class. e.g.
   * GR32RegisterClass have more register (R8D, R9D etc) in 64bit subtarget.
   *
   * @return An array of allocatable registers for specified sub-target.
   */
  public int[] getAllocableRegs(MachineFunction mf) {
    return regs;
  }

  /**
   * Return true if the specified register is included in this
   * register class.
   *
   * @param reg
   * @return
   */
  public boolean contains(int reg) {
    return regSet.contains(reg);
  }

  public boolean hasSuperClass(MCRegisterClass rc) {
    if (superClasses == null) return false;

    for (MCRegisterClass superRC : superClasses)
      if (superRC.equals(rc))
        return true;

    return false;
  }

  public boolean hasSubClass(MCRegisterClass subRC) {
    if (subClasses == null || subClasses.length <= 0)
      return false;
    for (MCRegisterClass rc : subClasses)
      if (rc.equals(subRC))
        return true;
    return false;
  }

  public MCRegisterClass[] getSubClasses() {
    return subClasses;
  }

  public MCRegisterClass[] getSuperClasses() {
    return superClasses;
  }

  public MCRegisterClass getSubRegisterRegClass(long subIdx) {
    for (int i = 0; i < subIdx - 1; i++)
      if (subRegClasses[i] == null)
        return null;
    return subRegClasses[(int) (subIdx - 1)];
  }

  public MCRegisterClass getSuperRegisterRegClass(
      TargetRegisterInfo tri,
      MCRegisterClass rc,
      int subIdx, EVT vt) {
    for (MCRegisterClass itr : superRegClasses) {
      if (tri.hasType(itr, vt) && itr.getSubRegisterRegClass(subIdx).equals(rc))
        return itr;
    }
    Util.assertion("Couldn't find the register class!");
    return null;
  }

  public int getCopyCost() {
    return copyCost;
  }
}
