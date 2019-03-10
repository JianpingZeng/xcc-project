/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.target.x86;

import backend.codegen.TargetLoweringObjectFileELF;
import backend.target.TargetMachine;

import static backend.support.DwarfConstants.*;
import static backend.target.TargetMachine.RelocModel.PIC_;

public class X86_64ELFTargetObjectFile extends TargetLoweringObjectFileELF {
  private X86TargetMachine tm;
  public X86_64ELFTargetObjectFile(X86TargetMachine tm) {
    this.tm = tm;
  }

  @Override
  public int getPersonalityEncoding() {
    TargetMachine.CodeModel cm = tm.getCodeModel();
    if (tm.getRelocationModel() == PIC_)
      return DW_EH_PE_indirect | DW_EH_PE_pcrel |
          (cm == TargetMachine.CodeModel.Small ||
          cm == TargetMachine.CodeModel.Medium ?
          DW_EH_PE_sdata4 : DW_EH_PE_sdata8);

    if (cm == TargetMachine.CodeModel.Small ||
        cm == TargetMachine.CodeModel.Medium)
      return DW_EH_PE_udata4;
    return DW_EH_PE_absptr;
  }

  @Override
  public int getLSDAEncoding() {
    TargetMachine.CodeModel cm = tm.getCodeModel();
    if (tm.getRelocationModel() == PIC_)
      return DW_EH_PE_pcrel |
          (cm == TargetMachine.CodeModel.Small ?
              DW_EH_PE_sdata4 : DW_EH_PE_sdata8);

    if (cm == TargetMachine.CodeModel.Small)
      return DW_EH_PE_udata4;
    return DW_EH_PE_absptr;
  }

  @Override
  public int getFDEEncoding() {
    TargetMachine.CodeModel cm = tm.getCodeModel();
    if (tm.getRelocationModel() == PIC_)
      return DW_EH_PE_pcrel |
          (cm == TargetMachine.CodeModel.Small ||
              cm == TargetMachine.CodeModel.Medium ?
              DW_EH_PE_sdata4 : DW_EH_PE_sdata8);

    if (cm == TargetMachine.CodeModel.Small ||
        cm == TargetMachine.CodeModel.Medium)
      return DW_EH_PE_udata4;
    return DW_EH_PE_absptr;
  }

  @Override
  public int getTTypeEncoding() {
    TargetMachine.CodeModel cm = tm.getCodeModel();
    if (tm.getRelocationModel() == PIC_)
      return DW_EH_PE_indirect | DW_EH_PE_pcrel |
          (cm == TargetMachine.CodeModel.Small ||
              cm == TargetMachine.CodeModel.Medium ?
              DW_EH_PE_sdata4 : DW_EH_PE_sdata8);

    if (cm == TargetMachine.CodeModel.Small)
      return DW_EH_PE_udata4;
    return DW_EH_PE_absptr;
  }
}
