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
import backend.codegen.MachineConstantPool;
import backend.codegen.MachineFunction;
import backend.codegen.MachineInstrBuilder;
import backend.debug.DebugLoc;
import backend.target.TargetInstrInfo;
import backend.type.Type;
import backend.value.Constant;
import backend.value.ConstantInt;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
class Thumb2RegisterInfo extends ARMGenRegisterInfo {
  Thumb2RegisterInfo(ARMSubtarget subtarget, int mode) {
    super(subtarget, mode);
  }

  @Override
  public int emitLoadConstantPool(MachineBasicBlock mbb,
                                  int mbbi, DebugLoc dl,
                                  int destReg, int subIdx,
                                  int val,
                                  ARMCC.CondCodes pred,
                                  int miFlags) {
    MachineFunction mf = mbb.getParent();
    MachineConstantPool constantPool = mf.getConstantPool();
    Constant c = ConstantInt.get(Type.getInt32Ty(mf.getFunction().getContext()), val);
    int idx = constantPool.getConstantPoolIndex(c, 4);
    TargetInstrInfo tii = subtarget.getInstrInfo();

    addDefaultPred(MachineInstrBuilder.buildMI(mbb, mbbi, dl, tii.get(ARMGenInstrNames.t2LDRpci))
    .addReg(destReg, MachineInstrBuilder.getDefRegState(true), subIdx)
    .addConstantPoolIndex(idx, 0, 0)).setMIFlags(miFlags);
    return ++mbbi;
  }
}
