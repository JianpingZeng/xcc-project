package backend.codegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng
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

import backend.analysis.MachineLoop;
import backend.analysis.MachineLoopInfo;
import backend.mc.*;
import backend.mc.MCAsmInfo.MCSymbolAttr;
import backend.pass.AnalysisUsage;
import backend.support.LLVMContext;
import backend.support.MachineFunctionPass;
import backend.support.NameMangler;
import backend.target.*;
import backend.type.Type;
import backend.value.*;
import backend.value.Value.UndefValue;
import gnu.trove.list.array.TIntArrayList;

import tools.TextUtils;
import tools.Util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import static backend.support.AssemblyWriter.writeAsOperand;
import static backend.target.TargetMachine.RelocModel.Static;

/**
 * This is a common base class be used for target-specific asmwriters. This
 * class just primarily tack care of printing global constants, which is printed
 * in a very similar way across different target.
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public abstract class AsmPrinter extends MachineFunctionPass {
  /**
   * The current section asmName where we are emitting to.
   */
  private String currentSection;

  private Section currentSection_;

  private boolean isInTextSection;
  /**
   * This provides a unique ID for the compiling machine function in the
   * current translation unit.
   * <p>
   * It is automatically increased by calling method {}
   */
  private int functionNumber;

  /**
   * The output stream on which the assembly code will be emitted.
   */
  protected PrintStream os;
  /**
   * The target machine text.
   */
  public TargetMachine tm;

  public MCAsmInfo mai;

  protected TargetRegisterInfo tri;

  protected boolean verboseAsm;

  protected MachineLoop li;
  /**
   * A asmName mangler for performing necessary mangling on global linkage entity.
   */
  protected NameMangler mangler;
  /**
   * The asmName of current being processed machine function.
   */
  protected MCSymbol curFuncSym;

  protected MCSymbol.MCContext outContext;

  protected MCStreamer outStreamer;
  private TargetSubtarget subtarget;
  public MachineFunction mf;

  protected AsmPrinter(OutputStream os, TargetMachine tm,
                       MCSymbol.MCContext ctx,
                       MCStreamer streamer, MCAsmInfo mai) {
    functionNumber = 0;
    this.os = new PrintStream(os);
    this.tm = tm;
    this.mai = mai;
    this.tri = tm.getRegisterInfo();
    subtarget = tm.getSubtarget();

    outContext = ctx;
    outStreamer = streamer;
    verboseAsm = streamer.isVerboseAsm();
  }

  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    if (verboseAsm)
      au.addRequired(MachineLoop.class);
    super.getAnalysisUsage(au);
  }

  public void emitStartOfAsmFile(Module module) {}

  public void emitEndOfAsmFile(Module module) {}

  @Override
  public boolean doInitialization(Module m) {
    mangler = new NameMangler(m, mai.getGlobalPrefix(),
        mai.getPrivateGlobalPrefix(),
        mai.getLinkerPrivateGlobalPrefix());

    emitStartOfAsmFile(m);
    if (mai.hasSingleParameterDotFile()) {
      // .file "foo.c"
      outStreamer.emitFileDirective(m.getModuleIdentifier());
    }
    return false;
  }

  @Override
  public boolean doFinalization(Module m) {
    // Emit global variables.
    for (GlobalVariable gv : m.getGlobalVariableList()) {
      emitGlobalVariable(gv);
    }

    // TODO Emit debug information

    // If the target wants to know about weak references, print them all.
    if (mai.getWeakDefDirective() != null) {
      // Print out module-level global variables here.
      for (GlobalVariable gv : m.getGlobalVariableList()) {
        if (gv.hasExternalLinkage())
          outStreamer.emitSymbolAttribute(getGlobalValueSymbol(gv),
              MCSymbolAttr.MCSA_WeakReference);
      }

      for (Function fn : m.getFunctionList()) {
        if (fn.hasExternalWeakLinkage())
          outStreamer.emitSymbolAttribute(getGlobalValueSymbol(fn),
              MCSymbolAttr.MCSA_WeakReference);
      }
    }
    if (mai.hasSetDirective()) {
      outStreamer.addBlankLine();
      // TODO, handle alias
    }

    emitEndOfAsmFile(m);
    outStreamer.finish();
    return false;
  }

  public void emitGlobalVariable(GlobalVariable gv) {
    TargetData td = tm.getTargetData();

    // External global require no code
    if (!gv.hasInitializer())
      return;

    // Check to see if this is a special global used by LLVM, if so, emit it.
    if (emitSpecialLLVMGlobal(gv))
      return;
    MCSymbol gsym = getGlobalValueSymbol(gv);
    emitVisibility(gsym, gv.getVisibility());

    if (mai.hasDotTypeDotSizeDirective())
      outStreamer.emitSymbolAttribute(gsym, MCSymbolAttr.MCSA_ELF_TypeObject);;

    SectionKind kind = TargetLoweringObjectFile.getKindForGlobal(gv, tm);
    long size = td.getTypeAllocSize(gv.getType());
    int alignLog = Util.log2(td.getPrefTypeAlignment(gv.getType()));

    // Handle common and BSS local symbols (.lcomm).
    if (kind.isCommon() || kind.isBSSLocal()) {
      if (size == 0) size = 1;

      if (verboseAsm) {
        writeAsOperand(outStreamer.getCommentOS(),
            gv, false, gv.getParent());
        outStreamer.getCommentOS().println();
      }

      // Handle common symbols.
      if (kind.isCommon()) {
        // .comm _foo, 42, 4
        outStreamer.emitCommonSymbol(curFuncSym, size, 1 << alignLog);;
        return;
      }

      // Handle local BSS symbols.
      if (mai.hasMachoZeroFillDirective()) {
        MCSection theSection =
            getObjFileLowering().sectionForGlobal(gv, kind, mangler, tm);
        // .zerofill __DATA, __bss, _foo, 400, 5
        outStreamer.emitZeroFill(theSection, gsym, (int) size, 1 << alignLog);;
        return;
      }

      if (mai.hasLCOMMDirective()) {
        // .lcomm _foo, 42
        outStreamer.emitLocalCommonSymbol(gsym, size);
        return;
      }

      // .local _foo
      outStreamer.emitSymbolAttribute(gsym, MCSymbolAttr.MCSA_Local);;
      // .comm _foo, 42, 4
      outStreamer.emitCommonSymbol(gsym, size, 1 << alignLog);;
      return;
    }

    MCSection theSection = getObjFileLowering().sectionForGlobal(gv, kind, mangler, tm);

    // Handle the zerofill directive on darwin, which is a special form of BSS
    // emission.
    if (kind.isBSSExtern() && mai.hasMachoZeroFillDirective()) {
      // .globl _foo
      outStreamer.emitSymbolAttribute(gsym, MCSymbolAttr.MCSA_Global);;
      // .zerofill __DATA, __common, _foo, 400, 5
      outStreamer.emitZeroFill(theSection, gsym, (int) size, 1<<alignLog);;
      return;
    }

    outStreamer.switchSection(theSection);
    emitLinkage(gv.getLinkage(), gsym);
    emitAlignment(alignLog, gv);

    if (verboseAsm) {
      writeAsOperand(outStreamer.getCommentOS(),
          gv, false, gv.getParent());
      outStreamer.getCommentOS().println();
    }

    outStreamer.emitLabel(gsym);
    emitGlobalConstant(gv.getInitializer(), 0);
    if (mai.hasDotTypeDotSizeDirective()) {
      // .size foo, 42
      outStreamer.emitELFSize(gsym, MCConstantExpr.create(size, outContext));;
    }
    outStreamer.addBlankLine();
  }

  public static boolean isScale(MachineOperand mo) {
    long imm;
    return mo.isImm() && (
        ((imm = mo.getImm()) & (imm - 1)) == 0)
        && imm >= 1 && imm <= 8;
  }

  /**
   * Memory operand is like this: baseReg + scale*indexReg+ disp.
   *
   * @param mi
   * @param opNo
   * @return
   */
  public static boolean isMem(MachineInstr mi, int opNo) {
    if (mi.getOperand(opNo).isFrameIndex()) return true;
    return mi.getNumOperands() >= opNo + 4 &&
        mi.getOperand(opNo).isRegister()
        && isScale(mi.getOperand(opNo + 1))
        && mi.getOperand(opNo + 2).isRegister()
        && (mi.getOperand(opNo + 3).isImm()
        || mi.getOperand(opNo + 3).isGlobalAddress()
        || mi.getOperand(opNo + 3).isConstantPoolIndex());
  }

  @Override
  public boolean runOnMachineFunction(MachineFunction mf) {
    setupMachineFunction(mf);
    emitFunctionHeader();
    emitFunctionBody();
    return false;
  }

  /**
   * this method emits the header of current function, such as
   * constant pool etc.
   */
  public void emitFunctionHeader() {
    emitConstantPool();

    // print the header of function.
    Function f = mf.getFunction();
    outStreamer.switchSection(getObjFileLowering().sectionForGlobal(f, mangler, tm));;
    emitVisibility(curFuncSym, f.getVisibility());
    emitLinkage(f.getLinkage(), curFuncSym);
    emitAlignment(mf.getAlignment(), f);

    if (mai.hasDotTypeDotSizeDirective())
      outStreamer.emitSymbolAttribute(curFuncSym, MCSymbolAttr.MCSA_ELF_TypeFunction);;

    if (verboseAsm) {
      writeAsOperand(outStreamer.getCommentOS(),
          f, false, f.getParent());
      outStreamer.getCommentOS().println();
    }

    // Emit the CurrentFnSym.  This is a virtual function to allow targets to
    // do their wild and crazy things as required.
    emitFunctionEntryLabel();

    // TODO add some workaround for linkonce linkage on Cygwin/Mingw.
    // TODO emit pre-function debug eh information.
  }

  /**
   * Emit the label that is the entrypoint for the
   * function. This can be overrided by targets as
   * required to do other stuff.
   */
  protected void emitFunctionEntryLabel() {
    outStreamer.emitLabel(curFuncSym);
  }

  protected void emitVisibility(MCSymbol sym, GlobalValue.VisibilityTypes visibility) {
    MCSymbolAttr attr = MCSymbolAttr.MCSA_Invalid;

    switch (visibility) {
      default: break;
      case HiddenVisibility:
        attr = mai.getHiddenVisibilityAttr();
        break;
      case ProtectedVisibility:
        attr = mai.getProtectedVisibilityAttr();
        break;
    }

    if (attr != MCSymbolAttr.MCSA_Invalid)
      outStreamer.emitSymbolAttribute(sym, attr);
  }

  protected void emitLinkage(GlobalValue.LinkageType linkage,
                             MCSymbol sym) {
    switch (linkage) {
      case CommonLinkage:
      case LinkerPrivateLinkage:
        if (mai.getWeakDefDirective() != null) {
          // .globl _foo
          outStreamer.emitSymbolAttribute(sym, MCSymbolAttr.MCSA_Global);;
          // .weak_definition _foo
          outStreamer.emitSymbolAttribute(sym, MCSymbolAttr.MCSA_WeakDefinition);;
        }
        else if (mai.getLinkOnceDirective() != null) {
          String linkOnce = mai.getLinkOnceDirective();
          // .globl _foo
          outStreamer.emitSymbolAttribute(sym, MCSymbolAttr.MCSA_Global);;
          os.print(linkOnce);
        }
        else {
          // .weak _foo
          outStreamer.emitSymbolAttribute(sym, MCSymbolAttr.MCSA_Weak);;
        }
        break;
      case ExternalLinkage:
        // if the external or appending declare as a global symbol,.
        // .globl _foo
        outStreamer.emitSymbolAttribute(sym, MCSymbolAttr.MCSA_Global);;
        break;
      case PrivateLinkage:
      case InternalLinkage:
        break;
      default:
        Util.shouldNotReachHere("Unknown linkage type!");
    }
  }

  /**
   * This method emits the body and trailer for a
   * function.
   */
  public void emitFunctionBody() {
    // Emit target-specific gunk before the function body.
    emitFunctionBodyStart();

    // Print out code for the function.
    boolean hasAnyRealCode = false;
    for (MachineBasicBlock mbb : mf.getBasicBlocks()) {
      // print a label for the machine block.
      emitBasicBlockStart(mbb);
      for (MachineInstr mi : mbb.getInsts()) {
        // print the assembly code for the machine instr.
        if (!mi.isLabel())
          hasAnyRealCode = true;

        // TODO processDebugLoc();
        if (verboseAsm)
          emitComments(mi, outStreamer.getCommentOS());;

          switch (mi.getOpcode()) {
            case TargetInstrInfo.DBG_LABEL:
            case TargetInstrInfo.EH_LABEL:
            case TargetInstrInfo.GC_LABEL:
              printLabel(mi);
              break;
            case TargetInstrInfo.INLINEASM:
              printInlineAsm(mi);
              break;
            case TargetInstrInfo.IMPLICIT_DEF:
              printImplicitDef(mi);
              break;
            case TargetInstrInfo.DECLARE:
              printDeclare(mi);
              break;
            default:
              emitInstruction(mi);
              break;
          }

          // TODO print debug information.
      }
    }

    // If the function is empty and the object file uses .subsections_via_symbols,
    // then we need to emit *something* to the function body to prevent the
    // labels from collapsing together.  Just emit a 0 byte
    if (mai.hasSubsectionsViaSymbols() && !hasAnyRealCode)
      outStreamer.emitIntValue(0, 1, 0);

    // Emit target-specific gunk after the function body.
    emitFunctionBodyEnd();

    if (mai.hasDotTypeDotSizeDirective()) {
      os.printf("\t.size\t");
      curFuncSym.print(os);
      os.print(", .-");
      curFuncSym.print(os);
      os.println();
    }

    // Emit post-function debug information.
    // TODO if (mai.doesSupportDebugInformation() || mai.doesSupportExceptionHandling())

    // Print out jump tables referenced by the function.
    emitJumpTableInfo();
    outStreamer.addBlankLine();
  }

  protected abstract void emitInstruction(MachineInstr mi);

  private void emitJumpTableInfo() {
    Util.shouldNotReachHere();
  }

  private void emitFunctionBodyEnd() { }
  private void emitBasicBlockStart(MachineBasicBlock mbb) { }

  protected void emitFunctionBodyStart() {}

  /**
   * This should be called when a new machine function is being processed when
   * running method runOnMachineFunction();
   */
  protected void setupMachineFunction(MachineFunction mf) {
    this.mf = mf;
    curFuncSym = getGlobalValueSymbol(mf.getFunction());

    if (verboseAsm)
      li = (MachineLoop) getAnalysisToUpDate(MachineLoop.class);
  }

  private class SectionCPs {
    MCSection sec;
    int alignment;
    TIntArrayList cpes;
    SectionCPs(MCSection s, int align) {
      sec = s;
      alignment = align;
      cpes = new TIntArrayList();
    }
  }

  protected int getFunctionNumber() {
    return mf.getFunctionNumber();
  }

  /**
   * Print to the current output stream assembly
   * representations of the constants in the constant pool MCP. This is
   * used to print out constants which have been "spilled to memory" by
   * the code generator.
   */
  protected void emitConstantPool() {
    MachineConstantPool mcp = mf.getConstantPool();
    ArrayList<MachineConstantPoolEntry> consts = mcp.getConstants();
    if (consts.isEmpty()) return;

    ArrayList<SectionCPs> cpSections = new ArrayList<>();
    for (int i = 0, e = consts.size(); i < e; i++) {
      MachineConstantPoolEntry entry = consts.get(i);
      int align = entry.getAlignment();

      SectionKind kind = null;
      switch (entry.getRelocationInfo()) {
        default:
          Util.shouldNotReachHere("Unknown section kind");
          break;
        case 2: kind = SectionKind.getReadOnlyWithRel(); break;
        case 1: kind = SectionKind.getReadOnlyWithRelLocal(); break;
        case 0: {
          switch ((int)tm.getTargetData().getTypeAllocSize(entry.getType())) {
            case 4:
              kind = SectionKind.getMergeableConst4(); break;
            case 8:
              kind = SectionKind.getMergeableConst8(); break;
            case 16:
              kind = SectionKind.getMergeableConst16(); break;
            default:
              kind = SectionKind.getMergeableConst(); break;
          }
        }
      }

      MCSection s = getObjFileLowering().getSectionForConstant(kind);

      int secIdx = cpSections.size();
      cpSections.add(new SectionCPs(s, align));
      if (align > cpSections.get(secIdx).alignment)
        cpSections.get(secIdx).alignment = align;

      cpSections.get(secIdx).cpes.add(i);
    }

    // Now print stuff into the calculated sections.
    for (int i = 0, e = cpSections.size(); i < e; i++) {
      outStreamer.switchSection(cpSections.get(i).sec);
      emitAlignment(Util.log2(cpSections.get(i).alignment));

      int offset = 0;
      for (int j = 0, ee = cpSections.get(i).cpes.size(); j < ee; j++) {
        int cpi = cpSections.get(i).cpes.get(j);
        MachineConstantPoolEntry cpe = consts.get(cpi);

        // Emit inter-object padding for alignment.
        int alignMask = cpe.getAlignment();
        int newOffset = (offset + alignMask) & ~alignMask;
        outStreamer.emitFill(newOffset - offset, 0, 0);;

        Type ty = cpe.getType();
        offset = (int) (newOffset + tm.getTargetData().getTypeAllocSize(ty));

        // Emit the label with a comment on it.
        if (verboseAsm) {
          outStreamer.getCommentOS().print("constant pool ");
          writeTypeSymbolic(outStreamer.getCommentOS(),
              cpe.getType(), mf.getFunction().getParent());
          outStreamer.getCommentOS().println();
        }

        outStreamer.emitLabel(getCPISymbol(cpi));

        if (cpe.isMachineConstantPoolEntry())
          emitMachineConstantPoolValue(cpe.getValueAsCPV());
        else
          emitGlobalConstant(cpe.getValueAsConstant(), 0);
      }
    }
  }

  private TargetLoweringObjectFile getObjFileLowering() {
    return tm.getTargetLowering().getObjFileLowering();
  }

  private void emitMachineConstantPoolValue(MachineConstantPoolValue cpv) {
    Util.shouldNotReachHere("Target does not support EmitMachineConstantPoolValue");
  }

  public static PrintStream writeTypeSymbolic(PrintStream os, Type ty, Module m) {
    os.print(" ");
    if (m != null) {
      return os;
    } else {
      os.print(ty.getDescription());
      return os;
    }
  }

  protected void emitAlignment(int numBits) {
    emitAlignment(numBits, null);
  }

  /**
   * Emits a alignment directive to the specified power of two.
   *
   * @param numBits
   * @param gv
   */
  protected void emitAlignment(int numBits, GlobalValue gv) {
    if (gv != null && gv.getAlignment() != 0)
      numBits = Util.log2(gv.getAlignment());
    numBits = Math.max(numBits, 0);
    if (numBits == 0) return;

    if (getCurrentSection().getKind().isText())
      outStreamer.emitCodeAlignment(1<<numBits, 0);
    else
      outStreamer.emitValueToAlignment(1<<numBits, 0, 1, 0);
  }

  private MCSection getCurrentSection() {
    return outStreamer.getCurrentSection();
  }

  /**
   * Emits a block of zeros.
   *
   * @param numZeros
   */
  protected void emitZero(long numZeros) {
    if (numZeros != 0) {
      String zeroDirective = mai.getZeroDirective();
      if (zeroDirective != null)
        os.println(zeroDirective + numZeros);
      else {
        String data8BitDirective = mai.getData8bitsDirective();
        for (; numZeros != 0; numZeros--)
          os.print(data8BitDirective + "0\n");
      }
    }
  }

  /**
   * Prints a general Backend constant into .s file.
   *
   * @param c
   */
  protected void emitGlobalConstant(Constant c, int addrSpace) {
    TargetData td = tm.getTargetData();
    if (c instanceof ConstantAggregateZero || c instanceof UndefValue) {
      long size = td.getTypeAllocSize(c.getType());
      if (size == 0) size = 1;
      outStreamer.emitZeros(size, addrSpace);
      return;
    }
    else if (c instanceof ConstantInt) {
      ConstantInt ci = (ConstantInt) c;
      int size = (int) td.getTypeAllocSize(ci.getType());
      switch (size) {
        case 1:
        case 2:
        case 4:
        case 8:
          if (verboseAsm)
            outStreamer.getCommentOS().printf("0x%%%x\n", ci.getZExtValue());
          outStreamer.emitIntValue(ci.getZExtValue(), size, addrSpace);;
          return;
        default:
          emitGlobalConstantLargeInt(ci, addrSpace, this);
          return;
      }
    }
    else if (c instanceof ConstantArray) {
      ConstantArray ca = (ConstantArray) c;
      emitGlobalConstantArray(ca, addrSpace, this);
      return;
    } else if (c instanceof ConstantStruct) {
      ConstantStruct cas = (ConstantStruct) c;
      emitGlobalConstantStruct(cas, addrSpace, this);
      return;
    } else if (c instanceof ConstantFP) {
      // FP Constants are printed as integer constants to avoid losing
      // precision.
      ConstantFP fp = (ConstantFP) c;
      emitGlobalConstantFP(fp, addrSpace, this);
      return;
    }
    else if (c instanceof ConstantVector) {
      emitGlobalConstantVector((ConstantVector)c, addrSpace, this);
      return;
    }
    else if (c instanceof ConstantPointerNull) {
      int size = (int) td.getTypeAllocSize(c.getType());
      outStreamer.emitIntValue(0, size, addrSpace);
      return;
    }
    // Otherwise, it must be a ConstantExpr.  Lower it to an MCExpr, then emit it
    // thread the streamer with EmitValue.
    outStreamer.emitValue(lowerConstant(c, this),
        (int) td.getTypeStoreSize(c.getType()),
        addrSpace);
  }

  private MCExpr lowerConstant(Constant c, AsmPrinter asmPrinter) {
    return null;
  }

  private void emitGlobalConstantVector(ConstantVector c, int addrSpace, AsmPrinter asmPrinter) {

  }

  private void emitGlobalConstantFP(ConstantFP fp, int addrSpace, AsmPrinter asmPrinter) {

  }

  private void emitGlobalConstantStruct(ConstantStruct cas, int addrSpace, AsmPrinter asmPrinter) {

  }

  private void emitGlobalConstantArray(ConstantArray ca, int addrSpace, AsmPrinter asmPrinter) {

  }

  private void emitGlobalConstantLargeInt(ConstantInt ci, int addrSpace, AsmPrinter asmPrinter) {

  }

  /**
   * Print the specified array as a C compatible string, only if
   * the predicate isString is true.
   *
   * @param os
   * @param ca
   * @param lastIndex
   */
  private void printAsCString(PrintStream os, ConstantArray ca, int lastIndex) {
    Util.assertion(ca.isString(), "Array is not string!");

    os.print("\"");
    for (int i = 0; i < lastIndex; i++) {
      int c = (int) ((ConstantInt) ca.operand(i)).getZExtValue();
      if (c == '"')
        os.print("\\\"");
      else if (c == '\\')
        os.print("\\\\");
      else if (TextUtils.isPrintable(c))
        os.print((char) c);
      else {
        switch (c) {
          case '\b':
            os.print("\\b");
            break;
          case '\f':
            os.print("\\f");
            break;
          case '\n':
            os.print("\\n");
            break;
          case '\r':
            os.print("\\r");
            break;
          case '\t':
            os.print("\\t");
            break;
          default:
            os.print('\\');
            os.print(Util.toOctal(c >> 6));
            os.print(Util.toOctal(c >> 3));
            os.print(Util.toOctal(c));
            break;
        }
      }
    }
    os.print("\"");
  }

  protected void printAsmOperand(MachineInstr mi, int opNo,
                                 int asmVariant, String extra) {
    Util.shouldNotReachHere("Target can not support");
  }

  protected void printAsmMemoryOperand(MachineInstr mi, int opNo,
                                       int asmVariant, String extra) {
    Util.shouldNotReachHere("Target can not support");
  }

  private boolean shouldOmitSectionDirective(String name, MCAsmInfo tai) {
    switch (name) {
      case ".text":
      case ".data":
        return true;
      case ".bss":
        return !tai.usesELFSectionDirectiveForBSS();
    }
    return false;
  }

  public int getPreferredAlignLog(GlobalVariable gv) {
    int align = tm.getTargetData().getTypeAlign(gv.getType());
    if (gv.getAlignment() > (1 << align))
      align = Util.log2(gv.getAlignment());

    if (gv.hasInitializer()) {
      // Always round up alignment of global doubles to 8 bytes.
      if (gv.getType().getElementType() == LLVMContext.DoubleTy && align < 3)
        align = 3;
      if (align < 4) {
        // If the global is not external, see if it is large.  If so, give it a
        // larger alignment.
        if (tm.getTargetData().getTypeSize(gv.getType().getElementType()) > 128)
          align = 4;
      }
    }
    return align;
  }

  private Module getModuleFromVal(Value v) {
    if (v instanceof Argument) {
      Argument arg = (Argument) v;
      return arg.getParent() != null ? arg.getParent().getParent() : null;
    }

    if (v instanceof BasicBlock) {
      BasicBlock bb = (BasicBlock) v;
      return bb.getParent() != null ? bb.getParent().getParent() : null;
    }

    if (v instanceof Instruction) {
      Instruction inst = (Instruction) v;
      Function f = inst.getParent() != null ? inst.getParent().getParent() : null;
      return f != null ? f.getParent() : null;
    }

    if (v instanceof GlobalValue) {
      GlobalValue gv = (GlobalValue) v;
      return gv.getParent();
    }

    return null;
  }

  /**
   * Pretty-print comments for instructions
   * @param mi
   */
  public void emitComments(MachineInstr mi,
                           PrintStream cos) {
    // TODO debug information.
  }

  private static PrintStream indent(
      PrintStream os,
      int level) {
    return indent(os, level, 2);
  }

  private static PrintStream indent(
      PrintStream os,
      int level,
      int scale) {
    for (int i = level * scale; i != 0; --i)
      os.print(" ");

    return os;
  }

  private static void printChildLoopComment(
      PrintStream os,
      MachineLoopInfo loop,
      MCAsmInfo tai,
      int functionNumber) {
    // Add child loop information.
    for (MachineLoopInfo childLoop : loop.getSubLoops()) {
      MachineBasicBlock header = childLoop.getHeaderBlock();
      Util.assertion(header != null, "No header for loop");

      os.println();
      os.print(Util.fixedLengthString(tai.getCommentColumn(), ' '));

      os.print(tai.getCommentString());
      indent(os, childLoop.getLoopDepth() - 1)
          .printf(" Child Loop BB%d_%d Depth %d",
              functionNumber,
              header.getNumber(),
              childLoop.getLoopDepth());

      printChildLoopComment(os, childLoop, tai, functionNumber);
    }
  }

  public boolean emitSpecialLLVMGlobal(GlobalVariable gv) {
    if (gv.getName().equals("llvm.used")) {
      if (mai.hasNoDeadStrip())
        emitLLVMUsedList(gv.getInitializer());
      return true;
    }

    if (gv.getSection().equals("llvm.metadata"))
      return true;

    Util.assertion(gv.hasInitializer());
    TargetData td = tm.getTargetData();
    int align = Util.log2(td.getPointerPrefAlign());
    if (gv.getName().equals("llvm.global_ctors")) {
      outStreamer.switchSection(getObjFileLowering().getStaticCtorSection());
      emitAlignment(align, null);
      // TODO EmitXXStructorList(GV->getInitializer());
      if (tm.getRelocationModel() == Static &&
          mai.hasStaticCtorDtorReferenceInStaticMode()) {
        String sym = ".constructors_used";
        outStreamer.emitSymbolAttribute(outContext.getOrCreateSymbol(sym),
            MCSymbolAttr.MCSA_Reference);
      }
      return true;
    }

    if (gv.getName().equals("llvm.global_dtors")) {
      outStreamer.switchSection(getObjFileLowering().getStaticDtorSection());
      emitAlignment(align, null);
      // TODO EmitXXStructorList(GV->getInitializer());
      if (tm.getRelocationModel() == Static &&
          mai.hasStaticCtorDtorReferenceInStaticMode()) {
        String sym = ".destructors_used";
        outStreamer.emitSymbolAttribute(outContext.getOrCreateSymbol(sym),
            MCSymbolAttr.MCSA_Reference);
      }
      return true;
    }
    return false;
  }

  private void emitLLVMUsedList(Constant list) {
    Util.assertion(false, "Should not reaching here!"); // TODO: 17-8-2
  }

  /**
   * This method prints a local label used for debugging and exception handling
   * tables.
   *
   * @param id
   */
  public void printLabel(long id) {
    os.printf(mai.getPrivateGlobalPrefix());
    os.print("label");
    os.print(id);
    os.print(':');
  }

  /**
   * This method prints a local label used for debugging and exception handling
   * tables.
   *
   * @param mi
   */
  public void printLabel(MachineInstr mi) {
    printLabel(mi.getOperand(0).getImm());
  }

  /**
   * This method formats and prints the specified machine
   * instruction that is an inline asm.
   *
   * @param mi
   */
  public void printInlineAsm(MachineInstr mi) {
    Util.shouldNotReachHere("Target can not support inline asm");
  }

  /**
   * This methods prints a local variable declaration used by debug table.
   *
   * @param mi
   */
  public void printDeclare(MachineInstr mi) {
    Util.shouldNotReachHere("Exception handling not supported!");
  }

  /**
   * This method prints the specified machine instruction that is an implicit
   * def.
   *
   * @param mi
   */
  public void printImplicitDef(MachineInstr mi) {
    if (verboseAsm) {
      outStreamer.getCommentOS().print(mai.getCommentColumn());
      outStreamer.getCommentOS().printf("%s implicit-def: %s%n",
          mai.getCommentString(),
          tri.getAsmName(mi.getOperand(0).getReg()));
    }
  }

  public MCSymbol getJTISymbol(int jtiId, boolean isLinkerPrivate) {
    return mf.getJTISymbol(jtiId, outContext, isLinkerPrivate);
  }

  public MCSymbol getBlockAddressSymbol(Function f,
                                           BasicBlock bb) {
    Util.assertion(bb.hasName(), "address of anonymous basic blokck not supoorted");
    String fnName = mangler.getMangledNameWithPrefix(f, false);
    String name = mangler.getMangledNameWithPrefix(
        "BA" + fnName.length() + '_' + fnName
        + "_" + bb.getName(),
        NameMangler.ManglerPrefixTy.Private);
    return outContext.getOrCreateSymbol(name);
  }

  /**
   * Return a symbol for the specified constant pool entry.
   * @param cpiId
   * @return
   */
  public MCSymbol getCPISymbol(int cpiId) {
    String name = mai.getPrivateGlobalPrefix() +
        getFunctionNumber() + "_" + cpiId;
    return outContext.getOrCreateSymbol(name);
  }

  public MCSymbol getJTSetSymbol(int id, int mbbID) {
    String name = mai.getPrivateGlobalPrefix() +
        getFunctionNumber() + "_" + id + "_set_" +
        mbbID;
    return outContext.getOrCreateSymbol(name);
  }

  protected MCSymbol getGlobalValueSymbol(GlobalValue gv) {
    String name = mangler.getMangledNameWithPrefix(gv, false);
    return outContext.getOrCreateSymbol(name);
  }

  protected MCSymbol getSymbolWithGlobalValueBase(GlobalValue gv,
                                                  String suffix,
                                                  boolean forcePrivate) {
    String name = mangler.getMangledNameWithPrefix(gv, forcePrivate);
    name += suffix;
    return outContext.getOrCreateSymbol(name);
  }

  protected MCSymbol getExternalSymbolSymbol(String sym) {
    String name = mangler.getMangledNameWithPrefix(sym);
    return outContext.getOrCreateSymbol(name);
  }

}
