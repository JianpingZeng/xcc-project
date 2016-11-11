package backend.pass;

import backend.hir.Module;
import backend.value.Function;

/**
 * This class is used to implement most global
 * optimizations.  Optimizations should subclass this class if they meet the
 * following constraints:
 *
 *  1. Optimizations are organized globally, i.e., a function at a time
 *  2. Optimizing a function does not cause the addition or removal of any
 *     functions in the module
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class FunctionPass extends Pass
{
	/**
	 * To run this pass on a module, we simply call runOnFunction once for
	 * each module.
	 * @param f
	 * @return
	 */
	public abstract boolean runOnFunction(Function f);
}
