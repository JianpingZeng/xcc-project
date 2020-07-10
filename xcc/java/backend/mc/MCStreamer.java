/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */
package backend.mc;

import backend.value.GlobalVariable;

import java.io.PrintStream;

/**
 * Streaming machine code generation interface.  This interface
 * is intended to provide a programatic interface that is very similar to the
 * level that an assembler .s file provides.  It has callbacks to emit bytes,
 * handle directives, etc.  The implementation of this interface retains
 * state to know what the current section is etc.
 * <br></br>
 * There are multiple implementations of this interface: one for writing out
 * a .s file, and implementations that write out .o files of various formats.
 * @author Jianping Zeng
 * @version 0.4
 */
public abstract class MCStreamer {
  private MCSymbol.MCContext context;
  protected MCSection curSection;

  protected MCStreamer(MCSymbol.MCContext ctx) {
    context = ctx;
    curSection = null;
  }

  public MCSymbol.MCContext getContext() {
    return context;
  }

  /**
   * Return true if this streamer supports verbose assembly at all.
   * @return
   */
  public boolean isVerboseAsm() { return false; }

  /**
   * Return true if this asm streamer supports emitting
   * unformatted text to the .s file with EmitRawText.
   * @return
   */
  public boolean hasRawTextSupport() { return false; }

  /**
   * If this file is backed by a assembly streamer, this dumps
   * the specified string in the output .s file. This capability is
   * indicated by the {@linkplain this::hasRawTextSupport()}.
   * @param str
   */
  public void emitRawText(String str) { }

  /** Add a comment that can be emitted to the generated .s
   * file if applicable as a QoI issue to make the output of the compiler
   * more readable.  This only affects the MCAsmStreamer, and only when
   * verbose assembly output is enabled.
   *
   * If the comment includes embedded \n's, they will each get the comment
   * prefix as appropriate.  The added comment should not end with a \n.
   */
  public void addComment(String str) {}

  /**
   * Return a raw_ostream that comments can be written to.
   * Unlike AddComment, you are required to terminate comments with \n if you
   * use this method.
   * @return
   */
  public PrintStream getCommentOS() { return null; }

  /**
   * Emit a blank line to a .s file to pretty it up.
   */
  public void addBlankLine() {}

  public MCSection getCurrentSection() {
    return curSection;
  }

  public abstract void switchSection(MCSection section);

  public abstract void emitLabel(MCSymbol symbol);

  public abstract void emitAssemblerFlag(MCAsmInfo.MCAssemblerFlag flag);

  public abstract void emitAssignment(MCSymbol sym, MCExpr val);

  public abstract void emitSymbolAttribute(MCSymbol sym, MCAsmInfo.MCSymbolAttr attr);

  public abstract void emitSymbolDesc(MCSymbol sym, int descValue);

  public abstract void emitELFSize(MCSymbol sym, MCExpr val);

  public abstract void emitCommonSymbol(MCSymbol sym, long size, int byteAlignment);

  public abstract void emitLocalCommonSymbol(MCSymbol sym, long size);

  public abstract void emitZeroFill(MCSection section, MCSymbol sym, int size, int byteAlignment);

  public abstract void emitBytes(String data, int addrSpace);

  public abstract void emitValue(MCExpr val, int size,  int addrSpace);

  public void emitIntValue(long val, int size, int addrSpace) {
    emitValue(MCConstantExpr.create(val, getContext()), size, addrSpace);
  }

  public abstract void emitGPRel32Value(MCExpr val);

  public void emitFill(long numBytes, int fillValue, int addrSpace) {
    MCExpr ce = MCConstantExpr.create(fillValue, getContext());
    for (int i = 0; i < numBytes; i++) {
      emitValue(ce, 1, addrSpace);
    }
  }

  public abstract void emitZeros(long numBytes, int addrSpace);

  public abstract void emitValueToAlignment(int byteAlignment, long value,
                                            int valueSize,
                                            int maxBytesToEmit);

  public abstract void emitCodeAlignment(int byteAlignment, int maxBytesToEmit);

  public abstract void emitValueToOffset(MCExpr offset, int value);

  public abstract void emitFileDirective(String filename);

  public abstract void emitDwarfFileDirective(int fileNo, String filename);

  public abstract void emitInstruction(MCInst inst);

  public abstract void finish();

  public String getRegisterName(int reg) {
    return "";
  }

  public abstract void emitThumbFunc(MCSymbol func);

  /**
   * Indicator of whether the previous data-or-code indicator was for
   * code or not.  Used to determine when we need to emit a new indicator.
   */
  public enum MCDataRegionType {
    /**
     * .data_region
     */
    MCDR_DataRegion,
    /**
     * .data_region jt8
     */
    MCDR_DataRegionJT8,
    /**
     * .data_region jt16
     */
    MCDR_DataRegionJT16,
    /**
     * .data_region jt32
     */
    MCDR_DataRegionJT32,
    /**
     * .end_data_region
     */
    MCDR_DataRegionEnd
  }

  /**
   * Specify the kind of data-in-code region
   * @param kind
   */
  public void emitDataRegion(MCDataRegionType kind) {}
}
