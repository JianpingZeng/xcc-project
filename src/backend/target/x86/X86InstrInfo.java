package backend.target.x86;

import backend.codegen.MachineInstr;
import backend.codegen.MachineInstrBuilder;
import backend.codegen.MachineOperand;
import backend.codegen.MachineOperand.UseType;
import backend.target.TargetInstrInfo;
import backend.target.TargetRegisterInfo;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86InstrInfo extends TargetInstrInfo implements X86InstrSets, X86RegsSet
{
	private static int[] ImplicitList1 = {EFLAGS};
	private static int[] ImplicitList2 = {ESP};
	private static int[] ImplicitList3 = {ESP, EFLAGS};
	private static int[] ImplicitList6 = {EAX, EBX, ECX, EDX};
	private static int[] ImplicitList9 = {EAX, ECX, EDX, FP0, FP1, FP2, FP3, FP4, FP5, FP6, ST0, EFLAGS};
	private static int[] ImplicitList10 = {EBP, ESP};
	private static int[] ImplicitList11 = {AL};
	private static int[] ImplicitList12 = {AL, AH, EFLAGS};
	private static int[] ImplicitList13 = {AX};
	private static int[] ImplicitList14 = {AX, DX, EFLAGS};
	private static int[] ImplicitList15 = {EAX};
	private static int[] ImplicitList16 = {EAX, EDX, EFLAGS};
	private static int[] ImplicitList17 = {AX, DX};
	private static int[] ImplicitList18 = {EAX, EDX};
	private static int[] ImplicitList19 = {CL};
	private static int[] ImplicitList20 = {AH};
	private static int[] ImplicitList21 = {ST0};

	/**
	 * A static array for holding all machine instruction for X86 architecture.
	 * <emp><b>Note that: the target-specified flags are not setted currently.</b></emp>
	 */
	private static TargetInstrDescriptor[] x86Insts =
	{
		new TargetInstrDescriptor(PHI, "PHI", -1, M_PSEUDO_FLAG, X86II.NoArg, null, null),
		new TargetInstrDescriptor(NOOP, "nop", -1, M_PSEUDO_FLAG, X86II.RawFrm|X86II.NoArg, null, null),
		new TargetInstrDescriptor(ADJCALLSTACKDOWN, "ADJCALLSTACKDOWN", 1, M_PSEUDO_FLAG, X86II.NoArg, ImplicitList2, ImplicitList3),
		new TargetInstrDescriptor(ADJCALLSTACKUP, "ADJCALLSTACKUP", 2, M_PSEUDO_FLAG, X86II.NoArg, ImplicitList2, ImplicitList3),
		new TargetInstrDescriptor(IMPLICIT_USE, "IMPLICIT_USE", -1, M_PSEUDO_FLAG, 0, null, null),
		new TargetInstrDescriptor(IMPLICIT_DEF, "IMPLICIT_DEF", -1, M_PSEUDO_FLAG, 0, null, null),

		new TargetInstrDescriptor(RET, "ret", 0, M_RET_FLAG|M_TERMINATOR_FLAG, X86II.RawFrm|X86II.NoArg, null, null),

		new TargetInstrDescriptor(JMP, "jmp", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, X86II.RawFrm|X86II.NoArg, null, null),
		new TargetInstrDescriptor(JB, "jb", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, X86II.RawFrm|X86II.NoArg, ImplicitList1, null),
		new TargetInstrDescriptor(JAE, "jae", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, X86II.RawFrm|X86II.NoArg, ImplicitList1, null),
		new TargetInstrDescriptor(JE, "je", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, X86II.RawFrm|X86II.NoArg, ImplicitList1, null),
		new TargetInstrDescriptor(JNE, "jne", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, X86II.RawFrm|X86II.NoArg, ImplicitList1, null),
		new TargetInstrDescriptor(JBE, "jbe", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, X86II.RawFrm|X86II.NoArg, ImplicitList1, null),
		new TargetInstrDescriptor(JA, "ja", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, X86II.RawFrm|X86II.NoArg, ImplicitList1, null),
		new TargetInstrDescriptor(JS, "js", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, X86II.RawFrm|X86II.NoArg, ImplicitList1, null),
		new TargetInstrDescriptor(JNS, "jns", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, X86II.RawFrm|X86II.NoArg, ImplicitList1, null),
		new TargetInstrDescriptor(JL, "jl", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, X86II.RawFrm|X86II.NoArg, ImplicitList1, null),
		new TargetInstrDescriptor(JGE, "jge", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, X86II.RawFrm|X86II.NoArg, ImplicitList1, null),
		new TargetInstrDescriptor(JLE, "jle", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, X86II.RawFrm|X86II.NoArg, ImplicitList1, null),
		new TargetInstrDescriptor(JG, "jg", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, X86II.RawFrm|X86II.NoArg, ImplicitList1, null),

		new TargetInstrDescriptor(CALLpcrel32, "call", 1, M_CALL_FLAG, X86II.RawFrm|X86II.NoArg, ImplicitList1, ImplicitList9),
		new TargetInstrDescriptor(CALLr32, "call", 1, M_CALL_FLAG, X86II.MRMS2r|X86II.Arg32, ImplicitList2, ImplicitList9),
		new TargetInstrDescriptor(CALLm32, "call", 1, M_LOAD_FLAG, X86II.MRMS2m|X86II.Arg32, ImplicitList2, ImplicitList9),
		new TargetInstrDescriptor(LEAVE, "leave", 0, M_LOAD_FLAG, X86II.RawFrm|X86II.Arg32, ImplicitList10, ImplicitList10),

		new TargetInstrDescriptor(BSWAPr32, "bswap", 2, M_2_ADDR_FLAG, X86II.AddRegFrm|X86II.Arg32, null, null),
		new TargetInstrDescriptor(XCHGrr8, "xchg", 2, 0, X86II.MRMDestReg|X86II.Arg8, null, null),
		new TargetInstrDescriptor(XCHGrr16, "xchg", 2, 0, X86II.MRMDestReg|X86II.Arg16, null, null),
		new TargetInstrDescriptor(XCHGrr32, "xchg", 2, 0, X86II.MRMDestReg|X86II.Arg32, null, null),
		new TargetInstrDescriptor(LEAr16, "lea", 6, 0, X86II.MRMSrcMem|X86II.Arg16, null, null),
		new TargetInstrDescriptor(LEAr32, "lea", 5, 0, X86II.MRMSrcMem|X86II.Arg32, null, null),

		new TargetInstrDescriptor(MOVrr8, "mov", 2, 0, X86II.MRMDestReg|X86II.Arg8, null, null),
		new TargetInstrDescriptor(MOVrr16, "mov", 2, 0, X86II.MRMDestReg|X86II.Arg16, null, null),
		new TargetInstrDescriptor(MOVrr32, "mov", 2, 0, X86II.MRMDestReg|X86II.Arg32, null, null),

		new TargetInstrDescriptor(MOVir8, "mov", 2, 0, X86II.AddRegFrm|X86II.Arg8, null, null),
		new TargetInstrDescriptor(MOVir16, "mov", 2, 0, X86II.AddRegFrm|X86II.Arg16, null, null),
		new TargetInstrDescriptor(MOVir32, "mov", 2, 0, X86II.AddRegFrm|X86II.Arg32, null, null),

		new TargetInstrDescriptor(MOVim8, "mov", 6, 0, X86II.MRMS0m|X86II.Arg8, null, null),
		new TargetInstrDescriptor(MOVim16, "mov", 6, 0, X86II.MRMS0m|X86II.Arg16, null, null),
		new TargetInstrDescriptor(MOVim32, "mov", 6, 0, X86II.MRMS0m|X86II.Arg32, null, null),

		new TargetInstrDescriptor(MOVmr8, "mov", 6, 0, X86II.MRMSrcMem|X86II.Arg8, null, null),
		new TargetInstrDescriptor(MOVmr16, "mov", 6, 0, X86II.MRMSrcMem|X86II.Arg16, null, null),
		new TargetInstrDescriptor(MOVmr32, "mov", 6, 0, X86II.MRMSrcMem|X86II.Arg32, null, null),

		new TargetInstrDescriptor(MOVrm8, "mov", 6, 0, X86II.MRMDestMem|X86II.Arg8, null, null),
		new TargetInstrDescriptor(MOVrm16, "mov", 6, 0, X86II.MRMDestMem|X86II.Arg16, null, null),
		new TargetInstrDescriptor(MOVrm32, "mov", 6, 0, X86II.MRMDestMem|X86II.Arg32, null, null),

		new TargetInstrDescriptor(MULr8, "mul", 1, 0, X86II.MRMS4r|X86II.Arg8, ImplicitList11, ImplicitList12),
		new TargetInstrDescriptor(MULr16, "mul", 1, 0, X86II.MRMS4r|X86II.Arg16, ImplicitList13, ImplicitList14),
		new TargetInstrDescriptor(MULr32, "mul", 1, 0, X86II.MRMS4r|X86II.Arg32, ImplicitList15, ImplicitList16),

		new TargetInstrDescriptor(DIVr8, "div", 1, 0, X86II.MRMS6r|X86II.Arg8, ImplicitList13, ImplicitList12),
		new TargetInstrDescriptor(DIVr16, "div", 1, 0, X86II.MRMS6r|X86II.Arg16, ImplicitList17, ImplicitList14),
		new TargetInstrDescriptor(DIVr32, "div", 1, 0, X86II.MRMS6r|X86II.Arg32, ImplicitList18, ImplicitList16),

		new TargetInstrDescriptor(IDIVr8, "idiv", 1, 0, X86II.MRMS7r|X86II.Arg8, ImplicitList13, ImplicitList12),
		new TargetInstrDescriptor(IDIVr16, "idiv", 1, 0, X86II.MRMS7r|X86II.Arg16, ImplicitList17, ImplicitList14),
		new TargetInstrDescriptor(IDIVr32, "idiv", 1, 0, X86II.MRMS7r|X86II.Arg32, ImplicitList18, ImplicitList16),

		new TargetInstrDescriptor(CBW, "cbw", 0, 0, X86II.RawFrm|X86II.Arg8, ImplicitList11, ImplicitList16),
		new TargetInstrDescriptor(CWD, "cwd", 0, 0, X86II.RawFrm|X86II.Arg8, ImplicitList13, ImplicitList17),
		new TargetInstrDescriptor(CDQ, "cdq", 0, 0, X86II.RawFrm|X86II.Arg8, ImplicitList15, ImplicitList18),

		new TargetInstrDescriptor(NEGr8, "neg", 2, M_2_ADDR_FLAG, X86II.MRMS3r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(NEGr16, "neg", 2, M_2_ADDR_FLAG, X86II.MRMS3r|X86II.Arg16, null, ImplicitList1),
		new TargetInstrDescriptor(NEGr32, "neg", 2, M_2_ADDR_FLAG, X86II.MRMS3r|X86II.Arg32, null, ImplicitList1),

		new TargetInstrDescriptor(NOTr8, "not", 2, M_2_ADDR_FLAG, X86II.MRMS2r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(NOTr16, "not", 2, M_2_ADDR_FLAG, X86II.MRMS2r|X86II.Arg16, null, ImplicitList1),
		new TargetInstrDescriptor(NOTr32, "not", 2, M_2_ADDR_FLAG, X86II.MRMS2r|X86II.Arg32, null, ImplicitList1),

		new TargetInstrDescriptor(INCr8, "inc", 2, M_2_ADDR_FLAG, X86II.MRMS0r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(INCr16, "inc", 2, M_2_ADDR_FLAG, X86II.MRMS0r|X86II.Arg16, null, ImplicitList1),
		new TargetInstrDescriptor(INCr32, "inc", 2, M_2_ADDR_FLAG, X86II.MRMS0r|X86II.Arg32, null, ImplicitList1),

		new TargetInstrDescriptor(DECr8, "dec", 2, M_2_ADDR_FLAG, X86II.MRMS1r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(DECr16, "dec", 2, M_2_ADDR_FLAG, X86II.MRMS1r|X86II.Arg16, null, ImplicitList1),
		new TargetInstrDescriptor(DECr32, "dec", 2, M_2_ADDR_FLAG, X86II.MRMS1r|X86II.Arg32, null, ImplicitList1),

		new TargetInstrDescriptor(ADDrr8, "add", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMDestReg|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(ADDrr16, "add", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMDestReg|X86II.Arg16, null, ImplicitList1),
		new TargetInstrDescriptor(ADDrr32, "add", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMDestReg|X86II.Arg32, null, ImplicitList1),
		new TargetInstrDescriptor(ADDri8, "add", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMS0r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(ADDri16, "add", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMS0r|X86II.Arg16, null, ImplicitList1),
		new TargetInstrDescriptor(ADDri32, "add", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMS0r|X86II.Arg32, null, ImplicitList1),
		new TargetInstrDescriptor(ADDri16b, "add", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMS0r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(ADDri32b, "add", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMS0r|X86II.Arg8, null, ImplicitList1),

		new TargetInstrDescriptor(ADCrr32, "adc", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMDestReg|X86II.Arg32, ImplicitList1, ImplicitList1),

		new TargetInstrDescriptor(SUBrr8, "sub", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMDestReg|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(SUBrr16, "sub", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMDestReg|X86II.Arg16, null, ImplicitList1),
		new TargetInstrDescriptor(SUBrr32, "sub", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMDestReg|X86II.Arg32, null, ImplicitList1),

		new TargetInstrDescriptor(SUBri8, "sub", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMS5r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(SUBri16, "sub", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMS5r|X86II.Arg16, null, ImplicitList1),
		new TargetInstrDescriptor(SUBri32, "sub", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMS5r|X86II.Arg32, null, ImplicitList1),
		new TargetInstrDescriptor(SUBri16b, "sub", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMS5r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(SUBri32b, "sub", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMS5r|X86II.Arg8, null, ImplicitList1),

		new TargetInstrDescriptor(SBBrr32, "sbb", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMDestReg|X86II.Arg32, ImplicitList1, ImplicitList1),

		new TargetInstrDescriptor(IMULrr16, "imul", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMSrcReg|X86II.Arg16, null, ImplicitList1),
		new TargetInstrDescriptor(IMULrr32, "imul", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMSrcReg|X86II.Arg32, null, ImplicitList1),
		new TargetInstrDescriptor(IMULri16, "imul", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMSrcReg|X86II.Arg16, null, ImplicitList1),
		new TargetInstrDescriptor(IMULri32, "imul", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMSrcReg|X86II.Arg32, null, ImplicitList1),
		new TargetInstrDescriptor(IMULri16b, "imul", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMSrcReg|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(IMULri32b, "imul", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, X86II.MRMSrcReg|X86II.Arg8, null, ImplicitList1),

		new TargetInstrDescriptor(ANDrr8, "and", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMDestReg|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(ANDrr16, "and", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMDestReg|X86II.Arg16, null, ImplicitList1),
		new TargetInstrDescriptor(ANDrr32, "and", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMDestReg|X86II.Arg32, null, ImplicitList1),
		new TargetInstrDescriptor(ANDri8, "and", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMS4r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(ANDri16, "and", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMS4r|X86II.Arg16, null, ImplicitList1),
		new TargetInstrDescriptor(ANDri32, "and", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMS4r|X86II.Arg32, null, ImplicitList1),
		new TargetInstrDescriptor(ANDri16b, "and", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMS4r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(ANDri32b, "and", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMS4r|X86II.Arg8, null, ImplicitList1),

		new TargetInstrDescriptor(ORrr8, "or", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMDestReg|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(ORrr16, "or", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMDestReg|X86II.Arg16, null, ImplicitList1),
		new TargetInstrDescriptor(ORrr32, "or", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMDestReg|X86II.Arg32, null, ImplicitList1),
		new TargetInstrDescriptor(ORri8, "or", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMS1r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(ORri16, "or", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMS1r|X86II.Arg16, null, ImplicitList1),
		new TargetInstrDescriptor(ORri32, "or", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMS1r|X86II.Arg32, null, ImplicitList1),
		new TargetInstrDescriptor(ORri16b, "or", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMS1r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(ORri32b, "or", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMS1r|X86II.Arg8, null, ImplicitList1),

		new TargetInstrDescriptor(XORrr8, "xor", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMDestReg|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(XORrr16, "xor", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMDestReg|X86II.Arg16, null, ImplicitList1),
		new TargetInstrDescriptor(XORrr32, "xor", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMDestReg|X86II.Arg32, null, ImplicitList1),
		new TargetInstrDescriptor(XORri8, "xor", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMS6r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(XORri16, "xor", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMS6r|X86II.Arg16, null, ImplicitList1),
		new TargetInstrDescriptor(XORri32, "xor", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMS6r|X86II.Arg32, null, ImplicitList1),
		new TargetInstrDescriptor(XORri16b, "xor", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMS6r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(XORri32b, "xor", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, X86II.MRMS6r|X86II.Arg8, null, ImplicitList1),

		new TargetInstrDescriptor(TESTrr8, "test", 2, 0, X86II.MRMDestReg|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(TESTrr16, "test", 2, 0, X86II.MRMDestReg|X86II.Arg16, null, ImplicitList1),
		new TargetInstrDescriptor(TESTrr32, "test", 2, 0, X86II.MRMDestReg|X86II.Arg32, null, ImplicitList1),
		new TargetInstrDescriptor(TESTri8, "test", 2, 0, X86II.MRMS0r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(TESTri16, "test", 2, 0, X86II.MRMS0r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(TESTri32, "test", 2, 0, X86II.MRMS0r|X86II.Arg8, null, ImplicitList1),

		new TargetInstrDescriptor(SHLrCL8, "shl", 2, 0, X86II.MRMS4r|X86II.Arg8, ImplicitList19, ImplicitList1), // R8 <= cl.
		new TargetInstrDescriptor(SHLrCL16, "shl", 2, 0, X86II.MRMS4r|X86II.Arg8, ImplicitList19, ImplicitList1),
		new TargetInstrDescriptor(SHLrCL32, "shl", 2, 0, X86II.MRMS4r|X86II.Arg8, ImplicitList19, ImplicitList1),
		new TargetInstrDescriptor(SHLir8, "shl", 3, 0, X86II.MRMS4r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(SHLir16, "shl", 3, 0, X86II.MRMS4r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(SHLir32, "shl", 3, 0, X86II.MRMS4r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(SHRrCL8, "shr", 2, 0, X86II.MRMS5r|X86II.Arg8, ImplicitList19, ImplicitList1),
		new TargetInstrDescriptor(SHRrCL16, "shr", 2, 0, X86II.MRMS5r|X86II.Arg8, ImplicitList19, ImplicitList1),
		new TargetInstrDescriptor(SHRrCL32, "shr", 2, 0, X86II.MRMS5r|X86II.Arg8, ImplicitList19, ImplicitList1),
		new TargetInstrDescriptor(SHRir8, "shr", 3, 0, X86II.MRMS5r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(SHRir16, "shr", 3, 0, X86II.MRMS5r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(SHRir32, "shr", 3, 0, X86II.MRMS5r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(SARrCL8, "sar", 2, 0, X86II.MRMS7r|X86II.Arg8, ImplicitList19, ImplicitList1),
		new TargetInstrDescriptor(SARrCL16, "sar", 2, 0, X86II.MRMS7r|X86II.Arg8, ImplicitList19, ImplicitList1),
		new TargetInstrDescriptor(SARrCL32, "sar", 2, 0, X86II.MRMS7r|X86II.Arg8, ImplicitList19, ImplicitList1),
		new TargetInstrDescriptor(SARir8, "sar", 3, 0, X86II.MRMS7r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(SARir16, "sar", 3, 0, X86II.MRMS7r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(SARir32, "sar", 3, 0, X86II.MRMS7r|X86II.Arg8, null, ImplicitList1),

		new TargetInstrDescriptor(SHLDrCL32, "shld", 3, 0, X86II.MRMDestReg|X86II.Arg8, ImplicitList19, ImplicitList1),
		new TargetInstrDescriptor(SHLDir32, "shld", 4, 0, X86II.MRMDestReg|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(SHRDrCL32, "shrd", 3, 0, X86II.MRMDestReg|X86II.Arg8, ImplicitList19, ImplicitList1),
		new TargetInstrDescriptor(SHRDir32, "shrd", 4, 0, X86II.MRMDestReg|X86II.Arg8, null, ImplicitList1),

		new TargetInstrDescriptor(SAHF, "sahf", 0, 0, X86II.RawFrm|X86II.Arg8, ImplicitList20, ImplicitList1),
		new TargetInstrDescriptor(SETBr, "setb", 1, 0, X86II.MRMS0r|X86II.Arg8, ImplicitList1, null),
		new TargetInstrDescriptor(SETAEr, "setae", 1, 0, X86II.MRMS0r|X86II.Arg8, ImplicitList1, null),
		new TargetInstrDescriptor(SETEr, "sete", 1, 0, X86II.MRMS0r|X86II.Arg8, ImplicitList1, null),
		new TargetInstrDescriptor(SETNEr, "setne", 1, 0, X86II.MRMS0r|X86II.Arg8, ImplicitList1, null),
		new TargetInstrDescriptor(SETBEr, "setbe", 1, 0, X86II.MRMS0r|X86II.Arg8, ImplicitList1, null),
		new TargetInstrDescriptor(SETAr, "seta", 1, 0, X86II.MRMS0r|X86II.Arg8, ImplicitList1, null),
		new TargetInstrDescriptor(SETSr, "sets", 1, 0, X86II.MRMS0r|X86II.Arg8, ImplicitList1, null),
		new TargetInstrDescriptor(SETNSr, "setns", 1, 0, X86II.MRMS0r|X86II.Arg8, ImplicitList1, null),
		new TargetInstrDescriptor(SETLr, "setl", 1, 0, X86II.MRMS0r|X86II.Arg8, ImplicitList1, null),
		new TargetInstrDescriptor(SETGEr, "setge", 1, 0, X86II.MRMS0r|X86II.Arg8, ImplicitList1, null),
		new TargetInstrDescriptor(SETLEr, "setle", 1, 0, X86II.MRMS0r|X86II.Arg8, ImplicitList1, null),
		new TargetInstrDescriptor(SETGr, "setg", 1, 0, X86II.MRMS0r|X86II.Arg8, ImplicitList1, null),

		new TargetInstrDescriptor(CMOVErr16, "cmov", 3, 0, X86II.MRMSrcReg|X86II.Arg16, ImplicitList1, null),
		new TargetInstrDescriptor(CMOVNErr32, "cmovne", 3, 0, X86II.MRMSrcReg|X86II.Arg32, ImplicitList1, null),

		new TargetInstrDescriptor(CMPrr8, "cmp", 2, 0, X86II.MRMDestReg|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(CMPrr16, "cmp", 2, 0, X86II.MRMDestReg|X86II.Arg16, null, ImplicitList1),
		new TargetInstrDescriptor(CMPrr32, "cmp", 2, 0, X86II.MRMDestReg|X86II.Arg32, null, ImplicitList1),
		new TargetInstrDescriptor(CMPri8, "cmp", 2, 0, X86II.MRMS7r|X86II.Arg8, null, ImplicitList1),
		new TargetInstrDescriptor(CMPri16, "cmp", 2, 0, X86II.MRMS7r|X86II.Arg16, null, ImplicitList1),
		new TargetInstrDescriptor(CMPri32, "cmp", 2, 0, X86II.MRMS7r|X86II.Arg32, null, ImplicitList1),

		new TargetInstrDescriptor(MOVSXr16r8, "movsx", 2, 0, X86II.MRMSrcReg|X86II.Arg8, null, null),
		new TargetInstrDescriptor(MOVSXr32r8, "movsx", 2, 0, X86II.MRMSrcReg|X86II.Arg8, null, null),
		new TargetInstrDescriptor(MOVSXr32r16, "movsx", 2, 0, X86II.MRMSrcReg|X86II.Arg8, null, null),
		new TargetInstrDescriptor(MOVZXr16r8, "movzx", 2, 0, X86II.MRMSrcReg|X86II.Arg8, null, null),
		new TargetInstrDescriptor(MOVZXr32r8, "movzx", 2, 0, X86II.MRMSrcReg|X86II.Arg8, null, null),
		new TargetInstrDescriptor(MOVZXr32r16, "movzx", 2, 0, X86II.MRMSrcReg|X86II.Arg8, null, null),

		// those floating-point instruction maybe are not correct.
		new TargetInstrDescriptor(FpMOV, "FpMOV", 2, 0, X86II.Pseudo|X86II.SpecialFP|X86II.ArgF80, null, null),
		new TargetInstrDescriptor(FpADD, "FpADD", 3, 0, X86II.Pseudo|X86II.TwoArgFP|X86II.ArgF80, null, null),
		new TargetInstrDescriptor(FpSUB, "FpSUB", 3, 0, X86II.Pseudo|X86II.TwoArgFP|X86II.ArgF80, null, null),
		new TargetInstrDescriptor(FpMUL, "FpMUL", 3, 0, X86II.Pseudo|X86II.TwoArgFP|X86II.ArgF80, null, null),
		new TargetInstrDescriptor(FpDIV, "FpDIV", 3, 0, X86II.Pseudo|X86II.TwoArgFP|X86II.ArgF80, null, null),
		new TargetInstrDescriptor(FpUCOM, "FpUCOM", 2, 0, X86II.Pseudo|X86II.TwoArgFP|X86II.ArgF80, null, null),
		new TargetInstrDescriptor(FpGETRESULT, "FpGETRESULT", 1, 0, X86II.Pseudo|X86II.SpecialFP|X86II.ArgF80, null, null),
		new TargetInstrDescriptor(FpSETRESULT, "FpSETRESULT", 1, 0, X86II.Pseudo|X86II.SpecialFP|X86II.ArgF80, null, null),

		new TargetInstrDescriptor(FLDrr, "fld", 1, 0, X86II.AddRegFrm|X86II.ArgF80, null, null),
		new TargetInstrDescriptor(FLDr32, "fld", 1, 0, X86II.MRMS0m|X86II.ZeroArgFP|X86II.ArgF32, null, null),
		new TargetInstrDescriptor(FLDr64, "fld", 1, 0, X86II.MRMS0m|X86II.ZeroArgFP|X86II.ArgF64, null, null),
		new TargetInstrDescriptor(FLDr80, "fld", 1, 0, X86II.MRMS5m|X86II.ZeroArgFP|X86II.ArgF64, null, null),
		new TargetInstrDescriptor(FILDr16, "fild", 1, 0, X86II.MRMS0m|X86II.ZeroArgFP|X86II.Arg16, null, null),
		new TargetInstrDescriptor(FILDr32, "fild", 1, 0, X86II.MRMS0m|X86II.ZeroArgFP|X86II.Arg32, null, null),
		new TargetInstrDescriptor(FILDr64, "fild", 1, 0, X86II.MRMS0m|X86II.ZeroArgFP|X86II.Arg64, null, null),

		new TargetInstrDescriptor(FSTr32, "fst", 1, 0, X86II.MRMS2m|X86II.OneArgFP|X86II.ArgF32, null, null),
		new TargetInstrDescriptor(FSTr64, "fst", 1, 0, X86II.MRMS2m|X86II.OneArgFP|X86II.ArgF64, null, null),
		new TargetInstrDescriptor(FSTPr32, "fstp", 1, 0, X86II.MRMS3m|X86II.OneArgFP|X86II.ArgF32, null, null),
		new TargetInstrDescriptor(FSTPr64, "fstp", 1, 0, X86II.MRMS3m|X86II.OneArgFP|X86II.ArgF64, null, null),
		new TargetInstrDescriptor(FSTPr80, "fstp", 1, 0, X86II.MRMS7m|X86II.OneArgFP|X86II.ArgF80, null, null),
		new TargetInstrDescriptor(FSTrr, "fst", 1, 0, X86II.AddRegFrm|X86II.ArgF80, null, null),
		new TargetInstrDescriptor(FSTPrr, "fstp", 1, 0, X86II.AddRegFrm|X86II.ArgF80, null, null),

		new TargetInstrDescriptor(FISTr16, "fist", 1, 0, X86II.MRMS2m|X86II.OneArgFP|X86II.Arg16, null, null),
		new TargetInstrDescriptor(FISTr32, "fist", 1, 0, X86II.MRMS2m|X86II.OneArgFP|X86II.Arg32, null, null),
		new TargetInstrDescriptor(FISTr64, "fist", 1, 0, X86II.MRMS2m|X86II.OneArgFP|X86II.Arg64, null, null),
		new TargetInstrDescriptor(FISTPr16, "fistp", 1, 0, X86II.MRMS3m|X86II.Arg16, null, null),
		new TargetInstrDescriptor(FISTPr32, "fistp", 1, 0, X86II.MRMS3m|X86II.Arg32, null, null),
		new TargetInstrDescriptor(FISTPr64, "fistpll", 1, 0, X86II.MRMS7m|X86II.OneArgFP|X86II.Arg64, null, null),
		new TargetInstrDescriptor(FXCH, "fxch", 2, 0, X86II.AddRegFrm|X86II.ArgF80, null, null),

		new TargetInstrDescriptor(FLD0, "fldz", 0, 0, X86II.RawFrm|X86II.ZeroArgFP|X86II.ArgF80, null, null),
		new TargetInstrDescriptor(FLD1, "fld1", 0, 0, X86II.RawFrm|X86II.ZeroArgFP|X86II.ArgF80, null, null),

		new TargetInstrDescriptor(FADDST0r, "fadd", 1, 0, X86II.AddRegFrm|X86II.ArgF80, ImplicitList21, ImplicitList21),
		new TargetInstrDescriptor(FADDrST0, "fadd", 1, 0, X86II.AddRegFrm|X86II.ArgF80, ImplicitList21, null),
		new TargetInstrDescriptor(FADDPrST0, "faddp", 1, 0, X86II.AddRegFrm|X86II.ArgF80, ImplicitList21, null),

		new TargetInstrDescriptor(FSUBRST0r, "fsubr", 1, 0, X86II.AddRegFrm|X86II.ArgF80, ImplicitList21, ImplicitList21),
		new TargetInstrDescriptor(FSUBrST0, "fsub", 1, 0, X86II.AddRegFrm|X86II.ArgF80, ImplicitList21, null),
		new TargetInstrDescriptor(FSUBPrST0, "fsubp", 1, 0, X86II.AddRegFrm|X86II.ArgF80, ImplicitList21, null),

		new TargetInstrDescriptor(FSUBST0r, "fsub", 1, 0, X86II.AddRegFrm|X86II.ArgF80, ImplicitList21, ImplicitList21),
		new TargetInstrDescriptor(FSUBRrST0, "fsubr", 1, 0, X86II.AddRegFrm|X86II.ArgF80, ImplicitList21, null),
		new TargetInstrDescriptor(FSUBRPrST0, "fsubrp", 1, 0, X86II.AddRegFrm|X86II.ArgF80, ImplicitList21, null),

		new TargetInstrDescriptor(FMULST0r, "fmul", 1, 0, X86II.AddRegFrm|X86II.ArgF80, ImplicitList21, ImplicitList21),
		new TargetInstrDescriptor(FMULrST0, "fmul", 1, 0, X86II.AddRegFrm|X86II.ArgF80, ImplicitList21, null),
		new TargetInstrDescriptor(FMULPrST0, "fmulp", 1, 0, X86II.AddRegFrm|X86II.ArgF80, ImplicitList21, null),

		new TargetInstrDescriptor(FDIVRST0r, "fdivr", 1, 0, X86II.AddRegFrm|X86II.ArgF80, ImplicitList21, ImplicitList21),
		new TargetInstrDescriptor(FDIVrST0, "fdiv", 1, 0, X86II.AddRegFrm|X86II.ArgF80, ImplicitList21, null),
		new TargetInstrDescriptor(FDIVPrST0, "fdivp", 1, 0, X86II.AddRegFrm|X86II.ArgF80, ImplicitList21, null),

		new TargetInstrDescriptor(FDIVST0r, "fdiv", 1, 0, X86II.AddRegFrm|X86II.ArgF80, ImplicitList21, ImplicitList21),
		new TargetInstrDescriptor(FDIVRrST0, "fdivr", 1, 0, X86II.AddRegFrm|X86II.ArgF80, ImplicitList21, null),
		new TargetInstrDescriptor(FDIVRPrST0, "fdivrp", 1, 0, X86II.AddRegFrm|X86II.ArgF80, ImplicitList21, null),

		new TargetInstrDescriptor(FUCOMr, "fucom", 1, 0, X86II.AddRegFrm|X86II.ArgF80, ImplicitList21, null),
		new TargetInstrDescriptor(FUCOMPr, "fucomp", 1, 0, X86II.AddRegFrm|X86II.ArgF80, ImplicitList21, null),
		new TargetInstrDescriptor(FUCOMPPr, "fucompp", 1, 0, X86II.RawFrm|X86II.ArgF80, ImplicitList21, null),

		new TargetInstrDescriptor(FNSTSWr8, "fnstsw", 0, 0, X86II.RawFrm|X86II.ArgF80, null, ImplicitList13),
		new TargetInstrDescriptor(FNSTCWm16, "fnstcw", 4, 0, X86II.MRMS7m|X86II.Arg16, null, null),
		new TargetInstrDescriptor(FLDCWm16, "fldcw", 4, 0, X86II.MRMS5m|X86II.Arg16, null, null),
		// Others floating-point instruction are suspending currently,
		// it will be enabled after making sure that
		// sse and mmx instructions are unvailable.
	};

	private X86RegisterInfo registerInfo;
	public X86InstrInfo()
	{
		super(x86Insts);
		registerInfo = new X86RegisterInfo();
	}

	/**
	 * TargetInstrInfo is a superset of MRegister info.  As such, whenever
	 * a client has an instance of instruction info, it should always be able
	 * to get register info as well (through this method).
	 */
	public TargetRegisterInfo getRegisterInfo() { return registerInfo; }

	/**
	 * Returns the target's implementation of NOP, which is
	 * usually a pseudo-instruction, implemented by a degenerate version of
	 * another instruction, e.g. X86: xchg ax, ax;
	 *
	 * @return
	 */
	@Override
	public MachineInstr createNOPinstr()
	{
		return MachineInstrBuilder.buildMI(X86InstrSets.XCHGrr16, 2).
				addReg(X86RegsSet.AX, UseType.UseAndDef).
				addReg( X86RegsSet.AX, UseType.UseAndDef)
				.getMInstr();
	}

	/**
	 * Not having a special NOP opcode, we need to know if a given
	 * instruction is interpreted as an `official' NOP instr, i.e., there may be
	 * more than one way to `do nothing' but only one canonical way to slack off.
	 *
	 * @param mi
	 * @return
	 */
	@Override
	public boolean isNOPinstr(MachineInstr mi)
	{
		if (mi.getOpCode() == XCHGrr16)
		{
			MachineOperand op1 = mi.getOperand(0);
			MachineOperand op2 = mi.getOperand(1);
			if (op1.isMachineRegister() && op1.getMachineRegNum() == AX
					&& op2.isMachineRegister() && op2.getMachineRegNum() == AX)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * This function returns the "base" X86 opcode for the
	 * specified opcode number.
	 * @param opcode
	 * @return
	 */
	public int getBaseOpcodeFor(int opcode)
	{
		return get(opcode).tSFlags >> X86II.OpcodeShift;
	}
}
