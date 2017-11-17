package backend.target.x86;

import backend.codegen.ELFWriter;
import backend.codegen.MachineCodeEmitter;
import backend.passManaging.FunctionPassManager;
import backend.passManaging.PassManagerBase;
import backend.target.*;
import backend.value.Module;

import java.io.OutputStream;
import java.io.PrintStream;

import static backend.target.TargetFrameInfo.StackDirection.StackGrowDown;
import static backend.target.TargetMachine.CodeModel.Small;
import static backend.target.TargetMachine.RelocModel.*;
import static backend.target.x86.X86ATTAsmPrinter.createX86AsmCodeEmitter;
import static backend.target.x86.X86CodeEmitter.createX86CodeEmitterPass;
import static backend.target.x86.X86FastISel.createX86FastISel;
import static backend.target.x86.X86Subtarget.PICStyle.*;

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
		subtarget = X86Subtarget.createX86Subtarget(triple, fs, is64Bit);
		dataLayout = new TargetData(subtarget.getDataLayout());
		frameInfo = new TargetFrameInfo(StackGrowDown, subtarget.getStackAlignemnt(),
				(subtarget.isTargetWin64() ? -40 :
				(subtarget.is64Bit ? -8 : -4)));
		instrInfo = new X86InstrInfo(this);
		tli = new X86TargetLowering(this);
		defRelocModel = getRelocationModel();

		if (getRelocationModel() == RelocModel.Default)
		{
			if (!subtarget.isTargetDarwin())
				setRelocationModel(Static);
			else if (subtarget.is64Bit())
				setRelocationModel(PIC_);
			else
				setRelocationModel(DynamicNoPIC);
		}

		assert getRelocationModel() != Default :"Relocation mode not picked";
		if (getCodeModel() == CodeModel.Default)
			setCodeModel(Small);

		// ELF and X86-64 don't have a distinct DynamicNoPIC model.  DynamicNoPIC
		// is defined as a model for code which may be used in static or dynamic
		// executables but not necessarily a shared library. On X86-32 we just
		// compile in -static mode, in x86-64 we use PIC.
		if (getRelocationModel() == DynamicNoPIC)
		{
			if (is64Bit)
				setRelocationModel(PIC_);
			else if (!subtarget.isTargetDarwin())
				setRelocationModel(Static);
		}

		// If we are on Darwin, disallow static relocation model in X86-64 mode, since
		// the Mach-O file format doesn't support it.
		if (getRelocationModel() == Static &&
				subtarget.isTargetDarwin() &&
				is64Bit)
		{
			setRelocationModel(PIC_);
		}

		// Determine the PICStyle based on the target selected.
		if (getRelocationModel() == Static)
		{
			// Unless we're in PIC or DynamicNoPIC mode, set the PIC style to None.
			subtarget.setPICStyle(None);
		}
		else if (subtarget.isTargetCygMing())
		{
			subtarget.setPICStyle(None);
		}
		else if (subtarget.isTargetDarwin())
		{
			if (subtarget.is64Bit())
				subtarget.setPICStyle(RIPRel);
			else if (getRelocationModel() == PIC_)
				subtarget.setPICStyle(StubPIC);
			else
			{
				assert(getRelocationModel() == DynamicNoPIC);
				subtarget.setPICStyle(StubDynamicNoPIC);
			}
		}
		else if (subtarget.isTargetELF())
		{
			if (subtarget.is64Bit())
				subtarget.setPICStyle(RIPRel);
			else
				subtarget.setPICStyle(GOT);
		}
		// Finally, if we have "none" as our PIC style, force to static mode.
		if (subtarget.getPICStyle() == None)
			setRelocationModel(Static);
	}

	@Override
	public X86Subtarget getSubtarget()
	{
		return subtarget;
	}

	@Override
	public TargetData getTargetData()
	{
		return dataLayout;
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
	public TargetInstrInfo getInstrInfo()
    {
        return instrInfo;
    }

	@Override
	public TargetRegisterInfo getRegisterInfo()
    {
        return instrInfo.getRegisterInfo();
    }

	@Override
	public TargetFrameInfo getFrameInfo()
    {
        return frameInfo;
    }

	@Override
	public X86TargetLowering getTargetLowering()
	{
		return tli;
	}

	@Override
	public boolean addInstSelector(PassManagerBase pm, CodeGenOpt level)
	{
		pm.add(createX86FastISel(this, level));
		return false;
	}

    @Override
    public boolean addPreRegAlloc(PassManagerBase pm, CodeGenOpt level)
    {
        // TODO: 17-7-24 pm.add(createX86MaxStackAlignmentCalculatorPass());
        return super.addPreRegAlloc(pm, level);
    }

    @Override
	public boolean addPostRegAlloc(PassManagerBase pm, CodeGenOpt level)
	{
		// converts virtual register in X86 FP inst into floating point stack slot.
		// todo 2017-10-14 pm.add(createX86FloatingPointStackitifierPass());
		return false;
	}

	@Override
	public boolean addAssemblyEmitter(PassManagerBase pm, CodeGenOpt level,
			boolean verbose, OutputStream os)
	{
		pm.add(createX86AsmCodeEmitter(os, this, getTargetAsmInfo(), verbose));
		return false;
	}

	@Override
	public boolean addSimpleCodeEmitter(PassManagerBase pm, CodeGenOpt level,
			MachineCodeEmitter mce)
	{
        pm.add(createX86CodeEmitterPass(this, mce));
		return false;
	}

    @Override
    public MachineCodeEmitter addELFWriter(
            FunctionPassManager pm,
            PrintStream os)
    {
        ELFWriter writer = new ELFWriter(os, this);
        pm.add(writer);
        return writer.getMachineCodeEmitter();
    }
}
