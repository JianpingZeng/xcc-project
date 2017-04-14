package jlang.cpp;
/*
 * Extremely C language Compiler
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

import jlang.basic.HeaderSearch;
import jlang.basic.LangOptions;
import jlang.diag.Diagnostic;
import jlang.diag.FullSourceLoc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static jlang.cpp.PPTokenKind.*;
import static jlang.cpp.PredefinedDirectiveCommand.CPP_ERROR;

/**
 * <p>
 * A C Preprocessor.
 * The Preprocessor outputs a token stream which does not need
 * re-lexing for C or C++. Alternatively, the output text may be
 * reconstructed by concatenating the {@link PPToken#getText() text}
 * values of the returned {@link PPToken Tokens}.
 * </p>
 * <pre>
 * Source file name and line number information is conveyed by lines of the form:
 *
 * # linenum filename flags
 *
 * These are called linemarkers. They are inserted as needed into
 * the output (but never within a string or character constant). They
 * mean that the following line originated in file filename at line
 * linenum. filename will never contain any non-printing characters;
 * they are replaced with octal escape sequences.
 * </pre>
 * <p>
 * After the file name comes zero or more flags, which are `1', `2',
 * `3', or `4'. If there are multiple flags, spaces separate them. Here
 * is what the flags mean:
 * <ol>
 *     <li>This indicates the start of a new file.</li>
 *     <li>This indicates returning to a file (after having included another
 * file).</li>
 *     <li>This indicates that the following text comes from a system header
 * file, so certain warnings should be suppressed.</li>
 *     <li>This indicates that the following text should be treated as being
 * wrapped in an implicit extern "C" block.</li>
 * </ol>
 * </p>
 * @author Xlous.zeng
 * @version 0.1
 */
public final class Preprocessor implements AutoCloseable
{

    private static final Source INTERNAL = new Source()
    {
        @Override
        public PPToken token() throws IOException, LexerException
        {
            throw new LexerException("Cannot read from " + getName());
        }

        @Override
        public void close() throws Exception
        {

        }

        @Override
        public String getPath()
        {
            return "<internal-data>";
        }

        @Override
        public String getName()
        {
            return "internal data";
        }
    };
    
    private Diagnostic diags;
    private LangOptions langInfo;
    private HeaderSearch headers;

    /**
     * This string is the predefined macros that preprocessor
     * should use from the command line etc.
     */
    private String predefines;

    private static final Macro __LINE__ = new Macro(INTERNAL, "__LINE__");
    private static final Macro __FILE__ = new Macro(INTERNAL, "__FILE__");
    private static final Macro __COUNTER__ = new Macro(INTERNAL, "__COUNTER__");

    private final List<Source> inputs;

    /* The fundamental engine. */
    private final Map<String, Macro> macros;

    private final Stack<State> states;
    private Source source;

    /* Miscellaneous support. */
    private int counter;
    private final Set<String> onceseenpaths = new HashSet<>();
    private final List<VirtualFile> includes = new ArrayList<VirtualFile>();

    /* Support junk to make it work like cpp */
    private List<String> quoteincludepath;	/* -iquote */

    private List<String> sysincludepath;		/* -I */
    private VirtualFileSystem fileSystem;

    private String inputFile;

    public LangOptions getLangOption()
    {
        return langInfo;
    }

    public Diagnostic getDiagnostics()
    {
        return diags;
    }

    public String getInputFile()
    {
        return inputFile;
    }

    public String getPredefines()
    {
        return predefines;
    }

    public void setPredefines(String predefines)
    {
        this.predefines = predefines;
    }

    public List<PPToken> expand(MacroArgument macroMacroArgument)
    {
        return Collections.emptyList();
    }

    public Preprocessor(Diagnostic diag,
            LangOptions langOptions,
            HeaderSearch headerSearch)
    {
        this.diags = diag;
        langInfo = langOptions;
        headers = headerSearch;
        inputs = new ArrayList<>();
        macros = new HashMap<>();
        macros.put(__LINE__.getName(), __LINE__);
        macros.put(__FILE__.getName(), __FILE__);
        macros.put(__COUNTER__.getName(), __COUNTER__);
        states = new Stack<>();
        states.push(new State());
        source = new StringLexerSource(getPredefines());

        quoteincludepath = headerSearch.getQuotedPaths();
        sysincludepath = headerSearch.getSystemPaths();
        fileSystem = new JavaFileSystem();
    }

    /**
     * Adds input for the Preprocessor.
     * <p>
     * Inputs are processed in the order in which they are added.
     */
    private void addInput(Source source)
    {
        source.init(this);
        inputs.add(source);
    }

    /**
     * Adds input for the Preprocessor.
     *
     * @see #addInput(Source)
     */
    public void addInput(InputStream is, String name) throws IOException
    {
        inputFile = name;
        addInput(new FileLexerSource(is, name));
    }

    /**
     * Handles an error.
     * <p>
     * If a PreprocessorListener is installed, it receives the
     * error. Otherwise, an exception is thrown.
     */
    protected void error(SourceLocation loc, String msg) throws LexerException
    {
        diags.report(new FullSourceLoc(loc, inputFile), 0);
    }

    /**
     * Handles an error.
     * <p>
     * If a PreprocessorListener is installed, it receives the
     * error. Otherwise, an exception is thrown.
     *
     */
    protected void error(PPToken tok, String msg) throws LexerException
    {
        error(tok.getLocation(), msg);
    }

    /**
     * Handles a warning.
     * <p>
     * If a PreprocessorListener is installed, it receives the
     * warning. Otherwise, an exception is thrown.
     */
    protected void warning(SourceLocation loc, String msg)
            throws LexerException
    {

    }

    /**
     * Handles a warning.
     * <p>
     * If a PreprocessorListener is installed, it receives the
     * warning. Otherwise, an exception is thrown.
     *
     */
    protected void warning(PPToken tok, String msg) throws LexerException
    {
        warning(tok.getLocation(), msg);
    }

    /**
     * Adds a Macro to this Preprocessor.
     * <p>
     * The given {@link Macro} object encapsulates both the name
     * and the expansion.
     *
     * @throws LexerException if the definition fails or is otherwise illegal.
     */
    public void addMacro(Macro m) throws LexerException
    {
        // System.out.println("Macro " + m);
        String name = m.getName();
        /* Already handled as a source error in macro(). */
        if ("defined".equals(name))
            throw new LexerException("Cannot redefine name 'defined'");
        macros.put(m.getName(), m);
    }

