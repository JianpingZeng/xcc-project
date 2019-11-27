package backend.target.mips;
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

import backend.codegen.dagisel.ISD;

/**
 * This interface defines all selection dag node types for the Mips target.
 * @author Jianping Zeng.
 * @version 0.4
 */
public interface MipsISD {
  // Start the numbering from where ISD NodeType finishes.
  int FIRST_NUMBER = ISD.BUILTIN_OP_END;

  // Jump and link (call)
  int JmpLink = FIRST_NUMBER + 1;

  // Get the Higher 16 bits from a 32-bit immediate
  // No relation with Mips Hi register
  int Hi = JmpLink + 1;

  // Get the Lower 16 bits from a 32-bit immediate
  // No relation with Mips Lo register
  int Lo = Hi + 1;

  // Handle gp_rel (small data/bss sections) relocation.
  int GPRel = Lo + 1;

  // General Dynamic TLS
  int TlsGd = GPRel + 1;

  // Local Exec TLS
  int TprelHi = TlsGd + 1;
  int TprelLo = TprelHi + 1;

  // Thread Pointer
  int ThreadPointer = TprelLo + 1;

  // Floating Point Branch Conditional
  int FPBrcond = ThreadPointer + 1;

  // Floating Point Compare
  int FPCmp = FPBrcond + 1;

  // Floating Point Conditional Moves
  int CMovFP_T = FPCmp + 1;
  int CMovFP_F = CMovFP_T + 1;

  // Floating Point Rounding
  int FPRound = CMovFP_F + 1;

  // Return
  int Ret = FPRound + 1;

  // MAdd/Sub nodes
  int MAdd = Ret + 1;
  int MAddu = MAdd + 1;
  int MSub = MAddu + 1;
  int MSubu = MSub + 1;

  // DivRem(u)
  int DivRem = MSubu + 1;
  int DivRemU = DivRem + 1;

  int BuildPairF64 = DivRemU + 1;
  int ExtractElementF64 = BuildPairF64 + 1;

  int WrapperPIC = ExtractElementF64 + 1;

  int DynAlloc = WrapperPIC + 1;

  int Sync = DynAlloc + 1;

  int Ext = Sync + 1;
  int Ins = Ext + 1;
}
