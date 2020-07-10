package utils.tablegen;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
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

import backend.codegen.EVT;
import backend.codegen.MVT;
import gnu.trove.list.array.TIntArrayList;
import tools.Error;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;

import static backend.codegen.MVT.iAny;
import static backend.codegen.MVT.vAny;

/**
 * This class defines a wrapper for the 'Intrinsic' TablleGen class.
 *
 * @author Jianping Zeng
 * @version 0.4
 * @since 0.1
 */
public final class CodeGenIntrinsic {
  /**
   * The actual record defining this 'Intrinsic' instruction.
   */
  Record theDef;
  /**
   * The namespace of the LLVM function "llvm.bswap.i32"
   */
  String name;
  /**
   * The namespace of the enum "bswap_i32".
   */
  String enumName;

  /**
   * Name of the corresponding GCC builtin, or "".
   */
  String gccBuiltinName;

  /**
   * Target prefix, e.g. "x86".
   */
  String targetPrefix;

  /**
   * This structure holds the return values and
   * parameter values of an intrinsic. If the number of return values is > 1,
   * then the intrinsic implicitly returns a first-class aggregate. The
   * numbering of the types starts at 0 with the first return value and
   * continues from there through the parameter list. This is useful for
   * "matching" types.
   */
  public static final class IntrinsicSignature {
    /**
     * The {@linkplain backend.codegen.MVT} for each return type.
     * Note that this list is only populated when in the context of a
     * target.td file. when building Intrinsics.td, this is not available,
     * because we don't known the target pointer size.
     */
    TIntArrayList retVTs = new TIntArrayList();

    /**
     * The records for each return type.
     */
    ArrayList<Record> retTypeDefs = new ArrayList<>();

    /**
     * The {@linkplain backend.codegen.MVT} for each parameter type. Note that
     * this list is only populated when in the context of a target.td. When
     * building Intrinsic.td, this is not available, because we don't known
     * the target pointer size.
     */
    TIntArrayList paramVTs = new TIntArrayList();

    /**
     * The records for each parameter type.
     */
    ArrayList<Record> paramTypeDefs = new ArrayList<>();
  }

  IntrinsicSignature is;

  /**
   * Memory mod/ref behavior of this intrinsic.
   */
  enum ModRefType {
    NoMem, ReadArgMem, ReadMem, WriteArgMem, WriteMem, IntrReadWriteArgMem
  }

  ModRefType modRef;

  /**
   * This is set as true if the Intrinsic is overloaded by it's argument
   * types.
   */
  boolean isOverloaded;

  /**
   * This is set to true if the intrinsic is commutative.
   */
  boolean isCommutative;

  enum ArgAttribute {
    NoCapture
  }

