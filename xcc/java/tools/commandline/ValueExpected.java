package tools.commandline;
/*
 * Extremely Compiler Collection.
 * Copyright (c) 2015-2020, Jianping Zeng.
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
 * This enumerate defines some enum constant for indicating whether the value
 * of this option is required.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public enum ValueExpected {
  ValueOptional(0x08),      // The value can appear... or not
  ValueRequired(0x10),      // The value is required to appear!
  ValueDisallowed(0x18),      // A value may not be specified (for flags)

  ValueMask(0x18);

  public int value;

  ValueExpected(int val) {
    value = val;
  }

  public static ValueExpected getFromValue(int val) {
    for (ValueExpected ve : values())
      if (ve.value == val)
        return ve;
    return null;
  }
}
