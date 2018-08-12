package backend.analysis;

import backend.support.CallSite;
import backend.value.Function;
import tools.Pair;
import tools.Util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

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

/**
 * This class would be used for representing one node in Call Graph. Actually, it is a
 * representative {@linkplain Function} in {@linkplain backend.value.Module}.
 *
 * @author Jianping Zeng.
 * @version 0.4
 */
public class CallGraphNode implements Iterable<Pair<CallSite, CallGraphNode>>
{
    /**
     * Represents the function related to this node.
     */
    private Function function;
    /**
     * It represents all functions called by this function.
     */
    private ArrayList<Pair<CallSite, CallGraphNode>> calledFunctions;

    public CallGraphNode(Function f)
    {
        calledFunctions = new ArrayList<>();
        function = f;
    }

    public void addCalledFunction(CallSite cs, CallGraphNode targetNode)
    {
        calledFunctions.add(Pair.get(cs, targetNode));
    }
    public boolean isEmpty()
    {
        return calledFunctions.isEmpty();
    }
    public int size()
    {
        return calledFunctions.size();
    }
    public Function getFunction()
    {
        return function;
    }

    @Override
    public Iterator<Pair<CallSite, CallGraphNode>> iterator()
    {
        return calledFunctions.iterator();
    }

    /**
     * Get the idx'th call node in call graph.
     * @param idx
     * @return
     */
    public CallGraphNode getCallGraphNodeAt(int idx)
    {
        Util.assertion(idx >= 0 && idx < size());
        return calledFunctions.get(idx).second;
    }
    public void dump()
    {
        print(System.err);
    }

    public void print(PrintStream os)
    {
        Function f = getFunction();
        if (f != null)
            os.printf("Call Graph node for function: '%s'%n", f.getName());
        else
            os.printf("Call Graph node <<null function: 0x%d>>:%n", hashCode());
        Iterator<Pair<CallSite, CallGraphNode>> itr = iterator();
        while (itr.hasNext())
        {
            f = itr.next().second.getFunction();
            if (f != null)
                os.printf("  Calls function '%s'%n", f.getName());
            else
                os.println("  Calls external function");
        }
        os.println();
    }

    public void removeAllCalledFunctions()
    {
        calledFunctions.clear();
    }

    /**
     * Remove the first call in the specified call site. Note that, this function will not
     * attempts to remove all same call site.
     * @param cs
     */
    public void removeCallEdgeFor(CallSite cs)
    {
        for (int i = 0, e = size(); i < e; i++)
        {
            if (calledFunctions.get(i).first.equals(cs))
            {
                calledFunctions.remove(i);
                break;
            }
        }
    }

    /**
     * Remove all calls to the specified call graph node.
     * @param callee
     */
    public void removeAnyCallEdgeTo(CallGraphNode callee)
    {
        for (int i = 0, e = size(); i < e; i++)
        {
            if (calledFunctions.get(i).second.equals(callee))
            {
                calledFunctions.remove(i);
                --i;
                --e;
            }
        }
    }

    /**
     * remove the first one call to the specified call graph node which is abstract
     * (in other words, it is external).
     * @param callee
     */
    public void removeOneAbstractCallEdgeTo(CallGraphNode callee)
    {
        for (int i = 0, e = size(); i < e; i++)
        {
            if (calledFunctions.get(i).second.equals(callee) &&
                    callee.getFunction() == null)
            {
                calledFunctions.remove(i);
                break;
            }
        }
    }

    /**
     * Replace all call graph record from old with new one.
     * @param oldCS
     * @param newCS
     */
    public void replaceCallSite(CallSite oldCS, CallSite newCS)
    {
        Util.assertion(oldCS != null, "can not use null call site");
        for (int i = 0, e = size(); i < e; i++)
        {
            if (calledFunctions.get(i).first.equals(oldCS))
            {
                calledFunctions.get(i).first = newCS;
            }
        }
    }

    public boolean containsCalledFunction(CallGraphNode node)
    {
        for (Pair<CallSite, CallGraphNode> n : calledFunctions)
            if (n.second.equals(node))
                return true;
        return false;
    }

    /**
     * Removes the first call graph node when iterating on SCC.
     * @param node
     */
    public void removeCalledFunction(CallGraphNode node)
    {
        Util.assertion(containsCalledFunction(node),
                "must call this method when specified node contained in calledFunctions");
        for (Iterator<Pair<CallSite, CallGraphNode>> itr = iterator(); itr.hasNext(); )
        {
            if (itr.next().second == node) {
                itr.remove();
                return;
            }
        }
    }
}
