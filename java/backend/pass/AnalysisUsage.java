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

import tools.Util;

import java.util.HashSet;
import java.util.LinkedHashSet;

import static backend.pass.PassRegistrar.getPassInfo;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public final class AnalysisUsage {
  private LinkedHashSet<PassInfo> required, preserved;
  private boolean preservedAll;

  public AnalysisUsage() {
    required = new LinkedHashSet<>();
    preserved = new LinkedHashSet<>();
  }

  public AnalysisUsage addRequired(Class<? extends Pass> reqPass) {
    PassInfo info = getPassInfo(reqPass);
    Util.assertion(info != null, "Pass is not registered!");
    return addRequiredPassInfo(info);
  }

  public AnalysisUsage addRequiredPassInfo(PassInfo info) {
    if (!required.contains(info))
      required.add(info);
    return this;
  }

  public AnalysisUsage addPreserved(Class<? extends Pass> prePass) {
    PassInfo info = getPassInfo(prePass);
    Util.assertion(info != null, "Pass is not registered");
    return addPreservedPassInfo(info);
  }

  public AnalysisUsage addPreservedPassInfo(PassInfo info) {
    if (!preserved.contains(info))
      preserved.add(info);
    return this;
  }

  public void setPreservedAll() {
    preservedAll = true;
  }

  public boolean getPreservedAll() {
    return preservedAll;
  }

  public HashSet<PassInfo> getRequired() {
    return required;
  }

  public HashSet<PassInfo> getPreserved() {
    return preserved;
  }

  /**
   * This function should be called to by the pass, iff they do
   * not:
   * <ol>
   * <li>Add or remove basic blocks from the function</li>
   * <li>Modify terminator instructions in any way.</li>
   * </ol>
   * This function annotates the AnalysisUsage info object to say that analyses
   * that only depend on the CFG are preserved by this pass.
   */
  public void setPreservesCFG() {
    // TODO: 17-11-16
  }
}
