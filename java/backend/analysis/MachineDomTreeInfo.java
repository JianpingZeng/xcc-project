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
import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineFunction;
import tools.Pair;

import java.util.*;

/**
 * This file defines the MachineDomTreeInfo class, which provides fast and efficient
 * dominance construction and queries according to Lengauer-Tarjan algorithm for
 * Machine CFG.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public final class MachineDomTreeInfo
{
    /**
     * The root nodes set of this tree.
     */
    private ArrayList<MachineBasicBlock> roots;

    /**
     * Determiates whether its post dominance.
     */
    private boolean IsPostDominators;

    private HashMap<MachineBasicBlock, DomTreeNodeBase<MachineBasicBlock>> domTreeNodes;

    private HashMap<MachineBasicBlock, MachineBasicBlock> iDoms;

    private DomTreeNodeBase<MachineBasicBlock> rootNode;
    /**
     * Vertex - Map the DFS number to the MachineBasicBlock(usually block).
     */
    private MachineBasicBlock[] Vertex;

    /**
     * Info - Collection of information used during the computation of idoms.
     */
    private HashMap<MachineBasicBlock, InfoRecord<MachineBasicBlock>> info;

    private boolean DFSInfoValid = false;

    private int slowQueries = 0;

    private MachineFunction m;

    /**
     * Constructs a instance of creating dominator tree of a CFG.
     * @param isPostData    whether it is post dominator tree.
     */
    public MachineDomTreeInfo(boolean isPostData)
    {
        this.IsPostDominators = isPostData;
        this.info = new HashMap<>();
    }

    /**
     * Constructs a instance of creating dominator tree of a CFG.
     */
    public MachineDomTreeInfo()
    {
        this(false);
    }

    /**
     * Returns the root blocks of current CFG. This may include multiple blocks
     * if we are computing post dominators. For forward dominators, this wil always
     * be a single block (the entry block of CFG).
     * @return
     */
    public ArrayList<MachineBasicBlock> getRoots()
    {
        return roots;
    }

    /**
     * This returns the entry dominator tree node of the CFG attached to the
     * function. IfStmt this tree represents the post-dominator relation for a
     * function, however, this root may be a node with the block == null. This
     * is teh case when there are multiple exit nodes from a particular function.
     * @return
     */
    public DomTreeNodeBase<MachineBasicBlock> getRootNode()
    {
        return rootNode;
    }

    /**
     * Gets the corresponding dominator tree node for specifed basic block.
     * @param bb
     * @return
     */
    public DomTreeNodeBase<MachineBasicBlock> getTreeNodeForBlock(MachineBasicBlock bb)
    {
        return domTreeNodes.get(bb);
    }

    /**
     * Returns true if analysis based on postdoms.
     * @return
     */
    public boolean isPostDominators(){return IsPostDominators;}

    /**
     * Determine whether A dominates B.
     * @param A
     * @param B
     * @return  ReturnInst true iff A dominates B.
     */
    public boolean dominates(
            DomTreeNodeBase<MachineBasicBlock> A, DomTreeNodeBase<MachineBasicBlock> B)
    {
        if (A == B)
            return true;
        // An unreachable node is dominated by anything.
        if (!isReachableFromEntry(B))
            return true;
        // An unreachable node dominates by nothing.
        if (!isReachableFromEntry(A))
            return false;

        if (DFSInfoValid)
            return B.dominatedBy(A);

        // IfStmt we end up with too many slow queries, just update the
        // DFS numbers on the theory that we are going to keep querying.
        slowQueries++;
        if (slowQueries > 32)
        {
            updateDFSNumber();
            return B.dominatedBy(A);
        }

        return dominateBySlowTreeWalk(A, B);
    }

    public boolean dominates(MachineBasicBlock A, MachineBasicBlock B)
    {
        DomTreeNodeBase<MachineBasicBlock> first = domTreeNodes.get(A);
        DomTreeNodeBase<MachineBasicBlock> second = domTreeNodes.get(B);
        return dominates(first, second);
    }

    private boolean dominateBySlowTreeWalk(DomTreeNodeBase<MachineBasicBlock> A, DomTreeNodeBase<MachineBasicBlock> B)
    {
        Util.assertion( (A != B));
        Util.assertion( (isReachableFromEntry(B)));
        Util.assertion( (isReachableFromEntry(A)));

        DomTreeNodeBase<MachineBasicBlock> IDom = null;
        while ((IDom = B.getIDom()) != null && IDom != A && IDom != B)
        {
            // wolk up the tree.
            B = IDom;
        }
        return IDom != null;
    }

    /**
     * ReturnInst true if B dominated by A, but A != B.
     * @param A
     * @param B
     * @return
     */
    public boolean strictDominate(
            DomTreeNodeBase<MachineBasicBlock> A, DomTreeNodeBase<MachineBasicBlock> B)
    {
        if (A == null || B == null)
            return false;
        if (A == B)
            return false;
        return dominates(A, B);
    }

    public boolean strictDominate(MachineBasicBlock bb1, MachineBasicBlock bb2)
    {
        if (bb1 == null || bb2 == null)
            return false;
        if (bb1 == bb2)
            return false;
        return dominates(bb1, bb2);
    }

    /**
     * Determines whether BB is reachable from the entry block of a function.
     * @param BB
     * @return
     */
    public boolean isReachableFromEntry(MachineBasicBlock BB)
    {
        Util.assertion(isPostDominators(),                 "This is not implemented for post dominatror");

        MachineBasicBlock entry = m.getEntryBlock();
        return dominates(entry, BB);
    }

    public boolean isReachableFromEntry(DomTreeNodeBase<MachineBasicBlock> node)
    {
        Util.assertion(isPostDominators(),                 "This is not implemented for post dominatror");


        DomTreeNodeBase<MachineBasicBlock> entry = domTreeNodes.get(m.getEntryBlock());
        return dominates(entry, node);
    }

    /**
     * Gets the dominated block of given block.
     * @param block
     * @return
     */
    public MachineBasicBlock getIDom(MachineBasicBlock block)
    {
        DomTreeNodeBase<MachineBasicBlock> node = domTreeNodes.get(block);
        if (node == null)
            return null;
        return node.getIDom().getBlock();
    }

    /**
     * Recalculate - compute a dominator tree for the given function.
     */
    public void recalculate(MachineFunction mf)
    {
        m = mf;
        this.Vertex = new MachineBasicBlock[m.getNumBlockIDs()];
        if (!IsPostDominators)
        {
            // initialize the root
            MachineBasicBlock entry = m.getEntryBlock();
            this.roots.add(entry);
            this.info.put(entry, new InfoRecord());
            this.iDoms.put(entry, null);
            this.domTreeNodes.put(entry, null);
            caculate();
        }
        else
        {
            MachineBasicBlock exit = m.getEntryBlock();
            info.put(exit, new InfoRecord());
            roots.add(exit);
            iDoms.put(exit, null);
            domTreeNodes.put(exit, null);

            // The desired of refinition is need in the future.
            caculate();
        }
    }

    /**
     * An actual calculation of dominator tree method.
     */
    private void caculate()
    {
        boolean multipleRoots = this.roots.size() > 1;
        int N = 0;
        if (multipleRoots)
        {
            InfoRecord BBInfo = info.get(null);
            BBInfo.DFSNum = ++N;
            BBInfo.label = null;
        }

        // Step #1: Number blocks in depth-first order and initialize variables
        // used in later stages of the algorithm.
        for (MachineBasicBlock block : roots)
            N = DFS(block, N);

        // When naively implemented, the Lengauer-Tarjan algorithm requires a separate
        // bucket for each vertex. However, this is unnecessary, because each vertex
        // is only placed into a single bucket (that of its semi-dominator), and each
        // vertex's bucket is processed before it is added to any bucket itself.
        //
        // Instead of using a bucket per all vertexes, we use a single array Buckets that
        // has two purposes. Before the vertex V with preorder number i is processed,
        // Buckets[i] stores the index of the first element in V's bucket. After V's
        // bucket is processed, Buckets[i] stores the index of the next element in the
        // bucket containing V, if any.
        int[] buckets = new int[N + 1];
        for (int idx = 1; idx <= N; idx++)
            buckets[idx] = idx;

        // process all vertex one by one in decreasing order
        for (int i = N; i >= 2; --i)
        {
            // the block with number i
            MachineBasicBlock WBlock = Vertex[i];

            // the InfoRec of current block
            InfoRecord WInfo = info.get(WBlock);

            // Step #2: Implicitly define the immediate dominator of vertices
            for(int j = i; buckets[j] != i; j = buckets[i])
            {
                MachineBasicBlock V = this.Vertex[buckets[j]];
                MachineBasicBlock U = eval(V, i + 1);
                this.iDoms.put(V, this.info.get(U).semi < i ? U :WBlock);
            }

            // Step #3: caculate the semidominators of all vertice

            // initialize the semi dominator to point into it's parent node
            WInfo.semi = WInfo.parent;
            Iterator<MachineBasicBlock> succItr = WBlock.getSuccessors().iterator();
            while(succItr.hasNext())
            {
                MachineBasicBlock BB = succItr.next();
                int semiU = info.get(eval(BB, i+1)).semi;
                if (semiU < WInfo.semi)
                    WInfo.semi = semiU;
            }

            // IfStmt V is a non-root vertex and sdom(V) = parent(V), then idom(V) is
            // necessarily parent(V). In this case, set idom(V) here and avoid placing
            // V into a bucket.
            if (WInfo.semi == WInfo.parent)
                this.iDoms.put(WBlock, Vertex[WInfo.parent]);
            else
            {
                buckets[i] = buckets[WInfo.semi];
                buckets[WInfo.semi] = i;
            }

            if (N >= 1)
            {
                MachineBasicBlock root = Vertex[1];
                for (int j = 1; buckets[j] != 1; j = buckets[j])
                {
                    MachineBasicBlock V = Vertex[buckets[j]];
                    iDoms.put(V, root);
                }
            }

            // Step #4: Explicitly define the immediate dominator of each vertex
            for (i = 2; i <=N; i++)
            {
                MachineBasicBlock W = Vertex[i];
                MachineBasicBlock WIDom = iDoms.get(W);
                if (WIDom != Vertex[info.get(W).semi])
                    WIDom = iDoms.get(WIDom);
            }

            if (getRoots().isEmpty()) return;


            // Add a node for the root.  This node might be the actual root, if there is
            // one exit block, or it may be the virtual exit (denoted by (MachineBasicBlock *)0)
            // which postdominates all real exits if there are multiple exit blocks, or
            // an infinite loop.
            MachineBasicBlock root = !multipleRoots ? roots.get(0) : null;
            this.rootNode = new DomTreeNodeBase<MachineBasicBlock>(root, null);
            domTreeNodes.put(root, this.rootNode);

            // loop over all of the reachable blocks in the method.
            for (int idx = 2; idx <=N; idx++)
            {
                MachineBasicBlock W = this.Vertex[idx];
                DomTreeNodeBase<MachineBasicBlock> BBNode = this.domTreeNodes.get(W);

                // Haven't caculated this node yet?
                if (BBNode != null) continue;

                MachineBasicBlock ImmDom = this.iDoms.get(W);
                Util.assertion( (ImmDom != null || this.domTreeNodes.get(null) != null));

                // Get or calculates the node for the imediate dominator
                DomTreeNodeBase<MachineBasicBlock> IDomNode = this.getTreeNodeForBlock(ImmDom);

                // add a new tree node for this Basic block, and link it as a child of
                // IDomNode
                DomTreeNodeBase<MachineBasicBlock> children = new DomTreeNodeBase<MachineBasicBlock>(W, IDomNode);
                this.domTreeNodes.put(W, IDomNode.addChidren(children));
            }

            // free temporary memory used to construct idom.
            this.iDoms.clear();
            this.info.clear();
            this.Vertex = null;
            updateDFSNumber();
        }

    }

    /**
     * Updates DFS number - Assign In and Out numbers to the nodes
     * while walking dominator tree in dfs order.
     */
    private void updateDFSNumber()
    {
        int DFSNumber = 0;
        LinkedList<Pair<DomTreeNodeBase<MachineBasicBlock>, ListIterator<DomTreeNodeBase<MachineBasicBlock>>>> worklist = new LinkedList<>();

        DomTreeNodeBase<MachineBasicBlock> thisRoot = getRootNode();
        if (thisRoot == null)
            return;

        ListIterator<DomTreeNodeBase<MachineBasicBlock>> rit = thisRoot.getChildren().listIterator();

        // add a pair of root and predecessor into worklist in the order that
        // last firstly added.
        worklist.addLast(new Pair<>(thisRoot, rit));

        // Even in the case of multiple exits that form the post dominator root
        // nodes, do not iterate over all exits, but start from the virtual root
        // node. Otherwise bbs, that are not post dominated by any exit but by the
        // virtual root node, will never be assigned a DFS number.
        thisRoot.DFSNumIn = DFSNumber++;
        while (!worklist.isEmpty())
        {
            Pair<DomTreeNodeBase<MachineBasicBlock>, ListIterator<DomTreeNodeBase<MachineBasicBlock>>> top = worklist.getLast();
            DomTreeNodeBase<MachineBasicBlock> node = top.first;
            ListIterator<DomTreeNodeBase<MachineBasicBlock>> childItr = top.second;

            // IfStmt we visited all of the children of this node, "recurse" back up the
            // stack setting the DFOutNum.
            if (!childItr.hasNext())
            {
                node.DFSNumOut = DFSNumber++;
                worklist.removeLast();
            }
            else
            {
                // otherwise, recursively visit it's children
                DomTreeNodeBase<MachineBasicBlock> child = childItr.next();
                worklist.addLast(new Pair<>(child, child.getChildren().listIterator()));
                child.DFSNumIn = DFSNumber++;
            }
        }

        DFSInfoValid = true;
    }

    /**
     * Traveling the CFG with root node CompoundStmt in depth-first order. And, number
     * every root with an integer.
     * @param root The root block of a CFG.
     * @param N The initialization value of counter.
     * @return
     */
    private int DFS(MachineBasicBlock root, int N)
    {
        // This is more understandable as a recursive algorithm, but we can't use the
        // recursive algorithm due to stack depth issues.  Keep it here for
        // documentation purposes.
        LinkedList<Pair<MachineBasicBlock, ListIterator<MachineBasicBlock>>> worklist =
                new LinkedList<>();

        worklist.add(new Pair<>(root, getSuccsIterator(root)));
        while (!worklist.isEmpty())
        {
            Pair<MachineBasicBlock, ListIterator<MachineBasicBlock>> top = worklist.getLast();
            MachineBasicBlock curr = top.first;
            ListIterator<MachineBasicBlock> succItr = top.second;

            // first visit the current basic block.
            if (succItr.nextIndex() == 0)
            {
                if (!info.containsKey(curr))
                    info.put(curr, new InfoRecord());

                InfoRecord BBInfo = info.get(curr);
                BBInfo.DFSNum = BBInfo.semi = ++N;
                BBInfo.label = curr;

                // Vertex[N] = curr;
                this.Vertex[N] = curr;
            }

            // if all child have been processed, just break down this loop.
            if (!succItr.hasNext())
            {
                worklist.removeLast();
                continue;
            }
            MachineBasicBlock nextSucc = succItr.next();

            if (!info.containsKey(nextSucc))
                info.put(nextSucc, new InfoRecord());
            InfoRecord infoRecord = info.get(nextSucc);
            if (infoRecord.semi == 0)
            {
                infoRecord.parent = N;
                ListIterator<MachineBasicBlock> it = getSuccsIterator(nextSucc);
                if (it.hasNext())
                    worklist.add(new Pair<>(nextSucc, it));
            }
        }

        return N;
    }

    /**
     * Gets the successors or predecessors iterator depend upon if
     * {@code #IsPostDominators} is specified of the current basic block
     * into the work list that will be processed in depth first search.
     * @param curBB The current basic block.
     */
    private ListIterator<MachineBasicBlock> getSuccsIterator(MachineBasicBlock curBB)
    {
        if (IsPostDominators)
        {
            List<MachineBasicBlock> preds = new LinkedList<>();
            Iterator<MachineBasicBlock> itr = curBB.predIterator();
            while(itr.hasNext())
                preds.add(itr.next());
            Collections.reverse(preds);
            return preds.listIterator();
        }
        else
        {
            List<MachineBasicBlock> succs = new LinkedList<>();
            Iterator<MachineBasicBlock> itr = curBB.succIterator();
            while(itr.hasNext())
                succs.add(itr.next());
            Collections.reverse(succs);
            return succs.listIterator();
        }
    }

    private MachineBasicBlock eval(MachineBasicBlock VIn, int lastLinked)
    {
        InfoRecord<MachineBasicBlock> VInInfo = this.info.get(VIn);
        if (VInInfo.DFSNum < lastLinked)
            return VIn;

        LinkedList<MachineBasicBlock> work = new LinkedList<>();
        HashSet<MachineBasicBlock> visited = new HashSet<>(32);

        if (VInInfo.parent >= lastLinked)
            work.add(VIn);

        while (! work.isEmpty())
        {
            MachineBasicBlock V = work.removeLast();
            InfoRecord<MachineBasicBlock> VInfo = this.info.get(V);
            MachineBasicBlock VAncestor = this.Vertex[VInfo.parent];

            // process Ancestor first
            if (visited.add(VAncestor) && VInfo.parent >= lastLinked)
            {
                work.addLast(VAncestor);
                continue;
            }
            //work.removeLast();

            // update VInfo based on the ancestor info
            if (VInfo.parent < lastLinked)
                continue;

            InfoRecord<MachineBasicBlock> VAInfo = info.get(VAncestor);
            MachineBasicBlock VAncestorLabel = VAInfo.label;
            MachineBasicBlock VLabel = VInfo.label;

            if (info.get(VAncestorLabel).semi < info.get(VLabel).semi)
            {
                VInfo.label = VAncestorLabel;
                VInfo.parent = VAInfo.parent;
            }
        }
        return VInInfo.label;
    }

    /**
     * Finds the nearest common dominator block ,if there is no such block return
     * null.
     * @param bb1
     * @param bb2
     * @return
     */
    public MachineBasicBlock findNearestCommonDominator(MachineBasicBlock bb1,
            MachineBasicBlock bb2)
    {
        Util.assertion(!isPostDominators(), "This is not implement for post dominator");
        Util.assertion(bb1.getParent() == bb1.getParent(), "Two blocks are not in the same function");

        // If either bb1 or bb2 is entry, then entry is returned.
        MachineBasicBlock entry = bb1.getParent().getEntryBlock();
        if (bb1 == entry || bb2 == entry)
            return entry;

        // if bb1 dominates bb2, return bb1.
        if (dominates(bb1, bb2))
            return bb1;

        // if bb2 dominates bb1, return bb2.
        if (dominates(bb2, bb1))
            return bb2;

        DomTreeNodeBase<MachineBasicBlock> nodeA = getTreeNodeForBlock(bb1);
        DomTreeNodeBase<MachineBasicBlock> nodeB = getTreeNodeForBlock(bb2);

        // Collects all dominator set of nodeA.
        HashSet<DomTreeNodeBase<MachineBasicBlock>> idomSetA = new HashSet<>();
        idomSetA.add(nodeA);
        DomTreeNodeBase<MachineBasicBlock> idom = nodeA.getIDom();
        while (idom != null)
        {
            idomSetA.add(idom);
            idom = idom.getIDom();
        }

        // Walk nodeB immediate dominators chain and find the common dominator node.
        DomTreeNodeBase<MachineBasicBlock> idomB = nodeB.getIDom();
        while (idomB != null)
        {
            if (idomSetA.contains(idomB))
                return idomB.getBlock();

            idomB = idomB.getIDom();
        }

        return null;
    }

    /**
     * Removes a node from  the dominator tree. Block must not
     * domiante any other blocks. Removes node from its immediate dominator's
     * children list. Deletes dominator node associated with basic block BB.
     * @param bb
     */
    public void eraseNode(MachineBasicBlock bb)
    {
        DomTreeNodeBase<MachineBasicBlock> node = getTreeNodeForBlock(bb);
        Util.assertion(node != null, "Removed node is not in Dominator tree!");
        Util.assertion(node.getChildren().isEmpty(), "Node is not a leaf node!");

        // Remove the node from it's immediate dominator children list.
        DomTreeNodeBase<MachineBasicBlock> idom = node.getIDom();
        if (idom != null)
        {
            Util.assertion(idom.getChildren().contains(node),                     "Not in immediate dominator children list");

            idom.getChildren().remove(node);
        }
        domTreeNodes.remove(bb, node);
        iDoms.remove(bb);
    }
}

