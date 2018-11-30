/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.target;

import backend.codegen.MachineModuleInfo;
import backend.mc.MCExpr;
import backend.mc.MCSection;
import backend.mc.MCSymbol;
import backend.mc.MCSymbolRefExpr;
import backend.support.NameMangler;
import backend.type.ArrayType;
import backend.type.IntegerType;
import backend.value.Constant;
import backend.value.GlobalValue;
import backend.value.GlobalVariable;
import tools.Util;

import static backend.support.ErrorHandling.llvmReportError;

/**
 * This class used for target specific format of object file.
 */
public abstract class TargetLoweringObjectFile {
  private MCSymbol.MCContext ctx;
  protected TargetLoweringObjectFile() {}
  protected MCSection textSection;
  protected MCSection dataSection;
  protected MCSection bssSection;
  protected MCSection readOnlySection;
  protected MCSection staticCtorSection;
  protected MCSection staticDtorSection;
  /**
   * If exception handling is supported by the target, this is
   * the section the Language Specific Data Area information is emitted to.
   */
  protected MCSection lsdaSection;
  /**
   * If exception handling is supported by the target, this is
   * the section the EH Frame is emitted to.
   */
  protected MCSection ehFrameSection;

  // Dwarf sections for debug info.  If a target supports debug info, these must
  // be set.
  protected MCSection dwarfAbbrevSection;
  protected MCSection dwarfInfoSection;
  protected MCSection dwarfLineSection;
  protected MCSection dwarfFrameSection;
  protected MCSection dwarfPubNamesSection;
  protected MCSection dwarfPubTypesSection;
  protected MCSection dwarfDebugInlineSection;
  protected MCSection dwarfStrSection;
  protected MCSection dwarfLocSection;
  protected MCSection dwarfARangesSection;
  protected MCSection dwarfRangesSection;
  protected MCSection dwarfMacroInfoSection;

  public MCSymbol.MCContext getContext() {
    return ctx;
  }

  /**
   * this method must be called before any actual lowering is
   * done.  This specifies the current context for codegen, and gives the
   * lowering implementations a chance to set up their default sections.
   * @param ctx
   * @param tm
   */
  public void initialize(MCSymbol.MCContext ctx, TargetMachine tm) {
    this.ctx = ctx;
  }

  public MCSection getTextSection() { return textSection; }
  public MCSection getDataSection() { return dataSection; }
  public MCSection getBSSSection() { return bssSection; }
  public MCSection getStaticCtorSection() { return staticCtorSection; }
  public MCSection getStaticDtorSection() { return staticDtorSection; }
  public MCSection getLSDASection() { return lsdaSection; }
  public MCSection getEHFrameSection() { return ehFrameSection; }
  public MCSection getDwarfAbbrevSection() { return dwarfAbbrevSection; }
  public MCSection getDwarfInfoSection() { return dwarfInfoSection; }
  public MCSection getDwarfLineSection() { return dwarfLineSection; }
  public MCSection getDwarfFrameSection() { return dwarfFrameSection; }
  public MCSection getDwarfPubNamesSection() {return dwarfPubNamesSection;}
  public MCSection getDwarfPubTypesSection() {return dwarfPubTypesSection;}
  public MCSection getDwarfDebugInlineSection() {
    return dwarfDebugInlineSection;
  }
  public MCSection getDwarfStrSection() { return dwarfStrSection; }
  public MCSection getDwarfLocSection() { return dwarfLocSection; }
  public MCSection getDwarfARangesSection() { return dwarfARangesSection;}
  public MCSection getDwarfRangesSection() { return dwarfRangesSection; }
  public MCSection getDwarfMacroInfoSection() { return dwarfMacroInfoSection; }

  /**
   * Given a mergableant with the
   * specified size and relocation information, return a section that it
   * should be placed in.
   * @param kind
   * @return
   */
  public MCSection getSectionForConstant(SectionKind kind) {
    Util.assertion(kind != null);
    if (kind.isReadOnly() && readOnlySection != null)
      return readOnlySection;
    return dataSection;
  }

