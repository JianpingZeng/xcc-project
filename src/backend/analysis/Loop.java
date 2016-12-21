package backend.analysis;

import backend.hir.BasicBlock;
import backend.hir.PredIterator;
import backend.hir.SuccIterator;
import backend.value.Instruction;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/** 
 * <p>
 * This class describe the concept of loop in a control flow graph of a method, 
 * which usually used for {@linkplain LoopInfo} when performing loop
 * backend.opt.
 * </p>
 * <p>Note that only reducible loop, so called natural loop, can be identified 
 * and optimized. In other word, all of irreducible loops were ignored when 
 * performing loop backend.opt.
 * </p>
 * @author Xlous.zeng
 * @version 0.1
 */
public class Loop
{
	/**
	 * <p>
	 * A sequence of block Id. 
	 * <br>
	 * The first item must be a loop header block. Also all of block 
	 * id are sorted in descending the {@linkplain #getLoopDepth()} by an
	 * instance of {@linkplain Comparator}.   
	 * </p>
	 */
	final LinkedList<BasicBlock> blocks;
	/**
	 * The index of this loop.
	 */
	final int loopIndex;
	/**
	 * A pointer to its outer loop loop which isDeclScope this.
	 */
	private Loop outerLoop;
	/**
	 * An array of its subLoops loop contained in this.
	 */
	ArrayList<Loop> subLoops;
	
	public Loop(Loop parent, LinkedList<BasicBlock> blocks)
	{
		assert blocks!= null && !blocks.isEmpty();
		
		this.blocks = blocks;
		this.outerLoop = parent;
		this.loopIndex = getHeaderBlock().loopIndex;
	}
	
	/**
	 * Retrieves the index of a basic block at the specified position where indexed by a index 
	 * variable. 
	 * @param index	A index that indexed into specified position where TargetData block located.
	 * @return	A basic block.
	 */
	public BasicBlock getBlock(int index)
	{
		assert index >= 0 && index < blocks.size();
		return blocks.get(index);
	}

	/**
	 * Obtains the header block of this loop.
	 * @return
	 */
	public BasicBlock getHeaderBlock()
	{
		assert blocks != null && !blocks.isEmpty() :"There is no block in loop";
		return blocks.get(0);				
	}
	
	public int getNumOfBlocks()
	{
		return blocks.size();
	}

	/**
	 * Obtains the depth of this loop in the loop forest it attached, begins from
	 * 1.
	 * @return
	 */
	public int getLoopDepth()
	{
		int d = 1;
		for (Loop outer = this; outer.outerLoop != null; outer = outer.outerLoop)
			d++;
		return d;
	}

	public Loop getOuterLoop()
	{
		return outerLoop;
	}

	public void setOuterLoop(Loop loop)
	{
		outerLoop = loop;
	}

	public ArrayList<Loop> getSubLoops()
	{
		return subLoops;
	}

	public List<BasicBlock> getBlocks()
	{
		return blocks;
	}

	/**
	 * Check to see if a basic block is the loop exit block or not on that
	 * if the any successor block of the given bb is outside this loop, so that
	 * this bb would be a loop exit block..
	 * @param bb
	 * @return True if the given block is the exit block of this loop, otherwise
	 * returned false.
	 */
	public boolean isLoopExitBlock(BasicBlock bb)
	{
		// The special case: bb is not contained in current loop, just return false.
		if (!contains(bb))
			return false;

		for (SuccIterator succItr = bb.succIterator(); succItr.hasNext();)
		{
			if (!contains(succItr.next()))
				return true;
		}
		return false;
	}

	/**
	 * Computes the backward edge leading to the header block in the loop.
	 * @return
	 */
	public int getNumBackEdges()
	{
		int numBackEdges = 0;
		PredIterator<BasicBlock> itr = getHeaderBlock().predIterator();
		while (itr.hasNext())
		{
			if (contains(itr.next()))
				++numBackEdges;
		}
		return numBackEdges;
	}

	/**
	 * <p>
	 * IfStmt there is a preheader for this loop, return it.  A loop has a preheader
	 * if there is only one edge to the header of the loop from outside of the 
	 * loop.  IfStmt this is the case, the block branching to the header of the loop
	 * is the preheader node.
	 * </p>
	 * <p>This method returns null if there is no preheader for the loop.</p>
	 * @return
	 */
	public BasicBlock getPreheader()
	{
		// keep track of blocks outside the loop branching to the header
		BasicBlock out = getLoopPrecedessor();
		if (out == null) return null;
		
		// make sure there is exactly one exit out of the preheader
		if (out.getNumSuccessors() > 1)
			return null;
		// the predecessor has exactly one successor, so it is 
		// a preheader.
		return out;
	}
	
