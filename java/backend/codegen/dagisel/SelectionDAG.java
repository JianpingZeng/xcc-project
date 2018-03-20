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

import backend.codegen.EVT;
import backend.codegen.MachineFunction;
import backend.codegen.MachineModuleInfo;
import backend.codegen.dagisel.SDNode.CondCodeSDNode;
import backend.codegen.dagisel.SDNode.SDVTList;
import backend.codegen.fastISel.ISD;
import backend.target.TargetLowering;
import backend.target.TargetMachine;
import backend.value.ConstantInt;
import gnu.trove.map.hash.TIntObjectHashMap;
import tools.APInt;
import tools.FoldingSetNodeID;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class SelectionDAG
{
    private TargetMachine target;
    private TargetLowering tli;
    private MachineFunction mf;
    private FunctionLoweringInfo fli;
    private MachineModuleInfo mmi;
    private SDNode entryNode;
    private SDValue root;
    private ArrayList<SDNode> allNodes;
    private ArrayList<SDVTList> vtlist;
    private ArrayList<CondCodeSDNode> condCodeNodes;
    private TIntObjectHashMap<SDNode> cseMap;
    public SelectionDAG()
    {
        allNodes = new ArrayList<>();
        vtlist = new ArrayList<>();
        condCodeNodes = new ArrayList<>();
        cseMap = new TIntObjectHashMap<>();
    }

    public TargetMachine getTarget()
    {
        return target;
    }

    public SDValue getNode(int opc, EVT vt, SDValue... ops)
    {
    }

    public SDValue getCondCode(CondCode cond)
    {
        int idx = cond.ordinal();
        if (idx < condCodeNodes.size())
        {
            return new SDValue(condCodeNodes.get(idx), 0);
        }
        for (int i = condCodeNodes.size(); i < idx; i++)
            condCodeNodes.add(null);
        CondCodeSDNode node = new CondCodeSDNode(cond);
        condCodeNodes.set(idx, node);
        allNodes.add(node);
        return new SDValue(node, 0);
    }

    public SDValue getSetCC(EVT vt, SDValue op1, SDValue op2, CondCode cond)
    {
        return getNode(ISD.SETCC, vt, op1, op2, getCondCode(cond));
    }

    public SDValue getIntPtrConstant(long amt)
    {
        return getIntPtrConstant(amt, false);
    }

    public SDValue getIntPtrConstant(long amt, boolean isTarget)
    {
        return getConstant(amt, new EVT(tli.getPointerTy()), isTarget);
    }

    public SDValue getConstant(long val, EVT vt, boolean isTarget)
    {
        EVT eltVt = vt.isVector()?vt.getVectorElementType() : vt;
        assert eltVt.getSizeInBits() >= 64 || (val >> eltVt.getSizeInBits()) + 1 < 2
                :"getConstant with a long value that doesn't fit in type!";
        return getConstant(new APInt(eltVt.getSizeInBits(), val), vt, isTarget);
    }

    public SDValue getConstant(APInt val, EVT vt, boolean isTarget)
    {
        return getConstant(ConstantInt.get(val), vt, isTarget);
    }

    public SDVTList getVTList(EVT vt)
    {}

    public SDValue getConstant(ConstantInt ci, EVT vt, boolean isTarget)
    {
        assert vt.isInteger():"Can't create FP integer constant";
        EVT eltVT = vt.isVector()?vt.getVectorElementType() : vt;
        assert ci.getBitsWidth() == eltVT.getSizeInBits():"APInt size doesn't match type size!";

        int opc = isTarget?ISD.TargetConstant : ISD.Constant;
        FoldingSetNodeID id = new FoldingSetNodeID();
        addNodeToIDNode(id, opc, getVTList(eltVT), null);
        id.addInteger(ci.hashCode());
        int hash = id.computeHash();
        SDNode n = null;
        if (cseMap.containsKey(hash))
        {
            n = cseMap.get(hash);
            if (!vt.isVector())
                return new SDValue(cseMap.get(hash), 0);
        }
        if (n == null)
        {
            n = new SDNode.ConstantSDNode(isTarget, ci, eltVT);
            cseMap.put(hash, n);
            allNodes.add(n);
        }

        SDValue res = new SDValue(n, 0);
        if (vt.isVector())
        {
            SDValue[] ops = new SDValue[vt.getVectorNumElements()];
            for (int i = 0; i < ops.length; i++)
                ops[i]  = res;

            res = getNode(ISD.BUILD_VECTOR, vt, ops);
        }
        return res;
    }

    private void addNodeToIDNode(FoldingSetNodeID id, int opc, SDVTList vtList,
            SDValue... ops)
    {

    }
}
