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
import backend.codegen.dagisel.SDNode.*;
import backend.codegen.fastISel.ISD;
import backend.target.TargetLowering;
import backend.value.Value;
import tools.Pair;
import tools.Util;
import utils.tablegen.SDNP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static backend.codegen.dagisel.DAGTypeLegalizer.NodeIdFlags.*;

/**
 * This takes an arbitrary SelectionDAG as input and transform on it until
 * only value types the target machine can handle are left. This involves
 * promoting small sizes to large one or splitting large into multiple small
 * values.
 * @author Xlous.zeng
 * @version 0.1
 */
public class DAGTypeLegalizer
{
    private TargetLowering tli;
    private SelectionDAG dag;

    public interface NodeIdFlags
    {
        int ReadyToProcess = 0;
        int NewNode = -11;
        int Unanalyzed = -2;
        int Processed = -3;
    }

    private enum LegalizeAction
    {
        Legal,
        PromotedInteger,
        ExpandInteger,
        SoftenFloat,
        ExpandFloat,
        ScalarizeVector,
        SplitVector,
        WidenVector
    }

    private ValueTypeAction valueTypeActions;

    private HashMap<SDValue, SDValue> promotedIntegers;

    private HashMap<SDValue, Pair<SDValue, SDValue>> expandedIntegers;

    private HashMap<SDValue, SDValue> softenedFloats;

    private HashMap<SDValue, Pair<SDValue, SDValue>> expandedFloats;

    private HashMap<SDValue, SDValue> scalarizedVectors;

    private HashMap<SDValue, Pair<SDValue, SDValue>> splitVectors;

    private HashMap<SDValue, SDValue> widenedVectors;

    private HashMap<SDValue, SDValue> replacedValues;

    private LinkedList<SDNode> worklist;

    public DAGTypeLegalizer(SelectionDAG dag)
    {
        tli = dag.getTargetLoweringInfo();
        this.dag = dag;
        valueTypeActions = tli.getValueTypeActions();
        promotedIntegers = new HashMap<>();
        expandedIntegers = new HashMap<>();
        expandedFloats = new HashMap<>();
        softenedFloats = new HashMap<>();
        scalarizedVectors = new HashMap<>();
        splitVectors = new HashMap<>();
        widenedVectors = new HashMap<>();
        replacedValues = new HashMap<>();
        worklist = new LinkedList<>();
    }

    private LegalizeAction getTypeAction(EVT vt)
    {
        switch (valueTypeActions.getTypeAction(vt))
        {
            default:
                assert false:"Unknown legalize action!";
                return null;
            case Legal:
                return LegalizeAction.Legal;
            case Promote:
                // promote can means two different situations:
                // 1. promote a small value to large one, like i8 -> i32.
                // 2. widen short vector value, such as v3i32 -> v4i32.
                return !vt.isVector()? LegalizeAction.PromotedInteger:
                        LegalizeAction.WidenVector;
            case Expand:
                // Expand can means:
                // 1. split scalar in half
                // 2. convert a float to an integer.
                // 3. scalarize a single-element vector
                // 4. split a vector in two elements.
                if (!vt.isVector())
                {
                    if (!vt.isInteger())
                        return LegalizeAction.ExpandInteger;
                    else if (vt.getSizeInBits() ==
                            tli.getTypeToTransformTo(vt).getSizeInBits())
                        return LegalizeAction.SoftenFloat;
                    else
                        return LegalizeAction.ExpandFloat;
                }
                else if (vt.getVectorNumElements() == 1)
                    return LegalizeAction.ScalarizeVector;
                else
                    return LegalizeAction.SplitVector;
        }
    }

    private boolean isTypeLegal(EVT vt)
    {
        return valueTypeActions.getTypeAction(vt) == TargetLowering.LegalizeAction.Legal;
    }

    private boolean ignoreNodeResults(SDNode n)
    {
        return n.getOpcode() == ISD.TargetConstant;
    }

