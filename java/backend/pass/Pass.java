package backend.pass;

import backend.passManaging.PMDataManager;
import backend.passManaging.PassManagerType;
import tools.Util;

import java.util.Stack;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public interface Pass
{
	String getPassName();

	/**
	 * Return the PassInfo data structure that corresponding to this
	 * pass. If the pass has not been registered, this would return null.
	 * @return
	 */
	default PassInfo getPassInfo()
	{
		return PassInfoSupport.getPassInfo(getClass());
	}

	default <T> T getAnalysisToUpDate(Class<T> klass)
	{
		PassInfo pi = PassInfoSupport.getPassInfo(klass);
		if (pi == null) return null;

		return (T)PassDataBase.getAnalysisOrNull(pi);
	}

	/**
	 * Add some pre-requisizement pass of this pass into PassManager.
	 * @param au
	 */
	default void getAnalysisUsage(AnalysisUsage au) {}

    default void dumpPassStructures(int offset)
    {
    	System.err.printf("%s%s\n", Util.fixedLengthString(offset<<1, ' '), getPassName());
    }

    default PassManagerType getPotentialPassManagerType()
    {
    	return PassManagerType.PMT_Unknow;
    }

    default void assignPassManager(Stack<PMDataManager> pms)
    {
    	assignPassManager(pms, PassManagerType.PMT_Unknow);
    }

    default void assignPassManager(Stack<PMDataManager> pms,
		    PassManagerType pmt) {}

	/**
	 * This method should be overridden by ImmutablePass.
	 * @return
	 */
	default ImmutablePass getAsImmutablePass()
    {
    	return null;
    }

    default PMDataManager getAsPMDataManager()
    {
    	return null;
    }

	/**
	 * This member can be implemented by a analysis pass to
	 * check state of analysis information.
	 */
	default void verifyAnalysis()
    {}

	/**
	 * Check if available pass managers are suitable for this pass or not.
	 * @param activeStack
	 */
	default void preparePassManager(Stack<PMDataManager> activeStack)
    {}
}

