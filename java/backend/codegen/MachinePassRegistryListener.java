/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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
 * A listener for client registering a MachineFunction pass.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public interface MachinePassRegistryListener<T> {
  /**
   * Notify the registered listener when a new machine function pass has been
   * added into registry manager.
   *
   * @param name The name of registered MachineFunction pass used to print out stdout
   * @param ctor The constructor interface to create a machine function pass.
   */
  void notifyAdd(String name, T ctor, String description);

  /**
   * Notify registered listeners when a machine pass with specified name was removed
   * for registry manager.
   *
   * @param name The name of machine pass to be removed.
   */
  void notifyRemove(String name);
}
