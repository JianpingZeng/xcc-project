/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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
package utils.tablegen;

import tools.Util;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import static utils.tablegen.CodeGenHwModes.DefaultMode;

/**
 * @param <T>
 * @author Jianping Zeng.
 */
public abstract class InfoByHwMode<T> {
  protected TreeMap<Integer, T> map;

  public InfoByHwMode() {
    map = new TreeMap<>();
  }
  public Iterator<Map.Entry<Integer,T>> iterator() {
    return map.entrySet().iterator();
  }

  public int size() {return map.size(); }

  public boolean isEmpty() {
    return map.isEmpty();
  }

  public boolean hasMode(int m) {
    return map.containsKey(m);
  }

  public boolean hasDefault() {
    return hasMode(DefaultMode);
  }

  public abstract T get(int mode);

  public boolean isSimple() {
    if (map.size() != 1) return false;
    return map.firstKey() == DefaultMode;
  }

  public T getSimple() {
    Util.assertion(isSimple());
    return map.get(map.firstKey());
  }

  public void makeSimple(int mode) {
    Util.assertion(hasMode(mode) || hasDefault());
    T res = get(mode);
    map.clear();
    map.put(DefaultMode, res);
  }

  @Override
  public int hashCode() {
    return map.hashCode();
  }
}
