package backend.support;

import backend.value.BasicBlock;
import backend.utils.PredIterator;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class is an extremely trivial cache implementation for predecessor
 * iterator quires. This is very useful for the code that repeatedly wants
 * the predecessor list for the same blocks.
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public final class PredIteratorCache
{
	private HashMap<BasicBlock, List<BasicBlock>> blockToPredsMap;
	private TObjectIntHashMap<BasicBlock> blockToPredsNumMap;

	/**
	 * Gets the cached predecessors list for the given basic block {@code parent}.
	 * @param bb
	 * @return
	 */
	public List<BasicBlock> getPreds(BasicBlock bb)
	{
		List<BasicBlock> preds;
		if (!blockToPredsMap.containsKey(bb))
		{
			preds = new ArrayList<>();
			for (PredIterator<BasicBlock> itr = bb.predIterator(); itr.hasNext();)
				preds.add(itr.next());
			blockToPredsMap.put(bb, preds);
		}
		else
			preds = blockToPredsMap.get(bb);
		blockToPredsNumMap.put(bb, preds.size() - 1);
		return preds;
	}

	public int getNumPreds(BasicBlock bb)
	{
		getPreds(bb);
		return blockToPredsNumMap.get(bb);
	}

	public void clear()
	{
		blockToPredsNumMap.clear();
		blockToPredsMap.clear();
	}
}
