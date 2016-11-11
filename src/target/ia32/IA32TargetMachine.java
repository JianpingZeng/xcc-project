package target.ia32;

import backend.hir.Module;
import target.TargetMachine;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class IA32TargetMachine extends TargetMachine
{
	public IA32TargetMachine(Module module)
	{
		super("X86", true, 4, 4, 4, 4, 4, 4, 2, 1);
	}
}