    public boolean run()
    {
        boolean changed = false;
        HandleSDNode dummy = new HandleSDNode(dag.getRoot());
        dummy.setNodeID(Unanalyzed);

        dag.setRoot(new SDValue());

        for (SDNode node : dag.allNodes)
        {
            if (node.getNumOperands() <= 0)
            {
                node.setNodeID(ReadyToProcess);
                worklist.add(node);
            }
            else
                node.setNodeID(Unanalyzed);
        }

        while (!worklist.isEmpty())
        {
            performExpensiveChecks();
            SDNode n = worklist.pop();
            assert n.getNodeID() == ReadyToProcess:
                    "Node should be ready if on worklist!";

            nodeDone:
            {
                if (!ignoreNodeResults(n))
                {
                    for (int i = 0, numResults = n.getNumValues(); i < numResults; i++)
                    {
                        EVT resultVT = n.getValueType(i);
                        switch (getTypeAction(resultVT))
                        {
                            default:
                                assert false : "Unknown action!";
                                break;
                            case Legal:
                                break;
                            case PromotedInteger:
                                promotedIntegerResult(n, i);
                                changed = true;
                                break nodeDone;
                            case ExpandInteger:
                                expandIntegerResult(n, i);
                                break nodeDone;
                            case ExpandFloat:
                                expandFloatResult(n, i);
                                break nodeDone;
                            case ScalarizeVector:
                                scalarizeVectorResult(n, i);
                                break nodeDone;
                            case SplitVector:
                                splitVectorResult(n, i);
                                break nodeDone;
                            case WidenVector:
                                widenVectorResult(n, i);
                                break nodeDone;
                        }
                    }
                }

                int numOperands = n.getNumOperands();
                boolean needsReanalyzing = false;
                int i = 0;
                for (; i < numOperands; i++)
                {
                    if (ignoreNodeResults(n.getOperand(i).getNode()))
                        continue;

                    EVT opVT = n.getOperand(i).getValueType();
                    switch (getTypeAction(opVT))
                    {
                        default:
                            assert false:"Unknown action!";
                        case Legal:
                            continue;
                        case PromotedInteger:
                            needsReanalyzing = promoteIntegerOperand(n, i);
                            changed = true;
                            break;
                        case ExpandInteger:
                            needsReanalyzing = expandIntegerOperand(n, i);
                            changed = true;
                            break;
                        case SoftenFloat:
                            needsReanalyzing = softenFloatOperand(n, i);
                            changed = true;
                            break;
                        case ExpandFloat:
                            needsReanalyzing = expandFloatOperand(n, i);
                            changed = true;
                            break;
                        case ScalarizeVector:
                            needsReanalyzing = scalarizeVectorOperand(n, i);
                            changed = true;
                            break;
                        case SplitVector:
                            needsReanalyzing = splitVectorOperand(n, i);
                            changed = true;
                            break;
                        case WidenVector:
                            needsReanalyzing = widenVectorOperand(n, i);
                            changed = true;
                            break;
                    }
                    break;
                }

                if (needsReanalyzing)
                {
                    assert n.getNodeID() == ReadyToProcess:"Node ID recaculated?";
                    n.setNodeID(NewNode);

                    SDNode nn = analyzeNewNode(n);
                    if (nn.equals(n))
                    {
                        continue;
                    }

                    assert n.getNumValues() == nn.getNumValues():
                            "Node morphing changed the number of results!";
                    for (int j = 0, e = n.getNumValues(); j < e; j++)
                    {
                        replaceValueWithHelper(new SDValue(n, i), new SDValue(nn, i));
                    }
                    assert n.getNodeID() == NewNode:"Unexpected node state!";
                    continue;
                }

                if (i == numOperands)
                {
                    if (Util.DEBUG)
                    {
                        System.err.print("Legally typed node: ");
                        n.dump(dag);
                        System.err.println();
                    }
                }
            }

            assert n.getNodeID() == ReadyToProcess:"Node ID recaculated?";
            n.setNodeID(Processed);
            for (SDUse use : n.useList)
            {
                SDNode user = use.getUser();
                int nodeId = user.getNodeID();

                if (nodeId > 0)
                {
                    user.setNodeID(nodeId - 1);
                    if (nodeId - 1 == ReadyToProcess)
                        worklist.push(user);

                    continue;
                }

                if (nodeId == NewNode)
                    continue;

                assert nodeId == Unanalyzed:"Unknown node ID!";
                user.setNodeID(user.getNumOperands() - 1);

                if (user.getNumOperands() == 1)
                    worklist.add(user);
            }
        }

        performExpensiveChecks();
        dag.setRoot(dummy.getValue());
        dag.removeDeadNodes();

        return changed;
    }

    public void nodeDeletion(SDNode oldOne, SDNode newOne)
    {
        expungeNode(oldOne);
        expungeNode(newOne);
        for (int i = 0, e = oldOne.getNumValues(); i < e; i++)
            replacedValues.put(new SDValue(oldOne, i), new SDValue(newOne, i));
    }

    private SDNode analyzeNewNode(SDNode n)
    {
        if (n.getNodeID() != NewNode && n.getNodeID() != Unanalyzed)
            return n;

        expungeNode(n);

        ArrayList<SDValue> newOps = new ArrayList<>();
        int numProcessed = 0;
        for (int i = 0, e = n.getNumOperands(); i < e; i++)
        {
            SDValue origOp = n.getOperand(i);
            SDValue op = origOp.clone();

            analyzeNewValue(op);
            if (op.getNode().getNodeID() == Processed)
                ++numProcessed;

            if (!newOps.isEmpty())
                newOps.add(op);
            else if (!op.equals(origOp))
            {
                for (int j = 0; j < i; j++)
                    newOps.add(op.getOperand(j));
                newOps.add(op);
            }
        }
        if (!newOps.isEmpty())
        {
            SDNode nn = dag.updateNodeOperands(new SDValue(n, 0),
                    newOps).getNode();
            if (!nn.equals(n))
            {
                n.setNodeID(NewNode);
                if (nn.getNodeID() != NewNode && nn.getNodeID() != Unanalyzed)
                {
                    return nn;
                }

                n = nn;
                expungeNode(n);
            }
        }

        n.setNodeID(n.getNumOperands() - numProcessed);
        if (n.getNodeID() == ReadyToProcess)
            worklist.push(n);

        return n;
    }

