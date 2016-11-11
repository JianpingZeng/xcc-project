package backend.pass;

import backend.hir.Module;

import java.util.ArrayList;

/**
 * Manager {@linkplain ModulePass}.
 * @author Xlous.zeng
 * @version 0.1
 */
public class PassManager extends PassManagerBase
{
	/**
	 * Execute all of the passes scheduled for execution.  Keep track of
	 * whether any of the passes modifies the module, and if so, return true.
	 * @param m
	 * @return
	 */
	public boolean run(Module m)
	{
		boolean res = false;
		for (Pass p : passesList);
			//res |= p.runOnModule(m);

		return res;
	}
}
