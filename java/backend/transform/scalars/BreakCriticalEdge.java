package backend.transform.scalars;
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

import backend.analysis.DomTree;
import backend.analysis.DomTreeNodeBase;
import backend.analysis.DominanceFrontier;
import backend.analysis.LoopInfo;
import backend.pass.AnalysisResolver;
import backend.pass.AnalysisUsage;
import backend.pass.FunctionPass;
import backend.pass.Pass;
import backend.utils.PredIterator;
import backend.value.BasicBlock;
import backend.value.Function;
import backend.value.Instruction;
import backend.value.Instruction.BranchInst;
import backend.value.Instruction.PhiNode;
import backend.value.Instruction.TerminatorInst;
import backend.value.Loop;
import tools.Util;

import java.util.HashSet;
import java.util.LinkedList;

/**
 * This file define a pass which operates on LLVM IR to break critical edge as
 * following.
 * <pre>
 *     P1      P2    [Two predecessor blocks]
 *      \     /
 *       \   /
 *         BB        [the center BasicBlock]
 *      /    \
 *     S1      S2    [Two successor blocks]
 * </pre>
 * to following.
 * <pre>
 *     P1      P2    [Two predecessor blocks]
 *      \     /
 *       Sentinel    [The latest created block]
 *         |
 *         BB        [the center BasicBlock]
 *      /    \
 *     S1      S2    [Two successor blocks]
 * </pre>
 * @author Xlous.zeng
 * @version 0.1
 */
