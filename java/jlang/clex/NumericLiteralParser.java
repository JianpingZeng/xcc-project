package jlang.clex;
/*
 * Extremely C language Compiler.
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

import jlang.support.SourceLocation;
import backend.support.APFloat;
import backend.support.APInt;
import backend.support.FltSemantics;
import tools.OutParamWrapper;

import static backend.support.APFloat.FltCategory.fcZero;
import static backend.support.APFloat.OpStatus.opOK;
import static backend.support.APFloat.RoundingMode.rmNearestTiesToEven;
import static jlang.clex.LiteralSupport.hexDigitValue;
import static jlang.diag.DiagnosticLexKindsTag.*;

/**
 * This performs strict semantic analysis of the content of a pp number,
 * classifying it as either integer, floating, or erroneous, determines the
 * radix of the value and can convert it to a useful value.
 * @author Xlous.zeng
 * @version 0.1
 */
public class NumericLiteralParser
{
    private Preprocessor pp;
    private char[] tokenStr;
    private int curPos;
    private int radix;
    private boolean sawExponent, sawPeriod;
    private int digitBegin, suffixBegin;

    public boolean hadError,
            isUnsigned,
            isLong,
            isLongLong,
            isFloat,    // "1.0f"
            isImaginary;    // "1.0f"


    public boolean isIntegerLiteral()
    {
        return !isFloatingLiteral();
    }

    public boolean isFloatingLiteral()
    {
        return sawPeriod || sawExponent;
    }

    public boolean hasSuffix()
    {
        return suffixBegin != tokenStr.length;
    }

    public int getRadix()
    {
        return radix;
    }

    /**
     * Convert this numeric literal value to an APInt that
     * matches Val's input width.  If there is an overflow, set Val to the low bits
     * of the result and return true.  Otherwise, return false.
     * @param val
     * @return
     */
    public boolean getIntegerValue(APInt val)
    {
        int maxBitsPerDigit = 1;
        while ((1<<maxBitsPerDigit) < radix)
        {
            maxBitsPerDigit += 1;
        }

        if (suffixBegin * maxBitsPerDigit <= 64)
        {
            long n = 0;
            for (curPos = digitBegin; curPos < suffixBegin; ++curPos)
            {
                n = n * radix + hexDigitValue(tokenStr[curPos]);
            }

            val.assign(n);

            return val.getZExtValue() != n;
        }

        val.assign(0);
        curPos = digitBegin;

        APInt radixVal = new APInt(val.getBitWidth(), radix);
        APInt charVal = new APInt(val.getBitWidth(), 0);
        APInt oldVal = new APInt(val);

        boolean overflowOccurred = false;
        while (curPos < suffixBegin)
        {
            int ch = hexDigitValue(tokenStr[curPos++]);
            assert ch < radix :"NumericLiteralParser constructor should have reject this";

            charVal.assign(ch);

            // Multiply by radix, did overflow occur on the multiply?
            oldVal.assign(val);
            val.mulAssign(radixVal);
            overflowOccurred |= !val.udiv(radixVal).eq(oldVal);

            // Add value, did overflow occur on the value?
            //   (a + b) ult b  <=> overflow
            val.addAssign(charVal);
            overflowOccurred |= val.ult(charVal);
        }
        return overflowOccurred;
    }

    public APFloat getFloatValue(FltSemantics format, OutParamWrapper<Boolean> isExact)
    {
        StringBuilder buf = new StringBuilder();
        int n = Math.min(suffixBegin, tokenStr.length);
        for (int i = 0; i < n; i++)
            buf.append(tokenStr[i]);

        buf.append('\0');

        APFloat v = new APFloat(format, fcZero, false);
        int status = v.convertFromString(buf.toString(), rmNearestTiesToEven);
        if (isExact != null)
            isExact.set(status == opOK);
        return v;
    }

    public NumericLiteralParser(String literal, SourceLocation loc, Preprocessor pp)
    {
       this(literal.toCharArray(), loc, pp);
    }

