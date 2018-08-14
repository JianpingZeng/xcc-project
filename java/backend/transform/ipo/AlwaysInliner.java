package backend.transform.ipo;
/*
 * Extremely C language Compiler
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
import backend.pass.Pass;
import backend.support.Attribute;
import backend.support.CallSite;
import backend.value.Function;
import backend.value.Module;

import java.util.HashSet;

/**
 * @author JianpingZeng
 * @version 0.4
 */
public class AlwaysInliner extends Inliner
{
    public AlwaysInliner()
    {
        // Uses a negative number as threshold to always enable
        // inline on call site.
        super(-2000000);
        analyzer = new InlineCostAnalyzer();
    }

    @Override
    public boolean doInitialization(CallGraph cg)
    {
        Module m = cg.getModule();
        for (Function f : m)
        {
            if (f == null) continue;
            if (!f.isDeclaration() && !f.hasFnAttr(Attribute.AlwaysInline))
                neverInlined.add(f);
        }
        return false;
    }

    @Override
    public InlineCost getInlineCost(CallSite cs)
    {
        return analyzer.getInlineCost(cs, neverInlined);
    }

    @Override
    public float getInlineFudgeFactor(CallSite cs)
    {
        return analyzer.getInlineFudgeFactor(cs);
    }

    @Override
    public String getPassName()
    {
        return "Always Function Inliner Pass";
    }
    public static Pass createAlwaysInlinerPass()
    {
        return new AlwaysInliner();
    }
}
