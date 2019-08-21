package backend.type;
/*
 * Extremely C language CompilerInstance
 * Copyright (c) 2015-2019, Jianping Zeng
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

import backend.support.LLVMContext;
import backend.support.TypePrinting;
import backend.value.Module;
import tools.Util;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * This is a core base class for representing backend type of value.
 * <p>
 * The instances of the Type class are immutable: once they are created,
 * they are never changed.  Also note that only one instance of a particular
 * type is ever created.  Thus seeing if two types are equal is a matter of
 * doing a trivial pointer comparison. To enforce that no two equal instances
 * are created, Type instances can only be created via static factory methods
 * in class Type and in derived classes.
 * </p>
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class Type implements LLVMTypeID, AbstractTypeUser {
  /**
   * The current base type of this type.
   */
  private int id;
  protected boolean isAbstract;

  /**
   * Implement a list of the users that need to be notified if i am a type, and
   * i get resolved into a more concrete type.
   */
  protected LinkedList<AbstractTypeUser> abstractTypeUsers;

  /**
   * A pointer to the array of Types (PATypeHandle) contained
   * by this Type.  For example, this includes the arguments of a function
   * type, the elements of a structure, the pointee of a pointer, the element
   * type of an array, etc.  This pointer may be 0 for types that don't
   * contain other types (Integer, Double, Float).  In general, the subclass
   * should arrange for space for the PATypeHandles to be included in the
   * allocation of the type object and set this pointer to the address of the
   * first element. This allows the Type class to manipulate the ContainedTys
   * without understanding the subclass's placement for this array.  keeping
   * it here also allows the subtype_* members to be implemented MUCH more
   * efficiently, and dynamically very few types do not contain any elements.
   */
  protected PATypeHandle containedTys[];

  private static HashMap<Type, String> concreteTypeDescription =
      new HashMap<>();
  /**
   * This refers to the LLVMContext in which this type is uniqued.
   */
  private LLVMContext context;

  public Type(LLVMContext ctx, int typeID) {
    context = ctx;
    id = typeID;
    isAbstract = false;
    abstractTypeUsers = new LinkedList<>();
  }

  public void setAbstract(boolean anAbstract) {
    isAbstract = anAbstract;
  }

  public boolean isAbstract() {
    return isAbstract;
  }

  public int getTypeID() {
    return id;
  }

  public void print(PrintStream os) {
    new TypePrinting().print(this, os);
  }

  public void dump() {
    dump(null);
  }

  public void dump(Module context) {
    // TODO: 17-6-11 writeTypeSymbolic(System.err, this, context);
    //
    System.err.println();
  }

  public String getDescription() {
    switch (getTypeID()) {
      case VoidTyID:
        return "void";
      case IntegerTyID:
        return "i" + ((IntegerType) this).getBitWidth();
      case FloatTyID:
        return "f32";
      case DoubleTyID:
        return "f64";
      case LabelTyID:
        return "label";
      default:
        return "<unknown type>";
    }
  }

  public boolean isIntegerTy() {
    return id == IntegerTyID;
  }

  public boolean isIntOrVectorTy() {
    if (isIntegerTy()) return true;
    if (id != VectorTyID) return false;
    return ((VectorType)this).getElementType().isIntegerTy();
  }

  public boolean isFPOrFPVectorTy() {
    if (isFloatingPointType())
      return true;
    if (id != VectorTyID) return false;
    return ((VectorType)this).getElementType().isFloatingPointType();
  }

  public int getPrimitiveSizeInBits() {
    switch (getTypeID()) {
      case FloatTyID:
        return 32;
      case DoubleTyID:
        return 64;
      case X86_FP80TyID:
        return 80;
      case FP128TyID:
        return 128;
      case IntegerTyID:
        return ((IntegerType) this).getBitWidth();
      default:
        return 0;
    }
  }

  public boolean isSigned() {
    return false;
  }

  public boolean isUnsigned() {
    return !isSigned();
  }

  public boolean isIntegerTy(int bitwidth) {
    return isIntegerTy() && ((IntegerType)this).getBitWidth() == bitwidth;
  }

  public boolean isIntegral() {
    return isIntegerTy() || this == Type.getInt1Ty(getContext());
  }

  public boolean isPrimitiveType() {
    return id < FirstDerivedTyID;
  }

  public boolean isDerivedType() {
    return id >= FirstDerivedTyID;
  }

  public boolean isFunctionType() {
    return id == FunctionTyID;
  }

  public boolean isArrayType() {
    return id == ArrayTyID;
  }

  public boolean isPointerType() {
    return id == PointerTyID;
  }

  public boolean isStructType() {
    return id == StructTyID;
  }

  public boolean isVoidType() {
    return id == VoidTyID;
  }

  public boolean isFloatingPointType() {
    return id == FloatTyID || id == DoubleTyID
        || id == X86_FP80TyID || id == FP128TyID;
  }

  public boolean isFloatTy() {
    return id == FloatTyID;
  }

  public boolean isDoubleTy() {
    return id == DoubleTyID;
  }

  public boolean isX86_FP80Ty() {
    return id == X86_FP80TyID;
  }

  public boolean isFP128Ty() {
    return id == FP128TyID;
  }

  public boolean isPPC_FP128Ty() {
    return id == PPC_FP128TyID;
  }

  public boolean isLabelTy() {
    return id == LabelTyID;
  }

  public boolean isMetadataTy() {
    return id == MetadataTyID;
  }

  public boolean isVectorTy() {
    return id == VectorTyID;
  }

  public boolean isOpaqueTy() {
    return id == OpaqueTyID;
  }

  /**
   * Return true if the type is "first class", meaning it
   * is a valid type for a Value.
   */
  public boolean isFirstClassType() {
    // There are more first-class kinds than non-first-class kinds, so a
    // negative test is simpler than a positive one.
    return id != FunctionTyID && id != VoidTyID && id != OpaqueTyID;
  }

  /**
   * Return true if the type is a valid type for a virtual register in codegen.
   * This include all first-class type except struct and array type.
   *
   * @return
   */
  public boolean isSingleValueType() {
    return id != VoidTyID && id <= LastPrimitiveTyID
        || (id >= IntegerTyID && id <= IntegerTyID) ||
        id == PointerTyID;
  }

  /**
   * Return true if the type is an aggregate type. it means it is valid as
   * the first operand of an insertValue or extractValue instruction.
   * This includes struct and array types.
   *
   * @return
   */
  public boolean isAggregateType() {
    return id == StructTyID || id == ArrayTyID;
  }

  /**
   * Checks if this type could holded in register.
   *
   * @return
   */
  public boolean isHoldableInRegister() {
    return isPrimitiveType() || id == PointerTyID;
  }

  /**
   * Return true if it makes sense to take the size of this type.
   * To get the actual size for a particular TargetData, it is reasonable
   * to use the TargetData subsystem to do that.
   *
   * @return
   */
  public boolean isSized() {
    // If it's a primitive, it is always sized.
    if (id == IntegerTyID || isFloatingPointType() ||
        id == PointerTyID || id == X86_MMXTyID)
      return true;

    if (id != StructTyID && id != ArrayTyID && id != VectorTyID)
      return false;
    // Otherwise we have to try harder to decide.
    return isSizedDerivedType() || !isAbstract();
  }

  /**
   * Returns true if the derived type is sized.
   * DerivedType is sized if and only if all members of it are sized.
   *
   * @return
   */
  private boolean isSizedDerivedType() {
    if (isIntegerTy())
      return true;

    if (isArrayType()) {
      return ((ArrayType) this).getElementType().isSized();
    }
    if (isVectorTy())
      return ((VectorType)this).getElementType().isSized();

    if (!isStructType())
      return false;
    StructType st = (StructType) this;
    for (int i = 0, e = st.getNumOfElements(); i != e; i++)
      if (!st.getElementType(i).isSized())
        return false;

    return true;
  }

  public Type getScalarType() {
    return this;
  }

  public int getScalarSizeBits() {
    return getScalarType().getPrimitiveSizeInBits();
  }

  @Override
  public void refineAbstractType(DerivedType oldTy, Type newTy) {
    Util.assertion(oldTy != newTy);
    if (newTy.isOpaqueTy())
      newTy = newTy.getForwardType();

    for (int i = 0, e = getNumContainedTypes(); i < e; i++) {
      if (containedTys[i].getType() == oldTy)
        containedTys[i].setType(newTy);
    }
    oldTy.abstractTypeUsers.remove(this);
    isAbstract = false;
  }

  @Override
  public void typeBecameConcrete(DerivedType absTy) {
    Util.shouldNotReachHere("DerivedType is already a concrete type!");
  }

  public void addAbstractTypeUser(AbstractTypeUser user) {
    abstractTypeUsers.add(user);
  }

  public void removeAbstractTypeUser(AbstractTypeUser user) {
    abstractTypeUsers.remove(user);
  }

  public int getNumContainedTypes() {
    return containedTys == null ? 0 : containedTys.length;
  }

  public Type getContainedType(int idx) {
    Util.assertion(containedTys != null && idx >= 0 && idx < containedTys.length);
    Type ty = containedTys[idx].getType();
    if (ty != null && ty.isOpaqueTy()) {
      return ty.getForwardType();
    }
    return ty;
  }

  public Type getForwardType() {
    return this;
  }

  public LLVMContext getContext() {
    return context;
  }

  private Type getPointerTo(int as) { return PointerType.get(this, as); }

  public static Type getVoidTy(LLVMContext ctx) { return ctx.VoidTy; }
  public static Type getLabelTy(LLVMContext ctx) { return ctx.LabelTy; }
  public static Type getFloatTy(LLVMContext ctx) { return ctx.FloatTy; }
  public static Type getDoubleTy(LLVMContext ctx) { return ctx.DoubleTy; }
  public static Type getMetadataTy(LLVMContext ctx) { return ctx.MetadataTy; }
  public static Type getX86_FP80Ty(LLVMContext ctx) { return ctx.X86_FP80Ty; }
  public static Type getFP128Ty(LLVMContext ctx) { return ctx.FP128Ty; }
  public static Type getPPC_FP128Ty(LLVMContext ctx) { return ctx.PPC_FP128Ty; }
  public static Type getIntNTy(LLVMContext ctx, int bits) { return IntegerType.get(ctx, bits); }
  public static Type getInt1Ty(LLVMContext ctx) { return ctx.Int1Ty; }
  public static Type getInt8Ty(LLVMContext ctx) { return ctx.Int8Ty; }
  public static Type getInt16Ty(LLVMContext ctx) { return ctx.Int16Ty; }
  public static Type getInt32Ty(LLVMContext ctx) { return ctx.Int32Ty; }
  public static Type getInt64Ty(LLVMContext ctx) { return ctx.Int64Ty; }
  public static Type getFloatPtrTy(LLVMContext ctx, int as) { return getFloatTy(ctx).getPointerTo(as); }
  public static Type getDoublePtrTy(LLVMContext ctx, int as) { return getDoubleTy(ctx).getPointerTo(as); }
  public static Type getX86_FP80PtrTy(LLVMContext ctx, int as) { return getX86_FP80Ty(ctx).getPointerTo(as); }
  public static Type getFP128PtrTy(LLVMContext ctx, int as) { return getFP128Ty(ctx).getPointerTo(as); }
  public static Type getPPC_FP128PtrTy(LLVMContext ctx, int as) { return getPPC_FP128Ty(ctx).getPointerTo(as); }
  public static Type getIntNPtrTy(LLVMContext ctx, int bits, int as) { return getIntNTy(ctx, bits).getPointerTo(as); }
  public static Type getInt1PtrTy(LLVMContext ctx, int as) { return getInt1Ty(ctx).getPointerTo(as); }
  public static Type getInt8PtrTy(LLVMContext ctx, int as) { return getInt8Ty(ctx).getPointerTo(as); }
  public static Type getInt16PtrTy(LLVMContext ctx, int as) { return getInt16Ty(ctx).getPointerTo(as); }
  public static Type getInt32PtrTy(LLVMContext ctx, int as) { return getInt32Ty(ctx).getPointerTo(as); }
  public static Type getInt64PtrTy(LLVMContext ctx, int as) { return getInt64Ty(ctx).getPointerTo(as); }
}
