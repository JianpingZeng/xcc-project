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

package utils.tablegen;

import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Error;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class CodeGenHwModes {
  public static class HwMode {
    public HwMode(Record r) {
      name = r.getName();
      features = r.getValueAsString("Features");
    }

    public String name;
    public String features;

    public void dump() {
      System.err.printf("%s: %s\n", name, features);
    }
  }

  public static class HwModeSelect {
    public HwModeSelect(Record r, CodeGenHwModes cgh) {
      items = new ArrayList<>();
      ArrayList<Record> modes = r.getValueAsListOfDefs("Modes");
      ArrayList<Record> objects = r.getValueAsListOfDefs("Objects");
      if (modes.size() != objects.size()) {
        Error.printError(r.getLoc(),
            String.format("in record %s derived from HwModeSelect: "
                + "the lists Modes and Objects should have same "
                + "size", r.getName()));
        Error.printFatalError("error in target description");
      }
      for (int i = 0, e = modes.size(); i < e; i++) {
        Record m = modes.get(i);
        int modeId = cgh.getHwMode(m.getName());
        items.add(Pair.get(modeId, objects.get(i)));
      }
    }

    public ArrayList<Pair<Integer, Record>> items;

    public void dump() {
      System.err.print("{");
      for (Pair<Integer, Record> itr : items) {
        System.err.printf("(%d, %s)", itr.first, itr.second);
      }
      System.err.println("}");
    }
  }

  public final static int DefaultMode = 0;
  public final static String DefaultModeName = "DefaultMode";

  private RecordKeeper recordKeeper;
  private TObjectIntHashMap<String> nodeIds;
  private ArrayList<HwMode> modes;
  private HashMap<Record, HwModeSelect> modeSelects;

  public CodeGenHwModes(RecordKeeper keeper) {
    nodeIds = new TObjectIntHashMap<>();
    modes = new ArrayList<>();
    modeSelects = new HashMap<>();

    ArrayList<Record> ms = keeper.getAllDerivedDefinition("HwMode");
    // remove default HwMode.
    for (Iterator<Record> itr = ms.iterator(); itr.hasNext(); ) {
      Record m = itr.next();
      if (!m.getName().equals(DefaultModeName))
        continue;
      itr.remove();
    }

    for (Record r : ms) {
      modes.add(new HwMode(r));
      int newId = modes.size();
      nodeIds.put(r.getName(), newId);
    }
    ArrayList<Record> hwSelects = keeper.getAllDerivedDefinition("HwModeSelect");
    hwSelects.forEach(r -> modeSelects.put(r, new HwModeSelect(r, this)));
  }

  public int getHwMode(String name) {
    if (name.equals(DefaultModeName))
      return DefaultMode;
    Util.assertion(nodeIds.containsKey(name));
    return nodeIds.get(name);
  }

  public HwModeSelect getHwModeSelect(Record r) {
    Util.assertion(modeSelects.containsKey(r));
    return modeSelects.get(r);
  }

  public int getNumNodeIds() {
    return nodeIds.size() + 1;
  }

  public HwMode getMode(int idx) {
    Util.assertion(idx > 0, "0 index reserved for default mode");
    return modes.get(idx-1);
  }

  public void dump() {
    System.err.println("Modes: {");
    modes.forEach(m ->
    {
      System.err.print("  ");
      m.dump();
    });
    System.err.println("}");

    System.err.println("ModeIds: {");
    nodeIds.forEachEntry((str, id) ->
    {
      System.err.printf(" %s -> %d\n", str, id);
      return true;
    });
    modeSelects.forEach((r, hwMode) ->
    {
      System.err.print(" %s -> ");
      hwMode.dump();
    });
    System.err.println("}");
  }


  public static String getModeName(int mode)
  {
    if (mode == DefaultMode)
      return "*";
    return "m" + mode;
  }
}