    /**
     * Defines the given name as a macro.
     * <p>
     * The String value is lexed into a token stream, which is
     * used as the macro expansion.
     *
     * @throws LexerException if the definition fails or is otherwise illegal.
     */
    public void addMacro(String name, String value) throws LexerException
    {
        try
        {
            Macro m = new Macro(name);
            StringLexerSource s = new StringLexerSource(value);
            for (; ; )
            {
                PPToken tok = s.token();
                if (tok.getPPTokenKind() == EOF)
                    break;
                m.addToken(tok);
            }
            addMacro(m);
        }
        catch (IOException e)
        {
            throw new LexerException(e);
        }
    }

    /**
     * Defines the given name as a macro, with the value <code>1</code>.
     * <p>
     * This is a convnience method, and is equivalent to
     * <code>addMacro(name, "1")</code>.
     *
     * @throws LexerException if the definition fails or is otherwise illegal.
     */
    public void addMacro(String name) throws LexerException
    {
        addMacro(name, "1");
    }

    /**
     * Sets the user include path used by this Preprocessor.
     */
    /* Note for future: Create an IncludeHandler? */
    public void setQuoteIncludePath(List<String> path)
    {
        this.quoteincludepath = path;
    }

    /**
     * Returns the user include-path of this Preprocessor.
     * <p>
     * This list may be freely modified by user code.
     */
    public List<String> getQuoteIncludePath()
    {
        return quoteincludepath;
    }

    /**
     * Sets the system include path used by this Preprocessor.
     */
    /* Note for future: Create an IncludeHandler? */
    public void setSystemIncludePath(List<String> path)
    {
        this.sysincludepath = path;
    }

    /**
     * Returns the system include-path of this Preprocessor.
     * <p>
     * This list may be freely modified by user code.
     */
    public List<String> getSystemIncludePath()
    {
        return sysincludepath;
    }

    /**
     * Returns the Map of Macros parsed during the run of this
     * Preprocessor.
     *
     * @return The {@link Map} of macros currently defined.
     */
    public Map<String, Macro> getMacros()
    {
        return macros;
    }

    /**
     * Returns the named macro.
     * <p>
     * While you can modify the returned object, unexpected things
     * might happen if you do.
     *
     * @return the Macro object, or null if not found.
     */
    public Macro getMacro(String name)
    {
        return macros.get(name);
    }

    /* States */
    private void push_state()
    {
        State top = states.peek();
        states.push(new State(top));
    }

    private void pop_state() throws LexerException
    {
        State s = states.pop();
        if (states.isEmpty())
        {
            error(new SourceLocation(0, 0), "#" + "endif without #" + "if");
            states.push(s);
        }
    }

    private boolean isActive()
    {
        State state = states.peek();
        return state.isParentActive() && state.isActive();
    }


    /* Sources */

    /**
     * Returns the top Source on the input stack.
     *
     * @return the top Source on the input stack.
     * @see Source
     * @see #push_source(Source, boolean)
     * @see #pop_source()
     */
    // 
    protected Source getSource()
    {
        return source;
    }

    /**
     * Pushes a Source onto the input stack.
     *
     * @param source  the new Source to push onto the top of the input stack.
     * @param autopop if true, the Source is automatically removed from the input stack at EOF.
     * @see #getSource()
     * @see #pop_source()
     */
    protected void push_source(Source source, boolean autopop)
    {
        source.init(this);
        source.setParent(this.source, autopop);
    }

    /**
     * Pops a Source from the input stack.
     *
     * @param linemarker TODO: currently ignored, might be a bug?
     * @throws IOException if an I/O error occurs.
     * @see #getSource()
     * @see #push_source(Source, boolean)
     */
    protected PPToken pop_source(boolean linemarker) throws Exception
    {
        Source s = this.source;
        this.source = s.getParent();
        /* Always a noop unless called externally. */
        s.close();

        Source t = getSource();
        if (langInfo.gnuMode && s.isNumbered() && t != null)
        {
            /* We actually want 'did the nested source
             * contain a newline token', which isNumbered()
             * approximates. This is not perfect, but works. */
            return line_token(t.getLine(), t.getName(), " 2");
        }

        return null;
    }

    protected void pop_source() throws Exception
    {
        pop_source(false);
    }

    private PPToken next_source()
    {
        if (inputs.isEmpty())
            return new PPToken(EOF);
        Source s = inputs.remove(0);
        push_source(s, true);
        return line_token(s.getLine(), s.getName(), " 1");
    }

    /* Source tokens */
    private PPToken source_token;

    /* XXX Make this include the NL, and make all cpp directives eat
     * their own NL. */
    private PPToken line_token(int line, String name, String extra)
    {
        StringBuilder buf = new StringBuilder();
        buf.append("#line ").append(line).append(" \"");
        /* XXX This call to escape(name) is correct but ugly. */
        if (name == null)
            buf.append("<no file>");
        else
            MacroTokenSource.escape(buf, name);
        buf.append("\"").append(extra).append("\n");
        return new PPToken(P_LINE, line, 0, buf.toString(), null);
    }

    private PPToken source_token() throws Exception
    {
        if (source_token != null)
        {
            PPToken tok = source_token;
            source_token = null;
            return tok;
        }

        for (; ; )
        {
            Source s = getSource();
            if (s == null)
            {
                PPToken t = next_source();
                if (t.getPPTokenKind() == P_LINE && !langInfo.gnuMode)
                    continue;
                return t;
            }
            PPToken tok = s.token();
            /* XXX Refactor with skipline() */
            if (tok.getPPTokenKind() == EOF && s.isAutopop())
            {
                // System.out.println("Autopop " + s);
                PPToken mark = pop_source(true);
                if (mark != null)
                    return mark;
                continue;
            }
            return tok;
        }
    }

    private void source_untoken(PPToken tok)
    {
        if (this.source_token != null)
            throw new IllegalStateException("Cannot return two tokens");
        this.source_token = tok;
    }

    private boolean isWhite(PPToken tok)
    {
        PPTokenKind type = tok.getPPTokenKind();
        return (type == WHITESPACE) || (type == CCOMMENT) || (type
                == CPPCOMMENT);
    }

