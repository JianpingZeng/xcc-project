package jlang.support;
/*
 * Extremely C language Compiler.
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

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class LineEntry
{
    /// fileOffset - The offset in this file that the line entry occurs at.
    public int fileOffset;

    /// lineNo - The presumed line number of this line entry: #line 4.
    public int lineNo;

    /// filenameID - The ID of the filename identified by this line entry:
    /// #line 4 "foo.c".  This is -1 if not specified.
    public int filenameID;

    /// Flags - Set the 0 if no flags, 1 if a system header,
    public CharacteristicKind fileKind;

    /// includeOffset - This is the offset of the virtual include stack location,
    /// which is manipulated by GNU linemarker directives.  If this is 0 then
    /// there is no virtual #includer.
    public int includeOffset;

    public static LineEntry get(int offs, int line, int filename,
            CharacteristicKind fileKind, int includeOffset)
    {
        LineEntry entry = new LineEntry();
        entry.fileOffset = offs;
        entry.lineNo = line;
        entry.filenameID = filename;
        entry.fileKind = fileKind;
        entry.includeOffset = includeOffset;
        return entry;
    }
}
