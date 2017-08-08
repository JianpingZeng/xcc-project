package backend.transform.scalars;

import backend.analysis.LiveVariables;
import backend.analysis.MachineDomTreeInfo;
import backend.analysis.MachineLoopInfo;
import backend.codegen.*;
import backend.codegen.MachineRegisterInfo.DefUseChainIterator;
import backend.pass.AnalysisUsage;
import backend.support.IntStatistic;
import backend.target.*;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import tools.BitMap;
import tools.OutParamWrapper;

import static backend.target.TargetOperandInfo.OperandConstraint.TIED_TO;
import static backend.target.TargetRegisterInfo.isVirtualRegister;

/**
 * <pre>
 * This file implements the TwoAddress instruction pass which is used
 * by most register allocators. Two-Address instructions are rewritten
 * from:
 *
 *     A = B op C
 *
 * to:
 *
 *     A = B
 *     A op= C
 * </pre>
 * <p>
 * Note that if a register allocator chooses to use this pass, that it
 * has to be capable of handling the non-SSA nature of these rewritten
 * virtual registers.
 * </p>
 * <p>
 * It is also worth noting that the duplicate operand of the two
 * address instruction is removed.
 * </p>
 * @author Xlous.zeng
 * @version 0.1
 */
public final class TwoAddrInstructionPass extends MachineFunctionPass
{
    public static final IntStatistic NumTwoAddressInsts = new IntStatistic(
            "NumTwoAddressInsts", "Number of two address instructions");
    public static final IntStatistic NumCommuted = new IntStatistic(
            "NumCommuted", "Number of instructions commuted to coalese");
    public static final IntStatistic NumAggrCommuted = new IntStatistic(
            "NumAggrCommuted", "Number of instruction aggressively commuted");
    public static final IntStatistic NumConvertedTo3Addr = new IntStatistic(
            "NumConvertedTo3Addr", "    Number of instructions promoted to 3-address");
    public static final IntStatistic Num3AddrSunk = new IntStatistic(
            "Num3AddrSunk", "Number of 3 address sunk");
    public static final IntStatistic NumReMats = new IntStatistic(
            "NumReMats", "Number of instructions rematerialized");

    private TargetInstrInfo tii;
    private TargetRegisterInfo tri;
    private MachineRegisterInfo mri;
    private LiveVariables lv;

    @Override
    public String getPassName()
    {
        return "Two addr instruction pass";
    }

    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        System.err.println("Machine Function");
        TargetMachine tm = mf.getTarget();
        mri = mf.getMachineRegisterInfo();
        tii = tm.getInstrInfo();
        tri = tm.getRegisterInfo();
        lv = getAnalysisToUpDate(LiveVariables.class);

        boolean madeChange = false;
        System.err.println("******* Rewriting two-addr instrs ********");
        System.err.printf("******** Function: %s\n", mf.getFunction().getName());

        // Keep track of the registers whose def's are remat'ed.
        BitMap reMatRegs = new BitMap(mri.getLastVirReg() + 1);