    public NumericLiteralParser(char[] literal, SourceLocation loc, Preprocessor pp)
    {
        tokenStr = new char[literal.length];
        System.arraycopy(literal, 0, tokenStr, 0, literal.length);
        this.pp = pp;

        if (tokenStr[curPos] == '0')
        {
            // parse radix.
            parseNumberStartingWithZero(loc);
            if (hadError)
                return;
        }
        else
        {
            // the first digit is non-zero.
            radix = 10;
            curPos = LiteralSupport.skipDigit(tokenStr, curPos);
            if (curPos == tokenStr.length);
                // Done
            else if (LiteralSupport.isHexDigit(tokenStr[curPos]) && !(tokenStr[curPos] =='e'
                    || tokenStr[curPos] == 'E'))
            {
                pp.diag(pp.advanceToTokenCharacter(loc, curPos), err_invalid_decimal_digit)
                        .addTaggedVal(String.valueOf(tokenStr[curPos])).emit();
                hadError = true;
                return;
            }
            else if (tokenStr[curPos] == '.')
            {
                // this is a fraction.
                curPos++;
                sawPeriod = true;
                curPos = LiteralSupport.skipDigit(tokenStr, curPos);
            }
            if (tokenStr[curPos] == 'e' || tokenStr[curPos] == 'E')
            {
                // Exponent.
                int exponentPos = curPos;
                curPos++;
                // '+' or '-' prefix.
                if (tokenStr[curPos] == '+' || tokenStr[curPos] == '-')
                    ++curPos;
                int firstNoDigit = LiteralSupport.skipDigit(tokenStr, curPos);
                if (firstNoDigit != curPos)
                    curPos = firstNoDigit;
                else
                {
                    pp.diag(pp.advanceToTokenCharacter(loc, exponentPos),
                            err_exponent_has_no_digits).emit();
                    hadError = true;
                    return;
                }
            }
        }

        suffixBegin = curPos;

        // Parse the suffix.  At this point we can classify whether we have an FP or
        // integer constant.
        boolean isFPConstant = isFloatingLiteral();

        for (; curPos < tokenStr.length; ++curPos)
        {
            switch (tokenStr[curPos])
            {
                case 'f':
                case 'F':
                    // FP suffix for 'float'.
                    if (!isFPConstant) break; // Error
                    if (isFloat || isLong) break;
                    isFloat = true;
                    continue;  // success.
                case 'u':
                case 'U':
                    if (isFPConstant) break;
                    if (isUnsigned) break;  // can not be repeated.
                    isUnsigned = true;
                    continue;
                case 'l':
                case 'L':
                    if (isLong || isLongLong) break;
                    if (isFloat) break;

                    if (curPos + 1 != tokenStr.length && tokenStr[curPos+1] == tokenStr[curPos])
                    {
                        // this is LL or ll suffixed.
                        if (isFPConstant) break;
                        isLongLong = true;
                        ++curPos;
                    }
                    else
                    {
                        isLong = true;
                    }
                    continue;   // success.
                case 'i':
                    // Imaginary.
                case 'I':
                case 'j':
                case 'J':
                {
                    if (isImaginary) break; // can not repeated.
                    pp.diag(pp.advanceToTokenCharacter(loc, curPos),
                            ext_imaginary_constant).emit();
                    isImaginary = true;
                    continue;   // Success
                }
            }
            // If we reached here, there was an error.
            break;
        }

        //report an error if there are any.
        if (curPos != tokenStr.length)
        {
            pp.diag(pp.advanceToTokenCharacter(loc, curPos),
                    isFPConstant ? err_invalid_suffix_float_constant
                    :err_invalid_suffix_integer_constant)
                    .addTaggedVal(String.valueOf(tokenStr, suffixBegin, tokenStr.length - suffixBegin))
                    .emit();
            hadError = true;
        }
    }

