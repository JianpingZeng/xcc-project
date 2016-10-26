package sema;
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

import type.QualType;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class APSInt extends APInt
{
    private boolean isUnsigned;

    public APSInt() {super();}

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

    public APSInt assign(long rhs)
    {
        super.assign(rhs);
        return this;
    }

    public boolean isSigned() { return !isUnsigned; }
    public boolean isUnsigned() { return  isUnsigned;}
    public void setIsUnsigned(boolean x) { isUnsigned = x;}
    public void setIssigned(boolean x) { isUnsigned = !x;}

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

    public boolean lt(final APSInt rhs)
    {
        assert isUnsigned == rhs.isUnsigned:"Signedness mismatch!";
        return isUnsigned ? ult(rhs) : slt(rhs);
    }

    public boolean le(final  APSInt rhs)
    {

    }

    public boolean gt(final  APSInt rhs)
    {

    }

    public boolean ge(final  APSInt rhs)
    {

    }

    public boolean eq(final APSInt rhs)
    {}

    public boolean ne(final  APSInt rhs)
    {}

    public APSInt multiply(final  APSInt rhs)
    {
        assert (isUnsigned == rhs.isUnsigned):"Signedness mismatch!";
        return new APSInt(((APInt)this).multiply(rhs), isUnsigned);
    }

    public APSInt addictive(final APSInt rhs)
    {
        assert (isUnsigned == rhs.isUnsigned):"Signedness mismatch!";
        return new APSInt(((APInt)this).addictive(rhs), isUnsigned);
    }

    public APSInt subtraction(final APSInt rhs)
    {
        assert (isUnsigned == rhs.isUnsigned):"Signedness mismatch!";
        return new APSInt(((APInt)this).subtraction(rhs), isUnsigned);
    }

    public APSInt negative()
    {
        return new APSInt(((APInt)this).negative(), isUnsigned);
    }

    public APSInt not()
    {
        return new APSInt(((APInt)this).not(), isUnsigned);
    }

    public APSInt bitAnd(final APSInt rhs)
    {
        assert (isUnsigned == rhs.isUnsigned):"Signedness mismatch!";
        return new APSInt(((APInt)this).bitAnd(rhs), isUnsigned);
    }

    public APSInt bitOr(final APSInt rhs)
    {
        assert (isUnsigned == rhs.isUnsigned):"Signedness mismatch!";
        return new APSInt(((APInt)this).bitOr(rhs), isUnsigned);
    }

    public APSInt bitXor(final APSInt rhs)
    {
        assert (isUnsigned == rhs.isUnsigned):"Signedness mismatch!";
        return new APSInt(((APInt)this).bitXor(rhs), isUnsigned);
    }

    public APSInt percentAssign(final APSInt rhs)
    {

    }

    public APSInt divideAssign(final APSInt rhs)
    {

    }

    public APSInt remainder(final APSInt rhs)
    {

    }

    public APSInt divide(final APSInt rhs)
    {

    }

    public APSInt shr(int amt)
    {

    }

    public APSInt shrAssign(int amt)
    {

    }

    public APSInt shl(int bits)
    {

    }

    public APSInt shlAssign(int amt)
    {

    }

    public APSInt increment()
    {

    }

    public APSInt decremnt()
    {

    }

    public String toString(int radix)
    {
        return super.toString(radix, isSigned());
    }

    public APSInt extOrTrunc(int width)
    {
        if (isUnsigned)
            return new APSInt(zextOrTrunc(width), isUnsigned);
        else
            return new APSInt(sextOrTrunc(width), isUnsigned);
    }

    public static APSInt makeIntValue(long value, QualType type)
    {
        APSInt res = new APSInt(QualType.getIntWidth(type),
                !type.isSignedIntegerOrEnumerationType());
        res.assign(value);
        return res;
    }
}
