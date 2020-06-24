package tools.commandline;
/*
 * Extremely C language Compiler.
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
 * Flags for the number of occurrences allowed
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public enum NumOccurrences {
  Optional(0x01),      // Zero or One occurrence
  ZeroOrMore(0x02),    // Zero or more occurrences allowed
  Required(0x03),      // One occurrence required
  OneOrMore(0x04),     // One or more occurrences required

  // ConsumeAfter - Indicates that this option is fed anything that follows the
  // last positional argument required by the application (it is an error if
  // there are zero positional arguments, and a ConsumeAfter option is used).
  // Thus, for example, all arguments to LLI are processed until a filename is
  // found.  Once a filename is found, all of the succeeding arguments are
  // passed, unprocessed, to the ConsumeAfter option.
  //
  ConsumeAfter(0x05),

  OccurrencesMask(0x07);

  public final int value;

  NumOccurrences(int val) {
    value = val;
  }

  public static NumOccurrences getFromValue(int val) {
    for (NumOccurrences no : values())
      if (no.value == val)
        return no;
    return null;
  }
}
