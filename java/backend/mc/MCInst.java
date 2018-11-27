/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.mc;

import java.io.PrintStream;
import java.util.ArrayList;

public class MCInst {
  private int opcode;
  private ArrayList<MCOperand> operands;

  public MCInst() {
    opcode = 0;
    operands = new ArrayList<>();
  }
  public int getOpcode() {
    return opcode;
  }

  public ArrayList<MCOperand> getOperands() {
    return operands;
  }
  public void addOperand(MCOperand op) {
    operands.add(op);
  }

  public void print(PrintStream os, MCAsmInfo mai) {
    os.printf("<MCInst %d", getOpcode());
    operands.forEach(op->{
      os.print(' ');
      op.print(os, mai);
    });
    os.printf(">");
  }

  public void dump() {
    print(System.err, null);
  }
}
