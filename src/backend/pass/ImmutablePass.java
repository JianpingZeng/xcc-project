package backend.pass;

import backend.hir.Module;

/**
 * his class is used to provide information that does not need to be run.
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class ImmutablePass extends Pass
{
	public boolean run(Module m) {return false;}
}
