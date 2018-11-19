package backend.target.riscv;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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
 * The enumeration value for target-specific operations supported by RISCV target.
 *
 * @author Jianping Zeng.
 * @version 0.4
 */
public interface RISCVISD {
  int FIRST_NUMBER = ISD.BUILTIN_OP_END;
  int RET_FLAG = FIRST_NUMBER + 1;
  int URET_FLAG = RET_FLAG + 1;
  int SRET_FLAG = URET_FLAG + 1;
  int MRET_FLAG = SRET_FLAG + 1;
  int CALL = MRET_FLAG + 1;
  int SELECT_CC = CALL + 1;
  int BuildPairF64 = SELECT_CC + 1;
  int SplitF64 = BuildPairF64 + 1;
  int TAIL = SplitF64 + 1;
}
