package jlang.cpp;
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
 * This enum class define some useful predefined preprocessed directive command.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public enum PredefinedDirectiveCommand
{
    CPP_DEFINE("define"),
    CPP_ELIF("elif"),
    CPP_ELSE("else"),
    CPP_ENDIF("endif"),
    CPP_ERROR("error"),
    CPP_IF("if"),
    CPP_IFDEF("ifdef"),
    CPP_IFNDEF("ifndef"),
    CPP_INCLUDE("include"),
    CPP_LINE("line"),
    CPP_PRAGMA("pragma"),
    CPP_UNDEF("undef"),
    CPP_WARNING("warning"),
    CPP_INCLUDE_NEXT("include_next");
    
    private final String text;

    PredefinedDirectiveCommand(String text)
    {
        this.text = text;
    }

    public static PredefinedDirectiveCommand forText(String text)
    {
        for (PredefinedDirectiveCommand ppcmd : PredefinedDirectiveCommand
                .values())
            if (ppcmd.text.equals(text))
                return ppcmd;
        return null;
    }
}
