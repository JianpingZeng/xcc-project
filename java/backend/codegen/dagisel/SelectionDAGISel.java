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
import static backend.support.ErrorHandling.llvmReportError;

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
        au.addRequired(AliasAnalysis.class);
        au.addPreserved(AliasAnalysis.class);
        au.addRequired(MachineModuleInfo.class);
        au.addPreserved(MachineModuleInfo.class);
        super.getAnalysisUsage(au);
    }

    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        this.mf = mf;
        Function f = mf.getFunction();
        AliasAnalysis aa = (AliasAnalysis) getAnalysisToUpDate(AliasAnalysis.class);
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
                assert lowerArguments(llvmBB) :"Fail to lower argument!";
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
            codeGenEmit();
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

    private void codeGenEmit()
    {
        if (Util.DEBUG)
            System.err.println("Instrtuction Selection");
        instructionSelect();

        new ListScheduler(curDAG, mbb).emit();
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
                    for (int j = i; j < i+numParts;j++)
                        ops[j] = inVals.get(j);
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
        // TODO: 18-5-1
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
