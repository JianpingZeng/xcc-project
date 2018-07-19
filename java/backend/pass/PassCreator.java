package backend.pass;

import backend.passManaging.FunctionPassManager;
import backend.passManaging.PassManager;
import backend.transform.scalars.CFGSimplifyPass;
import backend.transform.scalars.Mem2Reg;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class PassCreator
{
	public static void createStandardFunctionPasses(
			FunctionPassManager fpm, int optimizationLevel)
	{
		if (optimizationLevel > 0)
		{
			fpm.add(CFGSimplifyPass.createCFGSimplificationPass());

			if (optimizationLevel == 1)
				fpm.add(Mem2Reg.createPromoteMemoryToRegisterPass());
		}
	}

	public static Pass createFunctionInliningPass(int threshold)
	{
		// TODO inliner.
		return null;
	}

	public static Pass createAlwaysInliningPass()
	{
		// TODO always inliner.
		return null;
	}

	public static void createStandardModulePasses(PassManager pm,
			int optimizationLevel,
			boolean optimizeSize,
			boolean unrollLoops,
			Pass inliningPass)
	{
		if (optimizationLevel == 0)
		{
			if (inliningPass != null)
				pm.add(inliningPass);
		}
		else
		{
			// remove useless Basic block.
			pm.add(CFGSimplifyPass.createCFGSimplificationPass());
			// remove redundant alloca.
			pm.add(Mem2Reg.createPromoteMemoryToRegisterPass());

			//pm.add(InstructionCombine.createInstructionCombinePass());
			pm.add(CFGSimplifyPass.createCFGSimplificationPass());

			if (inliningPass != null)
				pm.add(inliningPass);
			/*pm.add(SROA.createScalarRreplacementOfAggregatePass());
			pm.add(InstructionCombine.createInstructionCombinePass());
			pm.add(ConditionalPropagate.createCondPropagatePass());
			pm.add(TailCallElim.createTailCallElimination());
			pm.add(CFGSimplifyPass.createCFGSimplificationPass());
			pm.add(DCE.createDeadCodeEliminationPass());
			pm.add(GVNPRE.createGVNPREPass());
			pm.add(LoopSimplify.createLoopSimplifyPass());
			pm.add(LCSSA.createLCSSAPass());
			pm.add(IndVarSimplify.createIndVarSimplifyPass());
			pm.add(LICM.createLICMPass());
			pm.add(LoopDeletion.createLoopDeletionPass());
			pm.add(GVNPRE.createGVNPREPass());
			pm.add(SCCP.createSparseConditionalConstantPropagatePass());
			pm.add(DCE.createDeadCodeEliminationPass());
			pm.add(CFGSimplifyPass.createCFGSimplificationPass());
			*/
		}
	}
}
