package backend.codegen;
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

import backend.support.CallingConv;
import backend.target.TargetMachine;
import backend.target.TargetRegisterInfo;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;

/**
 * This class holds information needed while lowering arguments and
 × return values.  It captures which registers are already assigned and which
 × stack slots are used.  It provides accessors to allocate these values.
 * @author Xlous.zeng
 * @version 0.1
 */
public class CCState
{
    CallingConv CallingConv;
    boolean IsVarArg;
   TargetMachine tm;
   TargetRegisterInfo TRI;
    ArrayList<CCValAssign> Locs;

    int StackOffset;
    TIntArrayList UsedRegs;
    public CCState(backend.support.CallingConv CC, boolean isVarArg, TargetMachine tm,
            ArrayList<CCValAssign> locs)
    {}

    void addLoc( CCValAssign V) {
        Locs.add(V);
    }
    
   TargetMachine getTarget()  { return tm; }
    int getCallingConv()  { return CallingConv; }
    boolean isVarArg()  { return IsVarArg; }

    public int getNextStackOffset()  { return StackOffset; }

    /// isAllocated - Return true if the specified register (or an alias) is
    /// allocated.
    boolean isAllocated(int Reg)  {
    return UsedRegs[Reg/32]  (1 << (Reg31));
}

    /// AnalyzeFormalArguments - Analyze an array of argument values,
    /// incorporating info about the formals into this state.
    void AnalyzeFormalArguments( SmallVectorImpl<ISD::InputArg> Ins,
            CCAssignFn Fn);

    /// AnalyzeReturn - Analyze the returned values of a return,
    /// incorporating info about the result values into this state.
    void AnalyzeReturn( SmallVectorImpl<ISD::OutputArg> Outs,
            CCAssignFn Fn);

    /// AnalyzeCallOperands - Analyze the outgoing arguments to a call,
    /// incorporating info about the passed values into this state.
    void AnalyzeCallOperands( SmallVectorImpl<ISD::OutputArg> Outs,
            CCAssignFn Fn);

    /// AnalyzeCallOperands - Same as above except it takes vectors of types
    /// and argument flags.
    void AnalyzeCallOperands(SmallVectorImpl<EVT> ArgVTs,
            SmallVectorImpl<ISD::ArgFlagsTy> Flags,
            CCAssignFn Fn);

    /// AnalyzeCallResult - Analyze the return values of a call,
    /// incorporating info about the passed values into this state.
    void AnalyzeCallResult( SmallVectorImpl<ISD::InputArg> Ins,
            CCAssignFn Fn);

    /// AnalyzeCallResult - Same as above except it's specialized for calls which
    /// produce a single value.
    void AnalyzeCallResult(EVT VT, CCAssignFn Fn);

    /// getFirstUnallocated - Return the first unallocated register in the set, or
    /// NumRegs if they are all allocated.
    int getFirstUnallocated( int *Regs, int NumRegs)  {
    for (int i = 0; i != NumRegs; ++i)
        if (!isAllocated(Regs[i]))
            return i;
    return NumRegs;
}

    /// AllocateReg - Attempt to allocate one register.  If it is not available,
    /// return zero.  Otherwise, return the register, marking it and any aliases
    /// as allocated.
    int AllocateReg(int Reg) {
        if (isAllocated(Reg)) return 0;
        MarkAllocated(Reg);
        return Reg;
    }

    /// Version of AllocateReg with extra register to be shadowed.
    int AllocateReg(int Reg, int ShadowReg) {
        if (isAllocated(Reg)) return 0;
        MarkAllocated(Reg);
        MarkAllocated(ShadowReg);
        return Reg;
    }

    /// AllocateReg - Attempt to allocate one of the specified registers.  If none
    /// are available, return zero.  Otherwise, return the first one available,
    /// marking it and any aliases as allocated.
    int AllocateReg( int *Regs, int NumRegs) {
        int FirstUnalloc = getFirstUnallocated(Regs, NumRegs);
        if (FirstUnalloc == NumRegs)
            return 0;    // Didn't find the reg.

        // Mark the register and any aliases as allocated.
        int Reg = Regs[FirstUnalloc];
        MarkAllocated(Reg);
        return Reg;
    }

    /// Version of AllocateReg with list of registers to be shadowed.
    int AllocateReg( int *Regs,  int *ShadowRegs,
            int NumRegs) {
        int FirstUnalloc = getFirstUnallocated(Regs, NumRegs);
        if (FirstUnalloc == NumRegs)
            return 0;    // Didn't find the reg.

        // Mark the register and any aliases as allocated.
        int Reg = Regs[FirstUnalloc], ShadowReg = ShadowRegs[FirstUnalloc];
        MarkAllocated(Reg);
        MarkAllocated(ShadowReg);
        return Reg;
    }

    /// AllocateStack - Allocate a chunk of stack space with the specified size
    /// and alignment.
    int AllocateStack(int Size, int Align) {
        assert(Align && ((Align-1)  Align) == 0); // Align is power of 2.
        StackOffset = ((StackOffset + Align-1)  ~(Align-1));
        int Result = StackOffset;
        StackOffset += Size;
        return Result;
    }

    // HandleByVal - Allocate a stack slot large enough to pass an argument by
    // value. The size and alignment information of the argument is encoded in its
    // parameter attribute.
    void HandleByVal(int ValNo, EVT ValVT,
            EVT LocVT, CCValAssign::LocInfo LocInfo,
            int MinSize, int MinAlign, ISD::ArgFlagsTy ArgFlags);

    private:
    /// MarkAllocated - Mark a register and all of its aliases as allocated.
    void MarkAllocated(int Reg);
}
