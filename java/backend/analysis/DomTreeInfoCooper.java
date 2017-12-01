/*
 * Extremely C language Compiler
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

package backend.analysis;

import backend.utils.PredIterator;
import backend.value.BasicBlock;
import backend.value.Function;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static backend.support.DepthFirstOrder.dfTraversal;

/**
 * This file defines a class named of {@linkplain DomTreeInfoCooper} to compute
 * Dominator tree and Immediately Dominator. The inspection of implementation
 * stems from the literature "Cooper K D, Harvey T J, Kennedy K. A Simple,
 * Fast Dominance Algorithm[J]. Software Practice & Experience, 2001."
 * <p>
 * It argue that the finely tuned iterable algorithm competitive with well known
 * "Thomas lengauer and Robert Endre Tarjan's fast algorithm" even if in the
 * scenario of larger unrealistic program graph.
 * </p>
 * <p>
 * More important, when running on smaller program graph (number of nodes less than
 * 2000) with tuned Cooper algorithm is 2.5x faster than LT algorithm. Another
 * advantage versus LT algorithm is the cooper algorithm easily to be implemented
 * and tested.
 * </p>
 * @author Xlous.zeng
 * @version 0.1
 */
public final class DomTreeInfoCooper implements IDomTreeInfo
{
    /**
     * An index array whose element's value is index of idom.
     * The index is number of Reverse PostOrder on CFG.
     */
    private int[] doms;
    private ArrayList<BasicBlock> reversePostOrder;
    private ArrayList<BasicBlock> roots;
    private DomTreeNodeBase<BasicBlock> rootNodes;
    private HashMap<BasicBlock, DomTreeNodeBase<BasicBlock>> bb2DomTreeNode;
    private Function fn;
    private TObjectIntHashMap<BasicBlock> bb2Number;
    private static final int UNDEF = -1;

    @Override
    public void recalculate(Function f)
    {
        if (f == null) return;

        fn = f;
        BasicBlock entryBB = f.getEntryBlock();
        reversePostOrder = dfTraversal(entryBB);
        doms = new int[reversePostOrder.size()];
        bb2Number = new TObjectIntHashMap<>();

        rootNodes = new DomTreeNodeBase<>(entryBB, null);
        bb2DomTreeNode = new HashMap<>();

        int e = reversePostOrder.size();
        for (int i = 0; i < e; i++)
        {
            BasicBlock bb = reversePostOrder.get(i);
            bb2Number.put(bb, e - 1 - i);
            // initially setting the idom node as null
            bb2DomTreeNode.put(bb, new DomTreeNodeBase<>(bb, null));
        }

        roots = new ArrayList<>();
        roots.add(entryBB);

        // Step#1: initialize array doms with undefined value(-1)
        Arrays.fill(doms, UNDEF);
        // set the idom of start node as itself
        doms[e-1] = e-1;

        // Step#2: iterate until no changed on doms
        boolean changed = true;
        while (changed)
        {
            changed = false;
            for (int i = 1; i < e; i++)
            {
                BasicBlock bb = reversePostOrder.get(i);
                int numPreds = bb.getNumPredecessors();
                if (numPreds <= 0)
                    continue;

                int newIdom = bb2Number.get(bb.predAt(0));
                for (int j = 1; j < numPreds; j++)
                {
                    int pred = bb2Number.get(bb.predAt(j));
                    if (doms[pred] != UNDEF)
                        newIdom = interset(pred, newIdom);
                }
                if (doms[i] != newIdom)
                {
                    doms[i] = newIdom;
                    changed = true;
                }
            }
        }

        // Step#3: create a dom tree
        createDomTree();

        // For debug
        if (Util.DEBUG) dump();
    }

    private int interset(int finger1, int finger2)
    {
        while (finger1 != finger2)
        {
            while (finger1 < finger2)
                finger1 = doms[finger1];
            while (finger2 < finger1)
                finger2 = doms[finger2];
        }
        return finger1;
    }

    private void createDomTree()
    {
        for (int i = 0; i < doms.length; i++)
        {
            int idomIdx = doms[i];
            // skip entry bb
            if (i == idomIdx) continue;

            BasicBlock idomBB = reversePostOrder.get(idomIdx);
            bb2DomTreeNode.get(reversePostOrder.get(i))
                    .setIDom(bb2DomTreeNode.get(idomBB));
        }
    }

