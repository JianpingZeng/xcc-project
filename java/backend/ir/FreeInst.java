/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.ir;

import backend.type.Type;
import backend.value.BasicBlock;
import backend.value.Instruction;
import backend.value.Operator;
import backend.value.Value;
import tools.Util;

/**
 * An instruction to deallocate memory mallocated in Heap.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class FreeInst extends Instruction.UnaryInstruction {
  public FreeInst(Value ptr) {
    this(ptr, (Instruction) null);
  }

  public FreeInst(Value ptr, Instruction insertBefore) {
    super(Type.getVoidTy(ptr.getContext()), Operator.Free, ptr, "", insertBefore);
    assertOK();
  }

  public FreeInst(Value ptr, BasicBlock insertAtEnd) {
    super(Type.getVoidTy(ptr.getContext()), Operator.Free, ptr, "", insertAtEnd);
    assertOK();
  }

  private void assertOK() {
    Util.assertion(operand(0).getType().isPointerType(), "Ptr must have pointer type");
  }
}
