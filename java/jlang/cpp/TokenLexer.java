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

import jlang.basic.FileID;
import jlang.basic.SourceLocation;
import jlang.basic.SourceManager;

import java.util.ArrayList;
import java.util.List;

import static jlang.cpp.Token.TokenFlags.LeadingSpace;
import static jlang.cpp.Token.TokenFlags.StartOfLine;
import static jlang.cpp.TokenKind.*;
import static jlang.diag.DiagnosticLexKindsTag.*;

/**
 * This implements a lexer that returns token from a macro body
 * or token stream instead of lexing from a character buffer.  
 * This is used for
 * macro expansion and _Pragma handling
 * @author Xlous.zeng
 * @version 0.1
 */
public class TokenLexer
{
    private MacroInfo macro;

    private MacroArgs actualArgs;

    private Preprocessor pp;

    private Token[] tokens;
    /**
     * This is the next token that lex will return;
     */
    private int curToken;

    private SourceLocation instantiationLocStart;
    private SourceLocation instantiationLocEnd;

    private boolean atStartOfLine;
    private boolean hasLeadingSpace;

    private boolean ownsTokens;

    private boolean disableMacroExpansion;

    public TokenLexer(Token tok,
            SourceLocation ilEnd,
            MacroArgs actualArgs,
            Preprocessor pp)
    {
        this.pp = pp;
        ownsTokens = false;
        init(tok, ilEnd, actualArgs);
    }

    public TokenLexer(Token[] tokens, boolean disableMacroExpansion,
            boolean ownsTokens, Preprocessor pp)
    {
        this.pp = pp;
        this.ownsTokens = false;
        init(tokens, disableMacroExpansion, ownsTokens);
    }

    private void init(Token tok, SourceLocation ilEnd, MacroArgs actualArgs)
    {
        macro = pp.getMacroInfo(tok.getIdentifierInfo());
        this.actualArgs = actualArgs;
        curToken = 0;

        instantiationLocStart = tok.getLocation();
        instantiationLocEnd = ilEnd;
        atStartOfLine = tok.isAtStartOfLine();
        hasLeadingSpace = tok.hasLeadingSpace();
        tokens = new Token[macro.getNumTokens()];
        macro.getReplacementTokens().toArray(tokens);
        ownsTokens = false;
        disableMacroExpansion = false;

        // If this is a function-like macro, expand the arguments and change
        // Tokens to point to the expanded tokens.
        if (macro.isFunctionLike() && macro.getNumArgs() != 0)
            expandFunctionArguments();

        // Mark the macro as currently disabled, so that it is not recursively
        // expanded.  The macro must be disabled only after argument pre-expansion of
        // function-like macro arguments occurs.
        macro.disableMacro();
    }

    private void init(Token[] tokens, boolean disableMacroExpansion,
            boolean ownsTokens)
    {
        macro = null;
        actualArgs = null;
        this.tokens = tokens;
        this.ownsTokens = ownsTokens;
        this.disableMacroExpansion = disableMacroExpansion;
        curToken = 0;
        instantiationLocStart = instantiationLocEnd = new SourceLocation();
        atStartOfLine = false;
        hasLeadingSpace = false;

        // Set HasLeadingSpace/AtStartOfLine so that the first token will be
        // returned unmodified.
        if (tokens.length != 0)
        {
            atStartOfLine = tokens[0].isAtStartOfLine();
            hasLeadingSpace = tokens[0].hasLeadingSpace();
        }
    }

