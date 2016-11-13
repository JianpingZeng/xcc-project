package backend.target;

import backend.hir.Module;
import backend.pass.FunctionPassManager;
import backend.target.x86.X86TargetMachine;

import java.io.FileOutputStream;

/**
 * Primary interface to complete machine description for the backend.target machine.
 * Our goal is that all backend.target-specific information should accessible through
 * this interface.
 * @see TargetData
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class TargetMachine
{
    /**
     * Code generation optimization level.
     */
    public enum CodeGenOpt
    {
        None,
        Default,
        Aggressiv
    }

	/**
     * hese enums are meant to be passed into
     * addPassesToEmitFile to indicate what type of file to emit.
     */
    public enum CodeGenFileType
    {
        AssemblyFile, ObjectFile, DynamicLibrary
    }

	/**
	 * The backend.target name.
	 */
	private String name;
	/**
	 * Calculate type size and alignment.
	 */
	private TargetData dataLayout;

	/**
	 * Can only called by subclass.
	 */
	protected TargetMachine(String name,
			boolean littleEndian,
			int ptrSize, int ptrAlign,
			int doubleAlign, int floatAlign,
			int longAlign, int intAlign,
			int shortAlign, int byteAlign)
	{
		this.name = name;
	}

	protected TargetMachine(String name)
	{
		this(name, false, 8, 8, 8, 4, 8, 4, 2, 1);
	}

	/**
	 * Allocates and returns a subclass of {@linkplain TargetMachine} that
	 * implements the IA32 machine.
	 * @param module
	 * @return
	 */
	public static TargetMachine allocateIA32TargetMachine(Module module)
	{
		return new X86TargetMachine(module);
	}

	public String getName(){return name;}

	// Interface to the major aspects of target machine information:
	// 1.Instruction opcode and operand information.
	// 2.Pipeline and scheduling information.
	// 3.Register information.
	// 4.Stack frame information.
	// 5.Cache hierarchy information.
	// 6.Machine-level optimization information (peepole only).
	public abstract TargetInstrInfo getInstrInfo();

	public abstract TargetRegisterInfo getRegInfo();

	public abstract TargetFrameInfo getFrameInfo();

	public TargetData getTargetData(){return dataLayout;}

	/**
     * Add passes to the specified pass manager to get assembly language code
     * emitted.  Typically this will involve several steps of code generation.
     * This method should return true if assembly emission is not supported.
     *
     * Note that: this method would be overriden by concrete subclass for
     * different backend.target, like IA32, Sparc.
     * @param pm
     * @param fast
     * @param asmOutStream
     * @param genFileType
     * @param optLevel
     * @return
     */
    public abstract boolean addPassesToEmitFile(FunctionPassManager pm,
            boolean fast,
            FileOutputStream asmOutStream,
            CodeGenFileType genFileType,
            CodeGenOpt optLevel);
}
