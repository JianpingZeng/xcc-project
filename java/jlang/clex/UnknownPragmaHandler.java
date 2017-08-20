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

package jlang.clex;

import jlang.driver.PrintPPOutputPPCallbacks;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class UnknownPragmaHandler extends PragmaHandler
{
    private String prefix;
    private PrintPPOutputPPCallbacks callbacks;

    public UnknownPragmaHandler(String prefix,
            PrintPPOutputPPCallbacks callbacks)
    {
        super(null);
        this.prefix = prefix;
        this.callbacks = callbacks;
    }

    @Override
    public void handlePragma(Preprocessor pp, Token pragmaTok)
    {
        callbacks.moveToLine(pragmaTok.getLocation());
        callbacks.os.print(prefix);

        // Read and print all of the pragma tokens.
        while (pragmaTok.isNot(TokenKind.eom))
        {
            if (pragmaTok.hasLeadingSpace())
                callbacks.os.print(' ');

            String tokSpell = pp.getSpelling(pragmaTok);
            callbacks.os.print(tokSpell);
            pp.lexUnexpandedToken(pragmaTok);
        }
        callbacks.os.println();
    }

    @Override
    public PragmaNameSpace getIfNamespace()
    {
        return null;
    }
}
