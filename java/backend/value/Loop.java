package backend.value;

import tools.Util;
import backend.support.LoopBase;
import backend.analysis.LoopInfo;
import backend.support.LoopInfoBase;
import backend.utils.PredIterator;
import backend.utils.SuccIterator;
//import backend.transform.scalars.LoopSimplify;
import backend.value.Instruction.PhiNode;
import tools.OutRef;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;

/** 
 * <p>
 * This class describe the concept of loop in a control flow graph of a method, 
 * which usually used for {@linkplain LoopInfo} when performing loop
 * backend.transform.
 * </p>
 * <p>Note that only reducible loop, so called natural loop, can be identified 
 * and optimized. In other word, all of irreducible loops were ignored when 
 * performing loop backend.transform.
 * </p>
 * @author Jianping Zeng
 * @version 0.1
 */
public class Loop extends LoopBase<BasicBlock, Loop>
{
	public Loop(BasicBlock block)
	{
		super(block);
	}

	public Loop() {super();}

	/**
	 * Return true if the specified value is loop-invariant.
	 * @param val
	 * @return
	 */
	public boolean isLoopInVariant(Value val)
	{
		if (val instanceof Instruction)
			return isLoopInVariant((Instruction)val);
		// All non-instructions are loop invariant
		return true;
	}

	/**
	 * Return true if the specified instruction is
	 * loop-invariant.
	 * @param inst
	 * @return
	 */
	public boolean isLoopInvariant(Instruction inst)
	{
		return !contains(inst.getParent());
	}

	public boolean makeLoopInvariant(Value val,
			OutRef<Boolean> changed,
			Instruction insertPtr)
	{
		return makeLoopInvariant(val, changed, null);
	}

	public boolean makeLoopInvariant(Value val,
			OutRef<Boolean> changed)
	{
		if (val instanceof Instruction)
		{
			return makeLoopInvariant((Instruction)val, changed);
		}
		return true;
	}

	public boolean makeLoopInvariant(Instruction inst,
			OutRef<Boolean> changed,
			Instruction insertPtr)
	{
		if (isLoopInVariant(inst))
			return true;
		if (inst.mayReadMemory())
			return false;
		if (insertPtr == null)
		{
			BasicBlock preheader = getLoopPreheader();
			if (preheader == null)
				return false;
			insertPtr = preheader.getTerminator();
		}
		for (int i = 0, e = inst.getNumOfOperands(); i < e; i++)
		{
			if (!makeLoopInvariant(inst.operand(i), changed, insertPtr))
				return false;
		}

		// Hoist.
		//inst.moveBefore(insertPtr);
		changed.set(false);
		return true;
	}

