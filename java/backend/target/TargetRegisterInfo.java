package backend.target;

import backend.codegen.*;
import backend.mc.MCRegisterClass;
import backend.mc.MCRegisterInfo;
import tools.BitMap;
import tools.FormattedOutputStream;
import tools.OutRef;
import tools.Util;

import java.util.ArrayList;

/**
 * This file describes an abstract interface used to get information about a
 * target machines register file.  This information is used for a variety of
 * purposed, especially register allocation.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public abstract class TargetRegisterInfo extends MCRegisterInfo {
  public MCRegisterClass getPhysicalRegisterRegClass(int reg) {
    return getPhysicalRegisterRegClass(reg, new EVT(MVT.Other));
  }

  public MCRegisterClass getPhysicalRegisterRegClass(int reg, EVT vt) {
    Util.assertion(isPhysicalRegister(reg), "reg must be physical register!");

    MCRegisterClass bestRC = null;
    for (MCRegisterClass rc : regClasses) {
      if ((vt.equals(new EVT(MVT.Other)) || hasType(rc, vt)) &&
          rc.contains(reg) && (bestRC == null || bestRC.hasSuperClass(rc))) {
        bestRC = rc;
      }
    }
    return bestRC;
  }

  public int getMatchingSuperReg(int reg, int subIdx, MCRegisterClass rc) {
    int[] srs = getSuperRegisters(reg);
    for (int sr : srs) {
      if (reg == getSubReg(sr, subIdx) && rc.contains(sr))
        return sr;
    }
    return 0;
  }

  public MCRegisterClass getMatchingSuperRegClass(MCRegisterClass a,
                                                  MCRegisterClass b) {
    return null;
  }

  /**
   * Returns a MCRegisterClass used for pointer
   * values.  If a target supports multiple different pointer register classes,
   * kind specifies which one is indicated.
   *
   * @param kind
   * @return
   */
  public MCRegisterClass getPointerRegClass(int kind) {
    Util.assertion(false, "Target didn't implement getPointerRegClass!");
    return null;
  }

  /**
   * Returns a legal register class to copy a register
   * in the specified class to or from. Returns NULL if it is possible to copy
   * between a two registers of the specified class.
   *
   * @param rc
   * @return
   */
  public MCRegisterClass getCrossCopyRegClass(MCRegisterClass rc) {
    return null;
  }

  /**
   * Resolves the specified register allocation hint
   * to a physical register. Returns the physical register if it is successful.
   *
   * @param type
   * @param reg
   * @param mf
   * @return
   */
  public int resolveRegAllocHint(int type, int reg, MachineFunction mf) {
    if (type == 0 && reg != 0 && isPhysicalRegister(reg))
      return reg;
    return 0;
  }

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
  public void updateRegAllocHint(int reg, int newReg, MachineFunction mf) {

  }

  public boolean targetHandlessStackFrameRounding() {
    return false;
  }

  /**
   * Return true if the target needs register scavenging for some application
   * scenario, such as cope with virtual register for virtual register after
   * frame index elimination of PrologEpilogInserter
   * after
   * @param mf
   * @return
   */
  public boolean requiresRegisterScavenging(MachineFunction mf) {
    return false;
  }

  /**
   * returns true if the target requires post scavenging of registers
   * for materializing frame index constants, e.g. PrologEpilogInserter.
   * @return
   */
  public boolean requiresFrameIndexScavenging(MachineFunction mf) {
    return false;
  }

  public boolean hasReservedSpillSlot(MachineFunction mf, int reg,
                                      OutRef<Integer> frameIdx) {
    return false;
  }

  public boolean needsStackRealignment(MachineFunction mf) {
    return false;
  }

  /*
   * eliminateFrameIndex - This method must be overridden to eliminate abstract
   * frame indices from instructions which may use them.  The instruction
   * referenced by the iterator contains an MO_FrameIndex operand which must be
   * eliminated by this method.  This method may modify or replace the
   * specified instruction, as long as it keeps the iterator pointing the the
   * finished product.
   */
  public void eliminateFrameIndex(MachineFunction mf,
                                  int spAdj,
                                  MachineInstr mi) {
    eliminateFrameIndex(mf, spAdj, mi, null);
  }

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
   * This is method is called by {@linkplain
   * PrologEpilogInserter#replaceFrameIndices(MachineFunction)}
   * when it encounters a frame setup/destroy pseudo instruction to
   * replace all pseudo instruction with actual stack adjustment instruction
   * according to the specific target machine.
   * @param mf
   * @param mi
   */
  public abstract void eliminateCallFramePseudoInstr(MachineFunction mf,
                                                     MachineInstr mi);

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
  public boolean saveScavengerRegister(MachineBasicBlock mbb,
                                       int itr,
                                       OutRef<MachineInstr> useMI,
                                       MCRegisterClass rc,
                                       int reg) {
    return false;
  }

  public abstract int getFrameRegister(MachineFunction mf);

  public int getFrameIndexOffset(MachineFunction mf, int fi) {
    TargetFrameLowering tfi = mf.getTarget().getSubtarget().getFrameLowering();
    MachineFrameInfo mfi = mf.getFrameInfo();
    return (int) (mfi.getObjectOffset(fi) + mfi.getStackSize() -
        tfi.getLocalAreaOffset() + mfi.getOffsetAdjustment());
  }

  /**
   * Returns a list of machine moves that are assumed
   * on entry to all functions.  Note that LabelID is ignored (assumed to be
   * the beginning of the function.)
   *
   * @param moves
   */
  public void getInitializeFrameState(ArrayList<MachineMove> moves) {
    // TODO: 2017/7/27
  }

  /**
   * Checks if the specified machine instr is a move instr or not.
   * if it is, return true and store the srcReg, destReg, srcSubReg,
   * destSubReg into regs in the mentioned order.
   *
   * @param mi
   * @param regs
   * @return
   */
  public boolean isMoveInstr(MachineInstr mi, int[] regs) {
    return false;
  }

  public int getSpillSize(MCRegisterClass rc) {
    return getRegClassInfo(rc).spillSize/8;
  }

  public int getRegSizeInBit(MCRegisterClass rc) {
    return getRegClassInfo(rc).regSize;
  }

  public int getRegSize(MCRegisterClass rc) {
    return getRegClassInfo(rc).regSize/8;
  }

  public int getSpillAlignmentInBit(MCRegisterClass rc) {
    return getRegClassInfo(rc).spillAlignment;
  }

  public int getSpillAlignment(MCRegisterClass rc) {
    return getRegClassInfo(rc).spillAlignment/8;
  }

  public int[] getRegisterClassVTs(MCRegisterClass rc) {
    return getRegClassInfo(rc).vts;
  }

  public MCRegisterClass getCommonSubClass(
      MCRegisterClass rc1,
      MCRegisterClass rc2) {
    if (rc1 == rc2) return rc1;
    if (rc1 == null || rc2 == null) return null;

    if (rc2.hasSubClass(rc1)) return rc1;

    MCRegisterClass bestRC = null;
    for (MCRegisterClass rc : rc1.getSubClasses()) {
      if (rc == rc2) return rc;

      if (!rc2.hasSubClass(rc)) continue;

      if (bestRC == null || bestRC.hasSuperClass(rc)) {
        bestRC = rc;
        continue;
      }

      if (bestRC.hasSubClass(rc))
        continue;
      int nb = bestRC.getNumRegs();
      int ni = rc.getNumRegs();
      if (ni > nb || (ni == nb && getRegSizeInBit(rc) < getRegSizeInBit(bestRC)))
        bestRC = rc;
    }
    return bestRC;
  }
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
