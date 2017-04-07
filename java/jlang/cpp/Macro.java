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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * This class encapsulates some useful information relative to Macro definition
 * in C language header file.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public final class Macro
{
    /**
     * The defined source where this macro stems.
     */
    private Source source;
    /**
     * The name of this defined macro.
     */
    private String name;

    /**
     * The list of argument name.
     */
    private ArrayList<String> args;

    /**
     * Indicates if this macro accept variable argument.
     */
    private boolean variadic;

    /**
     * The
     */
    private LinkedList<PPToken> tokens;

    public Macro(Source source, String name)
    {
        this.source = source;
        this.name = name;
        tokens = new LinkedList<>();
    }

    public Macro(String name)
    {
        this(null, name);
    }

    public Source getSource()
    {
        return source;
    }

    public void setSource(Source source)
    {
        this.source = source;
    }

    public String getName()
    {
        return name;
    }

    public void setArgs(ArrayList<String> args)
    {
        this.args = args;
    }

    public ArrayList<String> getArgs()
    {
        return args;
    }

    public boolean isVariadic()
    {
        return variadic;
    }

    public void setVariadic(boolean variadic)
    {
        this.variadic = variadic;
    }

    public LinkedList<PPToken> getTokens()
    {
        return tokens;
    }

    public boolean isFunctional()
    {
        return args != null;
    }

    public void addToken(PPToken tok)
    {
        tokens.add(tok);
    }

    /**
     * Adds a "paste" operator to the expansion of this macro.
     *
     * A paste operator causes the next token added to be pasted
     * to the previous token when the macro is expanded.
     * It is an error for a macro to end with a paste token.
     */
    public void addPaste(PPToken tok)
    {
        tokens.addLast(tok);
    }

    /* Paste tokens are inserted before the first of the two pasted
     * tokens, so it's a kind of bytecode notation. This method
     * swaps them around again. We know that there will never be two
     * sequential paste tokens, so a boolean is sufficient.
     */
    public String getText()
    {
        StringBuilder buf = new StringBuilder();
        boolean paste = false;
        for (PPToken tok : tokens)
        {
            if (tok.getPPTokenKind() == PPTokenKind.M_PASTE)
            {
                assert !paste: "Two sequential pastes.";
                paste = true;
                continue;
            }
            else
            {
                buf.append(tok.getText());
            }
            if (paste)
            {
                buf.append(" #").append("# ");
                paste = false;
            }
        }
        return buf.toString();
    }

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder(name);
        if (args != null)
        {
            buf.append('(');
            Iterator<String> it = args.iterator();
            while (it.hasNext())
            {
                buf.append(it.next());
                if (it.hasNext())
                    buf.append(", ");
                else if (isVariadic())
                    buf.append("...");
            }
            buf.append(')');
        }
        if (!tokens.isEmpty())
        {
            buf.append(" => ").append(getText());
        }
        return buf.toString();
    }

    public int getNumOfArgs()
    {
        return args.size();
    }
}
