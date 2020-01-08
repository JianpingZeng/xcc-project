package backend.target.arm;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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
import backend.target.TargetFrameLowering;
import tools.Util;

import java.util.ArrayList;

import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.target.arm.ARMRegisterInfo.*;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ARMFrameLowering extends TargetFrameLowering {
  private ARMTargetMachine tm;
  private ARMSubtarget subtarget;
  private int framePtr;

  public ARMFrameLowering(ARMTargetMachine tm) {
    super(StackDirection.StackGrowDown, tm.getSubtarget().getStackAlignment(), -4);
    this.tm = tm;
    this.subtarget = tm.getSubtarget();
    framePtr = (subtarget.isThumb() || subtarget.isTargetDarwin()) ?
        ARMGenRegisterNames.R7 : ARMGenRegisterNames.R11;
  }

  @Override
  public boolean hasReservedCallFrame(MachineFunction mf) {
    MachineFrameInfo mfi = mf.getFrameInfo();
    long frameSize = mfi.getMaxCallFrameSize();
    ARMFunctionInfo afi = (ARMFunctionInfo) mf.getInfo();
    // It is not always good to include the call frame as a part of the current
    // function stack frame. Because a large call frames might cause poor codegen
    // and may even makes it impossible to scavenge a register.
    if (afi.isThumbFunction()) {
      // less than the half of imm8/2
      if (frameSize >= ((1 << 8)-1)/2)
        return false;
    }
    else {
      if (frameSize >= ((1 << 12)-1)/2)
        return false;
    }

    return super.hasReservedCallFrame(mf);
  }

  private static int emitSPUpdate(boolean isARM, MachineBasicBlock mbb,
                                   int mbbi, DebugLoc dl, ARMInstrInfo tii,
                                   int numBytes) {
    return emitSPUpdate(isARM, mbb, mbbi, dl, tii, numBytes, ARMCC.CondCodes.AL);
  }

  private static int emitSPUpdate(boolean isARM, MachineBasicBlock mbb,
                                   int mbbi, DebugLoc dl, ARMInstrInfo tii,
                                   int numBytes, ARMCC.CondCodes pred) {
    return emitSPUpdate(isARM, mbb, mbbi, dl, tii, numBytes, pred, 0);
  }

  static int emitSPUpdate(boolean isARM, MachineBasicBlock mbb,
                            int mbbi, DebugLoc dl, ARMInstrInfo tii,
                            int numBytes, ARMCC.CondCodes pred,
                            int predReg) {
    if (isARM)
      return ARMRegisterInfo.emitARMRegPlusImmediate(mbb, mbbi, dl, ARMGenRegisterNames.SP,
          ARMGenRegisterNames.SP, numBytes, pred, predReg, tii);
    else
      return emitT2RegPlusImmediate(mbb, mbbi, dl, ARMGenRegisterNames.SP,
          ARMGenRegisterNames.SP, numBytes, pred, predReg, tii);
  }
  @Override
  public void emitPrologue(MachineFunction mf) {
    ARMFunctionInfo afi = (ARMFunctionInfo) mf.getInfo();
    Util.assertion(!afi.isThumb1OnlyFunction(), "this emitPrologue dosen't support Thumb1");
    MachineFrameInfo mfi = mf.getFrameInfo();
    boolean isARM = !afi.isThumbFunction();
    int varRegSaveSize = afi.getVarArgsRegSaveSize();
    int numBytes = mfi.getStackSize();
    ArrayList<CalleeSavedInfo> csis = mfi.getCalleeSavedInfo();
    MachineBasicBlock mbb = mf.getEntryBlock();
    ARMInstrInfo tii = subtarget.getInstrInfo();
    int mbbi = 0;

    DebugLoc dl = mbbi == mbb.size() ? new DebugLoc() : mbb.getInstAt(mbbi).getDebugLoc();

    // keep tracks of the size of callee saved registers for GPR, GPR of darwin, and DPR.
    int gprCS1Size = 0, gprCS2Size = 0, dprCSSize = 0;
    int framePtrSpillFI = 0;
    if (varRegSaveSize != 0)
      mbbi = emitSPUpdate(isARM, mbb, mbbi, dl, tii, -varRegSaveSize);

    if (!afi.hasStackFrame()) {
      if (numBytes != 0)
        mbbi = emitSPUpdate(isARM, mbb, mbbi, dl, tii, -numBytes);
      return;
    }

    for (CalleeSavedInfo csi : csis) {
      int reg = csi.getReg();
      int fi = csi.getFrameIdx();
      switch (reg) {
        case ARMGenRegisterNames.R4:
        case ARMGenRegisterNames.R5:
        case ARMGenRegisterNames.R6:
        case ARMGenRegisterNames.R7:
        case ARMGenRegisterNames.LR:
          // The frame pointer register might be R7 or R11 depends on thumb || darwin or ARM mode.
          if (reg == framePtr)
            framePtrSpillFI = fi;
          afi.addGPRCalleeSavedArea1Frame(fi);
          gprCS1Size += 4;
        case ARMGenRegisterNames.R8:
        case ARMGenRegisterNames.R9:
        case ARMGenRegisterNames.R10:
        case ARMGenRegisterNames.R11:
          if (reg == framePtr)
            framePtrSpillFI = fi;
          if (subtarget.isTargetDarwin()) {
            afi.addGPRCalleeSavedArea2Frame(fi);
            gprCS2Size += 4;
          }
          else {
            afi.addGPRCalleeSavedArea1Frame(fi);
            gprCS1Size += 4;
          }
          break;
        default:
          afi.addDPRCalleeSavedAreaFrame(fi);
          dprCSSize += 8;
      }
    }

    // Build teh new SUBri to adjust SP for integer callee-saved spill area 1.
    mbbi = emitSPUpdate(isARM, mbb, mbbi, dl, tii, -gprCS1Size);
    mbbi = movePastCSLoadStoreOps(mbb, mbbi, ARMGenInstrNames.STRi12, ARMGenInstrNames.t2STRi12, 1, subtarget);

    // Darwin ABI requires FP to point to the stack slot that contains the previous FP.
    if (subtarget.isTargetDarwin() || hasFP(mf)) {
      int addRIOpc = !afi.isThumbFunction() ? ARMGenInstrNames.ADDri : ARMGenInstrNames.t2ADDri;
      MachineInstrBuilder mib = buildMI(mbb, mbbi, dl, tii.get(addRIOpc), framePtr)
          .addFrameIndex(framePtrSpillFI).addImm(0);
      addDefaultCC(addDefaultPred(mib));
    }

    // Build the new SUBri to adjust SP for integer callee-saved-registers area 2.
    mbbi = emitSPUpdate(isARM, mbb, mbbi, dl, tii, -gprCS2Size);
    mbbi = movePastCSLoadStoreOps(mbb, mbbi, ARMGenInstrNames.STRi12, ARMGenInstrNames.t2STRi12, 2, subtarget);

    // Build the new SUBri for double float fp callee-saved-registers.
    mbbi = emitSPUpdate(isARM, mbb, mbbi, dl, tii, -dprCSSize);

    // Determine starting offsets of spill areas.
    // The following is a stack layout for ARMV6&V7
    // https://developer.apple.com/library/archive/documentation/Xcode/Conceptual/iPhoneOSABIReference/Articles/ARMv6FunctionCallingConventions.html#//apple_ref/doc/uid/TP40009021-SW1
    // [   GPRCS area 1    ]
    // [   GPRCS area 2    ]
    // [   DPRCS area      ]
    // [   local variables ]
    //         | stack growth downside.
    //         v
    int dprCSOffset = numBytes - (gprCS1Size + gprCS2Size + dprCSSize);
    int gprCS1Offset = dprCSOffset + dprCSSize;
    int gprCS2Offset = gprCS1Offset + gprCS2Size;
    afi.setDPRCalleeSavedAreaOffset(dprCSOffset);
    afi.setGPRCalleeSavedArea1Offset(gprCS1Offset);
    afi.setGPRCalleeSavedArea2Offset(gprCS2Offset);
    afi.setFramePtrSpillOffset(mfi.getObjectOffset(framePtrSpillFI) + numBytes);

    numBytes = dprCSOffset;
    // if the offset of DPR callee-saved-register area is not zero, adjust SP.
    if (numBytes != 0) {
      mbbi = movePastCSLoadStoreOps(mbb, mbbi, ARMGenInstrNames.VSTRD, 0, 3, subtarget);
      mbbi = emitSPUpdate(isARM, mbb, mbbi, dl, tii, -dprCSSize);
    }
    if (subtarget.isTargetELF() && hasFP(mf)) {
      mfi.setOffsetAdjustment(mfi.getOffsetAdjustment() + afi.getFramePtrSpillOffset());
    }

    afi.setGPRCalleeSavedArea1Size(gprCS1Size);
    afi.setGPRCalleeSavedArea2Size(gprCS2Size);
    afi.setDPRCalleeSavedAreaSize(dprCSSize);
  }

  /**
   * Move the machine basic block iterator pass through those opcodes given in the {@code opc1} and
   * {@code opc2} for the given type of callee-saved-register.
   * In generally, all callee-saved-registers are divided into the following three kinds depends on
   * the platform, e.g. Darwin, or if the Thumb mode is enable.
   * Kind1: R4-R7, LR
   * Kind2: R8-R11 if darwin is used. Otherwise, it might be assigned to kind1.
   * Kind3: D8-D15.
   * @param mbb
   * @param mbbi
   * @param opc1
   * @param opc2
   * @param area
   * @param subtarget
   * @return
   */
  private static int movePastCSLoadStoreOps(MachineBasicBlock mbb, int mbbi,
                                            int opc1, int opc2, int area,
                                            ARMSubtarget subtarget) {
    while (mbbi < mbb.size() && (mbb.getInstAt(mbbi).getOpcode() == opc1 ||
        mbb.getInstAt(mbbi).getOpcode() == opc2) && mbb.getInstAt(mbbi).getOperand(1).isFrameIndex()) {
      if (area != 0) {
        int category = 0;
        boolean done = false;
        switch (mbb.getInstAt(mbbi).getOperand(0).getReg()) {
          case ARMGenRegisterNames.R4:
          case ARMGenRegisterNames.R5:
          case ARMGenRegisterNames.R6:
          case ARMGenRegisterNames.R7:
          case ARMGenRegisterNames.LR:
            category = 1;
            break;
          case ARMGenRegisterNames.R8:
          case ARMGenRegisterNames.R9:
          case ARMGenRegisterNames.R10:
          case ARMGenRegisterNames.R11:
            category = subtarget.isTargetDarwin() ? 2 : 1;
            break;
          case ARMGenRegisterNames.D8:
          case ARMGenRegisterNames.D9:
          case ARMGenRegisterNames.D10:
          case ARMGenRegisterNames.D11:
          case ARMGenRegisterNames.D12:
          case ARMGenRegisterNames.D13:
          case ARMGenRegisterNames.D14:
          case ARMGenRegisterNames.D15:
            category = 3;
            break;
          default:
            done = true;
            break;
        }
        if (done || category == area)
          break;
      }
      ++mbbi;
    }
    return mbbi;
  }

  private static boolean isCSRestore(MachineInstr mi, int[] csRegs) {
    Util.assertion(csRegs != null && csRegs.length > 0);
    int opc = mi.getOpcode();
    return (opc == ARMGenInstrNames.VLDRD ||
        opc == ARMGenInstrNames.LDRi12 ||
        opc == ARMGenInstrNames.t2LDRi12) &&
        mi.getOperand(1).isFrameIndex() &&
        isCalleeSavedRegister(mi.getOperand(0).getReg(), csRegs);
  }

  private static boolean isCalleeSavedRegister(int reg, int[] csRegs) {
    Util.assertion(csRegs != null && csRegs.length > 0);
    for (int r : csRegs)
      if (r == reg) return true;
    return false;
  }

  @Override
  public void emitEpilogue(MachineFunction mf, MachineBasicBlock mbb) {
    ARMFunctionInfo afi = (ARMFunctionInfo) mf.getInfo();
    Util.assertion(!afi.isThumb1OnlyFunction(), "this emitPrologue dosen't support Thumb1");
    MachineFrameInfo mfi = mf.getFrameInfo();
    boolean isARM = !afi.isThumbFunction();
    int varRegSaveSize = afi.getVarArgsRegSaveSize();
    int numBytes = mfi.getStackSize();
    ARMInstrInfo tii = subtarget.getInstrInfo();
    int mbbi = mbb.size() - 1;

    DebugLoc dl = mbbi == mbb.size() ? new DebugLoc() : mbb.getInstAt(mbbi).getDebugLoc();
    if (varRegSaveSize != 0)
      mbbi = emitSPUpdate(isARM, mbb, mbbi, dl, tii, varRegSaveSize);

    if (!afi.hasStackFrame()) {
      if (numBytes != 0)
        emitSPUpdate(isARM, mbb, mbbi, dl, tii, numBytes);
      return;
    }

    // move the mbbi to point to the first LDR/VLD
    ARMRegisterInfo tri = subtarget.getRegisterInfo();
    int[] csRegs = tri.getCalleeSavedRegs(mf);
    if (csRegs != null && csRegs.length > 0) {
      if (mbbi != 0) {
        do {
          --mbbi;
        }while (mbbi != 0 && isCSRestore(mbb.getInstAt(mbbi), csRegs));
        if (!isCSRestore(mbb.getInstAt(mbbi), csRegs))
          ++mbbi;
      }
    }

    // move SP to the start of FP callee save spill area.
    numBytes -= afi.getGPRCalleeSavedArea2Size() +
        afi.getGPRCalleeSavedArea2Size() +
        afi.getDPRCalleeSavedAreaSize();

    if ((subtarget.isTargetDarwin() && numBytes != 0) || hasFP(mf)) {
      // Reset SP based on frame pointer only if the stack frame extends beyond
      // frame pointer stack slot or target is ELF and the function has FP.
      if (hasFP(mf) || afi.getGPRCalleeSavedArea2Size() != 0 ||
          afi.getDPRCalleeSavedAreaSize() != 0 ||
          afi.getDPRCalleeSavedAreaOffset() != 0) {
        if (numBytes != 0) {
          if (isARM)
            mbbi = emitARMRegPlusImmediate(mbb, mbbi, dl, ARMGenRegisterNames.SP, framePtr, -numBytes,
                ARMCC.CondCodes.AL, 0, tii);
          else
            mbbi = emitT2RegPlusImmediate(mbb, mbbi, dl, ARMGenRegisterNames.SP, framePtr, -numBytes,
                ARMCC.CondCodes.AL, 0, tii);
        }
        else {
          if (isARM)
            buildMI(mbb, mbbi, dl, tii.get(ARMGenInstrNames.MOVr), ARMGenRegisterNames.SP)
                .addReg(framePtr)
                .addImm(ARMCC.CondCodes.AL.ordinal())
                .addReg(0)
                .addReg(0);
          else
            buildMI(mbb, mbbi, dl, tii.get(ARMGenInstrNames.t2MOVr), ARMGenRegisterNames.SP)
                .addReg(framePtr);
        }
      }
    }
    else if (numBytes != 0) {
      mbbi = emitSPUpdate(isARM, mbb, mbbi, dl, tii, numBytes);
    }

    // Move SP to start of integer callee save spill area 2.
    mbbi = movePastCSLoadStoreOps(mbb, mbbi, ARMGenInstrNames.VLDRD, 0, 3, subtarget);
    mbbi = emitSPUpdate(isARM, mbb, mbbi, dl, tii, afi.getDPRCalleeSavedAreaSize());

    // move SP to start of integer callee save spill area 1.
    mbbi = movePastCSLoadStoreOps(mbb, mbbi, ARMGenInstrNames.LDRi12, ARMGenInstrNames.t2LDRi12, 2, subtarget);
    mbbi = emitSPUpdate(isARM, mbb, mbbi, dl, tii, afi.getGPRCalleeSavedArea2Size());

    // Move SP to SP upon entry to the function.
    mbbi = movePastCSLoadStoreOps(mbb, mbbi, ARMGenInstrNames.LDRi12, ARMGenInstrNames.t2LDRi12, 1, subtarget);
    emitSPUpdate(isARM, mbb, mbbi, dl, tii, afi.getGPRCalleeSavedArea1Size());
  }

  /**
   * Check if the given function requires a dedicated frame pointer. It
   * returns true if the target platform is MacOSX or the function has
   * variable sized allocas or the elimination of frame pointer is disabled.
   * @param mf
   * @return
   */
  @Override
  public boolean hasFP(MachineFunction mf) {
    if (subtarget.isTargetDarwin()) return true;

    MachineFrameInfo mfi = mf.getFrameInfo();
    ARMRegisterInfo ari = subtarget.getRegisterInfo();
    return (disableFramePointerElim(mf) && mfi.hasCalls()) ||
        ari.needsStackRealignment(mf) || mfi.hasVarSizedObjects() ||
        mfi.isFrameAddressTaken();
  }

  @Override
  public void processFunctionBeforeCalleeSavedScan(MachineFunction mf, RegScavenger scavenger) {
    // TODO
  }
}
