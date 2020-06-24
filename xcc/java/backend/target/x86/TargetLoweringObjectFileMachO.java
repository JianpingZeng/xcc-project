/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2019, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.target.x86;

import backend.codegen.MachineModuleInfo;
import backend.mc.*;
import backend.support.Dwarf;
import backend.support.NameMangler;
import backend.target.SectionKind;
import backend.target.TargetLoweringObjectFile;
import backend.target.TargetMachine;
import backend.value.Function;
import backend.value.GlobalValue;
import backend.value.GlobalVariable;
import tools.OutRef;
import tools.Util;

import static backend.support.ErrorHandling.reportFatalError;

/**
 * This class is designed here for describing some common assembly and mach-o object file
 * to the 32 bit and 64 bit X86 archiecture.
 */
public class TargetLoweringObjectFileMachO extends TargetLoweringObjectFile{
  /**
   * For thread local data. Default to ".tdata"
   */
  private MCSection tlsDataSection;
  /**
   * Section for thread local uninitialized data. Default to ".tbss"
   */
  private MCSection tlsBSSSection;
  /**
   * Section for thread local structure information. It usually contains
   * the source code name of the variable, visibility and a pointer to
   * the initial value (.tdata or .tbss).
   * Defaults to ".tly"
   */
  private MCSection tlsTLVSection;
  /**
   * Section for thread local data initialization functions. Defauls to ".thread_init_func".
   */
  private MCSection tlsThreadInitSection;

  private MCSection cstringSection;
  private MCSection ustringSection;
  private MCSection textCoalSection;
  private MCSection constTextCoalSection;
  private MCSection constDataSection;
  private MCSection dataCoalSection;
  private MCSection dataCommonSection;
  private MCSection dataBSSSection;
  private MCSection fourByteConstantSection;
  private MCSection eightByteConstantSection;
  private MCSection sixteenByteConstantSection;
  private MCSection lazySymbolPointerSection;
  private MCSection nonLazySymbolPointerSection;

