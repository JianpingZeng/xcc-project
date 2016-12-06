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
import backend.target.TargetInstrInfo;
import backend.target.TargetInstrInfo.TargetInstrDescriptor;
import backend.target.TargetMachine;
import backend.target.TargetRegisterInfo;
import backend.value.Function;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.OutputStream;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class X86ATTAsmPrinter extends AsmPrinter
{
    /**
     * An array for storing the name of each x86 machine instruction.
     */
    private static String[] instName = {
                    "PHINODE",
                    "nop\n",
                    "#ADJCALLSTACKDOWN ",
                    "#ADJCALLSTACKUP ",
                    "#IMPLICIT_USE ",
                    "#IMPLICIT_DEF ",
                    "ret ",
                    "jmp ",
                    "jb ",
                    "jae ",
                    "je ",
                    "jne ",
                    "jbe ",
                    "ja ",
                    "js ",
                    "jns ",
                    "jl ",
                    "jge ",
                    "jle ",
                    "jg ",
                    "call ",
                    "call *",
                    "call *",
                    "leave ",
                    "bswapl ",
                    "xchgb ",
                    "xchgw ",
                    "xchgl ",
                    "leaw ",
                    "leal ",

                    "movb ",
                    "movw ",
                    "movl ",
                    "movb ",
                    "movw ",
                    "movl ",
                    "movb ",
                    "movw ",
                    "movl ",
                    "movb ",
                    "movw ",
                    "movl ",
                    "movb ",
                    "movw ",
                    "movl ",

                    "mulb ",
                    "mulw ",
                    "mull ",

                    "divb ",
                    "divw ",
                    "divl ",

                    "idivb ",
                    "idivw ",
                    "idivl ",

                    "cbw ",
                    "cwd ",
                    "cdq ",

                    "negb ",
                    "negw ",
                    "negl ",
                    "notb ",
                    "notw ",
                    "notl ",
                    "incb ",
                    "incw ",
                    "incl ",
                    "decb ",
                    "decw ",
                    "decl ",

                    "addb ",
                    "addw ",
                    "addl ",
                    "addb ",
                    "addw ",
                    "addl ",
                    "addw ",
                    "addl ",
                    "adcl ",

                    "subb ",
                    "subw ",
                    "subl ",
                    "subb ",
                    "subw ",
                    "subl ",
                    "subw ",
                    "subl ",
                    "sbbl ",

                    "imulw ",
                    "imull ",
                    "imulw ",
                    "imull ",
                    "imulw ",
                    "imull ",
                    "imulw ",
                    "imull ",

                    "andb ",
                    "andw ",
                    "andl ",
                    "andb ",
                    "andw ",
                    "andl ",
                    "andw ",
                    "andl ",

                    "orb ",
                    "orw ",
                    "orl ",
                    "orb ",
                    "orw ",
                    "orl ",
                    "orw ",
                    "orl ",

                    "xorb ",
                    "xorw ",
                    "xorl ",
                    "xorb ",
                    "xorw ",
                    "xorl ",
                    "xorw ",
                    "xorl ",

                    "testb ",
                    "testw ",
                    "testl ",
                    "testb ",
                    "testw ",
                    "testl ",

                    "shlb %cl, ",
                    "shlw %cl, ",
                    "shll %cl, ",
                    "shlb ",
                    "shlw ",
                    "shll ",
                    "shrb %cl, ",
                    "shrw %cl, ",
                    "shrl %cl, ",
                    "shrb ",
                    "shrw ",
                    "shrl ",

                    "sarb %cl, ",
                    "sarw %cl, ",
                    "sarl %cl, ",
                    "sarb ",
                    "sarw ",
                    "sarl ",

                    "shldl %cl, ",
                    "shldl, ",
                    "shrdl %cl, ",
                    "shrdl, ",

                    "sahf",
                    "setbr",
                    "setaer",
                    "seter",
                    "setner",
                    "setber",
                    "setar",
                    "setsr",
                    "setnsr",
                    "setlr",
                    "setger",
                    "setler",
                    "setgr",

                    "cmove",
                    "cmovne",

                    "cmpb ",
                    "cmpw ",
                    "cmpl ",
                    "cmpb ",
                    "cmpw ",
                    "cmpl ",

                    "movsbw ",
                    "movsbl ",
                    "movswl ",
                    "movzbw ",
                    "movzbl ",
                    "movzwl ",

                    "FpMOV ",
                    "FpADD ",
                    "FpSUB ",
                    "FpMUL ",
                    "FpDIV ",
                    "FpUCOM ",
                    "FpGETRESULT ",
                    "FpSETRESULT ",

                    "fld ",
                    "fldw ",
                    "flds ",
                    "fldl ",
                    null,
                    "fildw ",
                    "filds ",
                    "fildl ",
                    "fsts ",
                    "fstl ",
                    "fstps ",
                    "fstpl ",
                    null,
                    "fst ",
                    "fstp ",
                    "fistw ",
                    "fists ",
                    "fistl ",
                    "fistpw ",
                    "fistps ",
                    "fistpl ",
                    "fxch ",

                    "fld0\n",
                    "fld1\n",

                    "fadd ",
                    "fadd %st(0), ",
                    "faddp ",

                    "fsubr ",
                    "fsub %st(0), ",
                    "fsubrp ",

                    "fsub ",
                    "fsubr %st(0), ",
                    "fsubrp ",

                    "fmul ",
                    "fmul %st(0), ",
                    "fmulp ",

                    "fdivr ",
                    "fdiv %st(0), ",
                    "fdivp ",

                    "fdiv ",
                    "fdiv %st(0), ",
                    "fdivrp ",

                    "fucom ",
                    "fucomp ",
                    "fucompp\n",

                    "fnstsw\n",
                    "fnstcw ",
                    "fldcw "
            };
    private TObjectIntHashMap<MachineBasicBlock> mbbNumber;
    /**
     * A statistic for indicating the numbeer of emitted machine
     * instruction by this asm printer.
     */
    private int emittedInsts;

    public X86ATTAsmPrinter(OutputStream os, TargetMachine tm)
    {
        super(os, tm);
        mbbNumber = new TObjectIntHashMap<>();
    }

    @Override
    public void printAsmOperand(MachineInstr mi, int opNo, int asmVariant,
            String extra)
    {

    }

    @Override
    public void printAsmMemoryOperand(MachineInstr mi, int opNo,
            int asmVariant, String extra)
    {

    }

    @Override
    public String getPassName()
    {
        return "X86 AT&T assembly code printer";
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
            os.println("\t.size " + curFnName + ", .-" + curFnName);

        // we didn't change anything.
        return false;
    }

    private void printOp(MachineOperand mo)
    {
        TargetRegisterInfo regInfo = tm.getRegInfo();
        switch (mo.getType())
        {
            case MO_VirtualRegister:
            case MO_MachineRegister:
            {
                assert mo.isPhysicalRegister();
                os.print("%");
                os.print(regInfo.getName(mo.getReg()).toLowerCase());
                return;
            }
            case MO_SignExtendedImmed:
            case MO_UnextendedImmed:
            {
                os.print("$" + mo.getImmedValue());
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
            case MO_PCRelativeDisp:
            {
                assert mbbNumber.contains(mo.getVRegValue()):"Could find a BB in the numberForBB";
                os.print(".LBB");
                os.print(mbbNumber.get(mo.getVRegValue()));
                os.print("\t#PC rel: ");
                os.print(mo.getVRegValue().getName());
                return;
            }
            case MO_ConstantPoolIndex:
            {
                os.print("$");
                os.print(privateGlobalPrefix);
                os.print("CPI");
                os.print(getFunctionNumber());
                os.print("_");
                os.print(mo.getConstantPoolIndex());
                return;
            }
            case MO_GlobalAddress:
            {
                os.print("$");
                os.print(mangler.getValueName(mo.getGlobal()));
                return;
            }
            case MO_ExternalSymbol:
            {
                os.print("$");
                os.print(mo.getSymbolName());
                return;
            }
            default:
                os.print("<unknown operand type>");
                return;
        }
    }

    private void checkImplUses(TargetInstrDescriptor desc)
    {
        TargetRegisterInfo regInfo = tm.getRegInfo();
        if ((desc.tSFlags & X86II.PrintImplUses) != 0)
        {
            for (int i = 0, e = desc.implicitUses.length; i < e; i++)
            {
                int reg = desc.implicitUses[i];
                os.print(regInfo.getName(reg).toLowerCase());
                if (i != e - 1)
                    os.print(", ");
            }
        }
    }

    private boolean isScale(MachineOperand mo)
    {
        long val = mo.getImmedValue();
        return mo.isImmediate() && (val == 1 || val == 2 || val == 4 || val == 8);
    }

    private boolean isMem(MachineInstr mi, int opNo)
    {
        MachineOperand mo = mi.getOperand(opNo);
        if (mo.isFrameIndex() || mo.isConstantPoolIndex()) return true;

        return opNo+4 <= mi.getNumOperands()
                && mi.getOperand(opNo).isRegister()
                && isScale(mi.getOperand(opNo+1))
                && mi.getOperand(opNo+2).isRegister()
                && mi.getOperand(opNo+3).isImmediate();

    }

    private void printMemReference(MachineInstr mi, int opNo)
    {
        assert isMem(mi, opNo) : "Invalid memory reference!";

        MachineOperand baseReg = mi.getOperand(opNo);
        int scaleVal = (int) mi.getOperand(opNo + 1).getImmedValue();
        MachineOperand indexReg = mi.getOperand(opNo + 2);
        MachineOperand disp = mi.getOperand(opNo + 3);

        if (baseReg.isFrameIndex())
        {
            os.print("[frame slot #]");
            os.print(baseReg.getFrameIndex());
            if (disp.getImmedValue() != 0)
                os.print("+" + disp.getImmedValue());
            os.print("]");
            return;
        }
        else if (baseReg.isConstantPoolIndex())
        {
            os.print(privateGlobalPrefix);
            os.print(curFnName);
            os.print("_");
            os.print(baseReg.getConstantPoolIndex());
            if (disp.getImmedValue() != 0)
                os.print(" + " + disp.getImmedValue());
            os.print("]");
            return;
        }

        if (disp.isGlobalAddress() || disp.isConstantPoolIndex())
        {
            printOp(disp);
        }
        else
        {
            int dispVal = (int) disp.getImmedValue();
            if (dispVal != 0 || (indexReg.getReg() == 0
                    && baseReg.getReg() == 0))
                os.print(dispVal);
        }

        if (indexReg.getReg() != 0
                || baseReg.getReg() != 0)
        {
            os.print("(");
            if (baseReg.getReg() != 0)
                printOp(baseReg);

            if (indexReg.getReg() != 0)
            {
                os.print(",");
                printOp(indexReg);
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

        TargetInstrInfo instrInfo = tm.getInstrInfo();
        int opcode = mi.getOpCode();
        TargetInstrDescriptor desc = instrInfo.get(opcode);
        String name = instName[opcode];

        switch (desc.tSFlags & X86II.FormMask)
        {
            case X86II.Pseudo:
            {
                // print pesudo instruction as comments;
                os.print("\t#");
                if (opcode == X86InstrSets.PHI)
                {
                    printOp(mi.getOperand(0));
                    os.print(" = phi ");
                    for (int i = 1, e = mi.getNumOperands(); i < e; i+=2)
                    {
                        if (i != 1) os.print(", ");
                        os.print("[");
                        printOp(mi.getOperand(i));
                        os.print(", ");
                        printOp(mi.getOperand(i+1));
                        os.print("]");
                    }
                }
                else
                {
                    int i = 0;
                    if (mi.getNumOperands() >=0 && (mi.getOperand(0).opIsDefAndUse()
                        || mi.getOperand(0).opIsDef()))
                    {
                        printOp(mi.getOperand(0));
                        os.print(" = ");
                        ++i;
                    }
                    os.print(instrInfo.getName(opcode));
                    for (int e = mi.getNumOperands(); i < e; i++)
                    {
                        os.print(" ");
                        boolean isDef = false;
                        if (mi.getOperand(i).opIsDef()
                                || mi.getOperand(i).opIsDefAndUse())
                        {
                            isDef = true;
                            os.print("*");
                        }
                        printOp(mi.getOperand(i));
                        if(isDef) os.print("*");
                        os.println();
                    }
                }
                return;
            }
            case X86II.RawFrm:
            {
                // The accepted forms of Raw instructions are:
                //   1. nop     - No operand required
                //   2. jmp foo - PC relative displacement operand
                //   3. call bar - GlobalAddress Operand or External Symbol Operand
                assert mi.getNumOperands() == 0
                        || (mi.getNumOperands() == 1 &&
                        (mi.getOperand(0).isPCRelativeDisp()
                        || mi.getOperand(0).isGlobalAddress()
                        || mi.getOperand(0).isExternalSymbol()))
                        :"Illegal raw instruction!";
                os.print(instrInfo.getName(opcode));

                if (mi.getNumOperands() == 1)
                    printOp(mi.getOperand(0));
                os.println();
                return;
            }
            case X86II.AddRegFrm:
            {
                // There are currently two forms of acceptable AddRegFrm instructions.
                // 1.the instruction JUST takes a single register (like inc, dec, etc);
                // 2.it takes a register and an immediate of the same size as the register
                // (move immediate f.e.).

                // Note that this immediate value might be stored as
                // an Backend value, for example, loading the address of a global
                // into a register.  The initial register might be duplicated if this is a
                // M_2_ADDR_REG instruction.
                assert mi.getOperand(0).isRegister() && (mi.getNumOperands() == 1
                || (mi.getNumOperands() == 2 && (mi.getOperand(1).getReg() != 0
                || mi.getOperand(1).isImmediate()
                || mi.getOperand(1).isRegister() || mi.getOperand(1).isGlobalAddress()
                || mi.getOperand(1).isExternalSymbol())))
                        :"Illegal form for AddRegFrm instruction!";

                os.print(name);
                if (mi.getNumOperands() == 2 && (!mi.getOperand(1).isRegister()
                        || mi.getOperand(1).getReg() != 0
                        || mi.getOperand(1).isGlobalAddress()
                        || mi.getOperand(1).isExternalSymbol()))
                {
                    printOp(mi.getOperand(1));
                    os.print(", ");
                }
                printOp(mi.getOperand(0));
                //checkImplUses(desc);
                os.println();
                return;
            }
            case X86II.MRMDestReg:
            {
                // There are two acceptable forms of MRMDestReg instructions, those with 2,
                // 3 and 4 operands:
                //
                // 2 Operands: this is for things like mov that do not read a second input
                //
                // 3 Operands: in this form, the first two registers (the destination, and
                // the first operand) should be the same, post register allocation.  The 3rd
                // operand is an additional input.  This should be for things like add
                // instructions.
                //
                // 4 Operands: This form is for instructions which are 3 operands forms, but
                // have a constant argument as well.
                boolean isTwoAddr = instrInfo.isTwoAddrInstr(opcode);
                assert mi.getOperand(0).isRegister() &&
                        (mi.getNumOperands() == 2 || (isTwoAddr && mi.getOperand(1).isRegister()
                        && mi.getOperand(0).getReg() == mi.getOperand(1).getReg()
                        && (mi.getNumOperands() == 3 || (mi.getNumOperands() == 4 && mi.getOperand(3).isImmediate()))))
                    :"Bad format for MRMDest instruction!";
                os.print(name);

                printOp(mi.getOperand(1+ (isTwoAddr ? 1 : 0)));
                if (mi.getNumOperands() == 4)
                {
                    os.print(", ");
                    printOp(mi.getOperand(3));
                }
                os.print(", ");
                printOp(mi.getOperand(0));
                os.println();
                return;
            }
            case X86II.MRMDestMem:
            {
                // These instructions are the same as MRMDestReg, but instead of having a
                // register reference for the mod/rm field, it's a memory reference.
                assert isMem(mi, 0) && mi.getNumOperands() == 5 && mi.getOperand(4).isRegister()
                        :"Bad format for MRMDestMem";
                os.print(name);
                printOp(mi.getOperand(4));
                os.print(", ");
                printMemReference(mi, 0);
                os.println();
                return;
            }
            case X86II.MRMSrcReg:
            {
                // There are three forms that are acceptable for MRMSrcReg instructions,
                // those with 3 and 2 operands:
                //
                // 3 Operands: in this form, the last register (the second input) is the
                // ModR/M input.  The first two operands should be the same, post register
                // allocation.  This is for things like: add r32, r/m32
                //
                // 3 Operands: in this form, we can have 'INST R, R, imm', which is used for
                // instructions like the IMULri instructions.
                //
                // 2 Operands: this is for things like mov that do not read a second input
                //
                assert mi.getOperand(0).isRegister()
                        && mi.getOperand(1).isRegister()
                        && (mi.getNumOperands() == 2 ||
                        (mi.getNumOperands() == 3 &&
                                (mi.getOperand(2).isRegister()
                                || mi.getOperand(2).isImmediate())))
                        :"Bad format for MRMSrcReg!";

                if (mi.getNumOperands() == 3 && mi.getOperand(0).getReg()
                        != mi.getOperand(1).getReg())
                    os.println("**");

                os.print(name);
                printOp(mi.getOperand(mi.getNumOperands() - 1));
                os.print(", ");

                if (mi.getNumOperands() == 3 && mi.getOperand(2).isImmediate())
                {
                    printOp(mi.getOperand(1));
                    os.print(", ");
                }
                printOp(mi.getOperand(0));
                os.println();
                return;
            }
            case X86II.MRMSrcMem:
            {
                // These instructions are the same as MRMSrcReg, but instead of having a
                // register reference for the mod/rm field, it's a memory reference.
                //
                assert mi.getOperand(0).isRegister() &&
                        (mi.getNumOperands() == 5 && isMem(mi, 1))
                        || (mi.getNumOperands() == 6 && mi.getOperand(1).isRegister()
                && isMem(mi, 2)) : "Bad format for MRMSrcMMem";

                os.print(name);
                /**
                if (mi.getNumOperands() == 6 && mi.getOperand(0).getReg()
                        != mi.getOperand(1).getReg())
                {
                    os.print("**");
                }*/
                printMemReference(mi, mi.getNumOperands() - 4);
                os.print(", ");
                printOp(mi.getOperand(0));
                os.println();
                return;
            }
            case X86II.MRMS0r:
            case X86II.MRMS1r:
            case X86II.MRMS2r:
            case X86II.MRMS3r:
            case X86II.MRMS4r:
            case X86II.MRMS5r:
            case X86II.MRMS6r:
            case X86II.MRMS7r:
            {
                // In this form, the following are valid formats:
                //  1. sete r
                //  2. cmp reg, immediate
                //  2. shl rdest, rinput  <implicit CL or 1>
                //  3. sbb rdest, rinput, immediate   [rdest = rinput]

                assert mi.getNumOperands() > 0 && mi.getNumOperands() < 4
                        && mi.getOperand(0).isRegister() :"Bad MRMSxr format!";
                assert mi.getNumOperands() !=2 || mi.getOperand(1).isRegister()
                        || mi.getOperand(1).isImmediate() :"Badi MRMSxr format!";
                assert mi.getNumOperands() < 3 || (mi.getOperand(1).isRegister()
                && mi.getOperand(2).isImmediate()) :"Badi MRMSxr format!";
                /**
                if (mi.getNumOperands() > 1 && mi.getOperand(1).isRegister()
                        && mi.getOperand(0).getReg() != mi.getOperand(1).getReg())
                    os.print("**");
                 */
                os.print(name);
                //checkImplUses(desc);

                if (mi.getOperand(mi.getNumOperands() - 1).isImmediate())
                {
                    printOp(mi.getOperand(mi.getNumOperands() - 1));
                    os.print(", ");
                }

                printOp(mi.getOperand(0));
                os.println();
                return;
            }
            case X86II.MRMS0m:
            case X86II.MRMS1m:
            case X86II.MRMS2m:
            case X86II.MRMS3m:
            case X86II.MRMS4m:
            case X86II.MRMS5m:
            case X86II.MRMS6m:
            case X86II.MRMS7m:
            {
                // In this form, the following are valid formats:
                //  1. sete [m]
                //  2. cmp [m], immediate
                //  2. shl [m], rinput  <implicit CL or 1>
                //  3. sbb [m], immediate
                assert mi.getNumOperands()>=4 && mi.getNumOperands() <=5
                        && isMem(mi, 0) : "Bad MRMSxm format!";
                assert mi.getNumOperands() == 4 || mi.getOperand(4).isImmediate()
                        : "Bad MRMSxm format!";

                os.print(name);
                if (mi.getNumOperands() == 5)
                {
                    os.print(mi.getOperand(4));
                    os.print(", ");
                }
                printMemReference(mi, 0);
                os.println();
                return;
            }
        }
    }

    public static X86ATTAsmPrinter createX86AsmCodeEmitter(OutputStream os, TargetMachine tm)
    {
        return new X86ATTAsmPrinter(os, tm);
    }
}
