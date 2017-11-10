package backend.pass;

import backend.passManaging.PMDataManager;
import backend.passManaging.PassManagerType;
import tools.Util;

import java.util.Stack;

import static backend.pass.PassInfoSupport.getPassInfo;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public interface Pass
{
	String getPassName();

	default <T> T getAnalysisToUpDate(Class<T> klass)
	{
		PassInfo pi = getPassInfo(klass);
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
}

