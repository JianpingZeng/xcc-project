package optimization; 

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import utils.Pair;
import utils.TTY;
import hir.BasicBlock;
import hir.ControlFlowGraph;
import hir.DominatorTree;
import hir.Instruction;
import hir.Instruction.Branch;
import hir.Instruction.Goto;
import hir.Instruction.Op1;
import hir.Instruction.Op2;
import hir.Instruction.Phi;
import hir.Method;
import hir.Use;
import hir.Value;
import hir.Value.Constant;
import hir.Value.UndefValue;

/** 
 * </p>This class performs loop invariant code motion, attempting to remove
 * as much code from the body of a loop as possible. It does this by either
 * hoisting code into the preheader block, or by sinking code to the exit 
 * block if it is safe. Currently, this class does not use alias analysis
 * so that the all optimization operated upon memory access are excluded.
 * </p>
 * 
 * <p>This pass expected to run after {@linkplain LoopInversion Loop Inversion} 
 * performed.
 * </p>
 * 
 * <p>This file is a member of <a href={@docRoot/optimization}>Machine Independence
 * Optimization</a>
 * </p>.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class LICM
{
	/** 
	 * <p>
	 * This class describe the concept of loop in a control flow graph of a method, 
	 * which usually used for {@linkplain LoopIdentifier} when performing loop 
	 * optimization.
	 * </p>
	 * <p>Note that only reducible loop can be identified and optimized. 
	 * In other word, all of irreducible loops were ignored when performing 
	 * loop optimization. 
	 * </p>
	 * @author Xlous.zeng
	 * @version 0.1
	 */
	public static class Loop
	{
		/**
		 * <p>
		 * A sequence of block Id. 
		 * <br>
		 * The first item must be a loop header block and last one is loop end block.
		 * <br>
		 * Also all of block id are sorted in descending the {@linkplain #loopDepth} 
		 * by an instance of {@linkplain Comparator}.   
		 * </p>
		 */
		final int[] blocks;
		/**
		 * The index of this loop.
		 */
		final int loopIndex;
		/**
		 * The nested depth of this loop.
		 */
		final int loopDepth;
		/**
		 * A pointer to its outer loop which contains this.
		 */
		Loop outer;
		/**
		 * A pointer to its inner loop contained in this.
		 */
		Loop inner;
		/**
		 * The number of end basic block.
		 */
		final int numEndBlocks;
		
		private BasicBlock[] IdToBasicBlock; 
		
		public Loop(int[] blocks, int loopIndex, int loopDepth, int numEndBlocks)
		{
			this.blocks = blocks;
			this.loopIndex = loopIndex;
			this.loopDepth = loopDepth;
			this.numEndBlocks = numEndBlocks;
		}
		
		public Loop(Integer[] blocks, int loopIndex, int loopDepth, int numEndBlocks)	
		{
			this.blocks = new int[blocks.length];
			for (int i = 0; i < blocks.length; i++)
				this.blocks[i] = blocks[i];
			this.loopIndex = loopIndex;
			this.loopDepth = loopDepth;
			this.numEndBlocks = numEndBlocks;
		}
		public void setIdToBasicBlock(BasicBlock[] IdToBasicBlock)
		{
			this.IdToBasicBlock = IdToBasicBlock;				
		}
		/**
		 * Retrieves the index of a basic block at the specified position where indexed by a index 
		 * variable. 
		 * @param index	A index that indexed into specified position where target block located.
		 * @return	A basic block.
		 */
		public int getBlockId(int index)
		{
			assert index >= 0 && index < blocks.length;
			return blocks[index];
		}
		/**
		 * Obtains all end basic blocks as an array in this loop.
		 * @return
		 */
		public int[] endBlocks()
		{
			int[] res = new int[numEndBlocks];
			int j = blocks.length - 1;
			for (int i = numEndBlocks - 1; i >= 0; i--)
			{
				res[i] = blocks[j--];
			}
			return res;
		}
		
		public BasicBlock getHeaderBlock()
		{
			return IdToBasicBlock[blocks[0]];				
		}
		
		public int getNumOfBlocks()
		{
			return blocks.length;
		}
		/**
		 * <p>
		 * If there is a preheader for this loop, return it.  A loop has a preheader 
		 * if there is only one edge to the header of the loop from outside of the 
		 * loop.  If this is the case, the block branching to the header of the loop 
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
		 * If given loop's header has exactly one predecessor outside of loop,
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
		 * If given basic block is contained in this loop, return true,
		 * otherwise false returned.
		 * @param block
		 * @return
		 */
		public boolean contains(BasicBlock block)
		{
			int blkID = block.getID();
			for (int id : blocks)
			{
				if (id == blkID)
					return true;
			}
			return false;
		}
		/**
		 * If given instruction is contained in this loop, return true,
		 * otherwise false returned
		 * @param inst
		 * @return
		 */
		public boolean contains(Instruction inst)
		{
			return contains(inst.getParent());
		}
	}
	
	private final Loop[] loops;
	private boolean[] marked;
	private Loop[] loopIdToLoops;
	private BasicBlock[] IdToBasicBlock;
	private boolean[][] instInvariant;
	private ArrayList<Pair<Integer, Integer>> invarOrder;
	private DominatorTree dt;
	/**
	 * Constructor.
	 * @param hir	The control flow graph of function being compiled.
	 */
	public LICM(Method method)
	{
		dt = new DominatorTree(method);
		dt.recalculate();
		LoopIdentifier identifier = new LoopIdentifier(method);
		loops = identifier.getLoopList();
		IdToBasicBlock = identifier.IdToBasicBlock;
		marked = new boolean[loops.length];
		int maxLoopIndex = -1;
		for (Loop l : loops)
		{
			if (l.loopIndex > maxLoopIndex)
				maxLoopIndex = l.loopIndex;
		}
		
		loopIdToLoops = new Loop[maxLoopIndex + 1];
		// initialize the map IdToLoops
		for (Loop l : loops)
			loopIdToLoops[l.loopIndex] = l;		
		invarOrder = new ArrayList<>(16);
	}
	
	/**
	 * Starting to perform optimization of loop invariant code motion.
	 */
	public void runOnLoop()
	{
		// go through all loop from nested depth-most loop to outer
		// to mark invariant and take code motion
		for (Loop loop : loops)
		{
			if (!marked[loop.loopIndex])
			{
				do 
				{
					// 1#: identify loop invariant
					markInvariant(loop);
					
					// 2#: move loop invariant to preheader block
					moveInvariant(loop);
					// 
					marked[loop.loopIndex] =true;
					loop = loop.outer;
				}while (loop.outer != null);
			}
		}
	}	
	
	private void markInvariant(Loop loop)
	{
		// initialize some necessarily data structure
		int nblocks = loop.blocks.length;
		// represents which instruction is invariant
		instInvariant = new boolean[nblocks][];		
		invarOrder.clear();
		
		for (int i = 0; i < nblocks; i++)
		{
			BasicBlock bb = IdToBasicBlock[loop.blocks[i]];
			boolean[] insts = new boolean[bb.size()];
			for (int j = 0; j < bb.size(); j++)
			{
				insts[j] = false;
			}
			instInvariant[i] = insts;
		}
		boolean changed = false;
		// then call markBlock() for each block
		do
        {
			for (int i = 0; i < loop.blocks.length; i++)
			{
				BasicBlock bb = IdToBasicBlock[loop.blocks[i]];
				changed |= markBlock(loop.blocks, IdToBasicBlock[loop.blocks[0]], bb, i);
			}
        } while (changed);
	}
	
	private boolean markBlock(int[] blocks, BasicBlock entry, BasicBlock bb, int i)
	{
		boolean changed = false;
		Instruction inst = null;
		for (int j = 0; j < bb.size(); j++)
		{
			// check whether each instruction in this block has loop invariant
			// operands and appropriate reaching definition; if so, mark it as
			// loop invariant.
			if (!instInvariant[i][j])
			{
				inst = bb.getInst(j);
				// check unary expression
				if (inst instanceof Op1)
				{
					Op1 op = (Op1)inst;
					if (loopConstant(op.x)
							|| reachDefsOut(blocks, op.x)
							|| reachDefsIn(blocks, op.x))
					{
						instInvariant[i][j] = true;
					}
				}
				// check binary expression
				else if (inst instanceof Op2)
				{
					Op2 op = (Op2)inst;
					Value x = op.x;
					Value y = op.y;
					if ((loopConstant(x)
						|| reachDefsOut(blocks, x)
						|| reachDefsIn(blocks, x))
						&& 
						(loopConstant(y)
						|| reachDefsOut(blocks, y)
						|| reachDefsIn(blocks, y))
						)
					{
						instInvariant[i][j] = true;
					}
				}
			}
			// if the instruction being handled is invariant, then we would add its 
			// coordinate into invarOrder list
			if (instInvariant[i][j])
			{
				invarOrder.add(new Pair<Integer, Integer>(i, j));
				changed = true;
			}
		}
		return changed;
	}
	
	private boolean loopConstant(Value x)
	{
		return x instanceof Constant;
	}
	/**
	 * <p>Checks if the basic block where the operand defined was out of this loop.</p>
	 * <p>Return true if it is out of Loop, otherwise false returned.</p>
	 * @param blocks
	 * @param operand
	 * @return
	 */
	private boolean reachDefsOut(int[] blocks, Value operand)
	{
		if (operand instanceof Instruction)
		{
			Instruction inst = (Instruction)operand;
			BasicBlock bb = inst.getParent();
			if (blockIndex(bb.getID(), blocks) >= 0)
				return false;
			else 
				return true;			
		}
		return false;
	}
	/**
	 * Obtains the index by which the specified block will be indexed. 
	 * @param blockId	The id of specified basic block.
	 * @param nblocks	The array of block id.
	 * @return If there no block with specified blockId existed in nblocks, return -1
	 * , otherwise, return its index.
	 */
	private int blockIndex(int blockId, int[] nblocks)
	{
		for (int i = 0; i < nblocks.length; i++)			
		{
			int Id = nblocks[i];
			if (blockId == Id)
				return i; 
		}
		return -1;
	}
	
	/**
	 * Checks have exactly one reaching definition and that definition is in the 
	 * set marked "invariant", true returned when above condition was satisfied,
	 * otherwise return false.
	 * 
	 * <p>Due to SSA property was applied, only have exactly one definition is 
	 * implicitly satisfied.
	 * <p>
	 * @param blocks
	 * @param operand
	 * @return
	 */
	private boolean reachDefsIn(int[] blocks, Value operand)	
	{
		if (operand instanceof Instruction)
		{
			Instruction inst = (Instruction)operand;
			BasicBlock defBB = inst.getParent();
			// the block index
			int i = blockIndex(defBB.getID(), blocks);
			if (i < 0)
			{
				return false;
			}
						
			// the instruction index in specified block
			int j = defBB.indexOf(inst);
			return instInvariant[i][j];					
		}
		return false;
	}
	/**
	 * Move all invariant satisfying follow two condition to the preheader block.
	 * <ol>
	 * 	<li>The statement defining invariant must dominates all uses of it in 
	 * 		the loop. 
	 * 	</li>
	 * 	<li>
	 * 		The statement defining invariant must dominates all loop exit blocks.
	 * 	</li>
	 * </ol>
	 * @param loop
	 */
	private void moveInvariant(Loop loop)
	{
		int blockId;
		BasicBlock bb;
		Instruction inst;
		BasicBlock preheader = insertPreheader(loop);		
		for (Pair<Integer, Integer> coor : invarOrder)
		{
			blockId = loop.getBlockId(coor.first);
			bb = IdToBasicBlock[blockId];
			inst = bb.getInst(coor.second);			
			if (domAllExits(bb, loop) && domAllUses(bb, inst.usesList))
			{
				// delete invariant and move loop-invariant to preheader
				inst.eraseFromBasicBlock();
				appendPreheader(inst, preheader);
			}
		}
	}
	/**
	 * Append an instruction have been removed from original basic block
	 * into the last of preheader block.
	 * @param inst
	 * @param preheader
	 */
	private void appendPreheader(Instruction inst, BasicBlock preheader)
	{
		assert inst != null && preheader != null;
		preheader.appendInst(inst);
	}	
	
	/**
	 * Determines if given basic block dominates all exit block of {@code loop}. 
	 * @param bb
	 * @param loop
	 * @return return true if {@code bb} dominates all exit block, otherwise, 
	 * false returned.
	 */
	private boolean domAllExits(BasicBlock bb, Loop loop)
	{
		int[] exitIds = loop.endBlocks();
		BasicBlock exit;
		for (int i : exitIds)
		{
			exit = IdToBasicBlock[i];
			if (!dt.dominates(bb, exit))
				return false;
		}
		return true;
	}
	/**
	 * Checks if a statement defining a invariant in basic block that dominates all
	 * uses of it in this loop.
	 * @param bb
	 * @param uselists
	 * @return
	 */
	private boolean domAllUses(BasicBlock bb, List<Use> uselists)
	{
		BasicBlock useBlock;
		for (Use u : uselists)
		{
			  useBlock = ((Instruction)u.getUser()).getParent();
			  if (!dt.dominates(bb, useBlock))
				  return false;			  
		}				
		return true;
	}
	/**
	 * inserts a pre-header block to the preceded position of loop-header block when no exactly one
	 * predecessor of header. Otherwise, just return it.
	 * @param loop
	 * @return
	 */
	private BasicBlock insertPreheader(Loop loop)
	{
		BasicBlock preheader = loop.getPreheader();
		if (preheader != null)
			return preheader;
		
		BasicBlock header = loop.getHeaderBlock();
		// compute the set of predecessors of the loop that are
		// not in the loop
		ArrayList<BasicBlock> outsideBlocks = new ArrayList<>();
		for (BasicBlock pred : header.getPreds())
		{
			if (!loop.contains(pred))
			{
				outsideBlocks.add(pred);
			}
		}
		
		preheader = splitBlockPredecessor(header, outsideBlocks, ".preheader",loop);
		TTY.println("LICM: creating pre-header %s", preheader.bbName);
		return preheader;
	}
	
	private BasicBlock splitBlockPredecessor(
			BasicBlock header, 
			ArrayList<BasicBlock> outsideBlocks,
			String suffix,
			Loop loop)
	{
		// create a new basic block, insert right before the original block
		ControlFlowGraph cfg = header.getCFG();	
		BasicBlock newBB = cfg.createBasicBlock(header.bbName + suffix);
		for (BasicBlock pred : outsideBlocks)
		{
			// unlink the predecessors with header block
			header.removePredeccessor(pred);
			pred.removeSuccssor(header);
			
			// link predecessors with the header block.
			pred.addSucc(newBB);
			newBB.addPred(pred);
			
			// replace the old branching target of newBB
			pred.getTerminator().replaceTargetWith(header, newBB);
		}
		// the new block unconditionally branches to the header block
		Goto inst = new Goto(header, newBB.bbName + ".goto");
		
        // Insert a new PHI node into NewBB for every PHI node in BB and that new PHI
        // node becomes an incoming value for BB's phi node.  However, if the Preds
        // list is empty, we need to insert dummy entries into the PHI nodes in BB to
        // account for the newly created predecessor.
		if (outsideBlocks.isEmpty())
		{
			// insert dummy values as the incoming value
			for (Instruction i : header)
			{
				if (i instanceof Phi)
				{
					((Phi)i).addIncoming(UndefValue.get(i.kind), newBB);
				}
			}
			return newBB;
		}
		
		// updates the phi node in header whith values coming from newBB
		updatePhiNodes(header, newBB, outsideBlocks, inst, false);
		return newBB;
	}
	
	/**
	 * Update the PHI node in orginBB to include the values coming from newBB. 
	 * @param originBB	
	 * @param newBB
	 * @param preds
	 * @param inst
	 * @param hasLoopExit
	 */
	private void updatePhiNodes(BasicBlock originBB, BasicBlock newBB, 
			ArrayList<BasicBlock> preds, Branch inst, boolean hasLoopExit)
	{
		for (Instruction i : originBB)
		{
			if (!(i instanceof Phi))
				continue;
			
			Phi phi = (Phi)i;
			// check to see if all of the values coming in are same. If so, 
			// we don't need to create a new phi node, unless it's needed for LCSSA.
			Value inVal = null;
			if (!hasLoopExit)
			{
				inVal = phi.getIncomingValueForBlock(preds.get(0));
				for (int j =0, e = phi.getNumberIncomingValues(); j != e; j++)
				{
					if (!preds.contains(phi.getIncomingBlock(j)))
						continue;
					if (inVal == null)
						inVal = phi.getIncomingValue(j);
					else if (inVal != phi.getIncomingValue(j))
					{
						inVal = null;
						break;
					}				
				}
			}
			
			if (inVal != null)
			{
				// If all incoming values for the new PHI would be the same, just don't
				// make a new PHI.  Instead, just remove the incoming values from the old
				// PHI.

				// NOTE! This loop walks backwards for a reason! First off, this minimizes
				// the cost of removal if we end up removing a large number of values, and
				// second off, this ensures that the indices for the incoming values
				// aren't invalidated when we remove one.
				for (int j = phi.getNumberIncomingValues() - 1; j >= 0; j--)
				{
					if (preds.contains(phi.getIncomingBlock(j)))
					{
						phi.removeIncomingValue(j);
					}
				}
				// add an incoming value to the phi node in the loop for the preheader edge.
				phi.addIncoming(inVal, newBB);
				continue;
			}
			
			// If the values coming into the block are not the same, we need a new
		    // PHI.
		    // Create the new PHI node, insert it into NewBB at the end of the block		   
		    Phi newPHI = new Phi(phi.kind, preds.size(), phi.getName() + ".ph");
			newBB.appendInst(newPHI);
			newBB.appendInst(inst);
			
			
		    // NOTE! This loop walks backwards for a reason! First off, this minimizes
		    // the cost of removal if we end up removing a large number of values, and
		    // second off, this ensures that the indices for the incoming values aren't
		    // invalidated when we remove one.
		    for (int j = phi.getNumberIncomingValues() - 1; j >= 0; --j) 
		    {
		    	BasicBlock incomingBB = phi.getIncomingBlock(j);
		      	if (preds.contains(incomingBB)) 
		      	{
		        	Value V = phi.removeIncomingValue(j);
		        	newPHI.addIncoming(V, incomingBB);
		      	}
	    	}
		    phi.addIncoming(newPHI, newBB);
		}
	}

}
