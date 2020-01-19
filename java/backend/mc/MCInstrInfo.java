/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 *
 * Please refer the LICENSE for detail.
 */

package backend.mc;

import tools.Util;

/**
 *
 * @author Jianping Zeng.
 * @version 0.4
 */
public class MCInstrInfo {
  protected MCInstrDesc[] desc;

  /**
   * Initialize MCInstrInfo, called by TableGen auto-generated routines.
   * @param descs
   */
  protected void initMCInstrInfo(MCInstrDesc[] descs) {
    desc = descs;
  }

  public int getNumOperands() { return desc.length; }

  public MCInstrDesc get(int opcode) {
    Util.assertion(opcode >= 0 && opcode < getNumOperands(), "Invalid opcode!");
    return desc[opcode];
  }

  public String getName(int opCode) {
    return get(opCode).name;
  }

  public int getNumOperands(int opCode) {
    return get(opCode).numOperands;
  }
}
