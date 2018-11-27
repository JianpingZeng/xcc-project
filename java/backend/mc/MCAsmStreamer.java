/*
 *  Extremely C language Compiler
 *    Copyright (c) 2015-2018, Jianping Zeng.
 *
 *  Licensed under the BSD License version 3. Please refer LICENSE for details.
 */

package backend.mc;

import tools.FormattedOutputStream;
import tools.TextUtils;
import tools.Util;

import java.io.PrintStream;

public class MCAsmStreamer extends MCStreamer {

  public static MCStreamer createAsmStreamer(
      MCSymbol.MCContext context,
      PrintStream os,
      MCAsmInfo mai,
      boolean isLittleEndian,
      MCInstPrinter instPrinter,
      MCCodeEmitter ce,
      boolean showInst) {
    return new MCAsmStreamer(context, os, mai, isLittleEndian, instPrinter, ce, showInst);
  }

  private FormattedOutputStream fos;
  private PrintStream os;
  private MCAsmInfo mai;
  private MCInstPrinter instPrinter;
  private MCCodeEmitter emitter;
  private StringBuilder commentToEmit;
  private PrintStream commentStream;

  private boolean isLittleEndian;
  private boolean isVerboseAsm;
  private boolean showInst;

  protected MCAsmStreamer(MCSymbol.MCContext ctx,
                          PrintStream os,
                          MCAsmInfo mai,
                          boolean isLittleEndian,
                          MCInstPrinter instPrinter,
                          MCCodeEmitter ce,
                          boolean showInst) {
    super(ctx);
    fos = new FormattedOutputStream(os);
    this.os = os;
    this.mai = mai;
    this.isLittleEndian = isLittleEndian;
    this.instPrinter = instPrinter;
    this.emitter = ce;
    this.showInst = showInst;
  }

  public boolean isLittleEndian() {
    return isLittleEndian;
  }

  public void emitEOL() {
    if (!isVerboseAsm) {
      fos.println();
      return;
    }
    emitCommentsAndEOL();
  }

  public void emitCommentsAndEOL() {
    // TODO: 11/27/18
  }

  @Override
  public boolean isVerboseAsm() {
    return isVerboseAsm;
  }

  @Override
  public void addComment(String str) {
    if (!isVerboseAsm) return;

    commentStream.flush();
    commentToEmit.append(str);
    commentToEmit.append('\n');
  }

  public void addEncodingComment(MCInst inst) {
    // TODO: 11/27/18
  }

  @Override
  public PrintStream getCommentOS() {
    if (!isVerboseAsm)
      return null;
    return commentStream;
  }

  @Override
  public void addBlankLine() {
    emitEOL();
  }

  @Override
  public void switchSection(MCSection section) {
    Util.assertion(section != null);
    if (section != curSection) {
      curSection = section;
      section.printSwitchToSection(mai, os);
    }
  }

  @Override
  public void emitLabel(MCSymbol symbol) {
    Util.assertion(symbol.isUndefined());
    Util.assertion(curSection != null);
    symbol.print(os);
    os.print(':');
    emitEOL();
    symbol.setSection(curSection);
  }

  @Override
  public void emitAssemblerFlag(MCAsmInfo.MCAssemblerFlag flag) {
    switch (flag) {
      case MCAF_SubsectionsViaSymbols:
        os.print(".subsections_via_symbols");
        break;
      default:
        Util.assertion("invalid flag");
        break;
    }
    emitEOL();
  }

  @Override
  public void emitAssignment(MCSymbol sym, MCExpr val) {
    Util.assertion(sym.isUndefined() || sym.isAbsoluate());

    sym.print(os);
    os.print(" = ");
    val.print(os);
    emitEOL();
    sym.setValue(val);
  }

