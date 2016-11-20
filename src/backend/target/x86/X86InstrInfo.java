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
	private static MCInstrDescriptor[] x86Insts =
	{
		new MCInstrDescriptor(PHI, "PHI", 0, M_DUMMY_PHI_FLAG|M_PSEUDO_FLAG, 0, null, null),
		new MCInstrDescriptor(NOOP, "NOOP", 0, M_NOP_FLAG|M_PSEUDO_FLAG, 0, null, null),
		new MCInstrDescriptor(ADJCALLSTACKDOWN, "ADJCALLSTACKDOWN", 1, 0, 0, ImplicitList2, ImplicitList3),
		new MCInstrDescriptor(ADJCALLSTACKUP, "ADJCALLSTACKUP", 2, 0, 0, ImplicitList2, ImplicitList3),
		new MCInstrDescriptor(RET, "RET", 0, M_RET_FLAG|M_TERMINATOR_FLAG, 0, null, null),

		new MCInstrDescriptor(JMP, "JMP", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, null, null),
		new MCInstrDescriptor(JB, "JB", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),
		new MCInstrDescriptor(JAE, "JAE", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),
		new MCInstrDescriptor(JE, "JE", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),
		new MCInstrDescriptor(JNE, "JNE", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),
		new MCInstrDescriptor(JBE, "JBE", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),
		new MCInstrDescriptor(JA, "JA", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),
		new MCInstrDescriptor(JS, "JS", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),
		new MCInstrDescriptor(JNS, "JNS", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),
		new MCInstrDescriptor(JL, "JL", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),
		new MCInstrDescriptor(JGE, "JGE", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),
		new MCInstrDescriptor(JLE, "JLE", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),
		new MCInstrDescriptor(JG, "JG", 1, M_BRANCH_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, null),

		new MCInstrDescriptor(CALLpcrel32, "CALLpcrel32", 1, M_CALL_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList1, ImplicitList9),
		new MCInstrDescriptor(CALLr32, "CALLr32", 1, M_CALL_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList2, ImplicitList9),
		new MCInstrDescriptor(CALLm32, "CALLm32", 1, M_LOAD_FLAG|M_CALL_FLAG|M_TERMINATOR_FLAG, 0, ImplicitList2, ImplicitList9),
		new MCInstrDescriptor(LEAVE, "LEAVE", 0, M_LOAD_FLAG, 0, ImplicitList10, ImplicitList10),

		new MCInstrDescriptor(BSWAPr32, "BSWAPr32", 2, 0, 0, null, null),
		new MCInstrDescriptor(XCHGrr8, "XCHGrr8", 2, 0, 0, null, null),
		new MCInstrDescriptor(XCHGrr16, "XCHGrr16", 2, 0, 0, null, null),
		new MCInstrDescriptor(XCHGrr32, "XCHGrr32", 2, 0, 0, null, null),
		new MCInstrDescriptor(LEAr16, "LEAr16", 6, 0, 0, null, null),
		new MCInstrDescriptor(LEAr32, "LEAr32", 5, 0, 0, null, null),

		new MCInstrDescriptor(MOVrr8, "MOVrr8", 2, 0, 0, null, null),
		new MCInstrDescriptor(MOVrr16, "MOVrr16", 2, 0, 0, null, null),
		new MCInstrDescriptor(MOVrr32, "MOVrr32", 2, 0, 0, null, null),

		new MCInstrDescriptor(MOVir8, "MOVir8", 2, 0, 0, null, null),
		new MCInstrDescriptor(MOVir16, "MOVir8", 2, 0, 0, null, null),
		new MCInstrDescriptor(MOVir32, "MOVir8", 2, 0, 0, null, null),

		new MCInstrDescriptor(MOVim8, "MOVim8", 6, 0, 0, null, null),
		new MCInstrDescriptor(MOVim16, "MOVim16", 6, 0, 0, null, null),
		new MCInstrDescriptor(MOVim32, "MOVim32", 6, 0, 0, null, null),

		new MCInstrDescriptor(MOVmr8, "MOVmr8", 6, 0, 0, null, null),
		new MCInstrDescriptor(MOVmr16, "MOVmr16", 6, 0, 0, null, null),
		new MCInstrDescriptor(MOVmr32, "MOVmr32", 6, 0, 0, null, null),

		new MCInstrDescriptor(MOVrm8, "MOVrm8", 6, 0, 0, null, null),
		new MCInstrDescriptor(MOVrm16, "MOVrm16", 6, 0, 0, null, null),
		new MCInstrDescriptor(MOVrm32, "MOVrm32", 6, 0, 0, null, null),

		new MCInstrDescriptor(MOVim8, "MOVim8", 6, 0, 0, null, null),
		new MCInstrDescriptor(MOVim8, "MOVim8", 6, 0, 0, null, null),
		new MCInstrDescriptor(MOVim8, "MOVim8", 6, 0, 0, null, null),

		new MCInstrDescriptor(MULr8, "MULr8", 1, 0, 0, ImplicitList11, ImplicitList12),
		new MCInstrDescriptor(MULr16, "MULr16", 1, 0, 0, ImplicitList13, ImplicitList14),
		new MCInstrDescriptor(MULr32, "MULr32", 1, 0, 0, ImplicitList15, ImplicitList16),

		new MCInstrDescriptor(IDIVr8, "IDIVr8", 1, 0, 0, ImplicitList13, ImplicitList12),
		new MCInstrDescriptor(IDIVr16, "IDIVr16", 1, 0, 0, ImplicitList17, ImplicitList14),
		new MCInstrDescriptor(IDIVr32, "IDIVr32", 1, 0, 0, ImplicitList18, ImplicitList16),

		new MCInstrDescriptor(CBW, "CBW", 0, 0, 0, ImplicitList11, ImplicitList16),
		new MCInstrDescriptor(CWD, "CWD", 0, 0, 0, ImplicitList13, ImplicitList17),
		new MCInstrDescriptor(CDQ, "CDQ", 0, 0, 0, ImplicitList15, ImplicitList18),

		new MCInstrDescriptor(NEGr8, "NEGr8", 2, M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(NEGr16, "NEGr16", 2, M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(NEGr32, "NEGr32", 2, M_ARITH_FLAG, 0, null, ImplicitList1),

		new MCInstrDescriptor(NOTr8, "NOTr8", 2, 0, 0, null, ImplicitList1),
		new MCInstrDescriptor(NOTr16, "NOTr16", 2, 0, 0, null, ImplicitList1),
		new MCInstrDescriptor(NOTr32, "NOTr32", 2, 0, 0, null, ImplicitList1),

		new MCInstrDescriptor(INCr8, "INCr8", 2, M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(INCr16, "INCr16", 2, M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(INCr32, "INCr32", 2, M_ARITH_FLAG, 0, null, ImplicitList1),

		new MCInstrDescriptor(DECr8, "DECr8", 2, M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(DECr16, "DECr16", 2, M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(DECr32, "DECr32", 2, M_ARITH_FLAG, 0, null, ImplicitList1),

		new MCInstrDescriptor(ADDrr8, "ADDrr8", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(ADDrr16, "ADDrr16", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(ADDrr32, "ADDrr32", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(ADDri8, "ADDri8", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(ADDri16, "ADDri16", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(ADDri32, "ADDri32", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(ADCrr32, "ADCrr32", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, ImplicitList1, ImplicitList1),

		new MCInstrDescriptor(SUBrr8, "SUBrr8", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(SUBrr16, "SUBrr16", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(SUBrr32, "SUBrr32", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(SUBri8, "SUBri8", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(SUBri16, "SUBri16", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(SUBri32, "SUBri32", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(SUBri16b, "SUBri16b", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(SUBri32b, "SUBri32b", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(SBBrr32, "SBBrr32", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, ImplicitList1, ImplicitList1),

		new MCInstrDescriptor(IMULrr16, "IMULrr16", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(IMULrr32, "IMULrr32", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(IMULri16, "IMULri16", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(IMULri32, "IMULri32", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(IMULri16b, "IMULri16b", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(IMULri32b, "IMULri32b", 3, M_2_ADDR_FLAG|M_ARITH_FLAG, 0, null, ImplicitList1),

		new MCInstrDescriptor(ANDrr8, "ANDrr8", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(ANDrr16, "ANDrr16", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(ANDrr32, "ANDrr32", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(ANDri8, "ANDri8", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(ANDri16, "ANDri16", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(ANDri32, "ANDri32", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(ANDri16b, "ANDri16b", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(ANDri32b, "ANDri32b", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(ORrr8, "ORrr8", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(ORrr16, "ORrr16", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(ORrr32, "ORrr32", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(ORri8, "ORri8", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(ORri16, "ORri16", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(ORri32, "ORri32", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(ORri16b, "ORri16b", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(ORri32b, "ORri32b", 3, M_2_ADDR_FLAG|M_LOGICAL_FLAG, 0, null, ImplicitList1),

		new MCInstrDescriptor(TESTrr8, "TESTrr8", 2, M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(TESTrr16, "TESTrr16", 2, M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(TESTrr32, "TESTrr32", 2, M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(TESTri8, "TESTri8", 2, M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(TESTri16, "TESTri16", 2, M_LOGICAL_FLAG, 0, null, ImplicitList1),
		new MCInstrDescriptor(TESTri32, "TESTri32", 2, M_LOGICAL_FLAG, 0, null, ImplicitList1),

		new MCInstrDescriptor(SHLrCL8, "SHLrCL8", 2, 0, 0, ImplicitList19, ImplicitList1), // R8 <= cl.
		new MCInstrDescriptor(SHLrCL16, "SHLrCL16", 2, 0, 0, ImplicitList19, ImplicitList1),
		new MCInstrDescriptor(SHLrCL32, "SHLrCL32", 2, 0, 0, ImplicitList19, ImplicitList1),
		new MCInstrDescriptor(SHLir8, "SHLir8", 3, 0, 0, null, ImplicitList1),
		new MCInstrDescriptor(SHLir16, "SHLir16", 3, 0, 0, null, ImplicitList1),
		new MCInstrDescriptor(SHLir32, "SHLir32", 3, 0, 0, null, ImplicitList1),
		new MCInstrDescriptor(SHRrCL8, "SHRrCL8", 2, 0, 0, ImplicitList19, ImplicitList1),
		new MCInstrDescriptor(SHRrCL16, "SHRrCL16", 2, 0, 0, ImplicitList19, ImplicitList1),
		new MCInstrDescriptor(SHRrCL32, "SHRrCL32", 2, 0, 0, ImplicitList19, ImplicitList1),
		new MCInstrDescriptor(SHRir8, "SHRir8", 3, 0, 0, null, ImplicitList1),
		new MCInstrDescriptor(SHRir16, "SHRir16", 3, 0, 0, null, ImplicitList1),
		new MCInstrDescriptor(SHRir32, "SHRir32", 3, 0, 0, null, ImplicitList1),
		new MCInstrDescriptor(SARrCL8, "SARrCL8", 2, 0, 0, ImplicitList19, ImplicitList1),
		new MCInstrDescriptor(SARrCL16, "SARrCL16", 2, 0, 0, ImplicitList19, ImplicitList1),
		new MCInstrDescriptor(SARrCL32, "SARrCL32", 2, 0, 0, ImplicitList19, ImplicitList1),
		new MCInstrDescriptor(SARir8, "SARir8", 3, 0, 0, null, ImplicitList1),
		new MCInstrDescriptor(SARir16, "SARir16", 3, 0, 0, null, ImplicitList1),
		new MCInstrDescriptor(SARir32, "SARir32", 3, 0, 0, null, ImplicitList1),

		new MCInstrDescriptor(SHLDrCL32, "SHLDrCL32", 3, 0, 0, ImplicitList19, ImplicitList1),
		new MCInstrDescriptor(SHLDir32, "SHLDir32", 4, 0, 0, null, ImplicitList1),
		new MCInstrDescriptor(SHRDrCL32, "SHRDrCL32", 3, 0, 0, ImplicitList19, ImplicitList1),
		new MCInstrDescriptor(SHRDir32, "SHRDir32", 4, 0, 0, null, ImplicitList1),
		new MCInstrDescriptor(SAHF, "SAHF", 0, 0, 0, ImplicitList20, ImplicitList1),

		new MCInstrDescriptor(SETBr, "SETBr", 1, 0, 0, ImplicitList1, null),
		new MCInstrDescriptor(SETAEr, "SETAEr", 1, 0, 0, ImplicitList1, null),
		new MCInstrDescriptor(SETEr, "SETEr", 1, 0, 0, ImplicitList1, null),
		new MCInstrDescriptor(SETNEr, "SETNEr", 1, 0, 0, ImplicitList1, null),
		new MCInstrDescriptor(SETBEr, "SETBEr", 1, 0, 0, ImplicitList1, null),
		new MCInstrDescriptor(SETAr, "SETAr", 1, 0, 0, ImplicitList1, null),
		new MCInstrDescriptor(SETSr, "SETSr", 1, 0, 0, ImplicitList1, null),
		new MCInstrDescriptor(SETNSr, "SETNSr", 1, 0, 0, ImplicitList1, null),
		new MCInstrDescriptor(SETLr, "SETLr", 1, 0, 0, ImplicitList1, null),
		new MCInstrDescriptor(SETGEr, "SETGEr", 1, 0, 0, ImplicitList1, null),
		new MCInstrDescriptor(SETLEr, "SETLEr", 1, 0, 0, ImplicitList1, null),
		new MCInstrDescriptor(SETGr, "SETGr", 1, 0, 0, ImplicitList1, null),

		new MCInstrDescriptor(CMOVErr16, "CMOVErr16", 3, 0, 0, ImplicitList1, null),
		new MCInstrDescriptor(CMOVNErr32, "CMOVErr32", 3, 0, 0, ImplicitList1, null),

		new MCInstrDescriptor(CMPrr8, "CMPrr8", 2, 0, 0, null, ImplicitList1),
		new MCInstrDescriptor(CMPrr16, "CMPrr16", 2, 0, 0, null, ImplicitList1),
		new MCInstrDescriptor(CMPrr32, "CMPrr32", 2, 0, 0, null, ImplicitList1),
		new MCInstrDescriptor(CMPri8, "CMPri8", 2, 0, 0, null, ImplicitList1),
		new MCInstrDescriptor(CMPri16, "CMPri16", 2, 0, 0, null, ImplicitList1),
		new MCInstrDescriptor(CMPri32, "CMPri32", 2, 0, 0, null, ImplicitList1),

		new MCInstrDescriptor(MOVSXr16r8, "MOVSXr16r8", 2, 0, 0, null, null),
		new MCInstrDescriptor(MOVSXr32r8, "MOVSXr32r8", 2, 0, 0, null, null),
		new MCInstrDescriptor(MOVSXr32r16, "MOVSXr32r16", 2, 0, 0, null, null),
		new MCInstrDescriptor(MOVZXr16r8, "MOVZXr16r8", 2, 0, 0, null, null),
		new MCInstrDescriptor(MOVZXr32r8, "MOVZXr32r8", 2, 0, 0, null, null),
		new MCInstrDescriptor(MOVZXr32r16, "MOVZXr32r16", 2, 0, 0, null, null),

		// those floating-point instruction maybe are not correct.
		new MCInstrDescriptor(FpMOV, "FpMOV", 2, 0, 0, null, null),
		new MCInstrDescriptor(FpADD, "FpADD", 3, 0, 0, null, null),
		new MCInstrDescriptor(FpSUB, "FpSUB", 3, 0, 0, null, null),
		new MCInstrDescriptor(FpMUL, "FpMUL", 3, 0, 0, null, null),
		new MCInstrDescriptor(FpDIV, "FpDIV", 3, 0, 0, null, null),
		new MCInstrDescriptor(FpUCOM, "FpUCOM", 3, 0, 0, null, null),
		new MCInstrDescriptor(FpGETRESULT, "FpGETRESULT", 2, 0, 0, null, null),
		new MCInstrDescriptor(FpSETRESULT, "FpSETRESULT", 2, 0, 0, null, null),
		new MCInstrDescriptor(FLDrr, "FLDrr", 2, 0, 0, null, null),
		new MCInstrDescriptor(FLDr32, "FLDr32", 2, 0, 0, null, null),
		new MCInstrDescriptor(FLDr64, "FLDr64", 2, 0, 0, null, null),
		new MCInstrDescriptor(FLDr80, "FLDr80", 2, 0, 0, null, null),
		new MCInstrDescriptor(FILDr16, "FILDr16", 2, 0, 0, null, null),
		new MCInstrDescriptor(FILDr32, "FILDr32", 2, 0, 0, null, null),
		new MCInstrDescriptor(FILDr64, "FILDr64", 2, 0, 0, null, null),
		new MCInstrDescriptor(FSTr32, "FSTr32", 2, 0, 0, null, null),
		new MCInstrDescriptor(FSTPr32, "FSTPr32", 2, 0, 0, null, null),
		new MCInstrDescriptor(FSTPr64, "FSTPr64", 2, 0, 0, null, null),
		new MCInstrDescriptor(FSTPr80, "FSTPr80", 2, 0, 0, null, null),
		new MCInstrDescriptor(FSTrr, "FSTrr", 2, 0, 0, null, null),
		new MCInstrDescriptor(FISTr16, "FISTr16", 2, 0, 0, null, null),
		new MCInstrDescriptor(FISTr32, "FISTr32", 2, 0, 0, null, null),
		new MCInstrDescriptor(FISTPr32, "FISTPr32", 2, 0, 0, null, null),
		new MCInstrDescriptor(FISTPr64, "FISTPr64", 2, 0, 0, null, null),
		new MCInstrDescriptor(FXCH, "FXCH", 2, 0, 0, null, null),

		new MCInstrDescriptor(FLD0, "FLD0", 0, 0, 0, null, null),
		new MCInstrDescriptor(FLD1, "FLD1", 0, 0, 0, null, null),
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
