/*
 *  Extremely C language Compiler
 *    Copyright (c) 2015-2018, Jianping Zeng.
 *
 *  Licensed under the BSD License version 3. Please refer LICENSE for details.
 */

package backend.mc;

import tools.FormattedOutputStream;
import tools.NullOutputStream;
import tools.TextUtils;
import tools.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class MCAsmStreamer extends MCStreamer {

  public static MCStreamer createAsmStreamer(
      MCSymbol.MCContext context,
      PrintStream fos,
      MCAsmInfo mai,
      boolean isLittleEndian,
      boolean isVerboseAsm,
      MCInstPrinter instPrinter,
      MCCodeEmitter ce,
      boolean showInst) {
    return new MCAsmStreamer(context, fos, mai, isLittleEndian,
        isVerboseAsm, instPrinter, ce, showInst);
  }

  private FormattedOutputStream fos;
  private PrintStream os;
  private MCAsmInfo mai;
  private MCInstPrinter instPrinter;
  private MCCodeEmitter emitter;
  private ByteArrayOutputStream commentToEmit;
  private PrintStream commentStream;

  private boolean isLittleEndian;
  private boolean isVerboseAsm;
  private boolean showInst;

  protected MCAsmStreamer(MCSymbol.MCContext ctx,
                          PrintStream os,
                          MCAsmInfo mai,
                          boolean isLittleEndian,
                          boolean isVerboseAsm,
                          MCInstPrinter instPrinter,
                          MCCodeEmitter ce,
                          boolean showInst) {
    super(ctx);
    commentToEmit = new ByteArrayOutputStream();
    commentStream = new PrintStream(commentToEmit);
    fos = new FormattedOutputStream(os);
    this.os = os;
    this.mai = mai;
    this.isLittleEndian = isLittleEndian;
    this.isVerboseAsm = isVerboseAsm;
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
    if (commentToEmit.size() <= 0) {
      fos.println();
      return;
    }
    
    commentStream.flush();
    String comments = commentToEmit.toString();
    Util.assertion(comments.charAt(comments.length()-1) == '\n',
        "Comment array not newline terminated");
    do {
      fos.padToColumn(mai.getCommentColumn());
      int position = comments.indexOf('\n');
      fos.printf("%s %s\n", mai.getCommentString(), comments.substring(0, position));
      comments = comments.substring(position+1);
    }while (!comments.isEmpty());

    resetCommentOS();
  }

  private void resetCommentOS() {
    commentToEmit = new ByteArrayOutputStream();
    commentStream = new PrintStream(commentToEmit);
  }

  @Override
  public boolean isVerboseAsm() {
    return isVerboseAsm;
  }

  @Override
  public boolean hasRawTextSupport() {
    return true;
  }

  @Override
  public void emitRawText(String str) {
    if (!hasRawTextSupport()) return;
    if (!str.isEmpty() && str.charAt(str.length()-1) == '\n')
      str = str.substring(0, str.length()-1);
    fos.print(str);
    emitEOL();
  }

  @Override
  public void addComment(String str) {
    if (!isVerboseAsm) return;
    PrintStream cos = getCommentOS();
    cos.println(str);
  }

  public void addEncodingComment(MCInst inst) {
    // TODO: 11/27/18
  }

  @Override
  public PrintStream getCommentOS() {
    if (!isVerboseAsm)
      return NullOutputStream.nulls();
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
    fos.print(':');
    emitEOL();
    symbol.setSection(curSection);
  }

  @Override
  public void emitAssemblerFlag(MCAsmInfo.MCAssemblerFlag flag) {
    switch (flag) {
      case MCAF_SubsectionsViaSymbols:
        fos.print(".subsections_via_symbols");
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
    fos.print(" = ");
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
        fos.print("\t.type\t");
        sym.print(os);
        fos.print(',');
        fos.print(mai.getCommentString().charAt(0) != '@' ? '@' : '%');
        switch (attr) {
          default: Util.assertion("Unknown ELF .type");
          case MCSA_ELF_TypeFunction:    fos.print("function"); break;
          case MCSA_ELF_TypeIndFunction: fos.print("gnu_indirect_function"); break;
          case MCSA_ELF_TypeObject:      fos.print("object"); break;
          case MCSA_ELF_TypeTLS:         fos.print("tls_object"); break;
          case MCSA_ELF_TypeCommon:      fos.print("common"); break;
          case MCSA_ELF_TypeNoType:      fos.print("no_type"); break;
        }
        emitEOL();
        return;
      case MCSA_Global: // .globl/.global
        fos.print(mai.getGlobalDirective());
        break;
      case MCSA_Hidden:         fos.print(".hidden ");          break;
      case MCSA_IndirectSymbol: fos.print(".indirect_symbol "); break;
      case MCSA_Internal:       fos.print(".internal ");        break;
      case MCSA_LazyReference:  fos.print(".lazy_reference ");  break;
      case MCSA_Local:          fos.print(".local ");           break;
      case MCSA_NoDeadStrip:    fos.print(".no_dead_strip ");   break;
      case MCSA_PrivateExtern:  fos.print(".private_extern ");  break;
      case MCSA_Protected:      fos.print(".protected ");       break;
      case MCSA_Reference:      fos.print(".reference ");       break;
      case MCSA_Weak:           fos.print(".weak ");            break;
      case MCSA_WeakDefinition: fos.print(".weak_definition "); break;
      // .weak_reference
      case MCSA_WeakReference:  fos.print(mai.getWeakRefDirective()); break;
    }
    sym.print(os);
    emitEOL();
  }

  @Override
  public void emitSymbolDesc(MCSymbol sym, int descValue) {
    fos.print(".desc ");
    sym.print(os);
    fos.print(',');
    fos.print(descValue);
    emitEOL();
  }

  @Override
  public void emitELFSize(MCSymbol sym, MCExpr val) {
    Util.assertion(mai.hasDotTypeDotSizeDirective());
    fos.print("\t.size\t");
    sym.print(os);
    fos.print(", ");
    val.print(os);
    fos.println();
  }

  @Override
  public void emitCommonSymbol(MCSymbol sym, long size, int byteAlignment) {
    fos.print("\t.comm\t");
    sym.print(os);
    fos.print(',');
    fos.print(size);
    if (byteAlignment != 0) {
      if (mai.getCOMMDirectiveAlignmentIsInBytes())
        fos.printf(",%d", byteAlignment);
      else
        fos.printf(",%d", Util.log2(byteAlignment));
    }
  }

  @Override
  public void emitLocalCommonSymbol(MCSymbol sym, long size) {
    Util.assertion(mai.hasLCOMMDirective(), "Doesn't have .lcomm, can't emit it");
    fos.print("\t.lcomm\t");
    sym.print(os);
    fos.print(',');
    fos.print(size);
    emitEOL();
  }

  @Override
  public void emitZeroFill(MCSection section, MCSymbol sym, int size, int byteAlignment) {
    fos.print(".zerofill");

    // This is a mach-o specific directive.
    Util.shouldNotReachHere("Mach-O target is not supported as yet!");
  }

  private static void printQuotedString(String data, PrintStream fos) {
    fos.print('"');

    for (int i = 0, e = data.length(); i < e; i++) {
      char ch = data.charAt(i);
      if (ch == '"' || ch == '\\') {
        fos.print('\\');
        fos.print(ch);
        continue;
      }

      if (TextUtils.isPrintable(ch)) {
        fos.print(ch);
        continue;
      }

      switch (ch) {
        case '\b': fos.print("\\b"); break;
        case '\f': fos.print("\\f"); break;
        case '\n': fos.print("\\n"); break;
        case '\r': fos.print("\\r"); break;
        case '\t': fos.print("\\t"); break;
        default:
          fos.print("\\");
          fos.print(Util.toOctal(ch>>6));
          fos.print(Util.toOctal(ch >> 3));
          fos.print(Util.toOctal(ch));
          break;
      }
    }
    fos.print('"');
  }

  @Override
  public void emitBytes(String data, int addrSpace) {
    Util.assertion(curSection != null);
    if (data.isEmpty()) return;

    if (data.length() == 1) {
      fos.print(mai.getData8bitsDirective(addrSpace));
      fos.print(data.charAt(0));
      emitEOL();
      return;
    }

    // If the data ends with 0 and the target supports .asciz, use it, otherwise
    // use .ascii
    if (mai.getAscizDirective() != null && data.charAt(data.length()-1) == 0) {
      fos.print(mai.getAscizDirective());
      data = data.substring(0, data.length()-1);
    }
    else {
      fos.print(mai.getAsciiDirective());
    }

    fos.print(' ');
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
    fos.print(directive);
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
    fos.print(directive);
    fos.print(truncateToSize(val,size));
    emitEOL();
  }

  @Override
  public void emitGPRel32Value(MCExpr val) {
    Util.assertion(mai.getGPRel32Directive() != null);
    fos.print(mai.getGPRel32Directive());
    val.print(os);
    emitEOL();
  }

  @Override
  public void emitFill(long numBytes, int fillValue, int addrSpace) {
    if (numBytes == 0) return;
    if (addrSpace == 0) {
      String zeroDirective = mai.getZeroDirective();
      if (zeroDirective != null) {
        fos.print(zeroDirective);
        fos.print(numBytes);
        if (fillValue != 0)
          fos.printf(",%d", fillValue);
        emitEOL();
        return;
      }
    }

    super.emitFill(numBytes, fillValue, addrSpace);
  }

  @Override
  public void emitZeros(long numBytes, int addrSpace) {
    emitFill(numBytes, 0, addrSpace);
  }

  @Override
  public void emitValueToAlignment(int byteAlignment, long value, int valueSize, int maxBytesToEmit) {
    if (Util.isPowerOf2(byteAlignment)) {
      switch (valueSize) {
        default:
          Util.shouldNotReachHere("Invalid size for machine code value!");
          break;
        case 1: fos.print(mai.getAlignDirective());break;
        case 2: fos.print(".p2alignw"); break;
        case 4: fos.print(".p2alignl"); break;
        case 8: Util.shouldNotReachHere("Unsupported alignment size!");
      }

      if (mai.getAlignmentIsInBytes())
        fos.print(byteAlignment);
      else
        fos.print(Util.log2(byteAlignment));

      if (value != 0 || maxBytesToEmit != 0) {
        fos.print(", 0x");
        fos.printf("%x", truncateToSize(value, valueSize));
        if (maxBytesToEmit != 0)
          fos.printf(", %d", maxBytesToEmit);
      }
      emitEOL();
      return;
    }

    switch (valueSize) {
      default:Util.shouldNotReachHere("Invalid size for machine code value");
      case 1: fos.print(".balign"); break;
      case 2: fos.print(".balignw"); break;
      case 4: fos.print(".balignl"); break;
      case 8: Util.shouldNotReachHere("Unsupported alignment size!");
    }

    fos.printf(" %d", byteAlignment);
    fos.printf(", %d", truncateToSize(value, valueSize));
    if (maxBytesToEmit !=0 )
      fos.printf(", %d", maxBytesToEmit);
    emitEOL();
  }

  @Override
  public void emitCodeAlignment(int byteAlignment, int maxBytesToEmit) {
    emitValueToAlignment(byteAlignment, mai.getTextAlignFillValue(), 1, maxBytesToEmit);
  }

  @Override
  public void emitValueToOffset(MCExpr offset, int value) {
    fos.print(".org");
    offset.print(os);
    fos.print(value);
    emitEOL();
  }

  @Override
  public void emitFileDirective(String filename) {
    Util.assertion(mai.hasSingleParameterDotFile());
    fos.print("\t.file\t");
    printQuotedString(filename, os);
    emitEOL();
  }

  @Override
  public void emitDwarfFileDirective(int fileNo, String filename) {
    fos.printf("\t.file\t%d ", fileNo);
    printQuotedString(filename, os);
    emitEOL();
  }

  @Override
  public void emitInstruction(MCInst inst) {
    Util.assertion(curSection != null, "Can't emit contents before setting section");

    // show the encoding in a comment if we have a code emitter.
    if (emitter != null)
      addEncodingComment(inst);

    // show the MCInst if enabled.
    if (showInst) {
      PrintStream cos = getCommentOS();
      cos.printf("<MCInst #%d", inst.getOpcode());
      String instName = null;
      if (instPrinter != null)
        instName = instPrinter.getOpcodeName(inst.getOpcode());
      if (instName != null)
        cos.printf(" %s", instName);

      for (int i = 0, e = inst.getNumOperands(); i < e; i++) {
        cos.print("\n ");
        inst.getOperand(i).print(cos, mai);
      }
      cos.println(">");
    }

    if (instPrinter != null)
      instPrinter.printInst(inst);
    else
      inst.print(os, mai);
    emitEOL();
  }

  @Override
  public void finish() {
    os.flush();
  }
}
