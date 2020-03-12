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
import backend.mc.MCRegisterClass;

import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.codegen.MachineInstrBuilder.getKillRegState;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
class Thumb2InstrInfo extends ARMGenInstrInfo {
  Thumb2InstrInfo(ARMSubtarget subtarget) {
    super(subtarget);
  }

  @Override
  public boolean copyPhysReg(MachineBasicBlock mbb,
                             int insertPos,
                             int dstReg, int srcReg,
                             MCRegisterClass dstRC,
                             MCRegisterClass srcRC) {
    // Handle SPR, DPR, and QPR copies.
    if (!ARMGenRegisterInfo.GPRRegisterClass.contains(dstReg) ||
        !ARMGenRegisterInfo.GPRRegisterClass.contains(srcReg)) {
      return super.copyPhysReg(mbb, insertPos, dstReg, srcReg, dstRC, srcRC);
    }

    addDefaultPred(buildMI(mbb, insertPos, new DebugLoc(),
        get(ARMGenInstrNames.tMOVr), dstReg).addReg(srcReg, getKillRegState(false)));
    return true;
  }

  @Override
  public void storeRegToStackSlot(MachineBasicBlock mbb,
                                  int pos, int srcReg,
                                  boolean isKill,
                                  int frameIndex,
                                  MCRegisterClass rc) {
    if (rc == ARMGenRegisterInfo.GPRRegisterClass ||
        rc == ARMGenRegisterInfo.tGPRRegisterClass ||
        rc == ARMGenRegisterInfo.tcGPRRegisterClass ||
        rc == ARMGenRegisterInfo.rGPRRegisterClass ||
        rc == ARMGenRegisterInfo.GPRnopcRegisterClass) {
      DebugLoc dl = new DebugLoc();
      if (pos != mbb.size())
        dl = mbb.getInstAt(pos).getDebugLoc();

      MachineFunction mf = mbb.getParent();
      MachineFrameInfo mfi = mf.getFrameInfo();
      MachineMemOperand mmo = new MachineMemOperand(PseudoSourceValue.getFixedStack(frameIndex),
          MachineMemOperand.MOStore,
          mfi.getObjectOffset(frameIndex),
          mfi.getObjectSize(frameIndex),
          mfi.getObjectAlignment(frameIndex));
      addDefaultPred(buildMI(mbb, pos, dl, get(ARMGenInstrNames.t2STRi12))
          .addReg(srcReg, getKillRegState(isKill))
          .addFrameIndex(frameIndex).addImm(0).addMemOperand(mmo));
      return;
    }
    super.storeRegToStackSlot(mbb, pos, srcReg, isKill, frameIndex, rc);
  }

  @Override
  public void loadRegFromStackSlot(MachineBasicBlock mbb,
                                   int pos, int destReg,
                                   int frameIndex,
                                   MCRegisterClass rc) {
    if (rc == ARMGenRegisterInfo.GPRRegisterClass ||
        rc == ARMGenRegisterInfo.tGPRRegisterClass ||
        rc == ARMGenRegisterInfo.tcGPRRegisterClass ||
        rc == ARMGenRegisterInfo.rGPRRegisterClass ||
        rc == ARMGenRegisterInfo.GPRnopcRegisterClass) {
      DebugLoc dl = new DebugLoc();
      if (pos != mbb.size())
        dl = mbb.getInstAt(pos).getDebugLoc();

      MachineFunction mf = mbb.getParent();
      MachineFrameInfo mfi = mf.getFrameInfo();
      MachineMemOperand mmo = new MachineMemOperand(PseudoSourceValue.getFixedStack(frameIndex),
          MachineMemOperand.MOLoad,
          mfi.getObjectOffset(frameIndex),
          mfi.getObjectSize(frameIndex),
          mfi.getObjectAlignment(frameIndex));
      addDefaultPred(buildMI(mbb, pos, dl, get(ARMGenInstrNames.t2LDRi12), destReg)
          .addFrameIndex(frameIndex).addImm(0).addMemOperand(mmo));
      return;
    }
    super.loadRegFromStackSlot(mbb, pos, destReg, frameIndex, rc);
  }
}
