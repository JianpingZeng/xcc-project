package backend.value;
/*
 * Extremely C Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng
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

import backend.support.Attribute;
import backend.type.Type;
import tools.Util;

import java.util.Objects;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class Argument extends Value {
  private Function parent;

  public Argument(Type ty) {
    this(ty, "", null);
  }

  public Argument(Type ty, String name, Function f) {
    super(ty, ValueKind.ArgumentVal);
    this.name = name;
    parent = f;
  }

  public Function getParent() {
    return parent;
  }

  public void setParent(Function bb) {
    parent = bb;
  }

  public boolean hasByValAttr() {
    if (!getType().isPointerType()) return false;
    return getParent().paramHasAttr(getArgNo() + 1, Attribute.ByVal);
  }

  private int getArgNo() {
    Function f = getParent();
    Util.assertion(f != null);
    int argIdx = 0;
    for (Argument arg : f.getArgumentList()) {
      if (arg == this)
        return argIdx;
      ++argIdx;
    }
    return argIdx;
  }
}
