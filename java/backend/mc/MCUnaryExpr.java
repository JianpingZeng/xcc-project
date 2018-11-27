/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.mc;

import tools.OutRef;
import tools.Util;

import java.io.PrintStream;

import static backend.mc.MCUnaryExpr.Opcode.*;

public class MCUnaryExpr extends MCExpr {
  public enum Opcode {
    LNot,
    Minus,
    Not,
    Plus
  }

  private Opcode op;
  private MCExpr expr;
  private MCUnaryExpr(Opcode op, MCExpr e) {
    super(ExprKind.Unary);
    this.op = op;
    expr = e;
  }

  public Opcode getOpcode() {
    return op;
  }

  public MCExpr getSubExpr() {
    return expr;
  }

  @Override
  public void print(PrintStream os) {
    switch (getOpcode()) {
      case LNot: os.print('!'); break;
      case Not: os.print('~'); break;
      case Plus: os.print('+'); break;
      case Minus: os.print('-'); break;
      default:
        Util.assertion("Invalid unary opcode!");;
        break;
    }
    getSubExpr().print(os);
  }

  @Override
  public boolean evaluateAsRelocable(OutRef<MCValue> res) {
    OutRef<MCValue> ref = new OutRef<>(new MCValue());
    if (!getSubExpr().evaluateAsRelocable(res))
      return false;

    switch (getOpcode()) {
      case LNot:
        if (!ref.get().isAbsolute())
          return false;
        res.set(MCValue.get(ref.get().getConstant() == 0 ? 1: 0));;
        break;
      case Minus:
        /// -(a - b + const) ==> (b - a - const)
        if (ref.get().getSymA() != null &&
            ref.get().getSymB() == null)
          return false;

        res.set(MCValue.get(ref.get().getSymB(),
            ref.get().getSymA(), -ref.get().getConstant()));
        break;
      case Not:
        if (!ref.get().isAbsolute())
          return false;
        res.set(MCValue.get(~ref.get().getConstant()));
        break;
      case Plus:
        res.set(ref.get());
        break;
    }
    return true;
  }

  public static MCUnaryExpr Create(Opcode op,  MCExpr expr,
                                   MCSymbol.MCContext ctx) {
    return new MCUnaryExpr(op, expr);
  }
  public static MCUnaryExpr CreateLNot( MCExpr expr, MCSymbol.MCContext ctx) {
    return Create(LNot, expr, ctx);
  }
  public static MCUnaryExpr CreateMinus( MCExpr expr, MCSymbol.MCContext ctx) {
    return Create(Minus, expr, ctx);
  }
  public static MCUnaryExpr CreateNot( MCExpr expr, MCSymbol.MCContext ctx) {
    return Create(Not, expr, ctx);
  }
  public static MCUnaryExpr CreatePlus( MCExpr expr, MCSymbol.MCContext ctx) {
    return Create(Plus, expr, ctx);
  }
}
