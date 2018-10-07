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
public enum UnaryOperatorKind {
  UO_PostInc, UO_PostDec, // [C99 6.5.2.4] Postfix increment and decrement
  UO_PreInc, UO_PreDec,   // [C99 6.5.3.1] Prefix increment and decrement
  UO_AddrOf, UO_Deref,    // [C99 6.5.3.2] Address and indirection
  UO_Plus, UO_Minus,      // [C99 6.5.3.3] UnaryExpr arithmetic
  UO_Not, UO_LNot,        // [C99 6.5.3.3] UnaryExpr arithmetic
  UO_Real, UO_Imag,       // "__real expr"/"__imag expr" Extension.
  UO_Extension,           // __extension__ marker
  UO_Offsetof             //  builtin_offsetof
}
