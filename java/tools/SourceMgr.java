package tools;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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
import jlang.support.MemoryBuffer;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * This owns the files read by a parser, handles include stacks,
 * and handles diagnostic wrangling.
 * @author Jianping Zeng
 * @version 0.1
 */
public final class SourceMgr
{
    public static class SMLoc
    {
        private MemoryBuffer buffer;
        private int startPos;

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null) return false;
            if (this == obj) return true;
            if (getClass() != obj.getClass())
                return false;

            SMLoc loc = (SMLoc)obj;
            return loc.buffer.equals(buffer) && startPos == loc.startPos;
        }

        public static SMLoc get(MemoryBuffer buf, int start)
        {
            SMLoc loc = new SMLoc();
            loc.buffer = buf;
            loc.startPos = start;
            return loc;
        }

        public static SMLoc get(MemoryBuffer buf)
        {
            SMLoc loc = new SMLoc();
            loc.buffer = buf;
            loc.startPos = buf.getBufferStart();
            return loc;
        }

        public boolean isValid()
        {
            return buffer != null;
        }

        public int getPointer()
        {
            return startPos;
        }
    }

    static class SrcBuffer
    {
        MemoryBuffer buffer;

        /**
         * This is the location of the parent include, or
         * null if it is in the top level.
         */
        SMLoc includeLoc;
    }

    private static class LineNoCache
    {
        int lastQueryBufferID;
        MemoryBuffer lastQuery;
        int lineNoOfQuery;
    }

    /**
     * This is all of the buffers that we are reading from.
     */
    private ArrayList<SrcBuffer> buffers = new ArrayList<>();
    /**
     * This is the list of directories we should search for
     * include files in.
     */
    private LinkedList<String> includeDirs;

    /**
     * This s cache for line number queries.
     */
    private LineNoCache lineNoCache;

    public void setIncludeDirs(List<String> dirs)
    {
        includeDirs = new LinkedList<>(dirs);
    }

    public SrcBuffer getBufferInfo(int i)
    {
        Util.assertion(i >= 0 && i < buffers.size(), "Invalid buffer ID!");
        return buffers.get(i);
    }

    public MemoryBuffer getMemoryBuffer(int i)
    {
        Util.assertion(i >= 0 && i < buffers.size(), "Invalid buffer ID!");
        return buffers.get(i).buffer;
    }

    public SMLoc getParentIncludeLoc(int i)
    {
        Util.assertion(i >= 0 && i < buffers.size(), "Invalid buffer ID!");
        return buffers.get(i).includeLoc;
    }

    public int addNewSourceBuffer(MemoryBuffer buf, SMLoc includeLoc)
    {
        SrcBuffer buffer = new SrcBuffer();
        buffer.buffer =  buf;
        buffer.includeLoc = includeLoc;
        buffers.add(buffer);
        return buffers.size() - 1;
    }

    /**
     * Search for a file with the specified name in the current directory or
     * in one of the {@linkplain #includeDirs}. If no such file found, this return
     * ~0, otherwise it returns the buffer ID of the stacked file.
     * @param filename
     * @param includeLoc
     * @return
     */
    public int addIncludeFile(String filename, SMLoc includeLoc)
    {
        MemoryBuffer newBuf = MemoryBuffer.getFile(filename);

        for (int i = 0, e = includeDirs.size(); i < e && newBuf == null; i++)
        {
            String incFile = includeDirs.get(i) + "/" + filename;
            newBuf = MemoryBuffer.getFile(incFile);
        }
        if (newBuf == null)
            return ~0;
        return addNewSourceBuffer(newBuf, includeLoc);
    }

    /**
     * Return the ID of the buffer containing the specified location, return -1
     * if not found.
     * @param loc
     * @return
     */
    public int findBufferContainingLoc(SMLoc loc)
    {
        for (int i = 0, e = buffers.size(); i < e; i++)
        {
            MemoryBuffer buf = buffers.get(i).buffer;
            if (buf.contains(loc.buffer))
                return i;
        }
        return ~0;
    }

    /**
     * Find the line number for the specified location in the specified file.
     *
     * @param loc
     * @param bufferID
     * @return
     */
    public int findLineNumber(SMLoc loc, int bufferID)
    {
        if (bufferID == -1) bufferID = findBufferContainingLoc(loc);
        Util.assertion(bufferID != -1, "Invalid location!");

        int lineNo = 1;

        MemoryBuffer buf = getBufferInfo(bufferID).buffer;

        if (lineNoCache != null)
        {
            if (lineNoCache.lastQueryBufferID == bufferID
                    && lineNoCache.lastQuery.getBuffer().equals(buf.getBuffer())
                    && lineNoCache.lastQuery.getBufferStart() <= buf.getBufferStart())
            {
                buf = lineNoCache.lastQuery;
                lineNo = lineNoCache.lineNoOfQuery;
            }
        }

        for (; !SMLoc.get(buf).equals(loc); buf.advance())
        {
            if (buf.getCurChar() == '\n')
                ++lineNo;
        }

        if (lineNoCache == null)
            lineNoCache = new LineNoCache();

        lineNoCache.lastQueryBufferID = bufferID;
        lineNoCache.lastQuery = buf;
        lineNoCache.lineNoOfQuery = lineNo;
        return lineNo;
    }

    public int findLineNumber(SMLoc loc)
    {
        return findLineNumber(loc, -1);
    }

    /**
     * Emit a message about the specified location with the specified string.
     *
     * @param loc
     * @param msg
     * @param type If not null, it specified the kind of message to be emitted (e.g. "error")
     *             which is prefixed to the message.
     */
    public void printMessage(SMLoc loc, String msg, String type)
    {
        PrintStream os = System.err;

        int curBuf = findBufferContainingLoc(loc);
        Util.assertion(curBuf != -1, "Invalid or unspecified location!");
        printIncludeStack(getBufferInfo(curBuf).includeLoc, os);

        getMessage(loc, msg, type).print(null, os);
    }

    /**
     * Return an SMDiagnostic at the specified location with the specified string.
     * @param loc
     * @param msg
     * @param type If not null, it specified the kind of message to be emitted (e.g. "error")
     *             which is prefixed to the message.
     * @return
     */
    public SMDiagnostic getMessage(SMLoc loc, String msg, String type)
    {
        int curBuf = findBufferContainingLoc(loc);
        Util.assertion(curBuf != -1, "Invalid or unspecified location!");

        MemoryBuffer curMB = getBufferInfo(curBuf).buffer;
        Util.assertion( curMB.getBuffer() == loc.buffer.getBuffer());
        int columnStart = loc.getPointer();
        while (columnStart >= curMB.getBufferStart()
                && curMB.getCharAt(columnStart) != '\n'
                && curMB.getCharAt(columnStart) != '\r')
            --columnStart;

        int columnEnd = loc.getPointer();
        while (columnEnd < curMB.length()
                && curMB.getCharAt(columnEnd) != '\n'
                && curMB.getCharAt(columnEnd) != '\r')
            ++columnEnd;

        String printedMsg = "";
        if (type != null)
        {
            printedMsg = type;
            printedMsg += ": ";
        }

        printedMsg += msg;

        return new SMDiagnostic(curMB.getBufferName(), findLineNumber(loc, curBuf),
                curMB.getBufferStart() - columnStart, printedMsg,
                curMB.getSubString(columnStart, columnEnd));
    }

    private void printIncludeStack(SMLoc includeLoc, PrintStream os)
    {
        // Top stack.
        if (includeLoc.buffer == null) return;

        int curBuf = findBufferContainingLoc(includeLoc);
        Util.assertion(curBuf != -1, "Invalid or unspecified location!");

        printIncludeStack(getBufferInfo(curBuf).includeLoc, os);

        os.printf("Included from %s:%d:\n", getBufferInfo(curBuf).buffer.getBufferName(),
                findLineNumber(includeLoc, curBuf));
    }
}
