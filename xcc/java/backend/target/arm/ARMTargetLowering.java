package backend.target.arm;
/*
 * Extremely C language Compiler
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

import backend.codegen.*;
import backend.codegen.dagisel.*;
import backend.debug.DebugLoc;
import backend.intrinsic.Intrinsic;
import backend.mc.MCRegisterClass;
import backend.support.BackendCmdOptions;
import backend.support.CallingConv;
import backend.support.LLVMContext;
import backend.target.TargetLowering;
import backend.target.TargetLoweringObjectFile;
import backend.target.TargetMachine;
import backend.target.TargetOptions;
import backend.target.arm.ARMConstantPoolValue.ARMCP;
import backend.value.BlockAddress;
import backend.value.ConstantFP;
import backend.value.GlobalValue;
import backend.value.Value;
import tools.OutRef;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;

import static backend.target.TargetLowering.LegalizeAction.*;
import static backend.target.arm.ARMGenCallingConv.*;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ARMTargetLowering extends TargetLowering {
  public static class ARMCCState extends CCState {
    ARMCCState(CallingConv cc, boolean isVarArg,
               TargetMachine tm,
               ArrayList<CCValAssign> locs,
               LLVMContext ctx,
               ParmContext pc) {
      super(cc, isVarArg, tm, locs, ctx);
      Util.assertion(pc == ParmContext.Call || pc == ParmContext.Prologue);
      callOrPrologue = pc;
    }
  }

  private static TargetLoweringObjectFile createTLOF(TargetMachine tm) {
    if (tm.getSubtarget().isTargetDarwin()) {
      Util.shouldNotReachHere("ARM on Darwin is not supported now!");
      return null;
    }
    return new backend.target.arm.ARMELFTargetObjectFile();
  }

  private ARMSubtarget subtarget;
  private ARMRegisterInfo regInfo;
  private int ARMPCLabelIndex;

  public ARMTargetLowering(ARMTargetMachine tm) {
    super(tm, createTLOF(tm));
    subtarget = tm.getSubtarget();
    regInfo = subtarget.getRegisterInfo();
    setBooleanVectorContents(BooleanContent.ZeroOrNegativeOneBooleanContent);

    if (subtarget.isTargetDarwin()) {
      // Uses VFP for Thumb libfuncs if available.
      if (subtarget.isThumb() && subtarget.hasVFP2()) {
        // Single-precision floating-point arithmetic.
        setLibCallName(RTLIB.ADD_F32, "__addsf3vfp");
        setLibCallName(RTLIB.SUB_F32, "__subsf3vfp");
        setLibCallName(RTLIB.MUL_F32, "__mulsf3vfp");
        setLibCallName(RTLIB.DIV_F32, "__divsf3vfp");

        // Double-precision floating-point arithmetic.
        setLibCallName(RTLIB.ADD_F64, "__adddf3vfp");
        setLibCallName(RTLIB.SUB_F64, "__subdf3vfp");
        setLibCallName(RTLIB.MUL_F64, "__muldf3vfp");
        setLibCallName(RTLIB.DIV_F64, "__divdf3vfp");

        // Single-precision comparisons.
        setLibCallName(RTLIB.OEQ_F32, "__eqsf2vfp");
        setLibCallName(RTLIB.UNE_F32, "__nesf2vfp");
        setLibCallName(RTLIB.OLT_F32, "__ltsf2vfp");
        setLibCallName(RTLIB.OLE_F32, "__lesf2vfp");
        setLibCallName(RTLIB.OGE_F32, "__gesf2vfp");
        setLibCallName(RTLIB.OGT_F32, "__gtsf2vfp");
        setLibCallName(RTLIB.UO_F32,  "__unordsf2vfp");
        setLibCallName(RTLIB.O_F32,   "__unordsf2vfp");
        
        setCmpLibCallCC(RTLIB.OEQ_F32, CondCode.SETNE);
        setCmpLibCallCC(RTLIB.UNE_F32, CondCode.SETNE);
        setCmpLibCallCC(RTLIB.OLT_F32, CondCode.SETNE);
        setCmpLibCallCC(RTLIB.OLE_F32, CondCode.SETNE);
        setCmpLibCallCC(RTLIB.OGE_F32, CondCode.SETNE);
        setCmpLibCallCC(RTLIB.OGT_F32, CondCode.SETNE);
        setCmpLibCallCC(RTLIB.UO_F32,  CondCode.SETNE);
        setCmpLibCallCC(RTLIB.O_F32,   CondCode.SETEQ);

        // Double-precision comparisons.
        setLibCallName(RTLIB.OEQ_F64, "__eqdf2vfp");
        setLibCallName(RTLIB.UNE_F64, "__nedf2vfp");
        setLibCallName(RTLIB.OLT_F64, "__ltdf2vfp");
        setLibCallName(RTLIB.OLE_F64, "__ledf2vfp");
        setLibCallName(RTLIB.OGE_F64, "__gedf2vfp");
        setLibCallName(RTLIB.OGT_F64, "__gtdf2vfp");
        setLibCallName(RTLIB.UO_F64,  "__unorddf2vfp");
        setLibCallName(RTLIB.O_F64,   "__unorddf2vfp");

        setCmpLibCallCC(RTLIB.OEQ_F64, CondCode.SETNE);
        setCmpLibCallCC(RTLIB.UNE_F64, CondCode.SETNE);
        setCmpLibCallCC(RTLIB.OLT_F64, CondCode.SETNE);
        setCmpLibCallCC(RTLIB.OLE_F64, CondCode.SETNE);
        setCmpLibCallCC(RTLIB.OGE_F64, CondCode.SETNE);
        setCmpLibCallCC(RTLIB.OGT_F64, CondCode.SETNE);
        setCmpLibCallCC(RTLIB.UO_F64,  CondCode.SETNE);
        setCmpLibCallCC(RTLIB.O_F64,   CondCode.SETEQ);

        // Floating-point to integer conversions.
        // i64 conversions are done via library routines even when generating VFP
        // instructions, so use the same ones.
        setLibCallName(RTLIB.FPTOSINT_F64_I32, "__fixdfsivfp");
        setLibCallName(RTLIB.FPTOUINT_F64_I32, "__fixunsdfsivfp");
        setLibCallName(RTLIB.FPTOSINT_F32_I32, "__fixsfsivfp");
        setLibCallName(RTLIB.FPTOUINT_F32_I32, "__fixunssfsivfp");

        // Conversions between floating types.
        setLibCallName(RTLIB.FPROUND_F64_F32, "__truncdfsf2vfp");
        setLibCallName(RTLIB.FPEXT_F32_F64,   "__extendsfdf2vfp");

        // Integer to floating-point conversions.
        // i64 conversions are done via library routines even when generating VFP
        // instructions, so use the same ones.
        // FIXME: There appears to be some naming inconsistency in ARM libgcc: e.g.
        // __floatunsidf vs. __floatunssidfvfp.
        setLibCallName(RTLIB.SINTTOFP_I32_F64, "__floatsidfvfp");
        setLibCallName(RTLIB.UINTTOFP_I32_F64, "__floatunssidfvfp");
        setLibCallName(RTLIB.SINTTOFP_I32_F32, "__floatsisfvfp");
        setLibCallName(RTLIB.UINTTOFP_I32_F32, "__floatunssisfvfp");
      }
    }

    // These libcalls are not available in 32-bit.
    setLibCallName(RTLIB.SHL_I128, null);
    setLibCallName(RTLIB.SRL_I128, null);
    setLibCallName(RTLIB.SRA_I128, null);

    if (subtarget.isAAPCS_ABI()) {
      // Double-precision floating-point arithmetic helper functions
      // RTABI chapter 4.1.2, Table 2
      setLibCallName(RTLIB.ADD_F64, "__aeabi_dadd");
      setLibCallName(RTLIB.DIV_F64, "__aeabi_ddiv");
      setLibCallName(RTLIB.MUL_F64, "__aeabi_dmul");
      setLibCallName(RTLIB.SUB_F64, "__aeabi_dsub");
      setLibCallCallingConv(RTLIB.ADD_F64, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.DIV_F64, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.MUL_F64, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.SUB_F64, CallingConv.ARM_AAPCS);

      // Double-precision floating-point comparison helper functions
      // RTABI chapter 4.1.2, Table 3
      setLibCallName(RTLIB.OEQ_F64, "__aeabi_dcmpeq");
      setCmpLibCallCC(RTLIB.OEQ_F64, CondCode.SETNE);
      setLibCallName(RTLIB.UNE_F64, "__aeabi_dcmpeq");
      setCmpLibCallCC(RTLIB.UNE_F64, CondCode.SETEQ);
      setLibCallName(RTLIB.OLT_F64, "__aeabi_dcmplt");
      setCmpLibCallCC(RTLIB.OLT_F64, CondCode.SETNE);
      setLibCallName(RTLIB.OLE_F64, "__aeabi_dcmple");
      setCmpLibCallCC(RTLIB.OLE_F64, CondCode.SETNE);
      setLibCallName(RTLIB.OGE_F64, "__aeabi_dcmpge");
      setCmpLibCallCC(RTLIB.OGE_F64, CondCode.SETNE);
      setLibCallName(RTLIB.OGT_F64, "__aeabi_dcmpgt");
      setCmpLibCallCC(RTLIB.OGT_F64, CondCode.SETNE);
      setLibCallName(RTLIB.UO_F64,  "__aeabi_dcmpun");
      setCmpLibCallCC(RTLIB.UO_F64,  CondCode.SETNE);
      setLibCallName(RTLIB.O_F64,   "__aeabi_dcmpun");
      setCmpLibCallCC(RTLIB.O_F64,   CondCode.SETEQ);
      setLibCallCallingConv(RTLIB.OEQ_F64, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.UNE_F64, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.OLT_F64, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.OLE_F64, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.OGE_F64, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.OGT_F64, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.UO_F64, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.O_F64, CallingConv.ARM_AAPCS);

      // Single-precision floating-point arithmetic helper functions
      // RTABI chapter 4.1.2, Table 4
      setLibCallName(RTLIB.ADD_F32, "__aeabi_fadd");
      setLibCallName(RTLIB.DIV_F32, "__aeabi_fdiv");
      setLibCallName(RTLIB.MUL_F32, "__aeabi_fmul");
      setLibCallName(RTLIB.SUB_F32, "__aeabi_fsub");
      setLibCallCallingConv(RTLIB.ADD_F32, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.DIV_F32, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.MUL_F32, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.SUB_F32, CallingConv.ARM_AAPCS);

      // Single-precision floating-point comparison helper functions
      // RTABI chapter 4.1.2, Table 5
      setLibCallName(RTLIB.OEQ_F32, "__aeabi_fcmpeq");
      setCmpLibCallCC(RTLIB.OEQ_F32, CondCode.SETNE);
      setLibCallName(RTLIB.UNE_F32, "__aeabi_fcmpeq");
      setCmpLibCallCC(RTLIB.UNE_F32, CondCode.SETEQ);
      setLibCallName(RTLIB.OLT_F32, "__aeabi_fcmplt");
      setCmpLibCallCC(RTLIB.OLT_F32, CondCode.SETNE);
      setLibCallName(RTLIB.OLE_F32, "__aeabi_fcmple");
      setCmpLibCallCC(RTLIB.OLE_F32, CondCode.SETNE);
      setLibCallName(RTLIB.OGE_F32, "__aeabi_fcmpge");
      setCmpLibCallCC(RTLIB.OGE_F32, CondCode.SETNE);
      setLibCallName(RTLIB.OGT_F32, "__aeabi_fcmpgt");
      setCmpLibCallCC(RTLIB.OGT_F32, CondCode.SETNE);
      setLibCallName(RTLIB.UO_F32,  "__aeabi_fcmpun");
      setCmpLibCallCC(RTLIB.UO_F32,  CondCode.SETNE);
      setLibCallName(RTLIB.O_F32,   "__aeabi_fcmpun");
      setCmpLibCallCC(RTLIB.O_F32,   CondCode.SETEQ);
      setLibCallCallingConv(RTLIB.OEQ_F32, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.UNE_F32, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.OLT_F32, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.OLE_F32, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.OGE_F32, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.OGT_F32, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.UO_F32, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.O_F32, CallingConv.ARM_AAPCS);

      // Floating-point to integer conversions.
      // RTABI chapter 4.1.2, Table 6
      setLibCallName(RTLIB.FPTOSINT_F64_I32, "__aeabi_d2iz");
      setLibCallName(RTLIB.FPTOUINT_F64_I32, "__aeabi_d2uiz");
      setLibCallName(RTLIB.FPTOSINT_F64_I64, "__aeabi_d2lz");
      setLibCallName(RTLIB.FPTOUINT_F64_I64, "__aeabi_d2ulz");
      setLibCallName(RTLIB.FPTOSINT_F32_I32, "__aeabi_f2iz");
      setLibCallName(RTLIB.FPTOUINT_F32_I32, "__aeabi_f2uiz");
      setLibCallName(RTLIB.FPTOSINT_F32_I64, "__aeabi_f2lz");
      setLibCallName(RTLIB.FPTOUINT_F32_I64, "__aeabi_f2ulz");
      setLibCallCallingConv(RTLIB.FPTOSINT_F64_I32, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.FPTOUINT_F64_I32, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.FPTOSINT_F64_I64, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.FPTOUINT_F64_I64, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.FPTOSINT_F32_I32, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.FPTOUINT_F32_I32, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.FPTOSINT_F32_I64, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.FPTOUINT_F32_I64, CallingConv.ARM_AAPCS);

      // Conversions between floating types.
      // RTABI chapter 4.1.2, Table 7
      setLibCallName(RTLIB.FPROUND_F64_F32, "__aeabi_d2f");
      setLibCallName(RTLIB.FPEXT_F32_F64,   "__aeabi_f2d");
      setLibCallCallingConv(RTLIB.FPROUND_F64_F32, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.FPEXT_F32_F64, CallingConv.ARM_AAPCS);

      // Integer to floating-point conversions.
      // RTABI chapter 4.1.2, Table 8
      setLibCallName(RTLIB.SINTTOFP_I32_F64, "__aeabi_i2d");
      setLibCallName(RTLIB.UINTTOFP_I32_F64, "__aeabi_ui2d");
      setLibCallName(RTLIB.SINTTOFP_I64_F64, "__aeabi_l2d");
      setLibCallName(RTLIB.UINTTOFP_I64_F64, "__aeabi_ul2d");
      setLibCallName(RTLIB.SINTTOFP_I32_F32, "__aeabi_i2f");
      setLibCallName(RTLIB.UINTTOFP_I32_F32, "__aeabi_ui2f");
      setLibCallName(RTLIB.SINTTOFP_I64_F32, "__aeabi_l2f");
      setLibCallName(RTLIB.UINTTOFP_I64_F32, "__aeabi_ul2f");
      setLibCallCallingConv(RTLIB.SINTTOFP_I32_F64, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.UINTTOFP_I32_F64, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.SINTTOFP_I64_F64, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.UINTTOFP_I64_F64, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.SINTTOFP_I32_F32, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.UINTTOFP_I32_F32, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.SINTTOFP_I64_F32, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.UINTTOFP_I64_F32, CallingConv.ARM_AAPCS);

      // Long long helper functions
      // RTABI chapter 4.2, Table 9
      setLibCallName(RTLIB.MUL_I64,  "__aeabi_lmul");
      setLibCallName(RTLIB.SDIV_I64, "__aeabi_ldivmod");
      setLibCallName(RTLIB.UDIV_I64, "__aeabi_uldivmod");
      setLibCallName(RTLIB.SHL_I64, "__aeabi_llsl");
      setLibCallName(RTLIB.SRL_I64, "__aeabi_llsr");
      setLibCallName(RTLIB.SRA_I64, "__aeabi_lasr");
      setLibCallCallingConv(RTLIB.MUL_I64, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.SDIV_I64, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.UDIV_I64, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.SHL_I64, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.SRL_I64, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.SRA_I64, CallingConv.ARM_AAPCS);

      // Integer division functions
      // RTABI chapter 4.3.1
      setLibCallName(RTLIB.SDIV_I8,  "__aeabi_idiv");
      setLibCallName(RTLIB.SDIV_I16, "__aeabi_idiv");
      setLibCallName(RTLIB.SDIV_I32, "__aeabi_idiv");
      setLibCallName(RTLIB.UDIV_I8,  "__aeabi_uidiv");
      setLibCallName(RTLIB.UDIV_I16, "__aeabi_uidiv");
      setLibCallName(RTLIB.UDIV_I32, "__aeabi_uidiv");
      setLibCallCallingConv(RTLIB.SDIV_I8, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.SDIV_I16, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.SDIV_I32, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.UDIV_I8, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.UDIV_I16, CallingConv.ARM_AAPCS);
      setLibCallCallingConv(RTLIB.UDIV_I32, CallingConv.ARM_AAPCS);

      // Memory operations
      // RTABI chapter 4.3.4
      setLibCallName(RTLIB.MEMCPY,  "__aeabi_memcpy");
      setLibCallName(RTLIB.MEMMOVE, "__aeabi_memmove");
      setLibCallName(RTLIB.MEMSET,  "__aeabi_memset");
    }

    if (subtarget.isThumb1Only())
      addRegisterClass(MVT.i32, ARMGenRegisterInfo.tGPRRegisterClass);
    else
      addRegisterClass(MVT.i32, ARMGenRegisterInfo.GPRRegisterClass);

    if (!TargetOptions.GenerateSoftFloatCalls.value && subtarget.hasVFP2() && !subtarget.isThumb()) {
      addRegisterClass(MVT.f32, ARMGenRegisterInfo.SPRRegisterClass);
      addRegisterClass(MVT.f64, ARMGenRegisterInfo.DPRRegisterClass);
    }

    if (subtarget.hasNEON()) {
      addDRTypeForNEON(MVT.v2f32);
      addDRTypeForNEON(MVT.v8i8);
      addDRTypeForNEON(MVT.v4i16);
      addDRTypeForNEON(MVT.v2i32);
      addDRTypeForNEON(MVT.v1i64);

      addQRTypeForNEON(MVT.v4f32);
      addQRTypeForNEON(MVT.v2f64);
      addQRTypeForNEON(MVT.v16i8);
      addQRTypeForNEON(MVT.v8i16);
      addQRTypeForNEON(MVT.v4i32);
      addQRTypeForNEON(MVT.v2i64);

      // v2f64 is legal so that QR subregs can be extracted as f64 elements, but
      // neither Neon nor VFP support any arithmetic operations on it.
      setOperationAction(ISD.FADD, MVT.v2f64, Expand);
      setOperationAction(ISD.FSUB, MVT.v2f64, Expand);
      setOperationAction(ISD.FMUL, MVT.v2f64, Expand);
      setOperationAction(ISD.FDIV, MVT.v2f64, Expand);
      setOperationAction(ISD.FREM, MVT.v2f64, Expand);
      setOperationAction(ISD.FCOPYSIGN, MVT.v2f64, Expand);
      setOperationAction(ISD.SETCC, MVT.v2f64, Expand);
      setOperationAction(ISD.FNEG, MVT.v2f64, Expand);
      setOperationAction(ISD.FABS, MVT.v2f64, Expand);
      setOperationAction(ISD.FSQRT, MVT.v2f64, Expand);
      setOperationAction(ISD.FSIN, MVT.v2f64, Expand);
      setOperationAction(ISD.FCOS, MVT.v2f64, Expand);
      setOperationAction(ISD.FPOWI, MVT.v2f64, Expand);
      setOperationAction(ISD.FPOW, MVT.v2f64, Expand);
      setOperationAction(ISD.FLOG, MVT.v2f64, Expand);
      setOperationAction(ISD.FLOG2, MVT.v2f64, Expand);
      setOperationAction(ISD.FLOG10, MVT.v2f64, Expand);
      setOperationAction(ISD.FEXP, MVT.v2f64, Expand);
      setOperationAction(ISD.FEXP2, MVT.v2f64, Expand);
      setOperationAction(ISD.FCEIL, MVT.v2f64, Expand);
      setOperationAction(ISD.FTRUNC, MVT.v2f64, Expand);
      setOperationAction(ISD.FRINT, MVT.v2f64, Expand);
      setOperationAction(ISD.FNEARBYINT, MVT.v2f64, Expand);
      setOperationAction(ISD.FFLOOR, MVT.v2f64, Expand);

      setTruncStoreAction(MVT.v2f64, MVT.v2f32, Expand);

      // Neon does not support some operations on v1i64 and v2i64 types.
      setOperationAction(ISD.MUL, MVT.v1i64, Expand);
      // Custom handling for some quad-vector types to detect VMULL.
      setOperationAction(ISD.MUL, MVT.v8i16, Custom);
      setOperationAction(ISD.MUL, MVT.v4i32, Custom);
      setOperationAction(ISD.MUL, MVT.v2i64, Custom);
      // Custom handling for some vector types to avoid expensive expansions
      setOperationAction(ISD.SDIV, MVT.v4i16, Custom);
      setOperationAction(ISD.SDIV, MVT.v8i8, Custom);
      setOperationAction(ISD.UDIV, MVT.v4i16, Custom);
      setOperationAction(ISD.UDIV, MVT.v8i8, Custom);
      setOperationAction(ISD.SETCC, MVT.v1i64, Expand);
      setOperationAction(ISD.SETCC, MVT.v2i64, Expand);
      // Neon does not have single instruction SINT_TO_FP and UINT_TO_FP with
      // a destination type that is wider than the source.
      setOperationAction(ISD.SINT_TO_FP, MVT.v4i16, Custom);
      setOperationAction(ISD.UINT_TO_FP, MVT.v4i16, Custom);

      setTargetDAGCombine(ISD.INTRINSIC_VOID);
      setTargetDAGCombine(ISD.INTRINSIC_W_CHAIN);
      setTargetDAGCombine(ISD.INTRINSIC_WO_CHAIN);
      setTargetDAGCombine(ISD.SHL);
      setTargetDAGCombine(ISD.SRL);
      setTargetDAGCombine(ISD.SRA);
      setTargetDAGCombine(ISD.SIGN_EXTEND);
      setTargetDAGCombine(ISD.ZERO_EXTEND);
      setTargetDAGCombine(ISD.ANY_EXTEND);
      setTargetDAGCombine(ISD.SELECT_CC);
      setTargetDAGCombine(ISD.BUILD_VECTOR);
      setTargetDAGCombine(ISD.VECTOR_SHUFFLE);
      setTargetDAGCombine(ISD.INSERT_VECTOR_ELT);
      setTargetDAGCombine(ISD.STORE);
      setTargetDAGCombine(ISD.FP_TO_SINT);
      setTargetDAGCombine(ISD.FP_TO_UINT);
      setTargetDAGCombine(ISD.FDIV);
    }

    computeRegisterProperties();

    // ARM does not have f32 extending load.
    setLoadExtAction(LoadExtType.EXTLOAD, new MVT(MVT.f32), Expand);

    // ARM doesn't have i1 sign extending load.
    setLoadExtAction(LoadExtType.SEXTLOAD, new MVT(MVT.i1), Promote);

    if (!subtarget.isThumb1Only()) {
      // ARM supports all 4 flavors of integer indexed load / store.
      for (int i = 0, e = MemIndexedMode.values().length; i < e; ++i) {
        MemIndexedMode im = MemIndexedMode.values()[i];
        if (im == MemIndexedMode.LAST_INDEXED_MODE) continue;

        setIndexedLoadAction(im, MVT.i1, Legal);
        setIndexedLoadAction(im, MVT.i8, Legal);
        setIndexedLoadAction(im, MVT.i16, Legal);
        setIndexedLoadAction(im, MVT.i32, Legal);
        setIndexedStoreAction(im, MVT.i1, Legal);
        setIndexedStoreAction(im, MVT.i8, Legal);
        setIndexedStoreAction(im, MVT.i16, Legal);
        setIndexedStoreAction(im, MVT.i32, Legal);
      }
    }

    // i64 operation support.
    setOperationAction(ISD.MUL, MVT.i64, Expand);
    setOperationAction(ISD.MULHU, MVT.i32, Expand);
    if (subtarget.isThumb1Only()) {
      setOperationAction(ISD.UMUL_LOHI, MVT.i32, Expand);
      setOperationAction(ISD.SMUL_LOHI, MVT.i32, Expand);
    }

    if (subtarget.isThumb1Only() || !subtarget.hasV6Ops() ||
        (subtarget.isThumb2() && !subtarget.hasThumb2DSP()))
      setOperationAction(ISD.MULHS,   MVT.i32, Expand);

    setOperationAction(ISD.SHL_PARTS, MVT.i32, Expand);
    setOperationAction(ISD.SRA_PARTS, MVT.i32, Expand);
    setOperationAction(ISD.SRL_PARTS, MVT.i32, Expand);
    setOperationAction(ISD.SRL,       MVT.i64, Custom);
    setOperationAction(ISD.SRA,       MVT.i64, Custom);

    if (!subtarget.isThumb1Only()) {
      setOperationAction(ISD.ADDC, MVT.i32, Custom);
      setOperationAction(ISD.ADDE, MVT.i32, Custom);
      setOperationAction(ISD.SUBC, MVT.i32, Custom);
      setOperationAction(ISD.SUBE, MVT.i32, Custom);
    }

    // ARM does not have ROTL.
    setOperationAction(ISD.ROTL,  MVT.i32, Expand);
    setOperationAction(ISD.CTTZ , MVT.i32, Expand);
    setOperationAction(ISD.CTPOP, MVT.i32, Expand);
    if (!subtarget.hasV5TOps() || subtarget.isThumb1Only())
      setOperationAction(ISD.CTLZ, MVT.i32, Expand);

    // Only ARMv6 has BSWAP.
    if (!subtarget.hasV6Ops())
      setOperationAction(ISD.BSWAP, MVT.i32, Expand);

    if (!subtarget.hasDivide() || !subtarget.isThumb2()) {
      // v7M has a hardware divider
      setOperationAction(ISD.SDIV, MVT.i32, Expand);
      setOperationAction(ISD.UDIV, MVT.i32, Expand);
    }

    setOperationAction(ISD.SREM,  MVT.i32, Expand);
    setOperationAction(ISD.UREM,  MVT.i32, Expand);
    setOperationAction(ISD.SDIVREM, MVT.i32, Expand);
    setOperationAction(ISD.UDIVREM, MVT.i32, Expand);

    // Support label based line numbers.
    setOperationAction(ISD.DEBUG_LOC, MVT.Other, Expand);

    setOperationAction(ISD.GlobalAddress, MVT.i32,   Custom);
    setOperationAction(ISD.ConstantPool,  MVT.i32,   Custom);
    setOperationAction(ISD.GLOBAL_OFFSET_TABLE, MVT.i32, Custom);
    setOperationAction(ISD.GlobalTLSAddress, MVT.i32, Custom);
    setOperationAction(ISD.BlockAddress, MVT.i32, Custom);

    setOperationAction(ISD.TRAP, MVT.Other, Legal);

    // Use the default implementation.
    setOperationAction(ISD.VASTART           , MVT.Other, Custom);
    setOperationAction(ISD.VAARG             , MVT.Other, Expand);
    setOperationAction(ISD.VACOPY            , MVT.Other, Expand);
    setOperationAction(ISD.VAEND             , MVT.Other, Expand);
    setOperationAction(ISD.STACKSAVE,          MVT.Other, Expand);
    setOperationAction(ISD.STACKRESTORE,       MVT.Other, Expand);
    setOperationAction(ISD.DYNAMIC_STACKALLOC, MVT.i32  , Expand);
    setOperationAction(ISD.EHSELECTION, MVT.i32, Expand);
    setOperationAction(ISD.EXCEPTIONADDR, MVT.i32, Expand);
    setExceptionPointerRegister(ARMGenRegisterNames.R0);
    setExceptionSelectorRegister(ARMGenRegisterNames.R1);

    // ARMv6 Thumb1 (except for CPUs that support dmb / dsb) and earlier use
    // the default expansion.
    // FIXME: This should be checking for v6k, not just v6.
    if (subtarget.hasDataBarrier() || (subtarget.hasV6Ops() && !subtarget.isThumb())) {
      // membarrier needs custom lowering; the rest are legal and handled
      // normally.
      setOperationAction(ISD.MEMBARRIER, MVT.Other, Custom);
      setOperationAction(ISD.ATOMIC_FENCE, MVT.Other, Custom);
      // Custom lowering for 64-bit ops
      setOperationAction(ISD.ATOMIC_LOAD_ADD,  MVT.i64, Custom);
      setOperationAction(ISD.ATOMIC_LOAD_SUB,  MVT.i64, Custom);
      setOperationAction(ISD.ATOMIC_LOAD_AND,  MVT.i64, Custom);
      setOperationAction(ISD.ATOMIC_LOAD_OR,   MVT.i64, Custom);
      setOperationAction(ISD.ATOMIC_LOAD_XOR,  MVT.i64, Custom);
      setOperationAction(ISD.ATOMIC_SWAP,  MVT.i64, Custom);
      setOperationAction(ISD.ATOMIC_CMP_SWAP,  MVT.i64, Custom);
      // Automatically insert fences (dmb ist) around ATOMIC_SWAP etc.
      setInsertFencesForAtomic(true);
    } else {
      // Set them all for expansion, which will force libcalls.
      setOperationAction(ISD.MEMBARRIER, MVT.Other, Expand);
      setOperationAction(ISD.ATOMIC_FENCE,   MVT.Other, Expand);
      setOperationAction(ISD.ATOMIC_CMP_SWAP,  MVT.i32, Expand);
      setOperationAction(ISD.ATOMIC_SWAP,      MVT.i32, Expand);
      setOperationAction(ISD.ATOMIC_LOAD_ADD,  MVT.i32, Expand);
      setOperationAction(ISD.ATOMIC_LOAD_SUB,  MVT.i32, Expand);
      setOperationAction(ISD.ATOMIC_LOAD_AND,  MVT.i32, Expand);
      setOperationAction(ISD.ATOMIC_LOAD_OR,   MVT.i32, Expand);
      setOperationAction(ISD.ATOMIC_LOAD_XOR,  MVT.i32, Expand);
      setOperationAction(ISD.ATOMIC_LOAD_NAND, MVT.i32, Expand);
      setOperationAction(ISD.ATOMIC_LOAD_MIN, MVT.i32, Expand);
      setOperationAction(ISD.ATOMIC_LOAD_MAX, MVT.i32, Expand);
      setOperationAction(ISD.ATOMIC_LOAD_UMIN, MVT.i32, Expand);
      setOperationAction(ISD.ATOMIC_LOAD_UMAX, MVT.i32, Expand);
      // Mark ATOMIC_LOAD and ATOMIC_STORE custom so we can handle the
      // Unordered/Monotonic case.
      setOperationAction(ISD.ATOMIC_LOAD, MVT.i32, Custom);
      setOperationAction(ISD.ATOMIC_STORE, MVT.i32, Custom);
      // Since the libcalls include locking, fold in the fences
      setShouldFoldAtomicFences(true);
    }

    setOperationAction(ISD.PREFETCH,         MVT.Other, Custom);



    if (!TargetOptions.GenerateSoftFloatCalls.value && subtarget.hasVFP2() && !subtarget.isThumb1Only()) {
      // Turn f64->i64 into VMOVRRD, i64 -> f64 to VMOVDRR
      // iff target supports vfp2.
      setOperationAction(ISD.BIT_CONVERT, MVT.i64, Custom);
      setOperationAction(ISD.FLT_ROUNDS_, MVT.i32, Custom);
    }

    // We want to custom lower some of our intrinsics.
    setOperationAction(ISD.INTRINSIC_WO_CHAIN, MVT.Other, Custom);
    if (subtarget.isTargetDarwin()) {
      /*setOperationAction(ISD.EH_SJLJ_SETJMP, MVT.i32, Custom);
      setOperationAction(ISD.EH_SJLJ_LONGJMP, MVT.Other, Custom);
      setOperationAction(ISD.EH_SJLJ_DISPATCHSETUP, MVT.Other, Custom);*/
      setLibCallName(RTLIB.UNWIND_RESUME, "_Unwind_SjLj_Resume");
    }

    // Requires SXTB/SXTH, available on v6 and up in both ARM and Thumb modes.
    if (!subtarget.hasV6Ops()) {
      setOperationAction(ISD.SIGN_EXTEND_INREG, MVT.i16, Expand);
      setOperationAction(ISD.SIGN_EXTEND_INREG, MVT.i8,  Expand);
    }
    setOperationAction(ISD.SIGN_EXTEND_INREG, MVT.i1, Expand);

    
    setOperationAction(ISD.SETCC    , MVT.i32, Expand);
    setOperationAction(ISD.SETCC    , MVT.f32, Expand);
    setOperationAction(ISD.SETCC    , MVT.f64, Expand);
    setOperationAction(ISD.SELECT   , MVT.i32, Expand);
    setOperationAction(ISD.SELECT   , MVT.f32, Expand);
    setOperationAction(ISD.SELECT   , MVT.f64, Expand);
    setOperationAction(ISD.SELECT_CC, MVT.i32, Custom);
    setOperationAction(ISD.SELECT_CC, MVT.f32, Custom);
    setOperationAction(ISD.SELECT_CC, MVT.f64, Custom);

    setOperationAction(ISD.BRCOND   , MVT.Other, Expand);
    setOperationAction(ISD.BR_CC    , MVT.i32,   Custom);
    setOperationAction(ISD.BR_CC    , MVT.f32,   Custom);
    setOperationAction(ISD.BR_CC    , MVT.f64,   Custom);
    setOperationAction(ISD.BR_JT    , MVT.Other, Custom);

    setOperationAction(ISD.VASTART,       MVT.Other, Custom);
    setOperationAction(ISD.VACOPY,        MVT.Other, Expand);
    setOperationAction(ISD.VAEND,         MVT.Other, Expand);
    setOperationAction(ISD.STACKSAVE,     MVT.Other, Expand);
    setOperationAction(ISD.STACKRESTORE,  MVT.Other, Expand);

    // FP Constants can't be immediates.
    setOperationAction(ISD.ConstantFP, MVT.f64, Expand);
    setOperationAction(ISD.ConstantFP, MVT.f32, Expand);

    // We don't support sin/cos/fmod/copysign
    setOperationAction(ISD.FSIN     , MVT.f64, Expand);
    setOperationAction(ISD.FSIN     , MVT.f32, Expand);
    setOperationAction(ISD.FCOS     , MVT.f32, Expand);
    setOperationAction(ISD.FCOS     , MVT.f64, Expand);
    setOperationAction(ISD.FREM     , MVT.f64, Expand);
    setOperationAction(ISD.FREM     , MVT.f32, Expand);
    if (!TargetOptions.GenerateSoftFloatCalls.value && subtarget.hasVFP2() && !subtarget.isThumb1Only()) {
      setOperationAction(ISD.FCOPYSIGN, MVT.f64, Custom);
      setOperationAction(ISD.FCOPYSIGN, MVT.f32, Custom);
    }

    setOperationAction(ISD.FPOW, MVT.f64, Expand);
    setOperationAction(ISD.FPOW, MVT.f32, Expand);

    setOperationAction(ISD.FMA, MVT.f64, Expand);
    setOperationAction(ISD.FMA, MVT.f32, Expand);

    // int <-> fp are custom expanded into bit_convert + ARMISD ops.
    if (!TargetOptions.GenerateSoftFloatCalls.value && !subtarget.isThumb1Only()) {
      if (subtarget.hasVFP2()) {
        setOperationAction(ISD.SINT_TO_FP, MVT.i32, Custom);
        setOperationAction(ISD.UINT_TO_FP, MVT.i32, Custom);
        setOperationAction(ISD.FP_TO_UINT, MVT.i32, Custom);
        setOperationAction(ISD.FP_TO_SINT, MVT.i32, Custom);
      }
      // Specail handling for half-precision FP.
      if (!subtarget.hasFP16()) {
        setOperationAction(ISD.FP16_TO_FP32, MVT.f32, Expand);
        setOperationAction(ISD.FP32_TO_FP16, MVT.i32, Expand);
      }
    }

    // We have target-specific dag combine patterns for the following nodes:
    // ARMISD.VMOVRRD  - No need to call setTargetDAGCombine
    setTargetDAGCombine(ISD.ADD);
    setTargetDAGCombine(ISD.SUB);
    setTargetDAGCombine(ISD.MUL);

    if (subtarget.hasV6T2Ops() || subtarget.hasNEON())
      setTargetDAGCombine(ISD.OR);
    if (subtarget.hasNEON())
      setTargetDAGCombine(ISD.AND);


    setStackPointerRegisterToSaveRestore(ARMGenRegisterNames.SP);

    maxStoresPerMemcpy = 1;
    setMinFunctionAlignment(subtarget.isThumb() ? 1 : 2);
    benefitFromCodePlacementOpt = true;
  }

  private void addDRTypeForNEON(int vt) {
    addRegisterClass(vt, ARMGenRegisterInfo.DPRRegisterClass);
    addTypeForNeon(vt, MVT.f64, MVT.v2i32);
  }

  private void addQRTypeForNEON(int vt) {
    addRegisterClass(vt, ARMGenRegisterInfo.QPRRegisterClass);
    addTypeForNeon(vt, MVT.v2f64, MVT.v4i32);
  }

  private void addTypeForNeon(int vt, int promotedLdStVT, int promotedBitwiseVT) {
    if (vt != promotedLdStVT) {
      setOperationAction(ISD.LOAD, vt, Promote);
      addPromotedToType(ISD.LOAD, vt, promotedLdStVT);

      setOperationAction(ISD.STORE, vt, Promote);
      addPromotedToType(ISD.STORE, vt, promotedLdStVT);
    }

    EVT eltTy = new EVT(vt).getVectorElementType();
    if (!eltTy.equals(new EVT(MVT.i64)) && !eltTy.equals(new EVT(MVT.f64)))
      setOperationAction(ISD.SETCC, vt, Custom);

    setOperationAction(ISD.EXTRACT_VECTOR_ELT, vt, Custom);
    if (!eltTy.equals(new EVT(MVT.i32))) {
      setOperationAction(ISD.SINT_TO_FP, vt, Expand);
      setOperationAction(ISD.UINT_TO_FP, vt, Expand);
      setOperationAction(ISD.FP_TO_SINT, vt, Expand);
      setOperationAction(ISD.FP_TO_UINT, vt, Expand);
    }

    setOperationAction(ISD.BUILD_VECTOR, vt, Custom);
    setOperationAction(ISD.VECTOR_SHUFFLE, vt, Custom);
    setOperationAction(ISD.CONCAT_VECTORS, vt, Legal);
    setOperationAction(ISD.EXTRACT_SUBVECTOR, vt, Legal);
    setOperationAction(ISD.SELECT, vt, Expand);
    setOperationAction(ISD.SELECT_CC, vt, Expand);

    if (new EVT(vt).isInteger()) {
      setOperationAction(ISD.SHL, vt, Custom);
      setOperationAction(ISD.SRA, vt, Custom);
      setOperationAction(ISD.SRL, vt, Custom);
      setOperationAction(ISD.SIGN_EXTEND, vt, Expand);
      setOperationAction(ISD.ZERO_EXTEND, vt, Expand);
      for (int innerVT = MVT.FIRST_VECTOR_VALUETYPE; innerVT <= MVT.LAST_VECTOR_VALUETYPE; ++innerVT)
        setTruncStoreAction(vt, innerVT, Expand);
    }

    setLoadExtAction(LoadExtType.EXTLOAD, new MVT(vt), Expand);

    // Promote all bit-wise operations.
    if (new EVT(vt).isInteger() && vt != promotedBitwiseVT) {
      setOperationAction(ISD.AND, vt, Promote);
      addPromotedToType(ISD.AND, vt, promotedBitwiseVT);

      setOperationAction(ISD.OR, vt, Promote);
      addPromotedToType(ISD.OR, vt, promotedBitwiseVT);

      setOperationAction(ISD.XOR, vt, Promote);
      addPromotedToType(ISD.XOR, vt, promotedBitwiseVT);
    }

    // Neon does not support vector divide/remainder operations.
    setOperationAction(ISD.SDIV, vt, Expand);
    setOperationAction(ISD.UDIV, vt, Expand);
    setOperationAction(ISD.FDIV, vt, Expand);
    setOperationAction(ISD.SREM, vt, Expand);
    setOperationAction(ISD.UREM, vt, Expand);
    setOperationAction(ISD.FREM, vt, Expand);
  }

  @Override
  public MachineFunctionInfo createMachineFunctionInfo(MachineFunction mf) {
    return new ARMFunctionInfo(mf);
  }

  private SDValue getF64FormalArgument(CCValAssign va, CCValAssign nextVA,
                                       SDValue root, SelectionDAG dag,
                                       DebugLoc dl) {
    ARMFunctionInfo afi = (ARMFunctionInfo) dag.getMachineFunction().getFunctionInfo();
    MachineFunction mf = dag.getMachineFunction();
    MCRegisterClass rc;
    if (afi.isThumb1OnlyFunction())
      rc = ARMGenRegisterInfo.tGPRRegisterClass;
    else
      rc = ARMGenRegisterInfo.GPRRegisterClass;

    // copy the first i32 to a virtual register.
    mf.addLiveIn(va.getLocReg(), rc);
    SDValue arg1 = dag.getCopyFromReg(root, va.getLocReg(), new EVT(MVT.i32));

    SDValue arg2;
    if (nextVA.isRegLoc()) {
      arg2 = dag.getCopyFromReg(root, nextVA.getLocReg(), new EVT(MVT.i32));
    }
    else {
      Util.assertion(nextVA.isMemLoc());
      int argSize = nextVA.getLocVT().getSizeInBits()/8;
      MachineFrameInfo mfi = mf.getFrameInfo();
      int fi = mfi.createFixedObject(argSize, nextVA.getLocMemOffset());
      SDValue fin = dag.getTargetFrameIndex(fi, new EVT(getPointerTy()));
      arg2 = dag.getLoad(new EVT(MVT.i32), root, fin, null, 0);
    }
    return dag.getNode(ARMISD.VMOVDRR, new EVT(MVT.f64), arg1, arg2);
  }

  @Override
  public SDValue lowerFormalArguments(SDValue chain,
                                      CallingConv callingConv,
                                      boolean varArg,
                                      ArrayList<InputArg> ins,
                                      SelectionDAG dag,
                                      ArrayList<SDValue> inVals) {
    MachineFunction mf = dag.getMachineFunction();
    MachineFrameInfo mfi = mf.getFrameInfo();
    ArrayList<CCValAssign> argLocs = new ArrayList<>();
    ARMFunctionInfo afi = (ARMFunctionInfo) mf.getFunctionInfo();
    DebugLoc dl = new DebugLoc();
    ARMCCState ccInfo = new ARMCCState(callingConv, varArg, getTargetMachine(),
        argLocs, dag.getContext(), CCState.ParmContext.Prologue);
    ccInfo.analyzeFormalArguments(ins, ccAssignFnForNode(callingConv, false, varArg));

    //int lastInsIndex = -1;
    SDValue argValue;
    for (int i = 0,  e = argLocs.size(); i < e; i++) {
      CCValAssign va = argLocs.get(i);
      if (va.isRegLoc()) {
        EVT regVT = va.getLocVT();
        if (va.needsCustom()) {
          // f64 and vector types are split up into multiple registers or combinations of registers and stack slots.
          if (va.getLocVT().equals(new EVT(MVT.v2f64))) {
            // get the first f64
            SDValue op0 = getF64FormalArgument(va, argLocs.get(++i), chain, dag, dl);

            // get the second f64.
            va = argLocs.get(++i);
            SDValue op1;
            if (va.isMemLoc()) {
              int fi = mfi.createFixedObject(8, va.getLocMemOffset(), true);
              SDValue fin = dag.getFrameIndex(fi, new EVT(getPointerTy()), false);
              op1 = dag.getLoad(new EVT(MVT.f64), chain, fin,
                      PseudoSourceValue.getFixedStack(fi), 0);
            }
            else
              op1 = getF64FormalArgument(va, argLocs.get(++i), chain, dag, dl);

            // create vector with both f64.
            SDValue vec = dag.getNode(ISD.UNDEF, new EVT(MVT.v2f64));
            vec = dag.getNode(ISD.INSERT_VECTOR_ELT, new EVT(MVT.v2f64), vec, op0, dag.getIntPtrConstant(0, true));
            argValue = dag.getNode(ISD.INSERT_VECTOR_ELT, new EVT(MVT.v2f64), vec, op1, dag.getIntPtrConstant(1, true));
          }
          else {
            argValue = getF64FormalArgument(va, argLocs.get(++i), chain, dag, dl);
          }
        }
        else {
          MCRegisterClass rc = null;
          if (regVT.equals(new EVT(MVT.i32)))
            rc = afi.isThumb1OnlyFunction() ? ARMGenRegisterInfo.tGPRRegisterClass :
                ARMGenRegisterInfo.GPRRegisterClass;
          else if (regVT.equals(new EVT(MVT.f32)))
            rc = ARMGenRegisterInfo.SPRRegisterClass;
          else if (regVT.equals(new EVT(MVT.f64)))
            rc = ARMGenRegisterInfo.DPRRegisterClass;
          else if (regVT.equals(new EVT(MVT.v2f64)))
            rc = ARMGenRegisterInfo.QPRRegisterClass;
          else
            Util.shouldNotReachHere("regVT is not supported!");

          int reg = mf.addLiveIn(va.getLocReg(), rc);
          argValue = dag.getCopyFromReg(chain, reg, regVT);
        }

        // handle extension.
        switch (va.getLocInfo()) {
          default:
            Util.shouldNotReachHere("Unknown loc info!");
          case Full: break;
          case BCvt:
            argValue = dag.getNode(ISD.BIT_CONVERT, va.getValVT(), argValue);
            break;
          case SExt:
            argValue = dag.getNode(ISD.AssertSext, regVT, argValue,
                dag.getValueType(va.getValVT()));
            argValue = dag.getNode(ISD.TRUNCATE, va.getValVT(), argValue);
            break;
          case ZExt:
            argValue = dag.getNode(ISD.AssertZext, regVT, argValue,
                dag.getValueType(va.getValVT()));
            argValue = dag.getNode(ISD.TRUNCATE, va.getValVT(), argValue);
            break;
        }
        inVals.add(argValue);
      }
      else {
        Util.assertion(va.isMemLoc() && !va.getValVT().equals(new EVT(MVT.i64)));
        int fi = mfi.createFixedObject(va.getLocVT().getSizeInBits()/8,
            va.getLocMemOffset(), true);
        SDValue fin = dag.getFrameIndex(fi, new EVT(getPointerTy()), false);
        inVals.add(dag.getLoad(va.getValVT(), chain, fin,
            PseudoSourceValue.getFixedStack(fi), 0, false, 0));
      }
    }
    if (varArg) {
      final int[] GPRArgRegs = {ARMGenRegisterNames.R0, ARMGenRegisterNames.R1,
                                 ARMGenRegisterNames.R2, ARMGenRegisterNames.R3};
      // Return the starting index of GPRArgRegs to indicate which register was not allocated.
      // When those unallocated registers are used to pass variable arguments, we have to save
      // it on the stack slot for further use by loading it back to the register in the callee.
      int numGPRs = ccInfo.getFirstUnallocated(GPRArgRegs);
      int align = subtarget.getFrameLowering().getStackAlignment();
      // stack space cost needed for variable arguments.
      int vaRegSize = (4 - numGPRs) * 4;
      int vaRegAligned = (vaRegSize + align - 1) & -align;
      int argOffset = ccInfo.getNextStackOffset();
      if (vaRegAligned != 0) {
        afi.setVarArgsRegSaveSize(vaRegAligned);
        // [ argOffset      ]
        // [ stack alignment + argOffset]
        // [stack alignment + argOffset + argRegSize]
        afi.setVarArgsFrameIndex(mfi.createFixedObject(vaRegAligned,
            argOffset + vaRegAligned - vaRegSize, false));
        SDValue fin = dag.getFrameIndex(afi.getVarArgsFrameIndex(), new EVT(getPointerTy()), false);
        // Store the value of unallocated registers to the stack slot.
        ArrayList<SDValue> memOps = new ArrayList<>();
        for (; numGPRs < 4; ++numGPRs) {
          MCRegisterClass rc;
          if (afi.isThumb1OnlyFunction())
            rc = ARMGenRegisterInfo.tGPRRegisterClass;
          else
            rc = ARMGenRegisterInfo.GPRRegisterClass;

          mf.addLiveIn(GPRArgRegs[numGPRs], rc);
          SDValue reg = dag.getCopyFromReg(chain, GPRArgRegs[numGPRs], new EVT(MVT.i32));
          chain = reg.getValue(1);
          SDValue store = dag.getStore(chain, reg, fin, null, 0, false, 0);
          memOps.add(store);
          fin = dag.getNode(ISD.ADD, new EVT(getPointerTy()), fin, dag.getConstant(4, new EVT(getPointerTy()), false));
        }
        if (!memOps.isEmpty())
          chain = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), memOps);
      }
      else
        afi.setVarArgsFrameIndex(mfi.createFixedObject(4, argOffset, true));
    }
    return chain;
  }

  /**
   * Assign the incoming arguments to the specified locations, register or stack slot, according
   * to the specified calling convection.
   * @param cc
   * @return
   */
  private CCAssignFn ccAssignFnForNode(CallingConv cc, boolean isReturn, boolean isVarArg) {
    switch (cc) {
      default:
        Util.shouldNotReachHere("Unknown calling convenction");
      case Fast:
        if (subtarget.hasVFP2() && !isVarArg) {
          if (!subtarget.isAAPCS_ABI())
            return isReturn ? RetFastCC_ARM_APCS : FastCC_ARM_APCS;
          // For AAPCS ABI targets, just use VFP variant of the calling convention.
          return isReturn ? RetCC_ARM_AAPCS_VFP : CC_ARM_AAPCS_VFP;
        }
        // fall through.
      case C: {
        // Use target triple & subtarget features to do actual dispatch.
        if (!subtarget.isAAPCS_ABI())
          return isReturn ? RetCC_ARM_APCS : CC_ARM_APCS;
        else
          return isReturn ? RetCC_ARM_AAPCS : CC_ARM_AAPCS;
      }
      case ARM_AAPCS_VFP:
        return isReturn ? RetCC_ARM_AAPCS_VFP : CC_ARM_AAPCS_VFP;
      case ARM_AAPCS:
        return isReturn ? RetCC_ARM_AAPCS : CC_ARM_AAPCS;
      case ARM_APCS:
        return isReturn ? RetCC_ARM_APCS: CC_ARM_APCS;
    }
  }

  private static SDValue createCopyOfByValArgument(SDValue src, SDValue dst, SDValue chain,
                                                   ArgFlagsTy flags, SelectionDAG dag, DebugLoc dl) {
    SDValue sizeNode = dag.getConstant(flags.getByValSize(), new EVT(MVT.i32), false);
    return dag.getMemcpy(chain, dst, src, sizeNode, flags.getByValAlign(), false/*alwaysInline*/,
        null, 0, null, 0);
  }

  private SDValue lowerMemOpCallTo(SDValue chain, SDValue stackPtr, SDValue arg,
                                          DebugLoc dl, SelectionDAG dag, CCValAssign va,
                                          ArgFlagsTy flags) {
    int locMemOffset = va.getLocMemOffset();
    SDValue ptrOffset = dag.getIntPtrConstant(locMemOffset);
    ptrOffset = dag.getNode(ISD.ADD, new EVT(getPointerTy()), stackPtr, ptrOffset);
    if (flags.isByVal()) {
      return createCopyOfByValArgument(arg, ptrOffset, chain, flags, dag, dl);
    }
    return dag.getStore(chain, arg, ptrOffset, PseudoSourceValue.getStack(), locMemOffset, false, 0);
  }

  private void passF64ArgInRegs(DebugLoc dl, SelectionDAG dag,
                                SDValue chain, SDValue arg,
                                ArrayList<Pair<Integer, SDValue>> regsToPass,
                                CCValAssign va, CCValAssign nextVA,
                                SDValue stackPtr, ArrayList<SDValue> memOpChains,
                                ArgFlagsTy flags) {
    SDValue vmorrd = dag.getNode(ARMISD.VMOVRRD, dag.getVTList(new EVT(MVT.i32), new EVT(MVT.i32)), arg);
    // move the first half of f64 to the register.
    regsToPass.add(Pair.get(va.getLocReg(), vmorrd));
    if (nextVA.isRegLoc())
      // we can move the second half of f64 to the register as well
      regsToPass.add(Pair.get(nextVA.getLocReg(), vmorrd.getValue(1)));
    else {
      // Otherwise, move the second half of f64 to the stack slot.
      Util.assertion(nextVA.isMemLoc());
      memOpChains.add(lowerMemOpCallTo(chain, stackPtr, arg, dl, dag, nextVA, flags));
    }
  }

  /**
   * Constructs the following serial of DAG operations.
   * callseq_start <--- call <--- callseq_end.
   * @param chain
   * @param callee
   * @param cc
   * @param isVarArg
   * @param isTailCall
   * @param outs
   * @param ins
   * @param dag
   * @param inVals
   * @return
   */
  @Override
  public SDValue lowerCall(SDValue chain,
                           SDValue callee,
                           CallingConv cc,
                           boolean isVarArg,
                           boolean isTailCall,
                           ArrayList<OutputArg> outs,
                           ArrayList<InputArg> ins,
                           SelectionDAG dag,
                           ArrayList<SDValue> inVals) {
    // Analyze the argument locations where we have to write the function call parameters.
    ArrayList<CCValAssign> argLocs = new ArrayList<>();
    ARMCCState ccInfo = new ARMCCState(cc, isVarArg, tm, argLocs, dag.getContext(), CCState.ParmContext.Call);
    ccInfo.analyzeCallOperands(outs, ccAssignFnForNode(cc, false/*isReturn*/, isVarArg));

    // get number of bytes of stack space required for passing arguments.
    int numBytes = ccInfo.getNextStackOffset();
    // adjust the SP
    chain = dag.getCALLSEQ_START(chain, dag.getIntPtrConstant(numBytes, true));

    // SP register.
    SDValue stackPtr = dag.getRegister(ARMGenRegisterNames.SP, new EVT(MVT.i32));
    DebugLoc dl = new DebugLoc();
    ArrayList<Pair<Integer, SDValue>> regsToPass = new ArrayList<>();
    ArrayList<SDValue> memOpChains = new ArrayList<>();

    // Handle outgoing arguments.
    for (int i = 0, e = argLocs.size(); i < e; ++i) {
      CCValAssign va = argLocs.get(i);
      SDValue arg = outs.get(i).val;
      ArgFlagsTy flags = outs.get(i).flags;

      // perform needed type conversion based on the arg flag.
      switch (va.getLocInfo()) {
        case Full: break;
        case SExt:
          arg = dag.getNode(ISD.SIGN_EXTEND, va.getLocVT(), arg);
          break;
        case ZExt:
          arg = dag.getNode(ISD.ZERO_EXTEND, va.getLocVT(), arg);
          break;
        case AExt:
          arg = dag.getNode(ISD.ANY_EXTEND, va.getLocVT(), arg);
          break;
        case BCvt:
          arg = dag.getNode(ISD.BIT_CONVERT, va.getLocVT(), arg);
          break;
        default:
          Util.assertion("Unknown loc info!");
      }
      // handle f64 or 2xf64 with custom code.
      if (va.needsCustom()) {
        if (va.getLocVT().equals(new EVT(MVT.v2f64))) {
          // we need four i32 registers to pass the argument of type v2f64.
          SDValue op0 = dag.getNode(ISD.EXTRACT_VECTOR_ELT, new EVT(MVT.f64), arg, dag.getConstant(0, new EVT(MVT.i32), false));
          SDValue op1 = dag.getNode(ISD.EXTRACT_VECTOR_ELT, new EVT(MVT.f64), arg, dag.getConstant(1, new EVT(MVT.i32), false));

          // first f64.
          passF64ArgInRegs(dl, dag, chain, op0, regsToPass, va, argLocs.get(++i), stackPtr, memOpChains, flags);

          // second f64
          va = argLocs.get(++i);
          if (va.isRegLoc()) {
            passF64ArgInRegs(dl, dag, chain, op1, regsToPass, va, argLocs.get(++i), stackPtr, memOpChains, flags);
          }
          else {
            Util.assertion(va.isMemLoc());
            memOpChains.add(lowerMemOpCallTo(chain, stackPtr, arg, dl, dag, va, flags));
          }
        }
        else {
          passF64ArgInRegs(dl, dag, chain, arg, regsToPass, va, argLocs.get(++i), stackPtr, memOpChains, flags);
        }
      }
      else if (va.isRegLoc()) {
        regsToPass.add(Pair.get(va.getLocReg(), arg));
      }
      else {
        Util.assertion(va.isMemLoc(), "Unknown loc type!");
        memOpChains.add(lowerMemOpCallTo(chain, stackPtr, arg, dl, dag, va, flags));
      }
    }

    if (!memOpChains.isEmpty())
      chain = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), memOpChains);
    // build a sequence of copy-to-reg nodes for each argument copy.
    SDValue inFlag = new SDValue();
    for (int i = 0, e = regsToPass.size(); i < e; ++i) {
      chain = dag.getCopyToReg(chain, regsToPass.get(i).first, regsToPass.get(i).second, inFlag);
      inFlag = chain.getValue(1);
    }

    // if the callee is an External Symbol or Global Address, change it to TargetExternalSymbol or
    // TargetGlobalAddress.
    boolean isDirect = false;
    boolean isARMFunc = false;
    boolean isLocalARMFunc = false;
    if (callee.getNode() instanceof SDNode.GlobalAddressSDNode) {
      SDNode.GlobalAddressSDNode g = (SDNode.GlobalAddressSDNode) callee.getNode();
      GlobalValue gv = g.getGlobalValue();
      isDirect = true;
      // is external
      boolean isExt = gv.isDeclaration() || gv.isWeakForLinker();
      boolean isStub = (isExt && subtarget.isTargetDarwin()) &&
          tm.getRelocationModel() != TargetMachine.RelocModel.Static;
      isARMFunc = !subtarget.isThumb() || isStub;
      isLocalARMFunc = !subtarget.isThumb() && !isExt;
      if (isARMFunc && subtarget.isThumb1Only() && !subtarget.hasV5TOps()) {
        ARMConstantPoolValue cpv = ARMConstantPoolConstant.create(gv, ARMPCLabelIndex, ARMCP.ARMCPKind.CPValue, 4);
        SDValue cpAddr = dag.getTargetConstantPool(cpv, new EVT(getPointerTy()), 4, 0, true, 0);
        cpAddr = dag.getNode(ARMISD.Wrapper, new EVT(MVT.i32), cpAddr);
        callee = dag.getLoad(new EVT(getPointerTy()), dag.getEntryNode(), cpAddr,
            PseudoSourceValue.getConstantPool(), 0, false, 0);
        SDValue picLabel = dag.getConstant(ARMPCLabelIndex, new EVT(MVT.i32), false);
        callee = dag.getNode(ARMISD.PIC_ADD, new EVT(getPointerTy()), callee, picLabel);
      }
      else {
        // On ELF target for PIC mode, direct calls should go through PLT.
        int opFlags = 0;
        if (subtarget.isTargetELF() && tm.getRelocationModel() == TargetMachine.RelocModel.PIC_)
          opFlags = ARMII.MO_PLT;
        callee = dag.getTargetGlobalAddress(gv, new EVT(getPointerTy()), 0, opFlags);
      }
    }
    else if (callee.getNode() instanceof SDNode.ExternalSymbolSDNode) {
      SDNode.ExternalSymbolSDNode exn = (SDNode.ExternalSymbolSDNode) callee.getNode();
      isDirect = true;
      boolean isStub = subtarget.isTargetDarwin() &&
          tm.getRelocationModel() != TargetMachine.RelocModel.Static;
      isARMFunc = !subtarget.isThumb() || isStub;
      // tBX taks a register source operand.
      String sym = exn.getExtSymol();
      if (isARMFunc && subtarget.isThumb1Only() && !subtarget.hasV5TOps()) {
        ARMConstantPoolValue cpv = ARMConstantPoolSymbol.create(dag.getContext(), sym, ARMPCLabelIndex, 4);
        SDValue cpAddr = dag.getTargetConstantPool(cpv, new EVT(getPointerTy()), 4, 0, true, 0);
        cpAddr = dag.getNode(ARMISD.Wrapper, new EVT(MVT.i32), cpAddr);
        callee = dag.getLoad(new EVT(getPointerTy()), dag.getEntryNode(), cpAddr,
            PseudoSourceValue.getConstantPool(), 0, false, 0);
        SDValue picLabel = dag.getConstant(ARMPCLabelIndex, new EVT(MVT.i32), false);
        callee = dag.getNode(ARMISD.PIC_ADD, new EVT(getPointerTy()), callee, picLabel);
      }
      else {
        // On ELF target for PIC mode, direct calls should go through PLT.
        int opFlags = 0;
        if (subtarget.isTargetELF() && tm.getRelocationModel() == TargetMachine.RelocModel.PIC_)
          opFlags = ARMII.MO_PLT;
        callee = dag.getTargetExternalSymbol(sym, new EVT(getPointerTy()), opFlags);
      }
    }

    int callOpc;
    if (subtarget.isThumb()) {
      if ((!isDirect || isARMFunc) && !subtarget.hasV5TOps())
        callOpc = ARMISD.CALL_NOLINK;
      else
        callOpc = isARMFunc ? ARMISD.CALL : ARMISD.tCALL;
    }
    else {
      callOpc = (isDirect || subtarget.hasV5TOps()) ?
          (isLocalARMFunc ? ARMISD.CALL_PRED : ARMISD.CALL) : ARMISD.CALL_NOLINK;
    }

    ArrayList<SDValue> ops = new ArrayList<>();
    ops.add(chain);
    ops.add(callee);

    // add argument registers to the end of operands list.
    regsToPass.forEach(pair-> ops.add(dag.getRegister(pair.first, pair.second.getValueType())));

    if (inFlag.getNode() != null)
      ops.add(inFlag);

    SDNode.SDVTList vts = dag.getVTList(new EVT(MVT.Other), new EVT(MVT.Glue));
    chain = dag.getNode(callOpc, vts, ops);
    inFlag = chain.getValue(1);

    chain = dag.getCALLSEQ_END(chain, dag.getIntPtrConstant(numBytes, true), dag.getIntPtrConstant(0, true), inFlag);
    if (!ins.isEmpty())
      inFlag = chain.getValue(1);

    // handle result.
    return lowerCallResult(chain, inFlag, cc, isVarArg, ins, new DebugLoc(), dag, inVals);
  }

  private SDValue lowerCallResult(SDValue chain, SDValue inFlag,
                                  CallingConv cc, boolean isVarArg,
                                  ArrayList<InputArg> ins,
                                  DebugLoc dl, SelectionDAG dag,
                                  ArrayList<SDValue> inVals)  {
    // Analyze the return arguments locations where we have to write the function return value.
    ArrayList<CCValAssign> retLocs = new ArrayList<>();
    ARMCCState ccInfo = new ARMCCState(cc, isVarArg, tm, retLocs, dag.getContext(), CCState.ParmContext.Call);
    ccInfo.analyzeCallResult(ins, ccAssignFnForNode(cc, true/*isReturn*/, isVarArg));

    for (int i = 0, e = retLocs.size(); i < e; ++i) {
      CCValAssign va = retLocs.get(i);
      SDValue val = new SDValue();
      if (va.needsCustom()) {
        // handle f64 and 2xf64.
        SDValue lo = dag.getCopyFromReg(chain, va.getLocReg(), new EVT(MVT.i32), inFlag);
        chain = lo.getValue(1);
        inFlag = lo.getValue(2);

        va = retLocs.get(++i);
        SDValue hi = dag.getCopyFromReg(chain, va.getLocReg(), new EVT(MVT.i32), inFlag);
        chain = hi.getValue(1);
        inFlag = hi.getValue(2);

        // merge it to a wider one.
        val = dag.getNode(ARMISD.VMOVDRR, new EVT(MVT.f64), lo, hi);

        if (va.getLocVT().equals(new EVT(MVT.v2f64))) {
          // create a vec of type v2f64, insert the first f64 to the first position.
          SDValue vec = dag.getNode(ISD.UNDEF, new EVT(MVT.v2f64));
          vec = dag.getNode(ISD.INSERT_VECTOR_ELT, new EVT(MVT.v2f64), vec, val, dag.getConstant(0, new EVT(MVT.i32), false));

          va = retLocs.get(++i);
          lo = dag.getCopyFromReg(chain, va.getLocReg(), new EVT(MVT.i32), inFlag);
          chain = lo.getValue(1);
          inFlag = lo.getValue(2);

          va = retLocs.get(++i);
          hi = dag.getCopyFromReg(chain, va.getLocReg(), new EVT(MVT.i32), inFlag);
          chain = hi.getValue(1);
          inFlag = hi.getValue(2);

          // merge it to a wider one.
          val = dag.getNode(ARMISD.VMOVDRR, new EVT(MVT.f64), lo, hi);

          // insert second f64 to the second position of vec.
          val = dag.getNode(ISD.INSERT_VECTOR_ELT, new EVT(MVT.v2f64), vec, val, dag.getConstant(1, new EVT(MVT.i32), false));
        }
      }
      else {
        Util.assertion(va.isRegLoc(), "must be reg loc!");
        val = dag.getCopyFromReg(chain, va.getLocReg(), va.getValVT(), inFlag);
        chain = val.getValue(1);
        inFlag = val.getValue(2);
      }

      switch (va.getLocInfo()) {
        case Full: break;
        case BCvt:
          val = dag.getNode(ISD.BIT_CONVERT, va.getValVT(), val);
          break;
        default:
          Util.assertion("Unknown loc info!");
      }
      inVals.add(val);
    }
    return chain;
  }

  @Override
  public SDValue lowerReturn(SDValue chain,
                             CallingConv cc,
                             boolean isVarArg,
                             ArrayList<OutputArg> outs,
                             SelectionDAG dag) {
    ArrayList<CCValAssign> retLocs = new ArrayList<>();
    MachineFunction mf = dag.getMachineFunction();
    MachineFrameInfo mfi = mf.getFrameInfo();
    ARMCCState ccInfo = new ARMCCState(cc, isVarArg, tm,
        retLocs, dag.getContext(), CCState.ParmContext.Call);

    ccInfo.analyzeReturn(outs, ccAssignFnForNode(cc, true, isVarArg));

    // if this is the first return lowerred for this function, add the loc regs
    // to the live out set.
    if (mf.getMachineRegisterInfo().isLiveOutEmpty()) {
      retLocs.forEach(rv-> {
        if (rv.isRegLoc())
          mf.getMachineRegisterInfo().addLiveOut(rv.getLocReg());
      });
    }

    SDValue flag = new SDValue();
    for (int i = 0, realRVLocIdx = 0; i < retLocs.size(); ++i, ++realRVLocIdx) {
      CCValAssign va = retLocs.get(i);
      Util.assertion(va.isRegLoc(), "can only pass return in registers!");

      SDValue arg = outs.get(realRVLocIdx).val;
      switch (va.getLocInfo()) {
        default: Util.shouldNotReachHere("unknown loc info");
        case Full: break;
        case BCvt:
          arg = dag.getNode(ISD.BIT_CONVERT, va.getLocVT(), arg);
          break;
      }
      if (va.needsCustom()) {
        if (va.getLocVT().equals(new EVT(MVT.v2f64))) {
          // extract the first half and return it into two registers.
          SDValue half = dag.getNode(ISD.EXTRACT_VECTOR_ELT, new EVT(MVT.f64),
                  arg, dag.getConstant(0, new EVT(MVT.i32), false));
          SDValue halfGPRs = dag.getNode(ARMISD.VMOVRRD,
                  dag.getVTList(new EVT(MVT.i32), new EVT(MVT.i32)), half);
          chain = dag.getCopyToReg(chain, va.getLocReg(), halfGPRs, flag);
          flag = chain.getValue(1);
          va = retLocs.get(++i);
          chain = dag.getCopyToReg(chain, va.getLocReg(), halfGPRs.getValue(1), flag);
          flag = chain.getValue(1);
          va = retLocs.get(++i);
          // Extract the 2nd half and fall through to handle it as an f64 value.
          arg = dag.getNode(ISD.EXTRACT_VECTOR_ELT, new EVT(MVT.f64), arg,
                  dag.getConstant(1, new EVT(MVT.i32), false));
        }
        // Legalize ret f64 -> ret 2 x i32.  We always have fmrrd if f64 is
        // available.
        SDValue fmmrrd = dag.getNode(ARMISD.VMOVRRD,
                dag.getVTList(new EVT(MVT.i32), new EVT(MVT.i32)), arg);
        chain = dag.getCopyToReg(chain, va.getLocReg(), fmmrrd, flag);
        flag = chain.getValue(1);
        va = retLocs.get(++i);
        chain = dag.getCopyToReg(chain, va.getLocReg(), fmmrrd.getValue(1),
                flag);
      }
      else
        chain = dag.getCopyToReg(chain, va.getLocReg(), arg, flag);
      flag = chain.getValue(1);
    }
    SDValue result;
    if (flag.getNode() != null)
      result = dag.getNode(ARMISD.RET_FLAG, new EVT(MVT.Other), chain, flag);
    else
      // return void.
      result = dag.getNode(ARMISD.RET_FLAG, new EVT(MVT.Other), chain);

    return result;
  }

  @Override
  public String getTargetNodeName(int opcode) {
    switch (opcode) {
      default: return null;
      case ARMISD.Wrapper:       return "ARMISD.Wrapper";
      case ARMISD.WrapperDYN:    return "ARMISD.WrapperDYN";
      case ARMISD.WrapperPIC:    return "ARMISD.WrapperPIC";
      case ARMISD.WrapperJT:     return "ARMISD.WrapperJT";
      case ARMISD.CALL:          return "ARMISD.CALL";
      case ARMISD.CALL_PRED:     return "ARMISD.CALL_PRED";
      case ARMISD.CALL_NOLINK:   return "ARMISD.CALL_NOLINK";
      case ARMISD.tCALL:         return "ARMISD.tCALL";
      case ARMISD.BRCOND:        return "ARMISD.BRCOND";
      case ARMISD.BR_JT:         return "ARMISD.BR_JT";
      case ARMISD.BR2_JT:        return "ARMISD.BR2_JT";
      case ARMISD.RET_FLAG:      return "ARMISD.RET_FLAG";
      case ARMISD.PIC_ADD:       return "ARMISD.PIC_ADD";
      case ARMISD.CMP:           return "ARMISD.CMP";
      case ARMISD.CMPZ:          return "ARMISD.CMPZ";
      case ARMISD.CMPFP:         return "ARMISD.CMPFP";
      case ARMISD.CMPFPw0:       return "ARMISD.CMPFPw0";
      case ARMISD.BCC_i64:       return "ARMISD.BCC_i64";
      case ARMISD.FMSTAT:        return "ARMISD.FMSTAT";
      case ARMISD.CMOV:          return "ARMISD.CMOV";

      case ARMISD.RBIT:          return "ARMISD.RBIT";

      case ARMISD.FTOSI:         return "ARMISD.FTOSI";
      case ARMISD.FTOUI:         return "ARMISD.FTOUI";
      case ARMISD.SITOF:         return "ARMISD.SITOF";
      case ARMISD.UITOF:         return "ARMISD.UITOF";

      case ARMISD.SRL_FLAG:      return "ARMISD.SRL_FLAG";
      case ARMISD.SRA_FLAG:      return "ARMISD.SRA_FLAG";
      case ARMISD.RRX:           return "ARMISD.RRX";

      case ARMISD.ADDC:          return "ARMISD.ADDC";
      case ARMISD.ADDE:          return "ARMISD.ADDE";
      case ARMISD.SUBC:          return "ARMISD.SUBC";
      case ARMISD.SUBE:          return "ARMISD.SUBE";

      case ARMISD.VMOVRRD:       return "ARMISD.VMOVRRD";
      case ARMISD.VMOVDRR:       return "ARMISD.VMOVDRR";

      case ARMISD.EH_SJLJ_SETJMP: return "ARMISD.EH_SJLJ_SETJMP";
      case ARMISD.EH_SJLJ_LONGJMP:return "ARMISD.EH_SJLJ_LONGJMP";
      case ARMISD.EH_SJLJ_DISPATCHSETUP:return "ARMISD.EH_SJLJ_DISPATCHSETUP";

      case ARMISD.TC_RETURN:     return "ARMISD.TC_RETURN";

      case ARMISD.THREAD_POINTER:return "ARMISD.THREAD_POINTER";

      case ARMISD.DYN_ALLOC:     return "ARMISD.DYN_ALLOC";

      case ARMISD.MEMBARRIER:    return "ARMISD.MEMBARRIER";
      case ARMISD.MEMBARRIER_MCR: return "ARMISD.MEMBARRIER_MCR";

      case ARMISD.PRELOAD:       return "ARMISD.PRELOAD";

      case ARMISD.VCEQ:          return "ARMISD.VCEQ";
      case ARMISD.VCEQZ:         return "ARMISD.VCEQZ";
      case ARMISD.VCGE:          return "ARMISD.VCGE";
      case ARMISD.VCGEZ:         return "ARMISD.VCGEZ";
      case ARMISD.VCLEZ:         return "ARMISD.VCLEZ";
      case ARMISD.VCGEU:         return "ARMISD.VCGEU";
      case ARMISD.VCGT:          return "ARMISD.VCGT";
      case ARMISD.VCGTZ:         return "ARMISD.VCGTZ";
      case ARMISD.VCLTZ:         return "ARMISD.VCLTZ";
      case ARMISD.VCGTU:         return "ARMISD.VCGTU";
      case ARMISD.VTST:          return "ARMISD.VTST";

      case ARMISD.VSHL:          return "ARMISD.VSHL";
      case ARMISD.VSHRs:         return "ARMISD.VSHRs";
      case ARMISD.VSHRu:         return "ARMISD.VSHRu";
      case ARMISD.VSHLLs:        return "ARMISD.VSHLLs";
      case ARMISD.VSHLLu:        return "ARMISD.VSHLLu";
      case ARMISD.VSHLLi:        return "ARMISD.VSHLLi";
      case ARMISD.VSHRN:         return "ARMISD.VSHRN";
      case ARMISD.VRSHRs:        return "ARMISD.VRSHRs";
      case ARMISD.VRSHRu:        return "ARMISD.VRSHRu";
      case ARMISD.VRSHRN:        return "ARMISD.VRSHRN";
      case ARMISD.VQSHLs:        return "ARMISD.VQSHLs";
      case ARMISD.VQSHLu:        return "ARMISD.VQSHLu";
      case ARMISD.VQSHLsu:       return "ARMISD.VQSHLsu";
      case ARMISD.VQSHRNs:       return "ARMISD.VQSHRNs";
      case ARMISD.VQSHRNu:       return "ARMISD.VQSHRNu";
      case ARMISD.VQSHRNsu:      return "ARMISD.VQSHRNsu";
      case ARMISD.VQRSHRNs:      return "ARMISD.VQRSHRNs";
      case ARMISD.VQRSHRNu:      return "ARMISD.VQRSHRNu";
      case ARMISD.VQRSHRNsu:     return "ARMISD.VQRSHRNsu";
      case ARMISD.VGETLANEu:     return "ARMISD.VGETLANEu";
      case ARMISD.VGETLANEs:     return "ARMISD.VGETLANEs";
      case ARMISD.VMOVIMM:       return "ARMISD.VMOVIMM";
      case ARMISD.VMVNIMM:       return "ARMISD.VMVNIMM";
      case ARMISD.VDUP:          return "ARMISD.VDUP";
      case ARMISD.VDUPLANE:      return "ARMISD.VDUPLANE";
      case ARMISD.VEXT:          return "ARMISD.VEXT";
      case ARMISD.VREV64:        return "ARMISD.VREV64";
      case ARMISD.VREV32:        return "ARMISD.VREV32";
      case ARMISD.VREV16:        return "ARMISD.VREV16";
      case ARMISD.VZIP:          return "ARMISD.VZIP";
      case ARMISD.VUZP:          return "ARMISD.VUZP";
      case ARMISD.VTRN:          return "ARMISD.VTRN";
      case ARMISD.VTBL1:         return "ARMISD.VTBL1";
      case ARMISD.VTBL2:         return "ARMISD.VTBL2";
      case ARMISD.VMULLs:        return "ARMISD.VMULLs";
      case ARMISD.VMULLu:        return "ARMISD.VMULLu";
      case ARMISD.BUILD_VECTOR:  return "ARMISD.BUILD_VECTOR";
      case ARMISD.FMAX:          return "ARMISD.FMAX";
      case ARMISD.FMIN:          return "ARMISD.FMIN";
      case ARMISD.BFI:           return "ARMISD.BFI";
      case ARMISD.VORRIMM:       return "ARMISD.VORRIMM";
      case ARMISD.VBICIMM:       return "ARMISD.VBICIMM";
      case ARMISD.VBSL:          return "ARMISD.VBSL";
      case ARMISD.VLD2DUP:       return "ARMISD.VLD2DUP";
      case ARMISD.VLD3DUP:       return "ARMISD.VLD3DUP";
      case ARMISD.VLD4DUP:       return "ARMISD.VLD4DUP";
      case ARMISD.VLD1_UPD:      return "ARMISD.VLD1_UPD";
      case ARMISD.VLD2_UPD:      return "ARMISD.VLD2_UPD";
      case ARMISD.VLD3_UPD:      return "ARMISD.VLD3_UPD";
      case ARMISD.VLD4_UPD:      return "ARMISD.VLD4_UPD";
      case ARMISD.VLD2LN_UPD:    return "ARMISD.VLD2LN_UPD";
      case ARMISD.VLD3LN_UPD:    return "ARMISD.VLD3LN_UPD";
      case ARMISD.VLD4LN_UPD:    return "ARMISD.VLD4LN_UPD";
      case ARMISD.VLD2DUP_UPD:   return "ARMISD.VLD2DUP_UPD";
      case ARMISD.VLD3DUP_UPD:   return "ARMISD.VLD3DUP_UPD";
      case ARMISD.VLD4DUP_UPD:   return "ARMISD.VLD4DUP_UPD";
      case ARMISD.VST1_UPD:      return "ARMISD.VST1_UPD";
      case ARMISD.VST2_UPD:      return "ARMISD.VST2_UPD";
      case ARMISD.VST3_UPD:      return "ARMISD.VST3_UPD";
      case ARMISD.VST4_UPD:      return "ARMISD.VST4_UPD";
      case ARMISD.VST2LN_UPD:    return "ARMISD.VST2LN_UPD";
      case ARMISD.VST3LN_UPD:    return "ARMISD.VST3LN_UPD";
      case ARMISD.VST4LN_UPD:    return "ARMISD.VST4LN_UPD";
    }
  }

  /**
   * ConstantPool, JumpTable, GlobalAddress, and ExternalSymbol are lowered as their
   * target counterpart wrapped in the {@linkplain ARMISD#Wrapper} node.
   * @param op
   * @param dag
   * @return
   */
  private SDValue lowerConstantPool(SDValue op, SelectionDAG dag) {
    SDNode.ConstantPoolSDNode csp = (SDNode.ConstantPoolSDNode) op.getNode();
    SDValue res;
    EVT ptrVT = op.getValueType();
    if (csp.isMachineConstantPoolValue())
      res = dag.getTargetConstantPool(csp.getMachineConstantPoolValue(), ptrVT,
          csp.getAlignment(), csp.getOffset(), true, csp.getTargetFlags());
    else
      res = dag.getTargetConstantPool(csp.getConstantValue(), ptrVT,
          csp.getAlignment(), csp.getOffset(), csp.getTargetFlags());
    return dag.getNode(ARMISD.Wrapper, new EVT(MVT.i32), res);
  }

  private SDValue lowerBlockAddress(SDValue op, SelectionDAG dag) {
    SDNode.BlockAddressSDNode ban = (SDNode.BlockAddressSDNode) op.getNode();
    BlockAddress ba = ban.getBlockAddress();
    EVT ptrVT = new EVT(getPointerTy());
    TargetMachine.RelocModel rm = tm.getRelocationModel();
    ARMFunctionInfo afi = (ARMFunctionInfo) dag.getMachineFunction().getFunctionInfo();

    SDValue cpAddr;
    if (rm == TargetMachine.RelocModel.Static)
      cpAddr = dag.getTargetConstantPool(ba, ptrVT, 4, 0, 0);
    else {
      int pcAdj = subtarget.isThumb() ? 4 : 8;
      ARMPCLabelIndex = afi.createPICLabelUId();
      ARMConstantPoolValue cpv = ARMConstantPoolConstant.create(ba,
          ARMPCLabelIndex, ARMCP.ARMCPKind.CPBlockAddress, pcAdj);
      cpAddr = dag.getTargetConstantPool(cpv, ptrVT, 4, 0, true, 0);
    }
    cpAddr = dag.getNode(ARMISD.Wrapper, new EVT(MVT.i32), cpAddr);
    SDValue result = dag.getLoad(ptrVT, dag.getEntryNode(), cpAddr,
        PseudoSourceValue.getConstantPool(), 0);
    if (rm == TargetMachine.RelocModel.Static)
      return result;

    SDValue picLabel = dag.getConstant(ARMPCLabelIndex, new EVT(MVT.i32), false);
    return dag.getNode(ARMISD.PIC_ADD, ptrVT, result, picLabel);
  }

  private SDValue lowerGlobalAddressDarwin(SDValue op, SelectionDAG dag)  {
    Util.shouldNotReachHere();
    return null;
  }
  private SDValue lowerGlobalAddressELF(SDValue op, SelectionDAG dag) {
    EVT ptrTy = new EVT(getPointerTy());
    GlobalValue gv = ((SDNode.GlobalAddressSDNode)op.getNode()).getGlobalValue();
    TargetMachine.RelocModel relocModel = tm.getRelocationModel();
    if (relocModel == TargetMachine.RelocModel.PIC_) {
      boolean useGOTOFF = gv.hasLocalLinkage() || gv.hasHiddenVisibility();
      ARMConstantPoolValue cstValue = ARMConstantPoolConstant.create(gv, useGOTOFF ?
          ARMCP.ARMCPModifier.GOTOFF : ARMCP.ARMCPModifier.GOT);
      SDValue cpAddr = dag.getTargetConstantPool(cstValue, ptrTy, 4, 0, false, 0);
      cpAddr = dag.getNode(ARMISD.Wrapper, new EVT(MVT.i32), cpAddr);
      SDValue result = dag.getLoad(ptrTy, dag.getEntryNode(), cpAddr,
          PseudoSourceValue.getConstantPool(), 0);
      SDValue chain = result.getValue(1);
      SDValue got = dag.getGLOBAL_OFFSET_TABLE(ptrTy);
      result = dag.getNode(ISD.ADD, ptrTy, result, got);
      if (!useGOTOFF)
        result = dag.getLoad(ptrTy, chain, result, PseudoSourceValue.getGOT(), 0);

      return result;
    }

    // If we have T2 ops, we can materialize the address directly via movt/movw
    // pair. This is always cheaper.
    if (subtarget.useMovt()) {
      return dag.getNode(ARMISD.Wrapper, ptrTy, dag.getTargetGlobalAddress(gv, ptrTy, 0, 0));
    }
    else {
      SDValue cpAddr = dag.getTargetConstantPool(gv, ptrTy, 4, 0, 0);
      cpAddr = dag.getNode(ARMISD.Wrapper, new EVT(MVT.i32), cpAddr);
      return dag.getLoad(ptrTy, dag.getEntryNode(), cpAddr, PseudoSourceValue.getConstantPool(), 0);
    }
  }
  private SDValue lowerGlobalTLSAddress(SDValue op, SelectionDAG dag)  {
    Util.shouldNotReachHere();
    return null;
  }
  private SDValue lowerToTLSGeneralDynamicModel(SDNode.GlobalAddressSDNode ga, SelectionDAG dag)  {
    Util.shouldNotReachHere();
    return null;
  }
  private SDValue lowerToTLSExecModels(SDNode.GlobalAddressSDNode ga, SelectionDAG dag)  {
    Util.shouldNotReachHere();
    return null;
  }
  private SDValue lowerGLOBAL_OFFSET_TABLE(SDValue op, SelectionDAG dag) {
    Util.assertion(subtarget.isTargetELF(), "GLOBAL_OFFSET_TABELE not implemented for non-ELF targets");
    MachineFunction mf = dag.getMachineFunction();
    EVT ptrVT = new EVT(getPointerTy());
    int pcAdj = subtarget.isThumb() ? 4 : 8;
    ARMConstantPoolValue cpv = null;
    SDValue cpAddr = dag.getTargetConstantPool(cpv, ptrVT, 4, 0, true, 0);
    cpAddr = dag.getNode(ARMISD.Wrapper, new EVT(MVT.i32), cpAddr);
    SDValue result = dag.getLoad(ptrVT, dag.getEntryNode(), cpAddr, PseudoSourceValue.getConstantPool(), 0);
    SDValue picLabel = dag.getConstant(ARMPCLabelIndex++, new EVT(MVT.i32), false);
    return dag.getNode(ARMISD.PIC_ADD, ptrVT, result, picLabel);
  }

  private SDValue lowerBR_JT(SDValue op, SelectionDAG dag)  {
    SDValue chain = op.getOperand(0);
    SDValue table = op.getOperand(1);
    SDValue index = op.getOperand(2);
    DebugLoc dl = op.getDebugLoc();

    EVT ptrTy = new EVT(getPointerTy());
    SDNode.JumpTableSDNode jt = (SDNode.JumpTableSDNode) table.getNode();
    ARMFunctionInfo afi = (ARMFunctionInfo) dag.getMachineFunction().getFunctionInfo();
    SDValue uid = dag.getConstant(afi.createJumpTableUId(), ptrTy, false);
    SDValue jti = dag.getTargetJumpTable(jt.getJumpTableIndex(), ptrTy, 0);
    table = dag.getNode(ARMISD.WrapperJT, new EVT(MVT.i32), jti, uid);
    index = dag.getNode(ISD.MUL, ptrTy, index, dag.getConstant(4, ptrTy, false));
    SDValue addr = dag.getNode(ISD.ADD, ptrTy, index, table);
    if (subtarget.isThumb2()) {
      return dag.getNode(ARMISD.BR2_JT, new EVT(MVT.Other), chain, addr, op.getOperand(2), jti, uid);
    }
    if (tm.getRelocationModel() == TargetMachine.RelocModel.PIC_) {
      addr = dag.getLoad(new EVT(MVT.i32), chain, addr, PseudoSourceValue.getJumpTable(), 0);
      chain = addr.getValue(1);
      addr = dag.getNode(ISD.ADD, ptrTy, addr, table);
      return dag.getNode(ARMISD.BR_JT, new EVT(MVT.Other), chain, addr, jti, uid);
    }
    else {
      addr = dag.getLoad(ptrTy, chain, addr, PseudoSourceValue.getJumpTable(), 0);
      chain = addr.getValue(1);
      return dag.getNode(ARMISD.BR_JT, new EVT(MVT.Other), chain, addr, jti, uid);
    }
  }

  /**
   * Convert the {@linkplain ISD#SELECT} to the {@linkplain ISD#SELECT_CC} with the condition
   * cond != 0
   * @param op
   * @param dag
   * @return
   */
  private SDValue lowerSELECT(SDValue op, SelectionDAG dag)  {
    SDValue cond = op.getOperand(0);
    SDValue zero = dag.getConstant(0, cond.getValueType(), false);
    return dag.getSelectCC(cond, zero, op.getOperand(1), op.getOperand(2),
        CondCode.SETNE);
  }
  private SDValue lowerSELECT_CC(SDValue op, SelectionDAG dag)  {
    SDValue lhsCond = op.getOperand(0), rhsCond = op.getOperand(1);
    SDValue trueVal = op.getOperand(2), falseVal = op.getOperand(3);
    CondCode cc = ((SDNode.CondCodeSDNode)(op.getOperand(4).getNode())).getCondition();
    EVT vt = op.getValueType();
    DebugLoc dl = op.getDebugLoc();
    SDValue ccr = dag.getRegister(ARMGenRegisterNames.CPSR, new EVT(MVT.i32));

    if (lhsCond.getValueType().equals(new EVT(MVT.i32))) {
      SDValue armcc = new SDValue();
      SDValue cmp = getARMCmp(lhsCond, rhsCond, cc, armcc, dag, dl);
      return dag.getNode(ARMISD.CMOV, vt, falseVal, trueVal, armcc, ccr, cmp);
    }

    // float point.
    ARMCC.CondCodes[] res = FPCCToARMCC(cc);
    ARMCC.CondCodes cc1 = res[0], cc2 = res[1];
    SDValue armcc = dag.getConstant(cc1.ordinal(), new EVT(MVT.i32), false);
    SDValue cmp = getVFPCmp(lhsCond, rhsCond, dag, dl);
    SDValue result = dag.getNode(ARMISD.CMOV, vt, falseVal, trueVal, armcc, ccr, cmp);
    if (cc2 != ARMCC.CondCodes.AL) {
      // more predicate.
      SDValue cmp2 = getVFPCmp(lhsCond, rhsCond, dag, dl);
      SDValue armcc2 = dag.getConstant(cc2.ordinal(), new EVT(MVT.i32), false);
      result = dag.getNode(ARMISD.CMOV, vt, result, trueVal, armcc2, ccr, cmp2);
    }
    return result;
  }

  /**
   * Convert a DAG integer condition code to an ARM CC
   * @param cc
   * @return
   */
  private static ARMCC.CondCodes IntCCToARMCC(CondCode cc) {
    switch (cc) {
      default: Util.shouldNotReachHere("Unknown condition code!");
      case SETNE:  return ARMCC.CondCodes.NE;
      case SETEQ:  return ARMCC.CondCodes.EQ;
      case SETGT:  return ARMCC.CondCodes.GT;
      case SETGE:  return ARMCC.CondCodes.GE;
      case SETLT:  return ARMCC.CondCodes.LT;
      case SETLE:  return ARMCC.CondCodes.LE;
      case SETUGT: return ARMCC.CondCodes.HI;
      case SETUGE: return ARMCC.CondCodes.HS;
      case SETULT: return ARMCC.CondCodes.LO;
      case SETULE: return ARMCC.CondCodes.LS;
    }
  }

  /**
   * Convert a DAG fp condition code to an ARM cc.
   * @param cc
   */
  private static ARMCC.CondCodes[] FPCCToARMCC(CondCode cc) {
    ARMCC.CondCodes condCode, condCode2;
    condCode2 = ARMCC.CondCodes.AL;
    switch (cc) {
      default: Util.shouldNotReachHere("Unknown FP condition!");
      case SETEQ:
      case SETOEQ: condCode = ARMCC.CondCodes.EQ; break;
      case SETGT:
      case SETOGT: condCode = ARMCC.CondCodes.GT; break;
      case SETGE:
      case SETOGE: condCode = ARMCC.CondCodes.GE; break;
      case SETOLT: condCode = ARMCC.CondCodes.MI; break;
      case SETOLE: condCode = ARMCC.CondCodes.LS; break;
      case SETONE: condCode = ARMCC.CondCodes.MI; condCode2 = ARMCC.CondCodes.GT; break;
      case SETO:   condCode = ARMCC.CondCodes.VC; break;
      case SETUO:  condCode = ARMCC.CondCodes.VS; break;
      case SETUEQ: condCode = ARMCC.CondCodes.EQ; condCode2 = ARMCC.CondCodes.VS; break;
      case SETUGT: condCode = ARMCC.CondCodes.HI; break;
      case SETUGE: condCode = ARMCC.CondCodes.PL; break;
      case SETLT:
      case SETULT: condCode = ARMCC.CondCodes.LT; break;
      case SETLE:
      case SETULE: condCode = ARMCC.CondCodes.LE; break;
      case SETNE:
      case SETUNE: condCode = ARMCC.CondCodes.NE; break;
    }
    return new ARMCC.CondCodes[] {condCode, condCode2};
  }

  private static SDValue getARMCmp(SDValue lhs, SDValue rhs,
                                   CondCode cc, SDValue armcc,
                                   SelectionDAG dag, DebugLoc dl) {
    ARMCC.CondCodes condCode = IntCCToARMCC(cc);
    int compareType;
    switch (condCode) {
      default:
        compareType = ARMISD.CMP;
        break;
      case EQ:
      case NE:
        // only use zero flag.
        compareType = ARMISD.CMPZ;
        break;
    }

    SDValue ccConst = dag.getConstant(condCode.ordinal(), new EVT(MVT.i32), false);
    armcc.setNode(ccConst.getNode());
    armcc.setResNo(ccConst.getResNo());
    return dag.getNode(compareType, new EVT(MVT.Glue), lhs, rhs);
  }

  // return true if this is a +0.0
  private static boolean isFloatingPointZero(SDValue op) {
    if (op.getNode() instanceof SDNode.ConstantFPSDNode) {
      return ((SDNode.ConstantFPSDNode)op.getNode()).getValueAPF().isPosZero();
    }
    else if (op.getNode().isExtLoad() || op.getNode().isNONExtLoad()) {
      if (op.getOperand(1).getOpcode() == ARMISD.Wrapper) {
        SDValue wrapperOp = op.getOperand(1).getOperand(0);
        if (wrapperOp.getNode() instanceof SDNode.ConstantPoolSDNode) {
          SDNode.ConstantPoolSDNode cp = (SDNode.ConstantPoolSDNode) wrapperOp.getNode();
          if (cp.getConstantValue() instanceof ConstantFP)
            return ((ConstantFP)cp.getConstantValue()).getValueAPF().isPosZero();
        }
      }
    }
    return false;
  }

  private static boolean canChangeToInt(SDValue op, OutRef<Boolean> seenZero, ARMSubtarget subtarget) {
    SDNode n = op.getNode();
    if (!n.hasOneUse()) {
      // otherwise, it requires moving the value from fp to integer registers.
      return false;
    }
    if (n.getNumValues() == 0)
      return false;
    EVT vt = op.getValueType();
    if (!vt.equals(new EVT(MVT.f32)) && !subtarget.isFPBrccSlow()) {
      // f32 case is generally profitable. f64 case only makes sense when vcmpe +
      // vmrs are very slow, e.g. cortex-a8.
      return false;
    }
    if (isFloatingPointZero(op)) {
      seenZero.set(true);
      return true;
    }
    return op.getNode().isNormalLoad();
  }

  private static SDValue bitcastf32Toi32(SDValue op, SelectionDAG dag) {
    if (isFloatingPointZero(op))
      return dag.getConstant(0, new EVT(MVT.i32), false);

    if (op.getNode() instanceof SDNode.LoadSDNode) {
      SDNode.LoadSDNode ld = (SDNode.LoadSDNode) op.getNode();
      return dag.getLoad(new EVT(MVT.i32), ld.getChain(), ld.getBasePtr(),
          ld.getSrcValue(), ld.getSrcValueOffset(),ld.isVolatile(),ld.getAlignment());
    }
    Util.shouldNotReachHere("Unknown VFP cmp argument!");
    return new SDValue();
  }

  private static SDValue[] expandf64Toi32(SDValue op, SelectionDAG dag) {
    if (isFloatingPointZero(op)) {
      return new SDValue[] {dag.getConstant(0, new EVT(MVT.i32), false),
                             dag.getConstant(0, new EVT(MVT.i32), false)};
    }
    if (op.getNode() instanceof SDNode.LoadSDNode) {
      SDNode.LoadSDNode ld = (SDNode.LoadSDNode) op.getNode();
      SDValue ptr = ld.getBasePtr();
      SDValue retVal1 = dag.getLoad(new EVT(MVT.i32), ld.getChain(), ptr,
          ld.getSrcValue(), ld.getSrcValueOffset(), ld.isVolatile(), ld.getAlignment());
      EVT ptrType = ptr.getValueType();
      int newAlign = Util.minAlign(ld.getAlignment(), 4);
      SDValue newPtr = dag.getNode(ISD.ADD, ptrType, ptr, dag.getConstant(4, ptrType, false));
      SDValue retVal2 = dag.getLoad(new EVT(MVT.i32), newPtr,
          ld.getChain(), ld.getSrcValue(), ld.getSrcValueOffset(),
          ld.isVolatile(), newAlign);
      return new SDValue[] {retVal1, retVal2};
    }

    Util.shouldNotReachHere("Unknown VFP cmp argument!");
    return null;
  }

  private static SDValue optimizeVFPBrcond(SDValue op, SelectionDAG dag) {
    SDValue chain = op.getOperand(0);
    CondCode cc = ((SDNode.CondCodeSDNode)op.getOperand(1).getNode()).getCondition();
    SDValue lhs = op.getOperand(2);
    SDValue rhs = op.getOperand(3);
    SDValue dest = op.getOperand(4);
    DebugLoc dl = op.getDebugLoc();

    OutRef<Boolean> seenZero = new OutRef<>(false);
    ARMSubtarget subtarget = (ARMSubtarget) dag.getMachineFunction().getTarget().getSubtarget();
    if (canChangeToInt(lhs, seenZero, subtarget) && canChangeToInt(rhs, seenZero, subtarget) &&
        // If one of the operand is zero, it's safe to ignore the NaN case since
        // we only care about equality comparisons.
        (seenZero.get() || (dag.isKnownNeverNaN(lhs) && dag.isKnownNeverNaN(rhs)))) {
      // If unsafe fp math optimization is enabled and there are no other uses of
      // the CMP operands, and the condition code is EQ or NE, we can optimize it
      // to an integer comparison.
      if (cc == CondCode.SETOEQ)
        cc = CondCode.SETEQ;
      else if (cc == CondCode.SETUNE)
        cc = CondCode.SETNE;

      SDValue armcc = new SDValue();
      if (lhs.getValueType().equals(new EVT(MVT.f32))) {
        lhs = bitcastf32Toi32(lhs, dag);
        rhs = bitcastf32Toi32(rhs, dag);
        SDValue cmp = getARMCmp(lhs, rhs, cc, armcc, dag, dl);
        SDValue ccr = dag.getRegister(ARMGenRegisterNames.CPSR, new EVT(MVT.i32));
        return dag.getNode(ARMISD.BRCOND, new EVT(MVT.Other), chain, dest, armcc, ccr, cmp);
      }

      SDValue[] lhsParts = expandf64Toi32(lhs, dag);
      SDValue[] rhsParts = expandf64Toi32(rhs, dag);
      ARMCC.CondCodes condCode = IntCCToARMCC(cc);
      armcc = dag.getConstant(condCode.ordinal(), new EVT(MVT.i32), false);
      SDNode.SDVTList vts = dag.getVTList(new EVT(MVT.Other), new EVT(MVT.Glue));
      SDValue[] ops = new SDValue[] {chain, armcc, lhsParts[0], lhsParts[1], rhsParts[0], rhsParts[1], dest};
      return dag.getNode(ARMISD.BCC_i64, vts, ops);
    }
    return new SDValue();
  }

  private static SDValue getVFPCmp(SDValue lhs, SDValue rhs, SelectionDAG dag, DebugLoc dl) {
    SDValue cmp;
    if (!isFloatingPointZero(rhs))
      cmp = dag.getNode(ARMISD.CMPFP, new EVT(MVT.Glue), lhs, rhs);
    else
      cmp = dag.getNode(ARMISD.CMPFPw0, new EVT(MVT.Glue), lhs);
    return dag.getNode(ARMISD.FMSTAT, new EVT(MVT.Glue), cmp);
  }

  private SDValue lowerBR_CC(SDValue op, SelectionDAG dag) {
    // chain, cc, lhs, rhs, dest
    SDValue chain = op.getOperand(0);
    CondCode cc = ((SDNode.CondCodeSDNode)op.getOperand(1).getNode()).getCondition();
    SDValue lhs = op.getOperand(2);
    SDValue rhs = op.getOperand(3);
    SDValue dest = op.getOperand(4);
    DebugLoc dl = op.getDebugLoc();

    if (lhs.getValueType().equals(new EVT(MVT.i32))) {
      SDValue armcc = new SDValue();
      SDValue cmp = getARMCmp(lhs, rhs, cc, armcc, dag, dl);
      SDValue ccr = dag.getRegister(ARMGenRegisterNames.CPSR, new EVT(MVT.i32));
      return dag.getNode(ARMISD.BRCOND, new EVT(MVT.Other), chain, dest, armcc, ccr, cmp);
    }

    Util.assertion(lhs.getValueType().equals(new EVT(MVT.f32)) || lhs.getValueType().equals(new EVT(MVT.f64)));

    if (BackendCmdOptions.EnableUnsafeFPMath.value && (cc == CondCode.SETEQ ||
        cc == CondCode.SETOEQ || cc == CondCode.SETNE || cc == CondCode.SETONE)) {
      SDValue result = optimizeVFPBrcond(op, dag);
      if (result.getNode() != null)
        return result;
    }

    ARMCC.CondCodes condCodes[] = FPCCToARMCC(cc);
    ARMCC.CondCodes condCode1 = condCodes[0], condCode2 = condCodes[1];
    SDValue armcc = dag.getConstant(condCode1.ordinal(), new EVT(MVT.i32), false);
    SDValue cmp = getVFPCmp(lhs, rhs, dag, dl);
    SDValue ccr = dag.getRegister(ARMGenRegisterNames.CPSR, new EVT(MVT.i32));
    SDNode.SDVTList vts = dag.getVTList(new EVT(MVT.Other), new EVT(MVT.Glue));
    SDValue[] ops = new SDValue[] {chain, dest, armcc, ccr, cmp};
    SDValue res = dag.getNode(ARMISD.BRCOND, vts, ops);
    if (condCode2 != ARMCC.CondCodes.AL) {
      armcc = dag.getConstant(condCode2.ordinal(), new EVT(MVT.i32), false);
      ops = new SDValue[] {res, dest, armcc, ccr, res.getValue(1)};
      res = dag.getNode(ARMISD.BRCOND, vts, ops);
    }
    return res;
  }
  private SDValue lowerFCOPYSIGN(SDValue op, SelectionDAG dag)  {
    Util.shouldNotReachHere();
    return null;
  }
  private SDValue lowerRETURNADDR(SDValue op, SelectionDAG dag)  {
    Util.shouldNotReachHere();
    return null;
  }
  private SDValue lowerFRAMEADDR(SDValue op, SelectionDAG dag)  {
    Util.shouldNotReachHere();
    return null;
  }
  private SDValue lowerShiftRightParts(SDValue op, SelectionDAG dag)  {
    Util.assertion(op.getNumOperands() == 3, "Not a double-shift");
    EVT vt = op.getValueType();
    int vtBits = vt.getSizeInBits();
    DebugLoc dl = op.getDebugLoc();
    SDValue shOpLo = op.getOperand(0);
    SDValue shOpHi = op.getOperand(1);
    SDValue amt = op.getOperand(2);
    SDValue armcc = new SDValue();

    Util.assertion(op.getOpcode() == ISD.SRA_PARTS || op.getOpcode() == ISD.SRL_PARTS, "expected sra and srl");
    int opc = op.getOpcode() == ISD.SRA_PARTS ? ISD.SRA : ISD.SRL;

    SDValue revShAmt = dag.getNode(ISD.SUB, new EVT(MVT.i32), dag.getConstant(vtBits, new EVT(MVT.i32), false),
        amt);
    SDValue tmp1 = dag.getNode(ISD.SRL, vt, shOpLo, revShAmt);
    SDValue extraShAmt = dag.getNode(ISD.SUB, new EVT(MVT.i32), amt, dag.getConstant(vtBits, new EVT(MVT.i32), false));

    SDValue tmp2 = dag.getNode(ISD.SHL, vt, shOpHi, amt);
    SDValue falseVal = dag.getNode(ISD.OR, vt, tmp1, tmp2);
    SDValue trueVal = dag.getNode(opc, vt, shOpHi, extraShAmt);

    SDValue ccr = dag.getRegister(ARMGenRegisterNames.CPSR, new EVT(MVT.i32));
    SDValue cmp = getARMCmp(extraShAmt, dag.getConstant(0, new EVT(MVT.i32), false), CondCode.SETGE,
        armcc, dag, dl);

    SDValue lo = dag.getNode(opc, vt, shOpHi, amt);
    SDValue hi = dag.getNode(ARMISD.CMOV, vt, falseVal, trueVal, armcc, ccr, cmp);

    return dag.getMergeValues(new SDValue[] {lo, hi});
  }

  /**
   * Lower SHL_PARTS which returns two i32 values and take a 2 x i32 value to
   * shift plus a shift amount.
   * @param op
   * @param dag
   * @return
   */
  private SDValue lowerShiftLeftParts(SDValue op, SelectionDAG dag)  {
    Util.assertion(op.getNumOperands() == 3, "Not a double-shift");
    EVT vt = op.getValueType();
    int vtBits = vt.getSizeInBits();
    DebugLoc dl = op.getDebugLoc();
    SDValue shOpLo = op.getOperand(0);
    SDValue shOpHi = op.getOperand(1);
    SDValue amt = op.getOperand(2);
    SDValue armcc = new SDValue();


    Util.assertion(op.getOpcode() == ISD.SHL_PARTS);
    SDValue revShAmt = dag.getNode(ISD.SUB, new EVT(MVT.i32), dag.getConstant(vtBits, new EVT(MVT.i32), false),
        amt);
    SDValue tmp1 = dag.getNode(ISD.SRL, vt, shOpLo, revShAmt);
    SDValue extraShAmt = dag.getNode(ISD.SUB, new EVT(MVT.i32), amt, dag.getConstant(vtBits, new EVT(MVT.i32), false));
    SDValue tmp2 = dag.getNode(ISD.SHL, vt, shOpHi, amt);
    SDValue tmp3 = dag.getNode(ISD.SHL, vt, shOpLo, extraShAmt);

    SDValue falseVal = dag.getNode(ISD.OR, vt, tmp1, tmp2);
    SDValue ccr = dag.getRegister(ARMGenRegisterNames.CPSR, new EVT(MVT.i32));
    SDValue cmp = getARMCmp(extraShAmt, dag.getConstant(0, new EVT(MVT.i32), false), CondCode.SETGE,
        armcc, dag, dl);
    SDValue lo = dag.getNode(ISD.SHL, vt, shOpLo, amt);
    SDValue hi = dag.getNode(ARMISD.CMOV, vt, falseVal, tmp3, armcc, ccr, cmp);

    return dag.getMergeValues(new SDValue[] {lo, hi});
  }

  private SDValue lowerBUILD_VECTOR(SDValue op, SelectionDAG dag)  {
    Util.shouldNotReachHere();
    return null;
  }
  private SDValue lowerVASTART(SDValue op, SelectionDAG dag)  {
    // store the variable argument stack offset to the pointer operand of {@linkplain ISD#VASTART}
    SDValue chain = op.getOperand(0);
    SDValue pointer = op.getOperand(1);
    Value srcValue = ((SDNode.SrcValueSDNode)op.getOperand(2).getNode()).getValue();
    MachineFunction mf = dag.getMachineFunction();
    ARMFunctionInfo afi = (ARMFunctionInfo) mf.getFunctionInfo();
    int fi = afi.getVarArgsFrameIndex();
    SDValue fin = dag.getFrameIndex(fi, new EVT(getPointerTy()), false);
    return dag.getStore(chain, fin, pointer, srcValue, 0, false, 0);
  }

  private SDValue lowerMEMBARRIER(SDValue op, SelectionDAG dag)  {
    DebugLoc dl = op.getDebugLoc();
    if (!subtarget.hasDataBarrier()) {
      // Some ARMv6 cpus can support data barriers with an mcr instruction.
      // Thumb1 and pre-v6 ARM mode use a libcall instead and should never get
      // here.
      return dag.getNode(ARMISD.MEMBARRIER_MCR, new EVT(MVT.Other), op.getOperand(0),
          dag.getConstant(0, new EVT(MVT.i32), false));
    }
    SDValue op5 = op.getOperand(5);
    boolean isDeviceBarrier = ((SDNode.ConstantSDNode)op5.getNode()).getZExtValue() != 0;
    long isLL = ((SDNode.ConstantSDNode)op.getOperand(1).getNode()).getZExtValue();
    long isLS = ((SDNode.ConstantSDNode)op.getOperand(2).getNode()).getZExtValue();
    boolean isOnlyStoreBarrier = isLL == 0 && isLS == 0;

    int dmbOpt;
    if (isDeviceBarrier)
      dmbOpt = isOnlyStoreBarrier ? ARM_MB.ST : ARM_MB.SY;
    else
      dmbOpt = isOnlyStoreBarrier ? ARM_MB.ISHST : ARM_MB.ISH;
    return dag.getNode(ARMISD.MEMBARRIER, new EVT(MVT.Other), op.getOperand(0),
        dag.getConstant(dmbOpt, new EVT(MVT.i32), false));
  }

  private SDValue lowerATOMIC_FENCE(SDValue op, SelectionDAG dag)  {
    DebugLoc dl = op.getDebugLoc();
    if (!subtarget.hasDataBarrier()) {
      // Some ARMv6 cpus can support data barriers with an mcr instruction.
      // Thumb1 and pre-v6 ARM mode use a libcall instead and should never get
      // here.
      return dag.getNode(ARMISD.MEMBARRIER_MCR, new EVT(MVT.Other), op.getOperand(0),
          dag.getConstant(0, new EVT(MVT.i32), false));
    }
    return dag.getNode(ARMISD.MEMBARRIER, new EVT(MVT.Other), op.getOperand(0),
        dag.getConstant(ARM_MB.ISH, new EVT(MVT.i32), false));
  }

  /**
   * Usually, arm use preload instruction to achieve prefetch.
   * @param op
   * @param dag
   * @return
   */
  private SDValue lowerPREFETCH(SDValue op, SelectionDAG dag)  {
    // ARM pre v5TE and Thumb1 does not have preload instructions.
    if (!(subtarget.isThumb2() || (!subtarget.isThumb1Only() && subtarget.hasV5TEOps()))) {
      // just preserve the chain.
      return op.getOperand(0);
    }

    DebugLoc dl = op.getDebugLoc();
    boolean isRead = (~((SDNode.ConstantSDNode)op.getOperand(2).getNode()).getZExtValue() & 1 ) != 0;
    if (isRead && (!subtarget.hasV7Ops() || !subtarget.hasMPExtension())) {
      // ARMv7 with MP extension has PLDW.
      return op.getOperand(0);
    }

    boolean isData = ((SDNode.ConstantSDNode)op.getOperand(4).getNode()).getZExtValue() != 0;
    if (subtarget.isThumb()) {
      // invert the bits.
      isRead = !isRead;
      isData = !isData;
    }
    return dag.getNode(ARMISD.PRELOAD, new EVT(MVT.Other), op.getOperand(0),
        op.getOperand(1), dag.getConstant(isRead, new EVT(MVT.i32), false),
        dag.getConstant(isData, new EVT(MVT.i32), false));
  }

  private static SDValue lowerVectorINT_TO_FP(SDValue op, SelectionDAG dag) {
    EVT vt = op.getValueType();
    DebugLoc dl = op.getDebugLoc();
    Util.assertion(op.getOperand(0).getValueType().equals(new EVT(MVT.v4i16)),
        "invalid type for custom lowring!");
    if (!vt.equals(new EVT(MVT.v4i16)))
      return dag.unrollVectorOp(op.getNode());

    int castOpc;
    int opc;
    switch (op.getOpcode()) {
      default:
        Util.assertion("invalid opc!");
        castOpc = 0;
        opc = 0;
        break;
      case ISD.SINT_TO_FP:
        castOpc = ISD.SIGN_EXTEND;
        opc = ISD.SINT_TO_FP;
        break;
      case ISD.UINT_TO_FP:
        castOpc = ISD.ZERO_EXTEND;
        opc = ISD.UINT_TO_FP;
        break;
    }
    op = dag.getNode(castOpc, new EVT(MVT.v4i16), op.getOperand(0));
    return dag.getNode(opc, vt, op);
  }

  private SDValue lowerINT_TO_FP(SDValue op, SelectionDAG dag) {
    EVT vt = op.getValueType();
    if (vt.isVector())
      return lowerVectorINT_TO_FP(op, dag);

    DebugLoc dl = op.getDebugLoc();
    int opc = 0;
    switch (op.getOpcode()) {
      default:
        Util.assertion("Invalid opcode!");
        break;
      case ISD.SINT_TO_FP:
        opc = ARMISD.SITOF;
        break;
      case ISD.UINT_TO_FP:
        opc = ARMISD.UITOF;
        break;
    }
    op = dag.getNode(ISD.BIT_CONVERT, new EVT(MVT.f32), op.getOperand(0));
    return dag.getNode(opc, vt, op);
  }
  private SDValue lowerFP_TO_INT(SDValue op, SelectionDAG dag)  {
    int opc = 0;
    EVT vt = op.getValueType();
    switch (op.getOpcode()) {
      default:
        Util.assertion("Invalid opcode");
        break;
      case ISD.FP_TO_SINT:
        opc = ARMISD.FTOSI;
        break;
      case ISD.FP_TO_UINT:
        opc = ARMISD.FTOUI;
        break;
    }
    return dag.getNode(opc, vt, op.getOperand(0));
  }

  private SDValue lowerINTRINSIC_WO_CAHIN(SDValue op, SelectionDAG dag)  {
    Util.shouldNotReachHere();
    return null;
  }
  private SDValue expandBITCAST(SDNode n, SelectionDAG dag)  {
    DebugLoc dl = n.getDebugLoc();
    SDValue op = n.getOperand(0);

    EVT srcVT = op.getValueType();
    EVT dstVT = n.getValueType(0);
    Util.assertion(srcVT.equals(new EVT(MVT.i64)) || dstVT.equals(new EVT(MVT.i64)),
        "expandBITCAST called for non-i64 type!");

    // turn i64->f64 into VMOVDRR.
    if (srcVT.equals(new EVT(MVT.i64)) && isTypeLegal(dstVT)) {
      // first separate the i64 to double i32
      // then merge it to a f64
      // then bit cast.
      SDValue lo = dag.getNode(ISD.EXTRACT_ELEMENT, new EVT(MVT.i32), op,
          dag.getConstant(0, new EVT(MVT.i32), false));
      SDValue hi = dag.getNode(ISD.EXTRACT_ELEMENT, new EVT(MVT.i32), op,
          dag.getConstant(1, new EVT(MVT.i32), false));
      return dag.getNode(ISD.BIT_CONVERT, dstVT, dag.getNode(ARMISD.VMOVDRR, new EVT(MVT.f64),
          lo, hi));
    }

    // turn f64->i64 into VMOVRRD.
    if (dstVT.equals(new EVT(MVT.i64)) && isTypeLegal(srcVT)) {
      SDValue split = dag.getNode(ARMISD.VMOVRRD, dag.getVTList(new EVT(MVT.i32), new EVT(MVT.i32)),
          op);
      // merge it
      return dag.getNode(ISD.BUILD_PAIR, new EVT(MVT.i64), split, split.getValue(1));
    }
    return new SDValue();
  }

  private SDValue lowerShift(SDNode n, SelectionDAG dag)  {
    EVT vt = n.getValueType(0);
    DebugLoc dl = n.getDebugLoc();

    if (!vt.isVector())
      return new SDValue();

    // we must use neon to support vector shifter.
    Util.assertion(subtarget.hasNEON(), "unexpected vector shift!");

    if (n.getOpcode() == ISD.SHL)
      return dag.getNode(ISD.INTRINSIC_WO_CHAIN, vt,
          dag.getConstant(Intrinsic.ID.arm_neon_vshiftu.ordinal(), new EVT(MVT.i32), false),
          n.getOperand(0), n.getOperand(1));

    Util.assertion(n.getOpcode() == ISD.SRA || n.getOpcode() == ISD.SRL);
    EVT shiftVT = n.getOperand(1).getValueType();
    SDValue negatedCount = dag.getNode(ISD.SUB, shiftVT, getZeroVector(shiftVT, dag),
        n.getOperand(1));
    Intrinsic.ID vshiftInt = n.getOpcode() == ISD.SRA ? Intrinsic.ID.arm_neon_vshifts :
        Intrinsic.ID.arm_neon_vshiftu;

    return dag.getNode(ISD.INTRINSIC_WO_CHAIN, vt,
        dag.getConstant(vshiftInt.ordinal(), new EVT(MVT.i32), false), n.getOperand(0), negatedCount);
  }

  private static SDValue getZeroVector(EVT vt, SelectionDAG dag) {
    Util.assertion(vt.isVector(), "expect vector type");
    SDValue encodedVal = dag.getTargetConstant(0, new EVT(MVT.i32));
    EVT vmovVT = vt.is128BitVector() ? new EVT(MVT.v4i32) : new EVT(MVT.v2i32);
    SDValue vmov = dag.getNode(ARMISD.VMOVIMM, vmovVT, encodedVal);
    return dag.getNode(ISD.BIT_CONVERT, vt, vmov);
  }

  private SDValue lowerCTTZ(SDNode n, SelectionDAG dag)  {
      EVT vt = n.getValueType(0);
      DebugLoc dl = n.getDebugLoc();

      if (!subtarget.hasV6T2Ops())
        return new SDValue();

      SDValue rbit = dag.getNode(ARMISD.RBIT, vt, n.getOperand(0));
      return dag.getNode(ISD.CTLZ, vt, rbit);
  }

  private SDValue lowerVSETCC(SDValue op, SelectionDAG dag)  {
    Util.shouldNotReachHere();
    return null;
  }
  private SDValue lowerVECTOR_SHUFFLE(SDValue op, SelectionDAG dag)  {
    Util.shouldNotReachHere();
    return null;
  }
  private SDValue lowerEXTRACT_VECTOR_ELT(SDValue op, SelectionDAG dag)  {
    // extract_vector_elt only valid for immediate index.
    SDValue lane = op.getOperand(1);
    if (!(lane.getNode() instanceof SDNode.ConstantSDNode))
      return new SDValue();

    SDValue vec = op.getOperand(0);
    if (op.getValueType().equals(new EVT(MVT.i32)) &&
        vec.getValueType().getVectorElementType().getSizeInBits() < 32) {
      DebugLoc dl = op.getDebugLoc();
      return dag.getNode(ARMISD.VGETLANEu, new EVT(MVT.i32), vec, lane);
    }
    return op;
  }

  private SDValue lowerCONCAT_VECTORS(SDValue op, SelectionDAG dag)  {
    // The only time a CONCAT_VECTORS operation can have legal types is when
    // two 64-bit vectors are concatenated to a 128-bit vector.
    Util.assertion(op.getValueType().is128BitVector() && op.getNumOperands() == 2, "unexpected CONCAT_VECTORS");

    DebugLoc dl = op.getDebugLoc();
    SDValue val = dag.getUNDEF(new EVT(MVT.v2f64));
    SDValue op0 = op.getOperand(0);
    SDValue op1 = op.getOperand(1);
    if (op0.getOpcode() != ISD.UNDEF)
      val = dag.getNode(ISD.INSERT_VECTOR_ELT, new EVT(MVT.v2f64), val,
          dag.getNode(ISD.BIT_CONVERT, new EVT(MVT.f64), op0), dag.getIntPtrConstant(0));
    if (op1.getOpcode() != ISD.UNDEF)
      val = dag.getNode(ISD.INSERT_VECTOR_ELT, new EVT(MVT.v2f64), val,
          dag.getNode(ISD.BIT_CONVERT, new EVT(MVT.f64), op1), dag.getIntPtrConstant(1));

    return dag.getNode(ISD.BIT_CONVERT, op.getValueType(), val);
  }

  private SDValue lowerFLT_ROUNDS(SDValue op, SelectionDAG dag)  {
    // The rounding mode is in bits 23:22 of the FPSCR.
    // The ARM rounding mode value to FLT_ROUNDS mapping is 0->1, 1->2, 2->3, 3->0
    // The formula we use to implement this is (((FPSCR + 1 << 22) >> 22) & 3)
    // so that the shift + and get folded into a bitfield extract.
    SDValue fpcsr = dag.getNode(ISD.INTRINSIC_WO_CHAIN, new EVT(MVT.i32),
        dag.getConstant(Intrinsic.ID.arm_get_fpscr.ordinal(), new EVT(MVT.i32), false));
    SDValue fltRounds = dag.getNode(ISD.ADD, new EVT(MVT.i32), fpcsr,
        dag.getConstant(1<<22, new EVT(MVT.i32), false));
    SDValue rmode = dag.getNode(ISD.SRL, new EVT(MVT.i32), fltRounds,
        dag.getConstant(22, new EVT(MVT.i32), false));

    return dag.getNode(ISD.ADD, new EVT(MVT.i32), rmode, dag.getConstant(3, new EVT(MVT.i32), false));
  }

  private SDValue lowerMUL(SDValue op, SelectionDAG dag)  {
    Util.shouldNotReachHere();
    return null;
  }
  private SDValue lowerSDIV(SDValue op, SelectionDAG dag)  {
    Util.shouldNotReachHere();
    return null;
  }
  private SDValue lowerUDIV(SDValue op, SelectionDAG dag)  {
    Util.shouldNotReachHere();
    return null;
  }
  private SDValue lowerADDC_ADDE_SUBC_SUBE(SDValue op, SelectionDAG dag)  {
    EVT vt = op.getValueType();
    SDNode.SDVTList vts = dag.getVTList(vt, new EVT(MVT.i32));

    int opc = 0;
    boolean extraOp = false;
    switch (op.getOpcode()) {
      default:
        Util.assertion("invalid opc");
        break;
      case ISD.ADDC: opc = ARMISD.ADDC; break;
      case ISD.ADDE: opc = ARMISD.ADDE; extraOp = true; break;
      case ISD.SUBC: opc = ARMISD.SUBC; break;
      case ISD.SUBE: opc = ARMISD.SUBE; extraOp = true; break;
    }

    if (!extraOp)
      return dag.getNode(opc, vts, op.getOperand(0), op.getOperand(1));
    return dag.getNode(opc, vts, op.getOperand(0), op.getOperand(1), op.getOperand(2));
  }

  private static SDValue lowerAtomicLoadStore(SDValue op, SelectionDAG dag) {
      Util.shouldNotReachHere();
      return null;
  }

  private SDValue reconstructShuffle(SDValue op, SelectionDAG dag)  {
    Util.shouldNotReachHere();
    return null;
  }

  private void varArgStyleRegisters(CCState ccInfo, SelectionDAG dag,
                                    DebugLoc dl, SDValue chain, int argOffset)  {
    Util.shouldNotReachHere();
  }

  private int[] computeRegArea(CCState ccInfo, MachineFunction mf)  {
    Util.shouldNotReachHere();
    return null;
  }

  @Override
  public SDValue lowerOperation(SDValue op, SelectionDAG dag) {
    switch (op.getOpcode()) {
      default:
        Util.shouldNotReachHere("Don't know how to custom lower this!");
        return null;
      case ISD.ConstantPool: return lowerConstantPool(op, dag);
      case ISD.BlockAddress: return lowerBlockAddress(op, dag);
      case ISD.GlobalAddress:
        return subtarget.isTargetWindows() ? lowerGlobalAddressDarwin(op, dag) :
            lowerGlobalAddressELF(op, dag);
      case ISD.GlobalTLSAddress: return lowerGlobalTLSAddress(op, dag);
      case ISD.SELECT: return lowerSELECT(op, dag);
      case ISD.SELECT_CC: return lowerSELECT_CC(op, dag);
      case ISD.BR_CC: return lowerBR_CC(op, dag);
      case ISD.BR_JT: return lowerBR_JT(op, dag);
      case ISD.VASTART: return lowerVASTART(op, dag);
      case ISD.MEMBARRIER: return lowerMEMBARRIER(op, dag);
      case ISD.ATOMIC_FENCE: return lowerATOMIC_FENCE(op, dag);
      case ISD.PREFETCH: return lowerPREFETCH(op, dag);
      case ISD.SINT_TO_FP:
      case ISD.UINT_TO_FP:
        return lowerINT_TO_FP(op, dag);
      case ISD.FP_TO_SINT:
      case ISD.FP_TO_UINT:
        return lowerFP_TO_INT(op, dag);
      case ISD.FCOPYSIGN: return lowerFCOPYSIGN(op, dag);
      case ISD.RETURNADDR: return lowerRETURNADDR(op, dag);
      case ISD.FRAMEADDR: return lowerFRAMEADDR(op, dag);
      case ISD.GLOBAL_OFFSET_TABLE: return lowerGLOBAL_OFFSET_TABLE(op, dag);
      case ISD.INTRINSIC_WO_CHAIN: return lowerINTRINSIC_WO_CAHIN(op, dag);

      case ISD.BIT_CONVERT: return expandBITCAST(op.getNode(), dag);
      case ISD.SHL:
      case ISD.SRL:
      case ISD.SRA:
        return lowerShift(op.getNode(), dag);
      case ISD.SHL_PARTS:
        return lowerShiftLeftParts(op, dag);
      case ISD.SRL_PARTS:
      case ISD.SRA_PARTS:
        return lowerShiftRightParts(op, dag);
      case ISD.CTTZ:
        return lowerCTTZ(op.getNode(), dag);
      case ISD.SETCC:
        return lowerVSETCC(op, dag);
      case ISD.BUILD_VECTOR:
        return lowerBUILD_VECTOR(op, dag);
      case ISD.VECTOR_SHUFFLE:
        return lowerVECTOR_SHUFFLE(op, dag);
      case ISD.EXTRACT_VECTOR_ELT:
        return lowerEXTRACT_VECTOR_ELT(op, dag);
      case ISD.CONCAT_VECTORS:
        return lowerCONCAT_VECTORS(op, dag);
      case ISD.FLT_ROUNDS_:
        return lowerFLT_ROUNDS(op, dag);
      case ISD.MUL: return lowerMUL(op, dag);
      case ISD.SDIV: return lowerSDIV(op, dag);
      case ISD.UDIV: return lowerUDIV(op, dag);
      case ISD.ADDC:
      case ISD.ADDE:
      case ISD.SUBC:
      case ISD.SUBE:
        return lowerADDC_ADDE_SUBC_SUBE(op, dag);
      case ISD.ATOMIC_LOAD:
      case ISD.ATOMIC_STORE:
        return lowerAtomicLoadStore(op, dag);
    }
  }


  @Override
  public void replaceNodeResults(SDNode n, ArrayList<SDValue> results, SelectionDAG dag) {
    SDValue res = null;
    switch (n.getOpcode()) {
      default:
        Util.shouldNotReachHere("Don't know how to custom expand this!");
        break;
      case ISD.BIT_CONVERT:
        res = expandBITCAST(n, dag);
        break;
      case ISD.SRL:
      case ISD.SRA:
        res = expand64BitShift(n, dag);
        break;
      case ISD.ATOMIC_LOAD_ADD:
        replaceATOMIC_OP_64(n, results, dag, ARMISD.ATOMADD64_DAG);
        return;
      case ISD.ATOMIC_LOAD_AND:
        replaceATOMIC_OP_64(n, results, dag, ARMISD.ATOMAND64_DAG);
        return;
      case ISD.ATOMIC_LOAD_NAND:
        replaceATOMIC_OP_64(n, results, dag, ARMISD.ATOMNAND64_DAG);
        return;
      case ISD.ATOMIC_LOAD_OR:
        replaceATOMIC_OP_64(n, results, dag, ARMISD.ATOMOR64_DAG);
        return;
      case ISD.ATOMIC_LOAD_SUB:
        replaceATOMIC_OP_64(n, results, dag, ARMISD.ATOMSUB64_DAG);
        return;
      case ISD.ATOMIC_LOAD_XOR:
        replaceATOMIC_OP_64(n, results, dag, ARMISD.ATOMXOR64_DAG);
        return;
      case ISD.ATOMIC_SWAP:
        replaceATOMIC_OP_64(n, results, dag, ARMISD.ATOMSWAP64_DAG);
        return;
      case ISD.ATOMIC_CMP_SWAP:
        replaceATOMIC_OP_64(n, results, dag, ARMISD.ATOMCMPXCHG64_DAG);
        return;
    }
    if (res != null && res.getNode() != null)
      results.add(res);
  }

  private SDValue expand64BitShift(SDNode n, SelectionDAG dag)  {
    EVT vt = n.getValueType(0);
    DebugLoc dl = n.getDebugLoc();

    if (!vt.equals(new EVT(MVT.i64)))
      return new SDValue();

    int opc = n.getOpcode();
    Util.assertion(opc == ISD.SRL || opc == ISD.SRA, "unknown shift to lower!");

    // We only lower SRA, SRL of 1 here, all others use generic lowering.
    if (!(n.getOperand(1).getNode() instanceof SDNode.ConstantSDNode) ||
        ((SDNode.ConstantSDNode)n.getOperand(1).getNode()).getZExtValue() != 1)
      return new SDValue();

    // if we are in thumb mode, don't have RRX.
    if (subtarget.isThumb1Only()) return new SDValue();

    SDValue lo = dag.getNode(ISD.EXTRACT_ELEMENT, new EVT(MVT.i32), n.getOperand(0),
        dag.getConstant(0, new EVT(MVT.i32), false));
    SDValue hi = dag.getNode(ISD.EXTRACT_ELEMENT, new EVT(MVT.i32), n.getOperand(0),
        dag.getConstant(1, new EVT(MVT.i32), false));

    opc = opc == ISD.SRL ? ARMISD.SRL_FLAG : ARMISD.SRA_FLAG;
    hi = dag.getNode(opc, dag.getVTList(new EVT(MVT.i32), new EVT(MVT.Glue)), hi);
    lo = dag.getNode(ARMISD.RRX, new EVT(MVT.i32), lo, hi.getValue(1));

    return dag.getNode(ISD.BUILD_PAIR, new EVT(MVT.i64), lo, hi);
  }

  /**
   * Replace those ISD.ATMOC_* operations with a serial of supported
   * operations.
   * @param n
   * @param results
   * @param dag
   * @param newOp
   */
  private static void replaceATOMIC_OP_64(SDNode n,
                                          ArrayList<SDValue> results,
                                          SelectionDAG dag,
                                          int newOp) {
    DebugLoc dl = n.getDebugLoc();
    Util.assertion(n.getValueType(0).equals(new EVT(MVT.i64)), "Only know how to expand i64 atomics");

    ArrayList<SDValue> ops = new ArrayList<>();
    ops.add(n.getOperand(0)); // chain
    ops.add(n.getOperand(1)); // ptr.
    // low part of val1.
    ops.add(dag.getNode(ISD.EXTRACT_ELEMENT, new EVT(MVT.i32),
        n.getOperand(2), dag.getIntPtrConstant(0)));
    // high part of val1.
    ops.add(dag.getNode(ISD.EXTRACT_ELEMENT, new EVT(MVT.i32),
        n.getOperand(2), dag.getIntPtrConstant(1)));

    if (newOp == ARMISD.ATOMCMPXCHG64_DAG) {
      // low part of val2
      ops.add(dag.getNode(ISD.EXTRACT_ELEMENT, new EVT(MVT.i32),
          n.getOperand(3), dag.getIntPtrConstant(0)));
      // high part of val2
      ops.add(dag.getNode(ISD.EXTRACT_ELEMENT, new EVT(MVT.i32),
          n.getOperand(3), dag.getIntPtrConstant(1)));
    }

    SDNode.SDVTList vts = dag.getVTList(new EVT(MVT.i32), new EVT(MVT.i32), new EVT(MVT.Other));
    SDValue result = dag.getMemIntrinsicNode(newOp,
        vts, ops, new EVT(MVT.i64), ((SDNode.MemSDNode)n).getMemOperand());
    SDValue[] opsFI = {result.getValue(0), result.getValue(1)};
    results.add(dag.getNode(ISD.BUILD_VECTOR, new EVT(MVT.i64), opsFI));
    results.add(result.getValue(2));
  }
}
