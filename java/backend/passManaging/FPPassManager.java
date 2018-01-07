package backend.passManaging;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
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
import backend.value.Function;
import backend.value.Module;

import static backend.passManaging.PMDataManager.PassDebuggingString.EXECUTION_MSG;
import static backend.passManaging.PMDataManager.PassDebuggingString.MODIFICATION_MSG;
import static backend.passManaging.PMDataManager.PassDebuggingString.ON_FUNCTION_MSG;

/**
 * FPPassManager itself is a ModulePass, which manages BBPassManagers and FunctionPasses.
 * It batches all function passes and basic block pass managers together and
 * sequence them to process one function at a time before processing next
 * function.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class FPPassManager extends PMDataManager implements ModulePass
{
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

    public FPPassManager(int depth)
    {
        super(depth);
    }

    public boolean runOnFunction(Function f)
    {
        if (f.isDeclaration())
            return false;

        boolean changed = false;

        // Collects inherited analysis from module level pass manager.
        populateInheritedAnalysis(topLevelManager.getActiveStack());

        for (int index = 0; index < getNumContainedPasses(); ++index)
        {
            FunctionPass fp = getContainedPass(index);
            dumpPassInfo(fp, EXECUTION_MSG, ON_FUNCTION_MSG, f.getName());
            dumpRequiredSet(fp);

            initializeAnalysisImpl(fp);
            changed |= fp.runOnFunction(f);

            if (changed)
            {
                dumpPassInfo(fp, MODIFICATION_MSG, ON_FUNCTION_MSG, f.getName());;
            }
            dumpPreservedSet(fp);

            verifyPreservedAnalysis(fp);
            removeNotPreservedAnalysis(fp);
            recordAvailableAnalysis(fp);
            removeDeadPasses(fp, f.getName(), ON_FUNCTION_MSG);

            // if dominator information is available then verify it.
            verifyDomInfo(fp, f);
        }
        return changed;
    }

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.setPreservedAll();
    }

    /**
     * Dump passes structure managed by this FPPassManager.
     * @param offset
     */
    public void dumpPassStructure(int offset)
    {

    }

    @Override
    public boolean runOnModule(Module m)
    {
        boolean changed = doInitialization(m);
        for (Function f : m.getFunctionList())
            changed |= runOnFunction(f);

        return changed | doFinalization(m);
    }

    @Override
    public String getPassName()
    {
        return "Function Pass Manager";
    }

    public boolean doInitialization(Module m)
    {
        boolean changed = false;
        for (int i = 0; i < getNumContainedPasses(); ++i)
            changed |= getContainedPass(i).doInitialization(m);
        return changed;
    }

    public boolean doFinalization(Module m)
    {
        boolean changed = false;
        for (int i = 0; i < getNumContainedPasses(); ++i)
            changed |= getContainedPass(i).doFinalization(m);
        return changed;
    }

    public FunctionPass getContainedPass(int index)
    {
        assert index >=0 && index < getNumContainedPasses();
        return (FunctionPass) passVector.get(index);
    }

    @Override
    public PassManagerType getPassManagerType()
    {
        return PassManagerType.PMT_FunctionPassManager;
    }

    @Override
    public Pass getAsPass()
    {
        return this;
    }
}
