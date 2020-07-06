package tools;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.

 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;


/**
 * @author Jianping Zeng.
 */
public class Util {
  /* Minimum of signed integral types.  */
  public static final byte INT8_MIN = (-128);
  public static final short INT16_MIN = (-32767 - 1);
  public static final int INT32_MIN = (-2147483647 - 1);
  public static final long INT64_MIN = Long.MIN_VALUE;

  /* Maximum of signed integral types.  */
  public static final byte INT8_MAX = (127);
  public static final short INT16_MAX = (32767);
  public static final int INT32_MAX = (2147483647);
  public static final long INT64_MAX = 9223372036854775807L;

  /* Maximum of unsigned integral types.  */
  public static final short UINT8_MAX = 255;
  public static final int UINT16_MAX = (65535);
  public static final long UINT32_MAX = 4294967295L;
  public static final Long UINT64_MAX = Long.parseUnsignedLong("18446744073709551615");

  /**
   * This chained stack trace entry is used to facilitate printing expressive stack
   * information about when/what pass the program is running on when compiler crashes.
   */
  static PrintStackTraceEntry stackTraceEntry;

  /**
   * A flag indicates whether dump debug information.
   */
  public static boolean DEBUG;

  /**
   * Utility method to combine a base hash with the identity hash of one or more objects.
   *
   * @param hash the base hash
   * @param x    the object to add to the hash
   * @return the combined hash
   */
  public static int hash1(int hash, Object x) {
    // always set at least one bit in case the hash wraps to zero
    return 0x10000000 | (hash + 7 * System.identityHashCode(x));
  }

  /**
   * Utility method to combine a base hash with the identity hash of one or more objects.
   *
   * @param hash the base hash
   * @param x    the first object to add to the hash
   * @param y    the second object to add to the hash
   * @return the combined hash
   */
  public static int hash2(int hash, Object x, Object y) {
    // always set at least one bit in case the hash wraps to zero
    return 0x20000000 | (hash + 7 * System.identityHashCode(x) + 11 * System
        .identityHashCode(y));
  }

  /**
   * Computes the log (base 2) of the specified integer, rounding down.
   * (E.g {@code log2(8) = 3}, {@code log2(21) = 4})
   *
   * @param val the value
   * @return the log base 2 of the value
   */
  public static int log2(int val) {
    return 31 - Integer.numberOfLeadingZeros(val);
  }

  public static int log2(long val) {
    return 63 - Long.numberOfLeadingZeros(val);
  }

  public static int log2Ceil(int val) {
    return 32 - countLeadingZeros32(val - 1);
  }

  public static int log2Ceil(long val) {
    return 64 - countLeadingZeros64(val - 1);
  }

  public static boolean isPowerOf2(long val) {
    return val == 0 || (val & val - 1) == 0;
  }

  public static void shouldNotReachHere() {
    Util.assertion(false, "should not reach here");
  }

  public static void shouldNotReachHere(String msg) {
    Util.assertion("should not reach here, " + msg);
  }

  public static void unimplemented() {
    Util.assertion(false, "Unimplemented method");
  }

  /**
   * Rounds the input size up to the most least size of pow of align,  which
   * not less than {@code size}.
   *
   * @param size
   * @param align
   * @return
   */
  public static int roundUp(int size, int align) {
    // insufficient algorithm
    // return (size/align + 1) * align;
    return (size + (align - 1)) & ~(align - 1);
  }

  /**
   * Rounds the input size up to the most least size of pow of align,  which
   * not less than {@code size}.
   *
   * @param size
   * @param align
   * @return
   */
  public static long roundUp(long size, long align) {
    // insufficient algorithm
    // return (size/align + 1) * align;
    return (size + (align - 1)) & ~(align - 1);
  }

  /**
   * Sets the element at a given position of a list and ensures that this
   * position exists. IfStmt the list is current shorter than the position, intermediate
   * positions are filled with a given value.
   *
   * @param list   the list to put the element into
   * @param pos    the position at which to insert the element
   * @param x      the element that should be inserted
   * @param filler the filler element that is used for the intermediate positions in case the
   *               list is shorter than pos
   */
  public static <T> void atPutGrow(List<T> list, int pos, T x, T filler) {
    if (list.size() < pos + 1) {
      while (list.size() < pos + 1) {
        list.add(filler);
      }
      Util.assertion(list.size() == pos + 1);
    }

    Util.assertion(list.size() >= pos + 1);
    list.set(pos, x);
  }

  /**
   * checks out whether specified number is odd or not.
   *
   * @param num
   * @return
   */
  public static boolean isOdd(int num) {
    return (num & 0x1) != 0;
  }