    @Override
    public ArrayList<BasicBlock> getRoots()
    {
        return roots;
    }

    @Override
    public DomTreeNodeBase<BasicBlock> getRootNode()
    {
        return rootNodes;
    }

    @Override
    public DomTreeNodeBase<BasicBlock> getTreeNodeForBlock(BasicBlock bb)
    {
        return bb2DomTreeNode.get(bb);
    }

    @Override
    public boolean isPostDominators()
    {
        return false;
    }

    @Override
    public boolean dominates(DomTreeNodeBase<BasicBlock> A,
            DomTreeNodeBase<BasicBlock> B)
    {
        assert A.getBlock().getParent() == fn
                && B.getBlock().getParent() == A.getBlock().getParent();

        if (A == B)
            return true;
        while (B != A && B != null)
        {
            B = B.getIDom();
        }

        return B != null;
    }

    @Override
    public boolean dominates(BasicBlock A, BasicBlock B)
    {
        assert A.getParent() == fn && B.getParent() == A.getParent();

        if (A == B)
            return true;
        int indexA = bb2Number.get(A);
        int indexB = bb2Number.get(B);
        while (indexB != indexA && indexB != doms[indexB])
        {
            indexB = doms[indexB];
        }

        return indexB != doms[indexB];
    }

    @Override
    public boolean strictDominate(DomTreeNodeBase<BasicBlock> A,
            DomTreeNodeBase<BasicBlock> B)
    {
        return dominates(A, B) && A != B;
    }

    @Override
    public boolean strictDominate(BasicBlock a, BasicBlock b)
    {
        return dominates(a, b) && a != b;
    }

    @Override
    public boolean isReachableFromEntry(BasicBlock bb)
    {
        int idx = bb2Number.get(bb);
        return doms[idx] != UNDEF;
    }

    @Override
    public boolean isReachableFromEntry(DomTreeNodeBase<BasicBlock> node)
    {
        return isReachableFromEntry(node.getBlock());
    }

    @Override
    public BasicBlock getIDom(BasicBlock block)
    {
        return reversePostOrder.get(doms[bb2Number.get(block)]);
    }

    @Override
    public BasicBlock findNearestCommonDominator(BasicBlock bb1, BasicBlock bb2)
    {
        if (bb1 == null || bb2 == null)
            return null;

        if (bb1.getParent() != bb2.getParent() && bb1.getParent() == fn)
            return null;

        int idx1 = bb2Number.get(bb1);
        int idx2 = bb2Number.get(bb2);
        while (idx1 != idx2)
        {
            while (idx1 < idx2)
                idx1 = doms[idx1];
            while (idx2 < idx1)
                idx2 = doms[idx2];
        }
        return reversePostOrder.get(idx1);
    }

    @Override
    public void eraseNode(BasicBlock bb)
    {
        assert bb != null;

        int index = bb2Number.get(bb);
        boolean exist = false;
        for (int i : doms)
        {
            if (i == index)
            {
                exist = true;
                break;
            }
        }
        assert !exist:"Can not remove non-leaf node";
        doms[index] = UNDEF;
        bb2Number.remove(bb);
        bb2DomTreeNode.remove(bb);
        if (roots.contains(bb)) roots.remove(bb);
        if (rootNodes.getBlock().equals(bb)) rootNodes = null;
    }

