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

package backend.codegen.dagisel;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public enum CondCode {
  // Opcode          N U L G E       Intuitive operation
  SETFALSE,      //    0 0 0 0       Always false (always folded)
  SETOEQ,        //    0 0 0 1       True if ordered and equal
  SETOGT,        //    0 0 1 0       True if ordered and greater than
  SETOGE,        //    0 0 1 1       True if ordered and greater than or equal
  SETOLT,        //    0 1 0 0       True if ordered and less than
  SETOLE,        //    0 1 0 1       True if ordered and less than or equal
  SETONE,        //    0 1 1 0       True if ordered and operands are unequal
  SETO,          //    0 1 1 1       True if ordered (no nans)
  SETUO,         //    1 0 0 0       True if unordered: isnan(X) | isnan(Y)
  SETUEQ,        //    1 0 0 1       True if unordered or equal
  SETUGT,        //    1 0 1 0       True if unordered or greater than
  SETUGE,        //    1 0 1 1       True if unordered, greater than, or equal
  SETULT,        //    1 1 0 0       True if unordered or less than
  SETULE,        //    1 1 0 1       True if unordered, less than, or equal
  SETUNE,        //    1 1 1 0       True if unordered or not equal
  SETTRUE,       //    1 1 1 1       Always true (always folded)
  // Don't care operations: undefined if the input is a nan.
  SETFALSE2,     //  1 X 0 0 0       Always false (always folded)
  SETEQ,         //  1 X 0 0 1       True if equal
  SETGT,         //  1 X 0 1 0       True if greater than
  SETGE,         //  1 X 0 1 1       True if greater than or equal
  SETLT,         //  1 X 1 0 0       True if less than
  SETLE,         //  1 X 1 0 1       True if less than or equal
  SETNE,         //  1 X 1 1 0       True if not equal
  SETTRUE2,      //  1 X 1 1 1       Always true (always folded)

  SETCC_INVALID;       // Marker value.

  public boolean isSignedIntSetCC() {
    return this == SETGT || this == SETGE ||
        this == SETLT || this == SETLE;
  }

  public boolean isUnsignedIntSetCC() {
    return this == SETUGT || this == SETUGE ||
        this == SETULT || this == SETULE;
  }

  public boolean isTrueWhenEqual() {
    return (this.ordinal() & 1) != 0;
  }

  public int getUnorderedFlavor() {
    return (ordinal() >> 3) & 3;
  }
}
