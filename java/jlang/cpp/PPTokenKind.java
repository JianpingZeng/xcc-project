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

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public enum PPTokenKind
{
    UNKNOWN("UNKNOWN"),
    AND("&"),
    ARROW("->"),
    AT("@"),
    BANG("!"),
    CCOMMENT("CCOMMENT"),
    CHARACTER("CHARACTER"),
    CPPCOMMENT("CPPCOMMENT"),
    CARET("^"),
    COLON(":"),
    COMMA(","),
    DEC("--"),
    DIV("/"),
    DIV_EQ("/="),
    DOT("."),
    ELLIPSIS("..."),
    EOF("EOF"),
    EQ("="),
    EQEQ("=="),
    GE(">="),
    GT(">"),
    HASH("#"),
    HEADER("HEADER"),
    IDENTIFIER("IDENTIFIER"),
    INC("++"),
    LAND("&&"),
    LAND_EQ("&&="),
    LBRACE("{"),
    LBRACKET("["),
    LE("<="),
    LITERAL("LITERAL"),
    LOR("||"),
    LOR_EQ("||="),
    LPAREN("("),
    LSH("<<"),
    LSH_EQ("<<="),
    LT("<"),
    MOD_EQ("%="),
    MUL("*"),
    MULTI_EQ("*="),
    NE("!="),
    NL("NEW_LINE"),
    NUMBER("NUMBER"),
    OR("|"),
    OR_EQ("|="),
    PASTE("##"),
    PERCENTAGE("%"),
    PERCENTAGE_EQ("%="),
    PLUS("+"),
    PLUS_EQ("+="),
    QUES("?"),
    RANGE(".."),
    RBRACE("}"),
    RBRACKET("]"),
    RPAREN(")"),
    RSH(">>"),
    RSH_EQ(">>="),
    SEMI(";"),
    SINGLE_QUOTE("'"),
    SQSTRING("SQSTRING"),
    STRING("STRING"),
    SUB("-"),
    SUB_EQ("-="),
    TILDE("~"),
    WHITESPACE("WHITE_SPACE"),
    XOR("^"),
    XOR_EQ("^="),
    M_ARG("M_ARG"),
    M_PASTE("M_PASTE"),
    M_STRING("M_STRING"),
    P_LINE("P_LINE");

    String text;

    PPTokenKind(String text)
    {
        this.text = text;
    }
}
