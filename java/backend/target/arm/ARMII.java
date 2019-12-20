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

import tools.Util;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public interface ARMII {
  //===------------------------------------------------------------------===//
  // ARM Specific MachineOperand flags.

  int MO_NO_FLAG = 0;

  /// MO_LO16 - On a symbol operand, this represents a relocation containing
  /// lower 16 bit of the address. Used only via movw instruction.
  int MO_LO16 = 1;

  /// MO_HI16 - On a symbol operand, this represents a relocation containing
  /// higher 16 bit of the address. Used only via movt instruction.
  int MO_HI16 = 2;

  /// MO_LO16_NONLAZY - On a symbol operand "FOO", this represents a
  /// relocation containing lower 16 bit of the non-lazy-ptr indirect symbol,
  /// i.e. "FOO$non_lazy_ptr".
  /// Used only via movw instruction.
  int MO_LO16_NONLAZY = 3;

  /// MO_HI16_NONLAZY - On a symbol operand "FOO", this represents a
  /// relocation containing lower 16 bit of the non-lazy-ptr indirect symbol,
  /// i.e. "FOO$non_lazy_ptr". Used only via movt instruction.
  int MO_HI16_NONLAZY = 4;

  /// MO_LO16_NONLAZY_PIC - On a symbol operand "FOO", this represents a
  /// relocation containing lower 16 bit of the PC relative address of the
  /// non-lazy-ptr indirect symbol, i.e. "FOO$non_lazy_ptr - LABEL".
  /// Used only via movw instruction.
  int MO_LO16_NONLAZY_PIC = 5;

  /// MO_HI16_NONLAZY_PIC - On a symbol operand "FOO", this represents a
  /// relocation containing lower 16 bit of the PC relative address of the
  /// non-lazy-ptr indirect symbol, i.e. "FOO$non_lazy_ptr - LABEL".
  /// Used only via movt instruction.
  int MO_HI16_NONLAZY_PIC = 6;

  /// MO_PLT - On a symbol operand, this represents an ELF PLT reference on a
  /// call operand.
  int MO_PLT = 7;

  /// ARM Index Modes
  enum IndexMode {
    IndexModeNone,
    IndexModePre,
    IndexModePost,
    IndexModeUpd
  }

  /// ARM Addressing Modes
  enum AddrMode {
    AddrModeNone,
    AddrMode1,
    AddrMode2,
    AddrMode3,
    AddrMode4,
    AddrMode5,
    AddrMode6,
    AddrModeT1_1,
    AddrModeT1_2,
    AddrModeT1_4,
    AddrModeT1_s, // i8 * 4 for pc and sp relative data
    AddrModeT2_i12,
    AddrModeT2_i8,
    AddrModeT2_so,
    AddrModeT2_pc, // +/- i12 for pc relative data
    AddrModeT2_i8s4, // i8 * 4
    AddrMode_i12
  }

  static String AddrModeToString(AddrMode addrmode) {
    switch (addrmode) {
      default:
        Util.shouldNotReachHere("Unknown memory operation");
      case AddrModeNone:    return "AddrModeNone";
      case AddrMode1:       return "AddrMode1";
      case AddrMode2:       return "AddrMode2";
      case AddrMode3:       return "AddrMode3";
      case AddrMode4:       return "AddrMode4";
      case AddrMode5:       return "AddrMode5";
      case AddrMode6:       return "AddrMode6";
      case AddrModeT1_1:    return "AddrModeT1_1";
      case AddrModeT1_2:    return "AddrModeT1_2";
      case AddrModeT1_4:    return "AddrModeT1_4";
      case AddrModeT1_s:    return "AddrModeT1_s";
      case AddrModeT2_i12:  return "AddrModeT2_i12";
      case AddrModeT2_i8:   return "AddrModeT2_i8";
      case AddrModeT2_so:   return "AddrModeT2_so";
      case AddrModeT2_pc:   return "AddrModeT2_pc";
      case AddrModeT2_i8s4: return "AddrModeT2_i8s4";
      case AddrMode_i12:    return "AddrMode_i12";
    }
  }

  /// Target Operand Flag enum.
  enum TOF {
    //===------------------------------------------------------------------===//
    // ARM Specific MachineOperand flags.

    MO_NO_FLAG,

    /// MO_LO16 - On a symbol operand, this represents a relocation containing
    /// lower 16 bit of the address. Used only via movw instruction.
    MO_LO16,

    /// MO_HI16 - On a symbol operand, this represents a relocation containing
    /// higher 16 bit of the address. Used only via movt instruction.
    MO_HI16,

    /// MO_LO16_NONLAZY - On a symbol operand "FOO", this represents a
    /// relocation containing lower 16 bit of the non-lazy-ptr indirect symbol,
    /// i.e. "FOO$non_lazy_ptr".
    /// Used only via movw instruction.
    MO_LO16_NONLAZY,

    /// MO_HI16_NONLAZY - On a symbol operand "FOO", this represents a
    /// relocation containing lower 16 bit of the non-lazy-ptr indirect symbol,
    /// i.e. "FOO$non_lazy_ptr". Used only via movt instruction.
    MO_HI16_NONLAZY,

    /// MO_LO16_NONLAZY_PIC - On a symbol operand "FOO", this represents a
    /// relocation containing lower 16 bit of the PC relative address of the
    /// non-lazy-ptr indirect symbol, i.e. "FOO$non_lazy_ptr - LABEL".
    /// Used only via movw instruction.
    MO_LO16_NONLAZY_PIC,

    /// MO_HI16_NONLAZY_PIC - On a symbol operand "FOO", this represents a
    /// relocation containing lower 16 bit of the PC relative address of the
    /// non-lazy-ptr indirect symbol, i.e. "FOO$non_lazy_ptr - LABEL".
    /// Used only via movt instruction.
    MO_HI16_NONLAZY_PIC,

    /// MO_PLT - On a symbol operand, this represents an ELF PLT reference on a
    /// call operand.
    MO_PLT
  }

    //===------------------------------------------------------------------===//
    // Instruction Flags.

    //===------------------------------------------------------------------===//
    // This four-bit field describes the addressing mode used.
  int AddrModeMask  = 0x1f, // The AddrMode enums are declared in ARMBaseInfo.h

  // IndexMode - Unindex, pre-indexed, or post-indexed are valid for load
  // and store ops only.  Generic "updating" flag is used for ld/st multiple.
  // The index mode enums are declared in ARMBaseInfo.h
  IndexModeShift = 5,
  IndexModeMask  = 3 << IndexModeShift,

  //===------------------------------------------------------------------===//
  // Instruction encoding formats.
  //
  FormShift     = 7,
  FormMask      = 0x3f << FormShift,

  // Pseudo instructions
  Pseudo        = 0  << FormShift,

  // Multiply instructions
  MulFrm        = 1  << FormShift,

  // Branch instructions
  BrFrm         = 2  << FormShift,
  BrMiscFrm     = 3  << FormShift,

  // Data Processing instructions
  DPFrm         = 4  << FormShift,
  DPSoRegFrm    = 5  << FormShift,

  // Load and Store
  LdFrm         = 6  << FormShift,
  StFrm         = 7  << FormShift,
  LdMiscFrm     = 8  << FormShift,
  StMiscFrm     = 9  << FormShift,
  LdStMulFrm    = 10 << FormShift,

  LdStExFrm     = 11 << FormShift,

  // Miscellaneous arithmetic instructions
  ArithMiscFrm  = 12 << FormShift,
  SatFrm        = 13 << FormShift,

  // Extend instructions
  ExtFrm        = 14 << FormShift,

  // VFP formats
  VFPUnaryFrm   = 15 << FormShift,
  VFPBinaryFrm  = 16 << FormShift,
  VFPConv1Frm   = 17 << FormShift,
  VFPConv2Frm   = 18 << FormShift,
  VFPConv3Frm   = 19 << FormShift,
  VFPConv4Frm   = 20 << FormShift,
  VFPConv5Frm   = 21 << FormShift,
  VFPLdStFrm    = 22 << FormShift,
  VFPLdStMulFrm = 23 << FormShift,
  VFPMiscFrm    = 24 << FormShift,

  // Thumb format
  ThumbFrm      = 25 << FormShift,

  // Miscelleaneous format
  MiscFrm       = 26 << FormShift,

  // NEON formats
  NGetLnFrm     = 27 << FormShift,
  NSetLnFrm     = 28 << FormShift,
  NDupFrm       = 29 << FormShift,
  NLdStFrm      = 30 << FormShift,
  N1RegModImmFrm= 31 << FormShift,
  N2RegFrm      = 32 << FormShift,
  NVCVTFrm      = 33 << FormShift,
  NVDupLnFrm    = 34 << FormShift,
  N2RegVShLFrm  = 35 << FormShift,
  N2RegVShRFrm  = 36 << FormShift,
  N3RegFrm      = 37 << FormShift,
  N3RegVShFrm   = 38 << FormShift,
  NVExtFrm      = 39 << FormShift,
  NVMulSLFrm    = 40 << FormShift,
  NVTBLFrm      = 41 << FormShift,

  //===------------------------------------------------------------------===//
  // Misc flags.

  // UnaryDP - Indicates this is a unary data processing instruction, i.e.
  // it doesn't have a Rn operand.
  UnaryDP       = 1 << 13,

  // Xform16Bit - Indicates this Thumb2 instruction may be transformed into
  // a 16-bit Thumb instruction if certain conditions are met.
  Xform16Bit    = 1 << 14,

  // ThumbArithFlagSetting - The instruction is a 16-bit flag setting Thumb
  // instruction. Used by the parser to determine whether to require the 'S'
  // suffix on the mnemonic (when not in an IT block) or preclude it (when
  // in an IT block).
  ThumbArithFlagSetting = 1 << 18,

  //===------------------------------------------------------------------===//
  // Code domain.
  DomainShift   = 15,
  DomainMask    = 7 << DomainShift,
  DomainGeneral = 0 << DomainShift,
  DomainVFP     = 1 << DomainShift,
  DomainNEON    = 2 << DomainShift,
  DomainNEONA8  = 4 << DomainShift,

  //===------------------------------------------------------------------===//
  // Field shifts - such shifts are used to set field while generating
  // machine instructions.
  //
  // FIXME: This list will need adjusting/fixing as the MC code emitter
  // takes shape and the ARMCodeEmitter.cpp bits go away.
  ShiftTypeShift = 4,

  M_BitShift     = 5,
  ShiftImmShift  = 5,
  ShiftShift     = 7,
  N_BitShift     = 7,
  ImmHiShift     = 8,
  SoRotImmShift  = 8,
  RegRsShift     = 8,
  ExtRotImmShift = 10,
  RegRdLoShift   = 12,
  RegRdShift     = 12,
  RegRdHiShift   = 16,
  RegRnShift     = 16,
  S_BitShift     = 20,
  W_BitShift     = 21,
  AM3_I_BitShift = 22,
  D_BitShift     = 22,
  U_BitShift     = 23,
  P_BitShift     = 24,
  I_BitShift     = 25,
  CondShift      = 28;
}
