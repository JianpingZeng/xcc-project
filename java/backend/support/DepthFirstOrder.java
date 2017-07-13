package backend.support;
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

import backend.analysis.DomTreeNodeBase;
import backend.codegen.MachineBasicBlock;
import backend.value.BasicBlock;
import backend.utils.PredIterator;
import backend.utils.SuccIterator;

import java.util.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class DepthFirstOrder
{
    /**
     * Computes the reverse post order for the specified CFG from the start node.
     * The reverse post order of Basic Block is restored in returned list.
     * @param start
     * @return
     */
    public static LinkedHashSet<BasicBlock> reversePostOrder(BasicBlock start)
    {
        LinkedHashSet visited = new LinkedHashSet();
        reversePostOrder(start, visited, true);
        return visited;
    }

    /**
     * Computes the reverse post order for the specified CFG from the start node.
     * The reverse post order of Basic Block is restored in returned list.
     *
     * But the difference is this method can take a argument specifying the
     * visiting direction (down or up) with above method.
     * @return
     */
    public static void reversePostOrder(
            BasicBlock start,
            LinkedHashSet<BasicBlock> visited,
            boolean direction)
    {
        if (visited.contains(start))
            return;

        LinkedList<BasicBlock> worklist = new LinkedList<>();
        worklist.addLast(start);

        while (!worklist.isEmpty())
        {
            BasicBlock curr = worklist.removeLast();
            if (!visited.add(curr))
                continue;

            Stack<BasicBlock> res = new Stack<>();
            if (direction)
            {
                for (SuccIterator itr = curr.succIterator(); itr.hasNext();)
                    res.push(itr.next());
            }
            else
            {
                for (PredIterator<BasicBlock> itr = curr.predIterator(); itr.hasNext();)
                    res.push(itr.next());
            }
            if (!res.isEmpty())
                res.forEach(worklist::addLast);
        }
    }

    /**
     * Computes the reverse post order for the specified CFG from the start node.
     * The reverse post order of Basic Block is restored in returned list.
     * @param start
     * @return
     */
    public static LinkedHashSet<MachineBasicBlock> reversePostOrder(MachineBasicBlock start)
    {
        LinkedHashSet visited = new LinkedHashSet();
        reversePostOrder(start, visited, true);
        return visited;
    }

    /**
     * Computes the reverse post order for the specified CFG from the start node.
     * The reverse post order of Basic Block is restored in returned list.
     *
     * But the difference is this method can take a argument specifying the
     * visiting direction (down or up) with above method.
     * @return
     */
    public static void reversePostOrder(
            MachineBasicBlock start,
            LinkedHashSet<MachineBasicBlock> visited,
            boolean direction)
    {
        if (visited.contains(start))
            return;

        LinkedList<MachineBasicBlock> worklist = new LinkedList<>();
        worklist.addLast(start);

        while (!worklist.isEmpty())
        {
            MachineBasicBlock curr = worklist.removeLast();
            if (!visited.add(curr))
                continue;

            List<MachineBasicBlock> list = direction ?
                    curr.getSuccessors() : curr.getPredecessors();

            if (!list.isEmpty())
                list.forEach(worklist::addLast);
        }
    }

    public static LinkedList<BasicBlock> postOrder(BasicBlock startBlock)
    {
        LinkedList<BasicBlock> res = new LinkedList<>();
        HashSet<BasicBlock> visited = new HashSet<>();
        visit(startBlock, res, visited);
        return res;
    }

    private static void visit(BasicBlock bb, LinkedList<BasicBlock> res,
            HashSet<BasicBlock> visited)
    {
        if (visited.add(bb))
        {
            for (SuccIterator itr = bb.succIterator(); itr.hasNext(); )
                visit(itr.next(), res, visited);

            res.add(bb);
        }
    }

    public static LinkedList<DomTreeNodeBase<BasicBlock>> dfTravesal(
            DomTreeNodeBase<BasicBlock> entryNode)
    {
        LinkedList<DomTreeNodeBase<BasicBlock>> ret = new LinkedList<>();
        Stack<DomTreeNodeBase<BasicBlock>> stack = new Stack<>();
        stack.push(entryNode);
        ArrayList<DomTreeNodeBase<BasicBlock>> temps = new ArrayList<>();

        while (!stack.isEmpty())
        {
            DomTreeNodeBase<BasicBlock> cur = stack.pop();
            ret.add(cur);

            temps.clear();
            Collections.copy(temps, cur.getChildren());

            Collections.reverse(temps);
            temps.forEach(stack::push);
        }
        return ret;
    }
}
