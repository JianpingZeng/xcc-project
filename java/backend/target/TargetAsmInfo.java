package backend.target;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import backend.codegen.AsmWriterFlavorTy;
import backend.support.LLVMContext;
import backend.type.ArrayType;
import backend.value.*;
import tools.Util;

import java.util.TreeMap;

import static backend.target.SectionKind.isBSS;
import static backend.target.SectionKind.isReadOnly;
import static backend.target.TargetOptions.DontPlaceZerosInBSS;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class TargetAsmInfo {
  protected static String[] x86_asm_table = {
      "{si}", "S",
      "{di}", "D",
      "{ax}", "a",
      "{cx}", "c",
      "{memory}", "memory",
      "{flags}", "",
      "{dirflag}", "",
      "{fpsr}", "",
      "{cc}", "cc",
  };
  /// textSection - Section directive for standard text.
  ///
  protected Section textSection = new Section(".text");           // Defaults to ".text".

  /// dataSection - Section directive for standard data.
  ///
  protected Section dataSection = new Section(".data");           // Defaults to ".data".

  /// bssSection - Section directive for uninitialized data.  Null if this
  /// target doesn't support a BSS section.
  ///
  protected String bssSection = ".bss";               // Default to ".bss".
  protected Section bssSection_ = new Section(".bss");

  /// readOnlySection - This is the directive that is emitted to switch to a
  /// read-only section for constant data (e.g. data declared const,
  /// jump tables).
  protected Section readOnlySection;       // Defaults to NULL

  /// smallDataSection - This is the directive that is emitted to switch to a
  /// small data section.
  ///
  protected Section smallDataSection;      // Defaults to NULL

  /// smallBSSSection - This is the directive that is emitted to switch to a
  /// small bss section.
  ///
  protected Section smallBSSSection;       // Defaults to NULL

  /// smallRODataSection - This is the directive that is emitted to switch to
  /// a small read-only data section.
  ///
  protected Section smallRODataSection;    // Defaults to NULL

  /// tlsDataSection - Section directive for Thread Local data.
  ///
  protected Section tlsDataSection = new Section(".tdata");        // Defaults to ".tdata".

  /// tlsBSSSection - Section directive for Thread Local uninitialized data.
  /// Null if this target doesn't support a BSS section.
  ///
  protected Section tlsBSSSection = new Section(".tbss");         // Defaults to ".tbss".
  //===------------------------------------------------------------------===//
  // Properties to be set by the target writer, used to configure asm printer.
  //

  /// zeroFillDirective - Directive for emitting a global to the ZeroFill
  /// section on this target.  null if this target doesn't support zerofill.
  protected String zeroFillDirective;        // Default is null.

  /// nonexecutableStackDirective - Directive for declaring to the
  /// linker and beyond that the emitted code does not require stack
  /// memory to be executable.
  protected String nonexecutableStackDirective; // Default is null.

  /// needsSet - True if target asm treats expressions in data directives
  /// as linktime-relocatable.  For assembly-time computation, we need to
  /// use a .set.  Thus:
  /// .set w, x-y
  /// .long w
  /// is computed at assembly time, while
  /// .long x-y
  /// is relocated if the relative locations of x and y change at linktime.
  /// We want both these things in different places.
  protected boolean needsSet;                        // Defaults to false.

  /// maxInstLength - This is the maximum possible length of an instruction,
  /// which is needed to compute the size of an inline asm.
  protected int maxInstLength;               // Defaults to 4.

  /// pcSymbol - The symbol used to represent the current PC.  Used in PC
  /// relative expressions.
  protected String pcSymbol;                 // Defaults to "$".

  /// SeparatorChar - This character, if specified, is used to separate
  /// instructions from each other when on the same line.  This is used to
  /// measure inline asm instructions.
  protected char SeparatorChar;                   // Defaults to ';'

  /// CommentColumn - This indicates the comment num (zero-based) at
  /// which asm comments should be printed.
  protected int CommentColumn;               // Defaults to 60

  /// CommentString - This indicates the comment character used by the
  /// assembler.
  protected String CommentString;            // Defaults to "#"

  /// GlobalPrefix - If this is set to a non-empty string, it is prepended
  /// onto all global symbols.  This is often used for "_" or ".".
  protected String GlobalPrefix;             // Defaults to ""

  /// privateGlobalPrefix - This prefix is used for globals like constant
  /// pool entries that are completely private to the .s file and should not
  /// have names in the .o file.  This is often "." or "L".
  protected String privateGlobalPrefix;      // Defaults to "."

  /// LinkerPrivateGlobalPrefix - This prefix is used for symbols that should
  /// be passed through the assembler but be removed by the linker.  This
  /// is "l" on Darwin, currently used for some ObjC metadata.
  protected String LinkerPrivateGlobalPrefix;      // Defaults to ""

  /// JumpTableSpecialLabelPrefix - If not null, a extra (dead) label is
  /// emitted before jump tables with the specified prefix.
  protected String JumpTableSpecialLabelPrefix;  // Default to null.

  /// GlobalVarAddrPrefix/Suffix - If these are nonempty, these strings
  /// will enclose any GlobalVariable (that isn't a function)
  ///
  protected String GlobalVarAddrPrefix;      // Defaults to ""
  protected String GlobalVarAddrSuffix;      // Defaults to ""

  /// FunctionAddrPrefix/Suffix - If these are nonempty, these strings
  /// will enclose any GlobalVariable that points to a function.
  ///
  protected String FunctionAddrPrefix;       // Defaults to ""
  protected String FunctionAddrSuffix;       // Defaults to ""

  /// PersonalityPrefix/Suffix - If these are nonempty, these strings will
  /// enclose any personality function in the common frame section.
  ///
  protected String PersonalityPrefix;        // Defaults to ""
  protected String PersonalitySuffix;        // Defaults to ""

  /// NeedsIndirectEncoding - If set, we need to set the indirect encoding bit
  /// for EH in Dwarf.
  ///
  protected boolean NeedsIndirectEncoding;           // Defaults to false

  /// InlineAsmStart/End - If these are nonempty, they contain a directive to
  /// emit before and after an inline assembly statement.
  protected String InlineAsmStart;           // Defaults to "#APP\n"
  protected String InlineAsmEnd;             // Defaults to "#NO_APP\n"

  /// assemblerDialect - Which dialect of an assembler variant to use.
  protected AsmWriterFlavorTy assemblerDialect;            // Defaults to AT&T

  /// AllowQuotesInName - This is true if the assembler allows for complex
  /// symbol names to be surrounded in quotes.  This defaults to false.
  protected boolean AllowQuotesInName;

  //===--- Data Emission Directives -------------------------------------===//

  /// ZeroDirective - this should be set to the directive used to get some
  /// number of zero bytes emitted to the current section.  Common cases are
  /// "\t.zero\t" and "\t.space\t".  If this is set to null, the
  /// Data*bitsDirective's will be used to emit zero bytes.
  protected String ZeroDirective;            // Defaults to "\t.zero\t"
  protected String ZeroDirectiveSuffix;      // Defaults to ""

  /// AsciiDirective - This directive allows emission of an ascii string with
  /// the standard C escape characters embedded into it.
  protected String AsciiDirective;           // Defaults to "\t.ascii\t"

  /// AscizDirective - If not null, this allows for special handling of
  /// zero terminated strings on this target.  This is commonly supported as
  /// ".asciz".  If a target doesn't support this, it can be set to null.
  protected String AscizDirective;           // Defaults to "\t.asciz\t"

  /// DataDirectives - These directives are used to output some unit of
  /// integer data to the current section.  If a data directive is set to
  /// null, smaller data directives will be used to emit the large sizes.
  protected String Data8bitsDirective;       // Defaults to "\t.byte\t"
  protected String Data16bitsDirective;      // Defaults to "\t.short\t"
  protected String Data32bitsDirective;      // Defaults to "\t.long\t"
  protected String Data64bitsDirective;      // Defaults to "\t.quad\t"

  /**
   * If this is true (the default) then the asmprinter
   * emits ".align N" directives, where N is the number of bytes to align to.
   * Otherwise, it emits ".align log2(N)", e.g. 3 to align to an 8 byte
   * boundary.
   * Default to true.
   */
  protected boolean alignIsInByte;
  /**
   * Default to "\t.section\t".
   */
  protected String switchToSectionDirective;
  /**
   * This is the section that we SwitchToSection right
   * before emitting the constant pool for a function.
   * <p>
   * Default to "\t.section .rodata\n".
   */
  protected String constantPoolSection = ".section .rodata";

  protected String textSectionStartSuffix;

  protected String dataSectionStartSuffix;

  protected String sectionEndDirectiveSuffix;

  private TreeMap<String, Section> sections = new TreeMap<>();

  /// getDataASDirective - Return the directive that should be used to emit
  /// data of the specified size to the specified numeric address space.
  protected String getDataASDirective(int Size, int AS) {
    Util.assertion(AS != 0, "Don't know the directives for default addr space");
    return null;
  }

  /// SunStyleELFSectionSwitchSyntax - This is true if this target uses "Sun
  /// Style" syntax for section switching ("#alloc,#write" etc) instead of the
  /// normal ELF syntax (,"a,w") in .section directives.
  protected boolean SunStyleELFSectionSwitchSyntax;   // Defaults to false.

  /// UsesELFSectionDirectiveForBSS - This is true if this target uses ELF
  /// '.section' directive before the '.bss' one. It's used for PPC/Linux
  /// which doesn't support the '.bss' directive only.
  protected boolean UsesELFSectionDirectiveForBSS;  // Defaults to false.

  //===--- Alignment Information ----------------------------------------===//

  /// AlignDirective - The directive used to emit round up to an alignment
  /// boundary.
  ///
  protected String AlignDirective;           // Defaults to "\t.align\t"

  /// AlignmentIsInBytes - If this is true (the default) then the asmprinter
  /// emits ".align N" directives, where N is the number of bytes to align to.
  /// Otherwise, it emits ".align log2(N)", e.g. 3 to align to an 8 byte
  /// boundary.
  protected boolean AlignmentIsInBytes;              // Defaults to true

  /// TextAlignFillValue - If non-zero, this is used to fill the executable
  /// space created as the result of a alignment directive.
  protected int TextAlignFillValue;

  //===--- Section Switching Directives ---------------------------------===//

  /// JumpTableDirective - if non-null, the directive to emit before jump
  /// table entries.  FIXME: REMOVE THIS.
  protected String JumpTableDirective;
  protected String PICJumpTableDirective;

  //===--- Global Variable Emission Directives --------------------------===//

  /// GlobalDirective - This is the directive used to declare a global entity.
  ///
  protected String GlobalDirective;          // Defaults to null.

  /// ExternDirective - This is the directive used to declare external
  /// globals.
  ///
  protected String ExternDirective;          // Defaults to null.

  /// setDirective - This is the name of a directive that can be used to tell
  /// the assembler to set the value of a variable to some expression.
  protected String setDirective;             // Defaults to null.

  /// LCOMMDirective - This is the name of a directive (if supported) that can
  /// be used to efficiently declare a local (internal) block of zero
  /// initialized data in the .bss/.data section.  The syntax expected is:
  /// @verbatim <LCOMMDirective> SYMBOLNAME LENGTHINBYTES, ALIGNMENT
  /// @endverbatim
  protected String LCOMMDirective;           // Defaults to null.

  protected String COMMDirective;            // Defaults to "\t.comm\t".

  /// COMMDirectiveTakesAlignment - True if COMMDirective take a third
  /// argument that specifies the alignment of the declaration.
  protected boolean COMMDirectiveTakesAlignment;     // Defaults to true.

  /// HasDotTypeDotSizeDirective - True if the target has .type and .size
  /// directives, this is true for most ELF targets.
  protected boolean HasDotTypeDotSizeDirective;      // Defaults to true.

  /// HasSingleParameterDotFile - True if the target has a single parameter
  /// .file directive, this is true for ELF targets.
  protected boolean HasSingleParameterDotFile;      // Defaults to true.

  /// UsedDirective - This directive, if non-null, is used to declare a global
  /// as being used somehow that the assembler can't see.  This prevents dead
  /// code elimination on some targets.
  protected String UsedDirective;            // Defaults to null.

  /// weakRefDirective - This directive, if non-null, is used to declare a
  /// global as being a weak undefined symbol.
  protected String weakRefDirective;         // Defaults to null.

  /// WeakDefDirective - This directive, if non-null, is used to declare a
  /// global as being a weak defined symbol.
  protected String WeakDefDirective;         // Defaults to null.

  /// HiddenDirective - This directive, if non-null, is used to declare a
  /// global or function as having hidden visibility.
  protected String HiddenDirective;          // Defaults to "\t.hidden\t".

  /// ProtectedDirective - This directive, if non-null, is used to declare a
  /// global or function as having protected visibility.
  protected String ProtectedDirective;       // Defaults to "\t.protected\t".

  //===--- Dwarf Emission Directives -----------------------------------===//

  /// absoluteDebugSectionOffsets - True if we should emit abolute section
  /// offsets for debug information. Defaults to false.
  protected boolean absoluteDebugSectionOffsets;

  /// absoluteEHSectionOffsets - True if we should emit abolute section
  /// offsets for EH information. Defaults to false.
  protected boolean absoluteEHSectionOffsets;

  /// hasLEB128 - True if target asm supports leb128 directives.
  ///
  protected boolean hasLEB128; // Defaults to false.

  /// hasDotLocAndDotFile - True if target asm supports .loc and .file
  /// directives for emitting debugging information.
  ///
  protected boolean HasDotLocAndDotFile; // Defaults to false.

  /// supportsDebugInformation - True if target supports emission of debugging
  /// information.
  protected boolean supportsDebugInformation;

  /// SupportsExceptionHandling - True if target supports
  /// exception handling.
  ///
  // Defaults to None
  // ExceptionHandling::ExceptionsType ExceptionsType;

  /// RequiresFrameSection - true if the Dwarf2 output needs a frame section
  ///
  protected boolean DwarfRequiresFrameSection; // Defaults to true.

  /// DwarfUsesInlineInfoSection - True if DwarfDebugInlineSection is used to
  /// encode inline subroutine information.
  protected boolean DwarfUsesInlineInfoSection; // Defaults to false.

  /// Is_EHSymbolPrivate - If set, the "_foo.eh" is made private so that it
  /// doesn't show up in the symbol table of the object file.
  protected boolean Is_EHSymbolPrivate;                // Defaults to true.

  /// GlobalEHDirective - This is the directive used to make exception frame
  /// tables globally visible.
  ///
  protected String GlobalEHDirective;          // Defaults to null.

  /// SupportsWeakEmptyEHFrame - True if target assembler and linker will
  /// handle a weak_definition of constant 0 for an omitted EH frame.
  protected boolean SupportsWeakOmittedEHFrame;  // Defaults to true.

  /// DwarfSectionOffsetDirective - Special section offset directive.
  protected String DwarfSectionOffsetDirective; // Defaults to null

  //===--- CBE Asm Translation Table -----------------------------------===//

  protected String[] asmTransCBE; // Defaults to empty

  public TargetAsmInfo() {
    zeroFillDirective = null;
    nonexecutableStackDirective = null;
    needsSet = false;
    maxInstLength = 4;
    pcSymbol = "$";
    SeparatorChar = ';';
    CommentColumn = 60;
    CommentString = "#";
    GlobalPrefix = "";
    privateGlobalPrefix = ".";
    LinkerPrivateGlobalPrefix = "";
    JumpTableSpecialLabelPrefix = null;
    GlobalVarAddrPrefix = "";
    GlobalVarAddrSuffix = "";
    FunctionAddrPrefix = "";
    FunctionAddrSuffix = "";
    PersonalityPrefix = "";
    PersonalitySuffix = "";
    NeedsIndirectEncoding = false;
    InlineAsmStart = "APP";
    InlineAsmEnd = "NO_APP";
    assemblerDialect = AsmWriterFlavorTy.ATT;
    AllowQuotesInName = false;
    ZeroDirective = "\t.zero\t";
    ZeroDirectiveSuffix = null;
    AsciiDirective = "\t.ascii\t";
    AscizDirective = "\t.asciz\t";
    Data8bitsDirective = "\t.byte\t";
    Data16bitsDirective = "\t.short\t";
    Data32bitsDirective = "\t.long\t";
    Data64bitsDirective = "\t.quad\t";
    AlignmentIsInBytes = true;
    switchToSectionDirective = "\t.section\t";
    SunStyleELFSectionSwitchSyntax = false;
    UsesELFSectionDirectiveForBSS = false;
    AlignDirective = "\t.align\t";
    AlignmentIsInBytes = true;
    TextAlignFillValue = 0;
    JumpTableDirective = null;
    PICJumpTableDirective = null;
    GlobalDirective = "\t.globl\t";
    setDirective = null;
    LCOMMDirective = null;
    COMMDirective = "\t.comm\t";
    COMMDirectiveTakesAlignment = true;
    HasDotTypeDotSizeDirective = true;
    HasSingleParameterDotFile = true;
    UsedDirective = null;
    weakRefDirective = null;
    WeakDefDirective = null;
    // FIXME: These are ELFish - move to ELFTAI.
    HiddenDirective = "\t.hidden\t";
    ProtectedDirective = "\t.protected\t";
    absoluteDebugSectionOffsets = false;
    absoluteEHSectionOffsets = false;
    hasLEB128 = false;
    HasDotLocAndDotFile = false;
    supportsDebugInformation = false;
    //ExceptionsType = ExceptionHandling::None;
    DwarfRequiresFrameSection = true;
    DwarfUsesInlineInfoSection = false;
    Is_EHSymbolPrivate = true;
    GlobalEHDirective = null;
    SupportsWeakOmittedEHFrame = true;
    DwarfSectionOffsetDirective = null;

    asmTransCBE = null;
  }

  /// getSLEB128Size - Compute the number of bytes required for a signed
  /// leb128 value.
  public static int getSLEB128Size(int Value) {
    int size = 0;
    do {
      Value >>>= 7;
      size += 1;
    } while (Value != 0);
    return size;
  }

  /// getULEB128Size - Compute the number of bytes required for an public int
  /// leb128 value.
  static public int getULEB128Size(int Value) {
    int size = 0;
    int sign = Value >> (8 * 32 - 1);
    boolean isMove;
    do {
      int _byte = Value & 0x7f;
      Value >>= 7;
      isMove = Value != sign || ((_byte ^ sign) & 0x40) != 0;
      size += 1;
    } while (isMove);
    return size;
  }

  public String getData8bitsDirective() {
    return getData8bitsDirective(0);
  }

  // Data directive accessors.
  //
  public String getData8bitsDirective(int AS) {
    return AS == 0 ? Data8bitsDirective : getDataASDirective(8, AS);
  }

  public String getData16bitsDirective() {
    return getData16bitsDirective(0);
  }

  public String getData16bitsDirective(int AS) {
    return AS == 0 ? Data16bitsDirective : getDataASDirective(16, AS);
  }

  public String getData32bitsDirective() {
    return getData32bitsDirective(0);
  }

  public String getData32bitsDirective(int AS) {
    return AS == 0 ? Data32bitsDirective : getDataASDirective(32, AS);
  }

  public String getData64bitsDirective() {
    return getData64bitsDirective(0);
  }

  public String getData64bitsDirective(int AS) {
    return AS == 0 ? Data64bitsDirective : getDataASDirective(64, AS);
  }

  public boolean usesSunStyleELFSectionSwitchSyntax() {
    return SunStyleELFSectionSwitchSyntax;
  }

  public boolean usesELFSectionDirectiveForBSS() {
    return UsesELFSectionDirectiveForBSS;
  }

  // Accessors.
  //
  public String getZeroFillDirective() {
    return zeroFillDirective;
  }

  public String getNonexecutableStackDirective() {
    return nonexecutableStackDirective;
  }

  public boolean needsSet() {
    return needsSet;
  }

  public int getMaxInstLength() {
    return maxInstLength;
  }

  public String getPCSymbol() {
    return pcSymbol;
  }

  public char getSeparatorChar() {
    return SeparatorChar;
  }

  public int getCommentColumn() {
    return CommentColumn;
  }

  public String getCommentString() {
    return CommentString;
  }

  public String getGlobalPrefix() {
    return GlobalPrefix;
  }

  public String getPrivateGlobalPrefix() {
    return privateGlobalPrefix;
  }

  public String getLinkerPrivateGlobalPrefix() {
    return LinkerPrivateGlobalPrefix;
  }

  public String getJumpTableSpecialLabelPrefix() {
    return JumpTableSpecialLabelPrefix;
  }

  public String getGlobalVarAddrPrefix() {
    return GlobalVarAddrPrefix;
  }

  public String getGlobalVarAddrSuffix() {
    return GlobalVarAddrSuffix;
  }

  public String getFunctionAddrPrefix() {
    return FunctionAddrPrefix;
  }

  public String getFunctionAddrSuffix() {
    return FunctionAddrSuffix;
  }

  public String getPersonalityPrefix() {
    return PersonalityPrefix;
  }

  public String getPersonalitySuffix() {
    return PersonalitySuffix;
  }

  public boolean getNeedsIndirectEncoding() {
    return NeedsIndirectEncoding;
  }

  public String getInlineAsmStart() {
    return InlineAsmStart;
  }

  public String getInlineAsmEnd() {
    return InlineAsmEnd;
  }

  public AsmWriterFlavorTy getAssemblerDialect() {
    return assemblerDialect;
  }

  public boolean doesAllowQuotesInName() {
    return AllowQuotesInName;
  }

  public String getZeroDirective() {
    return ZeroDirective;
  }

  public String getZeroDirectiveSuffix() {
    return ZeroDirectiveSuffix;
  }

  public String getAsciiDirective() {
    return AsciiDirective;
  }

  public String getAscizDirective() {
    return AscizDirective;
  }

  public String getJumpTableDirective(boolean isPIC) {
    return isPIC ? PICJumpTableDirective : JumpTableDirective;
  }

  public String getAlignDirective() {
    return AlignDirective;
  }

  public boolean getAlignmentIsInBytes() {
    return AlignmentIsInBytes;
  }

  public int getTextAlignFillValue() {
    return TextAlignFillValue;
  }

  public String getGlobalDirective() {
    return GlobalDirective;
  }

  public String getExternDirective() {
    return ExternDirective;
  }

  public String getSetDirective() {
    return setDirective;
  }

  public String getLCOMMDirective() {
    return LCOMMDirective;
  }

  public String getCOMMDirective() {
    return COMMDirective;
  }

  public boolean getCOMMDirectiveTakesAlignment() {
    return COMMDirectiveTakesAlignment;
  }

  public boolean hasDotTypeDotSizeDirective() {
    return HasDotTypeDotSizeDirective;
  }

  public boolean hasSingleParameterDotFile() {
    return HasSingleParameterDotFile;
  }

  public String getUsedDirective() {
    return UsedDirective;
  }

  public String getWeakRefDirective() {
    return weakRefDirective;
  }

  public String getWeakDefDirective() {
    return WeakDefDirective;
  }

  public String getHiddenDirective() {
    return HiddenDirective;
  }

  public String getProtectedDirective() {
    return ProtectedDirective;
  }

  public boolean isAbsoluteDebugSectionOffsets() {
    return absoluteDebugSectionOffsets;
  }

  public boolean isAbsoluteEHSectionOffsets() {
    return absoluteEHSectionOffsets;
  }

  public boolean hasLEB128() {
    return hasLEB128;
  }

  public boolean hasDotLocAndDotFile() {
    return HasDotLocAndDotFile;
  }

  public boolean doesSupportDebugInformation() {
    return supportsDebugInformation;
  }

  public boolean doesDwarfRequireFrameSection() {
    return DwarfRequiresFrameSection;
  }

  public boolean doesDwarfUsesInlineInfoSection() {
    return DwarfUsesInlineInfoSection;
  }

  public boolean is_EHSymbolPrivate() {
    return Is_EHSymbolPrivate;
  }

  public String getGlobalEHDirective() {
    return GlobalEHDirective;
  }

  public boolean getSupportsWeakOmittedEHFrame() {
    return SupportsWeakOmittedEHFrame;
  }

  public String getDwarfSectionOffsetDirective() {
    return DwarfSectionOffsetDirective;
  }

  public String[] getAsmCBE() {
    return asmTransCBE;
  }

  public boolean isAlignIsInByte() {
    return alignIsInByte;
  }

  public String getSwitchToSectionDirective() {
    return switchToSectionDirective;
  }

  public String getConstantPoolSectionDirective() {
    return constantPoolSection;
  }

  public Section getSectionForGlobal(GlobalValue gv) {
    Section s = null;
    if (gv.hasSection()) {
      int flags = sectionFlagsForGlobal(gv, gv.getSection());
      s = getNamedSection(gv.getSection(), flags);
    } else {
      // Use default section depending on the 'type' of global
      s = selectSectionForGlobal(gv);
    }

    return s;
  }

  private Section getNamedSection(String name, int flags) {
    if (sections.containsKey(name))
      return sections.get(name);

    Section s = new Section(name, flags | SectionFlags.Named);
    sections.put(name, s);
    return s;
  }

  public int sectionFlagsForGlobal(GlobalValue gv, String name) {
    int flags = SectionFlags.None;
    if (gv != null) {
      SectionKind kind = sectionKindForGlobal(gv);
      switch (kind) {
        case Text:
          flags |= SectionFlags.Code;
          break;
        case ThreadData:
        case ThreadBSS:
          flags |= SectionFlags.TLS;
          // FALLS THROUGH
        case Data:
        case BSS:
          flags |= SectionFlags.Writeable;
          break;
        case ROData:
        case RODataMergeStr:
        case RODataMergeConst:
          // No additional flags here
          break;
        case SmallData:
        case SmallBSS:
          flags |= SectionFlags.Writeable;
          // FALLS THROUGH
        case SmallROData:
          flags |= SectionFlags.Small;
          break;
        default:
          Util.assertion(false, "Unexpected section kind!");
      }
    }

    if (name != null && !name.isEmpty()) {
      flags |= SectionFlags.Named;

      // Some lame default implementation based on some magic section names.
      if (name.regionMatches(0, ".gnu.linkonce.b.", 0, 16) ||
          name.regionMatches(0, ".llvm.linkonce.b.", 0, 17) ||
          name.regionMatches(0, ".gnu.linkonce.sb.", 0, 17) ||
          name.regionMatches(0, ".llvm.linkonce.sb.", 0, 18))
        flags |= SectionFlags.BSS;
      else if (name.equals(".tdata") ||
          name.regionMatches(0, ".tdata.", 0, 7) ||
          name.regionMatches(0, ".gnu.linkonce.td.", 0, 17) ||
          name.regionMatches(0, ".llvm.linkonce.td.", 0, 18))
        flags |= SectionFlags.TLS;
      else if (name.equals(".tbss") ||
          name.regionMatches(0, ".tbss.", 0, 6) ||
          name.regionMatches(0, ".gnu.linkonce.tb.", 0, 17) ||
          name.regionMatches(0, ".llvm.linkonce.tb.", 0, 18))
        flags |= SectionFlags.BSS | SectionFlags.TLS;
    }
    return flags;
  }

  public Section selectSectionForGlobal(GlobalValue gv) {
    SectionKind kind = sectionKindForGlobal(gv);
    if (kind == SectionKind.Text)
      return getTextSection();
    else if (isBSS(kind) && getBssSection_() != null) {
      return getBssSection_();
    } else if (getReadOnlySection() != null && isReadOnly(kind))
      return getReadOnlySection();

    return getDataSection();
  }

  public Section getBssSection_() {
    return bssSection_;
  }

  public Section getDataSection() {
    return dataSection;
  }

  public Section getTextSection() {
    return textSection;
  }

  public String getBSSSection() {
    return bssSection;
  }

  public Section getSmallBSSSection() {
    return smallBSSSection;
  }

  public Section getSmallDataSection() {
    return smallDataSection;
  }

  public Section getReadOnlySection() {
    return readOnlySection;
  }

  public Section getSmallRODataSection() {
    return smallRODataSection;
  }

  public Section getTlsBSSSection() {
    return tlsBSSSection;
  }

  public Section getTlsDataSection() {
    return tlsDataSection;
  }

  public SectionKind sectionKindForGlobal(GlobalValue gv) {
    if (gv instanceof Function)
      return SectionKind.Text;

    GlobalVariable gvar = gv instanceof GlobalVariable ? (GlobalVariable) gv : null;
    Util.assertion(gvar != null);
    boolean isThreadLocal = gvar.isThreadLocal();

    if (isSuitableForBSS(gvar)) {
      return isThreadLocal ? SectionKind.ThreadBSS : SectionKind.BSS;
    } else if (gvar.isConstant() && !isThreadLocal) {
      Constant c = gvar.getInitializer();
      if (c.containsRelocations())
        return SectionKind.ROData;
      else {
        if (isConstantString(c))
          return SectionKind.RODataMergeStr;
        else
          return SectionKind.RODataMergeConst;
      }
    }
    return isThreadLocal ? SectionKind.ThreadData : SectionKind.Data;
  }

  private static boolean isConstantString(Constant c) {
    ConstantArray cva = c instanceof ConstantArray ? (ConstantArray) c : null;
    if (cva != null && cva.isString())
      return true;

    // Another possibility: [1 x i8] zeroinitializer
    if (c instanceof ConstantAggregateZero) {
      ArrayType at = c.getType() instanceof ArrayType ? (ArrayType) c.getType() : null;
      if (at != null) {
        return at.getElementType().equals(LLVMContext.Int8Ty) &&
            at.getNumElements() == 1;
      }
    }
    return false;
  }

  private static boolean isSuitableForBSS(GlobalVariable gvar) {
    if (!gvar.hasInitializer())
      return true;

    Constant c = gvar.getInitializer();
    return c.isNullValue() && !gvar.isConstant() && !DontPlaceZerosInBSS.value;
  }

  public String getSectionEndDirectiveSuffix() {
    return sectionEndDirectiveSuffix;
  }

  public String getSectionFlags(int flags) {
    // TODO: 17-8-2
    return "";
  }

  public String getDataSectionStartSuffix() {
    return dataSectionStartSuffix;
  }
}
