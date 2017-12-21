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

import backend.target.TargetRegisterClass;
import gnu.trove.map.hash.TIntIntHashMap;
import tools.SetMultiMap;

import java.util.Set;

import static backend.target.TargetRegisterInfo.isPhysicalRegister;
import static backend.target.TargetRegisterInfo.isVirtualRegister;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class VirtRegMap
{
    public interface ModRef
    {
        int isRef = 1;
        int isMod = 2;
        int isModRef = 3;
    }

    private MachineFunction mf;
    private TIntIntHashMap v2pMap;
    private TIntIntHashMap v2StackSlotMap;
    private SetMultiMap<MachineInstr, Integer> mi2vMap;

    public VirtRegMap(MachineFunction mf)
    {
        this.mf = mf;
        v2pMap = new TIntIntHashMap();
        v2StackSlotMap = new TIntIntHashMap();
        mi2vMap = new SetMultiMap<>();
    }

    public int getPhys(int virReg)
    {
        assert isVirtualRegister(virReg);
        return v2pMap.get(virReg);
    }

    public void assignVirt2Phys(int virtReg, int phyReg)
    {
        assert isVirtualRegister(virtReg) && isPhysicalRegister(phyReg);
        v2pMap.put(virtReg, phyReg);
    }

    public int assignVirt2StackSlot(int virtReg)
    {
        TargetRegisterClass rc = mf.getMachineRegisterInfo().getRegClass(virtReg);
        int fi = mf.getFrameInfo().createStackObject(rc);
        v2StackSlotMap.put(virtReg, fi);
        return fi;
    }

    public void assignVirt2StackSlot(int virtReg, int slot)
    {
        assert isVirtualRegister(virtReg);
        assert !v2StackSlotMap.containsKey(virtReg);
        v2StackSlotMap.put(virtReg, slot);
    }

    /**
     * Unlink the virtual register from map v2pMap.
     *
     * @param virtReg
     */
    public void clearVirt(int virtReg)
    {
        v2pMap.remove(virtReg);
    }

    public boolean hasStackSlot(int virtReg)
    {
        assert isVirtualRegister(virtReg) : "Should be virtual register";
        return v2StackSlotMap.containsKey(virtReg);
    }

    public int getStackSlot(int virtReg)
    {
        assert isVirtualRegister(virtReg) : "Should be virtual register";
        return v2StackSlotMap.get(virtReg);
    }

    public void virtFolded(int virtReg, MachineInstr oldMI, MachineInstr newMI)
    {
        // move previous memory references folded to new instruction
        assert mi2vMap.containsKey(oldMI);
        Set<Integer> regs = mi2vMap.get(oldMI);
        assert regs != null;
        regs.forEach(reg -> mi2vMap.remove(oldMI, reg));

        regs.forEach(reg ->
        {
            mi2vMap.put(newMI, reg);
        });
        mi2vMap.put(newMI, virtReg);
    }

    public Set<Integer> getFoldedVirts(MachineInstr mi)
    {
        return mi2vMap.containsKey(mi) ? mi2vMap.get(mi) : null;
    }
}