    private void expungeNode(SDNode n)
    {
        if (n.getNodeID() != NewNode)
            return;


        int i = 0, e = n.getNumValues();
        for (; i < e; i++)
        {
            if (replacedValues.containsKey(new SDValue(n,i)))
                break;
        }
        if (i == e)
            return;
        for (Map.Entry<SDValue, SDValue> pair : promotedIntegers.entrySet())
        {
            assert !pair.getKey().getNode().equals(n);
            pair.setValue(remapValue(pair.getValue()));
        }

        for (Map.Entry<SDValue, SDValue> pair : softenedFloats.entrySet())
        {
            assert !pair.getKey().getNode().equals(n);
            pair.setValue(remapValue(pair.getValue()));
        }

        for (Map.Entry<SDValue, SDValue> pair : scalarizedVectors.entrySet())
        {
            assert !pair.getKey().getNode().equals(n);
            pair.setValue(remapValue(pair.getValue()));
        }

        for (Map.Entry<SDValue, SDValue> pair : widenedVectors.entrySet())
        {
            assert !pair.getKey().getNode().equals(n);
            pair.setValue(remapValue(pair.getValue()));
        }

        for (Map.Entry<SDValue, Pair<SDValue, SDValue>> pair : expandedIntegers.entrySet())
        {
            assert !pair.getKey().getNode().equals(n);
            pair.getValue().first = remapValue(pair.getValue().first);
            pair.getValue().second = remapValue(pair.getValue().second);
        }

        for (Map.Entry<SDValue, Pair<SDValue, SDValue>> pair : expandedFloats.entrySet())
        {
            assert !pair.getKey().getNode().equals(n);
            pair.getValue().first = remapValue(pair.getValue().first);
            pair.getValue().second = remapValue(pair.getValue().second);
        }

        for (Map.Entry<SDValue, Pair<SDValue, SDValue>> pair : splitVectors.entrySet())
        {
            assert !pair.getKey().getNode().equals(n);
            pair.getValue().first = remapValue(pair.getValue().first);
            pair.getValue().second = remapValue(pair.getValue().second);
        }

        for (Map.Entry<SDValue, SDValue> pair : replacedValues.entrySet())
        {
            pair.setValue(remapValue(pair.getValue()));
        }
        for (int j = 0, sz = n.getNumValues(); j < sz; j++)
            replacedValues.remove(new SDValue(n, j));
    }

    private void analyzeNewValue(SDValue val)
    {
        val.setNode(analyzeNewNode(val.getNode()));
        if (val.getNode().getNodeID() == Processed)
            remapValue(val);
    }

    private void performExpensiveChecks()
    {
        // TODO: 18-5-17
    }

    private SDValue remapValue(SDValue val)
    {
        if (replacedValues.containsKey(val))
        {
            val = replacedValues.get(val);
            val = remapValue(val);
            assert val.getNode().getNodeID() != NewNode:"Mapped to new node!";
        }
        return val;
    }

    private SDValue bitConvertToInteger(SDValue op)
    {
        int bitWidth = op.getValueType().getSizeInBits();
        return dag.getNode(ISD.BIT_CONVERT, EVT.getIntegerVT(bitWidth), op);
    }

    private SDValue bitConvertVectorToIntegerVector(SDValue op)
    {
        assert op.getValueType().isVector():"Only applies to vectors!";
        int eltWidth = op.getValueType().getVectorElementType().getSizeInBits();
        EVT eltVT = EVT.getIntegerVT(eltWidth);
        int numElts = op.getValueType().getVectorNumElements();
        return dag.getNode(ISD.BIT_CONVERT, EVT.getVectorVT(eltVT, numElts),
                op);
    }

    private SDValue createStackStoreLoad(SDValue op, EVT destVT)
    {
        SDValue stackPtr = dag.createStackTemporary(op.getValueType(), destVT);
        SDValue store = dag.getStore(dag.getEntryNode(), op, stackPtr, null, 0, false, 0);
        return dag.getLoad(destVT, store, stackPtr, null, 0);
    }

    private boolean customLowerNode(SDNode n, EVT vt, boolean legalizeResult)
    {
        if (tli.getOperationAction(n.getOpcode(), vt) != TargetLowering.LegalizeAction.Custom)
        {
            return false;
        }

        ArrayList<SDValue> results = new ArrayList<>();
        if (legalizeResult)
            tli.replaceNodeResults(n, results, dag);
        else
            tli.lowerOperationWrapper(n, results, dag);

        if (results.isEmpty())
            return false;

        assert results.size() == n.getNumValues();
        for (int i = 0, e = results.size(); i < e; i++)
            replaceValueWith(new SDValue(n, i), results.get(i));
        return true;
    }

    private SDValue getVectorElementPointer(SDValue vecPtr, EVT eltVT, SDValue index)
    {
        if (index.getValueType().bitsGT(new EVT(tli.getPointerTy())))
            index = dag.getNode(ISD.TRUNCATE, new EVT(tli.getPointerTy()), index);
        else
            index = dag.getNode(ISD.ZERO_EXTEND, new EVT(tli.getPointerTy()), index);
        int eltSize = eltVT.getSizeInBits()/8;
        index = dag.getNode(ISD.MUL, index.getValueType(), index,
                dag.getConstant(eltSize, index.getValueType(), false));
        return dag.getNode(ISD.ADD, index.getValueType(), index, vecPtr);
    }

    private SDValue joinIntegers(SDValue lo, SDValue hi)
    {
        EVT loVT = lo.getValueType(), hiVT = hi.getValueType();
        EVT nvt = EVT.getIntegerVT(loVT.getSizeInBits() + hiVT.getSizeInBits());
        lo = dag.getNode(ISD.ZERO_EXTEND, nvt, lo);
        hi = dag.getNode(ISD.ANY_EXTEND, nvt, hi);
        hi = dag.getNode(ISD.SHL, nvt, hi, dag.getConstant(loVT.getSizeInBits(),
                new EVT(tli.getPointerTy()), false));
        return dag.getNode(ISD.OR, nvt, lo, hi);
    }

    private SDValue promoteTargetBoolean(SDValue boolVal, EVT vt)
    {
        int extendCode = 0;
        switch (tli.getBooleanContents())
        {
            default:
                assert false:"Unknown booleanConstant";
                break;
            case UndefinedBooleanContent:
                extendCode = ISD.ANY_EXTEND;
                break;
            case ZeroOrOneBooleanContent:
                extendCode = ISD.ZERO_EXTEND;
                break;
            case ZeroOrNegativeOneBooleanContent:
                extendCode = ISD.SIGN_EXTEND;
                break;
        }
        return dag.getNode(extendCode, vt, boolVal);
    }

