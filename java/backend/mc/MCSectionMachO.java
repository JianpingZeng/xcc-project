/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2019, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.mc;

import backend.target.SectionKind;
import tools.OutRef;
import tools.Util;

import java.io.PrintStream;

/**
 * This represents a section on a Mach-O platform (used by MAC OSX).
 * On the mac system, there are also described in /usr/include/mach-o/loader.h
 */
public class MCSectionMachO extends MCSection {

  /// These are the section type and attributes fields.  A MachO section can
  /// have only one Type, but can have any of the attributes specified.
  public static final int
    // TypeAndAttributes bitmasks.
    SECTION_TYPE       = 0x000000FF,
    SECTION_ATTRIBUTES = 0xFFFFFF00,

    // Valid section types.

    /// S_REGULAR - Regular section.
    S_REGULAR                    = 0x00,
    /// S_ZEROFILL - Zero fill on demand section.
    S_ZEROFILL                   = 0x01,
    /// S_CSTRING_LITERALS - Section with literal C strings.
    S_CSTRING_LITERALS           = 0x02,
    /// S_4BYTE_LITERALS - Section with 4 byte literals.
    S_4BYTE_LITERALS             = 0x03,
    /// S_8BYTE_LITERALS - Section with 8 byte literals.
    S_8BYTE_LITERALS             = 0x04,
    /// S_LITERAL_POINTERS - Section with pointers to literals.
    S_LITERAL_POINTERS           = 0x05,
    /// S_NON_LAZY_SYMBOL_POINTERS - Section with non-lazy symbol pointers.
    S_NON_LAZY_SYMBOL_POINTERS   = 0x06,
    /// S_LAZY_SYMBOL_POINTERS - Section with lazy symbol pointers.
    S_LAZY_SYMBOL_POINTERS       = 0x07,
    /// S_SYMBOL_STUBS - Section with symbol stubs, byte size of stub in
    /// the Reserved2 field.
    S_SYMBOL_STUBS               = 0x08,
    /// S_SYMBOL_STUBS - Section with only function pointers for
    /// initialization.
    S_MOD_INIT_FUNC_POINTERS     = 0x09,
    /// S_MOD_INIT_FUNC_POINTERS - Section with only function pointers for
    /// termination.
    S_MOD_TERM_FUNC_POINTERS     = 0x0A,
    /// S_COALESCED - Section contains symbols that are to be coalesced.
    S_COALESCED                  = 0x0B,
    /// S_GB_ZEROFILL - Zero fill on demand section (that can be larger than 4
    /// gigabytes).
    S_GB_ZEROFILL                = 0x0C,
    /// S_INTERPOSING - Section with only pairs of function pointers for
    /// interposing.
    S_INTERPOSING                = 0x0D,
    /// S_16BYTE_LITERALS - Section with only 16 byte literals.
    S_16BYTE_LITERALS            = 0x0E,
    /// S_DTRACE_DOF - Section contains DTrace Object Format.
    S_DTRACE_DOF                 = 0x0F,
    /// S_LAZY_DYLIB_SYMBOL_POINTERS - Section with lazy symbol pointers to
    /// lazy loaded dylibs.
    S_LAZY_DYLIB_SYMBOL_POINTERS = 0x10,
    /// S_THREAD_LOCAL_REGULAR - Section with ....
    S_THREAD_LOCAL_REGULAR = 0x11,
    /// S_THREAD_LOCAL_ZEROFILL - Thread local zerofill section.
    S_THREAD_LOCAL_ZEROFILL = 0x12,
    /// S_THREAD_LOCAL_VARIABLES - Section with thread local variable structure
    /// data.
    S_THREAD_LOCAL_VARIABLES = 0x13,
    /// S_THREAD_LOCAL_VARIABLE_POINTERS - Section with ....
    S_THREAD_LOCAL_VARIABLE_POINTERS = 0x14,
    /// S_THREAD_LOCAL_INIT_FUNCTION_POINTERS - Section with thread local
    /// variable initialization pointers to functions.
    S_THREAD_LOCAL_INIT_FUNCTION_POINTERS = 0x15,

    LAST_KNOWN_SECTION_TYPE = S_THREAD_LOCAL_INIT_FUNCTION_POINTERS,


    // Valid section attributes.