  /**
   * This is a top-level target-independent classifier for
   * a global variable.  Given an global variable and information from TM, it
   * classifies the global in a variety of ways that make various target
   * implementations simpler.  The target implementation is free to ignore this
   * extra info of course.
   * @param gv
   * @param tm
   * @return
   */
  public static SectionKind getKindForGlobal(GlobalValue gv,
                                             TargetMachine tm) {
    Util.assertion(!gv.isDeclaration());
    TargetMachine.RelocModel relocModel = tm.getRelocationModel();
    if (!(gv instanceof GlobalVariable))
      return SectionKind.getText();

    GlobalVariable gvar = (GlobalVariable) gv;
    if (gvar.isThreadLocal()) {
      if (isSuitableForBSS(gvar))
        return SectionKind.getThreadBSS();
      return SectionKind.getThreadData();
    }

    if (isSuitableForBSS(gvar)) {
      if (gvar.hasLocalLinkage())
        return SectionKind.getBSSLocal();
      else if (gvar.hasExternalLinkage())
        return SectionKind.getBSSExtern();
      return SectionKind.getBSS();
    }

    Constant c = gvar.getInitializer();
    if (gvar.isConstant()) {
      switch (c.getRelocationInfo()) {
        default:
          Util.assertion("unknown relocation info");
          break;
        case Constant.NoRelocation:
          if (c.getType() instanceof ArrayType) {
            ArrayType aty = (ArrayType) c.getType();
            if (aty.getElementType() instanceof IntegerType) {
              IntegerType ity = (IntegerType) aty.getElementType();
              if ((ity.getBitWidth() == 8 || ity.getBitWidth() == 16 ||
                  ity.getBitWidth() == 32) &&
                  isNullTerminatedString(c)) {
                if (ity.getBitWidth() == 8)
                  return SectionKind.getMergeable1ByteCString();
                if (ity.getBitWidth() == 16)
                  return SectionKind.getMergeable2ByteCString();

                Util.assertion(ity.getBitWidth() == 32);;
                return SectionKind.getMergeable4ByteCString();
              }
            }
          }

          // Otherwise, just drop it into a mergable constant section.  If we have
          // a section for this size, use it, otherwise use the arbitrary sized
          // mergable section.
          switch ((int) tm.getTargetData().getTypeAllocSize(c.getType())) {
            case 4: return SectionKind.getMergeableConst4();
            case 8: return SectionKind.getMergeableConst8();
            case 16: return SectionKind.getMergeableConst16();
            default: return SectionKind.getMergeableConst();
          }

        case Constant.LocalRelocation:
          // In static relocation model, the linker will resolve all addresses, so
          // the relocation entries will actually be constants by the time the app
          // starts up.  However, we can't put this into a mergable section, because
          // the linker doesn't take relocations into consideration when it tries to
          // merge entries in the section.
          if (relocModel == TargetMachine.RelocModel.Static)
            return SectionKind.getReadOnly();

          // Otherwise, the dynamic linker needs to fix it up, put it in the
          // writable data.rel.local section.
          return SectionKind.getReadOnlyWithRelLocal();

          case Constant.GlobalRelocations:
            // In static relocation model, the linker will resolve all addresses, so
            // the relocation entries will actually be constants by the time the app
            // starts up.  However, we can't put this into a mergable section, because
            // the linker doesn't take relocations into consideration when it tries to
            // merge entries in the section.
            if (relocModel == TargetMachine.RelocModel.Static)
              return SectionKind.getReadOnly();

            return SectionKind.getReadOnlyWithRel();
      }
    }

    // Okay, this isn't a constant.  If the initializer for the global is going
    // to require a runtime relocation by the dynamic linker, put it into a more
    // specific section to improve startup time of the app.  This coalesces these
    // globals together onto fewer pages, improving the locality of the dynamic
    // linker.
    if (relocModel == TargetMachine.RelocModel.Static)
      return SectionKind.getDataNoRel();

    switch (c.getRelocationInfo()) {
      case Constant.GlobalRelocations:
        return SectionKind.getDataRel();
      case Constant.LocalRelocation:
        return SectionKind.getDataRelLocal();
      case Constant.NoRelocation:
        return SectionKind.getDataNoRel();
      default:
          Util.assertion("unknown relocation nfo kind");
          break;
    }
    return null;
  }