    private void replaceValueWith(SDValue from, SDValue to)
    {
        expungeNode(from.getNode());
        analyzeNewValue(to);

        replacedValues.put(from, to);
        replaceValueWithHelper(from, to);
    }

    private void replaceValueWithHelper(SDValue from, SDValue to)
    {
        assert !from.getNode().equals(to.getNode()):"Potential legalization loop!";

        analyzeNewValue(to);

        LinkedList<SDNode> nodesToAnalyze = new LinkedList<>();
        NodeUpdateListener nul = new NodeUpdateListener(this, nodesToAnalyze);
        dag.replaceAllUsesOfValueWith(from, to, nul);

        while (!nodesToAnalyze.isEmpty())
        {
            SDNode n = nodesToAnalyze.pop();
            if (n.getNodeID() != NewNode)
                continue;

            SDNode nn = analyzeNewNode(n);
            if (!nn.equals(n))
            {
                assert nn.getNodeID() != NewNode:"Analysis resulted in newNode!";
                assert n.getNumValues() == nn.getNumValues():"Node morphing changed the number of results!";
                for (int i = 0,  e = n.getNumValues(); i < e; i++)
                {
                    SDValue oldVal = new SDValue(n,i);
                    SDValue newVal = new SDValue(nn, i);
                    if (nn.getNodeID() == Processed)
                        remapValue(newVal);
                    dag.replaceAllUsesOfValueWith(oldVal, newVal, nul);
                }
            }
        }
    }

    /**
     * Returned array represents the low value and high value respectively.
     * @param op
     * @return
     */
    private SDValue[] splitInteger(SDValue op)
    {
        SDValue[] vals = new SDValue[2];
        EVT halfVT = EVT.getIntegerVT(op.getValueType().getSizeInBits()/2);
        return splitInteger(op, halfVT,halfVT);
    }

    /**
     * Returned array represents the low value and high value respectively.
     * @param op
     * @param loVT
     * @param hiVT
     * @return
     */
    private SDValue[] splitInteger(SDValue op, EVT loVT, EVT hiVT)
    {
        assert loVT.getSizeInBits() + hiVT.getSizeInBits() ==
            op.getValueType().getSizeInBits();
        SDValue[] res = new SDValue[2];
        res[0] = dag.getNode(ISD.TRUNCATE, loVT, op);
        res[1] = dag.getNode(ISD.SRL, op.getValueType(), op,
                dag.getConstant(loVT.getSizeInBits(), new EVT(tli.getPointerTy()),
                        false));
        res[1] = dag.getNode(ISD.TRUNCATE, hiVT, res[1]);
        return res;
    }

    private SDValue getPromotedInteger(SDValue op)
    {
        SDValue promotedOp = promotedIntegers.get(op);
        promotedOp = remapValue(promotedOp);
        assert promotedOp != null && promotedOp.getNode() != null;
        promotedIntegers.put(op, promotedOp);
        return promotedOp;
    }

    private void setPromotedIntegers(SDValue op, SDValue result)
    {
        analyzeNewValue(result);
        assert !promotedIntegers.containsKey(op):"Node is already promoted!";
        promotedIntegers.put(op, result);
    }

    private SDValue sextPromotedInteger(SDValue op)
    {
        EVT oldVT = op.getValueType();
        op = getPromotedInteger(op);
        return dag.getNode(ISD.SIGN_EXTEND_INREG,
                op.getValueType(), op, dag.getValueType(oldVT));
    }

    private SDValue zextPromotedInteger(SDValue op)
    {
        EVT oldVT = op.getValueType();
        op = getPromotedInteger(op);
        return dag.getZeroExtendInReg(op, oldVT);
    }

    private void promotedIntegerResult(SDNode n, int resNo)
    {
    }

    private SDValue promoteIntResAssertSext(SDNode n)
    {}
    private SDValue promoteIntResAssertZext(SDNode n)
    {}
    private SDValue promoteIntResAtomic1(SDNode n)
    {}
    private SDValue promoteIntResAtomic2(SDNode n)
    {}
    private SDValue promoteIntResBitConvert(SDNode n)
    {}
    private SDValue promoteIntResBSWAP(SDNode n)
    {}
    private SDValue promoteIntResBuildPair(SDNode n)
    {}
    private SDValue promoteIntResConstant(SDNode n)
    {}
    private SDValue promoteIntResConvertRndsat(SDNode n)
    {}
    private SDValue promoteIntResCTLZ(SDNode n)
    {}
    private SDValue promoteIntResCTTZ(SDNode n)
    {}
    private SDValue promoteIntResExtractVectorElt(SDNode n)
    {}
    private SDValue promoteIntResFPToXInt(SDNode n)
    {}
    private SDValue promoteIntResIntExtend(SDNode n)
    {}
    private SDValue promoteIntResLoad(SDNode n)
    {}
    private SDValue promoteIntResOverflow(SDNode n)
    {}
    private SDValue promoteIntResSADDSUBO(SDNode n, int resNo)
    {}
    private SDValue promoteIntResSDIV(SDNode n)
    {}
    private SDValue promoteIntResSELECT(SDNode n)
    {}
    private SDValue promoteIntResSELECTCC(SDNode n)
    {}
    private SDValue promoteIntResSETCC(SDNode n)
    {}
    private SDValue promoteIntResSHL(SDNode n)
    {}
    private SDValue promoteIntResSimpleINtBinOp(SDNode n)
    {}
    private SDValue promoteIntResSignExtendInreg(SDNode n)
    {}
    private SDValue promoteIntResSRA(SDNode n)
    {}
    private SDValue promoteIntResSRL(SDNode n)
    {}
    private SDValue promoteIntResTruncate(SDNode n)
    {}
    private SDValue promoteIntResUADDSUBO(SDNode n, int resNo)
    {}
    private SDValue promoteIntResUDIV(SDNode n)
    {}
    private SDValue promoteIntResUNDEF(SDNode n)
    {}
    private SDValue promoteIntResVAARG(SDNode n)
    {}
    private SDValue promoteIntResXMULO(SDNode n, int resNo)
    {}