    @Override
    public void splitBlock(BasicBlock newBB)
    {
        int e = newBB.getNumSuccessors();
        BasicBlock succ = newBB.suxAt(0);
        assert e == 1 && succ != null : "newBB must have a single successor";

        ArrayList<BasicBlock> preds = new ArrayList<>();
        for (PredIterator<BasicBlock> itr = newBB.predIterator(); itr.hasNext();)
            preds.add(itr.next());

        assert !preds.isEmpty() :"No predecessors block!";

        boolean newBBDominatesSucc = true;
        for (PredIterator<BasicBlock> succPredItr = succ.predIterator();
             succPredItr.hasNext();)
        {
            BasicBlock p = succPredItr.next();
            if (p != newBB && !dominates(succ, p)
                    && isReachableFromEntry(p))
            {
                newBBDominatesSucc = false;
                break;
            }
        }

        // Find newBB's immediate dominator and create new dominator tree node
        // for newBB.
        BasicBlock newBBIDom = null;
        int i = 0;
        for (; i < preds.size(); i++)
        {
            if (isReachableFromEntry(preds.get(i)))
            {
                newBBIDom = preds.get(i);
                break;
            }
        }

        // It's possible that none of the predecessors of NewBB are reachable;
        // in that case, NewBB itself is unreachable, so nothing needs to be
        // changed.
        if (newBBIDom == null)
            return;

        for (i += 1; i < preds.size(); i++)
        {
            if (isReachableFromEntry(preds.get(i)))
                newBBIDom = findNearestCommonDominator(newBB, preds.get(i));
        }

        // Create a new dominator tree node, and set it as the idom of newBB.
        DomTreeNodeBase<BasicBlock> newBBNode = addNewBlock(newBB, newBBIDom);

        // If newBB strictly dominates other blocks, then it is now the immediate
        // dominator of cucc.  Update the dominator tree as appropriate.
        if (newBBDominatesSucc)
        {
            DomTreeNodeBase<BasicBlock> newBBSuccNode = getTreeNodeForBlock(succ);
            changeIDom(newBBSuccNode, newBBNode);
        }
    }

    @Override
    public DomTreeNodeBase<BasicBlock> addNewBlock(BasicBlock bb,
            BasicBlock idom)
    {
        assert bb != null && idom != null && bb2Number.containsKey(idom);
        int bbIndex = reversePostOrder.size() - reversePostOrder.indexOf(bb) - 1;
        int idomIndex = bb2Number.get(idom);
        doms[bbIndex] = idomIndex;
        bb2Number.put(bb, bbIndex);
        DomTreeNodeBase<BasicBlock> domBB = new DomTreeNodeBase<>(
                bb, bb2DomTreeNode.get(idom));
        bb2DomTreeNode.put(bb, domBB);
        return domBB;
    }

    @Override
    public void changeIDom(DomTreeNodeBase<BasicBlock> oldIDom,
            DomTreeNodeBase<BasicBlock> newIDom)
    {
        assert bb2Number.containsKey(oldIDom.getBlock()) &&
                bb2Number.containsKey(newIDom.getBlock());
        int oldIdomIndex = bb2Number.get(oldIDom.getBlock());
        int newIdomIndex = bb2Number.get(newIDom.getBlock());
        for (int idx = 0; idx < doms.length; idx++)
        {
            if (doms[idx] == oldIdomIndex)
                doms[idx] = newIdomIndex;
        }

        for (DomTreeNodeBase<BasicBlock> domBB : bb2DomTreeNode.values())
        {
            if (domBB.getIDom().equals(oldIDom))
                domBB.setIDom(newIDom);
        }
    }

    @Override
    public void changeIDom(BasicBlock oldIDomBB, BasicBlock newIDomBB)
    {
        assert bb2Number.containsKey(oldIDomBB) &&
                bb2Number.containsKey(newIDomBB);
        int oldIdomIndex = bb2Number.get(oldIDomBB);
        int newIdomIndex = bb2Number.get(newIDomBB);
        for (int idx = 0; idx < doms.length; idx++)
        {
            if (doms[idx] == oldIdomIndex)
                doms[idx] = newIdomIndex;
        }

        for (BasicBlock bb : bb2DomTreeNode.keySet())
        {
            if (bb2DomTreeNode.get(bb).getIDom().getBlock().equals(oldIDomBB))
            {
                bb2DomTreeNode.put(bb, bb2DomTreeNode.get(newIDomBB));
            }
        }
    }

    public void dump()
    {
        for (BasicBlock bb : reversePostOrder)
        {
            if (bb.hasName())
                System.err.println("BB_"+bb.getName());
            else
                System.err.printf("BB_0x%x ", bb.hashCode());
        }

        System.err.println();

        // Draw dot graph with graph-java
        for (int i = 0; i < doms.length; i++)
        {
            // the index of idom
            BasicBlock src = reversePostOrder.get(i);
            if (src.hasName())
                System.err.print(src.getName());
            else
                System.err.printf("BB_0x%x", src.hashCode());

            int idomIdx = doms[i];
            if (idomIdx != i)
            {
                BasicBlock dest = reversePostOrder.get(idomIdx);
                if (dest.hasName())
                    System.err.printf("--->BB_%s", dest.getName());
                else
                    System.err.printf("--->BB_0x%x", dest.hashCode());
            }
            System.err.println();
        }
    }
}
