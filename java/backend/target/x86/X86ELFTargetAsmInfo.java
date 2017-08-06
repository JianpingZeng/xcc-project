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

import backend.target.TargetAsmInfo;
import jlang.system.Triple;
import tools.commandline.*;

import static backend.target.x86.X86ELFTargetAsmInfo.AsmWriterFlavorTy.ATT;
import static backend.target.x86.X86ELFTargetAsmInfo.AsmWriterFlavorTy.Intel;
import static tools.commandline.OptionNameApplicator.optionName;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86ELFTargetAsmInfo extends TargetAsmInfo
{
    public enum AsmWriterFlavorTy
    {
        ATT,
        Intel
    }

    /**
     * A command line option to control what assembly dialect would be emitedd.
     */
    public static final Opt<AsmWriterFlavorTy> AsmWriterFlavor =
            new Opt<AsmWriterFlavorTy>(new Parser<>(),
                    optionName("x86-asm-syntax"),
                    Initializer.init(ATT),
                    new ValueClass<>(
                            new ValueClass.Entry<>(ATT, "att", "Emit AT&T-style assembly"),
                            new ValueClass.Entry<>(Intel, "intel", "Emit Intel-style assembly"))
                    );

    private static String[] x86_asm_table = {
            "{si}", "S",
            "{di}", "D",
            "{ax}", "a",
            "{cx}", "c",
            "{memory}", "memory",
            "{flags}", "",
            "{dirflag}", "",
            "{fpsr}", "",
            "{cc}", "cc",
    };


    public X86ELFTargetAsmInfo(Triple theTriple)
    {
        AsmTransCBE = x86_asm_table;
        AssemblerDialect = AsmWriterFlavor.value;

        PrivateGlobalPrefix = ".L";
        WeakRefDirective = "\t.weak\t";
        SetDirective = "\t.set\t";
        PCSymbol = ".";

        // Set up DWARF directives
        HasLEB128 = true;  // Target asm supports leb128 directives (little-endian)

        // Debug Information
        AbsoluteDebugSectionOffsets = true;
        SupportsDebugInformation = true;

        // Exceptions handling
        // ExceptionsType = ExceptionHandling::Dwarf;
        AbsoluteEHSectionOffsets = false;

        // On Linux we must declare when we can use a non-executable stack.
        if (theTriple.getOS() == Triple.OSType.Linux)
            NonexecutableStackDirective = "\t.section\t.note.GNU-stack,\"\",@progbits";
    }
}