package backend.value;


import tools.Util;
import backend.support.LLVMContext;
import backend.utils.BackwardIterator;
import backend.utils.PredIterator;
import backend.utils.SuccIterator;
import backend.value.Instruction.BranchInst;
import backend.value.Instruction.PhiNode;
import backend.value.Instruction.TerminatorInst;

import java.util.*;

/**
 * Represents a basic block in the quad intermediate representation. Basic
 * blocks are single-entry regions, but not necessarily single-exit regions. Due
 * to the fact that control flow may exit a basic block early due to runtime
 * jlang.exception.
 * <p>
 * Each basic block isDeclScope a serial of quads, a list of predecessors, a list
 * of successors. It also has an id id that is unique within its control
 * flow graph.
 * <p>
 * Note that you should never create directly a basic block using the
 * constructor of {@link BasicBlock}, you should create it via a
 * Control flow graph so that id id is unique.
 * <p>
 * @author Jianping Zeng
 * @version 1.0
 * @see Instruction
 */
public final class BasicBlock extends Value implements Iterable<Instruction>
{
	public static final BasicBlock USELESS_BLOCK =
			new BasicBlock("useless", null);

	/**
	 * Unique id id for this basic block.
	 */
	private int idNumber;

	/**
	 * The numbering when performing linear scanning.
	 */
	public int linearScanNumber = -1;

	/**
	 * A list of quads.
	 */
	private final LinkedList<Instruction> instructions;

	private int blockFlags;

	public int loopIndex = -1;

	public int loopDepth;

    private Function parent;
	
	/**
	 * A field of loop containing this basic block.
	 */
	private Loop outLoop;
	
	/**
	 * Obtains a loop containing this basic block.
	 * @return
	 */
	public Loop getOuterLoop()
	{
		return this.outLoop;
	}
	/**
	 * Update the loop containing this basic block with a new loop.
	 * @param loop
	 */
	public void setOutLoop(Loop loop)
	{
		this.outLoop = loop;
	}

	public boolean isCriticalEdgeSplit()
	{
		return (blockFlags | BlockFlag.CriticalEdgeSplit.mask) != 0;
	}

	public boolean isPredecessor(BasicBlock block)
	{
	    PredIterator itr = predIterator();
		while (itr.hasNext())
        {
            if (itr.next() == block)
                return true;
        }
        return false;
	}

    public Function getParent()
    {
        return parent;
    }

    public void setParent(Function fn)
    {
    	Util.assertion( fn != null);
    	parent = fn;
    }

	/**
	 * This predicate returns true if there is a constant user refers it existing.
	 * @return
	 */
	public boolean hasConstantReference()
	{
		for (Use u : usesList)
			if (u.getUser() instanceof Constant)
				return true;
		return false;
	}

	/**
	 * If this block has only one predecessor block, just return it, otherwise
	 * return null.
	 * @return
	 */
	public BasicBlock getSinglePredecessor()
	{
		int num = getNumPredecessors();
		return num == 1 ? predAt(0) : null;
	}

	/**
	 * Unlink this basic block from its current function and insert it into the
	 * function that movePos resides, right after movePos.
	 * @param movePos
	 */
	public void moveAfter(BasicBlock movePos)
	{
		List<BasicBlock> list = movePos.getParent().getBasicBlockList();
		int idx = list.indexOf(movePos);
		ArrayList<BasicBlock> removed = new ArrayList<>();
		LinkedList<BasicBlock> self = getParent().getBasicBlockList();
		for (int i = self.indexOf(this), e = self.size(); i < e;i++)
		{
			removed.add(self.remove(i));
		}
		list.addAll(idx, removed);
	}

	/**
	 * Unlink this basic block from its current function and insert it into the
	 * function that movePos resides, right before movePos.
	 * @param movePos
	 */
	public void moveBefore(BasicBlock movePos)
	{
		List<BasicBlock> list = movePos.getParent().getBasicBlockList();
		int idx = list.indexOf(movePos);
		ArrayList<BasicBlock> removed = new ArrayList<>();
		LinkedList<BasicBlock> self = getParent().getBasicBlockList();
		for (int i = self.indexOf(this), e = self.size(); i < e;i++)
		{
			removed.add(self.remove(i));
		}
		list.addAll(--idx, removed);
	}

