package backend.target.mips;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import backend.mc.MCSymbol;
import backend.mc.MCTargetExpr;
import backend.mc.MCValue;
import tools.OutRef;
import tools.Util;

import java.io.PrintStream;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class MipsMCSymbolRefExpr extends MCTargetExpr {
  enum VariantKind {
    VK_Mips_None,
    VK_Mips_GPREL,
    VK_Mips_GOT_CALL,
    VK_Mips_GOT,
    VK_Mips_ABS_HI,
    VK_Mips_ABS_LO,
    VK_Mips_TLSGD,
    VK_Mips_GOTTPREL,
    VK_Mips_TPREL_HI,
    VK_Mips_TPREL_LO,
    VK_Mips_GPOFF_HI,
    VK_Mips_GPOFF_LO,
    VK_Mips_GOT_DISP,
    VK_Mips_GOT_PAGE,
    VK_Mips_GOT_OFST
  }

  private VariantKind kind;
  private MCSymbol symbol;
  private int offset;

  private MipsMCSymbolRefExpr(VariantKind kind, MCSymbol sym, int offset) {
    this.kind = kind;
    this.symbol = sym;
    this.offset = offset;
  }

  public static MipsMCSymbolRefExpr create(VariantKind kind, MCSymbol symbol, int offset) {
    return new MipsMCSymbolRefExpr(kind, symbol, offset);
  }

  @Override
  public void print(PrintStream os) {
    switch (kind) {
      default:
        Util.assertion("invalid kind!");
        break;
      case VK_Mips_None: break;
      case VK_Mips_GPREL: os.print("%gp_ref("); break;
      case VK_Mips_GOT_CALL: os.print("%call16("); break;
      case VK_Mips_GOT: os.print("%got("); break;
      case VK_Mips_ABS_HI: os.print("%hi"); break;
      case VK_Mips_ABS_LO: os.print("%lo("); break;
      case VK_Mips_TLSGD: os.print("%tlsgd("); break;
      case VK_Mips_GOTTPREL: os.print("%gottpref("); break;
      case VK_Mips_TPREL_HI: os.print("%tpref_hi"); break;
      case VK_Mips_TPREL_LO: os.print("%tpref_lo("); break;
      case VK_Mips_GPOFF_HI: os.print("%hi(%neg(%gp_rel("); break;
      case VK_Mips_GPOFF_LO: os.print("%lo(%neg(%gp_rel("); break;
      case VK_Mips_GOT_DISP: os.print("%got_disp("); break;
      case VK_Mips_GOT_PAGE: os.print("%got_page("); break;
      case VK_Mips_GOT_OFST: os.print("%got_ofst("); break;
    }

    symbol.print(os);
    if (offset != 0) {
      if (offset > 0)
        os.print("+");
      os.print(offset);
    }

    if (kind == VariantKind.VK_Mips_GPOFF_HI || kind == VariantKind.VK_Mips_GPOFF_LO)
      os.print(")))");
    else if (kind != VariantKind.VK_Mips_None)
      os.print(")");
  }

  @Override
  public boolean evaluateAsRelocable(OutRef<MCValue> res) {
    return false;
  }
}
