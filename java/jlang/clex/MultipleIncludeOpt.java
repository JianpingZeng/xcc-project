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

/**
 * This class implements the simple state machine that the
 * Lexer class uses to detect files subject to the 'multiple-include'
 * optimization.  The public methods in this class are triggered by various
 * events that occur when a file is lexed, and after the entire file is lexed,
 * information about which macro (if any) controls the header is returned.
 * @author Xlous.zeng
 * @version 0.1
 */
public class MultipleIncludeOpt
{
    /**
     * This is set to false when a file is first opened and true
     * any time a token is returned to the client or a (non-multiple-include)
     * directive is parsed.  When the final #endif is parsed this is reset back
     * to false, that way any tokens before the first #ifdef or after the last
     * #endif can be easily detected.
     */
    private boolean readAnyTokens;

    /**
     * This is set to false when a file is first opened and true
     * any time a token is returned to the client or a (non-multiple-include)
     * directive is parsed.  When the final #endif is parsed this is reset back
     * to false, that way any tokens before the first #ifdef or after the last
     * #endif can be easily detected.
     */
    private boolean didMacroExpansion;

    /**
     * The controlling macro for a file, if valid.
     */
    private IdentifierInfo theMacro;

    /**
     * Permenantly mark this file as not being suitable for the
     * include-file optimization.
     */
    public void invalidate()
    {
        readAnyTokens = true;
        theMacro = null;
    }

    /**
     * This is used for the #ifndef hande-shake at the
     * top of the file when reading preprocessor directives.  Otherwise, reading
     * the "ifndef x" would count as reading tokens.
     * @return
     */
    public boolean hasReadAnyTokens()
    {
        return readAnyTokens;
    }

    /**
     * If a token is read, remember that we have seen a side-effect in this file.
     */
    public void readToken()
    {
        readAnyTokens = true;
    }

    /**
     * When a macro is expanded with this lexer as the current
     * buffer, this method is called to disable the MIOpt if needed.
     */
    public void expandedMacro()
    {
        didMacroExpansion = true;
    }

    /**
     * When entering a top-level #ifndef directive (or the
     * "#if !defined" equivalent) without any preceding tokens, this method is
     * called.
     *
     * Note, we don't care about the input value of 'ReadAnyTokens'.  The caller
     * ensures that this is only called if there are no tokens read before the
     * #ifndef.  The caller is required to do this, because reading the #if line
     * obviously reads in in tokens.
     * @param macro
     */
    public void enterTopLevelIFNDEF(IdentifierInfo macro)
    {
        if (theMacro != null)
        {
            invalidate();
            return;
        }

        if (didMacroExpansion)
        {
            invalidate();
            return;
        }

        readAnyTokens = true;
        theMacro = macro;
    }

    /**
     * This is invoked when a top level conditional
     * (except #ifndef) is found.
     */
    public void enterTopLevelConditional()
    {
        invalidate();
    }

    /**
     * This method is called when the lexer exits the
     * top-level conditional.
     */
    public void exitTopLevelConditional()
    {
        if (theMacro == null)
        {
            invalidate();
            return;
        }

        readAnyTokens = false;
    }

    /**
     * Once the entire file has been lexed, if there is a controlling macro,
     * return it.
     * @return
     */
    public IdentifierInfo getControllingMacroAtEndOfFile()
    {
        if (!readAnyTokens)
            return theMacro;
        return null;
    }
}
