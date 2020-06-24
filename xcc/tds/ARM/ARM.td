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

 include "../Target.td"

//===----------------------------------------------------------------------===//
// ARM Subtarget state.
//

def ModeThumb  : SubtargetFeature<"thumb-mode", "inThumbMode", "true",
                                  "Thumb mode">;

def ModeNaCl   : SubtargetFeature<"nacl-mode", "inNaClMode", "true",
                                  "Native client mode">;

//===----------------------------------------------------------------------===//
// ARM Subtarget features.
//

def FeatureVFP2 : SubtargetFeature<"vfp2", "hasVFPv2", "true",
                                   "Enable VFP2 instructions">;
def FeatureVFP3 : SubtargetFeature<"vfp3", "hasVFPv3", "true",
                                   "Enable VFP3 instructions",
                                   [FeatureVFP2]>;
def FeatureNEON : SubtargetFeature<"neon", "hasNEON", "true",
                                   "Enable NEON instructions",
                                   [FeatureVFP3]>;
def FeatureThumb2 : SubtargetFeature<"thumb2", "hasThumb2", "true",
                                     "Enable Thumb2 instructions">;
def FeatureNoARM  : SubtargetFeature<"noarm", "noARM", "true",
                                     "Does not support ARM mode execution">;
def FeatureFP16   : SubtargetFeature<"fp16", "hasFP16", "true",
                                     "Enable half-precision floating point">;
def FeatureD16    : SubtargetFeature<"d16", "hasD16", "true",
                                     "Restrict VFP3 to 16 double registers">;
def FeatureHWDiv  : SubtargetFeature<"hwdiv", "hasHardwareDivide", "true",
                                     "Enable divide instructions">;
def FeatureT2XtPk : SubtargetFeature<"t2xtpk", "hasT2ExtractPack", "true",
                                 "Enable Thumb2 extract and pack instructions">;
def FeatureDB     : SubtargetFeature<"db", "hasDataBarrier", "true",
                                   "Has data barrier (dmb / dsb) instructions">;
def FeatureSlowFPBrcc : SubtargetFeature<"slow-fp-brcc", "slowFPBrcc", "true",
                                         "FP compare + branch is slow">;
def FeatureVFPOnlySP : SubtargetFeature<"fp-only-sp", "fpOnlySP", "true",
                          "Floating point unit supports single precision only">;

// Some processors have FP multiply-accumulate instructions that don't
// play nicely with other VFP / NEON instructions, and it's generally better
// to just not use them.
def FeatureHasSlowFPVMLx : SubtargetFeature<"slowfpvmlx", "slowFPVMLx", "true",
                                         "Disable VFP / NEON MAC instructions">;

// Cortex-A8 / A9 Advanced SIMD has multiplier accumulator forwarding.
def FeatureVMLxForwarding : SubtargetFeature<"vmlx-forwarding",
                                       "hasVMLxForwarding", "true",
                                       "Has multiplier accumulator forwarding">;

// Some processors benefit from using NEON instructions for scalar
// single-precision FP operations.
def FeatureNEONForFP : SubtargetFeature<"neonfp", "useNEONForSinglePrecisionFP",
                                        "true",
                                        "Use NEON for single precision FP">;

// Disable 32-bit to 16-bit narrowing for experimentation.
def FeaturePref32BitThumb : SubtargetFeature<"32bit", "pref32BitThumb", "true",
                                             "Prefer 32-bit Thumb instrs">;

/// Some instructions update CPSR partially, which can add false dependency for
/// out-of-order implementation, e.g. Cortex-A9, unless each individual bit is
/// mapped to a separate physical register. Avoid partial CPSR update for these
/// processors.
def FeatureAvoidPartialCPSR : SubtargetFeature<"avoid-partial-cpsr",
                                               "avoidCPSRPartialUpdate", "true",
                                 "Avoid CPSR partial update for OOO execution">;

/// Some M architectures don't have the DSP extension (v7E-M vs. v7M)
def FeatureDSPThumb2 : SubtargetFeature<"t2dsp", "thumb2DSP", "true",
                                 "Supports v7 DSP instructions in Thumb2">;

