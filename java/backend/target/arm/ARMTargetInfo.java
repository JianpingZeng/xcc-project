package backend.target.arm;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
 * All rights reserved.
 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import backend.codegen.AsmWriterFlavorTy;
import backend.mc.MCAsmInfo;
import backend.support.Triple;
import backend.target.Target;
import backend.target.Target.TargetRegistry;

import java.io.PrintStream;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ARMTargetInfo {
  private static Target theARMTarget = new Target();

  public static void InitializeARMTargetInfo() {
    TargetRegistry.registerTarget(theARMTarget, "arm",
        "ARM", Triple.ArchType.arm, false);
  }

  /**
   * A function interface variable to create ARM target machine.
   */
  private static Target.TargetMachineCtor ARMTargetMachineMaker =
      (t, triple, cpu, features) -> new ARMTargetMachine(t, triple, cpu, features);


  private static Target.AsmInfoCtor createTargetAsmInfo = (t, triple) ->
  {
    //Triple theTriple = new Triple(triple);
    // only arm on ELF target.
    return new ARMELFMCAsmInfo();
  };

  private static Target.AsmPrinterCtor createARMAsmPrinter = (os, tm, ctx, streamer, mai) -> {
    assert tm instanceof ARMTargetMachine;
    return new ARMAsmPrinter(os, (ARMTargetMachine) tm, ctx, streamer, mai);
  };

  private static Target.MCInstPrinterCtor createARMMCInstPrinter = (AsmWriterFlavorTy ty,
                                                                    PrintStream os,
                                                                    MCAsmInfo mai) -> {
    return ARMInstPrinter.createARMInstPrinter(os, mai);
  };

  public static void InitiliazeARMTarget() {
    TargetRegistry.registerTargetMachine(theARMTarget, ARMTargetMachineMaker);
    TargetRegistry.registerAsmInfo(theARMTarget, createTargetAsmInfo);
    TargetRegistry.registerAsmPrinter( theARMTarget, createARMAsmPrinter);
    TargetRegistry.registerMCInstPrinter(theARMTarget, createARMMCInstPrinter);
  }
}
