package hir;

import java.util.*;

import utils.Pair;

/**
 * This file defines the DominatorTree class, which provides fast and efficient
 * dominance construction and queries according to Lengauer-Tarjan algorithm.
 *
 * Created by Jianping Zeng  on 2016/2/29.
 */
public class DominatorTree
{
	/**
	 * This class for defining actual dominator tree node.
	 */
	public static class DomTreeNode implements Iterable<DomTreeNode>
	{
		/**
		 * The corresponding basic block of this dominator tree node.
		 */
		private BasicBlock block;

		/**
		 * Immediate dominator.
		 */
		private DomTreeNode IDom;
		/**
		 * All of child node.
		 */
		private ArrayList<DomTreeNode> children;

		private int DFSNumIn = -1;
		private int DFSNumOut = -1;

		public BasicBlock getBlock()
		{
			return block;
		}

		public DomTreeNode getIDom()
		{
			return IDom;
		}

		public void setIDom(DomTreeNode newIDom)
		{
			assert IDom != null : "No immediate dominator";
			if (this.IDom != newIDom)
			{
				assert IDom.children.contains(this)
						: "Not in immediate dominator chidren set";
				// erase this, no longer idom's child
				IDom.children.remove(this);
				this.IDom = newIDom;
				IDom.children.add(this);
			}
		}

		public ArrayList<DomTreeNode> getChildren()
		{
			return children;
		}

		@Override
		public Iterator<DomTreeNode> iterator()
		{
			if (children == null)
				return Collections.<DomTreeNode>emptyIterator();
			else
				return children.iterator();
		}

		public DomTreeNode(BasicBlock bb, DomTreeNode IDom)
		{
			this.block = bb;
			this.IDom = IDom;
		}

		public DomTreeNode addChidren(DomTreeNode C)
		{
			children.add(C);
			return C;
		}

		public final int getNumbChidren() {return children.size();}

		public final void clearChidren() {children.clear();}

		/**
		 * Return true if this node is dominated by other.
		 * Use this only if DFS info is valid.
		 * @param other
		 * @return
		 */
		public boolean dominatedBy(DomTreeNode other)
		{
			return this.DFSNumIn >= other.DFSNumIn &&
					this.DFSNumOut <= other.DFSNumOut;
		}

		public int getDFSNumIn()
		{
			return DFSNumIn;
		}

		public int getDFSNumOut()
		{
			return DFSNumOut;
		}
	}

	/**
	 * Information record used during immediators computation.
	 */
	public class InfoRecord
	{
		int DFSNum = 0;
		int parent = 0;
		int semi = 0;
		BasicBlock label = null;
	}
	/**
	 * The root nodes set of this tree.
	 */
	private ArrayList<BasicBlock> roots;

	/**
	 * Determiates whether its post dominance.
	 */
	private boolean IsPostDominators;

	private HashMap<BasicBlock, DomTreeNode> DomTreeNodes;

	private HashMap<BasicBlock, BasicBlock> IDoms;

	private DomTreeNode rootNode;
	/**
	 * Vertex - Map the DFS number to the BasicBlock(usually block).
	 */
	private BasicBlock[] Vertex;

	/**
	 * Info - Collection of information used during the computation of idoms.
	 */
	private HashMap<BasicBlock, InfoRecord> info;

	private boolean DFSInfoValid = false;

	private int slowQueries = 0;

	private Method m;