    private PPToken source_token_nonwhite() throws Exception
    {
        PPToken tok;
        do
        {
            tok = source_token();
        } while (isWhite(tok));
        return tok;
    }

    /**
     * Returns an NL or an EOF token.
     * <p>
     * The metadata on the token will be correct, which is better
     * than generating a new one.
     * <p>
     * This method can, as of recent patches, return a P_LINE token.
     */
    private PPToken source_skipline(boolean white)
            throws Exception
    {
        // (new Exception("skipping line")).printStackTrace(System.out);
        Source s = getSource();
        PPToken tok = s.skipUntilNewLine(white);
        /* XXX Refactor with source_token() */
        if (tok.getPPTokenKind() == EOF && s.isAutopop())
        {
            // System.out.println("Autopop " + s);
            PPToken mark = pop_source(true);
            if (mark != null)
                return mark;
        }
        return tok;
    }

    /* processes and expands a macro. */
    private boolean macro(Macro m, PPToken orig)
            throws Exception
    {
        PPToken tok;
        List<MacroArgument> args;

        // System.out.println("pp: expanding " + m);
        if (m.isFunctional())
        {
            OPEN:
            for (; ; )
            {
                tok = source_token();
                // System.out.println("pp: open: token is " + tok);
                switch (tok.getPPTokenKind())
                {
                    case WHITESPACE:	/* XXX Really? */

                    case CCOMMENT:
                    case CPPCOMMENT:
                    case NL:
                        break;	/* continue */

                    case LPAREN:
                        break OPEN;
                    default:
                        source_untoken(tok);
                        return false;
                }
            }

            // tok = expanded_token_nonwhite();
            tok = source_token_nonwhite();

            /* We either have, or we should have args.
             * This deals elegantly with the case that we have
             * one empty arg. */
            if (tok.getPPTokenKind() != RPAREN || m.getNumOfArgs() > 0)
            {
                args = new ArrayList<MacroArgument>();

                MacroArgument arg = new MacroArgument();
                int depth = 0;
                boolean space = false;

                ARGS:
                for (; ; )
                {
                    // System.out.println("pp: arg: token is " + tok);
                    switch (tok.getPPTokenKind())
                    {
                        case EOF:
                            error(tok, "EOF in macro args");
                            return false;

                        case COMMA:
                            if (depth == 0)
                            {
                                if (m.isVariadic() && /* We are building the last arg. */
                                        args.size() == m.getNumOfArgs() - 1)
                                {
                                    /* Just add the comma. */
                                    arg.addPPToken(tok);
                                }
                                else
                                {
                                    args.add(arg);
                                    arg = new MacroArgument();
                                }
                            }
                            else
                            {
                                arg.addPPToken(tok);
                            }
                            space = false;
                            break;
                        case RPAREN:
                            if (depth == 0)
                            {
                                args.add(arg);
                                break ARGS;
                            }
                            else
                            {
                                depth--;
                                arg.addPPToken(tok);
                            }
                            space = false;
                            break;
                        case LPAREN:
                            depth++;
                            arg.addPPToken(tok);
                            space = false;
                            break;

                        case WHITESPACE:
                        case CCOMMENT:
                        case CPPCOMMENT:
                        case NL:
                            /* Avoid duplicating spaces. */
                            space = true;
                            break;

                        default:
                            /* Do not put space on the beginning of
                             * an argument token. */
                            if (space && !arg.isEmpty())
                                arg.addPPToken(
                                        new PPToken(PPTokenKind.WHITESPACE));
                            arg.addPPToken(tok);
                            space = false;
                            break;

                    }
                    // tok = expanded_token();
                    tok = source_token();
                }
                /* space may still be true here, thus trailing space
                 * is stripped from arguments. */

                if (args.size() != m.getNumOfArgs())
                {
                    if (m.isVariadic())
                    {
                        if (args.size() == m.getNumOfArgs() - 1)
                        {
                            args.add(new MacroArgument());
                        }
                        else
                        {
                            error(tok, "variadic macro " + m.getName()
                                    + " has at least " + (m.getNumOfArgs() - 1)
                                    + " parameters " + "but given " + args
                                    .size() + " args");
                            return false;
                        }
                    }
                    else
                    {
                        error(tok,
                                "macro " + m.getName() + " has " + m.getArgs()
                                        + " parameters " + "but given " + args
                                        .size() + " args");
                        /* We could replay the arg tokens, but I
                         * note that GNU cpp does exactly what we do,
                         * i.e. output the macro name and chew the args.
                         */
                        return false;
                    }
                }

                for (MacroArgument a : args)
                {
                    a.expand(this);
                }

                // System.out.println("Macro " + m + " args " + args);
            }
            else
            {
                /* nargs == 0 and we (correctly) got () */
                args = null;
            }

        }
        else
        {
            /* Macro without args. */
            args = null;
        }

        if (m == __LINE__)
        {
            push_source(new FixedTokenSource(
                    new PPToken(NUMBER, orig.getLocation(), new NumericValue(10,
                            String.valueOf(orig.getLocation().line)),
                            String.valueOf(orig.getLocation().line))), true);
        }
        else if (m == __FILE__)
        {
            StringBuilder buf = new StringBuilder("\"");
            String name = getSource().getName();
            if (name == null)
                name = "<no file>";
            for (int i = 0; i < name.length(); i++)
            {
                char c = name.charAt(i);
                switch (c)
                {
                    case '\\':
                        buf.append("\\\\");
                        break;
                    case '"':
                        buf.append("\\\"");
                        break;
                    default:
                        buf.append(c);
                        break;
                }
            }
            buf.append("\"");
            String text = buf.toString();
            push_source(new FixedTokenSource(
                    new PPToken(STRING, orig.getLocation(), text, text)), true);
        }
        else if (m == __COUNTER__)
        {
            /* This could equivalently have been done by adding
             * a special Macro subclass which overrides getPPTokens(). */
            int value = this.counter++;
            push_source(new FixedTokenSource(
                    new PPToken(NUMBER, orig.getLocation(),
                            new NumericValue(10, String.valueOf(value)),
                            String.valueOf(value))), true);
        }
        else
        {
            push_source(new MacroTokenSource(m, args), true);
        }

        return true;
    }

