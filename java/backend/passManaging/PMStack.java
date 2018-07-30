package backend.passManaging;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import tools.Util;
import backend.pass.Pass;

import java.util.Iterator;
import java.util.Stack;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public final class PMStack implements Iterable<PMDataManager>
{
    private Stack<PMDataManager> pms;

    public PMStack()
    {
        pms = new Stack<>();
    }

    public boolean isEmpty()
    {
        return pms.isEmpty();
    }

    public PMDataManager peek()
    {
        if(isEmpty()) return null;
        return pms.peek();
    }

    public PMDataManager pop()
    {
        if (isEmpty()) return null;
        PMDataManager pm = pms.pop();
        pm.initializeAnalysisInfo();
        return pm;
    }

    public void push(PMDataManager pm)
    {
        if (!pms.isEmpty())
        {
            PMTopLevelManager tpm = pms.peek().getTopLevelManager();
            Util.assertion(tpm != null, "unable to find top level manager");
            pm.setTopLevelManager(tpm);
        }

        pms.push(pm);
    }

    @Override
    public Iterator<PMDataManager> iterator()
    {
        return pms.iterator();
    }

    public void dump()
    {
        pms.forEach(pm->System.err.printf("%s ", ((Pass)pm).getPassName()));
        if (!isEmpty())
            System.err.println();
    }
}
