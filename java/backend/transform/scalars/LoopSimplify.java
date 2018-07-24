package backend.transform.scalars;

import backend.analysis.DomTree;
import backend.analysis.DomTreeNodeBase;
import backend.analysis.DominanceFrontier;
import backend.analysis.LoopInfo;
import backend.analysis.aa.AliasAnalysis;
import backend.pass.AnalysisResolver;
import backend.pass.AnalysisUsage;
import backend.pass.FunctionPass;
import backend.utils.PredIterator;
import backend.utils.SuccIterator;
import backend.value.*;
import backend.value.Instruction.*;
import tools.OutParamWrapper;
import tools.Util;

import java.util.*;

import static backend.support.BasicBlockUtil.splitBlockPredecessors;

/**
 * This is a pass which responsible for performing simplification of loop contained 
 * in specified {@linkplain Function function}.
 * 
 * <b>Note that</b> only two kinds of backend.transform yet have been implemented in here,
 * <b>Loop inversion(also called of <a href ="http://llvm.org/docs/doxygen/html/LoopSimplify_8cpp_source.html">
 * Loop rotation</a>)</b> and <b>Insertion of pre-header</b>.
 * 
 * @author Xlous.zeng
 * @version 0.1
 */
public final class LoopSimplify implements FunctionPass
{
	private LoopInfo li;
	private DomTree dt;
	private AliasAnalysis aliasAnalysis;

	private AnalysisResolver resolver;

	@Override
	public void setAnalysisResolver(AnalysisResolver resolver)
	{
		this.resolver = resolver;
	}

	@Override
	public AnalysisResolver getAnalysisResolver()
	{
		return resolver;
	}
	@Override
	public void getAnalysisUsage(AnalysisUsage au)
	{
		au.addRequired(LoopInfo.class);
		au.addRequired(DomTree.class);
		au.addPreserved(AliasAnalysis.class);
		au.addPreserved(BreakCriticalEdge.class);
	}
	@Override
	public boolean runOnFunction(Function f)
	{
		li = (LoopInfo) getAnalysisToUpDate(LoopInfo.class);
		dt = (DomTree) getAnalysisToUpDate(DomTree.class);
		aliasAnalysis = (AliasAnalysis) getAnalysisToUpDate(AliasAnalysis.class);
		boolean changed = false;
		for (Loop loop : li.getTopLevelLoop())
			 changed |= processLoop(loop);
		return changed;
	}

	@Override
	public String getPassName()
	{
		return "Loop simplification pass";
	}

