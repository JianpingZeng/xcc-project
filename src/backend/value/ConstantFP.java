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

import java.math.BigDecimal;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class ConstantFP extends Constant
{
    private BigDecimal val;
    /**
     * Constructs a new instruction representing the specified constant.
     *
     * @param ty
     * @param v
     */
    private ConstantFP(Type ty, BigDecimal v)
    {
        super(ty, ValueKind.ConstantFPVal);
        val = v;
    }

    public static ConstantFP get(Type ty, double v)
    {
        return get(ty, BigDecimal.valueOf(v));
    }

    public static ConstantFP get(Type ty, BigDecimal v)
    {
        return new ConstantFP(ty, v);
    }
}
