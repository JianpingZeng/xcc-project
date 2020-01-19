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

import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineFunction;
import backend.codegen.MachineInstr;
import backend.support.MachineFunctionPass;
import backend.target.TargetInstrInfo;

import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.codegen.MachineInstrBuilder.getKillRegState;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ARMExpandPseudoInsts extends MachineFunctionPass {
  private ARMFunctionInfo afi;
  private TargetInstrInfo tii;

  static MachineFunctionPass createARMExpandPseudoPass() {
    return new ARMExpandPseudoInsts();
  }

  @Override
  public boolean runOnMachineFunction(MachineFunction mf) {
    boolean changed = false;
    afi = (ARMFunctionInfo) mf.getInfo();
    tii = mf.getTarget().getInstrInfo();

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
    switch (mi.getOpcode()) {
      case ARMGenInstrNames.t2MOVCCr:
      case ARMGenInstrNames.MOVCCr: {
        int opc = afi.isThumbFunction()? ARMGenInstrNames.t2MOVr : ARMGenInstrNames.MOVr;
        buildMI(mbb, mbbi, mi.getDebugLoc(), tii.get(opc), mi.getOperand(1).getReg())
            .addReg(mi.getOperand(2).getReg(), getKillRegState(mi.getOperand(2).isKill()))
            .addImm(mi.getOperand(3).getImm()) // pred
            .addReg(mi.getOperand(4).getReg()) // pred reg.
            .addReg(0); // 's' bit

        mi.removeFromParent();
        return true;
      }
      case ARMGenInstrNames.t2MOVCCi:
      case ARMGenInstrNames.MOVCCi: {
        int opc = afi.isThumbFunction()? ARMGenInstrNames.t2MOVi : ARMGenInstrNames.MOVi;
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
    }
    return false;
  }
}
