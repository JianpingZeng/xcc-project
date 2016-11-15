package backend.target.x86;
/**
 * @author Xlous.zeng
 * @version 0.1
 */
public interface X86InstrSets
{
	// Pseudo instruction.
	int PHI = 0;
	int NOOP = PHI + 1;
	int ADJCALLSTACKDOWN = NOOP + 1;
	int ADJCALLSTACKUP = ADJCALLSTACKDOWN + 1;

	// Control flow instructions.
	int RET = ADJCALLSTACKUP + 1;
	int JMP = RET + 1;
	int JB = JMP + 1;
	int JAE = JB + 1;
	int JE = JAE + 1;
	int JNE = JE + 1;
	int JBE = JNE + 1;
	int JA = JBE + 1;
	int JS = JA + 1;
	int JNS = JS + 1;
	int JL = JNS + 1;
	int JGE = JL + 1;
	int JLE = JGE + 1;
	int JG = JLE + 1;

	// Call instruction.
	int CALLpcrel32 = JG + 1;
	int CALLr32 = CALLpcrel32 + 1;
	int CALLm32 = CALLr32 + 1;

	// Miscellaneous instructions.
	int LEAVE = CALLm32 + 1;
	int BSWAPr32 = LEAVE + 1;
	int XCHGrr8 = BSWAPr32 + 1;        // xchg R8, R8
	int XCHGrr16 = XCHGrr8 + 1;        // xchg R16, R16
	int XCHGrr32 = XCHGrr16 + 1;        // xchg R32, R32
	int LEAr16 = XCHGrr32 + 1;  // R16 = lea [mem]
	int LEAr32 = LEAr16 + 1;       // R32 = lea [mem]

	//===----------------------------------------------------------------------===//
	//  Move Instructions...
	//
	int MOVrr8 = LEAr32 + 1;
	int MOVrr16 = MOVrr8 + 1;
	int MOVrr32 = MOVrr16 + 1;
	int MOVir8 = MOVrr32 + 1;
	int MOVir16 = MOVir8 + 1;
	int MOVir32 = MOVir16 + 1;
	int MOVim8 = MOVir32 + 1;             // [mem] = imm8
	int MOVim16 = MOVim8 + 1;            // [mem] = imm16
	int MOVim32 = MOVim16 + 1;            // [mem] = imm32
	int MOVmr8 = MOVim32 + 1;            // R8  = [mem]
	int MOVmr16 = MOVmr8 + 1;    // R16 = [mem]
	int MOVmr32 = MOVmr16 + 1;            // R32 = [mem]
	int MOVrm8 = MOVmr32 + 1;             // [mem] = R8
	int MOVrm16 = MOVrm8 + 1;             // [mem] = R16
	int MOVrm32 = MOVrm16 + 1;            // [mem] = R32

	// Extra precision multiplication
	int MULr8 = MOVrm32 + 1;               // AL,AH = AL*R8
	int MULr16 = MULr8 + 1;    // AX,DX = AX*R16
	int MULr32 = MULr16 + 1;         // EAX,EDX = EAX*R32

	// unsigned division/remainder
	int DIVr8 = MULr32 + 1;              // AX/r8 = AL,AH
	int DIVr16 = DIVr8 + 1;              // DX:AX/r16 = AX,DX
	int DIVr32 = DIVr16 + 1;     // EDX:EAX/r32 = EAX,EDX

	// signed division/remainder
	int IDIVr8 = DIVr32 + 1;               // AX/r8 = AL,AH
	int IDIVr16 = IDIVr8 + 1;              // DX:AX/r16 = AX,DX
	int IDIVr32 = IDIVr16 + 1;     // EDX:EAX/r32 = EAX,EDX

	// Sign-extenders for division
	int CBW = IDIVr32 + 1;              // AX = signext(AL)
	int CWD = CBW + 1;              // DX:AX = signext(AX)
	int CDQ = CWD + 1;             // EDX:EAX = signext(EAX)

