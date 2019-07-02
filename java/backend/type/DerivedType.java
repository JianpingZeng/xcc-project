package backend.type;
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

import backend.support.LLVMContext;
import tools.Util;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class DerivedType extends Type {
  /**
   * This field used to implement the abstract type refine system.
   * When an abstract type be used to concreted, this field would be set as
   * the corresponding new type.
   */
  private Type forwardType;

  protected DerivedType(int typeID) {
    super(typeID);
  }

  protected void notifyUsesThatTypeBecameConcrete() {
    int oldSize = abstractTypeUsers.size();
    while (!abstractTypeUsers.isEmpty()) {
      AbstractTypeUser user = abstractTypeUsers.getLast();
      user.typeBecameConcrete(this);

      Util.assertion(abstractTypeUsers.size() < oldSize--, "AbstractTypeUser did not remove ifself");
    }
  }

  private void unlockRefineAbstractTypeTo(Type newType) {
    Util.assertion(isAbstract(), "refinedAbstractTypeto: Current type is not abstract");
    Util.assertion(this != newType, "Can not refine to itself!");

    forwardType = newType;

    dropAllTypeUses();
    while (!abstractTypeUsers.isEmpty() && newType != this) {
      AbstractTypeUser user = abstractTypeUsers.getLast();
      int oldSize = abstractTypeUsers.size();
      user.refineAbstractType(this, newType);

      Util.assertion(abstractTypeUsers.size() != oldSize, "AbstractTypeUser did not remove ifself from user list!");

    }
  }

  public void dropAllTypeUses() {
    if (getNumContainedTypes() > 0) {
      containedTys[0] = new PATypeHandle(OpaqueType.get(), this);

      for (int i = 0, e = getNumContainedTypes(); i < e; i++)
        containedTys[i] = new PATypeHandle(LLVMContext.Int32Ty, this);
    }
  }

  public void refineAbstractTypeTo(Type newType) {
    unlockRefineAbstractTypeTo(newType);
  }

  public void dump() {
    super.dump();
  }

  public Type getForwardType() {
    return forwardType;
  }
}
