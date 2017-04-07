package backend.target.x86;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2016, Xlous
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

import backend.codegen.AsmPrinter;
import backend.codegen.MachineInstr;
import backend.codegen.MachineOperand;

import java.io.OutputStream;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class X86SharedAsmPrinter extends AsmPrinter
{
    protected X86SharedAsmPrinter(OutputStream os, X86TargetMachine tm)
    {
        super(os, tm);
    }

    static boolean isScale(MachineOperand mo)
    {
        long imm;
        return mo.isImmediate() && (
                ((imm = mo.getImmedValue()) & (imm - 1)) == 0)
                && imm >= 1 && imm <= 8;
    }

    /**
     * Memory operand is like this: baseReg + scale*indexReg+ disp.
     * @param mi
     * @param opNo
     * @return
     */
    static boolean isMem(MachineInstr mi, int opNo)
    {
        if (mi.getOperand(opNo).isFrameIndex()) return true;
        return mi.getNumOperands() >= opNo + 4 &&
                mi.getOperand(opNo).isRegister()
                && isScale(mi.getOperand(opNo + 1))
                && mi.getOperand(opNo + 2).isRegister()
                && (mi.getOperand(opNo + 3).isImmediate()
        || mi.getOperand(opNo + 3).isGlobalAddress()
        || mi.getOperand(opNo + 3).isConstantPoolIndex());
    }
}
