include "../Target.td"

//===----------------------------------------------------------------------===//
// X86 Subtarget features.
//===----------------------------------------------------------------------===//

def FeatureMMX     : SubtargetFeature<"mmx","x86SSELevel", "X86SSEEnum.MMX",
                                      "Enable MMX instructions">;
def FeatureSSE1    : SubtargetFeature<"sse", "x86SSELevel", "X86SSEEnum.SSE1",
                                      "Enable SSE instructions",
                                      [FeatureMMX]>;
def FeatureSSE2    : SubtargetFeature<"sse2", "x86SSELevel", "X86SSEEnum.SSE2",
                                      "Enable SSE2 instructions",
                                      [FeatureSSE1]>;
def FeatureSSE3    : SubtargetFeature<"sse3", "x86SSELevel", "X86SSEEnum.SSE3",
                                      "Enable SSE3 instructions",
                                      [FeatureSSE2]>;
def FeatureSSSE3   : SubtargetFeature<"ssse3", "x86SSELevel", "X86SSEEnum.SSE3",
                                      "Enable SSE3 instructions",
                                      [FeatureSSE3]>;
def FeatureSSE41   : SubtargetFeature<"sse41", "x86SSELevel", "X86SSEEnum.SSE41",
                                      "Enable SSE 4.1 instructions",
                                      [FeatureSSSE3]>;
def FeatureSSE42   : SubtargetFeature<"sse42", "x86SSELevel", "X86SSEEnum.SSE42",
                                      "Enable SSE 4.2 instructions",
                                      [FeatureSSE41]>;
def Feature3DNow   : SubtargetFeature<"3dnow", "x863DNowLevel", "X863DNowEnum.ThreeDNow",
                                      "Enable 3DNow! instructions">;
def Feature3DNowA  : SubtargetFeature<"3dnowa", "x863DNowLevel", "X863DNowEnum.ThreeDNowA",
                                      "Enable 3DNow! Athlon instructions",
                                      [Feature3DNow]>;
// All x86-64 hardware has SSE2, but we don't mark SSE2 as an implied
// feature, because SSE2 can be disabled (e.g. for compiling OS kernels)
// without disabling 64-bit mode.
def Feature64Bit   : SubtargetFeature<"64bit", "hasX86_64", "true",
                                      "Support 64-bit instructions">;
def FeatureSlowBTMem : SubtargetFeature<"slow-bt-mem", "isBTMemSlow", "true",
                                       "Bit testing of memory is slow">;
def FeatureSSE4A   : SubtargetFeature<"sse4a", "hasSSE4A", "true",
                                      "Support SSE 4a instructions">;

def FeatureAVX     : SubtargetFeature<"avx", "hasAVX", "true",
                                      "Enable AVX instructions">;
def FeatureFMA3    : SubtargetFeature<"fma3", "hasFMA3", "true",
                                      "Enable three-operand fused multiple-add">;
def FeatureFMA4    : SubtargetFeature<"fma4", "hasFMA4", "true",
                                      "Enable four-operand fused multiple-add">;

//===----------------------------------------------------------------------===//
// X86 processors supported.
//===----------------------------------------------------------------------===//

class Proc<string Name, list<SubtargetFeature> Features>
 : Processor<Name, NoItineraries, Features>;

def : Proc<"generic",         []>;
def : Proc<"i386",            []>;
def : Proc<"i486",            []>;
def : Proc<"i586",            []>;
def : Proc<"pentium",         []>;
def : Proc<"pentium-mmx",     [FeatureMMX]>;
def : Proc<"i686",            []>;
def : Proc<"pentiumpro",      []>;
def : Proc<"pentium2",        [FeatureMMX]>;
def : Proc<"pentium3",        [FeatureSSE1]>;
def : Proc<"pentium-m",       [FeatureSSE2, FeatureSlowBTMem]>;
def : Proc<"pentium4",        [FeatureSSE2]>;
def : Proc<"x86-64",          [FeatureSSE2,   Feature64Bit, FeatureSlowBTMem]>;
def : Proc<"yonah",           [FeatureSSE3, FeatureSlowBTMem]>;
def : Proc<"prescott",        [FeatureSSE3, FeatureSlowBTMem]>;
def : Proc<"nocona",          [FeatureSSE3,   Feature64Bit, FeatureSlowBTMem]>;
def : Proc<"core2",           [FeatureSSSE3,  Feature64Bit, FeatureSlowBTMem]>;
def : Proc<"penryn",          [FeatureSSE41,  Feature64Bit, FeatureSlowBTMem]>;
def : Proc<"atom",            [FeatureSSE3,   Feature64Bit, FeatureSlowBTMem]>;
def : Proc<"corei7",          [FeatureSSE42,  Feature64Bit, FeatureSlowBTMem]>;
def : Proc<"nehalem",         [FeatureSSE42,  Feature64Bit, FeatureSlowBTMem]>;
// Sandy Bridge does not have FMA
def : Proc<"sandybridge",     [FeatureSSE42,  FeatureAVX,   Feature64Bit]>;