    /**
     * Expands an argument.
     */
    /* I'd rather this were done lazily, but doing so breaks spec. */
     List<PPToken> expand(List<PPToken> arg)
            throws Exception, LexerException
    {
        List<PPToken> expansion = new ArrayList<PPToken>();
        boolean space = false;

        push_source(new FixedTokenSource(arg), false);

        EXPANSION:
        for (; ; )
        {
            PPToken tok = expanded_token();
            switch (tok.getPPTokenKind())
            {
                case EOF:
                    break EXPANSION;

                case WHITESPACE:
                case CCOMMENT:
                case CPPCOMMENT:
                    space = true;
                    break;

                default:
                    if (space && !expansion.isEmpty())
                        expansion.add(new PPToken(PPTokenKind.WHITESPACE));
                    expansion.add(tok);
                    space = false;
                    break;
            }
        }

        // Always returns null.
        pop_source(false);

        return expansion;
    }

    /* processes a #define directive */
    private PPToken define() throws Exception
    {
        PPToken tok = source_token_nonwhite();
        if (tok.getPPTokenKind() != IDENTIFIER)
        {
            error(tok, "Expected identifier");
            return source_skipline(false);
        }
        /* if predefined */

        String name = tok.getText();
        if ("defined".equals(name))
        {
            error(tok, "Cannot redefine name 'defined'");
            return source_skipline(false);
        }

        Macro m = new Macro(getSource(), name);
        ArrayList<String> args;

        tok = source_token();
        if (tok.getPPTokenKind() == LPAREN)
        {
            tok = source_token_nonwhite();
            if (tok.getPPTokenKind() != RPAREN)
            {
                args = new ArrayList<String>();
                ARGS:
                for (; ; )
                {
                    switch (tok.getPPTokenKind())
                    {
                        case IDENTIFIER:
                            args.add(tok.getText());
                            break;
                        case ELLIPSIS:
                            // Unnamed Variadic macro
                            args.add("__VA_ARGS__");
                            // We just named the ellipsis, but we unget the token
                            // to allow the ELLIPSIS handling below to process it.
                            source_untoken(tok);
                            break;
                        case NL:
                        case EOF:
                            error(tok, "Unterminated macro parameter list");
                            return tok;
                        default:
                            error(tok, "error in macro parameters: " + tok
                                    .getText());
                            return source_skipline(false);
                    }
                    tok = source_token_nonwhite();
                    switch (tok.getPPTokenKind())
                    {
                        case COMMA:
                            break;
                        case ELLIPSIS:
                            tok = source_token_nonwhite();
                            if (tok.getPPTokenKind() != RPAREN)
                                error(tok, "ellipsis must be on last argument");
                            m.setVariadic(true);
                            break ARGS;
                        case RPAREN:
                            break ARGS;

                        case NL:
                        case EOF:
                            /* Do not skip line. */
                            error(tok, "Unterminated macro parameters");
                            return tok;
                        default:
                            error(tok, "Bad token in macro parameters: " + tok
                                    .getText());
                            return source_skipline(false);
                    }
                    tok = source_token_nonwhite();
                }
            }
            else
            {
                assert tok.getPPTokenKind() == RPAREN : "Expected RPAREN";
                args = new ArrayList<>();
            }

            m.setArgs(args);
        }
        else
        {
            /* For searching. */
            args = new ArrayList<>();
            source_untoken(tok);
        }

        /* Get an expansion for the macro, using indexOf. */
        boolean space = false;
        boolean paste = false;
        int idx;

        /* Ensure no space at start. */
        tok = source_token_nonwhite();
        EXPANSION:
        for (; ; )
        {
            switch (tok.getPPTokenKind())
            {
                case EOF:
                    break EXPANSION;
                case NL:
                    break EXPANSION;

                case CCOMMENT:
                case CPPCOMMENT:
                /* XXX This is where we implement GNU's cpp -CC. */
                    // break;
                case WHITESPACE:
                    if (!paste)
                        space = true;
                    break;

                /* Paste. */
                case PASTE:
                    space = false;
                    paste = true;
                    m.addPaste(
                            new PPToken(M_PASTE, tok.getLocation(),
                                    "#" + "#", null));
                    break;

                /* Stringify. */
                case HASH:
                    if (space)
                        m.addToken(PPToken.space);
                    space = false;
                    PPToken la = source_token_nonwhite();
                    if (la.getPPTokenKind() == IDENTIFIER && (
                            (idx = args.indexOf(la.getText())) != -1))
                    {
                        m.addToken(new PPToken(M_STRING, la.getLocation(),
                                "#" + la.getText(),
                                String.valueOf(idx)));
                    }
                    else
                    {
                        m.addToken(tok);
                        /* Allow for special processing. */
                        source_untoken(la);
                    }
                    break;

                case IDENTIFIER:
                    if (space)
                        m.addToken(PPToken.space);
                    space = false;
                    paste = false;
                    idx = args.indexOf(tok.getText());
                    if (idx == -1)
                        m.addToken(tok);
                    else
                        m.addToken(new PPToken(M_ARG, tok.getLocation(),
                                tok.getText(),
                                String.valueOf(idx)));
                    break;

                default:
                    if (space)
                        m.addToken(PPToken.space);
                    space = false;
                    paste = false;
                    m.addToken(tok);
                    break;
            }
            tok = source_token();
        }
        addMacro(m);

        return tok;	/* NL or EOF. */

    }

    private PPToken undef() throws Exception
    {
        PPToken tok = source_token_nonwhite();
        if (tok.getPPTokenKind() != IDENTIFIER)
        {
            error(tok, "Expected identifier, not " + tok.getText());
            if (tok.getPPTokenKind() == NL || tok.getPPTokenKind() == EOF)
                return tok;
        }
        else
        {
            Macro m = getMacro(tok.getText());
            if (m != null)
            {
                /* XXX error if predefined */
                macros.remove(m.getName());
            }
        }
        return source_skipline(true);
    }

    /**
     * Attempts to include the given file.
     * <p>
     * User code may override this method to implement a virtual
     * file system.
     *
     * @param file The VirtualFile to attempt to include.
     * @return true if the file was successfully included, false otherwise.
     * @throws IOException if an I/O error occurs.
     */
    protected boolean include(VirtualFile file) throws IOException
    {
        if (!file.isFile())
            return false;
        includes.add(file);
        push_source(file.getSource(), true);
        return true;
    }

