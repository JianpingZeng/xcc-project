package cfe.codegen;
/*
 * Extremely C language CompilerInstance
 * Copyright (c) 2015-2020, Jianping Zeng.
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

import backend.ir.CGBuilder;
import backend.type.ArrayType;
import backend.value.BasicBlock;
import backend.value.Constant;
import backend.value.Value;
import cfe.ast.CastKind;
import cfe.ast.StmtVisitor;
import cfe.ast.Tree;
import cfe.ast.Tree.Expr;
import cfe.sema.Decl;
import cfe.type.QualType;
import cfe.type.RecordType;
import tools.Util;

/**
 * This class responsible for emitting HIR code for aggregate type expression.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class AggExprEmitter extends StmtVisitor<Void> {
  private CodeGenFunction cgf;
  private CGBuilder builder;
  private Value destPtr;
  private boolean volatileDest;
  private boolean ignoreResult;
  private boolean isInitializer;

  public AggExprEmitter(CodeGenFunction codeGenFunction,
                        Value destPtr,
                        boolean ignoreResult,
                        boolean isInitializer) {
    cgf = codeGenFunction;
    builder = cgf.builder;
    this.destPtr = destPtr;
    this.ignoreResult = ignoreResult;
    this.isInitializer = isInitializer;
  }

  //===--------------------------------------------------------------------===//
  //                               Utilities
  //===--------------------------------------------------------------------===//

  /**
   * Given an expression with aggregate type that
   * represents a value lvalue, this method emits the address of the lvalue,
   * then loads the result into DestPtr.
   *
   * @param expr
   */
  public void emitAggLoadOfLValue(Expr expr) {
    LValue lv = cgf.emitLValue(expr);
    emitFinalDestCopy(expr, lv);
  }

  @Override
  public Void visitStmt(Tree.Stmt s) {
    cgf.errorUnsupported(s, "");
    return null;
  }

  /**
   * emitFinalDestCopy - Perform the final copy to DestPtr, if desired.
   *
   * @param expr
   * @param src
   */
  public void emitFinalDestCopy(Expr expr, LValue src) {
    emitFinalDestCopy(expr, src, false);
  }

  public void emitFinalDestCopy(Expr expr, LValue src, boolean Ignore) {
    Util.assertion(src.isSimple(), "Can't have aggregate bitfield,etc");

    emitFinalDestCopy(expr, RValue.getAggregate
        (src.getAddress(), src.isVolatileQualified()));
    ;
  }

  public void emitFinalDestCopy(Expr expr, RValue src) {
    emitFinalDestCopy(expr, src, false);
  }

  public void emitFinalDestCopy(Expr expr, RValue src, boolean ignore) {
    Util.assertion(src.isAggregate(), "Value must be aggregate type.");

    // If the result is ignored, don't copy from the value.
    if (destPtr == null) {
      if (!src.isVolatileQualified() || (ignoreResult && ignore))
        return;

      // If the source is volatile, we must read from it; to do that, we
      // need some place to hold it.
      destPtr = cgf.createTempAlloca(cgf.convertType(expr.getType()), "agg.tmp");
    }

    // If the result of the assignment is used, copy the LHS there also.
    cgf.emitAggregateCopy(destPtr, src.getAggregateAddr(), expr.getType(),
        volatileDest | src.isVolatileQualified());
  }

  @Override
  public Void visitParenExpr(Tree.ParenExpr expr) {
    visit(expr.getSubExpr());
    return null;
  }

  // lvalue.

  @Override
  public Void visitDeclRefExpr(Tree.DeclRefExpr expr) {
    emitAggLoadOfLValue(expr);
    return null;
  }

  @Override
  public Void visitMemberExpr(Tree.MemberExpr expr) {
    emitAggLoadOfLValue(expr);
    return null;
  }

  @Override
  public Void visitUnaryDeref(Tree.UnaryExpr expr) {
    emitAggLoadOfLValue(expr);
    return null;
  }

  @Override
  public Void visitStringLiteral(Tree.StringLiteral expr) {
    emitAggLoadOfLValue(expr);
    return null;
  }

  @Override
  public Void visitCompoundLiteralExpr(Tree.CompoundLiteralExpr expr) {
    emitAggLoadOfLValue(expr);
    return null;
  }

  @Override
  public Void visitArraySubscriptExpr(Tree.ArraySubscriptExpr expr) {
    emitAggLoadOfLValue(expr);
    return null;
  }

  @Override
  public Void visitImplicitCastExpr(Tree.ImplicitCastExpr expr) {
    return visitCastExpr(expr);
  }

  @Override
  public Void visitExplicitCastExpr(Tree.ExplicitCastExpr expr) {
    return visitCastExpr(expr);
  }

  public Void visitCastExpr(Tree.CastExpr expr) {
    if (expr.getCastKind() == CastKind.CK_ToUnion) {
      // not supported.
      cgf.errorUnsupported(expr, "GCC union extension not supported!");
      return null;
    }

    CastKind kind = expr.getCastKind();
    Util.assertion(kind == CastKind.CK_NoOp, "Only no-op cast allowed.");
    return visit(expr.getSubExpr());
  }

  @Override
  public Void visitCallExpr(Tree.CallExpr expr) {
    RValue rv = cgf.emitCallExpr(expr);
    emitFinalDestCopy(expr, rv);
    return null;
  }

  @Override
  public Void visitBinaryExpr(Tree.BinaryExpr expr) {
    cgf.errorUnsupported(expr, "aggregate binary expression");
    return null;
  }

  @Override
  public Void visitBinAssign(Tree.BinaryExpr expr) {
    // So as to assignment to work correctly, the value on the right
    // side must has the type compatible with type of right side.

    Util.assertion(cgf.getContext().hasSameUnqualifiedType(expr.getLHS().getType(), expr.getRHS().getType()), "Invalid assignment");


    LValue lhsLV = cgf.emitLValue(expr.getLHS());

    // Codegen the RHS so that it stores directly into the LHS.
    cgf.emitAggExpr(expr.getRHS(), lhsLV.getAddress());
    emitFinalDestCopy(expr, lhsLV, true);
    return null;
  }

  @Override
  public Void visitBinComma(Tree.BinaryExpr expr) {
    cgf.emitAnyExpr(expr.getLHS());
    cgf.emitAnyExpr(expr.getRHS(), destPtr,
        volatileDest,
        /*ignore result*/false,
        isInitializer);
    return null;
  }

  @Override
  public Void visitConditionalExpr(Tree.ConditionalExpr expr) {
    BasicBlock lhsBlock = cgf.createBasicBlock("cond.true");
    BasicBlock rhsBlock = cgf.createBasicBlock("cond.false");
    BasicBlock endBlock = cgf.createBasicBlock("cond.end");

    Value cond = cgf.evaluateExprAsBool(expr.getCond());
    builder.createCondBr(cond, lhsBlock, rhsBlock);

    // emit the block that it will branch to, when condition is evaluated
    // as true.
    cgf.emitBlock(lhsBlock);
    visit(expr.getTrueExpr());
    cgf.emitBranch(endBlock);


    // emit the block that it will branch to, when condition is evaluated
    // as false.
    cgf.emitBlock(rhsBlock);
    visit(expr.getFalseExpr());
    cgf.emitBranch(endBlock);

    // enter the exit block.
    cgf.emitBlock(endBlock);
    return null;
  }

  @Override
  public Void visitInitListExpr(Tree.InitListExpr expr) {
    // Handle the initialization of an array.
    if (expr.getType().isArrayType()) {
      backend.type.PointerType ptType = (backend.type.PointerType) destPtr
          .getType();
      ArrayType atType = (ArrayType) ptType.getElementType();

      int numInitElements = expr.getNumInits();
      if (numInitElements > 0) {
        QualType t1 = expr.getType();
        QualType t2 = expr.getInitAt(0).getType();

        if (cgf.getContext().hasSameUnqualifiedType(t1, t2)) {
          emitAggLoadOfLValue(expr.getInitAt(0));
          return null;
        }
      }

      long numArrayElts = atType.getNumElements();
      QualType eltType = cgf.getContext().getPromotedIntegerType(expr.getType());
      eltType = cgf.getContext().getAsArrayType(eltType).getElementType();

      int cvrQualifiers = eltType.getCVRQualifiers();

      for (int i = 0; i < numArrayElts; i++) {
        Value nextValue = builder.createStructGEPInbounds(destPtr, i, ".array");
        if (i < numInitElements)
          emitInitializationToLValue(expr.getInitAt(i),
              LValue.makeAddr(nextValue, cvrQualifiers));
        else
          emitNullInitializationToLValue(LValue.makeAddr(nextValue, cvrQualifiers)
              , eltType);
      }
      return null;
    }

    Util.assertion(expr.getType().isRecordType(), "Only supported struct/unions here!");

    // Do struct initialization; this code just sets each individual member
    // to the approprate value.  This makes bitfield support automatic;
    // the disadvantage is that the generated code is more difficult for
    // the optimizer, especially with bitfields.
    int numInitElements = expr.getNumInits();
    Decl.RecordDecl rd = cgf.getContext().getAs(expr.getType(), RecordType.class).getDecl();
    int curInitVal = 0;

    if (expr.getType().isUnionType()) {
      // Only initialize one field of a union. The field itself is
      // specified by the initializer list.

    }

    // Here we iterate over the fields; this makes it simpler to both
    // default-initialize fields and skip over unnamed fields.
    for (int i = 0, e = rd.getDeclCounts(); i < e; i++) {
      Decl.FieldDecl field = rd.getDeclAt(i);

      // We're done once we hit the flexible array member
      if (field.getType().isIncompleteArrayType())
        break;

      if (field.isUnamaedBitField())
        continue;

      LValue fieldLoc = cgf.emitLValueForField(destPtr, field, false, 0);
      if (curInitVal < numInitElements) {
        // Store the initializer into the field.
        emitInitializationToLValue(expr.getInitAt(curInitVal++), fieldLoc);
      } else {
        // Store the default null value (0 for int) to field.
        emitNullInitializationToLValue(fieldLoc, field.getType());
      }
    }
    return null;
  }

  /**
   * Compute the value of given expr, and store it into specified address {@code lv}.
   *
   * @param init
   * @param lv
   */
  public void emitInitializationToLValue(Expr init, LValue lv) {
    if (init.getType().isComplexType()) {
      // // TODO: 2016/11/9
    } else if (cgf.hasAggregateLLVMType(init.getType()))
      cgf.emitAnyExpr(init, lv.getAddress(), false, false, false);
    else
      cgf.emitStoreThroughLValue(cgf.emitAnyExpr(init), lv, init.getType());
  }

  /**
   * emit a null value depending on different QualType and store it into given
   * address {@code lv}.
   *
   * @param lv
   * @param ty
   */
  public void emitNullInitializationToLValue(LValue lv, QualType ty) {
    if (!cgf.hasAggregateLLVMType(ty)) {
      // For non-aggregate type, directly store zero.
      Value zero = Constant.getNullValue(cgf.convertType(ty));
      cgf.emitStoreThroughLValue(RValue.get(zero), lv, ty);
    } else {
      // Otherwise, just memset the whole thing to zero.
      cgf.emitMemSetToZero(lv.getAddress(), ty);
    }
  }
}
