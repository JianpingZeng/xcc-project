/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package utils.tablegen;

import tools.Util;

import java.io.PrintStream;
import java.util.TreeMap;

public class StringToOffsetTable {
  private TreeMap<String, Integer> stringOffset;
  private StringBuilder bufferedString;

  public StringToOffsetTable() {
    stringOffset = new TreeMap<>();
    bufferedString = new StringBuilder();
  }

  public int getOrAddStringOffset(String str) {
    Util.assertion(str != null && !str.isEmpty());
    if (stringOffset.containsKey(str))
      return stringOffset.get(str);
    int offset = bufferedString.length();
    bufferedString.append(str).append("\\0");
    stringOffset.put(str, offset);
    return offset;
  }

  public void emitString(PrintStream os) {
    os.print("    \"");
    int charsPrinted = 0;
    for (int i = 0, e = bufferedString.length(); i < e; i++) {
      if (charsPrinted > 70) {
        os.print("\"+\n    \"");
        charsPrinted = 0;
      }

      os.print(bufferedString.charAt(i));
      ++charsPrinted;

      if (bufferedString.charAt(i) != '\\')
        continue;

      Util.assertion(i+1 < e, "Incomplete escape sequence!");;
      if (Character.isDigit(bufferedString.charAt(i+1))) {
        Util.assertion(Character.isDigit(bufferedString.charAt(i+2)) &&
            Character.isDigit(bufferedString.charAt(i+3)),
            "Expected 3 digit octal escape!");
        os.print(bufferedString.charAt(++i));
        os.print(bufferedString.charAt(++i));
        os.print(bufferedString.charAt(++i));
        charsPrinted += 3;
      }
      else {
        os.print(bufferedString.charAt(++i));
        ++charsPrinted;
      }
    }
    os.print("\"");
  }
}
