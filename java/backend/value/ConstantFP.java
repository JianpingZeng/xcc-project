package backend.value;
/*
 * Xlous C language CompilerInstance
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
import jlang.support.APFloat;
import jlang.support.APSInt;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class ConstantFP extends Constant
{
    private APFloat val;
    /**
     * Constructs a new instruction representing the specified constants.
     *
     * @param ty
     * @param v
     */
    private ConstantFP(Type ty, APFloat v)
    {
        super(ty, ValueKind.ConstantFPVal);
        val = v;
    }

    public static ConstantFP get(Type ty, double v)
    {
        return get(ty, new APFloat(v));
    }

    public static ConstantFP get(Type ty, APFloat v)
    {
        return new ConstantFP(ty, v);
    }

    public static Constant get(APFloat aFloat)
    {
        // TODO
        return null;
    }

    @Override
    public boolean isNullValue()
    {
        return false;
    }

    public static Constant get(APSInt complexIntReal)
    {
        return null;
    }

    public double getValue()
    {
        return val.convertToDouble();
    }

    public APFloat getValueAPF()
    {
        return val;
    }
}
