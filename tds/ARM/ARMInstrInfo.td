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
 //
 // This file describes the ARM instructions in TableGen format.
 //
 //===----------------------------------------------------------------------===//

 // Address operands
 def memri : Operand<iPTR> {
   let PrintMethod = "printMemRegImm";
   let MIOperandInfo = (ops i32imm, ptr_rc);
 }

 // Define ARM specific addressing mode.
 //register plus/minus 12 bit offset
 def iaddr  : ComplexPattern<iPTR, 2, "selectAddrRegImm", []>;
 //register plus scaled register
 //def raddr  : ComplexPattern<iPTR, 2, "SelectAddrRegReg", []>;

 //===----------------------------------------------------------------------===//
 // Instructions
 //===----------------------------------------------------------------------===//

 class InstARM<dag outs, dag ins, string asmstr, list<dag> pattern> : Instruction {
   let Namespace = "ARM";
   dag OutOperandList = outs;
   dag InOperandList = ins;
   let AsmString   = asmstr;
   let Pattern = pattern;
 }

 def SDT_ARMCallSeq : SDTypeProfile<0, 1, [ SDTCisVT<0, i32> ]>;
 def callseq_start  : SDNode<"ISD.CALLSEQ_START", SDT_ARMCallSeq, [SDNPHasChain]>;
 def callseq_end    : SDNode<"ISD.CALLSEQ_END",   SDT_ARMCallSeq, [SDNPHasChain]>;

 def SDT_ARMcall    : SDTypeProfile<0, -1, [SDTCisInt<0>]>;
 def ARMcall        : SDNode<"ARMISD.CALL", SDT_ARMcall,
                            [SDNPHasChain, SDNPOptInFlag, SDNPOutFlag]>;

 def ADJCALLSTACKUP : InstARM<(outs), (ins i32imm:$amt),
                             "@ADJCALLSTACKUP $amt",
                             [(callseq_end imm:$amt)]>;

 def ADJCALLSTACKDOWN : InstARM<(outs), (ins i32imm:$amt),
                                "@ADJCALLSTACKDOWN $amt",
                                [(callseq_start imm:$amt)]>;

 let isReturn = 1 in {
   def bx: InstARM<(outs), (ins IntRegs:$dst), "bx $dst", [(brind IntRegs:$dst)]>;
 }

 def bl: InstARM<(outs), (ins i32imm:$func, variable_ops), "bl $func", [(ARMcall tglobaladdr:$func)]>;

 def ldr   : InstARM<(outs IntRegs:$dst), (ins memri:$addr),
                      "ldr $dst, [$addr]",
                      [(set IntRegs:$dst, (load iaddr:$addr))]>;

 def str  : InstARM<(outs), (ins IntRegs:$src, IntRegs:$addr),
                     "str $src, [$addr]",
                     [(store IntRegs:$src, IntRegs:$addr)]>;

 def movrr   : InstARM<(outs IntRegs:$dst), (ins IntRegs:$src),
                        "mov $dst, $src", []>;

 def movri   : InstARM<(outs IntRegs:$dst), (ins IntRegs:$src),
                        "mov $dst, $src", [(set IntRegs:$dst, imm:$src)]>;

 def addri   : InstARM<(outs IntRegs:$dst), (ins IntRegs:$a, i32imm:$b),
                        "add $dst, $a, $b",
 		       [(set IntRegs:$dst, (add IntRegs:$a, imm:$b))]>;

 def subri   : InstARM<(outs IntRegs:$dst), (ins IntRegs:$a, i32imm:$b),
                        "sub $dst, $a, $b",
 		       [(set IntRegs:$dst, (sub IntRegs:$a, imm:$b))]>;
