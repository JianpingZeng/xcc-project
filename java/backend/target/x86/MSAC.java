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

import backend.codegen.MachineFrameInfo;
import backend.codegen.MachineFunction;
import backend.codegen.MachineFunctionPass;
import backend.codegen.MachineRegisterInfo;
import backend.pass.AnalysisUsage;
import backend.target.TargetRegisterInfo;

/**
 * This machine function pass take responsibility for calculating the maximum
 * alignment for specified MachineFunction.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class MSAC extends MachineFunctionPass
{
    /**
     * Set the maximum alignment for the specified machine function according to
     * all of stack object in current stack frame and alignment of each Virtual
     * reigster.
     * @param mf
     * @return  Return true if alignment of specified function is changed.
     */
    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        int maxAlign = -1;
        // Compute maximum alignment for each stack object.
        MachineFrameInfo mfi = mf.getFrameInfo();
        for (int i = mfi.getObjectIndexBegin(); i < mfi.getObjectIndexEnd(); i++)
        {
            int align = mfi.getObjectAlignment(i);
            if (align > maxAlign)
                maxAlign = align;
        }
        // Compute maximum alignment for each virtual register.
        MachineRegisterInfo mri = mf.getMachineRegisterInfo();
        int lastVR = mri.getLastVirReg();
        for (int vr =TargetRegisterInfo.FirstVirtualRegister; vr < lastVR; vr++)
        {
            int align = mri.getRegClass(vr).getAlignment();
            if (align > maxAlign)
                maxAlign = align;
        }
        if (mfi.getMaxAlignment() == maxAlign)
            return false;
        mfi.setMaxCallFrameSize(maxAlign);
        return true;
    }

    @Override
    public String getPassName()
    {
        return "X86 Maximal Stack Alignment Calculator";
    }

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.setPreservesCFG();
        super.getAnalysisUsage(au);
    }

    public static MSAC createMaxStackAlignmentCalculatorPass()
    {
        return new MSAC();
    }
}
