/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 *
 * Please refer the LICENSE for detail.
 */

package backend.mc;

import backend.target.TID;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class MCInstrDesc {
  /**
   * The opcode of this instruction specfified with target machine.
   */
  public int opCode;
  /**
   * Assembly language mnemonic for the opcode.
   */
  public String name;
  /**
   * Number of args; -1 if variable #args
   */
  public int numOperands;
  /**
   * Number of args that are definitions.
   */
  public int numDefs;
  /**
   * enum identifying instr sched class.
   */
  public int schedClass;
  /**
   * flags identifying machine instr class
   */
  public int flags;
  /**
   * Target Specific Flag values
   */
  public int tSFlags;
  /**
   * Registers implicitly read by this instr
   */
  public int[] implicitUses;
  /**
   * Registers implicitly defined by this instr
   */
  public int[] implicitDefs;

  /**
   * Reg classes completely "clobbered".
   */
  public MCRegisterClass[] rcBarriers;

  /**
   * {@linkplain this#numOperands} entries about operands.
   */
  public MCOperandInfo[] opInfo;

  /**
   * The constructor that creats an instance of class {@linkplain MCInstrDesc}
   * with the specified several parameters.
   *
   * @param opcode      The opcode.
   * @param numOperands The number of operands are desired.
   * @param numDefs     The number of operand defined by this instruction.
   * @param name        The instruction memonic.
   * @param flags       The flags indicating machine instruction class.
   * @param tSFlags     The target-specified flags.
   * @param implUses    The implicitly used register.
   * @param implDefs    The implicit registers defined by this instruction.
   */
  public MCInstrDesc(int opcode,
                     int numOperands,
                     int numDefs,
                     int schedClass,
                     String name,
                     int flags,
                     int tSFlags,
                     int[] implUses,
                     int[] implDefs,
                     MCRegisterClass[] rcBarriers,
                     MCOperandInfo[] opInfo) {
    opCode = opcode;
    this.numOperands = numOperands;
    this.numDefs = numDefs;
    this.schedClass = schedClass;
    this.name = name;
    this.flags = flags;
    this.tSFlags = tSFlags;
    implicitUses = implUses;
    implicitDefs = implDefs;
    this.rcBarriers = rcBarriers;
    this.opInfo = opInfo;
  }

  /**
   * Get an operand tied to defined operand. The specified opNum is
   * index to general operand.
   *
   * @param opNum
   * @param constraint
   * @return
   */
  public int getOperandConstraint(int opNum, int constraint) {
    if (opNum < numOperands
        && (opInfo[opNum].constraints & (1 << constraint)) != 0) {
      int pos = 16 + constraint * 4;
      return (opInfo[opNum].constraints >> pos) & 0xf;
    }
    return -1;
  }

  public int getOpcode() {
    return opCode;
  }

  public String getName() {
    return name;
  }

  public int getNumOperands() {
    return numOperands;
  }

  public int getNumDefs() {
    return numDefs;
  }

  public boolean isVariadic() {
    return (flags & (1 << TID.Variadic)) != 0;
  }

  public boolean hasOptionalDef() {
    return (flags & (1 << TID.OptionalDef)) != 0;
  }

  public int[] getImplicitUses() {
    return implicitUses;
  }

  public int[] getImplicitDefs() {
    return implicitDefs;
  }

  public boolean hasImplicitUseOfPhysReg(int reg) {
    if (implicitUses != null)
      for (int i = 0; i < implicitUses.length; i++)
        if (implicitUses[i] == reg)
          return true;
    return false;
  }

  public boolean hasImplicitUDefOfPhysReg(int reg) {
    if (implicitDefs != null)
      for (int i = 0; i < implicitDefs.length; i++)
        if (implicitDefs[i] == reg)
          return true;
    return false;
  }

  public MCRegisterClass[] getRegClassBarriers() {
    return rcBarriers;
  }

  public int getSchedClass() {
    return schedClass;
  }

  public boolean isReturn() {
    return (flags & (1 << TID.Return)) != 0;
  }

  public boolean isCall() {
    return (flags & (1 << TID.Call)) != 0;
  }

  public boolean isBarrier() {
    return (flags & (1 << TID.Barrier)) != 0;
  }

  public boolean isTerminator() {
    return (flags & (1 << TID.Terminator)) != 0;
  }

  public boolean isBranch() {
    return (flags & (1 << TID.Branch)) != 0;
  }

  public boolean isIndirectBranch() {
    return (flags & (1 << TID.IndirectBranch)) != 0;
  }

  public boolean isConditionalBranch() {
    return isBranch() & !isBarrier() & !isIndirectBranch();
  }

  public boolean isUnconditionalBranch() {
    return isBranch() & isBarrier() & !isIndirectBranch();
  }

  public boolean isPredicable() {
    return (flags & (1 << TID.Predicable)) != 0;
  }

  public boolean isNotDuplicable() {
    return (flags & (1 << TID.NotDuplicable)) != 0;
  }

  public boolean hasDelaySlot() {
    return (flags & (1 << TID.DelaySlot)) != 0;
  }

  public boolean canFoldAsLoad() {
    return (flags & (1 << TID.FoldAsLoad)) != 0;
  }

  public boolean mayLoad() {
    return (flags & (1 << TID.MayLoad)) != 0;
  }

  public boolean mayStore() {
    return (flags & (1 << TID.MayStore)) != 0;
  }

  public boolean hasUnmodeledSideEffects() {
    return (flags & (1 << TID.SideEffects)) != 0;
  }

  public boolean isCommutable() {
    return (flags & (1 << TID.Commutable)) != 0;
  }

  public boolean isConvertibleTo3Addr() {
    return (flags & (1 << TID.ConvertibleToThreeAddress)) != 0;
  }

  public boolean usesCustomInsertionHook() {
    return (flags & (1 << TID.UsesCustomInserter)) != 0;
  }

  public boolean isRematerializable() {
    return (flags & (1 << TID.ReMaterializable)) != 0;
  }

  public boolean isAsCheapAsAMove() {
    return (flags & (1 << TID.CheapAsAMove)) != 0;
  }
}
