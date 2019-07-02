/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2019, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.target.x86;

import backend.codegen.TargetLoweringObjectFileELF;

import static backend.support.DwarfConstants.*;
import static backend.target.TargetMachine.RelocModel.PIC_;

public class X86_32ELFTargetObjectFile extends TargetLoweringObjectFileELF {
  private X86TargetMachine tm;

  public X86_32ELFTargetObjectFile(X86TargetMachine tm) {
    this.tm = tm;
  }

  @Override
  public int getPersonalityEncoding() {
    if (tm.getRelocationModel() == PIC_)
      return DW_EH_PE_indirect | DW_EH_PE_pcrel | DW_EH_PE_sdata4;
    return DW_EH_PE_absptr;
  }

  @Override
  public int getLSDAEncoding() {
    if (tm.getRelocationModel() == PIC_)
      return DW_EH_PE_pcrel | DW_EH_PE_sdata4;
    else
      return DW_EH_PE_absptr;
  }

  @Override
  public int getFDEEncoding() {
    if (tm.getRelocationModel() == PIC_)
      return DW_EH_PE_pcrel | DW_EH_PE_sdata4;
    else
      return DW_EH_PE_absptr;
  }

  @Override
  public int getTTypeEncoding() {
    if (tm.getRelocationModel() == PIC_)
      return DW_EH_PE_indirect | DW_EH_PE_pcrel | DW_EH_PE_sdata4;
    else
      return DW_EH_PE_absptr;
  }
}
