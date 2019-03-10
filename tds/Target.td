include "SDNodeProperties.td"
include "ValueType.td"
include "Intrinsics.td"

class RegisterClass; // Forward def

class HwMode<string FS> {
  // A string represents subtarget features that turn on in
  // this Hardware mode. for example, "+sse1,-sse2" will
  // indicates the mode is enabled when "sse1" is enabled and
  // "sse2" is disabled.
  string Features = FS;
}

// Default HW mode for target when there is no other mode active.
def DefaultMode : HwMode<"">;

class HwModeSelect<list<HwMode> Ms> {
  list<HwMode> Modes = Ms;
}

class ValueTypeByHwMode<list<HwMode> Ms, list<ValueType> Ts>
    : HwModeSelect<Ms>, ValueType<0,0> {
  list<ValueType> Objects = Ts;
}

class RegInfo<int RS, int SS, int SA> {
  int RegSize = RS;
  int SpillSize = SS;
  int SpillAlignment = SA;
}

class RegInfoByHwMode<list<HwMode> Ms, list<RegInfo> Rs>
    : HwModeSelect<Ms> {
  list<RegInfo> Objects = Rs;
}

// SubRegIndex - Use instances of SubRegIndex to identify subregisters.
class SubRegIndex<int size, int offset = 0> {
  string Namespace = "";

  // Size - Size (in bits) of the sub-registers represented by this index.
  int Size = size;

  // Offset - Offset of the first bit that is part of this sub-register index.
  // Set it to -1 if the same index is used to represent sub-registers that can
  // be at different offsets (for example when using an index to access an
  // element in a register tuple).
  int Offset = offset;

  // ComposedOf - A list of two SubRegIndex instances, [A, B].
  // This indicates that this SubRegIndex is the result of composing A and B.
  // See ComposedSubRegIndex.
  list<SubRegIndex> ComposedOf = [];

  // CoveringSubRegIndices - A list of two or more sub-register indexes that
  // cover this sub-register.
  //
  // This field should normally be left blank as TableGen can infer it.
  //
  // TableGen automatically detects sub-registers that straddle the registers
  // in the SubRegs field of a Register definition. For example:
  //
  //   Q0    = dsub_0 -> D0, dsub_1 -> D1
  //   Q1    = dsub_0 -> D2, dsub_1 -> D3
  //   D1_D2 = dsub_0 -> D1, dsub_1 -> D2
  //   QQ0   = qsub_0 -> Q0, qsub_1 -> Q1
  //
  // TableGen will infer that D1_D2 is a sub-register of QQ0. It will be given
  // the synthetic index dsub_1_dsub_2 unless some SubRegIndex is defined with
  // CoveringSubRegIndices = [dsub_1, dsub_2].
  list<SubRegIndex> CoveringSubRegIndices = [];
}

// ComposedSubRegIndex - A sub-register that is the result of composing A and B.
// Offset is set to the sum of A and B's Offsets. Size is set to B's Size.
class ComposedSubRegIndex<SubRegIndex A, SubRegIndex B>
  : SubRegIndex<B.Size, !if(!eq(A.Offset, -1), -1,
                        !if(!eq(B.Offset, -1), -1,
                            !add(A.Offset, B.Offset)))> {
  // See SubRegIndex.
  let ComposedOf = [A, B];
}

// RegAltNameIndex - The alternate name set to use for register operands of
// this register class when printing.
class RegAltNameIndex {
  string Namespace = "";
}
def NoRegAltName : RegAltNameIndex;

class Register<string n> {
  string Namespace = "";
  string AsmName = n;
  list<string> AltNames;

  int SpillSize = 0;

  int SpillAlignment = 0;

  list<Register> Aliases = [];

  list<Register> SubRegs = [];

  list<int> DwarfNumbers = [];

  // SubRegIndices - For each register in SubRegs, specify the SubRegIndex used
  // to address it. Sub-sub-register indices are automatically inherited from
  // SubRegs.
  list<SubRegIndex> SubRegIndices = [];

  // RegAltNameIndices - The alternate name indices which are valid for this
  // register.
  list<RegAltNameIndex> RegAltNameIndices = [];

  // CostPerUse - Additional cost of instructions using this register compared
  // to other registers in its class. The register allocator will try to
  // minimize the number of instructions using a register with a CostPerUse.
  // This is used by the x86-64 and ARM Thumb targets where some registers
  // require larger instruction encodings.
  int CostPerUse = 0;

