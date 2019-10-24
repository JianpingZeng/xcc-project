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

//===----------------------------------------------------------------------===//
//  Declarations that describe the ARM register file
//===----------------------------------------------------------------------===//

// Registers are identified with 4-bit ID numbers.
class ARMReg<bits<4> num, string n, list<Register> subregs = []> : Register<n> {
  field bits<4> Num;
  let Namespace = "ARM";
  let SubRegs = subregs;
}

 // Integer registers
class ARMFReg<bits<5> num, string n> : Register<n> {
  field bits<5> Num;
  let Namespace = "ARM";
}

// Integer registers
def R0  : ARMReg< 0, "r0">,  DwarfRegNum<[0]>;
def R1  : ARMReg< 1, "r1">,  DwarfRegNum<[1]>;
def R2  : ARMReg< 2, "r2">,  DwarfRegNum<[2]>;
def R3  : ARMReg< 3, "r3">,  DwarfRegNum<[3]>;
def R4  : ARMReg< 4, "r4">,  DwarfRegNum<[4]>;
def R5  : ARMReg< 5, "r5">,  DwarfRegNum<[5]>;
def R6  : ARMReg< 6, "r6">,  DwarfRegNum<[6]>;
def R7  : ARMReg< 7, "r7">,  DwarfRegNum<[7]>;
def R8  : ARMReg< 8, "r8">,  DwarfRegNum<[8]>;
def R9  : ARMReg< 9, "r9">,  DwarfRegNum<[9]>;
def R10 : ARMReg<10, "r10">, DwarfRegNum<[10]>;
def R11 : ARMReg<11, "r11">, DwarfRegNum<[11]>;
def R12 : ARMReg<12, "r12">, DwarfRegNum<[12]>;
def SP  : ARMReg<13, "sp">,  DwarfRegNum<[13]>;
def LR  : ARMReg<14, "lr">,  DwarfRegNum<[14]>;
def PC  : ARMReg<15, "pc">,  DwarfRegNum<[15]>;

// Float registers
def S0  : ARMFReg< 0, "s0">;  def S1  : ARMFReg< 1, "s1">;
def S2  : ARMFReg< 2, "s2">;  def S3  : ARMFReg< 3, "s3">;
def S4  : ARMFReg< 4, "s4">;  def S5  : ARMFReg< 5, "s5">;
def S6  : ARMFReg< 6, "s6">;  def S7  : ARMFReg< 7, "s7">;
def S8  : ARMFReg< 8, "s8">;  def S9  : ARMFReg< 9, "s9">;
def S10 : ARMFReg<10, "s10">; def S11 : ARMFReg<11, "s11">;
def S12 : ARMFReg<12, "s12">; def S13 : ARMFReg<13, "s13">;
def S14 : ARMFReg<14, "s14">; def S15 : ARMFReg<15, "s15">;
def S16 : ARMFReg<16, "s16">; def S17 : ARMFReg<17, "s17">;
def S18 : ARMFReg<18, "s18">; def S19 : ARMFReg<19, "s19">;
def S20 : ARMFReg<20, "s20">; def S21 : ARMFReg<21, "s21">;
def S22 : ARMFReg<22, "s22">; def S23 : ARMFReg<23, "s23">;
def S24 : ARMFReg<24, "s24">; def S25 : ARMFReg<25, "s25">;
def S26 : ARMFReg<26, "s26">; def S27 : ARMFReg<27, "s27">;
def S28 : ARMFReg<28, "s28">; def S29 : ARMFReg<29, "s29">;
def S30 : ARMFReg<30, "s30">; def S31 : ARMFReg<31, "s31">;

// Aliases of the F* registers used to hold 64-bit fp values (doubles)
def D0  : ARMReg< 0,  "d0", [S0,   S1]>;
def D1  : ARMReg< 1,  "d1", [S2,   S3]>;
def D2  : ARMReg< 2,  "d2", [S4,   S5]>;
def D3  : ARMReg< 3,  "d3", [S6,   S7]>;
def D4  : ARMReg< 4,  "d4", [S8,   S9]>;
def D5  : ARMReg< 5,  "d5", [S10, S11]>;
def D6  : ARMReg< 6,  "d6", [S12, S13]>;
def D7  : ARMReg< 7,  "d7", [S14, S15]>;
def D8  : ARMReg< 8,  "d8", [S16, S17]>;
def D9  : ARMReg< 9,  "d9", [S18, S19]>;
def D10 : ARMReg<10, "d10", [S20, S21]>;
def D11 : ARMReg<11, "d11", [S22, S23]>;
def D12 : ARMReg<12, "d12", [S24, S25]>;
def D13 : ARMReg<13, "d13", [S26, S27]>;
def D14 : ARMReg<14, "d14", [S28, S29]>;
def D15 : ARMReg<15, "d15", [S30, S31]>;

// Current Program Status Register.
def CPSR    : ARMReg<0, "cpsr">;
def APSR    : ARMReg<1, "apsr">;
def SPSR    : ARMReg<2, "spsr">;
def FPSCR   : ARMReg<3, "fpscr">;
def ITSTATE : ARMReg<4, "itstate">;

// Special Registers - only available in privileged mode.
def FPSID   : ARMReg<0, "fpsid">;
def FPEXC   : ARMReg<8, "fpexc">;

 // Register classes.
 //
 // FIXME: the register order should be defined in terms of the preferred
 // allocation order...
 //
def IntRegs : RegisterClass<"ARM", [i32], 32, [R0, R1, R2, R3, R4, R5, R6,
                                               R7, R8, R9, R10, R11, R12,
                                               SP, LR, PC]> {
 let MethodBodies = [{
   // r15 == Program Counter
   // r14 == Link Register
   // r13 == Stack Pointer
   // r12 == ip (scratch)
   // r11 == Frame Pointer
   // r10 == Stack Limit
  private final int[] ARM_GPR =
  {
    R0, R1, R2, R3,
    R12, LR,
    R4, R5, R6, R7,
    R8, R9, R10,
    R11
  };

   @Override
   public int[] getAllocableRegs(MachineFunction mf)
   {
       return ARM_GPR;
   }
 }];
}