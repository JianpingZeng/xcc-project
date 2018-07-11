/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.analysis;

import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineFunction;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static backend.support.DepthFirstOrder.dfTraversal;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class MachineDomTreeInfoCooper implements IMachineDomTreeInfo
{
    /**
     * An index array whose element's value is index of idom.
     * The index is number of Reverse PostOrder on CFG.
     */
    private int[] doms;
    private ArrayList<MachineBasicBlock> reversePostOrder;
    private ArrayList<MachineBasicBlock> roots;
    private DomTreeNodeBase<MachineBasicBlock> rootNodes;
    private HashMap<MachineBasicBlock, DomTreeNodeBase<MachineBasicBlock>> bb2DomTreeNode;
    private MachineFunction fn;
    private TObjectIntHashMap<MachineBasicBlock> bb2Number;
    private static final int UNDEF = -1;

    @Override
    public void recalculate(MachineFunction f)
    {
        if (f == null) return;

        fn = f;
        MachineBasicBlock entryBB = f.getEntryBlock();
        reversePostOrder = dfTraversal(entryBB);
        doms = new int[reversePostOrder.size()];
        bb2Number = new TObjectIntHashMap<>();
        bb2DomTreeNode = new HashMap<>();

        int e = reversePostOrder.size();
        for (int i = 0; i < e; i++)
        {
            MachineBasicBlock bb = reversePostOrder.get(i);
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
            int idx = e-2;
            for (int i = 1; i < e; i++)
            {
                MachineBasicBlock bb = reversePostOrder.get(i);
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
                if (doms[idx] != newIdom)
                {
                    doms[idx] = newIdom;
                    changed = true;
                }
                --idx;
            }
            Util.assertion(idx == -1, "Remained unhandled MachineBasicBlock");
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
        int e = doms.length;
        for (int i = 0; i < e; i++)
        {
            int idomIdx = doms[i];
            // skip entry bb
            if (i == idomIdx) continue;

            MachineBasicBlock idomBB = reversePostOrder.get(e-1-idomIdx);

            bb2DomTreeNode.get(reversePostOrder.get(e-1-i))
                    .setIDom(bb2DomTreeNode.get(idomBB));
        }
        rootNodes = bb2DomTreeNode.get(reversePostOrder.get(0));
    }

    @Override
    public ArrayList<MachineBasicBlock> getRoots()
    {
        return roots;
    }

    @Override
    public DomTreeNodeBase<MachineBasicBlock> getRootNode()
    {
        return rootNodes;
    }

    @Override
    public DomTreeNodeBase<MachineBasicBlock> getTreeNodeForBlock(MachineBasicBlock bb)
    {
        return bb2DomTreeNode.get(bb);
    }

    @Override
    public boolean isPostDominators()
    {
        return false;
    }

    @Override
    public boolean dominates(DomTreeNodeBase<MachineBasicBlock> A,
            DomTreeNodeBase<MachineBasicBlock> B)
    {
        Util.assertion( A.getBlock().getParent() == fn                && B.getBlock().getParent() == A.getBlock().getParent());


        if (A == B)
            return true;
        while (B != A && B != null)
        {
            B = B.getIDom();
        }

        return B != null;
    }

    @Override
    public boolean dominates(MachineBasicBlock mbb1, MachineBasicBlock mbb2)
    {
        Util.assertion( mbb1.getParent() == fn && mbb2.getParent() == mbb1.getParent());

        if (mbb1.equals(mbb2))
            return true;
        int indexA = bb2Number.get(mbb1);
        int indexB = bb2Number.get(mbb2);
        while (indexB != indexA && indexB != doms[indexB])
        {
            indexB = doms[indexB];
        }

        return indexB == indexA;
    }

    @Override
    public boolean strictDominate(DomTreeNodeBase<MachineBasicBlock> a,
            DomTreeNodeBase<MachineBasicBlock> b)
    {
        return dominates(a, b) && a != b;
    }

    @Override
    public boolean strictDominate(MachineBasicBlock a, MachineBasicBlock b)
    {
        return dominates(a, b) && a != b;
    }

    @Override
    public boolean isReachableFromEntry(MachineBasicBlock bb)
    {
        int idx = bb2Number.get(bb);
        return doms[idx] != UNDEF;
    }

    @Override
    public boolean isReachableFromEntry(DomTreeNodeBase<MachineBasicBlock> node)
    {
        return isReachableFromEntry(node.getBlock());
    }

    @Override
    public MachineBasicBlock getIDom(MachineBasicBlock block)
    {
        int len = reversePostOrder.size()-1;
        return reversePostOrder.get(len-doms[bb2Number.get(block)]);
    }

    @Override
    public MachineBasicBlock findNearestCommonDominator(MachineBasicBlock bb1, MachineBasicBlock bb2)
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
    public void eraseNode(MachineBasicBlock bb)
    {
        Util.assertion( bb != null);

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
        Util.assertion(!exist, "Can not remove non-leaf node");
        doms[index] = UNDEF;
        bb2Number.remove(bb);
        bb2DomTreeNode.remove(bb);
        if (roots.contains(bb)) roots.remove(bb);
        if (rootNodes.getBlock().equals(bb)) rootNodes = null;
    }

    @Override
    public void splitBlock(MachineBasicBlock newBB)
    {
        int e = newBB.getNumSuccessors();
        MachineBasicBlock succ = newBB.suxAt(0);
        Util.assertion(e == 1 && succ != null,  "newBB must have a single successor");

        ArrayList<MachineBasicBlock> preds = newBB.getPredecessors();

        Util.assertion(!preds.isEmpty(), "No predecessors block!");

        boolean newBBDominatesSucc = true;
        for (MachineBasicBlock p : succ.getPredecessors())
        {
            if (p != newBB && !dominates(succ, p)
                    && isReachableFromEntry(p))
            {
                newBBDominatesSucc = false;
                break;
            }
        }

        // Find newBB's immediate dominator and create new dominator tree node
        // for newBB.
        MachineBasicBlock newBBIDom = null;
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
        DomTreeNodeBase<MachineBasicBlock> newBBNode = addNewBlock(newBB, newBBIDom);

        // If newBB strictly dominates other blocks, then it is now the immediate
        // dominator of cucc.  Update the dominator tree as appropriate.
        if (newBBDominatesSucc)
        {
            DomTreeNodeBase<MachineBasicBlock> newBBSuccNode = getTreeNodeForBlock(succ);
            changeIDom(newBBSuccNode, newBBNode);
        }
    }

    @Override
    public DomTreeNodeBase<MachineBasicBlock> addNewBlock(MachineBasicBlock bb,
            MachineBasicBlock idom)
    {
        Util.assertion( bb != null && idom != null && bb2Number.containsKey(idom));
        int bbIndex = reversePostOrder.size() - reversePostOrder.indexOf(bb) - 1;
        int idomIndex = bb2Number.get(idom);
        doms[bbIndex] = idomIndex;
        bb2Number.put(bb, bbIndex);
        DomTreeNodeBase<MachineBasicBlock> domBB = new DomTreeNodeBase<>(
                bb, bb2DomTreeNode.get(idom));
        bb2DomTreeNode.put(bb, domBB);
        return domBB;
    }

    @Override
    public void changeIDom(DomTreeNodeBase<MachineBasicBlock> oldIDom,
            DomTreeNodeBase<MachineBasicBlock> newIDom)
    {
        Util.assertion( bb2Number.containsKey(oldIDom.getBlock()) &&                bb2Number.containsKey(newIDom.getBlock()));

        int oldIdomIndex = bb2Number.get(oldIDom.getBlock());
        int newIdomIndex = bb2Number.get(newIDom.getBlock());
        for (int idx = 0; idx < doms.length; idx++)
        {
            if (doms[idx] == oldIdomIndex)
                doms[idx] = newIdomIndex;
        }

        for (DomTreeNodeBase<MachineBasicBlock> domBB : bb2DomTreeNode.values())
        {
            if (domBB.getIDom().equals(oldIDom))
                domBB.setIDom(newIDom);
        }
    }

    @Override
    public void changeIDom(MachineBasicBlock oldIDomBB, MachineBasicBlock newIDomBB)
    {
        Util.assertion( bb2Number.containsKey(oldIDomBB) &&                bb2Number.containsKey(newIDomBB));

        int oldIdomIndex = bb2Number.get(oldIDomBB);
        int newIdomIndex = bb2Number.get(newIDomBB);
        for (int idx = 0; idx < doms.length; idx++)
        {
            if (doms[idx] == oldIdomIndex)
                doms[idx] = newIdomIndex;
        }

        for (MachineBasicBlock bb : bb2DomTreeNode.keySet())
        {
            if (bb2DomTreeNode.get(bb).getIDom().getBlock().equals(oldIDomBB))
            {
                bb2DomTreeNode.put(bb, bb2DomTreeNode.get(newIDomBB));
            }
        }
    }

    public void dump()
    {
        for (MachineBasicBlock bb : reversePostOrder)
        {
            if (bb.getBasicBlock().hasName())
                System.err.println("MBB_"+ bb.getBasicBlock().getName());
            else
                System.err.printf("MBB_0x%x ", bb.hashCode());
        }

        System.err.println();

        // Draw dot graph with graph-java
        for (int i = 0; i < doms.length; i++)
        {
            // the index of idom
            MachineBasicBlock src = reversePostOrder.get(i);
            if (src.getBasicBlock().hasName())
                System.err.print(src.getBasicBlock().getName());
            else
                System.err.printf("BB_0x%x", src.hashCode());

            int idomIdx = doms[i];
            if (idomIdx != i)
            {
                MachineBasicBlock dest = reversePostOrder.get(idomIdx);
                if (dest.getBasicBlock().hasName())
                    System.err.printf("--->BB_%s", dest.getBasicBlock().getName());
                else
                    System.err.printf("--->BB_0x%x", dest.hashCode());
            }
            System.err.println();
        }
    }
}
