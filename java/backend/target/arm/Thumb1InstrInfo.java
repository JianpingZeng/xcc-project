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
import backend.target.TargetFrameLowering;
import backend.target.TargetRegisterInfo;
import backend.target.TargetSubtarget;
import tools.OutRef;
import tools.Util;

import java.util.ArrayList;

import static backend.codegen.MachineInstrBuilder.getKillRegState;
import static backend.target.arm.ARMRegisterInfo.isARMLowRegister;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
class Thumb1InstrInfo extends ARMGenInstrInfo {
  Thumb1InstrInfo(ARMSubtarget subtarget) {
    super(subtarget);
  }

  @Override
  public boolean spillCalleeSavedRegisters(MachineBasicBlock mbb,
                                           OutRef<Integer> insertPos,
                                           ArrayList<CalleeSavedInfo> csi) {
    if (csi.isEmpty())
      return false;

    DebugLoc dl = new DebugLoc();
    MachineFunction mf = mbb.getParent();
    int pos = insertPos.get();
    if (pos != mbb.size())
      dl = mbb.getInstAt(pos).getDebugLoc();

    MachineInstrBuilder mib = MachineInstrBuilder.buildMI(mbb, pos++, dl, get(ARMGenInstrNames.tPUSH));
    addDefaultPred(mib);
    for (int i = csi.size(); i != 0; --i) {
      int reg = csi.get(i-1).getReg();
      mbb.addLiveIn(reg);
      mib.addReg(reg, getKillRegState(true));
    }

    mib.setMIFlags(MachineInstr.FrameSetup);
    insertPos.set(pos);
    return true;
  }

  @Override
  public boolean restoreCalleeSavedRegisters(MachineBasicBlock mbb,
                                             int pos,
                                             ArrayList<CalleeSavedInfo> csi) {
    if (csi.isEmpty())
      return false;

    MachineFunction mf = mbb.getParent();
    ARMFunctionInfo afi = (ARMFunctionInfo) mf.getInfo();
    boolean isVarArg = afi.getVarArgsRegSaveSize() > 0;
    DebugLoc dl = mbb.getInstAt(pos).getDebugLoc();
    MachineInstrBuilder mib = MachineInstrBuilder.buildMI(get(ARMGenInstrNames.tPOP), dl);
    addDefaultPred(mib);

    boolean numRegs = false;
    for (int i = csi.size(); i != 0; --i) {
      int reg = csi.get(i-1).getReg();
      if (reg == ARMGenRegisterNames.LR) {
        if (isVarArg)
          continue;

        reg = ARMGenRegisterNames.PC;
        mib.getMInstr().setDesc(get(ARMGenInstrNames.tPOP_RET));
        mbb.remove(pos);
      }
      mib.addReg(reg, MachineInstrBuilder.getDefRegState(true));
      numRegs = true;
    }

    if (numRegs)
      mbb.insert(pos, mib.getMInstr());
    return true;
  }

  @Override
  public boolean copyPhysReg(MachineBasicBlock mbb,
                             int insertPos,
                             int dstReg,
                             int srcReg,
                             boolean isKill) {
    Util.assertion(ARMGenRegisterInfo.GPRRegisterClass.contains(dstReg) &&
        ARMGenRegisterInfo.GPRRegisterClass.contains(srcReg), "Thumb1 can only copy GPR registers");

    addDefaultPred(MachineInstrBuilder.buildMI(mbb, insertPos, new DebugLoc(),
        get(ARMGenInstrNames.tMOVr), dstReg).addReg(srcReg, getKillRegState(isKill)));
    return true;
  }

  @Override
  public void storeRegToStackSlot(MachineBasicBlock mbb,
                                  int pos, int srcReg,
                                  boolean isKill,
                                  int frameIndex,
                                  MCRegisterClass rc) {
    Util.assertion(rc == ARMGenRegisterInfo.tGPRRegisterClass ||
            (TargetRegisterInfo.isPhysicalRegister(srcReg) && isARMLowRegister(srcReg)),
        "Unknown register class");

    if (rc == ARMGenRegisterInfo.tGPRRegisterClass ||
        (TargetRegisterInfo.isPhysicalRegister(srcReg) && isARMLowRegister(srcReg))) {
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
      addDefaultPred(MachineInstrBuilder.buildMI(mbb, pos, dl, get(ARMGenInstrNames.tSTRspi))
          .addReg(srcReg, getKillRegState(isKill))
          .addFrameIndex(frameIndex).addImm(0).addMemOperand(mmo));
    }
  }

  @Override
  public void loadRegFromStackSlot(MachineBasicBlock mbb,
                                   int pos,
                                   int destReg,
                                   int frameIndex,
                                   MCRegisterClass rc) {
    Util.assertion(rc == ARMGenRegisterInfo.tGPRRegisterClass ||
            (TargetRegisterInfo.isPhysicalRegister(destReg) && isARMLowRegister(destReg)),
        "Unknown register class");

    if (rc == ARMGenRegisterInfo.tGPRRegisterClass ||
        (TargetRegisterInfo.isPhysicalRegister(destReg) && isARMLowRegister(destReg))) {
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
      addDefaultPred(MachineInstrBuilder.buildMI(mbb, pos, dl, get(ARMGenInstrNames.tLDRspi), destReg)
          .addFrameIndex(frameIndex).addImm(0).addMemOperand(mmo));
    }
  }

  private static int emitSPUpdate(MachineBasicBlock mbb,
                           int mbbi,
                           ARMInstrInfo tii,
                           DebugLoc dl,
                           Thumb1RegisterInfo tri,
                           int numBytes) {
    return ARMRegisterInfo.emitThumbRegPlusImmediate(mbb, mbbi, dl, ARMGenRegisterNames.SP,
        ARMGenRegisterNames.SP, numBytes, tii, tri);
  }

  @Override
  public void eliminateCallFramePseudoInstr(MachineFunction mf, MachineInstr old) {
    TargetSubtarget subtarget = mf.getTarget().getSubtarget();
    TargetFrameLowering tfl = subtarget.getFrameLowering();
    MachineBasicBlock mbb = old.getParent();
    int toDelete = old.getIndexInMBB();

    if (!tfl.hasReservedCallFrame(mf)) {
      // If we have alloca, convert as follows:
      // ADJCALLSTACKDOWN -> sub, sp, sp, amount
      // ADJCALLSTACKUP   -> add, sp, sp, amount
      DebugLoc dl = old.getDebugLoc();
      int amount = (int) old.getOperand(0).getImm();
      if (amount != 0) {
        int align = tfl.getStackAlignment();
        amount = (amount + align - 1) /align * align;
        int opc = old.getOpcode();
        if (opc == ARMGenInstrNames.tADJCALLSTACKDOWN || opc == ARMGenInstrNames.ADJCALLSTACKDOWN) {
          toDelete = emitSPUpdate(mbb, toDelete, this, dl, (Thumb1RegisterInfo) subtarget.getRegisterInfo(), -amount);
        }
        else {
          Util.assertion(opc == ARMGenInstrNames.tADJCALLSTACKUP || opc == ARMGenInstrNames.ADJCALLSTACKUP);
          toDelete = emitSPUpdate(mbb, toDelete, this, dl, (Thumb1RegisterInfo) subtarget.getRegisterInfo(), amount);
        }
      }
    }
    mbb.remove(toDelete);
  }
}
