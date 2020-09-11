/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.mc;

import backend.codegen.AsmWriterFlavorTy;
import tools.Util;

import static backend.mc.MCAsmInfo.MCSymbolAttr.MCSA_Hidden;
import static backend.mc.MCAsmInfo.MCSymbolAttr.MCSA_Protected;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class MCAsmInfo {
    public enum ExceptionHandlingType {
        None, Dwarf, SjLj, ARM
    }

    public enum MCSymbolAttr {
        MCSA_Invalid,    ///< Not a valid directive.

        // Various directives in alphabetical order.
        MCSA_ELF_TypeFunction,    ///< .type _foo, STT_FUNC  # aka @function
        MCSA_ELF_TypeIndFunction, ///< .type _foo, STT_GNU_IFUNC
        MCSA_ELF_TypeObject,      ///< .type _foo, STT_OBJECT  # aka @object
        MCSA_ELF_TypeTLS,         ///< .type _foo, STT_TLS     # aka @tls_object
        MCSA_ELF_TypeCommon,      ///< .type _foo, STT_COMMON  # aka @common
        MCSA_ELF_TypeNoType,      ///< .type _foo, STT_NOTYPE  # aka @notype
        MCSA_Global,              ///< .globl
        MCSA_Hidden,              ///< .hidden (ELF)
        MCSA_IndirectSymbol,      ///< .indirect_symbol (MachO)
        MCSA_Internal,            ///< .internal (ELF)
        MCSA_LazyReference,       ///< .lazy_reference (MachO)
        MCSA_Local,               ///< .local (ELF)
        MCSA_NoDeadStrip,         ///< .no_dead_strip (MachO)
        MCSA_PrivateExtern,       ///< .private_extern (MachO)
        MCSA_Protected,           ///< .protected (ELF)
        MCSA_Reference,           ///< .reference (MachO)
        MCSA_Weak,                ///< .weak
        MCSA_WeakDefinition,      ///< .weak_definition (MachO)
        MCSA_WeakReference        ///< .weak_reference (MachO)
    }

    public enum MCAssemblerFlag {
        MCAF_SyntaxUnified,         ///< .syntax (ARM/ELF)
        MCAF_SubsectionsViaSymbols, ///< .subsections_via_symbols (MachO)
        MCAF_Code16,                ///< .code16 (X86) / .code 16 (ARM)
        MCAF_Code32,                ///< .code32 (X86) / .code 32 (ARM)
        MCAF_Code64                 ///< .code64 (X86)
    }
    //===------------------------------------------------------------------===//
    // Properties to be set by the target writer, used to configure asm printer.
    //

    /// HasSubsectionsViaSymbols - True if this target has the MachO
    /// .subsections_via_symbols directive.
    protected boolean HasSubsectionsViaSymbols;           // Default is false.

    /// HasMachoZeroFillDirective - True if this is a MachO target that supports
    /// the macho-specific .zerofill directive for emitting BSS Symbols.
    protected boolean HasMachoZeroFillDirective;               // Default is false.

    /// HasStaticCtorDtorReferenceInStaticMode - True if the compiler should
    /// emit a ".reference .constructors_used" or ".reference .destructors_used"
    /// directive after the a static ctor/dtor list.  This directive is only
    /// emitted in Static relocation model.
    protected boolean HasStaticCtorDtorReferenceInStaticMode;  // Default is false.

    /// MaxInstLength - This is the maximum possible length of an instruction,
    /// which is needed to compute the size of an inline asm.
    protected int MaxInstLength;                  // Defaults to 4.

    /// PCSymbol - The symbol used to represent the current PC.  Used in PC
    /// relative expressions.
    protected String PCSymbol;                    // Defaults to "$".

    /// SeparatorChar - This character, if specified, is used to separate
    /// instructions from each other when on the same line.  This is used to
    /// measure inline asm instructions.
    protected String SeparatorChar;                      // Defaults to ';'

    /// CommentColumn - This indicates the comment num (zero-based) at
    /// which asm comments should be printed.
    protected int CommentColumn;                  // Defaults to 40

    /// CommentString - This indicates the comment character used by the
    /// assembler.
    protected String CommentString;               // Defaults to "#"

    /// GlobalPrefix - If this is set to a non-empty string, it is prepended
    /// onto all global symbols.  This is often used for "_" or ".".
    protected String GlobalPrefix;                // Defaults to ""

    /// PrivateGlobalPrefix - This prefix is used for globals like constant
    /// pool entries that are completely private to the .s file and should not
    /// have names in the .o file.  This is often "." or "L".
    protected String PrivateGlobalPrefix;         // Defaults to "."

    /// LinkerPrivateGlobalPrefix - This prefix is used for symbols that should
    /// be passed through the assembler but be removed by the linker.  This
    /// is "l" on Darwin, currently used for some ObjC metadata.
    protected String LinkerPrivateGlobalPrefix;   // Defaults to ""

    /// InlineAsmStart/End - If these are nonempty, they contain a directive to
    /// emit before and after an inline assembly statement.
    protected String InlineAsmStart;              // Defaults to "#APP\n"
    protected String InlineAsmEnd;                // Defaults to "#NO_APP\n"

    /// AssemblerDialect - Which dialect of an assembler variant to use.
    protected AsmWriterFlavorTy AssemblerDialect;               // Defaults to 0

    /// AllowQuotesInName - This is true if the assembler allows for complex
    /// symbol names to be surrounded in quotes.  This defaults to false.
    protected boolean AllowQuotesInName;

    /// AllowNameToStartWithDigit - This is true if the assembler allows symbol
    /// names to start with a digit (e.g., "0x0021").  This defaults to false.
    protected boolean AllowNameToStartWithDigit;

    //===--- Data Emission Directives -------------------------------------===//

    /// ZeroDirective - this should be set to the directive used to get some
    /// number of zero bytes emitted to the current section.  Common cases are
    /// "\t.zero\t" and "\t.space\t".  If this is set to null, the
    /// Data*bitsDirective's will be used to emit zero bytes.
    protected String ZeroDirective;               // Defaults to "\t.zero\t"

    /// AsciiDirective - This directive allows emission of an ascii string with
    /// the standard C escape characters embedded into it.
    protected String AsciiDirective;              // Defaults to "\t.ascii\t"

    /// AscizDirective - If not null, this allows for special handling of
    /// zero terminated strings on this target.  This is commonly supported as
    /// ".asciz".  If a target doesn't support this, it can be set to null.
    protected String AscizDirective;              // Defaults to "\t.asciz\t"

    /// DataDirectives - These directives are used to output some unit of
    /// integer data to the current section.  If a data directive is set to
    /// null, smaller data directives will be used to emit the large sizes.
    protected String Data8bitsDirective;          // Defaults to "\t.byte\t"
    protected String Data16bitsDirective;         // Defaults to "\t.short\t"
    protected String Data32bitsDirective;         // Defaults to "\t.long\t"
    protected String Data64bitsDirective;         // Defaults to "\t.quad\t"

    /// Code16Directive, Code32Directive, Code64Directive - These are assembly
    /// directives that tells the assembler to interpret the following
    /// instructions differently.
    protected String Code16Directive;             // Defaults to ".code16"
    protected String Code32Directive;             // Defaults to ".code32"
    protected String Code64Directive;             // Defaults to ".code64"

    /**
     * This is true if data region markers should be printed as
     * ".data_region/.end_data_region" directives. If false, use "$d/$a" labels
     * instead.
     */
    protected boolean supportsDataRegions;

    /// GPRel32Directive - if non-null, a directive that is used to emit a word
    /// which should be relocated as a 32-bit GP-relative offset, e.g. .gpword
    /// on Mips or .gprel32 on Alpha.
    protected String GPRel32Directive;            // Defaults to NULL.

    /// getDataASDirective - Return the directive that should be used to emit
    /// data of the specified size to the specified numeric address space.
    protected String getDataASDirective(int Size, int AS) {
        Util.assertion(AS != 0, "Don't know the directives for default addr space");
        return null;
    }

    /// SunStyleELFSectionSwitchSyntax - This is true if this target uses "Sun
    /// Style" syntax for section switching ("#alloc,#write" etc) instead of the
    /// normal ELF syntax (,"a,w") in .section directives.
    protected boolean SunStyleELFSectionSwitchSyntax;     // Defaults to false.

    /// UsesELFSectionDirectiveForBSS - This is true if this target uses ELF
    /// '.section' directive before the '.bss' one. It's used for PPC/Linux
    /// which doesn't support the '.bss' directive only.
    boolean UsesELFSectionDirectiveForBSS;      // Defaults to false.

    //===--- Alignment Information ----------------------------------------===//

    /// AlignDirective - The directive used to emit round up to an alignment
    /// boundary.
    ///
    protected String AlignDirective;              // Defaults to "\t.align\t"

    /// AlignmentIsInBytes - If this is true (the default) then the asmprinter
    /// emits ".align N" directives, where N is the number of bytes to align to.
    /// Otherwise, it emits ".align log2(N)", e.g. 3 to align to an 8 byte
    /// boundary.
    protected boolean AlignmentIsInBytes;                 // Defaults to true

    /// TextAlignFillValue - If non-zero, this is used to fill the executable
    /// space created as the result of a alignment directive.
    protected int TextAlignFillValue;             // Defaults to 0

    //===--- Global Variable Emission Directives --------------------------===//

    /// GlobalDirective - This is the directive used to declare a global entity.
    ///
    protected String GlobalDirective;             // Defaults to NULL.

    /// ExternDirective - This is the directive used to declare external
    /// globals.
    ///
    protected String ExternDirective;             // Defaults to NULL.

    /// HasSetDirective - True if the assembler supports the .set directive.
    protected boolean HasSetDirective;                    // Defaults to true.

    /// HasLCOMMDirective - This is true if the target supports the .lcomm
    /// directive.
    protected boolean HasLCOMMDirective;                  // Defaults to false.

    /// COMMDirectiveAlignmentIsInBytes - True is COMMDirective's optional
    /// alignment is to be specified in bytes instead of log2(n).
    protected boolean COMMDirectiveAlignmentIsInBytes;    // Defaults to true;

    /// HasDotTypeDotSizeDirective - True if the target has .type and .size
    /// directives, this is true for most ELF targets.
    protected boolean HasDotTypeDotSizeDirective;         // Defaults to true.

    /// HasSingleParameterDotFile - True if the target has a single parameter
    /// .file directive, this is true for ELF targets.
    protected boolean HasSingleParameterDotFile;          // Defaults to true.

    /// HasNoDeadStrip - True if this target supports the MachO .no_dead_strip
    /// directive.
    protected boolean HasNoDeadStrip;                     // Defaults to false.

    /// WeakRefDirective - This directive, if non-null, is used to declare a
    /// global as being a weak undefined symbol.
    protected String WeakRefDirective;            // Defaults to NULL.

    /// WeakDefDirective - This directive, if non-null, is used to declare a
    /// global as being a weak defined symbol.
    protected String WeakDefDirective;            // Defaults to NULL.

    /// LinkOnceDirective - This directive, if non-null is used to declare a
    /// global as being a weak defined symbol.  This is used on cygwin/mingw.
    protected String LinkOnceDirective;           // Defaults to NULL.

    /// HiddenVisibilityAttr - This attribute, if not MCSA_Invalid, is used to
    /// declare a symbol as having hidden visibility.
    MCSymbolAttr HiddenVisibilityAttr;       // Defaults to MCSA_Hidden.

    /// ProtectedVisibilityAttr - This attribute, if not MCSA_Invalid, is used
    /// to declare a symbol as having protected visibility.
    MCSymbolAttr ProtectedVisibilityAttr;    // Defaults to MCSA_Protected

    //===--- Dwarf Emission Directives -----------------------------------===//

    /// AbsoluteDebugSectionOffsets - True if we should emit abolute section
    /// offsets for debug information.
    protected boolean AbsoluteDebugSectionOffsets;        // Defaults to false.

    /// AbsoluteEHSectionOffsets - True if we should emit abolute section
    /// offsets for EH information. Defaults to false.
    protected boolean AbsoluteEHSectionOffsets;

    /// HasLEB128 - True if target asm supports leb128 directives.
    protected boolean HasLEB128;                          // Defaults to false.

    /// hasDotLocAndDotFile - True if target asm supports .loc and .file
    /// directives for emitting debugging information.
    protected boolean HasDotLocAndDotFile;                // Defaults to false.

    /// SupportsDebugInformation - True if target supports emission of debugging
    /// information.
    protected boolean SupportsDebugInformation;           // Defaults to false.

    /// SupportsExceptionHandling - True if target supports exception handling.
    protected ExceptionHandlingType ExceptionsType; // Defaults to None

    /// RequiresFrameSection - true if the Dwarf2 output needs a frame section
    protected boolean DwarfRequiresFrameSection;          // Defaults to true.

    /// DwarfUsesInlineInfoSection - True if DwarfDebugInlineSection is used to
    /// encode inline subroutine information.
    protected boolean DwarfUsesInlineInfoSection;         // Defaults to false.

    /// Is_EHSymbolPrivate - If set, the "_foo.eh" is made private so that it
    /// doesn't show up in the symbol table of the object file.
    protected boolean Is_EHSymbolPrivate;                 // Defaults to true.

    /// GlobalEHDirective - This is the directive used to make exception frame
    /// tables globally visible.
    protected String GlobalEHDirective;           // Defaults to NULL.

    /// SupportsWeakEmptyEHFrame - True if target assembler and linker will
    /// handle a weak_definition of constant 0 for an omitted EH frame.
    protected boolean SupportsWeakOmittedEHFrame;         // Defaults to true.

    /// DwarfSectionOffsetDirective - Special section offset directive.
    protected String DwarfSectionOffsetDirective; // Defaults to NULL

    //===--- CBE Asm Translation Table -----------------------------------===//

    protected String[] AsmTransCBE;          // Defaults to empty

    protected boolean isLittleEndian;

    public MCAsmInfo() {
        HasSubsectionsViaSymbols = false;
        HasMachoZeroFillDirective = false;
        HasStaticCtorDtorReferenceInStaticMode = false;
        MaxInstLength = 4;
        PCSymbol = "$";
        SeparatorChar = ";";
        CommentColumn = 40;
        CommentString = "#";
        GlobalPrefix = "";
        PrivateGlobalPrefix = ".";
        LinkerPrivateGlobalPrefix = "";
        InlineAsmStart = "APP";
        InlineAsmEnd = "NO_APP";
        AssemblerDialect = AsmWriterFlavorTy.ATT;
        AllowQuotesInName = false;
        AllowNameToStartWithDigit = false;
        ZeroDirective = "\t.zero\t";
        AsciiDirective = "\t.ascii\t";
        AscizDirective = "\t.asciz\t";
        Data8bitsDirective = "\t.byte\t";
        Data16bitsDirective = "\t.short\t";
        Data32bitsDirective = "\t.long\t";
        Data64bitsDirective = "\t.quad\t";
        Code16Directive = "\t.code16\t";
        Code32Directive = "\t.code32\t";
        Code64Directive = "\t.code64\t";
        supportsDataRegions = false;
        SunStyleELFSectionSwitchSyntax = false;
        UsesELFSectionDirectiveForBSS = false;
        AlignDirective = "\t.align\t";
        AlignmentIsInBytes = true;
        TextAlignFillValue = 0;
        GPRel32Directive = null;
        GlobalDirective = "\t.globl\t";
        HasSetDirective = true;
        HasLCOMMDirective = false;
        COMMDirectiveAlignmentIsInBytes = true;
        HasDotTypeDotSizeDirective = true;
        HasSingleParameterDotFile = true;
        HasNoDeadStrip = false;
        WeakRefDirective = null;
        WeakDefDirective = null;
        LinkOnceDirective = null;
        HiddenVisibilityAttr = MCSA_Hidden;
        ProtectedVisibilityAttr = MCSA_Protected;
        AbsoluteDebugSectionOffsets = false;
        AbsoluteEHSectionOffsets = false;
        HasLEB128 = false;
        HasDotLocAndDotFile = false;
        SupportsDebugInformation = false;
        ExceptionsType = ExceptionHandlingType.None;
        DwarfRequiresFrameSection = true;
        DwarfUsesInlineInfoSection = false;
        Is_EHSymbolPrivate = true;
        GlobalEHDirective = null;
        SupportsWeakOmittedEHFrame = true;
        DwarfSectionOffsetDirective = null;
        AsmTransCBE = null;
        isLittleEndian = true;
    }

    // FIXME: move these methods to DwarfPrinter when the JIT stops using them.
    public static int getULEB128Size(long Value) {
        int size = 0;
        do {
            Value >>>= 7;
            ++size;
        } while (Value != 0);
        return size;
    }

    public static int getSLEB128Size(long Value) {
        int size = 0;
        long sign = Value >> 31;
        boolean isMore;
        do {
            long b = Value & 0xf;
            Value >>= 7;
            isMore = Value != sign || ((b ^ sign) & 0x40) != 0;
            ++size;
        } while (isMore);
        return size;
    }

    public boolean hasSubsectionsViaSymbols() {
        return HasSubsectionsViaSymbols;
    }

    // Data directive accessors.
    //
    public String getData8bitsDirective() {
        return getData8bitsDirective(0);
    }

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

    public String getGPRel32Directive() {
        return GPRel32Directive;
    }

    /// getNonexecutableStackSection - Targets can implement this method to
    /// specify a section to switch to if the translation unit doesn't have any
    /// trampolines that require an executable stack.
    public MCSection getNonexecutableStackSection(MCSymbol.MCContext Ctx) {
        return null;
    }

    public boolean usesSunStyleELFSectionSwitchSyntax() {
        return SunStyleELFSectionSwitchSyntax;
    }

    public boolean usesELFSectionDirectiveForBSS() {
        return UsesELFSectionDirectiveForBSS;
    }

    // Accessors.
    //
    public boolean hasMachoZeroFillDirective() {
        return HasMachoZeroFillDirective;
    }

    public boolean hasStaticCtorDtorReferenceInStaticMode() {
        return HasStaticCtorDtorReferenceInStaticMode;
    }

    public int getMaxInstLength() {
        return MaxInstLength;
    }

    public String getPCSymbol() {
        return PCSymbol;
    }

    public String getSeparatorChar() {
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
        return PrivateGlobalPrefix;
    }

    public String getLinkerPrivateGlobalPrefix() {
        return LinkerPrivateGlobalPrefix;
    }

    public String getInlineAsmStart() {
        return InlineAsmStart;
    }

    public String getInlineAsmEnd() {
        return InlineAsmEnd;
    }

    public AsmWriterFlavorTy getAssemblerDialect() {
        return AssemblerDialect;
    }

    public boolean doesAllowQuotesInName() {
        return AllowQuotesInName;
    }

    public boolean doesAllowNameToStartWithDigit() {
        return AllowNameToStartWithDigit;
    }

    public String getZeroDirective() {
        return ZeroDirective;
    }

    public String getAsciiDirective() {
        return AsciiDirective;
    }

    public String getAscizDirective() {
        return AscizDirective;
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

    public boolean hasSetDirective() {
        return HasSetDirective;
    }

    public boolean hasLCOMMDirective() {
        return HasLCOMMDirective;
    }

    public boolean hasDotTypeDotSizeDirective() {
        return HasDotTypeDotSizeDirective;
    }

    public boolean getCOMMDirectiveAlignmentIsInBytes() {
        return COMMDirectiveAlignmentIsInBytes;
    }

    public boolean hasSingleParameterDotFile() {
        return HasSingleParameterDotFile;
    }

    public boolean hasNoDeadStrip() {
        return HasNoDeadStrip;
    }

    public String getWeakRefDirective() {
        return WeakRefDirective;
    }

    public String getWeakDefDirective() {
        return WeakDefDirective;
    }

    public String getLinkOnceDirective() {
        return LinkOnceDirective;
    }

    public MCSymbolAttr getHiddenVisibilityAttr() {
        return HiddenVisibilityAttr;
    }

    public MCSymbolAttr getProtectedVisibilityAttr() {
        return ProtectedVisibilityAttr;
    }

    public boolean isAbsoluteDebugSectionOffsets() {
        return AbsoluteDebugSectionOffsets;
    }

    public boolean isAbsoluteEHSectionOffsets() {
        return AbsoluteEHSectionOffsets;
    }

    public boolean hasLEB128() {
        return HasLEB128;
    }

    public boolean hasDotLocAndDotFile() {
        return HasDotLocAndDotFile;
    }

    public boolean doesSupportDebugInformation() {
        return SupportsDebugInformation;
    }

    public boolean doesSupportExceptionHandling() {
        return ExceptionsType != ExceptionHandlingType.None;
    }

    public ExceptionHandlingType getExceptionHandlingType() {
        return ExceptionsType;
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

    String getDwarfSectionOffsetDirective() {
        return DwarfSectionOffsetDirective;
    }

    public String[] getAsmCBE() {
        return AsmTransCBE;
    }

    public boolean isLittleEndian() {
        return isLittleEndian;
    }

    public boolean doesSupportDataRegionDirectives() {
        return supportsDataRegions;
    }
}