    private boolean promoteIntegerOperand(SDNode node, int operandNo)
    {}

    private SDValue promoteOpAnyExtend(SDNode node)
    {}
    private SDValue promoteOpBitConvert(SDNode node)
    {}
    private SDValue promoteOpBuildPair(SDNode node)
    {}
    private SDValue promoteOpBRCC(SDNode node, int opNo)
    {}
    private SDValue promoteOpBRCond(SDNode node, int opNo)
    {}
    private SDValue promoteOpBuildVector(SDNode node)
    {}
    private SDValue promoteOpConvertRndsat(SDNode node)
    {}
    private SDValue promoteOpInsertVectorElt(SDNode node)
    {}
    private SDValue promoteOpMemBarrier(SDNode node)
    {}
    private SDValue promoteOpScalarToVector(SDNode node)
    {}
    private SDValue promoteOpSelect(SDNode node, int opNo)
    {}
    private SDValue promoteOpSelectCC(SDNode node, int opNo)
    {}
    private SDValue promoteOpSetCC(SDNode node, int opNo)
    {}
    private SDValue promoteOpShift(SDNode node)
    {}
    private SDValue promoteOpSignExtend(SDNode node)
    {}
    private SDValue promoteOpSINTToFP(SDNode node)
    {}
    private SDValue promoteOpStore(SDNode node, int opNo)
    {}
    private SDValue promoteOpTruncate(SDNode node)
    {}
    private SDValue promoteOpUINTToFP(SDNode node)
    {}
    private SDValue promoteOpZeroExtend(SDNode node)
    {}

    private SDValue[] promoteSetCCOperands(CondCode cc)
    {}

    private SDValue[] getExpandedInteger(SDValue op)
    {}

    private void setExpandedIntegers(SDValue op, SDValue lo, SDValue hi)
    {}

    private void expandIntegerResult(SDNode n, int resNo) {}
    private SDValue[] expandIntResAnyExtend(SDNode n) {}
    private SDValue[] expandIntResAssertSext(SDNode n) {}
    private SDValue[] expandIntResAssertZext(SDNode n) {}
    private SDValue[] expandIntResConstant(SDNode n) {}
    private SDValue[] expandIntResCTLZ(SDNode n) {}
    private SDValue[] expandIntResCTPOP(SDNode n) {}
    private SDValue[] expandIntResCTTZ(SDNode n) {}
    private SDValue[] expandIntResLoad(SDNode n) {}
    private SDValue[] expandIntResSignExtend(SDNode n) {}
    private SDValue[] expandIntResSignExtendInreg(SDNode n) {}
    private SDValue[] expandIntResTruncate(SDNode n) {}
    private SDValue[] expandIntResZeroExtend(SDNode n) {}
    private SDValue[] expandIntResFPToSINT(SDNode n) {}
    private SDValue[] expandIntResFPToUINT(SDNode n) {}
    private SDValue[] expandIntResLogical(SDNode n) {}
    private SDValue[] expandIntResAddSub(SDNode n) {}
    private SDValue[] expandIntResAddSubc(SDNode n) {}
    private SDValue[] expandIntResAddSube(SDNode n) {}
    private SDValue[] expandIntResBSWAP(SDNode n) {}
    private SDValue[] expandIntResMul(SDNode n) {}
    private SDValue[] expandIntResSDIV(SDNode n) {}
    private SDValue[] expandIntResSREM(SDNode n) {}
    private SDValue[] expandIntResUDIV(SDNode n) {}
    private SDValue[] expandIntResUREM(SDNode n) {}
    private SDValue[] expandIntResShift(SDNode n) {}

    private SDValue[] expandShiftByConstant(SDNode n, int amt)
    {}

    private SDValue[] expandShiftWithKnownAmountBit(SDNode n)
    {}

    private SDValue expandShiftWithUnknownAmountBit(SDNode n)
    {}

    private boolean expandIntegerOperand(SDNode n, int operandNo)
    {}
    private SDValue expandIntOpBitConvert(SDNode n)
    {}
    private SDValue expandIntOpBRCC(SDNode n)
    {}
    private SDValue expandIntOpBuildVector(SDNode n)
    {}
    private SDValue expandIntOpExtractElement(SDNode n)
    {}
    private SDValue expandIntOpSelectCC(SDNode n)
    {}
    private SDValue expandIntOpSetCC(SDNode n)
    {}
    private SDValue expandIntOpShift(SDValue n)
    {}
    private SDValue expandIntOpSINTToFP(SDNode n)
    {}
    private SDValue expandIntOpStore(StoreSDNode n, int opNo)
    {}
    private SDValue expandIntOpTruncate(SDNode n)
    {}
    private SDValue expandIntOpUINTToFP(SDNode n)
    {}

    private SDValue[] integerExpandSetCCOperands(CondCode cc)
    {}

    private SDValue getSoftenedFloat(SDValue op)
    {}

    private void setSoftenedFloat(SDValue op, SDValue result)
    {}

