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
import backend.codegen.dagisel.SDValue;
import backend.codegen.dagisel.SelectionDAG;
import backend.codegen.fastISel.ISD;
import backend.support.Attribute;
import backend.support.CallingConv;
import backend.target.*;
import backend.target.x86.X86MachineFunctionInfo.NameDecorationStyle;
import backend.value.Function;
import tools.APInt;
import tools.Util;

import java.util.ArrayList;

import static backend.target.TargetOptions.EnablePerformTailCallOpt;
import static backend.target.x86.X86GenCallingConv.*;

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
        addRegisterClass(new MVT(MVT.i8), X86GenRegisterInfo.GR8RegisterClass);
        addRegisterClass(new MVT(MVT.i16), X86GenRegisterInfo.GR16RegisterClass);
        addRegisterClass(new MVT(MVT.i32), X86GenRegisterInfo.GR32RegisterClass);
        if (subtarget.is64Bit)
            addRegisterClass(new MVT(MVT.i64), X86GenRegisterInfo.GR64RegisterClass);

        if (x86ScalarSSEf64)
        {
            // f32 and f64 use SSE.
            // Set up the FP register class.
            addRegisterClass(new MVT(MVT.f32), X86GenRegisterInfo.FR32RegisterClass);
            addRegisterClass(new MVT(MVT.f64), X86GenRegisterInfo.FR64RegisterClass);
        }
        else if (x86ScalarSSEf32)
        {
            // f32 use SSE, but f64 use x87.
            // Set up the FP register class.
            addRegisterClass(new MVT(MVT.f32), X86GenRegisterInfo.FR32RegisterClass);
            addRegisterClass(new MVT(MVT.f64), X86GenRegisterInfo.RFP64RegisterClass);
        }
        else
        {
            // f32 and f64 in x87.
            // Set up the FP register classes.
            addRegisterClass(new MVT(MVT.f32), X86GenRegisterInfo.RFP32RegisterClass);
            addRegisterClass(new MVT(MVT.f64), X86GenRegisterInfo.RFP64RegisterClass);
        }

        // Long double always uses X87.
        addRegisterClass(new MVT(MVT.f80), X86GenRegisterInfo.RFP80RegisterClass);

        if (subtarget.hasMMX())
        {
            addRegisterClass(new MVT(MVT.v8i8), X86GenRegisterInfo.VR64RegisterClass);
            addRegisterClass(new MVT(MVT.v4i16), X86GenRegisterInfo.VR64RegisterClass);
            addRegisterClass(new MVT(MVT.v2i32), X86GenRegisterInfo.VR64RegisterClass);
            addRegisterClass(new MVT(MVT.v2f32), X86GenRegisterInfo.VR64RegisterClass);
            addRegisterClass(new MVT(MVT.v1i64), X86GenRegisterInfo.VR64RegisterClass);
        }
        if (subtarget.hasSSE1())
        {
            addRegisterClass(new MVT(MVT.v4f32), X86GenRegisterInfo.VR128RegisterClass);
        }
        if (subtarget.hasSSE2())
        {
            addRegisterClass(new MVT(MVT.v2f64), X86GenRegisterInfo.VR128RegisterClass);

            addRegisterClass(new MVT(MVT.v16i8), X86GenRegisterInfo.VR128RegisterClass);
            addRegisterClass(new MVT(MVT.v8i16), X86GenRegisterInfo.VR128RegisterClass);
            addRegisterClass(new MVT(MVT.v4i32), X86GenRegisterInfo.VR128RegisterClass);
            addRegisterClass(new MVT(MVT.v2i64), X86GenRegisterInfo.VR128RegisterClass);
        }
        if (subtarget.hasAVX())
        {
            addRegisterClass(new MVT(MVT.v8f32), X86GenRegisterInfo.VR256RegisterClass);
            addRegisterClass(new MVT(MVT.v4f64), X86GenRegisterInfo.VR256RegisterClass);
            addRegisterClass(new MVT(MVT.v8i32), X86GenRegisterInfo.VR256RegisterClass);
            addRegisterClass(new MVT(MVT.v4i64), X86GenRegisterInfo.VR256RegisterClass);
        }

        computeRegisterProperties();
    }

    @Override
    public X86MachineFunctionInfo createMachineFunctionInfo(MachineFunction mf)
    {
        return new X86MachineFunctionInfo(mf);
    }

    @Override public int getFunctionAlignment(Function fn)
    {
        return fn.hasFnAttr(Attribute.OptimizeForSize) ? 0 : 4;
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
                    // Handle MMX valeus passed in XMM regs.
                    if (regVT.isVector())
                    {
                        argValue = dag.getNode(ISD.EXTRACT_VECTOR_ELT, new EVT(MVT.i64),
                                argValue,
                                dag.getConstant(0, new EVT(MVT.i64), false));
                        argValue = dag.getNode(ISD.BIT_CONVERT, va.getValVT(),
                                argValue);
                    }
                    else
                        argValue = dag
                                .getNode(ISD.TRUNCATE, va.getValVT(), argValue);
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
                reg = mf.getMachineRegisterInfo().createVirtualRegister(getRegClassFor(new EVT(MVT.i64)));
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
}
