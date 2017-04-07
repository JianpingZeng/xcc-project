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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A macro argument.
 * This encapsulates a raw and preprocessed token stream.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class MacroArgument extends ArrayList<PPToken>
{
    private List<PPToken> expansion;

    public MacroArgument()
    {
        expansion = null;
    }

    public void addPPToken(PPToken tok)
    {
        add(tok);
    }

    void expand(Preprocessor p) throws IOException, LexerException
    {
        /* Cache expansion. */
        if (expansion == null)
        {
            this.expansion = p.expand(this);
        }
    }

    public Iterator<PPToken> expansion()
    {
        return expansion.iterator();
    }

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append("Argument(");
        buf.append("raw=[ ");
        for (int i = 0; i < size(); i++)
            buf.append(get(i).getText());
        buf.append(" ];expansion=[ ");
        if (expansion == null)
            buf.append("null");
        else
            for (PPToken token : expansion)
                buf.append(token.getText());
        buf.append(" ])");
        return buf.toString();
    }
}

