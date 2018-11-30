package backend.target.x86;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import backend.codegen.AsmWriterFlavorTy;
import backend.mc.MCAsmInfo;
import backend.mc.MCInstPrinter;
import backend.mc.MCStreamer;
import backend.mc.MCSymbol;
import backend.support.Triple;
import backend.target.Target;
import backend.target.Target.TargetRegistry;
import backend.target.TargetMachine;

import java.io.PrintStream;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class X86TargetInfo {
  private static Target theX86_32Target = new Target();
  private static Target theX86_64Target = new Target();

  static void InitializeX86TargetInfo() {
    TargetRegistry.registerTarget(theX86_32Target, "x86",
        "32-bit X86: Pentium-Pro and above",
        Triple.ArchType.x86, false);

    TargetRegistry.registerTarget(theX86_64Target, "x86-64",
        "64-bit X86: EM64T and AMD64",
        Triple.ArchType.x86_64, false);
  }

  /**
   * A function interface variable to create X86 32bit target machine.
   */
  private static Target.TargetMachineCtor X86_32TargetMachineMaker =
      (t, triple, features) -> new X86_32TargetMachine(t, triple, features);

  /**
   * A function interface variable to create X86 64 bit target machine.
   */
  private static Target.TargetMachineCtor X86_64TargetMachineMaker =
      (t, triple, features) -> new X86_64TargetMachine(t, triple, features);

  private static Target.AsmInfoCtor createTargetAsmInfo = (t, triple) ->
  {
    Triple theTriple = new Triple(triple);
    switch (theTriple.getOS()) {
      case Darwin:
        return new X86MCAsmInfoDarwin(theTriple);
      default:
        return new X86ELFMCAsmInfo(theTriple);
    }
  };

  private static Target.AsmPrinterCtor createX86AsmPrinter = (os, tm, ctx, streamer, mai) -> {
    assert tm instanceof X86TargetMachine;
    return new X86AsmPrinter(os, (X86TargetMachine) tm, ctx, streamer, mai);
  };

  private static Target.MCInstPrinterCtor createX86MCInstPrinter = (AsmWriterFlavorTy ty,
                                                                    PrintStream os,
                                                                    MCAsmInfo mai) -> {
    return X86ATTInstPrinter.createX86ATTInstPrinter(os, mai);
    // TODO X86 sytle asm printer not supported;
  };

  public static void LLVMInitiliazeX86Target() {
    TargetRegistry.registerTargetMachine(theX86_32Target, X86_32TargetMachineMaker);
    TargetRegistry.registerTargetMachine(theX86_64Target, X86_64TargetMachineMaker);

    TargetRegistry.registerAsmInfo(theX86_32Target, createTargetAsmInfo);
    TargetRegistry.registerAsmInfo(theX86_64Target, createTargetAsmInfo);

    TargetRegistry.registerAsmPrinter(theX86_32Target, createX86AsmPrinter);
    TargetRegistry.registerAsmPrinter(theX86_64Target, createX86AsmPrinter);

    TargetRegistry.registerMCInstPrinter(theX86_32Target, createX86MCInstPrinter);
    TargetRegistry.registerMCInstPrinter(theX86_64Target, createX86MCInstPrinter);
  }
}
