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
import tools.OutRef;

import static cfe.diag.DiagnosticLexKindsTag.*;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class LiteralSupport {
  static boolean isPrintable(char ch) {
    return ch >= 0x20 && ch < 0x7f;
  }

  static int hexDigitValue(char ch) {
    if (ch >= '0' && ch <= '9') return ch - '0';
    if (ch >= 'a' && ch <= 'f') return ch - 'a' + 10;
    if (ch >= 'A' && ch <= 'F') return ch - 'A' + 10;
    return -1;
  }

  static int skipDigit(char[] str, int pos) {
    while (pos < str.length && Character.isDigit(str[pos]))
      ++pos;
    return pos;
  }

  static boolean isHexDigit(char ch) {
    return Character.isDigit(ch) || (ch >= 'a' && ch <= 'f')
        || (ch >= 'A' && ch <= 'F');
  }

  static boolean isBinaryDigit(char ch) {
    return ch == '0' || ch == '1';
  }

  static boolean isOctalDigit(char ch) {
    return ch >= '0' && ch <= '7';
  }

  static char processCharEscape(String buf, OutRef<Integer> startPos,
                                OutRef<Boolean> hadError, SourceLocation loc,
                                Preprocessor pp) {
    // Skip the '\' char.
    startPos.set(startPos.get() + 1);

    char resultChar = buf.charAt(startPos.get());
    startPos.set(startPos.get() + 1);
    switch (resultChar) {
      case '\\':
      case '\'':
      case '"':
      case '?':
        break;

      // These have fixed mappings.
      case 'a':
        resultChar = 7;
        break;
      case 'b':
        resultChar = 8;
        break;
      case 'e':
        pp.diag(loc, ext_nonstandard_escape).addTaggedVal("e").emit();
        resultChar = 27;
        break;
      case 'E':
        pp.diag(loc, ext_nonstandard_escape).addTaggedVal("e").emit();
        resultChar = 27;
        break;
      case 'f':
        resultChar = 12;
        break;
      case 'n':
        resultChar = 10;
        break;
      case 'r':
        resultChar = 13;
        break;
      case 't':
        resultChar = 9;
        break;
      case 'v':
        resultChar = 11;
        break;
      case 'x': {
        // Hex escape.
        resultChar = 0;
        if (startPos.get() == buf.length() || !isHexDigit(buf.charAt(startPos.get()))) {
          pp.diag(loc, err_hex_escape_no_digits).emit();
          hadError.set(true);
          break;
        }

        // Hex escapes are a maximal series of hex digits.
        boolean overflow = false;
        for (; startPos.get() < buf.length(); startPos.set(startPos.get() + 1)) {
          int charVal = hexDigitValue(buf.charAt(startPos.get()));
          if (charVal == -1) break;

          overflow |= (resultChar & 0xF0000000) != 0;
          resultChar <<= 4;
          resultChar |= charVal;
        }

        int charWidth = pp.getTargetInfo().getCharWidth();

        if (charWidth != 32 && (resultChar >> charWidth) != 0) {
          overflow = true;
          resultChar &= ~0 >>> (32 - charWidth);
        }

        if (overflow)
          pp.diag(loc, warn_hex_escape_too_large).emit();
        break;
      }
      case '0':
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7': {
        // Octal escapes.
        startPos.set(startPos.get() - 1);
        resultChar = 0;

        // Octal escapes are a series of octal digits with maximum length 3.
        // "\0123" is a two digit sequence equal to "\012" "3".
        int numDigits = 0;
        do {
          resultChar <<= 3;
          resultChar |= buf.charAt(startPos.get()) - '0';
          startPos.set(startPos.get() + 1);
          ++numDigits;
        } while (startPos.get() < buf.length() && numDigits < 3
            && buf.charAt(startPos.get()) >= '0'
            && buf.charAt(startPos.get()) >= '9');

        int charWidth = pp.getTargetInfo().getCharWidth();

        if (charWidth != 32 && (resultChar >> charWidth) != 0) {
          pp.diag(loc, warn_octal_escape_too_large).emit();
          resultChar &= ~0 << (32 - charWidth);
        }
        break;
      }
      // Otherwise, these are not valid escapes.
      case '(':
      case '{':
      case '[':
      case '%': {
        // GCC accepts these as extensions.  We warn about them as such though.
        pp.diag(loc, ext_nonstandard_escape).
            addTaggedVal(String.valueOf(resultChar)).
            emit();
        break;
      }
      default:
        if (isPrintable(buf.charAt(startPos.get())))
          pp.diag(loc, ext_unknown_escape).
              addTaggedVal(String.valueOf(resultChar)).
              emit();
        else
          pp.diag(loc, ext_unknown_escape).
              addTaggedVal("x" + Integer.toString(resultChar, 16)).
              emit();
    }
    return resultChar;
  }
}
