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

import backend.codegen.TargetLoweringObjectFileELF;
import backend.mc.MCSection;
import backend.mc.MCSectionELF;
import backend.mc.MCSymbol;
import backend.support.NameMangler;
import backend.target.SectionKind;
import backend.target.TargetMachine;
import backend.type.Type;
import backend.value.GlobalValue;
import backend.value.GlobalVariable;
import tools.commandline.IntOpt;
import tools.commandline.OptionHidden;
import tools.commandline.OptionHiddenApplicator;

import static tools.commandline.Desc.desc;
import static tools.commandline.Initializer.init;
import static tools.commandline.OptionNameApplicator.optionName;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class MipsTargetObjectFile extends TargetLoweringObjectFileELF {
  static final IntOpt SSThreshold = new IntOpt(
      optionName("mips-ssection-threshold"),
      new OptionHiddenApplicator(OptionHidden.Hidden),
      desc("Small data and bss section threshold size (default to 8)"),
      init(8));

  private MCSection smallDataSection;
  private MCSection smallBSSSection;

  @Override
  public void initialize(MCSymbol.MCContext ctx, TargetMachine tm) {
    super.initialize(ctx, tm);
    smallDataSection = getContext().getELFSection(".sdata", MCSectionELF.SHT_PROGBITS,
        MCSectionELF.SHF_WRITE | MCSectionELF.SHF_ALLOC,
        SectionKind.getDataRel());
    smallBSSSection = getContext().getELFSection(".sbss", MCSectionELF.SHT_NOBITS,
        MCSectionELF.SHF_WRITE | MCSectionELF.SHF_ALLOC,
        SectionKind.getBSS());
  }

  @Override
  public MCSection selectSectionForGlobal(GlobalValue gv, SectionKind kind,
                                          NameMangler mang, TargetMachine tm) {
    return super.selectSectionForGlobal(gv, kind, mang, tm);
  }

  public boolean isGlobalInSmallSection(GlobalValue gv,
                                        MipsTargetMachine tm,
                                        SectionKind kind) {
    MipsSubtarget subtarget = tm.getSubtarget();
    if (subtarget.isLinux())
      return false;

    if (!(gv instanceof GlobalVariable))
      return false;

    GlobalVariable gvar = (GlobalVariable) gv;
    if (!kind.isBSS() && !kind.isDataRel())
      return false;

    if (kind.isMergeable1ByteCString())
      return false;

    Type ty = gv.getType().getElementType();
    long size = tm.getTargetData().getTypeAllocSize(ty);
    return size > 0 && size <= SSThreshold.value;
  }

  public boolean isGlobalInSmallSection(GlobalValue gv,
                                        MipsTargetMachine tm) {
    if (gv.isDeclaration() || gv.hasAvailableExternallyLinkage())
      return false;

    return isGlobalInSmallSection(gv, tm, getKindForGlobal(gv, tm));
  }
}
