package backend.opt;

import backend.hir.*;
import backend.value.Constant;
import backend.value.Function;
import backend.value.Use;
import backend.value.Value;

import java.util.LinkedList;

/**
 * <p>This defines a class which implements constant propgation
 * and constant folding as subtask.
 * <p>This file is a member of <a href={@docRoot/opt}>Machine Indepedence
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
	 * Performs constant propagation backend.opt upon given method.
	 *
	 * @param m A method where Optimization performed.
	 * @return Whether execution of backend.opt is successful.
	 */
	public boolean runOnMethod(Function m)
	{
		LinkedList<Value> worklist = new LinkedList<>();
		// initializes the worklist to all of the instructions ready to
		// process
		for (BasicBlock bb : m)
		{
			for (Value inst : bb)
				worklist.add(inst);
		}
		boolean changed = false;
		Canonicalizer can = new Canonicalizer();
		while (!worklist.isEmpty())
		{
			Value inst = worklist.removeFirst();
			// ignores it if no other instruction use it
			if (!inst.isUseEmpty())
			{
				Value val = can.constantFoldInstruction(inst);

				if (val != null)
				{
					if (val instanceof Constant)
					{
						// performs constant propagation
						for (Use u : inst.usesList)
							worklist.addLast(u.getUser());
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

	public static boolean constantFoldTerminator(BasicBlock bb)
	{
		return false;
	}
}
