/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2."" (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.""
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package xcc;

import jlang.diag.CompilationPhase;
import tools.Util;

/**
 * @author Jianping Zeng
 * @version "".1
 */
public enum InputType implements TypeID {
  CPP_OUT("cpp-output", TY_PP_C, TY_INVALID, "i", "u"),
  C("c", TY_C, TY_PP_C, "", "u"),
  OBJECTIVE_C_CPP_OUTPUT("objective-c-cpp-output", TY_PP_ObjC, TY_INVALID, "mi", "u"),
  OBJECTIVE_C("objective-c", TY_ObjC, TY_PP_ObjC, "", "u"),
  CXX_CPP_OUTPUT("c++-cpp-output", TY_PP_CXX, TY_INVALID, "ii", "u"),
  CXX("c++", TY_CXX, TY_PP_CXX, "", "u"),
  OBJECTIVE_CXX_CPP_OUTPUT("objective-c++-cpp-output", TY_PP_ObjCXX, TY_INVALID, "mii", "u"),
  OBJECTIVE_CXX("objective-c++", TY_ObjCXX, TY_PP_ObjCXX, "", "u"),

  // C family input files to precompile.
  C_HEADER_CPP_OUTPUT("c-header-cpp-output", TY_PP_CHeader, TY_INVALID, "i", "p"),
  C_HEADER("c-header", TY_CHeader, TY_PP_CHeader, "", "pu"),
  OBJECTIVE_C_HEADER_CPP_OUTPUT("objective-c-header-cpp-output", TY_PP_ObjCHeader, TY_INVALID, "mi", "p"),
  OBJECTIVE_C_HEADER("objective-c-header", TY_ObjCHeader, TY_PP_ObjCHeader, "", "pu"),
  CXX_HEADER_CPP_OUTPUT("c++-header-cpp-output", TY_PP_CXXHeader, TY_INVALID, "ii", "p"),
  CXX_HEADER("c++-header", TY_CXXHeader, TY_PP_CXXHeader, "", "pu"),
  OBJECTIVE_CXX_HEADER_CPP_OUTPUT("objective-c++-header-cpp-output", TY_PP_ObjCXXHeader, TY_INVALID, "mii", "p"),
  OBJECTIVE_CXX_HEADER("objective-c++-header", TY_ObjCXXHeader, TY_PP_ObjCXXHeader, "", "pu"),

  // Other languages.
  ADA("ada", TY_Ada, TY_INVALID, "", "u"),
  ASSEMBLER("assembler", TY_PP_Asm, TY_INVALID, "s", "au"),
  ASSEMBLER_WITH_CPP("assembler-with-cpp", TY_Asm, TY_PP_Asm, "", "au"),
  F95("f95", TY_PP_Fortran, TY_INVALID, "", "u"),
  F96_CPP_INPUT("f95-cpp-input", TY_Fortran, TY_PP_Fortran, "", "u"),
  JAVA("java", TY_Java, TY_INVALID, "", "u"),

  // Misc.
  LLVM_ASM("llvm-asm", TY_LLVMAsm, TY_INVALID, "s", ""),
  LLVM_BC("llvm-bc", TY_LLVMBC, TY_INVALID, "o", ""),
  PLIST("plist", TY_Plist, TY_INVALID, "plist", ""),
  PRECOMPILED_HEADER("precompiled-header", TY_PCH, TY_INVALID, "gch", "A"),
  OBJECT("object", TY_Object, TY_INVALID, "o", ""),
  TREELANG("treelang", TY_Treelang, TY_INVALID, "", "u"),
  IMAGE("image", TY_Image, TY_INVALID, "out", ""),
  DEPENDENCIES("dependencies", TY_Dependencies, TY_INVALID, "d", ""),
  NONE("none", TY_Nothing, TY_INVALID, "", "u");

  public final String name;
  public final int id;
  public final int preprocessed;
  public final String tempSuffix;
  public final String flags;

  InputType(String name, int id, int preprocessed, String tempSuffix, String flags) {
    this.name = name;
    this.id = id;
    this.preprocessed = preprocessed;
    this.tempSuffix = tempSuffix;
    this.flags = flags;
  }


  public static int lookupTypeForTypeSpecifier(String name) {
    for (int i = 0; i < values().length; i++) {
      if (canBeUserSpecified(i) && values()[i].name.equals(name))
        return i;
    }
    return TY_INVALID;
  }

  public static boolean canBeUserSpecified(int id) {
    return values()[id].flags.contains("u");
  }

  public static int lookupTypeForExtension(String ext) {
    switch (ext) {
      case "c":
        return TY_C;
      case "i":
        return TY_PP_C;
      case "m":
        return TY_ObjC;
      case "M":
        return TY_ObjCXX;
      case "h":
        return TY_CHeader;
      case "C":
        return TY_CXX;
      case "H":
        return TY_CXXHeader;
      case "f":
        return TY_PP_Fortran;
      case "F":
        return TY_Fortran;
      case "s":
        return TY_PP_Asm;
      case "S":
        return TY_Asm;
      case "ii":
        return TY_PP_CXX;
      case "mi":
        return TY_PP_ObjC;
      case "mm":
        return TY_ObjCXX;
      case "cc":
        return TY_CXX;
      case "cp":
        return TY_CXX;
      case "hh":
        return TY_CXXHeader;
      case "ads":
        return TY_Ada;
      case "adb":
        return TY_Ada;
      case "cxx":
        return TY_CXX;
      case "cpp":
        return TY_CXX;
      case "CPP":
        return TY_CXX;
      case "cXX":
        return TY_CXX;
      case "for":
        return TY_PP_Fortran;
      case "FOR":
        return TY_PP_Fortran;
      case "fpp":
        return TY_Fortran;
      case "FPP":
        return TY_Fortran;
      case "f90":
        return TY_PP_Fortran;
      case "f95":
        return TY_PP_Fortran;
      case "F90":
        return TY_Fortran;
      case "F95":
        return TY_Fortran;
      case "mii":
        return TY_PP_ObjCXX;
      default:
        return TY_INVALID;
    }
  }

  public static int getNumCompilationPhases(int id) {
    if (id == TY_Object)
      return 1;

    int n = 0;
    if (getPreprocessedType(id) != TY_INVALID)
      n += 1;

    if (onlyAssembleType(id))
      return n + 2;
    if (onlyPrecompileType(id))
      return n + 1;

    return n + 3;
  }

  public static int getCompilationPhase(int id, int n) {
    Util.assertion(n < getNumCompilationPhases(id), "Invalid index");

    if (id == TY_Object)
      return CompilationPhase.Link;

    if (getPreprocessedType(id) != TY_INVALID) {
      if (n == 0)
        return CompilationPhase.Preprocess;
      --n;
    }
    if (onlyAssembleType(id))
      return n == 0 ? CompilationPhase.Assemble : CompilationPhase.Link;

    if (onlyPrecompileType(id))
      return CompilationPhase.Precompile;

    switch (n) {
      case 0:
        return CompilationPhase.Compile;
      case 1:
        return CompilationPhase.Assemble;
      default:
        return CompilationPhase.Link;
    }
  }

  public static int getPreprocessedType(int id) {
    return values()[id].preprocessed;
  }

  public static String getTypeName(int id) {
    return values()[id].name;
  }

  public static String getTypeTempSuffix(int id) {
    return values()[id].tempSuffix;
  }

  public static boolean onlyAssembleType(int id) {
    return values()[id].flags.contains("a");
  }

  public static boolean onlyPrecompileType(int id) {
    return values()[id].flags.contains("p");
  }

  public static boolean appendSuffixForType(int id) {
    return values()[id].flags.contains("A");
  }

  public static boolean isAcceptedByJlang(int id) {
    switch (id) {
      case TY_C:
        return true;
      default:
        return false;
    }
  }
}
