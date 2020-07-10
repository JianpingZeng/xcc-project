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

package backend.analysis.aa;

/**
 * Summary of how a function affects memory in the program.
 * Loads from constant globals are not considered memory accesses for this
 * interface.  Also, functions may freely modify stack space local to their
 * invocation without having to report it through these interfaces.
 */
public enum ModRefBehavior {
  /**
   * This function does not perform any non-local loads
   * or stores to memory.
   * </p>
   * This property corresponds to the GCC 'const' attribute.
   */
  DoesNotAccessMemory,

  /**
   * This function accesses function arguments in well
   * known (possibly volatile) ways, but does not access any other memory.
   * <p>
   * Clients may use the Info parameter of getModRefBehavior to get specific
   * information about how pointer arguments are used.
   */
  AccessArguments,

  /**
   * This function has accesses function
   * arguments and global variables well known (possibly volatile) ways, but
   * does not access any other memory.
   * <p>
   * Clients may use the Info parameter of getModRefBehavior to get specific
   * information about how pointer arguments are used.
   */
  AccessArgumentsAndGlobals,

  /**
   * This function does not perform any non-local stores or
   * volatile loads, but may read from any memory location.
   * </p>
   * This property corresponds to the GCC 'pure' attribute.
   */
  OnlyReadsMemory,

  /**
   * This indicates that the function could not be
   * classified into one of the behaviors above.
   */
  UnknownModRefBehavior
}
