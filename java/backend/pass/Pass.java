package backend.pass;

import backend.hir.Module;
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

		return (T)PassDataBase.getAnalysisOrNull(pi);
	}

	/**
	 * Add some pre-requisizement pass of this pass into PassManager.
	 * @param au
	 */
	default void getAnalysisUsage(AnalysisUsage au) {}

	default boolean run(Module m) { return false; }
}