	public boolean makeLoopInvariant(Instruction inst,
			OutRef<Boolean> changed)
	{
		return makeLoopInvariant(inst, changed, null);
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
	 * if the any successor block of the given parent is outside this loop, so that
	 * this parent would be a loop exit block..
	 * @param bb
	 * @return True if the given block is the exit block of this loop, otherwise
	 * returned false.
	 */
	@Override
	public boolean isLoopExitingBlock(BasicBlock bb)
	{
		// The special case: parent is not contained in current loop, just return false.
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
	public BasicBlock getLoopPreheader()
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

	@Override
	public BasicBlock getLoopLatch()
	{
		BasicBlock header = getHeaderBlock();
		if (header == null) return null;
		BasicBlock latch = null;
		for (PredIterator<BasicBlock> predItr = header.predIterator(); predItr.hasNext();)
		{
			BasicBlock pred = predItr.next();
			if (contains(pred))
			{
				// If there are more than two latch blocks, return null.
				if (latch != null)
					return null;
				latch = pred;
			}
		}
		return latch;
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
		Util.assertion(blocks.isEmpty() || li.getLoopFor(getHeaderBlock()) != null, "Incorrect LI specifed for this loop");

		Util.assertion( bb != null);
		Util.assertion( li.getLoopFor(bb) == null);

		li.getBBMap().put(bb, this);
		Loop l = this;
		while (l != null)
		{
			l.blocks.add(bb);
			l = l.getParentLoop();
		}
	}

	@Override
	public void replaceChildLoopWith(Loop newOne, Loop oldOne)
	{
		Util.assertion( newOne != null && oldOne != null);
		Util.assertion( oldOne.outerLoop == this);
		Util.assertion( newOne.outerLoop == null);

		Util.assertion(subLoops.contains(oldOne), "oldOne loop not contained in current");
		int idx = subLoops.indexOf(oldOne);
		newOne.outerLoop = this;
		subLoops.set(idx, newOne);
	}

	@Override
	public void addChildLoop(Loop loop)
	{
		Util.assertion( loop != null && loop.outerLoop == null);
		loop.outerLoop = this;
		subLoops.add(loop);
	}

	/**
	 * Return true if there is no a exit block has a predecessor block which is
	 * outside of the loop.
	 * @return
	 */
	public boolean hasDedicatedExits()
	{
		ArrayList<BasicBlock> exits = getExitingBlocks();
		for (BasicBlock exitBB : exits)
		{
			PredIterator<BasicBlock> predItr = exitBB.predIterator();
			while(predItr.hasNext())
			{
				BasicBlock pred = predItr.next();
				if (!contains(pred))
					return false;
			}
		}
		return true;
	}

	/**
	 * Returns true if the loop is in the form that {LoopSimplify}
	 * transforms loops to, sometimes it is also called normal form.
	 *
	 * Normal-form loops have a preheader, a single backedge, and all of their
	 * exits have all their predecessors inside the loop.
	 * @return
	 */
	public boolean isLoopSimplifyForm()
	{
		return getLoopPreheader() != null && getLoopLatch() != null
				&& hasDedicatedExits();
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
				if (isLoopExitingBlock(bb))
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

	/**
	 * Check to see if the loop has a canonical
	 * induction variable: an integer recurrence that starts at 0 and increments
	 * by one each time through the loop.  If so, return the phi node that
	 * corresponds to it.
	 *
	 * The IndVarSimplify pass transforms loops to have a canonical induction
	 * variable.
	 * @return
	 */
	public PhiNode getCanonicalInductionVariable()
	{
		BasicBlock header = getHeaderBlock();
		BasicBlock incoming = null, backege = null;
		int numPreds = header.getNumPredecessors();
		Util.assertion(numPreds > 0,  "Loop must have at least one backedge!");

		backege = header.predAt(1);
		if (numPreds == 1)
			return null;    // dead loop.
		if (numPreds > 2)
			return null;    // multiple backedge.
		incoming = header.predAt(0);

		if (contains(incoming))
		{
			if (contains(backege))
				return null;
			BasicBlock tmp = incoming;
			incoming = backege;
			backege = tmp;
		}
		else if (!contains(backege))
			return null;

		// Loop over all of the PHI nodes, looking for a canonical indvar.
		for (Instruction inst : header)
		{
			if (!(inst instanceof PhiNode))
				break;
			PhiNode pn = (PhiNode)inst;
			Value incomingVal = pn.getIncomingValueForBlock(incoming);
			if (incomingVal instanceof ConstantInt)
			{
				ConstantInt ci = (ConstantInt)incomingVal;
				if (ci.isNullValue())
				{
					Value backedgeVal = pn.getIncomingValueForBlock(backege);
					if (backedgeVal instanceof Instruction)
					{
						Instruction inc = (Instruction)backedgeVal;
						if (inc.getOpcode() == Operator.Add
								&& inc.operand(0) == pn)
						{
							if (inc.operand(1) instanceof ConstantInt)
								if (((ConstantInt)inc.operand(1)).equalsInt(1))
									return pn;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Checks to see if this loop conforms to the LCSSA form, return {@code true}
	 * if this is conformed.
	 * <p>
	 * You can visit this <a href="https://gcc.gnu.org/onlinedocs/gccint/LCSSA.html">Gnu LCSSA form</a>
	 * for obtains more information about LCSSA form in detail.
	 * </p>
	 * @return
	 */
	public boolean isLCSSAForm()
	{
		// Checks it by determining if all uses of each instruction in the Basic block
		// contained in this loop are in this loop or not.
		// If so, return true, otherwise, return false.
		for (BasicBlock bb : blocks)
		{
			for (Instruction inst : bb.getInstList())
			{
				for (Use u : inst.getUseList())
				{
					// the basic block where the use of inst contained.
					BasicBlock userBB = ((Instruction)u.getUser()).getParent();
					if (u.getUser() instanceof PhiNode)
					{
						PhiNode pn = (PhiNode)u.getUser();
						userBB = pn.getIncomingBlock(u);
					}

					if (userBB != bb && !blocks.contains(userBB))
						return false;
				}
			}
		}
		return true;
	}

	/**
	 * Returns a list of all loop exit block.
	 * @return
	 */
	@Override
	public ArrayList<BasicBlock> getExitingBlocks()
	{
		ArrayList<BasicBlock> exitBBs = new ArrayList<>();
		for (BasicBlock block : blocks)
		{
			for (SuccIterator itr = block.succIterator(); itr.hasNext();)
			{
				BasicBlock succ = itr.next();
				if (!blocks.contains(succ))
					exitBBs.add(succ);
			}
		}
		return exitBBs;
	}

	/**
	 * Returns the unique exit blocks list of this loop.
	 * <p>
	 * The unique exit block means that if there are multiple edge from
	 * a block in loop to this exit block, we just count one.
	 * </p>
	 * @return
	 */
	@Override
	public ArrayList<BasicBlock> getUniqueExitBlocks()
	{
		HashSet<BasicBlock> switchExitBlocks = new HashSet<>();
		ArrayList<BasicBlock> exitBBs = new ArrayList<>();

		for (BasicBlock curBB : blocks)
		{
			switchExitBlocks.clear();
			for (SuccIterator succItr = curBB.succIterator(); succItr.hasNext();)
			{
				BasicBlock succBB = succItr.next();
				BasicBlock firstPred = succBB.predAt(0);

				if (curBB != firstPred)
						continue;

				if (curBB.getNumSuccessors() <= 2)
				{
					exitBBs.add(succBB);
					continue;
				}

				if (!switchExitBlocks.contains(succBB))
				{
					switchExitBlocks.add(succBB);
					exitBBs.add(succBB);
				}
			}
		}
		return exitBBs;
	}

	public boolean isEmpty()
	{
		return subLoops.isEmpty();
	}
}
