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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * A {@link Source} which lexes a file.
 * <p>
 * The input is buffered.
 *
 * @see Source
 */
public class FileLexerSource extends InputLexerSource
{
    private final String path;

    /**
     * Creates a new Source for lexing the given File.
     * <p>
     * Preprocessor directives are honoured within the file.
     */
    public FileLexerSource(InputStream os, Charset charset, String path)
            throws IOException
    {
        super(os, charset);
        this.path = path;
    }

    public FileLexerSource(InputStream is, String path) throws IOException
    {
        this(is, Charset.defaultCharset(), path);
    }

    /**
     * This is not necessarily the same as getFilename().getPath() in case we are in a chroot.
     */
    @Override
    public String getPath()
    {
        return path;
    }

    @Override
    public String getName()
    {
        return getPath();
    }

    @Override
    public String toString()
    {
        return "file " + getPath();
    }
}

