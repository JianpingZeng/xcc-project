package backend.codegen.dagisel;
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
public interface NodeFlag {

  int OPFL_None = 0;     // Node has no chain or flag input and isn't variadic.
  int OPFL_Chain = 1;     // Node has a chain input.
  int OPFL_FlagInput = 2;     // Node has a flag input.
  int OPFL_FlagOutput = 4;     // Node has a flag output.
  int OPFL_MemRefs = 8;     // Node gets accumulated MemRefs.
  int OPFL_Variadic0 = 1 << 4;  // Node is variadic, root has 0 fixed inputs.
  int OPFL_Variadic1 = 2 << 4;  // Node is variadic, root has 1 fixed inputs.
  int OPFL_Variadic2 = 3 << 4;  // Node is variadic, root has 2 fixed inputs.
  int OPFL_Variadic3 = 4 << 4;  // Node is variadic, root has 3 fixed inputs.
  int OPFL_Variadic4 = 5 << 4;  // Node is variadic, root has 4 fixed inputs.
  int OPFL_Variadic5 = 6 << 4;  // Node is variadic, root has 5 fixed inputs.
  int OPFL_Variadic6 = 7 << 4;  // Node is variadic, root has 6 fixed inputs.

  int OPFL_VariadicInfo = OPFL_Variadic6;
}
