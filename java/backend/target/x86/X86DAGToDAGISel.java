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

package backend.target.x86;

import backend.codegen.*;
import backend.codegen.dagisel.*;
import backend.codegen.dagisel.SDNode.FrameIndexSDNode;
import backend.codegen.dagisel.SDNode.GlobalAddressSDNode;
import backend.codegen.dagisel.SDNode.HandleSDNode;
import backend.support.Attribute;
import backend.target.TargetMachine;
import backend.value.Function;
import backend.value.GlobalValue;

import java.util.ArrayList;

public abstract class X86DAGToDAGISel extends SelectionDAGISel
{
    protected X86TargetLowering tli;
    protected X86Subtarget subtarget;
    protected int iselPosition;
    protected boolean optForSize;

    public X86DAGToDAGISel(X86TargetMachine tm, TargetMachine.CodeGenOpt optLevel)
    {
        super(tm, optLevel);
        subtarget = tm.getSubtarget();
        tli = tm.getTargetLowering();
    }

    @Override
    public void instructionSelect()
    {
        Function f = mf.getFunction();
        optForSize = f.hasFnAttr(Attribute.OptimizeForSize);
        if (optLevel != TargetMachine.CodeGenOpt.None)
            preprocessForFPConvert();

        selectRoot();
        curDAG.removeDeadNodes();
    }

    private void selectRoot()
    {
        selectRootInit();

        HandleSDNode dummy = new HandleSDNode(curDAG.getRoot());
        iselPosition = curDAG.allNodes.indexOf(curDAG.getRoot().getNode());
        assert iselPosition != -1:"Specified root node not exists in allNodes!";
        iselPosition++;
        while (iselPosition != 0)
        {
            SDNode node = curDAG.allNodes.get(--iselPosition);
            if (node.isUseEmpty())
                continue;

            SDNode resNode = select(new SDValue(node, 0));
            if (resNode.equals(node))
                continue;
            if (resNode != null)
                replaceUses(node, resNode);

            if (node.isUseEmpty())
            {
                ISelUpdater updater = new ISelUpdater(iselPosition, curDAG.allNodes);
                curDAG.removeDeadNode(node, updater);
                iselPosition = updater.getIselPos();
            }
        }
        curDAG.setRoot(dummy.getValue());
    }

    private void selectRootInit()
    {
        dagSize = curDAG.assignTopoLogicalOrder();
    }

    /**
     * Replace the old node with new one.
     * @param oldNode
     * @param newNode
     */
    public void replaceUses(SDNode oldNode, SDNode newNode)
    {
        ISelUpdater isu = new ISelUpdater(iselPosition, curDAG.allNodes);
        curDAG.replaceAllUsesOfValueWith(oldNode, newNode, isu);
        iselPosition = isu.getIselPos();
    }

    private static class ISelUpdater implements DAGUpdateListener
    {
        private int iselPos;
        private ArrayList<SDNode> allNodes;
        public ISelUpdater(int pos, ArrayList<SDNode> nodes)
        {
            iselPos = pos;
            allNodes = nodes;
        }

        @Override
        public void nodeDeleted(SDNode node, SDNode e)
        {
            if (allNodes.get(iselPos) == node)
                ++iselPos;
        }

        @Override
        public void nodeUpdated(SDNode node)
        {
        }

        public int getIselPos()
        {
            return iselPos;
        }
    }

    public static X86DAGToDAGISel createX86DAGToDAGISel(X86TargetMachine tm, TargetMachine.CodeGenOpt level)
    {
        return null;//new X86GenDAGToDAGISel(tm, level);
    }

    @Override
    public String getPassName()
    {
        return "X86 DAG To DAG Instruction Selector";
    }

    @Override
    public void emitFunctionEntryCode(Function fn, MachineFunction mf)
    {
        // TODO: 2018/4/10
    }

    @Override
    public boolean isLegalAndProfitableToFold(SDNode node, SDNode use, SDNode root)
    {
        return super.isLegalAndProfitableToFold(node, use, root);
    }

    /**
     * This method should be overrided by tablegen class.
     * @param node
     * @return
     */
    public abstract SDNode selectCode(SDValue node);

    protected SDNode select(SDValue val)
    {
        return selectCode(val);
    }