public final class BreakCriticalEdge implements FunctionPass
{
    private int numBroken;

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
    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.addPreserved(DomTree.class);
        au.addPreserved(LoopInfo.class);
        au.addPreserved(DominanceFrontier.class);
    }

    @Override
    public String getPassName()
    {
        return "Break critical edges in CFG";
    }
    private LinkedList<BasicBlock> blocks;
    @Override
    public boolean runOnFunction(Function f)
    {
        boolean changed = false;
        blocks = f.getBasicBlockList();
        for (int i = 0; i < blocks.size(); i++)
        {
            BasicBlock bb = blocks.get(i);
            TerminatorInst ti = bb.getTerminator();
            Util.assertion( ti != null);
            if (ti.getNumOfSuccessors() > 1)
            {
                for (int j = 0, e = ti.getNumOfSuccessors(); j < e; j++)
                {
                    if (splitCriticalEdge(ti, j, this))
                    {
                        numBroken++;
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    public static boolean isCriticalEdge(TerminatorInst ti,
            int succNum,
            boolean allowIdenticalEdges)
    {
        Util.assertion(succNum < ti.getNumOfSuccessors(),  "Illegal edge specified!");
        if (ti.getNumOfSuccessors() == 1)
            return false;

        BasicBlock dest = ti.getSuccessor(succNum);
        PredIterator<BasicBlock> predItr = dest.predIterator();

        Util.assertion(predItr.hasNext(),  "No preds");
        BasicBlock firstPred = predItr.next();
        if (!allowIdenticalEdges)
            return predItr.hasNext();

        while (predItr.hasNext())
        {
            BasicBlock curPred = predItr.next();
            if (curPred != firstPred)
                return true;
        }
        return false;
    }

    public static boolean splitCriticalEdge(TerminatorInst ti,
            int succNum,
            Pass pass)
    {
        return splitCriticalEdge(ti, succNum, pass, false);
    }

    /**
     * If this edge is a critical edge, insert a new node to
     * split the critical edge.  This will update DominatorTree and
     * DominanceFrontier  information if it is available, thus calling this pass
     * will not invalidate  any of them.  This returns true if the edge was split,
     * false otherwise.  This ensures that all edges to that dest go to one block
     * instead of each going to a different block.
     * @param ti
     * @param succNum
     * @param pass
     * @return
     */
    public static boolean splitCriticalEdge(TerminatorInst ti,
            int succNum,
            Pass pass, boolean mergeIdenticalEdge)
    {
        if (!isCriticalEdge(ti, succNum, mergeIdenticalEdge))
            return false;
        BasicBlock tibb = ti.getParent();
        BasicBlock destBB = ti.getSuccessor(succNum);
        Function f = tibb.getParent();

        // Create a new block, linking it into the CFG.
        BasicBlock newBB = BasicBlock.createBasicBlock(
                tibb.getName()+"."+destBB.getName()+"_crit_edge",
                f);
        BranchInst br = new BranchInst(destBB, newBB);
        ti.setSuccessor(succNum, newBB);

        // Insert the newBB into the correct position right after tibb.
        f.addBasicBlockBefore(tibb, newBB);

        // If there are any PHI nodes in DestBB, we need to update them so that they
        // merge incoming values from NewBB instead of from TIBB.
        Instruction inst;
        for (int i = 0, e = destBB.size();
             i < e && ((inst = destBB.getInstAt(i)) instanceof PhiNode);
             i++)
        {
            PhiNode pn = (PhiNode)destBB.getInstAt(i);
            int bbIdx = pn.getBasicBlockIndex(tibb);
            pn.setIncomingBlock(bbIdx, newBB);
        }

        if (mergeIdenticalEdge)
        {
            for (int i = succNum+1, e = ti.getNumOfSuccessors(); i < e; i++)
            {
                if (ti.getSuccessor(i) != destBB)
                    continue;

                destBB.removePredecessor(tibb);
                ti.setSuccessor(i, newBB);
            }
        }

        if (pass == null) return true;

        // Now update analysis information.  Since the only predecessor of NewBB is
        // the TIBB, TIBB clearly dominates NewBB.  TIBB usually doesn't dominate
        // anything, as there are other successors of DestBB.  However, if all other
        // predecessors of DestBB are already dominated by DestBB (e.g. DestBB is a
        // loop header) then NewBB dominates DestBB.
        LinkedList<BasicBlock> otherPreds = new LinkedList<>();

        for (PredIterator predItr = destBB.predIterator(); predItr.hasNext();)
        {
            BasicBlock pred = predItr.next();
            if (pred != newBB)
                otherPreds.add(pred);
        }

        boolean newBBDominatesDestBB = true;
        DomTree dt = (DomTree) pass.getAnalysisToUpDate(DomTree.class);
        if (dt != null)
        {
            DomTreeNodeBase<BasicBlock> tiNode = dt.getNode(tibb);

            if (tiNode != null)
            {
                DomTreeNodeBase<BasicBlock> newBBNode = dt.getNode(newBB);
                DomTreeNodeBase<BasicBlock> destBBNode = null;

                if (!otherPreds.isEmpty())
                {
                    destBBNode = dt.getNode(destBB);
                    while (!otherPreds.isEmpty() && newBBDominatesDestBB)
                    {
                        DomTreeNodeBase<BasicBlock> opNode;
                        if ((opNode = dt.getNode(otherPreds.pop())) != null)
                            newBBDominatesDestBB = dt.dominates(destBBNode, opNode);
                    }
                    otherPreds.clear();
                }

                if (newBBDominatesDestBB)
                {
                    if(destBBNode == null)
                        destBBNode = dt.getNode(destBB);
                    dt.changeIDom(destBBNode, newBBNode);
                }
            }
        }

        // Update dominator frontier information.
        DominanceFrontier df = (DominanceFrontier) pass.getAnalysisToUpDate(DominanceFrontier.class);
        if (df != null)
        {
            if (!otherPreds.isEmpty())
            {
                Util.shouldNotReachHere("Requiring domfrontiers but not idom/domtree/domset. "
                        + "not implemented yet!");
            }

            // Since the new block is dominated by its only predecessor TIBB,
            // it cannot be in any block's dominance frontier.  If NewBB dominates
            // DestBB, its dominance frontier is the same as DestBB's, otherwise it is
            // just {DestBB}.
            HashSet<BasicBlock> newDFSet;
            if (newBBDominatesDestBB)
            {
                newDFSet = df.find(destBB);
                if (newDFSet != null)
                {
                    df.addBasicBlock(newBB, newDFSet);

                    if (newDFSet.contains(destBB))
                    {
                        df.removeFromFrontier(newBB, destBB);
                    }
                }
                else
                {
                    df.addBasicBlock(newBB, new HashSet<>());
                }
            }
            else
            {
                HashSet<BasicBlock> set = new HashSet<>();
                set.add(destBB);
                df.addBasicBlock(newBB, set);
            }
        }

        // Update LoopInfo if it is around.
        LoopInfo li;
        if ((li = (LoopInfo) pass.getAnalysisToUpDate(LoopInfo.class)) != null)
        {
            Loop loop;
            if ((loop = li.getLoopFor(tibb)) != null)
            {
                // If one or the other blocks were not in a loop, the new block is not
                // either, and thus LI doesn't need to be updated.
                Loop destLoop = li.getLoopFor(destBB);
                if (destBB != null)
                {
                    if (loop == destLoop)
                    {
                        // Both in the same loop, the NewBB joins loop.
                        destLoop.addBasicBlockIntoLoop(newBB, li);
                    }
                    else if (loop.contains(destLoop.getHeaderBlock()))
                    {
                        // Edge from an outer loop to an inner loop.  Add to the outer loop.
                        loop.addBasicBlockIntoLoop(newBB, li);
                    }
                    else if (destLoop.contains(loop.getHeaderBlock()))
                    {
                        // Edge from an inner loop to an outer loop.  Add to the outer loop.
                        destLoop.addBasicBlockIntoLoop(newBB, li);
                    }
                    else
                    {
                        // Edge from two loops with no containment relation.  Because these
                        // are natural loops, we know that the destination block must be the
                        // header of its loop (adding a branch into a loop elsewhere would
                        // create an irreducible loop).
                        Util.assertion(destLoop.getHeaderBlock() == destBB,                                 "Should not create irreducible loops!");

                        Loop parentLoop = destLoop.getParentLoop();
                        if (parentLoop != null)
                        {
                            parentLoop.addBasicBlockIntoLoop(newBB, li);
                        }
                    }
                }
            }
        }
        return true;
    }
}