	/**
	 * Constructs a instance of creating dominator tree of a CFG.
	 * @param isPostData    whether it is post dominator tree.
	 * @param m The targetAbstractLayer function.
	 */
	public DominatorTree(boolean isPostData, Method m)
	{
		this.IsPostDominators = isPostData;
		this.m = m;
		this.info = new HashMap<>();
	}
	/**
	 * Constructs a instance of creating dominator tree of a CFG.
	 * @param m The targetAbstractLayer function.
	 */
	public DominatorTree(Method m)
	{
		this(false, m);
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
	 * function. If this tree represents the post-dominator relation for a
	 * function, however, this root may be a node with the block == null. This
	 * is teh case when there are multiple exit nodes from a particular function.
	 * @return
	 */
	public DomTreeNode getRootNode()
	{
		return rootNode;
	}

	/**
	 * Gets the corresponding dominator tree node for specifed basic block.
	 * @param bb
	 * @return
	 */
	public DomTreeNode getTreeNodeForBlock(BasicBlock bb)
	{
		return DomTreeNodes.get(bb);
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
	 * @return  Return true iff A dominates B.
	 */
	public boolean dominates(DomTreeNode A, DomTreeNode B)
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

		// If we end up with too many slow queries, just update the
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
		DomTreeNode first = DomTreeNodes.get(A);
		DomTreeNode second = DomTreeNodes.get(B);
		return dominates(first, second);
	}

	private boolean dominateBySlowTreeWalk(DomTreeNode A, DomTreeNode B)
	{
		assert (A != B);
		assert (isReachableFromEntry(B));
		assert (isReachableFromEntry(A));

		DomTreeNode IDom = null;
		while ((IDom = B.getIDom()) != null && IDom != A && IDom != B)
		{
			// wolk up the tree.
			B = IDom;
		}
		return IDom != null;
	}

	/**
	 * Return true if B dominated by A, but A != B.
	 * @param A
	 * @param B
	 * @return
	 */
	public boolean strictDominate(DomTreeNode A, DomTreeNode B)
	{
		if (A == null || B == null)
			return false;
		if (A == B)
			return false;
		return dominates(A, B);
	}

	/**
	 * Determines whether BB is reachable from the entry block of a function.
	 * @param BB
	 * @return
	 */
	public boolean isReachableFromEntry(BasicBlock BB)
	{
		assert isPostDominators() :
				"This is not implemented for post dominatror";
		BasicBlock entry = this.m.getEntryBlock();
		return dominates(entry, BB);
	}

	public boolean isReachableFromEntry(DomTreeNode node)
	{
		assert isPostDominators() :
				"This is not implemented for post dominatror";

		DomTreeNode entry = DomTreeNodes.get(m.getEntryBlock());
		return dominates(entry, node);
	}

	/**
	 * Gets the dominated block of given block.
	 * @param block
	 * @return
	 */
	public BasicBlock getIDom(BasicBlock block)
	{
		DomTreeNode node = DomTreeNodes.get(block);
		if (node == null)
			return null;
		return node.IDom.block;
	}

	/**
	 * Recalculate - compute a dominator tree for the given function.
	 */
	public void recalculate()
	{
		this.Vertex = new BasicBlock[m.cfg.getNumberOfBasicBlocks()];
		if (!IsPostDominators)
		{
			// initialize the root
			BasicBlock entry = m.getEntryBlock();
			this.roots.add(entry);
			this.info.put(entry, new InfoRecord());
			this.IDoms.put(entry, null);
			this.DomTreeNodes.put(entry, null);
			caculate();
		}
		else
		{
			BasicBlock exit = m.getEntryBlock();
			info.put(exit, new InfoRecord());
			roots.add(exit);
			IDoms.put(exit, null);
			DomTreeNodes.put(exit, null);

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
			BasicBlock WBlock = Vertex[i];

			// the InfoRec of current block
			InfoRecord WInfo = info.get(WBlock);

			// Step #2: Implicitly define the immediate dominator of vertices
			for(int j = i; buckets[j] != i; j = buckets[i])
			{
				BasicBlock V = this.Vertex[buckets[j]];
				BasicBlock U = eval(V, i + 1);
				this.IDoms.put(V, this.info.get(U).semi < i ? U :WBlock);
			}

			// Step #3: caculate the semidominators of all vertice

			// initialize the semi dominator to point into it's parent node
			WInfo.semi = WInfo.parent;
			for (BasicBlock BB : WBlock.getSuccs())
			{
				int semiU = info.get(eval(BB, i+1)).semi;
				if (semiU < WInfo.semi)
					WInfo.semi = semiU;
			}

			// If V is a non-root vertex and sdom(V) = parent(V), then idom(V) is
			// necessarily parent(V). In this case, set idom(V) here and avoid placing
			// V into a bucket.
			if (WInfo.semi == WInfo.parent)
				this.IDoms.put(WBlock, Vertex[WInfo.parent]);
			else
			{
				buckets[i] = buckets[WInfo.semi];
				buckets[WInfo.semi] = i;
			}

			if (N >= 1)
			{
				BasicBlock root = Vertex[1];
				for (int j = 1; buckets[j] != 1; j = buckets[j])
				{
					BasicBlock V = Vertex[buckets[j]];
					IDoms.put(V, root);
				}
			}

			// Step #4: Explicitly define the immediate dominator of each vertex
			for (i = 2; i <=N; i++)
			{
				BasicBlock W = Vertex[i];
				BasicBlock WIDom = IDoms.get(W);
				if (WIDom != Vertex[info.get(W).semi])
					WIDom = IDoms.get(WIDom);
			}

			if (getRoots().isEmpty()) return;


			// Add a node for the root.  This node might be the actual root, if there is
			// one exit block, or it may be the virtual exit (denoted by (BasicBlock *)0)
			// which postdominates all real exits if there are multiple exit blocks, or
			// an infinite loop.
			BasicBlock root = !multipleRoots ? roots.get(0) : null;
			this.rootNode = new DomTreeNode(root, null);
			DomTreeNodes.put(root, this.rootNode);

			// loop over all of the reachable blocks in the method.
			for (int idx = 2; idx <=N; idx++)
			{
				BasicBlock W = this.Vertex[idx];
				DomTreeNode BBNode = this.DomTreeNodes.get(W);

				// Haven't caculated this node yet?
				if (BBNode != null) continue;

				BasicBlock ImmDom = this.IDoms.get(W);
				assert (ImmDom != null || this.DomTreeNodes.get(null) != null);

				// Get or calculates the node for the imediate dominator
				DomTreeNode IDomNode = this.getTreeNodeForBlock(ImmDom);

				// add a new tree node for this Basic block, and link it as a child of
				// IDomNode
				DomTreeNode children = new DomTreeNode(W, IDomNode);
				this.DomTreeNodes.put(W, IDomNode.addChidren(children));
			}

			// free temporary memory used to construct idom.
			this.IDoms.clear();
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
		LinkedList<Pair> worklist = new LinkedList<>();

		DomTreeNode thisRoot = getRootNode();
		if (thisRoot == null)
			return;
		
		ListIterator<DomTreeNode> rit = thisRoot.children.listIterator();

		// add a pair of root and predecessor into worklist in the order that
		// last firstly added.
		worklist.addLast(new Pair(thisRoot, rit));		

		// Even in the case of multiple exits that form the post dominator root
		// nodes, do not iterate over all exits, but start from the virtual root
		// node. Otherwise bbs, that are not post dominated by any exit but by the
		// virtual root node, will never be assigned a DFS number.
		thisRoot.DFSNumIn = DFSNumber++;
		while (!worklist.isEmpty())
		{
			Pair top = worklist.getLast();
			DomTreeNode node = (DomTreeNode)top.first;
			ListIterator<DomTreeNode> childItr = (ListIterator<DomTreeNode>)top.second;

			// If we visited all of the children of this node, "recurse" back up the
			// stack setting the DFOutNum.
			if (!childItr.hasNext())
			{
				node.DFSNumOut = DFSNumber++;
				worklist.removeLast();
			}
			else
			{
				// otherwise, recursively visit it's children
				DomTreeNode child = childItr.next();
				worklist.addLast(new Pair(child, child.children.listIterator()));
				child.DFSNumIn = DFSNumber++;
			}
		}

		DFSInfoValid = true;
	}

	/**
	 * Traveling the CFG with root node Block in depth-first order. And, number
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
			BasicBlock nextSucc = succItr.next();

			if (!info.containsKey(nextSucc))
				info.put(nextSucc, new InfoRecord());
			InfoRecord infoRecord = info.get(nextSucc);
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
	 * {@code #IsPostDominators} is specified of the current basic block
	 * into the work list that will be processed in depth first search.
	 * @param curBB The current basic block.
	 */
	private ListIterator<BasicBlock> getSuccsIterator(BasicBlock curBB)
	{
		if (IsPostDominators)
		{
			List<BasicBlock> preds = new LinkedList<>();
			for (BasicBlock pred : curBB.getPreds())
				preds.add(pred);
			Collections.reverse(preds);
			return preds.listIterator();
		}
		else
		{
			List<BasicBlock> succs = new LinkedList<>();
			for (BasicBlock succ : curBB.getSuccs())
				succs.add(succ);
			Collections.reverse(succs);
			return succs.listIterator();
		}
	}

	private BasicBlock eval(BasicBlock VIn, int lastLinked)
	{
		InfoRecord VInInfo = this.info.get(VIn);
		if (VInInfo.DFSNum < lastLinked)
			return VIn;

		LinkedList<BasicBlock> work = new LinkedList<>();
		HashSet<BasicBlock> visited = new HashSet<>(32);

		if (VInInfo.parent >= lastLinked)
			work.add(VIn);

		while (! work.isEmpty())
		{
			BasicBlock V = work.removeLast();
			InfoRecord VInfo = this.info.get(V);
			BasicBlock VAncestor = this.Vertex[VInfo.parent];

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

			InfoRecord VAInfo = this.info.get(VAncestor);
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
}