  // CoveredBySubRegs - When this bit is set, the value of this register is
  // completely determined by the value of its sub-registers.  For example, the
  // x86 register AX is covered by its sub-registers AL and AH, but EAX is not
  // covered by its sub-register AX.
  bit CoveredBySubRegs = 0;

  // The target specific hardware encoding for this target.
  bits<16> HWEncoding = 0;

  bit isArtificial = 0;
}

class RegisterWithSubRegs<string n, list<Register> subregs> : Register<n> {
  let SubRegs = subregs;
}

class SubRegSet<int n, list<Register> regs, list<Register> subregs> {
  int index = n;

  list<Register> From = regs;
  list<Register> To = subregs;
}

class RegisterClass<string namespace, list<ValueType> regTypes, int alignment,
                    list<Register> regList> {
  string Namespace = namespace;
  // The register spill size and alignment selected by hardware mode.
  RegInfoByHwMode RegInfos;

  list<ValueType> RegTypes = regTypes;

  int Size = 0;

  int Alignment = alignment;

  int CopyCost = 1;

  list<Register> MemberList = regList;

  list<RegisterClass> SubRegClassList = [];

  code MethodProtos = [{}];
  code MethodBodies = [{}];
}


//===----------------------------------------------------------------------===//
// DwarfRegNum - This class provides a mapping of the llvm register enumeration
// to the register numbering used by gcc and gdb.  These values are used by a
// debug information writer (ex. DwarfWriter) to describe where values may be
// located during execution.
class DwarfRegNum<list<int> Numbers> {
  // DwarfNumbers - Numbers used internally by gcc/gdb to identify the register.
  // These values can be determined by locating the <target>.h file in the
  // directory llvmgcc/gcc/config/<target>/ and looking for REGISTER_NAMES.  The
  // order of these names correspond to the enumeration used by gcc.  A value of
  // -1 indicates that the gcc number is undefined and -2 that register number is
  // invalid for this mode/flavour.
  list<int> DwarfNumbers = Numbers;
}

//===----------------------------------------------------------------------===//
// Pull in the common support for scheduling
// Commented by Jianping Zeng on 2017.6.19
include "./TargetSchedule.td"

class Predicate; // Forward def

//===----------------------------------------------------------------------===//
// Instruction set description - These classes correspond to the Java classes in
// the Target/TargetOpcodes.java file.
//
class Instruction {
  string Namespace = "";

  dag OutOperandList;       // An dag containing the MI def operand list.
  dag InOperandList;        // An dag containing the MI use operand list.
  string AsmString = "";    // The .s format to print the instruction with.

  // Pattern - Set to the DAG pattern for this instruction, if we know of one,
  // otherwise, uninitialized.
  list<dag> Pattern;

  // The follow state will eventually be inferred automatically from the
  // instruction pattern.

  list<Register> Uses = []; // Default to using no non-operand registers
  list<Register> Defs = []; // Default to modifying no non-operand registers

  // Predicates - List of predicates which will be turned into isel matching
  // code.
  list<Predicate> Predicates = [];

  // Code size.
  int CodeSize = 0;

  // Added complexity passed onto matching pattern.
  int AddedComplexity  = 0;

  // These bits capture information about the high-level semantics of the
  // instruction.
  bit isReturn     = 0;     // Is this instruction a return instruction?
  bit isBranch     = 0;     // Is this instruction a branch instruction?
  bit isIndirectBranch = 0; // Is this instruction an indirect branch?
  bit isBarrier    = 0;     // Can control flow fall through this instruction?
  bit isCall       = 0;     // Is this instruction a call instruction?
  bit canFoldAsLoad = 0;    // Can this be folded as a simple memory operand?
  bit mayLoad      = 0;     // Is it possible for this inst to read memory?
  bit mayStore     = 0;     // Is it possible for this inst to write memory?
  bit isTwoAddress = 0;     // Is this a two address instruction?
  bit isConvertibleToThreeAddress = 0;  // Can this 2-addr instruction promote?
  bit isCommutable = 0;     // Is this 3 operand instruction commutable?
  bit isTerminator = 0;     // Is this part of the terminator for a basic block?
  bit isReMaterializable = 0; // Is this instruction re-materializable?
  bit isPredicable = 0;     // Is this instruction predicable?
  bit hasDelaySlot = 0;     // Does this instruction have an delay slot?
  bit usesCustomDAGSchedInserter = 0; // Pseudo instr needing special help.
  bit hasCtrlDep   = 0;     // Does this instruction r/w ctrl-flow chains?
  bit isNotDuplicable = 0;  // Is it unsafe to duplicate this instruction?
  bit isAsCheapAsAMove = 0; // As cheap (or cheaper) than a move instruction.
  bit isPseudo = 0;         // Is this instruction pseduo?
  bit usesCustomInserter = 0; // Pseudo instr needing special help.

