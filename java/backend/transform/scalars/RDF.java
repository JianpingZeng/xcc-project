package backend.transform.scalars;

import backend.analysis.DomTree;
import backend.analysis.DomTreeNodeBase;
import backend.value.BasicBlock;
import backend.utils.SuccIterator;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Pair;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;

/**
 * This file defines a helpful method for obtains the reverse dominator frontier
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public final class RDF
{
	private static TObjectIntHashMap<DomTreeNodeBase<BasicBlock>> domLevels
			= new TObjectIntHashMap<>();
	/**
	 * This method used to initialize a map that maps dominator tree node
	 * into it's depth level in the Dominator tree.
	 */
	private static void init(DomTree dt)
	{
		if (domLevels.isEmpty())
		{
			LinkedList<DomTreeNodeBase<BasicBlock>> worklist
					= new LinkedList<>();
			DomTreeNodeBase<BasicBlock> root = dt.getRootNode();
			worklist.addLast(root);
			domLevels.put(root, 0);
			while (!worklist.isEmpty())
			{
				DomTreeNodeBase<BasicBlock> pop = worklist.removeLast();
				int N = domLevels.get(pop);

				for (DomTreeNodeBase<BasicBlock> child : pop.getChildren())
				{
					domLevels.put(child, N + 1);
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
	public static LinkedList<BasicBlock> run(DomTree dt, BasicBlock entry)
	{
		LinkedList<BasicBlock> rdf = new LinkedList<>();

		init(dt);

		// 使用一个优先级队列，按照在支配树中的层次，越深的结点放在前面
		PriorityQueue<Pair<DomTreeNodeBase<BasicBlock>, Integer>> PQ
				= new PriorityQueue<>(32,
				new Comparator<Pair<DomTreeNodeBase<BasicBlock>, Integer>>()
				{
					@Override
					public int compare(Pair<DomTreeNodeBase<BasicBlock>, Integer>
							o1, Pair<DomTreeNodeBase<BasicBlock>, Integer> o2)
					{
						return -1;
					}
				});

		DomTreeNodeBase<BasicBlock> root = dt.getTreeNodeForBlock(entry);
		PQ.add(new Pair<>(root, domLevels.get(root)));

		LinkedList<DomTreeNodeBase<BasicBlock>> worklist = new LinkedList<>();
		HashSet<DomTreeNodeBase<BasicBlock>> visited = new HashSet<>(32);

		// 从在支配树中最底层的定义块开始向上一个一个的遍历，
		// 在每个基本块的支配边界中放入Phi结点。
		while (!PQ.isEmpty())
		{
			Pair<DomTreeNodeBase<BasicBlock>, Integer> rootPair = PQ.poll();
			DomTreeNodeBase<BasicBlock> rootNode = rootPair.first;
			int rootLevel = rootPair.second;

			worklist.clear();
			worklist.addLast(rootNode);

			while (!worklist.isEmpty())
			{
				DomTreeNodeBase<BasicBlock> Node = worklist.removeLast();
				BasicBlock curr = Node.getBlock();

				for (SuccIterator itr = curr.succIterator(); itr.hasNext();)
				{
					BasicBlock succ = itr.next();
					DomTreeNodeBase<BasicBlock> succNode =
							dt.getTreeNodeForBlock(succ);

					// 跳过所有succ块所支配的的块
					if (succNode.getIDom() == Node)
						continue;

					// skips those dominator tree nodes whose depth level
					// is greater than root's level.
					int succLevel = domLevels.get(succNode);
					if (succLevel > rootLevel)
						continue;

					// skip the visisted dom tree node
					if (!visited.add(succNode))
						continue;

					rdf.add(succ);
				}// end for successor

				for (DomTreeNodeBase<BasicBlock> domNode : Node)
					if (!visited.contains(domNode))
						worklist.addLast(domNode);
			}
		}
		return rdf;
	}
}
