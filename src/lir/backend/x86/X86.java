package lir.backend.x86;

import lir.backend.Architecture;
import lir.backend.ByteOrder;
import lir.backend.TargetMachine;
import lir.ci.LIRRegister;
import lir.ci.LIRRegisterValue;
import static lir.ci.LIRKind.Long;
import static lir.ci.LIRRegister.RegisterFlag.Byte;
import static lir.ci.LIRRegister.RegisterFlag.CPU;
import static lir.ci.LIRRegister.RegisterFlag.FPU;

/**
 * This file defines a class named of {@code X86} which represents the X86 architecture,
 * including available generable register, frame register and stack register,
 * moreover, there are many register for advanced vector computation. Please visit
 * the website <a href="http://www.sco.com/developers/devspecs/abi386-4.pdf">
 *     System V Application Binary Interface</a> for more details.
 * @author Xlous.zeng
 */
public final class X86 extends Architecture
{
	// general purpose register for cpu
	public static final LIRRegister eax = new LIRRegister(0, 0, 4, "eax", CPU,
			Byte);
	public static final LIRRegister edx = new LIRRegister(1, 1, 4, "edx", CPU,
			Byte);
	public static final LIRRegister ecx = new LIRRegister(2, 2, 4, "ecx", CPU,
			Byte);
	public static final LIRRegister ebx = new LIRRegister(3, 3, 4, "ebx", CPU,
			Byte);
	public static final LIRRegister ebp = new LIRRegister(4, 4, 4, "ebp", CPU,
			Byte);
	public static final LIRRegister esi = new LIRRegister(5, 5, 4, "esi", CPU,
			Byte);
	public static final LIRRegister edi = new LIRRegister(6, 6, 4, "edi", CPU,
			Byte);
	public static final LIRRegister esp = new LIRRegister(7, 7, 4, "esp", CPU,
			Byte);

	/**
	 * All registers used in cpu.
	 */
	public static final LIRRegister[] CPU_LIR_REGISTERs = new LIRRegister[]
			{eax, edx, ecx, ebx, ebp, esi, edi, esp};

	// xmm register
	public static final LIRRegister xmm0 = new LIRRegister(8, 0, 16, "xmm0", FPU);
	public static final LIRRegister xmm1 = new LIRRegister(9, 1, 16, "xmm1", FPU);
	public static final LIRRegister xmm2 = new LIRRegister(10, 2, 16, "xmm2", FPU);
	public static final LIRRegister xmm3 = new LIRRegister(11, 3, 16, "xmm3", FPU);
	public static final LIRRegister xmm4 = new LIRRegister(12, 4, 16, "xmm4", FPU);
	public static final LIRRegister xmm5 = new LIRRegister(13, 5, 16, "xmm5", FPU);
	public static final LIRRegister xmm6 = new LIRRegister(14, 6, 16, "xmm6", FPU);
	public static final LIRRegister xmm7 = new LIRRegister(15, 7, 16, "xmm7", FPU);

	/**
	 * All float point registers.
	 */
	public static final LIRRegister[] FLOAT_LIR_REGISTERs = new LIRRegister[]
			{ xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7 };

	/**
	 * All registers including integer register and float point in {@code X86}
	 * architecture excepts that {@code eip} register.
	 */
	public static final LIRRegister[] CPU_FLOAT_LIR_REGISTERs =
			new LIRRegister[]
			{eax, edx, ecx, ebx, ebp, esi, edi, esp, xmm0, xmm1, xmm2, xmm3,
					xmm4, xmm5, xmm6, xmm7 };

	/**
	 * This register just used for constructing instruction-relative address.
	 */
	public static final LIRRegister eip = new LIRRegister(16, -1, 0, "rip");

	/**
	 * All registers including integer register and float point in {@code X86}
	 * architecture.
	 */
	public static final LIRRegister[] ALL_LIR_REGISTERs = new LIRRegister[]
			{eax, edx, ecx, ebx, ebp, esi, edi, esp, xmm0, xmm1, xmm2, xmm3,
					xmm4, xmm5, xmm6, xmm7, eip};

	public static final LIRRegisterValue ESP = esp.asValue(Long);

	public X86()
	{
		super("X86", 4, ByteOrder.LittleEndian,
				ALL_LIR_REGISTERs, 1, xmm7.encoding+1, 4);
	}

	@Override public boolean isX86()
	{
		return true;
	}

	@Override public boolean twoOperandMode()
	{
		return true;
	}

	public static TargetMachine target()
	{
		return new TargetMachine(new X86(), false, 4, 4, 4096,
				1<<20, false, false, false);
	}
}
