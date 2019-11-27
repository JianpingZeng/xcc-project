package backend.target.mips;
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
public class MipsTargetInfo {
  private static Target theMipsTarget = new Target();
  private static Target theMipselTarget = new Target();
  private static Target theMips64Target = new Target();
  private static Target theMips64elTarget = new Target();

  public static void InitializeMipsTargetInfo() {
    TargetRegistry.registerTarget(theMipsTarget, "mips",
        "Mips", Triple.ArchType.mips, false);
    TargetRegistry.registerTarget(theMipselTarget, "mipsel",
        "Mipsel", Triple.ArchType.mipsel, false);
    TargetRegistry.registerTarget(theMips64Target, "mips64",
        "Mips64 [experimental]", Triple.ArchType.mips, false);
    TargetRegistry.registerTarget(theMips64elTarget, "mips64el",
        "Mips64el [experimental]", Triple.ArchType.mips, false);
  }

  private static Target.AsmInfoCtor createTargetAsmInfo = (t, triple) ->
  {
    //Triple theTriple = new Triple(triple);
    // only mips on ELF target.
    return new MipsELFMCAsmInfo();
  };

  private static Target.AsmPrinterCtor createMipsAsmPrinter = (os, tm, ctx, streamer, mai) -> {
    assert tm instanceof MipsTargetMachine;
    return new MipsAsmPrinter(os, tm, ctx, streamer, mai);
  };

  private static Target.MCInstPrinterCtor createMipsMCInstPrinter = (AsmWriterFlavorTy ty,
                                                                     PrintStream os,
                                                                     MCAsmInfo mai) -> {
    return MipsInstPrinter.createMipsInstPrinter(os, mai);
  };

  public static void InitializeMipsTarget() {
    TargetRegistry.registerTargetMachine(theMipsTarget, MipsebTargetMachine::new);
    TargetRegistry.registerTargetMachine(theMipselTarget, MipselTargetMachine::new);
    TargetRegistry.registerTargetMachine(theMips64Target, Mips64ebTargetMachine::new);
    TargetRegistry.registerTargetMachine(theMips64elTarget, Mips64elTargetMachine::new);

    TargetRegistry.registerAsmInfo(theMipsTarget, createTargetAsmInfo);
    TargetRegistry.registerAsmInfo(theMipselTarget, createTargetAsmInfo);
    TargetRegistry.registerAsmInfo(theMips64Target, createTargetAsmInfo);
    TargetRegistry.registerAsmInfo(theMips64elTarget, createTargetAsmInfo);

    TargetRegistry.registerAsmPrinter(theMipsTarget, createMipsAsmPrinter);
    TargetRegistry.registerAsmPrinter(theMipselTarget, createMipsAsmPrinter);
    TargetRegistry.registerAsmPrinter(theMips64Target, createMipsAsmPrinter);
    TargetRegistry.registerAsmPrinter(theMips64elTarget, createMipsAsmPrinter);

    TargetRegistry.registerMCInstPrinter(theMipsTarget, createMipsMCInstPrinter);
    TargetRegistry.registerMCInstPrinter(theMipselTarget, createMipsMCInstPrinter);
    TargetRegistry.registerMCInstPrinter(theMips64Target, createMipsMCInstPrinter);
    TargetRegistry.registerMCInstPrinter(theMips64elTarget, createMipsMCInstPrinter);
  }
}
