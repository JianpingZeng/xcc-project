package backend.passManaging;
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

import backend.pass.AnalysisUsage;
import backend.pass.ImmutablePass;
import backend.pass.Pass;
import backend.pass.PassInfo;

import java.util.ArrayList;

/**
 * PMTopLevelManager manages lastUser information and collects some usefully
 * common API used by top level pass manager.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public interface PMTopLevelManager {
  enum TopLevelPassManagerType {
    TLM_Function,
    /**
     * For pass
     */
    TLM_Pass,
  }

  /**
   * Schedule pass p for execution. Make sure that passes required by
   * p are run before p is run. Update analysis info maintained by
   * the manager. Remove dead passes. This is a recursive function.
   *
   * @param p
   */
  void schedulePass(Pass p);

  PassManagerType getTopLevelPassManagerType();

  void setLastUser(ArrayList<Pass> analysisPasses, Pass p);

  void collectLastUses(ArrayList<Pass> lastUsers, Pass p);

  Pass findAnalysisPass(PassInfo pi);

  AnalysisUsage findAnalysisUsage(Pass p);

  void addImmutablePass(ImmutablePass p);

  ArrayList<ImmutablePass> getImmutablePasses();

  void addPassManager(PMDataManager pm);

  void dumpPasses();

  void dumpArguments();

  void initializeAllAnalysisInfo();

  PMStack getActiveStack();

  int getNumContainedManagers();
}
