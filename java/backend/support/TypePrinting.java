package backend.support;
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

import backend.type.*;
import tools.FormattedOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Stack;

import static backend.type.LLVMTypeID.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class TypePrinting
{
    private HashMap<Type, String> typenames;

    public TypePrinting()
    {
        typenames = new HashMap<>();
    }

    public void clear()
    {
        typenames.clear();
    }

    public void print(Type ty, FormattedOutputStream os)
    {
        print(ty, os, false);
    }

    public void print(Type ty, PrintStream os)
    {
        print(ty, os, false);
    }

    public void print(Type ty, FormattedOutputStream os, boolean ignoreTopLevelName)
    {
        // Check to see if the type is named.
        if (!ignoreTopLevelName)
        {
            if (typenames.containsKey(ty))
            {
                os.print(typenames.get(ty));
                return;
            }
        }

        // Otherwise we have a type that has not been named but is a derived type.
        // Carefully recurse the type hierarchy to print out any contained symbolic
        // names.
        Stack<Type> typeStack = new Stack<>();
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream out = new PrintStream(baos))
        {
            calcTypeName(ty, typeStack, out, ignoreTopLevelName);
            os.print(baos.toString());

            if (!ignoreTopLevelName)
                typenames.put(ty, baos.toString());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void print(Type ty, PrintStream os, boolean ignoreTopLevelName)
    {
        // Check to see if the type is named.
        if (!ignoreTopLevelName)
        {
            if (typenames.containsKey(ty))
            {
                os.print(typenames.get(ty));
                return;
            }
        }

        // Otherwise we have a type that has not been named but is a derived type.
        // Carefully recurse the type hierarchy to print out any contained symbolic
        // names.
        Stack<Type> typeStack = new Stack<>();
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream out = new PrintStream(baos))
        {
            calcTypeName(ty, typeStack, out, ignoreTopLevelName);
            os.print(baos.toString());

            if (!ignoreTopLevelName)
                typenames.put(ty, baos.toString());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public boolean hasTypeName(Type ty)
    {
        return typenames.containsKey(ty);
    }

    public void addTypeName(Type ty, String name)
    {
        typenames.put(ty, name);
    }

    private void calcTypeName(Type ty, Stack<Type> typeStack, PrintStream os)
    {
        calcTypeName(ty, typeStack, os, false);
    }

    private void calcTypeName(Type ty, Stack<Type> typeStack,
            PrintStream os, boolean ignroeTopLevelName)
    {
        // TODO: 2017/10/10
        if (!ignroeTopLevelName)
        {
            if (typenames.containsKey(ty))
            {
                os.print(typenames.get(ty));
                return;
            }
        }

        int slot = 0, curSize = typeStack.size();
        while (slot < curSize && !typeStack.get(slot).equals(ty))
            ++slot;

        if (slot < curSize)
        {
            os.printf("\\%d", curSize - slot);
            return;
        }

        typeStack.push(ty);
        switch (ty.getTypeID())
        {
            case VoidTyID: os.print("void");break;
            case FloatTyID: os.print("float");break;
            case DoubleTyID: os.print("double");break;
            case X86_FP80TyID: os.print("x86_fp80");break;
            case FP128TyID: os.print("fp128");break;
            case LabelTyID: os.print("label");break;
            case IntegerTyID:
                os.printf("i%d", ((IntegerType)ty).getBitWidth());
                break;
            case FunctionTyID:
            {
                FunctionType ft = (FunctionType)ty;
                calcTypeName(ft.getReturnType(), typeStack, os);
                os.print("(");
                for (int i = 0,e = ft.getNumParams(); i != e; i++)
                {
                    if (i != 0)
                        os.print(", ");
                    calcTypeName(ft.getParamType(i), typeStack, os);
                }
                if (ft.isVarArg())
                {
                    if (ft.getNumParams() != 0)
                        os.print(",");
                    os.print("...");
                }
                os.print(')');
                break;
            }
            case StructTyID:
            {
                StructType st = (StructType)ty;
                if (st.isPacked())
                    os.print('<');

                os.print('{');
                for (int i = 0, e = st.getNumOfElements(); i != e; i++)
                {
                    calcTypeName(st.getElementType(i), typeStack, os);
                    if (i != e - 1)
                        os.print(',');
                    os.print(' ');
                }
                os.print('}');
                if (st.isPacked())
                    os.print('>');
                break;
            }
            case PointerTyID:
            {
                PointerType pty = (PointerType)ty;
                calcTypeName(pty.getElementType(), typeStack, os);
                int addrspace = pty.getAddressSpace();
                if (addrspace != 0)
                    os.printf("addrspace(%d)", addrspace);
                os.printf("*");
                break;
            }
            case ArrayTyID:
            {
                ArrayType at = (ArrayType)ty;
                os.printf("[%d x ", at.getNumElements());
                calcTypeName(at.getElementType(), typeStack, os);
                os.printf("]");
                break;
            }
            case OpaqueTyID:
            {
                os.printf("opaque");
                break;
            }
            default:
                os.printf("<unrecognized-type>");
                break;
        }
        // remove self from stack.
        typeStack.pop();
    }

    public void printAtLeastOneLevel(Type type, FormattedOutputStream out)
    {
        print(type, out, true);
    }
}
