package backend.transform.ipo;
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
import backend.analysis.CallGraph;
import backend.analysis.CallGraphNode;
import backend.pass.CallGraphSCCPass;
import backend.support.CallSite;
import backend.value.Function;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public abstract class Inliner extends CallGraphSCCPass
{
    /**
     * A value used for controlling if we should inline the specified call site.
     * set it's value default to 0.
     */
    protected int inlineThreshold;
    protected HashSet<Function> neverInlined;
    protected InlineCostAnalyzer analyzer;

    public Inliner()
    {
        this(0);
    }

    public Inliner(int threshold)
    {
        inlineThreshold = threshold;
        neverInlined = new HashSet<>();
    }

    public int getInlineThreshold() { return inlineThreshold; }

    @Override
    public boolean runOnSCC(ArrayList<CallGraphNode> nodes) {
        return false;
    }

    @Override
    public boolean doFinalization(CallGraph cg)
    {
        removeDeadFunctions(cg, neverInlined);
        return true;
    }

    public boolean shouldInline(CallSite cs)
    {
        return false;
    }

    public abstract int getInlineCost(CallSite cs);
    public abstract int getInlineFudgeFactor(CallSite cs);

    public boolean inlineCallIfPossible()
    {
        return false;
    }
    public void removeDeadFunctions(CallGraph cg, HashSet<Function> deadFuncs)
    {
    }
}
