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

import gnu.trove.list.array.TIntArrayList;
import jlang.basic.*;
import jlang.cparser.Token;
import jlang.cpp.PPCallBack.FileChangeReason;
import jlang.diag.Diagnostic;
import jlang.diag.FixItHint;
import jlang.diag.FullSourceLoc;
import tools.OutParamWrapper;
import tools.Pair;

import java.nio.file.Path;
import java.util.*;

import static jlang.cparser.Token.TokenFlags.*;
import static jlang.cpp.TokenKind.*;
import static jlang.diag.DiagnosticLexKindsTag.*;

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
public final class Preprocessor
{
    private Diagnostic diags;
    private LangOptions langInfo;
    private TargetInfo target;
    private HeaderSearch headerInfo;
    private SourceManager sourceMgr;
    private ScratchBuffer scrachBuf;

    /**
     * Identifiers for builtin macros and other builtins.
     */
    private IdentifierInfo Ident__LINE__, Ident__FILE__;   // __LINE__, __FILE__
    private IdentifierInfo Ident__DATE__, Ident__TIME__;   // __DATE__, __TIME__
    private IdentifierInfo Ident__INCLUDE_LEVEL__;          // __INCLUDE_LEVEL__
    private IdentifierInfo Ident__BASE_FILE__;              // __BASE_FILE__
    private IdentifierInfo Ident__TIMESTAMP__;              // __TIMESTAMP__
    private IdentifierInfo Ident__COUNTER__;                // __COUNTER__
    private IdentifierInfo Ident_Pragma, Ident__VA_ARGS__; // _Pragma, __VA_ARGS__

    private SourceLocation DATELoc, TIMELoc;
    private int counterValue;   // next __COUNTER__.

    /**
     * Maximum depth of #includes.
     */
    private static final int maxAllowedIncludesStackDepth = 200;

    private boolean keepComments;

    private boolean keepMacroComments;

    private boolean disableMacroExpansion;
    private boolean inMacroArgs;

    private IdentifierTable identifiers;

    /**
     * This tracks all of the pragmas that the client registered
     * with this preprocessor.
     */
    private PragmaNameSpace pragmaHandlers;

    private ArrayList<CommentHandler> commentHandlers;

    /**
     * This is the current top of the stack that we're lexing from if
     * * not expanding a macro and we are lexing directly from source code.
     * *  Only one of curLexer, or curTokenLexer will be non-null.
     */
    private Lexer curLexer;

    /**
     * This is the current macro we are expanding, if we are
     * expanding a macro.  One of {@linkplain #curLexer} and
     * {@linkplain #curTokenLexer} must be null.
     */
    private TokenLexer curTokenLexer;

    public Diagnostic.DiagnosticBuilder diag(SourceLocation loc, int diagID)
    {
        return diags.report(new FullSourceLoc(loc, getSourceManager()), diagID);
    }

    public Diagnostic.DiagnosticBuilder diag(Token tok, int diagID)
    {
        return diag(tok.getLocation(), diagID);
    }

    /**
     * Return the 'spelling' of this token.  The spelling of a
     * token are the characters used to represent the token in the source file
     * after trigraph expansion and escaped-newline folding.  In particular, this
     * wants to get the true, uncanonicalized, spelling of things like digraphs
     * UCNs, etc.
     *
     * @param tok
     * @return
     */
    public String getSpelling(Token tok)
    {
        assert tok.getLength() >= 0 : "Token character range is bogus!";

        Token.StrData tokStart = sourceMgr.getCharacterData(tok.getLocation());
        if (!tok.needsCleaning())
            return tokStart.data.substring(tokStart.offset, tok.getName().length());

        StringBuilder result = new StringBuilder();
        for (int i = tokStart.offset, e = tok.getLength() + tokStart.offset; i < e; )
        {
            OutParamWrapper<Integer> charSize = new OutParamWrapper<>(0);
            result.append(
                    Lexer.getCharAndSizeNoWarn(tokStart.data.toCharArray(), i,
                            charSize, langInfo));
            i += charSize.get();
        }

        assert result.length() != tok
                .getLength() : "NeedsCleaning flag set on something that didn't need cleaning!";
        return result.toString();
    }

    public void createString(String spelling, Token tok,
            SourceLocation instantiationLoc)
    {
        tok.setLength(spelling.length());
        OutParamWrapper<Integer> x = new OutParamWrapper<>();
        SourceLocation loc = scrachBuf.getToken(spelling.toCharArray(), x);
        int dest = x.get();

        if (instantiationLoc.isValid())
            loc = sourceMgr.createInstantiationLoc(loc, instantiationLoc,
                    instantiationLoc, spelling.length(), 0, 0);

        tok.setLocation(loc);

        if (tok.isLiteral())
            tok.setLiteralData(spelling, scrachBuf.getBuffer(), dest);
    }

    public void handleComment(SourceRange comment)
    {
        commentHandlers.forEach(handler -> handler.handleComoment(this, comment));
    }

    /**
     * Given a identifier token, look up the
     * identifier information for the token and install it into the token.
     *
     * @param identifier
     * @param buf
     * @param startPos
     * @return
     */
    public IdentifierInfo lookupIdentifierInfo(Token identifier, char[] buf, int startPos)
    {
        assert identifier.is(Identifier) : "Not an identifier!";
        assert identifier.getIdentifierInfo()
                == null : "Identifier already exists!";

        IdentifierInfo ii;
        if (startPos != 0 && !identifier.needsCleaning())
        {
            ii = getIdentifierInfo(String.valueOf(buf, startPos,
                    identifier.getName().length()));
        }
        else
        {
            String sb = getSpelling(identifier);
            ii = getIdentifierInfo(sb);
        }
        identifier.setIdentifierInfo(ii);
        return ii;
    }

    /**
     * This callback is invoked when the lexer reads an
     * identifier.  This callback looks up the identifier in the map and/or
     * potentially macro expands it or turns it into a named token (like 'for').
     * <p>
     * Note that callers of this method are guarded by checking the
     * IdentifierInfo's 'isHandleIdentifierCase' bit.  If this method changes, the
     * IdentifierInfo methods that compute these properties will need to change to
     * match.
     *
     * @param token
     */
    public void handleIdentifier(Token token)
    {
        assert token.getIdentifierInfo()
                != null : "Can't handle identifiers without identifier info!";

        IdentifierInfo ii = token.getIdentifierInfo();

        // If this identifier was poisoned, and if it was not produced from a macro
        // expansion, emit an error.
        if (ii.isPoisoned() && curLexer != null)
        {
            if (!ii.equals(Ident__VA_ARGS__))
                diag(token, err_pp_used_poisoned_id);
            else
                diag(token, ext_pp_bad_vaargs_use);
        }

        // If this is a macro to be expanded, do it.
        MacroInfo mi = getMacroInfo(ii);
        if (mi != null)
        {
            if (!disableMacroExpansion && !token.isExpandingDisabled())
            {
                if (mi.isEnabled())
                {
                    OutParamWrapper<Token> x = new OutParamWrapper<>(token);
                    boolean res = !handleMacroExpandedIdentifier(x, mi);
                    token = x.get();
                    if (res) return;
                }
                else
                {
                    // C99 6.10.3.4p2 says that a disabled macro may never again be
                    // expanded, even if it's in a context where it could be expanded in the
                    // future.
                    token.setFlag(DisableExpand);
                }
            }
        }

        // If this is an extension token, diagnose its use.
        // We avoid diagnosing tokens that originate from macro definitions.
        if (ii.isExtensionToken() && !disableMacroExpansion)
            diag(token, ext_token_used);
    }

