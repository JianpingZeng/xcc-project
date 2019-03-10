/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2015-2018, Jianping Zeng.
 * All rights reserved.
 *
 * Please refer the LICENSE for detail.
 */

package backend.target.x86;

import backend.mc.MCCodeEmitter;
import backend.mc.MCFixup;
import backend.mc.MCInst;

import java.io.PrintStream;
import java.util.ArrayList;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class X86MCCodeEmitter implements MCCodeEmitter {
  
  @Override
  public void encodeInstruction(MCInst inst, PrintStream os, ArrayList<MCFixup> fixups) {

  }
}
