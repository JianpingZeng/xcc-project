package backend.analysis;
/*
 * Extremely Compiler Collection
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

import backend.type.Type;
import backend.value.*;
import tools.Pair;

import java.io.PrintStream;
import java.util.HashMap;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public final class SCEVSDivExpr extends SCEV {
  private static final HashMap<Pair<SCEV, SCEV>, SCEVSDivExpr>
      scevSDivMap = new HashMap<>();

  private SCEV lhs, rhs;

  private SCEVSDivExpr(SCEV lhsVal, SCEV rhsVal) {
    super(SCEVType.scSDivExpr);
    lhs = lhsVal;
    rhs = rhsVal;
  }

  public SCEV getLHS() {
    return lhs;
  }

  public SCEV getRHS() {
    return rhs;
  }

  /**
   * Constructs a chains of recurrence for division expression.
   *
   * @param lhs
   * @param rhs
   * @return
   */
  public static SCEV get(SCEV lhs, SCEV rhs) {
    if (rhs instanceof SCEVConstant) {
      SCEVConstant cons = (SCEVConstant) rhs;
      if (cons.getValue().equalsInt(1))
        return lhs;     // lhs/1 = lhs.
      if (cons.getValue().isAllOnesValue())
        return ScalarEvolution.getNegativeSCEV(lhs);

      if (lhs instanceof SCEVConstant) {
        SCEVConstant lhsCons = (SCEVConstant) lhs;
        ConstantInt lhsV = lhsCons.getValue();
        ConstantInt rhsV = cons.getValue();
        Constant expr;
        if (!lhsV.getType().isSigned() && !rhsV.getType().isSigned())
          expr = ConstantExpr.getUDiv(lhsV, rhsV);
        else
          expr = ConstantExpr.getSDiv(lhsV, rhsV);
        return SCEVUnknown.get(expr);
      }
    }
    Pair<SCEV, SCEV> key = Pair.get(lhs, rhs);
    if (!scevSDivMap.containsKey(key)) {
      SCEVSDivExpr res = new SCEVSDivExpr(lhs, rhs);
      scevSDivMap.put(key, res);
      return res;
    }
    return scevSDivMap.get(key);
  }

  @Override
  public boolean isLoopInvariant(Loop loop) {
    return lhs.isLoopInvariant(loop) && rhs.isLoopInvariant(loop);
  }

  @Override
  public boolean hasComputableLoopEvolution(Loop loop) {
    return lhs.hasComputableLoopEvolution(loop)
        && rhs.hasComputableLoopEvolution(loop);
  }

  @Override
  public SCEV replaceSymbolicValuesWithConcrete(SCEV sym, SCEV concrete) {
    SCEV l = lhs.replaceSymbolicValuesWithConcrete(sym, concrete);
    SCEV r = rhs.replaceSymbolicValuesWithConcrete(sym, concrete);
    if (l.equals(rhs) && r.equals(rhs))
      return this;
    else
      return get(l, r);
  }

  @Override
  public Type getType() {
    return rhs.getType();
  }

  @Override
  public boolean dominates(BasicBlock bb, DomTree dt) {
    return false;
  }

  @Override
  public void print(PrintStream os) {

  }
}
