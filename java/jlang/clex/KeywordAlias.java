/*
 * Extremely C language Compiler
 * Copyright (c), 2015-2017, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"),;
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

package jlang.clex;

import static jlang.clex.IdentifierTable.KEYALL;

/**
 * This enumerate defines some Alias for GNU extension keyword, like "__asm" kind
 * to "asm".
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public enum KeywordAlias
{
    // Alternate spelling for various tokens.  There are GCC extensions in all
    // languages, but should not be disabled in strict conformance mode.
    __Attribute__("__attribute__", TokenKind.__Attribute, KEYALL),
    __Const("__const"      , TokenKind.Const      , KEYALL),
    __Const__("__const__"    ,TokenKind.Const      , KEYALL),
    __Alignof__("__alignof__"  ,TokenKind.__Alignof, KEYALL),
    __Asm("__asm"        ,TokenKind.Asm, KEYALL),
    __Asm__("__asm__"      ,TokenKind.Asm, KEYALL),
    __Complex("__complex"    ,TokenKind.Complex, KEYALL),
    __Complex__("__complex__"  ,TokenKind.Complex, KEYALL),
    __Imag__("__imag__"     ,TokenKind.__Imag, KEYALL),
    __Inline("__inline"     ,TokenKind.Inline, KEYALL),
    __Inline__("__inline__"   , TokenKind.Inline, KEYALL),
    __Real__("__real__"     , TokenKind.__Real__, KEYALL),
    __Restrict("__restrict"   ,TokenKind.Restrict, KEYALL),
    __Restrict__("__restrict__" ,TokenKind.Restrict, KEYALL),
    __Signed("__signed"     , TokenKind.Signed, KEYALL),
    __Signed__("__signed__"   ,TokenKind.Signed , KEYALL),
    __Typeof("__typeof"     , TokenKind.Typeof, KEYALL),
    __Typeof__("__typeof__"   ,TokenKind.Typeof, KEYALL),
    __Volatile("__volatile"   ,TokenKind.Volatile, KEYALL),
    __Volatile__("__volatile__" ,TokenKind.Volatile, KEYALL);

    String name;
    TokenKind kind;
    int flag;

    /**
     * Constructs a aliased TokenKind for the GNU extension.
     * @param name  The name of aliased keyword.
     * @param kind  Which realistic TokenKind this kind to.
     * @param flag  Indicate in what language standard this keyword would be enabeld.
     */
    KeywordAlias(String name, TokenKind kind, int flag)
    {
        this.name = name;
        this.kind = kind;
        this.flag = flag;
    }
}
