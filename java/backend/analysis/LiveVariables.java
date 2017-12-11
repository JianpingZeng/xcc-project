package backend.analysis;
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

import backend.codegen.*;
import backend.codegen.MachineRegisterInfo.DefUseChainIterator;
import backend.pass.AnalysisUsage;
import backend.support.DepthFirstOrder;
import backend.target.TargetInstrDesc;
import backend.target.TargetInstrInfo;
import backend.target.TargetRegisterInfo;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import tools.BitMap;
import tools.OutParamWrapper;

import java.util.*;

import static backend.target.TargetRegisterInfo.FirstVirtualRegister;
import static backend.target.TargetRegisterInfo.isPhysicalRegister;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class LiveVariables extends MachineFunctionPass
{
    /**
     * This class canpsulates some live information about virtual register.
     * We represents this with there different piece of info: the set of
     * blocks in which the virtual register variable is live throughout,
     * the set of blocks in which the virtual register is actually used,
     * and the set of non-phi instructions that are the last user of this
     * virtual register.
     * <p>
     *  In the common case where a value is defined and used in the same
     *  block.There is only one defined instruction and used instruction,
     *  and aliveBlock is empty.
     * </p>
     * <p>
     *  Otherwise, the virtual register is live out of the block. If the
     *  virtual register is live throughout any blocks, these blocks are
     *  listed in aliveBlock list. Note that those blocks where the liveness
     *  range ends are not included in aliveBlocks, instead being contained
     *  in the lastUsed list. In these blocks, the virtual registe is live
     *  into the block and lives until the last used instruction.
     *
     *  Note that there cannot ever be a virtual register whose lastUsed set
     *  contians two instructions from the same basic block.
     * </p>
     * <p>
     *  PHI nodes complicate things a bit. If a PHI node is the last user of
     *  a specified virtual register in one of its predecessor block, it will
     *  not be includes in lastUsed set, instead dose include the predecessor
     *  block in the aliveBlock list. This leads to the situation where a
     *  virtual register is defined in a block and the last use is PHI in the
     *  successor. In this case, aliveBlock is empty (this virtual register
     *  is not live across any blocks) and lastUsed set is empty (PHI node not
     *  included). This is sential since the virtual register must be live to
     *  the end of the block, but is not live in any successor blocks.
     *  </p>
     */
    public static class VarInfo
    {
        //todo add a DefInst. 2017.7.27
        public MachineInstr defInst;

        /**
         * Set of blocks where the virtual register associated with this VarInfo
         * is live throughout.
         * This is a bit set which uses the block number as its index.
         */
        public TIntArrayList aliveBlocks;
        /**
         * The numbers of uses of this virtual register across entire function.
         *
         * Note that the defined instruction of this virtual register can be
         * found by {@linkplain MachineRegisterInfo#vregInfo}'s second
         * part.
         */
        public int numUses;
        /**
         * The list of MachineInstr which are the last using of this virtual
         * register in their machine basic block.
         */
        public ArrayList<MachineInstr> kills;

        public VarInfo()
        {
            aliveBlocks = new TIntArrayList();
            numUses = 0;
            kills = new ArrayList<>();
        }

        /**
         * Remove the usage corresponding to {@code mi}. Return true if
         * the {@code mi} contained in {@linkplain #kills} otherwise
         * return false.
         * @param mi
         * @return
         */
        public boolean removeLastUse(MachineInstr mi)
        {
            return kills.remove(mi);
        }
    }

    /**
     * This list is mapping from the virtual register number to variable information.
     * {@linkplain TargetRegisterInfo#FirstVirtualRegister} is subtracted from
     * virtual register number before indexing into this list.
     *
     */
    private VarInfo[] virRegInfo;

    /**
     * This map keeps track of all of the registers that
     * are dead immediately after an instruction reads its operands.  If an
     * instruction does not have an entry in this map, it kills no registers.
     */
    private HashMap<MachineInstr, TIntArrayList> registerKilled;

    /**
     * This map keeps track of all of the registers that are
     * dead immediately after an instruction executes, which are not dead after
     * the operands are evaluated.  In practice, this only contains registers
     * which are defined by an instruction, but never used.
     */
    private HashMap<MachineInstr, TIntArrayList> registerDeaded;

    /**
     * Records which instruction was the last use of a physical register.
     * This is a purely local property, since all physical register are presumed
     * destroyed before across another block.
     */
    private MachineInstr[] phyRegDef;

    /**
     * Keep track of if the specified physical register is used or not.
     */
    private MachineInstr[] phyRegUses;

    /**
     * Keeps track of PHI node.
     */
    private TIntArrayList[] phiVarInfo;

    /**
     * Keeps track the distance of a MI away from the start of block.
     */
    private TObjectIntHashMap<MachineInstr> distanceMap;
    /**
     * The set of allocatable physical register in the target machine.
     */
    private BitMap allocatablePhyRegs;

    private TargetRegisterInfo regInfo;
    private MachineRegisterInfo machineRegInfo;

    public LiveVariables()
    {
        distanceMap = new TObjectIntHashMap<>();
    }

    @Override
    public String getPassName()
    {
        return "Live variable analysis pass";
    }

    /**
     * Walking through each block in the entire control flow graph of this
     * machine function.
     * Computing the live information about each virtual register, like
     * what machine instruction defines it, where it is alive through and
     * what is the set of last used mi.
     *
     * @param mf
     * @return
     */
    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        regInfo = mf.getTarget().getRegisterInfo();
        machineRegInfo = mf.getMachineRegisterInfo();
        virRegInfo = new VarInfo[machineRegInfo.getLastVirReg()-FirstVirtualRegister+1];

        allocatablePhyRegs = regInfo.getAllocatableSet(mf);
        TargetInstrInfo instInfo = mf.getTarget().getInstrInfo();
        registerDeaded = new HashMap<>();
        registerKilled = new HashMap<>();

        int numRegs = regInfo.getNumRegs();
        phyRegDef = new MachineInstr[numRegs];
        phyRegUses = new MachineInstr[numRegs];

        phiVarInfo = new TIntArrayList[mf.getNumBlockIDs()];
        for (int i = 0; i < mf.getNumBlockIDs(); i++)
            phiVarInfo[i] = new TIntArrayList();

        analyzePhiNodes(mf);

        // Computes the live information for virtual register in the depth-first
        // first order on the CFG of the function. It ensures that we we see the
        // definition before use since the domination property of SSA form.
        // But there is a exception that PHI node, that will be handled specially
        // later.
        ArrayList<MachineBasicBlock> visited = DepthFirstOrder.reversePostOrder(mf.getEntryBlock());
               for (MachineBasicBlock mbb : visited)
        {
            distanceMap.clear();
            for (int i = 0, e = mbb.getLiveIns().size(); i < e; i++)
            {
                int preg = mbb.getLiveIns().get(i);
                assert isPhysicalRegister(preg):"Virtual register can not used in Live-in";
                handlePhyRegDef(preg, null);
            }

            // loop over all of the mi, processing them.
            int dist = 0;
            for (MachineInstr inst : mbb.getInsts())
            {
                distanceMap.put(inst, dist++);

                // process all the operands.
                int numOperands = inst.getNumOperands();
                if (inst.getOpcode() == TargetInstrInfo.PHI)
                    numOperands = 1;

                TIntArrayList defRegs = new TIntArrayList();
                TIntArrayList useRegs = new TIntArrayList();
                for (int i = 0; i < numOperands; i++)
                {
                    MachineOperand mo = inst.getOperand(i);
                    if (!mo.isRegister() || mo.getReg() == 0)
                        continue;
                    if (mo.isUse() && !mo.isDef())
                        useRegs.add(mo.getReg());
                    else if (mo.isDef())
                        defRegs.add(mo.getReg());
                }

                // process all uses.
                for (int i = 0; i < useRegs.size(); i++)
                {
                    int reg = useRegs.get(i);
                    if (machineRegInfo.isPhysicalReg(reg)
                            && allocatablePhyRegs.get(reg))
                    {
                        handlePhyRegUse(reg, inst);
                    }
                    else if (machineRegInfo.isVirtualReg(reg))
                    {
                        handleVirRegUse(reg, inst);
                    }
                }
                // process all defs.
                for (int i = 0; i < defRegs.size(); i++)
                {
                    int reg = defRegs.get(i);
                    if (machineRegInfo.isPhysicalReg(reg)
                            && allocatablePhyRegs.get(reg))
                        handlePhyRegDef(reg, inst);
                    else if (machineRegInfo.isVirtualReg(reg))
                    {
                        handleVirRegDef(reg, inst);
                    }
                }
            }

            // handle any virtual assignments from PHI node which might be
            // at the bottom of this basic block. We check all of successor
            // blocks to see if they have PHI node, and if so, we simulate
            // an assignment at the end of the current block.
            TIntArrayList varInfoForPhi = phiVarInfo[mbb.getNumber()];
            if (varInfoForPhi != null && !varInfoForPhi.isEmpty())
            {
                for (int i = 0, e = varInfoForPhi.size(); i < e; i++)
                {
                    // mark it alive only in the block where it coming from
                    // (means current block).
                    int reg = varInfoForPhi.get(i);
                    markVirRegAliveInBlock(getVarInfo(reg),
                            machineRegInfo.getDefMI(reg).getParent(),
                            mbb);
                }
            }

            // Finally, if the last instruction in the block is a return,
            // make sure it as using all of the live out values in the fucntion.
            TargetInstrDesc tid = mbb.getInsts().getLast().getDesc();
            if (!mbb.isEmpty() && tid.isReturn())
            {
                MachineInstr ret = mbb.getInsts().getLast();
                TIntArrayList liveouts = mf.getMachineRegisterInfo().getLiveOuts();
                for (int i = 0; i < liveouts.size(); i++)
                {
                    int preg = liveouts.get(i);
                    assert isPhysicalRegister(preg):"Virtual register can't used as Live-out";
                    handlePhyRegUse(preg, ret);

                    // Add this physical register as implicit-use operand of ret instr
                    if (!ret.readsRegister(preg, null))
                        ret.addOperand(MachineOperand.createReg(preg, false, true));
                }
            }

            // Loop over PhysRegDef / PhysRegUse, killing any registers that are
            // available at the end of the basic block.
            for (int i = 0; i < numRegs; i++)
            {
                if (phyRegDef[i] != null)
                    handlePhyRegDef(i, null);
            }
            Arrays.fill(phyRegDef, null);
            Arrays.fill(phyRegUses, false);
        }

        for (int i = 0; i < virRegInfo.length; i++)
        {
            VarInfo vi = virRegInfo[i];
            for (int j = 0, sz2 = vi.kills.size(); j != sz2; j++)
            {
                MachineInstr mi = vi.kills.get(j);
                if (mi == machineRegInfo.getDefMI(i + FirstVirtualRegister))
                {
                    // this instruction defines this virtual register and use it,
                    // also there is no other inst use it later.
                    if (!registerDeaded.containsKey(mi))
                        registerDeaded.put(mi, new TIntArrayList());
                    registerDeaded.get(mi).add(i+FirstVirtualRegister);
                }
                else
                {
                    // otherwise, this instruction is the last use of the
                    // virtual register.
                    if (!registerKilled.containsKey(mi))
                        registerKilled.put(mi, new TIntArrayList());
                    registerKilled.get(mi).add(i+FirstVirtualRegister);
                }
            }
        }

        // sort the all register killed or deaded by walking through registerDeaded
        // registerKilled in the order of increasing the register number.
        // in order to perform binary search for efficiency.
        // registerDeaded.forEach((key1, value1) -> value1.sort());
        // registerKilled.forEach((key, value) -> value.sort());

        return false;
    }

    private MachineInstr findLastPartialDef(int reg, OutParamWrapper<Integer> partDefReg)
    {
        int lastDefReg = 0;
        int lastDefDist = 0;
        MachineInstr lastDef = null;
        for (int subReg : regInfo.getSubRegisters(reg))
        {
            MachineInstr def = phyRegDef[subReg];
            if (def == null) continue;
            int dist = distanceMap.get(def);
            if (dist > lastDefDist)
            {
                lastDefReg = subReg;
                lastDef = def;
                lastDefDist = dist;
            }
        }
        partDefReg.set(lastDefReg);
        return lastDef;
    }

    private void handlePhyRegUse(int phyReg, MachineInstr mi)
    {
        if (phyRegDef[phyReg] == null && phyRegUses[phyReg] == null)
        {
            OutParamWrapper<Integer> x = new OutParamWrapper<>(0);
            MachineInstr lastPartialDef = findLastPartialDef(phyReg, x);
            int partDefReg = x.get();
            if (lastPartialDef != null)
            {
                lastPartialDef.addOperand(MachineOperand.createReg(phyReg, true, true));
                phyRegDef[phyReg] = lastPartialDef;
                TIntHashSet processed = new TIntHashSet();
                for (int sub : regInfo.getSubRegisters(phyReg))
                {
                    if (processed.contains(sub)) continue;
                    if (sub == partDefReg || regInfo.isSubRegister(partDefReg, sub))
                        continue;
                    lastPartialDef.addOperand(MachineOperand.createReg(sub, false, true));
                    phyRegDef[sub] = lastPartialDef;
                    for (int ss : regInfo.getSubRegisters(sub))
                        processed.add(ss);
                }
            }
        }
        phyRegUses[phyReg] = mi;

        for (int subReg : regInfo.get(phyReg).subRegs)
        {
            phyRegDef[subReg] = mi;
            phyRegUses[subReg] = mi;
        }
    }

    private void handleVirRegUse(int virReg, MachineInstr mi)
    {
        assert machineRegInfo.getDefMI(virReg) != null:"register use before def!";
        VarInfo varInfo = getVarInfo(virReg);
        MachineBasicBlock mbb = mi.getParent();
        MachineBasicBlock defBB = machineRegInfo.getDefMI(virReg).getParent();
        ++varInfo.numUses;

        if (!varInfo.kills.isEmpty() && varInfo.kills
                .get(varInfo.kills.size()-1).getParent() == mbb)
        {
            // If this basic block is already a kill block. Increase the live
            // range by updating the kill instruction
            varInfo.kills.set(varInfo.kills.size() - 1, mi);
            return;
        }

        if (mbb.equals(defBB)) return;

        varInfo.kills.add(mi);
        // Update all dominating blocks to mark them known live.
        markVirRegAliveInBlock(varInfo, defBB, mbb);
    }

    /**
     * Adds kill register and its sub-register of specified machine instr.
     * @param reg
     * @param mi
     * @return  Return true if add a register as kill to mi.
     */
    private boolean handlePhysRegKill(int reg, MachineInstr mi)
    {
        if (phyRegUses[reg] == null && phyRegDef[reg] == null)
            return false;

        MachineInstr lastRefOrPartRef = phyRegUses[reg] != null ?
                phyRegUses[reg] : phyRegDef[reg];

        int lastRefOrPartRefDist = distanceMap.get(lastRefOrPartRef);
        TIntHashSet partUses = new TIntHashSet();
        for (int subReg : regInfo.getSubRegisters(reg))
        {
            MachineInstr use = phyRegUses[subReg];
            if (use != null)
            {
                for (int ss : regInfo.getSubRegisters(subReg))
                    partUses.add(ss);
                int dist = distanceMap.get(use);
                if (dist > lastRefOrPartRefDist)
                {
                    lastRefOrPartRef = use;
                    lastRefOrPartRefDist = dist;
                }
            }
        }

        if (lastRefOrPartRef == phyRegDef[reg] && lastRefOrPartRef != mi)
        {
            lastRefOrPartRef.addRegisterDead(reg, regInfo, true);
        }
        else if (phyRegUses[reg] == null)
        {
            phyRegDef[reg].addRegisterDead(reg, regInfo, true);
            for (int sub : regInfo.getSubRegisters(reg))
            {
                if (partUses.contains(sub))
                {
                    boolean needDef = true;
                    if (phyRegDef[reg] == phyRegDef[sub])
                    {
                        MachineOperand mo = phyRegDef[reg].findRegisterDefOperand(sub, false, null);
                        if (mo != null)
                        {
                            needDef = false;
                            assert !mo.isDead();
                        }
                    }

                    if (needDef)
                        phyRegDef[reg].addOperand(MachineOperand.createReg(sub, true, true));
                    lastRefOrPartRef.addRegisterKilled(sub, regInfo, true);
                    for (int ss : regInfo.getSubRegisters(sub))
                    {
                        partUses.remove(ss);
                    }
                }
            }
        }
        else
            lastRefOrPartRef.addRegisterKilled(reg, regInfo, true);
        return true;
    }

    /**
     * Return true if the specified register will be used after current mi and
     * before the next defined.
     * @param reg
     * @param mi
     * @param mbb
     * @return
     */
    private boolean hasRegisterUseBelow(int reg, MachineInstr mi, MachineBasicBlock mbb)
    {
        boolean hasDistInfo = true;
        int curDist = distanceMap.get(mi);
        ArrayList<MachineInstr> uses = new ArrayList<>();
        ArrayList<MachineInstr> defs = new ArrayList<>();
        for (DefUseChainIterator itr = machineRegInfo.getRegIterator(reg); itr.hasNext(); itr.next())
        {
            MachineOperand mo = itr.getOpearnd();
            MachineInstr udMI = mo.getParent();
            if (udMI.getParent() != mbb)
                continue;

            boolean isBelow = false;
            if (!distanceMap.containsKey(udMI))
            {
                isBelow = true;
                hasDistInfo = false;
            }
            else if (distanceMap.get(udMI) > curDist)
            {
                isBelow = true;
            }
            if (isBelow)
            {
                if (mo.isUse())
                    uses.add(udMI);
                if (mo.isDef())
                    defs.add(udMI);
            }
        }

        if (uses.isEmpty())
            return false;
        else if (defs.isEmpty())
            return true;
        if (!hasDistInfo)
        {
            ++curDist;
            int idx = mbb.getInsts().indexOf(mi);
            ++idx;
            for (; idx < mbb.size(); idx++)
            {
                distanceMap.put(mbb.getInstAt(idx), curDist);
            }
        }

        int earliestUse = distanceMap.get(uses.get(0));
        for (int i = 1, e = uses.size(); i < e; i++)
        {
            int dist = distanceMap.get(uses.get(i));
            if (dist < earliestUse)
                earliestUse = dist;
        }
        for (MachineInstr defMI : defs)
        {
            int dist = distanceMap.get(defMI);
            if (dist < earliestUse)
                return false;
        }
        return true;
    }

    private void handlePhyRegDef(int phyReg, MachineInstr mi)
    {
        TIntHashSet live = new TIntHashSet();
        if (phyRegDef[phyReg] != null || phyRegUses[phyReg] != null)
        {
            live.add(phyReg);
            for (int sub : regInfo.getSubRegisters(phyReg))
            {
                live.add(sub);
            }
        }
        else
        {
            // Check if the sub register of this was used or defined previously.
            for (int sub : regInfo.getSubRegisters(phyReg))
            {
                if (phyRegDef[sub] != null || phyRegUses[sub] != null)
                {
                    live.add(sub);
                    for (int ss : regInfo.getSubRegisters(sub))
                    {
                        live.add(ss);
                    }
                }
            }
        }

        if (!handlePhysRegKill(phyReg, mi))
        {
            for (int sub : regInfo.getSubRegisters(phyReg))
            {
                if (!live.contains(sub))
                    continue;
                if (handlePhysRegKill(sub, mi))
                {
                    live.remove(sub);
                    for (int ss : regInfo.getSubRegisters(sub))
                        live.remove(ss);
                }
            }
            assert live.isEmpty():"Not all defined registers are killed/dead";
        }

        if (mi != null)
        {
            // Does this extend the live range of a super-register?
            TIntHashSet processed = new TIntHashSet();
            for (int superReg : regInfo.getSubRegisters(phyReg))
            {
                if (!processed.add(superReg))
                    continue;
                MachineInstr lastRef = phyRegUses[superReg] != null ?
                        phyRegUses[superReg] : phyRegDef[superReg];
                if (lastRef != null && lastRef != mi)
                {
                    if (hasRegisterUseBelow(superReg, mi, mi.getParent()))
                    {
                        mi.addOperand(MachineOperand.createReg(superReg, false,
                                true, true, false, false, false, 0));
                        mi.addOperand(MachineOperand.createReg(superReg, true,
                                true));
                        phyRegDef[superReg] = mi;
                        phyRegUses[superReg] = null;
                        processed.add(superReg);
                        for (int ss : regInfo.getSubRegisters(superReg))
                        {
                            phyRegDef[ss] = mi;
                            phyRegUses[ss] = null;
                            processed.add(ss);
                        }
                    }
                    else
                    {
                        if (handlePhysRegKill(superReg, mi))
                        {
                            phyRegDef[superReg] = null;
                            phyRegUses[superReg] = null;
                            for (int ss : regInfo.getSubRegisters(superReg))
                            {
                                phyRegUses[ss] = null;
                                phyRegDef[ss] = null;
                                processed.add(ss);
                            }
                        }
                    }
                }
            }
        }

        phyRegDef[phyReg] = mi;

        for (int subReg : regInfo.get(phyReg).subRegs)
        {
            phyRegDef[subReg] = mi;
        }
    }

    private void handleVirRegDef(int virReg, MachineInstr mi)
    {
        VarInfo varInfo = getVarInfo(virReg);
        assert varInfo.defInst == null:"Multiple defs";
        varInfo.defInst = mi;

        // if the varInfo is not alive in any block, default to dead.
        if (varInfo.aliveBlocks.isEmpty())
            varInfo.kills.add(mi);
    }

    /**
     * Gets the VarInfo for virtual register, possible creating a
     * new instance of VarInfo is needed.
     * @param regIdx
     * @return
     */
    public VarInfo getVarInfo(int regIdx)
    {
        assert machineRegInfo.isVirtualReg(regIdx)
                && regIdx <= machineRegInfo.getLastVirReg()
                :"not a valid virtual register!";
        regIdx -= FirstVirtualRegister;
        if (regIdx >= virRegInfo.length)
        {
            VarInfo[] temp = new VarInfo[regIdx*2];
            System.arraycopy(virRegInfo, 0, temp, 0, virRegInfo.length);
            virRegInfo = temp;
        }

        if (virRegInfo[regIdx] != null)
            return virRegInfo[regIdx];

        return virRegInfo[regIdx] = new VarInfo();
    }

    /**
     * Looks for the block this variable is live in throughout upside along with
     * predecessor link.
     * <pre>
     *  [virReg = ....] (defMBB)
     *          |
     *          |
     *  [... = virReg + 1;] previous uses (mbb)
     *  [.................]
     *  [....= virReg + 1;] current uses
     *  </pre>
     * @param varInfo
     * @param defMBB
     * @param mbb
     */
    private void markVirRegAliveInBlock(VarInfo varInfo,
            MachineBasicBlock defMBB, MachineBasicBlock mbb)
    {
        LinkedList<MachineBasicBlock> worklist = new LinkedList<>();
        markVirRegAliveInBlock(varInfo, defMBB, mbb, worklist);

        while(!worklist.isEmpty())
        {
            MachineBasicBlock pred = worklist.getFirst();
            worklist.removeFirst();
            markVirRegAliveInBlock(varInfo, defMBB, pred, worklist);
        }
    }

    /**
     * Looks for the block this variable is live in throughout upside along with
     * predecessor link.
     * <pre>
     *  [virReg = ....] (defMBB)
     *          |
     *          |
     *  [... = virReg + 1;] previous uses (mbb)
     *  [.................]
     *  [....= virReg + 1;] current uses
     *  </pre>
     * @param varInfo
     * @param defMBB
     * @param mbb
     * @param worklist
     */
    private void markVirRegAliveInBlock(VarInfo varInfo,
            MachineBasicBlock defMBB, MachineBasicBlock mbb,
            LinkedList<MachineBasicBlock> worklist)
    {
        int mbbNo = mbb.getNumber();

        // check to see if this block is one of the lastUsed blocks.
        // if so, remove it, because virReg is live out in the same block.
        for (int i = 0; i < varInfo.kills.size();)
        {
            if (varInfo.kills.get(i).getParent() == mbb)
            {
                varInfo.kills.remove(i);
                break;
            }
            i++;
        }

        if (defMBB == mbb)
            return;

        // we already known the blocks is live, return early.
        if (varInfo.aliveBlocks.contains(mbbNo))
            return;

        // mark the variable known alive in this mbb.
        varInfo.aliveBlocks.add(mbbNo);
        for (Iterator<MachineBasicBlock> predItr = mbb.predIterator(); predItr.hasNext();)
        {
            worklist.add(predItr.next());
        }
    }

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        assert au != null;
        au.addRequired(UnreachableMachineBlockElim.class);
        au.setPreservedAll();
        super.getAnalysisUsage(au);
    }

    /**
     * Collects information about PHI nodes. In particular, we want
     * to map the variable information of a virtual register which used
     * in a PHI node to the BB the virtual register is comign from.
     * @param mf
     */
    private void analyzePhiNodes(MachineFunction mf)
    {
        for (MachineBasicBlock mbb : mf.getBasicBlocks())
        {
            MachineInstr mi;
            for (int i = 0, e = mbb.size(); i < e &&
                    ( mi = mbb.getInstAt(i)).getOpcode() == TargetInstrInfo.PHI; i++)
            {
                for (int j = 1, sz = mi.getNumOperands(); j < sz; j+=2)
                    phiVarInfo[mi.getOperand(j+1).getMBB().getNumber()].
                            add(mi.getOperand(j).getReg());
            }
        }
    }

    private static TIntArrayList dummmy = new TIntArrayList();

    public TIntArrayList getKillsList(MachineInstr mi)
    {
        return registerKilled.containsKey(mi)
                ? registerKilled.get(mi) : dummmy;
    }

    public TIntArrayList getDeadedDefList(MachineInstr mi)
    {
        return registerDeaded.containsKey(mi)
                ? registerDeaded.get(mi) : dummmy;
    }

    /**
     * Checks to see if the specified mi kills this reg.
     * @param mi
     * @param reg
     * @return
     */
    public boolean killRegister(MachineInstr mi, int reg)
    {
        return getKillsList(mi).contains(reg);
    }

    /**
     * Checks to see if this reg is deaded after this mi. That means
     * the reg is defined by this mi and not used later.
     * @param mi
     * @param reg
     * @return
     */
    public boolean registerDefIsDeaded(MachineInstr mi, int reg)
    {
        return getDeadedDefList(mi).contains(reg);
    }

    /**
     * When the address of an instruction changes, this
     * method should be called so that live variables can update its internal
     * data structures.  This removes the records for OldMI, transfering them to
     * the records for NewMI.
     * @param oldMI
     * @param newMI
     */
    public void instructionChanged(MachineInstr oldMI, MachineInstr newMI)
    {

    }

    /**
     * Add a register as the killing of the specified mi in the order of increasing
     * the register number.
     * @param incomingReg
     * @param mi
     */
    public void addVirtualRegisterKilled(int incomingReg, MachineInstr mi)
    {
        // handle the special common case.
        if (!registerKilled.containsKey(mi))
            registerKilled.put(mi, new TIntArrayList());

        TIntArrayList list = registerKilled.get(mi);
        if (list.isEmpty() || incomingReg > list.get(list.size() - 1))
            list.add(incomingReg);
        // sort the order.
        for (int i = 0; i < list.size(); i++)
        {
            if (list.get(i) < incomingReg)
                continue;
            // avoids duplicate insertion.
            if (incomingReg != list.get(i))
            {
                list.insert(i, incomingReg);
                break;
            }
        }
        getVarInfo(incomingReg).kills.add(mi);
    }

    /**
     * Remove the specified virtual register from the live variable information.
     * Returns true if the variable was marked as killed by the specified
     * instruction, false otherwise.
     * @param reg
     * @param mi
     */
    public boolean removeVirtualRegisterKilled(int reg, MachineInstr mi)
    {
        if (!getVarInfo(reg).removeLastUse(mi))
            return false;

        TIntArrayList list = getKillsList(mi);
        for (int i = 0; i < list.size(); i++)
        {
            if (list.get(i) == reg)
            {
                list.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Remove all killed info for the specified instruction.
     * @param mi
     */
    public void removeVirtualRegisterKilled(MachineInstr mi)
    {
        if (registerKilled.containsKey(mi))
        {
             TIntArrayList list = registerKilled.get(mi);
            for (int i = 0, e = list.size(); i< e; i++)
            {
                boolean removed = removeVirtualRegisterKilled(list.get(i), mi);
                assert removed :"Removed vir reg is not existed";
            }
            registerKilled.remove(mi);
        }
    }

    /**
     * Add information about the fact that the specified
     * register is dead after being used by the specified instruction.
     * @param reg
     * @param mi
     */
    public void addVirtualRegisterDead(int reg, MachineInstr mi)
    {
        if (!registerDeaded.containsKey(mi))
            registerDeaded.put(mi, new TIntArrayList());

        TIntArrayList list = registerDeaded.get(mi);
        // handles the special common cases.
        if (list.isEmpty() || reg > list.size())
            list.add(reg);

        for (int i = 0, e = list.size(); i < e; i++)
        {
            if (list.get(i) < reg)
                continue;

            // avoids duplicate insertion.
            if (list.get(i) != reg)
            {
                list.insert(i, reg);
                break;
            }
        }
        getVarInfo(reg).kills.add(mi);
    }

    /**
     * Remove the specified virtual register from the live variable information.
     * Returns true if the variable was marked dead at the specified instruction,
     * false otherwise.
     * @param reg
     * @param mi
     * @return
     */
    public boolean removeVirtualRegisterDead(int reg, MachineInstr mi)
    {
        if (!getVarInfo(reg).removeLastUse(mi))
            return false;

        TIntArrayList list = getDeadedDefList(mi);
        for (int i = 0, e = list.size(); i < e; i++)
        {
            if (list.get(i) == reg)
            {
                list.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Remove all of the specified dead registers from the live variable
     * information.
     * @param mi
     */
    public void removeVirtualRegisterDead(MachineInstr mi)
    {
        if (registerDeaded.containsKey(mi))
        {
            TIntArrayList list = registerDeaded.get(mi);
            for (int i = 0, e = list.size(); i < e; i++)
            {
                boolean removed = removeVirtualRegisterDead(list.get(i), mi);
                assert removed :"Virtual register is not existed!";
            }
            registerDeaded.remove(mi);
        }
    }

    public void replaceKillInstruction(int reg, MachineInstr oldMI,
            MachineInstr newMI)
    {
        VarInfo vi = getVarInfo(reg);

        vi.kills.replaceAll(machineInstr ->
        {
            if (machineInstr == oldMI)
                return newMI;
            else
                return oldMI;
        });
    }

    public BitMap getAllocatablePhyRegs()
    {
        return allocatablePhyRegs;
    }
}