  // Side effect flags - When set, the flags have these meanings:
  //
  //  hasSideEffects - The instruction has side effects that are not
  //    captured by any operands of the instruction or other flags.
  //
  //  mayHaveSideEffects  - Some instances of the instruction can have side
  //    effects. The virtual method "isReallySideEffectFree" is called to
  //    determine this. Load instructions are an example of where this is
  //    useful. In general, loads always have side effects. However, loads from
  //    constant pools don't. Individual back ends make this determination.
  //
  //  neverHasSideEffects - Set on an instruction with no pattern if it has no
  //    side effects.
  bit hasSideEffects = 0;
  bit mayHaveSideEffects = 0;
  bit neverHasSideEffects = 0;

  // Is this instruction a "real" instruction (with a distinct machine
  // encoding), or is it a pseudo instruction used for codegen modeling
  // purposes.
  bit isCodeGenOnly = 0;

  bit isAsmParserOnly = 0;

  InstrItinClass Itinerary = NoItinerary;// Execution steps used for scheduling.

  string Constraints = "";  // OperandConstraint, e.g. $src = $dst.

  /// DisableEncoding - List of operand names (e.g. "$op1,$op2") that should not
  /// be encoded into the output machineinstr.
  string DisableEncoding = "";

  // Target-specific flags which will be encoded into MCInstrDesc.
  bits<64> TSFlags = 0;
}

/// PseudoInstExpansion - Expansion information for a pseudo-instruction.
/// Which instruction it expands to and how the operands map from the
/// pseudo.
class PseudoInstExpansion<dag Result> {
  dag ResultInst = Result;     // The instruction to generate.
  bit isPseudo = 1;
}

/// Predicates - These are extra conditionals which are turned into instruction
/// selector matching code. Currently each predicate is just a string.
class Predicate<string cond> {
  string CondString = cond;
}

/// NoHonorSignDependentRounding - This predicate is true if support for
/// sign-dependent-rounding is not enabled.
def NoHonorSignDependentRounding
 : Predicate<"!HonorSignDependentRoundingFPMath()">;

class Requires<list<Predicate> preds> {
  list<Predicate> Predicates = preds;
}

/// ops definition - This is just a simple marker used to identify the operands
/// list for an instruction. outs and ins are identical both syntatically and
/// semantically, they are used to define def operands and use operands to
/// improve readibility. This should be used like this:
///     (outs R32:$dst), (ins R32:$src1, R32:$src2) or something similar.
def ops;
def outs;
def ins;

/// variable_ops definition - Mark this instruction as taking a variable number
/// of operands.
def variable_ops;


/// PointerLikeRegClass - Values that are designed to have pointer width are
/// derived from this.  TableGen treats the register class as having a symbolic
/// type that it doesn't know, and resolves the actual regclass to use by using
/// the TargetRegisterInfo::getPointerRegClass() hook at codegen time.
class PointerLikeRegClass<int Kind> {
  int RegClassKind = Kind;
}


/// ptr_rc definition - Mark this operand as being a pointer value whose
/// register class is resolved dynamically via a callback to TargetInstrInfo.
/// FIXME: We should probably change this to a class which contain a list of
/// flags. But currently we have but one flag.
def ptr_rc : PointerLikeRegClass<0>;

/// unknown definition - Mark this operand as being of unknown type, causing
/// it to be resolved by inference in the context it is used.
class unknown_class;
def unknown : unknown_class;

/// AsmOperandClass - Representation for the kinds of operands which the target
/// specific parser can create and the assembly matcher may need to distinguish.
///
/// Operand classes are used to define the order in which instructions are
/// matched, to ensure that the instruction which gets matched for any
/// particular list of operands is deterministic.
///
/// The target specific parser must be able to classify a parsed operand into a
/// unique class which does not partially overlap with any other classes. It can
/// match a subset of some other class, in which case the super class field
/// should be defined.
class AsmOperandClass {
  /// The name to use for this class, which should be usable as an enum value.
  string Name = ?;

