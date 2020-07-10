/*
 * Extremely Compiler Collection
 *   Copyright (c) 2015-2020, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.value;

public interface OverflowingBinaryOperator {
  void setHasNoUnsignedWrap(boolean val);
  boolean getHasNoUnsignedWrap();
  void setHasNoSignedWrap(boolean val);
  boolean getHasNoSignedWrap();
}
