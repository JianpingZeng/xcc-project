/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2015-2018, Jianping Zeng.
 * All rights reserved.
 *
 * Please refer the LICENSE for detail.
 */

package backend.target.x86;

import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineFrameInfo;
import backend.codegen.MachineFunction;
import backend.codegen.MachineInstr;
import backend.target.TargetFrameLowering;
import backend.target.TargetInstrInfo;
import backend.target.TargetRegisterInfo;
import tools.OutRef;

import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.target.TargetOptions.DisableFramePointerElim;
import static backend.target.x86.X86GenInstrNames.*;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class X86FrameLowering extends TargetFrameLowering {
  private X86TargetMachine tm;
  private X86Subtarget subtarget;

  public X86FrameLowering(X86TargetMachine tm, X86Subtarget subtarget) {
    super(StackDirection.StackGrowDown, subtarget.getStackAlignemnt(),
        subtarget.is64Bit() ? -8 : -4);
    this.tm = tm;
    this.subtarget = subtarget;
  }

  /**
   * This method insert prologue code into the function. Such as push callee-saved
   * registers onto the stack, which automatically adjust the stack pointer.
   * Adjust the stack pointer to space for local variables. Also emit labels
   * used by the exception handler to generate the exception handling frames.
   *
   * @param mf
   */
  @Override
  public void emitPrologue(MachineFunction mf) {

  }

  private int mergeSPUpdates(
      MachineBasicBlock mbb,
      OutRef<Integer> mbbi,
      int stackPtr,
      boolean doMergeWithPrevious) {
    if ((doMergeWithPrevious && mbbi.get() == 0) ||
        (!doMergeWithPrevious && mbbi.get() == mbb.size())) {
      return 0;
    }

    int prev = doMergeWithPrevious ? mbbi.get() - 1 : mbbi.get();
    int next = doMergeWithPrevious ? -1 : mbbi.get() + 1;
    int opc = mbb.getInstAt(prev).getOpcode();
    int offset = 0;
    if ((opc == ADD64ri32 || opc == ADD64ri8 ||
        opc == ADD32ri || opc == ADD32ri8) &&
        mbb.getInstAt(prev).getOperand(0).getReg() == stackPtr) {
      offset += mbb.getInstAt(prev).getOperand(2).getImm();
      mbb.remove(prev);
      if (!doMergeWithPrevious)
        mbbi.set(next);
    } else if ((opc == SUB64ri32 || opc == SUB64ri8 ||
        opc == SUB32ri || opc == SUB32ri8) && mbb.getInstAt(prev).getOperand(0)
        .getReg() == stackPtr) {
      offset -= mbb.getInstAt(prev).getOperand(2).getImm();
      mbb.remove(prev);
      if (!doMergeWithPrevious)
        mbbi.set(next);
    }
    return offset;
  }

  private void mergeSPUpdatesDown(
      MachineBasicBlock mbb,
      OutRef<Integer> mbbi,
      int stackPtr,
      OutRef<Integer> numBytes) {
  }

  private void emitSPUpdate(MachineBasicBlock mbb,
                            int mbbi,
                            int stackPtr, int numBytes, boolean is64Bit,
                            TargetInstrInfo tii) {
    boolean isSub = numBytes < 0;
    long offset = isSub ? -numBytes : numBytes;
    int opc = isSub
        ? ((offset < 128) ?
        (is64Bit ? SUB64ri8 : SUB32ri8) :
        (is64Bit ? SUB64ri32 : SUB32ri))
        : ((offset < 128) ?
        (is64Bit ? ADD64ri8 : ADD32ri8) :
        (is64Bit ? ADD64ri32 : ADD32ri));
    long chunk = (1L << 31) - 1;
    while (offset != 0) {
      long thisVal = (offset > chunk) ? chunk : offset;
      MachineInstr mi = buildMI(mbb, mbbi,
          tii.get(opc), stackPtr).addReg(stackPtr)
          .addImm(thisVal).getMInstr();
      mi.getOperand(3).setIsDead(true);
      offset -= thisVal;
    }
  }

  /**
   * This method insert epilogue code into the function.
   *
   * @param mf
   * @param mbb
   */
  @Override
  public void emitEpilogue(MachineFunction mf,
                           MachineBasicBlock mbb) {

  }

  /**
   * Return true if the specified function should have a dedicated stack pointer
   * register. This is true if function has variable sized objects or if frame
   * pointer elimination is disabled.
   * <p>
   * the frame pointer is usually EBP in X86 target machine.
   *
   * @param mf
   * @return
   */
  public boolean hasFP(MachineFunction mf) {
    MachineFrameInfo mfi = mf.getFrameInfo();
    TargetRegisterInfo tri = tm.getRegisterInfo();
    return (DisableFramePointerElim.value || tri.needsStackRealignment(mf)
        || mfi.hasVarSizedObjects() || mfi.isFrameAddressTaken());
  }

  @Override
  public boolean hasReservedCallFrame(MachineFunction mf) {
    return !mf.getFrameInfo().hasVarSizedObjects();
  }

}
