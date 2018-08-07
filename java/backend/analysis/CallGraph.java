package backend.analysis;
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
import backend.pass.AnalysisResolver;
import backend.pass.ModulePass;
import backend.value.Function;
import backend.value.Module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class CallGraph implements ModulePass
{
    protected Module mod;
    private AnalysisResolver resolver;
    protected HashMap<Function, CallGraphNode> functionMap;

    public CallGraph()
    {
        functionMap = new HashMap<>();
    }

    public Module getModule()
    {
        return mod;
    }

    public HashMap<Function, CallGraphNode> getFunctionMap()
    {
        return functionMap;
    }

    public CallGraphNode getGraphNode(Function f)
    {
        return f != null ? functionMap.get(f) : null;
    }

    @Override
    public boolean runOnModule(Module m)
    {
        mod = m;
        return false;
    }

    @Override
    public String getPassName()
    {
        return "Collecting CallGraph over Module Pass";
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

    public SCCIterator getSCCIterator()
    {
        return new SCCIterator();
    }

    public final class SCCIterator implements Iterator<ArrayList<CallGraphNode>>
    {
        private SCCIterator()
        {}

        @Override
        public boolean hasNext()
        {
            return false;
        }

        @Override
        public ArrayList<CallGraphNode> next()
        {
            return null;
        }
    }
}