  /**
   * checks whether specified number is even or not.
   *
   * @param num
   * @return
   */
  public static boolean isEven(int num) {
    return (num & 0x1) == 0;
  }

  public static void truncate(List<?> list, int length) {
    while (list.size() > length) {
      list.remove(list.size() - 1);
    }
  }

  /**
   * Computes the number of one bit about given value of jlang.type long when isUnsigned
   * determines if it is a unsigned.
   *
   * @param val
   * @param isUnsigned
   * @return
   */
  public static int bitsOfOne(long val, boolean isUnsigned) {
    int res = 0;
    while (val != 0) {
      res++;
      val = val & (val - 1);
    }
    return res;
  }

  /**
   * Checks if the first long number is less than second in unsigned comparison.
   * <table border="1">
   * <caption>"Truth table" for an unsigned comparison x &lt; y using signed arithmetic</caption>
   * <tr>
   * <td></td>
   * <th colspan="2">Top bit of x</th>
   * </tr>
   * <tr>
   * <th>Top bit of y</th>
   * <th>0</th>
   * <th>1</th>
   * </tr>
   * </tr>
   * <th>0</th>
   * <td><tt>x &lt; y</tt><br><em>(Signed comparison)</em></td>
   * <th>false</th>
   * </tr>
   * <tr>
   * <th>1</th>
   * <th>true</th>
   * <td><em><tt>x &lt; y</tt><br><em>(Signed comparison)</em></td>
   * </tr>
   * </table>
   *
   * @param n1
   * @param n2
   * @return
   */
  public static boolean ult(long n1, long n2) {
    /**
     boolean cmp = (n1 < n2);
     if ((n1<0) != (n2<0))
     cmp = !cmp;
     return cmp;
     */
    // efficient method.
    return (n1 < n2) ^ ((n1 < 0) != (n2 < 0));
  }

  public static boolean ule(long n1, long n2) {
    return ult(n1, n2) || n1 == n2;
  }

  public static boolean uge(long n1, long n2) {
    return ule(n2, n1);
  }

  public static boolean ugt(long n1, long n2) {
    return ult(n2, n1);
  }

  public static boolean ult(int n1, int n2) {
    return (n1 & 0xffffffffL) < (n2 & 0xffffffffL);
  }

  public static int unsignedDiv(int i1, int i2) {
    long l1 = i1 & 0xffffffffL, l2 = i2 & 0xffffffffL;
    return (int) (l1 / l2);
  }

  public static long unsignedDiv(long l1, long l2) {
    return Long.divideUnsigned(l2, l1);
  }

  public static int countLeadingZeros64(long x) {
    if (x == 0)
      return 64;
    int count = 0;
    for (int shift = 64 >> 1; shift != 0; shift >>>= 1) {
      long temp = x >> shift;
      if (temp != 0)
        x = temp;
      else
        count |= shift;
    }
    return count;
  }

  /**
   * this function performs the platform optimal form of
   * counting the number of zeros from the most significant bit to the first one
   * bit.
   *
   * @param v
   * @return
   */
  public static int countLeadingZeros32(int v) {
    if (v == 0) return 32;

    int count = 0;
    for (int shift = 32 >>> 1; shift != 0; shift >>>= 1) {
      int temp = v >>> shift;
      if (temp != 0) {
        v = temp;
      } else {
        count |= shift;
      }
    }
    return count;
  }

  public static int countLeadingOnes32(int val) {
    return countLeadingZeros32(~val);
  }

  public static int countLeadingOnes64(long val) {
    return countLeadingZeros64(~val);
  }

  private static int[] mod37BitPosition =
      {
          32, 0, 1, 26, 2, 23, 27, 0, 3, 16, 24, 30, 28, 11, 0, 13,
          4, 7, 17, 0, 25, 22, 31, 15, 29, 10, 12, 6, 0, 21, 14, 9,
          5, 20, 8, 19, 18
      };

  public static int countTrailingZeros(int value) {
    return mod37BitPosition[Integer.remainderUnsigned(-value & value, 37)];
  }

  public static int countTrailingOnes(int value) {
    return countTrailingZeros(~value);
  }

  private static int[] mod67Position =
      {
          64, 0, 1, 39, 2, 15, 40, 23, 3, 12, 16, 59, 41, 19, 24, 54,
          4, 64, 13, 10, 17, 62, 60, 28, 42, 30, 20, 51, 25, 44, 55,
          47, 5, 32, 65, 38, 14, 22, 11, 58, 18, 53, 63, 9, 61, 27,
          29, 50, 43, 46, 31, 37, 21, 57, 52, 8, 26, 49, 45, 36, 56,
          7, 48, 35, 6, 34, 33, 0
      };

