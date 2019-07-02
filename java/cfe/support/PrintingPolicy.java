package cfe.support;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2019, Jianping Zeng.
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
 * Describes how types, statements, expressions, and
 * declarations should be printed.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public final class PrintingPolicy {
  /**
   * The number of spaces to use to indent each line.
   */
  public int indentation;

  /**
   * What language we're printing.
   */
  public LangOptions opts;

  /**
   * Whether we should suppress printing of the actual specifiers for
   * /// the given type or declaration.
   * ///
   * /// This flag is only used when we are printing declarators beyond
   * /// the first declarator within a declaration group. For example, given:
   * ///
   * /// \code
   * /// const int *x, *y;
   * /// \endcode
   * ///
   * /// SuppressSpecifiers will be false when printing the
   * /// declaration for "x", so that we will print "int *x"; it will be
   * /// \c true when we print "y", so that we suppress printing the
   * /// "const int" type specifier and instead only print the "*y".
   */
  public boolean suppressSpecifiers;

  /**
   * Whether type printing should skip printing the actual stmtClass type.
   * ///
   * /// This is used when the caller needs to print a stmtClass definition in front
   * /// of the type, as in constructs like the following:
   * ///
   * /// \code
   * /// typedef struct { int x, y; } Point;
   * /// \endcode
   */
  public boolean suppressTag;

  /**
   * If we are printing a stmtClass type, suppresses printing of the
   * /// kind of stmtClass, e.g., "struct", "union", "enum".
   */
  public boolean suppressTagKind;
  /**
   * True when we are "dumping" rather than "pretty-printing",
   * /// where dumping involves printing the internal details of the AST
   * /// and pretty-printing involves printing something similar to
   * /// source code.
   */
  public boolean dump;
  /**
   * Whether we should print the sizes of constant array expressions
   * /// as written in the sources.
   * ///
   * /// This flag is determines whether arrays types declared as
   * ///
   * /// \code
   * /// int a[4+10*10];
   * /// char a[] = "A string";
   * /// \endcode
   * ///
   * /// will be printed as written or as follows:
   * ///
   * /// \code
   * /// int a[104];
   * /// char a[9] = "A string";
   * /// \endcode
   */
  public boolean constantArraySizeAsWritten;

  public PrintingPolicy(LangOptions opts) {
    indentation = 2;
    this.opts = opts;
  }

  public PrintingPolicy(PrintingPolicy policy) {
    indentation = policy.indentation;
    opts = policy.opts;
    suppressSpecifiers = policy.suppressSpecifiers;
    suppressTag = policy.suppressTag;
    suppressTagKind = policy.suppressTagKind;
    dump = policy.dump;
    constantArraySizeAsWritten = policy.constantArraySizeAsWritten;
  }
}
