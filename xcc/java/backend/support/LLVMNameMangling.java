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

import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Util;

/**
 * This class used for computing unique name for LLVM value when there is no
 * SymbolTable exists for it.
 */
public class LLVMNameMangling {
  private static final TObjectIntHashMap<String> UniqueNames
      = new TObjectIntHashMap<>();

  public static String computeUniqueName(String name) {
    Util.assertion(name != null);
    if (UniqueNames.containsKey(name)) {
      int nextSuffix = UniqueNames.get(name) + 1;
      UniqueNames.put(name, nextSuffix);
      return name + nextSuffix;
    }
    UniqueNames.put(name, 1);
    return name;
  }
}
