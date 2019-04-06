package cfe.diag;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import cfe.basic.SourceManager;
import cfe.clex.Lexer;
import cfe.support.*;
import tools.Colors;
import tools.FormattedOutputStream;
import tools.Pair;
import tools.Util;

import java.io.PrintStream;
import java.util.Arrays;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public final class TextDiagnosticPrinter implements DiagnosticClient {
  private static final Colors CaretColor = Colors.GREEN;
  private static final Colors noteColor = Colors.CYAN;
  private static final Colors fixItColor = Colors.GREEN;
  private static final Colors warnColor = Colors.MAGENTA;
  private static final Colors errorColor = Colors.RED;
  private static final Colors fatalColor = Colors.RED;
  private static final Colors SavedColor = Colors.SAVEDCOLOR;


  private FormattedOutputStream os;
  private LangOptions langOpts;
  private SourceLocation lastWarningLoc;
  private FullSourceLoc lastLoc;
  private boolean lastCaretDiagnosticWasNote;

  private boolean showColumn;
  private boolean caretDiagnostics;
  private boolean showLocation;
  private boolean printRangeInfo;
  private boolean printDiagnosticOption;
  private boolean printFixItInfo;
  private int messageLength;
  private boolean useColors;

  /**
   * Number of spaces to indent when word-wrapping.
   */
  private final static int wordWrapIndentation = 6;

  public TextDiagnosticPrinter(PrintStream os) {
    this(os, true, true, true, true, true, true, 0, false);
  }

  public TextDiagnosticPrinter(PrintStream os, boolean showColumn,
                               boolean caretDiagnostics, boolean showLocation,
                               boolean printRangeInfo, boolean printDiagnosticOption,
                               boolean printFixItInfo, int messageLength, boolean useColors) {
    this.os = new FormattedOutputStream(os);
    lastWarningLoc = SourceLocation.NOPOS;
    lastLoc = new FullSourceLoc();
    this.lastCaretDiagnosticWasNote = false;
    this.showColumn = showColumn;
    this.caretDiagnostics = caretDiagnostics;
    this.showLocation = showLocation;
    this.printRangeInfo = printRangeInfo;
    this.printDiagnosticOption = printDiagnosticOption;
    this.printFixItInfo = printFixItInfo;
    this.messageLength = messageLength;
    this.useColors = useColors;
  }

  @Override
  public void setLangOptions(LangOptions langOptions) {
    this.langOpts = langOptions;
  }

  public LangOptions getLangOpts() {
    return langOpts;
  }

  public void printIncludeStack(SourceLocation loc, SourceManager sgr) {
    if (!loc.isValid()) return;
    PresumedLoc ploc = sgr.getPresumedLoc(loc);
    printIncludeStack(ploc.getIncludeLoc(), sgr);

    if (showLocation) {
      os.printf("In file included from %s:%d:\n",
          ploc.getFilename(),
          ploc.getLine());
    } else
      os.println("In included file:");
  }

  /**
   * Highlight (with ~'s) the given source range in the source file.
   *
   * @param range
   * @param sgr
   * @param lineNo
   * @param fid
   * @param caretLine
   * @param sourceLine
   */
  public void highlightRange(
      SourceRange range,
      SourceManager sgr,
      int lineNo,
      FileID fid,
      StringBuilder caretLine,
      String sourceLine) {
    Util.assertion(caretLine.length() == sourceLine.length(), "Expect a correspondence between source and caret line");

    if (!range.isValid())
      return;

    SourceLocation begin = sgr.getInstantiationLoc(range.getBegin());
    SourceLocation end = sgr.getInstantiationLoc(range.getEnd());

    if (begin.equals(end) && range.getEnd().isMacroID()) {
      end = sgr.getInstantiationRange(range.getEnd()).getEnd();
    }

    int startLineNo = sgr.getInstantiationLineNumber(begin);
    if (startLineNo > lineNo || !sgr.getFileID(begin).equals(fid))
      return; // No intersection.

    int endLineNo = sgr.getInstantiationLineNumber(end);
    if (endLineNo < lineNo || !sgr.getFileID(end).equals(fid))
      return; // No intersection.

    // Compute the column number of the start.
    int startColNo = 0;
    if (startLineNo == lineNo) {
      startColNo = sgr.getInstantiationColumnNumber(begin);
      if (startColNo != 0)
        --startColNo;   // Zero base the col #.
    }

    // Pick the first non-whitespace column.
    while (startColNo < sourceLine.length()
        && (sourceLine.charAt(startColNo) == ' '
        || sourceLine.charAt(startColNo) == '\t')) {
      ++startColNo;
    }

    int endColNo = caretLine.length();
    if (endLineNo == lineNo) {
      endColNo = sgr.getInstantiationColumnNumber(end);
      if (endColNo != 0) {
        --endColNo; //Zero based the col #.

        // Add in the length of the token, so that we cover multi-char tokens.
        endColNo += Lexer.measureTokenLength(end, sgr, langOpts);
      } else
        endColNo = caretLine.length();
    }

    // Pick the last non-whitespace column.
    if (endColNo < sourceLine.length())
      while (endColNo != 0 && (sourceLine.charAt(endColNo) == ' '
          || (sourceLine.charAt(endColNo) == '\t')))
        --endColNo;
    else
      endColNo = sourceLine.length();

    Util.assertion(startColNo <= endColNo, "Invalid range!");
    for (int i = startColNo; i < endColNo; ++i)
      caretLine.setCharAt(i, '~');
  }

  public void emitCaretDiagnostic(
      SourceLocation loc,
      SourceRange[] ranges,
      SourceManager sgr,
      FixItHint[] hints,
      int columns) {
    Util.assertion(loc.isValid(), "must have a valid location");

    if (!loc.isFileID()) {
      SourceLocation oneLevelUp = sgr.getImmediateInstantiationRange(loc).getBegin();
      emitCaretDiagnostic(oneLevelUp, ranges, sgr, null, columns);

      loc = sgr.getImmediateLiteralLoc(loc);

      for (int i = 0, size = ranges.length; i < size; i++) {
        SourceRange r = ranges[i];
        SourceLocation s = r.getBegin();
        SourceLocation e = r.getEnd();
        if (s.isMacroID())
          s = sgr.getImmediateLiteralLoc(s);
        if (e.isMacroID())
          e = sgr.getImmediateLiteralLoc(e);
        ranges[i] = new SourceRange(s, e);
      }

      if (showLocation) {
        if (useColors)
          os.changeColor(Colors.SAVEDCOLOR, true);
        Pair<FileID, Integer> locInfo = sgr.getDecomposedInstantiationLoc(loc);

        os.printf("%s:%d:", sgr.getBuffer(locInfo.first).getBufferIdentifier(),
            sgr.getLineNumber(locInfo.first, locInfo.second));
        if (showColumn)
          os.printf("%d:", sgr.getColumnNumber(locInfo.first, locInfo.second));
        os.print(' ');
      }
      if (useColors)
        os.resetColor();
      os.println("note: instantiated from:");

      emitCaretDiagnostic(loc, ranges, sgr, hints, columns);
      return;
    }

    // Decompose the location into a FID/Offset pair.
    Pair<FileID, Integer> locInfo = sgr.getDecomposedLoc(loc);
    FileID fid = locInfo.first;
    int fileOffset = locInfo.second;

    char[] strData = sgr.getBufferData(fid);
    int colNo = sgr.getColumnNumber(fid, fileOffset);
    int caretEndColNo = colNo + Lexer.measureTokenLength(loc, sgr, langOpts);

    int tokPos = fileOffset;
    int lineStart = tokPos - colNo + 1;

    int lineEnd = tokPos;
    while (strData[lineEnd] != '\n' && strData[lineEnd] != '\r'
        && strData[lineEnd] != '\0')
      ++lineEnd;

    StringBuilder sourceLine = new StringBuilder();
    sourceLine.append(Arrays.copyOfRange(strData, lineStart, lineEnd));
    StringBuilder caretLine = new StringBuilder(Util.fixedLengthString(lineEnd - lineStart, ' '));

    if (ranges.length != 0) {
      int lineNo = sgr.getLineNumber(fid, fileOffset);
      for (SourceRange r : ranges)
        highlightRange(r, sgr, lineNo, fid, caretLine, sourceLine.toString());
    }

    // Next, insert the caret itself.
    if (colNo - 1 < caretLine.length())
      caretLine.setCharAt(colNo - 1, '^');
    else
      caretLine.append('^');

    for (int i = 0, e = sourceLine.length(); i < e; ++i) {
      if (sourceLine.charAt(i) != '\t')
        continue;
      sourceLine.setCharAt(i, ' ');

      // Compute the number of spaces we need to insert.
      int numSpaces = ((i + 8) & ~7) - (i + 1);
      Util.assertion(numSpaces < 8, "Invalid computation of spaces.");

      sourceLine.insert(i + 1, Util.fixedLengthString(numSpaces, ' '));

      // Insert spaces or ~'s into CaretLine.
      char insertedChar = caretLine.charAt(i) == '~' ? '?' : ' ';
      caretLine.insert(i + 1, Util.fixedLengthString(numSpaces, insertedChar));
    }

    // If we are in -fdiagnostics-print-source-range-info mode, we are trying to
    // produce easily machine parsable output.  Add a space before the source line
    // and the caret to make it trivial to tell the main diagnostic line from what
    // the user is intended to see.
    if (printRangeInfo) {
      sourceLine.insert(0, ' ');
      caretLine.insert(0, ' ');
    }

    StringBuilder fixtItInsertionLine = new StringBuilder();
    if (hints != null && hints.length != 0 && printFixItInfo) {
      for (FixItHint hint : hints) {
        if (hint.insertionLoc.isValid()) {
          Pair<FileID, Integer> hintLocInfo =
              sgr.getDecomposedInstantiationLoc(hint.insertionLoc);
          if (sgr.getLineNumber(hintLocInfo.first, hintLocInfo.second)
              == sgr.getLineNumber(fid, fileOffset)) {
            // insert the new code into the line just below the code
            // that the user wrote.
            int hintColNo =
                sgr.getColumnNumber(hintLocInfo.first, hintLocInfo.second);
            int lastColumnModified = hintColNo - 1 + hint.codeToInsert.length();
            if (lastColumnModified > fixtItInsertionLine.length()) {
              fixtItInsertionLine.delete(0, fixtItInsertionLine.length());
              fixtItInsertionLine.append(Util
                  .fixedLengthString(lastColumnModified, ' '));
            }

            fixtItInsertionLine.insert(hintColNo - 1, hint.codeToInsert);
          } else {
            fixtItInsertionLine.delete(0, fixtItInsertionLine.length());
            break;
          }
        }
      }
    }

    // If the source line is too long for our terminal, select only the
    // "interesting" source region within that line.
    if (columns != 0 && sourceLine.length() > columns)
      selectInterestingSourceRegion(sourceLine, caretLine,
          fixtItInsertionLine, caretEndColNo, columns);

    while (caretLine.charAt(caretLine.length() - 1) == ' ')
      caretLine.deleteCharAt(caretLine.length() - 1);

    // Emit what we have computed.
    os.println(sourceLine.toString());
    if (useColors)
      os.changeColor(SavedColor, true);

    if (useColors)
      os.changeColor(CaretColor, true);
    os.println(caretLine.toString());

    if (useColors)
      os.resetColor();

    if (fixtItInsertionLine.length() != 0) {
      if (useColors)
        os.changeColor(fixItColor, true);
      if (printRangeInfo)
        os.print(' ');
      os.print(fixtItInsertionLine.toString());
      if (useColors)
        os.resetColor();
      os.println();
    }
  }

  /**
   * When the source code line we want to print is too long for
   * the terminal, select the "interesting" region.
   *
   * @param sourceLine
   * @param caretLine
   * @param fixtItInsertionLine
   * @param endOfCaretToken
   * @param columns
   */
  private static void selectInterestingSourceRegion(
      StringBuilder sourceLine,
      StringBuilder caretLine,
      StringBuilder fixtItInsertionLine,
      int endOfCaretToken,
      int columns) {
    if (caretLine.length() > sourceLine.length()) {
      sourceLine.delete(0, sourceLine.length());
      sourceLine.append(Util.fixedLengthString(caretLine.length(), ' '));
    }

    // Skip the begining spaces
    int caretStart = 0, caretEnd = caretLine.length();
    for (; caretStart < caretEnd; caretStart++) {
      if (!Character.isSpaceChar(caretLine.charAt(caretStart)))
        break;
    }

    // skip the tailing spaces.
    for (; caretEnd != caretStart; --caretEnd) {
      if (!Character.isSpaceChar(caretLine.charAt(caretEnd - 1)))
        break;
    }

    if (caretEnd < endOfCaretToken)
      caretEnd = endOfCaretToken;

    // If we have a fix-it line, make sure the slice includes all of the
    // fix-it information.
    if (fixtItInsertionLine.length() != 0) {
      int fixItStart = 0, fixItEnd = fixtItInsertionLine.length();
      for (; fixItStart < fixItEnd; ++fixItStart) {
        if (!Character.isSpaceChar(fixtItInsertionLine.charAt(fixItStart)))
          break;
      }

      for (; fixItEnd != fixItStart; --fixItEnd) {
        if (!Character.isSpaceChar(fixtItInsertionLine.charAt(fixItEnd)))
          break;
      }

      if (fixItStart < caretStart)
        caretStart = fixItStart;
      if (fixItEnd > caretEnd)
        caretEnd = fixItEnd;
    }

    // CaretLine[CaretStart, CaretEnd) contains all of the interesting
    // parts of the caret line. While this slice is smaller than the
    // number of columns we have, try to grow the slice to encompass
    // more context.

    // If the end of the interesting region comes before we run out of
    // space in the terminal, start at the beginning of the line.
    if (columns > 3 && caretEnd < columns - 3)
      caretStart = 0;

    int targetColumns = columns;
    if (targetColumns > 8)
      targetColumns = 8;      // Give us extra space for the ellipse.
    int sourceLength = sourceLine.length();
    while (caretEnd - caretStart < targetColumns) {
      boolean expanedRegion = false;
      if (caretStart == 1)
        caretStart = 0;
      else if (caretStart > 1) {
        int newStart = caretStart - 1;

        // Skip over any whitespace we see here; we're looking for
        // another bit of interesting text.
        while (newStart != 0 && Character.isSpaceChar(sourceLine.charAt(newStart)))
          --newStart;

        // Skip over this bit of "interesting" text.
        while (newStart != 0 && !Character.isSpaceChar(sourceLine.charAt(newStart)))
          --newStart;

        // Move up to the non-whitespace character we just saw.
        if (newStart != 0)
          ++newStart;

        if (caretEnd - newStart <= targetColumns) {
          caretStart = newStart;
          expanedRegion = true;
        }
      }

      if (caretEnd != sourceLength) {
        int newEnd = caretEnd;

        while (newEnd != sourceLength && Character.isSpaceChar(sourceLine.charAt(newEnd)))
          ++newEnd;

        while (newEnd != sourceLength && !Character.isSpaceChar(sourceLine.charAt(newEnd)))
          ++newEnd;

        if (newEnd - caretStart <= targetColumns) {
          caretEnd = newEnd;
          expanedRegion = true;
        }
      }
      if (!expanedRegion)
        break;
    }

    if (caretEnd < sourceLine.length()) {
      sourceLine.replace(caretEnd, sourceLine.length(), "...");
    }
    if (caretEnd < caretLine.length()) {
      caretLine.delete(caretEnd, caretLine.length());
    }
    if (fixtItInsertionLine.length() > caretEnd)
      fixtItInsertionLine.delete(caretEnd, fixtItInsertionLine.length());

    if (caretStart > 2) {
      sourceLine.replace(0, caretStart, "  ...");
      caretLine.replace(0, caretStart, "     ");
      if (fixtItInsertionLine.length() >= caretStart)
        fixtItInsertionLine.replace(0, caretStart, "     ");
    }
  }

  /**
   * Handle this diagnostic, reporting it or capturing it to a log as needed.
   *
   * @param diagLevel
   * @param info
   */
  @Override
  public void handleDiagnostic(
      Diagnostic.Level diagLevel,
      DiagnosticInfo info) {
    // Keeps track of the the starting position of the location
    // information (e.g., "foo.c:10:4:") that precedes the error
    // message. We use this information to determine how long the
    // file+line+column number prefix is.
    int startOfLocationInfo = 0;

    // If the location is specified, print out a file/line/col and include trace
    // if enabled.
    if (info.getLocation().isValid()) {
      SourceManager sgr = info.getLocation().getSourceMgr();
      PresumedLoc ploc = sgr.getPresumedLoc(info.getLocation());
      int lineNo = ploc.getLine();

      if (!lastWarningLoc.equals(ploc.getIncludeLoc())) {
        lastWarningLoc = ploc.getIncludeLoc();
        printIncludeStack(lastWarningLoc, sgr);
        // startOfLocationInfo = os.tell();
      }

      if (showLocation) {
        if (useColors)
          os.changeColor(SavedColor, true);

        os.printf("%s:%d:", ploc.getFilename(), lineNo);
        if (showColumn) {
          int colNo = ploc.getColumn();
          if (colNo != 0)
            os.printf("%d:", colNo);
        }

        if (printRangeInfo && info.getNumRanges() != 0) {
          FileID caretFileID = sgr.getFileID(sgr.getInstantiationLoc(info.getLocation()));
          boolean printedRange = false;

          for (int i = 0, size = info.getNumRanges(); i < size; i++) {
            SourceRange r = info.getRange(i);
            if (!r.isValid())
              continue;

            SourceLocation b = r.getBegin(), e = r.getEnd();
            b = sgr.getInstantiationLoc(b);
            e = sgr.getInstantiationLoc(e);

            if (b.equals(e) && r.getEnd().isMacroID())
              e = sgr.getInstantiationRange(r.getEnd()).getEnd();

            Pair<FileID, Integer> binfo = sgr.getDecomposedLoc(b);
            Pair<FileID, Integer> einfo = sgr.getDecomposedLoc(e);

            if (!binfo.first.equals(caretFileID)
                || !einfo.first.equals(caretFileID))
              continue;

            int tokSize = Lexer.measureTokenLength(e, sgr, langOpts);

            os.printf("{%d:%d-%d:%d}",
                sgr.getLineNumber(binfo.first, binfo.second),
                sgr.getColumnNumber(binfo.first, binfo.second),
                sgr.getLineNumber(einfo.first, einfo.second),
                sgr.getColumnNumber(einfo.first, einfo.second) + tokSize);
            printedRange = true;
          }

          if (printedRange)
            os.print(":");
        }

        os.print(' ');
      }
    }

    if (useColors)
      os.resetColor();

    if (useColors) {
      // Print diagnostic category in bold and color
      switch (diagLevel) {
        case Ignored:
          Util.assertion(false, "Invalid diagnostic type!");
        case Note:
          os.changeColor(noteColor, true);
          break;
        case Warning:
          os.changeColor(warnColor, true);
          break;
        case Error:
          os.changeColor(errorColor, true);
          break;
        case Fatal:
          os.changeColor(fatalColor, true);
          break;
      }
    }

    switch (diagLevel) {
      case Ignored:
        Util.assertion(false, "Invalid diagnostic type!");
      case Note:
        os.print("note: ");
        break;
      case Warning:
        os.print("warning: ");
        break;
      case Error:
        os.print("error: ");
        break;
      case Fatal:
        os.print("fatal error: ");
        break;
    }

    if (useColors)
      os.resetColor();

    StringBuilder outStr = new StringBuilder();
    info.formatDiagnostic(outStr);

    if (printDiagnosticOption) {
      String opt = Diagnostic.getWarningOptionForDiag(info.getID());
      if (opt != null && !opt.isEmpty()) {
        outStr.append(" [-W");
        outStr.append(opt);
        outStr.append(']');
      }
    }
    if (useColors) {
      switch (diagLevel) {
        case Warning:
        case Error:
        case Fatal:
          os.changeColor(SavedColor, true);
          break;
        default:
          break;
      }
    }
    os.println(outStr.toString());
    if (useColors)
      os.resetColor();

    if (((!lastLoc.equals(info.getLocation())) || info.getNumRanges() != 0
        || (lastCaretDiagnosticWasNote
        && diagLevel != Diagnostic.Level.Note)
        || info.getNumFixtItHints() != 0)) {
      if (caretDiagnostics && info.getLocation().isValid()) {
        lastLoc = info.getLocation();
        lastCaretDiagnosticWasNote = diagLevel == Diagnostic.Level.Note;

        SourceRange[] ranges = new SourceRange[20];
        for (int i = 0, e = ranges.length; i != e; i++)
          ranges[i] = new SourceRange();

        int numRanges = info.getNumRanges();
        Util.assertion(numRanges < 20, "Out of space");
        for (int i = 0; i < numRanges; i++)
          ranges[i] = info.getRange(i);

        int numHints = info.getNumFixtItHints();
        for (int i = 0; i < numHints; i++) {
          FixItHint hint = info.getHint(i);
          if (hint.removeRange.isValid()) {
            Util.assertion(numRanges < 20, "Out of range");
            ranges[numRanges++] = hint.removeRange;
          }
        }

        emitCaretDiagnostic(lastLoc, ranges, lastLoc.getSourceMgr(),
            info.getFixItHints(), messageLength);
      }
    }
  }
}
