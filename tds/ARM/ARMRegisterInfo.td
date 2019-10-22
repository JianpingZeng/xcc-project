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
class ARMReg<bits<4> num, string n> : Register<n> {
  field bits<4> Num;
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
def R13 : ARMReg<13, "R13">, DwarfRegNum<[13]>;
def R14 : ARMReg<14, "R14">, DwarfRegNum<[14]>;
def R15 : ARMReg<15, "R15">, DwarfRegNum<[15]>;

 // Register classes.
 //
 // FIXME: the register order should be defined in terms of the preferred
 // allocation order...
 //
 def IntRegs : RegisterClass<"ARM", [i32], 32, [R0, R1, R2, R3, R4, R5, R6,
 	                                            R7, R8, R9, R10, R11, R12,
                                                R13, R14, R15]> {
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
     R12, R14,
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