  /// The super class of this operand.
  AsmOperandClass SuperClass = ?;

  /// The name of the method on the target specific operand to call to test
  /// whether the operand is an instance of this class. If not set, this will
  /// default to "isFoo", where Foo is the AsmOperandClass name. The method
  /// signature should be:
  ///   boolean isFoo();
  string PredicateMethod = ?;

  /// The name of the method on the target specific operand to call to add the
  /// target specific operand to an MCInst. If not set, this will default to
  /// "addFooOperands", where Foo is the AsmOperandClass name. The method
  /// signature should be:
  ///   void addFooOperands(MCInst &Inst, unsigned n) const;
  string RenderMethod = ?;

  string DiagnosticType = ?;
}

def ImmAsmOperand : AsmOperandClass {
  let Name = "Imm";
}

/// Operand Types - These provide the built-in operand types that may be used
/// by a target.  Targets can optionally provide their own operand types as
/// needed, though this should not be needed for RISC targets.
class Operand<ValueType ty> {
  ValueType Type = ty;
  string PrintMethod = "printOperand";
  string AsmOperandLowerMethod = ?;
  dag MIOperandInfo = (ops);

  // ParserMatchClass - The "match class" that operands of this type fit
  // in. Match classes are used to define the order in which instructions are
  // match, to ensure that which instructions gets matched is deterministic.
  //
  // The target specific parser must be able to classify an parsed operand
  // into a unique class, which does not partially overlap with any other
  // classes. It can match a subset of some other class, in which case
  // ParserMatchSuperClass should be set to the name of that class.
  AsmOperandClass ParserMatchClass = ImmAsmOperand;
}

def i1imm  : Operand<i1>;
def i8imm  : Operand<i8>;
def i16imm : Operand<i16>;
def i32imm : Operand<i32>;
def i64imm : Operand<i64>;

def f32imm : Operand<f32>;
def f64imm : Operand<f64>;

/// zero_reg definition - Special node to stand for the zero register.
///
def zero_reg;

/// PredicateOperand - This can be used to define a predicate operand for an
/// instruction.  OpTypes specifies the MIOperandInfo for the operand, and
/// AlwaysVal specifies the value of this predicate when set to "always
/// execute".
class PredicateOperand<ValueType ty, dag OpTypes, dag AlwaysVal>
  : Operand<ty> {
  let MIOperandInfo = OpTypes;
  dag DefaultOps = AlwaysVal;
}

/// OptionalDefOperand - This is used to define a optional definition operand
/// for an instruction. DefaultOps is the register the operand represents if
/// none is supplied, e.g. zero_reg.
class OptionalDefOperand<ValueType ty, dag OpTypes, dag defaultops>
  : Operand<ty> {
  let MIOperandInfo = OpTypes;
  dag DefaultOps = defaultops;
}


// InstrInfo - This class should only be instantiated once to provide parameters
// which are global to the the target machine.
//
class InstrInfo {
  // If the target wants to associate some target-specific information with each
  // instruction, it should provide these two lists to indicate how to assemble
  // the target specific information into the 32 bits available.
  //
  list<string> TSFlagsFields = [];
  list<int>    TSFlagsShifts = [];

  // Target can specify its instructions in either big or little-endian formats.
  // For instance, while both Sparc and PowerPC are big-endian platforms, the
  // Sparc manual specifies its instructions in the format [31..0] (big), while
  // PowerPC specifies them using the format [0..31] (little).
  bit isLittleEndianEncoding = 0;
}

