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

import jlang.basic.*;
import jlang.cparser.Token;
import jlang.diag.Diagnostic;
import jlang.diag.FixItHint;
import tools.OutParamWrapper;

import static jlang.cparser.Token.TokenFlags.LeadingSpace;
import static jlang.cparser.Token.TokenFlags.NeedsCleaning;
import static jlang.cparser.Token.TokenFlags.StartOfLine;
import static jlang.cpp.Lexer.WhiteSpacesKind.*;
import static jlang.cpp.TokenKind.*;
import static jlang.diag.DiagnosticLexKindsTag.*;

/**
 * This provides a simple interface that turns a text buffer into a
 * stream of tokens.  This provides no support for file reading or buffering,
 * or buffering/seeking of tokens, only forward lexing is supported.  It relies
 * on the specified Preprocessor object to handle preprocessor directives, etc.
 * @author Xlous.zeng
 * @version 0.1
 */
public class Lexer extends PreprocessorLexer
{
    // The buffer to content to be read.
    char[] buffer;
    // the position of the character next to be read.
    int bufferPtr;
    // The end position of character legally to be read.
    int bufferEnd;

    /**
     * Location for the start of file.
     */
    private SourceLocation fileLoc;

    private LangOptions langOpts;

    /**
     * True if lexer for _Pragma handling.
     */
    private boolean isPragmaLexer;

    private int extendedTokenMode;

    private boolean isAtStartOfLine;

    private void initLexer(char[] buffer, int curPos, int endPos)
    {
        initCharacterInfo();

        this.buffer = buffer;
        this.bufferPtr = curPos;
        bufferEnd = endPos;
        assert buffer[buffer.length-1] == '\0':
                "We assume that the input buffer has a null character at the end" +
        " to simplify lexing!";

        isPragmaLexer = false;

        // Start of the file is a start of line.
        isAtStartOfLine = true;

        // We are not after parsing a #.
        parsingPreprocessorDirective = false;

        // We are not after parsing #include.
        parsingFilename = false;

        // We are not in raw mode.  Raw mode disables diagnostics and interpretation
        // of tokens (e.g. identifiers, thus disabling macro expansion).  It is used
        // to quickly lex the tokens of the buffer, e.g. when handling a "#if 0" block
        // or otherwise skipping over tokens.
        lexingRawMode = false;

        // Default to not keeping comments.
        extendedTokenMode = 0;
    }

    public Lexer(FileID fid, Preprocessor pp)
    {
        super(pp, fid);
        this.fileLoc = pp.getSourceManager().getLocForStartOfFile(fid);
        langOpts = pp.getLangOptions();

        MemoryBuffer inputFile = pp.getSourceManager().getBuffer(fid);

        char[] arr = inputFile.getBuffer().array();
        initLexer(arr, 0, arr.length);

        setCommentRetentionState(pp.getCommentRetentionState());
    }

    /**
     * Create a new raw lexer object.  This object is only
     /// suitable for calls to 'LexRawToken'.  This lexer assumes that the text
     /// range will outlive it, so it doesn't take ownership of it.
     * @param fileLoc
     * @param opts
     * @param buffer
     * @param bufferStartPos
     */
    public Lexer(SourceLocation fileLoc, LangOptions opts,
            char[] buffer, int bufferStartPos, int bufferEnd)
    {
        this.fileLoc = fileLoc;
        this.langOpts = opts;
        initLexer(buffer, bufferStartPos, bufferEnd);

        // We *are* in raw mode.
        lexingRawMode = true;
    }

    /**
     * Create a new raw lexer object.  This object is only
     /// suitable for calls to 'LexRawToken'.  This lexer assumes that the text
     /// range will outlive it, so it doesn't take ownership of it.
     * @param fid
     * @param sourceMgr
     * @param opts
     */
    public Lexer(FileID fid, SourceManager sourceMgr, LangOptions opts)
    {
        fileLoc = sourceMgr.getLocForStartOfFile(fid);
        langOpts = opts;
        MemoryBuffer buf = sourceMgr.getBuffer(fid);
        char[] arr = buf.getBuffer().array();
        initLexer(arr, 0, arr.length);

        lexingRawMode = true;
    }

    public static Lexer createPragmaLexer(SourceLocation spellingLoc,
            SourceLocation instantiationLocStart,
            SourceLocation instantiationLocEnd, int tokenLen, Preprocessor pp)
    {
        SourceManager sgr = pp.getSourceManager();

        FileID spellingFID = sgr.getFileID(spellingLoc);
        Lexer l = new Lexer(spellingFID, pp);

        int strData = sgr.getCharacterData(spellingLoc).offset;

        l.bufferPtr = strData;
        l.bufferEnd = strData + tokenLen;
        assert l.buffer[l.bufferEnd] == '\0': "Buffer is not nul terminated!";

        // Set the SourceLocation with the remapping information.  This ensures that
        // GetMappedTokenLoc will remap the tokens as they are lexed.
        l.fileLoc = sgr.createInstantiationLoc(sgr.getLocForStartOfFile(spellingFID),
                instantiationLocStart,
                instantiationLocEnd, tokenLen,
                0, 0);

        // Ensure that the lexer thinks it is inside a directive, so that end \n will
        // return an EOM token.
        l.parsingPreprocessorDirective = true;

        // This lexer really is for _Pragma.
        l.isPragmaLexer = true;
        return l;
    }

    public LangOptions getLangOpts()
    {
        return langOpts;
    }

    public SourceLocation getFileLoc()
    {
        return fileLoc;
    }

    /**
     * Return the next token in the file.  If this is the end of file, it
     * return the eof token.  Return true if an error occurred and
     * compilation should terminate, false if normal.  This implicitly involves
     * the preprocessor.
     *
     * @return
     */
    public void lex(Token result)
    {
        // Start a new token.
        result.startToken();

        if (isAtStartOfLine)
        {
            result.setFlag(StartOfLine);
            isAtStartOfLine = false;
        }

        // Get a token.  Note that this may delete the current lexer if the end of
        // file is reached.
        lexTokenInternal(result);
    }

    public boolean isPragmaLexer()
    {
        return isPragmaLexer;
    }

    /**
     * Lex a token from a designated raw lexer (one with no
     * associated preprocessor object.  Return true if the 'next character to
     * read' pointer points at the end of the lexer buffer, false otherwise.
     *
     * @param result
     */
    @Override
    public void indirectLex(Token result)
    {
        lex(result);
    }

    public boolean isKeepWhiteSpaceMode()
    {
        return extendedTokenMode > 1;
    }

    public void setKeepWhitespaceMode(boolean val)
    {
        assert (!val
                || lexingRawMode) : "Can only enable whitespace retentation in raw mode";
        extendedTokenMode = val ? 2 : 0;
    }

    public boolean inKeepCommentMode()
    {
        return extendedTokenMode > 0;
    }

    public void setCommentRetentionState(boolean mode)
    {
        assert !isKeepWhiteSpaceMode() : "Can't play with comment retention state when retaining whitespace";
        extendedTokenMode = mode ? 1 : 0;
    }

    public static boolean isObviouslySimpleCharacter(char ch)
    {
        return ch != '?' && ch != '\\';
    }

    /**
     * Like the getCharAndSize method, but does not ever emit a warning.
     *
     * @param chars
     * @param startPos
     * @param charSize
     * @param langOpts
     * @return
     */
    public static char getCharAndSizeNoWarn(char[] chars, int startPos,
            OutParamWrapper<Integer> charSize, LangOptions langOpts)
    {
        if (isIdentifierBody(chars[startPos]))
        {
            charSize.set(1);
            return chars[startPos];
        }

        charSize.set(0);
        return getCharAndSizeSlowNoWarn(chars, startPos, charSize, langOpts);
    }

    private static int getEscapedNewLineSize(char[] buffer, int pos)
    {
        int oldPos = pos;
        while (isWhitespace(buffer[pos]))
        {
            ++pos;

            if (buffer[pos - 1] != '\n' && buffer[pos - 1] != '\r')
                continue;

            if ((buffer[pos] == '\r' || buffer[pos] == '\n')
                    && buffer[pos - 1] != buffer[pos])
                ++pos;

            return pos - oldPos;
        }
        return 0;
    }

