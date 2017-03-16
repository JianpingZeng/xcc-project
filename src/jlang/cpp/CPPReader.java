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

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class CPPReader implements AutoCloseable
{
    private final Preprocessor cpp;
    private String token;
    private int idx;

    public CPPReader(Preprocessor p)
    {
        cpp = p;
        token = "";
        idx = 0;
    }

    private boolean refill() throws Exception
    {
        try
        {
            assert cpp != null : "cpp is null : was it closed?";
            if (token == null)
                return false;
            while (idx >= token.length())
            {
                PPToken tok = cpp.token();
                switch (tok.getPPTokenKind())
                {
                    case EOF:
                        token = null;
                        return false;
                    case CCOMMENT:
                    case CPPCOMMENT:
                        if (!/*cpp.getFeature(Feature.KEEPCOMMENTS)*/true)
                        {
                            token = " ";
                            break;
                        }
                    default:
                        token = tok.getText();
                        break;
                }
                idx = 0;
            }
            return true;
        }
        catch (LexerException e)
        {
            throw new IOException(String.valueOf(e), e);
        }
    }

    public int read() throws Exception
    {
        if (!refill())
            return -1;
        return token.charAt(idx++);
    }

    /* XXX Very slow and inefficient. */
    public int read(char cbuf[], int off, int len) throws Exception
    {
        if (token == null)
            return -1;
        for (int i = 0; i < len; i++)
        {
            int ch = read();
            if (ch == -1)
                return i;
            cbuf[off + i] = (char) ch;
        }
        return len;
    }

    @Override public void close() throws Exception
    {
        cpp.close();
        token = null;
    }
}
