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
import backend.codegen.MachineFunction;
import backend.debug.DebugLoc;
import backend.pass.FunctionPass;
import backend.support.MachineFunctionPass;
import backend.target.TargetInstrInfo;
import backend.target.TargetMachine;
import backend.target.TargetOpcode;

import static backend.codegen.MachineInstrBuilder.buildMI;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class MipsEmitGPRestorePass extends MachineFunctionPass {
  private MipsTargetMachine tm;
  MipsEmitGPRestorePass(MipsTargetMachine tm) {
    this.tm = tm;
  }

  public static FunctionPass createMipsEmitGPRestorePass(MipsTargetMachine tm) {
    return new MipsEmitGPRestorePass(tm);
  }

  @Override
  public boolean runOnMachineFunction(MachineFunction mf) {
    if (tm.getRelocationModel() != TargetMachine.RelocModel.PIC_)
      return false;

    TargetInstrInfo tii = tm.getInstrInfo();
    boolean changed = false;

    int fi = ((MipsFunctionInfo)mf.getInfo()).getGPFI();
    for (int i = 0, e = mf.getNumBlocks(); i < e; ++i) {
      MachineBasicBlock mbb = mf.getMBBAt(i);
      int itr = 0;
      if (mbb.isLandingPad()) {
        for (; mbb.getInstAt(itr).getOpcode() != TargetOpcode.EH_LABEL; ++itr);

        // insert lw
        ++itr;
        DebugLoc dl = itr != mbb.size() ? mbb.getInstAt(itr).getDebugLoc() : new DebugLoc();
        buildMI(mbb, itr, dl, tii.get(MipsGenInstrNames.LW), MipsGenRegisterNames.GP)
            .addFrameIndex(fi).addImm(0);
        changed = true;
      }

      while ( itr != mbb.size()) {
        if (mbb.getInstAt(itr).getOpcode() != MipsGenInstrNames.JALR) {
          ++i;
          continue;
        }

        DebugLoc dl = mbb.getInstAt(itr).getDebugLoc();
        buildMI(mbb, ++itr, dl, tii.get(MipsGenInstrNames.LW), MipsGenRegisterNames.GP)
            .addFrameIndex(fi).addImm(0);
        changed = true;
      }
    }

    return changed;
  }

  @Override
  public String getPassName() {
    return "Mips Emit GP Restores";
  }
}
