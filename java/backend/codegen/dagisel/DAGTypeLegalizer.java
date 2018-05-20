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
import backend.codegen.MVT;
import backend.codegen.RTLIB;
import backend.codegen.ValueTypeAction;
import backend.codegen.dagisel.SDNode.*;
import backend.codegen.fastISel.ISD;
import backend.target.TargetLowering;
import backend.type.Type;
import backend.value.ConstantInt;
import backend.value.Value;
import javafx.scene.shape.SVGPath;
import tools.APInt;
import tools.Pair;
import tools.Util;

import java.util.*;

import static backend.codegen.dagisel.CondCode.*;
import static backend.codegen.dagisel.DAGTypeLegalizer.NodeIdFlags.*;
import static backend.target.TargetLowering.LegalizeAction.Custom;

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

            op = analyzeNewValue(op);
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

    private SDValue analyzeNewValue(SDValue val)
    {
        val.setNode(analyzeNewNode(val.getNode()));
        if (val.getNode().getNodeID() == Processed)
            val = remapValue(val);
        return val;
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
        if (tli.getOperationAction(n.getOpcode(), vt) != Custom)
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
        to = analyzeNewValue(to);

        replacedValues.put(from, to);
        replaceValueWithHelper(from, to);
    }

    private void replaceValueWithHelper(SDValue from, SDValue to)
    {
        assert !from.getNode().equals(to.getNode()):"Potential legalization loop!";

        to = analyzeNewValue(to);

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
        result = analyzeNewValue(result);
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
        if (Util.DEBUG)
        {
            System.err.print("Promote integer result:");
            n.dump(dag);
            System.err.println();
        }
        SDValue res = new SDValue();
        if (customLowerNode(n, n.getValueType(resNo), true))
            return;

        switch (n.getOpcode())
        {
            default:
                if (Util.DEBUG)
                {
                    System.err.printf("promoteIntegerResult #%d: ", resNo);
                    n.dump(dag);
                    System.err.println();
                }
                Util.shouldNotReachHere("Don't know how to promote operator!");
                break;
            case ISD.AssertSext:
                res = promoteIntResAssertSext(n); break;
            case ISD.AssertZext:
                res = promoteIntResAssertZext(n); break;
            case ISD.BIT_CONVERT:
                res = promoteIntResBitConvert(n); break;
            case ISD.BSWAP:
                res = promoteIntResBSWAP(n); break;
            case ISD.BUILD_PAIR:
                res = promoteIntResBuildPair(n); break;
            case ISD.Constant:
                res = promoteIntResConstant(n); break;
            case ISD.CONVERT_RNDSAT:
                res = promoteIntResConvertRndsat(n); break;
            case ISD.CTLZ:
                res = promoteIntResCTLZ(n); break;
            case ISD.CTPOP:
                res = promoteIntResCTPOP(n); break;
            case ISD.CTTZ:
                res = promoteIntResCTTZ(n); break;
            case ISD.EXTRACT_VECTOR_ELT:
                res = promoteIntResExtractVectorElt(n); break;
            case ISD.LOAD:
                res = promoteIntResLoad((LoadSDNode) n); break;
            case ISD.SELECT:
                res = promoteIntResSELECT(n); break;
            case ISD.SELECT_CC:
                res = promoteIntResSELECTCC(n); break;
            case ISD.SETCC:
                res = promoteIntResSETCC(n); break;
            case ISD.SHL:
                res = promoteIntResSHL(n); break;
            case ISD.SIGN_EXTEND_INREG:
                res = promoteIntResSignExtendInreg(n); break;
            case ISD.SRA:
                res = promoteIntResSRA(n); break;
            case ISD.SRL:
                res = promoteIntResSRL(n); break;
            case ISD.TRUNCATE:
                res = promoteIntResTruncate(n); break;
            case ISD.UNDEF:
                res = promoteIntResUNDEF(n); break;
            case ISD.VAARG:
                res = promoteIntResVAARG(n); break;
            case ISD.SIGN_EXTEND:
            case ISD.ZERO_EXTEND:
            case ISD.ANY_EXTEND:
                res = promoteIntResIntExtend(n); break;
            case ISD.FP_TO_SINT:
            case ISD.FP_TO_UINT:
                res = promoteIntResFPToXInt(n); break;
            case ISD.AND:
            case ISD.OR:
            case ISD.XOR:
            case ISD.ADD:
            case ISD.SUB:
            case ISD.MUL:
                res = promoteIntResSimpleIntBinOp(n); break;
            case ISD.SDIV:
            case ISD.SREM:
                res = promoteIntResSDIV(n); break;
            case ISD.UDIV:
            case ISD.UREM:
                res = promoteIntResUDIV(n); break;
            case ISD.SADDO:
            case ISD.SSUBO:
                res = promoteIntResSADDSUBO(n, resNo); break;
            case ISD.UADDO:
            case ISD.USUBO:
                res = promoteIntResUADDSUBO(n, resNo); break;
            case ISD.SMULO:
            case ISD.UMULO:
                res = promoteIntResXMULO(n, resNo); break;
            case ISD.ATOMIC_LOAD_ADD:
            case ISD.ATOMIC_LOAD_SUB:
            case ISD.ATOMIC_LOAD_AND:
            case ISD.ATOMIC_LOAD_OR:
            case ISD.ATOMIC_LOAD_XOR:
            case ISD.ATOMIC_LOAD_NAND:
            case ISD.ATOMIC_LOAD_MIN:
            case ISD.ATOMIC_LOAD_MAX:
            case ISD.ATOMIC_LOAD_UMIN:
            case ISD.ATOMIC_LOAD_UMAX:
            case ISD.ATOMIC_SWAP:
                res = promoteIntResAtomic1((AtomicSDNode) n); break;
            case ISD.ATOMIC_CMP_SWAP:
                res = promoteIntResAtomic2((AtomicSDNode) n); break;
        }
        if (res.getNode() != null)
            setPromotedIntegers(new SDValue(n, resNo), res);
    }

    private SDValue promoteIntResAssertSext(SDNode n)
    {
        SDValue op = sextPromotedInteger(n.getOperand(0));
        return dag.getNode(ISD.AssertSext, op.getValueType(), op, n.getOperand(1));
    }
    private SDValue promoteIntResAssertZext(SDNode n)
    {
        SDValue op = sextPromotedInteger(n.getOperand(0));
        return dag.getNode(ISD.AssertZext, op.getValueType(), op, n.getOperand(1));
    }
    private SDValue promoteIntResAtomic1(AtomicSDNode n)
    {
        SDValue op2 = sextPromotedInteger(n.getOperand(2));
        SDValue res = dag.getAtomic(n.getOpcode(),
                n.getMemoryVT(),
                n.getChain(),
                n.getBasePtr(),
                op2, n.getSrcValue(),
                n.getAlignment());

        replaceValueWith(new SDValue(n, 1), res.getValue(1));
        return res;
    }
    private SDValue promoteIntResAtomic2(AtomicSDNode n)
    {
        SDValue op2 = getPromotedInteger(n.getOperand(2));
        SDValue op3 = getPromotedInteger(n.getOperand(3));
        SDValue res = dag.getAtomic(n.getOpcode(),
                n.getMemoryVT(),
                n.getChain(),
                n.getBasePtr(),
                op2, op3, n.getSrcValue(), n.getAlignment());
        replaceValueWith(new SDValue(n, 1), res.getValue(1));
        return res;
    }
    private SDValue promoteIntResBitConvert(SDNode n)
    {
        SDValue inOp = n.getOperand(0);
        EVT inVT = inOp.getValueType();
        EVT ninVT = tli.getTypeToTransformTo(inVT);
        EVT outVT = n.getValueType(0);
        EVT noutVT = tli.getTypeToTransformTo(outVT);
        switch (getTypeAction(inVT))
        {
            default:
                assert false:"Unknown type action!";
                break;
            case Legal:
            case ExpandInteger:
            case ExpandFloat:
                break;
            case PromotedInteger:
                if (noutVT.bitsEq(ninVT))
                    return dag.getNode(ISD.BIT_CONVERT, noutVT,
                            getPromotedInteger(inOp));
                break;
            case SoftenFloat:
                return dag.getNode(ISD.ANY_EXTEND, noutVT, getSoftenedFloat(inOp));
            case ScalarizeVector:
                return dag.getNode(ISD.ANY_EXTEND, noutVT,
                        bitConvertToInteger(getScalarizedVector(inOp)));
            case SplitVector:
            {
                SDValue[] res = getSplitVector(n.getOperand(0));
                SDValue lo = bitConvertToInteger(res[0]);
                SDValue hi = bitConvertToInteger(res[1]);
                if (tli.isBigEndian())
                {
                    SDValue temp = lo;
                    lo = hi;
                    hi = temp;
                }
                inOp = dag.getNode(ISD.ANY_EXTEND,
                        EVT.getIntegerVT(noutVT.getSizeInBits()),
                        joinIntegers(lo, hi));
                return dag.getNode(ISD.BIT_CONVERT, noutVT, inOp);
            }
            case WidenVector:
                if (outVT.bitsEq(ninVT))
                    return dag.getNode(ISD.BIT_CONVERT, outVT,
                            getWidenedVector(inOp));
                break;
        }
        return dag.getNode(ISD.ANY_EXTEND, noutVT,
                createStackStoreLoad(inOp, outVT));
    }
    private SDValue promoteIntResBSWAP(SDNode n)
    {
        SDValue op = getPromotedInteger(n.getOperand(0));
        EVT outVT = n.getValueType(0);
        EVT opVT = op.getValueType();
        int diffBits = opVT.getSizeInBits() - outVT.getSizeInBits();
        return dag.getNode(ISD.SRL, opVT, dag.getNode(ISD.BSWAP, opVT, op),
                dag.getConstant(diffBits, new EVT(tli.getPointerTy()), false));
    }
    private SDValue promoteIntResBuildPair(SDNode n)
    {
        return dag.getNode(ISD.ANY_EXTEND, tli.getTypeToTransformTo(n.getValueType(0)),
                joinIntegers(n.getOperand(0), n.getOperand(1)));
    }
    private SDValue promoteIntResConstant(SDNode n)
    {
        EVT vt = n.getValueType(0);
        int opc = vt.isByteSized() ? ISD.SIGN_EXTEND : ISD.ZERO_EXTEND;
        return dag.getNode(opc,
                tli.getTypeToTransformTo(vt),
                new SDValue(n, 0));
    }
    private SDValue promoteIntResConvertRndsat(SDNode n)
    {
        CvtCode cc = ((CvtRndSatSDNode)n).getCvtCode();
        assert cc == CvtCode.CVT_SS || cc == CvtCode.CVT_SU ||
                cc == CvtCode.CVT_US || cc == CvtCode.CVT_UU ||
                cc == CvtCode.CVT_SF || cc == CvtCode.CVT_UF;
        EVT outVT = tli.getTypeToTransformTo(n.getValueType(0));
        return dag.getConvertRndSat(outVT,
                n.getOperand(0), n.getOperand(1),
                n.getOperand(2),
                n.getOperand(3),
                n.getOperand(4),
                cc);
    }
    private SDValue promoteIntResCTLZ(SDNode n)
    {
        SDValue op = zextPromotedInteger(n.getOperand(0));
        EVT outVT = n.getValueType(0);
        EVT opVT = op.getValueType();

        op = dag.getNode(ISD.CTLZ, opVT, op);
        return dag.getNode(ISD.SUB, opVT, op,
                dag.getConstant(opVT.getSizeInBits() - outVT.getSizeInBits(),
                        opVT,false));
    }
    private SDValue promoteIntResCTTZ(SDNode n)
    {
        SDValue op = getPromotedInteger(n.getOperand(0));
        EVT opVT = op.getValueType();
        EVT outVT = n.getValueType(0);

        APInt topBit = new APInt(opVT.getSizeInBits(), 0);
        topBit.set(opVT.getSizeInBits());
        op = dag.getNode(ISD.OR, opVT, op, dag.getConstant(topBit, opVT, false));
        return dag.getNode(ISD.CTTZ, opVT, op);
    }
    private SDValue promoteIntResCTPOP(SDNode n)
    {
        SDValue op = zextPromotedInteger(n.getOperand(0));
        return dag.getNode(ISD.CTPOP, op.getValueType(), op);
    }
    private SDValue promoteIntResExtractVectorElt(SDNode n)
    {
        EVT outVT = tli.getTypeToTransformTo(n.getValueType(0));
        return dag.getNode(ISD.EXTRACT_VECTOR_ELT, outVT,
                n.getOperand(0), n.getOperand(1));
    }
    private SDValue promoteIntResFPToXInt(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        int newOpc = n.getOpcode();
        if (n.getOpcode() == ISD.FP_TO_UINT &&
            !tli.isOperationLegal(ISD.FP_TO_UINT, nvt) &&
            !tli.isOperationLegal(ISD.FP_TO_SINT, nvt))
            newOpc = ISD.FP_TO_SINT;
        SDValue res = dag.getNode(newOpc, nvt, n.getOperand(0));
        return dag.getNode(n.getOpcode() == ISD.FP_TO_UINT ?
                ISD.AssertZext : ISD.AssertSext, nvt, res,
                dag.getValueType(n.getValueType(0)));
    }
    private SDValue promoteIntResIntExtend(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        if (getTypeAction(n.getOperand(0).getValueType()) == LegalizeAction.PromotedInteger)
        {
            SDValue res = getPromotedInteger(n.getOperand(0));
            assert res.getValueType().bitsLE(nvt):"Extension doesn't make sense!";

            if (nvt.equals(res.getValueType()))
            {
                if (n.getOpcode() == ISD.SIGN_EXTEND)
                    return dag.getNode(ISD.SIGN_EXTEND_INREG, nvt, res,
                            dag.getValueType(n.getOperand(0).getValueType()));
                if (n.getOpcode() == ISD.ZERO_EXTEND)
                    return dag.getZeroExtendInReg(res, n.getOperand(0).getValueType());
                assert n.getOpcode() == ISD.ANY_EXTEND:"Unknown integer extension!";
                return res;
            }
        }
        return dag.getNode(n.getOpcode(), nvt, n.getOperand(0));
    }
    private SDValue promoteIntResLoad(LoadSDNode n)
    {
        assert  n.isUNINDEXEDLoad():"Indexed load during type legalization!";
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        LoadExtType ext = n.isNONExtLoad() ? LoadExtType.EXTLOAD : n.getExtensionType();
        SDValue res = dag.getExtLoad(ext,
                nvt, n.getChain(), n.getBasePtr(), n.getSrcValue(),
                n.getSrcValueOffset(),
                n.getMemoryVT(), n.isVolatile(),
                n.getAlignment());
        replaceValueWith(new SDValue(n, 1), res.getValue(1));
        return res;
    }
    private SDValue promoteIntResOverflow(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(1));
        EVT[] valueVTs = {n.getValueType(0), nvt};
        SDValue[] ops = {n.getOperand(0), n.getOperand(1)};
        SDValue res = dag.getNode(n.getOpcode(), dag.getVTList(valueVTs), ops);
        replaceValueWith(new SDValue(n, 0), res);
        return new SDValue(res.getNode(), 1);
    }
    private SDValue promoteIntResSADDSUBO(SDNode n, int resNo)
    {
        if (resNo == 1)
            return promoteIntResOverflow(n);

        SDValue lhs = sextPromotedInteger(n.getOperand(0));
        SDValue rhs = sextPromotedInteger(n.getOperand(1));
        EVT origVT = n.getOperand(0).getValueType();
        EVT promotedVT = lhs.getValueType();

        int opc = n.getOpcode() == ISD.SADDO ? ISD.ADD: ISD.SUB;
        SDValue res = dag.getNode(opc, promotedVT, lhs, rhs);

        SDValue off = dag.getNode(ISD.SIGN_EXTEND_INREG,
                promotedVT, res, dag.getValueType(origVT));
        off = dag.getSetCC(n.getValueType(1), off, res, CondCode.SETNE);
        replaceValueWith(new SDValue(n, 1), off);
        return res;
    }
    private SDValue promoteIntResSDIV(SDNode n)
    {
        SDValue lhs = sextPromotedInteger(n.getOperand(0));
        SDValue rhs = sextPromotedInteger(n.getOperand(1));
        return dag.getNode(n.getOpcode(), lhs.getValueType(),
                lhs, rhs);
    }
    private SDValue promoteIntResSELECT(SDNode n)
    {
        SDValue lhs = getPromotedInteger(n.getOperand(1));
        SDValue rhs = getPromotedInteger(n.getOperand(2));
        return dag.getNode(ISD.SETCC, lhs.getValueType(),
                n.getOperand(0), lhs, rhs);
    }
    private SDValue promoteIntResSELECTCC(SDNode n)
    {
        SDValue lhs = getPromotedInteger(n.getOperand(2));
        SDValue rhs = getPromotedInteger(n.getOperand(3));
        return dag.getNode(ISD.SELECT_CC, lhs.getValueType(),
                n.getOperand(0), n.getOperand(1), lhs, rhs,
                n.getOperand(4));
    }
    private SDValue promoteIntResSETCC(SDNode n)
    {
        EVT svt = new EVT(tli.getSetCCResultType(n.getOperand(0).getValueType()));
        assert isTypeLegal(svt):"Illegal SetCC type!";
        SDValue setcc = dag.getNode(ISD.SETCC, svt, n.getOperand(0),
                n.getOperand(1), n.getOperand(2));
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        assert nvt.bitsLE(svt):"Integer type overpromoted!";
        return dag.getNode(ISD.TRUNCATE, nvt, setcc);
    }
    private SDValue promoteIntResSHL(SDNode n)
    {
        return dag.getNode(ISD.SHL, tli.getTypeToTransformTo(n.getValueType(0)),
                getPromotedInteger(n.getOperand(0)), n.getOperand(1));
    }
    private SDValue promoteIntResSimpleIntBinOp(SDNode n)
    {
        SDValue lhs = getPromotedInteger(n.getOperand(0));
        SDValue rhs = getPromotedInteger(n.getOperand(1));
        return dag.getNode(n.getOpcode(), lhs.getValueType(), lhs, rhs);
    }
    private SDValue promoteIntResSignExtendInreg(SDNode n)
    {
        SDValue op = getPromotedInteger(n.getOperand(0));
        return dag.getNode(ISD.SIGN_EXTEND_INREG, op.getValueType(),
                op, n.getOperand(1));
    }
    private SDValue promoteIntResSRA(SDNode n)
    {
        SDValue res = sextPromotedInteger(n.getOperand(0));
        return dag.getNode(ISD.SRA, res.getValueType(), res, n.getOperand(1));
    }
    private SDValue promoteIntResSRL(SDNode n)
    {
        EVT vt = n.getValueType(0);
        EVT nvt = tli.getTypeToTransformTo(vt);
        SDValue res = zextPromotedInteger(n.getOperand(0));
        return dag.getNode(ISD.SRL, nvt, res, n.getOperand(1));
    }
    private SDValue promoteIntResTruncate(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue res = new SDValue();
        switch (getTypeAction(n.getOperand(0).getValueType()))
        {
            default:
                Util.shouldNotReachHere("Unknown type action!");
                break;
            case Legal:
            case ExpandInteger:
                res = n.getOperand(0);
                break;
            case PromotedInteger:
                res = getPromotedInteger(n.getOperand(0));
                break;
        }
        return dag.getNode(ISD.TRUNCATE, nvt, res);
    }
    private SDValue promoteIntResUADDSUBO(SDNode n, int resNo)
    {
        if (resNo == 1)
            return promoteIntResOverflow(n);

        SDValue lhs = zextPromotedInteger(n.getOperand(0));
        SDValue rhs = zextPromotedInteger(n.getOperand(1));
        EVT ovt = n.getOperand(0).getValueType();
        EVT nvt = lhs.getValueType();
        int opc = n.getOpcode() == ISD.UADDO ? ISD.ADD : ISD.SUB;
        SDValue res = dag.getNode(opc, nvt, lhs, rhs);

        SDValue ofl = dag.getZeroExtendInReg(res, ovt);
        ofl = dag.getSetCC(n.getValueType(1), ofl, res, CondCode.SETNE);

        replaceValueWith(new SDValue(n, 1), ofl);
        return res;
    }
    private SDValue promoteIntResUDIV(SDNode n)
    {
        SDValue lhs = zextPromotedInteger(n.getOperand(0));
        SDValue rhs = zextPromotedInteger(n.getOperand(1));
        return dag.getNode(n.getOpcode(), lhs.getValueType(),
                lhs, rhs);
    }
    private SDValue promoteIntResUNDEF(SDNode n)
    {
        return dag.getUNDEF(tli.getTypeToTransformTo(n.getValueType(0)));
    }
    private SDValue promoteIntResVAARG(SDNode n)
    {
        SDValue chain = n.getOperand(0);
        SDValue ptr = n.getOperand(1);
        EVT vt = n.getValueType(0);
        EVT regVT = tli.getRegisterType(vt);
        int numRegs = tli.getNumRegisters(vt);

        SDValue[] parts = new SDValue[numRegs];
        for (int i = 0; i < numRegs; i++)
        {
            parts[i] = dag.getVAArg(regVT, chain, ptr, n.getOperand(2));
            chain = parts[i].getValue(1);
        }

        if (tli.isBigEndian())
        {
            Util.reverse(parts);
        }
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue res = dag.getNode(ISD.ZERO_EXTEND, nvt, parts[0]);
        for (int i = 1; i < numRegs; i++)
        {
            SDValue part = dag.getNode(ISD.ZERO_EXTEND, nvt, parts[i]);
            part = dag.getNode(ISD.SHL, nvt, part, dag.getConstant(i*regVT.getSizeInBits(),
                    new EVT(tli.getPointerTy()), false));
            res = dag.getNode(ISD.OR, nvt, res, part);
        }
        replaceValueWith(new SDValue(n, 1), chain);
        return res;
    }
    private SDValue promoteIntResXMULO(SDNode n, int resNo)
    {
        assert resNo == 1;
        return promoteIntResOverflow(n);
    }

    private boolean promoteIntegerOperand(SDNode node, int opNo)
    {
        if (Util.DEBUG)
        {
            System.err.printf("Promote integer operand: ");
            node.dump(dag);
            System.err.println();
        }
        SDValue res = new SDValue();
        if (customLowerNode(node, node.getOperand(opNo).getValueType(), false))
            return false;

        switch (node.getOpcode())
        {
            default:
                System.err.printf("promoteIntegerOperand op#%d: ", opNo);
                Util.shouldNotReachHere("Don't know how to promote this operator's operand!");
                break;
            case ISD.ANY_EXTEND:
                res = promoteOpAnyExtend(node); break;
            case ISD.BIT_CONVERT:
                res = promoteOpBitConvert(node); break;
            case ISD.BR_CC:
                res = promoteOpBRCC(node, opNo); break;
            case ISD.BRCOND:
                res = promoteOpBRCond(node, opNo); break;
            case ISD.BUILD_PAIR:
                res = promoteOpBuildPair(node); break;
            case ISD.BUILD_VECTOR:
                res = promoteOpBuildVector(node); break;
            case ISD.CONVERT_RNDSAT:
                res = promoteOpConvertRndsat(node); break;
            case ISD.INSERT_VECTOR_ELT:
                res = promoteOpInsertVectorElt(node, opNo); break;
            case ISD.MEMBARRIER:
                res = promoteOpMemBarrier(node); break;
            case ISD.SCALAR_TO_VECTOR:
                res = promoteOpScalarToVector(node); break;
            case ISD.SELECT:
                res = promoteOpSelectCC(node, opNo); break;
            case ISD.SELECT_CC:
                res = promoteOpSelectCC(node, opNo); break;
            case ISD.SETCC:
                res = promoteOpSetCC(node, opNo); break;
            case ISD.SIGN_EXTEND:
                res = promoteOpSignExtend(node); break;
            case ISD.SINT_TO_FP:
                res = promoteOpSINTToFP(node); break;
            case ISD.STORE:
                res = promoteOpStore((StoreSDNode) node, opNo); break;
            case ISD.TRUNCATE:
                res = promoteOpTruncate(node); break;
            case ISD.UINT_TO_FP:
                res = promoteOpUINTToFP(node); break;
            case ISD.ZERO_EXTEND:
                res = promoteOpZeroExtend(node); break;
            case ISD.SHL:
            case ISD.SRA:
            case ISD.SRL:
            case ISD.ROTL:
            case ISD.ROTR:
                res = promoteOpShift(node); break;
        }
        if (res.getNode() == null)
            return false;

        if (res.getNode().equals(node))
            return true;

        assert res.getValueType().equals(node.getValueType(0)) && node
                .getNumValues() == 1;
        replaceValueWith(new SDValue(node, 0), res);
        return false;
    }

    private SDValue promoteOpAnyExtend(SDNode node)
    {
        SDValue op = getPromotedInteger(node.getOperand(0));
        return dag.getNode(ISD.ANY_EXTEND, node.getValueType(0), op);
    }
    private SDValue promoteOpBitConvert(SDNode node)
    {
        return createStackStoreLoad(node.getOperand(0), node.getValueType(0));
    }
    private SDValue promoteOpBuildPair(SDNode node)
    {
        EVT ovt = node.getOperand(0).getValueType();
        SDValue lo = zextPromotedInteger(node.getOperand(0));
        SDValue hi = getPromotedInteger(node.getOperand(1));
        assert lo.getValueType().equals(node.getValueType(0));
        hi = dag.getNode(ISD.SHL, node.getValueType(0), hi,
                dag.getConstant(ovt.getSizeInBits(), new EVT(tli.getPointerTy()),
                        false));
        return dag.getNode(ISD.OR, node.getValueType(0), lo, hi);
    }
    private SDValue promoteOpBRCC(SDNode node, int opNo)
    {
        assert opNo == 2;
        SDValue lhs = node.getOperand(2);
        SDValue rhs = node.getOperand(3);
        SDValue[] res = promoteSetCCOperands(lhs, rhs,
                ((CondCodeSDNode)node.getOperand(1).getNode()).getCondition());
        lhs = res[0];
        rhs = res[1];
        return dag.updateNodeOperands(new SDValue(node, 0),
                node.getOperand(0), node.getOperand(1), lhs, rhs,
                node.getOperand(4));

    }
    private SDValue promoteOpBRCond(SDNode node, int opNo)
    {
        assert opNo == 1;
        EVT svt = new EVT(tli.getSetCCResultType(new EVT(MVT.Other)));
        SDValue cond = promoteTargetBoolean(node.getOperand(1), svt);
        return dag.updateNodeOperands(new SDValue(node, 0),
                node.getOperand(0), cond, node.getOperand(2));
    }
    private SDValue promoteOpBuildVector(SDNode node)
    {
        EVT vecVT = node.getValueType(0);
        int numElts = vecVT.getVectorNumElements();
        assert (numElts & 1) == 0;
        assert node.getOperand(0).getValueType().getSizeInBits() >=
                node.getValueType(0).getVectorElementType().getSizeInBits();

        SDValue[] ops = new SDValue[numElts];
        for (int i = 0; i < numElts; i++)
            ops[i] = getPromotedInteger(node.getOperand(i));
        return dag.updateNodeOperands(new SDValue(node, 0), ops);
    }

    private SDValue promoteOpConvertRndsat(SDNode node)
    {
        CvtCode cc = ((CvtRndSatSDNode)node).getCvtCode();
        assert cc == CvtCode.CVT_SS || cc == CvtCode.CVT_SU ||
                cc == CvtCode.CVT_US || cc == CvtCode.CVT_UU ||
                cc == CvtCode.CVT_FS || cc == CvtCode.CVT_FU;
        SDValue inOp = getPromotedInteger(node.getOperand(0));
        return dag.getConvertRndSat(node.getValueType(0),
                inOp, node.getOperand(1),
                node.getOperand(2),
                node.getOperand(3),
                node.getOperand(4),
                cc);
    }

    private SDValue promoteOpInsertVectorElt(SDNode node, int opNo)
    {
        if (opNo == 1)
        {
            assert node.getOperand(1).getValueType().getSizeInBits() >= node
                    .getValueType(0).getVectorElementType().getSizeInBits();
            return dag.updateNodeOperands(new SDValue(node, 0),
                    node.getOperand(0), getPromotedInteger(node.getOperand(1)),
                    node.getOperand(2));
        }
        assert opNo == 2;
        SDValue idx = zextPromotedInteger(node.getOperand(2));
        return dag.updateNodeOperands(new SDValue(node, 0),
                node.getOperand(0),
                node.getOperand(1), idx);
    }
    private SDValue promoteOpMemBarrier(SDNode node)
    {
        SDValue[] ops = new SDValue[6];
        ops[0] = node.getOperand(0);
        for (int i = 0; i < ops.length; i++)
        {
            SDValue flag = getPromotedInteger(node.getOperand(0));
            ops[i] = dag.getZeroExtendInReg(flag, new EVT(MVT.i1));
        }
        return dag.updateNodeOperands(new SDValue(node, 0), ops);
    }
    private SDValue promoteOpScalarToVector(SDNode node)
    {
        return dag.updateNodeOperands(new SDValue(node, 0),
                getPromotedInteger(node.getOperand(0)));
    }
    private SDValue promoteOpSelect(SDNode node, int opNo)
    {
        assert opNo == 0;
        EVT svt = new EVT(tli.getSetCCResultType(node.getOperand(1).getValueType()));
        SDValue cond = promoteTargetBoolean(node.getOperand(0), svt);
        return dag.updateNodeOperands(new SDValue(node, 0), cond,
                node.getOperand(1), node.getOperand(2));
    }
    private SDValue promoteOpSelectCC(SDNode node, int opNo)
    {
        assert opNo == 0;
        SDValue lhs = node.getOperand(0);
        SDValue rhs = node.getOperand(1);
        promoteSetCCOperands(lhs, rhs, ((CondCodeSDNode)node.getOperand(4).getNode()).getCondition());
        return dag.updateNodeOperands(new SDValue(node, 0), lhs, rhs,
                node.getOperand(2), node.getOperand(3), node.getOperand(4));
    }
    private SDValue promoteOpSetCC(SDNode node, int opNo)
    {
        assert opNo == 0;
        SDValue lhs = node.getOperand(0);
        SDValue rhs = node.getOperand(1);
        promoteSetCCOperands(lhs, rhs, ((CondCodeSDNode)node.getOperand(2).getNode()).getCondition());
        return dag.updateNodeOperands(new SDValue(node, 0), lhs, rhs,
                node.getOperand(2));
    }
    private SDValue promoteOpShift(SDNode node)
    {
        return dag.updateNodeOperands(new SDValue(node, 0), node.getOperand(0),
                zextPromotedInteger(node.getOperand(1)));
    }
    private SDValue promoteOpSignExtend(SDNode node)
    {
        SDValue op = getPromotedInteger(node.getOperand(0));
        op = dag.getNode(ISD.ANY_EXTEND, node.getValueType(0), op);
        return dag.getNode(ISD.SIGN_EXTEND_INREG, op.getValueType(),
                op, dag.getValueType(node.getOperand(0).getValueType()));
    }
    private SDValue promoteOpSINTToFP(SDNode node)
    {
        return dag.updateNodeOperands(new SDValue(node, 0),
                sextPromotedInteger(node.getOperand(0)));
    }
    private SDValue promoteOpStore(StoreSDNode node, int opNo)
    {
        assert node.isUNINDEXEDLoad();
        SDValue ch = node.getChain(), ptr = node.getBasePtr();
        int svOffset = node.getSrcValueOffset();
        int alignment = node.getAlignment();
        boolean isVolatile = node.isVolatile();
        SDValue val = getPromotedInteger(node.getValue());
        return dag.getTruncStore(ch, val, ptr, node.getSrcValue(),
                svOffset, node.getMemoryVT(), isVolatile, alignment);
    }
    private SDValue promoteOpTruncate(SDNode node)
    {
        SDValue op = getPromotedInteger(node.getOperand(0));
        return dag.getNode(ISD.TRUNCATE, node.getValueType(0), op);
    }
    private SDValue promoteOpUINTToFP(SDNode node)
    {
        return dag.updateNodeOperands(new SDValue(node, 0),
                zextPromotedInteger(node.getOperand(0)));
    }
    private SDValue promoteOpZeroExtend(SDNode node)
    {
        SDValue op = getPromotedInteger(node.getOperand(0));
        op = dag.getNode(ISD.ZERO_EXTEND, node.getValueType(0), op);
        return dag.getZeroExtendInReg(op, node.getOperand(0).getValueType());
    }

    private SDValue[] promoteSetCCOperands(SDValue lhs, SDValue rhs, CondCode cc)
    {
        switch (cc)
        {
            default:
                Util.shouldNotReachHere("Unknown integer comparsion!");
                break;
            case SETEQ:
            case SETNE:
            case SETUGE:
            case SETUGT:
            case SETULE:
            case SETULT:
                lhs = zextPromotedInteger(lhs);
                rhs = zextPromotedInteger(rhs);
                break;
            case SETGE:
            case SETGT:
            case SETLT:
            case SETLE:
                lhs = sextPromotedInteger(lhs);
                rhs = sextPromotedInteger(rhs);
                break;
        }
        return new SDValue[]{lhs, rhs};
    }

    private SDValue[] getExpandedInteger(SDValue op)
    {
        if (!expandedIntegers.containsKey(op))
        {
            expandedIntegers.put(op, Pair.get(new SDValue(), new SDValue()));
        }
        Pair<SDValue, SDValue> entry = expandedIntegers.get(op);
        entry.first = remapValue(entry.first);
        entry.second = remapValue(entry.second);
        assert entry.first.getNode() != null:"Operand isn't expanded";
        return new SDValue[]{entry.first, entry.second};

    }

    private void setExpandedIntegers(SDValue op, SDValue lo, SDValue hi)
    {
        assert lo.getValueType().equals(tli.getTypeToTransformTo(op.getValueType()))
                && hi.getValueType().equals(lo.getValueType()):"Invalid type for expanded integer";
        lo = analyzeNewValue(lo);
        hi = analyzeNewValue(hi);
        assert !expandedIntegers.containsKey(op):"Node already expanded!";
        expandedIntegers.put(op, Pair.get(lo, hi));
    }

    private void expandIntegerResult(SDNode n, int resNo)
    {
        if (Util.DEBUG)
        {
            System.err.print("Expand integer result: ");
            n.dump(dag);
            System.err.println();
        }
        SDValue[] res = null;
        if (customLowerNode(n, n.getValueType(resNo), true))
            return;

        switch (n.getOpcode())
        {
            default:
                if (Util.DEBUG)
                {
                    System.err.printf("expandIntegerResult #%d: ", resNo);
                    n.dump(dag);
                    System.err.println();
                }
                Util.shouldNotReachHere("Don't how to expand the result of this operator");
                break;
            case ISD.MERGE_VALUES:
                res = splitRes_MERGE_VALUES(n); break;
            case ISD.SELECT:
                res = splitRes_SELECT(n); break;
            case ISD.SELECT_CC:
                res = splitRes_SELECT_CC(n); break;
            case ISD.UNDEF:
                res = splitRes_UNDEF(n); break;
            case ISD.BIT_CONVERT:
                res = expandRes_BIT_CONVERT(n); break;
            case ISD.BUILD_PAIR:
                res = expandRes_BUILD_PAIR(n); break;
            case ISD.EXTRACT_ELEMENT:
                res = expandRes_EXTRACT_ELEMENT(n); break;
            case ISD.EXTRACT_VECTOR_ELT:
                res = expandRes_EXTRACT_VECTOR_ELT(n); break;
            case ISD.VAARG:
                res = expandRes_VAARG(n); break;
            case ISD.ANY_EXTEND:
                res = expandIntResAnyExtend(n); break;
            case ISD.AssertSext:
                res = expandIntResAssertSext(n); break;
            case ISD.AssertZext:
                res = expandIntResAssertZext(n); break;
            case ISD.BSWAP:
                res = expandIntResBSWAP(n); break;
            case ISD.Constant:
                res = expandIntResConstant(n); break;
            case ISD.CTLZ:
                res = expandIntResCTLZ(n); break;
            case ISD.CTPOP:
                res = expandIntResCTPOP(n); break;
            case ISD.CTTZ:
                res = expandIntResCTTZ(n); break;
            case ISD.FP_TO_SINT:
                res = expandIntResFPToSINT(n); break;
            case ISD.FP_TO_UINT:
                res = expandIntResFPToUINT(n); break;
            case ISD.LOAD:
                res = expandIntResLoad((LoadSDNode) n); break;
            case ISD.MUL:
                res = expandIntResMul(n); break;
            case ISD.SDIV:
                res = expandIntResSDIV(n); break;
            case ISD.SIGN_EXTEND:
                res = expandIntResSignExtend(n); break;
            case ISD.SIGN_EXTEND_INREG:
                res = expandIntResSignExtendInreg(n); break;
            case ISD.SREM:
                res = expandIntResSREM(n); break;
            case ISD.TRUNCATE:
                res = expandIntResTruncate(n); break;
            case ISD.UDIV:
                res = expandIntResUDIV(n); break;
            case ISD.UREM:
                res = expandIntResUREM(n); break;
            case ISD.ZERO_EXTEND:
                res = expandIntResZeroExtend(n); break;

            case ISD.AND:
            case ISD.OR:
            case ISD.XOR:
                res = expandIntResLogical(n); break;
            case ISD.ADD:
            case ISD.SUB:
                res = expandIntResAddSub(n); break;
            case ISD.ADDC:
            case ISD.SUBC:
                res = expandIntResAddSubc(n); break;
            case ISD.ADDE:
            case ISD.SUBE:
                res = expandIntResAddSube(n); break;
            case ISD.SHL:
            case ISD.SRA:
            case ISD.SRL:
                res = expandIntResShift(n); break;
        }
        assert res != null && res.length == 2:"Illegal status!";
        SDValue lo = res[0];
        SDValue hi = res[1];
        if (lo.getNode() != null)
            setExpandedIntegers(new SDValue(n, resNo), lo, hi);
    }
    private SDValue[] expandIntResAnyExtend(SDNode n)
    {
        SDValue[] res = new SDValue[2];
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue op = n.getOperand(0);
        if (op.getValueType().bitsLE(nvt))
        {
            res[0] = dag.getNode(ISD.ANY_EXTEND, nvt, op);
            res[1] = dag.getUNDEF(nvt);
        }
        else
        {
            assert getTypeAction(op.getValueType()) == LegalizeAction.PromotedInteger:
                    "Only know how to promote this result!";
            SDValue t = getPromotedInteger(op);
            assert t.getValueType().equals(n.getValueType(0)):"Operand over promoted!";
            res = splitInteger(t);
        }
        return res;
    }
    private SDValue[] expandIntResAssertSext(SDNode n)
    {
        SDValue[] res;
        res = getExpandedInteger(n.getOperand(0));
        assert res.length == 2 :"Illegal status!";
        EVT nvt = res[0].getValueType();
        EVT evt = ((VTSDNode)n.getOperand(1).getNode()).getVT();
        int nvtBits = nvt.getSizeInBits();
        int evtBits = evt.getSizeInBits();

        if (nvtBits < evtBits)
            res[1] = dag.getNode(ISD.AssertSext,
                    nvt, res[1], dag.getValueType(EVT.getIntegerVT(
                            evtBits - nvtBits)));
        else
        {
            res[0] = dag.getNode(ISD.AssertSext, nvt, res[0], dag.getValueType(evt));
            res[1] = dag.getNode(ISD.SRA, nvt, res[0], dag.getConstant(nvtBits-1,
                    new EVT(tli.getPointerTy()), false));
        }
        return res;
    }
    private SDValue[] expandIntResAssertZext(SDNode n)
    {
        SDValue[] res;
        res = getExpandedInteger(n.getOperand(0));
        assert res.length == 2 :"Illegal status!";
        EVT nvt = res[0].getValueType();
        EVT evt = ((VTSDNode)n.getOperand(1).getNode()).getVT();
        int nvtBits = nvt.getSizeInBits();
        int evtBits = evt.getSizeInBits();

        if (nvtBits < evtBits)
            res[1] = dag.getNode(ISD.AssertZext,
                    nvt, res[1], dag.getValueType(EVT.getIntegerVT(
                            evtBits - nvtBits)));
        else
        {
            res[0] = dag.getNode(ISD.AssertZext, nvt, res[0], dag.getValueType(evt));
            res[1] = dag.getConstant(0, nvt, false);
        }
        return res;
    }
    private SDValue[] expandIntResConstant(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        int nvtBits = nvt.getSizeInBits();
        APInt cst = ((ConstantSDNode)n).getAPIntValue();
        SDValue[] res = new SDValue[2];
        res[0] = dag.getConstant(new APInt(cst).trunc(nvtBits), nvt, false);
        res[1] = dag.getConstant(cst.lshr(nvtBits).trunc(nvtBits), nvt, false);
        return res;
    }
    private SDValue[] expandIntResCTLZ(SDNode n)
    {
        SDValue[] res = getExpandedInteger(n.getOperand(0));
        EVT nvt = res[0].getValueType();
        SDValue hiNotZero = dag.getSetCC(
                new EVT(tli.getSetCCResultType(nvt)),
                res[1], dag.getConstant(0, nvt, false), CondCode.SETNE);
        SDValue loLZ = dag.getNode(ISD.CTLZ, nvt, res[0]);
        SDValue hiLZ = dag.getNode(ISD.CTLZ, nvt, res[1]);
        res[0] = dag.getNode(ISD.SELECT, nvt, hiNotZero, hiLZ,
                dag.getNode(ISD.ADD, nvt, loLZ,
                        dag.getConstant(nvt.getSizeInBits(), nvt, false)));
        res[1] = dag.getConstant(0, nvt, false);
        return res;
    }
    private SDValue[] expandIntResCTPOP(SDNode n)
    {
        SDValue[] res = getExpandedInteger(n.getOperand(0));
        EVT nvt = res[0].getValueType();
        res[0] = dag.getNode(ISD.ADD, nvt, dag.getNode(ISD.CTPOP, nvt, res[0]),
                dag.getNode(ISD.CTPOP, nvt, res[1]));
        res[1] = dag.getConstant(0, nvt, false);
        return res;
    }
    private SDValue[] expandIntResCTTZ(SDNode n)
    {
        SDValue[] res = getExpandedInteger(n.getOperand(0));
        EVT nvt = res[0].getValueType();

        SDValue loNotZero = dag.getSetCC(new EVT(tli.getSetCCResultType(nvt)),
                res[0], dag.getConstant(0, nvt, false), CondCode.SETNE);
        SDValue loLZ = dag.getNode(ISD.CTTZ, nvt,res[0]);
        SDValue hiLZ = dag.getNode(ISD.CTTZ, nvt,res[1]);
        res[0] = dag.getNode(ISD.SELECT, nvt, loNotZero, loLZ,
                dag.getNode(ISD.ADD, nvt, hiLZ, dag.getConstant(nvt.getSizeInBits(),
                        nvt, false)));
        res[1] = dag.getConstant(0, nvt, false);
        return res;
    }
    private SDValue[] expandIntResLoad(LoadSDNode n)
    {
        if (n.isNormalLoad())
        {
            return expandRes_NormalLoad(n);
        }

        assert n.isUNINDEXEDLoad():"indexed load during type legalization!";

        EVT vt = n.getValueType(0);
        EVT nvt = tli.getTypeToTransformTo(vt);
        SDValue ch = n.getChain();
        SDValue ptr = n.getBasePtr();
        LoadExtType ext = n.getExtensionType();
        int svOffset = n.getSrcValueOffset();
        int alignment = n.getAlignment();
        boolean isVolatile = n.isVolatile();

        assert nvt.isByteSized():"expanded type not byte sized";
        SDValue lo, hi;
        if (n.getMemoryVT().bitsLE(nvt))
        {
            EVT evt = n.getMemoryVT();
            lo = dag.getExtLoad(ext, nvt, ch, ptr, n.getSrcValue(),
                    svOffset, evt, isVolatile, alignment);
            ch = lo.getValue(1);
            if (ext == LoadExtType.SEXTLOAD)
            {
                int loSize = lo.getValueType().getSizeInBits();
                hi = dag.getNode(ISD.SRA, nvt, lo, dag.getConstant(loSize-1,
                        new EVT(tli.getPointerTy()), false));
            }
            else if (ext == LoadExtType.ZEXTLOAD)
            {
                hi = dag.getConstant(0, nvt, false);
            }
            else
            {
                assert ext == LoadExtType.EXTLOAD:"Unknown extload!";
                hi = dag.getUNDEF(nvt);
            }
        }
        else if (tli.isLittleEndian())
        {
            lo = dag.getLoad(nvt, ch, ptr, n.getSrcValue(), svOffset, isVolatile,
                    alignment);
            int excessBits = n.getMemoryVT().getSizeInBits() - nvt.getSizeInBits();
            EVT nevt = EVT.getIntegerVT(excessBits);
            int incrementSize = nvt.getSizeInBits()/8;
            ptr = dag.getNode(ISD.ADD, ptr.getValueType(), ptr,
                    dag.getIntPtrConstant(incrementSize));
            hi = dag.getExtLoad(ext, nvt, ch, ptr, n.getSrcValue(),
                    svOffset+incrementSize, nevt,
                    isVolatile, Util.minAlign(alignment, incrementSize));
            ch = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other),
                    lo.getValue(1), hi.getValue(1));
        }
        else
        {
            EVT evt = n.getMemoryVT();
            int ebytes = evt.getStoreSizeInBits()/8;
            int increemntSize = nvt.getSizeInBits()/8;
            int excessBits = (ebytes - increemntSize)<<3;

            hi = dag.getExtLoad(ext, nvt, ch, ptr, n.getSrcValue(), svOffset,
                    EVT.getIntegerVT(evt.getSizeInBits() - excessBits),
                    isVolatile, alignment);
            ptr = dag.getNode(ISD.ADD, ptr.getValueType(), ptr, dag.getIntPtrConstant(increemntSize));
            lo = dag.getExtLoad(LoadExtType.ZEXTLOAD, nvt, ch, ptr, n.getSrcValue(),
                    svOffset+increemntSize,
                    EVT.getIntegerVT(excessBits),
                    isVolatile, Util.minAlign(alignment, increemntSize));

            ch = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), lo.getValue(1),
                    hi.getValue(1));

            if (excessBits < nvt.getSizeInBits())
            {
                lo = dag.getNode(ISD.OR, nvt,lo, dag.getNode(ISD.SHL, nvt, hi,
                        dag.getConstant(excessBits, new EVT(tli.getPointerTy()), false)));
                hi = dag.getNode(ext == LoadExtType.SEXTLOAD ?
                ISD.SRA: ISD.SRL, nvt, hi, dag.getConstant(nvt.getSizeInBits() -
                    excessBits, new EVT(tli.getPointerTy()), false));
            }
            replaceValueWith(new SDValue(n, 1), ch);
        }
        return new SDValue[]{lo, hi};
    }

    private SDValue makeLibCall(RTLIB libCall, EVT retVT,
            SDValue[] ops, boolean isSigned)
    {
        ArrayList<ArgListEntry> args = new ArrayList<>(ops.length);
        for (int i = 0; i < ops.length; i++)
        {
            ArgListEntry entry = new ArgListEntry();
            entry.node = ops[i];
            entry.ty = entry.node.getValueType().getTypeForEVT();
            entry.isSExt = isSigned;
            entry.isZExt = !isSigned;
            args.add(entry);
        }
        SDValue callee = dag.getExternalSymbol(tli.getLibCallName(libCall),
                new EVT(tli.getPointerTy()));
        Type retTy = retVT.getTypeForEVT();
        Pair<SDValue, SDValue> callInfo =
                tli.lowerCallTo(dag.getEntryNode(), retTy, isSigned,
                        !isSigned, false, false, 0,
                        tli.getLibCallCallingConv(libCall), false, true,
                        callee, args, dag);
        return callInfo.first;
    }

    private SDValue[] expandIntResSignExtend(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue op = n.getOperand(0);
        SDValue lo, hi;
        if (op.getValueType().bitsLE(nvt))
        {
            lo = dag.getNode(ISD.SIGN_EXTEND, nvt, n.getOperand(0));
            int loSize = nvt.getSizeInBits();
            hi = dag.getNode(ISD.SRA, nvt,lo, dag.getConstant(loSize-1,
                    new EVT(tli.getPointerTy()), false));
        }
        else
        {
            assert getTypeAction(op.getValueType()) == LegalizeAction.PromotedInteger;
            SDValue res = getPromotedInteger(op);
            assert res.getValueType().equals(n.getValueType(0)):"Operand over promoted!";
            SDValue[] t = splitInteger(res);
            lo = t[0];
            hi = t[1];
            int excessBits = op.getValueType().getSizeInBits() - nvt.getSizeInBits();
            hi = dag.getNode(ISD.SIGN_EXTEND_INREG, hi.getValueType(),
                    hi, dag.getValueType(EVT.getIntegerVT(excessBits)));
        }
        return new SDValue[]{lo, hi};
    }
    private SDValue[] expandIntResSignExtendInreg(SDNode n)
    {
        SDValue[] res = getExpandedInteger(n.getOperand(0));
        EVT vt = ((VTSDNode)n.getOperand(1).getNode()).getVT();

        if (vt.bitsLE(res[0].getValueType()))
        {
            res[0] = dag.getNode(ISD.SIGN_EXTEND_INREG, res[0].getValueType(),
                    res[0], n.getOperand(1));
            res[1] = dag.getNode(ISD.SRA, res[1].getValueType(), res[0],
                    dag.getConstant(res[1].getValueType().getSizeInBits()-1,
                            new EVT(tli.getPointerTy()), false));
        }
        else
        {
            int excessBits = vt.getSizeInBits() - res[0].getValueType().getSizeInBits();
            res[1] = dag.getNode(ISD.SIGN_EXTEND_INREG, res[1].getValueType(),
                    res[1], dag.getValueType(EVT.getIntegerVT(excessBits)));
        }
        return res;
    }
    private SDValue[] expandIntResTruncate(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue lo, hi;
        lo = dag.getNode(ISD.TRUNCATE, nvt, n.getOperand(0));
        hi = dag.getNode(ISD.SRL, n.getOperand(0).getValueType(), n.getOperand(0),
                dag.getConstant(nvt.getSizeInBits(), new EVT(tli.getPointerTy()), false));
        hi = dag.getNode(ISD.TRUNCATE, nvt, hi);
        return new SDValue[]{lo, hi};
    }
    private SDValue[] expandIntResZeroExtend(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue op = n.getOperand(0);
        SDValue lo, hi;
        if (op.getValueType().bitsLE(nvt))
        {
            lo = dag.getNode(ISD.ZERO_EXTEND, nvt, n.getOperand(0));
            hi = dag.getConstant(0, nvt,false);
        }
        else
        {
            assert getTypeAction(op.getValueType()) == LegalizeAction.PromotedInteger:
                    "Don't know how to handle this result!";
            SDValue res = getPromotedInteger(op);
            assert res.getValueType().equals(n.getValueType(0)):"Operand over promoted!";
            SDValue[] t = splitInteger(res);
            lo = t[0];
            hi = t[1];
            int excessBits = op.getValueType().getSizeInBits() - nvt.getSizeInBits();
            hi = dag.getZeroExtendInReg(hi, EVT.getIntegerVT(excessBits));
        }
        return new SDValue[]{lo, hi};
    }
    private SDValue[] expandIntResFPToSINT(SDNode n)
    {
        EVT vt = n.getValueType(0);
        SDValue op = n.getOperand(0);
        RTLIB libCall = tli.getFPTOSINT(op.getValueType(), vt);
        assert libCall != RTLIB.UNKNOWN_LIBCALL:"Unexpected fp-to-sint conversion!";
        return splitInteger(makeLibCall(libCall, vt, new SDValue[] { op }, true));
    }
    private SDValue[] expandIntResFPToUINT(SDNode n)
    {
        EVT vt = n.getValueType(0);
        SDValue op = n.getOperand(0);
        RTLIB libCall = tli.getFPTOUINT(op.getValueType(), vt);
        assert libCall != RTLIB.UNKNOWN_LIBCALL:"Unexpected fp-to-uint conversion!";
        return splitInteger(makeLibCall(libCall, vt, new SDValue[] { op }, true));
    }
    private SDValue[] expandIntResLogical(SDNode n)
    {
        SDValue[] res0 = getExpandedInteger(n.getOperand(0));
        SDValue[] res1 = getExpandedInteger(n.getOperand(1));
        return new SDValue[]{
                dag.getNode(n.getOpcode(),  res0[0].getValueType(), res0[0], res1[0]),
                dag.getNode(n.getOpcode(),  res0[0].getValueType(), res0[1], res1[1])
        };
    }
    private SDValue[] expandIntResAddSub(SDNode n)
    {
        SDValue[] res = new SDValue[2];
        SDValue[] lhsT = getExpandedInteger(n.getOperand(0));
        SDValue[] rhsT = getExpandedInteger(n.getOperand(1));
        EVT nvt = lhsT[0].getValueType();

        SDValue[] loOps = {lhsT[0], rhsT[0]};
        SDValue[] hiOps = {lhsT[1], rhsT[1], null};

        boolean hasCarry = tli.isOperationLegalOrCustom(n.getOpcode() == ISD.ADD ?
                ISD.ADDC : ISD.SUBC , tli.getTypeToExpandTo(nvt));
        if (hasCarry)
        {
            SDVTList vts = dag.getVTList(nvt, new EVT(MVT.Flag));
            if (n.getOpcode() == ISD.ADD)
            {
                res[0] = dag.getNode(ISD.ADDC, vts, loOps);
                hiOps[2] = res[0].getValue(1);
                res[1] = dag.getNode(ISD.ADDE, vts, hiOps);
            }
            else
            {
                res[0] = dag.getNode(ISD.SUBC, vts, loOps);
                hiOps[2] = res[0].getValue(1);
                res[1] = dag.getNode(ISD.SUBE, vts, hiOps);
            }
        }
        else
        {
            if (n.getOpcode() == ISD.ADD)
            {
                res[0] = dag.getNode(ISD.ADD, nvt, loOps);
                res[1] = dag.getNode(ISD.ADD, nvt, hiOps);
                SDValue cmp1 = dag.getSetCC(new EVT(tli.getSetCCResultType(nvt)),
                        res[0], loOps[0], CondCode.SETULT);
                SDValue carry1 = dag.getNode(ISD.SELECT, nvt, cmp1,
                        dag.getConstant(1, nvt, false),
                        dag.getConstant(0, nvt, false));
                SDValue cmp2 = dag.getSetCC(new EVT(tli.getSetCCResultType(nvt)),
                        res[0], loOps[1], CondCode.SETULT);
                SDValue carry2 = dag.getNode(ISD.SELECT, nvt, cmp2,
                        dag.getConstant(1, nvt, false),
                        carry1);
                res[1] = dag.getNode(ISD.ADD, nvt, res[1], carry2);
            }
            else
            {
                res[0] = dag.getNode(ISD.SUB, nvt, loOps);
                res[1] = dag.getNode(ISD.SUBC, nvt, hiOps);
                SDValue cmp = dag.getSetCC(new EVT(tli.getSetCCResultType(
                        loOps[0].getValueType())),
                        loOps[0], loOps[1], CondCode.SETULT);
                SDValue borrow = dag.getNode(ISD.SELECT, nvt, cmp,
                        dag.getConstant(1, nvt, false),
                        dag.getConstant(0, nvt, false));
                res[1] = dag.getNode(ISD.SUB, nvt, res[1], borrow);
            }
        }
        return res;
    }
    private SDValue[] expandIntResAddSubc(SDNode n)
    {
        SDValue[] res = new SDValue[2];
        SDValue[] lhsT = getExpandedInteger(n.getOperand(0));
        SDValue[] rhsT = getExpandedInteger(n.getOperand(1));
        SDVTList vts = dag.getVTList(lhsT[0].getValueType(), new EVT(MVT.Flag));
        SDValue[] loOps = {lhsT[0], rhsT[0]};
        SDValue[] hiOps = {lhsT[1], rhsT[1], null};

        if (n.getOpcode() == ISD.ADDC)
        {
            res[0] = dag.getNode(ISD.ADDC, vts, loOps);
            hiOps[2] = res[0].getValue(1);
            res[1] = dag.getNode(ISD.ADDE, vts, hiOps);
        }
        else
        {
            res[0] = dag.getNode(ISD.SUBC, vts, loOps);
            hiOps[2] = res[0].getValue(1);
            res[1] = dag.getNode(ISD.SUBE, vts, hiOps);
        }

        replaceValueWith(new SDValue(n, 1), res[1].getValue(1));
        return res;
    }
    private SDValue[] expandIntResAddSube(SDNode n)
    {
        SDValue[] res = new SDValue[2];
        SDValue[] lhsT = getExpandedInteger(n.getOperand(0));
        SDValue[] rhsT = getExpandedInteger(n.getOperand(1));
        SDVTList vts = dag.getVTList(lhsT[0].getValueType(), new EVT(MVT.Flag));
        SDValue[] loOps = {lhsT[0], rhsT[0], n.getOperand(2)};
        SDValue[] hiOps = {lhsT[1], rhsT[1]};

        res[0] = dag.getNode(n.getOpcode(), vts, loOps);
        hiOps[2] = res[0].getValue(1);
        res[1] = dag.getNode(n.getOpcode(), vts, hiOps);
        replaceValueWith(new SDValue(n, 1), res[1].getValue(1));
        return res;
    }
    private SDValue[] expandIntResBSWAP(SDNode n)
    {
        SDValue[] res = getExpandedInteger(n.getOperand(0));
        return new SDValue[]{dag.getNode(ISD.BSWAP, res[0].getValueType(), res[0]),
                dag.getNode(ISD.BSWAP, res[1].getValueType(), res[1])
        };
    }
    private SDValue[] expandIntResMul(SDNode n)
    {
        EVT vt = n.getValueType(0);
        EVT nvt = tli.getTypeToTransformTo(vt);
        boolean hasMULHS = tli.isOperationLegal(ISD.MULHS, nvt);
        boolean hasMULHU = tli.isOperationLegal(ISD.MULHU, nvt);
        boolean hasSMUL_LOHI = tli.isOperationLegal(ISD.SMUL_LOHI, nvt);
        boolean hasUMUL_LOHI = tli.isOperationLegal(ISD.UMUL_LOHI, nvt);
        SDValue lo = new SDValue(), hi = new SDValue();
        if (hasMULHU || hasMULHS || hasUMUL_LOHI || hasSMUL_LOHI)
        {
            SDValue[] res0 = getExpandedInteger(n.getOperand(0));
            SDValue[] res1 = getExpandedInteger(n.getOperand(1));
            int outerBitsize = vt.getSizeInBits();
            int innerBitsize = nvt.getSizeInBits();
            int lhssb = dag.computeNumSignBits(n.getOperand(0));
            int rhssb = dag.computeNumSignBits(n.getOperand(1));

            APInt highMask = APInt.getHighBitsSet(outerBitsize, innerBitsize);
            if (dag.maskedValueIsZero(n.getOperand(0), highMask) && dag
                    .maskedValueIsZero(n.getOperand(1), highMask))
            {
                if (hasUMUL_LOHI)
                {
                    return new SDValue[] { lo = dag.getNode(ISD.UMUL_LOHI,
                            dag.getVTList(nvt, nvt), res0[0], res1[0]),
                            new SDValue(lo.getNode(), 1) };
                }
                if (hasSMUL_LOHI)
                {
                    return new SDValue[] {
                            dag.getNode(ISD.MUL, nvt, res0[0], res1[0]),
                            dag.getNode(ISD.MULHU, nvt, res0[1], res1[1]) };
                }
            }
            if (lhssb > innerBitsize && rhssb > innerBitsize)
            {
                if (hasSMUL_LOHI)
                {
                    return new SDValue[] { lo = dag.getNode(ISD.SMUL_LOHI,
                            dag.getVTList(nvt, nvt), res0[0], res1[0]),
                            new SDValue(lo.getNode(), 1) };
                }
                if (hasMULHS)
                {
                    return new SDValue[] {
                            dag.getNode(ISD.MUL, nvt, res0[0], res1[0]),
                            dag.getNode(ISD.MULHS, nvt, res0[0], res1[0]) };
                }
            }
            if (hasUMUL_LOHI)
            {
                SDValue umulLOHI = dag
                        .getNode(ISD.UMUL_LOHI, dag.getVTList(nvt, nvt), res0[0], res1[0]);
                lo = umulLOHI;
                hi = umulLOHI.getValue(1);
                res1[1] = dag.getNode(ISD.MUL, nvt, res0[0], res1[1]);
                res0[1] = dag.getNode(ISD.MUL, nvt, res0[1], res1[0]);
                hi = dag.getNode(ISD.ADD, nvt, hi, res0[1]);
                hi = dag.getNode(ISD.ADD, nvt, hi, res0[1]);
                return new SDValue[] { lo, hi };
            }
            if (hasMULHU)
            {
                lo = dag.getNode(ISD.MUL, nvt, res0[0], res1[0]);
                hi = dag.getNode(ISD.MULHU, nvt, res0[0], res1[0]);
                res1[1] = dag.getNode(ISD.MUL, nvt, res0[0], res1[1]);
                res0[1] = dag.getNode(ISD.MUL, nvt, res0[1], res1[0]);
                hi = dag.getNode(ISD.ADD, nvt, hi, res1[1]);
                hi = dag.getNode(ISD.ADD, nvt, hi, res0[1]);
                return new SDValue[] { lo, hi };
            }
        }

        RTLIB lc = RTLIB.UNKNOWN_LIBCALL;
        switch (vt.getSimpleVT().simpleVT)
        {
            case MVT.i16:
                lc = RTLIB.MUL_I16;
                break;
            case MVT.i32:
                lc = RTLIB.MUL_I32;
                break;
            case MVT.i64:
                lc = RTLIB.MUL_I64;
                break;
            case MVT.i128:
                lc = RTLIB.MUL_I128;
                break;
        }
        assert lc != RTLIB.UNKNOWN_LIBCALL:"Unsupported mul!";
        SDValue[] ops = {n.getOperand(0), n.getOperand(1)};
        return splitInteger(makeLibCall(lc, vt, ops, true));
    }
    private SDValue[] expandIntResSDIV(SDNode n)
    {
        EVT vt = n.getValueType(0);
        RTLIB lc = RTLIB.UNKNOWN_LIBCALL;
        switch (vt.getSimpleVT().simpleVT)
        {
            case MVT.i16:
                lc = RTLIB.SDIV_I16;
                break;
            case MVT.i32:
                lc = RTLIB.SDIV_I32;
                break;
            case MVT.i64:
                lc = RTLIB.SDIV_I64;
                break;
            case MVT.i128:
                lc = RTLIB.SDIV_I128;
                break;
        }
        assert lc != RTLIB.UNKNOWN_LIBCALL:"Unsupported SDIV!";
        SDValue[] ops = {n.getOperand(0), n.getOperand(1)};
        return splitInteger(makeLibCall(lc, vt, ops, true));
    }
    private SDValue[] expandIntResSREM(SDNode n)
    {
        EVT vt = n.getValueType(0);
        RTLIB lc = RTLIB.UNKNOWN_LIBCALL;
        switch (vt.getSimpleVT().simpleVT)
        {
            case MVT.i16:
                lc = RTLIB.SREM_I16;
                break;
            case MVT.i32:
                lc = RTLIB.SREM_I32;
                break;
            case MVT.i64:
                lc = RTLIB.SREM_I64;
                break;
            case MVT.i128:
                lc = RTLIB.SREM_I128;
                break;
        }
        assert lc != RTLIB.UNKNOWN_LIBCALL:"Unsupported SREM!";
        SDValue[] ops = {n.getOperand(0), n.getOperand(1)};
        return splitInteger(makeLibCall(lc, vt, ops, true));
    }
    private SDValue[] expandIntResUDIV(SDNode n)
    {
        EVT vt = n.getValueType(0);
        RTLIB lc = RTLIB.UNKNOWN_LIBCALL;
        switch (vt.getSimpleVT().simpleVT)
        {
            case MVT.i16:
                lc = RTLIB.UDIV_I16;
                break;
            case MVT.i32:
                lc = RTLIB.UDIV_I32;
                break;
            case MVT.i64:
                lc = RTLIB.UDIV_I64;
                break;
            case MVT.i128:
                lc = RTLIB.UDIV_I128;
                break;
        }
        assert lc != RTLIB.UNKNOWN_LIBCALL:"Unsupported UDIV!";
        SDValue[] ops = {n.getOperand(0), n.getOperand(1)};
        return splitInteger(makeLibCall(lc, vt, ops, true));
    }
    private SDValue[] expandIntResUREM(SDNode n)
    {
        EVT vt = n.getValueType(0);
        RTLIB lc = RTLIB.UNKNOWN_LIBCALL;
        switch (vt.getSimpleVT().simpleVT)
        {
            case MVT.i16:
                lc = RTLIB.UREM_I16;
                break;
            case MVT.i32:
                lc = RTLIB.UREM_I32;
                break;
            case MVT.i64:
                lc = RTLIB.UREM_I64;
                break;
            case MVT.i128:
                lc = RTLIB.UREM_I128;
                break;
        }
        assert lc != RTLIB.UNKNOWN_LIBCALL:"Unsupported UREM!";
        SDValue[] ops = {n.getOperand(0), n.getOperand(1)};
        return splitInteger(makeLibCall(lc, vt, ops, true));
    }
    private SDValue[] expandIntResShift(SDNode n)
    {
        EVT vt = n.getValueType(0);
        if (n.getOperand(1).getNode() instanceof ConstantSDNode)
        {
            ConstantSDNode cn = (ConstantSDNode)n.getOperand(1).getNode();
            return expandShiftByConstant(n, cn.getZExtValue());
        }
        SDValue[] res = new SDValue[2];
        if (expandShiftWithKnownAmountBit(n, res))
            return res;

        int partsOpc;
        if (n.getOpcode() == ISD.SHL)
            partsOpc = ISD.SHL_PARTS;
        else if (n.getOpcode() == ISD.SRL)
            partsOpc = ISD.SRL_PARTS;
        else
        {
            assert n.getOpcode() == ISD.SRA;
            partsOpc = ISD.SRA_PARTS;
        }

        EVT nvt = tli.getTypeToTransformTo(vt);
        TargetLowering.LegalizeAction action = tli.getOperationAction(partsOpc, nvt);
        if ((action == TargetLowering.LegalizeAction.Legal && tli.isTypeLegal(nvt)) ||
                action == Custom)
        {
            SDValue[] t = getExpandedInteger(n.getOperand(0));

            SDValue[] ops = {t[0], t[1], n.getOperand(1)};
            vt = t[0].getValueType();
            res[0] = dag.getNode(partsOpc, dag.getVTList(vt, vt), ops);
            res[1] = res[0].getValue(1);
            return res;
        }

        RTLIB lc = RTLIB.UNKNOWN_LIBCALL;
        boolean isSigned = false;
        switch (n.getOpcode())
        {
            case ISD.SHL:
            {
                switch (vt.getSimpleVT().simpleVT)
                {
                    case MVT.i16:
                        lc = RTLIB.SHL_I16; break;
                    case MVT.i32:
                        lc = RTLIB.SHL_I32; break;
                    case MVT.i64:
                        lc = RTLIB.SHL_I64; break;
                    case MVT.i128:
                        lc = RTLIB.SHL_I128; break;
                }
                break;
            }
            case ISD.SRL:
            {
                switch (vt.getSimpleVT().simpleVT)
                {
                    case MVT.i16:
                        lc = RTLIB.SRL_I16; break;
                    case MVT.i32:
                        lc = RTLIB.SRL_I32; break;
                    case MVT.i64:
                        lc = RTLIB.SRL_I64; break;
                    case MVT.i128:
                        lc = RTLIB.SRL_I128; break;
                }
                break;
            }
            default:
                assert n.getOpcode() == ISD.SRA;
                isSigned = true;
                switch (vt.getSimpleVT().simpleVT)
                {
                    case MVT.i16:
                        lc = RTLIB.SRA_I16; break;
                    case MVT.i32:
                        lc = RTLIB.SRA_I32; break;
                    case MVT.i64:
                        lc = RTLIB.SRA_I64; break;
                    case MVT.i128:
                        lc = RTLIB.SRA_I128; break;
                }
                break;
        }
        if(lc != RTLIB.UNKNOWN_LIBCALL && tli.getLibCallName(lc) != null)
        {
            SDValue[] ops = {n.getOperand(0), n.getOperand(1)};
            return splitInteger(makeLibCall(lc, vt, ops, isSigned));
        }

        if (!expandShiftWithUnknownAmountBit(n, res))
        {
            Util.shouldNotReachHere("Unsupported shift!");
        }
        return res;
    }

    private SDValue[] expandShiftByConstant(SDNode n, long amt)
    {
        SDValue[] res = new SDValue[2];
        SDValue[] t = getExpandedInteger(n.getOperand(0));
        EVT nvt = t[0].getValueType();
        int vtBits = n.getValueType(0).getSizeInBits();
        int nvtBits = nvt.getSizeInBits();
        EVT shTy = n.getOperand(1).getValueType();
        if (n.getOpcode() == ISD.SHL)
        {
            if (amt > vtBits)
            {
                res[0] = res[1] = dag.getConstant(0, nvt, false);
            }
            else if (amt > nvtBits)
            {
                res[0] = dag.getConstant(0, nvt, false);
                res[1] = dag.getNode(ISD.SHL, nvt, t[0],
                        dag.getConstant(amt-nvtBits, shTy, false));
            }
            else if (amt == nvtBits)
            {
                res[0] = dag.getConstant(0, nvt, false);
                res[1] = t[0];
            }
            else if (amt == 1 && tli.isOperationLegal(ISD.ADDC,
                    tli.getTypeToExpandTo(nvt)))
            {
                SDVTList vts = dag.getVTList(nvt, new EVT(MVT.Flag));
                SDValue[] ops = {t[0], t[0]};
                res[0] = dag.getNode(ISD.ADDC, vts, ops);
                SDValue[] ops3 = {t[1], t[1], res[0].getValue(1)};
                res[1] = dag.getNode(ISD.ADDE, vts, ops3);
            }
            else
            {
                res[0] = dag.getNode(ISD.SHL, nvt, t[0], dag.getConstant(amt, shTy, false));
                res[1] = dag.getNode(ISD.OR, nvt, dag.getNode(ISD.SHL,
                        nvt, t[1], dag.getConstant(amt, shTy, false)),
                        dag.getNode(ISD.SRL, nvt, t[0],
                                dag.getConstant(nvtBits-amt, shTy, false)));
            }
            return res;
        }
        if (n.getOpcode() == ISD.SRL)
        {
            if (amt > vtBits)
            {
                res[0] = dag.getConstant(0, nvt, false);
                res[1] = dag.getConstant(0, nvt, false);
            }
            else if (amt > nvtBits)
            {
                res[0] = dag.getNode(ISD.SRL, nvt,
                        t[1], dag.getConstant(amt-nvtBits, shTy, false));
                res[1] = dag.getConstant(0, nvt, false);
            }
            else if (amt == nvtBits)
            {
                res[0] = t[1];
                res[1] = dag.getConstant(0, nvt, false);
            }
            else
            {
                res[0] = dag.getNode(ISD.OR, nvt,
                        dag.getNode(ISD.SRL, nvt, t[0], dag.getConstant(amt, shTy, false)),
                        dag.getNode(ISD.SHL, nvt, t[1], dag.getConstant(nvtBits-amt, shTy, false)));
                res[1] = dag.getNode(ISD.SRL, nvt, t[1], dag.getConstant(amt, shTy, false));
            }
            return res;
        }
        assert n.getOpcode() == ISD.SRA:"Unkown shift!";
        if (amt > vtBits)
            res[0] =res[1] = dag.getNode(ISD.SRA, nvt, t[1], dag.getConstant(nvtBits-1, shTy, false));
        else if (amt > nvtBits)
        {
            res[0] = dag.getNode(ISD.SRA, nvt, t[1], dag.getConstant(amt-nvtBits, shTy, false));
            res[1] = dag.getNode(ISD.SRA, nvt, t[1], dag.getConstant(nvtBits-1, shTy, false));
        }
        else if (amt == nvtBits)
        {
            res[0] = t[1];
            res[1] = dag.getNode(ISD.SRA, nvt, t[1], dag.getConstant(nvtBits-1, shTy, false));
        }
        else
        {
            res[0] = dag.getNode(ISD.OR, nvt, dag.getNode(ISD.SRL, nvt, t[0],
                                    dag.getConstant(amt, shTy, false)),
                    dag.getNode(ISD.SHL, nvt, t[1], dag.getConstant(nvtBits-amt, shTy, false)));
            res[1] = dag.getNode(ISD.SRA, nvt, t[1], dag.getConstant(amt, shTy, false));
        }
        return res;
    }

    private boolean expandShiftWithKnownAmountBit(SDNode n, SDValue[] res)
    {
        SDValue amt = n.getOperand(1);
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        EVT shTy = amt.getValueType();
        int shBits = shTy.getSizeInBits();
        int nvtBits = nvt.getSizeInBits();
        assert Util.isPowerOf2(nvtBits):"Expanded integer type size must be power of two!";
        APInt highBitMask = APInt.getHighBitsSet(shBits, shBits - Util.log2(nvtBits));
        APInt[] t = new APInt[2];
        dag.computeMaskedBits(n.getOperand(1), highBitMask, t, 0);
        APInt knownZero = t[0], knownOne = t[1];

        if ((knownZero.or(knownOne).and(highBitMask).eq(0)))
            return false;

        SDValue[] tt = getExpandedInteger(n.getOperand(0));

        if (knownOne.intersects(highBitMask))
        {
            amt = dag.getNode(ISD.AND, shTy, amt,
                    dag.getConstant(highBitMask.not(), shTy, false));
            switch (n.getOpcode())
            {
                default:
                    Util.shouldNotReachHere("Unknown shift!");
                    break;
                case ISD.SHL:
                    res[0] = dag.getConstant(0, nvt,false);
                    res[1] = dag.getNode(ISD.SHL, nvt, tt[0], amt);
                    return true;
                case ISD.SRL:
                    res[1] = dag.getConstant(0, nvt,false);
                    res[0] = dag.getNode(ISD.SRL, nvt, tt[1], amt);
                    return true;
                case ISD.SRA:
                    res[1] = dag.getNode(ISD.SRA, nvt, tt[1], dag.getConstant(nvtBits-1, shTy, false));
                    res[0] = dag.getNode(ISD.SRA, nvt, tt[1], amt);
                    return true;
            }
        }
        return false;
    }

    private boolean expandShiftWithUnknownAmountBit(SDNode n, SDValue[] res)
    {
        SDValue amt = n.getOperand(1);
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        EVT shTy = amt.getValueType();
        int nvtBits = nvt.getSizeInBits();
        assert Util.isPowerOf2(nvtBits):"Expanded integer type size not a power of two!";

        SDValue[] t = getExpandedInteger(n.getOperand(0));
        SDValue nvBitsNode = dag.getConstant(nvtBits, shTy, false);
        SDValue amt2 = dag.getNode(ISD.SUB, shTy, nvBitsNode, amt);
        SDValue cmp = dag.getSetCC(new EVT(tli.getSetCCResultType(shTy)),
                amt, nvBitsNode, CondCode.SETULT);

        SDValue lo1, hi1, lo2, hi2;
        switch (n.getOpcode())
        {
            case ISD.SHL:
                lo1 = dag.getConstant(0, nvt, false);
                hi1 = dag.getNode(ISD.SHL, nvt, t[0], amt);
                lo2 = dag.getNode(ISD.SHL, nvt, t[0], amt);
                hi2 = dag.getNode(ISD.OR, nvt,
                        dag.getNode(ISD.SHL, nvt, t[1], amt),
                        dag.getNode(ISD.SRL, nvt, t[0], amt2));
                res[0] = dag.getNode(ISD.SELECT, nvt, cmp, lo1, lo2);
                res[1] = dag.getNode(ISD.SELECT, nvt, cmp, hi1, hi2);
                return true;
            case ISD.SRL:
                hi1 = dag.getConstant(0, nvt, false);
                lo1 = dag.getNode(ISD.SRL, nvt, t[1], amt);
                hi2 = dag.getNode(ISD.SRL, nvt, t[1], amt);
                lo2 = dag.getNode(ISD.OR, nvt,
                        dag.getNode(ISD.SRL, nvt, t[0], amt),
                        dag.getNode(ISD.SHL, nvt, t[1], amt2));
                res[0] = dag.getNode(ISD.SELECT, nvt, cmp, lo1, lo2);
                res[1] = dag.getNode(ISD.SELECT, nvt, cmp, hi1, hi2);
                return true;
            case ISD.SRA:
                hi1 = dag.getNode(ISD.SRA, nvt, t[1], dag.getConstant(nvtBits-1,shTy,false));
                lo1 = dag.getNode(ISD.SRA, nvt, t[1], amt);

                hi2 = dag.getNode(ISD.SRA, nvt, t[1], amt);
                lo2 = dag.getNode(ISD.OR, nvt,
                        dag.getNode(ISD.SRL, nvt, t[0], amt),
                        dag.getNode(ISD.SHL, nvt, t[1], amt2));

                res[0] = dag.getNode(ISD.SELECT, nvt, cmp, lo1, lo2);
                res[1] = dag.getNode(ISD.SELECT, nvt, cmp, hi1, hi2);
                return true;
        }
        return false;
    }

    private boolean expandIntegerOperand(SDNode n, int opNo)
    {
        if (Util.DEBUG)
        {
            System.err.print("Expand integer operand: ");
            n.dump(dag);
            System.err.println();
        }
        SDValue res = new SDValue();

        if (customLowerNode(n, n.getOperand(opNo).getValueType(), false))
            return false;

        switch (n.getOpcode())
        {
            default:
                if (Util.DEBUG)
                {
                    System.err.printf("expandIntegerOperand op#%d: ", opNo);
                    n.dump(dag);
                    System.err.println();
                }
                Util.shouldNotReachHere("Don't know how to expand this operator's operand");
                break;
            case ISD.BIT_CONVERT:
                res = expandOp_BIT_CONVERT(n); break;
            case ISD.BR_CC:
                res = expandIntOpBRCC(n); break;
            case ISD.BUILD_VECTOR:
                res = expandIntOpBuildVector(n); break;
            case ISD.EXTRACT_ELEMENT:
                res = expandIntOpExtractElement(n); break;
            case ISD.INSERT_VECTOR_ELT:
                res = expandOp_INSERT_VECTOR_ELT(n); break;
            case ISD.SCALAR_TO_VECTOR:
                res = expandOp_SCALAR_TO_VECTOR(n); break;
            case ISD.SELECT_CC:
                res = expandIntOpSelectCC(n); break;
            case ISD.SETCC:
                res = expandIntOpSetCC(n); break;
            case ISD.SINT_TO_FP:
                res = expandIntOpSINTToFP(n); break;
            case ISD.STORE:
                res = expandIntOpStore((StoreSDNode) n, opNo); break;
            case ISD.TRUNCATE:
                res = expandIntOpTruncate(n); break;
            case ISD.UINT_TO_FP:
                res = expandIntOpUINTToFP(n); break;
            case ISD.SHL:
            case ISD.SRA:
            case ISD.SRL:
            case ISD.ROTL:
            case ISD.ROTR:
                res = expandIntOpShift(n); break;
        }
        if (res.getNode() == null) return false;

        if (res.getNode().equals(n))
            return true;

        assert res.getValueType().equals(n.getValueType(0)) &&
                n.getNumValues() == 1:"Invalid operand expansion!";
        replaceValueWith(new SDValue(n, 0), res);
        return false;
    }

    private SDValue expandIntOpBitConvert(SDNode n)
    {
        assert false:"Unimplemented!";
        return null;
    }

    private SDValue expandIntOpBRCC(SDNode n)
    {
        SDValue newLHS = n.getOperand(2);
        SDValue newRHS = n.getOperand(3);
        CondCode cc = ((CondCodeSDNode)n.getOperand(1).getNode()).getCondition();
        SDValue[] t = integerExpandSetCCOperands(newLHS, newRHS, cc);
        newLHS = t[0];
        newRHS = t[1];
        if (newRHS.getNode() == null)
        {
            newRHS = dag.getConstant(0, newLHS.getValueType(), false);
            cc = CondCode.SETNE;
        }
        return dag.updateNodeOperands(new SDValue(n, 0),n.getOperand(0),
                dag.getCondCode(cc), newLHS, newRHS, n.getOperand(4));
    }
    private SDValue expandIntOpBuildVector(SDNode n)
    {
        assert false:"Unimplemented!";
        return null;
    }
    private SDValue expandIntOpExtractElement(SDNode n)
    {
        assert false:"Unimplemented!";
        return null;
    }
    private SDValue expandIntOpSelectCC(SDNode n)
    {
        SDValue newLHS = n.getOperand(0);
        SDValue newRHS = n.getOperand(1);
        CondCode cc = ((CondCodeSDNode)n.getOperand(4).getNode()).getCondition();
        SDValue[] t = integerExpandSetCCOperands(newLHS, newRHS, cc);
        newLHS = t[0];
        newRHS = t[1];
        if (newRHS.getNode() == null)
        {
            newRHS = dag.getConstant(0, newLHS.getValueType(), false);
            cc = CondCode.SETNE;
        }
        return dag.updateNodeOperands(new SDValue(n, 0),
                newLHS, newRHS, n.getOperand(2), n.getOperand(3),
                dag.getCondCode(cc));
    }
    private SDValue expandIntOpSetCC(SDNode n)
    {
        SDValue newLHS = n.getOperand(0);
        SDValue newRHS = n.getOperand(1);
        CondCode cc = ((CondCodeSDNode)n.getOperand(2).getNode()).getCondition();
        SDValue[] t = integerExpandSetCCOperands(newLHS, newRHS, cc);
        newLHS = t[0];
        newRHS = t[1];
        if (newRHS.getNode() == null)
        {
            assert newLHS.getValueType().equals(n.getValueType(0)):"Unexected setcc expansion!";
            return newLHS;
        }
        return dag.updateNodeOperands(new SDValue(n, 0),
                newLHS, newRHS,dag.getCondCode(cc));
    }
    private SDValue expandIntOpShift(SDNode n)
    {
        SDValue[] t = getExpandedInteger(n.getOperand(1));
        return dag.updateNodeOperands(new SDValue(n, 0), n.getOperand(0),
                t[0]);
    }
    private SDValue expandIntOpSINTToFP(SDNode n)
    {
        SDValue op = n.getOperand(0);
        EVT destVT = n.getValueType(0);
        RTLIB lc = tli.getSINTTOFP(op.getValueType(), destVT);
        assert lc != RTLIB.UNKNOWN_LIBCALL;
        return makeLibCall(lc, destVT, new SDValue[]{op}, true);
    }
    private SDValue expandIntOpStore(StoreSDNode n, int opNo)
    {
        if (n.isNormalStore())
            return expandOp_NormalStore(n, opNo);

        assert n.isUNINDEXEDStore() : "Indexed store during type legalization?";
        assert opNo == 1 : "Can only expand the stored value so far?";

        EVT vt = n.getOperand(1).getValueType();
        EVT evt = tli.getTypeToTransformTo(vt);
        SDValue ch = n.getChain();
        SDValue ptr = n.getBasePtr();
        int svOffset = n.getSrcValueOffset();
        int alignment = n.getAlignment();
        boolean isVolatile = n.isVolatile();
        SDValue lo, hi;

        assert evt.isByteSized() : "Expanded type not byte sized!";
        if (n.getMemoryVT().bitsLE(evt))
        {
            SDValue[] t = getExpandedInteger(n.getValue());
            lo = t[0];
            hi = t[1];
            return dag.getTruncStore(ch, lo, ptr, n.getSrcValue(), svOffset, n.getMemoryVT(), isVolatile, alignment);
        }
        else if (tli.isLittleEndian())
        {
            SDValue[] t = getExpandedInteger(n.getValue());
            lo = t[0];
            hi = t[1];
            lo = dag.getStore(ch, lo, ptr, n.getSrcValue(), svOffset,
                    isVolatile, alignment);
            int excessBit = n.getMemoryVT().getSizeInBits() - evt.getSizeInBits();
            EVT nevt = EVT.getIntegerVT(excessBit);

            int incrementSize = evt.getSizeInBits()/8;
            ptr = dag.getNode(ISD.ADD, ptr.getValueType(), ptr,
                    dag.getIntPtrConstant(incrementSize));
            hi = dag.getTruncStore(ch, hi, ptr, n.getSrcValue(),
                    svOffset+incrementSize, nevt,
                    isVolatile, Util.minAlign(alignment, incrementSize));
            return dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), lo, hi);
        }
        else
        {
            SDValue[] t = getExpandedInteger(n.getValue());
            lo = t[0];
            hi = t[1];
            EVT extVT = n.getMemoryVT();
            int bytes = extVT.getStoreSizeInBits()/8;
            int incrementSize = evt.getSizeInBits()/8;
            int excessBits = (bytes - incrementSize)*8;
            EVT hiVT = EVT.getIntegerVT(extVT.getSizeInBits()-excessBits);

            if (excessBits < evt.getSizeInBits())
            {
                hi = dag.getNode(ISD.SHL, evt, hi, dag.getConstant(evt.getSizeInBits()
                - excessBits, new EVT(tli.getPointerTy()), false));
                hi = dag.getNode(ISD.OR, evt, hi, dag.getNode(ISD.SRL,
                        evt, lo, dag.getConstant(excessBits, new EVT(tli.getPointerTy()), false)));
            }

            hi = dag.getTruncStore(ch, hi, ptr, n.getSrcValue(), svOffset, hiVT, isVolatile, alignment);
            ptr = dag.getNode(ISD.ADD, ptr.getValueType(), ptr, dag.getIntPtrConstant(incrementSize));
            lo = dag.getTruncStore(ch, lo, ptr,n.getSrcValue(), svOffset+incrementSize,
                    EVT.getIntegerVT(excessBits), isVolatile, Util.minAlign(alignment, incrementSize));
            return dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), lo, hi);
        }
    }
    private SDValue expandIntOpTruncate(SDNode n)
    {
        SDValue[] t = getExpandedInteger(n.getOperand(0));
        return dag.getNode(ISD.TRUNCATE, n.getValueType(0), t[0]);
    }
    private SDValue expandIntOpUINTToFP(SDNode n)
    {
        SDValue op = n.getOperand(0);
        EVT srcVT = op.getValueType();
        EVT destVT = n.getValueType(0);

        if (tli.getOperationAction(ISD.SINT_TO_FP, srcVT) == Custom)
        {
            SDValue signedConv = dag.getNode(ISD.SINT_TO_FP, destVT, op);
            signedConv = tli.lowerOperation(signedConv, dag);

            long f32TwoE32 = 0x4F800000L;
            long f32TwoE64  = 0x5F800000L;
            long f32TwoE128 = 0x7F800000L;
            APInt ff = new APInt(32, 0);
            if (srcVT.getSimpleVT().simpleVT == MVT.i32)
                ff = new APInt(32, f32TwoE32);
            else if (srcVT.getSimpleVT().simpleVT == MVT.i64)
                ff = new APInt(32, f32TwoE64);
            else if (srcVT.getSimpleVT().simpleVT == MVT.i128)
            ff = new APInt(32, f32TwoE128);
            else
                assert false:"Unsupported UINT_TO_FP!";

            SDValue lo, hi;
            SDValue[] t = getExpandedInteger(op);
            lo = t[0];
            hi = t[1];
            SDValue signSet = dag.getSetCC(new EVT(tli.getSetCCResultType(hi.getValueType())),
                    hi, dag.getConstant(0, hi.getValueType(), false),
                    CondCode.SETLT);
            SDValue fudgePtr = dag.getConstantPool(ConstantInt.get(ff.zext(64)),
                    new EVT(tli.getPointerTy()), 0, 0, false, 0);

            SDValue zero = dag.getIntPtrConstant(0);
            SDValue four = dag.getIntPtrConstant(4);
            if (tli.isBigEndian())
            {
                SDValue tt = zero;
                zero = four;
                four = tt;
            }
            SDValue offset = dag.getNode(ISD.SELECT, zero.getValueType(),signSet,
                    zero, four);
            int alignment = ((ConstantPoolSDNode)fudgePtr.getNode()).getAlign();
            fudgePtr = dag.getNode(ISD.ADD, new EVT(tli.getPointerTy()), fudgePtr, offset);
            alignment = Math.min(alignment, 4);

            SDValue fudge = dag.getExtLoad(LoadExtType.EXTLOAD, destVT,
                    dag.getEntryNode(), fudgePtr, null,0, new EVT(MVT.f32),
                    false, alignment);
            return dag.getNode(ISD.FADD, destVT, signedConv, fudge);
        }
        RTLIB lc = tli.getUINTTOFP(srcVT, destVT);
        assert lc != RTLIB.UNKNOWN_LIBCALL:"Don't know how to expand this UINT_TO_FP!";
        return makeLibCall(lc, destVT, new SDValue[]{op}, true);
    }

    private SDValue[] integerExpandSetCCOperands(SDValue newLHS, SDValue newRHS, CondCode cc)
    {
        SDValue[] lhsT, rhsT;
        lhsT = getExpandedInteger(newLHS);
        rhsT = getExpandedInteger(newRHS);

        SDValue lhslo = lhsT[0], lhshi = lhsT[1], rhslo = rhsT[0], rhshi = rhsT[1];
        EVT vt = newLHS.getValueType();
        if (cc == CondCode.SETEQ || cc == CondCode.SETNE)
        {
            if (rhslo.equals(rhshi))
            {
                if (rhslo.getNode() instanceof ConstantSDNode)
                {
                    ConstantSDNode csd = (ConstantSDNode)rhslo.getNode();
                    if (csd.isAllOnesValue())
                    {
                        newLHS = dag.getNode(ISD.AND, lhslo.getValueType(), lhslo, lhshi);
                        newRHS = rhslo;
                        return new SDValue[]{newLHS, newRHS};
                    }
                }
            }

            newLHS = dag.getNode(ISD.XOR, lhslo.getValueType(), lhslo, rhslo);
            newRHS = dag.getNode(ISD.XOR, lhslo.getValueType(), lhshi, rhshi);
            newLHS = dag.getNode(ISD.OR, newLHS.getValueType(), newLHS, newRHS);
            newRHS = dag.getConstant(0, newLHS.getValueType(), false);
            return new SDValue[]{newLHS, newRHS};
        }

        if (newRHS.getNode() instanceof ConstantSDNode)
        {
            ConstantSDNode csd = (ConstantSDNode)newRHS.getNode();
            if ((cc == CondCode.SETLT && csd.isNullValue()) ||
                    cc == CondCode.SETGT && csd.isAllOnesValue())
            {
                newLHS = lhshi;
                newRHS = rhshi;
                return new SDValue[]{newLHS, newRHS};
            }
        }

        CondCode lowCC;
        switch (cc)
        {
            default:
                Util.shouldNotReachHere();
            case SETLT:
            case SETULT:
                lowCC = CondCode.SETULT; break;
            case SETGT:
            case SETUGT:
                lowCC = CondCode.SETUGT; break;
            case SETLE:
            case SETULE:
                lowCC = SETULE; break;
            case SETGE:
            case SETUGE:
                lowCC = SETUGE; break;
        }
        DAGCombinerInfo  dagCombinerInfo = new DAGCombinerInfo(dag, false, true,true, null);
        SDValue temp1, temp2;
        temp1 = tli.simplifySetCC(new EVT(tli.getSetCCResultType(lhslo.getValueType())),
                lhslo, rhslo, lowCC, false, dagCombinerInfo);
        if (temp1.getNode() == null)
            temp1 = dag.getSetCC(new EVT(tli.getSetCCResultType(lhslo.getValueType())),
                    lhslo, rhslo, lowCC);
        temp2 = tli.simplifySetCC(new EVT(tli.getSetCCResultType(lhshi.getValueType())),
                lhshi, rhshi, lowCC, false, dagCombinerInfo);
        if (temp2.getNode() == null)
            temp2 = dag.getNode(ISD.SETCC, new EVT(tli.getSetCCResultType(lhshi.getValueType())),
                    lhshi, rhshi, dag.getCondCode(cc));

        ConstantSDNode temp1C = temp1.getNode() instanceof ConstantSDNode? (ConstantSDNode)temp1.getNode():null;
        ConstantSDNode temp2C = temp2.getNode() instanceof ConstantSDNode? (ConstantSDNode)temp2.getNode():null;
        if ((temp1C != null && temp1C.isNullValue()) ||
                (temp2C != null && temp2C.isNullValue() &&
                        (cc == SETLE || cc == SETGE|| cc == SETUGE || cc == SETULE)) ||
                (temp2C != null && temp2C.getAPIntValue().eq(1) &&
                        (cc == SETLT || cc == SETGT || cc == SETUGT || cc== SETULT)))
        {
            newLHS =temp2;
            newRHS = new SDValue();
            return new SDValue[]{newLHS, newRHS};
        }

        newLHS = tli.simplifySetCC(new EVT(tli.getSetCCResultType(lhshi.getValueType())),
                lhshi, rhshi, SETEQ, false, dagCombinerInfo);

        if (newLHS.getNode() == null)
            newLHS = dag.getSetCC(new EVT(tli.getSetCCResultType(lhshi.getValueType())),
                    lhshi, rhshi, SETEQ);
        newLHS = dag.getNode(ISD.SELECT, temp1.getValueType(), newLHS, temp1, temp2);
        newRHS = new SDValue();
        return new SDValue[]{newLHS, newRHS};
    }

    private SDValue getSoftenedFloat(SDValue op)
    {
        if (!softenedFloats.containsKey(op))
            softenedFloats.put(op, new SDValue());
        SDValue softenedOp = softenedFloats.get(op);
        softenedOp = remapValue(softenedOp);
        softenedFloats.put(op, softenedOp);
        assert softenedOp.getNode() != null:"Operand wasn't converted to integer?";
        return softenedOp;
    }

    private void setSoftenedFloat(SDValue op, SDValue result)
    {
        assert result.getValueType().equals(tli.getTypeToTransformTo(op.getValueType()));
        result = analyzeNewValue(result);

        assert !softenedFloats.containsKey(op):"Node already converted to integer!";
        softenedFloats.put(op, result);
    }

    private void softenFloatResult(SDNode n, int resNo)
    {
        if (Util.DEBUG)
        {
            System.err.printf("Soften float result: %d: ", resNo);
            n.dump(dag);
            System.err.println();
        }
        SDValue res = new SDValue();
        switch (n.getOpcode())
        {
            default:
                if (Util.DEBUG)
                {
                    System.err.printf("softenFloatResult #%d: ", resNo);
                    n.dump(dag);
                    System.err.println();
                }
                Util.shouldNotReachHere("Don't know how to soften the result of this oeprator!");
                break;
            case ISD.BIT_CONVERT:
                res = softenFloatRes_BIT_CONVERT(n); break;
            case ISD.BUILD_PAIR:
                res = softenFloatRes_BUILD_PAIR(n); break;
            case ISD.ConstantFP:
                res = softenFloatRes_ConstantFP((ConstantFPSDNode)n); break;
            case ISD.EXTRACT_VECTOR_ELT:
                res = softenFloatRes_EXTRACT_VECTOR_ELT(n); break;
            case ISD.FABS:        res = softenFloatRes_FABS(n); break;
            case ISD.FADD:        res = softenFloatRes_FADD(n); break;
            case ISD.FCEIL:       res = softenFloatRes_FCEIL(n); break;
            case ISD.FCOPYSIGN:   res = softenFloatRes_FCOPYSIGN(n); break;
            case ISD.FCOS:        res = softenFloatRes_FCOS(n); break;
            case ISD.FDIV:        res = softenFloatRes_FDIV(n); break;
            case ISD.FEXP:        res = softenFloatRes_FEXP(n); break;
            case ISD.FEXP2:       res = softenFloatRes_FEXP2(n); break;
            case ISD.FFLOOR:      res = softenFloatRes_FFLOOR(n); break;
            case ISD.FLOG:        res = softenFloatRes_FLOG(n); break;
            case ISD.FLOG2:       res = softenFloatRes_FLOG2(n); break;
            case ISD.FLOG10:      res = softenFloatRes_FLOG10(n); break;
            case ISD.FMUL:        res = softenFloatRes_FMUL(n); break;
            case ISD.FNEARBYINT:  res = softenFloatRes_FNEARBYINT(n); break;
            case ISD.FNEG:        res = softenFloatRes_FNEG(n); break;
            case ISD.FP_EXTEND:   res = softenFloatRes_FP_EXTEND(n); break;
            case ISD.FP_ROUND:    res = softenFloatRes_FP_ROUND(n); break;
            case ISD.FPOW:        res = softenFloatRes_FPOW(n); break;
            case ISD.FPOWI:       res = softenFloatRes_FPOWI(n); break;
            case ISD.FREM:        res = softenFloatRes_FREM(n); break;
            case ISD.FRINT:       res = softenFloatRes_FRINT(n); break;
            case ISD.FSIN:        res = softenFloatRes_FSIN(n); break;
            case ISD.FSQRT:       res = softenFloatRes_FSQRT(n); break;
            case ISD.FSUB:        res = softenFloatRes_FSUB(n); break;
            case ISD.FTRUNC:      res = softenFloatRes_FTRUNC(n); break;
            case ISD.LOAD:        res = softenFloatRes_LOAD(n); break;
            case ISD.SELECT:      res = softenFloatRes_SELECT(n); break;
            case ISD.SELECT_CC:   res = softenFloatRes_SELECT_CC(n); break;
            case ISD.SINT_TO_FP:
            case ISD.UINT_TO_FP:  res = softenFloatRes_XINT_TO_FP(n); break;
            case ISD.UNDEF:       res = softenFloatRes_UNDEF(n); break;
            case ISD.VAARG:       res = softenFloatRes_VAARG(n); break;
        }
        if (res.getNode() != null)
            setSoftenedFloat(new SDValue(n, resNo), res);
    }
    private SDValue softenFloatRes_BIT_CONVERT(SDNode n)
    {
        return bitConvertToInteger(n.getOperand(0));
    }
    private SDValue softenFloatRes_BUILD_PAIR(SDNode n)
    {
        return dag.getNode(ISD.BUILD_PAIR,
                tli.getTypeToTransformTo(n.getValueType(0)),
                bitConvertToInteger(n.getOperand(0)),
                bitConvertToInteger(n.getOperand(1)));
    }
    private SDValue softenFloatRes_ConstantFP(ConstantFPSDNode n)
    {
        return dag.getConstant(n.getValueAPF().bitcastToAPInt(),
                tli.getTypeToTransformTo(n.getValueType(0)), false);
    }
    private SDValue softenFloatRes_EXTRACT_VECTOR_ELT(SDNode n)
    {
        SDValue newOP = bitConvertVectorToIntegerVector(n.getOperand(0));
        return dag.getNode(ISD.EXTRACT_VECTOR_ELT,
                newOP.getValueType().getVectorElementType(),
                newOP, n.getOperand(1));
    }
    private SDValue softenFloatRes_FABS(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        int size = nvt.getSizeInBits();

        SDValue mask = dag.getConstant(APInt.getAllOnesValue(size).clear(size-1),
                nvt, false);
        SDValue op = getSoftenedFloat(n.getOperand(0));
        return dag.getNode(ISD.AND, nvt, op, mask);
    }

    public static RTLIB getFPLibCall(EVT vt, RTLIB callF32,
            RTLIB callF64, RTLIB callF80,
            RTLIB callPPCF128)
    {
        int simpleVT = vt.getSimpleVT().simpleVT;
        RTLIB[] libs = {callF32, callF64, callF80, callPPCF128};
        if (simpleVT >= MVT.f32 && simpleVT <= MVT.ppcf128)
            return libs[simpleVT- MVT.f32];
        return RTLIB.UNKNOWN_LIBCALL;
    }

    private SDValue softenFloatRes_FADD(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue[] ops = {getSoftenedFloat(n.getOperand(0)),
                getSoftenedFloat(n.getOperand(1))};
        return makeLibCall(getFPLibCall(n.getValueType(0),
                RTLIB.ADD_F32,
                RTLIB.ADD_F64,
                RTLIB.ADD_F80,
                RTLIB.ADD_PPCF128),
                nvt, ops, false);
    }
    private SDValue softenFloatRes_FCEIL(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue op = getSoftenedFloat(n.getOperand(0));

        return makeLibCall(getFPLibCall(n.getValueType(0),
                RTLIB.CEIL_F32,
                RTLIB.CEIL_F64,
                RTLIB.CEIL_F80,
                RTLIB.CEIL_PPCF128),
                nvt, new SDValue[] { op }, false);
    }
    private SDValue softenFloatRes_FCOPYSIGN(SDNode n)
    {
        SDValue lhs = getSoftenedFloat(n.getOperand(0));
        SDValue rhs = getSoftenedFloat(n.getOperand(0));

        EVT lvt = lhs.getValueType();
        EVT rvt = rhs.getValueType();

        int lsize = lvt.getSizeInBits();
        int rsize = rvt.getSizeInBits();
        SDValue signedBit = dag.getNode(ISD.SHL, rvt, dag.getConstant(1, rvt, false),
                dag.getConstant(rsize-1, new EVT(tli.getShiftAmountTy()), false));
        signedBit = dag.getNode(ISD.AND, rvt, rhs, signedBit);

        int sizeDiff = rvt.getSizeInBits() - lvt.getSizeInBits();
        if (sizeDiff > 0)
        {
            signedBit = dag.getNode(ISD.SRL, rvt, signedBit,
                    dag.getConstant(sizeDiff, new EVT(tli.getShiftAmountTy()), false));
            signedBit = dag.getNode(ISD.TRUNCATE, lvt, signedBit);
        }
        else if (sizeDiff < 0)
        {
            signedBit = dag.getNode(ISD.ANY_EXTEND, lvt, signedBit);
            signedBit = dag.getNode(ISD.SHL, lvt, signedBit, dag.getNode(-sizeDiff,
                    new EVT(tli.getShiftAmountTy())));
        }

        SDValue mask = dag.getNode(ISD.SHL, lvt, dag.getConstant(1, lvt, false),
                dag.getConstant(lsize-1, new EVT(tli.getShiftAmountTy()), false));
        mask = dag.getNode(ISD.SUB, lvt, mask, dag.getConstant(1, lvt, false));
        lhs = dag.getNode(ISD.AND, lvt, lhs, mask);
        return dag.getNode(ISD.OR, lvt, lhs, signedBit);
    }
    private SDValue softenFloatRes_FCOS(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue op = getSoftenedFloat(n.getOperand(0));
        return makeLibCall(getFPLibCall(n.getValueType(0),
                RTLIB.COS_F32,
                RTLIB.COS_F64,
                RTLIB.COS_F80,
                RTLIB.COS_PPCF128),
                nvt, new SDValue[] { op }, false);
    }
    private SDValue softenFloatRes_FDIV(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue[] ops = {getSoftenedFloat(n.getOperand(0)),
                getSoftenedFloat(n.getOperand(1))};
        return makeLibCall(getFPLibCall(n.getValueType(0),
                RTLIB.DIV_F32,
                RTLIB.DIV_F64,
                RTLIB.DIV_F80,
                RTLIB.DIV_PPCF128),
                nvt, ops, false);
    }
    private SDValue softenFloatRes_FEXP(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue op = getSoftenedFloat(n.getOperand(0));

        return makeLibCall(getFPLibCall(n.getValueType(0),
                RTLIB.EXP_F32,
                RTLIB.EXP_F64,
                RTLIB.EXP_F80,
                RTLIB.EXP_PPCF128),
                nvt, new SDValue[] { op }, false);
    }
    private SDValue softenFloatRes_FEXP2(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue op = getSoftenedFloat(n.getOperand(0));

        return makeLibCall(getFPLibCall(n.getValueType(0),
                RTLIB.EXP2_F32,
                RTLIB.EXP2_F64,
                RTLIB.EXP2_F80,
                RTLIB.EXP2_PPCF128),
                nvt, new SDValue[] { op }, false);
    }
    private SDValue softenFloatRes_FFLOOR(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue op = getSoftenedFloat(n.getOperand(0));

        return makeLibCall(getFPLibCall(n.getValueType(0),
                RTLIB.FLOOR_F32,
                RTLIB.FLOOR_F64,
                RTLIB.FLOOR_F80,
                RTLIB.FLOOR_PPCF128),
                nvt, new SDValue[] { op }, false);
    }
    private SDValue softenFloatRes_FLOG(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue op = getSoftenedFloat(n.getOperand(0));

        return makeLibCall(getFPLibCall(n.getValueType(0),
                RTLIB.LOG_F32,
                RTLIB.LOG_F64,
                RTLIB.LOG_F80,
                RTLIB.LOG_PPCF128),
                nvt, new SDValue[] { op }, false);
    }
    private SDValue softenFloatRes_FLOG2(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue op = getSoftenedFloat(n.getOperand(0));

        return makeLibCall(getFPLibCall(n.getValueType(0),
                RTLIB.LOG2_F32,
                RTLIB.LOG2_F64,
                RTLIB.LOG2_F80,
                RTLIB.LOG2_PPCF128),
                nvt, new SDValue[] { op }, false);
    }
    private SDValue softenFloatRes_FLOG10(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue op = getSoftenedFloat(n.getOperand(0));

        return makeLibCall(getFPLibCall(n.getValueType(0),
                RTLIB.LOG10_F32,
                RTLIB.LOG10_F64,
                RTLIB.LOG10_F80,
                RTLIB.LOG10_PPCF128),
                nvt, new SDValue[] { op }, false);
    }
    private SDValue softenFloatRes_FMUL(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue[] ops = {getSoftenedFloat(n.getOperand(0)),
                getSoftenedFloat(n.getOperand(1))};
        return makeLibCall(getFPLibCall(n.getValueType(0),
                RTLIB.MUL_F32,
                RTLIB.MUL_F64,
                RTLIB.MUL_F80,
                RTLIB.MUL_PPCF128),
                nvt, ops, false);
    }
    private SDValue softenFloatRes_FNEARBYINT(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue op = getSoftenedFloat(n.getOperand(0));

        return makeLibCall(getFPLibCall(n.getValueType(0),
                RTLIB.NEARBYINT_F32,
                RTLIB.NEARBYINT_F64,
                RTLIB.NEARBYINT_F80,
                RTLIB.NEARBYINT_PPCF128),
                nvt, new SDValue[] { op }, false);
    }
    private SDValue softenFloatRes_FNEG(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue[] ops = {dag.getConstantFP(-0.0, n.getValueType(0), false),
                getSoftenedFloat(n.getOperand(0))};

        return makeLibCall(getFPLibCall(n.getValueType(0),
                RTLIB.SUB_F32,
                RTLIB.SUB_F64,
                RTLIB.SUB_F80,
                RTLIB.SUB_PPCF128),
                nvt,
                ops, false);
    }
    private SDValue softenFloatRes_FP_EXTEND(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue op = n.getOperand(0);
        RTLIB lc = tli.getFPEXT(op.getValueType(), n.getValueType(0));
        assert lc != RTLIB.UNKNOWN_LIBCALL;
        return makeLibCall(lc, nvt, new SDValue[] { op }, false);
    }
    private SDValue softenFloatRes_FP_ROUND(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue op = n.getOperand(0);
        RTLIB lc = tli.getFPROUND(op.getValueType(), n.getValueType(0));
        assert lc != RTLIB.UNKNOWN_LIBCALL;
        return makeLibCall(lc, nvt, new SDValue[] { op }, false);
    }
    private SDValue softenFloatRes_FPOW(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue[] ops = {getSoftenedFloat(n.getOperand(1)),
                getSoftenedFloat(n.getOperand(0))};

        return makeLibCall(getFPLibCall(n.getValueType(0),
                RTLIB.POW_F32,
                RTLIB.POW_F64,
                RTLIB.POW_F80,
                RTLIB.POW_PPCF128),
                nvt,
                ops, false);
    }
    private SDValue softenFloatRes_FPOWI(SDNode n)
    {
        assert n.getOperand(1).getValueType().getSimpleVT().simpleVT == MVT.i32;
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue[] ops = {getSoftenedFloat(n.getOperand(1)),
                getSoftenedFloat(n.getOperand(0))};

        return makeLibCall(getFPLibCall(n.getValueType(0),
                RTLIB.POWI_F32,
                RTLIB.POWI_F64,
                RTLIB.POWI_F80,
                RTLIB.POWI_PPCF128),
                nvt,
                ops, false);
    }
    private SDValue softenFloatRes_FREM(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue[] ops = {getSoftenedFloat(n.getOperand(1)),
                getSoftenedFloat(n.getOperand(0))};

        return makeLibCall(getFPLibCall(n.getValueType(0),
                RTLIB.REM_F32,
                RTLIB.REM_F64,
                RTLIB.REM_F80,
                RTLIB.REM_PPCF128),
                nvt,
                ops, false);
    }
    private SDValue softenFloatRes_FRINT(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue[] ops = {getSoftenedFloat(n.getOperand(1))};

        return makeLibCall(getFPLibCall(n.getValueType(0),
                RTLIB.RINT_F32,
                RTLIB.RINT_F64,
                RTLIB.RINT_F80,
                RTLIB.RINT_PPCF128),
                nvt,
                ops, false);
    }
    private SDValue softenFloatRes_FSIN(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue[] ops = {getSoftenedFloat(n.getOperand(1))};

        return makeLibCall(getFPLibCall(n.getValueType(0),
                RTLIB.SIN_F32,
                RTLIB.SIN_F64,
                RTLIB.SIN_F80,
                RTLIB.SIN_PPCF128),
                nvt,
                ops, false);
    }
    private SDValue softenFloatRes_FSQRT(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue[] ops = {getSoftenedFloat(n.getOperand(1))};

        return makeLibCall(getFPLibCall(n.getValueType(0),
                RTLIB.SQRT_F32,
                RTLIB.SQRT_F64,
                RTLIB.SQRT_F80,
                RTLIB.SQRT_PPCF128),
                nvt,
                ops, false);
    }
    private SDValue softenFloatRes_FSUB(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue[] ops = {getSoftenedFloat(n.getOperand(1)),
                getSoftenedFloat(n.getOperand(0))};

        return makeLibCall(getFPLibCall(n.getValueType(0),
                RTLIB.SUB_F32,
                RTLIB.SUB_F64,
                RTLIB.SUB_F80,
                RTLIB.SUB_PPCF128),
                nvt,
                ops, false);
    }
    private SDValue softenFloatRes_FTRUNC(SDNode n)
    {
        EVT nvt = tli.getTypeToTransformTo(n.getValueType(0));
        SDValue[] ops = {getSoftenedFloat(n.getOperand(1))};

        return makeLibCall(getFPLibCall(n.getValueType(0),
                RTLIB.TRUNC_F32,
                RTLIB.TRUNC_F64,
                RTLIB.TRUNC_F80,
                RTLIB.TRUNC_PPCF128),
                nvt,
                ops, false);
    }
    private SDValue softenFloatRes_LOAD(SDNode n)
    {

    }
    private SDValue softenFloatRes_SELECT(SDNode n)
    {
        SDValue lhs = getSoftenedFloat(n.getOperand(1));
        SDValue rhs = getSoftenedFloat(n.getOperand(2));
        return dag.getNode(ISD.SELECT, lhs.getValueType(), n.getOperand(0), lhs, rhs);
    }
    private SDValue softenFloatRes_SELECT_CC(SDNode n)
    {
        SDValue lhs = getSoftenedFloat(n.getOperand(2));
        SDValue rhs = getSoftenedFloat(n.getOperand(3));
        return dag.getNode(ISD.SELECT_CC, lhs.getValueType(),
                n.getOperand(0), n.getOperand(1), lhs, rhs,
                n.getOperand(4));
    }
    private SDValue softenFloatRes_UNDEF(SDNode n)
    {
        return dag.getUNDEF(tli.getTypeToTransformTo(n.getValueType(0)));
    }
    private SDValue softenFloatRes_VAARG(SDNode n)
    {
        SDValue chain = n.getOperand(0);
        SDValue ptr = n.getOperand(1);
        EVT vt = n.getValueType(0);
        EVT nvt = tli.getTypeToTransformTo(vt);
        SDValue newVAARG = dag.getVAArg(nvt, chain, ptr, n.getOperand(2));

        replaceValueWith(new SDValue(n, 1), newVAARG.getValue(1));
        return newVAARG;
    }
    private SDValue softenFloatRes_XINT_TO_FP(SDNode n)
    {
        boolean signed = n.getOpcode() == ISD.SINT_TO_FP;
        EVT svt = n.getOperand(0).getValueType();
        EVT rvt = n.getValueType(0);
        EVT nvt = new EVT();

        RTLIB lib = RTLIB.UNKNOWN_LIBCALL;
        for (int t = MVT.FIRST_INTEGER_VALUETYPE;
                 t <= MVT.LAST_INTEGER_VALUETYPE && lib == RTLIB.UNKNOWN_LIBCALL;
                 ++t)
        {
            nvt = new EVT(t);
            if (nvt.bitsGE(svt))
                lib = signed ? tli.getSINTTOFP(nvt, rvt) : tli.getUINTTOFP(nvt, rvt);
        }
        assert lib != RTLIB.UNKNOWN_LIBCALL;
        SDValue op = dag.getNode(signed ? ISD.SIGN_EXTEND : ISD.ZERO_EXTEND,
                nvt, n.getOperand(0));
        return makeLibCall(lib, tli.getTypeToTransformTo(rvt),
                new SDValue[] { op }, false);
    }

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
    private SDValue[] splitRes_MERGE_VALUES(SDNode n) {}
    private SDValue[] splitRes_SELECT      (SDNode n) {}
    private SDValue[] splitRes_SELECT_CC   (SDNode n) {}
    private SDValue[] splitRes_UNDEF       (SDNode n) {}

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
    private SDValue[] expandRes_BIT_CONVERT       (SDNode n) {}
    private SDValue[] expandRes_BUILD_PAIR        (SDNode n) {}
    private SDValue[] expandRes_EXTRACT_ELEMENT   (SDNode n) {}
    private SDValue[] expandRes_EXTRACT_VECTOR_ELT(SDNode n) {}
    private SDValue[] expandRes_NormalLoad        (SDNode n) {}
    private SDValue[] expandRes_VAARG             (SDNode n) {}

    // Generic Operand Expansion.
    private SDValue expandOp_BIT_CONVERT      (SDNode n) {}
    private SDValue expandOp_BUILD_VECTOR     (SDNode n) {}
    private SDValue expandOp_EXTRACT_ELEMENT  (SDNode n) {}
    private SDValue expandOp_INSERT_VECTOR_ELT(SDNode n) {}
    private SDValue expandOp_SCALAR_TO_VECTOR (SDNode n) {}
    private SDValue expandOp_NormalStore      (SDNode n, int opNo)
    {}
}
