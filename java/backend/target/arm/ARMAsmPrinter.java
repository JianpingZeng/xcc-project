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

import backend.codegen.AsmPrinter;
import backend.codegen.MachineInstr;
import backend.codegen.MachineOperand;
import backend.mc.*;
import backend.target.FloatABI;
import backend.target.TargetMachine;
import backend.value.Module;
import tools.Util;

import java.io.PrintStream;

import static backend.support.BackendCmdOptions.EnableNoInfsFPMath;
import static backend.support.BackendCmdOptions.EnableNoNaNsFPMath;
import static backend.support.BackendCmdOptions.EnableUnsafeFPMath;
import static backend.target.TargetOptions.FloatABIForType;

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
      case ARMGenInstrNames.BMOVPCRXr9_CALL:
      case ARMGenInstrNames.BMOVPCRX_CALL: {
        MCInst tmpInst;

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
    }
    MCInst inst = new MCInst();
    instLowering.lower(mi, inst);
    outStreamer.emitInstruction(inst);
  }

  @Override
  public String getPassName() {
    return "ARM Assembly Printer";
  }

  private MCOperand getSymbolRef(MachineOperand mo, MCSymbol symbol) {
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
    public void maybeSwitchVendor(String vendor) { }

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
    public void finish() { }
  }
}
