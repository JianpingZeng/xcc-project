package backend.pass;

import backend.value.BasicBlock;
import backend.value.Function;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class BasicBlockPass implements FunctionPass
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

	public abstract boolean runOnBlock(BasicBlock block);
}
