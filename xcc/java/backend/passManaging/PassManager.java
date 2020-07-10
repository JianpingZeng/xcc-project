package backend.passManaging;
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

import backend.pass.Pass;
import backend.value.Module;

/**
 * This class just a thin wrapper of class {@linkplain backend.passManaging.PassManagerImpl}.
 * This provides simple interface methods with client.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class PassManager implements PassManagerBase {
  private PassManagerImpl pm;

  public PassManager() {
    pm = new PassManagerImpl(0);
    // PM is the top level manager of itself.
    pm.setTopLevelManager(pm);
  }

  /**
   * Add a pass to the queue of passes to be run.
   *
   * @param p
   */
  @Override
  public void add(Pass p) {
    pm.add(p);
  }

  /**
   * Execute all of the passes scheduled for execution. Keep track of whether
   * any of passes modifies the module, and if so, return true.
   *
   * @param m
   * @return
   */
  public boolean run(Module m) {
    return pm.run(m);
  }
}
