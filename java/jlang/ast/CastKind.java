package jlang.ast;
/*
 * Extremely C language CompilerInstance
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

/**
 * The kind of operator required for type conversion.
 * @author Xlous.zeng
 * @version 0.1
 */
public enum CastKind
{
    CK_Invalid,
    /// CK_Dependent - A conversion which cannot yet be analyzed because
    /// either the expression or TargetData type is dependent.  These are
    /// created only for explicit casts; dependent ASTs aren't required
    /// to even approximately type-check.
    ///   (T*) malloc(sizeof(T))
    CK_Dependent,

    /// CK_BitCast - A conversion which causes a bit pattern of one type
    /// to be reinterpreted as a bit pattern of another type.  Generally
    /// the operands must have equivalent getTypeSize and unrelated types.
    ///
    /// The pointer conversion char* -> int* is a bitcast.  A conversion
    /// from any pointer type to a C pointer type is a bitcast unless
    /// it's actually BaseToDerived or DerivedToBase.
    /// specialized casts below.
    ///
    /// Vector coercions are bitcasts.
    CK_BitCast,

    /// CK_LValueToRValue - A conversion which causes the extraction of
    /// an r-value from the operand gl-value.  The result of an r-value
    /// conversion is always unqualified.
    CK_LValueToRValue,

    /// CK_NoOp - A conversion which does not affect the type other than
    /// (possibly) adding qualifiers.
    ///   int    -> int
    ///   char** -> const char * const *
    CK_NoOp,

    /// CK_ToUnion - The GCC cast-to-union extension.
    ///   int   -> union { int x; float y; }
    ///   float -> union { int x; float y; }
    CK_ToUnion,

    /// CK_ArrayToPointerDecay - Array to pointer decay.
    ///   int[10] -> int*
    ///   char[5][6] -> char(*)[6]
    CK_ArrayToPointerDecay,

    /// CK_FunctionToPointerDecay - FunctionProto to pointer decay.
    ///   void(int) -> void(*)(int)
    CK_FunctionToPointerDecay,

    /// CK_NullToPointer - Null pointer constant to pointer.
    ///   (void*) 0
    CK_NullToPointer,

    /// CK_IntegralToPointer - Integral to pointer.  A special kind of
    /// reinterpreting conversion.  Applies to normal pointers.
    ///    (char*) 0x1001aab0
    CK_IntegralToPointer,

    /// CK_PointerToIntegral - Pointer to integral.  A special kind of
    /// reinterpreting conversion.  Applies to normal pointers.
    ///    (intptr_t) "help!"
    CK_PointerToIntegral,

    /// CK_PointerToBoolean - Pointer to boolean conversion.  A check
    /// against null.  Applies to normal pointers.
    CK_PointerToBoolean,

    /// CK_ToVoid - Cast to void, discarding the computed value.
    ///    (void) malloc(2048)
    CK_ToVoid,

    /// CK_IntegralCast - A cast between integral types (other than to
    /// boolean).  Variously a bitcast, a truncation, a sign-extension,
    /// or a zero-extension.
    ///    long l = 5;
    ///    (unsigned) i
    CK_IntegralCast,

    /// CK_IntegralToBoolean - Integral to boolean.  A check against zero.
    ///    (bool) i
    CK_IntegralToBoolean,

    /// CK_IntegralToFloating - Integral to floating point.
    ///    float f = i;
    CK_IntegralToFloating,

    /// CK_FloatingToIntegral - Floating point to integral.  Rounds
    /// towards zero, discarding any fractional component.
    ///    (int) f
    CK_FloatingToIntegral,

    /// CK_FloatingToBoolean - Floating point to boolean.
    ///    (bool) f
    CK_FloatingToBoolean,

    /// CK_FloatingCast - Casting between floating types of different getTypeSize.
    ///    (double) f
    ///    (float) ld
    CK_FloatingCast,

    /// \brief A conversion of a floating point real to a floating point
    /// complex of the original type.  Injects the value as the real
    /// component with a zero imaginary component.
    ///   float -> _Complex float
    CK_FloatingRealToComplex,

    /// \brief Converts a floating point complex to floating point real
    /// of the source's element type.  Just discards the imaginary
    /// component.
    ///   _Complex long double -> long double
    CK_FloatingComplexToReal,

    /// \brief Converts a floating point complex to bool by comparing
    /// against 0+0i.
    CK_FloatingComplexToBoolean,

    /// \brief Converts between different floating point complex types.
    ///   _Complex float -> _Complex double
    CK_FloatingComplexCast,

    /// \brief Converts from a floating complex to an integral complex.
    ///   _Complex float -> _Complex int
    CK_FloatingComplexToIntegralComplex,

    /// \brief Converts from an integral real to an integral complex
    /// whose element type matches the source.  Injects the value as
    /// the real component with a zero imaginary component.
    ///   long -> _Complex long
    CK_IntegralRealToComplex,

    /// \brief Converts an integral complex to an integral real of the
    /// source's element type by discarding the imaginary component.
    ///   _Complex short -> short
    CK_IntegralComplexToReal,

    /// \brief Converts an integral complex to bool by comparing against
    /// 0+0i.
    CK_IntegralComplexToBoolean,

    /// \brief Converts between different integral complex types.
    ///   _Complex char -> _Complex long long
    ///   _Complex unsigned int -> _Complex signed int
    CK_IntegralComplexCast,

    /// \brief Converts from an integral complex to a floating complex.
    ///   _Complex unsigned -> _Complex float
    CK_IntegralComplexToFloatingComplex,
}
