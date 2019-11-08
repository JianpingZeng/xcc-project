package backend.target.x86;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import backend.codegen.*;
import backend.mc.*;
import backend.pass.AnalysisUsage;
import backend.support.CallingConv;
import backend.support.IntStatistic;
import backend.target.TargetData;
import backend.target.TargetMachine;
import backend.target.TargetOpcodes;
import backend.target.TargetRegisterInfo;
import backend.type.FunctionType;
import backend.type.Type;
import backend.value.Function;
import backend.value.GlobalValue;
import backend.value.Module;
import backend.value.Value;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Util;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.TreeMap;

import static backend.target.TargetMachine.RelocModel.PIC_;
import static backend.target.TargetMachine.RelocModel.Static;
import static backend.target.x86.X86GenRegisterNames.*;
import static backend.target.x86.X86II.MO_GOT_ABSOLUTE_ADDRESS;
import static backend.target.x86.X86MachineFunctionInfo.NameDecorationStyle.FastCall;
import static backend.target.x86.X86MachineFunctionInfo.NameDecorationStyle.StdCall;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class X86AsmPrinter extends AsmPrinter {
  /**
   * A statistic for indicating the numbeer of emitted machine
   * instruction by this asm printer.
   */
  public static final IntStatistic EmittedInsts = new IntStatistic(
      "EmittedInstrs", "Number of machine instrs printed");

  private TObjectIntHashMap<MachineBasicBlock> mbbNumber;
  private X86Subtarget subtarget;
  private HashMap<Function, X86MachineFunctionInfo> functionInfoMap;

  private TreeMap<String, String> gvStubs;
  private TreeMap<String, String> hiddenGVStubs;
  private TreeMap<String, String> fnStubs;

  public X86AsmPrinter(OutputStream os,
                       X86TargetMachine tm,
                       MCSymbol.MCContext ctx,
                       MCStreamer streamer,
                       MCAsmInfo mai) {
    super(os, tm, ctx, streamer, mai);
    mbbNumber = new TObjectIntHashMap<>();
    subtarget = tm.getSubtarget();
    functionInfoMap = new HashMap<>();
    gvStubs = new TreeMap<>();
    hiddenGVStubs = new TreeMap<>();
    fnStubs = new TreeMap<>();
  }

  public X86Subtarget getSubtarget() {
    return subtarget;
  }

  @Override
  public String getPassName() {
    return "X86 AT&T style assembly printer";
  }

  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    au.setPreservedAll();
    if (subtarget.isTargetDarwin() || subtarget.isTargetELF()
        || subtarget.isTargetCygMing()) {
      au.addRequired(MachineModuleInfo.class);
    }
    // TODO dwarf writer.
    super.getAnalysisUsage(au);
  }

  @Override
  public void emitEndOfAsmFile(Module module) {
    super.emitEndOfAsmFile(module);
  }

  @Override
  protected void emitInstruction(MachineInstr mi) {
    X86MCInstLower InstLowering = new X86MCInstLower(outContext, mangler, this);
    switch (mi.getOpcode()) {
      case TargetOpcodes.PROLOG_LABEL: {
        if (!verboseAsm)
          return;

        os.printf("\t%sDEBUG_VALUE: ", mai.getCommentString());
        int nOps = mi.getNumOperands();
        // TODO debug information
      }
      case X86GenInstrNames.MOVPC32r: {
        MCInst inst = new MCInst();
        // This is a pseudo op for a two instruction sequence with a label, which
        // looks like:
        //     call "L1$pb"
        // "L1$pb":
        //     popl %esi
        MCSymbol picBase = InstLowering.getPICBaseSymbol();
        inst.setOpcode(X86GenInstrNames.CALLpcrel32);
        inst.addOperand(MCOperand.createExpr(MCSymbolRefExpr.create(picBase)));

        outStreamer.emitInstruction(inst);
        outStreamer.emitLabel(picBase);
        inst.setOpcode(X86GenInstrNames.POP32r);
        inst.setOperand(0, MCOperand.createReg(mi.getOperand(0).getReg()));
        outStreamer.emitInstruction(inst);
        return;
      }
      case X86GenInstrNames.ADD32ri: {
        // Lower the MO_GOT_ABSOLUTE_ADDRESS form of ADD32ri.
        if (mi.getOperand(2).getTargetFlags() != MO_GOT_ABSOLUTE_ADDRESS)
          break;

        // Okay, we have something like:
        //  EAX = ADD32ri EAX, MO_GOT_ABSOLUTE_ADDRESS(@MYGLOBAL)

        // For this, we want to print something like:
        //   MYGLOBAL + (. - PICBASE)
        // However, we can't generate a ".", so just emit a new label here and refer
        // to it.  We know that this operand flag occurs at most once per function.
        String prefix = mai.getPrivateGlobalPrefix();
        MCSymbol dotSym = outContext.getOrCreateSymbol(prefix + "picbaseref" + getFunctionNumber());
        outStreamer.emitLabel(dotSym);

        // Now that we have emitted the label, lower the complex operand expression.
        MCSymbol opSym = InstLowering.getSymbolFromOperand(mi.getOperand(2));
        MCExpr dotExpr = MCSymbolRefExpr.create(dotSym);
        MCExpr picBase = MCSymbolRefExpr.create(InstLowering.getPICBaseSymbol());
        dotExpr = MCBinaryExpr.createSub(dotExpr, picBase, outContext);
        dotExpr = MCBinaryExpr.createAdd(MCSymbolRefExpr.create(opSym), dotExpr, outContext);

        MCInst inst = new MCInst();
        inst.setOpcode(X86GenInstrNames.ADD32ri);
        inst.addOperand(MCOperand.createReg(mi.getOperand(0).getReg()));
        inst.addOperand(MCOperand.createReg(mi.getOperand(1).getReg()));
        inst.addOperand(MCOperand.createExpr(dotExpr));
        outStreamer.emitInstruction(inst);
        return;
      }
    }
    MCInst inst = new MCInst();
    InstLowering.lower(mi, inst);
    outStreamer.emitInstruction(inst);
  }

  public void printi8mem(MachineInstr mi, int opNo) {
    printMemReference(mi, opNo);
  }

  public void printi16mem(MachineInstr mi, int opNo) {
    printMemReference(mi, opNo);
  }

  public void printi32mem(MachineInstr mi, int opNo) {
    printMemReference(mi, opNo);
  }

  public void printi64mem(MachineInstr mi, int opNo) {
    printMemReference(mi, opNo);
  }

  public void printf32mem(MachineInstr mi, int opNo) {
    printMemReference(mi, opNo);
  }

  public void printf64mem(MachineInstr mi, int opNo) {
    printMemReference(mi, opNo);
  }

  public void printf80mem(MachineInstr mi, int opNo) {
    printMemReference(mi, opNo);
  }

  public void printf128mem(MachineInstr mi, int opNo) {
    printMemReference(mi, opNo);
  }

  /**
   * Print a raw symbol reference operand. THis handles jump tables, constant
   * pools, global address and external symbols, all of which part to a label
   * with various suffixes for relocation types etc.
   *
   * @param mo
   */
  private void printSymbolOperand(MachineOperand mo) {
    switch (mo.getType()) {
      default:
        Util.shouldNotReachHere("Unknown symbol type!");
      case MO_JumpTableIndex:
        getJTISymbol(mo.getIndex(), false).print(os);
        break;
      case MO_ConstantPoolIndex:
        getCPISymbol(mo.getIndex()).print(os);
        printOffset(mo.getOffset());
        break;
      case MO_GlobalAddress:
        GlobalValue gv = mo.getGlobal();
        int ts = mo.getTargetFlags();
        MCSymbol gvSym = null;
        if (ts == X86II.MO_DARWIN_STUB)
          gvSym = getSymbolWithGlobalValueBase(gv, "$stub", true);
        else if (ts == X86II.MO_DARWIN_NONLAZY ||
            ts == X86II.MO_DARWIN_NONLAZY_PIC_BASE ||
            ts == X86II.MO_DARWIN_HIDDEN_NONLAZY ||
            ts == X86II.MO_DARWIN_HIDDEN_NONLAZY_PIC_BASE) {
          gvSym = getSymbolWithGlobalValueBase(gv, "$non_lazy_ptr", true);
        } else
          gvSym = getGlobalValueSymbol(gv);

        // handle DLLImport linkage
        if (ts == X86II.MO_DLLIMPORT)
          gvSym = outContext.getOrCreateSymbol("__imp__" + gvSym.getName());

        if (ts == X86II.MO_DARWIN_NONLAZY || ts == X86II.MO_DARWIN_NONLAZY_PIC_BASE) {
          MCSymbol sym = getSymbolWithGlobalValueBase(gv,
              "non_lazy_ptr", true);
          Util.shouldNotReachHere("Darwin not supported!");
        } else if (ts == X86II.MO_DARWIN_HIDDEN_NONLAZY ||
            ts == X86II.MO_DARWIN_HIDDEN_NONLAZY_PIC_BASE) {
          Util.shouldNotReachHere("Darwin not supported!");
        } else if (ts == X86II.MO_DARWIN_STUB)
          Util.shouldNotReachHere("Darwin not supported!");

        // If the name begins with a dollar-sign, enclose it in parens.  We do this
        // to avoid having it look like an integer immediate to the assembler.
        if (!gvSym.getName().startsWith("$"))
          gvSym.print(os);
        else {
          os.print('(');
          gvSym.print(os);
          os.print(')');
        }

        printOffset(mo.getOffset());
        break;
      case MO_ExternalSymbol:
        MCSymbol symToPrint = null;
        if (mo.getTargetFlags() == X86II.MO_DARWIN_STUB) {
          String name = mo.getSymbolName() + "$stub";
          Util.shouldNotReachHere("Darwin not supported!");
        } else
          symToPrint = getExternalSymbolSymbol(mo.getSymbolName());

        // If the name begins with a dollar-sign, enclose it in parens.  We do this
        // to avoid having it look like an integer immediate to the assembler.
        if (!symToPrint.getName().startsWith("$"))
          symToPrint.print(os);
        else {
          os.print('(');
          symToPrint.print(os);
          os.print(')');
        }
        break;
    }

    switch (mo.getTargetFlags()) {
      default:
        Util.shouldNotReachHere("Unknown target flag on GlobalValue operand");
      case X86II.MO_NO_FLAG:
        break;
      case X86II.MO_DARWIN_NONLAZY:
      case X86II.MO_DARWIN_HIDDEN_NONLAZY:
      case X86II.MO_DLLIMPORT:
      case X86II.MO_DARWIN_STUB:
        // there affect the name of symbol, not any suffix.
        break;
      case MO_GOT_ABSOLUTE_ADDRESS:
        os.printf(" + [.-");
        printPICBaseSymbol();
        os.printf("]");
        break;
      case X86II.MO_PIC_BASE_OFFSET:
      case X86II.MO_DARWIN_NONLAZY_PIC_BASE:
      case X86II.MO_DARWIN_HIDDEN_NONLAZY_PIC_BASE:
        os.print('-');
        printPICBaseSymbol();
        break;
      case X86II.MO_TLSGD:
        os.print("@TLSGD");
        break;
      case X86II.MO_GOTTPOFF:
        os.print("@GOTTPOFF");
        break;
      case X86II.MO_INDNTPOFF:
        os.print("@INDNTPOFF");
        break;
      case X86II.MO_TPOFF:
        os.print("@TPOFF");
        break;
      case X86II.MO_NTPOFF:
        os.print("@NTPOFF");
        break;
      case X86II.MO_GOTPCREL:
        os.print("@GOTPCREL");
        break;
      case X86II.MO_GOT:
        os.print("@GOT");
        break;
      case X86II.MO_GOTOFF:
        os.print("@GOTOFF");
        break;
      case X86II.MO_PLT:
        os.print("@PLT");
        break;
    }
  }

  private void printPICBaseSymbol() {
    X86TargetLowering tli = (X86TargetLowering) tm.getTargetLowering();
    tli.getPICBaseSymbol(mf, outContext).print(os);
  }

  public void printi128mem(MachineInstr mi, int opNo) {
    printMemReference(mi, opNo);
  }

  public void printlea64_32mem(MachineInstr mi, int opNo) {
    printMemReference(mi, opNo, "subreg64");
  }

  public void printPICLabel(MachineInstr mi, int opNo) {
    printMemReference(mi, opNo);
  }

  public void printlea64mem(MachineInstr mi, int opNo) {
    printMemReference(mi, opNo);
  }

  public void printlea32mem(MachineInstr mi, int opNo) {
    printMemReference(mi, opNo);
  }

  @Override
  protected void printAsmMemoryOperand(MachineInstr mi, int opNo, int asmVariant, String extra) {
    super.printAsmMemoryOperand(mi, opNo, asmVariant, extra);
  }

  @Override
  protected void printAsmOperand(MachineInstr mi, int opNo, int asmVariant, String extra) {
    super.printAsmOperand(mi, opNo, asmVariant, extra);
  }

  private void printAsmMRegister(MachineOperand mo, char mode) {

  }

  /**
   * This method is called used for print assembly code for each
   * machine instruction in {@code mf}.
   *
   * @param mf
   * @return
   */
  @Override
  public boolean runOnMachineFunction(MachineFunction mf) {
    setupMachineFunction(mf);

    if (subtarget.isTargetCOFF()) {
      Function f = mf.getFunction();
      os.print("\t.def\t");
      curFuncSym.print(os);
      os.print(";\t.scl\t");
      Util.shouldNotReachHere("COFF is not supported");
    }
    emitFunctionHeader();
    emitFunctionBody();
    return false;
  }

  public void printOperand(MachineInstr mi, int opNo) {
    printOperand(mi, opNo, null);
  }

  public void printOperand(MachineInstr mi, int opNo, String modifier) {
    printOperand(mi, opNo, modifier, false);
  }

  /**
   * Theses methods are used by the tablegen created instruction printer.
   *
   * @param mi
   * @param opNo
   * @param modifier
   */
  public void printOperand(MachineInstr mi, int opNo, String modifier, boolean notRIPRel) {
    MachineOperand mo = mi.getOperand(opNo);
    TargetRegisterInfo regInfo = tm.getRegisterInfo();
    switch (mo.getType()) {
      case MO_Register: {
        Util.assertion(TargetRegisterInfo.isPhysicalRegister(mo.getReg()),
            "Virtual register should not make it this far!");

        os.print("%");
        int reg = mo.getReg();
        if (modifier != null && modifier.substring(0, 6).equals("subreg")) {
          String bits = modifier.substring(6, 8);
          int vt = bits.equals("64") ? MVT.i64 :
              (bits.equals("32") ?
                  MVT.i32 :
                  (bits.equals("16") ? MVT.i16 : MVT.i8));
          reg = getX86SubSuperRegister(reg, vt);
        }

        os.print(regInfo.getAsmName(reg).toLowerCase());
        return;
      }
      case MO_Immediate: {
        if (modifier == null || (!modifier.equals("debug") && !modifier.equals("mem")))
          os.print("$");
        os.print(mo.getImm());
        return;
      }
      case MO_JumpTableIndex:
      case MO_ConstantPoolIndex:
      case MO_GlobalAddress:
      case MO_ExternalSymbol:
        os.print('$');
        printSymbolOperand(mo);
        break;
      default:
        os.print("<unknown operand type>");
    }
  }

  private void printOffset(long offset) {
    if (offset > 0)
      os.printf("+%d", offset);
    else if (offset < 0)
      os.print(offset);
  }

  private static boolean shouldPrintGOT(TargetMachine tm, X86Subtarget subtarget) {
    return subtarget.isPICStyleGOT() && tm.getRelocationModel() == PIC_;
  }

  private static X86MachineFunctionInfo calculateFunctionInfo(
      Function f,
      TargetData td) {
    X86MachineFunctionInfo info = new X86MachineFunctionInfo();
    long size = 0;

    switch (f.getCallingConv()) {
      case X86_StdCall:
        info.setDecorationStyle(StdCall);
        break;
      case X86_FastCall:
        info.setDecorationStyle(FastCall);
        break;
      default:
        return info;
    }

    int argNum = 1;
    for (Value arg : f.getArgumentList()) {
      Type ty = arg.getType();
      size += ((td.getTypePaddedSize(ty) + 3) / 4) * 4;
    }

    info.setBytesToPopOnReturn((int) size);
    return info;
  }

  private String decorateName(String name, GlobalValue gv) {
    if (!(gv instanceof Function))
      return name;

    Function f = (Function) gv;
    CallingConv cc = f.getCallingConv();
    if (cc != CallingConv.X86_StdCall && cc != CallingConv.X86_FastCall)
      return name;

    if (!subtarget.isTargetCygMing())
      return name;

    X86MachineFunctionInfo fi = null;
    if (!functionInfoMap.containsKey(f)) {
      functionInfoMap.put(f, calculateFunctionInfo(f, tm.getTargetData()));
      fi = functionInfoMap.get(f);
    } else {
      fi = functionInfoMap.get(f);
    }

    FunctionType fy = f.getFunctionType();
    switch (fi.getDecorationStyle()) {
      case None:
      case StdCall:
        break;
      case FastCall:
        if (name.charAt(0) == '_')
          name = '@' + name.substring(1);
        else
          name = '@' + name;
        break;
      default:
        Util.assertion(false, "Unsupported DecorationStyle");
    }
    return name;
  }

  private static boolean shouldPrintStub(TargetMachine tm, X86Subtarget subtarget) {
    return subtarget.isPICStyleStubPIC() && tm.getRelocationModel() != Static;
  }

  private static String getPICLabelString(int fnNumber,
                                          MCAsmInfo tai,
                                          X86Subtarget subtarget) {
    StringBuilder label = new StringBuilder();
    if (subtarget.isTargetDarwin())
      label.append("\"L").append(fnNumber).append("$pb\"");
    else if (subtarget.isTargetELF())
      label.append(".Lllvm$").append(fnNumber).append(".$piclabel");
    else
      Util.assertion(false, "Don't known how to print PIC label!\n");
    return label.toString();
  }

  private static boolean shouldPrintPLT(TargetMachine tm, X86Subtarget subtarget) {
    return subtarget.isTargetELF() && tm.getRelocationModel() == PIC_ &&
        (subtarget.isPICStyleRIPRel() || subtarget.isPICStyleGOT());
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

  private void printMemReference(MachineInstr mi, int opNo) {
    printMemReference(mi, opNo, null);
  }

  private void printMemReference(MachineInstr mi, int opNo, String modifier) {
    Util.assertion(isMem(mi, opNo), "Invalid memory reference!");

    MachineOperand segment = mi.getOperand(opNo + 4);
    if (segment.getReg() != 0) {
      printOperand(mi, opNo + 4, modifier);
      os.print(':');
    }
    printLeaMemReference(mi, opNo, modifier);
  }

  private void printLeaMemReference(MachineInstr mi,
                                    int opNo,
                                    String modifier) {
    MachineOperand baseReg = mi.getOperand(opNo);
    MachineOperand indexReg = mi.getOperand(opNo + 2);
    MachineOperand disp = mi.getOperand(opNo + 3);

    // If we really don't want to print out (rip), don't.
    boolean hasBaseReg = baseReg.getReg() != 0;
    if (hasBaseReg && modifier != null && modifier.equals("no-rip") &&
        baseReg.getReg() == X86GenRegisterNames.RIP) {
      hasBaseReg = false;
    }

    // HasParenPart - True if we will print out the () part of the mem ref.
    boolean hasParenPart = indexReg.getReg() != 0 || hasBaseReg;

    if (disp.isImm()) {
      long dispVal = disp.getImm();
      if (dispVal != 0 || !hasParenPart)
        os.print(dispVal);
    } else {
      Util.assertion(disp.isGlobalAddress() ||
          disp.isConstantPoolIndex() ||
          disp.isJumpTableIndex() ||
          disp.isExternalSymbol());
      printSymbolOperand(mi.getOperand(opNo + 3));
    }

    if (hasParenPart) {
      Util.assertion(indexReg.getReg() != X86GenRegisterNames.ESP,
          "X86 doesn't allow scaling by ESP");
      os.print('(');
      if (hasBaseReg)
        printOperand(mi, opNo, modifier);

      if (indexReg.getReg() != 0) {
        os.print(',');
        printOperand(mi, opNo + 2, modifier);
        long scaleVal = mi.getOperand(opNo + 2).getImm();
        if (scaleVal != 1)
          os.printf(",%d", scaleVal);
      }
      os.print(')');
    }
  }

  public static X86AsmPrinter createX86AsmCodeEmitter(
      OutputStream os,
      X86TargetMachine tm,
      MCSymbol.MCContext ctx,
      MCStreamer streamer,
      MCAsmInfo mai) {
    return new X86AsmPrinter(os, tm, ctx, streamer, mai);
  }
}
