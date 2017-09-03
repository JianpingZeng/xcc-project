package jlang.clex;
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
import jlang.clex.PPCallBack.FileChangeReason;
import jlang.clex.Preprocessor.DefinedTracker.TrackerState;
import jlang.diag.Diagnostic;
import jlang.diag.FixItHint;
import jlang.diag.FullSourceLoc;
import tools.APSInt;
import jlang.support.*;
import tools.OutParamWrapper;
import tools.Pair;

import java.nio.file.Path;
import java.util.*;

import static java.lang.System.err;
import static jlang.support.CharacteristicKind.C_User;
import static jlang.clex.Token.TokenFlags.*;
import static jlang.clex.PPCallBack.FileChangeReason.RenameFile;
import static jlang.clex.Preprocessor.DefinedTracker.TrackerState.DefinedMacro;
import static jlang.clex.Preprocessor.DefinedTracker.TrackerState.NotDefMacro;
import static jlang.clex.TokenKind.*;
import static jlang.diag.DiagnosticCommonKindsTag.*;
import static jlang.diag.DiagnosticLexKindsTag.*;

/**
 * <p>
 * A C Preprocessor.
 * The Preprocessor outputs a token stream which does not need
 * re-lexing for C or C++.
 * </p>
 * <pre>
 * Source file asmName and line number information is conveyed by lines of the form:
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
 * After the file asmName comes zero or more flags, which are `1', `2',
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
     * not expanding a macro and we are lexing directly from source code.
     *  Only one of curLexer, or curTokenLexer will be non-null.
     */
    private Lexer curLexer;

    private Path curDirLookup;

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

        StrData tokStart = sourceMgr.getCharacterData(tok.getLocation());
        if (!tok.needsCleaning())
            return String.valueOf(Arrays.copyOfRange(tokStart.buffer,
                    tokStart.offset, tok.getLength() + tokStart.offset));

        StringBuilder result = new StringBuilder();
        for (int i = tokStart.offset, e = tok.getLength() + tokStart.offset; i < e; )
        {
            OutParamWrapper<Integer> charSize = new OutParamWrapper<>(0);
            result.append(
                    Lexer.getCharAndSizeNoWarn(tokStart.buffer, i,
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
            tok.setLiteralData(scrachBuf.getBuffer(), dest);
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
        assert identifier.is(TokenKind.identifier) : "Not an identifier!";
        assert identifier.getIdentifierInfo()
                == null : "identifier already exists!";

        IdentifierInfo ii;
        if (startPos != 0 && !identifier.needsCleaning())
        {
            ii = getIdentifierInfo(String.valueOf(buf, startPos, identifier.getLength()));
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
                diag(token, err_pp_used_poisoned_id).emit();
            else
                diag(token, ext_pp_bad_vaargs_use).emit();
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
            diag(token, ext_token_used).emit();
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
        this.target = target;
        headerInfo = headerSearch;
        this.sourceMgr = sourceMgr;
        identifiers = new IdentifierTable(langOptions, iilookup);
        commentHandlers = new ArrayList<>();
        includeMacroStack = new LinkedList<>();
        macros = new HashMap<>();
        scrachBuf = new ScratchBuffer(sourceMgr);
        cachedTokens = new ArrayList<>();
        backtrackPositions = new TIntArrayList();

        counterValue = 0;   // __COUNTER__ starts at 0.
        NumDirectives = NumDefined = NumUndefined = NumPragma = 0;
        NumIf = NumElse = NumEndif = 0;
        numEnteredSourceFiles = 0;
        NumMacroExpanded = 0;
        NumFnMacroExpanded = 0;
        NumBuiltinMacroExpanded = 0;
        NumFastMacroExpanded = 0;
        NumTokenPaste = 0;
        NumFastTokenPaste = 0;
        MaxIncludeStackDepth = 0;
        NumSkipped = 0;

        // Default to discarding comments.
        keepComments = false;
        keepMacroComments = false;

        // Macro expansion is enabled.
        disableMacroExpansion = false;
        inMacroArgs = false;


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
    public void addPragmaHandler(String namespace, PragmaHandler handler)
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
                                + " handler with the same asmName!";
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
        return l != null && !l.isPragmaLexer();
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
        assert !commentHandlers.contains(handler)
                : "Comment handler already exist";
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
    public void enterMainSourceFile()
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

        // Generate memory buffer for built-in predefined macroes.
        MemoryBuffer sb = MemoryBuffer
                .getMemBuffer(buffer.toString(), "<built-in>");
        //assert sb != null : "Cannot fail to create predefined source buffer";
        FileID fid = sourceMgr.createFileIDForMemBuffer(sb);
        assert !fid.isInvalid() : "Could not create FileID for predefines?";

        // Star parsing the predefines.
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
            int endPos = curLexer.buffer.length - 1;
            if (endPos != 0 && (curLexer.buffer[endPos - 1] == '\n'
                    || curLexer.buffer[endPos - 1] == '\r'))
                --endPos;

            // Handle \n\r and \r\n:
            if (endPos != 0 && (curLexer.buffer[endPos - 1] == '\n'
                    || curLexer.buffer[endPos - 1] == '\r') &&
                    (curLexer.buffer[endPos - 1] != curLexer.buffer[endPos]))
                --endPos;

            result.startToken();
            curLexer.bufferPtr = endPos;
            curLexer.formTokenWithChars(result, endPos, TokenKind.eof);

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
                    diag(pair.getValue().getDefinitionLoc(), pp_macro_not_used).emit();
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
        // First, lookup the cached tokens list to see whether there is cached
        // token, if so, just fetch one from cache instead of calling to lex
        if (!cachedTokens.isEmpty())
        {
            if (cachedLexPos < cachedTokens.size())
            {
                Token t = cachedTokens.get(cachedLexPos++);
                result.setKind(t.getKind());
                result.setFlag(t.getFlags());
                result.setLocation(t.getLocation());
                result.setLength(t.getLength());
                if (t.isLiteral())
                    result.setLiteralData(t.getLiteralData());
                else
                    result.setIdentifierInfo(t.getIdentifierInfo());
                return;
            }
            else
            {
                // In this case, all of cached token were consumed by Lex.
                cachedTokens.clear();
                cachedLexPos = 0;
            }
        }
        if (curLexer != null)
            curLexer.lex(result);
        else if (curTokenLexer != null)
            curTokenLexer.lex(result);
    }

    /**
     * Convert a numeric token into an int value, emitting
     * Diagnostic DiagID if it is invalid, and returning the value in value.
     * @param digitTok
     * @param lineNo
     * @param diagID
     * @param pp
     * @return
     */
    private static boolean getLineValue(
            Token digitTok,
            OutParamWrapper<Integer> lineNo,
            int diagID,
            Preprocessor pp)
    {
        if (digitTok.isNot(numeric_constant))
        {
            pp.diag(digitTok.getLocation(), diagID).emit();
            if (digitTok.isNot(eom))
                pp.discardUntilEndOfDirective();
            return true;
        }

        String digitStr = pp.getSpelling(digitTok);

        int val = 0;
        for (int i = 0, e = digitStr.length(); i < e; ++i)
        {
            if (!Character.isDigit(digitStr.charAt(i)))
            {
                pp.diag(pp.advanceToTokenCharacter(digitTok.getLocation(), i),
                        err_pp_line_digit_sequence).emit();
                pp.discardUntilEndOfDirective();
                return true;
            }

            int nextVal = val * 10 + (digitStr.charAt(i) - '0');
            if (nextVal < val)
            {
                // overflow.
                pp.diag(digitTok, diagID).emit();
                pp.discardUntilEndOfDirective();
                return true;
            }
            val = nextVal;
        }

        // Reject 0, this is needed both by #line numbers and flags.
        if (val == 0)
        {
            pp.diag(digitTok, diagID).emit();
            pp.discardUntilEndOfDirective();
            return true;
        }

        if (digitStr.charAt(0) == '0')
            pp.diag(digitTok.getLocation(), warn_pp_line_decimal).emit();
        lineNo.set(val);
        return false;
    }

    /**
     * Parse and validate any flags at the end of a GNU line
     * marker directive.
     * @param isFileEntry
     * @param isFileExit
     * @param isSystemHeader
     * @param pp
     * @return
     */
    private static boolean readLineMarkerFlags(
            OutParamWrapper<Boolean> isFileEntry,
            OutParamWrapper<Boolean> isFileExit,
            OutParamWrapper<Boolean> isSystemHeader,
            Preprocessor pp)
    {
        int flagVal = 0;
        Token flagTok = new Token();
        pp.lex(flagTok);

        if (flagTok.is(eom)) return false;
        OutParamWrapper<Integer> x = new OutParamWrapper<>(flagVal);
        if (getLineValue(flagTok, x, err_pp_linemarker_invalid_filename, pp))
            return true;

        flagVal = x.get();

        if (flagVal == 1)
        {
            isFileEntry.set(true);

            pp.lex(flagTok);
            if (flagTok.is(eom)) return false;
            x = new OutParamWrapper<>(flagVal);
            if (getLineValue(flagTok, x, err_pp_linemarker_invalid_flag, pp))
                return true;

            flagVal = x.get();
        }
        else if (flagVal == 2)
        {
            isFileExit.set(true);

            SourceManager sgr = pp.getSourceManager();

            FileID curFileID = sgr.getDecomposedInstantiationLoc(flagTok.getLocation()).first;
            PresumedLoc ploc = sgr.getPresumedLoc(flagTok.getLocation());

            SourceLocation loc = ploc.getIncludeLoc();
            if (!loc.isValid() || !sgr.getDecomposedInstantiationLoc(loc).first
                    .equals(curFileID))
            {
                pp.diag(flagTok, err_pp_linemarker_invalid_pop).emit();
                pp.discardUntilEndOfDirective();
                return true;
            }

            pp.lex(flagTok);
            if (flagTok.is(eom)) return false;
            if (getLineValue(flagTok, x, err_pp_linemarker_invalid_flag, pp))
                return true;

            flagVal = x.get();
        }

        if (flagVal != 3)
        {
            pp.diag(flagTok, err_pp_linemarker_invalid_flag).emit();
            pp.discardUntilEndOfDirective();
            return true;
        }

        isSystemHeader.set(true);

        pp.lex(flagTok);
        if (flagTok.is(eom)) return false;
        if (getLineValue(flagTok, x, err_pp_linemarker_invalid_flag, pp))
            return true;

        flagVal = x.get();

        if (flagVal != 4)
        {
            pp.diag(flagTok, err_pp_linemarker_invalid_flag).emit();
            pp.discardUntilEndOfDirective();
            return true;
        }

        // There are no more valid flags here.
        pp.diag(flagTok, err_pp_linemarker_invalid_flag).emit();
        pp.discardUntilEndOfDirective();
        return true;
    }

    /**
     * Handle a GNU line marker directive, whose syntax is
     * one of the following forms:
     * <pre>
     *     # 42
     *     # 42 "file" ('1' | '2')?
     *     # 42 "file" ('1' | '2')? '3' '4'?
     * </pre>
     * @param digitTok
     */
    private void handleDigitDirective(Token digitTok)
    {
        // Validate the number and convert it to an int.  GNU does not have a
        // line # limit other than it fit in 32-bits.
        OutParamWrapper<Integer> lineNo = new OutParamWrapper<>(0);
        if (getLineValue(digitTok, lineNo, err_pp_linemarker_requires_integer, this))
            return;

        Token strTok = new Token();
        lex(strTok);

        boolean isFileEntry = false, isFileExit = false;
        boolean isSystemHeader = false;
        int filenameID = -1;

        // If the StrTok is "eom", then it wasn't present.  Otherwise, it must be a
        // string followed by eom.
        if (strTok.is(eom));  // OK! just return
        else if (strTok.isNot(string_literal))
        {
            diag(strTok, err_pp_linemarker_invalid_filename).emit();
            discardUntilEndOfDirective();
            return;
        }
        else
        {
            // Parse and validate the string, converting it into a unique ID.
            StringLiteralParser literal = new StringLiteralParser(new Token[]{strTok}, this);;
            if (literal.hadError)
            {
                discardUntilEndOfDirective();
                return;
            }

            filenameID = sourceMgr.getLineTableFilenameID(literal.getString());

            // If a filename was present, read any flags that are present.
            OutParamWrapper<Boolean> x = new OutParamWrapper<>(isFileEntry),
            y = new OutParamWrapper<>(isFileExit),
            z = new OutParamWrapper<>(isSystemHeader);

            if (readLineMarkerFlags(x, y, z, this))
                return;
            isFileEntry = x.get();
            isFileExit = y.get();
            isSystemHeader = z.get();
        }

        // Create a line note with this information.
        sourceMgr.addLineNote(digitTok.getLocation(), lineNo.get(), filenameID,
                isFileEntry, isFileExit, isSystemHeader);

        if (callbacks != null)
        {
            FileChangeReason reason = RenameFile;
            if (isFileEntry)
                reason = FileChangeReason.EnterFile;
            else if (isFileExit)
                reason = FileChangeReason.ExitFile;
            CharacteristicKind kind = C_User;
            if (isSystemHeader)
                kind = CharacteristicKind.C_System;

            callbacks.fileChanged(digitTok.getLocation(), reason, kind);;
        }
    }

    static class PPValue
    {
        private SourceRange range;

        public APSInt val;

        public PPValue(int bitWidth)
        {
            val = new APSInt(bitWidth);
        }

        public int getBitWidth() { return val.getBitWidth(); }

        public boolean isUnsigned() {return val.isUnsigned(); }

        public SourceRange getRange()
        {
            return range;
        }

        public void setRange(SourceLocation loc)
        {
            range = new SourceRange(loc, loc);
        }

        public void setRange(SourceLocation l, SourceLocation r)
        {
            range = new SourceRange(l, r);
        }

        public void setBegin(SourceLocation loc)
        {
            range.setBegin(loc);
        }

        public void setEnd(SourceLocation loc)
        {
            range.setEnd(loc);
        }
    }

    static class DefinedTracker
    {
        // Each time a Value is evaluated, it returns information about whether the
        // parsed value is of the form defined(X), !defined(X) or is something else.
        enum TrackerState
        {
            DefinedMacro,
            NotDefMacro,
            Unknown,
        }

        /**
         *  When the state is DefinedMacro or NotDefinedMacro, this
         * indicates the macro that was checked.
         */
        TrackerState state;

        IdentifierInfo theMacro;
    }

    /**
     * Evaluate the token {@code peekTok} (and any others needed) and
     * return the computed value in {@code result}. This function also returns
     * information about the form of the expression in DT.  See above for
     * information on what DT means.
     *
     * If ValueLive is false, then this value is being evaluated in a context where
     * the result is not used.  As such, avoid diagnostics that relate to
     * evaluation.
     * @param result
     * @param peekTok
     * @param dt
     * @param valueLive
     * @param pp
     * @return Return true if there was an error parsing.
     */
    private static boolean evaluateValue(
            PPValue result,
            Token peekTok,
            DefinedTracker dt,
            boolean valueLive,
            Preprocessor pp)
    {
        dt.state = TrackerState.Unknown;

        // If this tokens spelling is a pp-identifier, check to see if it is
        // 'defined' or if it is a macro.
        IdentifierInfo ii = peekTok.getIdentifierInfo();
        if (ii != null)
        {
            // If this identifier isn't 'defined' and it wasn't macro expanded,
            // it returns into a simple 0.
            if (!ii.isStr("defined"))
            {
                if (valueLive)
                    pp.diag(peekTok, warn_pp_undef_identifier).emit();
                result.val.assign(0);
                result.val.setIsUnsigned(false); // '0' is signed intmat_t 0.
                pp.lexNonComment(peekTok);
                return false;
            }

            // Handle "defined X" and "defined(X)".
            result.setBegin(peekTok.getLocation());

            // Get the next token, don't expand it.
            pp.lexNonComment(peekTok);

            // Two options, it can either be a pp-identifier or a '('.
            SourceLocation lParenLoc = new SourceLocation();
            if (peekTok.is(l_paren))
            {
                // Found a left parenthesis, remember its localtion, skip it.
                lParenLoc = peekTok.getLocation();
                pp.lexUnexpandedToken(peekTok);
            }

            // if we don't have a pp-identifier now, this is an error.
            if ((ii = peekTok.getIdentifierInfo()) == null)
            {
                pp.diag(peekTok, err_pp_defined_requires_identifier).emit();
                return true;
            }

            // Otherwise, we got an identifier, is it defined?
            result.val.assign(ii.isHasMacroDefinition() ? 1 : 0);
            result.val.setIsUnsigned(false);  // result is signed.

            // If this is a macro, mark is as used.
            if (!result.val.eq(0) && valueLive)
            {
                pp.getMacroInfo(ii).setIsUsed(true);
            }

            // Consume identifier.
            result.setEnd(peekTok.getLocation());
            pp.lexNonComment(peekTok);

            // If we have seen a '(', ensure we also have a tailling ')'.
            if (lParenLoc.isValid())
            {
                if (!peekTok.is(r_paren))
                {
                    pp.diag(peekTok.getLocation(), err_pp_missing_rparen).emit();
                    pp.diag(lParenLoc, note_matching).addTaggedVal("(").emit();
                    return true;
                }

                // consume the ')'.
                result.setEnd(peekTok.getLocation());
                pp.lexNonComment(peekTok);
            }

            // Success, remember that we saw defined(X).
            dt.state = DefinedMacro;
            dt.theMacro = ii;
            return false;
        }

        switch (peekTok.getKind())
        {
            default:
                pp.diag(peekTok.getLocation(), err_pp_expr_bad_token_start_expr).emit();
                return true;
            case eom:
            case r_paren:
            {
                // If there is no expression, report and exit.
                pp.diag(peekTok, err_pp_expected_value_in_expr).emit();
                return true;
            }
            case numeric_constant:
            {
                String lit = pp.getSpelling(peekTok);
                NumericLiteralParser litParser =
                        new NumericLiteralParser(lit, peekTok.getLocation(), pp);
                if (litParser.hadError)
                {
                    return true;    // Error occurred.
                }
                if (litParser.isFloatingLiteral() || litParser.isImaginary)
                {
                    pp.diag(peekTok, err_pp_illegal_floating_literal).emit();
                    return true;
                }

                assert litParser.isIntegerLiteral():"Unknown ppnumber";

                // long long is C99 feature.
                if (!pp.getLangOptions().c99 && litParser.isLongLong)
                    pp.diag(peekTok, ext_longlong).emit();

                // Parse the integer lietal into result.
                if (litParser.getIntegerValue(result.val))
                {
                    // Overflow parsing integer literal.
                    if (valueLive) pp.diag(peekTok, warn_integer_too_large).emit();
                    result.val.setIsUnsigned(true);
                }
                else
                {
                    // Set the signedness of the result to match whether there was a U suffix
                    // or not.
                    result.val.setIsUnsigned(litParser.isUnsigned);

                    if (!litParser.isUnsigned && result.val.isNegative())
                    {
                        if (valueLive && litParser.getRadix() != 16)
                            pp.diag(peekTok, warn_integer_too_large_for_signed).emit();
                        result.val.setIsUnsigned(true);
                    }
                }

                // Consume the token.
                result.setRange(peekTok.getLocation());
                pp.lexNonComment(peekTok);
                return false;
            }
            case char_constant:
            {
                String tokStr = pp.getSpelling(peekTok);
                CharLiteralParser literal =
                        new CharLiteralParser(tokStr, peekTok.getLocation(), pp);
                if (literal.hadError())
                    return true;    // A diagnostic was already emitted.

                TargetInfo ti = pp.getTargetInfo();
                int numBits;
                if (literal.isMultChar())
                    numBits = ti.getIntWidth();
                else
                    numBits = ti.getCharWidth();

                // set the width.
                APSInt val = new APSInt(numBits);
                // set the value
                val.assign(literal.getValue());
                val.setIsUnsigned(false);

                if (result.val.getBitWidth() > val.getBitWidth())
                {
                    result.val = val.extend(result.val.getBitWidth());
                }
                else
                {
                    assert result.val.getBitWidth() == val.getBitWidth()
                            :"intmax_t smaller than char?";
                    result.val.assign(val);
                }

                // Consume the token.
                result.setRange(peekTok.getLocation());
                pp.lexUnexpandedToken(peekTok);
                return false;
            }
            case l_paren:
            {
                // Parse the value and if there are any binary operators involved, parse
                // them.
                SourceLocation start = peekTok.getLocation();
                pp.lexNonComment(peekTok);

                if (evaluateValue(result, peekTok, dt, valueLive, pp))
                    return true;

                if (peekTok.is(r_paren))
                {
                    // done!
                }
                else
                {
                    // Otherwise, we have something like (x+y), and we consumed '(x'.
                    if (evaluateDirectiveSubExpr(result, 1, peekTok, valueLive, pp))
                        return true;

                    if (peekTok.isNot(r_paren))
                    {
                        pp.diag(peekTok.getLocation(),err_pp_expected_rparen).
                                addSourceRange(result.getRange()).emit();
                        pp.diag(start, note_matching).addTaggedVal("(").emit();
                        return true;
                    }
                    dt.state = TrackerState.Unknown;
                }
                result.setRange(start, peekTok.getLocation());
                pp.lexNonComment(peekTok);
                return false;
            }
            case plus:
            {
                SourceLocation start = peekTok.getLocation();
                // unary plus doesn't modify the value.
                pp.lexNonComment(peekTok);
                if (evaluateValue(result, peekTok, dt, valueLive, pp))
                    return true;

                result.setBegin(start);
                return false;
            }
            case sub:
            {
                SourceLocation start = peekTok.getLocation();
                // unary minus doesn't modify the value.
                pp.lexNonComment(peekTok);
                if (evaluateValue(result, peekTok, dt, valueLive, pp))
                    return true;

                result.setBegin(start);
                // C99 6.5.3.3p3: The sign of the result matches the sign of the operand.
                result.val.negative();

                boolean overflow = !result.isUnsigned() && result.val.isMinSignedValue();
                if (overflow && valueLive)
                    pp.diag(peekTok.getLocation(), warn_pp_expr_overflow).emit();

                dt.state = TrackerState.Unknown;
                return false;
            }
            case tilde:
            {
                SourceLocation start = peekTok.getLocation();
                // unary ~ doesn't modify the value.
                pp.lexNonComment(peekTok);
                if (evaluateValue(result, peekTok, dt, valueLive, pp))
                    return true;

                result.setBegin(start);
                // C99 6.5.3.3p4: The sign of the result matches the sign of the operand.
                result.val.not();
                dt.state = TrackerState.Unknown;
                return false;
            }
            case bang:
            {
                SourceLocation start = peekTok.getLocation();
                // unary ! doesn't modify the value.
                pp.lexNonComment(peekTok);
                if (evaluateValue(result, peekTok, dt, valueLive, pp))
                    return true;

                result.setBegin(start);
                // C99 6.5.3.3p5: The sign of the result is 'int', aka it is signed.
                result.val.lNot();

                result.val.setIsUnsigned(false);
                if (dt.state == DefinedMacro)
                    dt.state = NotDefMacro;
                else if (dt.state == NotDefMacro)
                    dt.state = DefinedMacro;

                return false;
            }
        }
    }

    /**
     * Evaluate the subexpression whose first token is
     * peekTok (usually it is a operator, like 'x+y'), and whose precedence
     * is PeekPrec.  This returns the result in lhs.
     *
     * If valueLive is false, then this value is being evaluated in a context where
     * the result is not used.  As such, avoid diagnostics that relate to
     * evaluation, such as division by zero warnings.
     * @param lhs
     * @param minPrec
     * @param peekTok
     * @param valueLive
     * @param pp
     * @return
     */
    private static boolean evaluateDirectiveSubExpr(
            PPValue lhs, int minPrec,
            Token peekTok, boolean valueLive,
            Preprocessor pp)
    {
        int peekPrec = getPrecedence(peekTok.getKind());
        if (peekPrec == ~0)
        {
            pp.diag(peekTok.getLocation(), err_pp_expr_bad_token_binop)
                .addSourceRange(lhs.getRange()).emit();
            return true;
        }

        while (true)
        {
            // Only the precedence of current operator is not less than previous pred,
            // continue to handle this operator.
            if (peekPrec < minPrec)
                return false;

            TokenKind operator = peekTok.getKind();

            boolean rhsIsLive;
            if (operator == ampamp && lhs.val.eq(0)) // 0 && X
                rhsIsLive = false;
            else if (operator == barbar && lhs.val.eq(1)) // 1 || X
                rhsIsLive = false;
            else if (operator == question && lhs.val.eq(0))  // 1 ? X : Y
                rhsIsLive = false;
            else
                rhsIsLive = true;

            SourceLocation opLoc = peekTok.getLocation();
            pp.lexNonComment(peekTok);

            // Parse the rhs of the operator.
            PPValue rhs = new PPValue(lhs.getBitWidth());
            DefinedTracker dt = new DefinedTracker();
            if (evaluateValue(rhs, peekTok, dt, rhsIsLive, pp))
                return true;

            int thisPrec = peekPrec;
            peekPrec = getPrecedence(peekTok.getKind());

            if (peekPrec == ~0)
            {
                pp.diag(peekTok.getLocation(), err_pp_expr_bad_token_binop).
                        addSourceRange(rhs.getRange()).emit();
                return true;
            }

            int rhsPrec;
            if (operator == question)
                rhsPrec = getPrecedence(comma);
            else
                rhsPrec = thisPrec + 1;

            if (peekPrec >= rhsPrec)
            {
                if (evaluateDirectiveSubExpr(rhs, rhsPrec, peekTok, rhsIsLive, pp))
                    return true;
                peekPrec = getPrecedence(peekTok.getKind());
            }

            assert peekPrec <= thisPrec :"Recursion didn't work!";

            // Usual arithmetic conversions (C99 6.3.1.8p1): result is int if
            // either operand is int.
            APSInt res = new APSInt(lhs.getBitWidth());
            switch (operator)
            {
                case question:
                case lessless:
                case greatergreater:
                case comma:
                case barbar:
                case ampamp:
                    break;
                default:
                    res.setIssigned(lhs.isUnsigned() | rhs.isUnsigned());

                    if (valueLive && res.isUnsigned())
                    {
                        if (!lhs.isUnsigned() && lhs.val.isNegative())
                        {
                            pp.diag(opLoc, warn_pp_convert_lhs_to_positive)
                                    .addTaggedVal(lhs.val.toString(10, true) + "to")
                                    .addTaggedVal(lhs.val.toString(10, false))
                                    .emit();
                        }
                        if (!rhs.isUnsigned() && rhs.val.isNegative())
                        {
                            pp.diag(opLoc, warn_pp_convert_lhs_to_positive)
                                    .addTaggedVal(rhs.val.toString(10, true) + "to")
                                    .addTaggedVal(rhs.val.toString(10, false))
                                    .emit();
                        }
                    }
                    lhs.val.setIsUnsigned(res.isUnsigned());
                    rhs.val.setIsUnsigned(res.isUnsigned());
            }

            boolean overflow = false;
            switch (operator)
            {
                default: assert false : "Unknown operator token!";
                case percent:
                    if (!rhs.val.eq(0))
                        res = lhs.val.rem(rhs.val);
                    else if (valueLive)
                    {
                        pp.diag(opLoc, err_pp_remainder_by_zero)
                                .addSourceRange(lhs.getRange())
                                .addSourceRange(rhs.getRange())
                                .emit();
                        return true;
                    }
                    break;
                case slash:
                    if (!rhs.val.eq(0))
                    {
                        res = lhs.val.div(rhs.val);
                        if (lhs.val.isSigned())
                            overflow = lhs.val.isMinSignedValue();
                    }
                    else if (valueLive)
                    {
                        pp.diag(opLoc, err_pp_division_by_zero)
                                .addSourceRange(lhs.getRange())
                                .addSourceRange(rhs.getRange())
                                .emit();
                        return true;
                    }
                    break;
                case star:
                    res = lhs.val.mul(rhs.val);
                    if (res.isSigned() && !lhs.val.eq(0) && !rhs.val.eq(0))
                        overflow = !(res.div(rhs.val).eq(lhs.val)) || !res.div(lhs.val).eq(rhs.val);
                    break;
                case lessless:
                {
                    // Determine whether overflow is about to happen.
                    int ShAmt = (int)rhs.val.getLimitedValue(~0);
                    if (ShAmt >= lhs.val.getBitWidth())
                    {
                        overflow = true;
                        ShAmt = lhs.val.getBitWidth() - 1;
                    }
                    else if (lhs.isUnsigned())
                        overflow = false;
                    else if (lhs.val.isNonNegative()) // Don't allow sign change.
                        overflow = ShAmt >= lhs.val.countLeadingZeros();
                    else
                        overflow = ShAmt >= lhs.val.countLeadingOnes();

                    res = lhs.val.shl(ShAmt);
                    break;
                }
                case greatergreater: {
                    // Determine whether overflow is about to happen.
                    int ShAmt = (int)(rhs.val.getLimitedValue(~0));
                    if (ShAmt >= lhs.getBitWidth())
                    {
                        overflow = true;
                        ShAmt = lhs.getBitWidth() - 1;
                    }
                    res = lhs.val.shl(ShAmt);
                    break;
                }
                case plus:
                    res = lhs.val.add(rhs.val);
                    if (lhs.isUnsigned())
                        overflow = false;
                    else if (lhs.val.isNonNegative() == rhs.val.isNonNegative() &&
                            res.isNonNegative() != lhs.val.isNonNegative())
                        overflow = true;  // overflow for signed addition.
                    break;
                case sub:
                    res = lhs.val.sub(rhs.val);
                    if (lhs.isUnsigned())
                        overflow = false;
                    else if (lhs.val.isNonNegative() != rhs.val.isNonNegative() &&
                            res.isNonNegative() != lhs.val.isNonNegative())
                        overflow = true;  // overflow for signed subtraction.
                    break;
                case lessequal:
                    res = new APSInt(lhs.val.le(rhs.val)?1:0);
                    res.setIsUnsigned(false);  // C99 6.5.8p6, result is always int (signed)
                    break;
                case less:
                    res = new APSInt(lhs.val.lt(rhs.val)?1:0);
                    res.setIsUnsigned(false);  // C99 6.5.8p6, result is always int (signed)
                    break;
                case greaterequal:
                    res = new APSInt(lhs.val.ge(rhs.val)?1:0);
                    res.setIsUnsigned(false);  // C99 6.5.8p6, result is always int (signed)
                    break;
                case greater:
                    res = new APSInt(lhs.val.gt(rhs.val)? 1: 0);
                    res.setIsUnsigned(false);  // C99 6.5.8p6, result is always int (signed)
                    break;
                case bangequal:
                    res = new APSInt(lhs.val.eq(rhs.val)? 0 : 1);
                    res.setIsUnsigned(false);  // C99 6.5.9p3, result is always int (signed)
                    break;
                case equalequal:
                    res = new APSInt(lhs.val.eq(rhs.val)?1:0);
                    res.setIsUnsigned(false);  // C99 6.5.9p3, result is always int (signed)
                    break;
                case amp:
                    res = lhs.val.and(rhs.val);
                    break;
                case caret:
                    res = lhs.val.xor(rhs.val);
                    break;
                case bar:
                    res = lhs.val.or(rhs.val);
                    break;
                case ampamp:
                    res = new APSInt(!lhs.val.eq(0) && !rhs.val.eq(0) ? 1 : 0);
                    res.setIsUnsigned(false);  // C99 6.5.13p3, result is always int (signed)
                    break;
                case barbar:
                    res = new APSInt(!lhs.val.eq(0) || !rhs.val.eq(0) ? 1 : 0);
                    res.setIsUnsigned(false);  // C99 6.5.14p3, result is always int (signed)
                    break;
                case comma:
                    // Comma is invalid in pp expressions in c89/c++ mode, but is valid in C99
                    // if not being evaluated.
                    if (!pp.getLangOptions().c99 || valueLive)
                        pp.diag(opLoc, ext_pp_comma_expr)
                                .addSourceRange(lhs.getRange())
                                .addSourceRange(rhs.getRange())
                                .emit();
                    res = rhs.val; // lhs = lhs,rhs -> rhs.
                    break;
                case question: {
                    // Parse the : part of the expression.
                    if (peekTok.isNot(colon)) {
                        pp.diag(peekTok.getLocation(), err_expected_colon)
                                .addSourceRange(lhs.getRange())
                                .addSourceRange(rhs.getRange())
                                .emit();
                        pp.diag(opLoc, note_matching).addTaggedVal("?").emit();
                        return true;
                    }
                    // Consume the :.
                    pp.lexNonComment(peekTok);

                    // Evaluate the value after the :.
                    boolean AfterColonLive = valueLive && lhs.val.eq(0);
                    PPValue AfterColonVal = new PPValue(lhs.getBitWidth());
                    DefinedTracker DT;
                    if (evaluateValue(AfterColonVal, peekTok, dt, AfterColonLive, pp))
                        return true;

                    // Parse anything after the : with the same precedence as ?.  We allow
                    // things of equal precedence because ?: is right associative.
                    if (evaluateDirectiveSubExpr(AfterColonVal, thisPrec,
                            peekTok, AfterColonLive, pp))
                        return true;

                    // Now that we have the condition, the lhs and the rhs of the :, evaluate.
                    res = !lhs.val.eq(0) ? rhs.val : AfterColonVal.val;
                    rhs.setEnd(AfterColonVal.getRange().getEnd());

                    // Usual arithmetic conversions (C99 6.3.1.8p1): result is int if
                    // either operand is int.
                    res.setIsUnsigned(rhs.isUnsigned() | AfterColonVal.isUnsigned());

                    // Figure out the precedence of the token after the : part.
                    peekPrec = getPrecedence(peekTok.getKind());
                    break;
                }
                case colon:
                    // Don't allow :'s to float around without being part of ?: exprs.
                    pp.diag(opLoc, err_pp_colon_without_question)
                            .addSourceRange(lhs.getRange())
                            .addSourceRange(rhs.getRange())
                            .emit();
                    return true;

            }

            if (overflow && valueLive)
            {
                pp.diag(opLoc, warn_pp_expr_overflow)
                        .addSourceRange(lhs.getRange())
                        .addSourceRange(rhs.getRange())
                        .emit();
            }
            lhs.val = res;
            lhs.setEnd(rhs.range.getEnd());
            return false;
        }
    }

    private void lexNonComment(Token result)
    {
        do
        {
            lex(result);
        }while (result.is(Comment));
    }

    /**
     * Return the precedence of the specified binary operator
     * token.  This returns:
     *   ~0 - Invalid token.
     *   14 -> 3 - various operators.
     *    0 - 'eom' or ')'
     * @param tokKind
     * @return
     */
    private static int getPrecedence(TokenKind tokKind)
    {
        switch (tokKind)
        {
            default:return ~0;
            case percent:
            case slash:
            case star:
                return 14;
            case plus:
            case sub:
                return 13;
            case lessless:
            case greatergreater:
                return 12;
            case lessequal:
            case less:
            case greaterequal:
            case greater:
                return 11;
            case bangequal:
            case equalequal:
                return 10;
            case amp:
                return 9;
            case caret:
                return 8;
            case bar:
                return 7;
            case ampamp:
                return 6;
            case barbar:
                return 5;
            case question:
                return 4;
            case comma:
                return 3;
            case colon:
                return 2;
            case r_paren:
            case eom:
                return 0;
        }
    }

    /**
     * Evaluate an integer constant expression that
     * may occur after a #if or #elif directive.  If the expression is equivalent
     * to "!defined(X)" return X in IfNDefMacro.
     * @param ifNDefMacro
     * @return
     */
    private boolean evaluateDirectiveExpression(OutParamWrapper<IdentifierInfo> ifNDefMacro)
    {
        // Peek ahead one token.
        Token tok = new Token();
        lex(tok);

        // C99 6.10.1p3 - All expressions are evaluated as intmax_t or uintmax_t.
        int bitWidth = getTargetInfo().getIntMaxWidth();
        PPValue resVal = new PPValue(bitWidth);
        DefinedTracker dt = new DefinedTracker();
        if (evaluateValue(resVal, tok, dt, true, this))
        {
            // Parse error, skip the rest of the macro line.
            if (tok.isNot(eom))
                discardUntilEndOfDirective();
            return false;
        }

        if (tok.is(eom))
        {
            if (dt.state == NotDefMacro)
                ifNDefMacro.set(dt.theMacro);

            return !resVal.val.eq(0);
        }

        // Otherwise, we must have a binary operator (e.g. "#if 1 < 2"), so parse the
        // operator and the stuff after it.
        if (evaluateDirectiveSubExpr(resVal, getPrecedence(TokenKind.question),
                tok, true, this))
        {
            // Parse error, skip the rest of the macro line.
            if (tok.isNot(eom))
                discardUntilEndOfDirective();
            return false;
        }

        if (tok.isNot(eom))
        {
            diag(tok, err_pp_expected_eol).emit();
            discardUntilEndOfDirective();
        }

        return !resVal.val.eq(0);
    }

    /**
     * Implements the #if directive.
     * @param ifToken
     * @param readAnyTokensBeforeDirective
     */
    private void handleIfDirective(Token ifToken, boolean readAnyTokensBeforeDirective)
    {
        ++NumIf;

        // Parse and evaluation the conditional expression.
        IdentifierInfo ifNDefMacro;
        OutParamWrapper<IdentifierInfo> x = new OutParamWrapper<>();
        boolean conditionalTrue = evaluateDirectiveExpression(x);
        ifNDefMacro = x.get();

        if (curLexer.getConditionalStackDepth() == 0)
        {
            if (!readAnyTokensBeforeDirective)
                curLexer.miOpt.enterTopLevelIFNDEF(ifNDefMacro);
            else
                curLexer.miOpt.enterTopLevelConditional();
        }

        if (conditionalTrue)
        {
            curLexer.pushConditionalLevel(ifToken.getLocation(), false, true, false);
        }
        else
        {
            skipExcludedConditionalBlock(ifToken.getLocation(), false, false);
        }
    }

    private void readMacroName(Token macroNameTok, boolean isDefineUndef)
    {
        lexUnexpandedToken(macroNameTok);

        // Missing macro asmName.
        if (macroNameTok.is(eom))
        {
            diag(macroNameTok, err_pp_missing_macro_name).emit();
            return;
        }

        IdentifierInfo ii = macroNameTok.getIdentifierInfo();
        if (ii == null)
        {
            diag(macroNameTok, err_pp_macro_not_identifier).emit();
        }
        else if (isDefineUndef && ii.getPPKeywordID() == PPKeyWordKind.pp_define)
        {
            // Error if defining "defined": C99 6.10.8.4.
            diag(macroNameTok, err_defined_macro_name).emit();
        }
        else if (isDefineUndef && ii.isHasMacroDefinition()
                && getMacroInfo(ii).isBuiltinMacro())
        {
            // Error if defining "__LINE__" and other builtins: C99 6.10.8.4.
            if (isDefineUndef)
                diag(macroNameTok, pp_redef_builtin_macro).emit();
            else
                diag(macroNameTok, pp_undef_builtin_macro).emit();
        }
        else
        {
            // Okay, we got a good identifier node, return it.
        }
    }

    /**
     * We just read a #if or related directive and
     * decided that the subsequent tokens are in the #if'd out portion of the
     * file.  Lex the rest of the file, until we see an #endif.  If
     * FoundNonSkipPortion is true, then we have already emitted code for part of
     * this #if directive, so #else/#elif blocks should never be entered. If ElseOk
     * is true, then #else directives are ok, if not, then we have already seen one
     * so a #else directive is a duplicate.  When this returns, the caller can lex
     * the first valid token.
     * @param ifTokenLoc
     * @param foundNonSkip
     * @param foundElse
     */
    private void skipExcludedConditionalBlock(
            SourceLocation ifTokenLoc,
            boolean foundNonSkip,
            boolean foundElse)
    {
        ++NumSkipped;
        assert curTokenLexer == null && curLexer != null:"Lexing a macro, not file!";

        curLexer.pushConditionalLevel(ifTokenLoc, false, foundNonSkip, foundElse);;


        curLexer.lexingRawMode = true;
        Token tok = new Token();
        while (true)
        {
            if (curLexer != null)
                curLexer.lex(tok);

            if (tok.is(eof))
            {
                while (!curLexer.conditionalStack.isEmpty())
                {
                    diag(curLexer.conditionalStack.peek().ifLoc,
                            err_pp_unterminated_conditional).emit();
                    curLexer.conditionalStack.pop();
                }

                break;
            }

            if (tok.isNot(hash) || !tok.isAtStartOfLine())
                continue;

            curLexer.parsingPreprocessorDirective = true;
            if (curLexer != null)
                curLexer.setCommentRetentionState(false);

            lexUnexpandedToken(tok);

            if (tok.isNot(identifier))
            {
                curLexer.parsingPreprocessorDirective = false;
                if (curLexer != null)
                    curLexer.setCommentRetentionState(keepComments);;
                continue;
            }

            StrData strData = sourceMgr.getCharacterData(tok.getLocation());
            char[] rawCharData = strData.buffer;
            int offset = strData.offset;
            char firstChar = rawCharData[offset];
            if (firstChar >= 'a' && firstChar <= 'z'
                    && firstChar != 'i' && firstChar != 'e')
            {
                curLexer.parsingPreprocessorDirective = false;
                if (curLexer != null)
                    curLexer.setCommentRetentionState(keepComments);;
                continue;
            }

            // Get the identifier asmName without trigraphs or embedded newlines.  Note
            // that we can't use Tok.getIdentifierInfo() because its lookup is disabled
            // when skipping.
            String directive;
            int idLen;
            if (!tok.needsCleaning() && tok.getLength() < 20)
            {
                idLen = tok.getLength();
                char[] temp = new char[idLen];
                System.arraycopy(rawCharData, offset, temp, 0, idLen);
                directive = String.valueOf(temp);
            }
            else
            {
                directive = getSpelling(tok);
                if (directive.length() >= 20)
                {
                    if (curLexer != null)
                    {
                        curLexer.parsingPreprocessorDirective = false;
                        curLexer.setCommentRetentionState(keepComments);
                        continue;
                    }
                }
            }

            if (directive.equals("if")
                    || directive.equals("ifdef")
                    || directive.equals("ifndef"))
            {
                discardUntilEndOfDirective();
                curLexer.pushConditionalLevel(tok.getLocation(), true,
                        false, false);
            }
            else if (directive.equals("endif"))
            {
                checkEndOfDirective("endif", false);
                PPConditionalInfo condInfo;
                condInfo = curLexer.popConditionalLevel();
                assert condInfo != null:"Can't be skipping if not in a condition";

                if (!condInfo.wasSkipping)
                    break;
            }
            else if (directive.equals("else"))
            {
                discardUntilEndOfDirective();
                PPConditionalInfo condInfo = curLexer.peekConditionalLevel();

                if (condInfo.foundElse)
                    diag(tok, pp_err_else_after_else).emit();

                condInfo.foundElse= true;

                if (!condInfo.wasSkipping && !condInfo.foundNonSkip)
                {
                    condInfo.foundNonSkip = true;
                    break;
                }
            }
            else if (directive.equals("elif"))
            {
                PPConditionalInfo condInfo = curLexer.peekConditionalLevel();

                boolean shouldEnter;

                if (condInfo.wasSkipping || condInfo.foundNonSkip)
                {
                    discardUntilEndOfDirective();
                    shouldEnter = false;
                }
                else
                {
                    assert curLexer.lexingRawMode : "We have to be skipping here!";

                    curLexer.lexingRawMode = false;
                    IdentifierInfo ifNdefMacro;
                    OutParamWrapper<IdentifierInfo> x = new OutParamWrapper<>();
                    shouldEnter = evaluateDirectiveExpression(x);
                    ifNdefMacro = x.get();
                    curLexer.lexingRawMode = true;
                }

                if (condInfo.foundElse)
                    diag(tok, pp_err_elif_after_else).emit();

                if (shouldEnter)
                {
                    condInfo.foundNonSkip = true;
                    break;
                }
            }

            if (curLexer != null)
            {
                curLexer.parsingPreprocessorDirective = false;
                curLexer.setCommentRetentionState(keepComments);
            }
        }

        curLexer.lexingRawMode = false;
    }

    /**
     * Implements the #ifdef/#ifndef directive.  isIfndef is
     * true when this is a #ifndef directive.  ReadAnyTokensBeforeDirective is true
     * if any tokens have been returned or pp-directives activated before this
     * #ifndef has been lexed.
     * @param result
     * @param isIfndef
     * @param readAnyTokensBeforeDirective
     */
    private void handleIfdefDirective(Token result, boolean isIfndef,
            boolean readAnyTokensBeforeDirective)
    {
        ++NumIf;
        Token directiveTok = result;

        Token macroNameTok = new Token();
        readMacroName(macroNameTok, false);

        // Error reading macro asmName?  If so, diagnostic already issued.
        if (macroNameTok.is(eom))
        {
            skipExcludedConditionalBlock(directiveTok.getLocation(), false, false);
            return;
        }

        checkEndOfDirective(isIfndef ? "ifndef" : "ifdef", false);

        if (curLexer.getConditionalStackDepth() == 0)
        {
            // If the start of a top-level #ifdef, inform MIOpt.
            if (!readAnyTokensBeforeDirective)
            {
                assert isIfndef :"#ifdef shouldn't reach here";
                curLexer.miOpt.enterTopLevelIFNDEF(macroNameTok.getIdentifierInfo());;
            }
            else
                curLexer.miOpt.enterTopLevelConditional();
        }

        IdentifierInfo mii = macroNameTok.getIdentifierInfo();
        MacroInfo mi = getMacroInfo(mii);

        if (mi != null)
        {
            mi.setIsUsed(true);
        }

        if (mi == null == isIfndef)
        {
            // Whether the directive condition is true?
            curLexer.pushConditionalLevel(directiveTok.getLocation(), false, true, false);
        }
        else
        {
            // otherwise, skip the following tokens when directive condition is not true.
            skipExcludedConditionalBlock(directiveTok.getLocation(), false, false);
        }
    }

    private void handleElifDirective(Token elifToken)
    {
        ++NumElse;

        // #elif directive in a non-skipping conditional... start skipping.
        // We don't care what the condition is, because we will always skip it (since
        // the block immediately before it was included).
        discardUntilEndOfDirective();

        PPConditionalInfo ci;
        if ((ci = curLexer.popConditionalLevel()) == null)
        {
            diag(elifToken, pp_err_elif_without_if).emit();
            return;
        }

        // If this is a top-level #elif, inform the miopt.
        if (curLexer.getConditionalStackDepth() == 0)
            curLexer.miOpt.enterTopLevelConditional();

        // If this is a #elif with a #else before it, report the error.
        if (ci.foundElse) diag(elifToken, pp_err_elif_after_else).emit();

        // Finally, skip the rest of the contents of this block and return the first
        // token after it.
        skipExcludedConditionalBlock(ci.ifLoc, true, ci.foundElse);
    }

    private void handleElseDirective(Token elseToken)
    {
        ++NumElse;

        checkEndOfDirective("else", false);

        PPConditionalInfo ci;
        if ((ci = curLexer.popConditionalLevel()) == null)
        {
            diag(elseToken, pp_err_elif_without_if).emit();
            return;
        }

        if (curLexer.getConditionalStackDepth() == 0)
            curLexer.miOpt.enterTopLevelConditional();

        if (ci.foundElse) diag(elseToken, pp_err_else_after_else).emit();

        skipExcludedConditionalBlock(ci.ifLoc, true, true);
    }

    /**
     * Implements the #endif directive.
     * @param endifToken
     */
    private void handleEndifDirective(Token endifToken)
    {
        ++NumEndif;

        checkEndOfDirective("endif", false);

        PPConditionalInfo ci;
        if ((ci = curLexer.popConditionalLevel()) == null)
        {
            diag(endifToken, err_pp_endif_without_if).emit();
            return;
        }

        if (curLexer.getConditionalStackDepth() == 0)
            curLexer.miOpt.enterTopLevelConditional();

        assert !ci.wasSkipping && !curLexer.lexingRawMode:
                "This code should only be reachable in the non-skipping case!";
    }

    /**
     * Handle cases where the #include asmName is expanded
     * from a macro as multiple tokens, which need to be glued together.  This
     * occurs for code like:
     *    #define FOO <a/b.h>
     *    #include FOO
     * because in this case, "<a/b.h>" is returned as 7 tokens, not one.
     *
     * This code concatenates and consumes tokens up to the '>' token.  It returns
     * false if the > was found, otherwise it returns true if it finds and consumes
     * the EOM marker.
     * @param filenameBuffer
     * @param pp
     * @return
     */
    static boolean concatenateIncludeName(StringBuilder filenameBuffer, Preprocessor pp)
    {
        Token tok = new Token();
        pp.lex(tok);
        while (tok.isNot(eom))
        {
            if (tok.hasLeadingSpace())
                filenameBuffer.append(' ');

            filenameBuffer.append(pp.getSpelling(tok));

            // If we found the '>' marker, return success.
            if (tok.is(greater))
                return false;

            pp.lex(tok);
        }

        pp.diag(tok.getLocation(), err_pp_expects_filename).emit();
        return true;
    }

    private boolean getIncludeFilenameSpelling(SourceLocation loc, StringBuilder buf)
    {
        boolean isAngled = false;
        // make sure the filename is <x> or "x".
        char firstChar = buf.charAt(0), lastChar = buf.charAt(buf.length() - 1);
        if (firstChar == '<')
        {
            if (lastChar != '>')
            {
                diag(loc, err_pp_expects_filename).emit();
                buf.delete(0, buf.length());
                return true;
            }
            isAngled = true;
        }
        else if (firstChar == '"')
        {
            if (lastChar != '"')
            {
                diag(loc, err_pp_expects_filename).emit();
                buf.delete(0, buf.length());
                return true;
            }
        }
        else
        {
            diag(loc, err_pp_expects_filename).emit();
            buf.delete(0, buf.length());
            return true;
        }

        // Diagnose #include "" as invalid.
        if (buf.length() <= 2)
        {
            diag(loc, err_pp_empty_filename).emit();
            buf.delete(0, buf.length());
            return true;
        }

        // SKipt the brackets.
        buf.deleteCharAt(0);
        buf.deleteCharAt(buf.length() - 1);
        return isAngled;
    }

    private Path lookupFile(String filename, boolean isAngled,
            OutParamWrapper<Path> curDir)
    {
        Path curFileEntry = null;
        FileID fid = getCurrentFileLexer().getFileID();
        curFileEntry = sourceMgr.getFileEntryForID(fid);

        if (curFileEntry == null)
        {
            fid = sourceMgr.getMainFileID();
            curFileEntry = sourceMgr.getFileEntryForID(fid);
        }
        curDir.set(curDirLookup);

        Path fe = headerInfo
                .lookupFile(filename, isAngled, null, curDir, curFileEntry);
        if (fe != null)
            return fe;

        // Otherwise, lookup for it from include stack.
        for (int i = includeMacroStack.size() - 1; i >= 0; ++i)
        {
            IncludeStackInfo entry = includeMacroStack.get(i);
            if (isFileLexer(entry))
            {
                curFileEntry = sourceMgr.getFileEntryForID(entry.theLexer.getFileID());
                if (curFileEntry != null)
                {
                    fe = headerInfo.lookupFile(filename, isAngled, null, curDir, curFileEntry);
                    if (fe != null)
                        return fe;
                }
            }
        }
        return null;
    }

    private void handleIncludeDirective(Token includeToken)
    {
        handleIncludeDirective(includeToken, null, false);
    }

    private void handleIncludeDirective(Token includeToken, Path lookupFrom, boolean pragmaOnce)
    {
        Token filenameTok = new Token();
        curLexer.lexIncludeFilename(filenameTok);

        StringBuilder filename = new StringBuilder();
        switch (filenameTok.getKind())
        {
            case eom:
                return;
            case angle_string_literal:
            case string_literal:
                filename.append(getSpelling(filenameTok));
                break;
            case less:
                // This could be a <foo/bar.h> file coming from a macro expansion.  In this
                // case, glue the tokens together into FilenameBuffer and interpret those.
                filename.append('<');
                if (concatenateIncludeName(filename, this))
                    return;     // Found <eom> but no ">" diagnositcs already issured.
                break;
                default:
                    diag(filenameTok, err_pp_expects_filename).emit();
                    discardUntilEndOfDirective();
                    return;
        }
        boolean isAngled = getIncludeFilenameSpelling(filenameTok.getLocation(), filename);

        if (filename.length() <= 0)
        {
            discardUntilEndOfDirective();
            return;
        }

        checkEndOfDirective(includeToken.getIdentifierInfo().getName(), true);;

        // Check that we don't have infinite #include recursion.
        if (includeMacroStack.size() == maxAllowedIncludesStackDepth)
        {
            diag(filenameTok, err_pp_include_too_deep).emit();
            return;
        }
        OutParamWrapper<Path> curDir = new OutParamWrapper<>();
        Path file = lookupFile(filename.toString(), isAngled, curDir);
        if (file == null)
        {
            diag(filenameTok, err_pp_file_not_found).
                    addTaggedVal(filename.toString()).emit();
            return;
        }

        if (!headerInfo.shouldEnterIncludeFile(file, pragmaOnce))
            return;

        CharacteristicKind kind1 = sourceMgr.getFileCharacteristicKind(filenameTok.getLocation());
        CharacteristicKind kind2 = headerInfo.getFileDirFlavor(file);
        CharacteristicKind fileType = kind1.ordinal() > kind2.ordinal()? kind1 : kind2;

        FileID fid = sourceMgr.createFileID(file, filenameTok.getLocation(), fileType, 0, 0);
        if (fid.isInvalid())
        {
            diag(filenameTok, err_pp_file_not_found).
                    addTaggedVal(filename.toString()).emit();
            return;
        }

        enterSourceFile(fid, curDir.get());
    }

    /**
     * The -imacros command line option turns into a
     * pseudo directive in the predefines buffer.  This handles it by sucking all
     * tokens through the preprocessor and discarding them (only keeping the side
     * effects on the preprocessor).
     * @param includeMacroTok
     */
    private void handleIncludeMacrosDirective(Token includeMacroTok)
    {
        SourceLocation loc = includeMacroTok.getLocation();
        if (sourceMgr.getBufferName(loc).equals("<built-in>"))
        {
            diag(includeMacroTok.getLocation(), pp_include_macros_out_of_predefines).emit();
            discardUntilEndOfDirective();
            return;
        }

        handleIncludeDirective(includeMacroTok, null, false);

        Token tmpTok = new Token();
        do
        {
            lex(tmpTok);
            assert tmpTok.isNot(eof) : "Didn't find end of -imacros!";
        }while (tmpTok.isNot(hashhash));
    }

    /**
     * Reads the formal arguments list of the function-like macro definition.
     * Return {@code true} when error occurred, otherwise return {@code false}.
     * @param mi
     * @return
     */
    private boolean readMacroDefinitionArgList(MacroInfo mi)
    {
        ArrayList<IdentifierInfo> argLists = new ArrayList<>();

        Token tok = new Token();
        while (true)
        {
            lexUnexpandedToken(tok);
            switch (tok.getKind())
            {
                case r_paren:
                    if (argLists.isEmpty())
                        return false;
                    diag(tok, err_pp_expected_ident_in_arg_list).emit();
                    return true;
                case ellipsis:
                    // #define X(...) -> C99 feature.
                    if (!langInfo.c99) diag(tok, ext_variadic_macro).emit();

                    lexUnexpandedToken(tok);
                    if (tok.isNot(r_paren))
                    {
                        diag(tok, err_pp_missing_rparen).emit();
                        return true;
                    }

                    // Add the __VA_ARGS__ identifier as an argument.
                    argLists.add(Ident__VA_ARGS__);
                    mi.setIsC99Varargs();
                    IdentifierInfo[] arr = new IdentifierInfo[argLists.size()];
                    argLists.toArray(arr);
                    mi.setArgumentList(arr);
                case eom:
                    diag(tok, err_pp_missing_rparen).emit();
                    return true;
                default:
                    // Handle keywords and identifiers here to accept things like
                    // #define Foo(for) for.
                    IdentifierInfo ii = tok.getIdentifierInfo();
                    if (ii == null)
                    {
                        // #define X(1
                        diag(tok, err_pp_invalid_tok_in_arg_list).emit();
                        return true;
                    }

                    // If this is already used as an argument, it is used multiple times (e.g.
                    // #define X(A,A.
                    if (argLists.contains(ii))
                    {
                        diag(tok, err_pp_duplicate_name_in_arg_list).emit();
                        return true;
                    }

                    // Add this macro argument into argument list.
                    argLists.add(ii);

                    // Lex the token after the identifier.
                    lexUnexpandedToken(tok);

                    switch (tok.getKind())
                    {
                        default:
                            // #define X(A B)
                            diag(tok, err_pp_expected_comma_in_arg_list).emit();
                            return true;
                        case comma:
                            break;  // #define X(A,
                        case r_paren:
                            // #define X(A)
                            arr = new IdentifierInfo[argLists.size()];
                            argLists.toArray(arr);
                            mi.setArgumentList(arr);
                            return false;
                        case ellipsis:
                            // #define X(A...) -> C99 feature.
                            diag(tok, ext_named_variadic_macro).emit();

                            lexUnexpandedToken(tok);
                            if (tok.isNot(r_paren))
                            {
                                diag(tok, err_pp_missing_rparen_in_macro_def).emit();
                                return true;
                            }

                            mi.setGNUVarargs();
                            arr = new IdentifierInfo[argLists.size()];
                            argLists.toArray(arr);
                            mi.setArgumentList(arr);
                            return false;
                    }
            }
        }
    }

    /**
     * Handle the #define pp directive.
     * @param defineTok
     */
    private void handleDefineDirective(Token defineTok)
    {
        ++NumDefined;

        Token macroNameTok = new Token();
        readMacroName(macroNameTok, true);

        if (macroNameTok.is(eom))
            return;

        Token lastTok = macroNameTok.clone();

        if (curLexer != null)
            curLexer.setCommentRetentionState(keepComments);

        MacroInfo mi = new MacroInfo(macroNameTok.getLocation());
        Token tok = new Token();
        lexUnexpandedToken(tok);

        if (tok.is(eom))
        {
            // Done
        }
        else if (tok.hasLeadingSpace())
        {
            tok.clearFlag(LeadingSpace);
        }
        else if (tok.is(l_paren))
        {
            mi.setIsFunctionLike(true);
            if (readMacroDefinitionArgList(mi))
            {
                if (curLexer.parsingPreprocessorDirective)
                    discardUntilEndOfDirective();
                return;
            }

            // If this is a definition of a variadic C99 function-like macro, not using
            // the GNU named varargs extension, enabled __VA_ARGS__.

            // "Poison" __VA_ARGS__, which can only appear in the expansion of a macro.
            // This gets unpoisoned where it is allowed.
            assert Ident__VA_ARGS__.isPoisoned(): "__VA_ARGS__ should be poisoned!";
            if (mi.isC99Varargs())
                Ident__VA_ARGS__.setIsPoisoned(false);

            // Read the first token after the arg list for down below.
            lexUnexpandedToken(tok);
        }
        else if (langInfo.c99)
        {
            // C99 requires whitespace between the macro definition and the body.  Emit
            // a diagnostic for something like "#define X+".
            diag(tok, ext_c99_whitespace_required_after_macro_name).emit();
        }
        else
        {
            // C90 6.8 TC1 says: "In the definition of an object-like macro, if the
            // first character of a replacement list is not a character required by
            // subclause 5.2.1, then there shall be white-space separation between the
            // identifier and the replacement list.".  5.2.1 lists this set:
            //   "A-Za-z0-9!"#%&'()*+,_./:;<=>?[\]^_{|}~" as well as whitespace, which
            // is irrelevant here.
            boolean isInvalid = false;
            if (tok.is(TokenKind.Unknown))
            {
                isInvalid = true;
            }
            if (isInvalid)
                diag(tok, ext_missing_whitespace_after_macro_name).emit();
            else
                diag(tok, warn_missing_whitespace_after_macro_name).emit();
        }

        if (!tok.is(eom))
        {
            lastTok = tok.clone();
        }

        // Read the rest of the macro body.
        if (mi.isObjectLike())
        {
            // Object-like macros are very simple, just read their body.
            while (tok.isNot(eom))
            {
                lastTok = tok.clone();
                mi.addTokenBody(lastTok);
                lexUnexpandedToken(tok);
            }
        }
        else
        {
            // Otherwise, read the body of a function-like macro.  While we are at it,
            // check C99 6.10.3.2p1: ensure that # operators are followed by macro
            // parameters in function-like macro expansions.
            while (tok.isNot(eom))
            {
                lastTok = tok.clone();
                if (tok.isNot(hash))
                {
                    mi.addTokenBody(tok);

                    // Obtains the next token of the macro body.
                    lexUnexpandedToken(tok);
                    continue;
                }

                // get the next tokens of the macro body.
                lexUnexpandedToken(tok);

                // check for a valid macro arg identifier.
                IdentifierInfo argII;
                if ((argII = tok.getIdentifierInfo()) == null || mi.getArgumentNum(argII) < 0)
                {
                    // If this is assembler-with-clex mode, we accept random gibberish after
                    // the '#' because '#' is often a comment character.  However, change
                    // the kind of the token to tok::unknown so that the preprocessor isn't
                    // confused.
                    if (getLangOptions().asmPreprocessor && tok.isNot(eom))
                    {
                        lastTok.setKind(TokenKind.Unknown);
                    }
                    else
                    {
                        diag(tok, err_pp_stringize_not_parameter).emit();

                        //Disable __VA_ARGS__ again.
                        Ident__VA_ARGS__.setIsPoisoned(true);
                    }
                }

                // Things look ok, add the '#' and param asmName tokens to the macro.
                mi.addTokenBody(lastTok);
                mi.addTokenBody(tok);
                lastTok = tok.clone();

                // Get the next token of the macro.
                lexUnexpandedToken(tok);
            }
        }

        //Disable __VA_ARGS__ again.
        Ident__VA_ARGS__.setIsPoisoned(true);

        // Check that there is no paste (##) operator at the begining or end of the
        // replacement list.
        int numTokens = mi.getNumTokens();
        if (numTokens != 0)
        {
            if (mi.getReplacementToken(0).is(hashhash))
            {
                diag(mi.getReplacementToken(0), err_paste_at_start).emit();
                return;
            }
            if (mi.getReplacementToken(numTokens - 1).is(hashhash))
            {
                diag(mi.getReplacementToken(numTokens - 1), err_paste_at_end).emit();
                return;
            }
        }

        // If this is the primary source file, remember that this macro hasn't been
        // used yet.
        if (isInPrimaryFile())
            mi.setIsUsed(false);

        mi.setDefinitionEndLoc(lastTok.getLocation());

        MacroInfo otherMI = getMacroInfo(macroNameTok.getIdentifierInfo());
        if (otherMI != null)
        {
            if (!getDiagnostics().getSuppressSystemWarnings()
                    || !sourceMgr.isInSystemHeader(defineTok.getLocation()))
            {
                if (!otherMI.isUsed())
                {
                    diag(otherMI.getDefinitionLoc(), pp_macro_not_used).emit();
                }

                if (!mi.isIdenticalTo(otherMI, this))
                {
                    diag(mi.getDefinitionLoc(), ext_pp_macro_redef)
                            .addTaggedVal(macroNameTok.getIdentifierInfo())
                            .emit();
                    diag(otherMI.getDefinitionLoc(), note_previous_definition)
                            .emit();
                }
            }
        }

        setMacroInfo(macroNameTok.getIdentifierInfo(), mi);
        // If the callbacks wnat to know, tell them about the macro definition.
        if (callbacks != null)
            callbacks.macroDefined(macroNameTok.getIdentifierInfo(), mi);
    }

    /**
     * Implements {@code #undef} directive.
     * @param undefToken
     */
    private void handleUndefDirective(Token undefToken)
    {
        ++NumUndefined;

        Token macroNameToken = new Token();
        readMacroName(macroNameToken, false);

        // error reading macro asmName?
        if (macroNameToken.is(eom))
            return;
        // Check to see if this is the last token on the #undef line.
        checkEndOfDirective("undef", false);

        IdentifierInfo ii = macroNameToken.getIdentifierInfo();
        MacroInfo mi = getMacroInfo(ii);
        if (mi == null)
            return;

        if (!mi.isUsed())
            diag(mi.getDefinitionLoc(), pp_macro_not_used).emit();

        if (callbacks != null)
            callbacks.macroUndefined(ii, mi);
        setMacroInfo(ii, mi);
    }

    /**
     * Handle #line directive: C99 6.10.4.  The two
     * acceptable forms are:
     *   # line digit-sequence
     *   # line digit-sequence "s-char-sequence"
     * @param lineTok
     */
    private void handleLineDirective(Token lineTok)
    {
        // Read the line # and string argument.  Per C99 6.10.4p5, these tokens are
        // expanded.
        Token digitTok = new Token();
        lex(digitTok);

        OutParamWrapper<Integer> x = new OutParamWrapper<>(0);
        if (getLineValue(digitTok, x, err_pp_line_requires_integer, this))
            return;
        int lineNo = x.get();

        // Enforce C99 6.10.4p3: "The digit sequence shall not specify ... a
        // number greater than 2147483647".  C90 requires that the line # be <= 32767.
        int lineLimit = langInfo.c99 ? 2147483647: 32767;
        if (lineNo >= lineLimit)
            diag(digitTok, ext_pp_line_too_big).addTaggedVal(lineLimit).emit();

        int filenameID = -1;
        Token strTok = new Token();
        lex(strTok);

        if (strTok.is(eom));  // OK
        else if (strTok.isNot(string_literal))
        {
            diag(strTok, err_pp_line_invalid_filename).emit();
            discardUntilEndOfDirective();
            return;
        }
        else
        {
            // Parse and validate the string, converting it into a unique ID.
            StringLiteralParser literal = new StringLiteralParser(new Token[]{strTok}, this);
            assert !literal.anyWide :"Didn't allow wide string in";
            if (literal.hadError)
            {
                discardUntilEndOfDirective();
                return;
            }

            filenameID = sourceMgr.getLineTableFilenameID(literal.getString());
            checkEndOfDirective("line", true);
        }

        sourceMgr.addLineNote(digitTok.getLocation(), lineNo, filenameID);

        if (callbacks != null)
            callbacks.fileChanged(digitTok.getLocation(), RenameFile, C_User);
    }

    /**
     * Handle a #warning or #error directive.
     * @param pragmaToken
     * @param isWarning
     */
    private void handleUserDiagnosticDirective(Token pragmaToken, boolean isWarning)
    {
        String message = curLexer.readToEndOfLine();
        if (isWarning)
            diag(pragmaToken, pp_hash_warning).addTaggedVal(message).emit();
        else
            diag(pragmaToken, err_pp_hash_error).addTaggedVal(message).emit();
    }

    public void handleDirective(Token result)
    {
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
        Token savedHash = result.clone();

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
            diag(result, ext_embedded_directive).emit();

        boolean loop = false;
        do
        {
            switch (result.getKind())
            {
                case eom:
                    return;   // null directive.
                case Comment:
                    // Handle stuff like "# /*foo*/ define X" in -E -C mode.
                    lexUnexpandedToken(result);
                    loop = true;
                    break;

                case numeric_constant:  // # 7  GNU line marker directive.
                    if (getLangOptions().asmPreprocessor)
                        break;  // # 4 is not a preprocessor directive in .S files.
                    handleDigitDirective(result);
                    return;
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
                            handleIfDirective(result,
                                    ReadAnyTokensBeforeDirective);
                            return;
                        case pp_ifdef:
                            handleIfdefDirective(result, false, true/*not valid for miopt*/);
                            return;
                        case pp_ifndef:
                            handleIfdefDirective(result, true,
                                    ReadAnyTokensBeforeDirective);
                            return;
                        case pp_elif:
                            handleElifDirective(result);
                            return;
                        case pp_else:
                            handleElseDirective(result);
                            return;
                        case pp_endif:
                            handleEndifDirective(result);
                            return;

                        // C99 6.10.2 - Source File Inclusion.
                        case pp_include:
                            handleIncludeDirective(result);       // Handle #include.
                            return;
                        case pp___include_macros:
                            handleIncludeMacrosDirective(result); // Handle -imacros.
                            return;

                        // C99 6.10.3 - Macro Replacement.
                        case pp_define:
                            handleDefineDirective(result);
                            return;
                        case pp_undef:
                            handleUndefDirective(result);
                            return;

                        // C99 6.10.4 - Line Control.
                        case pp_line:
                            handleLineDirective(result);
                            return;

                        // C99 6.10.5 - Error Directive.
                        case pp_error:
                            handleUserDiagnosticDirective(result, false);
                            return;

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
        diag(result, err_pp_invalid_directive).emit();

        // Read the rest of the pp line.
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
                if (tok.is(eof) || tok.is(eom))
                {
                    // "#if f(<eof>" & "#if f(\n"
                    diag(macroName, err_unterm_macro_invoc).emit();
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

                diag(argStartLoc, err_too_many_args_in_macro_invoc).emit();
                return Pair.get(null, null);
            }

            // Empty arguments are standard in C99 and supported as an extension in
            // other modes.
            if(argTokens.size() == argTokenStart && !langInfo.c99)
            {
                diag(tok, ext_empty_fnmacro_arg).emit();
            }

            Token eofToken = new Token();
            eofToken.startToken();
            eofToken.setKind(eof);
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
                diag(tok, ext_missing_varargs_arg).emit();

                isVarargsElided = true;
            }
            else
            {
                diag(tok, err_too_few_args_in_macro_invoc).emit();
                return Pair.get(null, null);
            }

            SourceLocation endLoc = tok.getLocation();
            tok.startToken();
            tok.setKind(eof);
            tok.setLocation(endLoc);
            tok.setLength(0);
            argTokens.add(tok);

            // If we expect two arguments, add both as empty.
            if (numActuals == 0 && minArgsExpected == 2)
                argTokens.add(tok);
        }
        else if (numActuals > minArgsExpected &&!mi.isVariadic())
        {
            // Emit the diagnostic at the macro asmName in case there is a missing ).
            // Emitting it at the , could be far away from the macro asmName.
            diag(macroName, err_too_many_args_in_macro_invoc).emit();
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
            diag(pragmaLoc, err_pragma_comment_malformed).emit();
            return;
        }

        // Read the '"..."'.
        lex(tok);
        if (tok.isNot(string_literal))
        {
            diag(pragmaLoc, err_pragma_comment_malformed).emit();
            return;
        }

        // Remember the string.
        char[] strVal = getSpelling(tok).toCharArray();

        // Read the ')'.
        lex(tok);
        if (tok.isNot(r_paren))
        {
            diag(pragmaLoc, err_pragma_comment_malformed).emit();
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
    public SourceLocation advanceToTokenCharacter(SourceLocation tokStart,
            int charNo)
    {
        StrData charData = sourceMgr.getCharacterData(tokStart);
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
            // Skip down through instantiation points until we find a file identLoc for the
            // end of the instantiation history.
            Loc = sourceMgr.getInstantiationRange(Loc).getEnd();
            PresumedLoc PLoc = sourceMgr.getPresumedLoc(Loc);

            // __LINE__ expands to a simple numeric value.
            TmpBuffer = String.format("%d", PLoc.getLine());
            tok.setKind(numeric_constant);
            createString(TmpBuffer, tok, tok.getLocation());
        } else if (ii == Ident__FILE__ || ii == Ident__BASE_FILE__) 
        {
            // C99 6.10.8: "__FILE__: The presumed asmName of the current source file (a
            // character string literal)". This can be affected by #line.
            PresumedLoc PLoc = sourceMgr.getPresumedLoc(tok.getLocation());

            // __BASE_FILE__ is a GNU extension that returns the top of the presumed
            // #include stack instead of the current file.
            if (ii == Ident__BASE_FILE__) 
            {
                diag(tok, ext_pp_base_file).emit();
                SourceLocation NextLoc = PLoc.getIncludeLoc();
                while (NextLoc.isValid()) {
                    PLoc = sourceMgr.getPresumedLoc(NextLoc);
                    NextLoc = PLoc.getIncludeLoc();
                }
            }

            // Escape this filename.  Turn '\' -> '\\' '"' -> '\"'
            String FN = PLoc.getFilename();
            FN = '"' + Lexer.stringify(FN) + '"';
            tok.setKind(string_literal);
            createString(FN, tok, tok.getLocation());
        }
        else if (ii.equals(Ident__DATE__))
        {
            if (!DATELoc.isValid())
                computeDATE_TIME();
            
            tok.setKind(string_literal);
            tok.setLength("\"Mmm dd yyyy\"".length());
            tok.setLocation(sourceMgr.createInstantiationLoc(DATELoc, tok.getLocation(),
                    tok.getLocation(),
                    tok.getLength(), 0, 0));
        }
        else if (ii.equals(Ident__TIME__))
        {
            if (!TIMELoc.isValid())
                computeDATE_TIME();
            tok.setKind(string_literal);
            tok.setLength("\"hh:mm:ss\"".length());
            tok.setLocation(sourceMgr.createInstantiationLoc(TIMELoc, tok.getLocation(),
                    tok.getLocation(),
                    tok.getLength(), 0, 0));
        } else if (ii == Ident__INCLUDE_LEVEL__) 
        {
            diag(tok, ext_pp_include_level).emit();

            // Compute the presumed include depth of this token.  This can be affected
            // by GNU line markers.
            int Depth = 0;

            PresumedLoc PLoc = sourceMgr.getPresumedLoc(tok.getLocation());
            PLoc = sourceMgr.getPresumedLoc(PLoc.getIncludeLoc());
            for (; PLoc.isValid(); ++Depth)
                PLoc = sourceMgr.getPresumedLoc(PLoc.getIncludeLoc());

            // __INCLUDE_LEVEL__ expands to a simple numeric value.
            TmpBuffer = String.format("%d", Depth);
            tok.setKind(numeric_constant);
            createString(TmpBuffer, tok, tok.getLocation());
        }
        else if (ii.equals(Ident__TIMESTAMP__))
        {
            // MSVC, ICC, GCC, VisualAge C++ extension.  The generated string should be
            // of the form "Ddd Mmm dd hh::mm::ss yyyy", which is returned by asctime.
            diag(tok, ext_pp_timestamp).emit();

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
            tok.setKind(string_literal);
            createString(buf.toString(), tok, tok.getLocation());
        }
        else if (ii == Ident__COUNTER__)
        {
            diag(tok, ext_pp_counter).emit();

            // __COUNTER__ expands to a simple numeric value.
            TmpBuffer = String.format("%d", counterValue++);
            tok.setKind(numeric_constant);
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
     * * expanded as a macro, handle it and return the next token as 'identifier'.
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
        // for each macro argument, the list of tokens that were provided to the
        // invocation.
        MacroArgs args = null;

        // Remember where the end of the instantiation occurred.  For an object-like
        // macro, this is the ident.  For a function-like macro, this is the ')'.
        SourceLocation instantiationEnd = ident.getLocation();

        // If this is a function-like macro, read the arguments.
        if (mi.isFunctionLike())
        {
            // C99 6.10.3p10: If the preprocessing token immediately after the the macro
            // asmName isn't a '(', this macro should not be expanded.
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
            ident.setFlag(StartOfLine, isAtStartOfLine);
            ident.setFlag(LeadingSpace, hasLeadingSpace);

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

        // Star expanding the macro.
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

        // If there are any stacked lexers, we're in a #include.
        assert isFileLexer(includeMacroStack.get(0))
                :"Top level include stack isn't our primary lexer?";
        for (int i = 1, e = includeMacroStack.size(); i != e; i++)
            if (isFileLexer(includeMacroStack.get(i)))
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
            diag(onceTok, pp_pragma_once_in_main_file).emit();
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

        if (tmp.isNot(eom))
        {
            // Add a fixit in GNU/C99 mode.  Don't offer a fixit for strict-C89,
            // because it is more trouble than it is worth to insert /**/ and check that
            // there is no /**/ in the range also.
            FixItHint hint = null;
            if (langInfo.gnuMode || langInfo.c99)
                hint = FixItHint.createInsertion(tmp.getLocation(), "//");
            diag(tmp, ext_pp_extra_tokens_at_eol).addTaggedVal(dirType).addFixItHint(hint).emit();
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
        }while (tmp.isNot(eom));
    }

    /**
     * This is just like Lex, but this disables macro
     * expansion of identifier tokens.
     * @param result
     */
    void lexUnexpandedToken(Token result)
    {
        boolean oldVal = disableMacroExpansion;
        disableMacroExpansion = true;

        lex(result);

        disableMacroExpansion = oldVal;
    }

    /**
     * This peeks ahead N tokens and returns that token without
     * consuming any tokens.  lookAhead(0) returns the next token that would be
     * returned by {@link #lex(Token)}, lookAhead(1) returns the token after
     * it, etc.  This returns normal tokens after phase 5.  As such, it is
     * equivalent to using 'lex', not 'lexUnexpandedToken'.
     * @param n
     * @return
     */
    public Token lookAhead(int n)
    {
        if (cachedLexPos + n < cachedTokens.size())
            return cachedTokens.get(cachedLexPos + n);
        else
            return peekAhead(n + 1);
    }

    public Token peekAhead(int n)
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

    /**
     * Tok is a numeric constant with length 1, return the character.
     * @param token
     * @return
     */
    public char getSpellingOfSingleCharacterNumericConstant(Token token)
    {
        assert token != null && token.is(numeric_constant)
                && token.getLength() == 1:"Called on unsupported token";
        assert !token.needsCleaning():"Token can't need cleaning with lenght 1";

        // If the token is carrying a literal data, just return its first character.
        StrData data = token.getLiteralData();
        if (data != null)
            return data.buffer[data.offset];
        // Otherwise, performing the slower path.
        data = sourceMgr.getCharacterData(token.getLocation());
        return data.buffer[data.offset];
    }

    /**
     * Print the token to stderr, used for debugging.
     * @param tok
     * @param dumpFlags
     */
    public void dumpToken(Token tok, boolean dumpFlags)
    {
        err.printf("%s '%s'", tok.getName(), getSpelling(tok));

        if (!dumpFlags)
            return;
        err.print('\t');
        if (tok.isAtStartOfLine())
            err.print(" [StartOfLine]");
        if (tok.hasLeadingSpace())
            err.print(" [LeadingSpace]");
        if (tok.isExpandingDisabled())
            err.print(" [ExpandDisabled]");
        if (tok.needsCleaning())
        {
            int offset = tok.getLiteralData().offset;
            String str = String.valueOf(Arrays.copyOfRange(tok.getLiteralData().buffer,
                    offset, offset + tok.getLength()));
            err.printf(" [UnClean='%s']", str);
        }

        err.print("\tLoc=<");
        dumpLocation(tok.getLocation());
        err.print(">");
    }

    public void dumpLocation(SourceLocation loc)
    {
        loc.dump(sourceMgr);
    }

    public void dumpMacro(MacroInfo mi)
    {
        err.print("MACRO: ");
        for (int i = 0, e = mi.getNumTokens(); i!= e; i++)
        {
            dumpToken(mi.getReplacementToken(i), false);
            err.print(" ");
        }
        err.println();
    }
}
