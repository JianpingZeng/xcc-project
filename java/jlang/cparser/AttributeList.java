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

package jlang.cparser;

import jlang.clex.IdentifierInfo;
import jlang.support.SourceLocation;

/**
 * Represents GCC's __attribute__ declaration. There are 4 forms of this
 * construct.
 * they are:
 *
 * 1: __attribute__(( const )). ParmName/Args/NumArgs will all be unused.
 * 2: __attribute__(( mode(byte) )). ParmName used, Args/NumArgs unused.
 * 3: __attribute__(( format(printf, 1, 2) )). ParmName/Args/NumArgs all used.
 * 4: __attribute__(( aligned(16) )). ParmName is unused, Args/Num used.
 * @author Xlous.zeng
 * @version 0.1
 */
public class AttributeList
{
    /**
     * Keeps this list alphabetized.
     */
    public enum Kind
    {
        AT_IBOutlet,          // Clang-specific.
        AT_address_space,
        AT_alias,
        AT_aligned,
        AT_always_inline,
        AT_analyzer_noreturn,
        AT_annotate,
        AT_blocks,
        AT_cleanup,
        AT_const,
        AT_constructor,
        AT_deprecated,
        AT_destructor,
        AT_dllexport,
        AT_dllimport,
        AT_ext_vector_type,
        AT_fastcall,
        AT_format,
        AT_format_arg,
        AT_gnu_inline,
        AT_malloc,
        AT_mode,
        AT_nodebug,
        AT_noinline,
        AT_no_instrument_function,
        AT_nonnull,
        AT_noreturn,
        AT_nothrow,
        AT_packed,
        AT_pure,
        AT_regparm,
        AT_section,
        AT_sentinel,
        AT_stdcall,
        AT_transparent_union,
        AT_unavailable,
        AT_unused,
        AT_used,
        AT_vector_size,
        AT_visibility,
        AT_warn_unused_result,
        AT_weak,
        AT_weak_import,
        AT_reqd_wg_size,
        IgnoredAttribute,
        UnknownAttribute
    }

    private IdentifierInfo attrName;
    private SourceLocation attrLoc;
    private IdentifierInfo paramName;
    private SourceLocation paramLoc;
    private Object[] args;
    private AttributeList next;
    private boolean declspecAttribute;

    public AttributeList(IdentifierInfo attrName,
            SourceLocation attrLoc,
            IdentifierInfo paramName,
            SourceLocation paramLoc,
            Object[] args,
            AttributeList next)
    {
        this(attrName, attrLoc, paramName, paramLoc, args, next, false);
    }

    public AttributeList(IdentifierInfo attrName,
            SourceLocation attrLoc,
            IdentifierInfo paramName,
            SourceLocation paramLoc,
            Object[] args,
            AttributeList next,
            boolean declspec)
    {
        this.attrName = attrName;
        this.attrLoc = attrLoc;
        this.paramName = paramName;
        this.paramLoc = paramLoc;
        if (args == null || args.length <= 0)
            this.args = null;
        else
        {
            this.args = new Object[args.length];
            System.arraycopy(args, 0, this.args, 0, this.args.length);
        }

        this.next = next;
        this.declspecAttribute = declspec;
    }

    public IdentifierInfo getAttrName()
    {
        return attrName;
    }

    public SourceLocation getAttrLoc()
    {
        return attrLoc;
    }

    public IdentifierInfo getParamName()
    {
        return paramName;
    }

    public SourceLocation getParamLoc()
    {
        return paramLoc;
    }

    public boolean isDeclspecAttribute()
    {
        return declspecAttribute;
    }

    public Kind getKind()
    {
        return getKind(getAttrName());
    }

    public AttributeList getNext()
    {
        return next;
    }

    public void setNext(AttributeList next)
    {
        this.next = next;
    }

    public void addAttributeList(AttributeList alist)
    {
        assert alist != null:"addAttributeList(): alist is null";
        AttributeList next = this, prev;
        do
        {
            prev = next;
            next = next.getNext();
        }while (next != null);
        prev.setNext(alist);
    }

    public int getNumArgs()
    {
        return args == null ? 0 : args.length;
    }

    public Object getArg(int index)
    {
        assert index < getNumArgs() :"Arg access out of range";
        return args[index];
    }

    public Object[] getArgs()
    {
        return args;
    }

    public static Kind getKind(IdentifierInfo name)
    {
        String str = name.getName();
        int len = str.length();

        // Normalize the attribute name, __foo__ becomes foo.
        if (len > 4 && str.startsWith("__") && str.endsWith("__"))
        {
            str = str.substring(2, len - 2);
            len -= 4;
        }

        switch (str)
        {
            case "weak":
                return Kind.AT_weak;
            case "pure":
                return Kind.AT_pure;
            case "mode":
                return Kind.AT_mode;
            case "used":
                return Kind.AT_used;
            case "alias":
                return Kind.AT_alias;
            case "const":
                return Kind.AT_const;
            case "packed":
                return Kind.AT_packed;
            case "malloc":
                return Kind.AT_malloc;
            case "format":
                return Kind.AT_format;
            case "unused":
                return Kind.AT_unused;
            case "blocks":
                return Kind.AT_blocks;
            case "aligned":
                return Kind.AT_aligned;
            case "cleanup":
                return Kind.AT_cleanup;
            case "nodebug":
                return Kind.AT_nodebug;
            case "nonnull":
                return Kind.AT_nonnull;
            case "nothrow":
                return Kind.AT_nothrow;
            case "regparm":
                return Kind.AT_regparm;
            case "section":
                return Kind.AT_section;
            case "stdcall":
                return Kind.AT_stdcall;
            case "annotate":
                return Kind.AT_annotate;
            case "noreturn":
                return Kind.AT_noreturn;
            case "noinline":
                return Kind.AT_noinline;
            case "fastcall":
                return Kind.AT_fastcall;
            case "iboutlet":
                return Kind.AT_IBOutlet;
            case "sentinel":
                return Kind.AT_sentinel;
            case "dllimport":
                return Kind.AT_dllimport;
            case "dllexport":
                return Kind.AT_dllexport;
            case "my_alias":
                return Kind.IgnoredAttribute;
            case "deprecated":
                return Kind.AT_deprecated;
            case "visibility":
                return Kind.AT_visibility;
            case "destructor":
                return Kind.AT_destructor;
            case "format_arg":
                return Kind.AT_format_arg;
            case "gnu_inline":
                return Kind.AT_gnu_inline;
            case "weak_import":
                return Kind.AT_weak_import;
            case "vector_size":
                return Kind.AT_vector_size;
            case "constructor":
                return Kind.AT_constructor;
            case "unavailable":
                return Kind.AT_unavailable;
            case "address_space":
                return Kind.AT_address_space;
            case "always_inline":
                return Kind.AT_always_inline;
            case "vec_type_hint":
                return Kind.IgnoredAttribute;
            case "ext_vector_type":
                return Kind.AT_ext_vector_type;
            case "transparent_union":
                return Kind.AT_transparent_union;
            case "analyzer_noreturn":
                return Kind.AT_analyzer_noreturn;
            case "warn_unused_result":
                return Kind.AT_warn_unused_result;
            case "reqd_work_group_size":
                return Kind.AT_reqd_wg_size;
            case "no_instrument_function":
                return Kind.AT_no_instrument_function;
            default:
                return Kind.UnknownAttribute;
        }
    }
}
