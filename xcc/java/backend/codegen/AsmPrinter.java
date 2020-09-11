package backend.codegen;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng
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
import backend.support.MachineFunctionPass;
import backend.support.NameMangler;
import backend.target.*;
import backend.type.Type;
import backend.value.*;
import backend.value.Value.UndefValue;
import gnu.trove.list.array.TIntArrayList;
import tools.*;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static backend.support.AssemblyWriter.writeAsOperand;
import static backend.target.TargetMachine.RelocModel.Static;
import static tools.Util.unEscapeLexed;

/**
 * This is a common base class be used for target-specific asmwriters. This
 * class just primarily tack care of printing global constants, which is printed
 * in a very similar way across different target.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public abstract class AsmPrinter extends MachineFunctionPass {
  /**
   * The current section asmName where we are emitting to.
   */
  private String currentSection;

  private Section currentSection_;

  private boolean isInTextSection;
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

  protected MachineLoopInfo li;
  /**
   * A asmName mangler for performing necessary mangling on global linkage entity.
   */
  public NameMangler mangler;
  /**
   * The asmName of current being processed machine function.
   */
  protected MCSymbol curFuncSym;

  protected MCSymbol.MCContext outContext;

  public MCStreamer outStreamer;
  protected TargetSubtarget subtarget;
  public MachineFunction mf;
  protected MachineModuleInfo mmi;
  private HashMap<BasicBlock, MCSymbol> addrLabelSymbols;
  private int setCounter;

  protected AsmPrinter(OutputStream os, TargetMachine tm,
                       MCSymbol.MCContext ctx,
                       MCStreamer streamer, MCAsmInfo mai) {
    this.os = new PrintStream(os);
    this.tm = tm;
    this.mai = mai;
    subtarget = tm.getSubtarget();
    this.tri = subtarget.getRegisterInfo();

    outContext = ctx;
    outStreamer = streamer;
    // FIXME, disable verbose output by default for avoiding a bug of
    // printing out a double value.
    verboseAsm = false; //streamer.isVerboseAsm();
    setCounter = 0;
  }

  public TargetData getTargetData() { return tm.getTargetData(); }

  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    if (verboseAsm)
      au.addRequired(MachineLoopInfo.class);
    super.getAnalysisUsage(au);
  }

  public void emitStartOfAsmFile(Module module) {}

  public void emitEndOfAsmFile(Module module) {}

  @Override
  public boolean doInitialization(Module m) {
    mangler = new NameMangler(outContext, m, mai.getGlobalPrefix(),
        mai.getPrivateGlobalPrefix(),
        mai.getLinkerPrivateGlobalPrefix());

    // Initialize TargetLoweringObjectFile.
    getObjFileLowering().initialize(outContext, tm);

    // Allow the target to emit any magic that it wants at the start of the file.
    emitStartOfAsmFile(m);
    // Very minimal debug info. It is ignored if we emit actual debug info. If we
    // don't, this at least helps the user find where a global came from.
    if (mai.hasSingleParameterDotFile()) {
      // .file "foo.c"
      outStreamer.emitFileDirective(m.getModuleIdentifier());
    }

    // Emit module-level inline asm if it exists.
    if (!m.getModuleInlineAsm().isEmpty()) {
      outStreamer.addComment("Start of file scope inline assembly");
      outStreamer.addBlankLine();
      emitInlineAsm(m.getModuleInlineAsm()+"\n");
      outStreamer.addComment("End of file scope inline assembly");
      outStreamer.addBlankLine();
    }
    return false;
  }

  private void emitInlineAsm(String inlineAsm) {
    outStreamer.emitRawText(unEscapeLexed(inlineAsm));
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
      for (GlobalAlias ga : m.getAliasList()) {
        MCSymbol name = mangler.getSymbol(ga);
        GlobalValue gv = ga.getAliasedGlobal();
        MCSymbol target = mangler.getSymbol(gv);
        if (ga.hasExternalLinkage() ||
                mai.getWeakRefDirective() == null ||
                mai.getWeakRefDirective().isEmpty())
          outStreamer.emitSymbolAttribute(name, MCSymbolAttr.MCSA_Global);
        else if (ga.hasWeakLinkage())
          outStreamer.emitSymbolAttribute(name, MCSymbolAttr.MCSA_WeakReference);
        else
          Util.assertion(ga.hasLocalLinkage(), "invalid alias linkage");

        emitVisibility(name, ga.getVisibility());
        outStreamer.emitAssignment(name, MCSymbolRefExpr.create(target));
      }
    }

    Function trampolineIntrinsic = m.getFunction("llvm.init.trampoline");
    if (trampolineIntrinsic == null || trampolineIntrinsic.isUseEmpty()) {
      MCSection s = mai.getNonexecutableStackSection(outContext);
      if (s != null)
        outStreamer.switchSection(s);
    }
    emitEndOfAsmFile(m);
    outStreamer.finish();
    return false;
  }

  private void emitGlobalVariable(GlobalVariable gv) {
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
      outStreamer.emitSymbolAttribute(gsym, MCSymbolAttr.MCSA_ELF_TypeObject);

    SectionKind kind = TargetLoweringObjectFile.getKindForGlobal(gv, tm);
    long size = td.getTypeAllocSize(gv.getType().getElementType());
    int alignLog = getGVAlignmentLog2(gv, td, 0);

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
        outStreamer.emitCommonSymbol(curFuncSym, size, 1 << alignLog);
        return;
      }

      // Handle local BSS symbols.
      if (mai.hasMachoZeroFillDirective()) {
        MCSection theSection =
            getObjFileLowering().sectionForGlobal(gv, kind, mangler, tm);
        // .zerofill __DATA, __bss, _foo, 400, 5
        outStreamer.emitZeroFill(theSection, gsym, (int) size, 1 << alignLog);
        return;
      }

      if (mai.hasLCOMMDirective()) {
        // .lcomm _foo, 42
        outStreamer.emitLocalCommonSymbol(gsym, size);
        return;
      }

      // .local _foo
      outStreamer.emitSymbolAttribute(gsym, MCSymbolAttr.MCSA_Local);
      // .comm _foo, 42, 4
      outStreamer.emitCommonSymbol(gsym, size, 1 << alignLog);
      return;
    }

    MCSection theSection = getObjFileLowering().sectionForGlobal(gv, kind, mangler, tm);

    // Handle the zerofill directive on darwin, which is a special form of BSS
    // emission.
    if (kind.isBSSExtern() && mai.hasMachoZeroFillDirective()) {
      // .globl _foo
      outStreamer.emitSymbolAttribute(gsym, MCSymbolAttr.MCSA_Global);
      // .zerofill __DATA, __common, _foo, 400, 5
      outStreamer.emitZeroFill(theSection, gsym, (int) size, 1 << alignLog);
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
      outStreamer.emitELFSize(gsym, MCConstantExpr.create(size, outContext));
    }
    outStreamer.addBlankLine();
  }

  private static boolean isScale(MachineOperand mo) {
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
  protected static boolean isMem(MachineInstr mi, int opNo) {
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
    outStreamer.switchSection(getObjFileLowering().sectionForGlobal(f, mangler, tm));
    emitVisibility(curFuncSym, f.getVisibility());
    emitLinkage(f.getLinkage(), curFuncSym);
    emitAlignment(mf.getAlignment(), f);

    if (mai.hasDotTypeDotSizeDirective())
      outStreamer.emitSymbolAttribute(curFuncSym, MCSymbolAttr.MCSA_ELF_TypeFunction);

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
      default:
        break;
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
      case LinkOnceAnyLinkage:
      case LinkOnceODRLinkage:
      case WeakAnyLinkage:
      case WeakODRLinkage:
      case LinkerPrivateWeakLinkage:
      case LinkerPrivateWeakDefAutoLinkage:
        if (mai.getWeakDefDirective() != null) {
          // .globl _foo
          outStreamer.emitSymbolAttribute(sym, MCSymbolAttr.MCSA_Global);
          // .weak_definition _foo
          outStreamer.emitSymbolAttribute(sym, MCSymbolAttr.MCSA_WeakDefinition);
        } else if (mai.getLinkOnceDirective() != null) {
          String linkOnce = mai.getLinkOnceDirective();
          // .globl _foo
          outStreamer.emitSymbolAttribute(sym, MCSymbolAttr.MCSA_Global);
          os.print(linkOnce);
        } else {
          // .weak _foo
          outStreamer.emitSymbolAttribute(sym, MCSymbolAttr.MCSA_Weak);
        }
        break;
      case DLLExportLinkage:
      case AppendingLinkage:
      case ExternalLinkage:
        // if the external or appending declare as a global symbol,.
        // .globl _foo
        outStreamer.emitSymbolAttribute(sym, MCSymbolAttr.MCSA_Global);
        break;
      case PrivateLinkage:
      case InternalLinkage:
      case LinkerPrivateLinkage:
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
          emitComments(mi, outStreamer.getCommentOS());

        switch (mi.getOpcode()) {
          case TargetOpcode.PROLOG_LABEL:
            printLabel(mi);
            break;
          case TargetOpcode.GC_LABEL:
          case TargetOpcode.EH_LABEL:
            outStreamer.emitLabel(mi.getOperand(0).getMCSymbol());
            break;
          case TargetOpcode.INLINEASM:
            printInlineAsm(mi);
            break;
          case TargetOpcode.IMPLICIT_DEF:
            printImplicitDef(mi);
            break;
          case TargetOpcode.KILL:
            printKill(mi);
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
      os.print("\t.size\t");
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
    MachineJumpTableInfo jumpTableInfo = mf.getJumpTableInfo();
    if (jumpTableInfo == null || jumpTableInfo.isEmpty())
      return;

    ArrayList<MachineJumpTableEntry> jumpEntries = jumpTableInfo.getJumpTables();
    if (jumpEntries == null || jumpEntries.isEmpty()) return;

    Function f = mf.getFunction();
    boolean jtInDiffSection = false;
    if (jumpTableInfo.getEntryKind() == MachineJumpTableInfo.JTEntryKind.EK_LabelDifference32
        || f.isWeakForLinker()) {
      outStreamer.switchSection(getObjFileLowering().sectionForGlobal(f, mangler, tm));
    } else {
      MCSection readOnlySection = getObjFileLowering().getSectionForConstant(SectionKind.getReadOnly());
      outStreamer.switchSection(readOnlySection);
      jtInDiffSection = true;
    }
    emitAlignment(Util.log2(jumpTableInfo.getEntrySize(tm.getTargetData())));
    for (int jti = 0, e = jumpEntries.size(); jti < e; ++jti) {
      ArrayList<MachineBasicBlock> mbbs = jumpEntries.get(jti).getMBBs();
      if (mbbs == null || mbbs.isEmpty())
        continue;

      // For the EK_LabelDifference32 entry, if the target supports .set, emit a
      // .set directive for each unique entry.  This reduces the number of
      // relocations the assembler will generate for the jump table.
      if (jumpTableInfo.getEntryKind() == MachineJumpTableInfo.JTEntryKind.EK_LabelDifference32 &&
          mai.hasSetDirective()) {
        HashSet<MachineBasicBlock> emittedSets = new HashSet<>();
        TargetLowering tli = subtarget.getTargetLowering();
        MCExpr base = tli.getPICJumpTableRelocBaseExpr(mf, jti, outContext);
        for (MachineBasicBlock mbb : mbbs) {
          if (!emittedSets.add(mbb))
            continue;

          MCExpr lhs = MCSymbolRefExpr.create(mbb.getSymbol(outContext));
          outStreamer.emitAssignment(getJTSetSymbol(jti, mbb.getNumber()),
              MCBinaryExpr.createSub(lhs, base, outContext));
        }
      }

      // On some targets (e.g. Darwin) we want to emit two consequtive labels
      // before each jump table.  The first label is never referenced, but tells
      // the assembler and linker the extents of the jump table object.  The
      // second label is actually referenced by the code.
      if (jtInDiffSection && !mai.getLinkerPrivateGlobalPrefix().isEmpty()) {
        outStreamer.emitLabel(getJTISymbol(jti, true));
      }

      for (MachineBasicBlock mbb : mbbs) {
        emitJumpTableEntry(jumpTableInfo, mbb, jti);
      }
    }
  }

  private void emitJumpTableEntry(MachineJumpTableInfo jumpTable,
                                  MachineBasicBlock mbb,
                                  int jti) {
    MCExpr value = null;
    switch (jumpTable.getEntryKind()) {
      case EK_Custom32:
        value = subtarget.getTargetLowering().lowerJumpTableEntry(jumpTable, mbb, jti, outContext);
        break;
      case EK_BlockAddress:
        // EK_BlockAddress - Each entry is a plain address of block, e.g.:
        //     .word LBB123
        value = MCSymbolRefExpr.create(mbb.getSymbol(outContext));
        break;
      case EK_GPRel32BlockAddress:
        // EK_GPRel32BlockAddress - Each entry is an address of block, encoded
        // with a relocation as gp-relative, e.g.:
        //     .gprel32 LBB123
        MCSymbol mbbSym = mbb.getSymbol(outContext);
        outStreamer.emitGPRel32Value(MCSymbolRefExpr.create(mbbSym));
        break;
      case EK_LabelDifference32:
        // EK_LabelDifference32 - Each entry is the address of the block minus
        // the address of the jump table.  This is used for PIC jump tables where
        // gprel32 is not supported.  e.g.:
        //      .word LBB123 - LJTI1_2
        // If the .set directive is supported, this is emitted as:
        //      .set L4_5_set_123, LBB123 - LJTI1_2
        //      .word L4_5_set_123
        if (mai.hasSetDirective()) {
          value = MCSymbolRefExpr.create(getJTSetSymbol(jti, mbb.getNumber()));
          break;
        }
        value = MCSymbolRefExpr.create(mbb.getSymbol(outContext));
        MCExpr jtiSym = MCSymbolRefExpr.create(getJTISymbol(jti, false));
        value = MCBinaryExpr.createSub(value, jtiSym, outContext);
        break;
      default:
        Util.shouldNotReachHere("Unknown entry kind!");
    }
    int entrySize = jumpTable.getEntrySize(tm.getTargetData());
    outStreamer.emitValue(value, entrySize, 0);
  }

  protected void emitFunctionBodyEnd() {
  }

  private MCSymbol getAddrLabelSymbol(BasicBlock bb) {
    if (addrLabelSymbols == null)
      addrLabelSymbols = new HashMap<>();

    if (addrLabelSymbols.containsKey(bb))
      return addrLabelSymbols.get(bb);

    MCSymbol sym = outContext.createTemporarySymbol();
    addrLabelSymbols.put(bb, sym);
    return sym;
  }

  /**
   * This method prints the label for the specified basic block.
   * An alignment and a comment describing it if appropriate.
   *
   * @param mbb
   */
  private void emitBasicBlockStart(MachineBasicBlock mbb) {
    int align = mbb.getAlignment();
    if (align != 0)
      emitAlignment(Util.log2(align));

    // If the block has its address taken, emit any labels that were used to
    // reference the block.  It is possible that there is more than one label
    // here, because multiple LLVM BB's may have been RAUW'd to this block after
    // the references were generated.
    if (mbb.hasAddressTaken()) {
      BasicBlock bb = mbb.getBasicBlock();
      if (verboseAsm) {
        outStreamer.addComment("Block address taken");
      }
      outStreamer.emitLabel(getAddrLabelSymbol(bb));
    }
    // Print the main label for the block.
    if (mbb.getNumPredecessors() == 0/* || isBlockOnlyReachableByFallThrough(mbb)*/) {
      if (verboseAsm && outStreamer.hasRawTextSupport()) {
        BasicBlock bb = mbb.getBasicBlock();
        if (bb != null && bb.hasName()) {
          outStreamer.addComment("%" + bb.getName());
        }

        emitBasicBlockLoopComments(mbb, li, this);
        // NOTE: Want this comment at start of line, don't emit with AddComment.
        outStreamer.emitRawText(mai.getCommentString() + "BB#" + mbb.getNumber() + ":");
      }
    } else {
      if (verboseAsm) {
        BasicBlock bb = mbb.getBasicBlock();
        if (bb != null && bb.hasName()) {
          outStreamer.addComment("%" + bb.getName());
        }
        emitBasicBlockLoopComments(mbb, li, this);
      }
      outStreamer.emitLabel(mbb.getSymbol(outContext));
    }
  }

  /**
   * Return true if the specified mbb has exactly one predecessor and is reached
   * from it's single predecessor by fall through.
   *
   * @param mbb
   * @return
   */
  private boolean isBlockOnlyReachableByFallThrough(MachineBasicBlock mbb) {
    if (mbb.isLandingPad() || mbb.isPredEmpty())
      return false;

    if (mbb.getNumPredecessors() != 1)
      return false;

    MachineBasicBlock pred = mbb.predAt(0);
    if (!pred.isLayoutSuccessor(mbb))
      return false;

    // If the only predecessor is empty, it definitely satisfy the condition.
    if (mbb.predAt(0).isEmpty())
      return true;

    return !pred.getLastInst().getDesc().isBarrier();
  }

  private static void emitBasicBlockLoopComments(MachineBasicBlock mbb,
                                                 MachineLoopInfo li,
                                                 AsmPrinter printer) {
    MachineLoop loop = li.getLoopFor(mbb);
    if (loop == null) return;

    MachineBasicBlock header = loop.getHeaderBlock();
    Util.assertion(header != null, "no header for loop");

    // If this block is not a loop header, just print out what is the loop header
    // and return.
    if (header != mbb) {
      printer.outStreamer.addComment(" in Loop: Header=BB" +
          printer.getFunctionNumber() + "_" +
          header.getNumber() + " Depth=" + loop.getLoopDepth());
      return;
    }

    // Otherwise, it is a loop header.  Print out information about child and
    // parent loops.
    PrintStream cos = printer.outStreamer.getCommentOS();
    printParentLoopComment(cos, loop.getParentLoop(), printer.getFunctionNumber());

    cos.print("=>");
    cos.print(Util.fixedLengthString(loop.getLoopDepth() * 2 - 2, ' '));

    cos.print("This ");
    if (loop.isEmpty())
      cos.print("Inner ");
    cos.printf("Loop Header: Depth=%d\n", loop.getLoopDepth());
    printChildLoopComment(cos, loop, printer.mai, printer.getFunctionNumber());
  }

  /**
   * Print comments about parent loops of this one.
   *
   * @param os
   * @param loop
   * @param functionNumber
   */
  private static void printParentLoopComment(PrintStream os,
                                             MachineLoop loop,
                                             int functionNumber) {
    if (loop == null) return;
    printParentLoopComment(os, loop.getParentLoop(), functionNumber);

    os.print(Util.fixedLengthString(loop.getLoopDepth() * 2 - 2, ' '));
    os.printf("Parent Loop BB%d_%d Depth=%d\n",
        functionNumber,
        loop.getHeaderBlock().getNumber(),
        loop.getLoopDepth());
  }

  protected void emitFunctionBodyStart() {}

  /**
   * This should be called when a new machine function is being processed when
   * running method runOnMachineFunction();
   */
  protected void setupMachineFunction(MachineFunction mf) {
    this.mf = mf;
    curFuncSym = getGlobalValueSymbol(mf.getFunction());

    if (verboseAsm)
      li = (MachineLoopInfo) getAnalysisToUpDate(MachineLoopInfo.class);
  }

  public void emitLabelDifference(MCSymbol labelHI, MCSymbol labelLO, int size) {
    MCExpr diff = MCBinaryExpr.createSub(MCSymbolRefExpr.create(labelHI),
            MCSymbolRefExpr.create(labelLO), outContext);
    if (!mai.hasSetDirective()) {
      outStreamer.emitValue(diff, size, 0);
      return;
    }

    // otherwise, emit with .set
    MCSymbol setLabel = getTempSymbol("set", setCounter++);
    outStreamer.emitAssignment(setLabel, diff);
    outStreamer.emitSymbolValue(setLabel, size, 0);
  }

  public MCSymbol getTempSymbol(String name, int id) {
    return outContext.getOrCreateSymbol(mai.getPrivateGlobalPrefix() + name + id);
  }

  public void emitInt8(int value) {
    outStreamer.emitIntValue(value, 1, 0);
  }

  public void emitInt16(int value) {
    outStreamer.emitIntValue(value, 2, 0);
  }

  public void emitInt32(int value) {
    outStreamer.emitIntValue(value, 4, 0);
  }

  public int getISAEncoding() { return 0; }

  /**
   * Get location information encoded by DBG_VALUE operands.
   * @param mi
   * @return
   */
  public MachineLocation getDebugValueLocation(MachineInstr mi) {
    return new MachineLocation();
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
        case 2:
          kind = SectionKind.getReadOnlyWithRel();
          break;
        case 1:
          kind = SectionKind.getReadOnlyWithRelLocal();
          break;
        case 0: {
          switch ((int) tm.getTargetData().getTypeAllocSize(entry.getType())) {
            case 4:
              kind = SectionKind.getMergeableConst4();
              break;
            case 8:
              kind = SectionKind.getMergeableConst8();
              break;
            case 16:
              kind = SectionKind.getMergeableConst16();
              break;
            default:
              kind = SectionKind.getMergeableConst();
              break;
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
        outStreamer.emitFill(newOffset - offset, 0, 0);

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

  protected TargetLoweringObjectFile getObjFileLowering() {
    return subtarget.getTargetLowering().getObjFileLowering();
  }

  protected void emitMachineConstantPoolValue(MachineConstantPoolValue cpv) {
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
   * Return the alignment to use for the specified global value in log2 form.
   *
   * @param gv
   * @param td
   * @param inBits
   * @return
   */
  protected static int getGVAlignmentLog2(GlobalValue gv,
                                        TargetData td,
                                        int inBits) {
    int numBits = 0;
    if (gv instanceof GlobalVariable)
      numBits = td.getPreferredAlignmentLog((GlobalVariable) gv);

    // If InBits is specified, round it to it.
    if (inBits > numBits)
      numBits = inBits;
    if (gv.getAlignment() == 0)
      return numBits;

    int gvAlign = Util.log2(gv.getAlignment());
    if (gvAlign > numBits || gv.hasSection())
      numBits = gvAlign;
    return numBits;
  }

  /**
   * Emits a alignment directive to the specified power of two.
   *
   * @param numBits
   * @param gv
   */
  protected void emitAlignment(int numBits, GlobalValue gv) {
    if (gv != null)
      numBits = getGVAlignmentLog2(gv, tm.getTargetData(), numBits);

    if (numBits == 0) return;

    if (getCurrentSection().getKind().isText())
      outStreamer.emitCodeAlignment(1 << numBits, 0);
    else
      outStreamer.emitValueToAlignment(1 << numBits, 0, 1, 0);
  }

  protected MCSection getCurrentSection() {
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
    } else if (c instanceof ConstantInt) {
      ConstantInt ci = (ConstantInt) c;
      int size = (int) td.getTypeAllocSize(ci.getType());
      switch (size) {
        case 1:
        case 2:
        case 4:
        case 8:
          if (verboseAsm)
            outStreamer.getCommentOS().printf("0x%%%x\n", ci.getZExtValue());
          outStreamer.emitIntValue(ci.getZExtValue(), size, addrSpace);
          return;
        default:
          emitGlobalConstantLargeInt(ci, addrSpace);
          return;
      }
    } else if (c instanceof ConstantArray) {
      ConstantArray ca = (ConstantArray) c;
      emitGlobalConstantArray(ca, addrSpace);
      return;
    } else if (c instanceof ConstantStruct) {
      ConstantStruct cas = (ConstantStruct) c;
      emitGlobalConstantStruct(cas, addrSpace);
      return;
    } else if (c instanceof ConstantFP) {
      // FP Constants are printed as integer constants to avoid losing
      // precision.
      ConstantFP fp = (ConstantFP) c;
      emitGlobalConstantFP(fp, addrSpace);
      return;
    } else if (c instanceof ConstantVector) {
      emitGlobalConstantVector((ConstantVector) c, addrSpace);
      return;
    } else if (c instanceof ConstantPointerNull) {
      int size = (int) td.getTypeAllocSize(c.getType());
      outStreamer.emitIntValue(0, size, addrSpace);
      return;
    }
    // Otherwise, it must be a ConstantExpr.  Lower it to an MCExpr, then emit it
    // thread the streamer with EmitValue.
    outStreamer.emitValue(lowerConstant(c), (int) td.getTypeStoreSize(c.getType()), addrSpace);
  }

  /**
   * Lower the specified LLVM constant into the MCExpr.
   *
   * @param c
   * @return
   */
  private MCExpr lowerConstant(Constant c) {
    MCSymbol.MCContext ctx = outContext;
    if (c.isNullValue() || c instanceof UndefValue)
      return MCConstantExpr.create(0, ctx);

    if (c instanceof ConstantInt)
      return MCConstantExpr.create(((ConstantInt) c).getZExtValue(), ctx);

    if (c instanceof GlobalValue) {
      return MCSymbolRefExpr.create(getGlobalValueSymbol((GlobalValue) c));
    }

    if (c instanceof BlockAddress)
      return MCSymbolRefExpr.create(getBlockAddressSymbol((BlockAddress) c));

    if (!(c instanceof ConstantExpr)) {
      Util.shouldNotReachHere("unknown constant value to lower!");
      return MCConstantExpr.create(0, ctx);
    }

    ConstantExpr ce = (ConstantExpr) c;
    switch (ce.getOpcode()) {
      case GetElementPtr: {
        TargetData td = tm.getTargetData();
        // Generate a symbolic expression for the byte address
        Constant ptrVal = ce.operand(0);
        ArrayList<Value> idxVec = new ArrayList<>();
        for (int i = 1, e = ce.getNumOfOperands(); i < e; i++)
          idxVec.add(ce.operand(i));

        long offset = td.getIndexedOffset(ptrVal.getType(), idxVec);
        MCExpr base = lowerConstant(ce.operand(0));
        if (offset == 0)
          return base;

        // Truncate/sext the offset to the pointer size.
        if (td.getPointerSizeInBits() != 64) {
          int SExtAmount = 64 - td.getPointerSizeInBits();
          offset = (offset << SExtAmount) >> SExtAmount;
        }

        return MCBinaryExpr.createAdd(base, MCConstantExpr.create(offset, ctx),
            ctx);
      }

      case Trunc:
        // We emit the value and depend on the assembler to truncate the generated
        // expression properly.  This is important for differences between
        // blockaddress labels.  Since the two labels are in the same function, it
        // is reasonable to treat their delta as a 32-bit value.
        // FALL THROUGH.
      case BitCast:
        return lowerConstant(ce.operand(0));

      case IntToPtr: {
        TargetData td = tm.getTargetData();
        // Handle casts to pointers by changing them into casts to the appropriate
        // integer type.  This promotes constant folding and simplifies this code.
        Constant op = ce.operand(0);
        op = ConstantExpr.getIntegerCast(op, td.getIntPtrType(c.getContext()), false/*ZExt*/);
        return lowerConstant(op);
      }

      case PtrToInt: {
        TargetData td = tm.getTargetData();
        // Support only foldable casts to/from pointers that can be eliminated by
        // changing the pointer to the appropriately sized integer type.
        Constant op = ce.operand(0);
        Type Ty = ce.getType();

        MCExpr opExpr = lowerConstant(op);

        // We can emit the pointer value into this slot if the slot is an
        // integer slot equal to the size of the pointer.
        if (td.getTypeAllocSize(Ty) == td.getTypeAllocSize(op.getType()))
          return opExpr;

        // Otherwise the pointer is smaller than the resultant integer, mask off
        // the high bits so we are sure to get a proper truncation if the input is
        // a constant expr.
        long inBits = td.getTypeAllocSizeInBits(op.getType());
        MCExpr maskExpr = MCConstantExpr.create(~0L >> (64 - inBits), ctx);
        return MCBinaryExpr.createAnd(opExpr, maskExpr, ctx);
      }

      // The MC library also has a right-shift operator, but it isn't consistently
      // signed or int between different targets.
      case Add:
      case Sub:
      case Mul:
      case SDiv:
      case SRem:
      case Shl:
      case And:
      case Or:
      case Xor: {
        MCExpr lhs = lowerConstant(ce.operand(0));
        MCExpr rhs = lowerConstant(ce.operand(1));
        switch (ce.getOpcode()) {
          default:
            Util.assertion("Unknown binary operator constant cast expr");
          case Add:
            return MCBinaryExpr.createAdd(lhs, rhs, ctx);
          case Sub:
            return MCBinaryExpr.createSub(lhs, rhs, ctx);
          case Mul:
            return MCBinaryExpr.createMul(lhs, rhs, ctx);
          case SDiv:
            return MCBinaryExpr.createDiv(lhs, rhs, ctx);
          case SRem:
            return MCBinaryExpr.createMod(lhs, rhs, ctx);
          case Shl:
            return MCBinaryExpr.createShl(lhs, rhs, ctx);
          case And:
            return MCBinaryExpr.createAnd(lhs, rhs, ctx);
          case Or:
            return MCBinaryExpr.createOr(lhs, rhs, ctx);
          case Xor:
            return MCBinaryExpr.createXor(lhs, rhs, ctx);
        }
      }
    }
    return null;
  }

  private void emitGlobalConstantVector(ConstantVector c, int addrSpace) {
    for (int i = 0, e = (int) c.getType().getNumElements(); i < e; i++)
      emitGlobalConstant(c.operand(i), addrSpace);
  }

  private void emitGlobalConstantFP(ConstantFP fp, int addrSpace) {
    // FP constants are printed as integer constants to avoid losing precisions.
    if (fp.getType().isDoubleTy()) {
      if (verboseAsm) {
        double val = fp.getValueAPF().convertToDouble();
        outStreamer.getCommentOS().printf("double %e\n", val);
      }

      long val = fp.getValueAPF().bitcastToAPInt().getZExtValue();
      outStreamer.emitIntValue(val, 8, addrSpace);
      return;
    }

    if (fp.getType().isFloatTy()) {
      if (verboseAsm) {
        float val = fp.getValueAPF().convertToFloat();
        outStreamer.getCommentOS().printf("float %e\n", val);
      }

      long val = fp.getValueAPF().bitcastToAPInt().getZExtValue();
      outStreamer.emitIntValue(val, 4, addrSpace);
      return;
    }

    if (fp.getType().isX86_FP80Ty()) {
      APInt api = fp.getValueAPF().bitcastToAPInt();
      long[] rawData = api.getRawData();
      if (verboseAsm) {
        // Convert to double so we can print the approximate val as a comment.
        APFloat doubleVal = fp.getValueAPF();
        OutRef<Boolean> ignored = new OutRef<>(false);
        doubleVal.convert(APFloat.IEEEdouble, APFloat.RoundingMode.rmNearestTiesToEven,
            ignored);
        outStreamer.getCommentOS().printf("x86_fp80 ~=%f\n", doubleVal.convertToDouble());
      }

      if (tm.getTargetData().isBigEndidan()) {
        outStreamer.emitIntValue(rawData[1], 2, addrSpace);
        outStreamer.emitIntValue(rawData[0], 8, addrSpace);
      } else {
        outStreamer.emitIntValue(rawData[0], 8, addrSpace);
        outStreamer.emitIntValue(rawData[1], 2, addrSpace);
      }

      // emit the label tail padding for the long double.
      TargetData td = tm.getTargetData();
      outStreamer.emitZeros(td.getTypeAllocSize(fp.getType()) - td.getTypeStoreSize(fp.getType()), addrSpace);
      return;
    }

    Util.assertion("Floating point constant type are not handled!");
  }

  private void emitGlobalConstantStruct(ConstantStruct cas, int addrSpace) {
    TargetData td = tm.getTargetData();
    long size = td.getTypeAllocSize(cas.getType());
    TargetData.StructLayout layout = td.getStructLayout(cas.getType());
    long sizeSoFar = 0;
    for (int i = 0, e = cas.getNumOfOperands(); i < e; i++) {
      Constant field = cas.operand(i);

      long fieldSize = td.getTypeAllocSize(field.getType());
      long padSize = ((i == e - 1) ? size : layout.getElementOffset(i + 1)) -
          layout.getElementOffset(i) - fieldSize;
      sizeSoFar += fieldSize + padSize;

      emitGlobalConstant(field, addrSpace);
      outStreamer.emitZeros(padSize, addrSpace);
    }
    Util.assertion(sizeSoFar == layout.getSizeInBytes(), "Layout of constant struct may be corrupted!");
  }

  private void emitGlobalConstantArray(ConstantArray ca, int addrSpace) {
    if (addrSpace != 0 || !ca.isString()) {
      // not a string, print the values in sucessive locations.
      for (int i = 0, e = ca.getNumOfOperands(); i < e; i++)
        emitGlobalConstant(ca.operand(i), addrSpace);
      return;
    }

    // otherwise,print it as a ascii string.
    StringBuilder buf = new StringBuilder();
    for (int i = 0, e = ca.getNumOfOperands(); i < e; i++) {
      buf.append((char) ((ConstantInt) ca.operand(i)).getZExtValue());
    }
    outStreamer.emitBytes(buf.toString(), addrSpace);
  }

  private void emitGlobalConstantLargeInt(ConstantInt ci, int addrSpace) {
    TargetData td = tm.getTargetData();
    int bitwidth = ci.getBitsWidth();
    Util.assertion((bitwidth & 63) == 0, "only support multiples of 64-bit");
    long[] rawData = ci.getValue().getRawData();
    for (int i = 0, e = rawData.length; i < e; i++) {
      long val = td.isBigEndidan() ? rawData[e - i - 1] : rawData[i];
      outStreamer.emitIntValue(val, 8, addrSpace);
    }
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
      if (gv.getType().getElementType().isDoubleTy() && align < 3)
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
   *
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
      MachineLoop loop,
      MCAsmInfo tai,
      int functionNumber) {
    // Add child loop information.
    for (MachineLoop childLoop : loop.getSubLoops()) {
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
  public void printKill(MachineInstr mi) {
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

  public MCSymbol getBlockAddressSymbol(BlockAddress ba) {
    return getAddrLabelSymbol(ba.getBasicBlock());
  }

  /**
   * Return a symbol for the specified constant pool entry.
   *
   * @param cpiId
   * @return
   */
  public MCSymbol getCPISymbol(int cpiId) {
    String name = mai.getPrivateGlobalPrefix() + "CPI" +
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

  public MCSymbol getExternalSymbolSymbol(String sym) {
    String name = mangler.getMangledNameWithPrefix(sym);
    return outContext.getOrCreateSymbol(name);
  }

  public void emitSLEB128(long value) { emitSLEB128(value, null);}
  public void emitSLEB128(long value, String desc) {
    if (verboseAsm && desc != null)
      outStreamer.addComment(desc);

    if (mai.hasLEB128()) {
      outStreamer.emitSLEB128IntValue(value, 0);
      return;
    }

    // .sleb128 doesn't support, emit as .bytes
    long sign = value >> 31;
    boolean isMore;
    do {
      long tmp = value & 0x7f;
      value >>= 7;
      isMore = value != sign || ((tmp ^ sign) & 0x40) != 0;
      if (isMore) tmp |= 0x80;
      outStreamer.emitIntValue(tmp, 1, 0);
    }while (isMore);
  }
  public void emitULEB128(long value) { emitULEB128(value, null);}
  public void emitULEB128(long value, String desc) { emitULEB128(value, desc, 0);}
  public void emitULEB128(long value, String desc, int padTo) {
    if (verboseAsm && desc != null)
      outStreamer.addComment(desc);

    if (mai.hasLEB128() && padTo == 0) {
      outStreamer.emitULEB128IntValue(value, 0);
      return;
    }

    // .uleb128 doesn't support, emit as .bytes
    do {
      long tmp = value & 0x7f;
      value >>>= 7;
      if (value != 0 || padTo != 0) tmp |= 0x80;
      outStreamer.emitIntValue(tmp, 1, 0);
    }while (value != 0);
    if (padTo != 0) {
      if (padTo > 1)
        outStreamer.emitFill(padTo - 1, 0x80, 0);
      outStreamer.emitFill(1, 0, 0);
    }
  }
}
