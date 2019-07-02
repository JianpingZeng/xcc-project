package backend.codegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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
import backend.value.Constant;
import tools.Util;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class MachineConstantPoolEntry {
  // The constant itself.
  // It be type of Constant or MachineConstantPoolValue.
  public Object val;

  int alignemnt;

  public MachineConstantPoolEntry(Constant v, int align) {
    val = v;
    alignemnt = align;
  }

  public MachineConstantPoolEntry(MachineConstantPoolValue v, int align) {
    val = v;
    alignemnt = align;
    alignemnt |= (1 << 31);
  }

  public boolean isMachineConstantPoolEntry() {
    return alignemnt < 0;
  }


  public Type getType() {
    if (isMachineConstantPoolEntry())
      return getValueAsCPV().getType();
    return getValueAsConstant().getType();
  }

  public int getRelocationInfo() {
    if (isMachineConstantPoolEntry())
      return getValueAsCPV().getRelocationInfo();
    return getValueAsConstant().getRelocationInfo();
  }

  public int getAlignment() {
    return alignemnt;
  }

  public MachineConstantPoolValue getValueAsCPV() {
    Util.assertion(val instanceof MachineConstantPoolValue);
    return (MachineConstantPoolValue) val;
  }

  public Constant getValueAsConstant() {
    Util.assertion(val instanceof Constant);
    return (Constant) val;
  }
}
