/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */
package backend.mc;

import backend.target.SectionKind;

import java.io.PrintStream;

public class MCSectionELF extends MCSection {
  // This value marks the section header as inactive.
  public static final int SHT_NULL = 0x00;

  // Holds information defined by the program, with custom format and meaning.
  public static final int SHT_PROGBITS = 0x01;

  // This section holds a symbol table.
  public static final int SHT_SYMTAB = 0x02;

  // The section holds a string table.
  public static final int SHT_STRTAB = 0x03;

  // The section holds relocation entries with explicit addends.
  public static final int SHT_RELA = 0x04;

  // The section holds a symbol hash table.
  public static final int SHT_HASH = 0x05;

  // Information for dynamic linking.
  public static final int SHT_DYNAMIC = 0x06;

  // The section holds information that marks the file in some way.
  public static final int SHT_NOTE = 0x07;

  // A section of this type occupies no space in the file.
  public static final int SHT_NOBITS = 0x08;

  // The section holds relocation entries without explicit addends.
  public static final int SHT_REL = 0x09;

  // This section type is reserved but has unspecified semantics.
  public static final int SHT_SHLIB = 0x0A;

  // This section holds a symbol table.
  public static final int SHT_DYNSYM = 0x0B;

  // This section contains an array of pointers to initialization functions.
  public static final int SHT_INIT_ARRAY = 0x0E;

  // This section contains an array of pointers to termination functions.
  public static final int SHT_FINI_ARRAY = 0x0F;

  // This section contains an array of pointers to functions that are invoked
  // before all other initialization functions.
  public static final int SHT_PREINIT_ARRAY = 0x10;

  // A section group is a set of sections that are related and that must be
  // treated specially by the linker.
  public static final int SHT_GROUP = 0x11;

  // This section is associated with a section of type SHT_SYMTAB, when the
  // referenced symbol table contain the escape value SHN_XINDEX
  public static final int SHT_SYMTAB_SHNDX = 0x12;

  public static final int LAST_KNOWN_SECTION_TYPE = SHT_SYMTAB_SHNDX;

  /// Valid section flags.
  // The section contains data that should be writable.
  public static final int SHF_WRITE = 0x1;

  // The section occupies memory during execution.
  public static final int SHF_ALLOC = 0x2;

  // The section contains executable machine instructions.
  public static final int SHF_EXECINSTR = 0x4;

  // The data in the section may be merged to eliminate duplication.
  public static final int SHF_MERGE = 0x10;

  // Elements in the section consist of null-terminated character strings.
  public static final int SHF_STRINGS = 0x20;

  // A field in this section holds a section header table index.
  public static final int SHF_INFO_LINK = 0x40;

  // Adds special ordering requirements for link editors.
  public static final int SHF_LINK_ORDER = 0x80;

  // This section requires special OS-specific processing to avoid incorrect
  // behavior.
  public static final int SHF_OS_NONCONFORMING = 0x100;

  // This section is a member of a section group.
  public static final int SHF_GROUP = 0x200;

  // This section holds Thread-Local Storage.
  public static final int SHF_TLS = 0x400;

  // Start of target-specific flags.

  /// XCORE_SHF_CP_SECTION - All sections with the "c" flag are grouped
  /// together by the linker to form the constant pool and the cp register is
  /// set to the start of the constant pool by the boot code.
  public static final int XCORE_SHF_CP_SECTION = 0x800;

  /// XCORE_SHF_DP_SECTION - All sections with the "d" flag are grouped
  /// together by the linker to form the data section and the dp register is
  /// set to the start of the section by the boot code.
  public static final int XCORE_SHF_DP_SECTION = 0x1000;

  private String sectionName;
  private int type;
  private int flags;
  private boolean isExplicit;
  private int entrySize;

  public MCSectionELF(String sectionName,
                      int type,
                      int flags,
                      SectionKind kind,
                      boolean isExplicit) {
    this(sectionName, type, flags, kind, isExplicit, 0);
  }

