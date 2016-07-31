package compiler;

import hir.BasicBlock;
import hir.Method;
import hir.Module;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import lir.CompilerStub;
import lir.LIRGenerator;
import lir.StackFrame;
import lir.backend.RegisterConfig;
import lir.backend.TargetAbstractLayer;
import lir.backend.TargetMachine;
import lir.linearScan.LinearScan;

/**
 * <p>
 * This class encapsulates global information about the compilation of a specified
 * file(compilation unit), including a reference to the runtime, targetAbstractLayer
 * machine etc.
 *
 * @author Xlous.zeng
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
	public final Map<Object, CompilerStub> stubs = new HashMap<>();

	public Backend(utils.Context context, TargetMachine targetMachine,
			RegisterConfig registerConfig)
	{
		this.opt = Options.instance(context);
		this.targetMachine = targetMachine;
		this.registerConfig = registerConfig;
		this.targetAbstractLayer = TargetAbstractLayer.create(targetMachine.arch, this);
	}
	/**
	 * Yield machine code for a single compilation unit upon specified architecture
	 * (like X86 or AMD64). Note that, every Module instance takes role in representing
	 * a single compilation unit.
	 * @param hir
	 */
	public void emitMachineInst(Module hir)
	{
		emitLIR(hir);
		emitCode(hir);
	}

	private void emitLIR(Module hir)
	{
		Iterator<Method> itr = hir.iterator();
	
		while (itr.hasNext())
		{
			Method m = itr.next();
			// create LIRGenerator for every method
			LIRGenerator lirGenerator = targetAbstractLayer.newLIRGenerator();
			this.stackFrame = new StackFrame(this, m);

			for (BasicBlock block : m)
				lirGenerator.doBlock(block);

			new LinearScan(this, m, lirGenerator, frameMap()).allocate();
		}
	}
	/**
	 * Emits assembly code for specified Module instance. 
	 * @param hir
	 */
	private void emitCode(Module hir)
	{
		
	}

	/**
	 * Yield machine code for multiple compilation units upon specified architecture
	 *  (like X86 or AMD64). Note that, every Module instance takes role in representing
	 *  a single compilation unit.
	 * @param hirs
	 */
	public void emitMachineInst(Module[] hirs)
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
			emitMachineInst(hirs[i]);
		}
	}

	public StackFrame frameMap()
	{
		return stackFrame;
	}

}
