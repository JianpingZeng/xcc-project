/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

package jlang.diag;

import jlang.support.SourceLocation;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;

/**
 * This handler to diagnostic is used for saving all of error and warning
 * information into a buffer rather than standard output(like Terminal).
 *
 * The mainly using of this class is for verifing diagnostics information
 * by "-verify" option in command line.
 * @author Xlous.zeng
 * @version 0.1
 */
public class TextDiagnosticBuffer implements DiagnosticClient, Cloneable
{
    private ArrayList<Pair<SourceLocation, String>> errors;
    private ArrayList<Pair<SourceLocation, String>> warnings;
    private ArrayList<Pair<SourceLocation, String>> notes;

    public TextDiagnosticBuffer()
    {
        errors = new ArrayList<>();
        warnings = new ArrayList<>();
        notes = new ArrayList<>();
    }

    public ArrayList<Pair<SourceLocation, String>> getErrors()
    {
        return errors;
    }

    public ArrayList<Pair<SourceLocation, String>> getWarnings()
    {
        return warnings;
    }

    public ArrayList<Pair<SourceLocation, String>> getNotes()
    {
        return notes;
    }

    /**
     * Stores the errors, warnings, and notes that are reported into a buffer.
     * @param diagLevel
     * @param info
     */
    @Override
    public void handleDiagnostic(Diagnostic.Level diagLevel,
            DiagnosticInfo info)
    {
        StringBuilder buf = new StringBuilder();
        info.formatDiagnostic(buf);
        switch (diagLevel)
        {
            default:
                Util.shouldNotReachHere("Diagnostic not handled during diagnostic buffering");
                break;
            case Note:
                notes.add(Pair.get(info.getLocation(), buf.toString()));
                break;
            case Warning:
                warnings.add(Pair.get(info.getLocation(), buf.toString()));
                break;
            case Error:
            case Fatal:
                errors.add(Pair.get(info.getLocation(), buf.toString()));
                break;
        }
    }

    @Override
    public TextDiagnosticBuffer clone()
    {
        return new TextDiagnosticBuffer();
    }
}
