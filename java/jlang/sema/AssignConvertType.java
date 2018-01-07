/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
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

package jlang.sema;

/**
 * All of the 'assignment' semantic checks return this
 * enum to indicate whether the assignment was allowed.  These checks are
 * done for simple assignments, as well as initialization, return from
 * function, argument passing, etc.  The query is phrased in terms of a
 * source and destination type.
 * @author Xlous.zeng
 * @version 0.1
 */
enum AssignConvertType
{
    /**
     * the types are compatible according to the standard.
     */
    Compatible,

    /**
     * The assignment converts a pointer to an int, which we
     * accept as an extension.
     */
    PointerToInt,

    /**
     * The assignment converts an int to a pointer, which we
     * accept as an extension.
     */
    IntToPointer,

    /**
     * The assignment is between a function pointer and
     * void*, which the standard doesn't allow, but we accept as an extension.
     */
    FunctionVoidPointer,

    /**
     * The assignment is between two pointers types that
     * are not compatible, but we accept them as an extension.
     */
    IncompatiblePointer,

    /**
     * The assignment is between two pointers types which
     * point to integers which have a different sign, but are otherwise identical.
     * This is a subset of the above, but broken out because it's by far the most
     * common case of incompatible pointers.
     */
    IncompatiblePointerSign,

    IncompatibleNestedPointerQualifiers,

    /**
     * The assignment discards
     * c/v/r qualifiers, which we accept as an extension.
     */
    CompatiblePointerDiscardsQualifiers,

    /**
     * We reject this conversion outright, it is invalid to
     * represent it in the AST.
     */
    Incompatible
}