	/**
	 * Walk the loop structure in depth first order, ensuring that
	 * all loops have preheaders.
	 * @param loop
	 * @return
	 */
	private boolean processLoop(Loop loop)
	{
		boolean changed = false;
		ArrayList<BasicBlock> exitBlocks;
		BasicBlock preHeader;

		while (true)
		{
			for (Loop sub : loop.getSubLoops())
				changed |= processLoop(sub);

			Util.assertion(loop.getBlocks().get(0) == loop					.getHeaderBlock(),  "Header isn't in the first");

			preHeader = loop.getLoopPreheader();
			if (preHeader == null)
			{
				preHeader = insertPreHeaderForLoop(loop);
				changed = true;
			}

			// Next, check to make sure that all exit nodes of the loop only have
			// predecessors that are inside of the loop.  This check guarantees that the
			// loop preheader/header will dominate the exit blocks.  If the exit block has
			// predecessors from outside of the loop, split the edge now.
			exitBlocks = loop.getExitingBlocks();

			for (BasicBlock exitBB : exitBlocks)
			{
				for (PredIterator<BasicBlock> itr = exitBB.predIterator(); itr
						.hasNext(); )
				{
					if (!loop.contains(itr.next()))
					{
						rewriteLoopExitBlock(loop, exitBB);
						changed = true;
						break;
					}
				}
			}

			// If the header has more than two predecessors at this point (from the
			// preheader and from multiple backedges), we must adjust the loop.
			int numBackEdges = loop.getNumBackEdges();
			if (numBackEdges != 1)
			{
				if (numBackEdges < 8)
				{
					Loop nestedLoop = separateNestedLoop(loop);
					if (nestedLoop != null)
					{
						processLoop(nestedLoop);
						changed = true;
						continue;
					}
				}

				insertUniqueBackedgeBlock(loop, preHeader);
				changed = true;
			}
			break;
		}

		// Scan over the PHI nodes in the loop header.  Since they now have only two
		// incoming values (the loop is canonicalized), we may have simplified the PHI
		// down to 'X = phi [X, Y]', which should be replaced with 'Y'.
		PhiNode phiNode;
		for (int i = 0, e = loop.getHeaderBlock().size(); i < e; i++)
		{
			Instruction inst = loop.getLoopPreheader().getInstAt(i);
			if (inst instanceof PhiNode)
			{
				phiNode = (PhiNode)inst;
				Value v;
				if ((v = phiNode.hasConstantValue()) != null)
				{
					phiNode.replaceAllUsesWith(v);
					phiNode.eraseFromParent();
				}
			}
			else
				break;
		}

		// If this loop has muliple exits and the exits all go to the same
		// block, attempt to merge the exits. This helps several passes, such
		// as LoopRotation, which do not support loops with multiple exits.
		// SimplifyCFG also does this (and this code uses the same utility
		// function), however this code is loop-aware, where SimplifyCFG is
		// not. That gives it the advantage of being able to hoist
		// loop-invariant instructions out of the way to open up more
		// opportunities, and the disadvantage of having the responsibility
		// to preserve dominator information.
		if (exitBlocks.size() > 1 && loop.getExitingBlock() != null)
		{
			exitBlocks = loop.getExitingBlocks();
			for (Iterator<BasicBlock> itr = exitBlocks.iterator(); itr.hasNext();)
			{
				BasicBlock exitingBB = itr.next();
				if (exitingBB.getSinglePredecessor() == null)
					continue;

				Instruction instr = exitingBB.getTerminator();
				BranchInst bi;
				if (!(instr instanceof BranchInst) || !(bi = (BranchInst)instr).isConditional())
					continue;

				Value val = bi.getCondition();
				CmpInst ci;
				if (!(val instanceof CmpInst) || (ci = (CmpInst)val).getParent() != exitingBB)
					continue;

				boolean allInvariant = true;
				for (Instruction inst : exitingBB)
				{
					if (inst == ci)
						continue;
					OutParamWrapper<Boolean> x = new OutParamWrapper<>(changed);
					if (!loop.makeLoopInvariant(inst, x, preHeader.getTerminator()))
					{
						allInvariant = false;
						break;
					}
					changed = x.get();
				}
				if (!allInvariant) continue;

				// The block has now been cleared of all instructions except for
				// a comparison and a conditional branch. SimplifyCFG may be able
				// to fold it now.
				if (!foldBranchToCommonDest(bi))
					continue;

				// Success. The block is now dead, so remove it from the loop,
				// update the dominator tree and dominance frontier, and delete it.
				Util.assertion( !itr.hasNext());
				changed = true;
				li.removeBlock(exitingBB);

				DominanceFrontier df = (DominanceFrontier) getAnalysisToUpDate(DominanceFrontier.class);
				DomTreeNodeBase<BasicBlock> node = dt.getNode(exitingBB);
				ArrayList<DomTreeNodeBase<BasicBlock>> children = node.getChildren();

				children.forEach(child ->
				{
					dt.changeIDom(child, node.getIDom());
					if(df != null)
						df.changeIDom(child.getBlock(), node.getIDom().getBlock(),
								dt);
				});

				dt.eraseNode(exitingBB);
				if (df != null)
					df.removeBlock(exitingBB);

				bi.getSuccessor(0).removePredecessor(exitingBB);
				bi.getSuccessor(1).removePredecessor(exitingBB);
				exitingBB.eraseFromParent();
			}
		}
		return changed;
	}

