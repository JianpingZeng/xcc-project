/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
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
/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class APSInt extends APInt
{
    private boolean isUnsigned;

    public APSInt()
    {
        super();
    }

    public APSInt(int bitWidth)
    {
        this(bitWidth, true);
    }

    public APSInt(final APInt i)
    {
        this(i, true);
    }

    public APSInt(int bitWidth, boolean isUnsigned)
    {
        super(bitWidth, 0);
        this.isUnsigned = isUnsigned;
    }

    public APSInt(final APInt i, boolean isUnsigned)
    {
        super(i);
        this.isUnsigned = isUnsigned;
    }

    public APSInt assign(final APSInt rhs)
    {
        super.assign(rhs);
        isUnsigned = rhs.isUnsigned;
        return this;
    }

    public APSInt assign(final APInt rhs)
    {
        super.assign(rhs);
        return this;
    }

    public APSInt assign(boolean val)
    {
        super.assign(val);
        return this;
    }

    public APSInt assign(long rhs)
    {
        super.assign(rhs);
        return this;
    }

    public boolean isSigned()
    {
        return !isUnsigned;
    }

    public boolean isUnsigned()
    {
        return isUnsigned;
    }

    public void setIsUnsigned(boolean x)
    {
        isUnsigned = x;
    }

    public void setIssigned(boolean x)
    {
        isUnsigned = !x;
    }

    /**
     * Converts an APInt to a String.
     * @param radix
     * @return
     */
    public String toString(int radix)
    {
        return super.toString(radix, isSigned());
    }

    /**
     * Append this APSInt to the specified StringBiulder object.
     * @param builder
     * @param radix
     */
    public void toString(StringBuilder builder, int radix)
    {
        super.toString(builder, radix, isSigned(), false);
    }

    public APSInt extend(long width)
    {
        if (isUnsigned)
            return new APSInt(zext((int)width), isUnsigned);
        else
            return new APSInt(sext((int)width), isUnsigned);
    }

    public APSInt trunc(long width)
    {
        return new APSInt(trunc((int)width), isUnsigned);
    }

    public APSInt extOrTrunc(int width)
    {
        if (isUnsigned)
            return new APSInt(zextOrTrunc(width), isUnsigned);
        else
            return new APSInt(sextOrTrunc(width), isUnsigned);
    }

    public APInt remAssign(final APSInt rhs)
    {
        Util.assertion(isUnsigned() == rhs.isUnsigned(), "Signedness mismatch!");
        if (isUnsigned)
            assign(urem(rhs));
        else
            assign(srem(rhs));
        return this;
    }

    public APSInt divAssign(final APSInt rhs)
    {
        Util.assertion(isUnsigned == rhs.isUnsigned, "Signedness mismatch!");
        if (isUnsigned)
            assign(udiv(rhs));
        else
            assign(sdiv(rhs));
        return this;
    }

    public APSInt rem(final APSInt rhs)
    {
        Util.assertion(isUnsigned == rhs.isUnsigned, "Signedness mismatch!");
        return isUnsigned?new APSInt(urem(rhs), true)
                : new APSInt(srem(rhs), false);
    }

    public APSInt div(final APSInt rhs)
    {
        Util.assertion(isUnsigned == rhs.isUnsigned, "Signedness mismatch!");
        return isUnsigned?new APSInt(udiv(rhs), true):
                new APSInt(sdiv(rhs), false);
    }

    public APSInt shr(int amt)
    {
        return isUnsigned?new APSInt(lshr(amt), true)
                : new APSInt(ashr(amt), false);
    }

    public APSInt shrAssign(int amt)
    {
        assign(shr(amt));
        return this;
    }

    public boolean lt(final APSInt rhs)
    {
        Util.assertion(isUnsigned == rhs.isUnsigned, "Signedness mismatch!");
        return isUnsigned ? ult(rhs) : slt(rhs);
    }

    public boolean le(final  APSInt rhs)
    {
        Util.assertion(isUnsigned == rhs.isUnsigned, "Signedness mismatch!");
        return isUnsigned ? ule(rhs) : sle(rhs);
    }

    public boolean gt(final  APSInt rhs)
    {
        Util.assertion(isUnsigned == rhs.isUnsigned, "Signedness mismatch!");
        return isUnsigned ? ugt(rhs) : sgt(rhs);
    }

    public boolean ge(final  APSInt rhs)
    {
        Util.assertion(isUnsigned == rhs.isUnsigned, "Signedness mismatch!");
        return isUnsigned ? uge(rhs) : sge(rhs);
    }

    public APSInt shl(int bits)
    {
        return new APSInt(super.shl(bits), isUnsigned);
    }

    public APSInt shlAssign(int amt)
    {
        assign(shl(amt));
        return this;
    }

    public APSInt increment()
    {
        super.increase();
        return this;
    }

    public APSInt decremnt()
    {
        super.decrease();
        return this;
    }

    public APSInt addAssign(final APSInt rhs)
    {
        Util.assertion((isUnsigned == rhs.isUnsigned), "Signedness mismatch!");
        super.addAssign(rhs);
        return this;
    }

    public APSInt subAssign(final APSInt rhs)
    {
        Util.assertion((isUnsigned == rhs.isUnsigned), "Signedness mismatch!");
        super.subAssign(rhs);
        return this;
    }

    public APSInt mulAssign(final APSInt rhs)
    {
        Util.assertion((isUnsigned == rhs.isUnsigned), "Signedness mismatch!");
        super.mulAssign(rhs);
        return this;
    }

    public APSInt andAssign(final APSInt rhs)
    {
        Util.assertion((isUnsigned == rhs.isUnsigned), "Signedness mismatch!");
        super.andAssign(rhs);
        return this;
    }

    public APSInt orAssign(final APSInt rhs)
    {
        Util.assertion((isUnsigned == rhs.isUnsigned), "Signedness mismatch!");
        super.orAssign(rhs);
        return this;
    }

    public APSInt xorAssign(final APSInt rhs)
    {
        Util.assertion((isUnsigned == rhs.isUnsigned), "Signedness mismatch!");
        super.xorAssign(rhs);
        return this;
    }

    public APSInt and(final APSInt rhs)
    {
        Util.assertion((isUnsigned == rhs.isUnsigned), "Signedness mismatch!");
        return new APSInt(super.and(rhs), isUnsigned);
    }

    public APSInt and(long rhs)
    {
        return new APSInt(super.and(rhs), isUnsigned);
    }

    public APSInt or(final APSInt rhs)
    {
        Util.assertion((isUnsigned == rhs.isUnsigned), "Signedness mismatch!");
        return new APSInt(super.or(rhs), isUnsigned);
    }

    public APSInt or(long rhs)
    {
        return new APSInt(super.or(rhs), isUnsigned);
    }

    public APSInt xor(final APSInt rhs)
    {
        Util.assertion((isUnsigned == rhs.isUnsigned), "Signedness mismatch!");
        return new APSInt(super.xor(rhs), isUnsigned);
    }

    public APSInt xor(long rhs)
    {
        return new APSInt(super.xor(rhs), isUnsigned);
    }

    public APSInt mul(final  APSInt rhs)
    {
        Util.assertion((isUnsigned == rhs.isUnsigned), "Signedness mismatch!");
        return new APSInt(super.mul(rhs), isUnsigned);
    }

    public APSInt mul(long rhs)
    {
        return new APSInt(super.mul(rhs), isUnsigned);
    }

    public APSInt add(final APSInt rhs)
    {
        Util.assertion((isUnsigned == rhs.isUnsigned), "Signedness mismatch!");
        return new APSInt(super.add(rhs), isUnsigned);
    }

    public APSInt add(long rhs)
    {
        return new APSInt(super.add(rhs), isUnsigned);
    }

    public APSInt sub(final APSInt rhs)
    {
        Util.assertion((isUnsigned == rhs.isUnsigned), "Signedness mismatch!");
        return new APSInt(super.sub(rhs), isUnsigned);
    }

    public APSInt sub(long rhs)
    {
        return new APSInt(super.sub(rhs), isUnsigned);
    }

    /**
     * unary ~ operation.
     * @return
     */
    public APSInt not()
    {
        return new APSInt(((APInt)this).not(), isUnsigned);
    }

    /**
     * unary minus operation
     * @return
     */
    public APSInt negative()
    {
        APInt neg = super.negative();
        return new APSInt(neg, isUnsigned);
    }

    /**
     * Return the APSInt represent the integer number value with given bit width
     * and signedness.
     * @param numBits
     * @param isUnsigned
     * @return
     */
    public static APSInt getMaxValue(int numBits, boolean isUnsigned)
    {
        return new APSInt(isUnsigned?APInt.getMaxValue(numBits)
                            :APInt.getSignedMaxValue(numBits), isUnsigned);
    }

    public static APSInt getMinValue(int numBits, boolean isUnsigned)
    {
        return new APSInt(isUnsigned? APInt.getMinValue(numBits)
                        : APInt.getSignedMinValue(numBits), isUnsigned);
    }
}
