package backend.pass;
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

import backend.analysis.LoopInfo;
import backend.passManaging.*;
import backend.support.PrintLoopPass;
import backend.value.Loop;
import tools.Util;

import java.io.PrintStream;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public interface LoopPass extends Pass {
  @Override
  default void getAnalysisUsage(AnalysisUsage au) {
    au.addRequired(LoopInfo.class);
  }

  boolean runOnLoop(Loop loop, LPPassManager ppm);

  default boolean doInitialization(Loop loop, LPPassManager ppm) {
    return false;
  }

  default boolean doFinalization() {
    return false;
  }

  @Override
  default PassManagerType getPotentialPassManagerType() {
    return PassManagerType.PMT_LoopPassManager;
  }

  @Override
  default void assignPassManager(PMStack pms,
                                 PassManagerType pmt) {
    while (!pms.isEmpty()) {
      if (pms.peek().getPassManagerType().compareTo(PassManagerType.PMT_LoopPassManager) > 0) {
        pms.pop();
      } else
        break;
    }
    Util.assertion(!pms.isEmpty(), "Errorous status");
    LPPassManager lpm;
    if (!(pms.peek() instanceof FPPassManager)) {
      PMDataManager pmd = pms.peek();
      // Step#1 create new Function Pass Manager
      lpm = new LPPassManager(pmd.getDepth() + 1);
      lpm.populateInheritedAnalysis(pms);

      // Step#2 Assign manager to manage this new manager.
      lpm.assignPassManager(pms, pmd.getPassManagerType());
      // Step#3 Push new manager into stack.
      pms.push(lpm);
    }
    lpm = (LPPassManager) pms.peek();
    lpm.add(this);
  }

  @Override
  default void assignPassManager(PMStack pms) {
    assignPassManager(pms, PassManagerType.PMT_LoopPassManager);
  }

  @Override
  default Pass createPrinterPass(PrintStream os, String banner) {
    return PrintLoopPass.createPrintLoopPass(os, banner);
  }
}
