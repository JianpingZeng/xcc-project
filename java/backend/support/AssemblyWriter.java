/*
 * Extremely C language Compiler.
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

package backend.support;

import backend.type.OpaqueType;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.*;
import backend.value.GlobalValue.LinkageType;
import backend.value.GlobalValue.VisibilityTypes;
import jlang.support.Linkage;
import tools.Util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static backend.support.AssemblyWriter.PrefixType.GlobalPrefix;
import static backend.support.AssemblyWriter.PrefixType.LocalPrefix;

public class AssemblyWriter
{
    private PrintStream out;
    private Module theModule;
    private TypePrinting typePrinter;
    private ArrayList<Type> numberedTypes;
    private SlotTracker slotTracker;

    public AssemblyWriter(PrintStream os, Module m, SlotTracker tracker)
    {
        out = os;
        theModule = m;
        typePrinter = new TypePrinting();
        numberedTypes = new ArrayList<>();
        slotTracker = tracker;
        addModuleTypesToPrinter(typePrinter, numberedTypes, m);
    }

    public static void addModuleTypesToPrinter(TypePrinting printer,
                                               ArrayList<Type> numberedTypes,
                                               Module m)
    {
        if (m == null)
            return;

        HashMap<String, Type> st = m.getTypeSymbolTable();
        for (Map.Entry<String, Type> entry : st.entrySet())
        {
            Type ty = entry.getValue();
            if (ty instanceof PointerType)
            {
                PointerType ptr = (PointerType)ty;
                Type eleTy = ptr.getElementType();
                if ((eleTy.isPrimitiveType() || eleTy.isInteger())
                        && !(eleTy instanceof OpaqueType))
                {
                    continue;
                }
            }

            if (ty.isInteger() || ty.isPrimitiveType())
                continue;

            StringBuilder buf = new StringBuilder();
            printLLVMName(buf, entry.getKey(), LocalPrefix);
            printer.addTypeName(ty, buf.toString());
        }

        // Walk the entire module to find references to unnamed structure and opaque
        // types.  This is required for correctness by opaque types (because multiple
        // uses of an unnamed opaque type needs to be referred to by the same ID) and
        // it shrinks complex recursive structure types substantially in some cases.
        new TypeFinder(printer, numberedTypes).run(m);
    }

    enum PrefixType
    {
        GlobalPrefix,
        LabelPrefix,
        LocalPrefix,
        NoPrefix
    }

    private static void printLLVMName(PrintStream os,
                                      Value val)
    {
        printLLVMName(os, val.getName(), val instanceof GlobalValue ? GlobalPrefix
            : LocalPrefix);
    }

    /**
     * Turn the specified name into "LLVM name", which is either
     * prefixed with % or is surrounded with ""'s. Print it now.
     * @param os
     * @param name
     * @param pt
     */
    private static void printLLVMName(PrintStream os,
                               String name,
                               PrefixType pt)
    {
        assert name != null && !name.isEmpty():"Cannot get empty name!";
        switch (pt)
        {
            default:
                assert false:"Unknown PrefixType";
                break;
            case NoPrefix:
                break;
            case GlobalPrefix:
                os.print("@");
                break;
            case LocalPrefix:
            case LabelPrefix:
                os.print("%");
                break;
        }

        boolean needQuotes = Character.isDigit(name.charAt(0));
        if (!needQuotes)
        {
            for (int i = 0, e = name.length(); i != e; i++)
            {
                char c = name.charAt(i);
                if (c != '_' && c != '.' && !Character.isJavaIdentifierPart(c))
                {
                    needQuotes = true;
                    break;
                }
            }
        }

        if (!needQuotes)
        {
            os.print(name);
            return;
        }
        os.printf("\"%s\"", name);
    }

    public void write(Module m)
    {
        printModule(m);
    }

    public void write(GlobalValue gv)
    {
        if (gv instanceof GlobalVariable)
        {
            printGlobal((GlobalVariable)gv);
        }
        else
        {
            assert gv instanceof Function :"Unknown global value kind";
            printFunction((Function)gv);
        }
    }

    /**
     * Output all global variables into ouput stream.
     * @param gv
     */
    private void printGlobal(GlobalVariable gv)
    {
        writeAsOperandInternal(out, gv, typePrinter, slotTracker);
        out.print(" = ");

        if (!gv.hasInitializer() && gv.hasExternalLinkage())
            out.print("external ");

        printLinkage(gv.getLinkage(), out);
        printVisiblity(gv.getVisibility(), out);

        int addressSpace = gv.getType().getAddressSpace();
        if (addressSpace != 0)
            out.printf("addrspace(%d) ", addressSpace);
        out.print(gv.isConstant() ? "constant " : "global ");
        typePrinter.print(gv.getType().getElementType(), out);

        if (gv.hasInitializer())
        {
            out.print(" ");
            writeOperand(gv.getInitializer(), false);
        }
        int align = gv.getAlignment();
        if (align != 0)
        {
            out.printf(", align %d", align);
        }
        printInfoComment(gv);
        out.println();
    }

    private void printInfoComment(Value val)
    {
        if (!val.getType().equals(Type.VoidTy))
        {
            out.print("; <");
            typePrinter.print(val.getType(), out);
            // output number of uses.
            out.printf("> [#uses=%d]", val.getNumUses());
        }
    }

    private static SlotTracker createSlotTracker(Value val)
    {
        // TODO: 17-10-9
    }

    public static void writeAsOperandInternal(PrintStream out,
                                        Value val,
                                        TypePrinting printer,
                                        SlotTracker tracker)
    {
        if (val.hasName())
        {
            printLLVMName(out, val);
            return;
        }

        Constant cv = val instanceof Constant ? (Constant)val : null;
        if (cv != null && !(cv instanceof GlobalValue))
        {
            assert printer != null:"Constants require TypePrintering";
            writeConstantInt(out, cv, printer, tracker);
            return;
        }

        char prefix = '%';
        int slot = -1;
        if (tracker == null)
            tracker = createSlotTracker(val);

        if (tracker != null)
        {
            GlobalValue gv = val instanceof GlobalValue ? (GlobalValue)val : null;
            if (gv != null)
            {
                slot = tracker.getGlobalSlot(gv);
                prefix = '@';
            }
            else
            {
                slot = tracker.getLocalSlot(val);
            }
        }
        if (slot != -1)
            out.printf("%c%d", prefix, slot);
        else
            out.print("<badref>");
    }

    public static void writeAsOperand(PrintStream out,
                                       Value val,
                                       boolean printType,
                                       Module context) {


        if (!printType && (!(val instanceof Constant)) || val.hasName() || val instanceof GlobalValue)
        {
            writeAsOperandInternal(out, val, null, null);
            return;
        }

        if (context == null)
            context = getModuleFromVal(v);

        TypePrinting typePrinter = new TypePrinting();
        // TODO: 17-7-31
    }

    public static void writeConstantInt(PrintStream out,
                                  Constant cv,
                                  TypePrinting printer,
                                  SlotTracker tracker)
    {
        // TODO: 17-10-9
    }

    public static void printLinkage(LinkageType linkage, PrintStream out)
    {
        // TODO: 17-10-9
    }

    private static void printVisiblity(VisibilityTypes vt, PrintStream out)
    {
        // TODO: 17-10-9
    }

    private void writeOperand(Value operand, boolean printType)
    {
        if (operand == null)
        {
            out.print("<null operand!>");
        }
        else
        {
            if (printType)
            {
                typePrinter.print(operand.getType(), out);
                out.print(" ");
            }
            writeAsOperandInternal(out, operand, typePrinter, slotTracker);
        }
    }

    private void printFunction(Function f)
    {
        // TODO: 17-10-9
    }

    public void printModule(Module m)
    {
        if (m.getModuleIdentifier() != null && m.getModuleIdentifier().isEmpty())
        {
            out.printf(";ModuleID = '%s'\n", m.getModuleIdentifier());
        }

        if (m.getDataLayout() != null && !m.getDataLayout().isEmpty())
        {
            out.printf("target datalayout = \"%s\"\n", m.getDataLayout());
        }
        if (m.getTargetTriple() != null && !m.getTargetTriple().isEmpty())
        {
            out.printf("target triple = \"%s\"\n", m.getTargetTriple());
        }

        // Loop over all symbol, emitting all id's types.
        if (!m.getTypeSymbolTable().isEmpty() || !numberedTypes.isEmpty())
            out.println();

        printTypeSymbolTable(m.getTypeSymbolTable());

        // Emitting all globals.
        if (!m.getGlobalVariableList().isEmpty())
            out.println();

        for (GlobalVariable gv : m.getGlobalVariableList())
        {
            printGlobal(gv);
        }

        // Emitting all functions.
        for (Function f m.getFunctionList())
        {
            printFunction(f);
        }
    }

    private void printTypeSymbolTable(HashMap<String,Type> st)
    {
        // TODO: 17-10-9
        // Emit all numbered types.
        for (int i = 0, e = numberedTypes.size(); i !=e; i++)
        {
            out.printf("%%%d = type ", i);

            typePrinter.printAtLeastOneLevel(numberedTypes.get(i), out);
            out.println();
        }

        // print named types.
        for (Map.Entry<String, Type> entry : st)
        {
            printLLVMName(out, entry.getKey(), LocalPrefix);
            out.print(" = type");

            typePrinter.printAtLeastOneLevel(entry.getValue(), out);
            out.println();
        }
    }
}
