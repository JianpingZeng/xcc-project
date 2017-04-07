package backend.target;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous
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
 * This record contains all of the information known about a
 * particular register.  The subRegs field (if not null) is an array of
 * registers that are sub-register of the specified register. The superRegs
 * field (if not null) is an array of registers that are super-register of
 * the specified register. This is needed for architectures like X86 which
 * have AL alias AX alias EAX. Registers that this does not apply to simply
 * should set this to null.
 */
public final class TargetRegisterDesc
{
    /**
     * Assembly language getIdentifier for the register.
     */
    public String name;
    /**
     * Register Alias Set, described above
     */
    public int[] subRegs;

    public int[] superRegs;
    /**
     * flags identifying register properties (below).
     */
    public int flags;
    /**
     * Target Specific flags.
     */
    public int tSFlags;

    public TargetRegisterDesc(String Name, int[] SubRegs,
            int[] SuperRegs, int Flags, int TSFlags)
    {
        name = Name;
        subRegs = SubRegs;
        superRegs = SuperRegs;
        flags = Flags;
        tSFlags = TSFlags;
    }
}
