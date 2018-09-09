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
import backend.target.DarwinTargetAsmInfo;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class X86DarwinTargetAsmInfo extends DarwinTargetAsmInfo {
  public X86DarwinTargetAsmInfo(Triple triple) {
    super();
    asmTransCBE = x86_asm_table;
    assemblerDialect = BackendCmdOptions.AsmWriterFlavor.value;
    boolean is64Bit = triple.getArch() == Triple.ArchType.x86_64;
    TextAlignFillValue = 0x90;
    if (!is64Bit)
      Data64bitsDirective = "";
    COMMDirectiveTakesAlignment = triple.getDarwinMajorNumber() > 9;
    if (is64Bit) {
      PersonalityPrefix = "";
      PersonalitySuffix = "+4@GOTPCREL";
    } else {
      PersonalityPrefix = "L";
      PersonalitySuffix = "$non_lazy_ptr";
    }

    CommentString = "##";
    pcSymbol = ".";
    supportsDebugInformation = true;
    DwarfUsesInlineInfoSection = true;
    absoluteEHSectionOffsets = false;
  }
}
