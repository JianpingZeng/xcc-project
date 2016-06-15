package optimization; 

import hir.BasicBlock;
import hir.BasicBlock.BlockFlag;
import hir.Method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import optimization.LICM.Loop;
import utils.BitMap2D;

/**
 * This class defined as a helper class for identifying all loop in a method. 
 * @author Xlous.zeng
 * @version 0.1
 */
public class LoopIdentifier
{	
	/**
	 * The maximun block id at given cfg.
	 */
	private final int maxBlockID;
	/**
	 * A bit set whose element determines whether specified block is visited or not.
	 */
	private BitSet visitedBlocks;
	/**
	 * A bit set whose element determines whether specified block is active or not.
	 */
	private BitSet activeBlocks;
	
	/**
	 * A stack simulated by LinkedList that used for explicitly recursively
	 * traverse CFG.
	 */
	private LinkedList<BasicBlock> workList;
	
	/**
	 * An array of which every element is represented as the numbers of incoming
	 * forward edge of block.
	 */
	private int[] forwardBranches;
	/**
	 * The numbers of loop in the current CFG starts at entry.
	 */
	private int numLoops;

	private BitMap2D bitset;

	/**
	 * A list of all loop end block collected during traverse recursively.
	 */
	private List<BasicBlock> loopEndBlocks;
	
	/**
	 * A mapping from id to basic block.
	 */
	BasicBlock[] IdToBasicBlock;

	public LoopIdentifier(Method method)
    {
		maxBlockID = method.cfg.getNumberOfBasicBlocks();
		IdToBasicBlock = new BasicBlock[maxBlockID];		
		visitedBlocks = new BitSet(maxBlockID);
		activeBlocks = new BitSet(maxBlockID);
		forwardBranches = new int[maxBlockID];
		loopEndBlocks = new ArrayList<>(8);		

		workList = new LinkedList<>();
		BasicBlock entry = method.getEntryBlock();
		
		createIdToBlockMap(method);
		
		// depth first traverse to count loop
		countLoops(entry, null);
		
		// handles loop if the numbers of loop is greater than zero.
		if (numLoops > 0)
		{
			markLoops();
			clearNonNatureLoops();
			assignLoopDepth(entry);
		}
    }
	/**
	 * Associates the block id with basic block.
	 * @param method
	 */
	private void createIdToBlockMap(Method method)
	{
		for (BasicBlock bb : method)
		{
			IdToBasicBlock[bb.getID()] = bb;
		}
	}
	/**
	 * Retrieves an array of {@linkplain Loop} whose each item represents a loop in 
	 * control flow graph. 
	 * @return
	 */
	public Loop[] getLoopList()
	{
		LinkedList<Integer> list = new LinkedList<>();
		ArrayList<Loop> loops = new ArrayList<>();
		int headerBlock = -1;
		List<Integer> endBlocks = new ArrayList<>();
		for (int i = 0; i < numLoops; i++)
		{
			list.clear();					
			
			// walk through all block bit instead of skipping 
			// those block that does not contained in loop			
			for (int j = 0; j < maxBlockID; j++)
			{
				BasicBlock bb = null;
				if (bitset.at(i, j))
				{
					bb = IdToBasicBlock[j];
					
					if (bb.checkBlockFlags(BlockFlag.LinearScanLoopHeader))
					{					
						headerBlock = j;
						continue;
					}
					if (bb.checkBlockFlags(BlockFlag.LinearScanLoopEnd))
					{
						endBlocks.add(j);
						continue;
					}
					list.add(j);			
				}
			}
			
			assert headerBlock >= 0 : "No header block found in loop";
			assert endBlocks.size() > 0 : "No end block found in loop";
			
			// a loop consists of a single block
			if (endBlocks.size() == 1 && headerBlock == endBlocks.get(0))
			{
				list.add(headerBlock);
			}
			else 
			{
				list.addFirst(headerBlock);
				list.addAll(endBlocks);
			}
			if (!list.isEmpty())
			{
				Integer[] arr = list.toArray(new Integer[list.size()]);
				BasicBlock bb = IdToBasicBlock[headerBlock];
				Loop loop = new Loop(arr, bb.loopIndex, bb.loopDepth, endBlocks.size());
				loop.setIdToBasicBlock(IdToBasicBlock);
				loops.add(loop);
			}
		}
		
		// creates a linked list of nested relation using pointer outer and inner 
		markNested(loops);
		Loop[] sortedLoops = loops.toArray(new Loop[loops.size()]);
		sortedByLoopDepth(sortedLoops);
		return sortedLoops;
	}
	
	/**
	 * Sorts the specified array of {@linkplain Loop} in decreasing the 
	 * {@linkplain Loop#loopDepth}.
	 * @param loops
	 */
	private void sortedByLoopDepth(Loop[] loops)
	{
		Arrays.sort(loops, new Comparator<Loop>()
		{
			@Override
			public int compare(Loop o1, Loop o2)
			{
				if (o1.loopDepth > o1.loopDepth)
					return -1;
				else if (o1.loopDepth == o2.loopDepth)
					return 0;
				else 				
					return 1;			
			}
		});
	}
	
