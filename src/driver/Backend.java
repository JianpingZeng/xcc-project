package driver;

import lir.CompilerStub;
import lir.backend.TargetAbstractLayer;
import lir.backend.RegisterConfig;
import lir.backend.TargetMachine;
import hir.BasicBlock;
import hir.HIR;
import hir.Method;
import lir.StackFrame;
import lir.LIRGenerator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class encapsulates global information about the compilation of a specified
 * file(compilation unit), including a reference to the runtime, targetAbstractLayer machine etc.
 * <p>
 * @author Jianping Zeng
 */
public final class Backend
{
	public final TargetMachine targetMachine;
	public 	final RegisterConfig registerConfig;
	public final TargetAbstractLayer targetAbstractLayer;
	final Options opt;
	private StackFrame stackFrame;
	public final Map<Object, CompilerStub> stubs = new HashMap<Object, CompilerStub>();

	public Backend(Options opt, TargetMachine targetMachine,
			RegisterConfig registerConfig)
	{
		this.opt = opt;
		this.targetMachine = targetMachine;
		this.registerConfig = registerConfig;
		this.targetAbstractLayer = TargetAbstractLayer.create(targetMachine.arch, this);
	}

	public void emitMachineInst(HIR hir)
	{
		Iterator<Method> itr = hir.iterator();
		while (itr.hasNext())
		{
			Method m = itr.next();
			LIRGenerator lirGenerator = targetAbstractLayer.newLIRGenerator(m);
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

	public StackFrame frameMap()
	{
		return stackFrame;
	}

}