    public void enterTokenStream(Token[] toks, boolean disableMacroExpansion,
            boolean ownsTokens)
    {
        // Save our current state.
        pushIncludeMacroStack();
        // Create a macro expander to expand from the specified token stream.
        curTokenLexer = new TokenLexer(toks, disableMacroExpansion, ownsTokens,
                this);
    }

    class IncludeStackInfo
    {
        Lexer theLexer;
        TokenLexer theTokenLexer;

        IncludeStackInfo(Lexer theLexer, TokenLexer theTokenLexer)
        {
            this.theLexer = theLexer;
            this.theTokenLexer = theTokenLexer;
        }
    }

    private LinkedList<IncludeStackInfo> includeMacroStack;

    /**
     * These are actions invoked when some preprocessor activity is
     * encountered (e.g. a file is #included, etc).
     */
    private PPCallBack callbacks;

    private HashMap<IdentifierInfo, MacroInfo> macros;

    // Various statistics we track for performance analysis.
    int NumDirectives, NumIncluded, NumDefined, NumUndefined, NumPragma;
    int NumIf, NumElse, NumEndif;
    int numEnteredSourceFiles, MaxIncludeStackDepth;
    int NumMacroExpanded, NumFnMacroExpanded, NumBuiltinMacroExpanded;
    int NumFastMacroExpanded, NumTokenPaste, NumFastTokenPaste;
    int NumSkipped;

    /**
     * This string is the predefined macros that preprocessor
     * should use from the command line etc.
     */
    private String predefines;

    /**
     * Cached tokens are stored here when we do backtracking or
     * lookahead. They are "lexed" by the CachingLex() method.
     */
    private ArrayList<Token> cachedTokens;

    /**
     * The position of the cached token that CachingLex() should
     * "lex" next. If it points beyond the CachedTokens vector, it means that
     * a normal Lex() should be invoked.
     */
    private int cachedLexPos;

    /**
     * Stack of backtrack positions, allowing nested
     * backtracks. The EnableBacktrackAtThisPos() method pushes a position to
     * indicate where CachedLexPos should be set when the BackTrack() method is
     * invoked (at which point the last position is popped).
     */
    private TIntArrayList backtrackPositions;

    public List<PPToken> expand(MacroArgument macroMacroArgument)
    {
        return Collections.emptyList();
    }

    public Preprocessor(Diagnostic diag, LangOptions langOptions,
            TargetInfo target, SourceManager sourceMgr,
            HeaderSearch headerSearch)
    {
        this(diag, langOptions, target, sourceMgr, headerSearch, null);
    }

    public Preprocessor(Diagnostic diag, LangOptions langOptions,
            TargetInfo target, SourceManager sourceMgr,
            HeaderSearch headerSearch, IdentifierInfoLookup iilookup)
    {
        this.diags = diag;
        langInfo = langOptions;
        headerInfo = headerSearch;
        this.sourceMgr = sourceMgr;
        identifiers = new IdentifierTable(langOptions, iilookup);
        commentHandlers = new ArrayList<>();
        includeMacroStack = new LinkedList<>();
        macros = new HashMap<>();
        scrachBuf = new ScratchBuffer(sourceMgr);
        cachedTokens = new ArrayList<>();
        backtrackPositions = new TIntArrayList();

        (Ident__VA_ARGS__ = getIdentifierInfo("__VA_ARGS__")).setIsPoisoned(true);

        // Initialize the pragma handlers.
        pragmaHandlers = new PragmaNameSpace(null);

        registerBuiltinPragmas();

        // Initialize builtin macros like __LINE__ and friends.
        registerBuiltinMacros();
    }

    public LangOptions getLangOptions()
    {
        return langInfo;
    }

    public TargetInfo getTargetInfo()
    {
        return target;
    }

    public SourceManager getSourceManager()
    {
        return sourceMgr;
    }

    public HeaderSearch getHeaderInfo()
    {
        return headerInfo;
    }

    public IdentifierTable getIdentifierTable()
    {
        return identifiers;
    }

    public Diagnostic getDiagnostics()
    {
        return diags;
    }

    public void setDiagnostics(Diagnostic diags)
    {
        this.diags = diags;
    }

    /**
     * Specify a macro for this identifier.
     *
     * @param ii
     * @param mi
     */
    public void setMacroInfo(IdentifierInfo ii, MacroInfo mi)
    {
        if (mi != null)
        {
            macros.put(ii, mi);
            ii.setHasMacroDefinition(true);
        }
        else if (ii.isHasMacroDefinition())
        {
            macros.remove(ii);
            ii.setHasMacroDefinition(false);
        }
    }

    private void registerBuiltinMacros()
    {
        Ident__LINE__ = registerBuiltintMacro(this, "__LINE__");
        Ident__FILE__ = registerBuiltintMacro(this, "__FILE__");
        Ident__DATE__ = registerBuiltintMacro(this, "__DATE__");
        Ident__TIME__ = registerBuiltintMacro(this, "__TIME__");
        Ident__COUNTER__ = registerBuiltintMacro(this, "__COUNTER__");
        Ident_Pragma = registerBuiltintMacro(this, "_Pragma");

        // GCC Extensions.
        Ident__BASE_FILE__ = registerBuiltintMacro(this, "__BASE_FILE__");
        Ident__INCLUDE_LEVEL__ = registerBuiltintMacro(this, "__INCLUDE_LEVEL__");
        Ident__TIMESTAMP__ = registerBuiltintMacro(this, "__TIMESTAMP__");
    }

    static IdentifierInfo registerBuiltintMacro(Preprocessor pp, String name)
    {
        IdentifierInfo ii = pp.getIdentifierInfo(name);
        // Mark it as being a macro that is builtin.
        MacroInfo mi = new MacroInfo(new SourceLocation());
        mi.setIsBuiltinMacro();
        pp.setMacroInfo(ii, mi);
        return ii;
    }

    private void registerBuiltinPragmas()
    {
        addPragmaHandler(null, new PragmaOnceHandler(getIdentifierInfo("once")));
        ;
        addPragmaHandler(null, new PragmaMarkHandler(getIdentifierInfo("mark")));
        ;
    }

