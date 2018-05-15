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
import backend.codegen.ValueTypeAction;
import backend.codegen.dagisel.SDNode.ConstantFPSDNode;
import backend.codegen.dagisel.SDNode.LoadSDNode;
import backend.codegen.dagisel.SDNode.ShuffleVectorSDNode;
import backend.codegen.dagisel.SDNode.StoreSDNode;
import backend.codegen.fastISel.ISD;
import backend.target.TargetLowering;
import backend.value.Value;
import tools.Pair;

import java.util.ArrayList;
import java.util.HashMap;

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

    private ArrayList<SDNode> worklist;

    public DAGTypeLegalizer(SelectionDAG dag)
    {
        tli = dag.getTargetLoweringInfo();
        this.dag = dag;
        valueTypeActions = tli.getValueTypeActions();
        assert MVT.LAST_INTEGER_VALUETYPE <= MVT.MAX_ALLOWED_VALUETYPE:
                "Too many value types for ValueTypeActions to hold!";
        promotedIntegers = new HashMap<>();
        expandedIntegers = new HashMap<>();
        expandedFloats = new HashMap<>();
        softenedFloats = new HashMap<>();
        scalarizedVectors = new HashMap<>();
        splitVectors = new HashMap<>();
        widenedVectors = new HashMap<>();
        replacedValues = new HashMap<>();
        worklist = new ArrayList<>();
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
        // TODO: 18-5-15
    }

    public void nodeDeletion(SDNode oldOne, SDNode newOne)
    {
        expungeNode(oldOne);
        expungeNode(newOne);
        for (int i = 0, e = oldOne.getNumValues(); i < e; i++)
            replacedValues.put(new SDValue(oldOne, i), new SDValue(newOne, i));
    }

    private SDNode analyzeNewNode(SDNode n)
    {}

    private void expungeNode(SDNode n)
    {}

    private void analyzeNewValue(SDValue val)
    {}

    private void performExpensiveChecks()
    {}

    private void remapValue(SDValue val)
    {}


    private SDValue bitConvertToInteger(SDValue op)
    {}

    private SDValue bitConvertVectorToIntegerVector(SDValue op)
    {}

    private SDValue createStackStoreLoad(SDValue op, EVT destVT)
    {}

    private boolean customLowerNode(SDNode n, EVT vt, boolean legalizeResult)
    {}

    private SDValue getVectorElementPointer(SDValue vecPtr, EVT eltVT, SDValue index)
    {}

    private SDValue joinIntegers(SDValue lo, SDValue hi)
    {}

    private SDValue promoteTargetBoolean(SDValue boolVal, EVT vt)
    {}

    private void replaceValueWith(SDValue from, SDValue to)
    {}

    private void replaceValueWithHelper(SDValue from, SDValue to)
    {}

    private SDValue[] splitInteger(SDValue op)
    {}

    private SDValue[] splitInteger(SDValue op, EVT loVT, EVT hiVT)
    {}

    private SDValue getPromotedInteger(SDValue op)
    {}

    private void setPromotedIntegers(SDValue op, SDValue result)
    {}

    private SDValue sextPromotedInteger(SDValue op)
    {}

    private SDValue zextPromotedInteger(SDValue op)
    {}

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
    void SplitVectorResult(SDNode n, int opNo) {}
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
    private boolean WidenVectorOperand(SDNode n, int ResNo){}
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
