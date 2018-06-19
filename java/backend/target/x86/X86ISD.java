package backend.target.x86;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
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

import backend.codegen.dagisel.ISD;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86ISD
{
    //================== X86 Specific DAG Nodes ============================//
    //======================================================================//

    // Start the numbering where the builtin ops leave off.
    public static final int FIRST_NUMBER = ISD.BUILTIN_OP_END;
    /// BSF - Bit scan forward.
    /// BSR - Bit scan reverse.
    public static final int  BSF = FIRST_NUMBER + 1;
    public static final int BSR = BSF + 1;
    /// SHLD, SHRD - Double shift instructions. These correspond to
    /// X86::SHLDxx and X86::SHRDxx instructions.
    public static final int SHLD = BSR + 1;
    public static final int SHRD = SHLD + 1;
    /// FAND - Bitwise logical AND of floating point values. This corresponds
    /// to X86::ANDPS or X86::ANDPD.
    public static final int FAND = SHRD + 1;
    /// FOR - Bitwise logical OR of floating point values. This corresponds
    /// to X86::ORPS or X86::ORPD.
    public static final int FOR = FAND + 1;
    /// FXOR - Bitwise logical XOR of floating point values. This corresponds
    /// to X86::XORPS or X86::XORPD.
    public static final int FXOR = FOR + 1;
    /// FSRL - Bitwise logical right shift of floating point values. These
    /// corresponds to X86::PSRLDQ.
    public static final int FSRL = FXOR + 1;
    /// FILD, FILD_FLAG - This instruction implements SINT_TO_FP with the
    /// integer source in memory and FP reg result.  This corresponds to the
    /// X86::FILD*m instructions. It has three inputs (token chain, address,
    /// and source type) and two outputs (FP value and token chain). FILD_FLAG
    /// also produces a flag).
    public static final int FILD = FSRL + 1;
    public static final int FILD_FLAG = FILD + 1;
    /// FP_TO_INT*_IN_MEM - This instruction implements FP_TO_SINT with the
    /// integer destination in memory and a FP reg source.  This corresponds
    /// to the X86::FIST*m instructions and the rounding mode change stuff. It
    /// has two inputs (token chain and address) and two outputs (int value
    /// and token chain).
    public static final int FP_TO_INT16_IN_MEM = FILD_FLAG + 1;
    public static final int FP_TO_INT32_IN_MEM = FP_TO_INT16_IN_MEM + 1;
    public static final int FP_TO_INT64_IN_MEM = FP_TO_INT32_IN_MEM + 1;
    /// FLD - This instruction implements an extending load to FP stack slots.
    /// This corresponds to the X86::FLD32m / X86::FLD64m. It takes a chain
    /// operand, ptr to load from, and a ValueType node indicating the type
    /// to load to.
    public static final int FLD = FP_TO_INT64_IN_MEM + 1;
    /// FST - This instruction implements a truncating store to FP stack
    /// slots. This corresponds to the X86::FST32m / X86::FST64m. It takes a
    /// chain operand, value to store, address, and a ValueType to store it
    /// as.
    public static final int FST = FLD + 1;
    /// CALL - These operations represent an abstract X86 call
    /// instruction, which includes a bunch of information.  In particular the
    /// operands of these node are:
    ///
    ///     #0 - The incoming token chain
    ///     #1 - The callee
    ///     #2 - The number of arg bytes the caller pushes on the stack.
    ///     #3 - The number of arg bytes the callee pops off the stack.
    ///     #4 - The value to pass in AL/AX/EAX (optional)
    ///     #5 - The value to pass in DL/DX/EDX (optional)
    ///
    /// The result values of these nodes are:
    ///
    ///     #0 - The outgoing token chain
    ///     #1 - The first register result value (optional)
    ///     #2 - The second register result value (optional)
    ///
    public static final int CALL = FST + 1;
    /// RDTSC_DAG - This operation implements the lowering for
    /// readcyclecounter
    public static final int RDTSC_DAG = CALL + 1;
    /// X86 compare and logical compare instructions.
    public static final int CMP = RDTSC_DAG + 1;
    public static final int COMI = CMP + 1;
    public static final int UCOMI = COMI + 1;
    /// X86 bit-test instructions.
    public static final int BT = UCOMI + 1;
    /// X86 SetCC. Operand 0 is condition code, and operand 1 is the flag
    /// operand produced by a CMP instruction.
    public static final int SETCC = BT + 1;
    /// X86 conditional moves. Operand 0 and operand 1 are the two values
    /// to select from. Operand 2 is the condition code, and operand 3 is the
    /// flag operand produced by a CMP or TEST instruction. It also writes a
    /// flag result.
    public static final int CMOV = SETCC + 1;
    /// X86 conditional branches. Operand 0 is the chain operand, operand 1
    /// is the block to branch if condition is true, operand 2 is the
    /// condition code, and operand 3 is the flag operand produced by a CMP
    /// or TEST instruction.
    public static final int BRCOND = CMOV + 1;
    /// Return with a flag operand. Operand 0 is the chain operand, operand
    /// 1 is the number of bytes of stack to pop.
    public static final int RET_FLAG = BRCOND + 1;
    /// REP_STOS - Repeat fill, corresponds to X86::REP_STOSx.
    public static final int REP_STOS = RET_FLAG + 1;
    /// REP_MOVS - Repeat move, corresponds to X86::REP_MOVSx.
    public static final int REP_MOVS = REP_STOS + 1;
    /// globalBaseReg - On Darwin, this node represents the result of the popl
    /// at function entry, used for PIC code.
    public static final int GlobalBaseReg = REP_MOVS + 1;
    /// Wrapper - A wrapper node for TargetConstantPool,
    /// TargetExternalSymbol, and TargetGlobalAddress.
    public static final int Wrapper = GlobalBaseReg + 1;
    /// WrapperRIP - Special wrapper used under X86-64 PIC mode for RIP
    /// relative displacements.
    public static final int WrapperRIP = Wrapper + 1;
    /// PEXTRB - Extract an 8-bit value from a vector and zero extend it to
    /// i32, corresponds to X86::PEXTRB.
    public static final int PEXTRB = WrapperRIP + 1;
    /// PEXTRW - Extract a 16-bit value from a vector and zero extend it to
    /// i32, corresponds to X86::PEXTRW.
    public static final int PEXTRW = PEXTRB + 1;
    /// INSERTPS - Insert any element of a 4 x float vector into any element
    /// of a destination 4 x floatvector.
    public static final int INSERTPS = PEXTRW + 1;
    /// PINSRB - Insert the lower 8-bits of a 32-bit value to a vector,
    /// corresponds to X86::PINSRB.
    public static final int PINSRB = INSERTPS + 1;
    /// PINSRW - Insert the lower 16-bits of a 32-bit value to a vector,
    /// corresponds to X86::PINSRW.
    public static final int PINSRW = PINSRB + 1;
    /// PSHUFB - Shuffle 16 8-bit values within a vector.
    public static final int PSHUFB = PINSRW + 1;
    /// FMAX; FMIN - Floating point max and min.
    ///
    public static final int FMAX = PSHUFB + 1;
    public static final int FMIN = FMAX + 1;
    /// FRSQRT, FRCP - Floating point reciprocal-sqrt and reciprocal
    /// approximation.  Note that these typically require refinement
    /// in order to obtain suitable precision.
    public static final int FRSQRT = FMIN + 1;
    public static final int FRCP = FRSQRT + 1;
    // TLSADDR - Thread Local Storage.
    public static final int TLSADDR = FRCP + 1;
    // SegmentBaseAddress - The address segment:0
    public static final int SegmentBaseAddress = TLSADDR + 1;
    // EH_RETURN - Exception Handling helpers.
    public static final int EH_RETURN = SegmentBaseAddress + 1;
    /// TC_RETURN - Tail call return.
    ///   operand #0 chain
    ///   operand #1 callee (register or absolute)
    ///   operand #2 stack adjustment
    ///   operand #3 optional in flag
    public static final int TC_RETURN = EH_RETURN + 1;
    // LCMPXCHG_DAG, LCMPXCHG8_DAG - Compare and swap.
    public static final int LCMPXCHG_DAG = TC_RETURN + 1;
    public static final int LCMPXCHG8_DAG = LCMPXCHG_DAG + 1;
    // ATOMADD64_DAG, ATOMSUB64_DAG, ATOMOR64_DAG, ATOMAND64_DAG,
    // ATOMXOR64_DAG, ATOMNAND64_DAG, ATOMSWAP64_DAG -
    // Atomic 64-bit binary operations.
    public static final int ATOMADD64_DAG = LCMPXCHG8_DAG + 1;
    public static final int ATOMSUB64_DAG = ATOMADD64_DAG + 1;
    public static final int ATOMOR64_DAG = ATOMSUB64_DAG + 1;
    public static final int ATOMXOR64_DAG = ATOMOR64_DAG + 1;
    public static final int ATOMAND64_DAG = ATOMXOR64_DAG + 1;
    public static final int ATOMNAND64_DAG = ATOMAND64_DAG + 1;
    public static final int ATOMSWAP64_DAG = ATOMNAND64_DAG + 1;
    // FNSTCW16m - Store FP control world into i16 memory.
    public static final int FNSTCW16m = ATOMSWAP64_DAG + 1;
    // VZEXT_MOVL - Vector move low and zero extend.
    public static final int VZEXT_MOVL = FNSTCW16m + 1;
    // VZEXT_LOAD - Load, scalar_to_vector, and zero extend.
    public static final int VZEXT_LOAD = VZEXT_MOVL + 1;
    // VSHL, VSRL - Vector logical left / right shift.
    public static final int VSHL = VZEXT_LOAD + 1;
    public static final int VSRL = VSHL + 1;
    // CMPPD, CMPPS - Vector double/float comparison.
    // CMPPD, CMPPS - Vector double/float comparison.
    public static final int CMPPD = VSRL + 1;
    public static final int CMPPS = CMPPD + 1;
    // PCMP* - Vector integer comparisons.
    public static final int PCMPEQB = CMPPS + 1;
    public static final int PCMPEQW = PCMPEQB + 1;
    public static final int PCMPEQD = PCMPEQW + 1;
    public static final int PCMPEQQ = PCMPEQD + 1;
    public static final int PCMPGTB = PCMPEQQ + 1;
    public static final int PCMPGTW = PCMPGTB + 1;
    public static final int PCMPGTD = PCMPGTW + 1;
    public static final int PCMPGTQ = PCMPGTD + 1;
    // ADD, SUB, SMUL, UMUL, etc. - Arithmetic operations with FLAGS results.
    public static final int ADD = PCMPGTQ + 1;
    public static final int SUB = ADD + 1;
    public static final int SMUL = SUB + 1;
    public static final int UMUL = SMUL + 1;
    public static final int INC = UMUL + 1;
    public static final int DEC = INC + 1;
    // MUL_IMM - X86 specific multiply by immediate.
    public static final int MUL_IMM = DEC + 1;
    // PTEST - Vector bitwise comparisons
    public static final int PTEST = MUL_IMM + 1;
    // VASTART_SAVE_XMM_REGS - Save xmm argument registers to the stack,
    // according to %al. An operator is needed so that this can be expanded
    // with control flow.
    public static final int VASTART_SAVE_XMM_REGS = PTEST + 1;
}
