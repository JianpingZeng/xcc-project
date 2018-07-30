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

package jlang.cparser;

import jlang.clex.TokenKind;
import jlang.diag.DiagnosticCommonKindsTag;
import jlang.support.SourceLocation;
import jlang.support.SourceRange;
import tools.Util;

import static jlang.diag.DiagnosticParseTag.*;

/**
 * A helpful Object used for parsing a pair of open/close delimiter, such as
 * the '(...)' or '{...}'.
 * @author Jianping Zeng
 * @version 0.1
 */
public class BalancedDelimiterTracker
{
    private TokenKind kind, close;
    private Parser p;
    private int maxDepth;
    private SourceLocation lopen, rclose;

    private void assignClosingDelimiter()
    {
        switch (kind)
        {
            default:
                Util.shouldNotReachHere("Unexpected unbalanced token");
            case l_brace:
                close = TokenKind.r_brace;
                break;
            case l_paren:
                close = TokenKind.r_paren;
                break;
            case l_bracket:
                close = TokenKind.r_bracket;
                break;
            case less:
                close = TokenKind.greater;
                break;
        }
    }

    public BalancedDelimiterTracker(Parser p, TokenKind k)
    {
        kind = k;
        this.p = p;
        maxDepth = 256;
        assignClosingDelimiter();
    }

    public SourceLocation getOpenLocation()
    {
        return lopen;
    }

    public SourceLocation getCloseLocation()
    {
        return rclose;
    }

    public SourceRange getSourceRange()
    {
        return new SourceRange(lopen, rclose);
    }

    public boolean expectAndConsume(int diagID)
    {
        return expectAndConsume(diagID, "");
    }

    public boolean expectAndConsume(int diagID, String msg)
    {
        return expectAndConsume(diagID, msg, TokenKind.Unknown);
    }

    public boolean expectAndConsume(int diagID, String msg, TokenKind skipToTok)
    {
        lopen = p.tok.getLocation();
        if (!p.expectAndConsume(kind, diagID, msg, skipToTok))
        {
            p.quantityTracker.push(kind);
            if (p.quantityTracker.getDepth(kind) < maxDepth)
                return false;
            else
            {
                p.diag(p.tok.getLocation(), err_parser_impl_limit_overflow);
                p.skipUntil(TokenKind.eof, true);
            }
        }
        return true;
    }

    public boolean consumeOpen()
    {
        // Try to consume the token we are holding
        if (p.tok.is(kind))
        {
            p.quantityTracker.push(kind);
            if (p.quantityTracker.getDepth(kind) < maxDepth)
            {
                lopen = p.consumeAnyToken();
                return false;
            }
            else
            {
                p.diag(p.tok.getLocation(), err_parser_impl_limit_overflow);
                p.skipUntil(TokenKind.eof, true);
            }
        }
        return true;
    }

    public boolean consumeClose()
    {
        if (p.tok.is(close))
        {
            rclose = p.consumeAnyToken();
            return false;
        }
        else
        {
            String lhsname = "unknown";
            int diagID = err_parse_error;
            switch (close)
            {
                default:break;
                case r_paren:
                    lhsname = "(";
                    diagID = err_expected_rparen;
                    break;
                case r_brace:
                    lhsname = "{";
                    diagID = err_expected_rbrace;
                    break;
                case r_bracket:
                    lhsname = "[";
                    diagID = err_expected_rsquare;
                    break;
                case greater:
                    lhsname = "<";
                    diagID = err_expected_greater;
                    break;
            }
            p.diag(p.tok.getLocation(), diagID).emit();
            p.diag(lopen, DiagnosticCommonKindsTag.note_matching)
                    .addTaggedVal(lhsname)
                    .emit();
            if (p.skipUntil(close, true))
                rclose = p.tok.getLocation();
        }
        return true;
    }
}
