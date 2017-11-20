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

import backend.pass.*;
import backend.support.BackendCmdOptions;
import backend.value.Function;
import tools.Util;

import java.util.*;

import static backend.passManaging.PMDataManager.PassDebugLevel.*;
import static backend.passManaging.PMDataManager.PassDebuggingString.FREEING_MSG;

/**
 * PMDataManager provides the common place to manage the analysis data used by
 * pass managers, such sa {@linkplain MPPassManager}, {@linkplain FPPassManager},
 * and {@linkplain BBPassManager}.
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class PMDataManager
{
    public enum PassDebugLevel
    {
        None,
        Arguments,
        Structures,
        Executions,
        Details
    }

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

    protected PMTopLevelManager topLevelManager;
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
        if (Util.DEBUG)
        {
            AnalysisUsage au = topLevelManager.findAnalysisUsage(p);
            HashSet<PassInfo> preservedSet = au.getPreserved();
            // verify preserved analysis.
            for (PassInfo pi : preservedSet)
            {
                Pass ap = findAnalysisPass(pi, true);
                if (ap != null)
                {
                    ap.verifyAnalysis();
                }
            }
        }
    }

    public void verifyDomInfo(Pass p, Function f)
    {
        // TODO: 2017/11/10
    }

    public void removeNotPreservedAnalysis(Pass p)
    {
        AnalysisUsage au = topLevelManager.findAnalysisUsage(p);
        if (au.getPreservedAll())
            return;

        HashSet<PassInfo> preservedSet = au.getPreserved();
        Iterator<Map.Entry<PassInfo, Pass>> itr = availableAnalysis.entrySet().iterator();
        while (itr.hasNext())
        {
            Map.Entry<PassInfo, Pass> entry = itr.next();
            if (!(entry.getValue() instanceof ImmutablePass) &&
                    !preservedSet.contains(entry.getKey()))
            {
                // remove this analysis.
                if (BackendCmdOptions.PassDebugging.value.compareTo(Details) >= 0)
                {
                    Pass s = entry.getValue();
                    System.err.printf(" -- '%s' is not preserving '%s'\n",
                            p.getPassName(),
                            s.getPassName());
                }
                itr.remove();
                //availableAnalysis.remove(entry.getKey());
            }
        }

        for (int index = 0; index < PassManagerType.values().length; index++)
        {
            if (inheritedAnalysis[index] == null)
                continue;
            for (Map.Entry<PassInfo, Pass> entry : inheritedAnalysis[index].entrySet())
            {
                if (!(entry.getValue() instanceof ImmutablePass) &&
                        !preservedSet.contains(entry.getKey()))
                {
                    // remove this analysis.
                    inheritedAnalysis[index].remove(entry.getKey());
                }
            }
        }
    }

    public void removeDeadPasses(Pass p,
            String msg,
            PassDebuggingString dgb_str)
    {
        ArrayList<Pass> deadPasses = new ArrayList<>();
        if (topLevelManager== null)
            return;
        topLevelManager.collectLastUses(deadPasses, p);
        if (BackendCmdOptions.PassDebugging.value.compareTo(Details) >= 0 &&
                !deadPasses.isEmpty())
        {
            System.err.printf(" -*- '%s'", p.getPassName());
            System.err.printf(" is the last user of following pass instance.");
            System.err.printf(" Free these instance\n");
        }

        for (Pass dp : deadPasses)
        {
            dumpPassInfo(dp, FREEING_MSG, dgb_str, msg);

            PassInfo pi = dp.getPassInfo();
            if (pi != null)
            {
                if (availableAnalysis.containsKey(pi))
                    availableAnalysis.remove(pi);
            }
        }
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
        // p is a immutable pass and it will be managed by this
        // top level manager. Set up analysis resolver to connect them.
        AnalysisResolver ar = new AnalysisResolver(this);
        p.setAnalysisResolver(ar);

        if (!processAnalysis)
        {
            passVector.add(p);
            return;
        }

        ArrayList<Pass> requiredPasses = new ArrayList<>();
        ArrayList<PassInfo> reqAnalysisNotAvailable = new ArrayList<>();

        collectRequiredAnalysis(requiredPasses, reqAnalysisNotAvailable, p);;

        // Now, take care of required analysises that are not available.
        for (PassInfo pi : reqAnalysisNotAvailable)
        {
            Pass analysisPass = pi.createPass();
            addLowerLevelRequiredPass(p, analysisPass);
        }

        removeNotPreservedAnalysis(p);
        recordAvailableAnalysis(p);
        passVector.add(p);
    }

    public void setTopLevelManager(PMTopLevelManager tlm)
    {
        topLevelManager = tlm;
    }

    public PMTopLevelManager getTopLevelManager()
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

    public void collectRequiredAnalysis(
            ArrayList<Pass> requiredPasses,
            ArrayList<PassInfo> reqPassButNotAvail,
            Pass p)
    {
        AnalysisUsage au = topLevelManager.findAnalysisUsage(p);
        HashSet<PassInfo> requiredSet = au.getRequired();
        for (PassInfo pi : requiredSet)
        {
            Pass analysisPass = findAnalysisPass(pi, true);
            if (analysisPass != null)
                requiredPasses.add(analysisPass);
            else
                reqPassButNotAvail.add(pi);
        }
    }

    public void initializeAnalysisImpl(Pass p)
    {
        availableAnalysis.clear();
        for (int i = 0; i < PassManagerType.values().length; i++)
            inheritedAnalysis[i] = null;
    }

    /**
     * Find the pass that implements PassInfo. If desired pass is not
     * found then return null.
     * @param pi
     * @param searchParent
     * @return
     */
    public Pass findAnalysisPass(PassInfo pi, boolean searchParent)
    {
        if (availableAnalysis.containsKey(pi))
            return availableAnalysis.get(pi);

        // search parents through TopLevelManager.
        if (searchParent)
            return topLevelManager.findAnalysisPass(pi);
        return null;
    }

    public int getDepth()
    {
        return depth;
    }

    /**
     * Print list of passes that are last used by p.
     * @param p
     * @param offset
     */
    public void dumpLastUses(Pass p, int offset)
    {
        ArrayList<Pass> luses = new ArrayList<>();
        if (topLevelManager == null)
            return;

        topLevelManager.collectLastUses(luses, p);
        luses.forEach(lu->
        {
            System.err.printf("--%s", Util.fixedLengthString(offset<<1, ' '));
            lu.dumpPassStructures(0);
        });
    }

    public void dumpPassArguments()
    {
        for (Pass p : passVector)
        {
            if (p instanceof PMDataManager)
            {
                PMDataManager pmd = (PMDataManager)p;
                pmd.dumpPassArguments();
            }
            else
            {
                PassInfo pi = p.getPassInfo();
                if (pi != null)
                {
                    if (!pi.isAnalysisGroup())
                        System.err.printf(" -%s", pi.getPassArgument());
                }
            }
        }
    }

    public void dumpPassInfo(Pass p, PassDebuggingString s1,
            PassDebuggingString s2, String msg)
    {
        if (BackendCmdOptions.PassDebugging.value.compareTo(Executions) < 0)
            return;
        System.err.printf("0x%x%s", hashCode(), Util.fixedLengthString(depth*2+1, ' '));
        switch (s1)
        {
            case EXECUTION_MSG:
                System.err.printf("Executing Pass '%s", p.getPassName());
                break;
            case MODIFICATION_MSG:
                System.err.printf("Made modification '%s", p.getPassName());
                break;
            case FREEING_MSG:
                System.err.printf("Freeing Pass '%s", p.getPassName());
                break;
            default:break;
        }
        switch (s2)
        {
            case ON_BASICBLOCK_MSG:
                System.err.printf(" ' on BasicBlock '%s'...\n", msg);
                break;
            case ON_FUNCTION_MSG:
                System.err.printf(" ' on Function '%s'...\n", msg);
                break;
            case ON_MODULE_MSG:
                System.err.printf(" ' on Module '%s'...\n", msg);
                break;
            case ON_CG_MSG:
                System.err.printf(" ' on CallGraph '%s'...\n", msg);
                break;
        }
    }

    public void dumpPreservedSet(Pass p)
    {
        if (BackendCmdOptions.PassDebugging.value.compareTo(Details) < 0)
            return;

        AnalysisUsage au = new AnalysisUsage();
        p.getAnalysisUsage(au);
        dumpAnalysisUsage("Preserved", p, au.getPreserved());
    }

    public void dumpAnalysisUsage(
            String msg,
            Pass p,
            HashSet<PassInfo> set)
    {
        assert BackendCmdOptions.PassDebugging.value.compareTo(Details) >= 0;
        if (set.isEmpty())
            return;

        System.err.printf("0x%x%s%s%s", p.hashCode(),
                Util.fixedLengthString(depth*2+3, ' '),
                msg, " Analyses:");
        int i = 0;
        for (PassInfo pi : set)
        {
            if (i != 0)
                System.err.print(',');
            System.err.printf(" %s", pi.getPassName());
            ++i;
        }
        System.err.println();
    }

    public void dumpRequiredSet(Pass p)
    {
        if (BackendCmdOptions.PassDebugging.value.compareTo(Details) < 0)
            return;

        AnalysisUsage au = new AnalysisUsage();
        p.getAnalysisUsage(au);
        dumpAnalysisUsage("Required", p, au.getRequired());
    }

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

    public void addLowerLevelRequiredPass(Pass p, Pass requiredPass)
    {
        if (topLevelManager != null)
        {
            topLevelManager.dumpArguments();
            topLevelManager.dumpPasses();
        }

        if (Util.DEBUG)
        {
            System.err.printf("Unable to schedule '%s'", requiredPass.getPassName());
            System.err.printf(" required by '%s'\n", p.getPassName());
        }
        Util.shouldNotReachHere("Unable to schedule pass");
    }

    public abstract Pass getAsPass();
}
