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
import backend.pass.AnalysisUsage;
import backend.support.DepthFirstOrder;
import backend.target.TargetInstrDesc;
import backend.target.TargetInstrInfo;
import backend.target.TargetRegisterInfo;
import backend.transform.scalars.UnreachableMachineBlockElim;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.*;

import static backend.target.TargetRegisterInfo.FirstVirtualRegister;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class LiveVariable extends MachineFunctionPass
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
        public ArrayList<MachineInstr> lastUsedInst;

        public VarInfo()
        {
            aliveBlocks = new TIntArrayList();
            numUses = 0;
            lastUsedInst = new ArrayList<>();
        }

        /**
         * Remove the usage corresponding to {@code mi}. Return true if
         * the {@code mi} contained in {@linkplain #lastUsedInst} otherwise
         * return false.
         * @param mi
         * @return
         */
        public boolean removeLastUse(MachineInstr mi)
        {
            return lastUsedInst.remove(mi);
        }
    }

    /**
     * This list is mapping from the virtual register number to variable information.
     * {@linkplain TargetRegisterInfo#FirstVirtualRegister} is subtracted from
     * virtual register numbe before indexing into this list.
     *
     */
    private ArrayList<VarInfo> virRegInfo;

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
    private MachineInstr[] phyRegInfo;

    /**
     * Keep track of if the specified physical register is used or not.
     */
    private boolean[] phyRegUses;

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
    private TIntArrayList allocatablePhyRegs;

    private MachineFunction mf;
    private TargetRegisterInfo regInfo;
    private MachineRegisterInfo machineRegInfo;

    public LiveVariable()
    {
        virRegInfo = new ArrayList<>();
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
        this.mf = mf;
        regInfo = mf.getTarget().getRegisterInfo();
        machineRegInfo = mf.getMachineRegisterInfo();
        allocatablePhyRegs = regInfo.getAllocatableSet(mf);
        TargetInstrInfo instInfo = mf.getTarget().getInstrInfo();
        registerDeaded = new HashMap<>();
        registerKilled = new HashMap<>();

        int numRegs = regInfo.getNumRegs();
        phyRegInfo = new MachineInstr[numRegs];
        phyRegUses = new boolean[numRegs];
        phiVarInfo = new TIntArrayList[mf.getNumBlockIDs()];
        for (int i = 0; i < mf.getNumBlockIDs(); i++)
            phiVarInfo[i] = new TIntArrayList();

        analyzePhiNodes(mf);

        // TODO handle physical register which are live in the function.
        // TODO 2016.12.3

        // Computes the live information for virtual register in the depth-first
        // first order on the CFG of the function. It ensures that we we see the
        // definition before use since the domination property of SSA form.
        // But there is a exception that PHI node, that will be handled specially
        // later.
        Set<MachineBasicBlock> visited = DepthFirstOrder.reversePostOrder(mf.getEntryBlock());
        for (MachineBasicBlock mbb : visited)
        {
            distanceMap.clear();
            // loop over all of the mi, processing them.
            int dist = 0;
            for (MachineInstr inst : mbb.getInsts())
            {
                distanceMap.put(inst, dist++);

                // process all the operands.
                int numOperands = inst.getNumOperands();
                if (inst.getOpCode() == TargetInstrInfo.PHI)
                    numOperands = 1;
                TargetInstrDesc instDesc = instInfo.get(inst.getOpCode());

                // process all implicit uses reg.
                if (instDesc.implicitUses != null)
                {
                    for (int implReg : instDesc.implicitUses)
                    {
                        handlePhyRegUse(implReg, inst);
                    }
                }

                // process all explicit uses.
                for (int i = 0; i < numOperands; i++)
                {
                    MachineOperand mo = inst.getOperand(i);
                    if (mo.opIsUse() && mo.getReg() != 0 && mo.isReg())
                    {
                        int reg = mo.getReg();
                        if (machineRegInfo.isPhysicalReg(reg)
                                && allocatablePhyRegs.contains(reg))
                        {
                            handlePhyRegUse(reg, inst);
                        }
                        else if (machineRegInfo.isVirtualReg(reg))
                        {
                            handleVirRegUse(reg, inst);
                        }
                    }
                }

                // process all implicit defs.
                if (instDesc.implicitDefs != null && instDesc.implicitDefs.length != 0)
                {
                    for (int regDef : instDesc.implicitDefs)
                    {
                        handlePhyRegDef(regDef, inst);
                    }
                }

                // process all explicit defs.
                for (int i = 0; i < numOperands; i++)
                {
                    MachineOperand mo = inst.getOperand(i);
                    int reg = mo.getReg();
                    if (mo.opIsDef() && mo.isReg() && mo.getReg() != 0)
                    {
                        if (machineRegInfo.isPhysicalReg(reg)
                                && allocatablePhyRegs.contains(reg))
                            handlePhyRegDef(reg, inst);
                        else if (machineRegInfo.isVirtualReg(reg))
                            handleVirRegDef(reg, inst);
                    }
                }

                // handle any virtual assignments from PHI node which might be
                // at the bottom of this basic block. We check all of successor
                // blocks to see if they have PHI node, and if so, we simulate
                // an assignment at the end of the current block.
                TIntArrayList varInfoForPhi = phiVarInfo[mbb.getNumber()];
                if (!varInfoForPhi.isEmpty())
                {
                    for (TIntIterator itr = varInfoForPhi.iterator(); itr.hasNext();)
                    {
                        // mark it alive only in the block where it coming from
                        // (means current block).
                        int reg = itr.next();
                        markVirRegAliveInBlock(getVarInfo(reg),
                                machineRegInfo.getDefMI(reg).getParent(),
                                mbb);
                    }
                }
            }


            // Finally, if the last instruction in the block is a return,
            // make sure it as using all of the live out values in the fucntion.
            if (!mbb.isEmpty() && instInfo.isReturn(mbb.getInsts().getLast().getOpCode()))
            {
                MachineInstr ret = mbb.getInsts().getLast();
                // TODO live out of function, 2016.12.3.
            }

            // Loop over PhysRegDef / PhysRegUse, killing any registers that are
            // available at the end of the basic block.
            for (int i = 0; i < numRegs; i++)
            {
                if (phyRegInfo[i] != null)
                    handlePhyRegDef(i, null);
            }
            Arrays.fill(phyRegInfo, null);
            Arrays.fill(phyRegUses, false);
        }

        for (int i = 0, sz = virRegInfo.size(); i < sz; i++)
        {
            for (int j = 0, sz2 = virRegInfo.get(i).lastUsedInst.size(); j != sz2; j++)
            {
                MachineInstr mi = virRegInfo.get(i).lastUsedInst.get(j);
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
        registerDeaded.entrySet().forEach(pair -> pair.getValue().sort());
        registerKilled.entrySet().forEach(pair -> pair.getValue().sort());

        return false;
    }

    private void handlePhyRegUse(int phyReg, MachineInstr mi)
    {
        phyRegInfo[phyReg] = mi;
        phyRegUses[phyReg] = true;

        for (int subReg : regInfo.get(phyReg).subRegs)
        {
            phyRegInfo[subReg] = mi;
            phyRegUses[subReg] = true;
        }
    }

    private void handleVirRegUse(int virReg, MachineInstr mi)
    {
        assert machineRegInfo.getDefMI(virReg) != null:"register use before def!";
        VarInfo varInfo = getVarInfo(virReg);
        MachineBasicBlock mbb = mi.getParent();
        MachineBasicBlock defBB = machineRegInfo.getDefMI(virReg).getParent();

        for (int i = 0; i < varInfo.lastUsedInst.size();)
        {
            if (varInfo.lastUsedInst.get(i).getParent() == mbb)
            {
                varInfo.lastUsedInst.remove(i);
                return;
            }
            i++;
        }

        assert mbb != defBB :"should have ";

        varInfo.lastUsedInst.add(mi);
        // Update all dominating blocks to mark them known live.
        markVirRegAliveInBlock(varInfo, defBB, mbb);
    }

    private void handlePhyRegDef(int phyReg, MachineInstr mi)
    {
        // Does this kill a previous version of this register?
        MachineInstr prev = phyRegInfo[phyReg];
        if (prev != null)
        {
            if (phyRegUses[phyReg])
                // previous one is use
                registerKilled.get(prev).add(phyReg);
            else
                // previous one is definition.
                registerDeaded.get(prev).add(phyReg);
        }

        phyRegInfo[phyReg] = mi;
        phyRegUses[phyReg] = false;

        for (int subReg : regInfo.get(phyReg).subRegs)
        {
            phyRegInfo[subReg] = mi;
            phyRegUses[subReg] = false;
        }
    }

    private void handleVirRegDef(int virReg, MachineInstr mi)
    {
        VarInfo varInfo = getVarInfo(virReg);
        // if the varInfo is not alive in any block, default to dead.
        if (varInfo.aliveBlocks.isEmpty())
            varInfo.lastUsedInst.add(mi);
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
                :"not a virtual register!";
        regIdx -= FirstVirtualRegister;
        if (regIdx >= virRegInfo.size())
            if (regIdx < virRegInfo.size() * 2)
                virRegInfo.ensureCapacity(2*regIdx);
            else
                virRegInfo.ensureCapacity(2*virRegInfo.size());

        return virRegInfo.set(regIdx, new VarInfo());
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
            MachineBasicBlock pred = worklist.getLast();
            worklist.removeLast();
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
        // if so, remove it, because there is only one last use instr in
        // the same block.
        for (int i = 0; i < varInfo.lastUsedInst.size();)
        {
            if (varInfo.lastUsedInst.get(i).getParent() == mbb)
            {
                varInfo.lastUsedInst.remove(i);
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
                    ( mi = mbb.getInstAt(i)).getOpCode() == TargetInstrInfo.PHI; i++)
            {
                for (int j = 1, sz = mi.getNumOperands(); j < sz; j++)
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
        TIntArrayList list = registerKilled.get(mi);
        // handle the special common case.
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
        getVarInfo(incomingReg).lastUsedInst.add(mi);
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
        getVarInfo(reg).lastUsedInst.add(mi);
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
}