  public void initialize(MCSymbol.MCContext ctx, TargetMachine tm) {
    super.initialize(ctx, tm);

    // .text
    TextSection = getContext().getMachOSection("__TEXT", "__text",
        MCSectionMachO.S_ATTR_PURE_INSTRUCTIONS,
        SectionKind.getText());
    // .data
    DataSection = getContext().getMachOSection("__DATA", "__data",
        0, SectionKind.getDataRel());
    // .tdata
    tlsDataSection // .tdata
        = getContext().getMachOSection("__DATA", "__thread_data",
        MCSectionMachO.S_THREAD_LOCAL_REGULAR,
        SectionKind.getDataRel());
    tlsBSSSection // .tbss
        = getContext().getMachOSection("__DATA", "__thread_bss",
        MCSectionMachO.S_THREAD_LOCAL_ZEROFILL,
        SectionKind.getThreadBSS());

    // TODO: Verify datarel below.
    tlsTLVSection // .tlv
        = getContext().getMachOSection("__DATA", "__thread_vars",
        MCSectionMachO.S_THREAD_LOCAL_VARIABLES,
        SectionKind.getDataRel());

    tlsThreadInitSection
        = getContext().getMachOSection("__DATA", "__thread_init",
        MCSectionMachO.S_THREAD_LOCAL_INIT_FUNCTION_POINTERS,
        SectionKind.getDataRel());

    cstringSection // .cstring
        = getContext().getMachOSection("__TEXT", "__cstring",
        MCSectionMachO.S_CSTRING_LITERALS,
        SectionKind.getMergeable1ByteCString());
    ustringSection
        = getContext().getMachOSection("__TEXT","__ustring", 0,
        SectionKind.getMergeable2ByteCString());
    fourByteConstantSection // .literal4
        = getContext().getMachOSection("__TEXT", "__literal4",
        MCSectionMachO.S_4BYTE_LITERALS,
        SectionKind.getMergeableConst4());
    eightByteConstantSection // .literal8
        = getContext().getMachOSection("__TEXT", "__literal8",
        MCSectionMachO.S_8BYTE_LITERALS,
        SectionKind.getMergeableConst8());

    // ld_classic doesn't support .literal16 in 32-bit mode, and ld64 falls back
    // to using it in -static mode.
    sixteenByteConstantSection = null;
    if (tm.getRelocationModel() != TargetMachine.RelocModel.Static &&
        tm.getTargetData().getPointerSize() == 32)
    sixteenByteConstantSection =   // .literal16
        getContext().getMachOSection("__TEXT", "__literal16",
            MCSectionMachO.S_16BYTE_LITERALS,
            SectionKind.getMergeableConst16());

    ReadOnlySection  // .const
        = getContext().getMachOSection("__TEXT", "__const", 0,
        SectionKind.getReadOnly());

    textCoalSection
        = getContext().getMachOSection("__TEXT", "__textcoal_nt",
        MCSectionMachO.S_COALESCED |
            MCSectionMachO.S_ATTR_PURE_INSTRUCTIONS,
        SectionKind.getText());
     constTextCoalSection
        = getContext().getMachOSection("__TEXT", "__const_coal",
        MCSectionMachO.S_COALESCED,
        SectionKind.getReadOnly());

    constDataSection  // .const_data
        = getContext().getMachOSection("__DATA", "__const", 0,
        SectionKind.getReadOnlyWithRel());
    dataCoalSection
        = getContext().getMachOSection("__DATA","__datacoal_nt",
        MCSectionMachO.S_COALESCED,
        SectionKind.getDataRel());
    dataCommonSection
        = getContext().getMachOSection("__DATA","__common",
        MCSectionMachO.S_ZEROFILL,
        SectionKind.getBSS());
    dataBSSSection
        = getContext().getMachOSection("__DATA","__bss", MCSectionMachO.S_ZEROFILL,
        SectionKind.getBSS());

    lazySymbolPointerSection
        = getContext().getMachOSection("__DATA", "__la_symbol_ptr",
        MCSectionMachO.S_LAZY_SYMBOL_POINTERS,
        SectionKind.getMetadata());
    nonLazySymbolPointerSection
        = getContext().getMachOSection("__DATA", "__nl_symbol_ptr",
        MCSectionMachO.S_NON_LAZY_SYMBOL_POINTERS,
        SectionKind.getMetadata());

    if (tm.getRelocationModel() == TargetMachine.RelocModel.Static) {
      StaticCtorSection
          = getContext().getMachOSection("__TEXT", "__constructor", 0,
          SectionKind.getDataRel());
      StaticDtorSection
          = getContext().getMachOSection("__TEXT", "__destructor", 0,
          SectionKind.getDataRel());
    } else {
      StaticCtorSection
          = getContext().getMachOSection("__DATA", "__mod_init_func",
          MCSectionMachO.S_MOD_INIT_FUNC_POINTERS,
          SectionKind.getDataRel());
      StaticDtorSection
          = getContext().getMachOSection("__DATA", "__mod_term_func",
          MCSectionMachO.S_MOD_TERM_FUNC_POINTERS,
          SectionKind.getDataRel());
    }

    // Exception Handling.
    LSDASection = getContext().getMachOSection("__TEXT", "__gcc_except_tab", 0,
        SectionKind.getReadOnlyWithRel());
    EHFrameSection =
        getContext().getMachOSection("__TEXT", "__eh_frame",
            MCSectionMachO.S_COALESCED |
                MCSectionMachO.S_ATTR_NO_TOC |
                MCSectionMachO.S_ATTR_STRIP_STATIC_SYMS |
                MCSectionMachO.S_ATTR_LIVE_SUPPORT,
            SectionKind.getReadOnly());

    // Debug Information.
    DwarfAbbrevSection =
        getContext().getMachOSection("__DWARF", "__debug_abbrev",
            MCSectionMachO.S_ATTR_DEBUG,
            SectionKind.getMetadata());
    DwarfInfoSection =
        getContext().getMachOSection("__DWARF", "__debug_info",
            MCSectionMachO.S_ATTR_DEBUG,
            SectionKind.getMetadata());
    DwarfLineSection =
        getContext().getMachOSection("__DWARF", "__debug_line",
            MCSectionMachO.S_ATTR_DEBUG,
            SectionKind.getMetadata());
    DwarfFrameSection =
        getContext().getMachOSection("__DWARF", "__debug_frame",
            MCSectionMachO.S_ATTR_DEBUG,
            SectionKind.getMetadata());
    DwarfPubNamesSection =
        getContext().getMachOSection("__DWARF", "__debug_pubnames",
            MCSectionMachO.S_ATTR_DEBUG,
            SectionKind.getMetadata());
    DwarfPubTypesSection =
        getContext().getMachOSection("__DWARF", "__debug_pubtypes",
            MCSectionMachO.S_ATTR_DEBUG,
            SectionKind.getMetadata());
    DwarfStrSection =
        getContext().getMachOSection("__DWARF", "__debug_str",
            MCSectionMachO.S_ATTR_DEBUG,
            SectionKind.getMetadata());
    DwarfLocSection =
        getContext().getMachOSection("__DWARF", "__debug_loc",
            MCSectionMachO.S_ATTR_DEBUG,
            SectionKind.getMetadata());
    DwarfARangesSection =
        getContext().getMachOSection("__DWARF", "__debug_aranges",
            MCSectionMachO.S_ATTR_DEBUG,
            SectionKind.getMetadata());
    DwarfRangesSection =
        getContext().getMachOSection("__DWARF", "__debug_ranges",
            MCSectionMachO.S_ATTR_DEBUG,
            SectionKind.getMetadata());
    DwarfMacroInfoSection =
        getContext().getMachOSection("__DWARF", "__debug_macinfo",
            MCSectionMachO.S_ATTR_DEBUG,
            SectionKind.getMetadata());
    DwarfDebugInlineSection =
        getContext().getMachOSection("__DWARF", "__debug_inlined",
            MCSectionMachO.S_ATTR_DEBUG,
            SectionKind.getMetadata());
  }

