package backend.target.x86;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous zeng.
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
import backend.pass.AnalysisUsage;
import backend.support.CallingConv;
import backend.support.IntStatistic;
import backend.target.TargetAsmInfo;
import backend.target.TargetRegisterInfo;
import backend.value.Function;
import backend.value.GlobalValue;
import backend.value.GlobalValue.VisibilityTypes;
import backend.value.Module;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Util;
import tools.commandline.BooleanOpt;
import tools.commandline.OptionHidden;
import tools.commandline.OptionHiddenApplicator;
import tools.commandline.OptionNameApplicator;

import java.io.OutputStream;
import java.util.HashMap;

import static tools.commandline.OptionNameApplicator.optionName;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class X86ATTAsmPrinter extends AsmPrinter
{
    public static final BooleanOpt NewAsmPrinter =
            new BooleanOpt(optionName("experimental-asm-printer"),
                    new OptionHiddenApplicator(OptionHidden.Hidden));
    /**
     * A statistic for indicating the numbeer of emitted machine
     * instruction by this asm printer.
     */
    public static final IntStatistic EmittedInsts =
            new IntStatistic("EmittedInstrs", "Number of machine instrs printed");

    private TObjectIntHashMap<MachineBasicBlock> mbbNumber;
    private X86Subtarget subtarget;
    private HashMap<Function, X86MachineFunctionInfo> functionInfoMap;

    public X86ATTAsmPrinter(OutputStream os,
            X86TargetMachine tm,
            TargetAsmInfo tai,
            boolean v)
    {
        super(os, tm, tai, v);
        mbbNumber = new TObjectIntHashMap<>();
        subtarget = tm.getSubtarget();
        functionInfoMap = new HashMap<>();
    }

    @Override
    public String getPassName()
    {
        return "X86 AT&T style assembly printer";
    }

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        if (subtarget.isTargetDarwin() ||
                subtarget.isTargetELF() ||
                subtarget.isTargetCygMing())
        {
            au.addRequired(MachineModuleInfo.class);
        }
        // TODO dwarf writer.
        super.getAnalysisUsage(au);
    }

    @Override
    public boolean doFinalization(Module m)
    {
        // TODO: 17-7-31
        return false;
    }

    public void printi8mem(MachineInstr mi, int opNo)
    {
        printMemReference(mi, opNo);
    }

    public void printi16mem(MachineInstr mi, int opNo)
    {
        printMemReference(mi, opNo);
    }

    public void printi32mem(MachineInstr mi, int opNo)
    {
        printMemReference(mi, opNo);
    }

    public void printi64mem(MachineInstr mi, int opNo)
    {
        printMemReference(mi, opNo);
    }

    public void printf32mem(MachineInstr mi, int opNo)
    {
        printMemReference(mi, opNo);
    }

    public void printf64mem(MachineInstr mi, int opNo)
    {
        printMemReference(mi, opNo);
    }

    public void printf128mem(MachineInstr mi, int opNo)
    {
        printMemReference(mi, opNo);
    }

    public void print_pcrel_imm(MachineInstr mi, int opNo)
    {
        // TODO: 17-7-31
    }

    public void printi128mem(MachineInstr mi, int opNo)
    {
        printMemReference(mi, opNo);
    }

    private void emitFunctionHeader(MachineFunction mf)
    {
        int fnAlign = mf.getAlignment();
        Function f = mf.getFunction();

        // todo if (subtarget.isTargetCygMing())
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

        printVisibility(curFnName, f.getVisibility());
        if (subtarget.isTargetELF())
        {
            os.printf("\t.type\t%s,@function\n", curFnName);
        }
        else if (subtarget.isTargetCygMing())
        {
            // TODO: 17-7-31  Handle targeting to cygwin and mingw.
        }

        os.printf("%s:", curFnName);
        if (verboseAsm)
        {
            os.print(Util.fixedLengthString(tai.getCommentColumn(), ' '));
            os.printf("%s ", tai.getCommentString());
            writeAsOperand(os, f, false, f.getParent());
        }

        os.println();
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

        // print out labels for the function.
        Function f = mf.getFunction();
        CallingConv cc = f.getCallingConv();

        // Populate function information map.  Actually, We don't want to populate
        // non-stdcall or non-fastcall functions' information right now.
        if (cc == CallingConv.X86_StdCall || cc == CallingConv.X86_FastCall)
            functionInfoMap.put(f, (X86MachineFunctionInfo) mf.getInfo());

        // Print out constants referenced by the function.
        emitConstantPool(mf.getConstantPool());

        // print the 'header' of function.
        emitFunctionHeader(mf);
        os.println(curFnName+":");

        int number = 0;
        for (MachineBasicBlock mbb : mf.getBasicBlocks())
            mbbNumber.put(mbb, number++);

        boolean hasAnyRealCode = false;
        // print out the code for each by walking through all basic block.
        for (MachineBasicBlock mbb : mf.getBasicBlocks())
        {
            // Print a label for the basic block.
            if (!verboseAsm && (mbb.predIsEmpty() || mbb.isOnlyReachableByFallThrough()))
            {
                // This is an entry block or a block that's only reachable via a
                // fallthrough edge. In non-VerboseAsm mode, don't print the label.
            }
            else
            {
                // print the label for the basic block.
                printBasicBlockLabel(mbb, true, true, verboseAsm);
                os.println();
            }

            for (MachineInstr mi : mbb.getInsts())
            {
                // Print the assembly for the instruction.
                if (!mi.isLabel())
                    hasAnyRealCode = true;
                printMachineInstruction(mi);
            }
        }

        if (subtarget.isTargetDarwin() && !hasAnyRealCode)
        {
            os.printf("\tnop\n");
        }

        if (tai.hasDotTypeDotSizeDirective())
            os.println("\t.getNumOfSubLoop " + curFnName + ", .-" + curFnName);

        // todo emitJumpTableInfo(mf.);

        // we didn't change anything.
        return false;
    }

    public void printOperand(MachineInstr mi, int opNo)
    {
        printOperand(mi, opNo, null);
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
                os.print(tai.getPrivateGlobalPrefix());
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
                os.printf("%s%s", tai.getGlobalPrefix(), mo.getSymbolName());
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
        EmittedInsts.inc();
        if (!NewAsmPrinter.value)
        {
            // Call the autogenerated instruction printer routines.
            printInstruction(mi);
        }
    }

    protected abstract boolean printInstruction(MachineInstr mi);

    public static X86ATTAsmPrinter createX86AsmCodeEmitter(
            OutputStream os,
            X86TargetMachine tm,
            TargetAsmInfo tai,
            boolean verbose)
    {
        return new X86GenATTAsmPrinter(os, tm, tai, verbose);
    }
}
