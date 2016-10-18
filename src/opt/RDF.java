package opt;

import hir.BasicBlock;
import hir.DominatorTree;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;

import utils.Pair;

/**
 * This file defines a helpful method for obtains the reverse dominator frontier
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class RDF
{

	private static HashMap<DominatorTree.DomTreeNode, Integer> DomLevels
			= new HashMap<>();
	/**
	 * This method used to initialize a map that maps dominator tree node
	 * into it's depth level in the Dominator tree.
	 */
	private static void init(DominatorTree DT)
	{
		if (DomLevels.isEmpty())
		{
			LinkedList<DominatorTree.DomTreeNode> worklist
					= new LinkedList<>();
			DominatorTree.DomTreeNode root = DT.getRootNode();
			worklist.addLast(root);
			DomLevels.put(root, 0);
			while (!worklist.isEmpty())
			{
				DominatorTree.DomTreeNode pop = worklist.removeLast();
				int N = DomLevels.get(pop);

				for (DominatorTree.DomTreeNode child : pop.getChildren())
				{
					DomLevels.put(child, N + 1);
					worklist.addLast(child);
				}
			}
		}
	}

	/**
	 * Gets the reverse dominator frontier set of given basic block BB.
	 * @param entry    The basic block computed reverse dominator frontier.
	 * @return  The reverse dominator frontier set.
	 */
	public static LinkedList<BasicBlock> run(DominatorTree DT, BasicBlock entry)
	{
		LinkedList<BasicBlock> rdf = new LinkedList<>();

		init(DT);

		// 使用一个优先级队列，按照在支配树中的层次，越深的结点放在前面
		PriorityQueue<Pair<DominatorTree.DomTreeNode, Integer>> PQ
				= new PriorityQueue<>(32,
				new Comparator<Pair<DominatorTree.DomTreeNode, Integer>>()
				{
					@Override
					public int compare(Pair<DominatorTree.DomTreeNode, Integer>
							o1, Pair<DominatorTree.DomTreeNode, Integer> o2)
					{
						return -1;
					}
				});

		DominatorTree.DomTreeNode root = DT.getTreeNodeForBlock(entry);
		PQ.add(new Pair<>(root, DomLevels.get(root)));

		LinkedList<DominatorTree.DomTreeNode> worklist = new LinkedList<>();
		HashSet<DominatorTree.DomTreeNode> visited = new HashSet<>(32);

		// 从在支配树中最底层的定义块开始向上一个一个的遍历，
		// 在每个基本块的支配边界中放入Phi结点。
		while (!PQ.isEmpty())
		{
			Pair<DominatorTree.DomTreeNode, Integer> rootPair = PQ.poll();
			DominatorTree.DomTreeNode rootNode = rootPair.first;
			int rootLevel = rootPair.second;

			worklist.clear();
			worklist.addLast(rootNode);

			while (!worklist.isEmpty())
			{
				DominatorTree.DomTreeNode Node = worklist.removeLast();
				BasicBlock curr = Node.getBlock();

				for (BasicBlock succ : curr.getSuccs())
				{
					DominatorTree.DomTreeNode succNode =
							DT.getTreeNodeForBlock(succ);

					// 跳过所有succ块所支配的的块
					if (succNode.getIDom() == Node)
						continue;

					// skips those dominator tree nodes whose depth level
					// is greater than root's level.
					int succLevel = DomLevels.get(succNode);
					if (succLevel > rootLevel)
						continue;

					// skip the visisted dom tree node
					if (!visited.add(succNode))
						continue;

					rdf.add(succ);
				}// end for successor

				for (DominatorTree.DomTreeNode domNode : Node)
					if (!visited.contains(domNode))
						worklist.addLast(domNode);
			}
		}
		return rdf;
	}
}
