package backend.pass;

import backend.hir.Module;

import java.io.FileOutputStream;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class PrintModulePass extends ModulePass
{
	private FileOutputStream os;

	public PrintModulePass(FileOutputStream out)
	{
		super();
		os = out;
	}

	@Override
	public boolean runOnModule(Module m)
	{
		return false;
	}
}