    private static char handleEscapedLine(char[] chars, int startPos,
            OutParamWrapper<Integer> size, LangOptions langOpts)
    {
        if (!isWhitespace(chars[startPos]))
            return '\\';

        int escapedNewLineSize = getEscapedNewLineSize(chars, startPos);
        if (escapedNewLineSize != 0)
        {
            size.set(size.get() + escapedNewLineSize);
            startPos += escapedNewLineSize;

            return getCharAndSizeSlowNoWarn(chars, startPos, size, langOpts);
        }

        return '\\';
    }

    /**
     * Given a character that occurs after a ?? pair,
     * return the decoded trigraph letter it corresponds to, or '\0' if nothing.
     *
     * @param letter
     * @return
     */
    private static char getTrigraphCharForLetter(char letter)
    {
        switch (letter)
        {
            default:
                return 0;
            case '=':
                return '#';
            case ')':
                return ']';
            case '(':
                return '[';
            case '!':
                return '|';
            case '\'':
                return '^';
            case '>':
                return '}';
            case '/':
                return '\\';
            case '<':
                return '{';
            case '-':
                return '~';
        }
    }

    public static char getCharAndSizeSlowNoWarn(char[] chars, int startPos,
            OutParamWrapper<Integer> size, LangOptions langOpts)
    {
        if (chars[startPos] == '\\')
        {
            size.set(size.get() + 1);
            ++startPos;
            return handleEscapedLine(chars, startPos, size, langOpts);
        }

        // If this is a trigraph, process it.
        if (langOpts.trigraph && chars[startPos] == '?'
                && chars[startPos + 1] == '?')
        {
            // If this is actually a legal trigraph (not something like "??x"), return
            // it.
            char ch = getTrigraphCharForLetter(chars[startPos + 2]);
            if (ch != 0)
            {
                startPos += 3;
                size.set(size.get() + 3);
                if (ch == '\\')
                    return handleEscapedLine(chars, startPos, size, langOpts);
                return ch;
            }
        }

        size.set(size.get() + 1);
        return chars[startPos];
    }

    public boolean lexFromRawLexer(Token result)
    {
        assert lexingRawMode:"Not already in raw mode!";
        lex(result);

        return bufferPtr == bufferEnd;
    }

    public static int skipEscapedNewLine(char[] buffer, int start)
    {
        while (true)
        {
            int afterEscape = 0;
            if (buffer[start] == '\\')
                afterEscape = start + 1;
            else if (buffer[start] == '?')
            {
                if (buffer[start+1] != '?' || buffer[start+2]!='/')
                    return start;
                afterEscape = start + 3;
            }
            else
            {
                return start;
            }

            int newLineSize = getEscapedNewLineSize(buffer, afterEscape);
            if (newLineSize == 0) return start;
            start = afterEscape + newLineSize;
        }
    }

    //===----------------------------------------------------------------------===//
    // Character information.
    //===----------------------------------------------------------------------===//

    public interface WhiteSpacesKind
    {
        int CHAR_HORZ_WS = 0x01,  // ' ', '\t', '\f', '\v'.  Note, no '\0'
                CHAR_VERT_WS = 0x02,  // '\r', '\n'
                CHAR_LETTER = 0x04,  // a-z,A-Z
                CHAR_NUMBER = 0x08,  // 0-9
                CHAR_UNDER = 0x10,  // _
                CHAR_PERIOD = 0x20;   // .
    }

    // Statically initialize CharInfo table based on ASCII character set
    // Reference: FreeBSD 7.2 /usr/share/misc/ascii
    private static char[] CharInfo = {
            // 0 NUL         1 SOH         2 STX         3 ETX
            // 4 EOT         5 ENQ         6 ACK         7 BEL
            0, 0, 0, 0, 0, 0, 0, 0,
            // 8 BS          9 HT         10 NL         11 VT
            //12 NP         13 CR         14 SO         15 SI
            0, CHAR_HORZ_WS, CHAR_VERT_WS, CHAR_HORZ_WS, CHAR_HORZ_WS,
            CHAR_VERT_WS, 0, 0,
            //16 DLE        17 DC1        18 DC2        19 DC3
            //20 DC4        21 NAK        22 SYN        23 ETB
            0, 0, 0, 0, 0, 0, 0, 0,
            //24 CAN        25 EM         26 SUB        27 ESC
            //28 FS         29 GS         30 RS         31 US
            0, 0, 0, 0, 0, 0, 0, 0,
            //32 SP         33  !         34  "         35  #
            //36  $         37  %         38  &         39  '
            CHAR_HORZ_WS, 0, 0, 0, 0, 0, 0, 0,
            //40  (         41  )         42  *         43  +
            //44  ,         45  -         46  .         47  /
            0, 0, 0, 0, 0, 0, CHAR_PERIOD, 0,
            //48  0         49  1         50  2         51  3
            //52  4         53  5         54  6         55  7
            CHAR_NUMBER, CHAR_NUMBER, CHAR_NUMBER, CHAR_NUMBER, CHAR_NUMBER,
            CHAR_NUMBER, CHAR_NUMBER, CHAR_NUMBER,
            //56  8         57  9         58  :         59  ;
            //60  <         61  =         62  >         63  ?
            CHAR_NUMBER, CHAR_NUMBER, 0, 0, 0, 0, 0, 0,
            //64  @         65  A         66  B         67  C
            //68  D         69  E         70  F         71  G
            0, CHAR_LETTER, CHAR_LETTER, CHAR_LETTER, CHAR_LETTER, CHAR_LETTER,
            CHAR_LETTER, CHAR_LETTER,
            //72  H         73  I         74  J         75  K
            //76  L         77  M         78  N         79  O
            CHAR_LETTER, CHAR_LETTER, CHAR_LETTER, CHAR_LETTER, CHAR_LETTER,
            CHAR_LETTER, CHAR_LETTER, CHAR_LETTER,
            //80  P         81  Q         82  R         83  S
            //84  T         85  U         86  V         87  W
            CHAR_LETTER, CHAR_LETTER, CHAR_LETTER, CHAR_LETTER, CHAR_LETTER,
            CHAR_LETTER, CHAR_LETTER, CHAR_LETTER,
            //88  X         89  Y         90  Z         91  [
            //92  \         93  ]         94  ^         95  _
            CHAR_LETTER, CHAR_LETTER, CHAR_LETTER, 0, 0, 0, 0, CHAR_UNDER,
            //96  `         97  a         98  b         99  c
            //100  d       101  e        102  f        103  g
            0, CHAR_LETTER, CHAR_LETTER, CHAR_LETTER, CHAR_LETTER, CHAR_LETTER,
            CHAR_LETTER, CHAR_LETTER,
            //104  h       105  i        106  j        107  k
            //108  l       109  m        110  n        111  o
            CHAR_LETTER, CHAR_LETTER, CHAR_LETTER, CHAR_LETTER, CHAR_LETTER,
            CHAR_LETTER, CHAR_LETTER, CHAR_LETTER,
            //112  p       113  q        114  r        115  s
            //116  t       117  u        118  v        119  w
            CHAR_LETTER, CHAR_LETTER, CHAR_LETTER, CHAR_LETTER, CHAR_LETTER,
            CHAR_LETTER, CHAR_LETTER, CHAR_LETTER,
            //120  x       121  y        122  z        123  {
            //124  |        125  }        126  ~        127 DEL
            CHAR_LETTER, CHAR_LETTER, CHAR_LETTER, 0, 0, 0, 0, 0 };

    private static boolean isInited = false;

    private static void initCharacterInfo()
    {
        if (isInited)
            return;
        // check the statically-initialized CharInfo table
        assert (CHAR_HORZ_WS == CharInfo[(int) ' ']);
        assert (CHAR_HORZ_WS == CharInfo[(int) '\t']);
        assert (CHAR_HORZ_WS == CharInfo[(int) '\f']);
        assert (CHAR_HORZ_WS == CharInfo[11]);   // for '\v' which is not supported in Java.
        assert (CHAR_VERT_WS == CharInfo[(int) '\n']);
        assert (CHAR_VERT_WS == CharInfo[(int) '\r']);
        assert (CHAR_UNDER == CharInfo[(int) '_']);
        assert (CHAR_PERIOD == CharInfo[(int) '.']);
        for (int i = 'a'; i <= 'z'; ++i)
        {
            assert (CHAR_LETTER == CharInfo[i]);
            assert (CHAR_LETTER == CharInfo[i + 'A' - 'a']);
        }
        for (int i = '0'; i <= '9'; ++i)
            assert (CHAR_NUMBER == CharInfo[i]);
        isInited = true;
    }