    /*
    * Add the specified pragma handler to the preprocessor.
    * If 'Namespace' is non-null, then it is a token required to exist on the
    * pragma line before the pragma string starts, e.g. "STDC" or "GCC".
    */
    private void addPragmaHandler(String namespace, PragmaHandler handler)
    {
        PragmaNameSpace insertNS = pragmaHandlers;
        if (namespace != null)
        {
            IdentifierInfo ii = getIdentifierInfo(namespace);

            PragmaHandler existing = pragmaHandlers.findHandler(ii, true);
            if (existing != null)
            {
                insertNS = existing.getIfNamespace();
                assert insertNS != null :
                        "Cannot have a pragma namespace and pragma"
                                + " handler with the same name!";
            }
            else
            {
                insertNS = new PragmaNameSpace(ii);
                pragmaHandlers.addPragma(insertNS);
            }
        }

        assert insertNS.findHandler(handler.getName(), true) == null : "Pragma handler already exists for this identifier!";
        insertNS.addPragma(handler);
    }

    public void removePragmaHandler(String namespace, PragmaHandler handler)
    {
        PragmaNameSpace ns = pragmaHandlers;

        if (namespace != null)
        {
            IdentifierInfo nsid = getIdentifierInfo(namespace);
            PragmaHandler existing = pragmaHandlers.findHandler(nsid, true);
            assert existing != null : "Namespace containing handler does not exist!";

            ns = existing.getIfNamespace();
            assert ns
                    != null : "Invalid namespace, registered as a regular pragma handler!";
        }

        ns.removePragmaHandler(handler);

        if (!ns.equals(pragmaHandlers) && ns.isEmpty())
        {
            pragmaHandlers.removePragmaHandler(ns);
        }
    }

    private IdentifierInfo getIdentifierInfo(String name)
    {
        return identifiers.get(name);
    }

    public void setCommentRetentionState(boolean keepComments, boolean keepMacroComments)
    {
        this.keepComments |= keepComments;
        this.keepMacroComments |= keepMacroComments;
    }

    public boolean getCommentRetentionState()
    {
        return keepComments;
    }

    public boolean isCurrentLexer(Lexer lexer)
    {
        return curLexer.equals(lexer);
    }

    static boolean isFileLexer(Lexer l)
    {
        return l != null ? l.isPragmaLexer() : false;
    }

    public boolean isFileLexer()
    {
        return isFileLexer(curLexer);
    }

    public static boolean isFileLexer(IncludeStackInfo info)
    {
        return isFileLexer(info.theLexer);
    }

    public PreprocessorLexer getCurrentFileLexer()
    {
        if (isFileLexer())
            return curLexer;

        for (IncludeStackInfo info : includeMacroStack)
        {
            if (isFileLexer(info))
                return info.theLexer;
        }
        return null;
    }

    public PPCallBack getPPCallBacks()
    {
        return callbacks;
    }

    public void setPPCallbacks(PPCallBack cb)
    {
        if (callbacks != null)
            callbacks = new PPCallBack.PPChainedCallBack(cb, callbacks);
        callbacks = cb;
    }

    public MacroInfo getMacroInfo(IdentifierInfo ii)
    {
        return ii.isHasMacroDefinition() ? macros.getOrDefault(ii, null) : null;
    }

    public HashMap<IdentifierInfo, MacroInfo> getMacros()
    {
        return macros;
    }

    public String getPredefines()
    {
        return predefines;
    }

    public void setPredefines(String predefines)
    {
        this.predefines = predefines;
    }

    public void addCommentHandler(CommentHandler handler)
    {
        assert handler != null : "NULL comment handler!";
        assert commentHandlers
                .contains(handler) : "Comment handler already exist";
        commentHandlers.add(handler);
    }

    public void removeCommentHandler(CommentHandler handler)
    {
        assert commentHandlers
                .contains(handler) : "Comment handler not registered!";
        commentHandlers.remove(handler);
    }

    /**
     * Enter the specified FileID as the main source file,
     * which implicitly adds the builtin defines etc.
     */
    public void enterMainFile()
    {
        assert numEnteredSourceFiles == 0 : "Cannot reenter the tmain file!";

        FileID mainFileID = sourceMgr.getMainFileID();

        // Enter the main file source buffer.
        enterSourceFile(mainFileID, null);

        Path fileEntry = sourceMgr.getFileEntryForID(mainFileID);
        if (fileEntry != null)
            headerInfo.incrementIncludeCount(fileEntry);

        StringBuilder buffer = new StringBuilder();
        buffer.append(predefines);

        MemoryBuffer sb = MemoryBuffer
                .getMemBuffer(buffer.toString(), "<built-in>");
        assert sb != null : "Cannot fail to create predefined source buffer";
        FileID fid = sourceMgr.createFileIDForMemBuffer(sb);
        assert !fid.isInvalid() : "Could not create FileID for predefines?";

        // Start parsing the predefines.
        enterSourceFile(fid, null);
    }

    private void enterSourceFile(FileID fid, Path curDir)
    {
        assert curTokenLexer == null : "Cannot #include a file inside a macro!";
        ++numEnteredSourceFiles;

        if (MaxIncludeStackDepth < includeMacroStack.size())
            MaxIncludeStackDepth = includeMacroStack.size();

        enterSourceFileWithLexer(new Lexer(fid, this), curDir);
    }

    /**
     * Add a Macro to the top of the include stack and start lexing
     * tokens from it instead of the current buffer.
     * @param tok
     * @param end
     * @param args
     */
    private void enterMacro(Token tok, SourceLocation end, MacroArgs args)
    {
        pushIncludeMacroStack();
        curTokenLexer = new TokenLexer(tok, end, args, this);
    }

    private void enterSourceFileWithLexer(Lexer lexer, Path curDir)
    {
        if(curLexer != null || curTokenLexer != null)
            pushIncludeMacroStack();

        curLexer = lexer;
        if (callbacks != null && !curLexer.isPragmaLexer())
        {
            CharacteristicKind fileType = sourceMgr.getFileCharacteristicKind(curLexer.getFileLoc());
            callbacks.fileChanged(curLexer.getFileLoc(),
                    FileChangeReason.EnterFile, fileType);
        }
    }

    void removeTopOfLexerStack()
    {
        assert !includeMacroStack
                .isEmpty() : "Ran out of stack entries to loead";

        popIncludeMacroStack();
    }

    private void pushIncludeMacroStack()
    {
        includeMacroStack.add(new IncludeStackInfo(curLexer, curTokenLexer));
        curTokenLexer = null;
        curLexer = null;
    }

    private void popIncludeMacroStack()
    {
        curLexer = includeMacroStack.peek().theLexer;
        curTokenLexer = includeMacroStack.peek().theTokenLexer;
        includeMacroStack.pop();
    }

