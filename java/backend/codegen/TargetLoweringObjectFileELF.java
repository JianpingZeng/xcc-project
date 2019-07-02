/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2015-2019, Jianping Zeng.
 * All rights reserved.
 *
 * Please refer the LICENSE for detail.
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
import backend.target.TargetOptions;
import backend.value.GlobalValue;
import backend.value.GlobalVariable;
import tools.Util;

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

  @Override
  public void initialize(MCContext ctx, TargetMachine tm) {
    super.initialize(ctx, tm);
    BSSSection =
        getContext().getELFSection(".bss", MCSectionELF.SHT_NOBITS,
            MCSectionELF.SHF_WRITE | MCSectionELF.SHF_ALLOC,
            SectionKind.getBSS());

    TextSection =
        getContext().getELFSection(".text", MCSectionELF.SHT_PROGBITS,
            MCSectionELF.SHF_EXECINSTR |
                MCSectionELF.SHF_ALLOC,
            SectionKind.getText());

    DataSection =
        getContext().getELFSection(".data", MCSectionELF.SHT_PROGBITS,
            MCSectionELF.SHF_WRITE | MCSectionELF.SHF_ALLOC,
            SectionKind.getDataRel());

    ReadOnlySection =
        getContext().getELFSection(".rodata", MCSectionELF.SHT_PROGBITS,
            MCSectionELF.SHF_ALLOC,
            SectionKind.getReadOnly());

    TLSDataSection =
        getContext().getELFSection(".tdata", MCSectionELF.SHT_PROGBITS,
            MCSectionELF.SHF_ALLOC | MCSectionELF.SHF_TLS |
                MCSectionELF.SHF_WRITE,
            SectionKind.getThreadData());

    TLSBSSSection =
        getContext().getELFSection(".tbss", MCSectionELF.SHT_NOBITS,
            MCSectionELF.SHF_ALLOC | MCSectionELF.SHF_TLS |
                MCSectionELF.SHF_WRITE,
            SectionKind.getThreadBSS());

    DataRelSection =
        getContext().getELFSection(".data.rel", MCSectionELF.SHT_PROGBITS,
            MCSectionELF.SHF_ALLOC | MCSectionELF.SHF_WRITE,
            SectionKind.getDataRel());

    DataRelLocalSection =
        getContext().getELFSection(".data.rel.local", MCSectionELF.SHT_PROGBITS,
            MCSectionELF.SHF_ALLOC | MCSectionELF.SHF_WRITE,
            SectionKind.getDataRelLocal());

    DataRelROSection =
        getContext().getELFSection(".data.rel.ro", MCSectionELF.SHT_PROGBITS,
            MCSectionELF.SHF_ALLOC | MCSectionELF.SHF_WRITE,
            SectionKind.getReadOnlyWithRel());

    DataRelROLocalSection =
        getContext().getELFSection(".data.rel.ro.local", MCSectionELF.SHT_PROGBITS,
            MCSectionELF.SHF_ALLOC | MCSectionELF.SHF_WRITE,
            SectionKind.getReadOnlyWithRelLocal());

    MergeableConst4Section =
        getContext().getELFSection(".rodata.cst4", MCSectionELF.SHT_PROGBITS,
            MCSectionELF.SHF_ALLOC | MCSectionELF.SHF_MERGE,
            SectionKind.getMergeableConst4());

    MergeableConst8Section =
        getContext().getELFSection(".rodata.cst8", MCSectionELF.SHT_PROGBITS,
            MCSectionELF.SHF_ALLOC | MCSectionELF.SHF_MERGE,
            SectionKind.getMergeableConst8());

    MergeableConst16Section =
        getContext().getELFSection(".rodata.cst16", MCSectionELF.SHT_PROGBITS,
            MCSectionELF.SHF_ALLOC | MCSectionELF.SHF_MERGE,
            SectionKind.getMergeableConst16());

    StaticCtorSection =
        getContext().getELFSection(".ctors", MCSectionELF.SHT_PROGBITS,
            MCSectionELF.SHF_ALLOC | MCSectionELF.SHF_WRITE,
            SectionKind.getDataRel());

    StaticDtorSection =
        getContext().getELFSection(".dtors", MCSectionELF.SHT_PROGBITS,
            MCSectionELF.SHF_ALLOC | MCSectionELF.SHF_WRITE,
            SectionKind.getDataRel());

    // Exception Handling Sections.

    // FIXME: We're emitting LSDA info into a readonly section on ELF, even though
    // it contains relocatable pointers.  In PIC mode, this is probably a big
    // runtime hit for C++ apps.  Either the contents of the LSDA need to be
    // adjusted or this should be a data section.
    LSDASection =
        getContext().getELFSection(".gcc_except_table", MCSectionELF.SHT_PROGBITS,
            MCSectionELF.SHF_ALLOC,
            SectionKind.getReadOnly());
    EHFrameSection =
        getContext().getELFSection(".eh_frame", MCSectionELF.SHT_PROGBITS,
            MCSectionELF.SHF_ALLOC | MCSectionELF.SHF_WRITE,
            SectionKind.getDataRel());

    // Debug Info Sections.
    DwarfAbbrevSection =
        getContext().getELFSection(".debug_abbrev", MCSectionELF.SHT_PROGBITS, 0,
            SectionKind.getMetadata());
    DwarfInfoSection =
        getContext().getELFSection(".debug_info", MCSectionELF.SHT_PROGBITS, 0,
            SectionKind.getMetadata());
    DwarfLineSection =
        getContext().getELFSection(".debug_line", MCSectionELF.SHT_PROGBITS, 0,
            SectionKind.getMetadata());
    DwarfFrameSection =
        getContext().getELFSection(".debug_frame", MCSectionELF.SHT_PROGBITS, 0,
            SectionKind.getMetadata());
    DwarfPubNamesSection =
        getContext().getELFSection(".debug_pubnames", MCSectionELF.SHT_PROGBITS, 0,
            SectionKind.getMetadata());
    DwarfPubTypesSection =
        getContext().getELFSection(".debug_pubtypes", MCSectionELF.SHT_PROGBITS, 0,
            SectionKind.getMetadata());
    DwarfStrSection =
        getContext().getELFSection(".debug_str", MCSectionELF.SHT_PROGBITS, 0,
            SectionKind.getMetadata());
    DwarfLocSection =
        getContext().getELFSection(".debug_loc", MCSectionELF.SHT_PROGBITS, 0,
            SectionKind.getMetadata());
    DwarfARangesSection =
        getContext().getELFSection(".debug_aranges", MCSectionELF.SHT_PROGBITS, 0,
            SectionKind.getMetadata());
    DwarfRangesSection =
        getContext().getELFSection(".debug_ranges", MCSectionELF.SHT_PROGBITS, 0,
            SectionKind.getMetadata());
    DwarfMacroInfoSection =
        getContext().getELFSection(".debug_macinfo", MCSectionELF.SHT_PROGBITS, 0,
            SectionKind.getMetadata());
  }

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
    BSSSection = getELFSection(".bss", MCSectionELF.SHT_NOBITS,
        MCSectionELF.SHF_WRITE | MCSectionELF.SHF_ALLOC,
        SectionKind.getBSS());
    TextSection = getELFSection(".text",
        MCSectionELF.SHT_PROGBITS,
        MCSectionELF.SHF_EXECINSTR | MCSectionELF.SHF_ALLOC,
        SectionKind.getText());
    DataSection = getELFSection(".data",
        MCSectionELF.SHT_PROGBITS,
        MCSectionELF.SHF_WRITE | MCSectionELF.SHF_ALLOC,
        SectionKind.getDataRel());

    ReadOnlySection = getELFSection(".rodata",
        MCSectionELF.SHT_PROGBITS,
        MCSectionELF.SHF_TLS | MCSectionELF.SHF_WRITE | MCSectionELF.SHF_ALLOC,
        SectionKind.getBSS());
    tlsDataSection = getELFSection(".tbss",
        MCSectionELF.SHT_NOBITS,
        MCSectionELF.SHF_TLS | MCSectionELF.SHF_WRITE | MCSectionELF.SHF_ALLOC,
        SectionKind.getBSS());

  }

  public MCSection getDataRelSection() {
    return DataRelSection;
  }

  /**
   * Given a constant with the SectionKind, return a section that it should be placed in.
   *
   * @param kind
   * @return
   */
  public MCSection getSectionForConstant(SectionKind kind) {
    if (kind.isMergeableConst4() && MergeableConst4Section != null)
      return MergeableConst4Section;
    if (kind.isMergeableConst8() && MergeableConst8Section != null)
      return MergeableConst8Section;
    if (kind.isMergeableConst16() && MergeableConst16Section != null)
      return MergeableConst16Section;
    if (kind.isReadOnly())
      return ReadOnlySection;
    if (kind.isReadOnlyWithRelLocal()) return DataRelROLocalSection;
    Util.assertion(kind.isReadOnlyWithRel(), "Unknown section kind");
    return DataRelROSection;
  }

  static int getELFSectionType(String name, SectionKind k) {
    switch (name) {
      case ".init_array":
        return MCSectionELF.SHT_INIT_ARRAY;
      case ".fini_array":
        return MCSectionELF.SHT_FINI_ARRAY;
      case ".preinit_array":
        return MCSectionELF.SHT_PREINIT_ARRAY;
      default:
        break;
    }
    if (k.isBSS() || k.isThreadBSS())
      return MCSectionELF.SHT_NOBITS;
    return MCSectionELF.SHT_PROGBITS;
  }

  static int getELFSectionFlags(SectionKind k) {
    int flags = 0;
    if (!k.isMetadata())
      flags |= MCSectionELF.SHF_ALLOC;
    if (k.isText())
      flags |= MCSectionELF.SHF_EXECINSTR;
    if (k.isWriteable())
      flags |= MCSectionELF.SHF_WRITE;
    if (k.isThreadLocal())
      flags |= MCSectionELF.SHF_TLS;

    if (k.isMergeableCString() || k.isMergeableConst4() ||
        k.isMergeableConst8() || k.isMergeableConst16())
      flags |= MCSectionELF.SHF_MERGE;

    if (k.isMergeableCString())
      flags |= MCSectionELF.SHF_STRINGS;
    return flags;
  }

  static SectionKind getELFKindForNamedSection(String name, SectionKind k) {
    if (name == null || name.isEmpty() || name.charAt(0) == '.')
      return k;

    // Some lame default implementation based on some magic section names.
    if (name.equals(".bss") ||
        name.startsWith(".bss.") ||
        name.startsWith(".gnu.linkonce.b.") ||
        name.startsWith(".llvm.linkonce.b.") ||
        name.equals(".sbss") ||
        name.startsWith(".sbss.") ||
        name.startsWith(".gnu.linkonce.sb.") ||
        name.startsWith(".llvm.linkonce.sb."))
      return SectionKind.getBSS();

    if (name.equals(".tdata") ||
        name.startsWith(".tdata.") ||
        name.startsWith(".gnu.linkonce.td.") ||
        name.startsWith(".llvm.linkonce.td."))
      return SectionKind.getThreadData();

    if (name.equals(".tbss") ||
        name.startsWith(".tbss.") ||
        name.startsWith(".gnu.linkonce.tb.") ||
        name.startsWith(".llvm.linkonce.tb."))
      return SectionKind.getThreadBSS();

    return k;
  }

  public MCSection
  getExplicitSectionGlobal(GlobalValue gv, SectionKind kind,
                           NameMangler mang, TargetMachine tm) {
    String sectionName = gv.getSection();
    // Infer section flags from the section name if we can.
    kind = getELFKindForNamedSection(sectionName, kind);

    return getContext().getELFSection(sectionName,
        getELFSectionType(sectionName, kind),
        getELFSectionFlags(kind), kind, true);
  }

  /**
   * If we have -ffunction-section or -fdata-section then we should emit the
   * global value to a uniqued section specifically for it.
   *
   * @param gv
   * @param kind
   * @param mang
   * @param tm
   * @return
   */
  public MCSection selectSectionForGlobal(GlobalValue gv, SectionKind kind,
                                          NameMangler mang, TargetMachine tm) {
    boolean emitUniquedSection = kind.isText() ?
        TargetOptions.FunctionSections.value : TargetOptions.DataSections.value;

    // If this global is linkonce/weak and the target handles this by emitting it
    // into a 'uniqued' section name, create and return the section now.
    if ((gv.isWeakForLinker() || emitUniquedSection) &&
        !kind.isCommon() && !kind.isBSS()) {
      String prefix;
      if (gv.isWeakForLinker())
        prefix = getSectionPrefixUniqueGlobal(kind);
      else {
        Util.assertion(emitUniquedSection);
        prefix = getSectionPrefixForGlobal(kind);
      }
      String name = mang.getMangledNameWithPrefix(gv, false);
      MCSymbol sym = getContext().createSymbol(name);
      name = prefix + name;
      return getContext().getELFSection(name, getELFSectionType(name, kind),
          getELFSectionFlags(kind), kind);
    }

    if (kind.isText()) return TextSection;
    if (kind.isMergeable1ByteCString() ||
        kind.isMergeable2ByteCString() ||
        kind.isMergeable4ByteCString()) {

      // We also need alignment here.
      // FIXME: this is getting the alignment of the character, not the
      // alignment of the global!
      int align = tm.getTargetData().getPreferredAlignment((GlobalVariable) gv);
      String sizeSpec = ".rodata.str1.";
      if (kind.isMergeable2ByteCString())
        sizeSpec = ".rodata.str2.";
      else if (kind.isMergeable4ByteCString())
        sizeSpec = ".rodata.str4.";
      else
        Util.assertion(kind.isMergeable1ByteCString(), "unknown string width");

      String name = sizeSpec + align;
      return getContext().getELFSection(name, MCSectionELF.SHT_PROGBITS,
          MCSectionELF.SHF_ALLOC |
              MCSectionELF.SHF_MERGE |
              MCSectionELF.SHF_STRINGS,
          kind);
    }

    if (kind.isMergeableConst()) {
      if (kind.isMergeableConst4() && MergeableConst4Section != null)
        return MergeableConst4Section;
      if (kind.isMergeableConst8() && MergeableConst8Section != null)
        return MergeableConst8Section;
      if (kind.isMergeableConst16() && MergeableConst16Section != null)
        return MergeableConst16Section;
      return ReadOnlySection;  // .const
    }

    if (kind.isReadOnly()) return ReadOnlySection;

    if (kind.isThreadData()) return TLSDataSection;
    if (kind.isThreadBSS()) return TLSBSSSection;

    // Note: we claim that common symbols are put in BSSSection, but they are
    // really emitted with the magic .comm directive, which creates a symbol table
    // entry but not a section.
    if (kind.isBSS() || kind.isCommon()) return BSSSection;

    if (kind.isDataNoRel()) return DataSection;
    if (kind.isDataRelLocal()) return DataRelLocalSection;
    if (kind.isDataRel()) return DataRelSection;
    if (kind.isReadOnlyWithRelLocal()) return DataRelROLocalSection;

    Util.assertion(kind.isReadOnlyWithRel(), "Unknown section kind");
    return DataRelROSection;
  }

  private static String getSectionPrefixUniqueGlobal(SectionKind kind) {
    if (kind.isText()) return ".gnu.linkonce.t.";
    if (kind.isReadOnly()) return ".gnu.linkonce.r.";

    if (kind.isThreadData()) return ".gnu.linkonce.td.";
    if (kind.isThreadBSS()) return ".gnu.linkonce.tb.";

    if (kind.isDataNoRel()) return ".gnu.linkonce.d.";
    if (kind.isDataRelLocal()) return ".gnu.linkonce.d.rel.local.";
    if (kind.isDataRel()) return ".gnu.linkonce.d.rel.";
    if (kind.isReadOnlyWithRelLocal()) return ".gnu.linkonce.d.rel.ro.local.";

    Util.assertion(kind.isReadOnlyWithRel(), "Unknown section kind");
    return ".gnu.linkonce.d.rel.ro.";
  }

  /**
   * Return the section prefix name used by options FunctionsSections and DataSections.
   *
   * @param kind
   * @return
   */
  private static String getSectionPrefixForGlobal(SectionKind kind) {
    if (kind.isText()) return ".text.";
    if (kind.isReadOnly()) return ".rodata.";

    if (kind.isThreadData()) return ".tdata.";
    if (kind.isThreadBSS()) return ".tbss.";

    if (kind.isDataNoRel()) return ".data.";
    if (kind.isDataRelLocal()) return ".data.rel.local.";
    if (kind.isDataRel()) return ".data.rel.";
    if (kind.isReadOnlyWithRelLocal()) return ".data.rel.ro.local.";

    Util.assertion(kind.isReadOnlyWithRel(), "Unknown section kind");
    return ".data.rel.ro.";
  }

  /**
   * Return an MCExpr to use for a reference to the specified global
   * variable from exception handling information.
   *
   * @param gv
   * @param mang
   * @param mmi
   * @param encoding
   * @return
   */
  public MCExpr getSymbolForDwarfGlobalReference(GlobalValue gv,
                                                 NameMangler mang,
                                                 MachineModuleInfo mmi,
                                                 int encoding) {
    Util.shouldNotReachHere();
    return null;
  }
}
