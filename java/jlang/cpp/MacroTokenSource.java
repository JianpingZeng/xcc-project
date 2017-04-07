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
import java.util.List;
import static jlang.cpp.PPTokenKind.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
/* This source should always be active, since we don't expand macros
 * in any inactive context. */
public class MacroTokenSource extends Source
{
    private final Macro macro;
    private final Iterator<PPToken> tokens;	/* Pointer into the macro.  */

    private final List<MacroArgument> args;	/* { unexpanded, expanded } */

    private Iterator<PPToken> arg;	/* "current expansion" */

    MacroTokenSource(Macro m, List<MacroArgument> args)
    {
        macro = m;
        tokens = m.getTokens().iterator();
        this.args = args;
        this.arg = null;
    }

    /* XXX Called from Preprocessor [ugly]. */
    /* pp */
    static void escape(StringBuilder buf, CharSequence cs)
    {
        if (buf == null)
            throw new NullPointerException("Buffer was null.");
        if (cs == null)
            throw new NullPointerException("CharSequence was null.");
        for (int i = 0; i < cs.length(); i++)
        {
            char c = cs.charAt(i);
            switch (c)
            {
                case '\\':
                    buf.append("\\\\");
                    break;
                case '"':
                    buf.append("\\\"");
                    break;
                case '\n':
                    buf.append("\\n");
                    break;
                case '\r':
                    buf.append("\\r");
                    break;
                default:
                    buf.append(c);
            }
        }
    }

    private void concat(StringBuilder buf, MacroArgument arg)
    {
        for (PPToken tok : arg)
        {
            buf.append(tok.getText());
        }
    }

    private PPToken stringify(PPToken pos, MacroArgument arg)
    {
        StringBuilder buf = new StringBuilder();
        concat(buf, arg);
        // System.out.println("Concat: " + arg + " -> " + buf);
        StringBuilder str = new StringBuilder("\"");
        escape(str, buf);
        str.append("\"");
        // System.out.println("Escape: " + buf + " -> " + str);
        return new PPToken(STRING, pos.getLocation(), str.toString(),
                buf.toString());
    }

    /**
     * Returns true if the given argumentIndex is the last argument of a variadic macro.
     *
     * @param argumentIndex The index of the argument to inspect.
     * @return true if the given argumentIndex is the last argument of a variadic macro.
     */
    private boolean isVariadicMacroArgs(int argumentIndex)
    {
        if (!macro.isVariadic())
            return false;
        return argumentIndex == args.size() - 1;
    }

    /* At this point, we have consumed the first M_PASTE.
     * @see Macro#addPaste(PPToken) */
    private void paste(PPToken ptok) throws IOException, LexerException
    {
        // List<PPToken> out = new ArrayList<PPToken>();
        StringBuilder buf = new StringBuilder();
        // PPToken err = null;
        /* We know here that arg is null or expired,
         * since we cannot paste an expanded arg. */

        int count = 2;
        // While I hate auxiliary booleans, this does actually seem to be the simplest solution,
        // as it avoids duplicating all the logic around hasNext() in case COMMA.
        boolean comma = false;
        TOKEN:
        for (int i = 0; i < count; i++)
        {
            if (!tokens.hasNext())
            {
                /* XXX This one really should throw. */
                error(ptok.getLocation(),
                        "Paste at end of expansion");
                buf.append(' ').append(ptok.getText());
                break;
            }
            PPToken tok = tokens.next();
            // System.out.println("Paste " + tok);
            switch (tok.getPPTokenKind())
            {
                case M_PASTE:
                    /* One extra to paste, plus one because the
                     * paste token didn't count. */
                    count += 2;
                    ptok = tok;
                    break;
                case M_ARG:
                    int idx = (Integer) tok.getValue();
                    MacroArgument arg = args.get(idx);
                    if (comma && isVariadicMacroArgs(idx) && arg.isEmpty())
                    {
                        // Ugly way to strip the comma.
                        buf.setLength(buf.length() - 1);
                    }
                    else
                    {
                        concat(buf, arg);
                    }
                    break;
                /* XXX Test this. */
                case CCOMMENT:
                case CPPCOMMENT:
                    // TODO: In cpp, -CC keeps these comments too,
                    // but turns all C++ comments into C comments.
                    break;
                case COMMA:
                    comma = true;
                    buf.append(tok.getText());
                    continue TOKEN;
                default:
                    buf.append(tok.getText());
                    break;
            }
            comma = false;
        }

        /* Push and re-lex. */
        /*
         StringBuilder		src = new StringBuilder();
         escape(src, buf);
         StringLexerSource	sl = new StringLexerSource(src.toString());
         */
        StringLexerSource sl = new StringLexerSource(buf.toString());

        /* XXX Check that concatenation produces a valid token. */
        arg = new SourceIterator(sl);
    }

    @Override
    public PPToken token() throws IOException, LexerException
    {
        for (; ; )
        {
            /* Deal with lexed tokens first. */

            if (arg != null)
            {
                if (arg.hasNext())
                {
                    PPToken tok = arg.next();
                    /* XXX PASTE -> INVALID. */
                    assert tok.getPPTokenKind() != M_PASTE : "Unexpected paste token";
                    return tok;
                }
                arg = null;
            }

            if (!tokens.hasNext())
                return new PPToken(EOF, new SourceLocation(-1, -1),
                        "");	/* End of macro. */

            PPToken tok = tokens.next();
            int idx;
            switch (tok.getPPTokenKind())
            {
                case M_STRING:
                    /* Use the nonexpanded arg. */
                    idx = (Integer) tok.getValue();
                    return stringify(tok, args.get(idx));
                case M_ARG:
                    /* Expand the arg. */
                    idx = (Integer) tok.getValue();
                    // System.out.println("Pushing arg " + args.get(idx));
                    arg = args.get(idx).expansion();
                    break;
                case M_PASTE:
                    paste(tok);
                    break;
                default:
                    return tok;
            }
        } /* for */

    }

    @Override
    public void close() throws Exception
    {

    }

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append("expansion of ").append(macro.getName());
        Source parent = getParent();
        if (parent != null)
            buf.append(" in ").append(String.valueOf(parent));
        return buf.toString();
    }
}