// Standard Pseudo Instructions.
let isCodeGenOnly = 1 in {
def PHI : Instruction {
  let OutOperandList = (ops);
  let InOperandList = (ops variable_ops);
  let AsmString = "PHINODE";
  let Namespace = "TargetOpcodes";
}
def INLINEASM : Instruction {
  let OutOperandList = (ops);
  let InOperandList = (ops variable_ops);
  let AsmString = "";
  let Namespace = "TargetOpcodes";
}
def DBG_LABEL : Instruction {
  let OutOperandList = (ops);
  let InOperandList = (ops i32imm:$id);
  let AsmString = "";
  let Namespace = "TargetOpcodes";
  let hasCtrlDep = 1;
}
def EH_LABEL : Instruction {
  let OutOperandList = (ops);
  let InOperandList = (ops i32imm:$id);
  let AsmString = "";
  let Namespace = "TargetOpcodes";
  let hasCtrlDep = 1;
}
def GC_LABEL : Instruction {
  let OutOperandList = (ops);
  let InOperandList = (ops i32imm:$id);
  let AsmString = "";
  let Namespace = "TargetOpcodes";
  let hasCtrlDep = 1;
}
def DECLARE : Instruction {
  let OutOperandList = (ops);
  let InOperandList = (ops variable_ops);
  let AsmString = "";
  let Namespace = "TargetOpcodes";
  let hasCtrlDep = 1;
}
def EXTRACT_SUBREG : Instruction {
  let OutOperandList = (ops unknown:$dst);
  let InOperandList = (ops unknown:$supersrc, i32imm:$subidx);
  let AsmString = "";
  let Namespace = "TargetOpcodes";
  let neverHasSideEffects = 1;
}
def INSERT_SUBREG : Instruction {
  let OutOperandList = (ops unknown:$dst);
  let InOperandList = (ops unknown:$supersrc, unknown:$subsrc, i32imm:$subidx);
  let AsmString = "";
  let Namespace = "TargetOpcodes";
  let neverHasSideEffects = 1;
  let Constraints = "$supersrc = $dst";
}
def IMPLICIT_DEF : Instruction {
  let OutOperandList = (ops unknown:$dst);
  let InOperandList = (ops);
  let AsmString = "";
  let Namespace = "TargetOpcodes";
  let neverHasSideEffects = 1;
  let isReMaterializable = 1;
  let isAsCheapAsAMove = 1;
}
def SUBREG_TO_REG : Instruction {
  let OutOperandList = (ops unknown:$dst);
  let InOperandList = (ops unknown:$implsrc, unknown:$subsrc, i32imm:$subidx);
  let AsmString = "";
  let Namespace = "TargetOpcodes";
  let neverHasSideEffects = 1;
}
def COPY_TO_REGCLASS : Instruction {
  let OutOperandList = (ops unknown:$dst);
  let InOperandList = (ops unknown:$src, i32imm:$regclass);
  let AsmString = "";
  let Namespace = "TargetOpcodes";
  let neverHasSideEffects = 1;
  let isAsCheapAsAMove = 1;
}
}

//===----------------------------------------------------------------------===//
// AsmParser - This class can be implemented by targets that wish to implement
// .s file parsing.
//
// Subtargets can have multiple different assembly parsers (e.g. AT&T vs Intel
// syntax on X86 for example).
//
class AsmParser {
  // AsmParserClassName - This specifies the suffix to use for the asmparser
  // class.  Generated AsmParser classes are always prefixed with the target
  // name.
  string AsmParserClassName  = "AsmParser";

  // Variant - AsmParsers can be of multiple different variants.  Variants are
  // used to support targets that need to parser multiple formats for the
  // assembly language.
  int Variant = 0;

  // CommentDelimiter - If given, the delimiter string used to recognize
  // comments which are hard coded in the .td assembler strings for individual
  // instructions.
  string CommentDelimiter = "";

  // RegisterPrefix - If given, the token prefix which indicates a register
  // token. This is used by the matcher to automatically recognize hard coded
  // register tokens as constrained registers, instead of tokens, for the
  // purposes of matching.
  string RegisterPrefix = "";
}
def DefaultAsmParser : AsmParser;

/// This class allows targets to define assembler mnemonic
/// aliases.  This should be used when all forms of one mnemonic are accepted
/// with a different mnemonic.  For example, X86 allows:
///   sal %al, 1    -> shl %al, 1
///   sal %ax, %cl  -> shl %ax, %cl
///   sal %eax, %cl -> shl %eax, %cl
/// etc.  Though "sal" is accepted with many forms, all of them are directly
/// translated to a shl, so it can be handled with (in the case of X86, it
/// actually has one for each suffix as well):
///   def : MnemonicAlias<"sal", "shl">;
///
/// Mnemonic aliases are mapped before any other translation in the match phase,
/// and do allow Requires predicates, e.g.:
///
///  def : MnemonicAlias<"pushf", "pushfq">, Requires<[In64BitMode]>;
///  def : MnemonicAlias<"pushf", "pushfl">, Requires<[In32BitMode]>;
///
/// Mnemonic aliases can also be constrained to specific variants, e.g.:
///
///  def : MnemonicAlias<"pushf", "pushfq", "att">, Requires<[In64BitMode]>;
///
/// If no variant (e.g., "att" or "intel") is specified then the alias is
/// applied unconditionally.
class MnemonicAlias<string From, string To, string VariantName = ""> {
  string FromMnemonic = From;
  string ToMnemonic = To;
  string AsmVariantName = VariantName;
  list<Predicate> Predicates = [];
}

