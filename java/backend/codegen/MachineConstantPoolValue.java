package backend.codegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import backend.type.Type;
import tools.FoldingSetNodeID;

import java.io.PrintStream;

/**
 * Abstract base class for all machine specific constant pool value subclass.
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public abstract class MachineConstantPoolValue {
  private Type ty;

  public MachineConstantPoolValue(Type ty) {
    this.ty = ty;
  }

  public Type getType() {
    return ty;
  }

  public abstract int getRelocationInfo();

  public abstract int getExistingMachineCPValue(MachineConstantPool pool, int align);

  public abstract void addSelectionDAGCSEId(FoldingSetNodeID id);

  public abstract void print(PrintStream os);
}
