package frontend.codegen;
/*
 * Xlous C language CompilerInstance
 * Copyright (c) 2015-2016, Xlous
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

import backend.value.Value;
import frontend.type.QualType;

/**
 * This represents an lvalue references.  Because C allow bitfields,
 * this is not a simple HIR pointer, it may be a pointer plus a bitrange.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public final class LValue
{
    private enum Kind
    {
        /**
         * This is a normal l-value, use getAddress().
         */
        Simple,

        /**
         * This is a bitfield l-value, use getBitfield.
         */
        BitField
    }
    private Kind LVType;

    private Value v;

    private class BitFieldData
    {
        int startBit;
        int size;
        boolean isSigned;
    }
    private BitFieldData bitFieldData;

    private boolean Volatile;
    private boolean restrict;

    private static void setQualifiers(int qualifiers, LValue r)
    {
        r.Volatile = (qualifiers& QualType.VOLATILE_QUALIFIER) != 0;
        r.restrict = (qualifiers& QualType.RESTRICT_QUALIFIER) != 0;
    }

    public boolean isSimple() {return  LVType == Kind.Simple;}
    public boolean isBitField() {return LVType == Kind.BitField;}

    public boolean isVolatileQualified() { return Volatile;}
    public boolean isRestrictQualified() {return restrict;}
    public int getQualifiers()
    {
        return (Volatile? QualType.VOLATILE_QUALIFIER:0)
                | (restrict?QualType.RESTRICT_QUALIFIER: 0);
    }

    /**
     * Simple value.
     * @return
     */
    public Value getAddress() { assert isSimple(); return v;}

    public Value getBitFieldAddr() { assert isBitField(); return v;}
    public int getBitfieldStartBits(){assert isBitField(); return bitFieldData.startBit;}
    public int getBitfieldSize() {assert isBitField(); return bitFieldData.size;}

    public static LValue makeAddr(Value v, int qualifers)
    {
        LValue r = new LValue();
        r.LVType = Kind.Simple;
        r.v = v;
        setQualifiers(qualifers, r);
        return r;
    }

    public static LValue makeBitfield(Value v,
            int startBit,
            int size,
            boolean isSigned,
            int qualifiers)
    {
        LValue r = new LValue();
        r.v = v;
        r.LVType = Kind.BitField;
        r.bitFieldData.startBit = startBit;
        r.bitFieldData.size = size;
        r.bitFieldData.isSigned = isSigned;
        setQualifiers(qualifiers, r);
        return r;
    }
}
