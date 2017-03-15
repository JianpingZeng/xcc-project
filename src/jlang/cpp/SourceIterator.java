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
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class SourceIterator implements Iterator<PPToken>
{
    private final Source source;
    private PPToken tok;

    public SourceIterator(Source source)
    {
        this.source = source;
    }

    private void advance()
    {
        try
        {
            if (tok == null)
                tok = source.token();
        }
        catch (LexerException | IOException ex)
        {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Returns {@code true} if the iteration has more elements.
     * (In other words, returns {@code true} if {@link #next} would
     * return an element rather than throwing an exception.)
     *
     * @return {@code true} if the iteration has more elements
     */
    @Override public boolean hasNext()
    {
        advance();
        return tok.getPPTokenKind() != PPTokenKind.EOF;
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    @Override public PPToken next() throws NoSuchElementException
    {
        if (!hasNext())
            throw new NoSuchElementException();
        PPToken t = tok;
        tok = null;
        return t;
    }
}
