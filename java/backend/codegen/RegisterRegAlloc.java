/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

/**
 * Track the registration of register allocations.
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public class RegisterRegAlloc extends MachinePassRegistryNode<MachinePassCtor> {
  public static final MachinePassRegistry<MachinePassCtor> registry =
      new MachinePassRegistry<>();

  public RegisterRegAlloc(String name, String desc, MachinePassCtor ctor) {
    super(name, desc, ctor);
    registry.add(this);
  }

  public void clear() {
    registry.remove(this);
  }

  @Override
  public RegisterRegAlloc getNext() {
    return (RegisterRegAlloc) super.getNext();
  }

  public static RegisterRegAlloc getList() {
    return (RegisterRegAlloc) registry.getList();
  }

  public static MachinePassCtor getDefault() {
    return registry.getDefault();
  }

  public static void setDefault(MachinePassCtor ctor) {
    registry.setDefaultCtor(ctor);
  }

  public static void setListener(MachinePassRegistryListener listener) {
    registry.setListener(listener);
  }

  @Override
  public MachinePassCtor getCtor() {
    return super.getCtor();
  }
}