	public boolean hasSuccessor(BasicBlock succ)
	{
		for (SuccIterator itr = succIterator(); itr.hasNext();)
			if (itr.next() == succ)
				return true;
		return false;
	}

	public boolean hasPredecessor(BasicBlock pred)
	{
		for (PredIterator<BasicBlock> itr = predIterator(); itr.hasNext();)
			if (itr.next() == pred)
				return true;
		return false;
	}

    public enum BlockFlag
	{
		LinearScanLoopHeader,
		LinearScanLoopEnd,
		BackwardBrachTarget,
		CriticalEdgeSplit;

		public final int mask = 1 << ordinal();
	}

	/**
	 * A private constructor for entry node
	 */
	private BasicBlock(
			String bbName,
			Function newParent,
			BasicBlock insertBefore)
	{
		super(LLVMContext.LabelTy, ValueKind.BasicBlockVal);
		this.idNumber = 0;
		this.instructions = new LinkedList<>();
        if (insertBefore != null)
        {
            Util.assertion(newParent!=null, "Can't insert block before another block");
            newParent.addBasicBlockBefore(insertBefore, this);
        }
        else if (newParent != null)
        {
            newParent.addBasicBlock(this);
        }
        setName(bbName);
	}

	private BasicBlock(
			String bbName,
			Function parent)
	{
		this(bbName, parent, null);
	}

	/**
	 * Create new internal basic block.
	 */
	public static BasicBlock createBasicBlock(
            String bbName,
            Function parent, BasicBlock before)
	{
		return new BasicBlock(bbName, parent, before);
	}

	/**
	 * Create new internal basic block.
	 */
	public static BasicBlock createBasicBlock(
			String bbName,
			Function parent)
	{
		return new BasicBlock(bbName, parent);
	}

	public static BasicBlock createBasicBlock(String bbName, BasicBlock insertBefore)
	{
		return new BasicBlock(bbName, null, insertBefore);
	}

	/**
	 * Returns iterator over Instructions in this basic block in forward order.
	 *
	 * @return Returns iterator over Instructions in this basic block in forward order.
	 */
	public ListIterator<Instruction> iterator()
	{
		if (instructions == null)
			return Collections.<Instruction>emptyList().listIterator();
		else
			return instructions.listIterator();
	}

	/**
	 * Returns iterator over Quads in this basic block in forward order.
	 *
	 * @return Returns iterator over Quads in this basic block in forward order.
	 */
	public BackwardIterator<Instruction> backwardIterator()
	{
		if (instructions == null)
			return new BackwardIterator<Instruction>(
					Collections.<Instruction>emptyList().listIterator());
		else
			return new BackwardIterator<Instruction>(
					instructions.listIterator());
	}

	/**
	 * Gets the index into instructions list. ReturnInst -1 if instruction no isDeclScope
	 * specified inst. Otherwise, return the index of first occurrence.
	 * @param inst
	 * @return
	 */
	public int indexOf(Instruction inst)
	{
		if (inst == null) return -1;
		return instructions.indexOf(inst);
	}
	/**
	 * Returns the id of quads in this basic block.
	 *
	 * @return the id of quads in this basic block.
	 */
	public int size()
	{
		if (instructions == null)
			return 0; // entry or exit block
		return instructions.size();
	}

	/**
	 * Determines Wether the instructions list of this basic block is empty or not.
	 * @return return true if this instructions list is empty or null.
	 */
	public boolean isEmpty()
	{
		if (instructions == null)
			return true;
		return instructions.isEmpty();
	}

	public Instruction getInstAt(int i)
	{
		return instructions.get(i);
	}

	public boolean removeInst(Instruction q)
	{
		return instructions.remove(q);
	}

	public void clear()
	{
		instructions.clear();
	}

	/**
	 * Add a quad to this basic block at the given location. Cannot add quads to
	 * the entry or exit basic blocks.
	 *
	 * @param index the index to add the quad
	 * @param inst the instuction to be added into insts list.
	 */
	public void insertAt(Instruction inst, int index)
	{
		Util.assertion((inst != null),  "Cannot add null instruction to block");
		Util.assertion((index >= 0 && index < instructions.size()), 				"The index into insertion of gieven inst is bound out.");


		instructions.add(index, inst);
	}

