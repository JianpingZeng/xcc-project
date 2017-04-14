package jlang.diag;
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

import jlang.basic.LangOptions;
import jlang.basic.SourceRange;
import jlang.cpp.SourceLocation;

import java.io.PrintStream;
import java.util.ArrayList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class TextDiagnosticClient implements DiagnosticClient
{
    private PrintStream os;
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
    private int messageLenght;
    private boolean useColors;

    public TextDiagnosticClient(PrintStream os)
    {
        this(os, true, true, true, true, true, true, 0, false);
    }

    public TextDiagnosticClient(PrintStream os, boolean showColumn,
            boolean caretDiagnostics, boolean showLocation,
            boolean printRangeInfo, boolean printDiagnosticOption,
            boolean printFixItInfo, int messageLenght, boolean useColors)
    {
        this.os = os;
        lastWarningLoc = SourceLocation.NOPOS;
        this.lastCaretDiagnosticWasNote = false;
        this.showColumn = showColumn;
        this.caretDiagnostics = caretDiagnostics;
        this.showLocation = showLocation;
        this.printRangeInfo = printRangeInfo;
        this.printDiagnosticOption = printDiagnosticOption;
        this.printFixItInfo = printFixItInfo;
        this.messageLenght = messageLenght;
        this.useColors = useColors;
    }

    @Override
    public void setLangOptions(LangOptions langOptions)
    {
        this.langOpts = langOptions;
    }

    public LangOptions getLangOpts()
    {
        return langOpts;
    }

    public void printIncludeStack(SourceLocation loc)
    {}

    public void highlightRange(
            SourceRange range,
            int lineNo,
            int fid,
            String caretLine,
            String sourceLine)
    {

    }

    public void emitCaretDiganostic(
            SourceLocation loc,
            ArrayList<SourceRange> ranges,
            ArrayList<FixItHint> hints,
            int columns)
    {

    }

    /**
     * This method (whose default implementation returns true) indicates whether
     * the diagnostics handled by this DiagnosticClient should be included in
     * the number of diagnostics reported by Diagnostic.
     *
     * @return
     */
    @Override
    public boolean includeInDiagnosticCounts()
    {
        return false;
    }

    /**
     * Handle this diagnostic, reporting it or capturing it to a log as needed.
     *
     * @param diagLevel
     * @param diag
     */
    @Override
    public void handleDiagnostic(
            Diagnostic.Level diagLevel,
            Diagnostic diag)
    {

    }
}
