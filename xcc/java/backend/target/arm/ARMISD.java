package backend.target.arm;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import backend.codegen.dagisel.ISD;

/**
 * The ARM specific Selection DAG node types.
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ARMISD {
	// Start the numbering where the builtin ops leave off.
	public static final int FIRST_NUMBER = ISD.BUILTIN_OP_END;
	public static final int Wrapper = FIRST_NUMBER + 1;      // Wrapper - A wrapper node for TargetConstantPool, TargetExternalSymbol, and TargetGlobalAddress.
	public static final int WrapperDYN = Wrapper + 1;   // WrapperDYN - A wrapper node for TargetGlobalAddress in DYN mode.
	public static final int WrapperPIC = WrapperDYN + 1;   // WrapperPIC - A wrapper node for TargetGlobalAddress in PIC mode.
	public static final int WrapperJT = WrapperPIC + 1;    // WrapperJT - A wrapper node for TargetJumpTable
	public static final int CALL = WrapperJT + 1;         // Function call.
	public static final int CALL_PRED = CALL + 1;    // Function call that's predicable.
	public static final int CALL_NOLINK = CALL_PRED + 1;  // Function call with branch not branch-and-link.
	public static final int tCALL = CALL_NOLINK + 1;        // Thumb function call.
	public static final int BRCOND = tCALL + 1;       // Conditional branch.
	public static final int BR_JT = BRCOND + 1;        // Jumptable branch.
	public static final int BR2_JT = BR_JT + 1;       // Jumptable branch (2 level - jumptable entry is a jump).
	public static final int RET_FLAG = BR2_JT + 1;     // Return with a flag operand.

	public static final int PIC_ADD = RET_FLAG + 1;      // Add with a PC operand and a PIC label.

	public static final int CMP = PIC_ADD + 1;          // ARM compare instructions.
	public static final int CMPZ = CMP + 1;         // ARM compare that sets only Z flag.
	public static final int CMPFP = CMPZ + 1;        // ARM VFP compare instruction, sets FPSCR.
	public static final int CMPFPw0 = CMPFP + 1;      // ARM VFP compare against zero instruction, sets FPSCR.
	public static final int FMSTAT = CMPFPw0 + 1;       // ARM fmstat instruction.
	public static final int CMOV = FMSTAT + 1;         // ARM conditional move instructions.

	public static final int BCC_i64 = CMOV + 1;

	public static final int RBIT = BCC_i64 + 1;         // ARM bitreverse instruction

	public static final int FTOSI = RBIT + 1;        // FP to sint within a FP register.
	public static final int FTOUI = FTOSI + 1;        // FP to uint within a FP register.
	public static final int SITOF = FTOUI + 1;        // sint to FP within a FP register.
	public static final int UITOF = SITOF + 1;        // uint to FP within a FP register.

	public static final int SRL_FLAG = UITOF + 1;     // V,Flag = srl_flag X -> srl X, 1 + save carry out.
	public static final int SRA_FLAG = SRL_FLAG + 1;     // V,Flag = sra_flag X -> sra X, 1 + save carry out.
	public static final int RRX = SRA_FLAG + 1;          // V = RRX X, Flag     -> srl X, 1 + shift in carry flag.

	public static final int ADDC = RRX + 1;         // Add with carry
	public static final int ADDE = ADDC + 1;         // Add using carry
	public static final int SUBC = ADDE + 1;         // Sub with carry
	public static final int SUBE = SUBC + 1;         // Sub using carry

	public static final int VMOVRRD = SUBE + 1;      // double to two gprs.
	public static final int VMOVDRR = VMOVRRD + 1;      // Two gprs to double.

	public static final int EH_SJLJ_SETJMP = VMOVDRR + 1;         // SjLj exception handling setjmp.
	public static final int EH_SJLJ_LONGJMP = EH_SJLJ_SETJMP + 1;        // SjLj exception handling longjmp.
	public static final int EH_SJLJ_DISPATCHSETUP = EH_SJLJ_LONGJMP + 1;  // SjLj exception handling dispatch setup.

	public static final int TC_RETURN = EH_SJLJ_DISPATCHSETUP + 1;    // Tail call return pseudo.

	public static final int THREAD_POINTER = TC_RETURN + 1;

	public static final int DYN_ALLOC = THREAD_POINTER + 1;    // Dynamic allocation on the stack.

	public static final int MEMBARRIER = DYN_ALLOC + 1;   // Memory barrier (DMB)
	public static final int MEMBARRIER_MCR = MEMBARRIER + 1; // Memory barrier (MCR)

	public static final int PRELOAD = MEMBARRIER_MCR + 1;      // Preload

	public static final int VCEQ = PRELOAD + 1;         // Vector compare equal.
	public static final int VCEQZ = VCEQ + 1;        // Vector compare equal to zero.
	public static final int VCGE = VCEQZ + 1;         // Vector compare greater than or equal.
	public static final int VCGEZ = VCGE + 1;        // Vector compare greater than or equal to zero.
	public static final int VCLEZ = VCGEZ + 1;        // Vector compare less than or equal to zero.
	public static final int VCGEU = VCLEZ + 1;        // Vector compare unsigned greater than or equal.
	public static final int VCGT = VCGEU + 1;         // Vector compare greater than.
	public static final int VCGTZ = VCGT + 1;        // Vector compare greater than zero.
	public static final int VCLTZ = VCGTZ + 1;        // Vector compare less than zero.
	public static final int VCGTU = VCLTZ + 1;        // Vector compare unsigned greater than.
	public static final int VTST = VCGTU + 1;         // Vector test bits.

	// Vector shift by immediate:
	public static final int VSHL = VTST + 1;         // ...left
	public static final int VSHRs = VSHL + 1;        // ...right (signed)
	public static final int VSHRu = VSHRs + 1;        // ...right (unsigned)
	public static final int VSHLLs = VSHRu + 1;       // ...left long (signed)
	public static final int VSHLLu = VSHLLs + 1;       // ...left long (unsigned)
	public static final int VSHLLi = VSHLLu + 1;       // ...left long (with maximum shift count)
	public static final int VSHRN = VSHLLi + 1;        // ...right narrow

	// Vector rounding shift by immediate:
	public static final int VRSHRs = VSHRN + 1;       // ...right (signed)
	public static final int VRSHRu = VRSHRs + 1;       // ...right (unsigned)
	public static final int VRSHRN = VRSHRu + 1;       // ...right narrow

	// Vector saturating shift by immediate:
	public static final int VQSHLs = VRSHRN + 1;       // ...left (signed)
	public static final int VQSHLu = VQSHLs + 1;       // ...left (unsigned)
	public static final int VQSHLsu = VQSHLu + 1;      // ...left (signed to unsigned)
	public static final int VQSHRNs = VQSHLsu + 1;      // ...right narrow (signed)
	public static final int VQSHRNu = VQSHRNs + 1;      // ...right narrow (unsigned)
	public static final int VQSHRNsu = VQSHRNu + 1;     // ...right narrow (signed to unsigned)

	// Vector saturating rounding shift by immediate:
	public static final int VQRSHRNs = VQSHRNsu + 1;     // ...right narrow (signed)
	public static final int VQRSHRNu = VQRSHRNs + 1;     // ...right narrow (unsigned)
	public static final int VQRSHRNsu = VQRSHRNu + 1;    // ...right narrow (signed to unsigned)

	// Vector shift and insert:
	public static final int VSLI = VQRSHRNsu + 1;         // ...left
	public static final int VSRI = VSLI + 1;         // ...right

	// Vector get lane (VMOV scalar to ARM core register)
	// (These are used for 8- and 16-bit element types only.)
	public static final int VGETLANEu = VSRI + 1;    // zero-extend vector extract element
	public static final int VGETLANEs = VGETLANEu + 1;    // sign-extend vector extract element

	// Vector move immediate and move negated immediate:
	public static final int VMOVIMM = VGETLANEs + 1;
	public static final int VMVNIMM = VMOVIMM + 1;

	// Vector duplicate:
	public static final int VDUP = VMVNIMM + 1;
	public static final int VDUPLANE = VDUP + 1;

	// Vector shuffles:
	public static final int VEXT = VDUPLANE + 1;         // extract
	public static final int VREV64 = VEXT + 1;       // reverse elements within 64-bit doublewords
	public static final int VREV32 = VREV64 + 1;       // reverse elements within 32-bit words
	public static final int VREV16 = VREV32 + 1;       // reverse elements within 16-bit halfwords
	public static final int VZIP = VREV16 + 1;         // zip (interleave)
	public static final int VUZP = VZIP + 1;         // unzip (deinterleave)
	public static final int VTRN = VUZP + 1;         // transpose
	public static final int VTBL1 = VTRN + 1;        // 1-register shuffle with mask
	public static final int VTBL2 = VTBL1 + 1;        // 2-register shuffle with mask

	// Vector multiply long:
	public static final int VMULLs = VTBL2 + 1;       // ...signed
	public static final int VMULLu = VMULLs + 1;       // ...unsigned

	// Operands of the standard BUILD_VECTOR node are not legalized, which
	// is fine if BUILD_VECTORs are always lowered to shuffles or other
	// operations, but for ARM some BUILD_VECTORs are legal as-is and their
	// operands need to be legalized.  Define an ARM-specific version of
	// BUILD_VECTOR for this purpose.
	public static final int BUILD_VECTOR = VMULLu + 1;

	// Floating-point max and min:
	public static final int FMAX = BUILD_VECTOR + 1;
	public static final int FMIN = FMAX + 1;

	// Bit-field insert
	public static final int BFI = FMIN + 1;

	// Vector OR with immediate
	public static final int VORRIMM = BFI + 1;
	// Vector AND with NOT of immediate
	public static final int VBICIMM = VORRIMM + 1;

	// Vector bitwise select
	public static final int VBSL = VBICIMM + 1;

	// Vector load N-element structure to all lanes:
	public static final int VLD2DUP = ISD.FIRST_TARGET_MEMORY_OPCODE;
	public static final int VLD3DUP = VLD2DUP + 1;
	public static final int VLD4DUP = VLD3DUP + 1;

	// NEON loads with post-increment base updates:
	public static final int VLD1_UPD = VLD4DUP + 1;
	public static final int VLD2_UPD = VLD1_UPD + 1;
	public static final int VLD3_UPD = VLD2_UPD + 1;
	public static final int VLD4_UPD = VLD3_UPD + 1;
	public static final int VLD2LN_UPD = VLD4_UPD + 1;
	public static final int VLD3LN_UPD = VLD2LN_UPD + 1;
	public static final int VLD4LN_UPD = VLD3LN_UPD + 1;
	public static final int VLD2DUP_UPD = VLD4LN_UPD + 1;
	public static final int VLD3DUP_UPD = VLD2DUP_UPD + 1;
	public static final int VLD4DUP_UPD = VLD3DUP_UPD + 1;

	// NEON stores with post-increment base updates:
	public static final int VST1_UPD = VLD4DUP_UPD + 1;
	public static final int VST2_UPD = VST1_UPD + 1;
	public static final int VST3_UPD = VST2_UPD + 1;
	public static final int VST4_UPD = VST3_UPD + 1;
	public static final int VST2LN_UPD = VST4_UPD + 1;
	public static final int VST3LN_UPD = VST2LN_UPD + 1;
	public static final int VST4LN_UPD = VST3LN_UPD + 1;

	// 64-bit atomic ops (value split into two registers)
	public static final int ATOMADD64_DAG = VST4LN_UPD + 1;
	public static final int ATOMSUB64_DAG = ATOMADD64_DAG + 1;
	public static final int ATOMOR64_DAG = ATOMSUB64_DAG + 1;
	public static final int ATOMXOR64_DAG = ATOMOR64_DAG + 1;
	public static final int ATOMAND64_DAG = ATOMXOR64_DAG + 1;
	public static final int ATOMNAND64_DAG = ATOMAND64_DAG + 1;
	public static final int ATOMSWAP64_DAG = ATOMNAND64_DAG + 1;
	public static final int ATOMCMPXCHG64_DAG = ATOMSWAP64_DAG + 1;

	public static boolean isBitFieldInvertedMask(long v) {
		if (v == 0xffffffff) return false;

		int lsb = 0, msb = 31;
		while ((v & (1 << msb)) != 0) --msb;
		while ((v & (1 << lsb)) != 0) ++lsb;
		for (int i = lsb; i <= msb; ++i)
			if ((v & (1 << i)) != 0)
				return false;
		return true;
	}
}
