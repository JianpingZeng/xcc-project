package cfe.sema;
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

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class DeclLink<T extends Redeclarable<T>> {
  T decl;
  private boolean isLatest;

  DeclLink(T d, boolean isLatest) {
    decl = d;
    this.isLatest = isLatest;
  }

  boolean nextIsPrevious() {
    return !isLatest;
  }

  boolean nextIsLatest() {
    return isLatest;
  }

  T getNext() {
    return decl;
  }

  public static class PreviousDeclLink<T extends Redeclarable<T>> extends DeclLink<T> {
    PreviousDeclLink(T d) {
      super(d, false);
    }
  }

  public static class LatestDeclLink<T extends Redeclarable<T>> extends DeclLink<T> {
    LatestDeclLink(T d) {
      super(d, true);
    }
  }
}
