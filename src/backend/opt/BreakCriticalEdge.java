package backend.opt;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2016, Xlous
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

import backend.analysis.DomTreeInfo;
import backend.analysis.LoopInfo;
import backend.pass.AnalysisUsage;
import backend.pass.FunctionPass;
import backend.value.Function;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class BreakCriticalEdge extends FunctionPass
{
    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.addPreserved(DomTreeInfo.class);
        au.addPreserved(LoopInfo.class);
        au.addPreserved(DominatorFrontier.class);
    }
    @Override
    public String getPassName()
    {
        return null;
    }

    @Override
    public boolean runOnFunction(Function f)
    {
        return false;
    }
}