    /**
     * Attempts to include a file from an include path, by name.
     *
     * @param path The list of virtual directories to search for the given name.
     * @param name The name of the file to attempt to include.
     * @return true if the file was successfully included, false otherwise.
     * @throws IOException if an I/O error occurs.
     */
    protected boolean include(Iterable<String> path, String name)
            throws IOException
    {
        for (String dir : path)
        {
            VirtualFile file = fileSystem.getFile(dir, name);
            if (include(file))
                return true;
        }
        return false;
    }

    /**
     * Handles an include directive.
     *
     * @throws IOException    if an I/O error occurs.
     * @throws LexerException if the include fails, and the error handler is fatal.
     */
    private void include(String parent, int line, String name, boolean quoted,
            boolean next) throws IOException, LexerException
    {
        if (name.startsWith("/"))
        {
            VirtualFile file = fileSystem.getFile(name);
            if (include(file))
                return;
            StringBuilder buf = new StringBuilder();
            buf.append("File not found: ").append(name);
            error(new SourceLocation(line, 0), buf.toString());
            return;
        }

        VirtualFile pdir = null;
        if (quoted)
        {
            if (parent != null)
            {
                VirtualFile pfile = fileSystem.getFile(parent);
                pdir = pfile.getParentFile();
            }
            if (pdir != null)
            {
                VirtualFile ifile = pdir.getChildFile(name);
                if (include(ifile))
                    return;
            }
            if (include(quoteincludepath, name))
                return;
        }

        if (include(sysincludepath, name))
            return;

        StringBuilder buf = new StringBuilder();
        buf.append("File not found: ").append(name);
        buf.append(" in");
        if (quoted)
        {
            buf.append(" .").append(LPAREN).append(pdir).append(RPAREN);
            for (String dir : quoteincludepath)
                buf.append(" ").append(dir);
        }
        for (String dir : sysincludepath)
            buf.append(" ").append(dir);
        error(new SourceLocation(line, 0), buf.toString());
    }

    private PPToken include(boolean next) throws Exception
    {
        LexerSource lexer = (LexerSource) source;
        try
        {
            lexer.setInclude(true);
            PPToken tok = token_nonwhite();

            String name;
            boolean quoted;

            if (tok.getPPTokenKind() == STRING)
            {
                /* XXX Use the original text, not the value.
                 * Backslashes must not be treated as escapes here. */
                StringBuilder buf = new StringBuilder((String) tok.getValue());
                HEADER:
                for (; ; )
                {
                    tok = token_nonwhite();
                    switch (tok.getPPTokenKind())
                    {
                        case STRING:
                            buf.append((String) tok.getValue());
                            break;
                        case NL:
                        case EOF:
                            break HEADER;
                        default:
                            warning(tok,
                                    "Unexpected token on #" + "include line");
                            return source_skipline(false);
                    }
                }
                name = buf.toString();
                quoted = true;
            }
            else if (tok.getPPTokenKind() == HEADER)
            {
                name = (String) tok.getValue();
                quoted = false;
                tok = source_skipline(true);
            }
            else
            {
                error(tok, "Expected string or header, not " + tok.getText());
                switch (tok.getPPTokenKind())
                {
                    case NL:
                    case EOF:
                        return tok;
                    default:
                        /* Only if not a NL or EOF already. */
                        return source_skipline(false);
                }
            }

            /* Do the inclusion. */
            include(source.getPath(), tok.getLocation().line, name, quoted, next);

            /* 'tok' is the 'nl' after the include. We use it after the
             * #line directive. */
            if (langInfo.gnuMode)
                return line_token(1, source.getName(), " 1");
            return tok;
        }
        finally
        {
            lexer.setInclude(false);
        }
    }

    protected void pragma_once(PPToken name) throws Exception, LexerException
    {
        Source s = this.source;
        if (!onceseenpaths.add(s.getPath()))
        {
            PPToken mark = pop_source(true);
            // FixedPPTokenSource should never generate a linemarker on exit.
            if (mark != null)
                push_source(new FixedTokenSource(Arrays.asList(mark)), true);
        }
    }

    protected void pragma(PPToken name, List<PPToken> value)
            throws Exception
    {
        if (/*getFeature(Feature.PRAGMA_ONCE)*/false)
        {
            if ("once".equals(name.getText()))
            {
                pragma_once(name);
                return;
            }
        }
        warning(name, "Unknown #" + "pragma: " + name.getText());
    }

    private PPToken pragma() throws Exception
    {
        PPToken name;

        NAME:
        for (; ; )
        {
            PPToken tok = source_token();
            switch (tok.getPPTokenKind())
            {
                case EOF:
                    /* There ought to be a newline before EOF.
                     * At least, in any skipline context. */
                    /* XXX Are we sure about this? */
                    warning(tok, "End of file in #" + "pragma");
                    return tok;
                case NL:
                    /* This may contain one or more newlines. */
                    warning(tok, "Empty #" + "pragma");
                    return tok;
                case CCOMMENT:
                case CPPCOMMENT:
                case WHITESPACE:
                    continue NAME;
                case IDENTIFIER:
                    name = tok;
                    break NAME;
                default:
                    warning(tok, "Illegal #" + "pragma " + tok.getText());
                    return source_skipline(false);
            }
        }

        PPToken tok;
        List<PPToken> value = new ArrayList<PPToken>();
        VALUE:
        for (; ; )
        {
            tok = source_token();
            switch (tok.getPPTokenKind())
            {
                case EOF:
                    /* There ought to be a newline before EOF.
                     * At least, in any skipline context. */
                    /* XXX Are we sure about this? */
                    warning(tok, "End of file in #" + "pragma");
                    break VALUE;
                case NL:
                    /* This may contain one or more newlines. */
                    break VALUE;
                case CCOMMENT:
                case CPPCOMMENT:
                    break;
                case WHITESPACE:
                    value.add(tok);
                    break;
                default:
                    value.add(tok);
                    break;
            }
        }

        pragma(name, value);

        return tok;	/* The NL. */

    }

