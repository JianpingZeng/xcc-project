package opt;

import hir.*;
import hir.BasicBlock;

import java.util.List;
import java.util.HashMap;

/**
 * An internal class for global value numbering which is desired when performing
 * global common subexpression elimination.
 * 
 * @author xlous.zeng
 * @version 0.1
 */
public class GVN
{
	private final Function m;

	private final HashMap<BasicBlock, ValueMap> valueMaps;

	private ValueMap currentMap;

	private final DominatorTree DT;

	/**
	 * Creates a new Global value numbering with given method.
	 * @param m The method whose CFG where global value number optimized.
	 */
	public GVN(Function m)
	{
		this.m = m;
		DT = new DominatorTree(false, m);
		DT.recalculate();

		List<BasicBlock> blocks = m.linearScanOrder();
		this.valueMaps = new HashMap<>(blocks.size());
		currentMap = null;
		optimize(blocks);
	}

	/**
	 * Performs opt.
	 * @param blocks    A list of all basic block in linear scan order.
	 */
	private void optimize(List<BasicBlock> blocks)
	{
		if (blocks.isEmpty()) return;
		BasicBlock startBlock = blocks.get(0);

		assert startBlock == m.getEntryBlock() &&
				startBlock.getNumOfPreds() == 0 &&
				DT.getIDom(startBlock) == null:
				"None start block of CFG.";

		valueMaps.put(startBlock, new ValueMap());

		int numBlocks = blocks.size();
		// iterate over all blocks
		for (int idx = 1; idx < numBlocks; ++idx)
		{
			BasicBlock block = blocks.get(idx);

			int numPreds = block.getNumOfPreds();
			assert numPreds > 0 : "CompoundStmt must have at least one predecessor";

			BasicBlock dominator = DT.getIDom(block);
			assert dominator != null : "Dominator must exists";
			assert valueMaps.get(dominator) != null :
					"Value map of dominator must exists";

			// creates a new value map with parent map
			currentMap = new ValueMap(valueMaps.get(dominator));
			assert block.getPreds().contains(dominator) :
					"The predecssors list must isDeclScope dominator";

			// visit all instructions in current block
			for (hir.Value inst : block)
			{
				// attemp value numbering
				Value f = currentMap.findInsert(inst);
				if (f != inst)
				{
					// replace all usesList to inst with f.
					inst.replaceAllUsesWith(f);
				}
			}

			valueMaps.put(block, currentMap);
		}
	}

}
