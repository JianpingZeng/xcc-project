package backend.opt;

import backend.hir.BasicBlock;
import backend.value.Function;
import backend.value.Instruction;
import backend.value.Value;

/** 
 * <p>
 * This pass was designed to reduce the number of branch instruction by
 * a simply transformation: a {@code while} loop is converted to a 
 * {@code do-while} loop wrapped by an {@code if} statement as shown follow:
 * </p>
 * <pre>
 * void pre_inversion()          void post_inversion()
 * {                             {
 * 		while (condition)         	if (condition)
 * 		{                           {
 * 			// loop body               	do{
 * 		}                         		// loop body   
 * }                                   	}while(condition)
 * 1.a).before loop inversion       } 
 *    							}	
 *    							1.b).after inversion
 * </pre>                                
 * @author Xlous.zeng
 * @version 0.1
 */
public final class LoopInversion
{
	private Loop[] loops;
	
	public LoopInversion(Function function)
	{
		this.loops = function.getLoops();
		assert loops!= null : "must performed after loop analysis pass";
	}
	
	/**
	 * Run loop rotation pass over given many loops.
	 * Note that: before this pass performed, it is must to make sure that 
	 * the Loop has exactly one entry block and exit block. 
	 */
	public void runOnLoops()
	{
		for (Loop L : loops)
		{
			rotateLoop(L);
		}
	}
	
	private void rotateLoop(Loop loop)
	{
		if (!isNeededRotation(loop))
			return;
		
		BasicBlock header = loop.getHeaderBlock();
		Instruction.BranchInst br = (Instruction.BranchInst)header.lastInst();
		
		BasicBlock newHeader = header.getCFG().createBasicBlock(header.bbName + ".newheader");
		
		// remove all instruction, except the last branch instruction, and append it into
		// newHeader in original order
		for (int i = 0; i < header.size() - 1; i++)
		{
			Value inst = header.getInst(i);
			inst.eraseFromBasicBlock();
			newHeader.appendInst(inst);
		}
		
		for (BasicBlock sux : header.getSuccs())
		{
			if (loop.contains(sux))
			{
				header.removeSuccssor(sux);
				newHeader.addSucc(sux);
				br.replaceTargetWith(sux, newHeader);				
			}
		}
		
		header.addSucc(newHeader);
		newHeader.addPred(header);
		
		for (BasicBlock pred : header.getPreds())
		{
			if (loop.contains(pred))
			{
				header.removePredecessor(pred);
				pred.removeSuccssor(header);
				
				if (pred.lastInst() instanceof Instruction.BranchInst)
				{
					Instruction.BranchInst br2 = (Instruction.BranchInst) pred.lastInst();
					br2.replaceTargetWith(header, newHeader);
				}
			}
		}
		
		// remove original header block, then insert a new header in the first position
		loop.removeBlock(header);
		loop.addFirstBlock(newHeader);
		header.setOutLoop(null);
		newHeader.setOutLoop(loop);
		
		BasicBlock exitBlock = header.getCFG().createBasicBlock(header.bbName + ".newExit");
		
		// append a new branch in new exit block
		Instruction.BranchInst newBR = br.clone();
		
		// replace the true TargetData of branch with new header
		newBR.replaceTargetWith(header, newHeader);		
		exitBlock.appendInst(newBR);
		
		for (BasicBlock bb : loop.exitBlocks())
		{
			if (bb.lastInst() instanceof Instruction.BranchInst)
			{
				Instruction.BranchInst br2 = (Instruction.BranchInst)bb.lastInst();
				br2.replaceTargetWith(loop.getFollowBlock(), exitBlock);
				bb.removeSuccssor(loop.getFollowBlock());
				loop.getFollowBlock().removePredecessor(bb);
				
				bb.addSucc(exitBlock);
				exitBlock.addPred(bb);
				
				loop.removeExitBlock(bb);
			}
		}
		
		loop.addExitBlock(exitBlock);
		
		// true TargetData
		exitBlock.addSucc(newHeader);
		// false TargetData
		exitBlock.addSucc(loop.getFollowBlock());
		loop.addBlock(exitBlock);
		
		for (BasicBlock pred : newHeader.getPreds())
		{
			if (!loop.contains(pred) || loop.isExitBlock(pred))
				continue;
			
			if (pred.lastInst() instanceof Instruction.BranchInst)
			{
				Instruction.BranchInst br2 = (Instruction.BranchInst)pred.lastInst();
				br2.replaceTargetWith(newHeader, exitBlock);
				pred.removeSuccssor(newHeader);
				newHeader.removePredecessor(pred);
				
				pred.addSucc(exitBlock);
				exitBlock.addPred(pred);
			}
		}		
	}
	/**
	 * Check whether it is needed to rotate specified loop by seeing the last
	 * instruction in header block. 
	 * @param loop
	 * @return
	 */
	private boolean isNeededRotation(Loop loop)
	{
		Value lastInst = loop.getHeaderBlock().lastInst();
		return (lastInst instanceof Instruction.BranchInst);
	}
}