/// This defines an alternate assembly syntax that is allowed to
/// match an instruction that has a different (more canonical) assembly
/// representation.
class InstAlias<string Asm, dag Result, int Emit = 1> {
  string AsmString = Asm;
  dag ResultInst = Result;
  int EmitPriority = Emit;
  list<Predicate> Predicates = [];
  bit UseInstAsmMatchConverter = 1;
  string AsmVariantName = "";
}

//===----------------------------------------------------------------------===//
// AsmWriter - This class can be implemented by targets that need to customize
// the format of the .s file writer.
//
// Subtargets can have multiple different asmwriters (e.g. AT&T vs Intel syntax
// on X86 for example).
//
class AsmWriter {
  // AsmWriterClassName - This specifies the suffix to use for the asmwriter
  // class.  Generated AsmWriter classes are always prefixed with the target
  // name.
  string AsmWriterClassName  = "AsmPrinter";

  // InstFormatName - AsmWriters can specify the name of the format string to
  // print instructions with.
  string InstFormatName = "AsmString";

  // Variant - AsmWriters can be of multiple different variants.  Variants are
  // used to support targets that need to emit assembly code in ways that are
  // mostly the same for different targets, but have minor differences in
  // syntax.  If the asmstring contains {|} characters in them, this integer
  // will specify which alternative to use.  For example "{x|y|z}" with Variant
  // == 1, will expand to "y".
  int Variant = 0;


  // FirstOperandColumn/OperandSpacing - If the assembler syntax uses a columnar
  // layout, the asmwriter can actually generate output in this columns (in
  // verbose-asm mode).  These two values indicate the width of the first column
  // (the "opcode" area) and the width to reserve for subsequent operands.  When
  // verbose asm mode is enabled, operands will be indented to respect this.
  int FirstOperandColumn = -1;

  // OperandSpacing - Space between operand columns.
  int OperandSpacing = -1;
}
def DefaultAsmWriter : AsmWriter;


//===----------------------------------------------------------------------===//
// Target - This class contains the "global" target information
//
class Target {
  // InstructionSet - Instruction set description for this target.
  InstrInfo InstructionSet;

  // AssemblyParsers - The AsmParser instances available for this target.
  list<AsmParser> AssemblyParsers = [DefaultAsmParser];

  // AssemblyWriters - The AsmWriter instances available for this target.
  list<AsmWriter> AssemblyWriters = [DefaultAsmWriter];
}

//===----------------------------------------------------------------------===//
// SubtargetFeature - A characteristic of the chip set.
//
class SubtargetFeature<string n, string a,  string v, string d,
                       list<SubtargetFeature> i = []> {
  // Name - Feature name.  Used by command line (-mattr=) to determine the
  // appropriate target chip.
  //
  string Name = n;

  // Attribute - Attribute to be set by feature.
  //
  string Attribute = a;

  // Value - Value the attribute to be set to by feature.
  //
  string Value = v;

  // Desc - Feature description.  Used by command line (-mattr=) to display help
  // information.
  //
  string Desc = d;

  // Implies - Features that this feature implies are present. If one of those
  // features isn't set, then this one shouldn't be set either.
  //
  list<SubtargetFeature> Implies = i;
}

//===----------------------------------------------------------------------===//
// Processor chip sets - These values represent each of the chip sets supported
// by the scheduler.  Each Processor definition requires corresponding
// instruction itineraries.
//
class Processor<string n, ProcessorItineraries pi, list<SubtargetFeature> f> {
  // Name - Chip set name.  Used by command line (-mcpu=) to determine the
  // appropriate target chip.
  //
  string Name = n;

  // ProcItin - The scheduling information for the target processor.
  //
  ProcessorItineraries ProcItin = pi;

  // Features - list of
  list<SubtargetFeature> Features = f;
}

//===----------------------------------------------------------------------===//
// Pull in the common support for calling conventions.
// Commented by Jianping Zeng on 2017.6.19
include "./TargetCallingConv.td"

//===----------------------------------------------------------------------===//
// Pull in the common support for DAG isel generation.
//
// Commented by Jianping Zeng on 2017.6.19
include "./TargetSelectionDAG.td"
