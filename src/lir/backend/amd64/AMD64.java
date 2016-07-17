package lir.backend.amd64;

import static lir.ci.LIRKind.Long;
import static lir.ci.LIRRegister.RegisterFlag.Byte;
import static lir.ci.LIRRegister.RegisterFlag.CPU;
import static lir.ci.LIRRegister.RegisterFlag.FPU;
import lir.backend.Architecture;
import lir.backend.ByteOrder;
import lir.backend.TargetMachine;
import lir.ci.LIRRegister;
import lir.ci.LIRRegisterValue;

/**
 * Represents the AMD64 architecture.
 */
public class AMD64 extends Architecture
{
	// General purpose CPU LIRRegisters

	// caller-saved register group
	public static final LIRRegister rax = new LIRRegister(0, 0, 8, "rax", CPU,
			Byte);
	public static final LIRRegister rcx = new LIRRegister(1, 1, 8, "rcx", CPU,
			Byte);
	public static final LIRRegister rdx = new LIRRegister(2, 2, 8, "rdx", CPU,
			Byte);

	// callee-saved register group
	public static final LIRRegister rbx = new LIRRegister(3, 3, 8, "rbx", CPU,
			Byte);
	public static final LIRRegister rsp = new LIRRegister(4, 4, 8, "rsp", CPU,
			Byte);
	public static final LIRRegister rbp = new LIRRegister(5, 5, 8, "rbp", CPU,
			Byte);

	public static final LIRRegister rsi = new LIRRegister(6, 6, 8, "rsi", CPU,
			Byte);
	public static final LIRRegister rdi = new LIRRegister(7, 7, 8, "rdi", CPU,
			Byte);

	public static final LIRRegister r8 = new LIRRegister(8, 8, 8, "r8", CPU,
			Byte);
	public static final LIRRegister r9 = new LIRRegister(9, 9, 8, "r9", CPU,
			Byte);
	public static final LIRRegister r10 = new LIRRegister(10, 10, 8, "r10", CPU,
			Byte);
	public static final LIRRegister r11 = new LIRRegister(11, 11, 8, "r11", CPU,
			Byte);
	public static final LIRRegister r12 = new LIRRegister(12, 12, 8, "r12", CPU,
			Byte);
	public static final LIRRegister r13 = new LIRRegister(13, 13, 8, "r13", CPU,
			Byte);
	public static final LIRRegister r14 = new LIRRegister(14, 14, 8, "r14", CPU,
			Byte);
	public static final LIRRegister r15 = new LIRRegister(15, 15, 8, "r15", CPU,
			Byte);

	public static final LIRRegister[] CPU_LIR_REGISTERs = { rax, rcx, rdx, rbx, rsp,
			rbp, rsi, rdi, r8, r9, r10, r11, r12, r13, r14, r15 };

	// XMM LIRRegisters
	public static final LIRRegister xmm0 = new LIRRegister(16, 0, 8, "xmm0", FPU);
	public static final LIRRegister xmm1 = new LIRRegister(17, 1, 8, "xmm1", FPU);
	public static final LIRRegister xmm2 = new LIRRegister(18, 2, 8, "xmm2", FPU);
	public static final LIRRegister xmm3 = new LIRRegister(19, 3, 8, "xmm3", FPU);
	public static final LIRRegister xmm4 = new LIRRegister(20, 4, 8, "xmm4", FPU);
	public static final LIRRegister xmm5 = new LIRRegister(21, 5, 8, "xmm5", FPU);
	public static final LIRRegister xmm6 = new LIRRegister(22, 6, 8, "xmm6", FPU);
	public static final LIRRegister xmm7 = new LIRRegister(23, 7, 8, "xmm7", FPU);

	public static final LIRRegister xmm8 = new LIRRegister(24, 8, 8, "xmm8", FPU);
	public static final LIRRegister xmm9 = new LIRRegister(25, 9, 8, "xmm9", FPU);
	public static final LIRRegister xmm10 = new LIRRegister(26, 10, 8, "xmm10",
			FPU);
	public static final LIRRegister xmm11 = new LIRRegister(27, 11, 8, "xmm11",
			FPU);
	public static final LIRRegister xmm12 = new LIRRegister(28, 12, 8, "xmm12",
			FPU);
	public static final LIRRegister xmm13 = new LIRRegister(29, 13, 8, "xmm13",
			FPU);
	public static final LIRRegister xmm14 = new LIRRegister(30, 14, 8, "xmm14",
			FPU);
	public static final LIRRegister xmm15 = new LIRRegister(31, 15, 8, "xmm15",
			FPU);

	public static final LIRRegister[] XMM_LIR_REGISTERs = { xmm0, xmm1, xmm2, xmm3,
			xmm4, xmm5, xmm6, xmm7, xmm8, xmm9, xmm10, xmm11, xmm12, xmm13,
			xmm14, xmm15 };

	public static final LIRRegister[] CPUXMM_LIR_REGISTERs = { rax, rcx, rdx, rbx,
			rsp, rbp, rsi, rdi, r8, r9, r10, r11, r12, r13, r14, r15, xmm0,
			xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7, xmm8, xmm9, xmm10, xmm11,
			xmm12, xmm13, xmm14, xmm15 };

	/**
	 * LIRRegister used to construct an instruction-relative address.
	 */
	public static final LIRRegister rip = new LIRRegister(32, -1, 0, "rip");

	public static final LIRRegister[] ALL_LIR_REGISTERs = { rax, rcx, rdx, rbx, rsp,
			rbp, rsi, rdi, r8, r9, r10, r11, r12, r13, r14, r15, xmm0, xmm1,
			xmm2, xmm3, xmm4, xmm5, xmm6, xmm7, xmm8, xmm9, xmm10, xmm11, xmm12,
			xmm13, xmm14, xmm15, rip };

	public static final LIRRegisterValue RSP = rsp.asValue(Long);

	public AMD64()
	{
		super("AMD64", 8, ByteOrder.LittleEndian, ALL_LIR_REGISTERs, 1,
				r15.encoding + 1, 8);
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
		return new TargetMachine(new AMD64(), false, 8, 8, 4096, 1<<20, false, false, false);
	}
}
