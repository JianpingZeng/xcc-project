/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.target.x86;

import backend.mc.MCSymbol;
import backend.mc.MCTargetExpr;
import backend.mc.MCValue;
import tools.OutRef;

import java.io.PrintStream;

public class X86MCTargetExpr extends MCTargetExpr {
  public enum VariantKind {
    Invalid,
    GOT,
    GOTOFF,
    GOTPCREL,
    GOTTPOFF,
    INDNTPOFF,
    NTPOFF,
    PLT,
    TLSGD,
    TPOFF
  }

  private MCSymbol sym;
  private VariantKind kind;

  private X86MCTargetExpr(MCSymbol s, VariantKind k) {
    sym  = s;
    kind = k;
  }

  public static X86MCTargetExpr create(MCSymbol sym,
                                       VariantKind kind,
                                       MCSymbol.MCContext ctx) {
    return new X86MCTargetExpr(sym, kind);
  }
  @Override
  public void print(PrintStream os) {
    sym.print(os);

    switch (kind) {
      case Invalid:   os.print("@<invalid>"); break;
      case GOT:       os.print("@GOT"); break;
      case GOTOFF:    os.print("@GOTOFF"); break;
      case GOTPCREL:  os.print("@GOTPCREL"); break;
      case GOTTPOFF:  os.print("@GOTTPOFF"); break;
      case INDNTPOFF: os.print("@INDNTPOFF"); break;
      case NTPOFF:    os.print("@NTPOFF"); break;
      case PLT:       os.print("@PLT"); break;
      case TLSGD:     os.print("@TLSGD"); break;
      case TPOFF:     os.print("@TPOFF"); break;
    }
  }

  @Override
  public boolean evaluateAsRelocable(OutRef<MCValue> res) {
    // Evaluate recursively if this is a variable.
    if (sym.isVariable())
      return sym.getValue().evaluateAsRelocable(res);

    res.set(MCValue.get(sym, null, 0));
    return true;
  }
}
