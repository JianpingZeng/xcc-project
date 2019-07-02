/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2019, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.value;

/**
 * This interface abstracts some important common operations which require exact attribute attached,
 * such as sdiv, udiv, ashr, lshr, between instruction and constant expression.
 */
public interface ExactBinaryOperator {
  boolean isExact();
  void setIsExact(boolean b);
}
