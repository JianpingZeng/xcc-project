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

import backend.codegen.MachineFunction;
import backend.codegen.MachineInstr;
import backend.codegen.RegScavenger;
import backend.mc.MCRegisterClass;
import backend.target.TargetRegisterInfo;
import tools.BitMap;
import tools.Util;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public abstract class ARMRegisterInfo  extends TargetRegisterInfo {
  protected ARMRegisterInfo(ARMTargetMachine tm) {

  }

  public static TargetRegisterInfo createARMRegisterInfo(ARMTargetMachine tm, int mode) {
    return new ARMGenRegisterInfo(tm, mode);
  }

  @Override
  public int[] getCalleeSavedRegs(MachineFunction mf) {
    return new int[0];
  }

  @Override
  public MCRegisterClass[] getCalleeSavedRegClasses(MachineFunction mf) {
    return new MCRegisterClass[0];
  }

  @Override
  public BitMap getReservedRegs(MachineFunction mf) {
    return null;
  }

  @Override
  public int getSubReg(int regNo, int index) {
    return 0;
  }

  @Override
  public void eliminateFrameIndex(MachineFunction mf, MachineInstr mi, RegScavenger rs) {

  }

  @Override
  public int getFrameRegister(MachineFunction mf) {
    return 0;
  }

  @Override
  public boolean isMoveInstr(MachineInstr mi, int[] regs) {
    switch (mi.getOpcode()) {
      default: return false;
      case ARMGenInstrNames.MOVr:
        Util.assertion(mi.getNumOperands() >= 2 && mi.getOperand(0).isRegister() &&
                mi.getOperand(1).isRegister(),
            "invalid register-register move instruction");

        regs[0] = mi.getOperand(1).getReg();
        regs[1] = mi.getOperand(0).getReg();
        regs[2] = mi.getOperand(1).getSubReg();
        regs[3] = mi.getOperand(0).getSubReg();
        return true;
    }
  }
}
