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

package backend.codegen.fastISel;

import backend.codegen.*;
import backend.support.AttrList;
import backend.support.Attribute;
import backend.support.CallingConv;
import backend.target.*;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.Function;
import backend.value.Instruction.CallInst;
import backend.value.Value;
import gnu.trove.list.array.TIntArrayList;
import tools.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class CallLowering
{
    public static class ArgInfo
    {
        public int reg;
        public Type ty;
        public Value val;
        public ArgFlagsTy flags;
        public boolean isFixed;
        public ArgInfo(int reg, Type ty, Value val)
        {
            this(reg, ty, val, new ArgFlagsTy());
        }

        public ArgInfo(int reg, Type ty, Value val, ArgFlagsTy flags)
        {
            this(reg, ty, val, flags, true);
        }
        public ArgInfo(int reg, Type ty, Value val, ArgFlagsTy flags, boolean isFixed)
        {
            this.reg = reg;
            this.ty = ty;
            this.val = val;
            this.flags = flags;
            this.isFixed = isFixed;
        }
    }

    private TargetLowering tli;
    private FastISel isel;

    public CallLowering(TargetLowering tli)
    {
        this.tli = tli;
    }

    public TargetLowering getTLI()
    {
        return tli;
    }

    public void setIsel(FastISel isel)
    {
        this.isel = isel;
    }

    public FastISel getIsel()
    {
        return isel;
    }

    /**
     * Lower the given call instruction, including argument and return value.
     * @param mbb The machine basic block where emit calling code.
     * @param ci    The Calling instruction.
     * @param resReg   The register where resides the returned value from callee.
     * @param args  The list virutal registeres of actual arguments.
     * @param calleeReg The register that resides the address of called function
     *                  if the target determines it can't jump to the destination
     *                  based purely on {@code ci}. This might be because {@code ci}
     *                  is indirect, or because of the limited range of an immediate
     *                  jump.
     * @return
     */
    public boolean lowerCall(
            MachineBasicBlock mbb,
            CallInst ci,
            int resReg,
            TIntArrayList args,
            int calleeReg)
    {
        TargetData td = ci.getParent().getParent().getMachineFunc().getTarget().getTargetData();

        int numFixedArgs = ci.getNumsOfArgs();
        ArrayList<ArgInfo> orignArgs = new ArrayList<>();
        for (int i = 0; i < numFixedArgs; i++)
        {
            ArgInfo ai = new ArgInfo(args.get(i), ci.argumentAt(i).getType(), ci.argumentAt(i));
            setArgFlags(ai, i+ AttrList.FirstArgIndex, td, ci);
            orignArgs.add(ai);
        }
        MachineOperand callee;
        Function f;
        if ((f = ci.getCalledFunction()) != null)
            callee = MachineOperand.createGlobalAddress(f, 0, 0);
        else
            callee = MachineOperand.createReg(calleeReg, false, false);

        ArgInfo origRet = new ArgInfo(resReg, ci.getType(), ci, new ArgFlagsTy());
        if (!origRet.ty.isVoidType())
            setArgFlags(origRet, AttrList.ReturnIndex, td, ci);

        return !lowerCall(mbb, ci.getCallingConv(), callee, origRet, orignArgs);
    }

    /**
     * This hook must be implemented to lower the given call instruction, formal
     * argument and return value.
     * @param mbb The machine basic block where emit calling code.
     * @param callingConv   The calling convention.
     * @param callee    The destination of the call, It should be either a register,
     *                  global address or a external symbol.
     * @param origRet   The Return value information.
     * @param orignArgs The formal arguments information.
     * @return
     */
    public boolean lowerCall(
            MachineBasicBlock mbb,
            CallingConv callingConv,
            MachineOperand callee,
            ArgInfo origRet,
            ArrayList<ArgInfo> orignArgs)
    {
        return false;
    }

    public void setArgFlags(ArgInfo argInfo, int opIdx, TargetData td, CallInst ci)
    {
        AttrList attrs = ci.getAttributes();
        // FIXME atts would be used in future! 2018/2/28
        if (attrs == null) return;

        if (attrs.paramHasAttr(opIdx,Attribute.ZExt))
            argInfo.flags.setZExt();
        if (attrs.paramHasAttr(opIdx, Attribute.SExt))
            argInfo.flags.setSExt();
        if (attrs.paramHasAttr(opIdx, Attribute.StructRet))
            argInfo.flags.setSRet();
        if (attrs.paramHasAttr(opIdx, Attribute.ByVal))
            argInfo.flags.setByVal();

        if (argInfo.flags.isByVal())
        {
            Type eltTy = ((PointerType)argInfo.ty).getElementType();
            argInfo.flags.setByValSize((int) td.getTypeAllocSize(eltTy));
            int frameAlign = 0;
            if (ci.getParamAlignment(opIdx-2) != 0)
                frameAlign = ci.getParamAlignment(opIdx-2);
            else
                Util.shouldNotReachHere();
            argInfo.flags.setByValAlign(frameAlign);
        }
        if (attrs.paramHasAttr(opIdx, Attribute.Nest))
            argInfo.flags.setNest();
        argInfo.flags.setOrigAlign(td.getABITypeAlignment(argInfo.ty));

    }

    public static abstract class ValueHandler
    {
        protected MachineBasicBlock mbb;
        protected MachineRegisterInfo mri;
        protected CCAssignFn assignFn;
        protected TargetLowering tli;
        protected TargetInstrInfo tii;
        protected TargetMachine tm;
        protected TargetSubtarget subtarget;
        protected long stackSize;
        protected FastISel isel;
        protected MachineFrameInfo mfi;

        public ValueHandler(FastISel isel, MachineBasicBlock mbb, CCAssignFn assignFn)
        {
            this.isel = isel;
            this.mbb = mbb;
            this.assignFn = assignFn;
            tm = mbb.getParent().getTarget();
            mri = mbb.getParent().getMachineRegisterInfo();
            tli = tm.getTargetLowering();
            tii = tm.getInstrInfo();
            subtarget = tm.getSubtarget();
            mfi = mbb.getParent().getFrameInfo();
        }

        public FastISel getISel()
        {
            return isel;
        }

        public TargetSubtarget getSubtarget()
        {
            return subtarget;
        }

        public abstract void assignValueToReg(ArgInfo argInfo, CCValAssign ca);

        public abstract void assignValueToStackAddress(
                ArgInfo argInfo,
                int locMemOffset,
                CCValAssign ca);

        public boolean assignArg(int valNo,
                EVT valVT, EVT locVT,
                CCValAssign.LocInfo locInfo,
                ArgInfo argInfo,
                CCState ccInfo)
        {
            return assignFn.apply(valNo, valVT, locVT, locInfo, argInfo.flags, ccInfo);
        }

        public abstract int assignCustomValue(ArgInfo argInfo,
                List<CCValAssign> ccValAssigns);

        public long getStackSize()
        {
            return stackSize;
        }

        public void setStackSize(int stackSize)
        {
            this.stackSize = stackSize;
        }

        public MachineBasicBlock getMBB()
        {
            return mbb;
        }
    }
}
