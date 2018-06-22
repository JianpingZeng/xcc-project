package backend.target.x86;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous zeng.
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
import backend.target.TargetData;
import backend.target.TargetMachine;
import backend.target.TargetRegisterInfo;
import backend.type.FunctionType;
import backend.type.Type;
import backend.value.*;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Util;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.TreeMap;

import static backend.support.AssemblyWriter.writeAsOperand;
import static backend.target.TargetMachine.RelocModel.PIC_;
import static backend.target.TargetMachine.RelocModel.Static;
import static backend.target.x86.X86GenRegisterNames.*;
import static backend.target.x86.X86MachineFunctionInfo.NameDecorationStyle.FastCall;
import static backend.target.x86.X86MachineFunctionInfo.NameDecorationStyle.StdCall;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class X86ATTAsmPrinter extends AsmPrinter
{
    /**
     * A statistic for indicating the numbeer of emitted machine
     * instruction by this asm printer.
     */
    public static final IntStatistic EmittedInsts = new IntStatistic(
            "EmittedInstrs", "Number of machine instrs printed");

    private TObjectIntHashMap<MachineBasicBlock> mbbNumber;
    private X86Subtarget subtarget;
    private HashMap<Function, X86MachineFunctionInfo> functionInfoMap;

    private TreeMap<String, String> gvStubs;
    private TreeMap<String, String> hiddenGVStubs;
    private TreeMap<String, String> fnStubs;

    public X86ATTAsmPrinter(OutputStream os, X86TargetMachine tm,
            TargetAsmInfo tai, boolean v)
    {
        super(os, tm, tai, v);
        mbbNumber = new TObjectIntHashMap<>();
        subtarget = tm.getSubtarget();
        functionInfoMap = new HashMap<>();
        gvStubs = new TreeMap<>();
        hiddenGVStubs = new TreeMap<>();
        fnStubs = new TreeMap<>();
    }

    @Override
    public String getPassName()
    {
        return "X86 AT&T style assembly printer";
    }

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.setPreservedAll();
        if (subtarget.isTargetDarwin() || subtarget.isTargetELF()
                || subtarget.isTargetCygMing())
        {
            au.addRequired(MachineModuleInfo.class);
        }
        // TODO dwarf writer.
        super.getAnalysisUsage(au);
    }

    @Override
    public boolean doFinalization(Module m)
    {
        if (subtarget.isTargetDarwin() || subtarget.isTargetCygMing())
        {
            assert false:"Currently, Darwin, Cygwin, MinGW not supported";
        }
        super.doFinalization(m);
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

    public void printf80mem(MachineInstr mi, int opNo)
    {
        printMemReference(mi, opNo);
    }

    public void printf128mem(MachineInstr mi, int opNo)
    {
        printMemReference(mi, opNo);
    }

    /**
     * This method used to print an immediate value that ends up being encoded
     * as a pc-relative value.
     * @param mi
     * @param opNo
     */
    public void print_pcrel_imm(MachineInstr mi, int opNo)
    {
        MachineOperand mo = mi.getOperand(opNo);
        switch(mo.getType())
        {
            default:
                Util.shouldNotReachHere("Unknown pcrel immediate operand");
            case MO_Immediate:
                os.print(mo.getImm());
                return;
            case MO_MachineBasicBlock:
                printBasicBlockLabel(mo.getMBB(), false, false, false);
                return;
            case MO_GlobalAddress:
            case MO_ExternalSymbol:
                printSymbolOperand(mo);
                return;
        }
    }

    /**
     * Print a raw symbol reference operand. THis handles jump tables, constant
     * pools, global address and external symbols, all of which part to a label
     * with various suffixes for relocation types etc.
     * @param mo
     */
    private void printSymbolOperand(MachineOperand mo)
    {
        switch (mo.getType())
        {
            default:
                Util.shouldNotReachHere("Unknown symbol type!");
            case MO_JumpTableIndex:
                os.printf("%sJIT%d_%d", tai.getPrivateGlobalPrefix(),
                        getFunctionNumber(), mo.getIndex());
                break;
            case MO_ConstantPoolIndex:
                os.printf("%sCPI%d_%d", tai.getPrivateGlobalPrefix(),
                        getFunctionNumber(), mo.getIndex());
                break;
            case MO_GlobalAddress:
                GlobalValue gv = mo.getGlobal();
                StringBuilder suffix = new StringBuilder();
                int ts = mo.getTargetFlags();
                if (ts == X86II.MO_DARWIN_STUB) suffix.append("$stub");
                else if (ts == X86II.MO_DARWIN_NONLAZY ||
                        ts == X86II.MO_DARWIN_NONLAZY_PIC_BASE ||
                        ts == X86II.MO_DARWIN_HIDDEN_NONLAZY ||
                        ts == X86II.MO_DARWIN_HIDDEN_NONLAZY_PIC_BASE)
                {
                    suffix.append("$non_lazy_ptr");
                }

                String name = mangler.getValueName(gv) + suffix.toString();
                if (subtarget.isTargetCygMing())
                    name = decorateName(name, gv);

                // handle DLLImport linkage
                if (ts ==X86II.MO_DLLIMPORT)
                    name = "__imp_" + name;
                if (ts == X86II.MO_DARWIN_NONLAZY || ts == X86II.MO_DARWIN_NONLAZY_PIC_BASE)
                    gvStubs.put(name, mangler.getValueName(gv));
                else if (ts == X86II.MO_DARWIN_HIDDEN_NONLAZY ||
                        ts == X86II.MO_DARWIN_HIDDEN_NONLAZY_PIC_BASE)
                    hiddenGVStubs.put(name, mangler.getValueName(gv));
                else if (ts == X86II.MO_DARWIN_STUB)
                    fnStubs.put(name, mangler.getValueName(gv));

                // If the name begins with a dollar-sign, enclose it in parens.  We do this
                // to avoid having it look like an integer immediate to the assembler.
                if (name.startsWith("$"))
                    os.printf("(%s)", name);
                else
                    os.print(name);

                printOffset(mo.getOffset());
                break;
            case MO_ExternalSymbol:
                name = mangler.makeNameProperly(mo.getSymbolName());
                if (mo.getTargetFlags() == X86II.MO_DARWIN_STUB)
                {
                    fnStubs.put(name + "$stub", name);
                    name += "$stub";
                }

                if (name.startsWith("$"))
                    os.printf("(%s)", name);
                else
                    os.print(name);
                break;
        }

        switch (mo.getTargetFlags())
        {
            default:
                Util.shouldNotReachHere("Unknown target flag on GlobalValue operand");
            case X86II.MO_NO_FLAG:
                break;
            case X86II.MO_DARWIN_NONLAZY:
            case X86II.MO_DARWIN_HIDDEN_NONLAZY:
            case X86II.MO_DLLIMPORT:
            case X86II.MO_DARWIN_STUB:
                // there affect the name of symbol, not any suffix.
                break;
            case X86II.MO_GOT_ABSOLUTE_ADDRESS:
                os.printf(" + [.-");
                printPICBaseSymbol();
                os.printf("]");
                break;
            case X86II.MO_PIC_BASE_OFFSET:
            case X86II.MO_DARWIN_NONLAZY_PIC_BASE:
            case X86II.MO_DARWIN_HIDDEN_NONLAZY_PIC_BASE:
                os.print('-');
                printPICBaseSymbol();
                break;
            case X86II.MO_TLSGD: os.print("@TLSGD"); break;
            case X86II.MO_GOTTPOFF: os.print("@GOTTPOFF"); break;
            case X86II.MO_INDNTPOFF: os.print("@INDNTPOFF"); break;
            case X86II.MO_TPOFF: os.print("@TPOFF"); break;
            case X86II.MO_NTPOFF: os.print("@NTPOFF"); break;
            case X86II.MO_GOTPCREL: os.print("@GOTPCREL"); break;
            case X86II.MO_GOT: os.print("@GOT"); break;
            case X86II.MO_GOTOFF: os.print("@GOTOFF"); break;
            case X86II.MO_PLT: os.print("@PLT"); break;
        }
    }

    private void printPICBaseSymbol()
    {
        if (subtarget.isTargetDarwin())
            os.printf("\"L%d$pb\"", getFunctionNumber());
        else
        {
            assert subtarget.isTargetELF():"Don't know how to print PIC label!";
            os.printf(".LXCC$%d.$piclabel", getFunctionNumber());
        }
    }

    public void printi128mem(MachineInstr mi, int opNo)
    {
        printMemReference(mi, opNo);
    }

    public void printlea64_32mem(MachineInstr mi, int opNo)
    {
        printMemReference(mi, opNo, "subreg64");
    }

    public void printPICLabel(MachineInstr mi, int opNo)
    {
        printMemReference(mi, opNo);
    }

    public void printlea64mem(MachineInstr mi, int opNo)
    {
        printMemReference(mi, opNo);
    }

    public void printlea32mem(MachineInstr mi, int opNo)
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
            case InternalLinkage:
                switchSection(".text", f);
                emitAlignment(fnAlign, f);
                break;
            case ExternalLinkage:
                switchSection(".text", f);
                emitAlignment(fnAlign, f);
                os.println("\t.globl\t" + curFnName);
                break;
            default:
                assert false : "Undefined linkage type!";
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
            os.padToColumn(tai.getCommentColumn());
            os.printf("%s ", tai.getCommentString());
            writeAsOperand(os, f, false, f.getParent());
        }

        os.println();
    }

    /**
     * This method is called used for print assembly code for each
     * machine instruction in {@code mf}.
     *
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
            os.println("\t.size " + curFnName + ", .-" + curFnName);

        // todo emitJumpTableInfo(mf.);

        // we didn't change anything.
        return false;
    }

    public void printOperand(MachineInstr mi, int opNo)
    {
        printOperand(mi, opNo, null);
    }

    public void printOperand(MachineInstr mi, int opNo, String modifier)
    {
        printOperand(mi, opNo, modifier, false);
    }

    /**
     * Theses methods are used by the tablegen created instruction printer.
     *
     * @param mi
     * @param opNo
     * @param modifier
     */
    public void printOperand(MachineInstr mi, int opNo, String modifier, boolean notRIPRel)
    {
        MachineOperand mo = mi.getOperand(opNo);
        TargetRegisterInfo regInfo = tm.getRegisterInfo();
        switch (mo.getType())
        {
            case MO_Register:
            {
                assert TargetRegisterInfo.isPhysicalRegister(
                        mo.getReg()) : "Virtual register should not make it this far!";
                os.print("%");
                int reg = mo.getReg();
                if (modifier != null && modifier.substring(0, 6).equals("subreg"))
                {
                    String bits = modifier.substring(6, 8);
                    int vt = bits.equals("64") ? MVT.i64 :
                            (bits.equals("32") ?
                                    MVT.i32 :
                                    (bits.equals("16") ? MVT.i16 : MVT.i8));
                    reg = getX86SubSuperRegister(reg, vt);
                }

                os.print(regInfo.getAsmName(reg).toLowerCase());
                return;
            }
            case MO_Immediate:
            {
                if (modifier == null || (!modifier.equals("debug") && !modifier.equals("mem")))
                    os.print("$");
                os.print(mo.getImm());
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
                boolean isCallOp = modifier != null && modifier.equals("call");
                boolean isMemOp = modifier != null && modifier.equals("mem");
                boolean needCloseParen = false;

                GlobalValue gv = mo.getGlobal();
                GlobalVariable gvar = gv instanceof GlobalVariable ?
                        (GlobalVariable) gv :
                        null;

                boolean isThreadLocal = gvar != null && gvar.isThreadLocal();
                String name = mangler.getValueName(gv);
                if (subtarget.isTargetCygMing())
                    name = decorateName(name, gv);

                if (!isCallOp && !isMemOp)
                    os.print("$");
                else if (name.charAt(0) == '$')
                {
                    os.print("(");
                    needCloseParen = true;
                }
                else
                    os.print(name);

                printOffset(mo.getOffset());

                if (isThreadLocal)
                {
                    if (tm.getRelocationModel() == PIC_ || subtarget.is64Bit())
                        os.printf("@TLSGD");
                    else
                    {
                        if (gv.isDeclaration())
                            os.printf("@INDNTPOFF");
                        else
                            os.printf("@NTPOFF");
                    }
                }
                else if (isMemOp)
                {
                    if (shouldPrintGOT(tm, subtarget))
                    {
                        os.printf("@GOTOFF");
                    }
                    else if (subtarget.isPICStyleRIPRel() && !notRIPRel &&
                            tm.getRelocationModel() != Static)
                    {
                        if (needCloseParen)
                        {
                            needCloseParen = false;
                            os.print(")");
                        }

                        os.print("(%rip)");
                    }
                }

                if (needCloseParen)
                    os.print(")");

                return;
            }
            case MO_ExternalSymbol:
            {
                boolean isCallOp = modifier != null && modifier.equals("call");
                boolean needCloseParen = false;
                String name = tai.getGlobalPrefix();
                name += mo.getSymbolName();

                if (!isCallOp)
                    os.print("$");
                else if (name.charAt(0) == '$')
                {
                    // The name begins with a dollar-sign. In order to avoid having it look
                    // like an integer immediate to the assembler, enclose it in parens.
                    os.print("(");
                    needCloseParen = true;
                }
                os.print(name);
                if (shouldPrintPLT(tm, subtarget))
                {
                    String gotName = tai.getGlobalPrefix();
                    gotName += "_GLOBAL_OFFSET_TABLE_";
                    if (gotName.equals(name))
                    {
                        // HACK! Emit extra offset to PC during printing GOT offset to
                        // compensate for the size of popl instruction. The resulting code
                        // should look like:
                        //   call .piclabel
                        // piclabel:
                        //   popl %some_register
                        //   addl $_GLOBAL_ADDRESS_TABLE_ + [.-piclabel], %some_register
                        os.printf(" + [.-%s]",
                                getPICLabelString(getFunctionNumber(), tai,
                                        subtarget));
                    }
                }
                if (needCloseParen)
                    os.print(")");
                if (!isCallOp && subtarget.isPICStyleRIPRel())
                    os.print("(%rip)");

                return;
            }
            case MO_JumpTableIndex:
            {
                boolean isMemOp = modifier != null && modifier.equals("mem");
                if (!isMemOp)
                    os.print("$");
                os.printf("%sJTI%d_%d", tai.getPrivateGlobalPrefix(),
                        getFunctionNumber(), mo.getIndex());

                if (tm.getRelocationModel() == PIC_)
                {
                    if (subtarget.isPICStyleStubPIC())
                    {
                        os.printf("-\"%s%d$pb\"", tai.getPrivateGlobalPrefix(),
                                getFunctionNumber());
                    }
                    else if (subtarget.isPICStyleGOT())
                        os.printf("@GOTOFF");
                }

                if (isMemOp && subtarget.isPICStyleRIPRel() && !notRIPRel)
                    os.print("(%rip)");
                return;
            }
            case MO_ConstantPoolIndex:
            {
                boolean isMemOp = modifier != null && modifier.equals("mem");
                if (!isMemOp)
                    os.print("$");
                os.printf("%sCPI%d_%d", tai.getPrivateGlobalPrefix(),
                        getFunctionNumber(), mo.getIndex());

                if (tm.getRelocationModel() == PIC_)
                {
                    if (subtarget.isPICStyleStubPIC())
                    {
                        os.printf("-\"%s%d$pb\"", tai.getPrivateGlobalPrefix(),
                                getFunctionNumber());
                    }
                    else if (subtarget.isPICStyleGOT())
                        os.printf("@GOTOFF");
                }

                printOffset(mo.getOffset());
                if (isMemOp && subtarget.isPICStyleRIPRel() && !notRIPRel)
                    os.print("(%rip)");
                return;
            }
            default:
                os.print("<unknown operand type>");
        }
    }

    private void printOffset(long offset)
    {
        if (offset > 0)
            os.printf("+%d", offset);
        else if (offset < 0)
            os.print(offset);
    }

    private static boolean shouldPrintGOT(TargetMachine tm, X86Subtarget subtarget)
    {
        return subtarget.isPICStyleGOT() && tm.getRelocationModel() == PIC_;
    }

    private static X86MachineFunctionInfo calculateFunctionInfo(
            Function f,
            TargetData td)
    {
        X86MachineFunctionInfo info = new X86MachineFunctionInfo();
        long size = 0;

        switch (f.getCallingConv())
        {
            case X86_StdCall:
                info.setDecorationStyle(StdCall);
                break;
            case X86_FastCall:
                info.setDecorationStyle(FastCall);
                break;
            default:
                return info;
        }

        int argNum = 1;
        for (Value arg : f.getArgumentList())
        {
            Type ty = arg.getType();
            size += ((td.getTypePaddedSize(ty) + 3)/4)*4;
        }

        info.setBytesToPopOnReturn((int) size);
        return info;
    }

    private String decorateName(String name, GlobalValue gv)
    {
        if (!(gv instanceof Function))
            return name;

        Function f = (Function)gv;
        CallingConv cc = f.getCallingConv();
        if (cc != CallingConv.X86_StdCall && cc != CallingConv.X86_FastCall)
            return name;

        if (!subtarget.isTargetCygMing())
            return name;

        X86MachineFunctionInfo fi = null;
        if (!functionInfoMap.containsKey(f))
        {
            functionInfoMap.put(f, calculateFunctionInfo(f, tm.getTargetData()));
            fi = functionInfoMap.get(f);
        }
        else
        {
            fi = functionInfoMap.get(f);
        }

        FunctionType fy = f.getFunctionType();
        switch(fi.getDecorationStyle())
        {
            case None:
            case StdCall:
                break;
            case FastCall:
                if (name.charAt(0) == '_')
                    name = '@' + name.substring(1);
                else
                    name = '@' + name;
                break;
            default:
                assert false:"Unsupported DecorationStyle";
        }
        return name;
    }

    private static boolean shouldPrintStub(TargetMachine tm, X86Subtarget subtarget)
    {
        return subtarget.isPICStyleStubPIC() && tm.getRelocationModel() != Static;
    }

    private static String getPICLabelString(int fnNumber,
            TargetAsmInfo tai,
            X86Subtarget subtarget)
    {
        StringBuilder label = new StringBuilder();
        if (subtarget.isTargetDarwin())
            label.append("\"L").append(fnNumber).append("$pb\"");
        else if (subtarget.isTargetELF())
            label.append(".Lllvm$").append(fnNumber).append(".$piclabel");
        else
            assert false:"Don't known how to print PIC label!\n";
        return label.toString();
    }

    private static boolean shouldPrintPLT(TargetMachine tm, X86Subtarget subtarget)
    {
        return subtarget.isTargetELF() && tm.getRelocationModel() == PIC_ &&
                (subtarget.isPICStyleRIPRel() || subtarget.isPICStyleGOT());
    }

    private static int getX86SubSuperRegister(int reg, int vt)
    {
        return getX86SubSuperRegister(reg, vt, false);
    }

    /**
     * X86 utility function. It returns the sub or super register of a
     * specific X86 register.
     * <p>
     * The returned may be sub-register or super-register of the specified X86
     * register {@code reg} and it is just suitable to fit the bit width of MVT
     * {@code vt}.
     * e.g. getX86SubSuperRegister(X86GenInstrNames.EAX, MVT.i16) return AX.
     * </p>
     * @param reg
     * @param vt
     * @param high  default to false.
     * @return
     */
    private static int getX86SubSuperRegister(int reg, int vt, boolean high)
    {
        switch (vt)
        {
            default:
                return reg;
            case MVT.i8:
                if (high)
                {
                    switch (reg)
                    {
                        default:
                            return 0;
                        case AH:
                        case AL:
                        case AX:
                        case EAX:
                        case RAX:
                            return AH;
                        case DH:
                        case DL:
                        case DX:
                        case EDX:
                        case RDX:
                            return DH;
                        case CH:
                        case CL:
                        case CX:
                        case ECX:
                        case RCX:
                            return CH;
                        case BH:
                        case BL:
                        case BX:
                        case EBX:
                        case RBX:
                            return BH;
                    }
                }
                else
                {
                    switch (reg)
                    {
                        default:
                            return 0;
                        case AH:
                        case AL:
                        case AX:
                        case EAX:
                        case RAX:
                            return AL;
                        case DH:
                        case DL:
                        case DX:
                        case EDX:
                        case RDX:
                            return DL;
                        case CH:
                        case CL:
                        case CX:
                        case ECX:
                        case RCX:
                            return CL;
                        case BH:
                        case BL:
                        case BX:
                        case EBX:
                        case RBX:
                            return BL;
                        case SIL:
                        case SI:
                        case ESI:
                        case RSI:
                            return SIL;
                        case DIL:
                        case DI:
                        case EDI:
                        case RDI:
                            return DIL;
                        case BPL:
                        case BP:
                        case EBP:
                        case RBP:
                            return BPL;
                        case SPL:
                        case SP:
                        case ESP:
                        case RSP:
                            return SPL;
                        case R8B:
                        case R8W:
                        case R8D:
                        case R8:
                            return R8B;
                        case R9B:
                        case R9W:
                        case R9D:
                        case R9:
                            return R9B;
                        case R10B:
                        case R10W:
                        case R10D:
                        case R10:
                            return R10B;
                        case R11B:
                        case R11W:
                        case R11D:
                        case R11:
                            return R11B;
                        case R12B:
                        case R12W:
                        case R12D:
                        case R12:
                            return R12B;
                        case R13B:
                        case R13W:
                        case R13D:
                        case R13:
                            return R13B;
                        case R14B:
                        case R14W:
                        case R14D:
                        case R14:
                            return R14B;
                        case R15B:
                        case R15W:
                        case R15D:
                        case R15:
                            return R15B;
                    }
                }
            case MVT.i16:
                switch (reg)
                {
                    default:
                        return reg;
                    case AH:
                    case AL:
                    case AX:
                    case EAX:
                    case RAX:
                        return AX;
                    case DH:
                    case DL:
                    case DX:
                    case EDX:
                    case RDX:
                        return DX;
                    case CH:
                    case CL:
                    case CX:
                    case ECX:
                    case RCX:
                        return CX;
                    case BH:
                    case BL:
                    case BX:
                    case EBX:
                    case RBX:
                        return BX;
                    case SIL:
                    case SI:
                    case ESI:
                    case RSI:
                        return SI;
                    case DIL:
                    case DI:
                    case EDI:
                    case RDI:
                        return DI;
                    case BPL:
                    case BP:
                    case EBP:
                    case RBP:
                        return BP;
                    case SPL:
                    case SP:
                    case ESP:
                    case RSP:
                        return SP;
                    case R8B:
                    case R8W:
                    case R8D:
                    case R8:
                        return R8W;
                    case R9B:
                    case R9W:
                    case R9D:
                    case R9:
                        return R9W;
                    case R10B:
                    case R10W:
                    case R10D:
                    case R10:
                        return R10W;
                    case R11B:
                    case R11W:
                    case R11D:
                    case R11:
                        return R11W;
                    case R12B:
                    case R12W:
                    case R12D:
                    case R12:
                        return R12W;
                    case R13B:
                    case R13W:
                    case R13D:
                    case R13:
                        return R13W;
                    case R14B:
                    case R14W:
                    case R14D:
                    case R14:
                        return R14W;
                    case R15B:
                    case R15W:
                    case R15D:
                    case R15:
                        return R15W;
                }
            case MVT.i32:
                switch (reg)
                {
                    default:
                        return reg;
                    case AH:
                    case AL:
                    case AX:
                    case EAX:
                    case RAX:
                        return EAX;
                    case DH:
                    case DL:
                    case DX:
                    case EDX:
                    case RDX:
                        return EDX;
                    case CH:
                    case CL:
                    case CX:
                    case ECX:
                    case RCX:
                        return ECX;
                    case BH:
                    case BL:
                    case BX:
                    case EBX:
                    case RBX:
                        return EBX;
                    case SIL:
                    case SI:
                    case ESI:
                    case RSI:
                        return ESI;
                    case DIL:
                    case DI:
                    case EDI:
                    case RDI:
                        return EDI;
                    case BPL:
                    case BP:
                    case EBP:
                    case RBP:
                        return EBP;
                    case SPL:
                    case SP:
                    case ESP:
                    case RSP:
                        return ESP;
                    case R8B:
                    case R8W:
                    case R8D:
                    case R8:
                        return R8D;
                    case R9B:
                    case R9W:
                    case R9D:
                    case R9:
                        return R9D;
                    case R10B:
                    case R10W:
                    case R10D:
                    case R10:
                        return R10D;
                    case R11B:
                    case R11W:
                    case R11D:
                    case R11:
                        return R11D;
                    case R12B:
                    case R12W:
                    case R12D:
                    case R12:
                        return R12D;
                    case R13B:
                    case R13W:
                    case R13D:
                    case R13:
                        return R13D;
                    case R14B:
                    case R14W:
                    case R14D:
                    case R14:
                        return R14D;
                    case R15B:
                    case R15W:
                    case R15D:
                    case R15:
                        return R15D;
                }
            case MVT.i64:
                switch (reg)
                {
                    default:
                        return reg;
                    case AH:
                    case AL:
                    case AX:
                    case EAX:
                    case RAX:
                        return RAX;
                    case DH:
                    case DL:
                    case DX:
                    case EDX:
                    case RDX:
                        return RDX;
                    case CH:
                    case CL:
                    case CX:
                    case ECX:
                    case RCX:
                        return RCX;
                    case BH:
                    case BL:
                    case BX:
                    case EBX:
                    case RBX:
                        return RBX;
                    case SIL:
                    case SI:
                    case ESI:
                    case RSI:
                        return RSI;
                    case DIL:
                    case DI:
                    case EDI:
                    case RDI:
                        return RDI;
                    case BPL:
                    case BP:
                    case EBP:
                    case RBP:
                        return RBP;
                    case SPL:
                    case SP:
                    case ESP:
                    case RSP:
                        return RSP;
                    case R8B:
                    case R8W:
                    case R8D:
                    case R8:
                        return R8;
                    case R9B:
                    case R9W:
                    case R9D:
                    case R9:
                        return R9;
                    case R10B:
                    case R10W:
                    case R10D:
                    case R10:
                        return R10;
                    case R11B:
                    case R11W:
                    case R11D:
                    case R11:
                        return R11;
                    case R12B:
                    case R12W:
                    case R12D:
                    case R12:
                        return R12;
                    case R13B:
                    case R13W:
                    case R13D:
                    case R13:
                        return R13;
                    case R14B:
                    case R14W:
                    case R14D:
                    case R14:
                        return R14;
                    case R15B:
                    case R15W:
                    case R15D:
                    case R15:
                        return R15;
                }
        }
    }

    private void printMemReference(MachineInstr mi, int opNo)
    {
        printMemReference(mi, opNo, null);
    }

    private void printMemReference(MachineInstr mi, int opNo, String modifier)
    {
        assert isMem(mi, opNo) : "Invalid memory reference!";

        MachineOperand baseReg = mi.getOperand(opNo);
        int scaleVal = (int) mi.getOperand(opNo + 1).getImm();
        MachineOperand indexReg = mi.getOperand(opNo + 2);
        MachineOperand disp = mi.getOperand(opNo + 3);

        boolean notRIPRel = indexReg.getReg()!=0 || baseReg.getReg()!=0;
        if (disp.isGlobalAddress() ||
                disp.isConstantPoolIndex() ||
                disp.isJumpTableIndex())
        {
            printOperand(mi, opNo+3, "mem", notRIPRel);
        }
        else
        {
            long dispVal = disp.getImm();
            if (dispVal != 0 || (indexReg.getReg() == 0 && baseReg.getReg() == 0))
                os.print(dispVal);
        }

        if (indexReg.getReg() != 0 || baseReg.getReg() != 0)
        {
            int baseRegOperand = 0, indexRegOperand = 2;
            if (indexReg.getReg() == ESP || indexReg.getReg() == RSP)
            {
                assert scaleVal == 1 :"Scale not supported for stack pointer!";
                MachineOperand o = baseReg;
                baseReg = indexReg;
                indexReg = o;

                // swap baseRegOperand and indexRegOperand.
                baseRegOperand = baseRegOperand ^ indexRegOperand;
                indexRegOperand = baseRegOperand ^ indexRegOperand;
                baseRegOperand = baseRegOperand ^ indexRegOperand;
            }

            os.print("(");
            if (baseReg.getReg() != 0)
                // Must add a offset(opNo) on baseRegOperand
                printOperand(mi, baseRegOperand + opNo, modifier);
            if (indexReg.getReg() != 0)
            {
                os.printf(",");
                printOperand(mi, indexRegOperand + opNo, modifier);
                if (scaleVal != 1)
                    os.printf(",%d", scaleVal);
            }
            os.print(")");
        }
    }

    public void printSSECC(MachineInstr mi, int op)
    {
        int value = (int)mi.getOperand(op).getImm();
        assert value <= 7:"Invalid ssecc argument!";
        switch (value)
        {
            case 0: os.print("eq"); break;
            case 1: os.print("lt"); break;
            case 2: os.print("le"); break;
            case 3: os.print("unord"); break;
            case 4: os.print("neq"); break;
            case 5: os.print("nlt"); break;
            case 6: os.print("nle"); break;
            case 7: os.print("ord"); break;
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
        //if (!BackendCmdOptions.NewAsmPrinter.value)
        {
            // Call the autogenerated instruction printer routines.
            os.print('\t');
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
