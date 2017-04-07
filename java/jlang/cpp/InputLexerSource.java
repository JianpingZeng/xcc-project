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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

/**
 * A {@link Source} which lexes an {@link InputStream}.
 * <p>
 * The input is buffered.
 *
 * @author Xlous.zeng
 * @version 0.1
 * @see Source
 */
public class InputLexerSource extends LexerSource
{
    /**
     * Creates a new Source for lexing the given Reader.
     * <p>
     * Preprocessor directives are honoured within the file.
     */
    public InputLexerSource(InputStream input, Charset charset)
    {
        this(new InputStreamReader(input, charset));
    }

    public InputLexerSource(Reader input)
    {
        super(toBufferedReader(input), true);
    }

    @Override
    public String getPath()
    {
        return "<standard-input>";
    }

    @Override
    public String getName()
    {
        return "standard input";
    }

    @Override
    public String toString()
    {
        return String.valueOf(getPath());
    }
}

