package backend.codegen;
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

import tools.Util;
import backend.codegen.CCValAssign.LocInfo;
import backend.support.CallingConv;
import backend.target.TargetMachine;
import backend.target.TargetRegisterInfo;
import gnu.trove.list.array.TIntArrayList;
import tools.Util;

import java.util.ArrayList;

/**
 * This class holds information needed while lowering arguments and
 * return values.  It captures which registers are already assigned and which
 * stack slots are used.  It provides accessors to allocate these values.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class CCState
{
    private CallingConv callingConv;
    private boolean isVarArg;
    private TargetMachine tm;
    private TargetRegisterInfo tri;
    private ArrayList<CCValAssign> locs;

    private int stackOffset;
    private TIntArrayList usedRegs;

    public CCState(backend.support.CallingConv cc,
            boolean isVarArg,
            TargetMachine tm,
            ArrayList<CCValAssign> locs)
    {
        callingConv = cc;
        this.isVarArg = isVarArg;
        this.tm = tm;
        tri = tm.getRegisterInfo();
        this.locs = locs;
        stackOffset = 0;
        usedRegs = new TIntArrayList();
        for (int capacity = (tri.getNumRegs() + 31) / 32;
             capacity != 0; --capacity)
            usedRegs.add(0);
    }

    public void addLoc(CCValAssign V)
    {
        locs.add(V);
    }

    public TargetMachine getTarget()
    {
        return tm;
    }

    public CallingConv getCallingConv()
    {
        return callingConv;
    }

    public boolean isVarArg()
    {
        return isVarArg;
    }

    public int getNextStackOffset()
    {
        return stackOffset;
    }

    /// isAllocated - Return true if the specified register (or an alias) is
    /// allocated.
    public boolean isAllocated(int reg)
    {
        return (usedRegs.get(reg / 32) & (1 << (reg & 31))) != 0;
    }

    /// analyzeFormalArguments - Analyze an array of argument values,
    /// incorporating info about the formals into this state.
    public void analyzeFormalArguments(ArrayList<InputArg> ins, CCAssignFn fn)
    {
        int idx = 0;
        for(InputArg arg : ins)
        {
            ArgFlagsTy argFlags = arg.flags;
            EVT argVT = arg.vt;
            if (fn.apply(idx++, argVT, argVT, LocInfo.Full, argFlags, this))
            {
                Util.shouldNotReachHere();
            }
        }
    }

    /// analyzeReturn - Analyze the returned values of a return,
    /// incorporating info about the result values into this state.
    public void analyzeReturn(ArrayList<OutputArg> outs, CCAssignFn fn)
    {
        int idx = 0;
        for(OutputArg arg : outs)
        {
            ArgFlagsTy argFlags = arg.flags;
            EVT argVT = arg.val.getValueType();
            if (fn.apply(idx++, argVT, argVT, LocInfo.Full, argFlags, this))
            {
                Util.shouldNotReachHere();
            }
        }
    }

    /// analyzeCallOperands - Analyze the outgoing arguments to a call,
    /// incorporating info about the passed values into this state.
    public void analyzeCallOperands(
            ArrayList<OutputArg> outs,
            CCAssignFn fn)
    {
        int idx = 0;
        for(OutputArg arg : outs)
        {
            ArgFlagsTy argFlags = arg.flags;
            EVT argVT = arg.val.getValueType();
            if (fn.apply(idx++, argVT, argVT, LocInfo.Full, argFlags, this))
            {
                Util.shouldNotReachHere();
            }
        }
    }

    /// analyzeCallOperands - Same as above except it takes vectors of types
    /// and argument flags.
    public void analyzeCallOperands(
            ArrayList<EVT> argVTs,
            ArrayList<ArgFlagsTy> flags,
            CCAssignFn fn)
    {
        for(int idx = 0; idx < argVTs.size(); idx++)
        {
            ArgFlagsTy argFlags = flags.get(idx);
            EVT argVT = argVTs.get(idx);
            if (fn.apply(idx, argVT, argVT, LocInfo.Full, argFlags, this))
            {
                Util.shouldNotReachHere();
            }
        }
    }

    /// analyzeCallResult - Analyze the return values of a call,
    /// incorporating info about the passed values into this state.
    public void analyzeCallResult(ArrayList<InputArg> ins, CCAssignFn fn)
    {
        int idx = 0;
        for(InputArg arg : ins)
        {
            ArgFlagsTy argFlags = arg.flags;
            EVT argVT = arg.vt;
            if (fn.apply(idx++, argVT, argVT, LocInfo.Full, argFlags, this))
            {
                Util.shouldNotReachHere();
            }
        }
    }

    /// analyzeCallResult - Same as above except it's specialized for calls which
    /// produce a single value.
    public void analyzeCallResult(EVT vt, CCAssignFn fn)
    {
        if (fn.apply(0, vt, vt, LocInfo.Full, new ArgFlagsTy(), this))
        {
            Util.shouldNotReachHere();
        }
    }

    /// getFirstUnallocated - Return the first unallocated register in the set, or
    /// NumRegs if they are all allocated.
    public int getFirstUnallocated(int[] regs)
    {
        for (int i = 0; i != regs.length; ++i)
            if (!isAllocated(regs[i]))
                return i;
        return -1;
    }

    /// allocateReg - Attempt to allocate one register.  If it is not available,
    /// return zero.  Otherwise, return the register, marking it and any aliases
    /// as allocated.
    public int allocateReg(int reg)
    {
        if (isAllocated(reg))
            return 0;
        markAllocated(reg);
        return reg;
    }

    /// Version of allocateReg with extra register to be shadowed.
    public int allocateReg(int reg, int shadowReg)
    {
        if (isAllocated(reg))
            return 0;
        markAllocated(reg);
        markAllocated(shadowReg);
        return reg;
    }

    /// allocateReg - Attempt to allocate one of the specified registers.  If none
    /// are available, return zero.  Otherwise, return the first one available,
    /// marking it and any aliases as allocated.
    public int allocateReg(int[] regs)
    {
        int FirstUnalloc = getFirstUnallocated(regs);
        if (FirstUnalloc < 0)
            return 0;    // Didn't find the reg.

        // Mark the register and any aliases as allocated.
        int Reg = regs[FirstUnalloc];
        markAllocated(Reg);
        return Reg;
    }

    /// Version of allocateReg with list of registers to be shadowed.
    public int allocateReg(int[] regs, int[] shadowRegs)
    {
        int FirstUnalloc = getFirstUnallocated(regs);
        if (FirstUnalloc < 0)
            return 0;    // Didn't find the reg.

        // Mark the register and any aliases as allocated.
        int Reg = regs[FirstUnalloc], ShadowReg = shadowRegs[FirstUnalloc];
        markAllocated(Reg);
        markAllocated(ShadowReg);
        return Reg;
    }

    /// allocateStack - Allocate a chunk of stack space with the specified size
    /// and alignment.
    public int allocateStack(int size, int align)
    {
        Util.assertion( (align != 0 && ((align - 1) & align) == 0)); // align is power of 2.        stackOffset = ((stackOffset + align - 1) & ~(align - 1));

        int Result = stackOffset;
        stackOffset += size;
        return Result;
    }

    /**
     * Allocate a stack slot large enough to pass an argument by
     * value. The size and alignment information of the argument is encoded in its
     * parameter attribute.
     * @param valNo
     * @param valVT
     * @param locVT
     * @param locInfo
     * @param minSize
     * @param minAlign
     * @param argFlags
     */
    public void handleByVal(int valNo, EVT valVT, EVT locVT, LocInfo locInfo,
            int minSize, int minAlign, ArgFlagsTy argFlags)
    {
        int align = argFlags.getByValAlign();
        int size = argFlags.getByValSize();
        if (minSize > size)
            size = minSize;
        if (minAlign > align)
            align = minAlign;

        int offset = allocateStack(size, align);
        addLoc(CCValAssign.getMem(valNo, valVT, offset, locVT, locInfo));
    }

    /// markAllocated - Mark a register and all of its aliases as allocated.
    private void markAllocated(int reg)
    {
        usedRegs.set(reg/32, usedRegs.get(reg/32) | (1<<reg&31));
        int[] aliases = tri.getAliasSet(reg);
        if (aliases != null)
        {
            for (int alias : aliases)
                usedRegs.set(alias/32, usedRegs.get(alias/32) | (1<<alias&31));
        }
    }
}
