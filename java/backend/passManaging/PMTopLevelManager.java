package backend.passManaging;
/*
 * Xlous C language Compiler
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

import backend.pass.AnalysisUsage;
import backend.pass.ImmutablePass;
import backend.pass.Pass;

import java.util.*;

/**
 * PMTopLevelManager manages lastUser information and collects some usefully
 * common API used by top level pass manager.
 * @author Xlous.zeng
 * @version 0.1
 */
public class PMTopLevelManager implements IPMTopLevelManager
{
    public enum TopLevelPassManagerType
    {
        TLM_Function,
        /**
         * For pass
         */
        TLM_Pass,
    }
    private HashMap<Pass, Pass> lastUsers;
    private HashMap<Pass, HashSet<Pass>> inversedLastUser;
    private ArrayList<ImmutablePass> immutablePasses;
    private HashMap<Pass, AnalysisUsage> anUsageMap;
    Stack<PMDataManager> activeStack;
    protected ArrayList<PMDataManager> passManagers;

    public PMTopLevelManager(TopLevelPassManagerType pmt)
    {
        lastUsers = new HashMap<>();
        inversedLastUser = new HashMap<>();
        immutablePasses = new ArrayList<>();
        anUsageMap = new HashMap<>();
        activeStack = new Stack<>();
        passManagers = new ArrayList<>();

        if (pmt == TopLevelPassManagerType.TLM_Pass)
        {
            MPPassManager mp = new MPPassManager();
            mp.setTopLevelManager(this);
            activeStack.push(mp);
        }
        else
            assert false:"Unknown kind of top level pass manager";
    }

    public void schedulePass(Pass p)
    {}

    public void addTopLevelPass(Pass p)
    {}

    public void setLastUser(ArrayList<Pass> analysisPasses, Pass p)
    {
    }

    public void collectLastUses(ArrayList<Pass> lastUsers, Pass p)
    {}

    public Pass findAnalysisPass()
    {
        // TODO: 2017/11/10
        return null;
    }

    public AnalysisUsage findAnalysisUsage(Pass p)
    {}

    public void addImmutablePass(ImmutablePass p)
    {
        p.initializePass();
        immutablePasses.add(p);
    }

    public ArrayList<ImmutablePass> getImmutablePasses()
    {
        return immutablePasses;
    }

    public void addPassManager(PMDataManager pm)
    {
        passManagers.add(pm);
    }

    public void dumpPasses()
    {
        // TODO: 2017/11/10
    }

    public void dumpArguments()
    {}

    public void initializeAllAnalysisInfo()
    {
        for (PMDataManager pm : passManagers)
        {
            pm.initializeAnalysisInfo();
        }

        for (Map.Entry<Pass, Pass> entry : lastUsers.entrySet())
        {
            if (inversedLastUser.containsKey(entry.getValue()))
            {
                HashSet<Pass> l = inversedLastUser.get(entry.getValue());
                l.add(entry.getKey());
            }
            else
            {
                HashSet<Pass> l = new HashSet<>();
                l.add(entry.getKey());
                inversedLastUser.put(entry.getValue(), l);
            }
        }
    }

    public Stack<PMDataManager> getActiveStack()
    {
        return activeStack;
    }
}
