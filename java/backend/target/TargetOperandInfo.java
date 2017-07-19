package backend.target;
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

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class TargetOperandInfo
{
    interface OperandFlags
    {
        int LookupPtrRegClass = 0;
        int Predicate = 1;
        int OptionalDef = 2;
    }

    interface OperandConstraint
    {
        int TIED_TO = 0;
    }

    public int regClass;
    public int flags;
    public int constraints;

    public TargetRegisterClass getRegClass(TargetRegisterInfo tri)
    {
        if (isLookupPtrRegClass())
            return tri.getPointerRegClass(regClass);

        return tri.getRegClass(regClass);
    }

    public boolean isLookupPtrRegClass()
    {
        return (flags & (1 << OperandFlags.LookupPtrRegClass)) != 0;
    }

    public boolean isPredicate()
    {
        return (flags & OperandFlags.Predicate) != 0;
    }

    public boolean isOptionalDef()
    {
        return (flags & OperandFlags.OptionalDef) != 0;
    }
}
