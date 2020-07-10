/*
 * Extremely Compiler Collection
 *   Copyright (c) 2015-2020, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.mc;

import tools.OutRef;

import java.io.PrintStream;

public class MCSymbolRefExpr extends MCExpr {
  public interface VariantKind {
    int VK_None = 0;
    int VK_Invalid = 1;

    int VK_GOT = 2;
    int VK_GOTOFF = 3;
    int VK_GOTPCREL = 4;
    int VK_GOTTPOFF = 5;
    int VK_INDNTPOFF = 6;
    int VK_NTPOFF = 7;
    int VK_GOTNTPOFF = 8;
    int VK_PLT = 9;
    int VK_TLSGD = 10;
    int VK_TLSLD = 11;
    int VK_TLSLDM = 12;
    int VK_TPOFF = 13;
    int VK_DTPOFF = 14;
    int VK_TLVP = 15;      // Mach-O thread local variable relocation
    // FIXME: We'd really like to use the generic Kinds listed above for these.
    int VK_ARM_PLT = 15;   // ARM-style PLT references. i.e., (PLT) instead of @PLT
    int VK_ARM_TLSGD = 16; //   ditto for TLSGD, GOT, GOTOFF, TPOFF and GOTTPOFF
    int VK_ARM_GOT = 17;
    int VK_ARM_GOTOFF = 18;
    int VK_ARM_TPOFF = 19;
    int VK_ARM_GOTTPOFF = 20;

    int VK_PPC_TOC = 21;
    int VK_PPC_DARWIN_HA16 = 22;  // ha16(symbol)
    int VK_PPC_DARWIN_LO16 = 23;  // lo16(symbol)
    int VK_PPC_GAS_HA16 = 24;     // symbol@ha
    int VK_PPC_GAS_LO16 = 25;     // symbol@l
  }

  private MCSymbol symbol;
  private int kind;
  private MCSymbolRefExpr(MCSymbol sym) {
    this(sym, VariantKind.VK_None);
  }

  private MCSymbolRefExpr(MCSymbol sym, int kind) {
    super(ExprKind.SymbolRef);
    symbol = sym;
    this.kind = kind;
  }

  public MCSymbol getSymbol() {
    return symbol;
  }

  public static MCSymbolRefExpr create(MCSymbol symbol) {
    return new MCSymbolRefExpr(symbol);
  }

  public static MCSymbolRefExpr create(MCSymbol symbol, int kind) {
    return new MCSymbolRefExpr(symbol, kind);
  }

  public static MCSymbolRefExpr create(String name, int kind, MCSymbol.MCContext ctx) {
    return create(ctx.getOrCreateSymbol(name), kind);
  }

  @Override
  public int getKind() { return kind;  }

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
