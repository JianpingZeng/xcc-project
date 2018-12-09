/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

package jlang.sema;

import jlang.type.QualType;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class TypeLoc {
  protected QualType ty;
  private Object data;

  public TypeLoc(QualType ty, Object data) {
    this.ty = ty;
    this.data = data;
  }

  public TypeLoc() {
    ty = new QualType();
    data = null;
  }

  public boolean isNull() {
    return ty.isNull();
  }

  public QualType getSourceType() {
    return ty;
  }

  public TypeSpecLoc getTypeSpecLoc() {
    return null;
  }

  public static class TypeSpecLoc extends TypeLoc {

  }
}
