package backend.transform.scalars;

import backend.analysis.*;
import backend.pass.AnalysisUsage;
import backend.pass.LPPassManager;
import backend.pass.LoopPass;
import backend.value.BasicBlock;
import backend.value.Instruction;
import backend.value.Instruction.PhiNode;
import backend.value.Instruction.TerminatorInst;
import backend.value.Loop;
import backend.value.Value;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class LoopDeletion implements LoopPass
{
	/**
	 * A static method served as creating an instance of this class
	 * that operated on program loop.
	 * @return
	 */
	public static LoopDeletion createLoopDeletionPass()
	{
		return new LoopDeletion();
	}

	@Override
	public String getPassName()
	{
		return "Loop deletion optimization pass";
	}

	/**
	 * Add some pre-requisizement pass of this pass into PassManager.
	 *
	 * @param au
	 */
	@Override
	public void getAnalysisUsage(AnalysisUsage au)
	{
		au.addRequired(ScalarEvolution.class);
		au.addRequired(DomTreeInfo.class);
		au.addRequired(LoopInfo.class);
		au.addRequired(LoopSimplify.class);
		au.addRequired(LCSSA.class);
	}

	/**
	 * Checks if there is just on exiting block and the sole exiting block also
	 * dominate the latch block.
	 * @param loop
	 * @param exitingBBs
	 * @return Return {@code true} if the checking condition is satisfied.
	 *         Otherwise {@code false} returned.
	 */
	private boolean isSingleDominateExit(Loop loop,
			ArrayList<BasicBlock> exitingBBs)
	{
		if (exitingBBs.size() > 1)
			return false;

		BasicBlock latch = loop.getLoopLatch();
		if (latch == null)
			return false;
		
		DomTreeInfo dt = (DomTreeInfo) getAnalysisToUpDate(DomTreeInfo.class);
		return dt != null && dt.dominates(exitingBBs.get(0), latch);
	}

	/**
	 * Checks whether this loop is not used any more.
	 * This assumes that we've already checked for unique exit and exiting
	 * blocks, and that the code is in LCSSA form.
	 * @param loop
	 * @param exitingBBs
	 * @param exitBBs
	 * @return Return {@code true} if the loop is useless, otherwise, return
	 * {@code false}.
	 */
	private boolean isLoopDead(Loop loop,
			ArrayList<BasicBlock> exitingBBs,
			ArrayList<BasicBlock> exitBBs)
	{
		BasicBlock exitBlock = exitBBs.get(0);
		BasicBlock exitingBlock = exitingBBs.get(0);

		// Checks if the incoming value of all phi nodes in exit block are
		// loop invariant.
		// Because the code is in LCSSA form, any values used outside of the loop
		// must pass through a PHI in the exit block, meaning that this check is
		// sufficient to guarantee that no loop-variant values are used outside
		// of the loop.
		for (Instruction inst : exitBlock)
		{
			if (!(inst instanceof PhiNode))
				break;
			PhiNode pn = (PhiNode)inst;
			Value incomingVal = pn.getIncomingValueForBlock(exitingBlock);
			if (!loop.isLoopInVariant(incomingVal))
				return false;
		}

		// Make sure that no instructions in the block have potential side-effects.
		// This includes instructions that could write to memory, and loads that are
		// marked volatile.
		for (BasicBlock bb : loop.getBlocks())
		{
			for (Instruction inst : bb)
				if (inst.mayHasSideEffects())
					return false;
		}
		return true;
	}

	@Override
	public boolean runOnLoop(Loop loop, LPPassManager ppm)
	{
		BasicBlock preheader = loop.getLoopPreheader();
		assert preheader != null :"The loop is not canonicalized yet?";

		ArrayList<BasicBlock> exitingBlocks = loop.getExitingBlocks();
		ArrayList<BasicBlock> exitBlocks = loop.getUniqueExitBlocks();

		// We cannot handle the loop that have at least one exit block.
		if (exitBlocks.size() > 1)
			return false;

		if (!isSingleDominateExit(loop, exitingBlocks))
			return false;

		// Finally, we should to check if this loop is dead.
		boolean changed = false;
		if (!isLoopDead(loop, exitingBlocks, exitBlocks))
			return changed;

		// We just process the loop when the back edge taken count is not
		// SCEVCouldNotCompute.
		ScalarEvolution se = (ScalarEvolution) getAnalysisToUpDate(ScalarEvolution.class);
		SCEV itrCnt = se.getIterationCount(loop);
		if (itrCnt instanceof SCEVCouldNotCompute)
			return changed;

		// Now we know the removal is safe, removing the loop by changing
		// the branch from the preheader block to go to the exit block.
		BasicBlock exitBB = exitBlocks.get(0);
		BasicBlock exitingBB = exitingBlocks.get(0);

		// Inform the se modular that this loop is deleted.
		se.forgetLoopBackendTakenCount(loop);

		// Connect the preheader directly to the exit block.
		TerminatorInst ti = preheader.getTerminator();
		if (ti == null)
			return changed;
		ti.replaceUsesOfWith(loop.getHeaderBlock(), exitBB);

		// Rewrite all of the phi node in the exit block.
		// replace the incoming value from exiting parent to the preheader block.
		for (Instruction inst : exitBB)
		{
			if (!(inst instanceof PhiNode))
				break;
			PhiNode pn = (PhiNode)inst;
			pn.replaceUsesOfWith(exitingBB, preheader);
		}

		// Update the dominator information and dominator frontier if available.
		DomTreeInfo dt = (DomTreeInfo) getAnalysisToUpDate(DomTreeInfo.class);
		DominanceFrontier df = (DominanceFrontier) getAnalysisToUpDate(DominanceFrontier.class);
		Iterator<BasicBlock> itr = loop.getBlocks().iterator();
		while (itr.hasNext())
		{
			BasicBlock curBB = itr.next();
			// Change the immediate dominator of the children node of the
			// current BB to the preheader.
			dt.getNode(curBB).getChildren().forEach(childNode->
			{
				dt.changeIDom(childNode, dt.getNode(preheader));
			});

			dt.eraseNode(curBB);
			if (df != null)
				df.removeBlock(curBB);

			curBB.dropAllReferences();
		}

		// Erase the instruction and the blocks within this loop.
		// Remove the loop info.
		LoopInfo li = (LoopInfo) getAnalysisToUpDate(LoopInfo.class);
		for (BasicBlock bb : loop.getBlocks())
		{
			bb.eraseFromParent();
			li.removeBlock(bb);
		}

		// TODO inform the LoopPassManager that this loop is deleted.
		ppm.deleteLoopFromQueue(loop);
		return true;
	}
}
