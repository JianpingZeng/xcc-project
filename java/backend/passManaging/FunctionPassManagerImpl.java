package backend.passManaging;
/*
 * Extremely C language Compiler
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

import backend.pass.*;
import backend.support.BackendCmdOptions;
import backend.value.Function;
import backend.value.Module;

import java.util.*;

import static backend.passManaging.PMDataManager.PassDebugLevel.Arguments;
import static backend.passManaging.PMDataManager.PassDebugLevel.Structures;
import static backend.passManaging.PassManagerType.PMT_ModulePassManager;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class FunctionPassManagerImpl extends PMDataManager implements
        Pass,
        PMTopLevelManager
{
    private boolean wasRun;
    private TopLevelPassManagerType tpmt;
    private HashMap<Pass, Pass> lastUsers;
    private HashMap<Pass, HashSet<Pass>> inversedLastUser;
    private ArrayList<ImmutablePass> immutablePasses;
    private HashMap<Pass, AnalysisUsage> anUsageMap;
    private PMStack activeStack;
    protected ArrayList<PMDataManager> passManagers;

    private AnalysisResolver resolver;

    @Override
    public void setAnalysisResolver(AnalysisResolver resolver)
    {
        this.resolver = resolver;
    }

    @Override
    public AnalysisResolver getAnalysisResolver()
    {
        return resolver;
    }
    public FunctionPassManagerImpl(int depth)
    {
        super(depth);
        initTopLevelManager();
        wasRun = false;
    }

    @Override
    public String getPassName()
    {
        return "FunctionPassManagerImpl pass";
    }

    @Override
    public Pass getAsPass()
    {
        return this;
    }

    @Override
    public PMDataManager getAsPMDataManager()
    {
        return this;
    }

    @Override
    public void add(Pass p)
    {
        schedulePass(p);
    }

    /**
     * Execute all of the passes scheduled for execution.  Keep track of
     * whether any of the passes modifies the module, and if so, return true.
     * @param f
     * @return
     */
    public boolean run(Function f)
    {
        dumpArguments();
        dumpPasses();
        initializeAllAnalysisInfo();

        boolean changed = false;
        for (int i = 0, e = getNumContainedManagers(); i !=e; i++)
        {
            changed |= getContainedManager(i).runOnFunction(f);
        }
        return changed;
    }

    public FPPassManager getContainedManager(int index)
    {
        assert index >=0 && index < passManagers.size();
        return (FPPassManager) passManagers.get(index);
    }

    public boolean doInitialization(Module m)
    {
        boolean changed = false;
        for (int i = 0, e = getNumContainedManagers(); i != e; i++)
            changed |= getContainedManager(i).doInitialization(m);

        return changed;
    }

    public int getNumContainedManagers()
    {
        return passManagers.size();
    }

    public boolean doFinalization(Module m)
    {
        boolean changed = false;
        for (int i = 0, e = getNumContainedManagers(); i != e; i++)
            changed |= getContainedManager(i).doFinalization(m);

        return changed;
    }

    private void initTopLevelManager()
    {
        lastUsers = new HashMap<>();
        inversedLastUser = new HashMap<>();
        immutablePasses = new ArrayList<>();
        anUsageMap = new HashMap<>();
        activeStack = new PMStack();
        passManagers = new ArrayList<>();
        FPPassManager fp = new FPPassManager(1);
        fp.setTopLevelManager(this);
        activeStack.push(fp);
        addPassManager(fp);
        tpmt = TopLevelPassManagerType.TLM_Function;
    }

    @Override
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
        // Now all required passes are available.
        addTopLevelPass(p);
    }

    @Override
    public void addTopLevelPass(Pass p)
    {
        ImmutablePass ip = p.getAsImmutablePass();
        if (ip != null)
        {
            // p is a immutable pass and it will be managed by this
            // top level manager. Set up analysis resolver to connect them.
            AnalysisResolver ar = new AnalysisResolver(this);
            p.setAnalysisResolver(ar);
            initializeAnalysisImpl(p);
            addImmutablePass(ip);
            recordAvailableAnalysis(ip);
        }
        else
        {
            p.assignPassManager(getActiveStack(), PMT_ModulePassManager);
        }
    }

    @Override
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

    @Override
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
        if (BackendCmdOptions.PassDebugging.value.compareTo(Structures) < 0)
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
        if (BackendCmdOptions.PassDebugging.value.compareTo(Arguments) < 0)
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

    public PMStack getActiveStack()
    {
        return activeStack;
    }
}
