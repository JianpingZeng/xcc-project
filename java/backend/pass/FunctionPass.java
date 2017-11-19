package backend.pass;

import backend.passManaging.FPPassManager;
import backend.passManaging.PMDataManager;
import backend.passManaging.PassManagerType;
import backend.value.Module;
import backend.value.Function;

import java.util.Stack;

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

    @Override
    default PassManagerType getPotentialPassManagerType()
    {
        return PassManagerType.PMT_FunctionPassManager;
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

	@Override
	default void assignPassManager(Stack<PMDataManager> pms,
			PassManagerType pmt)
	{
        while (!pms.isEmpty())
        {
            if (pms.peek().getPassManagerType().compareTo(PassManagerType.PMT_FunctionPassManager) > 0)
            {
                pms.pop();
            }
            else
               break;
        }
        assert !pms.isEmpty():"Errorous status";
        FPPassManager fpm;
        if (!(pms.peek() instanceof FPPassManager))
        {
            PMDataManager pmd = pms.peek();
            // Step#1 Create new Function Pass Manager
            fpm = new FPPassManager(pmd.getDepth()+1);
            fpm.populateInheritedAnalysis(pms);

            // Step#2 Assign manager to manage this new manager.
            fpm.assignPassManager(pms, pmd.getPassManagerType());
            // Step#3 Push new manager into stack.
            pms.add(fpm);
        }
        fpm = (FPPassManager)pms.peek();
        fpm.add(this);
	}

    @Override
    default void assignPassManager(Stack<PMDataManager> pms)
    {
        assignPassManager(pms, PassManagerType.PMT_FunctionPassManager);
    }

    @Override
    default void getAnalysisUsage(AnalysisUsage au)
    {
        // By default, no analysis results are used. all are invalidated.
    }
}