  private static boolean isSuitableForBSS(GlobalVariable gvar) {
    return false;
  }

  private static boolean isNullTerminatedString(Constant c) {
    return false;
  }

  /**
   * This method computes the appropriate section to emit
   * the specified global variable or function definition.  This should not
   * be passed external (or available externally) globals.
   * @param gv
   * @param kind
   * @param mangler
   * @param tm
   * @return
   */
  public MCSection sectionForGlobal(GlobalValue gv,
                                    SectionKind kind,
                                    NameMangler mangler,
                                    TargetMachine tm) {
    // Select section name
    if (gv.hasSection())
      return getExplicitSectionGlobal(gv, kind, mangler, tm);

    // Use default section depending on the 'type' of global
    return selectSectionForGlobal(gv, kind, mangler, tm);
  }

  public MCSection selectSectionForGlobal(GlobalValue gv, SectionKind kind, NameMangler mangler, TargetMachine tm) {
    Util.assertion(!kind.isThreadLocal(), "Doesn't support TLS!");
    if (kind.isText())
      return getTextSection();

    if (kind.isBSS() && bssSection != null)
      return bssSection;

    if (kind.isReadOnly() && readOnlySection != null)
      return readOnlySection;

    return getDataSection();
  }

  public MCSection sectionForGlobal(GlobalValue gv,
                                    NameMangler mangler,
                                    TargetMachine tm) {
    return sectionForGlobal(gv, getKindForGlobal(gv, tm), mangler, tm);
  }

  /**
   * Targets should implement this method to assign
   * a section to globals with an explicit section specfied.  The
   * implementation of this method can assume that GV->hasSection() is true.
   * @param gv
   * @param kind
   * @param mangler
   * @param tm
   * @return
   */
  public abstract MCSection getExplicitSectionGlobal(GlobalValue gv,
                                            SectionKind kind,
                                            NameMangler mangler,
                                            TargetMachine tm);

  /**
   * Allow the target to completely override section assignment of a global.
   * @param gv
   * @param mangler
   * @param kind
   * @return
   */
  public MCSection getSpecialCasedSectionGlobals(GlobalValue gv,
                                                 NameMangler mangler,
                                                 SectionKind kind) {
    return null;
  }

  public MCExpr getSymbolForDwarfGlobalReference(GlobalValue gv,
                                                 NameMangler mangler,
                                                 MachineModuleInfo mmi,
                                                 int encoding) {
    String name = mangler.getMangledNameWithPrefix(gv, false);
    MCSymbol sym = getContext().getOrCreateSymbol(name);
    return getSymbolForDwarfReference(sym, mmi, encoding);
  }

  public MCExpr getSymbolForDwarfReference(MCSymbol sym,
                                           MachineModuleInfo mmi,
                                           int encoding) {
    Util.shouldNotReachHere("Dwarf is not supported yet");
    return null;
  }

  public int getPersonalityEncoding() {
    Util.shouldNotReachHere("Dwarf is not supported yet");
    return 0;
  }
  public int getLSDAEncoding() {
    Util.shouldNotReachHere("Dwarf is not supported yet");
    return 0;
  }
  public int getFDEEncoding() {
    Util.shouldNotReachHere("Dwarf is not supported yet");
    return 0;
  }
  public int getTTypeEncoding() {
    Util.shouldNotReachHere("Dwarf is not supported yet");
    return 0;
  }
}
