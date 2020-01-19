package tools;
/*
 * Extremely C language Compiler
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public final class SetMultiMap<K, V> {
  private HashMap<K, Set<V>> map;

  public SetMultiMap() {
    map = new HashMap<>();
  }

  /**
   * Return the number of keys.
   *
   * @return
   */
  public int size() {
    return map.size();
  }

  public boolean isEmpty() {
    return map.isEmpty();
  }

  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  public boolean containsValue(Object value) {
    for (Set<V> vals : map.values()) {
      for (V v : vals)
        if (Objects.equals(v, value))
          return true;
    }
    return false;
  }

  public Set<V> get(Object key) {
    if (map.containsKey(key))
      return map.get(key);
    return null;
  }

  /**
   * put a pair of key-value into this multimap.
   *
   * @param key
   * @param value
   * @return
   */
  public void put(K key, V value) {
    if (map.containsKey(key)) {
      map.get(key).add(value);
      return;
    }
    HashSet<V> set = new HashSet<V>();
    set.add(value);
    map.put(key, set);
  }

  /**
   * Remove the specified key-value pair. Also this map keeps unchanged when
   * the specified pair is not contained in this multimap.
   *
   * @param key
   * @param value
   * @return
   */
  public boolean remove(K key, V value) {
    if (!containsKey(key) || !containsValue(value))
      return false;
    Set<V> set = get(key);
    boolean res = set.remove(value);
    if (set.isEmpty())
      map.remove(key);
    return res;
  }

  public void clear() {
    map.clear();
  }

  public Set<K> keySet() {
    return map.keySet();
  }

  @Override
  public boolean equals(Object o) {
    return false;
  }

  @Override
  public int hashCode() {
    return 0;
  }
}
