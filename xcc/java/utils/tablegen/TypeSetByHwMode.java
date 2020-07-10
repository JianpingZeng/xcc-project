package utils.tablegen;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import backend.codegen.MVT;
import tools.Util;

import java.util.*;
import java.util.function.Predicate;

import static utils.tablegen.CodeGenHwModes.DefaultMode;
import static utils.tablegen.CodeGenHwModes.getModeName;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class TypeSetByHwMode extends InfoByHwMode<MachineValueTypeSet> implements Cloneable {

  public TypeSetByHwMode() {
    super();
  }

  public TypeSetByHwMode(int vt) {
    this(new ValueTypeByHwMode(new MVT(vt)));
  }

  public TypeSetByHwMode(ValueTypeByHwMode vt) {
    insert(vt);
  }

  public TypeSetByHwMode(ArrayList<ValueTypeByHwMode> vts) {
    vts.forEach(this::insert);
  }

  public MachineValueTypeSet getOrCreate(int mode) {
    if (hasMode(mode))
      return get(mode);
    else {
      map.put(mode, new MachineValueTypeSet());
      return map.get(mode);
    }
  }

  public boolean isValueTypeByHwMode(boolean allowEmpty) {
    for (Iterator<Map.Entry<Integer, MachineValueTypeSet>> itr = iterator();
         itr.hasNext(); ) {
      Map.Entry<Integer, MachineValueTypeSet> item = itr.next();
      if (item.getValue().size() > 1)
        return false;
      if (!allowEmpty && item.getValue().isEmpty())
        return false;
    }
    return true;
  }

  public ValueTypeByHwMode getValueTypeByHwMode() {
    ValueTypeByHwMode res = new ValueTypeByHwMode();
    map.forEach((mode, info) -> {
      MVT vt = info.isEmpty() ? new MVT(MVT.Other) : info.iterator().next();
      res.getOrCreateTypeForMode(mode, vt);
    });
    return res;
  }

  public boolean isMachineValueType() {
    return isDefaultOnly() && map.firstEntry().getValue().size() == 1;
  }

  public boolean isDefaultOnly() {
    return size() == 1 && map.firstKey() == DefaultMode;
  }

  public MVT getMachineValueType() {
    Util.assertion(isMachineValueType());
    return map.firstEntry().getValue().iterator().next();
  }

  public boolean isPossible() {
    for (Map.Entry<Integer, MachineValueTypeSet> itr : map.entrySet()) {
      if (!itr.getValue().isEmpty())
        return true;
    }
    return false;
  }

  public boolean insert(ValueTypeByHwMode vvt) {
    boolean changed = false;
    TreeSet<Integer> modes = new TreeSet<>();
    // A cache for type when default mode is selected.
    MVT dt = new MVT(MVT.Other);
    boolean containsDefaultMode = false;

    for (Map.Entry<Integer, MVT> itr : vvt.map.entrySet()) {
      int mode = itr.getKey();
      modes.add(mode);
      MachineValueTypeSet vts = getOrCreate(mode);
      Util.assertion(vts != null);
      changed |= getOrCreate(mode).insert(itr.getValue());
      if (mode == DefaultMode) {
        dt = itr.getValue();
        containsDefaultMode = true;
      }
    }

    if (containsDefaultMode) {
      for (Map.Entry<Integer, MachineValueTypeSet> itr : map.entrySet()) {
        if (!modes.contains(itr.getKey()))
          changed |= itr.getValue().insert(dt);
      }
    }
    return changed;
  }

  public boolean insert(TypeSetByHwMode set) {
    Util.assertion(isEmpty(), "Only use this method on empty");
    set.map.forEach((mode, vts) -> {
      map.put(mode, vts.clone());
    });
    return true;
  }

  /**
   * Constrain the type set to be the intersection with VTS.
   *
   * @param vts
   * @return Return true when this has changed.
   */
  public boolean constrain(TypeSetByHwMode vts) {
    boolean changed = false;
    if (hasDefault()) {
      for (Map.Entry<Integer, MachineValueTypeSet> itr : vts.map.entrySet()) {
        if (itr.getKey() == DefaultMode || hasMode(itr.getKey()))
          continue;
        map.put(itr.getKey(), map.get(DefaultMode).clone());
        changed = true;
      }
    }

    for (Map.Entry<Integer, MachineValueTypeSet> itr : map.entrySet()) {
      int m = itr.getKey();
      MachineValueTypeSet set = itr.getValue();
      if (vts.hasMode(m) || vts.hasDefault())
        changed |= intersect(set, vts.get(m));
      else if (!set.isEmpty()) {
        set.clear();
        changed = true;
      }
    }
    return changed;
  }

  /**
   * Removes any machine value type in this set if it is predicated on true by {@code pred}.
   *
   * @param pred
   * @return
   */
  public boolean constrain(Predicate<MVT> pred) {
    boolean changed = false;
    for (Map.Entry<Integer, MachineValueTypeSet> itr : map.entrySet()) {
      changed |= itr.getValue().eraseIf(mvt -> !pred.test(mvt));
    }
    return changed;
  }

  /**
   * Assigns all Pair from mode to MVT list to a MachineValueTypeSet object.
   *
   * @param vts
   * @param pred
   * @return Return true if the result map is not empty.
   */
  public boolean assignIf(TypeSetByHwMode vts, Predicate<MVT> pred) {
    for (Map.Entry<Integer, MachineValueTypeSet> itr : vts.map.entrySet()) {
      MachineValueTypeSet set = getOrCreate(itr.getKey());
      for (Iterator<MVT> vt = itr.getValue().iterator(); vt.hasNext(); ) {
        MVT v = vt.next();
        if (pred.test(v))
          set.insert(v);
      }
    }
    return !isEmpty();
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    ArrayList<Integer> modes = new ArrayList<>();
    modes.addAll(map.keySet());
    if (modes.isEmpty()) {
      buf.append("{}");
      return buf.toString();
    }

    Collections.sort(modes);
    buf.append('{');
    modes.forEach(m -> {
      buf.append(" ").append(getModeName(m)).
          append(":").append(get(m).toString());
    });
    buf.append("}");
    return buf.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (this == obj) return true;
    if (getClass() != obj.getClass()) return false;

    TypeSetByHwMode set = (TypeSetByHwMode) obj;
    boolean hasDefault = hasDefault();
    if (hasDefault != set.hasDefault())
      return false;

    if (isEmpty()) {
      if (set.isSimple())
        return this.map.firstKey() == set.map.firstKey() &&
            this.map.firstEntry().getValue().equals(set.map.firstEntry().getValue());
      return false;
    }

    TreeSet<Integer> modes = new TreeSet<>();
    modes.addAll(map.keySet());
    modes.addAll(set.map.keySet());

    for (int m : modes) {
      if (map.containsKey(m) != set.map.containsKey(m))
        return false;
      Util.assertion(map.containsKey(m));

      if (!get(m).equals(set.get(m)))
        return false;
    }
    return true;
  }

  public void dump() {
    System.err.println(toString());
  }

  /**
   * Intersects two sets. Returns true if anything has changed.
   *
   * @param out
   * @param in
   * @return
   */
  private boolean intersect(MachineValueTypeSet out, MachineValueTypeSet in) {
    boolean outP = out.count(new MVT(MVT.iPTR)), inP = in.count(new MVT(MVT.iPTR));
    Predicate<MVT> NotContainedInIn = vt -> !in.count(vt);
    if (outP == inP)
      return out.eraseIf(NotContainedInIn);

    // Compute the intersection of scalars separately to account for only
    // one set containing iPTR.
    // The itersection of iPTR with a set of integer scalar types that does not
    // include iPTR will result in the most specific scalar type:
    // - iPTR is more specific than any set with two elements or more
    // - iPTR is less specific than any single integer scalar type.
    // For example
    // { iPTR } * { i32 }     -> { i32 }
    // { iPTR } * { i32 i64 } -> { iPTR }
    // and
    // { iPTR i32 } * { i32 }          -> { i32 }
    // { iPTR i32 } * { i32 i64 }      -> { i32 i64 }
    // { iPTR i32 } * { i32 i64 i128 } -> { iPTR i32 }
    MachineValueTypeSet diff = new MachineValueTypeSet();
    if (inP) {
      diff = out.clone();
      diff.eraseIf(in::count);
      out.eraseIf(diff::count);
    } else {
      diff.insert(in);
      diff.eraseIf(out::count);
      out.erase(new MVT(MVT.iPTR));
    }

    boolean changed = out.eraseIf(NotContainedInIn);
    int numO = diff.size();
    if (numO == 0) return changed;

    if (numO == 1) {
      out.insert(diff.getFirstSetBit());
      changed |= outP;
    } else {
      out.insert(new MVT(MVT.iPTR));
      changed |= !outP;
    }
    return changed;
  }

  @Override
  public TypeSetByHwMode clone() {
    TypeSetByHwMode res = new TypeSetByHwMode();
    for (Map.Entry<Integer, MachineValueTypeSet> pair : map.entrySet()) {
      MachineValueTypeSet vts = res.getOrCreate(pair.getKey());
      vts.insert(pair.getValue().clone());
    }
    return res;
  }

  public MachineValueTypeSet get(int mode) {
    if (!hasMode(mode)) {
      Util.assertion(hasDefault());
      map.put(mode, map.get(DefaultMode).clone());
    }
    return map.get(mode);
  }
}
