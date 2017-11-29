package backend.transform.scalars;

import backend.analysis.*;
import backend.analysis.LoopInfo;
import backend.support.IntStatistic;
import backend.value.*;
import backend.pass.*;
import backend.support.PredIteratorCache;
import backend.value.Instruction.PhiNode;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Converts loop into loop-closed SSA form.
 *
 * <p>
 *   This pass transforms loops by placing phi nodes at the end of the loops for
 *   all values that are live across the loop boundary.  For example, it turns
 *   the left into the right code:
 * </p>
 * <pre>
 *	 for (...)                for (...)
 *	   if (c)                   if (c)
 *	     X1 = ...                 X1 = ...
 *	   else                     else
 *	     X2 = ...                 X2 = ...
 *	   X3 = phi(X1, X2)         X3 = phi(X1, X2)
 *	 ... = X3 + 4             X4 = phi(X3)
 *	                          ... = X4 + 4
 * </pre>
 * <p>
 *	This is still valid LLVM IR form; the extra phi nodes are purely redundant, and will
 *	be trivially eliminated by InstCombine.  The major benefit of this
 *	transformation is that it makes many other loop optimizations, such as
 *	LoopUnswitching, simpler.
 * </p>
 * @author Xlous.zeng
 * @version 0.1
 */
public final class LCSSA implements LoopPass
{
	private LoopInfo li;
	private DomTree dt;
	private ArrayList<BasicBlock> loopBlocks;
	PredIteratorCache predCache;

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

	private LCSSA()
	{
		predCache = new PredIteratorCache();
	}

	public static final IntStatistic numLCSSA =
			new IntStatistic("numLCSSA", "the number of lcssa");

	public static Pass createLCSSAPass()
	{
		return new LCSSA();
	}

	/**
	 * Process all loops in the entire function.
	 * @param loop
	 * @return
	 */
	@Override
	public boolean runOnLoop(Loop loop, LPPassManager ppm)
	{
		predCache.clear();
		li = (LoopInfo) getAnalysisToUpDate(LoopInfo.class);
		dt = (DomTree) getAnalysisToUpDate(DomTree.class);

		loopBlocks = new ArrayList<>(loop.getBlocks());
		ArrayList<BasicBlock> exitBlocks = loop.getExitingBlocks();

		ArrayList<Instruction> affectedValues = new ArrayList<>();
		getLoopValueUsedOutsideLoop(loop, affectedValues, exitBlocks);

		// If no values are affected, we can save a lot of work, since we know that
		// nothing will be changed.
		if (affectedValues.isEmpty())
			return false;

		// Iterate over all affected values for this loop and insert Phi nodes
		// for them in the appropriate exit blocks
		affectedValues.forEach(var->
		{
			processInstruction(var, exitBlocks);
		});
		return true;
	}

	/**
	 * Gets any values defined in the loop and used by instructions outside of it.
	 * @param loop
	 * @param affectedValues
	 * @param exitBlocks
	 */
	private void getLoopValueUsedOutsideLoop(Loop loop,
			ArrayList<Instruction> affectedValues,
			ArrayList<BasicBlock> exitBlocks)
	{
		for (BasicBlock bb : loop.getBlocks())
		{
			for (Instruction inst : bb)
			{
				for (int i = 0, e = inst.getNumUses(); i < e; i++)
				{
					Use u = inst.useAt(i);
					BasicBlock userBB = ((Instruction)u.getUser()).getParent();
					if (u.getUser() instanceof PhiNode)
					{
						userBB = ((PhiNode)u.getUser()).getIncomingBlock(i);
					}

					if (!bb.equals(userBB) && !inLoop(userBB))
					{
						affectedValues.add(inst);
						break;
					}
				}
			}
		}
	}

