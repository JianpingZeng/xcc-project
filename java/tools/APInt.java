/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package tools;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import static tools.Util.countLeadingZeros32;

/**
 * Class for arbitrary precision integers.
 * <br>
 * APInt - This class represents arbitrary precision constant integral values.
 * It is a functional replacement for common case  integer jlang.type like
 * "", " long" or "uint64_t", but also allows non-byte-width
 * integer sizes and large integer value types such as 3-bits, 15-bits, or more
 * than 64-bits of precision. {@linkplain APInt} provides a variety of arithmetic
 * operators and methods to manipulate integer values of any bit-width. It supports
 * both the typical integer arithmetic and comparison operations as well as bitwise
 * manipulation.
 * </br>
 * The class has several invariants worth noting:
 * <ol>
 *     <li>
 *      * All bit, byte, and word positions are zero-based.
 *     </li>
 *     <li>
 *      * Once the bit width is set, it doesn't change except by the Truncate,
 *     SignExtend, or ZeroExtend operations.
 *     </li>
 *     <li>
 *      * All binary operators must be on APInt instances of the same bit width.
 *     Attempting to use these operators on instances with different bit
 *     widths will yield an assertion.
 *     </li>
 *     <li>
 *      * The value is stored canonically as an  value. For operations
 *     where it makes a difference, there are both signed and  variants
 *     of the operation. For example, sdiv and udiv. However, because the bit
 *     widths must be the same, operations such as Mul and Add produce the same
 *     results regardless of whether the values are interpreted as signed or
 *     not.
 *     </li>
 *     <li>
 *       * In general, the class tries to follow the style of computation that LLVM
 *     uses in its IR. This simplifies its use for LLVM.
 *     </li>
 * </ol>
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class APInt implements Cloneable
{
    /**
     * THe number of bits in this {@linkplain APInt}.
     */
    private int bitWidth;
    /**
     * Used to store the <= 64 bits integer.
     */
    private long val;
    /**
     * Used to store the >64 bits integer.
     */
    private long[] pVal;

    private static int CHAR_BIT = 8;

    /**
     * Bytes of a word (b bytes).
     */
    private static int APINT_WORD_SIZE = 8;

    /**
     * Bits in a word (64 bits).
     */
    private static int APINT_BITS_PER_WORD = APINT_WORD_SIZE * CHAR_BIT;

    private APInt(long[] val, int bits)
    {
        assert val.length == bits;
        assert bits > 0 : "bitwidth too small";
        bitWidth = bits;
        pVal = val;
    }

    public APInt clone()
    {
        try
        {
            return (APInt)super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Checks if this integer value is stored in a single word(64).
     *
     * @return
     */
    private boolean isSingleWord()
    {
        return bitWidth <= APINT_BITS_PER_WORD;
    }

    /**
     * Determines which word a bit is in.
     *
     * @param bitPosition
     * @return Return the word position for the specified bits position.
     */
    private static int whichWord(int bitPosition)
    {
        return bitPosition / APINT_BITS_PER_WORD;
    }

    /**
     * Determines which bits in a word a bit is in.
     *
     * @param bitPosition
     * @return
     */
    private static long whichBit(long bitPosition)
    {
        return bitPosition % APINT_BITS_PER_WORD;
    }

    /**
     * This function generates and returns a long(word) mask for a single bit.
     * at a specified bit position.
     *
     * @param bitPosition
     * @return
     */
    private static long maskBit(int bitPosition)
    {
        return 1L << whichBit(bitPosition);
    }

    /**
     * This method is used internally to clear the to "N" bits in the high order
     * word that are not used by the APInt. This is needed after the most
     * significant word is assigned a value to ensure that those bits are
     * zero'd out.
     *
     * @return
     */
    private APInt clearUnusedBits()
    {
        // compute how many bits are used in the final wordd.
        long wordBits = bitWidth % APINT_BITS_PER_WORD;
        if (wordBits == 0)
        {
            // if all bits are used, we want to leave the value alone.
            // This also avoids the undefined behavior of >> when the shift
            // is the same getNumOfSubLoop as the word getNumOfSubLoop(64).
            return this;
        }

        long mask = ~0L;
        if (isSingleWord())
            val &= mask;
        else
            pVal[getNumWords() - 1] &= mask;
        return this;
    }

    private long getWord(int bitPosition)
    {
        return isSingleWord() ? val : pVal[whichWord(bitPosition)];
    }

    private int getNumWords(int bitWidth)
    {
        return (bitWidth + APINT_BITS_PER_WORD - 1) / APINT_BITS_PER_WORD;
    }

    public int getNumWords()
    {
        return getNumWords(bitWidth);
    }

    /**
     * This is used by the constructors that take string arguments.
     * @param numBits
     * @param str
     * @param radix
     */
    private void fromString(int numBits, String str, int radix)
    {
        assert !str.isEmpty() : "Invalid string length";
        assert (radix == 10 || radix == 8 || radix == 2
                || radix == 16) : "Radix should be 2, 8, 10, or 16!";

        int slen = str.length();
        char[] arr = str.toCharArray();
        boolean isNeg = arr[0] == '-';
        int i = 0;
        if (arr[i] == '-' || arr[i] == '+')
        {
            i++;
            slen--;
            assert slen != 0 : "String is only a sign, needs a value.";
        }

        assert slen < numBits || radix != 2 : "Insufficient bit width";
        assert (slen - 1) * 3 <= numBits
                || radix != 8 : "Insufficient bit width";
        assert (slen - 1) * 4 <= numBits
                || radix != 16 : "Insufficient bit width";
        assert ((slen - 1) * 64) / 22 <= numBits
                || radix != 10 : "Insufficient bit width";

        // Allocate memory.
        if (!isSingleWord())
            pVal = new long[getNumWords()];

        // Figure out if we can shift instead of mul
        int shift = (radix == 16 ? 4 : radix == 8 ? 3 : radix == 2 ? 1 : 0);

        // Set up an APInt for the digit to add outside the loop so we don't
        // constantly construct/destruct it.
        APInt apdigit = new APInt(getBitWidth(), 0);
        APInt apradix = new APInt(getBitWidth(), radix);

        // Enter digit traversal loop
        for (; i != str.length(); i++)
        {
            int digit = getDigit(arr[i], radix);
            assert digit < radix && digit >= 0: "Invalid character in digit string";

            // Shift or mul the value by the radix
            if (slen > 1)
            {
                if (shift != 0)
                    shlAssign(shift);
                else
                    mulAssign(apradix);
            }

            // Add in the digit we just interpreted
            if (apdigit.isSingleWord())
                apdigit.val = digit;
            else
                apdigit.pVal[0] = digit;
            this.addAssign(apdigit);
        }

        // If its negative, put it in two's complement form
        if (isNeg)
        {
            this.decrease();
            this.flip();
        }
    }

    /**
     *  A utility function that converts a character to a digit.
     * @param ch
     * @param radix
     * @return
     */
    private static int getDigit(char ch, int radix)
    {
        int r;
        if (radix == 16)
        {
            r = ch - '0';
            if (r <= 9)
                return r;
            r = ch - 'A';
            if (r <= 5)
                return r + 10;
            r = ch - 'a';
            if (r <= 5)
                return r + 10;
        }
        if (radix == 8)
        {
            r = ch - '0';
            if (r <= 7)
                return r;
        }
        if (radix == 2)
        {
            r = ch - '0';
            if (r<= 1)
                return r;
        }
        return -1;
    }

    private void initSlowCase(int numBits, long val, boolean isSigned)
    {
        pVal = new long[getNumWords()];
        pVal[0] = val;
        if (isSigned && val < 0)
        {
            for (int i = 1; i < getNumWords(); ++i)
                pVal[i] = -1L;
        }
    }

    private void initFromArray(long[] bigVal)
    {
        assert bitWidth > 0 : "bitwidth too small " + bitWidth;
        assert bigVal != null : "empty list";
        if (isSingleWord())
            val = bigVal[0];
        else
        {
            pVal = new long[getNumWords()];
            int words = Math.min(bigVal.length, getNumWords());
            System.arraycopy(bigVal, 0, pVal, 0, words * APINT_WORD_SIZE);
        }
        clearUnusedBits();
    }

    private void initFromArray(ArrayList<Long> bigVal)
    {
        assert bitWidth > 0 : "bitwidth too small " + bitWidth;
        assert !bigVal.isEmpty() : "empty list";
        if (isSingleWord())
            val = bigVal.get(0);
        else
        {
            pVal = new long[getNumWords()];
            int words = Math.min(bigVal.size(), getNumWords());
            System.arraycopy(bigVal.toArray(), 0, pVal, 0, words * APINT_WORD_SIZE);
        }
        clearUnusedBits();
    }

    private void initSlowCase(final APInt that)
    {
        pVal = new long[getNumWords()];
        System.arraycopy(that.pVal, 0, pVal, 0, getNumWords());
    }

    private APInt shlSlowCase(int shiftAmt)
    {
        // If all the bits were shifted out, the result is 0. This avoids issues
        // with shifting by the getNumOfSubLoop of the integer type, which produces undefined
        // results. We define these "undefined results" to always be 0.
        if (shiftAmt == bitWidth)
            return new APInt(bitWidth, 0);

        if (shiftAmt == 0)
            return this;

        long[] val = new long[getNumWords()];

        if (shiftAmt < APINT_BITS_PER_WORD)
        {
            long carray = 0;
            for (int i = 0; i < getNumWords(); ++i)
            {
                val[i] = (pVal[i] << shiftAmt) | carray;
                carray = pVal[i] >> (APINT_BITS_PER_WORD - shiftAmt);
            }
            return new APInt(val, bitWidth).clearUnusedBits();
        }

        int wordShift = shiftAmt % APINT_BITS_PER_WORD;
        int offset = shiftAmt / APINT_BITS_PER_WORD;

        // If we are shifting whole words, just move whole words
        if (wordShift == 0)
        {
            for (int i = 0; i < offset; i++)
                val[i] = 0;

            for (int i = offset; i < getNumWords(); i++)
                val[i] = pVal[i - offset];
            return new APInt(val, bitWidth).clearUnusedBits();
        }

        int i = getNumWords() - 1;
        for (; i > offset; i--)
        {
            val[i] = pVal[i - offset] << wordShift | pVal[i - offset - 1] >> (
                    APINT_BITS_PER_WORD - wordShift);
        }
        val[offset] = pVal[0] << wordShift;
        for (i = 0; i < offset; i++)
            val[i] = 0;

        return new APInt(val, bitWidth).clearUnusedBits();
    }

    private APInt andSlowCase(final APInt rhs)
    {
        int numWords = getNumWords();
        long[] val = new long[numWords];

        for (int i = 0; i < numWords; i++)
            val[i] = pVal[i] & rhs.pVal[i];
        return new APInt(val, bitWidth);
    }

    private APInt orSlowCase(final APInt rhs)
    {
        int numWords = getNumWords();
        long[] val = new long[numWords];

        for (int i = 0; i < numWords; i++)
            val[i] = pVal[i] | rhs.pVal[i];
        return new APInt(val, bitWidth);
    }

    private APInt xorSlowCase(final APInt rhs)
    {
        int numWords = getNumWords();
        long[] val = new long[numWords];

        for (int i = 0; i < numWords; i++)
            val[i] = pVal[i] ^ rhs.pVal[i];
        return new APInt(val, bitWidth);
    }

    private APInt assignSlowCase(final APInt rhs)
    {
        if (this == rhs)
            return this;
        if (bitWidth == rhs.bitWidth)
        {
            assert !isSingleWord();
            System.arraycopy(rhs.pVal, 0, pVal, 0, getNumWords() * APINT_WORD_SIZE);
            return this;
        }

        if (isSingleWord())
        {
            assert !rhs.isSingleWord();
            val = 0;
            pVal = new long[rhs.getNumWords()];
            System.arraycopy(rhs.pVal, 0, pVal, 0, rhs.getNumWords() * APINT_WORD_SIZE);
        }
        else if (getNumWords() == rhs.getNumWords())
        {
            System.arraycopy(rhs.pVal, 0, pVal, 0, rhs.getNumWords() * APINT_WORD_SIZE);
        }
        else if (rhs.isSingleWord())
        {
            pVal = null;
            val = rhs.val;
        }
        else
        {
            pVal = new long[rhs.getNumWords()];
            System.arraycopy(rhs.pVal, 0, pVal, 0, rhs.getNumWords() * APINT_WORD_SIZE);
        }
        bitWidth = rhs.bitWidth;
        return clearUnusedBits();
    }

    private boolean equalSlowCase(final APInt rhs)
    {
        // optimize the common cases.
        // get some facts about the numbers of bits used in the two operands.
        int n1 = getActiveBits();
        int n2 = rhs.getActiveBits();

        if (n1 != n2)
            return false;

        // If the number of bits fits in a word, we only need to compare the low word.
        if (n1 <= APINT_BITS_PER_WORD)
            return pVal[0] == rhs.pVal[0];

        // Otherwise, compare each word.
        for (int i = whichWord(n1 - 1); i >= 0; i--)
            if (pVal[i] != rhs.pVal[i])
                return false;

        return true;
    }

    private boolean equalSlowCase(long val)
    {
        int n = getActiveBits();
        if (n <= APINT_BITS_PER_WORD)
            return pVal[0] == val;

        return false;
    }

    public APInt(int numBits, long val)
    {
        this(numBits, val, false);
    }

    public APInt(int numBits, long val, boolean isSigned)
    {
        bitWidth = numBits;
        this.val = 0;
        assert bitWidth > 0 : "bitwidth too small";
        if (isSingleWord())
            this.val = val;
        else
            initSlowCase(numBits, val, isSigned);
        clearUnusedBits();
    }

    public APInt(int numBits, ArrayList<Long> bigVal)
    {
        bitWidth = numBits;
        val = 0;
        initFromArray(bigVal);
    }

    public APInt(int numBits, int numWords, final long bigVal[])
    {
        bitWidth = numBits;
        val = 0;
        initFromArray(bigVal);
    }

    public APInt(int numBits, String str, int radix)
    {
        bitWidth = numBits;
        assert numBits > 0 : "bitwidth too small";
        fromString(numBits, str, radix);
    }

    public APInt()
    {
        bitWidth = 1;
    }

    public APInt(final APInt that)
    {
        bitWidth = that.bitWidth;
        val = 0;
        assert bitWidth > 0 : "bitwidth too small";
        if (isSingleWord())
            val = that.val;
        else
            initSlowCase(that);
    }

    public boolean isNegative()
    {
        return get(bitWidth - 1);
    }

    public boolean isNonNegative()
    {
        return !isNegative();
    }

    public APInt assign(long rhs)
    {
        if (isSingleWord())
            val = rhs;
        else
        {
            pVal[0] = rhs;
        }
        return clearUnusedBits();
    }

    public APInt assign(final APInt rhs)
    {
        if (isSingleWord() && rhs.isSingleWord())
        {
            val = rhs.val;
            bitWidth = rhs.bitWidth;
            return clearUnusedBits();
        }
        return assignSlowCase(rhs);
    }

    public int getBitWidth()
    {
        return bitWidth;
    }

    private boolean add_1(long dest[], long x[], int len, long y)
    {
        for (int i = 0; i < len; i++)
        {
            dest[i] = x[i] + y;
            if (dest[i] < y)
                y = 1;      // carry one to next digit.
            else
            {
                y = 0;
                break;
            }
        }
        return y != 0;
    }

    private boolean sub_1(long[] dest, int len, long y)
    {
        for (int i = 0; i < len; i++)
        {
            long X = dest[i];
            dest[i] -= y;
            if (y > X)
                y = 1; // we have to borrow 1 from next digit.
            else
            {
                y = 0;  // no need to borrow.
                break;  // remaining digits are unchanged so exit early.
            }
        }
        return y != 0;
    }

    public APInt increase()
    {
        if (isSingleWord())
            val++;
        else
            add_1(pVal, pVal, getNumWords(), 1);
        return clearUnusedBits();
    }

    public APInt decrease()
    {
        if (isSingleWord())
            --val;
        else
            sub_1(pVal, getNumWords(), 1);
        return clearUnusedBits();
    }

    /**
     * Zero extend to a new width.
     *
     * @param width
     * @return
     */
    public APInt zext(int width)
    {
        assert width > 0 : "Invalid APInt ZeroExtend request";
        if (width <= APINT_WORD_SIZE)
            return new APInt(width, val);

        APInt result = new APInt(new long[getNumWords(width)], width);

        // Copys words
        int i;
        for (i = 0; i != getNumWords(); i++)
            result.pVal[i] = getRawData()[i];

        // Zero remaining words.
        Arrays.fill(result.pVal, i, result.pVal.length, 0);
        return result;
    }

    public APInt sext(int width)
    {
        assert width > bitWidth : "Invalid APInt SignExtend request";

        // if the sign bits of this is not set, this is the zext.
        if (!isNegative())
        {
            zext(width);
            return this;
        }
        // the sign bit is set.
        if (width <= APINT_BITS_PER_WORD)
        {
            long val_ = val << (APINT_BITS_PER_WORD - bitWidth);
            return new APInt(width, val_ >> (APINT_BITS_PER_WORD - width));
        }

        APInt result = new APInt(new long[getNumWords(width)], width);

        int i;
        long word = 0;
        for (i = 0; i != bitWidth / APINT_BITS_PER_WORD; ++i)
        {
            word = getRawData()[i];
            result.pVal[i] = word;
        }

        int bits = (0 - bitWidth) % APINT_BITS_PER_WORD;
        if (bits != 0)
            word = getRawData()[i] << bits >> bits;
        else
            word = word >> (APINT_BITS_PER_WORD - 1);

        for (; i != width / APINT_BITS_PER_WORD; ++i)
        {
            result.pVal[i] = word;
            word = word >> (APINT_BITS_PER_WORD - 1);
        }

        bits = (0 - width) % APINT_BITS_PER_WORD;
        if (bits != 0)
            result.pVal[i] = word << bits >> bits;
        return result;
    }

    public APInt trunc(int width)
    {
        assert width < bitWidth : "Invalid APInt Truncate request";
        assert width > 0 : " Can't truncate to 0 bits";

        if (width <= APINT_BITS_PER_WORD)
            return new APInt(width, getRawData()[0]);

        APInt result = new APInt(new long[getNumWords(width)], width);
        int i;
        for (i = 0; i != width / APINT_BITS_PER_WORD; i++)
            result.pVal[i] = pVal[i];

        int bits = (0 - width) % APINT_BITS_PER_WORD;
        if (bits != 0)
            result.pVal[i] = pVal[i];
        return result.clearUnusedBits();
    }

    public APInt zextOrTrunc(int width)
    {
        if (bitWidth < width)
            return zext(width);
        if (bitWidth > width)
            return trunc(width);
        return this;
    }

    public APInt sextOrTrunc(int width)
    {
        if (bitWidth < width)
            return sext(width);
        if (bitWidth > width)
            return trunc(width);
        return this;
    }

    public long[] getRawData()
    {
        if (isSingleWord())
            return new long[] { val };
        else
            return pVal;
    }

    public boolean ult(final APInt rhs)
    {
        assert bitWidth == rhs.bitWidth : "Bit widths must be same for comparision";
        if (isSingleWord())
            return Util.ult(val, rhs.val);
        // Get active bit length of both operands
        int n1 = getActiveBits();
        int n2 = rhs.getActiveBits();

        // If magnitude of LHS is less than RHS, return true.
        if (n1 < n2)
            return true;

        // If magnitude of RHS is greather than LHS, return false.
        if (n1 > n2)
            return false;
        // reach here, the n1 must equal to n2.
        assert n1 == n2;

        // If they bot fit in a word, just compare the low order word
        if (n1 < APINT_BITS_PER_WORD)
            return Util.ult(pVal[0], rhs.pVal[0]);

        // Otherwise, compare all words
        int topWord = whichWord(n1 - 1);
        for (int i = topWord; i >= 0; i--)
        {
            if (Util.ult(pVal[i], rhs.pVal[i]))
                return true;
            if (Util.ugt(pVal[i], rhs.pVal[i]))
                return false;
        }
        return false;
    }

    public boolean ult(long rhs)
    {
        return ult(new APInt(getBitWidth(), rhs));
    }

    public boolean slt(final APInt rhs)
    {
        assert bitWidth
                == rhs.bitWidth : "Bit width must be same for comparison";
        if (isSingleWord())
        {
            return val < rhs.val;
        }

        boolean lhsNeg = isNegative();
        boolean rhsNeg = rhs.isNegative();

        if (lhsNeg)
        {
            // Sign bit is set so perform two's completion to make it positive.
            flip();
            increase();
        }

        if (rhsNeg)
        {
            // Sign bit is set so perform two's complement to make it positive
            rhs.flip();
            rhs.increase();
        }

        // Now we have  values to compare so do the comparison if necessary
        // based on the negativeness of the values.
        if (lhsNeg)
            if (rhsNeg)
                return ugt(rhs);
            else
                return true;
        else if (rhsNeg)
            return false;
        else
            return ult(rhs);
    }

    /**
     * Toggle every bit to its opposite.
     *
     * @return
     */
    private APInt flip()
    {
        if (isSingleWord())
        {
            val ^= -1L;
            return clearUnusedBits();
        }
        for (int i = 0; i < getNumWords(); i++)
            pVal[i] ^= -1L;
        return clearUnusedBits();
    }

    public boolean slt(long rhs)
    {
        return slt(new APInt(getBitWidth(), rhs));
    }

    public boolean ule(final APInt rhs)
    {
        return ult(rhs) || eq(rhs);
    }

    public boolean sle(long rhs)
    {
        return slt(rhs) || eq(rhs);
    }

    public boolean sle(final APInt rhs)
    {
        return slt(rhs) || eq(rhs);
    }

    public boolean ule(long rhs)
    {
        return ult(rhs) || eq(rhs);
    }

    public boolean ugt(final APInt rhs)
    {
        return !ult(rhs) && !eq(rhs);
    }

    public boolean ugt(long rhs)
    {
        return ugt(new APInt(getBitWidth(), rhs));
    }

    public boolean sgt(final APInt rhs)
    {
        return !slt(rhs) && !eq(rhs);
    }

    public boolean sgt(long rhs)
    {
        return sgt(new APInt(getBitWidth(), rhs));
    }

    public boolean uge(final APInt rhs)
    {
        return !ult(rhs);
    }

    public boolean uge(long rhs)
    {
        return uge(new APInt(getBitWidth(), rhs));
    }

    public boolean sge(final APInt rhs)
    {
        return !slt(rhs);
    }

    public boolean sge(long rhs)
    {
        return sge(new APInt(getBitWidth(), rhs));
    }

    public boolean eq(long val)
    {
        if (isSingleWord())
            return val == this.val;
        return equalSlowCase(val);
    }

    /**
     * Compares this APInt with {@code rhs} for the validity of the equality
     * relationship.
     *
     * @param rhs
     * @return
     */
    public boolean eq(final APInt rhs)
    {
        assert bitWidth == rhs.bitWidth : "Comparison requires equal bit widths";
        if (isSingleWord())
            return val == rhs.val;
        return equalSlowCase(rhs);
    }

    public boolean ne(APInt rhs)
    {
        return !eq(rhs);
    }

    public boolean ne(long rhs)
    {
        return !eq(rhs);
    }

    public String toString(int radix)
    {
        return toString(radix, true);
    }

    public void toString(StringBuilder buffer,
            int radix,
            boolean isSigned,
            boolean formatAsCLiteral)
    {
        assert radix == 10 || radix == 8 || radix == 16 || radix == 2
                || radix == 36 : "radix sholud be 2, 4, 8, 16!";

        String prefix = null;
        if (formatAsCLiteral)
        {
            switch (radix)
            {
                case 2:
                    prefix = "0b";
                    break;
                case 8:
                    prefix = "0";
                    break;
                case 16:
                    prefix = "0x";
                    break;
            }
        }

        int idx = 0, size = prefix.length();
        if (eq(0))
        {
            for (; idx < size; idx++)
                buffer.append(prefix.charAt(idx));
            buffer.append('0');
            return;
        }
        final String digits = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        if (isSingleWord())
        {
            char[] buf = new char[65];
            int bufLen = buf.length;

            long n;
            if (!isSigned)
                n = getZExtValue();
            else
            {
                long i = getSExtValue();
                if (i >= 0)
                    n = i;
                else
                {
                    buffer.append('-');
                    n = -i;
                }
            }

            while (idx < size)
            {
                buffer.append(prefix.charAt(idx));
                idx++;
            }
            while (n != 0)
            {
                buf[--bufLen] = digits.charAt((int) n % radix);
                n /= radix;
            }
            buffer.append(buf, bufLen, 65 - bufLen);
            return;
        }

        APInt temp = new APInt(this);
        if (isSigned && isNegative())
        {
            temp.flip();
            temp.increase();
            buffer.append('-');
        }

        while (idx < size)
        {
            buffer.append(prefix.charAt(idx));
            ++idx;
        }
        int startDig = buffer.length();
        if (radix == 2 || radix == 8 || radix == 16)
        {
            long shiftAmt = (radix == 16 ? 4 : (radix == 8 ? 3 : 1));
            long maskAmt = radix - 1;
            while (!temp.eq(0))
            {
                long digit = temp.getRawData()[0] & maskAmt;
                buffer.append(digits.charAt((int) digit));
                temp = temp.lshr(shiftAmt);
            }
        }
        else
        {
            APInt divisor = new APInt(radix == 10 ? 4 : 8, radix);
            while (!temp.eq(0))
            {
                APInt apDigit = new APInt(1, 0);
                APInt temp2 = new APInt(temp.getBitWidth(), 0);
                OutParamWrapper<APInt> x = new OutParamWrapper<>(apDigit);
                OutParamWrapper<APInt> y = new OutParamWrapper<>(temp2);

                divide(temp, temp.getNumWords(), divisor, divisor.getNumWords(),
                        x, y);
                apDigit = x.get();
                temp2 = y.get();
                long digit = apDigit.getZExtValue();
                assert digit < radix : "div failure";
                buffer.append(digits.charAt((int) digit));
                temp = temp2;
            }
        }

        for (int i = startDig, j = buffer.length() - 1; i < j; )
        {
            char t = buffer.charAt(i);
            buffer.setCharAt(i, buffer.charAt(j));
            buffer.setCharAt(j, t);
            i++;
            j--;
        }
    }

    public String toString(int radix, boolean isSigned)
    {
        StringBuilder sb = new StringBuilder();
        toString(sb, radix, isSigned, false);
        return sb.toString();
    }

    /**
     * Check if this APInt has an N-bits  integer value.
     *
     * @param N
     * @return
     */
    public boolean isIntN(int N)
    {
        assert N > 0;
        if (N >= getBitWidth())
            return true;

        if (isSingleWord())
            return isUIntN(N, val);
        return new APInt(N, getNumWords(), pVal).zext(getBitWidth()).eq(this);
    }

    /**
     * This method count the number of bits from the most significant bit
     * to the first one bit.
     *
     * @return
     */
    public int countLeadingZeros()
    {
        if (isSingleWord())
        {
            int unusedBits = APINT_BITS_PER_WORD - bitWidth;
            return Util.countLeadingZeros64(val) - unusedBits;
        }
        return countLeadingZeroSlowCase();
    }

    private int countLeadingZeroSlowCase()
    {
        int count = 0;
        for (int i = getNumWords() - 1; i >= 0; i--)
        {
            if (pVal[i] == 0)
                count += APINT_BITS_PER_WORD;
            else
            {
                count += Util.countLeadingZeros64(pVal[i]);
                break;
            }
        }
        int remainder = bitWidth % APINT_BITS_PER_WORD;
        if (remainder != 0)
            count -= APINT_BITS_PER_WORD - remainder;
        return Math.min(count, bitWidth);
    }

    /**
     * This method counts the number of ones from the significant bit
     * to the first zero bit.
     *
     * @return
     */
    public int countLeadingOnes()
    {
        if (isSingleWord())
        {
            return countLeadingOnes64(val, APINT_BITS_PER_WORD - bitWidth);
        }

        int highWordBits = bitWidth % APINT_BITS_PER_WORD;
        int shift;
        if (highWordBits == 0)
        {
            highWordBits = APINT_BITS_PER_WORD;
            shift = 0;
        }
        else
        {
            shift = APINT_BITS_PER_WORD - highWordBits;
        }

        int i = getNumWords() - 1;
        int count = countLeadingOnes64(pVal[i], shift);
        if (count == highWordBits)
        {
            for (i--; i >= 0; i--)
            {
                if (pVal[i] == -1)
                    count += APINT_BITS_PER_WORD;
                else
                {
                    count += countLeadingOnes64(pVal[i], 0);
                    break;
                }
            }
        }
        return count;
    }

    private int countLeadingOnes64(long val, int skipped)
    {
        int count = 0;
        if (skipped != 0)
            val <<= skipped;
        while ((val != 0) && ((val & 1L << 63) != 0))
        {
            count++;
            val <<= 1;
        }
        return count;
    }

    /**
     * Check if this APInt has an N-bits signed integer value.
     *
     * @param N
     * @return
     */
    public boolean isSignedIntN(int N)
    {
        assert N > 0;
        return getMinSignedBits() <= N;
    }

    public int getActiveBits()
    {
        return bitWidth - countLeadingZeros();
    }

    public int getActiveWords()
    {
        return whichWord(getActiveBits() - 1) + 1;
    }

    /**
     * Computes the minimum bit width for this APInt while considering it to be
     * /// a signed (and probably negative) value. If the value is not negative,
     * /// this function returns the same value as getActiveBits()+1. Otherwise, it
     * /// returns the smallest bit width that will retain the negative value. For
     * /// example, -1 can be written as 0b1 or 0xFFFFFFFFFF. 0b1 is shorter and so
     * /// for -1, this function will always return 1.
     *
     * @return
     */
    public int getMinSignedBits()
    {
        if (isNegative())
            return bitWidth - countLeadingOnes() - 1;
        return getActiveBits() + 1;

    }

    /**
     * This checks to see if the value of this APInt is the minimum signed
     * value for the APInt's bit width.
     */
    public boolean isMinSignedValue()
    {
        return bitWidth == 1 ? val == 1 : isNegative();
    }

    /**
     * This method check to see if the value of this APInt is the minimum 
     * value for the APInt's bit width.
     *
     * @return
     */
    public boolean isMinValue()
    {
        return countPopulation() == 0;
    }

    /**
     * This method is an APInt version of the countPopulation32 or 64.
     * <p>
     * It counts the number of one bit in APInt value.
     *
     * @return
     */
    private int countPopulation()
    {
        if (isSingleWord())
            return countPopulation_64(val);
        return countPopulationSlowCase();
    }

    /**
     * This method checks to see if this APInt is the maximum  value.
     *
     * @return
     */
    public boolean isMaxValue()
    {
        return countPopulation() == bitWidth;
    }

    /**
     * This method checks to see if this APInt is the maximum signed value.
     *
     * @return
     */
    public boolean isMaxSignedValue()
    {
        return bitWidth == 1 ? val == 1 :
                (!isNegative() && countPopulation() == bitWidth - 1);
    }

    /**
     * Checks if this number is the power of 2.
     *
     * @return
     */
    public boolean isPowerOf2()
    {
        if (isSingleWord())
            return isPowerOf2_64(val);
        return countPopulationSlowCase() == 1;
    }

    public boolean isPowerOf2_64(long val)
    {
        return (val & (val - 1)) == 0;
    }

    /**
     * this function counts the number of one bits in a value,
     * (64 bit edition.)
     *
     * @param val
     * @return
     */
    public int countPopulation_64(long val)
    {
        long v = val - ((val >> 1) & 0x5555555555555555L);
        v = (v & 0x3333333333333333L) + ((v >> 2) & 0x3333333333333333L);
        v = (v + (v >> 4)) & 0x0F0F0F0F0F0F0F0FL;

        return (int) ((v * 0x0101010101010101L) >> 56);
    }

    public int countPopulationSlowCase()
    {
        int count = 0;
        for (int i = 0; i < getNumWords(); ++i)
        {
            count += countPopulation_64(pVal[i]);
        }
        return count;
    }

    /**
     * Checks if an  integer fits into the given (dynamic)
     * bit width.
     *
     * @param N
     * @param val
     * @return
     */
    public boolean isUIntN(int N, long val)
    {
        return val == (val & (~0 >> (64 - N)));
    }

    /**
     * Checks if an signed integer fits into the given (dynamic)
     * bit width.
     *
     * @param N
     * @param val
     * @return
     */
    public boolean isIntN(int N, long val)
    {
        return val == (val & (~0 >> (64 - N)));
    }

    public boolean get(int bitPosition)
    {
        assert bitPosition < getBitWidth() : "Bit position out of bounds!";
        return (maskBit(bitPosition) & (isSingleWord() ? val : pVal[whichWord(bitPosition)])) != 0;
    }

    public long getSExtValue()
    {
        if (isSingleWord())
            return (val << ((APINT_BITS_PER_WORD - bitWidth))) >> (
                    APINT_BITS_PER_WORD - bitWidth);
        assert getMinSignedBits() <= 64 : "Too many bits for long";
        return pVal[0];
    }

    public long getZExtValue()
    {
        if (isSingleWord())
            return val;
        assert getActiveBits() <= 64 : "Too many bits for long";
        return pVal[0];
    }

    public long getLimitedValue(int limit)
    {
        return getActiveBits() > 64 || getZExtValue() > limit ?
                limit :
                getZExtValue();
    }

    /**
     * Performs arithmetic shift-right operation by {@code shiftAmt}.
     *
     * @param shiftAmt
     * @return
     */
    public APInt ashr(long shiftAmt)
    {
        assert shiftAmt <= bitWidth : "Invalid shift amount";
        if (shiftAmt == 0)
            return this;

        // Handle single word shifts with built-in ashr.
        if (isSingleWord())
        {
            if (shiftAmt == bitWidth)
                return new APInt(bitWidth, 0); // undefined behavior.
            else
            {
                int signBit = APINT_BITS_PER_WORD - bitWidth;
                return new APInt(bitWidth,
                        (val << signBit >> signBit) >> shiftAmt);
            }
        }

        // If all the bits were shifted out, the result is, technically, undefined.
        // We return -1 if it was negative, 0 otherwise. We check this early to avoid
        // issues in the algorithm below.
        if (shiftAmt == bitWidth)
        {
            if (isNegative())
                return new APInt(bitWidth, -1L, true);
            else
                return new APInt(bitWidth, 0);
        }

        // create some space for the result.
        long[] valPtr = new long[getNumWords()];

        int wordShift = (int) shiftAmt % APINT_BITS_PER_WORD;
        int offset = (int) shiftAmt / APINT_BITS_PER_WORD;
        int breakWord = getNumWords() - 1 - offset;
        int bitsInWord = (int) whichBit(bitWidth); // how many bits in last word?

        if (bitsInWord == 0)
            bitsInWord = APINT_BITS_PER_WORD;

        if (wordShift == 0)
        {
            for (int i = 0; i <= breakWord; i++)
            {
                valPtr[i] = pVal[i + offset];
            }

            if (isNegative())
                if (bitsInWord < APINT_BITS_PER_WORD)
                    valPtr[breakWord] |= ~0l << bitsInWord;
        }
        else
        {
            for (int i = 0; i < breakWord; i++)
            {
                valPtr[i] =
                        pVal[i + offset] >> wordShift | (pVal[i + offset + 1] << (APINT_BITS_PER_WORD - wordShift));
                ;
            }

            valPtr[breakWord] = pVal[breakWord + offset] >> wordShift;

            if (isNegative())
            {
                if (wordShift > bitsInWord)
                {
                    if (breakWord > 0)
                        valPtr[breakWord - 1] |=
                                ~0L << (APINT_BITS_PER_WORD - wordShift + bitsInWord);
                }
                else
                {
                    valPtr[breakWord] |= (~0L << (bitsInWord - wordShift));
                }
            }
        }

        // Remaining words are 0 or -1, just assign them.
        long fillValue = (isNegative()) ? -1L : 0;
        for (int i = breakWord + 1; i < getNumWords(); i++)
            valPtr[i] = fillValue;

        return new APInt(valPtr, bitWidth).clearUnusedBits();
    }

    public APInt ashr(final APInt shiftAmt)
    {
        return ashr((int) shiftAmt.getLimitedValue(bitWidth));
    }

    /**
     * Logical right-shift this APInt by shiftAmt.
     *
     * @param shiftAmt
     * @return
     */
    public APInt lshr(long shiftAmt)
    {
        if (isSingleWord())
        {
            if (shiftAmt == bitWidth)
                return new APInt(bitWidth, 0);
            else
                return new APInt(bitWidth, val >> shiftAmt);
        }

        // If all the bits were shifted out, the result is 0. This avoids issues
        // with shifting by the getNumOfSubLoop of the integer type, which produces undefined
        // results. We define these "undefined results" to always be 0.
        if (shiftAmt == bitWidth)
            return new APInt(bitWidth, 0);

        // If none of the bits are shifted out, the result is *this. This avoids
        // issues with shifting by the getNumOfSubLoop of the integer type, which produces
        // undefined results in the code below. This is also an optimization.
        if (shiftAmt == 0)
            return this;

        long[] valPtr = new long[getNumWords()];

        // If we are shifting less than a word, compute the shift with a simple carry
        if (shiftAmt < APINT_BITS_PER_WORD)
        {
            long carray = 0;
            for (int i = getNumWords() - 1; i >= 0; i--)
            {
                valPtr[i] = (pVal[i] >> shiftAmt) | carray;
                carray = pVal[i] << (APINT_BITS_PER_WORD - shiftAmt);
            }

            return new APInt(valPtr, bitWidth).clearUnusedBits();
        }

        // Compute some values needed by the remaining shift algorithms
        int wordShift = (int) shiftAmt % APINT_BITS_PER_WORD;
        int offset = (int) shiftAmt / APINT_BITS_PER_WORD;

        if (wordShift == 0)
        {
            for (int i = 0; i < getNumWords() - offset; i++)
                valPtr[i] = pVal[i + offset];
            for (int i = getNumWords() - offset; i < getNumWords(); i++)
                valPtr[i] = 0;
            return new APInt(valPtr, bitWidth).clearUnusedBits();
        }

        // Shift the low order words
        int breakWord = getNumWords() - offset - 1;
        for (int i = 0; i < breakWord; i++)
        {
            valPtr[i] =
                    (pVal[i + offset] >> wordShift) | (pVal[i + offset + 1] << (
                            APINT_BITS_PER_WORD - wordShift));
        }

        // Shift the break word.
        valPtr[breakWord] = pVal[breakWord + offset] >> wordShift;

        // Remaining words are 0.
        for (int i = breakWord + 1; i < getNumWords(); i++)
            valPtr[i] = 0;

        return new APInt(valPtr, bitWidth).clearUnusedBits();
    }

    public APInt lshr(final APInt shiftAmt)
    {
        return lshr((int) shiftAmt.getLimitedValue(bitWidth));
    }

    public APInt shl(final APInt shiftAmt)
    {
        return shl((int) shiftAmt.getLimitedValue(bitWidth));
    }

    /**
     * Left-shit this APInt by {@code shiftAmt}.
     *
     * @param shiftAmt
     * @return
     */
    public APInt shl(int shiftAmt)
    {
        assert shiftAmt <= bitWidth : "Invalid shift amounts";
        if (isSingleWord())
        {
            if (shiftAmt == bitWidth)
                return new APInt(bitWidth, 0); // avoid undefined behavior.
            return new APInt(bitWidth, val << shiftAmt);
        }
        return shlSlowCase(shiftAmt);
    }

    /**
     * Arithmetic right-shift the APInt by shiftAmt.
     *
     * @param lhs
     * @param shiftAmt
     * @return
     */
    public static APInt ashr(final APInt lhs, int shiftAmt)
    {
        return lhs.ashr(shiftAmt);
    }

    public boolean getBoolValue()
    {
        return this != null;
    }

    public APInt set()
    {
        if (isSingleWord())
        {
            val = -1L;
            return clearUnusedBits();
        }
        for (int i = 0; i < getNumWords(); i++)
            pVal[i] = -1l;
        return clearUnusedBits();
    }

    public APInt set(int bitPosition)
    {
        if (isSingleWord())
        {
            val |= maskBit(bitPosition);
        }
        else
            pVal[whichWord(bitPosition)] |= maskBit(bitPosition);
        return this;
    }

    public APInt clear()
    {
        if (isSingleWord())
        {
            val = 0L;
            return clearUnusedBits();
        }
        for (int i = 0; i < getNumWords(); i++)
            pVal[i] = 0L;
        return clearUnusedBits();
    }

    public APInt clear(int bitPosition)
    {
        if (isSingleWord())
        {
            val &= ~maskBit(bitPosition);
        }
        else
            pVal[whichWord(bitPosition)] &= ~maskBit(bitPosition);
        return this;
    }

    public static APInt getMaxValue(int numBits)
    {
        return new APInt(numBits, 0).set();
    }

    public static APInt getSignedMaxValue(int numBits)
    {
        return new APInt(numBits, 0).clear(numBits - 1);
    }

    public static APInt getMinValue(int numBits)
    {
        return new APInt(numBits, 0);
    }

    public static APInt getSignedMinValue(int numBits)
    {
        return new APInt(numBits, 0).set(numBits - 1);
    }

    public static APInt getSignBit(int bitwidth)
    {
        return getSignedMinValue(bitwidth);
    }

    public static APInt getAllOnesValue(int numBits)
    {
        return new APInt(numBits, 0).set();
    }

    public static APInt getNullValue(int numBits)
    {
        return new APInt(numBits, 0);
    }

    /**
     * Performs a bitwise complement operation on this APInt value.
     *
     * @return
     */
    public APInt not()
    {
        APInt result = new APInt(this);
        result.flip();
        return result;
    }

    /**
     * unary logical not operation.
     * @return
     */
    public boolean lNot()
    {
        if (isSingleWord())
            return val == 0;

        for (int i = 0; i < getNumWords(); i++)
            if (pVal[i] != 0)
                return false;

        return true;
    }

    public APInt add(final APInt rhs)
    {
        assert bitWidth == rhs.bitWidth : "bit width must be same!";

        if (isSingleWord())
            return new APInt(bitWidth, val + rhs.val);

        APInt result = new APInt(bitWidth, 0);
        add(result.pVal, pVal, rhs.pVal, getNumWords());
        return result.clearUnusedBits();
    }

    public APInt add(long rhs)
    {
        return add(new APInt(bitWidth, rhs));
    }

    public static boolean add(long[] dest, long[] x, long[] y, int len)
    {
        boolean carray = false;
        for (int i = 0; i < len; i++)
        {
            long temp = Math.min(x[i], y[i]);
            dest[i] = x[i] + y[i] + (carray ? 1 : 0);
            carray = dest[i] < temp || (carray && dest[i] == temp);
        }
        return carray;
    }

    /**
     * Subtracts the integer array y from the integer array x
     *
     * @param dest
     * @param x
     * @param y
     * @param len
     * @return return the borrow out.
     */
    public static boolean sub(long[] dest, long[] x, long[] y, int len)
    {
        boolean borrow = false;
        for (int i = 0; i < len; i++)
        {
            long temp = borrow ? x[i] - 1 : x[i];
            borrow = y[i] > temp || (borrow && x[i] == 0);
            dest[i] = temp - y[i];
        }
        return borrow;
    }

    public APInt sub(final APInt rhs)
    {
        assert bitWidth == rhs.bitWidth : "Bit width must be same!";

        if (isSingleWord())
            return new APInt(bitWidth, val - rhs.val);
        APInt result = new APInt(bitWidth, 0);
        sub(result.pVal, pVal, rhs.pVal, getNumWords());
        return result.clearUnusedBits();
    }

    public APInt sub(long rhs)
    {
        return sub(new APInt(bitWidth, rhs));
    }

    /**
     * Multiplies an integer array, x by a a uint64_t integer and places the result
     * into dest.
     *
     * @param dest
     * @param x
     * @param len
     * @param y
     * @return
     */
    public static long mul1(long[] dest, long[] x, int len, long y)
    {
        // Split y into high 32-bit part (hy)  and low 32-bit part (ly)
        long ly = y & 0xffffffffL, hy = y >>> 32;
        long carry = 0;

        // For each digit of x.
        for (int i = 0; i < len; i++)
        {
            // Split x into high and low words
            long lx = x[i] & 0xffffffffL;
            long hx = x[i] >>> 32;

            // hasCarry - A flag to indicate if there is a carry to the next digit.
            // hasCarry == 0, no carry
            // hasCarry == 1, has carry
            // hasCarry == 2, no carry and the calculation result == 0.
            int hasCarry = 0;
            dest[i] = carry + lx * ly;

            // Determine if the add above introduces carry.
            hasCarry = (dest[i] < carry) ? 1 : 0;
            carry = hx * hy + (dest[i] >>> 32) + (hasCarry != 0 ? (1 << 32) : 0);

            // The upper limit of carry can be (2^32 - 1)(2^32 - 1) +
            // (2^32 - 1) + 2^32 = 2^64.
            hasCarry = (carry == 0 && hasCarry != 0) ? 1 : (carry == 0 ? 2 : 0);

            carry += (lx * ly) * 0xffffffffL;
            dest[i] = (carry << 32) | (dest[i] & 0xffffffffL);
            carry = (((carry == 0 && hasCarry != 2) || hasCarry == 1) ?
                    (1L << 32) :
                    0) + (carry >> 32) + ((lx * hy) >> 32) + hx * hy;
        }
        return carry;
    }

    /**
     * Multiplies integer array x by integer array y and stores the result into
     * the integer array dest. Note that dest's getNumOfSubLoop must be >= xlen + ylen.
     *
     * @param dest
     * @param x
     * @param lenX
     * @param y
     * @param lenY
     */
    public static void mul(long[] dest, long[] x, int lenX, long[] y, int lenY)
    {
        assert dest.length >= lenX + lenY;
        dest[lenX] = mul1(dest, x, lenX, y[0]);

        for (int i = 1; i < lenY; i++)
        {
            long ly = y[i] & 0xffffffffL, hy = y[i] >>> 32;
            long carry = 0, lx = 0, hx = 0;

            for (int j = 0; j < lenY; j++)
            {
                lx = x[j] & 0xffffffffL;
                hx = x[j] >>> 32;

                // hasCarry - A flag to indicate if has carry.
                // hasCarry == 0, no carry
                // hasCarry == 1, has carry
                // hasCarry == 2, no carry and the calculation result == 0.
                int hasCarry = 0;
                long result = carry + lx * ly;
                hasCarry = (result < carry) ? 1 : 0;
                carry = (hasCarry != 0 ? (1L << 32) : 0);
                hasCarry = (carry == 0 && hasCarry != 0) ?
                        1 :
                        (carry == 0 ? 2 : 0);

                carry += (lx * hy) & 0xffffffffL;
                result = (carry << 32) | (result & 0xffffffffL);
                dest[i + j] = result;
                carry = (((carry == 0 && hasCarry != 2) || hasCarry == 1) ?
                        (1L << 32) :
                        0) + (carry >> 32) + (dest[i + j] < result ? 1 : 0) + (
                        (lx * hy) >> 32) + hx * hy;
            }
            dest[i + lenX] = carry;
        }
    }

    public APInt mul(final APInt rhs)
    {
        assert bitWidth == rhs.bitWidth : "Bitwidth must be same!";
        if (isSingleWord())
            return new APInt(bitWidth, val * rhs.val);
        APInt result = new APInt(bitWidth, 0);
        result.mulAssign(rhs);
        return result.clearUnusedBits();
    }

    public APInt mul(long rhs)
    {
        return mul(new APInt(bitWidth, rhs));
    }

    /**
     * Performs &= operation on this APInt value and rhs.
     *
     * @param rhs
     * @return
     */
    public APInt andAssign(final APInt rhs)
    {
        assert bitWidth == rhs.bitWidth : "Bit width must be same!";
        if (isSingleWord())
        {
            val &= rhs.val;
            return this;
        }

        int numWords = getNumWords();
        for (int i = 0; i < numWords; i++)
            pVal[i] &= rhs.pVal[i];
        return this;
    }

    /**
     * Performs |= operation on this APInt value and rhs.
     *
     * @param rhs
     * @return
     */
    public APInt orAssign(final APInt rhs)
    {
        assert bitWidth == rhs.bitWidth : "Bit width must be same!";
        if (isSingleWord())
        {
            val |= rhs.val;
            return this;
        }

        int numWords = getNumWords();
        for (int i = 0; i < numWords; i++)
            pVal[i] |= rhs.pVal[i];
        return this;
    }

    /**
     * Performs ^= operation on this APInt value and rhs.
     *
     * @param rhs
     * @return
     */
    public APInt xorAssign(final APInt rhs)
    {
        assert bitWidth == rhs.bitWidth : "Bit width must be same!";
        if (isSingleWord())
        {
            val ^= rhs.val;
            return this;
        }

        int numWords = getNumWords();
        for (int i = 0; i < numWords; i++)
            pVal[i] ^= rhs.pVal[i];
        return this;
    }

    public APInt addAssign(final APInt rhs)
    {
        assert bitWidth == rhs.bitWidth : "Bit width must be same!";

        if (isSingleWord())
        {
            val += rhs.val;
            return this;
        }

        int lhsBits = getActiveBits();
        int lhsWords = lhsBits == 0 ? 0 : whichWord(lhsBits - 1) + 1;
        if (lhsWords == 0)
            //　0 + X = X;
            return this;

        int rhsBits = rhs.getActiveBits();
        int rhsWords = rhsBits == 0 ? 0 : whichWord(rhsBits - 1) + 1;
        if (rhsWords == 0)
            //X + 0 = X;
            return this;

        add(pVal, pVal, rhs.pVal, getNumWords());
        return clearUnusedBits();
    }

    public APInt subAssign(final APInt rhs)
    {
        assert bitWidth == rhs.bitWidth : "Bit width must be same!";

        if (isSingleWord())
            val -= rhs.val;
        else
            sub(pVal, pVal, rhs.pVal, getNumWords());
        return clearUnusedBits();
    }

    public APInt shlAssign(int shiftAmt)
    {
        shl(shiftAmt);
        return this;
    }

    public APInt ashrAssign(int shiftAmt)
    {
        ashr(shiftAmt);
        return this;
    }

    public APInt lshrAssign(int shiftAmt)
    {
        lshr(shiftAmt);
        return this;
    }

    /**
     * Performs *= operation on this APInt value and rhs.
     *
     * @param rhs
     * @return
     */
    public APInt mulAssign(final APInt rhs)
    {
        assert bitWidth == rhs.bitWidth : "Bit width must be same!";
        if (isSingleWord())
        {
            val *= rhs.val;
            clearUnusedBits();
            return this;
        }

        int lhsBits = getActiveBits();
        int lhsWords = lhsBits == 0 ? 0 : whichWord(lhsBits - 1) + 1;
        if (lhsWords == 0)
            // 0 * x = 0.
            return this;

        int rhsBits = getActiveBits();
        int rhsWords = rhsBits == 0 ? 0 : whichWord(rhsBits - 1) + 1;
        if (rhsWords == 0)
            // X * 0 = 0.
            return this;

        int destWords = rhsWords + lhsWords;
        long[] dest = new long[destWords];

        mul(dest, pVal, lhsWords, rhs.pVal, rhsWords);

        clear();
        int wordsToCopy = destWords >= getNumWords() ? getNumWords() : destWords;
        System.arraycopy(dest, 0, pVal, 0, wordsToCopy);
        dest = null;
        return this;
    }

    /**
     * Perform an  div operation on this APInt by RHS. Both this and
     * RHS are treated as  quantities for purposes of this division.
     *
     * @param rhs
     * @return a new APInt value containing the division result
     */
    public APInt udiv(final APInt rhs)
    {
        assert bitWidth == rhs.bitWidth : "Bit width must be same!";

        // first, deal with the easy case.
        if (isSingleWord())
        {
            assert rhs.val != 0 : "Divide by zero?";
            return new APInt(bitWidth, val / rhs.val);
        }

        // Get some facts about the LHS and RHS number of bits and words
        int rhsBits = rhs.getActiveBits();
        int rhsWords = rhsBits == 0 ? 0 : whichWord(rhsBits - 1) + 1;
        assert rhsWords != 0 : "div by zeror?";

        int lhsBits = getActiveBits();
        int lhsWords = lhsBits == 0 ? 0 : whichWord(lhsBits - 1) + 1;

        if (lhsWords == 0)
            // 0 / X = 0
            return new APInt(bitWidth, 0);
        else if (lhsWords < rhsWords || ult(rhs))
        {
            // X/Y ===> 0, if and only X < Y.
            return new APInt(bitWidth, 0);
        }
        else if (this.eq(rhs))
        {
            // X == Y ===> 1.
            return new APInt(bitWidth, 1);
        }
        else if (lhsWords == 1 && rhsWords == 1)
        {
            // All high words are zero, just use native div
            return new APInt(bitWidth, pVal[0] / rhs.pVal[0]);
        }

        // We have to compute it the hard way. Call the Knuth div algorithm.
        APInt quotient = new APInt(1, 0);
        OutParamWrapper<APInt> x = new OutParamWrapper<>(quotient);
        divide(this, lhsWords, rhs, rhsWords, x, null);
        return x.get();
    }

    /**
     * Signed div this APInt by APInt RHS.
     *
     * @param rhs
     * @return
     */
    public APInt sdiv(final APInt rhs)
    {
        if (isNegative())
            if (rhs.isNegative())
                return negative().udiv(rhs.negative());
            else
                return negative().udiv(rhs).negative();
        else if (rhs.isNegative())
            return udiv(rhs.negative()).negative();
        return udiv(rhs);
    }

    /**
     * This is used by the toString method to div by the radix. It simply
     * provides a more convenient form of div for internal use since KnuthDiv
     * has specific constraints on its inputs. If those constraints are not met
     * then it provides a simpler form of div.
     *
     * @param lhs
     * @param lhsWords
     * @param rhs
     * @param rhsWords
     * @param quotient
     * @param remainder
     */
    public static void divide(final APInt lhs, int lhsWords, final APInt rhs,
            int rhsWords, OutParamWrapper<APInt> quotient,
            OutParamWrapper<APInt> remainder)
    {
        assert lhsWords >= rhsWords : "Fractional result";

        // First, compose the values into an array of 32-bit words instead of
        // 64-bit words. This is a necessity of both the "short division" algorithm
        // and the the Knuth "classical algorithm" which requires there to be native
        // operations for +, -, and * on an m bit value with an m*2 bit result. We
        // can't use 64-bit operands here because we don't have native results of
        // 128-bits. Furthermore, casting the 64-bit values to 32-bit values won't
        // work on large-endian machines.
        long mask = ~0L >> 32;
        int n = rhsWords * 2;
        int m = (lhsWords * 2) - n;

        // Allocate space for the temporary values we need either on the stack, if
        // it will fit, or on the heap if it won't.
        int space[] = new int[128];
        int u[] = null, v[] = null, q[] = null, r[] = null;

        if ((remainder.get().ne(0) ? 4 : 3) * n + 2 * m + 1 <= 128)
        {
            u = space;
            v = new int[128 - (m + n + 1)];
            System.arraycopy(space, (m + n + 1), v, 0, v.length);
            q = new int[128 - (m + n + 1 + n)];
            System.arraycopy(space, m + n + 1 + n, q, 0, q.length);
            if (remainder.get().ne(0))
            {
                r = new int[128 - ((m + n + 1) + n + (m + n))];
                System.arraycopy(space, (m + n + 1) + n + (m + n), r, 0, r.length);
            }
        }
        else
        {
            u = new int[m + n + 1];
            v = new int[n];
            q = new int[m + n];
            if (remainder.get().ne(0))
                r = new int[n];
        }

        // Initialize the dividend
        Arrays.fill(u, 0, u.length, 0);
        for (int i = 0; i < lhsWords; i++)
        {
            long temp = lhs.getNumWords() == 1 ? lhs.val : lhs.pVal[i];
            u[i * 2] = (int) (temp & mask);
            u[i * 2 + 1] = (int) (temp >> 32);
        }

        // this extra word is for "spill" in the Knuth algorithm.
        u[m + n] = 0;

        // Initialize the divisor
        Arrays.fill(v, 0, v.length, 0);
        for (int i = 0; i < rhsWords; i++)
        {
            long temp = rhs.getNumWords() == 1 ? rhs.val : rhs.pVal[i];
            u[i * 2] = (int) (temp & mask);
            u[i * 2 + 1] = (int) (temp >> 32);
        }

        // initialize the quotient and rem
        Arrays.fill(q, 0, q.length, 0);
        if (remainder.get() != null)
            Arrays.fill(r, 0, r.length, 0);

        // Now, adjust m and n for the Knuth division. n is the number of words in
        // the divisor. m is the number of words by which the dividend exceeds the
        // divisor (i.e. m+n is the length of the dividend). These sizes must not
        // contain any zero words or the Knuth algorithm fails.
        for (int i = n; i > 0 && v[i - 1] == 0; i--)
        {
            n--;
            m++;
        }

        for (int i = m + n; i > 0 && u[i - 1] == 0; i--)
            m--;

        // If we're left with only a single word for the divisor, Knuth doesn't work
        // so we implement the short division algorithm here. This is much simpler
        // and faster because we are certain that we can div a 64-bit quantity
        // by a 32-bit quantity at hardware speed and short division is simply a
        // series of such operations. This is just like doing short division but we
        // are using base 2^32 instead of base 10.
        assert n != 0 : "Divide by zero?";

        if (n == 1)
        {
            int divisor = v[0];
            int rem = 0;
            for (int i = m + n - 1; i >= 0; i--)
            {
                long partial_div = (long) rem << 32 | u[i];
                if (partial_div == 0)
                {
                    q[i] = 0;
                    rem = 0;
                }
                else if (partial_div < divisor)
                {
                    q[i] = 0;
                    rem = (int) partial_div;
                }
                else if (partial_div == divisor)
                {
                    q[i] = 1;
                    rem = 0;
                }
                else
                {
                    q[i] = (int) (partial_div / divisor);
                    rem = (int) (partial_div - q[i] * divisor);
                }
            }

            if (r != null)
                r[0] = rem;
        }
        else
        {
            // Now we're ready to invoke the Knuth classical div algorithm. In this
            // case n > 1.
            knuthDiv(u, v, q, r, m, n);
        }

        if (quotient != null)
        {
            // Set up the Quotient values's memory.
            if (quotient.get().bitWidth != lhs.bitWidth)
            {
                if (quotient.get().isSingleWord())
                    quotient.get().val = 0;
                else
                    quotient.get().pVal = null;
                quotient.get().bitWidth = lhs.bitWidth;

                if (!quotient.get().isSingleWord())
                    quotient.get().pVal = new long[quotient.get().getNumWords()];
            }
            else
                quotient.get().clear();

            // The quotient is in Q. Reconstitute the quotient into Quotient's low
            // order words.
            if (lhsWords == 1)
            {
                long temp = (long) q[0] | (((long) q[1]) << (APINT_BITS_PER_WORD
                        / 2));
                if (quotient.get().isSingleWord())
                    quotient.get().val = temp;
                else
                    quotient.get().pVal[0] = temp;
            }
            else
            {
                assert !quotient.get()
                        .isSingleWord() : "Quotient APInt not large enough";
                for (int i = 0; i < lhsWords; i++)
                    quotient.get().pVal[i] =
                            (long) (q[i * 2]) | ((long) (q[i * 2 + 1]) << (
                                    APINT_BITS_PER_WORD / 2));
            }
        }

        // If the caller wants the rem
        if (remainder != null)
        {
            // Set up the Remainder value's memory.
            if (remainder.get().bitWidth != rhs.bitWidth)
            {
                if (remainder.get().isSingleWord())
                    remainder.get().val = 0;
                else
                    remainder.get().pVal = null;
                remainder.get().bitWidth = rhs.bitWidth;
                if (!remainder.get().isSingleWord())
                    remainder.get().pVal = new long[remainder.get().getNumWords()];
            }
            else
            {
                remainder.get().clear();

                // The rem is in R. Reconstitute the rem into Remainder's low
                // order words.
                if (rhsWords == 1)
                {
                    long temp =
                            (long) r[0] | (long) r[1] << (APINT_BITS_PER_WORD / 2);
                    if (remainder.get().isSingleWord())
                        remainder.get().val = temp;
                    else
                        remainder.get().pVal[0] = temp;
                }
                else
                {
                    assert !remainder.get()
                            .isSingleWord() : "Remainder APInt not large enough";
                    for (int i = 0; i < rhsWords; i++)
                        remainder.get().pVal[i] =
                                (long) r[i * 2] | (long) r[2 * i + 1] << (
                                        APINT_BITS_PER_WORD / 2);
                }
            }
        }

        // clean up the memory.
        u = null;
        v = null;
        q = null;
        r = null;
    }

    /**
     * Implementation of Knuth's Algorithm D (Division of nonnegative integers)
     * from "Art of Computer Programming, Volume 2", section 4.3.1, p. 272. The
     * variables here have the same names as in the algorithm. Comments explain
     * the algorithm and any deviation from it.
     *
     * @param U
     * @param V
     * @param Q
     * @param R
     * @param m
     * @param n
     */
    private static void knuthDiv(int[] U, int[] V, int[] Q, int[] R, int m, int n)
    {
        assert U != null : "Must provide dividend!";
        assert V != null : "Must provide divisor!";
        assert Q != null : "Must provide quotient!";
        assert U != V && U != Q && V != Q : "Must use different memory";
        assert n > 1 : "n must be >1";

        // Knuth uses the value b as the base of the number system. In our case b
        // is 2^31 so we just set it to -1u.
        long b = (long) 1 << 32;

        // D1. [Normalize.] Set d = b / (v[n-1] + 1) and mul all the digits of
        // u and v by d. Note that we have taken Knuth's advice here to use a power
        // of 2 value for d such that d * v[n-1] >= b/2 (b is the base). A power of
        // 2 allows us to shift instead of mul and it is easy to determine the
        // shift amount from the leading zeros.  We are basically normalizing the u
        // and v so that its high bits are shifted to the top of v's range without
        // overflow. Note that this can require an extra word in u so that u must
        // be of length m+n+1.
        int shift = countLeadingZeros32(V[n - 1]);
        int vCarry = 0;
        int uCarry = 0;
        if (shift != 0)
        {
            for (int i = 0; i < m + n; i++)
            {
                int temp = U[i] >>> (32 - shift);
                U[i] = (U[i] << shift) | uCarry;
                uCarry = temp;
            }

            for (int i = 0; i < n; i++)
            {
                int temp = V[i] >>> (32 - shift);
                V[i] = (V[i] << shift) | vCarry;
                vCarry = temp;
            }
        }
        U[m + n] = uCarry;

        // D2. [Initialize j.]  Set j to m. This is the loop counter over the places.
        int j = m;
        do
        {
            // D3. [Calculate q'.].
            //     Set qp = (u[j+n]*b + u[j+n-1]) / v[n-1]. (qp=qprime=q')
            //     Set rp = (u[j+n]*b + u[j+n-1]) % v[n-1]. (rp=rprime=r')
            // Now test if qp == b or qp*v[n-2] > b*rp + u[j+n-2]; if so, decrease
            // qp by 1, inrease rp by v[n-1], and repeat this test if rp < b. The test
            // on v[n-2] determines at high speed most of the cases in which the trial
            // value qp is one too large, and it eliminates all cases where qp is two
            // too large.
            long dividend = ((long) U[j + n] << 32) + U[j + n - 1];
            long qp = dividend / V[n - 1];
            long rp = dividend % V[n - 1];
            if (qp == b || qp * V[n - 2] > b * rp + U[j + n - 2])
            {
                qp--;
                rp += V[n - 1];
                if (rp < b && (qp == b || qp * V[n - 2] > b * rp + U[j + n - 2]))
                    qp--;
            }

            // D4. [Multiply and subtract.] Replace (u[j+n]u[j+n-1]...u[j]) with
            // (u[j+n]u[j+n-1]..u[j]) - qp * (v[n-1]...v[1]v[0]). This computation
            // consists of a simple multiplication by a one-place number, combined with
            // a sub.
            boolean isNeg = false;
            for (int i = 0; i < n; i++)
            {
                long temp = (long) U[j + i] | (long) U[j + i + 1] << 32;
                long subtrahend = (long) qp * (long) V[i];
                boolean borrow = subtrahend > temp;

                long result = temp - subtrahend;
                int k = j + i;
                U[k++] = (int) (result & (b - 1));
                U[k++] = (int) (result >>> 32);
                while (borrow && k <= m + n)
                {
                    borrow = U[k] == 0;
                    U[k]--;
                    k++;
                }
                isNeg |= borrow;
            }
            // The digits (u[j+n]...u[j]) should be kept positive; if the result of
            // this step is actually negative, (u[j+n]...u[j]) should be left as the
            // true value plus b**(n+1), namely as the b's complement of
            // the true value, and a "borrow" to the left should be remembered.
            if (isNeg)
            {
                boolean carry = true;
                for (int i = 0; i < m + n; i++)
                {
                    U[i] = ~U[i] + (carry ? 1 : 0);
                    carry = carry && U[i] == 0;
                }
            }

            // D5. [Test rem.] Set q[j] = qp. If the result of step D4 was
            // negative, go to step D6; otherwise go on to step D7.
            Q[j] = (int) qp;
            if (isNeg)
            {
                // D6. [Add back]. The probability that this step is necessary is very
                // small, on the order of only 2/b. Make sure that test data accounts for
                // this possibility. Decrease q[j] by 1
                Q[j]--;
                // and add (0v[n-1]...v[1]v[0]) to (u[j+n]u[j+n-1]...u[j+1]u[j]).
                // A carry will occur to the left of u[j+n], and it should be ignored
                // since it cancels with the borrow that occurred in D4.
                boolean carry = false;
                for (int i = 0; i < n; i++)
                {
                    int limit = Math.min(U[j + i], V[i]);
                    U[j + i] += V[i] + (carry ? 1 : 0);
                    carry = U[j + i] < limit || (carry && U[j + i] == limit);
                }

                U[j + n] = carry ? 1 : 0;
            }

            // D7. [Loop on j.]  Decrease j by one. Now if j >= 0, go back to D3.
        } while (--j >= 0);

        // D8. [Unnormalize]. Now q[...] is the desired quotient, and the desired
        // rem may be obtained by dividing u[...] by d. If r is non-null we
        // compute the rem (urem uses this).
        if (R != null)
        {
            // The value d is expressed by the "shift" value above since we avoided
            // multiplication by d by using a shift left. So, all we have to do is
            // shift right here. In order to mak
            if (shift != 0)
            {
                int carry = 0;
                for (int i = n - 1; i >= 0; i--)
                {
                    R[i] = (U[i] >> shift) | carry;
                    carry = U[i] << (32 - shift);
                }
            }
            else
            {
                for (int i = n - 1; i >= 0; i--)
                {
                    R[i] = U[i];
                }
            }
        }
    }

    /**
     * Negates this using two's complement logic.
     *
     * @return
     */
    public APInt negative()
    {
        return new APInt(bitWidth, 0).sub(this);
    }

    /**
     * Perform an  rem operation on this APInt with RHS being the
     * divisor. Both this and RHS are treated as  quantities for purposes
     * of this operation. Note that this is a true rem operation and not
     * a modulo operation because the sign follows the sign of the dividend
     * which is *this.
     * @param rhs
     * @return
     */
    public APInt urem(final APInt rhs)
    {
        assert bitWidth == rhs.bitWidth : "Bit width must be same!";
        if (isSingleWord())
        {
            assert rhs.val != 0 : "Remainder by zero?";
            return new APInt(bitWidth, val % rhs.val);
        }

        int lhsBits = getActiveBits();
        int lhsWords = lhsBits == 0 ? 0 : whichWord(lhsBits - 1) + 1;

        int rhsBits = rhs.getActiveBits();
        int rhsWords = rhsBits == 0 ? 0 : whichWord(rhsBits - 1) + 1;
        assert rhsWords != 0 : "Performing rem operation by zero ???";

        if (lhsWords == 0)
        {
            // 0 % Y ==> 0.
            return new APInt(bitWidth, 0);
        }
        else if (lhsWords < rhsWords || ult(rhs))
        {
            // X % Y => X, iff X<Y.
            return this;
        }
        else if (this.eq(rhs))
        {
            // X % X = 0.
            return new APInt(bitWidth, 0);
        }
        else if (lhsWords == 1)
        {
            // All high words are zero, just use native rem
            return new APInt(bitWidth, pVal[0] % rhs.pVal[0]);
        }

        APInt rem = new APInt(1, 0);
        OutParamWrapper<APInt> x = new OutParamWrapper<>(rem);
        divide(this, lhsWords, rhs, rhsWords, null, x);
        return x.get();
    }

    /**
     * Signed rem operation on APInt.
     * @param rhs
     * @return
     */
    public APInt srem(final APInt rhs)
    {
        if (isNegative())
            if (rhs.isNegative())
                return negative().urem(rhs.negative()).negative();
            else
                return negative().urem(rhs).negative();
        else if (rhs.isNegative())
            return urem(rhs.negative());
        else
            return urem(rhs);
    }

    /**
     * Sometimes it is convenient to div two APInt values and obtain both the
     * quotient and rem. This function does both operations in the same
     * computation making it a little more efficient. The pair of input arguments
     * may overlap with the pair of output arguments. It is safe to call
     * udivrem(X, Y, X, Y), for example.
     * @param lhs
     * @param rhs
     * @param quotient
     * @param remainder
     */
    public static void udivrem(final APInt lhs,
            final APInt rhs,
            OutParamWrapper<APInt> quotient,
            OutParamWrapper<APInt> remainder)
    {
        int lhsBits = lhs.getActiveBits();
        int rhsBits = rhs.getActiveBits();

        int lhsWords = lhsBits == 0 ? 0 : whichWord(lhsBits - 1) + 1;
        int rhsWords = rhsBits == 0 ? 0 : whichWord(rhsBits - 1) + 1;

        if (lhsWords == 0)
        {
            // 0 / Y == 0.
            quotient.get().assign(0);
            // 0 % Y == 0.
            remainder.get().assign(0);
            return;
        }
        if (lhsWords < rhsWords || lhs.ult(rhs))
        {
            // X / Y == 0, iff X < Y.
            quotient.get().assign(0);
            // X % Y == X, iff X < Y.
            remainder.get().assign(lhs);
            return;
        }

        if (lhs.eq(rhs))
        {
            // Y / Y == 1.
            quotient.get().assign(1);
            // Y % Y == 0.
            remainder.get().assign(0);
            return;
        }

        if (lhsWords == 1 && rhsWords == 1)
        {
            // There is only one word to consider so use the native versions.
            long lhsValue = lhs.isSingleWord()?lhs.val:lhs.pVal[0];
            long rhsValue = rhs.isSingleWord()?rhs.val:rhs.pVal[0];

            quotient.get().assign(new APInt(lhs.getBitWidth(), lhsValue / rhsValue));
            remainder.get().assign(new APInt(lhs.getBitWidth(), lhsValue % rhsValue));
            return;
        }

        // Okay, lets do it the long way.
        divide(lhs, lhsWords, rhs, rhsWords, quotient, remainder);
    }

    public static void sdivrem(final APInt lhs,
            final APInt rhs,
            OutParamWrapper<APInt> quotient,
            OutParamWrapper<APInt> remainder)
    {
        if (lhs.isNegative())
        {
            if (rhs.isNegative())
                udivrem(lhs.negative(), rhs.negative(), quotient, remainder);
            else
                udivrem(lhs.negative(), rhs, quotient, remainder);
            quotient.set(quotient.get().negative());
            remainder.set(remainder.get().negative());
        }
        else if (rhs.isNegative())
        {
            udivrem(lhs, rhs.negative(), quotient, remainder);
            quotient.set(quotient.get().negative());
        }
        else
        {
            udivrem(lhs, rhs, quotient, remainder);
        }
    }

    public APInt and(final APInt rhs)
    {
        assert bitWidth == rhs.bitWidth:"Bit widths must be the same";
        if (isSingleWord())
            return new APInt(getBitWidth(), val & rhs.val);
        return andSlowCase(rhs);
    }

    public APInt and(long rhs)
    {
        return and(new APInt(bitWidth, rhs));
    }

    public APInt or(final APInt rhs)
    {
        assert bitWidth == rhs.bitWidth:"Bit widths must be the same";
        if (isSingleWord())
            return new APInt(getBitWidth(), val | rhs.val);
        return orSlowCase(rhs);
    }

    public APInt or(long rhs)
    {
        return or(new APInt(bitWidth, rhs));
    }

    public APInt xor(final APInt rhs)
    {
        assert bitWidth == rhs.bitWidth:"Bit widths must be the same";
        if (isSingleWord())
            return new APInt(getBitWidth(), val ^ rhs.val);
        return xorSlowCase(rhs);
    }

    public APInt xor(long rhs)
    {
        return xor(new APInt(bitWidth, rhs));
    }

	/**
     *  Constructs an APInt value that has the bottom loBitsSet bits set.
     * @return
     */
    public static APInt getLowBitsSet(int numBits, int loBitsSet)
    {
        assert loBitsSet <=numBits;
        if (loBitsSet == 0)
            return new APInt(numBits, 0);
        if (loBitsSet == APINT_BITS_PER_WORD)
            return new APInt(numBits, -1L);

        if (numBits < APINT_BITS_PER_WORD)
            return new APInt(numBits, 1L << loBitsSet);
        return new APInt(numBits, 0).lshr(numBits - loBitsSet);
    }

    /**
     * Constructs and returns a new APInt that has the top {@code hiBitsSet}
     * bits set.
     * @param numBits
     * @param hiBitsSet
     * @return
     */
    public static APInt getHighBitsSet(int numBits, int hiBitsSet)
    {
        assert hiBitsSet <= numBits :"Too many bits to set";
        if(hiBitsSet == 0)
            return new APInt(numBits, 0);
        int shiftAmt = numBits - hiBitsSet;
        // For the small number, performing the quickly operation.
        if (numBits <= APINT_BITS_PER_WORD)
            return new APInt(numBits, ~0L << shiftAmt);
        return new APInt(numBits, 0).negative().shl(shiftAmt);
    }

	/**
     * This checks whether all bits of this APInt are set or not.
     * @return
     */
    public boolean isAllOnesValue()
    {
        return countPopulation() == bitWidth;
    }

    /**
     * A table for computing the square root of the smaller number whose
     * active bits is less than 5bits.
     */
    private static final byte[] results =
    {
            /*    0 */0,
            /*  1-2 */1, 1,
            /*  3-6 */2, 2, 2, 2,
            /* 7-12 */3, 3, 3, 3, 3, 3,
            /* 13-2 */4, 4, 4, 4, 4, 4, 4, 4,
            /*21-30 */5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            /*    31*/6
    };
    /**
     * This method compute and return the square root of <b>this</b>.
     * Threr mechanisms are used for computation. For example, for less than 5bit,
     * a table lookup is done. This gets some performance for the common cases.
     * For the values using less than 52 bits, the value is converted to the double
     * and the libc sqrt function is called. The result is rounded and then converted
     * back to the long with is then used for constructing result. Finally the
     * Babylinian method for computing square root is usded.
     * @return
     */
    public APInt sqrt()
    {
        // Determine the magnitude of the value.
        int magnitude = getActiveBits();

        // Use the table lookup method for some small value.
        if (magnitude <= 5)
        {
            return new APInt(bitWidth, results[(int)(isSingleWord()?val:pVal[0])]);
        }

        // If the magnitude of the value fits in less than 52 bits (the precision of
        // an IEEE double precision floating point value), then we can use the
        // libc sqrt function which will probably use a hardware sqrt computation.
        // This should be faster than the algorithm below.
        if (magnitude < 52)
        {
            return new APInt(bitWidth, (long)Math.sqrt((double) (isSingleWord()?val:pVal[0])));
        }

        /// The following is a classical Newton iteration method for computing the
        // square root. This code was adapted to APINt from a wikipedia article
        // on such computations. See https://en.wikipedia.org/wiki/Integer_square_root
        // for more details.
        int bitwidth = getBitWidth(), i = 4;
        APInt testy = new APInt(bitwidth, 16);
        APInt x1 = new APInt(bitwidth, 1);  // the older value.
        APInt x2 = new APInt(bitwidth, 0);  // the newer value.
        APInt two = new APInt(bitwidth, 2);

        // Choise a good starting value using binary algorightmn.
        while(true)
        {
            if (i > bitwidth || ule(testy))
            {
                x1 = x1.shl(i/2);
                break;
            }
            i+=2;
            testy = testy.shl(2);
        }

        // Using the newton iteration method for computing square root.
        while(true)
        {
            // x2 = (x1 + n/x1)/2.
            x2 = x1.add(this.udiv(x1)).udiv(two);
            if (x1.ule(x2))
                break;
            x1 = x2;
        }

        // Make sure we return the closest approximation
        APInt square = new APInt(x1.mul(x1));
        APInt nextSquare = new APInt(x1.increase().mul(x1.increase()));
        if (this.ult(square))
            return x1;
        else if (this.ule(nextSquare))
        {
            APInt midPoint = new APInt(nextSquare.sub(square)).udiv(two);
            APInt offset = new APInt(this.sub(square));
            if (offset.ult(midPoint))
                return x1;
            else
                return x1.increase();
        }
        else
            Util.shouldNotReachHere("Error in public static sqrt method");
        return x1.increase();
    }

    public static APInt smax(APInt a, APInt b)
    {
        return a.sgt(b) ? a : b;
    }

    public static APInt smin(APInt a, APInt b)
    {
        return a.slt(b) ? a : b;
    }

    public static APInt umax(APInt a, APInt b)
    {
        return a.ugt(b) ? a : b;
    }

    public static APInt umin(APInt a, APInt b)
    {
        return a.ult(b) ? a : b;
    }

	/**
     * returns the floor log base 2 of this APInt.
     * @return
     */
    public long logBase2()
    {
        return bitWidth - 1 - countLeadingZeros();
    }

    public void print(PrintStream os)
    {
        print(os, true);
    }

    public void print(PrintStream os, boolean isSigned)
    {
        StringBuilder sb = new StringBuilder();
        toString(sb, 10, isSigned, true);
        os.print(sb.toString());
    }

    public boolean isStrictlyPositive()
    {
        return isNonNegative() && !eq(0);
    }

    /**
     * his function converts this APInt to a double.
     * The layout for double is as following (IEEE Standard 754):
     *  --------------------------------------
     * |  Sign    Exponent    Fraction    Bias |
     * |-------------------------------------- |
     * |  1[63]   11[62-52]   52[51-00]   1023 |
     *  --------------------------------------
     * @param isSigned
     * @return
     */
    public double roundToDouble(boolean isSigned)
    {
        // Handle the simple case where the value is contained in one uint64_t.
        // It is wrong to optimize getWord(0) to VAL; there might be more than one word.
        if (isSingleWord() || getActiveBits() <= APINT_BITS_PER_WORD)
        {
            if (isSigned)
            {
                long next = (getWord(0) << (64 - bitWidth))  >> (64 - bitWidth);
                return (double)next;
            }
            else
                return (double)getWord(0);
        }

        boolean isNeg = isSigned && get(bitWidth - 1);

        APInt temp = new APInt(isNeg? negative(): this);

        int n = temp.getActiveBits();

        long exp = n;
        if (exp > 1023)
        {
            if (!isSigned || !isNeg)
                return Double.POSITIVE_INFINITY;
            else
                return Double.NEGATIVE_INFINITY;
        }

        exp += 1023;

        long mantissa;
        long hiWord = whichWord(n - 1);
        if (hiWord == 0)
        {
            mantissa = temp.pVal[0];
            if (n > 52)
            {
                mantissa >>>= n - 52;
            }
        }
        else
        {
            assert hiWord > 0 :"huh?";
            long hiBits = temp.pVal[(int)hiWord] << (52 - n % APINT_BITS_PER_WORD);
            long loBits = temp.pVal[(int)(hiWord-1)] >>> (11 + n % APINT_BITS_PER_WORD);
            mantissa = hiBits | loBits;
        }

        long sign = isNeg ? (1 << (APINT_BITS_PER_WORD - 1)): 0;
        return Double.longBitsToDouble(sign | (exp << 52) | mantissa);
    }

    /**
     * Converts this signed APInt to a double value.
     * @return
     */
    public double signedRoundToDouble()
    {
        return roundToDouble(true);
    }

    /// The conversion does not do a translation from integer to double, it just
    /// re-interprets the bits as a double. Note that it is valid to do this on
    /// any bit width. Exactly 64 bits will be translated.
    /// @brief Converts APInt bits to a double
    public double bitsToDouble()
    {
        return Double.longBitsToDouble(isSingleWord() ? val : pVal[0]);
    }

    /// The conversion does not do a translation from integer to float, it just
    /// re-interprets the bits as a float. Note that it is valid to do this on
    /// any bit width. Exactly 32 bits will be translated.
    /// @brief Converts APInt bits to a double
    public float bitsToFloat()
    {
        return Float.intBitsToFloat((int)((isSingleWord() ? val : pVal[0])));
    }

    /// The conversion does not do a translation from double to integer, it just
    /// re-interprets the bits of the double. Note that it is valid to do this on
    /// any bit width but bits from v may get truncated.
    /// @brief Converts a double to APInt bits.
    public APInt doubleToBits(double v)
    {
        long t = Double.doubleToLongBits(v);;
        if (isSingleWord())
           val = t;
        else
            pVal[0] = t;
        return clearUnusedBits();
    }

    /// The conversion does not do a translation from float to integer, it just
    /// re-interprets the bits of the float. Note that it is valid to do this on
    /// any bit width but bits from v may get truncated.
    /// @brief Converts a float to APInt bits.
    public APInt floatToBits(float v)
    {
        int t = Float.floatToIntBits(v);
        if (isSingleWord())
            val = t;
        else
            pVal[0] = t;
        return clearUnusedBits();
    }
    /**
     * Sets the least significant part of a bignum to the input value, and
     * zeroes out higher parts.
     */
    public static void tcSet(long[] dest, int part, int parts)
    {
        assert dest != null && dest.length > 0;

        dest[0] = part;
        for (int i = 1; i < parts; i++)
            dest[i] = 0;
    }
    /** Returns true if a bignum is zero, false otherwise.  */
    public static boolean tcIsZero(long[] src, int parts)
    {
        for (int i = 0; i < parts; ++i)
            if (src[i] != 0)
                return false;

        return true;
    }

    public static boolean tcExtractBit(long[] parts, int bit)
    {
        long tmp = parts[bit / 64];
        long shift = (long)1 << (bit % 64);
        long res = tmp & shift;
        return res != 0;
    }

    public static void tcSetBit(long[] parts, int bit)
    {
        parts[bit / 64] |= (long)1 << (bit % 64);
    }

    public static void tcAssign(long[] src, long[] dest, int len)
    {
        System.arraycopy(src, 0, dest, 0, len);
    }

    public static int partMSB(long value)
    {
        int n, msb = 0;
        if (value == 0)
            return -1;

        n = 64 / 2;
        do
        {
            if ((value >>> n) != 0)
            {
                value >>>= n;
                msb += n;
            }
            n >>>= 1;
        }while (n != 0);

        return msb;
    }

    public static int partLSB(long value)
    {
        int n, lsb;
        if (value == 0)
            return -1;

        lsb = 64 - 1;
        n = 64 / 2;

        do
        {
            if ((value << n) != 0)
            {
                value <<= n;
                lsb -= n;
            }

            n >>>= 1;
        }while (n != 0);

        return lsb;
    }

    /**
     * Returns the bit number of the least significant set bit of a
     * number.  If the input number has no bits set -1 is returned.
     * @param parts
     * @param n
     * @return
     */
    public static int tcLSB(long[] parts, int n)
    {
       int i, lsb;
       for (i = 0; i < n; ++i)
       {
           if (parts[i] != 0)
           {
               lsb = partLSB(parts[i]);
               return lsb + i * 64;
           }
       }
       return -1;
    }

    public static int tcMSB(long[] parts, int n)
    {
        int msb;
        do
        {
            --n;
            if(parts[n] != 0)
            {
                msb = partMSB(parts[n]);
                return msb + n * 64;
            }
        }while (n != 0);

        return -1;
    }

    public static long lowBitMask(int bits)
    {
        assert (bits != 0 && bits <= 64);

        return ~0;
    }

    public static void tcExtract(
            long[] dest,
            int dstCount,
            long[] src,
            int srcBits,
            int srcLSB)
    {
        int firstSrcPart = 0, dstParts = 0, shift = 0, n = 0;
        dstParts = (srcBits + 64 - 1) / 64;
        assert dstParts <= dstCount;

        firstSrcPart = srcLSB / 64;
        System.arraycopy(src, firstSrcPart, dest, 0, dstParts);
        shift = srcLSB % 64;
        tcShiftRight(dest, dstParts, shift);

        n = dstParts * 64 - shift;
        if (n < srcBits)
        {
            long mask = lowBitMask(srcBits - n);
            dest[dstParts - 1] |= (src[firstSrcPart + dstParts] & mask) << n % 64;
        }
        else if (n > srcBits)
        {
            if ((srcBits % 64) != 0)
                dest[dstParts - 1] &= lowBitMask(srcBits % 64);
        }

        // Clear the higher parts.
        Arrays.fill(dest, dstParts, dstCount, 0);
    }

    /**
     * DST += RHS + C, where c is zero or one. return the carring flag.
     * @return
     */
    public static long tcAdd(long[] dst, long[] rhs, long c, int parts)
    {
        int i = 0;
        assert c >= 0 && c <= 1;
        for (; i < parts; i++)
        {
            long l = dst[i];

            if (c != 0)
            {
                dst[i] += rhs[i] + 1;
                c = (dst[i] <= 1)?1:0;
            }
            else
            {
                dst[i] += rhs[i];
                c = dst[i] < 1?1:0;
            }
        }
        return c;
    }

    /**
     * DST -= RHS + C, where c is zero or one. Return the carry flag.
     * @param dst
     * @param rhs
     * @param c
     * @param parts
     * @return
     */
    public static long tcSubtract(long[] dst, long[] rhs, long c, int parts)
    {
        int i;
        assert c >= 0 && c <= 1;

        for (i = 0; i < parts; i++)
        {
            long l = dst[i];
            if (c != 0)
            {
                dst[i] -= rhs[i] + 1;
                c = dst[i] >= 1 ? 1 : 0;
            }
            else
            {
                dst[i] -= rhs[i];
                c = dst[i] > 1 ? 1 : 0;
            }
        }
        return c;
    }

    /**
     * Negate a big number in-place.
     * @param dst
     * @param parts
     */
    public static void tcNegate(long[] dst, int parts)
    {
        tcComplement(dst, parts);
        tcIncrement(dst, parts);
    }

    public static long lowHalf(long v)
    {
        return v & ((1L << 32) - 1);
    }

    public static long highHalf(long v)
    {
        return (v & ~((1L << 32) - 1)) >>> 32;
    }
    /**
     * DST += SRC * MULTIPLIER + CARRY   if add is true
    DST -= SRC * MULTIPLIER + CARRY   if add is false

    Requires 0 <= DSTPARTS <= SRCPARTS + 1.  If DST overlaps SRC
    they must start at the same point, i.e. DST == SRC.

    If DSTPARTS == SRCPARTS + 1 no overflow occurs and zero is
    returned.  Otherwise DST is filled with the least significant
    DSTPARTS parts of the result, and if all of the omitted higher
    parts were zero return zero, otherwise overflow occurred and
    return one.
     */
    public static int tcMultiplyPart(long[] dst, int dstFromIndex,
            long[] src, long multiplier,
            long carry, int srcParts, int dstParts, boolean add)
    {
        int i, n;
        assert dstParts <= srcParts + 1;

        /* N loops; minimum of dstParts and srcParts.  */
        n = dstParts < srcParts ? dstParts : srcParts;

        for (i = 0; i < n; i++)
        {
            long low, mid, high, srcPart;

            srcPart = src[i];
            if (multiplier == 0 || srcPart == 0)
            {
                low = carry;
                high = 0;
            }
            else
            {
                low = lowHalf(srcPart) * lowHalf(multiplier);
                high = highHalf(srcPart) * highHalf(multiplier);

                mid = lowHalf(srcPart) * highHalf(multiplier);
                high += highHalf(mid);
                mid <<= 64 / 2;
                if (Long.compareUnsigned(low + mid, low) < 0)
                    high++;
                low += mid;

                mid = highHalf(srcPart) * lowHalf(multiplier);
                high += highHalf(mid);
                mid <<= 64 /2 ;
                if (Long.compareUnsigned(low + mid, low) < 0)
                    high++;
                low += mid;

                /* Now add carry.  */
                if (Long.compareUnsigned(low + carry, low) < 0)
                    high++;
                low += carry;
            }

            if (add)
            {
                if (Long.compareUnsigned(low + dst[i+dstFromIndex], low) < 0)
                    high++;
                dst[i + dstFromIndex] += low;
            }
            else
            {
                dst[i + dstFromIndex] = low;
            }
            carry = high;
        }

        if (i < dstParts)
        {
            assert  i + 1 == dstParts;
            dst[i + dstFromIndex] = carry;
            return 0;
        }
        else
        {
            if (carry != 0)
                return 1;

            if (multiplier != 0)
                for (; i < srcParts; i++)
                    if (src[i] != 0)
                        return i;
            return 0;
        }
    }

    /* DST = LHS * RHS, where DST has the same width as the operands and
   is filled with the least significant parts of the result.  Returns
   one if overflow occurred, otherwise zero.  DST must be disjoint
   from both operands.  */
    public static int tcMultiply(long[] dst, long[] lhs, long[] rhs, int parts)
    {
        int i;
        int overflow;

        assert (dst != lhs && dst != rhs);

        overflow = 0;
        tcSet(dst, 0, parts);

        for (i = 0; i < parts; i++)
            overflow |= tcMultiplyPart(dst, i, lhs, rhs[i], 0, parts, parts - i,
                    true);

        return overflow;
    }

    /* DST = LHS * RHS, where DST has width the sum of the widths of the
       operands.  No overflow occurs.  DST must be disjoint from both
       operands.  Returns the number of parts required to hold the
       result.  */
    public static int tcFullMultiply(long[] dst, long[] lhs, long[] rhs,
            int lhsParts, int rhsParts)
    {
  /* Put the narrower number on the LHS for less loops below.  */
        if (lhsParts > rhsParts)
        {
            return tcFullMultiply(dst, rhs, lhs, rhsParts, lhsParts);
        }
        else
        {
            int n;

            assert (dst != lhs && dst != rhs);

            tcSet(dst, 0, rhsParts);

            for (n = 0; n < lhsParts; n++)
                tcMultiplyPart(dst, n, rhs, lhs[n], 0, rhsParts, rhsParts + 1,
                        true);

            n = lhsParts + rhsParts;

            return n - (dst[n - 1] == 0 ? 1 : 0);
        }
    }

    /* If RHS is zero LHS and REMAINDER are left unchanged, return one.
       Otherwise set LHS to LHS / RHS with the fractional part discarded,
       set REMAINDER to the remainder, return zero.  i.e.

       OLD_LHS = RHS * LHS + REMAINDER

       SCRATCH is a bignum of the same size as the operands and result for
       use by the routine; its contents need not be initialized and are
       destroyed.  LHS, REMAINDER and SCRATCH must be distinct.
    */
    public static boolean tcDivide(long[] lhs, long[] rhs, long[] remainder,
            long[] srhs, int parts)
    {
        int n, shiftCount;
        long mask;

        assert (lhs != remainder && lhs != srhs && remainder != srhs);

        shiftCount = tcMSB(rhs, parts) + 1;
        if (shiftCount == 0)
            return true;

        shiftCount = parts * 64 - shiftCount;
        n = shiftCount / 64;
        mask = (long) 1 << (shiftCount % 64);

        tcAssign(srhs, rhs, parts);
        tcShiftLeft(srhs, parts, shiftCount);
        tcAssign(remainder, lhs, parts);
        tcSet(lhs, 0, parts);

  /* Loop, subtracting SRHS if REMAINDER is greater and adding that to
     the total.  */
        for (; ; )
        {
            int compare;

            compare = tcCompare(remainder, srhs, parts);
            if (compare >= 0)
            {
                tcSubtract(remainder, srhs, 0, parts);
                lhs[n] |= mask;
            }

            if (shiftCount == 0)
                break;
            shiftCount--;
            tcShiftRight(srhs, parts, 1);
            if ((mask >>= 1) == 0)
            {
                mask = 1L << (64 - 1);
                n--;
            }
        }

        return false;
    }

    /**
     * Shift a bignum left COUNT bits in-place.  Shifted in bits are zero.
     * There are no restrictions on COUNT.
     */
    public static void tcShiftLeft(long[] dst, int parts, int count)
    {
        if (count != 0)
        {
            int jump, shift;
            jump = count / 64;
            shift = count % 64;

            while (parts > jump)
            {
                parts--;
                /* dst[i] comes from the two parts src[i - jump] and,
                 * if we have
                 * an intra-part shift, src[i - jump - 1].
                 */
                long part = dst[parts - jump];

                if (shift != 0)
                {
                    part <<= shift;
                    if (Long.compareUnsigned(parts, jump + 1) >= 0)
                        part |= dst[parts - jump - 1] >>> (64 - shift);
                }
                dst[parts] = part;
            }
            Arrays.fill(dst, 0, parts, 0);
        }
    }
    /**
     * Shift a bignum right COUNT bits in-place.  Shifted in bits are
     * zero.  There are no restrictions on COUNT.
     */
    public static void tcShiftRight(long[] dst, int parts, int count)
    {
        if (count != 0)
        {
            int i, jump, shift;

            jump = count / 64;
            shift = count % 64;

            for (i = 0; i < parts; i++)
            {
                long part;

                if (i + jump >= parts)
                    part = 0;
                else
                {
                    part = dst[i + jump];
                    if (shift != 0)
                    {
                        part >>>= shift;
                        if (i + jump + 1 < parts)
                            part |= dst[i + jump + 1] << (64 - shift);
                    }
                }

                dst[i] = part;
            }
        }
    }

    /* Bitwise and of two bignums.  */
    public static void And(long[] dst, long[] rhs, int parts)
    {
        int i;

        for (i = 0; i < parts; i++)
            dst[i] &= rhs[i];
    }

    /* Bitwise inclusive or of two bignums.  */
    public static void tcOr(long[] dst, long[] rhs, int parts)
    {
        int i;

        for (i = 0; i < parts; i++)
            dst[i] |= rhs[i];
    }

    /* Bitwise exclusive or of two bignums.  */
    public static void tcXor(long[] dst, long[] rhs, int parts)
    {
        int i;

        for (i = 0; i < parts; i++)
            dst[i] ^= rhs[i];
    }

    /* Complement a bignum in-place.  */
    public static void tcComplement(long[] dst, int parts)
    {
        int i;

        for (i = 0; i < parts; i++)
            dst[i] = ~dst[i];
    }

    /* Comparison () of two bignums.  */
    public static int tcCompare(long[] lhs, long[] rhs, int parts)
    {
        while (parts != 0)
        {
            parts--;
            int res = Long.compareUnsigned(lhs[parts], rhs[parts]);
            if (res == 0)
                continue;
            return res;
        }

        return 0;
    }

    /* Increment a bignum in-place, return the carry flag.  */
    public static long tcIncrement(long[] dst, int parts)
    {
        int i;

        for (i = 0; i < parts; i++)
            if (++dst[i] != 0)
                break;

        return i == parts ? 1 : 0;
    }

    /* Set the least significant BITS bits of a bignum, clear the
       rest.  */
    public static void tcSetLeastSignificantBits(long[] dst, int parts,
            int bits)
    {
        int i;

        i = 0;
        while (bits > 64)
        {
            dst[i++] = ~(long) 0;
            bits -= 64;
        }

        if (bits != 0)
            dst[i++] = ~(long) 0 >> (64 - bits);

        while (i < parts)
            dst[i++] = 0;
    }
}