def : Proc<"k6",              [FeatureMMX]>;
def : Proc<"k6-2",            [FeatureMMX,    Feature3DNow]>;
def : Proc<"k6-3",            [FeatureMMX,    Feature3DNow]>;
def : Proc<"athlon",          [FeatureMMX,    Feature3DNowA, FeatureSlowBTMem]>;
def : Proc<"athlon-tbird",    [FeatureMMX,    Feature3DNowA, FeatureSlowBTMem]>;
def : Proc<"athlon-4",        [FeatureSSE1,   Feature3DNowA, FeatureSlowBTMem]>;
def : Proc<"athlon-xp",       [FeatureSSE1,   Feature3DNowA, FeatureSlowBTMem]>;
def : Proc<"athlon-mp",       [FeatureSSE1,   Feature3DNowA, FeatureSlowBTMem]>;
def : Proc<"k8",              [FeatureSSE2,   Feature3DNowA, Feature64Bit,
                               FeatureSlowBTMem]>;
def : Proc<"opteron",         [FeatureSSE2,   Feature3DNowA, Feature64Bit,
                               FeatureSlowBTMem]>;
def : Proc<"athlon64",        [FeatureSSE2,   Feature3DNowA, Feature64Bit,
                               FeatureSlowBTMem]>;
def : Proc<"athlon-fx",       [FeatureSSE2,   Feature3DNowA, Feature64Bit,
                               FeatureSlowBTMem]>;
def : Proc<"k8-sse3",         [FeatureSSE3,   Feature3DNowA, Feature64Bit,
                               FeatureSlowBTMem]>;
def : Proc<"opteron-sse3",    [FeatureSSE3,   Feature3DNowA, Feature64Bit,
                               FeatureSlowBTMem]>;
def : Proc<"athlon64-sse3",   [FeatureSSE3,   Feature3DNowA, Feature64Bit,
                               FeatureSlowBTMem]>;
def : Proc<"amdfam10",        [FeatureSSE3,   FeatureSSE4A,
                               Feature3DNowA, Feature64Bit, FeatureSlowBTMem]>;
def : Proc<"barcelona",       [FeatureSSE3,   FeatureSSE4A,
                               Feature3DNowA, Feature64Bit, FeatureSlowBTMem]>;
def : Proc<"istanbul",        [Feature3DNowA, Feature64Bit, FeatureSSE4A,
                               Feature3DNowA]>;
def : Proc<"shanghai",        [Feature3DNowA, Feature64Bit, FeatureSSE4A,
                               Feature3DNowA]>;

def : Proc<"winchip-c6",      [FeatureMMX]>;
def : Proc<"winchip2",        [FeatureMMX, Feature3DNow]>;
def : Proc<"c3",              [FeatureMMX, Feature3DNow]>;
def : Proc<"c3-2",            [FeatureSSE1]>;

//===----------------------------------------------------------------------===//
// Register File Description
//===----------------------------------------------------------------------===//

include "X86RegisterInfo.td"

//===----------------------------------------------------------------------===//
// Instruction Descriptions
//===----------------------------------------------------------------------===//

include "X86InstrInfo.td"

def X86InstrInfo : InstrInfo {

  // Define how we want to layout our TargetSpecific information field... This
  // should be kept up-to-date with the fields in the X86InstrInfo.h file.
  let TSFlagsFields = ["FormBits",
                       "hasOpSizePrefix",
                       "hasAdSizePrefix",
                       "Prefix",
                       "hasREX_WPrefix",
                       "ImmTypeBits",
                       "FPFormBits",
                       "hasLockPrefix",
                       "SegOvrBits",
                       "Opcode"];
  let TSFlagsShifts = [0,
                       6,
                       7,
                       8,
                       12,
                       13,
                       16,
                       19,
                       20,
                       24];
}

//===----------------------------------------------------------------------===//
// Calling Conventions
//===----------------------------------------------------------------------===//

// Commented by Jianping Zeng on 2017.6.19
include "X86CallingConv.td"


//===----------------------------------------------------------------------===//
// Assembly Printers
//===----------------------------------------------------------------------===//

// Currently the X86 assembly parser only supports ATT syntax.
def ATTAsmParser : AsmParser {
  string AsmParserClassName  = "ATTAsmParser";
  int Variant = 0;

  // Discard comments in assembly strings.
  string CommentDelimiter = "#";

  // Recognize hard coded registers.
  string RegisterPrefix = "%";
}

// The X86 target supports two different syntaxes for emitting machine code.
// This is controlled by the -x86-asm-syntax={att|intel}
def ATTAsmWriter : AsmWriter {
  string AsmWriterClassName  = "ATTInstPrinter";
  int Variant = 0;
}
def IntelAsmWriter : AsmWriter {
  string AsmWriterClassName  = "IntelInstPrinter";
  int Variant = 1;
}

def X86 : Target {
  // Information about the instructions...
  let InstructionSet = X86InstrInfo;

  let AssemblyParsers = [ATTAsmParser];

  let AssemblyWriters = [ATTAsmWriter, IntelAsmWriter];
}
