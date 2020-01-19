/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2020, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.mc;

import tools.OutRef;

import java.io.PrintStream;

public abstract class MCTargetExpr extends MCExpr {
  protected MCTargetExpr() {
    super(ExprKind.Target);
  }
}
