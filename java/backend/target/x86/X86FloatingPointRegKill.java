/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

package backend.target.x86;

import backend.analysis.MachineDomTree;
import backend.analysis.MachineLoop;
import backend.codegen.*;
import backend.pass.AnalysisUsage;
import backend.support.IntStatistic;
import backend.support.LLVMContext;
import backend.support.MachineFunctionPass;
import backend.mc.MCRegisterClass;
import backend.target.TargetInstrInfo;
import backend.utils.SuccIterator;
import backend.value.BasicBlock;
import backend.value.Instruction.PhiNode;

import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.target.TargetRegisterInfo.isVirtualRegister;
import static backend.target.x86.X86GenInstrNames.FP_REG_KILL;
import static backend.target.x86.X86GenRegisterInfo.*;

/**
 * This class defines a pass used for inserting FP_REG_KILL instruction before TerminatorInstr at
 * each machine basic block wherever FP register is used.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class X86FloatingPointRegKill extends MachineFunctionPass {
  public static final IntStatistic NumFPKills =
      new IntStatistic("NumFPKills", "Number of inserted FP kill");

  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    au.setPreservesCFG();
    au.addPreserved(MachineLoop.class);
    au.addPreserved(MachineDomTree.class);
    super.getAnalysisUsage(au);
  }

  @Override
  public boolean runOnMachineFunction(MachineFunction mf) {
    // If no FP stack register used in the mf, fast exit!
    MachineRegisterInfo mri = mf.getMachineRegisterInfo();
    TargetInstrInfo tii = mf.getTarget().getInstrInfo();

    if (mri.getRegClassVirReg(RFP80RegisterClass).isEmpty() &&
        mri.getRegClassVirReg(RFP64RegisterClass).isEmpty() &&
        mri.getRegClassVirReg(RFP32RegisterClass).isEmpty())
      return false;

    boolean changed = false;

    X86Subtarget subtarget = (X86Subtarget) mf.getTarget().getSubtarget();
    for (int i = 0, e = mf.getNumBlocks(); i < e; i++) {
      MachineBasicBlock mbb = mf.getMBBAt(i);
      // if this mbb is return mbb, ignore it. We don't want to insert an FP_REG_KILL
      // before the return.
      if (!mbb.isEmpty() && mbb.getLastInst().isReturn()) {
        continue;
      }

      boolean containsFPCode = false;
      for (int j = 0, end = mbb.size(); j < end && !containsFPCode; j++) {
        MachineInstr mi = mbb.getInstAt(j);
        if (mi.getNumOperands() != 0 && mi.getOperand(0).isRegister()) {
          MCRegisterClass rc;
          for (int op = 0; op < mi.getNumOperands(); op++) {
            MachineOperand mo = mi.getOperand(op);
            if (mo.isRegister() && mo.isDef() && mo.getReg() != 0 &&
                isVirtualRegister(mo.getReg()) &&
                (rc = mri.getRegClass(mo.getReg())) != null &&
                (rc == RFP80RegisterClass ||
                    rc == RFP64RegisterClass ||
                    rc == RFP32RegisterClass))
              containsFPCode = true;
          }
        }
      }

      if (!containsFPCode) {
        BasicBlock llvmBB = mbb.getBasicBlock();
        for (SuccIterator itr = llvmBB.succIterator(); itr.hasNext() && !containsFPCode; ) {
          BasicBlock succBB = itr.next();
          PhiNode inst;
          for (int j = 0, sz = succBB.size(); j < sz && succBB.getInstAt(j) instanceof PhiNode; j++) {
            inst = (PhiNode) succBB.getInstAt(j);
            if (inst.getType().equals(LLVMContext.X86_FP80Ty) ||
                (!subtarget.hasSSE1() && inst.getType().isFloatingPoint()) ||
                (!subtarget.hasSSE2() && inst.getType().equals(LLVMContext.DoubleTy))) {
              containsFPCode = true;
              break;
            }
          }
        }
      }
      if (containsFPCode) {
        buildMI(mbb, mbb.getFirstTerminator(), tii.get(FP_REG_KILL));
        NumFPKills.inc();
        changed = true;
      }
    }
    return changed;
  }

  @Override
  public String getPassName() {
    return "X86 FP_REG_KILL inserter";
  }

  public static MachineFunctionPass createX86FPRegKillPass() {
    return new X86FloatingPointRegKill();
  }
}
