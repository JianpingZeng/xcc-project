package jlang.sema;
/*
 * Extremely C language CompilerInstance
 * Copyright (c) 2015-2018, Jianping Zeng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import jlang.ast.Tree.*;
import jlang.sema.Decl.EnumConstantDecl;
import jlang.sema.Decl.ParamVarDecl;
import jlang.sema.Decl.VarDecl;
import jlang.support.SourceLocation;
import jlang.type.PointerType;
import jlang.type.QualType;
import tools.*;
import tools.APFloat.CmpResult;

/**
 * This class represents a implementation of evaluating whether the value of constant
 * expression is an integer. Otherwise, issue error messages if failed.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public final class IntExprEvaluator extends ExprEvaluatorBase<Boolean> {
  private OutRef<APValue> result;

  public IntExprEvaluator(OutRef<APValue> result, ASTContext ctx) {
    super(ctx);
    this.result = result;
  }

  protected boolean success(final APSInt si, final Expr e) {
    Util.assertion(e.getType().isIntegralOrEnumerationType(), "Invalid evaluation result.");

    Util.assertion(si.isSigned() == context.isSignedIntegerOrEnumerationType(e.getType()), "Invalid evaluation result.");

    Util.assertion(si.getBitWidth() == context.getIntWidth(e.getType()), "Invalid evaluation result.");

    result.set(new APValue(si));
    return true;
  }

  protected boolean success(final APInt i, final Expr e) {
    Util.assertion(e.getType().isIntegralOrEnumerationType(), "Invalid evaluation result.");

    Util.assertion(i.getBitWidth() == context.getIntWidth(e.getType()), "Invalid evaluation result.");

    result.set(new APValue(new APSInt(i)));
    result.get().getInt().setIsUnsigned(
        context.isUnsignedIntegerOrEnumerationType(e.getType()));
    return true;
  }

  private boolean success(long value, final Expr e) {
    Util.assertion(e.getType().isIntegralOrEnumerationType(), "Invalid evaluation result.");

    result.set(new APValue(context.makeIntValue(value, e.getType())));
    return true;
  }

  @Override
  protected Boolean success(APValue v, Expr e) {
    return false;
  }

  @Override
  protected Boolean error(Expr expr) {
    return error(expr.getExprLocation(), "invalid sub-expression in IntExprEvaluator",
        expr);
  }

  protected boolean error(SourceLocation loc, String diag, Expr e) {
    return false;
  }

  @Override
  public Boolean visitIntegerLiteral(IntegerLiteral e) {
    return success(e.getValue(), e);
  }

  @Override
  public Boolean visitCharacterLiteral(CharacterLiteral e) {
    return success(e.getValue(), e);
  }

  @Override
  public Boolean visitDeclRefExpr(DeclRefExpr e) {
    if (checkReferencedDecl(e, e.getDecl()))
      return true;
    return super.visitDeclRefExpr(e);
  }

  private boolean checkReferencedDecl(Expr e, Decl d) {
    // Enums are integer constant expres.
    if (d instanceof EnumConstantDecl) {
      EnumConstantDecl ecd = (EnumConstantDecl) d;
      boolean sameSign = ecd.getInitValue().isSigned()
          == context.isSignedIntegerOrEnumerationType(e.getType());
      boolean sameWidth = ecd.getInitValue().getBitWidth()
          == context.getIntWidth(e.getType());
      if (sameSign && sameWidth) {
        return success(ecd.getInitValue(), e);
      } else {
        // Get rid of mismatch (otherwise Success assertion will fail)
        // by computing a new value matching the jlang.type of E.
        APSInt val = ecd.getInitValue();

        if (!sameSign)
          val.setIssigned(!ecd.getInitValue().isSigned());
        if (!sameWidth)
          val = val.extOrTrunc(context.getIntWidth(e.getType()));
        return success(val, e);
      }
    }

    // In C, they can also be folded, although they are not ICEs.
    if (e.getType().getCVRQualifiers() == QualType.CONST_QUALIFIER) {
      if (d instanceof ParamVarDecl)
        return false;

      if (d instanceof VarDecl) {
        VarDecl vd = (VarDecl) d;
        Expr init = vd.getInit();
        if (init != null) {
          APValue v = vd.getEvaluatedValue();
          if (v != null) {
            if (v.isInt())
              return success(v.getInt(), e);
            return false;
          }

          if (vd.isEvaluatingValue())
            return false;

          vd.setEvaluatingValue();

          Expr.EvalResult evalResult = new Expr.EvalResult();
          if (init.evaluate(evalResult, context) && !evalResult.hasSideEffects()
              && evalResult.getValue().isInt()) {
            // Cache the evaluated value in the variable declaration.
            result.set(evalResult.getValue());
            vd.setEvaluatedValue(evalResult.getValue());
            return true;
          }
          vd.setEvaluatedValue(new APValue());
        }
      }
    }

    // Otherwise, random variable references are not constants.
    return false;
  }

  @Override
  public Boolean visitMemberExpr(MemberExpr e) {
    if (checkReferencedDecl(e, e.getMemberDecl())) {
      // Conservatively assume a MemberExpr will have side-effetcs.
      return true;
    }
    return super.visitMemberExpr(e);
  }

  @Override
  public Boolean visitCallExpr(CallExpr expr) {
    return error(expr);
  }

  @Override
  public Boolean visitBinaryExpr(BinaryExpr expr) {
    if (expr.getOpcode() == BinaryOperatorKind.BO_Comma) {
      if (!visit(expr.getRHS()))
        return false;

      // If we can't evaluate the LHS, it might have side effects.
      // mark it.
      return true;
    }

    if (expr.isLogicalOp()) {
      OutRef<Boolean> lhsResult = new OutRef<>(), rhsResult = new OutRef<>();
      if (handleConversionToBool(expr.getLHS(), lhsResult, context)) {
        // make constant folding operation.
        // evaluating the RHS: 0&& RHS -> 0, 1||RHS -> 1
        if (lhsResult.get() == (expr.getOpcode() == BinaryOperatorKind.BO_LOr)) {
          return success(lhsResult.get() ? 1 : 0, expr);
        }
        if (handleConversionToBool(expr.getRHS(), rhsResult, context)) {
          if (expr.getOpcode() == BinaryOperatorKind.BO_LOr)
            return success(lhsResult.get() || rhsResult.get() ? 1 : 0, expr);
          else
            return success(lhsResult.get() && rhsResult.get() ? 1 : 0, expr);
        }
      } else {
        if (handleConversionToBool(expr.getRHS(), lhsResult, context)) {
          if (rhsResult.get() == (expr.getOpcode() == BinaryOperatorKind.BO_LOr
              || !rhsResult.get() == (expr.getOpcode() == BinaryOperatorKind.BO_LAnd))) {
            return success(rhsResult.get() ? 1 : 0, expr);
          }
        }
      }

      return false;
    }

    QualType lhsTy = expr.getLHS().getType();
    QualType rhsTy = expr.getRHS().getType();

    if (lhsTy.isComplexType()) {
      Util.assertion(rhsTy.isComplexType(), "Invalid comparison");
      // TODO Clang 3.0 ExprConstant.clex:1398
    }

    if (lhsTy.isRealFloatingType() && rhsTy.isRealFloatingType()) {
      OutRef<APFloat> rhs = new OutRef<>(new APFloat(0.0));
      OutRef<APFloat> lhs = new OutRef<>(new APFloat(0.0));

      if (!evaluateFloat(expr.getRHS(), rhs, context))
        return false;

      if (!evaluateFloat(expr.getLHS(), lhs, context))
        return false;

      CmpResult compResult = lhs.get().compare(rhs.get());

      switch (expr.getOpcode()) {
        default:
          Util.shouldNotReachHere("Invalid binary operator!");
          break;
        case BO_LT:
          success(compResult == CmpResult.cmpLessThan ? 1 : 0, expr);
          break;
        case BO_GT:
          success(compResult == CmpResult.cmpGreaterThan ? 1 : 0, expr);
          break;
        case BO_LE: {
          boolean res = compResult == CmpResult.cmpLessThan
              || compResult == CmpResult.cmpEqual;
          success(res ? 1 : 0, expr);
          break;
        }
        case BO_GE: {
          boolean res = compResult == CmpResult.cmpGreaterThan
              || compResult == CmpResult.cmpEqual;
          success(res ? 1 : 0, expr);
          break;
        }
        case BO_EQ: {
          success(compResult == CmpResult.cmpEqual ? 1 : 0, expr);
          break;
        }
        case BO_NE: {
          success(compResult == CmpResult.cmpEqual ? 0 : 1, expr);
          break;
        }
      }
    }

    if (lhsTy.isPointerType() && rhsTy.isPointerType()) {
      if (expr.getOpcode() == BinaryOperatorKind.BO_Sub
          || expr.isEqualityOp()) {
        OutRef<LValue> lhsValue = new OutRef<>();
        if (!evaluatePointer(expr.getLHS(), lhsValue, context))
          return false;

        OutRef<LValue> rhsValue = new OutRef<>();
        if (!evaluatePointer(expr.getRHS(), rhsValue, context))
          return false;

        // Reject any bases from the normal codepath; we special-case comparisons
        // to null.
        if (lhsValue.get().getLValueBase() != null) {
          if (!expr.isEqualityOp())
            return false;

          if (rhsValue.get().getLValueBase() != null
              || rhsValue.get().getLValueOffset() != 0) {
            return false;
          }

          OutRef<Boolean> bres = new OutRef<>();
          if (evaluatePointerValueAsBool(lhsValue.get(), bres))
            return false;
          return success(bres.get() ? 1 :
              (expr.getOpcode() == BinaryOperatorKind.BO_EQ ? 1 : 0), expr);
        } else if (rhsValue.get().getLValueBase() != null) {
          if (!expr.isEqualityOp())
            return false;

          if (lhsValue.get().getLValueBase() != null
              || lhsValue.get().getLValueOffset() != 0) {
            return false;
          }

          OutRef<Boolean> bres = new OutRef<>();
          if (evaluatePointerValueAsBool(rhsValue.get(), bres))
            return false;
          return success(bres.get() ? 1 :
              (expr.getOpcode() == BinaryOperatorKind.BO_EQ ? 1 : 0), expr);
        }

        if (expr.getOpcode() == BinaryOperatorKind.BO_Sub) {
          QualType type = expr.getLHS().getType();
          QualType elemType = context.getAs(type, PointerType.class).getPointeeType();

          long elemSize = 1;
          if (!elemType.isVoidType() && !elemType.isFunctionType()) {
            elemSize = context.getTypeSize(elemType) >> 3;
          }

          long diff = lhsValue.get().getLValueOffset() - rhsValue.get().getLValueOffset();
          return success(diff / elemSize, expr);
        }

        boolean result;
        if (expr.getOpcode() == BinaryOperatorKind.BO_EQ) {
          result = lhsValue.get().getLValueOffset() == rhsValue.get().getLValueOffset();
        } else {
          result = lhsValue.get().getLValueOffset() != rhsValue.get().getLValueOffset();
        }
        return success(result ? 1 : 0, expr);
      }
    }

    if (!lhsTy.isIntegralOrEnumerationType()
        || !rhsTy.isIntegralOrEnumerationType()) {
      // We can't continue from here for non-integral types, and they
      // could potentially confuse the following operations.
      return false;
    }

    // The LHS of a constant expr is always evaluated and needed.
    if (!visit(expr.getLHS()))
      return false; // error in sub-expression.

    OutRef<APValue> rhsVal = new OutRef<>();
    if (!evaluateIntegerOrLValue(expr.getRHS(), rhsVal, context))
      return false;

    // Handle cases like (unsigned long)&a + 4.
    if (expr.isAdditiveOp() && result.get().isLValue()
        && rhsVal.get().isInt()) {
      long offset = result.get().getLValueOffset();
      long additionalOffset = rhsVal.get().getInt().getZExtValue();
      if (expr.getOpcode() == BinaryOperatorKind.BO_Add)
        offset += additionalOffset;
      else
        offset -= additionalOffset;

      result.set(new APValue(result.get().getLValueBase(), offset));
      ;
      return true;
    }


    // Handle cases like 4 + (unsigned long)&a
    if (expr.getOpcode() == BinaryOperatorKind.BO_Add
        && rhsVal.get().isLValue() && result.get().isInt()) {
      long offset = rhsVal.get().getLValueOffset();
      offset += result.get().getInt().getZExtValue();
      result.set(new APValue(rhsVal.get().getLValueBase(), offset));
      return true;
    }

    // All the following cases expect both operands to be an integer
    if (!result.get().isInt() || !rhsVal.get().isInt())
      return false;

    APSInt rhs = rhsVal.get().getInt();


    switch (expr.getOpcode()) {
      default:
        return error(expr.getOperatorLoc(),
            "invalid sub-expression in IntExprEvaluator",
            expr);
      case BO_Mul:
        return success(result.get().getInt().mul(rhs), expr);
      case BO_Add:
        return success(result.get().getInt().add(rhs), expr);
      case BO_Sub:
        return success(result.get().getInt().sub(rhs), expr);
      case BO_And:
        return success(result.get().getInt().and(rhs), expr);
      case BO_Xor:
        return success(result.get().getInt().xor(rhs), expr);
      case BO_Or:
        return success(result.get().getInt().or(rhs), expr);
      case BO_Div: {
        if (rhs.eq(0))
          return error(expr.getOperatorLoc(), "division by zero", expr);
        return success(result.get().getInt().div(rhs), expr);
      }
      case BO_Rem: {
        if (rhs.eq(0))
          return error(expr.getOperatorLoc(), "division by zero", expr);
        return success(result.get().getInt().rem(rhs), expr);
      }
      case BO_Shl: {
        if (rhs.isSigned() && rhs.isNegative()) {
          rhs = rhs.negative();
        }
        int sa = (int) rhs.getLimitedValue(result.get().getInt().getBitWidth() - 1);
        return success(result.get().getInt().shl(sa), expr);
      }
      case BO_Shr: {
        if (rhs.isSigned() && rhs.isNegative()) {
          rhs = rhs.negative();
        }
        int sa = (int) rhs.getLimitedValue(result.get().getInt().getBitWidth() - 1);
        return success(result.get().getInt().shr(sa), expr);
      }

      case BO_LT:
        return success(result.get().getInt().lt(rhs) ? 1 : 0, expr);
      case BO_GT:
        return success(result.get().getInt().gt(rhs) ? 1 : 0, expr);
      case BO_LE:
        return success(result.get().getInt().le(rhs) ? 1 : 0, expr);
      case BO_GE:
        return success(result.get().getInt().ge(rhs) ? 1 : 0, expr);
      case BO_EQ:
        return success(result.get().getInt().eq(rhs) ? 1 : 0, expr);
      case BO_NE:
        return success(result.get().getInt().ne(rhs) ? 1 : 0, expr);
    }
  }

  @Override
  public Boolean visitUnaryExpr(UnaryExpr expr) {
    if (expr.getOpCode() == UnaryOperatorKind.UO_LNot) {
      // LNot's operand isn't necessarily an integer, so
      // we handle it specially.
      OutRef<Boolean> bres = new OutRef<>();
      if (!handleConversionToBool(expr.getSubExpr(), bres, context))
        return false;

      return success(bres.get() ? 0 : 1, expr);
    }

    // Only handles integral operation.
    if (!expr.getSubExpr().getType().isIntegralOrEnumerationType())
      return false;

    // Let the operand value into 'Result'.
    if (!visit(expr.getSubExpr()))
      return false;

    switch (expr.getOpCode()) {
      default:
        // Address, indirect, pre/post inc/dec, etc are not valid constant exprs.
        // See C99 6.6p3.
        return error(expr.getExprLocation(), "invalid sub-expression in IntExprEvaluator", expr);
      case UO_Plus:
        return true;
      case UO_Minus:
        if (!result.get().isInt()) return false;
        return success(result.get().getInt().negative(), expr);
      case UO_Not:
        if (!result.get().isInt()) return false;
        return success(result.get().getInt().not(), expr);
    }
  }

  @Override
  public Boolean visitImplicitCastExpr(ImplicitCastExpr expr) {
    return visitCastExpr(expr);
  }

  @Override
  public Boolean visitExplicitCastExpr(ExplicitCastExpr expr) {
    return visitCastExpr(expr);
  }

  /**
   * Evaluate a sizeof a alignof with a result as the expression'type.
   *
   * @param expr
   * @return
   */
  @Override
  public Boolean visitSizeofAlignofExpr(SizeOfAlignOfExpr expr) {
    // Handle alignof separately.
    if (!expr.isSizeof()) {
      Util.assertion(false, "Alignof operator not supported currently");
      return false;
    }

    QualType srcTy = expr.getTypeOfArgument();

    // sizeof(void), __alignof__(void), sizeof(function) = 1 as a gcc
    // extension.
    if (srcTy.isVoidType() || srcTy.isFunctionType())
      return success(1, expr);

    // sizeof(vla) is not a constant expr. C99 6.5.3.4p2.
    if (!srcTy.isConstantSizeType())
      return false;

    long bitwidth = context.getTypeSize(srcTy);
    return success(bitwidth / context.target.getCharWidth(), expr);
  }

  /**
   * @param expr
   * @return
   */
  @Override
  public Boolean visitCastExpr(CastExpr expr) {
    Expr subExpr = expr.getSubExpr();
    QualType destType = expr.getType();
    QualType srcType = subExpr.getType();

    switch (expr.getCastKind()) {
      case CK_ArrayToPointerDecay:
      case CK_FunctionToPointerDecay:
      case CK_IntegralToPointer:
      case CK_ToVoid:
      case CK_IntegralToFloating:
      case CK_FloatingCast:
      case CK_FloatingRealToComplex:
      case CK_FloatingComplexCast:
      case CK_FloatingComplexToIntegralComplex:
      case CK_IntegralRealToComplex:
      case CK_IntegralComplexCast:
      case CK_IntegralComplexToFloatingComplex:
        Util.shouldNotReachHere("Invalid cast kind for integral value");

      case CK_BitCast:
        return false;
      case CK_LValueToRValue:
      case CK_NoOp:
        return visit(expr.getSubExpr());
      case CK_PointerToBoolean:
      case CK_IntegralToBoolean:
      case CK_FloatingToBoolean:
      case CK_FloatingComplexToBoolean:
      case CK_IntegralComplexToBoolean: {
        OutRef<Boolean> boolResult = new OutRef<>();
        if (!handleConversionToBool(subExpr, boolResult, context))
          return false;
        return success(boolResult.get() ? 1 : 0, expr);
      }

      case CK_IntegralCast: {
        if (!visit(subExpr))
          return false;

        if (!result.get().isInt()) {
          // Only allow casts of lvalues if they are lossless.
          return context.getTypeSize(destType) == context.getTypeSize(srcType);
        }

        return success(handleIntToIntCast(destType, srcType, result.get().getInt(), context), expr);
      }

      case CK_PointerToIntegral: {
        OutRef<LValue> lv = new OutRef<>();
        if (!evaluatePointer(subExpr, lv, context))
          return false;

        if (lv.get().getLValueBase() != null) {
          if (context.getTypeSize(destType) != context.getTypeSize(srcType))
            return false;

          result.set(lv.get().moveInto());
          return true;
        }

        APSInt asInt = context.makeIntValue(lv.get().getLValueOffset(), srcType);
        return success(handleIntToIntCast(destType, srcType, asInt, context), expr);
      }

      case CK_IntegralComplexToReal: {
        // TODO Clang3.0 ExprConstant.clex:1907
      }
      case CK_FloatingToIntegral: {
        OutRef<APFloat> f = new OutRef<>();
        if (!evaluateFloat(subExpr, f, context))
          return false;

        return success(handleFloatToIntCast(destType, srcType, f.get(), context), expr);
      }
    }
    Util.shouldNotReachHere("unknown cast result in integral value");
    return false;
  }

  private static APSInt handleIntToIntCast(QualType destType, QualType srcType, APSInt i, ASTContext ctx) {
    int destWidth = ctx.getIntWidth(destType);
    APSInt result = i;
    // Figure out if this is a truncate, extend or noop cast.
    // If the input is signed, do a sign extend, noop, or truncate.
    result = result.extOrTrunc(destWidth);
    result.setIsUnsigned(ctx.isUnsignedIntegerOrEnumerationType(destType));
    return result;
  }

  private static APSInt handleFloatToIntCast(QualType destType, QualType srcType, APFloat val, ASTContext ctx) {
    int destWidth = ctx.getIntWidth(destType);

    // Determine whether we are converting to unsigned or signed.
    boolean destIsSigned = ctx.isSignedIntegerOrEnumerationType(destType);

    APSInt result = new APSInt(val.bitcastToAPInt(), !destIsSigned);
    if (destIsSigned)
      result.sextOrTrunc(destWidth);
    else
      result.zextOrTrunc(destWidth);
    return result;
  }
}
