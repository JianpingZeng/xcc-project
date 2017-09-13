package backend.codegen;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

import backend.type.IntegerType;
import backend.type.Type;
import tools.Util;

import static backend.codegen.MVT.*;
import static backend.type.LLVMTypeID.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class EVT
{
    private MVT v;
    private Type llvmTy;

    public EVT()
    {
        v = new MVT(INVALID_SIMPLE_VALUE_TYPE);
    }

    public EVT(int svt)
    {
        v = new MVT(svt);
    }

    public EVT(MVT vt)
    {
        v = vt;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) return false;
        if (this == obj) return true;

        if (getClass() != obj.getClass())
            return false;
        EVT other = (EVT)obj;
        if (v.simpleVT == other.v.simpleVT)
        {
            if (v.simpleVT == INVALID_SIMPLE_VALUE_TYPE)
                return llvmTy == other.llvmTy;
            return true;
        }
        return false;
    }

    /**
     * Return true if this is an overloaded type for TableGen.
     * @return
     */
    public boolean isOverloaded()
    {
        switch (v.simpleVT)
        {
            case iAny:
            case fAny:
            case vAny:
            case iPTRAny:
                return true;
            default:
                return false;
        }
    }

    public static EVT getFloatingPointVT(int bitWidth)
    {
        return new EVT(MVT.getFloatingPointVT(bitWidth));
    }

    public static EVT getIntegerVT(int bitWidth)
    {
        MVT m = MVT.getIntegerVT(bitWidth);
        if (m.simpleVT == INVALID_SIMPLE_VALUE_TYPE)
            return getExtendedIntegerVT(bitWidth);
        else
            return new EVT(m);
    }

    public static EVT getVectorVT(EVT vt, int numElts)
    {
        MVT m = MVT.getVectorVT(vt.v, numElts);
        if (m.simpleVT == INVALID_SIMPLE_VALUE_TYPE)
            return getExtendedVectorVT(vt, numElts);
        else
            return new EVT(m);
    }

    public static EVT getIntVectorWithNumElements(int numElts)
    {
        MVT m = MVT.getIntVectorWithNumElements(numElts);
        if (m.simpleVT == INVALID_SIMPLE_VALUE_TYPE)
            return getVectorVT(new EVT(i8), numElts);
        else
            return new EVT(m);
    }

    public boolean isSimple()
    {
        return v.simpleVT <= LastSimpleValueType;
    }

    public boolean isExtended()
    {
        return !isSimple();
    }

    public boolean isFloatingPoint()
    {
        return isSimple() ?
                (v.simpleVT >= f32 && v.simpleVT <= ppcf128) ||
                (v.simpleVT >= v2f32 && v.simpleVT <= v4f64) :
                isExtendedFloatingPoint();
    }

    public boolean isInteger()
    {
        return isSimple() ?
                (v.simpleVT >= FIRST_INTEGER_VALUETYPE &&
                 v.simpleVT <= LAST_INTEGER_VALUETYPE) ||
                (v.simpleVT >= v2i8 && v.simpleVT <= v4i64)
                : isExtendedInteger();
    }

    public boolean isVector()
    {
        return isSimple() ? (v.simpleVT >= FIRST_VECTOR_VALUETYPE
                && v.simpleVT <= LAST_VECTOR_VALUETYPE) : isExtendedVector();
    }

    public boolean is64BitVector()
    {
        return isSimple() ? (v.simpleVT == v8i8 || v.simpleVT
                == v4i16 || v.simpleVT == v2i32
                    || v.simpleVT == v1i64 || v.simpleVT == v2f32) : isExtended64BitVector();
    }

    public boolean is128BitVector()
    {
        return isSimple()? (v.simpleVT == v16i8 || v.simpleVT
                == v8i16 || v.simpleVT == v4i32
        || v.simpleVT == v2i64 || v.simpleVT == v4f32 || v.simpleVT == v2f64)
                : isExtended128BitVector();
    }

    public boolean is256BitVector()
    {
        return isSimple()? (v.simpleVT == v4i64 || v.simpleVT == v8i32
                || v.simpleVT == v16i16 || v.simpleVT == v32i8
                || v.simpleVT == v8f32 || v.simpleVT == v4f64)
                : isExtended256BitVector();
    }

    public boolean isByteSize()
    {
        return getSizeInBits() == 8;
    }

    public int getSizeInBits()
    {
        return isSimple() ? v.getSizeInBits() : getExtendedSizeInBits();
    }

    public int getStoreSizeInBits()
    {
        return (getSizeInBits() + 7) / 8 * 8;
    }

    public EVT getRoundIntegerType()
    {
        assert isInteger() && !isVector() :"Invalid integer type!";
        int bitwidth = getSizeInBits();
        if (bitwidth <= 8)
            return new EVT(i8);
        else
            return getIntegerVT(1 << Util.log2(bitwidth));
    }

    public boolean isPow2VectorType()
    {
        int numElts = getVectorNumElements();
        return (numElts & (numElts - 1)) == 0;
    }

    public int getVectorNumElements()
    {
        assert isVector():"Invalid vector type!";
        if (isSimple())
            return v.getVectorNumElements();
        else
            return getExtendedVectorNumElements();
    }

    public EVT getPow2VectorType()
    {
        if (!isPow2VectorType())
        {
            int numElts = getExtendedVectorNumElements();
            int pow2NumElts = 1 << Util.log2(numElts);
            return EVT.getVectorVT(getExtendedVectorElementType(), pow2NumElts);
        }
        else
            return this;
    }

    /**
     * This function returns value type as a string, e.g. "i32".
     * @return
     */
    public String getEVTString()
    {
        switch (v.simpleVT)
        {
            default:
                if (isVector())
                {
                    return new StringBuilder().append("v")
                            .append(getExtendedVectorNumElements())
                            .append(getExtendedVectorElementType().toString())
                            .toString();
                }
                if (isInteger())
                    return "i" + getSizeInBits();
                Util.shouldNotReachHere("Invalid EVT!");
                return "?";
            case i1: return "i1";
            case i8:      return "i8";
            case i16:     return "i16";
            case i32:     return "i32";
            case i64:     return "i64";
            case i128:    return "i128";
            case f32:     return "f32";
            case f64:     return "f64";
            case f80:     return "f80";
            case f128:    return "f128";
            case ppcf128: return "ppcf128";
            case isVoid:  return "isVoid";
            case Other:   return "ch";
            case Flag:    return "flag";
            case v2i8:    return "v2i8";
            case v4i8:    return "v4i8";
            case v8i8:    return "v8i8";
            case v16i8:   return "v16i8";
            case v32i8:   return "v32i8";
            case v2i16:   return "v2i16";
            case v4i16:   return "v4i16";
            case v8i16:   return "v8i16";
            case v16i16:  return "v16i16";
            case v2i32:   return "v2i32";
            case v4i32:   return "v4i32";
            case v8i32:   return "v8i32";
            case v1i64:   return "v1i64";
            case v2i64:   return "v2i64";
            case v4i64:   return "v4i64";
            case v2f32:   return "v2f32";
            case v4f32:   return "v4f32";
            case v8f32:   return "v8f32";
            case v2f64:   return "v2f64";
            case v4f64:   return "v4f64";
        }
    }

    /**
     * This method returns an LLVM type corresponding to the
     * specified EVT.  For integer types, this returns an int type.  Note
     * that this will abort for types that cannot be represented.
     * @return
     */
    public Type getTypeForEVT()
    {
        switch (v.simpleVT)
        {
            default:
                assert isExtended() : "Type is not extended!";
                assert false:"Vector type currently not supported";
                return llvmTy;
            case isVoid:
                return Type.VoidTy;
            case i1:
                return Type.Int1Ty;
            case i8:
                return Type.Int8Ty;
            case i16:
                return Type.Int16Ty;
            case i32:
                return Type.Int32Ty;
            case i64:
                return Type.Int64Ty;
            case i128:
                return IntegerType.get(128);
            case f32:
                return Type.FloatTy;
            case f64:
                return Type.DoubleTy;
            case f80:
                return Type.X86_FP80Ty;
            case f128:
                return Type.FP128Ty;
            case ppcf128:
                return null;
            //case v2i8:

                /*VectorType.get(Type.getInt8Ty(), 2);
            case v4i8:
                return VectorType.get(Type.getInt8Ty(), 4);
            case v8i8:
                return VectorType.get(Type.getInt8Ty(), 8);
            case v16i8:
                return VectorType.get(Type.getInt8Ty(), 16);
            case v32i8:
                return VectorType.get(Type.getInt8Ty(), 32);
            case v2i16:
                return VectorType.get(Type.getInt16Ty(), 2);
            case v4i16:
                return VectorType.get(Type.getInt16Ty(), 4);
            case v8i16:
                return VectorType.get(Type.getInt16Ty(), 8);
            case v16i16:
                return VectorType.get(Type.getInt16Ty(), 16);
            case v2i32:
                return VectorType.get(Type.getInt32Ty(), 2);
            case v4i32:
                return VectorType.get(Type.getInt32Ty(), 4);
            case v8i32:
                return VectorType.get(Type.getInt32Ty(), 8);
            case v1i64:
                return VectorType.get(Type.getInt64Ty(), 1);
            case v2i64:
                return VectorType.get(Type.getInt64Ty(), 2);
            case v4i64:
                return VectorType.get(Type.getInt64Ty(), 4);
            case v2f32:
                return VectorType.get(Type.getFloatTy(), 2);
            case v4f32:
                return VectorType.get(Type.getFloatTy(), 4);
            case v8f32:
                return VectorType.get(Type.getFloatTy(), 8);
            case v2f64:
                return VectorType.get(Type.getDoubleTy(), 2);
            case v4f64:
                return VectorType.get(Type.getDoubleTy(), 4);
                */
        }
    }

    public static  EVT getEVT(Type ty)
    {
        return getEVT(ty,false);
    }

    /**
     * Return the value type corresponding to the specified type.
     * This returns all pointers as iPTR.  If HandleUnknown is true, unknown
     * types are returned as Other, otherwise they are invalid.
     * @param ty
     * @param handleUnkown
     * @return
     */
    public static  EVT getEVT(Type ty, boolean handleUnkown)
    {
        switch (ty.getTypeID())
        {
            default:
                if (handleUnkown) return new EVT(Other);
                Util.shouldNotReachHere("Undefined type");
                return new EVT(isVoid);
            case VoidTyID:
                return new EVT(isVoid);
            case IntegerTyID:
                return getIntegerVT(((IntegerType)(ty)).getBitWidth());
            case FloatTyID: return new EVT(new MVT(f32));
            case DoubleTyID: return new EVT(new MVT(f32));
            case X86_FP80TyID: return new EVT(new MVT(f80));
            case FP128TyID: return new EVT(new MVT(f128));
            case PointerTyID: return new EVT(new MVT(iPTR));
        }
    }

    public Object getRawBits()
    {
        return v.simpleVT <= LastSimpleValueType?v.simpleVT : llvmTy;
    }


    @Override
    public String toString()
    {
        return getEVTString();
    }

    private static EVT getExtendedIntegerVT(int bitWidth)
    {
        EVT vt = new EVT();
        vt.llvmTy = IntegerType.get(bitWidth);
        assert vt.isExtended() :"Type is not extended!";
        return vt;
    }

    private static EVT getExtendedVectorVT(EVT vt, int numElements)
    {
        assert false:"Should not reaching here!";
        return null;
    }

    private boolean isExtendedFloatingPoint()
    {
        assert isExtended():"Type is not extended";
        // FIXME: 17-7-1 Vector type is not supported currently.
        return false;
        //return llvmTy.isFPOrFPVector();
    }

    private boolean isExtendedInteger()
    {
        assert isExtended():"Type is not extended";
        // FIXME: 17-7-1 Vector type is not supported currently.
        return false;
        //return llvmTy.isIntOrIntVector();
    }

    private boolean isExtendedVector()
    {
        assert isExtended():"Type is not extended";
        // FIXME: 17-7-1 Vector type is not supported currently.
        return false;
        //return llvmTy instanceof VectorType;
    }

    private boolean isExtended64BitVector()
    {
        return isExtendedVector() && getSizeInBits() == 64;
    }

    private boolean isExtended128BitVector()
    {
        return isExtendedVector() && getSizeInBits() == 128;
    }

    private boolean isExtended256BitVector()
    {
        return isExtendedVector() && getSizeInBits() == 256;
    }

    private EVT getExtendedVectorElementType()
    {
        assert isExtended():"Type is not extended!";
        // FIXME: 17-7-1 Vector type is not supported currently.
        return null;
        //return EVT.getEVT(((VectorType)llvmTy).getElementType());
    }

    private int getExtendedVectorNumElements()
    {
        assert isExtended():"Type is not extended!";
        // FIXME: 17-7-1 Vector type is not supported currently.
        return -1;
        //return ((VectorType)llvmTy).getNumElements();
    }

    private int getExtendedSizeInBits()
    {
        assert isExtended():"Type is not extended!";
        if (llvmTy instanceof IntegerType)
        {
            return ((IntegerType)llvmTy).getBitWidth();
        }
        /*
        if (llvmTy instanceof VectorType)
        {
            return ((VectorType)llvmTy).getBitWidth();
        }
        */
        // FIXME: 17-7-1 Vector type is not supported currently.
        assert false:"Unrecognized extended type!";
        return 0;
    }

    public EVT getVectorElementType()
    {
        assert isVector():"Invalid vector type!";
        if (isSimple())
            return new EVT(v.getVectorElementType());
        else
            return getExtendedVectorElementType();
    }

    public MVT getSimpleVT()
    {
        assert isSimple():"Expected a int!";
        return v;
    }

    /// isRound - Return true if the size is a power-of-two number of bytes.
    public boolean isRound() 
    {
        int BitSize = getSizeInBits();
        return BitSize >= 8 && (BitSize & (BitSize - 1)) == 0;
    }

    /// bitsEq - Return true if this has the same number of bits as VT.
    public boolean bitsEq(EVT VT)
    {
        return getSizeInBits() == VT.getSizeInBits();
    }

    /**
     * Return true if this has more bits than srcVT.
     * @param srcVT
     * @return
     */
    public boolean bitsGT(EVT srcVT)
    {
        return getSizeInBits() > srcVT.getSizeInBits();
    }

    /// bitsGE - Return true if this has no less bits than VT.
    public boolean bitsGE(EVT VT)
    {
        return getSizeInBits() >= VT.getSizeInBits();
    }

    /// bitsLT - Return true if this has less bits than VT.
    public boolean bitsLT(EVT VT)
    {
        return getSizeInBits() < VT.getSizeInBits();
    }

    /// bitsLE - Return true if this has no more bits than VT.
    public boolean bitsLE(EVT VT)
    {
        return getSizeInBits() <= VT.getSizeInBits();
    }
}
