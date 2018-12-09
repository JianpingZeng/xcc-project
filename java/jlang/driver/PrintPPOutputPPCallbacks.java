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

package jlang.driver;

import jlang.basic.SourceManager;
import jlang.clex.*;
import jlang.support.CharacteristicKind;
import jlang.support.SourceLocation;

import java.io.PrintStream;

import static jlang.driver.JlangCC.printMacroDefinition;
import static tools.TextUtils.isPrintable;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class PrintPPOutputPPCallbacks implements PPCallBack {
  private Preprocessor pp;
  private TokenConcatenation concatInfo;
  private int curLine;
  private boolean emittedTokensOnThisLine;
  private boolean emittedMacroOnThisLine;
  private CharacteristicKind fileType;
  private String curFilename;
  private boolean initialized;
  private boolean disableLineMarkers;
  private boolean dumpDefines;
  public PrintStream os;

  public PrintPPOutputPPCallbacks(Preprocessor pp, PrintStream os,
                                  boolean linemarkers, boolean defines) {
    this.pp = pp;
    concatInfo = new TokenConcatenation(pp);
    this.os = os;
    disableLineMarkers = linemarkers;
    dumpDefines = defines;
    curLine = 0;
    curFilename = "<uninit>";
    emittedMacroOnThisLine = false;
    emittedTokensOnThisLine = false;
    fileType = CharacteristicKind.C_User;
    initialized = false;
  }

  public void setEmittedTokensOnThisLine() {
    emittedTokensOnThisLine = true;
  }

  public boolean hasEmittedTokensOnThisLine() {
    return emittedTokensOnThisLine;
  }

  @Override
  public void fileChanged(SourceLocation loc, FileChangeReason reason,
                          CharacteristicKind kind) {
    SourceManager sm = pp.getSourceManager();
    if (reason == FileChangeReason.EnterFile) {
      SourceLocation includeLoc = sm.getPresumedLoc(loc).getIncludeLoc();
      if (includeLoc.isValid())
        moveToLine(includeLoc);
    } else if (reason == FileChangeReason.SystemHeaderPragma) {
      moveToLine(loc);

      // TODO GCC emits the # directive for this directive on the line AFTER the
      // directive and emits a bunch of spaces that aren't needed.  Emulate this
      // strange behavior.
    }

    loc = sm.getInstantiationLoc(loc);
    curLine = sm.getInstantiationLineNumber(loc);

    if (disableLineMarkers)
      return;

    curFilename = "";
    curFilename += sm.getPresumedLoc(loc).getFilename();
    curFilename = Lexer.stringify(curFilename);
    fileType = kind;

    if (!initialized) {
      writeLineInfo(curLine);
      initialized = true;
    }

    switch (reason) {
      case EnterFile:
        writeLineInfo(curLine, " 1");
        break;
      case ExitFile:
        writeLineInfo(curLine, " 2");
        break;
      case SystemHeaderPragma:
      case RenameFile:
        writeLineInfo(curLine);
        break;
    }
  }

  @Override
  public void ident(SourceLocation loc, String str) {
    moveToLine(loc);
    os.printf("#ident ");
    os.print(str);
    emittedTokensOnThisLine = true;
  }

  @Override
  public void pragmaComment(SourceLocation loc, IdentifierInfo kind,
                            String str) {
    moveToLine(loc);
    os.printf("#pragma comment(%s", kind.getName());

    if (!str.isEmpty()) {
      os.print(", \"");
      for (int i = 0, e = str.length(); i != e; i++) {
        char ch = str.charAt(i);
        if (isPrintable(ch) && ch != '\\' && ch != '"')
          os.print(ch);
        else {
          // Output anything hard as an octal escape.
          os.print('\\');
          os.print('0' + (ch >> 6) & 7);
          os.print('0' + (ch >> 3) & 7);
          os.print('0' + (ch >> 0) & 7);
        }
      }
      os.print('"');
    }

    os.print(')');
    emittedTokensOnThisLine = true;
  }

  @Override
  public void macroExpands(Token id, MacroInfo mi) {
  }

  /**
   * This hook is called whenever a macro definition is seen.
   *
   * @param ii
   * @param mi
   */
  @Override
  public void macroDefined(IdentifierInfo ii, MacroInfo mi) {
    if (!dumpDefines ||
        // Ignores __FILE__ etc.
        mi.isBuiltinMacro())
      return;

    moveToLine(mi.getDefinitionLoc());
    printMacroDefinition(ii, mi, pp, os);
    emittedMacroOnThisLine = true;
  }

  @Override
  public void macroUndefined(IdentifierInfo ii, MacroInfo mi) {
  }

  /**
   * When emitting a preprocessed file in -E mode, this
   * is called for the first token on each new line.  If this really is the start
   * of a new logical line, handle it and return true, otherwise return false.
   * This may not be the start of a logical line because the "start of line"
   * marker is set for spelling lines, not instantiation ones.
   *
   * @param tok
   * @return
   */
  public boolean handleFirstTokenOnLine(Token tok) {
    if (!moveToLine(tok.getLocation()))
      return false;

    SourceManager sm = pp.getSourceManager();
    int colNo = sm.getInstantiationColumnNumber(tok.getLocation());

    // This hack prevents stuff like:
    // #define HASH #
    // HASH define foo bar
    // From having the # character end up at column 1, which makes it so it
    // is not handled as a #define next time through the preprocessor if in
    // -fpreprocessed mode.
    if (colNo <= 1 && tok.is(TokenKind.hash))
      os.print(' ');

    for (; colNo > 1; --colNo)
      os.print(' ');

    return true;
  }

  /**
   * Move the output to the source line specified by the location
   * object.  We can do this by emitting some number of \n's, or be emitting a
   * #line directive.  This returns false if already at the specified line, true
   * if some newlines were emitted.
   *
   * @param loc
   * @return
   */
  public boolean moveToLine(SourceLocation loc) {
    int lineNo = pp.getSourceManager().getInstantiationLineNumber(loc);

    if (disableLineMarkers) {
      if (lineNo == curLine)
        return false;

      curLine = lineNo;

      if (!emittedTokensOnThisLine && !emittedMacroOnThisLine)
        return true;

      os.println();
      emittedMacroOnThisLine = false;
      emittedTokensOnThisLine = false;
      return true;
    }

    // If this line is "close enough" to the original line, just print newlines,
    // otherwise print a #line directive.
    if (lineNo - curLine <= 8) {
      if (lineNo - curLine == 1)
        os.println();
      else if (curLine == lineNo)
        return false;   // Spelling line moved, but instantiation line didn't.
      else {
        String newLine = "\n\n\n\n\n\n\n\n";
        os.print(newLine);
      }
    } else {
      writeLineInfo(lineNo);
    }

    curLine = lineNo;
    return true;
  }

  public boolean avoidConcat(Token prevTok, Token tok) {
    return concatInfo.avoidConcat(prevTok, tok);
  }

  public void writeLineInfo(int lineNo) {
    writeLineInfo(lineNo, "");
  }

  public void writeLineInfo(int lineNo, String extra) {
    if (emittedTokensOnThisLine || emittedMacroOnThisLine) {
      os.println();
      emittedMacroOnThisLine = false;
      emittedTokensOnThisLine = false;
    }

    os.printf("# %d \"", lineNo);
    os.print(curFilename);
    os.print('"');

    if (extra != null) {
      os.print(extra);
    }
    if (fileType == CharacteristicKind.C_System) {
      os.print(" 3");
    }
    os.println();
  }

  public void handleNewLinesInToken(String tokStr) {
    int numNewLines = 0;
    int len = tokStr.length();
    int i = 0;
    for (; len != 0; ++i, --len) {
      if (tokStr.charAt(i) != '\n' && tokStr.charAt(i) != '\r')
        continue;
      ++numNewLines;

      // If we have \n\r or \r\n, skip both and count as one line.
      if (len != 1 &&
          (tokStr.charAt(i + 1) == '\n' || tokStr.charAt(i + 1) == '\r')
          && (tokStr.charAt(i) != tokStr.charAt(i + 1))) {
        ++i;
        --len;
      }
    }
    if (numNewLines == 0)
      return;
    curLine += numNewLines;
  }
}