    /**
     * Lex and return a token from this macro stream.
     * @param result
     */
    public void lex(Token result)
    {
        if (isAtEnd())
        {
            if (macro != null)
                macro.enableMacro();;

            Preprocessor ppCache = pp;
            if (pp.handleEndOfTokenLexer(result))
                return;

            ppCache.lex(result);
            return;
        }

        boolean isFirstToken = curToken == 0;
        result = tokens[curToken--];

        boolean tokenisFromPaste = false;

        // If this token is followed by a token paste (##) operator, paste the tokens!
        if (!isAtEnd() && tokens[curToken].is(hashhash))
        {
            if (pasteTokens(result))
            {
                return;
            }
            else
            {
                tokenisFromPaste = true;
            }
        }

        if (instantiationLocStart.isValid())
        {
            SourceManager sgr = pp.getSourceManager();
            result.setLocation(sgr.createInstantiationLoc(result.getLocation(),
                    instantiationLocStart,
                    instantiationLocEnd,
                    result.getLength(), 0, 0));
        }

        if (isFirstToken)
        {
            result.setFlagValue(StartOfLine, atStartOfLine);
            result.setFlagValue(LeadingSpace, hasLeadingSpace);
        }

        // Handle recursive expansion!
        if (result.getIdentifierInfo() != null)
        {
            IdentifierInfo ii = result.getIdentifierInfo();
            result.setKind(ii.getTokenID());

            // If this identifier was poisoned and from a paste, emit an error.  This
            // won't be handled by Preprocessor::HandleIdentifier because this is coming
            // from a macro expansion.
            if (ii.isPoisoned() && tokenisFromPaste)
            {
                // We warn about __VA_ARGS__ with poisoning.
                if (ii.isStr("__VA__ARGS__"))
                    pp.diag(result, ext_pp_bad_vaargs_use).emit();
                else
                    pp.diag(result, err_pp_used_poisoned_id).emit();
            }

            if (!disableMacroExpansion && ii.isNeedsHandleIdentifier())
                pp.handleIdentifier(result);
        }

        // Otherwise, return a normal token.
    }

    private boolean isAtEnd()
    {
        return curToken == tokens.length;
    }

    /**
     * Tok is the LHS of a ## operator, and CurToken is the ##
     * operator.  Read the ## and RHS, and paste the LHS/RHS together.  If there
     * are more ## after it, chomp them iteratively.  Return the result as Tok.
     * If this returns true, the caller should immediately return the token.
     * @param tok
     * @return
     */
    private boolean pasteTokens(Token tok)
    {
        StringBuffer buffer = new StringBuffer();
        int resultTokenStrDataPos;
        char[] scratchBuf = null;
        do
        {
            SourceLocation pasteOpLoc = tokens[curToken].getLocation();
            ++curToken;

            assert !isAtEnd() : "No token on the RHS of a paste operator!";

            // Get the RHS token.
            Token rhs = tokens[curToken];

            String tmp = pp.getSpelling(tok);
            int lhsLen = tmp.length();
            buffer.append(tmp);
            tmp = pp.getSpelling(rhs);
            int rhsLen = tmp.length();
            buffer.append(tmp);

            Token resultTokTmp = new Token();
            resultTokTmp.startToken();

            resultTokTmp.setKind(string_literal);
            pp.createString(buffer.toString(), resultTokTmp, new SourceLocation());

            SourceLocation resultTokLoc = resultTokTmp.getLocation();
            resultTokenStrDataPos = resultTokTmp.getLiteralData().offset;

            Token result = new Token();

            if (tok.is(Identifier) && rhs.is(Identifier))
            {
                pp.incrementPasteCounter(true);
                result.startToken();
                result.setKind(Identifier);
                result.setLocation(resultTokLoc);
                result.setLength(lhsLen + rhsLen);
            }
            else
            {
                pp.incrementPasteCounter(true);

                assert resultTokLoc.isFileID() : "Should be a raw location into scratch buffer";
                SourceManager sgr = pp.getSourceManager();
                FileID locFileID = sgr.getFileID(resultTokLoc);

                scratchBuf = sgr.getBufferData(locFileID);

                // fixme 求resultTokenStrData在scratchBuf中的起始位置

                Lexer l = new Lexer(sgr.getLocForStartOfFile(locFileID),
                        pp.getLangOptions(), scratchBuf, resultTokenStrDataPos,
                        resultTokenStrDataPos + rhsLen + lhsLen);

                boolean isInvalid = !l.lexFromRawLexer(result);

                isInvalid |= result.is(eof);

                if (isInvalid)
                {
                    if (!pp.getLangOptions().asmPreprocessor)
                    {
                        sgr = pp.getSourceManager();
                        SourceLocation loc = sgr.createInstantiationLoc(pasteOpLoc,
                                instantiationLocStart, instantiationLocEnd, 2, 0, 0);
                        pp.diag(loc, err_pp_bad_paste).addTaggedVal(buffer.toString()).emit();
                    }

                    --curToken;
                }

                // Turn ## into 'unknown' to avoid # ## # from looking like a paste
                // operator.
                if (result.is(hashhash))
                    result.setKind(Unknown);
            }

            result.setFlagValue(StartOfLine, tok.isAtStartOfLine());
            result.setFlagValue(LeadingSpace, tok.hasLeadingSpace());

            ++curToken;
            tok = result;
        } while (!isAtEnd() && tokens[curToken].is(hashhash));

        if (tok.is(Identifier))
        {
            IdentifierInfo ii = pp.lookupIdentifierInfo(tok, scratchBuf, resultTokenStrDataPos);
            tok.setIdentifierInfo(ii);
        }
        return false;
    }

