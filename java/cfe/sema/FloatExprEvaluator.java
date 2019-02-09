package cfe.sema;
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

import cfe.ast.Tree;
import cfe.type.QualType;
import tools.APFloat;
import tools.APSInt;
import tools.OutRef;

import static tools.APFloat.RoundingMode.rmNearestTiesToEven;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class FloatExprEvaluator extends ExprEvaluatorBase<Boolean> {
  private OutRef<APFloat> result;

  public FloatExprEvaluator(OutRef<APFloat> result, ASTContext ctx) {
    super(ctx);
    this.result = result;
  }

  @Override
  protected Boolean success(APValue v, Tree.Expr e) {
    return false;
  }

  @Override
  protected Boolean error(Tree.Expr expr) {
    return false;
  }

  @Override
  public Boolean visitParenExpr(Tree.ParenExpr expr) {
    return visit(expr.getSubExpr());
  }

  @Override
  public Boolean visitCallExpr(Tree.CallExpr expr) {
    // TODO: 17-10-22 Builtin function
    return false;
  }

  @Override
  public Boolean visitUnaryExpr(Tree.UnaryExpr expr) {
    if (expr.getOpCode() == UnaryOperatorKind.UO_Deref)
      return false;

    if (!evaluateFloat(expr.getSubExpr(), result, context))
      return false;

    switch (expr.getOpCode()) {
      default:
        return false;
      case UO_Plus:
        return true;
      case UO_Minus:
        return true;
    }
  }

  @Override
  public Boolean visitBinaryExpr(Tree.BinaryExpr expr) {
    OutRef<APFloat> rhs = new OutRef<>(new APFloat(0.0));
    if (!evaluateFloat(expr.getRHS(), result, context))
      return false;
    if (!evaluateFloat(expr.getLHS(), rhs, context))
      return false;

    switch (expr.getOpcode()) {
      default:
        return false;
      case BO_Mul:
        result.get().multiply(rhs.get(), rmNearestTiesToEven);
        return true;
      case BO_Add:
        result.get().add(rhs.get(), rmNearestTiesToEven);
        return true;
      case BO_Sub:
        result.get().subtract(rhs.get(), rmNearestTiesToEven);
        return true;
      case BO_Div:
        result.get().divide(rhs.get(), rmNearestTiesToEven);
        return true;
    }
  }

  @Override
  public Boolean visitFloatLiteral(Tree.FloatingLiteral literal) {
    result.set(literal.getValue());
    return true;
  }

  @Override
  public Boolean visitCastExpr(Tree.CastExpr expr) {
    Tree.Expr subExpr = expr.getSubExpr();
    if (subExpr.getType().isIntegerType()) {
      OutRef<APSInt> intResult = new OutRef<>(new APSInt());
      if (!evaluateInteger(subExpr, intResult, context))
        return false;

      result.set(handleIntToFloatCast(expr.getType(), subExpr.getType(),
          intResult.get(), context));
      return true;
    }
    if (subExpr.getType().isRealFloatingType()) {
      if (!visit(subExpr))
        return false;

      result.set(handleFloatToFloatCast(expr.getType(), subExpr.getType(),
          result.get(), context));
      return true;
    }
    return false;
  }

  static APFloat handleIntToFloatCast(QualType destTy, QualType srcTy,
                                      APSInt value, ASTContext ctx) {
    APFloat result = new APFloat(ctx.getFloatTypeSemantics(destTy), 1);
    result.convertFromAPInt(value, value.isSigned(), rmNearestTiesToEven);
    return result;
  }

  static APFloat handleFloatToFloatCast(
      QualType destTy,
      QualType srcTy,
      APFloat value,
      ASTContext ctx) {
    value.convert(ctx.getFloatTypeSemantics(destTy),
        rmNearestTiesToEven,
        new OutRef<>(false));
    return value;
  }
}
