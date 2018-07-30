package backend.codegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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
 * @author Jianping Zeng
 * @version 0.1
 */
public class MachineLocation
{
    private boolean IsRegister;                      // True if location is a register.
    private int Register;                    // gcc/gdb register number.
    private int Offset;                           // Displacement if not register.

    // The target register number for an abstract frame pointer. The value is
    // an arbitrary value greater than TargetRegisterInfo::FirstVirtualRegister.
    public static final int VirtualFP = ~0;

    public MachineLocation()
    {
        IsRegister = false;
        Register = 0;
        Offset = 0;
    }

    public MachineLocation(int R)
    {
        IsRegister = true;
        Register = R;
        Offset = 0;
    }

    public MachineLocation(int R, int O)
    {
        IsRegister = false;
        Register = R;
        Offset = O;
    }

    public MachineLocation(MachineLocation other)
    {
        super();
        IsRegister = other.IsRegister;
        Register = other.Register;
        Offset = other.Offset;
    }

    // Accessors
    public boolean isReg()
    {
        return IsRegister;
    }

    public int getReg()
    {
        return Register;
    }

    public int getOffset()
    {
        return Offset;
    }

    public void setIsRegister(boolean Is)
    {
        IsRegister = Is;
    }

    public void setRegister(int R)
    {
        Register = R;
    }

    public void setOffset(int O)
    {
        Offset = O;
    }

    public void set(int R)
    {
        IsRegister = true;
        Register = R;
        Offset = 0;
    }

    public void set(int R, int O)
    {
        IsRegister = false;
        Register = R;
        Offset = O;
    }

    public void dump()
    {
    }
}
