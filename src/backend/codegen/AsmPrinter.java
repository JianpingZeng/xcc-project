package backend.codegen;
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

import backend.hir.Module;
import backend.support.NameMangler;
import backend.target.TargetData;
import backend.target.TargetMachine;
import backend.type.Type;
import backend.value.*;
import backend.value.Value.UndefValue;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import tools.Util;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Objects;

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
     * The current section name where we are emitting to.
     */
    private String currentSection;
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
    protected PrintWriter os;
    /**
     * The target machine description.
     */
    protected TargetMachine tm;
    /**
     * A name mangler for performing necessary mangling on global linkage entity.
     */
    protected NameMangler mangler;
    /**
     * The name of current being processed machine function.
     */
    protected String curFnName;
    /**
     * The comment string prefix, default to '#'.
     */
    protected String commentString;
    /**
     * If this is set to non-empty string, so that it is prepended to the all
     * global symbols. This often is set to '_' or '.', but current default to
     * "".
     */
    protected String globalPrefix;
    /**
     * This prefix is used for globals like constant
     * pool entries that are completely private to the .o file and should not
     * have names in the .o file.  This is often "." or "L".
     */
    protected String privateGlobalPrefix;

    protected String globalVarAddrPrefix;  // default to ""
    protected String globalVarAddrSuffix;  // default to ""
    // default to ""
    protected String functionAddrPrefix;
    // default to ""
    protected String functionAddrSuffix;
    /**
     * Defaults to "\t.zero\t".
     */
    protected String zeroDirective;
    /**
     * Defaults to "\t.ascii\t".
     */
    protected String asciiDirective;
    /**
     * Defaults to "\t.asciz\t".
     */
    protected String ascizDirective;
    /**
     * Defaults to "\t.byte\t".
     */
    protected String data8BitDirective;
    /**
     * Defaults to "\t.short\t".
     */
    protected String data16BitDirective;
    /**
     * Defaults to "\t.long\t".
     */
    protected String data32BitDirective;
    /**
     * Defaults to "\t.quad\t".
     */
    protected String data64BitDirective;
    /**
     * Defaults to "\t.align\t".
     */
    protected String alignDirective;
    /**
     * If this is true (the default) then the asmprinter
     * emits ".align N" directives, where N is the number of bytes to align to.
     * Otherwise, it emits ".align log2(N)", e.g. 3 to align to an 8 byte
     * boundary.
     * Default to true.
     */
    protected boolean alignIsInByte;
    /**
     * Default to "\t.section\t".
     */
    protected String switchToSectionDirective;
    /**
     * This is the section that we SwitchToSection right
     * before emitting the constant pool for a function.
     *
     * Default to "\t.section .rodata\n".
     */
    protected String constantPoolSection;
    /**
     * This is the name of a directive (if supported) that can
     * be used to efficiently declare a local (internal) block of zero
     * initialized data in the .bss/.data section.  The syntax expected is:
     *   <LCOMMDirective> SYMBOLNAME LENGTHINBYTES, ALIGNMENT
     *
     * <p>Defaults to null.</p>
     */
    protected String lCOMMDirective;
    /**
     * Defaults to "\t.comm\t".
     */
    protected String commDirective;
    /**
     * True if commDirective takes the third argument which specifies
     * the alignment of declaration.
     */
    protected boolean commDirectiveTakesAlign;
    /**
     * Indicates if there is a directive, ".getNumOfSubLoop" or ".type". This is true
     * in the most ELF target.
     */
    protected boolean hasDotTypeDotSizeDirective;

    protected AsmPrinter(OutputStream os, TargetMachine tm)
    {
        functionNumber = 0;
        this.os = new PrintWriter(os);
        this.tm = tm;
        commentString = "#";
        globalPrefix = "";
        privateGlobalPrefix = ".";
        globalVarAddrPrefix = "";
        globalVarAddrSuffix = "";
        functionAddrPrefix = "";
        functionAddrSuffix = "";
        zeroDirective = "\t.zero\t";
        asciiDirective = "\t.ascii\t";
        ascizDirective = "\tasciz\t";
        data8BitDirective = "\t.byte\t";
        data16BitDirective = "\t.short\t";
        data32BitDirective = "\t.long\t";
        data64BitDirective = "\t.quad\t";
        alignDirective = "\t.align\t";
        alignIsInByte = true;
        switchToSectionDirective = "\t.section\t";
        constantPoolSection = "\t.section .rodata\t";
        commDirective = "\t.comm\t";
        commDirectiveTakesAlign = true;
        hasDotTypeDotSizeDirective = true;
    }

    /**
     * This should be called when a new machine function is being processed when
     * running method runOnMachineFunction();
     */
    protected void setupMachineFunction(MachineFunction mf)
    {
        curFnName = mangler.getValueName(mf.getFunction());
        incrementFnNumeber();
    }

    protected int getFunctionNumber() {return functionNumber;}

    protected void incrementFnNumeber() {functionNumber++;}

    protected void emitConstantPool(MachineConstantPool mcp)
    {
        ArrayList<Constant> consts = mcp.getConstantPool();
        if (consts.isEmpty()) return;

        TargetData td = tm.getTargetData();
        switchSection(constantPoolSection, null);
        emitAlignment(mcp.getContantPoolAlignment(), null);
        for (int i = 0, e = consts.size(); i < e; i++)
        {
            os.print(privateGlobalPrefix + "CPI" + getFunctionNumber());
            os.print("_" + i + ":\\t\\t\\t\\t\\t" + commentString + " ");
            writeTypeSymbol(os, consts.get(i).getType(), null);
            emitGlobalConstant(consts.get(i));

            if (i != e -1)
            {
                long entSize = tm.getTargetData().getTypeSize(consts.get(i).getType());
                long valEnd = mcp.getConstantPoolIndex(consts.get(i)) + entSize;
                long zeros = mcp.getConstantPoolIndex(consts.get(i+1)) - valEnd;
                emitZero(zeros);
            }
        }
    }

    public static PrintWriter writeTypeSymbol(PrintWriter os, Type ty, Module m)
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

        if (alignIsInByte) numBits = 1 << numBits;
        os.println(alignDirective + numBits);
    }

    /**
     * Emits a block of zeros.
     * @param numZeros
     */
    protected void emitZero(long numZeros)
    {
        if (numZeros != 0)
        {
            if (zeroDirective != null)
                os.println(zeroDirective + numZeros);
            else
                for (;numZeros != 0; numZeros--)
                    os.print(data8BitDirective + "0\n");
        }
    }

    /**
     * Prints a general Backend constant into .s file.
     * @param c
     */
    protected void emitGlobalConstant(Constant c)
    {
        TargetData td = tm.getTargetData();

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
                if (ascizDirective != null && numElts != 0
                        && ((ConstantInt)ca.operand(numElts-1)).getZExtValue() == 0)
                {
                    os.print(ascizDirective);
                    printAsCString(os, ca, numElts-1);
                }
                else
                {
                    os.print(asciiDirective);
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
                return;
            }
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
            assert sizeSoFar == layout.structSize:"layout of constant struct may be incorrect!";
            return;
        }
        else if (c instanceof ConstantFP)
        {
            // FP Constants are printed as integer constants to avoid losing
            // precision.
            ConstantFP fp = (ConstantFP)c;
            double val = fp.getValue();
            if (fp.getType() == Type.DoubleTy)
            {
                if (data64BitDirective != null)
                {
                    os.println(data64BitDirective + doubleToBits(val)
                    + "\t" + commentString + "double value: " + val);
                }
                else if (td.isLittleEndian())
                {
                    os.print(data32BitDirective + (int)doubleToBits(val));
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
                os.print(data32BitDirective + floatToBits((float) val));
                os.println("\t" + commentString + " float " + val);
                return;
            }
        }
        else if (c.getType() == Type.Int64Ty)
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
        switch (type.getPrimitiveID())
        {
            case Type.Int1TyID:
            case Type.Int8TyID:
                os.print(data8BitDirective);
                break;
            case Type.Int16TyID:
                os.print(data16BitDirective);
                break;
            case Type.PointerTyID:
                if (td.getPointerSize() == 8)
                {
                    os.print(data64BitDirective);
                    break;
                }
            case Type.Int32TyID:
                os.print(data32BitDirective);
                break;
            case Type.Int64TyID:
                assert data64BitDirective != null:"Target cannot handle 64-bit constant!";
                os.print(data64BitDirective);
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
                os.print(functionAddrPrefix + mangler.getValueName(gv));
                os.print(functionAddrSuffix);
            }
            else
            {
                os.print(globalVarAddrPrefix + mangler.getValueName(gv));
                os.print(globalVarAddrSuffix);
            }
        }
        else if (c instanceof ConstantExpr)
        {
            ConstantExpr ce = (ConstantExpr)c;
            TargetData td = tm.getTargetData();
            switch (ce.getOpCode())
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
            assert false:"Unknown constant expre!";
    }

    private long doubleToBits(double val)
    {
        return (long)val;
    }

    private int floatToBits(float val)
    {
        return (int)val;
    }

    private boolean isprint(int c)
    {
        // TODO
        return true;
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
    private void printAsCString(PrintWriter os, ConstantArray ca, int lastIndex)
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
            else if (isprint(c))
                os.print(c);
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
        throw new NotImplementedException();
    }

    protected void printAsmMemoryOperand(MachineInstr mi, int opNo,
            int asmVariant, String extra)
    {
        throw new NotImplementedException();
    }

    /**
     * This method formats and prints the specified machine
     * instruction that is an inline asm.
     * @param mi
     */
    protected void printInlineAsm(MachineInstr mi)
    {

    }

    public void switchSection(String newSection, GlobalValue gv)
    {
        String ns;
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
            if (gv.getType().getElemType() == Type.DoubleTy && align < 3)
                align = 3;
            if (align < 4)
            {
                // If the global is not external, see if it is large.  If so, give it a
                // larger alignment.
                if (tm.getTargetData().getTypeSize(gv.getType().getElemType()) > 128)
                    align = 4;
            }
        }
        return align;
    }
}