    /**isIdentifierBody -Return true if this
    is the
    body character
    of an
    *identifier,
    which is[
    a-zA-Z0-9_].
    */
    private static boolean isIdentifierBody(char c)
    {
        return (CharInfo[c] & (CHAR_LETTER | CHAR_NUMBER | CHAR_UNDER)) != 0;
    }

    /**isHorizontalWhitespace -Return true if this
    character is
    horizontal
    *whitespace:' ','\t','\f','\v'.
    Note that this returns false for'\0'.
     */
    private static boolean isHorizontalWhitespace(char c)
    {
        return (CharInfo[c] & CHAR_HORZ_WS) != 0;
    }

    /***isNumberBody -Return true if this
    is the
    body character
    of an
    *
    preprocessing number, which
    is [a-zA-Z0-9_.].
    */
    private static boolean isNumberBody(char c)
    {
        return (CharInfo[c] & (CHAR_LETTER | CHAR_NUMBER | CHAR_UNDER
                | CHAR_PERIOD)) != 0;
    }

    /**
     * Return true if this character is horizontal or vertical
     * whitespace: ' ', '\t', '\f', '\v', '\n', '\r'.  Note that this returns false
     * for '\0'.
     *
     * @param ch
     * @return
     */
    private static boolean isWhitespace(char ch)
    {
        return (CharInfo[ch] & (CHAR_HORZ_WS | CHAR_VERT_WS)) != 0;
    }

    private int getEscapeNewLineSize(int curPos)
    {
        int size = curPos;
        while (isWhitespace(buffer[size]))
        {
            ++size;

            if (buffer[size] != '\n' && buffer[size - 1] != '\r')
                continue;

            if ((buffer[size] == '\r' || buffer[size] == '\n')
                    && buffer[size - 1] != buffer[size])
            {
                ++size;
            }

            return size;
        }

        return 0;
    }

    /**
     * If lexing out of a 'mapped buffer', where we pretend the
     * lexer buffer was all instantiated at a single point, perform the mapping.
     * This is currently only used for _Pragma implementation, so it is the slow
     * path of the hot getSourceLocation method.  Do not allow it to be inlined.
     *
     * @param pp
     * @param fileLoc
     * @param charNo
     * @param tokLen
     * @return
     */
    private static SourceLocation getMappedTokenLoc(Preprocessor pp,
            SourceLocation fileLoc, int charNo, int tokLen)
    {
        assert fileLoc.isMacroID() : "Must be an instantiation";

        SourceManager srg = pp.getSourceManager();

        SourceLocation spellingLoc = srg.getLiteralLoc(fileLoc);
        spellingLoc = spellingLoc.getFileLocWithOffset(charNo);
        SourceRange range = srg.getImmediateInstantiationRange(fileLoc);

        return srg.createInstantiationLoc(spellingLoc, range.getBegin(),
                range.getEnd(), tokLen, 0, 0);
    }

    private SourceLocation getSourceLocation(int curPos, int toklen)
    {
        assert curPos >= 0 && curPos < bufferEnd
                : "Location out of range for this buffer!";

        int charNo = curPos;
        if (fileLoc.isFileID())
            return fileLoc.getFileLocWithOffset(charNo);

        // Otherwise, this is the _Pragma lexer case, which pretends that all of the
        // tokens are lexed from where the _Pragma was defined.
        assert pp != null : "This doesn't work on raw lexers";
        return getMappedTokenLoc(pp, fileLoc, charNo, toklen);
    }

    private Diagnostic.DiagnosticBuilder diag(int curPos, int diagID)
    {
        return pp.diag(getSourceLocation(curPos, 1), diagID);
    }

    private char handleLeadingSlash(int curPos, OutParamWrapper<Integer> size,
            Token tok)
    {
        // it just a '\\'
        if (!isWhitespace(buffer[curPos]))
            return '\\';

        // See if we have optional whitespace characters between the slash and
        // newline.
        int escapeNewLineSize = getEscapeNewLineSize(curPos);
        if (escapeNewLineSize != 0)
        {
            // Remember that this token needs to be cleaned.
            if (tok != null)
                tok.setFlag(NeedsCleaning);

            // Warn if there was whitespace between the backslash and newline.
            if (buffer[curPos] != '\n' && buffer[curPos] != '\r' && tok != null
                    && !lexingRawMode)
                diag(curPos, backslash_newline_space);

            // Found backslash<whitespace><newline>.  Parse the char after it.
            size.set(size.get() + escapeNewLineSize);
            curPos += escapeNewLineSize;

            return getCharAndSizeSlow(curPos, size, tok);
        }

        // Otherwise, this is not an escaped newline, just return the slash.
        return '\\';
    }

    private static char decodeTrigraphChar(int curPos, Lexer l)
    {
        char res = getTrigraphCharForLetter(l.buffer[curPos]);
        if (res == 0 || l == null) return res;

        if (!l.getLangOpts().trigraph)
        {
            if (!l.isLexingRawMode())
                l.diag(curPos - 2, trigraph_ignored);
            return 0;
        }

        if (!l.isLexingRawMode())
            l.diag(curPos - 2, trigraph_ignored).addTaggedVal(String.valueOf(res));
        return res;
    }

    /**
     * Peek a single 'character' from the specified buffer,
     * get its size, and return it.  This is tricky in several cases:
     * 1. If currently at the start of a trigraph, we warn about the trigraph,
     * then either return the trigraph (skipping 3 chars) or the '?',
     * depending on whether trigraphs are enabled or not.
     * 2. If this is an escaped newline (potentially with whitespace between
     * the backslash and newline), implicitly skip the newline and return
     * the char after it.
     * 3. If this is a UCN, return it.  FIXME: C++ UCN's?
     * <p>
     * This handles the slow/uncommon case of the getCharAndSize method.  Here we
     * know that we can accumulate into Size, and that we have already incremented
     * Ptr by Size bytes.
     *
     * @param curPos
     * @param size
     * @param tok
     * @return
     */
    private char getCharAndSizeSlow(int curPos, OutParamWrapper<Integer> size,
            Token tok)
    {
        if (buffer[curPos] == '\\')
        {
            size.set(size.get() + 1);
            curPos++;
            return handleLeadingSlash(curPos, size, tok);
        }

        // If this is a trigraph, process it.
        if (buffer[curPos] == '?' && buffer[curPos + 1] == '?')
        {
            // If this is actually a legal trigraph (not something like "??x"), emit
            // a trigraph warning.  If so, and if trigraphs are enabled, return it.
            char c = decodeTrigraphChar(curPos + 2, tok != null ? this : null);
            if (tok != null)
                tok.setFlag(NeedsCleaning);

            // Skips the trigraph character.
            curPos += 3;
            size.set(size.get() + 3);
            if (c == '\\')
                return handleLeadingSlash(curPos, size, tok);
            return c;
        }
        size.set(size.get() + 1);
        return buffer[curPos];
    }

    private char getAndAdvanceChar(OutParamWrapper<Integer> curPos, Token tok)
    {
        if (isObviouslySimpleCharacter(buffer[curPos.get()]))
        {
            char ch = buffer[curPos.get()];
            curPos.set(curPos.get() + 1);
            return ch;
        }

        OutParamWrapper<Integer> size = new OutParamWrapper<>(0);
        char ch = getCharAndSizeSlow(curPos.get(), size, tok);
        curPos.set(curPos.get() + size.get());
        return ch;
    }

    /**
     * Read the rest of the current preprocessor line as an
     * uninterpreted string.  This switches the lexer out of directive mode.
     *
     * @return
     */
    public String readToEndOfLine()
    {
        assert parsingPreprocessorDirective && !parsingFilename
                : "Must be in a preprocessing directive!";
        String result = "";
        Token tmp = new Token();
        int curPos = this.bufferPtr;
        while (true)
        {
            OutParamWrapper<Integer> x = new OutParamWrapper<>(curPos);
            char ch = getAndAdvanceChar(x, tmp);
            curPos = x.get();
            switch (ch)
            {
                default:
                    result += ch;
                    break;
                case '\0':
                    // Found end of file?
                    if (curPos - 1 != buffer.length)
                    {
                        result += ch;
                        break;
                    }
                    // fall through
                case '\r':
                case '\n':
                    assert buffer[curPos - 1] == ch : "Trigraphs for newline?";
                    this.bufferPtr = curPos - 1;
                    lex(tmp);

                    // Next, lex the character, which should handle the EOM transition.
                    assert tmp.is(TokenKind.Eom) : "Unexpected token!";

                    // Finally, we're done, return the string we found.
                    return result;
            }
        }
    }

