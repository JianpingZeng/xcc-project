package backend.opt;

import backend.analysis.*;
import backend.hir.BasicBlock;
import backend.hir.PredIterator;
import backend.pass.AnalysisUsage;
import backend.pass.FunctionPass;
import backend.value.Function;
import backend.value.Instruction;
import backend.value.Instruction.BranchInst;
import backend.value.Instruction.CmpInst;
import backend.value.Instruction.PhiNode;
import backend.value.Value;
import tools.OutParamWrapper;

import java.util.ArrayList;
import java.util.Iterator;

import static backend.support.BasicBlockUtil.splitBlockPredecessors;

/**
 * This is a pass which responsible for performing simplification of loop contained 
 * in specified {@linkplain Function function}.
 * 
 * <b>Note that</b> only two kinds of backend.opt yet have been implemented in here,
 * <b>Loop inversion(also called of <a href ="http://llvm.org/docs/doxygen/html/LoopSimplify_8cpp_source.html">
 * Loop rotation</a>)</b> and <b>Insertion of pre-header</b>.
 * 
 * @author Xlous.zeng
 * @version 0.1
 */
public final class LoopSimplify extends FunctionPass
{
	private LoopInfo li;
	private DomTreeInfo dt;
	private AliasAnalysis aliasAnalysis;

	@Override
	public void getAnalysisUsage(AnalysisUsage au)
	{
		au.addRequired(LoopInfo.class);
		au.addRequired(DomTreeInfo.class);
		au.addPreserved(AliasAnalysis.class);
		au.addPreserved(BreakCriticalEdge.class);
	}
	@Override
	public boolean runOnFunction(Function f)
	{
		li = getAnalysisToUpDate(LoopInfo.class);
		dt = getAnalysisToUpDate(DomTreeInfo.class);
		aliasAnalysis = getAnalysisToUpDate(AliasAnalysis.class);
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

			assert loop.getBlocks().get(0) == loop
					.getHeaderBlock() : "Header isn't in the first";
			preHeader = loop.getPreheader();
			if (preHeader == null)
			{
				preHeader = insertPreHeaderForLoop(loop);
				changed = true;
			}

			// Next, check to make sure that all exit nodes of the loop only have
			// predecessors that are inside of the loop.  This check guarantees that the
			// loop preheader/header will dominate the exit blocks.  If the exit block has
			// predecessors from outside of the loop, split the edge now.
			exitBlocks = loop.getExitBlocks();

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
			Instruction inst = loop.getPreheader().getInstAt(i);
			if (inst instanceof PhiNode)
			{
				phiNode = (PhiNode)inst;
				Value v;
				if ((v = phiNode.hasConstantValue()) != null)
				{
					phiNode.replaceAllUsesWith(v);
					phiNode.eraseFromBasicBlock();
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
		for (exitBlocks.size() > 1 && loop.getExitBlock() != null)
		{
			exitBlocks = loop.getExitBlocks();
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

				boolean allInvariant = false;
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
				assert !itr.hasNext();
				changed = true;
				li.removeBlock(exitingBB);

				DominatorFrontier df = getAnalysisToUpDate(DominatorFrontier.class);
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
					df.removeBlock(exitBlocks);

				bi.suxAt(0).removePredecessor(exitingBB);
				bi.suxAt(1).removePredecessor(exitingBB);
				exitingBB.eraseFromParent();
			}
		}
		return changed;
	}

	private boolean foldBranchToCommonDest(BranchInst bi)
	{
		return false;
	}

	private void insertUniqueBackedgeBlock(Loop loop, BasicBlock preHeader)
	{

	}

	private Loop separateNestedLoop(Loop loop)
	{
		return null;
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

		BasicBlock newBB = splitBlockPredecessors(header, outSideBlocks, ".preheader", this);

		Loop parent;
		if((parent = loop.getParentLoop()) != null)
			parent.addBasicBlockIntoLoop(newBB, li);

		placeSplitBlockCarefully(newBB, outSideBlocks, loop);
		return newBB;
	}

	private void placeSplitBlockCarefully(BasicBlock newBB,
			ArrayList<BasicBlock> outSideBlocks, Loop loop)
	{

	}

	private void rewriteLoopExitBlock(Loop loop, BasicBlock exitBB)
	{

	}
}
