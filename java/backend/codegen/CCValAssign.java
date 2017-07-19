package backend.codegen;
/*
 * Xlous C language Compiler
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

import tools.OutParamWrapper;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class CCValAssign
{
    public EVT getLocVT()
    {
        return null;
    }

    public boolean isRegLoc()
    {
        return false;
    }

    public int getLocReg()
    {
        return 0;
    }

    public int getLocMemOffset()
    {
        return 0;
    }

    public EVT getValVT()
    {
        return null;
    }

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

    public int getValNo()
    {
        return 0;
    }

    public backend.codegen.CCValAssign.LocInfo getLocInfo()
    {
        return null;
    }

    public interface CCAssignFn
    {
        boolean apply(int valNo, EVT valVT,
                EVT locVT, LocInfo locInfo,
                ArgFlagsTy argFlags,
                OutParamWrapper<CCState> state);
    }

    public interface CCCustomFn
    {
        boolean apply(OutParamWrapper<Integer> valNo,
                OutParamWrapper<Integer> valVT,
                OutParamWrapper<EVT> locVT,
                OutParamWrapper<LocInfo> locInfo,
                OutParamWrapper<ArgFlagsTy> argFlags,
                OutParamWrapper<CCState> state);
    }
}
