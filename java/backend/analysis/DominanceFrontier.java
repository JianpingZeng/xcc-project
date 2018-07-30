package backend.analysis;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous
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

import tools.Util;
import backend.pass.AnalysisResolver;
import backend.pass.AnalysisUsage;
import backend.utils.PredIterator;
import backend.utils.SuccIterator;
import backend.value.BasicBlock;
import backend.value.Function;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

import static backend.support.DepthFirstOrder.dfTraversal;

/**
 * Concrete subclass of DominanceFrontierBase that is used to compute a
 * forward dominator frontiers.
 * @author Jianping Zeng
 * @version 0.1
 */
public final class DominanceFrontier extends DominanceFrontierBase
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
    public DominanceFrontier()
    {
        super(false);
    }

    public BasicBlock getRoot()
    {
        Util.assertion(roots.size() == 1, "Should always has one node!");
        return roots.get(0);
    }

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.setPreservedAll();
        au.addRequired(DomTree.class);
    }

    @Override
    public String getPassName()
    {
        return "Dominance Frontier Construction";
    }

    @Override
    public boolean runOnFunction(Function f)
    {
        frontiers.clear();
        DomTree dt = (DomTree)getAnalysisToUpDate(DomTree.class);
        Util.assertion(dt != null, "No available DomTree pass");
        roots = dt.getRoots();
        Util.assertion(roots.size() == 1, "Only have one root block!");
        calculateCooper(dt, dt.getRoot());
        return false;
    }

    /**
     * Compute dominance frontier for each graph node according to Cooper algorithm
     */
    private void calculateCooper(DomTree dt, BasicBlock entry)
    {
        // Inverse post-order of CFG
        ArrayList<BasicBlock> dfs = dfTraversal(entry);
        IDomTreeInfo di = dt.getDomTree();
        for (BasicBlock bb : dfs)
        {
            int e = bb.getNumPredecessors();
            if (e > 1)
            {
                BasicBlock idom = di.getIDom(bb);
                for (int i = 0; i < e; i++)
                {
                    BasicBlock runner = bb.predAt(i);
                    while (!runner.equals(idom))
                    {
                        // add to the runner's dominance frontier set
                        if (frontiers.containsKey(runner))
                        {
                            frontiers.get(runner).add(bb);
                        }
                        else
                        {
                            HashSet<BasicBlock> set = new HashSet<>();
                            set.add(bb);
                            frontiers.put(runner, set);
                        }
                        runner = di.getIDom(runner);
                    }
                }
            }
        }
    }

    /**
     * newBB is split and now it has one successor.
     * Update dominance frontier to reflect this change.
     * @param newBB
     */
    public void splitBlock(BasicBlock newBB)
    {
        Util.assertion(newBB.getTerminator().getNumOfSuccessors() == 1, "newBB should have a single successors");

        BasicBlock succ = newBB.getTerminator().getSuccessor(0);

        ArrayList<BasicBlock> predBlocks = new ArrayList<>();
        for (PredIterator<BasicBlock> itr = newBB.predIterator(); itr.hasNext();)
            predBlocks.add(itr.next());

        if (predBlocks.isEmpty())
            return;

        HashSet<BasicBlock> newBBFrontier = find(newBB);
        if (newBBFrontier != null)
        {
            HashSet<BasicBlock> succSet = new HashSet<>(newBBFrontier);
            addBasicBlock(succ, succSet);
        }

        DomTree dt = (DomTree) getAnalysisToUpDate(DomTree.class);
        if (dt != null)
        {
            if (dt.dominates(newBB, succ))
            {
                HashSet<BasicBlock> set = find(predBlocks.get(0));
                if (set != null)
                {
                    Iterator<BasicBlock> itr = set.iterator();
                    while (itr.hasNext())
                    {
                        BasicBlock frontierBB = itr.next();
                        boolean dominatePred = false;
                        for (PredIterator<BasicBlock> predItr = frontierBB.predIterator(); predItr.hasNext(); )
                            if (dt.dominates(newBB, frontierBB))
                                dominatePred = true;
                        if (!dominatePred)
                            set.remove(frontierBB);
                    }

                    if (newBBFrontier != null)
                    {
                        newBBFrontier.forEach(sb -> addToFrontier(newBB, sb));
                    }
                    else
                    {
                        addBasicBlock(newBB, set);
                    }
                }
            }
            else
            {
                HashSet<BasicBlock> newDFSet = new HashSet<>();
                newDFSet.add(succ);
                addBasicBlock(newBB, newDFSet);
            }

            for (BasicBlock dfBB : newBB.getParent().getBasicBlockList())
            {
                HashSet<BasicBlock> dfSet = find(dfBB);
                if (dfSet == null)
                    continue; // unreachable block.

                // Only consider nodes that have succ in their dominator frontier set.
                if (!dfSet.contains(succ))
                    continue;

                boolean blockDominateAny = false;
                for (BasicBlock pred : predBlocks)
                {
                    if (dt.dominates(dfBB, pred))
                    {
                        blockDominateAny = true;
                        break;
                    }
                }

                boolean shouldRemove = true;
                if (dfBB == succ || !dt.dominates(dfBB, succ))
                    for (PredIterator<BasicBlock> predItr = succ
                            .predIterator(); predItr.hasNext(); )
                    {
                        if (dt.dominates(dfBB, predItr.next()))
                        {
                            shouldRemove = false;
                            break;
                        }
                    }

                if (shouldRemove)
                    removeFromFrontier(dfBB, succ);
                if (blockDominateAny && dfBB == newBB || !dt
                        .dominates(dfBB, newBB))
                    addToFrontier(dfBB, newBB);
            }
        }
    }

    /**
     * BasicBlock parent's new dominator is newBB. Update parent's dominance frontier
     * to reflect this change.
     * @param bb
     * @param newBB
     * @param dt
     */
    public void changeIDom(BasicBlock bb, BasicBlock newBB,
            DomTree dt)
    {
         HashSet<BasicBlock> newDF = find(newBB);
         HashSet<BasicBlock> df = find(bb);

         if (df == null)
             return;

         for (BasicBlock dfMember : df)
         {
             if (!dt.dominates(newBB, dfMember))
                 newDF.add(dfMember);
         }
         newDF.remove(bb);
    }

    /**
     * Calculate the dominator frontier for each graph node. Return a set of
     * df node.
     * @param dt
     * @param node
     * @return
     */
    private HashSet<BasicBlock> calculate(DomTree dt,
            DomTreeNodeBase<BasicBlock> node)
    {
        BasicBlock bb = node.getBlock();
        HashSet<BasicBlock> result = null;

        Stack<DFCalcualteWorkObject> worklist = new Stack<>();
        HashSet<BasicBlock> visited = new HashSet<>();

        worklist.push(new DFCalcualteWorkObject(bb, null, node, null));
        do
        {
            DFCalcualteWorkObject curObject = worklist.pop();
            Util.assertion(curObject!= null, "Missing work object.");

            BasicBlock currentBB = curObject.currentBB;
            BasicBlock parentBB = curObject.parentBB;
            DomTreeNodeBase<BasicBlock> currentNode = curObject.node;
            DomTreeNodeBase<BasicBlock> parentNode = curObject.parentNode;

            Util.assertion( currentBB != null);
            Util.assertion( currentNode != null);

            HashSet<BasicBlock> s = frontiers.get(currentBB);

            if (!visited.contains(currentBB))
            {
                visited.add(currentBB);

                for (SuccIterator itr = currentBB.succIterator(); itr.hasNext();)
                {
                    BasicBlock suc = itr.next();
                    if (dt.getNode(suc).getIDom() != currentNode)
                        s.add(suc);
                }
            }

            boolean visitChild = false;
            for (DomTreeNodeBase<BasicBlock> child : currentNode.getChildren())
            {
                BasicBlock childBB = child.getBlock();
                if (!visited.contains(childBB))
                {
                    worklist.add(new DFCalcualteWorkObject(childBB, currentBB, child, currentNode));
                    visitChild = true;
                }
            }

            if (!visitChild)
            {
                if (parentBB == null)
                {
                    result = s;
                    break;
                }

                HashSet<BasicBlock> parentSet = frontiers.get(parentBB);
                for (BasicBlock childF : s)
                {
                    if (!dt.strictDominates(parentNode, dt.getNode(childF)))
                        parentSet.add(childF);
                }
            }
        }while (!worklist.isEmpty());
        return result;
    }

    private static class DFCalcualteWorkObject
    {
        public BasicBlock currentBB;
        public BasicBlock parentBB;
        public DomTreeNodeBase<BasicBlock> node;
        public DomTreeNodeBase<BasicBlock> parentNode;

        public DFCalcualteWorkObject(BasicBlock currentBB,
                BasicBlock parentBB,
                DomTreeNodeBase<BasicBlock> node,
                DomTreeNodeBase<BasicBlock> parentNode)
        {
            this.currentBB = currentBB;
            this.parentBB = parentBB;
            this.node = node;
            this.parentNode = parentNode;
        }
    }
}
