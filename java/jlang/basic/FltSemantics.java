package jlang.basic;
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

/**
 * Represents floating point arithmetic semantics.
 * @author Xlous.zeng
 * @version 0.1
 */
public class FltSemantics
{
    /**
     * The largest E such that 2^E is representable; this matches the
     * definition of IEEE 754.
     */
    short maxExponent;

    /**
     * The smallest E such that 2^E is a normalized number; this
     * matches the definition of IEEE 754.
     */
    short minExponent;

    /**
     * Number of bits in the significand.  This includes the integer
     * bit.
     **/
    int precision;

    /**
     * True if arithmetic is supported.
     */
    boolean arithmeticOK;

    public FltSemantics(
            short maxExponent,
            short minExponent,
            int precision,
            boolean arithmeticOK)
    {
        this.maxExponent = maxExponent;
        this.minExponent = minExponent;
        this.precision = precision;
        this.arithmeticOK = arithmeticOK;
    }
}
