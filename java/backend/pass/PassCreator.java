package backend.pass;

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
			//fpm.add(new CFGSimplificationPass());
			/*
			if (optimizationLevel == 1)
				fpm.add(new PromoteMemToReg());
			*/
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
			byte optimizationLevel,
			boolean optimizeSize,
			boolean unrollLoops,
			Pass inliningPass)
	{

	}
}
