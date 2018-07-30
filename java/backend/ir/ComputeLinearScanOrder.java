/*package backend.hir;

import tools.Util;
import backend.analysis.DomTree;
import backend.value.Instruction;
import tools.BitMap2D;
import tools.TTY;
import java.util.*;

/**
 * <p>
 * This class defined for computing the linear scanning order upon given control
 * flow graph.
 * </p>
 * @author Jianping Zeng
 * @version 0.1
 *
public class ComputeLinearScanOrder
{
	/**
	 * The maximun block id at given cfg.
	 *
	private final int maxBlockID;
	/**
	 * The corresponding dominator tree.
	 *
	private final DomTree DT;
	/**
	 * A bit set whose element determines whether specified block is visited or not.
	 *
	private BitSet visitedBlocks;
	/**
	 * A bit set whose element determines whether specified block is active or not.
	 *
	private BitSet activeBlocks;
	/**
	 * A stack simulated by LinkedList that used for explicitly recursively
	 * traverse CFG.
	 *

	private LinkedList<BasicBlock> workList;
	/**
	 * The list where all block stores in linear scanning order.
	 *
	private ArrayList<BasicBlock> linearScanOrder;
	/**
	 * An array of which every element is represented as the numbers of incoming
	 * forward edge of block.
	 *
	private int[] forwardBranches;
	/**
	 * The numbers of loop in the current CFG starts at entry.
	 *
	private int numLoops;

	private int numBlocks;

	private BitMap2D bitset;

	/**
	 * A list of all loop end block collected during traverse recursively.
	 *
	private List<BasicBlock> loopEndBlocks;

	/**
	 * Creates a new instance of this class for computes linear scanning order.
	 *
	 * @param maxBlockID The max block id of block of this cfg.
	 * @param entry      The entry of this control flow graph.
	 * @param DT         The dominator tree corresponding to this control flow graph.
	 *
	public ComputeLinearScanOrder(int maxBlockID, BasicBlock entry,
			DomTree DT)
	{
		this.maxBlockID = maxBlockID;
		visitedBlocks = new BitSet(maxBlockID);
		activeBlocks = new BitSet(maxBlockID);
		forwardBranches = new int[maxBlockID];
		this.DT = DT;
		loopEndBlocks = new ArrayList<>(8);		

		workList = new LinkedList<>();

		// Actually, critical edge split is needed here
		// but it is completed in ControlFlowGraph#linearScanOrder() method.
		countEdges(entry, null);

		// handles loop if the numbers of loop is greater than zero.
		if (numLoops > 0)
		{
			markLoops();
			clearNonNatureLoops(entry);
			assignLoopDepth(entry);
		}

		computeScanOrder(entry);
		printBlocks();

		Util.assertion( verify());
	}

	private boolean verify()
	{
		Util.assertion(linearScanOrder.size()				== numBlocks,  "wrong number of blocks in list");


		// check that all successors of a block have a higher linear-scan-number
		// and that all predecessors of a block have a lower linear-scan-number
		// (only backward branches of loops are ignored)

		for (int i = 0; i < linearScanOrder.size(); i++)
		{
			BasicBlock cur = linearScanOrder.get(i);

			Util.assertion(cur.linearScanNumber == i,  "incorrect linearScanNumber");
			Util.assertion(cur.linearScanNumber >= 0					&& cur.linearScanNumber == linearScanOrder
					.indexOf(cur),  "incorrect linearScanNumber");


			SuccIterator itr = cur.succIterator();
			while (itr.hasNext())
			{
				BasicBlock sux = itr.next();
				Util.assertion(sux.linearScanNumber >= 0						&& sux.linearScanNumber == linearScanOrder
						.indexOf(sux),  "incorrect linearScanNumber");

				if (!cur.checkBlockFlags(
						BasicBlock.BlockFlag.LinearScanLoopEnd))
				{
					Util.assertion(cur.linearScanNumber							< sux.linearScanNumber,  "invalid order");

				}
				if (cur.loopDepth == sux.loopDepth)
				{
					Util.assertion(cur.loopIndex == sux.loopIndex || sux							.checkBlockFlags(
									BasicBlock.BlockFlag.LinearScanLoopHeader),  "successing blocks with same loop depth must have same loop index");

				}
			}

			PredIterator<BasicBlock> predItr = cur.predIterator();
			while(predItr.hasNext())
			{
			    BasicBlock pred = predItr.next();
				Util.assertion(pred.linearScanNumber >= 0						&& pred.linearScanNumber == linearScanOrder
						.indexOf(pred),  "incorrect linearScanNumber");

				if (!cur.checkBlockFlags(
						BasicBlock.BlockFlag.LinearScanLoopHeader))
				{
					Util.assertion(cur.linearScanNumber							> pred.linearScanNumber,  "invalid order");

				}
				if (cur.loopDepth == pred.loopDepth)
				{
					Util.assertion(cur.loopIndex == pred.loopIndex || cur							.checkBlockFlags(
									BasicBlock.BlockFlag.LinearScanLoopHeader),  "successing blocks with same loop depth must have same loop index");

				}
				BasicBlock dom = DT.getIDom(cur);
				Util.assertion( dom != null);
				Util.assertion(dom.linearScanNumber						<= pred.linearScanNumber,  "dominator must be before predecessors");

			}
			BasicBlock dom = DT.getIDom(cur);

			// check dominator
			if (i == 0)
			{
				Util.assertion(dom == null,  "first block has no dominator");
			}
			else
			{
				Util.assertion(dom != null,  "all but first block must have dominator");
			}
			Util.assertion(cur.getNumOfPreds() != 1 || dom == cur					.predAt(0),  "Single predecessor must also be dominator");

		}

		// check that all loops are continuous
		for (int loopIdx = 0; loopIdx < numLoops; loopIdx++)
		{
			int blockIdx = 0;
			Util.assertion(!isBlockInLoop(loopIdx, linearScanOrder					.get(blockIdx)),  "the first block must not be present in any loop");


			// skip blocks before the loop
			while (blockIdx < numBlocks && !isBlockInLoop(loopIdx,
					linearScanOrder.get(blockIdx)))
			{
				blockIdx++;
			}
			// skip blocks of loop
			while (blockIdx < numBlocks && isBlockInLoop(loopIdx,
					linearScanOrder.get(blockIdx)))
			{
				blockIdx++;
			}
			// after the first non-loop block : there must not be another loop-block
			while (blockIdx < numBlocks)
			{
				Util.assertion(!isBlockInLoop(loopIdx, linearScanOrder						.get(blockIdx)),  "loop not continuous in linear-scan order");

				blockIdx++;
			}
		}

		return true;
	}

	private void printBlocks()
	{
		TTY.println("----- linear-scan block order:");
		for (BasicBlock cur : linearScanOrder)
		{
			TTY.print(String.format("%4d: B%02d    loop: %2d  depth: %2d",
					cur.linearScanNumber, cur.getID(), cur.loopIndex,
					cur.loopDepth));

			TTY.print(cur.isCriticalEdgeSplit() ? " ce" : "   ");
			TTY.print(cur.checkBlockFlags(
					BasicBlock.BlockFlag.LinearScanLoopHeader) ? " lh" : "   ");
			TTY.print(cur.checkBlockFlags(
					BasicBlock.BlockFlag.LinearScanLoopEnd) ? " le" : "   ");

			BasicBlock iDom = DT.getIDom(cur);
			if (iDom != null)
			{
				TTY.print("    dom: B%d ", iDom.getID());
			}
			else
			{
				TTY.print("    dom: null ");
			}

			if (cur.getNumOfPreds() > 0)
			{
				TTY.print("    preds: ");
				for (BasicBlock pred : cur.getPreds())
					TTY.print("B%d ", pred.getID());
			}

			if (cur.getNumOfSuccs() > 0)
			{
				TTY.print("    sux: ");
				for (BasicBlock sux : cur.getSuccs())
					TTY.print("B%d ", sux.getID());
			}
		}
		TTY.println();
	}

	private boolean isReadyProcess(BasicBlock block)
	{
		if (forwardBranches[block.getID()] == 0)
			return true;
		if (--forwardBranches[block.getID()] != 0)
			return false;
		Util.assertion(!linearScanOrder.contains(				block),  "block already processed(block can be ready only once)");

		Util.assertion(!workList.contains(				block),  "block already in work list(block can be ready only once)");

		return true;
	}

	private void sortInWorkList(BasicBlock block)
	{
		Util.assertion(!workList.contains(block),  "block already in work list");

		int curWdeight = computeWeight(block);

		// the linearScanNumber is used for cache the
		// weight of a block.
		block.linearScanNumber = curWdeight;

		// inserts a placeholder.
		workList.add(null);

		int insertIdx = workList.size() - 1;

		// performs a pass of insertion sort algorithm in increment order.
		while (insertIdx > 0 && (curWdeight <= workList
				.get(insertIdx - 1).linearScanNumber))
		{
			workList.set(insertIdx, workList.get(insertIdx - 1));
			insertIdx--;
		}

		workList.set(insertIdx, block);

		for (int i = 0; i < workList.size(); i++)
		{
			Util.assertion(workList.get(i).linearScanNumber > 0,  "weight not set");

			Util.assertion(i == 0 || workList.get(i - 1).linearScanNumber <= workList					.get(i).linearScanNumber,  "incorrect order");

		}
	}

	private int computeWeight(BasicBlock cur)
	{
		BasicBlock singleSux = null;
		if (cur.getNumOfSuccs() == 1)
		{
			singleSux = cur.succAt(0);
		}

		// limit loop-depth to 15 bit (only for security reason, it will never be so big)
		int weight = (cur.loopDepth & 0x7FFF) << 16;

		int curBit = 15;

		// this is necessary for the (very rare) case that two successive blocks have
		// the same loop depth, but a different loop index (can happen for endless loops
		// with frontend.exception handlers)
		if (!cur.checkBlockFlags(BasicBlock.BlockFlag.LinearScanLoopHeader))
		{
			weight |= 1 << curBit;
		}
		curBit--;

		// loop end blocks (blocks that end with a backward branch) are added
		// after all other blocks of the loop.
		if (!cur.checkBlockFlags(BasicBlock.BlockFlag.LinearScanLoopEnd))
		{
			weight |= 1 << curBit;
		}
		curBit--;

		// critical edge split blocks are preferred because then they have a greater
		// probability to be completely empty
		if (cur.isCriticalEdgeSplit())
		{
			weight |= 1 << curBit;
		}
		curBit--;

		if (!(cur.getLastInst() instanceof Instruction.ReturnInst) && (
				singleSux == null || !(singleSux
						.getLastInst() instanceof Instruction.ReturnInst)))
		{
			weight |= 1 << curBit;
		}
		curBit--;

		// guarantee that weight is > 0
		weight |= 1;

		Util.assertion(curBit >= 0,  "too many flags");
		Util.assertion(weight > 0,  "weight cannot become negative");

		return weight;
	}

	private void appendBlock(BasicBlock block)
	{
		Util.assertion(!linearScanOrder.contains(				block),  "duplicate append into linear scan order list");


		block.linearScanNumber = linearScanOrder.size();
		linearScanOrder.add(block);
	}

	private void initVisited()
	{
		visitedBlocks.clear();
		activeBlocks.clear();
	}

	private void computeScanOrder(BasicBlock entry)
	{
		System.out.println("computes finally linear scan order.");

		linearScanOrder = new ArrayList<>(numBlocks);
		Util.assertion(workList.isEmpty(),  "work list must be empty before computing");

		if (isReadyProcess(entry))
			sortInWorkList(entry);
		else
			throw new Error("the entry block must ready for processing.");

		while (!workList.isEmpty())
		{
			BasicBlock cur = workList.removeLast();

			appendBlock(cur);

			int numOfSuxs = cur.getNumOfSuccs();
			for (int i = 0; i < numOfSuxs; i++)
			{
				BasicBlock succ = cur.succAt(i);
				if (isReadyProcess(succ))
					sortInWorkList(succ);
			}
		}
	}

	private void assignLoopDepth(BasicBlock entry)
	{
		TTY.println("----- computing loop-depth and weight");

		initVisited();
		workList.clear();
		workList.addLast(entry);

		// depth first traverse the CFG.
		while (!workList.isEmpty())
		{
			BasicBlock curr = workList.removeLast();
			if (!isVisited(curr))
			{
				setVisited(curr);
				TTY.println("computing loop-depth for CompoundStmt%d", curr.getID());

				int minLoopIndex = -1;
				int loopDepth = 0;

				for (int idx = numLoops - 1; idx >= 0; idx--)
				{
					if (isBlockInLoop(idx, curr))
					{
						minLoopIndex = idx;
						loopDepth++;
					}
				}
				curr.loopDepth = loopDepth;
				curr.loopIndex = minLoopIndex;

				// appends all unvisited successor block into work list
				for (int idx = curr.getNumOfSuccs() - 1; idx >= 0; idx--)
					workList.addLast(curr.succAt(idx));
			}
		}
	}

	/**
	 * check for non-natural loops (loops where the loop header does not dominate
	 * all other loop blocks = loops with multiple entries).
	 * such loops are ignored
	 *
	 * @param entry
	 *
	private void clearNonNatureLoops(BasicBlock entry)
	{
		for (int idx = numLoops - 1; idx >= 0; idx--)
		{
			if (isBlockInLoop(idx, entry))
			{
				// loop i isDeclScope the entry block of method
				// this is not a natural loop, so ignore it
				for (int blockID = maxBlockID - 1; blockID >= 0; blockID--)
					bitset.clearBit(idx, blockID);
			}
		}
	}

	private void increForwardBrach(BasicBlock block)
	{
		forwardBranches[block.getID()]++;
	}

	public List<BasicBlock> linearScanOrder()
	{
		return linearScanOrder;
	}

	/**
	 * /**
	 * Traverses the CFG to analyze block and edge info. The analysis performed
	 * is:
	 * <br>
	 * 1. Count of total number of blocks.
	 * <br>
	 * 2. Count of all incoming edges and backward incoming edges.
	 * <br>
	 * 3. Number loop header blocks.
	 * <br>
	 * 4. Create a list with all loop end blocks.
	 *
	 * @param start
	 * @param parent
	 *
	private void countEdges(BasicBlock start, BasicBlock parent)
	{
		// Depth first traverse this cfg.
		// A naively understanding method to traverse is recursive algorithm but
		// that is no sufficient due to stack depth issue.

		if (isActive(start))
		{
			TTY.println("Backward edge.");
			Util.assertion(isVisited(					start),  "The backward block must be visied and actived");

			Util.assertion(parent != null,  "Backward block must heve parent");

			start.setBlockFlags(BasicBlock.BlockFlag.LinearScanLoopHeader);
			start.setBlockFlags(BasicBlock.BlockFlag.BackwardBrachTarget);

			parent.setBlockFlags(BasicBlock.BlockFlag.LinearScanLoopEnd);

			loopEndBlocks.add(parent);
			return;
		}
		if (parent != null)
			increForwardBrach(start);
		if (isVisited(start))
			return;

		setVisited(start);
		setActive(start);
		numBlocks++;

		// Recursively call of all successors.
		ListIterator<BasicBlock> itr = start.getSuccs().listIterator();
		while (itr.hasPrevious())
		{
			countEdges(itr.previous(), start);
		}

		// after handling all successors
		clearActive(start);

		// Each loop has a unique number.
		// When multiple loops are nested, assignLoopDepth assumes that the
		// innermost loop has the lowest number. This is guaranteed by setting
		// the loop number after the recursive calls for the successors above
		// have returned.
		if (start
				.checkBlockFlags(BasicBlock.BlockFlag.LinearScanLoopHeader))
		{
			Util.assertion(start.loopIndex					== -1,  "Can not set the loop index twice");


			TTY.println("CompoundStmt B%d is loop header of loop %d",
					start.getID(), numLoops);
			start.loopIndex = numLoops++;
		}

		TTY.println("Finished count edge for block%d", start.getID());
	}

	private void markLoops()
	{
		TTY.println("-----------Marking loops");

		workList.clear();
		bitset = new BitMap2D(numLoops, maxBlockID);

		for (int idx = loopEndBlocks.size() - 1; idx >= 0; idx--)
		{
			BasicBlock loopEnd = loopEndBlocks.get(idx);
			BasicBlock loopHeader = loopEnd.succAt(0);
			int loopIndex = loopHeader.loopIndex;

			TTY.println("Processing the loop from block%d to block%d(loop%d)",
					loopHeader.getID(), loopEnd.getID(), loopIndex);

			Util.assertion(loopEnd.checkBlockFlags(					BasicBlock.BlockFlag.LinearScanLoopEnd),  "loop end must be seted.");

			Util.assertion(loopHeader.checkBlockFlags(					BasicBlock.BlockFlag.LinearScanLoopHeader),  "loop header must be seted");

			Util.assertion(loopIndex >= 0 && loopIndex					< numLoops,  "numLoops: loop index must be set");

			// add the end-block of the loop to the working list
			workList.add(loopEnd);
			bitset.setBit(loopIndex, loopEnd.getID());

			do
			{
				BasicBlock top = workList.removeLast();
				TTY.println("-----------Processing CompoundStmt%d", top.getID());

				Util.assertion(isBlockInLoop(loopIndex,						top),  "bit in loop map must be set when block is in work list");


				// recursively processing predecessor ends when the loop header
				// block is reached
				if (top != loopHeader)
				{
					ListIterator<BasicBlock> itr = top.getPreds()
							.listIterator();
					while (itr.hasPrevious())
					{
						BasicBlock pred = itr.previous();
						if (!isBlockInLoop(loopIndex, pred))
						{
							workList.addLast(pred);
							bitset.setBit(loopIndex, pred.getID());
						}
					}
				}
			} while (!workList.isEmpty());
		}
	}

	private boolean isBlockInLoop(int loopIndex, BasicBlock block)
	{
		return bitset.at(loopIndex, block.getID());
	}

	private boolean isVisited(BasicBlock block)
	{
		boolean result = visitedBlocks.get(block.getID());
		Util.assertion(result,  "Visited already set.");
		return result;
	}

	private boolean isActive(BasicBlock block)
	{
		boolean result = activeBlocks.get(block.getID());
		Util.assertion(result,  "Active already set.");
		return result;
	}

	private void clearActive(BasicBlock block)
	{
		activeBlocks.clear(block.getID());
	}

	private void setVisited(BasicBlock block)
	{
		visitedBlocks.set(block.getID());
	}

	private void setActive(BasicBlock block)
	{
		activeBlocks.set(block.getID());
	}

	public int numLoops()
	{
		return numLoops;
	}
}
*/