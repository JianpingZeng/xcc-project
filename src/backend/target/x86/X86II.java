package backend.target.x86;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public interface X86II
{
	//===------------------------------------------------------------------===//
	// Instruction types.  These are the standard/most common forms for X86
	// instructions.
	//

	// PseudoFrm - This represents an instruction that is a pseudo instruction
	// or one that has not been implemented yet.  It is illegal to code generate
	// it, but tolerated for intermediate implementation stages.
	int Pseudo         = 0;
	/// Raw - This form is for instructions that don't have any operands, so
	/// they are just a fixed opcode value, like 'leave'.
	int RawFrm         = 1;

	/// AddRegFrm - This form is used for instructions like 'push r32' that have
	/// their one register operand added to their opcode.
	int AddRegFrm      = 2;

	/// MRMDestReg - This form is used for instructions that use the Mod/RM byte
	/// to specify a destination, which in this case is a register.
	///
	int MRMDestReg     = 3;

	/// MRMDestMem - This form is used for instructions that use the Mod/RM byte
	/// to specify a destination, which in this case is memory.
	///
	int MRMDestMem     = 4;

	/// MRMSrcReg - This form is used for instructions that use the Mod/RM byte
	/// to specify a source, which in this case is a register.
	///
	int MRMSrcReg      = 5;

	/// MRMSrcMem - This form is used for instructions that use the Mod/RM byte
	/// to specify a source, which in this case is memory.
	///
	int MRMSrcMem      = 6;

	/// MRMS[0-7][rm] - These forms are used to represent instructions that use
	/// a Mod/RM byte, and use the middle field to hold extended opcode
	/// information.  In the intel manual these are represented as /0, /1, ...
	///

	// First, instructions that operate on a register r/m operand...
	int MRMS0r = 16,  MRMS1r = 17,  MRMS2r = 18,  MRMS3r = 19; // Format /0 /1 /2 /3
	int MRMS4r = 20,  MRMS5r = 21,  MRMS6r = 22,  MRMS7r = 23; // Format /4 /5 /6 /7

	// Next, instructions that operate on a memory r/m operand...
	int MRMS0m = 24,  MRMS1m = 25,  MRMS2m = 26,  MRMS3m = 27; // Format /0 /1 /2 /3
	int MRMS4m = 28,  MRMS5m = 29,  MRMS6m = 30,  MRMS7m = 31; // Format /4 /5 /6 /7

	int FormMask       = 31;

	//===------------------------------------------------------------------===//
	// Actual flags...

	// OpSize - Set if this instruction requires an operand size prefix (0x66);
	// which most often indicates that the instruction operates on 16 bit data
	// instead of 32 bit data.
	int OpSize      = 1 << 5;

	// Op0Mask - There are several prefix bytes that are used to form two byte
	// opcodes.  These are currently 0x0F, and 0xD8-0xDF.  This mask is used to
	// obtain the setting of this field.  If no bits in this field is set, there
	// is no prefix byte for obtaining a multibyte opcode.
	//
	int Op0Shift    = 6;
	int Op0Mask     = 0xF << Op0Shift;

	// TB - TwoByte - Set if this instruction has a two byte opcode, which
	// starts with a 0x0F byte before the real opcode.
	int TB          = 1 << Op0Shift;

	// D8-DF - These escape opcodes are used by the floating point unit.  These
	// values must remain sequential.
	int D8 = 2 << Op0Shift,   D9 = 3 << Op0Shift;
	int DA = 4 << Op0Shift,   DB = 5 << Op0Shift;
	int DC = 6 << Op0Shift,   DD = 7 << Op0Shift;
	int DE = 8 << Op0Shift,   DF = 9 << Op0Shift;

	//===------------------------------------------------------------------===//
	// This three-bit field describes the size of a memory operand.  Zero is
	// unused so that we can tell if we forgot to set a value.
	int ArgShift = 10;
	int ArgMask  = 7 << ArgShift;
	int NoArg    = 0;
	int Arg8     = 1 << ArgShift;
	int Arg16    = 2 << ArgShift;
	int Arg32    = 3 << ArgShift;
	int Arg64    = 4 << ArgShift;  // 64 bit int argument for FILD64
	int ArgF32   = 5 << ArgShift;
	int ArgF64   = 6 << ArgShift;
	int ArgF80   = 7 << ArgShift;

	//===------------------------------------------------------------------===//
	// FP Instruction Classification...  Zero is non-fp instruction.

	// FPTypeMask - Mask for all of the FP types...
	int FPTypeShift = 13;
	int FPTypeMask  = 7 << FPTypeShift;

	// ZeroArgFP - 0 arg FP instruction which implicitly pushes ST(0), f.e. fld0
	int ZeroArgFP  = 1 << FPTypeShift;

	// OneArgFP - 1 arg FP instructions which implicitly read ST(0), such as fst
	int OneArgFP   = 2 << FPTypeShift;

	// OneArgFPRW - 1 arg FP instruction which implicitly read ST(0) and write a
	// result back to ST(0).  For example, fcos, fsqrt, etc.
	//
	int OneArgFPRW = 3 << FPTypeShift;

	// TwoArgFP - 2 arg FP instructions which implicitly read ST(0), and an
	// explicit argument, storing the result to either ST(0) or the implicit
	// argument.  For example: fadd, fsub, fmul, etc...
	int TwoArgFP   = 4 << FPTypeShift;

	// SpecialFP - Special instruction forms.  Dispatch by opcode explicitly.
	int SpecialFP  = 5 << FPTypeShift;

	// PrintImplUses - Print out implicit uses in the assembly output.
	int PrintImplUses = 1 << 16;

	int OpcodeShift   = 17;
	int OpcodeMask    = 0xFF << OpcodeShift;
	// Bits 25 -> 31 are unused
}