    private void softenFloatResult(SDNode n, int resNo) {}
    private SDValue softenFloatRes_BIT_CONVERT(SDNode n) {}
    private SDValue softenFloatRes_BUILD_PAIR(SDNode n) {}
    private SDValue softenFloatRes_ConstantFP(ConstantFPSDNode n) {}
    private SDValue softenFloatRes_EXTRACT_VECTOR_ELT(SDNode n) {}
    private SDValue softenFloatRes_FABS(SDNode n) {}
    private SDValue softenFloatRes_FADD(SDNode n) {}
    private SDValue softenFloatRes_FCEIL(SDNode n) {}
    private SDValue softenFloatRes_FCOPYSIGN(SDNode n) {}
    private SDValue softenFloatRes_FCOS(SDNode n) {}
    private SDValue softenFloatRes_FDIV(SDNode n) {}
    private SDValue softenFloatRes_FEXP(SDNode n) {}
    private SDValue softenFloatRes_FEXP2(SDNode n) {}
    private SDValue softenFloatRes_FFLOOR(SDNode n) {}
    private SDValue softenFloatRes_FLOG(SDNode n) {}
    private SDValue softenFloatRes_FLOG2(SDNode n) {}
    private SDValue softenFloatRes_FLOG10(SDNode n) {}
    private SDValue softenFloatRes_FMUL(SDNode n) {}
    private SDValue softenFloatRes_FNEARBYINT(SDNode n) {}
    private SDValue softenFloatRes_FNEG(SDNode n) {}
    private SDValue softenFloatRes_FP_EXTEND(SDNode n) {}
    private SDValue softenFloatRes_FP_ROUND(SDNode n) {}
    private SDValue softenFloatRes_FPOW(SDNode n) {}
    private SDValue softenFloatRes_FPOWI(SDNode n) {}
    private SDValue softenFloatRes_FREM(SDNode n) {}
    private SDValue softenFloatRes_FRINT(SDNode n) {}
    private SDValue softenFloatRes_FSIN(SDNode n) {}
    private SDValue softenFloatRes_FSQRT(SDNode n) {}
    private SDValue softenFloatRes_FSUB(SDNode n) {}
    private SDValue softenFloatRes_FTRUNC(SDNode n) {}
    private SDValue softenFloatRes_LOAD(SDNode n) {}
    private SDValue softenFloatRes_SELECT(SDNode n) {}
    private SDValue softenFloatRes_SELECT_CC(SDNode n) {}
    private SDValue softenFloatRes_UNDEF(SDNode n) {}
    private SDValue softenFloatRes_VAARG(SDNode n) {}
    private SDValue softenFloatRes_XINT_TO_FP(SDNode n) {}

    private boolean softenFloatOperand(SDNode n, int resNo) {}
    private SDValue softenFloatOp_BIT_CONVERT(SDNode n) {}
    private SDValue softenFloatOp_BR_CC(SDNode n) {}
    private SDValue softenFloatOp_FP_ROUND(SDNode n) {}
    private SDValue softenFloatOp_FP_TO_SINT(SDNode n) {}
    private SDValue softenFloatOp_FP_TO_UINT(SDNode n) {}
    private SDValue softenFloatOp_SELECT_CC(SDNode n) {}
    private SDValue softenFloatOp_SETCC(SDNode n) {}
    private SDValue softenFloatOp_STORE(SDNode n, int opNo) {}

    private SDValue[] softenSetCCOperands(CondCode cc)
    {}

    private SDValue[] getExpandedFloat(SDValue op)
    {}
    private void setExpandedFloat(SDValue op, SDValue lo, SDValue hi)
    {}

    private void expandFloatResult(SDNode n, int resNo)
    {}

    private SDValue[] expandFloatRes_ConstantFP(SDNode n) {}
    private SDValue[] expandFloatRes_FABS      (SDNode n) {}
    private SDValue[] expandFloatRes_FADD      (SDNode n) {}
    private SDValue[] expandFloatRes_FCEIL     (SDNode n) {}
    private SDValue[] expandFloatRes_FCOS      (SDNode n) {}
    private SDValue[] expandFloatRes_FDIV      (SDNode n) {}
    private SDValue[] expandFloatRes_FEXP      (SDNode n) {}
    private SDValue[] expandFloatRes_FEXP2     (SDNode n) {}
    private SDValue[] expandFloatRes_FFLOOR    (SDNode n) {}
    private SDValue[] expandFloatRes_FLOG      (SDNode n) {}
    private SDValue[] expandFloatRes_FLOG2     (SDNode n) {}
    private SDValue[] expandFloatRes_FLOG10    (SDNode n) {}
    private SDValue[] expandFloatRes_FMUL      (SDNode n) {}
    private SDValue[] expandFloatRes_FNEARBYINT(SDNode n) {}
    private SDValue[] expandFloatRes_FNEG      (SDNode n) {}
    private SDValue[] expandFloatRes_FP_EXTEND (SDNode n) {}
    private SDValue[] expandFloatRes_FPOW      (SDNode n) {}
    private SDValue[] expandFloatRes_FPOWI     (SDNode n) {}
    private SDValue[] expandFloatRes_FRINT     (SDNode n) {}
    private SDValue[] expandFloatRes_FSIN      (SDNode n) {}
    private SDValue[] expandFloatRes_FSQRT     (SDNode n) {}
    private SDValue[] expandFloatRes_FSUB      (SDNode n) {}
    private SDValue[] expandFloatRes_FTRUNC    (SDNode n) {}
    private SDValue[] expandFloatRes_LOAD      (SDNode n) {}
    private SDValue[] expandFloatRes_XINT_TO_FP(SDNode n) {}

