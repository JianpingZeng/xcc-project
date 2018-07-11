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

package backend.codegen.linearscan;

import backend.analysis.LiveVariables;
import backend.analysis.MachineDomTree;
import backend.codegen.*;
import backend.pass.AnalysisUsage;
import backend.target.TargetRegisterInfo;
import tools.BitMap;
import tools.Util;

import java.util.*;

import static backend.target.TargetRegisterInfo.isPhysicalRegister;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class LiveIntervalAnalysis extends MachineFunctionPass
{
    public interface InstrSlots
    {
        int LOAD = 0;
        int USE = 1;
        int NUM = 2;
    }

    /**
     * A mapping from instruction number to itself.
     */
    private MachineInstr[] idx2MI;

    /**
     * A mapping from MachineInstr to its number.
     */
    HashMap<MachineInstr, Integer> mi2Idx;

    private TargetRegisterInfo tri;

    TreeMap<Integer, LiveInterval> intervals;
    BitMap[] liveIns;
    BitMap[] liveOuts;

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.addRequired(LiveVariables.class);
        au.addPreserved(LiveVariables.class);

        // Eliminate phi node.
        au.addPreserved(PhiElimination.class);
        au.addRequired(PhiElimination.class);

        // Converts the RISC-like MachineInstr to two addr instruction in some
        // target, for example, X86.
        au.addRequired(TwoAddrInstructionPass.class);

        au.addPreserved(MachineDomTree.class);
        au.addRequired(MachineDomTree.class);

        super.getAnalysisUsage(au);
    }

    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        tri = mf.getTarget().getRegisterInfo();

        int size = mf.getNumBlocks();
        int[] numIncomingBranches = new int[size];
        MachineDomTree dt = (MachineDomTree) getAnalysisToUpDate(MachineDomTree.class);
        Util.assertion(dt != null);

        ArrayList<MachineBasicBlock> mbbs = mf.getBasicBlocks();
        for (int i = 0; i < size; i++)
        {
            MachineBasicBlock mbb = mbbs.get(i);
            int num = mbb.getNumPredecessors();

            for (int j = 0, e = mbb.getNumPredecessors(); j < e; j++)
            {
                if (dt.dominates(mbb, mbb.predAt(j)))
                    num--;
            }

            numIncomingBranches[i] = num;
        }

        // Step #1: compute the block order
        ArrayList<MachineBasicBlock> sequence = new ArrayList<>();

        LinkedList<MachineBasicBlock> worklist = new LinkedList<>();
        worklist.add(mf.getEntryBlock());
        while (!worklist.isEmpty())
        {
            MachineBasicBlock cur = worklist.getFirst();
            worklist.removeFirst();
            sequence.add(cur);

            for (Iterator<MachineBasicBlock> itr = cur.succIterator(); itr.hasNext(); )
            {
                MachineBasicBlock succ = itr.next();
                --numIncomingBranches[succ.getNumber()];
                if (numIncomingBranches[succ.getNumber()] == 0)
                {
                    worklist.add(succ);
                }
            }
        }

        if (Util.DEBUG)
        {
            for (MachineBasicBlock bb : sequence)
            {
                System.err.printf("[%s, %d]\n", bb.getName(), bb.getNumber());
            }
        }

        // Step #2: compute local live set.
        BitMap[] liveGen = new BitMap[size];
        BitMap[] liveKill = new BitMap[size];
        computeLocalLiveSet(sequence, liveGen, liveKill);

        // Step #3: compute global live set.
        liveIns = new BitMap[size];
        liveOuts = new BitMap[size];
        computeGlobalLiveSet(sequence, liveIns, liveOuts, liveGen, liveKill);

        // Step #4: number the machine instruction
        numberMachineInstr(sequence);

        // Step #5: build intervals.
        buildIntervals(sequence, liveOuts);
        return false;
    }

    /**
     * Number the machine instructions in the specified order.
     * @param sequence
     */
    private void numberMachineInstr(ArrayList<MachineBasicBlock> sequence)
    {
        if (sequence == null || sequence.isEmpty())
            return;
        int totalNumMI = 0;
        for (MachineBasicBlock mbb : sequence)
        {
            totalNumMI += mbb.size();
        }

        mi2Idx = new HashMap<>();
        idx2MI = new MachineInstr[totalNumMI];
        int index = 0;
        for (MachineBasicBlock mbb : sequence)
        {
            for (int i = 0, e = mbb.size(); i < e; i++)
            {
                MachineInstr mi = mbb.getInstAt(i);
                mi2Idx.put(mi, index);
                idx2MI[index/InstrSlots.NUM] = mi;
                index += InstrSlots.NUM;
            }
        }
        if (Util.DEBUG)
        {
            System.err.println("******** Number of machine instruction ********");
            for (int i = 0; i < idx2MI.length; i++)
            {
                System.err.printf("%d: ", i*InstrSlots.NUM);
                idx2MI[i].dump();
                System.err.println();
            }
        }
    }

    private void buildIntervals(ArrayList<MachineBasicBlock> sequence, BitMap[] liveOuts)
    {
        intervals = new TreeMap<>();
        for (int i = sequence.size()-1; i>= 0; i--)
        {
            MachineBasicBlock mbb = sequence.get(i);
            Util.assertion(mi2Idx.containsKey(mbb.getFirstInst()));
            Util.assertion(mi2Idx.containsKey(mbb.getLastInst()));
            int blockFrom = mi2Idx.get(mbb.getFirstInst());
            int blockTo = mi2Idx.get(mbb.getLastInst()) + InstrSlots.NUM;
            BitMap map = liveOuts[mbb.getNumber()];
            for (int reg = map.findFirst(); reg >= 0;)
            {
                LiveInterval li;
                if (intervals.containsKey(reg))
                    li = intervals.get(reg);
                else
                {
                    li = new LiveInterval();
                    li.register = reg;
                    intervals.put(reg, li);
                }
                li.addRange(blockFrom, blockTo);
                reg = map.findNext(reg);
            }

            for (int j = mbb.size() - 1; j >= 0; j--)
            {
                MachineInstr mi = mbb.getInstAt(j);
                int num = mi2Idx.get(mi);
                /*
                if (mi.isCall())
                {
                    for (int moIdx = 0, sz = mi.getNumOperands(); moIdx < sz; moIdx++)
                    {
                        if (mi.getOperand(moIdx).isRegister() &&
                                mi.getOperand(moIdx).isUse() &&
                                TargetRegisterInfo.isPhysicalRegister(mi.getOperand(moIdx).getReg()))
                        {
                            int reg = mi.getOperand(moIdx).getReg();
                            LiveInterval li;
                            if (intervals.containsKey(reg))
                                li = intervals.get(reg);
                            else
                            {
                                li = new LiveInterval();
                                intervals.put(reg, li);
                            }
                            li.addRange(num, num+1);
                        }
                    }
                }*/

                ArrayList<MachineOperand> uses = new ArrayList<>();
                ArrayList<MachineOperand> defs = new ArrayList<>();
                for (int moIdx = 0, sz = mi.getNumOperands(); moIdx < sz; moIdx++)
                {
                    MachineOperand mo = mi.getOperand(moIdx);
                    if (mo.isRegister() && mo.getReg() > 0)
                    {
                        if (mo.isDef())
                            defs.add(mo);
                        else if (mo.isUse())
                            uses.add(mo);
                    }
                }
                for (MachineOperand mo : defs)
                {
                    int reg = mo.getReg();
                    LiveInterval li;
                    if (intervals.containsKey(reg))
                        li = intervals.get(reg);
                    else
                    {
                        li = new LiveInterval();
                        li.register = reg;
                        intervals.put(reg, li);
                    }
                    Util.assertion(li != null);
                    if (mo.isImplicit() && isPhysicalRegister(reg))
                    {
                        li.addRange(num, num + 1);
                        li.addUsePoint(num, mo);
                    }
                    else
                    {
                        LiveRange lr = li.getFirst();
                        Util.assertion(lr != null);
                        lr.start = num;
                        li.addUsePoint(num, mo);
                    }
                }
                for (MachineOperand mo : uses)
                {
                    int reg = mo.getReg();
                    LiveInterval li;
                    if (intervals.containsKey(reg))
                        li = intervals.get(reg);
                    else
                    {
                        li = new LiveInterval();
                        li.register = reg;
                        intervals.put(reg, li);
                    }
                    Util.assertion(li != null);

                    li.addRange(blockFrom, num);
                    li.addUsePoint(num, mo);
                }
            }
        }

        if (Util.DEBUG)
        {
            for (LiveInterval interval : intervals.values())
                interval.dump(tri);
        }
    }

    /**
     * Compute local live set for each basic block according to classical
     * dataflow algortihm.
     * @param sequence
     * @param liveGen
     * @param liveKill
     */
    private void computeLocalLiveSet(ArrayList<MachineBasicBlock> sequence,
            BitMap[] liveGen, BitMap[] liveKill)
    {
        for (MachineBasicBlock bb : sequence)
        {
            liveGen[bb.getNumber()] = new BitMap();
            liveKill[bb.getNumber()] = new BitMap();

            for (int i = bb.size()-1; i >= 0; --i)
            {
                MachineInstr mi = bb.getInstAt(i);
                for (int j = mi.getNumOperands()-1; j >= 0; j--)
                {
                    MachineOperand mo = mi.getOperand(j);
                    if (!mo.isRegister())
                        continue;
                    int reg = mo.getReg();
                    if (mo.isUse())
                    {
                        if (!liveKill[bb.getNumber()].get(reg))
                            liveGen[bb.getNumber()].set(reg);
                    }
                    else if (mo.isDef())
                    {
                        liveKill[bb.getNumber()].set(reg);
                    }
                }
            }
        }
    }

    /**
     * Compute LiveIn and LiveOut set for each machine basic block using
     * iterative algorithm operated on machine basic blocks in reverse order.
     * @param sequence
     * @param liveIns
     * @param liveOuts
     */
    private void computeGlobalLiveSet(ArrayList<MachineBasicBlock> sequence,
            BitMap[] liveIns, BitMap[] liveOuts,
            BitMap[] liveGens, BitMap[] liveKills)
    {
        for (MachineBasicBlock mbb : sequence)
        {
            liveIns[mbb.getNumber()] = new BitMap();
            liveOuts[mbb.getNumber()] = new BitMap();
        }
        boolean changed;
        do
        {
            changed = false;
            for (int i = sequence.size()-1; i >= 0; --i)
            {
                MachineBasicBlock mbb = sequence.get(i);
                int num = mbb.getNumber();
                BitMap out = new BitMap();

                if (!mbb.succIsEmpty())
                {
                    for (MachineBasicBlock succ : mbb.getSuccessors())
                        out.and(liveIns[succ.getNumber()]);
                }
                out.and(liveOuts[num]);
                changed = !out.equals(liveOuts[num]);
                if (changed) liveOuts[num] = out;

                BitMap in = liveOuts[num].clone();
                in.diff(liveKills[num]);
                in.and(liveGens[num]);
                changed = !in.equals(liveIns[num]);
                if (changed) liveIns[num] = in;
            }
        }while (changed);
    }

    public MachineBasicBlock getBlockAtId(int pos)
    {
        int index = pos / InstrSlots.NUM;
        Util.assertion(index >= 0 && index < idx2MI.length);
        return idx2MI[index].getParent();
    }

    public boolean isBlockBegin(int pos)
    {
        int id = pos / InstrSlots.NUM;
        Util.assertion(id >= 0 && id < idx2MI.length);
        return idx2MI[id].equals(idx2MI[id].getParent().getFirstInst());
    }

    public int getIndex(int id)
    {
        return id / LiveIntervalAnalysis.InstrSlots.NUM;
    }

    @Override
    public String getPassName()
    {
        return "Computing live set for each virtual register";
    }
}
