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

import backend.codegen.AsmPrinter;
import backend.codegen.MachineInstr;
import backend.codegen.MachineOperand;
import backend.mc.*;
import tools.Util;

import java.io.PrintStream;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ARMAsmPrinter extends AsmPrinter {
  private ARMMCInstLower instLowering;
  public ARMAsmPrinter(PrintStream os,
                       ARMTargetMachine tm,
                       MCSymbol.MCContext ctx,
                       MCStreamer streamer,
                       MCAsmInfo mai) {
    super(os, tm, ctx, streamer, mai);
  }

  @Override
  protected void emitInstruction(MachineInstr mi) {
    if (instLowering == null)
      instLowering = new ARMMCInstLower(outContext, mangler, this);

    MCInst inst = new MCInst();
    instLowering.lower(mi, inst);
    outStreamer.emitInstruction(inst);
  }

  @Override
  public String getPassName() {
    return "ARM Assembly Printer";
  }

  public MCOperand getSymbolRef(MachineOperand mo, MCSymbol symbol) {
    MCExpr expr;
    switch (mo.getTargetFlags()) {
      default:{
        expr = MCSymbolRefExpr.create(symbol, MCSymbolRefExpr.VariantKind.VK_None);
        switch (mo.getTargetFlags()) {
          default:
            Util.shouldNotReachHere("Unknown target flag on symbol operand");
          case 0:
            break;
          case ARMII.MO_LO16:
            expr = MCSymbolRefExpr.create(symbol, MCSymbolRefExpr.VariantKind.VK_None);
            expr = ARMExpr.createExprLower16(expr);
            break;
          case ARMII.MO_HI16:
            expr = MCSymbolRefExpr.create(symbol, MCSymbolRefExpr.VariantKind.VK_None);
            expr = ARMExpr.createExprUpper16(expr);
            break;
        }
      }
      break;

      case ARMII.MO_PLT:
        expr = MCSymbolRefExpr.create(symbol, MCSymbolRefExpr.VariantKind.VK_ARM_PLT);
        break;
    }
    if (!mo.isJumpTableIndex() && mo.getOffset() != 0)
      expr = MCBinaryExpr.createAdd(expr, MCConstantExpr.create(mo.getOffset(), outContext), outContext);

    return MCOperand.createExpr(expr);
  }
}
