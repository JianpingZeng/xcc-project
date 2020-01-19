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
import tools.APInt;
import tools.OutRef;
import tools.Util;

import static cfe.clex.LiteralSupport.processCharEscape;
import static cfe.diag.DiagnosticLexKindsTag.*;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class CharLiteralParser {
  private long value;
  private boolean isMultChar;
  private boolean hadError;

  public CharLiteralParser(char[] tokStr, SourceLocation loc, Preprocessor pp) {
    hadError = false;
    int pos = 0;
    Util.assertion(tokStr[pos] == '\'', "Invalid token lexed");
    ++pos;

    Util.assertion(pp.getTargetInfo().getCharWidth() == 8, "Asssumes char is 8bit");
    Util.assertion(pp.getTargetInfo().getIntWidth() <= 64 && (pp.getTargetInfo().getIntWidth() & 7) == 0,
        "Assumes sizeof(int) on target is <= 64 and a multiple of char");


    APInt litVal = new APInt(pp.getTargetInfo().getIntWidth(), 0);
    int numCharsSoFar = 0;
    while (tokStr[pos] != '\'') {
      char resultChar;
      if (tokStr[pos] != '\\')
        resultChar = tokStr[pos++];
      else {
        OutRef<Boolean> x = new OutRef<>(hadError);
        OutRef<Integer> y = new OutRef<>(pos);
        resultChar = processCharEscape(String.valueOf(tokStr), y, x, loc, pp);
        hadError = x.get();
        pos = y.get();
      }

      if (numCharsSoFar != 0) {
        if (litVal.countLeadingZeros() < 8)
          pp.diag(loc, warn_char_constant_too_large).emit();
        litVal.shlAssign(8);
      }
      litVal.assign(litVal.add(resultChar));
      ++numCharsSoFar;
    }

    // if this is teh second character being processed, do special handling.
    if (numCharsSoFar > 1) {
      if (numCharsSoFar != 4)
        pp.diag(loc, ext_multichar_character_literal).emit();
      else
        pp.diag(loc, ext_four_char_character_literal).emit();
      isMultChar = true;
    } else
      isMultChar = false;

    value = litVal.getZExtValue();
  }

  public CharLiteralParser(String tokStr, SourceLocation loc, Preprocessor pp) {
    this(tokStr.toCharArray(), loc, pp);
  }

  public long getValue() {
    return value;
  }

  public boolean isMultChar() {
    return isMultChar;
  }

  public boolean hadError() {
    return hadError;
  }
}
