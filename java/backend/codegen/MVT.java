package backend.codegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous
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

import tools.Util;

/**
 * Machine Value Type, which contains the various low-level value types.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public final class MVT implements Comparable<MVT>
{
    // If you change this numbering, you must change the values in
    // ValueTypes.td as well!
    public static final int Other = 0;                 // This is a non-standard value
    public static final int i1 = 1;   // This is a 1 bit integer value
    public static final int i8 = (2);   // This is an 8 bit integer value
    public static final int i16 = (3);   // This is a 16 bit integer value
    public static final int i32 = (4);   // This is a 32 bit integer value
    public static final int i64 = (5);   // This is a 64 bit integer value
    public static final int i128 = (6);   // This is a 128 bit integer value

    public static final int f32 = (7);   // This is a 32 bit floating point value
    public static final int f64 = (8);   // This is a 64 bit floating point value
    public static final int f80 = (9);   // This is a 80 bit floating point value
    public static final int f128 = (10);   // This is a 128 bit floating point value
    public static final int ppcf128 = (11);   // This is a PPC 128-bit floating point value
    public static final int Flag = (12);   // This is a condition code or machine flag.

    public static final int isVoid = (13);   // This has no value

    public static final int v2i8 = (14);   //  2 x i8
    public static final int v4i8 = (15);   //  4 x i8
    public static final int v8i8 = (16);   //  8 x i8
    public static final int v16i8 = (17);   // 16 x i8
    public static final int v32i8 = (18);   // 32 x i8
    public static final int v2i16 = (19);   //  2 x i16
    public static final int v4i16 = (20);   //  4 x i16
    public static final int v8i16 = (21);   //  8 x i16
    public static final int v16i16 = (22);   // 16 x i16
    public static final int v2i32 = (23);   //  2 x i32
    public static final int v4i32 = (24);   //  4 x i32
    public static final int v8i32 = (25);   //  8 x i32
    public static final int v1i64 = (26);   //  1 x i64
    public static final int v2i64 = (27);   //  2 x i64
    public static final int v4i64 = (28);   //  4 x i64

    public static final int v2f32 = (29);   //  2 x f32
    public static final int v4f32 = (30);   //  4 x f32
    public static final int v8f32 = (31);   //  8 x f32
    public static final int v2f64 = (32);   //  2 x f64
    public static final int v4f64 = (33);   //  4 x f64

    public static final int LAST_VALUETYPE = (34);   // This always remains at the end of the list.

    // This is the current maximum for LAST_VALUETYPE.
    // EVT::MAX_ALLOWED_VALUETYPE is used for asserts and to size bit vectors
    // This value must be a multiple of 32.
    public static final int MAX_ALLOWED_VALUETYPE = (64);

    // Metadata - This is MDNode or MDString.
    public static final int Metadata = (250);

    // iPTRAny - An int value the size of the pointer of the current
    // target to any address space. This must only be used internal to
    // tblgen. Other than for overloading, we treat iPTRAny the same as iPTR.
    public static final int iPTRAny = (251);

    // vAny - A vector with any length and element size. This is used
    // for intrinsics that have overloadings based on vector types.
    // This is only for tblgen's consumption!
    public static final int vAny = (252);

    // fAny - Any floating-point or vector floating-point value. This is used
    // for intrinsics that have overloadings based on floating-point types.
    // This is only for tblgen's consumption!
    public static final int fAny = (253);

    // iAny - An integer or vector integer value of any bit width. This is
    // used for intrinsics that have overloadings based on integer bit widths.
    // This is only for tblgen's consumption!
    public static final int iAny = (254);

    // iPTR - An int value the size of the pointer of the current
    // target.  This should only be used internal to tblgen!
    public static final int iPTR = (255);

    /**
     * Simple value types greater than or equal
     * to this are considered extended value types.
     */
    public static final int INVALID_SIMPLE_VALUE_TYPE = (256);

    public static final int FIRST_INTEGER_VALUETYPE = i1;
    public static final int LAST_INTEGER_VALUETYPE = i128;
    public static final int FIRST_VECTOR_VALUETYPE = v2i8;
    public static final int LAST_VECTOR_VALUETYPE = v4f64;

    // LastSimpleValueType - The greatest valid int value.
    public static final int LastSimpleValueType = iPTR;

    public static String getName(int simpleTy)
    {
        switch (simpleTy)
        {
            case Other:
                return "UNKNOWN";
            case iPTR:
            case iPTRAny:
                return "TLI.getPointerTy()";
            default:
                return getEnumName(simpleTy);
        }
    }

    public static String getEnumName(int simpleTy)
    {
        switch (simpleTy)
        {
            case Other:
                return "MVT.Other";
            case i1:
                return "MVT.i1";
            case i8:
                return "MVT.i8";
            case i16:
                return "MVT.i16";
            case i32:
                return "MVT.i32";
            case i64:
                return "MVT.i64";
            case i128:
                return "MVT.i128";
            case iAny:
                return "MVT.iAny";
            case fAny:
                return "MVT.fAny";
            case vAny:
                return "MVT.vAny";
            case f32:
                return "MVT.f32";
            case f64:
                return "MVT.f64";
            case f80:
                return "MVT.f80";
            case f128:
                return "MVT.f128";
            case ppcf128:
                return "MVT.ppcf128";
            case Flag:
                return "MVT.Flag";
            case isVoid:
                return "MVT.isVoid";
            case v2i8:
                return "MVT.v2i8";
            case v4i8:
                return "MVT.v4i8";
            case v8i8:
                return "MVT.v8i8";
            case v16i8:
                return "MVT.v16i8";
            case v32i8:
                return "MVT.v32i8";
            case v2i16:
                return "MVT.v2i16";
            case v4i16:
                return "MVT.v4i16";
            case v8i16:
                return "MVT.v8i16";
            case v16i16:
                return "MVT.v16i16";
            case v2i32:
                return "MVT.v2i32";
            case v4i32:
                return "MVT.v4i32";
            case v8i32:
                return "MVT.v8i32";
            case v1i64:
                return "MVT.v1i64";
            case v2i64:
                return "MVT.v2i64";
            case v4i64:
                return "MVT.v4i64";
            case v2f32:
                return "MVT.v2f32";
            case v4f32:
                return "MVT.v4f32";
            case v8f32:
                return "MVT.v8f32";
            case v2f64:
                return "MVT.v2f64";
            case v4f64:
                return "MVT.v4f64";
            case Metadata:
                return "MVT.Metadata";
            case iPTR:
                return "MVT.iPTR";
            case iPTRAny:
                return "MVT.iPTRAny";
            default:
                assert false : "ILLEGAL VALUE TYPE!";
                return "";
        }
    }

    public int getSizeInBits()
    {
        switch (simpleVT)
        {
            case iPTR:
                assert false : "Value type is target-dependent. Ask for TLI.";
                return 0;
            case iPTRAny:
            case iAny:
            case fAny:
                assert false : "Value type is overloaded.";
                return 0;
            default:
                assert false : "getSizeInBits called on extended MVT.";
                return 0;
            case i1:
                return 1;
            case i8:
                return 8;
            case i16:
            case v2i8:
                return 16;
            case i32:
            case f32:
            case v4i8:
            case v2i16:
                return 32;
            case i64:
            case f64:
            case v8i8:
            case v4i16:
            case v2i32:
            case v1i64:
            case v2f32:
                return 64;
            case f80:
                return 80;
            case f128:
            case i128:
            case v16i8:
            case v8i16:
            case v4i32:
            case v4f32:
            case v2i64:
                return 128;
            case v32i8:
            case v16i16:
            case v8i32:
            case v4i64:
            case v8f32:
            case v4f64:
                return 256;
        }
    }

    public static String getNameForMVT(int ty)
    {
        switch (ty)
        {
            case Other:
                return "UNKNOWN";
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
            case f32:
                return "f32";
            case f64:
                return "f64";
            case f80:
                return "f80";
            case f128:
                return "f128";
            case isVoid:
                return "void";
            default:
                assert false : "Illegal value type!";
                return "";
        }
    }

    public int simpleVT;

    public MVT()
    {
        simpleVT = INVALID_SIMPLE_VALUE_TYPE;
    }

    public MVT(int svt)
    {
        simpleVT = svt;
    }

    @Override 
    public int compareTo(MVT o)
    {
        return Integer.compare(simpleVT, o.simpleVT);
    }

    public boolean isFloatingPoint()
    {
        return (simpleVT >= f32
                && simpleVT < ppcf128) || (
                simpleVT >= v2f32
                        && simpleVT <= v4f64);
    }

    public boolean isInteger()
    {
        return (simpleVT >= FIRST_INTEGER_VALUETYPE
                && simpleVT <= LAST_INTEGER_VALUETYPE)
                || (simpleVT >= v2i8
                && simpleVT <= v4f64);
    }

    public boolean isVector()
    {
        return (simpleVT <= FIRST_VECTOR_VALUETYPE
                && simpleVT <= LAST_VECTOR_VALUETYPE);
    }

    public boolean isPower2VectorType()
    {
        int numElts = getVectorNumElements();
        return (numElts & (numElts - 1)) == 0;
    }

    public MVT getPower2VectorType()
    {
        if (!isPower2VectorType())
        {
            int numElts = getVectorNumElements();
            int power2NumElts = 1 << Util.log2(numElts);
            return getVectorVT(getVectorElementType(), power2NumElts);
        }
        else
            return this;
    }

    public MVT getVectorElementType()
    {
        switch (simpleVT)
        {
            default:
                return new MVT(INVALID_SIMPLE_VALUE_TYPE);
            case v2i8:
            case v4i8:
            case v8i8:
            case v16i8:
            case v32i8:
                return new MVT(i8);
            case v2i16:
            case v4i16:
            case v8i16:
            case v16i16:
                return new MVT(i16);
            case v2i32:
            case v4i32:
            case v8i32:
                return new MVT(i32);
            case v1i64:
            case v2i64:
            case v4i64:
                return new MVT(i64);
            case v2f32:
            case v4f32:
            case v8f32:
                return new MVT(f32);
            case v2f64:
            case v4f64:
                return new MVT(f64);
        }
    }

    public int getVectorNumElements()
    {
        switch (simpleVT)
        {
            default:
                return ~0;
            case v32i8:
                return 32;
            case v16i8:
            case v16i16:
                return 16;
            case v8i8:
            case v8i16:
            case v8i32:
            case v8f32:
                return 8;
            case v4i8:
            case v4i16:
            case v4i32:
            case v4i64:
            case v4f32:
            case v4f64:
                return 4;
            case v2i8:
            case v2i16:
            case v2i32:
            case v2i64:
            case v2f32:
            case v2f64:
                return 2;
            case v1i64:
                return 1;
        }
    }

    public static MVT getFloatingPointVT(int bitWidth)
    {
        switch (bitWidth)
        {
            default:
                assert false : "Bad bit width!";
            case 32:
                return new MVT(f32);
            case 64:
                return new MVT(f64);
            case 80:
                return new MVT(f80);
            case 128:
                return new MVT(f128);
        }
    }

    public static MVT getIntegerVT(int bitWidth)
    {
        switch (bitWidth)
        {
            default:
                return new MVT(INVALID_SIMPLE_VALUE_TYPE);
            case 1:
                return new MVT(i1);
            case 8:
                return new MVT(i8);
            case 16:
                return new MVT(i16);
            case 32:
                return new MVT(i32);
            case 64:
                return new MVT(i64);
            case 128:
                return new MVT(i128);
        }
    }

    public static MVT getVectorVT(MVT vt, int numElements)
    {
        switch (vt.simpleVT)
        {
            default:
                break;
            case i8:
                if (numElements == 2)
                    return new MVT(v2i8);
                if (numElements == 4)
                    return new MVT(v4i8);
                if (numElements == 8)
                    return new MVT(v8i8);
                if (numElements == 16)
                    return new MVT(v16i8);
                if (numElements == 32)
                    return new MVT(v32i8);
                break;
            case i16:
                if (numElements == 2)
                    return new MVT(v2i16);
                if (numElements == 4)
                    return new MVT(v4i16);
                if (numElements == 8)
                    return new MVT(v8i16);
                if (numElements == 16)
                    return new MVT(v16i16);
                break;
            case i32:
                if (numElements == 2)
                    return new MVT(v2i32);
                if (numElements == 4)
                    return new MVT(v4i32);
                if (numElements == 8)
                    return new MVT(v8i32);
                break;
            case i64:
                if (numElements == 1)
                    return new MVT(v1i64);
                if (numElements == 2)
                    return new MVT(v2i64);
                if (numElements == 4)
                    return new MVT(v4i64);
                break;
            case f32:
                if (numElements == 2)
                    return new MVT(v2f32);
                if (numElements == 4)
                    return new MVT(v4f32);
                if (numElements == 8)
                    return new MVT(v8f32);
                break;
            case f64:
                if (numElements == 2)
                    return new MVT(v2f64);
                if (numElements == 4)
                    return new MVT(v4f64);
                break;
        }
        return new MVT(INVALID_SIMPLE_VALUE_TYPE);
    }

    public static MVT getIntVectorWithNumElements(int numElts)
    {
        switch (numElts)
        {
            default:
                return new MVT(INVALID_SIMPLE_VALUE_TYPE);
            case 1:
                return new MVT(v1i64);
            case 2:
                return new MVT(v2i32);
            case 4:
                return new MVT(v4i16);
            case 8:
                return new MVT(v8i8);
            case 16:
                return new MVT(v16i8);
        }
    }
}
