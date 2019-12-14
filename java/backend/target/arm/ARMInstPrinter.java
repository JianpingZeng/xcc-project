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

import backend.mc.*;
import tools.OutRef;
import tools.Util;

import java.io.PrintStream;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public abstract class ARMInstPrinter extends MCInstPrinter {
  public ARMInstPrinter(PrintStream os, MCAsmInfo mai) {
    super(os, mai);
  }

  static MCInstPrinter createARMInstPrinter(PrintStream os, MCAsmInfo mai) {
    return new ARMGenInstPrinter(os, mai);
  }

  protected void printOperand(MCInst mi, int opIdx) {
    MCOperand op = mi.getOperand(opIdx);
    if (op.isReg())
      os.printf("%s", getRegisterName(op.getReg()));
    else if (op.isImm()) {
      os.printf("#%d", op.getImm());
      if (commentStream != null && (op.getImm() > 255 || op.getImm() < -256))
        commentStream.printf("imm = 0x%%%x\n", op.getImm());
    } else {
      Util.assertion(op.isExpr(), "unknown operand kind in printOperand");
      MCConstantExpr branchTarget = op.getExpr() instanceof MCConstantExpr ?
          (MCConstantExpr) op.getExpr() : null;
      OutRef<Long> address = new OutRef<>(0L);
      if (branchTarget != null && branchTarget.evaluateAsAbsolute(address)) {
        os.printf("0x%x", address.get());
      }
      else {
        op.getExpr().print(os);
      }
    }
  }


  protected void printT2AddrModeImm8s4OffsetOperand(MCInst mi, int opNo) {

  }

  protected void printThumbSRImm(MCInst mi, int opNo) {

  }

  protected void printShiftImmOperand(MCInst mi, int opNo) {

  }

  protected void printPKHASRShiftImm(MCInst mi, int opNo) {

  }

  protected void printPKHLSLShiftImm(MCInst mi, int opNo) {

  }

  protected void printT2AddrModeImm8s4Operand(MCInst mi, int opNo) {

  }

  protected void printAddrMode3OffsetOperand(MCInst mi, int opNo) {

  }

  protected void printPostIdxRegOperand(MCInst mi, int opNo) {

  }

  protected void printPostIdxImm8Operand(MCInst mi, int opNo) {

  }

  protected void printAddrMode2OffsetOperand(MCInst mi, int opNo) {

  }

  protected void printPostIdxImm8s4Operand(MCInst mi, int opNo) {

  }

  protected void printCoprocOptionImm(MCInst mi, int opNo) {

  }

  protected void printT2AddrModeImm8OffsetOperand(MCInst mi, int opNo) {

  }

  protected void printAddrMode6OffsetOperand(MCInst mi, int opNo) {

  }

  protected void printVectorIndex(MCInst mi, int opNo) {

  }

  protected void printRotImmOperand(MCInst mi, int opNo) {

  }

  protected void printThumbAddrModeSPOperand(MCInst mi, int opNo) {

  }

  protected void printThumbAddrModeImm5S4Operand(MCInst mi, int opNo) {

  }

  protected void printThumbAddrModeImm5S2Operand(MCInst mi, int opNo) {

  }

  protected void printThumbAddrModeRROperand(MCInst mi, int opNo) {

  }

  protected void printThumbAddrModeImm5S1Operand(MCInst mi, int opNo) {

  }

  protected void printThumbS4ImmOperand(MCInst mi, int opNo) {

  }

  protected void printT2AddrModeImm0_1020s4Operand(MCInst mi, int opNo) {

  }

  protected void printT2LdrLabelOperand(MCInst mi, int opNo) {

  }

  protected void printT2SOOperand(MCInst mi, int opNo) {

  }

  protected void printNoHashImmediate(MCInst mi, int opNo) {

  }

  protected void printAddrMode6Operand(MCInst mi, int opNo) {

  }

  protected void printNEONModImmOperand(MCInst mi, int opNo) {

  }

  protected void printImmPlusOneOperand(MCInst mi, int opNo) {

  }

  protected void printAddrMode3Operand(MCInst mi, int opNo) {

  }

  protected void printAddrMode7Operand(MCInst mi, int opNo) {

  }

  protected void printAddrMode5Operand(MCInst mi, int opNo) {

  }

  protected void printFPImmOperand(MCInst mi, int opNo) {

  }

  protected void printSORegRegOperand(MCInst mi, int opNo) {

  }

  protected void printSORegImmOperand(MCInst mi, int opNo) {

  }

  protected void printBitfieldInvMaskImmOperand(MCInst mi, int opNo) {

  }

  protected void printRegisterList(MCInst mi, int opNo) {

  }

  protected void printAddrModeTBH(MCInst mi, int opNo) {

  }

  protected void printAddrModeTBB(MCInst mi, int opNo) {

  }

  protected void printT2AddrModeSoRegOperand(MCInst mi, int opNo) {


  }

  protected void printT2AddrModeImm8Operand(MCInst mi, int opNo) {

  }

  protected void printMSRMaskOperand(MCInst mi, int opNo) {

  }

  protected void printCImmediate(MCInst mi, int opNo) {

  }

  protected void printCPSIFlag(MCInst mi, int opNo) {

  }

  protected void printMandatoryPredicateOperand(MCInst mi, int opNo) {

  }

  protected void printThumbITMask(MCInst mi, int opNo) {

  }

  protected void printSetendOperand(MCInst mi, int opNo) {

  }

  protected void printAddrMode2Operand(MCInst mi, int opNo) {

  }

  protected void printAddrModeImm12Operand(MCInst mi, int opNo) {

  }

  protected void printMemBOption(MCInst mi, int opNo) {

  }

  protected void printCPSIMod(MCInst mi, int opNo) {

  }

  protected void printPImmediate(MCInst mi, int opNo) {

  }

  protected void printPredicateOperand(MCInst mi, int opNo) {

  }

  protected void printSBitModifierOperand(MCInst mi, int opNo) {

  }
}
