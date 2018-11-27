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

public abstract class MCExpr {
  public enum ExprKind {
    Binary,
    Constant,
    SymbolRef,
    Unary,
    Target
  }

  private ExprKind kind;
  protected MCExpr(ExprKind k) { kind = k; }

  public ExprKind getKind() {
    return kind;
  }

  public abstract void print(PrintStream os);
  public void dump() { print(System.err); }
  public boolean evaluateAsAbsolute(OutRef<Long> res) {
    OutRef<MCValue> ref = new OutRef<>();
    if (!evaluateAsRelocable(ref) || ref.get().isAbsolute()) {
      res.set(0L);
      return false;
    }
    res.set(ref.get().getConstant());
    return true;
  }

  protected static boolean evaluateSymblicAdd(MCValue lhs,
                                              MCSymbol rhsA,
                                              MCSymbol rhsB,
                                              long rhsCst,
                                              OutRef<MCValue> res) {
    if ((lhs.getSymA() != null && rhsA != null) ||
        (lhs.getSymB() != null && rhsB != null))
      return false;

    MCSymbol a = lhs.getSymA() != null ? lhs.getSymA() : rhsA;
    MCSymbol b = lhs.getSymB() != null ? lhs.getSymB() : rhsB;

    if (b != null) {
      if (a == null) return false;
    }

    res.set(MCValue.get(a, b, lhs.getConstant()));;
    return true;
  }

  public abstract boolean evaluateAsRelocable(OutRef<MCValue> res);
}
