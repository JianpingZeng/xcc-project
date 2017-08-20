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

import jlang.support.SourceLocation;

import java.util.ArrayList;

/**
 * Each identifier that is #define'd has an instance of this class
 * associated with it, used to implement macro expansion.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class MacroInfo
{
    /**
     * This is the place the macro is defined.
     */
    private SourceLocation location;
    /**
     * The location of the last token in the macro.
     */
    private SourceLocation endLocation;

    /**
     * The list of arguments for a function-like macro.  This can be
     * empty, for, e.g. "#define X()".  In a C99-style variadic macro, this
     * includes the __VA_ARGS__ identifier on the list.
     *
     * This is formal argument like the function declaration.
     */
    private IdentifierInfo[] argumentList;

    /**
     * This is the list of tokens that the macro is defined
     * to.
     */
    private ArrayList<Token> replacementTokens;

    /**
     * True if this macro is a function-like macro, false if it
     /// is an object-like macro.
     */
    private boolean isFunctionLike;

    /**
     * True if this macro is of the form "#define X(...)" or
     /// "#define X(Y,Z,...)".  The __VA_ARGS__ token should be replaced with the
     /// contents of "..." in an invocation.
     */
    private boolean isC99Varargs;

    /**
     * True if this macro is of the form "#define X(a...)".  The
     /// "a" identifier in the replacement list will be replaced with all arguments
     /// of the macro starting with the specified one.
     */
    private boolean isGNUVarargs;

    /**
     * True if this is a builtin macro, such as __LINE__, and if
     /// it has not yet been redefined or undefined.
     */
    private boolean isBuiltinMacro;

    /**
     * True if we have started an expansion of this macro already.
     /// This disbles recursive expansion, which would be quite bad for things like
     /// #define A A.
     */
    private boolean isDisabled;
    /**
     * True if this macro is either defined in the main file and has
     /// been used, or if it is not defined in the main file.  This is used to
     /// emit -Wunused-macros diagnostics.
     */
    private boolean isUsed;

    public MacroInfo(SourceLocation defloc)
    {
        isUsed = true;
        replacementTokens = new ArrayList<>();
    }

    public SourceLocation getDefinitionLoc()
    {
        return location;
    }

    public void setDefinitionLoc(SourceLocation loc)
    {
        this.location = loc;
    }

    public SourceLocation getDefinitionEndLoc()
    {
        return endLocation;
    }

    public void setDefinitionEndLoc(SourceLocation loc)
    {
        this.endLocation = loc;
    }

    /**
     * Return true if the specified macro definition is equal to
     /// this macro in spelling, arguments, and whitespace.  This is used to emit
     /// duplicate definition warnings.  This implements the rules in C99 6.10.3.
     * @param other
     * @param pp
     * @return
     */
    public boolean isIdenticalTo(MacroInfo other, Preprocessor pp)
    {
        if (replacementTokens.size() != other.replacementTokens.size()
                || argumentList.length != other.argumentList.length
                || isFunctionLike != other.isFunctionLike
                || isC99Varargs() != other.isC99Varargs()
                || isGNUVarargs() != other.isGNUVarargs())
            return false;

        // Check arguemnts.
        for (int i = 0, e = argumentList.length; i < e; ++i)
        {
            if (!argumentList[i].equals(other.argumentList[i]))
                return false;
        }

        // Check all tokens.
        for (int i = 0, e = replacementTokens.size(); i < e; i++)
        {
            Token a = replacementTokens.get(i);
            Token b = other.replacementTokens.get(i);

            if(a.getKind() != b.getKind())
                return false;

            // If this isn't the first first token, check that the whitespace and
            // start-of-line characteristics match.
            if (i != 0 && (a.isAtStartOfLine() != b.isAtStartOfLine()
            || a.hasLeadingSpace() != b.hasLeadingSpace()))
                return false;

            if (a.getIdentifierInfo() != null
                    || b.getIdentifierInfo() != null)
            {
                if (!a.getIdentifierInfo().equals(b.getIdentifierInfo()))
                    return false;
                continue;
            }

            // Otherwise, check the spelling.
            if (pp.getSpelling(a).equals(pp.getSpelling(b)))
                return false;
        }
        return true;
    }

    public void setIsBuiltinMacro()
    {
        setBuiltinMacro(true);
    }

    public void setBuiltinMacro(boolean builtinMacro)
    {
        isBuiltinMacro = builtinMacro;
    }

    public void setIsUsed(boolean used)
    {
        isUsed = used;
    }

    public void setArgumentList(IdentifierInfo[] argumentList)
    {
        assert argumentList != null && this.argumentList == null;
        if (argumentList.length == 0) return;

        this.argumentList = new IdentifierInfo[argumentList.length];
        for (int i = 0, e = argumentList.length; i < e; i++)
            this.argumentList[i] = argumentList[i];
    }

    public IdentifierInfo[] getArgumentList()
    {
        return argumentList;
    }

    /**
     * Return the argument number of the specified identifier,
     /// or -1 if the identifier is not a formal argument identifier.
     * @param arg
     * @return
     */
    public int getArgumentNum(IdentifierInfo arg)
    {
        for (int i = 0, e = argumentList.length; i < e; ++i)
            if (arg.equals(argumentList[i]))
                return i;
        return -1;
    }

    public void setIsFunctionLike(boolean functionLike)
    {
        isFunctionLike = functionLike;
    }

    public boolean isFunctionLike()
    {
        return isFunctionLike;
    }

    public boolean isObjectLike()
    {
        return !isFunctionLike;
    }

    public void setIsC99Varargs()
    {
        isC99Varargs = true;
    }

    public void setIsGNUVarargs()
    {
        isGNUVarargs = true;
    }

    public void setGNUVarargs()
    {
        isGNUVarargs = true;
    }

    public boolean isC99Varargs()
    {
        return isC99Varargs;
    }

    public boolean isGNUVarargs()
    {
        return isGNUVarargs;
    }

    public boolean isVariadic()
    {
        return isC99Varargs || isGNUVarargs;
    }

    public boolean isBuiltinMacro()
    {
        return isBuiltinMacro;
    }

    public boolean isUsed()
    {
        return isUsed;
    }

    public int getNumTokens()
    {
        return replacementTokens.size();
    }

    public Token getReplacementToken(int idx)
    {
        assert idx >= 0 && idx < replacementTokens.size() :"index out of range!";
        return replacementTokens.get(idx);
    }

    public ArrayList<Token> getReplacementTokens()
    {
        return replacementTokens;
    }

    public void addTokenBody(Token tok)
    {
        replacementTokens.add(tok);
    }

    public boolean isEnabled()
    {
        return !isDisabled;
    }

    public void enableMacro()
    {
        assert isDisabled: "Cannot enable an already-enabled macro!";
        isDisabled = false;
    }

    public void disableMacro()
    {
        assert !isDisabled: "Cannot disable an already-disabled macro!";
        isDisabled = true;
    }

    public int getNumArgs()
    {
        return argumentList.length;
    }

    public IdentifierInfo getArgAt(int idx)
    {
        assert idx >= 0 && idx < argumentList.length;
        return argumentList[idx];
    }
}
