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

package backend.codegen;

import tools.Util;
import backend.analysis.LiveVariables;
import backend.analysis.MachineDomTree;
import backend.analysis.MachineLoop;
import backend.codegen.MachineRegisterInfo.DefUseChainIterator;
import backend.pass.AnalysisUsage;
import backend.support.IntStatistic;
import backend.target.*;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import tools.BitMap;
import tools.OutParamWrapper;

import static backend.codegen.PrintMachineFunctionPass.createMachineFunctionPrinterPass;
import static backend.target.TargetInstrInfo.INLINEASM;
import static backend.target.TargetOperandInfo.OperandConstraint.TIED_TO;
import static backend.target.TargetRegisterInfo.isPhysicalRegister;
import static backend.target.TargetRegisterInfo.isVirtualRegister;

/**
 * <pre>
 * This file implements the TwoAddress instruction pass which is used
 * by most register allocators. Two-Address instructions are rewritten
 * from:
 *     A = B op C
 *
 * to:
 *     B op= C
 * </pre>
 * Then replace all uses of A with B below original 3-addr instr(A = B op C).
 * This is a optimization trick in contrast to LLVM's implementation.
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
        return "Two address instruction pass";
    }

    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        TargetMachine tm = mf.getTarget();
        mri = mf.getMachineRegisterInfo();
        tii = tm.getInstrInfo();
        tri = tm.getRegisterInfo();
        lv = (LiveVariables) getAnalysisToUpDate(LiveVariables.class);

        boolean madeChange = false;
        if (Util.DEBUG)
        {
            System.err.println("******* Rewriting two-addr instrs ********");
            System.err.printf("******** Function: %s\n", mf.getFunction().getName());
        }

        // Keep track of the registers whose def's are rematerialized.
        BitMap reMatRegs = new BitMap(mri.getLastVirReg() + 1);

        // Keep track the distance of a MI from the start of the
        // current basic block.
        TIntIntHashMap distanceMap = new TIntIntHashMap();
        for (MachineBasicBlock mbb : mf.getBasicBlocks())
        {
            distanceMap.clear();
            for (int i = 0, e = mbb.size(); i < e; )
            {
                MachineInstr mi = mbb.getInstAt(i);
                int nmi = i+1;
                TargetInstrDesc tid = mbb.getInstAt(i).getDesc();
                boolean firstTied = true;

                distanceMap.put(i, i + 1);
                int sz = mi.getOpcode() == INLINEASM ?
                        tid.getNumOperands() : mi.getNumOperands();
                // Walk through operand but skips defined operand.
                for (int si = 1; si < sz; ++si)
                {
                    MachineOperand op = mi.getOperand(si);
                    if (!op.isRegister() || op.getReg() == 0 || !op.isUse())
                        continue;

                    OutParamWrapper<Integer> tmp = new OutParamWrapper<>(0);
                    if (!mi.isRegTiedToDefOperand(si, tmp))
                        continue;

                    int ti = tmp.get();
                    if (!mi.getOperand(ti).isRegister() || mi.getOperand(ti).getReg() == 0)
                        continue;
                    if (isPhysicalRegister(mi.getOperand(ti).getReg()))
                        continue;

                    if (firstTied)
                    {
                        NumTwoAddressInsts.inc();
                        if (Util.DEBUG)
                        {
                            System.err.print("\t");
                            mi.print(System.err, tm);
                        }
                    }

                    firstTied = false;

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

                        Util.assertion(isVirtualRegister(regA) && isVirtualRegister(regB),
                                "cannot update physical register live information");

                        InstructionRearranged:
                        {
                            if (!mi.killsRegister(regB))
                            {
                                if (tid.isCommutable() && mbb.getInstAt(i).getNumOperands() >= 3)
                                {
                                    Util.assertion(mbb.getInstAt(i).getOperand(3 - si).isRegister(),  "Not a proper commutative instruction");

                                    int regC = mbb.getInstAt(i).getOperand(3 - si).getReg();
                                    if (mbb.getInstAt(i).killsRegister(regC))
                                    {
                                        OutParamWrapper<Integer> arg = new OutParamWrapper<>(i);
                                        boolean res = commuteInstruction(arg, mbb, regC, i,
                                                distanceMap);
                                        i = arg.get();
                                        if (res)
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
                                    if (Util.DEBUG)
                                    {
                                        for (int j = si + 1, numOps = tid.getNumOperands(); j != numOps; ++j)
                                            Util.assertion( tid.getOperandConstraint(j,TIED_TO) == -1);
                                    }

                                    MachineInstr oldMI = mbb.getInstAt(i);
                                    MachineInstr newMI = tii.convertToThreeAddress(mbb, i, lv);
                                    if (newMI != null)
                                    {
                                        if (Util.DEBUG)
                                        {
                                            System.err.print("2addr: CONVERTING 2-ADDR: ");
                                            oldMI.print(System.err, null);
                                            System.err.print("2addr:            3-ADDR: ");
                                            newMI.print(System.err, null);
                                        }
                                        boolean sunk = false;

                                        if (newMI.findRegisterUseOperand(regB,
                                                false, tri) != null)
                                        {
                                            sunk = sink3AddrInstruction(mbb,
                                                    newMI, regB, i+1);
                                        }
                                        oldMI.removeFromParent();

                                        mri.replaceDefRegInfo(regA, oldMI, newMI);
                                        if (!sunk)
                                        {
                                            distanceMap.put(newMI.index(), i + 1);
                                            i = newMI.index();
                                            nmi = i+1;
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
                                    OutParamWrapper<Integer> arg = new OutParamWrapper<>(i);
                                    boolean res = commuteInstruction(arg, mbb, regC, i + 1,
                                            distanceMap);
                                    i = arg.get();
                                    if (res)
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
                                && isProfitableToReMat(regB, rc, mbb.getInstAt(i), defMI, mbb,
                                i + 1, distanceMap))
                        {
                            if (Util.DEBUG)
                            {
                                System.err.printf("2addr: Rematting: ");
                                defMI.print(System.err, null);
                                System.err.print("\n");
                            }
                            tii.reMaterialize(mbb, i, regA, regASubIdx, defMI);
                            reMatRegs.set(regB);
                            NumReMats.inc();
                        }
                        else
                        {
                            tii.copyRegToReg(mbb, i, regA, regB, rc, rc);

                            // Uses another method to reduce 3-addr instruction
                            // to 2-addr instruction.
                            // Replace all of using of destination register(ti)
                            // with si register
                            //int srcReg = mi.getOperand(si).getReg();
                            //int destReg = mi.getOperand(ti).getReg();
                            // Step#1, unlink this machine operand for it's SSA chain
                            //mi.getOperand(ti).removeRegOperandFromRegInfo();
                            //mi.getOperand(ti).setReg(srcReg);
                            // Step#2, link this with another chain corresponding to
                            // srcReg.
                            //mi.getOperand(ti).addRegOperandToRegInfo(mri);
                            /*
                            DefUseChainIterator itr = mri.getUseIterator(destReg);
                            while (itr.hasNext())
                            {
                                //itr.getOpearnd().removeRegOperandFromRegInfo();
                                itr.getOpearnd().setReg(srcReg);

                                //itr.getOpearnd().addRegOperandToRegInfo(mri);
                                itr.next();
                            }*/
                        }


                        // After insert a copy instruction, the index of current
                        // mi is i+1, inserted instr is i.
                        MachineInstr prevMI = mbb.getInstAt(i);
                        distanceMap.put(i, i);
                        distanceMap.put(i+1, i+1);

                        // the nmi should point to the next instr to be fetched.
                        // i    ---> inserted copy instr
                        // i+1  ---> current mi
                        // i+2  ---> the next instr to be handled.
                        nmi = i+2;

                        if (lv != null)
                        {
                            if (lv.removeVirtualRegisterKilled(regB, mi))
                                lv.addVirtualRegisterKilled(regB, prevMI);
                            if (lv.removeVirtualRegisterDead(regB, mi))
                                lv.addVirtualRegisterDead(regB, prevMI);

                            lv.getVarInfo(regA).defInst = prevMI;
                        }

                        if (Util.DEBUG)
                        {
                            System.err.print("\t\tprepend:\t");
                            prevMI.print(System.err, null);
                        }

                        // Update the machine operand of this MI, replace regB with
                        // regA
                        for (int j = 0, numOps = mi.getNumOperands(); j != numOps; j++)
                        {
                            if (mi.getOperand(j).isRegister() && mi.getOperand(j).getReg() == regB)
                                mi.getOperand(j).setReg(regA);
                        }
                    }
                    Util.assertion( mi.getOperand(ti).isDef() && mi.getOperand(si).isUse());
                    mi.getOperand(ti).setReg(mi.getOperand(si).getReg());
                    madeChange = true;

                    if (Util.DEBUG)
                    {
                        System.err.print("\t\trewrite to:\t");
                        mbb.getInstAt(i).print(System.err, null);
                    }
                    break;
                }

                i = nmi;
            }
        }

        int vreg = reMatRegs.findFirst();
        while (vreg != -1)
        {
            if(!mri.hasUseOperand(vreg))
            {
                MachineInstr defMI = mri.getVRegDef(vreg);
                if (defMI != null)
                    defMI.removeFromParent();
            }
            vreg = reMatRegs.findNext(vreg);
        }

        if (TargetOptions.PrintMachineCode.value)
        {
            PrintMachineFunctionPass printer = createMachineFunctionPrinterPass(
                    System.err,
                    "# *** IR dump after 2-Addr instruction Pass ***:\n");
            printer.runOnMachineFunction(mf);
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
            int oldMI)
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
            itr.next();
            if (!useMO.isKill())
                continue;
            killMI = useMO.getParent();
            break;
        }

        if (killMI == null || !killMI.getParent().equals(mbb))
            return false;

        MachineOperand killMO = null;
        int killPos = killMI.index();
        killPos++;

        int numVisited = 0;
        for (int i = oldMI+1; i != killPos; i++)
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
        Util.assertion(killMO != null);
        killMO.setIsKill(false);
        killMO = mi.findRegisterUseOperand(savedReg, false, tri);
        killMO.setIsKill(true);

        if (lv != null)
            lv.replaceKillInstruction(savedReg, killMI, mi);

        if (killPos == mi.index())
            return false;

        Util.assertion(killPos > mi.index(), "must sink the specified instr");
        mi.removeFromParent();
        mbb.insert(killPos-1, mi);
        return true;
    }

    private boolean commuteInstruction(
            OutParamWrapper<Integer> mi,
            MachineBasicBlock mbb,
            int regC,
            int dist,
            TIntIntHashMap distanceMap)
    {
        MachineInstr newMI = tii.commuteInstruction(mbb.getInstAt(mi.get()));

        if (newMI== null)
        {
            return false;
        }

        if (!newMI.equals(mbb.getInstAt(mi.get())))
        {
            if (lv != null)
                lv.replaceKillInstruction(regC, mbb.getInstAt(mi.get()), newMI);

            mbb.insert(mi.get(), newMI);
            mbb.remove(mi.get());
            mi.set(newMI.index());
            distanceMap.put(newMI.index(), dist);
        }

        return true;
    }

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        Util.assertion( au != null);
        au.setPreservesCFG();
        au.addPreserved(LiveVariables.class);
        au.addPreserved(MachineLoop.class);
        au.addPreserved(MachineDomTree.class);
        au.addPreserved(PhiElimination.class);
        super.getAnalysisUsage(au);
    }


    private boolean isProfitableToCommute(int regB,
            int regC,
            MachineInstr mi,
            MachineBasicBlock mbb,
            int dist,
            TIntIntHashMap distanceMap)
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
            TIntIntHashMap distanceMap,
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
            if (!distanceMap.containsKey(mi.index()))
                continue;

            if (mo.isUse() && distanceMap.get(mi.index()) < lastUse)
                lastUse = distanceMap.get(mi.index());
            if (mo.isDef() && distanceMap.get(mi.index()) > lastDef.get())
                lastDef.set(distanceMap.get(mi.index()));
            itr.next();
        }
        return (!(lastUse > lastDef.get() && lastUse < dist));
    }

    private boolean isProfitableToReMat(int reg,
            TargetRegisterClass rc,
            MachineInstr mi,
            MachineInstr defMI,
            MachineBasicBlock mbb,
            int loc,
            TIntIntHashMap distanceMap)
    {
        boolean otherUse = false;
        for (DefUseChainIterator itr = mri.getUseIterator(reg); itr.hasNext(); )
        {
            MachineOperand useMO = itr.getOpearnd();
            MachineInstr useMI = itr.getMachineInstr();
            MachineBasicBlock useBB = useMI.getParent();
            if (useBB.equals(mbb))
            {
                int idx = useMI.index();
                if (distanceMap.containsKey(idx) && distanceMap.get(idx) == loc)
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