  ArrayList<Pair<Integer, ArgAttribute>> argumentAttributes = new ArrayList<>();
  public CodeGenIntrinsic(Record r) {
    theDef = r;
    is = new IntrinsicSignature();
    String defName = r.getName();
    modRef = ModRefType.WriteMem;
    isOverloaded = false;
    isCommutative = false;

    if (defName.length() <= 4 || !defName.substring(0, 4).equals("int_"))
      Error.printFatalError("Intrinsic '" + defName + "' does not start with 'int_'");

    enumName = defName.substring(4);

    if (r.getValue("GCCBuiltinName") != null)
      gccBuiltinName = r.getValueAsString("GCCBuiltinName");

    targetPrefix = r.getValueAsString("TargetPrefix");
    Util.assertion(targetPrefix != null);
    name = r.getValueAsString("LLVMName");
    Util.assertion(name != null);

    if (name.isEmpty()) {
      name += "llvm.";

      for (int i = 0, e = enumName.length(); i < e; i++)
        name = String.format("%s%s", name, enumName.charAt(i) == '_' ? '.' : enumName.charAt(i));
    } else {
      // Verify it starts with "llvm.".
      if (name.length() <= 5 || !name.substring(0, 5).equals("llvm."))
        Error.printFatalError("Intrinsic '" + defName + "' does not start with 'llvm.'");
    }

    // If TargetPrefix is specified, make sure that Name starts with
    // "llvm.<targetprefix>.".
    if (!targetPrefix.isEmpty()) {
      if (name.length() < 6 + targetPrefix.length() ||
          !name.substring(5, 6 + targetPrefix.length()).equals(targetPrefix + ".")) {
        Error.printFatalError("Intrinsic '" + defName +
            "' does not start with 'llvm.'" + targetPrefix + ".!");
      }
    }

    // Parse the list of return types.
    TIntArrayList overloadedVTs = new TIntArrayList();
    Init.ListInit typeList = r.getValueAsListInit("RetTypes");
    Util.assertion(typeList != null, "Intrinsic should have the member named RetTypes");
    for (int i = 0, e = typeList.getSize(); i != e; i++) {
      Record tyElt = typeList.getElementAsRecord(i);
      Util.assertion(tyElt.isSubClassOf("LLVMType"), "Expected a type!");
      int vt;
      if (tyElt.isSubClassOf("LLVMMatchType")) {
        int matchTy = (int) tyElt.getValueAsInt("Number");
        Util.assertion(matchTy < overloadedVTs.size(), "Invalid matching number!");

        vt = overloadedVTs.get(matchTy);

        Util.assertion((!tyElt.isSubClassOf("LLVMExtendedElementVectorType") && !(tyElt.isSubClassOf("llvmTruncatedElementVectorType")))
            || vt == iAny || vt == vAny, "Expected iAny or vAny type");

      } else {
        vt = getValueType(tyElt.getValueAsDef("VT"));
      }

      /*if (vt == MVT.isVoid)
        Error.printFatalError("Intrinsic '" + defName + "' has void in result type list!");*/

      if (new EVT(vt).isOverloaded()) {
        overloadedVTs.add(vt);
        isOverloaded |= true;
      }
      is.retVTs.add(vt);
      is.retTypeDefs.add(tyElt);
    }

    // Comment this for compatible with ValueTypeByHwMode in latest LLVM version.
    // if (is.retVTs.isEmpty())
    // Error.printFatalError("Intrinsic '" + defName + "' needs at least a type for the ret value!");

    // // Parse the list of parameter types.
    typeList = r.getValueAsListInit("ParamTypes");
    Util.assertion(typeList != null, "Intrinsic should have the member named ParamTypes");
    for (int i = 0, e = typeList.getSize(); i != e; i++) {
      Record tyElt = typeList.getElementAsRecord(i);
      Util.assertion(tyElt.isSubClassOf("LLVMType"), "Expected a type!");
      int vt;
      if (tyElt.isSubClassOf("LLVMMatchType")) {
        int matchTy = (int) tyElt.getValueAsInt("Number");
        Util.assertion(matchTy < overloadedVTs.size(), "Invalid matching number!");

        vt = overloadedVTs.get(matchTy);

        Util.assertion((!tyElt.isSubClassOf("LLVMExtendedElementVectorType") && !(tyElt.isSubClassOf("llvmTruncatedElementVectorType")))
            || vt == iAny || vt == vAny, "Expected iAny or vAny type");

      } else {
        vt = getValueType(tyElt.getValueAsDef("VT"));
      }

      // Reject invalid types.
      if (vt == MVT.isVoid && i != e - 1 /*void at end means varargs*/)
        Error.printFatalError("Intrinsic '" + defName + "' has void in parameter type list!");

      if (new EVT(vt).isOverloaded()) {
        overloadedVTs.add(vt);
        isOverloaded |= true;
      }
      is.paramVTs.add(vt);
      is.retTypeDefs.add(tyElt);
    }

    // Parse the intrinsic properties.
    Init.ListInit propList = r.getValueAsListInit("Properties");
    Util.assertion(propList != null, "Intrinsic should have the member named Properties");
    for (int i = 0, e = propList.getSize(); i != e; i++) {
      Record property = propList.getElementAsRecord(i);
      switch (property.getName()) {
        case "IntrNoMem":
          modRef = ModRefType.NoMem;
          break;
        case "IntrReadArgMem":
          modRef = ModRefType.ReadArgMem;
          break;
        case "IntrReadMem":
          modRef = ModRefType.ReadMem;
          break;
        case "IntrWriteArgMem":
          modRef = ModRefType.WriteArgMem;
          break;
        case "IntrWriteMem":
          modRef = ModRefType.WriteMem;
          break;
        case "Commutative":
          isCommutative = true;
          break;
        case "IntrReadWriteArgMem":
          modRef = ModRefType.IntrReadWriteArgMem;
          break;
        default: {
          if (property.isSubClassOf("NoCapture")) {
            int argNo = (int) property.getValueAsInt("ArgNo");
            argumentAttributes.add(Pair.get(argNo, ArgAttribute.NoCapture));
            break;
          } else
            Util.assertion("Unknown property!");
        }
      }
    }
  }

  private int getValueType(Record record) {
    return (int) record.getValueAsInt("Value");
  }
}
