package cfe.codegen;
/*
 * Extremely C language CompilerInstance
 * Copyright (c) 2015-2020, Jianping Zeng.
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

import java.util.Set;
import java.util.TreeSet;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class CGRecordLayout {
  private backend.type.Type sty;
  private TreeSet<Integer> paddingFields;

  public CGRecordLayout(Type ty, Set<Integer> paddingFields) {
    sty = ty;
    this.paddingFields = new TreeSet<>();
    this.paddingFields.addAll(paddingFields);
  }

  public Type getLLVMType() {
    return sty;
  }

  public boolean isPaddingField(int no) {
    return paddingFields.contains(no);
  }

  public int getNumPaddingFields() {
    return paddingFields.size();
  }
}
