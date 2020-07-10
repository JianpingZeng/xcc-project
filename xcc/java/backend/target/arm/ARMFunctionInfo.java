package backend.target.arm;
/*
 * Extremely Compiler Collection
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

import backend.codegen.MachineFunction;
import backend.codegen.MachineFunctionInfo;
import tools.BitMap;
import tools.Util;

import java.util.TreeMap;

/**
 * This class is derived from MachineFunctionInfo and
 * contains private ARM-specific information for each MachineFunction.
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ARMFunctionInfo implements MachineFunctionInfo {
  /// isThumb - True if this function is compiled under Thumb mode.
  /// Used to initialized Align, so must precede it.
  private boolean isThumb;

  /// hasThumb2 - True if the target architecture supports Thumb2. Do not use
  /// to determine if function is compiled under Thumb mode, for that use
  /// 'isThumb'.
  private boolean hasThumb2;

  /// VarArgsRegSaveSize - Size of the register save area for vararg functions.
  ///
  private int VarArgsRegSaveSize;

  /// HasStackFrame - True if this function has a stack frame. Set by
  /// processFunctionBeforeCalleeSavedScan().
  private boolean HasStackFrame;

  /// RestoreSPFromFP - True if epilogue should restore SP from FP. Set by
  /// emitPrologue.
  private boolean RestoreSPFromFP;

  /// LRSpilledForFarJump - True if the LR register has been for spilled to
  /// enable far jump.
  private boolean LRSpilledForFarJump;

  /// FramePtrSpillOffset - If HasStackFrame, this records the frame pointer
  /// spill stack offset.
  private int FramePtrSpillOffset;

  /// GPRCS1Offset, GPRCS2Offset, DPRCSOffset - Starting offset of callee saved
  /// register spills areas. For Mac OS X:
  ///
  /// GPR callee-saved (1) : r4, r5, r6, r7, lr
  /// --------------------------------------------
  /// GPR callee-saved (2) : r8, r10, r11
  /// --------------------------------------------
  /// DPR callee-saved : d8 - d15
  private int GPRCS1Offset;
  private int GPRCS2Offset;
  private int DPRCSOffset;

  /// GPRCS1Size, GPRCS2Size, DPRCSSize - Sizes of callee saved register spills
  /// areas.
  private int GPRCS1Size;
  private int GPRCS2Size;
  private int DPRCSSize;

  /// GPRCS1Frames, GPRCS2Frames, DPRCSFrames - Keeps track of frame indices
  /// which belong to these spill areas.
  private BitMap GPRCS1Frames;
  private BitMap GPRCS2Frames;
  private BitMap DPRCSFrames;

  /// JumpTableUId - Unique id for jumptables.
  ///
  private int JumpTableUId;

  private int PICLabelUId;

  /// VarArgsFrameIndex - FrameIndex for start of varargs area.
  int VarArgsFrameIndex;

  /// HasITBlocks - True if IT blocks have been inserted.
  private boolean HasITBlocks;

  public ARMFunctionInfo() {}

  public ARMFunctionInfo(MachineFunction MF) {
    isThumb = ((ARMSubtarget)MF.getTarget().getSubtarget()).isThumb();
    hasThumb2 = ((ARMSubtarget)MF.getTarget().getSubtarget()).hasThumb2();
    VarArgsRegSaveSize = 0;
    HasStackFrame = false;
    RestoreSPFromFP = false;
    LRSpilledForFarJump = false;
    FramePtrSpillOffset = 0; 
    GPRCS1Offset = 0;
    GPRCS2Offset = 0;
    DPRCSOffset = 0;
    GPRCS1Size = 0;
    GPRCS2Size = 0;
    DPRCSSize = 0;
    GPRCS1Frames = new BitMap(32);
    GPRCS2Frames = new BitMap(32);
    DPRCSFrames = new BitMap(32);
    JumpTableUId = 0;
    PICLabelUId = 0;
    VarArgsFrameIndex = 0;
    HasITBlocks = false;
  }

  public boolean isThumbFunction() { return isThumb; }
  public boolean isThumb1OnlyFunction() { return isThumb && !hasThumb2; }
  public boolean isThumb2Function() { return isThumb && hasThumb2; }

  public int getVarArgsRegSaveSize() { return VarArgsRegSaveSize; }
  public void setVarArgsRegSaveSize(int s) { VarArgsRegSaveSize = s; }

  public boolean hasStackFrame() { return HasStackFrame; }
  public void setHasStackFrame(boolean s) { HasStackFrame = s; }

  public boolean shouldRestoreSPFromFP() { return RestoreSPFromFP; }
  public void setShouldRestoreSPFromFP(boolean s) { RestoreSPFromFP = s; }

  public boolean isLRSpilledForFarJump() { return LRSpilledForFarJump; }
  public void setLRIsSpilledForFarJump(boolean s) { LRSpilledForFarJump = s; }

  public int getFramePtrSpillOffset() { return FramePtrSpillOffset; }
  public void setFramePtrSpillOffset(int o) { FramePtrSpillOffset = o; }

  public int getGPRCalleeSavedArea1Offset() { return GPRCS1Offset; }
  public int getGPRCalleeSavedArea2Offset() { return GPRCS2Offset; }
  public int getDPRCalleeSavedAreaOffset()  { return DPRCSOffset; }

  public void setGPRCalleeSavedArea1Offset(int o) { GPRCS1Offset = o; }
  public void setGPRCalleeSavedArea2Offset(int o) { GPRCS2Offset = o; }
  public void setDPRCalleeSavedAreaOffset(int o)  { DPRCSOffset = o; }

  public int getGPRCalleeSavedArea1Size() { return GPRCS1Size; }
  public int getGPRCalleeSavedArea2Size() { return GPRCS2Size; }
  public int getDPRCalleeSavedAreaSize()  { return DPRCSSize; }

  public void setGPRCalleeSavedArea1Size(int s) { GPRCS1Size = s; }
  public void setGPRCalleeSavedArea2Size(int s) { GPRCS2Size = s; }
  public void setDPRCalleeSavedAreaSize(int s)  { DPRCSSize = s; }

  public boolean isGPRCalleeSavedArea1Frame(int fi) {
    if (fi < 0 || fi >= (int)GPRCS1Frames.size())
      return false;
    return GPRCS1Frames.get(fi);
  }
  public boolean isGPRCalleeSavedArea2Frame(int fi) {
    if (fi < 0 || fi >= (int)GPRCS2Frames.size())
      return false;
    return GPRCS2Frames.get(fi);
  }
  public boolean isDPRCalleeSavedAreaFrame(int fi) {
    if (fi < 0 || fi >= (int)DPRCSFrames.size())
      return false;
    return DPRCSFrames.get(fi);
  }

  public void addGPRCalleeSavedArea1Frame(int fi) {
    if (fi >= 0) {
      GPRCS1Frames.set(fi, true);
    }
  }
  public void addGPRCalleeSavedArea2Frame(int fi) {
    if (fi >= 0) {
      GPRCS2Frames.set(fi, true);
    }
  }
  public void addDPRCalleeSavedAreaFrame(int fi) {
    if (fi >= 0) {
      DPRCSFrames.set(fi, true);
    }
  }

  public int createJumpTableUId() {
    return JumpTableUId++;
  }

  public int getNumJumpTables() {
    return JumpTableUId;
  }

  public void initPICLabelUId(int UId) {
    PICLabelUId = UId;
  }

  public int getNumPICLabels() {
    return PICLabelUId;
  }

  public int createPICLabelUId() {
    return PICLabelUId++;
  }

  public int getVarArgsFrameIndex() { return VarArgsFrameIndex; }
  public void setVarArgsFrameIndex(int Index) { VarArgsFrameIndex = Index; }

  public boolean hasITBlocks() { return HasITBlocks; }
  public void setHasITBlocks(boolean h) { HasITBlocks = h; }
}
