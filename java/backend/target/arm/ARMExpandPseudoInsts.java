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
import backend.mc.MCInstrDesc;
import backend.support.MachineFunctionPass;
import backend.target.TargetInstrInfo;
import backend.target.TargetRegisterInfo;
import backend.value.GlobalValue;
import tools.OutRef;
import tools.Util;

import java.util.Arrays;

import static backend.codegen.MachineInstrBuilder.*;
import static backend.target.arm.ARMExpandPseudoInsts.NEONRegSpacing.*;
import static backend.target.arm.ARMRegisterInfo.addDefaultCC;
import static backend.target.arm.ARMRegisterInfo.addDefaultPred;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ARMExpandPseudoInsts extends MachineFunctionPass {
  private ARMFunctionInfo afi;
  private TargetInstrInfo tii;
  private TargetRegisterInfo tri;
  private ARMSubtarget subtarget;

  static MachineFunctionPass createARMExpandPseudoPass() {
    return new ARMExpandPseudoInsts();
  }

  @Override
  public boolean runOnMachineFunction(MachineFunction mf) {
    boolean changed = false;
    afi = (ARMFunctionInfo) mf.getFunctionInfo();
    tii = mf.getSubtarget().getInstrInfo();
    tri = mf.getSubtarget().getRegisterInfo();
    subtarget = (ARMSubtarget) mf.getTarget().getSubtarget();

    for (int i = 0, e = mf.getNumBlocks(); i < e; ++i) {
      MachineBasicBlock mbb = mf.getMBBAt(i);
      changed |= expandMBB(mbb);
    }
    return changed;
  }

  @Override
  public String getPassName() {
    return "ARM expand Pseudo Instruction";
  }

  private boolean expandMBB(MachineBasicBlock mbb) {
    boolean changed = false;
    for (int i = 0; i < mbb.size(); ++i) {
      changed |= expandMI(mbb, i);
    }
    return changed;
  }

  private boolean expandMI(MachineBasicBlock mbb, int mbbi) {
    MachineInstr mi = mbb.getInstAt(mbbi);
    int opcode = mi.getOpcode();
    switch (opcode) {
      default:
        return false;
      case ARMGenInstrNames.VMOVScc:
      case ARMGenInstrNames.VMOVDcc: {
        int newOpc = opcode == ARMGenInstrNames.VMOVScc ? ARMGenInstrNames.VMOVS :
            ARMGenInstrNames.VMOVD;
        buildMI(mbb, mbbi, mi.getDebugLoc(), tii.get(newOpc), mi.getOperand(1).getReg())
            .addReg(mi.getOperand(2).getReg(), getKillRegState(mi.getOperand(2).isKill()))
            .addImm(mi.getOperand(3).getImm()) // 'pred'
            .addReg(mi.getOperand(4).getReg()); // 'pred reg'
        mi.removeFromParent();
        return true;
      }
      case ARMGenInstrNames.t2MOVCCr:
      case ARMGenInstrNames.MOVCCr: {
        int opc = afi.isThumbFunction() ? ARMGenInstrNames.t2MOVr : ARMGenInstrNames.MOVr;
        buildMI(mbb, mbbi, mi.getDebugLoc(), tii.get(opc), mi.getOperand(1).getReg())
            .addReg(mi.getOperand(2).getReg(), getKillRegState(mi.getOperand(2).isKill()))
            .addImm(mi.getOperand(3).getImm()) // pred
            .addReg(mi.getOperand(4).getReg()) // pred reg.
            .addReg(0); // 's' bit

        mi.removeFromParent();
        return true;
      }
      case ARMGenInstrNames.MOVCCsi: {
        buildMI(mbb, mbbi, mi.getDebugLoc(), tii.get(ARMGenInstrNames.MOVsi),
            mi.getOperand(1).getReg())
            .addReg(mi.getOperand(2).getReg(), getKillRegState(mi.getOperand(2).isKill()))
            .addImm(mi.getOperand(3).getImm())
            .addImm(mi.getOperand(4).getImm()) // 'pred'
            .addReg(mi.getOperand(5).getReg())
            .addReg(0); // 's' bit.
        mi.removeFromParent();
        return true;
      }
      case ARMGenInstrNames.MOVCCsr: {
        buildMI(mbb, mbbi, mi.getDebugLoc(), tii.get(ARMGenInstrNames.MOVsr),
            mi.getOperand(1).getReg())
            .addReg(mi.getOperand(2).getReg(), getKillRegState(mi.getOperand(2).isKill()))
            .addReg(mi.getOperand(3).getReg(), getKillRegState(mi.getOperand(3).isKill()))
            .addImm(mi.getOperand(4).getImm())
            .addImm(mi.getOperand(5).getImm()) // 'pred'
            .addReg(mi.getOperand(6).getReg())
            .addReg(0); // 's' bit.
        mi.removeFromParent();
        return true;
      }
      case ARMGenInstrNames.MOVCCi16: {
        buildMI(mbb, mbbi, mi.getDebugLoc(), tii.get(ARMGenInstrNames.MOVi16),
            mi.getOperand(1).getReg())
            .addImm(mi.getOperand(2).getImm())
            .addImm(mi.getOperand(3).getImm())
            .addImm(mi.getOperand(4).getImm());
        mi.removeFromParent();
        return true;
      }
      case ARMGenInstrNames.t2MOVCCi:
      case ARMGenInstrNames.MOVCCi: {
        int opc = afi.isThumbFunction() ? ARMGenInstrNames.t2MOVi : ARMGenInstrNames.MOVi;
        buildMI(mbb, mbbi, mi.getDebugLoc(), tii.get(opc), mi.getOperand(1).getReg())
            .addImm(mi.getOperand(2).getImm())
            .addImm(mi.getOperand(3).getImm()) // pred
            .addReg(mi.getOperand(4).getReg()) // pred reg.
            .addReg(0); // 's' bit.

        mi.removeFromParent();
        return true;
      }
      case ARMGenInstrNames.MVNCCi: {
        buildMI(mbb, mbbi, mi.getDebugLoc(), tii.get(ARMGenInstrNames.MVNi), mi.getOperand(1).getReg())
            .addImm(mi.getOperand(2).getImm())
            .addImm(mi.getOperand(3).getImm()) // pred
            .addReg(mi.getOperand(4).getReg()) // pred reg.
            .addReg(0); // 's' bit.

        mi.removeFromParent();
        return true;
      }
      case ARMGenInstrNames.Int_eh_sjlj_dispatchsetup: {
        MachineFunction mf = mi.getParent().getParent();
        ARMInstrInfo aii = (ARMInstrInfo) tii;
        ARMRegisterInfo ri = (ARMRegisterInfo) mf.getSubtarget().getRegisterInfo();
        // For functions using a base pointer, we rematerialize it (via the frame
        // pointer) here since eh.sjlj.setjmp and eh.sjlj.longjmp don't do it
        // for us. Otherwise, expand to nothing.
        if (ri.hasBasePointer(mf)) {
          int numBytes = afi.getFramePtrSpillOffset();
          int framePtr = ri.getFrameRegister(mf);
          Util.assertion(mf.getSubtarget().getFrameLowering().hasFP(mf), "base pointer without frame pointer?");

          if (afi.isThumb2Function()) {
            mbbi = ARMRegisterInfo.emitT2RegPlusImmediate(mbb, mbbi, mi.getDebugLoc(),
                ARMGenRegisterNames.R6, framePtr, -numBytes, ARMCC.CondCodes.AL, 0, aii);
          } else if (afi.isThumbFunction()) {
            mbbi = ARMRegisterInfo.emitThumbRegPlusImmediate(mbb, mbbi, mi.getDebugLoc(),
                ARMGenRegisterNames.R6, framePtr, -numBytes, aii, ri);
          } else {
            mbbi = ARMRegisterInfo.emitARMRegPlusImmediate(mbb, mbbi, mi.getDebugLoc(),
                ARMGenRegisterNames.R6, framePtr, -numBytes, ARMCC.CondCodes.AL, 0, aii);
          }

          // if there is a dynamic realignment, adjust for it.
          if (ri.needsStackRealignment(mf)) {
            MachineFrameInfo mfi = mf.getFrameInfo();
            int maxAlign = mfi.getMaxAlignment();
            Util.assertion(!afi.isThumb1OnlyFunction());
            // emit bic r6, r6, maxAlign
            int bicOpc = afi.isThumbFunction() ? ARMGenInstrNames.t2BICri :
                ARMGenInstrNames.BICri;
            addDefaultCC(addDefaultPred(buildMI(mbb, mbbi, mi.getDebugLoc(),
                tii.get(bicOpc), ARMGenRegisterNames.R6)
                .addReg(ARMGenRegisterNames.R6, getKillRegState(true))
                .addImm(maxAlign - 1)));
          }
        }
        mi.removeFromParent();
        return true;
      }
      case ARMGenInstrNames.MOVsrl_flag:
      case ARMGenInstrNames.MOVsra_flag: {
        // there are just fancy MOVs instruction.
        addDefaultPred(buildMI(mbb, mbbi, mi.getDebugLoc(), tii.get(ARMGenInstrNames.MOVsi),
            mi.getOperand(0).getReg())
            .addOperand(mi.getOperand(1).clone())
            .addImm(ARM_AM.getSORegOpc(
                opcode == ARMGenInstrNames.MOVsrl_flag ? ARM_AM.ShiftOpc.lsr : ARM_AM.ShiftOpc.asr, 1)))
                .addReg(ARMGenRegisterNames.CPSR, getDefRegState(true));
        mi.removeFromParent();
        return true;
      }
      case ARMGenInstrNames.RRX: {
        // This encodes as "MOVs Rd, Rm, rrx
        MachineInstrBuilder mib =
                addDefaultPred(buildMI(mbb, mbbi, mi.getDebugLoc(),
                        tii.get(ARMGenInstrNames.MOVsi), mi.getOperand(0).getReg())
                        .addOperand(mi.getOperand(1).clone())
                        .addImm(ARM_AM.getSORegOpc(ARM_AM.ShiftOpc.rrx, 0)))
                        .addReg(0);
        transferTmpOps(mi, mib, mib);
        mi.removeFromParent();
        return true;
      }
      case ARMGenInstrNames.tTPsoft:
      case ARMGenInstrNames.TPsoft: {
        int newOpc = opcode == ARMGenInstrNames.tTPsoft ? ARMGenInstrNames.tBL : ARMGenInstrNames.BL;
        MachineInstrBuilder mib = buildMI(mbb, mbbi, mi.getDebugLoc(), tii.get(newOpc))
            .addExternalSymbol("__aeabi_read_tp", 0, 0);
        mib.setMemRefs(mi.getMemOperands());
        transferTmpOps(mi, mib, mib);
        mi.removeFromParent();
        return true;
      }
      case ARMGenInstrNames.tLDRpci_pic:
      case ARMGenInstrNames.t2LDRpci_pic: {
        int newOpc = opcode == ARMGenInstrNames.tLDRpci_pic ? ARMGenInstrNames.tLDRpci :
            ARMGenInstrNames.t2LDRpci;
        int destReg = mi.getOperand(0).getReg();
        boolean destIsDead = mi.getOperand(0).isDead();
        MachineInstrBuilder mib1 = addDefaultPred(buildMI(mbb, mbbi++, mi.getDebugLoc(),
            tii.get(newOpc), destReg).addOperand(mi.getOperand(1).clone()));
        mib1.setMemRefs(mi.getMemOperands());
        MachineInstrBuilder mib2 = buildMI(mbb, mbbi, mi.getDebugLoc(),
            tii.get(ARMGenInstrNames.tPICADD))
            .addReg(destReg, getDefRegState(true) | getDeadRegState(destIsDead))
            .addReg(destReg)
            .addOperand(mi.getOperand(2).clone());
        transferTmpOps(mi, mib1, mib2);
        mi.removeFromParent();
        return true;
      }
      case ARMGenInstrNames.MOV_ga_dyn:
      case ARMGenInstrNames.MOV_ga_pcrel:
      case ARMGenInstrNames.MOV_ga_pcrel_ldr:
      case ARMGenInstrNames.t2MOV_ga_dyn:
      case ARMGenInstrNames.t2MOV_ga_pcrel: {
        // Expand into movw + movw. Also "add pc" / ldr [pc] in PIC mode.
        int labelId = afi.createPICLabelUId();
        int dstReg = mi.getOperand(0).getReg();
        boolean dstIsDead = mi.getOperand(0).isDead();
        MachineOperand mo1 = mi.getOperand(1);
        GlobalValue gv = mo1.getGlobal();
        int tf = mo1.getTargetFlags();
        boolean isARM = (opcode != ARMGenInstrNames.t2MOV_ga_pcrel && opcode != ARMGenInstrNames.t2MOV_ga_dyn);
        boolean isPIC = (opcode != ARMGenInstrNames.MOV_ga_dyn && opcode != ARMGenInstrNames.t2MOV_ga_dyn);
        int lo16Opc = isARM ? ARMGenInstrNames.MOVi16_ga_pcrel : ARMGenInstrNames.t2MOVi16_ga_pcrel;
        int hi16Opc = isARM ? ARMGenInstrNames.MOVTi16_ga_pcrel : ARMGenInstrNames.t2MOVTi16_ga_pcrel;
        int lo16TF = isPIC
            ? ARMII.MO_LO16_NONLAZY_PIC : ARMII.MO_LO16_NONLAZY;
        int hi16TF = isPIC
            ? ARMII.MO_HI16_NONLAZY_PIC : ARMII.MO_HI16_NONLAZY;
        int picAddOpc = isARM
            ? (opcode == ARMGenInstrNames.MOV_ga_pcrel_ldr ? ARMGenInstrNames.PICLDR : ARMGenInstrNames.PICADD)
            : ARMGenInstrNames.tPICADD;
        MachineInstrBuilder mib1 = buildMI(mbb, mbbi, mi.getDebugLoc(),
            tii.get(lo16Opc), dstReg)
            .addGlobalAddress(gv, mo1.getOffset(), tf | lo16TF)
            .addImm(labelId);
        MachineInstrBuilder mib2 = buildMI(mbb, mbbi, mi.getDebugLoc(),
            tii.get(hi16Opc), dstReg)
            .addReg(dstReg)
            .addGlobalAddress(gv, mo1.getOffset(), tf | hi16TF)
            .addImm(labelId);
        if (!isPIC) {
          transferTmpOps(mi, mib1, mib2);
          mi.removeFromParent();
          return true;
        }

        MachineInstrBuilder mib3 = buildMI(mbb, mbbi, mi.getDebugLoc(),
            tii.get(picAddOpc))
            .addReg(dstReg, getDefRegState(true) | getDeadRegState(dstIsDead))
            .addReg(dstReg).addImm(labelId);
        if (isARM) {
          addDefaultPred(mib3);
          if (opcode == ARMGenInstrNames.MOV_ga_pcrel_ldr)
            mib2.setMemRefs(mi.getMemOperands());
        }
        transferTmpOps(mi, mib1, mib3);
        mi.removeFromParent();
        return true;
      }

      case ARMGenInstrNames.MOVi32imm:
      case ARMGenInstrNames.MOVCCi32imm:
      case ARMGenInstrNames.t2MOVi32imm:
      case ARMGenInstrNames.t2MOVCCi32imm:
        expandMOV32BitImm(mbb, mbbi);
        return true;

      case ARMGenInstrNames.VLDMQIA: {
        int newOpc = ARMGenInstrNames.VLDMDIA;
        MachineInstrBuilder mib =
            buildMI(mbb, mbbi, mi.getDebugLoc(), tii.get(newOpc));
        int opIdx = 0;

        // Grab the Q register destination.
        boolean dstIsDead = mi.getOperand(opIdx).isDead();
        int dstReg = mi.getOperand(opIdx++).getReg();

        // Copy the source register.
        mib.addOperand(mi.getOperand(opIdx++));

        // Copy the predicate operands.
        mib.addOperand(mi.getOperand(opIdx++));
        mib.addOperand(mi.getOperand(opIdx++));

        // Add the destination operands (D subregs).
        int d0 = tri.getSubReg(dstReg, ARMGenRegisterInfo.dsub_0);
        int d1 = tri.getSubReg(dstReg, ARMGenRegisterInfo.dsub_1);
        mib.addReg(d0, getDefRegState(true) | getDeadRegState(dstIsDead))
            .addReg(d1, getDefRegState(true) | getDeadRegState(dstIsDead));

        // Add an implicit def for the super-register.
        mib.addReg(dstReg, getImplRegState(true) | getDefRegState(true) | getDeadRegState(dstIsDead));
        transferTmpOps(mi, mib, mib);
        mi.removeFromParent();
        return true;
      }

      case ARMGenInstrNames.VSTMQIA: {
        int newOpc = ARMGenInstrNames.VSTMDIA;
        MachineInstrBuilder mib =
            buildMI(mbb, mbbi, mi.getDebugLoc(), tii.get(newOpc));
        int opIdx = 0;

        // Grab the Q register source.
        boolean srcIsKill = mi.getOperand(opIdx).isKill();
        int srcReg = mi.getOperand(opIdx++).getReg();

        // Copy the destination register.
        mib.addOperand(mi.getOperand(opIdx++));

        // Copy the predicate operands.
        mib.addOperand(mi.getOperand(opIdx++));
        mib.addOperand(mi.getOperand(opIdx++));

        // Add the source operands (D subregs).
        int d0 = tri.getSubReg(srcReg, ARMGenRegisterInfo.dsub_0);
        int d1 = tri.getSubReg(srcReg, ARMGenRegisterInfo.dsub_1);
        mib.addReg(d0).addReg(d1);

        if (srcIsKill)      // Add an implicit kill for the Q register.
          mib.getMInstr().addRegisterKilled(srcReg, tri, true);

        transferTmpOps(mi, mib, mib);
        mi.removeFromParent();
        return true;
      }
      case ARMGenInstrNames.VDUPfqf:
      case ARMGenInstrNames.VDUPfdf: {
        int newOpc = opcode == ARMGenInstrNames.VDUPfqf ? ARMGenInstrNames.VDUPLN32q :
            ARMGenInstrNames.VDUPLN32d;
        MachineInstrBuilder mib =
            buildMI(mbb, mbbi, mi.getDebugLoc(), tii.get(newOpc));
        int opIdx = 0;
        int srcReg = mi.getOperand(1).getReg();
        int lane = ARMRegisterInfo.getARMRegisterNumbering(srcReg) & 1;
        int dReg = tri.getMatchingSuperReg(srcReg,
            (lane & 1) != 0 ? ARMGenRegisterInfo.ssub_1 : ARMGenRegisterInfo.ssub_0,
            ARMGenRegisterInfo.DPR_VFP2RegisterClass);
        // The lane is [0,1] for the containing DReg superregister.
        // Copy the dst/src register operands.
        mib.addOperand(mi.getOperand(opIdx++));
        mib.addReg(dReg);
        ++opIdx;
        // Add the lane select operand.
        mib.addImm(lane);
        // Add the predicate operands.
        mib.addOperand(mi.getOperand(opIdx++));
        mib.addOperand(mi.getOperand(opIdx++));

        transferTmpOps(mi, mib, mib);
        mi.removeFromParent();
        return true;
      }

      case ARMGenInstrNames.VLD1q8Pseudo:
      case ARMGenInstrNames.VLD1q16Pseudo:
      case ARMGenInstrNames.VLD1q32Pseudo:
      case ARMGenInstrNames.VLD1q64Pseudo:
      case ARMGenInstrNames.VLD1q8Pseudo_UPD:
      case ARMGenInstrNames.VLD1q16Pseudo_UPD:
      case ARMGenInstrNames.VLD1q32Pseudo_UPD:
      case ARMGenInstrNames.VLD1q64Pseudo_UPD:
      case ARMGenInstrNames.VLD2d8Pseudo:
      case ARMGenInstrNames.VLD2d16Pseudo:
      case ARMGenInstrNames.VLD2d32Pseudo:
      case ARMGenInstrNames.VLD2q8Pseudo:
      case ARMGenInstrNames.VLD2q16Pseudo:
      case ARMGenInstrNames.VLD2q32Pseudo:
      case ARMGenInstrNames.VLD2d8Pseudo_UPD:
      case ARMGenInstrNames.VLD2d16Pseudo_UPD:
      case ARMGenInstrNames.VLD2d32Pseudo_UPD:
      case ARMGenInstrNames.VLD2q8Pseudo_UPD:
      case ARMGenInstrNames.VLD2q16Pseudo_UPD:
      case ARMGenInstrNames.VLD2q32Pseudo_UPD:
      case ARMGenInstrNames.VLD3d8Pseudo:
      case ARMGenInstrNames.VLD3d16Pseudo:
      case ARMGenInstrNames.VLD3d32Pseudo:
      case ARMGenInstrNames.VLD1d64TPseudo:
      case ARMGenInstrNames.VLD3d8Pseudo_UPD:
      case ARMGenInstrNames.VLD3d16Pseudo_UPD:
      case ARMGenInstrNames.VLD3d32Pseudo_UPD:
      case ARMGenInstrNames.VLD1d64TPseudo_UPD:
      case ARMGenInstrNames.VLD3q8Pseudo_UPD:
      case ARMGenInstrNames.VLD3q16Pseudo_UPD:
      case ARMGenInstrNames.VLD3q32Pseudo_UPD:
      case ARMGenInstrNames.VLD3q8oddPseudo:
      case ARMGenInstrNames.VLD3q16oddPseudo:
      case ARMGenInstrNames.VLD3q32oddPseudo:
      case ARMGenInstrNames.VLD3q8oddPseudo_UPD:
      case ARMGenInstrNames.VLD3q16oddPseudo_UPD:
      case ARMGenInstrNames.VLD3q32oddPseudo_UPD:
      case ARMGenInstrNames.VLD4d8Pseudo:
      case ARMGenInstrNames.VLD4d16Pseudo:
      case ARMGenInstrNames.VLD4d32Pseudo:
      case ARMGenInstrNames.VLD1d64QPseudo:
      case ARMGenInstrNames.VLD4d8Pseudo_UPD:
      case ARMGenInstrNames.VLD4d16Pseudo_UPD:
      case ARMGenInstrNames.VLD4d32Pseudo_UPD:
      case ARMGenInstrNames.VLD1d64QPseudo_UPD:
      case ARMGenInstrNames.VLD4q8Pseudo_UPD:
      case ARMGenInstrNames.VLD4q16Pseudo_UPD:
      case ARMGenInstrNames.VLD4q32Pseudo_UPD:
      case ARMGenInstrNames.VLD4q8oddPseudo:
      case ARMGenInstrNames.VLD4q16oddPseudo:
      case ARMGenInstrNames.VLD4q32oddPseudo:
      case ARMGenInstrNames.VLD4q8oddPseudo_UPD:
      case ARMGenInstrNames.VLD4q16oddPseudo_UPD:
      case ARMGenInstrNames.VLD4q32oddPseudo_UPD:
      case ARMGenInstrNames.VLD1DUPq8Pseudo:
      case ARMGenInstrNames.VLD1DUPq16Pseudo:
      case ARMGenInstrNames.VLD1DUPq32Pseudo:
      case ARMGenInstrNames.VLD1DUPq8Pseudo_UPD:
      case ARMGenInstrNames.VLD1DUPq16Pseudo_UPD:
      case ARMGenInstrNames.VLD1DUPq32Pseudo_UPD:
      case ARMGenInstrNames.VLD2DUPd8Pseudo:
      case ARMGenInstrNames.VLD2DUPd16Pseudo:
      case ARMGenInstrNames.VLD2DUPd32Pseudo:
      case ARMGenInstrNames.VLD2DUPd8Pseudo_UPD:
      case ARMGenInstrNames.VLD2DUPd16Pseudo_UPD:
      case ARMGenInstrNames.VLD2DUPd32Pseudo_UPD:
      case ARMGenInstrNames.VLD3DUPd8Pseudo:
      case ARMGenInstrNames.VLD3DUPd16Pseudo:
      case ARMGenInstrNames.VLD3DUPd32Pseudo:
      case ARMGenInstrNames.VLD3DUPd8Pseudo_UPD:
      case ARMGenInstrNames.VLD3DUPd16Pseudo_UPD:
      case ARMGenInstrNames.VLD3DUPd32Pseudo_UPD:
      case ARMGenInstrNames.VLD4DUPd8Pseudo:
      case ARMGenInstrNames.VLD4DUPd16Pseudo:
      case ARMGenInstrNames.VLD4DUPd32Pseudo:
      case ARMGenInstrNames.VLD4DUPd8Pseudo_UPD:
      case ARMGenInstrNames.VLD4DUPd16Pseudo_UPD:
      case ARMGenInstrNames.VLD4DUPd32Pseudo_UPD:
        expandVLD(mbb, mbbi);
        return true;

      case ARMGenInstrNames.VST1q8Pseudo:
      case ARMGenInstrNames.VST1q16Pseudo:
      case ARMGenInstrNames.VST1q32Pseudo:
      case ARMGenInstrNames.VST1q64Pseudo:
      case ARMGenInstrNames.VST1q8Pseudo_UPD:
      case ARMGenInstrNames.VST1q16Pseudo_UPD:
      case ARMGenInstrNames.VST1q32Pseudo_UPD:
      case ARMGenInstrNames.VST1q64Pseudo_UPD:
      case ARMGenInstrNames.VST2d8Pseudo:
      case ARMGenInstrNames.VST2d16Pseudo:
      case ARMGenInstrNames.VST2d32Pseudo:
      case ARMGenInstrNames.VST2q8Pseudo:
      case ARMGenInstrNames.VST2q16Pseudo:
      case ARMGenInstrNames.VST2q32Pseudo:
      case ARMGenInstrNames.VST2d8Pseudo_UPD:
      case ARMGenInstrNames.VST2d16Pseudo_UPD:
      case ARMGenInstrNames.VST2d32Pseudo_UPD:
      case ARMGenInstrNames.VST2q8Pseudo_UPD:
      case ARMGenInstrNames.VST2q16Pseudo_UPD:
      case ARMGenInstrNames.VST2q32Pseudo_UPD:
      case ARMGenInstrNames.VST3d8Pseudo:
      case ARMGenInstrNames.VST3d16Pseudo:
      case ARMGenInstrNames.VST3d32Pseudo:
      case ARMGenInstrNames.VST1d64TPseudo:
      case ARMGenInstrNames.VST3d8Pseudo_UPD:
      case ARMGenInstrNames.VST3d16Pseudo_UPD:
      case ARMGenInstrNames.VST3d32Pseudo_UPD:
      case ARMGenInstrNames.VST1d64TPseudo_UPD:
      case ARMGenInstrNames.VST3q8Pseudo_UPD:
      case ARMGenInstrNames.VST3q16Pseudo_UPD:
      case ARMGenInstrNames.VST3q32Pseudo_UPD:
      case ARMGenInstrNames.VST3q8oddPseudo:
      case ARMGenInstrNames.VST3q16oddPseudo:
      case ARMGenInstrNames.VST3q32oddPseudo:
      case ARMGenInstrNames.VST3q8oddPseudo_UPD:
      case ARMGenInstrNames.VST3q16oddPseudo_UPD:
      case ARMGenInstrNames.VST3q32oddPseudo_UPD:
      case ARMGenInstrNames.VST4d8Pseudo:
      case ARMGenInstrNames.VST4d16Pseudo:
      case ARMGenInstrNames.VST4d32Pseudo:
      case ARMGenInstrNames.VST1d64QPseudo:
      case ARMGenInstrNames.VST4d8Pseudo_UPD:
      case ARMGenInstrNames.VST4d16Pseudo_UPD:
      case ARMGenInstrNames.VST4d32Pseudo_UPD:
      case ARMGenInstrNames.VST1d64QPseudo_UPD:
      case ARMGenInstrNames.VST4q8Pseudo_UPD:
      case ARMGenInstrNames.VST4q16Pseudo_UPD:
      case ARMGenInstrNames.VST4q32Pseudo_UPD:
      case ARMGenInstrNames.VST4q8oddPseudo:
      case ARMGenInstrNames.VST4q16oddPseudo:
      case ARMGenInstrNames.VST4q32oddPseudo:
      case ARMGenInstrNames.VST4q8oddPseudo_UPD:
      case ARMGenInstrNames.VST4q16oddPseudo_UPD:
      case ARMGenInstrNames.VST4q32oddPseudo_UPD:
        expandVST(mbb, mbbi);
        return true;

      case ARMGenInstrNames.VLD1LNq8Pseudo:
      case ARMGenInstrNames.VLD1LNq16Pseudo:
      case ARMGenInstrNames.VLD1LNq32Pseudo:
      case ARMGenInstrNames.VLD1LNq8Pseudo_UPD:
      case ARMGenInstrNames.VLD1LNq16Pseudo_UPD:
      case ARMGenInstrNames.VLD1LNq32Pseudo_UPD:
      case ARMGenInstrNames.VLD2LNd8Pseudo:
      case ARMGenInstrNames.VLD2LNd16Pseudo:
      case ARMGenInstrNames.VLD2LNd32Pseudo:
      case ARMGenInstrNames.VLD2LNq16Pseudo:
      case ARMGenInstrNames.VLD2LNq32Pseudo:
      case ARMGenInstrNames.VLD2LNd8Pseudo_UPD:
      case ARMGenInstrNames.VLD2LNd16Pseudo_UPD:
      case ARMGenInstrNames.VLD2LNd32Pseudo_UPD:
      case ARMGenInstrNames.VLD2LNq16Pseudo_UPD:
      case ARMGenInstrNames.VLD2LNq32Pseudo_UPD:
      case ARMGenInstrNames.VLD3LNd8Pseudo:
      case ARMGenInstrNames.VLD3LNd16Pseudo:
      case ARMGenInstrNames.VLD3LNd32Pseudo:
      case ARMGenInstrNames.VLD3LNq16Pseudo:
      case ARMGenInstrNames.VLD3LNq32Pseudo:
      case ARMGenInstrNames.VLD3LNd8Pseudo_UPD:
      case ARMGenInstrNames.VLD3LNd16Pseudo_UPD:
      case ARMGenInstrNames.VLD3LNd32Pseudo_UPD:
      case ARMGenInstrNames.VLD3LNq16Pseudo_UPD:
      case ARMGenInstrNames.VLD3LNq32Pseudo_UPD:
      case ARMGenInstrNames.VLD4LNd8Pseudo:
      case ARMGenInstrNames.VLD4LNd16Pseudo:
      case ARMGenInstrNames.VLD4LNd32Pseudo:
      case ARMGenInstrNames.VLD4LNq16Pseudo:
      case ARMGenInstrNames.VLD4LNq32Pseudo:
      case ARMGenInstrNames.VLD4LNd8Pseudo_UPD:
      case ARMGenInstrNames.VLD4LNd16Pseudo_UPD:
      case ARMGenInstrNames.VLD4LNd32Pseudo_UPD:
      case ARMGenInstrNames.VLD4LNq16Pseudo_UPD:
      case ARMGenInstrNames.VLD4LNq32Pseudo_UPD:
      case ARMGenInstrNames.VST1LNq8Pseudo:
      case ARMGenInstrNames.VST1LNq16Pseudo:
      case ARMGenInstrNames.VST1LNq32Pseudo:
      case ARMGenInstrNames.VST1LNq8Pseudo_UPD:
      case ARMGenInstrNames.VST1LNq16Pseudo_UPD:
      case ARMGenInstrNames.VST1LNq32Pseudo_UPD:
      case ARMGenInstrNames.VST2LNd8Pseudo:
      case ARMGenInstrNames.VST2LNd16Pseudo:
      case ARMGenInstrNames.VST2LNd32Pseudo:
      case ARMGenInstrNames.VST2LNq16Pseudo:
      case ARMGenInstrNames.VST2LNq32Pseudo:
      case ARMGenInstrNames.VST2LNd8Pseudo_UPD:
      case ARMGenInstrNames.VST2LNd16Pseudo_UPD:
      case ARMGenInstrNames.VST2LNd32Pseudo_UPD:
      case ARMGenInstrNames.VST2LNq16Pseudo_UPD:
      case ARMGenInstrNames.VST2LNq32Pseudo_UPD:
      case ARMGenInstrNames.VST3LNd8Pseudo:
      case ARMGenInstrNames.VST3LNd16Pseudo:
      case ARMGenInstrNames.VST3LNd32Pseudo:
      case ARMGenInstrNames.VST3LNq16Pseudo:
      case ARMGenInstrNames.VST3LNq32Pseudo:
      case ARMGenInstrNames.VST3LNd8Pseudo_UPD:
      case ARMGenInstrNames.VST3LNd16Pseudo_UPD:
      case ARMGenInstrNames.VST3LNd32Pseudo_UPD:
      case ARMGenInstrNames.VST3LNq16Pseudo_UPD:
      case ARMGenInstrNames.VST3LNq32Pseudo_UPD:
      case ARMGenInstrNames.VST4LNd8Pseudo:
      case ARMGenInstrNames.VST4LNd16Pseudo:
      case ARMGenInstrNames.VST4LNd32Pseudo:
      case ARMGenInstrNames.VST4LNq16Pseudo:
      case ARMGenInstrNames.VST4LNq32Pseudo:
      case ARMGenInstrNames.VST4LNd8Pseudo_UPD:
      case ARMGenInstrNames.VST4LNd16Pseudo_UPD:
      case ARMGenInstrNames.VST4LNd32Pseudo_UPD:
      case ARMGenInstrNames.VST4LNq16Pseudo_UPD:
      case ARMGenInstrNames.VST4LNq32Pseudo_UPD:
        expandLaneOp(mbb, mbbi);
        return true;

      case ARMGenInstrNames.VTBL2Pseudo: expandVTBL(mbb, mbbi, ARMGenInstrNames.VTBL2, false, 2); return true;
      case ARMGenInstrNames.VTBL3Pseudo: expandVTBL(mbb, mbbi, ARMGenInstrNames.VTBL3, false, 3); return true;
      case ARMGenInstrNames.VTBL4Pseudo: expandVTBL(mbb, mbbi, ARMGenInstrNames.VTBL4, false, 4); return true;
      case ARMGenInstrNames.VTBX2Pseudo: expandVTBL(mbb, mbbi, ARMGenInstrNames.VTBX2, true, 2); return true;
      case ARMGenInstrNames.VTBX3Pseudo: expandVTBL(mbb, mbbi, ARMGenInstrNames.VTBX3, true, 3); return true;
      case ARMGenInstrNames.VTBX4Pseudo: expandVTBL(mbb, mbbi, ARMGenInstrNames.VTBX4, true, 4); return true;
    }
  }
  // Constants for register spacing in NEON load/store instructions.
  // For quad-register load-lane and store-lane pseudo instructors, the
  // spacing is initially assumed to be EvenDblSpc, and that is changed to
  // OddDblSpc depending on the lane number operand.
  enum NEONRegSpacing {
    SingleSpc,
    EvenDblSpc,
    OddDblSpc
  }

  // Entries for NEON load/store information table.  The table is sorted by
  // pseudoOpc for fast binary-search lookups.
  private static class NEONLdStTableEntry implements Comparable<NEONLdStTableEntry> {
    int pseudoOpc;
    int realOpc;
    boolean isLoad;
    boolean hasWriteBack;
    NEONRegSpacing regSpacing;
    int numRegs; // D registers loaded or stored
    int regElts; // elements per D register; used for lane ops

    NEONLdStTableEntry(int pseudo, int real, boolean load, boolean hasWB, NEONRegSpacing spacing, int regs, int elts) {
      pseudoOpc = pseudo;
      realOpc = real;
      isLoad = load;
      hasWriteBack = hasWB;
      regSpacing = spacing;
      numRegs = regs;
      regElts = elts;
    }

    // Comparison methods for binary search of the table.
    @Override
    public int compareTo(NEONLdStTableEntry o) {
      return pseudoOpc - o.pseudoOpc;
    }
  }

  private static final NEONLdStTableEntry NEONLdStTable[] = {
      new NEONLdStTableEntry(ARMGenInstrNames.VLD1DUPq16Pseudo,     ARMGenInstrNames.VLD1DUPq16,     true, false, SingleSpc, 2, 4),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD1DUPq16Pseudo_UPD, ARMGenInstrNames.VLD1DUPq16_UPD, true, true,  SingleSpc, 2, 4),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD1DUPq32Pseudo,     ARMGenInstrNames.VLD1DUPq32,     true, false, SingleSpc, 2, 2),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD1DUPq32Pseudo_UPD, ARMGenInstrNames.VLD1DUPq32_UPD, true, true,  SingleSpc, 2, 2),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD1DUPq8Pseudo,      ARMGenInstrNames.VLD1DUPq8,      true, false, SingleSpc, 2, 8),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD1DUPq8Pseudo_UPD,  ARMGenInstrNames.VLD1DUPq8_UPD,  true, true,  SingleSpc, 2, 8),

      new NEONLdStTableEntry(ARMGenInstrNames.VLD1LNq16Pseudo,     ARMGenInstrNames.VLD1LNd16,     true, false, EvenDblSpc, 1, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD1LNq16Pseudo_UPD, ARMGenInstrNames.VLD1LNd16_UPD, true, true,  EvenDblSpc, 1, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD1LNq32Pseudo,     ARMGenInstrNames.VLD1LNd32,     true, false, EvenDblSpc, 1, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD1LNq32Pseudo_UPD, ARMGenInstrNames.VLD1LNd32_UPD, true, true,  EvenDblSpc, 1, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD1LNq8Pseudo,      ARMGenInstrNames.VLD1LNd8,      true, false, EvenDblSpc, 1, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD1LNq8Pseudo_UPD,  ARMGenInstrNames.VLD1LNd8_UPD,  true, true,  EvenDblSpc, 1, 8 ),

      new NEONLdStTableEntry(ARMGenInstrNames.VLD1d64QPseudo,      ARMGenInstrNames.VLD1d64Q,     true,  false, SingleSpc,  4, 1 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD1d64QPseudo_UPD,  ARMGenInstrNames.VLD1d64Q_UPD, true,  true,  SingleSpc,  4, 1 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD1d64TPseudo,      ARMGenInstrNames.VLD1d64T,     true,  false, SingleSpc,  3, 1 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD1d64TPseudo_UPD,  ARMGenInstrNames.VLD1d64T_UPD, true,  true,  SingleSpc,  3, 1 ),

      new NEONLdStTableEntry(ARMGenInstrNames.VLD1q16Pseudo,       ARMGenInstrNames.VLD1q16,      true,  false, SingleSpc,  2, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD1q16Pseudo_UPD,   ARMGenInstrNames.VLD1q16_UPD,  true,  true,  SingleSpc,  2, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD1q32Pseudo,       ARMGenInstrNames.VLD1q32,      true,  false, SingleSpc,  2, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD1q32Pseudo_UPD,   ARMGenInstrNames.VLD1q32_UPD,  true,  true,  SingleSpc,  2, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD1q64Pseudo,       ARMGenInstrNames.VLD1q64,      true,  false, SingleSpc,  2, 1 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD1q64Pseudo_UPD,   ARMGenInstrNames.VLD1q64_UPD,  true,  true,  SingleSpc,  2, 1 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD1q8Pseudo,        ARMGenInstrNames.VLD1q8,       true,  false, SingleSpc,  2, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD1q8Pseudo_UPD,    ARMGenInstrNames.VLD1q8_UPD,   true,  true,  SingleSpc,  2, 8 ),

      new NEONLdStTableEntry(ARMGenInstrNames.VLD2DUPd16Pseudo,     ARMGenInstrNames.VLD2DUPd16,     true, false, SingleSpc, 2, 4),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2DUPd16Pseudo_UPD, ARMGenInstrNames.VLD2DUPd16_UPD, true, true,  SingleSpc, 2, 4),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2DUPd32Pseudo,     ARMGenInstrNames.VLD2DUPd32,     true, false, SingleSpc, 2, 2),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2DUPd32Pseudo_UPD, ARMGenInstrNames.VLD2DUPd32_UPD, true, true,  SingleSpc, 2, 2),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2DUPd8Pseudo,      ARMGenInstrNames.VLD2DUPd8,      true, false, SingleSpc, 2, 8),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2DUPd8Pseudo_UPD,  ARMGenInstrNames.VLD2DUPd8_UPD,  true, true,  SingleSpc, 2, 8),

      new NEONLdStTableEntry(ARMGenInstrNames.VLD2LNd16Pseudo,     ARMGenInstrNames.VLD2LNd16,     true, false, SingleSpc,  2, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2LNd16Pseudo_UPD, ARMGenInstrNames.VLD2LNd16_UPD, true, true,  SingleSpc,  2, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2LNd32Pseudo,     ARMGenInstrNames.VLD2LNd32,     true, false, SingleSpc,  2, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2LNd32Pseudo_UPD, ARMGenInstrNames.VLD2LNd32_UPD, true, true,  SingleSpc,  2, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2LNd8Pseudo,      ARMGenInstrNames.VLD2LNd8,      true, false, SingleSpc,  2, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2LNd8Pseudo_UPD,  ARMGenInstrNames.VLD2LNd8_UPD,  true, true,  SingleSpc,  2, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2LNq16Pseudo,     ARMGenInstrNames.VLD2LNq16,     true, false, EvenDblSpc, 2, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2LNq16Pseudo_UPD, ARMGenInstrNames.VLD2LNq16_UPD, true, true,  EvenDblSpc, 2, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2LNq32Pseudo,     ARMGenInstrNames.VLD2LNq32,     true, false, EvenDblSpc, 2, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2LNq32Pseudo_UPD, ARMGenInstrNames.VLD2LNq32_UPD, true, true,  EvenDblSpc, 2, 2 ),

      new NEONLdStTableEntry(ARMGenInstrNames.VLD2d16Pseudo,       ARMGenInstrNames.VLD2d16,      true,  false, SingleSpc,  2, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2d16Pseudo_UPD,   ARMGenInstrNames.VLD2d16_UPD,  true,  true,  SingleSpc,  2, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2d32Pseudo,       ARMGenInstrNames.VLD2d32,      true,  false, SingleSpc,  2, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2d32Pseudo_UPD,   ARMGenInstrNames.VLD2d32_UPD,  true,  true,  SingleSpc,  2, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2d8Pseudo,        ARMGenInstrNames.VLD2d8,       true,  false, SingleSpc,  2, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2d8Pseudo_UPD,    ARMGenInstrNames.VLD2d8_UPD,   true,  true,  SingleSpc,  2, 8 ),

      new NEONLdStTableEntry(ARMGenInstrNames.VLD2q16Pseudo,       ARMGenInstrNames.VLD2q16,      true,  false, SingleSpc,  4, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2q16Pseudo_UPD,   ARMGenInstrNames.VLD2q16_UPD,  true,  true,  SingleSpc,  4, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2q32Pseudo,       ARMGenInstrNames.VLD2q32,      true,  false, SingleSpc,  4, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2q32Pseudo_UPD,   ARMGenInstrNames.VLD2q32_UPD,  true,  true,  SingleSpc,  4, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2q8Pseudo,        ARMGenInstrNames.VLD2q8,       true,  false, SingleSpc,  4, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD2q8Pseudo_UPD,    ARMGenInstrNames.VLD2q8_UPD,   true,  true,  SingleSpc,  4, 8 ),

      new NEONLdStTableEntry(ARMGenInstrNames.VLD3DUPd16Pseudo,     ARMGenInstrNames.VLD3DUPd16,     true, false, SingleSpc, 3, 4),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3DUPd16Pseudo_UPD, ARMGenInstrNames.VLD3DUPd16_UPD, true, true,  SingleSpc, 3, 4),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3DUPd32Pseudo,     ARMGenInstrNames.VLD3DUPd32,     true, false, SingleSpc, 3, 2),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3DUPd32Pseudo_UPD, ARMGenInstrNames.VLD3DUPd32_UPD, true, true,  SingleSpc, 3, 2),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3DUPd8Pseudo,      ARMGenInstrNames.VLD3DUPd8,      true, false, SingleSpc, 3, 8),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3DUPd8Pseudo_UPD,  ARMGenInstrNames.VLD3DUPd8_UPD,  true, true,  SingleSpc, 3, 8),

      new NEONLdStTableEntry(ARMGenInstrNames.VLD3LNd16Pseudo,     ARMGenInstrNames.VLD3LNd16,     true, false, SingleSpc,  3, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3LNd16Pseudo_UPD, ARMGenInstrNames.VLD3LNd16_UPD, true, true,  SingleSpc,  3, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3LNd32Pseudo,     ARMGenInstrNames.VLD3LNd32,     true, false, SingleSpc,  3, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3LNd32Pseudo_UPD, ARMGenInstrNames.VLD3LNd32_UPD, true, true,  SingleSpc,  3, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3LNd8Pseudo,      ARMGenInstrNames.VLD3LNd8,      true, false, SingleSpc,  3, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3LNd8Pseudo_UPD,  ARMGenInstrNames.VLD3LNd8_UPD,  true, true,  SingleSpc,  3, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3LNq16Pseudo,     ARMGenInstrNames.VLD3LNq16,     true, false, EvenDblSpc, 3, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3LNq16Pseudo_UPD, ARMGenInstrNames.VLD3LNq16_UPD, true, true,  EvenDblSpc, 3, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3LNq32Pseudo,     ARMGenInstrNames.VLD3LNq32,     true, false, EvenDblSpc, 3, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3LNq32Pseudo_UPD, ARMGenInstrNames.VLD3LNq32_UPD, true, true,  EvenDblSpc, 3, 2 ),

      new NEONLdStTableEntry(ARMGenInstrNames.VLD3d16Pseudo,       ARMGenInstrNames.VLD3d16,      true,  false, SingleSpc,  3, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3d16Pseudo_UPD,   ARMGenInstrNames.VLD3d16_UPD,  true,  true,  SingleSpc,  3, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3d32Pseudo,       ARMGenInstrNames.VLD3d32,      true,  false, SingleSpc,  3, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3d32Pseudo_UPD,   ARMGenInstrNames.VLD3d32_UPD,  true,  true,  SingleSpc,  3, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3d8Pseudo,        ARMGenInstrNames.VLD3d8,       true,  false, SingleSpc,  3, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3d8Pseudo_UPD,    ARMGenInstrNames.VLD3d8_UPD,   true,  true,  SingleSpc,  3, 8 ),

      new NEONLdStTableEntry(ARMGenInstrNames.VLD3q16Pseudo_UPD,    ARMGenInstrNames.VLD3q16_UPD, true,  true,  EvenDblSpc, 3, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3q16oddPseudo,     ARMGenInstrNames.VLD3q16,     true,  false, OddDblSpc,  3, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3q16oddPseudo_UPD, ARMGenInstrNames.VLD3q16_UPD, true,  true,  OddDblSpc,  3, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3q32Pseudo_UPD,    ARMGenInstrNames.VLD3q32_UPD, true,  true,  EvenDblSpc, 3, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3q32oddPseudo,     ARMGenInstrNames.VLD3q32,     true,  false, OddDblSpc,  3, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3q32oddPseudo_UPD, ARMGenInstrNames.VLD3q32_UPD, true,  true,  OddDblSpc,  3, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3q8Pseudo_UPD,     ARMGenInstrNames.VLD3q8_UPD,  true,  true,  EvenDblSpc, 3, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3q8oddPseudo,      ARMGenInstrNames.VLD3q8,      true,  false, OddDblSpc,  3, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD3q8oddPseudo_UPD,  ARMGenInstrNames.VLD3q8_UPD,  true,  true,  OddDblSpc,  3, 8 ),

      new NEONLdStTableEntry(ARMGenInstrNames.VLD4DUPd16Pseudo,     ARMGenInstrNames.VLD4DUPd16,     true, false, SingleSpc, 4, 4),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4DUPd16Pseudo_UPD, ARMGenInstrNames.VLD4DUPd16_UPD, true, true,  SingleSpc, 4, 4),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4DUPd32Pseudo,     ARMGenInstrNames.VLD4DUPd32,     true, false, SingleSpc, 4, 2),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4DUPd32Pseudo_UPD, ARMGenInstrNames.VLD4DUPd32_UPD, true, true,  SingleSpc, 4, 2),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4DUPd8Pseudo,      ARMGenInstrNames.VLD4DUPd8,      true, false, SingleSpc, 4, 8),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4DUPd8Pseudo_UPD,  ARMGenInstrNames.VLD4DUPd8_UPD,  true, true,  SingleSpc, 4, 8),

      new NEONLdStTableEntry(ARMGenInstrNames.VLD4LNd16Pseudo,     ARMGenInstrNames.VLD4LNd16,     true, false, SingleSpc,  4, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4LNd16Pseudo_UPD, ARMGenInstrNames.VLD4LNd16_UPD, true, true,  SingleSpc,  4, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4LNd32Pseudo,     ARMGenInstrNames.VLD4LNd32,     true, false, SingleSpc,  4, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4LNd32Pseudo_UPD, ARMGenInstrNames.VLD4LNd32_UPD, true, true,  SingleSpc,  4, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4LNd8Pseudo,      ARMGenInstrNames.VLD4LNd8,      true, false, SingleSpc,  4, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4LNd8Pseudo_UPD,  ARMGenInstrNames.VLD4LNd8_UPD,  true, true,  SingleSpc,  4, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4LNq16Pseudo,     ARMGenInstrNames.VLD4LNq16,     true, false, EvenDblSpc, 4, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4LNq16Pseudo_UPD, ARMGenInstrNames.VLD4LNq16_UPD, true, true,  EvenDblSpc, 4, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4LNq32Pseudo,     ARMGenInstrNames.VLD4LNq32,     true, false, EvenDblSpc, 4, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4LNq32Pseudo_UPD, ARMGenInstrNames.VLD4LNq32_UPD, true, true,  EvenDblSpc, 4, 2 ),

      new NEONLdStTableEntry(ARMGenInstrNames.VLD4d16Pseudo,       ARMGenInstrNames.VLD4d16,      true,  false, SingleSpc,  4, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4d16Pseudo_UPD,   ARMGenInstrNames.VLD4d16_UPD,  true,  true,  SingleSpc,  4, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4d32Pseudo,       ARMGenInstrNames.VLD4d32,      true,  false, SingleSpc,  4, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4d32Pseudo_UPD,   ARMGenInstrNames.VLD4d32_UPD,  true,  true,  SingleSpc,  4, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4d8Pseudo,        ARMGenInstrNames.VLD4d8,       true,  false, SingleSpc,  4, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4d8Pseudo_UPD,    ARMGenInstrNames.VLD4d8_UPD,   true,  true,  SingleSpc,  4, 8 ),

      new NEONLdStTableEntry(ARMGenInstrNames.VLD4q16Pseudo_UPD,    ARMGenInstrNames.VLD4q16_UPD, true,  true,  EvenDblSpc, 4, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4q16oddPseudo,     ARMGenInstrNames.VLD4q16,     true,  false, OddDblSpc,  4, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4q16oddPseudo_UPD, ARMGenInstrNames.VLD4q16_UPD, true,  true,  OddDblSpc,  4, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4q32Pseudo_UPD,    ARMGenInstrNames.VLD4q32_UPD, true,  true,  EvenDblSpc, 4, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4q32oddPseudo,     ARMGenInstrNames.VLD4q32,     true,  false, OddDblSpc,  4, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4q32oddPseudo_UPD, ARMGenInstrNames.VLD4q32_UPD, true,  true,  OddDblSpc,  4, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4q8Pseudo_UPD,     ARMGenInstrNames.VLD4q8_UPD,  true,  true,  EvenDblSpc, 4, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4q8oddPseudo,      ARMGenInstrNames.VLD4q8,      true,  false, OddDblSpc,  4, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VLD4q8oddPseudo_UPD,  ARMGenInstrNames.VLD4q8_UPD,  true,  true,  OddDblSpc,  4, 8 ),

      new NEONLdStTableEntry(ARMGenInstrNames.VST1LNq16Pseudo,     ARMGenInstrNames.VST1LNd16,    false, false, EvenDblSpc, 1, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST1LNq16Pseudo_UPD, ARMGenInstrNames.VST1LNd16_UPD,false, true,  EvenDblSpc, 1, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST1LNq32Pseudo,     ARMGenInstrNames.VST1LNd32,    false, false, EvenDblSpc, 1, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST1LNq32Pseudo_UPD, ARMGenInstrNames.VST1LNd32_UPD,false, true,  EvenDblSpc, 1, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST1LNq8Pseudo,      ARMGenInstrNames.VST1LNd8,     false, false, EvenDblSpc, 1, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST1LNq8Pseudo_UPD,  ARMGenInstrNames.VST1LNd8_UPD, false, true,  EvenDblSpc, 1, 8 ),

      new NEONLdStTableEntry(ARMGenInstrNames.VST1d64QPseudo,      ARMGenInstrNames.VST1d64Q,     false, false, SingleSpc,  4, 1 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST1d64QPseudo_UPD,  ARMGenInstrNames.VST1d64Q_UPD, false, true,  SingleSpc,  4, 1 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST1d64TPseudo,      ARMGenInstrNames.VST1d64T,     false, false, SingleSpc,  3, 1 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST1d64TPseudo_UPD,  ARMGenInstrNames.VST1d64T_UPD, false, true,  SingleSpc,  3, 1 ),

      new NEONLdStTableEntry(ARMGenInstrNames.VST1q16Pseudo,       ARMGenInstrNames.VST1q16,      false, false, SingleSpc,  2, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST1q16Pseudo_UPD,   ARMGenInstrNames.VST1q16_UPD,  false, true,  SingleSpc,  2, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST1q32Pseudo,       ARMGenInstrNames.VST1q32,      false, false, SingleSpc,  2, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST1q32Pseudo_UPD,   ARMGenInstrNames.VST1q32_UPD,  false, true,  SingleSpc,  2, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST1q64Pseudo,       ARMGenInstrNames.VST1q64,      false, false, SingleSpc,  2, 1 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST1q64Pseudo_UPD,   ARMGenInstrNames.VST1q64_UPD,  false, true,  SingleSpc,  2, 1 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST1q8Pseudo,        ARMGenInstrNames.VST1q8,       false, false, SingleSpc,  2, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST1q8Pseudo_UPD,    ARMGenInstrNames.VST1q8_UPD,   false, true,  SingleSpc,  2, 8 ),

      new NEONLdStTableEntry(ARMGenInstrNames.VST2LNd16Pseudo,     ARMGenInstrNames.VST2LNd16,     false, false, SingleSpc, 2, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST2LNd16Pseudo_UPD, ARMGenInstrNames.VST2LNd16_UPD, false, true,  SingleSpc, 2, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST2LNd32Pseudo,     ARMGenInstrNames.VST2LNd32,     false, false, SingleSpc, 2, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST2LNd32Pseudo_UPD, ARMGenInstrNames.VST2LNd32_UPD, false, true,  SingleSpc, 2, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST2LNd8Pseudo,      ARMGenInstrNames.VST2LNd8,      false, false, SingleSpc, 2, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST2LNd8Pseudo_UPD,  ARMGenInstrNames.VST2LNd8_UPD,  false, true,  SingleSpc, 2, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST2LNq16Pseudo,     ARMGenInstrNames.VST2LNq16,     false, false, EvenDblSpc, 2, 4),
      new NEONLdStTableEntry(ARMGenInstrNames.VST2LNq16Pseudo_UPD, ARMGenInstrNames.VST2LNq16_UPD, false, true,  EvenDblSpc, 2, 4),
      new NEONLdStTableEntry(ARMGenInstrNames.VST2LNq32Pseudo,     ARMGenInstrNames.VST2LNq32,     false, false, EvenDblSpc, 2, 2),
      new NEONLdStTableEntry(ARMGenInstrNames.VST2LNq32Pseudo_UPD, ARMGenInstrNames.VST2LNq32_UPD, false, true,  EvenDblSpc, 2, 2),

      new NEONLdStTableEntry(ARMGenInstrNames.VST2d16Pseudo,       ARMGenInstrNames.VST2d16,      false, false, SingleSpc,  2, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST2d16Pseudo_UPD,   ARMGenInstrNames.VST2d16_UPD,  false, true,  SingleSpc,  2, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST2d32Pseudo,       ARMGenInstrNames.VST2d32,      false, false, SingleSpc,  2, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST2d32Pseudo_UPD,   ARMGenInstrNames.VST2d32_UPD,  false, true,  SingleSpc,  2, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST2d8Pseudo,        ARMGenInstrNames.VST2d8,       false, false, SingleSpc,  2, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST2d8Pseudo_UPD,    ARMGenInstrNames.VST2d8_UPD,   false, true,  SingleSpc,  2, 8 ),

      new NEONLdStTableEntry(ARMGenInstrNames.VST2q16Pseudo,       ARMGenInstrNames.VST2q16,      false, false, SingleSpc,  4, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST2q16Pseudo_UPD,   ARMGenInstrNames.VST2q16_UPD,  false, true,  SingleSpc,  4, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST2q32Pseudo,       ARMGenInstrNames.VST2q32,      false, false, SingleSpc,  4, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST2q32Pseudo_UPD,   ARMGenInstrNames.VST2q32_UPD,  false, true,  SingleSpc,  4, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST2q8Pseudo,        ARMGenInstrNames.VST2q8,       false, false, SingleSpc,  4, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST2q8Pseudo_UPD,    ARMGenInstrNames.VST2q8_UPD,   false, true,  SingleSpc,  4, 8 ),

      new NEONLdStTableEntry(ARMGenInstrNames.VST3LNd16Pseudo,     ARMGenInstrNames.VST3LNd16,     false, false, SingleSpc, 3, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST3LNd16Pseudo_UPD, ARMGenInstrNames.VST3LNd16_UPD, false, true,  SingleSpc, 3, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST3LNd32Pseudo,     ARMGenInstrNames.VST3LNd32,     false, false, SingleSpc, 3, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST3LNd32Pseudo_UPD, ARMGenInstrNames.VST3LNd32_UPD, false, true,  SingleSpc, 3, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST3LNd8Pseudo,      ARMGenInstrNames.VST3LNd8,      false, false, SingleSpc, 3, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST3LNd8Pseudo_UPD,  ARMGenInstrNames.VST3LNd8_UPD,  false, true,  SingleSpc, 3, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST3LNq16Pseudo,     ARMGenInstrNames.VST3LNq16,     false, false, EvenDblSpc, 3, 4),
      new NEONLdStTableEntry(ARMGenInstrNames.VST3LNq16Pseudo_UPD, ARMGenInstrNames.VST3LNq16_UPD, false, true,  EvenDblSpc, 3, 4),
      new NEONLdStTableEntry(ARMGenInstrNames.VST3LNq32Pseudo,     ARMGenInstrNames.VST3LNq32,     false, false, EvenDblSpc, 3, 2),
      new NEONLdStTableEntry(ARMGenInstrNames.VST3LNq32Pseudo_UPD, ARMGenInstrNames.VST3LNq32_UPD, false, true,  EvenDblSpc, 3, 2),

      new NEONLdStTableEntry(ARMGenInstrNames.VST3d16Pseudo,       ARMGenInstrNames.VST3d16,      false, false, SingleSpc,  3, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST3d16Pseudo_UPD,   ARMGenInstrNames.VST3d16_UPD,  false, true,  SingleSpc,  3, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST3d32Pseudo,       ARMGenInstrNames.VST3d32,      false, false, SingleSpc,  3, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST3d32Pseudo_UPD,   ARMGenInstrNames.VST3d32_UPD,  false, true,  SingleSpc,  3, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST3d8Pseudo,        ARMGenInstrNames.VST3d8,       false, false, SingleSpc,  3, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST3d8Pseudo_UPD,    ARMGenInstrNames.VST3d8_UPD,   false, true,  SingleSpc,  3, 8 ),

      new NEONLdStTableEntry(ARMGenInstrNames.VST3q16Pseudo_UPD,    ARMGenInstrNames.VST3q16_UPD, false, true,  EvenDblSpc, 3, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST3q16oddPseudo,     ARMGenInstrNames.VST3q16,     false, false, OddDblSpc,  3, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST3q16oddPseudo_UPD, ARMGenInstrNames.VST3q16_UPD, false, true,  OddDblSpc,  3, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST3q32Pseudo_UPD,    ARMGenInstrNames.VST3q32_UPD, false, true,  EvenDblSpc, 3, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST3q32oddPseudo,     ARMGenInstrNames.VST3q32,     false, false, OddDblSpc,  3, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST3q32oddPseudo_UPD, ARMGenInstrNames.VST3q32_UPD, false, true,  OddDblSpc,  3, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST3q8Pseudo_UPD,     ARMGenInstrNames.VST3q8_UPD,  false, true,  EvenDblSpc, 3, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST3q8oddPseudo,      ARMGenInstrNames.VST3q8,      false, false, OddDblSpc,  3, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST3q8oddPseudo_UPD,  ARMGenInstrNames.VST3q8_UPD,  false, true,  OddDblSpc,  3, 8 ),

      new NEONLdStTableEntry(ARMGenInstrNames.VST4LNd16Pseudo,     ARMGenInstrNames.VST4LNd16,     false, false, SingleSpc, 4, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST4LNd16Pseudo_UPD, ARMGenInstrNames.VST4LNd16_UPD, false, true,  SingleSpc, 4, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST4LNd32Pseudo,     ARMGenInstrNames.VST4LNd32,     false, false, SingleSpc, 4, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST4LNd32Pseudo_UPD, ARMGenInstrNames.VST4LNd32_UPD, false, true,  SingleSpc, 4, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST4LNd8Pseudo,      ARMGenInstrNames.VST4LNd8,      false, false, SingleSpc, 4, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST4LNd8Pseudo_UPD,  ARMGenInstrNames.VST4LNd8_UPD,  false, true,  SingleSpc, 4, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST4LNq16Pseudo,     ARMGenInstrNames.VST4LNq16,     false, false, EvenDblSpc, 4, 4),
      new NEONLdStTableEntry(ARMGenInstrNames.VST4LNq16Pseudo_UPD, ARMGenInstrNames.VST4LNq16_UPD, false, true,  EvenDblSpc, 4, 4),
      new NEONLdStTableEntry(ARMGenInstrNames.VST4LNq32Pseudo,     ARMGenInstrNames.VST4LNq32,     false, false, EvenDblSpc, 4, 2),
      new NEONLdStTableEntry(ARMGenInstrNames.VST4LNq32Pseudo_UPD, ARMGenInstrNames.VST4LNq32_UPD, false, true,  EvenDblSpc, 4, 2),

      new NEONLdStTableEntry(ARMGenInstrNames.VST4d16Pseudo,       ARMGenInstrNames.VST4d16,      false, false, SingleSpc,  4, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST4d16Pseudo_UPD,   ARMGenInstrNames.VST4d16_UPD,  false, true,  SingleSpc,  4, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST4d32Pseudo,       ARMGenInstrNames.VST4d32,      false, false, SingleSpc,  4, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST4d32Pseudo_UPD,   ARMGenInstrNames.VST4d32_UPD,  false, true,  SingleSpc,  4, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST4d8Pseudo,        ARMGenInstrNames.VST4d8,       false, false, SingleSpc,  4, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST4d8Pseudo_UPD,    ARMGenInstrNames.VST4d8_UPD,   false, true,  SingleSpc,  4, 8 ),

      new NEONLdStTableEntry(ARMGenInstrNames.VST4q16Pseudo_UPD,    ARMGenInstrNames.VST4q16_UPD, false, true,  EvenDblSpc, 4, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST4q16oddPseudo,     ARMGenInstrNames.VST4q16,     false, false, OddDblSpc,  4, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST4q16oddPseudo_UPD, ARMGenInstrNames.VST4q16_UPD, false, true,  OddDblSpc,  4, 4 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST4q32Pseudo_UPD,    ARMGenInstrNames.VST4q32_UPD, false, true,  EvenDblSpc, 4, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST4q32oddPseudo,     ARMGenInstrNames.VST4q32,     false, false, OddDblSpc,  4, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST4q32oddPseudo_UPD, ARMGenInstrNames.VST4q32_UPD, false, true,  OddDblSpc,  4, 2 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST4q8Pseudo_UPD,     ARMGenInstrNames.VST4q8_UPD,  false, true,  EvenDblSpc, 4, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST4q8oddPseudo,      ARMGenInstrNames.VST4q8,      false, false, OddDblSpc,  4, 8 ),
      new NEONLdStTableEntry(ARMGenInstrNames.VST4q8oddPseudo_UPD,  ARMGenInstrNames.VST4q8_UPD,  false, true,  OddDblSpc,  4, 8)
  };

  /**
   * Search the NEONLdStTable for information about a NEON load or store pseudo instruction.
   * @param opcode
   * @return
   */
  static NEONLdStTableEntry lookupNEONLdSt(int opcode) {
    int idx = Arrays.binarySearch(NEONLdStTable, new NEONLdStTableEntry(opcode, 0, false, false, SingleSpc, 0, 0));
    if (idx < 0) return null;
    return NEONLdStTable[idx];
  }

  /**
   * Get 4 D subregisters of a Q, QQ, or QQQQ register,
   * corresponding to the specified register spacing.  Not all of the results
   * are necessarily valid, e.g., a Q register only has 2 D subregisters.
   * @param Reg
   * @param RegSpc
   */
  private static int[] getDSubRegs(int Reg,
                                   NEONRegSpacing RegSpc,
                                   TargetRegisterInfo tri) {
    int D0, D1, D2, D3;
    if (RegSpc == SingleSpc) {
      D0 = tri.getSubReg(Reg, ARMGenRegisterInfo.dsub_0);
      D1 = tri.getSubReg(Reg, ARMGenRegisterInfo.dsub_1);
      D2 = tri.getSubReg(Reg, ARMGenRegisterInfo.dsub_2);
      D3 = tri.getSubReg(Reg, ARMGenRegisterInfo.dsub_3);
    } else if (RegSpc == EvenDblSpc) {
      D0 = tri.getSubReg(Reg, ARMGenRegisterInfo.dsub_0);
      D1 = tri.getSubReg(Reg, ARMGenRegisterInfo.dsub_2);
      D2 = tri.getSubReg(Reg, ARMGenRegisterInfo.dsub_4);
      D3 = tri.getSubReg(Reg, ARMGenRegisterInfo.dsub_6);
    } else {
      Util.assertion(RegSpc == OddDblSpc, "unknown register spacing");
      D0 = tri.getSubReg(Reg, ARMGenRegisterInfo.dsub_1);
      D1 = tri.getSubReg(Reg, ARMGenRegisterInfo.dsub_3);
      D2 = tri.getSubReg(Reg, ARMGenRegisterInfo.dsub_5);
      D3 = tri.getSubReg(Reg, ARMGenRegisterInfo.dsub_7);
    }
    return new int[] {D0, D1, D2, D3};
  }
  
  /**
   * Translate VST pseudo instructions with Q, QQ or QQQQ register
   * operands to real VST instructions with D register operands.
   * @param mbb
   * @param mbbi
   */
  private void expandVST(MachineBasicBlock mbb, int mbbi) {
    MachineInstr mi = mbb.getInstAt(mbbi);
    NEONLdStTableEntry tableEntry = lookupNEONLdSt(mi.getOpcode());
    Util.assertion(tableEntry != null, "NEONLdStTable lookup failed!");
    NEONRegSpacing regSpc = tableEntry.regSpacing;
    int numRegs = tableEntry.numRegs;

    MachineInstrBuilder mib = buildMI(mbb, mbbi, mi.getDebugLoc(), tii.get(tableEntry.realOpc));
    int opIdx = 0;
    if (tableEntry.hasWriteBack)
      mib.addOperand(mi.getOperand(opIdx++).clone());

    // copy the addrmode operands.
    mib.addOperand(mi.getOperand(opIdx++).clone());
    mib.addOperand(mi.getOperand(opIdx++).clone());
    // copy the am offset operand.
    if (tableEntry.hasWriteBack)
      mib.addOperand(mi.getOperand(opIdx++).clone());

    boolean srcIsKill = mi.getOperand(opIdx).isKill();
    int srcReg = mi.getOperand(opIdx++).getReg();
    int[] regs = getDSubRegs(srcReg, regSpc, tri);
    int d0 = regs[0], d1 = regs[1], d2 = regs[2], d3 = regs[3];
    mib.addReg(d0).addReg(d1);
    if (numRegs > 2)
      mib.addReg(d2);
    if (numRegs > 3)
      mib.addReg(d3);

    // copy the predicate operands.
    mib.addOperand(mi.getOperand(opIdx++).clone());
    mib.addOperand(mi.getOperand(opIdx++).clone());

    if (srcIsKill)
      mib.getMInstr().addRegisterKilled(srcReg, tri, true);
    transferTmpOps(mi, mib, mib);

    mib.setMemRefs(mi.getMemOperands());
    mi.removeFromParent();
  }

  /**
   * Translate VLD pseudo instructions with Q, QQ, or QQQQ register operands to real VLD 
   * instruction with D register operands.
   * @param mbb
   * @param mbbi
   */
  private void expandVLD(MachineBasicBlock mbb, int mbbi) {
    MachineInstr mi = mbb.getInstAt(mbbi);
    NEONLdStTableEntry tableEntry = lookupNEONLdSt(mi.getOpcode());
    Util.assertion(tableEntry != null && tableEntry.isLoad, "NEONLdStTable lookup failed");
    NEONRegSpacing regSpc = tableEntry.regSpacing;
    int numRegs = tableEntry.numRegs;

    MachineInstrBuilder mib = buildMI(mbb, mbbi, mi.getDebugLoc(), tii.get(tableEntry.realOpc));
    int opIdx = 0;
    boolean dstIsDead = mi.getOperand(opIdx).isDead();
    int dstReg = mi.getOperand(opIdx++).getReg();
    int[] regs = getDSubRegs(dstReg, regSpc, tri);
    int d0 = regs[0], d1 = regs[1], d2 = regs[2], d3 = regs[3];
    mib.addReg(d0, getDefRegState(true) | getDeadRegState(dstIsDead))
        .addReg(d1, getDefRegState(true) | getDeadRegState(dstIsDead));

    if (numRegs > 2)
      mib.addReg(d2, getDefRegState(true) | getDeadRegState(dstIsDead));
    if (numRegs > 3)
      mib.addReg(d3, getDefRegState(true) | getDeadRegState(dstIsDead));

    if (tableEntry.hasWriteBack)
      mib.addOperand(mi.getOperand(opIdx++));

    // Copy the addrmode6 operands.
    mib.addOperand(mi.getOperand(opIdx++));
    mib.addOperand(mi.getOperand(opIdx++));
    // Copy the am6offset operand.
    if (tableEntry.hasWriteBack)
      mib.addOperand(mi.getOperand(opIdx++));

    // For an instruction writing double-spaced subregs, the pseudo instruction
    // has an extra operand that is a use of the super-register.  Record the
    // operand index and skip over it.
    int SrcOpIdx = 0;
    if (regSpc == EvenDblSpc || regSpc == OddDblSpc)
      SrcOpIdx = opIdx++;

    // Copy the predicate operands.
    mib.addOperand(mi.getOperand(opIdx++));
    mib.addOperand(mi.getOperand(opIdx));

    // Copy the super-register source operand used for double-spaced subregs over
    // to the new instruction as an implicit operand.
    if (SrcOpIdx != 0) {
      MachineOperand MO = mi.getOperand(SrcOpIdx);
      MO.setImplicit(true);
      mib.addOperand(MO);
    }
    // Add an implicit def for the super-register.
    mib.addReg(dstReg,  getImplRegState(true)|getDeadRegState(true) | getDeadRegState(dstIsDead));
    transferTmpOps(mi, mib, mib);

    // Transfer memoperands.
    mib.getMInstr().setMemOperands(mi.getMemOperands());
    mi.removeFromParent();
  }

  private void expandMOV32BitImm(MachineBasicBlock mbb, int mbbi) {
    MachineInstr mi = mbb.getInstAt(mbbi);
    int opcode = mi.getOpcode();
    int pregReg = 0;
    OutRef<Integer> x = new OutRef<>(0);
    ARMCC.CondCodes pred = ((ARMRegisterInfo)tri).getInstrPredicate(mi, x);
    pregReg = x.get();
    int destReg = mi.getOperand(0).getReg();
    boolean destIsDead = mi.getOperand(0).isDead();
    boolean isCC = opcode == ARMGenInstrNames.MOVCCi32imm || opcode == ARMGenInstrNames.t2MOVCCi32imm;
    MachineOperand mo = mi.getOperand(isCC ? 2 : 1);
    MachineInstrBuilder lo16, hi16;

    if (!subtarget.hasV6T2Ops() &&
        (opcode == ARMGenInstrNames.MOVi32imm || opcode == ARMGenInstrNames.MOVCCi32imm)) {
      // Expand into a movi + orr.
      lo16 = buildMI(mbb, mbbi++, mi.getDebugLoc(), tii.get(ARMGenInstrNames.MOVi), destReg);
      hi16 = buildMI(mbb, mbbi++, mi.getDebugLoc(), tii.get(ARMGenInstrNames.ORRri))
          .addReg(destReg, getDefRegState(true) | getDeadRegState(destIsDead))
          .addReg(destReg);

      Util.assertion(mo.isImm(), "MOVi32imm w/non immedate source operand");
      int immVal = (int) mo.getImm();
      int soImmVal1 = ARM_AM.getSOImmTwoPartFirst(immVal);
      int soImmVal2 = ARM_AM.getSOImmTwoPartSecond(immVal);
      lo16 = lo16.addImm(soImmVal1);
      hi16 = hi16.addImm(soImmVal2);
      lo16.setMemRefs(mi.getMemOperands());
      hi16.setMemRefs(mi.getMemOperands());
      lo16.addImm(pred.ordinal()).addReg(pregReg).addReg(0);
      hi16.addImm(pred.ordinal()).addReg(pregReg).addReg(0);
      transferTmpOps(mi, lo16, hi16);
      mi.removeFromParent();
      return;
    }

    int lo16Opc = 0, hi16Opc = 0;
    if (opcode == ARMGenInstrNames.t2MOVi32imm || opcode == ARMGenInstrNames.t2MOVCCi32imm) {
      lo16Opc = ARMGenInstrNames.t2MOVi16;
      hi16Opc = ARMGenInstrNames.t2MOVTi16;
    }
    else {
      lo16Opc = ARMGenInstrNames.MOVi16;
      hi16Opc = ARMGenInstrNames.MOVTi16;
    }

    lo16 = buildMI(mbb, mbbi++, mi.getDebugLoc(), tii.get(lo16Opc), destReg);
    hi16 = buildMI(mbb, mbbi++, mi.getDebugLoc(), tii.get(hi16Opc))
        .addReg(destReg, getDefRegState(true) | getDeadRegState(destIsDead))
        .addReg(destReg);

    if (mo.isImm()) {
      int imm = (int) mo.getImm();
      int lo16Imm = imm & 0xffff;
      int hi16Imm = (imm >>> 16) & 0xffff;
      lo16 = lo16.addImm(lo16Imm);
      hi16 = hi16.addImm(hi16Imm);
    }
    else {
      GlobalValue gv = mo.getGlobal();
      int tf = mo.getTargetFlags();
      lo16 = lo16.addGlobalAddress(gv, mo.getOffset(), tf | ARMII.MO_LO16);
      hi16 = hi16.addGlobalAddress(gv, mo.getOffset(), tf | ARMII.MO_HI16);
    }

    lo16.setMemRefs(mi.getMemOperands());
    hi16.setMemRefs(mi.getMemOperands());
    lo16.addImm(pred.ordinal()).addReg(pregReg);
    hi16.addImm(pred.ordinal()).addReg(pregReg);
    transferTmpOps(mi, lo16, hi16);
    mi.removeFromParent();
  }

  /**
   * Translate VLD*LN and VST*LN instruction with Q, QQ, or QQQQ register operands
   * to real instructions with D register operands.
   * @param mbb
   * @param mbbi
   */
  private void expandLaneOp(MachineBasicBlock mbb, int mbbi) {
    MachineInstr mi = mbb.getInstAt(mbbi);
    NEONLdStTableEntry tableEntry = lookupNEONLdSt(mi.getOpcode());
    Util.assertion(tableEntry != null, "NEONLdStTable lookup failed");
    NEONRegSpacing regSpc = tableEntry.regSpacing;
    int numRegs = tableEntry.numRegs;
    int regElts = tableEntry.regElts;

    MachineInstrBuilder mib = buildMI(mbb, mbbi, mi.getDebugLoc(), tii.get(tableEntry.realOpc));
    int opIdx = 0;
    // The lane operand is always the 3rd from last operand, before the 2
    // predicate operands.
    int lane = (int) mi.getOperand(mi.getDesc().getNumOperands() - 3).getImm();

    // Adjust the lane and spacing as needed for Q registers.
    Util.assertion(regSpc != OddDblSpc, "unexpected register spacing for VLD/VST-lane");
    if (regSpc == EvenDblSpc && lane >= regElts) {
      regSpc = OddDblSpc;
      lane -= regElts;
    }

    Util.assertion(lane < regElts, "out of range lane for VLD/VST-lane");
    int destReg = 0;
    int d0 = 0, d1 = 0, d2 = 0, d3 = 0;
    boolean dstIsDead = false;
    if (tableEntry.isLoad) {
      dstIsDead = mi.getOperand(opIdx).isDead();
      destReg = mi.getOperand(opIdx++).getReg();
      int[] regs = getDSubRegs(destReg, regSpc, tri);
      d0 = regs[0];
      d1 = regs[1];
      d2 = regs[2];
      d3 = regs[3];
      mib.addReg(d0, getDefRegState(true) | getDeadRegState(dstIsDead));
      if (numRegs > 1)
        mib.addReg(d1, getDefRegState(true) | getDeadRegState(dstIsDead));
      if (numRegs > 2)
        mib.addReg(d2, getDefRegState(true) | getDeadRegState(dstIsDead));
      if (numRegs > 3)
        mib.addReg(d3, getDefRegState(true) | getDeadRegState(dstIsDead));
    }

    if (tableEntry.hasWriteBack)
      mib.addOperand(mi.getOperand(opIdx++).clone());

    // copy the addrmode6 operands.
    mib.addOperand(mi.getOperand(opIdx++).clone());
    mib.addOperand(mi.getOperand(opIdx++).clone());

    // copy the am6offset operand.
    if (tableEntry.hasWriteBack)
      mib.addOperand(mi.getOperand(opIdx++).clone());

    // grab the super-register source.
    MachineOperand mo = mi.getOperand(opIdx++);
    if (!tableEntry.isLoad) {
      int[] regs = getDSubRegs(mo.getReg(), regSpc, tri);
      d0 = regs[0];
      d1 = regs[1];
      d2 = regs[2];
      d3 = regs[3];
    }

    // add the subregs as sources of the new instruction.
    int srcFlags = getUndefRegState(mo.isUndef()) | getKillRegState(mo.isKill());
    mib.addReg(d0, srcFlags);
    if (numRegs > 1)
      mib.addReg(d1, srcFlags);
    if (numRegs > 2)
      mib.addReg(d2, srcFlags);
    if (numRegs > 3)
      mib.addReg(d3, srcFlags);

    // add the lane number operand.
    mib.addImm(lane);
    ++opIdx;

    // copy the predicate operands.
    mib.addOperand(mi.getOperand(opIdx++).clone());
    mib.addOperand(mi.getOperand(opIdx++).clone());

    // copy the super-register source to be an implicit source.
    mo.setImplicit(true);
    mib.addOperand(mo.clone());
    if (tableEntry.isLoad)
      // add an implicit def for the super-register.
      mib.addReg(destReg, getImplRegState(true)|getDeadRegState(dstIsDead)|getDefRegState(true));

    transferTmpOps(mi, mib, mib);
    mi.removeFromParent();
  }

  private void expandVTBL(MachineBasicBlock mbb, int mbbi, int opc, boolean isExt, int numRegs) {
    MachineInstr mi = mbb.getInstAt(mbbi);

    MachineInstrBuilder mib = buildMI(mbb, mbbi, mi.getDebugLoc(), tii.get(opc));
    int opIdx = 0;
    mib.addOperand(mi.getOperand(opIdx++).clone());
    if (isExt)
      mib.addOperand(mi.getOperand(opIdx++).clone());

    boolean srcIsKill = mi.getOperand(opIdx).isKill();
    int srcReg = mi.getOperand(opIdx++).getReg();
    int[] regs = getDSubRegs(srcReg, SingleSpc, tri);
    int d0 = regs[0], d1 = regs[1], d2 = regs[2], d3 = regs[3];
    mib.addReg(d0).addReg(d1);
    if (numRegs > 2)
      mib.addReg(d2);
    if (numRegs > 3)
      mib.addReg(d3);

    // copy the other source register operands.
    mib.addOperand(mi.getOperand(opIdx++).clone());

    // copy the predicate operands.
    mib.addOperand(mi.getOperand(opIdx++).clone());
    mib.addOperand(mi.getOperand(opIdx++).clone());

    if (srcIsKill)
      mib.getMInstr().addRegisterKilled(srcReg, tri, true);

    transferTmpOps(mi, mib, mib);
    mi.removeFromParent();
  }

  private static void transferTmpOps(MachineInstr oldMI,
                                     MachineInstrBuilder useMI,
                                     MachineInstrBuilder defMI) {
    MCInstrDesc mcid = oldMI.getDesc();
    for (int i = mcid.getNumOperands(), e = oldMI.getNumOperands(); i != e; ++i) {
      MachineOperand mo = oldMI.getOperand(i);
      Util.assertion(mo.isRegister() && mo.getReg() != 0);
      if (mo.isUse())
        useMI.addOperand(mo);
      else
        useMI.addOperand(mo);
    }
  }
}