    protected SDNode selectAtomic64(SDNode node, int opc)
    {
        SDValue chain = node.getOperand(0);
        SDValue in1 = node.getOperand(1);
        SDValue in2 = node.getOperand(2);
        SDValue in3 = node.getOperand(3);
        SDValue[] temp = new SDValue[5];
        if (!selectAddr(in1, in2, temp))
            return null;

        SDValue lsi = node.getOperand(4);
        SDValue[] ops = new SDValue[9];
        System.arraycopy(temp, 0, ops, 0, 5);
        ops[5] = in2;
        ops[6] = in3;
        ops[7] = lsi;
        ops[8] = chain;
        EVT[] vts = {new EVT(MVT.i32), new EVT(MVT.i32), new EVT(MVT.Other)};
        return curDAG.getTargetNode(opc, vts, ops);
    }

    protected SDNode selectAtomicLoadAdd(SDNode node, EVT vt)
    {
        return null;
    }

    protected boolean matchSegmentBaseAddress(SDValue val, X86ISelAddressMode am)
    {
        return false;
    }

    protected boolean matchLoad(SDValue val, X86ISelAddressMode am)
    {
        return false;
    }

    protected boolean matchWrapper(SDValue val, X86ISelAddressMode am)
    {
        return false;
    }

    protected boolean matchAddress(SDValue val, X86ISelAddressMode am)
    {
        return false;
    }

    protected boolean matchAddressRecursively(SDValue val, X86ISelAddressMode am)
    {
        return false;
    }

    protected boolean matchAddressBase(SDValue val, X86ISelAddressMode am)
    {
        return false;
    }

    /**
     * comp indicates the base, scale, index, disp and segment for X86 Address mode.
     * @param op
     * @param val
     * @param comp
     * @return
     */
    protected boolean selectAddr(SDValue op, SDValue val, SDValue[] comp)
    {
        return false;
    }

    protected boolean selectLEAAddr(SDValue op, SDValue val, SDValue[] comp)
    {
        return false;
    }

    protected boolean selectTLSADDRAddr(SDValue op, SDValue val, SDValue[] comp)
    {
        return false;
    }

    protected boolean selectScalarSSELoad(SDValue op, SDValue pred, SDValue node, SDValue[] comp)
    {
        return false;
    }

    protected boolean tryFoldLoad(SDValue pred, SDNode node, SDValue[] comp)
    {
        return false;
    }

    protected void preprocessForRMW()
    {

    }

    protected void preprocessForFPConvert()
    {}

    protected void emitSpecialCodeForMain(MachineBasicBlock mbb, MachineFrameInfo mfi)
    {}

    protected void getAddressOperands(X86ISelAddressMode am, SDValue[] comp)
    {}

    protected SDValue getI8Imm(int imm)
    {
        return curDAG.getTargetConstant(imm, new EVT(MVT.i8));
    }

    protected SDValue getI16Imm(int imm)
    {
        return curDAG.getTargetConstant(imm, new EVT(MVT.i16));
    }

    protected SDValue getI32Imm(int imm)
    {
        return curDAG.getTargetConstant(imm, new EVT(MVT.i32));
    }

    protected SDNode getGlobalBaseReg()
    {
        int baseReg = getTargetMachine().getInstrInfo().getGlobalBaseReg(mf);
        return curDAG.getRegister(baseReg, new EVT(tli.getPointerTy())).getNode();
    }

    public X86TargetMachine getTargetMachine()
    {
        return (X86TargetMachine)super.getTargetMachine();
    }

    public SDNode select_DECLARE(SDValue n)
    {
        SDValue chain = n.getOperand(0);
        SDValue n1 = n.getOperand(1);
        SDValue n2 = n.getOperand(2);
        if (!(n1.getNode() instanceof FrameIndexSDNode)
                || !(n2.getNode() instanceof GlobalAddressSDNode))
        {
            cannotYetSelect(n);
        }
        int fi = ((FrameIndexSDNode)n1.getNode()).getFrameIndex();
        GlobalValue gv = ((GlobalAddressSDNode)n2.getNode()).getGlobalValue();
        SDValue tmp1 = curDAG.getTargetFrameIndex(fi, tli.getPointerTy());
        SDValue tmp2 = curDAG.getTargetGlobalAddress(gv, tli.getPointerTy());
        return curDAG.selectNodeTo(n.getNode(),
                X86GenInstrNames.DECLARE,
                new EVT(MVT.Other), tmp1, tmp2, chain);
    }
}
