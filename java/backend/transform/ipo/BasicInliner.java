package backend.transform.ipo;
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

import backend.analysis.CallGraph;
import backend.pass.Pass;
import backend.support.Attribute;
import backend.support.CallSite;
import backend.value.*;
import backend.value.Module;
import tools.Util;

public class BasicInliner extends Inliner {
  public BasicInliner(int threshold) {
    super(threshold);
    analyzer = new InlineCostAnalyzer();
  }

  @Override
  public boolean doInitialization(CallGraph cg) {
    Module m = cg.getModule();
    for (Function f : m) {
      if (f != null && !f.isDeclaration() && f.hasFnAttr(Attribute.NoInline))
        neverInlined.add(f);
    }
    GlobalVariable gv = m.getGlobalVariable("llvm.noinline", true);
    if (gv == null || !gv.hasDefinitiveInitializer())
      return false;
    Constant c = gv.getInitializer();
    Util.assertion(c != null);
    if (!(c instanceof ConstantArray))
      return false;

    ConstantArray ca = (ConstantArray) c;
    for (int i = 0, e = ca.getNumOfOperands(); i < e; i++) {
      Constant opC = ca.operand(i);
      if (opC instanceof ConstantExpr &&
          ((ConstantExpr) opC).getOpcode() == Operator.BitCast)
        opC = ((ConstantExpr) opC).operand(0);
      if (opC instanceof Function) {
        neverInlined.add((Function) opC);
      }
    }
    return false;
  }

  @Override
  public InlineCost getInlineCost(CallSite cs) {
    return analyzer.getInlineCost(cs, neverInlined);
  }

  @Override
  public float getInlineFudgeFactor(CallSite cs) {
    return analyzer.getInlineFudgeFactor(cs);
  }

  @Override
  public String getPassName() {
    return "Basic Function Inlining/Integration";
  }

  public static Pass createFunctionInlinePass(int threshold) {
    return new BasicInliner(threshold);
  }
}
