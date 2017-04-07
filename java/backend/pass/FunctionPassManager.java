package backend.pass;

import backend.hir.Module;
import backend.value.Function;

import java.util.HashSet;
import java.util.LinkedList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class FunctionPassManager implements PassManagerBase, ModulePass
{
	/**
	 * A passes queue to be operated on the given function.
	 */
	private LinkedList<FunctionPass> passesToWork;
	private HashSet<FunctionPass> existed;
	private Module m;

	public FunctionPassManager()
	{
		passesToWork = new LinkedList<>();
		existed = new HashSet<>();
	}
	/**
	 * Execute all of the passes scheduled for execution.  Keep
	 * track of whether any of the passes modifies the function, and if
	 * so, return true.
	 * @param f
	 * @return
	 */
	public boolean run(Function f)
	{
		boolean changed = false;
		doInitialization();

		for (FunctionPass pass : passesToWork)
		{
			changed |= pass.doInitialization(m);

			changed |= pass.runOnFunction(f);

			changed |= pass.doFinalization(m);
		}

		doFinalization();
		return changed;
	}

	/**
	 * Add a pass to the queue of passes to run.
	 *
	 * @param p
	 */
	@Override
	public void add(Pass p)
	{
		if (p instanceof FunctionPass && existed.add((FunctionPass) p))
			passesToWork.addLast((FunctionPass) p);
	}

	@Override
	public PassManagerType getPassManagerType()
	{
		return PassManagerType.PMT_FunctionPassManager;
	}

	/**
	 * Run all the initialization works for those passes.
	 * @return
	 */
	public boolean doInitialization()
	{
		return false;
	}

	/**
	 * * Run all the finalization works for those passes.
	 * @return
	 */
	public boolean doFinalization()
	{
		return false;
	}

	@Override
	public boolean runOnModule(Module m)
	{
		this.m = m;
		m.getFunctionList().forEach(this::run);
		return false;
	}

	@Override
	public String getPassName()
	{
		return "Function pass manager";
	}
}
