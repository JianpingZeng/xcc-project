/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

package backend.value;

import backend.support.LLVMContext;

import static backend.value.UniqueConstantValueImpl.getUniqueImpl;
import static backend.value.ValueKind.MDStringVal;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class MDString extends Value {
  private String name;

  MDString(String str) {
    super(LLVMContext.LabelTy, MDStringVal);
    name = str;
  }

  public static MDString get(String name) {
    return getUniqueImpl().getOrCreate(name);
  }

  public String getString() {
    return name;
  }

  @Override
  public String toString() {
    return getString();
  }
}
