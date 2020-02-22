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

import backend.codegen.MachineFunction;
import backend.codegen.MachineInstr;
import backend.codegen.RegScavenger;
import backend.mc.MCRegisterClass;
import backend.target.TargetRegisterInfo;
import tools.BitMap;
import tools.Util;

import static backend.target.mips.MipsGenRegisterNames.*;
import static backend.target.mips.MipsGenRegisterInfo.*;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public abstract class MipsRegisterInfo extends TargetRegisterInfo {
  private MipsSubtarget subtarget;

  MipsRegisterInfo(MipsTargetMachine tm) {
    subtarget = tm.getSubtarget();
    this.ra = MipsGenRegisterNames.RA;
  }

  static TargetRegisterInfo createMipsRegisterInfo(MipsTargetMachine tm) {
    return new MipsGenRegisterInfo(tm, 0);
  }

  static int getRegisterNumbering(int reg) {
    switch (reg) {
      case MipsGenRegisterNames.ZERO: case MipsGenRegisterNames.ZERO_64: case MipsGenRegisterNames.F0: case MipsGenRegisterNames.D0_64:
      case MipsGenRegisterNames.D0:
        return 0;
      case MipsGenRegisterNames.AT: case MipsGenRegisterNames.AT_64: case MipsGenRegisterNames.F1: case MipsGenRegisterNames.D1_64:
        return 1;
      case MipsGenRegisterNames.V0: case MipsGenRegisterNames.V0_64: case MipsGenRegisterNames.F2: case MipsGenRegisterNames.D2_64:
      case MipsGenRegisterNames.D1:
        return 2;
      case MipsGenRegisterNames.V1: case MipsGenRegisterNames.V1_64: case MipsGenRegisterNames.F3: case MipsGenRegisterNames.D3_64:
        return 3;
      case MipsGenRegisterNames.A0: case MipsGenRegisterNames.A0_64: case MipsGenRegisterNames.F4: case MipsGenRegisterNames.D4_64:
      case MipsGenRegisterNames.D2:
        return 4;
      case MipsGenRegisterNames.A1: case MipsGenRegisterNames.A1_64: case MipsGenRegisterNames.F5: case MipsGenRegisterNames.D5_64:
        return 5;
      case MipsGenRegisterNames.A2: case MipsGenRegisterNames.A2_64: case MipsGenRegisterNames.F6: case MipsGenRegisterNames.D6_64:
      case MipsGenRegisterNames.D3:
        return 6;
      case MipsGenRegisterNames.A3: case MipsGenRegisterNames.A3_64: case MipsGenRegisterNames.F7: case MipsGenRegisterNames.D7_64:
        return 7;
      case MipsGenRegisterNames.T0: case MipsGenRegisterNames.T0_64: case MipsGenRegisterNames.F8: case MipsGenRegisterNames.D8_64:
      case MipsGenRegisterNames.D4:
        return 8;
      case MipsGenRegisterNames.T1: case MipsGenRegisterNames.T1_64: case MipsGenRegisterNames.F9: case MipsGenRegisterNames.D9_64:
        return 9;
      case MipsGenRegisterNames.T2: case MipsGenRegisterNames.T2_64: case MipsGenRegisterNames.F10: case MipsGenRegisterNames.D10_64:
      case MipsGenRegisterNames.D5:
        return 10;
      case MipsGenRegisterNames.T3: case MipsGenRegisterNames.T3_64: case MipsGenRegisterNames.F11: case MipsGenRegisterNames.D11_64:
        return 11;
      case MipsGenRegisterNames.T4: case MipsGenRegisterNames.T4_64: case MipsGenRegisterNames.F12: case MipsGenRegisterNames.D12_64:
      case MipsGenRegisterNames.D6:
        return 12;
      case MipsGenRegisterNames.T5: case MipsGenRegisterNames.T5_64: case MipsGenRegisterNames.F13: case MipsGenRegisterNames.D13_64:
        return 13;
      case MipsGenRegisterNames.T6: case MipsGenRegisterNames.T6_64: case MipsGenRegisterNames.F14: case MipsGenRegisterNames.D14_64:
      case MipsGenRegisterNames.D7:
        return 14;
      case MipsGenRegisterNames.T7: case MipsGenRegisterNames.T7_64: case MipsGenRegisterNames.F15: case MipsGenRegisterNames.D15_64:
        return 15;
      case MipsGenRegisterNames.S0: case MipsGenRegisterNames.S0_64: case MipsGenRegisterNames.F16: case MipsGenRegisterNames.D16_64:
      case MipsGenRegisterNames.D8:
        return 16;
      case MipsGenRegisterNames.S1: case MipsGenRegisterNames.S1_64: case MipsGenRegisterNames.F17: case MipsGenRegisterNames.D17_64:
        return 17;
      case MipsGenRegisterNames.S2: case MipsGenRegisterNames.S2_64: case MipsGenRegisterNames.F18: case MipsGenRegisterNames.D18_64:
      case MipsGenRegisterNames.D9:
        return 18;
      case MipsGenRegisterNames.S3: case MipsGenRegisterNames.S3_64: case MipsGenRegisterNames.F19: case MipsGenRegisterNames.D19_64:
        return 19;
      case MipsGenRegisterNames.S4: case MipsGenRegisterNames.S4_64: case MipsGenRegisterNames.F20: case MipsGenRegisterNames.D20_64:
      case MipsGenRegisterNames.D10:
        return 20;
      case MipsGenRegisterNames.S5: case MipsGenRegisterNames.S5_64: case MipsGenRegisterNames.F21: case MipsGenRegisterNames.D21_64:
        return 21;
      case MipsGenRegisterNames.S6: case MipsGenRegisterNames.S6_64: case MipsGenRegisterNames.F22: case MipsGenRegisterNames.D22_64:
      case MipsGenRegisterNames.D11:
        return 22;
      case MipsGenRegisterNames.S7: case MipsGenRegisterNames.S7_64: case MipsGenRegisterNames.F23: case MipsGenRegisterNames.D23_64:
        return 23;
      case MipsGenRegisterNames.T8: case MipsGenRegisterNames.T8_64: case MipsGenRegisterNames.F24: case MipsGenRegisterNames.D24_64:
      case MipsGenRegisterNames.D12:
        return 24;
      case MipsGenRegisterNames.T9: case MipsGenRegisterNames.T9_64: case MipsGenRegisterNames.F25: case MipsGenRegisterNames.D25_64:
        return 25;
      case MipsGenRegisterNames.K0: case MipsGenRegisterNames.K0_64: case MipsGenRegisterNames.F26: case MipsGenRegisterNames.D26_64:
      case MipsGenRegisterNames.D13:
        return 26;
      case MipsGenRegisterNames.K1: case MipsGenRegisterNames.K1_64: case MipsGenRegisterNames.F27: case MipsGenRegisterNames.D27_64:
        return 27;
      case MipsGenRegisterNames.GP: case MipsGenRegisterNames.GP_64: case MipsGenRegisterNames.F28: case MipsGenRegisterNames.D28_64:
      case MipsGenRegisterNames.D14:
        return 28;
      case MipsGenRegisterNames.SP: case MipsGenRegisterNames.SP_64: case MipsGenRegisterNames.F29: case MipsGenRegisterNames.D29_64:
        return 29;
      case MipsGenRegisterNames.FP: case MipsGenRegisterNames.FP_64: case MipsGenRegisterNames.F30: case MipsGenRegisterNames.D30_64:
      case MipsGenRegisterNames.D15:
        return 30;
      case MipsGenRegisterNames.RA: case MipsGenRegisterNames.RA_64: case MipsGenRegisterNames.F31: case MipsGenRegisterNames.D31_64:
        return 31;
      default: Util.shouldNotReachHere("Unknown register number!");
        return 0; // Not reached
    }
  }

  @Override
  public int[] getCalleeSavedRegs(MachineFunction mf) {
    int[] SingleFloatOnlyCalleeSavedRegs = {
        F31, F30, F29, F28, F27, F26,
        F25, F24, F23, F22, F21, F20,
        RA, FP, S7, S6, S5, S4,
        S3, S2, S1, S0
    };

    int[] Mips32CalleeSavedRegs = {
        D15, D14, D13, D12, D11, D10,
        RA, FP, S7, S6, S5, S4,
        S3, S2, S1, S0
    };

    int[] N32CalleeSavedRegs = {
        D31_64, D29_64, D27_64, D25_64, D23_64,
        D21_64,
        RA_64, FP_64, GP_64, S7_64, S6_64,
        S5_64, S4_64, S3_64, S2_64, S1_64,
        S0_64
    };

    int[] N64CalleeSavedRegs = {
        D31_64, D30_64, D29_64, D28_64, D27_64,
        D26_64, D25_64, D24_64,
        RA_64, FP_64, GP_64, S7_64, S6_64,
        S5_64, S4_64, S3_64, S2_64, S1_64,
        S0_64
    };

    if (subtarget.isSingleFloat())
      return SingleFloatOnlyCalleeSavedRegs;
    else if (!subtarget.hasMips64())
      return Mips32CalleeSavedRegs;
    else if (subtarget.isABI_N64())
      return N32CalleeSavedRegs;

    Util.assertion(subtarget.isABI_N64());
    return N64CalleeSavedRegs;
  }

  @Override
  public MCRegisterClass[] getCalleeSavedRegClasses(MachineFunction mf) {
    MCRegisterClass[] SingleFloatOnlyCalleeSavedRC = {
    CPURegsRegisterClass, CPURegsRegisterClass, CPURegsRegisterClass,
    CPURegsRegisterClass, CPURegsRegisterClass, CPURegsRegisterClass,
    CPURegsRegisterClass, CPURegsRegisterClass,
    FGR32RegisterClass, FGR32RegisterClass, FGR32RegisterClass,
    FGR32RegisterClass, FGR32RegisterClass, FGR32RegisterClass,
    FGR32RegisterClass, FGR32RegisterClass, FGR32RegisterClass,
    FGR32RegisterClass, FGR32RegisterClass
    };

    MCRegisterClass[] BitMode32CalleeSavedRC = {
    CPURegsRegisterClass, CPURegsRegisterClass, CPURegsRegisterClass,
    CPURegsRegisterClass, CPURegsRegisterClass, CPURegsRegisterClass,
    CPURegsRegisterClass, CPURegsRegisterClass,
    FGR32RegisterClass, FGR32RegisterClass, FGR32RegisterClass,
    FGR32RegisterClass, FGR32RegisterClass, FGR32RegisterClass,
    AFGR64RegisterClass, AFGR64RegisterClass, AFGR64RegisterClass,
    AFGR64RegisterClass, AFGR64RegisterClass, AFGR64RegisterClass
    };

    if (subtarget.isSingleFloat())
      return SingleFloatOnlyCalleeSavedRC;
    else
      return BitMode32CalleeSavedRC;
  }

  @Override
  public BitMap getReservedRegs(MachineFunction mf) {
    BitMap reserved = new BitMap(getNumRegs());
    reserved.set(ZERO);
    reserved.set(AT);
    reserved.set(K0);
    reserved.set(K1);
    reserved.set(GP);
    reserved.set(SP);
    reserved.set(FP);
    reserved.set(RA);

    // SRV4 requires that odd register can't be used.
    if (!subtarget.isSingleFloat())
      for (int fpreg = F0 + 1; fpreg < F30; fpreg += 2)
        reserved.set(fpreg);

    return reserved;
  }

  @Override
  public void eliminateFrameIndex(MachineFunction mf, int spAdj, MachineInstr mi, RegScavenger rs) {
    int offsetIdx = 0;
    while (offsetIdx < mi.getNumOperands() && mi.getOperand(offsetIdx).isFrameIndex()) {
      ++offsetIdx;
    }

    Util.assertion(offsetIdx != mi.getNumOperands(), "MachineInstr doesn't have frame index operand!");

    int frameIndex = mi.getOperand(offsetIdx).getIndex();
    int stackSize = mf.getFrameInfo().getStackSize();
    int stackOffset = mf.getFrameInfo().getObjectOffset(frameIndex);

    int offset = stackOffset < 0 ? (stackSize - (stackOffset+4)) : stackOffset;

    // Replace the frame index with the stack pointer.
    mi.getOperand(offsetIdx-1).changeToImmediate(offset);
    mi.getOperand(offsetIdx).changeToRegister(getFrameRegister(mf), false);
  }

  @Override
  public int getFrameRegister(MachineFunction mf) {
    return mf.getSubtarget().getFrameLowering().hasFP(mf) ? FP : SP;
  }

  public int getPICCallReg() {
    return MipsGenRegisterNames.T9;
  }
}
