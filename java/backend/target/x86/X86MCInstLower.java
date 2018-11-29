/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.target.x86;

import backend.codegen.MachineInstr;
import backend.codegen.MachineOperand;
import backend.mc.MCInst;
import backend.mc.MCOperand;
import backend.mc.MCSymbol;
import backend.mc.MCSymbol.MCContext;
import backend.support.NameMangler;

/**
 * This class is used to lower an MachineInstr into an MCInst.
 */
public class X86MCInstLower {
  private MCContext ctx;
  private NameMangler mangler;
  private X86AsmPrinter asmPrinter;

  X86MCInstLower(MCContext ctx, NameMangler mangler, X86AsmPrinter asmPrinter) {
    this.ctx = ctx;
    this.mangler = mangler;
    this.asmPrinter = asmPrinter;
  }

  private X86Subtarget getSubtarget() {
    return asmPrinter.getSubtarget();
  }
  public MCSymbol getPICBaseSymbol() {
    return null;
  }

  public MCSymbol getSymbolFromOperand(MachineOperand operand) {
    return null;
  }

  public MCOperand lowerSymbolOperand(MachineOperand mo, MCSymbol sym) {

  }
  public void lower(MachineInstr mi, MCInst inst) {

  }
}
