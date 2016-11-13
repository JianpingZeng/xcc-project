package backend.target.x86;

import backend.hir.Module;
import backend.pass.FunctionPassManager;
import backend.target.TargetFrameInfo;
import backend.target.TargetInstrInfo;
import backend.target.TargetMachine;
import backend.target.TargetRegisterInfo;

import java.io.FileOutputStream;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86TargetMachine extends TargetMachine
{
	public X86TargetMachine(Module module)
	{
		super("X86", true, 4, 4, 4, 4, 4, 4, 2, 1);
	}

	@Override
	public TargetInstrInfo getInstrInfo()
	{
		return null;
	}

	@Override
	public TargetRegisterInfo getRegInfo()
	{
		return null;
	}

	@Override
	public TargetFrameInfo getFrameInfo()
	{
		return null;
	}

	/**
	 * Add passes to the specified pass manager to get assembly language code
	 * emitted.  Typically this will involve several steps of code generation.
	 * This method should return true if assembly emission is not supported.
	 * <p>
	 * Note that: this method would be overriden by concrete subclass for
	 * different backend.target, like IA32, Sparc.
	 *
	 * @param pm
	 * @param fast
	 * @param asmOutStream
	 * @param genFileType
	 * @param optLevel
	 * @return
	 */
	@Override
	public boolean addPassesToEmitFile(FunctionPassManager pm,
			boolean fast, FileOutputStream asmOutStream,
			CodeGenFileType genFileType, CodeGenOpt optLevel)
	{
		return false;
	}
}