  public MCSection selectSectionForGlobal(GlobalValue gv,
                                          SectionKind kind,
                                          NameMangler mangler,
                                          TargetMachine tm) {
    // handle thread local data.
    if (kind.isThreadBSS()) return tlsBSSSection;
    if (kind.isThreadData()) return tlsDataSection;

    if (kind.isText())
      return gv.isWeakForLinker() ? textCoalSection : TextSection;

    if (gv.isWeakForLinker()) {
      if (kind.isReadOnly())
        return constTextCoalSection;
      return dataCoalSection;
    }

    if (kind.isMergeable1ByteCString() &&
        tm.getTargetData().getPreferredAlignment((GlobalVariable) gv) < 32)
      return cstringSection;

    if (kind.isMergeable2ByteCString() && !gv.hasExternalLinkage() &&
        tm.getTargetData().getPreferredAlignment((GlobalVariable)gv) < 32)
      return ustringSection;

    if (kind.isMergeableConst()) {
      if (kind.isMergeableConst4())
        return fourByteConstantSection;
      if (kind.isMergeableConst8())
        return eightByteConstantSection;
      if (kind.isMergeableConst16() && sixteenByteConstantSection != null)
        return sixteenByteConstantSection;
    }

    if (kind.isReadOnly())
      return ReadOnlySection;

    if (kind.isReadOnlyWithRel())
      return constDataSection;

    if (kind.isBSSExtern())
      return dataCommonSection;

    if (kind.isBSSLocal())
      return dataBSSSection;

    return DataSection;
  }

  @Override
  public MCSection getExplicitSectionGlobal(GlobalValue gv,
                                            SectionKind kind,
                                            NameMangler mangler,
                                            TargetMachine tm) {
    OutRef<String> segment = new OutRef<>(), section = new OutRef<>();
    OutRef<Integer> taa = new OutRef<>(), stubSize = new OutRef<>();
    String errorCode = MCSectionMachO.parseSectionSpecifier(gv.getSection(),
        segment, section, taa, stubSize);
    if (!errorCode.isEmpty()) {
      reportFatalError(String.format("Global variable '%s' has an invalid section specifier '%s': %s.",
          gv.getName(), gv.getSection(), errorCode));
      return DataSection;
    }
    MCSectionMachO sec = getContext().getMachOSection(segment.get(),
        section.get(), taa.get(), stubSize.get(), kind);
    if (sec.getTypeAndAttributes() != taa.get() || sec.getStubSize() != stubSize.get()) {
      reportFatalError(String.format("Global variable '%s' section type or attributes doesn't match previous section specifier",
          gv.getName()));
    }
    return sec;
  }