  @Override
  public void emitSymbolAttribute(MCSymbol sym, MCAsmInfo.MCSymbolAttr attr) {
    switch (attr) {
      case MCSA_Invalid: Util.assertion("Invalid symbol attribute");
      case MCSA_ELF_TypeFunction:    /// .type _foo, STT_FUNC  # aka @function
      case MCSA_ELF_TypeIndFunction: /// .type _foo, STT_GNU_IFUNC
      case MCSA_ELF_TypeObject:      /// .type _foo, STT_OBJECT  # aka @object
      case MCSA_ELF_TypeTLS:         /// .type _foo, STT_TLS     # aka @tls_object
      case MCSA_ELF_TypeCommon:      /// .type _foo, STT_COMMON  # aka @common
      case MCSA_ELF_TypeNoType:      /// .type _foo, STT_NOTYPE  # aka @notype
        Util.assertion(mai.hasDotTypeDotSizeDirective(), "Symbol Attr not supported");
        os.print("\t.type\t");
        sym.print(os);
        os.print(',');
        os.print(mai.getCommentString().charAt(0) != '@' ? '@' : '%');
        switch (attr) {
          default: Util.assertion("Unknown ELF .type");
          case MCSA_ELF_TypeFunction:    os.print("function"); break;
          case MCSA_ELF_TypeIndFunction: os.print("gnu_indirect_function"); break;
          case MCSA_ELF_TypeObject:      os.print("object"); break;
          case MCSA_ELF_TypeTLS:         os.print("tls_object"); break;
          case MCSA_ELF_TypeCommon:      os.print("common"); break;
          case MCSA_ELF_TypeNoType:      os.print("no_type"); break;
        }
        emitEOL();
        return;
      case MCSA_Global: // .globl/.global
        os.print(mai.getGlobalDirective());
        break;
      case MCSA_Hidden:         os.print(".hidden ");          break;
      case MCSA_IndirectSymbol: os.print(".indirect_symbol "); break;
      case MCSA_Internal:       os.print(".internal ");        break;
      case MCSA_LazyReference:  os.print(".lazy_reference ");  break;
      case MCSA_Local:          os.print(".local ");           break;
      case MCSA_NoDeadStrip:    os.print(".no_dead_strip ");   break;
      case MCSA_PrivateExtern:  os.print(".private_extern ");  break;
      case MCSA_Protected:      os.print(".protected ");       break;
      case MCSA_Reference:      os.print(".reference ");       break;
      case MCSA_Weak:           os.print(".weak ");            break;
      case MCSA_WeakDefinition: os.print(".weak_definition "); break;
      // .weak_reference
      case MCSA_WeakReference:  os.print(mai.getWeakRefDirective()); break;
    }
  }

  @Override
  public void emitSymbolDesc(MCSymbol sym, int descValue) {
    os.print(".desc ");
    sym.print(os);
    os.print(',');
    os.print(descValue);
    emitEOL();
  }

  @Override
  public void emitELFSize(MCSymbol sym, MCExpr val) {
    Util.assertion(mai.hasDotTypeDotSizeDirective());
    os.print("\t.size\t");
    sym.print(os);
    os.print(", ");
    val.print(os);
    os.println();
  }

  @Override
  public void emitCommonSymbol(MCSymbol sym, long size, int byteAlignment) {
    os.print("\t.comm\t");
    sym.print(os);
    os.print(',');
    os.print(size);
    if (byteAlignment != 0) {
      if (mai.getCOMMDirectiveAlignmentIsInBytes())
        os.printf(",%d", byteAlignment);
      else
        os.printf(",%d", Util.log2(byteAlignment));
    }
  }

  @Override
  public void emitLocalCommonSymbol(MCSymbol sym, long size) {
    Util.assertion(mai.hasLCOMMDirective(), "Doesn't have .lcomm, can't emit it");
    os.print("\t.lcomm\t");
    sym.print(os);
    os.print(',');
    os.print(size);
    emitEOL();
  }

  @Override
  public void emitZeroFill(MCSection section, MCSymbol sym, int size, int byteAlignment) {
    os.print(".zerofill");

    // This is a mach-o specific directive.
    Util.shouldNotReachHere("Mach-O target is not supported as yet!");
  }

  private static void printQuotedString(String data, PrintStream os) {
    os.print('"');

    for (int i = 0, e = data.length(); i < e; i++) {
      char ch = data.charAt(i);
      if (ch == '"' || ch == '\\') {
        os.print('\\');
        os.print(ch);
        continue;
      }

      if (TextUtils.isPrintable(ch)) {
        os.print(ch);
        continue;
      }

      switch (ch) {
        case '\b': os.print("\\b"); break;
        case '\f': os.print("\\f"); break;
        case '\n': os.print("\\n"); break;
        case '\r': os.print("\\r"); break;
        case '\t': os.print("\\t"); break;
        default:
          os.print("\\");
          os.print(Util.toOctal(ch>>6));
          os.print(Util.toOctal(ch >> 3));
          os.print(Util.toOctal(ch));
          break;
      }
    }
    os.print('"');
  }

