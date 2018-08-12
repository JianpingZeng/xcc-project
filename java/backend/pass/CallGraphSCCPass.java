package backend.pass;
/*
 * Extremely C language Compiler.
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
import backend.analysis.CallGraphAnalysis;
import backend.analysis.CallGraphNode;
import backend.passManaging.PMDataManager;
import backend.passManaging.PMStack;
import backend.passManaging.PMTopLevelManager;
import backend.passManaging.PassManagerType;
import backend.support.PrintCallGraphPass;
import tools.Util;

import java.io.PrintStream;
import java.util.ArrayList;

/**
 * Defines an interface for any pass operating on Strong connected component.
 * @author Jianping Zeng.
 * @version 0.4
 */
public abstract class CallGraphSCCPass implements Pass
{
    private AnalysisResolver resolver;

    public boolean doInitialization(CallGraphAnalysis cg)
    {
        return false;
    }
    public boolean doFinalization(CallGraphAnalysis cg)
    {
        return false;
    }

    @Override
    public AnalysisResolver getAnalysisResolver()
    {
        return resolver;
    }

    @Override
    public void setAnalysisResolver(AnalysisResolver resolver)
    {
        this.resolver = resolver;
    }

    public abstract boolean runOnSCC(ArrayList<CallGraphNode> nodes);

    public void assignPassManager(PMStack pms)
    {
        assignPassManager(pms, PassManagerType.PMT_CallGraphPassManager);
    }

    /**
     * arranges a pass manager for this pass.
     * @param pms
     * @param pmt
     */
    @Override
    public void assignPassManager(PMStack pms, PassManagerType pmt)
    {
        while (!pms.isEmpty() &&
                pms.peek().getPassManagerType()
                .compareTo(PassManagerType.PMT_CallGraphPassManager) > 0)
        {
            pms.pop();
        }

        Util.assertion(!pms.isEmpty(), "Unable to handle Call Graph Pass");
        if (!(pms.peek() instanceof CGPassManager))
        {
            // create a new call graph SCC pass manager if it does not exists.
            PMDataManager pmd = pms.peek();
            CGPassManager cgm = new CGPassManager(pmd.getDepth());

            // assigns a new top level manager to it and schedule it as
            // an appropriate time to be started.
            PMTopLevelManager tpm = pmd.getTopLevelManager();
            tpm.schedulePass(cgm);

            // push this call graph pass into according manager.
            pms.push(cgm);
        }

        pms.peek().add(this);
    }

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.addRequired(CallGraphAnalysis.class);
        au.addPreserved(CallGraphAnalysis.class);
    }

    public Pass createPrinterPass(PrintStream os, String banner)
    {
        return new PrintCallGraphPass(banner, os);
    }
}
