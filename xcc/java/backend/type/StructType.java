package backend.type;
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

import backend.support.LLVMContext;
import backend.value.Constant;
import backend.value.ConstantInt;
import backend.value.Value;
import tools.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

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
      int code = 1;
      for (Type ty : elemTypes)
        code |= ty.hashCode();

      code |= elemTypes.size() << 11 | (packed ? 1 : 0);
      return code;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) return false;
      if (this == obj) return true;
      if (getClass() != obj.getClass()) return false;

      StructValType svt = (StructValType) obj;
      if (elemTypes.size() != svt.elemTypes.size() ||
          packed != svt.packed) return false;
      for (int i = 0, e = elemTypes.size(); i < e; i++)
        if (!Objects.equals(elemTypes.get(i), svt.elemTypes.get(i)))
          return false;

      return true;
    }
  }

  private boolean packed;
  private boolean hasBody;
  private boolean literal;

  private static HashMap<StructValType, StructType> structTypes = new HashMap<>();

  /**
   * A place holder type.
   */
  private static StructType PlaceHolderType;
  private String name;

  private StructType(LLVMContext ctx) {
    super(ctx, StructTyID);
    packed = false;
    hasBody = false;
    literal = false;
    name = "";
  }

  public static boolean isValidElementType(Type elemTy) {
    return !elemTy.isVoidType() && !elemTy.isLabelTy();
  }

  public static StructType create(LLVMContext ctx) {
    return create(ctx, "");
  }

  public static StructType create(LLVMContext ctx, String name) {
    StructType st = new StructType(ctx);
    if (name != null && !name.isEmpty())
      st.setName(name);
    return st;
  }

  public static StructType create(LLVMContext ctx, ArrayList<Type> elementTypes, String name) {
    return create(ctx, elementTypes, name, false);
  }

  public static StructType create(LLVMContext ctx, ArrayList<Type> elementTypes, String name, boolean isPacked) {
    Util.assertion(elementTypes != null && !elementTypes.isEmpty(),
        "this method may not be invoked with an empty list");
    StructType st = create(ctx, name);
    st.setBody(elementTypes, isPacked);
    return st;
  }

  /**
   * The serial of get method are used to create a literal struct type object.
   * @param memberTypes
   * @param isPacked
   * @return
   */
  public static StructType get(LLVMContext ctx, ArrayList<Type> memberTypes, boolean isPacked) {
    StructValType svt = new StructValType(memberTypes, isPacked);
    StructType st = structTypes.get(svt);
    if (st != null)
      return st;

    st = new StructType(ctx);
    structTypes.put(svt, st);
    st.literal = true;
    st.setBody(memberTypes, isPacked);
    return st;
  }

  public void setBody(ArrayList<Type> eltTypes, boolean isPacked) {
    Util.assertion(isOpaqueTy(), "struct body already set");
    hasBody = true;
    containedTys = new PATypeHandle[eltTypes.size()];
    for (int i = 0, e = eltTypes.size(); i < e; i++) {
      Util.assertion(eltTypes.get(i) != null, "<null> type for structure type!");
      Util.assertion(isValidElementType(eltTypes.get(i)), "Invalid type for structure element!");
      isAbstract |= eltTypes.get(i).isAbstract();
      containedTys[i] = new PATypeHandle(eltTypes.get(i), this);
    }
    setAbstract(isAbstract);
    this.packed = isPacked;
  }

  public static StructType get(LLVMContext ctx, boolean packed) {
    return get(ctx, new ArrayList<>(), packed);
  }

  public static StructType get(LLVMContext ctx, Type... tys) {
    ArrayList<Type> elts = new ArrayList<>(Arrays.asList(tys));
    return get(ctx, elts, false);
  }

  public static StructType get(LLVMContext ctx) {
    return get(ctx, false);
  }

  @Override
  public Type getTypeAtIndex(Value v) {
    Util.assertion(v instanceof Constant);
    Util.assertion(v.getType().isIntegerTy(32));
    int idx = (int) ((ConstantInt) v).getZExtValue();
    Util.assertion(idx < containedTys.length);
    Util.assertion(indexValid(v));
    return containedTys[idx].getType();
  }

  @Override
  public boolean indexValid(Value v) {
    if (!(v instanceof Constant)) return false;
    if (!v.getType().isIntegerTy(32)) return false;
    int idx = (int) ((ConstantInt) v).getZExtValue();

    return idx < getNumOfElements();
  }

  @Override
  public boolean indexValid(int idx) {
    return idx >= 0 && idx < getNumContainedTypes();
  }

  @Override
  public Type getIndexType() {
    return Type.getInt32Ty(getContext());
  }

  public int getNumOfElements() {
    return containedTys == null ? 0: containedTys.length;
  }

  public Type getElementType(int idx) {
    Util.assertion(idx >= 0 && idx < getNumOfElements(), "index out of range");
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
  public void typeBecameConcrete(DerivedType absTy) {
    // TODO: 17-6-11
    super.typeBecameConcrete(absTy);
  }

  public String getName() { return name;  }
  public void setName(String name) { this.name = name; }
  @Override
  public boolean isOpaqueTy() { return !hasBody; }
}
