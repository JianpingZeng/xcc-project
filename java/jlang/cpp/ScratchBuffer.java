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

import jlang.basic.FileID;
import jlang.basic.MemoryBuffer;
import jlang.basic.SourceManager;
import tools.OutParamWrapper;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class ScratchBuffer
{
    private SourceManager sourceMgr;
    private char[] buffer;
    private SourceLocation bufferStartLoc;
    private int byteUsed;

    static final int ScratchBufSize = 4060;

    public ScratchBuffer(SourceManager sgr)
    {
        this.sourceMgr = sgr;
        byteUsed = ScratchBufSize;
    }

    /**
     * Splat the specified text into a temporary MemoryBuffer and
     /// return a SourceLocation that refers to the token.  This is just like the
     /// previous method, but returns a location that indicates the physloc of the
     /// token.
     */
    SourceLocation getToken(char[] buf, OutParamWrapper<Integer> dest)
    {
        if (byteUsed + buf.length +2 > ScratchBufSize)
            allocateScratchBuffer(buf.length + 2);

        buffer[byteUsed++] = '\0';

        dest.set(byteUsed);

        // Return a pointer to the character data.
        System.arraycopy(buf, 0, buffer, byteUsed, buf.length);

        byteUsed += buf.length + 1;

        // Add a NUL terminator to the token.  This keeps the tokens separated, in
        // case they get relexed, and puts them on their own virtual lines in case a
        // diagnostic points to one.
        buffer[byteUsed-1] = '\0';

        return bufferStartLoc.getFileLocWithOffset(byteUsed-buf.length-1);
    }

    private void allocateScratchBuffer(int requestedLen)
    {
        if (requestedLen < ScratchBufSize)
            requestedLen = ScratchBufSize;

        MemoryBuffer buf = MemoryBuffer.getNewMemBuffer(requestedLen, "<scratch space>");;
        FileID fid = sourceMgr.createFileIDForMemBuffer(buf);
        bufferStartLoc = sourceMgr.getLocForStartOfFile(fid);
        buffer = buf.getBuffer().array();
        byteUsed = 1;
        buffer[0] = '\0';  // Start out with a \0 for cleanliness.
    }

    public char[] getBuffer()
    {
        return buffer;
    }
}
