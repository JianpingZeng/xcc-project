package backend.mc;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
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

import static backend.mc.MCAsmInfo.MCSymbolAttr.MCSA_Global;
import static backend.mc.MCAsmInfo.MCSymbolAttr.MCSA_PrivateExtern;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class MCAsmInfoDarwin extends MCAsmInfo {
  public MCAsmInfoDarwin() {
    super();
    // Common settings for all Darwin targets.
    // Syntax:
    GlobalPrefix = "_";
    PrivateGlobalPrefix = "L";
    LinkerPrivateGlobalPrefix = "l";
    AllowQuotesInName = true;
    HasSingleParameterDotFile = false;
    HasSubsectionsViaSymbols = true;

    AlignmentIsInBytes = false;
    COMMDirectiveAlignmentIsInBytes = false;
    InlineAsmStart = " InlineAsm Start";
    InlineAsmEnd = " InlineAsm End";

    // Directives:
    WeakDefDirective = "\t.weak_definition ";
    WeakRefDirective = "\t.weak_reference ";
    ZeroDirective = "\t.space\t";  // ".space N" emits N zeros.
    HasMachoZeroFillDirective = true;  // Uses .zerofill
    HasStaticCtorDtorReferenceInStaticMode = true;

    CodeBegin = "L$start$code$";
    DataBegin = "L$start$data$";
    JT8Begin  = "L$start$jt8$";
    JT16Begin = "L$start$jt16$";
    JT32Begin = "L$start$jt32$";
    SupportsDataRegions = true;

    HiddenVisibilityAttr = MCSA_PrivateExtern;
    // Doesn't support protected visibility.
    ProtectedVisibilityAttr = MCSA_Global;


    HasDotTypeDotSizeDirective = false;
    HasNoDeadStrip = true;
    // Note: Even though darwin has the .lcomm directive, it is just a synonym for
    // zerofill, so we prefer to use .zerofill.

    // _foo.eh symbols are currently always exported so that the linker knows
    // about them.  This is not necessary on 10.6 and later, but it
    // doesn't hurt anything.
    // FIXME: I need to get this from Triple.
    Is_EHSymbolPrivate = false;
    GlobalEHDirective = "\t.globl\t";
    SupportsWeakOmittedEHFrame = false;
  }
}
