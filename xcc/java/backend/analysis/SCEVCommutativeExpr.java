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
import backend.value.Loop;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public abstract class SCEVCommutativeExpr extends SCEV {
  /**
   * A cache for keeping only SCEVMulExpr instance of a pair of two SCEV value.
   */
  protected static final HashMap<Pair<SCEVType, ArrayList<SCEV>>, SCEVCommutativeExpr>
      scevCommExprsMap = new HashMap<>();

  protected ArrayList<SCEV> operands;

  protected SCEVCommutativeExpr(SCEVType scevType, ArrayList<SCEV> ops) {
    super(scevType);
    operands = new ArrayList<>(ops.size());
    operands.addAll(ops);
  }

  public final int getNumOperands() {
    return operands.size();
  }

  public final SCEV getOperand(int index) {
    Util.assertion(index >= 0 && index < operands.size());
    return operands.get(index);
  }

  public final ArrayList<SCEV> getOperands() {
    return operands;
  }

  @Override
  public final boolean isLoopInvariant(Loop loop) {
    for (SCEV val : operands)
      if (!val.isLoopInvariant(loop)) return false;

    return true;
  }

  @Override
  public final boolean hasComputableLoopEvolution(Loop loop) {
    boolean hasVaring = false;
    for (SCEV val : operands)
      if (!val.isLoopInvariant(loop)) {
        if (val.hasComputableLoopEvolution(loop))
          hasVaring = true;
        else
          return false;
      }
    return hasVaring;
  }

  @Override
  public final SCEV replaceSymbolicValuesWithConcrete(SCEV sym, SCEV concrete) {
    for (int i = 0, e = operands.size(); i < e; i++) {
      SCEV midd = operands.get(i).replaceSymbolicValuesWithConcrete(sym, concrete);
      if (!midd.equals(operands.get(i))) {
        ArrayList<SCEV> res = new ArrayList<>(getNumOperands());
        for (int j = 0; j < i; j++)
          res.add(getOperand(j));

        res.add(midd);
        for (int j = i + 1; j < e; j++)
          res.add(operands.get(j));

        if (this instanceof SCEVAddExpr)
          return SCEVAddExpr.get(res);
        if (this instanceof SCEVMulExpr)
          return SCEVMulExpr.get(res);

        Util.assertion(false, "Invalid comutative opr!");
      }
    }
    return this;
  }

  public abstract String getOperationString();

  public Type getType() {
    return getOperand(0).getType();
  }

  /**
   * Return true if the complexity of the lhs is less than the complexity of
   * rhs. This comparator is used for canonical expression.
   */
  public static Comparator<SCEV> SCEVComplexityCompare = new Comparator<SCEV>() {
    @Override
    public int compare(SCEV o1, SCEV o2) {
      return o1.getSCEVType().ordinal() - o2.getSCEVType().ordinal();
    }
  };

  public static void groupByComplexity(ArrayList<SCEV> ops) {
    if (ops.size() < 2) return;

    if (ops.size() == 2) {
      if (ops.get(0).getSCEVType().ordinal() > ops.get(1).getSCEVType().ordinal()) {
        SCEV temp = ops.get(0);
        ops.set(0, ops.get(1));
        ops.set(1, temp);
      }
      return;
    }
    // do the rough sort by complexity.
    ops.sort(SCEVComplexityCompare);

    // Now that we are sorted by complexity, group elements of the same
    // complexity.  Note that this is, at worst, N^2, but the vector is likely to
    // be extremely short in practice.  Note that we take this approach because we
    // do not want to depend on the addresses of the objects we are grouping.
    for (int i = 0, e = ops.size(); i < e - 2; i++) {
      SCEV val = ops.get(i);
      int complexity = val.getSCEVType().ordinal();

      for (int j = i + 1; j < e && ops.get(j).getSCEVType().ordinal() == complexity; j++) {
        if (ops.get(j).equals(val)) {
          // Move the j'th element to immediately after i'th element.
          SCEV temp = ops.get(i + 1);
          ops.set(i + 1, ops.get(j));
          ops.set(j, temp);
          // avoids rescan.
          i++;
          if (i == e - 2) return;
        }
      }
    }
  }
}
