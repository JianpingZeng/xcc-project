/*
 * Extremely C language Compiler
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

package cfe.cparser;

import tools.Util;

import java.util.ArrayList;

/**
 * Represent a full designation, which is a sequence of
 * designators.  This class is mostly a helper for initListDesignations.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class Designation {
  /**
   * The index of the initializer expression this is for.  For
   * example, if the initializer were "{ A, .foo=B, C }" a Designation would
   * exist with InitIndex=1, because element #1 has a designation.
   */
  private int initIndex;

  /**
   * The actual designators for this initializer.
   */
  private ArrayList<Designator> designators;

  public Designation() {
    this(Integer.MAX_VALUE);
  }

  public Designation(int idx) {
    initIndex = idx;
    designators = new ArrayList<>();
  }

  public void addDesignator(Designator d) {
    designators.add(d);
  }

  public boolean isEmpty() {
    return designators.isEmpty();
  }

  public int getNumDesignators() {
    return designators.size();
  }

  public Designator getDesignator(int idx) {
    Util.assertion(idx >= 0 && idx < getNumDesignators());
    return designators.get(idx);
  }

  public void clearExprs() {
    for (Designator d : designators)
      d.clearExprs();
  }
}
