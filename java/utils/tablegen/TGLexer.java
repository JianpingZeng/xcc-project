package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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

import cfe.support.MemoryBuffer;
import tools.Error;
import tools.SourceMgr;
import tools.SourceMgr.SMLoc;
import tools.Util;

import java.io.File;

/**
 * This is a lexer for TableGen Files.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public final class TGLexer {
  enum TokKind {
    Eof, Error,

    minus, plus,        // - +
    l_square, r_square, // [ ]
    l_brace, r_brace,   // { }
    l_paren, r_paren,   // ( )
    less, greater,      // < >
    colon, semi,        // : ;
    comma, dot,         // , .
    equal, question,    // = ?
    paste,              // #

    // Keywords
    Bit, Bits, Class, Code, Dag, Def, Defm, Field, In, Int, Let, List, Multiclass, String,

    // !keywords.
    XConcat, XSRA, XSRL, XSHL, XStrConcat, XNameConcat, XCast,
    XSubst, XForEach, XCar, XCdr, XNull, XIf, XEq, XNe, XLt,
    XLe, XGt, XGe, XAdd, XAnd,

    // Integer value.
    IntVal,

    // String valued tokens.
    Id, StrVal, VarName, CodeFragment
  }

  private static final int EOF = -1;

  private SourceMgr sgr;
  private MemoryBuffer curBuf;
  private int curPtr;

  // Information about the current token.
  private int tokStart;
  private TokKind curCode;
  private String curStrVal;  // This is valid for ID, STRVAL, VARNAME, CODEFRAGMENT
  private long curIntVal;      // This is valid for INTVAL.

  /**
   * This is the current buffer index we're lexing from as managed
   * by the SourceManager object.
   */
  private int curBufferIdx;

  public TGLexer(SourceMgr sgr) {
    this.sgr = sgr;
    curBufferIdx = 0;
    curBuf = sgr.getMemoryBuffer(curBufferIdx);
    curPtr = curBuf.getBufferStart();
    tokStart = 0;
  }

  public TokKind lex() {
    curCode = nextToken();
    return curCode;
  }

  public TokKind getCode() {
    return curCode;
  }

  public String getCurStrVal() {
    Util.assertion(curCode == TokKind.Id || curCode == TokKind.StrVal ||
        curCode == TokKind.VarName ||
        curCode == TokKind.CodeFragment, "This is token does not have a string value!");

    return curStrVal;
  }

  long getCurIntVal() {
    Util.assertion(curCode == TokKind.IntVal, "This is token does not have a int value!");

    return curIntVal;
  }

  public SMLoc getLoc() {
    MemoryBuffer mb = curBuf.clone();
    mb.setBufferStart(tokStart);
    return SMLoc.get(mb);
  }

  /**
   * Read the next token and return its token kind.
   *
   * @return
   */
  private TokKind nextToken() {
    while (true) {
      tokStart = curPtr;
      int curChar = getNextChar();
      switch (curChar) {
        default:
          // Handle the letters: [a-zA-Z].
          if (Character.isJavaIdentifierPart(curChar))
            return lexIdentifier();

          // Unknown character, emit an error.
          MemoryBuffer mb = curBuf.clone();
          mb.setBufferStart(tokStart);
          return returnError(mb, "unexpected character");
        case EOF:
          return TokKind.Eof;
        case ';':
          return TokKind.semi;
        case ':':
          return TokKind.colon;
        case ',':
          return TokKind.comma;
        case '.':
          return TokKind.dot;
        case '<':
          return TokKind.less;
        case '=':
          return TokKind.equal;
        case '>':
          return TokKind.greater;
        case '?':
          return TokKind.question;
        case ']':
          return TokKind.r_square;
        case '{':
          return TokKind.l_brace;
        case '}':
          return TokKind.r_brace;
        case '(':
          return TokKind.l_paren;
        case ')':
          return TokKind.r_paren;
        case '#':
          return TokKind.paste;

        case 0:
        case ' ':
        case '\t':
        case '\n':
        case '\r':
          // ignores the whitespaces.
          break;
        case '/':
          if (curBuf.getCharAt(curPtr) == '/')
            skipBCPLComment();
          else if (curBuf.getCharAt(curPtr) == '*') {
            if (skipCComment())
              return TokKind.Error;
          } else {
            // Undefined character, emit an error.
            MemoryBuffer mb2 = curBuf.clone();
            mb2.setBufferStart(tokStart);
            return returnError(mb2, "unexpected character");
          }
          break;
        case '-':
        case '+':
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
          return lexNumber();
        case '"':
          return lexString();
        case '$':
          return lexVarName();
        case '[':
          return lexBracket();
        case '!':
          return lexExclaim();
      }
    }
  }

  private TokKind returnError(MemoryBuffer loc, String msg) {
    Error.printError(loc, msg);
    return TokKind.Error;
  }

  private int getNextChar() {
    char curChar = curBuf.getCharAt(curPtr++);
    switch (curChar) {
      default:
        return curChar;
      case 0: {
        // A nul character in the stream is either the end of the current buffer or
        // a random nul in the file.  Disambiguate that here.
        if (curPtr - 1 != curBuf.length())
          return 0;

        // If this is the end of an included file, pop the parent file off
        // the include stack.
        SMLoc parentIncludeLoc = sgr.getParentIncludeLoc(curBufferIdx);
        if (parentIncludeLoc.isValid()) {
          curBufferIdx = sgr.findBufferContainingLoc(parentIncludeLoc);
          curBuf = sgr.getMemoryBuffer(curBufferIdx);
          curPtr = parentIncludeLoc.getPointer();

          // When current being parsed file encounter a EOF, attempt to
          // see if this is included from a another file, pop the including
          // file off buffer stack and continue parsing it if it is.
          // Otherwise this control flow should not be taken and just return
          // EOF.
          return getNextChar();
        }

        --curPtr;
        return EOF;
      }
      case '\n':
      case '\r': {
        if ((curBuf.getCharAt(curPtr) == '\n' || curBuf.getCharAt(curPtr) == '\r')
            && curBuf.getCharAt(curPtr) != curChar)
          ++curPtr;   // Eat the two char newline sequence.
        return '\n';
      }
    }
  }

  private void skipBCPLComment() {
    ++curPtr;
    while (true) {
      char ch = curBuf.getCharAt(curPtr);
      switch (ch) {
        case '\r':
        case '\n':
          return; // newline
        case 0:
          if (curPtr == curBuf.length())
            return;
          break;
      }
      // Otherwise, skip the character.
      ++curPtr;
    }
  }

  /**
   * This skips C-style comments.  The only difference from C
   * is that we allow nesting.
   *
   * @return
   */
  private boolean skipCComment() {
    ++curPtr;   // skip the star.

    int commentDepth = 1;
    while (true) {
      int curChar = getNextChar();
      switch (curChar) {
        case EOF:

          // Undefined character, emit an error.
          MemoryBuffer mb2 = curBuf.clone();
          mb2.setBufferStart(tokStart);
          Error.printError(mb2, "unterminated comment!");
          return true;
        case '*':
          if (curBuf.getCharAt(curPtr) != '/') break;

          ++curPtr;
          if (--commentDepth == 0)
            return false;
          break;
        case '/':
          if (curBuf.getCharAt(curPtr) != '*') break;
          ++curPtr;
          ++commentDepth;
          break;
      }
    }
  }

  private TokKind lexIdentifier() {
    int identStart = tokStart;

    char ch = curBuf.getCharAt(curPtr);
    while (Character.isJavaIdentifierPart(ch) || ch == '#') {
      if (ch == '#') {
        if (!curBuf.getSubString(curPtr, curPtr + 6).equals("#NAME#"))
          return TokKind.Error;
        curPtr += 6;
      } else {
        ++curPtr;
      }
      ch = curBuf.getCharAt(curPtr);
    }
    String id = curBuf.getSubString(identStart, curPtr);
    switch (id) {
      case "int":
        return TokKind.Int;
      case "bit":
        return TokKind.Bit;
      case "bits":
        return TokKind.Bits;
      case "string":
        return TokKind.String;
      case "list":
        return TokKind.List;
      case "code":
        return TokKind.Code;
      case "dag":
        return TokKind.Dag;
      case "class":
        return TokKind.Class;
      case "def":
        return TokKind.Def;
      case "defm":
        return TokKind.Defm;
      case "multiclass":
        return TokKind.Multiclass;
      case "field":
        return TokKind.Field;
      case "let":
        return TokKind.Let;
      case "in":
        return TokKind.In;
      case "include": {
        if (lexInclude()) return TokKind.Error;
        return lex();
      }
    }
    curStrVal = id;
    return TokKind.Id;
  }


  private boolean lexInclude() {
    TokKind tok = nextToken();
    if (tok == TokKind.Error) return true;
    if (tok != TokKind.StrVal) {
      Error.printError(getLoc(), "Expected filename after include");
      return true;
    }

    String filename = curStrVal;

    MemoryBuffer mb2 = curBuf.clone();
    mb2.setBufferStart(curPtr);
    if (!curBuf.isRegular() && Util.isAbsolutePath(filename))
      Util.assertion("Can not include relative file from non regular file");

    if (!Util.isAbsolutePath(filename)) {
      String base = curBuf.getFilename();
      int idx = base.lastIndexOf(File.separator);
      if (idx != -1)
        filename = base.substring(0, idx + 1) + filename;
    }

    curBufferIdx = sgr.addIncludeFile(filename, SMLoc.get(mb2));
    if (curBufferIdx == -1) {
      Error.printError(getLoc(), "Could not find include file '" + filename + "'");
      return true;
    }

    curBuf = sgr.getMemoryBuffer(curBufferIdx);
    curPtr = curBuf.getBufferStart();
    return false;
  }

  private TokKind lexString() {
    int strStart = curPtr;

    StringBuilder sb = new StringBuilder();
    char ch = curBuf.getCharAt(curPtr);
    while (ch != '"') {
      if (ch == 0 && curPtr == curBuf.length()) {
        MemoryBuffer mb2 = curBuf.clone();
        mb2.setBufferStart(strStart);
        return returnError(mb2, "End of file in string literal");
      }
      if (ch == '\n' || ch == '\r') {
        MemoryBuffer mb2 = curBuf.clone();
        mb2.setBufferStart(strStart);
        return returnError(mb2, "End of file in string literal");
      }
      if (ch != '\\') {
        sb.append(ch);
        ++curPtr;
        ch = curBuf.getCharAt(curPtr);
        continue;
      }
      ++curPtr;
      ch = curBuf.getCharAt(curPtr);
      switch (ch) {
        case '\\':
        case '\'':
        case '"':
          sb.append(ch);
          ++curPtr;
          ch = curBuf.getCharAt(curPtr);
          break;
        case 't':
          sb.append('\t');
          ++curPtr;
          ch = curBuf.getCharAt(curPtr);
          break;
        case 'n':
          sb.append('\n');
          ++curPtr;
          ch = curBuf.getCharAt(curPtr);
          break;

        case '\n':
        case '\r':
          MemoryBuffer mb2 = curBuf.clone();
          mb2.setBufferStart(curPtr);
          return returnError(mb2, "escaped newlines not supported in tblgen");

        case '\0':
          if (curPtr == curBuf.length()) {
            MemoryBuffer mb3 = curBuf.clone();
            mb3.setBufferStart(strStart);
            return returnError(mb3, "End of file in string literal");
          }
          // fall through
        default:
          MemoryBuffer mb3 = curBuf.clone();
          mb3.setBufferStart(curPtr);
          return returnError(mb3, "invalid escape in string literal");
      }
    }
    ++curPtr;
    curStrVal = sb.toString();
    return TokKind.StrVal;
  }

  private TokKind lexVarName() {
    char ch = curBuf.getCharAt(curPtr);
    if (!Character.isLetter(ch) && ch != '_') {
      MemoryBuffer mb2 = curBuf.clone();
      mb2.setBufferStart(tokStart);
      return returnError(mb2, "Invalid variable namespace");
    }

    int varnameStart = curPtr++;
    while (Character.isDigit(ch = curBuf.getCharAt(curPtr))
        || Character.isLetter(ch) || ch == '_') {
      ++curPtr;
    }

    curStrVal = curBuf.getSubString(varnameStart, curPtr);
    return TokKind.VarName;
  }

  /**
   * Number:: [-+]?[0-9]+
   * :: 0x[0-9a-fA-F]+
   * :: 0b[0-1]+
   *
   * @return
   */
  private TokKind lexNumber() {
    int prevChar = curBuf.getCharAt(curPtr - 1);
    if (prevChar == '0') {
      char curChar = curBuf.getCharAt(curPtr);
      if (curChar == 'x') {
        // hexdecimal number.
        ++curPtr;

        int numStart = curPtr;
        curChar = curBuf.getCharAt(curPtr);
        while (Character.isDigit(curChar)
            || (curChar >= 'a' && curChar <= 'f')
            || (curChar >= 'A' && curChar <= 'F')) {
          curChar = curBuf.getCharAt(++curPtr);
        }

        if (curPtr <= numStart) {
          MemoryBuffer mb2 = curBuf.clone();
          mb2.setBufferStart(tokStart);
          return returnError(mb2, "Invalid hexdecimal number!");
        }
        try {
          String t = curBuf.getSubString(numStart, curPtr);
          curIntVal = Long.parseUnsignedLong(t, 16);
          return TokKind.IntVal;
        } catch (NumberFormatException e) {
          MemoryBuffer mb2 = curBuf.clone();
          mb2.setBufferStart(tokStart);
          return returnError(mb2, "Invalid hexdecimal number!");
        }
      } else if (curChar == 'b') {
        // binary number.
        ++curPtr;

        int numStart = curPtr;
        curChar = curBuf.getCharAt(curPtr);
        while (curChar == '0' || curChar == '1') {
          curChar = curBuf.getCharAt(++curPtr);
        }

        if (curPtr <= numStart) {
          MemoryBuffer mb2 = curBuf.clone();
          mb2.setBufferStart(tokStart);
          return returnError(mb2, "Invalid binary number!");
        }
        try {
          curIntVal = Long.parseLong(curBuf.getSubString(numStart, curPtr), 2);
          return TokKind.IntVal;
        } catch (NumberFormatException e) {
          MemoryBuffer mb2 = curBuf.clone();
          mb2.setBufferStart(tokStart);
          return returnError(mb2, "Invalid binary number!");
        }
      }
    }
    // Check for a sign without a digit.
    if (!Character.isDigit(curBuf.getCharAt(curPtr))) {
      if (prevChar == '-')
        return TokKind.minus;
      else if (prevChar == '+')
        return TokKind.plus;
    }

    // leading '+' or '-'.
    while (Character.isDigit(curBuf.getCharAt(curPtr)))
      ++curPtr;
    try {
      curIntVal = Integer.parseInt(curBuf.getSubString(tokStart, curPtr));
      return TokKind.IntVal;
    } catch (NumberFormatException e) {
      MemoryBuffer mb2 = curBuf.clone();
      mb2.setBufferStart(tokStart);
      return returnError(mb2, "Invalid decimal number!");
    }
  }

  /**
   * We just read '['.  If this is a code block, return it,
   * otherwise return the bracket.  Match: '[' and '[{ ( [^}]+ | }[^]] )* }]'
   *
   * @return
   */
  private TokKind lexBracket() {
    if (curBuf.getCharAt(curPtr) != '{')
      return TokKind.l_square;
    ++curPtr;
    int codeStart = curPtr;
    while (true) {
      int ch = getNextChar();
      if (ch == EOF) break;
      if (ch != '}') continue;

      ch = getNextChar();
      if (ch == EOF) break;
      if (ch == ']') {
        curStrVal = curBuf.getSubString(codeStart, curPtr - 2);
        return TokKind.CodeFragment;
      }
    }
    MemoryBuffer mb2 = curBuf.clone();
    mb2.setBufferStart(codeStart - 2);
    return returnError(mb2, "Unterminated code block");
  }

  /**
   * Lex '!' and '![a-zA-Z]+'.
   *
   * @return
   */
  private TokKind lexExclaim() {
    int ch = curBuf.getCharAt(curPtr);
    if (!Character.isLetter(ch)) {
      MemoryBuffer mb2 = curBuf.clone();
      mb2.setBufferStart(curPtr - 1);
      return returnError(mb2, "Invalid \"!operator\"");
    }
    int start = curPtr++;
    while (Character.isLetter(curBuf.getCharAt(curPtr)))
      ++curPtr;

    int len = curPtr - start;
    String op = curBuf.getSubString(start, curPtr);
    switch (op) {
      case "con":
        return TokKind.XConcat;
      case "sra":
        return TokKind.XSRA;
      case "srl":
        return TokKind.XSRL;
      case "shl":
        return TokKind.XSHL;
      case "strconcat":
        return TokKind.XStrConcat;
      case "nameconcat":
        return TokKind.XNameConcat;
      case "subst":
        return TokKind.XSubst;
      case "foreach":
        return TokKind.XForEach;
      case "cast":
        return TokKind.XCast;
      case "car":
        return TokKind.XCar;
      case "cdr":
        return TokKind.XCdr;
      case "null":
        return TokKind.XNull;
      case "if":
        return TokKind.XIf;
      case "eq":
        return TokKind.XEq;
      case "ne":
        return TokKind.XNe;
      case "le":
        return TokKind.XLe;
      case "lt":
        return TokKind.XLt;
      case "gt":
        return TokKind.XGt;
      case "ge":
        return TokKind.XGe;
      case "add":
        return TokKind.XAdd;
      case "and":
        return TokKind.XAnd;
    }
    MemoryBuffer mb2 = curBuf.clone();
    mb2.setBufferStart(start - 1);
    return returnError(mb2, "Undefined operator");
  }

  /**
   * Return a binary integer with it's value in decimal and it's bits
   * in Pair.
   *
   * @return
   */
  /*public Pair<Long, Integer> getCurBinaryIntVal() {
    Util.assertion(getCode() == TokKind.BinaryIntVal);
    return Pair.get(curIntVal, curPtr - tokStart - 2);
  }*/
}