	/**
	 * If this basic block is only
	 * @param bi
	 * @return
	 */
	private boolean foldBranchToCommonDest(BranchInst bi)
	{
		BasicBlock bb = bi.getParent();
		Value condVal = bi.getCondition();
		if (!(condVal instanceof Instruction))
			return false;
		Instruction cond = (Instruction)condVal;
		// Only allow this if the condition is a simple instruction that can be
		// executed unconditionally.  It must be in the same block as the branch, and
		// must be at the front of the block.
		if ((!(cond instanceof CmpInst) && !(cond instanceof BinaryOps))
				|| cond.getParent() != bb || !cond.hasOneUses())
			return false;

		BasicBlock trueDest = bi.getSuccessor(0);
		BasicBlock falseDest = bi.getSuccessor(1);
		if (trueDest == bb || falseDest == bb)
			return false;

		for (PredIterator<BasicBlock> itr = bb.predIterator(); itr.hasNext();)
		{
			BasicBlock predBlock = itr.next();
			TerminatorInst ti = predBlock.getTerminator();
			BranchInst pbi;
			if (!(ti instanceof BranchInst) || (pbi = (BranchInst)ti).isUnconditional()
					|| !safeToMergeTerminator(bi, pbi))
				continue;

			Operator opcode;
			boolean invertPredCond = false;
			if (pbi.getSuccessor(0) == trueDest)
				opcode = Operator.Or;
			else if (pbi.getSuccessor(1) == falseDest)
				opcode = Operator.And;
			else if (pbi.getSuccessor(0) == falseDest)
			{
				opcode = Operator.And;
				invertPredCond = true;
			}
			else if (pbi.getSuccessor(1) == trueDest)
			{
				opcode = Operator.Or;
				invertPredCond = true;
			}
			else
				continue;

			System.err.println("Folding branch to common dest:");
			System.err.printf("%s %s\n", pbi.getName(), bb.getName());

			// If we need to invert the condition in the pred block to match, do so now.
			if (invertPredCond)
			{
				Value newCond = BinaryOps.createNot(pbi.getCondition(),
						pbi.getCondition().getName()+".not", pbi);
				pbi.setCondition(newCond);
				BasicBlock oldTrue = pbi.getSuccessor(0);
				BasicBlock oldFalse = pbi.getSuccessor(1);
				pbi.setSuccessor(0, oldFalse);
				pbi.setSuccessor(1, oldTrue);
			}

			// Clone Cond into the predecessor basic block, and or/and the
			// two conditions together.
			Instruction newInst = cond.clone();
			predBlock.insertBefore(newInst, predBlock.getInstList().indexOf(pbi));
			newInst.setName(cond.getName());
			cond.setName(newInst.getName()+".old");

			Value newCond = BinaryOps.create(opcode, pbi.getCondition(),
					newInst, "or.cond", pbi);
			pbi.setCondition(newCond);
			if (pbi.getSuccessor(0) == bb)
			{
				addPredecessorToBlock(trueDest, predBlock, bb);
				pbi.setSuccessor(0, trueDest);
			}
			if (pbi.getSuccessor(1) == bb)
			{
				addPredecessorToBlock(falseDest, predBlock, bb);
				pbi.setSuccessor(1, falseDest);
			}
			return true;
		}
		return false;
	}

	/**
	 * Returns true if it is safe to merge the two terminator instructions together.
	 * @param ti1
	 * @param ti2
	 * @return
	 */
	public static boolean safeToMergeTerminator(TerminatorInst ti1, TerminatorInst ti2)
	{
		if (ti1 == ti2) return false;

		BasicBlock ti1BB = ti1.getParent();
		BasicBlock ti2BB = ti2.getParent();
		HashSet<BasicBlock> succs1 = new HashSet<>();
		for (SuccIterator itr = ti1BB.succIterator(); itr.hasNext();)
			succs1.add(itr.next());

		for (SuccIterator succItr2 = ti2BB.succIterator(); succItr2.hasNext();)
		{
			BasicBlock bb = succItr2.next();
			if (succs1.contains(bb))
			{
				for (Instruction inst : bb)
				{
					if (!(inst instanceof PhiNode))
						break;

					PhiNode pn = (PhiNode)inst;
					if (pn.getIncomingValueForBlock(ti1BB) !=
							pn.getIncomingValueForBlock(ti2BB))
						return false;
				}
			}
		}
		return true;
	}

	/**
	 * Update PHI nodes in Succ to indicate that there will
	 * now be entries in it from the 'NewPred' block.  The values that will be
	 * flowing into the PHI nodes will be the same as those coming in from
	 * ExistPred, an existing predecessor of Succ.
	 * @param succ
	 * @param newPred
	 * @param exitPred
	 */
	private void addPredecessorToBlock(BasicBlock succ, BasicBlock newPred, BasicBlock exitPred)
	{
		Util.assertion(exitPred.hasSuccessor(succ), "ExitPred is not a predecessor of succ");
		if (!(succ.getFirstInst() instanceof PhiNode))
			return;   // early terminates if there is no PHI node in succ.

		PhiNode pn;
		for (Instruction inst : succ)
		{
			if (!(inst instanceof PhiNode))
				break;
			pn = (PhiNode)inst;
			pn.addIncoming(pn.getIncomingValueForBlock(exitPred), newPred);
		}
	}

