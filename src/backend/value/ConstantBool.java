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

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class ConstantBool extends ConstantInteger
{
    private boolean val;

    public static final ConstantBool True = new ConstantBool(true);
    public static final ConstantBool False = new ConstantBool(false);
    /**
     * Constructs a new instruction representing the specified constant.
     *
     * @param v
     */
    protected ConstantBool(boolean v)
    {
        super(Type.Int1Ty);
        val = v;
    }

    public static ConstantBool get(boolean value)
    {
        return value ? True:False;
    }

    public static ConstantBool get(Type ty, boolean value)
    {
        return get(value);
    }

    public boolean getValue() {return val;}

    @Override public boolean isNullValue()
    {
        return this == False;
    }

    @Override public boolean isMaxValue()
    {
        return this == True;
    }

    @Override public boolean isMinValue()
    {
        return this == False;
    }

    /**
     * Returns true if every bit is set to true.
     *
     * @return
     */
    @Override public boolean isAllOnesValue()
    {
        return this == True;
    }
}
