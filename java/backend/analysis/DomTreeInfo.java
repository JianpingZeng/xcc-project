package backend.analysis;

import tools.Util;
import backend.value.BasicBlock;
import backend.utils.PredIterator;
import backend.utils.SuccIterator;
import backend.value.Function;
import tools.Pair;

import java.util.*;

/**
 * This file defines the DomTree class, which provides fast and efficient
 * dominance construction and queries according to Lengauer-Tarjan algorithm.
 *
 * Created by Jianping Zeng  on 2016/2/29.
 */
public final class DomTreeInfo implements IDomTreeInfo
{
	/**
	 * The root nodes set of this tree.
	 */
	private ArrayList<BasicBlock> roots;

	/**
	 * Determiates whether its post dominance.
	 */
	private boolean isPostDominators;

	private HashMap<BasicBlock, DomTreeNodeBase<BasicBlock>> domTreeNodes;

	private HashMap<BasicBlock, BasicBlock> iDoms;

	private DomTreeNodeBase<BasicBlock> rootNode;
	/**
	 * vertex - Map the DFS number to the BasicBlock(usually block).
	 */
	private ArrayList<BasicBlock> vertex;

	/**
	 * Info - Collection of information used during the computation of iDoms.
	 */
	private HashMap<BasicBlock, InfoRecord<BasicBlock>> info;

	private boolean DFSInfoValid = false;

	private int slowQueries = 0;

	private Function m;

	/**
	 * Constructs a instance of creating dominator tree of a CFG.
	 * @param isPostData    whether it is post dominator tree.
	 */
	public DomTreeInfo(boolean isPostData)
	{
		roots = new ArrayList<>();
		isPostDominators = isPostData;
		domTreeNodes = new HashMap<>();
		iDoms = new HashMap<>();
		info = new HashMap<>();
	}
	/**
	 * Constructs a instance of creating dominator tree of a CFG.
	 */
	public DomTreeInfo()
	{
		this(false);
	}

	/**
	 * Returns the root blocks of current CFG. This may include multiple blocks
	 * if we are computing post dominators. For forward dominators, this wil always
	 * be a single block (the entry block of CFG).
	 * @return
	 */
	public ArrayList<BasicBlock> getRoots()
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
	public DomTreeNodeBase<BasicBlock> getRootNode()
	{
		return rootNode;
	}

	/**
	 * Gets the corresponding dominator tree node for specifed basic block.
	 * @param bb
	 * @return
	 */
	public DomTreeNodeBase<BasicBlock> getTreeNodeForBlock(BasicBlock bb)
	{
		return domTreeNodes.get(bb);
	}

	/**
	 * Returns true if analysis based on postdoms.
	 * @return
	 */
	public boolean isPostDominators(){return isPostDominators;}

	/**
	 * Determine whether A dominates B.
	 * @param A
	 * @param B
	 * @return  ReturnInst true iff A dominates B.
	 */
	public boolean dominates(DomTreeNodeBase<BasicBlock> A, DomTreeNodeBase<BasicBlock> B)
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

	public boolean dominates(BasicBlock A, BasicBlock B)
	{
		DomTreeNodeBase<BasicBlock> first = domTreeNodes.get(A);
		DomTreeNodeBase<BasicBlock> second = domTreeNodes.get(B);
		return dominates(first, second);
	}

