/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2020, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.target;

import backend.codegen.MachineModuleInfo;
import backend.mc.MCExpr;
import backend.mc.MCSection;
import backend.mc.MCSymbol;
import backend.support.NameMangler;
import backend.type.ArrayType;
import backend.type.IntegerType;
import backend.value.*;
import tools.Util;

/**
 * This class used for target specific format of object file.
 */
public abstract class TargetLoweringObjectFile {
  private MCSymbol.MCContext ctx;
  protected TargetLoweringObjectFile() {}
  protected MCSection TextSection;
  protected MCSection DataSection;
  protected MCSection BSSSection;
  protected MCSection ReadOnlySection;
  protected MCSection StaticCtorSection;
  protected MCSection StaticDtorSection;
  /**
   * If exception handling is supported by the target, this is
   * the section the Language Specific Data Area information is emitted to.
   */
  protected MCSection LSDASection;
  /**
   * If exception handling is supported by the target, this is
   * the section the EH Frame is emitted to.
   */
  protected MCSection EHFrameSection;

  // Dwarf sections for debug info.  If a target supports debug info, these must
  // be set.
  protected MCSection DwarfAbbrevSection;
  protected MCSection DwarfInfoSection;
  protected MCSection DwarfLineSection;
  protected MCSection DwarfFrameSection;
  protected MCSection DwarfPubNamesSection;
  protected MCSection DwarfPubTypesSection;
  protected MCSection DwarfDebugInlineSection;
  protected MCSection DwarfStrSection;
  protected MCSection DwarfLocSection;
  protected MCSection DwarfARangesSection;
  protected MCSection DwarfRangesSection;
  protected MCSection DwarfMacroInfoSection;

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

  public MCSection getTextSection() { return TextSection; }
  public MCSection getDataSection() { return DataSection; }
  public MCSection getBSSSection() { return BSSSection; }
  public MCSection getStaticCtorSection() { return StaticCtorSection; }
  public MCSection getStaticDtorSection() { return StaticDtorSection; }
  public MCSection getLSDASection() { return LSDASection; }
  public MCSection getEHFrameSection() { return EHFrameSection; }
  public MCSection getDwarfAbbrevSection() { return DwarfAbbrevSection; }
  public MCSection getDwarfInfoSection() { return DwarfInfoSection; }
  public MCSection getDwarfLineSection() { return DwarfLineSection; }
  public MCSection getDwarfFrameSection() { return DwarfFrameSection; }
  public MCSection getDwarfPubNamesSection() {return DwarfPubNamesSection;}
  public MCSection getDwarfPubTypesSection() {return DwarfPubTypesSection;}
  public MCSection getDwarfDebugInlineSection() {
    return DwarfDebugInlineSection;
  }
  public MCSection getDwarfStrSection() { return DwarfStrSection; }
  public MCSection getDwarfLocSection() { return DwarfLocSection; }
  public MCSection getDwarfARangesSection() { return DwarfARangesSection;}
  public MCSection getDwarfRangesSection() { return DwarfRangesSection; }
  public MCSection getDwarfMacroInfoSection() { return DwarfMacroInfoSection; }

  /**
   * Given a mergableant with the
   * specified size and relocation information, return a section that it
   * should be placed in.
   * @param kind
   * @return
   */
  public MCSection getSectionForConstant(SectionKind kind) {
    Util.assertion(kind != null);
    if (kind.isReadOnly() && ReadOnlySection != null)
      return ReadOnlySection;
    return DataSection;
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
    Constant c = gvar.getInitializer();
    // must have zero initializer
    if (!c.isNullValue())
      return false;

    // leave constant  zeros in readonly constant section.
    if (gvar.isConstant())
      return false;

    // if the global has an explicit section specified, don't put it in BSS.
    if (!gvar.getSection().isEmpty())
      return false;
    // otherwise, put it in BSS!
    return false;
  }

  /**
   * Return true if the specified constant (which is known to have a type that
   * is an array of 1/2/4 byte elements) ends with a null value and constants
   * no other nulls in it.
   * @param c
   * @return
   */
  private static boolean isNullTerminatedString(Constant c) {
    ArrayType aty = (ArrayType) c.getType();
    if (c instanceof ConstantArray) {
      ConstantArray ca = (ConstantArray) c;
      if (aty.getNumElements() == 0) return false;

      ConstantInt nullptr = ca.getOperand((int) (aty.getNumElements()-1)).getValue() instanceof ConstantInt ?
          (ConstantInt)ca.getOperand((int) (aty.getNumElements()-1)).getValue() : null;
      if (nullptr == null || !nullptr.isZero())
        return false;

      // Verify that the null doesn't occur anywhere else in the string.
      for (int i = 0, e = (int)aty.getNumElements()-1; i !=e; i++) {
        // Reject constantexpr elements etc.
        if (!(ca.getOperand(i).getValue() instanceof ConstantInt) ||
        ca.getOperand(i).getValue() == nullptr)
          return false;
      }
      return true;
    }

    // Another possibility: [1 x i8] zeroinitializer
    if (c instanceof ConstantAggregateZero)
      return aty.getNumElements() == 1;

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

    if (kind.isBSS() && BSSSection != null)
      return BSSSection;

    if (kind.isReadOnly() && ReadOnlySection != null)
      return ReadOnlySection;

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