    /**
     * This callback is invoked when the lexer hits the end of
     * the current file.  This either returns the EOF token or pops a level off
     * the include stack and keeps going.
     *
     * @param result
     * @param isEndOfMacro
     * @return
     */
    public boolean handleEndOfFile(Token result, boolean isEndOfMacro)
    {
        assert curTokenLexer
                == null : "Ending a file when currently in a macro!";

        // See if this file had a controlling macro.
        if (curLexer != null)
        {
            // Not ending a macro, ignore it.
            IdentifierInfo controllingMacro = curLexer.miOpt.getControllingMacroAtEndOfFile();
            if (controllingMacro != null)
            {
                // Okay, this has a controlling macro, remember in HeaderFileInfo.
                Path fileEntry = sourceMgr.getFileEntryForID(curLexer.getFileID());
                if (fileEntry != null)
                    headerInfo.setFileControllingMacro(fileEntry,
                            controllingMacro);
                ;
            }
        }

        // If this is a #include'd file, pop it off the include stack and continue
        // lexing the #includer file.
        if (!includeMacroStack.isEmpty())
        {
            // We're done with the #include file.
            removeTopOfLexerStack();

            if (callbacks != null && !isEndOfMacro && curLexer != null)
            {
                CharacteristicKind fileType = sourceMgr
                        .getFileCharacteristicKind(curLexer.getSourceLocation());
                callbacks.fileChanged(curLexer.getSourceLocation(),
                        FileChangeReason.ExitFile, fileType);
            }

            // Client should lex another token.
            return false;
        }

        // Otherwise, this file is the top level.

        // If the file ends with a newline, form the EOF token on the newline itself,
        // rather than "on the line following it", which doesn't exist.  This makes
        // diagnostics relating to the end of file include the last file that the user
        // actually typed, which is goodness.
        if (curLexer != null)
        {
            int endPos = curLexer.buffer.length;
            if (endPos != 0 && (curLexer.buffer[endPos - 1] == '\n'
                    || curLexer.buffer[endPos - 1] == '\r'))
                --endPos;

            // Handle \n\r and \r\n:
            if (endPos != 0 && (curLexer.buffer[endPos - 1] == '\n'
                    || curLexer.buffer[endPos] == '\r') && (
                    curLexer.buffer[endPos - 1] != curLexer.buffer[endPos]))
                --endPos;

            result.startToken();
            curLexer.bufferPtr = endPos;
            curLexer.formTokenWithChars(result, endPos, TokenKind.Eof);

            // We are done with the #include file.
            curLexer = null;
        }

        // This is the end of the top-level file.  If the pp_macro_not_used
        // diagnostic is enabled, look for macros that have not been used.
        if (getDiagnostics().getDiagnosticLevel(pp_macro_not_used)
                != Diagnostic.Level.Ignored)
        {
            for (Map.Entry<IdentifierInfo, MacroInfo> pair : macros.entrySet())
            {
                if (!pair.getValue().isUsed())
                    diag(pair.getValue().getDefinitionLoc(), pp_macro_not_used);
            }
        }
        return true;
    }

    /**
     * To lex a token from the preprocessor, just pull a token from the
     * current lexer or macro object.
     *
     * @param result
     */
    public void lex(Token result)
    {
        if (curLexer != null)
            curLexer.lex(result);
        else if (curTokenLexer != null)
            curTokenLexer.lex(result);
    }

    public void handleDirective(Token result)
    {
        // TODO: 17-4-28 完善处理条件编译指令
        // FIXME: Traditional: # with whitespace before it not recognized by K&R?

        // We just parsed a # character at the start of a line, so we're in directive
        // mode.  Tell the lexer this so any newlines we see will be converted into an
        // EOM token (which terminates the directive).
        curLexer.parsingPreprocessorDirective = true;

        ++NumDirectives;

        // We are about to read a token.  For the multiple-include optimization FA to
        // work, we have to remember if we had read any tokens *before* this 
        // pp-directive.
        boolean ReadAnyTokensBeforeDirective = curLexer.miOpt.hasReadAnyTokens();

        // Save the '#' token in case we need to return it later.
        Token savedHash = result;

        // Read the next token, the directive flavor.  This isn't expanded due to
        // C99 6.10.3p8.
        lexUnexpandedToken(result);

        // C99 6.10.3p11: Is this preprocessor directive in macro invocation?  e.g.:
        //   #define A(x) #x
        //   A(abc
        //     #warning blah
        //   def)
        // If so, the user is relying on non-portable behavior, emit a diagnostic.
        if (inMacroArgs)
            diag(result, ext_embedded_directive);

        boolean loop = false;
        do
        {
            switch (result.getKind())
            {
                case Eom:
                    return;   // null directive.
                case Comment:
                    // Handle stuff like "# /*foo*/ define X" in -E -C mode.
                    lexUnexpandedToken(result);
                    loop = true;
                    break;

                case Numeric_constant:  // # 7  GNU line marker directive.
                    if (getLangOptions().asmPreprocessor)
                        break;  // # 4 is not a preprocessor directive in .S files.
                    return handleDigitDirective(result);
                default:
                    IdentifierInfo II = result.getIdentifierInfo();
                    if (II == null)
                        break;  // Not an identifier.

                    // Ask what the preprocessor keyword ID is.
                    switch (II.getPPKeywordID())
                    {
                        default:
                            break;
                        // C99 6.10.1 - Conditional Inclusion.
                        case pp_if:
                            return handleIfDirective(result,
                                    ReadAnyTokensBeforeDirective);
                        case pp_ifdef:
                            return handleIfdefDirective(result, false, true/*not valid for miopt*/);
                        case pp_ifndef:
                            return handleIfdefDirective(result, true,
                                    ReadAnyTokensBeforeDirective);
                        case pp_elif:
                            return handleElifDirective(result);
                        case pp_else:
                            return handleElseDirective(result);
                        case pp_endif:
                            return handleEndifDirective(result);

                        // C99 6.10.2 - Source File Inclusion.
                        case pp_include:
                            return handleIncludeDirective(result);       // Handle #include.
                        case pp___include_macros:
                            return handleIncludeMacrosDirective(result); // Handle -imacros.

                        // C99 6.10.3 - Macro Replacement.
                        case pp_define:
                            return handleDefineDirective(result);
                        case pp_undef:
                            return handleUndefDirective(result);

                        // C99 6.10.4 - Line Control.
                        case pp_line:
                            return handleLineDirective(result);

                        // C99 6.10.5 - Error Directive.
                        case pp_error:
                            return handleUserDiagnosticDirective(result, false);

                        // C99 6.10.6 - Pragma Directive.
                        case pp_pragma:
                            handlePragmaDirective();
                            return;
                    }
                    break;
            }
        }while (loop);

        // If this is a .S file, treat unknown # directives as non-preprocessor
        // directives.  This is important because # may be a comment or introduce
        // various pseudo-ops.  Just return the # token and push back the following
        // token to be lexed next time.
        if (getLangOptions().asmPreprocessor)
        {
            Token[] Toks = new Token[2];
            // Return the # and the token after it.
            Toks[0] = savedHash;
            Toks[1] = result;
            // Enter this token stream so that we re-lex the tokens.  Make sure to
            // enable macro expansion, in case the token after the # is an identifier
            // that is expanded.
            enterTokenStream(Toks,false, true);
            return;
        }

        // If we reached here, the preprocessing token is not valid!
        diag(result, err_pp_invalid_directive);

        // Read the rest of the PP line.
        discardUntilEndOfDirective();

        // Okay, we're done parsing the directive.
    }

