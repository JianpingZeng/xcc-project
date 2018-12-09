/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package jlang.clex;

import jlang.basic.SourceManager;
import tools.Util;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class TokenConcatenation {
  private Preprocessor pp;

  /// By default, a token never needs to avoid concatenation.  Most tokens
  /// (e.g. ',', ')', etc) don't cause a problem when concatenated.
  public final static int aci_never_avoid_concat = 0;

  /// aci_custom_firstchar - avoidConcat contains custom code to handle this
  /// token's requirements, and it needs to know the first character of the
  /// token.
  public final static int aci_custom_firstchar = 1;

  /// aci_custom - avoidConcat contains custom code to handle this token's
  /// requirements, but it doesn't need to know the first character of the
  /// token.
  public final static int aci_custom = 2;

  /// aci_avoid_equal - Many tokens cannot be safely followed by an '='
  /// character.  For example, "<<" turns into "<<=" when followed by an =.
  public final static int aci_avoid_equal = 3;


  /// tokenInfo - This array contains information for each token on what
  /// action to take when avoiding concatenation of tokens in the avoidConcat
  /// method.
  private int[] tokenInfo;

  public TokenConcatenation(Preprocessor PP) {
    tokenInfo = new int[TokenKind.values().length];
    // These tokens have custom code in avoidConcat.
    tokenInfo[TokenKind.identifier.tokenID] |= aci_custom;
    tokenInfo[TokenKind.numeric_constant.tokenID] |= aci_custom_firstchar;
    tokenInfo[TokenKind.dot.tokenID] |= aci_custom_firstchar;
    tokenInfo[TokenKind.amp.tokenID] |= aci_custom_firstchar;
    tokenInfo[TokenKind.plus.tokenID] |= aci_custom_firstchar;
    tokenInfo[TokenKind.sub.tokenID] |= aci_custom_firstchar;
    tokenInfo[TokenKind.slash.tokenID] |= aci_custom_firstchar;
    tokenInfo[TokenKind.less.tokenID] |= aci_custom_firstchar;
    tokenInfo[TokenKind.greater.tokenID] |= aci_custom_firstchar;
    tokenInfo[TokenKind.bar.tokenID] |= aci_custom_firstchar;
    tokenInfo[TokenKind.percent.tokenID] |= aci_custom_firstchar;
    tokenInfo[TokenKind.colon.tokenID] |= aci_custom_firstchar;
    tokenInfo[TokenKind.hash.tokenID] |= aci_custom_firstchar;
    tokenInfo[TokenKind.arrow.tokenID] |= aci_custom_firstchar;

    // These tokens change behavior if followed by an '='.
    tokenInfo[TokenKind.amp.tokenID] |= aci_avoid_equal;           // &=
    tokenInfo[TokenKind.plus.tokenID] |= aci_avoid_equal;           // +=
    tokenInfo[TokenKind.sub.tokenID] |= aci_avoid_equal;           // -=
    tokenInfo[TokenKind.slash.tokenID] |= aci_avoid_equal;           // /=
    tokenInfo[TokenKind.less.tokenID] |= aci_avoid_equal;           // <=
    tokenInfo[TokenKind.greater.tokenID] |= aci_avoid_equal;           // >=
    tokenInfo[TokenKind.bar.tokenID] |= aci_avoid_equal;           // |=
    tokenInfo[TokenKind.percent.tokenID] |= aci_avoid_equal;           // %=
    tokenInfo[TokenKind.star.tokenID] |= aci_avoid_equal;           // *=
    tokenInfo[TokenKind.bang.tokenID] |= aci_avoid_equal;           // !=
    tokenInfo[TokenKind.lessless.tokenID] |= aci_avoid_equal;           // <<=
    tokenInfo[TokenKind.greaterequal.tokenID] |= aci_avoid_equal;           // >>=
    tokenInfo[TokenKind.caret.tokenID] |= aci_avoid_equal;           // ^=
    tokenInfo[TokenKind.equal.tokenID] |= aci_avoid_equal;           // ==
  }

  /**
   * Return the first character of the {@code tok}, avoding calls to
   * getSpelling where possible.
   *
   * @param pp
   * @param tok
   * @return
   */
  private static char getFirstChar(Preprocessor pp, Token tok) {
    IdentifierInfo ii = tok.getIdentifierInfo();
    if (ii != null) {
      return ii.getName().charAt(0);
    } else if (!tok.needsCleaning()) {
      if (tok.isLiteral() && tok.getLiteralData() != null) {
        return tok.getLiteralData().buffer[tok.getLiteralData().offset];
      } else {
        SourceManager sm = pp.getSourceManager();
        StrData str = sm.getCharacterData(sm.getLiteralLoc(tok.getLocation()));
        return str.buffer[str.offset];
      }
    } else {
      return pp.getSpelling(tok).charAt(0);
    }
  }

  public boolean avoidConcat(Token prevTok, Token tok) {
    if (prevTok.getLocation().isFileID() && tok.getLocation().isFileID()
        && prevTok.getLocation().getFileLocWithOffset(prevTok.getLength())
        == tok.getLocation())
      return false;

    TokenKind prevKind = prevTok.getKind();
    if (prevTok.getIdentifierInfo() != null)
      prevKind = TokenKind.identifier;

    int concatInfo = tokenInfo[prevKind.tokenID];

    if (concatInfo == 0)
      return false;

    if ((concatInfo & aci_avoid_equal) != 0) {
      if (tok.is(TokenKind.equal) || tok.is(TokenKind.equalequal))
        return true;
      concatInfo &= ~aci_avoid_equal;
    }

    if (concatInfo == 0)
      return false;

    // Basic algorithm: we look at the first character of the second token, and
    // determine whether it, if appended to the first token, would form (or
    // would contribute) to a larger token if concatenated.
    char firstChar = 0;
    if ((concatInfo & aci_custom) != 0) {
      // If the token does not need to know the first character, don't get it.
    } else {
      firstChar = getFirstChar(pp, tok);
    }

    switch (prevKind) {
      default:
        Util.assertion(false, "InitAvoidConcatTokenInfo built wrong");
      case identifier:
        // id+id or id+number or id+L"foo".
        if (tok.is(TokenKind.numeric_constant))
          return getFirstChar(pp, tok) != '.';
        if (tok.getIdentifierInfo() != null)
          return true;

        if (tok.isNot(TokenKind.char_constant) && tok.isNot(TokenKind.string_literal))
          return false;

        // If the string was a wide string L"foo" or wide char L'f', it would
        // concat with the previous identifier into fooL"bar".  Avoid this.
        if (startsWithL(tok))
          return true;

        return isIdentifierL(prevTok);
      case numeric_constant:
        return Character.isDigit(firstChar) || Character.isLetter(firstChar)
            || tok.is(TokenKind.numeric_constant) ||
            firstChar == '+' || firstChar == '-' || firstChar == '.';
      case dot:
        return firstChar == '.' || Character.isDigit(firstChar);
      case amp:
        return firstChar == '&';    // &&
      case plus:
        return firstChar == '+';    // ++
      case sub:
        return firstChar == '-' || firstChar == '>';    // --, ->, ->*
      case slash: //, /*, //
        return firstChar == '/' || firstChar == '*';
      case less:
        return firstChar == '<' || firstChar == ':' || firstChar == '%';
      case greater:   // >>, >>=
        return firstChar == '>';
      case bang:  // ||
        return firstChar == '|';
      case percent:    // %>, %:
        return firstChar == '>' || firstChar == ':';
      case colon:     // :>
        return firstChar == '>';
      case hash:      // ##, #@, %:%:
        return firstChar == '#' || firstChar == '@' || firstChar == '%';
    }
  }

  /**
   * Return true if the spelling of this token starts with 'L'.
   *
   * @param tok
   * @return
   */
  private boolean startsWithL(Token tok) {
    if (!tok.needsCleaning()) {
      SourceManager sm = pp.getSourceManager();
      StrData data = sm.getCharacterData(tok.getLocation());
      return data.buffer[data.offset] == 'L';
    }

    return pp.getSpelling(tok).charAt(0) == 'L';
  }

  /**
   * Return true if the spelling of this token is literally 'L'.
   *
   * @param tok
   * @return
   */
  private boolean isIdentifierL(Token tok) {
    if (!tok.needsCleaning()) {
      if (tok.getLength() != 1)
        return false;

      SourceManager sm = pp.getSourceManager();
      StrData data = sm.getCharacterData(tok.getLocation());
      return data.buffer[data.offset] == 'L';
    }

    return pp.getSpelling(tok).equals("L");
  }
}