	/**
	 * IfStmt given loop's header has exactly one predecessor outside of loop,
	 * return it, otherwise, return null.
	 * @return
	 */
	private BasicBlock getLoopPrecedessor()
	{
		BasicBlock header = getHeaderBlock();
		BasicBlock outer = null;
		for (PredIterator<BasicBlock> predItr = header.predIterator(); predItr.hasNext();)
		{
			BasicBlock pred = predItr.next();
			if (!contains(pred))
			{
				if (outer != null && outer != pred)
					return null;
				outer = pred;
			}
		}
		return outer;
	}
	/**
	 * IfStmt given basic block is contained in this loop, return true,
	 * otherwise false returned.
	 * @param block
	 * @return
	 */
	public boolean contains(BasicBlock block)
	{
		return blocks.contains(block);		
	}
	/**
	 * IfStmt given instruction is contained in this loop, return true,
	 * otherwise false returned
	 * @param inst
	 * @return
	 */
	public boolean contains(Instruction inst)
	{
		return contains(inst.getParent());
	}
	
	/**
	 * ReturnInst true if the specified loop contained in this.
	 * @param loop
	 * @return
	 */
	public boolean contains(Loop loop)
	{
		if (loop == null) return false;
		if (loop == this) return true;
		return contains(loop.outerLoop);
	}
	
	public void removeBlock(BasicBlock bb)
	{
		assert bb != null : "bb not be null";
		assert contains(bb) : "bb must contained in loop";
		
		blocks.remove(bb);
	}
	
	public void addFirstBlock(BasicBlock bb)
	{
		assert bb != null : "bb not be null";
		assert !contains(bb) : "duplicated block added";
		
		blocks.addFirst(bb);
	}

	/**
	 * Returns a list of all loop exit block.
	 * @return
	 */
	public ArrayList<BasicBlock> getExitBlocks()
	{
		ArrayList<BasicBlock> exitBlocks = new ArrayList<>(blocks.size());
		blocks.forEach(bb ->
		{
			if (isLoopExitBlock(bb))
				exitBlocks.add(bb);
		});
		return exitBlocks;
	}

	/**
	 * If {@linkplain #getExitBlocks()} exactly return one block, then this
	 * method will return it, otherwise retunn null;
	 * @return
	 */
	public BasicBlock getExitBlock()
	{
		ArrayList<BasicBlock> res = getExitBlocks();
		if (res.size() == 1)
			return res.get(0);
		return null;
	}
	//========================================================================//
	// API for changing the CFG.

	public void addBasicBlockIntoLoop(BasicBlock bb, LoopInfo li)
	{

	}

	public void replaceChildLoopWith(Loop newOne, Loop oldOne)
	{
		assert newOne != null && oldOne != null;
		assert oldOne.outerLoop == this;
		assert newOne.outerLoop == null;

		assert subLoops.contains(oldOne) :"oldOne loop not contained in current";
		int idx = subLoops.indexOf(oldOne);
		newOne.outerLoop = this;
		subLoops.set(idx, newOne);
	}

	public void addChildLoop(Loop loop)
	{
		assert loop != null && loop.outerLoop == null;
		loop.outerLoop = this;
		subLoops.add(loop);
	}

	public void removeChildLoop(int index)
	{
		assert index>= 0 && index < subLoops.size();
		subLoops.remove(index);
	}

	public void addBlockEntry(BasicBlock bb)
	{
		assert bb != null;
		blocks.add(bb);
	}

	public void removeBlockFromLoop(BasicBlock bb)
	{
		assert bb != null;
		blocks.remove(bb);
	}

	public void print(OutputStream os, int depth)
	{
		try (PrintWriter writer = new PrintWriter(os))
		{
			writer.print(String.format("%" + depth * 2 + "s", " "));
			writer.printf("Loop at depth: %d, containing: ", getLoopDepth());
			for (int i = 0, e = blocks.size(); i < e; i++)
			{
				if (i != 0)
					writer.print(",");
				BasicBlock bb = blocks.get(i);
				writer.printf("Block#%s", bb.getName());
				if (bb == getHeaderBlock())
					writer.print("<header>");
				if (isLoopExitBlock(bb))
					writer.print("<exit>");
			}
			writer.println();
			for (Loop subLoop : subLoops)
				subLoop.print(os, depth + 2);
		}
	}

	public void dump()
	{
		print(System.err, 0);
	}
}

