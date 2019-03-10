/*
 *  Extremely C language Compiler
 *    Copyright (c) 2015-2018, Jianping Zeng.
 *
 *  Licensed under the BSD License version 3. Please refer LICENSE for details.
 */

package backend.mc;

public class MCNullStreamer extends MCStreamer {
  public static MCStreamer createNullStreamer(MCSymbol.MCContext ctx) {
    return new MCNullStreamer(ctx);
  }

  protected MCNullStreamer(MCSymbol.MCContext ctx) {
    super(ctx);
  }

  @Override
  public void switchSection(MCSection section) {
    curSection = section;
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
  public void emitIntValue(long val, int size, int addrSpace) {

  }

  @Override
  public void emitGPRel32Value(MCExpr val) {

  }

  @Override
  public void emitFill(long numBytes, int fillValue, int addrSpace) {

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
