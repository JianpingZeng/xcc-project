/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2015-2019, Jianping Zeng.
 * All rights reserved.
 *
 * Please refer the LICENSE for detail.
 */

package backend.target.x86;

import backend.codegen.*;
import backend.support.Attribute;
import backend.target.TargetData;
import backend.target.TargetFrameLowering;
import backend.target.TargetInstrInfo;
import backend.target.TargetRegisterInfo;
import backend.value.Function;
import tools.OutRef;
import tools.Pair;
import tools.Util;

import static backend.codegen.MachineInstrBuilder.addRegOffset;
import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.target.TargetOptions.DisableFPElim;
import static backend.target.TargetOptions.DisableFPEliMLeaf;
import static backend.target.x86.X86GenInstrNames.*;
import static backend.target.x86.X86GenRegisterNames.*;

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
    MachineBasicBlock mbb = mf.getEntryBlock();
    Function fn = mf.getFunction();
    int mbbi = 0;  // a index position where a new instr will inserts.
    MachineFrameInfo mfi = mf.getFrameInfo();
    MachineInstr mi;
    X86MachineFunctionInfo x86FI = (X86MachineFunctionInfo) mf.getInfo();
    int maxAlign = mfi.getMaxAlignment();
    int stackSize = mfi.getStackSize();
    boolean hasFP = hasFP(mf);
    boolean is64Bit = subtarget.is64Bit();
    X86Subtarget subtarget = (X86Subtarget) mf.getTarget().getSubtarget();
    X86RegisterInfo tri = subtarget.getRegisterInfo();
    int slotSize = tri.getSlotSize();
    X86InstrInfo tii = subtarget.getInstrInfo();
    int stackPtr = tri.getStackRegister();
    int framePtr = tri.getFrameRegister(mf);

    int tailCallReturnAddrDelta = x86FI.getTCReturnAddrDelta();
    if (tailCallReturnAddrDelta < 0) {
      x86FI.setCalleeSavedFrameSize(x86FI.getCalleeSavedFrameSize() - tailCallReturnAddrDelta);
    }

    if (is64Bit && !fn.hasFnAttr(Attribute.NoRedZone) &&
        !tri.needsStackRealignment(mf) && !mfi.hasVarSizedObjects()
        && !mfi.hasCalls() && !subtarget.isTargetWin64()) {
      int minSize = x86FI.getCalleeSavedFrameSize();
      if (hasFP)
        minSize += slotSize;
      stackSize = Math.max(minSize, stackSize > 128 ? stackSize - 128 : 0);
      mfi.setStackSize(stackSize);
    } else if (subtarget.isTargetWin64()) {
      stackSize += 32;
      mfi.setStackSize(stackSize);
    }

    if (tailCallReturnAddrDelta < 0) {
      mi = buildMI(mbb, mbbi, tii.get(is64Bit ? SUB64ri32 : SUB32ri),
          stackPtr).addReg(stackPtr).addImm(-tailCallReturnAddrDelta).getMInstr();
      mi.getOperand(3).setIsDead(true);   // The EFLAGS implicit def is dead.
    }

    // Get the number of bytes to allocate from the FrameInfo.
    int numBytes = 0;
    TargetData td = tm.getTargetData();

    int stackGrowth = (tm.getFrameLowering().getStackGrowDirection()
        == TargetFrameLowering.StackDirection.StackGrowUp ?
        td.getPointerSize() : -td.getPointerSize());

    if (hasFP) {
      // get the offset of the stack slot for the %ebp register.
      // Note that: this offset is away from ESP.
      int frameSize = stackSize - slotSize;
      if (tri.needsStackRealignment(mf))
        frameSize = (frameSize + maxAlign - 1) / maxAlign * maxAlign;
      numBytes = frameSize - x86FI.getCalleeSavedFrameSize();
      mfi.setOffsetAdjustment(-numBytes);

      buildMI(mbb, mbbi++, tii.get(is64Bit ? PUSH64r : PUSH32r))
          .addReg(framePtr, MachineOperand.RegState.Kill);
      // update EBP with new base value
      buildMI(mbb, mbbi++, tii.get(is64Bit ? MOV64rr : MOV32rr), framePtr)
          .addReg(stackPtr);

      // mark the frameptr as live-in in every block excepts the entry.
      for (int i = 1, e = mf.getNumBlocks(); i < e; i++) {
        mf.getBasicBlocks().get(i).addLiveIn(framePtr);

        // realign stack.
        if (tri.needsStackRealignment(mf)) {
          mi = buildMI(mbb, mbbi++,
              tii.get(is64Bit ? AND64ri32 : AND32ri), stackPtr)
              .addReg(stackPtr).addImm(-maxAlign).getMInstr();
          mi.getOperand(3).setIsDead(true);
        }
      }
    } else {
      numBytes = stackSize - x86FI.getCalleeSavedFrameSize();
    }

    boolean pushedRegs = false;
    int stackOffset = 2 * stackGrowth;
    while (mbbi < mbb.size() && mbb.getInstAt(mbbi).getOpcode() == PUSH32r
        || mbb.getInstAt(mbbi).getOpcode() == PUSH64r) {
      pushedRegs = true;
      ++mbbi;
    }
    if (numBytes >= 4096 && subtarget.isTargetCygMing()) {
      boolean isEAXLive = false;
      for (Pair<Integer, Integer> intPair : mf.getMachineRegisterInfo().getLiveIns()) {
        int reg = intPair.first;
        isEAXLive = reg == EAX || reg == AX || reg == AH || reg == AL;
      }
      if (!isEAXLive) {
        buildMI(mbb, mbbi++, tii.get(MOV32ri), EAX).addImm(numBytes);
        buildMI(mbb, mbbi++, tii.get(CALLpcrel32))
            .addExternalSymbol("_alloca", 0, 0);
      } else {
        buildMI(mbb, mbbi++, tii.get(PUSH32r)).addReg(EAX, MachineOperand.RegState.Kill);
        buildMI(mbb, mbbi++, tii.get(MOV32ri), EAX).addImm(numBytes - 4);
        buildMI(mbb, mbbi++, tii.get(CALLpcrel32))
            .addExternalSymbol("_alloca", 0, 0);

        mi = addRegOffset(buildMI(tii.get(MOV32rm), EAX), stackPtr,
            false, numBytes - 4).getMInstr();
        mbb.insert(mbbi++, mi);
      }
    } else if (numBytes != 0) {
      OutRef<Integer> x = new OutRef<>(mbbi);
      numBytes -= mergeSPUpdates(mbb, x, stackPtr, true);
      mbbi = x.get();
      x = new OutRef<>(numBytes);
      mergeSPUpdatesDown(mbb, mbbi, stackPtr, x);
      numBytes = x.get();

      if (numBytes != 0)
        emitSPUpdate(mbb, mbbi, stackPtr, -numBytes, is64Bit, tii);
    }
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
      int mbbi,
      int stackPtr,
      OutRef<Integer> numBytes) {
    // FIXME, not run!
  }

  private void emitSPUpdate(MachineBasicBlock mbb,
                            int mbbi,
                            int stackPtr,
                            int numBytes,
                            boolean is64Bit,
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
    MachineFrameInfo mfi = mf.getFrameInfo();
    boolean is64Bit = subtarget.is64Bit();
    X86Subtarget subtarget = (X86Subtarget) mf.getTarget().getSubtarget();
    X86RegisterInfo tri = subtarget.getRegisterInfo();
    X86InstrInfo tii = subtarget.getInstrInfo();
    int stackPtr = tri.getStackRegister();
    int framePtr = tri.getFrameRegister(mf);
    X86MachineFunctionInfo x86FI = (X86MachineFunctionInfo) mf.getInfo();

    // get the position where epilogue code will inserts after.
    int mbbi = mbb.size() - 1;
    int retOpcode = mbb.getInstAt(mbbi).getOpcode();
    switch (retOpcode) {
      case RET:
      case RETI:
      case TCRETURNdi:
      case TCRETURNri:
      case TCRETURNdi64:
      case TCRETURNri64:
      case EH_RETURN:
      case EH_RETURN64:
        break;  //it is ok.
      default:
        Util.shouldNotReachHere("Can only insert epilog into returning block!");
    }

    int stackSize = mfi.getStackSize();
    int maxAlign = mf.getAlignment();
    int cssSize = x86FI.getCalleeSavedFrameSize();
    int numBytes = 0;
    int stackAlign = getStackAlignment();
    int slotSize = tri.getSlotSize();

    if (X86RegisterInfo.ForceStackAlign.value) {
      if (mfi.hasCalls())
        maxAlign = (stackAlign > maxAlign) ? stackAlign : maxAlign;
      else
        maxAlign = maxAlign != 0 ? maxAlign : 4;
    }

    if (hasFP(mf)) {
      // get the offset of the stack slot for the %ebp register.
      // which is guaranteed to be the last slot by processFunctionBeforeFrameFinalized().
      int frameSize = stackSize - slotSize;
      if (tri.needsStackRealignment(mf))
        frameSize = (frameSize + maxAlign - 1)/maxAlign * maxAlign;
      numBytes = frameSize - cssSize;

      // Pop EBP.
      buildMI(mbb, mbbi++, tii.get(is64Bit ? POP64r : POP32r), framePtr);
    }
    else {
      numBytes = stackSize - cssSize;
    }
    // Skip the callee-saved pop instructions.
    int lastCSPop = mbbi;
    while (mbbi != 0) {
      MachineInstr mi = mbb.getInstAt(--mbbi);
      int opc = mi.getOpcode();
      if (opc != POP32r && opc != POP64r && !mi.getDesc().isTerminator())
        break;
    }

    // If there is an ADD32ri or SUB32ri of ESP immediately before this
    // instruction, merge the two instructions.
    if (numBytes != 0 || mfi.hasVarSizedObjects()) {
      OutRef<Integer> ref = new OutRef<>(numBytes);
      mergeSPUpdatesUp(mbb, mbbi, stackPtr, ref);
      numBytes = ref.get();
    }

    // If dynamic alloca is used, then reset esp to point to the last callee-saved
    // slot before popping them off! Same applies for the case, when stack was
    // realigned.
    if (tri.needsStackRealignment(mf)) {
      if (cssSize != 0) {
        emitSPUpdate(mbb, mbbi, stackPtr, numBytes, is64Bit, tii);
        mbbi = lastCSPop-1;
      }

      buildMI(mbb, mbbi, tii.get(is64Bit ? MOV64rr : MOV32rr), stackPtr).addReg(framePtr);
    }
    else if (mfi.hasVarSizedObjects()) {
      if (cssSize != 0) {
        int opc = is64Bit ? LEA64r : LEA32r;
        MachineInstrBuilder mib = new MachineInstrBuilder(new MachineInstr(tii.get(opc)));
        mib.addReg(stackPtr);
        MachineInstr mi = addRegOffset(mib, framePtr, false, -cssSize).getMInstr();
        mbb.insert(mbbi, mi);
      }
      else {
        buildMI(mbb, mbbi, tii.get(is64Bit ? MOV64rr : MOV32rr), stackPtr).addReg(framePtr);
      }
    }
    else if (numBytes != 0) {
      // Adjust stack pointer back: ESP += numbytes.
      emitSPUpdate(mbb, mbbi, stackPtr, numBytes, is64Bit, tii);
    }

    // We're returning from function via eh_return.
    if (retOpcode == RET || retOpcode == RETI) {
      if (x86FI.getTCReturnAddrDelta() < 0) {
        // Add the return addr area delta back since we are not tail calling.
        int delta = -1*x86FI.getTCReturnAddrDelta();
        mbbi = mbb.size() - 1;
        OutRef<Integer> ref = new OutRef<>(mbbi);
        delta += mergeSPUpdates(mbb, ref, stackPtr, true);
        mbbi = ref.get();
        emitSPUpdate(mbb, mbbi, stackPtr, delta, is64Bit, tii);
      }
    }
    else
      Util.shouldNotReachHere("Unsupported return instruction!");
  }

  private static void mergeSPUpdatesUp(MachineBasicBlock mbb,
                                       int mbbi,
                                       int stackPtr,
                                       OutRef<Integer> numBytes) {
    if (mbbi == 0) return;

    MachineInstr mi = mbb.getInstAt(mbbi - 1);
    int opc = mi.getOpcode();
    if ((opc == ADD64ri32 || opc == ADD64ri8 ||
        opc == ADD32ri || opc == ADD32ri8) &&
        mi.getOperand(0).getReg() == stackPtr) {
      if (numBytes != null)
        numBytes.set((int) (numBytes.get() + mi.getOperand(2).getImm()));
      mbb.remove(mbbi);
    } else if ((opc == SUB64ri32 || opc == SUB64ri8 ||
        opc == SUB32ri || opc == SUB32ri8) &&
        mi.getOperand(0).getReg() == stackPtr) {
      if (numBytes != null)
        numBytes.set((int) (numBytes.get() - mi.getOperand(2).getImm()));
      mbb.remove(mbbi);
    }
  }

  /**
   * This returns true if frame pointer elimination optimization should be
   * turned off for the given machine function.
   * @param mf
   * @return
   */
  public static boolean disableFramePointerElim(MachineFunction mf) {
    if (DisableFPElim.value && !DisableFPEliMLeaf.value) {
      MachineFrameInfo mfi = mf.getFrameInfo();
      return mfi.hasCalls();
    }
    return DisableFPElim.value;
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
    TargetRegisterInfo tri = subtarget.getRegisterInfo();

    return (disableFramePointerElim(mf) || tri.needsStackRealignment(mf)
        || mfi.hasVarSizedObjects() || mfi.isFrameAddressTaken());
  }

  @Override
  public boolean hasReservedCallFrame(MachineFunction mf) {
    return !mf.getFrameInfo().hasVarSizedObjects();
  }
}
