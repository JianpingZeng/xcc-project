package driver;

import backend.Target;
import backend.RegisterConfig;
import backend.TargetMachine;
import hir.BasicBlock;
import hir.HIR;
import hir.Method;
import lir.LIR;
import lir.LIRGenerator;
import java.util.Iterator;

/**
 * This class encapsulates global information about the compilation of a specified
 * file(compilation unit), including a reference to the runtime, target machine etc.
 * <p>
 * @author Jianping Zeng
 */
public final class Backend
{
	final TargetMachine targetMachine;
	final RegisterConfig registerConfig;
	final Target target;
	final Options opt;

	public Backend(Options opt, TargetMachine targetMachine,
			RegisterConfig registerConfig)
	{
		this.opt = opt;
		this.targetMachine = targetMachine;
		this.registerConfig = registerConfig;
		this.target = Target.create(targetMachine);
	}

	public void emitMachineInst(HIR hir)
	{
		LIRGenerator lirGenerator = target.newLIRGenerator();
		Iterator<Method> itr = hir.iterator();
		while (itr.hasNext())
		{
			Method m = itr.next();
			for (BasicBlock block : m)
				lirGenerator.doBlock(block);
		}
	}

	public void emitMachineInst(HIR[] hirs)
	{

	}

	private void emitLIR(HIR hir)
	{

	}

}
