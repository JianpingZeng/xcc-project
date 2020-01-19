/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2020, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.mc;

import java.io.PrintStream;

/**
 * This is an instance of a target assembly language printer that converts an MCInst to valid target
 * assembly syntax.
 */
public abstract class MCInstPrinter {
  protected PrintStream os;
  protected PrintStream commentStream;
  protected MCAsmInfo mai;

  public MCInstPrinter(PrintStream os, MCAsmInfo mai) {
    this.os = os;
    commentStream = null;
    this.mai = mai;
  }

  public void setCommentStream(PrintStream cos) {
    commentStream = cos;
  }

  /**
   * Return the name of this opcode(e.g. "MOV32ri") or empty if we can't resovle it.
   * This method should be overrided by concrete subclass.
   * @param opcode
   * @return
   */
  public String getOpcodeName(int opcode) {
    return "";
  }

  public abstract void printInstruction(MCInst inst);

  public abstract String getRegisterName(int regNo);

  public abstract String getInstructionName(int opc);
}
