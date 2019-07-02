/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2019, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.mc;

import java.io.PrintStream;

/**
 * This represents an "assembler immediate".  In its most general
 * form, this can hold "SymbolA - SymbolB + imm64".  Not all targets supports
 * relocations of this general form, but we need to represent this anyway.
 *
 * In the general form, SymbolB can only be defined if SymbolA is, and both
 * must be in the same (non-external) section. The latter constraint is not
 * enforced, since a symbol's section may not be known at construction.
 */
public class MCValue {
  private MCSymbol symA, symB;
  private long cst;

  public long getConstant() { return cst; }
  public MCSymbol getSymA() { return symA; }
  public MCSymbol getSymB() { return symB; }

  public boolean isAbsolute() { return symA == null && symB == null;}

  public MCSection getAssociatedSection() {
    return null;
  }

  public void print(PrintStream os, MCAsmInfo mai) {}
  public void dump() {
    print(System.err, null);
  }

  public static MCValue get(MCSymbol symA, MCSymbol symB,
                            long val) {
    MCValue res = new MCValue();
    res.cst = val;
    res.symA = symA;
    res.symB = symB;
    return res;
  }

  public static MCValue get(long val) {
    return get(null, null, val);
  }
}
