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

import com.sun.org.apache.xpath.internal.operations.Mod;
import lir.ci.LIRConstant;
import type.Type;

import java.util.ArrayList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class GlobalValue extends Value.Constant
{
    protected Module parent;
    /**
     * Constructs a new instruction representing the specified constant.
     *
     * @param
     */
    public GlobalValue(Type ty, int valueType, ArrayList<Use> oprands, String name)
    {
        super(ty, valueType, oprands);
        this.name = name;
    }
}