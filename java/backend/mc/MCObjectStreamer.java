/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2015-2019, Jianping Zeng.
 * All rights reserved.
 *
 * Please refer the LICENSE for detail.
 */

package backend.mc;
/**
 * Streaming object file generation interface.
 *
 * This class provides an implementation of the MCStreamer interfaces which
 * is suitable for use with the assembler backend. Specific object file formats
 * are expected to subclas this interface to implement directives sepecific to
 * that file format or custom semantics expected by the object writer implementation.
 * @author Jianping Zeng.
 * @version 0.4
 */
public class MCObjectStreamer extends MCStreamer {
  protected MCObjectStreamer(MCSymbol.MCContext ctx) {
    super(ctx);
  }

  @Override
  public void switchSection(MCSection section) {

  }

  @Override
  public void emitLabel(MCSymbol symbol) {

  }

  @Override
  public void emitAssemblerFlag(MCAsmInfo.MCAssemblerFlag flag) {

  }

  @Override
  public void emitAssignment(MCSymbol sym, MCExpr val) {

  }

  @Override
  public void emitSymbolAttribute(MCSymbol sym, MCAsmInfo.MCSymbolAttr attr) {

  }

  @Override
  public void emitSymbolDesc(MCSymbol sym, int descValue) {

  }

  @Override
  public void emitELFSize(MCSymbol sym, MCExpr val) {

  }

  @Override
  public void emitCommonSymbol(MCSymbol sym, long size, int byteAlignment) {

  }

  @Override
  public void emitLocalCommonSymbol(MCSymbol sym, long size) {

  }

  @Override
  public void emitZeroFill(MCSection section, MCSymbol sym, int size, int byteAlignment) {

  }

  @Override
  public void emitBytes(String data, int addrSpace) {

  }

  @Override
  public void emitValue(MCExpr val, int size, int addrSpace) {

  }

  @Override
  public void emitGPRel32Value(MCExpr val) {

  }

  @Override
  public void emitZeros(long numBytes, int addrSpace) {

  }

  @Override
  public void emitValueToAlignment(int byteAlignment, long value, int valueSize, int maxBytesToEmit) {

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
