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
import backend.mc.MCAsmInfo;
import backend.mc.MCInstrDesc;
import backend.mc.MCRegisterClass;
import backend.target.TargetInstrInfo;
import backend.target.TargetInstrInfoImpl;
import backend.target.TargetOpcode;
import backend.target.TargetRegisterInfo;
import tools.OutRef;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static backend.codegen.MachineInstrBuilder.*;

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
  public boolean copyPhysReg(MachineBasicBlock mbb,
                             int insertPos,
                             int dstReg,
                             int srcReg,
                             MCRegisterClass dstRC,
                             MCRegisterClass srcRC) {
    Util.assertion(TargetRegisterInfo.isPhysicalRegister(srcReg) &&
        TargetRegisterInfo.isPhysicalRegister(dstReg), "copy virtual register should be coped with COPY!");
    DebugLoc dl = DebugLoc.getUnknownLoc();
    if (insertPos != mbb.size())
      dl = mbb.getInstAt(insertPos).getDebugLoc();

    boolean gprDest = ARMGenRegisterInfo.GPRRegisterClass.contains(dstReg);
    boolean gprSrc = ARMGenRegisterInfo.GPRRegisterClass.contains(srcReg);

    if (gprDest && gprSrc) {
      int opc = ARMGenInstrNames.MOVr;
      MachineInstrBuilder mib = buildMI(mbb, insertPos, dl, get(opc), dstReg).addReg(srcReg);
      addDefaultPred(mib);
      addDefaultCC(mib);
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
    switch (tri.getRegSize(rc)) {
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
    switch (tri.getRegSize(rc)) {
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

  @Override
  public void eliminateCallFramePseudoInstr(MachineFunction mf, MachineInstr old) {
    Util.assertion(old.getOpcode() == ARMGenInstrNames.ADJCALLSTACKDOWN ||
        old.getOpcode() == ARMGenInstrNames.ADJCALLSTACKUP ||
        old.getOpcode() == ARMGenInstrNames.tADJCALLSTACKDOWN ||
        old.getOpcode() == ARMGenInstrNames.tADJCALLSTACKUP);

    if (!tm.getFrameLowering().hasReservedCallFrame(mf)) {
      // If we have alloca, convert as follows:
      // ADJCALLSTACKDOWN -> sub, sp, sp, amount
      // ADJCALLSTACKUP   -> add, sp, sp, amount
      DebugLoc dl = old.getDebugLoc();
      int oldOpcode = old.getOpcode();
      // get the immediate.
      int imm = (int) old.getOperand(0).getImm();
      if (imm != 0) {
        ARMFunctionInfo afi = (ARMFunctionInfo) mf.getInfo();
        Util.assertion(!afi.isThumb1OnlyFunction());
        boolean isARM = !afi.isThumbFunction();

        int predIdx = old.findFirstPredOperandIdx();
        ARMCC.CondCodes cc = predIdx == -1 ? ARMCC.CondCodes.AL : ARMCC.CondCodes.values()[predIdx];
        if (oldOpcode == ARMGenInstrNames.ADJCALLSTACKDOWN ||
            oldOpcode == ARMGenInstrNames.tADJCALLSTACKDOWN) {
          // PredReg is the 2'nd operand.
          int predReg = old.getOperand(2).getReg();
          ARMFrameLowering.emitSPUpdate(isARM, old.getParent(), old.getIndexInMBB(), dl, tm.getInstrInfo(), -imm, cc, predReg);
        }
        else {
          // PredReg is the 3'rd operand.
          int predReg = old.getOperand(3).getReg();
          ARMFrameLowering.emitSPUpdate(isARM, old.getParent(), old.getIndexInMBB(), dl, tm.getInstrInfo(), imm, cc, predReg);
        }
      }
    }
    old.removeFromParent();
  }

  private int emitPopInst(MachineBasicBlock mbb, int mbbi, List<CalleeSavedInfo> csi,
                          int ldmOpc, int ldOpc, boolean isVarArg, boolean noCap) {
    MachineFunction mf = mbb.getParent();
    MachineInstr mi = mbb.getInstAt(mbbi);
    int retOpcode = mi.getOpcode();
    ARMFunctionInfo afi = (ARMFunctionInfo) mf.getInfo();
    DebugLoc dl = mbbi != mbb.size() ? mi.getDebugLoc() : new DebugLoc();
    boolean isTailCall = retOpcode == ARMGenInstrNames.TCRETURNdi ||
        mi.getOpcode() == ARMGenInstrNames.TCRETURNdiND ||
        mi.getOpcode() == ARMGenInstrNames.TCRETURNri ||
        mi.getOpcode() == ARMGenInstrNames.TCRETURNriND;

    ArrayList<Integer> regs = new ArrayList<>();
    int i = csi.size();
    while (i != 0) {
      int lastReg = 0;
      boolean deleteReg = false;
      for (; i != 0; --i) {
        int reg = csi.get(i - 1).getReg();
        if (reg == ARMGenRegisterNames.LR && !isTailCall && !isVarArg && tm.getSubtarget().hasV5TOps()) {
          reg = ARMGenRegisterNames.PC;
          ldmOpc = afi.isThumbFunction() ? ARMGenInstrNames.t2LDMIA_RET : ARMGenInstrNames.LDMIA_RET;
          deleteReg = true;
        }

        if (noCap && lastReg != 0 && lastReg != reg - 1)
          break;
        lastReg = reg;
        regs.add(reg);
      }
      if (regs.isEmpty())
        continue;

      if (regs.size() > 1 || ldOpc == 0) {
        MachineInstrBuilder mib = addDefaultPred(buildMI(mbb, mbbi++, dl, get(ldmOpc), ARMGenRegisterNames.SP)
            .addReg(ARMGenRegisterNames.SP));
        regs.forEach(reg -> mib.addReg(reg, getDefRegState(true)));
        if (deleteReg) {
          mib.getMInstr().copyImplicitOps(mi);
          mi.removeFromParent();
          mi = mib.getMInstr();
        }
        mbbi = mbb.getIndexOf(mib.getMInstr());
      }
      else if (regs.size() == 1) {
        if (regs.get(0) == ARMGenRegisterNames.SP)
          regs.set(0, ARMGenRegisterNames.LR);

        MachineInstrBuilder mib = buildMI(mbb, mbbi++, dl, get(ldOpc), regs.get(0))
            .addReg(ARMGenRegisterNames.SP, getDefRegState(true))
            .addReg(ARMGenRegisterNames.SP);
        if (ldOpc == ARMGenInstrNames.LDR_POST_REG || ldOpc == ARMGenInstrNames.LDR_POST_IMM) {
          mib.addReg(0);
          mib.addImm(ARM_AM.getAM2Opc(ARM_AM.AddrOpc.add, 4, ARM_AM.ShiftOpc.no_shift));
        }
        else
          mib.addImm(4);
        addDefaultPred(mib);
      }
      regs.clear();
    }
    return mbbi;
  }

  @Override
  public boolean restoreCalleeSavedRegisters(MachineBasicBlock mbb, int pos, ArrayList<CalleeSavedInfo> csi) {
    if (csi.isEmpty()) return true;

    MachineFunction mf = mbb.getParent();
    ARMFunctionInfo afi = (ARMFunctionInfo) mf.getInfo();
    int ldmOpc = afi.isThumbFunction() ? ARMGenInstrNames.t2LDMIA_UPD : ARMGenInstrNames.LDMIA_UPD;
    int ldrOpc = afi.isThumbFunction() ? ARMGenInstrNames.t2LDR_POST : ARMGenInstrNames.LDR_POST_IMM;
    int fltOpc = ARMGenInstrNames.VLDMDIA_UPD;
    boolean isVarArg = mf.getFrameInfo().hasVarSizedObjects();
    List<CalleeSavedInfo> tmp;

    // load float callee saved register.
    tmp = csi.stream().filter(calleeSavedInfo -> isARMArea3Register(calleeSavedInfo.getReg(), tm.getSubtarget().isTargetDarwin())).collect(Collectors.toList());
    pos = emitPopInst(mbb, pos, tmp, fltOpc, 0, isVarArg, true);

    // load integer callee saved register area 2.
    tmp = csi.stream().filter(calleeSavedInfo -> isARMArea2Register(calleeSavedInfo.getReg(), tm.getSubtarget().isTargetDarwin())).collect(Collectors.toList());
    pos = emitPopInst(mbb, pos, tmp, ldmOpc, ldrOpc, isVarArg, false);

    // load integer callee saved register area 1.
    tmp = csi.stream().filter(calleeSavedInfo -> isARMArea1Register(calleeSavedInfo.getReg(), tm.getSubtarget().isTargetDarwin())).collect(Collectors.toList());
    pos = emitPopInst(mbb, pos, tmp, ldmOpc, ldrOpc, isVarArg, false);
    return true;
  }

  private static boolean isARMArea1Register(int reg, boolean isDarwin) {
    switch (reg) {
      case ARMGenRegisterNames.R0:
      case ARMGenRegisterNames.R1:
      case ARMGenRegisterNames.R2:
      case ARMGenRegisterNames.R3:
      case ARMGenRegisterNames.R4:
      case ARMGenRegisterNames.R5:
      case ARMGenRegisterNames.R6:
      case ARMGenRegisterNames.R7:
      case ARMGenRegisterNames.LR:
      case ARMGenRegisterNames.SP:
      case ARMGenRegisterNames.PC:
        return true;
      case ARMGenRegisterNames.R8:
      case ARMGenRegisterNames.R9:
      case ARMGenRegisterNames.R10:
      case ARMGenRegisterNames.R11:
        return !isDarwin;
      default:
        return false;
    }
  }

  private static boolean isARMArea2Register(int reg, boolean isDarwin) {
    switch (reg) {
      case ARMGenRegisterNames.R8:
      case ARMGenRegisterNames.R9:
      case ARMGenRegisterNames.R10:
      case ARMGenRegisterNames.R11:
        return isDarwin;
      default:
        return false;
    }
  }

  private static boolean isARMArea3Register(int reg, boolean isDarwin) {
    switch (reg) {
      case ARMGenRegisterNames.D8:
      case ARMGenRegisterNames.D9:
      case ARMGenRegisterNames.D10:
      case ARMGenRegisterNames.D11:
      case ARMGenRegisterNames.D12:
      case ARMGenRegisterNames.D13:
      case ARMGenRegisterNames.D14:
      case ARMGenRegisterNames.D15:
        return true;
      default:
        return false;
    }
  }

  private int emitPushInst(MachineBasicBlock mbb, int mbbi, List<CalleeSavedInfo> csi,
                           int strmOpc, int strOpc, boolean noCap, int miFlag) {
    DebugLoc dl = mbbi != mbb.size() ? mbb.getInstAt(mbbi).getDebugLoc() : new DebugLoc();
    MachineFunction mf = mbb.getParent();
    TargetInstrInfo tii = tm.getInstrInfo();

    ArrayList<Pair<Integer, Boolean>> regs = new ArrayList<>();
    int i = csi.size();
    while (i != 0) {
      int lastReg = 0;
      for (; i != 0; --i) {
        // use stm to push consecutive registers to the stack and leave str to cope with rest of registers.
        int reg = csi.get(i - 1).getReg();
        boolean isKill = true;
        // Add the callee-saved register as live-in unless it's LR and
        // @llvm.returnaddress is called. If LR is returned for
        // @llvm.returnaddress then it's already added to the function and
        // entry block live-in sets.
        if (isKill)
          mbb.addLiveIn(reg);

        if (noCap && lastReg != 0 && lastReg != reg - 1)
          break;
        lastReg = reg;
        regs.add(Pair.get(reg, isKill));
      }

      if (regs.isEmpty())
        continue;

      if (regs.size() > 1 || strOpc == 0) {
        MachineInstrBuilder mib = addDefaultPred(buildMI(mbb, mbbi++, dl, tii.get(strmOpc), ARMGenRegisterNames.SP)
            .addReg(ARMGenRegisterNames.SP).setMIFlags(miFlag));
        regs.forEach(pair -> mib.addReg(pair.first, getKillRegState(pair.second)));
      }
      else if (regs.size() == 1) {
        MachineInstrBuilder mib = buildMI(mbb, mbbi++, dl, tii.get(strOpc), ARMGenRegisterNames.SP)
            .addReg(regs.get(0).first, getKillRegState(regs.get(0).second))
            .addReg(ARMGenRegisterNames.SP).setMIFlags(miFlag)
            .addImm(-4);
        addDefaultPred(mib);
      }
      regs.clear();
    }
    return mbbi;
  }

  @Override
  public boolean spillCalleeSavedRegisters(MachineBasicBlock mbb, int pos, ArrayList<CalleeSavedInfo> csi) {
    if (csi.isEmpty()) return true;

    MachineFunction mf = mbb.getParent();
    ARMFunctionInfo afi = (ARMFunctionInfo) mf.getInfo();
    int pushOpc = afi.isThumbFunction() ? ARMGenInstrNames.t2STMDB_UPD : ARMGenInstrNames.STMDB_UPD;
    int pushOneOpc = afi.isThumbFunction() ? ARMGenInstrNames.t2STR_PRE : ARMGenInstrNames.STR_PRE_IMM;
    int fltOpc = ARMGenInstrNames.VSTMDDB_UPD;
    List<CalleeSavedInfo> tmp;

    // push integer callee saved register area 1.
    tmp = csi.stream().filter(calleeSavedInfo -> isARMArea1Register(calleeSavedInfo.getReg(), tm.getSubtarget().isTargetDarwin())).collect(Collectors.toList());
    pos = emitPushInst(mbb, pos, tmp, pushOpc, pushOneOpc, false, 0);

    // push integer callee saved register area 2.
    tmp = csi.stream().filter(calleeSavedInfo -> isARMArea2Register(calleeSavedInfo.getReg(), tm.getSubtarget().isTargetDarwin())).collect(Collectors.toList());
    pos = emitPushInst(mbb, pos, tmp, pushOpc, pushOneOpc, false, 0);

    // push float callee saved register.
    tmp = csi.stream().filter(calleeSavedInfo -> isARMArea3Register(calleeSavedInfo.getReg(), tm.getSubtarget().isTargetDarwin())).collect(Collectors.toList());
    pos = emitPushInst(mbb, pos, tmp, fltOpc, 0, true, 0);
    return true;
  }

  @Override
  public int getInstSizeInBytes(MachineInstr mi) {
    MachineBasicBlock mbb = mi.getParent();
    MachineFunction mf = mbb.getParent();
    MCAsmInfo mai = mf.getTarget().getMCAsmInfo();

    MCInstrDesc mcid = mi.getDesc();
    if (mi.getOpcode() == TargetOpcode.INLINEASM)
      return getInlineAsmLength(mi.getOperand(0).getSymbolName(), mai);
    if (mi.isLabel())
      return 0;
    int opc = mi.getOpcode();
    switch (opc) {
      case TargetOpcode.IMPLICIT_DEF:
      case TargetOpcode.KILL:
      case TargetOpcode.PROLOG_LABEL:
      case TargetOpcode.EH_LABEL:
      case TargetOpcode.DBG_VALUE:
        return 0;
      case ARMGenInstrNames.MOVi16_ga_pcrel:
      case ARMGenInstrNames.MOVTi16_ga_pcrel:
      case ARMGenInstrNames.t2MOVi16_ga_pcrel:
      case ARMGenInstrNames.t2MOVTi16_ga_pcrel:
        return 4;
      case ARMGenInstrNames.MOVi32imm:
      case ARMGenInstrNames.t2MOVi32imm:
        return 8;
      case ARMGenInstrNames.CONSTPOOL_ENTRY:
        // If this machine instr is a constant pool entry, its size is recorded as
        // operand #2.
        return (int) mi.getOperand(2).getImm();
      case ARMGenInstrNames.Int_eh_sjlj_longjmp:
        return 16;
      case ARMGenInstrNames.tInt_eh_sjlj_longjmp:
        return 10;
      case ARMGenInstrNames.Int_eh_sjlj_setjmp:
      case ARMGenInstrNames.Int_eh_sjlj_setjmp_nofp:
        return 20;
      case ARMGenInstrNames.tInt_eh_sjlj_setjmp:
      case ARMGenInstrNames.t2Int_eh_sjlj_setjmp:
      case ARMGenInstrNames.t2Int_eh_sjlj_setjmp_nofp:
        return 12;
      case ARMGenInstrNames.BR_JTr:
      case ARMGenInstrNames.BR_JTm:
      case ARMGenInstrNames.BR_JTadd:
      case ARMGenInstrNames.tBR_JTr:
      case ARMGenInstrNames.t2BR_JT:
      case ARMGenInstrNames.t2TBB_JT:
      case ARMGenInstrNames.t2TBH_JT: {
        // These are jumptable branches, i.e. a branch followed by an inlined
        // jumptable. The size is 4 + 4 * number of entries. For TBB, each
        // entry is one byte; TBH two byte each.
        int entrySize = opc == ARMGenInstrNames.t2TBB_JT ? 1 : (opc == ARMGenInstrNames.t2TBH_JT ? 2 : 4);
        int numOops = mi.getNumOperands();
        MachineOperand jtop = mi.getOperand(numOops - (mcid.isPredicable() ? 3 : 2));
        int jti = jtop.getIndex();
        MachineJumpTableInfo mjti = mf.getJumpTableInfo();
        Util.assertion(mjti != null);
        ArrayList<MachineJumpTableEntry> jt = mjti.getJumpTables();
        Util.assertion(jti < jt.size());

        int instSize = (opc == ARMGenInstrNames.tBR_JTr || opc == ARMGenInstrNames.t2BR_JT) ? 2 : 4;
        int numEntries = getNumJTEntries(jt, jti);
        if (opc == ARMGenInstrNames.t2TBB_JT && (numEntries & 1) != 0)
          ++numEntries;

        return numEntries * entrySize + instSize;
      }
      default:
        // otherwise, return zero for pseudo instruction.
        return 0;
    }
  }

  private static int getNumJTEntries(ArrayList<MachineJumpTableEntry> jt, int jti) {
    return jt.get(jti).mbbs.size();
  }

  @Override
  public int insertBranch(MachineBasicBlock mbb, MachineBasicBlock tbb, MachineBasicBlock fbb, ArrayList<MachineOperand> cond, DebugLoc dl) {
    return super.insertBranch(mbb, tbb, fbb, cond, dl);
  }

  @Override
  public boolean reverseBranchCondition(ArrayList<MachineOperand> cond) {
    return super.reverseBranchCondition(cond);
  }

  @Override
  public int removeBranch(MachineBasicBlock mbb) {
    return super.removeBranch(mbb);
  }

  @Override
  public boolean analyzeBranch(MachineBasicBlock mbb, MachineBasicBlock tbb, MachineBasicBlock fbb, ArrayList<MachineOperand> cond, boolean allowModify) {
    return super.analyzeBranch(mbb, tbb, fbb, cond, allowModify);
  }

  @Override
  public boolean predicateInstruction(MachineInstr mi, ArrayList<MachineOperand> pred) {
    return super.predicateInstruction(mi, pred);
  }

  @Override
  public boolean isPredicated(MachineInstr mi) {
    return super.isPredicated(mi);
  }

  @Override
  public int isLoadFromStackSlot(MachineInstr mi, OutRef<Integer> frameIndex) {
    return super.isLoadFromStackSlot(mi, frameIndex);
  }

  @Override
  public int isStoreToStackSlot(MachineInstr mi, OutRef<Integer> frameIndex) {
    return super.isStoreToStackSlot(mi, frameIndex);
  }

  @Override
  public void reMaterialize(MachineBasicBlock mbb, int insertPos, int destReg, int subIdx, MachineInstr origin) {
    super.reMaterialize(mbb, insertPos, destReg, subIdx, origin);
  }

  @Override
  public boolean subsumesPredicate(ArrayList<MachineOperand> pred1, ArrayList<MachineOperand> pred2) {
    return super.subsumesPredicate(pred1, pred2);
  }

  @Override
  public boolean definesPredicate(MachineInstr mi, ArrayList<MachineOperand> pred) {
    return super.definesPredicate(mi, pred);
  }
}
