package hir;
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

import type.Type;
import utils.Util;

/**
 * The base class of {@linkplain ConstantUInt} and {@linkplain ConstantSInt}.
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class ConstantInt extends ConstantInteger
{
    /**
     * Represents the signed integer or unsigned integer depends on the kind of it.
     */
    protected long val;
    /**
     * Constructs a new instruction representing the specified constant.
     *
     * @param ty
     */
    public ConstantInt(Type ty, long v)
    {
        super(ty);
        val = v;
    }

    public static ConstantInt get(Type ty, byte v)
    {
        if (ty.isSignedType()) return ConstantSInt.get(ty, v);
        return ConstantUInt.get(ty, v);
    }

    public long getRawValue() { return val;}


    @Override
    public boolean isNullValue()
    {
        return val == 0;
    }

    public static class ConstantSInt extends ConstantInt
    {
        /**
         * Constructs a new instruction representing the specified constant.
         *
         * @param ty
         */
        public ConstantSInt(Type ty, long v)
        {
            super(ty, v);
        }

        public static ConstantSInt get(Type ty, long v)
        {

        }

        public static boolean isValueValidForType(Type ty, long v)
        {

        }

        public long getValue() { return val;}

        @Override
        public boolean isMaxValue()
        {
            long v = val;
            if (v < 0)
                return false;
            ++v;
            return !isValueValidForType(getType(), v) || v < 0;
        }

        @Override
        public boolean isMinValue()
        {
            long v = val;
            if (v>0)
                return false;
            --v;
            return !isValueValidForType(getType(), v) || v>0;
        }

        /**
         * Returns true if every bit is set to true.
         *
         * @return
         */
        @Override
        public boolean isAllOnesValue()
        {
            return val == -1;
        }
    }

    public static class ConstantUInt extends ConstantInt
    {
        /**
         * Constructs a new instruction representing the specified constant.
         *
         * @param ty
         */
        public ConstantUInt(Type ty, long v)
        {
            super(ty, v);
        }

        public static ConstantUInt get(Type ty, long v)
        {

        }

        public static boolean isValueValidForType(Type ty, long v)
        {

        }

        public long getValue() { return val; }

        @Override
        public boolean isMaxValue()
        {
            return false;
        }

        @Override
        public boolean isMinValue()
        {
            return false;
        }

        /**
         * Returns true if every bit is set to true.
         *
         * @return
         */
        @Override
        public boolean isAllOnesValue()
        {
            int num = Util.bitsOfOne(val, true);
            return num == 64;
        }
    }
}
