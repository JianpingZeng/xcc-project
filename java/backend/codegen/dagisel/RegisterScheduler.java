/*
 * Extremely C language Compiler
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

package backend.codegen.dagisel;

import backend.codegen.MachinePassRegistry;
import backend.codegen.MachinePassRegistryListener;
import backend.codegen.MachinePassRegistryNode;
import backend.codegen.SchedPassCtor;

public class RegisterScheduler extends MachinePassRegistryNode<SchedPassCtor> {
  public static final MachinePassRegistry<SchedPassCtor> registry =
      new MachinePassRegistry<>();

  public RegisterScheduler(String name, String desc,
                           SchedPassCtor ctor) {
    super(name, desc, ctor);
    registry.add(this);
  }

  public void clear() {
    registry.remove(this);
  }

  @Override
  public RegisterScheduler getNext() {
    return (RegisterScheduler) super.getNext();
  }

  public static RegisterScheduler getList() {
    return (RegisterScheduler) registry.getList();
  }

  public static SchedPassCtor getDefault() {
    return registry.getDefault();
  }

  public static void setDefault(SchedPassCtor ctor) {
    registry.setDefaultCtor(ctor);
  }

  public static void setListener(MachinePassRegistryListener listener) {
    registry.setListener(listener);
  }

  @Override
  public SchedPassCtor getCtor() {
    return super.getCtor();
  }
}