  public MCSectionELF(String sectionName,
                      int type,
                      int flags,
                      SectionKind kind,
                      boolean isExplicit,
                      int entrySize) {
    super(kind);
    this.sectionName = sectionName;
    this.type = type;
    this.flags = flags;
    this.isExplicit = isExplicit;
    this.entrySize = entrySize;
  }

  public String getSectionName() {
    return sectionName;
  }

  public int getType() {
    return type;
  }

  public int getFlags() {
    return flags;
  }

  public static MCSectionELF create(String name, int type,
                                    int flags, SectionKind kind,
                                    boolean isExplicit,
                                    MCSymbol.MCContext ctx) {
    return new MCSectionELF(name, type, flags, kind, isExplicit);
  }

  /// ShouldOmitSectionDirective - Decides whether a '.section' directive
  /// should be printed before the section name
  public boolean ShouldOmitSectionDirective(String name,  MCAsmInfo mai) {
    switch (name) {
      case ".text":
      case ".data":
        return true;
      case ".bss":
        if (!mai.usesELFSectionDirectiveForBSS())
          return true;
        // fall through
      default:
        return false;
    }
  }

  /// ShouldPrintSectionType - Only prints the section type if supported
  public boolean ShouldPrintSectionType(int Ty) {
    return !isExplicit || (type == SHT_NOBITS || type == SHT_PROGBITS);
  }

  /// HasCommonSymbols - True if this section holds common symbols, this is
  /// indicated on the ELF object file by a symbol with SHN_COMMON section 
  /// header index.
  public boolean HasCommonSymbols() {
    return sectionName.startsWith(".gnu.linkonce.");
  }
  
  @Override
  public void printSwitchToSection(MCAsmInfo mai, PrintStream os) {
    if (ShouldOmitSectionDirective(sectionName, mai)) {
      os.printf("\t%s\n", getSectionName());
      return;
    }

    os.printf("\t.section\t%s", getSectionName());

    // Handle the weird solaris syntax if desired.
    if (mai.usesSunStyleELFSectionSwitchSyntax() &&
        (flags & SHF_MERGE) == 0) {
      if ((flags & SHF_ALLOC) != 0)
        os.print(",#alloc");
      if ((flags & SHF_EXECINSTR) != 0)
        os.print(",#execinstr");
      if ((flags & SHF_WRITE) != 0)
        os.print(",#write");
      if ((flags & SHF_TLS) != 0)
        os.print(",#tls");
    }
    else {
      os.print(",\"");
      if ((flags & SHF_ALLOC) != 0)
        os.print('a');
      if ((flags & SHF_EXECINSTR) != 0)
        os.print('x');
      if ((flags & SHF_WRITE) != 0)
        os.print('w');
      if ((flags & SHF_MERGE) != 0)
        os.print('M');
      if ((flags & SHF_STRINGS) != 0)
        os.print('S');
      if ((flags & SHF_TLS) != 0)
        os.print('T');

      // If there are target-specific flags, print them.
      if ((flags & XCORE_SHF_CP_SECTION) != 0)
        os.print('c');
      if ((flags & XCORE_SHF_DP_SECTION) != 0)
        os.print('d');

      os.print("\"");

      if (ShouldPrintSectionType(type)) {
        os.print(',');

        if (mai.getCommentString().charAt(0) == '@')
          os.print('%');
        else
          os.print('@');

        switch (type) {
          case SHT_INIT_ARRAY:
            os.print("init_array");
            break;
          case SHT_PREINIT_ARRAY:
            os.print("fini_array");
            break;
          case SHT_NOBITS:
            os.print("nobits");
            break;
          case SHT_PROGBITS:
            os.print("progbits");
            break;
        }

        if (getKind().isMergeable1ByteCString())
          os.print(",1");
        else if (getKind().isMergeable2ByteCString())
          os.print(",2");
        else if (getKind().isMergeable4ByteCString() ||
            getKind().isMergeableConst4())
          os.print(",4");
        else if (getKind().isMergeableConst8())
          os.print(",8");
        else if (getKind().isMergeableConst16())
          os.print(",16");
      }
    }
    os.println();
  }
}
