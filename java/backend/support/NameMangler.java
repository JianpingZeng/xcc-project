package backend.support;
/*
 * Extremely C language Compiler
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

import backend.value.Module;
import backend.type.Type;
import backend.value.Function;
import backend.value.GlobalValue;
import backend.value.GlobalVariable;
import backend.value.Value;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.HashMap;
import java.util.HashSet;

/**
 * This file defines a class which responsible for make asmName mangling for global
 * linkage entity of a module.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class NameMangler
{
    /**
     * This string is added to each symbol that is emitted, unless the
     * symbol is marked as not needed this prefix.
     */
    private String prefix;
    /**
     * If this set, the target accepts global names in input.
     * e.g. "foo bar" is a legal asmName. This syntax is used instead
     * of escape the space character. By default, this is false.
     */
    private boolean useQuotes;
    /**
     * Memorize the asmName that we assign a value.
     */
    private HashMap<Value, String> memo;

    private int count;

    private TObjectIntHashMap<Type> typeMap;
    private int typeCount;
    /**
     * Keep track of which global value is mangled in the current module.
     */
    private HashSet<GlobalValue> mangledGlobals;

    private int acceptableChars[];

    public NameMangler(Module m)
    {
        this(m, "");
    }

    public NameMangler(Module m, String prefix)
    {
        this.prefix = prefix;
        useQuotes = false;
        memo = new HashMap<>();
        count = 0;
        typeMap = new TObjectIntHashMap<>();
        typeCount = 0;
        mangledGlobals = new HashSet<>();
        acceptableChars = new int[256/32];

        // Letters and numbers are acceptable.
        for (char x = 'a'; x <= 'z'; x++)
            markCharAcceptable(x);

        for (char x = 'A'; x <= 'Z'; x++)
            markCharAcceptable(x);

        for (char x = '0'; x <= '9'; x++)
            markCharAcceptable(x);

        // These chars are acceptable.
        markCharAcceptable('_');
        markCharAcceptable('$');
        markCharAcceptable('.');

        HashMap<String, GlobalValue> names = new HashMap<>();
        for (Function fn : m.getFunctionList())
            insertName(fn, names);

        for (GlobalVariable gv : m.getGlobalVariableList())
            insertName(gv, names);
    }

    private void insertName(GlobalValue gv, HashMap<String, GlobalValue> names)
    {
        if (!gv.hasName()) return;

        // Figure out if this is already used.
        GlobalValue existingValue = names.get(gv.getName());
        if (existingValue == null)
            existingValue = gv;
        else
        {
            // If GV is external but the existing one is static, mangle the existing one
            if (gv.hasExternalLinkage() && !existingValue.hasExternalLinkage())
            {
                mangledGlobals.add(existingValue);
                existingValue = gv;
            }
            else if (gv.hasExternalLinkage() && existingValue.hasExternalLinkage()
                    && gv.isExternal() && existingValue.isExternal())
            {
                // If the two globals both have external inkage, and are both external,
                // don't mangle either of them, we just have some silly type mismatch.
            }
            else
            {
                // otherwise, mangle gv.
                mangledGlobals.add(gv);
            }
        }
    }

    private int getTypeID(Type ty)
    {
        int e = typeMap.get(ty);
        if (e == 0) e = ++typeCount;
        return e;
    }

    public String getValueName(Value v)
    {
        if (v instanceof GlobalValue)
            return getValueName((GlobalValue)v);

        String name = memo.get(v);
        if (name != null)
            return name;

        name = "ltmp_" + String.valueOf(count++) + "_" + String.valueOf(getTypeID(v.getType()));
        memo.put(v, name);
        return name;
    }

    private static int globalID = 0;
    public String getValueName(GlobalValue gv)
    {
        String name = memo.get(gv);
        if (name != null)
            return name;

        if (gv instanceof Function && ((Function)gv).getIntrinsicID() != 0)
        {
            name = gv.getName();
        }
        else if (!gv.hasName())
        {
            int typeUniqueID = getTypeID(gv.getType());
            name = "__unnamed_" + String.valueOf(typeUniqueID) + "_" + globalID++;
        }
        else if (!mangledGlobals.contains(gv))
        {
            name = makeNameProper(gv.getName(), prefix);
        }
        else
        {
            int typeUniqueID = getTypeID(gv.getType());
            name = "1" + typeUniqueID + "_" + makeNameProper(gv.getName(), "");
        }
        memo.put(gv, name);
        return name;
    }

    private static char hexDigit(int v)
    {
        return v < 10 ?  Character.forDigit(v, 10): (char)(v - 10 + (int)'A');
    }

    private String mangleLetter(char digit)
    {
        return "_" + hexDigit(digit >> 4) + hexDigit(digit&15) + "_";
    }

    private String makeNameProper(String origin, String prefix)
    {
        String result = "";
        if (origin.isEmpty()) return origin;

        if (!useQuotes)
        {
            int i = 0;
            if (origin.charAt(i) != '1')
                result = prefix;
            else
                i++;

            if (origin.charAt(i) >= '0' && origin.charAt(i) <= '9')
                result += mangleLetter(origin.charAt(i++));

            for(; i < origin.length(); i++)
            {
                char ch = origin.charAt(i);
                if (!isCharAcceptable(ch))
                    result += mangleLetter(ch);
                else
                    result += ch;
            }
        }
        return result;
    }

    private void markCharAcceptable(char x)
    {
        acceptableChars[x/32] |= 1 << (x&31);
    }

    private void markCharUnacceptale(char x)
    {
        acceptableChars[x/32] &= ~(1 << (x&31));
    }

    private boolean isCharAcceptable(char ch)
    {
        return (acceptableChars[ch/32] & (1 << (ch &31))) != 0;
    }

    public String makeNameProperly(String name)
    {
        return null;
    }

    public void setUseQuotes(boolean val)
    {
        useQuotes = val;
    }
}