    void formTokenWithChars(Token result, int tokenEnd, TokenKind kind)
    {
        int len = tokenEnd - bufferPtr;

        result.setLength(len);
        result.setLocation(getSourceLocation(bufferPtr, len));
        result.setKind(kind);
        bufferPtr = tokenEnd;
    }

    public boolean lexEndOfFile(Token result, int curPos)
    {
        if (parsingPreprocessorDirective)
        {
            parsingPreprocessorDirective = false;
            // Update the location of token as well as BufferPtr.
            formTokenWithChars(result, curPos, TokenKind.Eom);

            setCommentRetentionState(pp.getCommentRetentionState());
            return true;  // Have a token.
        }

        // If we are in raw mode, return this event as an EOF token.  Let the caller
        // that put us in raw mode handle the event.
        if (isLexingRawMode())
        {
            result.startToken();
            this.bufferPtr = buffer.length;
            formTokenWithChars(result, buffer.length, TokenKind.Eof);
            return true;
        }

        // Otherwise, issue diagnostics for unterminated #if and missing newline.

        // If we are in a #if directive, emit an error.
        while (!conditionalStack.isEmpty())
        {
            pp.diag(conditionalStack.peek().ifLoc,
                    err_pp_unterminated_conditional);
            conditionalStack.pop();
        }

        // C99 5.1.1.2p2: If the file is non-empty and didn't end in a newline, issue
        // a pedwarn.
        if (curPos != 0 && (buffer[curPos - 1] != '\n' && buffer[curPos - 1] != '\r'))
        {
            diag(buffer.length, ext_no_newline_eof).addFixItHint(FixItHint
                    .createInsertion(getSourceLocation(buffer.length, 1), "\n"));
        }

        this.bufferPtr = curPos;
        // Finally, let the preprocessor handle this.
        return pp.handleEndOfFile(result, false);
    }

    public int isNextPPTokenLParen()
    {
        assert !lexingRawMode : "How can we expand a macro from a skipping buffer?";

        lexingRawMode = true;
        int tempPos = bufferPtr;
        boolean inPPDirectiveMode = parsingPreprocessorDirective;

        Token tok = new Token();
        tok.startToken();

        lexTokenInternal(tok);

        bufferPtr = tempPos;
        parsingPreprocessorDirective = inPPDirectiveMode;

        lexingRawMode = false;

        if (tok.is(TokenKind.Eof))
            return 2;
        return tok.is(l_paren) ? 1 : 0;
    }

    private boolean skipWhitespace(Token result, int curPos)
    {
        char ch = buffer[curPos];
        while (true)
        {
            while (isHorizontalWhitespace(ch))
                ch = buffer[++curPos];

            if (ch != '\n' && ch != '\r')
                break;

            if (parsingPreprocessorDirective)
            {
                // End of preprocessor directive line, let LexTokenInternal
                // handle this. bufferPtr = bufferPtr;
                return false;
            }

            result.setFlag(StartOfLine);
            result.clearFlag(LeadingSpace);
            ch = buffer[++curPos];
        }

        char prevCh = buffer[curPos - 1];
        if (prevCh != '\n' && prevCh != '\r')
            result.setFlag(LeadingSpace);

        if (isKeepWhiteSpaceMode())
        {
            formTokenWithChars(result, curPos, TokenKind.Unknown);
            return true;
        }

        this.bufferPtr = curPos;
        return false;
    }

    /**
     * If in save-comment mode, package up this BCPL comment in
     * an appropriate way and return it.
     *
     * @param result
     * @param curPos
     * @return
     */
    private boolean saveBCPLComment(Token result, int curPos)
    {
        formTokenWithChars(result, curPos, TokenKind.Comment);

        if (!parsingPreprocessorDirective)
            return true;

        // If this BCPL-style comment is in a macro definition, transmogrify it into
        // a C-style block comment.
        String spelling = pp.getSpelling(result);
        assert spelling.charAt(0) == '/'
                && spelling.charAt(1) == '/' : "Not bcpl comment?";
        StringBuilder sb = new StringBuilder(spelling);
        sb.setCharAt(1, '*');
        sb.append("*/");
        spelling = sb.toString();

        result.setKind(TokenKind.Comment);
        pp.createString(spelling, result, result.getLocation());
        return true;
    }

    /**
     * We have just read the // characters from input.  Skip until
     * we find the newline character thats terminate the comment.  Then update
     * BufferPtr and return.  If we're in KeepCommentMode, this will form the token
     * and return true.
     *
     * @param result
     * @param curPos
     * @return
     */
    private boolean skipBCPLComment(Token result, int curPos)
    {
        // If BCPL comments aren't explicitly enabled for this language, emit an
        // extension warning.
        if (!langOpts.bcplComment && !isLexingRawMode())
        {
            diag(this.bufferPtr, ext_bcpl_comment);

            // Mark them enabled so we only emit one warning for this translation
            // unit.
            langOpts.bcplComment = true;
        }

        char ch;
        do
        {
            ch = buffer[curPos];

            while (ch != '\0'       // Potentially EOF.
                    && ch != '\\'      // Potentially escaped newline.
                    && ch != '?'     // Potentially trigraph.
                    && ch != '\n' && ch != '\r') // Newline or DOS-style newline.
            {
                ch = buffer[++curPos];
            }

            if (ch == '\n' || ch == '\r')
                break;   // Found the newline? break out!

            // Otherwise, this is a hard case.  Fall back on getAndAdvanceChar to
            // properly decode the character.  Read it in raw mode to avoid emitting
            // diagnostics about things like trigraphs.  If we see an escaped newline,
            // we'll handle it below.
            int oldPos = curPos;
            boolean oldRawMode = isLexingRawMode();
            lexingRawMode = true;
            OutParamWrapper<Integer> x = new OutParamWrapper<>(curPos);
            ch = getAndAdvanceChar(x, result);
            curPos = x.get();
            lexingRawMode = oldRawMode;

            // If the char that we finally got was a \n, then we must have had something
            // like \<newline><newline>.  We don't want to have consumed the second
            // newline, we want CurPtr, to end up pointing to it down below.
            if (ch == '\n' || ch == '\r')
            {
                --curPos;
                ch = 'x';       // doesn't matter what this is.
            }

            if (curPos != oldPos + 1 && ch != '/' && buffer[curPos] != '/')
            {
                for (; oldPos != curPos; ++oldPos)
                {
                    if (buffer[oldPos] == '\n' || buffer[oldPos] == '\r')
                    {
                        // Okay, we found a // comment that ends in a newline, if the next
                        // line is also a // comment, but has spaces, don't emit a diagnostic.
                        if (Character.isSpaceChar(ch))
                        {
                            int forwardPos = curPos;
                            while (Character.isSpaceChar(buffer[forwardPos]))
                            {
                                ++forwardPos;
                            }
                            if (buffer[forwardPos] == '/'
                                    && buffer[forwardPos + 1] == '/')
                                break;
                        }

                        if (!isLexingRawMode())
                            diag(oldPos - 1, ext_multi_line_bcpl_comment);
                        break;
                    }
                }
            }
            if (curPos == buffer.length - 1)
            {
                ++curPos;
                break;
            }
        } while (ch != '\r' && ch != '\r');

        // Found but did not consume the newline.
        if (pp != null)
        {
            pp.handleComment(new SourceRange(getSourceLocation(this.bufferPtr, 1),
                    getSourceLocation(curPos, 1)));
        }

        // If we are returning comments as tokens, return this comment as a token.
        if (inKeepCommentMode())
            return saveBCPLComment(result, curPos);

        // If we are inside a preprocessor directive and we see the end of line,
        // return immediately, so that the lexer can return this as an EOM token.
        if (parsingPreprocessorDirective || curPos == buffer.length)
        {
            this.bufferPtr = curPos;
            return false;
        }

        // Otherwise, eat the \n character.  We don't care if this is a \n\r or
        // \r\n sequence.  This is an efficiency hack (because we know the \n can't
        // contribute to another token), it isn't needed for correctness.  Note that
        // this is ok even in KeepWhitespaceMode, because we would have returned the
        /// comment above in that mode.
        ++curPos;

        // The next returned token is at the start of the line.
        result.setFlag(StartOfLine);

        // No leading whitespace seen so far.
        result.clearFlag(LeadingSpace);
        this.bufferPtr = curPos;
        return false;
    }

