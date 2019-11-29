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

import backend.mc.InstrItineraryData;
import tools.Util;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class MipsSubtarget extends MipsGenSubtarget {
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
  public boolean isLittle;
  public boolean isLinux;
  private InstrItineraryData instrItins;
  private MipsTargetMachine tm;

  public MipsSubtarget(MipsTargetMachine tm, String tt, String cpu, String fs, boolean isLittle) {
    super(tt, cpu, fs);
    this.tm = tm;
    mipsArchVersion = MipsArchEnum.Mips32;
    mipsABI = MipsABIEnum.UnknownABI;
    this.isLittle = isLittle;
    isSingleFloat = false;
    isFP64bit = false;
    isGP64bit = false;
    hasVFPU = false;
    isLinux = true;
    hasSEInReg = false;
    hasCondMov = false;
    hasMulDivAdd = false;
    hasMinMax = false;
    hasSwap = false;
    hasBitCount = false;
    subtarget = this;

    String cpuName = cpu;
    if (cpuName == null || cpuName.isEmpty())
      cpuName = "mips32r1";

    parseSubtargetFeatures(cpuName, fs);
    instrItins = getInstrItineraryForCPU(cpuName);

    // Set mips abi if it hasn't been set.
    if (mipsABI == MipsABIEnum.UnknownABI)
      mipsABI = hasMips64() ? MipsABIEnum.N64 : MipsABIEnum.O32;

    // Check if Architecture and ABI are compatible.
    Util.assertion(((!hasMips64() && (isABI_O32() || isABI_EABI())) ||
        (hasMips64() && (isABI_N32() || isABI_N64()))),
        "Invalid  Arch & ABI pair.");

    if (!tt.contains("linux"))
      isLinux = false;
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

  public boolean isFP64bit() {
    return isFP64bit;
  }

  public boolean isGP64bit() {
    return isGP64bit;
  }

  public boolean isGP32bit() {
    return !isGP64bit;
  }

  public boolean hasVFPU() {
    return hasVFPU;
  }

  public boolean isLinux() {
    return isLinux;
  }
  public boolean hasBitCount() {
    return hasBitCount;
  }

  public boolean hasSEInReg() {
    return hasSEInReg;
  }

  public boolean hasSwap() {
    return hasSwap;
  }

  public boolean hasCondMov() {
    return hasCondMov;
  }

  public boolean hasMulDivAdd() {
    return hasMulDivAdd;
  }

  public boolean hasMinMax() {
    return hasMinMax;
  }

  public boolean hasMips32() {
    return mipsArchVersion.compareTo(MipsArchEnum.Mips32) >= 0;
  }

  public boolean hasMips32r2() {
    return mipsArchVersion == MipsArchEnum.Mips32r2 ||
        mipsArchVersion == MipsArchEnum.Mips64r2;
  }

  public boolean hasMips64() {
    return mipsArchVersion.compareTo(MipsArchEnum.Mips64) >= 0;
  }

  public boolean hasMips64r2() {
    return mipsArchVersion == MipsArchEnum.Mips64r2;
  }

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

  public boolean isABI_N64() {
    return mipsABI == MipsABIEnum.N64;
  }

  public boolean isABI_O32() {
    return mipsABI == MipsABIEnum.O32;
  }

  public MipsABIEnum getTargetABI() { return mipsABI; }

  public boolean isLittleEndian() { return isLittle; }

  @Override
  public MipsInstrInfo getInstrInfo() {
    if (instrInfo == null)
      instrInfo = new MipsInstrInfo(tm);
    return (MipsInstrInfo) instrInfo;
  }

  @Override
  public MipsRegisterInfo getRegisterInfo() {
    if (regInfo == null)
      regInfo = MipsRegisterInfo.createMipsRegisterInfo(tm);
    return (MipsRegisterInfo) regInfo;
  }
}
