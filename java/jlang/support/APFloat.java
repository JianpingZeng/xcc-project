package jlang.support;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous zeng
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

import tools.OutParamWrapper;
import tools.Util;

import java.util.Arrays;

import static jlang.support.APFloat.CmpResult.*;
import static jlang.support.APFloat.FltCategory.*;
import static jlang.support.APFloat.OpStatus.*;
import static jlang.support.APFloat.RoundingMode.*;

/**
 * This class provides an arbitrary precision floating point values and some
 * arithmetic operations on them.
 * <p>
 * A self-contained host-and target-independent arbitrary-precision
 * floating-point software implementation.  It uses bignum integer
 * arithmetic as provided by static functions in the APInt class.
 * The library will work with bignum integers whose parts are any
 *  type at least 16 bits wide, but 64 bits is recommended.
 * </p>
 * <p>
 * Written for clarity rather than speed, in particular with a view
 * to use in the front-end of a cross compiler so that target
 * arithmetic can be correctly performed on the host.  Performance
 * should nonetheless be reasonable, particularly for its intended
 * use.  It may be useful as a base implementation for a run-time
 * library during development of a faster target-specific one.
 * </p>
 * <p>
 * All 5 rounding modes in the IEEE-754R draft are handled correctly
 * for all implemented operations.  Currently implemented operations
 * are add, subtract, multiply, divide, fused-multiply-add,
 * conversion-to-float, conversion-to-integer and
 * conversion-from-integer.  New rounding modes (e.g. away from zero)
 * can be added with three or four lines of code.
 * </p>
 * Four formats are built-in: IEEE single precision, double
 * precision, quadruple precision, and x87 80-bit extended double
 * (when operating with full extended precision).  Adding a new
 * format that obeys IEEE semantics only requires adding two lines of
 * code: a declaration and definition of the format.
 * </p>
 * <p>
 * All operations return the status of that operation as an exception
 * bit-mask, so multiple operations can be done consecutively with
 * their results or-ed together.  The returned status can be useful
 * for compiler diagnostics; e.g., inexact, underflow and overflow
 * can be easily diagnosed on constant folding, and compiler
 * optimizers can determine what exceptions would be raised by
 * folding operations and optimize, or perhaps not optimize,
 * accordingly.
 * </p>
 * <p>
 * At present, underflow tininess is detected after rounding; it
 * should be straight forward to add support for the before-rounding
 * case too.
 * </p>
 * <p>
 * The library reads hexadecimal floating point numbers as per C99,
 * and correctly rounds if necessary according to the specified
 * rounding mode.  Syntax is required to have been validated by the
 * caller.  It also converts floating point numbers to hexadecimal
 * text as per the C99 %a and %A conversions.  The output precision
 * (or alternatively the natural minimal precision) can be specified;
 * if the requested precision is less than the natural precision the
 * output is correctly rounded for the specified rounding mode.
 * </p>
 * <p>
 * It also reads decimal floating point numbers and correctly rounds
 * according to the specified rounding mode.
 * </p>
 * <p>
 * Conversion to decimal text is not currently implemented.
 * </p>
 * <p>
 * Non-zero finite numbers are represented internally as a sign bit,
 * a 16-bit signed exponent, and the significand as an array of
 * integer parts.  After normalization of a number of precision P the
 * exponent is within the range of the format, and if the number is
 * not denormal the P-th bit of the significand is set as an explicit
 * integer bit.  For denormals the most significant bit is shifted
 * right so that the exponent is maintained at the format's minimum,
 * so that the smallest denormal has just the least significant bit
 * of the significand set.  The sign of zeroes and infinities is
 * significant; the exponent and significand of such numbers is not
 * stored, but has a known implicit (deterministic) value: 0 for the
 * significands, 0 for zero exponent, all 1 bits for infinity
 * exponent.  For NaNs the sign and significand are deterministic,
 * although not really meaningful, and preserved in non-conversion
 * operations.  The exponent is implicitly all 1 bits.
 * </p>
 * <p>
 * TODO <br></br>
 * ====
 *
 * Some features that may or may not be worth adding:
 *
 * Binary to decimal conversion (hard).

 * Optional ability to detect underflow tininess before rounding.
 *
 * New formats: x87 in single and double precision mode (IEEE apart
 * from extended exponent range) (hard).
 *
 * New operations: sqrt, IEEE remainder, C90 fmod, nextafter,
 * nexttoward.
 * </p>
 * @author Xlous.zeng
 * @version 0.1
 */
public class APFloat implements Cloneable
{
    public static final FltSemantics IEEEsingle =
            new FltSemantics((short) 127, (short) -126, 24, true);
    public static final FltSemantics IEEEdouble =
            new FltSemantics((short) 1023, (short) -1022, 53, true);
    public static final FltSemantics IEEEquad =
            new FltSemantics((short) 16383, (short) -16382, 113, true);
    public static final FltSemantics x87DoubleExtended =
            new FltSemantics((short) 16383, (short) -16382, 64, true);

    /**
     * And this pseudo, used to construct APFloats that cannot
     * conflict with anything real.
     */
    public static final FltSemantics Bogus = new FltSemantics((short) 0, (short) 0, 0, true);

    public static int semanticsPrecision(FltSemantics flt)
    {
        return flt.precision;
    }

    /**
     * Floating point numbers have a four-state comparison relation.
     */
    public enum CmpResult
    {
        cmpLessThan, cmpEqual, cmpGreaterThan, cmpUnordered
    }

    /**
     * IEEE-754R gives five rounding modes.
     */
    public enum RoundingMode
    {
        rmNearestTiesToEven,
        rmTowardPositive,
        rmTowardNegative,
        rmTowardZero,
        rmNearestTiesToAway
    }

    /**
     * Operation status.  opUnderflow or opOverflow are always returned
     * or-ed with opInexact.
     */
    public interface OpStatus
    {
        int opOK = 0x00, opInvalidOp = 0x01, opDivByZero = 0x02,
        opOverflow = 0x04, opUnderflow = 0x08, opInexact = 0x10;
    }

    /**
     * Category of internally-represented number.
     */
    public enum FltCategory
    {
        fcInfinity, fcNaN, fcNormal, fcZero
    }

    private static void assertArithmeticOK(FltSemantics sem)
    {
        assert sem.arithmeticOK :
                "Compile-time arithmetic does not support these semantics";
    }

    public APFloat(FltSemantics flt, String str)
    {
        assertArithmeticOK(flt);
        initialize(flt);
        convertFromString(str, rmNearestTiesToEven);
    }

    public APFloat(FltSemantics flt, long integralPart)
    {
        assertArithmeticOK(flt);
        initialize(flt);
        sign = false;
        zeroSignificand();
        exponent = flt.precision - 1;
        significandParts()[0] = integralPart;
        normalize(rmNearestTiesToEven, LostFraction.lfExactlyZero);
    }

    public APFloat(FltSemantics flt, FltCategory category, boolean negative)
    {
        this(flt, category, negative, 0);
    }

    public APFloat(FltSemantics flt, FltCategory category, boolean negative, int type)
    {
        assertArithmeticOK(flt);
        initialize(flt);
        this.category = category;
        sign = negative;
        if (category == fcNormal)
            this.category = fcZero;
        else if (category == fcNaN)
            makeNaN(type);
    }

    public APFloat(double d)
    {
        APInt api = new APInt(64, 0);
        initFromAPInt(api.doubleToBits(d));
    }

    public APFloat(float f)
    {
        APInt api = new APInt(32, 0);
        initFromAPInt(api.floatToBits(f));
    }

    public APFloat(APInt num)
    {
        this(num, false);
    }

    public APFloat(APInt api, boolean isIEEE)
    {
        initFromAPInt(api, isIEEE);
    }

    public APFloat(APFloat another)
    {
        initialize(another.semantics);
        assign(another);
    }

    public static APFloat getZero(FltSemantics sem)
    {
        return getZero(sem, false);
    }

    public static APFloat getZero(FltSemantics sem, boolean isNegative)
    {
        return new APFloat(sem, FltCategory.fcZero, isNegative);
    }

    public static APFloat getInf(FltSemantics sem)
    {
        return getInf(sem, false);
    }

    public static APFloat getInf(FltSemantics sem, boolean isNegative)
    {
        return new APFloat(sem, FltCategory.fcInfinity, isNegative);
    }

    public static APFloat getNan(FltSemantics sem)
    {
        return getNan(sem, false, 0);
    }

    public static APFloat getNan(FltSemantics sem, boolean isNegative, int type)
    {
        return new APFloat(sem, FltCategory.fcNaN, isNegative, type);
    }

    //=====================================================================//
    // Arithmetic operation methods.
    //=====================================================================//
    public int add(APFloat rhs, RoundingMode rm)
    {
        return addOrSubtract(rhs, rm, false);
    }

    public int subtract(APFloat rhs, RoundingMode rm)
    {
        return addOrSubtract(rhs, rm, true);
    }

    public int multiply(APFloat rhs, RoundingMode rm)
    {
        int status;
        assertArithmeticOK(semantics);

        status = multiplySpecials(rhs);
        if (category == fcNormal)
        {
            LostFraction LostFraction = multiplySignificand(rhs, null);
            status = normalize(rm, LostFraction);
            if (LostFraction != jlang.support.LostFraction.lfExactlyZero)
                status = status | opInexact;
        }
        return status;
    }

    public int divide(APFloat divident, RoundingMode rm)
    {
        int fs;
        assertArithmeticOK(semantics);
        sign ^= divident.sign;
        fs = divideSpecials(divident);

        if (category == fcNormal)
        {
            LostFraction LostFraction = divideSignificand(divident);
            fs = normalize(rm, LostFraction);
            if (LostFraction != jlang.support.LostFraction.lfExactlyZero)
                fs |= opInexact;
        }
        return fs;
    }

