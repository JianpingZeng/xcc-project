package backend.pass;

import backend.hir.Module;
import tools.Pair;

import java.util.ArrayList;

/**
 * his class is used to provide information that does not need to be run.
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class ImmutablePass implements Pass
{
	protected AnalysisResolver resolver;
	public ArrayList<Pair<PassInfo, Pass>> analysisImpls;

	protected ImmutablePass() { analysisImpls = new ArrayList<>();}

	public boolean run(Module m) {return false;}

	public abstract void initializePass();

	public ArrayList<Pair<PassInfo, Pass>> getAnalysisImpls()
	{
		return analysisImpls;
	}
}
