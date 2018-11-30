/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.codegen;

import backend.mc.MCExpr;
import backend.mc.MCSection;
import backend.mc.MCSectionELF;
import backend.mc.MCSymbol;
import backend.mc.MCSymbol.MCContext;
import backend.support.NameMangler;
import backend.target.SectionKind;
import backend.target.TargetLoweringObjectFile;
import backend.target.TargetMachine;
import backend.value.GlobalValue;

import java.util.TreeMap;

public abstract class TargetLoweringObjectFileELF extends TargetLoweringObjectFile {
  private TreeMap<String, MCSectionELF> uniqueMap;


  /// TLSDataSection - Section directive for Thread Local data.
  // for ".tdata"
  protected MCSection tlsDataSection;

  protected MCSection TLSDataSection;        // Defaults to ".tdata".

  /// TLSBSSSection - Section directive for Thread Local uninitialized data.
  /// Null if this target doesn't support a BSS section.
  ///
  protected MCSection TLSBSSSection;         // Defaults to ".tbss".

  protected MCSection DataRelSection;
  protected MCSection DataRelLocalSection;
  protected MCSection DataRelROSection;
  protected MCSection DataRelROLocalSection;

  protected MCSection MergeableConst4Section;
  protected MCSection MergeableConst8Section;
  protected MCSection MergeableConst16Section;

  protected MCSection getELFSection(String Section, int Type,
                                    int Flags, SectionKind Kind) {
    return getELFSection(Section, Type, Flags, Kind, false);
  }

  protected MCSection getELFSection(String section, int type,
                                    int Flags, SectionKind Kind,
                                    boolean IsExplicit) {
    if (uniqueMap == null)
      uniqueMap = new TreeMap<>();
    if (uniqueMap.containsKey(section))
      return uniqueMap.get(section);

    MCSectionELF entry = MCSectionELF.create(section, type, Flags, Kind, IsExplicit, getContext());
    uniqueMap.put(section, entry);
    return entry;
  }

  public void Initialize(MCContext Ctx, TargetMachine TM) {
    if (uniqueMap != null)
      uniqueMap.clear();

    super.initialize(Ctx, TM);
    bssSection = getELFSection(".bss", MCSectionELF.SHT_NOBITS,
        MCSectionELF.SHF_WRITE|MCSectionELF.SHF_ALLOC,
        SectionKind.getBSS());
    textSection = getELFSection(".text",
        MCSectionELF.SHT_PROGBITS,
        MCSectionELF.SHF_EXECINSTR | MCSectionELF.SHF_ALLOC,
        SectionKind.getText());
    dataSection = getELFSection(".data",
        MCSectionELF.SHT_PROGBITS,
        MCSectionELF.SHF_WRITE | MCSectionELF.SHF_ALLOC,
        SectionKind.getDataRel());

    readOnlySection = getELFSection(".rodata",
        MCSectionELF.SHT_PROGBITS,
        MCSectionELF.SHF_TLS | MCSectionELF.SHF_WRITE | MCSectionELF.SHF_ALLOC,
        SectionKind.getBSS());
    tlsDataSection = getELFSection(".tbss",
        MCSectionELF.SHT_NOBITS,
        MCSectionELF.SHF_TLS | MCSectionELF.SHF_WRITE|MCSectionELF.SHF_ALLOC,
        SectionKind.getBSS());

  }

  public MCSection getDataRelSection() {
    return DataRelSection;
  }

  /// getSectionForConstant - Given a constant with the SectionKind, return a
  /// section that it should be placed in.
  public MCSection getSectionForConstant(SectionKind Kind) {

  }


  public MCSection
  getExplicitSectionGlobal(GlobalValue gv, SectionKind Kind,
                           NameMangler Mang, TargetMachine TM) {
  }

  public MCSection selectSectionForGlobal(GlobalValue gv, SectionKind Kind,
                                          NameMangler Mang, TargetMachine TM) {
  }

  /// getSymbolForDwarfGlobalReference - Return an MCExpr to use for a reference
  /// to the specified global variable from exception handling information.
  ///
  public MCExpr getSymbolForDwarfGlobalReference(GlobalValue gv, NameMangler Mang,
                                                 MachineModuleInfo MMI, int Encoding) {
  }
}
