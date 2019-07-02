/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2019, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.mc;

import tools.OutRef;

import java.io.PrintStream;

public class MCSymbolRefExpr extends MCExpr {
  private MCSymbol symbol;
  private MCSymbolRefExpr(MCSymbol sym) {
    super(ExprKind.SymbolRef);
    symbol = sym;
  }

  public MCSymbol getSymbol() {
    return symbol;
  }

  public static MCSymbolRefExpr create(MCSymbol symbol, MCSymbol.MCContext ctx) {
    return new MCSymbolRefExpr(symbol);
  }

  public static MCSymbolRefExpr create(String name, MCSymbol.MCContext ctx) {
    return create(ctx.getOrCreateSymbol(name), ctx);
  }

  @Override
  public void print(PrintStream os) {
    if (symbol.getName().charAt(0) == '$') {
      os.print('(');
      symbol.print(os);
      os.print(')');
    }
    else
      symbol.print(os);
  }

  @Override
  public boolean evaluateAsRelocable(OutRef<MCValue> res) {
    if (symbol.isVariable())
      return symbol.getValue().evaluateAsRelocable(res);
    res.set(MCValue.get(symbol, null, 0));
    return true;
  }
}