  public MCSection getSectionForConstant(SectionKind kind) {
    if (kind.isDataRel() || kind.isReadOnlyWithRel())
      return constDataSection;

    if (kind.isMergeableConst4())
      return fourByteConstantSection;
    if (kind.isMergeableConst8())
      return eightByteConstantSection;
    if (kind.isMergeableConst16() && sixteenByteConstantSection != null)
      return sixteenByteConstantSection;
    return ReadOnlySection;
  }

  /**
   * This hook allows targets to selectively decide not to emit the UsedDirective
   * for some symbols in llvm.used.
   * @param gv
   * @param mangler
   * @return
   */
  public boolean shouldEmitUsedDirectiveFor(GlobalValue gv,
                                            NameMangler mangler) {
    if (gv == null) return false;
    if (gv.hasLocalLinkage() && !(gv instanceof Function)) {
      MCSymbol sym = mangler.getSymbol(gv);
      if (sym.getName().charAt(0) == 'L' || sym.getName().charAt(0) == 'l')
        return false;
    }
    return true;
  }

  /**
   * Return the "__TEXT,__textcoal_nt" section we put weak text symbols into.
   * @return
   */
  public MCSection getTextCoalSection() { return textCoalSection; }

  /**
   * Return the "__TEXT,__const_coal" section we put weak read-only symbols into.
   * @return
   */
  public MCSection getConstTextCoalSection() { return constTextCoalSection; }

  /**
   * Return the section corresponding to the ".lazy_symbol_pointer" directive.
   * @return
   */
  public MCSection getLazySymbolPointerSection() { return lazySymbolPointerSection; }

  /**
   * Return the section corresponding to the ".non_lazy_symbol_pointer" directive.
   * @return
   */
  public MCSection getNonLazySymbolPointerSection() { return nonLazySymbolPointerSection; }

  public MCSection getConstDataSection() { return constDataSection; }

  public MCSection getDataCoalSection() { return dataCoalSection; }

  public MCSection getDataCommonSection() { return dataCommonSection; }

  public MCSection getDataBSSSection() { return dataBSSSection; }

  public MCSection getFourByteConstantSection() { return fourByteConstantSection; }

  public MCSection getEightByteConstantSection() { return eightByteConstantSection; }

  public MCSection getSixteenByteConstantSection() { return sixteenByteConstantSection; }

  public MCSection getCStringSection() { return cstringSection; }

  public MCSection getUStringSection() { return ustringSection; }

  public MCSection getTLSBSSSection() { return tlsBSSSection; }

  public MCSection getTLSDataSection() { return tlsDataSection; }

  public MCSection getTLSTLVSection() { return tlsTLVSection; }

  public MCExpr getExprForDwarfGlobalReference(GlobalValue gv,
                                               NameMangler mangler,
                                               MachineModuleInfo mmi,
                                               int encoding,
                                               MCStreamer streamer) {
      Util.shouldNotReachHere();
      return null;
  }

  public int getPersonalityEncoding() {
    return Dwarf.DW_EH_PE_indirect | Dwarf.DW_EH_PE_pcrel | Dwarf.DW_EH_PE_sdata4;
  }
  public int getLSDAEncoding() { return Dwarf.DW_EH_PE_pcrel; }
  public int getFDEEncoding() { return Dwarf.DW_EH_PE_pcrel; }
  public int getTTypeEncoding() {
    return Dwarf.DW_EH_PE_indirect | Dwarf.DW_EH_PE_pcrel | Dwarf.DW_EH_PE_sdata4;
  }
}
