/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.codegen.dagisel;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public enum NodeType {
  // DELETED_NODE - This is an illegal value that is used to catch
  // errors.  This opcode is not a legal opcode for any node.
  DELETED_NODE,

  // EntryToken - This is the marker used to indicate the start of the region.
  EntryToken,

  // TokenFactor - This node takes multiple tokens as input and produces a
  // single token result.  This is used to represent the fact that the operand
  // operators are independent of each other.
  TokenFactor,

  // AssertSext, AssertZext - These nodes record if a register contains a
  // value that has already been zero or sign extended from a narrower type.
  // These nodes take two operands.  The first is the node that has already
  // been extended, and the second is a value type node indicating the width
  // of the extension
  AssertSext, AssertZext,

  // Various leaf nodes.
  BasicBlock, VALUETYPE, CONDCODE, Register,
  Constant, ConstantFP,
  GlobalAddress, GlobalTLSAddress, FrameIndex,
  JumpTable, ConstantPool, ExternalSymbol,

  // The address of the GOT
  GLOBAL_OFFSET_TABLE,

  // FRAMEADDR, RETURNADDR - These nodes represent llvm.frameaddress and
  // llvm.returnaddress on the DAG.  These nodes take one operand, the index
  // of the frame or return address to return.  An index of zero corresponds
  // to the current function's frame or return address, an index of one to the
  // parent's frame or return address, and so on.
  FRAMEADDR, RETURNADDR,

  // FRAME_TO_ARGS_OFFSET - This node represents offset from frame pointer to
  // first (possible) on-stack argument. This is needed for correct stack
  // adjustment during unwind.
  FRAME_TO_ARGS_OFFSET,

  // RESULT, OUTCHAIN = EXCEPTIONADDR(INCHAIN) - This node represents the
  // address of the exception block on entry to an landing pad block.
  EXCEPTIONADDR,

  // RESULT, OUTCHAIN = LSDAADDR(INCHAIN) - This node represents the
  // address of the Language Specific Data Area for the enclosing function.
  LSDAADDR,

  // RESULT, OUTCHAIN = EHSELECTION(INCHAIN, EXCEPTION) - This node represents
  // the selection index of the exception thrown.
  EHSELECTION,

  // OUTCHAIN = EH_RETURN(INCHAIN, OFFSET, HANDLER) - This node represents
  // 'eh_return' gcc dwarf builtin, which is used to return from
  // exception. The general meaning is: adjust stack by OFFSET and pass
  // execution to HANDLER. Many platform-related details also :)
  EH_RETURN,

  // TargetConstant* - Like Constant*, but the DAG does not do any folding or
  // simplification of the constant.
  TargetConstant,
  TargetConstantFP,

  // TargetGlobalAddress - Like GlobalAddress, but the DAG does no folding or
  // anything else with this node, and this is valid in the target-specific
  // dag, turning into a GlobalAddress operand.
  TargetGlobalAddress,
  TargetGlobalTLSAddress,
  TargetFrameIndex,
  TargetJumpTable,
  TargetConstantPool,
  TargetExternalSymbol,

  /// RESULT = INTRINSIC_WO_CHAIN(INTRINSICID, arg1, arg2, ...)
  /// This node represents a target intrinsic function with no side effects.
  /// The first operand is the ID number of the intrinsic from the
  /// llvm::Intrinsic namespace.  The operands to the intrinsic follow.  The
  /// node has returns the result of the intrinsic.
  INTRINSIC_WO_CHAIN,

  /// RESULT,OUTCHAIN = INTRINSIC_W_CHAIN(INCHAIN, INTRINSICID, arg1, ...)
  /// This node represents a target intrinsic function with side effects that
  /// returns a result.  The first operand is a chain pointer.  The second is
  /// the ID number of the intrinsic from the llvm::Intrinsic namespace.  The
  /// operands to the intrinsic follow.  The node has two results, the result
  /// of the intrinsic and an output chain.
  INTRINSIC_W_CHAIN,

  /// OUTCHAIN = INTRINSIC_VOID(INCHAIN, INTRINSICID, arg1, arg2, ...)
  /// This node represents a target intrinsic function with side effects that
  /// does not return a result.  The first operand is a chain pointer.  The
  /// second is the ID number of the intrinsic from the llvm::Intrinsic
  /// namespace.  The operands to the intrinsic follow.
  INTRINSIC_VOID,

  // CopyToReg - This node has three operands: a chain, a register number to
  // set to this value, and a value.
  CopyToReg,

  // CopyFromReg - This node indicates that the input value is a virtual or
  // physical register that is defined outside of the scope of this
  // SelectionDAG.  The register is available from the RegisterSDNode object.
  CopyFromReg,

  // UNDEF - An undefined node
  UNDEF,

  // EXTRACT_ELEMENT - This is used to get the lower or upper (determined by
  // a Constant, which is required to be operand #1) half of the integer or
  // float value specified as operand #0.  This is only for use before
  // legalization, for values that will be broken into multiple registers.
  EXTRACT_ELEMENT,

  // BUILD_PAIR - This is the opposite of EXTRACT_ELEMENT in some ways.  Given
  // two values of the same integer value type, this produces a value twice as
  // big.  Like EXTRACT_ELEMENT, this can only be used before legalization.
  BUILD_PAIR,

  // MERGE_VALUES - This node takes multiple discrete operands and returns
  // them all as its individual results.  This nodes has exactly the same
  // number of inputs and outputs. This node is useful for some pieces of the
  // code generator that want to think about a single node with multiple
  // results, not multiple nodes.
  MERGE_VALUES,

  // Simple integer binary arithmetic operators.
  ADD, SUB, MUL, SDIV, UDIV, SREM, UREM,

  // SMUL_LOHI/UMUL_LOHI - Multiply two integers of type iN, producing
  // a signed/unsigned value of type i[2*N], and return the full value as
  // two results, each of type iN.
  SMUL_LOHI, UMUL_LOHI,

  // SDIVREM/UDIVREM - Divide two integers and produce both a quotient and
  // remainder result.
  SDIVREM, UDIVREM,

  // CARRY_FALSE - This node is used when folding other nodes,
  // like ADDC/SUBC, which indicate the carry result is always false.
  CARRY_FALSE,

  // Carry-setting nodes for multiple precision addition and subtraction.
  // These nodes take two operands of the same value type, and produce two
  // results.  The first result is the normal add or sub result, the second
  // result is the carry flag result.
  ADDC, SUBC,

  // Carry-using nodes for multiple precision addition and subtraction.  These
  // nodes take three operands: The first two are the normal lhs and rhs to
  // the add or sub, and the third is the input carry flag.  These nodes
  // produce two results; the normal result of the add or sub, and the output
  // carry flag.  These nodes both read and write a carry flag to allow them
  // to them to be chained together for add and sub of arbitrarily large
  // values.
  ADDE, SUBE,

  // RESULT, BOOL = [SU]ADDO(LHS, RHS) - Overflow-aware nodes for addition.
  // These nodes take two operands: the normal LHS and RHS to the add. They
  // produce two results: the normal result of the add, and a boolean that
  // indicates if an overflow occured (*not* a flag, because it may be stored
  // to memory, etc.).  If the type of the boolean is not i1 then the high
  // bits conform to getBooleanContents.
  // These nodes are generated from the llvm.[su]add.with.overflow intrinsics.
  SADDO, UADDO,

  // Same for subtraction
  SSUBO, USUBO,

  // Same for multiplication
  SMULO, UMULO,

  // Simple binary floating point operators.
  FADD, FSUB, FMUL, FDIV, FREM,

  // FCOPYSIGN(X, Y) - Return the value of X with the sign of Y.  NOTE: This
  // DAG node does not require that X and Y have the same type, just that they
  // are both floating point.  X and the result must have the same type.
  // FCOPYSIGN(f32, f64) is allowed.
  FCOPYSIGN,

  // INT = FGETSIGN(FP) - Return the sign bit of the specified floating point
  // value as an integer 0/1 value.
  FGETSIGN,

  /// BUILD_VECTOR(ELT0, ELT1, ELT2, ELT3,...) - Return a vector with the
  /// specified, possibly variable, elements.  The number of elements is
  /// required to be a power of two.  The types of the operands must all be
  /// the same and must match the vector element type, except that integer
  /// types are allowed to be larger than the element type, in which case
  /// the operands are implicitly truncated.
  BUILD_VECTOR,

  /// INSERT_VECTOR_ELT(VECTOR, VAL, IDX) - Returns VECTOR with the element
  /// at IDX replaced with VAL.  If the type of VAL is larger than the vector
  /// element type then VAL is truncated before replacement.
  INSERT_VECTOR_ELT,

  /// EXTRACT_VECTOR_ELT(VECTOR, IDX) - Returns a single element from VECTOR
  /// identified by the (potentially variable) element number IDX.  If the
  /// return type is an integer type larger than the element type of the
  /// vector, the result is extended to the width of the return type.
  EXTRACT_VECTOR_ELT,

  /// CONCAT_VECTORS(VECTOR0, VECTOR1, ...) - Given a number of values of
  /// vector type with the same length and element type, this produces a
  /// concatenated vector result value, with length equal to the sum of the
  /// lengths of the input vectors.
  CONCAT_VECTORS,

  /// EXTRACT_SUBVECTOR(VECTOR, IDX) - Returns a subvector from VECTOR (an
  /// vector value) starting with the (potentially variable) element number
  /// IDX, which must be a multiple of the result vector length.
  EXTRACT_SUBVECTOR,

  /// VECTOR_SHUFFLE(VEC1, VEC2) - Returns a vector, of the same type as
  /// VEC1/VEC2.  A VECTOR_SHUFFLE node also contains an array of constant int
  /// values that indicate which value (or undef) each result element will
  /// get.  These constant ints are accessible through the
  /// ShuffleVectorSDNode class.  This is quite similar to the Altivec
  /// 'vperm' instruction, except that the indices must be constants and are
  /// in terms of the element size of VEC1/VEC2, not in terms of bytes.
  VECTOR_SHUFFLE,

  /// SCALAR_TO_VECTOR(VAL) - This represents the operation of loading a
  /// scalar value into element 0 of the resultant vector type.  The top
  /// elements 1 to N-1 of the N-element vector are undefined.  The type
  /// of the operand must match the vector element type, except when they
  /// are integer types.  In this case the operand is allowed to be wider
  /// than the vector element type, and is implicitly truncated to it.
  SCALAR_TO_VECTOR,

  // MULHU/MULHS - Multiply high - Multiply two integers of type iN, producing
  // an unsigned/signed value of type i[2*N], then return the top part.
  MULHU, MULHS,

  // Bitwise operators - logical and, logical or, logical xor, shift left,
  // shift right algebraic (shift in sign bits), shift right logical (shift in
  // zeroes), rotate left, rotate right, and byteswap.
  AND, OR, XOR, SHL, SRA, SRL, ROTL, ROTR, BSWAP,

  // Counting operators
  CTTZ, CTLZ, CTPOP,

  // Select(COND, TRUEVAL, FALSEVAL).  If the type of the boolean COND is not
  // i1 then the high bits must conform to getBooleanContents.
  SELECT,

  // Select with condition operator - This selects between a true value and
  // a false value (ops #2 and #3) based on the boolean result of comparing
  // the lhs and rhs (ops #0 and #1) of a conditional expression with the
  // condition code in op #4, a CondCodeSDNode.
  SELECT_CC,

  // SetCC operator - This evaluates to a true value iff the condition is
  // true.  If the result value type is not i1 then the high bits conform
  // to getBooleanContents.  The operands to this are the left and right
  // operands to compare (ops #0, and #1) and the condition code to compare
  // them with (op #2) as a CondCodeSDNode.
  SETCC,

  // RESULT = VSETCC(LHS, RHS, COND) operator - This evaluates to a vector of
  // integer elements with all bits of the result elements set to true if the
  // comparison is true or all cleared if the comparison is false.  The
  // operands to this are the left and right operands to compare (LHS/RHS) and
  // the condition code to compare them with (COND) as a CondCodeSDNode.
  VSETCC,

  // SHL_PARTS/SRA_PARTS/SRL_PARTS - These operators are used for expanded
  // integer shift operations, just like ADD/SUB_PARTS.  The operation
  // ordering is:
  //       [Lo,Hi] = op [LoLHS,HiLHS], Amt
  SHL_PARTS, SRA_PARTS, SRL_PARTS,

  // Conversion operators.  These are all single input single output
  // operations.  For all of these, the result type must be strictly
  // wider or narrower (depending on the operation) than the source
  // type.

  // SIGN_EXTEND - Used for integer types, replicating the sign bit
  // into new bits.
  SIGN_EXTEND,

  // ZERO_EXTEND - Used for integer types, zeroing the new bits.
  ZERO_EXTEND,

  // ANY_EXTEND - Used for integer types.  The high bits are undefined.
  ANY_EXTEND,

  // TRUNCATE - Completely drop the high bits.
  TRUNCATE,

  // [SU]INT_TO_FP - These operators convert integers (whose interpreted sign
  // depends on the first letter) to floating point.
  SINT_TO_FP,
  UINT_TO_FP,

  // SIGN_EXTEND_INREG - This operator atomically performs a SHL/SRA pair to
  // sign extend a small value in a large integer register (e.g. sign
  // extending the low 8 bits of a 32-bit register to fill the top 24 bits
  // with the 7th bit).  The size of the smaller type is indicated by the 1th
  // operand, a ValueType node.
  SIGN_EXTEND_INREG,

  /// FP_TO_[US]INT - Convert a floating point value to a signed or unsigned
  /// integer.
  FP_TO_SINT,
  FP_TO_UINT,

  /// X = FP_ROUND(Y, TRUNC) - Rounding 'Y' from a larger floating point type
  /// down to the precision of the destination VT.  TRUNC is a flag, which is
  /// always an integer that is zero or one.  If TRUNC is 0, this is a
  /// normal rounding, if it is 1, this FP_ROUND is known to not change the
  /// value of Y.
  ///
  /// The TRUNC = 1 case is used in cases where we know that the value will
  /// not be modified by the node, because Y is not using any of the extra
  /// precision of source type.  This allows certain transformations like
  /// FP_EXTEND(FP_ROUND(X,1)) -> X which are not safe for
  /// FP_EXTEND(FP_ROUND(X,0)) because the extra bits aren't removed.
  FP_ROUND,

  // FLT_ROUNDS_ - Returns current rounding mode:
  // -1 Undefined
  //  0 Round to 0
  //  1 Round to nearest
  //  2 Round to +inf
  //  3 Round to -inf
  FLT_ROUNDS_,

  /// X = FP_ROUND_INREG(Y, VT) - This operator takes an FP register, and
  /// rounds it to a floating point value.  It then promotes it and returns it
  /// in a register of the same size.  This operation effectively just
  /// discards excess precision.  The type to round down to is specified by
  /// the VT operand, a VTSDNode.
  FP_ROUND_INREG,

  /// X = FP_EXTEND(Y) - Extend a smaller FP type into a larger FP type.
  FP_EXTEND,

  // BIT_CONVERT - Theis operator converts between integer and FP values, as
  // if one was stored to memory as integer and the other was loaded from the
  // same address (or equivalently for vector format conversions, etc).  The
  // source and result are required to have the same bit size (e.g.
  // f32 <-> i32).  This can also be used for int-to-int or fp-to-fp
  // conversions, but that is a noop, deleted by getNode().
  BIT_CONVERT,

  // CONVERT_RNDSAT - This operator is used to support various conversions
  // between various types (float, signed, unsigned and vectors of those
  // types) with rounding and saturation. NOTE: Avoid using this operator as
  // most target don't support it and the operator might be removed in the
  // future. It takes the following arguments:
  //   0) value
  //   1) dest type (type to convert to)
  //   2) src type (type to convert from)
  //   3) rounding imm
  //   4) saturation imm
  //   5) ISD::CvtCode indicating the type of conversion to do
  CONVERT_RNDSAT,

  // FNEG, FABS, FSQRT, FSIN, FCOS, FPOWI, FPOW,
  // FLOG, FLOG2, FLOG10, FEXP, FEXP2,
  // FCEIL, FTRUNC, FRINT, FNEARBYINT, FFLOOR - Perform various unary floating
  // point operations. These are inspired by libm.
  FNEG, FABS, FSQRT, FSIN, FCOS, FPOWI, FPOW,
  FLOG, FLOG2, FLOG10, FEXP, FEXP2,
  FCEIL, FTRUNC, FRINT, FNEARBYINT, FFLOOR,

  // LOAD and STORE have token chains as their first operand, then the same
  // operands as an LLVM load/store instruction, then an offset node that
  // is added / subtracted from the base pointer to form the address (for
  // indexed memory ops).
  LOAD, STORE,

  // DYNAMIC_STACKALLOC - Allocate some number of bytes on the stack aligned
  // to a specified boundary.  This node always has two return values: a new
  // stack pointer value and a chain. The first operand is the token chain,
  // the second is the number of bytes to allocate, and the third is the
  // alignment boundary.  The size is guaranteed to be a multiple of the stack
  // alignment, and the alignment is guaranteed to be bigger than the stack
  // alignment (if required) or 0 to get standard stack alignment.
  DYNAMIC_STACKALLOC,

  // Control flow instructions.  These all have token chains.

  // BR - Unconditional branch.  The first operand is the chain
  // operand, the second is the MBB to branch to.
  BR,

  // BRIND - Indirect branch.  The first operand is the chain, the second
  // is the value to branch to, which must be of the same type as the target's
  // pointer type.
  BRIND,

  // BR_JT - Jumptable branch. The first operand is the chain, the second
  // is the jumptable index, the last one is the jumptable entry index.
  BR_JT,

  // BRCOND - Conditional branch.  The first operand is the chain, the
  // second is the condition, the third is the block to branch to if the
  // condition is true.  If the type of the condition is not i1, then the
  // high bits must conform to getBooleanContents.
  BRCOND,

  // BR_CC - Conditional branch.  The behavior is like that of SELECT_CC, in
  // that the condition is represented as condition code, and two nodes to
  // compare, rather than as a combined SetCC node.  The operands in order are
  // chain, cc, lhs, rhs, block to branch to if condition is true.
  BR_CC,

  // INLINEASM - Represents an inline asm block.  This node always has two
  // return values: a chain and a flag result.  The inputs are as follows:
  //   Operand #0   : Input chain.
  //   Operand #1   : a ExternalSymbolSDNode with a pointer to the asm string.
  //   Operand #2n+2: A RegisterNode.
  //   Operand #2n+3: A TargetConstant, indicating if the reg is a use/def
  //   Operand #last: Optional, an incoming flag.
  INLINEASM,

  // DBG_LABEL, EH_LABEL - Represents a label in mid basic block used to track
  // locations needed for debug and exception handling tables.  These nodes
  // take a chain as input and return a chain.
  DBG_LABEL,
  EH_LABEL,

  // DECLARE - Represents a llvm.dbg.declare intrinsic. It's used to track
  // local variable declarations for debugging information. First operand is
  // a chain, while the next two operands are first two arguments (address
  // and variable) of a llvm.dbg.declare instruction.
  DECLARE,

  // STACKSAVE - STACKSAVE has one operand, an input chain.  It produces a
  // value, the same type as the pointer type for the system, and an output
  // chain.
  STACKSAVE,

  // STACKRESTORE has two operands, an input chain and a pointer to restore to
  // it returns an output chain.
  STACKRESTORE,

  // CALLSEQ_START/CALLSEQ_END - These operators mark the beginning and end of
  // a call sequence, and carry arbitrary information that target might want
  // to know.  The first operand is a chain, the rest are specified by the
  // target and not touched by the DAG optimizers.
  // CALLSEQ_START..CALLSEQ_END pairs may not be nested.
  CALLSEQ_START,  // Beginning of a call sequence
  CALLSEQ_END,    // End of a call sequence

  // VAARG - VAARG has three operands: an input chain, a pointer, and a
  // SRCVALUE.  It returns a pair of values: the vaarg value and a new chain.
  VAARG,

  // VACOPY - VACOPY has five operands: an input chain, a destination pointer,
  // a source pointer, a SRCVALUE for the destination, and a SRCVALUE for the
  // source.
  VACOPY,

  // VAEND, VASTART - VAEND and VASTART have three operands: an input chain, a
  // pointer, and a SRCVALUE.
  VAEND, VASTART,

  // SRCVALUE - This is a node type that holds a Value* that is used to
  // make reference to a value in the LLVM IR.
  SRCVALUE,

  // MEMOPERAND - This is a node that contains a MachineMemOperand which
  // records information about a memory reference. This is used to make
  // AliasAnalysis queries from the backend.
  MEMOPERAND,

  // PCMARKER - This corresponds to the pcmarker intrinsic.
  PCMARKER,

  // READCYCLECOUNTER - This corresponds to the readcyclecounter intrinsic.
  // The only operand is a chain and a value and a chain are produced.  The
  // value is the contents of the architecture specific cycle counter like
  // register (or other high accuracy low latency clock source)
  READCYCLECOUNTER,

  // HANDLENODE node - Used as a handle for various purposes.
  HANDLENODE,

  // DBG_STOPPOINT - This node is used to represent a source location for
  // debug info.  It takes token chain as input, and carries a line number,
  // column number, and a pointer to a CompileUnit object identifying
  // the containing compilation unit.  It produces a token chain as output.
  DBG_STOPPOINT,

  // DEBUG_LOC - This node is used to represent source line information
  // embedded in the code.  It takes a token chain as input, then a line
  // number, then a column then a file id (provided by MachineModuleInfo.) It
  // produces a token chain as output.
  DEBUG_LOC,

  // TRAMPOLINE - This corresponds to the init_trampoline intrinsic.
  // It takes as input a token chain, the pointer to the trampoline,
  // the pointer to the nested function, the pointer to pass for the
  // 'nest' parameter, a SRCVALUE for the trampoline and another for
  // the nested function (allowing targets to access the original
  // Function*).  It produces the result of the intrinsic and a token
  // chain as output.
  TRAMPOLINE,

  // TRAP - Trapping instruction
  TRAP,

  // PREFETCH - This corresponds to a prefetch intrinsic. It takes chains are
  // their first operand. The other operands are the address to prefetch,
  // read / write specifier, and locality specifier.
  PREFETCH,

  // OUTCHAIN = MEMBARRIER(INCHAIN, load-load, load-store, store-load,
  //                       store-store, device)
  // This corresponds to the memory.barrier intrinsic.
  // it takes an input chain, 4 operands to specify the type of barrier, an
  // operand specifying if the barrier applies to device and uncached memory
  // and produces an output chain.
  MEMBARRIER,

  // Val, OUTCHAIN = ATOMIC_CMP_SWAP(INCHAIN, ptr, cmp, swap)
  // this corresponds to the atomic.lcs intrinsic.
  // cmp is compared to *ptr, and if equal, swap is stored in *ptr.
  // the return is always the original value in *ptr
  ATOMIC_CMP_SWAP,

  // Val, OUTCHAIN = ATOMIC_SWAP(INCHAIN, ptr, amt)
  // this corresponds to the atomic.swap intrinsic.
  // amt is stored to *ptr atomically.
  // the return is always the original value in *ptr
  ATOMIC_SWAP,

  // Val, OUTCHAIN = ATOMIC_LOAD_[OpName](INCHAIN, ptr, amt)
  // this corresponds to the atomic.load.[OpName] intrinsic.
  // op(*ptr, amt) is stored to *ptr atomically.
  // the return is always the original value in *ptr
  ATOMIC_LOAD_ADD,
  ATOMIC_LOAD_SUB,
  ATOMIC_LOAD_AND,
  ATOMIC_LOAD_OR,
  ATOMIC_LOAD_XOR,
  ATOMIC_LOAD_NAND,
  ATOMIC_LOAD_MIN,
  ATOMIC_LOAD_MAX,
  ATOMIC_LOAD_UMIN,
  ATOMIC_LOAD_UMAX,

  // BUILTIN_OP_END - This must be the last enum value in this list.
  BUILTIN_OP_END
}
