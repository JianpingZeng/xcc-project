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
 * @author Jianping Zeng
 */
public final class X86 extends Architecture
{
	// general purpose register for cpu
	public static final LIRRegister eax = new LIRRegister(0, 0, 4, "eax", CPU,
			Byte);
	public static final LIRRegister edx = new LIRRegister(1, 0, 4, "edx", CPU,
			Byte);
	public static final LIRRegister ecx = new LIRRegister(2, 0, 4, "ecx", CPU,
			Byte);
	public static final LIRRegister ebx = new LIRRegister(3, 0, 4, "ebx", CPU,
			Byte);
	public static final LIRRegister ebp = new LIRRegister(4, 0, 4, "ebp", CPU,
			Byte);
	public static final LIRRegister esi = new LIRRegister(5, 0, 4, "esi", CPU,
			Byte);
	public static final LIRRegister edi = new LIRRegister(6, 0, 4, "edi", CPU,
			Byte);
	public static final LIRRegister esp = new LIRRegister(7, 0, 4, "esp", CPU,
			Byte);

	/**
	 * All registers used in cpu.
	 */
	public static final LIRRegister[] CPU_LIR_REGISTERs = new LIRRegister[]
			{eax, edx, ecx, ebx, ebp, esi, edi, esp};

	// float point register
	public static final LIRRegister st0 = new LIRRegister(8, 0, 4, "st0", FPU);
	public static final LIRRegister st1 = new LIRRegister(9, 0, 4, "st1", FPU);
	public static final LIRRegister st2 = new LIRRegister(10, 0, 4, "st2", FPU);
	public static final LIRRegister st3 = new LIRRegister(11, 0, 4, "st3", FPU);
	public static final LIRRegister st4 = new LIRRegister(12, 0, 4, "st4", FPU);
	public static final LIRRegister st5 = new LIRRegister(13, 0, 4, "st5", FPU);
	public static final LIRRegister st6 = new LIRRegister(14, 0, 4, "st6", FPU);
	public static final LIRRegister st7 = new LIRRegister(15, 0, 4, "st7", FPU);

	/**
	 * All float point registers.
	 */
	public static final LIRRegister[] FLOAT_LIR_REGISTERs = new LIRRegister[]
			{st0, st1, st2, st3, st4, st5, st6, st7};

	/**
	 * All registers including integer register and float point in {@code X86}
	 * architecture excepts that {@code eip} register.
	 */
	public static final LIRRegister[] CPU_FLOAT_LIR_REGISTERs =
			new LIRRegister[]
			{eax, edx, ecx, ebx, ebp, esi, edi, esp,
			st0, st1, st2, st3, st4, st5, st6, st7};

	/**
	 * This register just used for constructing instruction-relative address.
	 */
	public static final LIRRegister eip = new LIRRegister(16, -1, 0, "rip");

	/**
	 * All registers including integer register and float point in {@code X86}
	 * architecture.
	 */
	public static final LIRRegister[] ALL_LIR_REGISTERs = new LIRRegister[]
			{eax, edx, ecx, ebx, ebp, esi, edi, esp,
					st0, st1, st2, st3, st4, st5, st6, st7, eip};

	public static final LIRRegisterValue ESP = esp.asValue(Long);

	public X86()
	{
		super("X86", 4, ByteOrder.LittleEndian,
				ALL_LIR_REGISTERs, 1, st7.encoding+1, 4);
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
