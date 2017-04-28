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
 * This class defines some options that can be enabled for controlling
 * the dialect of C accepted.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class LangOptions
{
    /**
     * Whether allowing trigraph in source file.
     */
    public boolean trigraph;
    /**
     * BCPL-style comments, '//;'.
     */
    public boolean bcplComment;
    /**
     * bool, false, true keyword.
     */
    public boolean bool;

    /**
     * Whether the '$' in identifier is allowed.
     */
    public boolean dollarIdents;

    // For C94, C99.
    public boolean digraphs;
    /**
     * Preprocessor in asm mode.
     */
    public boolean asmPreprocessor;
    /**
     * True in gnu mode, false in Standard C mode.
     */
    public boolean gnuMode;
    /**
     * C89 implicit 'int'.
     */
    public boolean implicitInt;
    /**
     * Whether the hex float point number is allowed in source file.
     */
    public boolean hexFloats;
    /**
     * C99 support.
     */
    public boolean c99;
    /**
     * Whether __OPTIMIZE__ should be defined.
     */
    public boolean optimize;
    /**
     * Whether __OPTIMIZE_SIZE__ should be defined.
     */
    public boolean optimizeSize;

    public enum VisibilityMode
    {
        Default,
        Protected,
        Hidden
    }

    private VisibilityMode symbolVisibility =
            VisibilityMode.Default;

    /**
     * The user provided name for the 'main file'.
     */
    private String mainFileName;

    public String getMainFileName()
    {
        return mainFileName;
    }

    public void setMainFileName(String mainFileName)
    {
        this.mainFileName = mainFileName;
    }

    public VisibilityMode getSymbolVisibility()
    {
        return symbolVisibility;
    }

    public void setSymbolVisibility(VisibilityMode symbolVisibility)
    {
        this.symbolVisibility = symbolVisibility;
    }
}
