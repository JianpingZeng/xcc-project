package backend.codegen.dagisel;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import tools.Util;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class ISD {
  //===--------------------------------------------------------------------===//
  /*
   * This enum defines the target-independent operators for a SelectionDAG.

   * Targets may also define target-dependent operator codes for SDNodes. For
   * example, on x86, these are the enum values in the X86ISD namespace.
   * Targets should aim to use target-independent operators to model their
   * instruction sets as much as possible, and only use target-dependent
   * operators when they have special requirements.
   *
   * Finally, during and after selection proper, SNodes may use special
   * operator codes that correspond directly with MachineInstr opcodes. These
   * are used to represent selected instructions. See the isMachineOpcode()
   * and getMachineOpcode() member functions of SDNode.
  */

  // Conversion operators.  These are all single input single output
  // operations.  For all of these, the result type must be strictly
  // wider or narrower (depending on the operation) than the source
  // type.

  // Control flow instructions.  These all have token chains.

  // DELETED_NODE - This is an illegal value that is used to catch
  // errors.  This opcode is not a legal opcode for any node.
  public static final int DELETED_NODE = 0;
  // EntryToken - This is the marker used to indicate the start of the region.
  public static final int EntryToken = 1;
  // TokenFactor - This node takes multiple tokens as input and produces a
  // single token result.  This is used to represent the fact that the operand
  // operators are independent of each other.
  public static final int TokenFactor = EntryToken + 1;
  // AssertSext, AssertZext - These nodes record if a register contains a
  // value that has already been zero or sign extended from a narrower type.
  // These nodes take two operands.  The first is the node that has already
  // been extended, and the second is a value type node indicating the width
  // of the extension
  public static final int AssertSext = TokenFactor + 1;
  public static final int AssertZext = AssertSext + 1;
  // Various leaf nodes.
  public static final int BasicBlock = AssertZext + 1;
  public static final int VALUETYPE = BasicBlock + 1;
  public static final int CONDCODE = VALUETYPE + 1;
  public static final int Register = CONDCODE + 1;
  public static final int Constant = Register + 1;
  public static final int ConstantFP = Constant + 1;
  public static final int GlobalAddress = ConstantFP + 1;
  public static final int GlobalTLSAddress = GlobalAddress + 1;
  public static final int FrameIndex = GlobalTLSAddress + 1;
  public static final int JumpTable = FrameIndex + 1;
  public static final int ConstantPool = JumpTable + 1;
  public static final int ExternalSymbol = ConstantPool + 1;
  public static final int BlockAddress = ExternalSymbol + 1;

  // The address of the GOT
  public static final int GLOBAL_OFFSET_TABLE = BlockAddress + 1;
  // FRAMEADDR, RETURNADDR - These nodes represent llvm.frameaddress and
  // llvm.returnaddress on the DAG.  These nodes take one operand, the index
  // of the frame or return address to return.  An index of zero corresponds
  // to the current function's frame or return address, an index of one to the
  // parent's frame or return address, and so on.
  public static final int FRAMEADDR = GLOBAL_OFFSET_TABLE + 1;
  public static final int RETURNADDR = FRAMEADDR + 1;
  // FRAME_TO_ARGS_OFFSET - This node represents offset from frame pointer to
  // first (possible) on-stack argument. This is needed for correct stack
  // adjustment during unwind.
  public static final int FRAME_TO_ARGS_OFFSET = RETURNADDR + 1;
  // RESULT, OUTCHAIN = EXCEPTIONADDR(INCHAIN) - This node represents the
  // address of the exception block on entry to an landing pad block.
  public static final int EXCEPTIONADDR = FRAME_TO_ARGS_OFFSET + 1;
  // RESULT, OUTCHAIN = LSDAADDR(INCHAIN) - This node represents the
  // address of the Language Specific Data Area for the enclosing function.
  public static final int LSDAADDR = EXCEPTIONADDR + 1;
  // RESULT, OUTCHAIN = EHSELECTION(INCHAIN, EXCEPTION) - This node represents
  // the selection index of the exception thrown.
  public static final int EHSELECTION = LSDAADDR + 1;
  // OUTCHAIN = EH_RETURN(INCHAIN, OFFSET, HANDLER) - This node represents
  // 'eh_return' gcc dwarf builtin, which is used to return from
  // exception. The general meaning is: adjust stack by OFFSET and pass
  // execution to HANDLER. Many platform-related details also :)
  public static final int EH_RETURN = EHSELECTION + 1;

  // RESULT, OUTCHAIN = EH_SJLJ_SETJMP(INCHAIN, buffer)
  // This node operator corresponds to the eh.sjlj.setjmp intrinsic.
  public static final int EH_SJLJ_SETJMP = EH_RETURN + 1;

  // OUTCHAIN = EH_SJLJ_LONGJMP(INCHAIN, buffer)
  // This node operator corresponds to the eh.sjlj.longjmp intrinsic.
  public static final int EH_SJLJ_LONGJMP = EH_SJLJ_SETJMP + 1;

  // TargetConstant* - Like Constant*, but the DAG does not do any folding or
  // simplification of the constant.
  public static final int TargetConstant = EH_SJLJ_LONGJMP + 1;
  public static final int TargetConstantFP = TargetConstant + 1;
  // TargetGlobalAddress - Like GlobalAddress, but the DAG does no folding or
  // anything else with this node, and this is valid in the target-specific
  // dag, turning into a GlobalAddress operand.
  public static final int TargetGlobalAddress = TargetConstantFP + 1;
  public static final int TargetGlobalTLSAddress = TargetGlobalAddress + 1;
  public static final int TargetFrameIndex = TargetGlobalTLSAddress + 1;
  public static final int TargetJumpTable = TargetFrameIndex + 1;
  public static final int TargetConstantPool = TargetJumpTable + 1;
  public static final int TargetExternalSymbol = TargetConstantPool + 1;
  public static final int TargetBlockAddress = TargetExternalSymbol + 1;

  /// RESULT = INTRINSIC_WO_CHAIN(INTRINSICID, arg1, arg2, ...)
  /// This node represents a target intrinsic function with no side effects.
  /// The first operand is the ID number of the intrinsic from the
  /// llvm::Intrinsic namespace.  The operands to the intrinsic follow.  The
  /// node has returns the result of the intrinsic.
  public static final int INTRINSIC_WO_CHAIN = TargetBlockAddress + 1;
  /// RESULT,OUTCHAIN = INTRINSIC_W_CHAIN(INCHAIN, INTRINSICID, arg1, ...)
  /// This node represents a target intrinsic function with side effects that
  /// returns a result.  The first operand is a chain pointer.  The second is
  /// the ID number of the intrinsic from the llvm::Intrinsic namespace.  The
  /// operands to the intrinsic follow.  The node has two results, the result
  /// of the intrinsic and an output chain.
  public static final int INTRINSIC_W_CHAIN = INTRINSIC_WO_CHAIN + 1;
  /// OUTCHAIN = INTRINSIC_VOID(INCHAIN, INTRINSICID, arg1, arg2, ...)
  /// This node represents a target intrinsic function with side effects that
  /// does not return a result.  The first operand is a chain pointer.  The
  /// second is the ID number of the intrinsic from the llvm::Intrinsic
  /// namespace.  The operands to the intrinsic follow.
  public static final int INTRINSIC_VOID = INTRINSIC_W_CHAIN + 1;
  // CopyToReg - This node has three operands: a chain, a register number to
  // set to this value, and a value.
  public static final int CopyToReg = INTRINSIC_VOID + 1;
  // CopyFromReg - This node indicates that the input value is a virtual or
  // physical register that is defined outside of the scope of this
  // SelectionDAG.  The register is available from the RegisterSDNode object.
  public static final int CopyFromReg = CopyToReg + 1;
  // UNDEF - An undefined node
  public static final int UNDEF = CopyFromReg + 1;
  // EXTRACT_ELEMENT - This is used to get the lower or upper (determined by
  // a Constant, which is required to be operand #1) half of the integer or
  // float value specified as operand #0.  This is only for use before
  // legalization, for values that will be broken into multiple registers.
  public static final int EXTRACT_ELEMENT = UNDEF + 1;
  // BUILD_PAIR - This is the opposite of EXTRACT_ELEMENT in some ways.  Given
  // two values of the same integer value type, this produces a value twice as
  // big.  Like EXTRACT_ELEMENT, this can only be used before legalization.
  public static final int BUILD_PAIR = EXTRACT_ELEMENT + 1;
  // MERGE_VALUES - This node takes multiple discrete operands and returns
  // them all as its individual results.  This nodes has exactly the same
  // number of inputs and outputs. This node is useful for some pieces of the
  // code generator that want to think about a single node with multiple
  // results, not multiple nodes.
  public static final int MERGE_VALUES = BUILD_PAIR + 1;
  // Simple integer binary arithmetic operators.
  public static final int ADD = MERGE_VALUES + 1;
  public static final int SUB = ADD + 1;
  public static final int MUL = SUB + 1;
  public static final int SDIV = MUL + 1;
  public static final int UDIV = SDIV + 1;
  public static final int SREM = UDIV + 1;
  public static final int UREM = SREM + 1;
  // SMUL_LOHI/UMUL_LOHI - Multiply two integers of type iN, producing
  // a signed/unsigned value of type i[2*N], and return the full value as
  // two results, each of type iN.
  public static final int SMUL_LOHI = UREM + 1;
  public static final int UMUL_LOHI = SMUL_LOHI + 1;
  // SDIVREM/UDIVREM - Divide two integers and produce both a quotient and
  // remainder result.
  public static final int SDIVREM = UMUL_LOHI + 1;
  public static final int UDIVREM = SDIVREM + 1;
  // CARRY_FALSE - This node is used when folding other nodes,
  // like ADDC/SUBC, which indicate the carry result is always false.
  public static final int CARRY_FALSE = UDIVREM + 1;
  // Carry-setting nodes for multiple precision addition and subtraction.
  // These nodes take two operands of the same value type, and produce two
  // results.  The first result is the normal add or sub result, the second
  // result is the carry flag result.
  public static final int ADDC = CARRY_FALSE + 1;
  public static final int SUBC = ADDC + 1;
  // Carry-using nodes for multiple precision addition and subtraction.  These
  // nodes take three operands: The first two are the normal lhs and rhs to
  // the add or sub, and the third is the input carry flag.  These nodes
  // produce two results; the normal result of the add or sub, and the output
  // carry flag.  These nodes both read and write a carry flag to allow them
  // to them to be chained together for add and sub of arbitrarily large
  // values.
  public static final int ADDE = SUBC + 1;
  public static final int SUBE = ADDE + 1;
  // RESULT, BOOL = [SU]ADDO(LHS, RHS) - Overflow-aware nodes for addition.
  // These nodes take two operands: the normal LHS and RHS to the add. They
  // produce two results: the normal result of the add, and a boolean that
  // indicates if an overflow occured (*not* a flag, because it may be stored
  // to memory, etc.).  If the type of the boolean is not i1 then the high
  // bits conform to getBooleanContents.
  // These nodes are generated from the llvm.[su]add.with.overflow intrinsics.
  public static final int SADDO = SUBE + 1;
  public static final int UADDO = SADDO + 1;
  // Same for subtraction
  public static final int SSUBO = UADDO + 1;
  public static final int USUBO = SSUBO + 1;
  // Same for multiplication
  public static final int SMULO = USUBO + 1;
  public static final int UMULO = SMULO + 1;
  // Simple binary floating point operators.
  public static final int FADD = UMULO + 1;
  public static final int FSUB = FADD + 1;
  public static final int FMUL = FSUB + 1;
  public static final int FDIV = FMUL + 1;
  public static final int FREM = FDIV + 1;
  // FCOPYSIGN(X, Y) - Return the value of X with the sign of Y.  NOTE: This
  // DAG node does not require that X and Y have the same type, just that they
  // are both floating point.  X and the result must have the same type.
  // FCOPYSIGN(f32, f64) is allowed.
  public static final int FCOPYSIGN = FREM + 1;
  // INT = FGETSIGN(FP) - Return the sign bit of the specified floating point
  // value as an integer 0/1 value.
  public static final int FGETSIGN = FCOPYSIGN + 1;
  /// BUILD_VECTOR(ELT0, ELT1, ELT2, ELT3,...) - Return a vector with the
  /// specified, possibly variable, elements.  The number of elements is
  /// required to be a power of two.  The types of the operands must all be
  /// the same and must match the vector element type, except that integer
  /// types are allowed to be larger than the element type, in which case
  /// the operands are implicitly truncated.
  public static final int BUILD_VECTOR = FGETSIGN + 1;
  /// INSERT_VECTOR_ELT(VECTOR, VAL, IDX) - Returns VECTOR with the element
  /// at IDX replaced with VAL.  If the type of VAL is larger than the vector
  /// element type then VAL is truncated before replacement.
  public static final int INSERT_VECTOR_ELT = BUILD_VECTOR + 1;
  /// EXTRACT_VECTOR_ELT(VECTOR, IDX) - Returns a single element from VECTOR
  /// identified by the (potentially variable) element number IDX.  If the
  /// return type is an integer type larger than the element type of the
  /// vector, the result is extended to the width of the return type.
  public static final int EXTRACT_VECTOR_ELT = INSERT_VECTOR_ELT + 1;
  /// CONCAT_VECTORS(VECTOR0, VECTOR1, ...) - Given a number of values of
  /// vector type with the same length and element type, this produces a
  /// concatenated vector result value, with length equal to the sum of the
  /// lengths of the input vectors.
  public static final int CONCAT_VECTORS = EXTRACT_VECTOR_ELT + 1;
  /// EXTRACT_SUBVECTOR(VECTOR, IDX) - Returns a subvector from VECTOR (an
  /// vector value) starting with the (potentially variable) element number
  /// IDX, which must be a multiple of the result vector length.
  public static final int EXTRACT_SUBVECTOR = CONCAT_VECTORS + 1;
  /// VECTOR_SHUFFLE(VEC1, VEC2) - Returns a vector, of the same type as
  /// VEC1/VEC2.  A VECTOR_SHUFFLE node also contains an array of constant int
  /// values that indicate which value (or undef) each result element will
  /// get.  These constant ints are accessible through the
  /// ShuffleVectorSDNode class.  This is quite similar to the Altivec
  /// 'vperm' instruction, except that the indices must be constants and are
  /// in terms of the element size of VEC1/VEC2, not in terms of bytes.
  public static final int VECTOR_SHUFFLE = EXTRACT_SUBVECTOR + 1;
  /// SCALAR_TO_VECTOR(VAL) - This represents the operation of loading a
  /// scalar value into element 0 of the resultant vector type.  The top
  /// elements 1 to N-1 of the N-element vector are undefined.  The type
  /// of the operand must match the vector element type, except when they
  /// are integer types.  In this case the operand is allowed to be wider
  /// than the vector element type, and is implicitly truncated to it.
  public static final int SCALAR_TO_VECTOR = VECTOR_SHUFFLE + 1;
  // MULHU/MULHS - Multiply high - Multiply two integers of type iN, producing
  // an unsigned/signed value of type i[2*N], then return the top part.
  public static final int MULHU = SCALAR_TO_VECTOR + 1;
  public static final int MULHS = MULHU + 1;
  // Bitwise operators - logical and, logical or, logical xor, shift left,
  // shift right algebraic (shift in sign bits), shift right logical (shift in
  // zeroes), rotate left, rotate right, and byteswap.
  public static final int AND = MULHS + 1;
  public static final int OR = AND + 1;
  public static final int XOR = OR + 1;
  public static final int SHL = XOR + 1;
  public static final int SRA = SHL + 1;
  public static final int SRL = SRA + 1;
  public static final int ROTL = SRL + 1;
  public static final int ROTR = ROTL + 1;
  public static final int BSWAP = ROTR + 1;
  // Counting operators
  public static final int CTTZ = BSWAP + 1;
  public static final int CTLZ = CTTZ + 1;
  public static final int CTPOP = CTLZ + 1;
  // Select(COND, TRUEVAL, FALSEVAL).  If the type of the boolean COND is not
  // i1 then the high bits must conform to getBooleanContents.
  public static final int SELECT = CTPOP + 1;
  // Select with condition operator - This selects between a true value and
  // a false value (ops #2 and #3) based on the boolean result of comparing
  // the lhs and rhs (ops #0 and #1) of a conditional expression with the
  // condition code in op #4, a CondCodeSDNode.
  public static final int SELECT_CC = SELECT + 1;
  // SetCC operator - This evaluates to a true value iff the condition is
  // true.  If the result value type is not i1 then the high bits conform
  // to getBooleanContents.  The operands to this are the left and right
  // operands to compare (ops #0, and #1) and the condition code to compare
  // them with (op #2) as a CondCodeSDNode.
  public static final int SETCC = SELECT_CC + 1;
  // RESULT = VSETCC(LHS, RHS, COND) operator - This evaluates to a vector of
  // integer elements with all bits of the result elements set to true if the
  // comparison is true or all cleared if the comparison is false.  The
  // operands to this are the left and right operands to compare (LHS/RHS) and
  // the condition code to compare them with (COND) as a CondCodeSDNode.
  public static final int VSETCC = SETCC + 1;
  // SHL_PARTS/SRA_PARTS/SRL_PARTS - These operators are used for expanded
  // integer shift operations, just like ADD/SUB_PARTS.  The operation
  // ordering is:
  //       [Lo,Hi] = op [LoLHS,HiLHS], Amt
  public static final int SHL_PARTS = VSETCC + 1;
  public static final int SRA_PARTS = SHL_PARTS + 1;
  public static final int SRL_PARTS = SRA_PARTS + 1;
  // SIGN_EXTEND - Used for integer types, replicating the sign bit
  // into new bits.
  public static final int SIGN_EXTEND = SRL_PARTS + 1;
  // ZERO_EXTEND - Used for integer types, zeroing the new bits.
  public static final int ZERO_EXTEND = SIGN_EXTEND + 1;
  // ANY_EXTEND - Used for integer types.  The high bits are undefined.
  public static final int ANY_EXTEND = ZERO_EXTEND + 1;
  // TRUNCATE - Completely drop the high bits.
  public static final int TRUNCATE = ANY_EXTEND + 1;
  // [SU]INT_TO_FP - These operators convert integers (whose interpreted sign
  // depends on the first letter) to floating point.
  public static final int SINT_TO_FP = TRUNCATE + 1;
  public static final int UINT_TO_FP = SINT_TO_FP + 1;
  // SIGN_EXTEND_INREG - This operator atomically performs a SHL/SRA pair to
  // sign extend a small value in a large integer register (e.g. sign
  // extending the low 8 bits of a 32-bit register to fill the top 24 bits
  // with the 7th bit).  The size of the smaller type is indicated by the 1th
  // operand, a ValueType node.
  public static final int SIGN_EXTEND_INREG = UINT_TO_FP + 1;
  /// FP_TO_[US]INT - Convert a floating point value to a signed or unsigned
  /// integer.
  public static final int FP_TO_SINT = SIGN_EXTEND_INREG + 1;
  public static final int FP_TO_UINT = FP_TO_SINT + 1;
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
  public static final int FP_ROUND = FP_TO_UINT + 1;
  // FLT_ROUNDS_ - Returns current rounding mode:
  // -1 Undefined
  //  0 Round to 0
  //  1 Round to nearest
  //  2 Round to +inf
  //  3 Round to -inf
  public static final int FLT_ROUNDS_ = FP_ROUND + 1;
  /// X = FP_ROUND_INREG(Y, VT) - This operator takes an FP register, and
  /// rounds it to a floating point value.  It then promotes it and returns it
  /// in a register of the same size.  This operation effectively just
  /// discards excess precision.  The type to round down to is specified by
  /// the VT operand, a VTSDNode.
  public static final int FP_ROUND_INREG = FLT_ROUNDS_ + 1;
  /// X = FP_EXTEND(Y) - Extend a smaller FP type into a larger FP type.
  public static final int FP_EXTEND = FP_ROUND_INREG + 1;
  // BIT_CONVERT - Theis operator converts between integer and FP values, as
  // if one was stored to memory as integer and the other was loaded from the
  // same address (or equivalently for vector format conversions, etc).  The
  // source and result are required to have the same bit size (e.g.
  // f32 <-> i32).  This can also be used for int-to-int or fp-to-fp
  // conversions, but that is a noop, deleted by getNode().
  public static final int BIT_CONVERT = FP_EXTEND + 1;
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
  //   5) ISD.CvtCode indicating the type of conversion to do
  public static final int CONVERT_RNDSAT = BIT_CONVERT + 1;

  // FP16_TO_FP32, FP32_TO_FP16 - These operators are used to perform
  // promotions and truncation for half-precision (16 bit) floating
  // numbers. We need special nodes since FP16 is a storage-only type with
  // special semantics of operations.
  public static final int FP16_TO_FP32 = CONVERT_RNDSAT + 1;
  public static final int FP32_TO_FP16 = FP16_TO_FP32 + 1;

  // FNEG, FABS, FSQRT, FSIN, FCOS, FPOWI, FPOW,
  // FLOG, FLOG2, FLOG10, FEXP, FEXP2,
  // FCEIL, FTRUNC, FRINT, FNEARBYINT, FFLOOR - Perform various unary floating
  // point operations. These are inspired by libm.
  public static final int FNEG = FP32_TO_FP16 + 1;
  public static final int FABS = FNEG + 1;
  public static final int FSQRT = FABS + 1;
  public static final int FSIN = FSQRT + 1;
  public static final int FCOS = FSIN + 1;
  public static final int FPOWI = FCOS + 1;
  public static final int FPOW = FPOWI + 1;
  public static final int FLOG = FPOW + 1;
  public static final int FLOG2 = FLOG + 1;
  public static final int FLOG10 = FLOG2 + 1;
  public static final int FEXP = FLOG10 + 1;
  public static final int FEXP2 = FEXP + 1;
  public static final int FCEIL = FEXP2 + 1;
  public static final int FTRUNC = FCEIL + 1;
  public static final int FRINT = FTRUNC + 1;
  public static final int FMA = FRINT + 1;
  public static final int FNEARBYINT = FMA + 1;
  public static final int FFLOOR = FNEARBYINT + 1;
  // LOAD and STORE have token chains as their first operand, then the same
  // operands as an LLVM load/store instruction, then an offset node that
  // is added / subtracted from the base pointer to form the address (for
  // indexed memory ops).
  public static final int LOAD = FFLOOR + 1;
  public static final int STORE = LOAD + 1;
  // DYNAMIC_STACKALLOC - Allocate some number of bytes on the stack aligned
  // to a specified boundary.  This node always has two return values: a new
  // stack pointer value and a chain. The first operand is the token chain,
  // the second is the number of bytes to allocate, and the third is the
  // alignment boundary.  The size is guaranteed to be a multiple of the stack
  // alignment, and the alignment is guaranteed to be bigger than the stack
  // alignment (if required) or 0 to get standard stack alignment.
  public static final int DYNAMIC_STACKALLOC = STORE + 1;
  // BR - Unconditional branch.  The first operand is the chain
  // operand, the second is the MBB to branch to.
  public static final int BR = DYNAMIC_STACKALLOC + 1;
  // BRIND - Indirect branch.  The first operand is the chain, the second
  // is the value to branch to, which must be of the same type as the target's
  // pointer type.
  public static final int BRIND = BR + 1;
  // BR_JT - Jumptable branch. The first operand is the chain, the second
  // is the jumptable index, the last one is the jumptable entry index.
  public static final int BR_JT = BRIND + 1;
  // BRCOND - Conditional branch.  The first operand is the chain, the
  // second is the condition, the third is the block to branch to if the
  // condition is true.  If the type of the condition is not i1, then the
  // high bits must conform to getBooleanContents.
  public static final int BRCOND = BR_JT + 1;
  // BR_CC - Conditional branch.  The behavior is like that of SELECT_CC, in
  // that the condition is represented as condition code, and two nodes to
  // compare, rather than as a combined SetCC node.  The operands in order are
  // chain, cc, lhs, rhs, block to branch to if condition is true.
  public static final int BR_CC = BRCOND + 1;
  // INLINEASM - Represents an inline asm block.  This node always has two
  // return values: a chain and a flag result.  The inputs are as follows:
  //   Operand #0   : Input chain.
  //   Operand #1   : a ExternalSymbolSDNode with a pointer to the asm string.
  //   Operand #2n+2: A RegisterNode.
  //   Operand #2n+3: A TargetConstant, indicating if the reg is a use/def
  //   Operand #last: Optional, an incoming flag.
  public static final int INLINEASM = BR_CC + 1;
  // DBG_LABEL, EH_LABEL - Represents a label in mid basic block used to track
  // locations needed for debug and exception handling tables.  These nodes
  // take a chain as input and return a chain.
  public static final int DBG_LABEL = INLINEASM + 1;
  public static final int EH_LABEL = DBG_LABEL + 1;
  // DECLARE - Represents a llvm.dbg.declare intrinsic. It's used to track
  // local variable declarations for debugging information. First operand is
  // a chain, while the next two operands are first two arguments (address
  // and variable) of a llvm.dbg.declare instruction.
  public static final int DECLARE = EH_LABEL + 1;
  // STACKSAVE - STACKSAVE has one operand, an input chain.  It produces a
  // value, the same type as the pointer type for the system, and an output
  // chain.
  public static final int STACKSAVE = DECLARE + 1;
  // STACKRESTORE has two operands, an input chain and a pointer to restore to
  // it returns an output chain.
  public static final int STACKRESTORE = STACKSAVE + 1;
  // CALLSEQ_START/CALLSEQ_END - These operators mark the beginning and end of
  // a call sequence, and carry arbitrary information that target might want
  // to know.  The first operand is a chain, the rest are specified by the
  // target and not touched by the DAG optimizers.
  // CALLSEQ_START..CALLSEQ_END pairs may not be nested.
  public static final int CALLSEQ_START = STACKRESTORE + 1;  // Beginning of a call sequence
  public static final int CALLSEQ_END = CALLSEQ_START + 1;    // End of a call sequence
  // VAARG - VAARG has three operands: an input chain, a pointer, and a
  // SRCVALUE.  It returns a pair of values: the vaarg value and a new chain.
  public static final int VAARG = CALLSEQ_END + 1;
  // VACOPY - VACOPY has five operands: an input chain, a destination pointer,
  // a source pointer, a SRCVALUE for the destination, and a SRCVALUE for the
  // source.
  public static final int VACOPY = VAARG + 1;
  // VAEND, VASTART - VAEND and VASTART have three operands: an input chain, a
  // pointer, and a SRCVALUE.
  public static final int VAEND = VACOPY + 1;
  public static final int VASTART = VAEND + 1;
  // SRCVALUE - This is a node type that holds a Value* that is used to
  // make reference to a value in the LLVM IR.
  public static final int SRCVALUE = VASTART + 1;
  // MEMOPERAND - This is a node that contains a MachineMemOperand which
  // records information about a memory reference. This is used to make
  // AliasAnalysis queries from the backend.
  public static final int MEMOPERAND = SRCVALUE + 1;
  // PCMARKER - This corresponds to the pcmarker intrinsic.
  public static final int PCMARKER = MEMOPERAND + 1;
  // READCYCLECOUNTER - This corresponds to the readcyclecounter intrinsic.
  // The only operand is a chain and a value and a chain are produced.  The
  // value is the contents of the architecture specific cycle counter like
  // register (or other high accuracy low latency clock source)
  public static final int READCYCLECOUNTER = PCMARKER + 1;
  // HANDLENODE node - Used as a handle for various purposes.
  public static final int HANDLENODE = READCYCLECOUNTER + 1;
  // DBG_STOPPOINT - This node is used to represent a source location for
  // debug info.  It takes token chain as input, and carries a line number,
  // column number, and a pointer to a CompileUnit object identifying
  // the containing compilation unit.  It produces a token chain as output.
  public static final int DBG_STOPPOINT = HANDLENODE + 1;
  // DEBUG_LOC - This node is used to represent source line information
  // embedded in the code.  It takes a token chain as input, then a line
  // number, then a column then a file id (provided by MachineModuleInfo.) It
  // produces a token chain as output.
  public static final int DEBUG_LOC = DBG_STOPPOINT + 1;
  // TRAMPOLINE - This corresponds to the init_trampoline intrinsic.
  // It takes as input a token chain, the pointer to the trampoline,
  // the pointer to the nested function, the pointer to pass for the
  // 'nest' parameter, a SRCVALUE for the trampoline and another for
  // the nested function (allowing targets to access the original
  // Function*).  It produces the result of the intrinsic and a token
  // chain as output.
  public static final int TRAMPOLINE = DEBUG_LOC + 1;
  // TRAP - Trapping instruction
  public static final int TRAP = TRAMPOLINE + 1;
  // PREFETCH - This corresponds to a prefetch intrinsic. It takes chains are
  // their first operand. The other operands are the address to prefetch,
  // read / write specifier = 0; and locality specifier.
  public static final int PREFETCH = TRAP + 1;
  // OUTCHAIN = MEMBARRIER(INCHAIN, load-load, load-store, store-load,
  //                       store-store, device)
  // This corresponds to the memory.barrier intrinsic.
  // it takes an input chain, 4 operands to specify the type of barrier, an
  // operand specifying if the barrier applies to device and uncached memory
  // and produces an output chain.
  public static final int MEMBARRIER = PREFETCH + 1;
  // Val, OUTCHAIN = ATOMIC_CMP_SWAP(INCHAIN, ptr, cmp, swap)
  // this corresponds to the atomic.lcs intrinsic.
  // cmp is compared to *ptr, and if equal, swap is stored in *ptr.
  // the return is always the original value in *ptr
  /// OUTCHAIN = ATOMIC_FENCE(INCHAIN, ordering, scope)
  /// This corresponds to the fence instruction. It takes an input chain, and
  /// two integer constants: an AtomicOrdering and a SynchronizationScope.
  public static final int ATOMIC_FENCE = MEMBARRIER + 1;

  /// Val, OUTCHAIN = ATOMIC_LOAD(INCHAIN, ptr)
  /// This corresponds to "load atomic" instruction.
  public static final int ATOMIC_LOAD = ATOMIC_FENCE + 1;

  /// OUTCHAIN = ATOMIC_STORE(INCHAIN, ptr, val)
  /// This corresponds to "store atomic" instruction.
  public static final int ATOMIC_STORE = ATOMIC_LOAD + 1;

  public static final int ATOMIC_CMP_SWAP = ATOMIC_STORE + 1;
  // Val, OUTCHAIN = ATOMIC_SWAP(INCHAIN, ptr, amt)
  // this corresponds to the atomic.swap intrinsic.
  // amt is stored to *ptr atomically.
  // the return is always the original value in *ptr
  public static final int ATOMIC_SWAP = ATOMIC_CMP_SWAP + 1;
  // Val, OUTCHAIN = ATOMIC_LOAD_[OpName](INCHAIN, ptr, amt)
  // this corresponds to the atomic.load.[OpName] intrinsic.
  // op(*ptr, amt) is stored to *ptr atomically.
  // the return is always the original value in *ptr
  public static final int ATOMIC_LOAD_ADD = ATOMIC_SWAP + 1;
  public static final int ATOMIC_LOAD_SUB = ATOMIC_LOAD_ADD + 1;
  public static final int ATOMIC_LOAD_AND = ATOMIC_LOAD_SUB + 1;
  public static final int ATOMIC_LOAD_OR = ATOMIC_LOAD_AND + 1;
  public static final int ATOMIC_LOAD_XOR = ATOMIC_LOAD_OR + 1;
  public static final int ATOMIC_LOAD_NAND = ATOMIC_LOAD_XOR + 1;
  public static final int ATOMIC_LOAD_MIN = ATOMIC_LOAD_NAND + 1;
  public static final int ATOMIC_LOAD_MAX = ATOMIC_LOAD_MIN + 1;
  public static final int ATOMIC_LOAD_UMIN = ATOMIC_LOAD_MAX + 1;
  public static final int ATOMIC_LOAD_UMAX = ATOMIC_LOAD_UMIN + 1;

  // BUILTIN_OP_END - This must be the last enum value in this list.
  public static final int BUILTIN_OP_END = ATOMIC_LOAD_UMAX + 1;

  public static final int FIRST_TARGET_MEMORY_OPCODE = BUILTIN_OP_END+150;

  public static CondCode getSetCCInverse(CondCode cc, boolean isInteger) {
    int operator = cc.ordinal();
    if (isInteger)
      operator ^= 7;
    else
      operator ^= 15;
    if (operator > CondCode.SETTRUE2.ordinal())
      operator &= ~8;
    return CondCode.values()[operator];
  }

  public static CondCode getSetCCSwappedOperands(CondCode cc) {
    int oldL = (cc.ordinal() >> 2) & 1;
    int oldG = (cc.ordinal() >> 1) & 1;
    return CondCode.values()[(cc.ordinal() & ~6) | (oldL << 1) | (oldG << 2)];
  }

  public static CondCode getSetCCAndOperation(CondCode cc1, CondCode cc2,
                                              boolean isInteger) {
    if (isInteger & (isSignedOp(cc1) | isSignedOp(cc2)) == 3)
      return CondCode.SETCC_INVALID;

    CondCode result = CondCode.values()[cc1.ordinal() & cc2.ordinal()];
    if (isInteger) {
      switch (result) {
        default:
          break;
        case SETUO:
          result = CondCode.SETFALSE;
          break;
        case SETOEQ:
        case SETUEQ:
          result = CondCode.SETEQ;
          break;
        case SETOLT:
          result = CondCode.SETULT;
          break;
        case SETOGT:
          result = CondCode.SETUGT;
          break;
      }
    }
    return result;
  }

  private static int isSignedOp(CondCode opc) {
    switch (opc) {
      default:
        Util.shouldNotReachHere("Illegal integer setcc operation!");
        return 0;
      case SETEQ:
      case SETNE:
        return 0;
      case SETLT:
      case SETLE:
      case SETGT:
      case SETGE:
        return 1;
      case SETULT:
      case SETULE:
      case SETUGT:
      case SETUGE:
        return 2;
    }
  }

  /**
   * Return the result of a logical OR between different
   * comparisons of identical values: ((X op1 Y) | (X op2 Y)).  This function
   * returns SETCC_INVALID if it is not possible to represent the resultant
   * comparison.
   *
   * @param cc1
   * @param cc2
   * @param isInteger
   * @return
   */
  public static CondCode getSetCCOrOperation(CondCode cc1, CondCode cc2, boolean isInteger) {
    if (isInteger && (isSignedOp(cc1) | isSignedOp(cc2)) == 3)
      return CondCode.SETCC_INVALID;

    int op = cc1.ordinal() | cc2.ordinal();
    if (op > CondCode.SETTRUE2.ordinal())
      op &= ~16;

    if (isInteger && op == CondCode.SETUNE.ordinal())
      op = CondCode.SETNE.ordinal();

    return CondCode.values()[op];
  }

  public static boolean isBuildVectorAllOnes(SDNode n) {
    if (n.getOpcode() == BIT_CONVERT)
      n = n.getOperand(0).getNode();

    if (n.getOpcode() != BUILD_VECTOR)
      return false;

    int i = 0, e = n.getNumOperands();
    while (i < e && n.getOperand(i).getOpcode() == UNDEF)
      ++i;

    if (i == e)
      return false;

    SDValue notZero = n.getOperand(i);
    if (notZero.getNode() instanceof SDNode.ConstantSDNode) {
      if (!((SDNode.ConstantSDNode) notZero.getNode()).isAllOnesValue())
        return false;
    } else if (notZero.getNode() instanceof SDNode.ConstantFPSDNode) {
      if (((SDNode.ConstantFPSDNode) notZero.getNode()).getValueAPF().bitcastToAPInt().isAllOnesValue())
        return false;
    } else
      return false;

    for (++i; i < e; i++) {
      if (!n.getOperand(i).equals(notZero)
          && n.getOperand(i).getOpcode() != UNDEF)
        return false;
    }
    return true;
  }

  public static boolean isBuildVectorAllZeros(SDNode n) {
    if (n.getOpcode() == BIT_CONVERT)
      n = n.getOperand(0).getNode();

    if (n.getOpcode() != BUILD_VECTOR)
      return false;

    int i = 0, e = n.getNumOperands();
    while (i < e && n.getOperand(i).getOpcode() == UNDEF)
      ++i;

    if (i == e)
      return false;

    SDValue notZero = n.getOperand(i);
    if (notZero.getNode() instanceof SDNode.ConstantSDNode) {
      if (!((SDNode.ConstantSDNode) notZero.getNode()).isNullValue())
        return false;
    } else if (notZero.getNode() instanceof SDNode.ConstantFPSDNode) {
      if (((SDNode.ConstantFPSDNode) notZero.getNode()).getValueAPF().isPosZero())
        return false;
    } else
      return false;

    for (++i; i < e; i++) {
      if (!n.getOperand(i).equals(notZero)
          && n.getOperand(i).getOpcode() != UNDEF)
        return false;
    }
    return true;
  }
}
