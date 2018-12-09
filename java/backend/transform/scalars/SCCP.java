package backend.transform.scalars;
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

import backend.pass.AnalysisResolver;
import backend.pass.FunctionPass;
import backend.support.IntStatistic;
import backend.transform.scalars.SCCPSolver.LatticeStatus;
import backend.value.*;
import tools.Util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * This file defines a class implements the classical sparse conditional
 * constant propagation algorithm based on SSA form.
 * </br>
 * This algorithm uses the complete lattice as the mathematical modelling iterable
 * computing from the top element (Undefined value) into bottom element
 * (Overdefined value).
 * <p>
 * In order to make best effect of optimization, the DCE pass should be run after
 * this pass.
 * </p>
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class SCCP implements FunctionPass {
  public static final IntStatistic NumInstrToDels =
      new IntStatistic("NumInstrToDels", "Number instructions to be deleted");

  private AnalysisResolver resolver;

  @Override
  public void setAnalysisResolver(AnalysisResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public AnalysisResolver getAnalysisResolver() {
    return resolver;
  }

  @Override
  public String getPassName() {
    return "Sparse conditional constant propagation";
  }

  @Override
  public boolean runOnFunction(Function f) {
    if (f == null || f.empty())
      return false;

    SCCPSolver solver = new SCCPSolver();

    // Mark entry block of this function as executable and push it into
    // bbWorklist.
    solver.markBBExecutable(f.getEntryBlock());

    boolean madeChanged = false;
    f.getArgumentList().forEach(argument ->
    {
      solver.markOverdefined(argument);
    });

    solver.solve();

    HashMap<Value, LatticeStatus> latticeStatus = solver.getValue2LatticeMap();
    HashSet<BasicBlock> bbExecutable = solver.getExecutableBBs();
    LinkedList<Instruction> instToDelete = new LinkedList<>();

    for (BasicBlock bb : f.getBasicBlockList()) {
      if (!bbExecutable.contains(bb)) {
        // This parent is dead.
        instToDelete.addAll(bb.getInstList());

        // We just delete the instruction but not remove this BasicBlock
        // since must preserved CFG fo this function.
        while (!instToDelete.isEmpty()) {
          Instruction del = instToDelete.removeFirst();
          del.replaceAllUsesWith(Value.UndefValue.get(del.getType()));
          del.eraseFromParent();
          NumInstrToDels.inc();
        }
        madeChanged = true;
      } else {
        for (int i = 0; i != bb.size(); i++) {
          Instruction inst = bb.getInstAt(i);
          LatticeStatus ls = latticeStatus.get(inst);
          if (ls == null) continue;
          if (ls.isConstant()) {
            Constant constVal = ls.getConstVal();
            Util.assertion(constVal != null, "Constant lattice must have non-null constant value");
            inst.replaceAllUsesWith(constVal);
            NumInstrToDels.inc();
            inst.eraseFromParent();
            madeChanged = true;
            i--;
          }
        }
      }
    }
    return madeChanged;
  }

  public static FunctionPass createSparseConditionalConstantPropagatePass() {
    return new SCCP();
  }
}