	/**
	 * find out the relationship of nested among loops, and outer loop is indexed by 
	 * {@linkplain Loop#outer} and {@linkplain Loop#inner} is responsible for inner nest.
	 * @param loops
	 */
	private void markNested(ArrayList<Loop> loops)
	{
		for (int i = 0; i < loops.size(); i++)
		{
			Loop first = loops.get(i);
			for (int j = i + 1; j < loops.size(); j++)
			{
				Loop second = loops.get(j);
				if (first.loopDepth == second.loopDepth)
					continue;
				else if (first.loopDepth > second.loopDepth
						&& contains(second, first))
				{
					second.inner = first;
					first.outer = second;
				}
				else if (first.loopDepth < second.loopDepth
						&& contains(first, second))
				{
					first.inner = second;
					second.outer = first;
				}
			}
		}
	}
	/**
	 * checks if the loop {@code src} contains another loop {@code dest}
	 * @param src	The outer loop
	 * @param dest	The inner loop.
	 * @return	return true if condition satisfied, otherwise return false. 
	 */
	private boolean contains(Loop src, Loop dest)
	{
		for(int bbId : dest.blocks)
		{
			if (!bitset.at(src.loopIndex, bbId))
				return false;
		}
		return true;
	}
	
	/**
	 * check for non-natural loops (loops where the loop header does not dominate
	 * all other loop blocks = loops with multiple entries).
	 * such loops are ignored
	 *
	 * @param entry
	 */
	private void clearNonNatureLoops()
	{
		int headers = 0;
		for (int idx = numLoops - 1; idx >= 0; idx--)
		{		
			// loop i contains the entry block of method
			// this is not a natural loop, so ignore it
			for (int blockID = maxBlockID - 1; 
					blockID >= 0;
					blockID--
					)
			{
				if (!bitset.at(idx, blockID))
					continue;
				
				if (IdToBasicBlock[blockID].checkBlockFlags(BlockFlag.LinearScanLoopHeader))
				{
					headers++;
				}
				
				// if there are more than one loop header block, it is irreducible
				if (headers > 1)
				{
					for (int i = 0; i < maxBlockID; i++)
						bitset.clearBit(idx, i);
					break;
				}
			}
			headers = 0;
		}
	}
	private void initVisited()
	{
		visitedBlocks.clear();
		activeBlocks.clear();
	}
	
	private void assignLoopDepth(BasicBlock entry)
	{
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

	private boolean isBlockInLoop(int loopIndex, BasicBlock block)
	{
		return bitset.at(loopIndex, block.getID());
	}
	
	private void markLoops()
	{
		workList.clear();
		bitset = new BitMap2D(numLoops, maxBlockID);

		for (int idx = loopEndBlocks.size() - 1; idx >= 0; idx--)
		{
			BasicBlock loopEnd = loopEndBlocks.get(idx);
			BasicBlock loopHeader = loopEnd.succAt(0);
			int loopIndex = loopHeader.loopIndex;

			assert loopEnd.checkBlockFlags(
					BasicBlock.BlockFlag.LinearScanLoopEnd)
					: "loop end must be seted.";
			assert loopHeader.checkBlockFlags(
					BasicBlock.BlockFlag.LinearScanLoopHeader)
					: "loop header must be seted";
			assert loopIndex >= 0 && loopIndex
					< numLoops : "numLoops: loop index must be set";
			// add the end-block of the loop to the working list
			workList.add(loopEnd);
			bitset.setBit(loopIndex, loopEnd.getID());

			do
			{
				BasicBlock top = workList.removeLast();

				assert isBlockInLoop(loopIndex, top) 
					: "bit in loop map must be set when block is in work list";

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
	
	private boolean isActive(BasicBlock block)
	{
		boolean result = activeBlocks.get(block.getID());
		assert result : "Active already set.";
		return result;
	}
	
	private boolean isVisited(BasicBlock block)
	{
		boolean result = visitedBlocks.get(block.getID());
		assert result : "Visited already set.";
		return result;
	}
	private void setVisited(BasicBlock block)
	{
		visitedBlocks.set(block.getID());
	}

	private void setActive(BasicBlock block)
	{
		activeBlocks.set(block.getID());
	}
	
	private void clearActive(BasicBlock block)
	{
		activeBlocks.clear(block.getID());
	}

	private void increForwardBrach(BasicBlock block)
	{
		forwardBranches[block.getID()]++;
	}
	
	/**
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
	 */
	private void countLoops(BasicBlock start, BasicBlock parent)
	{
		// Depth first traverse this cfg.
		// A naively understanding method to traverse is recursive algorithm but
		// that is no sufficient due to stack depth issue.

		if (isActive(start))
		{
			assert isVisited(start) : "The backward block must be visied and actived";
			assert parent != null : "Backward block must heve parent";

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

		// Recursively call of all successors.
		ListIterator<BasicBlock> itr = start.getSuccs().listIterator();
		while (itr.hasPrevious())
		{
			countLoops(itr.previous(), start);
		}

		// after handling all successors
		clearActive(start);

		// Each loop has a unique number.
		// When multiple loops are nested, assignLoopDepth assumes that the
		// innermost loop has the lowest number. This is guaranteed by setting
		// the loop number after the recursive calls for the successors above
		// have returned.
		if (start.checkBlockFlags(BasicBlock.BlockFlag.LinearScanLoopHeader))
		{
			assert start.loopIndex == -1 
					: "Can not set the loop index twice";
			start.loopIndex = numLoops++;
		}
	}
}
