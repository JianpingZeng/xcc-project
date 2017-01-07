package backend.pass;

import tools.Pair;

import java.util.ArrayList;

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

		return (T)getAnalysisToUpDate(pi);
	}

	/**
	 * Add some pre-requisizement pass of this pass into PassManager.
	 * @param au
	 */
	default void getAnalysisUsage(AnalysisUsage au) {}

	/**
	 * This class must be overridden by concrete subclass.
	 * @param pi
	 * @return
	 */
	Pass getAnalysisToUpDate(PassInfo pi);

	void addToPassManager(ModulePassManager pm, AnalysisUsage au);

	void addToPassManager(FunctionPassManager pm, AnalysisUsage au);

	ArrayList<Pair<PassInfo, Pass>> getAnalysisImpls();
}

