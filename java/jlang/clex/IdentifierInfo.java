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

import jlang.type.FoldingSetNodeID;
import tools.Pair;

/**
 * One of these records is kept for each identifier that
 * is lexed.  This contains information about whether the token was #define'd,
 * is a language keyword, or if it is a front-end token of some sort (e.g. a
 * variable or function asmName).  The preprocessor keeps this information in a
 * set, and all tok::identifier tokens have a pointer to one of these.
 * @author Xlous.zeng
 * @version 0.1
 */
public class IdentifierInfo implements Cloneable
{
    private TokenKind tokenID;
    /**
     * // True if there is a #define for this.
     */
    private boolean hasMacro;
    /**
     * // True if identifier is a lang extension.
     */
    private boolean isExtension;

    /**
     * True if identifier is poisoned.
     */
    private boolean isPoisoned;
    /**
     * See "RecomputeNeedsHandleIdentifier".
     */
    private boolean needsHandleIdentifier;

    private Pair<String, IdentifierInfo> entry;

    private int builtID;

    public IdentifierInfo()
    {
        tokenID = TokenKind.identifier;
    }

    public String getName()
    {
        if (entry != null)
            return entry.first;
        return null;
    }

    public int getLength()
    {
        return entry != null ? getName().length() : 0;
    }

    /**
     *  Return true if this identifier is #defined to some other value.
     * @return
     */
    public boolean isHasMacroDefinition()
    {
        return hasMacro;
    }

    public void setHasMacroDefinition(boolean val)
    {
        if (val == hasMacro) return;

        hasMacro = val;
        if (val)
            needsHandleIdentifier = true;
        recomputeNeedsHandleIdentifier();
    }

    private void recomputeNeedsHandleIdentifier()
    {
        needsHandleIdentifier = (isPoisoned | hasMacro | isExtensionToken());
    }

    public TokenKind getTokenID()
    {
        return tokenID;
    }

    public void setTokenID(TokenKind tokenID)
    {
        this.tokenID = tokenID;
    }

    public PPKeyWordKind getPPKeywordID()
    {
        int len = getLength();
        if (len < 2) return PPKeyWordKind.pp_not_keyword;
        String name = getName();

        switch (name)
        {
            default: return PPKeyWordKind.pp_not_keyword;
            case "if": return PPKeyWordKind.pp_if;
            case "elif": return PPKeyWordKind.pp_elif;
            case "else": return PPKeyWordKind.pp_else;
            case "line": return PPKeyWordKind.pp_line;
            case "endif": return PPKeyWordKind.pp_endif;
            case "error": return PPKeyWordKind.pp_error;
            case "ifdef": return PPKeyWordKind.pp_ifdef;
            case "undef": return PPKeyWordKind.pp_undef;

            case "define": return PPKeyWordKind.pp_define;
            case "ifndef": return PPKeyWordKind.pp_ifndef;
            case "pragma": return PPKeyWordKind.pp_pragma;
            case "defined": return PPKeyWordKind.pp_defined;
            case "include": return PPKeyWordKind.pp_include;
            case "__include_macros": return PPKeyWordKind.pp___include_macros;
        }
    }

    public boolean isExtensionToken()
    {
        return isExtension;
    }

    public void setIsExtensionToken(boolean val)
    {
        isExtension = val;
        if (val)
            needsHandleIdentifier = true;
        else
            recomputeNeedsHandleIdentifier();
    }

    public boolean isPoisoned()
    {
        return isPoisoned;
    }

    public void setIsPoisoned(boolean poisoned)
    {
        isPoisoned = poisoned;
    }

    /**
     * Return true if the Preprocessor::HandleIdentifier
     * must be called on a token of this identifier.  If this returns false, we
     * know that HandleIdentifier will not affect the token.
     * @return
     */
    public boolean isNeedsHandleIdentifier()
    {
        return needsHandleIdentifier;
    }

    /**
     * Return true if this is the identifier for the specified string.
     * This is intended to be used for string literals only: II->isStr("foo").
     * @param str
     * @return
     */
    public boolean isStr(String str)
    {
        return getLength() == str.length() && str.equals(getName());
    }

    /*
    * FIXME Comment this to avoid deeply copy. 2017/10/29
    @Override
    public IdentifierInfo clone()
    {
        IdentifierInfo ii = new IdentifierInfo();
        ii.tokenID = tokenID;
        ii.hasMacro = hasMacro;
        ii.isExtension = isExtension;
        ii.isPoisoned = isPoisoned;
        ii.needsHandleIdentifier = needsHandleIdentifier;
        ii.entry = entry.clone();
        return ii;
    }
    */

    public void setEntry(Pair<String, IdentifierInfo> pair)
    {
        entry = pair;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        IdentifierInfo ii = (IdentifierInfo)obj;
        return ii.getName().equals(getName());
    }

    @Override
    public int hashCode()
    {
        FoldingSetNodeID id = new FoldingSetNodeID();
        id.addInteger(tokenID.hashCode());
        id.addString(entry.first);
        return id.computeHash();
    }

    public void setBuiltID(int id)
    {
        builtID = id;
    }

    public int getBuiltID()
    {
        return builtID;
    }

    @Override
    public String toString()
    {
        return "[" + tokenID.name + ", " + getName() + "]";
    }
}
