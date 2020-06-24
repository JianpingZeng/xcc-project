package cfe.clex;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2020, Jianping Zeng.
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

import cfe.support.SourceLocation;
import tools.Util;

import java.util.ArrayList;
import java.util.Arrays;

import static cfe.clex.TokenKind.*;
import static cfe.diag.DiagnosticLexKindsTag.pp_invalid_string_literal;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class MacroArgs {
  /// The number of raw, unexpanded tokens for the
  /// arguments.  All of the actual argument tokens are allocated immediately
  /// after the MacroArgs object in memory.  This is all of the arguments
  /// concatenated together, with 'EOF' markers at the end of each argument.
  private Token[] unexpandedArgTokens;

  /// Pre-expanded tokens for arguments that need them.  Empty
  /// if not yet computed.  This includes the EOF marker at the end of the
  /// stream.
  private ArrayList<ArrayList<Token>> preExpArgTokens;

  /// This contains arguments in 'stringified' form.  If the
  /// stringified form of an argument has not yet been computed, this is empty.
  private ArrayList<Token> stringifiedArgs;

  /// True if this is a C99 style varargs macro invocation and
  /// there was no argument specified for the "..." argument.  If the argument
  /// was specified (even empty) or this isn't a C99 style varargs function, or
  /// if in strict mode and the C99 varargs macro had only a ... argument, this
  /// is false.
  private boolean varargsElided;

  private MacroArgs(int numToks, boolean varargsElided) {
    unexpandedArgTokens = new Token[numToks];
    preExpArgTokens = new ArrayList<>();
    stringifiedArgs = new ArrayList<>();
    this.varargsElided = varargsElided;
  }

  /// Create a new MacroArgs object with the specified
  /// macro and argument info.
  public static MacroArgs create(
      MacroInfo mi,
      Token[] unexpArgTokens,
      boolean varargsElided) {
    Util.assertion(mi.isFunctionLike(), "Can't have args for ab object-like macro!");

    MacroArgs result = new MacroArgs(unexpArgTokens.length, varargsElided);
    System.arraycopy(unexpArgTokens, 0,
        result.unexpandedArgTokens, 0,
        unexpArgTokens.length);
    return result;
  }

  /// If we can prove that the argument won't be affected
  /// by pre-expansion, return false.  Otherwise, conservatively return true.
  public boolean argNeedsPreexpansion(Token[] argToks, Preprocessor pp) {
    for (int i = 0, e = argToks.length; i < e && argToks[i].isNot(eof); i++) {
      IdentifierInfo ii = argToks[i].getIdentifierInfo();
      if (ii != null) {
        if (ii.isHasMacroDefinition() && pp.getMacroInfo(ii).isEnabled())
          return true;
      }
    }
    return false;
  }

  /// Return a pointer to the first token of the unexpanded
  /// token list for the specified formal.
  ///
  public Token[] getUnexpandedArgument(int arg) {
    Util.assertion(arg >= 0);
    int lastEOF = -1;
    int end = 0;
    int e = unexpandedArgTokens.length;
    for (int i = 0; i < e; i++) {
      if (unexpandedArgTokens[i].is(eof)) {
        if (arg == 0) {
          end = i;
          break;
        }
        --arg;
        lastEOF = i;
      }
    }
    Util.assertion(lastEOF < end && end <= e, "Illegal macro arguments tokenized!");
    return Arrays.copyOfRange(unexpandedArgTokens, lastEOF + 1, end + 1);
  }

  /// Given a pointer to an expanded or unexpanded argument,
  /// return the number of tokens, not counting the EOF, that make up the
  /// argument.
  public static int getArgLength(Token[] argPtr) {
    int numTokens = 0;
    for (int i = 0, e = argPtr.length; i < e && argPtr[i].isNot(eof); ++i)
      ++numTokens;
    return numTokens;
  }

  /// Return the pre-expanded form of the specified
  /// argument.
  public ArrayList<Token> getPreExpArgument(int arg, Preprocessor pp) {
    Util.assertion(arg >= 0 && arg < getNumArguments(), "Invalid argument number!");

    if (preExpArgTokens.isEmpty()) {
      for (int n = getNumArguments(); n != 0; --n)
        preExpArgTokens.add(new ArrayList<>());
    }

    ArrayList<Token> result = preExpArgTokens.get(arg);

    if (!result.isEmpty()) return result;

    Token[] ai = getUnexpandedArgument(arg);
        /*
        Token[] newAI = new Token[getArgLength(ai) + 1];
        System.arraycopy(ai, 0, newAI, 0, ai.length);
        newAI[newAI.length - 1] = ai[ai.length];
        */
    pp.enterTokenStream(ai,
        false /*disable expand*/,
        false /*owns tokens*/);

    do {
      Token t = new Token();
      result.add(t);
      pp.lex(t);
    } while (result.get(result.size() - 1).isNot(eof));

    pp.removeTopOfLexerStack();
    return result;
  }

  /// Compute, cache, and return the specified argument
  /// that has been 'stringified' as required by the # operator.
  public Token getStringifiedArgument(int argNo, Preprocessor pp) {
    Util.assertion(argNo >= 0 && argNo < getNumArguments(), "Invalid argument number!");

    if (stringifiedArgs.isEmpty()) {
      for (int i = 0; i < getNumArguments(); i++)
        stringifiedArgs.add(null);
    }

    if (stringifiedArgs.get(argNo) == null ||
        stringifiedArgs.get(argNo).isNot(string_literal))
      stringifiedArgs.set(argNo, stringifyArgument(getUnexpandedArgument(argNo), pp));
    return stringifiedArgs.get(argNo);
  }

  /// Return the number of arguments passed into this macro
  /// invocation.
  public int getNumArguments() {
    return unexpandedArgTokens.length;
  }

  /// isVarargsElidedUse - Return true if this is a C99 style varargs macro
  /// invocation and there was no argument specified for the "..." argument.  If
  /// the argument was specified (even empty) or this isn't a C99 style varargs
  /// function, or if in strict mode and the C99 varargs macro had only a ...
  /// argument, this returns false.
  public boolean isVarargsElidedUse() {
    return varargsElided;
  }

  /**
   * Implement C99 6.10.3.2p2, converting a sequence of
   * tokens into the literal string token that should be produced by the C #
   * preprocessor operator.
   *
   * @param argToks
   * @param pp
   * @return
   */
  public static Token stringifyArgument(Token[] argToks, Preprocessor pp) {
    Token tok = new Token();
    tok.startToken();
    tok.setKind(string_literal);

    int i = 0;
    StringBuilder sb = new StringBuilder();
    sb.append('"');

    boolean isFirst = true;
    for (; argToks[i].isNot(eof); i++) {
      Token tmp = argToks[i];

      // Handle the non-first token.
      if (!isFirst && (tok.hasLeadingSpace() || tok.isAtStartOfLine())) {
        sb.append(' ');
      }
      isFirst = false;

      // If this is a string or character constant, escape the token as specified
      // by 6.10.3.2p2.
      if (tmp.is(string_literal) ||   // string literal, "foo"
          tmp.is(char_constant))  // char literal
      {
        String str = Lexer.stringify(pp.getSpelling(tmp));
        sb.append(str);
      } else {
        // Otherwise, just append the token.  Do some gymnastics to get the token
        // in place and avoid copies where possible.
        sb.append(pp.getSpelling(tmp));
      }
    }

    // If the last character of the string is a \, and if it isn't escaped, this
    // is an invalid string literal, diagnose it as specified in C99.
    if (sb.charAt(sb.length() - 1) == '\\') {
      int firstNonSlash = sb.length() - 2;
      while (sb.charAt(firstNonSlash) == '\\')
        --firstNonSlash;

      if ((sb.length() - 1 - firstNonSlash & 1) != 0) {
        pp.diag(argToks[i - 1], pp_invalid_string_literal).emit();
        sb.deleteCharAt(sb.length() - 1);
      }
    }

    sb.append('"');

    pp.createString(sb.toString(), tok, new SourceLocation());
    return tok;
  }
}
