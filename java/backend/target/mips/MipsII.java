package backend.target.mips;
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

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public interface MipsII {
  /// Target Operand Flag enum.
  //===------------------------------------------------------------------===//
  // Mips Specific MachineOperand flags.
  int MO_NO_FLAG = 0,

  /// MO_GOT - Represents the offset into the global offset table at which
  /// the address the relocation entry symbol resides during execution.
  MO_GOT = 1,

  /// MO_GOT_CALL - Represents the offset into the global offset table at
  /// which the address of a call site relocation entry symbol resides
  /// during execution. This is different from the above since this flag
  /// can only be present in call instructions.
  MO_GOT_CALL = 2,

  /// MO_GPREL - Represents the offset from the current gp value to be used
  /// for the relocatable object file being produced.
  MO_GPREL = 3,

  /// MO_ABS_HI/LO - Represents the hi or low part of an absolute symbol
  /// address.
  MO_ABS_HI = 4,
  MO_ABS_LO = 5,

  /// MO_TLSGD - Represents the offset into the global offset table at which
  // the module ID and TSL block offset reside during execution (General
  // Dynamic TLS).
  MO_TLSGD = 6,

  /// MO_GOTTPREL - Represents the offset from the thread pointer (Initial
  // Exec TLS).
  MO_GOTTPREL = 7,

  /// MO_TPREL_HI/LO - Represents the hi and low part of the offset from
  // the thread pointer (Local Exec TLS).
  MO_TPREL_HI = 8,
  MO_TPREL_LO = 9,

  // N32/64 Flags.
  MO_GPOFF_HI = 10,
  MO_GPOFF_LO = 11,
  MO_GOT_DISP = 12,
  MO_GOT_PAGE = 13,
  MO_GOT_OFST = 14;


  //===------------------------------------------------------------------===//
  // Instruction encodings.  These are the standard/most common forms for
  // Mips instructions.
  //

  // Pseudo - This represents an instruction that is a pseudo instruction
  // or one that has not been implemented yet.  It is illegal to code generate
  // it, but tolerated for intermediate implementation stages.
  int Pseudo   = 0,

  /// FrmR - This form is for instructions of the format R.
  FrmR  = 1,
  /// FrmI - This form is for instructions of the format I.
  FrmI  = 2,
  /// FrmJ - This form is for instructions of the format J.
  FrmJ  = 3,
  /// FrmFR - This form is for instructions of the format FR.
  FrmFR = 4,
  /// FrmFI - This form is for instructions of the format FI.
  FrmFI = 5,
  /// FrmOther - This form is for instructions that have no specific format.
  FrmOther = 6,

  FormMask = 15;
}
