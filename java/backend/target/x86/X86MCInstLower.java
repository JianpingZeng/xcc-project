/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2019, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.target.x86;

import backend.codegen.MVT;
import backend.codegen.MachineInstr;
import backend.codegen.MachineOperand;
import backend.mc.*;
import backend.mc.MCSymbol.MCContext;
import backend.support.NameMangler;
import backend.value.GlobalValue;
import tools.Util;

import static backend.target.x86.X86GenRegisterNames.*;

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
    X86TargetLowering tli = (X86TargetLowering) asmPrinter.tm.getTargetLowering();
    return tli.getPICBaseSymbol(asmPrinter.mf, ctx);
  }

  /**
   * Lower an MO_GlobalAddress or MO_ExternalSymbol operand to an MCSymbol.
   * @param mo
   * @return
   */
  public MCSymbol getSymbolFromOperand(MachineOperand mo) {
    Util.assertion(mo.isGlobalAddress() || mo.isExternalSymbol(), "Isn't a global address or external symbol?");
    String name = "";
    if (mo.isGlobalAddress()) {
      boolean isImplicitlyPrivate = false;
      switch (mo.getTargetFlags()) {
        case X86II.MO_DARWIN_STUB:
        case X86II.MO_DARWIN_NONLAZY:
        case X86II.MO_DARWIN_NONLAZY_PIC_BASE:
        case X86II.MO_DARWIN_HIDDEN_NONLAZY_PIC_BASE:
          isImplicitlyPrivate = true;
          break;
      }

      GlobalValue gv = mo.getGlobal();
      name = mangler.getMangledNameWithPrefix(gv, isImplicitlyPrivate);
      if (getSubtarget().isTargetCygMing()) {
        Util.shouldNotReachHere("Cygwin is not supported");;
      }
    }
    else {
      Util.assertion(mo.isExternalSymbol());
      name += asmPrinter.mai.getGlobalPrefix();
      name += mo.getSymbolName();
    }

    // If the target flags on the operand changes the name of the symbol, do that
    // before we return the symbol.
    switch (mo.getTargetFlags()) {
      default: break;
      case X86II.MO_DLLIMPORT:
        // Handle dllimport linkage.
        name = "__imp_" + name;
        break;
      case X86II.MO_DARWIN_NONLAZY:
      case X86II.MO_DARWIN_NONLAZY_PIC_BASE:
      case X86II.MO_DARWIN_HIDDEN_NONLAZY_PIC_BASE:
      case X86II.MO_DARWIN_STUB: {
        name += "$non_lazy_ptr";
        MCSymbol sym = ctx.getOrCreateSymbol(name);
        Util.shouldNotReachHere("Mach-O is not supported");
        break;
      }
    }
    return ctx.getOrCreateSymbol(name);
  }

  public MCOperand lowerSymbolOperand(MachineOperand mo, MCSymbol sym) {
    MCExpr expr = null;
    X86MCTargetExpr.VariantKind refKind = X86MCTargetExpr.VariantKind.Invalid;

    switch (mo.getTargetFlags()) {
      default: Util.shouldNotReachHere("Unknown target flag on GV operand");
      case X86II.MO_NO_FLAG:    // No flag.
        // These affect the name of the symbol, not any suffix.
      case X86II.MO_DARWIN_NONLAZY:
      case X86II.MO_DLLIMPORT:
      case X86II.MO_DARWIN_STUB:
        break;

      case X86II.MO_TLSGD:     refKind = X86MCTargetExpr.VariantKind.TLSGD; break;
      case X86II.MO_GOTTPOFF:  refKind = X86MCTargetExpr.VariantKind.GOTTPOFF; break;
      case X86II.MO_INDNTPOFF: refKind = X86MCTargetExpr.VariantKind.INDNTPOFF; break;
      case X86II.MO_TPOFF:     refKind = X86MCTargetExpr.VariantKind.TPOFF; break;
      case X86II.MO_NTPOFF:    refKind = X86MCTargetExpr.VariantKind.NTPOFF; break;
      case X86II.MO_GOTPCREL:  refKind = X86MCTargetExpr.VariantKind.GOTPCREL; break;
      case X86II.MO_GOT:       refKind = X86MCTargetExpr.VariantKind.GOT; break;
      case X86II.MO_GOTOFF:    refKind = X86MCTargetExpr.VariantKind.GOTOFF; break;
      case X86II.MO_PLT:       refKind = X86MCTargetExpr.VariantKind.PLT; break;
      case X86II.MO_PIC_BASE_OFFSET:
      case X86II.MO_DARWIN_NONLAZY_PIC_BASE:
      case X86II.MO_DARWIN_HIDDEN_NONLAZY_PIC_BASE:
        expr = MCSymbolRefExpr.create(sym);
        // Subtract the pic base.
        expr = MCBinaryExpr.createSub(expr,
          MCSymbolRefExpr.create(getPICBaseSymbol()),
        ctx);
        break;
    }

    if (expr == null) {
      if (refKind == X86MCTargetExpr.VariantKind.Invalid)
        expr = MCSymbolRefExpr.create(sym);
    else
      expr = X86MCTargetExpr.create(sym, refKind, ctx);
    }

    if (mo.isJumpTableIndex() && mo.getOffset() != 0)
      expr = MCBinaryExpr.createAdd(expr,
        MCConstantExpr.create(mo.getOffset(), ctx),
    ctx);
    return MCOperand.createExpr(expr);
    }

  private static int getX86SubSuperRegister(int reg, int vt) {
    return getX86SubSuperRegister(reg, vt, false);
  }

  /**
   * X86 utility function. It returns the sub or super register of a
   * specific X86 register.
   * <p>
   * The returned may be sub-register or super-register of the specified X86
   * register {@code reg} and it is just suitable to fit the bit width of MVT
   * {@code vt}.
   * e.g. getX86SubSuperRegister(X86GenInstrNames.EAX, MVT.i16) return AX.
   * </p>
   *
   * @param reg
   * @param vt
   * @param high default to false.
   * @return
   */
  private static int getX86SubSuperRegister(int reg, int vt, boolean high) {
    switch (vt) {
      default:
        return reg;
      case MVT.i8:
        if (high) {
          switch (reg) {
            default:
              return 0;
            case AH:
            case AL:
            case AX:
            case EAX:
            case RAX:
              return AH;
            case DH:
            case DL:
            case DX:
            case EDX:
            case RDX:
              return DH;
            case CH:
            case CL:
            case CX:
            case ECX:
            case RCX:
              return CH;
            case BH:
            case BL:
            case BX:
            case EBX:
            case RBX:
              return BH;
          }
        } else {
          switch (reg) {
            default:
              return 0;
            case AH:
            case AL:
            case AX:
            case EAX:
            case RAX:
              return AL;
            case DH:
            case DL:
            case DX:
            case EDX:
            case RDX:
              return DL;
            case CH:
            case CL:
            case CX:
            case ECX:
            case RCX:
              return CL;
            case BH:
            case BL:
            case BX:
            case EBX:
            case RBX:
              return BL;
            case SIL:
            case SI:
            case ESI:
            case RSI:
              return SIL;
            case DIL:
            case DI:
            case EDI:
            case RDI:
              return DIL;
            case BPL:
            case BP:
            case EBP:
            case RBP:
              return BPL;
            case SPL:
            case SP:
            case ESP:
            case RSP:
              return SPL;
            case R8B:
            case R8W:
            case R8D:
            case R8:
              return R8B;
            case R9B:
            case R9W:
            case R9D:
            case R9:
              return R9B;
            case R10B:
            case R10W:
            case R10D:
            case R10:
              return R10B;
            case R11B:
            case R11W:
            case R11D:
            case R11:
              return R11B;
            case R12B:
            case R12W:
            case R12D:
            case R12:
              return R12B;
            case R13B:
            case R13W:
            case R13D:
            case R13:
              return R13B;
            case R14B:
            case R14W:
            case R14D:
            case R14:
              return R14B;
            case R15B:
            case R15W:
            case R15D:
            case R15:
              return R15B;
          }
        }
      case MVT.i16:
        switch (reg) {
          default:
            return reg;
          case AH:
          case AL:
          case AX:
          case EAX:
          case RAX:
            return AX;
          case DH:
          case DL:
          case DX:
          case EDX:
          case RDX:
            return DX;
          case CH:
          case CL:
          case CX:
          case ECX:
          case RCX:
            return CX;
          case BH:
          case BL:
          case BX:
          case EBX:
          case RBX:
            return BX;
          case SIL:
          case SI:
          case ESI:
          case RSI:
            return SI;
          case DIL:
          case DI:
          case EDI:
          case RDI:
            return DI;
          case BPL:
          case BP:
          case EBP:
          case RBP:
            return BP;
          case SPL:
          case SP:
          case ESP:
          case RSP:
            return SP;
          case R8B:
          case R8W:
          case R8D:
          case R8:
            return R8W;
          case R9B:
          case R9W:
          case R9D:
          case R9:
            return R9W;
          case R10B:
          case R10W:
          case R10D:
          case R10:
            return R10W;
          case R11B:
          case R11W:
          case R11D:
          case R11:
            return R11W;
          case R12B:
          case R12W:
          case R12D:
          case R12:
            return R12W;
          case R13B:
          case R13W:
          case R13D:
          case R13:
            return R13W;
          case R14B:
          case R14W:
          case R14D:
          case R14:
            return R14W;
          case R15B:
          case R15W:
          case R15D:
          case R15:
            return R15W;
        }
      case MVT.i32:
        switch (reg) {
          default:
            return reg;
          case AH:
          case AL:
          case AX:
          case EAX:
          case RAX:
            return EAX;
          case DH:
          case DL:
          case DX:
          case EDX:
          case RDX:
            return EDX;
          case CH:
          case CL:
          case CX:
          case ECX:
          case RCX:
            return ECX;
          case BH:
          case BL:
          case BX:
          case EBX:
          case RBX:
            return EBX;
          case SIL:
          case SI:
          case ESI:
          case RSI:
            return ESI;
          case DIL:
          case DI:
          case EDI:
          case RDI:
            return EDI;
          case BPL:
          case BP:
          case EBP:
          case RBP:
            return EBP;
          case SPL:
          case SP:
          case ESP:
          case RSP:
            return ESP;
          case R8B:
          case R8W:
          case R8D:
          case R8:
            return R8D;
          case R9B:
          case R9W:
          case R9D:
          case R9:
            return R9D;
          case R10B:
          case R10W:
          case R10D:
          case R10:
            return R10D;
          case R11B:
          case R11W:
          case R11D:
          case R11:
            return R11D;
          case R12B:
          case R12W:
          case R12D:
          case R12:
            return R12D;
          case R13B:
          case R13W:
          case R13D:
          case R13:
            return R13D;
          case R14B:
          case R14W:
          case R14D:
          case R14:
            return R14D;
          case R15B:
          case R15W:
          case R15D:
          case R15:
            return R15D;
        }
      case MVT.i64:
        switch (reg) {
          default:
            return reg;
          case AH:
          case AL:
          case AX:
          case EAX:
          case RAX:
            return RAX;
          case DH:
          case DL:
          case DX:
          case EDX:
          case RDX:
            return RDX;
          case CH:
          case CL:
          case CX:
          case ECX:
          case RCX:
            return RCX;
          case BH:
          case BL:
          case BX:
          case EBX:
          case RBX:
            return RBX;
          case SIL:
          case SI:
          case ESI:
          case RSI:
            return RSI;
          case DIL:
          case DI:
          case EDI:
          case RDI:
            return RDI;
          case BPL:
          case BP:
          case EBP:
          case RBP:
            return RBP;
          case SPL:
          case SP:
          case ESP:
          case RSP:
            return RSP;
          case R8B:
          case R8W:
          case R8D:
          case R8:
            return R8;
          case R9B:
          case R9W:
          case R9D:
          case R9:
            return R9;
          case R10B:
          case R10W:
          case R10D:
          case R10:
            return R10;
          case R11B:
          case R11W:
          case R11D:
          case R11:
            return R11;
          case R12B:
          case R12W:
          case R12D:
          case R12:
            return R12;
          case R13B:
          case R13W:
          case R13D:
          case R13:
            return R13;
          case R14B:
          case R14W:
          case R14D:
          case R14:
            return R14;
          case R15B:
          case R15W:
          case R15D:
          case R15:
            return R15;
        }
    }
  }

  private static void lower_subreg32(MCInst inst,
                                     int opNo) {
    int reg = inst.getOperand(opNo).getReg();
    if (reg != 0)
      inst.getOperand(opNo).setReg(getX86SubSuperRegister(reg, MVT.i32));
  }

  /**
   * Things like MOVZX16rr8 -> MOVZX32rr8.
   * @param outMI
   * @param newOpc
   */
  private static void lowerSubReg32_Op0(MCInst outMI,
                                   int newOpc) {
    outMI.setOpcode(newOpc);
    lower_subreg32(outMI, 0);
  }

  /**
   * R = setb   -> R = sbb R, R
   * @param outMI
   * @param newOpc
   */
  private static void lowerUnaryToTwoAddr(MCInst outMI,
                                          int newOpc) {
    outMI.setOpcode(newOpc);
    outMI.addOperand(outMI.getOperand(0));
    outMI.addOperand(outMI.getOperand(0));
  }

  private static void lower_lea64_32mem(MCInst outMI,
                                        int opNo) {
    // Convert registers in the addr mode according to subreg64.
    for (int i = 0; i < 4; i++) {
      if (!outMI.getOperand(opNo+i).isReg()) continue;

      int reg = outMI.getOperand(opNo+i).getReg();
      if (reg == 0) continue;
      outMI.getOperand(opNo+i).setReg(getX86SubSuperRegister(reg, MVT.i64));
    }
  }

  public void lower(MachineInstr mi, MCInst outMI) {
    outMI.setOpcode(mi.getOpcode());

    for (int i = 0, e = mi.getNumOperands(); i < e; i++) {
      MachineOperand mo = mi.getOperand(i);
      MCOperand mcOp = null;
      switch (mo.getType()) {
        default:
          mi.dump();
          Util.shouldNotReachHere("unknown operand type");
          break;
        case MO_Register:
          // Ignore all implicit register operands.
          if (mo.isImplicit()) continue;
          mcOp = MCOperand.createReg(mo.getReg());
          break;
        case MO_Immediate:
          mcOp = MCOperand.createImm(mo.getImm());
          break;
        case MO_MachineBasicBlock:
          mcOp = MCOperand.createExpr(MCSymbolRefExpr.create(
              mo.getMBB().getSymbol(ctx)));
          break;
        case MO_GlobalAddress:
          mcOp = lowerSymbolOperand(mo, getSymbolFromOperand(mo));
          break;
        case MO_ExternalSymbol:
          mcOp = lowerSymbolOperand(mo, getSymbolFromOperand(mo));
          break;
        case MO_JumpTableIndex:
          mcOp = lowerSymbolOperand(mo, asmPrinter.getJTISymbol(mo.getIndex(), false));
          break;
        case MO_ConstantPoolIndex:
          mcOp = lowerSymbolOperand(mo, asmPrinter.getCPISymbol(mo.getIndex()));
          break;
        case MO_BlockAddress:
          mcOp = lowerSymbolOperand(mo, asmPrinter.getBlockAddressSymbol(mo.getBlockAddress()));
      }
      outMI.addOperand(mcOp);
    }

    // Handle a few special cases to eliminate operand modifiers.
    switch (outMI.getOpcode()) {
      case X86GenInstrNames.LEA64_32r:
        lower_lea64_32mem(outMI, 1);
        break;
      case X86GenInstrNames.MOVZX16rr8:
        lowerSubReg32_Op0(outMI, X86GenInstrNames.MOVZX32rr8);
        break;
      case X86GenInstrNames.MOVZX16rm8:
        lowerSubReg32_Op0(outMI, X86GenInstrNames.MOVZX32rm8);
        break;
      case X86GenInstrNames.MOVSX16rr8:
        lowerSubReg32_Op0(outMI, X86GenInstrNames.MOVSX32rr8);
        break;
      case X86GenInstrNames.MOVSX16rm8:
        lowerSubReg32_Op0(outMI, X86GenInstrNames.MOVSX32rm8);
        break;
      case X86GenInstrNames.MOVZX64rr32:
        lowerSubReg32_Op0(outMI, X86GenInstrNames.MOV32rr);
        break;
      case X86GenInstrNames.MOVZX64rm32:
        lowerSubReg32_Op0(outMI, X86GenInstrNames.MOV32rm);
        break;
      case X86GenInstrNames.MOV64ri64i32:
        lowerSubReg32_Op0(outMI, X86GenInstrNames.MOV32ri);
        break;
      case X86GenInstrNames.MOVZX64rr8:
        lowerSubReg32_Op0(outMI, X86GenInstrNames.MOVZX32rr8);
        break;
      case X86GenInstrNames.MOVZX64rm8:
        lowerSubReg32_Op0(outMI, X86GenInstrNames.MOVZX32rm8);
        break;
      case X86GenInstrNames.MOVZX64rr16:
        lowerSubReg32_Op0(outMI, X86GenInstrNames.MOVZX32rr16);
        break;
      case X86GenInstrNames.MOVZX64rm16:
        lowerSubReg32_Op0(outMI, X86GenInstrNames.MOVZX32rm16);
        break;
      case X86GenInstrNames.MOV8r0:
        lowerUnaryToTwoAddr(outMI, X86GenInstrNames.XOR8rr);
        break;
      case X86GenInstrNames.MOV32r0:
        lowerUnaryToTwoAddr(outMI, X86GenInstrNames.XOR32rr);
        break;
      case X86GenInstrNames.MMX_V_SET0:
        lowerUnaryToTwoAddr(outMI, X86GenInstrNames.MMX_PXORrr);
        break;
      case X86GenInstrNames.MMX_V_SETALLONES:
        lowerUnaryToTwoAddr(outMI, X86GenInstrNames.MMX_PCMPEQDrr);
        break;
      case X86GenInstrNames.FsFLD0SS:
      case X86GenInstrNames.FsFLD0SD:
        lowerUnaryToTwoAddr(outMI, X86GenInstrNames.PXORrr);
        break;
      case X86GenInstrNames.V_SET0:
        lowerUnaryToTwoAddr(outMI, X86GenInstrNames.XORPSrr);
        break;
      case X86GenInstrNames.V_SETALLONES:
        lowerUnaryToTwoAddr(outMI, X86GenInstrNames.PCMPEQDrr);
        break;
      case X86GenInstrNames.MOV16r0:
        // MOV16r0 -> MOV32r0
        lowerSubReg32_Op0(outMI, X86GenInstrNames.MOV32r0);
        // MOV32r0 -> XOR32rr
        lowerUnaryToTwoAddr(outMI, X86GenInstrNames.XOR32rr);
        break;
      case X86GenInstrNames.MOV64r0:
        // MOV64r0 -> MOV32r0
        lowerSubReg32_Op0(outMI, X86GenInstrNames.MOV32r0);
        // MOV32r0 -> XOR32rr
        lowerUnaryToTwoAddr(outMI, X86GenInstrNames.XOR32rr);
        break;
    }
  }
}
