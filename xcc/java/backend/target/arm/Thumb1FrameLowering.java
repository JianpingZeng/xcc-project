package backend.target.arm;
/*
 * Extremely Compiler Collection
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
import tools.Util;

import java.util.ArrayList;

import static backend.support.ErrorHandling.reportFatalError;
import static backend.target.arm.ARMRegisterInfo.addDefaultPred;
import static backend.target.arm.ARMRegisterInfo.emitThumbRegPlusImmediate;

/**
 * This file defines some Thumb1 specific frame lowering interfaces, such as emit function
 * prologue, epilogue, etc.
 * @author Jianping Zeng.
 * @version 0.4
 */
class Thumb1FrameLowering extends ARMFrameLowering {
  Thumb1FrameLowering(ARMSubtarget subtarget) {
    super(subtarget);
  }

  private static int emitSPUpdate(MachineBasicBlock mbb,
                                   int mbbi,
                                   ARMInstrInfo tii,
                                   DebugLoc dl,
                                   Thumb1RegisterInfo mri,
                                   int numBytes) {
    return emitSPUpdate(mbb, mbbi, tii, dl, mri, numBytes, MachineInstr.NoFlags);
  }

  private static int emitSPUpdate(MachineBasicBlock mbb,
                                   int mbbi,
                                   ARMInstrInfo tii,
                                   DebugLoc dl,
                                   Thumb1RegisterInfo mri,
                                   int numBytes,
                                   int miFlags) {
    return ARMRegisterInfo.emitThumbRegPlusImmediate(mbb, mbbi, dl, ARMGenRegisterNames.SP,
        ARMGenRegisterNames.SP, numBytes, tii, mri, miFlags);
  }

