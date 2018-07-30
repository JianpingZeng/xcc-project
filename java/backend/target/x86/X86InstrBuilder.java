/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import tools.Util;
import backend.codegen.MachineInstrBuilder;

import static backend.target.x86.X86AddressMode.BaseType.FrameIndexBase;
import static backend.target.x86.X86AddressMode.BaseType.RegBase;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class X86InstrBuilder
{
    public static MachineInstrBuilder addFullAddress(MachineInstrBuilder mib,
            X86AddressMode am)
    {
        return addLeaAddress(mib, am).addReg(0);
    }

    public static MachineInstrBuilder addLeaAddress(MachineInstrBuilder mib,
            X86AddressMode am)
    {
        Util.assertion( am.scale == 1 || am.scale == 2 || am.scale == 4 || am.scale == 8);

        if (am.baseType == RegBase)
            mib.addReg(am.base.getBase());
        else if (am.baseType == FrameIndexBase)
            mib.addFrameIndex(am.base.getBase());
        else
            Util.assertion( false);

        mib.addImm(am.scale).addReg(am.indexReg);
        if (am.gv != null)
            return mib.addGlobalAddress(am.gv, am.disp, am.gvOpFlags);
        else
            return mib.addImm(am.disp);
    }
}
