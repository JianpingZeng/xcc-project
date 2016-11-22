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
	private static int[] ImplicitList21 = {};

	/**
	 * A static array for holding all machine instruction for X86 architecture.
	 * <emp><b>Note that: the target-specified flags are not setted currently.</b></emp>
	 */
	private static TargetInstrDescriptor[] x86Insts =
	{
		new TargetInstrDescriptor(PHI, "PHI", -1, M_DUMMY_PHI_FLAG|M_PSEUDO_FLAG, 0, null, null),
		new TargetInstrDescriptor(NOOP, "NOOP", -1, M_NOP_FLAG|M_PSEUDO_FLAG, 0, null, null),
		new TargetInstrDescriptor(ADJCALLSTACKDOWN, "ADJCALLSTACKDOWN", 1, 0, 0, ImplicitList2, ImplicitList3),
		new TargetInstrDescriptor(ADJCALLSTACKUP, "ADJCALLSTACKUP", 2, 0, 0, ImplicitList2, ImplicitList3),
		new TargetInstrDescriptor(IMPLICIT_USE, "IMPLICIT_USE", -1, M_PSEUDO_FLAG, 0, null, null),
		new TargetInstrDescriptor(IMPLICIT_DEF, "IMPLICIT_DEF", -1, M_PSEUDO_FLAG, 0, null, null),

		new TargetInstrDescriptor(RET, "RET", 0, M_RET_FLAG|M_TERMINATOR_FLAG, 0, null, null),

		new TargetInstrDescriptor(JMP, "JMP", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, null, null),
		new TargetInstrDescriptor(JB, "JB", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),
		new TargetInstrDescriptor(JAE, "JAE", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),
		new TargetInstrDescriptor(JE, "JE", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),
		new TargetInstrDescriptor(JNE, "JNE", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),
		new TargetInstrDescriptor(JBE, "JBE", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),
		new TargetInstrDescriptor(JA, "JA", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),
		new TargetInstrDescriptor(JS, "JS", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),
		new TargetInstrDescriptor(JNS, "JNS", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),
		new TargetInstrDescriptor(JL, "JL", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),
		new TargetInstrDescriptor(JGE, "JGE", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),
		new TargetInstrDescriptor(JLE, "JLE", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),
		new TargetInstrDescriptor(JG, "JG", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),

		new TargetInstrDescriptor(CALLpcrel32, "CALLpcrel32", 1, M_CALL_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, ImplicitList9),
		new TargetInstrDescriptor(CALLr32, "CALLr32", 1, M_CALL_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList2, ImplicitList9),
		new TargetInstrDescriptor(CALLm32, "CALLm32", 1, M_LOAD_FLAG|M_CALL_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList2, ImplicitList9),
		new TargetInstrDescriptor(LEAVE, "LEAVE", 0, M_LOAD_FLAG, 0, ImplicitList10, ImplicitList10),

		new TargetInstrDescriptor(BSWAPr32, "BSWAPr32", 2, M_2_ADDR_FLAG, 0, null, null),
		new TargetInstrDescriptor(XCHGrr8, "XCHGrr8", 2, 0, 0, null, null),
		new TargetInstrDescriptor(XCHGrr16, "XCHGrr16", 2, 0, 0, null, null),
		new TargetInstrDescriptor(XCHGrr32, "XCHGrr32", 2, 0, 0, null, null),
		new TargetInstrDescriptor(LEAr16, "LEAr16", 6, 0, 0, null, null),
		new TargetInstrDescriptor(LEAr32, "LEAr32", 5, 0, 0, null, null),

		new TargetInstrDescriptor(MOVrr8, "MOVrr8", 2, 0, 0, null, null),
		new TargetInstrDescriptor(MOVrr16, "MOVrr16", 2, 0, 0, null, null),
		new TargetInstrDescriptor(MOVrr32, "MOVrr32", 2, 0, 0, null, null),

		new TargetInstrDescriptor(MOVir8, "MOVir8", 2, 0, 0, null, null),
		new TargetInstrDescriptor(MOVir16, "MOVir8", 2, 0, 0, null, null),
		new TargetInstrDescriptor(MOVir32, "MOVir8", 2, 0, 0, null, null),

		new TargetInstrDescriptor(MOVim8, "MOVim8", 6, 0, 0, null, null),
		new TargetInstrDescriptor(MOVim16, "MOVim16", 6, 0, 0, null, null),
		new TargetInstrDescriptor(MOVim32, "MOVim32", 6, 0, 0, null, null),

		new TargetInstrDescriptor(MOVmr8, "MOVmr8", 6, 0, 0, null, null),
		new TargetInstrDescriptor(MOVmr16, "MOVmr16", 6, 0, 0, null, null),
		new TargetInstrDescriptor(MOVmr32, "MOVmr32", 6, 0, 0, null, null),

		new TargetInstrDescriptor(MOVrm8, "MOVrm8", 6, 0, 0, null, null),
		new TargetInstrDescriptor(MOVrm16, "MOVrm16", 6, 0, 0, null, null),
		new TargetInstrDescriptor(MOVrm32, "MOVrm32", 6, 0, 0, null, null),

		new TargetInstrDescriptor(MOVim8, "MOVim8", 6, 0, 0, null, null),
		new TargetInstrDescriptor(MOVim8, "MOVim8", 6, 0, 0, null, null),
		new TargetInstrDescriptor(MOVim8, "MOVim8", 6, 0, 0, null, null),

		new TargetInstrDescriptor(MULr8, "MULr8", 1, 0, 0, ImplicitList11, ImplicitList12),
		new TargetInstrDescriptor(MULr16, "MULr16", 1, 0, 0, ImplicitList13, ImplicitList14),
		new TargetInstrDescriptor(MULr32, "MULr32", 1, 0, 0, ImplicitList15, ImplicitList16),

		new TargetInstrDescriptor(IDIVr8, "IDIVr8", 1, 0, 0, ImplicitList13, ImplicitList12),
		new TargetInstrDescriptor(IDIVr16, "IDIVr16", 1, 0, 0, ImplicitList17, ImplicitList14),
		new TargetInstrDescriptor(IDIVr32, "IDIVr32", 1, 0, 0, ImplicitList18, ImplicitList16),

		new TargetInstrDescriptor(CBW, "CBW", 0, 0, 0, ImplicitList11, ImplicitList16),
		new TargetInstrDescriptor(CWD, "CWD", 0, 0, 0, ImplicitList13, ImplicitList17),
		new TargetInstrDescriptor(CDQ, "CDQ", 0, 0, 0, ImplicitList15, ImplicitList18),

		new TargetInstrDescriptor(NEGr8, "NEGr8", 2, M_ARITH_FLAG|M_2_ADDR_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(NEGr16, "NEGr16", 2, M_ARITH_FLAG|M_2_ADDR_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(NEGr32, "NEGr32", 2, M_ARITH_FLAG|M_2_ADDR_FLAG, 0, null, ImplicitList1),

		new TargetInstrDescriptor(NOTr8, "NOTr8", 2, M_2_ADDR_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(NOTr16, "NOTr16", 2, M_2_ADDR_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(NOTr32, "NOTr32", 2, M_2_ADDR_FLAG, 0, null, ImplicitList1),

		new TargetInstrDescriptor(INCr8, "INCr8", 2, M_ARITH_FLAG|M_2_ADDR_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(INCr16, "INCr16", 2, M_ARITH_FLAG|M_2_ADDR_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(INCr32, "INCr32", 2, M_ARITH_FLAG|M_2_ADDR_FLAG, 0, null, ImplicitList1),

		new TargetInstrDescriptor(DECr8, "DECr8", 2, M_ARITH_FLAG|M_2_ADDR_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(DECr16, "DECr16", 2, M_ARITH_FLAG|M_2_ADDR_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(DECr32, "DECr32", 2, M_ARITH_FLAG|M_2_ADDR_FLAG, 0, null, ImplicitList1),

		new TargetInstrDescriptor(ADDrr8, "ADDrr8", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(ADDrr16, "ADDrr16", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(ADDrr32, "ADDrr32", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(ADDri8, "ADDri8", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(ADDri16, "ADDri16", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(ADDri32, "ADDri32", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(ADCrr32, "ADCrr32", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, ImplicitList1, ImplicitList1),

		new TargetInstrDescriptor(SUBrr8, "SUBrr8", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(SUBrr16, "SUBrr16", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(SUBrr32, "SUBrr32", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(SUBri8, "SUBri8", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(SUBri16, "SUBri16", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(SUBri32, "SUBri32", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(SUBri16b, "SUBri16b", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(SUBri32b, "SUBri32b", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(SBBrr32, "SBBrr32", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, ImplicitList1, ImplicitList1),

		new TargetInstrDescriptor(IMULrr16, "IMULrr16", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(IMULrr32, "IMULrr32", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(IMULri16, "IMULri16", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(IMULri32, "IMULri32", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(IMULri16b, "IMULri16b", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(IMULri32b, "IMULri32b", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),

		new TargetInstrDescriptor(ANDrr8, "ANDrr8", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(ANDrr16, "ANDrr16", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(ANDrr32, "ANDrr32", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(ANDri8, "ANDri8", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(ANDri16, "ANDri16", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(ANDri32, "ANDri32", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(ANDri16b, "ANDri16b", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(ANDri32b, "ANDri32b", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(ORrr8, "ORrr8", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(ORrr16, "ORrr16", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(ORrr32, "ORrr32", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(ORri8, "ORri8", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(ORri16, "ORri16", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(ORri32, "ORri32", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(ORri16b, "ORri16b", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(ORri32b, "ORri32b", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),

		new TargetInstrDescriptor(TESTrr8, "TESTrr8", 2, M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(TESTrr16, "TESTrr16", 2, M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(TESTrr32, "TESTrr32", 2, M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(TESTri8, "TESTri8", 2, M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(TESTri16, "TESTri16", 2, M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new TargetInstrDescriptor(TESTri32, "TESTri32", 2, M_LOGICAL_FLAG, 0, null, ImplicitList1),

		new TargetInstrDescriptor(SHLrCL8, "SHLrCL8", 2, 0, 0, ImplicitList19, ImplicitList1), // R8 <= cl.
		new TargetInstrDescriptor(SHLrCL16, "SHLrCL16", 2, 0, 0, ImplicitList19, ImplicitList1),
		new TargetInstrDescriptor(SHLrCL32, "SHLrCL32", 2, 0, 0, ImplicitList19, ImplicitList1),
		new TargetInstrDescriptor(SHLir8, "SHLir8", 3, 0, 0, null, ImplicitList1),
		new TargetInstrDescriptor(SHLir16, "SHLir16", 3, 0, 0, null, ImplicitList1),
		new TargetInstrDescriptor(SHLir32, "SHLir32", 3, 0, 0, null, ImplicitList1),
		new TargetInstrDescriptor(SHRrCL8, "SHRrCL8", 2, 0, 0, ImplicitList19, ImplicitList1),
		new TargetInstrDescriptor(SHRrCL16, "SHRrCL16", 2, 0, 0, ImplicitList19, ImplicitList1),
		new TargetInstrDescriptor(SHRrCL32, "SHRrCL32", 2, 0, 0, ImplicitList19, ImplicitList1),
		new TargetInstrDescriptor(SHRir8, "SHRir8", 3, 0, 0, null, ImplicitList1),
		new TargetInstrDescriptor(SHRir16, "SHRir16", 3, 0, 0, null, ImplicitList1),
		new TargetInstrDescriptor(SHRir32, "SHRir32", 3, 0, 0, null, ImplicitList1),
		new TargetInstrDescriptor(SARrCL8, "SARrCL8", 2, 0, 0, ImplicitList19, ImplicitList1),
		new TargetInstrDescriptor(SARrCL16, "SARrCL16", 2, 0, 0, ImplicitList19, ImplicitList1),
		new TargetInstrDescriptor(SARrCL32, "SARrCL32", 2, 0, 0, ImplicitList19, ImplicitList1),
		new TargetInstrDescriptor(SARir8, "SARir8", 3, 0, 0, null, ImplicitList1),
		new TargetInstrDescriptor(SARir16, "SARir16", 3, 0, 0, null, ImplicitList1),
		new TargetInstrDescriptor(SARir32, "SARir32", 3, 0, 0, null, ImplicitList1),

		new TargetInstrDescriptor(SHLDrCL32, "SHLDrCL32", 3, 0, 0, ImplicitList19, ImplicitList1),
		new TargetInstrDescriptor(SHLDir32, "SHLDir32", 4, 0, 0, null, ImplicitList1),
		new TargetInstrDescriptor(SHRDrCL32, "SHRDrCL32", 3, 0, 0, ImplicitList19, ImplicitList1),
		new TargetInstrDescriptor(SHRDir32, "SHRDir32", 4, 0, 0, null, ImplicitList1),
		new TargetInstrDescriptor(SAHF, "SAHF", 0, 0, 0, ImplicitList20, ImplicitList1),

		new TargetInstrDescriptor(SETBr, "SETBr", 1, 0, 0, ImplicitList1, null),
		new TargetInstrDescriptor(SETAEr, "SETAEr", 1, 0, 0, ImplicitList1, null),
		new TargetInstrDescriptor(SETEr, "SETEr", 1, 0, 0, ImplicitList1, null),
		new TargetInstrDescriptor(SETNEr, "SETNEr", 1, 0, 0, ImplicitList1, null),
		new TargetInstrDescriptor(SETBEr, "SETBEr", 1, 0, 0, ImplicitList1, null),
		new TargetInstrDescriptor(SETAr, "SETAr", 1, 0, 0, ImplicitList1, null),
		new TargetInstrDescriptor(SETSr, "SETSr", 1, 0, 0, ImplicitList1, null),
		new TargetInstrDescriptor(SETNSr, "SETNSr", 1, 0, 0, ImplicitList1, null),
		new TargetInstrDescriptor(SETLr, "SETLr", 1, 0, 0, ImplicitList1, null),
		new TargetInstrDescriptor(SETGEr, "SETGEr", 1, 0, 0, ImplicitList1, null),
		new TargetInstrDescriptor(SETLEr, "SETLEr", 1, 0, 0, ImplicitList1, null),
		new TargetInstrDescriptor(SETGr, "SETGr", 1, 0, 0, ImplicitList1, null),

		new TargetInstrDescriptor(CMOVErr16, "CMOVErr16", 3, 0, 0, ImplicitList1, null),
		new TargetInstrDescriptor(CMOVNErr32, "CMOVErr32", 3, 0, 0, ImplicitList1, null),

		new TargetInstrDescriptor(CMPrr8, "CMPrr8", 2, 0, 0, null, ImplicitList1),
		new TargetInstrDescriptor(CMPrr16, "CMPrr16", 2, 0, 0, null, ImplicitList1),
		new TargetInstrDescriptor(CMPrr32, "CMPrr32", 2, 0, 0, null, ImplicitList1),
		new TargetInstrDescriptor(CMPri8, "CMPri8", 2, 0, 0, null, ImplicitList1),
		new TargetInstrDescriptor(CMPri16, "CMPri16", 2, 0, 0, null, ImplicitList1),
		new TargetInstrDescriptor(CMPri32, "CMPri32", 2, 0, 0, null, ImplicitList1),

		new TargetInstrDescriptor(MOVSXr16r8, "MOVSXr16r8", 2, 0, 0, null, null),
		new TargetInstrDescriptor(MOVSXr32r8, "MOVSXr32r8", 2, 0, 0, null, null),
		new TargetInstrDescriptor(MOVSXr32r16, "MOVSXr32r16", 2, 0, 0, null, null),
		new TargetInstrDescriptor(MOVZXr16r8, "MOVZXr16r8", 2, 0, 0, null, null),
		new TargetInstrDescriptor(MOVZXr32r8, "MOVZXr32r8", 2, 0, 0, null, null),
		new TargetInstrDescriptor(MOVZXr32r16, "MOVZXr32r16", 2, 0, 0, null, null),

		// those floating-point instruction maybe are not correct.
		new TargetInstrDescriptor(FpMOV, "FpMOV", 2, 0, 0, null, null),
		new TargetInstrDescriptor(FpADD, "FpADD", 3, 0, 0, null, null),
		new TargetInstrDescriptor(FpSUB, "FpSUB", 3, 0, 0, null, null),
		new TargetInstrDescriptor(FpMUL, "FpMUL", 3, 0, 0, null, null),
		new TargetInstrDescriptor(FpDIV, "FpDIV", 3, 0, 0, null, null),
		new TargetInstrDescriptor(FpUCOM, "FpUCOM", 2, 0, 0, null, null),
		new TargetInstrDescriptor(FpGETRESULT, "FpGETRESULT", 1, 0, 0, null, null),
		new TargetInstrDescriptor(FpSETRESULT, "FpSETRESULT", 1, 0, 0, null, null),
		new TargetInstrDescriptor(FLDrr, "FLDrr", 1, 0, 0, null, null),
		new TargetInstrDescriptor(FLDr32, "FLDr32", 1, 0, 0, null, null),
		new TargetInstrDescriptor(FLDr64, "FLDr64", 1, 0, 0, null, null),
		new TargetInstrDescriptor(FLDr80, "FLDr80", 1, 0, 0, null, null),
		new TargetInstrDescriptor(FILDr16, "FILDr16", 1, 0, 0, null, null),
		new TargetInstrDescriptor(FILDr32, "FILDr32", 1, 0, 0, null, null),
		new TargetInstrDescriptor(FILDr64, "FILDr64", 1, 0, 0, null, null),
		new TargetInstrDescriptor(FSTr32, "FSTr32", 1, 0, 0, null, null),
		new TargetInstrDescriptor(FSTPr32, "FSTPr32", 1, 0, 0, null, null),
		new TargetInstrDescriptor(FSTPr64, "FSTPr64", 1, 0, 0, null, null),
		new TargetInstrDescriptor(FSTPr80, "FSTPr80", 1, 0, 0, null, null),
		new TargetInstrDescriptor(FSTrr, "FSTrr", 1, 0, 0, null, null),
		new TargetInstrDescriptor(FISTr16, "FISTr16", 1, 0, 0, null, null),
		new TargetInstrDescriptor(FISTr32, "FISTr32", 1, 0, 0, null, null),
		new TargetInstrDescriptor(FISTPr32, "FISTPr32", 1, 0, 0, null, null),
		new TargetInstrDescriptor(FISTPr64, "FISTPr64", 1, 0, 0, null, null),
		new TargetInstrDescriptor(FXCH, "FXCH", 2, 0, 0, null, null),

		new TargetInstrDescriptor(FLD0, "FLD0", 0, 0, 0, null, null),
		new TargetInstrDescriptor(FLD1, "FLD1", 0, 0, 0, null, null),
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
