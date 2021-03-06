package backend.value;
/*
 * Extremely C Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng
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

import backend.ir.SelectInst;
import backend.transform.utils.ConstantFolder;
import backend.type.PointerType;
import backend.type.Type;
import backend.type.VectorType;
import backend.value.Instruction.CmpInst.Predicate;
import backend.value.Instruction.GetElementPtrInst;
import backend.value.UniqueConstantValueImpl.ExprMapKeyType;
import tools.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static backend.transform.utils.ConstantFolder.constantFoldCastInstruction;
import static backend.value.Instruction.CmpInst.Predicate.*;
import static backend.value.UniqueConstantValueImpl.getUniqueImpl;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public abstract class ConstantExpr extends Constant {
  protected Operator opcode;

  /**
   * Constructs a new instruction representing the specified constants.
   *
   * @param ty
   */
  protected ConstantExpr(Type ty, Operator opcode) {
    super(ty, ValueKind.ConstantExprVal);
    this.opcode = opcode;
  }

  public static Constant getTruncOrBitCast(Constant c, Type ty) {
    if (c.getType().getScalarSizeBits() == ty.getScalarSizeBits())
      return getBitCast(c, ty);
    return getTrunc(c, ty);
  }

  @Override
  public boolean isNullValue() {
    return false;
  }

  public static Constant getSizeOf(Type ty) {
    ArrayList<Constant> index = new ArrayList<>();
    index.add(ConstantInt.get(Type.getInt32Ty(ty.getContext()), 1));

    Constant gep = getGetElementPtr(Constant.getNullValue(PointerType.getUnqual(ty)), index, false);
    return getCast(Operator.PtrToInt, gep, Type.getInt64Ty(ty.getContext()));
  }

  public static Constant getCast(Operator op, Constant c, Type ty) {
    Util.assertion(op.isCastOps(), "opcode out of range");
    Util.assertion(c != null && ty != null, "Null arguments to getCast");
    Util.assertion(ty.isFirstClassType(), "Cannot cast to an aggregate type!");

    switch (op) {
      default:
        Util.shouldNotReachHere("Invalid cast opcode");
        break;
      case Trunc:
        return getTrunc(c, ty);
      case ZExt:
        return getZExt(c, ty);
      case SExt:
        return getSExt(c, ty);
      case FPTrunc:
        return getFPTrunc(c, ty);
      case FPExt:
        return getFPExt(c, ty);
      case UIToFP:
        return getUIToFP(c, ty);
      case SIToFP:
        return getSIToFP(c, ty);
      case FPToSI:
        return getFPToSI(c, ty);
      case FPToUI:
        return getFPToUI(c, ty);
      case PtrToInt:
        return getPtrToInt(c, ty);
      case IntToPtr:
        return getIntToPtr(c, ty);
      case BitCast:
        return getBitCast(c, ty);
    }
    return null;
  }

  public static Constant getBitCast(Constant c, Type ty) {
    Util.assertion(c.getType().getScalarSizeBits() == ty.getScalarSizeBits(), "BitCast requires types of same width");


    // It is common to ask for a bitcast of a value to its own type, handle this
    // speedily.
    if (c.getType() == ty)
      return c;

    return getFoldedCast(Operator.BitCast, c, ty);
  }

  public static Constant getIntToPtr(Constant c, Type ty) {
    Util.assertion(ty.isPointerType(), "PtrToInt destination must be pointer");
    Util.assertion(c.getType().isIntegerTy(), "PtrToInt source must be integral");
    return getFoldedCast(Operator.IntToPtr, c, ty);
  }

  public static Constant getPtrToInt(Constant c, Type ty) {
    Util.assertion(c.getType().isPointerType(), "PtrToInt source must be pointer");
    Util.assertion(ty.isIntegerTy(), "PtrToInt destination must be integral");
    return getFoldedCast(Operator.PtrToInt, c, ty);
  }

  public static Constant getFPToUI(Constant c, Type ty) {
    Util.assertion(c.getType().isFloatingPointType() && ty.isIntegerTy(), "This is an illegal floating point to uint cast!");

    return getFoldedCast(Operator.FPToUI, c, ty);
  }

  public static Constant getFPToSI(Constant c, Type ty) {
    Util.assertion(c.getType().isFloatingPointType() && ty.isIntegerTy(), "This is an illegal floating point to sint cast!");

    return getFoldedCast(Operator.FPToSI, c, ty);
  }

  public static Constant getSIToFP(Constant c, Type ty) {
    Util.assertion(c.getType().isIntegerTy() && ty.isFloatingPointType(), "This is an illegal uint to floating point cast!");

    return getFoldedCast(Operator.SIToFP, c, ty);
  }

  public static Constant getUIToFP(Constant c, Type ty) {
    Util.assertion(c.getType().isIntegerTy() && ty.isFloatingPointType(), "This is an illegal uint to floating point cast!");

    return getFoldedCast(Operator.UIToFP, c, ty);
  }

  public static Constant getFPExt(Constant c, Type ty) {
    Util.assertion(c.getType().isFloatingPointType() && ty.isFloatingPointType()
        && c.getType().getScalarSizeBits() < ty.getScalarSizeBits(), "This is an illegal floating point extension!");


    return getFoldedCast(Operator.FPExt, c, ty);
  }

  public static Constant getFPTrunc(Constant c, Type ty) {
    Util.assertion(c.getType().isFloatingPointType() && ty.isFloatingPointType()
        && c.getType().getScalarSizeBits() > ty.getScalarSizeBits(), "This is an illegal floating point truncation!");


    return getFoldedCast(Operator.FPTrunc, c, ty);
  }

  public static Constant getSExt(Constant c, Type ty) {
    Util.assertion(c.getType().isIntegerTy(), "SExt operand must be integer");
    Util.assertion(ty.isIntegerTy(), "SExt produces only integral");
    Util.assertion(c.getType().getScalarSizeBits() < ty.getScalarSizeBits(), "SrcTy must be smaller than DestTy for Trunc!");


    return getFoldedCast(Operator.SExt, c, ty);
  }

  public static Constant getZExt(Constant c, Type ty) {
    Util.assertion(c.getType().isIntegerTy(), "ZExt operand must be integer");
    Util.assertion(ty.isIntegerTy(), "ZExt produces only integral");
    Util.assertion(c.getType().getScalarSizeBits() < ty.getScalarSizeBits(), "SrcTy must be smaller than DestTy for Trunc!");
    return getFoldedCast(Operator.ZExt, c, ty);
  }

  public static Constant getTrunc(Constant c, Type ty) {
    Util.assertion(c.getType().isIntegerTy(), "Trunc operand must be integer");
    Util.assertion(ty.isIntegerTy(), "Trunc produces only integral");
    Util.assertion(c.getType().getScalarSizeBits() > ty.getScalarSizeBits(), "SrcTy must be larger than DestTy for Trunc!");
    return getFoldedCast(Operator.Trunc, c, ty);
  }

  public static Constant getIntegerCast(Constant c, Type ty, boolean isSigned) {
    int srcSize = c.getType().getScalarSizeBits();
    int destSize = ty.getScalarSizeBits();
    if (srcSize == destSize)
      return c;
    Operator opc = destSize > srcSize ? (isSigned ? Operator.SExt : Operator.ZExt) :
        Operator.Trunc;
    return getFoldedCast(opc, c, ty);
  }

  /**
   * This is an utility function to handle folding of casts and lookup for the
   * cast in the ExprConstantMaps.
   *
   * @param op
   * @param c
   * @param ty
   * @return
   */
  private static Constant getFoldedCast(Operator op, Constant c, Type ty) {
    Util.assertion(ty.isFirstClassType(), "Cannot cast to an aggregate type!");
    Constant res = constantFoldCastInstruction(op, c, ty);
    if (res != null) return res;

    ExprMapKeyType key = new ExprMapKeyType(op, c, ty);
    return getUniqueImpl().getOrCreate(key);
  }

  public Operator getOpcode() {
    return opcode;
  }

  public void setOpcode(Operator opc) {
    opcode = opc;
  }

  public Constant operand(int index) {
    return super.operand(index);
  }

  public static Constant getNeg(Constant c) {
    if (c.getType().isFloatingPointType())
      return getFNeg(c);
    Util.assertion(c.getType().isIntegerTy(), "Cann't NEG a non integral value!");
    return get(Operator.Sub, ConstantInt.getNullValue(c.getType()), c);
  }

  public static Constant getFNeg(Constant c) {
    Util.assertion(c.getType().isFloatingPointType(), "Can not NEG a non floating point value!");

    return get(Operator.FSub, ConstantFP.getNullValue(c.getType()), c);
  }

  public static Constant get(Operator op, Constant c1, Constant c2) {
    Util.assertion(op.compareTo(Operator.BinaryOpsBegin) >= 0 &&
            op.compareTo(Operator.BinaryOpsEnd) < 0,
        "invalid binary opcode in binary constant expresion");
    Util.assertion(c1.getType().equals(c2.getType()),
        "Operand type in binary constant expresion should be indentical");

    if (c1.getType().isFloatingPointType()) {
      if (op == Operator.Add) op = Operator.FAdd;
      else if (op == Operator.Sub) op = Operator.FSub;
      else if (op == Operator.Mul) op = Operator.FMul;
    }
    ArrayList<Constant> list = new ArrayList<>(2);
    list.add(c1);
    list.add(c2);
    ExprMapKeyType key = new ExprMapKeyType(op, list, c1.getType());
    return getUniqueImpl().getOrCreate(key);
  }

  public static Constant getAdd(Constant lhs, Constant rhs) {
    return get(Operator.Add, lhs, rhs);
  }

  public static Constant getSub(Constant lhs, Constant rhs) {
    return get(Operator.Sub, lhs, rhs);
  }

  public static Constant getMul(Constant lhs, Constant rhs) {
    return get(Operator.Mul, lhs, rhs);
  }

  public static Constant getSDiv(Constant lhs, Constant rhs) {
    return get(Operator.SDiv, lhs, rhs);
  }

  public static Constant getUDiv(Constant lhs, Constant rhs) {
    return get(Operator.UDiv, lhs, rhs);
  }

  public static Constant getShl(Constant lhs, Constant rhs) {
    return get(Operator.Shl, lhs, rhs);
  }

  public static Constant getLShr(Constant lhs, Constant rhs) {
    return get(Operator.LShr, lhs, rhs);
  }

  public static Constant getAShr(Constant lhs, Constant rhs) {
    return get(Operator.AShr, lhs, rhs);
  }

  public static Constant getICmp(Predicate predicate,
                                 Constant lhs, Constant rhs) {
    Util.assertion(lhs.getType().equals(rhs.getType()));
    Util.assertion(predicate.ordinal() >= FIRST_ICMP_PREDICATE.ordinal() && predicate.ordinal() <= LAST_ICMP_PREDICATE.ordinal());

    Constant res = ConstantFolder.constantFoldCompareInstruction(predicate, lhs, rhs);
    if (res != null)
      return res;

    // FIXME.
    return BinaryConstantExpr.get(Operator.ICmp, lhs, rhs);
  }

  public static Constant getFCmp(Predicate predicate,
                                 Constant lhs, Constant rhs) {
    Util.assertion(lhs.getType().equals(rhs.getType()));
    Util.assertion(predicate.compareTo(FIRST_FCMP_PREDICATE) >= 0 && predicate.compareTo(LAST_FCMP_PREDICATE) >= 0);


    Constant res = ConstantFolder.constantFoldCompareInstruction(predicate, lhs, rhs);
    if (res != null)
      return res;

    // FIXME.
    return BinaryConstantExpr.get(Operator.FCmp, lhs, rhs);
  }

  public static Constant getNot(ConstantInt value) {
    Util.assertion(value.getType().isIntegerTy());
    return get(Operator.Xor, value, Constant.getAllOnesValue(value.getType()));
  }

  public static Constant getCompareTy(
      Predicate predicate,
      Constant lhs,
      Constant rhs) {
    switch (predicate) {
      case FCMP_FALSE:  /// 0 0 0 0    Always false (always folded)
      case FCMP_OEQ:  /// 0 0 0 1    True if ordered and equal
      case FCMP_OGT:  /// 0 0 1 0    True if ordered and greater than
      case FCMP_OGE:  /// 0 0 1 1    True if ordered and greater than or equal
      case FCMP_OLT:  /// 0 1 0 0    True if ordered and less than
      case FCMP_OLE:
      case FCMP_ONE:
      case FCMP_ORD:
      case FCMP_UNO:
      case FCMP_UEQ:
      case FCMP_UGT:
      case FCMP_UGE:
      case FCMP_ULT:
      case FCMP_ULE:
      case FCMP_UNE:
      case FCMP_TRUE:
        return getFCmp(predicate, lhs, rhs);
      case ICMP_EQ:  /// equal
      case ICMP_NE:  /// not equal
      case ICMP_UGT:  /// unsigned greater than
      case ICMP_UGE:  /// unsigned greater or equal
      case ICMP_ULT:  /// unsigned less than
      case ICMP_ULE:  /// unsigned less or equal
      case ICMP_SGT:  /// signed greater than
      case ICMP_SGE:  /// signed greater or equal
      case ICMP_SLT:  /// signed less than
      case ICMP_SLE:
        return getICmp(predicate, lhs, rhs);
      default:
        Util.assertion(false, "Unknown comparison instruction");
        return null;
    }
  }

  public static Constant getCompare(
      Predicate predicate,
      Constant lhs,
      Constant rhs) {
    Util.assertion(lhs.getType().equals(rhs.getType()), "Compare must have same type of operand");
    return getCompareTy(predicate, lhs, rhs);
  }

  public boolean isCast() {
    return Instruction.isCast(getOpcode());
  }

  public boolean isCompare() {
    return getOpcode() == Operator.ICmp || getOpcode() == Operator.FCmp;
  }

  public Predicate getPredicate() {
    Util.assertion(isCompare());
    return ((CmpConstantExpr) this).predicate;
  }

  private static Constant getGetElementPtrTy(Type ty, Constant c, List<Value> indices, boolean isInBounds) {
    Util.assertion(GetElementPtrInst.getIndexedType(c.getType(), indices).equals(((PointerType) ty).getElementType()), "GEP indices invalid!");

    Util.assertion(c.getType() instanceof PointerType, "Non-pointer type for constant GetElementPtr expression");

    ArrayList<Constant> elts = new ArrayList<>();
    elts.add(c);
    indices.forEach(ind -> elts.add((Constant) ind));
    ExprMapKeyType key = new ExprMapKeyType(Operator.GetElementPtr, elts, ty, isInBounds);
    return getUniqueImpl().getOrCreate(key);
  }

  public static Constant getGetElementPtr(Constant base, List<Constant> elts) {
    return getGetElementPtr(base, elts, false);
  }

  public static Constant getGetElementPtr(Constant base, List<Constant> elts, boolean isInBounds) {
    ArrayList<Value> ops = new ArrayList<>(elts);
    Type ty = GetElementPtrInst.getIndexedType(base.getType(), ops);
    Util.assertion(ty != null, "GEP indices invalid");
    int as = ((PointerType) base.getType()).getAddressSpace();
    return getGetElementPtrTy(PointerType.get(ty, as), base, ops, isInBounds);
  }

  public static Constant getInBoundsGetElementPtr(Constant c, List<Constant> idxs) {
    Constant result = getGetElementPtr(c, idxs, true);
    if (result instanceof GetElementPtrConstantExpr)
      ((GetElementPtrConstantExpr) result).setIsInBounds(true);

    return result;
  }

  @Override
  public void replaceUsesOfWithOnConstant(Value from, Value to, Use u) {
    Util.assertion(to instanceof Constant, "Can't make constant refer to non-constant!");
    Constant toV = (Constant) to;

    Constant replacement = null;
    if (getOpcode() == Operator.GetElementPtr) {
      ArrayList<Constant> indices = new ArrayList<>();
      Constant pointer = operand(0);
      if (pointer.equals(from)) pointer = toV;

      for (int i = 1, e = getNumOfOperands(); i < e; i++) {
        Constant val = operand(i);
        if (val.equals(from)) val = toV;
        indices.add(val);
      }
      replacement = ConstantExpr.getGetElementPtr(pointer, indices, ((GetElementPtrConstantExpr)this).isInBounds());
    } else if (isCast()) {
      Util.assertion(operand(0).equals(from), "Cast only has one use!");
      replacement = ConstantExpr.getCast(getOpcode(), toV, getType());
    } else if (isCompare()) {
      Constant c1 = operand(0);
      Constant c2 = operand(1);
      if (c1.equals(from)) c1 = toV;
      if (c2.equals(from)) c2 = toV;
      if (getOpcode() == Operator.ICmp)
        replacement = ConstantExpr.getICmp(getPredicate(), c1, c2);
      else {
        Util.assertion(getOpcode() == Operator.FCmp);
        replacement = ConstantExpr.getFCmp(getPredicate(), c1, c2);
      }
    } else if (getNumOfOperands() == 2) {
      Constant c1 = operand(0);
      Constant c2 = operand(1);
      if (c1.equals(from)) c1 = toV;
      if (c2.equals(from)) c2 = toV;

      replacement = ConstantExpr.get(getOpcode(), c1, c2);
    } else {
      Util.shouldNotReachHere("Unknown ConstantExpr type!");
      return;
    }
    Util.assertion(replacement != this, "Didn't contain from!");
    replaceAllUsesWith(replacement);

    // delete constant.
    destroyConstant();
  }

  public void destroyConstant() {
    getUniqueImpl().remove(this);
  }

  /**
   * This returns the current constant expression with the
   * operands replaced with the specified values.  The specified operands must
   * match count and type with the existing ones.
   * @param newOps
   * @return
   */
  public Constant getWithOperands(ArrayList<Constant> newOps) {
    Util.assertion(newOps.size() == getNumOfOperands(), "Operand count mismatch");
    boolean anyChange = false;
    for (int i = 0; i < getNumOfOperands(); i++) {
      Util.assertion(newOps.get(i).getType().equals(operand(i).getType()),
          "Operand type mismatch!");
      anyChange |= !newOps.get(i).equals(operand(i));
    }

    if (!anyChange)
      return this;

    switch (getOpcode()) {
      case Trunc:
      case ZExt:
      case SExt:
      case FPTrunc:
      case FPExt:
      case UIToFP:
      case SIToFP:
      case FPToUI:
      case FPToSI:
      case PtrToInt:
      case IntToPtr:
      case BitCast:
        return ConstantExpr.getCast(getOpcode(), newOps.get(0), getType());
      case Select:
        return ConstantExpr.getSelect(newOps.get(0), newOps.get(1),newOps.get(2));
      case InsertElement:
        return ConstantExpr.getInsertElement(newOps.get(0), newOps.get(1), newOps.get(2));
      case ExtractElement:
        return ConstantExpr.getExtractElement(newOps.get(0), newOps.get(1));
      case ShuffleVector:
        return ConstantExpr.getShuffleVector(newOps.get(0), newOps.get(1), newOps.get(2));
      case GetElementPtr:
        return ((GetElementPtrConstantExpr)this).isInBounds() ?
            ConstantExpr.getInBoundsGetElementPtr(newOps.get(0), newOps.subList(1, newOps.size())) :
            ConstantExpr.getGetElementPtr(newOps.get(0), newOps.subList(1, newOps.size()), false);
      case ICmp:
      case FCmp:
        return ConstantExpr.getCompare(getPredicate(), newOps.get(0), newOps.get(1));
      default:
        Util.assertion(getNumOfOperands() == 2, "Must be binary operator?");
        return ConstantExpr.get(getOpcode(), newOps.get(0), newOps.get(1));
    }
  }

  public static Constant getShuffleVector(Constant val1, Constant val2, Constant mask) {
    Util.assertion(Instruction.ShuffleVectorInst.isValidOperands(val1, val2, mask),
        "Invalid shuffle vector constant expr operands!");
    int numElts = (int) ((VectorType)mask.getType()).getNumElements();
    Type eltTy = ((VectorType)val1.getType()).getElementType();
    Type shuffleTy = VectorType.get(eltTy, numElts);
    return getShuffleVectorTy(shuffleTy, val1, val2, mask);
  }

  protected static Constant getShuffleVectorTy(Type reqTy, Constant val1, Constant val2, Constant mask) {
    ArrayList<Constant> argVec = new ArrayList<>();
    argVec.add(val1);
    argVec.add(val2);
    argVec.add(mask);
    return getCompositeTy(reqTy, Operator.ShuffleVector, argVec);
  }

  public static Constant getExtractElement(Constant val, Constant idx) {
    Util.assertion(val.getType().isVectorTy(),
        "Tried to create extractelement operation on non-vector type!");
    Util.assertion(idx.getType().isIntegerTy(32),
        "extractelement index must be i32 type!");
    return getExtractElementTy(((VectorType)val.getType()).getElementType(), val, idx);
  }

  protected static Constant getExtractElementTy(Type reqTy, Constant val, Constant idx) {
    ArrayList<Constant> argVec = new ArrayList<>();
    argVec.add(val);
    argVec.add(idx);
    return getCompositeTy(reqTy, Operator.ExtractElement, argVec);
  }

  public static Constant getInsertElement(Constant val, Constant elt, Constant idx) {
    Util.assertion(val.getType().isVectorTy(),
        "Tried to create insertelement operation on non-vector type!");
    Util.assertion(elt.getType().equals(((VectorType)val.getType()).getElementType()),
        "InsertElement types must match!");
    Util.assertion(idx.getType().isIntegerTy(32),
        "InsertElement idnex must be i32 type!");
    return getInsertElementTy(val.getType(), val, elt, idx);
  }

  protected static Constant getInsertElementTy(Type reqTy, Constant val, Constant elt, Constant idx) {
    ArrayList<Constant> argVec = new ArrayList<>();
    argVec.add(val);
    argVec.add(elt);
    argVec.add(idx);
    return getCompositeTy(reqTy, Operator.InsertElement, argVec);
  }

  public static Constant getInsertValue(Constant agg, Constant val,
                                         int[] idxList) {
    Util.assertion(agg.getType().isFirstClassType(),
        "Tried to create insertvalue operation on non-first-class type");
    Type reqTy = agg.getType();
    Type valTy = Instruction.ExtractValueInst.getIndexedType(reqTy, idxList);
    Util.assertion(reqTy.equals(valTy), "insertvalue indices invalid!");
    return getInsertValueTy(reqTy, agg, val, idxList);
  }

  protected static Constant getInsertValueTy(Type reqTy, Constant agg, Constant val, int[] idxList) {
    Util.shouldNotReachHere("getInsertedValueTy is not implemented as yet!");
    return null;
  }

  protected static Constant getCompositeTy(Type reqTy, Operator opc, ArrayList<Constant> argVec) {
    ExprMapKeyType key = new ExprMapKeyType(opc, argVec, reqTy);
    return getUniqueImpl().getOrCreate(key);
  }

  public static Constant getSelect(Constant cond, Constant lhs, Constant rhs) {
    return getSelectTy(lhs.getType(), cond, lhs, rhs);
  }

  protected static Constant getSelectTy(Type reqTy, Constant cond, Constant lhs, Constant rhs) {
    Util.assertion(SelectInst.areInvalidOperands(cond, lhs, rhs) == null,
        "Invalid select operands");

    ArrayList<Constant> argVec = new ArrayList<>();
    argVec.add(cond);
    argVec.add(lhs);
    argVec.add(rhs);
    ExprMapKeyType key = new ExprMapKeyType(Operator.Select, argVec, reqTy);
    return getUniqueImpl().getOrCreate(key);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ConstantExpr that = (ConstantExpr) o;
    if (opcode != that.opcode || getNumOfOperands() != that.getNumOfOperands()) return false;
    for (int i = 0, e = getNumOfOperands(); i < e; ++i)
      if (!Objects.equals(operand(i), that.operand(i)))
        return false;
    return true;
  }

  @Override
  public int hashCode() {
    int res = Util.hash2(super.hashCode(), opcode, getNumOfOperands());
    for (int i = 0, e = getNumOfOperands(); i < e; ++i)
      res = Util.hash1(res, operand(i));
    return res;
  }
}
