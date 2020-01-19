package cfe.sema;
/*
 * Extremely C language Compiler
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

import tools.Util;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public final class RedeclIterator<T extends Redeclarable<T>> {
  private Redeclarable<T> current;
  private Redeclarable<T> starter;

  public RedeclIterator(Redeclarable<T> cur) {
    current = cur;
    starter = cur;
  }

  /**
   * Obtain the next redeclaration by stepping over redeclaration link.
   *
   * @return The "next" redeclaration element that is previous.
   */
  public T getNext() {
    Util.assertion(hasNext(), "Must be calling hasNext() to ensure next is not null");
    T next = current.getRedeclLink().getNext();
    current = next == starter ? null : next;
    return next;
  }

  /**
   * Checking whether the iterator reaches the starter beginning to walk through
   * redeclaration link.
   *
   * @return Return true if there are remained redeclaration element to be walked.
   */
  public boolean hasNext() {
    return current != null;
  }
}
