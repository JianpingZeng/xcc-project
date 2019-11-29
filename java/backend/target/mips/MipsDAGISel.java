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

import backend.codegen.EVT;
import backend.codegen.MVT;
import backend.codegen.dagisel.SDNode;
import backend.codegen.dagisel.SDValue;
import backend.codegen.dagisel.SelectionDAGISel;
import backend.target.TargetMachine;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class MipsDAGISel extends SelectionDAGISel {
  protected MipsSubtarget subtarget;

  public MipsDAGISel(MipsTargetMachine tm, TargetMachine.CodeGenOpt optLevel) {
    super(tm, optLevel);
    subtarget = tm.getSubtarget();
  }

  @Override
  public SDNode select(SDNode nodeToMatch) {
    return null;
  }

  protected SDValue getI32Imm(int val) {
    return curDAG.getTargetConstant(val, new EVT(MVT.i32));
  }

  /**
   * This function is used to read the address expression and determine what kinds of
   * address mode could be suitable for it. The computed operand of address expression
   * is returned in the {@arg tmp} array.
   * @param root
   * @param n
   * @param tmp
   * @return
   */
  protected boolean selectAddr(SDNode root, SDValue n, SDValue[] tmp) {
    return false;
  }
}
