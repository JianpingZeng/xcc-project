package jlang.sema;
/*
 * Extremely C language CompilerInstance
 * Copyright (c) 2015-2018, Xlous
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

import jlang.ast.Tree;
import jlang.sema.Decl.FieldDecl;
import jlang.sema.Decl.RecordDecl;
import jlang.type.PointerType;
import jlang.type.QualType;
import jlang.type.RecordType;
import tools.APSInt;
import tools.OutRef;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class LValueExprEvaluator extends ExprEvaluatorBase<Boolean> {
  private OutRef<LValue> result;

  public LValueExprEvaluator(OutRef<LValue> result, ASTContext context) {
    super(context);
    this.result = result;
  }

  private boolean success(Tree.Expr expr) {
    result.get().base = expr;
    result.get().offset = 0;
    return true;
  }

  @Override
  protected Boolean success(APValue v, Tree.Expr e) {
    result.get().setFrom(v);
    return true;
  }

  @Override
  protected Boolean error(Tree.Expr expr) {
    return false;
  }

  @Override
  public Boolean visitCastExpr(Tree.CastExpr expr) {
    switch (expr.getCastKind()) {
      default:
        return false;

      case CK_NoOp:
        return visit(expr.getSubExpr());
    }
  }

  public Boolean visitDeclRefExpr(Tree.DeclRefExpr expr) {
    if (expr.getDecl() instanceof Decl.VarDecl) {
      return success(expr);
    }
    return false;
  }

  public Boolean visitCompoundLiteralExpr(Tree.CompoundLiteralExpr expr) {
    return success(expr);
  }

  public Boolean visitMemberExpr(Tree.MemberExpr expr) {
    QualType ty;
    if (expr.isArrow()) {
      if (!evaluatePointer(expr.getBase(), result, context))
        return false;
      ty = context.getAs(expr.getBase().getType(), PointerType.class).getPointeeType();
    } else {
      if (!visit(expr.getBase()))
        return false;
      ty = expr.getBase().getType();
    }

    RecordDecl recordDecl = context.getAs(ty, RecordType.class).getDecl();
    ASTRecordLayout recordLayout = ASTRecordLayout.getRecordLayout(context, recordDecl);

    if (!(expr.getMemberDecl() instanceof FieldDecl))
      return false;

    FieldDecl field = (FieldDecl) expr.getMemberDecl();

    int i = field.getFieldIndex();
    result.get().offset += context.toByteUnitFromBits(recordLayout.getFieldOffsetAt(i));
    return true;
  }

  public Boolean visitStringLiteral(Tree.StringLiteral expr) {
    return success(expr);
  }

  public Boolean visitArraySubscriptExpr(Tree.ArraySubscriptExpr expr) {
    if (!evaluatePointer(expr.getBase(), result, context))
      return false;

    APSInt index = new APSInt();
    OutRef<APSInt> x = new OutRef<>(index);
    if (!evaluateInteger(expr.getIdx(), x, context))
      return false;

    index = x.get();
    // FIXME, obtain the type size of given type in bytes.
    long elementSize = context.getTypeSize(expr.getType());
    result.get().offset += index.getSExtValue() * elementSize;
    return true;
  }

  public Boolean visitUnaryExpr(Tree.UnaryExpr expr) {
    if (expr.getOpCode() == UnaryOperatorKind.UO_Deref) {
      return evaluatePointer(expr, result, context);
    }
    return false;
  }

  @Override
  public Boolean visitImplicitCastExpr(Tree.ImplicitCastExpr expr) {
    return visitCastExpr(expr);
  }

  @Override
  public Boolean visitExplicitCastExpr(Tree.ExplicitCastExpr expr) {
    return visitCastExpr(expr);
  }
}
