/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */
package backend.mc;

import static backend.mc.MCAsmInfo.MCSymbolAttr.MCSA_Invalid;

public class MCAsmInfoCOFF extends MCAsmInfo{
  public MCAsmInfoCOFF() {
    GlobalPrefix = "_";
    COMMDirectiveAlignmentIsInBytes = false;
    HasLCOMMDirective = true;
    HasDotTypeDotSizeDirective = false;
    HasSingleParameterDotFile = false;
    PrivateGlobalPrefix = "L";  // Prefix for private global symbols
    WeakRefDirective = "\t.weak\t";
    LinkOnceDirective = "\t.linkonce discard\n";

    // Doesn't support visibility:
    HiddenVisibilityAttr = ProtectedVisibilityAttr = MCSA_Invalid;

    // Set up DWARF directives
    HasLEB128 = true;  // Target asm supports leb128 directives (little-endian)
    AbsoluteDebugSectionOffsets = true;
    AbsoluteEHSectionOffsets = false;
    SupportsDebugInformation = true;
    DwarfSectionOffsetDirective = "\t.secrel32\t";
  }
}
