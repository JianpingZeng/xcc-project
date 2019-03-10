/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2015-2018, Jianping Zeng.
 * All rights reserved.
 *
 * Please refer the LICENSE for detail.
 */

package backend.target;

import backend.analysis.LiveVariables;
import backend.codegen.*;
import backend.codegen.dagisel.SDNode;
import backend.codegen.dagisel.SelectionDAG;
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
  protected int callFrameSetupOpcode;
  /**
   * The opcode of destroying stack frame for function being compiled.
   * If the target machine does not support it, this field will be -1.
   */
  protected int callFrameDestroyOpcode;

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

  public boolean isMoveInstr(MachineInstr mi,
                             OutRef<Integer> srcReg,
                             OutRef<Integer> destReg,
                             OutRef<Integer> srcSubIdx,
                             OutRef<Integer> destSubIdx) {
    return false;
  }

  public int isLoadFromStackSlot(MachineInstr mi,
                                 OutRef<Integer> frameIndex) {
    return 0;
  }

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
   * @param idxOfInst
   * @param lv
   * @return
   */
  public MachineInstr convertToThreeAddress(
      MachineBasicBlock mbb,
      int idxOfInst,
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

  /// findCommutedOpIndices - If specified MI is commutable, return the two
  /// operand indices that would swap value. Return true if the instruction
  /// is not in a form which this routine understands.
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

  /// AnalyzeBranch - Analyze the branching code at the end of mbb, returning
  /// true if it cannot be understood (e.g. it's a switch dispatch or isn't
  /// implemented for a target).  Upon success, this returns false and returns
  /// with the following information in various cases:
  ///
  /// 1. If this block ends with no branches (it just falls through to its succ)
  ///    just return false, leaving tb/fbb null.
  /// 2. If this block ends with only an unconditional branch, it sets tb to be
  ///    the destination block.
  /// 3. If this block ends with an conditional branch and it falls through to
  ///    a successor block, it sets tb to be the branch destination block and
  ///    a list of operands that evaluate the condition. These
  ///    operands can be passed to other RISCVGenInstrInfo methods to create new
  ///    branches.
  /// 4. If this block ends with a conditional branch followed by an
  ///    unconditional branch, it returns the 'true' destination in tb, the
  ///    'false' destination in fbb, and a list of operands that evaluate the
  ///    condition.  These operands can be passed to other RISCVGenInstrInfo
  ///    methods to create new branches.
  ///
  /// Note that RemoveBranch and InsertBranch must be implemented to support
  /// cases where this method returns success.
  ///
  /// If AllowModify is true, then this routine is allowed to modify the basic
  /// block (e.g. delete instructions after the unconditional branch).
  ///
  public boolean analyzeBranch(
      MachineBasicBlock mbb,
      MachineBasicBlock tbb,
      MachineBasicBlock fbb,
      ArrayList<MachineOperand> cond,
      boolean allowModify) {
    return true;
  }

  /// RemoveBranch - Remove the branching code at the end of the specific MBB.
  /// This is only invoked in cases where AnalyzeBranch returns success. It
  /// returns the number of instructions that were removed.
  public int removeBranch(MachineBasicBlock mbb) {
    Util.assertion(false, "Target didn't implement RISCVGenInstrInfo::RemoveBranch!");
    return 0;
  }

  /// InsertBranch - Insert branch code into the end of the specified
  /// MachineBasicBlock.  The operands to this method are the same as those
  /// returned by AnalyzeBranch.  This is only invoked in cases where
  /// AnalyzeBranch returns success. It returns the number of instructions
  /// inserted.
  ///
  /// It is also invoked by tail merging to add unconditional branches in
  /// cases where AnalyzeBranch doesn't apply because there was no original
  /// branch to analyze.  At least this much must be implemented, else tail
  /// merging needs to be disabled.
  public int insertBranch(MachineBasicBlock mbb,
                          MachineBasicBlock tbb,
                          MachineBasicBlock fbb,
                          ArrayList<MachineOperand> cond) {
    Util.assertion(false, "Target didn't implement RISCVGenInstrInfo::InsertBranch!");
    return 0;
  }

  /// copyRegToReg - Emit instructions to copy between a pair of registers. It
  /// returns false if the target does not how to copy between the specified
  /// registers.
  public boolean copyRegToReg(MachineBasicBlock mbb,
                              int insertPos,
                              int dstReg,
                              int srcReg,
                              MCRegisterClass dstRC,
                              MCRegisterClass srcRC) {
    Util.assertion(false, "Target didn't implement TargetLowering::copyRegToReg");
    return false;
  }

  /// storeRegToStackSlot - Store the specified register of the given register
  /// class to the specified stack frame index. The store instruction is to be
  /// added to the given machine basic block before the specified machine
  /// instruction. If isDeclare is true, the register operand is the last use and
  /// must be marked kill.
  public void storeRegToStackSlot(MachineBasicBlock mbb,
                                  int pos,
                                  int srcReg,
                                  boolean isKill,
                                  int frameIndex,
                                  MCRegisterClass rc) {
    Util.assertion(false, "Target didn't implement RISCVGenInstrInfo::storeRegToStackSlot!");
  }

  /// loadRegFromStackSlot - Load the specified register of the given register
  /// class from the specified stack frame index. The load instruction is to be
  /// added to the given machine basic block before the specified machine
  /// instruction.
  public void loadRegFromStackSlot(MachineBasicBlock mbb,
                                   int pos,
                                   int destReg,
                                   int frameIndex,
                                   MCRegisterClass rc) {
    Util.assertion(false, "Target didn't implement RISCVGenInstrInfo::loadRegFromStackSlot!");
  }

  /// spillCalleeSavedRegisters - Issues instruction(s) to spill all callee
  /// saved registers and returns true if it isn't possible / profitable to do
  /// so by issuing a series of store instructions via
  /// storeRegToStackSlot(). Returns false otherwise.
  public boolean spillCalleeSavedRegisters(MachineBasicBlock mbb,
                                           int pos,
                                           ArrayList<CalleeSavedInfo> csi) {
    return false;
  }

  /// restoreCalleeSavedRegisters - Issues instruction(s) to restore all callee
  /// saved registers and returns true if it isn't possible / profitable to do
  /// so by issuing a series of load instructions via loadRegToStackSlot().
  /// Returns false otherwise.
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

  /// foldMemoryOperand - Attempt to fold a load or store of the specified stack
  /// slot into the specified machine instruction for the specified operand(s).
  /// If this is possible, a new instruction is returned with the specified
  /// operand folded, otherwise NULL is returned. The client is responsible for
  /// removing the old instruction and adding the new one in the instruction
  /// stream.
  public abstract MachineInstr foldMemoryOperand(
      MachineFunction mf,
      MachineInstr mi,
      TIntArrayList ops,
      int frameIndex);

  /// foldMemoryOperand - Same as the previous version except it allows folding
  /// of any load and store from / to any address, not just from a specific
  /// stack slot.
  public abstract MachineInstr foldMemoryOperand(
      MachineFunction mf,
      MachineInstr mi,
      TIntArrayList ops,
      MachineInstr loadMI);


  /// foldMemoryOperandImpl - Target-dependent implementation for
  /// foldMemoryOperand. Target-independent code in foldMemoryOperand will
  /// take care of adding a MachineMemOperand to the newly created instruction.
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

  /// foldMemoryOperandImpl - Target-dependent implementation for
  /// foldMemoryOperand. Target-independent code in foldMemoryOperand will
  /// take care of adding a MachineMemOperand to the newly created instruction.
  public MachineInstr foldMemoryOperandImpl(
      MachineFunction mf,
      MachineInstr mi,
      TIntArrayList ops,
      MachineInstr loadMI) {
    return null;
  }


  /// canFoldMemoryOperand - Returns true for the specified load / store if
  /// folding is possible.
  public boolean canFoldMemoryOperand(MachineInstr mi, TIntArrayList ops) {
    return false;
  }

  /// unfoldMemoryOperand - Separate a single instruction which folded a load or
  /// a store or a load and a store into two or more instruction. If this is
  /// possible, returns true as well as the new instructions by reference.
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


  /// getOpcodeAfterMemoryUnfold - Returns the opcode of the would be new
  /// instruction after load / store are unfolded from an instruction of the
  /// specified opcode. It returns zero if the specified unfolding is not
  /// possible.
  public int getOpcodeAfterMemoryUnfold(
      int opc,
      boolean unfoldLoad,
      boolean unfoldStore) {
    return 0;
  }

  /// BlockHasNoFallThrough - Return true if the specified block does not
  /// fall-through into its successor block.  This is primarily used when a
  /// branch is unanalyzable.  It is useful for things like unconditional
  /// indirect branches (jump tables).
  public boolean blockHasNoFallThrough(MachineBasicBlock mbb) {
    return false;
  }

  /// ReverseBranchCondition - Reverses the branch condition of the specified
  /// condition list, returning false on success and true if it cannot be
  /// reversed.
  public boolean reverseBranchCondition(ArrayList<MachineOperand> cond) {
    return true;
  }

  /// insertNoop - Insert a noop into the instruction stream at the specified
  /// point.
  public abstract void insertNoop(MachineBasicBlock mbb, int pos);

  /// isPredicated - Returns true if the instruction is already predicated.
  ///
  public boolean isPredicated(MachineInstr mi) {
    return false;
  }

  /// isUnpredicatedTerminator - Returns true if the instruction is a
  /// terminator instruction that has not been predicated.
  public abstract boolean isUnpredicatedTerminator(MachineInstr mi);

  /// predicateInstruction - Convert the instruction into a predicated
  /// instruction. It returns true if the operation was successful.
  public abstract boolean predicateInstruction(
      MachineInstr mi,
      ArrayList<MachineOperand> pred);

  /// SubsumesPredicate - Returns true if the first specified predicate
  /// subsumes the second, e.g. GE subsumes GT.
  public boolean subsumesPredicate(
      ArrayList<MachineOperand> pred1,
      ArrayList<MachineOperand> pred2) {
    return false;
  }

  /// DefinesPredicate - If the specified instruction defines any predicate
  /// or condition code register(s) used for predication, returns true as well
  /// as the definition predicate(s) by reference.
  public boolean definesPredicate(
      MachineInstr mi,
      ArrayList<MachineOperand> pred) {
    return false;
  }

  /// isSafeToMoveRegClassDefs - Return true if it's safe to move a machine
  /// instruction that defines the specified register class.
  public boolean isSafeToMoveRegClassDefs(MCRegisterClass rc) {
    return true;
  }

  /// isDeadInstruction - Return true if the instruction is considered dead.
  /// This allows some late codegen passes to delete them.
  public abstract boolean isDeadInstruction(MachineInstr mi);

  /// GetInstSize - Returns the size of the specified Instruction.
  ///
  public int getInstSizeInBytes(MachineInstr mi) {
    Util.assertion(false, "Target didn't implement RISCVGenInstrInfo::GetInstSize!");
    return 0;
  }

  /// GetFunctionSizeInBytes - Returns the size of the specified
  /// MachineFunction.
  ///
  public abstract int getFunctionSizeInBytes(MachineFunction mf);

  public int getGlobalBaseReg(MachineFunction mf) {
    return 0;
  }

  /// Measure the specified inline asm to determine an approximation of its
  /// length.
  // public abstract int getInlineAsmLength(String Str, MCAsmInfo TAI);
}
