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
import backend.codegen.ValueTypeAction;
import backend.codegen.dagisel.SDNode.StoreSDNode;
import backend.target.TargetLowering;
import backend.target.TargetMachine;
import gnu.trove.list.array.TIntArrayList;
import utils.tablegen.SDNP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This takes an arbitrary {@linkplain SelectionDAG} as input and transform it
 * until all operations and types are supported directly by target machine.
 * This involves eliminating value sizes the machine can't handle as well as
 * eliminating operations the machine can't cope with.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class SelectionDAGLegalizer
{
    private TargetLowering tli;
    private SelectionDAG dag;
    private TargetMachine.CodeGenOpt optLevel;

    private SDValue lastCALLSEQ_END;

    private boolean isLegalizingCall;
    private enum LegalizeAction
    {
        Legal,
        Promote,
        Expand
    }

    private ValueTypeAction valueTypeAction;
    private HashMap<SDValue, SDValue> legalizeNodes;

    public SelectionDAGLegalizer(SelectionDAG dag,
            TargetMachine.CodeGenOpt optLevel)
    {
        this.dag = dag;
        this.tli = dag.getTargetLoweringInfo();
        this.optLevel = optLevel;
        valueTypeAction = tli.getValueTypeActions();
    }

    private void addLegalizedOperand(SDValue from, SDValue to)
    {
        legalizeNodes.put(from, to);
        if (from.equals(to))
            legalizeNodes.put(to, to);
    }

    public void legalizeDAG()
    {
        lastCALLSEQ_END = dag.getEntryNode();
        isLegalizingCall = false;

        dag.assignTopologicalOrder();
        for (SDNode node : dag.allNodes)
            legalizeOp(new SDValue(node, 0));

        SDValue oldRoot = dag.getRoot();
        assert legalizeNodes.containsKey(oldRoot):"Root didn't get legalized!";
        dag.setRoot(legalizeNodes.get(oldRoot));

        legalizeNodes.clear();
        dag.removeDeadNodes();
    }

    public TargetLowering.LegalizeAction getTypeAction(EVT vt)
    {
        return valueTypeAction.getTypeAction(vt);
    }

    public boolean isTypeLegal(EVT vt)
    {
        return getTypeAction(vt) == TargetLowering.LegalizeAction.Legal;
    }

    private SDValue legalizeOp(SDValue val)
    {}

    private SDValue optimizeFloatStore(StoreSDNode st)
    {}

    private SDValue performInsertVectorEltInMemory(SDValue vec,
            SDValue val, SDValue idx)
    {}

    private SDValue expandInsertVectorElt(SDValue vec, SDValue val, SDValue idx)
    {}

    private SDValue shuffleWithNarrowerEltType(EVT nvt, EVT vt, SDValue n1,
            SDValue n2, TIntArrayList mask)
    {}

    private boolean legalizeAllNodesNotLeadingTo(SDNode n, SDNode dest,
            HashSet<SDNode> nodesLeadingTo)
    {}

    /**
     * values layout as follows.
     * values[0] -- lhs
     * values[1] -- rhs
     * values[2] -- cc
     * @param vt
     * @param values
     */
    private void legalizeSetCCCondCode(EVT vt, SDValue values)
    {}

    private SDValue emitStackConvert(SDValue srcOp, EVT slotVT,
            EVT destVT)
    {}

    private SDValue expandBuildVector(SDNode node)
    {}

    private SDValue expandScalarToVector(SDNode node)
    {}

    private SDValue expandDBGStoppoint(SDNode node)
    {}

    private void expandDynamicStackAlloc(SDNode node, ArrayList<SDValue> results)
    {}

    private SDValue expandFCopySign(SDNode node)
    {}

    private SDValue expandLegalIntToFP(SDValue legalOp,
            EVT destVT, boolean isSigned)
    {}

    private SDValue promoteLegalIntToFP(SDValue legalOp,
            EVT destVT, boolean isSigned)
    {}

    private SDValue promoteLegalFPToInt(SDValue legalOp,
            EVT destVT, boolean isSigned)
    {}

    private SDValue expandBSWAP(SDValue op)
    {}

    private SDValue expandBitCount(int opc, SDValue op)
    {}

    private SDValue expandExtractFromVectorThroughStack(SDValue op)
    {}

    private SDValue expandVectorBuildThroughStack(SDNode node)
    {}

    private void expandNode(SDNode node, ArrayList<SDValue> results)
    {}

    private void promoteNode(SDNode node, ArrayList<SDValue> results)
    {}
}