	/**
	 * Append a quad to the end of this basic block. Cannot add quads to the
	 * entry or exit basic blocks.
	 *
	 * @param inst quad to add
	 */
	public void appendInst(Instruction inst)
	{
		Util.assertion((inst != null),  "Can't add null instructions to block");
		if (instructions.isEmpty() || !(instructions.getLast() instanceof BranchInst))
		{
			instructions.add(inst);
		}
		else 
		{
			Util.assertion((getLastInst() instanceof BranchInst), 				"Can not insert more than one branch in basic block");

			instructions.add(inst);
		}
		inst.setParent(this);
	}

	public int getID()
	{
		return this.idNumber;
	}

	public Instruction getFirstInst()
	{
		return instructions.get(0);
	}

	public Instruction getLastInst()
	{
		if (instructions.isEmpty())
			return null;
		return instructions.get(instructions.size() - 1);
	}

	public void insertAfter(Instruction inst, int after)
	{
		Util.assertion( after >=1 && after < getNumOfInsts());
		if (after == getNumOfInsts() - 1)
			instructions.add(inst);
		else
			instructions.add(after + 1, inst);
		inst.setParent(this);
	}

	public void insertAfter(Instruction inst, Instruction insertAfter)
	{
		Util.assertion(inst != null && insertAfter != null, "Can't use null inst");
		Util.assertion(insertAfter != null, "May use append method to add it to the last!");
		int index = instructions.indexOf(insertAfter);
		Util.assertion(index != -1, "insertAfter isn't exists in current basic block!");
		insertAfter(inst, index);
		inst.setParent(this);
	}

	public void insertBefore(Instruction inst, Instruction insertBefore)
	{
		if (insertBefore == null)
		{
			appendInst(inst);
			return;
		}
		int index = instructions.indexOf(insertBefore);
		Util.assertion(index != - 1, "InsertBefore is not exists in Instruction list");
		insertBefore(inst, index);
		inst.setParent(this);
	}
	
	public void insertBefore(Instruction inst, int insertBefore)
	{
		Util.assertion( insertBefore >= 0 && insertBefore < getNumOfInsts());
		instructions.add(insertBefore, inst);
		inst.setParent(this);
	}

	/**
	 * Inserts a instruction into the position after the first inst of instructions
	 * list.
	 * @param inst
	 */
	public void insertAfterFirst(Instruction inst)
	{
		Util.assertion( inst != null);

		if (instructions.isEmpty())
			instructions.addFirst(inst);
		else
		{
			instructions.add(1, inst);
		}
		inst.setParent(this);
	}

	public int lastIndexOf(Instruction inst)
	{
		return instructions.lastIndexOf(inst);
	}

	/**
	 * Removes this removed block and unlink it with attached successors list.
	 * @param removed   The basic block to be remvoed.
	 * @return
	 */
	public boolean removeSuccssor(BasicBlock removed)
	{
		return false;
	}

	public void removePredecessor(BasicBlock pred)
	{
		removePredecessor(pred, false);
	}
	/**
	 * This method is used for notifying this block that it's specified predecessor
	 * is no longer able to reach it.This is actually not used to update the
	 * Predecessor list, but is actually used to update the PHI nodes that
	 * reside in the block.  Note that this should be called while the predecessor
	 * still refers to this block.
	 * @param pred   The basic block to be pred.
	 * @return
	 */
	public void removePredecessor(BasicBlock pred, boolean dontDeletedUselessPHI)
	{
		if (instructions.isEmpty()) return;
		if (!(getFirstInst() instanceof PhiNode))
			return;
		PhiNode pn = (PhiNode) getFirstInst();

		int idx = pn.getNumberIncomingValues();
		Util.assertion(idx != 0, "PHI node in block with 0 predecessors!");

		if (idx == 2)
		{
			BasicBlock other = pn.getIncomingBlock(pn.getIncomingBlock(0) == pred ? 1:0);
			if (this == other) idx = 3;
		}

		if (idx <= 2 && !dontDeletedUselessPHI)
		{
			while (getFirstInst() instanceof PhiNode)
			{
				pn = (PhiNode) getFirstInst();
				// Remove the predecessor first.
				pn.removeIncomingValue(pred, !dontDeletedUselessPHI);

				// If the PHI _HAD_ two uses, replace PHI node with its now *single* value
				if (idx == 2)
				{
					if (pn.getIncomingValue(0) == pn)
						pn.replaceAllUsesWith(pn.getIncomingValue(0));
					else
						// we are left with an infinite loop with no entries: kill the PHI.
						pn.replaceAllUsesWith(UndefValue.get(pn.getType()));
					instructions.removeFirst();
				}

				// If the PHI node already only had one entry, it got deleted by
				// removeIncomingValue.
			}
		}
		else
		{
			// Okay, now we know that we need to remove predecessor #pred_idx from all
			// PHI nodes.  Iterate over each PHI node fixing them up
			for (Instruction inst : instructions)
			{
				if (!(inst instanceof PhiNode)) break;

				pn = (PhiNode)inst;
				pn.removeIncomingValue(pred, false);
				// If all incoming values to the PHI are the same, we can
				// replace the PHI with that value.
				Value val;
				if (!dontDeletedUselessPHI && (val = pn.hasConstantValue()) != null)
				{
					if (val != pn)
					{
						pn.replaceAllUsesWith(val);
						pn.eraseFromParent();
					}
				}
			}
		}
	}