	/**
	 * This method is called when the specified loop
	 * has more than one backedge in it.  If this occurs, revector all of these
	 * backedges to target a new basic block and have that block branch to the loop
	 * header.  This ensures that loops have exactly one backedge.
	 * @param loop
	 * @param preHeader
	 */
	private void insertUniqueBackedgeBlock(Loop loop, BasicBlock preHeader)
	{
		Util.assertion(loop.getNumBackEdges() > 1, "Must have at least two backedge!");

		BasicBlock header = loop.getHeaderBlock();
		Function f = header.getParent();

		ArrayList<BasicBlock> backedgeBlocks = new ArrayList<>();
		for (PredIterator<BasicBlock> itr = header.predIterator(); itr.hasNext();)
		{
			BasicBlock pred = itr.next();
			if (pred != preHeader)
				backedgeBlocks.add(pred);
		}

		// create and insert the new backedge block.
		BasicBlock beBlock = BasicBlock.createBasicBlock(header.getName()+".backedge",
				f);
		BranchInst beTerminator = new BranchInst(header, beBlock);

		// Move the new backedge blcok to right after hte last backedge block.

		// Now that the block has been inserted into the function, create PHI nodes in
		// the backedge block which correspond to any PHI nodes in the header block.
		for (Instruction inst : header)
		{
			if (!(inst instanceof PhiNode))
				break;

			PhiNode pn = (PhiNode)inst;
			PhiNode newPN = new PhiNode(pn.getType(), backedgeBlocks.size(),
					pn.getName()+".be",
					beTerminator);
			// Loop over the PHI node, moving all entries except the one for the
			// preheader over to the new PHI node.
			int preheadIdx = ~0;
			boolean hasUniqueIncomingValue = true;
			Value uniqueVal = null;
			for (int i = 0, e = pn.getNumberIncomingValues(); i < e; i++)
			{
				BasicBlock bb = pn.getIncomingBlock(i);
				Value incomingVal = pn.getIncomingValue(i);
				if (bb == preHeader)
					preheadIdx = i;
				else
				{
					newPN.addIncoming(incomingVal, bb);
					if (hasUniqueIncomingValue)
					{
						if (uniqueVal == null)
							uniqueVal = incomingVal;
						else if (uniqueVal != incomingVal)
							hasUniqueIncomingValue = false;

					}
				}
			}

			// Delete all of the incoming values from the old PN except the preheader's
			Util.assertion(preheadIdx != ~0, "PHI has no preheader entry?");
			if (preheadIdx != 0)
			{
				pn.setIncomingValue(0, pn.getIncomingValue(preheadIdx));
				pn.setIncomingBlock(0, pn.getIncomingBlock(preheadIdx));
			}
			for (int i = 0, e = pn.getNumberIncomingValues(); i < e; i++)
				pn.removeIncomingValue(e - i, false);

			// Finally, add the newly constructed PHI node as the entry for the BEBlock.
			pn.addIncoming(newPN, beBlock);

			if (hasUniqueIncomingValue)
			{
				newPN.replaceAllUsesWith(uniqueVal);
				beBlock.getInstList().remove(newPN);
			}
		}

		for (int i = 0, e = backedgeBlocks.size(); i < e; i++)
		{
			TerminatorInst ti = backedgeBlocks.get(i).getTerminator();
			for (int op = 0, sz = ti.getNumOfSuccessors(); op < sz; op++)
				if (ti.getSuccessor(op) == header)
					ti.setSuccessor(op, beBlock);
		}

		// Update Loop Information - we know that this block is now in the current
		// loop and all parent loops.
		loop.addBasicBlockIntoLoop(beBlock, li);

		// Update dominator information.
		dt.splitBlock(beBlock);
		DominanceFrontier df = (DominanceFrontier) getAnalysisToUpDate(DominanceFrontier.class);
		if (df != null)
			df.splitBlock(beBlock);
	}

