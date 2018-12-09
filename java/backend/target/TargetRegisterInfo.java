package backend.target;

import backend.codegen.*;
import backend.mc.MCRegisterClass;
import backend.mc.MCRegisterInfo;
import tools.BitMap;
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
      if ((vt.equals(new EVT(MVT.Other)) || isLegalTypeForRegClass(rc, vt.getSimpleVT())) &&
          rc.contains(reg) && (bestRC == null || bestRC.hasSuperClass(rc))) {
        bestRC = rc;
      }
    }
    return bestRC;
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

  public boolean requireRegisterScavenging(MachineFunction mf) {
    return false;
  }

  public boolean hasReservedSpillSlot(MachineFunction mf, int reg,
                                      OutRef<Integer> frameIdx) {
    return false;
  }

  public boolean needsStackRealignment(MachineFunction mf) {
    return false;
  }

  public void processFunctionBeforeCalleeSavedScan(MachineFunction mf) {
    processFunctionBeforeCalleeSavedScan(mf, null);
  }

  /**
   * This method is called immediately
   * before PrologEpilogInserter scans the physical registers used to determine
   * what callee saved registers should be spilled. This method is optional.
   *
   * @param mf
   * @param rs
   */
  public void processFunctionBeforeCalleeSavedScan(MachineFunction mf,
                                                   RegScavenger rs) {
  }

  /**
   * This method is called immediately before the specified functions frame
   * layout (MF.getFrameInfo()) is finalized.  Once the frame is finalized,
   * MO_FrameIndex operands are replaced with direct ants.  This method is
   * optional.
   */
  public void processFunctionBeforeFrameFinalized(MachineFunction mf) {

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
                                  MachineInstr mi) {
    eliminateFrameIndex(mf, mi, null);
  }

  public abstract void eliminateFrameIndex(MachineFunction mf,
                                           MachineInstr mi,
                                           RegScavenger rs);

  public abstract int getFrameRegister(MachineFunction mf);

  public int getFrameIndexOffset(MachineFunction mf, int fi) {
    TargetFrameLowering tfi = mf.getTarget().getFrameInfo();
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
    return getRegClassInfo(rc).spillSize;
  }

  public int getRegSizeInBit(MCRegisterClass rc) {
    return getRegClassInfo(rc).regSize / 8;
  }

  public int getRegSize(MCRegisterClass rc) {
    return getRegClassInfo(rc).regSize;
  }

  public int getSpillAlignmentInBit(MCRegisterClass rc) {
    return getRegClassInfo(rc).spillAlignment / 8;
  }

  public int getSpillAlignment(MCRegisterClass rc) {
    return getRegClassInfo(rc).spillAlignment;
  }

  public int[] getRegisterClassVTs(MCRegisterClass rc) {
    return getRegClassInfo(rc).vts;
  }

  public boolean isLegalTypeForRegClass(MCRegisterClass rc, MVT vt) {
    for (int v : getRegClassInfo(rc).vts) {
      if (v == vt.simpleVT)
        return true;
    }
    return false;
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
}