	private boolean dominateBySlowTreeWalk(DomTreeNodeBase<BasicBlock> A, DomTreeNodeBase<BasicBlock> B)
	{
		Util.assertion( (A != B));
		Util.assertion( (isReachableFromEntry(B)));
		Util.assertion( (isReachableFromEntry(A)));

		DomTreeNodeBase<BasicBlock> IDom = null;
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
	public boolean strictDominate(DomTreeNodeBase<BasicBlock> A, DomTreeNodeBase<BasicBlock> B)
	{
		if (A == null || B == null)
			return false;
		if (A == B)
			return false;
		return dominates(A, B);
	}

	public boolean strictDominate(BasicBlock a, BasicBlock b)
    {
        return strictDominate(getTreeNodeForBlock(b), getTreeNodeForBlock(b));
    }

	/**
	 * Determines whether BB is reachable from the entry block of a function.
	 * @param BB
	 * @return
	 */
	public boolean isReachableFromEntry(BasicBlock BB)
	{
		Util.assertion(isPostDominators(), 				"This is not implemented for post dominatror");

		BasicBlock entry = this.m.getEntryBlock();
		return dominates(entry, BB);
	}

	public boolean isReachableFromEntry(DomTreeNodeBase<BasicBlock> node)
	{
		Util.assertion(isPostDominators(), 				"This is not implemented for post dominatror");


		DomTreeNodeBase<BasicBlock> entry = domTreeNodes.get(m.getEntryBlock());
		return dominates(entry, node);
	}

	/**
	 * Gets the dominated block of given block.
	 * @param block
	 * @return
	 */
	public BasicBlock getIDom(BasicBlock block)
	{
		DomTreeNodeBase<BasicBlock> node = domTreeNodes.get(block);
		if (node == null)
			return null;
		return node.getIDom().getBlock();
	}

	/**
	 * Recalculate - compute a dominator tree for the given function.
	 */
	public void recalculate(Function f)
	{
	    m = f;
		// FIXME: 17-7-1
		vertex = new ArrayList<>();
		if (!isPostDominators)
		{
			// initialize the root
			BasicBlock entry = m.getEntryBlock();
			this.roots.add(entry);
			this.info.put(entry, new InfoRecord<BasicBlock>());
			this.iDoms.put(entry, null);
			this.domTreeNodes.put(entry, null);
			caculate();
		}
		else
		{
			BasicBlock exit = m.getEntryBlock();
			info.put(exit, new InfoRecord<BasicBlock>());
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
			InfoRecord<BasicBlock> BBInfo = info.get(null);
			BBInfo.DFSNum = ++N;
			BBInfo.label = null;
		}

		// Step #1: Number blocks in depth-first order and initialize variables
		// used in later stages of the algorithm.
		for (BasicBlock block : roots)
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
			BasicBlock WBlock = vertex.get(i);

			// the InfoRec of current block
			InfoRecord<BasicBlock> WInfo = info.get(WBlock);

			// Step #2: Implicitly define the immediate dominator of vertices
			for(int j = i; buckets[j] != i; j = buckets[i])
			{
				BasicBlock V = this.vertex.get(buckets[j]);
				BasicBlock U = eval(V, i + 1);
				this.iDoms.put(V, this.info.get(U).semi < i ? U :WBlock);
			}

			// Step #3: caculate the semidominators of all vertice

			// initialize the semi dominator to point into it's parent node
			WInfo.semi = WInfo.parent;
			SuccIterator succItr = WBlock.succIterator();
			while(succItr.hasNext())
			{
			    BasicBlock BB = succItr.next();
				int semiU = info.get(eval(BB, i+1)).semi;
				if (semiU < WInfo.semi)
					WInfo.semi = semiU;
			}

			// IfStmt V is a non-root vertex and sdom(V) = parent(V), then idom(V) is
			// necessarily parent(V). In this case, set idom(V) here and avoid placing
			// V into a bucket.
			if (WInfo.semi == WInfo.parent)
				this.iDoms.put(WBlock, vertex.get(WInfo.parent));
			else
			{
				buckets[i] = buckets[WInfo.semi];
				buckets[WInfo.semi] = i;
			}

			if (N >= 1)
			{
				BasicBlock root = vertex.get(1);
				for (int j = 1; buckets[j] != 1; j = buckets[j])
				{
					BasicBlock V = vertex.get(buckets[j]);
					iDoms.put(V, root);
				}
			}

			// Step #4: Explicitly define the immediate dominator of each vertex
			for (i = 2; i <=N; i++)
			{
				BasicBlock W = vertex.get(i);
				BasicBlock WIDom = iDoms.get(W);
				if (WIDom != vertex.get(info.get(W).semi))
					WIDom = iDoms.get(WIDom);
			}

			if (getRoots().isEmpty()) return;


			// Add a node for the root.  This node might be the actual root, if there is
			// one exit block, or it may be the virtual exit (denoted by (BasicBlock *)0)
			// which postdominates all real exits if there are multiple exit blocks, or
			// an infinite loop.
			BasicBlock root = !multipleRoots ? roots.get(0) : null;
			this.rootNode = new DomTreeNodeBase<BasicBlock>(root, null);
			domTreeNodes.put(root, this.rootNode);

			// loop over all of the reachable blocks in the method.
			for (int idx = 2; idx <=N; idx++)
			{
				BasicBlock W = this.vertex.get(idx);
				DomTreeNodeBase<BasicBlock> BBNode = this.domTreeNodes.get(W);

				// Haven't caculated this node yet?
				if (BBNode != null) continue;

				BasicBlock ImmDom = this.iDoms.get(W);
				Util.assertion( (ImmDom != null || this.domTreeNodes.get(null) != null));

				// Get or calculates the node for the imediate dominator
				DomTreeNodeBase<BasicBlock> IDomNode = this.getTreeNodeForBlock(ImmDom);

				// add a new tree node for this Basic block, and link it as a child of
				// IDomNode
				DomTreeNodeBase<BasicBlock> children = new DomTreeNodeBase<BasicBlock>(W, IDomNode);
				this.domTreeNodes.put(W, IDomNode.addChidren(children));
			}

			// free temporary memory used to construct idom.
			this.iDoms.clear();
			this.info.clear();
			this.vertex = null;
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
		LinkedList<Pair<DomTreeNodeBase<BasicBlock>,
				ListIterator<DomTreeNodeBase<BasicBlock>>>> worklist = new LinkedList<>();

		DomTreeNodeBase<BasicBlock> thisRoot = getRootNode();
		if (thisRoot == null)
			return;
		
		ListIterator<DomTreeNodeBase<BasicBlock>> rit = thisRoot.getChildren().listIterator();

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
			Pair<DomTreeNodeBase<BasicBlock>,
					ListIterator<DomTreeNodeBase<BasicBlock>>> top = worklist.getLast();
			DomTreeNodeBase<BasicBlock> node = top.first;
			ListIterator<DomTreeNodeBase<BasicBlock>> childItr = top.second;

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
				DomTreeNodeBase<BasicBlock> child = childItr.next();
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
	private int DFS(BasicBlock root, int N)
	{
		// This is more understandable as a recursive algorithm, but we can't use the
		// recursive algorithm due to stack depth issues.  Keep it here for
		// documentation purposes.
		LinkedList<Pair<BasicBlock, ListIterator<BasicBlock>>> worklist =
				new LinkedList<>();

		worklist.add(new Pair<>(root, getSuccsIterator(root)));
		while (!worklist.isEmpty())
		{
			Pair<BasicBlock, ListIterator<BasicBlock>> top = worklist.getLast();
			BasicBlock curr = top.first;
			ListIterator<BasicBlock> succItr = top.second;

			// first visit the current basic block.
			if (succItr.nextIndex() == 0)
			{
				if (!info.containsKey(curr))
					info.put(curr, new InfoRecord<BasicBlock>());

				InfoRecord<BasicBlock> BBInfo = info.get(curr);
				BBInfo.DFSNum = BBInfo.semi = ++N;
				BBInfo.label = curr;

				// vertex[N] = curr;
				vertex.add(curr);
			}

			// if all child have been processed, just break down this loop.
			if (!succItr.hasNext())
			{
				worklist.removeLast();
				continue;
			}
			BasicBlock nextSucc = succItr.next();

			if (!info.containsKey(nextSucc))
				info.put(nextSucc, new InfoRecord<BasicBlock>());
			InfoRecord<BasicBlock> infoRecord = info.get(nextSucc);
			if (infoRecord.semi == 0)
			{
				infoRecord.parent = N;
				ListIterator<BasicBlock> it = getSuccsIterator(nextSucc);
				if (it.hasNext())
					worklist.add(new Pair<>(nextSucc, it));
			}
		}

		return N;
	}

	/**
	 * Gets the successors or predecessors iterator depend upon if
	 * {@code #isPostDominators} is specified of the current basic block
	 * into the work list that will be processed in depth first search.
	 * @param curBB The current basic block.
	 */
	private ListIterator<BasicBlock> getSuccsIterator(BasicBlock curBB)
	{
		if (isPostDominators)
		{
			List<BasicBlock> preds = new LinkedList<>();
            PredIterator<BasicBlock> itr = curBB.predIterator();
			while(itr.hasNext())
				preds.add(itr.next());
			Collections.reverse(preds);
			return preds.listIterator();
		}
		else
		{
			List<BasicBlock> succs = new LinkedList<>();
            SuccIterator itr = curBB.succIterator();
			while(itr.hasNext())
				succs.add(itr.next());
			Collections.reverse(succs);
			return succs.listIterator();
		}
	}

	private BasicBlock eval(BasicBlock VIn, int lastLinked)
	{
		InfoRecord<BasicBlock> VInInfo = this.info.get(VIn);
		if (VInInfo.DFSNum < lastLinked)
			return VIn;

		LinkedList<BasicBlock> work = new LinkedList<>();
		HashSet<BasicBlock> visited = new HashSet<>(32);

		if (VInInfo.parent >= lastLinked)
			work.add(VIn);

		while (! work.isEmpty())
		{
			BasicBlock V = work.removeLast();
			InfoRecord<BasicBlock> VInfo = this.info.get(V);
			BasicBlock VAncestor = this.vertex.get(VInfo.parent);

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

			InfoRecord<BasicBlock> VAInfo = this.info.get(VAncestor);
			BasicBlock VAncestorLabel = VAInfo.label;
			BasicBlock VLabel = VInfo.label;

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
    public BasicBlock findNearestCommonDominator(BasicBlock bb1,
            BasicBlock bb2)
    {
        Util.assertion(!isPostDominators(), "This is not implement for post dominator");
        Util.assertion(bb1.getParent() == bb1.getParent(), "Two blocks are not in the same function");

        // If either bb1 or bb2 is entry, then entry is returned.
        BasicBlock entry = bb1.getParent().getEntryBlock();
        if (bb1 == entry || bb2 == entry)
            return entry;

        // if bb1 dominates bb2, return bb1.
        if (dominates(bb1, bb2))
            return bb1;

        // if bb2 dominates bb1, return bb2.
        if (dominates(bb2, bb1))
            return bb2;

        DomTreeNodeBase<BasicBlock> nodeA = getTreeNodeForBlock(bb1);
        DomTreeNodeBase<BasicBlock> nodeB = getTreeNodeForBlock(bb2);

        // Collects all dominator set of nodeA.
        HashSet<DomTreeNodeBase<BasicBlock>> idomSetA = new HashSet<>();
        idomSetA.add(nodeA);
        DomTreeNodeBase<BasicBlock> idom = nodeA.getIDom();
        while (idom != null)
        {
            idomSetA.add(idom);
            idom = idom.getIDom();
        }

        // Walk nodeB immediate dominators chain and find the common dominator node.
        DomTreeNodeBase<BasicBlock> idomB = nodeB.getIDom();
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
    public void eraseNode(BasicBlock bb)
    {
        DomTreeNodeBase<BasicBlock> node = getTreeNodeForBlock(bb);
        Util.assertion(node != null, "Removed node is not in Dominator tree!");
        Util.assertion(node.getChildren().isEmpty(), "Node is not a leaf node!");

        // Remove the node from it's immediate dominator children list.
        DomTreeNodeBase<BasicBlock> idom = node.getIDom();
        if (idom != null)
        {
            Util.assertion(idom.getChildren().contains(node),                     "Not in immediate dominator children list");

            idom.getChildren().remove(node);
        }
        domTreeNodes.remove(bb, node);
        iDoms.remove(bb);
    }

	/**
	 * newBB is split and now it has one successor.
	 * Update the dominator tree to reflect this effect.
	 * @param newBB
	 */
    public void splitBlock(BasicBlock newBB)
    {
		if (isPostDominators)
		{
			ArrayList<BasicBlock> preds = new ArrayList<>();
			for (PredIterator<BasicBlock> itr = newBB.predIterator();itr.hasNext();)
				preds.add(itr.next());
			splitPostDom(preds, newBB);
		}
		else
		{
			ArrayList<BasicBlock> succs = new ArrayList<>();
			for (SuccIterator itr = newBB.succIterator();itr.hasNext();)
				succs.add(itr.next());
			splitDom(succs, newBB);
		}
    }

    private void splitDom(ArrayList<BasicBlock> succs, BasicBlock newBB)
    {
		Util.assertion(succs.size() == 1, "newBB must have a single successor");

		BasicBlock succ = succs.get(0);

		ArrayList<BasicBlock> preds = new ArrayList<>();
		for (PredIterator<BasicBlock> itr = newBB.predIterator(); itr.hasNext();)
			preds.add(itr.next());

		Util.assertion(!preds.isEmpty(), "No predecessors block!");

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

    private void splitPostDom(ArrayList<BasicBlock> preds, BasicBlock newBB)
    {
	    Util.assertion(preds.size() == 1, "newBB must have a single predecessor");

	    BasicBlock pred = preds.get(0);

	    ArrayList<BasicBlock> succs = new ArrayList<>();
	    for (SuccIterator itr = newBB.succIterator(); itr.hasNext();)
		    succs.add(itr.next());

	    Util.assertion(!succs.isEmpty(), "No successors block!");

	    boolean newBBDominatesSucc = true;
	    for (SuccIterator itr = pred.succIterator(); itr.hasNext();)
	    {
		    BasicBlock succ = itr.next();
		    if (succ != newBB && !dominates(pred, succ)
				    && isReachableFromEntry(succ))
		    {
			    newBBDominatesSucc = false;
			    break;
		    }
	    }

	    // Find newBB's immediate dominator and create new dominator tree node
	    // for newBB.
	    BasicBlock newBBIDom = null;
	    int i = 0;
	    for (; i < succs.size(); i++)
	    {
		    if (isReachableFromEntry(succs.get(i)))
		    {
			    newBBIDom = succs.get(i);
			    break;
		    }
	    }

	    // It's possible that none of the predecessors of NewBB are reachable;
	    // in that case, NewBB itself is unreachable, so nothing needs to be
	    // changed.
	    if (newBBIDom == null)
		    return;

	    for (i += 1; i < succs.size(); i++)
	    {
		    if (isReachableFromEntry(succs.get(i)))
			    newBBIDom = findNearestCommonDominator(newBB, succs.get(i));
	    }

	    // Create a new dominator tree node, and set it as the idom of newBB.
	    DomTreeNodeBase<BasicBlock> newBBNode = addNewBlock(newBB, newBBIDom);

	    // If newBB strictly dominates other blocks, then it is now the immediate
	    // dominator of cucc.  Update the dominator tree as appropriate.
	    if (newBBDominatesSucc)
	    {
		    DomTreeNodeBase<BasicBlock> newBBSuccNode = getTreeNodeForBlock(pred);
		    changeIDom(newBBSuccNode, newBBNode);
	    }
    }

	/**
	 * Add a new node to the dominator tree information.  This
	 * creates a new node as a child of DomBB dominator node,linking it into
	 * the children list of the immediate dominator.
	 * @param bb
	 * @param idom
	 * @return
	 */
	public DomTreeNodeBase<BasicBlock> addNewBlock(BasicBlock bb, BasicBlock idom)
    {
		Util.assertion(getTreeNodeForBlock(bb) == null, "Block already in dominator tree");
		DomTreeNodeBase<BasicBlock> idomNode = getTreeNodeForBlock(idom);
		Util.assertion(idomNode != null, "Not immediate dominator specified for block!");
		return domTreeNodes.put(bb, idomNode.addChidren(new DomTreeNodeBase<>(bb, idomNode)));
    }

	/**
	 * Updates the dominator tree information when immediate dominator node changes.
	 * @param oldIDom
	 * @param newIDom
	 */
    public void changeIDom(DomTreeNodeBase<BasicBlock> oldIDom,
		    DomTreeNodeBase<BasicBlock> newIDom)
    {
		Util.assertion(oldIDom != null && newIDom != null, "Cannot change null node");
		oldIDom.setIDom(newIDom);
    }

	/**
	 * Updates the dominator tree information when immediate dominator node changes.
	 * @param oldIDomBB
	 * @param newIDomBB
	 */
	public void changeIDom(BasicBlock oldIDomBB, BasicBlock newIDomBB)
    {
		changeIDom(getTreeNodeForBlock(oldIDomBB), getTreeNodeForBlock(newIDomBB));
    }
}
