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

import backend.codegen.AsmPrinter;
import backend.codegen.CalleeSavedInfo;
import backend.codegen.MachineFrameInfo;
import backend.codegen.MachineInstr;
import backend.mc.MCAsmInfo;
import backend.mc.MCInst;
import backend.mc.MCStreamer;
import backend.mc.MCSymbol;
import backend.target.TargetMachine;
import backend.target.TargetRegisterInfo;
import backend.value.Module;
import tools.Util;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import static backend.target.mips.MipsGenRegisterInfo.AFGR64RegisterClass;
import static backend.target.mips.MipsGenRegisterInfo.CPURegsRegisterClass;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class MipsAsmPrinter extends AsmPrinter {
  protected MipsAsmPrinter(OutputStream os,
                           TargetMachine tm,
                           MCSymbol.MCContext ctx,
                           MCStreamer streamer,
                           MCAsmInfo mai) {
    super(os, tm, ctx, streamer, mai);
  }

  private static boolean isUnalignedLoadStore(int opc) {
    switch (opc) {
      case MipsGenInstrNames.ULW:
      case MipsGenInstrNames.ULH:
      case MipsGenInstrNames.ULHu:
      case MipsGenInstrNames.USW:
      case MipsGenInstrNames.USH:
      case MipsGenInstrNames.ULW_P8:
      case MipsGenInstrNames.ULH_P8:
      case MipsGenInstrNames.ULHu_P8:
      case MipsGenInstrNames.USW_P8:
      case MipsGenInstrNames.USH_P8:
        return true;
      default:
        return false;
    }
  }

  @Override
  protected void emitInstruction(MachineInstr mi) {
    MipsMCInstLower lower = new MipsMCInstLower(mangler, outContext, this);
    MCInst inst = new MCInst();
    int opc = mi.getOpcode();
    lower.lower(mi, inst);

    // Enclose unaligned load or store with .macro & .nomacro directives.
    if (isUnalignedLoadStore(opc)) {
      MCInst directive = new MCInst();
      directive.setOpcode(MipsGenInstrNames.MACRO);
      outStreamer.emitInstruction(directive);
      outStreamer.emitInstruction(inst);
      directive.setOpcode(MipsGenInstrNames.NOMACRO);
      outStreamer.emitInstruction(directive);
      return;
    }

    outStreamer.emitInstruction(inst);
  }

  @Override
  public String getPassName() {
    return "Mips Assembly Printer";
  }

  @Override
  protected void emitFunctionEntryLabel() {
    outStreamer.emitRawText("\t.ent\t" + curFuncSym.getName());
    outStreamer.emitLabel(curFuncSym);
  }

  /**
   * Create a bitmask with all callee saved registers for CPU or floating point coprocessor registers.
   * @param os
   */
  private void printSavedRegsBitmask(PrintStream os) {
    int cpuBitmask = 0, fpuBitmask = 0;
    int cpuTopSavedRegOff, fpuTopSavedRegOff;

    MachineFrameInfo mfi = mf.getFrameInfo();
    ArrayList<CalleeSavedInfo> csi = mfi.getCalleeSavedInfo();
    int cpuRegSize = tri.getRegSize(CPURegsRegisterClass);
    int fgr32RegSize = tri.getRegSize(MipsGenRegisterInfo.FGR32RegisterClass);
    int afgr64RegSize = tri.getRegSize(MipsGenRegisterInfo.AFGR64RegisterClass);
    boolean hasAFGR64Reg = false;
    int csFPRegsSize = 0;
    int i = 0, e = csi.size();

    for (; i < e; ++i) {
      int reg = csi.get(i).getReg();
      if (CPURegsRegisterClass.contains(reg))
        break;

      int regNum = MipsRegisterInfo.getRegisterNumbering(reg);
      if (AFGR64RegisterClass.contains(reg)) {
        fpuBitmask |= (3 << regNum);
        csFPRegsSize += afgr64RegSize;
        hasAFGR64Reg = true;
        continue;
      }

      fpuBitmask |= (1 << regNum);
      csFPRegsSize += fgr32RegSize;
    }

    // set CPU bitmask.
    for (; i < e; ++i) {
      int reg = csi.get(i).getReg();
      int regNum = MipsRegisterInfo.getRegisterNumbering(reg);
      cpuBitmask |= (1 << regNum);
    }

    // FP Regs are saved right below where the virtual frame pointer points to.
    fpuTopSavedRegOff = fpuBitmask != 0 ? (hasAFGR64Reg ? -afgr64RegSize : -fgr32RegSize) : 0;
    cpuTopSavedRegOff = cpuBitmask != 0 ? -csFPRegsSize - cpuRegSize : 0;

    os.print("\t.mask\t");
    printHex32(cpuBitmask, os);
    os.print(",");
    os.println(cpuTopSavedRegOff);

    os.print("\t.fmask\t");
    printHex32(fpuBitmask, os);
    os.print(",");
    os.println(fpuTopSavedRegOff);
  }

  private static void printHex32(int val, PrintStream os) {
    os.print("0x");
    for (int i = 7; i >= 0; --i)
      os.printf("%x", (val & (0xf << (i * 4))) >> (i*4));
  }

  private void emitFrameDirective() {
    TargetRegisterInfo tri = tm.getSubtarget().getRegisterInfo();
    int stackReg = tri.getFrameRegister(mf);
    int returnReg = tri.getRARegister();
    int stackSize = mf.getFrameInfo().getStackSize();

    String msg = String.format("\t.frame\t$%s,%d,$%s",
        outStreamer.getRegisterName(stackReg).toLowerCase(),
        stackSize, outStreamer.getRegisterName(returnReg).toLowerCase());
    outStreamer.emitRawText(msg);
  }

  @Override
  protected void emitFunctionBodyStart() {
    emitFrameDirective();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    printSavedRegsBitmask(new PrintStream(baos));
    outStreamer.emitRawText(baos.toString());
  }

  @Override
  protected void emitFunctionBodyEnd() {
    outStreamer.emitRawText("\t.set\tmacro");
    outStreamer.emitRawText("\t.set\treorder");
    outStreamer.emitRawText("\t.end\t" + curFuncSym.getName());
  }

  @Override
  public void emitStartOfAsmFile(Module module) {
    outStreamer.emitRawText("\t.section .mdebug." + getCurrentABIString());
    if (((MipsSubtarget)subtarget).isABI_EABI()) {
      if (((MipsSubtarget)subtarget).isGP32bit())
        outStreamer.emitRawText("\t.section .gcc_compiled_long32");
      else
        outStreamer.emitRawText("\t.section .gcc_compiled_long64");
    }
    outStreamer.emitRawText("\t.previous");
  }

  private String getCurrentABIString() {
    switch (((MipsSubtarget)subtarget).getTargetABI()) {
      case O32: return "abi32";
      case N32: return "abiN32";
      case N64: return "abi64";
      case EABI: return "eabi32";
      default:
        Util.shouldNotReachHere("Unknown Mips ABI!");
        return "";
    }
  }
}
