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

import jlang.support.SourceRange;
import jlang.clex.IdentifierInfo;
import jlang.diag.Diagnostic.ArgumentKind;
import tools.OutParamWrapper;
import tools.Pair;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class DiagnosticInfo
{
    private final Diagnostic diagObj;

    public DiagnosticInfo(Diagnostic diagObj)
    {
        this.diagObj = diagObj;
    }

    public Diagnostic getDiagObj()
    {
        return diagObj;
    }

    public int getID()
    {
        return diagObj.getID();
    }

    public FullSourceLoc getLocation()
    {
        return diagObj.getCurDiagLoc();
    }

    public int getNumArgs()
    {
        return diagObj.getNumDiagArgs();
    }

    /**
     * Obtain the kind qualified by {@linkplain ArgumentKind} of the specified
     * argument in the specified position.
     * @param index
     * @return
     */
    public ArgumentKind getArgKind(int index)
    {
        assert index >= 0 && index < getNumArgs() :"Argument index out of range!";
        return diagObj.getDiagArgKind(index);
    }

    public String getArgStdStr(int index)
    {
        return diagObj.getArgStdStr(index);
    }

    public int getArgSInt(int index)
    {
        return diagObj.getArgSInt(index);
    }

    public int getNumRanges()
    {
        return diagObj.getNumDiagRanges();
    }

    public SourceRange getRange(int idx)
    {
        assert idx >= 0 && idx < getNumRanges():"Invalid range index";
        return diagObj.getRange(idx);
    }

    /**
     * Format this diagnostic into a string, substituting the
     * formal arguments into the %0 slots.  The result is appended onto the Str
     * array.
     * @param outStr
     */
    public void formatDiagnostic(StringBuilder outStr)
    {
        String diagStr = Diagnostic.getDescription(getID());
        int i = 0, e = diagStr.length();
        while (i != e)
        {
            if (diagStr.charAt(i) != '%')
            {
                // Append non-%0 substrings to Str if we have one.
                int strEnd = diagStr.indexOf('%', i);
                if (strEnd < 0)
                    strEnd = e;
                outStr.append(diagStr.substring(i, strEnd));
                i = strEnd;
                continue;
            }
            else if (diagStr.charAt(i + 1) == '%')
            {
                outStr.append('%');  // %% -> %.
                i += 2;
                continue;
            }

            // Skip the %.
            ++i;

            // This must be a placeholder for a diagnostic argument.  The format for a
            // placeholder is one of "%0", "%modifier0", or "%modifier{arguments}0".
            // The digit is a number from 0-9 indicating which argument this comes from.
            // The modifier is a string of digits from the set [-a-z]+, arguments is a
            // brace enclosed string.
            int modifier = 0, argument = 0;
            int modifierLen = 0, argumentLen = 0;

            // Check to see if we have a modifier.  If so eat it.
            if (!Character.isDigit(diagStr.charAt(i)))
            {
                modifier = i;
                while (diagStr.charAt(i) == '-' ||
                        (diagStr.charAt(i) >= 'a' && diagStr.charAt(0) <= 'z'))
                    ++i;

                modifierLen = i - modifier;

                // If we have an argument, get it next.
                if (diagStr.charAt(i) == '{')
                {
                    ++i; // Skip {.
                    argument = i;

                    for (; diagStr.charAt(i) != '}'; ++i);

                    assert diagStr.charAt(i) != 0 : "Mismatched {}'s in diagnostic string!";
                    argumentLen = i - argument;
                    ++i;  // Skip }.
                }
            }

            assert Character.isDigit(diagStr.charAt(i)) : "Invalid format for argument in diagnostic";
            int argNo = diagStr.charAt(i++) - '0';

            switch (getArgKind(argNo))
            {
                // ---- STRINGS ----
                case ak_std_string:
                case ak_c_string:
                {
                    String s = getArgStdStr(argNo);
                    assert modifierLen == 0 : "No modifiers for strings yet";
                    outStr.append(s);
                    break;
                }
                // ---- INTEGERS ----
                case ak_sint:
                case ak_uint:
                {
                    int val = getArgSInt(argNo);

                    if (ModifierIs(diagStr, modifier, modifierLen, "select"))
                    {
                        handleSelectModifier(val, diagStr.substring(argument, argument+argumentLen), outStr);
                    }
                    else if (ModifierIs(diagStr, modifier, modifierLen, "s"))
                    {
                        handleIntegerSModifier(val, outStr);
                    }
                    else if (ModifierIs(diagStr, modifier, modifierLen, "plural"))
                    {
                        handlePluralModifier(val, diagStr.substring(argument, argument+argumentLen), outStr);
                    }
                    else
                    {
                        assert modifierLen == 0 : "Unknown integer modifier";
                        outStr.append(val);
                    }
                    break;
                }
                // ---- NAMES and TYPES ----
                case ak_identifier:
                {
                    IdentifierInfo II = getArgIdentifier(argNo);
                    assert modifierLen == 0 : "No modifiers for strings yet";

                    // Don't crash if get passed a null pointer by accident.
                    if (II == null)
                    {
                        outStr.append("(null)");
                        continue;
                    }

                    outStr.append('\'');
                    outStr.append(II.getName());
                    outStr.append('\'');
                    break;
                }
                case ak_qualtype:
                case ak_declarationname:
                case ak_nameddecl:
                    diagObj.convertArgToString(getArgKind(argNo), getRawArg(argNo),
                            diagStr.substring(modifier, modifier + modifierLen),
                            diagStr.substring(argument, argument + argumentLen),
                            outStr);
                    break;
            }
        }
    }

    private IdentifierInfo getArgIdentifier(int idx)
    {
        assert getArgKind(idx) == ArgumentKind.ak_identifier;
        return (IdentifierInfo)diagObj.getRawArg(idx);
    }

    public Object getRawArg(int idx)
    {
        return diagObj.getRawArg(idx);
    }

    private void handlePluralModifier(int valNo, String argument,
            StringBuilder outStr)
    {
        int end = argument.length();
        int start = 0;
        while (true)
        {
            assert start < end:"Plural expression didn't match";
            int exprEnd = 0;
            while (argument.charAt(exprEnd) != ':')
            {
                assert exprEnd != end:"Plural missing expression end";
                ++exprEnd;
            }

            if (evalPluralExpr(valNo, argument, start, exprEnd))
            {
                start = exprEnd + 1;
                exprEnd = argument.indexOf('|');
                outStr.append(argument.substring(start, exprEnd));
                return;
            }

            start = argument.substring (start, end-1).indexOf('|') + 1;
        }
    }

    private static boolean evalPluralExpr(int valNo, String str, int start, int end)
    {
        if (str.charAt(start) == ':')
            return true;

        while (true)
        {
            char c = str.charAt(start);
            if (c == '%')
            {
                ++start;
                Pair<Integer, Integer> res = pluralNumber(str, start, end);
                int arg = res.first;
                start = res.second;
                ++start;
                int valMod = valNo % arg;
                OutParamWrapper<Integer> x = new OutParamWrapper<>(start);
                boolean b = testPluralRange(valMod, str, x, end);
                start = x.get();
                if (b)
                    return true;
            }
            else
            {
                assert c == '[' || Character.isDigit(c):
                        "Bad plural expression syntax: unexpected character";
                OutParamWrapper<Integer> x = new OutParamWrapper<>(start);
                // Range expression
                boolean b = testPluralRange(valNo, str, x, end);
                start = x.get();
                if (b) return true;
            }

            for (; start < end && str.charAt(start) != ','; ++start);
            if (start == end)
                break;
            ++start;
        }
        return false;
    }

    /**
     * Test if Val is in the parsed range. Modifies Start.
     * @param val
     * @param str
     * @param start
     * @param end
     * @return
     */
    private static boolean testPluralRange(int val, String str,
            OutParamWrapper<Integer> start, int end)
    {
        int begin = start.get();
        if (str.charAt(begin) != '[')
        {
            Pair<Integer, Integer> res = pluralNumber(str, begin, end);
            int ref = res.first;
            begin = res.second;
            start.set(begin);
            return ref == val;
        }

        ++begin;
        Pair<Integer, Integer> res = pluralNumber(str, begin, end);
        int low = res.first;
        begin = res.second;
        assert str.charAt(begin) == ',': "Bad plural expression syntax: expected ,";
        ++begin;

        res = pluralNumber(str,begin, end);
        int high = res.first;
        begin = res.second;
        ++begin;
        start.set(begin);
        return low <= val && val <= high;
    }

    /**
     * Parse an unsigned integer and advance Start.
     * @return
     */
    private static Pair<Integer, Integer> pluralNumber(String str, int start, int end)
    {
        int val = 0;
        while (start < end && Character.isDigit(str.charAt(start)))
        {
            val *= 10;
            val += str.charAt(start) - '0';
            ++start;
        }
        return Pair.get(val, start);
    }

    /**
     * Handle the integer 's' modifier.  This adds the
     * letter 's' to the string if the value is not 1.  This is used in cases like
     * this:  "you idiot, you have %4 parameter%s4!".
     * @param valNo
     * @param outStr
     */
    private void handleIntegerSModifier(int valNo, StringBuilder outStr)
    {
        if (valNo != 1)
            outStr.append('s');
    }

    private static boolean ModifierIs(String modifier, int from, int len, String str)
    {
        return modifier.substring(from, from + len).equals(str);
    }

    /**
     * Handle the integer 'select' modifier.  This is used
     * like this:  %select{foo|bar|baz}2.  This means that the integer argument
     * "%2" has a value from 0-2.  If the value is 0, the diagnostic prints 'foo'.
     * If the value is 1, it prints 'bar'.  If it has the value 2, it prints 'baz'.
     * This is very useful for certain classes of variant diagnostics.
     * @param valNo
     * @param argument
     * @param outStr
     */
    private static void handleSelectModifier(
            int valNo,
            String argument,
            StringBuilder outStr)
    {
        int start = 0;

        // Skip over 'ValNo' |'s.
        while (valNo != 0)
        {
            int nextVal = argument.indexOf('|');
            assert nextVal >= 0 : "Value for integer select modifier was"+
                " larger than the number of options in the diagnostic string!";
            start = nextVal + 1;
            --valNo;
        }

        int end = argument.indexOf('|');
        outStr.append(argument.substring(start, end));
    }

    public int getNumFixtItHints()
    {
        return diagObj.getNumFixItHints();
    }

    public FixItHint getHint(int index)
    {
        assert index >= 0 && index < getNumFixtItHints();
        return diagObj.getFixItHint(index);
    }

    public FixItHint[] getFixItHints()
    {
        return diagObj.getFixItHints();
    }
}
