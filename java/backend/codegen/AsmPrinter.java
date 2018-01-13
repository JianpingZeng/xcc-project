package backend.codegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous
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

import backend.MC.MCContext;
import backend.MC.MCStreamer;
import backend.analysis.MachineLoop;
import backend.analysis.MachineLoopInfo;
import backend.pass.AnalysisUsage;
import backend.support.BackendCmdOptions;
import backend.support.FormattedOutputStream;
import backend.support.LLVMContext;
import backend.support.NameMangler;
import backend.target.*;
import backend.type.Type;
import backend.value.*;
import backend.value.Value.UndefValue;
import tools.TextUtils;
import tools.Util;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Objects;

import static backend.MC.MCStreamer.createAsmStreamer;
import static backend.support.AssemblyWriter.writeAsOperand;
import static backend.target.TargetMachine.RelocModel.Static;
import static backend.value.GlobalValue.VisibilityTypes.HiddenVisibility;
import static backend.value.GlobalValue.VisibilityTypes.ProtectedVisibility;
import static tools.Util.doubleToBits;
import static tools.Util.floatToBits;

/**
 * This is a common base class be used for target-specific asmwriters. This
 * class just primarily tack care of printing global constants, which is printed
 * in a very similar way across different target.
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class AsmPrinter extends MachineFunctionPass
{
    /**
     * The current section asmName where we are emitting to.
     */
    private String currentSection;

    private Section currentSection_;

    private boolean isInTextSection;
    /**
     * This provides a unique ID for the compiling machine function in the
     * current translation unit.
     *
     * It is automatically increased by calling method {}
     */
    private int functionNumber;

    /**
     * The output stream on which the assembly code will be emitted.
     */
    protected FormattedOutputStream os;
    /**
     * The target machine text.
     */
    protected TargetMachine tm;

    protected TargetAsmInfo tai;

    protected TargetRegisterInfo tri;

    protected boolean verboseAsm;

    protected MachineLoop li;
    /**
     * A asmName mangler for performing necessary mangling on global linkage entity.
     */
    protected NameMangler mangler;
    /**
     * The asmName of current being processed machine function.
     */
    protected String curFnName;

    protected MCContext outContext;

    protected MCStreamer outStreamer;
    private TargetSubtarget subtarget;

    public enum BoolOrDefault
    {
        BOU_UNSET,
        BOU_TRUE,
        BOU_FALSE
    }

    protected AsmPrinter(OutputStream os, TargetMachine tm, TargetAsmInfo tai, boolean v)
    {
        functionNumber = 0;
        this.os = new FormattedOutputStream(os);
        this.tm = tm;
        this.tai = tai;
        this.tri = tm.getRegisterInfo();
        subtarget = tm.getSubtarget();

        outContext = new MCContext();
        outStreamer = createAsmStreamer(outContext, this.os, tai, this);
        switch (BackendCmdOptions.AsmVerbose.value)
        {
            case BOU_UNSET:
                verboseAsm = v;
                break;
            case BOU_TRUE:
                verboseAsm = true;
                break;
            case BOU_FALSE:
                verboseAsm = false;
                break;
        }
    }

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        if (verboseAsm)
            au.addRequired(MachineLoop.class);
        super.getAnalysisUsage(au);
    }

    @Override
    public boolean doInitialization(Module m)
    {
        mangler = new NameMangler(m);

        if (tai.doesAllowQuotesInName())
            mangler.setUseQuotes(true);
        if (tai.hasSingleParameterDotFile())
        {
            os.printf("\t.file\t\"%s\"%n", m.getModuleIdentifier());
        }
        return false;
    }

    @Override
    public boolean doFinalization(Module m)
    {
        // Emit global variables.
        for (GlobalVariable gv : m.getGlobalVariableList())
        {
            printGlobalVariable(gv);
        }
        // TODO Emit debug information

        // If the target wants to know about weak references, print them all.
        if (tai.getWeakDefDirective() != null)
        {
            return false;
        }
        if (tai.getSetDirective() != null)
            os.println();

        return false;
    }

    public void printGlobalVariable(GlobalVariable gv)
    {
        TargetData td = tm.getTargetData();

        // External global require no code
        if (!gv.hasInitializer())
            return;

        // Check to see if this is a special global used by LLVM, if so, emit it.
        if (emitSpecialLLVMGlobal(gv))
        {
            if (subtarget.isTargetDarwin() && tm.getRelocationModel() == Static)
            {
                if (gv.getName().equals("llvm.global_ctors"))
                    os.printf(".reference .constructors_used\n");
                else if (gv.getName().equals("llvm.global_dtors"))
                    os.printf(".reference .destructors_used\n");
            }
            return;
        }

        String name = mangler.getValueName(gv);
        Constant c = gv.getInitializer();
        Type ty = c.getType();
        long size = td.getTypePaddedSize(ty);
        long align = td.getPrefTypeAlignment(ty);

        printVisibility(name, gv.getVisibility());
        if (subtarget.isTargetELF())
            os.printf("\t.type\t%s,@object\n", name);

        switchSection(tai.getSectionForGlobal(gv));

        if (c.isNullValue() && !gv.hasSection())
        {
            if (gv.hasExternalLinkage())
            {
                String directive = tai.getZeroFillDirective();
                if (directive != null)
                {
                    os.printf("\t.globl %s\n", name);
                    os.printf("%s__DATA, __common, %s, %d, %d\n", directive,
                            name, size, align);
                    return;
                }
            }

            if (!gv.isThreadLocal() && (gv.hasLocalLinkage()))
            {
                if (size == 0)
                    size = 1;

                if (tai.getLCOMMDirective() != null)
                {
                    if (gv.hasLocalLinkage())
                    {
                        os.printf("%s%s,%d", tai.getLCOMMDirective(), name, size);
                        if (subtarget.isTargetDarwin())
                            os.printf(",%d", align);
                    }
                    else
                    {
                        os.printf("%s%s,%d", tai.getCOMMDirective(), name, size);
                        if (tai.getCOMMDirectiveTakesAlignment())
                            os.printf(",%d", tai.getAlignmentIsInBytes() ?
                                    (1 << align) :
                                    align);
                    }
                }
                else
                {
                    if (!subtarget.isTargetCygMing())
                    {
                        if (gv.hasLocalLinkage())
                            os.printf("\t.local\t%s\n", name);
                    }

                    os.printf("%s%s,%d", tai.getCOMMDirective(), name, size);
                    if (tai.getCOMMDirectiveTakesAlignment())
                        os.printf(",%d", tai.getAlignmentIsInBytes() ?
                                (1 << align) :
                                align);
                }
                if (verboseAsm)
                {
                    os.padToColumn(tai.getCommentColumn());
                    os.printf("%s ", tai.getCommentString());
                }
                os.println();
                return;
            }
        }

        switch (gv.getLinkage())
        {
            case ExternalLinkage:
                os.printf("\t.globl %s\n", name);
            case PrivateLinkage:
            case InteralLinkage:
                break;
            default:
                assert false : "Unknown linkage type!";
        }

        emitAlignment((int) align, gv);
        os.printf("%s:", name);
        if (verboseAsm)
        {
            os.padToColumn(tai.getCommentColumn());
            os.printf("%s ", tai.getCommentString());
            writeAsOperand(os, gv, false, gv.getParent());
        }
        os.println();
        emitGlobalConstant(c);

        if (tai.hasDotTypeDotSizeDirective())
            os.printf("\t.size\t%s, %d\n", name, size);
    }

    public static boolean isScale(MachineOperand mo)
    {
        long imm;
        return mo.isImm() && (
                ((imm = mo.getImm()) & (imm - 1)) == 0)
                && imm >= 1 && imm <= 8;
    }

    /**
     * Memory operand is like this: baseReg + scale*indexReg+ disp.
     * @param mi
     * @param opNo
     * @return
     */
    public static boolean isMem(MachineInstr mi, int opNo)
    {
        if (mi.getOperand(opNo).isFrameIndex()) return true;
        return mi.getNumOperands() >= opNo + 4 &&
                mi.getOperand(opNo).isRegister()
                && isScale(mi.getOperand(opNo + 1))
                && mi.getOperand(opNo + 2).isRegister()
                && (mi.getOperand(opNo + 3).isImm()
                || mi.getOperand(opNo + 3).isGlobalAddress()
                || mi.getOperand(opNo + 3).isConstantPoolIndex());
    }

    /**
     * This should be called when a new machine function is being processed when
     * running method runOnMachineFunction();
     */
    protected void setupMachineFunction(MachineFunction mf)
    {
        curFnName = mangler.getValueName(mf.getFunction());
        incrementFnNumeber();

        if (verboseAsm)
            li = (MachineLoop) getAnalysisToUpDate(MachineLoop.class);
    }

    protected int getFunctionNumber() {return functionNumber;}

    protected void incrementFnNumeber() {functionNumber++;}

    /**
     * Print to the current output stream assembly
     * representations of the constants in the constant pool MCP. This is
     * used to print out constants which have been "spilled to memory" by
     * the code generator.
     * @param mcp
     */
    protected void emitConstantPool(MachineConstantPool mcp)
    {
        ArrayList<MachineConstantPoolEntry> consts = mcp.getConstants();
        if (consts.isEmpty()) return;

        TargetData td = tm.getTargetData();
        switchSection(tai.getConstantPoolSectionDirective(), null);
        int align = mcp.getContantPoolAlignment();
        emitAlignment(align, null);
        long offset = 0;
        for (int i = 0, e = consts.size(); i < e; i++)
        {
            // Emit inter-object padding for alignment.
            int alignMask = align - 1;
            long newOffset = (offset + alignMask) & ~(alignMask);
            emitZero(newOffset - offset);

            long entSize = td.getTypeSize(consts.get(i).getType());
            offset = newOffset + entSize;

            os.printf("%sCPI%d_%d:",
                    tai.getPrivateGlobalPrefix(),
                    getFunctionNumber(), i);
            if (verboseAsm)
            {
                os.print(Util.fixedLengthString(tai.getCommentColumn(), ' '));
                os.printf("%s constant ", tai.getCommentString());
                writeTypeSymbol(os, consts.get(i).getType(), null);
            }
            os.println();
            if (consts.get(i).isMachineConstantPoolEntry())
            {
                emitMachineConstantPoolValue(consts.get(i).getValueAsCPV());
            }
            else
            {
                Constant c = consts.get(i).getValueAsConstant();
                emitGlobalConstant(c);
            }
        }
    }

    private void emitMachineConstantPoolValue(MachineConstantPoolValue cpv)
    {
        Util.shouldNotReachHere("Target does not support EmitMachineConstantPoolValue");
    }

    public static FormattedOutputStream writeTypeSymbol(FormattedOutputStream os, Type ty, Module m)
    {
        os.print(" ");
        if (m != null)
        {
            return os;
        }
        else
        {
            os.print(ty.getDescription());
            return os;
        }
    }

    protected void emitAlignment(int numBits)
    {
        emitAlignment(numBits, null);
    }

    /**
     * Emits a alignment directive to the specified power of two.
     * @param numBits
     * @param gv
     */
    protected void emitAlignment(int numBits, GlobalValue gv)
    {
        if (gv != null && gv.getAlignment() != 0)
            numBits = Util.log2(gv.getAlignment());
        if (numBits == 0) return;

        boolean alignIsInByte = tai.isAlignIsInByte();
        if (alignIsInByte) numBits = 1 << numBits;
        os.println(tai.getAlignDirective() + (1<<numBits));
    }

    /**
     * Emits a block of zeros.
     * @param numZeros
     */
    protected void emitZero(long numZeros)
    {
        if (numZeros != 0)
        {
            String zeroDirective = tai.getZeroDirective();
            if (zeroDirective != null)
                os.println(zeroDirective + numZeros);
            else
            {
                String data8BitDirective = tai.getData8bitsDirective();
                for (; numZeros != 0; numZeros--)
                    os.print(data8BitDirective + "0\n");
            }
        }
    }

    /**
     * Prints a general Backend constant into .s file.
     * @param c
     */
    protected void emitGlobalConstant(Constant c)
    {
        TargetData td = tm.getTargetData();
        String data64BitDirective = tai.getData64bitsDirective();
        String data32BitDirective = tai.getData32bitsDirective();
        String data8BitDirective = tai.getData8bitsDirective();
        String data16BitDirective = tai.getData16bitsDirective();
        String commentString = tai.getCommentString();

        if (c.isNullValue() || c instanceof UndefValue)
        {
            emitZero(td.getTypeSize(c.getType()));
            return;
        }
        else if (c instanceof ConstantArray)
        {
            ConstantArray ca = (ConstantArray)c;
            if (ca.isString())
            {
                int numElts = ca.getNumOfOperands();
                String ascizDirective = tai.getAscizDirective();
                if (ascizDirective != null && numElts != 0
                        && ((ConstantInt)ca.operand(numElts-1)).getZExtValue() == 0)
                {
                    os.print(ascizDirective);
                    printAsCString(os, ca, numElts-1);
                }
                else
                {
                    os.print(tai.getAsciiDirective());
                    printAsCString(os, ca, numElts);
                }
                os.println();
            }
            else
            {
                // Not a string.
                for (int i = 0, e = ca.getNumOfOperands(); i < e; i++)
                {
                    emitGlobalConstant(ca.operand(i));
                }
            }
            return;
        }
        else if (c instanceof ConstantStruct)
        {
            ConstantStruct cas = (ConstantStruct)c;
            TargetData.StructLayout layout = td.getStructLayout(cas.getType());
            int sizeSoFar = 0;
            for (int i = 0, e = cas.getNumOfOperands(); i < e; i++)
            {
                Constant field = cas.operand(i);

                //Checking if padding is needed and insert one or more zeros.
                long fieldSize = td.getTypeSize(field.getType());
                long padSize = (i == e -1 ? layout.structSize
                        : layout.memberOffsets.get(i+1) -
                        layout.memberOffsets.get(i)) - fieldSize;
                sizeSoFar += fieldSize + padSize;

                emitGlobalConstant(field);
                emitZero(padSize);
            }
            assert sizeSoFar == layout.structSize :
                    "layout of constant struct may be incorrect!";
            return;
        }
        else if (c instanceof ConstantFP)
        {
            // FP Constants are printed as integer constants to avoid losing
            // precision.
            ConstantFP fp = (ConstantFP)c;
            if (fp.getType() == LLVMContext.DoubleTy)
            {
                double val = fp.getValueAPF().convertToDouble();
                if (data64BitDirective != null)
                {
                    os.println(data64BitDirective + doubleToBits(val)
                    + "\t" + commentString + "double value: " + val);
                }
                else if (td.isLittleEndian())
                {
                    os.print(data32BitDirective + (int)(doubleToBits(val)));
                    os.print("\t" + commentString + "double least significant word");
                    os.println(val);

                    os.print(data32BitDirective + (int)(doubleToBits(val) >> 32));
                    os.print("\t" + commentString + "double most significant word");
                    os.println(val);
                }
                else
                {
                    // big endian.
                    os.print(data32BitDirective + (int)(doubleToBits(val) >> 32));
                    os.print("\t" + commentString + "double most significant word");
                    os.println(val);

                    os.print(data32BitDirective + (int)doubleToBits(val));
                    os.print("\t" + commentString + "double least significant word");
                    os.println(val);

                }
                return;
            }
            else
            {
                float val = fp.getValueAPF().convertToFloat();
                os.print(data32BitDirective + floatToBits((float) val));
                os.println("\t" + commentString + " float " + val);
                return;
            }
        }
        else if (c.getType() == LLVMContext.Int64Ty)
        {
            if (c instanceof ConstantInt)
            {
                ConstantInt ci = (ConstantInt)c;
                long val = ci.getZExtValue();

                if (data64BitDirective != null)
                    os.println(data64BitDirective + val);
                else if (td.isLittleEndian())
                {
                    os.print(data32BitDirective + (int)val);
                    os.println("\t" + commentString + "Double-word least significant word" + val);

                    os.print(data32BitDirective + (int)(val >> 32));
                    os.println("\t" + commentString + "Double-world most significant word" + val);
                }
                else
                {
                    os.print(data32BitDirective + (int)(val >> 32));
                    os.println("\t" + commentString + "Double-world most significant word" + val);

                    os.print(data32BitDirective + (int)val);
                    os.println("\t" + commentString + "Double-word least significant word" + val);
                }
                return;
            }
        }

        Type type = c.getType();
        switch (type.getTypeID())
        {
            case Type.IntegerTyID:
                int sz = (int)td.getTypeSizeInBits(type);
                switch(sz)
                {
                    case 8:
                        os.print(data8BitDirective);
                        break;
                    case 16:
                        os.print(data16BitDirective);
                        break;
                    case 32:
                        os.print(data32BitDirective);
                        break;
                    case 64:
                        assert data64BitDirective != null:"Target cannot handle 64-bit constant!";
                        os.print(data64BitDirective);
                        break;
                    default:
                        assert false:"Undefined integral type";
                }
                break;
            case Type.PointerTyID:
                if (td.getPointerMemSize() == 8)
                {
                    os.print(data64BitDirective);
                    break;
                }
                break;
            case Type.FloatTyID:
            case Type.DoubleTyID:
                assert false:"Should have already been handled above!";
                break;
        }
        emitConstantValueOnly(c);
        os.println();
    }

    /**
     * Print out the specified constant, without a storage class.  Only the
     * constants valid in constant expressions can occur here.
     * @param c
     */
    private void emitConstantValueOnly(Constant c)
    {
        if (c.isNullValue() || c instanceof UndefValue)
            os.print('0');
        else if (c instanceof ConstantInt)
        {
            ConstantInt ci = (ConstantInt)c;
            os.print(ci.getZExtValue());
        }
        else if (c instanceof GlobalValue)
        {
            GlobalValue gv = (GlobalValue)c;
            if (gv instanceof Function)
            {
                String functionAddrPrefix = tai.getFunctionAddrPrefix();
                String functionAddrSuffix = tai.getFunctionAddrSuffix();
                os.print(functionAddrPrefix + mangler.getValueName(gv));
                os.print(functionAddrSuffix);
            }
            else
            {
                String globalVarAddrPrefix = tai.getGlobalVarAddrPrefix();
                String globalVarAddrSuffix = tai.getGlobalVarAddrSuffix();
                os.print(globalVarAddrPrefix + mangler.getValueName(gv));
                os.print(globalVarAddrSuffix);
            }
        }
        else if (c instanceof ConstantExpr)
        {
            ConstantExpr ce = (ConstantExpr)c;
            TargetData td = tm.getTargetData();
            switch (ce.getOpcode())
            {
                case GetElementPtr:
                {
                    // generate a symbolic expression for the byte address
                    Constant basePtr = ce.operand(0);

                }
                case Add:
                case FAdd:
                {
                    os.print("(");
                    emitConstantValueOnly(ce.operand(0));
                    os.print(") + (");
                    emitConstantValueOnly(ce.operand(1));
                    os.print(")");
                    break;
                }
                default:
                    assert false:"Unsupported operator!";
            }
        }
        else
            assert false:"Unknown constant expression!";
    }

    private static char toOctal(int x)
    {
        return (char)(x&0x7 + '0');
    }

    /**
     * Print the specified array as a C compatible string, only if
     * the predicate isString is true.
     * @param os
     * @param ca
     * @param lastIndex
     */
    private void printAsCString(FormattedOutputStream os, ConstantArray ca, int lastIndex)
    {
        assert ca.isString() :"Array is not string!";

        os.print("\"");
        for (int i = 0; i < lastIndex; i++)
        {
            int c = (int) ((ConstantInt)ca.operand(i)).getZExtValue();
            if (c == '"')
                os.print("\\\"");
            else if (c == '\\')
                os.print("\\\\");
            else if (TextUtils.isPrintable(c))
                os.print((char)c);
            else
            {
                switch(c)
                {
                    case '\b': os.print("\\b"); break;
                    case '\f': os.print("\\f"); break;
                    case '\n': os.print("\\n"); break;
                    case '\r': os.print("\\r"); break;
                    case '\t': os.print("\\t"); break;
                    default:
                        os.print('\\');
                        os.print(toOctal(c >>6));
                        os.print(toOctal(c>>3));
                        os.print(toOctal(c));
                        break;
                }
            }
        }
        os.print("\"");
    }

    protected void printAsmOperand(MachineInstr mi, int opNo,
            int asmVariant, String extra)
    {
        Util.shouldNotReachHere("Target can not support");
    }

    protected void printAsmMemoryOperand(MachineInstr mi, int opNo,
            int asmVariant, String extra)
    {
        Util.shouldNotReachHere("Target can not support");
    }

    /**
     * This method formats and prints the specified machine
     * instruction that is an inline asm.
     * @param mi
     */
    protected void printInlineAsm(MachineInstr mi)
    {
        Util.shouldNotReachHere("Target can not support inline asm");
    }

    private boolean shouldOmitSectionDirective(String name, TargetAsmInfo tai)
    {
        switch (name)
        {
            case ".text":
            case ".data":
                return true;
            case ".bss":
                return !tai.usesELFSectionDirectiveForBSS();
        }
        return false;
    }

    public void switchSection(Section ns)
    {
        String newSection = ns.getName();

        if (currentSection.equals(newSection))
            return;

        if (tai.getSectionEndDirectiveSuffix() != null && !currentSection.isEmpty())
        {
            os.printf("%s%s\n", currentSection, tai.getSectionEndDirectiveSuffix());
        }

        currentSection = newSection;
        currentSection_ = ns;

        if (shouldOmitSectionDirective(newSection, tai))
        {
            os.printf("\t%s%n", currentSection);
            return;
        }

        if (!currentSection.isEmpty())
        {
            os.printf("%s%s%s", tai.getSwitchToSectionDirective(),
                    currentSection,
                    tai.getSectionFlags(ns.getFlags()));
        }
        else
            os.print(currentSection);
        os.println(tai.getDataSectionStartSuffix());

        isInTextSection = (ns.getFlags() & SectionFlags.Code) != 0;
    }

    public void switchSection(String newSection, GlobalValue gv)
    {
        String ns;
        String switchToSectionDirective = tai.getSwitchToSectionDirective();
        if (gv != null && gv.hasSection())
            ns = switchToSectionDirective + gv.getSection();
        else
            ns = "\t" + newSection;

        if (!Objects.equals(currentSection, ns))
        {
            currentSection = ns;
            if (currentSection != null && !currentSection.isEmpty())
                os.println(currentSection);
        }
    }

    public int getPreferredAlignLog(GlobalVariable gv)
    {
        int align = tm.getTargetData().getTypeAlign(gv.getType());
        if (gv.getAlignment() > (1 << align))
            align = Util.log2(gv.getAlignment());

        if (gv.hasInitializer())
        {
            // Always round up alignment of global doubles to 8 bytes.
            if (gv.getType().getElementType() == LLVMContext.DoubleTy && align < 3)
                align = 3;
            if (align < 4)
            {
                // If the global is not external, see if it is large.  If so, give it a
                // larger alignment.
                if (tm.getTargetData().getTypeSize(gv.getType().getElementType()) > 128)
                    align = 4;
            }
        }
        return align;
    }

    protected void printVisibility(String name,
            GlobalValue.VisibilityTypes visibility)
    {
        if (visibility == HiddenVisibility)
        {
            String directive = tai.getHiddenDirective();
            if (directive != null)
                os.printf("%s%s\n", directive, name);
        }
        else if (visibility == ProtectedVisibility)
        {
            String directive = tai.getProtectedDirective();
            if (directive != null)
                os.printf("%s%s\n", directive, name);
        }

    }

    private Module getModuleFromVal(Value v)
    {
        if (v instanceof Argument)
        {
            Argument arg = (Argument)v;
            return arg.getParent() != null? arg.getParent().getParent() : null;
        }

        if (v instanceof BasicBlock)
        {
            BasicBlock bb = (BasicBlock)v;
            return bb.getParent() != null?bb.getParent().getParent() : null;
        }

        if (v instanceof Instruction)
        {
            Instruction inst = (Instruction)v;
            Function f = inst.getParent() != null ? inst.getParent().getParent() : null;
            return f != null? f.getParent(): null;
        }

        if (v instanceof GlobalValue)
        {
            GlobalValue gv = (GlobalValue)v;
            return gv.getParent();
        }

        return null;
    }

    protected void printBasicBlockLabel(
            MachineBasicBlock mbb,
            boolean printAlign,
            boolean printColon,
            boolean printComment)
    {
        if (printAlign)
        {
            int align = mbb.getAlignment();
            if (align != 0)
                emitAlignment(Util.log2(align));
        }

        os.printf("%sBB%d_%d", tai.getPrivateGlobalPrefix(),
                getFunctionNumber(), mbb.getNumber());
        if (printColon)
            os.print(":");

        if (printComment)
        {
            BasicBlock bb = mbb.getBasicBlock();
            if (bb != null)
            {
                if (bb.hasName())
                {
                    os.print(Util.fixedLengthString(tai.getCommentColumn(), ' '));
                    os.printf("%s ", tai.getCommentString());
                    writeAsOperand(os, bb, false, null);
                }

                if (printColon)
                    emitComments(mbb);
            }
        }
    }

    public void emitComments(MachineBasicBlock mbb)
    {
        if (verboseAsm)
        {
            MachineLoopInfo loop = li.getLoopFor(mbb);
            if (loop != null)
            {
                os.println();
                os.print(Util.fixedLengthString(tai.getCommentColumn(), ' '));
                os.printf("%s Loop Depth %d\n", tai.getCommentString(),
                        loop.getLoopDepth());

                os.print(Util.fixedLengthString(tai.getCommentColumn(), ' '));

                MachineBasicBlock header = loop.getHeaderBlock();
                assert header != null:"No header for loop";

                if (header.equals(mbb))
                {
                    os.printf("%s Loop Header", tai.getCommentString());
                    printChildLoopComment(os, loop, tai, getFunctionNumber());
                }
                else
                {
                    os.printf("%s Loop Header is BB%d_%d",
                            tai.getCommentString(),
                            getFunctionNumber(),
                            loop.getHeaderBlock().getNumber());
                    printChildLoopComment(os, loop, tai, getFunctionNumber());
                }

                if (loop.isEmpty())
                {
                    os.println();
                    os.print(Util.fixedLengthString(tai.getCommentColumn(), ' '));
                    os.printf("%s Inner Loop", tai.getCommentString());
                }

                // Add parent loop information.
                for (MachineLoopInfo curLoop = loop.getParentLoop();
                     curLoop != null;
                     curLoop = curLoop.getParentLoop())
                {
                    MachineBasicBlock hBB = curLoop.getHeaderBlock();
                    assert hBB != null:"No header for loop";

                    os.println();
                    os.print(Util.fixedLengthString(tai.getCommentColumn(), ' '));
                    os.printf(tai.getCommentString());
                    indent(os, curLoop.getLoopDepth() - 1)
                            .printf(" Inside Loop BB%d_%d Depth %d",
                                    getFunctionNumber(),
                                    hBB.getNumber(),
                                    curLoop.getLoopDepth());

                }
            }
        }
    }

    /**
     * Pretty-print comments for instructions
     * @param mi
     */
    public void emitComments(MachineInstr mi)
    {
        // TODO: 17-7-31  emit debug information for each MachineInstr.
    }

    private static FormattedOutputStream indent(
            FormattedOutputStream os,
            int level)
    {
        return indent(os, level, 2);
    }

    private static FormattedOutputStream indent(
            FormattedOutputStream os,
            int level,
            int scale)
    {
        for (int i = level * scale; i != 0; --i)
            os.print(" ");

        return os;
    }

    private static void printChildLoopComment(
            FormattedOutputStream os,
            MachineLoopInfo loop,
            TargetAsmInfo tai,
            int functionNumber)
    {
        // Add child loop information.
        for (MachineLoopInfo childLoop : loop.getSubLoops())
        {
            MachineBasicBlock header = childLoop.getHeaderBlock();
            assert header != null:"No header for loop";

            os.println();
            os.print(Util.fixedLengthString(tai.getCommentColumn(), ' '));

            os.print(tai.getCommentString());
            indent(os, childLoop.getLoopDepth()-1)
                    .printf(" Child Loop BB%d_%d Depth %d",
                            functionNumber,
                            header.getNumber(),
                            childLoop.getLoopDepth());

            printChildLoopComment(os, childLoop, tai, functionNumber);
        }
    }

    public boolean emitSpecialLLVMGlobal(GlobalVariable gv)
    {
        if (gv.getName().equals("llvm.used"))
        {
            if (tai.getUsedDirective() != null)
                emitLLVMUsedList(gv.getInitializer());
            return true;
        }

        if (gv.getSection().equals("llvm.metadata"))
            return true;

        return false;
    }

    private void emitLLVMUsedList(Constant list)
    {
        assert false:"Should not reaching here!"; // TODO: 17-8-2
    }
}
