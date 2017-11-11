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
import backend.pass.PassInfo;

import java.util.*;

import static backend.passManaging.PMDataManager.PassDebugLevel.Arguments;
import static backend.passManaging.PMDataManager.PassDebugLevel.Structures;
import static backend.passManaging.PMDataManager.PassDebugging;

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

    /**
     * Schedule pass p for execution. Make sure that passes required by
     * p are run before p is run. Update analysis info maintained by
     * the manager. Remove dead passes. This is a recursive function.
     * @param p
     */
    public void schedulePass(Pass p)
    {
        // Given pass a chance to prepare the stage.
        p.preparePassManager(activeStack);

        PassInfo pi = p.getPassInfo();
        if (pi != null && pi.isAnalysis() && findAnalysisPass(pi) != null)
        {
            return;
        }

        AnalysisUsage au = findAnalysisUsage(p);
        boolean checkAnalysis = true;
        while (checkAnalysis)
        {
            checkAnalysis = false;
            HashSet<PassInfo> requiredSet = au.getRequired();
            for (PassInfo pInfo : requiredSet)
            {
                Pass analysisPass = findAnalysisPass(pInfo);
                if (analysisPass == null)
                {
                    analysisPass = pInfo.createPass();
                    if (p.getPotentialPassManagerType() ==
                            analysisPass.getPotentialPassManagerType())
                    {
                        // schedule analysis pass that is managed by the same pass manager.
                        schedulePass(analysisPass);
                    }
                    else if (p.getPotentialPassManagerType()
                            .compareTo(analysisPass.getPotentialPassManagerType()) > 0)
                    {
                        // schedule analysis pass that is managed by a new manager.
                        schedulePass(analysisPass);
                        // recheck analysis passes to ensure that
                        // required analysises are already checked are
                        // still available.
                        checkAnalysis = true;
                    }
                    else
                    {
                        // don't schedule this analysis.
                    }
                }
            }
        }
        addTopLevelPass(p);
    }

    /**
     * This method should be overridden by subclass.
     * @param p
     */
    public void addTopLevelPass(Pass p)
    {}

    public void setLastUser(ArrayList<Pass> analysisPasses, Pass p)
    {
        for (Pass anaPass : analysisPasses)
        {
            lastUsers.put(anaPass, p);
            if (p.equals(anaPass))
                continue;

            for (Map.Entry<Pass, Pass> entry : lastUsers.entrySet())
            {
                if (entry.getValue().equals(anaPass))
                {
                    lastUsers.put(entry.getKey(), p);
                }
            }
        }
    }

    public void collectLastUses(ArrayList<Pass> lastUsers, Pass p)
    {
        if (!inversedLastUser.containsKey(p))
            return;

        HashSet<Pass> lu = inversedLastUser.get(p);
        lastUsers.addAll(lu);
    }

    @Override
    public Pass findAnalysisPass(PassInfo pi)
    {
        Pass p = null;
        // check pass manager.
        for (PMDataManager pm : passManagers)
        {
            p = pm.findAnalysisPass(pi, false);
        }

        for (ImmutablePass ip : immutablePasses)
        {
            if (ip.getPassInfo().equals(pi))
                p = ip;
        }
        return p;
    }

    public AnalysisUsage findAnalysisUsage(Pass p)
    {
        AnalysisUsage au = null;
        if (anUsageMap.containsKey(p))
            au = anUsageMap.get(p);
        else
        {
            au = new AnalysisUsage();
            p.getAnalysisUsage(au);
            anUsageMap.put(p, au);
        }
        return au;
    }

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
        if (PassDebugging.value.compareTo(Structures) < 0)
            return;

        // print out the immutable passes.
        immutablePasses.forEach(im->{im.dumpPassStructures(0);});

        // Every class that derives from PMDataManager also derives
        // from Pass.
        passManagers.forEach(pm->
        {
            ((Pass)pm).dumpPassStructures(1);
        });
    }

    public void dumpArguments()
    {
        if (PassDebugging.value.compareTo(Arguments) < 0)
            return;

        System.err.print("Pass Arguments: ");
        passManagers.forEach(PMDataManager::dumpPassArguments);
        System.err.println();
    }

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
