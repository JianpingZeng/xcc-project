package jlang.diag;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this filename except in compliance with the License.
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

import jlang.basic.FileID;
import jlang.basic.MemoryBuffer;
import jlang.basic.SourceManager;
import jlang.cpp.Token.StrData;
import jlang.basic.SourceLocation;
import tools.Pair;

/**
 * @author xlous.zeng
 * @version 0.1
 */
public class FullSourceLoc extends SourceLocation
{
	private SourceManager sourceMgr;

	public FullSourceLoc(SourceLocation loc, SourceManager sgr)
	{
		super(loc);
		sourceMgr = sgr;
	}

	public FullSourceLoc()
	{
		super();
	}

    public SourceManager getSourceMgr()
    {
        return sourceMgr;
    }

    public FileID getFileID()
    {
        assert isValid();
        return sourceMgr.getFileID(this);
    }

    public FullSourceLoc getInstantiationLoc()
    {
        assert isValid();
        return new FullSourceLoc(sourceMgr.getInstantiationLoc(this), sourceMgr);
    }

    public FullSourceLoc getSpellingLoc()
    {
        assert isValid();
        return new FullSourceLoc(sourceMgr.getLiteralLoc(this), sourceMgr);
    }

    public int getInstantiationLineNumber()
    {
        assert isValid();
        return sourceMgr.getInstantiationLineNumber(this);
    }

    public int getIntantiationColumnNumber()
    {
        assert isValid();
        return sourceMgr.getInstantiationColumnNumber(this);
    }

    public int getSpellingLineNumber()
    {
        assert isValid();
        return sourceMgr.getLiteralLineNumber(this);
    }

    public int getSpellingColumnNumber()
    {
        assert isValid();
        return sourceMgr.getLiteralColumnNumber(this);
    }

    public boolean isInSystemHeader()
    {
        assert isValid();
        return sourceMgr.isInSystemHeader(this);
    }

    public StrData getCharacterData()
    {
        assert isValid();
        return sourceMgr.getCharacterData(this);
    }

    public MemoryBuffer getBuffer()
    {
        assert isValid();
        return sourceMgr.getBuffer(sourceMgr.getFileID(this));
    }

    public char[] getBufferData()
    {
        assert isValid();
        return getBuffer().getBuffer().array();
    }

    public Pair<FileID, Integer> getDecomposedLoc()
    {
        return sourceMgr.getDecomposedLoc(this);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (obj == null) return false;
        if (obj.getClass() != getClass())
            return false;
        FullSourceLoc loc = (FullSourceLoc)obj;
        return getRawEncoding() == loc.getRawEncoding()
                && sourceMgr.equals(loc.sourceMgr);
    }

    @Override
    public int hashCode()
    {
        return getRawEncoding() << 11 + sourceMgr.hashCode();
    }
}