// Multiprocessing extension.
def FeatureMP : SubtargetFeature<"mp", "hasMPExtension", "true",
                                 "Supports Multiprocessing extension">;

// M-series ISA?
def FeatureMClass : SubtargetFeature<"mclass", "isMClass", "true",
                                     "Is microcontroller profile ('M' series)">;

// ARM ISAs.
def HasV4TOps   : SubtargetFeature<"v4t", "hasV4TOps", "true",
                                   "Support ARM v4T instructions">;
def HasV5TOps   : SubtargetFeature<"v5t", "hasV5TOps", "true",
                                   "Support ARM v5T instructions",
                                   [HasV4TOps]>;
def HasV5TEOps  : SubtargetFeature<"v5te", "hasV5TEOps", "true",
                             "Support ARM v5TE, v5TEj, and v5TExp instructions",
                                   [HasV5TOps]>;
def HasV6Ops    : SubtargetFeature<"v6", "hasV6Ops", "true",
                                   "Support ARM v6 instructions",
                                   [HasV5TEOps]>;
def HasV6T2Ops  : SubtargetFeature<"v6t2", "hasV6T2Ops", "true",
                                   "Support ARM v6t2 instructions",
                                   [HasV6Ops, FeatureThumb2]>;
def HasV7Ops    : SubtargetFeature<"v7", "hasV7Ops", "true",
                                   "Support ARM v7 instructions",
                                   [HasV6T2Ops]>;

//===----------------------------------------------------------------------===//
// ARM Processors supported.
//

include "ARMSchedule.td"

// ARM processor families.
def ProcA8      : SubtargetFeature<"a8", "ARMProcFamily", "ARMProcFamilyEnum.CortexA8",
                                   "Cortex-A8 ARM processors",
                                   [FeatureSlowFPBrcc, FeatureNEONForFP,
                                    FeatureHasSlowFPVMLx, FeatureVMLxForwarding,
                                    FeatureT2XtPk]>;
def ProcA9      : SubtargetFeature<"a9", "ARMProcFamily", "ARMProcFamilyEnum.CortexA9",
                                   "Cortex-A9 ARM processors",
                                   [FeatureVMLxForwarding,
                                    FeatureT2XtPk, FeatureFP16,
                                    FeatureAvoidPartialCPSR]>;

class ProcNoItin<string Name, list<SubtargetFeature> Features>
 : Processor<Name, GenericItineraries, Features>;

// V4 Processors.
def : ProcNoItin<"generic",         []>;
def : ProcNoItin<"arm8",            []>;
def : ProcNoItin<"arm810",          []>;
def : ProcNoItin<"strongarm",       []>;
def : ProcNoItin<"strongarm110",    []>;
def : ProcNoItin<"strongarm1100",   []>;
def : ProcNoItin<"strongarm1110",   []>;

// V4T Processors.
def : ProcNoItin<"arm7tdmi",        [HasV4TOps]>;
def : ProcNoItin<"arm7tdmi-s",      [HasV4TOps]>;
def : ProcNoItin<"arm710t",         [HasV4TOps]>;
def : ProcNoItin<"arm720t",         [HasV4TOps]>;
def : ProcNoItin<"arm9",            [HasV4TOps]>;
def : ProcNoItin<"arm9tdmi",        [HasV4TOps]>;
def : ProcNoItin<"arm920",          [HasV4TOps]>;
def : ProcNoItin<"arm920t",         [HasV4TOps]>;
def : ProcNoItin<"arm922t",         [HasV4TOps]>;
def : ProcNoItin<"arm940t",         [HasV4TOps]>;
def : ProcNoItin<"ep9312",          [HasV4TOps]>;

// V5T Processors.
def : ProcNoItin<"arm10tdmi",       [HasV5TOps]>;
def : ProcNoItin<"arm1020t",        [HasV5TOps]>;

