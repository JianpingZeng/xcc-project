package driver;

import hir.BasicBlock;
import hir.Function;
import hir.Module;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import backend.asm.AbstractAssembler;
import backend.lir.CompilerStub;
import backend.lir.LIRAssembler;
import backend.lir.LIRGenerator;
import backend.lir.StackFrame;
import backend.lir.backend.Architecture;
import backend.lir.backend.RegisterConfig;
import backend.lir.backend.MachineInfo;
import backend.lir.backend.TargetFunctionAssembler;
import backend.lir.linearScan.LinearScan;

/**
 * <p>
 * This class encapsulates some global information about the compilation of a 
 * specified file(compilation unit), including target machine information, 
 * OS-specific information etc.
 *
 * @author Xlous.zeng
 */
public abstract class Backend
{
    /**
     * A class which encapsulates many informations about specific machine.
     */
	public final MachineInfo machineInfo;
	public final RegisterConfig registerConfig;
	/**
	 * compilation options.
	 */
	final Options opt;
	/**
	 * stack frame.
	 */
	private StackFrame stackFrame;
	public final Map<Object, CompilerStub> stubs = new HashMap<>();
	private TargetFunctionAssembler assember;
	
	Backend(tools.Context context, MachineInfo machineInfo,
			RegisterConfig registerConfig)
	{
		this.opt = Options.instance(context);
		this.machineInfo = machineInfo;
		this.registerConfig = registerConfig;
	}
	/**
	 * Emits machine code for a single compilation unit upon specified architecture
	 * (like IA32 or AMD64). Note that, every Module instance takes role in representing
	 * a single compilation unit.
	 * @param hir
	 */
	public void emitMachineInst(Module hir)
	{
		emitLIR(hir);
		emitCode(hir);
	}
	/**
	 * Translates hir code into lir format.
	 * @param hir
	 */
	private void emitLIR(Module hir)
	{
		Iterator<Function> itr = hir.iterator();
		while (itr.hasNext())
		{
			Function m = itr.next();
			// create LIRGenerator for every method
			LIRGenerator lirGenerator = newLIRGenerator(machineInfo.arch, this);
			stackFrame = new StackFrame(this, m);
			for (BasicBlock block : m)
				lirGenerator.doBlock(block);
			
			// perform register allocation with linear scanning algorithm.
			new LinearScan(this, m, lirGenerator, frameMap()).allocate();
		}
	}
	/**
	 * Emits assembly code for specified Module, Currently.
	 * In the future, there will have a generator responsible for generating
	 *  immediately executable code.
	 * @param hir
	 */
	private void emitCode(Module hir)
	{
	    final LIRAssembler lirAssembler = newLIRAssember(this, assember());
	    for (Function m : hir) {
	        lirAssembler.emitCode(m.linearScanOrder());
	        
	    }
	}	

	/**
	 * Yield machine code for multiple compilation units upon specified architecture
	 *  (like IA32 or AMD64). Note that, every Module instance takes role in representing
	 *  a single compilation unit.
	 * @param hirs
	 */
	public void emitMachineInst(Module[] hirs)
	{
	    assert(hirs.length > 0) : "No empty module list permitted.";
		if (hirs.length == 1)
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
	public TargetFunctionAssembler assember()
	{
	    if (assember == null)
        {
            AbstractAssembler asm = newAssember(registerConfig);
            TargetFunctionAssembler tasm = new TargetFunctionAssembler(asm);
            tasm.setFrameSize(stackFrame.frameSize());          
        }
        return assember;
	}
	public abstract LIRAssembler newLIRAssember(Backend backend, TargetFunctionAssembler tasm);
	public abstract LIRGenerator newLIRGenerator(Architecture arch, Backend backend);
	public abstract AbstractAssembler newAssember(RegisterConfig registerConfig);	
}
