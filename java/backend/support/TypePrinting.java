package backend.support;
/*
 * Extremely C language Compiler
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

import backend.type.Type;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Stack;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class TypePrinting
{
    private HashMap<Type, String> typenames;

    public TypePrinting()
    {
        typenames = new HashMap<>();
    }

    public void clear()
    {
        typenames.clear();
    }
    public void print(Type ty, PrintStream os)
    {
        print(ty, os, false);
    }

    public void print(Type ty, PrintStream os, boolean ignoreTopLevelName)
    {}

    public boolean hasTypeName(Type ty)
    {
        return false;
    }

    public void addTypeName(Type ty, String name)
    {}

    private void calcTypeName(Type ty, Stack<Type> typeStack, PrintStream os)
    {
        calcTypeName(ty, typeStack, os, false);
    }

    private void calcTypeName(Type ty, Stack<Type> typeStack,
            PrintStream os, boolean ignroeTopLevelName)
    {}
}
