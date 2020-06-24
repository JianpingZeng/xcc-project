/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.target.x86;

import backend.codegen.dagisel.SDNode.RegisterSDNode;
import backend.codegen.dagisel.SDValue;
import backend.value.Constant;
import backend.value.GlobalValue;

public class X86ISelAddressMode implements Cloneable {
  enum BaseType {
    RegBase, FrameIndexBase
  }

  BaseType baseType;

  static class Base {
    SDValue reg;
    int frameIndex;

    Base() {
      reg = new SDValue();
      frameIndex = 0;
    }
  }

  Base base;

  int scale;
  SDValue indexReg;
  int disp;
  SDValue segment;
  GlobalValue gv;
  Constant cp;
  String externalSym;
  int jti;
  int align;
  int symbolFlags;

  public X86ISelAddressMode() {
    baseType = BaseType.RegBase;
    scale = 1;
    indexReg = new SDValue();
    disp = 0;
    segment = new SDValue();
    gv = null;
    cp = null;
    externalSym = null;
    jti = -1;
    align = 0;
    symbolFlags = 0;
    base = new Base();
  }

  public boolean hasSymbolicDisplacement() {
    return gv != null || cp != null || externalSym != null || jti != -1;
  }

  public boolean hasBaseOrIndexReg() {
    return indexReg.getNode() != null || base.reg.getNode() != null;
  }

  public boolean isRIPRelative() {
    if (baseType != BaseType.RegBase) return false;
    RegisterSDNode regNode = base.reg.getNode() instanceof RegisterSDNode ?
        (RegisterSDNode) base.reg.getNode() : null;
    if (regNode != null)
      return regNode.getReg() == X86GenRegisterNames.RIP;
    return false;
  }

  public void setBaseReg(SDValue reg) {
    baseType = BaseType.RegBase;
    base.reg = reg;
  }

  public void dump() {
    System.err.printf("X86ISelAddressMode %d%n", hashCode());
    System.err.print("Base.Reg ");
    if (base.reg.getNode() != null)
      base.reg.getNode().dump();
    else
      System.err.print("null");
    System.err.printf(" Base.FrameIndex %d%n", base.frameIndex);
    System.err.printf(" Scale %d%n", scale);
    System.err.print("IndexReg ");
    if (indexReg.getNode() != null)
      indexReg.getNode().dump();
    else
      System.err.print("null");
    System.err.printf(" Disp %d%n", disp);
    System.err.print("GV ");
    if (gv != null)
      gv.dump();
    else
      System.err.print("null");
    System.err.print(" CP ");
    if (cp != null)
      cp.dump();
    else
      System.err.print("null");
    System.err.println();
    System.err.print("ES ");
    if (externalSym != null)
      System.err.print(externalSym);
    else
      System.err.print("null");
    System.err.printf(" JTI %d Align %d%n", jti, align);
  }

  @Override
  public X86ISelAddressMode clone() {
    X86ISelAddressMode res = new X86ISelAddressMode();
    res.baseType = this.baseType;
    res.base = new Base();
    res.base.frameIndex = this.base.frameIndex;
    res.base.reg = this.base.reg.clone();
    res.scale = this.scale;
    res.indexReg = this.indexReg.clone();
    res.disp = this.disp;
    res.segment = this.segment.clone();
    res.gv = this.gv;
    res.cp = this.cp;
    res.externalSym = this.externalSym;
    res.jti = this.jti;
    res.align = this.align;
    res.symbolFlags = this.symbolFlags;
    return res;
  }

  public void setValuesFrom(X86ISelAddressMode am) {
    this.base = am.base;
    this.indexReg = am.indexReg;
    this.disp = am.disp;
    this.baseType = am.baseType;
    this.scale = am.scale;
    this.symbolFlags = am.symbolFlags;
    this.segment = am.segment;
    this.gv = am.gv;
    this.externalSym = am.externalSym;
    this.cp = am.cp;
    this.jti = am.jti;
    this.align = am.align;
  }
}