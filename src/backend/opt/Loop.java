package backend.opt;

import backend.hir.BasicBlock;
import backend.value.Instruction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/** 
 * <p>
 * This class describe the concept of loop in a control flow graph of a method, 
 * which usually used for {@linkplain LoopAnalysis} when performing loop 
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
	 * id are sorted in descending the {@linkplain #loopDepth} by an 
	 * instance of {@linkplain Comparator}.   
	 * </p>
	 */
	final LinkedList<BasicBlock> blocks;
	/**
	 * The index of this loop.
	 */
	final int loopIndex;
	/**
	 * The nested depth of this loop.
	 */
	final int loopDepth;
	/**
	 * A pointer to its outer loop loop which isDeclScope this.
	 */
	Loop outerLoop;
	/**
	 * An array of its subLoops loop contained in this.
	 */
	ArrayList<Loop> subLoops;
	/**
	 * A list of all exit blocks.
	 */
	private List<BasicBlock> exitBlocks;
	
	/**
	 * The basic block followed by this loop.
	 */
	private BasicBlock followBlock;
	
	public Loop(LinkedList<BasicBlock> blocks,
			List<BasicBlock> exitBlocks,
			BasicBlock followBlock)
	{
		assert blocks!= null && !blocks.isEmpty()
				&& exitBlocks != null && !exitBlocks.isEmpty();
		
		this.blocks = blocks;
		this.loopIndex = getHeaderBlock().loopIndex;
		this.loopDepth = getHeaderBlock().loopDepth;
		this.exitBlocks = exitBlocks;
		this.followBlock = followBlock;
	}
	
	public void setFollowBlock(BasicBlock bb){this.followBlock = bb;}
	
	public BasicBlock getFollowBlock() {return this.followBlock;}
	
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
	 * Obtains all exit basic blocks as an array in this loop.
	 * @return
	 */
	public List<BasicBlock> exitBlocks()
	{
		return exitBlocks;
	}
	
	/**
	 * Obtains the number of exit blocks.
	 * @return
	 */
	public int getNumOfExitBlock()
	{
		return exitBlocks.size();
	}
	/**
	 * Obtains the header block of this loop.
	 * @return
	 */
	public BasicBlock getHeaderBlock()
	{
		return blocks.get(0);				
	}
	
	public int getNumOfBlocks()
	{
		return blocks.size();
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
		if (out.getNumOfSuccs() > 1)
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
		for (BasicBlock pred : header.getPreds())
		{
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
	
	public void addBlock(BasicBlock bb)
	{
		assert bb != null : "bb not be null";
		assert !contains(bb) : "duplicated block added";
		blocks.add(bb);
	}
	
	public void removeExitBlock(BasicBlock bb)
	{
		exitBlocks.remove(bb);
	}
	public void addExitBlock(BasicBlock bb)
	{
		assert bb != null;
		assert !exitBlocks.contains(bb);
		exitBlocks.add(bb);
	}
	
	public boolean isExitBlock(BasicBlock bb )
	{
		assert bb != null;
		return exitBlocks.contains(bb);
	}
}

