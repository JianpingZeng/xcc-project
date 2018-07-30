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

import backend.support.MachineFunctionPass;
import backend.target.TargetMachine;

import java.io.PrintStream;

/**
 * This class implements the target-independent code for emitting ELF code.
 * The different target should derives the concrete subclass of this.
 * @author Jianping Zeng
 * @version 0.1
 */
public class ELFWriter extends MachineFunctionPass
{
    private MachineCodeEmitter mce;
    private PrintStream os;
    private TargetMachine tm;

    public ELFWriter(PrintStream os, TargetMachine tm)
    {
        this.os = os;
        this.tm = tm;
    }

    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        return false;
    }

    @Override
    public String getPassName()
    {
        return "ELF Writer for object code";
    }

    public MachineCodeEmitter getMachineCodeEmitter()
    {
        return mce;
    }
}
