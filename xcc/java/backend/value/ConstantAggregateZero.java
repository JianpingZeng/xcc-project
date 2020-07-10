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

import backend.type.Type;
import tools.Util;

import java.util.HashMap;
import java.util.Objects;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class ConstantAggregateZero extends Constant {
  private static HashMap<Type, ConstantAggregateZero> aggZeroConstants;

  static {
    aggZeroConstants = new HashMap<>();
  }

  /**
   * Constructs a new instruction representing the specified constant.
   *
   * @param ty
   */
  public ConstantAggregateZero(Type ty) {
    super(ty, ValueKind.ConstantAggregateZeroVal);
  }

  public static ConstantAggregateZero get(Type ty) {
    Util.assertion(ty.isArrayType() || ty.isStructType(), "Invalid aggregate type!");
    if (aggZeroConstants.containsKey(ty))
      return aggZeroConstants.get(ty);

    ConstantAggregateZero res = new ConstantAggregateZero(ty);
    aggZeroConstants.put(ty, res);
    return res;
  }

  @Override
  public boolean isNullValue() {
    return true;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    return Objects.deepEquals(((ConstantAggregateZero) obj).getType(), getType());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getType());
  }
}
