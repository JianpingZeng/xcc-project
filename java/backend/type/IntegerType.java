package backend.type;
/*
 * Extremely C language CompilerInstance
 * Copyright (c) 2015-2018, Xlous
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

import tools.Util;
import tools.APInt;
import tools.Util;

import java.util.HashMap;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class IntegerType extends DerivedType
{
    private int numBits;
    public static final int MIN_INT_BITS = 1;
    public static final int MAX_INT_BITS = (1<<23) - 1;

    private static final IntegerType Int1Ty = new IntegerType(1);
    private static final IntegerType Int8Ty = new IntegerType(8);
    private static final IntegerType Int16Ty = new IntegerType(16);
    private static final IntegerType Int32Ty = new IntegerType(32);
    private static final IntegerType Int64Ty = new IntegerType(64);

    private static HashMap<Integer, IntegerType> typeCaChes = new HashMap<>();

    private IntegerType(int numBits)
    {
        super(IntegerTyID);
        this.numBits = numBits;
    }

    public boolean isSigned()
    {
        return true;
    }

    public boolean isIntegerType()
    {
        return true;
    }

    public static IntegerType get(int numBits)
    {
        Util.assertion(numBits>= MIN_INT_BITS, "bitwidth too small!");
        Util.assertion(numBits<= MAX_INT_BITS, "bitwidth too large!");

        switch (numBits)
        {
            case 1: return Int1Ty;
            case 8: return Int8Ty;
            case 16: return Int16Ty;
            case 32: return Int32Ty;
            case 64: return Int64Ty;
        }

        if (typeCaChes.containsKey(numBits))
            return typeCaChes.get(numBits);

        IntegerType itt = new IntegerType(numBits);
        typeCaChes.put(numBits, itt);
        return itt;
    }

    public int getBitWidth()
    {
        return numBits;
    }

    public long getBitMask()
    {
        return ~0L >> (64 - numBits);
    }

    public APInt getMask()
    {
        return APInt.getAllOnesValue(getBitWidth());
    }

    /**
     * This method determines if the width of this IntegerType is a power of 2
     * in terms of 8 bit bytes.
     * @return
     */
    public boolean isPowerOf2ByteWidth()
    {
        return (numBits > 7) && Util.isPowerOf2(numBits);
    }
}
