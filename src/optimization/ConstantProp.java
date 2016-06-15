package optimization;

import hir.*;

import java.util.LinkedList;

/**
 * <p>This defines a class which implements constant propgation
 * and constant folding as subtask.
 * <p>This file is a member of <a href={@docRoot/optimization}>Machine Indepedence
 * Optimization</a>.
 *
 * @author Xlous.zeng
 * @see Canonicalizer
 * @see DCE
 * @see GVN
 * @see UCE
 */
public class ConstantProp
{
	private long numsInstKilled = 0;

	/**
	 * Performs constant propagation optimization upon given method.
	 *
	 * @param m A method where Optimization performed.
	 * @return Whether execution of optimization is successful.
	 */
	public boolean runOnMethod(Method m)
	{
		LinkedList<Instruction> worklist = new LinkedList<>();
		// initializes the worklist to all of the instructions ready to
		// process
		for (BasicBlock bb : m)
		{
			for (Instruction inst : bb)
				worklist.add(inst);
		}
		boolean changed = false;
		Canonicalizer can = new Canonicalizer();
		while (!worklist.isEmpty())
		{
			Instruction inst = worklist.removeFirst();
			// ignores it if no other instruction use it
			if (!inst.isUseEmpty())
			{
				Value val = can.constantFoldInstruction(inst);

				if (val != null)
				{
					if (val instanceof Value.Constant)
					{
						// performs constant propagation
						for (Use u : inst.usesList)
							worklist.addLast((Instruction) u.getUser());
					}
					// constant folding and strength reduction
					inst.replaceAllUsesWith(val);
					worklist.remove(inst);
					inst.eraseFromBasicBlock();

					// marks the changed flag
					changed = true;
					++numsInstKilled;
				}
			}
		}
		return changed;
	}
}
