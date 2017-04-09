package backend.target.x86;
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

import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineFunction;
import backend.codegen.MachineFunctionPass;
import backend.codegen.MachineInstr;
import backend.pass.RegisterPass;

import static backend.codegen.MachineInstrBuilder.buildMI;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86PeepholeOptimizer extends MachineFunctionPass
{
    @Override
    public String getPassName()
    {
        return "X86 Peephole optimization pass";
    }

    /**
     * This method must be overridded by concrete subclass for performing
     * desired machine code transformation or analysis.
     *
     * @param mf
     * @return
     */
    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        boolean changed = false;
        for (MachineBasicBlock mbb : mf.getBasicBlocks())
        {
            for (int i = 0; i < mbb.size(); )
            {
                i = optimizeInst(mbb, i);
                changed |= res;
            }
        }
        return changed;
    }
    private boolean res = false;
    private int optimizeInst(MachineBasicBlock mbb, int idx)
    {
        MachineInstr curMI = mbb.getInstAt(idx);
        MachineInstr next = idx == mbb.size() - 1? null: mbb.getInstAt(idx+1);
        switch (curMI.getOpCode())
        {
            case X86InstrNames.MOVrr8:
            case X86InstrNames.MOVrr16:
            case X86InstrNames.MOVrr32:
                // destroy X=X copy.
                if (curMI.getOperand(0).getReg()
                        == curMI.getOperand(1).getReg())
                {
                    mbb.erase(idx);
                    res = true;
                    return idx;
                }
                res = false;
                return idx+1;
            case X86InstrNames.ADDri16:
            case X86InstrNames.ADDri32:
            case X86InstrNames.SUBri16:
            case X86InstrNames.SUBri32:
            case X86InstrNames.IMULri16:
            case X86InstrNames.IMULri32:
            case X86InstrNames.ANDri16:
            case X86InstrNames.ANDri32:
            case X86InstrNames.ORri16:
            case X86InstrNames.ORri32:
            case X86InstrNames.XORri16:
            case X86InstrNames.XORri32:
                assert curMI.getNumOperands() == 3:"There should have 3 opernds!";
                if (curMI.getOperand(2).isImmediate())
                {
                    long val = curMI.getOperand(2).getImmedValue();
                    // If the value is the same when signed extended from 8 bits
                    if (val == (byte)val)
                    {
                        int opcode;
                        switch (curMI.getOpCode())
                        {
                            default: assert false:"Unknown opcode value!";
                            case X86InstrNames.ADDri16: opcode = X86InstrNames.ADDri16b;break;
                            case X86InstrNames.ADDri32: opcode = X86InstrNames.ADDri32b; break;
                            case X86InstrNames.SUBri16: opcode = X86InstrNames.SUBri16b; break;
                            case X86InstrNames.SUBri32: opcode = X86InstrNames.SUBri32b; break;
                            case X86InstrNames.IMULri16: opcode = X86InstrNames.IMULri16b; break;
                            case X86InstrNames.IMULri32: opcode = X86InstrNames.IMULri32b; break;
                            case X86InstrNames.ANDri16: opcode = X86InstrNames.ANDri16; break;
                            case X86InstrNames.ANDri32: opcode = X86InstrNames.ANDri32b; break;
                            case X86InstrNames.ORri16: opcode = X86InstrNames.ORri16b; break;
                            case X86InstrNames.ORri32: opcode = X86InstrNames.ORri32b; break;
                            case X86InstrNames.XORri16: opcode = X86InstrNames.XORri16b; break;
                            case X86InstrNames.XORri32:opcode = X86InstrNames.XORri32b; break;
                        }

                        int r0 = curMI.getOperand(0).getReg();
                        int r1 = curMI.getOperand(1).getReg();
                        mbb.replace(idx, buildMI(opcode, 2, r0).addReg(r1).
                                addZImm((byte)val).getMInstr());
                        res = true;
                        return idx + 1;
                    }
                }
                res = false;
                return idx+1;
            case X86InstrNames.BSWAPr32:
            {
                // Change bswap EAX, bswap EAX into nothing.
                if (next.getOpCode() == X86InstrNames.BSWAPr32
                        && curMI.getOperand(0).getReg() ==
                        next.getOperand(0).getReg())
                {
                    mbb.erase(idx);
                    res = true;
                    return idx;
                }
                res = false;
                return idx+1;
            }
            default:
                res = false;
                return idx+1;
        }
    }

    public static X86PeepholeOptimizer createX86PeepholeOptimizer()
    {
        return new X86PeepholeOptimizer();
    }

    /**
     * Register X86 peephole optimization pass.
     */
    public static RegisterPass x86PPOPassRegister =
            new RegisterPass("X86 peephole optimizer", X86PeepholeOptimizer.class);
}