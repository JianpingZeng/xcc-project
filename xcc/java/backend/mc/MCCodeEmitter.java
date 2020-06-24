/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2020, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.mc;

import java.io.PrintStream;
import java.util.ArrayList;

public interface MCCodeEmitter {
  void encodeInstruction(MCInst inst, PrintStream os, ArrayList<MCFixup> fixups);
}
