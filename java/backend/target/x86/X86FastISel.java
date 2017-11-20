package backend.target.x86;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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
import backend.codegen.selectDAG.FastISel;
import backend.codegen.selectDAG.ISD;
import backend.support.CallSite;
import backend.support.CallingConv;
import backend.support.LLVMContext;
import backend.target.TargetData;
import backend.target.TargetInstrInfo;
import backend.target.TargetMachine;
import backend.target.TargetRegisterClass;
import backend.type.*;
import backend.value.*;
import backend.value.Instruction.AllocaInst;
import backend.value.Instruction.BranchInst;
import backend.value.Instruction.CallInst;
import backend.value.Instruction.CmpInst;
import gnu.trove.list.array.TIntArrayList;
import tools.OutParamWrapper;
import tools.Util;

import java.util.ArrayList;

import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.support.CallingConv.Fast;
import static backend.support.CallingConv.X86_FastCall;
import static backend.target.TargetMachine.CodeModel.Small;
import static backend.target.TargetMachine.RelocModel.PIC_;
import static backend.target.TargetOptions.EnablePerformTailCallOpt;
import static backend.target.x86.X86AddressMode.BaseType.FrameIndexBase;
import static backend.target.x86.X86GenCallingConv.*;
import static backend.target.x86.X86GenInstrNames.*;
import static backend.target.x86.X86GenRegisterInfo.*;
import static backend.target.x86.X86GenRegisterNames.*;
import static backend.target.x86.X86II.*;
import static backend.target.x86.X86InstrInfo.isGlobalRelativeToPICBase;
import static backend.target.x86.X86InstrInfo.isGlobalStubReference;
import static backend.target.x86.X86RegisterInfo.SUBREG_8BIT;
import static tools.Util.isInt32;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86FastISel extends FastISel
{
    protected X86Subtarget subtarget;
    /**
     * Register used as the stack pointer.
     */
    protected int stackPtr;

    protected boolean x86ScalarSSEf64;
    protected boolean x86ScalarSSEf32;

    public X86FastISel(TargetMachine tm, TargetMachine.CodeGenOpt level)
    {
        super(tm, level);
        subtarget = (X86Subtarget) tm.getSubtarget();
        stackPtr = subtarget.is64Bit() ? RSP : ESP;
        x86ScalarSSEf32 = subtarget.hasSSE1();
        x86ScalarSSEf64 = subtarget.hasSSE2();
    }

    @Override
    public String getPassName()
    {
        return "X86 Fast Instruction Selector";
    }

    public static X86FastISel createX86FastISel(TargetMachine tm, TargetMachine.CodeGenOpt level)
    {
        return new X86GenFastISel(tm, level);
    }

    @Override
    public boolean targetSelectInstruction(Instruction inst)
    {
        switch (inst.getOpcode())
        {
            case Load:
                return x86SelectLoad(inst);
            case Store:
                return x86SelectStore(inst);
            case ICmp:
            case FCmp:
                return x86SelectCmp(inst);
            case ZExt:
                return x86SelectZExt(inst);
            case Br:
                return x86SelectBranch(inst);
            case Call:
                return x86SelectCall(inst);
            case LShr:
            case AShr:
            case Shl:
                return x86SelectShift(inst);
            case Trunc:
                return x86SelectTrunc(inst);
            case FPExt:
                return x86SelectFPExt(inst);
            case FPTrunc:
                return x86SelectFPTrunc(inst);
            case IntToPtr: // Deliberate fall-through.
            case PtrToInt:
            {
                EVT srcVT = tli.getValueType(inst.operand(0).getType());
                EVT dstVT = tli.getValueType(inst.getType());
                if (dstVT.bitsGT(srcVT))
                    return x86SelectZExt(inst);
                if (dstVT.bitsLT(srcVT))
                    return x86SelectTrunc(inst);
                int Reg = getRegForValue(inst.operand(0));
                if (Reg == 0) return false;
                updateValueMap(inst, Reg);
                return true;
            }
            case GetElementPtr:
                return x86SelectGetElementPtr(inst);
            case Ret:
                return x86SelectRet(inst);
            case Switch:
                assert false:"Switch should already be handled by LowerSwitch pass!";
                break;
            case Unreachable:
                assert false:"Unreachable instruction should be removed!";
                break;
        }
        return false;
    }

    private boolean x86SelectGetElementPtr(User u)
    {
        int baseAddr = getRegForValue(u.operand(0));
        if (baseAddr == 0)
            // Unhandled operand. Halt "Fast" instruction selection.
            return false;

        Type ty = u.operand(0).getType();
        MVT vt = tli.getPointerTy();
        for (int i = 1; i < u.getNumOfOperands(); i++)
        {
            Value idx = u.operand(i);
            if (ty instanceof StructType)
            {
                StructType sty = (StructType)ty;
                long field = ((ConstantInt)idx).getZExtValue();
                if (field != 0)
                {
                    // baseAddr = baseAddr + offset.
                    long offset = td.getStructLayout(sty).getElementOffset(field);

                    baseAddr = fastEmit_ri_(vt, ISD.ADD, baseAddr, offset, vt);
                    if (baseAddr == 0)
                        return false;
                }

                ty = sty.getElementType((int) field);
            }
            else
            {
                assert ty instanceof SequentialType;
                ty = ((SequentialType)ty).getElementType();

                // If this is a constant subscript, handle it quickly.
                if (idx instanceof ConstantInt)
                {
                    ConstantInt ci = (ConstantInt)idx;
                    if (ci.getZExtValue() == 0)
                        continue;
                    long off = td.getTypeAllocSize(ty) * ((ConstantInt)ci).getSExtValue();

                    // baseAddr = baseAddr + sizeOfElt * idx.
                    baseAddr = fastEmit_ri_(vt, ISD.ADD, baseAddr, off, vt);
                    if (baseAddr == 0)
                        // Failure.
                        return false;
                    continue;
                }

                // baseAddr = baseAddr + sizeOfElt * idx.
                long eltSize = td.getTypeAllocSize(ty);
                int idxN = getRegForGEPIndex(idx);
                if (idxN == 0)
                    return false;

                if (eltSize != 1)
                {
                    // If eltSize != 0, emit multiple instructon.
                    idxN = fastEmit_ri_(vt, ISD.MUL, idxN, eltSize, vt);
                    if (idxN == 0)
                        return false;
                }
                baseAddr = fastEmit_rr(vt, vt, ISD.ADD, baseAddr, idxN);
                if (baseAddr == 0)
                    return false;
            }
        }

        // We successfully emitted code for the given LLVM instruction.
        updateValueMap(u, baseAddr);
        return true;
    }

    private boolean x86SelectRet(Instruction inst)
    {
        // Now handle call return value (if any).
        Type retTy = fn.getReturnType();
        EVT retVT;
        OutParamWrapper<EVT> x = new OutParamWrapper<>();
        if (retTy.equals(LLVMContext.VoidTy))
        {
            retVT = new EVT(MVT.isVoid);
        }
        else
        {
            if (!isTypeLegal(retTy, x, true))
                return false;
            retVT = x.get();
        }

        CallingConv cc = fn.getCallingConv();
        if (cc != CallingConv.C && cc != CallingConv.Fast
                && cc != CallingConv.X86_FastCall)
            return false;

        // On X86, -tailcallopt changes the fastcc ABI. FastISel doesn't
        // handle this for now.
        if (cc == CallingConv.Fast && EnablePerformTailCallOpt.value)
            return false;

        if (retVT.getSimpleVT().simpleVT != MVT.isVoid)
        {
            ArrayList<CCValAssign> rvLocs = new ArrayList<>();
            CCState ccInfo = new CCState(cc, false, tm, rvLocs);
            ccInfo.analyzeCallResult(retVT, RetCC_X86);

            // Copy all of the result registers out of their specified physreg.
            assert rvLocs.size() == 1 : "Can't handle multi-value calls!";
            int srcReg = getRegForValue(inst);
            int opc;
            boolean isSigned = retTy.isSigned();
            int destReg = rvLocs.get(0).getLocReg();
            switch (retVT.getSimpleVT().simpleVT)
            {
                case MVT.i8:
                    opc = isSigned ? MOVSX32rr8:MOVZX32rr8;
                    buildMI(mbb, instrInfo.get(opc), destReg).addReg(srcReg);
                    buildMI(mbb, instrInfo.get(RET)).addReg(destReg);
                    break;
                case MVT.i16:
                    opc = isSigned ? MOVSX32rr16:MOVZX32rr16;
                    buildMI(mbb, instrInfo.get(opc), destReg).addReg(srcReg);
                    buildMI(mbb, instrInfo.get(RET)).addReg(destReg);
                    break;
                case MVT.i32:
                    opc = MOV32rr;
                    buildMI(mbb, instrInfo.get(opc), destReg).addReg(srcReg);
                    buildMI(mbb, instrInfo.get(RET)).addReg(destReg);
                    break;
                case MVT.f32:
                    if (subtarget.hasSSE1())
                    {
                        opc = MOVSSrr;
                        buildMI(mbb, instrInfo.get(opc), destReg).addReg(srcReg);
                    }
                    else
                    {
                        opc = FsFLD0SS;
                        buildMI(mbb, instrInfo.get(opc)).addReg(srcReg);
                    }
                    break;
                case MVT.f64:
                    if (subtarget.hasSSE2())
                    {
                        opc = MOVSDrr;
                        buildMI(mbb, instrInfo.get(opc), destReg).addReg(srcReg);
                    }
                    else
                    {
                        opc = FsFLD0SD;
                        buildMI(mbb, instrInfo.get(opc)).addReg(srcReg);
                    }
                    break;
                case MVT.i64:
                default:
                    assert false:"Unsupported integer type(beyond 64 bit)";
                    return false;
            }
        }
        else
            buildMI(mbb, instrInfo.get(RET));
        return true;
    }

    /**
     * If we have a comparison with RHS as the RHS
     * of the comparison, return an opcode that works for the compare (e.g.
     * CMP32ri) otherwise return 0.
     * @param vt
     * @param rhs
     * @return
     */
    private static int x86ChooseCmpImmediateOpcode(EVT vt, ConstantInt rhs)
    {
        switch (vt.getSimpleVT().simpleVT)
        {
            case MVT.i8: return CMP8ri;
            case MVT.i16: return CMP16ri;
            case MVT.i32: return CMP32ri;
            case MVT.i64:
            {
                // 64-bit comparisons are only valid if the immediate fits in a 32-bit sext
                // field.
                if (Util.isInt32(rhs.getSExtValue()))
                    return CMP64ri32;
                return 0;
            }
            default: return 0;
        }
    }

    private static int x86ChooseCmpOpcode(EVT vt)
    {
        switch (vt.getSimpleVT().simpleVT)
        {
            case MVT.i8: return CMP8rr;
            case MVT.i16: return CMP16rr;
            case MVT.i32: return CMP32rr;
            case MVT.i64: return CMP64rr;
            case MVT.f32: return UCOMISSrr;
            case MVT.f64: return UCOMISDrr;
            default: return 0;
        }
    }

    private boolean x86FastEmitCompare(Value lhs, Value rhs, EVT vt)
    {
        int op0Reg = getRegForValue(lhs);
        if (op0Reg == 0)
            return false;

        // Handle 'null' like i32/i64 0.
        if (rhs instanceof ConstantPointerNull)
        {
            rhs = backend.value.Constant.getNullValue(td.getIntPtrType());
        }

        if (rhs instanceof ConstantInt)
        {
            ConstantInt ciOp1 = (ConstantInt)rhs;
            int compareImmOpc = x86ChooseCmpImmediateOpcode(vt, ciOp1);
            if (compareImmOpc != 0)
            {
                buildMI(mbb, instrInfo.get(compareImmOpc)).
                        addReg(op0Reg).
                        addImm(ciOp1.getSExtValue());
                return true;
            }
        }

        int compareOpc = x86ChooseCmpOpcode(vt);
        if (compareOpc == 0)
            return false;

        int op1Reg = getRegForValue(rhs);
        if (op1Reg == 0)
            return false;

        buildMI(mbb, instrInfo.get(compareOpc)).addReg(op0Reg).addReg(op1Reg);
        return true;
    }

    /**
     * Emit a machine instruction to load a value of type VT.
     * The address is either pre-computed, i.e. Ptr, or a GlobalAddress, i.e. GV.
     * Return true and the result register by reference if it is possible.
     * @param vt
     * @param am
     * @param resultReg
     * @return
     */
    private boolean x86FastEmitLoad(EVT vt,
            X86AddressMode am,
            OutParamWrapper<Integer> resultReg)
    {
        int opc = 0;
        TargetRegisterClass rc = null;
        switch (vt.getSimpleVT().simpleVT)
        {
            default: return false;
            case MVT.i8:
                opc = MOV8rm;
                rc = GR8RegisterClass;
                break;
            case MVT.i16:
                opc = MOV16rm;
                rc = GR16RegisterClass;
                break;
            case MVT.i32:
                opc = MOV32rm;
                rc = GR32RegisterClass;
                break;
            case MVT.i64:
                opc = MOV64rm;
                rc = GR64RegisterClass;
                break;
            case MVT.f32:
                if (subtarget.hasSSE1())
                {
                    opc = MOVSSrm;
                    rc = FR32RegisterClass;
                }
                else
                {
                    opc = LD_Fp32m;
                    rc = RFP32RegisterClass;
                }
                break;
            case MVT.f64:
                if (subtarget.hasSSE2())
                {
                    opc = MOVSDrm;
                    rc = FR64RegisterClass;
                }
                else
                {
                    opc = LD_Fp64m;
                    rc = RFP64RegisterClass;
                }
                break;
            case MVT.f80:
                // NO f80 supported yet.
                return false;
        }
        resultReg.set(createResultReg(rc));
        X86InstrBuilder.addFullAddress(buildMI(mbb, instrInfo.get(opc),
                resultReg.get()), am);
        return true;
    }

    private boolean x86FastEmitStore(EVT vt, Value val, X86AddressMode am)
    {
        if (val instanceof ConstantPointerNull)
        {
            val = backend.value.Constant.getNullValue(td.getIntPtrType());
        }

        if (val instanceof ConstantInt)
        {
            ConstantInt ci = (ConstantInt)val;
            int opc = 0;
            switch (vt.getSimpleVT().simpleVT)
            {
                default:break;
                case MVT.i8:
                    opc = MOV8mi;
                    break;
                case MVT.i16:
                    opc = MOV16mi;
                    break;
                case MVT.i32:
                    opc = MOV32mi;
                    break;
                case MVT.i64:
                    // Must be a 32-bit sign extended value.
                    if (((int)ci.getSExtValue()) == ci.getSExtValue())
                        opc = MOV64mi32;
                    break;
            }
            if (opc != 0)
            {
                X86InstrBuilder
                        .addFullAddress(buildMI(mbb, instrInfo.get(opc)), am).addImm(ci.getSExtValue());
                return true;
            }
        }

        int valReg = getRegForValue(val);
        if (valReg == 0)
            return false;

        return x86FastEmitStore(vt, valReg, am);
    }

    private boolean x86FastEmitStore(EVT vt, int val,
                         X86AddressMode am)
    {
        int opc = 0;
        switch (vt.getSimpleVT().simpleVT)
        {
            case MVT.f80:   // unsupported as yet.
            default: return false;
            case MVT.i8:
                opc = MOV8mr;
                break;
            case MVT.i16:
                opc = MOV16mr;
                break;
            case MVT.i32:
                opc = MOV32mr;
                break;
            case MVT.i64:
                opc = MOV64mr;
                break;
            case MVT.f32:
                opc = subtarget.hasSSE1() ? MOVSSmr : ST_Fp32m;
                break;
            case MVT.f64:
                opc = subtarget.hasSSE2() ? MOVSDmr : ST_FP64m;
                break;
        }
        X86InstrBuilder.addFullAddress(buildMI(mbb, instrInfo.get(opc)), am).addReg(val);
        return true;
    }

    /**
     * Emit a machine instruction to extend a value src of
     * type srcVT to type dstVT using the specified extension opcode Opc (e.g.
     * ISD::SIGN_EXTEND).
     * @param opc
     * @param dstVT
     * @param src
     * @param srcVT
     * @param resultReg
     * @return
     */
    boolean X86FastEmitExtend(int opc,
            EVT dstVT,
            int src,
            EVT srcVT,
            OutParamWrapper<Integer> resultReg)
    {
        int rr = fastEmit_r(srcVT.getSimpleVT(), dstVT.getSimpleVT(), opc, src);
        if (rr != 0)
        {
            resultReg.set(rr);
            return true;
        }
        else
            return false;
    }

    /**
     * Attempt to fill in an address from the given value.
     * @param val
     * @param am
     * @return
     */
    private boolean x86SelectAddress(Value val, X86AddressMode am)
    {
        User u = null;
        Operator opcode = null;
        if (val instanceof Instruction)
        {
            u = (Instruction)val;
            opcode = ((Instruction) val).getOpcode();
        }
        else if (val instanceof ConstantExpr)
        {
            ConstantExpr ce = (ConstantExpr)val;
            u = ce;
            opcode = ce.getOpcode();
        }

    Unsupported:
        if (u != null)
        {
            switch (opcode)
            {
                case BitCast:
                    return x86SelectAddress(u.operand(0), am);

                case IntToPtr:
                    if (tli.getValueType(u.operand(0).getType()).
                            equals(new EVT(tli.getPointerTy())))
                        return x86SelectAddress(u.operand(0), am);
                    break;
                case PtrToInt:
                    if (tli.getValueType(u.getType()).
                            equals(new EVT(tli.getPointerTy())))
                        return x86SelectAddress(u.operand(0), am);
                    break;
                case Alloca:
                {
                    AllocaInst ai = (AllocaInst)u;
                    if (staticAllocMap.containsKey(ai))
                    {
                        am.baseType = FrameIndexBase;
                        am.base = new X86AddressMode.FrameIndexBase(staticAllocMap.get(ai));
                        return true;
                    }
                    break;
                }
                case Add:
                {
                    if (u.operand(1) instanceof ConstantInt)
                    {
                        ConstantInt ci = (ConstantInt)u.operand(1);
                        long disp = am.disp + ci.getSExtValue();
                        if (isInt32(disp))
                        {
                            am.disp = (int)disp;
                            return x86SelectAddress(u.operand(0), am);
                        }
                    }
                    break;
                }
                case GetElementPtr:
                {
                    long disp = am.disp;
                    int indexReg = am.indexReg;
                    int scale = am.scale;

                    Type baseType = u.operand(0).getType();
                    for (int i = 1, e = u.getNumOfOperands(); i != e; i++)
                    {
                        Value op = u.operand(i);
                        if (baseType instanceof StructType)
                        {
                            StructType sty = (StructType)baseType;
                            TargetData.StructLayout layout = td.getStructLayout(sty);
                            long idx = ((ConstantInt)op).getZExtValue();
                            disp += layout.getElementOffset(idx);
                        }
                        else
                        {
                            long s = td.getTypeAllocSize(baseType);
                            if (op instanceof ConstantInt)
                            {
                                disp += ((ConstantInt)op).getSExtValue();
                            }
                            else if (indexReg == 0  && (am.gv == null ||
                                    !subtarget.isPICStyleRIPRel()) &&
                                    (s == 1 || s == 2 || s == 4 ||s == 8))
                            {
                                scale = (int) s;
                                indexReg = getRegForGEPIndex(op);
                                if (indexReg == 0)
                                    return false;
                            }
                            else
                                // Unsupported.
                                break Unsupported;
                        }
                    }
                    if (!isInt32(disp))
                        break;
                    am.indexReg = indexReg;
                    am.scale = scale;
                    am.disp = (int) disp;
                    return x86SelectAddress(u.operand(0), am);
                }
            }
        }

        if (val instanceof GlobalValue)
        {
            GlobalValue gv = (GlobalValue)val;

            // Can't handle alternate code models yet.
            if (tm.getCodeModel() != Small)
                return false;

            if (subtarget.isPICStyleRIPRel() && (am.base.getBase() != 0 ||
                        am.indexReg != 0))
                return false;

            if (gv instanceof GlobalVariable)
            {
                GlobalVariable gvar = (GlobalVariable)gv;
                if (gvar.isThreadLocal())
                    return false;
            }

            am.gv = gv;
            // Allow the subtarget to classify the global.
            int gvFlags = subtarget.classifyGlobalReference(gv, tm);

            if (isGlobalRelativeToPICBase(gvFlags))
            {
                am.base.setBase(getInstrInfo().getGlobalBaseReg(mf));
            }

            if (!isGlobalStubReference(gvFlags))
            {
                if (subtarget.isPICStyleRIPRel())
                {
                    assert am.base.getBase() == 0 && am.indexReg == 0;
                    am.base.setBase(RIP);
                }
                am.gvOpFlags = gvFlags;
                return true;
            }

            // Ok, we need to do a load from a stub.  If we've already loaded from this
            // stub, reuse the loaded pointer, otherwise emit the load now.
            int loadReg;
            if (localValueMap.containsKey(val))
            {
                loadReg = localValueMap.get(val);
            }
            else
            {
                // Issue load from stub.
                int opc = 0;
                TargetRegisterClass rc = null;
                X86AddressMode stubAM = new X86AddressMode();
                stubAM.base.setBase(am.base.getBase());
                stubAM.gv = gv;
                stubAM.gvOpFlags = gvFlags;

                if (tli.getPointerTy().equals(new MVT(MVT.i64)))
                {
                    opc = MOV64rm;
                    rc = GR64RegisterClass;

                    if (subtarget.isPICStyleRIPRel())
                        stubAM.base.setBase(RIP);
                }
                else
                {
                    opc = MOV32rm;
                    rc = GR32RegisterClass;
                }

                loadReg = createResultReg(rc);
                X86InstrBuilder
                        .addFullAddress(buildMI(mbb, instrInfo.get(opc), loadReg), stubAM);

                // Prevent loading GV stub multiple times in same mbb.
                localValueMap.put(val, loadReg);
            }

            am.base.setBase(loadReg);
            am.gv = null;
            return true;
        }

        if (am.gv == null || !subtarget.isPICStyleRIPRel())
        {
            if (am.base.getBase() == 0)
            {
                am.base.setBase(getRegForValue(val));
                return am.base.getBase() != 0;
            }
            if (am.indexReg == 0)
            {
                assert am.scale == 1 :"Scale with no index!";
                am.indexReg = getRegForValue(val);
                return am.indexReg != 0;
            }
        }

        return false;
    }

    private boolean x86SelectLoad(Instruction inst)
    {
        EVT vt;
        OutParamWrapper<EVT> x = new OutParamWrapper<>();
        if (!isTypeLegal(inst.getType(), x))
            return false;

        vt = x.get();
        X86AddressMode am = new X86AddressMode();
        if (!x86SelectAddress(inst.operand(0), am))
            return false;

        int resultReg = 0;
        OutParamWrapper<Integer> xi = new OutParamWrapper<>(0);
        if (x86FastEmitLoad(vt, am, xi))
        {
            resultReg = xi.get();
            updateValueMap(inst, resultReg);
            return true;
        }
        return false;
    }

    private boolean x86SelectStore(Instruction inst)
    {
        EVT vt;
        OutParamWrapper<EVT> x = new OutParamWrapper<>();
        if (!isTypeLegal(inst.operand(0).getType(), x))
            return false;

        vt = x.get();
        X86AddressMode am = new X86AddressMode();
        if (!x86SelectAddress(inst.operand(1), am))
            return false;

        return x86FastEmitStore(vt, inst.operand(0), am);
    }

    private boolean x86SelectCmp(Instruction inst)
    {
        CmpInst ci = (CmpInst)inst;

        EVT vt;
        OutParamWrapper<EVT> x = new OutParamWrapper<>();
        if (!isTypeLegal(inst.operand(0).getType(), x))
            return false;

        vt = x.get();
        int resultReg = createResultReg(GR8RegisterClass);
        int setCCOpc;
        boolean swapArgs;       // false-> compare op0, op1,  true -> compare op1, op0.
        switch (ci.getPredicate())
        {
            case FCMP_OEQ:
            {
                if (!x86FastEmitCompare(ci.operand(0), ci.operand(1), vt))
                    return false;

                int ereg = createResultReg(GR8RegisterClass);
                int npreg = createResultReg(GR8RegisterClass);
                buildMI(mbb, instrInfo.get(SETEr), ereg);
                buildMI(mbb, instrInfo.get(SETNPr), npreg);
                buildMI(mbb, instrInfo.get(AND8rr), resultReg).addReg(npreg).addReg(ereg);
                updateValueMap(inst, resultReg);
                return true;
            }
            case FCMP_UNE:
            {
                if (!x86FastEmitCompare(ci.operand(0), ci.operand(1), vt))
                    return false;

                int ereg = createResultReg(GR8RegisterClass);
                int npreg = createResultReg(GR8RegisterClass);
                buildMI(mbb, instrInfo.get(SETNEr), ereg);
                buildMI(mbb, instrInfo.get(SETPr), npreg);
                buildMI(mbb, instrInfo.get(OR8rr), resultReg).addReg(npreg).addReg(ereg);
                updateValueMap(inst, resultReg);
                return true;
            }
            case FCMP_OGT: swapArgs = false; setCCOpc = SETAr; break;
            case FCMP_OGE: swapArgs = false; setCCOpc = SETAEr; break;
            case FCMP_OLT: swapArgs = true;  setCCOpc = SETAr;  break;
            case FCMP_OLE: swapArgs = true;  setCCOpc = SETAEr; break;
            case FCMP_ONE: swapArgs = false; setCCOpc = SETNEr; break;
            case FCMP_ORD: swapArgs = false; setCCOpc = SETNPr; break;
            case FCMP_UNO: swapArgs = false; setCCOpc = SETPr;  break;
            case FCMP_UEQ: swapArgs = false; setCCOpc = SETEr;  break;
            case FCMP_UGT: swapArgs = true;  setCCOpc = SETBr;  break;
            case FCMP_UGE: swapArgs = true;  setCCOpc = SETBEr; break;
            case FCMP_ULT: swapArgs = false; setCCOpc = SETBr;  break;
            case FCMP_ULE: swapArgs = false; setCCOpc = SETBEr; break;

            case ICMP_EQ:  swapArgs = false; setCCOpc = SETEr;  break;
            case ICMP_NE:  swapArgs = false; setCCOpc = SETNEr; break;
            case ICMP_UGT: swapArgs = false; setCCOpc = SETAr;  break;
            case ICMP_UGE: swapArgs = false; setCCOpc = SETAEr; break;
            case ICMP_ULT: swapArgs = false; setCCOpc = SETBr;  break;
            case ICMP_ULE: swapArgs = false; setCCOpc = SETBEr; break;
            case ICMP_SGT: swapArgs = false; setCCOpc = SETGr;  break;
            case ICMP_SGE: swapArgs = false; setCCOpc = SETGEr; break;
            case ICMP_SLT: swapArgs = false; setCCOpc = SETLr;  break;
            case ICMP_SLE: swapArgs = false; setCCOpc = SETLEr; break;
            default: return false;
        }

        Value op0 = inst.operand(0);
        Value op1 = inst.operand(1);
        if (swapArgs)
        {
            Value tmp = op0;
            op0 = op1;
            op1 = tmp;
        }

        // Emit a compare of op0, op1.
        if (!x86FastEmitCompare(op0, op1, vt))
            return false;

        buildMI(mbb, instrInfo.get(setCCOpc), resultReg);
        updateValueMap(inst, resultReg);
        return true;
    }

    private boolean x86SelectZExt(Instruction inst)
    {
        // Handle zero-extension from i1 to i8, which is common.
        if (inst.getType().equals(LLVMContext.Int8Ty) &&
                inst.operand(0).getType().equals(LLVMContext.Int1Ty))
        {
            int resultReg = getRegForValue(inst.operand(0));
            if (resultReg == 0)
                return false;

            resultReg = fastEmitZExtFromI1(new MVT(MVT.i8), resultReg);
            if (resultReg == 0)
                return false;

            updateValueMap(inst, resultReg);
            return true;
        }
        return false;
    }

    private boolean x86SelectBranch(Instruction inst)
    {
        BranchInst bi = (BranchInst)inst;
        MachineBasicBlock trueBB = mbbMap.get(bi.getSuccessor(0));
        MachineBasicBlock falseBB = mbbMap.get(bi.getSuccessor(1));

        // Fold the common case of a conditional branch with a comparison.
        if (bi.getCondition() instanceof CmpInst)
        {
            CmpInst ci = (CmpInst)bi.getCondition();
            if (ci.hasOneUses())
            {
                EVT vt = tli.getValueType(ci.operand(0).getType());
                CmpInst.Predicate pred = ci.getPredicate();

                // Try to take advantage of fallthrough opportunities.
                if (mbb.isLayoutSuccessor(trueBB))
                {
                    MachineBasicBlock temp = trueBB;
                    trueBB = falseBB;
                    falseBB = temp;
                }

                boolean swapArgs = false;
                int branchOpc;

                switch (pred)
                {
                    case FCMP_OEQ:
                    {
                        MachineBasicBlock temp = trueBB;
                        trueBB = falseBB;
                        falseBB = temp;
                        pred = CmpInst.Predicate.FCMP_UNE;
                        // fall through
                    }
                    case FCMP_UNE: swapArgs = false; branchOpc = JNE; break;
                    case FCMP_OGT: swapArgs = false; branchOpc = JA;  break;
                    case FCMP_OGE: swapArgs = false; branchOpc = JAE; break;
                    case FCMP_OLT: swapArgs = true;  branchOpc = JA;  break;
                    case FCMP_OLE: swapArgs = true;  branchOpc = JAE; break;
                    case FCMP_ONE: swapArgs = false; branchOpc = JNE; break;
                    case FCMP_ORD: swapArgs = false; branchOpc = JNP; break;
                    case FCMP_UNO: swapArgs = false; branchOpc = JP;  break;
                    case FCMP_UEQ: swapArgs = false; branchOpc = JE;  break;
                    case FCMP_UGT: swapArgs = true;  branchOpc = JB;  break;
                    case FCMP_UGE: swapArgs = true;  branchOpc = JBE; break;
                    case FCMP_ULT: swapArgs = false; branchOpc = JB;  break;
                    case FCMP_ULE: swapArgs = false; branchOpc = JBE; break;

                    case ICMP_EQ:  swapArgs = false; branchOpc = JE;  break;
                    case ICMP_NE:  swapArgs = false; branchOpc = JNE; break;
                    case ICMP_UGT: swapArgs = false; branchOpc = JA;  break;
                    case ICMP_UGE: swapArgs = false; branchOpc = JAE; break;
                    case ICMP_ULT: swapArgs = false; branchOpc = JB;  break;
                    case ICMP_ULE: swapArgs = false; branchOpc = JBE; break;
                    case ICMP_SGT: swapArgs = false; branchOpc = JG;  break;
                    case ICMP_SGE: swapArgs = false; branchOpc = JGE; break;
                    case ICMP_SLT: swapArgs = false; branchOpc = JL;  break;
                    case ICMP_SLE: swapArgs = false; branchOpc = JLE; break;
                    default:
                        return false;
                }

                Value op0 = ci.operand(0);
                Value op1 = ci.operand(1);
                if (swapArgs)
                {
                    Value temp = op0;
                    op0 = op1;
                    op1 = temp;
                }

                // Emit a compare of the LHS and RHS, setting the flags.
                if (!x86FastEmitCompare(op0, op1, vt))
                    return false;

                buildMI(mbb, instrInfo.get(branchOpc)).addMBB(trueBB).
                        addMBB(falseBB);

                if (pred == CmpInst.Predicate.FCMP_UNE)
                {
                    // X86 requires a second branch to handle UNE (and OEQ,
                    // which is mapped to UNE above).
                    buildMI(mbb, instrInfo.get(JP)).addMBB(trueBB);
                }

                fastEmitBranch(falseBB);
                mbb.addSuccessor(trueBB);
                return true;
            }
        }

        int opReg = getRegForValue(bi.getCondition());
        if (opReg == 0)
            return false;

        buildMI(mbb, instrInfo.get(TEST8rr)).addReg(opReg).addReg(opReg);
        buildMI(mbb, instrInfo.get(JNE)).addMBB(trueBB);
        fastEmitBranch(falseBB);
        mbb.addSuccessor(trueBB);
        return true;
    }

    private boolean x86SelectShift(Instruction inst)
    {
        int creg = 0, opReg = 0, opImm = 0;
        TargetRegisterClass rc = null;
        if (inst.getType().equals(LLVMContext.Int8Ty))
        {
            creg = CL;
            rc = GR8RegisterClass;
            switch (inst.getOpcode()) 
            {
                case LShr: opReg = SHR8rCL; opImm = SHR8ri; break;
                case AShr: opReg = SAR8rCL; opImm = SAR8ri; break;
                case Shl:  opReg = SHL8rCL; opImm = SHL8ri; break;
                default: return false;
            }
        } 
        else if (inst.getType().equals(LLVMContext.Int16Ty))
        {
            creg = CX;
            rc = GR16RegisterClass;
            switch (inst.getOpcode()) 
            {
                case LShr: opReg = SHR16rCL; opImm = SHR16ri; break;
                case AShr: opReg = SAR16rCL; opImm = SAR16ri; break;
                case Shl:  opReg = SHL16rCL; opImm = SHL16ri; break;
                default: return false;
            }
        }
        else if (inst.getType().equals(LLVMContext.Int32Ty))
        {
            creg = ECX;
            rc = GR32RegisterClass;
            switch (inst.getOpcode())
            {
                case LShr: opReg = SHR32rCL; opImm = SHR32ri; break;
                case AShr: opReg = SAR32rCL; opImm = SAR32ri; break;
                case Shl:  opReg = SHL32rCL; opImm = SHL32ri; break;
                default: return false;
            }
        }
        else if (inst.getType().equals(LLVMContext.Int64Ty))
        {
            creg = RCX;
            rc = GR64RegisterClass;
            switch (inst.getOpcode())
            {
                case LShr: opReg = SHR64rCL; opImm = SHR64ri; break;
                case AShr: opReg = SAR64rCL; opImm = SAR64ri; break;
                case Shl:  opReg = SHL64rCL; opImm = SHL64ri; break;
                default: return false;
            }
        }
        else
        {
            return false;
        }

        EVT vt = tli.getValueType(inst.getType(), true);
        OutParamWrapper<EVT> x = new OutParamWrapper<>(vt);
        if (vt.equals(new EVT(MVT.Other)) || !isTypeLegal(inst.getType(), x))
            return false;

        vt = x.get();

        int op0Reg = getRegForValue(inst.operand(0));
        if (op0Reg == 0) return false;

        if (inst.operand(1) instanceof ConstantInt)
        {
            ConstantInt ci = (ConstantInt)inst.operand(1);
            int resultReg = createResultReg(rc);
            buildMI(mbb, instrInfo.get(opImm), resultReg).addReg(op0Reg).
                    addImm(ci.getZExtValue() & 0xff);
            updateValueMap(inst, resultReg);
            return true;
        }

        int op1Reg = getRegForValue(inst.operand(1));
        if (op1Reg == 0)
            return false;

        // First move op1reg to CL register preparing for Shift with AL/AX/EAX/RAX.
        instrInfo.copyRegToReg(mbb, mbb.size(), creg, op1Reg, rc, rc);

        if (creg != CL)
        {
            buildMI(mbb, instrInfo.get(TargetInstrInfo.EXTRACT_SUBREG), CL).
                    addReg(creg).addImm(SUBREG_8BIT);
        }

        int resultReg = createResultReg(rc);
        buildMI(mbb, instrInfo.get(opReg), resultReg).addReg(op0Reg);
        updateValueMap(inst, resultReg);
        return true;
    }

    private boolean x86SelectSelect(Instruction inst)
    {
        EVT vt = tli.getValueType(inst.getType(), true);
        OutParamWrapper<EVT> x = new OutParamWrapper<>(vt);
        if (vt.equals(new EVT(MVT.Other)) || !isTypeLegal(inst.getType(), x))
            return false;

        vt = x.get();

        int opc = 0;
        TargetRegisterClass rc = null;
        switch (vt.getSimpleVT().simpleVT)
        {
            case MVT.i16:
                opc = CMOVE16rr;
                rc = GR16RegisterClass;
                break;
            case MVT.i32:
                opc = CMOVE32rr;
                rc = GR32RegisterClass;
                break;
            case MVT.i64:
                opc = CMOVE64rr;
                rc = GR64RegisterClass;
                break;
            default:
                return false;
        }
        int op0Reg = getRegForValue(inst.operand(0));
        if (op0Reg == 0)
            return false;

        int op1Reg = getRegForValue(inst.operand(1));
        if (op1Reg == 0)
            return false;

        int op2Reg = getRegForValue(inst.operand(2));
        if (op2Reg == 0)
            return false;

        buildMI(mbb, instrInfo.get(TEST8rr)).addReg(op0Reg).addReg(op0Reg);
        int resultReg = createResultReg(rc);
        buildMI(mbb, instrInfo.get(opc), resultReg).addReg(op1Reg).addReg(op2Reg);
        updateValueMap(inst, resultReg);
        return true;
    }

    private boolean x86SelectTrunc(Instruction inst)
    {
        if (subtarget.is64Bit())
            // All other cases should be handled by the tblgen generated code.
            return false;

        EVT srcVT = tli.getValueType(inst.operand(0).getType());
        EVT dstVT = tli.getValueType(inst.getType());

        // This code only handles truncation to byte right now.
        if (!dstVT.equals(new EVT(MVT.i8)) &&
                !dstVT.equals(new EVT(MVT.i1)))
            // All other cases should be handled by the tblgen generated code.
            return false;

        if (!srcVT.equals(new EVT(MVT.i16)) &&
                !srcVT.equals(new EVT(MVT.i32)))
            // All other cases should be handled by the tblgen generated code.
            return false;

        int op0Reg = getRegForValue(inst.operand(0));
        if (op0Reg == 0)
            return false;

        boolean srcIs16Bit = srcVT.equals(new EVT(MVT.i16));
        int copyOpc = srcIs16Bit ? MOV16rr : MOV32rr;
        TargetRegisterClass copyRC = srcIs16Bit ? GR16_ABCDRegisterClass :
                GR32_ABCDRegisterClass;
        int copyReg = createResultReg(copyRC);
        buildMI(mbb, instrInfo.get(copyOpc), copyReg).addReg(op0Reg);

        int resultReg = fastEmitInst_extractsubreg(new MVT(MVT.i8),
                copyReg, SUBREG_8BIT);

        if (resultReg == 0)
            return false;
        updateValueMap(inst, resultReg);
        return true;
    }

    private boolean x86SelectFPExt(Instruction inst)
    {
        // fpext from float to double.
        if (subtarget.hasSSE2() &&
                inst.getType().equals(LLVMContext.DoubleTy))
        {
            Value v = inst.operand(0);
            if (v.getType().equals(LLVMContext.FloatTy))
            {
                int opReg = getRegForValue(v);
                if (opReg == 0)
                    return false;

                int resultReg = createResultReg(FR64RegisterClass);
                buildMI(mbb, instrInfo.get(CVTSS2SDrr), resultReg).addReg(opReg);
                updateValueMap(inst, resultReg);
                return true;
            }
        }
        return false;
    }

    private boolean x86SelectFPTrunc(Instruction inst)
    {
        // fptrunc from double to float.
        if (subtarget.hasSSE2() &&
                inst.getType().equals(LLVMContext.FloatTy))
        {
            Value v = inst.operand(0);
            if (v.getType().equals(LLVMContext.DoubleTy))
            {
                int opReg = getRegForValue(v);
                if (opReg == 0)
                    return false;

                int resultReg = createResultReg(FR32RegisterClass);
                buildMI(mbb, instrInfo.get(CVTSD2SSrr), resultReg).addReg(opReg);
                updateValueMap(inst, resultReg);
                return true;
            }
        }
        return false;
    }

    private boolean x86SelectExtractValue(Instruction inst)
    {
        Util.shouldNotReachHere("Should not reaching here");
        return false;
    }

    private boolean x86VisitIntrinsicCall(IntrinsicInst inst)
    {
        Util.shouldNotReachHere("Should not reaching here");
        return false;
    }

    private boolean x86SelectCallAddress(Value v, X86AddressMode am)
    {
        User u = null;
        Operator opcode = null;
        if (v instanceof Instruction)
        {
            Instruction i = (Instruction) v;
            opcode = i.getOpcode();
            u = i;
        }
        else if (v instanceof ConstantExpr)
        {
            ConstantExpr ce = (ConstantExpr) v;
            opcode = ce.getOpcode();
            u = ce;
        }

        switch (opcode)
        {
            case BitCast:
                return x86SelectCallAddress(u.operand(0), am);

            case IntToPtr:
                if (tli.getValueType(u.operand(0).getType()).equals(new EVT(tli.getPointerTy())))
                    return x86SelectCallAddress(u.operand(0), am);
                break;
            case PtrToInt:
                if (tli.getValueType(u.getType()).equals(new EVT(tli.getPointerTy())))
                    return x86SelectCallAddress(u.operand(0), am);
                break;
        }

        if (v instanceof GlobalValue)
        {
            GlobalValue gv = (GlobalValue)v;
            if (tm.getCodeModel() != Small)
                return false;

            if (subtarget.isPICStyleRIPRel() &&
                    (am.base.getBase() != 0 || am.indexReg != 0))
                return false;

            if (gv instanceof GlobalVariable)
            {
                GlobalVariable gvar = (GlobalVariable)gv;
                if (gvar.isThreadLocal())
                    return false;
            }

            am.gv = gv;

            if (subtarget.isPICStyleRIPRel())
            {
                assert am.base.getBase()== 0 && am.indexReg == 0;
                am.base.setBase(RIP);
            }
            else if (subtarget.isPICStyleStubPIC())
            {
                am.gvOpFlags = MO_PIC_BASE_OFFSET;
            }
            else if (subtarget.isPICStyleGOT())
            {
                am.gvOpFlags = MO_GOTOFF;
            }
            return true;
        }

        if (am.gv == null || !subtarget.isPICStyleRIPRel())
        {
            if (am.base.getBase() == 0)
            {
                am.base.setBase(getRegForValue(v));
                return am.base.getBase() != 0;
            }
            if (am.indexReg == 0)
            {
                assert am.scale == 1 :"Scale with no index!";
                am.indexReg = getRegForValue(v);
                return am.indexReg != 0;
            }
        }
        return false;
    }

    private boolean x86SelectCall(Instruction inst)
    {
        CallInst ci = (CallInst)inst;
        Value Callee = inst.operand(0);

        // Can't handle inline asm yet.

        // Handle intrinsic calls.
        if (ci instanceof IntrinsicInst)
            return x86VisitIntrinsicCall((IntrinsicInst) ci);

        // Handle only C and fastcc calling conventions for now.
        CallSite cs = new CallSite(ci);
        CallingConv cc = cs.getCallingConv();
        if (cc != CallingConv.C && cc != CallingConv.Fast
                && cc != CallingConv.X86_FastCall)
            return false;

        // On X86, -tailcallopt changes the fastcc ABI. FastISel doesn't
        // handle this for now.
        if (cc == CallingConv.Fast && EnablePerformTailCallOpt.value)
            return false;

        // Let SDISel handle vararg functions.
        PointerType PT = (PointerType) cs.getCalledValue().getType();
        FunctionType FTy = (FunctionType) PT.getElementType();
        if (FTy.isVarArg())
            return false;

        // Handle *simple* calls for now.
        Type retTy = cs.getType();
        EVT retVT;
        OutParamWrapper<EVT> x = new OutParamWrapper<>();
        if (retTy.equals(LLVMContext.VoidTy))
            retVT = new EVT(MVT.isVoid);
        else if (!isTypeLegal(retTy, x, true))
            return false;

        retVT = x.get();

        // Materialize callee address in a register. FIXME: GV address can be
        // handled with a CALLpcrel32 instead.
        X86AddressMode CalleeAM = new X86AddressMode();
        if (!x86SelectCallAddress(Callee, CalleeAM))
            return false;
        int CalleeOp = 0;
        GlobalValue GV = null;

        if (CalleeAM.gv != null)
        {
            GV = CalleeAM.gv;
        }
        else if (CalleeAM.base.getBase() != 0)
        {
            CalleeOp = CalleeAM.base.getBase();
        }
        else
            return false;

        // Allow calls which produce i1 results.
        boolean AndToI1 = false;
        if (retVT.equals(new EVT(MVT.i1)))
        {
            retVT = new EVT(MVT.i8);
            AndToI1 = true;
        }

        // Deal with call operands first.
        ArrayList<Value> argVals = new ArrayList<>();
        TIntArrayList args = new TIntArrayList();
        ArrayList<EVT> argVTs = new ArrayList<>();
        ArrayList<ArgFlagsTy> argFlags = new ArrayList<>();

        // Fill the number of arguments zero.
        for (int i = cs.getNumOfArguments(); i > 0; i--)
        {
            args.add(0);
            argVals.add(null);
            argVTs.add(null);
            argFlags.add(null);
        }

        for (int i = 0, e = cs.getNumOfArguments(); i < e; i++)
        {
            Value arg = cs.getArgument(i);
            int Arg = getRegForValue(arg);
            if (Arg == 0)
                return false;
            ArgFlagsTy Flags = new ArgFlagsTy();
            int AttrInd = i + 1;

            Type argTy = arg.getType();
            EVT argVT = new EVT();
            x = new OutParamWrapper<>(argVT);
            if (!isTypeLegal(argTy, x))
                return false;
            argVT = x.get();
            int OriginalAlignment = td.getABITypeAlignment(argTy);
            Flags.setOrigAlign(OriginalAlignment);

            args.add(Arg);
            argVals.add(arg);
            argVTs.add(argVT);
            argFlags.add(Flags);
        }

        // Analyze operands of the call, assigning locations to each operand.
        ArrayList<CCValAssign> ArgLocs = new ArrayList<>();

        CCState CCInfo = new CCState(cc, false, tm, ArgLocs);
        CCInfo.analyzeCallOperands(argVTs, argFlags, CCAssignFnForCall(cc));

        // Get a count of how many bytes are to be pushed on the stack.
        int NumBytes = CCInfo.getNextStackOffset();

        // Issue CALLSEQ_START
        int AdjStackDown = tm.getRegisterInfo().getCallFrameSetupOpcode();
        buildMI(mbb, instrInfo.get(AdjStackDown)).addImm(NumBytes);

        // Process argument: walk the register/memloc assignments, inserting
        // copies / loads.
        TIntArrayList RegArgs = new TIntArrayList();
        for (int i = 0, e = ArgLocs.size(); i != e; ++i)
        {
            CCValAssign VA = ArgLocs.get(i);
            int Arg = args.get(VA.getValNo());
            EVT ArgVT = argVTs.get(VA.getValNo());

            // Promote the value if needed.
            switch (VA.getLocInfo())
            {
                default:
                    Util.shouldNotReachHere("Undefined loc info!");

                case Full:
                    break;
                case SExt:
                {
                    OutParamWrapper<Integer> xx = new OutParamWrapper<Integer>(Arg);
                    boolean Emitted = X86FastEmitExtend(ISD.SIGN_EXTEND,
                            VA.getLocVT(), Arg, ArgVT, xx);
                    Arg = xx.get();
                    assert Emitted : "Failed to emit a sext!";
                    Emitted = true;
                    ArgVT = VA.getLocVT();
                    break;
                }
                case ZExt:
                {
                    OutParamWrapper<Integer> xx = new OutParamWrapper<Integer>(Arg);
                    boolean Emitted = X86FastEmitExtend(ISD.ZERO_EXTEND,
                            VA.getLocVT(), Arg, ArgVT, xx);
                    Arg = xx.get();
                    assert Emitted : "Failed to emit a zext!";
                    Emitted = true;
                    ArgVT = VA.getLocVT();
                    break;
                }
                case AExt:
                {
                    OutParamWrapper<Integer> xx = new OutParamWrapper<Integer>(Arg);
                    boolean Emitted = X86FastEmitExtend(ISD.ANY_EXTEND,
                            VA.getLocVT(), Arg, ArgVT, xx);
                    if (!Emitted)
                        Emitted = X86FastEmitExtend(ISD.ZERO_EXTEND, VA.getLocVT(),
                                Arg, ArgVT, xx);
                    if (!Emitted)
                        Emitted = X86FastEmitExtend(ISD.SIGN_EXTEND, VA.getLocVT(),
                                Arg, ArgVT, xx);

                    Arg = xx.get();
                    assert Emitted : "Failed to emit a aext!";

                    ArgVT = VA.getLocVT();
                    break;
                }
                case BCvt:
                {
                    int BC = fastEmit_r(ArgVT.getSimpleVT(),
                            VA.getLocVT().getSimpleVT(), ISD.BIT_CONVERT, Arg);
                    assert BC != 0 : "Failed to emit a bitcast!";
                    Arg = BC;
                    ArgVT = VA.getLocVT();
                    break;
                }
            }

            if (VA.isRegLoc())
            {
                TargetRegisterClass RC = tli.getRegClassFor(ArgVT);
                boolean Emitted = instrInfo.copyRegToReg(mbb, mbb.size(),
                        VA.getLocReg(), Arg, RC, RC);
                assert Emitted : "Failed to emit a copy instruction!";
                Emitted = true;
                RegArgs.add(VA.getLocReg());
            }
            else
            {
                int LocMemOffset = VA.getLocMemOffset();
                X86AddressMode AM = new X86AddressMode();
                AM.base.setBase(stackPtr);
                AM.disp = LocMemOffset;
                Value ArgVal = argVals.get(VA.getValNo());

                // If this is a really simple value, emit this with the Value* version of
                // X86FastEmitStore.  If it isn't simple, we don't want to do this, as it
                // can cause us to reevaluate the argument.
                if (ArgVal instanceof ConstantInt
                        || ArgVal instanceof ConstantPointerNull)
                    x86FastEmitStore(ArgVT, ArgVal, AM);
                else
                    x86FastEmitStore(ArgVT, Arg, AM);
            }
        }

        // ELF / PIC requires GOT in the EBX register before function calls via PLT
        // GOT pointer.
        if (subtarget.isPICStyleGOT())
        {
            TargetRegisterClass RC = GR32RegisterClass;
            int Base = getInstrInfo().getGlobalBaseReg(mf);
            boolean Emitted = instrInfo
                    .copyRegToReg(mbb, mbb.size(), EBX, Base, RC, RC);
            assert Emitted : "Failed to emit a copy instruction!";
            Emitted = true;
        }

        // Issue the call.
        MachineInstrBuilder MIB;
        if (CalleeOp == 0)
        {
            // Register-indirect call.
            int CallOpc = subtarget.is64Bit() ? CALL64r : CALL32r;
            MIB = buildMI(mbb, DL, instrInfo.get(CallOpc)).addReg(CalleeOp);

        }
        else
        {
            // Direct call.
            assert GV != null : "Not a direct call";
            int CallOpc = subtarget.is64Bit() ? CALL64pcrel32 : CALLpcrel32;

            // See if we need any target-specific flags on the GV operand.
            int OpFlags = 0;

            // On ELF targets, in both X86-64 and X86-32 mode, direct calls to
            // external symbols most go through the PLT in PIC mode.  If the symbol
            // has hidden or protected visibility, or if it is static or local, then
            // we don't need to use the PLT - we can directly call it.
            if (subtarget.isTargetELF() && tm.getRelocationModel() == PIC_ && GV
                    .hasDefaultVisibility() && !GV.hasLocalLinkage())
            {
                OpFlags = MO_PLT;
            }
            else if (subtarget.isPICStyleStubAny() && (GV.isDeclaration() || GV
                    .isWeakForLinker()) && subtarget.getDarwinVers() < 9)
            {
                // PC-relative references to external symbols should go through $stub,
                // unless we're building with the leopard linker or later, which
                // automatically synthesizes these stubs.
                OpFlags = MO_DARWIN_STUB;
            }

            MIB = buildMI(mbb, DL, instrInfo.get(CallOpc))
                    .addGlobalAddress(GV, 0, OpFlags);
        }

        // Add an implicit use GOT pointer in EBX.
        if (subtarget.isPICStyleGOT())
            MIB.addReg(EBX);

        // Add implicit physical register uses to the call.
        for (int i = 0, e = RegArgs.size(); i != e; ++i)
            MIB.addReg(RegArgs.get(i));

        // Issue CALLSEQ_END
        int AdjStackUp = tm.getRegisterInfo().getCallFrameDestroyOpcode();
        buildMI(mbb, DL, instrInfo.get(AdjStackUp)).addImm(NumBytes).addImm(0);

        // Now handle call return value (if any).
        if (retVT.getSimpleVT().simpleVT != MVT.isVoid)
        {
            ArrayList<CCValAssign> RVLocs = new ArrayList<>();
            CCInfo = new CCState(cc, false, tm, RVLocs);
            CCInfo.analyzeCallResult(retVT, RetCC_X86);

            // Copy all of the result registers out of their specified physreg.
            assert RVLocs.size() == 1 : "Can't handle multi-value calls!";
            EVT CopyVT = RVLocs.get(0).getValVT();
            TargetRegisterClass DstRC = tli.getRegClassFor(CopyVT);
            TargetRegisterClass SrcRC = DstRC;

            // If this is a call to a function that returns an fp value on the x87 fp
            // stack, but where we prefer to use the value in xmm registers, copy it
            // out as F80 and use a truncate to move it from fp stack reg to xmm reg.
            if ((RVLocs.get(0).getLocReg() == ST0
                    || RVLocs.get(0).getLocReg() == ST1)
                    && isScalarFPTypeInSSEReg(RVLocs.get(0).getValVT()))
            {
                CopyVT = new EVT(MVT.f80);
                SrcRC = RSTRegisterClass;
                DstRC = RFP80RegisterClass;
            }

            int ResultReg = createResultReg(DstRC);
            boolean Emitted = instrInfo.copyRegToReg(mbb, mbb.size(), ResultReg,
                    RVLocs.get(0).getLocReg(), DstRC, SrcRC);
            assert Emitted : "Failed to emit a copy instruction!";
            Emitted = true;
            if (CopyVT != RVLocs.get(0).getValVT())
            {
                // Round the F80 the right size, which also moves to the appropriate xmm
                // register. This is accomplished by storing the F80 value in memory and
                // then loading it back. Ewww...
                EVT ResVT = RVLocs.get(0).getValVT();
                int Opc = ResVT.equals(new EVT(MVT.f32)) ?
                        ST_Fp80m32 :
                        ST_Fp80m64;
                int MemSize = ResVT.getSizeInBits() / 8;
                int FI = mfi.createStackObject(MemSize, MemSize);
                MachineInstrBuilder
                        .addFrameReference(buildMI(mbb, DL, instrInfo.get(Opc)), FI)
                        .addReg(ResultReg);
                DstRC = ResVT.equals(new EVT(MVT.f32)) ?
                        FR32RegisterClass :
                        FR64RegisterClass;
                Opc = ResVT.equals(new EVT(MVT.f32)) ?
                        MOVSSrm :
                        MOVSDrm;
                ResultReg = createResultReg(DstRC);
                MachineInstrBuilder.addFrameReference(buildMI(mbb, DL, instrInfo
                                .get(Opc), ResultReg),
                        FI);
            }

            if (AndToI1)
            {
                // Mask out all but lowest bit for some call which produces an i1.
                int AndResult = createResultReg(GR8RegisterClass);
                buildMI(mbb, DL, instrInfo.get(AND8ri), AndResult).addReg(ResultReg)
                        .addImm(1);
                ResultReg = AndResult;
            }

            updateValueMap(inst, ResultReg);
        }

        return true;
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

    @Override
    protected boolean lowerArguments(BasicBlock llvmBB)
    {
        // Firstly, assign a virtual register for each function argument,
        // also update localValueMap.
        for (Argument arg : fn.getArgumentList())
        {
            // just update those argument which is not in valueMap.
            if (!valueMap.containsKey(arg))
            {
                int reg = funcInfo.createRegForValue(arg);
                if (reg == 0)
                    return false;

                localValueMap.put(arg, reg);
            }
        }

        CallingConv cc = fn.getCallingConv();
        if (cc != CallingConv.C && cc != CallingConv.Fast
                && cc != CallingConv.X86_FastCall)
            return false;

        // On X86, -tailcallopt changes the fastcc ABI. FastISel doesn't
        // handle this for now.
        if (cc == CallingConv.Fast && EnablePerformTailCallOpt.value)
            return false;

        if (((FunctionType)fn.getType().getElementType()).isVarArg())
        {
            assert false:"Currently variadic function is not supported!";
            return false;
        }

        // A list of regsiters hold the value of each actual argument.
        TIntArrayList argRegs = new TIntArrayList();
        ArrayList<EVT> argVTs = new ArrayList<>();
        ArrayList<ArgFlagsTy> argFlags = new ArrayList<>();

        for (Value arg : fn.getArgumentList())
        {
            int argReg = getRegForValue(arg);
            if (argReg == 0)
                return false;

            ArgFlagsTy Flags = new ArgFlagsTy();

            Type argTy = arg.getType();
            EVT argVT = new EVT();
            OutParamWrapper<EVT> x = new OutParamWrapper<>(argVT);
            if (!isTypeLegal(argTy, x))
                return false;

            argVT = x.get();
            int OriginalAlignment = td.getABITypeAlignment(argTy);
            Flags.setOrigAlign(OriginalAlignment);

            argRegs.add(argReg);
            argVTs.add(argVT);
            argFlags.add(Flags);
        }

        // Analyze operands of the call, assigning locations to each operand.
        ArrayList<CCValAssign> argLocs = new ArrayList<>();

        CCState ccInfo = new CCState(cc, false, tm, argLocs);
        ccInfo.analyzeCallOperands(argVTs, argFlags, CCAssignFnForCall(cc));

        for (int i = 0, e = argLocs.size(); i != e; ++i)
        {
            CCValAssign va = argLocs.get(i);
            // The register where caller pass the specified argument into.
            int argReg = argRegs.get(va.getValNo());
            // The EVT of the specified actual argument.
            EVT argVT = argVTs.get(va.getValNo());

            if (va.isRegLoc())
            {
                // Emit an instruction copy the actual argument from assigned
                // location to the a local virtual register (argReg).
                TargetRegisterClass regClass = tli.getRegClassFor(argVT);
                boolean Emitted = instrInfo.copyRegToReg(mbb, mbb.size(),
                        argReg, va.getLocReg(), regClass, regClass);
                assert Emitted : "Failed to emit a copy instruction!";
                Emitted = true;
            }
            else
            {
                // Emit an instruction copy the actual argument from stack
                // location to the local location (a virtual register).
                int locMemOffset = va.getLocMemOffset();
                X86AddressMode addrMode = new X86AddressMode();
                addrMode.baseType = FrameIndexBase;
                addrMode.base = new X86AddressMode.FrameIndexBase(stackPtr);
                addrMode.disp = locMemOffset;

                int opc;
                if (argVT.getSizeInBits() == 32)
                    opc = MOV32rm;
                else if (argVT.getSizeInBits() == 64)
                    opc = MOV64rm;
                else
                {
                    assert false : "Unsupported passing argument!";
                    return false;
                }
                X86InstrBuilder.addFullAddress(buildMI(mbb, instrInfo.get(opc),
                        argReg), addrMode);
            }
        }
        return true;
    }

    public X86InstrInfo getInstrInfo()
    {
        return (X86InstrInfo)getTargetMachine().getInstrInfo();
    }

    public X86TargetMachine getTargetMachine()
    {
        return (X86TargetMachine)tm;
    }

    public int TargetMaterializeConstant(Constant c)
    {
        OutParamWrapper<EVT> x = new OutParamWrapper<>();
        if (!isTypeLegal(c.getType(), x))
            return 0;

        EVT vt = x.get();
        int opc = 0;
        TargetRegisterClass rc = null;
        switch (vt.getSimpleVT().simpleVT)
        {
            default: return 0;
            case MVT.i8:
                opc = MOV8rm;
                rc = GR8RegisterClass;
                break;
            case MVT.i16:
                opc = MOV16rm;
                rc = GR16RegisterClass;
                break;
            case MVT.i32:
                opc = MOV32rm;
                rc = GR32RegisterClass;
                break;
            case MVT.i64:
                // Must be in x86-64 mode.
                opc = MOV64rm;
                rc = GR64RegisterClass;
                break;
            case MVT.f32:
                if (subtarget.hasSSE1())
                {
                    opc = MOVSSrm;
                    rc = FR32RegisterClass;
                }
                else
                {
                    opc = LD_Fp32m;
                    rc = RFP32RegisterClass;
                }
                break;
            case MVT.f64:
                if (subtarget.hasSSE2())
                {
                    opc = MOVSDrm;
                    rc = FR64RegisterClass;
                } else {
                    opc = LD_Fp64m;
                    rc = RFP64RegisterClass;
                }
                break;
            case MVT.f80:
                // No f80 support yet.
                return 0;
        }

        if (c instanceof GlobalValue)
        {
            X86AddressMode am = new X86AddressMode();
            if (x86SelectAddress(c, am))
            {
                if (tli.getPointerTy() == new MVT(MVT.i32))
                    opc = LEA32r;
                else
                    opc = LEA64r;
                int resultReg = createResultReg(rc);
                X86InstrBuilder
                        .addLeaAddress(buildMI(mbb, instrInfo.get(opc), resultReg), am);
                return resultReg;
            }
            return 0;
        }

        int align = td.getPrefTypeAlignment(c.getType());
        if (align == 0)
        {
            align = (int) td.getTypeAllocSize(c.getType());
        }

        int picBase = 0;
        int opFlag = 0;
        if (subtarget.isPICStyleRIPRel())
        {
            opFlag = MO_PIC_BASE_OFFSET;
            picBase = getInstrInfo().getGlobalBaseReg(mf);
        }
        else if (subtarget.isPICStyleGOT())
        {
            opFlag = MO_GOTOFF;
            picBase = getInstrInfo().getGlobalBaseReg(mf);
        }
        else if (subtarget.isPICStyleRIPRel() &&
                tm.getCodeModel() == Small)
        {
            picBase = RIP;
        }

        int mcpOffset = mcp.getConstantPoolIndex(c, align);
        int resultReg = createResultReg(rc);
        MachineInstrBuilder
                .addConstantPoolReference(buildMI(mbb, instrInfo.get(opc), resultReg),
                mcpOffset, picBase, opFlag);
        return resultReg;
    }

    public int TargetMaterializeAlloca(AllocaInst c)
    {
        // Fail on dynamic allocas. At this point, getRegForValue has already
        // checked its CSE maps, so if we're here trying to handle a dynamic
        // alloca, we're not going to succeed. X86SelectAddress has a
        // check for dynamic allocas, because it's called directly from
        // various places, but TargetMaterializeAlloca also needs a check
        // in order to avoid recursion between getRegForValue,
        // X86SelectAddrss, and TargetMaterializeAlloca.
        if (!staticAllocMap.containsKey(c))
            return 0;

        X86AddressMode am = new X86AddressMode();
        if (!x86SelectAddress(c, am))
            return 0;

        int opc = subtarget.is64Bit() ? LEA64r : LEA32r;
        TargetRegisterClass rc = tli.getRegClassFor(new EVT(tli.getPointerTy()));
        int resultReg = createResultReg(rc);
        X86InstrBuilder
                .addLeaAddress(buildMI(mbb, instrInfo.get(opc), resultReg), am);
        return resultReg;
    }

    /// isScalarFPTypeInSSEReg - Return true if the specified scalar FP type is
    /// computed in an SSE register, not on the X87 floating point stack.
    public boolean isScalarFPTypeInSSEReg(EVT vt)
    {
        return (vt.equals(new EVT(MVT.f64)) && x86ScalarSSEf64) || // f64 is when SSE2
                (vt.equals(new EVT(MVT.f32)) && x86ScalarSSEf32);   // f32 is when SSE1

    }

    public boolean isTypeLegal(Type ty,
            OutParamWrapper<EVT> vt)
    {
        return isTypeLegal(ty, vt, false);
    }

    public boolean isTypeLegal(Type ty,
            OutParamWrapper<EVT> vt,
            boolean allowI1)
    {
        vt.set(tli.getValueType(ty, true));
        if (vt.get().equals(new EVT(MVT.Other)) || !vt.get().isSimple())
            return false;

        if (vt.get().equals(new EVT(MVT.f64)) && !x86ScalarSSEf64)
            return false;

        if (vt.get().equals(new EVT(MVT.f32)) && !x86ScalarSSEf32)
            return false;

        // Currently, f80 is not supported.
        if (vt.get().equals(new EVT(MVT.f80)))
            return false;

        return (allowI1 && vt.get().equals(new EVT(MVT.i1))) || tli.isTypeLegal(vt.get());
    }
}
