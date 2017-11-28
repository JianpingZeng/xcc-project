package backend.support;
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

import backend.type.PointerType;
import backend.type.Type;
import tools.Util;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class Attribute
{
    /// Function parameters and results can have attributes to indicate how they
    /// should be treated by optimizations and code generation. This enumeration
    /// lists the attributes that can be associated with parameters, function
    /// results or the function itself.
    /// @brief Function attributes.

    public static final int None = 0;     ///< No attributes have been set
    public static final int ZExt = 1 << 0;  ///< Zero extended before/after call
    public static final int SExt = 1 << 1;  ///< Sign extended before/after call
    public static final int NoReturn =
            1 << 2;  ///< Mark the function as not returning
    public static final int InReg =
            1 << 3;  ///< Force argument to be passed in register
    public static final int StructRet =
            1 << 4;  ///< Hidden pointer to structure to return
    public static final int NoUnwind =
            1 << 5;  ///< Function doesn't unwind stack
    public static final int NoAlias =
            1 << 6;  ///< Considered to not alias after call
    public static final int ByVal = 1 << 7;  ///< Pass structure by value
    public static final int Nest = 1 << 8;  ///< Nested function static chain
    public static final int ReadNone =
            1 << 9;  ///< Function does not access memory
    public static final int ReadOnly =
            1 << 10; ///< Function only reads from memory
    public static final int NoInline = 1 << 11; ///< inline=never
    public static final int AlwaysInline = 1 << 12; ///< inline=always
    public static final int OptimizeForSize = 1 << 13; ///< opt_size
    public static final int StackProtect = 1 << 14; ///< Stack protection.
    public static final int StackProtectReq =
            1 << 15; ///< Stack protection required.
    public static final int Alignment =
            31 << 16; ///< Alignment of parameter (5 bits)
    // stored as log2 of alignment with +1 bias
    // 0 means unaligned different from align 1
    public static final int NoCapture =
            1 << 21; ///< Function creates no aliases of pointer
    public static final int NoRedZone = 1 << 22; /// disable redzone
    public static final int NoImplicitFloat =
            1 << 23; /// disable implicit floating point
    /// instructions.
    public static final int Naked = 1 << 24; ///< Naked function

    /// @brief Attributes that only apply to function parameters.
    public static final int ParameterOnly =
            ByVal | Nest | StructRet | NoCapture;

    /// @brief Attributes that may be applied to the function itself.  These cannot
    /// be used on return values or function parameters.
    public static final int FunctionOnly =
            NoReturn | NoUnwind | ReadNone | ReadOnly | NoInline | AlwaysInline
                    | OptimizeForSize | StackProtect | StackProtectReq
                    | NoRedZone | NoImplicitFloat | Naked;

    /// @brief Parameter attributes that do not apply to vararg call arguments.
    public static final int VarArgsIncompatible = StructRet;

    /// @brief Attributes that are mutually incompatible.
    public static final int[] MutuallyIncompatible = {
            ByVal | InReg | Nest | StructRet, ZExt | SExt, ReadNone | ReadOnly,
            NoInline | AlwaysInline };

    /// This turns an int alignment (a power of 2, normally) into the
    /// form used internally in Attributes.

    public static int constructAlignmentFromInt(int i)
    {
        // Default alignment, allow the target to define how to align it.
        if (i == 0)
            return 0;

        assert Util.isPowerOf2(i) : "Alignment must be a power of two.";
        assert i <= 0x40000000 : "Alignment too large.";
        return (Util.log2(i) + 1) << 16;
    }

    /// This returns the alignment field of an attribute as a byte alignment value.
    public static int getAlignmentFromAttrs(int a)
    {
        int align = a & Alignment;
        if (align == 0)
            return 0;

        return 1 << ((align >> 16) - 1);
    }

    /// The set of Attributes set in Attributes is converted to a
    /// string of equivalent mnemonics. This is, presumably, for writing out
    /// the mnemonics for the assembly writer.
    /// @brief Convert attribute bits to text
    //===----------------------------------------------------------------------===//
    // Attribute Function Definitions
    //===----------------------------------------------------------------------===//

    public String getAsString(int Attrs)
    {
        StringBuffer result = new StringBuffer();
        if ((Attrs & ZExt) != 0)
            result.append("zeroext ");
        if ((Attrs & SExt) != 0)
            result.append("signext ");
        if ((Attrs & NoReturn) != 0)
            result.append("noreturn ");
        if ((Attrs & NoUnwind) != 0)
            result.append("nounwind ");
        if ((Attrs & InReg) != 0)
            result.append("inreg ");
        if ((Attrs & NoAlias) != 0)
            result.append("noalias ");
        if ((Attrs & NoCapture) != 0)
            result.append("nocapture ");
        if ((Attrs & StructRet) != 0)
            result.append("sret ");
        if ((Attrs & ByVal) != 0)
            result.append("byval ");
        if ((Attrs & Nest) != 0)
            result.append("nest ");
        if ((Attrs & ReadNone) != 0)
            result.append("readnone ");
        if ((Attrs & ReadOnly) != 0)
            result.append("readonly ");
        if ((Attrs & OptimizeForSize) != 0)
            result.append("optsize ");
        if ((Attrs & NoInline) != 0)
            result.append("noinline ");
        if ((Attrs & AlwaysInline) != 0)
            result.append("alwaysinline ");
        if ((Attrs & StackProtect) != 0)
            result.append("ssp ");
        if ((Attrs & StackProtectReq) != 0)
            result.append("sspreq ");
        if ((Attrs & NoRedZone) != 0)
            result.append("noredzone ");
        if ((Attrs & NoImplicitFloat) != 0)
            result.append("noimplicitfloat ");
        if ((Attrs & Naked) != 0)
            result.append("naked ");
        if ((Attrs & Alignment) != 0)
        {
            result.append("align ");
            result.append(getAlignmentFromAttrs(Attrs));
            result.append(" ");
        }
        // Trim the trailing space.
        assert result.length() != 0 : "Unknown attribute!";
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

    public static int typeIncompatible(Type ty)
    {
        int incompatible = None;

        if (!ty.isInteger())
            // Attributes that only apply to integers.
            incompatible |= SExt | ZExt;

        if (!(ty instanceof PointerType))
            // Attributes that only apply to pointers.
            incompatible |= ByVal | Nest | NoAlias | StructRet | NoCapture;

        return incompatible;
    }
}
