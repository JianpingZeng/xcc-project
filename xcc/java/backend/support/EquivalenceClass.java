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

package backend.support;

import java.util.HashSet;
import java.util.Iterator;

/**
 * This file defines a class that implements an equivalence set.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class EquivalenceClass<T> {
  /**
   * This class represents a node in equivalence set.
   */
  public static class ECNode<T> {
    /**
     * The value of this node contains
     */
    private T val;
    /**
     * The parent node in corresponding parent-link tree.
     */
    private ECNode<T> parent;
    /**
     * The next node in the same equivalence class.
     */
    private ECNode<T> next;
    /**
     * The previous node in the same equivalence class.
     */
    private ECNode<T> prev;

    public ECNode(T val) {
      this.val = val;
      parent = this;
      next = prev = null;
    }

    public boolean isLeading() {
      return parent == this;
    }

    public ECNode<T> getLeading() {
      if (isLeading()) return parent;
      if (parent.isLeading()) return parent;
      return parent.getLeading();
    }

    public T getValue() {
      return val;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass())
        return false;

      ECNode<T> rhs = (ECNode<T>) obj;
      return rhs.val.equals(val);
    }

    @Override
    public int hashCode() {
      return val.hashCode();
    }
  }

  private HashSet<ECNode<T>> ecs;

  public EquivalenceClass() {
    ecs = new HashSet<>();
  }

  /**
   * Insert a new value into the union/find set, ignore it if present.
   *
   * @param data
   */
  public void insert(T data) {
    if (data == null) return;
    ecs.add(new ECNode<>(data));
  }

  public ECNode<T> findLeading(T data) {
    if (data == null) return null;
    ECNode<T> t = new ECNode<>(data);
    for (Iterator<ECNode<T>> itr = ecs.iterator(); itr.hasNext(); ) {
      ECNode<T> val = itr.next();
      if (t.equals(val))
        return val.getLeading();
    }
    return null;
  }

  public void union(T val1, T val2) {
    if (val1 == null || val2 == null || val1 == val2)
      return;

    ECNode<T> leading1 = findLeading(val1);
    ECNode<T> leading2 = findLeading(val2);
    if (leading1 == null || leading2 == null)
      return;

    // link leading2 into leading1.
    leading2.parent = leading1;

    leading2.next = leading1.next;
    if (leading2.next != null)
      leading2.next.prev = leading2;

    leading1.next = leading2;
    leading2.prev = leading1;
  }

  public HashSet<ECNode<T>> getECs() {
    return ecs;
  }

  public void clear() {
    ecs.clear();
  }

  public boolean isEmpty() {
    return ecs.isEmpty();
  }
}