    /**
     * Increment the counters for the number of token
     * paste operations performed.  If fast was specified, this is a 'fast paste'
     * case we handled.
     *
     * @param isFast
     */
    public void incrementPasteCounter(boolean isFast)
    {
        if (isFast)
            ++NumFastTokenPaste;
        else
            ++NumTokenPaste;
    }

    public boolean handleEndOfTokenLexer(Token result)
    {
        assert curTokenLexer != null && curLexer == null : "Ending a macro when currently in a #include file!";

        // Handle this like a #include file being popped off the stack.
        return handleEndOfFile(result, true);
    }

    private boolean isNextPPTokenLParen()
    {
        int val;
        if (curLexer != null)
        {
            val = curLexer.isNextPPTokenLParen();
        }
        else
            val = curTokenLexer.isNextTokenLParen();

        if (val == 2)
        {
            // We have run off the end.  If it's a source file we don't
            // examine enclosing ones (C99 5.1.1.2p4).  Otherwise walk up the
            // macro stack.
            if (curLexer != null)
                return false;

            for (IncludeStackInfo info : includeMacroStack)
            {
                if (info.theLexer != null)
                    val = info.theLexer.isNextPPTokenLParen();
                else
                    val = info.theTokenLexer.isNextTokenLParen();

                if (val != 2)
                    break;

                if (info.theLexer != null)
                    return false;
            }
        }

        // Okay, if we know that the token is a '(', lex it and return.  Otherwise we
        // have found something that isn't a '(' or we found the end of the
        // translation unit.  In either case, return false.
        return val == 1;
    }

    private Pair<MacroArgs, SourceLocation> readFunctionLikeMacroArgs(Token macroName, MacroInfo mi)
    {
        SourceLocation macroEnd = null;
        int numFixedArgsLef = mi.getNumArgs();
        boolean isVariadic = mi.isVariadic();

        Token tok = new Token();

        // Read arguments as unexpanded tokens.  This avoids issues, e.g., where
        // an argument value in a macro could expand to ',' or '(' or ')'.
        lexUnexpandedToken(tok);
        assert tok.is(l_paren): "Error computing l-paren-ness?";

        ArrayList<Token> argTokens = new ArrayList<>(64);

        int numActuals = 0;
        while (tok.isNot(r_paren))
        {
            assert tok.is(l_paren) || tok
                    .is(comma) : "only expect argument separators here";

            int argTokenStart = argTokens.size();
            SourceLocation argStartLoc = tok.getLocation();

            int numParens = 0;
            while (true)
            {
                lexUnexpandedToken(tok);
                if (tok.is(Eof) || tok.is(Eom))
                {
                    // "#if f(<eof>" & "#if f(\n"
                    diag(macroName, err_unterm_macro_invoc);
                    macroName = tok;
                    return Pair.get(null, null);
                }
                else if (tok.is(r_paren))
                {
                    if (numParens-- == 0)
                    {
                        macroEnd = tok.getLocation();
                        break;
                    }
                }
                else if (tok.is(l_paren))
                {
                    ++numParens;
                }
                else if (tok.is(comma) && numParens == 0)
                {
                    // Comma ends this argument if there are more fixed arguments expected.
                    // However, if this is a variadic macro, and this is part of the
                    // variadic part, then the comma is just an argument token.
                    if (!isVariadic)
                        break;
                    if (numFixedArgsLef > 1)
                        break;
                }
                else if (tok.is(Comment) && !keepMacroComments)
                {
                    continue;
                }
                else if (tok.getIdentifierInfo() != null)
                {
                    // Reads a identifier token.

                    // Reading macro arguments can cause macros that we are currently
                    // expanding from to be popped off the expansion stack.  Doing so causes
                    // them to be reenabled for expansion.  Here we record whether any
                    // identifiers we lex as macro arguments correspond to disabled macros.
                    // If so, we mark the token as noexpand.  This is a subtle aspect of
                    // C99 6.10.3.4p2.
                    MacroInfo m = getMacroInfo(tok.getIdentifierInfo());
                    if (m != null && !m.isEnabled())
                        tok.setFlag(DisableExpand);
                }
                argTokens.add(tok);
            }

            // If this was an empty argument list foo(), don't add this as an empty
            // argument.
            if (argTokens.isEmpty() && tok.getKind() == r_paren)
                break;

            // If this is not a variadic macro, and too many args were specified, emit
            // an error.
            if (!isVariadic && numFixedArgsLef == 0)
            {
                if (argTokens.size() != argTokenStart)
                    argStartLoc = argTokens.get(argTokenStart).getLocation();

                diag(argStartLoc, err_too_many_args_in_macro_invoc);
                return Pair.get(null, null);
            }

            // Empty arguments are standard in C99 and supported as an extension in
            // other modes.
            if(argTokens.size() == argTokenStart && !langInfo.c99)
            {
                diag(tok, ext_empty_fnmacro_arg);
            }

            Token eofToken = new Token();
            eofToken.startToken();
            eofToken.setKind(Eof);
            eofToken.setLocation(tok.getLocation());
            eofToken.setLength(0);
            argTokens.add(eofToken);
            ++numActuals;
            assert numFixedArgsLef != 0: "Too many arguments parsed";
            --numFixedArgsLef;
        }

        int minArgsExpected = mi.getNumArgs();

        boolean isVarargsElided = false;

        if (numActuals < minArgsExpected)
        {
            // There are several cases where too few arguments is ok, handle them now.
            if (numActuals == 0 && minArgsExpected == 1)
            {
                // #define A(X)  or  #define A(...)   ---> A()

                isVarargsElided = mi.isVariadic();
            }
            else if (mi.isVariadic() &&
                    (numActuals + 1==minArgsExpected     // A(x, ...) -> A(X)
                     || (numActuals == 0 && minArgsExpected == 2))  // A(x,...) -> A()
                    )
            {
                // Varargs where the named vararg parameter is missing: ok as extension.
                // #define A(x, ...)
                // A("blah")
                diag(tok, ext_missing_varargs_arg);

                isVarargsElided = true;
            }
            else
            {
                diag(tok, err_too_few_args_in_macro_invoc);
                return Pair.get(null, null);
            }

            SourceLocation endLoc = tok.getLocation();
            tok.startToken();
            tok.setKind(Eof);
            tok.setLocation(endLoc);
            tok.setLength(0);
            argTokens.add(tok);

            // If we expect two arguments, add both as empty.
            if (numActuals == 0 && minArgsExpected == 2)
                argTokens.add(tok);
        }
        else if (numActuals > minArgsExpected &&!mi.isVariadic())
        {
            // Emit the diagnostic at the macro name in case there is a missing ).
            // Emitting it at the , could be far away from the macro name.
            diag(macroName, err_too_many_args_in_macro_invoc);
            return Pair.get(null, null);
        }

        Token[] toks = new Token[argTokens.size()];
        argTokens.toArray(toks);
        return Pair.get(MacroArgs.create(mi, toks, isVarargsElided), macroEnd);
    }

