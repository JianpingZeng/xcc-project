package opt;

import hir.*;
import hir.BasicBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * This file defines a class that performs useless control flow elimination.
 * This algorithm first proposed and implemented by J.Lu:
 * <p>
 * J.Lu, R.Shillner, Clean:removing useless control flow, unpublished manuscript
 * , Department of computer science, Rice university,Houston, TX, 1994.
 * </p>
 * @author Xlous.zeng
 * @version 0.1
 */
public class UCE
{
	private Function m;
	private boolean changed = true;
	private List<BasicBlock> postOrder;

	/**
	 * The beginning clean method.
	 * <p>
	 * After DCE, There are useless control flow be introduced by other
	 * opt. So that the useless control flow elimination is desired
	 * as follows.
	 * 1.merges redundant branch instruction.
	 * 2.unlinks empty basic block
	 * 3.merges basic block
	 * 4.hoist merge instruction
	 * </p>
	 *
	 * @param m
	 */
	public void clean(Function m)
	{
		postOrder = new ArrayList<>(m.cfg.postOrder());
		while (changed)
		{
			onePass();
			List<BasicBlock> now = new ArrayList<>(m.cfg.postOrder());
			changed = isChanged(postOrder, now);
			postOrder = now;
		}
	}

	private boolean isChanged(List<BasicBlock> before, List<BasicBlock> now)
	{
		if (before.size() != now.size())
			return true;
		for (int idx = 0; idx < before.size(); idx++)
			if (before.get(idx) != now.get(idx))
				return true;
		return false;
	}

	private void onePass()
	{
		// We must usesList the index loop instead of interative loop, because
		// the getArraySize of postOrder list is changing when iterating.
		for (int idx = 0; idx < postOrder.size(); idx++)
		{
			BasicBlock curr = postOrder.get(idx);
			if (curr.isEmpty())
				continue;

			Value lastInst = curr.lastInst();
			// handles conditional branch instruction ends in the basic block as
			// follow.
			//    |          |
			//   B.i         B.i
			//  |   \    ==  |
			//  \   |        |
			//    B.j        B.j
			if (lastInst instanceof Instruction.ConditionalBranchInst)
			{
				Instruction.ConditionalBranchInst branch = (Instruction.ConditionalBranchInst) lastInst;
				if (branch.trueTarget == branch.falseTarget)
				{
					Instruction.Goto go = new Instruction.Goto(
							branch.trueTarget, "Goto");
					branch.insertBefore(go);
					branch.eraseFromBasicBlock();
				}
			}
			// handles unconditional jump instruction.
			if (lastInst instanceof Instruction.Goto)
			{
				Instruction.Goto go = (Instruction.Goto) lastInst;
				BasicBlock target = go.target;
				/**
				 * \   |
				 *  B.i         \ | |
				 *   |  |   ==>   B.j
				 *   B.j
				 */
				// There is only one jump instruction in the B.i
				if (curr.size() == 1)
				{
					DominatorTree RDT = new DominatorTree(true, m);
					RDT.recalculate();

					List<BasicBlock> rdf = RDF.run(RDT, curr);
					for (BasicBlock pred : rdf)
					{
						Value last = pred.lastInst();
						if (last != null)
						{
							if (last instanceof Instruction.Goto
									&& ((Instruction.Goto) last).target == curr)
							{
								((Instruction.Goto) last).target = target;
							}
							else if (last instanceof Instruction.ConditionalBranchInst)
							{
								if (((Instruction.ConditionalBranchInst) last).falseTarget
										== curr)
									((Instruction.ConditionalBranchInst) last).falseTarget = target;
								if (((Instruction.ConditionalBranchInst) last).trueTarget
										== curr)
									((Instruction.ConditionalBranchInst) last).trueTarget = target;
							}
						}
					}
				}

				// |
				// B.i   ==> merge B.i and B.j into one.
				// |
				// B.j
				if (target.getNumOfPreds() == 1)
					merge(curr, target);

				if (target.size() == 1 && (lastInst = target
						.lastInst()) instanceof Instruction.ConditionalBranchInst)
				{
					go.insertBefore(lastInst);
					go.eraseFromBasicBlock();
				}
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
		for (Value inst : second)
		{
			first.appendInst(inst);
		}
		first.removeSuccssor(second);
		for (BasicBlock succ : second.getSuccs())
			first.addSucc(succ);

		// enable the GC.
		second = null;
	}
}
