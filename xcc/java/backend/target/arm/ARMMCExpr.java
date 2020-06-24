package backend.target.arm;
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

import backend.mc.MCExpr;
import backend.mc.MCTargetExpr;
import backend.mc.MCValue;
import tools.OutRef;
import tools.Util;

import java.io.PrintStream;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ARMMCExpr extends MCTargetExpr {
  public interface VariantKind {
    int VK_ARM_None = 0;
    int VK_ARM_HI16 = 1;  // The R_ARM_MOVT_ABS relocation (:upper16: in the .s file)
    int VK_ARM_LO16 = 2;   // The R_ARM_MOVW_ABS_NC relocation (:lower16: in the .s file)
  }

  private int kind;
  private MCExpr expr;

  private ARMMCExpr(int kind, MCExpr expr) {
    this.kind = kind;
    this.expr = expr;
  }

  public static ARMMCExpr create(int kind,
                                 MCExpr expr) {
    return new ARMMCExpr(kind, expr);
  }

  public static ARMMCExpr createLower16(MCExpr expr) {
    return create(VariantKind.VK_ARM_LO16, expr);
  }

  public static ARMMCExpr createUpper16(MCExpr expr) {
    return create(VariantKind.VK_ARM_HI16, expr);
  }


  public int getKind() { return kind; }

  public MCExpr getSubExpr() { return expr; }

  @Override
  public void print(PrintStream os) {
    switch (kind) {
      default: Util.assertion("Invalid kind");
      case VariantKind.VK_ARM_LO16: os.print(":lower16:"); break;
      case VariantKind.VK_ARM_HI16: os.print(":upper16:"); break;
    }
    MCExpr expr = getSubExpr();
    if (expr.getKind() != ExprKind.SymbolRef)
      os.print("(");
    expr.print(os);
    if (expr.getKind() != ExprKind.SymbolRef)
      os.print(")");
  }

  @Override
  public boolean evaluateAsRelocable(OutRef<MCValue> res) {
    return false;
  }
}
