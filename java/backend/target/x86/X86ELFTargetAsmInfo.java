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

import backend.support.BackendCmdOptions;
import backend.support.Triple;
import backend.target.TargetAsmInfo;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class X86ELFTargetAsmInfo extends TargetAsmInfo
{
    public X86ELFTargetAsmInfo(Triple theTriple)
    {
        asmTransCBE = x86_asm_table;
        assemblerDialect = BackendCmdOptions.AsmWriterFlavor.value;

        privateGlobalPrefix = ".L";
        weakRefDirective = "\t.weak\t";
        setDirective = "\t.set\t";
        pcSymbol = ".";

        // Set up DWARF directives
        hasLEB128 = true;  // Target asm supports leb128 directives (little-endian)

        // Debug Information
        absoluteDebugSectionOffsets = true;
        supportsDebugInformation = true;

        // Exceptions handling
        // ExceptionsType = ExceptionHandling::Dwarf;
        absoluteEHSectionOffsets = false;

        // On Linux we must declare when we can use a non-executable stack.
        if (theTriple.getOS() == Triple.OSType.Linux)
            nonexecutableStackDirective = "\t.section\t.note.GNU-stack,\"\",@progbits";
    }
}