    private char getCharAndSize(int curPos, OutParamWrapper<Integer> size)
    {
        if (isObviouslySimpleCharacter(buffer[curPos]))
        {
            size.set(1);
            return buffer[curPos];
        }

        size.set(0);
        return getCharAndSizeSlow(curPos, size, null);
    }

    private static boolean isEndOfBlockCommentWithEscapedNewLine(int curPos,
            Lexer l)
    {
        assert l.buffer[curPos] == '\n' || l.buffer[curPos] == '\r';
        --curPos;

        if (l.buffer[curPos] == '\n' || l.buffer[curPos] == 'r')
        {
            if (l.buffer[curPos] == l.buffer[curPos + 1])
                return false;

            --curPos;
        }

        boolean hasSpace = false;
        while (isHorizontalWhitespace(l.buffer[curPos]) || l.buffer[curPos] == 0)
        {
            --curPos;
            hasSpace = true;
        }

        if (l.buffer[curPos] == '\\')
        {
            if (l.buffer[curPos - 1] != '*')
                return false;
        }
        else
        {
            if (l.buffer[curPos] != '/' || l.buffer[curPos - 1] != '?'
                    || l.buffer[curPos - 2] != '?'
                    || l.buffer[curPos - 3] != '*')
                return false;

            curPos -= 2;

            if (!l.langOpts.trigraph)
            {
                if (!l.isLexingRawMode())
                    l.diag(curPos, trigraph_ignored_block_comment);
                return false;
            }
            if (!l.isLexingRawMode())
                l.diag(curPos, trigraph_ends_block_comment);
        }

        if (!l.isLexingRawMode())
        {
            l.diag(curPos, escaped_newline_block_comment_end);
        }

        if (hasSpace && !l.isLexingRawMode())
            l.diag(curPos, backslash_newline_space);

        return true;
    }

    private boolean skipBlockComment(Token result, int curPos)
    {
        OutParamWrapper<Integer> x = new OutParamWrapper<>(0);
        char ch = getCharAndSize(curPos, x);
        int size = x.get();
        curPos += size;

        if (ch == 0 && curPos == buffer.length + 1)
        {
            if (!isLexingRawMode())
                diag(this.bufferPtr, err_unterminated_block_comment);
            --curPos;

            if (isKeepWhiteSpaceMode())
            {
                formTokenWithChars(result, curPos, TokenKind.Unknown);
                return true;
            }

            this.bufferPtr = curPos;
            return false;
        }

        // Check to see if the first character after the '/*' is another /.  If so,
        // then this slash does not end the block comment, it is part of it.
        if (ch == '/')
            ch = buffer[curPos++];

        while (true)
        {
            // Skip over all non-interesting characters until we find end of buffer or a
            // (probably ending) '/' character.
            if (curPos + 24 < buffer.length)
            {
                while (ch != '/' && (curPos & 0x0f) != 0)
                    ch = buffer[curPos++];

                if (ch != '/')
                {

                    // Scan for '/' quickly.  Many block comments are very large.
                    while (buffer[curPos] != '/' && buffer[curPos + 1] != '/'
                            && buffer[curPos + 2] != '/'
                            && buffer[curPos + 3] != '/' && curPos < buffer.length)
                        curPos += 4;

                    ch = buffer[curPos++];
                }
            }

            while (ch != '/' && ch != '\0')
                ch = buffer[curPos++];

            if (ch == '/')
            {
                if (buffer[curPos - 2] == '*')
                    break;

                if (buffer[curPos - 2] == '\n' || buffer[curPos - 2] == '\r')
                {
                    if (isEndOfBlockCommentWithEscapedNewLine(curPos - 2, this))
                    {
                        // We found the final */, though it had an escaped newline between the
                        // * and /.  We're done!
                        break;
                    }
                }
                if (buffer[curPos] == '*' && buffer[curPos + 1] != '/')
                {
                    // If this is a /* inside of the comment, emit a warning.  Don't do this
                    // if this is a /*/, which will end the comment.  This misses cases with
                    // embedded escaped newlines, but oh well.
                    if (!isLexingRawMode())
                        diag(curPos - 1, warn_nested_block_comment);
                }
            }
            else if (ch == 0 && curPos == buffer.length + 1)
            {
                if (!isLexingRawMode())
                {
                    diag(this.bufferPtr, err_unterminated_block_comment);
                }
                --curPos;

                if (isKeepWhiteSpaceMode())
                {
                    formTokenWithChars(result, curPos, TokenKind.Unknown);
                    return true;
                }

                this.bufferPtr = curPos;
                return false;
            }
            ch = buffer[curPos++];
        }
        if (pp != null)
            pp.handleComment(new SourceRange(getSourceLocation(this.bufferPtr, 1),
                    getSourceLocation(curPos, 1)));

        if (isKeepWhiteSpaceMode())
        {
            formTokenWithChars(result, curPos, TokenKind.Comment);
            return true;
        }

        if (isHorizontalWhitespace(buffer[curPos]))
        {
            result.setFlag(LeadingSpace);
            skipWhitespace(result, curPos + 1);
            return false;
        }

        this.bufferPtr = curPos;
        result.setFlag(LeadingSpace);
        return false;
    }

    /**
     * Lex the remainder of a integer or floating point
     * constant. {@code bufferPtr} is the first character lexed.
     * Return the end of the constant.
     * @param result
     * @param curPos
     */
    private void lexNumbericConstant(Token result, int curPos)
    {
        OutParamWrapper<Integer> size = new OutParamWrapper<>(0);
        char ch = getCharAndSize(curPos, size);
        char prevChar = 0;
        while (isNumberBody(ch))
        {
            curPos = consumeChar(curPos, size.get(), result);
            prevChar = ch;
            ch = getCharAndSize(curPos, size);
        }

        // If we fell out, check for a sign, due to 1e+12.  If we have one, continue.
        if ((ch == '-' || ch == '+') && (prevChar == 'e' || prevChar == 'E'))
        {
            lexNumbericConstant(result, consumeChar(curPos, size.get(), result));
            return;
        }

        // If we have a hex FP constant, continue.
        if ((ch == '-' || ch == '+') && (prevChar == 'P' || prevChar == 'p'))
        {
            lexNumbericConstant(result, consumeChar(curPos, size.get(), result));
            return;
        }

        // Update the location of token as well as bufferPtr.
        int tokStart = this.bufferPtr;
        char[] literal = new char[curPos - tokStart];
        System.arraycopy(buffer, tokStart, literal, 0, literal.length);
        formTokenWithChars(result, curPos, TokenKind.Numeric_constant);
        result.setLiteralData(literal, buffer, tokStart);
    }

    private void finishIdentifier(Token result, int curPos)
    {
        int idStart = this.bufferPtr;
        formTokenWithChars(result, curPos, Identifier);

        if (isLexingRawMode()) return;

        IdentifierInfo ii = pp.lookupIdentifierInfo(result, buffer, idStart);

        result.setKind(ii.getTokenID());

        // Finally, now that we know we have an identifier, pass this off to the
        // preprocessor, which may macro expand it or something.
        if (ii.isNeedsHandleIdentifier())
            pp.handleIdentifier(result);
    }

