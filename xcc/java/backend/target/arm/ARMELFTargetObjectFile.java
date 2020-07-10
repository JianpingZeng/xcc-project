package backend.target.arm;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
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
import backend.target.SectionKind;
import backend.target.TargetMachine;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ARMELFTargetObjectFile extends TargetLoweringObjectFileELF {
  private MCSection attributeSection;
  public ARMELFTargetObjectFile() {
    super();
    attributeSection = null;
  }

  @Override
  public void initialize(MCSymbol.MCContext ctx, TargetMachine tm) {
    super.initialize(ctx, tm);
    ARMSubtarget subtarget = (ARMSubtarget) tm.getSubtarget();
    if (subtarget.isAAPCS_ABI()) {
      StaticCtorSection = getContext().getELFSection(".init_array",
          MCSectionELF.SHT_INIT_ARRAY,
          MCSectionELF.SHF_WRITE | MCSectionELF.SHF_ALLOC,
          SectionKind.getDataRel());

      StaticDtorSection = getContext().getELFSection(".fnit_array",
          MCSectionELF.SHT_FINI_ARRAY,
          MCSectionELF.SHF_WRITE | MCSectionELF.SHF_ALLOC,
          SectionKind.getDataRel());
      LSDASection = null;
    }
    attributeSection = getContext().getELFSection(".ARM.attributes",
        MCSectionELF.SHT_ARM_ATTRIBUTES,
        0,
        SectionKind.getMetadata());
  }

  public MCSection getAttributeSection() { return attributeSection; }
}
