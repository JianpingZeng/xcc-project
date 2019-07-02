/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2019, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.value;

import backend.type.PointerType;

public interface GEPOperator {
  boolean isInBounds();
  void setIsInBounds(boolean b);
  int getIndexBegin();
  int getIndexEnd();
  Value getPointerOperand();
  int getPointerOperandIndex();
  PointerType getPointerOperandType();
  int getNumIndices();
  boolean hasIndices();
  boolean hasAllZeroIndices();
  boolean hasAllConstantIndices();
}
