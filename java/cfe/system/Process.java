package cfe.system;
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

import config.Config;
import tools.OSInfo;
import tools.Pair;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class Process {
  public static int getStandardErrColumns() {
    if (OSInfo.isLinux() || OSInfo.isMacOSX() || OSInfo.isMacOS()) {
      String val = System.getenv("columns");
      if (val != null)
        return Integer.parseInt(val);
      return 143;
    } else
      return 90;
  }

  public static boolean isDigit(char ch) {
    return ch >= '0' && ch <= '9';
  }

  /**
   * Split into two substrings around the first occurence of a
   * separator character.
   * <p>
   * If {@code separator} is in the string, then the result is a pair (LHS, RHS)
   * such that (this == LHS + Separator + RHS) is true and RHS is
   * maximal. If eparator is not in the string, then the result is a
   * pair (LHS, RHS) where (this == LHS) and (RHS == "").
   *
   * @param str       The string to splited.
   * @param separator - The character to split on.
   * @return The split substrings.
   */
  private static Pair<String, String> split(String str, String separator) {
    int pos = str.indexOf(separator);
    if (pos != -1)
      return Pair.get(str.substring(0, pos), str.substring(pos + separator.length()));
    else
      return Pair.get(str, "");
  }

  public static String getHostTriple() {
    String hostTripleString = Config.HostTriple;
    Pair<String, String> archSplit = split(hostTripleString, "-");
    String triple = archSplit.first;
    triple += "-";
    triple += archSplit.second;
    char[] temp = triple.toCharArray();

    if (temp[0] == 'i' && isDigit(temp[1]) && temp[2] == '8' && temp[3] == '6')
      temp[1] = '3';
    triple = String.valueOf(temp);

    return triple;
  }

  public static Boolean getStandardErrHasColors() {
    return false;
  }
}