  public static int countTrailingZeros(long value) {
    return mod67Position[(int) Long.remainderUnsigned(-value & value, 67)];
  }

  public static int countTrailingOnes(long value) {
    return countTrailingZeros(~value);
  }

  public static int countPoplutation(int value) {
    int v = value - ((value >>> 1) & 0x55555555);
    v = (v & 0x33333333) + ((v >>> 2) & 0x33333333);
    return ((v + (v >>> 4) & 0xF0F0F0F) * 0x1010101) >>> 24;
  }

  public static int countPopulation(long value) {
    long v = value - ((value >> 1) & 0x5555555555555555L);
    v = (v & 0x3333333333333333L) + ((v >> 2) & 0x3333333333333333L);
    v = (v + (v >> 4)) & 0x0F0F0F0F0F0F0F0FL;
    return (int) (v * 0x0101010101010101L >> 56);
  }

  public static void Debug(Object... args) {
    if (Util.DEBUG) {
      for (Object arg : args)
        System.err.println(arg);
    }
  }

  public static String fixedLengthString(int repeatNum, String unit) {
    if (repeatNum <= 0 || unit == null || unit.isEmpty())
      return "";
    StringBuilder sb = new StringBuilder();
    for (; repeatNum > 0; --repeatNum)
      sb.append(unit);
    return sb.toString();
  }

  public static String fixedLengthString(int repeatNum, char unit) {
    if (repeatNum <= 0)
      return "";
    char[] temp = new char[repeatNum];
    Arrays.fill(temp, unit);
    return String.valueOf(temp);
  }

  /**
   * Checks the file path specified by arg {@code path} is absolutely or not.
   *
   * @param path
   * @return
   */
  public static boolean isAbsolutePath(String path) {
    Util.assertion(!(path == null || path.isEmpty()));
    if (OSInfo.isWindows()) {
      // Windows operation system.
      return path.startsWith("[A-Za-z]:");
    } else {
      // Unix like operation system.
      return path.startsWith("/");
    }
  }

  /**
   * Return the next number that is power of 2 and greater than the given parameter.
   *
   * @param val
   * @return
   */
  public static int NextPowerOf2(long val) {
    val |= (val >> 1);
    val |= (val >> 2);
    val |= (val >> 4);
    val |= (val >> 8);
    val |= (val >> 16);
    val |= (val >> 32);
    return (int) (val + 1);
  }

  public static int findFirstNonOf(String src, String delims) {
    return findFirstNonOf(src, delims, 0);
  }

  /**
   * Return the index to the first sub-string isn't match the specified
   * delim string from startIdx position.
   *
   * @param src
   * @param delims
   * @param startIdx
   * @return Return -1 when no found. Otherwise return the specified location.
   */
  public static int findFirstNonOf(String src, String delims, int startIdx) {
    for (int i = startIdx; i < src.length(); i++)
      if (delims.indexOf(src.charAt(i)) == -1)
        return i;

    return src.length();
  }

  public static int findFirstOf(String src, String delims, int startIdx) {
    for (int i = startIdx; i < src.length(); i++)
      if (delims.indexOf(src.charAt(i)) != -1)
        return i;

    return -1;
  }

