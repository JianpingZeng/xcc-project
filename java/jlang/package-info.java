/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous
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
package jlang;

/**
 * I implements this project in term of {@linkplain http://clang.llvm.org/ Clang},
 * so it is named of {@code JlangCC} to acknowledge LLVM and Clang open source
 * project.
 * </p>
 * This package defines a collection of classes were served as performing
 * tokenization, parsing, and following semantics checking.
 * </p>
 * The {@linkplain ast} package holds all of classes for represents each grammar
 * node with an {@linkplain https://en.wikipedia.org/wiki/Abstract_syntax_tree}.
 * </p>
 * The {@linkplain codegen} package is the container where all classes were used
 * for generating LLVM IR from C source code live in.
 * </p>
 * All of classes live in the {@linkplain cparser} package were responsible for
 * reading the character stream from input file or input device, converting it
 * to a stream of tokens, then performing parsing on tokens stream, also generating
 * an AST for each compilation unit (usually a C header or source file) as a subtask.
 * </p>
 * The {@linkplain exception} package defines all classes for some internal error
 * report.
 * </p>
 * The {@linkplain sema} package is used for performing semantics checking when
 * parsing.
 * </p>
 * The {@linkplain type} package defines so many classes which represent the each
 * type in C language in terms of {@linkplain http://www.open-std.org/jtc1/sc22/wg14/www/docs/n1256.pdf
 * C language standard}.
 */