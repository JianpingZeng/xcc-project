package backend.target.arm;
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
import backend.support.Triple;
import backend.target.TargetData;

import static backend.target.arm.ARMMCTargetDesc.parseARMTriple;
import static backend.target.arm.ARMSubtarget.ARMProcFamilyEnum.CortexA8;
import static backend.target.arm.ARMSubtarget.ARMProcFamilyEnum.CortexA9;
import static backend.target.arm.ARMSubtarget.TargetABI.ARM_ABI_AAPCS;
import static backend.target.arm.ARMSubtarget.TargetABI.ARM_ABI_APCS;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ARMSubtarget extends ARMGenSubtarget {
  public enum TargetType {
    isELF, isDarwin
  }

  public enum TargetABI {
    ARM_ABI_APCS,
    ARM_ABI_AAPCS // ARM EABI
  }

  enum ARMProcFamilyEnum {
    Others, CortexA8, CortexA9
  }

  /// ARMProcFamily - ARM processor family: Cortex-A8, Cortex-A9, and others.
  protected ARMProcFamilyEnum ARMProcFamily;
  protected boolean slowFPVMLx;
  protected boolean hasVFPv2;
  protected boolean hasVFPv3;
  protected boolean hasNEON;
  protected boolean hasV4TOps;
  protected boolean hasV5TEOps;
  protected boolean hasV5TOps;
  protected boolean hasV6Ops;
  protected boolean hasV6T2Ops;
  protected boolean hasV7Ops;
  protected boolean isR9Reserved;
  protected boolean noARM;
  protected boolean hasHardwareDivide;
  protected boolean hasT2ExtractPack;
  protected boolean hasDataBarrier;
  protected boolean hasVMLxForwarding;
  protected boolean hasMPExtension;
  protected boolean hasFP16;
  protected boolean hasD16;
  protected boolean hasThumb2;
  protected boolean slowFPBrcc;
  protected boolean fpOnlySP;
  protected boolean pref32BitThumb;
  protected boolean avoidCPSRPartialUpdate;
  protected boolean thumb2DSP;
  protected boolean isMClass;
  protected boolean useMovt;
  protected boolean supportsTailCall;
  protected boolean allowsUnalignedMem;
  protected boolean inThumbMode;
  protected boolean inNaClMode;
  protected boolean postRAScheduler;
  protected boolean useNEONForSinglePrecisionFP;

  protected int stackAlignment;
  protected ARMTargetMachine tm;
  private TargetData datalayout;
  private TargetType targetType;
  private TargetABI targetABI;
  private String cpuString;
  protected InstrItineraryData instrItins;

  public ARMSubtarget(ARMTargetMachine tm, String tt, String cpu, String fs, boolean isThumb) {
    super(tt, cpu, fs);
    subtarget = this;
    this.tm = tm;
    stackAlignment = 4;
    datalayout = new TargetData(subtarget.isAPCS_ABI() ?
        "e-p:32:32-f64:32:64-i64:32:64-v128:32:128-v64:32:64-n32-S32" :
        subtarget.isAAPCS_ABI() ?
            "e-p:32:32-f64:64:64-i64:64:64-v128:64:128-v64:64:64-n32-S64" :
            "e-p:32:32-f64:64:64-i64:64:64-v128:64:128-v64:64:64-n32-S32");
    targetType = TargetType.isELF;
    cpuString = cpu;
    // the abi default to ARM_ABI_APCS
    targetABI = ARM_ABI_APCS;
    
    if (cpuString == null || cpuString.isEmpty())
      cpuString = "generic";

    // Insert the architecture feature derived from the target triple into the
    // feature string. This is important for setting features that are implied
    // based on the architecture version.
    String archFS = parseARMTriple(tt);
    if (fs != null && !fs.isEmpty()) {
      if (!archFS.isEmpty())
        archFS = archFS + "," + fs;
      else
        archFS = fs;
    }

    // parse subtarget features.
    parseSubtargetFeatures(archFS, cpuString);

    if (!hasV6T2Ops && hasThumb2)
      hasV4TOps = hasV5TOps = hasV5TEOps = hasV6Ops = hasV6T2Ops = true;

    // Initialize scheduling itinerary for the specified CPU.
    instrItins = getInstrItineraryForCPU(cpuString);

    // After parsing itineraries, set IssueWidth.
    // computeIssueWidth();

    if (tt.length() > 5) {
      if (tt.contains("-darwin"))
        targetType = TargetType.isDarwin;
    }

    if (tt.contains("eabi"))
      targetABI = ARM_ABI_AAPCS;

    if (isAAPCS_ABI())
      stackAlignment = 8;

    if (!isTargetDarwin()) {
      useMovt = hasV6T2Ops;
    }
    else {
      isR9Reserved = !hasV6Ops;
      useMovt = hasV6T2Ops;
      supportsTailCall = false;
    }

    if (!isThumb() || hasThumb2())
      postRAScheduler = true;

    if (hasV6Ops() && isTargetDarwin())
      allowsUnalignedMem = true;
  }

  public int getStackAlignment() {
    return stackAlignment;
  }

  public TargetData getDataLayout() {
    return datalayout;
  }

  @Override
  public ARMInstrInfo getInstrInfo() {
    if (instrInfo == null)
      instrInfo = ARMInstrInfo.createARMInstrInfo(tm);
    return (ARMInstrInfo) instrInfo;
  }

  @Override
  public ARMRegisterInfo getRegisterInfo() {
    if (regInfo == null)
      regInfo = ARMRegisterInfo.createARMRegisterInfo(tm, getHwMode());
    return (ARMRegisterInfo) regInfo;
  }

  public boolean hasV4TOps()  { return hasV4TOps;  }
  public boolean hasV5TOps()  { return hasV5TOps;  }
  public boolean hasV5TEOps() { return hasV5TEOps; }
  public boolean hasV6Ops()   { return hasV6Ops;   }
  public boolean hasV6T2Ops() { return hasV6T2Ops; }
  public boolean hasV7Ops()   { return hasV7Ops;  }

  public boolean isCortexA8() { return ARMProcFamily == CortexA8; }
  public boolean isCortexA9() { return ARMProcFamily == CortexA9; }

  public boolean hasARMOps() { return !noARM; }

  public boolean hasVFP2() { return hasVFPv2; }
  public boolean hasVFP3() { return hasVFPv3; }
  public boolean hasNEON() { return hasNEON;  }
  public boolean useNEONForSinglePrecisionFP() {
    return hasNEON()& useNEONForSinglePrecisionFP; }

  public boolean hasDivide() { return hasHardwareDivide; }
  public boolean hasT2ExtractPack() { return hasT2ExtractPack; }
  public boolean hasDataBarrier() { return hasDataBarrier; }
  public boolean useFPVMLx() { return !slowFPVMLx; }
  public boolean hasVMLxForwarding() { return hasVMLxForwarding; }
  public boolean isFPBrccSlow() { return slowFPBrcc; }
  public boolean isFPOnlySP() { return fpOnlySP; }
  public boolean prefers32BitThumb() { return pref32BitThumb; }
  public boolean avoidCPSRPartialUpdate() { return avoidCPSRPartialUpdate; }
  public boolean hasMPExtension() { return hasMPExtension; }
  public boolean hasThumb2DSP() { return thumb2DSP; }

  public boolean hasFP16() { return hasFP16; }
  public boolean hasD16() { return hasD16; }

  public boolean isTargetDarwin() { return targetTriple.isOSDarwin(); }
  
  public boolean isTargetNaCl() {
    return targetTriple.getOS() == Triple.OSType.NativeClient;
  }
  public boolean isTargetELF() { return !isTargetDarwin(); }

  public boolean isAPCS_ABI() { return targetABI == ARM_ABI_APCS; }
  public boolean isAAPCS_ABI() { return targetABI == ARM_ABI_AAPCS; }

  public boolean isThumb() { return inThumbMode; }
  public boolean isThumb1Only() { return inThumbMode& !hasThumb2; }
  public boolean isThumb2() { return inThumbMode& hasThumb2; }
  public boolean hasThumb2() { return hasThumb2; }
  public boolean isMClass() { return isMClass; }
  public boolean isARClass() { return !isMClass; }

  public boolean isR9Reserved() { return isR9Reserved; }

  public boolean useMovt() { return useMovt & hasV6T2Ops(); }
  public boolean supportsTailCall() { return supportsTailCall; }

  public boolean allowsUnalignedMem() { return allowsUnalignedMem; }

  public String getCPUString() { return cpuString; }

  protected boolean honorSignDependentRoundingFPMath() {
    // TODO
    return true;
  }
}
