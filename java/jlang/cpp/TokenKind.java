package jlang.cpp;
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

import jlang.cparser.Tag;

import static jlang.cpp.IdentifierTable.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public enum TokenKind
{
    // Not a token.
    Unknown("unknown", Tag.UNKNOWN),
    // End of file.
    Eof("eof", Tag.EOF),
    // End of preprocessing directive (end of line inside a
    // directive).
    Eod("eod", Tag.EOD),

    Eom("eom", Tag.EOM),

    // C99 6.4.9: Comments.
    Comment("comment", Tag.COMMENT), // Comment (only in -E -C[C] mode)

    // C99 6.4.2: Identifiers.
    Identifier("identifier", Tag.IDENTIFIER), // abcde123

    // C99 6.4.4.1: Integer Constants
    // C99 6.4.4.2: Floating Constants
    Numeric_constant("numeric_constant", Tag.NUMERIC_CONSTANT), // 0x123

    // C99 6.4.4: Character Constants
    Char_constant("char_constant", Tag.CHAR_CONSTANT),

    // C99 6.4.5: String Literals.
    String_literal("string_literal", Tag.STRING_LITERAL), // "foo"
    Angle_string_literal("angle_string_literal", Tag.ANGLE_STRING_LITERAL), // <foo>

    // specifier
    Void("void", Tag.VOID, KEYALL),
    _Bool("_Bool", Tag.BOOL, KEYC99),
    Bool("bool", Tag.BOOL, BOOLSUPPORT),
    Char("char", Tag.CHAR, KEYALL),
    Short("short", Tag.SHORT, KEYALL),
    Int("int", Tag.INT, KEYALL),
    Long("long", Tag.LONG, KEYALL),
    Float("float", Tag.FLOAT, KEYALL),
    Double("double", Tag.DOUBLE, KEYALL),
    False("false", Tag.FALSE, BOOLSUPPORT),
    Unsigned("unsigned", Tag.UNSIGNED, KEYALL),
    Signed("singed", Tag.SIGNED, KEYALL),
    Struct("struct", Tag.STRUCT, KEYALL),
    Union("union", Tag.UNION, KEYALL),
    Enum("enum", Tag.ENUM, KEYALL),
    Complex("_Complex", Tag.COMPLEX, KEYC99),
    Imaginary("_Imaginary", Tag.IMAGINARY, KEYC99),
    

    // storage class specifier
    Static("static", Tag.STATIC),
    Register("register", Tag.REGISTER),
    Extern("extern", Tag.EXTERN),
    Typedef("typedef", Tag.TYPEDEF),
    Auto("auto", Tag.AUTO),

    // qualifier
    Const("const", Tag.CONST),
    Volatile("volatile", Tag.VOLATILE),
    Restrict("restrict", Tag.RESTRICT),

    // function-specifier
    Inline("inline", Tag.INLINE, KEYC99|KEYGNU),

    // statement
    Break("break", Tag.BREAK),
    Case("case", Tag.CASE),
    Continue("continue", Tag.CONTINUE),
    Default("default", Tag.DEFAULT),
    Do("do", Tag.DO),
    Else("else", Tag.ELSE),
    For("for", Tag.FOR),
    Goto("goto", Tag.GOTO),
    If("if", Tag.IF),
    Return("return", Tag.RETURN),
    Sizeof("sizeof", Tag.SIZEOF),
    Switch("switch", Tag.SWITCH),
    While("while", Tag.WHILE),

    // C99 6.4.6: Punctuators.
    l_paren("(", Tag.LPAREN),
    r_paren(")", Tag.RPAREN),
    l_brace("(", Tag.LBRACE),
    r_brace(")", Tag.RBRACE),
    l_bracket("[", Tag.LBRACKET),
    r_bracket("]", Tag.RBRACKET),
    semi(";", Tag.SEMI),
    comma(",", Tag.COMMA),

    dot(".", Tag.DOT),
    equal("=", Tag.EQ),
    greater(">", Tag.GT),
    less("<", Tag.LT),
    bang("!", Tag.BANG),          // '!'
    tilde("~", Tag.TILDE),       // '~'
    question("?", Tag.QUES),       // '?'
    colon(":", Tag.COLON),       // ':'
    equalequal("==", Tag.EQEQ),
    lessequal("<=", Tag.LTEQ),
    greaterequal(">=", Tag.GTEQ),
    bangequal("!=", Tag.BANGEQ),      // '!='
    ampamp("&&", Tag.AMPAMP),    // '&&'
    barbar("||", Tag.BARBAR),    // '||'
    plusplus("++", Tag.PLUSPLUS),
    subsub("--", Tag.SUBSUB),
    plus("+", Tag.PLUS),
    sub("-", Tag.SUB),
    arrow("->", Tag.SUBGT),
    star("*", Tag.STAR),        // '*'
    slash("/", Tag.SLASH),       // '/'
    amp("&", Tag.AMP),        // '&'
    bar("|", Tag.BAR),          // '|'
    caret("^", Tag.CARET),        // '^'
    percent("%", Tag.PERCENT),    // '%'
    lessless("<<", Tag.LTLT),
    greatergreater(">>", Tag.GTGT),
    plusequal("+=", Tag.PLUSEQ),
    subequal("-=", Tag.SUBEQ),
    starequal("*=", Tag.STAREQ),
    slashequal("/=", Tag.SLASHEQ),
    ampequal("&=", Tag.AMPEQ),
    barequal("|=", Tag.BAREQ),
    caretequal("^=", Tag.CARETEQ),
    percentequal("%=", Tag.PERCENTEQ),
    lesslessequal("<<=", Tag.LTLTEQ),
    greatergreaterequal(">>=", Tag.GTGTEQ),

    // for variable argument
    ellipsis("...", Tag.ELLIPSIS),

    hash("#", Tag.HASH),
    hashhash("##", Tag.HASHHASH);

    public final String name;
    public final int tokenID;
    public final int flags;

    TokenKind(String name, int id, int flags)
    {
        this.name = name;
        tokenID = id;
        this.flags = flags;
    }

    TokenKind(String name, int id)
    {
        this(name, id, 0);
    }
}
