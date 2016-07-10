package optimization; 

import java.util.ArrayList;

import utils.TTY;
import hir.BasicBlock;
import hir.ControlFlowGraph;
import hir.Method;
import hir.Value;
import hir.Instruction.Branch;
import hir.Instruction.Goto;
import hir.Instruction.Phi;
import hir.Value.UndefValue;


/** 
 * This is a pass which responsible for performing simplification of loop contained 
 * in specified {@linkplain Method function}.
 * 
 * <b>Note that</b> only two kinds of optimization yet have been implemented in here,
 * <b>Loop inversion(also called of <a href ="http://llvm.org/docs/doxygen/html/LoopSimplify_8cpp_source.html">
 * Loop rotation</a>)</b> and <b>Insertion of pre-header</b>.
 * 
 * @author Xlous.zeng
 * @version 0.1
 */
public final class LoopSimplify
{
	public void runOnFunction(Method m)
	{
		assert m != null && m.numLoops() > 0 
				: "it is no needed for there no loops in function";
		
		for (Loop loop : m.getLoops())
		{
			insertPreheader(loop);
		}
	}
	/**
	 * inserts a pre-header block to the preceded position of loop-header block when no exactly one
	 * predecessor of header. Otherwise, just return it.
	 * @param loop
	 * @return
	 */
	private void insertPreheader(Loop loop)
	{
		BasicBlock preheader = loop.getPreheader();
		// if there is a preheader, then immediately return
		if (preheader != null)
			return;
		
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
		return;
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
		for (Value i : originBB)
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
