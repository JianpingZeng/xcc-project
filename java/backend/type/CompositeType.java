package backend.type;
/*
 * Extremely C language CompilerInstance
 * Copyright (c) 2015-2018, Jianping Zeng
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

import backend.value.Value;

/**
 * Common super class of ArrayType, StructType, and PointerType.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public abstract class CompositeType extends Type {
  protected CompositeType(int typeID) {
    super(typeID);
  }

  public abstract Type getTypeAtIndex(Value v);

  public abstract Type getTypeAtIndex(int idx);

  public abstract boolean indexValid(Value v);

  public abstract boolean indexValid(int idx);

  // getIndexType - Return the type required of indices for this composite.
  // For structures, this is ubyte, for arrays, this is uint.
  public abstract Type getIndexType();
}
