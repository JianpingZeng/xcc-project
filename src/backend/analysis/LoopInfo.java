package backend.analysis;

import backend.hir.BasicBlock;
import backend.hir.BasicBlock.BlockFlag;
import backend.hir.PredIterator;
import backend.hir.SuccIterator;
import backend.opt.UnreachableMachineBlockElim;
import backend.pass.AnalysisUsage;
import backend.pass.FunctionPass;
import backend.value.Function;
import tools.BitMap2D;

import java.util.*;

/**
 * This class defined as a helper class for identifying all loop in a method. 
 * @author Xlous.zeng
 * @version 0.1
 */
public final class LoopInfo extends FunctionPass
	implements LoopInfoBase<BasicBlock, Loop>
{
	/**
	 * The maximum block id at given cfg.
	 */
	private int maxBlockID;
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
	private BasicBlock[] idToBasicBlock;

	private Function m;

	private HashMap<BasicBlock, Loop> bbMap = new HashMap<>();

	private ArrayList<Loop> topLevelLoops = new ArrayList<>();

	/**
	 * Associates the block id with basic block.
	 * @param function
	 */
	private void createIdToBlockMap(Function function)
	{
		for (BasicBlock bb : function)
		{
			idToBasicBlock[bb.getID()] = bb;
		}
	}

	private void init(Function f)
	{
		m = f;
		maxBlockID = f.cfg.getNumberOfBasicBlocks();
		idToBasicBlock = new BasicBlock[maxBlockID];
		visitedBlocks = new BitSet(maxBlockID);
		activeBlocks = new BitSet(maxBlockID);
		forwardBranches = new int[maxBlockID];
		loopEndBlocks = new ArrayList<>(8);

		workList = new LinkedList<>();
		BasicBlock entry = f.getEntryBlock();

		createIdToBlockMap(f);

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

	@Override
	public void getAnalysisUsage(AnalysisUsage au)
	{
		assert au != null;
		au.addRequired(UnreachableMachineBlockElim.class);
		super.getAnalysisUsage(au);
	}

	@Override
	public boolean runOnFunction(Function f)
	{
		init(f);

		LinkedList<BasicBlock> list = new LinkedList<>();
		ArrayList<Loop> loops = new ArrayList<>();
		BasicBlock headerBlock = null;
		List<BasicBlock> exitBlocks = new ArrayList<>();
		BasicBlock followBlock = null;
		
		for (int i = 0; i < numLoops; i++)
		{
			list.clear();					
			exitBlocks.clear();
			
			// walk through all block bit instead of skipping 
			// those block that does not contained in loop			
			for (int j = 0; j < maxBlockID; j++)
			{
				BasicBlock bb = null;
				if (bitset.at(i, j))
				{
					bb = idToBasicBlock[j];
					
					if (bb.checkBlockFlags(BlockFlag.LinearScanLoopHeader))
					{					
						headerBlock = bb;
						continue;
					}
					
					if (isExitBlock(bb, i, followBlock))
					{
						exitBlocks.add(bb);
					}
					
					list.add(bb);			
				}
			}
			// ignore empty loop
			if (list.isEmpty())
				continue;
			
			assert headerBlock != null : "No header block found in loop";			
			assert !exitBlocks.isEmpty() : "No exit block found in loop";
			// a loop consists of a single block
			if (list.isEmpty() && headerBlock != null)
			{
				list.add(headerBlock);
			}
			else 
			{
				list.addFirst(headerBlock);
			}
			
			Loop loop = new Loop(null,list);

			// set containing loop
			for (BasicBlock bb : list)
				bb.setOutLoop(loop);
			
			loops.add(loop);		
		}
		
		// creates a linked list of nested relation using pointer outerLoop and subLoops 
		markNested(loops);
		Loop[] sortedLoops = loops.toArray(new Loop[loops.size()]);
		sortedByLoopDepth(sortedLoops);
		
		// set loops for given function being compiled
		m.setLoops(sortedLoops);
		return false;
	}
	
	/**
	 * Check if specified basic block is a exit block in a loop.
	 * When it is return true, otherwise return false.
	 * @param bb	A basic block to be checked.
	 * @param rowIdx	The loop index.
	 * @return
	 */
	private boolean isExitBlock(
            BasicBlock bb, int rowIdx, BasicBlock followBlock)
	{
		assert bb != null && rowIdx>= 0 && rowIdx < bitset.sizeInSlots();
		// go through all successors of bb to check
		SuccIterator itr = bb.succIterator();
		while (itr.hasNext())
		{
			BasicBlock sux = itr.next();
			int id = sux.getID();			
			// if there is at least one successor that not contained in loop
			// , it is must be an exit block.
			if (!bitset.at(rowIdx, id)) 
			{
				followBlock = sux;
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * Sorts the specified array of {@linkplain Loop} in decreasing the 
	 * {@linkplain Loop#getLoopDepth()}.
	 * @param loops
	 */
	private void sortedByLoopDepth(Loop[] loops)
	{
		Arrays.sort(loops, new Comparator<Loop>()
		{
			@Override
			public int compare(Loop o1, Loop o2)
			{
				if (o1.getLoopDepth() > o1.getLoopDepth())
					return -1;
				else if (o1.getLoopDepth() == o2.getLoopDepth())
					return 0;
				else 				
					return 1;			
			}
		});
	}
	
	/**
	 * find out the relationship of nested among loops, and outerLoop loop is indexed by 
	 * {@linkplain Loop#outerLoop} and {@linkplain Loop#subLoops} is responsible for subLoops nest.
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
				if (first.getLoopDepth() == second.getLoopDepth())
					continue;
				else if (first.getLoopDepth() > second.getLoopDepth()
						&& contains(second, first))
				{
					second.subLoops.add(first);
					first.setOuterLoop(second);
				}
				else if (first.getLoopDepth() < second.getLoopDepth()
						&& contains(first, second))
				{
					first.subLoops.add(second);
					second.setOuterLoop(first);
				}
			}
		}
	}
	/**
	 * checks if the loop {@code src} isDeclScope another loop {@code dest}
	 * @param src	The outerLoop loop
	 * @param dest	The subLoops loop.
	 * @return	return true if condition satisfied, otherwise return false. 
	 */
	private boolean contains(Loop src, Loop dest)
	{
		for(BasicBlock bb : dest.blocks)
		{
			int bbId = bb.getID();
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
	 */
	private void clearNonNatureLoops()
	{
		int headers = 0;
		for (int idx = numLoops - 1; idx >= 0; idx--)
		{		
			// loop i isDeclScope the entry block of method
			// this is not a natural loop, so ignore it
			for (int blockID = maxBlockID - 1; 
					blockID >= 0;
					blockID--
					)
			{
				if (!bitset.at(idx, blockID))
					continue;
				
				if (idToBasicBlock[blockID].checkBlockFlags(BlockFlag.LinearScanLoopHeader))
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
				for (int idx = curr.getNumSuccessors() - 1; idx >= 0; idx--)
					workList.addLast(curr.suxAt(idx));
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
			BasicBlock loopHeader = loopEnd.suxAt(0);
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
					PredIterator<BasicBlock> itr = top.predIterator();
					ArrayList<BasicBlock> temp = new ArrayList<>();
					while(itr.hasNext()) temp.add(itr.next());

					for (int i = temp.size() - 1; i >= 0; i--)
					{
						BasicBlock pred = temp.get(i);
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
		for (int i = start.getNumSuccessors() - 1; i >= 0; i++)
		{
			countLoops(start.suxAt(i), start);
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

	@Override
	public String getPassName()
	{
		return "The statistic of loop info on HIR";
	}

	@Override
	public HashMap<BasicBlock, Loop> getBBMap()
	{
		return bbMap;
	}

	@Override
	public ArrayList<Loop> getTopLevelLoop()
	{
		return topLevelLoops;
	}

	@Override
	public int getLoopDepth(BasicBlock bb)
	{
		Loop loop = getLoopFor(bb);
		return loop != null ? loop.getLoopDepth() : 0;
	}

	@Override
	public boolean isLoopHeader(BasicBlock bb)
	{
		Loop loop = getLoopFor(bb);
		return loop != null && bb == loop.getHeaderBlock();
	}

	@Override
	public void ensureIsTopLevel(Loop loop, String msg)
	{
		assert loop.getParent() == null:msg;
	}

	@Override
	public void removeBlock(BasicBlock block)
	{
		if (bbMap.containsKey(block))
		{
			Loop loop = bbMap.get(block);
			while(loop != null)
			{
				loop.removeBlockFromLoop(block);
				loop = loop.getParent();
			}
			bbMap.remove(block);
		}
	}
}
