package backend.pass;

import backend.value.Module;
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
public interface FunctionPass extends Pass
{
	//protected AnalysisResolver resolver = new FunctionPassManager();
	//ArrayList<Pair<PassInfo, Pass>> analysisImpls
	//		= new ArrayList<>();
	/**
	 * To run this pass on a module, we simply call runOnFunction once for
	 * each module.
	 * @param f
	 * @return
	 */
	boolean runOnFunction(Function f);

	/**
	 * Do some initialization jobs in pre-function pass.
	 * This method must be overridden by concrete subclasses.
	 * @param m
	 * @return
	 */
	default boolean doInitialization(Module m)
	{
		return false;
	}

	/**
	 * Do some initialization jobs in pre-function pass.
	 * This method must be overridden by concrete subclasses.
	 * @param m
	 * @return
	 */
	default boolean doFinalization(Module m)
	{
		return false;
	}
}