    private void lexIdentifier(Token result, int curPos)
    {
        // Match [_A-Za-z0-9]*, we have already matched [_A-Za-z$]
        OutParamWrapper<Integer> size = new OutParamWrapper<>(0);
        char ch = buffer[curPos++];
        while (isIdentifierBody(ch))
            ch = buffer[curPos++];

        --curPos;

        if (ch != '\\' && ch != '?' && (ch != '$' || langOpts.dollarIdents))
        {
            finishIdentifier(result, curPos);
            return;
        }

        // Otherwise, $,\,? in identifier found.  Enter slower path.
        ch = getCharAndSize(curPos, size);
        while (true)
        {
            if (ch == '$')
            {
                // If we hit a $ and they are not supported in identifiers, we are done.
                if (!langOpts.dollarIdents)
                {
                    finishIdentifier(result, curPos);
                    return;
                }

                if (!isLexingRawMode())
                    diag(curPos, ext_dollar_in_identifier);
                curPos = consumeChar(curPos, size.get(), result);
                ch = getCharAndSize(curPos, size);
                continue;
            }
            else if (!isIdentifierBody(ch))
            {
                // Found end of identifier.
                finishIdentifier(result, curPos);
            }

            // Otherwise, this character is good, consume it.
            curPos = consumeChar(curPos, size.get(), result);

            ch = getCharAndSize(curPos, size);
            while (isIdentifierBody(ch))
            {
                curPos = consumeChar(curPos, size.get(), result);
                ch = getCharAndSize(curPos, size);
            }
        }
    }

    /**
     * Lex the remainder of a character constant, after having
     * lexed either ' or L'.
     * @param result
     * @param curPos
     */
    private void lexCharConstant(Token result, int curPos)
    {
        // Does this character contain the \0 character?
        int nullCharacterPos = 0;

        OutParamWrapper<Integer> x = new OutParamWrapper<>(curPos);
        char ch = getAndAdvanceChar(x, result);
        if (ch == '\'')
        {
            if (!isLexingRawMode() && !langOpts.asmPreprocessor)
            {
                diag(this.bufferPtr, err_empty_character);
            }
            formTokenWithChars(result, x.get(), Unknown);
            return;
        }
        else if (ch == '\\')
        {
            // Skip the escaped character.
            ch = getAndAdvanceChar(x, result);
        }

        if (ch != 0 && ch != '\n' && ch != '\r' && buffer[x.get()] == '\'')
        {
            x.set(x.get() + 1);
        }
        else
        {
            // Fall back on generic code for embedded nulls, newlines, wide chars.
            do
            {
                if (ch == '\\')
                {
                    ch = getAndAdvanceChar(x, result);
                }
                else if (ch == '\n' || ch == '\r' ||
                        (ch == 0 && x.get() - 1 == buffer.length))
                {
                    if (!isLexingRawMode() && !langOpts.asmPreprocessor)
                    {
                        diag(this.bufferPtr, err_unterminated_char);
                    }
                    formTokenWithChars(result, x.get() - 1, Unknown);
                    return;
                }
                else if (ch == 0)
                {
                    nullCharacterPos = x.get() - 1;
                }
                ch = getAndAdvanceChar(x, result);
            } while(ch != '\'');
        }

        if (nullCharacterPos != 0 && !isLexingRawMode())
            diag(nullCharacterPos, null_in_char);

        int tokStart = this.bufferPtr;
        char[] tokenBuf = new char[x.get() - tokStart];
        System.arraycopy(buffer, tokStart, tokenBuf, 0, tokenBuf.length);;
        formTokenWithChars(result, x.get(), Char_constant);
        result.setLiteralData(tokenBuf, buffer, tokStart);
    }

    /**
     * Lex the remainder of a string literal, after having lexed either " or L".
     * @param result
     * @param curPos
     */
    private void lexStringConstant(Token result, int curPos, boolean wide)
    {
        int nullCharPos = 0;
        OutParamWrapper<Integer> x = new OutParamWrapper<>(curPos);
        char ch = getAndAdvanceChar(x, result);

        while (ch != '"')
        {
            // SKip escaped characters.
            if (ch == '\\')
            {
                ch = getAndAdvanceChar(x, result);
            }
            else if (ch == '\n' || ch == '\r' ||
                    (ch == 0 && x.get() -1 == buffer.length))
            {
                if (!isLexingRawMode() && !langOpts.asmPreprocessor)
                    diag(curPos, err_unterminated_string);
                formTokenWithChars(result, x.get() - 1 , Unknown);
                return;
            }
            else if (ch == 0)
            {
                nullCharPos = x.get() - 1;
            }
            ch = getAndAdvanceChar(x, result);
        }

        // If a nul character existed in the string, warn about it.
        if (nullCharPos != 0 && !isLexingRawMode())
            diag(nullCharPos, null_in_string);

        int tokStart = this.bufferPtr;
        char[] tokenBuf = new char[x.get() - tokStart];
        System.arraycopy(buffer, tokStart, tokenBuf, 0, tokenBuf.length);
        formTokenWithChars(result, x.get(), String_literal);
        result.setLiteralData(tokenBuf, buffer, tokStart);
    }

    /**
     * Lex the remainder of an angled string literal,
     * after having lexed the '<' character.  This is used for #include filenames.
     * @param result
     * @param curPos
     */
    private void lexAngledStringLiteral(Token result, int curPos)
    {
        int nullCharacterPos = 0;
        OutParamWrapper<Integer> x = new OutParamWrapper<>(curPos);
        char ch = getAndAdvanceChar(x, result);

        while (ch != '>')
        {
            if (ch == '\\')
            {
                // SKip the escaped character.
                ch = getAndAdvanceChar(x, result);
            }
            else if (ch == '\n' || ch == '\r'
                    || (ch == 0 && buffer[x.get() - 1] == buffer.length))
            {
                // If the filename is unterminated, then it must just be a lone <
                // character.  Return this as such.
                formTokenWithChars(result, curPos, less);
                return;
            }
            else if (ch == 0)
            {
                nullCharacterPos = curPos - 1;
            }
            ch = getAndAdvanceChar(x, result);
        }

        // If a nul character existed in the string, warn about it.
        if(nullCharacterPos != 0 && !isLexingRawMode())
            diag(nullCharacterPos, null_in_string);

        int tokStart = this.bufferPtr;
        char[] tokenBuf = new char[x.get() - tokStart];
        System.arraycopy(buffer, tokStart, tokenBuf, 0, tokenBuf.length);;
        formTokenWithChars(result, x.get(), Angle_string_literal);
        result.setLiteralData(tokenBuf, buffer, tokStart);
    }

    private int consumeChar(int curPos, int size, Token tok)
    {
        if (size == 1)
            return curPos + size;

        OutParamWrapper<Integer> x = new OutParamWrapper<>(0);
        getCharAndSizeSlow(curPos, x, tok);
        return curPos + x.get();
    }

