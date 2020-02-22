/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.codegen;

import backend.analysis.MachineDomTree;
import backend.analysis.MachineLoopInfo;
import backend.codegen.MachineOperand.RegState;
import backend.debug.DebugLoc;
import backend.mc.MCRegisterClass;
import backend.pass.AnalysisUsage;
import backend.pass.FunctionPass;
import backend.support.MachineFunctionPass;
import backend.target.TargetInstrInfo;
import backend.target.TargetOpcode;
import backend.target.TargetRegisterInfo;
import tools.Util;

import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.codegen.MachineOperand.createReg;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class ExpandPostRAPseduos extends MachineFunctionPass {
  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    au.setPreservesCFG();
    au.addPreserved(MachineLoopInfo.class);
    au.addPreserved(MachineDomTree.class);
    super.getAnalysisUsage(au);
  }

  private int curPos;
  private TargetRegisterInfo tri;

  @Override
  public boolean runOnMachineFunction(MachineFunction mf) {
    if (Util.DEBUG) {
      System.err.println("********** Expanding pseudo instructions **********");
      System.err.printf("********** Function: %s%n", mf.getFunction().getName());
    }
    tri = mf.getTarget().getSubtarget().getRegisterInfo();
    boolean madeChange = false;
    for (MachineBasicBlock mbb : mf.getBasicBlocks()) {
      for (int i = 0; i < mbb.size(); i++) {
        curPos = i;
        MachineInstr mi = mbb.getInstAt(i);
        switch (mi.getOpcode()) {
          case TargetOpcode.EXTRACT_SUBREG:
            madeChange |= lowerExtract(mi);
            break;
          case TargetOpcode.INSERT_SUBREG:
            madeChange |= lowerInsert(mi);
            break;
          case TargetOpcode.SUBREG_TO_REG:
            madeChange |= lowerSubregToReg(mi);
            break;
          case TargetOpcode.COPY:
            madeChange |= lowerCopy(mi);
            break;
        }
        i = curPos;
      }
    }
    return madeChange;
  }

  @Override
  public String getPassName() {
    return "Post-RA pseudo instruction expansion pass";
  }

  private boolean lowerExtract(MachineInstr mi) {
    MachineBasicBlock mbb = mi.getParent();
    MachineFunction mf = mbb.getParent();
    TargetRegisterInfo tri = mf.getSubtarget().getRegisterInfo();
    TargetInstrInfo tii = mf.getSubtarget().getInstrInfo();

    MachineOperand mo0 = mi.getOperand(0), mo1 = mi.getOperand(1);
    Util.assertion(mo0.isRegister() && mo0.isDef() && mo1.isRegister() && mo1.isUse() &&
        mi.getOperand(2).isImm(), "Malformed extract_subreg");


    int destReg = mo0.getReg();
    int superReg = mo1.getReg();
    int subIdx = (int) mi.getOperand(2).getImm();
    int srcReg = tri.getSubReg(superReg, subIdx);

    Util.assertion(TargetRegisterInfo.isPhysicalRegister(destReg), "Extract superreg source must be a physical register");

    Util.assertion(TargetRegisterInfo.isPhysicalRegister(destReg), "Extract superreg dest must be a physical register");


    if (srcReg == destReg) {
      if (mo1.isKill()) {
        mi.setDesc(tii.get(TargetOpcode.IMPLICIT_DEF));
        mi.removeOperand(2);
        if (Util.DEBUG) {
          System.err.print("subreg: replace by: ");
          mi.dump();
          return true;
        }
      }
      --curPos;
      if (Util.DEBUG)
        System.err.print("subreg: eliminated!");
    } else {
      // insert a copy.
      MCRegisterClass srcRC = tri.getPhysicalRegisterRegClass(srcReg);
      MCRegisterClass destRC = tri.getPhysicalRegisterRegClass(destReg);
      boolean emitted = tii.copyPhysReg(mbb, mi.getIndexInMBB(), destReg, srcReg,
          destRC, srcRC);
      Util.assertion(emitted, "Subreg and dest must be of compatible register class!");
      if (mo0.isDead())
        transferDeadFlag(mi, destReg, tri);
      if (mo1.isKill())
        transferKillFlag(mi, superReg, tri, true);
    }
    if (Util.DEBUG)
      System.err.println();
    mbb.remove(mi);
    return true;
  }

  private boolean lowerInsert(MachineInstr mi) {
    MachineBasicBlock mbb = mi.getParent();
    MachineFunction mf = mbb.getParent();
    TargetRegisterInfo tri = mf.getSubtarget().getRegisterInfo();
    TargetInstrInfo tii = mf.getSubtarget().getInstrInfo();

    MachineOperand mo0 = mi.getOperand(0),
        mo1 = mi.getOperand(1),
        mo2 = mi.getOperand(2),
        mo3 = mi.getOperand(3);

    Util.assertion(mo0.isRegister() && mo0.isDef() && mo1.isRegister() && mo1.isUse() &&
        mo2.isRegister() && mo2.isUse() &&
        mo3.isImm(), "Malformed insert_subreg");


    int destReg = mo0.getReg();
    int srcReg = mo1.getReg();
    int insReg = mo2.getReg();
    int subIdx = (int) mo3.getImm();

    Util.assertion(destReg == srcReg, "insert_subreg not a two-address instruction?");
    Util.assertion(subIdx > 0, "Invalid index for insert_subreg");
    int destSubReg = tri.getSubReg(srcReg, subIdx);
    Util.assertion(destSubReg > 0, "Invalid subregister!");
    Util.assertion(TargetRegisterInfo.isPhysicalRegister(srcReg), "insert superreg source must be a physical register");

    Util.assertion(TargetRegisterInfo.isPhysicalRegister(insReg), "inserted value must be a physical register");


    if (destSubReg == insReg) {
      // No need to insert an identity copy instruction. If the SrcReg was
      // <undef>, we need to make sure it is alive by inserting an IMPLICIT_DEF
      if (mo1.isUndef() && !mo0.isDead()) {
        MachineInstrBuilder mib = buildMI(mbb, mi.getIndexInMBB(),
            new DebugLoc(),
            tii.get(TargetOpcode.IMPLICIT_DEF), destReg);
        if (mo2.isUndef())
          mib.addReg(insReg, RegState.Implicit | RegState.Undef);
        else
          mib.addReg(insReg, RegState.ImplicitKill);
      } else {
        if (Util.DEBUG)
          System.err.print("subreg: eliminated!");
        --curPos;
        mbb.remove(mi);
        return true;
      }
    } else {
      // Insert sub-register copy
      MCRegisterClass srcRC = tri.getPhysicalRegisterRegClass(insReg);
      MCRegisterClass destRC = tri.getPhysicalRegisterRegClass(destSubReg);
      if (mo2.isUndef())
        buildMI(mbb, mi.getIndexInMBB(), new DebugLoc(), tii.get(TargetOpcode.IMPLICIT_DEF), destSubReg);
      else {
        boolean emitted = tii.copyPhysReg(mbb, mi.getIndexInMBB(), destSubReg,
            insReg, destRC, srcRC);
        Util.assertion(emitted, "Subreg and dest must be of compatible register class!");
      }
      MachineInstr copyMI = mi.getParent().getInstAt(mi.getIndexInMBB() - 1);
      Util.assertion(copyMI != null);
      if (!mo1.isUndef())
        copyMI.addOperand(createReg(destReg, false, true, true, false, false, false, 0));

      // Transfer the kill/dead flags, if needed.
      if (mo0.isDead())
        transferDeadFlag(mi, destSubReg, tri);
      else {
        // Make sure the full DstReg is live after this replacement.
        copyMI.addOperand(createReg(destReg, true, true));
      }
      if (mo2.isKill() && !mo2.isUndef())
        transferKillFlag(mi, insReg, tri);
    }

    if (Util.DEBUG)
      System.err.println();
    mbb.remove(mi);
    return true;
  }

  private boolean lowerSubregToReg(MachineInstr mi) {
    MachineBasicBlock mbb = mi.getParent();
    MachineFunction mf = mbb.getParent();
    TargetRegisterInfo tri = mf.getSubtarget().getRegisterInfo();
    TargetInstrInfo tii = mf.getSubtarget().getInstrInfo();

    MachineOperand mo0 = mi.getOperand(0),
        mo1 = mi.getOperand(1),
        mo2 = mi.getOperand(2),
        mo3 = mi.getOperand(3);

    Util.assertion(mo0.isRegister() && mo0.isDef() && mo1.isImm() &&
        mo2.isRegister() && mo2.isUse() &&
        mo3.isImm(), "Malformed insert_subreg");

    int destReg = mo0.getReg();
    int insReg = mo2.getReg();
    int insSIdx = mo2.getSubReg();
    int subIdx = (int) mo3.getImm();
    Util.assertion(subIdx > 0, "Invalid index for insert_subreg!");
    int destSubReg = tri.getSubReg(destReg, subIdx);
    if (destSubReg == insReg && insSIdx == 0) {
      // No need to insert an identify copy instruction.
      // Watch out for case like this:
      // %RAX<def> = ...
      // %RAX<def> = SUBREG_TO_REG 0, %EAX:4<kill>, 4
      // The first def is defining RAX, not EAX so the top bits were not
      // zero extended.
      --curPos;
      if (Util.DEBUG)
        System.err.print("subreg: eliminated!");
    } else {
      MCRegisterClass destRC = tri.getPhysicalRegisterRegClass(destSubReg);
      MCRegisterClass srcRC = tri.getPhysicalRegisterRegClass(insReg);
      tii.copyPhysReg(mbb, mi.getIndexInMBB(), destSubReg, insReg, destRC, srcRC);
      if (mo0.isDead())
        transferDeadFlag(mi, destSubReg, tri);
      if (mo2.isKill())
        transferKillFlag(mi, insReg, tri);
    }

    if (Util.DEBUG) System.err.println();
    mbb.remove(mi);
    return true;
  }

  /**
   * mi is a pseduo-instruction with srcReg killed and lowered replacement
   * instruction immediately precede it. Mark the replacement instruction
   * with dead flag.
   *
   * @param mi
   * @param destReg
   * @param tri
   */
  private void transferDeadFlag(MachineInstr mi, int destReg, TargetRegisterInfo tri) {
    MachineBasicBlock mbb = mi.getParent();
    for (int i = mi.getIndexInMBB() - 1; i >= 0 ; i--) {
      if (mbb.getInstAt(i).addRegisterDead(destReg, tri))
        break;
      Util.assertion(i != 0, "copyPhysReg doesn't reference destination register!");
    }
  }

  private void transferKillFlag(MachineInstr mi, int srcReg, TargetRegisterInfo tri) {
    transferKillFlag(mi, srcReg, tri, false);
  }

  /**
   * mi is a pseduo-instruction with srcReg killed and lowered replacement
   * instruction immediately precede it. Mark the replacement instruction
   * with kill flag.
   *
   * @param mi
   * @param srcReg
   * @param tri
   * @param addIfNotFound
   */
  private void transferKillFlag(MachineInstr mi,
                                int srcReg, TargetRegisterInfo tri,
                                boolean addIfNotFound) {
    MachineBasicBlock mbb = mi.getParent();
    for (int i = mi.getIndexInMBB() - 1; ; i--) {
      if (mbb.getInstAt(i).addRegisterKilled(srcReg, tri, addIfNotFound))
        break;
      Util.assertion(i != 0, "copyPhysReg doesn't reference source register!");
    }
  }

  private boolean lowerCopy(MachineInstr mi) {
    MachineOperand destMO = mi.getOperand(0);
    MachineOperand srcMO = mi.getOperand(1);
    Util.assertion(destMO.isRegister() && srcMO.isRegister());
    if (srcMO.getReg() == destMO.getReg()) {
      // eliminate identical copy.
      mi.removeFromParent();
      --curPos;
      return true;
    }

    if (destMO.isDead())
      transferDeadFlag(mi, destMO.getReg(), tri);
    if (mi.getNumOperands() > 2)
      transferImplicitDefs(mi);
    mi.removeFromParent();
    --curPos;
    return true;
  }

  /**
   * Transfer implicitly defined register of a COPY pseudo instruction to the proceeding instruction.
   * @param mi
   */
  private void transferImplicitDefs(MachineInstr mi) {
    int pos = mi.getIndexInMBB();
    --pos;
    MachineInstr copyMI = mi.getParent().getInstAt(pos);
    for (int i = 0, e = mi.getNumOperands(); i < e; ++i) {
      MachineOperand mo = mi.getOperand(i);
      if (!mo.isRegister() || !mo.isImplicit() || mo.isUse())
        continue;
      copyMI.addOperand(MachineOperand.createReg(mo.getReg(), true, true));
    }
  }

  public static FunctionPass createLowerSubregPass() {
    return new ExpandPostRAPseduos();
  }
}
