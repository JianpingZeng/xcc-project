package backend.support;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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
import backend.value.Module;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.FormattedOutputStream;

import java.io.PrintStream;
import java.util.ArrayList;

import static backend.type.LLVMTypeID.*;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public final class TypePrinting {
  ArrayList<StructType> namedTypes;
  TObjectIntHashMap<StructType> numberedTypes;
  public TypePrinting() {
    namedTypes = new ArrayList<>();
    numberedTypes = new TObjectIntHashMap<>();
  }

  public void clear() {
    namedTypes.clear();
    numberedTypes.clear();
  }

  void incorporateType(Module m) {
    m.findUsedStructTypes(namedTypes);

    int nextNumber = 0;
    int nextUse = 0;
    for (StructType sty : namedTypes) {
      // ignore anonnymous literal structure.
      if (sty.isLiteral())
        continue;

      if (sty.getName().isEmpty())
        numberedTypes.put(sty, nextNumber++);
      else
        namedTypes.set(nextUse++, sty);
    }
    namedTypes = new ArrayList<>(namedTypes.subList(0, nextUse));
  }

  public void print(Type ty, PrintStream os) {
    print(ty, new FormattedOutputStream(os));
  }

  public void print(Type ty, FormattedOutputStream os) {
    switch (ty.getTypeID()) {
      case VoidTyID:
        os.print("void");
        break;
      case FloatTyID:
        os.print("float");
        break;
      case DoubleTyID:
        os.print("double");
        break;
      case X86_FP80TyID:
        os.print("x86_fp80");
        break;
      case FP128TyID:
        os.print("fp128");
        break;
      case LabelTyID:
        os.print("label");
        break;
      case MetadataTyID:
        os.print("metadata");
        break;
      case IntegerTyID:
        os.printf("i%d", ((IntegerType) ty).getBitWidth());
        break;
      case FunctionTyID: {
        FunctionType ft = (FunctionType) ty;
        print(ft.getReturnType(), os);
        os.print("(");
        for (int i = 0, e = ft.getNumParams(); i != e; i++) {
          if (i != 0)
            os.print(", ");
          print(ft.getParamType(i), os);
        }
        if (ft.isVarArg()) {
          if (ft.getNumParams() != 0)
            os.print(",");
          os.print("...");
        }
        os.print(')');
        break;
      }
      case StructTyID: {
        StructType st = (StructType) ty;
        if (st.isLiteral()) {
          printStructBody(st, os);
          return;
        }
        if (!st.getName().isEmpty()) {
          AssemblyWriter.printLLVMName(os, st.getName(), AssemblyWriter.PrefixType.LocalPrefix);
          return;
        }

        if (st.isPacked())
          os.print('<');

        os.print('{');
        for (int i = 0, e = st.getNumOfElements(); i != e; i++) {
          print(st.getElementType(i), os);
          if (i != e - 1)
            os.print(',');
          os.print(' ');
        }
        os.print('}');
        if (st.isPacked())
          os.print('>');
        break;
      }
      case PointerTyID: {
        PointerType pty = (PointerType) ty;
        print(pty.getElementType(), os);
        int addrspace = pty.getAddressSpace();
        if (addrspace != 0)
          os.printf("addrspace(%d)", addrspace);
        os.print("*");
        break;
      }
      case ArrayTyID: {
        ArrayType at = (ArrayType) ty;
        os.printf("[%d x ", at.getNumElements());
        print(at.getElementType(), os);
        os.print("]");
        break;
      }
      case VectorTyID: {
        VectorType vty = (VectorType) ty;
        os.printf("<%d x ", vty.getNumElements());
        print(vty.getElementType(), os);
        os.print(">");
        break;
      }
      default:
        os.print("<unrecognized-type>");
        break;
    }
  }

  public void printStructBody(StructType sty, FormattedOutputStream os) {
    if (sty.isOpaqueTy()) {
      os.print("opaque");
      return;
    }

    if (sty.isPacked())
      os.print('<');

    if (sty.getNumOfElements() == 0)
      os.print("{}");
    else {
      os.print("{ ");
      int e = sty.getNumContainedTypes();
      if (e > 0) {

        print(sty.getElementType(0), os);
        for (int i = 1; i < e; i++) {
          os.print(", ");
          print(sty.getElementType(i), os);
        }
      }
      os.print(" }");
    }
    if (sty.isPacked())
      os.print('>');
  }
}