	/**
	 * If this loop has multiple backedges, try to pull one of
	 * them out into a nested loop.  This is important for code that looks like
	 * this:
	 * <pre>
	 *  Loop:
	 *     ...
	 *     br cond, Loop, Next
	 *     ...
	 *     br cond2, Loop, Out
	 * </pre>
	 * To identify this common case, we lookup at the PHI nodes in the header of the
	 * loop.  PHI nodes with unchanging values on one backedge correspond to values
	 * that change in the "outer" loop, but not in the "inner" loop.
	 *
	 * If we are able to separate out a loop, return the new outer loop that was
	 * created.
	 * @param loop
	 * @return
	 */
	private Loop separateNestedLoop(Loop loop)
	{
		PhiNode pn = findPhiToPartitionLoop(loop, dt, aliasAnalysis);
		if (pn == null) return null;  // No known way to partition.

		// Pull out all predecessors that have varying values in the loop.  This
		// handles the case when a PHI node has multiple instances of itself as
		// arguments.
		ArrayList<BasicBlock> outerLoopPreds = new ArrayList<>();
		for (int i = 0, e = pn.getNumberIncomingValues(); i < e; i++)
		{
			BasicBlock incomingBB = pn.getIncomingBlock(i);
			if (pn.getIncomingValue(i) != pn || !loop.contains(incomingBB))
				outerLoopPreds.add(incomingBB);
		}

		BasicBlock header = loop.getHeaderBlock();
		BasicBlock newBB = splitBlockPredecessors(header, outerLoopPreds, ".out", this);

		// Make sure that NewBB is put someplace intelligent, which doesn't mess up
		// code layout too horribly.
		placeSplitBlockCarefully(newBB, outerLoopPreds, loop);

		Loop newOuter = new Loop();

		// Change the parent loop to use the outer loop as its child now.
		Loop parent;
		if ((parent = loop.getParentLoop()) != null)
			parent.replaceChildLoopWith(loop, newOuter);
		else
			li.replaceTopLevelLoop(loop, newOuter);

		// This block is going to be out new header block, add it into this
		// loop and all parent loops.
		newOuter.addBasicBlockIntoLoop(newBB, li);

		// loop is now a subloop of outer loop.
		newOuter.addChildLoop(loop);

		for (BasicBlock bb : loop.getBlocks())
			newOuter.addBlockEntry(bb);

		// Determine which blocks should stay in loop and which should be moved
		// out of the outer loop.
		HashSet<BasicBlock> blocksInLoop = new HashSet<>();
		for (PredIterator<BasicBlock> itr = header.predIterator(); itr.hasNext();)
		{
			BasicBlock pred = itr.next();
			if (dt.dominates(header, pred))
				addBlockAndPredsToSet(pred, header, blocksInLoop);
		}

		// Scan all of the loop children of loop, moving them to outer loop.
		ArrayList<Loop> subLoops = loop.getSubLoops();
		for (int i = 0, e = subLoops.size(); i < e;)
		{
			if (blocksInLoop.contains(subLoops.get(i).getHeaderBlock()))
				i++;
			else
				newOuter.addChildLoop(loop.removeChildLoop(i));
		}

		// Now that we know which blocks are in L and which need to be moved to
		// OuterLoop, move any blocks that need it.
		for (int i = 0, e = loop.getBlocks().size(); i < e; i++)
		{
			BasicBlock bb = loop.getBlock(i);
			if (!blocksInLoop.contains(bb))
			{
				// Move this block to the parent, updating the exit blocks set.
				loop.removeBlockFromLoop(bb);
				if (li.getLoopFor(bb) == loop)
					li.changeLoopFor(bb, newOuter);
				--i;
			}
		}
		return newOuter;
	}

	/**
	 * The first part of loop-nestification is to find a PHI node that tells us
	 * how to partition the loops.
	 * @param loop
	 * @param dt
	 * @param aliasAnalysis
	 * @return
	 */
	private PhiNode findPhiToPartitionLoop(Loop loop,
			DomTree dt,
			AliasAnalysis aliasAnalysis)
	{
		for (Instruction inst : loop.getHeaderBlock().getInstList())
		{
			if (!(inst instanceof PhiNode))
				break;
			PhiNode pn = (PhiNode)inst;
			Value val;
			if ((val = pn.hasConstantValue()) != null)
			{
				if (!(val instanceof Instruction) || dt.dominates((Instruction)val, pn))
				{
					// This is a degenerate PHI already, don't modify it.
					pn.replaceAllUsesWith(val);
					//if (aliasAnalysis != null) //TODO
						//aliasAnalysis.deleteValue(pn);
					pn.eraseFromParent();
					continue;
				}

				// Scan this phi node to lookup for a use of the PHI node itself.
				for (int i = 0, e = pn.getNumberIncomingValues(); i < e; i++)
				{
					if (pn.getIncomingValue(i) == pn &&
							loop.contains(pn.getIncomingBlock(i)))
						// found a PHI node santisfy those condition.
						return pn;
				}
			}
		}
		return null;
	}

