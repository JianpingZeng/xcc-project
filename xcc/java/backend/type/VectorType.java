/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
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

package backend.type;

import tools.Util;

import java.util.TreeMap;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class VectorType extends SequentialType {
  private static class VectorValType implements Comparable<VectorValType> {
    Type valTy;
    long size;

    public VectorValType(Type ty, long sz) {
      valTy = ty;
      size = sz;
    }

    public static VectorValType get(VectorType ty) {
      return new VectorValType(ty.getElementType(), ty.getNumElements());
    }

    public static long hashTypeStructure(VectorType pt) {
      return pt.getNumElements();
    }

    @Override
    public int compareTo(VectorValType o) {
      if (size < o.size) return -1;
      if (size > o.size) return 1;
      return valTy.equals(o.valTy) ? 0 : -1;
    }
  }

  private static TreeMap<VectorValType, VectorType> vectorTypes =
      new TreeMap<>();

  private long numElts;

  protected VectorType(Type eleType, long numEles) {
    super(eleType.getContext(), VectorTyID, eleType);
    this.numElts = numEles;
    setAbstract(eleType.isAbstract);
    Util.assertion(numEles > 0);
    Util.assertion(isValidElementType(eleType));
  }

  public static VectorType get(Type eltType, long numElts) {
    Util.assertion(eltType != null);
    VectorValType key = new VectorValType(eltType, numElts);
    if (vectorTypes.containsKey(key))
      return vectorTypes.get(key);
    VectorType vt = new VectorType(eltType, numElts);
    vectorTypes.put(key, vt);
    return vt;
  }

  public static VectorType getInteger(VectorType vty) {
    int eltBits = vty.getElementType().getPrimitiveSizeInBits();
    Type eleTy = Type.getIntNTy(vty.getContext(), eltBits);
    return get(eleTy, vty.getNumElements());
  }

  public static VectorType getExtendedElementVectorType(VectorType vty) {
    int eltBits = vty.getElementType().getPrimitiveSizeInBits();
    Type eleTy = Type.getIntNTy(vty.getContext(), eltBits*2);
    return get(eleTy, vty.getNumElements());
  }

  public static VectorType getTruncatedElementVectorType(VectorType vty) {
    int eltBits = vty.getElementType().getPrimitiveSizeInBits();
    Util.assertion((eltBits & 1) == 0);
    Type eleTy = Type.getIntNTy(vty.getContext(), eltBits/2);
    return get(eleTy, vty.getNumElements());
  }

  public static boolean isValidElementType(Type eltTy) {
    return (eltTy.isIntegerTy() || eltTy.isFloatingPointType() ||
        eltTy instanceof OpaqueType);
  }

  public long getNumElements() {
    return numElts;
  }

  public long getBitWidth() {
    return numElts * getElementType().getPrimitiveSizeInBits();
  }

  @Override
  public void typeBecameConcrete(DerivedType absTy) {
    // TODO: 18-6-24
    super.typeBecameConcrete(absTy);
  }
}
