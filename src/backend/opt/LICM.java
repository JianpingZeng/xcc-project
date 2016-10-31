package backend.opt;

import backend.hir.*;
import backend.hir.BasicBlock;
import backend.value.Function;
import backend.value.Instruction;
import backend.value.Instruction.Op1;
import backend.value.Instruction.Op2;
import backend.value.Instruction.PhiNode;
import backend.value.Use;
import backend.value.Value;
import backend.value.Value.Constant;
import backend.value.Value.UndefValue;

import java.util.ArrayList;
import java.util.List;

import tools.Pair;
import tools.TTY;

/** 
 * </p>
 * This class performs loop invariant code motion, attempting to remove
 * as much code from the body of a loop as possible. It does this by either
 * hoisting code into the pre-header block, or by sinking code to the exit 
 * block if it is safe. Currently, this class does not use alias analysis
 * so that the all backend.opt operated upon memory access are excluded.
 * </p>
 * 
 * <p>This pass expected to run after {@linkplain LoopInversion Loop Inversion}
 * and {@linkplain LoopAnalysis pass}. 
 * performed.
 * </p>
 * 
 * <p>This file is a member of <a href={@docRoot/opt}>Machine Independence
 * Optimization</a>
 * </p>.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class LICM
{	
	private final Loop[] loops;
	private boolean[] marked;
	private Loop[] loopIdToLoops;
	private boolean[][] instInvariant;
	private ArrayList<Pair<Integer, Integer>> invarOrder;
	private DominatorTree dt;
	/**
	 * Constructor.
	 * @param hir	The control flow graph of function being compiled.
	 */
	public LICM(Function function)
	{
		dt = new DominatorTree(function);
		dt.recalculate();
		loops = function.getLoops();
		assert loops != null: "must performed after loop analysis pass";
		
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
	 * Starting to perform backend.opt of loop invariant code motion.
	 */
	public void runOnLoop()
	{
		// go through all loop from nested depth-most loop to outerLoop
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
					loop = loop.outerLoop;
				}while (loop != null);
			}
		}
	}	
	
	private void markInvariant(Loop loop)
	{
		// initialize some necessarily data structure
		int nblocks = loop.getNumOfBlocks();
		// represents which instruction is invariant
		instInvariant = new boolean[nblocks][];		
		invarOrder.clear();
		
		for (int i = 0; i < nblocks; i++)
		{
			BasicBlock bb = loop.getBlock(i);
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
			for (int i = 0; i < loop.getNumOfBlocks(); i++)
			{
				BasicBlock bb = loop.getBlock(i);
				changed |= markBlock(loop.blocks, loop.getHeaderBlock(), bb, i);
			}
        } while (changed);
	}
	
	private boolean markBlock(List<BasicBlock> blocks, BasicBlock entry, BasicBlock bb, int i)
	{
		boolean changed = false;
		Value inst = null;
		for (int j = 0; j < bb.size(); j++)
		{
			// check whether each instruction in this block has loop invariant
			// reservedOperands and appropriate reaching definition; if so, mark it as
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
	 * <p>ReturnInst true if it is out of Loop, otherwise false returned.</p>
	 * @param blocks
	 * @param operand
	 * @return
	 */
	private boolean reachDefsOut(List<BasicBlock> blocks, Value operand)
	{
		if (operand instanceof Instruction)
		{
			Instruction inst = (Instruction)operand;
			BasicBlock bb = inst.getParent();
            return blockIndex(bb.getID(), blocks) < 0;
		}
		return false;
	}
	/**
	 * Obtains the index by which the specified block will be indexed. 
	 * @param blockId	The id of specified basic block.
	 * @param nblocks	The array of block id.
	 * @return IfStmt there no block with specified blockId existed in nblocks, return -1
	 * , otherwise, return its index.
	 */
	private int blockIndex(int blockId, List<BasicBlock> blocks)
	{
		for (int i = 0; i < blocks.size(); i++)			
		{
			int Id = blocks.get(i).getID();
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
	private boolean reachDefsIn(List<BasicBlock> blocks, Value operand)
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
		BasicBlock bb;
		Value inst;
		BasicBlock preheader = insertPreheader(loop);
		for (Pair<Integer, Integer> coor : invarOrder)
		{
			bb = loop.getBlock(coor.first);
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
	private void appendPreheader(Value inst, BasicBlock preheader)
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
		for (BasicBlock exitBB : loop.exitBlocks())
		{
			if (!dt.dominates(bb, exitBB))
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
			for (Value i : header)
			{
				if (i instanceof PhiNode)
				{
					((PhiNode)i).addIncoming(UndefValue.get(i.kind), newBB);
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
                                ArrayList<BasicBlock> preds, Instruction.BranchInst inst, boolean hasLoopExit)
	{
		for (Value i : originBB)
		{
			if (!(i instanceof PhiNode))
				continue;
			
			Instruction.PhiNode phiNode = (PhiNode)i;
			// check to see if all of the values coming in are same. IfStmt so,
			// we don't need to create a new phiNode node, unless it's needed for LCSSA.
			Value inVal = null;
			if (!hasLoopExit)
			{
				inVal = phiNode.getIncomingValueForBlock(preds.get(0));
				for (int j = 0, e = phiNode.getNumberIncomingValues(); j != e; j++)
				{
					if (!preds.contains(phiNode.getIncomingBlock(j)))
						continue;
					if (inVal == null)
						inVal = phiNode.getIncomingValue(j);
					else if (inVal != phiNode.getIncomingValue(j))
					{
						inVal = null;
						break;
					}				
				}
			}
			
			if (inVal != null)
			{
				// IfStmt all incoming values for the new PHI would be the same, just don't
				// make a new PHI.  Instead, just remove the incoming values from the old
				// PHI.

				// NOTE! This loop walks backwards for a reason! First off, this minimizes
				// the cost of removal if we end up removing a large number of values, and
				// second off, this ensures that the indices for the incoming values
				// aren't invalidated when we remove one.
				for (int j = phiNode.getNumberIncomingValues() - 1; j >= 0; j--)
				{
					if (preds.contains(phiNode.getIncomingBlock(j)))
					{
						phiNode.removeIncomingValue(j);
					}
				}
				// add an incoming value to the phiNode node in the loop for the preheader edge.
				phiNode.addIncoming(inVal, newBB);
				continue;
			}
			
			// IfStmt the values coming into the block are not the same, we need a new
		    // PHI.
		    // Create the new PHI node, insert it into NewBB at the end of the block		   
		    PhiNode newPHINode = new PhiNode(
                    phiNode.kind, preds.size(), phiNode.name() + ".ph");
			newBB.appendInst(newPHINode);
			newBB.appendInst(inst);
			
			
		    // NOTE! This loop walks backwards for a reason! First off, this minimizes
		    // the cost of removal if we end up removing a large number of values, and
		    // second off, this ensures that the indices for the incoming values aren't
		    // invalidated when we remove one.
		    for (int j = phiNode.getNumberIncomingValues() - 1; j >= 0; --j)
		    {
		    	BasicBlock incomingBB = phiNode.getIncomingBlock(j);
		      	if (preds.contains(incomingBB)) 
		      	{
		        	Value V = phiNode.removeIncomingValue(j);
		        	newPHINode.addIncoming(V, incomingBB);
		      	}
	    	}
		    phiNode.addIncoming(newPHINode, newBB);
		}
	}
}
