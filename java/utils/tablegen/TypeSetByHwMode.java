package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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
import gnu.trove.iterator.TIntObjectIterator;
import tools.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.function.Predicate;

import static utils.tablegen.CodeGenHwModes.DefaultMode;
import static utils.tablegen.CodeGenHwModes.getModeName;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class TypeSetByHwMode extends InfoByHwMode<MachineValueTypeSet> {

  public TypeSetByHwMode() {super();}
  public TypeSetByHwMode(int vt) {}
  public TypeSetByHwMode(ValueTypeByHwMode vt){}
  public TypeSetByHwMode(ArrayList<ValueTypeByHwMode> vts) {}

  public MachineValueTypeSet getOrCreate(int mode) {
    if (hasMode(mode))
      return get(mode);
    return map.put(mode, new MachineValueTypeSet());
  }

  public boolean isValueTypeByHwMode(boolean allowEmpty) {
    for (TIntObjectIterator<MachineValueTypeSet> itr = iterator(); itr.hasNext(); ) {
      if (itr.value().size() > 1)
        return false;
      if (!allowEmpty && itr.value().isEmpty())
        return false;
    }
    return true;
  }
  public ValueTypeByHwMode getValueTypeByHwMode() {
    ValueTypeByHwMode res = new ValueTypeByHwMode();
    map.forEachEntry((mode, info) -> {
      MVT vt = info.isEmpty() ? new MVT(MVT.Other) : info.iterator().next();
      res.getOrCreateTypeForMode(mode, vt);
      return true;
    });
    return res;
  }

  public boolean isMachineValueType() {
    return isDefaultOnly() && iterator().value().size() == 1;
  }

  public boolean isDefaultOnly() {
    return size() == 1 && iterator().key() == DefaultMode;
  }
  public MVT getMachineValueType() {
    Util.assertion(isMachineValueType());
    return map.iterator().value().iterator().next();
  }
  public boolean isPossible() {
    for (TIntObjectIterator<MachineValueTypeSet> itr = iterator(); itr.hasNext(); ) {
      if (!itr.value().isEmpty())
        return true;
    }
    return false;
  }
  public boolean insert(ValueTypeByHwMode vvt) {
    boolean changed = false;
    TreeSet<Integer> modes = new TreeSet<>();
    for (TIntObjectIterator<MVT> itr = vvt.iterator(); itr.hasNext(); ) {
      int mode = itr.key();
      modes.add(mode);
      changed |= getOrCreate(mode).insert(itr.value());
    }
    if (modes.contains(DefaultMode)) {
      MVT vt = vvt.getType(DefaultMode);
      for (TIntObjectIterator<MachineValueTypeSet> itr = iterator(); itr.hasNext(); ) {
        if (!modes.contains(itr.key()))
          changed |= itr.value().insert(vt);
      }
    }
    return changed;
  }

  /**
   * Constrain the type set to be the intersection with VTS.
   * @param vts
   * @return  Return true when this has changed.
   */
  public boolean constrain(TypeSetByHwMode vts) {
    boolean changed = false;
    if (hasDefault()) {
      for (TIntObjectIterator<MachineValueTypeSet> itr = vts.iterator(); itr.hasNext();){
        if (itr.key() == DefaultMode || hasMode(itr.key()))
          continue;
        map.put(itr.key(), map.get(DefaultMode));
        changed = true;
      }
    }

    for (TIntObjectIterator<MachineValueTypeSet> itr =iterator(); itr.hasNext();){
      int m = itr.key();
      MachineValueTypeSet set = itr.value();
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
   * Assigns all Pair from mode to MVT list to a MachineValueTypeSet object.
   * @param vts
   * @param pred
   * @return  Return true if the result map is not empty.
   */
  public boolean assignIf(TypeSetByHwMode vts, Predicate<MVT> pred) {
    for (TIntObjectIterator<MachineValueTypeSet> itr = map.iterator(); itr.hasNext(); ) {
      MachineValueTypeSet set = getOrCreate(itr.key());
      for (Iterator<MVT> vt = itr.value().iterator(); vt.hasNext(); ) {
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
    map.forEach(modes::add);
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
        return iterator().key() == set.iterator().key() &&
            iterator().value().equals(set.iterator().value());
      return false;
    }

    TreeSet<Integer> modes = new TreeSet<>();
    map.forEach(modes::add);
    set.map.forEach(modes::add);

    if (hasDefault) {
      for (int m : modes) {
        if (!get(m).equals(set.get(m)))
          return false;
      }
    }
    else {
      for (int m : modes) {
        boolean noModeThis = !hasMode(m) || get(m).isEmpty();
        boolean noModeVTs = !set.hasMode(m) || set.get(m).isEmpty();
        if (noModeThis != noModeVTs)
          return false;
        if (!noModeThis)
          if (!get(m).equals(set.get(m)))
            return false;
      }
    }
    return true;
  }

  public void dump() { System.err.println(toString());}

  /**
   * Intersects two sets. Returns true if anything has changed.
   * @param out
   * @param in
   * @return
   */
  private boolean intersect(MachineValueTypeSet out, MachineValueTypeSet in) {
    boolean outP = out.count(new MVT(MVT.iPTR)), inP = in.count(new MVT(MVT.iPTR));
    if (outP == inP)
      return out.eraseIf(vt-> !in.count(vt));

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
    }
    else {
      diff.eraseIf(out::count);
      out.erase(new MVT(MVT.iPTR));
    }

    boolean changed = out.eraseIf(vt->!in.count(vt));
    int numO = diff.size();
    if (numO == 0) return changed;

    if (numO == 1) {
      out.insert(diff.iterator().next());
      changed |= outP;
    }
    else {
      out.insert(new MVT(MVT.iPTR));
      changed |= outP;
    }
    return changed;
  }
}