	/**
	 * Add the specified block, and all of its predecessors, to the specified
	 * set, if it's not already in there.  Stop predecessor traversal when we
	 * reach StopBlock.
	 * @param inputBB
	 * @param stopBB
	 * @param blocks
	 */
	public static void addBlockAndPredsToSet(BasicBlock inputBB,
			BasicBlock stopBB,
			HashSet<BasicBlock> blocks)
	{
		Stack<BasicBlock> worklist = new Stack<>();
		worklist.add(inputBB);
		do
		{
			BasicBlock bb = worklist.pop();
			if (blocks.add(bb) && bb != stopBB)
			{
				for (PredIterator<BasicBlock> itr = bb.predIterator(); itr.hasNext();)
					worklist.add(itr.next());
			}
		}while (!worklist.isEmpty());
	}

	/**
	 * Once we discover that a loop doesn't have a
	 * preheader, this method is called to insert one.  This method has two phases:
	 * preheader insertion and analysis updating.
	 * @param loop
	 * @return
	 */
	private BasicBlock insertPreHeaderForLoop(Loop loop)
	{
		BasicBlock header = loop.getHeaderBlock();
		ArrayList<BasicBlock> outSideBlocks = new ArrayList<>();
		for (PredIterator<BasicBlock> itr = header.predIterator(); itr.hasNext(); )
		{
			BasicBlock pred = itr.next();
			if (!loop.contains(pred))
				outSideBlocks.add(pred);
		}
		// Split out the loop pre-header.
		BasicBlock newBB = splitBlockPredecessors(header, outSideBlocks, ".preheader", this);

		Loop parent;
		if((parent = loop.getParentLoop()) != null)
			parent.addBasicBlockIntoLoop(newBB, li);
		placeSplitBlockCarefully(newBB, outSideBlocks, loop);
		return newBB;
	}

	/**
	 * If the block isn't already, move the new block to
	 * right after some 'outside block' block.  This prevents the preheader from
	 * being placed inside the loop body, e.g. when the loop hasn't been rotated.
	 * @param newBB
	 * @param outSideBlocks
	 * @param loop
	 */
	private void placeSplitBlockCarefully(BasicBlock newBB,
			ArrayList<BasicBlock> outSideBlocks, Loop loop)
	{
		LinkedList<BasicBlock> list = newBB.getParent().getBasicBlockList();
		int idx = list.indexOf(newBB);
		idx--;
		// Check to see if newBB is already well placed.
		for (BasicBlock bb : outSideBlocks)
			if (bb == list.get(idx))
				return;

		// If it isn't already after an outside block, move it after one.  This is
		// always good as it makes the uncond branch from the outside block into a
		// fall-through.

		// Figure out *which* outside block to put this after.  Prefer an outside
		// block that neighbors a BB actually in the loop.
		BasicBlock foundBB = null;
		for (BasicBlock bb : outSideBlocks)
		{
			int bbi = list.indexOf(bb);
			if (++bbi != list.size() && loop.contains(bb))
			{
				foundBB = bb;
				break;
			}
		}

		// If our heuristic for a *good* parent to place this after doesn't find
		// anything, just pick something.  It's likely better than leaving it within
		// the loop.
		if (foundBB == null)
			foundBB = outSideBlocks.get(0);
		newBB.moveAfter(foundBB);
	}

	/**
	 * Make sure that the loop preheader dominates all exit blocks. This method
	 * is used to splict exit blocks that have predecessors outside of the loop.
	 * @param loop
	 * @param exitBB
	 */
	private BasicBlock rewriteLoopExitBlock(Loop loop, BasicBlock exitBB)
	{
		ArrayList<BasicBlock> loopBlocks = new ArrayList<>();
		for (PredIterator itr = exitBB.predIterator(); itr.hasNext();)
		{
			BasicBlock pred = itr.next();
			if (loop.contains(pred))
				loopBlocks.add(pred);
		}

		Util.assertion(!loopBlocks.isEmpty(),  "No edges coming in from outside the loop?");;
		BasicBlock newBB = splitBlockPredecessors(exitBB, loopBlocks, ".loopexit", this);

		// Update loop information.
		Loop succLoop = li.getLoopFor(exitBB);
		while(succLoop != null && succLoop.contains(loop.getHeaderBlock()))
		{
			succLoop = succLoop.getParentLoop();
		}
		if (succLoop != null)
			succLoop.addBasicBlockIntoLoop(newBB, li);

		return newBB;
	}

	public static FunctionPass createLoopSimplifyPass()
	{
		return new LoopSimplify();
	}
}
