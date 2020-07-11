/*
 *  Extremely C language Compiler
 *    Copyright (c) 2015-2019, Jianping Zeng.
 *
 *  Licensed under the BSD License version 3. Please refer LICENSE for details.
 */

package backend.target.x86;

import backend.mc.MCAsmInfo;
import backend.mc.MCInst;
import backend.mc.MCInstPrinter;
import backend.mc.MCOperand;
import tools.Util;

import java.io.PrintStream;

/**
 * This class is used for printing an X86 MCInst into AT&T style .s file syntax.
 */
public abstract class X86ATTInstPrinter extends MCInstPrinter {

  public X86ATTInstPrinter(PrintStream os, MCAsmInfo mai) {
    super(os, mai);
  }

  public static X86ATTInstPrinter createX86ATTInstPrinter(PrintStream os,
                                                          MCAsmInfo mai) {
    return new X86GenATTInstPrinter(os, mai);
  }

  protected abstract void printInst(MCInst inst);

  @Override
  public void printInstruction(MCInst inst) {
    printInst(inst);
  }

  public void printOperand(MCInst mi, int opNo) {
    MCOperand op = mi.getOperand(opNo);
    if (op.isReg())
      os.printf("%%%s", getRegisterName(op.getReg()));
    else if (op.isImm()) {
      os.printf("$%d", op.getImm());
      if (commentStream != null && (op.getImm() > 255 || op.getImm() < -256))
        commentStream.printf("imm = 0x%%%x\n", op.getImm());
    } else {
      Util.assertion(op.isExpr(), "unknown operand kind in printOperand");
      os.print("$");
      op.getExpr().print(os);
    }
  }

  public void printMemReference(MCInst mi, int op) {
    // if this has a segment register, print it.
    if (mi.getOperand(op + 4).getReg() != 0) {
      printOperand(mi, op + 4);
      os.print(':');
    }
    printLeaMemReference(mi, op);
  }

  public void printLeaMemReference(MCInst mi, int op) {
    MCOperand baseReg = mi.getOperand(op);
    MCOperand indexReg = mi.getOperand(op + 2);
    MCOperand dispSpec = mi.getOperand(op + 3);

    if (dispSpec.isImm()) {
      long dispVal = dispSpec.getImm();
      if (dispVal != 0 || (indexReg.getReg() == 0 && baseReg.getReg() == 0))
        os.print(dispVal);
    } else {
      Util.assertion(dispSpec.isExpr());
      dispSpec.getExpr().print(os);
    }

    if (indexReg.getReg() != 0 || baseReg.getReg() != 0) {
      os.print('(');
      if (baseReg.getReg() != 0)
        printOperand(mi, op);

      if (indexReg.getReg() != 0) {
        os.print(',');
        printOperand(mi, op + 2);
        long scaleVal = mi.getOperand(op + 1).getImm();
        if (scaleVal != 1)
          os.printf(",%d", scaleVal);
      }
      os.print(')');
    }
  }

  public void printSSECC(MCInst mi, int op) {
    long imm = mi.getOperand(op).getImm();
    if (imm == 0) os.print("eq");
    else if (imm == 1) os.print("lt");
    else if (imm == 2) os.print("le");
    else if (imm == 3) os.print("unord");
    else if (imm == 4) os.print("neq");
    else if (imm == 5) os.print("nlt");
    else if (imm == 6) os.print("nle");
    else if (imm == 7) os.print("ord");
    else
      Util.shouldNotReachHere("Invalid ssecc argument");

  }

  public void print_pcrel_imm(MCInst mi, int opNo) {
    MCOperand op = mi.getOperand(opNo);
    if (op.isImm())
      os.print(op.getImm());
    else {
      Util.assertion(op.isExpr(), "unknown pcrel immediate operand");
      op.getExpr().print(os);
    }
  }

  public void printopaquemem(MCInst mi, int opNo) {
    printMemReference(mi, opNo);
  }

  public void printi8mem(MCInst mi, int opNo) {
    printMemReference(mi, opNo);
  }

  public void printi16mem(MCInst mi, int opNo) {
    printMemReference(mi, opNo);
  }

  public void printi32mem(MCInst mi, int opNo) {
    printMemReference(mi, opNo);
  }

  public void printi64mem(MCInst mi, int opNo) {
    printMemReference(mi, opNo);
  }

  public void printi128mem(MCInst mi, int opNo) {
    printMemReference(mi, opNo);
  }

  public void printi256mem(MCInst mi, int opNo) {
    printMemReference(mi, opNo);
  }

  public void printf32mem(MCInst mi, int opNo) {
    printMemReference(mi, opNo);
  }

  public void printf64mem(MCInst mi, int opNo) {
    printMemReference(mi, opNo);
  }

  public void printf80mem(MCInst mi, int opNo) {
    printMemReference(mi, opNo);
  }

  public void printf128mem(MCInst mi, int opNo) {
    printMemReference(mi, opNo);
  }
  public void printf256mem(MCInst mi, int opNo) { printMemReference(mi, opNo); }

  public void printlea32mem(MCInst mi, int opNo) {
    printLeaMemReference(mi, opNo);
  }

  public void printlea64mem(MCInst mi, int opNo) {
    printLeaMemReference(mi, opNo);
  }

  public void printlea64_32mem(MCInst mi, int opNo) {
    printLeaMemReference(mi, opNo);
  }
}
