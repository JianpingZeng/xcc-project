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

import java.util.ArrayList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class PragmaNameSpace extends PragmaHandler
{
    private ArrayList<PragmaHandler> handlers;

    public PragmaNameSpace(IdentifierInfo name)
    {
        super(name);
        handlers = new ArrayList<>();
    }

    /**
     * Check to see if there is already a handler for the
     * specified asmName.  If not, return the handler for the null identifier if it
     * exists, otherwise return null.  If IgnoreNull is true (the default) then
     * the null handler isn't returned on failure to match.
     * @param name
     * @param ignoreNull
     * @return
     */
    public PragmaHandler findHandler(IdentifierInfo name, boolean ignoreNull)
    {
        PragmaHandler nullHandler = null;
        for (int i = 0, e = handlers.size(); i < e; i++)
        {
            if (handlers.get(i).getName() != null &&
                    handlers.get(i).getName().equals(name))
                return handlers.get(i);

            if (handlers.get(i).getName() == null)
                nullHandler = handlers.get(i);
        }
        return ignoreNull? null: nullHandler;
    }

    @Override
    public void handlePragma(Preprocessor pp, Token firstToken)
    {
        // TODO: 17-4-25
    }

    public void addPragma(PragmaHandler handler)
    {
        handlers.add(handler);
    }

    public void removePragmaHandler(PragmaHandler handler)
    {
        for (int i = 0, e = handlers.size(); i < e; i++)
        {
            if (handlers.get(i).equals(handler))
            {
                handlers.set(i, handlers.get(e - 1));
                handlers.remove(e - 1);
                return;
            }
        }
        assert false:"Handler not registered in this namespace";
    }

    @Override
    public PragmaNameSpace getIfNamespace()
    {
        return this;
    }

    public boolean isEmpty()
    {
        return handlers.isEmpty();
    }
}