	public void dropAllReferences()
	{
		for (Instruction inst : instructions)
			inst.dropAllReferences();
		instructions.clear();
	}

	/**
	 * Erases itself from control flow graph.
	 */
	public void eraseFromParent()
	{
	    if (parent != null)
		    parent.getBasicBlockList().remove(this);
	    TerminatorInst ti = getTerminator();
	    if (ti != null)
        {
            for (int i = 0, e = ti.getNumOfOperands(); i < e; i++)
            {
                if (ti.operand(0) instanceof BasicBlock)
                {
                    BasicBlock bb = (BasicBlock) ti.operand(0);
                    bb.getUseList().remove(ti.getOperand(i));
                    ti.operandList.remove(i);
                    --e;
                    --i;
                }
            }
        }
	}
	/**
	 * Returns the terminator instruction if the block is well formed or
	 * return null if block is not well formed.
	 * @return
	 */
	public TerminatorInst getTerminator()
	{
	    if (instructions.isEmpty()) return null;

		Instruction inst = instructions.getLast();
		if (inst instanceof TerminatorInst)
            return (TerminatorInst)inst;
		return null;
	}
	
	public void setBlockFlags(BlockFlag flag)
	{
		blockFlags |= flag.mask;
	}

	public void clearBlockFlags(BlockFlag flag)
	{
		blockFlags &= ~flag.mask;
	}

	public boolean checkBlockFlags(BlockFlag flag)
	{
		return (blockFlags & flag.mask) != 0;
	}

	public PredIterator<BasicBlock> predIterator()
    {
        return new PredIterator<>(this);
    }

    public SuccIterator succIterator()
    {
        return new SuccIterator(this);
    }

	public int getNumOfInsts()
	{
		return instructions.size();
	}

	public LinkedList<Instruction> getInstList(){return instructions;}

	public int getNumSuccessors()
	{
		TerminatorInst inst = getTerminator();
		if (inst == null) return 0;
		return inst.getNumOfSuccessors();
	}

	public BasicBlock suxAt(int index)
	{
		Util.assertion( index >= 0 && index < getNumSuccessors());
		TerminatorInst inst = getTerminator();
		if (inst == null) return null;
		return inst.getSuccessor(index);
	}

	public int getNumPredecessors()
	{
		if (usesList == null || usesList.isEmpty())
			return 0;

		return Math.toIntExact(usesList.stream()
				.filter(u -> u.getUser() instanceof TerminatorInst).count());
	}

	public BasicBlock predAt(int index)
	{
		Util.assertion( index >= 0 && index < getNumPredecessors());
		for (Use u : usesList)
		{
			if (u.getUser() instanceof TerminatorInst)
			{
				if (index == 0)
					return ((TerminatorInst)u.getUser()).getParent();
				--index;
			}
		}
		Util.assertion(false, "Should not reaching here!");
		return null;
	}

	/**
	 * Return the index that point to the first non-phi instruction in the basic
	 * block.
	 * @return
	 */
	public int getFirstNonPhi()
	{
	    int i = 0;
	    while (getInstAt(i) instanceof PhiNode) i++;
		return i;
	}
}
