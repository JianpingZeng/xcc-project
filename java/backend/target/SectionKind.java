package backend.target;
/*
 * Extremely C language Compiler
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
public enum SectionKind
{
    Unknown,      ///< Custom section
    Text,             ///< Text section
    Data,             ///< Data section
    BSS,              ///< BSS section
    ROData,           ///< Readonly data section
    RODataMergeStr,   ///< Readonly data section (mergeable strings)
    RODataMergeConst, ///< Readonly data section (mergeable constants)
    SmallData,        ///< Small data section
    SmallBSS,         ///< Small bss section
    SmallROData,      ///< Small readonly section
    ThreadData,       ///< Initialized TLS data objects
    ThreadBSS;        ///< Uninitialized TLS data objects

    public static boolean isReadOnly(SectionKind k)
    {
        return k == ROData || k == RODataMergeConst ||
                k == RODataMergeStr || k == SmallROData;
    }

    public static boolean isBSS(SectionKind k)
    {
        return k == BSS || k == SmallBSS;
    }
}
