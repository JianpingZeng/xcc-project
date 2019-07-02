package tools.commandline;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2019, Jianping Zeng.
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

import tools.OutRef;
import tools.Util;

import java.util.ArrayList;

import static java.lang.System.out;
import static tools.commandline.ValueExpected.ValueRequired;

/**
 * This file defines  an interface for providing some useful methods with Command
 * line parser.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public interface ParserInterface<T> {
  boolean parse(Option<?> opt,
                String optName, String arg,
                OutRef<T> val);

  int getNumOptions();

  String getOption(int index);

  String getDescription(int index);

  default String getValueStr(Option<?> opt, String defaultMsg) {
    if (opt.value == null)
      return defaultMsg;
    return String.valueOf(opt.value);
  }

  /**
   * Determines the width of the option tc for printing.
   *
   * @param opt
   * @return
   */
  default int getOptionWidth(Option<?> opt) {
    if (opt.hasOptionName()) {
      int size = opt.optionName.length() + 6;
      if (opt.valueStr != null && !opt.valueStr.isEmpty())
        size += opt.valueStr.length() + 6;

      for (int i = 0, e = getNumOptions(); i != e; i++) {
        size = Math.max(size, getOption(i).length() + 8);
      }
      return size;
    } else {
      int basesize = 0;
      if (opt.valueStr != null && !opt.valueStr.isEmpty())
        basesize += opt.valueStr.length() + 6;

      for (int i = 0, e = getNumOptions(); i != e; i++) {
        basesize = Math.max(basesize, getOption(i).length() + 8);
      }
      return basesize;
    }
  }

  /**
   * Print out information of the given {@code opt}.
   *
   * @param opt
   * @param globalWidth
   */
  default void printOptionInfo(Option<?> opt, int globalWidth) {
    if (opt.hasOptionName()) {
      int l = opt.optionName.length();
      out.printf("  -%s", opt.optionName);
      int valStrLen = 0;
      if (opt.valueStr != null && !opt.valueStr.isEmpty()) {
        out.printf("=<%s>", opt.valueStr);
        valStrLen = opt.valueStr.length() + 3;
      }

      out.printf("%s - %s\n",
          Util.fixedLengthString(globalWidth - l - valStrLen - 6, ' '),
          opt.helpStr);
      for (int i = 0, e = getNumOptions(); i != e; i++) {
        int numSpaces = globalWidth - getOption(i).length() - 8;
        out.printf("    =%s%s -  %s\n", getOption(i),
            Util.fixedLengthString(numSpaces, ' '),
            getDescription(i));
      }
    } else {
      if (opt.helpStr != null && !opt.helpStr.isEmpty())
        out.printf("  %s\n", opt.helpStr);

      for (int i = 0, e = getNumOptions(); i != e; i++) {
        int numSpaces = globalWidth - getOption(i).length() - 8;
        out.printf("    -%s%s - %s\n", getOption(i),
            Util.fixedLengthString(numSpaces, ' '),
            getDescription(i));
      }
    }
  }

  <T> void initialize(Option<T> opt);

  default void getExtraOptionNames(ArrayList<String> optionNames) {
  }

  default ValueExpected getValueExpectedFlagDefault() {
    return ValueRequired;
  }

  /**
   * Overloaded in subclass to provide a better default value.
   *
   * @return
   */
  default String getValueName() {
    return "value";
  }
}