    private void handlePragmaDirective()
    {
        ++NumPragma;
        Token tok = new Token();
        pragmaHandlers.handlePragma(this, tok);

        // If the pragma handler didn't read the rest of the line, consume it now.
        if (curLexer != null && curLexer.parsingPreprocessorDirective)
        {
            discardUntilEndOfDirective();
        }
    }

    private void handlePragma(Token tok)
    {
        SourceLocation pragmaLoc = tok.getLocation();

        // Read the '(.
        lex(tok);

        if (tok.isNot(l_paren))
        {
            diag(pragmaLoc, err_pragma_comment_malformed);
            return;
        }

        // Read the '"..."'.
        lex(tok);
        if (tok.isNot(String_literal))
        {
            diag(pragmaLoc, err_pragma_comment_malformed);
            return;
        }

        // Remember the string.
        char[] strVal = getSpelling(tok).toCharArray();

        // Read the ')'.
        lex(tok);
        if (tok.isNot(r_paren))
        {
            diag(pragmaLoc, err_pragma_comment_malformed);
            return;
        }

        SourceLocation rparenLoc = tok.getLocation();

        // Remove the front quote, replacing it with a space, so that the pragma
        // contents appear to have a space before them.
        strVal[0] = ' ';

        // Replace the terminating quote with a \n.
        strVal[strVal.length - 1] = '\n';

        // Remove escaped quotes and escapes.
        int e = strVal.length;
        for (int i = 0; i < e - 1; i++)
        {
            if (strVal[i] == '\\' && (strVal[i+1] == '\\'
                    || strVal[i+1] == '"') )
            {
                // remove the i'th element.
                for (int j = i + 1; j < e; j++)
                    strVal[j - 1] = strVal[j];
                --e;
            }
        }

        // Plop the string (including the newline and trailing null) into a buffer
        // where we can lex it.
        Token tmpTok = new Token();
        tmpTok.startToken();
        createString(String.valueOf(strVal, 0, e), tmpTok, new SourceLocation());
        SourceLocation tokLoc = tmpTok.getLocation();

        // Make and enter a lexer object so that we lex and expand the tokens just
        // like any others.
        Lexer l = Lexer.createPragmaLexer(tokLoc, pragmaLoc, rparenLoc, e, this);

        enterSourceFileWithLexer(l, null);

        // Whatever anything happened, lex this as a #pragma directive.
        handlePragmaDirective();

        // Finally, return whatever came after the pragma directive.
        lex(tok);
    }

    /**
     * Given a location that specifies the start of a
     * token, return a new location that specifies a character within the token.
     * @param tokStart
     * @param charNo
     * @return
     */
    private SourceLocation advanceToTokenCharacter(SourceLocation tokStart, int charNo)
    {
        Token.StrData charData = sourceMgr.getCharacterData(tokStart);
        char[] buffer = charData.buffer;
        int offset = charData.offset;
        if (charNo == 0 && Lexer.isObviouslySimpleCharacter(buffer[offset]))
        {
            return tokStart;
        }

        int physOffset = offset;
        while (Lexer.isObviouslySimpleCharacter(buffer[offset]))
        {
            if (charNo == 0)
                return tokStart.getFileLocWithOffset(physOffset);
            ++offset;
            --charNo;
            ++physOffset;
        }

        for (; charNo != 0; --charNo)
        {
            OutParamWrapper<Integer> size = new OutParamWrapper<>(0);
            Lexer.getCharAndSizeSlowNoWarn(buffer, offset, size, langInfo);
            offset += size.get();
            physOffset += size.get();
        }

        if (!Lexer.isObviouslySimpleCharacter(buffer[offset]))
            physOffset = Lexer.skipEscapedNewLine(buffer, offset);

        return tokStart.getFileLocWithOffset(physOffset);
    }

    /**
     * Computes the Date and Time for the current zone.
     */
    private void computeDATE_TIME()
    {
        String[] months =
        {
            "Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"
        };

        Calendar can = Calendar.getInstance();
        String tmp = String.format("%s %2d %4d", months[can.get(Calendar.MONTH)],
                can.get(Calendar.DAY_OF_MONTH), can.get(Calendar.YEAR) + 1900);

        Token tmpTok = new Token();
        tmpTok.startToken();
        createString(tmp, tmpTok, new SourceLocation());
        DATELoc = tmpTok.getLocation();

        tmp = String.format("%02d:%02d:%02d", can.get(Calendar.HOUR_OF_DAY),
                can.get(Calendar.MINUTE),
                can.get(Calendar.SECOND));
        createString(tmp, tmpTok, new SourceLocation());
        TIMELoc = tmpTok.getLocation();
    }

