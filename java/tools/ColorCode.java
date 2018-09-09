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

package tools;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class ColorCode {
  /**
   * This color code table is indexed by following format.
   * [background or not][bold or thick][ eight kind of colors code in line spectrum]
   */
  private static final String[][][] ColorCodes = {
      {
          {"\033[0;30m", "\033[0;31m", "\033[0;32m", "\033[0;33m", "\033[0;34m", "\033[0;35m", "\033[0;36m", "\033[0;37m"},
          {"\033[0;1;30m", "\033[0;1;31m", "\033[0;1;32m", "\033[0;1;33m", "\033[0;1;34m", "\033[0;1;35m", "\033[0;1;36m", "\033[0;1;37m"}
      },
      {
          {"\033[0;40m", "\033[0;41m", "\033[0;42m", "\033[0;43m", "\033[0;44m", "\033[0;45m", "\033[0;46m", "\033[0;47m"},
          {"\033[0;1;40m", "\033[0;1;41m", "\033[0;1;42m", "\033[0;1;43m", "\033[0;1;44m", "\033[0;1;45m", "\033[0;1;46m", "\033[0;1;47m"}
      }
  };

  /**
   * Changes the background or background color of this output stream.
   *
   * @param colors     The color which stream would be set.
   * @param bold       Indicates if the font is bold in screen.
   * @param background Indicates we should change the color of background
   */
  public static String getColorCode(Colors colors, boolean bold, boolean background) {
    return ColorCodes[background ? 1 : 0][bold ? 1 : 0][colors.ordinal() & 0x7];
  }

  public static String getResetColor() {
    return "\033[0m";
  }

  public static String getWhiteBold() {
    return "\033[1m";
  }
}
