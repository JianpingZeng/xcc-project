/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
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

package utils.filecheck;

import tools.Util;
import gnu.trove.list.array.TCharArrayList;
import jlang.support.MemoryBuffer;
import tools.SourceMgr;
import tools.commandline.*;

import java.util.ArrayList;

import static tools.commandline.Desc.desc;
import static tools.commandline.FormattingFlags.Positional;
import static tools.commandline.Initializer.init;
import static tools.commandline.NumOccurrences.Required;
import static tools.commandline.OptionNameApplicator.optionName;
import static tools.commandline.ValueDesc.valueDesc;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class FileCheck
{
    private static final StringOpt CheckFilename =
            new StringOpt(new FormattingFlagsApplicator(Positional),
                    desc("<check-file>"),
                    new NumOccurrencesApplicator(Required));

    private static final StringOpt InputFilename =
            new StringOpt(optionName("input-file"),
                    desc("File to check (default to stdin)"),
                    init("-"), valueDesc("filename"));
    private static final StringOpt CheckPrefix =
            new StringOpt(optionName("check-prefix"),
                    init("CHECK"),
                    desc("Prefix to use from check file (default to 'CHECK')"));

    private static final BooleanOpt NoCanonicalizeWithSpace =
            new BooleanOpt(optionName("strict-whitespace"),
                    desc("Do not treat all horizonal whitespace as equivalent"));
    static class CheckString
    {
        String str;
        SourceMgr.SMLoc loc;
        boolean isCheckNext;
        public CheckString(String str, SourceMgr.SMLoc loc, boolean isCheckNext)
        {
            this.str = str;
            this.loc = loc;
            this.isCheckNext = isCheckNext;
        }
    }

    private static int findFixedStringInBuffer(String str, int curPtr, MemoryBuffer buffer)
    {
        Util.assertion(!str.isEmpty(), "Can't find an empty string");

        int end = buffer.length();
        String bufStr = new String(buffer.getBuffer());
        while (true)
        {
            curPtr = bufStr.indexOf(str, curPtr);
            if (curPtr < 0) return end;

            if (str.length() == 1) return curPtr;

            if (str.length() <= (end - curPtr) &&
                    bufStr.substring(curPtr+1, str.length()-1)
                            .equals(str.substring(1, str.length()-1)))
                return curPtr;

            ++curPtr;
        }
    }

    private static boolean readCheckFile(SourceMgr smg,
            ArrayList<CheckString> checkStrings)
    {

        MemoryBuffer buffer = MemoryBuffer.getFileOrSTDIN(CheckFilename.value);
        if (buffer == null)
        {
            System.err.printf("Couldn't open check file '%s'\n",
                    CheckFilename.value);
            return true;
        }
        smg.addNewSourceBuffer(buffer, new SourceMgr.SMLoc());

        int curPtr = buffer.getBufferStart(), end = buffer.length();
        while (true)
        {
            int ptr = findFixedStringInBuffer(CheckPrefix.value, curPtr, buffer);;

            if (ptr == end)
                break;

            int checkPrefixStart = ptr;
            boolean isCheckNext;

            if (buffer.getCharAt(ptr+CheckPrefix.value.length()) == ':')
            {
                ptr += CheckPrefix.value.length() + 1;
                isCheckNext = false;
            }
            else if (end - ptr > 6 && buffer.getSubString(ptr+CheckPrefix.value.length(),
                    ptr+CheckPrefix.value.length() + 6).equals("-NEXT:"))
            {
                ptr += ptr+CheckPrefix.value.length() + 7;
                isCheckNext = true;
            }
            else
            {
                curPtr = ptr + 1;
                continue;
            }

            while (buffer.getCharAt(ptr) == ' ' || buffer.getCharAt(ptr) == '\t')
                ++ptr;

            curPtr = ptr;
            while (curPtr != end && buffer.getCharAt(curPtr) != '\n' &&
                    buffer.getCharAt(curPtr) != '\r')
                ++curPtr;

            while (buffer.getCharAt(curPtr-1) == ' ' ||
                    buffer.getCharAt(curPtr-1) == '\t')
                --curPtr;

            if (ptr >= curPtr)
            {
                smg.printMessage(SourceMgr.SMLoc.get(buffer, curPtr),
                        "found empty check string with prefix '"
                                + CheckPrefix.value + ":'", "error");
                return true;
            }

            if (isCheckNext && checkStrings.isEmpty())
            {
                smg.printMessage(SourceMgr.SMLoc.get(buffer, checkPrefixStart),
                        "found '" + CheckPrefix.value + "-NEXT:' without previous '"
                + CheckPrefix.value + ": line", "error");
                return true;
            }

            checkStrings.add(new CheckString(buffer.getSubString(ptr, curPtr),
                    SourceMgr.SMLoc.get(buffer, ptr), isCheckNext));
        }

        if (checkStrings.isEmpty())
        {
            System.err.printf("error: no check strings found with prefix '%s:'\n",
                    CheckPrefix.value);
            return true;
        }
        return false;
    }

    private static void canonicalizeCheckStrings(ArrayList<CheckString> checkStrings)
    {
        StringBuilder buf = new StringBuilder();
        for (int i =0,e = checkStrings.size(); i < e; i++)
        {
            String str = checkStrings.get(i).str;
            buf.append(str);
            for (int j = 0; j < buf.length(); j++)
            {
                if (buf.charAt(j)!= ' ' && buf.charAt(j) != '\t')
                    continue;

                buf.setCharAt(j, ' ');
                while (j+1 != buf.length() &&
                        (buf.charAt(j+1) == ' ' || buf.charAt(j+1) == '\t'))
                    buf.deleteCharAt(j+1);
            }
            checkStrings.get(i).str = buf.toString();
            buf.delete(0, buf.length());
        }
    }

    private static MemoryBuffer canonicalizeInputFile(MemoryBuffer buffer)
    {
        TCharArrayList newFile = new TCharArrayList(buffer.length());
        for (int ptr = buffer.getBufferStart(), end = buffer.length(); ptr < end; ptr++)
        {
            char ch = buffer.getCharAt(ptr);
            if (ch != ' ' && ch != '\t')
            {
                newFile.add(ch);
                continue;
            }

            newFile.add(' ');
            while (ptr+1 < end && (buffer.getCharAt(ptr+1) == ' ' ||
                    buffer.getCharAt(ptr + 1) == '\t'))
                ++ptr;
        }

        return MemoryBuffer.getMemBuffer(
                new String(newFile.toArray()), buffer.getBufferName());
    }

    private static void printCheckFailed(SourceMgr sgr,
            CheckString checkStr, MemoryBuffer buffer,
            int curPtr, int bufferEnd)
    {
        sgr.printMessage(checkStr.loc, "expected string not found in input", "error");;

        int scan = curPtr;
        while (scan < bufferEnd && (buffer.getCharAt(scan) == ' ' ||
        buffer.getCharAt(scan) == '\t'))
            ++scan;

        if (buffer.getCharAt(scan) == '\n' || buffer.getCharAt(scan) == '\r')
            curPtr = scan+1;

        sgr.printMessage(SourceMgr.SMLoc.get(buffer, curPtr),
                "scanning from here", "note");
    }

    private static int countNumNewlinesBetween(MemoryBuffer buffer, int start, int end)
    {
        int numNewLines = 0;
        for (;start < end;++start)
        {
            char ch = buffer.getCharAt(start);
            if (ch != '\n' && ch != '\r')
                continue;

            ++numNewLines;

            if (start + 1 < end && ch == buffer.getCharAt(start + 1))
                ++start;
        }
        return numNewLines;
    }

    public static void main(String[] args)
    {
        try
        {
            CL.parseCommandLineOptions(args);

            SourceMgr sm = new SourceMgr();

            ArrayList<CheckString> checkStrings = new ArrayList<>();
            if (readCheckFile(sm, checkStrings))
                System.exit(2);

            if (!NoCanonicalizeWithSpace.value)
                canonicalizeCheckStrings(checkStrings);

            MemoryBuffer buffer = MemoryBuffer.getFileOrSTDIN(InputFilename.value);
            if (buffer == null)
            {
                System.err.printf("Could not open input file '%s'\n",
                        InputFilename.value);
                return;
            }

            if (!NoCanonicalizeWithSpace.value)
                buffer = canonicalizeInputFile(buffer);

            sm.addNewSourceBuffer(buffer, new SourceMgr.SMLoc());

            int lastMatch = 0;
            int curPtr = buffer.getBufferStart(), end = buffer.length();

            for (int strNo = 0, e = checkStrings.size(); strNo < e; strNo++)
            {
                CheckString str = checkStrings.get(strNo);

                int ptr = findFixedStringInBuffer(str.str, curPtr, buffer);
                if (ptr == end)
                {
                    printCheckFailed(sm, str, buffer, curPtr, end);
                    System.exit(1);
                }

                if (str.isCheckNext)
                {
                    Util.assertion(lastMatch                            != 0,  "CHECK-NEXT can't be the first check in a file");


                    int numNewlines = countNumNewlinesBetween(buffer, lastMatch,
                            ptr);
                    if (numNewlines == 0)
                    {
                        sm.printMessage(str.loc, CheckPrefix.value
                                        + "-NEXT: is on the same line as previous match",
                                "error");
                        sm.printMessage(SourceMgr.SMLoc.get(buffer, ptr),
                                "'next' amtch was here", "note");
                        sm.printMessage(SourceMgr.SMLoc.get(buffer, lastMatch),
                                "previous match was here", "note");
                        System.exit(1);
                    }

                    if (numNewlines != 1)
                    {
                        sm.printMessage(str.loc, CheckPrefix.value
                                        + "-NEXT: is not on the line after previous match",
                                "error");
                        sm.printMessage(SourceMgr.SMLoc.get(buffer, ptr),
                                "'next' amtch was here", "note");
                        sm.printMessage(SourceMgr.SMLoc.get(buffer, lastMatch),
                                "previous match was here", "note");
                    }
                }
                lastMatch = ptr;
                curPtr = ptr + str.str.length();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
