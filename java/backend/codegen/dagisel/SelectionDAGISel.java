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

import backend.analysis.aa.AliasAnalysis;
import backend.codegen.*;
import backend.codegen.dagisel.SDNode.LabelSDNode;
import backend.codegen.fastISel.ISD;
import backend.pass.AnalysisUsage;
import backend.support.Attribute;
import backend.target.*;
import backend.target.TargetMachine.CodeGenOpt;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.*;
import backend.value.Instruction.AllocaInst;
import backend.value.Instruction.PhiNode;
import backend.value.Instruction.TerminatorInst;
import tools.APInt;
import tools.Pair;
import tools.Util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

import static backend.codegen.dagisel.FunctionLoweringInfo.computeValueVTs;
import static backend.codegen.dagisel.RegsForValue.getCopyFromParts;
import static backend.support.BackendCmdOptions.createScheduler;
import static backend.support.ErrorHandling.llvmReportError;
import static backend.target.TargetOptions.ViewDAGAfterSched;
import static backend.target.TargetOptions.ViewDAGBeforeISel;
import static backend.target.TargetOptions.ViewDAGBeforeSched;

/**
 * This class defined here attempts to implement the conversion from LLVM IR -> DAG -> target-specific DAG
 * -> Machine Instr, scoping on basic block. Conversely, the original implementation of LLVM would perform
 * Selection Scheduling based on local DAG before generating machine instr to facilitate pipeline provided
 * by target platform. This work might be added in the future if possible.
 *
 * The working flow of this Instruction Selection based on DAG covering as follows.
 * <pre>
 *     LLVM IR (scoping in basic block instead of global function)
 *        |  done by class SelectionDAGLowering
 *        v
 *  target-independent DAG
 *        | done by X86SelectionDAGISel or other ISel specified by concrete target machine, e.g. ARM, C-SKY etc.
 *        v
 *  target-specific DAG
 *        | Instruction Emitter
 *        v
 *  Machine instruction
 * </pre>
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class SelectionDAGISel extends MachineFunctionPass
{
    protected SelectionDAG curDAG;
    protected TargetLowering targetLowering;
    protected FunctionLoweringInfo funcInfo;
    protected SelectionDAGLowering sdl;
    protected int dagSize;
    protected CodeGenOpt optLevel;
    protected MachineFunction mf;
    protected MachineBasicBlock mbb;
    protected TargetMachine tm;
    protected AliasAnalysis aa;

    public SelectionDAGISel(TargetMachine tm, CodeGenOpt optLevel)
    {
        this.tm = tm;
        targetLowering = tm.getTargetLowering();
        funcInfo = new FunctionLoweringInfo(targetLowering);
        curDAG = new SelectionDAG(targetLowering, funcInfo);
        sdl = new SelectionDAGLowering(curDAG, targetLowering, funcInfo, optLevel);
        dagSize = 0;
        this.optLevel = optLevel;
    }

    public TargetMachine getTargetMachine()
    {
        return tm;
    }
    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        //au.addRequired(AliasAnalysis.class);
        //au.addPreserved(AliasAnalysis.class);
        au.addRequired(MachineModuleInfo.class);
        au.addPreserved(MachineModuleInfo.class);
        super.getAnalysisUsage(au);
    }

    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        this.mf = mf;
        Function f = mf.getFunction();
        //AliasAnalysis aa = (AliasAnalysis) getAnalysisToUpDate(AliasAnalysis.class);
        MachineModuleInfo mmi = (MachineModuleInfo) getAnalysisToUpDate(MachineModuleInfo.class);

        curDAG.init(mf, mmi);
        funcInfo.set(f, mf);
        sdl.init(aa);

        TargetInstrInfo tii = mf.getTarget().getInstrInfo();
        MachineRegisterInfo regInfo = mf.getMachineRegisterInfo();

        selectionAllBasicBlocks(f, mf, tii);
        emitLiveInCopies(mf, tii, regInfo);

        for (Pair<Integer, Integer> reg : regInfo.getLiveIns())
        {
            mf.getEntryBlock().addLiveIn(reg.first);
        }
        funcInfo.clear();
        return true;
    }

    private void emitLiveInCopies(MachineFunction mf, TargetInstrInfo tii, MachineRegisterInfo mri)
    {
        MachineBasicBlock entryBB = mf.getEntryBlock();
        for (Pair<Integer, Integer> regs : mri.getLiveIns())
        {
            if (regs.second != 0)
            {
                TargetRegisterClass rc = mri.getRegClass(regs.second);
                boolean emitted = tii.copyRegToReg(entryBB, 0, regs.second, regs.first, rc, rc);
                assert emitted:"Fail to emit a copy of live-in register!";
            }
        }
    }

    private void selectionAllBasicBlocks(Function fn, MachineFunction mf, TargetInstrInfo tii)
    {
        // Iterate over all basic blocks in the function.
        for (BasicBlock llvmBB : fn.getBasicBlockList())
        {
            // First, clear the locaValueMap.
            mbb = funcInfo.mbbmap.get(llvmBB);

            int bi = 0, end = llvmBB.size();

            // Lower any arguments needed in this block if this is entry block.
            if (llvmBB.equals(fn.getEntryBlock()))
            {
                boolean fail = lowerArguments(llvmBB);
                if (fail) Util.shouldNotReachHere("Fail to lower argument!");
            }

            // Do FastISel on as many instructions as possible.
            if (Util.DEBUG)
            {
                System.err.println("========Instructions============");
            }

            sdl.setCurrentBasicBlock(mbb);

            for (; bi != end && !sdl.hasTailCall(); ++bi)
            {
                Instruction inst = llvmBB.getInstAt(bi);
                if (Util.DEBUG)
                {
                    Util.Debug(inst.toString());
                }
                if (!(inst instanceof TerminatorInst))
                    sdl.visit(inst);
            }
            if (!sdl.hasTailCall())
            {
                for (; bi != end; ++bi)
                {
                    Instruction inst = llvmBB.getInstAt(bi);
                    if (!(inst instanceof PhiNode))
                        sdl.copyToExpendRegsIfNeeds(inst);
                }
                handlePhiNodeInSuccessorBlocks(llvmBB);
                sdl.visit(llvmBB.getTerminator());
            }

            curDAG.setRoot(sdl.getControlRoot());
            codeGenAndEmitInst();
            sdl.clear();

            // add operand for machine phinode
            finishBasicBlock();
        }
    }

    private void handlePhiNodeInSuccessorBlocks(BasicBlock llvmBB)
    {
        HashSet<MachineBasicBlock> handled = new HashSet<>();
        for (int i = 0,  e = llvmBB.getNumSuccessors(); i < e; i++)
        {
            BasicBlock succBB = llvmBB.suxAt(i);
            MachineBasicBlock succMBB = funcInfo.mbbmap.get(succBB);
            assert succBB != null && succMBB != null;

            if (!handled.add(succMBB)) continue;

            Instruction inst;
            int instItr = 0;
            for (int j = 0, sz = succBB.getNumOfInsts();
                 j < sz && ((inst = succBB.getInstAt(j)) instanceof PhiNode); ++j)
            {
                if (inst.isUseEmpty()) continue;
                PhiNode pn = (PhiNode)inst;
                Value incomingOp = pn.getIncomingValueForBlock(llvmBB);
                int reg;
                if (incomingOp instanceof Constant)
                {
                    if (sdl.constantsOut.containsKey(incomingOp))
                        reg = sdl.constantsOut.get(incomingOp);
                    else
                    {
                        reg = funcInfo.createRegForValue(incomingOp);
                        sdl.copyValueToVirtualRegister(incomingOp, reg);
                        sdl.constantsOut.put((Constant) incomingOp, reg);
                    }
                }
                else
                {
                    if (funcInfo.valueMap.containsKey(incomingOp))
                    {
                        reg = funcInfo.valueMap.get(incomingOp);
                    }
                    else
                    {
                        assert incomingOp instanceof AllocaInst &&
                                funcInfo.staticAllocaMap.containsKey(incomingOp);
                        reg = funcInfo.createRegForValue(incomingOp);
                        sdl.copyValueToVirtualRegister(incomingOp, reg);
                        funcInfo.valueMap.put(incomingOp, reg);
                    }
                }

                ArrayList<EVT> vts = new ArrayList<>();
                computeValueVTs(targetLowering, pn.getType(), vts);
                for (EVT vt : vts)
                {
                    int numberRegs = targetLowering.getNumRegisters(vt);
                    for (int k = 0; k < numberRegs; k++)
                    {
                        sdl.phiNodesToUpdate.add(Pair.get(succMBB.getInstAt(instItr++), reg+k));
                    }
                    reg += numberRegs;
                }
            }
        }
        sdl.constantsOut.clear();
    }

    private void codeGenAndEmitInst()
    {
        if (Util.DEBUG)
            System.err.println("Instruction Selection");

        String blockName = mf.getFunction().getName() + ":" +
                mbb.getBasicBlock().getName();
        {
            curDAG.viewGraph("dag-input-combine-first for " + blockName);
        }

        // combine
        curDAG.combine(CombineLevel.Unrestricted, aa, optLevel);

        //if (Util.DEBUG)
        {
            curDAG.viewGraph("dag-before-legalize for " + blockName);
        }

        boolean changed = curDAG.legalizeTypes();

        {
            curDAG.viewGraph("dag-after-first-legalize-types for " + blockName);
        }

        if (changed)
        {
            curDAG.combine(CombineLevel.NoIllegalTypes, aa, optLevel);
            if (Util.DEBUG)
            {
                curDAG.viewGraph("dag-after-second-combines for " + blockName);
            }

            //changed = curDAG.legalizeVectors();
            if (changed)
            {
                changed = curDAG.legalizeTypes();
            }
            if (changed)
                curDAG.combine(CombineLevel.NoIllegalOperations, aa, optLevel);
        }
        //if (Util.DEBUG)
        {
            curDAG.viewGraph("dag-after-combine2 for " + blockName);
        }

        curDAG.legalize(false, optLevel);
        curDAG.combine(CombineLevel.NoIllegalOperations, aa, optLevel);
        if (ViewDAGBeforeISel.value)
        {
            curDAG.viewGraph("dag-before-isel for " + blockName);
        }
        instructionSelect();

        if (ViewDAGBeforeSched.value)
        {
            curDAG.viewGraph("dag-before-sched for " + blockName);
        }

        ScheduleDAG scheduler = createScheduler(this, optLevel);
        scheduler.run(curDAG, mbb, mbb.size());
        if (ViewDAGAfterSched.value)
        {
            scheduler.viewGraph("dag-after-sched for " + blockName);
        }
        mbb = scheduler.emitSchedule();
    }

    public abstract void instructionSelect();

    private boolean lowerArguments(BasicBlock llvmBB)
    {
        Function fn = llvmBB.getParent();
        SelectionDAG dag = sdl.dag;
        SDValue oldRoot = sdl.getRoot();
        TargetData td = targetLowering.getTargetData();

        ArrayList<InputArg> ins = new ArrayList<>();
        int idx = 1;
        for(Argument arg : fn.getArgumentList())
        {
            boolean isArgUseEmpty = arg.isUseEmpty();
            ArrayList<EVT> vts = new ArrayList<>();
            computeValueVTs(targetLowering, arg.getType(), vts);
            for (EVT vt : vts)
            {
                Type argTy = vt.getTypeForEVT();
                ArgFlagsTy flags = new ArgFlagsTy();
                int originalAlign = td.getABITypeAlignment(argTy);

                if (fn.paramHasAttr(idx, Attribute.ZExt))
                    flags.setZExt();
                if (fn.paramHasAttr(idx, Attribute.SExt))
                    flags.setSExt();
                if (fn.paramHasAttr(idx, Attribute.InReg))
                    flags.setInReg();
                if (fn.paramHasAttr(idx, Attribute.StructRet))
                    flags.setSRet();
                if (fn.paramHasAttr(idx, Attribute.ByVal))
                {
                    flags.setByVal();
                    PointerType ty = (PointerType)arg.getType();
                    Type elemTy = ty.getElementType();
                    int frameAlign = td.getTypeAlign(elemTy);
                    long frameSize = td.getTypeSize(elemTy);

                    if (fn.getParamAlignment(idx) != 0)
                        frameAlign = fn.getParamAlignment(idx);
                    flags.setByValAlign(frameAlign);
                    flags.setByValSize((int) frameSize);
                }
                if (fn.paramHasAttr(idx, Attribute.Nest))
                    flags.setNest();
                flags.setOrigAlign(originalAlign);

                EVT registerVT = targetLowering.getRegisterType(vt);
                int numRegs = targetLowering.getNumRegisters(registerVT);
                for (int i = 0; i < numRegs; i++)
                {
                    InputArg myArgs = new InputArg(flags, registerVT, isArgUseEmpty);
                    if (numRegs > 1 && i ==0)
                    {
                         myArgs.flags.setSplit();
                    }
                    else if (i > 0)
                        myArgs.flags.setOrigAlign(1);
                    ins.add(myArgs);
                }
                ++idx;
            }
        }

        ArrayList<SDValue> inVals = new ArrayList<>();
        SDValue newRoot = targetLowering.lowerFormalArguments(dag.getRoot(),
                fn.getCallingConv(), fn.isVarArg(), ins, dag, inVals);

        assert newRoot.getNode() != null && newRoot.getValueType().equals(new EVT(MVT.Other))
                :"lowerFormalArguments din't return a valid chain!";
        assert inVals.size() == ins.size():"lowerFormalArguments didn't emit the correct number of values";

        dag.setRoot(newRoot);

        // set up the incoming arguments.
        int i = 0;
        idx = 1;
        for(Argument arg : fn.getArgumentList())
        {
            ArrayList<SDValue> argValues = new ArrayList<>();
            ArrayList<EVT> vts = new ArrayList<>();
            computeValueVTs(targetLowering, arg.getType(), vts);
            for (EVT vt : vts)
            {
                EVT partVT = targetLowering.getRegisterType(vt);
                int numParts = targetLowering.getNumRegisters(partVT);
                if (!arg.isUseEmpty())
                {
                    int op = ISD.DELETED_NODE;
                    if (fn.paramHasAttr(idx, Attribute.SExt))
                        op = ISD.AssertSext;
                    else if (fn.paramHasAttr(idx, Attribute.ZExt))
                        op = ISD.AssertZext;

                    SDValue[] ops = new SDValue[numParts];
                    for (int j = 0; j < numParts;j++)
                        ops[j] = inVals.get(j+i);
                    argValues.add(getCopyFromParts(dag, ops, partVT, vt, op));
                }
                i += numParts;
            }
            if (!arg.isUseEmpty())
            {
                sdl.setValue(arg, dag.getMergeValues(argValues));
                sdl.copyToExpendRegsIfNeeds(arg);
            }
            ++idx;
        }

        assert i == inVals.size();

        emitFunctionEntryCode(fn, mf);
        return false;
    }

    /**
     * For special target machine to implement special purpose.
     * @param fn
     * @param mf
     */
    public void emitFunctionEntryCode(Function fn, MachineFunction mf){}

    private void finishBasicBlock()
    {
        if (Util.DEBUG)
        {
            System.err.println("Target-post-processed machine code:");
            mbb.dump();

            System.err.printf("Total mount of phi nodes to update: %d%n",
                    sdl.phiNodesToUpdate.size());
            int i = 0;
            for (Pair<MachineInstr, Integer> itr : sdl.phiNodesToUpdate)
            {
                System.err.printf("Node %d : (", i++);
                itr.first.dump();
                System.err.printf(", %d)%n", itr.second);
            }
        }
        if (sdl.switchCases.isEmpty() && sdl.jtiCases.isEmpty() &&
                sdl.bitTestCases.isEmpty())
        {
            for (int i = 0, e = sdl.phiNodesToUpdate.size(); i < e; i++)
            {
                MachineInstr phi = sdl.phiNodesToUpdate.get(i).first;
                assert phi.getOpcode() == TargetInstrInfo.PHI:"This is not a phi node!";
                phi.addOperand(MachineOperand.createReg(sdl.phiNodesToUpdate.get(i).second,
                        false, false));
                phi.addOperand(MachineOperand.createMBB(mbb, 0));
            }
            sdl.phiNodesToUpdate.clear();
            return;
        }
        for (int i = 0, e = sdl.bitTestCases.size(); i < e; i++)
        {
            if (!sdl.bitTestCases.get(i).emitted)
            {
                mbb = sdl.bitTestCases.get(i).parentMBB;
                sdl.setCurrentBasicBlock(mbb);
                sdl.visitBitTestHeader(sdl.bitTestCases.get(i));
                curDAG.setRoot(sdl.getRoot());
                codeGenAndEmitInst();
                sdl.clear();
            }

            for (int j = 0, sz = sdl.bitTestCases.get(i).cases.size(); j < sz; j++)
            {
                mbb = (sdl.bitTestCases.get(i).cases.get(j)).thisMBB;
                sdl.setCurrentBasicBlock(mbb);

                if (j+1 < sz)
                {
                    sdl.visitBitTestCase(sdl.bitTestCases.get(i).cases.get(j+1).thisMBB,
                            sdl.bitTestCases.get(i).reg, sdl.bitTestCases.get(i).cases.get(j));
                }
                else
                {
                    sdl.visitBitTestCase(sdl.bitTestCases.get(i).defaultMBB,
                            sdl.bitTestCases.get(i).reg, sdl.bitTestCases.get(i).cases.get(j));
                }

                curDAG.setRoot(sdl.getRoot());
                codeGenAndEmitInst();
                sdl.clear();
            }

            for (int pi = 0, pe = sdl.phiNodesToUpdate.size(); pi < pe; pi++)
            {
                MachineInstr phi = sdl.phiNodesToUpdate.get(pi).first;
                MachineBasicBlock mbb = phi.getParent();
                assert phi.getOpcode() == TargetInstrInfo.PHI:"This is not a phi node!";

                if (mbb.equals(sdl.bitTestCases.get(i).defaultMBB))
                {
                    phi.addOperand(MachineOperand.createReg(sdl.phiNodesToUpdate.get(pi).second,
                            false, false));
                    phi.addOperand(MachineOperand.createMBB(sdl.bitTestCases.get(i).parentMBB, 0));
                    phi.addOperand(MachineOperand.createReg(sdl.phiNodesToUpdate.get(pi).second,
                            false, false));
                    int sz = sdl.bitTestCases.get(i).cases.size();
                    phi.addOperand(MachineOperand.createMBB(sdl.bitTestCases.get(i).cases.get(sz-1).thisMBB, 0));
                }

                for (int j = 0, ej = sdl.bitTestCases.get(i).cases.size(); j < ej; j++)
                {
                    MachineBasicBlock bb = sdl.bitTestCases.get(i).cases.get(j).thisMBB;
                    if (!mbb.getSuccessors().contains(mbb))
                    {
                        phi.addOperand(MachineOperand.createReg(sdl.phiNodesToUpdate.get(pi).second,
                                false, false));
                        phi.addOperand(MachineOperand.createMBB(bb, 0));
                    }
                }
            }
        }
        sdl.bitTestCases.clear();

        for (int i = 0, e = sdl.jtiCases.size(); i < e; i++)
        {
            if (!sdl.jtiCases.get(i).first.emitted)
            {
                mbb = sdl.jtiCases.get(i).first.headerBB;
                sdl.setCurrentBasicBlock(mbb);
                sdl.visitJumpTableHeader(sdl.jtiCases.get(i).second,
                        sdl.jtiCases.get(i).first);
                curDAG.setRoot(sdl.getRoot());
                codeGenAndEmitInst();
                sdl.clear();
            }

            mbb = sdl.jtiCases.get(i).second.mbb;
            sdl.setCurrentBasicBlock(mbb);

            sdl.visitJumpTable(sdl.jtiCases.get(i).second);
            curDAG.setRoot(sdl.getRoot());
            codeGenAndEmitInst();
            sdl.clear();

            for (int pi = 0, pe = sdl.phiNodesToUpdate.size(); pi < pe; pi++)
            {
                MachineInstr phi = sdl.phiNodesToUpdate.get(pi).first;
                MachineBasicBlock phiMBB = phi.getParent();
                assert phi.getOpcode() == TargetInstrInfo.PHI:"This is not a PHI node!";
                if (phiMBB.equals(sdl.jtiCases.get(i).second.defaultMBB))
                {
                    phi.addOperand(MachineOperand.createReg(sdl.phiNodesToUpdate.get(pi).second,
                            false, false));
                    phi.addOperand(MachineOperand.createMBB(sdl.jtiCases.get(i).first.headerBB, 0));
                }
                if (mbb.getSuccessors().contains(phiMBB))
                {
                    phi.addOperand(MachineOperand.createReg(sdl.phiNodesToUpdate.get(pi).second,
                            false, false));
                    phi.addOperand(MachineOperand.createMBB(mbb, 0));
                }
            }
        }
        sdl.jtiCases.clear();

        for (int i = 0, e = sdl.phiNodesToUpdate.size(); i < e; i++)
        {
            MachineInstr phi = sdl.phiNodesToUpdate.get(i).first;
            assert phi.getOpcode() == TargetInstrInfo.PHI;
            if (mbb.isSuccessor(phi.getParent()))
            {
                phi.addOperand(MachineOperand.createReg(sdl.phiNodesToUpdate.get(i).second,
                false,false));
                phi.addOperand(MachineOperand.createMBB(mbb, 0));
            }
        }

        for (int i = 0, e = sdl.switchCases.size(); i < e; i++)
        {
            mbb = sdl.switchCases.get(i).thisMBB;
            sdl.setCurrentBasicBlock(mbb);

            sdl.visitSwitchCase(sdl.switchCases.get(i));
            curDAG.setRoot(sdl.getRoot());
            codeGenAndEmitInst();
            sdl.clear();

            while ((mbb = sdl.switchCases.get(i).trueMBB) != null)
            {
                for (int pi = 0, sz = mbb.size(); pi < sz &&
                        mbb.getInstAt(pi).getOpcode() == TargetInstrInfo.PHI; ++pi)
                {
                    MachineInstr phi = mbb.getInstAt(pi);
                    for (int pn = 0; ; ++pn)
                    {
                        assert pn != sdl.phiNodesToUpdate.size():"Didn't find PHI entry!";
                        if (sdl.phiNodesToUpdate.get(pn).first.equals(phi))
                        {
                            phi.addOperand(MachineOperand.createReg(sdl.phiNodesToUpdate.get(pn).second,
                                    false, false));
                            phi.addOperand(MachineOperand.createMBB(sdl.switchCases.get(i).thisMBB, 0));
                            break;
                        }
                    }
                }

                if (mbb.equals(sdl.switchCases.get(i).falseMBB))
                    sdl.switchCases.get(i).falseMBB = null;

                sdl.switchCases.get(i).trueMBB = sdl.switchCases.get(i).falseMBB;
                sdl.switchCases.get(i).falseMBB = null;
            }
            assert sdl.switchCases.get(i).trueMBB == null && sdl.switchCases.get(i).falseMBB == null;
        }

        sdl.switchCases.clear();
        sdl.phiNodesToUpdate.clear();
    }

    public boolean isLegalAndProfitableToFold(SDNode node, SDNode use, SDNode root)
    {
        if (optLevel == CodeGenOpt.None) return false;

        EVT vt = root.getValueType(root.getNumValues()-1);
        while (vt.equals(new EVT(MVT.Flag)))
        {
            SDNode fu = findFlagUse(root);
            if (fu == null)
                break;
            root = fu;
            vt = root.getValueType(root.getNumValues()-1);
        }
        return !isNonImmUse(root, node, use);
    }

    static SDNode findFlagUse(SDNode node)
    {
        int flagResNo = node.getNumValues()-1;
        for (SDUse u : node.useList)
        {
            if (u.getResNo() == flagResNo)
                return u.getUser();
        }
        return null;
    }

    static boolean isNonImmUse(SDNode root, SDNode def, SDNode immedUse)
    {
        HashSet<SDNode> visited = new HashSet<>();
        return findNonImmUse(root, def, immedUse, root, visited);
    }

    static boolean findNonImmUse(SDNode use, SDNode def, SDNode immedUse, SDNode root,
            HashSet<SDNode> visited)
    {
        if (use.getNodeID() < def.getNodeID() || !visited.add(use))
            return false;

        for (int i = 0, e = use.getNumOperands(); i < e; i++)
        {
            SDNode n = use.getOperand(i).getNode();
            if (n.equals(def))
            {
                if (use.equals(immedUse) || use.equals(root))
                    continue;

                assert !Objects.equals(n, root);
                return true;
            }

            if (findNonImmUse(n, def, immedUse, root, visited))
                return true;
        }
        return false;
    }

    @Override
    public String getPassName()
    {
        return "Instruction Selector based on DAG covering";
    }

    protected boolean checkAndMask(SDValue lhs, SDNode.ConstantSDNode rhs,
            long desiredMaskS)
    {
        APInt actualMask = rhs.getAPIntValue();
        APInt desiredMask = new APInt(lhs.getValueSizeInBits(), desiredMaskS);
        if (actualMask.eq(desiredMask))
            return true;

        if (actualMask.intersects(desiredMask.negative()))
            return false;

        APInt neededMask = desiredMask.and(actualMask.negative());
        if (curDAG.maskedValueIsZero(lhs, neededMask, 0))
            return true;

        return false;
    }

    protected boolean checkOrMask(SDValue lhs, SDNode.ConstantSDNode rhs,
            long desiredMaskS)
    {
        APInt actualMask = rhs.getAPIntValue();
        APInt desiredMask = new APInt(lhs.getValueSizeInBits(), desiredMaskS);
        if (actualMask.eq(desiredMask))
            return true;

        if (actualMask.intersects(desiredMask.negative()))
            return false;

        APInt neededMask = desiredMask.and(actualMask.negative());
        APInt[] res = new APInt[2];
        curDAG.computeMaskedBits(lhs, neededMask, res, 0);
        APInt knownZero = res[0], knownOne = res[1];
        if (neededMask.and(knownOne).eq(neededMask))
            return true;

        return false;
    }

    public static boolean isChainCompatible(SDNode chain, SDNode op)
    {
        if (chain.getOpcode() == ISD.EntryToken)
            return true;
        if (chain.getOpcode() == ISD.TokenFactor)
            return false;
        if (chain.getNumOperands() > 0)
        {
            SDValue c0 = chain.getOperand(0);
            if (c0.getValueType().equals(new EVT(MVT.Other)))
                return !c0.getNode().equals(op) && isChainCompatible(c0.getNode(), op);
        }
        return true;
    }

    public void selectInlineAsmMemoryOperands(ArrayList<SDValue> ops)
    {
        // TODO: 18-4-21
        Util.shouldNotReachHere("Inline assembly not supported!");
    }

    public SDNode select_INLINEASM(SDValue n)
    {
        ArrayList<SDValue> ops = new ArrayList<>();
        for (int i = 0, e = n.getNumOperands(); i < e; i++)
            ops.add(n.getOperand(i));
        selectInlineAsmMemoryOperands(ops);

        ArrayList<EVT> vts = new ArrayList<>();
        vts.add(new EVT(MVT.Other));
        vts.add(new EVT(MVT.Flag));
        SDValue newNode = curDAG.getNode(ISD.INLINEASM,  vts);
        return newNode.getNode();
    }

    public SDNode select_UNDEF(SDValue n)
    {
        return curDAG.selectNodeTo(n.getNode(), TargetInstrInfo.IMPLICIT_DEF,
                n.getValueType());
    }

    public SDNode select_DBG_LABEL(SDValue n)
    {
        SDValue chain = n.getOperand(0);
        int c = ((LabelSDNode)n.getNode()).getLabelID();
        SDValue tmp = curDAG.getTargetConstant(c, new EVT(MVT.i32));
        return curDAG.selectNodeTo(n.getNode(), TargetInstrInfo.DBG_LABEL,
                new EVT(MVT.Other), tmp, chain);
    }

    public SDNode select_EH_LABEL(SDValue n)
    {
        SDValue chain = n.getOperand(0);
        int c = ((LabelSDNode)n.getNode()).getLabelID();
        SDValue tmp = curDAG.getTargetConstant(c, new EVT(MVT.i32));
        return curDAG.selectNodeTo(n.getNode(), TargetInstrInfo.EH_LABEL,
                new EVT(MVT.Other), tmp, chain);
    }

    public void cannotYetSelect(SDValue n)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream os = new PrintStream(baos);
        os.print("Cannot yet select: ");
        n.getNode().print(os, curDAG);
        os.close();
        llvmReportError(baos.toString());
    }

    public void cannotYetSelectIntrinsic(SDValue n)
    {
        System.err.println("Cannot yet select intrinsic function.");
    }
}
