package backend.target.x86;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import backend.codegen.MachineFunction;
import backend.codegen.MachineFunctionInfo;

import static backend.target.x86.X86MachineFunctionInfo.NameDecorationStyle.None;

/**
 * This class is derived from MachineFunction and
 * contains private X86 target-specific information for each MachineFunction.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class X86MachineFunctionInfo extends MachineFunctionInfo {
  public enum NameDecorationStyle {
    None, StdCall, FastCall
  }

  /// forceFramePointer - True if the function is required to use of frame
  /// pointer for reasons other than it containing dynamic allocation or
  /// that FP eliminatation is turned off. For example, Cygwin main function
  /// contains stack pointer re-alignment code which requires FP.
  private boolean forceFramePointer;

  /// calleeSavedFrameSize - Size of the callee-saved register portion of the
  /// stack frame in bytes.
  private int calleeSavedFrameSize;

  /// bytesToPopOnReturn - Number of bytes function pops on return.
  /// Used on windows platform for stdcall & fastcall name decoration
  private int bytesToPopOnReturn;

  /// decorationStyle - If the function requires additional name decoration,
  /// decorationStyle holds the right way to do so.
  private NameDecorationStyle decorationStyle;

  /// returnAddrIndex - FrameIndex for return slot.
  private int returnAddrIndex;

  /// tailCallReturnAddrDelta - Delta the ReturnAddr stack slot is moved
  /// Used for creating an area before the register spill area on the stack
  /// the returnaddr can be savely move to this area
  private int tailCallReturnAddrDelta;

  /// sRetReturnReg - Some subtargets require that sret lowering includes
  /// returning the value of the returned struct in a register. This field
  /// holds the virtual register into which the sret argument is passed.
  private int sRetReturnReg;

  /// globalBaseReg - keeps track of the virtual register initialized for
  /// use as the global base register. This is used for PIC in some PIC
  /// relocation models.
  private int globalBaseReg;

  public X86MachineFunctionInfo() {
    this(null);
  }

  public X86MachineFunctionInfo(MachineFunction mf) {
    forceFramePointer = false;
    calleeSavedFrameSize = 0;
    bytesToPopOnReturn = 0;
    decorationStyle = None;
    returnAddrIndex = 0;
    tailCallReturnAddrDelta = 0;
    sRetReturnReg = 0;
    globalBaseReg = 0;
  }

  public boolean getForceFramePointer() {
    return forceFramePointer;
  }

  public void setForceFramePointer(boolean forceFP) {
    forceFramePointer = forceFP;
  }

  public int getCalleeSavedFrameSize() {
    return calleeSavedFrameSize;
  }

  public void setCalleeSavedFrameSize(int bytes) {
    calleeSavedFrameSize = bytes;
  }

  public int getBytesToPopOnReturn() {
    return bytesToPopOnReturn;
  }

  public void setBytesToPopOnReturn(int bytes) {
    bytesToPopOnReturn = bytes;
  }

  public NameDecorationStyle getDecorationStyle() {
    return decorationStyle;
  }

  public void setDecorationStyle(NameDecorationStyle style) {
    decorationStyle = style;
  }

  public int getRAIndex() {
    return returnAddrIndex;
  }

  public void setRAIndex(int Index) {
    returnAddrIndex = Index;
  }

  public int getTCReturnAddrDelta() {
    return tailCallReturnAddrDelta;
  }

  public void setTCReturnAddrDelta(int delta) {
    tailCallReturnAddrDelta = delta;
  }

  public int getSRetReturnReg() {
    return sRetReturnReg;
  }

  public void setSRetReturnReg(int Reg) {
    sRetReturnReg = Reg;
  }

  public int getGlobalBaseReg() {
    return globalBaseReg;
  }

  public void setGlobalBaseReg(int Reg) {
    globalBaseReg = Reg;
  }
}
