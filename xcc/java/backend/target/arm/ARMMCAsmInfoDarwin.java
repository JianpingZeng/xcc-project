package backend.target.arm;
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

import backend.mc.MCAsmInfo;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
class ARMMCAsmInfoDarwin extends MCAsmInfo {
  private static final String[] arm_asm_table = {
    "{r0}", "r0",
    "{r1}", "r1",
    "{r2}", "r2",
    "{r3}", "r3",
    "{r4}", "r4",
    "{r5}", "r5",
    "{r6}", "r6",
    "{r7}", "r7",
    "{r8}", "r8",
    "{r9}", "r9",
    "{r10}", "r10",
    "{r11}", "r11",
    "{r12}", "r12",
    "{r13}", "r13",
    "{r14}", "r14",
    "{lr}", "lr",
    "{sp}", "sp",
    "{ip}", "ip",
    "{fp}", "fp",
    "{sl}", "sl",
    "{memory}", "memory",
    "{cc}", "cc",
  };

  ARMMCAsmInfoDarwin() {
    AsmTransCBE = arm_asm_table;
    Data64bitsDirective = null;
    CommentString = "@";
    Code16Directive = ".code\t16";
    Code32Directive = ".code\t32";

    SupportsDebugInformation = true;

    // Exceptions handling
    ExceptionsType = ExceptionHandlingType.SjLj;
  }
}
