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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class Source implements Iterable<PPToken>, AutoCloseable
{
    private Source parent;
    private boolean autopop;
    private boolean active;
    private Preprocessor pp;

    public void init(Preprocessor pp)
    {
        this.pp = pp;
    }

    /**
     * Obtain the parent source of this source.
     *
     * @return
     */
    public Source getParent()
    {
        return parent;
    }

    /**
     * Set the parent source of this source.
     *
     * @param parent
     */
    public void setParent(Source parent, boolean autopop)
    {
        this.parent = parent;
        this.autopop = autopop;
    }

    /**
     * Obtains the file path corresponding to this source (this is just
     * valid for FileLexerSource, return null for other kind of source).
     *
     * @return
     */
    public String getPath()
    {
        Source parent = getParent();
        if (parent != null)
            return parent.getPath();
        return null;
    }

    /**
     * It behaves like the method {@linkplain #getPath()}.
     *
     * @return
     */
    public String getName()
    {
        Source parent = getParent();
        if (parent != null)
            return parent.getName();
        return null;
    }

    /**
     * Obtains the line number within this source, its behaviour like {@linkplain
     * #getPath()}.
     *
     * @return
     */
    public int getLine()
    {
        Source parent = getParent();
        if (parent != null)
            return parent.getLine();
        return 0;
    }

    public int getColumn()
    {
        Source parent = getParent();
        if (parent != null)
            return parent.getColumn();
        return 0;
    }

    /**
     * Checks whether this source is expanding the given macro.
     * This method is used for prohibiting macro expanding recursively.
     * @param m Return true so does it is.
     * @return
     */
    public boolean isExpanding(Macro m)
    {
        Source parent = getParent();
        if (parent != null)
            return parent.isExpanding(m);
        return false;
    }

    public boolean isAutopop()
    {
        return autopop;
    }

    /**
     * Return true if this source has line number.
     * @return
     */
    public boolean isNumbered()
    {
        return false;
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }

    public boolean isActive()
    {
        return active;
    }

    /**
     * Return the next token lexed from this source.
     * @return
     * @throws IOException
     * @throws LexerException
     */
    public abstract PPToken token() throws IOException, LexerException;

    @Override
    public Iterator<PPToken> iterator()
    {
        return new SourceIterator(this);
    }

    /**
     * Advance the token stream until reaching the new line.
     * @param white
     * @return The NewLine PPToken.
     */
    public PPToken skipUntilNewLine(boolean white)
            throws IOException, LexerException
    {
        while (true)
        {
            PPToken t = token();
            switch (t.getPPTokenKind())
            {
                case EOF:
                    // TODO issure the warning message.
                    warning(t.getLocation(),"No newline before end of file");
                    return new PPToken(PPTokenKind.NL, t.getLocation(), File.separator);
                case NL:
                    return t;
                case CCOMMENT:
                case CPPCOMMENT:
                case WHITESPACE:
                    break;
                default:
                    if (white)
                    {
                        // TODO issure warning message.
                        warning(t.getLocation(), "Unexpected non white token");
                    }
                    break;
            }
        }
    }

    protected void error(SourceLocation loc, String msg)
    {}

    protected void warning(SourceLocation loc, String msg)
    {}

    protected void error(int line, int col, String msg)
    {}

    protected void warning(int line, int col, String msg)
    {}

    @Override
    public abstract void close() throws Exception;
}
