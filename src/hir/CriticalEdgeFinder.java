package hir;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class finds and splits "critical" edges in the control flow graph.
 * An edge between two blocks {@code A} and {@code B} is "critical" if {@code A}
 * has more than one successor and {@code B} has more than one predecessor. Such
 * edges are split by adding a block between the two blocks.
 * <br>
 * Created by Jianping Zeng  on 2016/3/17.
 */
public class CriticalEdgeFinder
{

	private final ControlFlowGraph cfg;

	/**
	 * The graph edges represented as a map from source to targetAbstractLayer nodes.
	 * Using a linked hash map makes compilation tracing more deterministic
	 * and thus eases debugging.
	 */
	private Map<BasicBlock, Set<BasicBlock>> edges =
			new HashMap<BasicBlock, Set<BasicBlock>>();

	public CriticalEdgeFinder(ControlFlowGraph cfg)
	{
		this.cfg = cfg;
	}

	public void apply(BasicBlock block)
	{
		if (block.getNumOfSuccs() >= 2)
		{
			for (BasicBlock succ : block.getSuccs())
			{
				if (succ.getNumOfPreds() >= 2)
				{
					// Probably we don't have to make it a critical
					// edge if succ only isDeclScope the _same_ predecessor multiple times.

					// That is not needed to take Condition above described into
					// consideration, due to useless control flow elimination.
					recordCriticalEdge(block, succ);
				}
			}
		}
	}

	private void recordCriticalEdge(BasicBlock block, BasicBlock succ)
	{
		if (!edges.containsKey(block))
		{
			edges.put(block, new HashSet<BasicBlock>());
		}

		edges.get(block).add(succ);
	}

	public void splitCriticalEdges()
	{
		for (BasicBlock from : edges.keySet())
		{
			for (BasicBlock to : edges.get(from))
			{
				BasicBlock split = cfg.splitEdge(from, to);

				StringBuilder sb = new StringBuilder();
				sb.append("Split edge between block ");
				sb.append(from.getID());
				sb.append(" and block ");
				sb.append(to.getID());
				sb.append(", creating new block ");
				sb.append(split.getID());

				System.out.println(sb.toString());
			}
		}
	}
}