    /// S_ATTR_PURE_INSTRUCTIONS - Section contains only true machine
    /// instructions.
    S_ATTR_PURE_INSTRUCTIONS   = 1 << 31,
    /// S_ATTR_NO_TOC - Section contains coalesced symbols that are not to be
    /// in a ranlib table of contents.
    S_ATTR_NO_TOC              = 1 << 30,
    /// S_ATTR_STRIP_STATIC_SYMS - Ok to strip static symbols in this section
    /// in files with the MY_DYLDLINK flag.
    S_ATTR_STRIP_STATIC_SYMS   = 1 << 29,
    /// S_ATTR_NO_DEAD_STRIP - No dead stripping.
    S_ATTR_NO_DEAD_STRIP       = 1 << 28,
    /// S_ATTR_LIVE_SUPPORT - Blocks are live if they reference live blocks.
    S_ATTR_LIVE_SUPPORT        = 1 << 27,
    /// S_ATTR_SELF_MODIFYING_CODE - Used with i386 code stubs written on by
    /// dyld.
    S_ATTR_SELF_MODIFYING_CODE = 1 << 26,
    /// S_ATTR_DEBUG - A debug section.
    S_ATTR_DEBUG               = 1 << 25,
    /// S_ATTR_SOME_INSTRUCTIONS - Section contains some machine instructions.
    S_ATTR_SOME_INSTRUCTIONS   = 1 << 10,
    /// S_ATTR_EXT_RELOC - Section has external relocation entries.
    S_ATTR_EXT_RELOC           = 1 << 9,
    /// S_ATTR_LOC_RELOC - Section has local relocation entries.
    S_ATTR_LOC_RELOC           = 1 << 8;

  /// These are strings that describe the various section
/// types.  This *must* be kept in order with and stay synchronized with the
/// section type list.
  private static class Descriptor {
    String assemblerName, enumName;
    Descriptor(String asmName, String enumName) {
      assemblerName = asmName;
      this.enumName = enumName;
    }
  }
  private static final Descriptor[] SectionTypeDescriptors = {
    new Descriptor("regular",                  "S_REGULAR"),                    // 0x00
    new Descriptor(null,                       "S_ZEROFILL"),                   // 0x01
    new Descriptor("cstring_literals",         "S_CSTRING_LITERALS"),           // 0x02
    new Descriptor("4byte_literals",           "S_4BYTE_LITERALS"),             // 0x03
    new Descriptor("8byte_literals",           "S_8BYTE_LITERALS"),             // 0x04
    new Descriptor("literal_pointers",         "S_LITERAL_POINTERS"),           // 0x05
    new Descriptor("non_lazy_symbol_pointers", "S_NON_LAZY_SYMBOL_POINTERS"),   // 0x06
    new Descriptor("lazy_symbol_pointers",     "S_LAZY_SYMBOL_POINTERS"),       // 0x07
    new Descriptor("symbol_stubs",             "S_SYMBOL_STUBS"),               // 0x08
    new Descriptor("mod_init_funcs",           "S_MOD_INIT_FUNC_POINTERS"),     // 0x09
    new Descriptor("mod_term_funcs",           "S_MOD_TERM_FUNC_POINTERS"),     // 0x0A
    new Descriptor("coalesced",                "S_COALESCED"),                  // 0x0B
    new Descriptor(null, /*FIXME??*/           "S_GB_ZEROFILL"),                // 0x0C
    new Descriptor("interposing",              "S_INTERPOSING"),                // 0x0D
    new Descriptor("16byte_literals",          "S_16BYTE_LITERALS"),            // 0x0E
    new Descriptor(null, /*FIXME??*/           "S_DTRACE_DOF"),                 // 0x0F
    new Descriptor(null, /*FIXME??*/           "S_LAZY_DYLIB_SYMBOL_POINTERS"), // 0x10
    new Descriptor("thread_local_regular",     "S_THREAD_LOCAL_REGULAR"),       // 0x11
    new Descriptor("thread_local_zerofill",    "S_THREAD_LOCAL_ZEROFILL"),      // 0x12
    new Descriptor("thread_local_variables",   "S_THREAD_LOCAL_VARIABLES"),     // 0x13
    new Descriptor("thread_local_variable_pointers",
        "S_THREAD_LOCAL_VARIABLE_POINTERS"),                         // 0x14
    new Descriptor("thread_local_init_function_pointers",
        "S_THREAD_LOCAL_INIT_FUNCTION_POINTERS")                     // 0x15
  };

  private static class AttrDescriptor {
    int attrFlag;
    String assemblyName, enumName;
    AttrDescriptor(int attr, String asmName, String enumName) {
      attrFlag = attr;
      assemblyName = asmName;
      this.enumName = enumName;
    }
  }

