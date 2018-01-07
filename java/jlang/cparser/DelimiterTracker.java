/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
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
import tools.Util;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class DelimiterTracker
{
    private int paren, brace, square, less, lessless;

    private void add(TokenKind t, int amount)
    {
        switch (t)
        {
            default:
                Util.shouldNotReachHere("Unexpected balanced token");
            case l_brace:
                brace += amount;
                break;
            case l_paren:
                paren += amount;
                break;
            case l_bracket:
                square += amount;
                break;
            case less:
                less += amount;
                break;
            case lessless:
                lessless += amount;
                break;
        }
    }

    public void push(TokenKind t)
    {
        add(t, 1);
    }

    public void pop(TokenKind t)
    {
        add(t, -1);
    }

    public int getDepth(TokenKind t)
    {
        switch (t)
        {
            default:
                Util.shouldNotReachHere("Unexpected balanced token");
            case l_brace:
                return brace;
            case l_paren:
                return paren;
            case l_bracket:
                return square;
            case less:
                return less;
            case lessless:
                return lessless;
        }
    }

    public DelimiterTracker()
    {
    }
}