	//===----------------------------------------------------------------------===//
	//  Two address Instructions...
	//
	// unary instructions
	int NEGr8 = CDQ + 1;         // R8  = -R8  = 0-R8
	int NEGr16 = NEGr8 + 1; // R16 = -R16 = 0-R16
	int NEGr32 = NEGr16 + 1;         // R32 = -R32 = 0-R32
	int NOTr8 = NEGr32 + 1;         // R8  = ~R8  = R8^-1
	int NOTr16 = NOTr8 + 1; // R16 = ~R16 = R16^-1
	int NOTr32 = NOTr16 + 1;         // R32 = ~R32 = R32^-1
	int INCr8 = NOTr32 + 1;         // R8  = R8 +1
	int INCr16 = INCr8 + 1; // R16 = R16+1
	int INCr32 = INCr16 + 1;         // R32 = R32+1
	int DECr8 = INCr32 + 1;         // R8  = R8 -1
	int DECr16 = DECr8 + 1; // R16 = R16-1
	int DECr32 = DECr16 + 1;         // R32 = R32-1

	// Arithmetic...
	int ADDrr8 = DECr32 + 1;
	int ADDrr16 = ADDrr8 + 1;
	int ADDrr32 = ADDrr16 + 1;
	int ADDri8 = ADDrr32 + 1;
	int ADDri16 = ADDri8 + 1;
	int ADDri32 = ADDri16 + 1;
	int ADDri16b = ADDri32 + 1;  // ADDri with sign extended 8 bit imm
	int ADDri32b = ADDri16b + 1;
	int ADCrr32 = -ADDri32b + 1;                // R32 += imm32+Carry
	int SUBrr8 = ADCrr32 + 1;
	int SUBrr16 = SUBrr8 + 1;
	int SUBrr32 = SUBrr16 + 1;
	int SUBri8 = SUBrr32 + 1;
	int SUBri16 = SUBri8 + 1;
	int SUBri32 = SUBri16 + 1;
	int SUBri16b = SUBri32 + 1;
	int SUBri32b = SUBri16b + 1;
	int SBBrr32 = SUBri32b + 1;               // R32 -= R32+Carry
	int IMULrr16 = SBBrr32 + 1;
	int IMULrr32 = IMULrr16 + 1;
	int IMULri16 = IMULrr32 + 1;
	int IMULri32 = IMULri16 + 1;
	int IMULri16b = IMULri32 + 1;
	int IMULri32b = IMULri16b + 1;

	// Logical operators...
	int ANDrr8 = IMULri32b + 1;
	int ANDrr16 = ANDrr8 + 1;
	int ANDrr32 = ANDrr16 + 1;
	int ANDri8 = ANDrr32 + 1;
	int ANDri16 = ANDri8 + 1;
	int ANDri32 = ANDri16 + 1;
	int ANDri16b = ANDri32 + 1;
	int ANDri32b = ANDri16b + 1;
	int ORrr8 = ANDri32b + 1;
	int ORrr16 = ORrr8 + 1;
	int ORrr32 = ORrr16 + 1;
	int ORri8 = ORrr32 + 1;
	int ORri16 = ORri8 + 1;
	int ORri32 = ORri16 + 1;
	int ORri16b = ORri32 + 1;
	int ORri32b = ORri16b + 1;
	int XORrr8 = ORri32b + 1;
	int XORrr16 = XORrr8 + 1;
	int XORrr32 = XORrr16 + 1;
	int XORri8 = XORrr32 + 1;
	int XORri16 = XORri8 + 1;
	int XORri32 = XORri16 + 1;
	int XORri16b = XORri32 + 1;
	int XORri32b = XORri16b + 1;

	// Test instructions are just like AND, except they don't generate a result.
	int TESTrr8 = XORri32b + 1;          // flags = R8  & R8
	int TESTrr16 = TESTrr8 + 1;          // flags = R16 & R16
	int TESTrr32 = TESTrr16 + 1;          // flags = R32 & R32
	int TESTri8 = TESTrr32 + 1;          // flags = R8  & imm8
	int TESTri16 = TESTri8 + 1;          // flags = R16 & imm16
	int TESTri32 = TESTri16 + 1;          // flags = R32 & imm32

