package backend.opt;

import backend.analysis.DomTree;
import backend.hir.BasicBlock;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;

import tools.Pair;

/**
 * This file defines a helpful method for obtains the reverse dominator frontier
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class RDF
{

	private static HashMap<DomTree.DomTreeNode, Integer> DomLevels
			= new HashMap<>();
	/**
	 * This method used to initialize a map that maps dominator tree node
	 * into it's depth level in the Dominator tree.
	 */
	private static void init(DomTree DT)
	{
		if (DomLevels.isEmpty())
		{
			LinkedList<DomTree.DomTreeNode> worklist
					= new LinkedList<>();
			DomTree.DomTreeNode root = DT.getRootNode();
			worklist.addLast(root);
			DomLevels.put(root, 0);
			while (!worklist.isEmpty())
			{
				DomTree.DomTreeNode pop = worklist.removeLast();
				int N = DomLevels.get(pop);

				for (DomTree.DomTreeNode child : pop.getChildren())
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
	public static LinkedList<BasicBlock> run(DomTree DT, BasicBlock entry)
	{
		LinkedList<BasicBlock> rdf = new LinkedList<>();

		init(DT);

		// 使用一个优先级队列，按照在支配树中的层次，越深的结点放在前面
		PriorityQueue<Pair<DomTree.DomTreeNode, Integer>> PQ
				= new PriorityQueue<>(32,
				new Comparator<Pair<DomTree.DomTreeNode, Integer>>()
				{
					@Override
					public int compare(Pair<DomTree.DomTreeNode, Integer>
							o1, Pair<DomTree.DomTreeNode, Integer> o2)
					{
						return -1;
					}
				});

		DomTree.DomTreeNode root = DT.getTreeNodeForBlock(entry);
		PQ.add(new Pair<>(root, DomLevels.get(root)));

		LinkedList<DomTree.DomTreeNode> worklist = new LinkedList<>();
		HashSet<DomTree.DomTreeNode> visited = new HashSet<>(32);

		// 从在支配树中最底层的定义块开始向上一个一个的遍历，
		// 在每个基本块的支配边界中放入Phi结点。
		while (!PQ.isEmpty())
		{
			Pair<DomTree.DomTreeNode, Integer> rootPair = PQ.poll();
			DomTree.DomTreeNode rootNode = rootPair.first;
			int rootLevel = rootPair.second;

			worklist.clear();
			worklist.addLast(rootNode);

			while (!worklist.isEmpty())
			{
				DomTree.DomTreeNode Node = worklist.removeLast();
				BasicBlock curr = Node.getBlock();

				for (BasicBlock succ : curr.getSuccs())
				{
					DomTree.DomTreeNode succNode =
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

				for (DomTree.DomTreeNode domNode : Node)
					if (!visited.contains(domNode))
						worklist.addLast(domNode);
			}
		}
		return rdf;
	}
}
