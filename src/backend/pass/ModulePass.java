package backend.pass;

import backend.hir.Module;
import tools.Pair;

import java.util.ArrayList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class ModulePass implements Pass
{
	protected AnalysisResolver resolver = new ModulePassManager();
	protected ArrayList<Pair<PassInfo, Pass>> analysisImpls
			= new ArrayList<>();

	public abstract boolean runOnModule(Module m);

	@Override
	public Pass getAnalysisToUpDate(PassInfo pi)
	{
		return resolver.getAnalysisToUpdate(pi);
	}

	public void addToPassManager(PassManagerBase pm, AnalysisUsage au)
	{
		pm.addPass(this, au);
	}

	@Override
	public ArrayList<Pair<PassInfo, Pass>> getAnalysisImpls()
	{
		return analysisImpls;
	}
}