    /**
     * If an identifier token is read that is to be expanded
     * as a builtin macro, handle it and return the next token as 'tok'.
     * @param tok
     */
    private void expandBuiltinMacro(Token tok)
    {
        // Figure out which token this is.
        IdentifierInfo ii = tok.getIdentifierInfo();
        assert ii != null : "Can't be a tok without id info!";

        // If this is an _Pragma directive, expand it, invoke the pragma handler, then
        // lex the token after it.
        if (ii.equals(Ident_Pragma))
        {
            handlePragma(tok);
            return;
        }
        
        ++NumBuiltinMacroExpanded;

        String TmpBuffer;

        // Set up the return result.
        tok.setIdentifierInfo(null);
        tok.clearFlag(NeedsCleaning);

        if (ii.equals(Ident__LINE__)) 
        {
            // C99 6.10.8: "__LINE__: The presumed line number (within the current
            // source file) of the current source line (an integer constant)".  This can
            // be affected by #line.
            SourceLocation Loc = tok.getLocation();

            // Advance to the location of the first _, this might not be the first byte
            // of the token if it starts with an escaped newline.
            Loc = advanceToTokenCharacter(Loc, 0);

            // One wrinkle here is that GCC expands __LINE__ to location of the *end* of
            // a tok instantiation.  This doesn't matter for object-like macros, but
            // can matter for a function-like tok that expands to contain __LINE__.
            // Skip down through instantiation points until we find a file loc for the
            // end of the instantiation history.
            Loc = sourceMgr.getInstantiationRange(Loc).getEnd();
            PresumedLoc PLoc = sourceMgr.getPresumedLoc(Loc);

            // __LINE__ expands to a simple numeric value.
            TmpBuffer = String.format("%d", PLoc.getLine());
            tok.setKind(Numeric_constant);
            createString(TmpBuffer, tok, tok.getLocation());
        } else if (ii == Ident__FILE__ || ii == Ident__BASE_FILE__) 
        {
            // C99 6.10.8: "__FILE__: The presumed name of the current source file (a
            // character string literal)". This can be affected by #line.
            PresumedLoc PLoc = sourceMgr.getPresumedLoc(tok.getLocation());

            // __BASE_FILE__ is a GNU extension that returns the top of the presumed
            // #include stack instead of the current file.
            if (ii == Ident__BASE_FILE__) 
            {
                diag(tok, ext_pp_base_file);
                SourceLocation NextLoc = PLoc.getIncludeLoc();
                while (NextLoc.isValid()) {
                    PLoc = sourceMgr.getPresumedLoc(NextLoc);
                    NextLoc = PLoc.getIncludeLoc();
                }
            }

            // Escape this filename.  Turn '\' -> '\\' '"' -> '\"'
            String FN = PLoc.getFilename();
            FN = '"' + Lexer.stringify(FN) + '"';
            tok.setKind(String_literal);
            createString(FN, tok, tok.getLocation());
        }
        else if (ii.equals(Ident__DATE__))
        {
            if (!DATELoc.isValid())
                computeDATE_TIME();
            
            tok.setKind(String_literal);
            tok.setLength("\"Mmm dd yyyy\"".length());
            tok.setLocation(sourceMgr.createInstantiationLoc(DATELoc, tok.getLocation(),
                    tok.getLocation(),
                    tok.getLength(), 0, 0));
        }
        else if (ii.equals(Ident__TIME__))
        {
            if (!TIMELoc.isValid())
                computeDATE_TIME();
            tok.setKind(String_literal);
            tok.setLength("\"hh:mm:ss\"".length());
            tok.setLocation(sourceMgr.createInstantiationLoc(TIMELoc, tok.getLocation(),
                    tok.getLocation(),
                    tok.getLength(), 0, 0));
        } else if (ii == Ident__INCLUDE_LEVEL__) 
        {
            diag(tok, ext_pp_include_level);

            // Compute the presumed include depth of this token.  This can be affected
            // by GNU line markers.
            int Depth = 0;

            PresumedLoc PLoc = sourceMgr.getPresumedLoc(tok.getLocation());
            PLoc = sourceMgr.getPresumedLoc(PLoc.getIncludeLoc());
            for (; PLoc.isValid(); ++Depth)
                PLoc = sourceMgr.getPresumedLoc(PLoc.getIncludeLoc());

            // __INCLUDE_LEVEL__ expands to a simple numeric value.
            TmpBuffer = String.format("%d", Depth);
            tok.setKind(Numeric_constant);
            createString(TmpBuffer, tok, tok.getLocation());
        }
        else if (ii.equals(Ident__TIMESTAMP__))
        {
            // MSVC, ICC, GCC, VisualAge C++ extension.  The generated string should be
            // of the form "Ddd Mmm dd hh::mm::ss yyyy", which is returned by asctime.
            diag(tok, ext_pp_timestamp);

            // Get the file that we are lexing out of.  If we're currently lexing from
            // a tok, dig into the include stack.
            Path CurFile = null;
            PreprocessorLexer TheLexer = getCurrentFileLexer();

            if (TheLexer != null)
                CurFile = sourceMgr.getFileEntryForID(TheLexer.getFileID());

            // If this file is older than the file it depends on, emit a diagnostic.
            String result;
            if (CurFile != null)
            {
                long tt = CurFile.toFile().lastModified();
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(tt);
                result = cal.getTime().toString();
            }
            else
            {
                result = "??? ??? ?? ??:??:?? ????\n";
            }
            StringBuilder buf = new StringBuilder('"');
            buf.append(result);
            buf.append('"'); // Replace the newline with a quote.
            tok.setKind(String_literal);
            createString(buf.toString(), tok, tok.getLocation());
        }
        else if (ii == Ident__COUNTER__)
        {
            diag(tok, ext_pp_counter);

            // __COUNTER__ expands to a simple numeric value.
            TmpBuffer = String.format("%d", counterValue++);
            tok.setKind(Numeric_constant);
            createString(TmpBuffer, tok, tok.getLocation());
        }
        else
        {
            assert false : "Unknown identifier!";
        }
    }