    /* For #error and #warning. */
    private void error(PPToken pptok, boolean is_error)
            throws Exception
    {
        StringBuilder buf = new StringBuilder();
        buf.append('#').append(pptok.getText()).append(' ');
        /* Peculiar construction to ditch first whitespace. */
        PPToken tok = source_token_nonwhite();
        ERROR:
        for (; ; )
        {
            switch (tok.getPPTokenKind())
            {
                case NL:
                case EOF:
                    break ERROR;
                default:
                    buf.append(tok.getText());
                    break;
            }
            tok = source_token();
        }
        if (is_error)
            error(pptok, buf.toString());
        else
            warning(pptok, buf.toString());
    }

    /* This bypasses token() for #elif expressions.
     * If we don't do this, then isActive() == false
     * causes token() to simply chew the entire input line. */
    private PPToken expanded_token() throws Exception
    {
        for (; ; )
        {
            PPToken tok = source_token();
            // System.out.println("Source token is " + tok);
            if (tok.getPPTokenKind() == IDENTIFIER)
            {
                Macro m = getMacro(tok.getText());
                if (m == null)
                    return tok;
                if (source.isExpanding(m))
                    return tok;
                if (macro(m, tok))
                    continue;
            }
            return tok;
        }
    }

    private PPToken expanded_token_nonwhite() throws Exception
    {
        PPToken tok;
        do
        {
            tok = expanded_token();
            // System.out.println("expanded token is " + tok);
        } while (isWhite(tok));
        return tok;
    }

    private PPToken expr_token = null;

    private PPToken expr_token() throws Exception
    {
        PPToken tok = expr_token;

        if (tok != null)
        {
            // System.out.println("ungetting");
            expr_token = null;
        }
        else
        {
            tok = expanded_token_nonwhite();
            // System.out.println("expt is " + tok);

            if (tok.getPPTokenKind() == IDENTIFIER && tok.getText()
                    .equals("defined"))
            {
                PPToken la = source_token_nonwhite();
                boolean paren = false;
                if (la.getPPTokenKind() == LPAREN)
                {
                    paren = true;
                    la = source_token_nonwhite();
                }

                // System.out.println("Core token is " + la);
                if (la.getPPTokenKind() != IDENTIFIER)
                {
                    error(la,
                            "defined() needs identifier, not " + la.getText());
                    tok = new PPToken(NUMBER, la.getLocation(),
                            new NumericValue(10, "0"), "0");
                }
                else if (macros.containsKey(la.getText()))
                {
                    // System.out.println("Found macro");
                    tok = new PPToken(NUMBER, la.getLocation(),
                            new NumericValue(10, "1"), "1");
                }
                else
                {
                    // System.out.println("Not found macro");
                    tok = new PPToken(NUMBER, la.getLocation(),
                            new NumericValue(10, "0"), "0");
                }

                if (paren)
                {
                    la = source_token_nonwhite();
                    if (la.getPPTokenKind() != RPAREN)
                    {
                        expr_untoken(la);
                        error(la,
                                "Missing ) in defined(). Got " + la.getText());
                    }
                }
            }
        }

        // System.out.println("expr_token returns " + tok);
        return tok;
    }

    private void expr_untoken(PPToken tok) throws Exception
    {
        if (expr_token != null)
            throw new Exception("Cannot unget two expression tokens.");
        expr_token = tok;
    }

    private int expr_priority(PPToken op)
    {
        switch (op.getPPTokenKind())
        {
            case DIV:
                return 11;
            case PERCENTAGE:
                return 11;
            case MUL:
                return 11;
            case PLUS:
                return 10;
            case SUB:
                return 10;
            case LSH:
                return 9;
            case RSH:
                return 9;
            case LT:
                return 8;
            case GT:
                return 8;
            case LE:
                return 8;
            case GE:
                return 8;
            case EQ:
                return 7;
            case NE:
                return 7;
            case AND:
                return 6;
            case XOR:
                return 5;
            case OR:
                return 4;
            case LAND:
                return 3;
            case LOR:
                return 2;
            case QUES:
                return 1;
            default:
                // System.out.println("Unrecognised operator " + op);
                return 0;
        }
    }

    private long expr(int priority) throws Exception
    {
        /*
         * (new Exception("expr(" + priority + ") called")).printStackTrace();
         */

        PPToken tok = expr_token();
        long lhs, rhs;

        // System.out.println("Expr lhs token is " + tok);
        switch (tok.getPPTokenKind())
        {
            case LPAREN:
                lhs = expr(0);
                tok = expr_token();
                if (tok.getPPTokenKind() != RPAREN)
                {
                    expr_untoken(tok);
                    error(tok, "Missing ) in expression. Got " + tok.getText());
                    return 0;
                }
                break;

            case TILDE:
                lhs = ~expr(11);
                break;
            case BANG:
                lhs = expr(11) == 0 ? 1 : 0;
                break;
            case SUB:
                lhs = -expr(11);
                break;
            case NUMBER:
                NumericValue value = (NumericValue) tok.getValue();
                lhs = value.longValue();
                break;
            case CHARACTER:
                lhs = (Character) tok.getValue();
                break;
            case IDENTIFIER:
                if (/*warnings.contains(Warning.UNDEF)*/true)
                    warning(tok, "Undefined token '" + tok.getText()
                            + "' encountered in conditional.");
                lhs = 0;
                break;

            default:
                expr_untoken(tok);
                error(tok, "Bad token in expression: " + tok.getText());
                return 0;
        }

        EXPR:
        for (; ; )
        {
            // System.out.println("expr: lhs is " + lhs + ", pri = " + priority);
            PPToken op = expr_token();
            int pri = expr_priority(op);	/* 0 if not a binop. */

            if (pri == 0 || priority >= pri)
            {
                expr_untoken(op);
                break EXPR;
            }
            rhs = expr(pri);
            // System.out.println("rhs token is " + rhs);
            switch (op.getPPTokenKind())
            {
                case DIV:
                    if (rhs == 0)
                    {
                        error(op, "Division by zero");
                        lhs = 0;
                    }
                    else
                    {
                        lhs = lhs / rhs;
                    }
                    break;
                case PERCENTAGE:
                    if (rhs == 0)
                    {
                        error(op, "Modulus by zero");
                        lhs = 0;
                    }
                    else
                    {
                        lhs = lhs % rhs;
                    }
                    break;
                case MUL:
                    lhs = lhs * rhs;
                    break;
                case PLUS:
                    lhs = lhs + rhs;
                    break;
                case SUB:
                    lhs = lhs - rhs;
                    break;
                case LT:
                    lhs = lhs < rhs ? 1 : 0;
                    break;
                case GT:
                    lhs = lhs > rhs ? 1 : 0;
                    break;
                case AND:
                    lhs = lhs & rhs;
                    break;
                case XOR:
                    lhs = lhs ^ rhs;
                    break;
                case OR:
                    lhs = lhs | rhs;
                    break;

                case LSH:
                    lhs = lhs << rhs;
                    break;
                case RSH:
                    lhs = lhs >> rhs;
                    break;
                case LE:
                    lhs = lhs <= rhs ? 1 : 0;
                    break;
                case GE:
                    lhs = lhs >= rhs ? 1 : 0;
                    break;
                case EQ:
                    lhs = lhs == rhs ? 1 : 0;
                    break;
                case NE:
                    lhs = lhs != rhs ? 1 : 0;
                    break;
                case LAND:
                    lhs = (lhs != 0) && (rhs != 0) ? 1 : 0;
                    break;
                case LOR:
                    lhs = (lhs != 0) || (rhs != 0) ? 1 : 0;
                    break;

                case QUES:
                {
                    tok = expr_token();
                    if (tok.getPPTokenKind() != COLON)
                    {
                        expr_untoken(tok);
                        error(tok, "Missing : in conditional expression. Got "
                                + tok.getText());
                        return 0;
                    }
                    long falseResult = expr(0);
                    lhs = (lhs != 0) ? rhs : falseResult;
                }
                break;

                default:
                    error(op, "Unexpected operator " + op.getText());
                    return 0;

            }
        }

        /*
         * (new Exception("expr returning " + lhs)).printStackTrace();
         */
        // System.out.println("expr returning " + lhs);
        return lhs;
    }

