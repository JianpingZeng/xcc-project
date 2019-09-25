package backend.ir;
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

import backend.type.Type;
import backend.value.*;
import tools.Util;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class IndirectBrInst extends Instruction.TerminatorInst {
  // Operand[0]    = Value to switch on
  // Operand[1]    = Default basic block destination
  // Operand[2n  ] = Value to match
  // Operand[2n+1] = BasicBlock to go to on match
  IndirectBrInst(Value address, int numDests, Instruction insertBefore) {
    super(Type.getVoidTy(address.getContext()), Operator.IndirectBr, null, insertBefore);
    init(address, numDests);
  }

  IndirectBrInst(Value address, int numDests, BasicBlock insertAtEnd) {
    super(Type.getVoidTy(address.getContext()), Operator.IndirectBr, null, insertAtEnd);
    init(address, numDests);
  }

  public static IndirectBrInst create(Value addr, int numDests) {
    return create(addr, numDests, (Instruction) null);
  }

  public static IndirectBrInst create(Value addr, int numDests, Instruction insertBefore) {
    return new IndirectBrInst(addr, numDests, insertBefore);
  }

  public static IndirectBrInst create(Value addr, int numDests, BasicBlock insertAtEnd) {
    return new IndirectBrInst(addr, numDests, insertAtEnd);
  }

  private void init(Value addr, int numDests) {
    Util.assertion(addr != null && addr.getType().isPointerType(),
        "Address of indirectbr must be a pointer");
    numOps = 1;
    operandList = new Use[1 + numDests];
    operandList[0] = new Use(addr, this);
  }

  public Value getAddress() { return operand(0); }

  public void setAddress(Value addr) { setOperand(0, addr);}

  public int getNumDestinations() { return getNumOfOperands() - 1;}

  public BasicBlock getDestination(int i) {
    return getSuccessor(i);
  }

  public void addDestination(BasicBlock dest) {
    int opNo = numOps;
    if (opNo >= operandList.length) {
      Use[] temp = new Use[opNo+1];
      System.arraycopy(operandList, 0, temp, 0, operandList.length);
      operandList = temp;
    }
    Util.assertion(opNo < operandList.length, "growing didn't work?");
    numOps = opNo + 1;
    operandList[opNo] = new Use(dest, this);
  }

  public void removeDestination(int i) {
    Util.assertion(i < getNumOfOperands() - 1, "Successor index out of range!");
    // replace the deleting value with the last one.
    operandList[i] = operandList[getNumOfOperands() - 1];
    operandList[getNumOfOperands()-1].setValue(null);
    operandList[getNumOfOperands()-1] = null;
    --numOps;
  }

  @Override
  public BasicBlock getSuccessor(int index) {
    return (BasicBlock) operand(index + 1);
  }

  @Override
  public int getNumOfSuccessors() {
    return getNumOfOperands() - 1;
  }

  @Override
  public void setSuccessor(int index, BasicBlock bb) {
    setOperand(index + 1, bb);
  }
}
