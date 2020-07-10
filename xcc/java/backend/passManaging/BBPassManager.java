package backend.passManaging;
/*
 * Extremely Compiler Collection
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

import backend.pass.*;
import backend.value.BasicBlock;
import backend.value.Function;
import backend.value.Module;
import tools.Util;

import static backend.passManaging.PMDataManager.PassDebuggingString.*;

/**
 * BBPassManager manages BasicBlockPass, itself is a Function Pass. It batches
 * all the pass together and sequence them to process one basic block before
 * processing next basic block.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class BBPassManager extends PMDataManager implements FunctionPass {
  private AnalysisResolver resolver;

  @Override
  public void setAnalysisResolver(AnalysisResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public AnalysisResolver getAnalysisResolver() {
    return resolver;
  }

  public BBPassManager(int depth) {
    super(depth);
  }

  /**
   * Execute all of the passes scheduled for execution by invoking
   * runOnBasicBlock method.  Keep track of whether any of the passes modifies
   * the function, and if so, return true.
   *
   * @param f
   * @return
   */
  @Override
  public boolean runOnFunction(Function f) {
    if (f.isDeclaration())
      return false;

    boolean changed = false;

    // Collects inherited analysis from module level pass manager.
    populateInheritedAnalysis(topLevelManager.getActiveStack());

    for (BasicBlock bb : f.getBasicBlockList()) {
      for (int index = 0; index < getNumContainedPasses(); ++index) {
        BasicBlockPass bbp = getContainedPass(index);
        dumpPassInfo(bbp, EXECUTION_MSG, ON_FUNCTION_MSG, f.getName());
        dumpRequiredSet(bbp);

        initializeAnalysisImpl(bbp);
        {
          PassManagerPrettyStackEntry x = new PassManagerPrettyStackEntry(bbp, bb);
          changed |= bbp.runOnBasicBlock(bb);
          x.unregister();
        }

        if (changed) {
          dumpPassInfo(bbp, MODIFICATION_MSG, ON_FUNCTION_MSG, f.getName());
        }
        dumpPreservedSet(bbp);

        verifyPreservedAnalysis(bbp);
        removeNotPreservedAnalysis(bbp);
        recordAvailableAnalysis(bbp);
        removeDeadPasses(bbp, f.getName(), ON_FUNCTION_MSG);

        // if dominator information is available then verify it.
        verifyDomInfo(bbp, f);
      }
    }
    return changed;
  }

  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    au.setPreservedAll();
  }

  public boolean doInitialization(Module m) {
    boolean changed = false;
    for (int i = 0; i < getNumContainedPasses(); ++i)
      changed |= getContainedPass(i).doInitialization(m);
    return changed;
  }

  @Override
  public boolean doFinalization(Module m) {
    boolean changed = false;
    for (int i = 0; i < getNumContainedPasses(); ++i)
      changed |= getContainedPass(i).doFinalization(m);
    return changed;
  }

  public boolean doInitialization(Function f) {
    boolean changed = false;
    for (int i = 0; i < getNumContainedPasses(); ++i)
      changed |= getContainedPass(i).doInitialization(f);
    return changed;
  }

  public boolean doFinalization(Function f) {
    boolean changed = false;
    for (int i = 0; i < getNumContainedPasses(); ++i)
      changed |= getContainedPass(i).doFinalization(f);
    return changed;
  }

  @Override
  public String getPassName() {
    return "BasicBlock Pass Manager";
  }

  public void dumpPassStructure(int offset) {
    System.err.println(Util.fixedLengthString(offset << 1, ' ')
        + "BasicBlockPass Manager");
    for (int i = 0; i < getNumContainedPasses(); i++) {
      BasicBlockPass bp = getContainedPass(i);
      bp.dumpPassStructures(offset + 1);
      dumpLastUses(bp, offset + 1);
    }
  }

  public BasicBlockPass getContainedPass(int index) {
    Util.assertion(index >= 0 && index < getNumContainedPasses());
    return (BasicBlockPass) passVector.get(index);
  }

  @Override
  public PassManagerType getPassManagerType() {
    return PassManagerType.PMT_BasicBlockPassManager;
  }

  @Override
  public Pass getAsPass() {
    return this;
  }
}
