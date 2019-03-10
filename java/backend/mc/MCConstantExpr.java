/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.mc;

import tools.OutRef;

import java.io.PrintStream;

public class MCConstantExpr extends MCExpr {
  private long value;
  private MCConstantExpr(long val) {
    super(ExprKind.Constant);
    value = val;
  }

  public long getValue() {
    return value;
  }

  public static MCConstantExpr create(long val, MCSymbol.MCContext ctx) {
    return new MCConstantExpr(val);
  }
  @Override
  public void print(PrintStream os) {
    os.print(getValue());
  }

  @Override
  public boolean evaluateAsRelocable(OutRef<MCValue> res) {
    res.set(MCValue.get(getValue()));
    return true;
  }
}
