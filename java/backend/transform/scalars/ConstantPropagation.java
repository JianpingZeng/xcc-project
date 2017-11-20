package backend.transform.scalars;

import backend.pass.AnalysisResolver;
import backend.pass.FunctionPass;
import backend.transform.utils.ConstantFolder;
import backend.value.Constant;
import backend.value.Function;
import backend.value.Instruction;
import backend.value.Use;

import java.util.LinkedList;

/**
 * <p>This defines a class which implements constant propgation
 * and constant folding as subtask.
 * <p>This file is a member of Machine Indepedence Optimization</p>.
 * @author Xlous.zeng
 * @see DCE
 * @see GVNPRE
 * @see CFGSimplifyPass
 * @see UnreachableBlockElim
 */
public final class ConstantPropagation implements FunctionPass
{
	public long numsInstKilled = 0;

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

	/**
	 * Performs constant propagation backend.transform upon given method.
	 *
	 * @param f A method where Optimization performed.
	 * @return Whether execution of backend.transform is successful.
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
					inst.eraseFromParent();

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
