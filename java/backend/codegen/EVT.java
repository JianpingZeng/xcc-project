package backend.codegen;
/*
 * Extremely C language Compiler
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

import backend.support.LLVMContext;
import backend.type.IntegerType;
import backend.type.Type;
import backend.type.VectorType;
import tools.Util;

import java.util.Objects;

import static backend.codegen.MVT.*;
import static backend.type.LLVMTypeID.*;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class EVT implements Comparable<EVT> {
  private MVT v;
  private Type llvmTy;

  public EVT() {
    v = new MVT(INVALID_SIMPLE_VALUE_TYPE);
  }

  public EVT(int svt) {
    v = new MVT(svt);
  }

  public EVT(MVT vt) {
    v = vt;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (this == obj) return true;

    if (getClass() != obj.getClass())
      return false;
    EVT other = (EVT) obj;
    if (v.simpleVT == other.v.simpleVT) {
      if (v.simpleVT == INVALID_SIMPLE_VALUE_TYPE)
        return Objects.equals(llvmTy, other.llvmTy);
      return true;
    }
    return false;
  }

  /**
   * Return true if this is an overloaded type for TableGen.
   *
   * @return
   */
  public boolean isOverloaded() {
    switch (v.simpleVT) {
      case iAny:
      case fAny:
      case vAny:
      case iPTRAny:
        return true;
      default:
        return false;
    }
  }

  public static EVT getFloatingPointVT(int bitWidth) {
    return new EVT(MVT.getFloatingPointVT(bitWidth));
  }

  public static EVT getIntegerVT(LLVMContext context, int bitWidth) {
    MVT m = MVT.getIntegerVT(bitWidth);
    if (m.simpleVT == INVALID_SIMPLE_VALUE_TYPE)
      return getExtendedIntegerVT(context, bitWidth);
    else
      return new EVT(m);
  }

  public static EVT getVectorVT(LLVMContext context, EVT vt, int numElts) {
    MVT m = MVT.getVectorVT(vt.v, numElts);
    if (m.simpleVT == INVALID_SIMPLE_VALUE_TYPE)
      return getExtendedVectorVT(context, vt, numElts);
    else
      return new EVT(m);
  }

  public static EVT getIntVectorWithNumElements(LLVMContext context, int numElts) {
    MVT m = MVT.getIntVectorWithNumElements(numElts);
    if (m.simpleVT == INVALID_SIMPLE_VALUE_TYPE)
      return getVectorVT(context, new EVT(i8), numElts);
    else
      return new EVT(m);
  }

  public boolean isSimple() {
    return v.simpleVT <= LastSimpleValueType;
  }

  public boolean isExtended() {
    return !isSimple() && llvmTy != null;
  }

  public boolean isFloatingPoint() {
    return isSimple() ?
        (v.simpleVT >= f32 && v.simpleVT <= ppcf128) ||
            (v.simpleVT >= v2f32 && v.simpleVT <= v4f64) :
        isExtendedFloatingPoint();
  }

  public boolean isInteger() {
    return isSimple() ?
        (v.simpleVT >= FIRST_INTEGER_VALUETYPE &&
            v.simpleVT <= LAST_INTEGER_VALUETYPE) ||
            (v.simpleVT >= v2i8 && v.simpleVT <= v4i64)
        : isExtendedInteger();
  }

  public boolean isVector() {
    return isSimple() ? (v.simpleVT >= FIRST_VECTOR_VALUETYPE
        && v.simpleVT <= LAST_VECTOR_VALUETYPE) : isExtendedVector();
  }

  public boolean is64BitVector() {
    return isSimple() ? (v.simpleVT == v8i8 || v.simpleVT
        == v4i16 || v.simpleVT == v2i32
        || v.simpleVT == v1i64 || v.simpleVT == v2f32) : isExtended64BitVector();
  }

  public boolean is128BitVector() {
    return isSimple() ? (v.simpleVT == v16i8 || v.simpleVT
        == v8i16 || v.simpleVT == v4i32
        || v.simpleVT == v2i64 || v.simpleVT == v4f32 || v.simpleVT == v2f64)
        : isExtended128BitVector();
  }

  public boolean is256BitVector() {
    return isSimple() ? (v.simpleVT == v4i64 || v.simpleVT == v8i32
        || v.simpleVT == v16i16 || v.simpleVT == v32i8
        || v.simpleVT == v8f32 || v.simpleVT == v4f64)
        : isExtended256BitVector();
  }

  public boolean isByteSized() {
    return (getSizeInBits() & 0x7) == 0;
  }

  public int getSizeInBits() {
    return isSimple() ? v.getSizeInBits() : getExtendedSizeInBits();
  }

  public int getStoreSizeInBits() {
    return (getSizeInBits() + 7) / 8 * 8;
  }

  public int getStoreSize() {
    return getStoreSizeInBits() * 8;
  }

  public EVT getRoundIntegerType(LLVMContext context) {
    Util.assertion(isInteger() && !isVector(), "Invalid integer type!");
    int bitwidth = getSizeInBits();
    if (bitwidth <= 8)
      return new EVT(i8);
    else
      return getIntegerVT(context, 1 << Util.log2Ceil(bitwidth));
  }

  public boolean isPow2VectorType() {
    int numElts = getVectorNumElements();
    return (numElts & (numElts - 1)) == 0;
  }

  public int getVectorNumElements() {
    Util.assertion(isVector(), "Invalid vector type!");
    if (isSimple())
      return v.getVectorNumElements();
    else
      return getExtendedVectorNumElements();
  }

  public EVT getPow2VectorType(LLVMContext context) {
    if (!isPow2VectorType()) {
      int numElts = getExtendedVectorNumElements();
      int pow2NumElts = 1 << Util.log2(numElts);
      return EVT.getVectorVT(context, getExtendedVectorElementType(), pow2NumElts);
    } else
      return this;
  }

  /**
   * This function returns value type as a string, e.g. "i32".
   *
   * @return
   */
  public String getEVTString() {
    switch (v.simpleVT) {
      default:
        if (isVector()) {
          return "v" +
              getExtendedVectorNumElements() +
              getExtendedVectorElementType().toString();
        }
        if (isInteger())
          return "i" + getSizeInBits();
        Util.shouldNotReachHere("Invalid EVT!");
        return "?";
      case i1:
        return "i1";
      case i8:
        return "i8";
      case i16:
        return "i16";
      case i32:
        return "i32";
      case i64:
        return "i64";
      case i128:
        return "i128";
      case f16:
        return "f16";
      case f32:
        return "f32";
      case f64:
        return "f64";
      case f80:
        return "f80";
      case f128:
        return "f128";
      case ppcf128:
        return "ppcf128";
      case isVoid:
        return "isVoid";
      case Other:
        return "ch";
      case Glue:
        return "glue";
      case v2i8:
        return "v2i8";
      case v4i8:
        return "v4i8";
      case v8i8:
        return "v8i8";
      case v16i8:
        return "v16i8";
      case v32i8:
        return "v32i8";
      case v2i16:
        return "v2i16";
      case v4i16:
        return "v4i16";
      case v8i16:
        return "v8i16";
      case v16i16:
        return "v16i16";
      case v2i32:
        return "v2i32";
      case v4i32:
        return "v4i32";
      case v8i32:
        return "v8i32";
      case v1i64:
        return "v1i64";
      case v2i64:
        return "v2i64";
      case v4i64:
        return "v4i64";
      case v2f32:
        return "v2f32";
      case v4f32:
        return "v4f32";
      case v8f32:
        return "v8f32";
      case v2f64:
        return "v2f64";
      case v4f64:
        return "v4f64";
      case v8f64:
        return "v8f64";
      case x86mmx:
        return "x86mmx";
    }
  }

  /**
   * This method returns an LLVM type corresponding to the
   * specified EVT.  For integer types, this returns an int type.  Note
   * that this will abort for types that cannot be represented.
   *
   * @return
   */
  public Type getTypeForEVT(LLVMContext context) {
    switch (v.simpleVT) {
      default:
        Util.assertion(isExtended(), "Type is not extended!");
        Util.assertion("Vector type currently not supported");
        return llvmTy;
      case isVoid:
        return Type.getVoidTy(context);
      case i1:
        return Type.getInt1Ty(context);
      case i8:
        return Type.getInt8Ty(context);
      case i16:
        return Type.getInt16Ty(context);
      case i32:
        return Type.getInt32Ty(context);
      case i64:
        return Type.getInt64Ty(context);
      case i128:
        return Type.getIntNTy(context, 128);
      case f32:
        return Type.getFloatTy(context);
      case f64:
        return Type.getDoubleTy(context);
      case f80:
        return Type.getX86_FP80Ty(context);
      case f128:
        return Type.getFP128Ty(context);
      case ppcf128:
        return null;
      case v2i8:
        return VectorType.get(Type.getInt8Ty(context), 2);
      case v4i8:
        return VectorType.get(Type.getInt8Ty(context), 4);
      case v8i8:
        return VectorType.get(Type.getInt8Ty(context), 8);
      case v16i8:
        return VectorType.get(Type.getInt8Ty(context), 16);
      case v32i8:
        return VectorType.get(Type.getInt8Ty(context), 32);
      case v2i16:
        return VectorType.get(Type.getInt16Ty(context), 2);
      case v4i16:
        return VectorType.get(Type.getInt16Ty(context), 4);
      case v8i16:
        return VectorType.get(Type.getInt16Ty(context), 8);
      case v16i16:
        return VectorType.get(Type.getInt16Ty(context), 16);
      case v2i32:
        return VectorType.get(Type.getInt32Ty(context), 2);
      case v4i32:
        return VectorType.get(Type.getInt32Ty(context), 4);
      case v8i32:
        return VectorType.get(Type.getInt32Ty(context), 8);
      case v1i64:
        return VectorType.get(Type.getInt64Ty(context), 1);
      case v2i64:
        return VectorType.get(Type.getInt64Ty(context), 2);
      case v4i64:
        return VectorType.get(Type.getInt64Ty(context), 4);
      case v2f32:
        return VectorType.get(Type.getFloatTy(context), 2);
      case v4f32:
        return VectorType.get(Type.getFloatTy(context), 4);
      case v8f32:
        return VectorType.get(Type.getFloatTy(context), 8);
      case v16f32:
        return VectorType.get(Type.getFloatTy(context), 16);
      case v2f64:
        return VectorType.get(Type.getDoubleTy(context), 2);
      case v4f64:
        return VectorType.get(Type.getDoubleTy(context), 4);
      case v8f64:
        return VectorType.get(Type.getDoubleTy(context), 8);
    }
  }

  public static EVT getEVT(Type ty) {
    return getEVT(ty, false);
  }

  /**
   * Return the value type corresponding to the specified type.
   * This returns all pointers as iPTR.  If HandleUnknown is true, unknown
   * types are returned as Other, otherwise they are invalid.
   *
   * @param ty
   * @param handleUnkown
   * @return
   */
  public static EVT getEVT(Type ty, boolean handleUnkown) {
    switch (ty.getTypeID()) {
      default:
        if (handleUnkown) return new EVT(Other);
        Util.shouldNotReachHere("Undefined type");
        return new EVT(isVoid);
      case VoidTyID:
        return new EVT(isVoid);
      case IntegerTyID:
        return getIntegerVT(ty.getContext(), ((IntegerType) (ty)).getBitWidth());
      case FloatTyID:
        return new EVT(new MVT(f32));
      case DoubleTyID:
        return new EVT(new MVT(f64));
      case X86_FP80TyID:
        return new EVT(new MVT(f80));
      case FP128TyID:
        return new EVT(new MVT(f128));
      case PointerTyID:
        return new EVT(new MVT(iPTR));
      case VectorTyID:
        VectorType vty = (VectorType) ty;
        return getVectorVT(vty.getContext(), getEVT(vty.getElementType(), false),
                (int) vty.getNumElements());
    }
  }

  public Object getRawBits() {
    return v.simpleVT <= LastSimpleValueType ? v.simpleVT : llvmTy;
  }


  @Override
  public String toString() {
    return getEVTString();
  }

  private static EVT getExtendedIntegerVT(LLVMContext context, int bitWidth) {
    EVT vt = new EVT();
    vt.llvmTy = Type.getIntNTy(context, bitWidth);
    Util.assertion(vt.isExtended(), "Type is not extended!");
    return vt;
  }

  private static EVT getExtendedVectorVT(LLVMContext context, EVT vt, int numElements) {
    EVT resultVT = new EVT();
    resultVT.llvmTy = VectorType.get(vt.getTypeForEVT(context), numElements);
    Util.assertion(resultVT.isExtended(), "Type is not extended!");
    return resultVT;
  }

  private boolean isExtendedFloatingPoint() {
    Util.assertion(isExtended(), "Type is not extended");
    return llvmTy.isFPOrFPVectorTy();
  }

  private boolean isExtendedInteger() {
    Util.assertion(isExtended(), "Type is not extended");
    return llvmTy.isIntOrIntVectorTy();
  }

  private boolean isExtendedVector() {
    Util.assertion(isExtended(), "Type is not extended");
    return llvmTy.isVectorTy();
  }

  private boolean isExtended64BitVector() {
    return isExtendedVector() && getSizeInBits() == 64;
  }

  private boolean isExtended128BitVector() {
    return isExtendedVector() && getSizeInBits() == 128;
  }

  private boolean isExtended256BitVector() {
    return isExtendedVector() && getSizeInBits() == 256;
  }

  private EVT getExtendedVectorElementType() {
    Util.assertion(isExtended(), "Type is not extended!");
    return getEVT(((VectorType)llvmTy).getElementType());
  }

  private int getExtendedVectorNumElements() {
    Util.assertion(isExtended(), "Type is not extended!");
    return (int) ((VectorType)llvmTy).getNumElements();
  }

  private int getExtendedSizeInBits() {
    Util.assertion(isExtended(), "Type is not extended!");
    if (llvmTy instanceof IntegerType) {
      return ((IntegerType) llvmTy).getBitWidth();
    }
    else if (llvmTy instanceof VectorType)
    {
        return (int) ((VectorType)llvmTy).getBitWidth();
    }
    Util.assertion("Extended type is not supported currently.");
    return 0;
  }

  public EVT getVectorElementType() {
    Util.assertion(isVector(), "Invalid vector type!");
    if (isSimple())
      return new EVT(v.getVectorElementType());
    else
      return getExtendedVectorElementType();
  }

  public MVT getSimpleVT() {
    Util.assertion(isSimple(), "Expected a int!");
    return v;
  }

  /// isRound - Return true if the size is a power-of-two number of bytes.
  public boolean isRound() {
    int BitSize = getSizeInBits();
    return BitSize >= 8 && (BitSize & (BitSize - 1)) == 0;
  }

  /// bitsEq - Return true if this has the same number of bits as VT.
  public boolean bitsEq(EVT VT) {
    return getSizeInBits() == VT.getSizeInBits();
  }

  /**
   * Return true if this has more bits than srcVT.
   *
   * @param srcVT
   * @return
   */
  public boolean bitsGT(EVT srcVT) {
    return getSizeInBits() > srcVT.getSizeInBits();
  }

  /// bitsGE - Return true if this has no less bits than VT.
  public boolean bitsGE(EVT VT) {
    return getSizeInBits() >= VT.getSizeInBits();
  }

  /// bitsLT - Return true if this has less bits than VT.
  public boolean bitsLT(EVT VT) {
    return getSizeInBits() < VT.getSizeInBits();
  }

  /// bitsLE - Return true if this has no more bits than VT.
  public boolean bitsLE(EVT VT) {
    return getSizeInBits() <= VT.getSizeInBits();
  }

  @Override
  public int compareTo(EVT o) {
    if (v.simpleVT == o.v.simpleVT)
      return llvmTy.hashCode() - o.llvmTy.hashCode();
    else
      return v.simpleVT - o.v.simpleVT;
  }

  /**
   * If this is a vector type, return the type of it's element. Otherwise return this.
   * @return
   */
  public EVT getScalarType() {
    return isVector() ? getVectorElementType() : this;
  }
}
