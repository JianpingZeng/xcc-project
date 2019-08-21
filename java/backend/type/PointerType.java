package backend.type;
/*
 * Extremely C language CompilerInstance
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

import tools.Util;

import java.util.HashMap;
import java.util.Objects;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class PointerType extends SequentialType {
  private static HashMap<Type, PointerType> pointerTypes;

  static {
    pointerTypes = new HashMap<>();
  }

  private int addressSpace;

  protected PointerType(Type elemType, int addrSpace) {
    super(elemType.getContext(), PointerTyID, elemType);
    addressSpace = addrSpace;

    setAbstract(elemType.isAbstract());
  }

  public static PointerType get(Type valueType, int addrSpace) {
    Util.assertion(valueType != null, "Can't get a pointer to <null> type");
    PointerType pt = pointerTypes.get(valueType);
    if (pt != null)
      return pt;

    pt = new PointerType(valueType, addrSpace);
    pointerTypes.put(valueType, pt);
    return pt;
  }

  /**
   * This constructs a pointer to an object of the
   * specified type in the generic address space (address space zero).
   *
   * @param elemType
   * @return
   */
  public static PointerType getUnqual(Type elemType) {
    return get(elemType, 0);
  }

  public static boolean isValidElementType(Type eleTy) {
    return !(eleTy.isVoidType() || eleTy.isLabelTy());
  }

  public int getAddressSpace() {
    return addressSpace;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (this == obj) return true;
    if (getClass() != obj.getClass())
      return false;
    PointerType ty = (PointerType) obj;
    return numElts == ty.getNumContainedTypes() //&& isAbstract == ty.isAbstract
        && getAddressSpace() == ty.getAddressSpace() &&
        (getElementType() == ty.getElementType() || Objects.equals(getElementType(), ty.getElementType()));
  }

  @Override
  public int hashCode() {
    int code = getAddressSpace();
    for (int i = 0, e = getNumContainedTypes(); i < e; i++) {
      if (getContainedType(i) != null)
        code |= getContainedType(i).hashCode();
    }
    code |= getNumContainedTypes();
    return code;
  }

  @Override
  public int getNumContainedTypes() {
    return 1;
  }
}
