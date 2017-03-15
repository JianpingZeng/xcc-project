package backend.pass;

import backend.hir.Module;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class PrintModulePass extends ModulePass
{
	private PrintStream os;

	public PrintModulePass(OutputStream out)
	{
		super();
		os = new PrintStream(out);
	}

	@Override
	public boolean runOnModule(Module m)
	{
		return false;
	}

	@Override public String getPassName()
	{
		return null;
	}

	@Override
	public void addToPassManager(ModulePassManager pm, AnalysisUsage au)
	{

	}

	@Override
	public void addToPassManager(FunctionPassManager pm, AnalysisUsage au)
	{

	}
}
