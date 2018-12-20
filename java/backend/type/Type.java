package backend.type;
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

  public Type(int typeID) {
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
        return "<unkown type>";
    }
  }

  public boolean isInteger() {
    return id == IntegerTyID;
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
    return false;
  }

  public boolean isIntegerType() {
    return false;
  }

  public boolean isIntegral() {
    return isIntegerType() || this == LLVMContext.Int1Ty;
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

  public boolean isLabelTy() {
    return id == LabelTyID;
  }

  public boolean isMetadataTy() {
    return id == MetadataTyID;
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
   * Return true if it makes sense to take the getNumOfSubLoop of this type.
   * To get the actual getNumOfSubLoop for a particular TargetData, it is reasonable
   * to use the TargetData subsystem to do that.
   *
   * @return
   */
  public boolean isSized() {
    if ((id >= IntegerTyID && id <= IntegerTyID)
        || isFloatingPointType() || id == PointerTyID)
      return true;

    if (id != StructTyID && id != ArrayTyID)
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
    if (isIntegerType())
      return true;

    if (isArrayType()) {
      return ((ArrayType) this).getElementType().isSized();
    }
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
    Util.shouldNotReachHere("Attempting to refine a derived type!");
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

    return containedTys[idx].getType();
  }
}
