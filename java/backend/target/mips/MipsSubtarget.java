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

import backend.target.TargetSubtarget;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class MipsSubtarget extends TargetSubtarget {
  public boolean hasBitCount;
  public boolean hasCondMov;
  public boolean isFP64bit;
  public boolean isGP64bit;
  public boolean hasMinMax;
  public boolean hasMulDivAdd;
  public boolean hasSEInReg;
  public boolean isSingleFloat;
  public boolean hasSwap;
  public boolean hasVFPU;

  public boolean isNotSingleFloat() {
    return !isSingleFloat;
  }

  public boolean isSingleFloat() {
    return isSingleFloat;
  }

  public boolean isABI_EABI() {
    return mipsABI == MipsABIEnum.EABI;
  }

  public boolean isABI_N32() {
    return mipsABI == MipsABIEnum.N32;
  }

  // NOTE: O64 will not be supported.
  enum MipsABIEnum {
    UnknownABI, O32, N32, N64, EABI
  }

  enum MipsArchEnum {
    Mips32, Mips32r2, Mips64, Mips64r2
  };

  // Mips architecture version
  MipsArchEnum mipsArchVersion;

  // Mips supported ABIs
  MipsABIEnum mipsABI;

  @Override
  public void parseSubtargetFeatures(String fs, String cpu) {

  }

  public boolean isABI_N64() {
    return false;
  }

  public boolean hasMips64() {
    return false;
  }

  public boolean isFP64bit() {
    return false;
  }

  public boolean hasBitCount() {
    return false;
  }

  public boolean hasMips32r2() {
    return false;
  }

  public boolean hasMips64r2() {
    return false;
  }

  public boolean hasSEInReg() {
    return false;
  }

  public boolean hasSwap() {
    return false;
  }

  public boolean hasMips32() {
    return false;
  }
}
