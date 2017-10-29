/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

package jlang.ast;

/**
 * This represents the kind of attr.
 * @author Xlous.zeng
 * @version 0.1
 */
public enum AttrKind
{
    Alias,
    Aligned,
    AlwaysInline,
    AnalyzerNoReturn, // Clang-specific.
    Annotate,
    AsmLabel, // Represent GCC asm label extension.
    Blocks,
    Cleanup,
    Const,
    Constructor,
    DLLExport,
    DLLImport,
    Deprecated,
    Destructor,
    FastCall,
    Format,
    FormatArg,
    GNUInline,
    IBOutletKind, // Clang-specific.  Use "Kind" suffix to not conflict with
    Malloc,
    NoReturn,
    NoThrow,
    Nodebug,
    Noinline,
    NonNull,
    Packed,
    PragmaPack,
    Pure,
    Regparm,
    ReqdWorkGroupSize,   // OpenCL-specific
    Section,
    Sentinel,
    StdCall,
    TransparentUnion,
    Unavailable,
    Unused,
    Used,
    Visibility,
    WarnUnusedResult,
    Weak,
    WeakImport
}
