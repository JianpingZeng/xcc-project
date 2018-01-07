package backend.codegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
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

/**
 * Represent assignment of one arg/retval to a location.
 * @author Xlous.zeng
 * @version 0.1
 */
public class CCValAssign
{
    public enum LocInfo
    {
        Full,   // The value fills the full location.
        SExt,   // The value is sign extended in the location.
        ZExt,   // The value is zero extended in the location.
        AExt,   // The value is extended with undefined upper bits.
        BCvt,   // The value is bit-converted in the location.
        Indirect // The location contains pointer to the value.
        // TODO: a subset of the value is in the location.
    }

    /**
     * This is the value number being assgined (e.g. an argument number).
     */
    private int valNo;
    /**
     * Either a stack offset or a register number.
     */
    private int loc;
    /**
     * True if the location assigned to is memory.
     */
    private boolean isMem;
    /**
     * True if this arg/retval requires special handling.
     */
    private boolean isCustom;
    /**
     * Information about how the arg/retval is assgined.
     */
    private LocInfo htp;

    /**
     * the type of value being assigned.
     */
    private EVT valVT;
    /**
     * the type of location being assigned to.
     */
    private EVT locVT;

    public EVT getLocVT()
    {
        return locVT;
    }

    public boolean isRegLoc()
    {
        return !isMem;
    }

    public boolean isMemLoc()
    {
        return isMem;
    }

    public int getLocReg()
    {
        assert isRegLoc();
        return loc;
    }

    public int getLocMemOffset()
    {
        assert isMem;
        return loc;
    }

    public EVT getValVT()
    {
        return valVT;
    }

    public int getValNo()
    {
        return valNo;
    }

    public backend.codegen.CCValAssign.LocInfo getLocInfo()
    {
        return htp;
    }

    public boolean needsCustom()
    {
        return isCustom;
    }

    public boolean isExtInLoc()
    {
        return htp == LocInfo.AExt ||
                htp == LocInfo.SExt ||
                htp == LocInfo.ZExt;
    }

    public static CCValAssign getReg(int valNo,
            EVT valVT,
            int regNo,
            EVT locVT,
            LocInfo htp)
    {
        CCValAssign assign = new CCValAssign();
        assign.valNo = valNo;
        assign.valVT = valVT;
        assign.loc = regNo;
        assign.isMem = false;
        assign.isCustom = false;
        assign.htp = htp;
        assign.locVT = locVT;
        return assign;
    }

    public static CCValAssign getCustomReg(int valNo,
            EVT valVT,
            int regNo,
            EVT locVT,
            LocInfo htp)
    {
        CCValAssign assign = getReg(valNo, valVT, regNo, locVT, htp);
        assign.isCustom = true;
        return assign;
    }

    public static CCValAssign getMem(int valNo,
            EVT valVT,
            int offset,
            EVT locVT,
            LocInfo htp)
    {
        CCValAssign assign = new CCValAssign();
        assign.valNo = valNo;
        assign.valVT = valVT;
        assign.loc = offset;
        assign.isMem = true;
        assign.isCustom = false;
        assign.htp = htp;
        assign.locVT = locVT;
        return assign;
    }

    public static CCValAssign getCustomMem(int valNo,
            EVT valVT,
            int offset,
            EVT locVT,
            LocInfo htp)
    {
        CCValAssign assign = getMem(valNo, valVT, offset, locVT, htp);
        assign.isCustom = true;
        return assign;
    }
}
