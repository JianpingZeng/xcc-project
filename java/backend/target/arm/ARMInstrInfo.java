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
import backend.mc.MCRegisterClass;
import backend.target.TargetInstrInfo;
import backend.target.TargetInstrInfoImpl;
import backend.target.TargetRegisterInfo;
import tools.Util;

import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.codegen.MachineInstrBuilder.getKillRegState;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public abstract class ARMInstrInfo  extends TargetInstrInfoImpl {
  private ARMTargetMachine tm;
  protected ARMInstrInfo(ARMTargetMachine tm) {
    super(ARMGenInstrNames.ADJCALLSTACKDOWN, ARMGenInstrNames.ADJCALLSTACKUP);
    this.tm = tm;
  }

  public static TargetInstrInfo createARMInstrInfo(ARMTargetMachine tm) {
    return new ARMGenInstrInfo(tm);
  }

  @Override
  public void insertNoop(MachineBasicBlock mbb, int pos) {

  }

  @Override
  public boolean copyPhysReg(MachineBasicBlock mbb,
                             int insertPos,
                             int dstReg,
                             int srcReg,
                             MCRegisterClass dstRC,
                             MCRegisterClass srcRC) {
    DebugLoc dl = DebugLoc.getUnknownLoc();
    if (insertPos != mbb.size())
      dl = mbb.getInstAt(insertPos).getDebugLoc();

    boolean gprDest = ARMGenRegisterInfo.GPRRegisterClass.contains(dstReg);
    boolean gprSrc = ARMGenRegisterInfo.GPRRegisterClass.contains(srcReg);

    if (gprDest && gprSrc) {
      int opc = ARMGenInstrNames.MOVr;
      buildMI(mbb, insertPos, dl, get(opc), dstReg).addReg(srcReg);
      return true;
    }

    boolean sprDst = ARMGenRegisterInfo.SPRRegisterClass.contains(dstReg);
    boolean sprSrc = ARMGenRegisterInfo.SPRRegisterClass.contains(srcReg);

    int opc = 0;
    if (sprDst && sprSrc)
      opc =  ARMGenInstrNames.VMOVS;
    else if (gprDest && sprSrc)
      opc = ARMGenInstrNames.VMOVRS;
    else if (gprSrc && sprDst)
      opc = ARMGenInstrNames.VMOVSR;
    else if (ARMGenRegisterInfo.DPRRegisterClass.contains(dstReg) &&
        ARMGenRegisterInfo.DPRRegisterClass.contains(srcReg)) {
      opc = ARMGenInstrNames.VMOVD;
    }
    else if (ARMGenRegisterInfo.QPRRegisterClass.contains(dstReg) &&
        ARMGenRegisterInfo.QPRRegisterClass.contains(srcReg)) {
      opc = ARMGenInstrNames.VORRq;
    }

    if (opc != 0) {
      MachineInstrBuilder mib = buildMI(mbb, insertPos, dl, get(opc), dstReg).addReg(srcReg);
      if (opc == ARMGenInstrNames.VORRq)
        mib.addReg(srcReg);
      return true;
    }

    // Generate instructions for VMOVQQ and VMOVQQQQ pseudos in place.
    if ((ARMGenRegisterInfo.QQPRRegisterClass.contains(dstReg) &&
        ARMGenRegisterInfo.QQPRRegisterClass.contains(srcReg)) ||
        (ARMGenRegisterInfo.QQQQPRRegisterClass.contains(dstReg) &&
            ARMGenRegisterInfo.QQQQPRRegisterClass.contains(srcReg))) {
      TargetRegisterInfo tri = tm.getSubtarget().getRegisterInfo();
      int endSubReg = (ARMGenRegisterInfo.QQPRRegisterClass.contains(dstReg) &&
          ARMGenRegisterInfo.QQPRRegisterClass.contains(srcReg)) ? ARMGenRegisterInfo.qsub_1 : ARMGenRegisterInfo.qsub_3;
      for (int i = ARMGenRegisterInfo.qsub_0, e = endSubReg + 1; i < e; ++i) {
        int dest = tri.getSubReg(dstReg, i);
        int src = tri.getSubReg(dstReg, i);
        MachineInstrBuilder mib = buildMI(mbb, insertPos, dl, get(ARMGenInstrNames.VORRq))
            .addReg(dest, MachineOperand.RegState.Define)
            .addReg(src)
            .addReg(src);
        if (i == endSubReg) {
          mib.addReg(dstReg, MachineOperand.RegState.ImplicitDefine);
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public void storeRegToStackSlot(MachineBasicBlock mbb,
                                  int pos,
                                  int srcReg,
                                  boolean isKill,
                                  int frameIndex,
                                  MCRegisterClass rc) {
    DebugLoc dl = new DebugLoc();
    if (pos != mbb.size())
      dl = mbb.getInstAt(pos).getDebugLoc();
    MachineFunction mf = mbb.getParent();
    MachineFrameInfo mfi = mf.getFrameInfo();
    int align = mfi.getObjectAlignment(frameIndex);

    MachineMemOperand mmo = new MachineMemOperand(PseudoSourceValue.getFixedStack(frameIndex),
        MachineMemOperand.MOStore, 0, mfi.getObjectSize(frameIndex), align);

    ARMRegisterInfo tri = tm.getRegisterInfo();
    switch (tri.getRegSize(rc)/8) {
      case 4:
        if (ARMGenRegisterInfo.GPRRegisterClass.hasSubClassEq(rc))
          addDefaultPred(buildMI(mbb, pos, dl, get(ARMGenInstrNames.STRi12))
              .addReg(srcReg, getKillRegState(isKill))
              .addFrameIndex(frameIndex).addImm(0).addMemOperand(mmo));
        else if (ARMGenRegisterInfo.SPRRegisterClass.hasSubClassEq(rc))
          addDefaultPred(buildMI(mbb, pos, dl, get(ARMGenInstrNames.VSTRS))
              .addReg(srcReg, getKillRegState(isKill))
              .addFrameIndex(frameIndex).addImm(0).addMemOperand(mmo));
        else
          Util.shouldNotReachHere("Unknown register class!");
        break;
      case 8:
        if (ARMGenRegisterInfo.DPRRegisterClass.hasSubClassEq(rc))
          addDefaultPred(buildMI(mbb, pos, dl, get(ARMGenInstrNames.VSTRD))
              .addReg(srcReg, getKillRegState(isKill))
              .addFrameIndex(frameIndex).addImm(0).addMemOperand(mmo));
        else
          Util.shouldNotReachHere("Unknown register class!");
        break;
      case 16:
        if (ARMGenRegisterInfo.QPRRegisterClass.hasSubClassEq(rc)) {
          if (align >= 16 && tri.needsStackRealignment(mf)) {
            addDefaultPred(buildMI(mbb, pos, dl, get(ARMGenInstrNames.VST1q64Pseudo))
                .addReg(srcReg, getKillRegState(isKill))
                .addFrameIndex(frameIndex).addImm(16).addMemOperand(mmo));
          }
          else {
            addDefaultPred(buildMI(mbb, pos, dl, get(ARMGenInstrNames.VSTMDIA))
                .addReg(srcReg, getKillRegState(isKill))
                .addFrameIndex(frameIndex).addMemOperand(mmo));
          }
        }
        else
          Util.shouldNotReachHere("Unknown register class!");
        break;
      case 32:
        if (ARMGenRegisterInfo.QQPRRegisterClass.hasSubClassEq(rc)) {
          if (align >= 16 && tri.canRealignStack(mf)) {
            addDefaultPred(buildMI(mbb, pos, dl, get(ARMGenInstrNames.VST1q64Pseudo))
                .addReg(srcReg, getKillRegState(isKill))
                .addFrameIndex(frameIndex).addImm(16).addMemOperand(mmo));
          }
          else {
            MachineInstrBuilder mib = addDefaultPred(buildMI(mbb, pos, dl, get(ARMGenInstrNames.VSTMDIA))
                .addReg(srcReg, getKillRegState(isKill))
                .addFrameIndex(frameIndex).addMemOperand(mmo));
            mib = addDReg(mib, srcReg, ARMGenRegisterInfo.dsub_0, getKillRegState(isKill), tri);
            mib = addDReg(mib, srcReg, ARMGenRegisterInfo.dsub_1, 0, tri);
            mib = addDReg(mib, srcReg, ARMGenRegisterInfo.dsub_2, 0, tri);
            mib = addDReg(mib, srcReg, ARMGenRegisterInfo.dsub_3, 0, tri);
          }
        }
        else
          Util.shouldNotReachHere("Unknown register class!");
        break;
      case 64:
        if (ARMGenRegisterInfo.QQQQPRRegisterClass.hasSubClassEq(rc)) {
          MachineInstrBuilder mib = addDefaultPred(buildMI(mbb, pos, dl, get(ARMGenInstrNames.VSTMDIA))
              .addReg(srcReg, getKillRegState(isKill))
              .addFrameIndex(frameIndex).addMemOperand(mmo));
          mib = addDReg(mib, srcReg, ARMGenRegisterInfo.dsub_0, getKillRegState(isKill), tri);
          mib = addDReg(mib, srcReg, ARMGenRegisterInfo.dsub_1, 0, tri);
          mib = addDReg(mib, srcReg, ARMGenRegisterInfo.dsub_2, 0, tri);
          mib = addDReg(mib, srcReg, ARMGenRegisterInfo.dsub_3, 0, tri);
          mib = addDReg(mib, srcReg, ARMGenRegisterInfo.dsub_4, 0, tri);
          mib = addDReg(mib, srcReg, ARMGenRegisterInfo.dsub_5, 0, tri);
          mib = addDReg(mib, srcReg, ARMGenRegisterInfo.dsub_6, 0, tri);
          mib = addDReg(mib, srcReg, ARMGenRegisterInfo.dsub_7, 0, tri);
        }
        else
          Util.shouldNotReachHere("Unknown register class!");
        break;
      default:
        Util.shouldNotReachHere("Unknown register class!");
    }
  }

  private static MachineInstrBuilder addDReg(MachineInstrBuilder mib,
                                             int reg, int subIdx,
                                             int state, TargetRegisterInfo tri) {
    if (subIdx == 0)
      return mib.addReg(reg, state);

    if (TargetRegisterInfo.isPhysicalRegister(reg))
      return mib.addReg(tri.getSubReg(reg, subIdx), state);
    return mib.addReg(reg, state, subIdx);
  }

  @Override
  public void loadRegFromStackSlot(MachineBasicBlock mbb,
                                   int pos,
                                   int destReg,
                                   int frameIndex,
                                   MCRegisterClass rc) {
    DebugLoc dl = new DebugLoc();
    if (pos != mbb.size())
      dl = mbb.getInstAt(pos).getDebugLoc();
    MachineFunction mf = mbb.getParent();
    MachineFrameInfo mfi = mf.getFrameInfo();
    int align = mfi.getObjectAlignment(frameIndex);

    MachineMemOperand mmo = new MachineMemOperand(PseudoSourceValue.getFixedStack(frameIndex),
        MachineMemOperand.MOLoad, 0, mfi.getObjectSize(frameIndex), align);

    ARMRegisterInfo tri = tm.getRegisterInfo();
    switch (tri.getRegSize(rc)/8) {
      case 4:
        if (ARMGenRegisterInfo.GPRRegisterClass.hasSubClassEq(rc))
          addDefaultPred(buildMI(mbb, pos, dl, get(ARMGenInstrNames.LDRi12), destReg)
              .addFrameIndex(frameIndex).addImm(0).addMemOperand(mmo));
        else if (ARMGenRegisterInfo.SPRRegisterClass.hasSubClassEq(rc))
          addDefaultPred(buildMI(mbb, pos, dl, get(ARMGenInstrNames.VLDRS), destReg)
              .addFrameIndex(frameIndex).addImm(0).addMemOperand(mmo));
        else
          Util.shouldNotReachHere("Unknown register class!");
        break;
      case 8:
        if (ARMGenRegisterInfo.DPRRegisterClass.hasSubClassEq(rc))
          addDefaultPred(buildMI(mbb, pos, dl, get(ARMGenInstrNames.VLDRD), destReg)
              .addFrameIndex(frameIndex).addImm(0).addMemOperand(mmo));
        else
          Util.shouldNotReachHere("Unknown register class!");
        break;
      case 16:
        if (ARMGenRegisterInfo.QPRRegisterClass.hasSubClassEq(rc)) {
          if (align >= 16 && tri.needsStackRealignment(mf)) {
            addDefaultPred(buildMI(mbb, pos, dl, get(ARMGenInstrNames.VLD1q64Pseudo), destReg)
                .addFrameIndex(frameIndex).addImm(16).addMemOperand(mmo));
          }
          else {
            addDefaultPred(buildMI(mbb, pos, dl, get(ARMGenInstrNames.VLDMDIA), destReg)
                .addFrameIndex(frameIndex).addMemOperand(mmo));
          }
        }
        else
          Util.shouldNotReachHere("Unknown register class!");
        break;
      case 32:
        if (ARMGenRegisterInfo.QQPRRegisterClass.hasSubClassEq(rc)) {
          if (align >= 16 && tri.canRealignStack(mf)) {
            addDefaultPred(buildMI(mbb, pos, dl, get(ARMGenInstrNames.VLD1q64Pseudo), destReg)
                .addFrameIndex(frameIndex).addImm(16).addMemOperand(mmo));
          }
          else {
            MachineInstrBuilder mib = addDefaultPred(buildMI(mbb, pos, dl, get(ARMGenInstrNames.VLDMDIA), destReg)
                .addFrameIndex(frameIndex).addMemOperand(mmo));
            mib = addDReg(mib, destReg, ARMGenRegisterInfo.dsub_0, MachineOperand.RegState.Define, tri);
            mib = addDReg(mib, destReg, ARMGenRegisterInfo.dsub_1, MachineOperand.RegState.Define, tri);
            mib = addDReg(mib, destReg, ARMGenRegisterInfo.dsub_2, MachineOperand.RegState.Define, tri);
            mib = addDReg(mib, destReg, ARMGenRegisterInfo.dsub_3, MachineOperand.RegState.Define, tri);
            mib.addReg(destReg, MachineOperand.RegState.ImplicitDefine);
          }
        }
        else
          Util.shouldNotReachHere("Unknown register class!");
        break;
      case 64:
        if (ARMGenRegisterInfo.QQQQPRRegisterClass.hasSubClassEq(rc)) {
          MachineInstrBuilder mib = addDefaultPred(buildMI(mbb, pos, dl, get(ARMGenInstrNames.VLDMDIA))
              .addFrameIndex(frameIndex).addMemOperand(mmo));
          mib = addDReg(mib, destReg, ARMGenRegisterInfo.dsub_0, MachineOperand.RegState.Define, tri);
          mib = addDReg(mib, destReg, ARMGenRegisterInfo.dsub_1, MachineOperand.RegState.Define, tri);
          mib = addDReg(mib, destReg, ARMGenRegisterInfo.dsub_2, MachineOperand.RegState.Define, tri);
          mib = addDReg(mib, destReg, ARMGenRegisterInfo.dsub_3, MachineOperand.RegState.Define, tri);
          mib = addDReg(mib, destReg, ARMGenRegisterInfo.dsub_4, MachineOperand.RegState.Define, tri);
          mib = addDReg(mib, destReg, ARMGenRegisterInfo.dsub_5, MachineOperand.RegState.Define, tri);
          mib = addDReg(mib, destReg, ARMGenRegisterInfo.dsub_6, MachineOperand.RegState.Define, tri);
          mib = addDReg(mib, destReg, ARMGenRegisterInfo.dsub_7, MachineOperand.RegState.Define, tri);
          mib.addReg(destReg, MachineOperand.RegState.ImplicitDefine);
        }
        else
          Util.shouldNotReachHere("Unknown register class!");
        break;
      default:
        Util.shouldNotReachHere("Unknown register class!");
    }
  }

  private MachineInstrBuilder addDefaultPred(MachineInstrBuilder mib) {
    return mib.addImm(ARMCC.CondCodes.AL.ordinal()).addReg(0);
  }
  private MachineInstrBuilder addDefaultCC(MachineInstrBuilder mib) {
    return mib.addReg(0);
  }
}
