package backend.target.arm;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import backend.codegen.*;
import backend.debug.DebugLoc;
import backend.mc.MCInstrDesc;
import backend.mc.MCRegisterClass;
import backend.support.LLVMContext;
import backend.target.TargetFrameLowering;
import backend.target.TargetRegisterInfo;
import backend.type.Type;
import backend.value.Constant;
import backend.value.ConstantInt;
import tools.BitMap;
import tools.OutRef;
import tools.Util;
import tools.commandline.BooleanOpt;
import tools.commandline.OptionHidden;
import tools.commandline.OptionHiddenApplicator;

import static backend.codegen.MachineInstrBuilder.*;
import static backend.target.TargetOptions.EnableRealignStack;
import static backend.target.arm.ARMGenRegisterNames.*;
import static tools.commandline.Desc.desc;
import static tools.commandline.Initializer.init;
import static tools.commandline.OptionNameApplicator.optionName;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public abstract class ARMRegisterInfo extends TargetRegisterInfo {

  private static final BooleanOpt ForceAllBaseRegAlloc = new BooleanOpt(
      optionName("arm-force-base-reg-alloc"),
      new OptionHiddenApplicator(OptionHidden.Hidden),
      init(false),
      desc("Force use of virtual base registers for stack load/store"));

  private static final BooleanOpt EnableLocalStackAlloc = new BooleanOpt(
      optionName("enable-local-stack-alloc"),
      init(true),
      new OptionHiddenApplicator(OptionHidden.Hidden),
      desc("Enable pre-regalloc stack frame index allocation"));

  private static final BooleanOpt EnableBasePointer = new BooleanOpt(
      optionName("arm-use-base-pointer"),
      new OptionHiddenApplicator(OptionHidden.Hidden),
      init(true),
      desc("Enable use of a base pointer for complex stack frames"));

  private int framePtr;
  private int basePtr;
  protected ARMSubtarget subtarget;
  private ARMFrameLowering tfl;

  protected ARMRegisterInfo(ARMSubtarget subtarget) {
    this.subtarget = subtarget;
    framePtr = subtarget.isTargetDarwin() || subtarget.isThumb() ?
        ARMGenRegisterNames.R7 : ARMGenRegisterNames.R11;
    basePtr = ARMGenRegisterNames.R6;
    tfl = subtarget.getFrameLowering();
  }

  public static TargetRegisterInfo createARMRegisterInfo(ARMSubtarget subtarget, int mode) {
    return new ARMGenRegisterInfo(subtarget, mode);
  }

  private static final int[] CalleeSavedRegs = {
      ARMGenRegisterNames.LR,
      ARMGenRegisterNames.R11, ARMGenRegisterNames.R10, ARMGenRegisterNames.R9, ARMGenRegisterNames.R8,
      ARMGenRegisterNames.R7, ARMGenRegisterNames.R6, ARMGenRegisterNames.R5, ARMGenRegisterNames.R4,

      ARMGenRegisterNames.D15, ARMGenRegisterNames.D14, ARMGenRegisterNames.D13, ARMGenRegisterNames.D12,
      ARMGenRegisterNames.D11, ARMGenRegisterNames.D10, ARMGenRegisterNames.D9, ARMGenRegisterNames.D8,
  };

  private static final int[] DarwinCalleeSavedRegs = {
      // Darwin ABI deviates from ARM standard ABI. R9 is not a callee-saved
      // register.
      ARMGenRegisterNames.LR,
      ARMGenRegisterNames.R7, ARMGenRegisterNames.R6, ARMGenRegisterNames.R5, ARMGenRegisterNames.R4,
      ARMGenRegisterNames.R11, ARMGenRegisterNames.R10, ARMGenRegisterNames.R8,

      ARMGenRegisterNames.D15, ARMGenRegisterNames.D14, ARMGenRegisterNames.D13, ARMGenRegisterNames.D12,
      ARMGenRegisterNames.D11, ARMGenRegisterNames.D10, ARMGenRegisterNames.D9, ARMGenRegisterNames.D8,
  };

  @Override
  public int[] getCalleeSavedRegs(MachineFunction mf) {
    return subtarget.isTargetDarwin() ? DarwinCalleeSavedRegs : CalleeSavedRegs;
  }

  private final MCRegisterClass[] CalleeSavedRegClasses = {
      ARMGenRegisterInfo.GPRRegisterClass, ARMGenRegisterInfo.GPRRegisterClass, ARMGenRegisterInfo.GPRRegisterClass,
      ARMGenRegisterInfo.GPRRegisterClass, ARMGenRegisterInfo.GPRRegisterClass, ARMGenRegisterInfo.GPRRegisterClass,
      ARMGenRegisterInfo.GPRRegisterClass, ARMGenRegisterInfo.GPRRegisterClass, ARMGenRegisterInfo.GPRRegisterClass,

      ARMGenRegisterInfo.DPRRegisterClass, ARMGenRegisterInfo.DPRRegisterClass, ARMGenRegisterInfo.DPRRegisterClass,
      ARMGenRegisterInfo.DPRRegisterClass, ARMGenRegisterInfo.DPRRegisterClass, ARMGenRegisterInfo.DPRRegisterClass,
      ARMGenRegisterInfo.DPRRegisterClass, ARMGenRegisterInfo.DPRRegisterClass
  };

  private final MCRegisterClass[] ThumbCalleeSavedRegClasses = {
      ARMGenRegisterInfo.GPRRegisterClass, ARMGenRegisterInfo.GPRRegisterClass, ARMGenRegisterInfo.GPRRegisterClass,
      ARMGenRegisterInfo.GPRRegisterClass, ARMGenRegisterInfo.GPRRegisterClass, ARMGenRegisterInfo.tGPRRegisterClass,
      ARMGenRegisterInfo.tGPRRegisterClass, ARMGenRegisterInfo.tGPRRegisterClass, ARMGenRegisterInfo.tGPRRegisterClass,

      ARMGenRegisterInfo.DPRRegisterClass, ARMGenRegisterInfo.DPRRegisterClass, ARMGenRegisterInfo.DPRRegisterClass,
      ARMGenRegisterInfo.DPRRegisterClass,
      ARMGenRegisterInfo.DPRRegisterClass, ARMGenRegisterInfo.DPRRegisterClass, ARMGenRegisterInfo.DPRRegisterClass,
      ARMGenRegisterInfo.DPRRegisterClass
  };

  private final MCRegisterClass[] DarwinCalleeSavedRegClasses = {
      ARMGenRegisterInfo.GPRRegisterClass, ARMGenRegisterInfo.GPRRegisterClass, ARMGenRegisterInfo.GPRRegisterClass,
      ARMGenRegisterInfo.GPRRegisterClass, ARMGenRegisterInfo.GPRRegisterClass, ARMGenRegisterInfo.GPRRegisterClass,
      ARMGenRegisterInfo.GPRRegisterClass, ARMGenRegisterInfo.GPRRegisterClass,

      ARMGenRegisterInfo.DPRRegisterClass, ARMGenRegisterInfo.DPRRegisterClass, ARMGenRegisterInfo.DPRRegisterClass,
      ARMGenRegisterInfo.DPRRegisterClass, ARMGenRegisterInfo.DPRRegisterClass, ARMGenRegisterInfo.DPRRegisterClass,
      ARMGenRegisterInfo.DPRRegisterClass, ARMGenRegisterInfo.DPRRegisterClass
  };

  private final MCRegisterClass[] DarwinThumbCalleeSavedRegClasses = {
      ARMGenRegisterInfo.GPRRegisterClass, ARMGenRegisterInfo.tGPRRegisterClass, ARMGenRegisterInfo.tGPRRegisterClass,
      ARMGenRegisterInfo.tGPRRegisterClass, ARMGenRegisterInfo.tGPRRegisterClass, ARMGenRegisterInfo.GPRRegisterClass,
      ARMGenRegisterInfo.GPRRegisterClass, ARMGenRegisterInfo.GPRRegisterClass,

      ARMGenRegisterInfo.DPRRegisterClass, ARMGenRegisterInfo.DPRRegisterClass, ARMGenRegisterInfo.DPRRegisterClass,
      ARMGenRegisterInfo.DPRRegisterClass, ARMGenRegisterInfo.DPRRegisterClass, ARMGenRegisterInfo.DPRRegisterClass,
      ARMGenRegisterInfo.DPRRegisterClass, ARMGenRegisterInfo.DPRRegisterClass
  };

  @Override
  public MCRegisterClass[] getCalleeSavedRegClasses(MachineFunction mf) {
    if (subtarget.isThumb1Only()) {
      return subtarget.isTargetDarwin()
          ? DarwinThumbCalleeSavedRegClasses : ThumbCalleeSavedRegClasses;
    }
    return subtarget.isTargetDarwin() ? DarwinCalleeSavedRegClasses : CalleeSavedRegClasses;
  }

  @Override
  public BitMap getReservedRegs(MachineFunction mf) {
    BitMap res = new BitMap(getNumRegs());
    res.set(ARMGenRegisterNames.SP);
    res.set(ARMGenRegisterNames.PC);
    if (subtarget.isTargetDarwin() || tfl.hasFP(mf))
      res.set(framePtr);

    // Some target reserves R9.
    if (subtarget.isR9Reserved())
      res.set(ARMGenRegisterNames.R9);

    return res;
  }

  public boolean isReservedReg(MachineFunction mf, int reg) {
    switch (reg) {
      default:
        break;
      case ARMGenRegisterNames.SP:
      case ARMGenRegisterNames.PC:
        return true;
      case ARMGenRegisterNames.R7:
      case ARMGenRegisterNames.R11:
        if (framePtr == reg && (subtarget.isTargetDarwin() || tfl.hasFP(mf)))
          return true;
      case ARMGenRegisterNames.R9:
        return subtarget.isR9Reserved();
    }
    return false;
  }

  @Override
  public void eliminateFrameIndex(MachineFunction mf, int spAdj, MachineInstr mi, RegScavenger rs) {
    MachineBasicBlock mbb = mi.getParent();
    MachineFrameInfo mfi = mf.getFrameInfo();
    ARMFrameLowering tfi = subtarget.getFrameLowering();
    ARMFunctionInfo afi = (ARMFunctionInfo) mf.getInfo();
    Util.assertion(!afi.isThumb1OnlyFunction(), "This eliminateFrameIndex doesn't support Thumb1!");

    int i = 0;
    while (!mi.getOperand(i).isFrameIndex()) {
      ++i;
      Util.assertion(i < mi.getNumOperands(), "Instruction doesn't have a FrameIndex!");
    }

    int frameIndex = mi.getOperand(i).getIndex();
    OutRef<Integer> frameRegRef = new OutRef<>(0);
    int offset = tfi.resolveFrameIndexReference(mf, frameIndex, frameRegRef, spAdj);
    int frameReg = frameRegRef.get();

    // Modify MI as necessary to handle as much of 'Offset' as possible
    if (afi.isGPRCalleeSavedArea1Frame(frameIndex))
      offset -= afi.getGPRCalleeSavedArea1Offset();
    else if (afi.isGPRCalleeSavedArea2Frame(frameIndex))
      offset -= afi.getGPRCalleeSavedArea2Offset();
    else if (afi.isDPRCalleeSavedAreaFrame(frameIndex))
      offset -= afi.getDPRCalleeSavedAreaOffset();
    else if (tfi.hasFP(mf) && afi.hasStackFrame()) {
      Util.assertion(spAdj == 0, "Unexpected stack offset");
      frameReg = getFrameRegister(mf);
      offset -= afi.getFramePtrSpillOffset();
    }

    // modify MI as necessary to handle as much of 'Offset' as possible
    boolean done;
    if (!afi.isThumbFunction()) {
      OutRef<Integer> ref = new OutRef<>(offset);
      done = rewriteARMFrameIndex(mi, i, frameReg, ref, subtarget.getInstrInfo());
      offset = ref.get();
    } else {
      Util.assertion(afi.isThumb2Function());
      OutRef<Integer> ref = new OutRef<>(offset);
      done = rewriteT2FrameIndex(mi, i, frameReg, ref, subtarget.getInstrInfo());
      offset = ref.get();
    }

    if (done)
      return;

    int tsFlags = mi.getDesc().tSFlags;
    Util.assertion(offset != 0 || ((tsFlags & ARMII.AddrModeMask) == ARMII.AddrMode.AddrMode4.ordinal() ||
            (tsFlags & ARMII.AddrModeMask) == ARMII.AddrMode.AddrMode6.ordinal()),
        "This code isn't needed if offset already handled!");

    int scratchReg;
    int pIdx = mi.findFirstPredOperandIdx();
    ARMCC.CondCodes pred = pIdx == -1 ? ARMCC.CondCodes.AL : ARMCC.CondCodes.values()[(int) mi.getOperand(pIdx).getImm()];
    int predReg = pIdx == -1 ? 0 : mi.getOperand(pIdx + 1).getReg();
    if (offset == 0)
      // must be addrmode 4/6
      mi.getOperand(i).changeToRegister(frameReg, false, false, false, false, false);
    else {
      scratchReg = mf.getMachineRegisterInfo().createVirtualRegister(ARMGenRegisterInfo.GPRRegisterClass);
      if (!afi.isThumbFunction())
        emitARMRegPlusImmediate(mbb, mbb.getIndexOf(mi), mi.getDebugLoc(), scratchReg, frameReg, offset, pred, predReg, subtarget.getInstrInfo());
      else {
        Util.assertion(afi.isThumb2Function());
        emitT2RegPlusImmediate(mbb, mbb.getIndexOf(mi), mi.getDebugLoc(), scratchReg, frameReg, offset, pred, predReg, subtarget.getInstrInfo());
      }
      // Update the original instruction to use the scratch register.
      mi.getOperand(i).changeToRegister(scratchReg, false, false, true, false, false);
    }
  }

  private boolean rewriteT2FrameIndex(MachineInstr mi,
                                      int frameRegIdx,
                                      int frameReg,
                                      OutRef<Integer> offset,
                                      ARMInstrInfo tii) {
    int opcode = mi.getOpcode();
    MCInstrDesc mid = mi.getDesc();
    ARMII.AddrMode addrMode = ARMII.AddrMode.values()[mid.tSFlags & ARMII.AddrModeMask];
    boolean isSub = false;

    if (opcode == ARMGenInstrNames.INLINEASM)
      addrMode = ARMII.AddrMode.AddrModeT2_i12;

    if (opcode == ARMGenInstrNames.t2ADDri || opcode == ARMGenInstrNames.t2ADDri12) {
      offset.set((int) (offset.get() + mi.getOperand(frameRegIdx + 1).getImm()));

      OutRef<Integer> predReg = new OutRef<>();
      if (offset.get() == 0 && getInstrPredicate(mi, predReg) == ARMCC.CondCodes.AL) {
        // turn it into a move.
        mi.setDesc(tii.get(ARMGenInstrNames.tMOVr));
        mi.getOperand(frameRegIdx).changeToRegister(frameReg, false);
        // Remove offset and remaining explicit predicate operands.
        do {
          mi.removeOperand(frameRegIdx + 1);
        } while (mi.getNumOperands() > frameRegIdx + 1);
        addDefaultPred(new MachineInstrBuilder(mi));
        return true;
      }

      boolean hasCCOut = opcode != ARMGenInstrNames.t2ADDri12;
      if (offset.get() < 0) {
        offset.set(-offset.get());
        isSub = true;
        mi.setDesc(tii.get(ARMGenInstrNames.t2SUBri));
      } else {
        mi.setDesc(tii.get(ARMGenInstrNames.t2ADDri));
      }

      // Common case: small offset, fits into instruction.
      if (ARM_AM.getT2SOImmVal(offset.get()) != -1) {
        mi.getOperand(frameRegIdx).changeToRegister(frameReg, false);
        mi.getOperand(frameRegIdx + 1).changeToImmediate(offset.get());
        if (!hasCCOut)
          mi.addOperand(MachineOperand.createReg(0, false, false));
        offset.set(0);
        return true;
      }

      // Another common case: imm12.
      if (offset.get() < 4096 && (!hasCCOut || mi.getOperand(mi.getNumOperands() - 1).getReg() == 0)) {
        int newOpc = isSub ? ARMGenInstrNames.t2SUBri12 : ARMGenInstrNames.t2ADDri12;
        mi.setDesc(tii.get(newOpc));
        mi.getOperand(frameRegIdx).changeToRegister(frameReg, false);
        mi.getOperand(frameRegIdx + 1).changeToImmediate(offset.get());
        // remove cc_out operand.
        if (hasCCOut)
          mi.removeOperand(mi.getNumOperands() - 1);
        offset.set(0);
        return true;
      }

      // Otherwise, extract 8 adjacent bits from the immediate into this
      // t2ADDri/t2SUBri.
      int rotAmt = Util.countLeadingZeros32(offset.get());
      int thisImmVal = offset.get() & ARM_AM.rotr32(0xff000000, rotAmt);

      // we will cope with there bits from offset, clear them.
      offset.set(offset.get() & ~thisImmVal);
      Util.assertion(ARM_AM.getT2SOImmVal(thisImmVal) != -1, "bit extract didn't work?");
      mi.getOperand(frameRegIdx + 1).changeToImmediate(thisImmVal);

      // add cc_out operand if the original instruction doesn't have it.
      if (!hasCCOut)
        mi.addOperand(MachineOperand.createReg(0, false, false));
    } else {
      // addrMode4 and addmode6 can't handle any offset.
      if (addrMode == ARMII.AddrMode.AddrMode4 || addrMode == ARMII.AddrMode.AddrMode6)
        return false;

      // AddrModeT2_so cannot handle any offset. If there is no offset
      // register then we change to an immediate version.
      int newOpc = opcode;
      if (addrMode == ARMII.AddrMode.AddrModeT2_so) {
        int offsetReg = mi.getOperand(frameRegIdx + 1).getReg();
        if (offsetReg != 0) {
          mi.getOperand(frameRegIdx).changeToRegister(frameReg, false);
          return offset.get() == 0;
        }

        mi.removeOperand(frameRegIdx + 1);
        mi.getOperand(frameRegIdx + 1).changeToImmediate(0);
        newOpc = immediateOffsetOpcode(opcode);
        addrMode = ARMII.AddrMode.AddrModeT2_i12;
      }
      int numBits = 0;
      int scale = 1;
      if (addrMode == ARMII.AddrMode.AddrModeT2_i8 || addrMode == ARMII.AddrMode.AddrModeT2_i12) {
        // i8 supports only negative, and i12 supports only positive, so
        // based on Offset sign convert Opcode to the appropriate
        // instruction
        offset.set((int) (offset.get() + mi.getOperand(frameRegIdx + 1).getImm()));
        if (offset.get() < 0) {
          newOpc = negativeOffsetOpcode(opcode);
          numBits = 8;
          isSub = true;
          offset.set(-offset.get());
        } else {
          newOpc = positiveOffsetOpcode(opcode);
          numBits = 12;
        }
      } else if (addrMode == ARMII.AddrMode.AddrMode5) {
        // VFP address mode.
        MachineOperand offMO = mi.getOperand(frameRegIdx + 1);
        int instrOffset = ARM_AM.getAM5Offset((int) offMO.getImm());
        if (ARM_AM.getAM5Op((int) offMO.getImm()) == ARM_AM.AddrOpc.sub)
          instrOffset *= -1;

        numBits = 8;
        scale = 4;
        offset.set(offset.get() + instrOffset * 4);
        Util.assertion((offset.get() & (scale - 1)) == 0, "can't encode this offset?");
        if (offset.get() < 0) {
          offset.set(-offset.get());
          isSub = true;
        }
      } else
        Util.shouldNotReachHere("unsupported addressing mode");

      if (newOpc != opcode)
        mi.setDesc(tii.get(newOpc));

      MachineOperand immOp = mi.getOperand(frameRegIdx + 1);
      // Attempt to fold address computation
      // Common case: small offset, fits into instruction.
      int immOffset = offset.get() / scale;
      int mask = (1 << numBits) - 1;
      if (offset.get() <= mask * scale) {
        // Replace the FrameIndex with fp/sp
        mi.getOperand(frameRegIdx).changeToRegister(frameReg, false);
        if (isSub) {
          if (addrMode == ARMII.AddrMode.AddrMode5)
            immOffset |= 1 << numBits;
          else
            immOffset = -immOffset;
        }

        immOp.changeToImmediate(immOffset);
        offset.set(0);
        return true;
      }

      // Otherwise, offset doesn't fit. Pull in what we can to simplify
      immOffset = immOffset & mask;
      if (isSub) {
        if (addrMode == ARMII.AddrMode.AddrMode5)
          immOffset |= 1 << numBits;
        else {
          immOffset = -immOffset;
          if (immOffset == 0)
            mi.setDesc(tii.get(positiveOffsetOpcode(newOpc)));
        }
      }

      immOp.changeToImmediate(immOffset);
      offset.set(offset.get() & ~(mask * scale));
    }
    if (isSub)
      offset.set(-offset.get());
    return offset.get() == 0;
  }


  private static int negativeOffsetOpcode(int opcode) {
    switch (opcode) {
      case ARMGenInstrNames.t2LDRi12:
        return ARMGenInstrNames.t2LDRi8;
      case ARMGenInstrNames.t2LDRHi12:
        return ARMGenInstrNames.t2LDRHi8;
      case ARMGenInstrNames.t2LDRBi12:
        return ARMGenInstrNames.t2LDRBi8;
      case ARMGenInstrNames.t2LDRSHi12:
        return ARMGenInstrNames.t2LDRSHi8;
      case ARMGenInstrNames.t2LDRSBi12:
        return ARMGenInstrNames.t2LDRSBi8;
      case ARMGenInstrNames.t2STRi12:
        return ARMGenInstrNames.t2STRi8;
      case ARMGenInstrNames.t2STRBi12:
        return ARMGenInstrNames.t2STRBi8;
      case ARMGenInstrNames.t2STRHi12:
        return ARMGenInstrNames.t2STRHi8;

      case ARMGenInstrNames.t2LDRi8:
      case ARMGenInstrNames.t2LDRHi8:
      case ARMGenInstrNames.t2LDRBi8:
      case ARMGenInstrNames.t2LDRSHi8:
      case ARMGenInstrNames.t2LDRSBi8:
      case ARMGenInstrNames.t2STRi8:
      case ARMGenInstrNames.t2STRBi8:
      case ARMGenInstrNames.t2STRHi8:
        return opcode;

      default:
        break;
    }

    return 0;
  }

  private static int positiveOffsetOpcode(int opcode) {
    switch (opcode) {
      case ARMGenInstrNames.t2LDRi8:
        return ARMGenInstrNames.t2LDRi12;
      case ARMGenInstrNames.t2LDRHi8:
        return ARMGenInstrNames.t2LDRHi12;
      case ARMGenInstrNames.t2LDRBi8:
        return ARMGenInstrNames.t2LDRBi12;
      case ARMGenInstrNames.t2LDRSHi8:
        return ARMGenInstrNames.t2LDRSHi12;
      case ARMGenInstrNames.t2LDRSBi8:
        return ARMGenInstrNames.t2LDRSBi12;
      case ARMGenInstrNames.t2STRi8:
        return ARMGenInstrNames.t2STRi12;
      case ARMGenInstrNames.t2STRBi8:
        return ARMGenInstrNames.t2STRBi12;
      case ARMGenInstrNames.t2STRHi8:
        return ARMGenInstrNames.t2STRHi12;

      case ARMGenInstrNames.t2LDRi12:
      case ARMGenInstrNames.t2LDRHi12:
      case ARMGenInstrNames.t2LDRBi12:
      case ARMGenInstrNames.t2LDRSHi12:
      case ARMGenInstrNames.t2LDRSBi12:
      case ARMGenInstrNames.t2STRi12:
      case ARMGenInstrNames.t2STRBi12:
      case ARMGenInstrNames.t2STRHi12:
        return opcode;

      default:
        break;
    }

    return 0;
  }

  private static int immediateOffsetOpcode(int opcode) {
    switch (opcode) {
      case ARMGenInstrNames.t2LDRs:
        return ARMGenInstrNames.t2LDRi12;
      case ARMGenInstrNames.t2LDRHs:
        return ARMGenInstrNames.t2LDRHi12;
      case ARMGenInstrNames.t2LDRBs:
        return ARMGenInstrNames.t2LDRBi12;
      case ARMGenInstrNames.t2LDRSHs:
        return ARMGenInstrNames.t2LDRSHi12;
      case ARMGenInstrNames.t2LDRSBs:
        return ARMGenInstrNames.t2LDRSBi12;
      case ARMGenInstrNames.t2STRs:
        return ARMGenInstrNames.t2STRi12;
      case ARMGenInstrNames.t2STRBs:
        return ARMGenInstrNames.t2STRBi12;
      case ARMGenInstrNames.t2STRHs:
        return ARMGenInstrNames.t2STRHi12;

      case ARMGenInstrNames.t2LDRi12:
      case ARMGenInstrNames.t2LDRHi12:
      case ARMGenInstrNames.t2LDRBi12:
      case ARMGenInstrNames.t2LDRSHi12:
      case ARMGenInstrNames.t2LDRSBi12:
      case ARMGenInstrNames.t2STRi12:
      case ARMGenInstrNames.t2STRBi12:
      case ARMGenInstrNames.t2STRHi12:
      case ARMGenInstrNames.t2LDRi8:
      case ARMGenInstrNames.t2LDRHi8:
      case ARMGenInstrNames.t2LDRBi8:
      case ARMGenInstrNames.t2LDRSHi8:
      case ARMGenInstrNames.t2LDRSBi8:
      case ARMGenInstrNames.t2STRi8:
      case ARMGenInstrNames.t2STRBi8:
      case ARMGenInstrNames.t2STRHi8:
        return opcode;

      default:
        break;
    }

    return 0;
  }

  public ARMCC.CondCodes getInstrPredicate(MachineInstr mi, OutRef<Integer> predReg) {
    int idx = mi.findFirstPredOperandIdx();
    if (idx == -1) {
      predReg.set(0);
      return ARMCC.CondCodes.AL;
    }

    predReg.set(mi.getOperand(idx + 1).getReg());
    return ARMCC.CondCodes.values()[(int) mi.getOperand(idx).getImm()];
  }

  private boolean rewriteARMFrameIndex(MachineInstr mi,
                                       int frameRegIdx,
                                       int frameReg,
                                       OutRef<Integer> offset,
                                       ARMInstrInfo tii) {
    int opcode = mi.getOpcode();
    MCInstrDesc mid = mi.getDesc();
    ARMII.AddrMode addrMode = ARMII.AddrMode.values()[mid.tSFlags & ARMII.AddrModeMask];
    boolean isSub = false;

    // Memory operands in inline assembly always use AddrMode2.
    if (opcode == ARMGenInstrNames.INLINEASM)
      addrMode = ARMII.AddrMode.AddrMode2;

    if (opcode == ARMGenInstrNames.ADDri) {
      offset.set((int) (offset.get() + mi.getOperand(frameRegIdx + 1).getImm()));
      if (offset.get() == 0) {
        // turn it into a move.
        mi.setDesc(tii.get(ARMGenInstrNames.MOVr));
        mi.getOperand(frameRegIdx).changeToRegister(frameReg, false);
        mi.removeOperand(frameRegIdx + 1);
        offset.set(0);
        return true;
      } else if (offset.get() < 0) {
        offset.set(-offset.get());
        isSub = true;
        mi.setDesc(tii.get(ARMGenInstrNames.SUBri));
      }

      // Common case: small offset, fits into instruction.
      if (ARM_AM.getSOImmVal(offset.get()) != -1) {
        // Replace the FrameIndex with sp / fp
        mi.getOperand(frameRegIdx).changeToRegister(frameReg, false);
        mi.getOperand(frameRegIdx + 1).changeToImmediate(offset.get());
        offset.set(0);
        return true;
      }

      // Otherwise, pull as much of the immedidate into this ADDri/SUBri
      // as possible.
      int rotAmt = ARM_AM.getSOImmValRotate(offset.get());
      int thisImmVal = offset.get() & ARM_AM.rotr32(0xff, rotAmt);

      offset.set(offset.get() & ~thisImmVal);
      Util.assertion(ARM_AM.getSOImmVal(thisImmVal) != -1, "bit extract didn't work?");
      mi.getOperand(frameRegIdx + 1).changeToImmediate(thisImmVal);
    } else {
      int immIdx = 0;
      int instrOffset = 0;
      int numBits = 0;
      int scale = 1;
      switch (addrMode) {
        case AddrMode_i12: {
          immIdx = frameRegIdx + 1;
          instrOffset = (int) mi.getOperand(immIdx).getImm();
          numBits = 12;
          break;
        }
        case AddrMode2: {
          immIdx = frameRegIdx + 2;
          instrOffset = ARM_AM.getAM2Offset((int) mi.getOperand(immIdx).getImm());
          if (ARM_AM.getAM2Op((int) mi.getOperand(immIdx).getImm()) == ARM_AM.AddrOpc.sub)
            instrOffset *= -1;
          numBits = 8;
          break;
        }
        case AddrMode4:
        case AddrMode6:
          // can't fold any offset even if it's zero.
          return false;
        case AddrMode5: {
          immIdx = frameRegIdx + 1;
          instrOffset = ARM_AM.getAM5Offset((int) mi.getOperand(immIdx).getImm());
          if (ARM_AM.getAM5Op((int) mi.getOperand(immIdx).getImm()) == ARM_AM.AddrOpc.sub)
            instrOffset *= -1;
          numBits = 8;
          scale = 4;
          break;
        }
        default:
          Util.shouldNotReachHere("Unsupported addressing mode");
          break;
      }

      offset.set(offset.get() + instrOffset * scale);
      Util.assertion((offset.get() & (scale - 1)) == 0, "can't encode this offset!");
      if (offset.get() < 0) {
        offset.set(-offset.get());
        isSub = true;
      }

      // Attempt to fold address comp. if opcode has offset bits
      if (numBits > 0) {
        // Common case: small offset, fits into instruction.
        MachineOperand immOp = mi.getOperand(immIdx);
        int immOffset = offset.get() / scale;
        int mask = (1 << numBits) - 1;
        if (offset.get() <= mask * scale) {
          // Replace the FrameIndex with sp
          mi.getOperand(frameRegIdx).changeToRegister(frameReg, false);
          if (isSub) {
            if (addrMode == ARMII.AddrMode.AddrMode_i12)
              immOffset = -immOffset;
            else
              immOffset |= 1 << numBits;
          }
          immOp.changeToImmediate(immOffset);
          offset.set(0);
          return true;
        }

        // Otherwise, it didn't fit. Pull in what we can to simplify the immed.
        immOffset = immOffset & mask;
        if (isSub) {
          if (addrMode == ARMII.AddrMode.AddrMode_i12)
            immOffset = -immOffset;
          else
            immOffset |= 1 << numBits;
        }
        immOp.changeToImmediate(immOffset);
        offset.set(offset.get() & (~(mask * scale)));
      }
    }
    if (isSub)
      offset.set(-offset.get());
    return offset.get() == 0;
  }

  static int emitT2RegPlusImmediate(MachineBasicBlock mbb,
                                    int mi,
                                    DebugLoc dl,
                                    int destReg,
                                    int baseReg,
                                    int numBytes,
                                    ARMCC.CondCodes pred,
                                    int predReg,
                                    ARMInstrInfo tii) {
    return emitT2RegPlusImmediate(mbb, mi, dl, destReg, baseReg, numBytes, pred, predReg, tii, 0);
  }

  static int emitT2RegPlusImmediate(MachineBasicBlock mbb,
                                    int mi,
                                    DebugLoc dl,
                                    int destReg,
                                    int baseReg,
                                    int numBytes,
                                    ARMCC.CondCodes pred,
                                    int predReg,
                                    ARMInstrInfo tii,
                                    int miFlags) {
    boolean isSub = numBytes < 0;
    if (isSub)
      numBytes = -numBytes;

    // If profitable, use a movw or movt to materialize the offset.
    if (destReg != ARMGenRegisterNames.SP && destReg != baseReg &&
        numBytes >= 4096 && ARM_AM.getT2SOImmVal(numBytes) == -1) {
      boolean fits = false;
      if (numBytes < 65536) {
        // Use a movw to materialize the 16-bit constant.
        buildMI(mbb, mi++, dl, tii.get(ARMGenInstrNames.t2MOVi16), destReg)
            .addImm(numBytes)
            .addImm(pred.ordinal())
            .addReg(predReg)
            .setMIFlags(miFlags);
        fits = true;
      } else if ((numBytes & 0xffff) == 0) {
        // Use a movt to materialize the 32-bit constant.
        buildMI(mbb, mi++, dl, tii.get(ARMGenInstrNames.t2MOVTi16), destReg)
            .addReg(destReg)
            .addImm(numBytes >>> 16)
            .addImm(pred.ordinal())
            .addReg(predReg)
            .setMIFlags(miFlags);
        fits = true;
      }

      if (fits) {
        if (isSub) {
          buildMI(mbb, mi, dl, tii.get(ARMGenInstrNames.t2SUBrr), destReg)
              .addReg(baseReg, MachineOperand.RegState.Kill)
              .addReg(destReg, MachineOperand.RegState.Kill)
              .addImm(pred.ordinal())
              .addReg(predReg)
              .addReg(0).setMIFlags(miFlags);
        } else {
          buildMI(mbb, mi, dl, tii.get(ARMGenInstrNames.t2ADDrr), destReg)
              .addReg(baseReg, MachineOperand.RegState.Kill)
              .addReg(destReg, MachineOperand.RegState.Kill)
              .addImm(pred.ordinal())
              .addReg(predReg)
              .addReg(0).setMIFlags(miFlags);
        }
        return mi;
      }
    }

    while (numBytes != 0) {
      int thisVal = numBytes;
      int opc = 0;
      if (destReg == ARMGenRegisterNames.SP && baseReg != ARMGenRegisterNames.SP) {
        addDefaultPred(buildMI(mbb, mi++, dl, tii.get(ARMGenInstrNames.tMOVr), destReg).addReg(baseReg).setMIFlags(miFlags));
        baseReg = ARMGenRegisterNames.SP;
        continue;
      }

      boolean hasCCOut = true;
      if (baseReg == ARMGenRegisterNames.SP) {
        // sub sp, sp, #imm7
        if (destReg == ARMGenRegisterNames.SP && (thisVal < ((1 << 7) - 1) * 4)) {
          Util.assertion((thisVal & 0x3) == 0, "stack update is not multiple of 4?");
          addDefaultPred(buildMI(mbb, mi++, dl, tii.get(opc), destReg).addReg(baseReg).addImm(thisVal / 4).setMIFlags(miFlags));
          numBytes = 0;
          continue;
        }

        // sub rd, sp, so_imm
        opc = isSub ? ARMGenInstrNames.t2SUBri : ARMGenInstrNames.t2ADDri;
        if (ARM_AM.getT2SOImmVal(numBytes) != -1) {
          numBytes = 0;
        } else {
          int rotAmt = Util.countLeadingZeros32(thisVal);
          thisVal = thisVal & ARM_AM.rotr32(0xff000000, rotAmt);
          numBytes &= ~thisVal;
          Util.assertion(ARM_AM.getT2SOImmVal(thisVal) != -1, "bit extract didn't work?");
        }
      } else {
        opc = isSub ? ARMGenInstrNames.t2SUBri : ARMGenInstrNames.t2ADDri;
        if (ARM_AM.getT2SOImmVal(numBytes) != -1) {
          numBytes = 0;
        } else if (thisVal < 4096) {
          opc = isSub ? ARMGenInstrNames.t2SUBri12 : ARMGenInstrNames.t2ADDri12;
          hasCCOut = false;
          numBytes = 0;
        } else {
          int rotAmt = Util.countLeadingZeros32(thisVal);
          thisVal = thisVal & ARM_AM.rotr32(0xff000000, rotAmt);
          numBytes &= ~thisVal;
          Util.assertion(ARM_AM.getT2SOImmVal(thisVal) != -1, "bit extract didn't work");
        }
      }

      // build a new add/sub
      MachineInstrBuilder mib = addDefaultPred(buildMI(mbb, mi++, dl, tii.get(opc), destReg)
          .addReg(baseReg, MachineOperand.RegState.Kill)
          .addImm(thisVal).setMIFlags(miFlags));
      if (hasCCOut)
        addDefaultCC(mib);

      baseReg = destReg;
    }
    return mi;
  }

  static MachineInstrBuilder addDefaultCC(MachineInstrBuilder mib) {
    return mib.addReg(0);
  }

  static MachineInstrBuilder addDefaultT1CC(MachineInstrBuilder mib) {
    return addDefaultT1CC(mib, false);
  }

  static MachineInstrBuilder addDefaultT1CC(MachineInstrBuilder mib, boolean isDead) {
    return mib.addReg(ARMGenRegisterNames.CPSR, getDefRegState(true) | getDeadRegState(isDead));
  }

  static MachineInstrBuilder addDefaultPred(MachineInstrBuilder mib) {
    return mib.addImm(ARMCC.CondCodes.AL.ordinal()).addReg(0);
  }

  static int emitARMRegPlusImmediate(MachineBasicBlock mbb,
                                     int mi,
                                     DebugLoc dl,
                                     int destReg,
                                     int baseReg,
                                     int numBytes,
                                     ARMCC.CondCodes cc,
                                     int predReg,
                                     ARMInstrInfo tii) {
    return emitARMRegPlusImmediate(mbb, mi, dl, destReg, baseReg, numBytes, cc, predReg, tii, 0);
  }

  private static int emitARMRegPlusImmediate(MachineBasicBlock mbb,
                                             int mi,
                                             DebugLoc dl,
                                             int destReg,
                                             int baseReg,
                                             int numBytes,
                                             ARMCC.CondCodes pred,
                                             int predReg,
                                             ARMInstrInfo tii,
                                             int miFlags) {
    boolean isSub = numBytes < 0;
    if (isSub) numBytes = -numBytes;

    while (numBytes != 0) {
      int rotAmt = ARM_AM.getSOImmValRotate(numBytes);
      int thisVal = numBytes & ARM_AM.rotr32(0xff, rotAmt);
      Util.assertion(thisVal != 0, "Didn't extract field correctly?");

      numBytes &= ~thisVal;
      Util.assertion(ARM_AM.getSOImmVal(thisVal) != -1, "bit extract didn't work?");
      // build the new ADD/SUB.
      int opc = isSub ? ARMGenInstrNames.SUBri : ARMGenInstrNames.ADDri;
      buildMI(mbb, mi++, dl, tii.get(opc), destReg)
          .addReg(baseReg, MachineOperand.RegState.Kill)
          .addImm(thisVal)
          .addImm(pred.ordinal())
          .addReg(predReg)
          .addReg(0)
          .setMIFlags(miFlags);
      baseReg = destReg;
    }
    return mi;
  }

  static int emitThumbRegPlusImmediate(MachineBasicBlock mbb,
                                       int mi,
                                       DebugLoc dl,
                                       int destReg,
                                       int baseReg,
                                       int numBytes,
                                       ARMInstrInfo tii,
                                       ARMRegisterInfo tri) {
    return emitThumbRegPlusImmediate(mbb, mi, dl, destReg, baseReg, numBytes, tii, tri, 0);
  }

  /**
   * Emits a series of instructions to materialize a destreg = basereg + immediate in Thumb code.
   *
   * @param mbb
   * @param mi
   * @param dl
   * @param destReg
   * @param baseReg
   * @param numBytes*
   * @param tii
   * @param tri
   * @param miFlags
   * @return
   */
  static int emitThumbRegPlusImmediate(MachineBasicBlock mbb,
                                       int mi,
                                       DebugLoc dl,
                                       int destReg,
                                       int baseReg,
                                       int numBytes,
                                       ARMInstrInfo tii,
                                       ARMRegisterInfo tri,
                                       int miFlags) {
    boolean isSub = numBytes < 0;
    if (isSub)
      numBytes = -numBytes;
    boolean isMul4 = (numBytes & 3) == 0;
    boolean isTwoAddr = false;
    boolean dstNotEqBase = false;
    int numBits = 1;
    int scale = 1;
    int opc = 0;
    int extraOpc = 0;
    boolean needCC = false;

    if (destReg == baseReg && baseReg == ARMGenRegisterNames.SP) {
      Util.assertion(isMul4, "Thumb sp inc / dec size must be multiple of 4");
      numBits = 7;
      scale = 4;
      opc = isSub ? ARMGenInstrNames.tSUBspi : ARMGenInstrNames.tADDspi;
      isTwoAddr = true;
    } else if (!isSub && baseReg == ARMGenRegisterNames.SP) {
      // r1 = add sp, 403
      // =>
      // r1 = add sp, 100 * 4
      // r1 = add r1, 3
      if (!isMul4) {
        numBits &= ~3;
        extraOpc = ARMGenInstrNames.tADDi3;
      }
      numBits = 8;
      scale = 4;
      opc = ARMGenInstrNames.tADDrSPi;
    } else {
      // sp = sub sp, c
      // r1 = sub sp, c
      // r8 = sub sp, c
      if (destReg != baseReg)
        dstNotEqBase = true;
      numBits = 8;
      if (destReg == ARMGenRegisterNames.SP) {
        opc = isSub ? ARMGenInstrNames.tSUBspi : ARMGenInstrNames.tADDspi;
        Util.assertion(isMul4, "Thumb sp inc /dec size must be multiple of 4");
        numBits = 7;
        scale = 4;
      } else {
        opc = isSub ? ARMGenInstrNames.tSUBi8 : ARMGenInstrNames.tADDi8;
        numBits = 8;
        needCC = true;
      }
      isTwoAddr = true;
    }

    int numMIs = calcNumMI(opc, extraOpc, numBytes, numBits, scale);
    int threshold = destReg == ARMGenRegisterNames.SP ? 3 : 2;
    if (numMIs > threshold) {
      // This will expand into too many instructions. Load the immediate from a
      // constpool entry.
      return emitThumbRegPlusImmInReg(mbb, mi++, dl, destReg, baseReg, numBits, true, tii, tri, miFlags);
    }

    if (dstNotEqBase) {
      if (isARMLowRegister(destReg) && isARMLowRegister(baseReg)) {
        // If both are low registers, emit DestReg = add BaseReg, max(Imm, 7)
        int chunk = (1 << 3) - 1;
        int thisVal = Math.min(chunk, numBytes);
        numBytes -= thisVal;
        MCInstrDesc mcid = tii.get(isSub ? ARMGenInstrNames.tSUBi3 : ARMGenInstrNames.tADDi3);
        MachineInstrBuilder mib = addDefaultT1CC(buildMI(mbb, mi++, dl, mcid, destReg).setMIFlags(miFlags));
        addDefaultPred(mib.addReg(baseReg, getKillRegState(true)).addImm(thisVal));
      } else {
        addDefaultPred(buildMI(mbb, mi++, dl, tii.get(ARMGenInstrNames.tMOVr), destReg)
            .addReg(baseReg, getKillRegState(true)).setMIFlags(miFlags));
      }
      baseReg = destReg;
    }

    int chunk = ((1 << numBits) - 1) * scale;
    if (numBytes != 0) {
      int thisVal = Math.min(numBytes, chunk);
      numBytes -= thisVal;
      thisVal /= scale;
      // build the new tADD / tSUB.
      if (isTwoAddr) {
        MachineInstrBuilder mib = buildMI(mbb, mi++, dl, tii.get(opc), destReg);
        if (needCC)
          mib = addDefaultT1CC(mib);

        mib.addReg(destReg).addImm(thisVal);
        mib = addDefaultPred(mib);
        mib.setMIFlags(miFlags);
      } else {
        boolean isKill = baseReg != ARMGenRegisterNames.SP;
        MachineInstrBuilder mib = buildMI(mbb, mi++, dl, tii.get(opc), destReg);
        if (needCC)
          mib = addDefaultT1CC(mib);
        mib.addReg(baseReg, getKillRegState(isKill)).addImm(thisVal);
        mib = addDefaultPred(mib);
        mib.setMIFlags(miFlags);

        baseReg = destReg;
        if (opc == ARMGenInstrNames.tADDrSPi) {
          // r4 = add sp, imm
          // r4 = add r4, imm
          // ...
          numBits = 8;
          scale = 1;
          chunk = ((1 << numBits) - 1) * scale;
          opc = isSub ? ARMGenInstrNames.tSUBi8 : ARMGenInstrNames.tADDi8;
          needCC = isTwoAddr = true;
        }
      }
    }

    if (extraOpc != 0) {
      MCInstrDesc mcid = tii.get(extraOpc);
      addDefaultPred(addDefaultT1CC(buildMI(mbb, mi++, dl, mcid, destReg)
          .addReg(destReg, getKillRegState(true))
          .addImm(numBytes & 3)
          .setMIFlags(miFlags)));
    }
    return mi;
  }

  static int emitThumbRegPlusImmInReg(MachineBasicBlock mbb,
                                      int mi,
                                      DebugLoc dl,
                                      int destReg,
                                      int baseReg,
                                      int numBits,
                                      boolean canChangeCC,
                                      ARMInstrInfo tii,
                                      ARMRegisterInfo tri) {
    return emitThumbRegPlusImmInReg(mbb, mi, dl, destReg, baseReg, numBits, canChangeCC, tii, tri, 0);
  }

  /**
   * This function is used for emitting a series of instructions to materialize a
   * destreg = basereg + immediate in Thumb code. Materialize the immediate in a
   * register useing mov/mvn sequences or load the immediate form a constantpool entry.
   * @param mbb
   * @param mi
   * @param dl
   * @param destReg
   * @param baseReg
   * @param numBits
   * @param canChangeCC
   * @param tii
   * @param tri
   * @param miFlags
   * @return
   */
  static int emitThumbRegPlusImmInReg(MachineBasicBlock mbb,
                                      int mi,
                                      DebugLoc dl,
                                      int destReg,
                                      int baseReg,
                                      int numBits,
                                      boolean canChangeCC,
                                      ARMInstrInfo tii,
                                      ARMRegisterInfo tri,
                                      int miFlags) {
    MachineFunction mf = mbb.getParent();
    boolean isHigh = !isARMLowRegister(destReg) || (baseReg != 0 && !isARMLowRegister(baseReg));
    boolean isSub = false;
    // Subtract doesn't have high register version. Load the negative value
    // if either base or dest register is a high register. Also, if do not
    // issue sub as part of the sequence if condition register is to be
    // preserved.
    if (numBits < 0 && !isHigh && canChangeCC) {
      isSub = true;
      numBits = -numBits;
    }
    int ldReg = destReg;
    if (destReg == ARMGenRegisterNames.SP) {
      Util.assertion(baseReg == ARMGenRegisterNames.SP, "unexpected");
      ldReg = mf.getMachineRegisterInfo().createVirtualRegister(ARMGenRegisterInfo.tGPRRegisterClass);
    }

    if (numBits <= 255 && numBits >= 0)
      addDefaultT1CC(buildMI(mbb, mi++, dl, tii.get(ARMGenInstrNames.tMOVi8), ldReg).
          addImm(numBits).setMIFlags(miFlags));
    else if (numBits < 0 && numBits >= -255) {
      addDefaultT1CC(buildMI(mbb, mi++, dl, tii.get(ARMGenInstrNames.tMOVi8), ldReg).
          addImm(numBits).setMIFlags(miFlags));
      addDefaultT1CC(buildMI(mbb, mi++, dl, tii.get(ARMGenInstrNames.tRSB), ldReg)
          .addReg(ldReg, getKillRegState(true)).setMIFlags(miFlags));
    }
    else
      mi = tri.emitLoadConstantPool(mbb, mi, dl, ldReg, 0, numBits, ARMCC.CondCodes.AL, 0, miFlags);

    // emit add / sub.
    int opc = isSub ? ARMGenInstrNames.tSUBrr : (isHigh ? ARMGenInstrNames.tADDhirr : ARMGenInstrNames.tADDrr);
    MachineInstrBuilder mib = buildMI(mbb, mi++, dl, tii.get(opc), destReg);
    if (opc != ARMGenInstrNames.tADDhirr)
      mib = addDefaultT1CC(mib);
    if (destReg == ARMGenRegisterNames.SP || isSub)
      mib.addReg(baseReg).addReg(ldReg, getKillRegState(true));
    else
      mib.addReg(ldReg).addReg(baseReg, getKillRegState(true));

    addDefaultPred(mib);
    return mi;
  }

  public int emitLoadConstantPool(MachineBasicBlock mbb,
                                  int mbbi,
                                  DebugLoc dl,
                                  int destReg,
                                  int subIdx,
                                  int val,
                                  ARMCC.CondCodes pred) {
    return emitLoadConstantPool(mbb, mbbi, dl, destReg, subIdx, val, pred, 0);
  }

  public int emitLoadConstantPool(MachineBasicBlock mbb,
                                  int mbbi,
                                  DebugLoc dl,
                                  int destReg,
                                  int subIdx,
                                  int val,
                                  ARMCC.CondCodes pred,
                                  int predReg) {
    return emitLoadConstantPool(mbb, mbbi, dl, destReg, subIdx, val, pred, predReg, 0);
  }

  /**
   * Emit a load from constpool to materialize the specified immediate.
   * @param mbb
   * @param mbbi
   * @param dl
   * @param destReg
   * @param subIdx
   * @param val
   * @param pred
   * @param predReg
   * @param miFlags
   */
  public int emitLoadConstantPool(MachineBasicBlock mbb,
                                    int mbbi,
                                    DebugLoc dl,
                                    int destReg,
                                    int subIdx,
                                    int val,
                                    ARMCC.CondCodes pred,
                                    int predReg,
                                    int miFlags) {
    MachineFunction mf = mbb.getParent();
    MachineConstantPool constantPool = mf.getConstantPool();
    LLVMContext ctx = mf.getFunction().getContext();
    Constant c = ConstantInt.get(Type.getInt32Ty(ctx), val);
    int idx = constantPool.getConstantPoolIndex(c, 4);
    buildMI(mbb, mbbi++, dl, subtarget.getInstrInfo().get(ARMGenInstrNames.LDRcp))
        .addReg(destReg, getDefRegState(true), subIdx)
        .addConstantPoolIndex(idx, 0, 0)
        .addImm(0).addImm(pred.ordinal()).addReg(predReg)
        .setMIFlags(miFlags);
    return mbbi;
  }

  /**
   * Return the number of instructions requried to materialize the specific add/sub r, c instruction.
   *
   * @param opc
   * @param extraOpc
   * @param bytes
   * @param numBits
   * @param scale
   * @return
   */
  static int calcNumMI(int opc, int extraOpc, int bytes, int numBits, int scale) {
    int numMIs = 0;
    int chunk = ((1 << numBits) - 1) * scale;
    if (opc == ARMGenInstrNames.tADDrSPi) {
      int thisVal = Math.min(bytes, chunk);
      bytes -= thisVal;
      ++numBits;
      numBits = 0;
      scale = 1;
      chunk = ((1 << numBits) - 1) * scale;
    }

    numMIs += bytes / chunk;
    if ((bytes % chunk) != 0)
      ++numMIs;
    if (extraOpc != 0)
      ++numMIs;
    return numMIs;
  }

  @Override
  public int getFrameRegister(MachineFunction mf) {
    TargetFrameLowering tfl = subtarget.getFrameLowering();
    return tfl.hasFP(mf) ? framePtr : ARMGenRegisterNames.SP;
  }

  public int getBaseRegister() {
    return basePtr;
  }

  public boolean hasBasePointer(MachineFunction mf) {
    MachineFrameInfo mfi = mf.getFrameInfo();
    ARMFunctionInfo afi = (ARMFunctionInfo) mf.getInfo();

    if (!EnableBasePointer.value) return false;

    if (needsStackRealignment(mf) && mfi.hasVarSizedObjects())
      return true;

    if (afi.isThumbFunction() && mfi.hasVarSizedObjects()) {
      if (afi.isThumb2Function() && mfi.getLocalFrameSize() < 128)
        return false;
      return true;
    }

    return false;
  }

  @Override
  public boolean isMoveInstr(MachineInstr mi, int[] regs) {
    switch (mi.getOpcode()) {
      default:
        return false;
      case ARMGenInstrNames.MOVr:
        Util.assertion(mi.getNumOperands() >= 2 && mi.getOperand(0).isRegister() &&
                mi.getOperand(1).isRegister(),
            "invalid register-register move instruction");

        regs[0] = mi.getOperand(1).getReg();
        regs[1] = mi.getOperand(0).getReg();
        regs[2] = mi.getOperand(1).getSubReg();
        regs[3] = mi.getOperand(0).getSubReg();
        return true;
    }
  }

  /**
   * Given the enum value for some register, e.g.
   * ARMGenInstrNames.LR, return the number that it corresponds to (e.g. 14).
   *
   * @param Reg
   * @return
   */
  static int getARMRegisterNumbering(int Reg) {
    switch (Reg) {
      default:
        Util.shouldNotReachHere("Unknown ARM register!");
      case R0:
      case S0:
      case D0:
      case Q0:
        return 0;
      case R1:
      case S1:
      case D1:
      case Q1:
        return 1;
      case R2:
      case S2:
      case D2:
      case Q2:
        return 2;
      case R3:
      case S3:
      case D3:
      case Q3:
        return 3;
      case R4:
      case S4:
      case D4:
      case Q4:
        return 4;
      case R5:
      case S5:
      case D5:
      case Q5:
        return 5;
      case R6:
      case S6:
      case D6:
      case Q6:
        return 6;
      case R7:
      case S7:
      case D7:
      case Q7:
        return 7;
      case R8:
      case S8:
      case D8:
      case Q8:
        return 8;
      case R9:
      case S9:
      case D9:
      case Q9:
        return 9;
      case R10:
      case S10:
      case D10:
      case Q10:
        return 10;
      case R11:
      case S11:
      case D11:
      case Q11:
        return 11;
      case R12:
      case S12:
      case D12:
      case Q12:
        return 12;
      case SP:
      case S13:
      case D13:
      case Q13:
        return 13;
      case LR:
      case S14:
      case D14:
      case Q14:
        return 14;
      case PC:
      case S15:
      case D15:
      case Q15:
        return 15;

      case S16:
      case D16:
        return 16;
      case S17:
      case D17:
        return 17;
      case S18:
      case D18:
        return 18;
      case S19:
      case D19:
        return 19;
      case S20:
      case D20:
        return 20;
      case S21:
      case D21:
        return 21;
      case S22:
      case D22:
        return 22;
      case S23:
      case D23:
        return 23;
      case S24:
      case D24:
        return 24;
      case S25:
      case D25:
        return 25;
      case S26:
      case D26:
        return 26;
      case S27:
      case D27:
        return 27;
      case S28:
      case D28:
        return 28;
      case S29:
      case D29:
        return 29;
      case S30:
      case D30:
        return 30;
      case S31:
      case D31:
        return 31;
    }
  }

  /**
   * Returns true if the register is a low register (r0-r7).
   *
   * @param Reg
   * @return
   */
  static boolean isARMLowRegister(int Reg) {
    switch (Reg) {
      case R0:
      case R1:
      case R2:
      case R3:
      case R4:
      case R5:
      case R6:
      case R7:
        return true;
      default:
        return false;
    }
  }

  /**
   * We can't realign the stack if:
   * 1. Dynamic stack realignment is explicitly disabled,
   * 2. This is a Thumb1 function (it's not useful, so we don't bother), or
   * 3. There are VLAs in the function and the base pointer is disabled.
   *
   * @param mf
   * @return
   */
  public boolean canRealignStack(MachineFunction mf) {
    MachineFrameInfo mfi = mf.getFrameInfo();
    ARMFunctionInfo funcInfo = (ARMFunctionInfo) mf.getInfo();
    return EnableRealignStack.value && !funcInfo.isThumb1OnlyFunction() &&
        (!mfi.hasVarSizedObjects() || EnableBasePointer.value);
  }
}