    /**
     * This method is called when the first character
     * of the number is found to be a zero.  This means it is either an octal
     * number (like '04') or a hex number ('0x123a') a binary number ('0b1010') or
     * a floating point number (01239.123e4).  Eat the prefix, determining the
     * radix etc.
     * @param loc
     */
    private void parseNumberStartingWithZero(SourceLocation loc)
    {
        assert tokenStr[curPos] == '0' :"Invalid method called";
        curPos++;

        // Handle the hex number like 0x123
        if ((tokenStr[curPos] == 'x' || tokenStr[curPos] == 'X')
                && (LiteralSupport.isHexDigit(tokenStr[curPos+1]) || tokenStr[curPos] == '.'))
        {
            ++curPos;
            radix = 16;
            digitBegin = curPos;
            curPos = skipHexDigit(curPos);
            if (curPos == tokenStr.length)
            {
                // Done.
            }
            else if (tokenStr[curPos] == '.')
            {
                // hex fraction.
                curPos++;
                sawPeriod = true;
                curPos = skipHexDigit(curPos);
            }

            // if binary exponent can appear with or with a '.'.
            if (tokenStr[curPos] == 'p' || tokenStr[curPos] == 'P')
            {
                int exponent = curPos;
                ++curPos;
                sawExponent = true;
                if (tokenStr[curPos] == '+'|| tokenStr[curPos]== '-')
                    ++curPos;
                int firstNonDigit = skipHexDigit(curPos);
                if (firstNonDigit == curPos)
                {
                    pp.diag(pp.advanceToTokenCharacter(loc, exponent),
                            err_exponent_has_no_digits).emit();
                    hadError = true;
                    return;
                }

                curPos = firstNonDigit;
                if (!pp.getLangOptions().hexFloats)
                {
                    pp.diag(loc, ext_hexconstant_invalid).emit();
                }
            }
            else if (sawPeriod)
            {
                pp.diag(pp.advanceToTokenCharacter(loc, curPos),
                        err_hexconstant_requires_exponent).emit();
                hadError = true;
            }
            return;
        }

        // Handle simple binary numbers 0b01010
        if (tokenStr[curPos] == 'B' || tokenStr[curPos] == 'b')
        {
            // 0x101010 is GCC extension.
            pp.diag(loc, ext_binary_literal).emit();
            ++curPos;
            radix = 2;
            digitBegin = curPos;
            curPos = skipBinaryDigit(curPos);
            if (curPos == tokenStr.length);
                // Done
            else if (LiteralSupport.isHexDigit(tokenStr[curPos]))
            {
                pp.diag(pp.advanceToTokenCharacter(loc, curPos), err_invalid_binary_digit)
                        .addTaggedVal(String.valueOf(tokenStr[curPos]))
                        .emit();
                hadError = true;
            }

            // Other suffixes will be diagnosed by the caller.
            return;
        }

        // For now, the radix is set to 8.
        radix = 8;
        digitBegin = curPos;
        curPos = skipOctalDigit(curPos);
        if (curPos == tokenStr.length)
            return;

        // If we have some other non-octal digit that *is* a decimal digit, see if
        // this is part of a floating point number like 094.123 or 09e1.
        if (Character.isDigit(tokenStr[curPos]))
        {
            int endecimal = LiteralSupport.skipDigit(tokenStr, curPos);
            if (tokenStr[endecimal] == '.' || tokenStr[endecimal] == 'e'
                    || tokenStr[endecimal] == 'E')
            {
                curPos = endecimal;
                radix = 10;
            }
        }

        // If we have a hex digit other than 'e' (which denotes a FP exponent) then
        // the code is using an incorrect base.
        if (LiteralSupport.isHexDigit(tokenStr[curPos]) && tokenStr[curPos] != 'e'
                && tokenStr[curPos] != 'E')
        {
            pp.diag(pp.advanceToTokenCharacter(loc, curPos),err_invalid_octal_digit).
                    addTaggedVal(String.valueOf(tokenStr[curPos])).
                    emit();
            hadError = true;
            return;
        }

        if (tokenStr[curPos] == '.')
        {
            ++curPos;
            radix = 10;
            sawPeriod = true;
            curPos = LiteralSupport.skipDigit(tokenStr, curPos);
        }

        if (tokenStr[curPos] == 'e' || tokenStr[curPos] == 'E')
        {
            int exponent = curPos;
            ++curPos;
            radix = 10;
            sawExponent = true;
            if (tokenStr[curPos] == '+' || tokenStr[curPos] == '-')
                ++curPos;
            int firstNoDigit = LiteralSupport.skipDigit(tokenStr, curPos);
            if (firstNoDigit != curPos)
                curPos = firstNoDigit;
            else
            {
                pp.diag(pp.advanceToTokenCharacter(loc, exponent), err_exponent_has_no_digits)
                        .emit();
                hadError = true;
            }
        }
    }

    private int skipOctalDigit(int curPos)
    {
        while (curPos < tokenStr.length && LiteralSupport.isOctalDigit(tokenStr[curPos]))
            ++curPos;
        return curPos;
    }

    private int skipBinaryDigit(int curPos)
    {
        while (curPos < tokenStr.length && LiteralSupport.isBinaryDigit(tokenStr[curPos]))
            ++curPos;
        return curPos;
    }

    private int skipHexDigit(int curPos)
    {
        while (curPos < tokenStr.length && LiteralSupport.isHexDigit(tokenStr[curPos]))
            ++curPos;
        return curPos;
    }
}
