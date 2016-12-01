package backend.codegen;
/*
 * Xlous C language Compiler
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

import backend.target.TargetRegisterInfo.TargetRegisterClass;
import tools.Pair;

import java.util.ArrayList;

import static backend.target.TargetRegisterInfo.FirstVirtualRegister;

/**
 * Maps register number to register classes which used to assist register allocation.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class SSARegMap
{
    /**
     * Mapping from virtual register number to its attached register class and
     * define machine operand.
     */
    private ArrayList<Pair<TargetRegisterClass, MachineOperand>> vregInfo;

    public SSARegMap()
    {
        vregInfo = new ArrayList<>();
    }

    private int rescale(int reg)
    {
        return reg - FirstVirtualRegister;
    }

    /**
     * Obatins the target register class for the given virtual register.
     * @param reg
     * @return
     */
    public TargetRegisterClass getRegClass(int reg)
    {
        int actualReg = rescale(reg);
        assert actualReg< vregInfo.size():"Register out of bound!";
        return vregInfo.get(actualReg).first;
    }

    /**
     * Creates and returns a new virtual register in the current function with
     * specified target register class.
     * @param regClass
     * @return
     */
    public int createVirtualRegister(TargetRegisterClass  regClass)
    {
        vregInfo.add(new Pair<>(regClass, null));
        return vregInfo.size() - 1 + FirstVirtualRegister;
    }

    public void clear()
    {
        vregInfo.clear();
    }

    /**
     * Gets the definition machine operand of the specified virtual register.
     * @param regNo
     * @return
     */
    public MachineOperand getDefinedMO(int regNo)
    {
        assert regNo >= FirstVirtualRegister
                : "the regNo is not a virtual register";
        int actualReg = rescale(regNo);
        assert actualReg< vregInfo.size():"Register out of bound!";
        return vregInfo.get(actualReg).second;
    }
}