	/**
	 * Given live-out instruction, inserts phi node in the appropriate block to
	 * eliminate all of out of loop uses.
	 * @param inst
	 * @param exitBlocks
	 */
	private void processInstruction(Instruction inst, ArrayList<BasicBlock> exitBlocks)
	{
		numLCSSA.inc();
		HashMap<DomTreeNodeBase<BasicBlock>, Value> phis = new HashMap<>();
		BasicBlock domBB = inst.getParent();

		DomTreeNodeBase<BasicBlock> domNode = dt.getNode(domBB);

		// Inserts the LCSSA phis into the exit block (dominated by value),
		// and add them to the phi's map.
		exitBlocks.forEach(exitBB->
		{
			DomTreeNodeBase<BasicBlock> exitDomNode = dt.getNode(exitBB);
			if (!phis.containsKey(exitDomNode) && dt.dominates(domNode, exitDomNode))
			{
				PhiNode pn = new PhiNode(inst.getType(),
						predCache.getNumPreds(exitBB), inst.getName()+".lcssa",
						exitBB);
				phis.put(exitDomNode, pn);
				// Add inputs from inside the loop for this PHI.
				predCache.getPreds(exitBB).forEach(pred->
				{
					pn.addIncoming(inst, pred);
				});
			}
		});

		// Record all uses of Instr outside the loop.  We need to rewrite these.  The
		// LCSSA phis won't be included because they use the value in the loop.
		int idx = 0;
		for (Use u : inst.getUseList())
		{
			BasicBlock userBB = ((Instruction)u.getUser()).getParent();
			if (u.getUser() instanceof PhiNode)
			{
				PhiNode pn = ((PhiNode) u.getUser());
				userBB = pn.getIncomingBlock(idx);
			}

			// just handle the uses out-of-loop.
			if (userBB.equals(inst.getParent()) || inLoop(userBB))
				continue;

			Value val = getValueForBlock(dt.getNode(userBB), inst, phis);
			u.setValue(val);
			idx++;
		}
	}

	private boolean inLoop(BasicBlock bb)
	{
		return loopBlocks.contains(bb);
	}

	/**
	 * Get the value to use within the specified basic block available values
	 * are in phis.
	 * @param domNode
	 * @param inst
	 * @param phis
	 * @return
	 */
	private Value getValueForBlock(DomTreeNodeBase<BasicBlock> domNode,
			Instruction inst, HashMap<DomTreeNodeBase<BasicBlock>, Value> phis)
	{
		if (domNode == null)
			return Value.UndefValue.get(inst.getType());

		if(phis.containsKey(domNode))
			return phis.get(domNode);

		DomTreeNodeBase<BasicBlock> idom = domNode.getIDom();

		if (!inLoop(idom.getBlock()))
		{
			Value val = getValueForBlock(idom, inst, phis);
			phis.put(domNode, val);
			return val;
		}

		BasicBlock bb = domNode.getBlock();
		PhiNode pn = new PhiNode(inst.getType(),
				predCache.getNumPreds(bb),
				inst.getName()+".lcssa",
				bb);
		phis.put(domNode, pn);
		predCache.getPreds(bb).forEach(pred->
		{
			pn.addIncoming(getValueForBlock(dt.getNode(pred), inst, phis), pred);
		});
		return pn;
	}

	@Override
	public String getPassName()
	{
		return "Loop-closed SSA form";
	}

	/**
	 * This transformation requires that natural loop simplification and
	 * loop pre-header be inserted into the CFG. Also, it requires dominator
	 * information.
	 * @param au
	 */
	@Override
	public void getAnalysisUsage(AnalysisUsage au)
	{
		au.addRequired(LoopSimplify.class);
		au.addPreserved(LoopSimplify.class);
		au.addRequired(LoopInfo.class);
		au.addPreserved(LoopInfo.class);
		au.addRequired(DomTree.class);
		au.addPreserved(ScalarEvolution.class);
		au.addPreserved(DomTree.class);

		// Request DominanceFrontier now, even though LCSSA does
		// not use it. This allows Pass Manager to schedule Dominance
		// Frontier early enough such that one LPPassManager can handle
		// multiple loop transformation passes.
		au.addRequired(DominanceFrontier.class);
		au.addPreserved(DominanceFrontier.class);
	}
}
