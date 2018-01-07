/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
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

package backend.target.x86;

import backend.codegen.*;
import backend.codegen.fastISel.CallLowering;
import backend.codegen.fastISel.FastISel;
import backend.codegen.fastISel.ISD;
import backend.support.CallingConv;
import backend.support.ErrorHandling;
import backend.target.*;
import backend.value.ConstantInt;
import backend.value.ConstantPointerNull;
import backend.value.Function;
import backend.value.Value;
import tools.OutParamWrapper;

import java.util.ArrayList;
import java.util.List;

import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.codegen.MachineOperand.RegState.Define;
import static backend.codegen.MachineOperand.RegState.Implicit;
import static backend.support.CallingConv.Fast;
import static backend.support.CallingConv.X86_FastCall;
import static backend.target.x86.X86AddressMode.BaseType.FrameIndexBase;
import static backend.target.x86.X86GenCallingConv.*;
import static backend.target.x86.X86GenInstrNames.*;
import static backend.target.x86.X86GenRegisterNames.*;
import static backend.target.x86.X86InstrBuilder.addFullAddress;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86CallLowering extends CallLowering
{
    private X86Subtarget subtarget;
    /**
     * Register used as the stack pointer.
     */
    protected int stackPtr;

    protected boolean x86ScalarSSEf64;
    protected boolean x86ScalarSSEf32;

    public X86CallLowering(X86TargetLowering tli)
    {
        super(tli);
        subtarget = (X86Subtarget) tli.getTargetMachine().getSubtarget();
        stackPtr = subtarget.is64Bit() ? RSP : ESP;
        x86ScalarSSEf32 = subtarget.hasSSE1();
        x86ScalarSSEf64 = subtarget.hasSSE2();
    }

    public void setIsel(X86FastISel isel)
    {
        super.setIsel(isel);
    }

    @Override
    public X86FastISel getIsel()
    {
        return (X86FastISel) super.getIsel();
    }

    private CCAssignFn CCAssignFnForCall(CallingConv CC)
    {
        return CCAssignFnForCall(CC, false);
    }

    /**
     * Selects the correct CCAssignFn for a given calling
     * convention.
     * @param CC
     * @param isTailCall
     * @return
     */
    private CCAssignFn CCAssignFnForCall(CallingConv CC, boolean isTailCall)
    {
        if (subtarget.is64Bit())
        {
            if (subtarget.isTargetWin64())
                return CC_X86_Win64_C;
            else
                return CC_X86_64_C;
        }

        if (CC == X86_FastCall)
            return CC_X86_32_FastCall;
        else if(CC == Fast)
            return CC_X86_32_FastCC;
        else
            return CC_X86_32_C;
    }

    static class OutgoingValueHandler extends ValueHandler
    {
        private int stackPtr;

        public OutgoingValueHandler(
                X86FastISel isel,
                MachineBasicBlock mbb,
                CCAssignFn assignFn)
        {
            super(isel, mbb, assignFn);
            stackPtr = isel.stackPtr;
        }

        public X86Subtarget getSubtarget()
        {
            return (X86Subtarget)subtarget;
        }

        @Override
        public X86FastISel getISel()
        {
            return (X86FastISel) super.getISel();
        }

        @Override
        public void assignValueToReg(ArgInfo argInfo, CCValAssign ca)
        {
            TargetRegisterClass locRC = tli.getRegClassFor(ca.getLocVT());
            TargetRegisterClass valRC = tli.getRegClassFor(ca.getValVT());
            boolean emitted = tii.copyRegToReg(mbb, mbb.size(),
                    ca.getLocReg(), argInfo.reg, locRC, valRC);
            assert emitted : "Failed to emit a copy instruction!";
        }

        @Override
        public void assignValueToStackAddress(
                ArgInfo argInfo,
                int locMemOffset,
                CCValAssign ca)
        {
            int reg = argInfo.reg;
            Value argVal = argInfo.val;
            X86AddressMode am = new X86AddressMode();
            am.base = new X86AddressMode.FrameIndexBase(stackPtr);
            am.disp = locMemOffset;

            // If this is a really simple value, emit this with the Value* version of
            // X86FastEmitStore.  If it isn't simple, we don't want to do this, as it
            // can cause us to reevaluate the argument.
            if (argVal instanceof ConstantInt
                    || argVal instanceof ConstantPointerNull)
                getISel().x86FastEmitStore(ca.getValVT(), argVal, am);
            else
                getISel().x86FastEmitStore(ca.getValVT(), reg, am);
        }

        @Override
        public int assignCustomValue(ArgInfo argInfo,
                List<CCValAssign> ccValAssigns)
        {
            return 0;
        }
    }

    private boolean handleAssignments(ArrayList<ArgInfo> args, ValueHandler handler)
    {
        MachineFunction mf = handler.getMBB().getParent();
        Function f = mf.getFunction();
        TargetMachine tm = mf.getTarget();

        ArrayList<CCValAssign> argLocs = new ArrayList<>();
        CCState ccInfo = new CCState(f.getCallingConv(), f.isVarArg(), tm, argLocs);
        int numArgs = args.size();
        for (int i = 0; i < numArgs; i++)
        {
            EVT vt = getTLI().getValueType(args.get(i).ty);
            if (handler.assignArg(i, vt, vt, CCValAssign.LocInfo.Full, args.get(i), ccInfo))
                return true;
        }

        handler.setStackSize(ccInfo.getNextStackOffset());
        for (int i = 0, j = 0, e = argLocs.size(); i < numArgs; i++, j++)
        {
            assert j < e:"Skipped too many arguments";
            CCValAssign ca = argLocs.get(j);
            assert ca.getValNo() == i:"Argument number doesn't match!";
            if (ca.needsCustom())
            {
                j+= handler.assignCustomValue(args.get(i), argLocs.subList(0, j));
            }
            if (ca.isRegLoc())
            {
                handler.assignValueToReg(args.get(i), ca);
            }
            else if (ca.isMemLoc())
            {
                handler.assignValueToStackAddress(args.get(i), ca.getLocMemOffset(), ca);
            }
            else
            {
                assert false:"Unknown argument location!";
                return true;
            }
        }
        return false;
    }

    abstract static class IncomingValueHandler extends ValueHandler
    {
        private MachineFrameInfo mfi;
        public IncomingValueHandler(
                FastISel isel,
                MachineBasicBlock mbb,
                CCAssignFn assignFn)
        {
            super(isel, mbb, assignFn);
            mfi = mbb.getParent().getFrameInfo();
        }

        @Override
        public X86FastISel getISel()
        {
            return (X86FastISel) super.getISel();
        }

        @Override
        public X86Subtarget getSubtarget()
        {
            return (X86Subtarget) super.getSubtarget();
        }

        @Override
        public void assignValueToReg(ArgInfo argInfo, CCValAssign ca)
        {
            int valReg = argInfo.reg;
            int locReg = ca.getLocReg();
            markPhysicalRegUsed(valReg);
            boolean emitted;
            int opc = 0;
            switch (ca.getLocInfo())
            {
                default:
                    break;
                case SExt:
                    opc = ISD.SIGN_EXTEND;
                    break;
                case ZExt:
                    opc = ISD.ZERO_EXTEND;
                    break;
                case AExt:
                    opc = ISD.ANY_EXTEND;
                    break;
            }

            if (opc != 0)
            {
                OutParamWrapper<Integer> xx = new OutParamWrapper<Integer>(
                        valReg);
                emitted = getISel()
                        .X86FastEmitExtend(opc, ca.getValVT(), locReg, ca.getLocVT(), xx);
                locReg = xx.get();
                if (ca.getLocInfo() == CCValAssign.LocInfo.AExt)
                {
                    if (!emitted)
                        emitted = getISel().X86FastEmitExtend(ISD.ZERO_EXTEND,
                                ca.getValVT(), locReg, ca.getLocVT(), xx);
                    if (!emitted)
                        emitted = getISel().X86FastEmitExtend(ISD.SIGN_EXTEND,
                                ca.getValVT(), locReg, ca.getLocVT(), xx);

                    locReg = xx.get();
                    assert emitted : "Failed to emit a aext!";
                }
            }
            tii.copyRegToReg(mbb, mbb.size(), valReg, locReg,
                    tli.getRegClassFor(ca.getValVT()),
                    tli.getRegClassFor(ca.getLocVT()));
            isel.updateValueMap(argInfo.val, valReg);
        }

        @Override
        public void assignValueToStackAddress(
                ArgInfo argInfo,
                int locMemOffset,
                CCValAssign ca)
        {
            assert ca.isMemLoc();
            int reg = argInfo.reg;
            ArgFlagsTy flags = argInfo.flags;
            boolean isImmutable = !flags.isByVal();
            EVT valVT = ca.getLocInfo() == CCValAssign.LocInfo.Indirect ?
                    ca.getLocVT() : ca.getValVT();

            int fi = mfi.createFixedObject(valVT.getSizeInBits()/8,
                    ca.getLocMemOffset(), isImmutable);
            int opc = 0;
            int locVT = ca.getLocVT().getSimpleVT().simpleVT;
            X86Subtarget subtarget = getSubtarget();
            switch (valVT.getSimpleVT().simpleVT)
            {
                case MVT.i8:
                    opc = MOV8rm;
                    break;
                case MVT.i16:
                    switch (ca.getLocInfo())
                    {
                        case SExt:
                            opc = MOVSX16rm8;
                            break;
                        case ZExt:
                        case AExt:
                            opc = MOVZX16rm8;
                            break;
                        default:
                            opc = MOV8rm;
                            break;
                    }
                    break;
                case MVT.i32:
                    switch (ca.getLocInfo())
                    {
                        case SExt:
                            if (locVT == MVT.i8)
                                opc = MOVSX32rm8;
                            else if (locVT == MVT.i16)
                                opc = MOVSX32rm16;
                        case ZExt:
                        case AExt:
                            if (locVT == MVT.i8)
                                opc = MOVZX32rm8;
                            else if (locVT == MVT.i16)
                                opc = MOVZX32rm16;
                            break;
                        default:
                            opc = MOV32rm;
                            break;
                    }
                    break;
                case MVT.i64:
                    switch (ca.getLocInfo())
                    {
                        case SExt:
                            if (locVT == MVT.i8)
                                opc = MOVSX64rm8;
                            else if (locVT == MVT.i16)
                                opc = MOVSX64rm16;
                            else if (locVT == MVT.i32)
                                opc = MOVSX64rm32;
                        case ZExt:
                        case AExt:
                            if (locVT == MVT.i8)
                                opc = MOVZX64rm8;
                            else if (locVT == MVT.i16)
                                opc = MOVZX64rm16;
                            else if (locVT == MVT.i32)
                                opc = MOVZX64rm32;
                            break;
                        default:
                            opc = MOV64rm;
                            break;
                    }
                    break;
                case MVT.f32:
                    if (!subtarget.hasSSE1())
                    {
                        opc = LD_Fp32m;
                    }
                    else
                    {
                        opc = MOVSSrm;
                    }
                    break;
                case MVT.f64:
                    if (!subtarget.hasSSE2())
                    {
                        if (locVT == MVT.f32)
                        {
                            opc = LD_Fp32m64;
                        }
                        else
                        {
                            assert locVT == MVT.f64;
                            opc = LD_Fp64m;
                        }
                    }
                    else
                    {
                        if (locVT == MVT.f32)
                        {
                            opc = CVTSS2SDrm;
                        }
                        else
                        {
                            assert locVT == MVT.f64;
                            opc = MOVSDrm;
                        }
                    }
                    break;
                case MVT.f80:
                    switch (locVT)
                    {
                        case MVT.f32:
                            opc = LD_Fp32m80;
                            break;
                        case MVT.f64:
                            opc = LD_Fp64m80;
                            break;
                        case MVT.f80:
                            opc = LD_Fp80m;
                            break;
                        default:
                            assert false:"Invalid value type";
                            break;
                    }
                    break;
                case MVT.v4i32:
                case MVT.v1i64:
                case MVT.v2i64:
                case MVT.v4f32:
                case MVT.v2f64:
                default:
                    assert false:"Unsupported value type (e.g. vector type)";
                    return;
            }

            X86AddressMode am = new X86AddressMode();
            am.baseType = FrameIndexBase;
            am.base = new X86AddressMode.FrameIndexBase(fi);
            addFullAddress(buildMI(mbb, tii.get(opc), reg), am);

            isel.updateValueMap(argInfo.val, reg);
        }

        @Override
        public int assignCustomValue(ArgInfo argInfo,
                List<CCValAssign> ccValAssigns)
        {
            return 0;
        }

        public abstract void markPhysicalRegUsed(int preg);
    }

    static class CallReturnHandler extends IncomingValueHandler
    {
        public CallReturnHandler(FastISel isel,
                MachineBasicBlock mbb,
                CCAssignFn assignFn)
        {
            super(isel, mbb, assignFn);
        }

        @Override
        public void markPhysicalRegUsed(int preg)
        {
            //mbb.getLastInst().addOperand(MachineOperand.createReg(preg, true, true));
        }
    }

    static class FormalArgHandler extends IncomingValueHandler
    {
        public FormalArgHandler(FastISel isel, MachineBasicBlock mbb,
                CCAssignFn assignFn)
        {
            super(isel, mbb, assignFn);
        }

        @Override
        public void markPhysicalRegUsed(int preg)
        {
            mbb.addLiveIn(preg);
        }
    }

    /**
     * This hook must be implemented to lower the given call instruction, formal
     * argument and return value.
     * @param mbb The machine basic block where emit calling code.
     * @param cc   The calling convention.
     * @param callee    The destination of the call, It should be either a register,
     *                  global address or a external symbol.
     * @param origRet   The Return value information.
     * @param orignArgs The formal arguments information.
     * @return
     */
    @Override
    public boolean lowerCall(
            MachineBasicBlock mbb,
            CallingConv cc,
            MachineOperand callee,
            ArgInfo origRet,
            ArrayList<ArgInfo> orignArgs)
    {
        MachineFunction mf = mbb.getParent();
        TargetMachine tm = mf.getTarget();
        if (!subtarget.isTargetELF() || cc != CallingConv.C)
        {
            ErrorHandling.llvmReportError("FastIsel not support non C calling conv!");
            return false;
        }

        // Issue CALLSEQ_START
        TargetInstrInfo tii = tm.getInstrInfo();
        TargetRegisterInfo tri = tm.getRegisterInfo();
        int adjStackDown = tri.getCallFrameSetupOpcode();
        MachineInstr setupFrame = buildMI(mbb, tii.get(adjStackDown)).getMInstr();

        // Analyze operands of the call, assigning locations to each operand.
        OutgoingValueHandler handler = new OutgoingValueHandler(getIsel(), mbb, CC_X86_32_C);
        if (handleAssignments(orignArgs, handler))
            return true;

        if (handler.getStackSize() <= 0)
            setupFrame.removeFromParent();
        else
            setupFrame.addOperand(MachineOperand.createImm(handler.getStackSize()));

        boolean is64Bit = subtarget.is64Bit();
        int callOpc = callee.isRegister() ? (is64Bit ? CALL64r : CALL32r)
                :(is64Bit ? CALL64pcrel32 : CALLpcrel32);

        buildMI(mbb, tii.get(callOpc)).addOperand(callee);

        boolean isFixed = orignArgs.isEmpty() || orignArgs
                .get(orignArgs.size() - 1).isFixed;
        if (subtarget.is64Bit() && !isFixed && !subtarget.isCallingConvWin64(cc))
        {
            // TODO: 17-12-27
            buildMI(mbb, tii.get(MOV8ri)).addReg(AL, Define |
                    Implicit).addImm(0);
        }

        // Finally we can copy the returned value back into its virtual-register. In
        // symmetry with the arguments, the physical register must be an
        // implicit-define of the call instruction.
        if (origRet.reg != 0)
        {
            CallReturnHandler retHandler = new CallReturnHandler(getIsel(), mbb, RetCC_X86);
            ArrayList<ArgInfo> rets = new ArrayList<>();
            rets.add(origRet);
            if (handleAssignments(rets, retHandler))
                return true;
        }

        //setupFrame.addOperand(MachineOperand.createImm(handler.getStackSize()));

        // Issue CALLSEQ_END
        if (handler.getStackSize() > 0)
        {
            int adjStackUp = tri.getCallFrameDestroyOpcode();
            buildMI(mbb, tii.get(adjStackUp)).addImm(handler.getStackSize()).addImm(0);
        }
        return false;
    }
}