    private PPToken toWhitespace(PPToken tok)
    {
        String text = tok.getText();
        int len = text.length();
        boolean cr = false;
        int nls = 0;

        for (int i = 0; i < len; i++)
        {
            char c = text.charAt(i);

            switch (c)
            {
                case '\r':
                    cr = true;
                    nls++;
                    break;
                case '\n':
                    if (cr)
                    {
                        cr = false;
                        break;
                    }
                /* fallthrough */
                case '\u2028':
                case '\u2029':
                case '\u000B':
                case '\u000C':
                case '\u0085':
                    cr = false;
                    nls++;
                    break;
            }
        }

        char[] cbuf = new char[nls];
        Arrays.fill(cbuf, '\n');
        return new PPToken(WHITESPACE, tok.getLocation(),
                new String(cbuf));
    }

    private PPToken _token() throws Exception
    {
        while (true)
        {
            PPToken tok;
            if (!isActive())
            {
                Source s = getSource();
                if (s == null)
                {
                    PPToken t = next_source();
                    if (t.getPPTokenKind() == P_LINE && !langInfo.gnuMode)
                        continue;
                    return t;
                }

                try
                {
                    /* XXX Tell lexer to ignore warnings. */
                    s.setActive(false);
                    tok = source_token();
                }
                finally
                {
                    /* XXX Tell lexer to stop ignoring warnings. */
                    s.setActive(true);
                }
                switch (tok.getPPTokenKind())
                {
                    case HASH:
                    case NL:
                    case EOF:
                        /* The preprocessor has to take action here. */
                        break;
                    case WHITESPACE:
                        return tok;
                    case CCOMMENT:
                    case CPPCOMMENT:
                        // Patch up to preserve whitespace.
                        if (/**getFeature(Feature.KEEPALLCOMMENTS)*/true)
                            return tok;
                        if (!isActive())
                            return toWhitespace(tok);
                        if (/*getFeature(Feature.KEEPCOMMENTS)*/true)
                            return tok;
                        return toWhitespace(tok);
                    default:
                        // Return NL to preserve whitespace.
                        /* XXX This might lose a comment. */
                        return source_skipline(false);
                }
            }
            else
            {
                tok = source_token();
            }

            LEX:
            switch (tok.getPPTokenKind())
            {
                case EOF:
                    /* Pop the stacks. */
                    return tok;

                case WHITESPACE:
                case NL:
                    return tok;

                case CCOMMENT:
                case CPPCOMMENT:
                    return tok;

                case BANG:
                case PERCENTAGE:
                case AND:
                case LPAREN:
                case RPAREN:
                case MUL:
                case PLUS:
                case COMMA:
                case SUB:
                case DIV:
                case COLON:
                case SEMI:
                case LT:
                case EQ:
                case GT:
                case QUES:
                case LBRACKET:
                case RBRACKET:
                case XOR:
                case LBRACE:
                case OR:
                case RBRACE:
                case TILDE:
                case DOT:

                /* From Olivier Chafik for Objective C? */
                case AT:
                /* The one remaining ASCII, might as well. */
                case SINGLE_QUOTE:

                    // case '#':
                case PERCENTAGE_EQ:
                case ARROW:
                case CHARACTER:
                case DEC:
                case DIV_EQ:
                case ELLIPSIS:
                case EQEQ:
                case GE:
                case HEADER:	/* Should only arise from include() */

                case INC:
                case LAND:
                case LE:
                case LOR:
                case LSH:
                case LSH_EQ:
                case SUB_EQ:
                case MOD_EQ:
                case MULTI_EQ:
                case NE:
                case OR_EQ:
                case PLUS_EQ:
                case RANGE:
                case RSH:
                case RSH_EQ:
                case STRING:
                case SQSTRING:
                case XOR_EQ:
                    return tok;

                case NUMBER:
                    return tok;

                case IDENTIFIER:
                    Macro m = getMacro(tok.getText());
                    if (m == null)
                        return tok;
                    if (source.isExpanding(m))
                        return tok;
                    if (macro(m, tok))
                        break;
                    return tok;

                case P_LINE:
                    if (langInfo.gnuMode)
                        return tok;
                    break;

                case UNKNOWN:
                    if (/*getFeature(Feature.CSYNTAX)*/true)
                        error(tok, String.valueOf(tok.getValue()));
                    return tok;

                default:
                    throw new Exception("Bad token " + tok);
                    // break;

                case HASH:
                    tok = source_token_nonwhite();
                    switch (tok.getPPTokenKind())
                    {
                        case NL:
                            break LEX;	/* Some code has #\n */

                        case IDENTIFIER:
                            break;
                        default:
                            error(tok,"Preprocessor directive not a word "
                                    + tok.getText());
                            return source_skipline(false);
                    }
                    PredefinedDirectiveCommand ppcmd = PredefinedDirectiveCommand
                            .forText(tok.getText());
                    if (ppcmd == null)
                    {
                        error(tok, "Unknown preprocessor directive " + tok
                                .getText());
                        return source_skipline(false);
                    }

                    PP:
                    switch (ppcmd)
                    {

                        case CPP_DEFINE:
                            if (!isActive())
                                return source_skipline(false);
                            else
                                return define();
                            // break;

                        case CPP_UNDEF:
                            if (!isActive())
                                return source_skipline(false);
                            else
                                return undef();
                            // break;

                        case CPP_INCLUDE:
                            if (!isActive())
                                return source_skipline(false);
                            else
                                return include(false);
                            // break;
                        case CPP_INCLUDE_NEXT:
                            if (!isActive())
                                return source_skipline(false);
                            if (!/*getFeature(Feature.INCLUDENEXT)*/true)
                            {
                                error(tok,
                                        "Directive include_next not enabled");
                                return source_skipline(false);
                            }
                            return include(true);
                        // break;

                        case CPP_WARNING:
                        case CPP_ERROR:
                            if (!isActive())
                                return source_skipline(false);
                            else
                                error(tok, ppcmd == CPP_ERROR);
                            break;

                        case CPP_IF:
                            push_state();
                            if (!isActive())
                            {
                                return source_skipline(false);
                            }
                            expr_token = null;
                            states.peek().setActive(expr(0) != 0);
                            tok = expr_token();	/* unget */

                            if (tok.getPPTokenKind() == NL)
                                return tok;
                            return source_skipline(true);
                        // break;

                        case CPP_ELIF:
                            State state = states.peek();
                            if (state.sawElse())
                            {
                                error(tok, "#elif after #" + "else");
                                return source_skipline(false);
                            }
                            else if (!state.isParentActive())
                            {
                                /* Nested in skipped 'if' */
                                return source_skipline(false);
                            }
                            else if (state.isActive())
                            {
                                /* The 'if' part got executed. */
                                state.setParentActive(false);
                                /* This is like # else # if but with
                                 * only one # end. */
                                state.setActive(false);
                                return source_skipline(false);
                            }
                            else
                            {
                                expr_token = null;
                                state.setActive(expr(0) != 0);
                                tok = expr_token();	/* unget */

                                if (tok.getPPTokenKind() == NL)
                                    return tok;
                                return source_skipline(true);
                            }
                            // break;

                        case CPP_ELSE:
                            state = states.peek();
                            if (state.sawElse())
                            {
                                error(tok, "#" + "else after #" + "else");
                                return source_skipline(false);
                            }
                            else
                            {
                                state.setSawElse();
                                state.setActive(!state.isActive());
                                return source_skipline(/*warnings.contains(
                                        Warning.ENDIF_LABELS)*/true);
                            }
                            // break;

                        case CPP_IFDEF:
                            push_state();
                            if (!isActive())
                            {
                                return source_skipline(false);
                            }
                            else
                            {
                                tok = source_token_nonwhite();
                                // System.out.println("ifdef " + tok);
                                if (tok.getPPTokenKind() != IDENTIFIER)
                                {
                                    error(tok, "Expected identifier, not " + tok
                                            .getText());
                                    return source_skipline(false);
                                }
                                else
                                {
                                    String text = tok.getText();
                                    boolean exists = macros.containsKey(text);
                                    states.peek().setActive(exists);
                                    return source_skipline(true);
                                }
                            }
                            // break;

                        case CPP_IFNDEF:
                            push_state();
                            if (!isActive())
                            {
                                return source_skipline(false);
                            }
                            else
                            {
                                tok = source_token_nonwhite();
                                if (tok.getPPTokenKind() != IDENTIFIER)
                                {
                                    error(tok, "Expected identifier, not " + tok
                                            .getText());
                                    return source_skipline(false);
                                }
                                else
                                {
                                    String text = tok.getText();
                                    boolean exists = macros.containsKey(text);
                                    states.peek().setActive(!exists);
                                    return source_skipline(true);
                                }
                            }
                            // break;

                        case CPP_ENDIF:
                            pop_state();
                            return source_skipline(/*
                                    warnings.contains(Warning.ENDIF_LABELS)*/true);
                        // break;

                        case CPP_LINE:
                            return source_skipline(false);
                        // break;

                        case CPP_PRAGMA:
                            if (!isActive())
                                return source_skipline(false);
                            return pragma();
                        // break;

                        default:
                            /* Actual unknown directives are
                             * processed above. If we get here,
                             * we succeeded the map lookup but
                             * failed to handle it. Therefore,
                             * this is (unconditionally?) fatal. */
                            // if (isActive()) /* XXX Could be warning. */
                            throw new Exception(
                                    "Internal error: Unknown directive " + tok);
                            // return source_skipline(false);
                    }

            }
        }
    }

    private PPToken token_nonwhite() throws Exception
    {
        PPToken tok;
        do
        {
            tok = _token();
        } while (isWhite(tok));
        return tok;
    }

    /**
     * Returns the next preprocessor token.
     *
     * @return The next fully preprocessed token.
     * @throws IOException       if an I/O error occurs.
     * @throws LexerException    if a preprocessing error occurs.
     * @throws Exception if an unexpected error condition arises.
     * @see PPToken
     */
    public PPToken token() throws Exception
    {
        return _token();
    }

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();

        Source s = getSource();
        while (s != null)
        {
            buf.append(" -> ").append(String.valueOf(s)).append(File.separator);
            s = s.getParent();
        }

        Map<String, Macro> macros = new TreeMap<String, Macro>(getMacros());
        for (Macro macro : macros.values())
        {
            buf.append("#").append("macro ").append(macro).append(File.separator);
        }

        return buf.toString();
    }

    @Override
    public void close() throws Exception
    {
        Source s = source;
        while (s != null)
        {
            s.close();
            s = s.getParent();
        }

        for (Source ss : inputs)
        {
            ss.close();
        }
    }
}