  @Override
  public void emitPrologue(MachineFunction mf) {
    // generate the following instructions for setup a function frame in the first MBB.
    // add/sub sp, imm
    Thumb1InstrInfo tii = (Thumb1InstrInfo) subtarget.getInstrInfo();
    Thumb1RegisterInfo tri = (Thumb1RegisterInfo) subtarget.getRegisterInfo();

    ARMFunctionInfo afi = (ARMFunctionInfo) mf.getFunctionInfo();
    int vaRegSaveSize = afi.getVarArgsRegSaveSize();
    MachineFrameInfo mfi = mf.getFrameInfo();
    int numBytes = mfi.getStackSize();
    ArrayList<CalleeSavedInfo> csi = mfi.getCalleeSavedInfo();
    MachineBasicBlock mbb = mf.getEntryBlock();
    DebugLoc dl = mbb.isEmpty() ? new DebugLoc() : mbb.getFirstInst().getDebugLoc();

    // align the stack size to the power of 4.
    numBytes = (numBytes + 3) & ~3;
    mfi.setStackSize(numBytes);

    int gprCS1Size = 0, gprCS2Size = 0, dprCSSize = 0;
    int framePtrSpillFI = 0;
    int mbbi = 0;

    if (vaRegSaveSize != 0) {
      mbbi = emitSPUpdate(mbb, mbbi, tii, dl, tri, -vaRegSaveSize, MachineInstr.FrameSetup);
    }

    if (!afi.hasStackFrame()) {
      if (numBytes != 0)
        mbbi = emitSPUpdate(mbb, mbbi, tii, dl, tri, -numBytes, MachineInstr.FrameSetup);
      return;
    }

    for (int i = 0, e = csi.size(); i != e; ++i) {
      int reg = csi.get(i).getReg();
      int fi = csi.get(i).getFrameIdx();
      switch (reg) {
        case ARMGenRegisterNames.R4:
        case ARMGenRegisterNames.R5:
        case ARMGenRegisterNames.R6:
        case ARMGenRegisterNames.R7:
        case ARMGenRegisterNames.LR:
          if (reg == framePtr)
            framePtrSpillFI = fi;
          afi.addGPRCalleeSavedArea1Frame(fi);
          gprCS1Size += 4;
          break;
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

      if (mbbi != mbb.size() && mbb.getInstAt(mbbi).getOpcode() == ARMGenInstrNames.tPUSH) {
        ++mbbi;
        if (mbbi != mbb.size())
          dl = mbb.getInstAt(mbbi).getDebugLoc();
      }

      // Determine starting offsets of spill areas.
      int dprCSOffset = numBytes - (gprCS1Size + gprCS2Size + dprCSSize);
      int gprCS2Offset = dprCSOffset + dprCSSize;
      int gprCS1Offset = gprCS2Offset + gprCS2Size;
      afi.setFramePtrSpillOffset(mfi.getObjectOffset(framePtrSpillFI) + numBytes);
      afi.setGPRCalleeSavedArea1Offset(gprCS1Offset);
      afi.setGPRCalleeSavedArea2Offset(gprCS2Offset);
      afi.setDPRCalleeSavedAreaOffset(dprCSOffset);
      numBytes = dprCSOffset;

      if (hasFP(mf)) {
        addDefaultPred(MachineInstrBuilder.buildMI(mbb, mbbi, dl, tii.get(ARMGenInstrNames.tADDrSPi),
            framePtr).addFrameIndex(framePtrSpillFI).addImm(0)
            .setMIFlags(MachineInstr.FrameSetup));
        if (numBytes > 508) {
          // if offset is > 508, then sp can't be adjusted in a single instruction.
          // try restoring from fp instead.
          afi.setShouldRestoreSPFromFP(true);
        }
      }

      if (numBytes != 0)
        emitSPUpdate(mbb, mbbi, tii, dl, tri, -numBytes, MachineInstr.FrameSetup);

      if (subtarget.isTargetELF() && hasFP(mf)) {
        mfi.setOffsetAdjustment(mfi.getOffsetAdjustment() - afi.getFramePtrSpillOffset());
      }

      afi.setGPRCalleeSavedArea1Size(gprCS1Size);
      afi.setGPRCalleeSavedArea2Size(gprCS2Size);
      afi.setDPRCalleeSavedAreaSize(dprCSSize);

      if (tri.needsStackRealignment(mf))
        reportFatalError("Dynamic stack realignment not supported for thumb1");

      // If we need a base pointer, set it up here. It's whatever the value
      // of the stack pointer is at this point. Any variable size objects
      // will be allocated after this, so we can still use the base pointer
      // to reference locals.
      int basePtr = tri.getBaseRegister();
      if (tri.hasBasePointer(mf))
        addDefaultPred(MachineInstrBuilder.buildMI(mbb, mbbi, dl, tii.get(ARMGenInstrNames.tMOVr), basePtr)
        .addReg(ARMGenRegisterNames.SP));

      // If the frame has variable sized objects then the epilogue must restore
      // the sp from fp. We can assume there's an FP here since hasFP already
      // checks for hasVarSizedObjects.
      if (mfi.hasVarSizedObjects())
        afi.setShouldRestoreSPFromFP(true);
    }
  }

  private static boolean isCSRestore(MachineInstr mi, int[] csRegs) {
    Util.assertion(csRegs != null && csRegs.length > 0);

    int opc = mi.getOpcode();
    if  (opc == ARMGenInstrNames.tLDRspi &&
        mi.getOperand(1).isFrameIndex() &&
        isCalleeSavedRegister(mi.getOperand(0).getReg(), csRegs))
      return true;

    else if (mi.getOpcode() == ARMGenInstrNames.tPOP) {
      // The first two operands are predicates. The last two are
      // imp-def and imp-use of SP. Check everything in between.
      for (int i = 2, e = mi.getNumOperands() - 2; i != e; ++i) {
        if (!isCalleeSavedRegister(mi.getOperand(i).getReg(), csRegs))
          return false;
      }
      return true;
    }
    return false;
  }

  private static boolean isCalleeSavedRegister(int reg, int[] csRegs) {
    Util.assertion(csRegs != null && csRegs.length > 0);
    for (int r : csRegs)
      if (r == reg) return true;
    return false;
  }

  @Override
  public void emitEpilogue(MachineFunction mf, MachineBasicBlock mbb) {
    ARMFunctionInfo afi = (ARMFunctionInfo) mf.getFunctionInfo();
    int vaRegSaveSize = afi.getVarArgsRegSaveSize();
    MachineFrameInfo mfi = mf.getFrameInfo();
    int numBytes = mfi.getStackSize();
    Thumb1RegisterInfo regInfo = (Thumb1RegisterInfo) subtarget.getRegisterInfo();
    int[] csi = regInfo.getCalleeSavedRegs(mf);
    int framePtr = regInfo.getFrameRegister(mf);
    int mbbi = 0;
    Thumb1InstrInfo tii = (Thumb1InstrInfo) subtarget.getInstrInfo();
    DebugLoc dl = mbb.getLastInst().getDebugLoc();

    if (!afi.hasStackFrame()) {
      if (numBytes != 0)
        mbbi = emitSPUpdate(mbb, mbbi, tii, dl, regInfo, numBytes);
    }
    else {
      // unwind mbbi to point to first LD/VLDRD.
      if (mbbi != 0) {
        do {
          --mbbi;
        }while (mbbi != 0 && isCSRestore(mbb.getInstAt(mbbi), csi));
        if (!isCSRestore(mbb.getInstAt(mbbi), csi))
          ++mbbi;
      }

      numBytes -= (afi.getGPRCalleeSavedArea1Size() +
          afi.getGPRCalleeSavedArea2Size() +
          afi.getDPRCalleeSavedAreaSize());

      if (afi.shouldRestoreSPFromFP()) {
        numBytes = afi.getFramePtrSpillOffset() - numBytes;
        if (numBytes != 0) {
          Util.assertion(mf.getMachineRegisterInfo().isPhysRegUsed(ARMGenRegisterNames.R4),
              "No scratch register to restore SP from FP!");
          emitThumbRegPlusImmediate(mbb, mbbi, dl, ARMGenRegisterNames.R4,
              framePtr, -numBytes, tii, regInfo);
          addDefaultPred(MachineInstrBuilder.buildMI(mbb, mbbi, dl, tii.get(ARMGenInstrNames.tMOVr),
              ARMGenRegisterNames.SP).addReg(ARMGenRegisterNames.R4));
        }
        else {
          addDefaultPred(MachineInstrBuilder.buildMI(mbb, mbbi, dl, tii.get(ARMGenInstrNames.tMOVr),
              ARMGenRegisterNames.SP).addReg(framePtr));
        }
      }
      else {
        if (mbb.getInstAt(mbbi).getOpcode() == ARMGenInstrNames.tBX_RET &&
            mbbi != 0 && mbb.getInstAt(mbbi-1).getOpcode() == ARMGenInstrNames.tPOP) {
          emitSPUpdate(mbb, mbbi-1, tii, dl, regInfo, numBytes);
        }
        else {
          mbbi = emitSPUpdate(mbb, mbbi, tii, dl, regInfo, numBytes);
        }
      }
    }

    if (vaRegSaveSize != 0) {
      while (mbbi != mbb.size() && isCSRestore(mbb.getInstAt(mbbi), csi))
        ++mbbi;

      addDefaultPred(MachineInstrBuilder.buildMI(mbb, mbbi, dl, tii.get(ARMGenInstrNames.tPOP)))
          .addReg(ARMGenRegisterNames.R3, MachineOperand.RegState.Define);

      mbbi = emitSPUpdate(mbb, mbbi, tii, dl, regInfo, vaRegSaveSize);
      addDefaultPred(MachineInstrBuilder.buildMI(mbb, mbbi, dl, tii.get(ARMGenInstrNames.tBX_RET_vararg))
          .addReg(ARMGenRegisterNames.R3, MachineInstrBuilder.getKillRegState(true)));
      mbb.remove(mbbi);
    }
  }

  @Override
  public boolean hasReservedCallFrame(MachineFunction mf) {
    MachineFrameInfo mfi = mf.getFrameInfo();
    long cfSize = mfi.getMaxCallFrameSize();
    // half of imm8*4
    if (cfSize >= ((1<<8) - 1) * 4 / 2)
      return false;

    return !mf.getFrameInfo().hasVarSizedObjects();
  }
}
