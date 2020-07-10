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

import backend.pass.AnalysisResolver;
import backend.pass.AnalysisUsage;
import backend.pass.ModulePass;
import backend.pass.Pass;
import backend.support.PrintModulePass;
import backend.value.Module;
import tools.Util;

import java.io.PrintStream;
import java.util.HashMap;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class MPPassManager extends PMDataManager implements Pass {
  private HashMap<Pass, FunctionPassManagerImpl> onTheFlyManager;

  private AnalysisResolver resolver;

  @Override
  public void setAnalysisResolver(AnalysisResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public AnalysisResolver getAnalysisResolver() {
    return resolver;
  }

  public MPPassManager() {
    super(0);
    onTheFlyManager = new HashMap<>();
  }

  @Override
  public String getPassName() {
    return "Module Pass Manager";
  }

  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    au.setPreservedAll();
  }

  /**
   * Execute all of the passes scheduled for execution.  Keep track of
   * whether any of the passes modifies the module, and if so, return true.
   *
   * @param m
   * @return
   */
  public boolean runOnModule(Module m) {
    boolean changed = false;
    for (int index = 0; index < getNumContainedPasses(); ++index) {
      Pass p = getContainedPass(index);
      PassManagerPrettyStackEntry x = new PassManagerPrettyStackEntry(p, m);
      changed |= getContainedPass(index).runOnModule(m);
      x.unregister();
    }
    return changed;
  }

  @Override
  public PassManagerType getPassManagerType() {
    return PassManagerType.PMT_ModulePassManager;
  }

  public PMDataManager getAsPMDataManager() {
    return this;
  }

  @Override
  public Pass createPrinterPass(PrintStream os, String banner) {
    return PrintModulePass.createPrintModulePass(os);
  }

  public Pass getAsPass() {
    return this;
  }

  public ModulePass getContainedPass(int index) {
    Util.assertion(index >= 0 && index < getNumContainedPasses());
    return (ModulePass) passVector.get(index);
  }
}
