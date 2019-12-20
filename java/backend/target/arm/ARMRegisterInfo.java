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

import static backend.target.arm.ARMGenRegisterNames.*;

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

  /**
   * Given the enum value for some register, e.g.
   * ARM::LR, return the number that it corresponds to (e.g. 14).
   * @param Reg
   * @return
   */
  static int getARMRegisterNumbering(int Reg) {
    switch (Reg) {
      default:
        Util.shouldNotReachHere("Unknown ARM register!");
      case R0:  case S0:  case D0:  case Q0:  return 0;
      case R1:  case S1:  case D1:  case Q1:  return 1;
      case R2:  case S2:  case D2:  case Q2:  return 2;
      case R3:  case S3:  case D3:  case Q3:  return 3;
      case R4:  case S4:  case D4:  case Q4:  return 4;
      case R5:  case S5:  case D5:  case Q5:  return 5;
      case R6:  case S6:  case D6:  case Q6:  return 6;
      case R7:  case S7:  case D7:  case Q7:  return 7;
      case R8:  case S8:  case D8:  case Q8:  return 8;
      case R9:  case S9:  case D9:  case Q9:  return 9;
      case R10: case S10: case D10: case Q10: return 10;
      case R11: case S11: case D11: case Q11: return 11;
      case R12: case S12: case D12: case Q12: return 12;
      case SP:  case S13: case D13: case Q13: return 13;
      case LR:  case S14: case D14: case Q14: return 14;
      case PC:  case S15: case D15: case Q15: return 15;

      case S16: case D16: return 16;
      case S17: case D17: return 17;
      case S18: case D18: return 18;
      case S19: case D19: return 19;
      case S20: case D20: return 20;
      case S21: case D21: return 21;
      case S22: case D22: return 22;
      case S23: case D23: return 23;
      case S24: case D24: return 24;
      case S25: case D25: return 25;
      case S26: case D26: return 26;
      case S27: case D27: return 27;
      case S28: case D28: return 28;
      case S29: case D29: return 29;
      case S30: case D30: return 30;
      case S31: case D31: return 31;
    }
  }

  /**
   * Returns true if the register is a low register (r0-r7).
   * @param Reg
   * @return
   */
  static boolean isARMLowRegister(int Reg) {
    switch (Reg) {
      case R0:  case R1:  case R2:  case R3:
      case R4:  case R5:  case R6:  case R7:
        return true;
      default:
        return false;
    }
  }
}
