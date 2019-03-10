/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.mc;

import backend.target.Target;

/**
 * Generic interface to target specific assembler backends.
 */
public class MCAsmBackend {
  protected Target theTarget;
  protected MCAsmBackend(Target t) {
    theTarget = t;
  }

  public Target getTarget() {
    return theTarget;
  }
}
