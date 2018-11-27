/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */
package backend.mc;

import backend.codegen.AsmPrinter;
import tools.FormattedOutputStream;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class MCStreamer {
  public static MCStreamer createAsmStreamer(
      MCContext context,
      FormattedOutputStream os,
      MCAsmInfo tai,
      AsmPrinter printer) {
    return new MCStreamer();
  }
}
