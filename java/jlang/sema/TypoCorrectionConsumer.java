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

package jlang.sema;

import jlang.clex.IdentifierInfo;
import jlang.sema.Decl.NamedDecl;
import tools.Util;

import java.util.ArrayList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class TypoCorrectionConsumer
{
    private String typo;
    private ArrayList<NamedDecl> bestResults;
    private int bestEditDistance;

    public TypoCorrectionConsumer(IdentifierInfo typo)
    {
        this.typo = typo.getName();
        bestResults = new ArrayList<>();
        bestEditDistance = 0;
    }

    public void foundDecl(NamedDecl nd)
    {
        IdentifierInfo name = nd.getIdentifier();
        if (name == null)
            return;

        int ed = Util.getEditDistance(typo, name.getName());
        if (bestResults.isEmpty())
        {
            bestResults.add(nd);
            bestEditDistance = ed;
        }
        else
        {
            if (ed < bestEditDistance)
            {
                bestEditDistance = ed;
                bestResults.add(nd);
            }
        }
    }

    public ArrayList<NamedDecl> getBestResults()
    {
        return bestResults;
    }

    public int getBestEditDistance()
    {
        return bestEditDistance;
    }

    public boolean isEmpty()
    {
        return bestResults.isEmpty();
    }
}
