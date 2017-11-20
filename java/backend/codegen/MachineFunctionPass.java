package backend.codegen;

import backend.analysis.*;
import backend.pass.AnalysisResolver;
import backend.pass.AnalysisUsage;
import backend.pass.FunctionPass;
import backend.value.Function;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class MachineFunctionPass implements FunctionPass
{
	private AnalysisResolver resolver;

	@Override
	public void setAnalysisResolver(AnalysisResolver resolver)
	{
		this.resolver = resolver;
	}

	@Override
	public AnalysisResolver getAnalysisResolver()
	{
		return resolver;
	}

	/**
	 * This method must be overridded by concrete subclass for performing
	 * desired machine code transformation or analysis.
	 * @param mf
	 * @return
	 */
	public abstract boolean runOnMachineFunction(MachineFunction mf);

	/**
	 * This method will be passed by {@linkplain #runOnMachineFunction(MachineFunction)}
	 * @param f
	 * @return
	 */
	@Override
	public boolean runOnFunction(Function f)
	{
		MachineFunction mf = f.getMachineFunc();
		assert mf != null:"Instruction selector did not be runned?";
		return runOnMachineFunction(mf);
	}

	/**
	 * Subclasses that override getAnalysisUsage
	 * must call this.
	 * @param au
	 */
	@Override
	public void getAnalysisUsage(AnalysisUsage au)
	{
		au.addRequired(MachineFunctionAnalysis.class);
		au.addPreserved(MachineFunctionAnalysis.class);

		// TODO 2017/11/19 au.addPreserved(AliasAnalysis.class);
		au.addPreserved(ScalarEvolution.class);
		au.addPreserved(IVUsers.class);
		// au.addPreserved(LoopDependenceAnalysis.class);
		// au.addPreserved(MemoryDependenceAnalysis.class);
		au.addPreserved(DomTreeInfo.class);
		au.addPreserved(DominanceFrontier.class);
		au.addPreserved(LoopInfo.class);

		FunctionPass.super.getAnalysisUsage(au);
	}
}