    private boolean expandFloatOperand(SDNode n, int operandNo) {}
    private SDValue expandFloatOp_BR_CC(SDNode n) {}
    private SDValue expandFloatOp_FP_ROUND(SDNode n) {}
    private SDValue expandFloatOp_FP_TO_SINT(SDNode n) {}
    private SDValue expandFloatOp_FP_TO_UINT(SDNode n) {}
    private SDValue expandFloatOp_SELECT_CC(SDNode n) {}
    private SDValue expandFloatOp_SETCC(SDNode n) {}
    private SDValue expandFloatOp_STORE(SDNode n, int opNo) {}

    private SDValue[] floatExpandSetCCOperands(CondCode cc) {}

    private SDValue getScalarizedVector(SDValue op)
    {
        SDValue
    }

    private void setScalarizedVector(SDValue op, SDValue result) {}
    private void scalarizeVectorResult(SDNode n, int opNo) {}
    private SDValue scalarizeVecRes_BinOp(SDNode n) {}
    private SDValue scalarizeVecRes_UnaryOp(SDNode n) {}

    private SDValue scalarizeVecRes_BIT_CONVERT(SDNode n) {}
    private SDValue scalarizeVecRes_CONVERT_RNDSAT(SDNode n) {}
    private SDValue scalarizeVecRes_EXTRACT_SUBVECTOR(SDNode n) {}
    private SDValue scalarizeVecRes_FPOWI(SDNode n) {}
    private SDValue scalarizeVecRes_INSERT_VECTOR_ELT(SDNode n) {}
    private SDValue scalarizeVecRes_LOAD(LoadSDNode n) {}
    private SDValue scalarizeVecRes_SCALAR_TO_VECTOR(SDNode n) {}
    private SDValue scalarizeVecRes_SELECT(SDNode n) {}
    private SDValue scalarizeVecRes_SELECT_CC(SDNode n) {}
    private SDValue scalarizeVecRes_SETCC(SDNode n) {}
    private SDValue scalarizeVecRes_UNDEF(SDNode n) {}
    private SDValue scalarizeVecRes_VECTOR_SHUFFLE(SDNode n) {}
    private SDValue scalarizeVecRes_VSETCC(SDNode n) {}

    // Vector Operand Scalarization: <1 x ty> -> ty.
    private boolean scalarizeVectorOperand(SDNode n, int opNo) {}
    private SDValue scalarizeVecOp_BIT_CONVERT(SDNode n) {}
    private SDValue scalarizeVecOp_CONCAT_VECTORS(SDNode n) {}
    private SDValue scalarizeVecOp_EXTRACT_VECTOR_ELT(SDNode n) {}
    private SDValue scalarizeVecOp_STORE(StoreSDNode n, int opNo) {}

    private SDValue[] getSplitVector(SDValue op) {}
    private void setSplitVector(SDValue op, SDValue lo, SDValue hi) {}

    // Vector Result Splitting: <128 x ty> -> 2 x <64 x ty>.
    private void splitVectorResult(SDNode n, int opNo) {}
    private SDValue[] splitVecRes_BinOp(SDNode n) {}
    private SDValue[] splitVecRes_UnaryOp(SDNode n) {}

    private SDValue[] splitVecRes_BIT_CONVERT(SDNode n) {}
    private SDValue[] splitVecRes_BUILD_PAIR(SDNode n) {}
    private SDValue[] splitVecRes_BUILD_VECTOR(SDNode n) {}
    private SDValue[] splitVecRes_CONCAT_VECTORS(SDNode n) {}
    private SDValue[] splitVecRes_CONVERT_RNDSAT(SDNode n) {}
    private SDValue[] splitVecRes_EXTRACT_SUBVECTOR(SDNode n) {}
    private SDValue[] splitVecRes_FPOWI(SDNode n) {}
    private SDValue[] splitVecRes_INSERT_VECTOR_ELT(SDNode n) {}
    private SDValue[] splitVecRes_LOAD(LoadSDNode n) {}
    private SDValue[] splitVecRes_SCALAR_TO_VECTOR(SDNode n) {}
    private SDValue[] splitVecRes_SETCC(SDNode n) {}
    private SDValue[] splitVecRes_UNDEF(SDNode n) {}
    private SDValue[] splitVecRes_VECTOR_SHUFFLE(ShuffleVectorSDNode n) {}

    // Vector Operand Splitting: <128 x ty> -> 2 x <64 x ty>.
    private boolean splitVectorOperand(SDNode n, int opNo) {}
    private SDValue splitVecOp_UnaryOp(SDNode n) {}

    private SDValue splitVecOp_BIT_CONVERT(SDNode n) {}
    private SDValue splitVecOp_EXTRACT_SUBVECTOR(SDNode n) {}
    private SDValue splitVecOp_EXTRACT_VECTOR_ELT(SDNode n) {}
    private SDValue splitVecOp_STORE(StoreSDNode n, int opNo) {}

    private SDValue getWidenedVector(SDValue op) 
    {
    
    }
    private void setWidenedVector(SDValue op, SDValue result) {}

