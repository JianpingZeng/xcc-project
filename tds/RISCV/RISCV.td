include "../Target.td"

//===----------------------------------------------------------------------===//
// RISCV Subtarget features and instruction predicates.
//===----------------------------------------------------------------------===//

def FeatureStdExtM : SubtargetFeature<"M", "hasStdExtM", "true",
                                      "'M' (Integer Multiplication and Division)">;
def FeatureStdExtA : SubtargetFeature<"A", "hasStdExtA", "true",
                                      "'A' (Atomic Instructions)">;
def FeatureStdExtF : SubtargetFeature<"F", "hasStdExtF", "true",
                                      "'F' (Single-Precision Floating Point)">;
def FeatureStdExtD : SubtargetFeature<"D", "hasStdExtD", "true",
                                      "'D' (Double-Precision Floating Point)">;
def FeatureStdExtC : SubtargetFeature<"c", "hasStdExtC", "true",
                                      "'C' (Compressed Instructions)">;
// Define 64bit or 32bit?
def Feature64Bit : SubtargetFeature<"64bit", "hasRV64", "true",
                                    "Implements RV64">;

def HasStdExtM : Predicate<"subtarget.hasStdExtM()">;
def HasStdExtA : Predicate<"subtarget.hasStdExtA()">;
def HasStdExtF : Predicate<"subtarget.hasStdExtF()">;
def HasStdExtD : Predicate<"subtarget.hasStdExtD()">;
def HasStdExtC : Predicate<"subtarget.hasStdExtC()">;
def IsRV64 : Predicate<"subtarget.is64Bit()">;
def IsRV32 : Predicate<"!subtarget.is64Bit()">;


def RV64           : HwMode<"+64bit">;
def RV32           : HwMode<"-64bit">;

//===----------------------------------------------------------------------===//
// Defines sub-cpu types for RISCV target.
//===----------------------------------------------------------------------===//
class Proc<string Name, list<SubtargetFeature> Features>
 : Processor<Name, NoItineraries, Features>;

def : Proc<"generic-rv32", []>;
def : Proc<"generic-rv64", [Feature64Bit]>;

//===----------------------------------------------------------------------===//
// RISCV Register description information.
//===----------------------------------------------------------------------===//
include "RISCVRegisterInfo.td"

//===----------------------------------------------------------------------===//
// RISCV Instruction description information.
//===----------------------------------------------------------------------===//
include "RISCVInstrInfo.td"

//===----------------------------------------------------------------------===//
// RISCV Calling convention.
//===----------------------------------------------------------------------===//
include "RISCVCallingConv.td"


//===----------------------------------------------------------------------===//
// Defines RISCV Instruction information.
//===----------------------------------------------------------------------===//
def RISCVInstrInfo : InstrInfo {
  let isLittleEndianEncoding = 1;
}

//===----------------------------------------------------------------------===//
// Assembly Printers
//===----------------------------------------------------------------------===//

// Currently the RISCV assembly parser only supports ATT syntax.
def RISCVAsmParser : AsmParser {
  string AsmParserClassName  = "RISCVAsmParser";
  int Variant = 0;

  // Discard comments in assembly strings.
  string CommentDelimiter = "#";

  // Recognize hard coded registers.
  string RegisterPrefix = "%";
}


def RISCVAsmWriter : AsmWriter {
  string AsmWriterClassName  = "RISCVAsmPrinter";
  int Variant = 0;
}

def RISCV : Target {
  // Information about the instructions...
  let InstructionSet = RISCVInstrInfo;

  let AssemblyParsers = [RISCVAsmParser];

  let AssemblyWriters = [RISCVAsmWriter];
}
