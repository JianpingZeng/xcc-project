package jlang.sema;
/*
 * Extremely C language CompilerInstance
 * Copyright (c) 2015-2018, Jianping Zeng
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
 * @author Jianping Zeng
 * @version 0.1
 */
public enum BinaryOperatorKind {
  // Operators listed in order of precedence.
  // Note that additions to this should also update the StmtVisitor class.
  BO_Mul("*"), BO_Div("/"), BO_Rem("%"),              // [C99 6.5.5] Multiplicative operators.
  BO_Add("+"), BO_Sub("-"),                           // [C99 6.5.6] Additive operators.
  BO_Shl("<<"), BO_Shr(">>"),                         // [C99 6.5.7] Bitwise shift operators.
  BO_LT("<"), BO_GT(">"), BO_LE("<="), BO_GE(">="),   // [C99 6.5.8] Relational operators.
  BO_EQ("=="), BO_NE("!="),                           // [C99 6.5.9] Equality operators.
  BO_And("&"),                                        // [C99 6.5.10] Bitwise AND operator.
  BO_Xor("^"),                                        // [C99 6.5.11] Bitwise XOR operator.
  BO_Or("|"),                                         // [C99 6.5.12] Bitwise OR operator.
  BO_LAnd("&&"),                                      // [C99 6.5.13] Logical AND operator.
  BO_LOr("||"),                                       // [C99 6.5.14] Logical OR operator.
  BO_Assign("="), BO_MulAssign("*="),                 // [C99 6.5.16] Assignment operators.
  BO_DivAssign("/="), BO_RemAssign("%="),
  BO_AddAssign("+="), BO_SubAssign("-="),
  BO_ShlAssign("<<="), BO_ShrAssign(">>="),
  BO_AndAssign("&="), BO_XorAssign("^="),
  BO_OrAssign("|="),
  BO_Comma(",");                                      // [C99 6.5.17] Comma operator.

  private String name;

  BinaryOperatorKind(String name) {
    this.name = name;
  }

  public String toString() {
    return name;
  }
}