        // Keep track the distance of a MI from the start of the
        // current basic block.
        TObjectIntHashMap<MachineInstr> distanceMap = new TObjectIntHashMap<>();
        for (MachineBasicBlock mbb : mf.getBasicBlocks())
        {
            for (int i = 0, e = mbb.size(); i < e; i++)
            {
                MachineInstr mi = mbb.getInstAt(i);
                MachineInstr nmi = i < e ? mbb.getInstAt(i + 1) : null;
                TargetInstrDesc tid = mi.getDesc();
                boolean firstTied = true;

                distanceMap.put(mi, i + 1);
                for (int si = 1, sz = tid.getNumOperands(); si != sz; ++si)
                {
                    int ti = tid.getOperandConstraint(si, TIED_TO);
                    if (ti == -1)
                        continue;

                    if (firstTied)
                    {
                        NumTwoAddressInsts.inc();
                        System.err.print("\t");
                        mi.print(System.err, tm);
                    }

                    firstTied = false;

                    assert mi.getOperand(si).isRegister()
                            && mi.getOperand(si).getReg() != 0 && mi.getOperand(si)
                            .isUse() : "two address instruction invalid";

                    // If the two operands are the same we just remove the use
                    // and mark the def as def&use, otherwise we have to insert a copy.
                    if (mi.getOperand(ti).getReg() != mi.getOperand(si).getReg())
                    {
                        // Rewrite:
                        //     a = b op c
                        // to:
                        //     a = b
                        //     a = a op c
                        int regA = mi.getOperand(ti).getReg();
                        int regB = mi.getOperand(si).getReg();
                        int regASubIdx = mi.getOperand(ti).getSubReg();

                        assert isVirtualRegister(regA) && isVirtualRegister(
                                regB) : "cannot update physical register live information";

                        InstructionRearranged:
                        {
                            if (!mi.killsRegister(regB))
                            {
                                if (tid.isCommutable() && mi.getNumOperands() >= 3)
                                {
                                    assert mi.getOperand(3 - si).isRegister() : "Not a proper commutative instruction";
                                    int regC = mi.getOperand(3 - si).getReg();
                                    if (mi.killsRegister(regC))
                                    {
                                        if (commuteInstruction(mi, mbb, regC, i,
                                                distanceMap))
                                        {
                                            NumCommuted.inc();
                                            regB = regC;
                                            break InstructionRearranged;
                                        }
                                    }
                                }

                                // If this instruction is potentially convertible to
                                // a true three-address instruction.
                                if (tid.isConvertibleTo3Addr())
                                {
                                    for (int j = si + 1, numOps = tid.getNumOperands();
                                         j != numOps; ++numOps)
                                    {
                                        assert tid.getOperandConstraint(j,
                                                TIED_TO) == -1;
                                    }

                                    MachineInstr newMI = tii
                                            .convertToThreeAddress(mbb, i, lv);
                                    if (newMI != null)
                                    {
                                        System.err.print("2addr: CONVERTING 2-ADDR: ");
                                        mi.print(System.err, null);
                                        System.err.print("2addr:            3-ADDR: ");
                                        newMI.print(System.err, null);
                                        boolean sunk = false;

                                        if (newMI.findRegisterUseOperand(regB,
                                                false, tri) != null)
                                        {
                                            sunk = sink3AddrInstruction(mbb,
                                                    newMI, regB, mi);
                                        }

                                        mbb.remove(mi); // Nuke the old mi.

                                        if (!sunk)
                                        {
                                            distanceMap.put(newMI, i + 1);
                                            mi = newMI;
                                            nmi = next(mi);
                                        }

                                        NumConvertedTo3Addr.inc();
                                        break;
                                    }
                                }
                            }

                            if (tid.isCommutable() && mi.getNumOperands() >= 3)
                            {
                                int regC = mi.getOperand(3 - si).getReg();
                                if (isProfitableToCommute(regB, regC, mi, mbb,
                                        i + 1, distanceMap))
                                {
                                    if (commuteInstruction(mi, mbb, regC, i + 1,
                                            distanceMap))
                                    {
                                        NumAggrCommuted.inc();
                                        NumCommuted.inc();
                                        regB = regC;
                                    }
                                }
                            }
                        }

                        TargetRegisterClass rc = mri.getRegClass(regA);
                        MachineInstr defMI = mri.getVRegDef(regB);

                        if (defMI != null && defMI.getDesc().isAsCheapAsAMove()
                                && defMI.isSafeToReMat(tii, regB)
                                && isProfitableToReMat(regB, rc, mi, defMI, mbb,
                                i + 1, distanceMap))
                        {
                            System.err.printf("2addr: Rematting: ");
                            defMI.print(System.err, null);
                            System.err.print("\n");
                            tii.reMaterialize(mbb, i, regA, regASubIdx, defMI);
                            reMatRegs.set(regB);
                            NumReMats.inc();
                        }
                        else
                        {
                            tii.copyRegToReg(mbb, i, regA, regB, rc, rc);
                        }

                        int prevMI = prior(mi);
                        distanceMap.put(mbb.getInstAt(prevMI), i+1);
                        distanceMap.put(mi, i+1);

                        if (lv != null)
                        {
                            if (lv.removeVirtualRegisterKilled(regB, mi))
                                lv.addVirtualRegisterKilled(regB, mbb.getInstAt(prevMI));
                            if (lv.removeVirtualRegisterDead(regB, mi))
                                lv.addVirtualRegisterDead(regB, mbb.getInstAt(prevMI));
                        }

                        System.err.print("\t\tprepend:\t");
                        mbb.getInstAt(prevMI).print(System.err, null);

                        for (int j = 0, numOps = mi.getNumOperands(); j != numOps; j++)
                        {
                            if (mi.getOperand(j).isRegister() && mi.getOperand(j).getReg() == regB)
                                mi.getOperand(j).setReg(regA);
                        }
                    }

                    assert mi.getOperand(ti).isDef() && mi.getOperand(si).isUse();
                    mi.getOperand(ti).setReg(mi.getOperand(si).getReg());
                    madeChange = true;

                    System.err.print("\t\trewrite to:\t");
                    mi.print(System.err, null);
                }

                mi = nmi;
            }
        }

