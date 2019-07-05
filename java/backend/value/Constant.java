package backend.value;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng
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

import backend.type.IntegerType;
import backend.type.Type;
import tools.APInt;
import tools.Util;

/**
 * The {@code Constant} instruction represents a constants such as an integer
 * inst, long, float, object reference, address, etc.
 */
public abstract class Constant extends User {
  /**
   * Constructs a new instruction representing the specified constants.
   */
  public Constant(Type ty, int valueKind) {
    super(ty, valueKind);
  }

  public static Constant getNullValue(Type type) {
    switch (type.getTypeID()) {
      case Type.IntegerTyID:
        return ConstantInt.get((IntegerType) type, 0);
      case Type.FloatTyID:
        return ConstantFP.get(type.getContext(), type, 0);
      case Type.DoubleTyID:
        return ConstantFP.get(type.getContext(), type, 0);
      case Type.PointerTyID:
        return ConstantPointerNull.get(type);
      case Type.StructTyID:
      case Type.ArrayTyID:
        return ConstantAggregateZero.get(type);
      default:
        return null;
    }
  }

  public abstract boolean isNullValue();

  public static Constant getAllOnesValue(Type ty) {
    if (ty instanceof IntegerType) {
      APInt val = APInt.getAllOnesValue(((IntegerType) ty).getBitWidth());
      return ConstantInt.get(ty.getContext(), val);
    }
    return null;
  }

  @Override
  public Constant operand(int index) {
    return (Constant) super.operand(index);
  }

  public boolean containsRelocations() {
    if (this instanceof GlobalValue)
      return true;
    for (int i = 0, e = getNumOfOperands(); i < e; i++)
      if (operand(i).containsRelocations())
        return true;
    return false;
  }

  /**
   * This method is a specialf form of {@linkplain Value#replaceAllUsesWith(Value)}
   * operates on {@linkplain Constant}. Basically this method gose through the trouble
   * of building a new constant that is equivalent to the current one, with all
   * from replaced with uses of to. After this construction is completed, all
   * of the uses of 'this' are replaced to use the new constant. In general,
   * you should not call this method, instead  use {@linkplain Value#replaceAllUsesWith(Value)}
   * , which automatically dispatches to this method as needed.
   *
   * @param from
   * @param to
   * @param u
   */
  public void replaceUsesOfWithOnConstant(Value from, Value to, Use u) {
    Util.assertion(getNumOfOperands() == 0, "replaceUsesOfWithOnConstant must be "
        + "implemented for all constants that have operands!");

    Util.assertion(false, "Constants that do not have operands cannot be using 'From'!");
  }

  public void destroyConstant() {
  }

  public static final int NoRelocation = 0;
  public static final int LocalRelocation = 1;
  public static final int GlobalRelocations = 2;

  public int getRelocationInfo() {
    if (this instanceof GlobalValue) {
      GlobalValue gv = (GlobalValue) this;
      if (gv.hasLocalLinkage() || gv.hasHiddenVisibility())
        return LocalRelocation;
      return GlobalRelocations;
    }

    int result = NoRelocation;
    for (int i = 0, e = getNumOfOperands(); i < e; i++)
      result = Math.max(result, operand(i).getRelocationInfo());
    return result;
  }

  public boolean canTrap() {
    Util.assertion(getType().isFirstClassType());

    if (!(this instanceof ConstantExpr)) return false;

    ConstantExpr ce = (ConstantExpr) this;
    for (int i = 0, e = getNumOfOperands(); i < e; i++) {
      if (operand(i).canTrap())
        return true;
    }

    switch (ce.getOpcode()) {
      default:
        return false;
      case UDiv:
      case SDiv:
      case FDiv:
      case URem:
      case SRem:
      case FRem:
        if (!(operand(1) instanceof ConstantInt) || operand(1).isNullValue())
          return true;
        return false;
    }
  }
}