  /***
   * Computes the edit distance between two string. The edit distance is defined
   * as follows.
   * The number of operation needed to be performed to transfrom the str1 to str2
   * 1.delete a character.
   * 2.add a character.
   * 3.replace the old one with new.
   * @param str1
   * @param str2
   * @return
   */
  public static int getEditDistance(String str1, String str2) {
    if ((str2 == null || str1 == null))
      return 0;

    if (str1.isEmpty() && str2.isEmpty())
      return 0;
    if (str1.length() == 1 && str2.length() == 1)
      return 1;

    str1 = str1.toLowerCase();
    str2 = str2.toLowerCase();
    int[] costs = new int[str2.length() + 1];
    for (int j = 0; j < costs.length; j++)
      costs[j] = j;
    for (int i = 1; i <= str1.length(); i++) {
      costs[0] = i;
      int nw = i - 1;
      for (int j = 1; j <= str2.length(); j++) {
        int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
            str1.charAt(i - 1) == str2.charAt(j - 1) ? nw : nw + 1);
        nw = costs[j];
        costs[j] = cj;
      }
    }
    return costs[str2.length()];
  }

  /**
   * A function similar with strpbrk in C library.
   *
   * @param src
   * @param pattern
   * @return
   */
  public static int strpbrk(String src, String pattern) {
    for (int i = 0, e = src.length(); i != e; i++) {
      if (pattern.indexOf(src.charAt(i)) != -1)
        return i;
    }
    return -1;
  }

  /**
   * <pre>
   * Converts a value of type int to float in bit. It equivalence to following
   * C code:
   * float bitsToDouble(int val)
   * {
   *   union
   *   {
   *     int i;
   *     float f;
   *   }t;
   *   t.i = val;
   *   return t.f;
   * }
   * </pre>
   *
   * @param val
   * @return
   */
  public static float bitsToFloat(int val) {
    int sign = (val >>> 31) & 0x1;
    int exp = val >>> 23 & ((1 << 8) - 1) - 127;
    int r = (val & ((1 << 23) - 1));
    float t = 0;
    for (int i = 0; i < 23; i++) {
      t += r & 0x1;
      t /= 2;
      r >>>= 1;
    }
    float res = (float) (Math.pow(2.0f, exp) * (1.0 + t));
    return sign == 1 ? -res : res;
  }

  /**
   * Converts a float value into integer in bitwise.
   * Likewise {@linkplain #bitsToFloat(int)}.
   *
   * @param val
   * @return
   */
  public static int floatToBits(float val) {
    return Float.floatToRawIntBits(val);
  }

  public static char hexDigit(int x) {
    return x < 10 ? (char) ('0' + x) : (char) ('A' + x - 10);
  }

  public static short byteSwap16(short val) {
    short high = (short) ((val >>> 8) & 0xff);
    short low = (short) (val & 0xff);
    return (short) ((low << 8) | high);
  }

  public static int byteSwap32(int val) {
    int high = (val >>> 16) & 0xffff;
    int low = val & 0xffff;
    return (((int) byteSwap16((short) low)) << 16) | byteSwap16((short) high);
  }

  public static long byteSwap64(long val) {
    int high = (int) ((val >>> 32));
    int low = (int) (val);
    return ((long) byteSwap32(low) << 32) | byteSwap32(high);
  }

  public static String longestCommonPrefix(String str1, String str2) {
    if (str1 == null || str2 == null) return null;
    if (str1.isEmpty() || str2.isEmpty()) return "";
    int e = Math.min(str1.length(), str2.length());
    int i = 0;
    for (; i < e && str1.charAt(i) == str2.charAt(i); i++) {
    }
    return str1.substring(0, i);
  }

  public static void reverse(Object[] arrays) {
    if (arrays == null || arrays.length <= 0)
      return;

    for (int i = 0; i < arrays.length / 2; i++) {
      Object t = arrays[i];
      arrays[i] = arrays[arrays.length - 1 - i];
      arrays[arrays.length - 1 - i] = t;
    }
  }

  public static void reverse(Object[] arrays, int begin, int end) {
    if (arrays == null || arrays.length == 0)
      return;
    Util.assertion(begin >= 0 && begin < end && end <= arrays.length);
    for (int i = begin; i < (end - begin) / 2; ++i) {
      Object t = arrays[i];
      arrays[i] = arrays[end - 1 - (i - begin)];
      arrays[end - 1 - (i - begin)] = t;
    }
  }

  public static int minAlign(int a, int b) {
    return (a | b) & -(a | b);
  }

  public static long nextPowerOf2(long a) {
    a |= (a >> 1);
    a |= (a >> 2);
    a |= (a >> 4);
    a |= (a >> 8);
    a |= (a >> 16);
    a |= (a >> 32);
    return a + 1;
  }

  public static String escapeString(String str) {
    StringBuilder buf = new StringBuilder();
    buf.append(str);
    for (int i = 0; i < buf.length(); i++) {
      switch (buf.charAt(i)) {
        case '\n':
          buf.insert(i, '\\');
          ++i;
          buf.setCharAt(i, 'n');
          break;
        case '\t':
          buf.insert(i, ' '); // convert to two spaces.
          ++i;
          buf.setCharAt(i, ' ');
          break;
        case '\\':
          if (i + 1 != buf.length()) {
            switch (buf.charAt(i + 1)) {
              case '1':
                continue;
              case '|':
              case '{':
              case '}':
                buf.deleteCharAt(i);
                continue;
              default:
                break;
            }
          }
          break;
        case '{':
        case '}':
        case '<':
        case '>':
        case '|':
        case '"':
          buf.insert(i, '\\');
          ++i;
          break;
      }
    }
    return buf.toString();
  }

  public static void assertion(boolean cond, String msg) {
    if (!cond)
      throw new CompilerException(msg);
  }

  public static void assertion(boolean cond) {
    assertion(cond, "");
  }

  public static void assertion(String msg) {
    assertion(false, msg);
  }

  private static int printStackTrace(PrintStackTraceEntry entry, PrintStream os) {
    int nextID = 0;
    if (entry.getNext() != null)
      nextID = printStackTrace(entry.getNext(), os);

    os.printf("%d.\t", nextID);
    entry.print(os);
    return nextID + 1;
  }

  static void printCurrentStackTrace(PrintStream os) {
    if (stackTraceEntry == null) return;
    os.println("Stack dump:");
    printStackTrace(stackTraceEntry, os);
  }

  public static <T> void unique(List<T> list) {
    HashSet<T> unique = new HashSet<>(list);
    list.clear();
    list.addAll(unique);
  }

  public static <T> boolean anyOf(Iterator<T> iterator, Predicate<T> pred) {
    while (iterator.hasNext()) {
      if (pred.test(iterator.next()))
        return true;
    }
    return false;
  }

  public static <T> boolean noneOf(Iterator<T> iterator, Predicate<T> pred) {
    while (iterator.hasNext()) {
      if (pred.test(iterator.next()))
        return false;
    }
    return true;
  }

  public static <T> T minIf(Iterator<T> itr,
                            Predicate<T> pred,
                            BiPredicate<T, T> less) {
    if (!itr.hasNext()) return null;
    T min = null;
    while (itr.hasNext()) {
      T val = itr.next();
      if (!pred.test(val)) continue;
      if (min == null)
        min = val;
      else if (val == min || less.test(val, min))
        min = val;
    }
    return min;
  }

  public static <T> T maxIf(Iterator<T> itr,
                            Predicate<T> pred,
                            BiPredicate<T, T> greater) {
    if (!itr.hasNext()) return null;
    T max = null;
    while (itr.hasNext()) {
      T val = itr.next();
      if (!pred.test(val)) continue;
      if (max == null)
        max = val;
      else if (val == max || greater.test(max, val))
        max = val;
    }
    return max;
  }

  public static String getLegalJavaName(String opName) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0, e = opName.length(); i < e; i++) {
      if (opName.charAt(i) == '.')
        sb.append("_");
      else if (opName.charAt(i) == ':') {
        if (i < e - 1 && opName.charAt(i + 1) == ':') {
          sb.append("_");
          i += 1;
        } else
          sb.append("_");
      } else
        sb.append(opName.charAt(i));
    }
    return sb.toString();
  }

  public static boolean isInt32(long val) {
    return val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE;
  }

  public static boolean isInt16(long val) {
    return val >= Short.MIN_VALUE && val <= Short.MAX_VALUE;
  }

  public static char toOctal(int x) {
    x = x & 0x07;
    char[] res = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e'};
    return res[x];
  }

  public static boolean isHexDigit(char ch) {
    return (ch >= '0' && ch <= '9') || (ch >= 'a' && ch < 'f') || (ch >= 'A' && ch <= 'F');
  }
  public static boolean isHexDigit(byte ch) {
    return (ch >= '0' && ch <= '9') || (ch >= 'a' && ch < 'f') || (ch >= 'A' && ch <= 'F');
  }

  public static String unEscapeLexed(String str) {
    if (str == null || str.isEmpty())
      return "";

    StringBuilder buf = new StringBuilder();
    for (int i = 0, e = str.length(); i != e; ) {
      if (str.charAt(i) == '\\') {
        if (i < e - 1 && str.charAt(i + 1) == '\\') {
          buf.append('\\');
          i += 2;
        } else if (i < e - 2 && isHexDigit(str.charAt(i + 1)) && isHexDigit(str.charAt(i + 2))) {
          buf.append((char)Integer.parseInt(str.substring(i + 1, i + 3), 16));
          i += 3;
        } else {
          buf.append(str.charAt(i++));
        }
      } else {
        buf.append(str.charAt(i++));
      }
    }
    return buf.toString();
  }

  public static String escapedString(String name) {
    StringBuilder buf = new StringBuilder();

    for (int i = 0, e = name.length(); i < e; i++) {
      char ch = name.charAt(i);
      if (TextUtils.isPrintable(ch) && ch != '\\' && ch != '"')
        buf.append(ch);
      else
        buf.append(String.format("\\%c%c", hexDigit(ch >> 4), hexDigit(ch & 0xF)));
    }
    return buf.toString();
  }
}
