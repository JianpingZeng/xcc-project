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

package backend.value;

import backend.support.LLVMContext;
import backend.value.UniqueConstantValueImpl.MDNodeKeyType;
import tools.Util;

import java.util.ArrayList;
import java.util.List;

import static backend.value.UniqueConstantValueImpl.getUniqueImpl;
import static backend.value.ValueKind.MDNodeVal;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class MDNode extends MetadataBase {
  private ArrayList<Value> nodes;

  public MDNode(List<Value> vals) {
    super(LLVMContext.MetadataTy, MDNodeVal);
    nodes = new ArrayList<>();
    reserve(vals.size());
    for (int i = 0, e = vals.size(); i < e; i++) {
      MetadataBase mb = vals.get(i) instanceof MetadataBase ?
          (MetadataBase) vals.get(i) : null;
      if (mb != null)
        setOperand(i, mb);
      nodes.add(vals.get(i));
    }
  }

  public static MDNode get(List<Value> vals) {
    MDNodeKeyType key = new MDNodeKeyType(vals);
    return getUniqueImpl().getOrCreate(key);
  }

  public int getNumOfNode() {
    return nodes.size();
  }

  public Value getNode(int index) {
    Util.assertion(index >= 0 && index < getNumOfNode());
    return nodes.get(index);
  }
}