    /**
     * If the next token lexed will pop this macro off the
     /// expansion stack, return 2.  If the next unexpanded token is a '(', return
     /// 1, otherwise return 0.
     * @return
     */
    public int isNextTokenLParen()
    {
        if (isAtEnd())
            return 2;
        return tokens[curToken].is(l_paren) ? 1 : 0;
    }

    /**
     * Expand the arguments of a function-like macro so that we can quickly
     * return preexpanded tokens from Tokens.
     */
    private void expandFunctionArguments()
    {
        ArrayList<Token> resultToks = new ArrayList<>();

        boolean madeChange = false;

        boolean nextTokGetsSpace = false;

        for (int i = 0; i < tokens.length; i++)
        {
            // If we found the stringify operator, get the argument stringified.  The
            // preprocessor already verified that the following token is a macro name
            // when the #define was parsed.
            Token curTok = tokens[i];
            if (curTok.is(TokenKind.hash))
            {
                int argNo = macro.getArgumentNum(tokens[i + 1].getIdentifierInfo());
                assert argNo >= 0 : "Token following # is not an argument?";

                Token res = actualArgs.getStringifiedArgument(argNo, pp);

                // The stringified string leading space flag gets set to match
                // the # operator.
                if (curTok.hasLeadingSpace() || nextTokGetsSpace)
                    res.setFlag(LeadingSpace);

                resultToks.add(res);
                madeChange = true;
                ++i;        // Skip arg name.
                nextTokGetsSpace = false;
                continue;
            }

            // Otherwise, if this is not an argument token, just add the token to the
            // output buffer.
            IdentifierInfo ii = curTok.getIdentifierInfo();
            int argNo = ii != null ? macro.getArgumentNum(ii) : -1;
            if (argNo < 0)
            {
                // this is not an argument, just add it.
                resultToks.add(curTok);

                if (nextTokGetsSpace)
                {
                    resultToks.get(resultToks.size() - 1).setFlag(LeadingSpace);
                    ;
                    nextTokGetsSpace = false;
                }
                continue;
            }

            // An argument is expanded somehow, the result is different than the
            // input.
            madeChange = true;

            // Otherwise, this is a use of the argument.  Find out if there is a paste
            // (##) operator before or after the argument.
            boolean pasteBefore = !resultToks.isEmpty() && resultToks
                    .get(resultToks.size() - 1).is(hashhash);
            boolean pasteAfter = i + 1 != tokens.length && tokens[i + 1].is(hashhash);

            // If it is not the LHS/RHS of a ## operator, we must pre-expand the
            // argument and substitute the expanded tokens into the result.  This is
            // C99 6.10.3.1p1.
            if (!pasteAfter && !pasteBefore)
            {
                Token[] resultArgToks;
                Token[] argTok = actualArgs.getUnexpArgument(argNo);
                if (actualArgs.argNeedsPreexpansion(argTok, pp))
                {
                    List<Token> list = actualArgs.getPreExpArgument(argNo, pp);
                    resultArgToks = new Token[list.size()];
                    list.toArray(resultArgToks);
                }
                else
                    resultArgToks = argTok;

                if (resultArgToks[0].isNot(eof))
                {
                    int firstResult = resultToks.size();

                    int numToks = MacroArgs.getArgLength(resultArgToks);
                    for (int j = 0; j < numToks; j++)
                        resultToks.add(resultArgToks[i]);

                    resultToks.get(firstResult).setFlagValue(LeadingSpace,
                            curTok.hasLeadingSpace() || nextTokGetsSpace);
                    nextTokGetsSpace = false;
                }
                else
                {
                    nextTokGetsSpace = curTok.hasLeadingSpace();
                }
                continue;
            }

            // Okay, we have a token that is either the LHS or RHS of a paste (##)
            // argument.  It gets substituted as its non-pre-expanded tokens.
            Token[] argToks = actualArgs.getUnexpArgument(argNo);
            int numToks = MacroArgs.getArgLength(argToks);
            if (numToks != 0)
            {
                // If this is the GNU ", ## __VA_ARG__" extension, and we just learned
                // that __VA_ARG__ expands to multiple tokens, avoid a pasting error when
                // the expander trys to paste ',' with the first token of the __VA_ARG__
                // expansion.
                if (pasteBefore && resultToks.size() >= 2 && resultToks
                        .get(resultToks.size() - 2).is(comma)
                        && argNo == macro.getNumArgs() - 1 && macro.isVariadic())
                {
                    pp.diag(resultToks.get(resultToks.size() - 1).getLocation(),
                            ext_paste_comma).emit();
                    resultToks.remove(resultToks.size() - 1);
                }

                for (int j = 0; j < numToks; j++)
                    resultToks.add(argToks[j]);

                // If this token (the macro argument) was supposed to get leading
                // whitespace, transfer this information onto the first token of the
                // expansion.
                //
                // Do not do this if the paste operator occurs before the macro argument,
                // as in "A ## MACROARG".  In valid code, the first token will get
                // smooshed onto the preceding one anyway (forming AMACROARG).  In
                // assembler-with-cpp mode, invalid pastes are allowed through: in this
                // case, we do not want the extra whitespace to be added.  For example,
                // we want ". ## foo" -> ".foo" not ". foo".
                if ((curTok.hasLeadingSpace() || nextTokGetsSpace) && !pasteBefore)
                {
                    resultToks.get(resultToks.size() - numToks).setFlag(LeadingSpace);
                }

                nextTokGetsSpace = false;
                continue;
            }

            // If an empty argument is on the LHS or RHS of a paste, the standard (C99
            // 6.10.3.3p2,3) calls for a bunch of placemarker stuff to occur.  We
            // implement this by eating ## operators when a LHS or RHS expands to
            // empty.
            nextTokGetsSpace |= curTok.hasLeadingSpace();
            if (pasteAfter)
            {
                // Discard the argument token and skip (don't copy to the expansion
                // buffer) the paste operator after it.
                nextTokGetsSpace |= tokens[i + 1].hasLeadingSpace();
                ++i;
                continue;
            }

            // If this is on the RHS of a paste operator, we've already copied the
            // paste operator to the ResultToks list.  Remove it.
            assert pasteBefore && resultToks.get(resultToks.size() - 1).is(hashhash);
            nextTokGetsSpace |= resultToks.get(resultToks.size() - 1).hasLeadingSpace();
            resultToks.remove(resultToks.size() - 1);

            // If this is the __VA_ARGS__ token, and if the argument wasn't provided,
            // and if the macro had at least one real argument, and if the token before
            // the ## was a comma, remove the comma.
            if (argNo == macro.getNumArgs() - 1 // is __VA_ARGS__
                    && actualArgs.isVarargsElidedUse()  // Argument elided.
                    && !resultToks.isEmpty() && resultToks
                    .get(resultToks.size() - 1).is(comma))
            {
                nextTokGetsSpace = false;
                pp.diag(resultToks.get(resultToks.size() - 1), ext_paste_comma).emit();
                resultToks.remove(resultToks.size() - 1);
            }
        }

        if (madeChange)
        {
            assert !ownsTokens : "This would leak if we already own the token list";

            int numToks = resultToks.size();

            Token[] res = new Token[numToks];
            if (numToks != 0)
                resultToks.toArray(res);
            tokens = res;

            // The preprocessor bump pointer owns these tokens, not us.
            ownsTokens = false;
        }
    }
}
