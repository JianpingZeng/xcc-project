package backend.target.mips;
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

import backend.mc.MCAsmInfo;
import backend.mc.MCInst;
import backend.mc.MCInstPrinter;
import backend.mc.MCOperand;
import tools.Util;

import java.io.PrintStream;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public abstract class MipsInstPrinter extends MCInstPrinter {
  // Mips Branch Codes
  enum FPBranchCode {
    BRANCH_F,
    BRANCH_T,
    BRANCH_FL,
    BRANCH_TL,
    BRANCH_INVALID
  };

  // Mips Condition Codes
  enum CondCode {
    // To be used with float branch True
    FCOND_F,
    FCOND_UN,
    FCOND_OEQ,
    FCOND_UEQ,
    FCOND_OLT,
    FCOND_ULT,
    FCOND_OLE,
    FCOND_ULE,
    FCOND_SF,
    FCOND_NGLE,
    FCOND_SEQ,
    FCOND_NGL,
    FCOND_LT,
    FCOND_NGE,
    FCOND_LE,
    FCOND_NGT,

    // To be used with float branch False
    // This conditions have the same mnemonic as the
    // above ones, but are used with a branch False;
    FCOND_T,
    FCOND_OR,
    FCOND_UNE,
    FCOND_ONE,
    FCOND_UGE,
    FCOND_OGE,
    FCOND_UGT,
    FCOND_OGT,
    FCOND_ST,
    FCOND_GLE,
    FCOND_SNE,
    FCOND_GL,
    FCOND_NLT,
    FCOND_GE,
    FCOND_NLE,
    FCOND_GT
  };

  public MipsInstPrinter(PrintStream os, MCAsmInfo mai) {
    super(os, mai);
  }

  static MCInstPrinter createMipsInstPrinter(PrintStream os, MCAsmInfo mai) {
    return new MipsGenInstPrinter(os, mai);
  }

  @Override
  public void printInstruction(MCInst inst) {
    printInst(inst);
  }

  protected abstract void printInst(MCInst inst);

  private static String mipsFCCToString(CondCode cc) {
    switch (cc) {
      case FCOND_F:
      case FCOND_T:   return "f";
      case FCOND_UN:
      case FCOND_OR:  return "un";
      case FCOND_OEQ:
      case FCOND_UNE: return "eq";
      case FCOND_UEQ:
      case FCOND_ONE: return "ueq";
      case FCOND_OLT:
      case FCOND_UGE: return "olt";
      case FCOND_ULT:
      case FCOND_OGE: return "ult";
      case FCOND_OLE:
      case FCOND_UGT: return "ole";
      case FCOND_ULE:
      case FCOND_OGT: return "ule";
      case FCOND_SF:
      case FCOND_ST:  return "sf";
      case FCOND_NGLE:
      case FCOND_GLE: return "ngle";
      case FCOND_SEQ:
      case FCOND_SNE: return "seq";
      case FCOND_NGL:
      case FCOND_GL:  return "ngl";
      case FCOND_LT:
      case FCOND_NLT: return "lt";
      case FCOND_NGE:
      case FCOND_GE:  return "nge";
      case FCOND_LE:
      case FCOND_NLE: return "le";
      case FCOND_NGT:
      case FCOND_GT:  return "ngt";
    }
    Util.shouldNotReachHere("Impossible condition code!");
    return null;
  }

  private void printRegName(int reg) {
    os.printf("$%s", getRegisterName(reg).toLowerCase());
  }

  protected void printOperand(MCInst inst, int opNo) {
    MCOperand op = inst.getOperand(opNo);
    if (op.isReg()) {
      printRegName(op.getReg());
    } else if (op.isImm()) {
      os.print(op.getImm());
    }
    else {
      Util.assertion(op.isExpr(), "unknown operand kind in printOperand");
      op.getExpr().print(os);
    }
  }

  protected void printUnsignedImm(MCInst inst, int opNo) {
    MCOperand op = inst.getOperand(opNo);
    if (op.isImm())
      os.print(Short.toString((short)op.getImm()));
    else
      printOperand(inst, opNo);
  }

  protected void printFCCOperand(MCInst inst, int opNo) {
    MCOperand mo = inst.getOperand(opNo);
    os.print(mipsFCCToString(CondCode.values()[(int) mo.getImm()]));
  }

  protected void printMemOperandEA(MCInst inst, int opNo) {
    // when using stack locations for not load/store instructions
    // print the same way as all normal 3 operand instructions.
    printOperand(inst, opNo+1);
    os.print(", ");
    printOperand(inst, opNo);
  }

  protected void printMemOperand(MCInst inst, int opNo) {
    // Load/Store memory operands -- imm($reg)
    // If PIC target the target is loaded as the
    // pattern lw $25,%call16($28)
    printOperand(inst, opNo+1);
    os.print('(');
    printOperand(inst, opNo);
    os.print(')');
  }
}
