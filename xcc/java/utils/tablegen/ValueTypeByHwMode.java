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

package utils.tablegen;

import backend.codegen.MVT;
import tools.Pair;
import tools.Util;
import utils.tablegen.CodeGenHwModes.HwModeSelect;

import java.util.TreeSet;

import static backend.codegen.MVT.getEnumName;
import static utils.tablegen.CodeGenHwModes.DefaultMode;
import static utils.tablegen.CodeGenHwModes.getModeName;
import static utils.tablegen.CodeGenTarget.getValueType;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ValueTypeByHwMode extends InfoByHwMode<MVT> {

  public ValueTypeByHwMode(Record r, CodeGenHwModes cgh) {
    super();
    HwModeSelect select = cgh.getHwModeSelect(r);
    for (Pair<Integer, Record> itr : select.items) {
      Util.assertion(!map.containsKey(itr.first), "Duplicate entry?");
      map.put(itr.first, new MVT(getValueType(itr.second)));
    }
  }

  public ValueTypeByHwMode(MVT vt) {
    super();
    map.put(DefaultMode, vt);
  }

  public ValueTypeByHwMode() {
    super();
  }

  public MVT getType(int mode) {
    return get(mode);
  }

  public MVT getOrCreateTypeForMode(int mode, MVT type) {
    if (map.containsKey(mode))
      return map.get(mode);

    if (map.containsKey(DefaultMode)) {
      map.put(mode, map.get(DefaultMode));
      return map.get(DefaultMode);
    }
    map.put(mode, type);
    return type;
  }

  public static String getMVTName(MVT vt) {
    String name = getEnumName(vt.simpleVT);
    return name.substring("MVT.".length());
  }

  @Override
  public String toString() {
    if (isSimple()) {
      return getMVTName(getSimple());
    }
    TreeSet<Pair<Integer, MVT>> pairs = new TreeSet<>(
        (o1, o2) -> {
          if (o1.first < o2.first)
            return -1;
          if (!(o2.first < o1.first && o1.second.simpleVT < o2.second.simpleVT))
            return -1;
          return 1;
        }
    );
    map.forEach((m, vt) -> pairs.add(Pair.get(m, vt)));
    StringBuilder buf = new StringBuilder("{");
    int i = 0, e = pairs.size();
    for (Pair<Integer, MVT> itr : pairs) {
      buf.append("(").append(getModeName(itr.first)).append(":")
          .append(getMVTName(itr.second)).append(")");
      if (i != e - 1)
        buf.append(",");
    }
    buf.append("}");
    return buf.toString();
  }

  public void dump() {
    System.err.println(toString());
  }

  public static ValueTypeByHwMode getValueTypeByHwMode(Record rec, CodeGenHwModes cgh) {
    Util.assertion(rec.isSubClassOf("ValueType"), "Record must be derived from ValueType");
    if (rec.isSubClassOf("HwModeSelect"))
      return new ValueTypeByHwMode(rec, cgh);
    return new ValueTypeByHwMode(new MVT(getValueType(rec)));
  }

  public boolean isValid() {
    return !map.isEmpty();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (this == obj) return true;
    if (getClass() != obj.getClass()) return false;
    ValueTypeByHwMode mode = (ValueTypeByHwMode) obj;
    Util.assertion(isValid() && mode.isValid());
    boolean isSimple = isSimple();
    if (isSimple != mode.isSimple()) return false;
    if (isSimple)
      return getSimple().simpleVT == mode.getSimple().simpleVT;
    return map.equals(mode.map);
  }

  public MVT get(int mode) {
    if (!hasMode(mode)) {
      Util.assertion(hasDefault());
      map.put(mode, map.get(DefaultMode).clone());
    }
    return map.get(mode);
  }
}
