package jlang.clex;
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

import static jlang.clex.IdentifierTable.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public enum TokenKind
{
    // Not a token.
    Unknown("unknown", Tag.UNKNOWN),
    // End of file.
    eof("eof", Tag.EOF),
    // End of preprocessing directive (end of line inside a
    // directive).
    Eod("eod", Tag.EOD),

    eom("eom", Tag.EOM),

    // C99 6.4.9: Comments.
    Comment("comment", Tag.COMMENT), // Comment (only in -E -C[C] mode)

    // C99 6.4.2: Identifiers.
    Identifier("identifier", Tag.IDENTIFIER), // abcde123

    // C99 6.4.4.1: Integer Constants
    // C99 6.4.4.2: Floating Constants
    numeric_constant("numeric_constant", Tag.NUMERIC_CONSTANT), // 0x123

    // C99 6.4.4: Character Constants
    char_constant("char_constant", Tag.CHAR_CONSTANT),

    // C99 6.4.5: String Literals.
    string_literal("string_literal", Tag.STRING_LITERAL), // "foo"
    angle_string_literal("angle_string_literal", Tag.ANGLE_STRING_LITERAL), // <foo>

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
    Static("static", Tag.STATIC, KEYALL),
    Register("register", Tag.REGISTER, KEYALL),
    Extern("extern", Tag.EXTERN, KEYALL),
    Typedef("typedef", Tag.TYPEDEF, KEYALL),
    Auto("auto", Tag.AUTO, KEYALL),

    // qualifier
    Const("const", Tag.CONST, KEYALL),
    Volatile("volatile", Tag.VOLATILE, KEYALL),
    Restrict("restrict", Tag.RESTRICT, KEYALL),

    // function-specifier
    Inline("inline", Tag.INLINE, KEYC99|KEYGNU),

    // statement
    Break("break", Tag.BREAK, KEYALL),
    Case("case", Tag.CASE, KEYALL),
    Continue("continue", Tag.CONTINUE, KEYALL),
    Default("default", Tag.DEFAULT, KEYALL),
    Do("do", Tag.DO, KEYALL),
    Else("else", Tag.ELSE, KEYALL),
    For("for", Tag.FOR, KEYALL),
    Goto("goto", Tag.GOTO, KEYALL),
    If("if", Tag.IF, KEYALL),
    Return("return", Tag.RETURN, KEYALL),
    Sizeof("sizeof", Tag.SIZEOF, KEYALL),
    Switch("switch", Tag.SWITCH, KEYALL),
    While("while", Tag.WHILE, KEYALL),

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
