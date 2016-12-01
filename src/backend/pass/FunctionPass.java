package backend.pass;

import backend.value.Function;
import tools.Pair;

import java.util.ArrayList;

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
public abstract class FunctionPass implements Pass
{
	protected AnalysisResolver resolver = new FunctionPassManager();
	protected ArrayList<Pair<PassInfo, Pass>> analysisImpls
			= new ArrayList<>();
	/**
	 * To run this pass on a module, we simply call runOnFunction once for
	 * each module.
	 * @param f
	 * @return
	 */
	public abstract boolean runOnFunction(Function f);

	@Override
	public void addToPassManager(FunctionPassManager pm, AnalysisUsage au)
	{
		pm.addPass(this, au);
	}

	@Override
	public void addToPassManager(ModulePassManager pm, AnalysisUsage au)
	{
		pm.addPass(this, au);
	}

	@Override
	public ArrayList<Pair<PassInfo, Pass>> getAnalysisImpls()
	{
		return analysisImpls;
	}
	/**
	 * This class must be overridden by concrete subclass.
	 * @param pi
	 * @return
	 */
	@Override
    public Pass getAnalysisToUpDate(PassInfo pi)
	{
		return resolver.getAnalysisToUpdate(pi);
	}
}
