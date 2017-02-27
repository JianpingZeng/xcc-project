package backend.codegen;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2016, Xlous
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
 * Machine Value Type, which contains the various low-level value types.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class MVT
{
    public enum ValueType
    {
        // If you change this numbering, you must change the values in Target.td as
        // well!
        Other,
        i1,
        i8,
        i16,
        i32,
        i64,
        i128,

        f32,
        f64,
        f80,
        f128,

        isVoid,

        // The pointer type, just use for tblgen internally.
        isPtr
    }

    public static int getSizeInBits(ValueType valueType)
    {
        switch (valueType)
        {
            default:
                assert false:"Value type has no known size!";
                return 0;
            case i1: return 1;
            case i8: return 8;
            case i16: return 16;
            case i32:
            case f32:
                return 32;
            case i64:
            case f64:
                return 64;
            case f80:
                return 80;
            case f128:
            case i128:
                return 128;
        }
    }
}
