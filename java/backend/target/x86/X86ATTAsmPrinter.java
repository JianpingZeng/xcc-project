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

import backend.codegen.*;
import backend.target.TargetRegisterInfo;
import backend.value.Function;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.OutputStream;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class X86ATTAsmPrinter extends X86SharedAsmPrinter
{
    private TObjectIntHashMap<MachineBasicBlock> mbbNumber;
    /**
     * A statistic for indicating the numbeer of emitted machine
     * instruction by this asm printer.
     */
    private int emittedInsts;

    public X86ATTAsmPrinter(OutputStream os, X86TargetMachine tm)
    {
        super(os, tm);
        mbbNumber = new TObjectIntHashMap<>();
    }

    @Override
    public String getPassName()
    {
        return "X86 AT&T assembly code printer";
    }

    public void printi8Mem(MachineInstr mi, int opNo)
    {
        printMemReference(mi, opNo);
    }

    public void printi16Mem(MachineInstr mi, int opNo)
    {
        printMemReference(mi, opNo);
    }

    public void printi32Mem(MachineInstr mi, int opNo)
    {
        printMemReference(mi, opNo);
    }

    public void printi64Mem(MachineInstr mi, int opNo)
    {
        printMemReference(mi, opNo);
    }

    public void printf32Mem(MachineInstr mi, int opNo)
    {
        printMemReference(mi, opNo);
    }

    public void printf64Mem(MachineInstr mi, int opNo)
    {
        printMemReference(mi, opNo);
    }

    public void printf128Mem(MachineInstr mi, int opNo)
    {
        printMemReference(mi, opNo);
    }

    /**
     * This method is called used for print assembly code for each
     * machine instruction in {@code mf}.
     * @param mf
     * @return
     */
    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        setupMachineFunction(mf);
        os.print("\n\n");

        // print out constants referenced by the function.
        emitConstantPool(mf.getConstantPool());

        // print out labels for the function.
        Function f = mf.getFunction();
        switch (f.getLinkage())
        {
            case InteralLinkage:
                switchSection(".text", f);
                emitAlignment(4, f);
                break;
            case ExternalLinkage:
                switchSection(".text", f);
                emitAlignment(4, f);
                os.println("\t.global\t" + curFnName);
                break;
            default:
                assert false:"Unknown linkage type!";
                break;
        }
        os.println(curFnName+":");

        int number = 0;
        for (MachineBasicBlock mbb : mf.getBasicBlocks())
            mbbNumber.put(mbb, number++);

        // print out the code for each by walking through all basic block.
        for (MachineBasicBlock mbb : mf.getBasicBlocks())
        {
            // print the label for the basic block.
            os.print(privateGlobalPrefix + "BB" + mbbNumber.get(mbb) + ":\t");
            os.println(commentString + " " + mbb.getBasicBlock().getName());

            for (MachineInstr mi : mbb.getInsts())
            {
                // Print the assembly for the instruction.
                os.print("\t");
                printMachineInstruction(mi);
            }
        }

        if (hasDotTypeDotSizeDirective)
            os.println("\t.getNumOfSubLoop " + curFnName + ", .-" + curFnName);

        // we didn't change anything.
        return false;
    }

    /**
     * Theses methods are used by the tablegen created instruction printer.
     * @param mi
     * @param opNo
     * @param modifier
     */
    public void printOperand(MachineInstr mi, int opNo, String modifier)
    {
        MachineOperand mo = mi.getOperand(opNo);
        TargetRegisterInfo regInfo = tm.getRegisterInfo();
        switch (mo.getType())
        {
            case MO_Register:
            {
                os.print("%");
                os.print(regInfo.getName(mo.getReg()).toLowerCase());
                return;
            }
            case MO_Immediate:
            {
                if (modifier == null || modifier.equals("debug"))
                    os.print("$");
                os.print((int)mo.getImm());
                return;
            }
            case MO_MachineBasicBlock:
            {
                MachineBasicBlock mbb = mo.getMBB();
                os.print(privateGlobalPrefix);
                os.print("BB");
                os.print(mangler.getValueName(mbb.getBasicBlock().getParent()));
                os.print("_");
                os.print(mbbNumber.get(mbb));
                os.print("\t#");
                os.print(mbb.getBasicBlock().getName());
                return;
            }
            case MO_GlobalAddress:
            {
                boolean isCallOp = modifier != null && !modifier.equals("call");
                boolean isMemOp = modifier != null && !modifier.equals("mem");
                if (!isCallOp && !isMemOp)
                    os.print("$");

                os.print(mangler.getValueName(mo.getGlobal()));
                return;
            }
            case MO_ExternalSymbol:
            {
                boolean isCallOp = modifier != null && !modifier.equals("call");
                if (!isCallOp)
                    os.print("$");
                os.printf("%s%s", globalPrefix, mo.getSymbolName());
                return;
            }
            default:
                os.print("<unknown operand type>");
        }
    }

    private void printMemReference(MachineInstr mi, int opNo)
    {
        assert isMem(mi, opNo) : "Invalid memory reference!";

        MachineOperand baseReg = mi.getOperand(opNo);
        int scaleVal = (int) mi.getOperand(opNo + 1).getImm();
        MachineOperand indexReg = mi.getOperand(opNo + 2);
        MachineOperand disp = mi.getOperand(opNo + 3);

        if (baseReg.isFrameIndex())
        {
            os.print("[frame slot #");
            os.print(baseReg.getIndex());
            if (disp.getImm() != 0)
                os.print("+" + disp.getImm());
            os.print("]");
            return;
        }

        if (disp.isGlobalAddress() || disp.isConstantPoolIndex())
        {
            printOperand(mi, opNo + 3, "mem");
        }
        else
        {
            int dispVal = (int)disp.getImm();
            if (dispVal != 0 || (indexReg.getReg() == 0) && baseReg.getReg() == 0)
                os.print(dispVal);
        }

        if (indexReg.getReg() != 0 || baseReg.getReg() != 0)
        {
            os.print("(");
            if (baseReg.getReg() != 0)
                printOperand(mi, opNo, null);

            if (indexReg.getReg() != 0)
            {
                os.print(",");
                printOperand(mi, opNo + 2, null);
                if (scaleVal != 1)
                    os.print(", " + scaleVal);
            }
            os.print(")");
        }
    }

    /**
     * Prints each machine instruction in AT&T syntax to the current output
     * stream.
     * @param mi
     */
    private void printMachineInstruction(MachineInstr mi)
    {
        emittedInsts++;

        // Call the autogenerated instruction printer routines.
        printInstruction(mi);
    }

    protected abstract boolean printInstruction(MachineInstr mi);

    public static X86ATTAsmPrinter createX86AsmCodeEmitter(
            OutputStream os, X86TargetMachine tm)
    {
        return new X86GenATTAsmPrinter(os, tm);
    }
}
