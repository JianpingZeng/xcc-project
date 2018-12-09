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
 * These are precedences for the binary/ternary
 * operators in the C99 grammar.  These have been named to relate
 * with the C99 grammar productions.  Low precedences numbers bind
 * more weakly than high numbers.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public final class PrecedenceLevel {
  public static final int Unkonw = 0;   // Not binary operator.
  public static final int Comma = 1;    // ,
  public static final int Assignment = 2;    // =, *=, /=, %=, +=, -=, <<=, >>=, &=, ^=, |=
  public static final int Conditional = 3;    // ?
  public static final int LogicalOr = 4;    // ||
  public static final int LogicalAnd = 5;    // &&
  public static final int InclusiveOr = 6;    // |
  public static final int ExclusiveOr = 7;    // ^
  public static final int And = 8;    // &
  public static final int Equality = 9;    // ==, !=
  public static final int Relational = 10;   //  >=, <=, >, <
  public static final int Shift = 11;   // <<, >>
  public static final int Additive = 12;   // -, +
  public static final int Multiplicative = 13;   // *, /, %
  public static final int PointerToMember = 14;    // .*, ->*
}