  private static final AttrDescriptor[] SectionAttrDescriptors = {
      new AttrDescriptor(S_ATTR_PURE_INSTRUCTIONS, "pure_instructions", "S_ATTR_PURE_INSTRUCTIONS"),
      new AttrDescriptor(S_ATTR_NO_TOC, "no_toc", "S_ATTR_NO_TOC"),
      new AttrDescriptor(S_ATTR_STRIP_STATIC_SYMS, "strip_static_syms", "S_ATTR_STRIP_STATIC_SYMS"),
      new AttrDescriptor(S_ATTR_NO_DEAD_STRIP, "no_dead_strip", "S_ATTR_NO_DEAD_STRIP"),
      new AttrDescriptor(S_ATTR_LIVE_SUPPORT, "live_support", "S_ATTR_LIVE_SUPPORT"),
      new AttrDescriptor(S_ATTR_SELF_MODIFYING_CODE, "self_modifying_code", "S_ATTR_SELF_MODIFYING_CODE"),
      new AttrDescriptor(S_ATTR_DEBUG, "debug", "S_ATTR_DEBUG"),
      new AttrDescriptor(S_ATTR_SOME_INSTRUCTIONS, null, "S_ATTR_SOME_INSTRUCTIONS"),
      new AttrDescriptor(S_ATTR_EXT_RELOC, null, "S_ATTR_EXT_RELOC"),
      new AttrDescriptor(S_ATTR_LOC_RELOC, null, "S_ATTR_LOC_RELOC"),
      new AttrDescriptor(0, "none", ""),
      new AttrDescriptor(0xffffffff, null, null)
  };

  private String segmentName;
  private String sectionName;

  /**
   * This is the SECTION_TYPE and SECTION_ATTRIBUTES field of
   * a section, drawn from the enums below.
   */
  private int typeAndAttributes;
  /**
   * The field of a section which is uesd to represent the
   * size of stubs.
   */
  private int reserved2;

  protected MCSectionMachO(String segment,
                           String section,
                           int taa,
                           int reserved2,
                           SectionKind kind) {
    super(kind);
    typeAndAttributes = taa;
    this.reserved2 = reserved2;
    Util.assertion(segment.length() <= 16 && section.length() <= 16,
        "Segment or section string is too long");
    segmentName = segment;
    sectionName = section;
  }

  @Override
  public void printSwitchToSection(MCAsmInfo mai, PrintStream os) {
    os.printf("\t.section\t%s,%s", segmentName, sectionName);
    // get the section type and attributes.
    if (typeAndAttributes == 0) {
      os.println();
      return;
    }

    os.print(',');
    int sectionType = typeAndAttributes & SECTION_TYPE;
    Util.assertion(sectionType <= LAST_KNOWN_SECTION_TYPE, "Invalid section type specified!");
    if (SectionTypeDescriptors[sectionType].assemblerName != null)
      os.print(SectionTypeDescriptors[sectionType].assemblerName);
    else
      os.printf("<<%s>>", SectionTypeDescriptors[sectionType].enumName);

    // if we don't have any attributes, we're done.
    int sectionAttrs = typeAndAttributes & SECTION_ATTRIBUTES;
    if (sectionAttrs == 0) {
      if (reserved2 != 0)
        os.printf(",none,%d", reserved2);
      os.println();
      return;
    }

    // check each attribute to see if we have it.
    char separator = ',';
    for (int i = 0; SectionAttrDescriptors[i].attrFlag != 0; i++) {
      // check to see if we have this attribute.
      if ((SectionAttrDescriptors[i].attrFlag & sectionAttrs) == 0)
        continue;

      // yeap, clear it and print it.
      sectionAttrs &= ~SectionAttrDescriptors[i].attrFlag;
      os.print(separator);
      if (SectionAttrDescriptors[i].assemblyName != null)
        os.print(SectionAttrDescriptors[i].assemblyName != null);
      else
        os.printf("<<%s>>", SectionTypeDescriptors[i].enumName);
      separator = '+';
    }

    Util.assertion(sectionAttrs == 0, "Unknown section attributes!");
    if (reserved2 != 0)
      os.printf(",%d", reserved2);
    os.println();
  }

  public String getSegmentName() { return segmentName; }

  public String getSectionName() { return sectionName; }

  public int getTypeAndAttributes() { return typeAndAttributes; }
  public int getStubSize() { return reserved2; }
  public int getType() { return typeAndAttributes & SECTION_TYPE; }
  public boolean hasAttribute(int value) { return (typeAndAttributes & value) != 0; }

  public static String parseSectionSpecifier(String spec, OutRef<String> segment, OutRef<String> section, OutRef<Integer> taa, OutRef<Integer> stubSize) { Util.shouldNotReachHere(); return null;}
}
