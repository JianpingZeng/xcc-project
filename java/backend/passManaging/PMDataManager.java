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
import backend.value.Function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class PMDataManager
{
    public enum PassDebuggingString
    {
        EXECUTION_MSG, // "Executing Pass '"
        MODIFICATION_MSG, // "' Made Modification '"
        FREEING_MSG, // " Freeing Pass '"
        ON_BASICBLOCK_MSG, // "'  on BasicBlock '" + PassName + "'...\n"
        ON_FUNCTION_MSG, // "' on Function '" + FunctionName + "'...\n"
        ON_MODULE_MSG, // "' on Module '" + ModuleName + "'...\n"
        ON_LOOP_MSG, // " 'on Loop ...\n'"
        ON_CG_MSG // "' on Call Graph ...\n'"
    }

    protected IPMTopLevelManager topLevelManager;
    /**
     * Collections of pass that are managed by this manager.
     */
    protected ArrayList<Pass> passVector;

    protected HashMap<PassInfo, Pass>[] inheritedAnalysis;
    protected HashMap<PassInfo, Pass> availableAnalysis;
    private int depth;
    private ArrayList<Pass> higherLevelAnalysis;

    public PMDataManager(int depth)
    {
        this.depth = depth;
        passVector = new ArrayList<>();
        availableAnalysis = new HashMap<>();
        higherLevelAnalysis = new ArrayList<>();
        inheritedAnalysis = new HashMap[PassManagerType.values().length];
        initializeAnalysisInfo();
    }

    public void verifyPreservedAnalysis(Pass p)
    {
        // TODO: 2017/11/10
    }

    public void verifyDomInfo(Pass p, Function f)
    {
        // TODO: 2017/11/10
    }

    public void removeNotPreservedAnalysis(Pass p)
    {
        // TODO: 2017/11/10
    }

    public void removeDataPasses(Pass p, String msg, PassDebuggingString str)
    {
        // TODO: 2017/11/10
    }

    public void add(Pass p)
    {
        add(p, true);
    }

    /**
     * Add pass P into the PassVector. Update
     * AvailableAnalysis appropriately if ProcessAnalysis is true.
     * @param p
     * @param processAnalysis
     */
    public void add(Pass p, boolean processAnalysis)
    {

    }

    public void setTopLevelManager(IPMTopLevelManager tlm)
    {
        topLevelManager = tlm;
    }

    public IPMTopLevelManager getTopLevelManager()
    {
        return topLevelManager;
    }

    public void initializeAnalysisInfo()
    {
        availableAnalysis.clear();
    }

    /**
     * Return true if P preserves high level analysis used by other
     * passes that are managed by this manager.
     * @param p
     * @return
     */
    public boolean preservedHigherLevelAnalysis(Pass p)
    {
        AnalysisUsage au = topLevelManager.findAnalysisUsage(p);
        if (au.getPreservedAll())
            return true;

        HashSet<PassInfo> preservedSet = au.getPreserved();
        for (Pass itr : higherLevelAnalysis)
        {
            if (!(itr instanceof ImmutablePass) &&
                    !preservedSet.contains(p.getPassInfo()))
            {
                return false;
            }
        }
        return true;
    }

    public void collectRequiredAnalysis(ArrayList<Pass> requiredPasses,
            ArrayList<PassInfo> reqPassButNotAvail, Pass p)
    {}

    public void initializeAnalysisImpl(Pass p)
    {
        availableAnalysis.clear();
        for (int i = 0; i < PassManagerType.values().length; i++)
            inheritedAnalysis[i] = null;
    }


    public Pass findAnalysisPass() {}

    public int getDepth()
    {
        return depth;
    }

    public void dumpLastUses(Pass p, int offset)
    {}

    public void dumpPassArguments()
    {}

    public void dumpPassInfo(Pass p, PassDebuggingString s1,
            PassDebuggingString s2, String msg)
    {}

    public void dumpPreservedSet(Pass p)
    {}

    public void dumpRequiredSet(Pass p)
    {}

    public int getNumContainedPasses()
    {
        return passVector.size();
    }

    public PassManagerType getPassManagerType()
    {
        assert false:"Invalid use of getPassManagerType()";
        return PassManagerType.PMT_Unknow;
    }

    public HashMap<PassInfo, Pass> getAvailableAnalysis()
    {
        return availableAnalysis;
    }

    public void populateInheritedAnalysis(Stack<PMDataManager> pms)
    {
        int index = 0;
        for (PMDataManager pm : pms)
        {
            inheritedAnalysis[index++] = pm.getAvailableAnalysis();
        }
    }

    public void recordAvailableAnalysis(Pass p)
    {
        PassInfo info = p.getPassInfo();
        if (info == null)
            return;

        availableAnalysis.put(info, p);

    }

    public abstract Pass getAsPass();
}
