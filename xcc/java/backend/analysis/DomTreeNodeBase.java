package backend.analysis;
/*
 * Extremely Compiler Collection
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

import tools.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * This class for defining actual dominator tree node.
 */
public class DomTreeNodeBase<T> implements Iterable<DomTreeNodeBase<T>> {
  /**
   * The corresponding basic block of this dominator tree node.
   */
  private T block;

  /**
   * Immediate dominator.
   */
  private DomTreeNodeBase<T> iDom;
  /**
   * All of child node.
   */
  private ArrayList<DomTreeNodeBase<T>> children;

  public int DFSNumIn = -1;
  public int DFSNumOut = -1;

  public DomTreeNodeBase() {
    iDom = null;
    children = new ArrayList<>();
  }

  public DomTreeNodeBase(T bb, DomTreeNodeBase<T> iDom) {
    this.block = bb;
    this.iDom = iDom;
    children = new ArrayList<>();
  }

  public T getBlock() {
    return block;
  }

  public DomTreeNodeBase<T> getIDom() {
    return iDom;
  }

  public void setIDom(DomTreeNodeBase<T> newIDom) {
    //Util.assertion(iDom != null,  "No immediate dominator");
    if (iDom == null) {
      iDom = newIDom;
      newIDom.children.add(this);
    } else if (iDom != newIDom) {
      Util.assertion(iDom.children.contains(this), "Not in immediate dominator chidren set");

      // erase this, no longer idom's child
      iDom.children.remove(this);
      this.iDom = newIDom;
      newIDom.children.add(this);
    }
  }

  public ArrayList<DomTreeNodeBase<T>> getChildren() {
    return children;
  }

  @Override
  public Iterator<DomTreeNodeBase<T>> iterator() {
    if (children == null)
      return Collections.emptyIterator();
    else
      return children.iterator();
  }

  public DomTreeNodeBase<T> addChidren(DomTreeNodeBase<T> C) {
    children.add(C);
    return C;
  }

  public final int getNumbChidren() {
    return children.size();
  }

  public final void clearChidren() {
    children.clear();
  }

  /**
   * ReturnInst true if this node is dominated by other.
   * Use this only if DFS info is valid.
   *
   * @param other
   * @return
   */
  public boolean dominatedBy(DomTreeNodeBase<T> other) {
    return this.DFSNumIn >= other.DFSNumIn
        && this.DFSNumOut <= other.DFSNumOut;
  }

  public int getDFSNumIn() {
    return DFSNumIn;
  }

  public int getDFSNumOut() {
    return DFSNumOut;
  }
}
