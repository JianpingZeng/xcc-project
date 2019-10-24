package backend.target.arm;
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

import backend.codegen.MachineInstr;
import backend.codegen.MachineOperand;
import backend.mc.MCInst;
import backend.mc.MCOperand;
import backend.mc.MCSymbol;
import backend.mc.MCSymbolRefExpr;
import backend.support.NameMangler;
import tools.APFloat;
import tools.OutRef;
import tools.Util;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ARMMCInstLower {
  private MCSymbol.MCContext ctx;
  private NameMangler mangler;
  private ARMAsmPrinter printer;

  public ARMMCInstLower(MCSymbol.MCContext ctx, NameMangler mangler, ARMAsmPrinter printer) {
    this.ctx = ctx;
    this.mangler = mangler;
    this.printer = printer;
  }

  private MCOperand lowerOperand(MachineOperand mo) {
    switch (mo.getType()) {
      default:
        Util.assertion("unknown machine operand type");
        break;
      case MO_Register:
        if (mo.isImplicit() && mo.getReg() != ARMGenRegisterNames.CPSR)
          break;
        Util.assertion(mo.getSubReg() == 0, "subregs should be eliminated");
        return MCOperand.createReg(mo.getReg());
      case MO_Immediate:
        return MCOperand.createImm(mo.getImm());
      case MO_MachineBasicBlock:
        return MCOperand.createExpr(MCSymbolRefExpr.create(mo.getMBB().getSymbol(ctx)));
      case MO_GlobalAddress:
        return printer.getSymbolRef(mo, mangler.getSymbol(mo.getGlobal()));
      case MO_ExternalSymbol:
        return printer.getSymbolRef(mo, printer.getExternalSymbolSymbol(mo.getSymbolName()));
      case MO_JumpTableIndex:
        return printer.getSymbolRef(mo, printer.getJTISymbol(mo.getIndex(), false));
      case MO_ConstantPoolIndex:
        return printer.getSymbolRef(mo, printer.getCPISymbol(mo.getIndex()));
      case MO_BlockAddress:
        return printer.getSymbolRef(mo, printer.getBlockAddressSymbol(mo.getBlockAddress()));
    }
    return null;
  }

  public void lower(MachineInstr mi, MCInst inst) {
    inst.setOpcode(mi.getOpcode());
    for (int i = 0, e = mi.getNumOperands(); i <e; i++) {
      MachineOperand mo = mi.getOperand(i);
      MCOperand mop = lowerOperand(mo);
      if (mop != null)
        inst.addOperand(mop);
    }
  }
}
