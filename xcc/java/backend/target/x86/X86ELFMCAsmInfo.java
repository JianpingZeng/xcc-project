package backend.target.x86;
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

import backend.mc.MCSectionELF;
import backend.mc.MCSymbol;
import backend.support.BackendCmdOptions;
import backend.support.Triple;
import backend.mc.MCAsmInfo;
import backend.target.SectionKind;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class X86ELFMCAsmInfo extends MCAsmInfo {

  public X86ELFMCAsmInfo(Triple theTriple) {
    AsmTransCBE = X86.x86_asm_table;
    AssemblerDialect = BackendCmdOptions.AsmWriterFlavor.value;

    TextAlignFillValue = 0x90;

    PrivateGlobalPrefix = ".L";
    WeakRefDirective = "\t.weak\t";
    PCSymbol = ".";

    // Set up DWARF directives
    HasLEB128 = true;  // Target asm supports leb128 directives (little-endian)

    // Debug Information
    AbsoluteDebugSectionOffsets = true;
    SupportsDebugInformation = true;

    // Exceptions handling
    ExceptionsType = ExceptionHandlingType.Dwarf;
    AbsoluteEHSectionOffsets = false;

    // OpenBSD has buggy support for .quad in 32-bit mode, just split into two
    // .words.
    if (theTriple.getOS() == Triple.OSType.OpenBSD && theTriple.getArch() == Triple.ArchType.x86)
      Data64bitsDirective = null;
  }

  @Override
  public MCSectionELF getNonexecutableStackSection(MCSymbol.MCContext Ctx) {
    return MCSectionELF.create(".note.GNU-stack", MCSectionELF.SHT_PROGBITS,
        0, SectionKind.getMetadata(), false, Ctx);
  }
}