        int vreg = reMatRegs.findFirst();
        while (vreg != -1)
        {
            if(!mri.hasUseOperand(vreg))
            {
                MachineInstr defMI = mri.getVRegDef(vreg);
                defMI.removeFromParent();
            }
            vreg = reMatRegs.findNext(vreg);
        }
        return madeChange;
    }

    private static boolean isTwoAddrUse(MachineInstr useMI, int reg)
    {
        TargetInstrDesc tid = useMI.getDesc();
        for (int i = 0, e = tid.getNumOperands(); i != e; i++)
        {
            MachineOperand mo = useMI.getOperand(i);
            if (mo.isRegister() && mo.getReg() == reg &&
                    (mo.isDef() || tid.getOperandConstraint(i, TIED_TO) != -1))
                return true;
        }
        return false;
    }

    private boolean sink3AddrInstruction(
            MachineBasicBlock mbb,
            MachineInstr mi,
            int savedReg,
            MachineInstr oldMI)
    {
        OutParamWrapper<Boolean> x = new OutParamWrapper<>(true);
        if (!mi.isSafeToMove(tii, x))
            return false;
        boolean seenStore = x.get();

        int defReg = 0;
        TIntHashSet useRegs = new TIntHashSet();

        for(int i = 0, e = mi.getNumOperands(); i < e; i++)
        {
            MachineOperand mo = mi.getOperand(i);
            if (!mo.isRegister())
                continue;
            int moReg = mo.getReg();
            if (moReg == 0)
                continue;

            if (mo.isUse() && moReg != savedReg)
                useRegs.add(moReg);
            if (!mo.isDef())
                continue;
            if (mo.isImplicit())
                return false;

            if (defReg != 0)
                return false;
            defReg = moReg;
        }

        MachineInstr killMI = null;
        for (DefUseChainIterator itr = mri.getUseIterator(savedReg); itr.hasNext(); )
        {
            MachineOperand useMO = itr.getOpearnd();
            if (!useMO.isKill())
                continue;
            killMI = useMO.getParentMI();
            break;
        }

        if (killMI == null || !killMI.getParent().equals(mbb))
            return false;

        MachineOperand killMO = null;
        int killPos = killMI.index();
        killPos++;

        int numVisited = 0;
        for (int i = oldMI.index()+1; i != killPos; i++)
        {
            MachineInstr otherMI = mbb.getInstAt(i);
            if (numVisited > 30)
                return false;

            ++numVisited;
            for (int j = 0,e = otherMI.getNumOperands(); j != e; j++)
            {
                MachineOperand mo = otherMI.getOperand(j);
                if (!mo.isRegister())
                    continue;
                int moreg = mo.getReg();
                if (moreg == 0)
                    continue;
                if (defReg == moreg)
                    return false;

                if (mo.isKill())
                {
                    if (otherMI.equals(killMI) && moreg == savedReg)
                    {
                        killMO = mo;
                    }
                    else if (useRegs.contains(moreg))
                        return false;
                }
            }
        }

        killMO.setIsKill(false);
        killMO = mi.findRegisterUseOperand(savedReg, false, tri);
        killMO.setIsKill(true);

        if (lv != null)
            lv.replaceKillInstruction(savedReg, killMI, mi);

        mbb.remove(mi);
        mbb.insert(killPos, mi);
        return true;
    }

    private boolean commuteInstruction(
            OutParamWrapper<MachineInstr> mi,
            MachineBasicBlock mbb,
            int regC,
            int dist,
            TObjectIntHashMap<MachineInstr> distanceMap)
    {
        MachineInstr newMI = tii.commuteInstruction(mi.get());

        if (newMI== null)
        {
            return false;
        }

        if (!newMI.equals(mi))
        {
            if (lv != null)
                lv.replaceKillInstruction(regC, mi.get(), newMI);

            mbb.insert(mi.get().index(), newMI);
            mbb.remove(mi.get());
            mi.set(newMI);
            distanceMap.put(newMI, dist);
        }

        return true;
    }

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.addPreserved(LiveVariables.class);
        au.addPreserved(MachineLoopInfo.class);
        au.addPreserved(MachineDomTreeInfo.class);
        au.addRequired(PhiElimination.class);
        super.getAnalysisUsage(au);
    }


    private boolean isProfitableToCommute(int regB,
            int regC,
            MachineInstr mi,
            MachineBasicBlock mbb,
            int dist,
            TObjectIntHashMap<MachineInstr> distanceMap)
    {
        // Determine if it's profitable to commute this two address instruction. In
        // general, we want no uses between this instruction and the definition of
        // the two-address register.
        // e.g.
        // %reg1028<def> = EXTRACT_SUBREG %reg1027<kill>, 1
        // %reg1029<def> = MOV8rr %reg1028
        // %reg1029<def> = SHR8ri %reg1029, 7, %EFLAGS<imp-def,dead>
        // insert => %reg1030<def> = MOV8rr %reg1028
        // %reg1030<def> = ADD8rr %reg1028<kill>, %reg1029<kill>, %EFLAGS<imp-def,dead>
        // In this case, it might not be possible to coalesce the second MOV8rr
        // instruction if the first one is coalesced. So it would be profitable to
        // commute it:
        // %reg1028<def> = EXTRACT_SUBREG %reg1027<kill>, 1
        // %reg1029<def> = MOV8rr %reg1028
        // %reg1029<def> = SHR8ri %reg1029, 7, %EFLAGS<imp-def,dead>
        // insert => %reg1030<def> = MOV8rr %reg1029
        // %reg1030<def> = ADD8rr %reg1029<kill>, %reg1028<kill>, %EFLAGS<imp-def,dead>
        if (!mi.killsRegister(regC))
            return false;

        OutParamWrapper<Integer> x = new OutParamWrapper<>(0);
        if (notUseAfterLastDef(regC, mbb, dist, distanceMap, x))
            return false;

        int lastDefC = x.get();

        x.set(0);
        if (notUseAfterLastDef(regB, mbb, dist, distanceMap, x))
            return false;
        int lastDefB = x.get();

        return lastDefB !=0 && lastDefC != 0 && lastDefC > lastDefB;
    }

    private boolean notUseAfterLastDef(int reg,
            MachineBasicBlock mbb,
            int dist,
            TObjectIntHashMap<MachineInstr> distanceMap,
            OutParamWrapper<Integer> lastDef)
    {
        lastDef.set(0);
        int lastUse = dist;
        for (DefUseChainIterator itr = mri.getRegIterator(reg); itr.hasNext();)
        {
            MachineOperand mo = itr.getOpearnd();
            MachineInstr mi = itr.getMachineInstr();
            if (!mi.getParent().equals(mbb))
                continue;
            if (!distanceMap.containsKey(mi))
                continue;

            if (mo.isUse() && distanceMap.get(mi) < lastUse)
                lastUse = distanceMap.get(mi);
            if (mo.isDef() && distanceMap.get(mi) > lastDef.get())
                lastDef.set(distanceMap.get(mi));
            itr.next();
        }
        return (!(lastUse > lastDef.get() && lastUse < dist));
    }

    private boolean isProfitableToReMat(int reg,
            TargetRegisterClass rc,
            MachineInstr mi, MachineInstr defMI,
            MachineBasicBlock mbb,
            int loc,
            TObjectIntHashMap<MachineInstr> distanceMap)
    {
        boolean otherUse = false;
        for (DefUseChainIterator itr = mri.getUseIterator(reg); itr.hasNext(); )
        {
            MachineOperand useMO = itr.getOpearnd();
            MachineInstr useMI = itr.getMachineInstr();
            MachineBasicBlock useBB = useMI.getParent();
            if (useBB.equals(mbb))
            {
                if (distanceMap.containsKey(useMI) && distanceMap.get(useMI) == loc)
                {
                    otherUse = true;
                    if (isTwoAddrUse(useMI, reg))
                        return true;
                }
            }
        }

        if (otherUse)
            return false;

        return mbb.equals(defMI.getParent());
    }
}
