package backend.pass;

import backend.hir.Module;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class ModulePass extends Pass
{
	public abstract boolean runOnModule(Module m);
}
