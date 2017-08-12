package backend.target.x86;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

import backend.target.Target;
import backend.target.Target.TargetRegistry;
import backend.support.Triple;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86TargetInfo
{
    public static Target theX86_32Target = new Target();
    public static Target theX86_64Target = new Target();

    public static void InitializeX86TargetInfo()
    {
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
    public static Target.TargetMachineCtor X86_32TargetMachineMaker =
            (t, triple, features) -> new X86_32TargetMachine(t, triple, features);

    /**
     * A function interface variable to create X86 64 bit target machine.
     */
    public static Target.TargetMachineCtor X86_64TargetMachineMaker =
            (t, triple, features) -> new X86_64TargetMachine(t, triple, features);

    public static Target.AsmInfoCtor createTargetAsmInfo = (t, triple) ->
    {
        Triple theTriple = new Triple(triple);
        switch (theTriple.getOS())
        {
            default: return new X86ELFTargetAsmInfo(theTriple);
        }
    };

    public static Target.AsmPrinterCtor createX86AsmPrinter = (os, tm, asmInfo, verbose) ->
    {
        /*
        FIXME currently only supporting ATT syntax assembler.
        if (tm.getTargetAsmInfo().getAssemblerDialect() == 1)
            return new X86IntelAsmPrinter(os, tm, asmInfo, verbose);
        */
        return X86ATTAsmPrinter.createX86AsmCodeEmitter(os, (X86TargetMachine) tm, asmInfo, verbose);
    };

    public static void LLVMInitiliazeX86Target()
    {
        TargetRegistry.registerTargetMachine(theX86_32Target, X86_32TargetMachineMaker);
        TargetRegistry.registerTargetMachine(theX86_64Target, X86_64TargetMachineMaker);

        TargetRegistry.registerAsmInfo(theX86_32Target, createTargetAsmInfo);
        TargetRegistry.registerAsmInfo(theX86_64Target, createTargetAsmInfo);

        TargetRegistry.registerAsmPrinter(theX86_32Target, createX86AsmPrinter);
    }
}
