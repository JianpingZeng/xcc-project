package backend.opt;

import backend.pass.FunctionPass;
import backend.value.*;

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
 * @see CFGSimplifyPass
 * @see UnreachableBlockElim
 */
public class ConstantPropagation extends FunctionPass
{
	public long numsInstKilled = 0;

	/**
	 * Performs constant propagation backend.opt upon given method.
	 *
	 * @param f A method where Optimization performed.
	 * @return Whether execution of backend.opt is successful.
	 */
	@Override
	public boolean runOnFunction(Function f)
	{
		LinkedList<Instruction> worklist = new LinkedList<>();
		// initializes the worklist to all of the instructions ready to
		// process
		f.getBasicBlockList().forEach(bb->
		{
			bb.getInstList().forEach(worklist::add);
		});

		boolean changed = false;
		while (!worklist.isEmpty())
		{
			Instruction inst = worklist.removeFirst();
			// ignores it if no other instruction use it
			if (!inst.isUseEmpty())
			{
				Constant val = ConstantFolder.constantFoldInstruction(inst);

				if (val != null)
				{
					// performs constant propagation
					for (Use u : inst.usesList)
						worklist.addLast((Instruction) u.getUser());

					// constant folding and strength reduction
					inst.replaceAllUsesWith(val);
					worklist.removeFirst();
					inst.eraseFromBasicBlock();

					// marks the changed flag
					changed = true;
					++numsInstKilled;
				}
			}
		}
		return changed;
	}

	@Override
	public String getPassName()
	{
		return "Constant Propagation pass";
	}
}
