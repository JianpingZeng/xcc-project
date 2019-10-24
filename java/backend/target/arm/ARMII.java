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
}
