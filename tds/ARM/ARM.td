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

 include "../Target.td"

//===----------------------------------------------------------------------===//
// ARM Subtarget features.
//

def FeatureVFP2 : SubtargetFeature<"vfp2", "hasVFPv2", "true",
                                   "Enable VFP2 instructions">;
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
// Some processors have FP multiply-accumulate instructions that don't
// play nicely with other VFP / NEON instructions, and it's generally better
// to just not use them.
def FeatureHasSlowFPVMLx : SubtargetFeature<"slowfpvmlx", "slowFPVMLx", "true",
                                         "Disable VFP / NEON MAC instructions">;

class ProcNoItin<string Name, list<SubtargetFeature> Features>
 : Processor<Name, NoItineraries, Features>;

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
def : ProcNoItin<"arm1136j-s",      [HasV6Ops]>;
def : ProcNoItin<"arm1136jf-s",     [HasV6Ops, FeatureVFP2,
                                     FeatureHasSlowFPVMLx]>;
def : ProcNoItin<"arm1176jz-s",     [HasV6Ops]>;
def : ProcNoItin<"arm1176jzf-s",    [HasV6Ops, FeatureVFP2,
                                     FeatureHasSlowFPVMLx]>;
def : ProcNoItin<"mpcorenovfp",     [HasV6Ops]>;
def : ProcNoItin<"mpcore",          [HasV6Ops, FeatureVFP2,
                                     FeatureHasSlowFPVMLx]>;


 //===----------------------------------------------------------------------===//
 // Register File Description
 //===----------------------------------------------------------------------===//

 include "ARMRegisterInfo.td"

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
