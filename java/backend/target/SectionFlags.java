package backend.target;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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
public class SectionFlags
{
    public static final int Invalid    = -1;
    public static final int None       = 0;
    public static final int Code       = 1 << 0;  ///< Section contains code
    public static final int Writeable  = 1 << 1;  ///< Section is writeable
    public static final int BSS        = 1 << 2;  ///< Section contains only zeroes
    public static final int Mergeable  = 1 << 3;  ///< Section contains mergeable data
    public static final int Strings    = 1 << 4;  ///< Section contains C-type strings
    public static final int TLS        = 1 << 5;  ///< Section contains thread-local data
    public static final int Debug      = 1 << 6;  ///< Section contains debug data
    public static final int Linkonce   = 1 << 7;  ///< Section is linkonce
    public static final int Small      = 1 << 8;  ///< Section is small
    public static final int TypeFlags  = 0xFF;
    // Some gap for future flags
    public static final int Named      = 1 << 23; ///< Section is named
    public static final int EntitySize = 0xFF << 24; ///< Entity size for mergeable stuff

    public static int getEntitySize(int Flags) {
    return (Flags >> 24) & 0xFF;
}

    public int setEntitySize(int Flags, int Size) {
    return ((Flags & ~EntitySize) | ((Size & 0xFF) << 24));
}
}
