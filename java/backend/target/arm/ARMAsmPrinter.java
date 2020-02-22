package backend.target.arm;
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

import backend.codegen.*;
import backend.mc.*;
import backend.target.FloatABI;
import backend.target.TargetMachine;
import backend.value.GlobalValue;
import backend.value.Module;
import tools.Util;

import java.io.PrintStream;
import java.util.ArrayList;

import static backend.support.BackendCmdOptions.*;
import static backend.target.TargetOptions.FloatABIForType;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ARMAsmPrinter extends AsmPrinter {
  private ARMMCInstLower instLowering;

  public ARMAsmPrinter(PrintStream os,
                       TargetMachine tm,
                       MCSymbol.MCContext ctx,
                       MCStreamer streamer,
                       MCAsmInfo mai) {
    super(os, tm, ctx, streamer, mai);
  }

  private static void populateADROperands(MCInst inst, int dest, MCSymbol label,
                                          long pred, int ccreg, MCSymbol.MCContext ctx) {
    MCExpr symbolExpr = MCSymbolRefExpr.create(label);
    inst.addOperand(MCOperand.createReg(dest));
    inst.addOperand(MCOperand.createExpr(symbolExpr));
    inst.addOperand(MCOperand.createImm(pred));
    inst.addOperand(MCOperand.createReg(ccreg));
  }

  @Override
  protected void emitInstruction(MachineInstr mi) {
    if (instLowering == null)
      instLowering = new ARMMCInstLower(this);

    // If this instruction is a pseudo and can be lowering to other instruction,
    // return immediately.
    if (ARMGenMCPseudoLowering.emitPseudoExpansionLowering(outStreamer, mi, this))
      return;

    int opc = mi.getOpcode();
    switch (opc) {
      case ARMGenInstrNames.t2MOVi32imm: {
        Util.assertion("Should be lowered by thumb2it pass");
        return;
      }
      case ARMGenInstrNames.DBG_VALUE: {
        // FIXME, print debug information.
        return;
      }
      case ARMGenInstrNames.LEApcrel:
      case ARMGenInstrNames.tLEApcrel:
      case ARMGenInstrNames.t2LEApcrel: {
        MCInst tmpInst = new MCInst();
        tmpInst.setOpcode(opc == ARMGenInstrNames.t2LEApcrel ? ARMGenInstrNames.t2ADR :
            opc == ARMGenInstrNames.tLEApcrel ? ARMGenInstrNames.tADR : ARMGenInstrNames.ADR);
        populateADROperands(tmpInst, mi.getOperand(0).getReg(), getCPISymbol(mi.getOperand(1).getIndex()),
            mi.getOperand(2).getImm(), mi.getOperand(3).getReg(), outContext);
        outStreamer.emitInstruction(tmpInst);
        return;
      }
      case ARMGenInstrNames.LEApcrelJT:
      case ARMGenInstrNames.tLEApcrelJT:
      case ARMGenInstrNames.t2LEApcrelJT: {
        MCInst tmpInst = new MCInst();
        tmpInst.setOpcode(mi.getOpcode() == ARMGenInstrNames.t2LEApcrelJT ? ARMGenInstrNames.t2ADR
            : (mi.getOpcode() == ARMGenInstrNames.tLEApcrelJT ? ARMGenInstrNames.tADR
            : ARMGenInstrNames.ADR));
        populateADROperands(tmpInst, mi.getOperand(0).getReg(),
            getARMJTIPICJumpTableLabel2(mi.getOperand(1).getIndex(),
                mi.getOperand(2).getImm()),
            mi.getOperand(3).getImm(), mi.getOperand(4).getReg(),
            outContext);
        outStreamer.emitInstruction(tmpInst);
        return;
      }
      // Darwin call instructions are just normal call instructions with different
      // clobber semantics (they clobber R9).
      case ARMGenInstrNames.BXr9_CALL:
      case ARMGenInstrNames.BX_CALL: {
        {
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.MOVr);
          tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.LR));
          tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.PC));
          // Add predicate operands.
          tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
          tmpInst.addOperand(MCOperand.createReg(0));
          // Add 's' bit operand (always reg0 for this)
          tmpInst.addOperand(MCOperand.createReg(0));
          outStreamer.emitInstruction(tmpInst);
        }
        {
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.BX);
          tmpInst.addOperand(MCOperand.createReg(mi.getOperand(0).getReg()));
          outStreamer.emitInstruction(tmpInst);
        }
        return;
      }
      case ARMGenInstrNames.tBXr9_CALL:
      case ARMGenInstrNames.tBX_CALL: {
        {
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.tMOVr);
          tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.LR));
          tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.PC));
          // Add predicate operands.
          tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
          tmpInst.addOperand(MCOperand.createReg(0));
          outStreamer.emitInstruction(tmpInst);
        }
        {
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.tBX);
          tmpInst.addOperand(MCOperand.createReg(mi.getOperand(0).getReg()));
          // Add predicate operands.
          tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
          tmpInst.addOperand(MCOperand.createReg(0));
          outStreamer.emitInstruction(tmpInst);
        }
        return;
      }
      case ARMGenInstrNames.BMOVPCRXr9_CALL:
      case ARMGenInstrNames.BMOVPCRX_CALL: {
        MCInst tmpInst = new MCInst();

        tmpInst = new MCInst();
        tmpInst.setOpcode(ARMGenInstrNames.MOVr);
        tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.LR));
        tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.PC));
        // add predicate operands.
        tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
        tmpInst.addOperand(MCOperand.createReg(0));

        // add 's' bit operand, always zero register for this.
        tmpInst.addOperand(MCOperand.createReg(0));
        outStreamer.emitInstruction(tmpInst);

        tmpInst = new MCInst();
        tmpInst.setOpcode(ARMGenInstrNames.MOVr);
        tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.PC));
        tmpInst.addOperand(MCOperand.createReg(mi.getOperand(0).getReg()));

        // add predicate operands.
        tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
        tmpInst.addOperand(MCOperand.createReg(0));

        // add 's' bit operand, always zero register for this.
        tmpInst.addOperand(MCOperand.createReg(0));
        outStreamer.emitInstruction(tmpInst);
        return;
      }

      case ARMGenInstrNames.MOVi16_ga_pcrel:
      case ARMGenInstrNames.t2MOVi16_ga_pcrel: {
        MCInst tmpInst = new MCInst();
        tmpInst.setOpcode(opc == ARMGenInstrNames.MOVi16_ga_pcrel ? ARMGenInstrNames.MOVi16 : ARMGenInstrNames.t2MOVi16);
        tmpInst.addOperand(MCOperand.createReg(mi.getOperand(0).getReg()));

        int TF = mi.getOperand(1).getTargetFlags();
        boolean isPIC = TF == ARMII.MO_LO16_NONLAZY_PIC;
        GlobalValue GV = mi.getOperand(1).getGlobal();
        MCSymbol GVSym = getARMGVSymbol(GV);
        MCExpr GVSymExpr = MCSymbolRefExpr.create(GVSym);
        if (isPIC) {
          MCSymbol LabelSym = getPICLabel(mai.getPrivateGlobalPrefix(),
              getFunctionNumber(),
              mi.getOperand(2).getImm(), outContext);
          MCExpr LabelSymExpr = MCSymbolRefExpr.create(LabelSym);
          int PCAdj = (opc == ARMGenInstrNames.MOVi16_ga_pcrel) ? 8 : 4;
          MCExpr PCRelExpr =
              ARMMCExpr.createLower16(MCBinaryExpr.createSub(GVSymExpr,
                  MCBinaryExpr.createAdd(LabelSymExpr,
                      MCConstantExpr.create(PCAdj, outContext),
                      outContext), outContext));
          tmpInst.addOperand(MCOperand.createExpr(PCRelExpr));
        } else {
          MCExpr RefExpr = ARMMCExpr.createLower16(GVSymExpr);
          tmpInst.addOperand(MCOperand.createExpr(RefExpr));
        }

        // Add predicate operands.
        tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
        tmpInst.addOperand(MCOperand.createReg(0));
        // Add 's' bit operand (always reg0 for this)
        tmpInst.addOperand(MCOperand.createReg(0));
        outStreamer.emitInstruction(tmpInst);
        return;
      }
      case ARMGenInstrNames.MOVTi16_ga_pcrel:
      case ARMGenInstrNames.t2MOVTi16_ga_pcrel: {
        MCInst tmpInst = new MCInst();
        tmpInst.setOpcode(opc == ARMGenInstrNames.MOVTi16_ga_pcrel
            ? ARMGenInstrNames.MOVTi16 : ARMGenInstrNames.t2MOVTi16);
        tmpInst.addOperand(MCOperand.createReg(mi.getOperand(0).getReg()));
        tmpInst.addOperand(MCOperand.createReg(mi.getOperand(1).getReg()));

        int TF = mi.getOperand(2).getTargetFlags();
        boolean isPIC = TF == ARMII.MO_HI16_NONLAZY_PIC;
        GlobalValue GV = mi.getOperand(2).getGlobal();
        MCSymbol GVSym = getARMGVSymbol(GV);
        MCExpr GVSymExpr = MCSymbolRefExpr.create(GVSym);
        if (isPIC) {
          MCSymbol LabelSym = getPICLabel(mai.getPrivateGlobalPrefix(),
              getFunctionNumber(),
              mi.getOperand(3).getImm(), outContext);
          MCExpr LabelSymExpr = MCSymbolRefExpr.create(LabelSym);
          int PCAdj = (opc == ARMGenInstrNames.MOVTi16_ga_pcrel) ? 8 : 4;
          MCExpr PCRelExpr =
              ARMMCExpr.createUpper16(MCBinaryExpr.createSub(GVSymExpr,
                  MCBinaryExpr.createAdd(LabelSymExpr,
                      MCConstantExpr.create(PCAdj, outContext),
                      outContext), outContext));
          tmpInst.addOperand(MCOperand.createExpr(PCRelExpr));
        } else {
          MCExpr RefExpr = ARMMCExpr.createUpper16(GVSymExpr);
          tmpInst.addOperand(MCOperand.createExpr(RefExpr));
        }
        // Add predicate operands.
        tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
        tmpInst.addOperand(MCOperand.createReg(0));
        // Add 's' bit operand (always reg0 for this)
        tmpInst.addOperand(MCOperand.createReg(0));
        outStreamer.emitInstruction(tmpInst);
        return;
      }
      case ARMGenInstrNames.tPICADD: {
        // This is a pseudo op for a label + instruction sequence, which looks like:
        // LPC0:
        //     add r0, pc
        // This adds the address of LPC0 to r0.

        // Emit the label.
        outStreamer.emitLabel(getPICLabel(mai.getPrivateGlobalPrefix(),
            getFunctionNumber(), mi.getOperand(2).getImm(),
            outContext));

        // Form and emit the add.
        MCInst addInst = new MCInst();
        addInst.setOpcode(ARMGenInstrNames.tADDhirr);
        addInst.addOperand(MCOperand.createReg(mi.getOperand(0).getReg()));
        addInst.addOperand(MCOperand.createReg(mi.getOperand(0).getReg()));
        addInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.PC));
        // Add predicate operands.
        addInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
        addInst.addOperand(MCOperand.createReg(0));
        outStreamer.emitInstruction(addInst);
        return;
      }
      case ARMGenInstrNames.PICADD: {
        // This is a pseudo op for a label + instruction sequence, which looks like:
        // LPC0:
        //     add r0, pc, r0
        // This adds the address of LPC0 to r0.

        // Emit the label.
        outStreamer.emitLabel(getPICLabel(mai.getPrivateGlobalPrefix(),
            getFunctionNumber(), mi.getOperand(2).getImm(),
            outContext));

        // Form and emit the add.
        MCInst addInst = new MCInst();
        addInst.setOpcode(ARMGenInstrNames.ADDrr);
        addInst.addOperand(MCOperand.createReg(mi.getOperand(0).getReg()));
        addInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.PC));
        addInst.addOperand(MCOperand.createReg(mi.getOperand(1).getReg()));
        // Add predicate operands.
        addInst.addOperand(MCOperand.createImm(mi.getOperand(3).getImm()));
        addInst.addOperand(MCOperand.createReg(mi.getOperand(4).getReg()));
        // Add 's' bit operand (always reg0 for this)
        addInst.addOperand(MCOperand.createReg(0));
        outStreamer.emitInstruction(addInst);
        return;
      }
      case ARMGenInstrNames.PICSTR:
      case ARMGenInstrNames.PICSTRB:
      case ARMGenInstrNames.PICSTRH:
      case ARMGenInstrNames.PICLDR:
      case ARMGenInstrNames.PICLDRB:
      case ARMGenInstrNames.PICLDRH:
      case ARMGenInstrNames.PICLDRSB:
      case ARMGenInstrNames.PICLDRSH: {
        // This is a pseudo op for a label + instruction sequence, which looks like:
        // LPC0:
        //     OP r0, [pc, r0]
        // The LCP0 label is referenced by a constant pool entry in order to get
        // a PC-relative address at the ldr instruction.

        // Emit the label.
        outStreamer.emitLabel(getPICLabel(mai.getPrivateGlobalPrefix(),
            getFunctionNumber(), mi.getOperand(2).getImm(),
            outContext));

        // Form and emit the load
        int Opcode;
        switch (mi.getOpcode()) {
          default:
            Util.shouldNotReachHere("Unexpected opcode!");
          case ARMGenInstrNames.PICSTR:
            Opcode = ARMGenInstrNames.STRrs;
            break;
          case ARMGenInstrNames.PICSTRB:
            Opcode = ARMGenInstrNames.STRBrs;
            break;
          case ARMGenInstrNames.PICSTRH:
            Opcode = ARMGenInstrNames.STRH;
            break;
          case ARMGenInstrNames.PICLDR:
            Opcode = ARMGenInstrNames.LDRrs;
            break;
          case ARMGenInstrNames.PICLDRB:
            Opcode = ARMGenInstrNames.LDRBrs;
            break;
          case ARMGenInstrNames.PICLDRH:
            Opcode = ARMGenInstrNames.LDRH;
            break;
          case ARMGenInstrNames.PICLDRSB:
            Opcode = ARMGenInstrNames.LDRSB;
            break;
          case ARMGenInstrNames.PICLDRSH:
            Opcode = ARMGenInstrNames.LDRSH;
            break;
        }
        MCInst ldStInst = new MCInst();
        ldStInst.setOpcode(Opcode);
        ldStInst.addOperand(MCOperand.createReg(mi.getOperand(0).getReg()));
        ldStInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.PC));
        ldStInst.addOperand(MCOperand.createReg(mi.getOperand(1).getReg()));
        ldStInst.addOperand(MCOperand.createImm(0));
        // Add predicate operands.
        ldStInst.addOperand(MCOperand.createImm(mi.getOperand(3).getImm()));
        ldStInst.addOperand(MCOperand.createReg(mi.getOperand(4).getReg()));
        outStreamer.emitInstruction(ldStInst);

        return;
      }
      case ARMGenInstrNames.CONSTPOOL_ENTRY: {
        /// CONSTPOOL_ENTRY - This instruction represents a floating constant pool
        /// in the function.  The first operand is the ID# for this instruction, the
        /// second is the index into the MachineConstantPool that this is, the third
        /// is the size in bytes of this constant pool entry.
        int LabelId = (int) mi.getOperand(0).getImm();
        int cpIdx = (int) mi.getOperand(1).getIndex();
        emitAlignment(2);

        // Mark the constant pool entry as data if we're not already in a data
        // region.
        outStreamer.emitDataRegion();
        outStreamer.emitLabel(getCPISymbol(LabelId));
        MachineConstantPool mcp = mf.getConstantPool();
        MachineConstantPoolEntry mcpe = mcp.getConstants().get(cpIdx);

        if (mcpe.isMachineConstantPoolEntry())
          emitMachineConstantPoolValue(mcpe.getValueAsCPV());
        else
          emitGlobalConstant(mcpe.getValueAsConstant(), 0);
        return;
      }
      case ARMGenInstrNames.t2BR_JT: {
        // Lower and emit the instruction itself, then the jump table following it.
        MCInst tmpInst = new MCInst();
        tmpInst.setOpcode(ARMGenInstrNames.tMOVr);
        tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.PC));
        tmpInst.addOperand(MCOperand.createReg(mi.getOperand(0).getReg()));
        // Add predicate operands.
        tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
        tmpInst.addOperand(MCOperand.createReg(0));
        outStreamer.emitInstruction(tmpInst);
        // Output the data for the jump table itself
        emitJump2Table(mi);
        return;
      }
      case ARMGenInstrNames.t2TBB_JT: {
        // Lower and emit the instruction itself, then the jump table following it.
        MCInst tmpInst = new MCInst();
        tmpInst.setOpcode(ARMGenInstrNames.t2TBB);
        tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.PC));
        tmpInst.addOperand(MCOperand.createReg(mi.getOperand(0).getReg()));
        // Add predicate operands.
        tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
        tmpInst.addOperand(MCOperand.createReg(0));
        outStreamer.emitInstruction(tmpInst);
        // Output the data for the jump table itself
        emitJump2Table(mi);
        // Make sure the next instruction is 2-byte aligned.
        emitAlignment(1);
        return;
      }
      case ARMGenInstrNames.t2TBH_JT: {
        // Lower and emit the instruction itself, then the jump table following it.
        MCInst tmpInst = new MCInst();

        tmpInst.setOpcode(ARMGenInstrNames.t2TBH);
        tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.PC));
        tmpInst.addOperand(MCOperand.createReg(mi.getOperand(0).getReg()));
        // Add predicate operands.
        tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
        tmpInst.addOperand(MCOperand.createReg(0));
        outStreamer.emitInstruction(tmpInst);
        // Output the data for the jump table itself
        emitJump2Table(mi);
        return;
      }
      case ARMGenInstrNames.tBR_JTr:
      case ARMGenInstrNames.BR_JTr: {
        // Lower and emit the instruction itself, then the jump table following it.
        // mov pc, target
        MCInst tmpInst = new MCInst();
        int newOpc = mi.getOpcode() == ARMGenInstrNames.BR_JTr ?
            ARMGenInstrNames.MOVr : ARMGenInstrNames.tMOVr;
        tmpInst.setOpcode(newOpc);
        tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.PC));
        tmpInst.addOperand(MCOperand.createReg(mi.getOperand(0).getReg()));
        // Add predicate operands.
        tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
        tmpInst.addOperand(MCOperand.createReg(0));
        // Add 's' bit operand (always reg0 for this)
        if (newOpc == ARMGenInstrNames.MOVr)
          tmpInst.addOperand(MCOperand.createReg(0));
        outStreamer.emitInstruction(tmpInst);

        // Make sure the Thumb jump table is 4-byte aligned.
        if (newOpc == ARMGenInstrNames.tMOVr)
          emitAlignment(2);

        // Output the data for the jump table itself
        emitJumpTable(mi);
        return;
      }
      case ARMGenInstrNames.BR_JTm: {
        // Lower and emit the instruction itself, then the jump table following it.
        // ldr pc, target
        MCInst tmpInst = new MCInst();
        if (mi.getOperand(1).getReg() == 0) {
          // literal offset
          tmpInst.setOpcode(ARMGenInstrNames.LDRi12);
          tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.PC));
          tmpInst.addOperand(MCOperand.createReg(mi.getOperand(0).getReg()));
          tmpInst.addOperand(MCOperand.createImm(mi.getOperand(2).getImm()));
        } else {
          tmpInst.setOpcode(ARMGenInstrNames.LDRrs);
          tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.PC));
          tmpInst.addOperand(MCOperand.createReg(mi.getOperand(0).getReg()));
          tmpInst.addOperand(MCOperand.createReg(mi.getOperand(1).getReg()));
          tmpInst.addOperand(MCOperand.createImm(0));
        }
        // Add predicate operands.
        tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
        tmpInst.addOperand(MCOperand.createReg(0));
        outStreamer.emitInstruction(tmpInst);

        // Output the data for the jump table itself
        emitJumpTable(mi);
        return;
      }
      case ARMGenInstrNames.BR_JTadd: {
        // Lower and emit the instruction itself, then the jump table following it.
        // add pc, target, idx
        MCInst tmpInst = new MCInst();
        tmpInst.setOpcode(ARMGenInstrNames.ADDrr);
        tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.PC));
        tmpInst.addOperand(MCOperand.createReg(mi.getOperand(0).getReg()));
        tmpInst.addOperand(MCOperand.createReg(mi.getOperand(1).getReg()));
        // Add predicate operands.
        tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
        tmpInst.addOperand(MCOperand.createReg(0));
        // Add 's' bit operand (always reg0 for this)
        tmpInst.addOperand(MCOperand.createReg(0));
        outStreamer.emitInstruction(tmpInst);

        // Output the data for the jump table itself
        emitJumpTable(mi);
        return;
      }
      case ARMGenInstrNames.TRAP: {
        // Non-Darwin binutils don't yet support the "trap" mnemonic.
        // FIXME: Remove this special case when they do.
        if (!subtarget.isTargetDarwin()) {
          //.long 0xe7ffdefe @ trap
          int Val = 0xe7ffdefe;
          outStreamer.addComment("trap");
          outStreamer.emitIntValue(Val, 4, 0);
          return;
        }
        break;
      }
      case ARMGenInstrNames.tTRAP: {
        // Non-Darwin binutils don't yet support the "trap" mnemonic.
        // FIXME: Remove this special case when they do.
        if (!subtarget.isTargetDarwin()) {
          //.short 57086 @ trap
          int Val = 0xdefe;
          outStreamer.addComment("trap");
          outStreamer.emitIntValue(Val, 2, 0);
          return;
        }
        break;
      }
      case ARMGenInstrNames.t2Int_eh_sjlj_setjmp:
      case ARMGenInstrNames.t2Int_eh_sjlj_setjmp_nofp:
      case ARMGenInstrNames.tInt_eh_sjlj_setjmp: {
        // Two incoming args: GPR:$src, GPR:$val
        // mov $val, pc
        // adds $val, #7
        // str $val, [$src, #4]
        // movs r0, #0
        // b 1f
        // movs r0, #1
        // 1:
        int SrcReg = mi.getOperand(0).getReg();
        int ValReg = mi.getOperand(1).getReg();
        MCSymbol Label = getARMSJLJEHLabel();
        {
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.tMOVr);
          tmpInst.addOperand(MCOperand.createReg(ValReg));
          tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.PC));
          // Predicate.
          tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
          tmpInst.addOperand(MCOperand.createReg(0));
          outStreamer.addComment("eh_setjmp begin");
          outStreamer.emitInstruction(tmpInst);
        }
        {
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.tADDi3);
          tmpInst.addOperand(MCOperand.createReg(ValReg));
          // 's' bit operand
          tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.CPSR));
          tmpInst.addOperand(MCOperand.createReg(ValReg));
          tmpInst.addOperand(MCOperand.createImm(7));
          // Predicate.
          tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
          tmpInst.addOperand(MCOperand.createReg(0));
          outStreamer.emitInstruction(tmpInst);
        }
        {
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.tSTRi);
          tmpInst.addOperand(MCOperand.createReg(ValReg));
          tmpInst.addOperand(MCOperand.createReg(SrcReg));
          // The offset immediate is #4. The operand value is scaled by 4 for the
          // tSTR instruction.
          tmpInst.addOperand(MCOperand.createImm(1));
          // Predicate.
          tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
          tmpInst.addOperand(MCOperand.createReg(0));
          outStreamer.emitInstruction(tmpInst);
        }
        {
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.tMOVi8);
          tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.R0));
          tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.CPSR));
          tmpInst.addOperand(MCOperand.createImm(0));
          // Predicate.
          tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
          tmpInst.addOperand(MCOperand.createReg(0));
          outStreamer.emitInstruction(tmpInst);
        }
        {
          MCExpr SymbolExpr = MCSymbolRefExpr.create(Label);
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.tB);
          tmpInst.addOperand(MCOperand.createExpr(SymbolExpr));
          tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
          tmpInst.addOperand(MCOperand.createReg(0));
          outStreamer.emitInstruction(tmpInst);
        }
        {
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.tMOVi8);
          tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.R0));
          tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.CPSR));
          tmpInst.addOperand(MCOperand.createImm(1));
          // Predicate.
          tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
          tmpInst.addOperand(MCOperand.createReg(0));
          outStreamer.addComment("eh_setjmp end");
          outStreamer.emitInstruction(tmpInst);
        }
        outStreamer.emitLabel(Label);
        return;
      }

      case ARMGenInstrNames.Int_eh_sjlj_setjmp_nofp:
      case ARMGenInstrNames.Int_eh_sjlj_setjmp: {
        // Two incoming args: GPR:$src, GPR:$val
        // add $val, pc, #8
        // str $val, [$src, #+4]
        // mov r0, #0
        // add pc, pc, #0
        // mov r0, #1
        int SrcReg = mi.getOperand(0).getReg();
        int ValReg = mi.getOperand(1).getReg();

        {
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.ADDri);
          tmpInst.addOperand(MCOperand.createReg(ValReg));
          tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.PC));
          tmpInst.addOperand(MCOperand.createImm(8));
          // Predicate.
          tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
          tmpInst.addOperand(MCOperand.createReg(0));
          // 's' bit operand (always reg0 for this).
          tmpInst.addOperand(MCOperand.createReg(0));
          outStreamer.addComment("eh_setjmp begin");
          outStreamer.emitInstruction(tmpInst);
        }
        {
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.STRi12);
          tmpInst.addOperand(MCOperand.createReg(ValReg));
          tmpInst.addOperand(MCOperand.createReg(SrcReg));
          tmpInst.addOperand(MCOperand.createImm(4));
          // Predicate.
          tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
          tmpInst.addOperand(MCOperand.createReg(0));
          outStreamer.emitInstruction(tmpInst);
        }
        {
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.MOVi);
          tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.R0));
          tmpInst.addOperand(MCOperand.createImm(0));
          // Predicate.
          tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
          tmpInst.addOperand(MCOperand.createReg(0));
          // 's' bit operand (always reg0 for this).
          tmpInst.addOperand(MCOperand.createReg(0));
          outStreamer.emitInstruction(tmpInst);
        }
        {
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.ADDri);
          tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.PC));
          tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.PC));
          tmpInst.addOperand(MCOperand.createImm(0));
          // Predicate.
          tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
          tmpInst.addOperand(MCOperand.createReg(0));
          // 's' bit operand (always reg0 for this).
          tmpInst.addOperand(MCOperand.createReg(0));
          outStreamer.emitInstruction(tmpInst);
        }
        {
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.MOVi);
          tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.R0));
          tmpInst.addOperand(MCOperand.createImm(1));
          // Predicate.
          tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
          tmpInst.addOperand(MCOperand.createReg(0));
          // 's' bit operand (always reg0 for this).
          tmpInst.addOperand(MCOperand.createReg(0));
          outStreamer.addComment("eh_setjmp end");
          outStreamer.emitInstruction(tmpInst);
        }
        return;
      }
      case ARMGenInstrNames.Int_eh_sjlj_longjmp: {
        // ldr sp, [$src, #8]
        // ldr $scratch, [$src, #4]
        // ldr r7, [$src]
        // bx $scratch
        int SrcReg = mi.getOperand(0).getReg();
        int ScratchReg = mi.getOperand(1).getReg();
        {
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.LDRi12);
          tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.SP));
          tmpInst.addOperand(MCOperand.createReg(SrcReg));
          tmpInst.addOperand(MCOperand.createImm(8));
          // Predicate.
          tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
          tmpInst.addOperand(MCOperand.createReg(0));
          outStreamer.emitInstruction(tmpInst);
        }
        {
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.LDRi12);
          tmpInst.addOperand(MCOperand.createReg(ScratchReg));
          tmpInst.addOperand(MCOperand.createReg(SrcReg));
          tmpInst.addOperand(MCOperand.createImm(4));
          // Predicate.
          tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
          tmpInst.addOperand(MCOperand.createReg(0));
          outStreamer.emitInstruction(tmpInst);
        }
        {
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.LDRi12);
          tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.R7));
          tmpInst.addOperand(MCOperand.createReg(SrcReg));
          tmpInst.addOperand(MCOperand.createImm(0));
          // Predicate.
          tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
          tmpInst.addOperand(MCOperand.createReg(0));
          outStreamer.emitInstruction(tmpInst);
        }
        {
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.BX);
          tmpInst.addOperand(MCOperand.createReg(ScratchReg));
          // Predicate.
          tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
          tmpInst.addOperand(MCOperand.createReg(0));
          outStreamer.emitInstruction(tmpInst);
        }
        return;
      }
      case ARMGenInstrNames.tInt_eh_sjlj_longjmp: {
        // ldr $scratch, [$src, #8]
        // mov sp, $scratch
        // ldr $scratch, [$src, #4]
        // ldr r7, [$src]
        // bx $scratch
        int SrcReg = mi.getOperand(0).getReg();
        int ScratchReg = mi.getOperand(1).getReg();
        {
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.tLDRi);
          tmpInst.addOperand(MCOperand.createReg(ScratchReg));
          tmpInst.addOperand(MCOperand.createReg(SrcReg));
          // The offset immediate is #8. The operand value is scaled by 4 for the
          // tLDR instruction.
          tmpInst.addOperand(MCOperand.createImm(2));
          // Predicate.
          tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
          tmpInst.addOperand(MCOperand.createReg(0));
          outStreamer.emitInstruction(tmpInst);
        }
        {
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.tMOVr);
          tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.SP));
          tmpInst.addOperand(MCOperand.createReg(ScratchReg));
          // Predicate.
          tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
          tmpInst.addOperand(MCOperand.createReg(0));
          outStreamer.emitInstruction(tmpInst);
        }
        {
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.tLDRi);
          tmpInst.addOperand(MCOperand.createReg(ScratchReg));
          tmpInst.addOperand(MCOperand.createReg(SrcReg));
          tmpInst.addOperand(MCOperand.createImm(1));
          // Predicate.
          tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
          tmpInst.addOperand(MCOperand.createReg(0));
          outStreamer.emitInstruction(tmpInst);
        }
        {
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.tLDRr);
          tmpInst.addOperand(MCOperand.createReg(ARMGenRegisterNames.R7));
          tmpInst.addOperand(MCOperand.createReg(SrcReg));
          tmpInst.addOperand(MCOperand.createReg(0));
          // Predicate.
          tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
          tmpInst.addOperand(MCOperand.createReg(0));
          outStreamer.emitInstruction(tmpInst);
        }
        {
          MCInst tmpInst = new MCInst();
          tmpInst.setOpcode(ARMGenInstrNames.tBX);
          tmpInst.addOperand(MCOperand.createReg(ScratchReg));
          // Predicate.
          tmpInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
          tmpInst.addOperand(MCOperand.createReg(0));
          outStreamer.emitInstruction(tmpInst);
        }
        return;
      }
    }
    MCInst inst = new MCInst();
    instLowering.lower(mi, inst);
    outStreamer.emitInstruction(inst);
  }

  private void emitJumpTable(MachineInstr mi) {
    int opcode = mi.getOpcode();
    int opNum = 1;
    if (opcode == ARMGenInstrNames.BR_JTadd)
      opNum = 2;
    else if (opcode == ARMGenInstrNames.BR_JTm)
      opNum = 3;

    MachineOperand mo1 = mi.getOperand(opNum);
    MachineOperand mo2 = mi.getOperand(opNum + 1);
    int jti = mo1.getIndex();

    // tag the jump table
    outStreamer.emitJumpTable32Region();

    // emit a label for the jump table.
    MCSymbol jtiSymbol = getARMJTIPICJumpTableLabel2(jti, mo2.getImm());
    outStreamer.emitLabel(jtiSymbol);

    // emit each entry of the table.
    MachineJumpTableInfo mjti = mf.getJumpTableInfo();
    ArrayList<MachineJumpTableEntry> jt = mjti.getJumpTables();
    ArrayList<MachineBasicBlock> jtMBBs = jt.get(jti).getMBBs();
    ARMFunctionInfo afi = (ARMFunctionInfo) mf.getInfo();

    for (MachineBasicBlock mbb : jtMBBs) {
      // Construct an MCExpr for the entry. We want a value of the form:
      // (BasicBlockAddr - TableBeginAddr)
      //
      // For example, a table with entries jumping to basic blocks BB0 and BB1
      // would look like:
      // LJTI_0_0:
      //    .word (LBB0 - LJTI_0_0)
      //    .word (LBB1 - LJTI_0_0)
      MCExpr expr = MCSymbolRefExpr.create(mbb.getSymbol(outContext));
      if (tm.getRelocationModel() == TargetMachine.RelocModel.PIC_)
        expr = MCBinaryExpr.createSub(expr, MCSymbolRefExpr.create(jtiSymbol), outContext);
      else if (afi.isThumbFunction())
        expr = MCBinaryExpr.createAdd(expr, MCConstantExpr.create(1, outContext), outContext);

      outStreamer.emitValue(expr, 4, 0);
    }
  }

  private void emitJump2Table(MachineInstr mi) {
    int opcode = mi.getOpcode();
    int opNum = opcode == ARMGenInstrNames.t2BR_JT ? 2 : 1;
    MachineOperand mo1 = mi.getOperand(opNum);
    MachineOperand mo2 = mi.getOperand(opNum + 1);
    int jti = mo1.getIndex();

    // tag the jump table
    if (opcode == ARMGenInstrNames.t2TBB_JT) {
      outStreamer.emitJumpTable8Region();
    } else if (opcode == ARMGenInstrNames.t2TBH_JT) {
      outStreamer.emitJumpTable16Region();
    } else
      outStreamer.emitJumpTable32Region();

    // emit a label for the jump table.
    MCSymbol jtiSymbol = getARMJTIPICJumpTableLabel2(jti, mo2.getImm());
    outStreamer.emitLabel(jtiSymbol);

    // emit each entry of the table.
    MachineJumpTableInfo mjti = mf.getJumpTableInfo();
    ArrayList<MachineJumpTableEntry> jt = mjti.getJumpTables();
    ArrayList<MachineBasicBlock> jtMBBs = jt.get(jti).getMBBs();
    ARMFunctionInfo afi = (ARMFunctionInfo) mf.getInfo();
    int offsetWidth = 4;
    if (opcode == ARMGenInstrNames.t2TBB_JT)
      offsetWidth = 1;
    else if (opcode == ARMGenInstrNames.t2TBH_JT)
      offsetWidth = 2;

    for (MachineBasicBlock mbb : jtMBBs) {
      MCExpr expr = MCSymbolRefExpr.create(mbb.getSymbol(outContext));
      // If this isn't a TBB or TBH, the entries are direct branch instructions.
      if (offsetWidth == 4) {
        MCInst brInst = new MCInst();
        brInst.setOpcode(ARMGenInstrNames.t2B);
        brInst.addOperand(MCOperand.createExpr(expr));
        brInst.addOperand(MCOperand.createImm(ARMCC.CondCodes.AL.ordinal()));
        brInst.addOperand(MCOperand.createReg(0));
        outStreamer.emitInstruction(brInst);
        continue;
      }

      // Otherwise it's an offset from the dispatch instruction. Construct an
      // MCExpr for the entry. We want a value of the form:
      // (BasicBlockAddr - TableBeginAddr) / 2
      //
      // For example, a TBB table with entries jumping to basic blocks BB0 and BB1
      // would look like:
      // LJTI_0_0:
      //    .byte (LBB0 - LJTI_0_0) / 2
      //    .byte (LBB1 - LJTI_0_0) / 2
      expr = MCBinaryExpr.createSub(expr, MCSymbolRefExpr.create(jtiSymbol), outContext);
      expr = MCBinaryExpr.createDiv(expr, MCConstantExpr.create(2, outContext), outContext);

      outStreamer.emitValue(expr, offsetWidth, 0);
    }
  }

  private MCSymbol getARMSJLJEHLabel() {
    String symName = mai.getPrivateGlobalPrefix() + "SJLJEH" + getFunctionNumber();
    return outContext.getOrCreateSymbol(symName);
  }

  private MCSymbol getPICLabel(String prefix,
                               int functionNumber,
                               long labelId,
                               MCSymbol.MCContext outContext) {
    return outContext.getOrCreateSymbol(prefix + "PC" + functionNumber + "_" + labelId);
  }

  private MCSymbol getARMGVSymbol(GlobalValue gv) {
    Util.shouldNotReachHere("Currently, darwin platform is not supported completely");
    return null;
  }

  private MCSymbol getARMJTIPICJumpTableLabel2(int uid1, long uid2) {
    return outContext.getOrCreateSymbol(mai.getPrivateGlobalPrefix() +
        "JTI" + getFunctionNumber() + "_" + uid1 + "_" + uid2);
  }

  @Override
  public String getPassName() {
    return "ARM Assembly Printer";
  }

  private MCOperand getSymbolRef(MachineOperand mo, MCSymbol symbol) {
    MCExpr expr;
    switch (mo.getTargetFlags()) {
      default: {
        expr = MCSymbolRefExpr.create(symbol, MCSymbolRefExpr.VariantKind.VK_None);
        switch (mo.getTargetFlags()) {
          default:
            Util.shouldNotReachHere("Unknown target flag on symbol operand");
          case 0:
            break;
          case ARMII.MO_LO16:
            expr = MCSymbolRefExpr.create(symbol, MCSymbolRefExpr.VariantKind.VK_None);
            expr = ARMMCExpr.createLower16(expr);
            break;
          case ARMII.MO_HI16:
            expr = MCSymbolRefExpr.create(symbol, MCSymbolRefExpr.VariantKind.VK_None);
            expr = ARMMCExpr.createUpper16(expr);
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

  MCOperand lowerOperand(MachineOperand mo) {
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
        return MCOperand.createExpr(MCSymbolRefExpr.create(mo.getMBB().getSymbol(outContext)));
      case MO_GlobalAddress:
        return getSymbolRef(mo, mangler.getSymbol(mo.getGlobal()));
      case MO_ExternalSymbol:
        return getSymbolRef(mo, getExternalSymbolSymbol(mo.getSymbolName()));
      case MO_JumpTableIndex:
        return getSymbolRef(mo, getJTISymbol(mo.getIndex(), false));
      case MO_ConstantPoolIndex:
        return getSymbolRef(mo, getCPISymbol(mo.getIndex()));
      case MO_BlockAddress:
        return getSymbolRef(mo, getBlockAddressSymbol(mo.getBlockAddress()));
    }
    return null;
  }

  @Override
  public void emitStartOfAsmFile(Module module) {
    if (subtarget.isTargetDarwin()) {
      TargetMachine.RelocModel rm = tm.getRelocationModel();
      if (rm == TargetMachine.RelocModel.PIC_ || rm == TargetMachine.RelocModel.DynamicNoPIC) {
        // Declare all the text sections up front (before the DWARF sections
        // emitted by AsmPrinter.doInitialization) so the assembler will keep
        // them together at the beginning of the object file.  This helps
        // avoid out-of-range branches that are due a fundamental limitation of
        // the way symbol offsets are encoded with the current Darwin ARM
        // relocations.
        Util.shouldNotReachHere("TODO for Darwin!");
      }
    }
    outStreamer.emitAssemblerFlag(MCAsmInfo.MCAssemblerFlag.MCAF_SyntaxUnified);
    // emit ARM build attributes.
    if (subtarget.isTargetELF()) {
      emitAttributes();
    }
  }

  private void emitAttributes() {
    emitARMAttributeSection();

    /* GAS expect .fpu to be emitted, regardless of VFP build attribute */
    boolean emitFPU = false;
    AttributeEmitter AttrEmitter = null;
    if (outStreamer.hasRawTextSupport()) {
      AttrEmitter = new AsmAttributeEmitter(outStreamer);
      emitFPU = true;
    } else {
      Util.assertion("Object stream is not supported yet!");
    }

    AttrEmitter.maybeSwitchVendor("aeabi");
    ARMSubtarget ts = (ARMSubtarget) subtarget;
    String CPUString = ts.getCPUString();

    if (CPUString.equals("cortex-a8") || ts.isCortexA8()) {
      AttrEmitter.emitTextAttribute(ARMBuildAttrs.CPU_name, "cortex-a8");
      AttrEmitter.emitAttribute(ARMBuildAttrs.CPU_arch, ARMBuildAttrs.v7);
      AttrEmitter.emitAttribute(ARMBuildAttrs.CPU_arch_profile,
          ARMBuildAttrs.ApplicationProfile);
      AttrEmitter.emitAttribute(ARMBuildAttrs.ARM_ISA_use,
          ARMBuildAttrs.Allowed);
      AttrEmitter.emitAttribute(ARMBuildAttrs.THUMB_ISA_use,
          ARMBuildAttrs.AllowThumb32);
      // Fixme: figure out when this is emitted.
      //AttrEmitter.emitAttribute(ARMBuildAttrs.WMMX_arch,
      //                           ARMBuildAttrs.AllowWMMXv1);
      //

      /// ADD additional Else-cases here!
    } else if (CPUString.equals("xscale")) {
      AttrEmitter.emitAttribute(ARMBuildAttrs.CPU_arch, ARMBuildAttrs.v5TEJ);
      AttrEmitter.emitAttribute(ARMBuildAttrs.ARM_ISA_use,
          ARMBuildAttrs.Allowed);
      AttrEmitter.emitAttribute(ARMBuildAttrs.THUMB_ISA_use,
          ARMBuildAttrs.Allowed);
    } else if (CPUString.equals("generic")) {
      // FIXME: Why these defaults?
      AttrEmitter.emitAttribute(ARMBuildAttrs.CPU_arch, ARMBuildAttrs.v4T);
      AttrEmitter.emitAttribute(ARMBuildAttrs.ARM_ISA_use,
          ARMBuildAttrs.Allowed);
      AttrEmitter.emitAttribute(ARMBuildAttrs.THUMB_ISA_use,
          ARMBuildAttrs.Allowed);
    }

    if (ts.hasNEON() && emitFPU) {
      /* NEON is not exactly a VFP architecture, but GAS emit one of
       * neon/vfpv3/vfpv2 for .fpu parameters */
      AttrEmitter.emitTextAttribute(ARMBuildAttrs.Advanced_SIMD_arch, "neon");
      /* If emitted for NEON, omit from VFP below, since you can have both
       * NEON and VFP in build attributes but only one .fpu */
      emitFPU = false;
    }

    /* VFPv3 + .fpu */
    if (ts.hasVFP3()) {
      AttrEmitter.emitAttribute(ARMBuildAttrs.VFP_arch,
          ARMBuildAttrs.AllowFPv3A);
      if (emitFPU)
        AttrEmitter.emitTextAttribute(ARMBuildAttrs.VFP_arch, "vfpv3");

      /* VFPv2 + .fpu */
    } else if (ts.hasVFP2()) {
      AttrEmitter.emitAttribute(ARMBuildAttrs.VFP_arch,
          ARMBuildAttrs.AllowFPv2);
      if (emitFPU)
        AttrEmitter.emitTextAttribute(ARMBuildAttrs.VFP_arch, "vfpv2");
    }

    /* TODO: ARMBuildAttrs.Allowed is not completely accurate,
     * since NEON can have 1 (allowed) or 2 (MAC operations) */
    if (ts.hasNEON()) {
      AttrEmitter.emitAttribute(ARMBuildAttrs.Advanced_SIMD_arch,
          ARMBuildAttrs.Allowed);
    }

    // Signal various FP modes.
    if (!EnableUnsafeFPMath.value) {
      AttrEmitter.emitAttribute(ARMBuildAttrs.ABI_FP_denormal,
          ARMBuildAttrs.Allowed);
      AttrEmitter.emitAttribute(ARMBuildAttrs.ABI_FP_exceptions,
          ARMBuildAttrs.Allowed);
    }

    if (EnableNoInfsFPMath.value && EnableNoNaNsFPMath.value)
      AttrEmitter.emitAttribute(ARMBuildAttrs.ABI_FP_number_model,
          ARMBuildAttrs.Allowed);
    else
      AttrEmitter.emitAttribute(ARMBuildAttrs.ABI_FP_number_model,
          ARMBuildAttrs.AllowIEE754);

    // FIXME: add more flags to ARMBuildAttrs.java
    // 8-bytes alignment stuff.
    AttrEmitter.emitAttribute(ARMBuildAttrs.ABI_align8_needed, 1);
    AttrEmitter.emitAttribute(ARMBuildAttrs.ABI_align8_preserved, 1);

    // Hard float.  Use both S and D registers and conform to AAPCS-VFP.
    if (ts.isAAPCS_ABI() && FloatABIForType.value == FloatABI.Hard) {
      AttrEmitter.emitAttribute(ARMBuildAttrs.ABI_HardFP_use, 3);
      AttrEmitter.emitAttribute(ARMBuildAttrs.ABI_VFP_args, 1);
    }

    // FIXME: Should we signal R9 usage?
    if (ts.hasDivide())
      AttrEmitter.emitAttribute(ARMBuildAttrs.DIV_use, 1);

    AttrEmitter.finish();
  }

  private void emitARMAttributeSection() {
    // <format-version>
    // [ <section-length> "vendor-name"
    // [ <file-tag> <size> <attribute>*
    //   | <section-tag> <size> <section-number>* 0 <attribute>*
    //   | <symbol-tag> <size> <symbol-number>* 0 <attribute>*
    //   ]+
    // ]*
    if (outStreamer.hasRawTextSupport())
      return;

    ARMELFTargetObjectFile tloELF = (ARMELFTargetObjectFile) getObjFileLowering();
    outStreamer.switchSection(tloELF.getAttributeSection());
    // format version.
    outStreamer.emitIntValue(0x41, 1, 0);
  }

  @Override
  public void emitEndOfAsmFile(Module module) {
    if (subtarget.isTargetDarwin()) {
      Util.shouldNotReachHere("TODO for darwin!");
    }
  }

  @Override
  protected void emitFunctionEntryLabel() {
    ARMFunctionInfo afi = (ARMFunctionInfo) mf.getInfo();
    if (afi.isThumbFunction()) {
      outStreamer.emitAssemblerFlag(MCAsmInfo.MCAssemblerFlag.MCAF_Code16);
      outStreamer.emitThumbFunc(curFuncSym);
    }
    outStreamer.emitLabel(curFuncSym);
  }

  private interface AttributeEmitter {
    void maybeSwitchVendor(String vendor);

    void emitAttribute(int attribute, int value);

    void emitTextAttribute(int attribute, String str);

    void finish();
  }

  private static class AsmAttributeEmitter implements AttributeEmitter {
    private MCStreamer streamer;

    AsmAttributeEmitter(MCStreamer stream) {
      streamer = stream;
    }

    @Override
    public void maybeSwitchVendor(String vendor) {
    }

    @Override
    public void emitAttribute(int attribute, int value) {
      streamer.emitRawText(String.format("\t.eabi_attribute %d, %d", attribute, value));
    }

    @Override
    public void emitTextAttribute(int attribute, String str) {
      switch (attribute) {
        case ARMBuildAttrs.CPU_name:
          streamer.emitRawText(String.format("\t.cpu %s", str.toLowerCase()));
          break;
        case ARMBuildAttrs.Advanced_SIMD_arch:
        case ARMBuildAttrs.VFP_arch:
          streamer.emitRawText(String.format("\t.fpu %s", str.toLowerCase()));
          break;
        default:
          Util.shouldNotReachHere("Unsupported text attribute in ASM mode!");
          break;
      }
    }

    @Override
    public void finish() {
    }
  }
}
