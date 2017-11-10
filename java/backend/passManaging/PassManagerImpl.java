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
import backend.value.Module;

import java.util.ArrayList;

import static backend.passManaging.PMTopLevelManager.TopLevelPassManagerType.TLM_Pass;
import static backend.passManaging.PassManagerType.PMT_ModulePassManager;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class PassManagerImpl extends PMDataManager implements
        IPMTopLevelManager,
        Pass
{
    private PMTopLevelManager tlm;

    public PassManagerImpl(int depth)
    {
        super(depth);
        tlm = new PMTopLevelManager(TLM_Pass);
    }

    /**
     * Execute all of the passes scheduled for execution.  Keep track of
     * whether any of the passes modifies the module, and if so, return true.
     * @param m
     * @return
     */
    public boolean run(Module m)
    {
        dumpArguments();
        dumpPasses();
        initializeAllAnalysisInfo();
        boolean changed = false;
        for (int i = 0, e = getNumContainedPasses(); i != e; i++)
            changed |= getContainedManager(i).runOnModule(m);
        return changed;
    }

    /**
     * Add a pass to the queue of passes scheduled to be run.
     * @param p
     */
    @Override
    public void add(Pass p)
    {
        schedulePass(p);
    }

    @Override
    public String getPassName()
    {
        return "Pass Manager Impl";
    }

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.setPreservedAll();
    }

    @Override
    public PMDataManager getAsPMDataManager()
    {
        return this;
    }

    @Override
    public Pass getAsPass()
    {
        return this;
    }

    public MPPassManager getContainedManager(int index)
    {
        assert index >= 0 && index < getNumContainedPasses();
        return (MPPassManager) passVector.get(index);
    }

    //========================================================================//
    // Following methods inherited from base class IPMTopLevelManager.========//
    //========================================================================//
    @Override
    public void schedulePass(Pass p)
    {
        tlm.schedulePass(p);
    }

    @Override
    public void addTopLevelPass(Pass p)
    {
        ImmutablePass ip = p.getAsImmutablePass();
        if (ip != null)
        {
            initializeAnalysisImpl(p);
            addImmutablePass(ip);
            recordAvailableAnalysis(ip);
        }
        else
        {
            p.assignPassManager(tlm.getActiveStack(), PMT_ModulePassManager);
        }
    }

    @Override
    public void setLastUser(ArrayList<Pass> analysisPasses, Pass p)
    {
        tlm.setLastUser(analysisPasses, p);
    }

    @Override
    public void collectLastUses(ArrayList<Pass> lastUsers, Pass p)
    {
        tlm.collectLastUses(lastUsers, p);
    }

    @Override
    public AnalysisUsage findAnalysisUsage(Pass p)
    {
        return tlm.findAnalysisUsage(p);
    }

    @Override
    public void addImmutablePass(ImmutablePass p)
    {
        tlm.addImmutablePass(p);
    }

    @Override
    public ArrayList<ImmutablePass> getImmutablePasses()
    {
        return tlm.getImmutablePasses();
    }

    @Override
    public void addPassManager(PMDataManager pm)
    {
        tlm.addPassManager(pm);
    }

    @Override
    public void dumpPasses()
    {
        tlm.dumpPasses();
    }

    @Override
    public void dumpArguments()
    {
        tlm.dumpArguments();
    }

    @Override
    public void initializeAllAnalysisInfo()
    {
        tlm.initializeAllAnalysisInfo();
    }
    //========================================================================//
    // End of methods inherited from base class IPMTopLevelManager.===========//
    //========================================================================//
}
