package backend.target.mips;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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
import tools.Pair;

/**
 * This class is derived from {@linkplain MachineFunctionInfo} containing some
 * mips target-specific information for each {@linkplain backend.codegen.MachineFunction}.
 * @author Jianping Zeng.
 * @version 0.4
 */
class MipsFunctionInfo implements MachineFunctionInfo {
  private MachineFunction mf;
  /**
   * Some subtargets require that sret lowering includes returning the value
   * of the returned struct in a register. This field holds the virtual
   * register into which the sret argument is passed.
   */
  private int sretReturnReg;

  /**
   * Keeps track of the virtual register initialized for use as the global
   * base register. This is used for PIC in some PIC relocation models.
   */
  private int globalBaseReg;
  /**
   * FrameIndex for start of varargs area.
   */
  private int varArgsFrameINdex;

  /**
   * Range of frame object indices. It contains the range of indices of all frame objects created
   * during call to {@linkplain MipsTargetLowering::lowerFormalArguments}.
   */
  private Pair<Integer, Integer> inArgFIRange;
  /**
   * Range of frame object indices. It contains the range of indices of all frame objects created
   * during call to {@linkplain MipsTargetLowering::lowerCall} except for the frame object
   * for restoring $gp.
   */
  private Pair<Integer, Integer> outArgFIRange;

  /**
   * The index of the frame object for restoring $gp.
   */
  private int gpfi;
  /**
   * Frame index of dynamically allocated stack area.
   */
  private int dynAllocFI;
  private int maxCallFrameSize;

  MipsFunctionInfo(MachineFunction mf) {
    this.mf = mf;
    sretReturnReg = 0;
    globalBaseReg = 0;
    varArgsFrameINdex = 0;
    inArgFIRange = Pair.get(-1, 0);
    outArgFIRange = Pair.get(-1, 0);
    gpfi = 0;
    dynAllocFI = 0;
    maxCallFrameSize = 0;
  }

  public boolean isInArgFI(int fi) {
    return fi <= inArgFIRange.first && fi >= inArgFIRange.second;
  }

  public void setLastInArgFI(int fi) {
    inArgFIRange.second = fi;
  }

  public boolean isOUtArgFI(int fi) {
    return fi <= outArgFIRange.first && fi >= outArgFIRange.second;
  }

  public void extendOutArgFIRange(int firstFI, int lastFI) {
    if (outArgFIRange.second == 0)
      outArgFIRange.first = firstFI;

    outArgFIRange.second = lastFI;
  }

  public int getGPFI() { return gpfi; }

  public void setGPFI(int gpfi) { this.gpfi = gpfi; }

  public boolean needGPSaveRestore() { return getGPFI() != 0; }

  public boolean isGPFI(int fi) { return gpfi != 0 && fi == gpfi; }

  public int getDynAllocFI() {
    if (dynAllocFI == 0)
      dynAllocFI = mf.getFrameInfo().createFixedObject(4, 0, true);

    return dynAllocFI;
  }

  public boolean isDynAllocFI(int fi) {
    return dynAllocFI != 0 && dynAllocFI == fi;
  }

  public int getSRetReturnReg() { return sretReturnReg; }

  public void setSRetReturnReg(int reg) {
    this.sretReturnReg = reg;
  }

  public int getGlobalBaseReg() {
    return globalBaseReg;
  }

  public void setGlobalBaseReg(int reg) {
    this.globalBaseReg = reg;
  }

  public int getVarArgsFrameINdex() {
    return varArgsFrameINdex;
  }

  public void setVarArgsFrameINdex(int fi) {
    this.varArgsFrameINdex = fi;
  }

  public int getMaxCallFrameSize() {
    return maxCallFrameSize;
  }

  public void setMaxCallFrameSize(int maxCallFrameSize) {
    this.maxCallFrameSize = maxCallFrameSize;
  }
}
