package jlang.basic;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
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

import jlang.clex.IdentifierTable;
import tools.Pair;
import tools.Util;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class Context
{
    private static Info[] BuiltinInfo = new Info[BuiltID.FirstTSBuiltin];

    static
    {
        BuiltinInfo[0] = new Info("not a builtin function", null,
                null, null, false);
        for (Builtin.BUILTIN item : Builtin.BUILTIN.values())
        {
            BuiltinInfo[item.id] = new Info(item.name, item.type, item.attr,
                    null, false);
        }

        for (Builtin.LIBBUILTIN item : Builtin.LIBBUILTIN.values())
        {
            BuiltinInfo[item.id] = new Info(item.name, item.type, item.attr,
                    item.header, false);
        }
    }
    /**
     * Target specific Builtin function information.
     */
    private Info[] tsRecords;

    public Context(TargetInfo target)
    {
        tsRecords = target.getTargetBuiltins();
    }
    public void initializeBuiltin(IdentifierTable table)
    {
        initializeBuiltin(table, false);
    }

    /**
     * Mark the identifiers for all the builtins with their
     * appropriate builtin ID # and mark any non-portable builtin identifiers as
     * such.
     * @param table
     * @param noBuiltin
     */
    public void initializeBuiltin(IdentifierTable table, boolean noBuiltin)
    {
        // Step 1, add target-independent builtins.
        for (int i = BuiltID.NotBuiltin + 1; i != BuiltID.FirstTSBuiltin; i++)
        {
            Info item = BuiltinInfo[i];
            if (!item.isSuppressed() &&
                    (!noBuiltin || (
                            item.getAttributes() != null &&
                            item.getAttributes().indexOf('f') == -1)))
                table.get(item.getName()).setBuiltID(i);
        }

        // Step 2, add target specific builtins.
        for (int i = 0; i < tsRecords.length; i++)
        {
            Info item = tsRecords[i];
            if (!item.isSuppressed() &&
                    (!noBuiltin || (
                            item.getAttributes() != null &&
                                    item.getAttributes().indexOf('f') == -1)))
            {
                table.get(item.getName()).setBuiltID(i + BuiltID.FirstTSBuiltin);
            }
        }
    }

    /**
     * Populate the String with the names of all the builtins.
     * @param noBuiltins
     * @return
     */
    public String getBuiltinNames(boolean noBuiltins)
    {
        StringBuilder sb = new StringBuilder();
        // Step 1,find target-independent builtins.
        for (int i = BuiltID.NotBuiltin + 1; i != BuiltID.FirstTSBuiltin; i++)
        {
            Info item = BuiltinInfo[i];
            if (!item.isSuppressed() &&
                    (!noBuiltins || (
                            item.getAttributes() != null &&
                                    item.getAttributes().indexOf('f') == -1)))
                sb.append(item.getName());
        }

        // Step 2, add target specific builtins.
        for (int i = 0; i < tsRecords.length; i++)
        {
            Info item = tsRecords[i];
            if (!item.isSuppressed() &&
                    (!noBuiltins || (
                            item.getAttributes() != null &&
                                    item.getAttributes().indexOf('f') == -1)))
            {
                sb.append(item.getName());
            }
        }
        return sb.toString();
    }

    /**
     * Get the builtin name of specified id.
     * @param id
     * @return
     */
    public String getName(int id)
    {
        return getRecord(id).getName();
    }

    public String getTypeString(int id)
    {
        return getRecord(id).getType();
    }

    /**
     * Return true if this function has no side effects and doesn't
     * read memory
     * @param id
     * @return
     */
    public boolean isConst(int id)
    {
        return getRecord(id).getAttributes().indexOf('c') != -1;
    }

    public boolean isNoThrow(int id)
    {
        return getRecord(id).getAttributes().indexOf('n') != -1;
    }

    public boolean isNoReturn(int id)
    {
        return getRecord(id).getAttributes().indexOf('r') != -1;
    }

    public boolean isLibFunction(int id)
    {
        return getRecord(id).getAttributes().indexOf('F') != -1;
    }

    public boolean isPredefinedLibFunction(int id)
    {
        return getRecord(id).getAttributes().indexOf('f') != -1;
    }

    public String getHeaderName(int id)
    {
        return getRecord(id).getHeaderName();
    }

    /**
     * Determine whether this builtin is like printf in its
     * formatting rules and, if so, set the index to the format string
     * argument and whether this function as a va_list argument.
     * @param id
     * @param res
     * @return
     */
    public boolean isPrintfLike(int id, Pair<Integer, Boolean> res)
    {
        String attr = getRecord(id).getAttributes();
        int idx = Util.strpbrk(attr, "Pp");
        if (idx < 0)
            return false;

        res.second = attr.charAt(idx) == 'P';

        ++idx;
        assert attr.charAt(idx) == ':' :
                "p or P specifier must have be followed by ':'";
        ++idx;
        assert attr.indexOf(':', idx) != -1 :"printf specifier must end with ':'";
        res.first = Integer.parseInt(attr.substring(idx), 10);
        return true;
    }

    /**
     * Return true of the specified builtin uses __builtin_va_list
     * as an operand or return type.
     * @return
     */
    public boolean hasVALListUse(int id)
    {
        return getRecord(id).getAttributes().contains("Aa");
    }

    public boolean isConstWithoutErrno(int id)
    {
        return getRecord(id).getAttributes().indexOf('e') != -1;
    }

    private Info getRecord(int id)
    {
        assert id >= 0;
        if (id < BuiltID.FirstTSBuiltin)
            return BuiltinInfo[id];

        assert id - BuiltID.FirstTSBuiltin < tsRecords.length
                :"Invalid Builtin ID!";
        return tsRecords[id - BuiltID.FirstTSBuiltin];
    }
}
