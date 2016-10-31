package backend.value;
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

import backend.type.Type;
import tools.Pair;
import tools.Util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

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
        if (ty.isSigned()) return ConstantSInt.get(ty, v);
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
        private static HashMap<Pair<Type, Long>, ConstantSInt> sIntConstants;
        static
        {
            sIntConstants = new HashMap();
        }
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
            Pair<Type, Long> key = new Pair<>(ty, v);
            ConstantSInt res = sIntConstants.get(key);
            if (res != null)
                return res;
            ConstantSInt val = new ConstantSInt(ty, v);
            sIntConstants.put(key, val);
            return val;
        }

        public static boolean isValueValidForType(Type ty, long v)
        {
            switch (ty.getPrimitiveID())
            {
                default:return false;

                case Type.SByteTyID:
                    return (v <= Util.INT8_MAX && v>=Util.INT8_MIN);
                case Type.ShortTyID:
                    return (v <= Util.INT16_MAX && v>=Util.INT16_MIN);
                case Type.IntTyID:
                    return (v <= Util.INT32_MAX && v>=Util.INT32_MIN);
                case Type.LongTyID:
                    return (v<= Util.INT64_MAX) && (v>= Util.INT64_MIN);
            }
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
        private static HashMap<Pair<Type, Long>, ConstantUInt> uIntConstants;
        static
        {
            uIntConstants = new HashMap();
        }
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
            Pair<Type, Long> key = new Pair<>(ty, v);
            ConstantUInt res = uIntConstants.get(key);
            if (res != null)
                return res;
            ConstantUInt val = new ConstantUInt(ty, v);
            uIntConstants.put(key, val);
            return val;
        }

        public static boolean isValueValidForType(Type ty, long v)
        {
            switch (ty.getPrimitiveID())
            {
                default:return false;

                case Type.UByteTyID:
                    return (Util.ule(v, Util.UINT8_MAX) && Util.ule(v, 0));
                case Type.ShortTyID:
                    return (Util.ule(v, Util.UINT16_MAX) && Util.ule(v, 0));
                case Type.IntTyID:
                    return (Util.ule(v, Util.UINT32_MAX) && Util.ule(v, 0));
                case Type.LongTyID:
                    return (Util.ule(v, Util.UINT64_MAX) && Util.ule(v, 0));
            }
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

    public static class ConstantCreator<ConstantClass>
    {
        private final Constructor<? extends ConstantClass> ctor;

        public ConstantCreator(Class<? extends ConstantClass> impl)
                throws NoSuchMethodException
        {
            ctor = impl.getConstructor(Type.class, Long.class);
        }

        public ConstantClass create(Type ty, Long val)
                throws IllegalAccessException, InvocationTargetException,
                InstantiationException
        {
            return ctor.newInstance(ty, val);
        }
    }
}
