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

import java.io.IOException;
import java.io.Reader;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
class JoinReader /* extends Reader */ implements AutoCloseable
{
    private final Reader in;
    private LexerSource source;
    private boolean trigraphs;
    private boolean warnings;

    private int newlines;
    private boolean flushnl;
    private int[] unget;
    private int uptr;

    public JoinReader(Reader in, boolean trigraphs)
    {
        this.in = in;
        this.trigraphs = trigraphs;
        this.newlines = 0;
        this.flushnl = false;
        this.unget = new int[2];
        this.uptr = 0;
    }

    public JoinReader(Reader in)
    {
        this(in, false);
    }

    public void setTrigraphs(boolean enable, boolean warnings)
    {
        this.trigraphs = enable;
        this.warnings = warnings;
    }

    void init(Preprocessor pp, LexerSource s)
    {
        this.source = s;
        setTrigraphs(true, true);
    }

    private int __read() throws IOException
    {
        if (uptr > 0)
            return unget[--uptr];
        return in.read();
    }

    private void _unread(int c)
    {
        if (c != -1)
            unget[uptr++] = c;
        assert uptr <= unget.length : "JoinReader ungets too many characters";
    }

    protected void warning(String msg) throws LexerException
    {
        if (source != null)
            source.warning(msg);
        else
            throw new LexerException(msg);
    }

    private char trigraph(char raw, char repl)
            throws IOException, LexerException
    {
        if (trigraphs)
        {
            if (warnings)
                warning("trigraph ??" + raw + " converted to " + repl);
            return repl;
        }
        else
        {
            if (warnings)
                warning("trigraph ??" + raw + " ignored");
            _unread(raw);
            _unread('?');
            return '?';
        }
    }

    private int _read() throws IOException, LexerException
    {
        int c = __read();
        if (c == '?' && (trigraphs || warnings))
        {
            int d = __read();
            if (d == '?')
            {
                int e = __read();
                switch (e)
                {
                    case '(':
                        return trigraph('(', '[');
                    case ')':
                        return trigraph(')', ']');
                    case '<':
                        return trigraph('<', '{');
                    case '>':
                        return trigraph('>', '}');
                    case '=':
                        return trigraph('=', '#');
                    case '/':
                        return trigraph('/', '\\');
                    case '\'':
                        return trigraph('\'', '^');
                    case '!':
                        return trigraph('!', '|');
                    case '-':
                        return trigraph('-', '~');
                }
                _unread(e);
            }
            _unread(d);
        }
        return c;
    }

    public int read() throws IOException, LexerException
    {
        if (flushnl)
        {
            if (newlines > 0)
            {
                newlines--;
                return '\n';
            }
            flushnl = false;
        }

        for (; ; )
        {
            int c = _read();
            switch (c)
            {
                case '\\':
                    int d = _read();
                    switch (d)
                    {
                        case '\n':
                            newlines++;
                            continue;
                        case '\r':
                            newlines++;
                            int e = _read();
                            if (e != '\n')
                                _unread(e);
                            continue;
                        default:
                            _unread(d);
                            return c;
                    }
                case '\r':
                case '\n':
                case '\u2028':
                case '\u2029':
                case '\u000B':
                case '\u000C':
                case '\u0085':
                    flushnl = true;
                    return c;
                case -1:
                    if (newlines > 0)
                    {
                        newlines--;
                        return '\n';
                    }
                default:
                    return c;
            }
        }
    }

    public int read(char cbuf[], int off, int len)
            throws IOException, LexerException
    {
        for (int i = 0; i < len; i++)
        {
            int ch = read();
            if (ch == -1)
                return i;
            cbuf[off + i] = (char) ch;
        }
        return len;
    }

    @Override public void close() throws IOException
    {
        in.close();
    }

    @Override public String toString()
    {
        return "JoinReader(nl=" + newlines + ")";
    }
}

