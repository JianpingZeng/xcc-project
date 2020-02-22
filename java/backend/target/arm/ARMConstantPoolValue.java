package backend.target.arm;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import backend.codegen.MachineConstantPool;
import backend.codegen.MachineConstantPoolValue;
import backend.support.LLVMContext;
import backend.type.Type;
import tools.FoldingSetNodeID;
import tools.Util;

import java.io.PrintStream;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ARMConstantPoolValue extends MachineConstantPoolValue {
  static class ARMCP {
    enum ARMCPKind {
      CPValue,
      CPExtSymbol,
      CPBlockAddress,
      CPLSDA,
      CPMachineBasicBlock
    }

    enum ARMCPModifier {
      no_modifier,
      TLSGD,
      GOT,
      GOTOFF,
      GOTTPOFF,
      TPOFF
    }
  }

  /**
   * label id for the load.
   */
  private int labelID;
  /**
   * kind of constant.
   */
  private ARMCP.ARMCPKind kind;
  /**
   * Extra adjustment if constantpool is pc-relative. 8 for ARM, 4 for thumb.
   */
  private int pcAdjust;

  /**
   * GV modifier, i.e. (GV(modifier)-(LPIC+8))
   */
  private ARMCP.ARMCPModifier modifier;

  private boolean addCurrentAddress;

  protected ARMConstantPoolValue(Type ty, int id, ARMCP.ARMCPKind kind,
                                 int pcAdj, ARMCP.ARMCPModifier modifier,
                                 boolean addCurrentAddress) {
    super(ty);
    this.labelID = id;
    this.kind = kind;
    this.pcAdjust = pcAdj;
    this.modifier = modifier;
    this.addCurrentAddress = addCurrentAddress;
  }

  protected ARMConstantPoolValue(LLVMContext ctx, int id,
                                 ARMCP.ARMCPKind kind,
                                 int pcAdj, ARMCP.ARMCPModifier modifier,
                                 boolean addCurrentAddress) {
    this(Type.getInt32Ty(ctx), id, kind, pcAdj, modifier, addCurrentAddress);
  }

  public String getModifierText() {
    switch (modifier) {
      default: Util.assertion("Unknown modifier!");
        // FIXME: Are these case sensitive? It'd be nice to lower-case all the
        // strings if that's legal.
      case no_modifier: return "none";
      case TLSGD:       return "tlsgd";
      case GOT:         return "GOT";
      case GOTOFF:      return "GOTOFF";
      case GOTTPOFF:    return "gottpoff";
      case TPOFF:       return "tpoff";
    }
  }
  
  @Override
  public int getRelocationInfo() {
    return 2;
  }

  public ARMCP.ARMCPModifier getModifier() { return modifier; }
  public String getModifieText() {
    switch (modifier) {
      case no_modifier: return "none";
      case TLSGD: return "tlsgd";
      case GOT: return "GOT";
      case GOTOFF: return "GOTOFF";
      case GOTTPOFF: return "gottpoff";
      case TPOFF: return "tpoff";
      default:
        Util.shouldNotReachHere("Unknown modifier?");
        return "";
    }
  }

  public boolean hasModifier() { return modifier != ARMCP.ARMCPModifier.no_modifier; }
  public boolean mustAddCurrentAddress() { return addCurrentAddress; }
  public int getLabelID() { return labelID; }
  public int getPCAdjust() { return pcAdjust; }
  public boolean isGlobalValue() { return kind == ARMCP.ARMCPKind.CPValue; }
  public boolean isExtSymbol() { return kind == ARMCP.ARMCPKind.CPExtSymbol; }
  public boolean isBlockAddress() { return kind == ARMCP.ARMCPKind.CPBlockAddress; }
  public boolean isLSDA() { return kind == ARMCP.ARMCPKind.CPLSDA; }
  public boolean isMachineBasicBlock() { return kind == ARMCP.ARMCPKind.CPMachineBasicBlock; }

  @Override
  public int getExistingMachineCPValue(MachineConstantPool pool, int align) {
    Util.shouldNotReachHere("should be overrided by sub class");
    return 0;
  }

  @Override
  public void addSelectionDAGCSEId(FoldingSetNodeID id) {
    id.addInteger(labelID);
    id.addInteger(pcAdjust);
  }

  public boolean hasSameValue(ARMConstantPoolValue val) {
    if (val.kind == kind && val.pcAdjust == pcAdjust &&
        val.modifier == modifier) {
      if (labelID == val.labelID)
        return true;

      // Two PC relative constpool entries containing the same GV address or
      // external symbols.
      if (kind == ARMCP.ARMCPKind.CPValue || kind == ARMCP.ARMCPKind.CPExtSymbol)
        return true;
    }
    return false;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (obj == this) return true;
    if (getClass() != obj.getClass()) return false;
    ARMConstantPoolValue cpv = (ARMConstantPoolValue) obj;
    return labelID == cpv.labelID && pcAdjust == cpv.pcAdjust && modifier == cpv.modifier;
  }

  public void dump() {
    System.err.print(" ");
    print(System.err);
  }

  @Override
  public void print(PrintStream os) {
    if (modifier != null)
      os.printf("(%s)", getModifierText());
    if (pcAdjust != 0) {
      os.printf("-(LPC%d+%d", labelID, pcAdjust);
      if (addCurrentAddress)
        os.print("-.");
      os.print(')');
    }
  }
}
