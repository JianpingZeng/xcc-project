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
import backend.value.Function;
import backend.value.Module;

import java.util.ArrayList;

import static backend.passManaging.PMTopLevelManager.TopLevelPassManagerType.TLM_Function;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class FunctionPassManagerImpl extends PMDataManager implements
        Pass,
        IPMTopLevelManager
{
    private PMTopLevelManager pmt;
    private boolean wasRun;

    public FunctionPassManagerImpl(int depth)
    {
        super(depth);
        pmt = new PMTopLevelManager(TLM_Function);
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
        for (int i = 0, e = getNumContainedPasses(); i !=e; i++)
        {
            changed |= getContainedManager(i).runOnFunction(f);
        }
        return changed;
    }

    public FPPassManager getContainedManager(int index)
    {
        assert index >=0 && index < getNumContainedPasses();
        return (FPPassManager) passVector.get(index);
    }

    public boolean doInitialization(Module m)
    {
        boolean changed = false;
        for (int i = 0, e = getNumContainedPasses(); i != e; i++)
            changed |= getContainedManager(i).doInitialization(m);

        return changed;
    }

    public boolean doFinalization(Module m)
    {
        boolean changed = false;
        for (int i = 0, e = getNumContainedPasses(); i != e; i++)
            changed |= getContainedManager(i).doFinalization(m);

        return changed;
    }
    //========================================================================//
    // Following methods inherited from base class IPMTopLevelManager.========//
    //========================================================================//
    @Override
    public void schedulePass(Pass p)
    {
        pmt.schedulePass(p);
    }

    @Override
    public void addTopLevelPass(Pass p)
    {
        pmt.addTopLevelPass(p);
    }

    @Override
    public void setLastUser(ArrayList<Pass> analysisPasses, Pass p)
    {
        pmt.setLastUser(analysisPasses, p);
    }

    @Override
    public void collectLastUses(ArrayList<Pass> lastUsers, Pass p)
    {
        pmt.collectLastUses(lastUsers, p);
    }

    @Override
    public AnalysisUsage findAnalysisUsage(Pass p)
    {
        return pmt.findAnalysisUsage(p);
    }

    @Override
    public void addImmutablePass(ImmutablePass p)
    {
        pmt.addImmutablePass(p);
    }

    @Override
    public ArrayList<ImmutablePass> getImmutablePasses()
    {
        return pmt.getImmutablePasses();
    }

    @Override
    public void addPassManager(PMDataManager pm)
    {
        pmt.addPassManager(pm);
    }

    @Override
    public void dumpPasses()
    {
        pmt.dumpPasses();
    }

    @Override
    public void dumpArguments()
    {
        pmt.dumpArguments();
    }

    @Override
    public void initializeAllAnalysisInfo()
    {
        pmt.initializeAllAnalysisInfo();
    }
    //========================================================================//
    // End of methods inherited from base class IPMTopLevelManager.===========//
    //========================================================================//
}
