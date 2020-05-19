package backend.passManaging;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
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
import backend.analysis.CallGraphNode;
import backend.pass.*;
import backend.passManaging.FPPassManager;
import backend.passManaging.PMDataManager;
import backend.passManaging.PassManagerType;
import backend.value.Function;
import backend.value.Module;
import tools.Util;

import java.util.ArrayList;

import static backend.passManaging.PMDataManager.PassDebuggingString.*;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class CGPassManager extends PMDataManager implements ModulePass {
  private AnalysisResolver resolver;

  public CGPassManager(int depth) {
    super(depth);
  }

  @Override
  public Pass getAsPass() {
    return this;
  }

  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    au.addRequired(CallGraph.class);
  }

  public boolean doInitialization(CallGraph cg) {
    boolean changed = false;
    for (int i = 0, e = getNumContainedPasses(); i < e; i++) {
      Pass p = getContainedPass(i);
      if (p instanceof CallGraphSCCPass) {
        CallGraphSCCPass cgPass = (CallGraphSCCPass) p;
        changed |= cgPass.doInitialization(cg);
      } else {
        Util.assertion(p instanceof FPPassManager);
        FPPassManager fp = (FPPassManager) p;
        changed |= fp.doInitialization(cg.getModule());
      }
    }
    return changed;
  }

  @Override
  public boolean runOnModule(Module m) {
    CallGraph cg = (CallGraph) getAnalysisToUpDate(CallGraph.class);
    boolean changed = doInitialization(cg);

    // iterating over SCC.
    CallGraph.SCCIterator itr = cg.getSCCIterator();
    while (itr.hasNext()) {
      ArrayList<CallGraphNode> cgNodes = itr.next();
      // runs all passes over this CallGraphNode.
      for (int i = 0, e = getNumContainedPasses(); i < e; i++) {
        Pass p = getContainedPass(i);
        dumpPassInfo(p, EXECUTION_MSG, ON_CG_MSG, "");
        dumpRequiredSet(p);
        initializeAnalysisImpl(p);

        if (p instanceof CallGraphSCCPass) {
          changed |= ((CallGraphSCCPass) p).runOnSCC(cgNodes);
        } else {
          Util.assertion(p instanceof FPPassManager);
          FPPassManager fp = (FPPassManager) p;
          for (CallGraphNode node : cgNodes) {
            Function f = node.getFunction();
            if (f != null) {
              dumpPassInfo(p, EXECUTION_MSG, ON_FUNCTION_MSG, "");
              changed |= fp.runOnFunction(f);
            }
          }
        }

        if (changed)
          dumpPassInfo(p, MODIFICATION_MSG, ON_CG_MSG, "");
        dumpPreservedSet(p);
        verifyPreservedAnalysis(p);
        removeNotPreservedAnalysis(p);
        recordAvailableAnalysis(p);
        removeDeadPasses(p, "", ON_CG_MSG);
      }
    }
    changed |= doFinalization(cg);
    return changed;
  }

  public boolean doFinalization(CallGraph cg) {
    boolean changed = false;
    for (int i = 0, e = getNumContainedPasses(); i < e; i++) {
      Pass p = getContainedPass(i);
      if (p instanceof CallGraphSCCPass) {
        CallGraphSCCPass cgPass = (CallGraphSCCPass) p;
        changed |= cgPass.doFinalization(cg);
      } else {
        Util.assertion(p instanceof FPPassManager);
        FPPassManager fp = (FPPassManager) p;
        changed |= fp.doFinalization(cg.getModule());
      }
    }
    return changed;
  }

  @Override
  public String getPassName() {
    return "Call Graph Pass Manager Pass";
  }

  @Override
  public AnalysisResolver getAnalysisResolver() {
    return resolver;
  }

  @Override
  public void setAnalysisResolver(AnalysisResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public PassManagerType getPassManagerType() {
    return PassManagerType.PMT_CallGraphPassManager;
  }

  @Override
  public void dumpPassStructures(int offset) {
    System.err.printf("%s%s%n",
        Util.fixedLengthString(offset * 2, ' '),
        "Call Graph SCC Pass Manager");
    for (int i = 0, e = getNumContainedPasses(); i < e; i++) {
      Pass p = getContainedPass(i);
      p.dumpPassStructures(offset + 1);
      dumpLastUses(p, offset + 1);
    }
  }

  public Pass getContainedPass(int n) {
    Util.assertion(n >= 0 && n < getNumContainedPasses());
    ;
    return passVector.get(n);
  }
}
