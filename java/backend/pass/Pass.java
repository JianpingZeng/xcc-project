package backend.pass;

import backend.passManaging.PMDataManager;
import backend.passManaging.PMStack;
import backend.passManaging.PassManagerType;
import backend.support.Printable;
import tools.Util;

import java.io.PrintStream;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public interface Pass extends Printable
{
	String getPassName();

	/**
	 * Return the PassInfo data structure that corresponding to this
	 * pass. If the pass has not been registered, this would return null.
	 * @return
	 */
	default PassInfo getPassInfo()
	{
		return PassRegistrar.getPassInfo(this.getClass());
	}

	AnalysisResolver getAnalysisResolver();

	void setAnalysisResolver(AnalysisResolver resolver);

	default Pass getAnalysisToUpDate(Class<? extends Pass> klass)
	{
		PassInfo pi = PassRegistrar.getPassInfo(klass);
		if (pi == null) return null;
        AnalysisResolver resolver = getAnalysisResolver();
        assert resolver != null:"Pass not resident in PassManger object!";
		return resolver.findImplPass(pi);
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

    default void assignPassManager(PMStack pms)
    {
    	assignPassManager(pms, PassManagerType.PMT_Unknow);
    }

    default void assignPassManager(PMStack pms,
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
	default void preparePassManager(PMStack activeStack)
    {}

    default void print(PrintStream os)
    {
    	os.printf("[Pass: %s]\n", getPassName());
    }
}

