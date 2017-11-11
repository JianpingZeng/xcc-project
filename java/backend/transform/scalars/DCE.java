package backend.transform.scalars;

import backend.analysis.DomTreeInfo;
import backend.analysis.DomTreeNodeBase;
import backend.pass.RegisterPass;
import backend.transform.utils.RDF;
import backend.value.BasicBlock;
import backend.pass.AnalysisUsage;
import backend.pass.FunctionPass;
import backend.value.Function;
import backend.value.Instruction;
import backend.value.Instruction.BranchInst;
import backend.value.Instruction.StoreInst;
import backend.value.User;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * This file defines a class that performs dead code elimination depending on
 * "Implementation of the Mark­Sweep Dead Code Elimination Algorithm in LLVM",
 * Yunming Zhang.el.
 * <p>
 * Dead code elimination perform a single pass over the function removing
 * instructions that are obviously useless.
 * </p>
 * <p>
 * At the sweep stage, a collection of another peephole control flow optimizations
 * will be performed. For instance:
 * <br>
 * 1.Removes basic block with no predecessors.
 * <br>
 * 2.Merges a basic with it's predecessor if there is only one and the predeccessor
 * just only have one successor.
 * <br>
 * </p>
 * Created by Xlous zeng on 2016/3/8.
 */
public class DCE implements FunctionPass
{
	static
	{
		new RegisterPass("die", "Dead Instruction Elimination", DCE.class);
	}
	/**
	 * The list where all critical instruction in Module term resides.
	 */
	private LinkedList<Instruction> criticalInst;
	/**
	 * The list that isDeclScope more than one critical instruction.
	 */
	private LinkedList<BasicBlock> usefulBlocks;

	/**
	 * The list isDeclScope all of no dead instruction.
	 */
	private HashSet<Instruction> liveInsts;

	private Function m;

	private DomTreeInfo dt;

	private void initialize(Function f)
	{
		criticalInst = new LinkedList<>();
		usefulBlocks = new LinkedList<>();
		liveInsts = new HashSet<>();
		m = f;
		dt = getAnalysisToUpDate(DomTreeInfo.class);
	}

	@Override
	public void getAnalysisUsage(AnalysisUsage au)
	{
		au.addRequired(DomTreeInfo.class);
	}

	/**
	 * Performs a single pass dead code elimination over specified method with
	 * follow Mark-Sweep algorithm:
	 * <br>
	 * <b>i.Initial stage</b> that obtains a critical instruction set over function
	 * <br>
	 * <b>ii.Mark stage</b>
	 * <br>
	 * <b>iii.Sweep stage</b>
	 */
	@Override
	public boolean runOnFunction(Function f)
	{
		initialize(f);

		// 1.Initialization stage
		initCriticalInst();
		LinkedList<Instruction> worklist = new LinkedList<>(criticalInst);
		// 2.Mark stage
		while (!worklist.isEmpty())
		{
			Instruction curr = worklist.removeLast();

			for (int i = 0, e = curr.getNumUses(); i < e; i++)
			{
				User u = curr.getOperand(i).getUser();
				if (u instanceof Instruction)
				{
					if (liveInsts.add((Instruction) u))
						worklist.add((Instruction)u);
				}
			}
			// marks branch instruction.
			markBranch(curr, worklist);
		}
		// 3. Sweep stage
		sweep();

		// peephole backend.transform
		eliminateDeadBlock();
		return false;
	}

	/**
	 * Performs sweep stage that removes useless instruction and replace the
	 * branch with an unconditional branch to the nearest and useful dominate
	 * block.
	 */
	private void sweep()
	{
		for (BasicBlock BB : m)
		{
			for (Instruction inst : BB)
			{
				if (!liveInsts.contains(inst))
				{
					// for branch instruction in the basic block, it is special
					// that replaces it with an unconditional branch to it's useful
					// and nearest dominate block.
					if (inst instanceof BranchInst)
					{
						BasicBlock nearestDom = findNearestUsefulPostDom(BB);
						if (nearestDom == BasicBlock.USELESS_BLOCK)
							continue;

						BranchInst go = new BranchInst(nearestDom, "gotoInst");
						inst.insertBefore(go);
						inst.eraseFromParent();
					}
					// the function invocation instruction is handled specially
					// for conservative and safe.
					else if (!(inst instanceof Instruction.CallInst))
						inst.eraseFromParent();
				}
			}
		}
	}