  @Override
  public void emitBytes(String data, int addrSpace) {
    Util.assertion(curSection != null);
    if (data.isEmpty()) return;

    if (data.length() == 1) {
      os.print(mai.getData8bitsDirective(addrSpace));
      os.print(data.charAt(0));
      emitEOL();
      return;
    }

    // If the data ends with 0 and the target supports .asciz, use it, otherwise
    // use .ascii
    if (mai.getAscizDirective() != null && data.charAt(data.length()-1) == 0) {
      os.print(mai.getAscizDirective());
      data = data.substring(0, data.length()-1);
    }
    else {
      os.print(mai.getAsciiDirective());
    }

    os.print(' ');
    printQuotedString(data, os);
    emitEOL();
  }

  @Override
  public void emitValue(MCExpr val, int size, int addrSpace) {
    Util.assertion(curSection != null);
    String directive = null;
    switch (size) {
      default: break;
      case 1: directive = mai.getData8bitsDirective(addrSpace); break;
      case 2: directive = mai.getData16bitsDirective(addrSpace); break;
      case 4: directive = mai.getData32bitsDirective(addrSpace); break;
      case 8:
        directive = mai.getData64bitsDirective(addrSpace);
        break;
    }
    Util.assertion(directive != null);
    os.print(directive);
    val.print(os);
    emitEOL();
  }

  private static long truncateToSize(long val, int bytes) {
    Util.assertion(bytes != 0);
    return val & (-1L >> (64 - bytes*8));
  }

  @Override
  public void emitIntValue(long val, int size, int addrSpace) {
    Util.assertion(curSection != null);
    String directive = null;
    switch (size) {
      default: break;
      case 1: directive = mai.getData8bitsDirective(addrSpace); break;
      case 2: directive = mai.getData16bitsDirective(addrSpace); break;
      case 4: directive = mai.getData32bitsDirective(addrSpace); break;
      case 8:
        directive = mai.getData64bitsDirective(addrSpace);
        // if the target doesn't support 64 bit data, eit as two 32 bit values.
        if (directive != null) break;
        if (isLittleEndian) {
          emitIntValue(((int)val), 4, addrSpace);
          emitIntValue((int)(val >>> 32), 4, addrSpace);
        }
        else {
          emitIntValue((int)(val >>> 32), 4, addrSpace);
          emitIntValue(((int)val), 4, addrSpace);
        }
        return;
    }
    Util.assertion(directive != null);
    os.print(directive);
    os.print(truncateToSize(val,size));
    emitEOL();
  }

  @Override
  public void emitGPRel32Value(MCExpr val) {
    Util.assertion(mai.getGPRel32Directive() != null);
    os.print(mai.getGPRel32Directive());
    val.print(os);
    emitEOL();
  }

  @Override
  public void emitFill(long numBytes, int fillValue, int addrSpace) {
    if (numBytes == 0) return;
    if (addrSpace == 0) {
      String zeroDirective = mai.getZeroDirective();
      if (zeroDirective != null) {
        os.print(zeroDirective);
        os.print(numBytes);
        if (fillValue != 0)
          os.printf(",%d", fillValue);
        emitEOL();
        return;
      }
    }

    super.emitFill(numBytes, fillValue, addrSpace);
  }

  @Override
  public void emitZeros(long numBytes, int addrSpace) {

  }

  @Override
  public void emitValueToAlignment(int byteAlignment, long value, int valueSize, int maxBytesToEmit) {
    if (Util.isPowerOf2(byteAlignment)) {

    }
  }

  @Override
  public void emitCodeAlignment(int byteAlignment, int maxBytesToEmit) {

  }

  @Override
  public void emitValueToOffset(MCExpr offset, int value) {

  }

  @Override
  public void emitFileDirective(String filename) {

  }

  @Override
  public void emitDwarfFileDirective(int fileNo, String filename) {

  }

  @Override
  public void emitInstruction(MCInst inst) {

  }

  @Override
  public void finish() {

  }
}
