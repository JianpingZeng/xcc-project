package backend.target.x86;

import backend.pass.PassManagerBase;
import backend.target.*;
import backend.value.Module;

import java.io.OutputStream;

import static backend.target.TargetFrameInfo.StackDirection.StackGrowDown;
import static backend.target.x86.FloatPointStackitifierPass.createX86FloatingPointStackitifierPass;
import static backend.target.x86.X86DAGtoDAGISel.createX86ISelDag;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86TargetMachine extends LLVMTargetMachine
{
	/**
	 * All x86 instruction information can be accessed by this.
	 */
	private X86InstrInfo instrInfo;
	/**
	 * A stack frame info class used for organizing data layout of frame when
	 * function calling.
	 */
	private TargetFrameInfo frameInfo;
	private X86Subtarget subtarget;
	private TargetData dataLayout;
	private X86TargetLowering tli;
	private RelocModel defRelocModel;

	public X86TargetMachine(Target t, String triple,
			String fs, boolean is64Bit)
	{
		super(t, triple);
		frameInfo = new TargetFrameInfo(StackGrowDown, 8, 4);
		// TODO: 17-7-23
	}

	@Override
	public X86Subtarget getSubtarget()
	{
		// TODO: 17-7-16
		return subtarget;
	}

	/**
     * Allocates and returns a subclass of {@linkplain TargetMachine} that
     * implements the IA32 machine.
     * @param module
     * @return
     */
    public static TargetMachine allocateIA32TargetMachine(Module module)
    {
        // return new X86TargetMachine(module);
        return null;
    }

    @Override
	public TargetInstrInfo getInstrInfo(){return instrInfo;}

	@Override
	public TargetRegisterInfo getRegisterInfo() {return instrInfo.getRegisterInfo();}

	@Override
	public TargetFrameInfo getFrameInfo() {return frameInfo;}

	@Override
	public X86TargetLowering getTargetLowering()
	{
		return tli;
	}

	@Override
	public boolean addInstSelector(PassManagerBase pm, CodeGenOpt level)
	{
		pm.add(createX86ISelDag(this, level));
		return false;
	}

	@Override
	public boolean addPostRegAlloc(PassManagerBase pm, CodeGenOpt level)
	{
		// converts virtual register in X86 FP inst into floating point stack slot.
		pm.add(createX86FloatingPointStackitifierPass());
		return true;
	}

	@Override
	public boolean addAssemblyEmitter(PassManagerBase pm, CodeGenOpt level,
			boolean verbose, OutputStream os)
	{
		pm.add(X86ATTAsmPrinter.createX86AsmCodeEmitter(os, this));
		return false;
	}
}
