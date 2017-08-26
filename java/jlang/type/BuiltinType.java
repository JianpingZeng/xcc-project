package jlang.type;
/*
 * Xlous C language CompilerInstance
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

import jlang.support.LangOptions;
import jlang.support.PrintingPolicy;

/**
 * This class represents the primitive type of C language.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class BuiltinType extends Type
{
    /**
     * Constructor with one parameter which represents the kind of jlang.type
     * for reason of comparison convenient.
     *
     * @param tag
     */
    public BuiltinType(int tag)
    {
        super(tag, new QualType());
    }

    @Override
    public String getAsStringInternal(String inner, PrintingPolicy policy)
    {
        return inner + getName(policy.opts);
    }

    public String getName(LangOptions opts)
    {
        switch (tc)
        {
            default: assert false :"Uknown builtin type!";
            case Void: return "void";
            case Bool: return opts.bool ? "bool" : "_Bool";
            case SChar: return "char";
            case Char_U: return "unsigned char";
            case Short: return "short";
            case UShort: return "unsigned short";
            case Int: return "int";
            case UInt: return "unsigned int";
            case Long: return "long";
            case ULong: return "unsigned long";
            case LongLong: return "long long";
            case ULongLong: return "unsigned long long";
            case Float: return "float";
            case Double : return "double";
            case LongDouble: return "long double";
        }
    }
}
