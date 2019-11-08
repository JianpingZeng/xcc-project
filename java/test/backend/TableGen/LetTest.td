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

  //list<Register> Uses = []; // Default to using no non-operand registers
  //list<Register> Defs = []; // Default to modifying no non-operand registers

  // Predicates - List of predicates which will be turned into isel matching
  // code.
  //list<Predicate> Predicates = [];

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
  bit usesCustomInserter = 0; // Pseudo instr needing special help.
  bit hasCtrlDep   = 0;     // Does this instruction r/w ctrl-flow chains?
  bit isNotDuplicable = 0;  // Is it unsafe to duplicate this instruction?
  bit isAsCheapAsAMove = 0; // As cheap (or cheaper) than a move instruction.

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


  string Constraints = "";  // OperandConstraint, e.g. $src = $dst.

  /// DisableEncoding - List of operand names (e.g. "$op1,$op2") that should not
  /// be encoded into the output machineinstr.
  string DisableEncoding = "";
}

class X86Inst<bits<8> opcod, string AsmStr>
  : Instruction {
  let Namespace = "X86";

  bits<8> Opcode = opcod;
  //bits<6> FormBits = Form.Value;
  //ImmType ImmT = i;
  //bits<3> ImmTypeBits = ImmT.Value;

  //dag OutOperandList = outs;
  //dag InOperandList = ins;
  string AsmString = AsmStr;

  //
  // Attributes specific to X86 instructions...
  //
  bit hasOpSizePrefix = 0;  // Does this inst have a 0x66 prefix?
  bit hasAdSizePrefix = 0;  // Does this inst have a 0x67 prefix?

  bits<4> Prefix = 0;       // Which prefix byte does this inst have?
  bit hasREX_WPrefix  = 0;  // Does this inst requires the REX.W prefix?
  //FPFormat FPForm;          // What flavor of FP instruction is this?
  bits<3> FPFormBits = 0;
  bit hasLockPrefix = 0;    // Does this inst have a 0xF0 prefix?
  bits<2> SegOvrBits = 0;   // Segment override prefix.
}

class I<bits<8> o, string asm>
  : X86Inst<o,  asm> {
  //let Pattern = pattern;
  let CodeSize = 3;
}

let isTwoAddress = 1 in {

let isTwoAddress = 0 in {
  def INC8m  : I<0xFE,"inc{b}\t$dst">;
}

// Arithmetic.
def ADD32rr  : I<0x01, "add{l}\t{$src2, $dst|$dst, $src2}">;
}
