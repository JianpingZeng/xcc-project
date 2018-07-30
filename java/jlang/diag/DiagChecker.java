/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http:*www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package jlang.diag;

import tools.Util;
import jlang.basic.SourceManager;
import jlang.clex.Lexer;
import jlang.clex.Preprocessor;
import jlang.clex.Token;
import jlang.clex.TokenKind;
import jlang.support.FileID;
import jlang.support.SourceLocation;
import tools.Pair;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class DiagChecker
{
    /**
     * Gather the expected diagnostics and check them.
     * @param pp
     * @return
     */
    public static boolean checkDiagnostics(Preprocessor pp)
    {
        ArrayList<Pair<SourceLocation, String>> expectedErrors = new ArrayList<>();
        ArrayList<Pair<SourceLocation, String>> expectedWarnings = new ArrayList<>();
        ArrayList<Pair<SourceLocation, String>> expectedNotes = new ArrayList<>();
        findExpectedDiags(pp, expectedErrors, expectedWarnings, expectedNotes);

        return checkResults(pp, expectedErrors, expectedWarnings, expectedNotes);
    }

    private static void findExpectedDiags(
            Preprocessor pp,
            ArrayList<Pair<SourceLocation, String>> expectedErrors,
            ArrayList<Pair<SourceLocation, String>> expectedWarnings,
            ArrayList<Pair<SourceLocation, String>> expectedNotes)
    {
        FileID mainFileID = pp.getSourceManager().getMainFileID();

        // Lex the given main file in raw mode.
        Lexer rawLexer = new Lexer(mainFileID, pp.getSourceManager(), pp.getLangOptions());

        // Return comments as token, this is how we find expected diagnostics.
        rawLexer.setCommentRetentionState(true);

        Token tok = new Token();
        tok.setKind(TokenKind.Comment);

        // Lexing the main file until a EOF reached.
        while (tok.isNot(TokenKind.eof))
        {
            rawLexer.lex(tok);
            if (!tok.is(TokenKind.Comment))
                continue;

            String comment = pp.getSpelling(tok);
            if (comment.isEmpty())
                continue;

            //  Find all expected errors.
            findDiagnostics(comment, expectedErrors, pp, tok.getLocation(), "expected-error");

            // Find all expected warnings.
            findDiagnostics(comment, expectedWarnings, pp, tok.getLocation(), "expected-warning");

            // Find all expected notes.
            findDiagnostics(comment, expectedNotes, pp, tok.getLocation(), "expected-note");
        }
    }

    /**
     * Indicating that a line expects an error or a warning is simple. Put a comment
     * on the line that has the diagnostic, use "expected-{error,warning}" to tag
     * if it's an expected error or warning, and place the expected text between {{
     * and }} markers. The full text doesn't have to be included, only enough to
     * ensure that the correct diagnostic was emitted.
     *
     * Here's an example:
     *
     *   int A = B; * expected-error {{use of undeclared identifier 'B'}}
     *
     * You can place as many diagnostics on one line as you wish. To make the code
     * more readable, you can use slash-newline to separate out the diagnostics.
     *
     * The simple syntax above allows each specification to match exactly one error.
     * You can use the extended syntax to customize this. The extended syntax is
     * "expected-<type> <n> {{diag text}}", where <type> is one of "error",
     * "warning" or "note", and <n> is a positive integer. This allows the
     * diagnostic to appear as many times as specified. Example:
     *
     *   void f(); * expected-note 2 {{previous declaration is here}}
     *

     /// FindDiagnostics - Go through the comment and see if it indicates expected
     /// diagnostics. If so, then put them in a diagnostic list.
     * @param comment
     * @param expectedDiags
     * @param pp
     * @param loc
     * @param expectedStr
     */
    private static void findDiagnostics(
            String comment,
            ArrayList<Pair<SourceLocation, String>> expectedDiags,
            Preprocessor pp,
            SourceLocation loc,
            String expectedStr)
    {
        int expectedStrLen = expectedStr.length();
        int i = 0, len = comment.length();
        while (i != len)
        {
            i = comment.indexOf('e', i);
            if (i < 0)
                return;

            // If this isn't expected-foo, ignore it.
            if ((len - i) < expectedStrLen ||
                    !comment.substring(i, i + expectedStrLen).equals(expectedStr))
            {
                ++i;
                continue;
            }

            i += expectedStrLen;

            // Skip whitespace.
            while (i != len && Character.isSpaceChar(comment.charAt(i)))
                ++i;

            int times = 1;
            int temp = 0;
            while (i != len && comment.charAt(i) >= '0' && comment.charAt(i)<='9')
            {
                temp *= 10;
                temp += comment.charAt(i) - '0';
                ++i;
            }

            if (temp > 0)
                times = temp;

            // We should have a {{ now.
            if (len - i < 2 || comment.charAt(i) != '{' ||
                    comment.charAt(i+1) != '{')
            {
                if (comment.indexOf('{', i) != -1)
                    emitError(pp, loc, "bogus characters before '{{' in expected string");
                else
                    emitError(pp, loc, "can not find start ('{{') of expected string");
                return;
            }

            i += 2;

            // Find '}}'.
            int expectedEnd = i;
            while (true)
            {
                expectedEnd = comment.indexOf('}', expectedEnd);
                if (len - expectedEnd < 2)
                {
                    emitError(pp, loc, "cannot find ('}}') of expected string");
                    return;
                }
                if (comment.charAt(expectedEnd+1) == '}')
                    break;
                ++expectedEnd;
            }

            String msg = comment.substring(i, expectedEnd);

            // Add is possibly multiple times.
            while (times-- > 0)
                expectedDiags.add(Pair.get(loc, msg));

            i = expectedEnd;
        }
    }

    private static void emitError(Preprocessor pp, SourceLocation pos, String str)
    {
        int id = pp.getDiagnostics().getCustomDiagID(Diagnostic.Level.Error, str);
        pp.diag(pos, id);
    }

    /**
     * This compares the expected results to those that
     * were actually reported. It emits any discrepencies. Return "true" if there
     * were problems. Return "false" otherwise.
     * @param pp
     * @param expectedErrors
     * @param expectedWarnings
     * @param expectedNotes
     * @return
     */
    private static boolean checkResults(
            Preprocessor pp,
            ArrayList<Pair<SourceLocation, String>> expectedErrors,
            ArrayList<Pair<SourceLocation, String>> expectedWarnings,
            ArrayList<Pair<SourceLocation, String>> expectedNotes)
    {
        DiagnosticClient client = pp.getDiagnostics().getClient();
        Util.assertion( client != null);
        TextDiagnosticBuffer diag = (TextDiagnosticBuffer)client;
        SourceManager sgm = pp.getSourceManager();

        // We want to capture the delta between what was expected and what was
        // seen.
        //
        //   Expected - Seen - set expected but not seen
        //   Seen - Expected - set seen but not expected
        boolean hadProblem = false;


        // Computes mismatch error information.
        hadProblem |= compareDiagLists(sgm, expectedErrors, diag.getErrors(),
                "Errors expected but not seen", "Errors seen but not expected");

        // Computes mismatch warnings information.
        hadProblem |= compareDiagLists(sgm, expectedWarnings, diag.getWarnings(),
                "Warnings expected but not seen", "Warnings seen but not expected");

        // Computes mismatch notes information.
        hadProblem |= compareDiagLists(sgm, expectedNotes, diag.getNotes(),
                "Notes expected but not seen", "Notes seen but not expected");

        return hadProblem;
    }

    /**
     * Compare two diagnostic lists and return the difference
     * between them.
     * @param sourceMgr
     * @param expectedDiags
     * @param diagList
     * @param expectedButNoSeen
     * @param seenButNotExpected
     * @return
     */
    private static boolean compareDiagLists(
            SourceManager sourceMgr,
            ArrayList<Pair<SourceLocation, String>> expectedDiags,
            ArrayList<Pair<SourceLocation, String>> diagList,
            String expectedButNoSeen,
            String seenButNotExpected)
    {
        ArrayList<Pair<SourceLocation, String>> left = new ArrayList<>();
        ArrayList<Pair<SourceLocation, String>> right = new ArrayList<>();
        left.addAll(expectedDiags);
        right.addAll(diagList);
        ArrayList<Pair<SourceLocation, String>> leftOnly = new ArrayList<>();

        Iterator<Pair<SourceLocation, String>> itr = left.iterator();
        for (; itr.hasNext();)
        {
            Pair<SourceLocation,String> pair1 = itr.next();
            int lineNo1 = sourceMgr.getInstantiationLineNumber(pair1.first);
            String expected = pair1.second;

            Iterator<Pair<SourceLocation, String>> itr2 = right.iterator();
            boolean notFound = true;

            while (itr2.hasNext())
            {
                Pair<SourceLocation,String> pair2 = itr2.next();
                int lineNo2 = sourceMgr.getInstantiationLineNumber(pair2.first);
                if (lineNo1 != lineNo2)
                    continue;

                String diagStr = pair2.second;
                if (expected.contains(diagStr) || diagStr.contains(expected))
                {
                    notFound = false;
                    break;
                }
            }
            if (notFound)
            {
                // left only.
                leftOnly.add(pair1);
            }
            else
            {
                // Found, the same can not be found twice.
                itr2.remove();
            }
        }

        // Now all that left in the rigth list are those that were
        // not expected but seen.
        return printProblem(sourceMgr, leftOnly, expectedButNoSeen)
                | printProblem(sourceMgr, right, seenButNotExpected);
    }

    /**
     * This takes a diagnostic map of the delta between expected and
     * seen diagnostics. If there's anything in it, then something unexpected
     * happened. Print the map out in a nice format and return "true".
     * If the map is empty and we're not going to print things, then
     * return "false".
     * @param sourceMgr
     * @param diagList
     * @param msg
     * @return
     */
    private static boolean printProblem(
            SourceManager sourceMgr,
            ArrayList<Pair<SourceLocation, String>> diagList,
            String msg)
    {
        if (diagList.isEmpty())
            return false;

        System.err.println(msg);

        diagList.forEach(pair->
        {
            System.err.printf("  Line %d: %s\n",
                    sourceMgr.getInstantiationLineNumber(pair.first),
                    pair.second);
        });
        return true;
    }
}