	private void eliminateDeadBlock()
	{
		for (BasicBlock BB : m)
		{
			if (BB.getNumPredecessors() == 0)
			{
				BB.eraseFromParent();
			}
			if (BB.getNumPredecessors() == 1)
			{
				BasicBlock pred = BB.predAt(0);
				if (pred.getNumSuccessors() == 0)
					merge(pred, BB);
			}
		}
	}

	/**
	 * Merges the second into first block.
	 *
	 * @param first  The first block to be merged.
	 * @param second The second block to be merged.
	 */
	private void merge(BasicBlock first, BasicBlock second)
	{
		for (Instruction inst : second)
		{
			first.appendInst(inst);
		}
		first.removeSuccssor(second);
		second.dropAllReferences();
		second.eraseFromParent();
	}

	/**
	 * Finds the first useful and nearest basic block in the post dominator of
	 * specified Basic CompoundStmt.
	 *
	 * @param BB The specified basic block.
	 * @return The nearest and useful post dominator that dominates specified
	 * block.
	 */
	private BasicBlock findNearestUsefulPostDom(BasicBlock BB)
	{
		DomTreeNodeBase<BasicBlock> node = dt.getDomTree().getTreeNodeForBlock(BB);
		LinkedList<DomTreeNodeBase<BasicBlock>> worklist = new LinkedList<>();
		worklist.add(node.getIDom());
		while (!worklist.isEmpty())
		{
			DomTreeNodeBase<BasicBlock> currDOM = worklist.removeLast();
			BasicBlock currBB = currDOM.getBlock();
			if (usefulBlocks.contains(currBB))
				return currBB;
			worklist.addLast(currDOM.getIDom());
		}
		return BasicBlock.USELESS_BLOCK;
	}

	/**
	 * Mark the branch instruction that is the last instruction in the basic block.
	 *
	 * @param inst
	 * @param worklist
	 */
	private void markBranch(Instruction inst, LinkedList<Instruction> worklist)
	{
		BasicBlock bb = inst.getParent();
		LinkedList<BasicBlock> rdf = RDF.run(dt.getDomTree(), bb);
		for (BasicBlock block : rdf)
		{
			Instruction last = block.getLastInst();
			// Only branch instruction will be handled.
			if (last instanceof BranchInst)
			{
				liveInsts.add(last);
				usefulBlocks.add(block);
				worklist.addLast(last);
			}
		}
	}

	/**
	 * Initialize the critical instruction set to be used mark-sweep algorithm.
	 */
	private void initCriticalInst()
	{
		// traverse the CFG with reverse post order.
		Iterator<BasicBlock> itr = m.iterator();
		while (itr.hasNext())
		{
			BasicBlock curr = itr.next();
			for (Instruction inst : curr)
			{
				if (isCritical(inst))
				{
					criticalInst.add(inst);
					usefulBlocks.add(curr);
				}
			}
		}
	}

	/**
	 * Determines whether the specified instruction is a critical or not in term
	 * of Module.
	 * <br>
	 * In the term of Module, a critical instruction must meets one of two conditons:
	 * it is either a return statement, or it "may have side effects".
	 * The second condition means that it may write data into memory. That is a
	 * safe and conservative approach due to the difficulty of disambiguating
	 * memory accesses at compile time.
	 *
	 * @param inst
	 * @return
	 */
	private boolean isCritical(Instruction inst)
	{
        return inst instanceof Instruction.ReturnInst
                || inst instanceof StoreInst || inst.mayHasSideEffects();
	}

	@Override
	public String getPassName()
	{
		return "Dead code elimination pass";
	}
}
