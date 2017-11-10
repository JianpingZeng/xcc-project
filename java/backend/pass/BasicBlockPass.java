package backend.pass;

import backend.value.BasicBlock;
import backend.value.Function;
import backend.value.Module;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class BasicBlockPass implements Pass
{
	/**
	 * To run this pass on a function, we simply call runOnBasicBlock once for
	 * each function.
	 * @return
	 */
	public boolean runOnFunction(Function f)
	{
		return false;
	}

	public abstract boolean runOnBasicBlock(BasicBlock block);

	public boolean doInitialization(Module m)
	{
		return false;
	}

	public boolean doFinalization(Module m)
	{
		return false;
	}

	public boolean doInitialization(Function f)
	{
		return false;
	}

	public boolean doFinalization(Function f)
	{
		return false;
	}

}
