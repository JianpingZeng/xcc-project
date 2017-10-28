/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

package jlang.driver;

/**
 * A enumerate to describe what action would to be performed of each
 * Action instance.
 * @author Xlous.zeng
 * @version 0.1
 */
enum ActionClass
{
    /***
     * Read a source code (e.g. C source, or asm code).
     */
    InputClass("input"),

    /**
     * Make the decision about binding architecture.
     */
    BindArchClass("bind-arch"),

    /**
     * Performs processor on C source code.
     */
    PreprocessJobClass("preprocessor"),
    /**
     *
     */
    PrecompileJobClass("precompiler"),

    /**
     * Perform static analyze on AST.
     */
    AnalyzeJobClass("analyzer"),

    /**
     * Performs the traditional compilation action, it can be decomposed
     * into 4 phase, preprocessing, lexing, parsing, emit asssembly code.
     */
    CompileJobClass("compiler"),

    /**
     * Invoking the native assembly to translates the output result
     * of CompileJob into native object code.
     */
    AssembleJobClass("assembler"),

    /**
     * Invoking the native linker to generate finally executable file.
     */
    LinkJobClass("linker"),
    LipoJobClass("lipo");

    String className;

    ActionClass(String name)
    {
        className = name;
    }
    public final static ActionClass JobClassFirst = PreprocessJobClass;
    public final static ActionClass JobClassLast = LipoJobClass;
}
