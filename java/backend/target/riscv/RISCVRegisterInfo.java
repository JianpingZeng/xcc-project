package backend.target.riscv;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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
import backend.codegen.RegScavenger;
import backend.target.RegClassInfo;
import backend.target.TargetRegisterClass;
import backend.target.TargetRegisterDesc;
import backend.target.TargetRegisterInfo;
import tools.BitMap;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class RISCVRegisterInfo extends TargetRegisterInfo {

  protected RISCVRegisterInfo(TargetRegisterDesc[] desc,
                              TargetRegisterClass[] regClasses,
                              int callFrameSetupOpCode,
                              int callFrameDestroyOpCode,
                              int[] subregs, int subregHashSize,
                              int[] superregs, int superregHashSize,
                              int[] aliases, int aliasHashSize,
                              RegClassInfo[] rcInfo,
                              int mode) {
    super(desc, regClasses, callFrameSetupOpCode,
        callFrameDestroyOpCode, subregs, subregHashSize,
        superregs, superregHashSize, aliases, aliasHashSize,
        rcInfo, mode);
  }

  @Override
  public int[] getCalleeSavedRegs(MachineFunction mf) {
    return new int[0];
  }

  @Override
  public TargetRegisterClass[] getCalleeSavedRegClasses(MachineFunction mf) {
    return new TargetRegisterClass[0];
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
  public boolean hasFP(MachineFunction mf) {
    return false;
  }

  @Override
  public void eliminateFrameIndex(MachineFunction mf, MachineInstr mi, RegScavenger rs) {

  }

  @Override
  public void emitPrologue(MachineFunction MF) {

  }

  @Override
  public void emitEpilogue(MachineFunction MF, MachineBasicBlock mbb) {

  }

  @Override
  public int getFrameRegister(MachineFunction mf) {
    return 0;
  }

  @Override
  public int getRARegister() {
    return 0;
  }
}