    /**
     * This implements a simple C family lexer.  It is an
     * extremely performance critical piece of code.  This assumes that the buffer
     * has a null character at the end of the file.  This returns a preprocessing
     * token, not a normal token, as such, it is an internal interface.  It assumes
     * that the Flags of result have been cleared before calling this.
     * @param result
     */
    public void lexTokenInternal(Token result)
    {
        // New token, can't need cleaning yet.
        result.clearFlag(NeedsCleaning);
        result.setIdentifierInfo(null);

        int curPos = this.bufferPtr;

        // Small amounts of horizontal whitespace is very common between tokens.
        if (buffer[curPos] == ' ' || buffer[curPos] == '\t')
        {
            ++curPos;

            while (buffer[curPos] == ' ' || buffer[curPos] == '\t')
                ++curPos;

            // If we are keeping whitespace and other tokens, just return what we just
            // skipped.  The next lexer invocation will return the token after the
            // whitespace.
            if (isKeepWhiteSpaceMode())
            {
                formTokenWithChars(result, curPos, TokenKind.Unknown);
                return;
            }

            this.bufferPtr = curPos;
            result.setFlag(LeadingSpace);
        }

        int sizeTmp = 0, sizeTmp2 = 0;   // Temporaries for use in cases below.

        // Read a character, advancing over it.
        OutParamWrapper<Integer> x = new OutParamWrapper<>(curPos);
        char ch = getAndAdvanceChar(x, result);
        curPos = x.get();

        TokenKind kind = null;
        switch (ch)
        {
            case '\0':
            {
                // Null.
                if (curPos - 1 == buffer.length)
                {
                    Preprocessor ppcache = pp;
                    // Retreat back into the file.
                    if (lexEndOfFile(result, curPos - 1))
                        return;   // Got a token to return.
                    assert ppcache!= null:"Raw buffer::LexEndOfFile should return a token";
                    ppcache.lex(result);
                    return;
                }

                if (!isLexingRawMode())
                    diag(curPos - 1, null_in_file);
                result.setFlag(LeadingSpace);
                if (skipWhitespace(result, curPos))
                    return;

                lexTokenInternal(result);
                return;
            }
            case '\n':
            case '\r':
            {
                // If we are inside a preprocessor directive and we see the end of line,
                // we know we are done with the directive, so return an EOM token.
                if (parsingPreprocessorDirective)
                {
                    parsingPreprocessorDirective = false;
                    setCommentRetentionState(pp.getCommentRetentionState());

                    isAtStartOfLine = true;

                    kind = TokenKind.Eom;
                    break;
                }

                result.setFlag(StartOfLine);
                result.clearFlag(LeadingSpace);
                if (skipWhitespace(result, curPos))
                    return;

                lexTokenInternal(result);
                return;
            }
            case ' ':
            case '\t':
            case '\f':
            case 11: // '\v'
            {
                while (true)
                {
                    result.setFlag(LeadingSpace);
                    if (skipWhitespace(result, curPos))
                        return;

                    while (true)
                    {
                        curPos = this.bufferPtr;

                        if (buffer[curPos] == '/' && buffer[curPos+1] == '/' && !inKeepCommentMode()
                                && langOpts.bcplComment)
                        {
                            skipBCPLComment(result, curPos+2);
                        }
                        else if (buffer[curPos] == '/' && buffer[curPos+1] == '*' && !inKeepCommentMode())
                        {
                            skipBlockComment(result, curPos+2);
                        }
                        else if (isHorizontalWhitespace(buffer[curPos]))
                        {
                            break;
                        }
                        else
                        {
                            lexTokenInternal(result);
                            return;
                        }
                    }
                }
            }
            case '0':case '1':case '2':case '3':case '4':
            case '5':case '6': case '7':case '8':case '9':
            {
                // C99 6.4.4.1: Integer Constants.
                // C99 6.4.4.2: Floating Constants.
                // Notify MIOpt that we read a non-whitespace/non-comment token.
                miOpt.readToken();
                lexNumbericConstant(result, curPos);
                return;
            }
            case 'L':
            {
                // TODO identify wide character or string.
                break;
            }

            case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': case 'G':
            case 'H': case 'I': case 'J': case 'K':    /*'L'*/case 'M': case 'N':
            case 'O': case 'P': case 'Q': case 'R': case 'S': case 'T': case 'U':
            case 'V': case 'W': case 'X': case 'Y': case 'Z':
            case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g':
            case 'h': case 'i': case 'j': case 'k': case 'l': case 'm': case 'n':
            case 'o': case 'p': case 'q': case 'r': case 's': case 't': case 'u':
            case 'v': case 'w': case 'x': case 'y': case 'z':
            case '_':
            {
                // C99 6.4.2: Identifiers.
                // Notify MIOpt that we read a non-whitespace/non-comment token.
                miOpt.readToken();
                lexIdentifier(result, curPos);
                return;
            }
            case '$':
            {
                // $ in identifier
                if (langOpts.dollarIdents)
                {
                    if (!isLexingRawMode())
                        diag(curPos-1, ext_dollar_in_identifier);

                    // Notify MIOpt that we read a non-whitespace/non-comment token.
                    miOpt.readToken();
                    lexIdentifier(result, curPos);
                    return;
                }

                kind = TokenKind.Unknown;
                break;
            }
            case '\'':
            {
                // C99 6.4.4: Character Constants.
                // Notify MIOpt that we read a non-whitespace/non-comment token.
                miOpt.readToken();
                lexCharConstant(result, curPos);
                return;
            }
            case '"':
            {
                // C99 6.4.5: String Literals.
                // Notify MIOpt that we read a non-whitespace/non-comment token.
                miOpt.readToken();
                lexStringConstant(result, curPos, false);
                return;
            }

            // C99 6.4.6: Punctuators.
            case '?':
                kind = question;
                break;
            case '[':
                kind = l_bracket;
                break;
            case ']':
                kind = r_bracket;
                break;
            case '(':
                kind = l_paren;
                break;
            case ')':
                kind = r_paren;
                break;
            case '{':
                kind = l_brace;
                break;
            case '}':
                kind = r_brace;
                break;
            case '.':
            {
                x = new OutParamWrapper<>(sizeTmp);
                ch = getCharAndSize(curPos, x);
                sizeTmp = x.get();
                OutParamWrapper<Integer> y = new OutParamWrapper<>(sizeTmp2);
                if (ch >= '0' && ch <= '9')
                {
                    // Notify MIOpt that we read a non-whitespace/non-comment token.
                    miOpt.readToken();

                    lexNumbericConstant(result, curPos);
                    return;
                }
                else if (ch == '.' && getCharAndSize(curPos + sizeTmp, y) == '.')
                {
                    sizeTmp2 = y.get();
                    kind = TokenKind.ellipsis;
                    curPos = consumeChar(consumeChar(curPos, sizeTmp, result), sizeTmp2, result);
                }
                else
                {
                    kind = TokenKind.dot;
                }
                break;
            }
            case '&':
            {
                x = new OutParamWrapper<>(sizeTmp);
                ch = getCharAndSize(curPos, x);
                sizeTmp = x.get();
                if (ch == '&')
                {
                    kind = ampamp;
                    curPos = consumeChar(curPos, sizeTmp, result);
                }
                else if (ch == '=')
                {
                    kind = ampequal;
                    curPos = consumeChar(curPos, sizeTmp, result);
                }
                else
                {
                    kind = amp;
                }
                break;
            }
            case '*':
            {
                x = new OutParamWrapper<>(sizeTmp);
                ch = getCharAndSize(curPos, x);
                sizeTmp = x.get();
                if (ch == '=')
                {
                    kind = starequal;
                    curPos = consumeChar(curPos, sizeTmp, result);
                }
                else
                {
                    kind = star;
                }
                break;
            }
            case '+':
            {
                x = new OutParamWrapper<>(sizeTmp);
                ch = getCharAndSize(curPos, x);
                sizeTmp = x.get();
                if (ch == '+')
                {
                    curPos = consumeChar(curPos, sizeTmp, result);
                    kind = plusplus;
                }
                else if (ch == '=')
                {
                    curPos = consumeChar(curPos, sizeTmp, result);
                    kind = plusequal;
                }
                else
                {
                    kind = plus;
                }
                break;
            }
            case '-':
            {
                x = new OutParamWrapper<>(sizeTmp);
                ch = getCharAndSize(curPos, x);
                sizeTmp = x.get();
                if (ch == '-')
                {
                    curPos = consumeChar(curPos, sizeTmp, result);
                    kind = subsub;
                }
                else if (ch == '>')
                {
                    curPos = consumeChar(curPos, sizeTmp, result);
                    kind = arrow;
                }
                else if (ch == '=')
                {
                    curPos = consumeChar(curPos, sizeTmp, result);
                    kind = subequal;
                }
                else
                {
                    kind = sub;
                }
                break;
            }
            case '~':
            {
                kind = tilde;
                break;
            }
            case '!':
            {
                x = new OutParamWrapper<>(sizeTmp);
                ch = getCharAndSize(curPos, x);
                sizeTmp = x.get();
                if (ch == '=')
                {
                    kind = TokenKind.bangequal;
                    curPos = consumeChar(curPos, sizeTmp, result);
                }
                else
                {
                    kind = bang;
                }
                break;
            }
            case '/':
                x = new OutParamWrapper<>(sizeTmp);
                ch = getCharAndSize(curPos, x);
                sizeTmp = x.get();
                if (ch == '/')
                {
                    // BCPL comment.
                    OutParamWrapper<Integer> xx = new OutParamWrapper<>(sizeTmp2);
                    char tmp = getCharAndSize(curPos + sizeTmp, xx);
                    sizeTmp2 = xx.get();
                    if (langOpts.bcplComment || tmp != '*')
                    {
                        if (skipBCPLComment(result, consumeChar(curPos, sizeTmp, result)))
                            return;

                        lexTokenInternal(result);
                        return;
                    }
                }
                if (ch == '*')
                {
                    //  /**/ comment.
                    if (skipBlockComment(result, consumeChar(curPos, sizeTmp, result)))
                        return;

                    lexTokenInternal(result);
                    return;
                }

                if (ch == '=')
                {
                    curPos = consumeChar(curPos, sizeTmp, result);
                    kind = slashequal;
                }
                else
                {
                    kind = slash;
                }
                break;
            case '%':
            {
                x = new OutParamWrapper<>(sizeTmp);
                ch = getCharAndSize(curPos, x);
                sizeTmp = x.get();
                if (ch == '=')
                {
                    kind = percentequal;
                    curPos = consumeChar(curPos, sizeTmp, result);
                }
                else if (langOpts.digraphs && ch == '>')
                {
                    // '%>' --> '}'.
                    kind = r_brace;
                    curPos = consumeChar(curPos, sizeTmp, result);
                }
                else if (langOpts.digraphs && ch == ':')
                {
                    curPos = consumeChar(curPos, sizeTmp, result);
                    x.set(sizeTmp);
                    ch = getCharAndSize(curPos, x);
                    sizeTmp = x.get();
                    OutParamWrapper<Integer> y = new OutParamWrapper<>(sizeTmp2);
                    char t = getCharAndSize(curPos + sizeTmp, y);
                    sizeTmp2 = y.get();

                    if (ch == '%' && t == ':')
                    {
                        // '%:%' --> '##'
                        kind = hashhash;
                        curPos = consumeChar(consumeChar(curPos, sizeTmp, result),
                                sizeTmp2, result);
                    }
                    else
                    {
                        // '%:' -> '#'

                        // We parsed a # character.  If this occurs at the start of the line,
                        // it's actually the start of a preprocessing directive.  Callback to
                        // the preprocessor to handle it.
                        // FIXME: -fpreprocessed mode??
                        if (result.isAtStartOfLine() && !lexingRawMode && isPragmaLexer)
                        {
                            formTokenWithChars(result, curPos, hash);
                            pp.handleDirective(result);

                            if (pp.isCurrentLexer(this))
                            {
                                if (isAtStartOfLine)
                                {
                                    result.setFlag(StartOfLine);
                                    isAtStartOfLine = false;
                                }
                                lexTokenInternal(result);
                                return;
                            }

                            pp.lex(result);
                            return;
                        }
                    }
                }
                else
                {
                    kind = percent;
                }
                break;
            }
            case '<':
                x = new OutParamWrapper<>(sizeTmp);
                ch = getCharAndSize(curPos, x);
                sizeTmp = x.get();
                OutParamWrapper<Integer> y = new OutParamWrapper<>(sizeTmp2);
                if (parsingFilename)
                {
                    lexAngledStringLiteral(result, curPos);
                    return;
                }
                else if (ch == '<' && getCharAndSize(curPos+sizeTmp, y) == '=')
                {
                    kind = lesslessequal;
                    sizeTmp2 = y.get();
                    curPos = consumeChar(consumeChar(curPos, sizeTmp, result), sizeTmp2, result);;
                }
                else if (ch == '<')
                {
                    kind = lessless;
                    curPos = consumeChar(consumeChar(curPos, sizeTmp, result), sizeTmp2, result);;
                }
                else if (ch == '=')
                {
                    kind = lessequal;
                    curPos = consumeChar(consumeChar(curPos, sizeTmp, result), sizeTmp2, result);;
                }
                else if (langOpts.digraphs && ch == ':')
                {
                    // '<:' -> '['
                    kind = l_bracket;
                    curPos = consumeChar(consumeChar(curPos, sizeTmp, result), sizeTmp2, result);;
                }
                else if (langOpts.digraphs && ch == '%')
                {
                    // '<%' -> '{'
                    kind = l_brace;
                    curPos = consumeChar(consumeChar(curPos, sizeTmp, result), sizeTmp2, result);;
                }
                else
                {
                    kind = less;
                }
                break;
            case '>':
            {
                x = new OutParamWrapper<>(sizeTmp);
                ch = getCharAndSize(curPos, x);
                sizeTmp = x.get();
                y = new OutParamWrapper<>(sizeTmp2);
                if (ch == '=')
                {
                    curPos = consumeChar(curPos, sizeTmp, result);
                    kind = greaterequal;
                }
                else if (ch == '>' && getCharAndSize(curPos+sizeTmp, y) == '=')
                {
                    sizeTmp2 = y.get();
                    curPos = consumeChar(consumeChar(curPos, sizeTmp, result), sizeTmp2, result);
                    kind = greatergreaterequal;
                }
                else if (ch == '>')
                {
                    curPos = consumeChar(curPos, sizeTmp, result);
                    kind = greatergreater;
                }
                else
                {
                    kind = greater;
                }
                break;
            }
            case '^':
                x = new OutParamWrapper<>(sizeTmp);
                ch = getCharAndSize(curPos, x);
                sizeTmp = x.get();
                if (ch == '=')
                {
                    curPos = consumeChar(curPos, sizeTmp, result);
                    kind = caretequal;
                }
                else
                {
                    kind = caret;
                }
                break;
            case '|':
                x = new OutParamWrapper<>(sizeTmp);
                ch = getCharAndSize(curPos, x);
                sizeTmp = x.get();
                if (ch == '=')
                {
                    curPos = consumeChar(curPos, sizeTmp, result);
                    kind = barequal;
                }
                else if (ch == '|')
                {
                    curPos = consumeChar(curPos, sizeTmp, result);
                    kind = barbar;
                }
                else
                {
                    kind = bar;
                }
                break;
            case ':':
                x = new OutParamWrapper<>(sizeTmp);
                ch = getCharAndSize(curPos, x);
                sizeTmp = x.get();
                if (ch == '>' && langOpts.digraphs)
                {
                    // ':>' --> ']'.
                    curPos = consumeChar(curPos, sizeTmp, result);
                    kind = r_bracket;
                }
                else
                {
                    kind = colon;
                }
                break;
            case ';':
                kind = semi;
                break;
            case '=':
                x = new OutParamWrapper<>(sizeTmp);
                ch = getCharAndSize(curPos, x);
                sizeTmp = x.get();
                if (ch == '=')
                {
                    curPos = consumeChar(curPos, sizeTmp, result);
                    kind = equalequal;
                }
                else
                {
                    kind = equal;
                }
                break;
            case ',':
                kind = comma;
                break;
            case '#':
                x = new OutParamWrapper<>(sizeTmp);
                ch = getCharAndSize(curPos, x);
                sizeTmp = x.get();
                if (ch == '#')
                {
                    curPos = consumeChar(curPos, sizeTmp, result);
                    kind = hashhash;
                }
                else
                {
                    // We parsed a # character.  If this occurs at the start of the line,
                    // it's actually the start of a preprocessing directive.  Callback to
                    // the preprocessor to handle it.
                    if (result.isAtStartOfLine() && !lexingRawMode && !isPragmaLexer)
                    {
                        formTokenWithChars(result, curPos, hash);
                        pp.handleDirective(result);

                        // As an optimization, if the preprocessor didn't switch lexers, tail
                        // recurse.
                        if (pp.isCurrentLexer(this))
                        {
                            // Start a new token.  If this is a #include or something, the PP may
                            // want us starting at the beginning of the line again.  If so, set
                            // the StartOfLine flag.
                            if (isAtStartOfLine)
                            {
                                result.setFlag(StartOfLine);
                                isAtStartOfLine = false;
                            }
                            lexTokenInternal(result);
                            return;
                        }
                        pp.lex(result);
                        return;
                    }

                    kind = hash;
                }
                break;
            case '\\':
            default:
                kind = Unknown;
                break;
        }
        miOpt.readToken();;

        // Update the location of token as well as current position.
        formTokenWithChars(result, curPos, kind);
    }

    @Override
    public SourceLocation getSourceLocation()
    {
        return getSourceLocation(bufferPtr, 1);
    }

    /**
     * Convert the specified string into a C string, with surrounding
     * ""'s, and with escaped \ and " characters.
     * @param str
     * @return
     */
    public static String stringify(String str)
    {
        StringBuilder result = new StringBuilder();
        char quote = '"';
        for (int i = 0, e = str.length(); i < e; i++)
        {
            if (str.charAt(i) == '\\' || str.charAt(i) == quote)
            {
                result.append('\\');
                result.append(str.charAt(i));
            }
        }
        return result.toString();
    }

    /**
     * Convert the specified string into a C string, with surrounding
     * ""'s, and with escaped \ and " characters.
     * @param str
     * @return
     */
    public void stringify(StringBuilder str)
    {
        for (int i = 0, e = str.length(); i < e; i++)
        {
            if (str.charAt(i) == '\\' || str.charAt(i) == '"')
            {
                str.insert(i, '\\');
                i++;
                ++e;
            }
        }
    }
}
