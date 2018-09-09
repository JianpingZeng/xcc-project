package tools.commandline;
/*
 * Extremely C language Compiler.
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

/**
 * Miscellaneous flags to adjust argument.
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public enum MiscFlags {
  CommaSeparated(0x200),  // Should this cl::list split between commas?
  PositionalEatsArgs(0x400),  // Should this positional cl::list eat -args?
  Sink(0x800),  // Should this cl::list eat all unknown options?
  MiscMask(0xE00);   // Union of the above flags.

  public int value;

  MiscFlags(int val) {
    value = val;
  }

  public static MiscFlags getFromValue(int val) {
    for (MiscFlags mf : values())
      if (mf.value == val)
        return mf;
    return null;
  }
}
