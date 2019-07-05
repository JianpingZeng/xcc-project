package backend.type;
/*
 * Extremely C language CompilerInstance
 * Copyright (c) 2015-2019, Jianping Zeng
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

import backend.support.LLVMContext;
import tools.APInt;
import tools.Util;

import java.util.HashMap;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class IntegerType extends DerivedType {
  private int numBits;
  public static final int MIN_INT_BITS = 1;
  public static final int MAX_INT_BITS = (1 << 23) - 1;

  private static HashMap<Integer, IntegerType> typeCaChes = new HashMap<>();

  public IntegerType(LLVMContext ctx, int numBits) {
    super(ctx, IntegerTyID);
    this.numBits = numBits;
  }

  public boolean isSigned() {
    return true;
  }

  public static IntegerType get(LLVMContext ctx, int numBits) {
    Util.assertion(numBits >= MIN_INT_BITS, "bitwidth too small!");
    Util.assertion(numBits <= MAX_INT_BITS, "bitwidth too large!");

    switch (numBits) {
      case 1:
        return (IntegerType) Type.getInt1Ty(ctx);
      case 8:
        return (IntegerType) Type.getInt8Ty(ctx);
      case 16:
        return (IntegerType) Type.getInt16Ty(ctx);
      case 32:
        return (IntegerType) Type.getInt32Ty(ctx);
      case 64:
        return (IntegerType) Type.getInt64Ty(ctx);
    }

    if (typeCaChes.containsKey(numBits))
      return typeCaChes.get(numBits);

    IntegerType itt = new IntegerType(ctx, numBits);
    typeCaChes.put(numBits, itt);
    return itt;
  }

  public int getBitWidth() {
    return numBits;
  }

  public long getBitMask() {
    return ~0L >> (64 - numBits);
  }

  public APInt getMask() {
    return APInt.getAllOnesValue(getBitWidth());
  }

  /**
   * This method determines if the width of this IntegerType is a power of 2
   * in terms of 8 bit bytes.
   *
   * @return
   */
  public boolean isPowerOf2ByteWidth() {
    return (numBits > 7) && Util.isPowerOf2(numBits);
  }
}
