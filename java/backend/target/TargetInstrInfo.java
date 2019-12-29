/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2015-2019, Jianping Zeng.
 * All rights reserved.
 *
 * Please refer the LICENSE for detail.
 */

package backend.target;

import backend.analysis.LiveVariables;
import backend.codegen.*;
import backend.codegen.dagisel.SDNode;
import backend.codegen.dagisel.SelectionDAG;
import backend.debug.DebugLoc;
import backend.mc.MCInstrDesc;
import backend.mc.MCInstrInfo;
import backend.mc.MCRegisterClass;
import gnu.trove.list.array.TIntArrayList;
import tools.OutRef;
import tools.Util;

import java.util.ArrayList;

/**
 * This class is an Interface to description of machine instructions, which
 * used for representing the information about machine instruction which
 * specified with target.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public abstract class TargetInstrInfo extends MCInstrInfo {
  /**
   * The opcode of setting up stack frame for function being compiled.
   * If the target machine does not support it, this field will be -1.
   */
  private int callFrameSetupOpcode;
  /**
   * The opcode of destroying stack frame for function being compiled.
   * If the target machine does not support it, this field will be -1.
   */
  private int callFrameDestroyOpcode;

  public TargetInstrInfo(int frameSetupOp, int frameDestroyOp) {
    callFrameSetupOpcode = frameSetupOp;
    callFrameDestroyOpcode = frameDestroyOp;
  }

  /**
   * This method return the opcode of the frame setup instructions if
   * they exist (-1 otherwise).  Some targets use pseudo instructions in order
   * to abstract away the difference between operating with a frame pointer
   * and operating without, through the use of these two instructions.
   *
   * @return
   */
  public int getCallFrameSetupOpcode() {
    return callFrameSetupOpcode;
  }

  /**
   * This method return the opcode of the frame destroy instructions if
   * they exist (-1 otherwise).  Some targets use pseudo instructions in order
   * to abstract away the difference between operating with a frame pointer
   * and operating without, through the use of these two instructions.
   *
   * @return
   */
  public int getCallFrameDestroyOpcode() {
    return callFrameDestroyOpcode;
  }

  //===--------------------------------------------------------------------===//
  // Interfaces used by the register allocator and stack frame manipulation
  // passes to move data around between registers, immediates and memory.
  //

  /**
   * This method is called during prolog/epilog code insertion to eliminate
   * call frame setup and destroy pseudo instructions (but only if the
   * Target is using them).  It is responsible for eliminating these
   * instructions, replacing them with concrete instructions.  This method
   * need only be implemented if using call frame setup/destroy pseudo
   * instructions.
   */
  public void eliminateCallFramePseudoInstr(
      MachineFunction mf,
      MachineInstr old) {
    Util.assertion((getCallFrameSetupOpcode() == -1 && getCallFrameDestroyOpcode() == -1),
        "eliminateCallFramePseudoInstr must be implemented if using"
            + " call frame setup/destroy pseudo instructions!");

    Util.assertion("Call Frame Pseudo Instructions do not exist on this target!");
  }

  public boolean isTriviallyReMaterializable(MachineInstr mi) {
    return mi.getDesc().isRematerializable() && isReallyTriviallyReMaterializable(mi);
  }

  protected boolean isReallyTriviallyReMaterializable(MachineInstr mi) {
    return false;
  }

  /**
   * Check the given machine instrution is a move register to register instruction. If it is,
   * return true and pass the source and destination register in the {@arg srcReg} and {@arg destReg}.
   * @param mi
   * @param srcReg
   * @param destReg
   * @param srcSubIdx
   * @param destSubIdx
   * @return
   */
  public boolean isMoveInstr(MachineInstr mi,
                             OutRef<Integer> srcReg,
                             OutRef<Integer> destReg,
                             OutRef<Integer> srcSubIdx,
                             OutRef<Integer> destSubIdx) {
    return false;
  }

  /**
   * Check the given instruction is a load from the stack slot. If it is, pass the frame index
   * to the specified frame slot in the {@arg frameIndex} and return the stack pointer. Otherwise,
   * return 0.
   * @param mi
   * @param frameIndex
   * @return
   */
  public int isLoadFromStackSlot(MachineInstr mi,
                                 OutRef<Integer> frameIndex) {
    return 0;
  }

  /**
   * Check the given instruction is a store a value to the stack slot. If it is, passing the
   * stack slot index in the {@arg frameIndex} and return stack pointer. Otherwise, return 0;
   * @param mi
   * @param frameIndex
   * @return
   */
  public int isStoreToStackSlot(MachineInstr mi,
                                OutRef<Integer> frameIndex) {
    return 0;
  }

  public abstract void reMaterialize(MachineBasicBlock mbb,
                                     int insertPos,
                                     int destReg,
                                     int subIdx,
                                     MachineInstr origin);

  public boolean isInvariantLoad(MachineInstr mi) {
    return false;
  }

  /**
   * This method must be implemented by targets that
   * set the M_CONVERTIBLE_TO_3_ADDR flag.  When this flag is set, the target
   * may be able to convert a two-address instruction into one or more true
   * three-address instructions on demand.  This allows the X86 target (for
   * example) to convert ADD and SHL instructions into LEA instructions if they
   * would require register copies due to two-addressness.
   * <p>
   * This method returns a null pointer if the transformation cannot be
   * performed, otherwise it returns the last new instruction.
   *
   * @param mbb
   * @param mbbi
   * @param lv
   * @return
   */
  public MachineInstr convertToThreeAddress(
      MachineBasicBlock mbb,
      OutRef<Integer> mbbi,
      LiveVariables lv) {
    return null;
  }

  public MachineInstr commuteInstruction(MachineInstr mi) {
    return commuteInstruction(mi, false);
  }

  /**
   * If a target has any instructions that are commutable,
   * but require converting to a different instruction or making non-trivial
   * changes to commute them, this method can overloaded to do this.  The
   * default implementation of this method simply swaps the first two operands
   * of MI and returns it.
   * <p>
   * If a target wants to make more aggressive changes, they can construct and
   * return a new machine instruction.  If an instruction cannot commute, it
   * can also return null.
   * <p>
   * If NewMI is true, then a new machine instruction must be created.
   *
   * @param mi
   * @param newMI
   * @return
   */
  public abstract MachineInstr commuteInstruction(MachineInstr mi, boolean newMI);

  /**
   * If specified MI is commutable, return the two operand indices that would swap value. Return true if the instruction
   * is not in a form which this routine understands.
   * @param mi
   * @param srcOpIdx1
   * @param srcOpIdx2
   * @return
   */
  public abstract boolean findCommutedOpIndices(
      MachineInstr mi,
      OutRef<Integer> srcOpIdx1,
      OutRef<Integer> srcOpIdx2);

  public boolean analyzeBranch(
      MachineBasicBlock mbb,
      MachineBasicBlock tbb,
      MachineBasicBlock fbb,
      ArrayList<MachineOperand> cond) {
    return analyzeBranch(mbb, tbb, fbb, cond, false);
  }

  /**
   * Analyze the branching code at the end of mbb, returning
   * true if it cannot be understood (e.g. it's a switch dispatch or isn't
   * implemented for a target).  Upon success, this returns false and returns
   * with the following information in various cases:
   *
   * 1. If this block ends with no branches (it just falls through to its succ)
   *    just return false, leaving tb/fbb null.
   * 2. If this block ends with only an unconditional branch, it sets tb to be
   *    the destination block.
   * 3. If this block ends with an conditional branch and it falls through to
   *    a successor block, it sets tb to be the branch destination block and
   *    a list of operands that evaluate the condition. These
   *    operands can be passed to other RISCVGenInstrInfo methods to create new
   *    branches.
   * 4. If this block ends with a conditional branch followed by an
   *    unconditional branch, it returns the 'true' destination in tb, the
   *    'false' destination in fbb, and a list of operands that evaluate the
   *    condition.  These operands can be passed to other RISCVGenInstrInfo
   *    methods to create new branches.
   *
   * Note that RemoveBranch and InsertBranch must be implemented to support
   * cases where this method returns success.
   *
   * If AllowModify is true, then this routine is allowed to modify the basic
   * block (e.g. delete instructions after the unconditional branch).
   * @param mbb
   * @param tbb
   * @param fbb
   * @param cond
   * @param allowModify
   * @return
   */
  public boolean analyzeBranch(
      MachineBasicBlock mbb,
      MachineBasicBlock tbb,
      MachineBasicBlock fbb,
      ArrayList<MachineOperand> cond,
      boolean allowModify) {
    return true;
  }

  /**
   * Remove the branching code at the end of the specific MBB.
   * This is only invoked in cases where AnalyzeBranch returns success. It
   * returns the number of instructions that were removed.
   * @param mbb
   * @return
   */
  public int removeBranch(MachineBasicBlock mbb) {
    Util.assertion("Target didn't implement RISCVGenInstrInfo::RemoveBranch!");
    return 0;
  }

  /**
   * Insert branch code into the end of the specified
   * MachineBasicBlock.  The operands to this method are the same as those
   * returned by AnalyzeBranch.  This is only invoked in cases where
   * AnalyzeBranch returns success. It returns the number of instructions
   * inserted.
   *
   * It is also invoked by tail merging to add unconditional branches in
   * cases where AnalyzeBranch doesn't apply because there was no original
   * branch to analyze.  At least this much must be implemented, else tail
   * merging needs to be disabled.
   * @param mbb
   * @param tbb
   * @param fbb
   * @param cond
   * @param dl
   * @return
   */
  public int insertBranch(MachineBasicBlock mbb,
                          MachineBasicBlock tbb,
                          MachineBasicBlock fbb,
                          ArrayList<MachineOperand> cond,
                          DebugLoc dl) {
    Util.assertion("Target didn't implement RISCVGenInstrInfo::InsertBranch!");
    return 0;
  }

  /**
   * Emit instructions to copy between a pair of registers. It
   * returns false if the target does not how to copy between the specified
   * registers.
   * @param mbb
   * @param insertPos
   * @param dstReg
   * @param srcReg
   * @param dstRC
   * @param srcRC
   * @return
   */
  public boolean copyPhysReg(MachineBasicBlock mbb,
                             int insertPos,
                             int dstReg,
                             int srcReg,
                             MCRegisterClass dstRC,
                             MCRegisterClass srcRC) {
    Util.assertion("Target didn't implement TargetLowering::copyPhysReg");
    return false;
  }

  /**
   * Store the specified register of the given register
   * class to the specified stack frame index. The store instruction is to be
   * added to the given machine basic block before the specified machine
   * instruction. If isDeclare is true, the register operand is the last use and
   * must be marked kill.
   * @param mbb
   * @param pos
   * @param srcReg
   * @param isKill
   * @param frameIndex
   * @param rc
   */
  public void storeRegToStackSlot(MachineBasicBlock mbb,
                                  int pos,
                                  int srcReg,
                                  boolean isKill,
                                  int frameIndex,
                                  MCRegisterClass rc) {
    Util.assertion("Target didn't implement RISCVGenInstrInfo::storeRegToStackSlot!");
  }

  /**
   * Load the specified register of the given register
   * class from the specified stack frame index. The load instruction is to be
   * added to the given machine basic block before the specified machine
   * instruction.
   * @param mbb
   * @param pos
   * @param destReg
   * @param frameIndex
   * @param rc
   */
  public void loadRegFromStackSlot(MachineBasicBlock mbb,
                                   int pos,
                                   int destReg,
                                   int frameIndex,
                                   MCRegisterClass rc) {
    Util.assertion("Target didn't implement RISCVGenInstrInfo::loadRegFromStackSlot!");
  }

  /**
   * Issues instruction(s) to spill all callee
   * saved registers and returns true if it isn't possible / profitable to do
   * so by issuing a series of store instructions via
   * storeRegToStackSlot(). Returns false otherwise.
   * @param mbb
   * @param pos
   * @param csi
   * @return
   */
  public boolean spillCalleeSavedRegisters(MachineBasicBlock mbb,
                                           int pos,
                                           ArrayList<CalleeSavedInfo> csi) {
    return false;
  }

  /**
   * Issues instruction(s) to restore all callee
   * saved registers and returns true if it isn't possible / profitable to do
   * so by issuing a series of load instructions via loadRegToStackSlot().
   * Returns false otherwise.
   * @param mbb
   * @param pos
   * @param csi
   * @return
   */
  public boolean restoreCalleeSavedRegisters(
      MachineBasicBlock mbb,
      int pos,
      ArrayList<CalleeSavedInfo> csi) {
    return false;
  }

  public MachineInstr foldMemoryOperand(MachineFunction mf,
                                        MachineInstr mi,
                                        int ops,
                                        int frameIndex) {
    TIntArrayList list = new TIntArrayList();
    list.add(ops);
    return foldMemoryOperand(mf, mi, list, frameIndex);
  }

  /**
   * Attempt to fold a load or store of the specified stack
   * slot into the specified machine instruction for the specified operand(s).
   * If this is possible, a new instruction is returned with the specified
   * operand folded, otherwise NULL is returned. The client is responsible for
   * removing the old instruction and adding the new one in the instruction
   * stream.
   * @param mf
   * @param mi
   * @param ops
   * @param frameIndex
   * @return
   */
  public abstract MachineInstr foldMemoryOperand(
      MachineFunction mf,
      MachineInstr mi,
      TIntArrayList ops,
      int frameIndex);

  /**
   * Same as the previous version except it allows folding
   * of any load and store from / to any address, not just from a specific
   * stack slot.
   * @param mf
   * @param mi
   * @param ops
   * @param loadMI
   * @return
   */
  public abstract MachineInstr foldMemoryOperand(
      MachineFunction mf,
      MachineInstr mi,
      TIntArrayList ops,
      MachineInstr loadMI);


  /**
   * Target-dependent implementation for
   * foldMemoryOperand. Target-independent code in foldMemoryOperand will
   * take care of adding a MachineMemOperand to the newly created instruction.
   * @param mf
   * @param mi
   * @param ops
   * @param frameIndex
   * @return
   */
  protected MachineInstr foldMemoryOperandImpl(
      MachineFunction mf,
      MachineInstr mi,
      TIntArrayList ops,
      int frameIndex) {
    return null;
  }

  protected MachineInstr foldMemoryOperandImpl(
      MachineFunction mf,
      MachineInstr mi,
      int i,
      ArrayList<MachineOperand> mos,
      int align) {
    return null;
  }

  /**
   * Target-dependent implementation for
   * foldMemoryOperand. Target-independent code in foldMemoryOperand will
   * take care of adding a MachineMemOperand to the newly created instruction.
   * @param mf
   * @param mi
   * @param ops
   * @param loadMI
   * @return
   */
  public MachineInstr foldMemoryOperandImpl(
      MachineFunction mf,
      MachineInstr mi,
      TIntArrayList ops,
      MachineInstr loadMI) {
    return null;
  }


  /**
   * Returns true for the specified load / store if folding is possible.
   * @param mi
   * @param ops
   * @return
   */
  public boolean canFoldMemoryOperand(MachineInstr mi, TIntArrayList ops) {
    return false;
  }

  /**
   * Separate a single instruction which folded a load or
   * a store or a load and a store into two or more instruction. If this is
   * possible, returns true as well as the new instructions by reference.
   * @param mf
   * @param mi
   * @param reg
   * @param unfoldLoad
   * @param unfoldStore
   * @param newMIs
   * @return
   */
  public boolean unfoldMemoryOperand(
      MachineFunction mf,
      MachineInstr mi,
      int reg,
      boolean unfoldLoad,
      boolean unfoldStore,
      ArrayList<MachineInstr> newMIs) {
    return false;
  }

  public boolean unfoldMemoryOperand(
      SelectionDAG dag, SDNode node,
      ArrayList<SDNode> newNodes) {
    return false;
  }


  /**
   * Returns the opcode of the would be new
   * instruction after load / store are unfolded from an instruction of the
   * specified opcode. It returns zero if the specified unfolding is not
   * possible.
   * @param opc
   * @param unfoldLoad
   * @param unfoldStore
   * @return
   */
  public int getOpcodeAfterMemoryUnfold(
      int opc,
      boolean unfoldLoad,
      boolean unfoldStore) {
    return 0;
  }

  /**
   * Return true if the specified block does not
   * fall-through into its successor block.  This is primarily used when a
   * branch is unanalyzable.  It is useful for things like unconditional
   * indirect branches (jump tables).
   * @param mbb
   * @return
   */
  public boolean blockHasNoFallThrough(MachineBasicBlock mbb) {
    return false;
  }

  /**
   * Reverses the branch condition of the specified
   * condition list, returning false on success and true if it cannot be
   * reversed.
   * @param cond
   * @return
   */
  public boolean reverseBranchCondition(ArrayList<MachineOperand> cond) {
    return true;
  }

  /**
   * Insert a noop into the instruction stream at the specified point.
   * @param mbb
   * @param pos
   */
  public void insertNoop(MachineBasicBlock mbb, int pos) {
    Util.shouldNotReachHere("Target doesn't implement the insertNoop!");
  }

  /**
   * Returns true if the instruction is already predicated.
   * @param mi
   * @return
   */
  public boolean isPredicated(MachineInstr mi) {
    return false;
  }

  /**
   * Returns true if the instruction is a terminator instruction that has not been predicated.
   * @param mi
   * @return
   */
  public boolean isUnpredicatedTerminator(MachineInstr mi) {
    MCInstrDesc tid = mi.getDesc();
    if (!tid.isTerminator())
      return false;

    if (tid.isBranch() && !tid.isBarrier())
      return true;
    if (!tid.isPredicable())
      return true;
    return !isPredicated(mi);
  }

  /**
   * Convert the instruction into a predicated
   * instruction. It returns true if the operation was successful.
   * @param mi
   * @param pred
   * @return
   */
  public abstract boolean predicateInstruction(
      MachineInstr mi,
      ArrayList<MachineOperand> pred);

  /**
   * Returns true if the first specified predicate subsumes the second, e.g. GE subsumes GT.
   * @param pred1
   * @param pred2
   * @return
   */
  public boolean subsumesPredicate(
      ArrayList<MachineOperand> pred1,
      ArrayList<MachineOperand> pred2) {
    return false;
  }

  /**
   * If the specified instruction defines any predicate
   * or condition code register(s) used for predication, returns true as well
   * as the definition predicate(s) by reference.
   * @param mi
   * @param pred
   * @return
   */
  public boolean definesPredicate(
      MachineInstr mi,
      ArrayList<MachineOperand> pred) {
    return false;
  }

  /**
   * Return true if it's safe to move a machine instruction that defines the specified register class.
   * @param rc
   * @return
   */
  public boolean isSafeToMoveRegClassDefs(MCRegisterClass rc) {
    return true;
  }

  /**
   * Returns the size of the specified Instruction.
   * @param mi
   * @return
   */
  public int getInstSizeInBytes(MachineInstr mi) {
    Util.assertion("Target didn't implement RISCVGenInstrInfo::GetInstSize!");
    return 0;
  }

  /**
   * Returns the size of the specified MachineFunction.
   */
  public abstract int getFunctionSizeInBytes(MachineFunction mf);

  public int getGlobalBaseReg(MachineFunction mf) {
    return 0;
  }

  /// Measure the specified inline asm to determine an approximation of its
  /// length.
  // public abstract int getInlineAsmLength(String Str, MCAsmInfo TAI);
}