// V5TE Processors.
def : ProcNoItin<"arm9e",           [HasV5TEOps]>;
def : ProcNoItin<"arm926ej-s",      [HasV5TEOps]>;
def : ProcNoItin<"arm946e-s",       [HasV5TEOps]>;
def : ProcNoItin<"arm966e-s",       [HasV5TEOps]>;
def : ProcNoItin<"arm968e-s",       [HasV5TEOps]>;
def : ProcNoItin<"arm10e",          [HasV5TEOps]>;
def : ProcNoItin<"arm1020e",        [HasV5TEOps]>;
def : ProcNoItin<"arm1022e",        [HasV5TEOps]>;
def : ProcNoItin<"xscale",          [HasV5TEOps]>;
def : ProcNoItin<"iwmmxt",          [HasV5TEOps]>;

// V6 Processors.
def : Processor<"arm1136j-s",       ARMV6Itineraries, [HasV6Ops]>;
def : Processor<"arm1136jf-s",      ARMV6Itineraries, [HasV6Ops, FeatureVFP2,
                                                       FeatureHasSlowFPVMLx]>;
def : Processor<"arm1176jz-s",      ARMV6Itineraries, [HasV6Ops]>;
def : Processor<"arm1176jzf-s",     ARMV6Itineraries, [HasV6Ops, FeatureVFP2,
                                                       FeatureHasSlowFPVMLx]>;
def : Processor<"mpcorenovfp",      ARMV6Itineraries, [HasV6Ops]>;
def : Processor<"mpcore",           ARMV6Itineraries, [HasV6Ops, FeatureVFP2,
                                                       FeatureHasSlowFPVMLx]>;

// V6M Processors.
def : Processor<"cortex-m0",        ARMV6Itineraries, [HasV6Ops, FeatureNoARM,
                                                       FeatureDB, FeatureMClass]>;

// V6T2 Processors.
def : Processor<"arm1156t2-s",      ARMV6Itineraries, [HasV6T2Ops,
                                                       FeatureDSPThumb2]>;
def : Processor<"arm1156t2f-s",     ARMV6Itineraries, [HasV6T2Ops, FeatureVFP2,
                                                       FeatureHasSlowFPVMLx,
                                                       FeatureDSPThumb2]>;

// V7a Processors.
def : Processor<"cortex-a8",        CortexA8Itineraries,
                                    [ProcA8, HasV7Ops, FeatureNEON, FeatureDB,
                                     FeatureDSPThumb2]>;
def : Processor<"cortex-a9",        CortexA9Itineraries,
                                    [ProcA9, HasV7Ops, FeatureNEON, FeatureDB,
                                     FeatureDSPThumb2]>;
def : Processor<"cortex-a9-mp",     CortexA9Itineraries,
                                    [ProcA9, HasV7Ops, FeatureNEON, FeatureDB,
                                     FeatureDSPThumb2, FeatureMP]>;

// V7M Processors.
def : ProcNoItin<"cortex-m3",       [HasV7Ops,
                                     FeatureThumb2, FeatureNoARM, FeatureDB,
                                     FeatureHWDiv, FeatureMClass]>;

// V7EM Processors.
def : ProcNoItin<"cortex-m4",       [HasV7Ops,
                                     FeatureThumb2, FeatureNoARM, FeatureDB,
                                     FeatureHWDiv, FeatureDSPThumb2,
                                     FeatureT2XtPk, FeatureVFP2,
                                     FeatureVFPOnlySP, FeatureMClass]>;

//===----------------------------------------------------------------------===//
// Register File Description
//===----------------------------------------------------------------------===//

include "ARMRegisterInfo.td"

include "ARMCallingConv.td"

//===----------------------------------------------------------------------===//
// Instruction Descriptions
//===----------------------------------------------------------------------===//

include "ARMInstrInfo.td"

def ARMInstrInfo : InstrInfo;


//===----------------------------------------------------------------------===//
// Assembly printer
//===----------------------------------------------------------------------===//
// ARM Uses the MC printer for asm output, so make sure the TableGen
// AsmWriter bits get associated with the correct class.
def ARMAsmWriter : AsmWriter {
  string AsmWriterClassName  = "InstPrinter";
  bit isMCAsmWriter = 1;
}

//===----------------------------------------------------------------------===//
// Declare the target which we are implementing
//===----------------------------------------------------------------------===//

def ARM : Target {
  // Pull in Instruction Info:
  let InstructionSet = ARMInstrInfo;

  let AssemblyWriters = [ARMAsmWriter];
}
