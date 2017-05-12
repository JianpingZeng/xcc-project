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

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public enum PPKeyWordKind
{
    // These have meaning after a '#' at the start of a line. These define enums in
    // the tok::pp_* namespace.  Note that IdentifierInfo::getPPKeywordID must be
    // manually updated if something is added here.
    pp_not_keyword,

    // C99 6.10.1 - Conditional Inclusion.
    pp_if,
    pp_ifdef,
    pp_ifndef,
    pp_elif,
    pp_else,
    pp_endif,
    pp_defined,

    // C99 6.10.2 - Source File Inclusion.
    pp_include,
    pp___include_macros,

    // C99 6.10.3 - Macro Replacement.
    pp_define,
    pp_undef,

    // C99 6.10.4 - Line Control.
    pp_line,

    // C99 6.10.5 - Error Directive.
    pp_error,

    // C99 6.10.6 - Pragma Directive.
    pp_pragma,
}
