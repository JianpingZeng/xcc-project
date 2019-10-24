package backend.target.arm;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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
import backend.mc.MCRegisterClass;
import backend.support.CallingConv;
import backend.support.LLVMContext;
import backend.target.TargetLowering;
import backend.target.TargetLoweringObjectFile;
import backend.target.TargetMachine;
import backend.value.Function;
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
    public ARMCCState(CallingConv cc, boolean isVarArg,
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
    return new ARMELFTargetObjectFile();
  }

  private ARMSubtarget subtarget;
  private ARMRegisterInfo regInfo;

  public ARMTargetLowering(ARMTargetMachine tm) {
    super(tm, createTLOF(tm));
    subtarget = tm.getSubtarget();
    regInfo = subtarget.getRegisterInfo();

    if (subtarget.isTargetDarwin()) {
      // Don't have these.
      setLibCallName(RTLIB.UINTTOFP_I64_F32, null);
      setLibCallName(RTLIB.UINTTOFP_I64_F64, null);

      // Uses VFP for Thumb libfuncs if available.
      if (subtarget.isThumb() && subtarget.hasVFPV2()) {
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

    addRegisterClass(MVT.i32, ARMGenRegisterInfo.IntRegsRegisterClass);
    /*if (!UseSoftFloat && subtarget.hasVFP2() && !subtarget.isThumb()) {
      addRegisterClass(MVT.f32, ARM.SPRRegisterClass);
      addRegisterClass(MVT.f64, ARM.DPRRegisterClass);
    }*/

    // ARM does not have f32 extending load.
    setLoadExtAction(LoadExtType.EXTLOAD, new MVT(MVT.f32), Expand);

    // ARM supports all 4 flavors of integer indexed load / store.
    for (int i = 0,  e = MemIndexedMode.values().length; i < e; ++i) {
      MemIndexedMode im = MemIndexedMode.values()[i];
      if (im == MemIndexedMode.LAST_INDEXED_MODE) continue;

      setIndexedLoadAction(im,  MVT.i1,  Legal);
      setIndexedLoadAction(im,  MVT.i8,  Legal);
      setIndexedLoadAction(im,  MVT.i16, Legal);
      setIndexedLoadAction(im,  MVT.i32, Legal);
      setIndexedStoreAction(im, MVT.i1,  Legal);
      setIndexedStoreAction(im, MVT.i8,  Legal);
      setIndexedStoreAction(im, MVT.i16, Legal);
      setIndexedStoreAction(im, MVT.i32, Legal);
    }

    // i64 operation support.
    if (subtarget.isThumb()) {
      setOperationAction(ISD.MUL,     MVT.i64, Expand);
      setOperationAction(ISD.MULHU,   MVT.i32, Expand);
      setOperationAction(ISD.MULHS,   MVT.i32, Expand);
    } else {
      setOperationAction(ISD.MUL,     MVT.i64, Custom);
      setOperationAction(ISD.MULHU,   MVT.i32, Custom);
      if (!subtarget.hasV6Ops())
        setOperationAction(ISD.MULHS, MVT.i32, Custom);
    }
    setOperationAction(ISD.SHL_PARTS, MVT.i32, Expand);
    setOperationAction(ISD.SRA_PARTS, MVT.i32, Expand);
    setOperationAction(ISD.SRL_PARTS, MVT.i32, Expand);
    setOperationAction(ISD.SRL,       MVT.i64, Custom);
    setOperationAction(ISD.SRA,       MVT.i64, Custom);

    // ARM does not have ROTL.
    setOperationAction(ISD.ROTL,  MVT.i32, Expand);
    setOperationAction(ISD.CTTZ , MVT.i32, Expand);
    setOperationAction(ISD.CTPOP, MVT.i32, Expand);
    if (!subtarget.hasV5TOps() || subtarget.isThumb())
      setOperationAction(ISD.CTLZ, MVT.i32, Expand);

    // Only ARMv6 has BSWAP.
    if (!subtarget.hasV6Ops())
      setOperationAction(ISD.BSWAP, MVT.i32, Expand);

    // These are expanded into libcalls.
    setOperationAction(ISD.SDIV,  MVT.i32, Expand);
    setOperationAction(ISD.UDIV,  MVT.i32, Expand);
    setOperationAction(ISD.SREM,  MVT.i32, Expand);
    setOperationAction(ISD.UREM,  MVT.i32, Expand);

    // Support label based line numbers.
    setOperationAction(ISD.DEBUG_LOC, MVT.Other, Expand);

    setOperationAction(ISD.GlobalAddress, MVT.i32,   Custom);
    setOperationAction(ISD.ConstantPool,  MVT.i32,   Custom);
    setOperationAction(ISD.GLOBAL_OFFSET_TABLE, MVT.i32, Custom);
    setOperationAction(ISD.GlobalTLSAddress, MVT.i32, Custom);

    // Use the default implementation.
    setOperationAction(ISD.VASTART           , MVT.Other, Expand);
    setOperationAction(ISD.VAARG             , MVT.Other, Expand);
    setOperationAction(ISD.VACOPY            , MVT.Other, Expand);
    setOperationAction(ISD.VAEND             , MVT.Other, Expand);
    setOperationAction(ISD.STACKSAVE,          MVT.Other, Expand);
    setOperationAction(ISD.STACKRESTORE,       MVT.Other, Expand);
    setOperationAction(ISD.DYNAMIC_STACKALLOC, MVT.i32  , Expand);

    if (!subtarget.hasV6Ops()) {
      setOperationAction(ISD.SIGN_EXTEND_INREG, MVT.i16, Expand);
      setOperationAction(ISD.SIGN_EXTEND_INREG, MVT.i8,  Expand);
    }
    setOperationAction(ISD.SIGN_EXTEND_INREG, MVT.i1, Expand);

    /*if (!UseSoftFloat && subtarget.hasVFP2() && !subtarget.isThumb())
      // Turn f64->i64 into FMRRD iff target supports vfp2.
      setOperationAction(ISD.BIT_CONVERT, MVT.i64, Custom);*/

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
    setOperationAction(ISD.FCOPYSIGN, MVT.f64, Custom);
    setOperationAction(ISD.FCOPYSIGN, MVT.f32, Custom);

    // int <-> fp are custom expanded into bit_convert + ARMISD ops.
    setOperationAction(ISD.SINT_TO_FP, MVT.i32, Custom);
    setOperationAction(ISD.UINT_TO_FP, MVT.i32, Custom);
    setOperationAction(ISD.FP_TO_UINT, MVT.i32, Custom);
    setOperationAction(ISD.FP_TO_SINT, MVT.i32, Custom);

    setStackPointerRegisterToSaveRestore(ARMGenRegisterNames.SP);

    //setSchedulingPreference(SchedulingForRegPressure);
    computeRegisterProperties();
  }

  @Override
  public MachineFunctionInfo createMachineFunctionInfo(MachineFunction mf) {
    return null;
  }

  @Override
  public int getFunctionAlignment(Function fn) {
    return 0;
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
    ARMCCState ccInfo = new ARMCCState(callingConv, varArg, getTargetMachine(),
        argLocs, dag.getContext(), CCState.ParmContext.Prologue);
    ccInfo.analyzeFormalArguments(ins, ccAssignFnForNode(callingConv, false, varArg));

    ArrayList<SDValue> argValues = new ArrayList<>();
    int lastInsIndex = -1;
    SDValue argValue = null;
    for (int i = 0,  e = argLocs.size(); i < e; i++) {
      CCValAssign va = argLocs.get(i);
      if (va.isRegLoc()) {
        EVT regVT = va.getLocVT();
        if (va.needsCustom()) {
          Util.shouldNotReachHere("custom cc is not supported yet!");
        }
        MCRegisterClass rc;
        if (!regVT.equals(new EVT(MVT.i32)))
          Util.shouldNotReachHere("float point is not supported yet!");

        rc = ARMGenRegisterInfo.IntRegsRegisterClass;
        int reg = mf.addLiveIn(va.getLocReg(), rc);
        argValue = dag.getCopyFromReg(chain, reg, regVT);

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
        Util.shouldNotReachHere("stack-based argument pass is not supported yet!");
       /* Util.assertion(va.isMemLoc());
        int index = argLocs.get(i).getValNo();
        if (index != lastInsIndex) {
          ArgFlagsTy flags = ins.get(index).flags;
          if (flags.isByVal()) {
            computeRegArea();

          }
          else {
            int fi = mfi.createFixedObject(va.getLocVT().getSizeInBits()/8,
                va.getLocMemOffset(), true);
            SDValue fin = dag.getFrameIndex(fi, new EVT(getPointerTy()), false);
            inVals.add(dag.getLoad(va.getValVT(), chain, fin,
                PseudoSourceValue.getFixedStack(fi), 0, false, 0));
          }
          lastInsIndex = index;
        }*/
      }
    }
    Util.assertion(!varArg, "variadic argument is not supported yet!");
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
        if (subtarget.hasVFPV2() && !isVarArg) {
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

  @Override
  public SDValue lowerReturn(SDValue chain,
                             CallingConv cc,
                             boolean isVarArg,
                             ArrayList<OutputArg> outs,
                             SelectionDAG dag) {
    ArrayList<CCValAssign> retLocs = new ArrayList<>();
    MachineFunction mf = dag.getMachineFunction();
    MachineFrameInfo mfi = mf.getFrameInfo();
    ARMCCState ccInfo = new ARMCCState(cc, isVarArg, getTargetMachine(),
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
      Util.assertion(!va.needsCustom(), "custom is not supported yet!");
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
      case ARMISD.CALL: return "ARMISD.CALL";
      case ARMISD.RET_FLAG: return "ARMISD.RET_FLAG";
      default:
        Util.shouldNotReachHere("Unknown arm opcode!");
        return null;
    }
  }
}
