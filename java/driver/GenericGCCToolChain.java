/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2017, Jianping Zeng.
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

package driver;

import backend.support.Triple;
import driver.HostInfo.UnknownHostInfo;

import java.util.ArrayList;

public class GenericGCCToolChain extends ToolChain {
  public GenericGCCToolChain(UnknownHostInfo unknownHostInfo, Triple triple) {
    super(unknownHostInfo, triple);
  }

  @Override
  public driver.tool.Tool selectTool(Compilation c, Action.JobAction ja) {
    return null;
  }

  @Override
  public String getLinker() {
    return null;
  }

  @Override
  public String getAssembler() {
    return null;
  }

  @Override
  public String getForcedPicModel() {
    return null;
  }

  @Override
  public String getDefaultRelocationModel() {
    return null;
  }

  @Override
  public void addSystemIncludeDir(ArrayList<String> cmdStrings) {

  }
}
