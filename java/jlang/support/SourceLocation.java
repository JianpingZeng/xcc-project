package jlang.support;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2018, Xlous Zeng.
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

import tools.Util;
import jlang.basic.SourceManager;

import java.io.PrintStream;

/**
 * This is a carefully crafted 32-bit identifier that encodes
 * a full include stack, line and column number information for a position in
 * an input translation unit.
 * @author Xlous.zeng
 * @version 0.1
 */
public class SourceLocation implements Comparable<SourceLocation>
{
    public static final SourceLocation NOPOS = new SourceLocation();

    private int id;
    private static final int macroBit = 1 << 31;

    public SourceLocation()
    {
        super();
        id = 0; // 0 is invalid fileID;
    }

    public SourceLocation(SourceLocation loc)
    {
        id = loc.id;
    }

    public boolean isFileID()
    {
        return (id & macroBit) == 0;
    }

    public boolean isMacroID()
    {
        return (id & macroBit) !=0;
    }

    public boolean isValid()
    {
        return id != 0;
    }

    public int getOffset()
    {
        return id & ~macroBit;
    }

    public static SourceLocation getFileLoc(int id)
    {
        Util.assertion((id & macroBit) == 0,  "Ran out of source location");
        SourceLocation l =  new SourceLocation();
        l.id = id;
        return l;
    }

    public static SourceLocation getMacroLoc(int id)
    {
        Util.assertion((id & macroBit) == 0,  "Ran out of source location");
        SourceLocation l =  new SourceLocation();
        l.id = id + macroBit;
        return l;
    }

    public SourceLocation getFileLocWithOffset(int offset)
    {
        Util.assertion(((getOffset() + offset) & macroBit) == 0, "invalid location");
        SourceLocation l = new SourceLocation();
        l.id = id + offset;
        return l;
    }

    public int getRawEncoding()
    {
        return id;
    }

    public static SourceLocation getFromRawEncoding(int loc)
    {
        SourceLocation res = new SourceLocation();
        res.id = loc;
        return res;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;

        if (getClass() != obj.getClass())
            return false;
        SourceLocation loc = (SourceLocation)obj;
        return  id == loc.id;
    }

    @Override
    public int hashCode()
    {
        return id;
    }

    @Override
    public int compareTo(SourceLocation o)
    {
        return id - o.id;
    }

    public void dump(SourceManager sourceMgr)
    {
        print(System.err, sourceMgr);
    }

    public void print(PrintStream os, SourceManager sgr)
    {
        if (!isValid())
        {
            os.print("<invalid loc>");
            return;
        }

        if (isFileID())
        {
            PresumedLoc ploc = sgr.getPresumedLoc(this);
            os.printf("%s:%d:%d", ploc.getFilename(), ploc.getLine(), ploc.getColumn());
            return;
        }

        sgr.getInstantiationLoc(this).print(os, sgr);
        os.print(" <Spelling=");
        sgr.getLiteralLoc(this).print(os, sgr);
        os.print(">");
    }

    @Override
    public SourceLocation clone()
    {
        SourceLocation loc = new SourceLocation();
        loc.id = id;
        return loc;
    }
}
