/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.codegen.dagisel;

import backend.codegen.EVT;
import backend.target.TargetLowering;
import backend.type.Type;
import gnu.trove.list.array.TIntArrayList;
import tools.OutParamWrapper;

import java.util.ArrayList;

import static backend.codegen.dagisel.FunctionLoweringInfo.computeValueVTs;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class RegsForValue
{
    TargetLowering tli;
    ArrayList<EVT> valueVTs;
    ArrayList<EVT> regVTs;
    TIntArrayList regs;

    public RegsForValue()
    {
        valueVTs = new ArrayList<>();
        regVTs = new ArrayList<>();
        regs = new TIntArrayList();
    }

    public RegsForValue(TargetLowering tli,
            TIntArrayList regs,
            EVT regvt, EVT valuevt)
    {
        this();
        this.tli = tli;
        valueVTs.add(valuevt);
        regVTs.add(regvt);
        this.regs = regs;
    }

    public RegsForValue(TargetLowering tli,
            int reg, Type ty)
    {
        this.tli = tli;
        computeValueVTs(tli, ty, valueVTs);
        for (int i = 0, e = valueVTs.size(); i < e; i++)
        {
            EVT valueVT = valueVTs.get(i);
            int numRegs = tli.getNumRegisters(valueVT);
            EVT registerVT = tli.getRegisterType(valueVT);
            for (int j = 0; j < numRegs; j++)
                regs.add(reg+j);
            regVTs.add(registerVT);
            reg += numRegs;
        }
    }

    public void append(RegsForValue rhs)
    {
        tli = rhs.tli;
        valueVTs.addAll(rhs.valueVTs);
        regVTs.addAll(rhs.regVTs);
        regs.addAll(rhs.regs);
    }

    public SDValue getCopyFromRegs(SelectionDAG dag,
            OutParamWrapper<SDValue> chain,
            OutParamWrapper<SDValue> flag)
    {
        ArrayList<SDValue> values = new ArrayList<>();
        ArrayList<SDValue> parts = new ArrayList<>();
        for (int i = 0, part = 0, e = valueVTs.size(); i < e; i++)
        {
            EVT valueVT = valueVTs.get(i);
            int numRegs = tli.getNumRegisters(valueVT);
            EVT registerVT = regVTs.get(i);

            for (int j = 0; j < numRegs; j++)
            {
                SDValue p;
                if (flag == null)
                    p = dag.getCopyFromReg(chain, regs.get(part + i), registerVT);
                else
                {
                    p = dag.getCopyFromReg(chain, regs.get(part + i), registerVT, flag);
                    flag
                }


            }
        }
    }

    public void getCopyFromRegs(SDValue val,
            SelectionDAG dag,
            OutParamWrapper<SDValue> chain,
            OutParamWrapper<SDValue> flag)
    {}
}