    /**
     * Return true if MI, which has a single token
     * in its expansion, currently expands to that token literally.
     * @param mi
     * @param macroIdent
     * @param pp
     * @return
     */
    private static boolean isTrivalSingleTokenExpansion(MacroInfo mi,
            IdentifierInfo macroIdent, Preprocessor pp)
    {
        IdentifierInfo ii = mi.getReplacementToken(0).getIdentifierInfo();

        if (ii == null) return true;

        if (ii.isHasMacroDefinition() && pp.getMacroInfo(ii).isEnabled()
                // Fast expanding "#define X X" is ok, because X would be disabled.
                &&!ii.equals(macroIdent))
        {
            return false;
        }

        if (mi.isObjectLike()) return true;

        // If this is an object-like macro invocation, it is safe to trivially expand
        // it.
        for (IdentifierInfo info : mi.getArgumentList())
            if (info.equals(ii))
                // If the first token in macro body is equivalent to some argument,
                // then, if can not be expanded trivially.
                return false;
        return true;
    }
    /**
     * If an identifier token is read that is to be
     * /// expanded as a macro, handle it and return the next token as 'Identifier'.
     *
     * @param identifier
     * @param mi
     * @return
     */
    private boolean handleMacroExpandedIdentifier(
            OutParamWrapper<Token> identifier, MacroInfo mi)
    {
        Token ident = identifier.get();
        if (callbacks != null)
            callbacks.macroExpands(ident, mi);

        // If this is a macro exapnsion in the "#if !defined(x)" line for the file,
        // then the macro could expand to different things in other contexts, we need
        // to disable the optimization in this case.
        if (curLexer != null)
            curLexer.miOpt.expandedMacro();

        // If this is a builtin macro, like __LINE__ or _Pragma, handle it specially.
        if (mi.isBuiltinMacro())
        {
            expandBuiltinMacro(ident);
            return false;
        }

        // A object for residing actual arguments.
        // If this is a function-like macro expansion, this contains,
        /// for each macro argument, the list of tokens that were provided to the
        /// invocation.
        MacroArgs args = null;

        // Remember where the end of the instantiation occurred.  For an object-like
        // macro, this is the ident.  For a function-like macro, this is the ')'.
        SourceLocation instantiationEnd = ident.getLocation();

        // If this is a function-like macro, read the arguments.
        if (mi.isFunctionLike())
        {
            // C99 6.10.3p10: If the preprocessing token immediately after the the macro
            // name isn't a '(', this macro should not be expanded.
            if (!isNextPPTokenLParen())
                return true;

            inMacroArgs = true;
            Pair<MacroArgs, SourceLocation> res = readFunctionLikeMacroArgs(ident, mi);
            args = res.first;
            if (res.second != null)
                instantiationEnd = res.second;

            // We are exiting the macro args.
            inMacroArgs = false;

            if (args == null)
                return false;

            ++NumFnMacroExpanded;
        }
        else
        {
            ++NumMacroExpanded;
        }

        // Notice that this macro has been used.
        mi.setIsUsed(true);

        // If we started lexing a macro, enter the macro expansion body.

        // If this macro expands to no tokens, don't bother to push it onto the
        // expansion stack, only to take it right back off.
        if (mi.getNumTokens() == 0)
        {

            boolean hasLeadingSpace = ident.hasLeadingSpace();
            boolean isAtStartOfLine = ident.isAtStartOfLine();

            lex(ident);

            // If the ident isn't on some OTHER line, inherit the leading
            // whitespace/first-on-a-line property of this token.  This handles
            // stuff like "! XX," -> "! ," and "   XX," -> "    ,", when XX is
            // empty.
            if (!ident.isAtStartOfLine())
            {
                if (isAtStartOfLine) ident.setFlag(StartOfLine);
                if (hasLeadingSpace) ident.setFlag(LeadingSpace);
            }

            ++NumFastMacroExpanded;
            return false;
        }
        else if (mi.getNumTokens() == 1 &&
                isTrivalSingleTokenExpansion(mi, ident.getIdentifierInfo(),
                        this))
        {
            // Otherwise, if this macro expands into a single trivially-expanded
            // token: expand it now.  This handles common cases like
            // "#define VAL 42".


            boolean hasLeadingSpace = ident.hasLeadingSpace();
            boolean isAtStartOfLine = ident.isAtStartOfLine();

            SourceLocation instantiationLoc = ident.getLocation();

            ident = mi.getReplacementToken(0);
            // Replace the result token.
            ident.setFlagValue(StartOfLine, isAtStartOfLine);
            ident.setFlagValue(LeadingSpace, hasLeadingSpace);

            SourceLocation loc = sourceMgr.createInstantiationLoc(ident.getLocation(),
                    instantiationLoc,
                    instantiationEnd, ident.getLength(), 0, 0);

            ident.setLocation(loc);

            // If this is #define X X, we must mark the result as unexpandible.
            IdentifierInfo newII = ident.getIdentifierInfo();
            if (newII != null)
            {
                if (getMacroInfo(newII).equals(mi))
                    ident.setFlag(DisableExpand);
            }

            // Since this is not an ident token, it can't be macro expanded, so
            // we're done.
            ++NumFastMacroExpanded;
            return false;

        }

        // Start expanding the macro.
        enterMacro(ident, instantiationEnd, args);

        // Now that the macro is at the top of the include stack, ask the
        // preprocessor to read the next token from it.
        lex(ident);
        identifier.set(ident);
        return false;
    }

    public void handlePragmaMark()
    {
        assert curLexer != null : "No current lexer?";
        curLexer.readToEndOfLine();
    }

    /**
     * Return true if we're in the top-level file, not in a
     * #include.  This looks through macro expansions and active _Pragma lexers.
     * @return
     */
    private boolean isInPrimaryFile()
    {
        if (isFileLexer())
            return includeMacroStack.isEmpty();

        assert isFileLexer(includeMacroStack.get(0))
                :"Top level include stack isn't our primary lexer?";
        for (IncludeStackInfo info : includeMacroStack)
            if (isFileLexer(info))
                return false;

        return true;
    }

    /**
     * Handle #pragma once.  OnceTok is the 'once'.
     * @param onceTok
     */
    public void handlePragmaOnce(Token onceTok)
    {
        if (isInPrimaryFile())
        {
            diag(onceTok, pp_pragma_once_in_main_file);
            return;
        }

        headerInfo.markFileIncludeOnce(getCurrentFileLexer().getFileEntry());
    }

    /**
     * Ensure that the next token is a eom token.  If
     * not, emit a diagnostic and consume up until the eom.  If EnableMacros is
     * true, then we consider macros that expand to zero tokens as being ok.
     * @param dirType
     * @param enableMacros
     */
    public void checkEndOfDirective(String dirType, boolean enableMacros)
    {
        Token tmp = new Token();
        if (enableMacros)
            lex(tmp);
        else
            lexUnexpandedToken(tmp);

        while (tmp.is(Comment))
        {
            lexUnexpandedToken(tmp);
        }

        if (tmp.isNot(Eom))
        {
            // Add a fixit in GNU/C99 mode.  Don't offer a fixit for strict-C89,
            // because it is more trouble than it is worth to insert /**/ and check that
            // there is no /**/ in the range also.
            FixItHint hint = null;
            if (langInfo.gnuMode || langInfo.c99)
                hint = FixItHint.createInsertion(tmp.getLocation(), "//");
            diag(tmp, ext_pp_extra_tokens_at_eol).addTaggedVal(dirType).addFixItHint(hint);
            discardUntilEndOfDirective();
        }
    }

    /**
     * Read and discard all tokens remaining on the
     * current line until the eom token is found.
     */
    private void discardUntilEndOfDirective()
    {
        Token tmp = new Token();
        do
        {
            lexUnexpandedToken(tmp);
        }while (tmp.isNot(Eom));
    }

    /**
     * This is just like Lex, but this disables macro
     * expansion of identifier tokens.
     * @param result
     */
    private void lexUnexpandedToken(Token result)
    {
        boolean oldVal = disableMacroExpansion;
        disableMacroExpansion = true;

        lex(result);

        disableMacroExpansion = oldVal;
    }

    public Token lookAhead(int n)
    {
        if (cachedLexPos + n < cachedTokens.size())
            return cachedTokens.get(cachedLexPos + n);
        else
            return peekAhead(n+1);
    }

    private Token peekAhead(int n)
    {
        assert cachedLexPos + n > cachedTokens.size(): "Confused caching.";
        for (int c = cachedLexPos + n - cachedTokens.size(); c > 0; --c)
        {
            Token res = new Token();
            lex(res);
            cachedTokens.add(res);
        }
        return cachedTokens.get(cachedTokens.size() - 1);
    }
}
