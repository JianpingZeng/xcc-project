/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.codegen;

import backend.codegen.dagisel.RegisterScheduler;
import tools.commandline.Option;
import tools.commandline.Parser;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class RegisterSchedulerParser extends Parser<SchedPassCtor>
    implements MachinePassRegistryListener<SchedPassCtor> {
  @Override
  public <T1> void initialize(Option<T1> opt) {
    super.initialize(opt);
    for (RegisterScheduler node = RegisterScheduler.getList(); node != null;
         node = node.getNext()) {
      addLiteralOption(node.getName(), node.getCtor(), node.getDescription());
    }
    RegisterScheduler.setListener(this);
  }

  @Override
  public void notifyAdd(String name, SchedPassCtor ctor, String desc) {
    addLiteralOption(name, ctor, desc);
  }

  @Override
  public void notifyRemove(String name) {
    removeLiteralOption(name);
  }
}