    // Widen Vector Result Promotion.
    private void widenVectorResult(SDNode n, int ResNo) {}
    private SDValue widenVecRes_BIT_CONVERT(SDNode n) {}
    private SDValue widenVecRes_BUILD_VECTOR(SDNode n) {}
    private SDValue widenVecRes_CONCAT_VECTORS(SDNode n) {}
    private SDValue widenVecRes_CONVERT_RNDSAT(SDNode n) {}
    private SDValue widenVecRes_EXTRACT_SUBVECTOR(SDNode n) {}
    private SDValue widenVecRes_INSERT_VECTOR_ELT(SDNode n) {}
    private SDValue widenVecRes_LOAD(SDNode n) {}
    private SDValue widenVecRes_SCALAR_TO_VECTOR(SDNode n) {}
    private SDValue widenVecRes_SELECT(SDNode n) {}
    private SDValue widenVecRes_SELECT_CC(SDNode n) {}
    private SDValue widenVecRes_UNDEF(SDNode n) {}
    private SDValue widenVecRes_VECTOR_SHUFFLE(ShuffleVectorSDNode n) {}
    private SDValue widenVecRes_VSETCC(SDNode n) {}

    private SDValue widenVecRes_Binary(SDNode n) {}
    private SDValue widenVecRes_Convert(SDNode n) {}
    private SDValue widenVecRes_Shift(SDNode n) {}
    private SDValue widenVecRes_Unary(SDNode n) {}

    // Widen Vector Operand.
    private boolean widenVectorOperand(SDNode n, int ResNo){}
    private SDValue widenVecOp_BIT_CONVERT(SDNode n) {}
    private SDValue widenVecOp_CONCAT_VECTORS(SDNode n) {}
    private SDValue widenVecOp_EXTRACT_VECTOR_ELT(SDNode n) {}
    private SDValue widenVecOp_STORE(SDNode n) {}

    private SDValue widenVecOp_Convert(SDNode n) {}

    /**
     * Helper function to generate a set of loads to load a vector with a
     * resulting wider type. It takes
     * @param ldChain   list of chains for the load we have generated.
     * @param chain     incoming chain for the ld vector.
     * @param basePtr   base pointer to load from.
     * @param sv        memory disambiguation source value.
     * @param svOffset  memory disambiugation offset.
     * @param alignment alignment of the memory.
     * @param isVolatile    volatile load.
     * @param ldWidth       width of memory that we want to load.
     * @param resType       the wider result result type for the resulting vector.
     * @return
     */
    private SDValue genWidenVectorLoads(ArrayList<SDValue> ldChain, SDValue chain,
            SDValue basePtr, Value sv,
            int svOffset, int alignment,
            boolean isVolatile, int ldWidth,
            EVT resType)
    {}

    /**
     * Helper function to generate a set of
     * stores to store a widen vector into non widen memory
     * @param stChain   list of chains for the store we have generated.
     * @param chain     incoming chain for the ld vector.
     * @param basePtr   base pointer to store from.
     * @param sv        memory disambiguation source value.
     * @param svOffset  memory disambiugation offset.
     * @param alignment alignment of the memory.
     * @param isVolatile    volatile store.
     * @param valOp          value to store
     * @param stWidth       width of memory that we want to store.
     */
    private void genWidenVectorStores(ArrayList<SDValue> stChain, SDValue chain,
            SDValue basePtr, Value sv,
            int svOffset, int alignment,
            boolean isVolatile, SDValue valOp,
            int stWidth) {}

    /**
     * Modifies a vector input (widen or narrows) to a vector of NVT.  The
     * input vector must have the same element type as NVT.
     * @param InOp
     * @param WidenVT
     * @return
     */
    private SDValue ModifyToType(SDValue InOp, EVT WidenVT) {}

    private SDValue[] getSplitOp(SDValue op) 
    {
        
    }

    /**
     * Compute the VTs needed for the low/hi parts of a type
     * which is split (or expanded) into two not necessarily identical pieces.
     * @param inVT
     * @return
     */
    private EVT[] getSplitDestVTs(EVT inVT) 
    {}

    /**
     * Use {@linkplain ISD#EXTRACT_ELEMENT} nodes to extract the low and
     * high parts of the given value.
     * @param pair
     */
    private void getPairElements(SDValue pair) {}

    // Generic Result Splitting.
    private void splitRes_MERGE_VALUES(SDNode n) {}
    private void splitRes_SELECT      (SDNode n) {}
    private void splitRes_SELECT_CC   (SDNode n) {}
    private void splitRes_UNDEF       (SDNode n) {}

    //===--------------------------------------------------------------------===//
    // Generic Expansion: LegalizeTypesGeneric.cpp
    //===--------------------------------------------------------------------===//

    /**
     * Legalization methods which only use that the illegal type is split into two
     * identical types of half the size, and that the Lo/Hi part is stored first
     * in memory on little/big-endian machines, followed by the Hi/Lo part.  As
     * such they can be used for expanding integers and floats.
     * @param op
     * @return
     */
    private SDValue[] getExpandedOp(SDValue op) 
    {
    
    }

    // Generic Result Expansion.
    private void expandRes_BIT_CONVERT       (SDNode n) {}
    private void expandRes_BUILD_PAIR        (SDNode n) {}
    private void expandRes_EXTRACT_ELEMENT   (SDNode n) {}
    private void expandRes_EXTRACT_VECTOR_ELT(SDNode n) {}
    private void expandRes_NormalLoad        (SDNode n) {}
    private void expandRes_VAARG             (SDNode n) {}

    // Generic Operand Expansion.
    private SDValue expandOp_BIT_CONVERT      (SDNode n) {}
    private SDValue expandOp_BUILD_VECTOR     (SDNode n) {}
    private SDValue expandOp_EXTRACT_ELEMENT  (SDNode n) {}
    private SDValue expandOp_INSERT_VECTOR_ELT(SDNode n) {}
    private SDValue expandOp_SCALAR_TO_VECTOR (SDNode n) {}
    private SDValue expandOp_NormalStore      (SDNode n, int opNo)
    {}
}
