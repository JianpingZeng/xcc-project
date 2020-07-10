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

import backend.mc.*;
import tools.OutRef;
import tools.Util;

import java.io.PrintStream;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public abstract class ARMInstPrinter extends MCInstPrinter {
  protected ARMInstPrinter(PrintStream os, MCAsmInfo mai) {
    super(os, mai);
  }

  static MCInstPrinter createARMInstPrinter(PrintStream os, MCAsmInfo mai) {
    return new ARMGenInstPrinter(os, mai);
  }

  @Override
  public void printInstruction(MCInst inst) {
    // check for some MOVs and print canonical forms.
    int opc = inst.getOpcode();
    switch (opc) {
      case ARMGenInstrNames.MOVsr: {
        MCOperand dst = inst.getOperand(0);
        MCOperand mo1 = inst.getOperand(1);
        MCOperand mo2 = inst.getOperand(2);
        MCOperand mo3 = inst.getOperand(3);

        os.print('\t');
        os.print(ARM_AM.getShiftOpcStr(ARM_AM.getSORegShOp(mo3.getImm())));
        printSBitModifierOperand(inst, 6);
        printPredicateOperand(inst, 4);
        os.printf("\t%s, %s", getRegisterName(dst.getReg()),
                              getRegisterName(mo1.getReg()));
        os.print(", ");
        os.print(getRegisterName(mo2.getReg()));
        Util.assertion(ARM_AM.getSORegOffset(mo3.getImm()) == 0);
        return;
      }
      case ARMGenInstrNames.MOVsi: {
        MCOperand dst = inst.getOperand(0);
        MCOperand mo1 = inst.getOperand(1);
        MCOperand mo2 = inst.getOperand(2);

        os.print('\t');
        os.print(ARM_AM.getShiftOpcStr(ARM_AM.getSORegShOp(mo2.getImm())));
        printSBitModifierOperand(inst, 5);
        printPredicateOperand(inst, 3);

        os.print('\t');
        os.print(getRegisterName(dst.getReg()));
        os.print(", ");
        os.print(getRegisterName(mo1.getReg()));

        if (ARM_AM.getSORegShOp(mo2.getImm()) == ARM_AM.ShiftOpc.rrx)
          return;

        os.print(", #");
        os.print(translateShiftImm(ARM_AM.getSORegOffset(mo2.getImm())));
        return;
      }
      case ARMGenInstrNames.STMDB_UPD:
      case ARMGenInstrNames.t2STMDB_UPD: {
        if (inst.getOperand(0).getReg() == ARMGenRegisterNames.SP) {
          // A8.6.123 PUSH
          os.print("\tpush");
          printPredicateOperand(inst, 2);
          if (opc == ARMGenInstrNames.t2STMDB_UPD)
            os.print(".w");

          os.print('\t');
          printRegisterList(inst, 4);
          return;
        }
      }
      case ARMGenInstrNames.STR_PRE_IMM: {
        // A8.6.123 PUSH
        if (inst.getOperand(2).getReg() == ARMGenRegisterNames.SP &&
            inst.getOperand(3).getImm() == -4) {
          os.print("\tpush");
          printPredicateOperand(inst, 4);
          os.print("\t{");
          os.print(getRegisterName(inst.getOperand(1).getReg()));
          os.print('}');
          return;
        }
      }
      case ARMGenInstrNames.LDMIA_UPD:
      case ARMGenInstrNames.t2LDMIA_UPD: {
        // A8.6.122 POP
        if (inst.getOperand(0).getReg() == ARMGenRegisterNames.SP) {
          os.print("\tpop");
          printPredicateOperand(inst, 2);
          if (opc == ARMGenInstrNames.t2LDMIA_UPD)
            os.print(".w");

          os.print('\t');
          printRegisterList(inst, 4);
          return;
        }
      }
      case ARMGenInstrNames.LDR_POST_IMM: {
        if (inst.getOperand(2).getReg() == ARMGenRegisterNames.SP &&
            inst.getOperand(4).getImm() == 4) {
          os.print("\tpop");
          printPredicateOperand(inst, 5);
          os.print("\t{");
          os.print(getRegisterName(inst.getOperand(0).getReg()));
          os.print('}');
          return;
        }
      }
      case ARMGenInstrNames.VSTMSDB_UPD:
      case ARMGenInstrNames.VSTMDDB_UPD: {
        if (inst.getOperand(0).getReg() == ARMGenRegisterNames.SP) {
          // A8.6.355 VPUSH
          os.print("\tvpush");
          printPredicateOperand(inst, 2);
          os.print('\t');
          printRegisterList(inst, 4);
          return;
        }
      }
      case ARMGenInstrNames.VLDMSIA_UPD:
      case ARMGenInstrNames.VLDMDIA_UPD: {
        if (inst.getOperand(0).getReg() == ARMGenRegisterNames.SP) {
          os.print("\tvpop");
          printPredicateOperand(inst, 2);
          os.print('\t');
          printRegisterList(inst, 4);
          return;
        }
      }
      case ARMGenInstrNames.tLDMIA: {
        boolean writeback = true;
        int baseReg = inst.getOperand(0).getReg();
        for (int i = 3; i < inst.getNumOperands(); ++i) {
          if (inst.getOperand(i).getReg() == baseReg)
            writeback = false;
        }

        os.print("\tldm");
        printPredicateOperand(inst, 1);
        os.print('\t');
        os.print(getRegisterName(baseReg));
        if (writeback) os.print('!');
        os.print(", ");
        printRegisterList(inst, 3);
        return;
      }
      case ARMGenInstrNames.tMOVr: {
        if (inst.getOperand(0).getReg() == ARMGenRegisterNames.R8 &&
            inst.getOperand(1).getReg() == ARMGenRegisterNames.R8) {
          os.print("\tnop");
          printPredicateOperand(inst, 2);
          return;
        }
      }
    }
    printInst(inst);
  }

  protected abstract void printInst(MCInst inst);

  protected void printOperand(MCInst mi, int opIdx) {
    MCOperand op = mi.getOperand(opIdx);
    if (op.isReg())
      os.printf("%s", getRegisterName(op.getReg()));
    else if (op.isImm()) {
      os.printf("#%d", op.getImm());
      if (commentStream != null && (op.getImm() > 255 || op.getImm() < -256))
        commentStream.printf("imm = 0x%%%x\n", op.getImm());
    } else {
      Util.assertion(op.isExpr(), "unknown operand kind in printOperand");
      MCConstantExpr branchTarget = op.getExpr() instanceof MCConstantExpr ?
          (MCConstantExpr) op.getExpr() : null;
      OutRef<Long> address = new OutRef<>(0L);
      if (branchTarget != null && branchTarget.evaluateAsAbsolute(address)) {
        os.printf("0x%x", address.get());
      }
      else {
        op.getExpr().print(os);
      }
    }
  }

  protected void printT2AddrModeImm8s4OffsetOperand(MCInst mi, int opNo) {
    MCOperand mo = mi.getOperand(opNo);
    int offImm = (int) (mo.getImm() / 4);
    if (offImm != 0) {
      os.printf(", #%d", offImm < 0 ? -offImm * 4 : offImm*4);
    }
  }

  protected void printThumbSRImm(MCInst mi, int opNo) {
    long imm = mi.getOperand(opNo).getImm();
    os.printf("#%d", translateShiftImm((int) imm));
  }

  protected void printShiftImmOperand(MCInst mi, int opNo) {
    int shiftOp = (int) mi.getOperand(opNo).getImm();
    boolean isASR = (shiftOp & (1 << 5)) != 0;
    int amt = shiftOp & 0x1f;
    if (isASR)
      os.printf(", asr #%d", translateShiftImm(amt));
    else if (amt != 0)
      os.printf(", lsl #%d", amt);
  }

  protected void printPKHASRShiftImm(MCInst mi, int opNo) {
    int imm = (int) mi.getOperand(opNo).getImm();
    imm = translateShiftImm(imm);
    Util.assertion(imm > 0 && imm < 32, "Invalid PKH shift immediate value!");
    os.printf(", asr #%d", imm);
  }

  protected void printPKHLSLShiftImm(MCInst mi, int opNo) {
    int imm = (int) mi.getOperand(opNo).getImm();
    imm = translateShiftImm(imm);
    Util.assertion(imm > 0 && imm < 32, "Invalid PKH shift immediate value!");
    os.printf(", lsl #%d", imm);
  }

  protected void printT2AddrModeImm8s4Operand(MCInst mi, int opNo) {
    MCOperand mo1 = mi.getOperand(opNo);
    MCOperand mo2 = mi.getOperand(opNo+1);

    os.printf("[%s", getRegisterName(mo1.getReg()));
    int offImm = (int) (mo2.getImm()/4);
    if (offImm < 0)
      os.printf(", #-%d", -offImm);
    else if (offImm > 0)
      os.printf(", #%d", offImm);
    os.print(']');
  }

  protected void printAddrMode3OffsetOperand(MCInst mi, int opNo) {
    MCOperand mo1 = mi.getOperand(opNo);
    MCOperand mo2 = mi.getOperand(opNo+1);
    if (mo1.isReg()) {
      os.printf("%s%s", ARM_AM.getAddrOpcStr(ARM_AM.getAM3Op((int) mo2.getImm())), getRegisterName(mo1.getReg()));
      return;
    }

    int immOff = ARM_AM.getAM3Offset((int) mo2.getImm());
    os.printf("#%s%d", ARM_AM.getAddrOpcStr(ARM_AM.getAM3Op((int) mo2.getImm())), immOff);
  }

  protected void printPostIdxRegOperand(MCInst mi, int opNo) {
    MCOperand mo1 = mi.getOperand(opNo);
    MCOperand mo2 = mi.getOperand(opNo+1);
    os.printf("%s%s", mo2.getImm() != 0 ? "" : "-", getRegisterName(mo1.getReg()));
  }

  protected void printPostIdxImm8Operand(MCInst mi, int opNo) {
    MCOperand mo = mi.getOperand(opNo);
    long imm = mo.getImm();
    os.printf("#%s%d", (imm &256) != 0 ? "" : "-", imm & 0xff);
  }

  protected void printAddrMode2OffsetOperand(MCInst mi, int opNo) {
    MCOperand mo1 = mi.getOperand(opNo);
    MCOperand mo2 = mi.getOperand(opNo+1);

    if (mo1.getReg() == 0) {
      int immOff = ARM_AM.getAM2Offset((int) mo2.getImm());
      os.printf("#%s%d", ARM_AM.getAddrOpcStr(ARM_AM.getAM2Op((int) mo2.getImm())), immOff);
      return;
    }

    os.printf("%s%s", ARM_AM.getAddrOpcStr(ARM_AM.getAM2Op((int) mo2.getImm())), getRegisterName(mo1.getReg()));
    int shImm = ARM_AM.getAM2Offset((int) mo2.getImm());
    if (shImm != 0) {
      os.printf(", %s#%d", ARM_AM.getAddrOpcStr(ARM_AM.getAM2Op((int) mo2.getImm())), shImm);
    }
  }

  protected void printPostIdxImm8s4Operand(MCInst mi, int opNo) {
    MCOperand mo = mi.getOperand(opNo);
    long imm = mo.getImm();
    os.printf("#%s%d", (imm & 256) != 0 ? "" : "-", (imm & 0xff) << 2);
  }

  protected void printCoprocOptionImm(MCInst mi, int opNo) {
    os.printf("{%d}", mi.getOperand(opNo).getImm());
  }

  protected void printT2AddrModeImm8OffsetOperand(MCInst mi, int opNo) {
    MCOperand mo1 = mi.getOperand(opNo);
    long offImm = mo1.getImm();
    if (offImm < 0)
      os.printf(", #-%d", -offImm);
    else
      os.printf(", #%d", offImm);
  }

  protected void printAddrMode6OffsetOperand(MCInst mi, int opNo) {
    MCOperand mo = mi.getOperand(opNo);
    if (mo.getReg() == 0)
      os.print('!');
    else
      os.printf(", %s", getRegisterName(mo.getReg()));
  }

  protected void printVectorIndex(MCInst mi, int opNo) {
    os.printf("[%d]", mi.getOperand(opNo).getImm());
  }

  protected void printRotImmOperand(MCInst mi, int opNo) {
    long imm = mi.getOperand(opNo).getImm();
    if (imm == 0)
      return;
    os.print(", ror#");
    switch ((int) imm) {
      default:
        Util.assertion("Illegal ror immediate!");
        break;
      case 1: os.print(8); break;
      case 2: os.print(16); break;
      case 3: os.print(24); break;
    }
  }

  private void printThumbAddrModeImm5SOperand(MCInst mi, int opNo, int scale) {
    MCOperand mo1 = mi.getOperand(opNo);
    MCOperand mo2 = mi.getOperand(opNo+1);

    if (!mo1.isReg()) {
      printOperand(mi, opNo);
      return;
    }
    long immOff = mo2.getImm();
    os.printf("[%s%s]", getRegisterName(mo1.getReg()), immOff == 0 ? "" : String.format(", #%d", immOff*scale));
  }

  protected void printThumbAddrModeSPOperand(MCInst mi, int opNo) {
    printThumbAddrModeImm5SOperand(mi, opNo, 1);
  }

  protected void printThumbAddrModeImm5S4Operand(MCInst mi, int opNo) {
      printThumbAddrModeImm5SOperand(mi, opNo, 4);
  }

  protected void printThumbAddrModeImm5S2Operand(MCInst mi, int opNo) {
    printThumbAddrModeImm5SOperand(mi, opNo, 2);
  }

  protected void printThumbAddrModeRROperand(MCInst mi, int opNo) {
    MCOperand mo1 = mi.getOperand(opNo);
    MCOperand mo2 = mi.getOperand(opNo+1);

    if (!mo1.isReg()) {
      printOperand(mi, opNo);
      return;
    }

    int reg = mo2.getReg();
    os.printf("[%s%s]", getRegisterName(mo1.getReg()), reg == 0 ? "" : String.format(", %s", getRegisterName(reg)));
  }

  protected void printThumbAddrModeImm5S1Operand(MCInst mi, int opNo) {
    printThumbAddrModeImm5SOperand(mi, opNo, 1);
  }

  protected void printThumbS4ImmOperand(MCInst mi, int opNo) {
    os.printf("#%d", mi.getOperand(opNo).getImm() * 4);
  }

  protected void printT2AddrModeImm0_1020s4Operand(MCInst mi, int opNo) {
    MCOperand mo1 = mi.getOperand(opNo);
    MCOperand mo2 = mi.getOperand(opNo+1);

    long imm = mo2.getImm();
    os.printf("[%s%s]", getRegisterName(mo1.getReg()), imm == 0 ? "" : String.format(", #%d", imm*4));
  }

  protected void printT2LdrLabelOperand(MCInst mi, int opNo) {
    MCOperand mo = mi.getOperand(opNo);
    if (mo.isExpr())
      mo.getExpr().print(os);
    else if (mo.isImm())
      os.printf("[pc, #%d]", mo.getImm());
    else
      Util.shouldNotReachHere("Unknown LDR label operand!");
  }

  /**
   * Constant shifts t2_so_reg is a 2-operand unit corresponding to the Thumb2
   * register with shift forms.
   * <pre>
   * REG 0   0           - e.g. R5
   * REG IMM, SH_OPC     - e.g. R5, LSL #3
   * </pre>
   * @param mi
   * @param opNo
   */
  protected void printT2SOOperand(MCInst mi, int opNo) {
    MCOperand mo1 = mi.getOperand(opNo);
    MCOperand mo2 = mi.getOperand(opNo+1);

    int reg = mo1.getReg();
    os.print(getRegisterName(reg));

    // print the shift opc.
    Util.assertion(mo2.isImm(), "Not a valid t2_so_reg value!");
    ARM_AM.ShiftOpc shOpc = ARM_AM.getSORegShOp(mo2.getImm());
    os.printf(", %s", ARM_AM.getShiftOpcStr(shOpc));
    if (shOpc != ARM_AM.ShiftOpc.rrx)
      os.printf(" #%d", translateShiftImm(ARM_AM.getSORegOffset(mo2.getImm())));
  }

  protected void printNoHashImmediate(MCInst mi, int opNo) {
    os.print(mi.getOperand(opNo).getImm());
  }

  protected void printAddrMode6Operand(MCInst mi, int opNo) {
    MCOperand mo1 = mi.getOperand(opNo);
    MCOperand mo2 = mi.getOperand(opNo+1);

    os.printf("[%s", getRegisterName(mo1.getReg()));
    long imm = mo2.getImm();
    if (imm != 0) {
      os.printf(", :%d", imm << 3);
    }
    os.print(']');
  }

  protected void printNEONModImmOperand(MCInst mi, int opNo) {
    long encodedImm = mi.getOperand(opNo).getImm();
    OutRef<Integer> eltBits = new OutRef<>(0);
    long val = ARM_AM.decodeNEONModImm(encodedImm, eltBits);
    os.printf("#0x%x", val);
  }

  protected void printImmPlusOneOperand(MCInst mi, int opNo) {
    os.printf("#%d", mi.getOperand(opNo).getImm() + 1);
  }

  //===--------------------------------------------------------------------===//
  // Addressing Mode #3
  //===--------------------------------------------------------------------===//
  private void printAM3PostIndexOp(MCInst mi, int opNo) {
    MCOperand mo1 = mi.getOperand(opNo);
    MCOperand mo2 = mi.getOperand(opNo+1);
    MCOperand mo3 = mi.getOperand(opNo+2);

    os.printf("[%s], ", getRegisterName(mo1.getReg()));
    int reg = mo2.getReg();
    if (reg != 0) {
      os.printf(", %s%s]", ARM_AM.getAM3Op((int) mo3.getImm()).ordinal(), getRegisterName(mo2.getReg()));
      return;
    }

    long immOff = ARM_AM.getAM3Offset((int) mo3.getImm());
    if (immOff != 0) {
      os.printf("#%s%d", ARM_AM.getAddrOpcStr(ARM_AM.getAM3Op((int) mo3.getImm())), immOff);
    }
  }

  private void printAM3PreOrOffsetIndexOp(MCInst mi, int opNo) {
    MCOperand mo1 = mi.getOperand(opNo);
    MCOperand mo2 = mi.getOperand(opNo+1);
    MCOperand mo3 = mi.getOperand(opNo+2);

    os.printf("[%s", getRegisterName(mo1.getReg()));
    int reg = mo2.getReg();
    if (reg != 0) {
      os.printf(", %s%s]", ARM_AM.getAddrOpcStr(ARM_AM.getAM3Op((int) mo3.getImm())), getRegisterName(mo2.getReg()));
      return;
    }

    long immOff = ARM_AM.getAM3Offset((int) mo3.getImm());
    if (immOff != 0) {
      os.printf(", #%s%d", ARM_AM.getAddrOpcStr(ARM_AM.getAM3Op((int) mo3.getImm())), immOff);
    }
    os.print(']');
  }

  protected void printAddrMode3Operand(MCInst mi, int opNo) {
    MCOperand mo3 = mi.getOperand(opNo+2);
    int idxMode = ARM_AM.getAM3IdxMode((int) mo3.getImm());
    if (idxMode == ARMII.IndexMode.IndexModePost.ordinal()) {
      printAM3PostIndexOp(mi, opNo);
      return;
    }

    printAM3PreOrOffsetIndexOp(mi, opNo);
  }

  protected void printAddrMode7Operand(MCInst mi, int opNo) {
    os.printf("[%s]", getRegisterName(mi.getOperand(opNo).getReg()));
  }

  protected void printAddrMode5Operand(MCInst mi, int opNo) {
    MCOperand mo1 = mi.getOperand(opNo);
    MCOperand mo2 = mi.getOperand(opNo+1);
    if (!mo1.isReg()) {
      printOperand(mi, opNo);
      return;
    }

    os.printf("[%s", getRegisterName(mo1.getReg()));
    long immOff = ARM_AM.getAM5Offset((int) mo2.getImm());
    ARM_AM.AddrOpc op = ARM_AM.getAM5Op((int) mo2.getImm());
    if (immOff != 0 || op == ARM_AM.AddrOpc.sub) {
      os.printf(", #%s%d", ARM_AM.getAddrOpcStr(op), immOff * 4);
    }
    os.print(']');
  }

  protected void printFPImmOperand(MCInst mi, int opNo) {
    os.printf("#%f", ARM_AM.getFPImmFloat((int) mi.getOperand(opNo).getImm()));
  }

  /**
   * so_reg is a 4-operand unit corresponding to register forms of the A5.1
   * "Addressing Mode 1 - Data-processing operands" forms.  This includes:
   *    REG 0   0           - e.g. R5
   *    REG REG 0,SH_OPC    - e.g. R5, ROR R3
   *    REG 0   IMM,SH_OPC  - e.g. R5, LSL #3
   * @param mi
   * @param opNo
   */
  protected void printSORegRegOperand(MCInst mi, int opNo) {
    MCOperand mo1 = mi.getOperand(opNo);
    MCOperand mo2 = mi.getOperand(opNo+1);
    MCOperand mo3 = mi.getOperand(opNo+2);

    os.print(getRegisterName(mo1.getReg()));

    // print the shift opc
    ARM_AM.ShiftOpc shOpc = ARM_AM.getSORegShOp(mo3.getImm());
    os.printf(", %s", shOpc.name());
    if (shOpc == ARM_AM.ShiftOpc.rrx)
      return;

    os.printf(" %s", getRegisterName(mo2.getReg()));
    Util.assertion(ARM_AM.getSORegOffset(mo3.getImm()) == 0);
  }

  protected void printSORegImmOperand(MCInst mi, int opNo) {
    MCOperand mo1 = mi.getOperand(opNo);
    MCOperand mo2 = mi.getOperand(opNo+1);

    os.print(getRegisterName(mo1.getReg()));

    // print the shift opc
    ARM_AM.ShiftOpc shOpc = ARM_AM.getSORegShOp(mo2.getImm());
    os.printf(", %s", shOpc.name());
    if (shOpc == ARM_AM.ShiftOpc.rrx)
      return;

    os.printf(" #%d", translateShiftImm(ARM_AM.getSORegOffset(mo2.getImm())));
  }

  private static int translateShiftImm(int imm) {
    return imm == 0 ? 32 : imm;
  }

  protected void printBitfieldInvMaskImmOperand(MCInst mi, int opNo) {
    MCOperand mo = mi.getOperand(opNo);
    int v = (int) ~mo.getImm();
    int lsb = Util.countTrailingZeros(v);
    int width = 32 - Util.countLeadingZeros32(v) - lsb;
    Util.assertion(mo.isImm(), "Not a valid bf_inv_mask_imm value!");
    os.printf("#%d, #%d", lsb, width);
  }

  protected void printRegisterList(MCInst mi, int opNo) {
    os.print("{");
    for (int i = opNo, e = mi.getNumOperands(); i < e; ++i) {
      if (i != opNo)
        os.print(',');
      os.print(getRegisterName(mi.getOperand(i).getReg()));
    }
    os.print("}");
  }

  protected void printAddrModeTBH(MCInst mi, int opNo) {
    MCOperand mo1 = mi.getOperand(opNo);
    MCOperand mo2 = mi.getOperand(opNo + 1);
    os.printf("[%s, %s, lsl #1]", getRegisterName(mo1.getReg()), getRegisterName(mo2.getReg()));
  }

  protected void printAddrModeTBB(MCInst mi, int opNo) {
    MCOperand mo1 = mi.getOperand(opNo);
    MCOperand mo2 = mi.getOperand(opNo + 1);
    os.printf("[%s, %s]", getRegisterName(mo1.getReg()), getRegisterName(mo2.getReg()));
  }

  protected void printT2AddrModeSoRegOperand(MCInst mi, int opNo) {
    MCOperand mo1 = mi.getOperand(opNo);
    MCOperand mo2 = mi.getOperand(opNo+1);
    MCOperand mo3 = mi.getOperand(opNo+2);

    os.printf("[%s", getRegisterName(mo1.getReg()));
    Util.assertion(mo2.isReg(), "Invalid so_reg load / store address!");
    os.printf(", %s", getRegisterName(mo2.getReg()));

    int shAmt = (int) mo3.getImm();
    if (shAmt != 0) {
      Util.assertion(shAmt <= 3 && shAmt > 0, "not a valid Thumb2 addressing mode!");
      os.printf(", lsl #%d", shAmt);
    }
    os.print(']');
  }

  protected void printT2AddrModeImm8Operand(MCInst mi, int opNo) {
    MCOperand mo0 = mi.getOperand(opNo);
    MCOperand mo1 = mi.getOperand(opNo+1);
    os.printf("[%s", getRegisterName(mo0.getReg()));

    int offImm = (int) mo1.getImm();
    if (offImm == Integer.MIN_VALUE)
      os.print(", #-0");
    else if (offImm < 0)
      os.printf(", #-%d", -offImm);
    else if (offImm > 0)
      os.printf(", #%d", offImm);
    os.print(']');
  }

  protected void printMSRMaskOperand(MCInst mi, int opNo) {
    MCOperand op = mi.getOperand(opNo);
    int specRegBit = (int) (op.getImm() >>> 4);
    int mask = (int) (op.getImm() & 0xf);
    Util.shouldNotReachHere();
    // TODO
  }

  protected void printCImmediate(MCInst mi, int opNo) {
    os.printf("c%d", mi.getOperand(opNo).getImm());
  }

  protected void printCPSIFlag(MCInst mi, int opNo) {
    MCOperand mo = mi.getOperand(opNo);
    int ifFlag = (int) mo.getImm();
    for (int i = 2; i >= 0; --i)
      os.print(ARM_PROC.IFlagsToString( 1<< i));

    if (ifFlag == 0)
      os.print("none");
  }

  protected void printMandatoryPredicateOperand(MCInst mi, int opNo) {
    ARMCC.CondCodes cc = ARMCC.CondCodes.values()[(int) mi.getOperand(opNo).getImm()];
    os.print(ARMCC.ARMCondCodeToString(cc));
  }

  protected void printThumbITMask(MCInst mi, int opNo) {
    int mask = (int) mi.getOperand(opNo).getImm();
    int condBit0 = (mask >> 4) & 1;
    int numTZ = Util.countTrailingZeros(mask);
    Util.assertion(numTZ <= 3, "invalid IT mask!");
    for (int pos = 3; pos > numTZ; --pos) {
      boolean t = ((mask >> pos) & 1) == condBit0;
      os.print(t ? 't' : 'e');
    }
  }

  protected void printSetendOperand(MCInst mi, int opNo) {
    MCOperand mo = mi.getOperand(opNo);
    os.print(mo.getImm() != 0 ? "be" : "le");
  }

  protected void printAddrMode2Operand(MCInst mi, int opNo) {
    MCOperand mo = mi.getOperand(opNo);
    if (!mo.isReg()) {
      printOperand(mi, opNo);
      return;
    }

    MCOperand mo3 = mi.getOperand(opNo + 2);
    int idxMode = ARM_AM.getAM2IdxMode(mo3.getImm());
    if (idxMode == ARMII.IndexMode.IndexModePost.ordinal()) {
      printAM2PostIndexOp(mi, opNo);
      return;
    }
    printAM2PreOrOffsetIndexOp(mi, opNo);
  }

  private void printAM2PreOrOffsetIndexOp(MCInst mi, int opNo) {
    MCOperand mo1 = mi.getOperand(opNo);
    MCOperand mo2 = mi.getOperand(opNo+1);
    MCOperand mo3 = mi.getOperand(opNo+2);
    os.printf("[%s", getRegisterName(mo1.getReg()));

    if (!mo2.isReg()) {
      int immOff = ARM_AM.getAM2Offset((int) mo3.getImm());
      if (immOff != 0)
        os.printf(", #%s%d", ARM_AM.getAddrOpcStr(ARM_AM.getAM2Op((int) mo3.getImm())),
            ARM_AM.getAM2Offset((int) mo3.getImm()));
      os.print(']');
      return;
    }

    os.printf(", %s%s", ARM_AM.getAddrOpcStr(ARM_AM.getAM2Op((int) mo3.getImm())), getRegisterName(mo2.getReg()));
    int shAmt = ARM_AM.getAM2Offset((int) mo3.getImm());
    if (shAmt != 0)
      os.printf(", %s #%d", ARM_AM.getShiftOpcStr(ARM_AM.getAM2ShiftOpc((int) mo3.getImm())), shAmt);
    os.print(']');
  }

  private void printAM2PostIndexOp(MCInst mi, int opNo) {
    MCOperand mo1 = mi.getOperand(opNo);
    MCOperand mo2 = mi.getOperand(opNo+1);
    MCOperand mo3 = mi.getOperand(opNo+2);
    os.printf("[%s], ", getRegisterName(mo1.getReg()));
    if (!mo2.isReg()) {
      int immOff = ARM_AM.getAM2Offset((int) mo3.getImm());
      os.printf("#%s%d", ARM_AM.getAddrOpcStr(ARM_AM.getAM2Op((int) mo3.getImm())), immOff);
      return;
    }

    os.printf("%s%s", ARM_AM.getAddrOpcStr(ARM_AM.getAM2Op((int) mo3.getImm())), getRegisterName(mo2.getReg()));
    int shAmt = ARM_AM.getAM2Offset((int) mo3.getImm());
    if (shAmt != 0)
      os.printf(", %s #%d", ARM_AM.getShiftOpcStr(ARM_AM.getAM2ShiftOpc((int) mo3.getImm())), shAmt);
  }

  protected void printAddrModeImm12Operand(MCInst mi, int opNo) {
    MCOperand mo1 = mi.getOperand(opNo);
    MCOperand mo2 = mi.getOperand(opNo+1);

    if (!mo1.isReg()) {
      printOperand(mi, opNo);
      return;
    }

    os.printf("[%s", getRegisterName(mo1.getReg()));

    int offImm = (int) mo2.getImm();
    boolean isSub = offImm < 0;
    if (offImm == Integer.MIN_VALUE)
      offImm = 0;
    if (isSub)
      os.printf(", #-%d", -offImm);
    else if (offImm > 0)
      os.printf(", #%d", offImm);
    os.print(']');
  }

  protected void printMemBOption(MCInst mi, int opNo) {
    int val = (int) mi.getOperand(opNo).getImm();
    os.print(ARM_MB.MemBOptToString(val));
  }

  protected void printCPSIMod(MCInst mi, int opNo) {
    int val = (int) mi.getOperand(opNo).getImm();
    os.print(ARM_PROC.IModToString(val));
  }

  protected void printPImmediate(MCInst mi, int opNo) {
    os.printf("p%d", mi.getOperand(opNo).getImm());
  }

  protected void printPredicateOperand(MCInst mi, int opNo) {
    ARMCC.CondCodes cc = ARMCC.CondCodes.values()[(int) mi.getOperand(opNo).getImm()];
    if (cc != ARMCC.CondCodes.AL)
      os.print(ARMCC.ARMCondCodeToString(cc));
  }

  protected void printSBitModifierOperand(MCInst mi, int opNo) {
    int reg = mi.getOperand(opNo).getReg();
    if (reg != 0) {
      Util.assertion(reg == ARMGenRegisterNames.CPSR, "Expect ARM CPSR register");
      os.print('s');
    }
  }
}
