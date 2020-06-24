package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the namespace of the <organization> nor the
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
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;
import tools.Util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static tools.Util.anyOf;
import static tools.Util.noneOf;
import static utils.tablegen.CodeGenHwModes.DefaultMode;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class TypeInfer {
  private TreePattern tp;
  public boolean forceMode;
  /**
   * Set during code generation phase.
   */
  public boolean codeGen;

  public boolean isLegalTypeCached;
  private TypeSetByHwMode legalCache;

  public TypeInfer(TreePattern tp) {
    this.tp = tp;
    forceMode = false;
    codeGen = false;
    isLegalTypeCached = false;
    legalCache = new TypeSetByHwMode();
  }

  public boolean isConcrete(TypeSetByHwMode vts,
                            boolean allowEmpty) {
    return vts.isValueTypeByHwMode(allowEmpty);
  }

  public ValueTypeByHwMode getConcrete(TypeSetByHwMode vts,
                                       boolean allowEmpty) {
    Util.assertion(vts.isValueTypeByHwMode(allowEmpty));
    return vts.getValueTypeByHwMode();
  }

  public boolean mergeInTypeInfo(TypeSetByHwMode out, TypeSetByHwMode in) {
    if (in.isEmpty() || out.equals(in) || tp.hasError())
      return false;
    if (out.isEmpty())
      return out.insert(in);

    boolean changed = out.constrain(in);
    if (changed && out.isEmpty())
      tp.error("Type inference contradiction");
    return changed;
  }

  public boolean mergeInTypeInfo(TypeSetByHwMode out, int simpleVT) {
    return mergeInTypeInfo(out, new TypeSetByHwMode(simpleVT));
  }

  public boolean mergeInTypeInfo(TypeSetByHwMode out, ValueTypeByHwMode in) {
    return mergeInTypeInfo(out, new TypeSetByHwMode(in));
  }

  /**
   * To reduce the set {@code out} has at most one VT for each mode.
   *
   * @param out
   * @return Return true if the {@code out} has been changed.
   */
  public boolean forceArbitrary(TypeSetByHwMode out) {
    if (tp.hasError()) return false;
    boolean changed = false;
    for (Map.Entry<Integer, MachineValueTypeSet> itr : out.map.entrySet()) {
      if (itr.getValue().size() <= 1)
        continue;
      MVT vt = itr.getValue().getFirstSetBit();
      Util.assertion(vt != null);
      itr.getValue().clear();
      itr.getValue().insert(vt);
      changed = true;
    }
    return changed;
  }

  public static final Predicate<MVT> IsIntegerOrPtr = vt ->
      vt.isInteger() || vt.simpleVT == MVT.iPTR;

  public static final Predicate<MVT> IsFloatingPoint = MVT::isFloatingPoint;
  public static final Predicate<MVT> IsVector = MVT::isVector;
  public static final Predicate<MVT> IsScalar = vt -> !vt.isVector();

  private boolean enforceByPredicate(TypeSetByHwMode out, Predicate<MVT> pred) {
    if (tp.hasError())
      return false;

    if (!out.isEmpty())
      return out.constrain(pred);
    return out.assignIf(getLegalType(), pred);
  }

  /**
   * Remove any non-integer type in this set.
   *
   * @param out
   * @return
   */
  public boolean enforceInteger(TypeSetByHwMode out) {
    return enforceByPredicate(out, IsIntegerOrPtr);
  }

  public boolean enforceFloatingPoint(TypeSetByHwMode out) {
    return enforceByPredicate(out, IsFloatingPoint);
  }

  public boolean enforceScalar(TypeSetByHwMode out) {
    return enforceByPredicate(out, IsScalar);
  }

  public boolean enforceVector(TypeSetByHwMode out) {
    return enforceByPredicate(out, IsVector);
  }

  public boolean enforceAny(TypeSetByHwMode out) {
    if (tp.hasError() || !out.isEmpty())
      return false;
    return out.insert(getLegalType());
  }

  private <T> TIntArrayList unionMode(InfoByHwMode<T> setA,
                                      InfoByHwMode<T> setB) {
    TIntArrayList res = new TIntArrayList();
    TIntHashSet unique = new TIntHashSet();
    unique.addAll(setA.map.keySet());
    unique.addAll(setB.map.keySet());

    boolean hasDefault = unique.contains(DefaultMode);
    unique.forEach(m -> {
      if (m != DefaultMode)
        res.add(m);
      return true;
    });
    if (hasDefault)
      res.add(DefaultMode);
    return res;
  }

  /**
   * Make sure that for each type in {@code small} set, there exists a larger type in
   * {@code big} set.
   *
   * @param small
   * @param big
   * @return Return true if given set has changed.
   */
  public boolean enforceSmallerThan(TypeSetByHwMode small, TypeSetByHwMode big) {
    if (tp.hasError()) return false;
    boolean changed = false;

    if (small.isEmpty())
      changed |= enforceAny(small);
    if (big.isEmpty())
      changed |= enforceAny(big);

    Util.assertion(small.hasDefault() && big.hasDefault());

    TIntArrayList modes = unionMode(small, big);
    for (int i = 0, e = modes.size(); i < e; i++) {
      int m = modes.get(i);
      MachineValueTypeSet s = small.get(m);
      MachineValueTypeSet b = big.get(m);

      if (anyOf(s.iterator(), IsIntegerOrPtr) &&
          anyOf(s.iterator(), IsIntegerOrPtr)) {
        Predicate<MVT> NotInt = vt -> !IsIntegerOrPtr.test(vt);
        changed |= s.eraseIf(NotInt) | b.eraseIf(NotInt);
      } else if (anyOf(s.iterator(), IsFloatingPoint) &&
          anyOf(b.iterator(), IsFloatingPoint)) {
        Predicate<MVT> NotFP = vt -> !IsFloatingPoint.test(vt);
        changed |= s.eraseIf(NotFP) | b.eraseIf(NotFP);
      } else if (s.isEmpty() || b.isEmpty()) {
        changed = !s.isEmpty() || !b.isEmpty();
        s.clear();
        b.clear();
      } else {
        tp.error("Incompatible types");
        return changed;
      }

      if (noneOf(s.iterator(), IsVector) ||
          noneOf(b.iterator(), IsVector)) {
        changed |= s.eraseIf(IsVector) | b.eraseIf(IsVector);
      }
    }

    BiPredicate<MVT, MVT> lt = (a, b) -> {
      return a.getScalarSizeInBits() < b.getScalarSizeInBits() ||
          (a.getScalarSizeInBits() == b.getScalarSizeInBits() &&
              a.getSizeInBits() < b.getSizeInBits());
    };

    BiPredicate<MVT, MVT> le = (a, b) -> {
      // This function is used when removing elements: when a vector is compared
      // to a non-vector, it should return false (to avoid removal).
      if (a.isVector() != b.isVector())
        return false;

      boolean res = a.getScalarSizeInBits() <= b.getScalarSizeInBits() ||
          a.getSizeInBits() < b.getSizeInBits();
      return res;
    };

    for (int i = 0, e = modes.size(); i < e; i++) {
      int m = modes.get(i);
      MachineValueTypeSet s = small.get(m);
      MachineValueTypeSet b = big.get(m);

      // Remove any element less or equal than minS in set b.
      MVT minS = Util.minIf(s.iterator(), IsScalar, lt);
      if (minS != null)
        changed |= b.eraseIf((vt) -> {
          return le.test(vt, minS);
        });

      // Remove any element great or equal than maxS in set s.
      MVT maxS = Util.maxIf(b.iterator(), IsScalar, lt);
      if (maxS != null)
        changed |= s.eraseIf((vt) -> {
          return le.test(maxS, vt);
        });

      // MinV = min vector in Small, remove all vectors from Big that are
      // smaller-or-equal than MinV.
      MVT minV = Util.minIf(s.iterator(), IsVector, lt);
      if (minV != null)
        changed |= b.eraseIf(vt -> {
          return le.test(vt, minV);
        });

      MVT maxV = Util.maxIf(b.iterator(), IsVector, lt);
      if (maxV != null)
        changed |= s.eraseIf(vt -> {
          return le.test(maxV, vt);
        });
    }
    return changed;
  }

  /**
   * 1. Ensure that for each type T in vec, T is a vector type, and that
   * for each type U in elt, U is a scalar type.
   * 2. Ensure that for each scalar type U in elt, there exits a vector
   * type T in vec, such that U is the element type of T.
   *
   * @param vec
   * @param elt
   * @return
   */
  public boolean enforceVectorEltTypeIs(TypeSetByHwMode vec,
                                        TypeSetByHwMode elt) {
    if (tp.hasError())
      return false;
    boolean changed = false;
    if (vec.isEmpty())
      changed = enforceVector(vec);
    if (elt.isEmpty())
      changed |= enforceScalar(elt);

    TIntArrayList list = unionMode(vec, elt);
    for (int i = 0, sz = list.size(); i < sz; i++) {
      int m = list.get(i);
      MachineValueTypeSet vs = vec.get(m), es = elt.get(m);
      changed |= vs.eraseIf(IsScalar);
      changed |= es.eraseIf(IsVector);
      Util.assertion(!vs.isEmpty() && !es.isEmpty());

      HashSet<MVT> an = new HashSet<>(), bn = new HashSet<>();
      vs.forEach(vt -> an.add(vt.getVectorElementType()));
      es.forEach(vt -> bn.add(vt));

      changed |= vs.eraseIf(vt -> !bn.contains(vt.getVectorElementType()));
      changed |= es.eraseIf(vt -> !an.contains(vt));
    }
    return changed;
  }

  public boolean enforceVectorEltTypeIs(TypeSetByHwMode vec,
                                        ValueTypeByHwMode elt) {
    TypeSetByHwMode set = new TypeSetByHwMode(elt);
    return enforceVectorEltTypeIs(vec, set);
  }

  public boolean enforceVectorSubVectorTypesIs(TypeSetByHwMode vec,
                                               TypeSetByHwMode sub) {
    if (tp.hasError())
      return false;

    BiPredicate<MVT, MVT> IsSubVec = (b, p) -> {
      if (!b.isVector() || !p.isVector())
        return false;

      if (b.isScalableVector() != p.isScalableVector())
        return false;
      if (!b.getVectorElementType().equals(p.getVectorElementType()))
        return false;
      return b.getVectorNumElements() < p.getVectorNumElements();
    };

    BiPredicate<MachineValueTypeSet, MVT> NoSubV = (s, t) -> {
      for (Iterator<MVT> itr = s.iterator(); itr.hasNext(); )
        if (IsSubVec.test(t, itr.next()))
          return false;
      return true;
    };

    BiPredicate<MachineValueTypeSet, MVT> NoSupV = (s, t) -> {
      for (Iterator<MVT> itr = s.iterator(); itr.hasNext(); )
        if (IsSubVec.test(itr.next(), t))
          return false;
      return true;
    };

    boolean changed = false;
    if (vec.isEmpty())
      changed |= enforceVector(vec);
    if (sub.isEmpty())
      changed |= enforceVector(sub);

    TIntArrayList list = unionMode(vec, sub);
    for (int i = 0, e = list.size(); i < e; i++) {
      int m = list.get(i);
      MachineValueTypeSet vs = vec.get(m), ws = sub.get(m);

      changed |= vs.eraseIf(IsScalar);
      changed |= vs.eraseIf(vt -> NoSubV.test(vs, vt));
      changed |= ws.eraseIf(vt -> NoSupV.test(ws, vt));
    }
    return changed;
  }

  public boolean enforceSameNumElts(TypeSetByHwMode v,
                                    TypeSetByHwMode w) {
    if (tp.hasError())
      return false;
    boolean changed = false;
    if (v.isEmpty())
      changed |= enforceAny(v);
    if (w.isEmpty())
      changed |= enforceAny(w);

    BiPredicate<TIntHashSet, MVT> NoLength = (lengths, vt) ->
        !lengths.contains(vt.isVector() ? vt.getVectorNumElements() : 0);

    TIntArrayList list = unionMode(v, w);
    for (int i = 0, e = list.size(); i < e; i++) {
      int m = list.get(i);
      MachineValueTypeSet vs = v.get(m), ws = w.get(m);
      TIntHashSet an = new TIntHashSet(), bn = new TIntHashSet();

      vs.forEach(vt -> an.add(vt.isVector() ? vt.getVectorNumElements() : 0));
      ws.forEach(vt -> an.add(vt.isVector() ? vt.getVectorNumElements() : 0));

      changed |= vs.eraseIf(vt -> NoLength.test(bn, vt));
      changed |= ws.eraseIf(vt -> NoLength.test(an, vt));
    }
    return changed;
  }

  public boolean enforceSameSize(TypeSetByHwMode a, TypeSetByHwMode b) {
    if (tp.hasError())
      return false;
    boolean changed = false;
    if (a.isEmpty())
      changed |= enforceAny(a);
    if (b.isEmpty())
      changed |= enforceAny(b);

    BiPredicate<TIntHashSet, MVT> NoSize = (size, vt) -> {
      return !size.contains(vt.getSizeInBits());
    };
    TIntArrayList list = unionMode(a, b);
    for (int i = 0, e = list.size(); i < e; i++) {
      int m = list.get(i);
      MachineValueTypeSet as = a.get(m), bs = b.get(m);
      TIntHashSet an = new TIntHashSet(), bn = new TIntHashSet();

      as.forEach(vt -> an.add(vt.getSizeInBits()));
      bs.forEach(vt -> an.add(vt.getSizeInBits()));

      changed |= as.eraseIf(vt -> NoSize.test(bn, vt));
      changed |= bs.eraseIf(vt -> NoSize.test(an, vt));
    }
    return changed;
  }

  public void expandOverloads(TypeSetByHwMode vts) {
    TypeSetByHwMode legalSet = getLegalType();
    boolean hasDefaultDef = legalSet.hasDefault();
    Util.assertion(legalSet.isDefaultOnly());
    MachineValueTypeSet legalTypes = legalSet.get(DefaultMode);

    for (Map.Entry<Integer, MachineValueTypeSet> itr : vts.map.entrySet()) {
      MachineValueTypeSet vt = itr.getValue();
      expandOverloads(vt, legalTypes);
    }
  }

  public void expandOverloads(MachineValueTypeSet out,
                              MachineValueTypeSet legal) {
    HashSet<MVT> ovs = new HashSet<>();
    for (Iterator<MVT> itr = out.iterator(); itr.hasNext(); ) {
      MVT vt = itr.next();
      if (!vt.isOverloaded())
        continue;

      ovs.add(vt);
      out.erase(vt);
    }

    for (MVT ov : ovs) {
      switch (ov.simpleVT) {
        case MVT.iPTRAny:
          out.insert(new MVT(MVT.iPTR));
        case MVT.iAny:
          for (int i = MVT.FIRST_INTEGER_VALUETYPE;
               i < MVT.LAST_INTEGER_VALUETYPE; i++) {
            if (legal.count(new MVT(i)))
              out.insert(new MVT(i));
          }
          for (int i = MVT.FIRST_INTEGER_VECTOR_VALUETYPE;
               i < MVT.LAST_INTEGER_VECTOR_VALUETYPE; i++) {
            if (legal.count(new MVT(i)))
              out.insert(new MVT(i));
          }
          return;
        case MVT.fAny:
          for (int i = MVT.FIRST_FP_VALUETYPE;
               i < MVT.LAST_FP_VALUETYPE; i++) {
            if (legal.count(new MVT(i)))
              out.insert(new MVT(i));
          }
          for (int i = MVT.FIRST_FP_VECTOR_VALUETYPE;
               i < MVT.LAST_FP_VECTOR_VALUETYPE; i++) {
            if (legal.count(new MVT(i)))
              out.insert(new MVT(i));
          }
          return;
        case MVT.vAny:
          for (int i = MVT.FIRST_VECTOR_VALUETYPE;
               i < MVT.LAST_VECTOR_VALUETYPE; i++) {
            if (legal.count(new MVT(i)))
              out.insert(new MVT(i));
          }
          return;
        default:
          break;
      }
    }
  }

  private TypeSetByHwMode getLegalType() {
    if (!isLegalTypeCached) {
      TypeSetByHwMode set = tp.getDAGPatterns().getLegalValueTypes();
      MachineValueTypeSet vts = legalCache.getOrCreate(DefaultMode);

      for (Map.Entry<Integer, MachineValueTypeSet> pair : set.map.entrySet()) {
        vts.insert(pair.getValue());
      }
      isLegalTypeCached = true;
    }
    Util.assertion(legalCache.isDefaultOnly(), "Default-only allowed!");
    return legalCache;
  }
}
