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

import backend.support.LLVMContext;
import backend.value.Constant;
import backend.value.ConstantInt;
import backend.value.Value;
import tools.TypeMap;
import tools.Util;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class StructType extends CompositeType {
  static class StructValType {
    ArrayList<Type> elemTypes;
    private boolean packed;

    StructValType(ArrayList<Type> args, boolean packed) {
      elemTypes = new ArrayList<>(args.size());
      elemTypes.addAll(args);
      this.packed = packed;
    }

    @Override
    public int hashCode() {
      return elemTypes.hashCode() << 23 + elemTypes.size() << 11 + (packed ? 1 : 0);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) return false;
      if (this == obj) return true;
      if (getClass() != obj.getClass()) return false;

      StructValType svt = (StructValType) obj;

      return elemTypes.equals(svt.elemTypes) && packed == svt.packed;
    }
  }

  private boolean packed;
  private boolean hasBody;
  private boolean literal;

  private static TypeMap<StructValType, StructType> structTypes = new TypeMap<>();

  /**
   * A place holder type.
   */
  private static StructType PlaceHolderType;
  private String name;

  private StructType() {
    super(StructTyID);
    packed = false;
    hasBody = false;
    literal = false;
    name = "";
  }

  public static boolean isValidElementType(Type elemTy) {
    return !elemTy.equals(LLVMContext.VoidTy) && !elemTy.equals(LLVMContext.LabelTy);
  }

  public static StructType create() {
    return create("");
  }

  public static StructType create(String name) {
    StructType st = new StructType();
    if (name != null && !name.isEmpty())
      st.setName(name);
    return st;
  }

  public static StructType create(ArrayList<Type> elementTypes, String name) {
    return create(elementTypes, name, false);
  }

  public static StructType create(ArrayList<Type> elementTypes, String name, boolean isPacked) {
    Util.assertion(elementTypes != null && !elementTypes.isEmpty(),
        "this method may not be invoked with an empty list");
    StructType st = create(name);
    st.setBody(elementTypes, isPacked);
    return st;
  }

  /**
   * The serial of get method are used to create a literal struct type object.
   * @param memberTypes
   * @param isPacked
   * @return
   */
  public static StructType get(ArrayList<Type> memberTypes, boolean isPacked) {
    StructValType svt = new StructValType(memberTypes, isPacked);
    StructType st = structTypes.get(svt);
    if (st != null)
      return st;

    st = new StructType();
    structTypes.put(svt, st);
    st.literal = true;
    st.setBody(memberTypes, isPacked);
    return st;
  }

  public void setBody(ArrayList<Type> eltTypes, boolean isPacked) {
    Util.assertion(isOpaque(), "struct body already set");
    hasBody = true;
    containedTys = new PATypeHandle[eltTypes.size()];
    for (int i = 0, e = eltTypes.size(); i < e; i++) {
      Util.assertion(eltTypes.get(i) != null, "<null> type for structure type!");
      Util.assertion(isValidElementType(eltTypes.get(i)), "Invalid type for structure element!");
      isAbstract |= eltTypes.get(i).isAbstract();
      containedTys[i] = new PATypeHandle(eltTypes.get(i), this);
    }
    setAbstract(isAbstract);
  }

  public static StructType get(boolean packed) {
    return get(new ArrayList<>(), packed);
  }

  public static StructType get(Type... tys) {
    ArrayList<Type> elts = new ArrayList<>(Arrays.asList(tys));
    return get(elts, false);
  }

  public static StructType get() {
    return get(false);
  }

  @Override
  public Type getTypeAtIndex(Value v) {
    Util.assertion(v instanceof Constant);
    Util.assertion(v.getType() == LLVMContext.Int32Ty);
    int idx = (int) ((ConstantInt) v).getZExtValue();
    Util.assertion(idx < containedTys.length);
    Util.assertion(indexValid(v));
    return containedTys[idx].getType();
  }

  @Override
  public boolean indexValid(Value v) {
    if (!(v instanceof Constant)) return false;
    if (v.getType() != LLVMContext.Int32Ty) return false;
    int idx = (int) ((ConstantInt) v).getZExtValue();

    return idx < containedTys.length;
  }

  @Override
  public boolean indexValid(int idx) {
    return idx >= 0 && idx < getNumContainedTypes();
  }

  @Override
  public Type getIndexType() {
    return LLVMContext.Int32Ty;
  }

  public int getNumOfElements() {
    return containedTys.length;
  }

  public Type getElementType(int idx) {
    return containedTys[idx].getType();
  }

  @Override
  public Type getTypeAtIndex(int index) {
    Util.assertion(index >= 0 && index < getNumContainedTypes(),
        "Invalid structure index!");
    return containedTys[index].getType();
  }

  public boolean isPacked() {
    return packed;
  }

  public boolean isHasBody() {
    return hasBody;
  }

  public boolean isLiteral() {
    return literal;
  }

  @Override
  public void refineAbstractType(DerivedType oldTy, Type newTy) {
    for (StructValType svt : structTypes.keySet()) {
      // TODO: 17-6-11
    }
  }

  @Override
  public void typeBecameConcrete(DerivedType absTy) {
    // TODO: 17-6-11
    super.typeBecameConcrete(absTy);
  }


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isOpaque() {
    return !hasBody;
  }
}