	// Shift instructions
	int SHLrCL8 = TESTri32 + 1; // R8  <<= cl
	int SHLrCL16 = SHLrCL8 + 1; // R16 <<= cl
	int SHLrCL32 = SHLrCL16 + 1; // R32 <<= cl
	int SHLir8 = SHLrCL32 + 1;                 // R8  <<= imm8
	int SHLir16 = SHLir8 + 1;         // R16 <<= imm16
	int SHLir32 = SHLir16 + 1;                 // R32 <<= imm32
	int SHRrCL8 = SHLir32 + 1; // R8  >>= cl
	int SHRrCL16 = SHRrCL8 + 1; // R16 >>= cl
	int SHRrCL32 = SHRrCL16 + 1; // R32 >>= cl
	int SHRir8 = SHRrCL32 + 1;                 // R8  >>= imm8
	int SHRir16 = SHRir8 + 1;         // R16 >>= imm16
	int SHRir32 = SHRir16 + 1;                 // R32 >>= imm32
	int SARrCL8 = SHRir32 + 1; // R8  >>>= cl
	int SARrCL16 = SARrCL8 + 1; // R16 >>>= cl
	int SARrCL32 = SARrCL16 + 1; // R32 >>>= cl
	int SARir8 = SARrCL32 + 1;                 // R8  >>>= imm8
	int SARir16 = SARir8 + 1;         // R16 >>>= imm16
	int SARir32 = SARir16 + 1;                 // R32 >>>= imm32
	int SHLDrrCL32 = SARir32 + 1;   // R32 <<= R32,R32 cl
	int SHLDir32 = SHLDrrCL32 + 1;           // R32 <<= R32,R32 imm8
	int SHRDrrCL32 = SHLDir32 + 1;   // R32 >>= R32,R32 cl
	int SHRDir32 = SHRDrrCL32 + 1;           // R32 >>= R32,R32 imm8

	// Condition code ops, incl. set if equal/not equal/...
	int SAHF = SHRDir32 + 1;  // flags = AH
	int SETBr = SAHF + 1;            // R8 = <  unsign
	int SETAEr = SETBr + 1;            // R8 = >= unsign
	int SETEr = SETAEr + 1;            // R8 = ==
	int SETNEr = SETEr + 1;            // R8 = !=
	int SETBEr = SETNEr + 1;            // R8 = <= unsign
	int SETAr = SETBEr + 1;            // R8 = >  signed
	int SETSr = SETAr + 1;            // R8 = <sign bit>
	int SETNSr = SETSr + 1;            // R8 = !<sign bit>
	int SETLr = SETNSr + 1;            // R8 = <  signed
	int SETGEr = SETLr + 1;            // R8 = >= signed
	int SETLEr = SETGEr + 1;            // R8 = <= signed
	int SETGr = SETLEr + 1;            // R8 = <  signed

	// Conditional moves.  These are modelled as X = cmovXX Y, Z.  Eventually
	// register allocated to cmovXX XY, Z
	int CMOVErr16 = SETGr + 1;        // if ==, R16 = R16
	int CMOVNErr32 = CMOVErr16 + 1;                // if !=, R32 = R32

	// Integer comparisons
	int CMPrr8 = CMOVNErr32 + 1;              // compare R8, R8
	int CMPrr16 = CMPrr8 + 1;      // compare R16, R16
	int CMPrr32 = CMPrr16 + 1;             // compare R32, R32
	int CMPri8 = CMPrr32 + 1;              // compare R8, imm8
	int CMPri16 = CMPri8 + 1;      // compare R16, imm16
	int CMPri32 = CMPri16 + 1;              // compare R32, imm32

	// Sign/Zero extenders
	int MOVSXr16r8 = CMPri32 + 1; // R16 = signext(R8)
	int MOVSXr32r8 = MOVSXr16r8 + 1;         // R32 = signext(R8)
	int MOVSXr32r16 = MOVSXr32r8 + 1;         // R32 = signext(R16)
	int MOVZXr16r8 = MOVSXr32r16 + 1; // R16 = zeroext(R8)
	int MOVZXr32r8 = MOVZXr16r8 + 1;         // R32 = zeroext(R8)
	int MOVZXr32r16 = MOVZXr32r8 + 1;         // R32 = zeroext(R16)

