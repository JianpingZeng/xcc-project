package backend.target.mips;
/*
 * Extremely C language Compiler
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

import backend.codegen.MachineInstr;
import backend.codegen.MachineOperand;
import backend.mc.MCInst;
import backend.mc.MCOperand;
import backend.mc.MCSymbol;
import backend.support.NameMangler;
import tools.Util;

/**
 * This class is used to lower a MachineInstr to a Mips-specific MCInst.
 * @author Jianping Zeng.
 * @version 0.4
 */
public class MipsMCInstLower {
  private MCSymbol.MCContext ctx;
  private NameMangler mangler;
  private MipsAsmPrinter asmPrinter;

  public MipsMCInstLower(NameMangler mangler, MCSymbol.MCContext context,
                         MipsAsmPrinter printer) {
    this.ctx = context;
    this.mangler = mangler;
    asmPrinter = printer;
  }

  public void lower(MachineInstr mi, MCInst outInst) {
    outInst.setOpcode(mi.getOpcode());

    for (int i = 0, e = mi.getNumOperands(); i < e; ++i) {
      MachineOperand mo = mi.getOperand(i);
      MCOperand mcop = lowerOperand(mo);
      if (mcop != null && mcop.isValid())
        outInst.addOperand(mcop);
    }
  }

  private MCOperand lowerSymbolOperand(MachineOperand mo,
                                       int offset) {
    MachineOperand.MachineOperandType moTy = mo.getType();
    MipsMCSymbolRefExpr.VariantKind kind;
    MCSymbol symbol = null;

    switch (mo.getTargetFlags()) {
      default: Util.assertion("Invalid target flag!");
      case MipsII.MO_NO_FLAG: kind = MipsMCSymbolRefExpr.VariantKind.VK_Mips_None; break;
      case MipsII.MO_GPREL: kind = MipsMCSymbolRefExpr.VariantKind.VK_Mips_GPREL; break;
      case MipsII.MO_GOT_CALL: kind = MipsMCSymbolRefExpr.VariantKind.VK_Mips_GOT_CALL; break;
      case MipsII.MO_GOT: kind = MipsMCSymbolRefExpr.VariantKind.VK_Mips_GOT; break;
      case MipsII.MO_ABS_HI: kind = MipsMCSymbolRefExpr.VariantKind.VK_Mips_ABS_HI; break;
      case MipsII.MO_ABS_LO: kind = MipsMCSymbolRefExpr.VariantKind.VK_Mips_ABS_LO; break;
      case MipsII.MO_TLSGD: kind = MipsMCSymbolRefExpr.VariantKind.VK_Mips_TLSGD; break;
      case MipsII.MO_GOTTPREL: kind = MipsMCSymbolRefExpr.VariantKind.VK_Mips_GOTTPREL; break;
      case MipsII.MO_TPREL_HI: kind = MipsMCSymbolRefExpr.VariantKind.VK_Mips_TPREL_HI; break;
      case MipsII.MO_TPREL_LO: kind = MipsMCSymbolRefExpr.VariantKind.VK_Mips_TPREL_LO; break;
      case MipsII.MO_GPOFF_HI: kind = MipsMCSymbolRefExpr.VariantKind.VK_Mips_GPOFF_HI; break;
      case MipsII.MO_GPOFF_LO: kind = MipsMCSymbolRefExpr.VariantKind.VK_Mips_GPOFF_LO; break;
      case MipsII.MO_GOT_DISP: kind = MipsMCSymbolRefExpr.VariantKind.VK_Mips_GOT_DISP; break;
      case MipsII.MO_GOT_PAGE: kind = MipsMCSymbolRefExpr.VariantKind.VK_Mips_GOT_PAGE; break;
      case MipsII.MO_GOT_OFST: kind = MipsMCSymbolRefExpr.VariantKind.VK_Mips_GOT_OFST; break;
    }

    switch (mo.getType()) {
      case MO_MachineBasicBlock:
        symbol = mo.getMBB().getSymbol(ctx);
        break;
      case MO_GlobalAddress:
        symbol = mangler.getSymbol(mo.getGlobal());
        break;
      case MO_BlockAddress:
        symbol = asmPrinter.getBlockAddressSymbol(mo.getBlockAddress());
        break;
      case MO_ExternalSymbol:
        symbol = asmPrinter.getExternalSymbolSymbol(mo.getSymbolName());
        break;
      case MO_JumpTableIndex:
        symbol = asmPrinter.getJTISymbol(mo.getIndex(), false);
        break;
      case MO_ConstantPoolIndex:
        symbol = asmPrinter.getCPISymbol(mo.getIndex());
        if (mo.getOffset() != 0)
          offset += mo.getOffset();
        break;
      default:
        Util.assertion("Unknown machine operand type!");
    }
    return MCOperand.createExpr(MipsMCSymbolRefExpr.create(kind, symbol, offset));
  }

  private MCOperand lowerOperand(MachineOperand mo) {
    switch (mo.getType()) {
      default:
        Util.assertion("unknown machine operand type!");
        return null;
      case MO_Immediate:
        return MCOperand.createImm(mo.getImm());
      case MO_GlobalAddress:
      case MO_ExternalSymbol:
      case MO_JumpTableIndex:
      case MO_ConstantPoolIndex:
      case MO_MachineBasicBlock:
      case MO_BlockAddress:
        return lowerSymbolOperand(mo, 0);
      case MO_Register:
        if (mo.isImplicit())
          break;
        return MCOperand.createReg(mo.getReg());
    }
    return null;
  }
}
