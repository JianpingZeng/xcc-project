/*
 * Extremely Compiler Collection
 *   Copyright (c) 2015-2020, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.mc;

import tools.OutRef;
import tools.Util;

import java.io.PrintStream;

import static backend.mc.MCBinaryExpr.Opcode.*;

public class MCBinaryExpr extends MCExpr {
  public   enum Opcode {
    Add,  ///< Addition.
    And,  ///< Bitwise and.
    Div,  ///< Signed division.
    EQ,   ///< Equality comparison.
    GT,   ///< Signed greater than comparison (result is either 0 or some
    ///< target-specific non-zero value)
    GTE,  ///< Signed greater than or equal comparison (result is either 0 or
    ///< some target-specific non-zero value).
    LAnd, ///< Logical and.
    LOr,  ///< Logical or.
    LT,   ///< Signed less than comparison (result is either 0 or
    ///< some target-specific non-zero value).
    LTE,  ///< Signed less than or equal comparison (result is either 0 or
    ///< some target-specific non-zero value).
    Mod,  ///< Signed remainder.
    Mul,  ///< Multiplication.
    NE,   ///< Inequality comparison.
    Or,   ///< Bitwise or.
    Shl,  ///< Shift left.
    Shr,  ///< Shift right (arithmetic or logical, depending on target)
    Sub,  ///< Subtraction.
    Xor   ///< Bitwise exclusive or.
  }

  private Opcode op;
  private MCExpr lhs, rhs;
  private MCBinaryExpr(Opcode op, MCExpr lhs, MCExpr rhs) {
    super(ExprKind.Binary);
    this.op = op;
    this.lhs = lhs;
    this.rhs = rhs;
  }

  public Opcode getOpcode() {
    return op;
  }

  public MCExpr getLHS() {
    return lhs;
  }

  public MCExpr getRHS() {
    return rhs;
  }

  @Override
  public void print(PrintStream os) {
    if (lhs instanceof MCConstantExpr ||
        lhs instanceof MCSymbolRefExpr) {
      lhs.print(os);
    }
    else {
      os.print('(');
      lhs.print(os);
      os.print(')');
    }

    switch (getOpcode()) {
      default:
        Util.assertion("Invalid opcode");
        break;
      case Add:
        // Print "X-42" instead of "X+-42".
        if (rhs instanceof MCConstantExpr) {
          MCConstantExpr rhsc = (MCConstantExpr)rhs;
          if (rhsc.getValue() < 0) {
            os.print(rhsc.getValue());
            return;
          }
        }
        os.print('+');
        break;
      case And: os.print('&'); break;
      case Div: os.print('/'); break;
      case EQ: os.print("=="); break;
      case GT: os.print('>'); break;
      case GTE: os.print(">="); break;
      case LAnd: os.print("&&"); break;
      case LOr: os.print("||"); break;
      case LT: os.print('<'); break;
      case LTE: os.print("<="); break;
      case Mod: os.print('%'); break;
      case Mul: os.print('*'); break;
      case NE: os.print("!="); break;
      case Or: os.print('|'); break;
      case Shl: os.print("<<"); break;
      case Shr: os.print(">>"); break;
      case Sub: os.print("-"); break;
      case Xor: os.print('^'); break;
    }

    if (rhs instanceof MCConstantExpr ||
        rhs instanceof MCSymbolRefExpr)
      rhs.print(os);
    else {
      os.print('(');
      rhs.print(os);
      os.print(')');
    }
  }

  @Override
  public boolean evaluateAsRelocable(OutRef<MCValue> res) {
    OutRef<MCValue> lhsValue = new OutRef<>(new MCValue());
    OutRef<MCValue> rhsValue = new OutRef<>(new MCValue());

    if (!getLHS().evaluateAsRelocable(lhsValue) ||
        !getRHS().evaluateAsRelocable(rhsValue))
      return false;

    if (!lhsValue.get().isAbsolute() || !rhsValue.get().isAbsolute()) {
      switch (getOpcode()) {
        default: return false;
        case Sub:
          // Negate RHS and add
          return evaluateSymblicAdd(lhsValue.get(), rhsValue.get().getSymB(),
              rhsValue.get().getSymA(),
              -rhsValue.get().getConstant(),
              res);
        case Add:
          return evaluateSymblicAdd(lhsValue.get(),
              rhsValue.get().getSymA(),
              rhsValue.get().getSymB(),
              rhsValue.get().getConstant(),
              res);
      }
    }

    long lhs = lhsValue.get().getConstant();
    long rhs = rhsValue.get().getConstant();
    long result = 0;
    switch (getOpcode()) {
      case Add:  result = lhs + rhs; break;
      case And:  result = lhs & rhs; break;
      case Div:  result = lhs / rhs; break;
      case EQ:   result = lhs == rhs ? 1:0; break;
      case GT:   result = lhs > rhs ? 1:0; break;
      case GTE:  result = lhs >= rhs ? 1:0; break;
      case LAnd: result = lhs != 0 && rhs != 0 ? 1:0; break;
      case LOr:  result = lhs != 0 || rhs != 0  ? 1:0; break;
      case LT:   result = lhs < rhs ? 1:0; break;
      case LTE:  result = lhs <= rhs ? 1:0; break;
      case Mod:  result = lhs % rhs; break;
      case Mul:  result = lhs * rhs; break;
      case NE:   result = lhs != rhs ? 1:0; break;
      case Or:   result = lhs | rhs; break;
      case Shl:  result = lhs << rhs; break;
      case Shr:  result = lhs >> rhs; break;
      case Sub:  result = lhs - rhs; break;
      case Xor:  result = lhs ^ rhs; break;
    }
    res.set(MCValue.get(result));
    return true;
  }

  public static MCBinaryExpr create(Opcode op,  MCExpr lhs,
                                     MCExpr rhs, MCSymbol.MCContext ctx) {
    return new MCBinaryExpr(op, lhs, rhs);
  }
  public static MCBinaryExpr createAdd( MCExpr lhs,  MCExpr rhs,
                                       MCSymbol.MCContext ctx) {
    return create(Add, lhs, rhs, ctx);
  }
  public static MCBinaryExpr createAnd( MCExpr lhs,  MCExpr rhs,
                                       MCSymbol.MCContext ctx) {
    return create(And, lhs, rhs, ctx);
  }
  public static MCBinaryExpr createDiv( MCExpr lhs,  MCExpr rhs,
                                       MCSymbol.MCContext ctx) {
    return create(Div, lhs, rhs, ctx);
  }
  public static MCBinaryExpr createEQ( MCExpr lhs,  MCExpr rhs,
                                      MCSymbol.MCContext ctx) {
    return create(EQ, lhs, rhs, ctx);
  }
  public static MCBinaryExpr createGT( MCExpr lhs,  MCExpr rhs,
                                      MCSymbol.MCContext ctx) {
    return create(GT, lhs, rhs, ctx);
  }
  public static MCBinaryExpr createGTE( MCExpr lhs,  MCExpr rhs,
                                       MCSymbol.MCContext ctx) {
    return create(GTE, lhs, rhs, ctx);
  }
  public static MCBinaryExpr createLAnd( MCExpr lhs,  MCExpr rhs,
                                        MCSymbol.MCContext ctx) {
    return create(LAnd, lhs, rhs, ctx);
  }
  public static MCBinaryExpr createLOr( MCExpr lhs,  MCExpr rhs,
                                       MCSymbol.MCContext ctx) {
    return create(LOr, lhs, rhs, ctx);
  }
  public static MCBinaryExpr createLT( MCExpr lhs,  MCExpr rhs,
                                      MCSymbol.MCContext ctx) {
    return create(LT, lhs, rhs, ctx);
  }
  public static MCBinaryExpr createLTE( MCExpr lhs,  MCExpr rhs,
                                       MCSymbol.MCContext ctx) {
    return create(LTE, lhs, rhs, ctx);
  }
  public static MCBinaryExpr createMod( MCExpr lhs,  MCExpr rhs,
                                       MCSymbol.MCContext ctx) {
    return create(Mod, lhs, rhs, ctx);
  }
  public static MCBinaryExpr createMul( MCExpr lhs,  MCExpr rhs,
                                       MCSymbol.MCContext ctx) {
    return create(Mul, lhs, rhs, ctx);
  }
  public static MCBinaryExpr createNE( MCExpr lhs,  MCExpr rhs,
                                      MCSymbol.MCContext ctx) {
    return create(NE, lhs, rhs, ctx);
  }
  public static MCBinaryExpr createOr( MCExpr lhs,  MCExpr rhs,
                                      MCSymbol.MCContext ctx) {
    return create(Or, lhs, rhs, ctx);
  }
  public static MCBinaryExpr createShl( MCExpr lhs,  MCExpr rhs,
                                       MCSymbol.MCContext ctx) {
    return create(Shl, lhs, rhs, ctx);
  }
  public static MCBinaryExpr createShr( MCExpr lhs,  MCExpr rhs,
                                       MCSymbol.MCContext ctx) {
    return create(Shr, lhs, rhs, ctx);
  }
  public static MCBinaryExpr createSub( MCExpr lhs,  MCExpr rhs,
                                       MCSymbol.MCContext ctx) {
    return create(Sub, lhs, rhs, ctx);
  }
  public static MCBinaryExpr createXor( MCExpr lhs,  MCExpr rhs,
                                       MCSymbol.MCContext ctx) {
    return create(Xor, lhs, rhs, ctx);
  }
}
