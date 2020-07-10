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

import tools.commandline.Option;
import tools.commandline.Parser;

/**
 * The command line option parser for registry of register allocator, like
 * RegAllocLocal, RegAllocaLinearScan etc.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class RegisterRegAllocParser extends Parser<MachinePassCtor>
    implements MachinePassRegistryListener<MachinePassCtor> {
  @Override
  public <T1> void initialize(Option<T1> opt) {
    super.initialize(opt);
    for (RegisterRegAlloc node = RegisterRegAlloc.getList(); node != null;
         node = node.getNext()) {
      addLiteralOption(node.getName(), node.getCtor(), node.getDescription());
    }
    RegisterRegAlloc.setListener(this);
  }

  @Override
  public void notifyAdd(String name, MachinePassCtor ctor, String desc) {
    addLiteralOption(name, ctor, desc);
  }

  @Override
  public void notifyRemove(String name) {
    removeLiteralOption(name);
  }
}
