package jlang.codegen;
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
import tools.Pair;
import tools.Util;

/**
 * This trivial value class is used to represent the result of an expression
 * evaluated. It can be one of three cases: either a simple HIR SSA value,
 * a pair of SSA values for complex numbers, or the address of an aggregate value
 * in the memory.
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public final class RValue {
  private Value v1, v2;

  private enum Kind {Scalar, Complex, Aggregate}

  ;
  private Kind flavor;

  private boolean Volatile;

  public boolean isScalar() {
    return flavor == Kind.Scalar;
  }

  public boolean isComplex() {
    return flavor == Kind.Complex;
  }

  public boolean isAggregate() {
    return flavor == Kind.Aggregate;
  }

  public boolean isVolatileQualified() {
    return Volatile;
  }

  public Value getScalarVal() {
    Util.assertion(isScalar(), "Not a scalar!");
    return v1;
  }

  public Pair<Value, Value> getComplexVal() {
    Util.assertion(isComplex(), "Not a complex!");
    return new Pair<>(v1, v2);
  }

  public Value getAggregateAddr() {
    Util.assertion(isAggregate(), "Not a aggregate type!");
    return v1;
  }

  public static RValue get(Value v) {
    RValue rv = new RValue();
    rv.v1 = v;
    rv.flavor = Kind.Scalar;
    rv.Volatile = false;
    return rv;
  }

  public static RValue getComplex(Value v1, Value v2) {
    RValue rv = new RValue();
    rv.v1 = v1;
    rv.v2 = v2;
    rv.flavor = Kind.Complex;
    rv.Volatile = false;
    return rv;
  }

  public static RValue getComplex(Pair<Value, Value> c) {
    RValue rv = new RValue();
    rv.v1 = c.first;
    rv.v2 = c.second;
    rv.flavor = Kind.Complex;
    rv.Volatile = false;
    return rv;
  }

  public static RValue getAggregate(Value v) {
    return getAggregate(v, false);
  }

  public static RValue getAggregate(Value v, boolean isVolatile) {
    RValue rv = new RValue();
    rv.v1 = v;
    rv.Volatile = isVolatile;
    rv.flavor = Kind.Aggregate;
    return rv;
  }
}
