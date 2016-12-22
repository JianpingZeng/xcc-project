package backend.analysis;

import backend.hir.BasicBlock;
import backend.hir.PredIterator;
import backend.hir.SuccIterator;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.LinkedList;

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
public class Loop extends LoopBase<BasicBlock, Loop>
{
	public Loop(Loop parent, LinkedList<BasicBlock> blocks)
	{
		super(parent, blocks);
	}

	/**
	 * Obtains the depth of this loop in the loop forest it attached, begins from
	 * 1.
	 * @return
	 */
	@Override
	public int getLoopDepth()
	{
		int d = 1;
		for (Loop curLoop = outerLoop; curLoop != null; curLoop = curLoop.outerLoop)
			d++;
		return d;
	}

	/**
	 * Check to see if a basic block is the loop exit block or not on that
	 * if the any successor block of the given bb is outside this loop, so that
	 * this bb would be a loop exit block..
	 * @param bb
	 * @return True if the given block is the exit block of this loop, otherwise
	 * returned false.
	 */
	@Override
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
	@Override
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
	 * If there is a preheader for this loop, return it.  A loop has a preheader
	 * if there is only one edge to the header of the loop from outside of the 
	 * loop.  IfStmt this is the case, the block branching to the header of the loop
	 * is the preheader node.
	 * </p>
	 * <p>This method returns null if there is no preheader for the loop.</p>
	 * @return
	 */
	@Override
	public BasicBlock getPreheader()
	{
		// keep track of blocks outside the loop branching to the header
		BasicBlock out = getLoopPredecessor();
		if (out == null) return null;
		
		// make sure there is exactly one exit out of the preheader
		if (out.getNumSuccessors() > 1)
			return null;
		// the predecessor has exactly one successor, so it is 
		// a preheader.
		return out;
	}
	
	/**
	 * If given loop's header has exactly one predecessor outside of loop,
	 * return it, otherwise, return null.
	 * @return
	 */
	@Override
	protected BasicBlock getLoopPredecessor()
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
	 * Return true if the specified loop contained in this.
	 * @param loop
	 * @return
	 */
	public boolean contains(Loop loop)
	{
		if (loop == null) return false;
		if (loop == this) return true;
		return contains(loop.outerLoop);
	}

	//========================================================================//
	// API for changing the CFG.
	@Override
	public void addBasicBlockIntoLoop(BasicBlock bb, LoopInfoBase<BasicBlock, Loop> li)
	{
		assert blocks.isEmpty() || li.getLoopFor(getHeaderBlock()) != null
				:"Incorrect LI specifed for this loop";
		assert bb != null;
		assert li.getLoopFor(bb) == null;

		li.getBBMap().put(bb, this);
		Loop l = this;
		while (l != null)
		{
			l.blocks.add(bb);
			l = l.getParent();
		}
	}

	@Override
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

	@Override
	public void addChildLoop(Loop loop)
	{
		assert loop != null && loop.outerLoop == null;
		loop.outerLoop = this;
		subLoops.add(loop);
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

