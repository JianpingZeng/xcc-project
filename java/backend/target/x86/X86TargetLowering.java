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

import backend.codegen.*;
import backend.codegen.dagisel.*;
import backend.codegen.dagisel.SDNode.*;
import backend.codegen.dagisel.ISD;
import backend.support.Attribute;
import backend.support.CallingConv;
import backend.support.LLVMContext;
import backend.target.*;
import backend.target.x86.X86MachineFunctionInfo.NameDecorationStyle;
import backend.type.Type;
import backend.value.*;
import gnu.trove.list.array.TIntArrayList;
import tools.*;

import java.util.ArrayList;
import java.util.Objects;

import static backend.codegen.MachineInstrBuilder.addFrameReference;
import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.codegen.dagisel.CondCode.*;
import static backend.codegen.dagisel.ISD.isBuildVectorAllOnes;
import static backend.codegen.dagisel.ISD.isBuildVectorAllZeros;
import static backend.codegen.dagisel.TLSModel.*;
import static backend.codegen.dagisel.ISD.getSetCCSwappedOperands;
import static backend.support.BackendCmdOptions.EnableUnsafeFPMath;
import static backend.target.TargetLowering.LegalizeAction.*;
import static backend.target.TargetMachine.CodeModel.Kernel;
import static backend.target.TargetMachine.CodeModel.Small;
import static backend.target.TargetMachine.RelocModel.PIC_;
import static backend.target.TargetOptions.DisableMMX;
import static backend.target.TargetOptions.EnablePerformTailCallOpt;
import static backend.target.TargetOptions.GenerateSoftFloatCalls;
import static backend.target.x86.CondCode.*;
import static backend.target.x86.X86GenCallingConv.*;
import static backend.target.x86.X86InstrBuilder.addFullAddress;
import static backend.target.x86.X86InstrInfo.isGlobalRelativeToPICBase;
import static backend.target.x86.X86InstrInfo.isGlobalStubReference;
import static tools.APFloat.RoundingMode.rmNearestTiesToEven;
import static tools.APFloat.x87DoubleExtended;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86TargetLowering extends TargetLowering
{
    private int varArgsFrameIndex;
    private int regSaveFrameIndex;
    private int varArgsGPOffset;
    private int varArgsFPOffset;
    private int bytesToPopOnReturn;
    private int bytesCallerReserves;
    private X86Subtarget subtarget;
    private boolean x86ScalarSSEf64;
    private boolean x86ScalarSSEf32;
    private int x86StackPtr;
    private TargetRegisterInfo regInfo;
    private final static int X86AddrNumOperands = 5;

    public X86TargetLowering(X86TargetMachine tm)
    {
        super(tm);
        subtarget = tm.getSubtarget();
        x86ScalarSSEf64 = subtarget.hasSSE2();
        x86ScalarSSEf32 = subtarget.hasSSE1();
        x86StackPtr = subtarget.is64Bit() ?
                X86GenRegisterNames.RSP :
                X86GenRegisterNames.ESP;

        regInfo = tm.getRegisterInfo();

        // Set up the register classes.
        setShiftAmountTy(new MVT(MVT.i8));
        setBooleanContents(BooleanContent.ZeroOrOneBooleanContent);
        setStackPointerRegisterToSaveRestore(x86StackPtr);
        if (subtarget.isTargetDarwin())
        {
            // Darwin should use _setjmp/_longjmp instead of setjmp/longjmp.
            setUseUnderscoreSetJmp(false);
            setUseUnderscoreLongJmp(false);
        }
        else if (subtarget.isTargetMingw())
        {
            setUseUnderscoreSetJmp(true);
            setUseUnderscoreLongJmp(false);
        }
        else
        {
            setUseUnderscoreSetJmp(true);
            setUseUnderscoreLongJmp(true);
        }
        // set up the register class.
        addRegisterClass(MVT.i8, X86GenRegisterInfo.GR8RegisterClass);
        addRegisterClass(MVT.i16, X86GenRegisterInfo.GR16RegisterClass);
        addRegisterClass(MVT.i32, X86GenRegisterInfo.GR32RegisterClass);
        if (subtarget.is64Bit())
            addRegisterClass(MVT.i64, X86GenRegisterInfo.GR64RegisterClass);

        setLoadExtAction(LoadExtType.SEXTLOAD, new MVT(MVT.i1), Promote);

        // We don't accept any truncstore of integer registers.
        setTruncStoreAction(MVT.i64, MVT.i32, Expand);
        setTruncStoreAction(MVT.i64, MVT.i16, Expand);
        setTruncStoreAction(MVT.i64, MVT.i8 , Expand);
        setTruncStoreAction(MVT.i32, MVT.i16, Expand);
        setTruncStoreAction(MVT.i32, MVT.i8 , Expand);
        setTruncStoreAction(MVT.i16, MVT.i8,  Expand);

        // SETOEQ and SETUNE require checking two conditions.
        setCondCodeAction(SETOEQ, MVT.f32, Expand);
        setCondCodeAction(SETOEQ, MVT.f64, Expand);
        setCondCodeAction(SETOEQ, MVT.f80, Expand);
        setCondCodeAction(SETUNE, MVT.f32, Expand);
        setCondCodeAction(SETUNE, MVT.f64, Expand);
        setCondCodeAction(SETUNE, MVT.f80, Expand);

        // Promote all UINT_TO_FP to larger SINT_TO_FP's, as X86 doesn't have this
        // operation.
        setOperationAction(ISD.UINT_TO_FP       ,MVT.i1, Promote);
        setOperationAction(ISD.UINT_TO_FP       ,MVT.i8, Promote);
        setOperationAction(ISD.UINT_TO_FP       ,MVT.i16, Promote);

        if (subtarget.is64Bit()) {
            setOperationAction(ISD.UINT_TO_FP     , MVT.i32  , Promote);
            setOperationAction(ISD.UINT_TO_FP     , MVT.i64  , Expand);
        } 
        else if (!GenerateSoftFloatCalls.value)
        {
            if (x86ScalarSSEf64) 
            {
                // We have an impenetrably clever algorithm for ui64->double only.
                setOperationAction(ISD.UINT_TO_FP   , MVT.i64  , Custom);
            }
            // We have an algorithm for SSE2, and we turn this into a 64-bit
            // FILD for other targets.
            setOperationAction(ISD.UINT_TO_FP   , MVT.i32  , Custom);
        }

        // Promote i1/i8 SINT_TO_FP to larger SINT_TO_FP's, as X86 doesn't have
        // this operation.
        setOperationAction(ISD.SINT_TO_FP       , MVT.i1   , Promote);
        setOperationAction(ISD.SINT_TO_FP       , MVT.i8   , Promote);

        if (!GenerateSoftFloatCalls.value) {
            // SSE has no i16 to fp conversion, only i32
            if (x86ScalarSSEf32) {
                setOperationAction(ISD.SINT_TO_FP     , MVT.i16  , Promote);
                // f32 and f64 cases are Legal, f80 case is not
                setOperationAction(ISD.SINT_TO_FP     , MVT.i32  , Custom);
            } else {
                setOperationAction(ISD.SINT_TO_FP     , MVT.i16  , Custom);
                setOperationAction(ISD.SINT_TO_FP     , MVT.i32  , Custom);
            }
        } else {
            setOperationAction(ISD.SINT_TO_FP     , MVT.i16  , Promote);
            setOperationAction(ISD.SINT_TO_FP     , MVT.i32  , Promote);
        }

        // In 32-bit mode these are custom lowered.  In 64-bit mode F32 and F64
        // are Legal, f80 is custom lowered.
        setOperationAction(ISD.FP_TO_SINT     , MVT.i64  , Custom);
        setOperationAction(ISD.SINT_TO_FP     , MVT.i64  , Custom);

        // Promote i1/i8 FP_TO_SINT to larger FP_TO_SINTS's, as X86 doesn't have
        // this operation.
        setOperationAction(ISD.FP_TO_SINT       , MVT.i1   , Promote);
        setOperationAction(ISD.FP_TO_SINT       , MVT.i8   , Promote);

        if (x86ScalarSSEf32) {
            setOperationAction(ISD.FP_TO_SINT     , MVT.i16  , Promote);
            // f32 and f64 cases are Legal, f80 case is not
            setOperationAction(ISD.FP_TO_SINT     , MVT.i32  , Custom);
        } else {
            setOperationAction(ISD.FP_TO_SINT     , MVT.i16  , Custom);
            setOperationAction(ISD.FP_TO_SINT     , MVT.i32  , Custom);
        }

        // Handle FP_TO_UINT by promoting the destination to a larger signed
        // conversion.
        setOperationAction(ISD.FP_TO_UINT       , MVT.i1   , Promote);
        setOperationAction(ISD.FP_TO_UINT       , MVT.i8   , Promote);
        setOperationAction(ISD.FP_TO_UINT       , MVT.i16  , Promote);

        if (subtarget.is64Bit()) {
            setOperationAction(ISD.FP_TO_UINT     , MVT.i64  , Expand);
            setOperationAction(ISD.FP_TO_UINT     , MVT.i32  , Promote);
        }
        else if (!GenerateSoftFloatCalls.value)
        {
            if (x86ScalarSSEf32 && !subtarget.hasSSE3())
                // Expand FP_TO_UINT into a select.
                // FIXME: We would like to use a Custom expander here eventually to do
                // the optimal thing for SSE vs. the default expansion in the legalizer.
                setOperationAction(ISD.FP_TO_UINT   , MVT.i32  , Expand);
            else
                // With SSE3 we can use fisttpll to convert to a signed i64; without
                // SSE, we're stuck with a fistpll.
                setOperationAction(ISD.FP_TO_UINT   , MVT.i32  , Custom);
        }

        // TODO: when we have SSE, these could be more efficient, by using movd/movq.
        if (!x86ScalarSSEf64) {
            setOperationAction(ISD.BIT_CONVERT      , MVT.f32  , Expand);
            setOperationAction(ISD.BIT_CONVERT      , MVT.i32  , Expand);
        }

        // Scalar integer divide and remainder are lowered to use operations that
        // produce two results, to match the available instructions. This exposes
        // the two-result form to trivial CSE, which is able to combine x/y and x%y
        // into a single instruction.
        //
        // Scalar integer multiply-high is also lowered to use two-result
        // operations, to match the available instructions. However, plain multiply
        // (low) operations are left as Legal, as there are single-result
        // instructions for this in x86. Using the two-result multiply instructions
        // when both high and low results are needed must be arranged by dagcombine.
        setOperationAction(ISD.MULHS           , MVT.i8    , Expand);
        setOperationAction(ISD.MULHU           , MVT.i8    , Expand);
        setOperationAction(ISD.SDIV            , MVT.i8    , Expand);
        setOperationAction(ISD.UDIV            , MVT.i8    , Expand);
        setOperationAction(ISD.SREM            , MVT.i8    , Expand);
        setOperationAction(ISD.UREM            , MVT.i8    , Expand);
        setOperationAction(ISD.MULHS           , MVT.i16   , Expand);
        setOperationAction(ISD.MULHU           , MVT.i16   , Expand);
        setOperationAction(ISD.SDIV            , MVT.i16   , Expand);
        setOperationAction(ISD.UDIV            , MVT.i16   , Expand);
        setOperationAction(ISD.SREM            , MVT.i16   , Expand);
        setOperationAction(ISD.UREM            , MVT.i16   , Expand);
        setOperationAction(ISD.MULHS           , MVT.i32   , Expand);
        setOperationAction(ISD.MULHU           , MVT.i32   , Expand);
        setOperationAction(ISD.SDIV            , MVT.i32   , Expand);
        setOperationAction(ISD.UDIV            , MVT.i32   , Expand);
        setOperationAction(ISD.SREM            , MVT.i32   , Expand);
        setOperationAction(ISD.UREM            , MVT.i32   , Expand);
        setOperationAction(ISD.MULHS           , MVT.i64   , Expand);
        setOperationAction(ISD.MULHU           , MVT.i64   , Expand);
        setOperationAction(ISD.SDIV            , MVT.i64   , Expand);
        setOperationAction(ISD.UDIV            , MVT.i64   , Expand);
        setOperationAction(ISD.SREM            , MVT.i64   , Expand);
        setOperationAction(ISD.UREM            , MVT.i64   , Expand);

        setOperationAction(ISD.BR_JT            , MVT.Other, Expand);
        setOperationAction(ISD.BRCOND           , MVT.Other, Custom);
        setOperationAction(ISD.BR_CC            , MVT.Other, Expand);
        setOperationAction(ISD.SELECT_CC        , MVT.Other, Expand);
        if (subtarget.is64Bit())
            setOperationAction(ISD.SIGN_EXTEND_INREG, MVT.i32, Legal);
        setOperationAction(ISD.SIGN_EXTEND_INREG, MVT.i16  , Legal);
        setOperationAction(ISD.SIGN_EXTEND_INREG, MVT.i8   , Legal);
        setOperationAction(ISD.SIGN_EXTEND_INREG, MVT.i1   , Expand);
        setOperationAction(ISD.FP_ROUND_INREG   , MVT.f32  , Expand);
        setOperationAction(ISD.FREM             , MVT.f32  , Expand);
        setOperationAction(ISD.FREM             , MVT.f64  , Expand);
        setOperationAction(ISD.FREM             , MVT.f80  , Expand);
        setOperationAction(ISD.FLT_ROUNDS_      , MVT.i32  , Custom);

        setOperationAction(ISD.CTPOP            , MVT.i8   , Expand);
        setOperationAction(ISD.CTTZ             , MVT.i8   , Custom);
        setOperationAction(ISD.CTLZ             , MVT.i8   , Custom);
        setOperationAction(ISD.CTPOP            , MVT.i16  , Expand);
        setOperationAction(ISD.CTTZ             , MVT.i16  , Custom);
        setOperationAction(ISD.CTLZ             , MVT.i16  , Custom);
        setOperationAction(ISD.CTPOP            , MVT.i32  , Expand);
        setOperationAction(ISD.CTTZ             , MVT.i32  , Custom);
        setOperationAction(ISD.CTLZ             , MVT.i32  , Custom);
        if (subtarget.is64Bit()) {
            setOperationAction(ISD.CTPOP          , MVT.i64  , Expand);
            setOperationAction(ISD.CTTZ           , MVT.i64  , Custom);
            setOperationAction(ISD.CTLZ           , MVT.i64  , Custom);
        }

        setOperationAction(ISD.READCYCLECOUNTER , MVT.i64  , Custom);
        setOperationAction(ISD.BSWAP            , MVT.i16  , Expand);

        // These should be promoted to a larger select which is supported.
        setOperationAction(ISD.SELECT           , MVT.i1   , Promote);
        setOperationAction(ISD.SELECT           , MVT.i8   , Promote);
        // X86 wants to expand cmov itself.
        setOperationAction(ISD.SELECT          , MVT.i16  , Custom);
        setOperationAction(ISD.SELECT          , MVT.i32  , Custom);
        setOperationAction(ISD.SELECT          , MVT.f32  , Custom);
        setOperationAction(ISD.SELECT          , MVT.f64  , Custom);
        setOperationAction(ISD.SELECT          , MVT.f80  , Custom);
        setOperationAction(ISD.SETCC           , MVT.i8   , Custom);
        setOperationAction(ISD.SETCC           , MVT.i16  , Custom);
        setOperationAction(ISD.SETCC           , MVT.i32  , Custom);
        setOperationAction(ISD.SETCC           , MVT.f32  , Custom);
        setOperationAction(ISD.SETCC           , MVT.f64  , Custom);
        setOperationAction(ISD.SETCC           , MVT.f80  , Custom);
        if (subtarget.is64Bit())
        {
            setOperationAction(ISD.SELECT        , MVT.i64  , Custom);
            setOperationAction(ISD.SETCC         , MVT.i64  , Custom);
        }
        setOperationAction(ISD.EH_RETURN       , MVT.Other, Custom);

        // Darwin ABI issue.
        setOperationAction(ISD.ConstantPool    , MVT.i32  , Custom);
        setOperationAction(ISD.JumpTable       , MVT.i32  , Custom);
        setOperationAction(ISD.GlobalAddress   , MVT.i32  , Custom);
        setOperationAction(ISD.GlobalTLSAddress, MVT.i32  , Custom);
        if (subtarget.is64Bit())
            setOperationAction(ISD.GlobalTLSAddress, MVT.i64, Custom);
        setOperationAction(ISD.ExternalSymbol  , MVT.i32  , Custom);
        if (subtarget.is64Bit()) {
            setOperationAction(ISD.ConstantPool  , MVT.i64  , Custom);
            setOperationAction(ISD.JumpTable     , MVT.i64  , Custom);
            setOperationAction(ISD.GlobalAddress , MVT.i64  , Custom);
            setOperationAction(ISD.ExternalSymbol, MVT.i64  , Custom);
        }
        // 64-bit addm sub, shl, sra, srl (iff 32-bit x86)
        setOperationAction(ISD.SHL_PARTS       , MVT.i32  , Custom);
        setOperationAction(ISD.SRA_PARTS       , MVT.i32  , Custom);
        setOperationAction(ISD.SRL_PARTS       , MVT.i32  , Custom);
        if (subtarget.is64Bit()) {
            setOperationAction(ISD.SHL_PARTS     , MVT.i64  , Custom);
            setOperationAction(ISD.SRA_PARTS     , MVT.i64  , Custom);
            setOperationAction(ISD.SRL_PARTS     , MVT.i64  , Custom);
        }

        if (subtarget.hasSSE1())
            setOperationAction(ISD.PREFETCH      , MVT.Other, Legal);

        if (!subtarget.hasSSE2())
            setOperationAction(ISD.MEMBARRIER    , MVT.Other, Expand);

        // Expand certain atomics
        setOperationAction(ISD.ATOMIC_CMP_SWAP, MVT.i8, Custom);
        setOperationAction(ISD.ATOMIC_CMP_SWAP, MVT.i16, Custom);
        setOperationAction(ISD.ATOMIC_CMP_SWAP, MVT.i32, Custom);
        setOperationAction(ISD.ATOMIC_CMP_SWAP, MVT.i64, Custom);

        setOperationAction(ISD.ATOMIC_LOAD_SUB, MVT.i8, Custom);
        setOperationAction(ISD.ATOMIC_LOAD_SUB, MVT.i16, Custom);
        setOperationAction(ISD.ATOMIC_LOAD_SUB, MVT.i32, Custom);
        setOperationAction(ISD.ATOMIC_LOAD_SUB, MVT.i64, Custom);

        if (!subtarget.is64Bit()) {
            setOperationAction(ISD.ATOMIC_LOAD_ADD, MVT.i64, Custom);
            setOperationAction(ISD.ATOMIC_LOAD_SUB, MVT.i64, Custom);
            setOperationAction(ISD.ATOMIC_LOAD_AND, MVT.i64, Custom);
            setOperationAction(ISD.ATOMIC_LOAD_OR, MVT.i64, Custom);
            setOperationAction(ISD.ATOMIC_LOAD_XOR, MVT.i64, Custom);
            setOperationAction(ISD.ATOMIC_LOAD_NAND, MVT.i64, Custom);
            setOperationAction(ISD.ATOMIC_SWAP, MVT.i64, Custom);
        }

        // Use the default ISD.DBG_STOPPOINT, ISD.DECLARE expansion.
        setOperationAction(ISD.DBG_STOPPOINT, MVT.Other, Expand);
        // FIXME - use subtarget debug flags
        if (!subtarget.isTargetDarwin() &&
                !subtarget.isTargetELF() &&
                        !subtarget.isTargetCygMing()) {
            setOperationAction(ISD.DBG_LABEL, MVT.Other, Expand);
            setOperationAction(ISD.EH_LABEL, MVT.Other, Expand);
        }

        setOperationAction(ISD.EXCEPTIONADDR, MVT.i64, Expand);
        setOperationAction(ISD.EHSELECTION,   MVT.i64, Expand);
        setOperationAction(ISD.EXCEPTIONADDR, MVT.i32, Expand);
        setOperationAction(ISD.EHSELECTION,   MVT.i32, Expand);
        if (subtarget.is64Bit())
        {
            setExceptionPointerRegister(X86GenRegisterNames.RAX);
            setExceptionSelectorRegister(X86GenRegisterNames.RDX);
        } else {
            setExceptionPointerRegister(X86GenRegisterNames.EAX);
            setExceptionSelectorRegister(X86GenRegisterNames.EDX);
        }
        setOperationAction(ISD.FRAME_TO_ARGS_OFFSET, MVT.i32, Custom);
        setOperationAction(ISD.FRAME_TO_ARGS_OFFSET, MVT.i64, Custom);

        setOperationAction(ISD.TRAMPOLINE, MVT.Other, Custom);

        setOperationAction(ISD.TRAP, MVT.Other, Legal);

        // VASTART needs to be custom lowered to use the VarArgsFrameIndex
        setOperationAction(ISD.VASTART           , MVT.Other, Custom);
        setOperationAction(ISD.VAEND             , MVT.Other, Expand);
        if (subtarget.is64Bit()) {
            setOperationAction(ISD.VAARG           , MVT.Other, Custom);
            setOperationAction(ISD.VACOPY          , MVT.Other, Custom);
        } else {
            setOperationAction(ISD.VAARG           , MVT.Other, Expand);
            setOperationAction(ISD.VACOPY          , MVT.Other, Expand);
        }

        setOperationAction(ISD.STACKSAVE,          MVT.Other, Expand);
        setOperationAction(ISD.STACKRESTORE,       MVT.Other, Expand);
        if (subtarget.is64Bit())
            setOperationAction(ISD.DYNAMIC_STACKALLOC, MVT.i64, Expand);
        if (subtarget.isTargetCygMing())
            setOperationAction(ISD.DYNAMIC_STACKALLOC, MVT.i32, Custom);
        else
            setOperationAction(ISD.DYNAMIC_STACKALLOC, MVT.i32, Expand);

        if (!GenerateSoftFloatCalls.value && x86ScalarSSEf64) {
            // f32 and f64 use SSE.
            // Set up the FP register classes.
            addRegisterClass(MVT.f32, X86GenRegisterInfo.FR32RegisterClass);
            addRegisterClass(MVT.f64, X86GenRegisterInfo.FR64RegisterClass);

            // Use ANDPD to simulate FABS.
            setOperationAction(ISD.FABS , MVT.f64, Custom);
            setOperationAction(ISD.FABS , MVT.f32, Custom);

            // Use XORP to simulate FNEG.
            setOperationAction(ISD.FNEG , MVT.f64, Custom);
            setOperationAction(ISD.FNEG , MVT.f32, Custom);

            // Use ANDPD and ORPD to simulate FCOPYSIGN.
            setOperationAction(ISD.FCOPYSIGN, MVT.f64, Custom);
            setOperationAction(ISD.FCOPYSIGN, MVT.f32, Custom);

            // We don't support sin/cos/fmod
            setOperationAction(ISD.FSIN , MVT.f64, Expand);
            setOperationAction(ISD.FCOS , MVT.f64, Expand);
            setOperationAction(ISD.FSIN , MVT.f32, Expand);
            setOperationAction(ISD.FCOS , MVT.f32, Expand);

            // Expand FP immediates into loads from the stack, except for the special
            // cases we handle.
            addLegalFPImmediate(new APFloat(+0.0)); // xorpd
            addLegalFPImmediate(new APFloat(+0.0f)); // xorps
        } else if (!GenerateSoftFloatCalls.value && x86ScalarSSEf32) {
            // Use SSE for f32, x87 for f64.
            // Set up the FP register classes.
            addRegisterClass(MVT.f32, X86GenRegisterInfo.FR32RegisterClass);
            addRegisterClass(MVT.f64, X86GenRegisterInfo.RFP64RegisterClass);

            // Use ANDPS to simulate FABS.
            setOperationAction(ISD.FABS , MVT.f32, Custom);

            // Use XORP to simulate FNEG.
            setOperationAction(ISD.FNEG , MVT.f32, Custom);

            setOperationAction(ISD.UNDEF,     MVT.f64, Expand);

            // Use ANDPS and ORPS to simulate FCOPYSIGN.
            setOperationAction(ISD.FCOPYSIGN, MVT.f64, Expand);
            setOperationAction(ISD.FCOPYSIGN, MVT.f32, Custom);

            // We don't support sin/cos/fmod
            setOperationAction(ISD.FSIN , MVT.f32, Expand);
            setOperationAction(ISD.FCOS , MVT.f32, Expand);

            // Special cases we handle for FP constants.
            addLegalFPImmediate(new APFloat(+0.0f)); // xorps
            addLegalFPImmediate(new APFloat(+0.0)); // FLD0
            addLegalFPImmediate(new APFloat(+1.0)); // FLD1
            addLegalFPImmediate(new APFloat(-0.0)); // FLD0/FCHS
            addLegalFPImmediate(new APFloat(-1.0)); // FLD1/FCHS

            if (!EnableUnsafeFPMath.value) {
                setOperationAction(ISD.FSIN           , MVT.f64  , Expand);
                setOperationAction(ISD.FCOS           , MVT.f64  , Expand);
            }
        } else if (!GenerateSoftFloatCalls.value) {
            // f32 and f64 in x87.
            // Set up the FP register classes.
            addRegisterClass(MVT.f64, X86GenRegisterInfo.RFP64RegisterClass);
            addRegisterClass(MVT.f32, X86GenRegisterInfo.RFP32RegisterClass);

            setOperationAction(ISD.UNDEF,     MVT.f64, Expand);
            setOperationAction(ISD.UNDEF,     MVT.f32, Expand);
            setOperationAction(ISD.FCOPYSIGN, MVT.f64, Expand);
            setOperationAction(ISD.FCOPYSIGN, MVT.f32, Expand);

            if (!EnableUnsafeFPMath.value) {
                setOperationAction(ISD.FSIN           , MVT.f64  , Expand);
                setOperationAction(ISD.FCOS           , MVT.f64  , Expand);
            }
            addLegalFPImmediate(new APFloat(+0.0)); // FLD0
            addLegalFPImmediate(new APFloat(+1.0)); // FLD1
            addLegalFPImmediate(new APFloat(-0.0)); // FLD0/FCHS
            addLegalFPImmediate(new APFloat(-1.0)); // FLD1/FCHS
            addLegalFPImmediate(new APFloat(+0.0f)); // FLD0
            addLegalFPImmediate(new APFloat(+1.0f)); // FLD1
            addLegalFPImmediate(new APFloat(-0.0f)); // FLD0/FCHS
            addLegalFPImmediate(new APFloat(-1.0f)); // FLD1/FCHS
        }

        // Long double always uses X87.
        if (!GenerateSoftFloatCalls.value) {
            addRegisterClass(MVT.f80, X86GenRegisterInfo.RFP80RegisterClass);
            setOperationAction(ISD.UNDEF,     MVT.f80, Expand);
            setOperationAction(ISD.FCOPYSIGN, MVT.f80, Expand);
            {
                OutParamWrapper<Boolean> ignored = new OutParamWrapper<>(false);

                APFloat TmpFlt = new APFloat(+0.0);
                TmpFlt.convert(x87DoubleExtended, rmNearestTiesToEven, ignored);
                addLegalFPImmediate(TmpFlt);  // FLD0
                TmpFlt.changeSign();
                addLegalFPImmediate(TmpFlt);  // FLD0/FCHS
                APFloat TmpFlt2 = new APFloat(+1.0);
                TmpFlt2.convert(x87DoubleExtended, rmNearestTiesToEven, ignored);
                addLegalFPImmediate(TmpFlt2);  // FLD1
                TmpFlt2.changeSign();
                addLegalFPImmediate(TmpFlt2);  // FLD1/FCHS
            }

            if (!EnableUnsafeFPMath.value) {
                setOperationAction(ISD.FSIN           , MVT.f80  , Expand);
                setOperationAction(ISD.FCOS           , MVT.f80  , Expand);
            }
        }

        // Always use a library call for pow.
        setOperationAction(ISD.FPOW             , MVT.f32  , Expand);
        setOperationAction(ISD.FPOW             , MVT.f64  , Expand);
        setOperationAction(ISD.FPOW             , MVT.f80  , Expand);

        setOperationAction(ISD.FLOG, MVT.f80, Expand);
        setOperationAction(ISD.FLOG2, MVT.f80, Expand);
        setOperationAction(ISD.FLOG10, MVT.f80, Expand);
        setOperationAction(ISD.FEXP, MVT.f80, Expand);
        setOperationAction(ISD.FEXP2, MVT.f80, Expand);

        // First set operation action for all vector types to either promote
        // (for widening) or expand (for scalarization). Then we will selectively
        // turn on ones that can be effectively codegen'd.
        for (int VT = MVT.FIRST_VECTOR_VALUETYPE;
             VT <= MVT.LAST_VECTOR_VALUETYPE; ++VT) 
        {
            setOperationAction(ISD.ADD , VT, Expand);
            setOperationAction(ISD.SUB , VT, Expand);
            setOperationAction(ISD.FADD, VT, Expand);
            setOperationAction(ISD.FNEG, VT, Expand);
            setOperationAction(ISD.FSUB, VT, Expand);
            setOperationAction(ISD.MUL , VT, Expand);
            setOperationAction(ISD.FMUL, VT, Expand);
            setOperationAction(ISD.SDIV, VT, Expand);
            setOperationAction(ISD.UDIV, VT, Expand);
            setOperationAction(ISD.FDIV, VT, Expand);
            setOperationAction(ISD.SREM, VT, Expand);
            setOperationAction(ISD.UREM, VT, Expand);
            setOperationAction(ISD.LOAD, VT, Expand);
            setOperationAction(ISD.VECTOR_SHUFFLE, VT, Expand);
            setOperationAction(ISD.EXTRACT_VECTOR_ELT,VT,Expand);
            setOperationAction(ISD.EXTRACT_SUBVECTOR,VT,Expand);
            setOperationAction(ISD.INSERT_VECTOR_ELT,VT, Expand);
            setOperationAction(ISD.FABS, VT, Expand);
            setOperationAction(ISD.FSIN, VT, Expand);
            setOperationAction(ISD.FCOS, VT, Expand);
            setOperationAction(ISD.FREM, VT, Expand);
            setOperationAction(ISD.FPOWI, VT, Expand);
            setOperationAction(ISD.FSQRT, VT, Expand);
            setOperationAction(ISD.FCOPYSIGN, VT, Expand);
            setOperationAction(ISD.SMUL_LOHI, VT, Expand);
            setOperationAction(ISD.UMUL_LOHI, VT, Expand);
            setOperationAction(ISD.SDIVREM, VT, Expand);
            setOperationAction(ISD.UDIVREM, VT, Expand);
            setOperationAction(ISD.FPOW, VT, Expand);
            setOperationAction(ISD.CTPOP, VT, Expand);
            setOperationAction(ISD.CTTZ, VT, Expand);
            setOperationAction(ISD.CTLZ, VT, Expand);
            setOperationAction(ISD.SHL, VT, Expand);
            setOperationAction(ISD.SRA, VT, Expand);
            setOperationAction(ISD.SRL, VT, Expand);
            setOperationAction(ISD.ROTL, VT, Expand);
            setOperationAction(ISD.ROTR, VT, Expand);
            setOperationAction(ISD.BSWAP, VT, Expand);
            setOperationAction(ISD.VSETCC, VT, Expand);
            setOperationAction(ISD.FLOG, VT, Expand);
            setOperationAction(ISD.FLOG2, VT, Expand);
            setOperationAction(ISD.FLOG10, VT, Expand);
            setOperationAction(ISD.FEXP, VT, Expand);
            setOperationAction(ISD.FEXP2, VT, Expand);
            setOperationAction(ISD.FP_TO_UINT, VT, Expand);
            setOperationAction(ISD.FP_TO_SINT, VT, Expand);
            setOperationAction(ISD.UINT_TO_FP, VT, Expand);
            setOperationAction(ISD.SINT_TO_FP, VT, Expand);
        }

        // FIXME: In order to prevent SSE instructions being expanded to MMX ones
        // with -msoft-float, disable use of MMX as well.
        if (!GenerateSoftFloatCalls.value && !DisableMMX.value && subtarget.hasMMX()) {
            addRegisterClass(MVT.v8i8,  X86GenRegisterInfo.VR64RegisterClass);
            addRegisterClass(MVT.v4i16, X86GenRegisterInfo.VR64RegisterClass);
            addRegisterClass(MVT.v2i32, X86GenRegisterInfo.VR64RegisterClass);
            addRegisterClass(MVT.v2f32, X86GenRegisterInfo.VR64RegisterClass);
            addRegisterClass(MVT.v1i64, X86GenRegisterInfo.VR64RegisterClass);

            setOperationAction(ISD.ADD,                MVT.v8i8,  Legal);
            setOperationAction(ISD.ADD,                MVT.v4i16, Legal);
            setOperationAction(ISD.ADD,                MVT.v2i32, Legal);
            setOperationAction(ISD.ADD,                MVT.v1i64, Legal);

            setOperationAction(ISD.SUB,                MVT.v8i8,  Legal);
            setOperationAction(ISD.SUB,                MVT.v4i16, Legal);
            setOperationAction(ISD.SUB,                MVT.v2i32, Legal);
            setOperationAction(ISD.SUB,                MVT.v1i64, Legal);

            setOperationAction(ISD.MULHS,              MVT.v4i16, Legal);
            setOperationAction(ISD.MUL,                MVT.v4i16, Legal);

            setOperationAction(ISD.AND,                MVT.v8i8,  Promote);
            addPromotedToType (ISD.AND,                MVT.v8i8,  MVT.v1i64);
            setOperationAction(ISD.AND,                MVT.v4i16, Promote);
            addPromotedToType (ISD.AND,                MVT.v4i16, MVT.v1i64);
            setOperationAction(ISD.AND,                MVT.v2i32, Promote);
            addPromotedToType (ISD.AND,                MVT.v2i32, MVT.v1i64);
            setOperationAction(ISD.AND,                MVT.v1i64, Legal);

            setOperationAction(ISD.OR,                 MVT.v8i8,  Promote);
            addPromotedToType (ISD.OR,                 MVT.v8i8,  MVT.v1i64);
            setOperationAction(ISD.OR,                 MVT.v4i16, Promote);
            addPromotedToType (ISD.OR,                 MVT.v4i16, MVT.v1i64);
            setOperationAction(ISD.OR,                 MVT.v2i32, Promote);
            addPromotedToType (ISD.OR,                 MVT.v2i32, MVT.v1i64);
            setOperationAction(ISD.OR,                 MVT.v1i64, Legal);

            setOperationAction(ISD.XOR,                MVT.v8i8,  Promote);
            addPromotedToType (ISD.XOR,                MVT.v8i8,  MVT.v1i64);
            setOperationAction(ISD.XOR,                MVT.v4i16, Promote);
            addPromotedToType (ISD.XOR,                MVT.v4i16, MVT.v1i64);
            setOperationAction(ISD.XOR,                MVT.v2i32, Promote);
            addPromotedToType (ISD.XOR,                MVT.v2i32, MVT.v1i64);
            setOperationAction(ISD.XOR,                MVT.v1i64, Legal);

            setOperationAction(ISD.LOAD,               MVT.v8i8,  Promote);
            addPromotedToType (ISD.LOAD,               MVT.v8i8,  MVT.v1i64);
            setOperationAction(ISD.LOAD,               MVT.v4i16, Promote);
            addPromotedToType (ISD.LOAD,               MVT.v4i16, MVT.v1i64);
            setOperationAction(ISD.LOAD,               MVT.v2i32, Promote);
            addPromotedToType (ISD.LOAD,               MVT.v2i32, MVT.v1i64);
            setOperationAction(ISD.LOAD,               MVT.v2f32, Promote);
            addPromotedToType (ISD.LOAD,               MVT.v2f32, MVT.v1i64);
            setOperationAction(ISD.LOAD,               MVT.v1i64, Legal);

            setOperationAction(ISD.BUILD_VECTOR,       MVT.v8i8,  Custom);
            setOperationAction(ISD.BUILD_VECTOR,       MVT.v4i16, Custom);
            setOperationAction(ISD.BUILD_VECTOR,       MVT.v2i32, Custom);
            setOperationAction(ISD.BUILD_VECTOR,       MVT.v2f32, Custom);
            setOperationAction(ISD.BUILD_VECTOR,       MVT.v1i64, Custom);

            setOperationAction(ISD.VECTOR_SHUFFLE,     MVT.v8i8,  Custom);
            setOperationAction(ISD.VECTOR_SHUFFLE,     MVT.v4i16, Custom);
            setOperationAction(ISD.VECTOR_SHUFFLE,     MVT.v2i32, Custom);
            setOperationAction(ISD.VECTOR_SHUFFLE,     MVT.v1i64, Custom);

            setOperationAction(ISD.SCALAR_TO_VECTOR,   MVT.v2f32, Custom);
            setOperationAction(ISD.SCALAR_TO_VECTOR,   MVT.v8i8,  Custom);
            setOperationAction(ISD.SCALAR_TO_VECTOR,   MVT.v4i16, Custom);
            setOperationAction(ISD.SCALAR_TO_VECTOR,   MVT.v1i64, Custom);

            setOperationAction(ISD.INSERT_VECTOR_ELT,  MVT.v4i16, Custom);

            setTruncStoreAction(MVT.v8i16,             MVT.v8i8, Expand);
            setOperationAction(ISD.TRUNCATE,           MVT.v8i8, Expand);
            setOperationAction(ISD.SELECT,             MVT.v8i8, Promote);
            setOperationAction(ISD.SELECT,             MVT.v4i16, Promote);
            setOperationAction(ISD.SELECT,             MVT.v2i32, Promote);
            setOperationAction(ISD.SELECT,             MVT.v1i64, Custom);
            setOperationAction(ISD.VSETCC,             MVT.v8i8, Custom);
            setOperationAction(ISD.VSETCC,             MVT.v4i16, Custom);
            setOperationAction(ISD.VSETCC,             MVT.v2i32, Custom);
        }

        if (!GenerateSoftFloatCalls.value && subtarget.hasSSE1()) 
        {
            addRegisterClass(MVT.v4f32, X86GenRegisterInfo.VR128RegisterClass);

            setOperationAction(ISD.FADD,               MVT.v4f32, Legal);
            setOperationAction(ISD.FSUB,               MVT.v4f32, Legal);
            setOperationAction(ISD.FMUL,               MVT.v4f32, Legal);
            setOperationAction(ISD.FDIV,               MVT.v4f32, Legal);
            setOperationAction(ISD.FSQRT,              MVT.v4f32, Legal);
            setOperationAction(ISD.FNEG,               MVT.v4f32, Custom);
            setOperationAction(ISD.LOAD,               MVT.v4f32, Legal);
            setOperationAction(ISD.BUILD_VECTOR,       MVT.v4f32, Custom);
            setOperationAction(ISD.VECTOR_SHUFFLE,     MVT.v4f32, Custom);
            setOperationAction(ISD.EXTRACT_VECTOR_ELT, MVT.v4f32, Custom);
            setOperationAction(ISD.SELECT,             MVT.v4f32, Custom);
            setOperationAction(ISD.VSETCC,             MVT.v4f32, Custom);
        }

        if (!GenerateSoftFloatCalls.value && subtarget.hasSSE2()) {
            addRegisterClass(MVT.v2f64, X86GenRegisterInfo.VR128RegisterClass);

            // FIXME: Unfortunately -soft-float and -no-implicit-float means XMM
            // registers cannot be used even for integer operations.
            addRegisterClass(MVT.v16i8, X86GenRegisterInfo.VR128RegisterClass);
            addRegisterClass(MVT.v8i16, X86GenRegisterInfo.VR128RegisterClass);
            addRegisterClass(MVT.v4i32, X86GenRegisterInfo.VR128RegisterClass);
            addRegisterClass(MVT.v2i64, X86GenRegisterInfo.VR128RegisterClass);

            setOperationAction(ISD.ADD,                MVT.v16i8, Legal);
            setOperationAction(ISD.ADD,                MVT.v8i16, Legal);
            setOperationAction(ISD.ADD,                MVT.v4i32, Legal);
            setOperationAction(ISD.ADD,                MVT.v2i64, Legal);
            setOperationAction(ISD.MUL,                MVT.v2i64, Custom);
            setOperationAction(ISD.SUB,                MVT.v16i8, Legal);
            setOperationAction(ISD.SUB,                MVT.v8i16, Legal);
            setOperationAction(ISD.SUB,                MVT.v4i32, Legal);
            setOperationAction(ISD.SUB,                MVT.v2i64, Legal);
            setOperationAction(ISD.MUL,                MVT.v8i16, Legal);
            setOperationAction(ISD.FADD,               MVT.v2f64, Legal);
            setOperationAction(ISD.FSUB,               MVT.v2f64, Legal);
            setOperationAction(ISD.FMUL,               MVT.v2f64, Legal);
            setOperationAction(ISD.FDIV,               MVT.v2f64, Legal);
            setOperationAction(ISD.FSQRT,              MVT.v2f64, Legal);
            setOperationAction(ISD.FNEG,               MVT.v2f64, Custom);

            setOperationAction(ISD.VSETCC,             MVT.v2f64, Custom);
            setOperationAction(ISD.VSETCC,             MVT.v16i8, Custom);
            setOperationAction(ISD.VSETCC,             MVT.v8i16, Custom);
            setOperationAction(ISD.VSETCC,             MVT.v4i32, Custom);

            setOperationAction(ISD.SCALAR_TO_VECTOR,   MVT.v16i8, Custom);
            setOperationAction(ISD.SCALAR_TO_VECTOR,   MVT.v8i16, Custom);
            setOperationAction(ISD.INSERT_VECTOR_ELT,  MVT.v8i16, Custom);
            setOperationAction(ISD.INSERT_VECTOR_ELT,  MVT.v4i32, Custom);
            setOperationAction(ISD.INSERT_VECTOR_ELT,  MVT.v4f32, Custom);

            // Custom lower build_vector, vector_shuffle, and extract_vector_elt.
            for (int i = MVT.v16i8; i != MVT.v2i64; ++i) {
                EVT VT = new EVT(i);
                // Do not attempt to custom lower non-power-of-2 vectors
                if (!Util.isPowerOf2(VT.getVectorNumElements()))
                    continue;
                // Do not attempt to custom lower non-128-bit vectors
                if (!VT.is128BitVector())
                    continue;
                setOperationAction(ISD.BUILD_VECTOR,
                        VT.getSimpleVT().simpleVT, Custom);
                setOperationAction(ISD.VECTOR_SHUFFLE,
                        VT.getSimpleVT().simpleVT, Custom);
                setOperationAction(ISD.EXTRACT_VECTOR_ELT,
                        VT.getSimpleVT().simpleVT, Custom);
            }

            setOperationAction(ISD.BUILD_VECTOR,       MVT.v2f64, Custom);
            setOperationAction(ISD.BUILD_VECTOR,       MVT.v2i64, Custom);
            setOperationAction(ISD.VECTOR_SHUFFLE,     MVT.v2f64, Custom);
            setOperationAction(ISD.VECTOR_SHUFFLE,     MVT.v2i64, Custom);
            setOperationAction(ISD.INSERT_VECTOR_ELT,  MVT.v2f64, Custom);
            setOperationAction(ISD.EXTRACT_VECTOR_ELT, MVT.v2f64, Custom);

            if (subtarget.is64Bit()) {
                setOperationAction(ISD.INSERT_VECTOR_ELT,  MVT.v2i64, Custom);
                setOperationAction(ISD.EXTRACT_VECTOR_ELT, MVT.v2i64, Custom);
            }

            // Promote v16i8, v8i16, v4i32 load, select, and, or, xor to v2i64.
            for (int i = (int)MVT.v16i8; i != (int)MVT.v2i64; i++) {
                MVT SVT = new MVT(i);
                EVT VT = new EVT(SVT);

                // Do not attempt to promote non-128-bit vectors
                if (!VT.is128BitVector()) {
                    continue;
                }
                setOperationAction(ISD.AND,    SVT.simpleVT, Promote);
                addPromotedToType (ISD.AND,    SVT.simpleVT, MVT.v2i64);
                setOperationAction(ISD.OR,     SVT.simpleVT, Promote);
                addPromotedToType (ISD.OR,     SVT.simpleVT, MVT.v2i64);
                setOperationAction(ISD.XOR,    SVT.simpleVT, Promote);
                addPromotedToType (ISD.XOR,    SVT.simpleVT, MVT.v2i64);
                setOperationAction(ISD.LOAD,   SVT.simpleVT, Promote);
                addPromotedToType (ISD.LOAD,   SVT.simpleVT, MVT.v2i64);
                setOperationAction(ISD.SELECT, SVT.simpleVT, Promote);
                addPromotedToType (ISD.SELECT, SVT.simpleVT, MVT.v2i64);
            }

            setTruncStoreAction(MVT.f64, MVT.f32, Expand);

            // Custom lower v2i64 and v2f64 selects.
            setOperationAction(ISD.LOAD,               MVT.v2f64, Legal);
            setOperationAction(ISD.LOAD,               MVT.v2i64, Legal);
            setOperationAction(ISD.SELECT,             MVT.v2f64, Custom);
            setOperationAction(ISD.SELECT,             MVT.v2i64, Custom);

            setOperationAction(ISD.FP_TO_SINT,         MVT.v4i32, Legal);
            setOperationAction(ISD.SINT_TO_FP,         MVT.v4i32, Legal);
            if (!DisableMMX.value && subtarget.hasMMX()) {
                setOperationAction(ISD.FP_TO_SINT,         MVT.v2i32, Custom);
                setOperationAction(ISD.SINT_TO_FP,         MVT.v2i32, Custom);
            }
        }

        if (subtarget.hasSSE41()) {
            // FIXME: Do we need to handle scalar-to-vector here?
            setOperationAction(ISD.MUL,                MVT.v4i32, Legal);

            // i8 and i16 vectors are custom , because the source register and source
            // source memory operand types are not the same width.  f32 vectors are
            // custom since the immediate controlling the insert encodes additional
            // information.
            setOperationAction(ISD.INSERT_VECTOR_ELT,  MVT.v16i8, Custom);
            setOperationAction(ISD.INSERT_VECTOR_ELT,  MVT.v8i16, Custom);
            setOperationAction(ISD.INSERT_VECTOR_ELT,  MVT.v4i32, Custom);
            setOperationAction(ISD.INSERT_VECTOR_ELT,  MVT.v4f32, Custom);

            setOperationAction(ISD.EXTRACT_VECTOR_ELT, MVT.v16i8, Custom);
            setOperationAction(ISD.EXTRACT_VECTOR_ELT, MVT.v8i16, Custom);
            setOperationAction(ISD.EXTRACT_VECTOR_ELT, MVT.v4i32, Custom);
            setOperationAction(ISD.EXTRACT_VECTOR_ELT, MVT.v4f32, Custom);

            if (subtarget.is64Bit()) {
                setOperationAction(ISD.INSERT_VECTOR_ELT,  MVT.v2i64, Legal);
                setOperationAction(ISD.EXTRACT_VECTOR_ELT, MVT.v2i64, Legal);
            }
        }

        if (subtarget.hasSSE42()) {
            setOperationAction(ISD.VSETCC,             MVT.v2i64, Custom);
        }

        if (!GenerateSoftFloatCalls.value && subtarget.hasAVX()) {
            addRegisterClass(MVT.v8f32, X86GenRegisterInfo.VR256RegisterClass);
            addRegisterClass(MVT.v4f64, X86GenRegisterInfo.VR256RegisterClass);
            addRegisterClass(MVT.v8i32, X86GenRegisterInfo.VR256RegisterClass);
            addRegisterClass(MVT.v4i64, X86GenRegisterInfo.VR256RegisterClass);

            setOperationAction(ISD.LOAD,               MVT.v8f32, Legal);
            setOperationAction(ISD.LOAD,               MVT.v8i32, Legal);
            setOperationAction(ISD.LOAD,               MVT.v4f64, Legal);
            setOperationAction(ISD.LOAD,               MVT.v4i64, Legal);
            setOperationAction(ISD.FADD,               MVT.v8f32, Legal);
            setOperationAction(ISD.FSUB,               MVT.v8f32, Legal);
            setOperationAction(ISD.FMUL,               MVT.v8f32, Legal);
            setOperationAction(ISD.FDIV,               MVT.v8f32, Legal);
            setOperationAction(ISD.FSQRT,              MVT.v8f32, Legal);
            setOperationAction(ISD.FNEG,               MVT.v8f32, Custom);
            //setOperationAction(ISD.BUILD_VECTOR,       MVT.v8f32, Custom);
            //setOperationAction(ISD.VECTOR_SHUFFLE,     MVT.v8f32, Custom);
            //setOperationAction(ISD.EXTRACT_VECTOR_ELT, MVT.v8f32, Custom);
            //setOperationAction(ISD.SELECT,             MVT.v8f32, Custom);
            //setOperationAction(ISD.VSETCC,             MVT.v8f32, Custom);

            // Operations to consider commented out -v16i16 v32i8
            //setOperationAction(ISD.ADD,                MVT.v16i16, Legal);
            setOperationAction(ISD.ADD,                MVT.v8i32, Custom);
            setOperationAction(ISD.ADD,                MVT.v4i64, Custom);
            //setOperationAction(ISD.SUB,                MVT.v32i8, Legal);
            //setOperationAction(ISD.SUB,                MVT.v16i16, Legal);
            setOperationAction(ISD.SUB,                MVT.v8i32, Custom);
            setOperationAction(ISD.SUB,                MVT.v4i64, Custom);
            //setOperationAction(ISD.MUL,                MVT.v16i16, Legal);
            setOperationAction(ISD.FADD,               MVT.v4f64, Legal);
            setOperationAction(ISD.FSUB,               MVT.v4f64, Legal);
            setOperationAction(ISD.FMUL,               MVT.v4f64, Legal);
            setOperationAction(ISD.FDIV,               MVT.v4f64, Legal);
            setOperationAction(ISD.FSQRT,              MVT.v4f64, Legal);
            setOperationAction(ISD.FNEG,               MVT.v4f64, Custom);

            setOperationAction(ISD.VSETCC,             MVT.v4f64, Custom);
            // setOperationAction(ISD.VSETCC,             MVT.v32i8, Custom);
            // setOperationAction(ISD.VSETCC,             MVT.v16i16, Custom);
            setOperationAction(ISD.VSETCC,             MVT.v8i32, Custom);

            // setOperationAction(ISD.SCALAR_TO_VECTOR,   MVT.v32i8, Custom);
            // setOperationAction(ISD.SCALAR_TO_VECTOR,   MVT.v16i16, Custom);
            // setOperationAction(ISD.INSERT_VECTOR_ELT,  MVT.v16i16, Custom);
            setOperationAction(ISD.INSERT_VECTOR_ELT,  MVT.v8i32, Custom);
            setOperationAction(ISD.INSERT_VECTOR_ELT,  MVT.v8f32, Custom);

            setOperationAction(ISD.BUILD_VECTOR,       MVT.v4f64, Custom);
            setOperationAction(ISD.BUILD_VECTOR,       MVT.v4i64, Custom);
            setOperationAction(ISD.VECTOR_SHUFFLE,     MVT.v4f64, Custom);
            setOperationAction(ISD.VECTOR_SHUFFLE,     MVT.v4i64, Custom);
            setOperationAction(ISD.INSERT_VECTOR_ELT,  MVT.v4f64, Custom);
            setOperationAction(ISD.EXTRACT_VECTOR_ELT, MVT.v4f64, Custom);

            // Not sure we want to do this since there are no 256-bit integer
            // operations in AVX

            // Custom lower build_vector, vector_shuffle, and extract_vector_elt.
            // This includes 256-bit vectors
            for (int i = (int)MVT.v16i8; i != (int)MVT.v4i64; ++i) {
                EVT VT = new EVT(i);

                // Do not attempt to custom lower non-power-of-2 vectors
                if (!Util.isPowerOf2(VT.getVectorNumElements()))
                    continue;

                setOperationAction(ISD.BUILD_VECTOR,       VT.getSimpleVT().simpleVT, Custom);
                setOperationAction(ISD.VECTOR_SHUFFLE,     VT.getSimpleVT().simpleVT, Custom);
                setOperationAction(ISD.EXTRACT_VECTOR_ELT, VT.getSimpleVT().simpleVT, Custom);
            }

            if (subtarget.is64Bit()) {
                setOperationAction(ISD.INSERT_VECTOR_ELT,  MVT.v4i64, Custom);
                setOperationAction(ISD.EXTRACT_VECTOR_ELT, MVT.v4i64, Custom);
            }

            // Not sure we want to do this since there are no 256-bit integer
            // operations in AVX

            // Promote v32i8, v16i16, v8i32 load, select, and, or, xor to v4i64.
            // Including 256-bit vectors
            for (int i = (int)MVT.v16i8; i != (int)MVT.v4i64; i++) {
                EVT VT = new EVT(i);

                if (!VT.is256BitVector()) {
                    continue;
                }
                setOperationAction(ISD.AND,    i, Promote);
                addPromotedToType (ISD.AND,    i, MVT.v4i64);
                setOperationAction(ISD.OR,     i, Promote);
                addPromotedToType (ISD.OR,     i, MVT.v4i64);
                setOperationAction(ISD.XOR,    i, Promote);
                addPromotedToType (ISD.XOR,    i, MVT.v4i64);
                setOperationAction(ISD.LOAD,   i, Promote);
                addPromotedToType (ISD.LOAD,   i, MVT.v4i64);
                setOperationAction(ISD.SELECT, i, Promote);
                addPromotedToType (ISD.SELECT, i, MVT.v4i64);
            }

            setTruncStoreAction(MVT.f64, MVT.f32, Expand);

        }

        // We want to custom lower some of our intrinsics.
        setOperationAction(ISD.INTRINSIC_WO_CHAIN, MVT.Other, Custom);

        // Add/Sub/Mul with overflow operations are custom lowered.
        setOperationAction(ISD.SADDO, MVT.i32, Custom);
        setOperationAction(ISD.SADDO, MVT.i64, Custom);
        setOperationAction(ISD.UADDO, MVT.i32, Custom);
        setOperationAction(ISD.UADDO, MVT.i64, Custom);
        setOperationAction(ISD.SSUBO, MVT.i32, Custom);
        setOperationAction(ISD.SSUBO, MVT.i64, Custom);
        setOperationAction(ISD.USUBO, MVT.i32, Custom);
        setOperationAction(ISD.USUBO, MVT.i64, Custom);
        setOperationAction(ISD.SMULO, MVT.i32, Custom);
        setOperationAction(ISD.SMULO, MVT.i64, Custom);

        if (!subtarget.is64Bit())
        {
            // These libcalls are not available in 32-bit.
            setLibCallName(RTLIB.SHL_I128, null);
            setLibCallName(RTLIB.SRL_I128, null);
            setLibCallName(RTLIB.SRA_I128, null);
        }

        // We have target-specific dag combine patterns for the following nodes:
        setTargetDAGCombine(ISD.VECTOR_SHUFFLE);
        setTargetDAGCombine(ISD.BUILD_VECTOR);
        setTargetDAGCombine(ISD.SELECT);
        setTargetDAGCombine(ISD.SHL);
        setTargetDAGCombine(ISD.SRA);
        setTargetDAGCombine(ISD.SRL);
        setTargetDAGCombine(ISD.STORE);
        setTargetDAGCombine(ISD.MEMBARRIER);
        if (subtarget.is64Bit())
            setTargetDAGCombine(ISD.MUL);

        computeRegisterProperties();

        // FIXME: These should be based on subtarget info. Plus, the values should
        // be smaller when we are in optimizing for size mode.
        maxStoresPerMemset = 16; // For @llvm.memset -> sequence of stores
        maxStoresPerMemcpy = 16; // For @llvm.memcpy -> sequence of stores
        maxStoresPerMemmove = 3; // For @llvm.memmove -> sequence of stores
        setPrefLoopAlignment = 16;
        benefitFromCodePlacementOpt = true;
    }

    @Override
    public X86MachineFunctionInfo createMachineFunctionInfo(MachineFunction mf)
    {
        return new X86MachineFunctionInfo(mf);
    }

    @Override
    public int getFunctionAlignment(Function fn)
    {
        return fn.hasFnAttr(Attribute.OptimizeForSize) ? 0 : 4;
    }

    @Override
    public String getTargetNodeName(int opcode)
    {
        switch (opcode)
        {  default: return null;
            case X86ISD.BSF:                return "X86ISD.BSF";
            case X86ISD.BSR:                return "X86ISD.BSR";
            case X86ISD.SHLD:               return "X86ISD.SHLD";
            case X86ISD.SHRD:               return "X86ISD.SHRD";
            case X86ISD.FAND:               return "X86ISD.FAND";
            case X86ISD.FOR:                return "X86ISD.FOR";
            case X86ISD.FXOR:               return "X86ISD.FXOR";
            case X86ISD.FSRL:               return "X86ISD.FSRL";
            case X86ISD.FILD:               return "X86ISD.FILD";
            case X86ISD.FILD_FLAG:          return "X86ISD.FILD_FLAG";
            case X86ISD.FP_TO_INT16_IN_MEM: return "X86ISD.FP_TO_INT16_IN_MEM";
            case X86ISD.FP_TO_INT32_IN_MEM: return "X86ISD.FP_TO_INT32_IN_MEM";
            case X86ISD.FP_TO_INT64_IN_MEM: return "X86ISD.FP_TO_INT64_IN_MEM";
            case X86ISD.FLD:                return "X86ISD.FLD";
            case X86ISD.FST:                return "X86ISD.FST";
            case X86ISD.CALL:               return "X86ISD.CALL";
            case X86ISD.RDTSC_DAG:          return "X86ISD.RDTSC_DAG";
            case X86ISD.BT:                 return "X86ISD.BT";
            case X86ISD.CMP:                return "X86ISD.CMP";
            case X86ISD.COMI:               return "X86ISD.COMI";
            case X86ISD.UCOMI:              return "X86ISD.UCOMI";
            case X86ISD.SETCC:              return "X86ISD.SETCC";
            case X86ISD.CMOV:               return "X86ISD.CMOV";
            case X86ISD.BRCOND:             return "X86ISD.BRCOND";
            case X86ISD.RET_FLAG:           return "X86ISD.RET_FLAG";
            case X86ISD.REP_STOS:           return "X86ISD.REP_STOS";
            case X86ISD.REP_MOVS:           return "X86ISD.REP_MOVS";
            case X86ISD.GlobalBaseReg:      return "X86ISD.GlobalBaseReg";
            case X86ISD.Wrapper:            return "X86ISD.Wrapper";
            case X86ISD.WrapperRIP:         return "X86ISD.WrapperRIP";
            case X86ISD.PEXTRB:             return "X86ISD.PEXTRB";
            case X86ISD.PEXTRW:             return "X86ISD.PEXTRW";
            case X86ISD.INSERTPS:           return "X86ISD.INSERTPS";
            case X86ISD.PINSRB:             return "X86ISD.PINSRB";
            case X86ISD.PINSRW:             return "X86ISD.PINSRW";
            case X86ISD.PSHUFB:             return "X86ISD.PSHUFB";
            case X86ISD.FMAX:               return "X86ISD.FMAX";
            case X86ISD.FMIN:               return "X86ISD.FMIN";
            case X86ISD.FRSQRT:             return "X86ISD.FRSQRT";
            case X86ISD.FRCP:               return "X86ISD.FRCP";
            case X86ISD.TLSADDR:            return "X86ISD.TLSADDR";
            case X86ISD.SegmentBaseAddress: return "X86ISD.SegmentBaseAddress";
            case X86ISD.EH_RETURN:          return "X86ISD.EH_RETURN";
            case X86ISD.TC_RETURN:          return "X86ISD.TC_RETURN";
            case X86ISD.FNSTCW16m:          return "X86ISD.FNSTCW16m";
            case X86ISD.LCMPXCHG_DAG:       return "X86ISD.LCMPXCHG_DAG";
            case X86ISD.LCMPXCHG8_DAG:      return "X86ISD.LCMPXCHG8_DAG";
            case X86ISD.ATOMADD64_DAG:      return "X86ISD.ATOMADD64_DAG";
            case X86ISD.ATOMSUB64_DAG:      return "X86ISD.ATOMSUB64_DAG";
            case X86ISD.ATOMOR64_DAG:       return "X86ISD.ATOMOR64_DAG";
            case X86ISD.ATOMXOR64_DAG:      return "X86ISD.ATOMXOR64_DAG";
            case X86ISD.ATOMAND64_DAG:      return "X86ISD.ATOMAND64_DAG";
            case X86ISD.ATOMNAND64_DAG:     return "X86ISD.ATOMNAND64_DAG";
            case X86ISD.VZEXT_MOVL:         return "X86ISD.VZEXT_MOVL";
            case X86ISD.VZEXT_LOAD:         return "X86ISD.VZEXT_LOAD";
            case X86ISD.VSHL:               return "X86ISD.VSHL";
            case X86ISD.VSRL:               return "X86ISD.VSRL";
            case X86ISD.CMPPD:              return "X86ISD.CMPPD";
            case X86ISD.CMPPS:              return "X86ISD.CMPPS";
            case X86ISD.PCMPEQB:            return "X86ISD.PCMPEQB";
            case X86ISD.PCMPEQW:            return "X86ISD.PCMPEQW";
            case X86ISD.PCMPEQD:            return "X86ISD.PCMPEQD";
            case X86ISD.PCMPEQQ:            return "X86ISD.PCMPEQQ";
            case X86ISD.PCMPGTB:            return "X86ISD.PCMPGTB";
            case X86ISD.PCMPGTW:            return "X86ISD.PCMPGTW";
            case X86ISD.PCMPGTD:            return "X86ISD.PCMPGTD";
            case X86ISD.PCMPGTQ:            return "X86ISD.PCMPGTQ";
            case X86ISD.ADD:                return "X86ISD.ADD";
            case X86ISD.SUB:                return "X86ISD.SUB";
            case X86ISD.SMUL:               return "X86ISD.SMUL";
            case X86ISD.UMUL:               return "X86ISD.UMUL";
            case X86ISD.INC:                return "X86ISD.INC";
            case X86ISD.DEC:                return "X86ISD.DEC";
            case X86ISD.MUL_IMM:            return "X86ISD.MUL_IMM";
            case X86ISD.PTEST:              return "X86ISD.PTEST";
            case X86ISD.VASTART_SAVE_XMM_REGS: return "X86ISD.VASTART_SAVE_XMM_REGS";
        }
    }

    @Override
    public SDValue lowerFormalArguments(SDValue chain, CallingConv callingConv,
            boolean varArg, ArrayList<InputArg> ins, SelectionDAG dag, ArrayList<SDValue> inVals)
    {
        MachineFunction mf = dag.getMachineFunction();
        X86MachineFunctionInfo fnInfo = (X86MachineFunctionInfo) mf.getInfo();
        Function fn = mf.getFunction();
        if (fn.hasExternalLinkage() && subtarget.isTargetCygMing() && fn.getName().equals("main"))
            fnInfo.setForceFramePointer(true);

        // Decorate the function name.
        fnInfo.setDecorationStyle(nameDeccorationCallConv(callingConv));

        MachineFrameInfo mfi = mf.getFrameInfo();
        boolean is64Bit = subtarget.is64Bit();
        boolean isWin64 = subtarget.isTargetWin64();

        assert !(varArg && callingConv
                == CallingConv.Fast) : "VarArg is not supported in Fast Calling convention";

        ArrayList<CCValAssign> argLocs = new ArrayList<>();
        CCState ccInfo = new CCState(callingConv, varArg, getTargetMachine(),
                argLocs);
        ccInfo.analyzeFormalArguments(ins, ccAssignFnForNode(callingConv));

        int lastVal = ~0;
        SDValue argValue;
        for (int i = 0, e = argLocs.size(); i < e; i++)
        {
            CCValAssign va = argLocs.get(i);
            assert va.getValNo() != lastVal;
            lastVal = va.getValNo();

            if (va.isRegLoc())
            {
                EVT regVT = va.getLocVT();
                TargetRegisterClass rc = null;
                int simpleVT = regVT.getSimpleVT().simpleVT;
                if (simpleVT == MVT.i32)
                    rc = X86GenRegisterInfo.GR32RegisterClass;
                else if (simpleVT == MVT.i64 && is64Bit)
                    rc = X86GenRegisterInfo.GR64RegisterClass;
                else if (simpleVT == MVT.f32)
                    rc = X86GenRegisterInfo.FR32RegisterClass;
                else if (simpleVT == MVT.f64)
                    rc = X86GenRegisterInfo.FR64RegisterClass;
                else if (regVT.isVector() && regVT.getSizeInBits() == 128)
                    rc = X86GenRegisterInfo.VR128RegisterClass;
                else if (regVT.isVector() && regVT.getSizeInBits() == 64)
                    rc = X86GenRegisterInfo.VR64RegisterClass;
                else
                    Util.shouldNotReachHere("Unknown argument type!");

                int reg = mf.addLiveIn(va.getLocReg(), rc);
                argValue = dag.getCopyFromReg(chain, reg, regVT);

                if (va.getLocInfo() == CCValAssign.LocInfo.SExt)
                    argValue = dag.getNode(ISD.AssertSext, regVT, argValue,
                            dag.getValueType(va.getValVT()));
                else if (va.getLocInfo() == CCValAssign.LocInfo.ZExt)
                    argValue = dag.getNode(ISD.AssertZext, regVT, argValue,
                            dag.getValueType(va.getValVT()));
                else if (va.getLocInfo() == CCValAssign.LocInfo.BCvt)
                    argValue = dag
                            .getNode(ISD.BIT_CONVERT, va.getValVT(), argValue);

                if (va.isExtInLoc())
                {
                    // Handle MMX values passed in XMM regs.
                    if (regVT.isVector())
                    {
                        argValue = dag.getNode(ISD.EXTRACT_VECTOR_ELT, new EVT(MVT.i64),
                                argValue,
                                dag.getConstant(0, new EVT(MVT.i64), false));
                        argValue = dag.getNode(ISD.BIT_CONVERT, va.getValVT(),
                                argValue);
                    }
                    else
                        argValue = dag.getNode(ISD.TRUNCATE, va.getValVT(), argValue);
                }
            }
            else
            {
                assert va.isMemLoc();
                argValue = lowerMemArgument(chain, callingConv, ins, dag, va,
                        mfi, i);
            }

            if (va.getLocInfo() == CCValAssign.LocInfo.Indirect)
                argValue = dag.getLoad(va.getValVT(), chain, argValue, null, 0);

            inVals.add(argValue);
        }

        if (is64Bit && mf.getFunction().hasStructRetAttr())
        {
            X86MachineFunctionInfo funcInfo = (X86MachineFunctionInfo) mf.getInfo();
            int reg = funcInfo.getSRetReturnReg();
            if (reg == 0)
            {
                reg = mf.getMachineRegisterInfo().
                        createVirtualRegister(getRegClassFor(new EVT(MVT.i64)));
                funcInfo.setSRetReturnReg(reg);
            }

            SDValue copy = dag.getCopyToReg(dag.getEntryNode(), reg, inVals.get(0));
            chain = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), copy, chain);
        }

        int stackSize = ccInfo.getNextStackOffset();
        if (EnablePerformTailCallOpt.value && callingConv == CallingConv.Fast)
            stackSize = getAlignedArgumentStackSize(stackSize, dag);

        if (varArg)
        {
            if (is64Bit | callingConv != CallingConv.X86_FastCall)
            {
                varArgsFrameIndex = mfi.createFixedObject(1, stackSize);
            }
            if (is64Bit)
            {
                int totalNumIntRegs = 0, totalNumXMMRegs = 0;

                // FIXME: We should really autogenerate these arrays
                int[] GPR64ArgRegsWin64 = { X86GenRegisterNames.RCX,
                        X86GenRegisterNames.RDX, X86GenRegisterNames.R8,
                        X86GenRegisterNames.R9 };

                int[] XMMArgRegsWin64 = { X86GenRegisterNames.XMM0,
                        X86GenRegisterNames.XMM1, X86GenRegisterNames.XMM2,
                        X86GenRegisterNames.XMM3 };

                int[] GPR64ArgRegs64Bit = {
                        X86GenRegisterNames.RDI, X86GenRegisterNames.RSI,
                        X86GenRegisterNames.RDX, X86GenRegisterNames.RCX,
                        X86GenRegisterNames.R8, X86GenRegisterNames.R9 };

                int[] XMMArgRegs64Bit = { X86GenRegisterNames.XMM0,
                        X86GenRegisterNames.XMM1, X86GenRegisterNames.XMM2,
                        X86GenRegisterNames.XMM3, X86GenRegisterNames.XMM4,
                        X86GenRegisterNames.XMM5, X86GenRegisterNames.XMM6,
                        X86GenRegisterNames.XMM7 };

                int[] gpr64ArgRegs, xmmArgRegs;

                if (isWin64)
                {
                    totalNumIntRegs = 4;
                    totalNumXMMRegs = 4;
                    gpr64ArgRegs = GPR64ArgRegsWin64;
                    xmmArgRegs = XMMArgRegsWin64;
                }
                else
                {
                    totalNumIntRegs = 6;
                    totalNumXMMRegs = 8;
                    gpr64ArgRegs = GPR64ArgRegs64Bit;
                    xmmArgRegs = XMMArgRegs64Bit;
                }

                int numIntRegs = ccInfo.getFirstUnallocated(gpr64ArgRegs);
                int numXMMRegs = ccInfo.getFirstUnallocated(xmmArgRegs);

                boolean notImplicitFloatOps = fn.hasFnAttr(Attribute.NoImplicitFloat);
                assert !(numXMMRegs != 0 && !subtarget.hasSSE1());
                assert !(numXMMRegs != 0 && notImplicitFloatOps);
                if (notImplicitFloatOps || !subtarget.hasSSE1())
                    totalNumXMMRegs = 0;

                varArgsGPOffset = numIntRegs * 8;
                varArgsFPOffset = totalNumIntRegs*8 + numXMMRegs*16;
                regSaveFrameIndex = mfi.createStackObject(
                        totalNumIntRegs*8+totalNumXMMRegs*16, 16);
                ArrayList<SDValue> memOps = new ArrayList<>();
                SDValue rsfin = dag.getFrameIndex(regSaveFrameIndex, new EVT(getPointerTy()), false);
                int offset = varArgsGPOffset;
                for (; numIntRegs != totalNumIntRegs; ++numIntRegs)
                {
                    SDValue fin = dag.getNode(ISD.ADD, new EVT(getPointerTy()), rsfin,
                            dag.getIntPtrConstant(offset));
                    int vreg = mf.addLiveIn(gpr64ArgRegs[numIntRegs], X86GenRegisterInfo.GR64RegisterClass);
                    SDValue val = dag.getCopyFromReg(chain, vreg, new EVT(MVT.i64));
                    SDValue store = dag.getStore(val.getValue(1), val, fin,
                            PseudoSourceValue.getFixedStack(regSaveFrameIndex),
                            offset, false, 0);
                    memOps.add(store);
                    offset += 8;
                }

                if (totalNumXMMRegs != 0 && numXMMRegs != totalNumXMMRegs)
                {
                    ArrayList<SDValue> savedXMMOps = new ArrayList<>();
                    savedXMMOps.add(chain);

                    int al = mf.addLiveIn(X86GenRegisterNames.AL, X86GenRegisterInfo.GR8RegisterClass);
                    SDValue alVal = dag.getCopyFromReg(dag.getEntryNode(), al, new EVT(MVT.i8));
                    savedXMMOps.add(alVal);

                    savedXMMOps.add(dag.getIntPtrConstant(regSaveFrameIndex));
                    savedXMMOps.add(dag.getIntPtrConstant(varArgsFPOffset));

                    for (; numXMMRegs != totalNumXMMRegs; ++numXMMRegs)
                    {
                        int vreg = mf.addLiveIn(xmmArgRegs[numXMMRegs], X86GenRegisterInfo.VR128RegisterClass);
                        SDValue val = dag.getCopyFromReg(chain, vreg, new EVT(MVT.v4f32));
                        savedXMMOps.add(val);
                    }

                    memOps.add(dag.getNode(X86ISD.VASTART_SAVE_XMM_REGS, new EVT(MVT.Other),
                            savedXMMOps));
                }

                if (!memOps.isEmpty())
                    chain = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), memOps);
            }
        }
        if (isCalleePop(varArg, callingConv))
        {
            bytesToPopOnReturn = stackSize;
            bytesCallerReserves = 0;
        }
        else
        {
            bytesToPopOnReturn = 0;
            if (!is64Bit && callingConv != CallingConv.Fast && argsAreStructReturn(ins))
                bytesToPopOnReturn = 4;
            bytesCallerReserves = stackSize;
        }

        if (!is64Bit)
        {
            regSaveFrameIndex = 0xAAAAAAA;
            if (callingConv == CallingConv.X86_FastCall)
                varArgsFrameIndex = 0xAAAAAAA;
        }
        fnInfo.setBytesToPopOnReturn(bytesToPopOnReturn);
        return chain;
    }

    private int getAlignedArgumentStackSize(int stackSize, SelectionDAG dag)
    {
        MachineFunction mf = dag.getMachineFunction();
        TargetMachine tm = mf.getTarget();
        TargetFrameInfo tfi = tm.getFrameInfo();
        int stackAlign = tfi.getStackAlignment();
        int alignMask = stackAlign - 1;
        int offset = stackSize;
        int slotSize = td.getPointerSize();
        if ((offset & alignMask) <= (stackAlign - slotSize))
            offset += (stackAlign - slotSize) - (offset & alignMask);
        else
        {
            offset = (~alignMask&offset) + stackAlign + stackAlign - slotSize;
        }
        return offset;
    }

    private boolean isCalleePop(boolean isVarArg, CallingConv cc)
    {
        if (isVarArg) return false;

        switch (cc)
        {
            default: return false;
            case X86_StdCall:
                return !subtarget.is64Bit();
            case X86_FastCall:
                return !subtarget.is64Bit();
            case Fast:
                return EnablePerformTailCallOpt.value;
        }
    }

    static boolean argsAreStructReturn(ArrayList<InputArg> ins)
    {
        if (ins.isEmpty()) return false;

        return ins.get(0).flags.isSRet();
    }

    @Override
    public SDValue lowerMemArgument(SDValue chain, CallingConv cc,
            ArrayList<InputArg> ins, SelectionDAG dag, CCValAssign va,
            MachineFrameInfo mfi, int i)
    {
        ArgFlagsTy flags = ins.get(i).flags;

        boolean alwaysUeMutable = cc == CallingConv.Fast && EnablePerformTailCallOpt.value;
        boolean isImmutable = !alwaysUeMutable && !flags.isByVal();

        EVT valVT;
        if (va.getLocInfo() == CCValAssign.LocInfo.Indirect)
            valVT = va.getLocVT();
        else
            valVT = va.getValVT();

        int fi = mfi.createFixedObject(valVT.getSizeInBits()/8, va.getLocMemOffset(), isImmutable);
        SDValue fin = dag.getFrameIndex(fi, new EVT(getPointerTy()), false);
        if (flags.isByVal())
            return fin;
        return dag.getLoad(valVT, chain, fin, PseudoSourceValue.getFixedStack(fi), 0);
    }

    private CCAssignFn ccAssignFnForNode(CallingConv cc)
    {
        if (subtarget.is64Bit())
        {
            if (subtarget.isTargetWin64())
                return CC_X86_Win64_C;
            else
                return CC_X86_64_C;
        }

        if (cc == CallingConv.X86_FastCall)
            return CC_X86_32_FastCall;
        else if (cc == CallingConv.Fast)
            return CC_X86_32_FastCC;
        else
            return CC_X86_32_C;
    }

    private NameDecorationStyle nameDeccorationCallConv(
            CallingConv cc)
    {
        if (cc == CallingConv.X86_FastCall)
            return NameDecorationStyle.FastCall;
        else if (cc == CallingConv.X86_StdCall)
            return NameDecorationStyle.StdCall;
        return NameDecorationStyle.None;
    }

    @Override
    public void computeMaskedBitsForTargetNode(SDValue op,
            APInt mask,
            APInt[] knownVals,
            SelectionDAG selectionDAG,
            int depth)
    {
        int opc = op.getOpcode();
        assert opc >= ISD.BUILTIN_OP_END ||
                opc == ISD.INTRINSIC_WO_CHAIN ||
                opc == ISD.INTRINSIC_W_CHAIN ||
                opc == ISD.INTRINSIC_VOID;

        knownVals[0] = knownVals[1] = new APInt(mask.getBitWidth(), 0);
        switch (opc)
        {
            default: break;
            case X86ISD.ADD:
            case X86ISD.SUB:
            case X86ISD.SMUL:
            case X86ISD.UMUL:
            case X86ISD.INC:
            case X86ISD.DEC:
                // These nodes' second result is a boolean.
                if (op.getResNo() == 0)
                    break;
                // Fallthrough
            case X86ISD.SETCC:
                knownVals[0] =knownVals[0].or(APInt.getHighBitsSet(mask.getBitWidth(),
                    mask.getBitWidth() - 1));
                break;
        }
    }

    public boolean isScalarFPTypeInSSEReg(EVT vt)
    {
        int simpleVT = vt.getSimpleVT().simpleVT;
        return (simpleVT == MVT.f64 && x86ScalarSSEf64) ||
                simpleVT == MVT.f32 && x86ScalarSSEf32;
    }

    public SDValue lowerReturn(SDValue chain,
            CallingConv cc,
            boolean isVarArg,
            ArrayList<OutputArg> outs,
            SelectionDAG dag)
    {
        ArrayList<CCValAssign> rvLocs = new ArrayList<>();
        CCState ccInfo = new CCState(cc, isVarArg, getTargetMachine(), rvLocs);
        ccInfo.analyzeReturn(outs, RetCC_X86);
        MachineFunction mf = dag.getMachineFunction();
        if (mf.getMachineRegisterInfo().isLiveOutEmpty())
        {
            for (int i = 0; i < rvLocs.size(); i++)
            {
                if (rvLocs.get(i).isRegLoc())
                    mf.getMachineRegisterInfo().addLiveOut(rvLocs.get(i).getLocReg());
            }
        }

        SDValue flag = new SDValue();
        ArrayList<SDValue> retOps = new ArrayList<>();
        retOps.add(chain);
        retOps.add(dag.getConstant(bytesToPopOnReturn, new EVT(MVT.i16), false));

        for (int i = 0; i < rvLocs.size(); i++)
        {
            CCValAssign va = rvLocs.get(i);
            assert va.isRegLoc();
            SDValue valToCopy = outs.get(i).val;

            if (va.getLocReg() == X86GenRegisterNames.ST0 ||
                    va.getLocReg() == X86GenRegisterNames.ST1)
            {
                if (isScalarFPTypeInSSEReg(va.getValVT()))
                    valToCopy = dag.getNode(ISD.FP_EXTEND, new EVT(MVT.f80), valToCopy);
                retOps.add(valToCopy);
                continue;
            }

            // 64-bit vector (MMX) values are returned in XMM0 / XMM1 except for v1i64
            // which is returned in RAX / RDX.
            if (subtarget.is64Bit())
            {
                EVT valVT = valToCopy.getValueType();
                if (valVT.isVector() && valVT.getSizeInBits() == 64)
                {
                    valToCopy = dag.getNode(ISD.BIT_CONVERT, new EVT(MVT.i64), valToCopy);
                    if (va.getLocReg() == X86GenRegisterNames.XMM0
                            || va.getLocReg() == X86GenRegisterNames.XMM1)
                        valToCopy = dag.getNode(ISD.SCALAR_TO_VECTOR, new EVT(MVT.v2i64), valToCopy);
                }
            }

            chain = dag.getCopyToReg(chain, va.getLocReg(), valToCopy, flag);
            flag = chain.getValue(1);
        }

        // The x86-64 ABI for returning structs by value requires that we copy
        // the sret argument into %rax for the return. We saved the argument into
        // a virtual register in the entry block, so now we copy the value out
        // and into %rax.
        if (subtarget.is64Bit() &&
                mf.getFunction().hasStructRetAttr())
        {
            X86MachineFunctionInfo funcInfo = (X86MachineFunctionInfo) mf.getInfo();
            int reg = funcInfo.getSRetReturnReg();
            if (reg == 0)
            {
                reg = mf.getMachineRegisterInfo().createVirtualRegister(getRegClassFor(new EVT(MVT.i64)));
                funcInfo.setSRetReturnReg(reg);
            }
            SDValue val = dag.getCopyFromReg(chain, reg, new EVT(getPointerTy()));
            chain = dag.getCopyToReg(chain, X86GenRegisterNames.RAX, val, flag);
            flag = chain.getValue(1);
        }
        retOps.set(0, chain);
        if (flag.getNode() != null)
            retOps.add(flag);

        return dag.getNode(X86ISD.RET_FLAG, new EVT(MVT.Other), retOps);
    }

    @Override
    public boolean isTruncateFree(Type ty1, Type ty2)
    {
        if (!ty1.isInteger() || !ty2.isInteger())
            return false;

        int numBit1 = ty1.getPrimitiveSizeInBits();
        int numBit2 = ty2.getPrimitiveSizeInBits();
        if (numBit1 <= numBit2)
            return false;
        return subtarget.is64Bit() || numBit1 < 64;
    }

    @Override
    public boolean isTruncateFree(EVT vt1, EVT vt2)
    {
        if (!vt1.isInteger() || !vt2.isInteger())
            return false;

        int numBits1 = vt1.getSizeInBits();
        int numBits2 = vt2.getSizeInBits();
        if (numBits1 <= numBits2)
            return false;

        return subtarget.is64Bit() || numBits1 < 64;
    }

    @Override
    public boolean isZExtFree(Type ty1, Type ty2)
    {
        return ty1.equals(LLVMContext.Int32Ty) &&
                ty2.equals(LLVMContext.Int64Ty) && subtarget.is64Bit();
    }

    @Override
    public boolean isZExtFree(EVT vt1, EVT vt2)
    {
        return vt1.getSimpleVT().simpleVT == MVT.i32 &&
                vt2.getSimpleVT().simpleVT == MVT.i64 &&
                subtarget.is64Bit();
    }

    @Override
    public boolean isEligibleTailCallOptimization(
            SDValue calle,
            CallingConv calleeCC,
            boolean isVarArg,
            ArrayList<InputArg> ins,
            SelectionDAG dag)
    {
        MachineFunction mf = dag.getMachineFunction();
        CallingConv callerCC = mf.getFunction().getCallingConv();
        return calleeCC == CallingConv.Fast && calleeCC == callerCC;
    }

    @Override
    public SDValue lowerCall(SDValue chain,
            SDValue callee,
            CallingConv cc,
            boolean isVarArg,
            boolean isTailCall,
            ArrayList<OutputArg> outs,
            ArrayList<InputArg> ins,
            SelectionDAG dag,
            ArrayList<SDValue> inVals)
    {
        MachineFunction mf = dag.getMachineFunction();
        boolean is64Bit = subtarget.is64Bit();
        boolean isStructRet = callIsStructReturn(outs);
        assert !isTailCall || (cc == CallingConv.Fast
                && EnablePerformTailCallOpt.value) : "isEligibleForTailCallOptimization missed a case!";
        assert !(isVarArg && cc
                == CallingConv.Fast) : "Var args not supported with calling convention fastcc!";
        ArrayList<CCValAssign> argLocs = new ArrayList<>();
        CCState ccInfo = new CCState(cc, isVarArg, getTargetMachine(), argLocs);
        ccInfo.analyzeCallOperands(outs, ccAssignFnForNode(cc));

        int numBytes = ccInfo.getNextStackOffset();
        if (EnablePerformTailCallOpt.value && cc == CallingConv.Fast)
            numBytes = getAlignedArgumentStackSize(numBytes, dag);

        int fpDiff = 0;
        if (isTailCall)
        {
            // Lower arguments at fp - stackoffset + fpdiff.
            int numBytesCallerPushed = ((X86MachineFunctionInfo) mf.getInfo()).
                    getBytesToPopOnReturn();
            fpDiff = numBytesCallerPushed - numBytes;

            if (fpDiff < ((X86MachineFunctionInfo) mf.getInfo()).getTCReturnAddrDelta())
                ((X86MachineFunctionInfo) mf.getInfo()).setTCReturnAddrDelta(fpDiff);
        }

        chain = dag.getCALLSEQ_START(chain, dag.getIntPtrConstant(numBytes, true));

        OutParamWrapper<SDValue> retAddrFrIdx = new OutParamWrapper<>();
        chain = emitTailCallLoadRetAddr(dag, retAddrFrIdx, chain, isTailCall,
                is64Bit, fpDiff);

        ArrayList<Pair<Integer, SDValue>> regsToPass = new ArrayList<>();
        ArrayList<SDValue> memOpChains = new ArrayList<>();
        SDValue stackPtr = new SDValue();

        for (int i = 0, e = argLocs.size(); i < e; i++)
        {
            CCValAssign va = argLocs.get(i);
            EVT regVT = va.getLocVT();
            SDValue arg = outs.get(i).val;
            ArgFlagsTy flags = outs.get(i).flags;
            boolean isByVal = flags.isByVal();

            // Promote the value if desired.
            switch (va.getLocInfo())
            {
                default:
                    Util.shouldNotReachHere("Unknown loc info!");
                case Full:
                    break;
                case SExt:
                    arg = dag.getNode(ISD.SIGN_EXTEND, regVT, arg);
                    break;
                case ZExt:
                    arg = dag.getNode(ISD.SIGN_EXTEND, regVT, arg);
                    break;
                case AExt:
                    if (regVT.isVector() && regVT.getSizeInBits() == 128)
                    {
                        // special case: passing MMX values in XMM register.
                        arg = dag.getNode(ISD.BIT_CONVERT, new EVT(MVT.i64), arg);
                        arg = dag.getNode(ISD.SCALAR_TO_VECTOR, new EVT(MVT.v2i64), arg);
                        arg = getMOVL(dag, new EVT(MVT.v2i64), dag.getUNDEF(new EVT(MVT.v2i64)), arg);
                    }
                    else
                        arg = dag.getNode(ISD.ANY_EXTEND, regVT, arg);
                    break;
                case BCvt:
                    arg = dag.getNode(ISD.BIT_CONVERT, regVT, arg);
                    break;
                case Indirect:
                {
                    SDValue spillSlot = dag.createStackTemporary(va.getValVT());
                    int fi = ((FrameIndexSDNode) spillSlot.getNode()).getFrameIndex();
                    chain = dag.getStore(chain, arg, spillSlot,
                            PseudoSourceValue.getFixedStack(fi), 0, false, 0);
                    arg = spillSlot;
                    break;
                }
            }
            if (va.isRegLoc())
                regsToPass.add(Pair.get(va.getLocReg(), arg));
            else
            {
                if (!isTailCall || (isTailCall && isByVal))
                {
                    assert va.isMemLoc();
                    if (stackPtr.getNode() == null)
                    {
                        stackPtr = dag.getCopyFromReg(chain, x86StackPtr, new EVT(getPointerTy()));
                    }
                    memOpChains.add(lowerMemOpCallTo(chain, stackPtr, arg, dag, va,
                                    flags));
                }
            }
        }

        if (!memOpChains.isEmpty())
            chain = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), memOpChains);

        SDValue inFlag = new SDValue();
        if (isTailCall)
        {
            for (int i = 0, e = regsToPass.size(); i < e; i++)
            {
                chain = dag.getCopyToReg(chain, regsToPass.get(i).first,
                        regsToPass.get(i).second, inFlag);
                inFlag = chain.getValue(1);
            }
        }

        if (subtarget.isPICStyleGOT())
        {
            if (!isTailCall)
            {
                chain = dag.getCopyToReg(chain, X86GenRegisterNames.EBX,
                        dag.getNode(X86ISD.GlobalBaseReg,
                                new EVT(getPointerTy())));
                inFlag = chain.getValue(1);
            }
            else
            {
                GlobalAddressSDNode gr = callee
                        .getNode() instanceof GlobalAddressSDNode ?
                        (GlobalAddressSDNode) callee.getNode() :
                        null;
                if (gr != null && !gr.getGlobalValue().hasHiddenVisibility())
                    callee = lowerGlobalAddress(callee, dag);
                else if (callee.getNode() instanceof ExternalSymbolSDNode)
                    callee = lowerExternalSymbol(callee, dag);
            }
        }

        if (is64Bit && isVarArg)
        {
            int[] XMMArgRegs = { X86GenRegisterNames.XMM0, X86GenRegisterNames.XMM1,
                    X86GenRegisterNames.XMM2, X86GenRegisterNames.XMM3,
                    X86GenRegisterNames.XMM4, X86GenRegisterNames.XMM5,
                    X86GenRegisterNames.XMM6, X86GenRegisterNames.XMM7, };
            int numXMMRegs = ccInfo.getFirstUnallocated(XMMArgRegs);
            assert subtarget.hasSSE1() || numXMMRegs == 0;
            chain = dag.getCopyToReg(chain, X86GenRegisterNames.AL,
                    dag.getConstant(numXMMRegs, new EVT(MVT.i8), false), inFlag);
            inFlag = chain.getValue(1);
        }

        if (isTailCall)
        {
            SDValue argChain = dag.getStackArgumentTokenFactor(chain);
            ArrayList<SDValue> memOpChains2 = new ArrayList<>();
            SDValue fin = new SDValue();
            int fi = 0;
            inFlag = new SDValue();
            for (int i = 0, e = argLocs.size(); i < e; i++)
            {
                CCValAssign va = argLocs.get(i);
                if (!va.isRegLoc())
                {
                    assert va.isMemLoc();
                    SDValue arg = outs.get(i).val;
                    ArgFlagsTy flags = outs.get(i).flags;

                    int offset = va.getLocMemOffset() + fpDiff;
                    int opSize = (va.getLocVT().getSizeInBits() + 7) / 8;
                    fi = mf.getFrameInfo().createFixedObject(opSize, offset);
                    fin = dag.getFrameIndex(fi, new EVT(getPointerTy()), false);

                    if (flags.isByVal())
                    {
                        SDValue source = dag.getIntPtrConstant(va.getLocMemOffset());
                        if (stackPtr.getNode() == null)
                        {
                            stackPtr = dag.getCopyFromReg(chain, x86StackPtr,
                                    new EVT(getPointerTy()));
                        }
                        source = dag.getNode(ISD.ADD, new EVT(getPointerTy()),
                                stackPtr, source);
                        memOpChains2.add(createCopyOfByValArgument(source, fin,
                                argChain, flags, dag));
                    }
                    else
                    {
                        memOpChains2.add(dag.getStore(argChain, arg, fin,
                                PseudoSourceValue.getFixedStack(fi), 0, false, 0));
                    }
                }
            }

            if (!memOpChains2.isEmpty())
                chain = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other),
                        memOpChains2);

            for (int i = 0, e = regsToPass.size(); i < e; i++)
            {
                chain = dag.getCopyToReg(chain, regsToPass.get(i).first,
                        regsToPass.get(i).second, inFlag);
                inFlag = chain.getValue(1);
            }
            inFlag = new SDValue();

            chain = emitTailCallStoreRetAddr(dag, mf, chain, retAddrFrIdx.get(),
                    is64Bit, fpDiff);
        }

        if (callee.getNode() instanceof GlobalAddressSDNode)
        {
            GlobalAddressSDNode gs = (GlobalAddressSDNode)callee.getNode();
            GlobalValue gv = gs.getGlobalValue();

            // default in non windows platform.
            int opFlags = 0;
            if (subtarget.isTargetELF() && getTargetMachine().getRelocationModel() == PIC_
                    && gv.hasDefaultVisibility() && !gv.hasLocalLinkage())
            {
                opFlags = X86II.MO_PLT;
            }
            else if (subtarget.isPICStyleStubAny() &&
                    (gv.isDeclaration() || gv.isWeakForLinker()) &&
                    subtarget.getDarwinVers() < 9)
            {
                opFlags = X86II.MO_DARWIN_STUB;
            }
            callee = dag.getTargetGlobalAddress(gv, new EVT(getPointerTy()), gs.getOffset(), opFlags);
        }
        else if (callee.getNode() instanceof ExternalSymbolSDNode)
        {
            ExternalSymbolSDNode es = (ExternalSymbolSDNode)callee.getNode();
            int opFlags = 0;
            if (subtarget.isTargetELF() && getTargetMachine().getRelocationModel() == PIC_)
            {
                opFlags = X86II.MO_PLT;
            }
            else if (subtarget.isPICStyleStubAny() &&
                    subtarget.getDarwinVers() < 9)
            {
                opFlags = X86II.MO_DARWIN_STUB;
            }
            callee = dag.getTargetExternalSymbol(es.getExtSymol(), new EVT(getPointerTy()),
                    opFlags);
        }
        else if (isTailCall)
        {
            int calleeReg = is64Bit ? X86GenRegisterNames.R11: X86GenRegisterNames.EAX;
            chain = dag.getCopyToReg(chain, dag.getRegister(calleeReg, new EVT(getPointerTy())),
                    callee, inFlag);
            callee = dag.getRegister(calleeReg, new EVT(getPointerTy()));
            mf.getMachineRegisterInfo().addLiveOut(calleeReg);
        }

        SDVTList vts = dag.getVTList(new EVT(MVT.Other), new EVT(MVT.Flag));
        ArrayList<SDValue> ops = new ArrayList<>();
        if (isTailCall)
        {
            chain = dag.getCALLSEQ_END(chain, dag.getIntPtrConstant(numBytes, true),
                    dag.getIntPtrConstant(0, true), inFlag);
            inFlag = chain.getValue(1);
        }

        ops.add(chain);
        ops.add(callee);

        if (isTailCall)
            ops.add(dag.getConstant(fpDiff, new EVT(MVT.i32), false));

        regsToPass.forEach(pair->ops.add(dag.getRegister(pair.first, pair.second.getValueType())));

        if (!isTailCall && subtarget.isPICStyleGOT())
            ops.add(dag.getRegister(X86GenRegisterNames.EBX, new EVT(getPointerTy())));

        if (is64Bit && isVarArg)
            ops.add(dag.getRegister(X86GenRegisterNames.AL, new EVT(MVT.i8)));

        if (inFlag.getNode() != null)
            ops.add(inFlag);

        if (isTailCall)
        {
            if (mf.getMachineRegisterInfo().isLiveOutEmpty())
            {
                ArrayList<CCValAssign> rvLocs = new ArrayList<>();
                ccInfo = new CCState(cc, isVarArg, getTargetMachine(), rvLocs);
                ccInfo.analyzeCallResult(ins, RetCC_X86);
                for (CCValAssign va : rvLocs)
                {
                    if (va.isRegLoc())
                        mf.getMachineRegisterInfo().addLiveOut(va.getLocReg());
                }
            }
            int calleeReg;
            assert (callee.getOpcode() == ISD.Register &&
                    (calleeReg = ((RegisterSDNode)callee.getNode()).getReg()) != 0 &&
                    (calleeReg == X86GenRegisterNames.EAX || calleeReg == X86GenRegisterNames.R9)) ||
                    callee.getOpcode() == ISD.TargetExternalSymbol ||
                    callee.getOpcode() == ISD.TargetGlobalAddress;

            return dag.getNode(X86ISD.TC_RETURN, vts, ops);
        }

        chain = dag.getNode(X86ISD.CALL, vts, ops);
        inFlag = chain.getValue(1);

        int numBytesForCalleeToPush = 0;
        if (isCalleePop(isVarArg, cc))
            numBytesForCalleeToPush = numBytes;
        else if (!is64Bit && cc != CallingConv.Fast && isStructRet)
        {
            numBytesForCalleeToPush = 4;
        }
        chain = dag.getCALLSEQ_END(chain, dag.getIntPtrConstant(numBytes, true),
                dag.getIntPtrConstant(numBytesForCalleeToPush, true), inFlag);
        inFlag = chain.getValue(1);

        return lowerCallResult(chain, inFlag, cc, isVarArg, ins, dag, inVals);
    }

    private SDValue lowerCallResult(SDValue chain, SDValue inFlag,
            CallingConv cc, boolean isVarArg,
            ArrayList<InputArg> ins,
            SelectionDAG dag,
            ArrayList<SDValue> inVals)
    {
        ArrayList<CCValAssign> rvLocs = new ArrayList<>();
        boolean is64Bit = subtarget.is64Bit();
        CCState ccInfo = new CCState(cc, isVarArg, getTargetMachine(), rvLocs);
        ccInfo.analyzeCallResult(ins, RetCC_X86);

        for (int i = 0, e = rvLocs.size(); i < e; i++)
        {
            CCValAssign va = rvLocs.get(i);
            EVT copyVT = va.getValVT();

            if ((copyVT.getSimpleVT().simpleVT == MVT.f32 ||
                    copyVT.getSimpleVT().simpleVT == MVT.f64)
                    && (is64Bit || ins.get(i).flags.isInReg()) &&
                    !subtarget.hasSSE1())
            {
                Util.shouldNotReachHere("SSE register return with SSE disabled!");
            }

            if ((va.getLocReg() == X86GenRegisterNames.ST0 ||
                    va.getLocReg() == X86GenRegisterNames.ST1) &&
                    isScalarFPTypeInSSEReg(va.getValVT()))
            {
                copyVT = new EVT(MVT.f80);
            }

            SDValue val;
            if (is64Bit && copyVT.isVector() && copyVT.getSizeInBits() == 64)
            {
                // for X86-64, MMX values are returned in XMM0/XMM1 execpt for v1i16.
                if (va.getLocReg() == X86GenRegisterNames.XMM0 ||
                        va.getLocReg() == X86GenRegisterNames.XMM1)
                {
                    chain = dag.getCopyFromReg(chain, va.getLocReg(),
                            new EVT(MVT.v2i64), inFlag).getValue(1);
                    val = chain.getValue(0);
                    val = dag.getNode(ISD.EXTRACT_VECTOR_ELT, new EVT(MVT.i64),
                            val, dag.getConstant(0, new EVT(MVT.i64), false));
                }
                else
                {
                    chain = dag.getCopyFromReg(chain, va.getLocReg(), new EVT(MVT.i64),
                            inFlag).getValue(1);
                    val = chain.getValue(0);
                }
                val = dag.getNode(ISD.BIT_CONVERT, copyVT, val);
            }
            else
            {
                chain = dag.getCopyFromReg(chain, va.getLocReg(), copyVT, inFlag).getValue(1);
                val = chain.getValue(0);
            }
            inFlag = chain.getValue(2);

            if (!Objects.equals(copyVT, va.getValVT()))
            {
                val = dag.getNode(ISD.FP_EXTEND, va.getValVT(), val,
                        dag.getIntPtrConstant(1));
            }
            inVals.add(val);
        }
        return chain;
    }

    private SDValue lowerMemOpCallTo(SDValue chain, SDValue stackPtr, SDValue arg,
            SelectionDAG dag, CCValAssign va, ArgFlagsTy flags)
    {
        int firstStackArgOffset = subtarget.isTargetWin64() ? 32 : 0;
        int locMemOffset = firstStackArgOffset + va.getLocMemOffset();
        SDValue ptrOff = dag.getIntPtrConstant(locMemOffset);
        ptrOff = dag.getNode(ISD.ADD, new EVT(getPointerTy()), stackPtr, ptrOff);
        if (flags.isByVal())
            return createCopyOfByValArgument(arg, ptrOff, chain, flags, dag);
        return dag.getStore(chain, arg, ptrOff, PseudoSourceValue.getStack(), locMemOffset, false, 0);
    }

    private static SDValue createCopyOfByValArgument(SDValue src, SDValue dst,
            SDValue chain, ArgFlagsTy flags, SelectionDAG dag)
    {
        SDValue sizeNode = dag.getConstant(flags.getByValSize(), new EVT(MVT.i32), false);
        return dag.getMemcpy(chain, dst, src, sizeNode, flags.getByValAlign(),
                true, null, 0, null, 0);
    }

    private static SDValue getMOVL(SelectionDAG dag, EVT vt, SDValue v1, SDValue v2)
    {
        int numElts = vt.getVectorNumElements();
        TIntArrayList mask = new TIntArrayList();
        mask.add(numElts);
        for (int i = 1; i < numElts; i++)
            mask.add(i);
        return dag.getVectorShuffle(vt, v1, v2, mask.toArray());
    }

    private static SDValue getUnpackl(SelectionDAG dag, EVT vt, SDValue v1, SDValue v2)
    {
        int numElts = vt.getVectorNumElements();
        TIntArrayList mask = new TIntArrayList();
        for (int i = 0; i < numElts/2; i++)
        {
            mask.add(i);
            mask.add(i+numElts);
        }
        return dag.getVectorShuffle(vt, v1, v2, mask.toArray());
    }

    private static SDValue getUnpackh(SelectionDAG dag, EVT vt, SDValue v1, SDValue v2)
    {
        int numElts = vt.getVectorNumElements();
        int half = numElts/2;
        TIntArrayList mask = new TIntArrayList();
        for (int i = 0; i < half; i++)
        {
            mask.add(i + half);
            mask.add(i + numElts + half);
        }
        return dag.getVectorShuffle(vt, v1, v2, mask.toArray());
    }

    private SDValue emitTailCallLoadRetAddr(SelectionDAG dag,
            OutParamWrapper<SDValue> outRetAddr, SDValue chain,
            boolean isTailCall, boolean is64Bit, int fpDiff)
    {
        if (!isTailCall || fpDiff == 0) return chain;

        EVT vt = new EVT(getPointerTy());
        outRetAddr.set(getReturnAddressFrameIndex(dag));

        outRetAddr.set(dag.getLoad(vt, chain, outRetAddr.get(), null, 0));
        return new SDValue(outRetAddr.get().getNode(), 1);
    }

    private SDValue emitTailCallStoreRetAddr(SelectionDAG dag,
            MachineFunction mf,
            SDValue chain,
            SDValue retAddrFrIdx,
            boolean is64Bit,
            int fpDiff)
    {
        if (fpDiff == 0) return chain;

        int slotSize = is64Bit?8:4;
        int newReturnAddrFI = mf.getFrameInfo().createFixedObject(slotSize, fpDiff-slotSize);
        EVT vt = is64Bit?new EVT(MVT.i64):new EVT(MVT.i32);
        SDValue newRetAddrFrIdx = dag.getFrameIndex(newReturnAddrFI, vt, false);
        return dag.getStore(chain, retAddrFrIdx, newRetAddrFrIdx,
                PseudoSourceValue.getFixedStack(newReturnAddrFI), 0, false, 0);
    }

    private SDValue getReturnAddressFrameIndex(SelectionDAG dag)
    {
        MachineFunction mf = dag.getMachineFunction();
        X86MachineFunctionInfo funcInfo = (X86MachineFunctionInfo) mf.getInfo();
        int returnAddrIndex = funcInfo.getRAIndex();

        if (returnAddrIndex == 0)
        {
            int slotSize = td.getPointerSize();
            returnAddrIndex = mf.getFrameInfo().createFixedObject(slotSize, -slotSize);
            funcInfo.setRAIndex(returnAddrIndex);
        }
        return dag.getFrameIndex(returnAddrIndex, new EVT(getPointerTy()), false);
    }

    static boolean callIsStructReturn(ArrayList<OutputArg> outs)
    {
        if (outs == null || outs.isEmpty())
            return false;

        return outs.get(0).flags.isSRet();
    }

    static boolean argIsStructReturn(ArrayList<InputArg> ins)
    {
        if (ins == null || ins.isEmpty())
            return false;

        return ins.get(0).flags.isSRet();
    }

    @Override
    public MachineBasicBlock emitInstrWithCustomInserter(
            MachineInstr mi,
            MachineBasicBlock mbb)
    {
        TargetInstrInfo tii = getTargetMachine().getInstrInfo();
        switch (mi.getOpcode())
        {
            case X86GenInstrNames.CMOV_V1I64:
            case X86GenInstrNames.CMOV_FR32:
            case X86GenInstrNames.CMOV_FR64:
            case X86GenInstrNames.CMOV_V4F32:
            case X86GenInstrNames.CMOV_V2F64:
            case X86GenInstrNames.CMOV_V2I64:
            {
                BasicBlock llvmBB = mbb.getBasicBlock();
                int insertPos = 1;


                //  thisMBB:
                //  ...
                //   TrueVal = ...
                //   cmpTY ccX, r1, r2
                //   bCC copy1MBB
                //   fallthrough --> copy0MBB
                MachineBasicBlock thisMBB = mbb;
                MachineFunction mf = thisMBB.getParent();
                MachineBasicBlock copy0MBB = mf.createMachineBasicBlock(llvmBB);
                MachineBasicBlock sinkMBB = mf.createMachineBasicBlock(llvmBB);

                int opc = X86InstrInfo.getCondBranchFromCond(mi.getOperand(3).getImm());
                buildMI(mbb, tii.get(opc)).addMBB(sinkMBB);
                mf.insert(insertPos, copy0MBB);
                mf.insert(insertPos, sinkMBB);

                mbb.transferSuccessor(mbb);
                mbb.addSuccessor(sinkMBB);

                mbb = copy0MBB;
                mbb.addSuccessor(sinkMBB);

                mbb = sinkMBB;
                buildMI(mbb, tii.get(X86GenInstrNames.PHI), mi.getOperand(0).getReg())
                        .addReg(mi.getOperand(1).getReg()).addMBB(copy0MBB)
                        .addReg(mi.getOperand(2).getReg()).addMBB(sinkMBB);
                mf.deleteMachineInstr(mi);
                return mbb;
            }
            case X86GenInstrNames.FP32_TO_INT16_IN_MEM:
            case X86GenInstrNames.FP32_TO_INT32_IN_MEM:
            case X86GenInstrNames.FP32_TO_INT64_IN_MEM:
            case X86GenInstrNames.FP64_TO_INT16_IN_MEM:
            case X86GenInstrNames.FP64_TO_INT32_IN_MEM:
            case X86GenInstrNames.FP64_TO_INT64_IN_MEM:
            case X86GenInstrNames.FP80_TO_INT16_IN_MEM:
            case X86GenInstrNames.FP80_TO_INT32_IN_MEM:
            case X86GenInstrNames.FP80_TO_INT64_IN_MEM: 
            {
                MachineFunction mf = mbb.getParent();
                int cwFrameIndex = mf.getFrameInfo().createStackObject(2, 2);
                addFrameReference(buildMI(mbb, tii.get(X86GenInstrNames.FNSTCW16m)), cwFrameIndex);

                int oldCW = mf.getMachineRegisterInfo().createVirtualRegister(X86GenRegisterInfo.GR16RegisterClass);
                addFrameReference(buildMI(mbb, tii.get(X86GenInstrNames.MOV16rm), oldCW), cwFrameIndex);

                addFrameReference(buildMI(mbb, tii.get(X86GenInstrNames.MOV16mi)), cwFrameIndex)
                        .addImm(0xC7F);
                addFrameReference(buildMI(mbb, tii.get(X86GenInstrNames.FLDCW16m)), cwFrameIndex);
                addFrameReference(buildMI(mbb, tii.get(X86GenInstrNames.MOV16mr)), cwFrameIndex)
                        .addReg(oldCW);

                int opc = -1;
                switch (mi.getOpcode())
                {
                    default:
                        Util.shouldNotReachHere("Illegal opcode!");
                    case X86GenInstrNames.FP32_TO_INT16_IN_MEM:
                        opc = X86GenInstrNames.IST_Fp16m32;
                        break;
                    case X86GenInstrNames.FP32_TO_INT32_IN_MEM:
                        opc = X86GenInstrNames.IST_Fp32m32;
                        break;
                    case X86GenInstrNames.FP32_TO_INT64_IN_MEM:
                        opc = X86GenInstrNames.IST_Fp64m32;
                        break;
                    case X86GenInstrNames.FP64_TO_INT16_IN_MEM:
                        opc = X86GenInstrNames.IST_Fp16m64;
                        break;
                    case X86GenInstrNames.FP64_TO_INT32_IN_MEM:
                        opc = X86GenInstrNames.IST_Fp32m64;
                        break;
                    case X86GenInstrNames.FP64_TO_INT64_IN_MEM:
                        opc = X86GenInstrNames.IST_Fp64m64;
                        break;
                    case X86GenInstrNames.FP80_TO_INT16_IN_MEM:
                        opc = X86GenInstrNames.IST_Fp16m80;
                        break;
                    case X86GenInstrNames.FP80_TO_INT32_IN_MEM:
                        opc = X86GenInstrNames.IST_Fp32m80;
                        break;
                    case X86GenInstrNames.FP80_TO_INT64_IN_MEM:
                        opc = X86GenInstrNames.IST_Fp64m80;
                        break;
                }

                X86AddressMode am = new X86AddressMode();
                MachineOperand op = mi.getOperand(0);
                if (op.isRegister())
                {
                    am.baseType = X86AddressMode.BaseType.RegBase;
                    am.base = new X86AddressMode.RegisterBase(op.getReg());
                }
                else
                {
                    am.baseType = X86AddressMode.BaseType.FrameIndexBase;
                    am.base = new X86AddressMode.FrameIndexBase(op.getIndex());
                }
                op = mi.getOperand(1);
                if (op.isImm())
                    am.scale = (int) op.getImm();
                op = mi.getOperand(2);
                if (op.isImm())
                    am.indexReg = (int) op.getImm();
                op = mi.getOperand(3);
                if (op.isGlobalAddress())
                {
                    am.gv = op.getGlobal();
                }
                else
                {
                    am.disp = (int) op.getImm();
                }
                addFullAddress(buildMI(mbb, tii.get(opc)), am)
                        .addReg(mi.getOperand(X86AddrNumOperands).getReg());
                addFrameReference(buildMI(mbb, tii.get(X86GenInstrNames.FLDCW16m)), cwFrameIndex);
                mf.deleteMachineInstr(mi);
                return mbb;
            }
            case X86GenInstrNames.PCMPISTRM128REG:
                return emitPCMP(mi, mbb, 3, false);
            case X86GenInstrNames.PCMPISTRM128MEM:
                return emitPCMP(mi, mbb, 3, true);
            case X86GenInstrNames.PCMPESTRM128REG:
                return emitPCMP(mi, mbb, 5, false);
            case X86GenInstrNames.PCMPESTRM128MEM:
                return emitPCMP(mi, mbb, 5, true);

            case X86GenInstrNames.ATOMAND32:
                return emitAtomicBitwiseWithCustomInserter(mi, mbb,
                        X86GenInstrNames.AND32rr,
                        X86GenInstrNames.AND32ri,
                        X86GenInstrNames.MOV32rm,
                        X86GenInstrNames.LCMPXCHG32,
                        X86GenInstrNames.MOV32rr,
                        X86GenInstrNames.NOT32r,
                        X86GenRegisterNames.EAX,
                        X86GenRegisterInfo.GR32RegisterClass);
            case X86GenInstrNames.ATOMOR32:
                return emitAtomicBitwiseWithCustomInserter(mi, mbb,
                        X86GenInstrNames.OR32rr,
                        X86GenInstrNames.OR32ri,
                        X86GenInstrNames.MOV32rm,
                        X86GenInstrNames.LCMPXCHG32,
                        X86GenInstrNames.MOV32rr,
                        X86GenInstrNames.NOT32r,
                        X86GenRegisterNames.EAX,
                        X86GenRegisterInfo.GR32RegisterClass);
            case X86GenInstrNames.ATOMXOR32:
                return emitAtomicBitwiseWithCustomInserter(mi, mbb,
                        X86GenInstrNames.XOR32rr,
                        X86GenInstrNames.XOR32ri,
                        X86GenInstrNames.MOV32rm,
                        X86GenInstrNames.LCMPXCHG32,
                        X86GenInstrNames.MOV32rr,
                        X86GenInstrNames.NOT32r,
                        X86GenRegisterNames.EAX,
                        X86GenRegisterInfo.GR32RegisterClass);
            case X86GenInstrNames.ATOMNAND32:
                return emitAtomicBitwiseWithCustomInserter(mi, mbb,
                        X86GenInstrNames.AND32rr,
                        X86GenInstrNames.AND32ri,
                        X86GenInstrNames.MOV32rm,
                        X86GenInstrNames.LCMPXCHG32,
                        X86GenInstrNames.MOV32rr,
                        X86GenInstrNames.NOT32r,
                        X86GenRegisterNames.EAX,
                        X86GenRegisterInfo.GR32RegisterClass,
                        true);
            case X86GenInstrNames.ATOMMIN32:
                return emitAtomicMinMaxWithCustomInserter(mi, mbb, X86GenInstrNames.CMOVL32rr);
            case X86GenInstrNames.ATOMMAX32:
                return emitAtomicMinMaxWithCustomInserter(mi, mbb, X86GenInstrNames.CMOVG32rr);
            case X86GenInstrNames.ATOMUMIN32:
                return emitAtomicMinMaxWithCustomInserter(mi, mbb, X86GenInstrNames.CMOVB32rr);
            case X86GenInstrNames.ATOMUMAX32:
                return emitAtomicMinMaxWithCustomInserter(mi, mbb, X86GenInstrNames.CMOVA32rr);
            case X86GenInstrNames.ATOMAND16:
                return emitAtomicBitwiseWithCustomInserter(mi, mbb, X86GenInstrNames.AND16rr,
                        X86GenInstrNames.AND16ri, X86GenInstrNames.MOV16rm,
                        X86GenInstrNames.LCMPXCHG16, X86GenInstrNames.MOV16rr,
                        X86GenInstrNames.NOT16r, X86GenRegisterNames.AX,
                        X86GenRegisterInfo.GR16RegisterClass);
            case X86GenInstrNames.ATOMOR16:
                return emitAtomicBitwiseWithCustomInserter(mi, mbb, X86GenInstrNames.OR16rr,
                        X86GenInstrNames.OR16ri, X86GenInstrNames.MOV16rm,
                        X86GenInstrNames.LCMPXCHG16, X86GenInstrNames.MOV16rr,
                        X86GenInstrNames.NOT16r, X86GenRegisterNames.AX,
                        X86GenRegisterInfo.GR16RegisterClass);
            case X86GenInstrNames.ATOMXOR16:
                return emitAtomicBitwiseWithCustomInserter(mi, mbb, X86GenInstrNames.XOR16rr,
                        X86GenInstrNames.XOR16ri, X86GenInstrNames.MOV16rm,
                        X86GenInstrNames.LCMPXCHG16, X86GenInstrNames.MOV16rr,
                        X86GenInstrNames.NOT16r, X86GenRegisterNames.AX,
                        X86GenRegisterInfo.GR16RegisterClass);
            case X86GenInstrNames.ATOMNAND16:
                return emitAtomicBitwiseWithCustomInserter(mi, mbb, X86GenInstrNames.AND16rr,
                        X86GenInstrNames.AND16ri, X86GenInstrNames.MOV16rm,
                        X86GenInstrNames.LCMPXCHG16, X86GenInstrNames.MOV16rr,
                        X86GenInstrNames.NOT16r, X86GenRegisterNames.AX,
                        X86GenRegisterInfo.GR16RegisterClass, true);
            case X86GenInstrNames.ATOMMIN16:
                return emitAtomicMinMaxWithCustomInserter(mi, mbb, X86GenInstrNames.CMOVL16rr);
            case X86GenInstrNames.ATOMMAX16:
                return emitAtomicMinMaxWithCustomInserter(mi, mbb, X86GenInstrNames.CMOVG16rr);
            case X86GenInstrNames.ATOMUMIN16:
                return emitAtomicMinMaxWithCustomInserter(mi, mbb, X86GenInstrNames.CMOVB16rr);
            case X86GenInstrNames.ATOMUMAX16:
                return emitAtomicMinMaxWithCustomInserter(mi, mbb, X86GenInstrNames.CMOVA16rr);

            case X86GenInstrNames.ATOMAND8:
                return emitAtomicBitwiseWithCustomInserter(mi, mbb, X86GenInstrNames.AND8rr,
                        X86GenInstrNames.AND8ri, X86GenInstrNames.MOV8rm,
                        X86GenInstrNames.LCMPXCHG8, X86GenInstrNames.MOV8rr,
                        X86GenInstrNames.NOT8r, X86GenRegisterNames.AL,
                        X86GenRegisterInfo.GR8RegisterClass);
            case X86GenInstrNames.ATOMOR8:
                return emitAtomicBitwiseWithCustomInserter(mi, mbb, X86GenInstrNames.OR8rr,
                        X86GenInstrNames.OR8ri, X86GenInstrNames.MOV8rm,
                        X86GenInstrNames.LCMPXCHG8, X86GenInstrNames.MOV8rr,
                        X86GenInstrNames.NOT8r, X86GenRegisterNames.AL,
                        X86GenRegisterInfo.GR8RegisterClass);
            case X86GenInstrNames.ATOMXOR8:
                return emitAtomicBitwiseWithCustomInserter(mi, mbb, X86GenInstrNames.XOR8rr,
                        X86GenInstrNames.XOR8ri, X86GenInstrNames.MOV8rm,
                        X86GenInstrNames.LCMPXCHG8, X86GenInstrNames.MOV8rr,
                        X86GenInstrNames.NOT8r, X86GenRegisterNames.AL,
                        X86GenRegisterInfo.GR8RegisterClass);
            case X86GenInstrNames.ATOMNAND8:
                return emitAtomicBitwiseWithCustomInserter(mi, mbb, X86GenInstrNames.AND8rr,
                        X86GenInstrNames.AND8ri, X86GenInstrNames.MOV8rm,
                        X86GenInstrNames.LCMPXCHG8, X86GenInstrNames.MOV8rr,
                        X86GenInstrNames.NOT8r, X86GenRegisterNames.AL,
                        X86GenRegisterInfo.GR8RegisterClass, true);
            // FIXME: There are no CMOV8 instructions; MIN/MAX need some other way.
            // This group is for 64-bit host.
            case X86GenInstrNames.ATOMAND64:
                return emitAtomicBitwiseWithCustomInserter(mi, mbb, X86GenInstrNames.AND64rr,
                        X86GenInstrNames.AND64ri32, X86GenInstrNames.MOV64rm,
                        X86GenInstrNames.LCMPXCHG64, X86GenInstrNames.MOV64rr,
                        X86GenInstrNames.NOT64r, X86GenRegisterNames.RAX,
                        X86GenRegisterInfo.GR64RegisterClass);
            case X86GenInstrNames.ATOMOR64:
                return emitAtomicBitwiseWithCustomInserter(mi, mbb, X86GenInstrNames.OR64rr,
                        X86GenInstrNames.OR64ri32, X86GenInstrNames.MOV64rm,
                        X86GenInstrNames.LCMPXCHG64, X86GenInstrNames.MOV64rr,
                        X86GenInstrNames.NOT64r, X86GenRegisterNames.RAX,
                        X86GenRegisterInfo.GR64RegisterClass);
            case X86GenInstrNames.ATOMXOR64:
                return emitAtomicBitwiseWithCustomInserter(mi, mbb, X86GenInstrNames.XOR64rr,
                        X86GenInstrNames.XOR64ri32, X86GenInstrNames.MOV64rm,
                        X86GenInstrNames.LCMPXCHG64, X86GenInstrNames.MOV64rr,
                        X86GenInstrNames.NOT64r, X86GenRegisterNames.RAX,
                        X86GenRegisterInfo.GR64RegisterClass);
            case X86GenInstrNames.ATOMNAND64:
                return emitAtomicBitwiseWithCustomInserter(mi, mbb, X86GenInstrNames.AND64rr,
                        X86GenInstrNames.AND64ri32, X86GenInstrNames.MOV64rm,
                        X86GenInstrNames.LCMPXCHG64, X86GenInstrNames.MOV64rr,
                        X86GenInstrNames.NOT64r, X86GenRegisterNames.RAX,
                        X86GenRegisterInfo.GR64RegisterClass, true);
            case X86GenInstrNames.ATOMMIN64:
                return emitAtomicMinMaxWithCustomInserter(mi, mbb, X86GenInstrNames.CMOVL64rr);
            case X86GenInstrNames.ATOMMAX64:
                return emitAtomicMinMaxWithCustomInserter(mi, mbb, X86GenInstrNames.CMOVG64rr);
            case X86GenInstrNames.ATOMUMIN64:
                return emitAtomicMinMaxWithCustomInserter(mi, mbb, X86GenInstrNames.CMOVB64rr);
            case X86GenInstrNames.ATOMUMAX64:
                return emitAtomicMinMaxWithCustomInserter(mi, mbb, X86GenInstrNames.CMOVA64rr);

            // This group does 64-bit operations on a 32-bit host.
            case X86GenInstrNames.ATOMAND6432:
                return emitAtomicBit6432WithCustomInserter(mi, mbb,
                        X86GenInstrNames.AND32rr, X86GenInstrNames.AND32rr,
                        X86GenInstrNames.AND32ri, X86GenInstrNames.AND32ri,
                        false);
            case X86GenInstrNames.ATOMOR6432:
                return emitAtomicBit6432WithCustomInserter(mi, mbb,
                        X86GenInstrNames.OR32rr, X86GenInstrNames.OR32rr,
                        X86GenInstrNames.OR32ri, X86GenInstrNames.OR32ri,
                        false);
            case X86GenInstrNames.ATOMXOR6432:
                return emitAtomicBit6432WithCustomInserter(mi, mbb,
                        X86GenInstrNames.XOR32rr, X86GenInstrNames.XOR32rr,
                        X86GenInstrNames.XOR32ri, X86GenInstrNames.XOR32ri,
                        false);
            case X86GenInstrNames.ATOMNAND6432:
                return emitAtomicBit6432WithCustomInserter(mi, mbb,
                        X86GenInstrNames.AND32rr, X86GenInstrNames.AND32rr,
                        X86GenInstrNames.AND32ri, X86GenInstrNames.AND32ri,
                        true);
            case X86GenInstrNames.ATOMADD6432:
                return emitAtomicBit6432WithCustomInserter(mi, mbb,
                        X86GenInstrNames.ADD32rr, X86GenInstrNames.ADC32rr,
                        X86GenInstrNames.ADD32ri, X86GenInstrNames.ADC32ri,
                        false);
            case X86GenInstrNames.ATOMSUB6432:
                return emitAtomicBit6432WithCustomInserter(mi, mbb,
                        X86GenInstrNames.SUB32rr, X86GenInstrNames.SBB32rr,
                        X86GenInstrNames.SUB32ri, X86GenInstrNames.SBB32ri,
                        false);
            case X86GenInstrNames.ATOMSWAP6432:
                return emitAtomicBit6432WithCustomInserter(mi, mbb,
                        X86GenInstrNames.MOV32rr, X86GenInstrNames.MOV32rr,
                        X86GenInstrNames.MOV32ri, X86GenInstrNames.MOV32ri,
                        false);
            case X86GenInstrNames.VASTART_SAVE_XMM_REGS:
                return emitVAStartSaveXMMRegsWithCustomInserter(mi,mbb);
            default:
                Util.shouldNotReachHere();
                return null;
        }
    }

    private MachineBasicBlock emitPCMP(MachineInstr mi, MachineBasicBlock mbb,
            int numArgs, boolean memArgs)
    {
        MachineFunction mf = mbb.getParent();
        TargetInstrInfo tii = getTargetMachine().getInstrInfo();

        int opc;
        if (memArgs)
        {
            opc = numArgs == 3? X86GenInstrNames.PCMPISTRM128rm:
                    X86GenInstrNames.PCMPESTRM128rm;
        }
        else
        {
            opc = numArgs == 3 ? X86GenInstrNames.PCMPISTRM128rr:
                X86GenInstrNames.PCMPESTRM128rr;
        }

        MachineInstrBuilder mib = buildMI(mbb, tii.get(opc));
        for (int i = 0; i < numArgs; i++)
        {
            MachineOperand mo = mi.getOperand(i);
            if (!(mo.isRegister() && mo.isImplicit()))
                mib.addOperand(mo);
        }

        buildMI(mbb, tii.get(X86GenInstrNames.MOVAPSrr), mi.getOperand(0).getReg())
                .addReg(X86GenRegisterNames.XMM0);
        mf.deleteMachineInstr(mi);
        return mbb;
    }

    private MachineBasicBlock emitAtomicBitwiseWithCustomInserter(
            MachineInstr mi,
            MachineBasicBlock mbb,
            int regOpc,
            int immOpc,
            int loadOpc,
            int cxchgOpc,
            int copyOpc,
            int notOpc,
            int eaxReg,
            TargetRegisterClass rc)
    {
        return emitAtomicBitwiseWithCustomInserter(mi, mbb, regOpc, immOpc,
                loadOpc, cxchgOpc, copyOpc,notOpc,eaxReg, rc, false);
    }

    private MachineBasicBlock emitAtomicBitwiseWithCustomInserter(
            MachineInstr mi, MachineBasicBlock mbb,
            int regOpc,
            int immOpc,
            int loadOpc,
            int cxchgOpc,
            int copyOpc,
            int notOpc,
            int eaxReg,
            TargetRegisterClass rc,
            boolean invSrc)
    {
        // For the atomic bitwise operator, we generate
        //   thisMBB:
        //   newMBB:
        //     ld  t1 = [bitinstr.addr]
        //     op  t2 = t1, [bitinstr.val]
        //     mov EAX = t1
        //     lcs dest = [bitinstr.addr], t2  [EAX is implicit]
        //     bz  newMBB
        //     fallthrough -->nextMBB
        TargetInstrInfo tii = getTargetMachine().getInstrInfo();
        BasicBlock llvmBB = mbb.getBasicBlock();
        int itr = 1;
        // First build the CFG.
        MachineFunction mf = mbb.getParent();
        MachineBasicBlock thisMBB = mbb;
        MachineBasicBlock newMBB = mf.createMachineBasicBlock(llvmBB);
        MachineBasicBlock nextMBB = mf.createMachineBasicBlock(llvmBB);
        mf.insert(itr, newMBB);
        mf.insert(itr, nextMBB);

        nextMBB.transferSuccessor(thisMBB);
        thisMBB.addSuccessor(newMBB);

        newMBB.addSuccessor(nextMBB);
        newMBB.addSuccessor(newMBB);

        assert mi.getNumOperands() < X86AddrNumOperands + 4:
                "unexpected number of operands";

        MachineOperand destOp = mi.getOperand(0);
        MachineOperand[] argOps = new MachineOperand[2+X86AddrNumOperands];
        int numArgs = mi.getNumOperands()-1;
        for (int i = 0; i< numArgs; i++)
            argOps[i] = mi.getOperand(i+1);

        int lastAddrIndex = X86AddrNumOperands -1 ;
        int valArgIndex = lastAddrIndex + 1;

        int t1 = mf.getMachineRegisterInfo().createVirtualRegister(rc);
        MachineInstrBuilder mib = buildMI(newMBB, tii.get(loadOpc), t1);
        for (int i = 0; i <= lastAddrIndex; i++)
            mib.addOperand(argOps[i]);

        int tt = mf.getMachineRegisterInfo().createVirtualRegister(rc);
        if (invSrc)
            mib = buildMI(newMBB, tii.get(notOpc), tt).addReg(t1);
        else
            tt = t1;

        int t2 = mf.getMachineRegisterInfo().createVirtualRegister(rc);
        assert argOps[valArgIndex].isRegister() || argOps[valArgIndex].isMBB()
                :"invalid operand!";

        if (argOps[valArgIndex].isRegister())
            mib = buildMI(newMBB, tii.get(regOpc), t2);
        else
            mib = buildMI(newMBB, tii.get(immOpc), t2);

        mib.addReg(tt);
        mib.addOperand(argOps[valArgIndex]);

        mib = buildMI(newMBB, tii.get(copyOpc), eaxReg).addReg(t1);
        mib = buildMI(newMBB, tii.get(cxchgOpc));
        for (int i = 0; i <= lastAddrIndex; i++)
            mib.addOperand(argOps[i]);
        mib.addReg(t2);
        assert mi.hasOneMemOperand():"Unexpected number of memoperand!";
        mib.addMemOperand(mi.getMemOperand(0));

        mib = buildMI(newMBB, tii.get(copyOpc), destOp.getReg()).addReg(eaxReg);
        // insert branch.
        buildMI(newMBB, tii.get(X86GenInstrNames.JNE)).addMBB(newMBB);
        mf.deleteMachineInstr(mi);
        return nextMBB;
    }

    private MachineBasicBlock emitAtomicBit6432WithCustomInserter(
            MachineInstr mi,
            MachineBasicBlock mbb,
            int regOpcL,
            int regOpcH,
            int immOpcL,
            int immOpcH,
            boolean invSrc)
    {
        // For the atomic bitwise operator, we generate
        //   thisMBB (instructions are in pairs, except cmpxchg8b)
        //     ld t1,t2 = [bitinstr.addr]
        //   newMBB:
        //     out1, out2 = phi (thisMBB, t1/t2) (newMBB, t3/t4)
        //     op  t5, t6 <- out1, out2, [bitinstr.val]
        //      (for SWAP, substitute:  mov t5, t6 <- [bitinstr.val])
        //     mov ECX, EBX <- t5, t6
        //     mov EAX, EDX <- t1, t2
        //     cmpxchg8b [bitinstr.addr]  [EAX, EDX, EBX, ECX implicit]
        //     mov t3, t4 <- EAX, EDX
        //     bz  newMBB
        //     result in out1, out2
        //     fallthrough -->nextMBB
        TargetRegisterClass rc = X86GenRegisterInfo.GR32RegisterClass;
        int loadOpc = X86GenInstrNames.MOV32rm;
        int copyOpc = X86GenInstrNames.MOV32rr;
        int notOpc = X86GenInstrNames.NOT32r;
        TargetInstrInfo tii = getTargetMachine().getInstrInfo();
        BasicBlock llvmBB = mbb.getBasicBlock();
        int itr = 1;

        MachineFunction mf = mbb.getParent();
        MachineRegisterInfo mri = mf.getMachineRegisterInfo();
        MachineBasicBlock newMBB = mf.createMachineBasicBlock(llvmBB);
        MachineBasicBlock nextMBB = mf.createMachineBasicBlock(llvmBB);
        mf.insert(itr, newMBB);
        mf.insert(itr, nextMBB);

        assert mi.getNumOperands() < X86AddrNumOperands + 14:
                "unexpected number of operands!";
        MachineOperand dest1Op = mi.getOperand(0);
        MachineOperand dest2Op = mi.getOperand(1);
        MachineOperand[] argOps = new MachineOperand[2+X86AddrNumOperands];
        for (int i = 0; i < 2+X86AddrNumOperands; i++)
            argOps[i] = mi.getOperand(i+2);

        int lastAddrIndex = X86AddrNumOperands - 1;
        int t1 = mri.createVirtualRegister(rc);
        MachineInstrBuilder mib = buildMI(mbb, tii.get(loadOpc), t1);
        for (int i = 0; i<=lastAddrIndex; i++)
            mib.addOperand(argOps[i]);

        int t2 = mri.createVirtualRegister(rc);
        mib = buildMI(mbb, tii.get(loadOpc), t2);
        for (int i = 0; i <= lastAddrIndex-2; i++)
            mib.addOperand(argOps[i]);

        MachineOperand newOp3 = argOps[3];
        if (newOp3.isImm())
            newOp3.setImm(newOp3.getImm()+4);
        else
            newOp3.setOffset(newOp3.getOffset()+4);
        mib.addOperand(newOp3);
        mib.addOperand(argOps[lastAddrIndex]);

        int t3 = mri.createVirtualRegister(rc);
        int t4 = mri.createVirtualRegister(rc);
        buildMI(newMBB, tii.get(X86GenInstrNames.PHI), dest1Op.getReg())
                .addReg(t1).addMBB(mbb).addReg(t3).addMBB(newMBB);
        buildMI(newMBB, tii.get(X86GenInstrNames.PHI), dest2Op.getReg())
                .addReg(t2).addMBB(mbb).addReg(t4).addMBB(newMBB);

        int tt1 = mri.createVirtualRegister(rc);
        int tt2 = mri.createVirtualRegister(rc);
        if (invSrc)
        {
            buildMI(newMBB, tii.get(notOpc), tt1).addReg(t1);
            buildMI(newMBB, tii.get(notOpc), tt2).addReg(t2);
        }
        else
        {
            tt1 = t1;
            tt2 = t2;
        }

        int valArgIndex = lastAddrIndex + 1;
        assert argOps[valArgIndex].isRegister() ||
                argOps[valArgIndex].isImm():"Invalid operand!";
        int t5 = mri.createVirtualRegister(rc);
        int t6 = mri.createVirtualRegister(rc);
        if (argOps[valArgIndex].isRegister())
            mib = buildMI(newMBB, tii.get(regOpcL), t5);
        else
            mib = buildMI(newMBB, tii.get(immOpcL), t5);

        if (regOpcL != X86GenInstrNames.MOV32rr)
            mib.addReg(tt1);
        mib.addOperand(argOps[valArgIndex]);
        assert argOps[valArgIndex+1].isRegister() || argOps[valArgIndex].isRegister();
        assert argOps[valArgIndex+1].isImm() || argOps[valArgIndex].isImm();

        if (argOps[valArgIndex+1].isRegister())
            mib = buildMI(newMBB, tii.get(regOpcH), t6);
        else
            mib = buildMI(newMBB, tii.get(immOpcH), t6);

        if (regOpcH != X86GenInstrNames.MOV32rr)
            mib.addReg(tt2);

        mib.addOperand(argOps[valArgIndex+1]);
        buildMI(newMBB, tii.get(copyOpc), X86GenRegisterNames.EAX).addReg(t1);
        buildMI(newMBB, tii.get(copyOpc), X86GenRegisterNames.EDX).addReg(t2);
        buildMI(newMBB, tii.get(copyOpc), X86GenRegisterNames.EBX).addReg(t5);
        buildMI(newMBB, tii.get(copyOpc), X86GenRegisterNames.ECX).addReg(t6);

        mib = buildMI(newMBB, tii.get(X86GenInstrNames.LCMPXCHG8B));
        for (int i = 0; i<= lastAddrIndex; i++)
            mib.addOperand(argOps[i]);

        assert mi.hasOneMemOperand():"Unexpected number of memoperand!";
        mib.addMemOperand(mi.getMemOperand(0));
        buildMI(newMBB, tii.get(copyOpc), t3).addReg(X86GenRegisterNames.EAX);
        buildMI(newMBB, tii.get(copyOpc), t4).addReg(X86GenRegisterNames.EDX);

        // insert branch.
        buildMI(newMBB, tii.get(X86GenInstrNames.JNE)).addMBB(newMBB);
        mf.deleteMachineInstr(mi);
        return nextMBB;
    }

    private MachineBasicBlock emitAtomicMinMaxWithCustomInserter(
            MachineInstr mi,
            MachineBasicBlock mbb,
            int cmovOpc)
    {
        // For the atomic min/max operator, we generate
        //   thisMBB:
        //   newMBB:
        //     ld t1 = [min/max.addr]
        //     mov t2 = [min/max.val]
        //     cmp  t1, t2
        //     cmov[cond] t2 = t1
        //     mov EAX = t1
        //     lcs dest = [bitinstr.addr], t2  [EAX is implicit]
        //     bz   newMBB
        //     fallthrough -->nextMBB
        //
        TargetInstrInfo tii = getTargetMachine().getInstrInfo();
        BasicBlock llvmBB = mbb.getBasicBlock();
        int itr = 1;
        // First build the CFG.
        MachineFunction mf = mbb.getParent();
        MachineRegisterInfo mri = mf.getMachineRegisterInfo();
        TargetRegisterClass rc = X86GenRegisterInfo.GR32RegisterClass;

        MachineBasicBlock newMBB = mf.createMachineBasicBlock(llvmBB);
        MachineBasicBlock nextMBB = mf.createMachineBasicBlock(llvmBB);
        mf.insert(itr, newMBB);
        mf.insert(itr, nextMBB);

        nextMBB.transferSuccessor(mbb);
        mbb.addSuccessor(newMBB);

        newMBB.addSuccessor(nextMBB);
        newMBB.addSuccessor(newMBB);

        assert mi.getNumOperands() < X86AddrNumOperands + 4:
                "unexpected number of operands";

        MachineOperand destOp = mi.getOperand(0);
        MachineOperand[] argOps = new MachineOperand[2+X86AddrNumOperands];
        int numArgs = mi.getNumOperands()-1;
        for (int i = 0; i< numArgs; i++)
            argOps[i] = mi.getOperand(i+1);

        int lastAddrIndex = X86AddrNumOperands -1 ;
        int valArgIndex = lastAddrIndex + 1;

        int t1 = mri.createVirtualRegister(rc);
        MachineInstrBuilder mib = buildMI(newMBB, tii.get(X86GenInstrNames.MOV32rm), t1);
        for (int i = 0; i <= lastAddrIndex; i++)
            mib.addOperand(argOps[i]);

        assert argOps[valArgIndex].isRegister() || argOps[valArgIndex].isImm();

        int t2 = mri.createVirtualRegister(rc);
        // FIXME, redundant if condition?  2018/5/6
        if (argOps[valArgIndex].isRegister())
            mib = buildMI(newMBB, tii.get(X86GenInstrNames.MOV32rr), t2);
        else
            mib = buildMI(newMBB, tii.get(X86GenInstrNames.MOV32rr), t2);

        mib.addOperand(argOps[valArgIndex]);

        buildMI(newMBB, tii.get(X86GenInstrNames.MOV32rr), X86GenRegisterNames.EAX).addReg(t1);
        buildMI(newMBB, tii.get(X86GenInstrNames.CMP32rr)).addReg(t1).addReg(t2);

        // generate cmov
        int t3 = mri.createVirtualRegister(rc);
        buildMI(newMBB, tii.get(cmovOpc), t3).addReg(t2).addReg(t1);

        // cmp and exchange if none has modified the memory location.
        mib = buildMI(newMBB, tii.get(X86GenInstrNames.LCMPXCHG32));
        for (int i = 0; i <= lastAddrIndex; i++)
            mib.addOperand(argOps[i]);

        mib.addReg(t3);
        assert mi.hasOneMemOperand():"Unexpected number of memoperand";
        mib.addMemOperand(mi.getMemOperand(0));

        buildMI(newMBB, tii.get(X86GenInstrNames.MOV32rr), destOp.getReg())
                .addReg(X86GenRegisterNames.EAX);

        // insert branch.
        buildMI(newMBB, tii.get(X86GenInstrNames.JNE)).addMBB(newMBB);
        mf.deleteMachineInstr(mi);
        return nextMBB;
    }

    private MachineBasicBlock emitVAStartSaveXMMRegsWithCustomInserter(
            MachineInstr mi,
            MachineBasicBlock mbb)
    {
        // Emit code to save XMM registers to the stack. The ABI says that the
        // number of registers to save is given in %al, so it's theoretically
        // possible to do an indirect jump trick to avoid saving all of them,
        // however this code takes a simpler approach and just executes all
        // of the stores if %al is non-zero. It's less code, and it's probably
        // easier on the hardware branch predictor, and stores aren't all that
        // expensive anyway.

        // Create the new basic blocks. One block contains all the XMM stores,
        // and one block is the final destination regardless of whether any
        // stores were performed.

        BasicBlock llvmBB = mbb.getBasicBlock();
        MachineFunction mf = mbb.getParent();
        int itr = 1;
        MachineBasicBlock xmmSavedMBB = mf.createMachineBasicBlock(llvmBB);
        MachineBasicBlock endMBB = mf.createMachineBasicBlock(llvmBB);
        mf.insert(itr, xmmSavedMBB);
        mf.insert(itr, endMBB);

        endMBB.transferSuccessor(mbb);
        mbb.addSuccessor(xmmSavedMBB);
        xmmSavedMBB.addSuccessor(endMBB);

        TargetInstrInfo tii = getTargetMachine().getInstrInfo();
        int countReg = mi.getOperand(0).getReg();
        long regSaveFrameIndex = mi.getOperand(1).getImm();
        long varArgsOffset = mi.getOperand(2).getImm();

        if (!subtarget.isTargetWin64())
        {
            // If %al is 0, branch around the XMM save block.
            buildMI(mbb, tii.get(X86GenInstrNames.TEST8ri)).addReg(countReg)
                    .addReg(countReg);
            buildMI(mbb, tii.get(X86GenInstrNames.JE)).addMBB(endMBB);
            mbb.addSuccessor(endMBB);
        }

        // In the XMM save block, save all the XMM argument registers.
        for (int i = 3, e = mi.getNumOperands(); i < e; i++)
        {
            long offset = (i-3)*16 + varArgsFPOffset;
            buildMI(xmmSavedMBB, tii.get(X86GenInstrNames.MOVAPSmr))
                    .addFrameIndex(this.regSaveFrameIndex)
                    .addImm(1)  // scale
                    .addReg(0)  // indexReg
                    .addImm(offset) // disp
                    .addReg(0)  // segment
                    .addReg(mi.getOperand(i).getReg())
                    .addMemOperand(new MachineMemOperand(
                            PseudoSourceValue.getFixedStack(
                                    this.regSaveFrameIndex),
                            MachineMemOperand.MOStore,
                            offset, 16, 16));
        }
        mf.deleteMachineInstr(mi);
        return endMBB;
    }

    private SDValue emitTest(SDValue op, int x86CC, SelectionDAG dag)
    {
        boolean needCF = false, needOF = false;
        switch (x86CC)
        {
            case CondCode.COND_A:
            case CondCode.COND_AE:
            case CondCode.COND_B:
            case CondCode.COND_BE:
                needCF = true;
                break;
            case CondCode.COND_G:
            case CondCode.COND_GE:
            case CondCode.COND_L:
            case CondCode.COND_LE:
            case CondCode.COND_O:
            case CondCode.COND_NO:
                needOF = true;
                break;
            default:
                break;
        }

        if (op.getResNo() == 0 && !needCF & !needOF)
        {
            int opcode = 0;
            int numOperands = 0;

            FAIL:
            switch (op.getNode().getOpcode())
            {
                case ISD.ADD:
                {
                    for (SDUse u : op.getNode().getUseList())
                    {
                        if (u.getUser().getOpcode() == ISD.STORE)
                            break FAIL;
                    }
                    if (op.getNode().getOperand(1).getNode() instanceof ConstantSDNode)
                    {
                        ConstantSDNode c = (ConstantSDNode)op.getNode().getOperand(1).getNode();
                        if (c.getAPIntValue().eq(1))
                        {
                            opcode = X86ISD.INC;
                            numOperands = 1;
                            break;
                        }
                        if (c.getAPIntValue().isAllOnesValue())
                        {
                            opcode = X86ISD.DEC;
                            numOperands = 1;
                            break;
                        }
                    }
                    opcode = X86ISD.ADD;
                    numOperands = 2;
                    break;
                }
                case ISD.SUB:
                {
                    for (SDUse u : op.getNode().getUseList())
                    {
                        if (u.getUser().getOpcode() == ISD.STORE)
                            break FAIL;
                    }

                    opcode = X86ISD.SUB;
                    numOperands = 2;
                    break;
                }
                case X86ISD.ADD:
                case X86ISD.SUB:
                case X86ISD.INC:
                case X86ISD.DEC:
                    return new SDValue(op.getNode(), 1);
                default:
                    break;
            }
            if (opcode != 0)
            {
                SDVTList vts = dag.getVTList(op.getValueType(), new EVT(MVT.i32));
                ArrayList<SDValue> ops = new ArrayList<>();
                for (int i = 0; i < numOperands; i++)
                    ops.add(op.getOperand(i));

                SDValue newVal = dag.getNode(opcode, vts, ops);
                dag.replaceAllUsesWith(op, newVal, null);
                return new SDValue(newVal.getNode(), 1);
            }
        }

        return dag.getNode(X86ISD.CMP, new EVT(MVT.i32), op,
                dag.getConstant(0, op.getValueType(), false));
    }

    private SDValue emitCmp(SDValue op0, SDValue op1, int x86CC, SelectionDAG dag)
    {
        if (op1.getNode() instanceof ConstantSDNode)
        {
            ConstantSDNode cs = (ConstantSDNode)op1.getNode();
            if (cs.getAPIntValue().eq(0))
                return emitTest(op0, x86CC, dag);
        }
        return dag.getNode(X86ISD.CMP, new EVT(MVT.i32), op0, op1);
    }

    @Override
    public int getSetCCResultType(EVT vt)
    {
        return MVT.i8;
    }

    @Override
    public boolean allowsUnalignedMemoryAccesses(EVT memVT)
    {
        return true;
    }

    static boolean isUnderOrEqual(int val, int cmpVal)
    {
        return (val < 0 || val == cmpVal);
    }

    static boolean isUnderOrInRange(int val, int low, int high)
    {
        return val < 0 || (val >= low && val < high);
    }

    static boolean isPSHUFDMask(TIntArrayList mask, EVT vt)
    {
        if (vt.equals(new EVT(MVT.v4f32)) || vt.equals(new EVT(MVT.v4i32)) ||
                vt.equals(new EVT(MVT.v4i16)))
            return mask.get(0) < 4 && mask.get(1) < 4 &&
                    mask.get(2) < 4 && mask.get(3) < 4;
        if (vt.equals(new EVT(MVT.v2f64)) || vt.equals(new EVT(MVT.v2i64)))
            return mask.get(0) < 2 && mask.get(1) < 2;
        return false;
    }

    static boolean isPSHUFHWMask(TIntArrayList mask, EVT vt)
    {
        if (!vt.equals(new EVT(MVT.v8i16)))
            return false;

        for (int i = 0; i < 4; i++)
            if (mask.get(i) >= 0 && mask.get(i) != i)
                return false;

        for (int i = 4; i < 8; i++)
            if (mask.get(i) >= 0 && (mask.get(i) < 4 || mask.get(i) > 7))
                return false;
        return true;
    }

    static boolean isPSHUFLWMask(TIntArrayList mask, EVT vt)
    {
        if (vt.equals(new EVT(MVT.v8i16)))
            return false;

        for (int i = 4; i < 8; i++)
            if (mask.get(i) >= 0 && mask.get(i) != i)
                return false;

        for (int i = 0; i < 4; i++)
            if (mask.get(i) >= 4)
                return false;
        return true;
    }

    static boolean isSHUFPMask(TIntArrayList mask, EVT vt)
    {
        int numElts = vt.getVectorNumElements();
        if (numElts !=2 && numElts != 4)
            return false;

        int half = numElts / 2;
        for (int i = 0; i < half; i++)
            if (!isUnderOrInRange(mask.get(i), 0, numElts))
                return false;
        for (int i = half; i < numElts; i++)
            if (!isUnderOrInRange(mask.get(i), numElts, numElts*2))
                return false;
        return true;
    }

    static boolean isMOVLMask(TIntArrayList mask, EVT vt)
    {
        if (vt.getVectorElementType().getSizeInBits() < 32)
            return false;

        int numElts = vt.getVectorNumElements();
        if (!isUnderOrEqual(mask.get(0), numElts))
            return false;
        for (int i = 1; i < numElts; i++)
            if (!isUnderOrEqual(mask.get(i), i))
                return false;
        return true;
    }

    static boolean isCommutedMOVLMask(TIntArrayList mask, EVT vt)
    {
        return isCommutedMOVLMask(mask, vt, false, false);
    }

    static boolean isCommutedMOVLMask(TIntArrayList mask, EVT vt,
            boolean v2IsPlat)
    {
        return isCommutedMOVLMask(mask, vt, v2IsPlat, false);
    }

    static boolean isCommutedMOVLMask(TIntArrayList mask, EVT vt,
            boolean v2IsPlat, boolean v2IsUndef)
    {
        int numOps = vt.getVectorNumElements();
        if (numOps != 2 && numOps != 4 & numOps != 8 && numOps != 16)
            return false;
        if (!isUnderOrEqual(mask.get(0), 0))
            return false;

        for (int i = 1; i < numOps; i++)
        {
            if (!(isUnderOrEqual(mask.get(i), i+numOps) ||
                    (v2IsUndef && isUnderOrInRange(mask.get(i), numOps, numOps*2))||
                    (v2IsPlat && isUnderOrEqual(mask.get(i), numOps))))
                return false;
        }
        return true;
    }
    static boolean isCommutedMOVL(ShuffleVectorSDNode n)
    {
        return isCommutedMOVL(n, false);
    }
    static boolean isCommutedMOVL(ShuffleVectorSDNode n, boolean v2IsPlat)
    {
        return isCommutedMOVL(n, v2IsPlat, false);
    }
    static boolean isCommutedMOVL(ShuffleVectorSDNode n, boolean v2IsPlat, boolean v2IsUndef)
    {
        TIntArrayList mask = new TIntArrayList();
        n.getMask(mask);
        return isCommutedMOVLMask(mask, n.getValueType(0), v2IsPlat, v2IsUndef);
    }

    static boolean isCommutedSHUFPMask(TIntArrayList mask, EVT vt)
    {
        int numElts = vt.getVectorNumElements();
        if (numElts !=2 && numElts != 4)
            return false;

        int half = numElts / 2;
        for (int i = 0; i < half; i++)
            if (!isUnderOrInRange(mask.get(i), numElts, numElts*2))
                return false;
        for (int i = half; i < numElts; i++)
            if (!isUnderOrInRange(mask.get(i), 0, numElts))
                return false;
        return true;
    }

    static boolean isCommutedSHUFP(ShuffleVectorSDNode n)
    {
        TIntArrayList mask = new TIntArrayList();
        n.getMask(mask);
        return isCommutedSHUFPMask(mask, n.getValueType(0));
    }
    static boolean isUNPCKLMask(TIntArrayList mask, EVT vt)
    {
        return isUNPCKLMask(mask, vt, false);
    }
    static boolean isUNPCKLMask(TIntArrayList mask, EVT vt,
            boolean v2IsSplat)
    {
        int numElts = vt.getVectorNumElements();
        if (numElts !=2 && numElts != 4 && numElts != 8 && numElts != 16)
            return false;

        for (int i = 0, j = 0; i < numElts; i += 2, j++)
        {
            int bit1 = mask.get(i);
            int bit2 = mask.get(i+1);
            if (!isUnderOrEqual(bit1, j))
                return false;

            if (v2IsSplat)
            {
                if (!isUnderOrEqual(bit2, numElts))
                    return false;
            }
            else
            {
                if (!isUnderOrEqual(bit2, j+numElts))
                    return false;
            }
        }
        return true;
    }

    static boolean isUNPCKHMask(TIntArrayList mask, EVT vt)
    {
        return isUNPCKHMask(mask, vt, false);
    }

    static boolean isUNPCKHMask(TIntArrayList mask, EVT vt, boolean v2IsSplat)
    {
        int numElts = vt.getVectorNumElements();
        if (numElts !=2 && numElts != 4 && numElts != 8 && numElts != 16)
            return false;

        for (int i = 0, j = 0; i < numElts; i += 2, j++)
        {
            int bit1 = mask.get(i);
            int bit2 = mask.get(i+1);
            if (!isUnderOrEqual(bit1, j + numElts/2))
                return false;

            if (v2IsSplat)
            {
                if (!isUnderOrEqual(bit2, numElts))
                    return false;
            }
            else
            {
                if (!isUnderOrEqual(bit2, j+numElts/2+numElts))
                    return false;
            }
        }
        return true;
    }

    static boolean isUNPCKL_v_under_mask(TIntArrayList mask, EVT vt)
    {
        int numElts = vt.getVectorNumElements();
        if (numElts != 2 && numElts != 4 && numElts != 8 && numElts != 16)
            return false;

        for (int i = 0, j = 0; i < numElts; i += 2, j++)
        {
            int bit1 = mask.get(i);
            int bit2 = mask.get(i+1);
            if (!isUnderOrEqual(bit1, j))
                return false;
            if (!isUnderOrEqual(bit2, j))
                return false;
        }
        return true;
    }

    static boolean isUNPCKH_v_under_mask(TIntArrayList mask, EVT vt)
    {
        int numElts = vt.getVectorNumElements();
        if (numElts != 2 && numElts != 4 && numElts != 8 && numElts != 16)
            return false;

        for (int i = 0, j = numElts/2; i < numElts; i += 2, j++)
        {
            int bit1 = mask.get(i);
            int bit2 = mask.get(i+1);
            if (!isUnderOrEqual(bit1, j))
                return false;
            if (!isUnderOrEqual(bit2, j))
                return false;
        }
        return true;
    }

    @Override
    public boolean isShuffleMaskLegal(TIntArrayList mask, EVT vt)
    {
        if (vt.getSizeInBits() == 64)
            return false;

        return vt.getVectorNumElements() == 2 ||
                ShuffleVectorSDNode.isSplatMask(mask.toArray(), vt) ||
                isMOVLMask(mask, vt) ||
                isSHUFPMask(mask, vt) ||
                isPSHUFDMask(mask, vt) ||
                isPSHUFHWMask(mask, vt) ||
                isPSHUFLWMask(mask, vt) ||
                isUNPCKLMask(mask, vt) ||
                isUNPCKHMask(mask, vt) ||
                isUNPCKL_v_under_mask(mask, vt) ||
                isUNPCKH_v_under_mask(mask, vt);
    }

    private SDValue getOnesVector(EVT vt, SelectionDAG dag)
    {
        assert vt.isVector();

        SDValue cst = dag.getTargetConstant(~0, new EVT(MVT.i32));
        SDValue vec;
        if (vt.getSizeInBits() == 64)   // MMX
            vec = dag.getNode(ISD.BUILD_VECTOR, new EVT(MVT.v2i32), cst, cst);
        else
            vec = dag.getNode(ISD.BUILD_VECTOR, new EVT(MVT.v4i32), cst,cst,cst,cst);
        return dag.getNode(ISD.BIT_CONVERT, vt, vec);
    }

    private static SDValue getZeroVector(EVT vt, boolean hasSSE2, SelectionDAG dag)
    {
        assert vt.isVector();
        SDValue vec;
        if (vt.getSizeInBits() == 64)
        {
            SDValue cst = dag.getTargetConstant(0, new EVT(MVT.i32));
            vec = dag.getNode(ISD.BUILD_VECTOR, new EVT(MVT.v2i32), cst, cst);
        }
        else if (hasSSE2)
        {
            SDValue cst = dag.getTargetConstant(0, new EVT(MVT.i32));
            vec = dag.getNode(ISD.BUILD_VECTOR, new EVT(MVT.v4i32), cst,cst,cst,cst);
        }
        else
        {
            // SSE1
            SDValue cst = dag.getTargetConstantFP(+0.0, new EVT(MVT.f32));
            vec = dag.getNode(ISD.BUILD_VECTOR, new EVT(MVT.v4f32), cst,cst,cst,cst);
        }
        return dag.getNode(ISD.BIT_CONVERT, vt,vec);
    }

    private static SDValue getShuffleVectorZeroOrUndef(SDValue v1, int idx, boolean isZero,
                                                       boolean hasSSE2, SelectionDAG dag)
    {
        EVT vt = v1.getValueType();
        SDValue temp = isZero ? getZeroVector(vt, hasSSE2, dag) : dag.getUNDEF(vt);
        int numElts = vt.getVectorNumElements();
        TIntArrayList mask = new TIntArrayList();
        for (int i = 0; i < numElts; i++)
            mask.add(i == idx? numElts : i);
        return dag.getVectorShuffle(vt, temp, v1, mask.toArray());
    }

    private static SDValue getVShift(boolean isLeft, EVT vt, SDValue srcOp,
                                     int numBits, SelectionDAG dag,
                                     TargetLowering tli)
    {
        boolean isMMX = vt.getSizeInBits() == 64;
        EVT shVT = isMMX ? new EVT(MVT.v1i64) : new EVT(MVT.v2i64);
        int opc = isLeft ? X86ISD.VSHL : X86ISD.VSRL;
        srcOp = dag.getNode(ISD.BIT_CONVERT, shVT, srcOp);
        return dag.getNode(ISD.BIT_CONVERT, vt, dag.getNode(opc, shVT, srcOp,
                dag.getConstant(numBits, new EVT(tli.getShiftAmountTy()), false)));
    }

    /**
     * Custom lower build_vector of v16i8.
     * @param op
     * @param nonZeros
     * @param numNonZero
     * @param numZero
     * @param dag
     * @return
     */
    private static SDValue lowerBuildVectorv16i8(SDValue op, int nonZeros,
                                                 int numNonZero,
                                                 int numZero,
                                                 SelectionDAG dag)
    {
        if (numNonZero > 8)
            return new SDValue();

        SDValue v = new SDValue();
        boolean first = true;
        for (int i = 0; i < 16; i++)
        {
            boolean thisIsNonZero = (nonZeros & (1 << i)) != 0;
            if (thisIsNonZero && first)
            {
                if (numZero != 0)
                    v = getZeroVector(new EVT(MVT.v8i16), true, dag);
                else
                    v = dag.getUNDEF(new EVT(MVT.v8i16));
                first = false;
            }

            if ((i&1) != 0)
            {
                SDValue thisElt = new SDValue(), lastElt = new SDValue();
                boolean lastIsNonZero = (nonZeros & (1<< (i-1))) != 0;
                if (lastIsNonZero)
                    lastElt = dag.getNode(ISD.ZERO_EXTEND, new EVT(MVT.i16), op.getOperand(i-1));
                if (thisIsNonZero)

                {
                    thisElt = dag.getNode(ISD.ZERO_EXTEND, new EVT(MVT.i16), op.getOperand(i));
                    thisElt = dag.getNode(ISD.SHL, new EVT(MVT.i16), thisElt,
                            dag.getConstant(8, new EVT(MVT.i8), false));
                    if (lastIsNonZero)
                        thisElt = dag.getNode(ISD.OR, new EVT(MVT.i16), thisElt, lastElt);
                }
                else
                    thisElt = lastElt;

                if (thisElt.getNode() != null)
                    v = dag.getNode(ISD.INSERT_VECTOR_ELT, new EVT(MVT.v8i16), v, thisElt,
                            dag.getIntPtrConstant(i/2));
            }
        }
        return dag.getNode(ISD.BIT_CONVERT, new EVT(MVT.v16i8), v);
    }

    /**
     * Custom lower build_vector of v8i16.
     * @param op
     * @param nonZeros
     * @param numNonZero
     * @param numZero
     * @param dag
     * @return
     */
    private static SDValue lowerBuildVectorv8i16(SDValue op, int nonZeros,
                                                 int numNonZero,
                                                 int numZero,
                                                 SelectionDAG dag)
    {
        if (numNonZero > 4)
            return new SDValue();

        SDValue v = new SDValue();
        boolean isFirst = true;
        for (int i = 0; i< 8; i++)
        {
            boolean isNonZero = (nonZeros & (1<<i)) != 0;
            if (isNonZero)
            {
                if (isFirst)
                {
                    if (numZero != 0)
                        v = getZeroVector(new EVT(MVT.v8i16), true, dag);
                    else
                        v = dag.getUNDEF(new EVT(MVT.v8i16));
                    isFirst = false;
                }
                v = dag.getNode(ISD.INSERT_VECTOR_ELT, new EVT(MVT.v8i16), v,
                        op.getOperand(i), dag.getIntPtrConstant(i));
            }
        }
        return v;
    }

    private SDValue lowerBUILD_VECTOR(SDValue op, SelectionDAG dag)
    {
        if (isBuildVectorAllZeros(op.getNode()) ||
                isBuildVectorAllOnes(op.getNode()))
        {
            if (op.getValueType().getSimpleVT().simpleVT == MVT.v4i32 ||
                    op.getValueType().getSimpleVT().simpleVT == MVT.v2i32)
                return op;

            if (isBuildVectorAllOnes(op.getNode()))
                return getOnesVector(op.getValueType(), dag);
            return getZeroVector(op.getValueType(), subtarget.hasSSE2(), dag);
        }

        EVT vt = op.getValueType();
        EVT extVT = vt.getVectorElementType();
        int evtBits = extVT.getSizeInBits();
        int numElts = op.getNumOperands();
        int numZeros = 0;
        int numNonZeros = 0;
        int nonZeros = 0;
        boolean isAllConstants = true;
        ArrayList<SDValue> values = new ArrayList<>();
        for (int i = 0; i < numElts; i++)
        {
            SDValue elt = op.getOperand(i);
            if (elt.getOpcode() == ISD.UNDEF)
                continue;
            values.add(elt);
            if (elt.getOpcode() != ISD.Constant && elt.getOpcode() != ISD.ConstantFP)
                isAllConstants = false;
            if (X86.isZeroMode(elt))
                ++numZeros;
            else
            {
                nonZeros |= 1<<i;
                ++numNonZeros;
            }
        }

        if (numNonZeros == 0)
            return dag.getUNDEF(vt);

        if (numNonZeros == 1)
        {
            int idx = Util.countTrailingZeros(nonZeros);
            SDValue item = op.getOperand(idx);

            if (extVT.getSimpleVT().simpleVT == MVT.i64 && !subtarget.is64Bit() &&
                    (!isAllConstants || idx == 0))
            {
                if (dag.maskedValueIsZero(item, APInt.getBitsSet(64, 32, 64)))
                {
                    EVT vecVT = vt.equals(new EVT(MVT.v2i64)) ? new EVT(MVT.v4i32):new EVT(MVT.v2i32);
                    int vecElts = vt.equals(new EVT(MVT.v2i64)) ? 4: 2;
                    item = dag.getNode(ISD.TRUNCATE, new EVT(MVT.i32), item);
                    item = dag.getNode(ISD.SCALAR_TO_VECTOR, vecVT, item);
                    item = getShuffleVectorZeroOrUndef(item, 0, true, subtarget.hasSSE2(), dag);

                    if (idx != 0)
                    {
                        TIntArrayList mask = new TIntArrayList();
                        mask.fill(0, vecElts, idx);
                        item = dag.getVectorShuffle(vecVT, item, dag.getUNDEF(item.getValueType()),
                                mask.toArray());
                    }
                    return dag.getNode(ISD.BIT_CONVERT, op.getValueType(), item);
                }
            }

            if (idx == 0)
            {
                if (numZeros == 0)
                {
                    return dag.getNode(ISD.SCALAR_TO_VECTOR, vt, item);
                }
                else if (extVT.equals(new EVT(MVT.i32)) || extVT.equals(new EVT(MVT.f32)) ||
                        extVT.equals(new EVT(MVT.f64)) || (extVT.equals(new EVT(MVT.i64)) &&
                            subtarget.is64Bit()))
                {
                    item = dag.getNode(ISD.SCALAR_TO_VECTOR, vt, item);
                    return getShuffleVectorZeroOrUndef(item, 0, true, subtarget.hasSSE2(), dag);
                }
                else if (extVT.getSimpleVT().simpleVT == MVT.i16 || extVT.getSimpleVT().simpleVT == MVT.i8)
                {
                    item = dag.getNode(ISD.ZERO_EXTEND, new EVT(MVT.i32), item);
                    EVT middleVT = vt.getSizeInBits() == 64 ? new EVT(MVT.v2i32) : new EVT(MVT.v4i32);
                    item = getShuffleVectorZeroOrUndef(item, 0, true, subtarget.hasSSE2(), dag);
                    return dag.getNode(ISD.BIT_CONVERT, vt, item);
                }
            }

            if (numElts == 2 && idx == 1 && X86.isZeroMode(op.getOperand(0)) &&
                    !X86.isZeroMode(op.getOperand(1)))
            {
                int numBits = vt.getSizeInBits();
                return getVShift(true, vt, dag.getNode(ISD.SCALAR_TO_VECTOR, vt, op.getOperand(1)),
                        numBits/2, dag, this);
            }
            if (isAllConstants)
                return new SDValue();

            if (evtBits == 32)
            {
                item = dag.getNode(ISD.SCALAR_TO_VECTOR, vt, item);

                item = getShuffleVectorZeroOrUndef(item, 0, numZeros>0, subtarget.hasSSE2(), dag);
                TIntArrayList mask = new TIntArrayList();
                mask.fill(0, numElts, 1);
                if (idx >= 0 && idx < numElts)
                    mask.set(idx, 0);
                return dag.getVectorShuffle(vt, item, dag.getUNDEF(vt), mask.toArray());
            }
        }

        if (values.size() == 1 || isAllConstants)
            return new SDValue();

        if (evtBits == 64)
        {
            if (numNonZeros == 1)
            {
                int idx = Util.countTrailingZeros(nonZeros);
                SDValue v2 = dag.getNode(ISD.SCALAR_TO_VECTOR, vt, op.getOperand(idx));
                return getShuffleVectorZeroOrUndef(v2, idx, true, subtarget.hasSSE2(), dag);
            }
            return new SDValue();
        }
        // If element VT is < 32 bits, convert it to inserts into a zero vector.
        if (evtBits == 8 && numElts == 16)
        {
            SDValue v = lowerBuildVectorv16i8(op, nonZeros, numNonZeros, numZeros, dag);
            if (v.getNode() != null)
                return v;
        }

        if (evtBits == 16 && numElts == 8)
        {
            SDValue v = lowerBuildVectorv8i16(op, nonZeros, numNonZeros, numZeros, dag);
            if (v.getNode() != null)
                return v;
        }

        // If element VT is == 32 bits, turn it into a number of shuffles.
        SDValue[] vals = new SDValue[numElts];
        if (numElts == 4 && numZeros > 0)
        {
            for (int i = 0; i < 4; i++)
            {
                boolean isZero = (nonZeros & (1<<i)) == 0;
                if (isZero)
                    vals[i] = getZeroVector(vt, subtarget.hasSSE2(), dag);
                else
                    vals[i] = dag.getNode(ISD.SCALAR_TO_VECTOR, vt, op.getOperand(i));
            }

            for (int i = 0; i < 2; i++)
            {
                switch ((nonZeros & (0x3 << i*2)) >> (i*2))
                {
                    default:
                        break;
                    case 0:
                        vals[i] = vals[i*2];
                        break;
                    case 1:
                        vals[i] = getMOVL(dag, vt, vals[i*2+1], vals[i*2]);
                        break;
                    case 2:
                        vals[i] = getMOVL(dag, vt, vals[i*2], vals[i*2+1]);
                        break;
                    case 3:
                        vals[i] = getUnpackl(dag, vt, vals[i*2], vals[i*2+1]);
                        break;
                }
            }

            TIntArrayList maskVec = new TIntArrayList();
            boolean reverse = (nonZeros & 0x3) == 2;
            for (int i = 0; i < 2; i++)
                maskVec.add(reverse?1-i : i);

            reverse = ((nonZeros & (0x3<<2)) >> 2) == 2;
            for (int i = 0; i < 2; i++)
                maskVec.add(reverse? 1-i+numElts : i+numElts);
            return dag.getVectorShuffle(vt, vals[0], vals[1], maskVec.toArray());
        }

        if (values.size() > 2)
        {
            // If we have SSE 4.1, Expand into a number of inserts unless the number of
            // values to be inserted is equal to the number of elements, in which case
            // use the unpack code below in the hopes of matching the consecutive elts
            // load merge pattern for shuffles.
            if (values.size() < numElts && vt.getSizeInBits() == 128 &&
                    subtarget.hasSSE41())
            {
                vals[0] = dag.getUNDEF(vt);
                for (int i = 0; i < numElts; i++)
                    if (op.getOperand(i).getOpcode() != ISD.UNDEF)
                        vals[0] = dag.getNode(ISD.INSERT_VECTOR_ELT, vt, vals[0],
                                op.getOperand(i), dag.getIntPtrConstant(i));
                return vals[0];
            }

            // Expand into a number of unpckl*.
            // e.g. for v4f32
            //   Step 1: unpcklps 0, 2 ==> X: <?, ?, 2, 0>
            //         : unpcklps 1, 3 ==> Y: <?, ?, 3, 1>
            //   Step 2: unpcklps X, Y ==>    <3, 2, 1, 0>
            for (int i = 0; i < numElts; i++)
                vals[i] = dag.getNode(ISD.SCALAR_TO_VECTOR, vt, op.getOperand(i));

            numElts >>= 1;
            while (numElts != 0)
            {
                for (int i = 0; i < numElts; i++)
                    vals[i] = getUnpackl(dag, vt, vals[i], vals[i+numElts]);
                numElts >>= 1;
            }
            return vals[0];
        }
        return new SDValue();
    }
    private SDValue lowerVECTOR_SHUFFLE(SDValue op, SelectionDAG dag)
    {
        Util.shouldNotReachHere("Vector shuffle not supported!");
        return new SDValue();
    }
    private SDValue lowerEXTRACT_VECTOR_ELT(SDValue op, SelectionDAG dag)
    {
        Util.shouldNotReachHere("Vector shuffle not supported!");
        return new SDValue();
    }
    private SDValue lowerEXTRACT_VECTOR_ELT_SSE4(SDValue op, SelectionDAG dag)
    {
        Util.shouldNotReachHere("Vector shuffle not supported!");
        return new SDValue();
    }
    private SDValue lowerINSERT_VECTOR_ELT(SDValue op, SelectionDAG dag)
    {
        Util.shouldNotReachHere("Vector shuffle not supported!");
        return new SDValue();
    }
    private SDValue lowerINSERT_VECTOR_ELT_SSE4(SDValue op, SelectionDAG dag)
    {
        Util.shouldNotReachHere("Vector shuffle not supported!");
        return new SDValue();
    }
    private SDValue lowerSCALAR_TO_VECTOR(SDValue op, SelectionDAG dag)
    {
        Util.shouldNotReachHere("Vector shuffle not supported!");
        return new SDValue();
    }
    private SDValue lowerConstantPool(SDValue op, SelectionDAG dag)
    {
        ConstantPoolSDNode cpn = (ConstantPoolSDNode)op.getNode();
        int opFlag = 0;
        int wrapperKind = X86ISD.Wrapper;
        TargetMachine.CodeModel m = tm.getCodeModel();
        if (subtarget.isPICStyleRIPRel() && (m == Small || m == Kernel))
            wrapperKind = X86ISD.WrapperRIP;
        else if (subtarget.isPICStyleGOT())
            opFlag = X86II.MO_GOTOFF;
        else if (subtarget.isPICStyleStubPIC())
            opFlag = X86II.MO_PIC_BASE_OFFSET;
        SDValue result = dag.getTargetConstantPool(cpn.getConstantValue(),
                new EVT(getPointerTy()), cpn.getAlign(), cpn.getOffset(), opFlag);
        result = dag.getNode(wrapperKind, new EVT(getPointerTy()), result);
        if (opFlag != 0)
        {
            result = dag.getNode(ISD.ADD, new EVT(getPointerTy()),
                    dag.getNode(X86ISD.GlobalBaseReg, new EVT(getPointerTy())),
                    result);
        }
        return result;
    }

    private SDValue lowerGlobalAddress(SDValue op, SelectionDAG dag)
    {
        GlobalAddressSDNode gvn = (GlobalAddressSDNode)op.getNode();
        return lowerGlobalAddress(gvn.getGlobalValue(), gvn.getOffset(), dag);
    }

    private SDValue lowerGlobalAddress(GlobalValue gv, long offset, SelectionDAG dag)
    {
        int opFlags = subtarget.classifyGlobalReference(gv, getTargetMachine());
        TargetMachine.CodeModel model = getTargetMachine().getCodeModel();
        SDValue result;
        if (opFlags == X86II.MO_NO_FLAG && X86
                .isOffsetSuitableForCodeModel(offset, model))
        {
            result = dag
                    .getTargetGlobalAddress(gv, new EVT(getPointerTy()), offset,
                            0);
            offset = 0;
        }
        else
        {
            result = dag.getTargetGlobalAddress(gv, new EVT(getPointerTy()), 0,
                    opFlags);
        }

        int opc = (subtarget.isPICStyleRIPRel() && (model == Small || model == Kernel)) ? X86ISD.WrapperRIP : X86ISD.Wrapper;
        result = dag.getNode(opc, new EVT(getPointerTy()), result);
        if (isGlobalRelativeToPICBase(opFlags))
        {
            result = dag.getNode(ISD.ADD, new EVT(getPointerTy()),
                    dag.getNode(X86ISD.GlobalBaseReg, new EVT(getPointerTy())), result);
        }

        if (isGlobalStubReference(opFlags))
        {
            result = dag.getLoad(new EVT(getPointerTy()), dag.getEntryNode(), result,
                    PseudoSourceValue.getGOT(), 0);
        }

        if (offset != 0)
            result = dag.getNode(ISD.ADD, new EVT(getPointerTy()), result,
                    dag.getConstant(offset, new EVT(getPointerTy()), false));

        return result;
    }

    private static SDValue getTLSADDR(SelectionDAG dag, SDValue chain, GlobalAddressSDNode ga,
                                      SDValue inFlag, EVT ptrVT, int returnReg,
                                      int operandFlag)
    {
        SDVTList vts = dag.getVTList(new EVT(MVT.Other), new EVT(MVT.Flag));
        SDValue tga = dag.getTargetGlobalAddress(ga.getGlobalValue(), ga.getValueType(0),
                ga.getOffset(), operandFlag);
        if (inFlag != null)
        {
            SDValue[] ops = {chain, tga, inFlag};
            chain = dag.getNode(X86ISD.TLSADDR, vts, ops);
        }
        SDValue flag = chain.getValue(1);
        return dag.getCopyFromReg(chain, returnReg, ptrVT, flag);
    }

    private static SDValue lowerToTLSGeneralDynamicModel32(GlobalAddressSDNode ga,
                                                           SelectionDAG dag, EVT ptrVT)
    {
        SDValue inFlag = new SDValue();
        SDValue chain = dag.getCopyToReg(dag.getEntryNode(), X86GenRegisterNames.EBX,
                dag.getNode(X86ISD.GlobalBaseReg, ptrVT), inFlag);
        inFlag = chain.getValue(1);
        return getTLSADDR(dag, chain, ga, inFlag, ptrVT, X86GenRegisterNames.EAX, X86II.MO_TLSGD);
    }

    private static SDValue lowerToTLSGeneralDynamicModel64(
            GlobalAddressSDNode ga,
            SelectionDAG dag,
            EVT ptrVT)
    {
        return getTLSADDR(dag, dag.getEntryNode(), ga, null,
                ptrVT, X86GenRegisterNames.RAX, X86II.MO_TLSGD);
    }

    private static SDValue lowerTLSExecModel(
            GlobalAddressSDNode ga, SelectionDAG dag,
            EVT ptrVT, TLSModel model, boolean is64)
    {
        SDValue base = dag.getNode(X86ISD.SegmentBaseAddress, ptrVT,
                dag.getRegister(is64?X86GenRegisterNames.FS: X86GenRegisterNames.GS,
                        new EVT(MVT.i32)));
        SDValue threadPointer = dag.getLoad(ptrVT, dag.getEntryNode(), base, null, 0);

        int operandFlag = 0;
        int wrapperKind = X86ISD.Wrapper;
        if (model == LocalExec)
            operandFlag = is64? X86II.MO_TPOFF : X86II.MO_NTPOFF;
        else if (is64)
        {
            assert model == InitialExec;
            operandFlag = X86II.MO_GOTTPOFF;
            wrapperKind = X86ISD.WrapperRIP;
        }
        else
        {
            assert model == InitialExec;
            operandFlag = X86II.MO_INDNTPOFF;
        }

        // emit "addl x@ntpoff,%eax" (local exec) or "addl x@indntpoff,%eax" (initial
        // exec)
        SDValue tga = dag.getTargetGlobalAddress(ga.getGlobalValue(), ga.getValueType(0),
                ga.getOffset(), operandFlag);
        SDValue offset = dag.getNode(wrapperKind, ptrVT, tga);
        if (model == InitialExec)
        {
            offset = dag.getLoad(ptrVT, dag.getEntryNode(), offset,
                    PseudoSourceValue.getGOT(), 0);
        }
        return dag.getNode(ISD.ADD, ptrVT, threadPointer, offset);
    }

    private SDValue lowerGlobalTLSAddress(SDValue op, SelectionDAG dag)
    {
        assert subtarget.isTargetELF() :"TLS not implemented in other platform except for ELF!";
        GlobalAddressSDNode ga = (GlobalAddressSDNode)op.getNode();
        GlobalValue gv = ga.getGlobalValue();
        TLSModel model = getTLSModel(gv, tm.getRelocationModel());
        EVT ptr = new EVT(getPointerTy());
        switch (model)
        {
            case GeneralDynamic:
            case LocalDynamic:
                if (subtarget.is64Bit())
                    return lowerToTLSGeneralDynamicModel64(ga, dag, ptr);
                return lowerToTLSGeneralDynamicModel32(ga, dag, ptr);
            case InitialExec:
            case LocalExec:
                return lowerTLSExecModel(ga, dag, ptr, model, subtarget.is64Bit());
            default:
                Util.shouldNotReachHere();
                return new SDValue();
        }
    }

    private SDValue lowerExternalSymbol(SDValue op, SelectionDAG dag)
    {
        ExternalSymbolSDNode esn = (ExternalSymbolSDNode)op.getNode();
        String es = esn.getExtSymol();
        int opFlag = 0;
        int wrapperKind = X86ISD.Wrapper;
        TargetMachine.CodeModel model = getTargetMachine().getCodeModel();
        if (subtarget.isPICStyleRIPRel() &&
                (model == Small || model == Kernel))
            wrapperKind = X86ISD.WrapperRIP;
        else if (subtarget.isPICStyleGOT())
            opFlag = X86II.MO_GOTOFF;
        else if (subtarget.isPICStyleStubPIC())
            opFlag = X86II.MO_PIC_BASE_OFFSET;

        SDValue result = dag.getTargetExternalSymbol(es, new EVT(getPointerTy()), opFlag);
        result = dag.getNode(wrapperKind, new EVT(getPointerTy()), result);

        if (getTargetMachine().getRelocationModel() == PIC_ &&
                !subtarget.is64Bit())
        {
            result = dag.getNode(ISD.ADD, new EVT(getPointerTy()),
                    dag.getNode(X86ISD.GlobalBaseReg, new EVT(getPointerTy())),
                    result);
        }
        return result;
    }

    private SDValue lowerShift(SDValue op, SelectionDAG dag)
    {
        assert op.getNumOperands() == 3:"Not a double-shift!";
        EVT vt = op.getValueType();
        int vtBits = vt.getSizeInBits();
        EVT i8VT = new EVT(MVT.i8);

        boolean isSRA = op.getOpcode() == ISD.SRA_PARTS;
        SDValue shOpLo = op.getOperand(0);
        SDValue shOpHi = op.getOperand(1);
        SDValue shAmt = op.getOperand(2);
        SDValue temp1 = isSRA ? dag.getNode(ISD.SRA, vt, shOpHi,
                dag.getConstant(vtBits-1, i8VT, false)):
                dag.getConstant(0, vt, false);
        SDValue temp2, temp3;
        if (op.getOpcode() == ISD.SHL_PARTS)
        {
            temp2 = dag.getNode(X86ISD.SHLD, vt, shOpHi, shOpLo, shAmt);
            temp3 = dag.getNode(ISD.SHL, vt, shOpLo, shAmt);
        }
        else
        {
            temp2 = dag.getNode(X86ISD.SHRD, vt, shOpLo, shOpHi, shAmt);
            temp3 = dag.getNode(isSRA?ISD.SRA:ISD.SRL, vt, shOpHi, shAmt);
        }

        SDValue andNode = dag.getNode(ISD.AND, i8VT, shAmt, dag.getConstant(vtBits, i8VT, false));
        SDValue cond = dag.getNode(X86ISD.CMP, vt, andNode, dag.getConstant(0, i8VT, false));
        SDValue hi, lo;
        SDValue cc = dag.getConstant(COND_NE, i8VT, false);
        SDValue[] ops0 = {temp2, temp3, cc, cond};
        SDValue[] ops1 = {temp3, temp1, cc, cond};
        if (op.getOpcode() == ISD.SHL_PARTS)
        {
            hi = dag.getNode(X86ISD.CMOV, vt, ops0);
            lo = dag.getNode(X86ISD.CMOV, vt, ops1);
        }
        else
        {
            lo = dag.getNode(X86ISD.CMOV, vt, ops0);
            hi = dag.getNode(X86ISD.CMOV, vt, ops1);
        }
        SDValue[] ops = {lo, hi};
        return dag.getMergeValues(ops);
    }

    private SDValue buildFILD(SDValue op,
            EVT srcVT,
            SDValue chain,
            SDValue stackSlot,
            SelectionDAG dag)
    {
        SDVTList vts;
        boolean useSSE = isScalarFPTypeInSSEReg(op.getValueType());
        if (useSSE)
            vts = dag.getVTList(new EVT(MVT.f64), new EVT(MVT.Other), new EVT(MVT.Flag));
        else
            vts = dag.getVTList(op.getValueType(), new EVT(MVT.Other));

        ArrayList<SDValue> ops = new ArrayList<>();
        ops.add(chain);
        ops.add(stackSlot);
        ops.add(dag.getValueType(srcVT));
        SDValue result = dag.getNode(useSSE ? X86ISD.FILD_FLAG : X86ISD.FILD,
                vts, ops);
        if (useSSE)
        {
            chain = result.getValue(1);
            SDValue inFlag = result.getValue(2);

            MachineFunction mf = dag.getMachineFunction();
            int ssfi = mf.getFrameInfo().createStackObject(8, 8);
            SDValue slot = dag.getFrameIndex(ssfi, new EVT(getPointerTy()), false);
            vts = dag.getVTList(new EVT(MVT.Other));
            ArrayList<SDValue> operands = new ArrayList<>();
            operands.add(chain);
            operands.add(result);
            operands.add(slot);
            operands.add(dag.getValueType(op.getValueType()));
            operands.add(inFlag);
            chain = dag.getNode(X86ISD.FST, vts, operands);
            result = dag.getLoad(op.getValueType(), chain, slot,
                    PseudoSourceValue.getFixedStack(ssfi), 0);
        }
        return result;
    }

    private SDValue lowerSINT_TO_FP(SDValue op, SelectionDAG dag)
    {
        EVT srcVT = op.getOperand(0).getValueType();
        if (srcVT.isVector())
        {
            if (srcVT.equals(new EVT(MVT.v2i32)) && op.getValueType().equals(new EVT(MVT.v2f64)))
                return op;
            return new SDValue();
        }

        assert srcVT.getSimpleVT().simpleVT <= MVT.i64 &&
                srcVT.getSimpleVT().simpleVT >= MVT.i16:"Unknown SINT_TO_FP to lower!";

        if (srcVT.equals(new EVT(MVT.i32)) && isScalarFPTypeInSSEReg(op.getValueType()))
            return op;

        if (srcVT.equals(new EVT(MVT.i64)) && isScalarFPTypeInSSEReg(op.getValueType()) &&
                subtarget.is64Bit())
            return op;

        int size = srcVT.getSizeInBits()/8;
        MachineFunction mf = dag.getMachineFunction();
        int ssfi = mf.getFrameInfo().createStackObject(size, size);
        SDValue stackSlot = dag.getFrameIndex(ssfi, new EVT(getPointerTy()), false);
        SDValue chain = dag.getStore(dag.getEntryNode(), op.getOperand(0),
                stackSlot, PseudoSourceValue.getFixedStack(ssfi), 0, false, 0);
        return buildFILD(op, srcVT, chain, stackSlot, dag);
    }

    private SDValue lowerUINT_TO_FP(SDValue op, SelectionDAG dag)
    {
        SDValue n0 = op.getOperand(0);
        if (dag.signBitIsZero(n0, 0))
            return dag.getNode(ISD.SINT_TO_FP, op.getValueType(), n0);

        EVT srcVT = n0.getValueType();
        if (srcVT.getSimpleVT().simpleVT == MVT.i64)
        {
            // We only handle SSE2 f64 target here; caller can expand the rest.
            if (op.getValueType().getSimpleVT().simpleVT != MVT.f64 ||
                    !x86ScalarSSEf64)
                return new SDValue();
            return lowerUINT_TO_FP_i64(op, dag);
        }
        else if (srcVT.getSimpleVT().simpleVT == MVT.i32 && x86ScalarSSEf64)
            return lowerUINT_TO_FP_i32(op, dag);

        assert srcVT.getSimpleVT().simpleVT == MVT.i32:"Unknown UINT_TO_FP to lower!";

        SDValue stackSlot = dag.createStackTemporary(new EVT(MVT.i64));
        SDValue wordOff = dag.getConstant(4, new EVT(getPointerTy()), false);
        SDValue offsetSlot = dag.getNode(ISD.ADD, new EVT(getPointerTy()),
                stackSlot, wordOff);
        SDValue st1 = dag.getStore(dag.getEntryNode(), op.getOperand(0),
                stackSlot, null, 0, false, 0);
        SDValue st2 = dag.getStore(st1, dag.getConstant(0, new EVT(MVT.i32), false),
                offsetSlot, null, 0, false, 0);
        return buildFILD(op, new EVT(MVT.i64), st2, stackSlot, dag);
    }

    /**
     * Converts unsigned i64 to float point type.
     * @param op
     * @param dag
     * @return
     */
    private SDValue lowerUINT_TO_FP_i64(SDValue op, SelectionDAG dag)
    {
        ArrayList<Constant> cs = new ArrayList<>();
        cs.add(ConstantInt.get(new APInt(32, 0x45300000)));
        cs.add(ConstantInt.get(new APInt(32, 0x43300000)));
        cs.add(ConstantInt.get(new APInt(32,0)));
        cs.add(ConstantInt.get(new APInt(32,0)));
        Constant cv = ConstantVector.get(cs);
        SDValue cpIdx0 = dag.getConstantPool(cv, new EVT(getPointerTy()), 16,
                0, false, 0);
        ArrayList<Constant> cv1 = new ArrayList<>();
        cv1.add(ConstantFP.get(new APFloat(new APInt(64, 0x4530000000000000L))));
        cv1.add(ConstantFP.get(new APFloat(new APInt(64, 0x4330000000000000L))));
        Constant c1 = ConstantVector.get(cv1);
        SDValue cpIdx1 = dag.getConstantPool(c1, new EVT(getPointerTy()), 16,
                0, false, 0);

        SDValue xr1 = dag.getNode(ISD.SCALAR_TO_VECTOR, new EVT(MVT.v4i32),
                dag.getNode(ISD.EXTRACT_ELEMENT, new EVT(MVT.i32),
                        op.getOperand(0), dag.getIntPtrConstant(1)));
        SDValue xr2 = dag.getNode(ISD.SCALAR_TO_VECTOR, new EVT(MVT.v4i32),
                dag.getNode(ISD.EXTRACT_ELEMENT, new EVT(MVT.i32),
                        op.getOperand(0), dag.getIntPtrConstant(0)));
        SDValue unpack1 = getUnpackl(dag, new EVT(MVT.v4i32), xr1, xr2);
        SDValue ld0 = dag.getLoad(new EVT(MVT.v4i32), dag.getEntryNode(),
                cpIdx0, PseudoSourceValue.getConstantPool(), 0,
                false, 16);
        SDValue unpack2 = getUnpackl(dag, new EVT(MVT.v4i32), unpack1, ld0);
        SDValue xr2f = dag.getNode(ISD.BIT_CONVERT, new EVT(MVT.v4i32), unpack2);
        SDValue ld1 = dag.getLoad(new EVT(MVT.v2f64), ld0.getValue(1),
                cpIdx1, PseudoSourceValue.getConstantPool(), 0, false, 16);
        SDValue sub = dag.getNode(ISD.FSUB, new EVT(MVT.v2f64), xr2f, ld1);

        int[] shufMask = {1, -1};
        SDValue shuf = dag.getVectorShuffle(new EVT(MVT.v2f64), sub,
                dag.getUNDEF(new EVT(MVT.v2f64)), shufMask);
        SDValue add = dag.getNode(ISD.FADD, new EVT(MVT.v2f64), shuf, sub);
        return dag.getNode(ISD.EXTRACT_VECTOR_ELT, new EVT(MVT.f64), add,
                dag.getIntPtrConstant(0));
    }

    private SDValue lowerUINT_TO_FP_i32(SDValue op, SelectionDAG dag)
    {
        SDValue bias = dag.getConstantFP(Util.bitsToDouble(0x4330000000000000L),
                new EVT(MVT.f64), false);
        SDValue load = dag.getNode(ISD.SCALAR_TO_VECTOR, new EVT(MVT.v4i32),
                dag.getNode(ISD.EXTRACT_ELEMENT, new EVT(MVT.i32),
                        op.getOperand(0), dag.getIntPtrConstant(0)));

        load = dag.getNode(ISD.EXTRACT_VECTOR_ELT, new EVT(MVT.f64),
                dag.getNode(ISD.BIT_CONVERT, new EVT(MVT.v2f64), load),
                dag.getIntPtrConstant(0));
        SDValue or = dag.getNode(ISD.OR, new EVT(MVT.v2i64),
                dag.getNode(ISD.BIT_CONVERT, new EVT(MVT.v2i64),
                        dag.getNode(ISD.SCALAR_TO_VECTOR, new EVT(MVT.v2f64), load)),
                dag.getNode(ISD.BIT_CONVERT, new EVT(MVT.v2i64),
                        dag.getNode(ISD.SCALAR_TO_VECTOR, new EVT(MVT.v2f64), bias)));
        or = dag.getNode(ISD.EXTRACT_VECTOR_ELT, new EVT(MVT.f64),
                dag.getNode(ISD.BIT_CONVERT, new EVT(MVT.v2f64), or),
                dag.getIntPtrConstant(0));

        SDValue sub = dag.getNode(ISD.FSUB, new EVT(MVT.f64), or, bias);
        EVT destVT = op.getValueType();
        if (destVT.bitsLT(new EVT(MVT.f64)))
            return dag.getNode(ISD.FP_ROUND, destVT, sub, dag.getIntPtrConstant(0));
        else if (destVT.bitsGT(new EVT(MVT.f64)))
            return dag.getNode(ISD.FP_EXTEND, destVT, sub);

        return sub;
    }

    private SDValue lowerFP_TO_SINT(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    private SDValue lowerFP_TO_UINT(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    private SDValue lowerFABS(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    private SDValue lowerFNEG(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    private SDValue lowerFCOPYSIGN(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    private SDValue lowerSETCC(SDValue op, SelectionDAG dag)
    {
        assert op.getValueType().getSimpleVT().simpleVT ==MVT.i8:
                "SetCC type must be 8-bits integer!";
        SDValue op0 = op.getOperand(0);
        SDValue op1 = op.getOperand(1);
        backend.codegen.dagisel.CondCode cc = ((CondCodeSDNode)op.getOperand(2).getNode()).getCondition();
        // lower (X & (1<<N)) == 0 --> BT(X, N)
        // lower ((X >>u N) & 1) != 0 --> BT(X, N)
        // lower ((X >>s N) & 1) != 0 --> BT(X, N)
        if (op0.getOpcode() == ISD.AND &&
                op0.hasOneUse() &&
                op1.getOpcode() == ISD.Constant &&
                ((ConstantSDNode)op1.getNode()).getZExtValue() == 0 &&
                (cc == SETEQ || cc == SETNE))
        {
            SDValue lhs = new SDValue(), rhs = new SDValue();
            if (op0.getOperand(1).getOpcode() == ISD.SHL)
            {
                if (op0.getOperand(1).getOperand(1).getNode() instanceof
                        ConstantSDNode)
                {
                    ConstantSDNode csd = (ConstantSDNode)op0.getOperand(1).getOperand(1).getNode();
                    if (csd.getZExtValue() == 1)
                    {
                        lhs = op0.getOperand(0);
                        rhs = op0.getOperand(1).getOperand(1);
                    }
                }
            }
            else if (op0.getOperand(0).getOpcode() == ISD.SHL)
            {
                if (op0.getOperand(1).getOperand(0).getNode() instanceof ConstantSDNode)
                {
                    ConstantSDNode csd = (ConstantSDNode)op0.getOperand(1).getOperand(0).getNode();
                    if (csd.getZExtValue() == 1)
                    {
                        lhs = op0.getOperand(0);
                        rhs = op0.getOperand(1).getOperand(1);
                    }
                }
            }
            else if (op0.getOperand(1).getOpcode() == ISD.Constant)
            {
                ConstantSDNode andRhs = (ConstantSDNode)op0.getOperand(1).getNode();
                SDValue andLhs = op0.getOperand(0);
                if (andRhs.getZExtValue() == 1 && andLhs.getOpcode() == ISD.SRL)
                {
                    lhs = andLhs.getOperand(0);
                    rhs = andLhs.getOperand(1);
                }
            }

            if (lhs.getNode() != null)
            {
                if (lhs.getValueType().getSimpleVT().simpleVT == MVT.i8)
                    lhs = dag.getNode(ISD.ANY_EXTEND, new EVT(MVT.i32), lhs);
                if (!lhs.getValueType().equals(rhs.getValueType()))
                    rhs = dag.getNode(ISD.ANY_EXTEND, lhs.getValueType(), rhs);

                SDValue bt = dag.getNode(X86ISD.BT, new EVT(MVT.i32), lhs, rhs);
                int cond = cc == SETEQ ? COND_AE : COND_B;
                return dag.getNode(X86ISD.SETCC, new EVT(MVT.i8),
                        dag.getConstant(cond, new EVT(MVT.i8), false), bt);
            }
        }
        boolean isFP = op.getOperand(1).getValueType().isFloatingPoint();
        int x86cc = translateX86CC(cc, isFP, op0, op1, dag);
        SDValue cond = emitCmp(op0, op1, x86cc, dag);
        return dag.getNode(X86ISD.SETCC, new EVT(MVT.i8),
                dag.getConstant(x86cc, new EVT(MVT.i8), false), cond);
    }

    /**
     * Translate the CondCode into X86 specific condition code.
     * @param cc
     * @param isFP
     * @param lhs
     * @param rhs
     * @return
     */
    private int translateX86CC(backend.codegen.dagisel.CondCode cc,
                               boolean isFP,
                               SDValue lhs,
                               SDValue rhs,
                               SelectionDAG dag)
    {
        if (!isFP)
        {
            if (rhs.getNode() instanceof ConstantSDNode)
            {
                ConstantSDNode rhsC = (ConstantSDNode)rhs.getNode();
                if (cc == SETGT && rhsC.isAllOnesValue())
                {
                    rhs =dag.getConstant(0, rhs.getValueType(), false);
                    return COND_NS;
                }
                else if (cc == SETLT && rhsC.isNullValue())
                {
                    return COND_S;
                }
                else if (cc == SETLT && rhsC.getZExtValue() ==1)
                {
                    rhs = dag.getConstant(0, rhs.getValueType(), false);
                    return COND_LE;
                }
            }
            switch(cc)
            {
                default:
                    Util.shouldNotReachHere("Invalid integer condition!");
                    break;
                case SETEQ: return COND_E;
                case SETGT:  return COND_G;
                case SETGE:  return COND_GE;
                case SETLT:  return COND_L;
                case SETLE:  return COND_LE;
                case SETNE:  return COND_NE;
                case SETULT: return COND_B;
                case SETUGT: return COND_A;
                case SETULE: return COND_BE;
                case SETUGE: return COND_AE;
            }
        }
        if (lhs.getNode().isNONExtLoad() && lhs.hasOneUse() &&
                !(rhs.getNode().isNONExtLoad() && rhs.hasOneUse()))
        {
            cc = getSetCCSwappedOperands(cc);
            SDValue t = lhs;
            lhs = rhs;
            rhs = t;
        }
        switch (cc)
        {
            default: break;
            case SETOLT:
            case SETOLE:
            case SETUGT:
            case SETUGE:
            {
                SDValue t = lhs;
                lhs = rhs;
                rhs = t;
                break;
            }
        }

        // On a floating point condition, the flags are set as follows:
        // ZF  PF  CF   op
        //  0 | 0 | 0 | X > Y
        //  0 | 0 | 1 | X < Y
        //  1 | 0 | 0 | X == Y
        //  1 | 1 | 1 | unordered
        switch (cc)
        {
            default:
                Util.shouldNotReachHere("CondCode shoult be pre-legalized!");
                break;
            case SETUEQ:
            case SETEQ:   return COND_E;
            case SETOLT:              // flipped
            case SETOGT:
            case SETGT:   return COND_A;
            case SETOLE:              // flipped
            case SETOGE:
            case SETGE:   return COND_AE;
            case SETUGT:              // flipped
            case SETULT:
            case SETLT:   return COND_B;
            case SETUGE:              // flipped
            case SETULE:
            case SETLE:   return COND_BE;
            case SETONE:
            case SETNE:   return COND_NE;
            case SETUO:   return COND_P;
            case SETO:    return COND_NP;
        }
        return COND_INVALID;
    }

    private SDValue lowerVSETCC(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    private SDValue lowerSELECT(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }

    /**
     * Return true if the specified op is a X86 logical comparison.
     */
    private static boolean isX86LogicalCmp(SDValue op)
    {
        int opc = op.getOpcode();
        if (opc == X86ISD.CMP || opc == X86ISD.COMI || opc == X86ISD.UCOMI)
        {
            return true;
        }
        return (op.getResNo() == 1 &&
            (opc == X86ISD.ADD ||
             opc == X86ISD.SUB ||
             opc == X86ISD.SMUL ||
             opc == X86ISD.UMUL ||
             opc == X86ISD.INC ||
             opc == X86ISD.DEC));
    }

    private static int isAndOrOfSetCCs(SDValue op)
    {
      int opc = op.getOpcode();
      if (opc != ISD.OR && opc != ISD.AND)
        return -1;
      return (op.getOperand(0).getOpcode() == X86ISD.SETCC &&
          op.getOperand(0).hasOneUse() &&
          op.getOperand(1).getOpcode() == X86ISD.SETCC &&
          op.getOperand(1).hasOneUse()) ? opc : -1;
    }

    /**
     * Return true if the specified SDValue is an ISD.XOR of a
     * X86ISD.SETCC and 1 that this SetCC node has a single use.
     */
    private static boolean isXor10OfSetCC(SDValue op)
    {
        if (op.getOpcode() != ISD.XOR)
          return false;
        ConstantSDNode rhsC = op.getOperand(1).getNode() instanceof ConstantSDNode ?
          (ConstantSDNode)op.getOperand(1).getNode() : null;
        if (rhsC != null && rhsC.getAPIntValue().eq(1))
        {
            return op.getOperand(0).getOpcode() == X86ISD.SETCC &&
              op.getOperand(0).hasOneUse();
        }
        return false;
    }

    private SDValue lowerBRCOND(SDValue op, SelectionDAG dag)
    {
        boolean addTest = true;
        SDValue chain = op.getOperand(0);
        SDValue cond = op.getOperand(1);
        SDValue dest = op.getOperand(2);
        SDValue cc = new SDValue();
        if (cond.getOpcode() == ISD.SETCC)
          cond = lowerSETCC(cond, dag);
        if (cond.getOpcode() == X86ISD.SETCC)
        {
            // If condition flag is set by a X86ISD.CMP, then use it as the
            // condition directly instead of using X86ISD.SETCC.
            cc = cond.getOperand(0);
            SDValue cmp = cond.getOperand(1);
            int opc = cmp.getOpcode();
            if (isX86LogicalCmp(cmp) || opc == X86ISD.BT)
            {
                cond = cmp;
                addTest = false;
            }
            else
            {
                switch ((int) ((ConstantSDNode)cc.getNode()).getZExtValue())
                {
                    default: break;
                    case COND_O:
                    case COND_B:
                        cond = cond.getNode().getOperand(1);
                        addTest = false;
                        break;
                }
            }
        }
        else
        {
            int condOpc;
            if (cond.hasOneUse() && (condOpc = isAndOrOfSetCCs(cond)) != -1)
            {
                SDValue cmp = cond.getOperand(0).getOperand(1);
                if (condOpc == ISD.OR)
                {
                    if (cmp.equals(cond.getOperand(1).getOperand(1)) &&
                        isX86LogicalCmp(cmp))
                    {
                        cc = cond.getOperand(0).getOperand(0);
                        chain = dag.getNode(X86ISD.BRCOND, op.getValueType(),
                            chain, dest, cc, cmp);
                        cc = cond.getOperand(1).getOperand(0);
                        cond = cmp;
                        addTest = false;
                    }
                }
                else
                {
                    // ISD.AND
                    if (cmp.equals(cond.getOperand(1).getOperand(1)) &&
                        isX86LogicalCmp(cmp) &&
                        op.getNode().hasOneUse())
                    {
                        long cccode = cond.getOperand(0).getConstantOperandVal(0);
                        cccode = X86.getOppositeBranchCondition(cccode);
                        cc = dag.getConstant(cccode, new EVT(MVT.i8), false);
                        SDValue user = new SDValue(op.getNode().getUse(0).getNode(), 0);
                        if (user.getOpcode() == ISD.BR)
                        {
                            SDValue falseBB = user.getOperand(1);
                            SDValue newBR = dag.updateNodeOperands(user,
                                user.getOperand(0), dest);
                            assert newBR.equals(user);
                            dest = falseBB;
                            chain = dag.getNode(X86ISD.BRCOND, op.getValueType(),
                                chain, dest, cc, cmp);
                            long cccode2 = cond.getOperand(1).getConstantOperandVal(0);
                            cccode2 = X86.getOppositeBranchCondition(cccode2);
                            cc = dag.getConstant(cccode2, new EVT(MVT.i8), false);
                            cond = cmp;
                            addTest = false;
                        }
                    }
                }
            }
            else if (cond.hasOneUse() && isXor10OfSetCC(cond))
            {
                long cccode = cond.getOperand(0).getConstantOperandVal(0);
                cccode = X86.getOppositeBranchCondition(cccode);
                cc = dag.getConstant(cccode, new EVT(MVT.i8), false);
                cond = cond.getOperand(0).getOperand(1);
                addTest = false;
            }
        }
        if (addTest)
        {
            cc = dag.getConstant(COND_NE, new EVT(MVT.i8), false);
            cond = emitTest(cond, COND_NE, dag);
        }
        return dag.getNode(X86ISD.BRCOND, op.getValueType(), chain, dest, cc, cond);
    }
    private SDValue lowerMEMSET(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    private SDValue lowerJumpTable(SDValue op, SelectionDAG dag)
    {
        JumpTableSDNode jtn = (JumpTableSDNode)op.getNode();

        int opFlag = 0;
        int wrapperKind = X86ISD.Wrapper;
        TargetMachine.CodeModel m = tm.getCodeModel();

        if (subtarget.isPICStyleRIPRel() && (m == Small || m == Kernel))
            wrapperKind = X86ISD.WrapperRIP;
        else if (subtarget.isPICStyleGOT())
            opFlag = X86II.MO_GOTOFF;
        else if (subtarget.isPICStyleStubPIC())
            opFlag = X86II.MO_PIC_BASE_OFFSET;

        EVT pty = new EVT(getPointerTy());
        SDValue result = dag.getTargetJumpTable(jtn.getJumpTableIndex(), pty, opFlag);
        result = dag.getNode(wrapperKind, pty, result);
        // With PIC, the address is actually $g + Offset.
        if (opFlag != 0)
        {
            result = dag.getNode(ISD.ADD, pty, dag.getNode(X86ISD.GlobalBaseReg, pty), result);
        }
        return result;
    }
    private SDValue lowerDYNAMIC_STACKALLOC(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    private SDValue lowerVASTART(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    private SDValue lowerVAARG(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    private SDValue lowerVACOPY(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    private SDValue lowerINTRINSIC_WO_CHAIN(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    private SDValue lowerRETURNADDR(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    private SDValue lowerFRAMEADDR(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    private SDValue lowerFRAME_TO_ARGS_OFFSET(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    private SDValue lowerEH_RETURN(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    private SDValue lowerTRAMPOLINE(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    private SDValue lowerFLT_ROUNDS_(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    private SDValue lowerCTLZ(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    private SDValue lowerCTTZ(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    private SDValue lowerMUL_V2I64(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    private SDValue lowerXALUO(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }

    private SDValue lowerCMP_SWAP(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    private SDValue lowerLOAD_SUB(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    private SDValue lowerREADCYCLECOUNTER(SDValue op, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }
    
    /**
     * Provide a customized hook for the X86 architecture with specified operation.
     * @param op
     * @param dag
     * @return
     */
    @Override
    public SDValue lowerOperation(SDValue op, SelectionDAG dag)
    {
        switch (op.getOpcode())
        {
            case ISD.ATOMIC_CMP_SWAP:    return lowerCMP_SWAP(op, dag);
            case ISD.ATOMIC_LOAD_SUB:    return lowerLOAD_SUB(op, dag);
            case ISD.BUILD_VECTOR:       return lowerBUILD_VECTOR(op, dag);
            case ISD.VECTOR_SHUFFLE:     return lowerVECTOR_SHUFFLE(op, dag);
            case ISD.EXTRACT_VECTOR_ELT: return lowerEXTRACT_VECTOR_ELT(op, dag);
            case ISD.INSERT_VECTOR_ELT:  return lowerINSERT_VECTOR_ELT(op, dag);
            case ISD.SCALAR_TO_VECTOR:   return lowerSCALAR_TO_VECTOR(op, dag);
            case ISD.ConstantPool:       return lowerConstantPool(op, dag);
            case ISD.GlobalAddress:      return lowerGlobalAddress(op, dag);
            case ISD.GlobalTLSAddress:   return lowerGlobalTLSAddress(op, dag);
            case ISD.ExternalSymbol:     return lowerExternalSymbol(op, dag);
            case ISD.SHL_PARTS:
            case ISD.SRA_PARTS:
            case ISD.SRL_PARTS:          return lowerShift(op, dag);
            case ISD.SINT_TO_FP:         return lowerSINT_TO_FP(op, dag);
            case ISD.UINT_TO_FP:         return lowerUINT_TO_FP(op, dag);
            case ISD.FP_TO_SINT:         return lowerFP_TO_SINT(op, dag);
            case ISD.FP_TO_UINT:         return lowerFP_TO_UINT(op, dag);
            case ISD.FABS:               return lowerFABS(op, dag);
            case ISD.FNEG:               return lowerFNEG(op, dag);
            case ISD.FCOPYSIGN:          return lowerFCOPYSIGN(op, dag);
            case ISD.SETCC:              return lowerSETCC(op, dag);
            case ISD.VSETCC:             return lowerVSETCC(op, dag);
            case ISD.SELECT:             return lowerSELECT(op, dag);
            case ISD.BRCOND:             return lowerBRCOND(op, dag);
            case ISD.JumpTable:          return lowerJumpTable(op, dag);
            case ISD.VASTART:            return lowerVASTART(op, dag);
            case ISD.VAARG:              return lowerVAARG(op, dag);
            case ISD.VACOPY:             return lowerVACOPY(op, dag);
            case ISD.INTRINSIC_WO_CHAIN: return lowerINTRINSIC_WO_CHAIN(op, dag);
            case ISD.RETURNADDR:         return lowerRETURNADDR(op, dag);
            case ISD.FRAMEADDR:          return lowerFRAMEADDR(op, dag);
            case ISD.FRAME_TO_ARGS_OFFSET:
                return lowerFRAME_TO_ARGS_OFFSET(op, dag);
            case ISD.DYNAMIC_STACKALLOC: return lowerDYNAMIC_STACKALLOC(op, dag);
            case ISD.EH_RETURN:          return lowerEH_RETURN(op, dag);
            case ISD.TRAMPOLINE:         return lowerTRAMPOLINE(op, dag);
            case ISD.FLT_ROUNDS_:        return lowerFLT_ROUNDS_(op, dag);
            case ISD.CTLZ:               return lowerCTLZ(op, dag);
            case ISD.CTTZ:               return lowerCTTZ(op, dag);
            case ISD.MUL:                return lowerMUL_V2I64(op, dag);
            case ISD.SADDO:
            case ISD.UADDO:
            case ISD.SSUBO:
            case ISD.USUBO:
            case ISD.SMULO:
            case ISD.UMULO:              return lowerXALUO(op, dag);
            case ISD.READCYCLECOUNTER:   return lowerREADCYCLECOUNTER(op, dag);
            default:
                Util.shouldNotReachHere("Should not custom lower this!");
                return new SDValue();
        }
    }

    private Pair<SDValue,SDValue> FPToIntHelper(SDValue op,
            SelectionDAG dag, boolean isSigned)
    {
        EVT destTy = op.getValueType();
        if (!isSigned)
        {
            assert destTy.getSimpleVT().simpleVT == MVT.i32
                    :"Unexpected FP_TO_UINT";
            destTy = new EVT(MVT.i64);
        }

        assert destTy.getSimpleVT().simpleVT <= MVT.i64 &&
                destTy.getSimpleVT().simpleVT >= MVT.i16:
                "Unknown FP_TO_SINT to lower!";

        if (destTy.getSimpleVT().simpleVT == MVT.i32 &&
                isScalarFPTypeInSSEReg(op.getOperand(0).getValueType()))
            return Pair.get(new SDValue(),  new SDValue());

        if (subtarget.is64Bit() && destTy.getSimpleVT().simpleVT == MVT.i64 &&
                isScalarFPTypeInSSEReg(op.getOperand(0).getValueType()))
            return Pair.get(new SDValue(),  new SDValue());

        MachineFunction mf = dag.getMachineFunction();
        int memSize = destTy.getSizeInBits()/8;
        int ssfi = mf.getFrameInfo().createStackObject(memSize, memSize);
        SDValue stackSlot = dag.getFrameIndex(ssfi, new EVT(getPointerTy()), false);

        int opc = 0;
        switch (destTy.getSimpleVT().simpleVT)
        {
            default:
                Util.shouldNotReachHere("Invalid FP_TO_SINT to lower!");
                break;
            case MVT.i16:
                opc = X86ISD.FP_TO_INT16_IN_MEM; break;
            case MVT.i32:
                opc = X86ISD.FP_TO_INT32_IN_MEM;
                break;
            case MVT.i64:
                opc = X86ISD.FP_TO_INT64_IN_MEM;
                break;
        }
        SDValue chain = dag.getEntryNode();
        SDValue value = op.getOperand(0);
        if (isScalarFPTypeInSSEReg(op.getOperand(0).getValueType()))
        {
            assert destTy.getSimpleVT().simpleVT == MVT.i64:
                    "Invalid FP_TO_SINT to lower!";
            chain = dag.getStore(chain, value, stackSlot,
                    PseudoSourceValue.getFixedStack(ssfi), 0, false, 0);
            SDVTList vts = dag.getVTList(op.getOperand(0).getValueType(),
                    new EVT(MVT.Other));
            SDValue[] ops = {chain, stackSlot,
                    dag.getValueType(op.getOperand(0).getValueType())};
            value = dag.getNode(X86ISD.FLD, vts, ops);
            chain = value.getValue(1);
            ssfi = mf.getFrameInfo().createStackObject(memSize, memSize);
            stackSlot = dag.getFrameIndex(ssfi, new EVT(getPointerTy()), false);
        }

        SDValue[] ops = {chain, value, stackSlot};
        SDValue fist = dag.getNode(opc, new EVT(MVT.Other), ops);
        return Pair.get(fist, stackSlot);
    }
    /**
     * Replace a node with an illegal result type with a new node built
     * out of custom code.
     * @param n
     * @param results
     * @param dag
     */
    @Override
    public void replaceNodeResults(
            SDNode n,
            ArrayList<SDValue> results,
            SelectionDAG dag)
    {
        switch (n.getOpcode())
        {
            default:
                Util.shouldNotReachHere("Don't know how to custom type legalize this operation!");
                return;
            case ISD.FP_TO_SINT:
            {
                Pair<SDValue, SDValue> vals = FPToIntHelper(new SDValue(n, 0), dag, true);
                SDValue fist = vals.first, stackSlot = vals.second;
                if (fist.getNode() != null)
                {
                    EVT vt = n.getValueType(0);
                    results.add(dag.getLoad(vt, fist, stackSlot, null, 0));
                }
                return;
            }
            case ISD.READCYCLECOUNTER:
            {
                SDVTList vts = dag.getVTList(new EVT(MVT.Other), new EVT(MVT.Flag));
                SDValue chain = n.getOperand(0);
                SDValue rd = dag.getNode(X86ISD.RDTSC_DAG, vts, chain);
                SDValue eax = dag.getCopyFromReg(rd, X86GenRegisterNames.EAX,
                        new EVT(MVT.i32), rd.getValue(1));
                SDValue edx = dag.getCopyFromReg(eax.getValue(1),
                        X86GenRegisterNames.EDX,
                        new EVT(MVT.i32), eax.getValue(2));
                SDValue[] ops = {eax, edx};
                results.add(dag.getNode(ISD.BUILD_PAIR, new EVT(MVT.i64), ops));
                results.add(edx.getValue(1));
                return;
            }
            case ISD.ATOMIC_CMP_SWAP:
            {
                EVT vt = n.getValueType(0);
                assert vt.getSimpleVT().simpleVT == MVT.i64:
                        "Only know how to expand i64 cmp and swap!";
                SDValue cpInL, cpInH;
                EVT i32 = new EVT(MVT.i32);
                cpInL = dag.getNode(ISD.EXTRACT_ELEMENT, i32, n.getOperand(2),
                        dag.getConstant(0, i32, false));
                cpInH = dag.getNode(ISD.EXTRACT_ELEMENT, i32, n.getOperand(2),
                        dag.getConstant(1, i32, false));
                cpInL = dag.getCopyToReg(n.getOperand(0),
                        X86GenRegisterNames.EAX, cpInL, new SDValue());
                cpInH = dag.getCopyToReg(cpInL.getValue(0), X86GenRegisterNames.EDX,
                        cpInH, cpInL.getValue(1));

                SDValue swapInL, swapInH;
                swapInL = dag.getNode(ISD.EXTRACT_ELEMENT, i32, n.getOperand(3),
                        dag.getConstant(0, i32, false));
                swapInH = dag.getNode(ISD.EXTRACT_ELEMENT, i32, n.getOperand(3),
                        dag.getConstant(1, i32, false));
                swapInL = dag.getCopyToReg(cpInH.getValue(0), X86GenRegisterNames.EBX,
                        swapInL, cpInH.getValue(1));
                swapInH = dag.getCopyToReg(swapInL.getValue(0), X86GenRegisterNames.ECX,
                        swapInH, swapInL.getValue(1));
                SDValue[] ops = {swapInH.getValue(0), n.getOperand(1), swapInH.getValue(1)};
                SDVTList vts = dag.getVTList(new EVT(MVT.Other), new EVT(MVT.Flag));
                SDValue result = dag.getNode(X86ISD.LCMPXCHG8_DAG, vts, ops);
                SDValue cpOutL = dag.getCopyFromReg(result.getValue(0),
                        X86GenRegisterNames.EAX, i32, result.getValue(1));

                SDValue cpOutH = dag.getCopyFromReg(cpOutL.getValue(1),
                        X86GenRegisterNames.EDX, i32, cpOutL.getValue(2));
                SDValue[] ops2 = {cpOutL.getValue(0), cpOutH.getValue(0)};
                results.add(dag.getNode(ISD.BUILD_PAIR, new EVT(MVT.i64), ops2));
                results.add(cpOutH.getValue(1));
                return;
            }
            case ISD.ATOMIC_LOAD_ADD:
                replaceAtomicBinary64(n, results, dag, X86ISD.ATOMADD64_DAG);
                return;
            case ISD.ATOMIC_LOAD_AND:
                replaceAtomicBinary64(n, results, dag, X86ISD.ATOMAND64_DAG);
                return;
            case ISD.ATOMIC_LOAD_NAND:
                replaceAtomicBinary64(n, results, dag, X86ISD.ATOMNAND64_DAG);
                return;
            case ISD.ATOMIC_LOAD_OR:
                replaceAtomicBinary64(n, results, dag, X86ISD.ATOMOR64_DAG);
                return;
            case ISD.ATOMIC_LOAD_SUB:
                replaceAtomicBinary64(n, results, dag, X86ISD.ATOMSUB64_DAG);
                return;
            case ISD.ATOMIC_LOAD_XOR:
                replaceAtomicBinary64(n, results, dag, X86ISD.ATOMXOR64_DAG);
                return;
            case ISD.ATOMIC_SWAP:
                replaceAtomicBinary64(n, results, dag, X86ISD.ATOMSWAP64_DAG);
                return;
        }
    }

    private void replaceAtomicBinary64(SDNode n,
            ArrayList<SDValue> results,
            SelectionDAG dag, int newOpc)
    {
        EVT vt = n.getValueType(0);
        assert vt.getSimpleVT().simpleVT == MVT.i64:
                "Only know how to expand i64 atomics!";

        SDValue chain = n.getOperand(0);
        SDValue in1 = n.getOperand(1);
        EVT i32 = new EVT(MVT.i32);
        SDValue in2L = dag.getNode(ISD.EXTRACT_ELEMENT, i32,
                n.getOperand(2), dag.getIntPtrConstant(0));
        SDValue in2H = dag.getNode(ISD.EXTRACT_ELEMENT, i32,
                n.getOperand(2), dag.getIntPtrConstant(1));
        SDValue lsi = dag.getMemOperand(((MemSDNode)n).getMemOperand());
        SDValue[] ops = {chain, in1, in2L, in2H, lsi};
        SDVTList vts = dag.getVTList(i32, i32, new EVT(MVT.Other));
        SDValue result = dag.getNode(newOpc, vts, ops);
        SDValue[] ops2 = {result.getValue(0), result.getValue(1)};
        results.add(dag.getNode(ISD.BUILD_PAIR, new EVT(MVT.i64), ops2));
        results.add(result.getValue(2));
    }

    @Override
    public SDValue performDAGCombine(SDNode n, DAGCombinerInfo combineInfo)
    {
        SelectionDAG dag = combineInfo.dag;
        switch (n.getOpcode())
        {
            default: return new SDValue();
            case ISD.VECTOR_SHUFFLE: return performShuffleCombine(n, dag);
            case ISD.SELECT: return performSELECTCombine(n, dag);
            case X86ISD.CMOV: return performCMOVCombine(n, dag);
            case ISD.MUL:            return performMulCombine(n, dag, combineInfo);
            case ISD.SHL:
            case ISD.SRA:
            case ISD.SRL:            return performShiftCombine(n, dag);
            case ISD.STORE:          return performSTORECombine(n, dag);
            case X86ISD.FXOR:
            case X86ISD.FOR:         return performFORCombine(n, dag);
            case X86ISD.FAND:        return performFANDCombine(n, dag);
            case X86ISD.BT:          return performBTCombine(n, dag, combineInfo);
            case X86ISD.VZEXT_MOVL:  return performVZEXT_MOVLCombine(n, dag);
            case ISD.MEMBARRIER:     return performMEMBARRIERCombine(n, dag);
        }
    }

    private SDValue performMEMBARRIERCombine(SDNode n, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }

    private SDValue performVZEXT_MOVLCombine(SDNode n, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }

    private SDValue performBTCombine(SDNode n, SelectionDAG dag,
            DAGCombinerInfo combineInfo)
    {
        // TODO
        return new SDValue();
    }

    private SDValue performFANDCombine(SDNode n, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }

    private SDValue performFORCombine(SDNode n, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }

    private SDValue performSTORECombine(SDNode n, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }

    /**
     * Transform vector shift nodes to use vector shifts when it possible.
     * @param n
     * @param dag
     * @return
     */
    private SDValue performShiftCombine(SDNode n, SelectionDAG dag)
    {
        if (!subtarget.hasSSE2())
            return new SDValue();

        EVT vt = n.getValueType(0);
        if (vt.getSimpleVT().simpleVT != MVT.v2i64 &&
                vt.getSimpleVT().simpleVT != MVT.v4i32 &&
                vt.getSimpleVT().simpleVT != MVT.v8i16)
            return new SDValue();

        SDValue shAmtOP = n.getOperand(0);
        EVT eltVT = vt.getVectorElementType();
        SDValue baseShAmt = new SDValue();
        if (shAmtOP.getOpcode() == ISD.BUILD_VECTOR)
        {
            int numElts = vt.getVectorNumElements();
            int i = 0;
            for (; i < numElts; i++)
            {
                SDValue arg = shAmtOP.getOperand(i);
                if (arg.getOpcode() == ISD.UNDEF) continue;
                baseShAmt = arg;
                break;
            }

            for (; i < numElts; i++)
            {
                SDValue arg = shAmtOP.getOperand(i);
                if (arg.getOpcode() == ISD.UNDEF) continue;
                if (!arg.equals(baseShAmt))
                    return new SDValue();
            }
        }
        else if (shAmtOP.getOpcode() == ISD.VECTOR_SHUFFLE &&
                ((ShuffleVectorSDNode)shAmtOP.getNode()).isSplat())
        {
            baseShAmt = dag.getNode(ISD.EXTRACT_VECTOR_ELT, eltVT, shAmtOP,
                    dag.getIntPtrConstant(0));
        }
        else
            return new SDValue();

        if (eltVT.bitsGT(new EVT(MVT.i32)))
            baseShAmt = dag.getNode(ISD.TRUNCATE, new EVT(MVT.i32), baseShAmt);
        else if (eltVT.bitsLT(new EVT(MVT.i32)))
            baseShAmt = dag.getNode(ISD.ANY_EXTEND, new EVT(MVT.i32), baseShAmt);

        SDValue valOp = n.getOperand(0);
        int intVT = vt.getSimpleVT().simpleVT;
        switch (n.getOpcode())
        {
            default:
                Util.shouldNotReachHere("Unknown shift opcode!");
                break;
            case ISD.SHL:
            case ISD.SRA:
            case ISD.SRL:
                Util.shouldNotReachHere("Not implemented!");
                break;
        }
        return new SDValue();
    }

    private SDValue performMulCombine(SDNode n, SelectionDAG dag,
            DAGCombinerInfo combineInfo)
    {
        // TODO
        return new SDValue();
    }

    private SDValue performCMOVCombine(SDNode n, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }

    private SDValue performSELECTCombine(SDNode n, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }

    private SDValue performShuffleCombine(SDNode n, SelectionDAG dag)
    {
        // TODO
        return new SDValue();
    }

    @Override
    public EVT getOptimalMemOpType(long size, int align,
                                   boolean isSrcConst,
                                   boolean isSrcStr,
                                   SelectionDAG dag)
    {
        Function f = dag.getMachineFunction().getFunction();
        boolean noImplicitFloatOps = f.hasFnAttr(Attribute.NoImplicitFloat);
        if (!noImplicitFloatOps && subtarget.getStackAlignemnt() >= 16)
        {
            if ((isSrcConst || isSrcStr) && subtarget.hasSSE2() && size>=16)
                return new EVT(MVT.v4i32);
            if ((isSrcConst || isSrcStr) && subtarget.hasSSE1() && size>=16)
                return new EVT(MVT.v4f32);
        }
        if (subtarget.is64Bit() && size >= 8)
            return new EVT(MVT.i64);
        return new EVT(MVT.i32);
    }

    @Override
    public SDValue emitTargetCodeForMemcpy(SelectionDAG dag,
                                           SDValue chain,
                                           SDValue dst,
                                           SDValue src,
                                           SDValue size,
                                           int align,
                                           boolean alwaysInline,
                                           Value dstVal,
                                           long dstOff,
                                           Value srcVal,
                                           long srcOff)
    {
        if (!(size.getNode() instanceof ConstantSDNode))
            return new SDValue();
        ConstantSDNode sdn = (ConstantSDNode)size.getNode();
        long sizeVal = sdn.getZExtValue();
        if (!alwaysInline && sizeVal > subtarget.getMaxInlineSizeThreshold())
            return new SDValue();

        if ((align & 3) != 0)
            return new SDValue();
        EVT vt = new EVT(MVT.i32);
        if (subtarget.is64Bit() && (align & 0x7) == 0)
            vt = new EVT(MVT.i64);
        int ubytes = vt.getSizeInBits()/8;
        int countVal = (int) (sizeVal / ubytes);
        SDValue count = dag.getIntPtrConstant(countVal);
        int byteLeft = (int) (sizeVal%ubytes);
        SDValue inFlag = new SDValue();
        chain = dag.getCopyToReg(chain, subtarget.is64Bit()?
        X86GenRegisterNames.RCX : X86GenRegisterNames.ECX,
                count, inFlag);
        inFlag = chain.getValue(1);

        chain = dag.getCopyToReg(chain, subtarget.is64Bit()?
                        X86GenRegisterNames.RDI : X86GenRegisterNames.EDI,
                dst, inFlag);
        inFlag = chain.getValue(1);

        chain = dag.getCopyToReg(chain, subtarget.is64Bit()?
                        X86GenRegisterNames.RSI : X86GenRegisterNames.ESI,
                src, inFlag);
        inFlag = chain.getValue(1);

        SDVTList vts = dag.getVTList(new EVT(MVT.Other), new EVT(MVT.Flag));
        ArrayList<SDValue> ops = new ArrayList<>();
        ops.add(chain);
        ops.add(dag.getValueType(vt));
        ops.add(inFlag);
        SDValue repMovs = dag.getNode(X86ISD.REP_MOVS, vts, ops);

        ArrayList<SDValue> results = new ArrayList<>();
        results.add(repMovs);
        if (byteLeft != 0)
        {
            int offset = (int) (sizeVal - byteLeft);
            EVT dstVT = dst.getValueType();
            EVT srcVT = src.getValueType();
            EVT sizeVT = size.getValueType();
            results.add(dag.getMemcpy(chain, dag.getNode(ISD.ADD, dstVT, dst,
                    dag.getConstant(offset, dstVT, false)),
                    dag.getNode(ISD.ADD, srcVT, src, dag.getConstant(offset, srcVT, false)),
                    dag.getConstant(byteLeft, sizeVT, false), align,
                    alwaysInline, dstVal, dstOff + offset,
                    srcVal, srcOff + offset));
        }
        return dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), results);
    }

    @Override
    public boolean isGAPlusOffset(SDNode n,
                                  OutParamWrapper<GlobalValue> gv,
                                  OutParamWrapper<Long> offset)
    {
        if (n.getOpcode() == X86ISD.Wrapper)
        {
            if (n.getOperand(0).getNode() instanceof GlobalAddressSDNode)
            {
                GlobalAddressSDNode gad = (GlobalAddressSDNode)n.getOperand(0).getNode();
                gv.set(gad.getGlobalValue());
                offset.set(gad.getOffset());
                return true;
            }
        }
        return super.isGAPlusOffset(n, gv, offset);
    }

    @Override
    public boolean isVectorClearMaskLegal(TIntArrayList indices, EVT vt)
    {
        int numElts = vt.getVectorNumElements();
        if (numElts == 2) return true;
        if (numElts == 4 && vt.getSizeInBits() == 128)
        {
            return isMOVLMask(indices, vt) ||
                    isCommutedMOVLMask(indices, vt, true) ||
                    isSHUFPMask(indices,vt) ||
                    isCommutedSHUFPMask(indices, vt);
        }
        return false;
    }
}