	//===----------------------------------------------------------------------===//
	// Floating point support
	//===----------------------------------------------------------------------===//
	int FpMOV = MOVZXr32r16 + 1;   // f1 = fmov f2
	int FpADD = FpMOV + 1;    // f1 = fadd f2, f3
	int FpSUB = FpADD + 1;    // f1 = fsub f2, f3
	int FpMUL = FpSUB + 1;    // f1 = fmul f2, f3
	int FpDIV = FpMUL + 1;    // f1 = fdiv f2, f3
	int FpUCOM = FpDIV + 1;  // FPSW = fucom f1, f2
	int FpGETRESULT = FpUCOM + 1;  // FPR = ST(0)
	int FpSETRESULT = FpGETRESULT + 1;  // ST(0) = FPR

	// Floating point loads & stores...
	int FLDrr = FpSETRESULT + 1;   // push(ST(i))
	int FLDr32 = FLDrr + 1;        // load float
	int FLDr64 = FLDr32 + 1;        // load double
	int FLDr80 = FLDr64 + 1;        // load extended
	int FILDr16 = FLDr80 + 1;        // load signed short
	int FILDr32 = FILDr16 + 1;        // load signed int
	int FILDr64 = FILDr32 + 1;        // load signed long
	int FSTr32 = FILDr64 + 1;          // store float
	int FSTr64 = FSTr32 + 1;          // store double
	int FSTPr32 = FSTr64 + 1;          // store float, pop
	int FSTPr64 = FSTPr32 + 1;          // store double, pop
	int FSTPr80 = FSTPr64 + 1;          // store extended, pop
	int FSTrr = FSTPr80 + 1;       // ST(i) = ST(0)
	int FSTPrr = FSTrr + 1;      // ST(i) = ST(0), pop
	int FISTr16 = FSTPrr + 1;          // store signed short
	int FISTr32 = FISTr16 + 1;          // store signed int
	int FISTPr16 = FISTr32 + 1;          // store signed short, pop
	int FISTPr32 = FISTPr16 + 1;          // store signed int, pop
	int FISTPr64 = FISTPr32 + 1;          // store signed long, pop
	int FXCH = FISTPr64 + 1;       // fxch ST(i), ST(0)

	// Floating point constant loads...
	int FLD0 = FXCH + 1;
	int FLD1 = FLD0 + 1;

	// Binary arithmetic operations...
	int FADDST0r = FLD1 + 1;
	int FADDrST0 = FADDST0r + 1;
	int FADDPrST0 = FADDrST0 + 1;
	int FSUBRST0r = FADDPrST0 + 1;
	int FSUBrST0 = FSUBRST0r + 1;
	int FSUBPrST0 = FSUBrST0+ 1;
	int FSUBST0r = FSUBPrST0 + 1;
	int FSUBRrST0 = FSUBST0r + 1;
	int FSUBRPrST0 = FSUBRrST0 + 1;
	int FMULST0r = FSUBRPrST0 + 1;
	int FMULrST0 = FMULST0r + 1;
	int FMULPrST0 = FMULrST0 + 1;
	int FDIVRST0r = FMULPrST0 + 1;
	int FDIVrST0 = FDIVRST0r + 1;
	int FDIVPrST0 = FDIVrST0 + 1;
	int FDIVST0r = FDIVPrST0 + 1;   // ST(0) = ST(0) / ST(i)
	int FDIVRrST0 = FDIVST0r + 1;   // ST(i) = ST(0) / ST(i)
	int FDIVRPrST0 = FDIVRrST0 + 1;   // ST(i) = ST(0) / ST(i), pop

	// Floating point compares
	int FUCOMr = FDIVRPrST0 + 1;  // FPSW = compare ST(0) with ST(i)
	int FUCOMPr = FUCOMr + 1;  // FPSW = compare ST(0) with ST(i), pop
	int FUCOMPPr = FUCOMPr + 1;  // compare ST(0) with ST(1), pop, pop

	// Floating point flag ops
	int FNSTSWr8 = FUCOMPPr + 1;   // AX = fp flags
	int FNSTCWm16 = FNSTSWr8 + 1;                     // [mem16] = X87 control world
	int FLDCWm16 = FNSTCWm16 + 1;                     // X87 control world = [mem16]
	int INSTRUCTION_LAST_END = FLDCWm16;
}
