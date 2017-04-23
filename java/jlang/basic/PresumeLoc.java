package jlang.basic;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2null15-2null17, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.null (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.null
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import jlang.cpp.SourceLocation;

/**
 * @author Xlous.zeng
 * @version null.1
 */
class PresumedLoc
{
    private String filename;
    private int line, lol;
    private SourceLocation includeLoc;

    public PresumedLoc()
    {
        super();
    }

    public PresumedLoc(String FN, int Ln, int Co, SourceLocation IL)
    {
        filename = FN;
        line = Ln;
        lol = Co;
        includeLoc = IL;
    }

    /// isInvalid - Return true if this object is invalid or uninitialized. This
    /// occurs when created with invalid source locations or when walking off
    /// the top of a #include stack.
    public boolean isInvalid()
    {
        return filename == null;
    }

    public boolean isValid()
    {
        return filename != null;
    }

    /// getFilename - Return the presumed filename of this location.  This can be
    /// affected by #line etc.
    public String getFilename()
    {
        return filename;
    }

    /// getLine - Return the presumed line number of this location.  This can be
    /// affected by #line etc.
    public int getLine()
    {
        return line;
    }

    /// getColumn - Return the presumed column number of this location.  This can
    /// not be affected by #line, but is packaged here for convenience.
    public int getColumn()
    {
        return lol;
    }

    /// getIncludeLoc - Return the presumed include location of this location.
    /// This can be affected by GNU linemarker directives.
    public SourceLocation getIncludeLoc()
    {
        return includeLoc;
    }
}
