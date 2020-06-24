//===- Mips.td - Describe the Mips Target Machine ----------*- tablegen -*-===//
//
//                     The LLVM Compiler Infrastructure
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
// This is the top level entry point for the Mips target.
//===----------------------------------------------------------------------===//

//===----------------------------------------------------------------------===//
// Target-independent interfaces
//===----------------------------------------------------------------------===//

include "../Target.td"

//===----------------------------------------------------------------------===//
// Register File, Calling Conv, Instruction Descriptions
//===----------------------------------------------------------------------===//

include "MipsRegisterInfo.td"
include "MipsSchedule.td"
include "MipsInstrInfo.td"
include "MipsCallingConv.td"

def MipsInstrInfo : InstrInfo;

//===----------------------------------------------------------------------===//
// Mips Subtarget features                                                    //
//===----------------------------------------------------------------------===//

def FeatureGP64Bit     : SubtargetFeature<"gp64", "isGP64bit", "true",
                                "General Purpose Registers are 64-bit wide.">;
def FeatureFP64Bit     : SubtargetFeature<"fp64", "isFP64bit", "true",
                                "Support 64-bit FP registers.">;
def FeatureSingleFloat : SubtargetFeature<"single-float", "isSingleFloat",
                                "true", "Only supports single precision float">;
def FeatureO32         : SubtargetFeature<"o32", "mipsABI", "MipsABIEnum.O32",
                                "Enable o32 ABI">;
def FeatureN32         : SubtargetFeature<"n32", "mipsABI", "MipsABIEnum.N32",
                                "Enable n32 ABI">;
def FeatureN64         : SubtargetFeature<"n64", "mipsABI", "MipsABIEnum.N64",
                                "Enable n64 ABI">;
def FeatureEABI        : SubtargetFeature<"eabi", "mipsABI", "MipsABIEnum.EABI",
                                "Enable eabi ABI">;
def FeatureVFPU        : SubtargetFeature<"vfpu", "hasVFPU",
                                "true", "Enable vector FPU instructions.">;
def FeatureSEInReg     : SubtargetFeature<"seinreg", "hasSEInReg", "true",
                                "Enable 'signext in register' instructions.">;
def FeatureCondMov     : SubtargetFeature<"condmov", "hasCondMov", "true",
                                "Enable 'conditional move' instructions.">;
def FeatureMulDivAdd   : SubtargetFeature<"muldivadd", "hasMulDivAdd", "true",
                                "Enable 'multiply add/sub' instructions.">;
def FeatureMinMax      : SubtargetFeature<"minmax", "hasMinMax", "true",
                                "Enable 'min/max' instructions.">;
def FeatureSwap        : SubtargetFeature<"swap", "hasSwap", "true",
                                "Enable 'byte/half swap' instructions.">;
def FeatureBitCount    : SubtargetFeature<"bitcount", "hasBitCount", "true",
                                "Enable 'count leading bits' instructions.">;
def FeatureMips32      : SubtargetFeature<"mips32", "mipsArchVersion", "MipsArchEnum.Mips32",
                                "Mips32 ISA Support",
                                [FeatureCondMov, FeatureBitCount]>;
def FeatureMips32r2    : SubtargetFeature<"mips32r2", "mipsArchVersion",
                                "MipsArchEnum.Mips32r2", "Mips32r2 ISA Support",
                                [FeatureMips32, FeatureSEInReg]>;
def FeatureMips64      : SubtargetFeature<"mips64", "mipsArchVersion",
                                "MipsArchEnum.Mips64", "Mips64 ISA Support",
                                [FeatureGP64Bit, FeatureFP64Bit,
                                 FeatureMips32]>;
def FeatureMips64r2    : SubtargetFeature<"mips64r2", "mipsArchVersion",
                                "MipsArchEnum.Mips64r2", "Mips64r2 ISA Support",
                                [FeatureMips64, FeatureMips32r2]>;

//===----------------------------------------------------------------------===//
// Mips processors supported.
//===----------------------------------------------------------------------===//

class Proc<string Name, list<SubtargetFeature> Features>
 : Processor<Name, MipsGenericItineraries, Features>;

def : Proc<"mips32r1", [FeatureMips32]>;
def : Proc<"4ke", [FeatureMips32r2]>;
def : Proc<"mips64r1", [FeatureMips64]>;
def : Proc<"mips64r2", [FeatureMips64r2]>;

def MipsAsmWriter : AsmWriter {
  string AsmWriterClassName  = "InstPrinter";
  bit isMCAsmWriter = 1;
}

def Mips : Target {
  let InstructionSet = MipsInstrInfo;

  let AssemblyWriters = [MipsAsmWriter];
}