    @Override
    public APFloat clone()
    {
        try
        {
            return (APFloat) super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    // IEEE remainder.
    public int remainder(APFloat rhs)
    {
        int fs;
        APFloat v = clone();
        boolean originSign = sign;

        assertArithmeticOK(semantics);
        fs = v.divide(rhs, rmNearestTiesToEven);
        if (fs == opDivByZero)
            return fs;

        int parts = partCount();
        long[] x = new long[parts];

        OutParamWrapper<Boolean> ignored = new OutParamWrapper<>(false);
        fs = v.convertToInteger(x, parts * 64, true, rmNearestTiesToEven, ignored);
        if (fs == opInvalidOp)
            return fs;

        fs = v.convertFromZeroExtendedInteger(x, parts * 64, true, rmNearestTiesToEven);

        assert fs == opOK;

        fs = v.multiply(rhs, rmNearestTiesToEven);
        assert fs == opOK || fs == opInexact;

        fs = subtract(v, rmNearestTiesToEven);
        assert fs == opOK || fs == opInexact;

        if (isZero())
            sign = originSign;
        return fs;
    }

    /**
     * C fmod or llvm frem.
     */
    public int mod(APFloat rhs, RoundingMode rm)
    {
        int fs;
        assertArithmeticOK(semantics);
        fs = modSpecials(rhs);

        if (category == fcNormal && rhs.category == fcNormal)
        {
            APFloat v = clone();
            boolean originSign = sign;

            fs = v.divide(rhs, rmNearestTiesToEven);
            if (fs == opDivByZero)
                return fs;

            int parts = partCount();
            long[] x = new long[parts];
            OutParamWrapper<Boolean> ignored = new OutParamWrapper<>(false);
            fs = v.convertToInteger(x, parts * 64, true, rmTowardZero, ignored);

            if (fs == opInvalidOp)
                return fs;

            fs = v.convertFromZeroExtendedInteger(x, parts * 64, true, rmNearestTiesToEven);
            assert fs == opOK;

            fs = v.multiply(rhs, rm);
            assert fs == opOK || fs == opInexact;

            fs = subtract(v, rm);
            assert fs == opOK || fs == opInexact;

            if (isZero())
                sign = originSign;
        }
        return fs;
    }

    public int fusedMultiplyAdd(APFloat rhs, APFloat rhs2, RoundingMode rm)
    {
        int fs;

        assertArithmeticOK(semantics);

        sign ^= rhs.sign;

        if (category == fcNormal && rhs.category == fcNormal
                && rhs2.category == fcNormal)
        {
            LostFraction LostFraction = multiplySignificand(rhs, rhs2);

            fs = normalize(rm, LostFraction);
            if (LostFraction != jlang.support.LostFraction.lfExactlyZero)
                fs |= opInexact;

            if (category == fcZero && sign != rhs2.sign)
                sign = rm == rmTowardNegative;
        }
        else
        {
            fs = multiplySpecials(rhs);
            if (fs == opOK)
                fs = addOrSubtract(rhs2, rm, false);
        }
        return fs;
    }

    /**
     * Sign operations.
     */
    public void changeSign()
    {
        sign = !sign;
    }

    public void clearSign()
    {
        sign = false;
    }

    public void copySign(APFloat other)
    {
        sign = other.sign;
    }

    private static LostFraction lostFractionThroughTruncation(long[] parts, int partCount, int bits)
    {
        int lsb = 0;
        lsb = APInt.tcLSB(parts, partCount);

        if (bits <= lsb)
            return LostFraction.lfExactlyZero;
        if (bits == lsb + 1)
            return LostFraction.lfExactlyHalf;
        if (bits <= partCount * 64 && APInt.tcExtractBit(parts, bits - 1))
            return LostFraction.lfMoreThanHalf;

        return LostFraction.lfLessThanHalf;
    }

    /***Conversion*/
    public int convert(FltSemantics toSem, RoundingMode rm, OutParamWrapper<Boolean> loseInfo)
    {
        LostFraction LostFraction;
        int newPartCount, oldPartCount;
        int fs;

        assertArithmeticOK(toSem);
        assertArithmeticOK(semantics);
        LostFraction = jlang.support.LostFraction.lfExactlyZero;
        newPartCount = partCountForBits(toSem.precision + 1);
        oldPartCount = partCount();

        if (newPartCount > oldPartCount)
        {
            long[] newParts = new long[newPartCount];
            APInt.tcSet(newParts, 0, newPartCount);
            if (category == fcNormal || category == fcNaN)
                APInt.tcAssign(significandParts(), newParts, oldPartCount);
            parts = newParts;
        }
        else if (newPartCount < oldPartCount)
        {
            if (category == fcNormal)
            {
                LostFraction = lostFractionThroughTruncation(significandParts(), oldPartCount, toSem.precision);
            }
            if (newPartCount == 1)
            {
                long newPart = 0;
                if (category == fcNormal || category == fcNaN)
                    newPart = significandParts()[0];
                part = newPart;
            }
        }

        if (category == fcNormal)
        {
            exponent += toSem.precision - semantics.precision;
            semantics = toSem;
            fs = normalize(rm, LostFraction);
            loseInfo.set(fs != opOK);
        }
        else if (category == fcNaN)
        {
            int shift = toSem.precision - semantics.precision;
            FltSemantics oldSemantics = semantics;
            loseInfo.set(false);

            if (shift > 0)
            {
                int ushift = - shift;

                if (APInt.tcLSB(significandParts(), newPartCount) < ushift)
                    loseInfo.set(true);
                if (oldSemantics == x87DoubleExtended
                        && ((significandParts()[0] & 0x8000000000000000L) == 0
                        || (significandParts()[0] & 0x4000000000000000L) == 0))
                    loseInfo.set(true);
                APInt.tcShiftRight(significandParts(), newPartCount, ushift);
            }

            // gcc forces the Quiet bit on, which means (float)(double)(float_sNan)
            // does not give you back the same bits.  This is dubious, and we
            // don't currently do it.  You're really supposed to get
            // an invalid operation signal at runtime, but nobody does that.
            fs = opOK;
        }
        else
        {
            semantics = toSem;
            fs = opOK;
            loseInfo.set(false);
        }
        return fs;
    }

    public int convertToInteger(long[] parts, int width, boolean isSigned,
            RoundingMode rm, OutParamWrapper<Boolean> isExact)
    {
        int fs = convertToSignExtendedInteger(parts, width, isSigned, rm, isExact);

        if (fs == opInvalidOp)
        {
            int bits, dstPartsCount;
            dstPartsCount = partCountForBits(width);

            if (category == fcNaN)
                bits = 0;
            else if (sign)
                bits = isSigned ?1:0;
            else
                bits = width - (isSigned?1:0);

            APInt.tcSetLeastSignificantBits(parts, dstPartsCount, bits);
            if (sign && isSigned)
                APInt.tcShiftLeft(parts,dstPartsCount, width - 1);
        }
        return fs;
    }

    public int convertFromAPInt(APInt val, boolean isSigned, RoundingMode rm)
    {
        int partCount = val.getNumWords();
        APInt api = val.clone();

        sign = false;
        if (isSigned && api.isNegative())
        {
            sign = true;
            api.negative();
        }
        return convertFromUnsignedParts(api.getRawData(), partCount, rm);
    }

    public int convertFromSignExtendedInteger(long[] src, int srcCount,
            boolean isSinged, RoundingMode rm)
    {
        int status;

        assertArithmeticOK(semantics);
        if (isSinged && APInt.tcExtractBit(src, srcCount * 64 -1))
        {
            long[] copy;

            sign = true;
            copy = new long[srcCount];
            APInt.tcAssign(copy, src, srcCount);
            APInt.tcNegate(copy, srcCount);
            status = convertFromUnsignedParts(copy, srcCount, rm);
        }
        else
        {
            sign = false;
            status = convertFromUnsignedParts(src, srcCount, rm);
        }
        return status;
    }

    public int convertFromZeroExtendedInteger(long[] parts, int width,
            boolean isSinged, RoundingMode rm)
    {
        int partCount = partCountForBits(width);
        APInt api = new APInt(width, partCount, parts);

        sign =false;
        if (isSinged  && APInt.tcExtractBit(parts, width - 1))
        {
            sign =true;
            api.negative();
        }
        return convertFromUnsignedParts(api.getRawData(), partCount, rm);
    }

    public int convertFromString(String str, RoundingMode rm)
    {
        assertArithmeticOK(semantics);
        assert !str.isEmpty():"Invalid string length!";

        int len = str.length();
        sign = str.charAt(0) == '-';
        int i = 0;
        if (str.charAt(0) == '-' || str.charAt(0) == '+')
        {
            ++i;
            --len;
            assert len != 0:"String has no digits";
        }

        if (len >= 2 && str.charAt(i) == '0' && (str.charAt(i+1) == 'x' || str.charAt(i+1) == 'X'))
        {
            assert (len - 2) != 0 :"Invalid string";
            return convertFromHexadecimalString(str.substring(i+2), rm);
        }

        return convertFromDecimalString(str.substring(i), rm);
    }

    public APInt bitcastToAPInt()
    {
        if (semantics == IEEEsingle)
            return convertFloatAPFloatToAPInt();
        if (semantics == IEEEdouble)
            return convertDoubleAPFloatToAPInt();
        if (semantics == IEEEquad)
            return convertQuadrupleAPFloatToAPInt();

        assert semantics == x87DoubleExtended :"Unknown format!";
        return convertF80LongDoubleAPFloatToAPInt();
    }

    public double convertToDouble()
    {
        assert semantics == IEEEdouble:"Invalid calling to this method!";
        APInt api = bitcastToAPInt();
        return api.bitsToDouble();
    }

    public float convertToFloat()
    {
        assert semantics == IEEEsingle:"Invalid calling to this method!";
        APInt api = bitcastToAPInt();
        return api.bitsToFloat();
    }
    /* The definition of equality is not straightforward for floating point,
    so we won't use operator==.  Use one of the following, or write
    whatever it is you really mean. */
    // boolean equals(APFloat rhs){}     // DO NOT IMPLEMENT

    /* IEEE comparison with another floating point number (NaNs
       compare unordered, 0==-0). */
    public CmpResult compare(APFloat rhs)
    {
        CmpResult result;

        assertArithmeticOK(semantics);
        assert semantics == rhs.semantics;

        switch (category)
        {
            default:
                Util.shouldNotReachHere();
                break;
            case fcNaN:
                switch (rhs.category)
                {
                    case fcZero:
                    case fcNormal:
                    case fcInfinity:
                    case fcNaN:
                        return CmpResult.cmpUnordered;
                }
                break;
            case fcZero:
                switch (rhs.category)
                {
                    case fcNaN:
                        return cmpUnordered;
                    case fcInfinity:
                    case fcNormal:
                        return rhs.sign? cmpGreaterThan : cmpLessThan;
                    case fcZero:
                        return cmpEqual;
                }

            case fcInfinity:
                switch (rhs.category)
                {
                    case fcNormal:
                    case fcZero:
                        if (sign)
                            return cmpLessThan;
                        else
                            return cmpGreaterThan;
                    case fcNaN:
                        return cmpUnordered;
                    case fcInfinity:
                        if (sign == rhs.sign)
                            return cmpEqual;
                        else if (sign)
                            return cmpLessThan;
                        else
                            return cmpGreaterThan;
                }
                break;
            case fcNormal:
                switch (rhs.category)
                {
                    case fcZero:
                        return sign ? cmpLessThan : cmpGreaterThan;
                    case fcNaN:
                        return cmpUnordered;
                    case fcInfinity:
                        return rhs.sign ? cmpGreaterThan : cmpLessThan;
                    case fcNormal:
                        break;
                }
                break;
        }

        if (sign != rhs.sign)
        {
            if (sign)
                result = cmpLessThan;
            else
                result = cmpGreaterThan;
        }
        else
        {
            result = compareAbsoluteValue(rhs);

            if (sign)
            {
                if (result == cmpLessThan)
                    result = cmpGreaterThan;
                else if (result == cmpGreaterThan)
                    result = cmpLessThan;
            }
        }
        return result;
    }

    public boolean bitwiseIsEqual(APFloat rhs)
    {
        if (rhs == this)
            return true;
        if (semantics != rhs.semantics
                || category != rhs.category
                || sign!= rhs.sign)
            return false;

        if (category == fcZero || category == fcInfinity)
            return true;
        else if (category == fcNormal && exponent != rhs.exponent)
            return false;
        else
        {
            int len = partCount();
            long[] p = significandParts();
            long[] q = rhs.significandParts();
            for (int i = 0; i < len; i++)
            {
                if (p[i] != q[i])
                    return false;
            }
            return true;
        }
    }

    /* Zero at the end to avoid modular arithmetic when adding one; used
   when rounding up during hexadecimal output.  */
    static String hexDigitsLower = "0123456789abcdef0";
    static String hexDigitsUpper = "0123456789ABCDEF0";
    static String infinityL = "infinity";
    static String infinityU = "INFINITY";
    static String NaNL = "nan";
    static String NaNU = "NAN";

    /**
     * Write out a hexadecimal representation of the floating point value
     to DST, which must be of sufficient size, in the C99 form
     [-]0xh.hhhhp[+-]d.  Return the number of characters written,
     excluding the terminating NUL.

     If UPPERCASE, the output is in upper case, otherwise in lower case.

     HEXDIGITS digits appear altogether, rounding the value if
     necessary.  If HEXDIGITS is 0, the minimal precision to display the
     number precisely is used instead.  If nothing would appear after
     the decimal point it is suppressed.

     The decimal exponent is always printed and has at least one digit.
     Zero values display an exponent of zero.  Infinities and NaNs
     appear as "infinity" or "nan" respectively.

     The above rules are as specified by C99.  There is ambiguity about
     what the leading hexadecimal digit should be.  This implementation
     uses whatever is necessary so that the exponent is displayed as
     stored.  This implies the exponent will fall within the IEEE format
     range, and the leading hexadecimal digit will be 0 (for denormals),
     1 (normal numbers) or 2 (normal numbers rounded-away-from-zero with
     any other digits zero).
     */
    public int convertToHexString(char[] dest, int hexDigits, boolean upperCase,
            RoundingMode rm)
    {
        assertArithmeticOK(semantics);
        int i = 0;
        if (sign)
            dest[i++] = '-';
        switch (category)
        {
            case fcInfinity:
                System.arraycopy((upperCase? infinityU:infinityL).toCharArray(), 0, dest, 0, infinityU.length());;
                i += infinityL.length();
                break;

            case fcNaN:
                System.arraycopy((upperCase? NaNU:NaNU).toCharArray(), 0, dest, 0, NaNU.length());;
                i += NaNU.length();
                break;
            case fcZero:
                dest[i++] = '0';
                dest[i++] = upperCase?'X':'x';
                dest[i++] = '0';
                if (hexDigits > 1)
                {
                    dest[i++] = '.';
                    Arrays.fill(dest, 0, hexDigits, '0');
                    i += hexDigits;
                }
                dest[i++] = upperCase ? 'P' : 'p';
                dest[i++] = '0';
                break;

            case fcNormal:
                dest = convertNormalToHexString(dest, hexDigits, upperCase, rm);
                break;
        }

        dest[i] = '0';
        return i;
    }

    public FltCategory getCategory()
    {
        return category;
    }

    public FltSemantics getSemantics()
    {
        return semantics;
    }

    public boolean isZero()
    {
        return category == FltCategory.fcZero;
    }

    public boolean isNonZero()
    {
        return !isZero();
    }

    public boolean isNaN()
    {
        return category == FltCategory.fcNaN;
    }

    public boolean isInfinity()
    {
        return category == FltCategory.fcInfinity;
    }

    public boolean isNegative()
    {
        return sign;
    }

    public boolean isPosZero()
    {
        return isZero() && !isNegative();
    }

    public boolean isNegZero()
    {
        return isZero() && isNegative();
    }

    @Override
    public int hashCode()
    {
        if(category == fcZero) return ((sign?1:0) << 8) | semantics.precision;
        else if (category == fcInfinity) return ((sign?1:0) << 9) | semantics.precision;
        else if (category == fcNaN) return (1<< 10) | semantics.precision;
        else
        {
            int hash = ((sign ?1:0) << 11) | semantics.precision | exponent <<12;
            long[] p = significandParts();
            for (int i = 0; i < partCount(); i++)
                hash ^= p[i] ^ (p[i] >> 32);
            return hash;
        }
    }

    private void setSignificand(int index, long value)
    {
        assert category == fcNormal || category == fcNaN;
        assert index < partCount() && index >= 0;
        if (partCount() > 1)
            parts[index] = value;
        else
            part = value;
    }

    private void setSignificand(long value)
    {
        setSignificand(0, value);
    }
    private long[] significandParts()
    {
        assert category == fcNormal || category == fcNaN;
        if (partCount() > 1)
            return parts;
        else
            return new long[]{part};
    }

    private int partCount()
    {
        return partCountForBits(semantics.precision + 1);
    }

    private static int partCountForBits(int bits)
    {
        return (bits + 64 - 1) / 64;
    }

    /* Add the significand of the RHS.  Returns the carry flag.  */
    private long addSignificand(APFloat rhs)
    {
        long[] parts = significandParts();
        assert semantics == rhs.semantics;
        assert exponent == rhs.exponent;

        return APInt.tcAdd(parts, rhs.significandParts(), 0, partCount());
    }

    private long subtractSignificand(APFloat rhs, long borrow)
    {
        long[] parts = significandParts();
        assert semantics == rhs.semantics;
        assert exponent == rhs.exponent;

        return APInt.tcSubtract(parts, rhs.significandParts(), 0, partCount());
    }

    private LostFraction addOrSubtractSignificand(APFloat rhs, boolean subtract)
    {
        long carry;
        LostFraction LostFraction;
        int bits;

        subtract ^= sign ^ rhs.sign;
        bits = exponent - rhs.exponent;

        if (subtract)
        {
            APFloat tempRhs = new APFloat(rhs);
            boolean reverse;

            if (bits == 0)
            {
                reverse = compareAbsoluteValue(tempRhs) == cmpLessThan;
                LostFraction = jlang.support.LostFraction.lfExactlyZero;
            }
            else if (bits > 0)
            {
                LostFraction = tempRhs.shiftSignificandRight(bits - 1);
                shiftSignificandLeft(1);
                reverse = false;
            }
            else
            {
                LostFraction = shiftSignificandRight(-bits - 1);
                tempRhs.shiftSignificandLeft(1);
                reverse = true;
            }

            if (reverse)
            {
                carry = tempRhs.subtractSignificand(this, LostFraction != jlang.support.LostFraction.lfExactlyZero?1:0);
                copySignificand(tempRhs);
                sign = !sign;
            }
            else
            {
                carry = subtractSignificand(tempRhs, LostFraction!= jlang.support.LostFraction.lfExactlyZero?1:0);
            }

            if (LostFraction == jlang.support.LostFraction.lfLessThanHalf)
                LostFraction = jlang.support.LostFraction.lfMoreThanHalf;
            else if (LostFraction == jlang.support.LostFraction.lfMoreThanHalf)
                LostFraction = jlang.support.LostFraction.lfLessThanHalf;

            assert carry == 0;
        }
        else
        {
            if (bits > 0)
            {
                APFloat tempRhs = new APFloat(rhs);

                LostFraction = tempRhs.shiftSignificandRight(bits);
                carry = addSignificand(tempRhs);
            }
            else
            {
                LostFraction = shiftSignificandRight(-bits);
                carry = addSignificand(rhs);
            }

            assert carry == 0;
        }
        return LostFraction;
    }
    /** Multiply the significand of the RHS.  If ADDEND is non-NULL, add it
   on to the full-precision result of the multiplication.  Returns the
   lost fraction.  */
    private LostFraction multiplySignificand(APFloat rhs, APFloat addend)
    {
        int omsb;       // One, not zero, based on MSB.
        int partCounts, newPartCount, precision;
        long[] lhsSignificand;
        long[] scratch = new long[4];
        long[] fullSignificand;
        LostFraction LostFraction;
        OutParamWrapper<Boolean> ignored = new OutParamWrapper<>(false);

        assert semantics == rhs.semantics;

        precision = semantics.precision;
        newPartCount = partCountForBits(precision * 2);

        if (newPartCount > 4)
            fullSignificand = new long[newPartCount];
        else
            fullSignificand = scratch;
        lhsSignificand = significandParts();
        partCounts = partCount();

        APInt.tcFullMultiply(fullSignificand, lhsSignificand,
                rhs.significandParts(), partCounts,
                partCounts);

        LostFraction = jlang.support.LostFraction.lfExactlyZero;
        omsb = APInt.tcMSB(fullSignificand, newPartCount) + 1;
        exponent += rhs.exponent;
        if (addend != null)
        {
            FltSemantics saveSemantics = semantics;
            long savedSignificand = part;
            long[] savedSignificands = parts;
            FltSemantics extendedSemantics;
            int status;
            int extendedPrecision;

            extendedPrecision = precision + precision - 1;
            if (omsb != extendedPrecision)
            {
                APInt.tcShiftLeft(fullSignificand, newPartCount,
                        extendedPrecision - omsb);
                exponent -= extendedPrecision - omsb;
            }

            extendedSemantics = semantics;
            extendedSemantics.precision = extendedPrecision;

            if (newPartCount == 1)
                part = fullSignificand[0];
            else
                parts = fullSignificand;

            semantics = extendedSemantics;

            APFloat extendedAddend =new APFloat(addend);

            status = extendedAddend.convert(extendedSemantics, rmTowardZero, ignored);
            assert status == opOK;
            LostFraction = addOrSubtractSignificand(extendedAddend, false);

            if (newPartCount == 1)
                fullSignificand[0] = part;
            part = savedSignificand;
            parts = savedSignificands;
            semantics = saveSemantics;
            omsb = APInt.tcMSB(fullSignificand, newPartCount) + 1;
        }

        exponent -= precision - 1;
        if (omsb  > precision)
        {
            int bits, significantParts;
            LostFraction lf;

            bits = omsb - precision;
            significantParts  = partCountForBits(omsb);
            lf = shiftRight(fullSignificand, significantParts, bits);
            LostFraction = combineLostFractions(lf, LostFraction);
            exponent += bits;
        }

        APInt.tcAssign(lhsSignificand, fullSignificand, partCounts);

        return LostFraction;
    }

    private static LostFraction shiftRight(long[] dest, int parts, int bits)
    {
        LostFraction ls = lostFractionThroughTruncation(dest, parts, bits);
        APInt.tcShiftRight(dest, parts, bits);
        return ls;
    }

    private static LostFraction combineLostFractions(
            LostFraction moreSignificant, LostFraction lessSignificant)
    {
        if (lessSignificant != LostFraction.lfExactlyZero)
        {
            if (moreSignificant == LostFraction.lfExactlyZero)
                moreSignificant = LostFraction.lfLessThanHalf;
            else if (moreSignificant == LostFraction.lfExactlyHalf)
                moreSignificant = LostFraction.lfMoreThanHalf;
        }
        return moreSignificant;
    }

    private LostFraction divideSignificand(APFloat rhs)
    {
        int bit, i , partsCounts;
        long[] rhsSignicand;
        long[] lhsSignicand, dividend, divisor;
        long[] scratch = new long[4];
        LostFraction LostFraction;

        assert semantics == rhs.semantics;

        lhsSignicand = significandParts();
        rhsSignicand = rhs.significandParts();
        partsCounts = partCount();

        if (partsCounts > 2)
            dividend = new long[partsCounts * 2];
        else
            dividend = scratch;

        divisor = Arrays.copyOfRange(dividend, partsCounts, dividend.length);
        /* Copy the dividend and divisor as they will be modified in-place.  */
        for (i = 0; i < partsCounts; i++)
        {
            dividend[i] = lhsSignicand[i];
            divisor[i] = rhsSignicand[i];
            lhsSignicand[i] = 0;
        }

        exponent -= rhs.exponent;

        int precision = semantics.precision;

        bit = precision - APInt.tcMSB(divisor, partsCounts) - 1;
        if (bit != 0)
        {
            exponent += bit;
            APInt.tcShiftLeft(divisor, partsCounts, bit);
        }

        bit = precision - APInt.tcMSB(dividend, partsCounts) - 1;
        if (bit != 0)
        {
            exponent -= bit;
            APInt.tcShiftLeft(dividend, partsCounts, bit);
        }

        if (APInt.tcCompare(dividend, divisor, partsCounts) < 0)
        {
            --exponent;
            APInt.tcShiftLeft(dividend, partsCounts, 1);
            assert APInt.tcCompare(dividend, divisor, partsCounts) >= 0;
        }

        for (bit = precision; bit != 0; --bit)
        {
            if (APInt.tcCompare(dividend, divisor, partsCounts) >= 0)
            {
                APInt.tcSubtract(dividend, divisor, 0, partsCounts);
                APInt.tcSetBit(lhsSignicand, bit - 1);
            }
            APInt.tcShiftLeft(dividend, partsCounts, 1);
        }

        int cmp = APInt.tcCompare(dividend, divisor, partsCounts);

        if (cmp > 0)
            LostFraction = jlang.support.LostFraction.lfMoreThanHalf;
        else if (cmp == 0)
            LostFraction = jlang.support.LostFraction.lfExactlyHalf;
        else if (APInt.tcIsZero(dividend, partsCounts))
            LostFraction = jlang.support.LostFraction.lfExactlyZero;
        else
            LostFraction = jlang.support.LostFraction.lfLessThanHalf;

        return LostFraction;
    }
    /** Increment an fcNormal floating point number's significand.  */
    private void incrementSignificand()
    {
        long carry = APInt.tcIncrement(significandParts(), partCount());

        /* Our callers should never cause us to overflow.  */
        assert carry == 0;
    }

    /***
     * Constructor called.
     * @param sem
     */
    private void initialize(FltSemantics sem)
    {
        int count;
        semantics = sem;
        count = partCount();
        if (count > 1)
            parts = new long[count];
    }
    /** Write out an  decimal integer.  */
    static char[] writeUnsignedDecimal(char[] dest, int n)
    {
        char[] buff = new char[40];
        int i = 0;
        do
        {
            buff[i++] = (char)('0' + n %  10);
            n /= 10;
        }while (n != 0);

        System.arraycopy(buff, 0, dest, 0, i);
        return dest;
    }
    /**
     * Shift the significand left BITS bits, subtract BITS from its exponent.
     */
    private void shiftSignificandLeft(int bits)
    {
        assert bits < semantics.precision;

        if (bits != 0)
        {
            int partsCount = partCount();
            APInt.tcShiftLeft(significandParts(), partsCount, bits);
            exponent -= bits;

            assert !APInt.tcIsZero(significandParts(), partsCount);
        }
    }
    /** Note that a zero result is NOT normalized to fcZero.  */
    private LostFraction shiftSignificandRight(int bits)
    {
        /* Our exponent should not overflow.  */
        assert exponent + bits >= exponent;
        exponent += bits;

        return shiftRight(significandParts(), partCount(), bits);
    }

    private int significandLSB()
    {
        return APInt.tcLSB(significandParts(), partCount());
    }

    private int significandMSB()
    {
        return APInt.tcMSB(significandParts(), partCount());
    }

    private void zeroSignificand()
    {
        category = fcNormal;
        APInt.tcSet(significandParts(), 0, partCount());
    }

    /* Arithmetic on special values.  */
    private int addOrSubtractSpecials(APFloat rhs, boolean subtract)
    {
        switch (category)
        {
            case fcNaN:
            {
                switch (rhs.category)
                {
                    case fcZero:
                    case fcNormal:
                    case fcInfinity:
                    case fcNaN:
                        return opOK;
                }
                break;
            }
            case fcNormal:
            {
                switch (rhs.category)
                {
                    case fcZero:
                        return opOK;
                    case fcNaN:
                        category = fcNaN;
                        copySignificand(rhs);
                        return opOK;
                    case fcInfinity:
                        category = fcInfinity;
                        sign = rhs.sign ^ subtract;
                        return opOK;
                    case fcNormal:
                        return opDivByZero;
                }
                break;
            }
            case fcInfinity:
            {
                switch (rhs.category)
                {
                    case fcNormal:
                    case fcZero:
                        return opOK;
                    case fcNaN:
                        category = fcNaN;
                        copySignificand(rhs);
                        return opOK;
                    case fcInfinity:
                        if (sign ^ rhs.sign != subtract)
                        {
                            makeNaN();
                            return opInvalidOp;
                        }
                        return opOK;
                }
                break;
            }
            case fcZero:
            {
                switch (rhs.category)
                {
                    case fcNaN:
                        category = fcNaN;
                        copySignificand(rhs);
                        return opOK;
                    case fcInfinity:
                        category = fcInfinity;
                        sign = rhs.sign ^ subtract;
                        return opOK;
                    case fcNormal:
                        assign(rhs);
                        sign = rhs.sign ^ subtract;
                        return opOK;
                    case fcZero:
                        return opOK;
                }
                break;
            }
        }
        Util.shouldNotReachHere();
        return -1;
    }

    private int divideSpecials(APFloat rhs)
    {
        switch (category)
        {
            case fcNaN:
            {
                switch (rhs.category)
                {
                    case fcZero:
                    case fcNormal:
                    case fcInfinity:
                    case fcNaN:
                        return opOK;
                }
                break;
            }
            case fcInfinity:
            {
                switch (rhs.category)
                {
                    case fcZero:
                    case fcNormal:
                        return opOK;
                    case fcInfinity:
                        makeNaN();
                        return opInvalidOp;
                }
                break;
            }
            case fcZero:
                if (rhs.category == fcInfinity || rhs.category == fcNormal)
                    return opOK;
                if (rhs.category == fcNaN)
                {
                    category = fcNaN;
                    copySignificand(rhs);
                    return opOK;
                }
                if (rhs.category == fcZero)
                {
                    makeNaN();
                    return opInvalidOp;
                }
                break;
            case fcNormal:
                switch (rhs.category)
                {
                    case fcNaN:
                        category = fcNaN;
                        copySignificand(rhs);
                        return opOK;
                    case fcInfinity:
                        category = fcZero;
                        return opOK;
                    case fcZero:
                        category = fcInfinity;
                        return opDivByZero;
                    case fcNormal:
                        return opOK;
                }
        }
        Util.shouldNotReachHere();
        return -1;
    }

    private int multiplySpecials(APFloat rhs)
    {
        switch (category)
        {
            case fcNaN:
            {
                switch (rhs.category)
                {
                    case fcZero:
                    case fcNormal:
                    case fcInfinity:
                    case fcNaN:
                        return opOK;
                }
                break;
            }
            case fcZero:
            {
                switch (rhs.category)
                {
                    case fcNaN:
                        category = fcNaN;
                        copySignificand(rhs);
                        return opOK;
                    case fcNormal:
                    case fcZero:
                        category = fcZero;
                        return opOK;
                    case fcInfinity:
                        makeNaN();
                        return opInvalidOp;
                }
                break;
            }
            case fcNormal:
            {
                switch (rhs.category)
                {
                    case fcNaN:
                        category = fcNaN;
                        copySignificand(rhs);
                        return opOK;
                    case fcInfinity:
                        category = fcInfinity;
                        return opOK;
                    case fcZero:
                        category = fcZero;
                        return opOK;
                    case fcNormal:
                        return opOK;
                }
                break;
            }
            case fcInfinity:
            {
                switch (rhs.category)
                {
                    case fcNaN:
                        category = fcNaN;
                        copySignificand(rhs);
                        return opOK;
                    case fcNormal:
                    case fcInfinity:
                        category = fcInfinity;
                        return opOK;
                    case fcZero:
                        makeNaN();
                        return opInvalidOp;
                }
                break;
            }
        }
        Util.shouldNotReachHere();
        return -1;
    }

    private int modSpecials(APFloat rhs)
    {
        switch (category)
        {
            case fcNaN:
            {
                switch (rhs.category)
                {
                    case fcZero:
                    case fcNormal:
                    case fcInfinity:
                    case fcNaN:
                        return opOK;
                }
                break;
            }
            case fcZero:
            {
                switch (rhs.category)
                {
                    case fcInfinity:
                    case fcNormal:
                        return opOK;
                    case fcNaN:
                        category = fcNaN;
                        copySignificand(rhs);
                        return opOK;
                    case fcZero:
                        makeNaN();
                        return opInvalidOp;
                }
                break;
            }
            case fcNormal:
            {
                switch (rhs.category)
                {
                    case fcNaN:
                        category = fcNaN;
                        copySignificand(rhs);
                        return opOK;
                    case fcZero:
                        makeNaN();
                        return opInvalidOp;
                    case fcNormal:
                        return opOK;
                }
                break;
            }
            case fcInfinity:
            {
                switch (rhs.category)
                {
                    case fcNaN:
                        category = fcNaN;
                        copySignificand(rhs);
                        return opOK;
                    case fcZero:
                    case fcNormal:
                    case fcInfinity:
                        makeNaN();
                        return opInvalidOp;
                }
                break;
            }
        }
        Util.shouldNotReachHere();
        return -1;
    }

    /**
     * Miscellany.
     */
    private void makeNaN()
    {
        makeNaN(0);
    }

    /**
     * Make this number a NaN, with an arbitrary but deterministic value
     * for the significand.  If double or longer, this is a signalling NaN,
     * which may not be ideal.  If float, this is QNaN(0).
     */
    private void makeNaN(int type)
    {
        category = fcNaN;
        if (semantics.precision == 24 && semantics.maxExponent == 127)
        {
            type |=  0x7fc00000;
            type &= ~0x80000000;
        }
        else
        {
            type = ~0;
        }
        APInt.tcSet(significandParts(), type, partCount());
    }

    private int normalize(RoundingMode rm, LostFraction LostFraction)
    {
        int omsb;
        int exponentChange;
        if (category != fcNormal)
            return opOK;

        omsb = significandMSB() + 1;
        if (omsb != 0)
        {
            exponentChange = omsb - semantics.precision;
            if (exponent + exponentChange > semantics.maxExponent)
                return handleOverflow(rm);

            if (exponent + exponentChange < semantics.minExponent)
                exponentChange = semantics.minExponent - exponent;

            if (exponentChange < 0)
            {
                assert LostFraction == jlang.support.LostFraction.lfExactlyZero;

                shiftSignificandLeft(-exponentChange);

                return opOK;
            }

            if (exponentChange > 0)
            {
                LostFraction lf = shiftSignificandRight(exponentChange);
                LostFraction = combineLostFractions(lf, LostFraction);

                if (omsb > exponentChange)
                    omsb -= exponentChange;
                else
                    omsb = 0;
            }
        }

        if (LostFraction == jlang.support.LostFraction.lfExactlyZero)
        {
            if (omsb == 0)
                category = fcZero;

            return opOK;
        }

        if (roundAwayFromZero(rm, LostFraction, 0))
        {
            if (omsb == 0)
                exponent = semantics.minExponent;

            incrementSignificand();
            omsb = significandMSB() + 1;

            if (omsb == semantics.precision + 1)
            {
                if (exponent == semantics.maxExponent)
                {
                    category = fcInfinity;

                    return opOverflow | opInexact;
                }

                shiftSignificandRight(1);
                return opInexact;
            }
        }

        if (omsb == semantics.precision)
            return opInexact;

        assert omsb < semantics.precision;

        if (omsb == 0)
            category = fcZero;

        return opUnderflow | opInexact;
    }
    /** Normalized addition or subtraction.  */
    private int addOrSubtract(APFloat rhs, RoundingMode rm, boolean subtract)
    {
        int fs;
        assertArithmeticOK(semantics);

        fs = addOrSubtractSpecials(rhs, subtract);

        if (fs == opDivByZero)
        {
            LostFraction LostFraction;
            LostFraction = addOrSubtractSignificand(rhs, subtract);
            fs = normalize(rm, LostFraction);

            assert category != fcZero || LostFraction == jlang.support.LostFraction.lfExactlyZero;
        }

        if (category == fcZero)
        {
            if (rhs.category != fcZero || (sign == rhs.sign) == subtract)
                sign = rm == rmTowardNegative;
        }

        return fs;
    }

    private CmpResult compareAbsoluteValue(APFloat rhs)
    {
        int compare;

        assert semantics == rhs.semantics;
        assert category == rhs.category;
        assert rhs.category == fcNormal;

        compare = exponent - rhs.exponent;

        if (compare == 0)
        {
            compare = APInt.tcCompare(significandParts(), rhs.significandParts()
                        , partCount());
        }

        if (compare > 0)
            return cmpGreaterThan;
        else if (compare < 0)
            return cmpLessThan;
        else
            return cmpEqual;
    }

    private int handleOverflow(RoundingMode rm)
    {
        if (rm == rmNearestTiesToEven
                || rm == rmNearestTiesToAway
                || (rm == rmTowardPositive && !sign)
                || (rm == rmTowardNegative && sign))
        {
            category = fcInfinity;
            return opOverflow | opInexact;
        }

        category = fcNormal;
        exponent = semantics.maxExponent;
        APInt.tcSetLeastSignificantBits(significandParts(),
                partCount(), semantics.precision);
        return opInexact;
    }
    /**
     * Returns TRUE if, when truncating the current number, with BIT the
     * new LSB, with the given lost fraction and rounding mode, the result
     * would need to be rounded away from zero (i.e., by increasing the
     * signficand).  This routine must work for fcZero of both signs, and
     * fcNormal numbers.
     */
    private boolean roundAwayFromZero(RoundingMode rm, LostFraction LostFraction, int bit)
    {
        assert category == fcNormal || category == fcZero;

        assert LostFraction != jlang.support.LostFraction.lfExactlyZero;

        switch (rm)
        {
            default:
                Util.shouldNotReachHere();
                return false;
            case rmNearestTiesToAway:
                return LostFraction == jlang.support.LostFraction.lfExactlyHalf || LostFraction ==  jlang.support.LostFraction.lfMoreThanHalf;
            case rmNearestTiesToEven:
                if (LostFraction == jlang.support.LostFraction.lfMoreThanHalf)
                    return true;

                if (LostFraction == jlang.support.LostFraction.lfExactlyHalf && category != fcZero)
                    return APInt.tcExtractBit(significandParts(), bit);

                return false;
            case rmTowardZero:
                return false;
            case rmTowardPositive:
                return !sign;
            case rmTowardNegative:
                return sign;
        }
    }

    private int convertToSignExtendedInteger(long[] parts, int width,
            boolean isSigned, RoundingMode rm, OutParamWrapper<Boolean> isExact)
    {
        int fs;

        fs = convertToSignExtendedInteger(parts, width, isSigned, rm, isExact);

        if (fs == opInvalidOp)
        {
            int bits, dstPartsCount;

            dstPartsCount = partCountForBits(width);

            if (category == fcNaN)
                bits = 0;
            else if (sign)
                bits = isSigned ? 1 : 0;
            else
                bits = width - (isSigned ? 1 : 0);

            APInt.tcSetLeastSignificantBits(parts, dstPartsCount, bits);
            if (sign && isSigned)
                APInt.tcShiftLeft(parts, dstPartsCount, width - 1);
        }

        return fs;
    }

    private int convertFromUnsignedParts(long[] src, int srcCount,
            RoundingMode rm)
    {
        int omsb, precision, dstCount;
        long[] dest;
        LostFraction lost_fraction;

        assertArithmeticOK(semantics);
        category = fcNormal;
        omsb = APInt.tcMSB(src, srcCount) + 1;
        dest = significandParts();
        dstCount = partCount();
        precision = semantics.precision;

        /* We want the most significant PRECISON bits of SRC.  There may not
         be that many; extract what we can.  */
        if (precision <= omsb)
        {
            exponent = omsb - 1;
            lost_fraction = lostFractionThroughTruncation(src, srcCount,
                    omsb - precision);
            APInt.tcExtract(dest, dstCount, src, precision, omsb - precision);
        }
        else
        {
            exponent = precision - 1;
            lost_fraction = LostFraction.lfExactlyZero;
            APInt.tcExtract (dest, dstCount, src, omsb, 0);
        }

        return normalize(rm, lost_fraction);
    }

    private static int skipLeadingZeroesAndAnyDot(String str, int begin, int end,
            OutParamWrapper<Integer> dot)
    {
        int i = begin;
        dot.set(end);
        while (str.charAt(i) == '0' && i != end)
        {
            ++i;
        }

        if (str.charAt(i) == '.')
        {
            dot.set(i++);
            assert end - begin != 1:"Significand has no digits";
            while (str.charAt(i) == '0' && i != end)
                ++i;
        }

        return i;
    }

    private static int hexDigitValue(char ch)
    {
        if (ch >= '0' && ch <= '9') return ch - '0';
        if (ch >= 'a' && ch <= 'f') return ch - 'a';
        if (ch >= 'A' && ch <= 'F') return ch - 'A';
        return -1;
    }

    private static LostFraction trailingHexadecimalFraction(String str, int begin, int end, int digitValue)
    {
        int hexDigits;

        if (digitValue > 8)
            return LostFraction.lfMoreThanHalf;
        else if (digitValue < 8 && digitValue > 0)
            return LostFraction.lfLessThanHalf;

        while (str.charAt(begin) == '0')
            ++begin;

        assert begin != end:"Invalid trailing hexadecimal fraction!";
        hexDigits = hexDigitValue(str.charAt(begin));

        if (hexDigits == -1)
            return digitValue == 0 ? LostFraction.lfExactlyZero : LostFraction.lfExactlyHalf;
        else
            return digitValue == 0 ? LostFraction.lfLessThanHalf : LostFraction.lfMoreThanHalf;
    }

    private static int decDigitValue(char ch)
    {
        return ch - '0';
    }

    private static int totalExponent(String str, int p, int end, int exponentAdjustment)
    {
        int unsignedExponent;
        boolean negative, overflow;
        int exponent = 0;

        assert p != end : "Exponent has no digits";

        negative = str.charAt(p) == '-';
        if (str.charAt(p) == '-' || str.charAt(p) == '+')
        {
            p++;
            assert p != end : "Exponent has no digits";
        }

        unsignedExponent = 0;
        overflow = false;
        for (; p != end; ++p)
        {
            int value;

            value = decDigitValue(str.charAt(p));
            assert value < 10 : "Invalid character in exponent";

            unsignedExponent = unsignedExponent * 10 + value;
            if (unsignedExponent > 65535)
                overflow = true;
        }

        if (exponentAdjustment > 65535 || exponentAdjustment < -65536)
            overflow = true;

        if (!overflow)
        {
            exponent = unsignedExponent;
            if (negative)
                exponent = -exponent;
            exponent += exponentAdjustment;
            if (exponent > 65535 || exponent < -65536)
                overflow = true;
        }

        if (overflow)
            exponent = negative ? -65536 : 65535;

        return exponent;
    }
    /** Return the value of a decimal exponent of the form
   [+-]ddddddd.

   If the exponent overflows, returns a large exponent with the
   appropriate sign.  */
    private static int readExponent(String str, int begin, int end)
    {
        boolean isNegative;
        int absExponent;
        int overlargeExponent = 24000;  /* FIXME.  */
        int p = begin;

        assert p != end : "Exponent has no digits";

        isNegative = (str.charAt(p) == '-');
        if (str.charAt(p) == '-' || str.charAt(p) == '+')
        {
        p++;
        assert p != end : "Exponent has no digits";
    }

        absExponent = decDigitValue(str.charAt(p++));
        assert absExponent < 10 : "Invalid character in exponent";

        for (; p != end; ++p) {
            int value;

            value = decDigitValue(str.charAt(p));
            assert value < 10 : "Invalid character in exponent";

            value += absExponent * 10;
            if (absExponent >= overlargeExponent) {
                absExponent = overlargeExponent;
                break;
            }
            absExponent = value;
        }

        assert p == end : "Invalid exponent in exponent";

        if (isNegative)
            return -absExponent;
        else
            return absExponent;
    }

    private int convertFromHexadecimalString(String str, RoundingMode rm)
    {
        LostFraction LostFraction = jlang.support.LostFraction.lfExactlyZero;
        long[] significand;
        int bitPos, partsCount;
        int dot, firstSignificantDigit;

        zeroSignificand();
        exponent = 0;
        category = fcNormal;

        significand = significandParts();
        partsCount = partCount();
        bitPos = partsCount * 64;
        OutParamWrapper<Integer> x = new OutParamWrapper<>();
        int begin = 0, end = str.length(), p = skipLeadingZeroesAndAnyDot(str, begin, end, x);
        firstSignificantDigit = p;
        dot = x.get();

        while (p != end)
        {
            long hexValue;

            if (str.charAt(p) == '.')
            {
                assert dot == end :"String contians multiple dots!";
                dot = p++;
                if (p == end)
                    break;
            }

            hexValue = hexDigitValue(str.charAt(p));
            if (hexValue == -1)
                break;

            p++;

            if (p == end)
            {
                break;
            }
            else
            {
                if (bitPos != 0)
                {
                    bitPos -= 4;
                    hexValue <<= bitPos % 64;
                    significand[bitPos / 64] |= hexValue;
                }
                else
                {
                    LostFraction = trailingHexadecimalFraction(str, p, end, (int)hexValue);
                    while (p != end && hexDigitValue(str.charAt(p)) != -1)
                        p++;
                    break;
                }
            }
        }

        assert p != end:"Hex strings require an exponent";
        assert str.charAt(p) == 'p' || str.charAt(p) == 'P' :"Invalid character in signicand";
        assert p != begin :"Sginificand has no digits";
        assert dot == end || p - begin != 1:"Significand has no digits";

        if (p != firstSignificantDigit)
        {
            int expAdjustment;

            if (dot == end)
                dot = p;

            expAdjustment = dot - firstSignificantDigit;
            if (expAdjustment < 0)
                expAdjustment++;

            expAdjustment = expAdjustment * 4 - 1;
            expAdjustment += semantics.precision;
            expAdjustment -= partsCount * 64;

            exponent = totalExponent(str, p+1, end, expAdjustment);
        }

        return normalize(rm, LostFraction);
    }

    private static decimalInfo interpretDecimal(String str, int begin, int end)
    {
        int dot = end;
        OutParamWrapper<Integer> x = new OutParamWrapper<>(dot);
        int p = skipLeadingZeroesAndAnyDot(str, begin, end, x);
        dot = x.get();
        decimalInfo d = new decimalInfo();
        d.digits = str;
        d.firstSigDigit = p;
        d.exponent = 0;
        d.normalizedExponent = 0;

        for (; p != end; ++p)
        {
            if (str.charAt(p) == '.')
            {
                assert dot == end : "String contains multiple dots";
                dot = p++;
                if (p == end)
                    break;
            }
            if (decDigitValue(str.charAt(p)) >= 10)
                break;
        }

        if (p != end)
        {
            assert str.charAt(p) == 'e' || str.charAt(p) == 'E';
            assert p != begin;
            assert dot == end || p - begin != 1;

            d.exponent = readExponent(str, p + 1, end);
            if (dot == end)
                dot = p;
        }

        if (p != d.firstSigDigit)
        {
            if (p != begin)
            {
                do
                {
                    do
                    {
                        p--;
                    } while (p != begin && str.charAt(p) == '0');
                } while (p != begin && str.charAt(p) == '.');
            }

            d.exponent += dot - p - (dot > p ? 1 : 0);
            d.normalizedExponent = (d.exponent + p - d.firstSigDigit - (
                    dot > d.firstSigDigit && dot < p ? 1 : 0));
        }

        d.lastSigDigit = p;
        return d;
    }

    private int convertFromDecimalString(String str, RoundingMode rm)
    {
        decimalInfo D;
        int fs;

        /* Scan the text.  */
        int p = 0;
        D = interpretDecimal(str, p, str.length());

      /* Handle the quick cases.  First the case of no significant digits,
         i.e. zero, and then exponents that are obviously too large or too
         small.  Writing L for log 10 / log 2, a number d.ddddd*10^exp
         definitely overflows if

               (exp - 1) * L >= maxExponent

         and definitely underflows to zero where

               (exp + 1) * L <= minExponent - precision

         With integer arithmetic the tightest bounds for L are

               93/28 < L < 196/59            [ numerator <= 256 ]
               42039/12655 < L < 28738/8651  [ numerator <= 65536 ]
      */
        if (decDigitValue(D.digits.charAt(D.firstSigDigit)) >= 10)
        {
            category = fcZero;
            fs = opOK;
        }
        else if ((D.normalizedExponent + 1) * 28738 <= 8651 * (
                semantics.minExponent - (int) semantics.precision))
        {
            /* Underflow to zero and round.  */
            zeroSignificand();
            fs = normalize(rm, LostFraction.lfLessThanHalf);
        }
        else if ((D.normalizedExponent - 1) * 42039
                >= 12655 * semantics.maxExponent)
        {
            /* Overflow and round.  */
            fs = handleOverflow(rm);
        }
        else
        {
            long[] decSignificand;
            int partCount;

            /* A tight upper bound on number of bits required to hold an
               N-digit decimal integer is N * 196 / 59.  Allocate enough space
               to hold the full significand, and an extra part required by
               tcMultiplyPart.  */
            partCount = D.lastSigDigit - D.firstSigDigit + 1;
            partCount = partCountForBits(1 + 196 * partCount / 59);
            decSignificand = new long[partCount + 1];
            partCount = 0;

            /* Convert to binary efficiently - we do almost all multiplication
               in an long.  When this would overflow do we do a single
               bignum multiplication, and then revert again to multiplication
               in an long.  */
            do
            {
                long decValue, val, multiplier;

                val = 0;
                multiplier = 1;

                do
                {
                    if (str.charAt(p) == '.')
                    {
                        p++;
                        if (p == str.length())
                        {
                            break;
                        }
                    }
                    decValue = decDigitValue(str.charAt(p++));
                    assert decValue < 10 : "Invalid character in significand";
                    multiplier *= 10;
                    val = val * 10 + decValue;
                    /* The maximum number that can be multiplied by ten with any
                       digit added without overflowing an long.  */
                } while (p <= D.lastSigDigit
                        && multiplier <= (~(long) 0 - 9) / 10);

                /* Multiply out the current part.  */
                APInt.tcMultiplyPart(decSignificand, 0, decSignificand,
                        multiplier, val, partCount, partCount + 1, false);

                  /* If we used another part (likely but not guaranteed), increase
                     the count.  */
                if (decSignificand[partCount] != 0)
                    partCount++;
            } while (p <= D.lastSigDigit);

            category = fcNormal;
            fs = roundSignificandWithExponent(decSignificand, partCount,
                    D.exponent, rm);
        }

        return fs;
    }

    /**
     * Write out an long in hexadecimal, starting with the most
     * significant nibble.  Write out exactly COUNT hexdigits, return
     * COUNT.
     */
    private static int partAsHex(char[] dst, long part, int count,
            char[] hexDigitChars)
    {
        int result = count;

        assert count != 0 : count <= 64 / 4;

        part >>= (64 - 4 * count);
        while ((count--) != 0)
        {
            dst[count] = hexDigitChars[(int) part & 0xf];
            part >>= 4;
        }

        return result;
    }

    /**
     * Write out a signed decimal integer.
     */
    private static char[] writeSignedDecimal(char[] dst, int value)
    {
        int i = 0;
        if (value < 0)
        {
            dst[i++] = '-';
            dst = writeUnsignedDecimal(dst, -(value));
        }
        else
            dst = writeUnsignedDecimal(dst, value);

        return dst;
    }

    /**
     * Place pow(5, power) in DST, and return the number of parts used.
     * DST must be at least one part larger than size of the answer.
     */
    private static int powerOf5(long[] dst, int power)
    {
        long firstEightPowers[] = { 1, 5, 25, 125, 625, 3125, 15625, 78125 };
        long[] pow5s = new long[maxPowerOfFiveParts * 2 + 5];
        pow5s[0] = 78125 * 5;

        int[] partsCount = new int[16];
        Arrays.fill(partsCount, 1);
        long[] scratch = new long[maxPowerOfFiveParts], p1, p2, pow5;
        int result;
        assert (power <= maxExponent);

        p1 = dst;
        p2 = scratch;
        p1[0] = firstEightPowers[power & 7];
        power >>= 3;

        result = 1;
        int pow5Idx = 0;

        for (int n = 0; power != 0; power >>>= 1, n++)
        {
            int pc;
            pc = partsCount[n];

            /* Calculate pow(5,pow(2,n+3)) if we haven't yet.  */
            if (pc == 0)
            {
                pc = partsCount[n - 1];
                long[] lhs = Arrays.copyOfRange(pow5s, pow5Idx - pc, pow5Idx);
                APInt.tcFullMultiply(pow5s, lhs, lhs, pc, pc);
                pc *= 2;
                if (pow5s[pow5Idx + pc - 1] == 0)
                    pc--;
                partsCount[n] = pc;
            }

            if ((power & 1) != 0)
            {
                long[] tmp;
                long[] rhs = Arrays.copyOfRange(pow5s, pow5Idx, pow5Idx + pc);
                APInt.tcFullMultiply(p2, p1, rhs, result, pc);
                result += pc;
                if (p2[result - 1] == 0)
                    result--;

              /* Now result is in p1 with partsCount parts and p2 is scratch
                 space.  */
                tmp = p1;
                p1 = p2;
                p2 = tmp;
            }

            pow5Idx += pc;
        }

        if (p1 != dst)
            APInt.tcAssign(dst, p1, result);

        return result;
    }

    private char[] convertNormalToHexString(char[] dest, int hexDigits,
            boolean upperCase, RoundingMode rm)
    {
        int count = 0, valueBits = 0, shift = 0, partsCount = 0, outputDigits = 0;
        String hexDigitChars;
        long[] significand;
        int p;
        boolean roundUp;
        int i = 0;
        dest[i++] = '0';
        dest[i++] = upperCase ? 'X' : 'x';

        roundUp = false;
        hexDigitChars = upperCase ? hexDigitsUpper : hexDigitsLower;

        significand = significandParts();
        partsCount = partCount();

        /* +3 because the first digit only uses the single integer bit, so
         we have 3 virtual zero most-significant-bits.  */
        valueBits = semantics.precision + 3;
        shift = 64 - valueBits % 64;

        /* The natural number of digits required ignoring trailing
         insignificant zeroes.  */
        outputDigits = (valueBits - significandLSB() + 3) / 4;

        /* hexDigits of zero means use the required number for the
         precision.  Otherwise, see if we are truncating.  If we are,
         find out if we need to round away from zero.  */
        if (hexDigits != 0)
        {
            if (hexDigits < outputDigits)
            {
                /* We are dropping non-zero bits, so need to check how to round.
                 "bits" is the number of dropped bits.  */
                int bits;
                LostFraction fraction;

                bits = valueBits - hexDigits * 4;
                fraction = lostFractionThroughTruncation(significand,
                        partsCount, bits);
                roundUp = roundAwayFromZero(rm, fraction, bits);
            }
            outputDigits = hexDigits;
        }

        /* Write the digits consecutively, and start writing in the location
         of the hexadecimal point.  We move the most significant digit
         left and add the hexadecimal point later.  */
        p = ++i;

        count = (valueBits + 64 - 1) / 64;

        while (outputDigits != 0 && count != 0)
        {
            long part;

            /* Put the most significant 64 bits in "part".  */
            if (--count == partsCount)
                part = 0;  /* An imaginary higher zero part.  */
            else
                part = significand[count] << shift;

            if (count != 0 && shift != 0)
                part |= significand[count - 1] >> (64 - shift);

            /* Convert as much of "part" to hexdigits as we can.  */
            int curDigits = 64 / 4;

            if (curDigits > outputDigits)
                curDigits = outputDigits;
            i += partAsHex(dest, part, curDigits, hexDigitChars.toCharArray());
            outputDigits -= curDigits;
        }

        if (roundUp)
        {
            int q = i;

            /* Note that hexDigitChars has a trailing '0'.  */
            do
            {
                q--;
                dest[q] = hexDigitChars.charAt(hexDigitValue(dest[q]) + 1);
            } while (dest[q] == '0');
            assert (q >= p);
        }
        else
        {
            /* Add trailing zeroes.  */
            Arrays.fill(dest, 0, outputDigits, '0');
            i += outputDigits;
        }

        /* Move the most significant digit to before the point, and if there
        is something after the decimal point add it.  This must come
        after rounding above.  */
        dest[p - 1] = dest[p];
        if (i - 1 == p)
            i--;
        else
            dest[p] = '.';

        /* Finally output the exponent.  */
        dest[i++] = upperCase ? 'P' : 'p';

        return writeSignedDecimal(dest, exponent);
    }

    /***
     * The error from the true value, in half-ulps, on multiplying two
     * floating point numbers, which differ from the value they
     * approximate by at most HUE1 and HUE2 half-ulps, is strictly less
     * than the returned value.

     * See "How to Read Floating Point Numbers Accurately" by William D
     * Clinger.
     */
    private static int HUerrBound(boolean inexactMultiply, int HUerr1,
            int HUerr2)
    {
        assert (HUerr1 < 2 || HUerr2 < 2 || (HUerr1 + HUerr2 < 8));

        if (HUerr1 + HUerr2 == 0)
            return (inexactMultiply ? 1 : 0) * 2;  /* <= inexactMultiply half-ulps.  */
        else
            return (inexactMultiply ? 1 : 0) + 2 * (HUerr1 + HUerr2);
    }

    /**
     * The number of ulps from the boundary (zero, or half if ISNEAREST)
     * when the least significant BITS are truncated.  BITS cannot be
     * zero.
     */
    private static long ulpsFromBoundary(long[] parts, int bits,
            boolean isNearest)
    {
        int count, partBits;
        long part, boundary;

        assert bits != 0;

        bits--;
        count = bits / 64;
        partBits = bits % 64 + 1;
        part = parts[count] & ((~0) >> (64 - partBits));
        if (isNearest)
            boundary = 1 << (partBits - 1);
        else
            boundary = 0;

        if (count == 0)
        {
            if (part - boundary <= boundary - part)
                return part - boundary;
            else
                return boundary - part;
        }
        if (part == boundary)
        {
            while ((--count) != 0)
            {
                if (parts[count] != 0)
                    return ~0;
            }
            return parts[0];
        }
        else if (part == boundary - 1)
        {
            while ((--count) != 0)
                if (~parts[count] != 0)
                    return ~0;
            return -parts[0];
        }
        return ~0;
    }

    private int roundSignificandWithExponent(long[] decSigParts,
            int sigPartCount, int exp, RoundingMode rm)
    {
        int parts, pow5PartCount;
        FltSemantics calcSemantics = new FltSemantics((short) 32767,
                (short) -32767, 0, true);
        long[] pow5Parts = new long[maxPowerOfFiveParts];
        boolean isNearest;

        isNearest = (rm == rmNearestTiesToEven || rm == rmNearestTiesToAway);

        parts = partCountForBits(semantics.precision + 11);

        /* Calculate pow(5, abs(exp)).  */
        pow5PartCount = powerOf5(pow5Parts, exp >= 0 ? exp : -exp);

        for (; ; parts *= 2)
        {
            int sigStatus, powStatus;
            int excessPrecision, truncatedBits;

            calcSemantics.precision = parts * 64 - 1;
            excessPrecision = calcSemantics.precision - semantics.precision;
            truncatedBits = excessPrecision;

            APFloat decSig = new APFloat(calcSemantics, fcZero, sign);
            APFloat pow5 = new APFloat(calcSemantics, fcZero, false);

            sigStatus = decSig
                    .convertFromUnsignedParts(decSigParts, sigPartCount,
                            rmNearestTiesToEven);
            powStatus = pow5.convertFromUnsignedParts(pow5Parts, pow5PartCount,
                    rmNearestTiesToEven);
            /* Add exp, as 10^n = 5^n * 2^n.  */
            decSig.exponent += exp;

            LostFraction calcLostFraction;
            long HUerr, HUdistance;
            int powHUerr;

            if (exp >= 0)
            {
                /* multiplySignificand leaves the precision-th bit set to 1.  */
                calcLostFraction = decSig.multiplySignificand(pow5, null);
                powHUerr = powStatus != opOK ? 1 : 0;
            }
            else
            {
                calcLostFraction = decSig.divideSignificand(pow5);
                /* Denormal numbers have less precision.  */
                if (decSig.exponent < semantics.minExponent)
                {
                    excessPrecision += (semantics.minExponent
                            - decSig.exponent);
                    truncatedBits = excessPrecision;
                    if (excessPrecision > calcSemantics.precision)
                        excessPrecision = calcSemantics.precision;
                }
                /* Extra half-ulp lost in reciprocal of exponent.  */
                powHUerr = (powStatus == opOK
                        && calcLostFraction == LostFraction.lfExactlyZero) ? 0 : 2;
            }

            /* Both multiplySignificand and divideSignificand return the
            result with the integer bit set.  */
            assert APInt.tcExtractBit(decSig.significandParts(),
                    calcSemantics.precision - 1);

            HUerr = HUerrBound(calcLostFraction != LostFraction.lfExactlyZero,
                    sigStatus != opOK ? 1 : 0, powHUerr);
            HUdistance = 2 * ulpsFromBoundary(decSig.significandParts(),
                    excessPrecision, isNearest);

            /* Are we guaranteed to round correctly if we truncate?  */
            if (HUdistance >= HUerr)
            {
                APInt.tcExtract(significandParts(), partCount(),
                        decSig.significandParts(),
                        calcSemantics.precision - excessPrecision,
                        excessPrecision);
                /* Take the exponent of decSig.  If we tcExtract-ed less bits
                 above we must adjust our exponent to compensate for the
                 implicit right shift.  */
                exponent = (decSig.exponent + semantics.precision - (
                        calcSemantics.precision - excessPrecision));
                calcLostFraction = lostFractionThroughTruncation(
                        decSig.significandParts(), decSig.partCount(),
                        truncatedBits);
                return normalize(rm, calcLostFraction);
            }
        }
    }

    private APInt convertFloatAPFloatToAPInt()
    {
        assert (semantics == IEEEsingle);
        assert (partCount() == 1);

        int myexponent, mysignificand;

        if (category == fcNormal)
        {
            myexponent = exponent + 127; //bias
            mysignificand = (int) significandParts()[0];
            if (myexponent == 1 && (mysignificand & 0x800000) == 0)
                myexponent = 0;   // denormal
        }
        else if (category == fcZero)
        {
            myexponent = 0;
            mysignificand = 0;
        }
        else if (category == fcInfinity)
        {
            myexponent = 0xff;
            mysignificand = 0;
        }
        else
        {
            assert category == fcNaN : "Unknown category!";
            myexponent = 0xff;
            mysignificand = (int) significandParts()[0];
        }

        return new APInt(32,
                ((((sign ? 1 : 0) & 1) << 31) | ((myexponent & 0xff) << 23) | (
                        mysignificand & 0x7fffff)));
    }

    private APInt convertDoubleAPFloatToAPInt()
    {
        assert (semantics == IEEEdouble);
        assert (partCount() == 1);

        long myexponent, mysignificand;

        if (category == fcNormal)
        {
            myexponent = exponent + 1023; //bias
            mysignificand = significandParts()[0];
            if (myexponent == 1 && (mysignificand & 0x10000000000000L) == 0)
                myexponent = 0;   // denormal
        }
        else if (category == fcZero)
        {
            myexponent = 0;
            mysignificand = 0;
        }
        else if (category == fcInfinity)
        {
            myexponent = 0x7ff;
            mysignificand = 0;
        }
        else
        {
            assert category == fcNaN : "Unknown category!";
            myexponent = 0x7ff;
            mysignificand = significandParts()[0];
        }

        return new APInt(64,
                ((((long) ((sign ? 1 : 0) & 1) << 63) | ((myexponent & 0x7ff)
                        << 52) | (mysignificand & 0xfffffffffffffL))));
    }

    private APInt convertQuadrupleAPFloatToAPInt()
    {
        assert (semantics == IEEEquad);
        assert (partCount() == 2);

        long myexponent, mysignificand, mysignificand2;

        if (category == fcNormal)
        {
            myexponent = exponent + 16383; //bias
            mysignificand = significandParts()[0];
            mysignificand2 = significandParts()[1];
            if (myexponent == 1 && (mysignificand2 & 0x1000000000000L) == 0)
                myexponent = 0;   // denormal
        }
        else if (category == fcZero)
        {
            myexponent = 0;
            mysignificand = mysignificand2 = 0;
        }
        else if (category == fcInfinity)
        {
            myexponent = 0x7fff;
            mysignificand = mysignificand2 = 0;
        }
        else
        {
            assert category == fcNaN : "Unknown category!";
            myexponent = 0x7fff;
            mysignificand = significandParts()[0];
            mysignificand2 = significandParts()[1];
        }

        long[] words = new long[2];
        words[0] = mysignificand;
        words[1] = ((long) ((sign ? 1 : 0) & 1) << 63) | ((myexponent & 0x7fff)
                << 48) | (mysignificand2 & 0xffffffffffffL);

        return new APInt(128, 2, words);
    }

    private APInt convertF80LongDoubleAPFloatToAPInt()
    {
        assert (semantics == x87DoubleExtended);
        assert (partCount() == 2);

        long myexponent, mysignificand;

        if (category == fcNormal)
        {
            myexponent = exponent + 16383; //bias
            mysignificand = significandParts()[0];
            if (myexponent == 1 && (mysignificand & 0x8000000000000000L) == 0)
                myexponent = 0;   // denormal
        }
        else if (category == fcZero)
        {
            myexponent = 0;
            mysignificand = 0;
        }
        else if (category == fcInfinity)
        {
            myexponent = 0x7fff;
            mysignificand = 0x8000000000000000L;
        }
        else
        {
            assert category == fcNaN : "Unknown category";
            myexponent = 0x7fff;
            mysignificand = significandParts()[0];
        }

        long[] words = new long[2];
        words[0] = mysignificand;
        words[1] = ((long) ((sign ? 1 : 0) & 1) << 15) | (myexponent & 0x7fffL);
        return new APInt(80, 2, words);
    }

    private void initFromAPInt(APInt api)
    {
        initFromAPInt(api, false);
    }

    private void initFromAPInt(APInt api, boolean isIEEE)
    {
        if (api.getBitWidth() == 32)
        {
            initFromFloatAPInt(api);
            return;
        }
        else if (api.getBitWidth() == 64)
        {
            initFromDoubleAPInt(api);
            return;
        }
        else if (api.getBitWidth() == 80)
        {
            initFromF80LongDoubleAPInt(api);
            return;
        }
        else if (api.getBitWidth() == 128)
            if (isIEEE)
                initFromQuadrupleAPInt(api);
            else
                Util.shouldNotReachHere();
    }

    private void initFromFloatAPInt(APInt api)
    {
        assert (api.getBitWidth() == 32);
        int i = (int) api.getRawData()[0];
        int myexponent = (i >> 23) & 0xff;
        int mysignificand = i & 0x7fffff;

        initialize(IEEEsingle);
        assert (partCount() == 1);

        sign = (i >> 31) != 0;
        if (myexponent == 0 && mysignificand == 0)
        {
            // exponent, significand meaningless
            category = fcZero;
        }
        else if (myexponent == 0xff && mysignificand == 0)
        {
            // exponent, significand meaningless
            category = fcInfinity;
        }
        else if (myexponent == 0xff && mysignificand != 0)
        {
            // sign, exponent, significand meaningless
            category = fcNaN;
            setSignificand(mysignificand);
        }
        else
        {
            category = fcNormal;
            exponent = myexponent - 127;  //bias
            setSignificand(mysignificand);
            if (myexponent == 0)    // denormal
                exponent = -126;
            else
                setSignificand(0x800000);   // integer bit
        }
    }

    private void initFromDoubleAPInt(APInt api)
    {
        assert (api.getBitWidth() == 64);
        long i = api.getRawData()[0];
        long myexponent = (i >> 52) & 0x7ff;
        long mysignificand = i & 0xfffffffffffffL;

        initialize(IEEEdouble);
        assert (partCount() == 1);

        sign = (i >> 63) != 0;
        if (myexponent == 0 && mysignificand == 0)
        {
            // exponent, significand meaningless
            category = fcZero;
        }
        else if (myexponent == 0x7ff && mysignificand == 0)
        {
            // exponent, significand meaningless
            category = fcInfinity;
        }
        else if (myexponent == 0x7ff && mysignificand != 0)
        {
            // exponent meaningless
            category = fcNaN;
            setSignificand(mysignificand);
        }
        else
        {
            category = fcNormal;
            exponent = (int) myexponent - 1023;
            setSignificand(mysignificand);
            if (myexponent == 0)          // denormal
                exponent = -1022;
            else
                setSignificand(significandParts()[0]
                        | 0x10000000000000L);  // integer bit
        }
    }

    private void initFromQuadrupleAPInt(APInt api)
    {
        assert (api.getBitWidth() == 128);
        long i1 = api.getRawData()[0];
        long i2 = api.getRawData()[1];
        long myexponent = (i2 >> 48) & 0x7fff;
        long mysignificand = i1;
        long mysignificand2 = i2 & 0xffffffffffffL;

        initialize(IEEEquad);
        assert (partCount() == 2);

        sign = (i2 >> 63) != 0;
        if (myexponent == 0 && (mysignificand == 0 && mysignificand2 == 0))
        {
            // exponent, significand meaningless
            category = fcZero;
        }
        else if (myexponent == 0x7fff && (mysignificand == 0
                && mysignificand2 == 0))
        {
            // exponent, significand meaningless
            category = fcInfinity;
        }
        else if (myexponent == 0x7fff && (mysignificand != 0
                || mysignificand2 != 0))
        {
            // exponent meaningless
            category = fcNaN;
            setSignificand(0, mysignificand);
            setSignificand(1, mysignificand2);
        }
        else
        {
            category = fcNormal;
            exponent = (int) myexponent - 16383;
            setSignificand(0, mysignificand);
            setSignificand(1, mysignificand2);
            if (myexponent == 0)          // denormal
                exponent = -16382;
            else
                // integer bit
                setSignificand(1, significandParts()[1] | 0x1000000000000L);
        }
    }

    /// Integer bit is explicit in this format.  Intel hardware (387 and later)
    /// does not support these bit patterns:
    ///  exponent = all 1's, integer bit 0, significand 0 ("pseudoinfinity")
    ///  exponent = all 1's, integer bit 0, significand nonzero ("pseudoNaN")
    ///  exponent = 0, integer bit 1 ("pseudodenormal")
    ///  exponent!=0 nor all 1's, integer bit 0 ("unnormal")
    /// At the moment, the first two are treated as NaNs, the second two as Normal.
    private void initFromF80LongDoubleAPInt(APInt api)
    {
        assert (api.getBitWidth() == 80);
        long i1 = api.getRawData()[0];
        long i2 = api.getRawData()[1];
        long myexponent = (i2 & 0x7fff);
        long mysignificand = i1;

        initialize(x87DoubleExtended);
        assert (partCount() == 2);

        sign = (i2 >> 15) != 0;
        if (myexponent == 0 && mysignificand == 0)
        {
            // exponent, significand meaningless
            category = fcZero;
        }
        else if (myexponent == 0x7fff && mysignificand == 0x8000000000000000L)
        {
            // exponent, significand meaningless
            category = fcInfinity;
        }
        else if (myexponent == 0x7fff && mysignificand != 0x8000000000000000L)
        {
            // exponent meaningless
            category = fcNaN;
            setSignificand(0, mysignificand);
            setSignificand(1, 0);
        }
        else
        {
            category = fcNormal;
            exponent = (int) myexponent - 16383;
            setSignificand(0, mysignificand);
            setSignificand(1, 0);
            if (myexponent == 0)          // denormal
                exponent = -16382;
        }
    }

    private void assign(APFloat rhs)
    {
        assert (semantics == rhs.semantics);

        sign = rhs.sign;
        category = rhs.category;
        exponent = rhs.exponent;
        if (category == fcNormal || category == fcNaN)
            copySignificand(rhs);
    }

    private void copySignificand(APFloat rhs)
    {
        assert (category == fcNormal || category == fcNaN);
        assert (rhs.partCount() >= partCount());

        APInt.tcAssign(significandParts(), rhs.significandParts(), partCount());
    }

    /* What kind of semantics does this value obey?  */
    private FltSemantics semantics;

    /* Significand - the fraction with an explicit integer bit.  Must be
       at least one bit wider than the target precision.  */
    private long part;
    private long[] parts;

    private int exponent;
    /* What kind of floating point number this is.  */
    private FltCategory category;
    /**
     * The sign bit of this number.
     */
    private boolean sign;

    /* A tight upper bound on number of parts required to hold the value
   pow(5, power) is

     power * 815 / (351 * integerPartWidth) + 1

   However, whilst the result may require only this many parts,
   because we are multiplying two values to get it, the
   multiplication may require an extra part with the excess part
   being zero (consider the trivial case of 1 * 1, tcFullMultiply
   requires two parts to hold the single-part result).  So we add an
   extra one to guarantee enough space whilst multiplying.  */
    public static final int maxExponent = 16383;
    public static final int maxPrecision = 113;
    public static final int maxPowerOfFiveExponent =
            maxExponent + maxPrecision - 1;
    public static final int maxPowerOfFiveParts =
            2 + ((maxPowerOfFiveExponent * 815) / (351 * 64));
}
