package frontend.sema;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2016, Xlous
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

import com.sun.deploy.net.proxy.pac.PACFunctionsImpl;
import sun.util.resources.cldr.hr.CalendarData_hr_HR;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class for arbitrary precision integers.
 * <br>
 * APInt - This class represents arbitrary precision constant integral values.
 * It is a functional replacement for common case unsigned integer frontend.type like
 * "unsigned", "unsigned long" or "uint64_t", but also allows non-byte-width
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
 *      * The value is stored canonically as an unsigned value. For operations
 *     where it makes a difference, there are both signed and unsigned variants
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
public class APInt
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
     * Bits in a word.
     */
    private static int APINT_BITS_PER_WORD = 4 * CHAR_BIT;
    /**
     * Bytes of a word.
     */
    private static int APINT_WORD_SIZE = 4;

    private APInt(long[] val, int bits)
    {
        assert val.length == bits;
        assert bits>0 :"bitwidth too small";
        bitWidth = bits;
        pVal = val;
    }

    /**
     * Checks if this integer value is stored in a single word(64).
     * @return
     */
    private boolean isSingleWord()
    {
        return bitWidth <= APINT_BITS_PER_WORD;
    }

    /**
     * Determines which word a bit is in.
     * @param bitPosition
     * @return Return the word position for the specified bits position.
     */
    private static int whichWord(int bitPosition)
    {
        return bitPosition / APINT_BITS_PER_WORD;
    }

    /**
     * Determines which bits in a word a bit is in.
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
            // is the same size as the word size(64).
            return this;
        }

        long mask = ~(0L >> (APINT_BITS_PER_WORD - wordBits));
        if (isSingleWord())
            val &= mask;
        else
            pVal[getNumWords()-1] &= mask;
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

    private int getNumWords()
    {
        return getNumWords(bitWidth);
    }

    private void fromString(int numBits, String str, int radix)
    {

    }

    private void divide(final APInt lhs, int lhsWords,
            final APInt rhs, int rhsWords,
            APInt[] result)
    {}

    private void initSlowCase(int numBits, long val, boolean isSigned)
    {
        pVal = new long[getNumWords()];
        pVal[0] = val;
        if (isSigned && val < 0)
        {
            for (int i = 1;i < getNumWords(); ++i)
                pVal[i] = -1L;
        }
    }
    private void initFromArray(long[] bigVal)
    {
        assert bitWidth> 0 :"bitwidth too small " + bitWidth;
        assert bigVal != null:"empty list";
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
        assert bitWidth> 0 :"bitwidth too small " + bitWidth;
        assert !bigVal.isEmpty():"empty list";
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
    {}

    private APInt andSlowCase(final APInt rhs)
    {}

    private APInt orSlowCase(final APInt rhs)
    {}

    private APInt xorSlowCase(final APInt rhs)
    {}

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
    {}

    private boolean equalSlowCase(long val)
    {}

    public APInt(int numBits, long val)
    {
        this(numBits, val, false);
    }

    public APInt(int numBits, long val, boolean isSigned)
    {
        bitWidth = numBits;
        val = 0;
        assert bitWidth> 0:"bitwidth too small";
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
        assert numBits>0 :"bitwidth too small";
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
        assert bitWidth>0 :"bitwidth too small";
        if (isSingleWord())
            val = that.val;
        else
            initSlowCase(that);
    }

    public boolean isNegative()
    {
        return
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
        for (int i = 0; i<len; i++)
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
     * @param width
     * @return
     */
    public APInt zext(int width)
    {
        assert width> 0:"Invalid APInt ZeroExtend request";
        if (width <=APINT_WORD_SIZE)
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
        if (width <= APINT_BITS_PER_WORD)
        {
            long val_ = val << (APINT_BITS_PER_WORD - bitWidth);
            val = val >> (width - bitWidth);
            return new APInt(width, val >> (APINT_BITS_PER_WORD - width))
        } long word = 0;
        APInt result = new APInt(new long[getNumWords(width)], width);

        int i;
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
        assert width < bitWidth :"Invalid APInt Truncate request";
        assert width> 0 : " Can't truncate to 0 bits";

        if (width <=APINT_WORD_SIZE)
            return new APInt(width, getRawData()[0]);

        APInt result = new APInt(new long[getNumWords(width)], width);
        int i;
        for (i = 0; i!= width /APINT_BITS_PER_WORD; i++)
            result.pVal[i] = pVal[i];

        int bits = ( 0 - width) % APINT_BITS_PER_WORD;
        if (bits != 0)
            result.pVal[i] = pVal[i] << bits >> bits;
        return result;
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
        if (bitWidth<width)
            return sext(width);
        if (bitWidth> width)
            return trunc(width);
        return this;
    }

    private long[] getRawData()
    {
        if(isSingleWord())
            return new long[]{val};
        else
            return pVal;
    }

    public boolean ult(final APInt rhs)
    {
        assert bitWidth == rhs.bitWidth:"Bit widths must be same for comparision";
        if (isSingleWord())
            return val < rhs.val;

    }

    public boolean ult(long rhs)
    {

    }

    public boolean slt(final  APInt rhs)
    {}

    public boolean slt(long rhs)
    {}

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
        assert bitWidth == rhs.bitWidth:"Comparison requires equal bit widths";
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

    private void toString(StringBuilder buffer,
            int radix,
            boolean isSigned,
            boolean formartAsCLiteral)
    {
        assert  radix == 10 || radix == 8 || radix == 16
                || radix == 2 || radix == 36 :"radix sholud be 2, 4, 8, 16!";

        String prefix = null;
        if (formartAsCLiteral)
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
        if(eq(0))
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
                if (i>= 0)
                    n = i;
                else
                {
                    buffer.append('-');
                    n = -i;
                }
            }

            while(idx < size)
            {
                buffer.append(prefix.charAt(idx));
                idx++;
            }
            while (n != 0)
            {
                buf[--bufLen] = digits.charAt((int)n % radix);
                n /= radix;
            }
            buffer.append(buf, bufLen, 65 - bufLen);
            return;
        }

        APInt temp = new APInt(this);
        if (isSigned && isNegative())
        {
            temp.flipAllBits();
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
            long shiftAmt = (radix == 16 ? 4: (radix == 8 ? 3 :1));
            long maskAmt = radix - 1;
            while(!temp.eq(0))
            {
                long digit = temp.getRawData()[0] & maskAmt;
                buffer.append(digits.charAt((int)digit));
                temp = temp.lshr(shiftAmt);
            }
        }
        else
        {
            APInt divisor = new APInt(radix == 10 ? 4: 8, radix);
            while(!temp.eq(0))
            {
                APInt apDigit = new APInt(1, 0);
                APInt temp2 = new APInt(temp.getBitWidth(), 0);
                divide(temp, temp.getNumWords(), divisor, divisor.getNumWords(),
                        new APInt[]{temp2, apDigit});
                int digit = apDigit.getZExtValue();
                assert  digit< radix :"divide failded";
                buffer.append(digits.charAt(digit));
                temp = temp2;
            }
        }

         for (int i = startDig, j = buffer.length()-1; i < j;)
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
     * Check if this APInt has an N-bits unsigned integer value.
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

    public int countLeadingZeros64(long x)
    {
        if (x == 0)
            return 64;
        int count = 0;
        for (int shift = 64 >> 1; shift != 0; shift >>>= 1)
        {
            long temp = x >> shift;
            if (temp != 0)
                x = temp;
            else
                count |= shift;
        }
        return count;
    }

    public int countLeadingZeros()
    {
        if (isSingleWord())
        {
            int unusedBits = APINT_BITS_PER_WORD - bitWidth;
            return countLeadingZeros64(val) - unusedBits;
        }
        return countLeadingZeroSlowCase();
    }

    public int countLeadingOnes()
    {

    }
    /**
     * Check if this APInt has an N-bits signed integer value.
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
        return whichWord(getActiveBits() - 1)+1
    }
    /**
     * Computes the minimum bit width for this APInt while considering it to be
     /// a signed (and probably negative) value. If the value is not negative,
     /// this function returns the same value as getActiveBits()+1. Otherwise, it
     /// returns the smallest bit width that will retain the negative value. For
     /// example, -1 can be written as 0b1 or 0xFFFFFFFFFF. 0b1 is shorter and so
     /// for -1, this function will always return 1.
     * @return
     */
    public int getMinSignedBits()
    {
        if (isNegative())
            return bitWidth - countLeadingOnes() -1;
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
     * Checks if this number is the power of 2.
     * @return
     */
    public boolean isPowerOf2()
    {
        if(isSingleWord())
            return isPowerOf2_64(val);
        return countPopulationSlowCase() == 1;
    }

    public boolean isPowerOf2_64(long val)
    {
        return (val & (val - 1)) == 0;
    }

    /**
     * this function counts the number of set bits in a value,
     * (64 bit edition.)
     * @param val
     * @return
     */
    public int countPopulation_64(long val)
    {
        long v = val - ((val >> 1) & 0x5555555555555555L);
        v = (v & 0x3333333333333333L) + ((v >> 2) & 0x3333333333333333L);
        v = (v + (v >> 4)) & 0x0F0F0F0F0F0F0F0FL;

        return (int)((v * 0x0101010101010101L) >> 56);
    }

    public int countPopulationSlowCase()
    {
        int count = 0;
        for (int i = 0; i< getNumWords(); ++i)
        {
            count += countPopulation_64(pVal[i]);
        }
        return count;
    }

    /**
     * Checks if an unsigned integer fits into the given (dynamic)
     * bit width.
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
        assert bitPosition< getBitWidth()
                :"Bit position out of bounds!";
        return (maskBit(bitPosition) &
                (isSingleWord()? val : pVal[whichWord(bitPosition)])) != 0;
    }

    public long getSExtValue()
    {
        if (isSingleWord())
            return (val << ((APINT_BITS_PER_WORD - bitWidth))) >>
                    (APINT_BITS_PER_WORD - bitWidth);
        assert getMinSignedBits()<=64 :"Too many bits for long";
        return pVal[0];
    }

    public long getZExtValue()
    {
        if (isSingleWord())
            return val;
        assert getActiveBits()<=64:"Too many bits for long";
        return pVal[0];
    }


    public long getLimitedValue(int limit)
    {
        return getActiveBits()>64 || getZExtValue()>limit ? limit:gtZExtValue();
    }

    public APInt ashr(int shiftAmt)
    {

    }

    public APInt ashr(final APInt shiftAmt)
    {
        return ashr((int) shiftAmt.getLimitedValue(bitWidth));
    }

    public APInt lshr(int shiftAmt)
    {

    }

    public APInt lshr(final APInt shiftAmt)
    {
        return lshr((int) shiftAmt.getLimitedValue(bitWidth));
    }

    public APInt shl(final APInt shiftAmt)
    {
        return shl((int) shiftAmt.getLimitedValue(bitWidth));
    }

    public APInt shl(int shiftAmt)
    {
        assert  shiftAmt<=bitWidth:"Invalid shift amounts";
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
     * @param lhs
     * @param shiftAmt
     * @return
     */
    public static APInt ashr(final APInt lhs, int shiftAmt)
    {
        return lhs.ashr(shiftAmt);
    }
}
