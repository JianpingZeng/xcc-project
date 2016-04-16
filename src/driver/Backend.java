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
 * <p>
 * This class encapsulates global information about the compilation of a specified
 * file(compilation unit), including a reference to the runtime, targetAbstractLayer
 * machine etc.
 * @author Jianping Zeng
 */
public final class Backend
{
	public final TargetMachine targetMachine;
	public 	final RegisterConfig registerConfig;
	public final TargetAbstractLayer targetAbstractLayer;
	// compilation options
	final Options opt;
	// stack frame
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
	/**
	 * Yield machine code for a single compilation unit upon specified architecture
	 * (like x86 or AMD64). Note that, every HIR instance takes role in representing
	 * a single compilation unit.
	 * @param hir
	 */
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

	/**
	 * Yield machine code for multiple compilation units upon specified architecture
	 *  (like x86 or AMD64). Note that, every HIR instance takes role in representing
	 *  a single compilation unit.
	 * @param hirs
	 */
	public void emitMachineInst(HIR[] hirs)
	{
		if (hirs.length < 1)
			return;
		else if (hirs.length == 1)
		{
			emitMachineInst(hirs[0]);
			return;
		}

		for (int i = 0; i < hirs.length;i++)
		{
			Iterator<Method> itr = hirs[i].iterator();
			while (itr.hasNext())
			{
				Method m = itr.next();
				LIRGenerator lirGenerator = targetAbstractLayer.newLIRGenerator(m);
				for (BasicBlock block : m)
					lirGenerator.doBlock(block);
			}
		}
	}

	public StackFrame frameMap()
	{
		return stackFrame;
	}

}
