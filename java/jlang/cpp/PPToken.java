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
 * @author Xlous.zeng
 * @version 0.1
 */
public final class PPToken
{
    private SourceLocation loc;
    private PPTokenKind ppTokenKind;
    private final Object value;
    private final String text;

    public static final PPToken space = new PPToken(PPTokenKind.WHITESPACE);

    public PPToken(PPTokenKind kind, SourceLocation loc, Object val, String txt)
    {
        ppTokenKind = kind;
        this.loc = loc;
        value = val;
        text = txt;
    }

    public PPToken(PPTokenKind kind, SourceLocation loc, String txt)
    {
        this(kind, loc, null, txt);
    }

    public PPToken(PPTokenKind kind, String filename, int line, int col, Object val, String txt)
    {
        this(kind, new SourceLocation(filename, line, col), val, txt);
    }

    public PPToken(PPTokenKind kind, int line, int col, Object val, String txt)
    {
        this(kind, new SourceLocation(line, col), val, txt);
    }

    public PPToken(PPTokenKind kind, String txt, Object val)
    {
        this(kind, -1, -1, val, txt);
    }

    public PPToken(PPTokenKind kind, String txt)
    {
        this(kind, txt, null);
    }

    public PPToken(PPTokenKind kind)
    {
        this(kind, kind.text);
    }

    public SourceLocation getLocation()
    {
        return loc;
    }

    public void setLocation(SourceLocation newLoc)
    {
        this.loc = newLoc;
    }

    public PPTokenKind getPPTokenKind()
    {
        return ppTokenKind;
    }

    public Object getValue()
    {
        return value;
    }

    public String getText()
    {
        return text;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append('[').append(ppTokenKind.text);
        if (loc.line != -1)
        {
            sb.append('@').append(loc.line);
            if (loc.column != -1)
                sb.append(',').append(loc.column);
        }
        sb.append("]:");
        if (text!= null && !text.isEmpty())
            sb.append('"').append(text).append('"');
        else
            sb.append(ppTokenKind.toString());
        if (value != null)
            sb.append('=').append(value);
        return sb.toString();
    }
}
