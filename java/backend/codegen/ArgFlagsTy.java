package backend.codegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class ArgFlagsTy
{
    private final static long NoFlagSet      = 0L;
    private final static long ZExt           = 1L<<0;  ///< Zero extended
    private final static long ZExtOffs       = 0;
    private final static long SExt           = 1L<<1;  ///< Sign extended
    private final static long SExtOffs       = 1;
    private final static long InReg          = 1L<<2;  ///< Passed in register
    private final static long InRegOffs      = 2;
    private final static long SRet           = 1L<<3;  ///< Hidden struct-ret ptr
    private final static long SRetOffs       = 3;
    private final static long ByVal          = 1L<<4;  ///< Struct passed by value
    private final static long ByValOffs      = 4;
    private final static long Nest           = 1L<<5;  ///< Nested fn static chain
    private final static long NestOffs       = 5;
    private final static long ByValAlign     = 0xFL << 6; //< Struct alignment
    private final static long ByValAlignOffs = 6;
    private final static long Split          = 1L << 10;
    private final static long SplitOffs      = 10;
    private final static long OrigAlign      = 0x1FL<<27;
    private final static long OrigAlignOffs  = 27;
    private final static long ByValSize      = 0xffffffffL << 32; //< Struct size
    private final static long ByValSizeOffs  = 32;

    private final static long One            = 1L; //< 1 of this type, for shifts

    private long Flags;
    
    public ArgFlagsTy()
    {}

    public boolean isZExt()
    {
        return (Flags & ZExt) != 0;
    }

    public void setZExt()
    {
        Flags |= One << ZExtOffs;
    }

    public boolean isSExt()
    {
        return (Flags & SExt) != 0;
    }

    public void setSExt()
    {
        Flags |= One << SExtOffs;
    }

    public boolean isInReg()
    {
        return (Flags & InReg) != 0;
    }

    public void setInReg()
    {
        Flags |= One << InRegOffs;
    }

    public boolean isSRet()
    {
        return (Flags & SRet) != 0;
    }

    public void setSRet()
    {
        Flags |= One << SRetOffs;
    }

    public boolean isByVal()
    {
        return (Flags & ByVal) != 0;
    }

    public void setByVal()
    {
        Flags |= One << ByValOffs;
    }

    public boolean isNest()
    {
        return (Flags & Nest) != 0;
    }

    public void setNest()
    {
        Flags |= One << NestOffs;
    }

    public int getByValAlign()
    {
        return (int) ((One << ((Flags & ByValAlign) >> ByValAlignOffs)) / 2);
    }

    public void setByValAlign(int A)
    {
        Flags = (Flags & ~ByValAlign) | ((Util.log2(A) + 1) << ByValAlignOffs);
    }

    public boolean isSplit()
    {
        return (Flags & Split) != 0;
    }

    public void setSplit()
    {
        Flags |= One << SplitOffs;
    }

    public int getOrigAlign()
    {
        return (int) ((One << ((Flags & OrigAlign) >> OrigAlignOffs)) / 2);
    }

    public void setOrigAlign(int A)
    {
        Flags = (Flags & ~OrigAlign) | (Util.log2((A) + 1) << OrigAlignOffs);
    }

    public int getByValSize()
    {
        return (int) ((Flags & ByValSize) >> ByValSizeOffs);
    }

    public void setByValSize(int S)
    {
        Flags = (Flags & ~ByValSize) | ((long) S) << ByValSizeOffs;
    }

    /// getArgFlagsString - Returns the flags as a string, eg: "zext align:4".
    public String getArgFlagsString()
    {
        StringBuilder buf = new StringBuilder("< ");
        if (isZExt())
            buf.append("zext ");
        if (isSExt())
            buf.append("sext ");
        if (isInReg())
            buf.append("inreg ");
        if (isSRet())
            buf.append("sret ");
        if (isByVal())
            buf.append("byval ");
        if (isNest())
            buf.append("nest ");
        if (getByValAlign()!=0)
            buf.append("byval-align:").append(getByValAlign()).append(" ");
        if (getOrigAlign()!=0)
            buf.append("orig-align:").append(getOrigAlign()).append(" ");
        if (getByValSize() != 0)
            buf.append("byval-size:").append(getByValSize()).append(" ");

        return buf.append(">").toString();
    }

    /// getRawBits - Represent the flags as a bunch of bits.
    public long getRawBits()
    {
        return Flags;
    }
}
