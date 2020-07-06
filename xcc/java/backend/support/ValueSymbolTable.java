package backend.support;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence.
 * For more details, please refers to the LICENSE file.
 */
import backend.value.Value;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Util;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class ValueSymbolTable {
  private TreeMap<String, Value> map;
  /**
   * Keeps a suffix number for those value with same name.
   */
  private TObjectIntHashMap<String> lastUnique;

  public ValueSymbolTable() {
    map = new TreeMap<>();
    lastUnique = new TObjectIntHashMap<>();
  }

  /**
   * Remove the value associate with the specified name from {@linkplain #map}.
   *
   * @param name The name to be handled.
   * @return The old value if the value associated with name exists in map.
   */
  public Value removeValueName(String name) {
    Util.assertion(name != null && !name.isEmpty());
    return map.remove(name);
  }

  /**
   * This method attempts to create a value name and insert
   * it into the symbol table with the specified name.  If it conflicts, it
   * auto-renames the name and returns that instead.
   *
   * @param name
   * @param value
   * @return
   */
  public String createValueName(String name, Value value) {
    Util.assertion(name != null && !name.isEmpty());
    // In the common case, the name is not already in the symbol table.
    if (!map.containsKey(name)) {
      map.put(name, value);
      lastUnique.put(name, 1);
      return name;
    }

    // Otherwise, there is a naming conflict. Rename this value.
    while (true) {
      int suffix = lastUnique.get(name);
      String uniqueName = name + suffix;
      lastUnique.put(name, suffix + 1);
      if (!map.containsKey(uniqueName)) {
        map.put(uniqueName, value);
        return uniqueName;
      }
    }
  }

  public Value getValue(String name) {
    Util.assertion(name != null && !name.isEmpty());
    return map.get(name);
  }

  public boolean isEmpty() {
    return map.isEmpty();
  }

  public int size() {
    return map.size();
  }

  public Map<String, Value> getMap() {
    return map;
  }

  public void dump() {
    // TODO: 17-11-7
  }
}
