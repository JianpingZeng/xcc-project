package jlang.codegen;
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

import backend.ir.HIRBuilder;
import backend.support.LLVMContext;
import backend.type.FunctionType;
import backend.type.IntegerType;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.*;
import backend.value.Instruction.CmpInst;
import backend.value.Instruction.PhiNode;
import jlang.ast.CastKind;
import jlang.ast.StmtVisitor;
import jlang.ast.Tree;
import jlang.ast.Tree.*;
import jlang.sema.BinaryOperatorKind;
import jlang.sema.Decl;
import jlang.type.QualType;
import tools.APFloat;
import tools.Util;

import java.util.Iterator;

import static backend.value.Instruction.CmpInst.Predicate.*;

/**
 * This class emit code for jlang expression of typed scalar to HIR code.
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public class ScalarExprEmitter extends StmtVisitor<Value> {
  public static class BinOpInfo {
    Value lhs;
    Value rhs;
    /**
     * Computation type.
     */
    QualType ty;
    BinaryExpr expr;
    BinaryOperatorKind opcode;
  }

  private CodeGenFunction cgf;
  private HIRBuilder builder;
  private boolean ignoreResultAssign;

  public ScalarExprEmitter(CodeGenFunction cgf) {
    this(cgf, false);
  }

  public ScalarExprEmitter(CodeGenFunction cgf, boolean ignoreResultAssign) {
    this.cgf = cgf;
    builder = cgf.builder;
    this.ignoreResultAssign = ignoreResultAssign;
  }

  /**
   * Converts the specified expression value to a boolean (i1) truth value.
   *
   * @return
   */
  private Value emitConversionToBool(Value v, QualType srcTy) {
    Util.assertion(srcTy.isCanonical(), "EmitScalarConversion strips typedefs");

    if (srcTy.isFloatingType()) {
      // Compare against 0.0 for fp scalar.
      Value zero = Constant.getNullValue(v.getType());
      return builder.createFCmpUNE(v, zero, "tobool");
    }

    Util.assertion(srcTy.isIntegerType() || srcTy.isPointerType(), "Undefined scalar type to convert");


    // Because of the type rules of C, we often end up computing a logical value,
    // then zero extending it to int, then wanting it as a logical value again.
    // Optimize this common case.
    if (v instanceof Instruction.ZExtInst) {
      Instruction.ZExtInst zi = (Instruction.ZExtInst) v;
      if (zi.operand(0).getType() == LLVMContext.Int1Ty) {
        Value result = zi.operand(0);

        // If there aren't any more uses, zap the instruction to save space.
        // Note that there can be more uses, for example if this
        // is the result of an assignment.
        if (zi.isUseEmpty())
          zi.eraseFromParent();
        ;

        return result;
      }
    }

    // Compare against an integer or pointer null.
    Value zero = Constant.getNullValue(v.getType());
    return builder.createICmpNE(v, zero, "tobool");
  }

  /**
   * emit a conversion from the specified type to the specified destination
   * type, both of which are backend scalar types.
   *
   * @param v
   * @param srcTy
   * @param destTy
   * @return
   */
  public Value emitScalarConversion(Value v, QualType srcTy, QualType destTy) {
    srcTy = cgf.getContext().getCanonicalType(srcTy);
    destTy = cgf.getContext().getCanonicalType(destTy);

    if (srcTy.equals(destTy))
      return v;

    if (destTy.isVoidType()) return null;

    // Handle conversions to bool first, they are special: comparisons
    // against 0.
    if (destTy.isBooleanType())
      return emitConversionToBool(v, srcTy);

    backend.type.Type dstTy = convertType(destTy);

    // Ignore conversions like int -> uint.
    if (v.getType().equals(dstTy))
      return v;

    // Handle pointer conversions: pointer can only be converted
    // to/from other pointers and integer.
    if (dstTy instanceof PointerType) {
      // The source type may be pointer or integer.
      if (v.getType() instanceof PointerType) {
        return builder.createBitCast(v, dstTy, "conv");
      }
      Util.assertion(srcTy.isIntegerType(), "Not ptr->ptr or int->ptr");

      // First, convert to the correct width so that we control the kind of
      // extension.
      backend.type.Type middleTy = IntegerType.get(cgf.pointerWidth);
      boolean inputSigned = srcTy.isSignedIntegerType();
      Value intResult = builder.createIntCast(v, middleTy, inputSigned, "conv");

      // Next, cast to pointer.
      return builder.createIntToPtr(intResult, dstTy, "conv");
    }

    if (v.getType() instanceof PointerType) {
      // must be an ptr -> int cast.
      Util.assertion(dstTy instanceof IntegerType, "Not ptr->int.");
      return builder.createPtrToInt(v, dstTy, "conv");
    }

    // Finally, we have the arithmetic types: real int/float.
    if (v.getType() instanceof IntegerType) {
      boolean inputSigned = srcTy.isSignedIntegerType();
      if (dstTy instanceof IntegerType)
        return builder.createIntCast(v, dstTy, inputSigned, "conv");
      else if (inputSigned)
        return builder.createSIToFP(v, dstTy, "conv");
      else
        return builder.createUIToFP(v, dstTy, "conv");
    }

    Util.assertion(v.getType().isFloatingPointType(), "Undefined real conversion");
    if (dstTy instanceof IntegerType) {
      if (dstTy.isSigned())
        return builder.createFPToSI(v, dstTy, "conv");
      else
        return builder.createFPToUI(v, dstTy, "conv");
    }

    Util.assertion(dstTy.isFloatingPointType(), "Undefined real conversion");
    if (dstTy.getTypeID() < v.getType().getTypeID())
      return builder.createFPTrunc(v, dstTy, "conv");
    else
      return builder.createFPExt(v, dstTy, "conv");
  }

  private LValue emitLValue(Expr expr) {
    return cgf.emitLValue(expr);
  }

  private boolean testAndClearIgnoreResultAssign() {
    boolean i = ignoreResultAssign;
    ignoreResultAssign = false;
    return i;
  }

  /**
   * emit the code for loading the value of given expression.
   *
   * @param expr
   * @return
   */
  private Value emitLoadOfLValue(Expr expr) {
    return emitLoadOfLValue(emitLValue(expr), expr.getType());
  }

  /**
   * @param val
   * @param type
   * @return
   */
  private Value emitLoadOfLValue(LValue val, QualType type) {
    return cgf.emitLoadOfLValue(val, type).getScalarVal();
  }

  private backend.type.Type convertType(QualType type) {
    return cgf.convertType(type);
  }

  private BinOpInfo emitBinOp(BinaryExpr expr) {
    BinOpInfo infos = new BinOpInfo();
    infos.opcode = expr.getOpcode();
    infos.ty = expr.getType();
    infos.lhs = visit(expr.getLHS());
    infos.rhs = visit(expr.getRHS());
    infos.expr = expr;
    return infos;
  }

  private Value emitMul(BinOpInfo info) {
    // TODO overflow checking.
    if (info.lhs.getType().isFloatingPointType())
      return builder.createFMul(info.lhs, info.rhs, "mul");
    return builder.createMul(info.lhs, info.rhs, "mul");
  }

  private Value emitDiv(BinOpInfo info) {
    if (info.lhs.getType().isFloatingPointType())
      return builder.createFDiv(info.lhs, info.rhs, "div");
    else if (info.ty.isSignedIntegerType())
      return builder.createSDiv(info.lhs, info.rhs, "div");
    else
      return builder.createUDiv(info.lhs, info.rhs, "div");
  }

  private Value emitRem(BinOpInfo info) {
    // Rem in C can't be a floating point type: C99 6.5.5p2.
    if (info.ty.isSignedIntegerType())
      return builder.createSRem(info.lhs, info.rhs, "rem");
    else
      return builder.createURem(info.lhs, info.rhs, "rem");
  }

  private Value emitAdd(BinOpInfo info) {
    // handle the non-pointer type addiction operation.
    if (!info.ty.isPointerType()) {
      // TODO overflow checking.
      if (info.lhs.getType().isFloatingPointType())
        return builder.createFAdd(info.lhs, info.rhs, "add");

      // Signed integer overflow is undefined behavior.
      // if (info.ty.isSignedIntegerType())
      // TODO return builder.createNSWAdd(info.lhs, info.rhs, "add");
      return builder.createAdd(info.lhs, info.rhs, "add");
    }

    if (info.ty.isPointerType() &&
        info.ty.getAsPointerType().isVariableArrayType()) {
      // The amount of the addition needs to account for the VLA getNumOfSubLoop
      cgf.errorUnsupported(info.expr, "VLA pointer addition");
      ;
    }

    Value ptr, idx;
    Expr idxExpr;
    jlang.type.PointerType pt = info.expr.getLHS().getType().getAsPointerType();
    if (pt != null) {
      ptr = info.lhs;
      idx = info.rhs;
      idxExpr = info.expr.getRHS();
    } else {
      pt = info.expr.getRHS().getType().getAsPointerType();
      Util.assertion(pt != null, "Invalid add expr");
      ptr = info.rhs;
      idx = info.lhs;
      idxExpr = info.expr.getLHS();
    }

    int width = ((IntegerType) idx.getType()).getBitWidth();
    if (width < cgf.pointerWidth) {
      // Zero or sign extend the pointer value based on whether the index is
      // signed or not.
      Type idxType = IntegerType.get(cgf.pointerWidth);
      if (idxExpr.getType().isSignedIntegerType())
        idx = builder.createSExt(idx, idxType, "idx.sext");
      else
        idx = builder.createZExt(idx, idxType, "idx.zext");
    }

    QualType eltType = pt.getPointeeType();

    // Explicitly handle GNU void* and function pointer arithmetic
    // extensions. The GNU void* casts amount to no-ops since our void*
    // type is i8*, but this is future proof.
    if (eltType.isVoidType() || eltType.isFunctionType()) {
      Type i8Ty = PointerType.get(LLVMContext.Int8Ty, eltType.getAddressSpace());
      Value casted = builder.createBitCast(ptr, i8Ty, "bitcast");
      Value res = builder.createGEP(casted, idx, "add.ptr");
      return builder.createBitCast(res, ptr.getType(), "bitcast");
    }

    return builder.createInBoundsGEP(ptr, idx, "add.ptr");
  }

  private Value emitSub(BinOpInfo info) {
    // Handle integer or floating point type.
    if (!(info.lhs.getType() instanceof PointerType)) {
      //TODO checking overflow.
      if (info.lhs.getType().isFloatingPointType())
        return builder.createFSub(info.lhs, info.rhs, "sub");
      return builder.createSub(info.lhs, info.rhs, "sub");
    }

    QualType lhsType = info.expr.getLHS().getType();
    QualType lhsEltType = lhsType.getPointeeType();

    QualType rhsType = info.expr.getRHS().getType();
    // reaching here, the type of lhs is pointer type.
    // ptr - int or ptr - ptr.
    if (!(info.rhs.getType() instanceof PointerType)) {
      // Case#1: ptr - int.
      Value idx = info.rhs;
      int width = ((IntegerType) (idx.getType())).getBitWidth();
      if (width < cgf.pointerWidth) {
        // zero or signe extend the pointer value based whether the
        // index is signed or not.
        Type idxType = IntegerType.get(cgf.pointerWidth);
        if (rhsType.isSignedIntegerType())
          idx = builder.createSExt(idx, idxType, "idx.sext");
        else
          idx = builder.createZExt(idx, idxType, "idx.zext");
      }
      idx = builder.createNeg(idx, "sub.ptr.neg");

      // Explicitly handle GNU void* and function pointer arithmetic
      // extensions. The GNU void* casts amount to no-ops since our
      // void* type is i8*, but this is future proof.
      if (lhsEltType.isVoidType() || lhsEltType.isFunctionType()) {
        Type i8Ty = PointerType.get(LLVMContext.Int8Ty, lhsEltType.getAddressSpace());
        Value lhsCasted = builder.createBitCast(info.lhs, i8Ty, "sub.ptr.bitcast");
        Value res = builder.createGEP(lhsCasted, idx, "sub.ptr");
        return builder.createBitCast(res, info.lhs.getType(), "bitcast");
      }
      return builder.createInBoundsGEP(info.lhs, idx, "sub.ptr");
    } else {
      // Case#2: ptr - ptr.
      Value lhs = info.lhs;
      Value rhs = info.rhs;

      long eleSize;
      if (lhsEltType.isVoidType() || lhsEltType.isFunctionType()) {
        eleSize = 1;
      } else {
        eleSize = cgf.getContext().getTypeSize(lhsEltType) / 8;
      }

      Type resultType = convertType(info.ty);
      lhs = builder.createPtrToInt(lhs, resultType, "sub.ptr.lhs.cast");
      rhs = builder.createPtrToInt(rhs, resultType, "sub.ptr.rhs.cast");
      Value bytesBetween = builder.createSub(lhs, rhs, "sub.ptr.sub");

      // Optimize out the shift for element getNumOfSubLoop of 1.
      if (eleSize == 1)
        return bytesBetween;

      // Otherwise, do a fully sdiv.
      Value bytesPerElt = ConstantInt.get(resultType, eleSize);
      return builder.createSDiv(bytesPerElt, bytesPerElt, "sub.ptr.sdiv");
    }
  }

  private Value emitShl(BinOpInfo info) {
    // It is required that the lhs and rhs to be the same type.
    // Performs truncation or extending rhs to the same getNumOfSubLoop as the lhs.
    Value rhs = info.rhs;
    if (info.lhs.getType() != info.rhs.getType())
      rhs = builder.createIntCast(rhs, info.lhs.getType(), false, "shl.prom");
    return builder.createShl(info.lhs, rhs, "shl");
  }

  private Value emitShr(BinOpInfo info) {
    // It is required that the lhs and rhs to be the same type.
    // Performs truncation or extending rhs to the same getNumOfSubLoop as the lhs.
    Value rhs = info.rhs;
    if (info.lhs.getType() != info.rhs.getType())
      rhs = builder.createIntCast(rhs, info.lhs.getType(), false, "shr.prom");
    // Note that: it is needed to distinguish between arithmetic shift right
    // and logical shift right.
    if (info.ty.isSignedIntegerType())
      return builder.createAShr(info.lhs, rhs, "ashr");
    return builder.createLShr(info.lhs, rhs, "lshr");
  }

  private Value emitAnd(BinOpInfo info) {
    return builder.createAnd(info.lhs, info.rhs, "and");
  }

  private Value emitXor(BinOpInfo info) {
    return builder.createXor(info.lhs, info.rhs, "xor");
  }

  private Value emitOr(BinOpInfo info) {
    return builder.createOr(info.lhs, info.rhs, "or");
  }

  private Value emitCompoundAssign(CompoundAssignExpr expr,
                                   java.util.function.Function<BinOpInfo, Value> f) {
    boolean ignore = testAndClearIgnoreResultAssign();
    QualType lhsTy = expr.getLHS().getType(), rhsTy = expr.getRHS().getType();

    BinOpInfo info = new BinOpInfo();

    // emit the rhs first.
    info.rhs = visit(expr.getRHS());
    info.ty = expr.getComputationResultType();
    info.expr = expr;

    // Load/convert the LHS.
    LValue lhsLV = emitLValue(expr.getLHS());
    info.lhs = emitLoadOfLValue(lhsLV, lhsTy);
    info.lhs = emitScalarConversion(info.lhs, lhsTy, expr.getComputationResultType());


    // Apply the binary operator.
    Value result = f.apply(info);

    // Convert the result back to the lhs type.
    result = emitScalarConversion(result, expr.getComputationResultType(), lhsTy);

    // Store the result value into the LHS lvalue. Bit-fields are
    // handled specially because the result is altered by the store,
    // i.e., [C99 6.5.16p1] 'An assignment expression has the value of
    // the left operand after the assignment...'.
    if (lhsLV.isBitField()) {
      // TODO handle bitfield.
    } else {
      cgf.emitStoreThroughLValue(RValue.get(result), lhsLV, lhsTy);
    }
    if (ignore)
      return null;
    else
      return emitLoadOfLValue(lhsLV, expr.getType());
  }

  public Value visitBinMul(BinaryExpr expr) {
    return emitMul(emitBinOp(expr));
  }

  public Value visitBinDiv(BinaryExpr expr) {
    return emitDiv(emitBinOp(expr));
  }

  public Value visitBinRem(BinaryExpr expr) {
    return emitRem(emitBinOp(expr));
  }

  public Value visitBinAdd(BinaryExpr expr) {
    return emitAdd(emitBinOp(expr));
  }

  public Value visitBinSub(BinaryExpr expr) {
    return emitSub(emitBinOp(expr));
  }

  public Value visitBinShl(BinaryExpr expr) {
    return emitShl(emitBinOp(expr));
  }

  public Value visitBinShr(BinaryExpr expr) {
    return emitShr(emitBinOp(expr));
  }

  public Value visitBinAnd(BinaryExpr expr) {
    return emitAnd(emitBinOp(expr));
  }

  public Value visitBinXor(BinaryExpr expr) {
    return emitXor(emitBinOp(expr));
  }

  public Value visitBinOr(BinaryExpr expr) {
    return emitOr(emitBinOp(expr));
  }

  public Value visitBinLAnd(BinaryExpr expr) {
    // If we have 0 && X, see if we can elide RHS, if so, just return 0.
    // If we have 1 && X, see if we can just emit X without inspecting control
    // flow.
    int cond = cgf.constantFoldsToSimpleInteger(expr.getLHS());
    if (cond != 0) {
      if (cond == 1) {
        // Case#1: 1 && X.
        Value rhsCond = cgf.evaluateExprAsBool(expr.getRHS());
        // ZExt to int.
        return builder.createZExt(rhsCond, cgf.BACKEND_INTTy, "land.ext");
      }

      // 0 && X: if it is safe, just elide the rhs, and return 0.
      if (!cgf.containsLabel(expr.getRHS(), false)) {
        return Constant.getNullValue(cgf.BACKEND_INTTy);
      }
    }

    BasicBlock endBlock = cgf.createBasicBlock("land.end");
    BasicBlock rhsBlock = cgf.createBasicBlock("land.rhs");

    // emit the branch first. if it is false, branch to end block.
    cgf.emitBranchOnBoolExpr(expr.getLHS(), rhsBlock, endBlock);

    // Any edges into the ContBlock are now from an (indeterminate number of)
    // edges from this first condition.  All of these values will be false.  Star
    // setting up the PHI node in the endBlock for this.
    PhiNode phiNode = new PhiNode(LLVMContext.Int1Ty, 2, "phi", endBlock);
    for (Iterator<BasicBlock> predItr = endBlock.predIterator(); predItr.hasNext(); ) {
      phiNode.addIncoming(ConstantInt.getFalse(), predItr.next());
    }

    cgf.emitBlock(rhsBlock);
    Value rhsCond = cgf.evaluateExprAsBool(expr.getRHS());

    // Reaquire the RHS block, as there may be subblocks inserted.
    rhsBlock = builder.getInsertBlock();

    // Emits unconditional branch from this block to endBlock.
    // Insert an entry into phi node for the edge with the value of rhsCond.
    cgf.emitBlock(endBlock);
    phiNode.addIncoming(rhsCond, rhsBlock);

    // ZExt result to int.
    return builder.createZExt(phiNode, cgf.BACKEND_INTTy, "land.zext");
  }

  public Value visitBinLOr(BinaryExpr expr) {
    // If we have 1 || rhs, see if we can elide rhs, if so, just return 1.
    // if we have 0 || rhs, see if we can just emit rhs without inserting
    // the control flow.
    int lhsCond = cgf.constantFoldsToSimpleInteger(expr.getLHS());
    if (lhsCond != 0) {
      if (lhsCond == 1) {
        //  1 || rhs.
        if (!cgf.containsLabel(expr.getRHS(), false))
          return ConstantInt.get(cgf.BACKEND_INTTy, 1);
      } else {
        Value cond = cgf.evaluateExprAsBool(expr.getRHS());

        // ZExt to int.
        return builder.createZExt(cond, cgf.BACKEND_INTTy, "lor.zext");
      }
    }

    BasicBlock endBlock = cgf.createBasicBlock("lor.end");
    BasicBlock rhsBlock = cgf.createBasicBlock("lor.rhs");

    // Branch on the LHS first.  If it is true, go to the success
    // end block.
    cgf.emitBranchOnBoolExpr(expr.getLHS(), rhsBlock, endBlock);

    // Any edges into the endBlock are now from an (indeterminate number of)
    // edges from this first condition.  All of these values will be true.  Star
    // setting up the PHI node in the end Block for this.
    PhiNode phiNode = new PhiNode(LLVMContext.Int1Ty, 2, "phi", endBlock);
    for (Iterator<BasicBlock> predItr = endBlock.predIterator(); predItr.hasNext(); ) {
      phiNode.addIncoming(ConstantInt.getTrue(), predItr.next());
    }

    // emit the rhs condition as a bool value.
    cgf.emitBlock(rhsBlock);
    Value rhsCond = cgf.evaluateExprAsBool(expr.getRHS());

    // reaquire the insertion point due to subblocks would be inserted.
    rhsBlock = builder.getInsertBlock();

    cgf.emitBlock(endBlock);
    phiNode.addIncoming(rhsCond, rhsBlock);

    // ZExt result to int.
    return builder.createZExt(rhsCond, cgf.BACKEND_INTTy, "lor.zext");
  }

  public Value visitBinAssign(BinaryExpr expr) {
    boolean ignore = testAndClearIgnoreResultAssign();

    // First, emit code for rhs expression.
    Value rhs = visit(expr.getRHS());
    LValue lhsLV = emitLValue(expr.getLHS());

    // Store the value of rhs into the lhs. bit-field is not handled currently.
    if (lhsLV.isBitField()) {
      // TODO: 2016/11/7
    } else {
      cgf.emitStoreThroughLValue(RValue.get(rhs), lhsLV, expr.getType());
    }
    if (ignore)
      return null;
    else
      return emitLoadOfLValue(lhsLV, expr.getType());
  }

  public Value visitBinComma(BinaryExpr expr) {
    cgf.emitStmt(expr.getLHS());
    cgf.ensureInsertPoint();
    return visit(expr.getRHS());
  }

  private Value emitCompare(BinaryExpr expr,
                            CmpInst.Predicate uICmpOpc,
                            CmpInst.Predicate sICmpOpc,
                            CmpInst.Predicate fCmpOpc) {
    testAndClearIgnoreResultAssign();
    Value result = null;
    QualType lhsTy = expr.getLHS().getType();
    if (!lhsTy.isComplexType()) {
      Value rhs = visit(expr.getRHS());
      Value lhs = visit(expr.getLHS());

      // floating point comparison.
      if (lhs.getType().isFloatingPointType()) {
        result = builder.createFCmp(fCmpOpc, lhs, rhs, "fcmp");
      } else if (lhsTy.isSignedIntegerType()) {
        result = builder.createICmp(sICmpOpc, lhs, rhs, "scmp");
      } else {
        // unsigned integer and pointer.
        result = builder.createICmp(uICmpOpc, lhs, rhs, "ucmp");
      }
    }

    return emitScalarConversion(result,
        cgf.getContext().BoolTy,
        expr.getType());
  }

  public Value visitBinLT(BinaryExpr expr) {
    return emitCompare(expr, ICMP_ULT, ICMP_SLT, FCMP_OLT);
  }

  public Value visitBinGT(BinaryExpr expr) {
    return emitCompare(expr, ICMP_UGT, ICMP_SGT, FCMP_OGT);
  }

  public Value visitBinLE(BinaryExpr expr) {
    return emitCompare(expr, ICMP_ULE, ICMP_SLE, FCMP_OLE);
  }

  public Value visitBinGE(BinaryExpr expr) {
    return emitCompare(expr, ICMP_UGE, ICMP_SGE, FCMP_OGE);
  }

  public Value visitBinEQ(BinaryExpr expr) {
    return emitCompare(expr, ICMP_EQ, ICMP_EQ, FCMP_OEQ);
  }

  public Value visitBinNE(BinaryExpr expr) {
    return emitCompare(expr, ICMP_NE, ICMP_NE, FCMP_ONE);
  }

  public Value visitBinMulAssign(CompoundAssignExpr expr) {
    return emitCompoundAssign(expr, this::emitMul);
  }

  public Value visitBinDivAssign(CompoundAssignExpr expr) {
    return emitCompoundAssign(expr, this::emitDiv);
  }

  public Value visitBinRemAssign(CompoundAssignExpr expr) {
    return emitCompoundAssign(expr, this::emitRem);
  }

  public Value visitBinAddAssign(CompoundAssignExpr expr) {
    return emitCompoundAssign(expr, this::emitAdd);
  }

  public Value visitBinSubAssign(CompoundAssignExpr expr) {
    return emitCompoundAssign(expr, this::emitSub);
  }

  public Value visitBinShlAssign(CompoundAssignExpr expr) {
    return emitCompoundAssign(expr, this::emitShl);
  }

  public Value visitBinShrAssign(CompoundAssignExpr expr) {
    return emitCompoundAssign(expr, this::emitShr);
  }

  public Value visitBinAndAssign(CompoundAssignExpr expr) {
    return emitCompoundAssign(expr, this::emitAnd);
  }

  public Value visitBinOrAssign(CompoundAssignExpr expr) {
    return emitCompoundAssign(expr, this::emitOr);
  }

  public Value visitBinXorAssign(CompoundAssignExpr expr) {
    return emitCompoundAssign(expr, this::emitXor);
  }

  public Value visitUnaryPostInc(UnaryExpr expr) {
    return visitPrePostIncDec(expr, true, false);
  }

  public Value visitUnaryPostDec(UnaryExpr expr) {
    return visitPrePostIncDec(expr, false, false);
  }

  public Value visitUnaryPreInc(UnaryExpr expr) {
    return visitPrePostIncDec(expr, true, true);
  }

  public Value visitUnaryPreDec(UnaryExpr expr) {
    return visitPrePostIncDec(expr, false, true);
  }

  public Value visitUnaryAddrOf(UnaryExpr expr) {
    return emitLValue(expr.getSubExpr()).getAddress();
  }

  public Value visitUnaryDeref(UnaryExpr expr) {
    return emitLoadOfLValue(expr);
  }

  public Value visitUnaryPlus(UnaryExpr expr) {
    testAndClearIgnoreResultAssign();
    return visit(expr.getSubExpr());
  }

  public Value visitUnaryMinus(UnaryExpr expr) {
    testAndClearIgnoreResultAssign();
    Value op = visit(expr.getSubExpr());
    if (op.getType().isFloatingPointType())
      return builder.createFNeg(op, "neg");
    return builder.createNeg(op, "neg");
  }

  public Value visitUnaryNot(UnaryExpr expr) {
    testAndClearIgnoreResultAssign();
    Value op = visit(expr.getSubExpr());
    return builder.createNot(op, "not");
  }

  public Value visitUnaryLNot(UnaryExpr expr) {
    Value op = cgf.evaluateExprAsBool(expr.getSubExpr());

    // Invert value.
    op = builder.createNot(op, "lnot");
    // ZExt result to the expr type.
    return builder.createZExt(op, convertType(expr.getType()), "lnot.zext");
  }

  public Value visitUnaryReal(UnaryExpr expr) {
    // TODO.
    Util.assertion(false, "Currently, visitUnaryReal() is not supported!");
    return null;
  }

  public Value visitUnaryImag(UnaryExpr expr) {
    // TODO.
    Util.assertion(false, "Currently, visitUnaryImag() is not supported!");
    return null;
  }

  public Value visitStmt(Tree.Stmt s) {
    Util.assertion(false, "Stmt cann't have a complex type!");
    return null;
  }

  @Override
  public Value visitParenExpr(ParenExpr expr) {
    return visit(expr.getSubExpr());
  }

  @Override
  public Value visitIntegerLiteral(Tree.IntegerLiteral expr) {
    return backend.value.ConstantInt.get(expr.getValue());
  }

  @Override
  public Value visitFloatLiteral(Tree.FloatingLiteral expr) {
    return backend.value.ConstantFP.get(expr.getValue());
  }

  @Override
  public Value visitCharacterLiteral(Tree.CharacterLiteral expr) {
    return backend.value.ConstantInt.get(
        convertType(expr.getType()), expr.getValue());
  }

  @Override
  public Value visitDeclRefExpr(Tree.DeclRefExpr expr) {
    if (expr.getDecl() instanceof Decl.EnumConstantDecl) {
      Decl.EnumConstantDecl ec = (Decl.EnumConstantDecl) expr.getDecl();
      return ConstantInt.get(ec.getInitValue());
    }
    return emitLoadOfLValue(expr);
  }

  @Override
  public Value visitArraySubscriptExpr(Tree.ArraySubscriptExpr expr) {
    testAndClearIgnoreResultAssign();
    return emitLoadOfLValue(expr);
  }

  @Override
  public Value visitMemberExpr(Tree.MemberExpr expr) {
    return emitLoadOfLValue(expr);
  }

  @Override
  public Value visitCompoundLiteralExpr(Tree.CompoundLiteralExpr expr) {
    return emitLoadOfLValue(expr);
  }

  @Override
  public Value visitStringLiteral(Tree.StringLiteral expr) {
    return emitLValue(expr).getAddress();
  }

  @Override
  public Value visitInitListExpr(Tree.InitListExpr expr) {
    boolean ignore = testAndClearIgnoreResultAssign();

    int numInitElements = expr.getNumInits();
    // TODO init list expression.
    return visit(expr.getInitAt(0));
  }

  /**
   * Implicit casts are the same as normal casts, but
   * also handle things like function to pointer-to-function decay, and array to
   * pointer decay.
   *
   * @param expr
   * @return
   */
  @Override
  public Value visitImplicitCastExpr(Tree.ImplicitCastExpr expr) {
    Expr op = expr.getSubExpr();

    // if this is due to array-pointer conversion, emit the array
    // expression as lvalue.
    if (op.getType().isArrayType()) {
      Util.assertion(expr.getCastKind() == CastKind.CK_ArrayToPointerDecay);
      Value v = emitLValue(op).getAddress();

      // Note that VLA pointers are always decayed, so we don't need to do
      // anything here.
      if (!op.getType().isVariableArrayType()) {
        Util.assertion(v.getType() instanceof PointerType);
        Util.assertion(((PointerType) v.getType()).getElementType() instanceof backend.type.ArrayType);
        v = builder.createStructGEPInbounds(v, 0, "arraydecay");
      }

      Type destTy = convertType(expr.getType());
      if (v.getType() != destTy) {
        if (destTy instanceof PointerType)
          v = builder.createBitCast(v, destTy, "ptrconv");
        else {
          Util.assertion(destTy instanceof IntegerType);
          v = builder.createPtrToInt(v, destTy, "ptrconv");
        }
      }
      return v;
    }
    return emitCastExpr(op, expr.getType());
  }

  @Override
  public Value visitExplicitCastExpr(Tree.ExplicitCastExpr expr) {
    if (expr.getType().isVariablyModifiedType()) {
      Util.assertion(false, "VLA not supported yet!");
    }
    return emitCastExpr(expr.getSubExpr(), expr.getType());
  }

  /**
   * emit code for an explicit or implicit cast.  Implicit casts
   * have to handle a more broad range of conversions than explicit casts, as they
   * handle things like function to ptr-to-function decay etc.
   *
   * @param expr
   * @param destTy
   * @return
   */
  public Value emitCastExpr(Expr expr, QualType destTy) {
    if (!destTy.isVoidType())
      testAndClearIgnoreResultAssign();

    if (expr.isNullPointerConstant(cgf.getContext()) && destTy.isPointerType()) {
      // handle the common cases, convert a zero to pointer.
      return ConstantPointerNull.get(cgf.convertType(destTy));
    }
    if (!CodeGenFunction.hasAggregateLLVMType(expr.getType())) {
      Value src = visit(expr);
      // Use EmitScalarConversion to perform the conversion.
      return emitScalarConversion(src, expr.getType(), destTy);
    }

    Util.assertion(!expr.getType().isComplexType(), "Complex type not supported!");
    // TODO complex type.

    // Okay, this is a cast from an aggregate.  It must be a cast to void.  Just
    // evaluate the result and return.
    cgf.emitAggExpr(expr, null, false, true);
    return null;
  }

  @Override
  public Value visitCallExpr(Tree.CallExpr expr) {
    return cgf.emitCallExpr(expr).getScalarVal();
  }

  private Value visitPrePostIncDec(UnaryExpr expr, boolean isInc, boolean isPrec) {
    LValue lv = emitLValue(expr.getSubExpr());
    QualType valTy = expr.getSubExpr().getType();

    Value inVal = cgf.emitLoadOfLValue(lv, valTy).getScalarVal();

    int amountVal = isInc ? 1 : -1;
    if (valTy.isPointerType() && valTy.getAsPointerType().isVariableArrayType()) {
      cgf.errorUnsupported(expr, "VLA pointer in dec/inc");
    }

    Value nextVal;
    // handle pointer type ++, --.
    if (inVal.getType() instanceof PointerType) {
      PointerType pt = (PointerType) inVal.getType();
      Constant inc = ConstantInt.get(LLVMContext.Int32Ty, amountVal);
      if (!(pt.getElementType() instanceof FunctionType)) {
        QualType ptee = valTy.getPointeeType();
        nextVal = builder.createGEP(inVal, inc, "ptrincdec");
      } else {
        Type i8Ty = PointerType.get(LLVMContext.Int8Ty, ((PointerType) inVal.getType()).getAddressSpace());
        nextVal = builder.createBitCast(inVal, i8Ty, "tmp");
        nextVal = builder.createGEP(nextVal, inc, "ptrincdec");
        ;
        nextVal = builder.createBitCast(nextVal, inVal.getType(), "");
      }
    } else if (inVal.getType() == LLVMContext.Int1Ty && isInc) {
      // Bool++ is an interesting case, due to promotion rules, we get:
      // Bool++ -> Bool = Bool+1 -> Bool = (int)Bool+1 ->
      // Bool = ((int)Bool+1) != 0
      // An interesting aspect of this is that increment is always true.
      // Decrement does not have this property.
      nextVal = ConstantInt.getTrue();
    } else if (inVal.getType() instanceof IntegerType) {
      nextVal = backend.value.ConstantInt.get(inVal.getType(), amountVal);

      // Signed integer overflow is undefined behavior.
      //if (valTy.isSignedIntegerType())
      //nextVal = builder.createNSWAdd(inVal, nextVal, isInc?"inc":"dec");
      //else
      nextVal = builder.createAdd(inVal, nextVal, isInc ? "inc" : "dece");
    } else {
      // Add the inc/dec to the real part.
      Util.assertion((inVal.getType() == LLVMContext.FloatTy || inVal.getType() == LLVMContext.DoubleTy));

      nextVal = ConstantFP.get(new APFloat(amountVal));
      nextVal = builder.createFAdd(inVal, nextVal, isInc ? "inc" : "dec");
    }

    // Store the updated result through the lvalue.
    if (lv.isBitField()) {
      // TODO.
    } else {
      cgf.emitStoreThroughLValue(RValue.get(nextVal), lv, valTy);
    }

    // if this is a postfix increment or decrement, return the value read from
    // memory, otherwise use the updated value.
    return isPrec ? nextVal : inVal;
  }


  @Override
  public Value visitConditionalExpr(Tree.ConditionalExpr expr) {
    if (expr.getTrueExpr() != null &&
        isCheapEnoughToEvaluateUnconditionally(expr.getTrueExpr())
        && isCheapEnoughToEvaluateUnconditionally(expr.getFalseExpr())) {
      Value cond = cgf.evaluateExprAsBool(expr.getCond());
      Value lhs = visit(expr.getTrueExpr());
      Value rhs = visit(expr.getFalseExpr());
      return builder.createSelect(cond, lhs, rhs, "select.cond");
    }
    BasicBlock lhsBlock = cgf.createBasicBlock("cond.true");
    BasicBlock rhsBlock = cgf.createBasicBlock("cond.false");
    BasicBlock endBlock = cgf.createBasicBlock("cond.end");

    Value condVal = null;
    // If this is not a GNU extension.
    if (expr.getTrueExpr() != null) {
      cgf.emitBranchOnBoolExpr(expr.getCond(), lhsBlock, rhsBlock);
    } else {
      condVal = cgf.emitScalarExpr(expr.getCond());
      new Instruction.BitCastInst(condVal, condVal.getType(), "dummy?:holder",
          builder.getInsertBlock());
      Value val = emitScalarConversion(condVal, expr.getCond().getType(),
          cgf.getContext().BoolTy);
      builder.createCondBr(val, lhsBlock, rhsBlock);
    }

    Value lhs, rhs;

    // emit the block that it will branch to, when condition is evaluated
    // as true.
    cgf.emitBlock(lhsBlock);

    if (expr.getTrueExpr() != null)
      lhs = visit(expr.getTrueExpr());
    else
      lhs = emitScalarConversion(condVal, expr.getCond().getType(),
          cgf.getContext().BoolTy);
    cgf.emitBranch(endBlock);

    // emit the block that it will branch to, when condition is evaluated
    // as false.
    cgf.emitBlock(rhsBlock);
    rhs = visit(expr.getFalseExpr());
    cgf.emitBranch(endBlock);

    // enter the exit block.
    cgf.emitBlock(endBlock);

    if (lhs == null || rhs == null) {
      Util.assertion(expr.getType().isVoidType(), "Can not generate value for non-void type");
      return null;
    }
    PhiNode pn = builder.createPhiNode(lhs.getType(), 2, "cond");
    pn.addIncoming(lhs, lhsBlock);
    pn.addIncoming(rhs, rhsBlock);
    return pn;
  }

  /**
   * To Check if the specified expression can be evaluated unconditionally.
   * An expression could be evaluated unconditionally if the expression is cheap
   * enough to compute and without side effect, such as integral, floating point
   * constant etc.
   *
   * @param expr
   * @return
   */
  private static boolean isCheapEnoughToEvaluateUnconditionally(Expr expr) {
    Util.assertion(expr != null, "Can't evaluate a null expression!");
    if (expr instanceof ParenExpr)
      return isCheapEnoughToEvaluateUnconditionally(((ParenExpr) expr).getSubExpr());

    if (expr instanceof CastExpr) {
      CastExpr ce = (CastExpr) expr;
      switch (ce.getCastKind()) {
        case CK_LValueToRValue:
        case CK_NoOp:
        case CK_NullToPointer:
          return isCheapEnoughToEvaluateUnconditionally(ce.getSubExpr());
        default:
          break;
      }
    }
    if (expr instanceof Tree.IntegerLiteral || expr instanceof Tree.FloatingLiteral
        || expr instanceof Tree.CharacterLiteral)
      return true;

    if (expr instanceof Tree.DeclRefExpr) {
      Tree.DeclRefExpr declRef = (Tree.DeclRefExpr) expr;
      if (declRef.getDecl() instanceof Decl.VarDecl) {
        Decl.VarDecl vd = (Decl.VarDecl) declRef.getDecl();
        if (vd.hasLocalStorage() && !vd.getType().isVolatileQualified())
          return true;
      }
    }
    return false;
  }

  @Override
  public Value visitSizeofAlignofExpr(SizeOfAlignOfExpr expr) {
    Type llvmTy;
    QualType ty;
    if (expr.isArgumentType()) {
      ty = expr.getArgumentType();
    } else {
      Util.assertion(expr.isArgumentExpr());
      ty = expr.getArgumentExpr().getType();
    }
    long size = cgf.getContext().getTypeSize(ty) / 8;
    llvmTy = cgf.convertType(cgf.getContext().getSizeType());
    return ConstantInt.get(llvmTy, size);
  }
}
