package backend.target.mips;
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

import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineInstr;
import backend.codegen.MachineInstrBuilder;
import backend.codegen.MachineOperand;
import backend.debug.DebugLoc;
import backend.mc.MCRegisterClass;
import backend.target.TargetInstrInfoImpl;
import backend.target.TargetRegisterInfo;
import tools.OutRef;
import tools.Util;

import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.codegen.MachineInstrBuilder.getKillRegState;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class MipsInstrInfo extends TargetInstrInfoImpl {
  private MipsSubtarget subtarget;
  public MipsInstrInfo(MipsSubtarget subtarget) {
    super(MipsGenInstrNames.ADJCALLSTACKDOWN,
          MipsGenInstrNames.ADJCALLSTACKUP);
    this.subtarget = subtarget;
  }

  @Override
  public void insertNoop(MachineBasicBlock mbb, int pos) {
    buildMI(mbb, pos, new DebugLoc(), get(MipsGenInstrNames.NOP));
  }

  @Override
  public boolean copyPhysReg(MachineBasicBlock mbb,
                             int insertPos,
                             int dstReg,
                             int srcReg,
                             boolean isKill) {
    Util.assertion(TargetRegisterInfo.isPhysicalRegister(srcReg) &&
        TargetRegisterInfo.isPhysicalRegister(dstReg), "copy virtual register should be coped with COPY!");

    // Unknown debug loc.
    DebugLoc dl = new DebugLoc();
    if (insertPos != mbb.size()) dl = mbb.getInstAt(insertPos).getDebugLoc();

    int opc = 0, zeroReg = 0;
    if (MipsGenRegisterInfo.CPURegsRegisterClass.contains(dstReg)) {
      // copy to CPU register.
      if (MipsGenRegisterInfo.CPURegsRegisterClass.contains(srcReg)) {
        // copy value from CPU register to CPU register.
        opc = MipsGenInstrNames.ADDu;
        zeroReg = MipsGenRegisterNames.ZERO;
      }
      else if (MipsGenRegisterInfo.CCRRegisterClass.contains(srcReg))
        opc = MipsGenInstrNames.CFC1;
      else if (MipsGenRegisterInfo.FGR32RegisterClass.contains(srcReg))
        opc = MipsGenInstrNames.MFC1;
      else if (MipsGenRegisterInfo.HILORegisterClass.contains(srcReg)) {
        opc = srcReg == MipsGenRegisterNames.HI ? MipsGenInstrNames.MFHI : MipsGenInstrNames.MFLO;
      }
    } else if (MipsGenRegisterInfo.CPURegsRegisterClass.contains(srcReg)) {
      // copy from CPU register.
      if (MipsGenRegisterInfo.CCRRegisterClass.contains(dstReg))
        opc = MipsGenInstrNames.CTC1;
      else if (MipsGenRegisterInfo.FGR32RegisterClass.contains(dstReg))
        opc = MipsGenInstrNames.MTC1;
      else if (MipsGenRegisterInfo.HILORegisterClass.contains(dstReg)) {
        opc = dstReg == MipsGenRegisterNames.HI ? MipsGenInstrNames.MTHI : MipsGenInstrNames.MTLO;
      }
    } else if (MipsGenRegisterInfo.FGR32RegisterClass.contains(dstReg, srcReg))
      opc = MipsGenInstrNames.FMOV_S;
    else if (MipsGenRegisterInfo.AFGR64RegisterClass.contains(dstReg, srcReg))
      opc = MipsGenInstrNames.FMOV_D32;
    else if (MipsGenRegisterInfo.FGR64RegisterClass.contains(dstReg, srcReg))
      opc = MipsGenInstrNames.FMOV_D64;

    if (opc == 0)
      // can't copy.
      return false;

    MachineInstrBuilder mib = buildMI(mbb, insertPos, dl, get(opc));
    if (dstReg != 0)
      mib.addReg(dstReg, MachineOperand.RegState.Define);
    if (srcReg != 0)
      mib.addReg(srcReg, getKillRegState(isKill));
    if (zeroReg != 0)
      mib.addReg(zeroReg);
    return true;
  }

  @Override
  public boolean isMoveInstr(MachineInstr mi, OutRef<Integer> srcReg,
                             OutRef<Integer> destReg,
                             OutRef<Integer> srcSubIdx,
                             OutRef<Integer> destSubIdx) {
    // no sub-registers.
    srcSubIdx.set(0);
    destSubIdx.set(0);

    // addu $dst, $src, $zero || addu $dst, $zero, $src
    // or   $dst, $src, $zero || or   $dst, $zero, $src
    if (mi.getOpcode() == MipsGenInstrNames.ADDu || mi.getOpcode() == MipsGenInstrNames.OR) {
      if (mi.getOperand(1).getReg() == MipsGenRegisterNames.ZERO) {
        destReg.set(mi.getOperand(0).getReg());
        srcReg.set(mi.getOperand(2).getReg());
        return true;
      }
      else if (mi.getOperand(2).getReg() == MipsGenRegisterNames.ZERO) {
        destReg.set(mi.getOperand(0).getReg());
        srcReg.set(mi.getOperand(1).getReg());
        return true;
      }
    }

    // mov $fpDst, $fpSrc
    // mfc $gpDst, $fpSrc
    // mtc $fpDst, $gpSrc
    if (mi.getOpcode() == MipsGenInstrNames.FMOV_S ||
        mi.getOpcode() == MipsGenInstrNames.FMOV_D32 ||
        mi.getOpcode() == MipsGenInstrNames.MFC1 ||
        mi.getOpcode() == MipsGenInstrNames.MTC1 ||
        mi.getOpcode() == MipsGenInstrNames.MOVCCRToCCR) {
      destReg.set(mi.getOperand(0).getReg());
      srcReg.set(mi.getOperand(1).getReg());
      return true;
    }

    // addiu $dst, $src, 0
    if (mi.getOpcode() == MipsGenInstrNames.ADDiu) {
      if (mi.getOperand(2).isImm() && mi.getOperand(2).getImm() == 0) {
        destReg.set(mi.getOperand(0).getReg());
        srcReg.set(mi.getOperand(1).getReg());
        return true;
      }
    }
    return false;
  }

  @Override
  public int isLoadFromStackSlot(MachineInstr mi, OutRef<Integer> frameIndex) {
    switch (mi.getOpcode()) {
      case MipsGenInstrNames.LW:
      case MipsGenInstrNames.LWC1:
      case MipsGenInstrNames.LDC1:
      case MipsGenInstrNames.LW_P8:
      case MipsGenInstrNames.LD:
      case MipsGenInstrNames.LD_P8:
      case MipsGenInstrNames.LWC1_P8:
      case MipsGenInstrNames.LDC164:
      case MipsGenInstrNames.LDC164_P8: {
        if (mi.getOperand(1).isFrameIndex() && // frame index
            mi.getOperand(2).isImm() && // imm is a zero
            mi.getOperand(2).getImm() == 0) {
          frameIndex.set(mi.getOperand(1).getIndex());
          return mi.getOperand(0).getReg();
        }
      }
    }
    return 0;
  }

  @Override
  public int isStoreToStackSlot(MachineInstr mi, OutRef<Integer> frameIndex) {
    switch (mi.getOpcode()) {
      case MipsGenInstrNames.SW:
      case MipsGenInstrNames.SW_P8:
      case MipsGenInstrNames.SD:
      case MipsGenInstrNames.SD_P8:
      case MipsGenInstrNames.SWC1:
      case MipsGenInstrNames.SWC1_P8:
      case MipsGenInstrNames.SDC1:
      case MipsGenInstrNames.SDC164:
      case MipsGenInstrNames.SDC164_P8:
        if (mi.getOperand(1).isFrameIndex() && // frame index
            mi.getOperand(2).isImm() && // imm is a zero
            mi.getOperand(2).getImm() == 0) {
          frameIndex.set(mi.getOperand(1).getIndex());
          return mi.getOperand(0).getReg();
        }
    }
    return 0;
  }

  @Override
  public void storeRegToStackSlot(MachineBasicBlock mbb, int pos,
                                  int srcReg, boolean isKill,
                                  int frameIndex, MCRegisterClass rc) {
    DebugLoc dl = new DebugLoc();
    if (pos != mbb.size())
      dl = mbb.getInstAt(pos).getDebugLoc();

    // determine the opcode according to the RC.
    int opc = 0;
    if (rc.equals(MipsGenRegisterInfo.CPURegsRegisterClass))
      opc = subtarget.isABI_N64() ? MipsGenInstrNames.SW_P8 : MipsGenInstrNames.SW;
    else if (rc.equals(MipsGenRegisterInfo.CPU64RegsRegisterClass))
      opc = subtarget.isABI_N64() ? MipsGenInstrNames.SD_P8 : MipsGenInstrNames.SD;
    else if (rc.equals(MipsGenRegisterInfo.FGR32RegisterClass))
      opc = subtarget.isABI_N64() ? MipsGenInstrNames.SWC1_P8 : MipsGenInstrNames.SWC1;
    else if (rc.equals(MipsGenRegisterInfo.AFGR64RegisterClass))
      opc = MipsGenInstrNames.SDC1;
    else if (rc.equals(MipsGenRegisterInfo.FGR64RegisterClass))
      opc = subtarget.isABI_N64() ? MipsGenInstrNames.SDC164_P8 : MipsGenInstrNames.SDC164;
    else
      Util.assertion("register class is not handled!");

    buildMI(mbb, pos, dl, get(opc)).addReg(srcReg, getKillRegState(isKill)).addFrameIndex(frameIndex).addImm(0);
  }

  @Override
  public void loadRegFromStackSlot(MachineBasicBlock mbb, int pos,
                                   int destReg, int frameIndex,
                                   MCRegisterClass rc) {
    DebugLoc dl = new DebugLoc();
    if (pos != mbb.size())
      dl = mbb.getInstAt(pos).getDebugLoc();

    // determine the opcode according to the RC.
    int opc = 0;
    if (rc.equals(MipsGenRegisterInfo.CPURegsRegisterClass))
      opc = subtarget.isABI_N64() ? MipsGenInstrNames.LW_P8 : MipsGenInstrNames.LW;
    else if (rc.equals(MipsGenRegisterInfo.CPU64RegsRegisterClass))
      opc = subtarget.isABI_N64() ? MipsGenInstrNames.LD_P8 : MipsGenInstrNames.LD;
    else if (rc.equals(MipsGenRegisterInfo.FGR32RegisterClass))
      opc = subtarget.isABI_N64() ? MipsGenInstrNames.LWC1_P8 : MipsGenInstrNames.LWC1;
    else if (rc.equals(MipsGenRegisterInfo.AFGR64RegisterClass))
      opc = MipsGenInstrNames.LDC1;
    else if (rc.equals(MipsGenRegisterInfo.FGR64RegisterClass))
      opc = subtarget.isABI_N64() ? MipsGenInstrNames.LDC164_P8 : MipsGenInstrNames.LDC164;
    else
      Util.assertion("register class is not handled!");

    buildMI(mbb, pos, dl, get(opc), destReg).addFrameIndex(frameIndex).addImm(0);
  }
}
