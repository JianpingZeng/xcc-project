package backend.target.x86;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public interface X86II {
  //===------------------------------------------------------------------===//
  // X86 Specific MachineOperand flags.

  int MO_NO_FLAG = 0;

  /// MO_GOT_ABSOLUTE_ADDRESS - On a symbol operand, this represents a
  /// relocation of:
  ///    SYMBOL_LABEL + [. - PICBASELABEL]
  int MO_GOT_ABSOLUTE_ADDRESS = 1;

  /// MO_PIC_BASE_OFFSET - On a symbol operand this indicates that the
  /// immediate should get the value of the symbol minus the PIC base label:
  ///    SYMBOL_LABEL - PICBASELABEL
  int MO_PIC_BASE_OFFSET = 2;

  /// MO_GOT - On a symbol operand this indicates that the immediate is the
  /// offset to the GOT entry for the symbol name from the base of the GOT.
  ///
  /// See the X86-64 ELF ABI supplement for more details.
  ///    SYMBOL_LABEL @GOT
  int MO_GOT = 3;

  /// MO_GOTOFF - On a symbol operand this indicates that the immediate is
  /// the offset to the location of the symbol name from the base of the GOT.
  ///
  /// See the X86-64 ELF ABI supplement for more details.
  ///    SYMBOL_LABEL @GOTOFF
  int MO_GOTOFF = 4;

  /// MO_GOTPCREL - On a symbol operand this indicates that the immediate is
  /// offset to the GOT entry for the symbol name from the current code
  /// location.
  ///
  /// See the X86-64 ELF ABI supplement for more details.
  ///    SYMBOL_LABEL @GOTPCREL
  int MO_GOTPCREL = 5;

  /// MO_PLT - On a symbol operand this indicates that the immediate is
  /// offset to the PLT entry of symbol name from the current code location.
  ///
  /// See the X86-64 ELF ABI supplement for more details.
  ///    SYMBOL_LABEL @PLT
  int MO_PLT = 6;

  /// MO_TLSGD - On a symbol operand this indicates that the immediate is
  /// some TLS offset.
  ///
  /// See 'ELF Handling for Thread-Local Storage' for more details.
  ///    SYMBOL_LABEL @TLSGD
  int MO_TLSGD = 7;

  /// MO_GOTTPOFF - On a symbol operand this indicates that the immediate is
  /// some TLS offset.
  ///
  /// See 'ELF Handling for Thread-Local Storage' for more details.
  ///    SYMBOL_LABEL @GOTTPOFF
  int MO_GOTTPOFF = 8;

  /// MO_INDNTPOFF - On a symbol operand this indicates that the immediate is
  /// some TLS offset.
  ///
  /// See 'ELF Handling for Thread-Local Storage' for more details.
  ///    SYMBOL_LABEL @INDNTPOFF
  int MO_INDNTPOFF = 9;

  /// MO_TPOFF - On a symbol operand this indicates that the immediate is
  /// some TLS offset.
  ///
  /// See 'ELF Handling for Thread-Local Storage' for more details.
  ///    SYMBOL_LABEL @TPOFF
  int MO_TPOFF = 10;

  /// MO_NTPOFF - On a symbol operand this indicates that the immediate is
  /// some TLS offset.
  ///
  /// See 'ELF Handling for Thread-Local Storage' for more details.
  ///    SYMBOL_LABEL @NTPOFF
  int MO_NTPOFF = 11;

  /// MO_DLLIMPORT - On a symbol operand "FOO", this indicates that the
  /// reference is actually to the "__imp_FOO" symbol.  This is used for
  /// dllimport linkage on windows.
  int MO_DLLIMPORT = 12;

  /// MO_DARWIN_STUB - On a symbol operand "FOO", this indicates that the
  /// reference is actually to the "FOO$stub" symbol.  This is used for calls
  /// and jumps to external functions on Tiger and before.
  int MO_DARWIN_STUB = 13;

  /// MO_DARWIN_NONLAZY - On a symbol operand "FOO", this indicates that the
  /// reference is actually to the "FOO$non_lazy_ptr" symbol, which is a
  /// non-PIC-base-relative reference to a non-hidden dyld lazy pointer stub.
  int MO_DARWIN_NONLAZY = 14;

  /// MO_DARWIN_NONLAZY_PIC_BASE - On a symbol operand "FOO", this indicates
  /// that the reference is actually to "FOO$non_lazy_ptr - PICBASE", which is
  /// a PIC-base-relative reference to a non-hidden dyld lazy pointer stub.
  int MO_DARWIN_NONLAZY_PIC_BASE = 15;

  /// MO_DARWIN_HIDDEN_NONLAZY - On a symbol operand "FOO", this indicates
  /// that the reference is actually to the "FOO$non_lazy_ptr" symbol, which
  /// is a non-PIC-base-relative reference to a hidden dyld lazy pointer stub.
  int MO_DARWIN_HIDDEN_NONLAZY = 16;

  /// MO_DARWIN_HIDDEN_NONLAZY_PIC_BASE - On a symbol operand "FOO", this
  /// indicates that the reference is actually to "FOO$non_lazy_ptr -PICBASE",
  /// which is a PIC-base-relative reference to a hidden dyld lazy pointer
  /// stub.
  int MO_DARWIN_HIDDEN_NONLAZY_PIC_BASE = 17;


  //===------------------------------------------------------------------===//
  // Instruction encodings.  These are the standard/most common forms for X86
  // instructions.
  //

  // PseudoFrm - This represents an instruction that is a pseudo instruction
  // or one that has not been implemented yet.  It is illegal to code generate
  // it, but tolerated for intermediate implementation stages.
  int Pseudo = 0;

  /// Raw - This form is for instructions that don't have any operands, so
  /// they are just a fixed opcode value, like 'leave'.
  int RawFrm = 1;

  /// AddRegFrm - This form is used for instructions like 'push r32' that have
  /// their one register operand added to their opcode.
  int AddRegFrm = 2;

  /// MRMDestReg - This form is used for instructions that use the Mod/RM byte
  /// to specify a destination, which in this case is a register.
  ///
  int MRMDestReg = 3;

  /// MRMDestMem - This form is used for instructions that use the Mod/RM byte
  /// to specify a destination, which in this case is memory.
  ///
  int MRMDestMem = 4;

  /// MRMSrcReg - This form is used for instructions that use the Mod/RM byte
  /// to specify a source, which in this case is a register.
  ///
  int MRMSrcReg = 5;

  /// MRMSrcMem - This form is used for instructions that use the Mod/RM byte
  /// to specify a source, which in this case is memory.
  ///
  int MRMSrcMem = 6;

  /// MRM[0-7][rm] - These forms are used to represent instructions that use
  /// a Mod/RM byte, and use the middle field to hold extended opcode
  /// information.  In the intel manual these are represented as /0, /1, ...
  ///

  // First, instructions that operate on a register r/m operand...
  int MRM0r = 16;
  int MRM1r = 17;
  int MRM2r = 18;
  int MRM3r = 19; // Format /0 /1 /2 /3
  int MRM4r = 20;
  int MRM5r = 21;
  int MRM6r = 22;
  int MRM7r = 23; // Format /4 /5 /6 /7

  // Next, instructions that operate on a memory r/m operand...
  int MRM0m = 24;
  int MRM1m = 25;
  int MRM2m = 26;
  int MRM3m = 27; // Format /0 /1 /2 /3
  int MRM4m = 28;
  int MRM5m = 29;
  int MRM6m = 30;
  int MRM7m = 31; // Format /4 /5 /6 /7

  // MRMInitReg - This form is used for instructions whose source and
  // destinations are the same register.
  int MRMInitReg = 32;

  int FormMask = 63;

  //===------------------------------------------------------------------===//
  // Actual flags...

  // OpSize - Set if this instruction requires an operand size prefix (0x66),
  // which most often indicates that the instruction operates on 16 bit data
  // instead of 32 bit data.
  int OpSize = 1 << 6;

  // AsSize - Set if this instruction requires an operand size prefix (0x67),
  // which most often indicates that the instruction address 16 bit address
  // instead of 32 bit address (or 32 bit address in 64 bit mode).
  int AdSize = 1 << 7;

  //===------------------------------------------------------------------===//
  // Op0Mask - There are several prefix bytes that are used to form two byte
  // opcodes.  These are currently 0x0F, 0xF3, and 0xD8-0xDF.  This mask is
  // used to obtain the setting of this field.  If no bits in this field is
  // set, there is no prefix byte for obtaining a multibyte opcode.
  //
  int Op0Shift = 8;
  int Op0Mask = 0xF << Op0Shift;

  // TB - TwoByte - Set if this instruction has a two byte opcode, which
  // starts with a 0x0F byte before the real opcode.
  int TB = 1 << Op0Shift;

  // REP - The 0xF3 prefix byte indicating repetition of the following
  // instruction.
  int REP = 2 << Op0Shift;

  // D8-DF - These escape opcodes are used by the floating point unit.  These
  // values must remain sequential.
  int D8 = 3 << Op0Shift;
  int D9 = 4 << Op0Shift;
  int DA = 5 << Op0Shift;
  int DB = 6 << Op0Shift;
  int DC = 7 << Op0Shift;
  int DD = 8 << Op0Shift;
  int DE = 9 << Op0Shift;
  int DF = 10 << Op0Shift;

  // XS, XD - These prefix codes are for single and double precision scalar
  // floating point operations performed in the SSE registers.
  int XD = 11 << Op0Shift;
  int XS = 12 << Op0Shift;

  // T8, TA - Prefix after the 0x0F prefix.
  int T8 = 13 << Op0Shift;
  int TA = 14 << Op0Shift;

  // TF - Prefix before and after 0x0F
  int TF = 15 << Op0Shift;

  //===------------------------------------------------------------------===//
  // REX_W - REX prefixes are instruction prefixes used in 64-bit mode.
  // They are used to specify GPRs and SSE registers, 64-bit operand size,
  // etc. We only cares about REX.W and REX.R bits and only the former is
  // statically determined.
  //
  int REXShift = 12;
  int REX_W = 1 << REXShift;

  //===------------------------------------------------------------------===//
  // This three-bit field describes the size of an immediate operand.  Zero is
  // unused so that we can tell if we forgot to set a value.
  int ImmShift = 13;
  int ImmMask = 7 << ImmShift;
  int Imm8 = 1 << ImmShift;
  int Imm16 = 2 << ImmShift;
  int Imm32 = 3 << ImmShift;
  int Imm64 = 4 << ImmShift;

  //===------------------------------------------------------------------===//
  // FP Instruction Classification...  Zero is non-fp instruction.

  // FPTypeMask - Mask for all of the FP types...
  int FPTypeShift = 16;
  int FPTypeMask = 7 << FPTypeShift;

  // NotFP - The default, set for instructions that do not use FP registers.
  int NotFP = 0 << FPTypeShift;

  // ZeroArgFP - 0 arg FP instruction which implicitly pushes ST(0), f.e. fld0
  int ZeroArgFP = 1 << FPTypeShift;

  // OneArgFP - 1 arg FP instructions which implicitly read ST(0), such as fst
  int OneArgFP = 2 << FPTypeShift;

  // OneArgFPRW - 1 arg FP instruction which implicitly read ST(0) and write a
  // result back to ST(0).  For example, fcos, fsqrt, etc.
  //
  int OneArgFPRW = 3 << FPTypeShift;

  // TwoArgFP - 2 arg FP instructions which implicitly read ST(0), and an
  // explicit argument, storing the result to either ST(0) or the implicit
  // argument.  For example: fadd, fsub, fmul, etc...
  int TwoArgFP = 4 << FPTypeShift;

  // CompareFP - 2 arg FP instructions which implicitly read ST(0) and an
  // explicit argument, but have no destination.  Example: fucom, fucomi, ...
  int CompareFP = 5 << FPTypeShift;

  // CondMovFP - "2 operand" floating point conditional move instructions.
  int CondMovFP = 6 << FPTypeShift;

  // SpecialFP - Special instruction forms.  Dispatch by opcode explicitly.
  int SpecialFP = 7 << FPTypeShift;

  // Lock prefix
  int LOCKShift = 19;
  int LOCK = 1 << LOCKShift;

  // Segment override prefixes. Currently we just need ability to address
  // stuff in gs and fs segments.
  int SegOvrShift = 20;
  int SegOvrMask = 3 << SegOvrShift;
  int FS = 1 << SegOvrShift;
  int GS = 2 << SegOvrShift;

  // Bits 22 -> 23 are unused
  int OpcodeShift = 24;
  int OpcodeMask = 0xFF << OpcodeShift;
}
