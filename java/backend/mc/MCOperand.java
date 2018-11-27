/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.mc;

import tools.Util;

import java.io.PrintStream;

public class MCOperand {
  private enum MachineOperandType {
    kInvalid,
    kRegister,
    kImmediate,
    kExpr
  }

  MachineOperandType kind;
  long immVal;
  int regVal;
  MCExpr exprVal;

  private MCOperand() { kind = MachineOperandType.kInvalid; }

  public boolean isValid() { return kind != MachineOperandType.kInvalid; }
  public boolean isReg() { return kind == MachineOperandType.kRegister; }
  public boolean isImm() { return kind == MachineOperandType.kImmediate; }
  public boolean isExpr() { return kind == MachineOperandType.kExpr; }

  public int getReg() {
    Util.assertion(isReg(), "This is not a register operand!");
    return regVal;
  }

  public void setReg(int reg) {
    Util.assertion(isReg(), "This is not a register operand!");
    regVal = reg;
  }

  public long getImm() {
    Util.assertion(isImm(), "This is not a immediate operand!");
    return immVal;
  }

  public void setImm(long imm) {
    Util.assertion(isImm(), "This is not a immediate operand!");
    immVal = imm;
  }

  public MCExpr getExpr() {
    Util.assertion(isExpr(), "This is not a expression operand!");
    return exprVal;
  }

  public void setExpr(MCExpr expr) {
    Util.assertion(isExpr(), "This is not a expression operand!");
    exprVal = expr;
  }

  public static MCOperand createReg(int reg) {
    MCOperand op = new MCOperand();
    op.kind = MachineOperandType.kRegister;
    op.regVal = reg;
    return op;
  }

  public static MCOperand createImm(long val) {
    MCOperand op = new MCOperand();
    op.kind = MachineOperandType.kImmediate;
    op.immVal = val;
    return op;
  }

  public static MCOperand createExpr(MCExpr expr) {
    MCOperand op = new MCOperand();
    op.kind = MachineOperandType.kExpr;
    op.exprVal = expr;
    return op;
  }

  public void print(PrintStream os, MCAsmInfo mai) {
    os.print("<MCOperand ");
    if (!isValid())
      os.print("INVALID");
    else if (isReg())
      os.printf("Reg:%d", getReg());
    else if (isImm())
      os.printf("Imm:%d", getImm());
    else if (isExpr()) {
      os.print("Expr:(");
      getExpr().print(os, mai);
      os.print(")");
    }
    else
      os.print("UNDEFINED");
    os.print('>');
  }
  public void dump() {
    print(System.err, null);
  }
}
