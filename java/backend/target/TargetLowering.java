package backend.target;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import backend.codegen.*;
import backend.codegen.dagisel.*;
import backend.codegen.fastISel.ISD;
import backend.support.CallingConv;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.Function;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.APInt;
import tools.OutParamWrapper;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;

import static backend.codegen.dagisel.FunctionLoweringInfo.computeValueVTs;
import static backend.codegen.dagisel.MemIndexedMode.LAST_INDEXED_MODE;
import static backend.codegen.dagisel.RegsForValue.getCopyFromParts;
import static backend.codegen.dagisel.RegsForValue.getCopyToParts;
import static backend.target.TargetLowering.LegalizeAction.*;
import static backend.target.TargetOptions.EnablePerformTailCallOpt;

/**
 * This class defines information used to lower LLVM code to
 * legal SelectionDAG operators that the target instruction selector can accept
 * natively.
 *
 * This class also defines callbacks that targets must implement to lower
 * target-specific constructs to SelectionDAG operators.
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class TargetLowering
{
    /**
     * This enum indicates whether operations are valid for a
     * target, and if not, what action should be used to make them valid.
     */
    public enum LegalizeAction
    {
        /**
         * The target natively supports this operation.
         */
        Legal,

        /**
         * This operation should be executed in a larger type.
         */
        Promote,

        /**
         * Try to expand this to other ops, otherwise use a libcall.
         */
        Expand,

        /**
         * Use the LowerOperation hook to implement custom lowering.
         */
        Custom
    }

    /**
     * How the target represents true/false values.
     */
    public enum BooleanContent
    {
        UndefinedBooleanContent,    // Only bit 0 counts, the rest can hold garbage.
        ZeroOrOneBooleanContent,        // All bits zero except for bit 0.
        ZeroOrNegativeOneBooleanContent // All bits equal to bit 0.
    }

    /**
     * This indicates the default register class to use for
     * each ValueType the target supports natively.
     */
    protected TargetRegisterClass[] registerClassForVT;
    protected EVT[] transformToType;
    protected int[] numRegistersForVT;
    protected EVT[] registerTypeForVT;

    protected ValueTypeAction valueTypeAction;

    protected TargetMachine tm;
    protected TargetData td;

    private ArrayList<Pair<EVT, TargetRegisterClass>> availableRegClasses;
    private boolean isLittleEndian;
    private MVT pointerTy;
    private MVT shiftAmountTy;
    private BooleanContent booleanContents;

    private long[][] opActions = new long[MVT.LAST_VALUETYPE/8*4][ISD.BUILTIN_OP_END];
    private long[] loadExtActions = new long[LoadExtType.LAST_LOADEXT_TYPE.ordinal()];
    private long[] truncStoreActions = new long[MVT.LAST_VALUETYPE];
    private long[][][] indexedModeActions = new long[MVT.LAST_VALUETYPE][2][LAST_INDEXED_MODE.ordinal()];
    private long[] convertActions = new long[MVT.LAST_VALUETYPE];
    private long[] condCodeActions = new long[CondCode.SETCC_INVALID.ordinal()];
    private byte[] targetDAGCombineArray = new byte[(ISD.BUILTIN_OP_END+7)/8];
    private TObjectIntHashMap<Pair<Integer,Integer>> promoteType = new TObjectIntHashMap<>();

    public TargetLowering(TargetMachine tm)
    {
        this.tm = tm;
        td = tm.getTargetData();
        valueTypeAction = new ValueTypeAction();
        pointerTy = getValueType(td.getIntPtrType()).getSimpleVT();
        availableRegClasses = new ArrayList<>();
        registerClassForVT = new TargetRegisterClass[MVT.LAST_VALUETYPE];
        transformToType = new EVT[MVT.LAST_VALUETYPE];
        numRegistersForVT = new int[MVT.LAST_VALUETYPE];
        registerTypeForVT = new EVT[MVT.LAST_VALUETYPE];
        booleanContents = BooleanContent.UndefinedBooleanContent;
    }

    public ValueTypeAction getValueTypeActions()
    {
        return valueTypeAction;
    }

    public BooleanContent getBooleanContents()
    {
        return booleanContents;
    }

    public TargetData getTargetData()
    {
        return td;
    }

    public TargetMachine getTargetMachine()
    {
        return tm;
    }

    public boolean isBigEndian()
    {
        return !isLittleEndian;
    }

    public boolean isLittleEndian()
    {
        return isLittleEndian;
    }

    /**
     * Create a detail info about machine function.
     *
     * @param mf
     * @return
     */
    public abstract MachineFunctionInfo createMachineFunctionInfo(
            MachineFunction mf);

    public EVT getValueType(Type type)
    {
        return getValueType(type, false);
    }

    /**
     * eturn the EVT corresponding to this LLVM type.
     * This is fixed by the LLVM operations except for the pointer size.  If
     * AllowUnknown is true, this will return MVT::Other for types with no EVT
     * counterpart (e.g. structs), otherwise it will assert.
     *
     * @param type
     * @param allowUnknown
     * @return
     */
    public EVT getValueType(Type type, boolean allowUnknown)
    {
        EVT vt = EVT.getEVT(type, allowUnknown);
        return vt.equals(new EVT(new MVT(MVT.iPTR))) ? new EVT(pointerTy) : vt;
    }

    /*
    public abstract FastISel createFastISel(MachineFunction mf,
            MachineModuleInfo mmi,
            TObjectIntHashMap vm,
            HashMap<BasicBlock, MachineBasicBlock> bm,
            TObjectIntHashMap<Instruction.AllocaInst> am);
    */

    /**
     * Return true if the target has native support for the
     * specified value type.  This means that it has a register that directly
     * holds it without promotions or expansions.
     *
     * @param vt
     * @return
     */
    public boolean isTypeLegal(EVT vt)
    {
        assert !vt.isSimple() || vt.getSimpleVT().simpleVT < registerClassForVT.length;
        return vt.isSimple() && registerClassForVT[vt.getSimpleVT().simpleVT] != null;
    }

    public EVT getTypeToTransformTo(EVT vt)
    {
        if (vt.isSimple())
        {
            assert vt.getSimpleVT().simpleVT < transformToType.length;
            EVT nvt = transformToType[vt.getSimpleVT().simpleVT];
            assert getTypeAction(nvt)
                    != Promote : "Promote may not follow expand or promote";
            return nvt;
        }

        if (vt.isVector())
        {
            EVT nvt = vt.getPow2VectorType();
            if (nvt.equals(vt))
            {
                // Vector length is a power of 2, split to half the size.
                int numElts = vt.getVectorNumElements();
                EVT eltVT = vt.getVectorElementType();
                return numElts == 1 ? eltVT : EVT.getVectorVT(eltVT, numElts / 2);
            }

            // Promote to a power of two size, avoiding multi-step promotions.
            return getTypeAction(nvt) == Promote ?
                    getTypeToTransformTo(nvt) :
                    nvt;
        }
        else if (vt.isInteger())
        {
            EVT nvt = vt.getRoundIntegerType();
            if (nvt.equals(vt))
            {
                // Size is a power of two, expand to half the size.
                return EVT.getIntegerVT(vt.getSizeInBits() / 2);
            }
            else
            {
                // Promote to a power of two size, avoiding multi-step promotion.
                return getTypeAction(nvt) == Promote ?
                        getTypeToTransformTo(nvt) :
                        nvt;
            }
        }
        assert false : "Unsupported extended type!";
        return new EVT(new MVT(MVT.Other));
    }

    /**
     * Return how we should legalize values of this type, either
     * it is already legal (return 'Legal') or we need to promote it to a larger
     * × type (return 'Promote'), or we need to expand it into multiple registers
     * × of smaller integer type (return 'Expand').  'Custom' is not an option.
     *
     * @param nvt
     * @return
     */
    private LegalizeAction getTypeAction(EVT nvt)
    {
        return valueTypeAction.getTypeAction(nvt);
    }

    public TargetRegisterClass getRegClassFor(EVT vt)
    {
        assert vt.isSimple() : "getRegClassFor called on illegal type!";
        TargetRegisterClass rc = registerClassForVT[vt.getSimpleVT().simpleVT];
        assert rc != null : "This value type is not natively supported!";
        return rc;
    }

    public MVT getPointerTy()
    {
        return pointerTy;
    }

    /**
     * Add the specified register class as an available
     * /// regclass for the specified value type.  This indicates the selector can
     * /// handle values of that class natively.
     *
     * @param vt
     */
    public void addRegisterClass(MVT vt, TargetRegisterClass regClass)
    {
        assert vt.simpleVT < registerClassForVT.length;
        availableRegClasses.add(Pair.get(new EVT(vt), regClass));
        registerClassForVT[vt.simpleVT] = regClass;
    }

    public int getVectorTypeBreakdown(EVT vt,
            OutParamWrapper<EVT> intermediateVT,
            OutParamWrapper<Integer> numIntermediates,
            OutParamWrapper<EVT> registerVT)
    {
        int numElts = vt.getVectorNumElements();
        EVT eltTy = vt.getVectorElementType();

        int numVectorRegs = 1;
        if (!Util.isPowerOf2(numElts))
        {
            numVectorRegs = numElts;
            numElts = 1;
        }

        while (numElts > 1 && !isTypeLegal(
                new EVT(MVT.getVectorVT(eltTy.getSimpleVT(), numElts))))
        {
            numElts >>>= 1;
            numVectorRegs <<= 1;
        }

        numIntermediates.set(numVectorRegs);
        EVT newVT = new EVT(MVT.getVectorVT(eltTy.getSimpleVT(), numElts));
        if (!isTypeLegal(newVT))
        {
            newVT = eltTy;
        }
        intermediateVT.set(newVT);

        EVT destVT = getRegisterType(newVT);
        registerVT.set(destVT);
        if (destVT.bitsLT(newVT))
        {
            return numVectorRegs * (newVT.getSizeInBits() / destVT.getSizeInBits());
        }
        else
        {
            // Otherwise, promotion or legal types use the same number of registers as
            // the vector decimated to the appropriate level.
            return numVectorRegs;
        }
    }

    public EVT getRegisterType(EVT valueVT)
    {
        if (valueVT.isSimple())
        {
            assert valueVT.getSimpleVT().simpleVT < registerTypeForVT.length;
            return registerTypeForVT[valueVT.getSimpleVT().simpleVT];
        }
        if (valueVT.isVector())
        {
            OutParamWrapper<EVT> registerVT = new OutParamWrapper<>();
            getVectorTypeBreakdown(valueVT, new OutParamWrapper<>(),
                    new OutParamWrapper<>(), registerVT);
            return registerVT.get();
        }
        if (valueVT.isInteger())
        {
            return getRegisterType(getTypeToTransformTo(valueVT));
        }
        assert false : "Unsupported extended type!";
        return new EVT(MVT.Other);
    }

    public int getNumRegisters(EVT valueVT)
    {
        assert valueVT.getSimpleVT().simpleVT < numRegistersForVT.length;
        return numRegistersForVT[valueVT.getSimpleVT().simpleVT];
    }

    private static int getVectorTypeBreakdownMVT(MVT vt,
            OutParamWrapper<MVT> intermediateVT,
            OutParamWrapper<Integer> numIntermediates,
            OutParamWrapper<EVT> registerVT, TargetLowering tli)
    {
        int numElts = vt.getVectorNumElements();
        MVT eltTy = vt.getVectorElementType();

        int numVectorRegs = 1;
        if (!Util.isPowerOf2(numElts))
        {
            numVectorRegs = numElts;
            numElts = 1;
        }

        while (numElts > 1 && !tli
                .isTypeLegal(new EVT(MVT.getVectorVT(eltTy, numElts))))
        {
            numElts >>>= 1;
            numVectorRegs <<= 1;
        }

        numIntermediates.set(numVectorRegs);
        MVT newVT = MVT.getVectorVT(eltTy, numElts);
        if (!tli.isTypeLegal(new EVT(newVT)))
        {
            newVT = eltTy;
        }
        intermediateVT.set(newVT);

        EVT destVT = tli.getRegisterType(new EVT(newVT));
        registerVT.set(destVT);
        if (destVT.bitsLT(new EVT(newVT)))
        {
            return numVectorRegs * (newVT.getSizeInBits() / destVT.getSizeInBits());
        }
        else
        {
            // Otherwise, promotion or legal types use the same number of registers as
            // the vector decimated to the appropriate level.
            return numVectorRegs;
        }
    }

    /**
     * Once all of the register classes are added. this allow use to compute
     * derived properties we expose.
     */
    public void computeRegisterProperties()
    {
        // Everything defaults to needing one register.
        for (int i = 0; i < MVT.LAST_VALUETYPE; ++i)
        {
            numRegistersForVT[i] = 1;
            registerTypeForVT[i] = transformToType[i] = new EVT(new MVT(i));
        }

        // Except for isVoid, which doesn't need any registers.
        numRegistersForVT[MVT.isVoid] = 0;

        // Find the largest integer register class.
        int largetestIntReg = MVT.LAST_INTEGER_VALUETYPE;
        for (; registerClassForVT[largetestIntReg] == null; --largetestIntReg)
            assert largetestIntReg != MVT.i1 : "No integer register defined";

        // Every integer value type larger than this largest register takes twice as
        // many registers to represent as the previous ValueType.
        for (int expandedReg = largetestIntReg + 1; ; ++expandedReg)
        {
            EVT vt = new EVT(new MVT(expandedReg));
            if (!vt.isInteger())
                break;

            numRegistersForVT[expandedReg] =
                    2 * numRegistersForVT[expandedReg - 1];
            registerTypeForVT[expandedReg] = new EVT(new MVT(largetestIntReg));
            transformToType[expandedReg] = new EVT(new MVT(expandedReg - 1));
        }

        // Inspect all of the ValueType's smaller than the largest integer
        // register to see which ones need promotion.
        int legalIntReg = largetestIntReg;
        for (int intReg = largetestIntReg - 1; intReg >= MVT.i1; --intReg)
        {
            EVT ivt = new EVT(new MVT(intReg));
            if (isTypeLegal(ivt))
            {
                legalIntReg = intReg;
            }
            else
            {
                registerTypeForVT[intReg] = transformToType[intReg] = new EVT(
                        new MVT(legalIntReg));
            }
        }

        // ppcf128 type is really two f64's.
        if (!isTypeLegal(new EVT(new MVT(MVT.ppcf128))))
        {
            numRegistersForVT[MVT.ppcf128] = 2 * numRegistersForVT[MVT.f64];
            registerTypeForVT[MVT.ppcf128] = transformToType[MVT.ppcf128] = new EVT(
                    new MVT(MVT.f64));
        }

        // Decide how to handle f64. If the target does not have native f64 support,
        // expand it to i64 and we will be generating soft float library calls.
        if (!isTypeLegal(new EVT(new MVT(MVT.f64))))
        {
            numRegistersForVT[MVT.i64] = numRegistersForVT[MVT.i64];
            registerTypeForVT[MVT.i64] = transformToType[MVT.i64] = new EVT(new MVT(MVT.i64));
        }

        // Decide how to handle f32. If the target does not have native support for
        // f32, promote it to f64 if it is legal. Otherwise, expand it to i32.
        if (!isTypeLegal(new EVT(new MVT(MVT.f32))))
        {
            if (isTypeLegal(new EVT(new MVT(MVT.f64))))
            {
                numRegistersForVT[MVT.f32] = numRegistersForVT[MVT.f64];
                registerTypeForVT[MVT.f32] = registerTypeForVT[MVT.f64];
                transformToType[MVT.f32] = new EVT(new MVT(MVT.f64));
            }
            else
            {
                numRegistersForVT[MVT.f32] = numRegistersForVT[MVT.i32];
                registerTypeForVT[MVT.f32] = registerTypeForVT[MVT.i32];
                transformToType[MVT.f32] = new EVT(new MVT(MVT.i32));
            }
        }

        // Loop over all of the vector value types to see which need transformations.
        for (int i = MVT.FIRST_VECTOR_VALUETYPE; i <= MVT.LAST_VECTOR_VALUETYPE; i++)
        {
            MVT vt = new MVT(i);
            if (!isTypeLegal(new EVT(vt)))
            {
                OutParamWrapper<MVT> intermediateVT = new OutParamWrapper<>(new MVT());
                OutParamWrapper<EVT> registerVT = new OutParamWrapper<>(new EVT());
                OutParamWrapper<Integer> numIntermediates = new OutParamWrapper<>(
                        0);

                numRegistersForVT[i] = getVectorTypeBreakdownMVT(vt,
                        intermediateVT, numIntermediates, registerVT, this);
                registerTypeForVT[i] = registerVT.get();

                boolean isLegalWiderType = false;
                EVT elTvt = new EVT(vt.getVectorElementType());
                int numElts = vt.getVectorNumElements();
                for (int nvt = i + 1; nvt <= MVT.LAST_VECTOR_VALUETYPE; ++nvt)
                {
                    EVT svt = new EVT(new MVT(nvt));
                    if (isTypeLegal(svt) && svt.getVectorElementType().equals(elTvt)
                            && svt.getVectorNumElements() > numElts)
                    {
                        transformToType[i] = svt;
                        valueTypeAction.setTypeAction(new EVT(vt), Promote);
                        isLegalWiderType = true;
                        break;
                    }
                }
                if (!isLegalWiderType)
                {
                    EVT nvt = new EVT(vt.getPower2VectorType());
                    if (nvt.equals(new EVT(vt)))
                    {
                        // Type is already a power of 2.  The default action is to split.
                        transformToType[i] = new EVT(new MVT(MVT.Other));
                        valueTypeAction.setTypeAction(new EVT(vt), Expand);
                    }
                    else
                    {
                        transformToType[i] = nvt;
                        valueTypeAction.setTypeAction(new EVT(vt), Promote);
                    }
                }
            }
        }
    }

    /**
     * Computing the assembly alignment for specified function with target-specific
     * method.
     *
     * @param fn
     * @return
     */
    public abstract int getFunctionAlignment(Function fn);

    public MVT getShiftAmountTy()
    {
        return shiftAmountTy;
    }

    public String getTargetNodeName(int opcode)
    {
        return null;
    }

    public abstract SDValue lowerFormalArguments(SDValue root, CallingConv callingConv,
            boolean varArg, ArrayList<InputArg> ins, SelectionDAG dag, ArrayList<SDValue> inVals);

    public abstract SDValue lowerMemArgument(SDValue chain, CallingConv cc,
            ArrayList<InputArg> argInfo, SelectionDAG dag, CCValAssign va,
            MachineFrameInfo mfi, int i);

    public void computeMaskedBitsForTargetNode(SDValue op, APInt mask,
            APInt[] knownVals, SelectionDAG selectionDAG, int depth)
    {
        int opc = op.getOpcode();
        assert opc >= ISD.BUILTIN_OP_END || opc == ISD.INTRINSIC_VOID
                || opc == ISD.INTRINSIC_W_CHAIN || opc == ISD.INTRINSIC_WO_CHAIN;
        knownVals[0] = knownVals[1] = new APInt(mask.getBitWidth(), 0);
    }

    public abstract SDValue lowerReturn(SDValue chain, CallingConv cc,
            boolean isVarArg, ArrayList<OutputArg> outs, SelectionDAG dag);

    public Pair<SDValue, SDValue> lowerCallTo(SDValue chain, Type retTy,
            boolean retSExt, boolean retZExt, boolean varArg, boolean isInReg,
            int numFixedArgs, CallingConv cc, boolean isTailCall,
            boolean isReturnValueUsed, SDValue callee,
            ArrayList<ArgListEntry> args, SelectionDAG dag)
    {
        assert !isTailCall || EnablePerformTailCallOpt.value;
        ArrayList<OutputArg> outs = new ArrayList<>(32);
        for (int i = 0, e = args.size(); i < e; i++)
        {
            ArrayList<EVT> valueVTs = new ArrayList<>();
            computeValueVTs(this, args.get(i).ty, valueVTs);
            for (int j = 0, sz = valueVTs.size(); j < sz; j++)
            {
                EVT vt = valueVTs.get(j);
                Type argTy = vt.getTypeForEVT();
                SDValue op = new SDValue(args.get(i).node.getNode(),
                        args.get(i).node.getResNo() + j);

                ArgFlagsTy flags = new ArgFlagsTy();
                int originalAlignment = getTargetData().getABITypeAlignment(argTy);

                if (args.get(i).isZExt)
                    flags.setZExt();
                if (args.get(i).isSRet)
                    flags.setSExt();
                if (args.get(i).isInReg)
                    flags.setInReg();
                if (args.get(i).isByVal)
                {
                    flags.setByVal();
                    PointerType pty = (PointerType) args.get(i).ty;
                    Type eltTy = pty.getElementType();
                    int frameAlign = getByValTypeAlignment(eltTy);
                    long frameSize = getTargetData().getTypeAllocSize(eltTy);

                    if (args.get(i).alignment != 0)
                        frameAlign = args.get(i).alignment;
                    flags.setByValAlign(frameAlign);
                    flags.setByValSize((int) frameSize);
                }
                if (args.get(i).isSRet)
                    flags.setSRet();

                flags.setOrigAlign(originalAlignment);

                EVT partVT = getRegisterType(vt);
                int numParts = getNumRegisters(vt);
                SDValue[] parts = new SDValue[numParts];
                int extendKind = ISD.ANY_EXTEND;

                if (args.get(i).isSExt)
                    extendKind = ISD.SIGN_EXTEND;
                else if (args.get(i).isZExt)
                    extendKind = ISD.ZERO_EXTEND;

                getCopyToParts(dag, op, parts, partVT, extendKind);

                for (int k = 0; k < numParts; k++)
                {
                    OutputArg outFlags = new OutputArg(flags, parts[k], i < numFixedArgs);
                    if (numParts > 1 && k == 0)
                        outFlags.flags.setSplit();
                    else if (k != 0)
                        outFlags.flags.setOrigAlign(1);

                    outs.add(outFlags);
                }
            }
        }

        ArrayList<InputArg> ins = new ArrayList<>(32);
        ArrayList<EVT> retTys = new ArrayList<>(4);

        computeValueVTs(this, retTy, retTys);
        for (EVT vt : retTys)
        {
            EVT registerVT = getRegisterType(vt);
            int numRegs = getNumRegisters(vt);
            for (int i = 0; i < numRegs; i++)
            {
                InputArg input = new InputArg();
                input.vt = registerVT;
                input.used = isReturnValueUsed;
                if (retSExt)
                    input.flags.setSExt();
                if (retZExt)
                    input.flags.setZExt();
                if (isInReg)
                    input.flags.setInReg();
                ins.add(input);
            }
        }

        if (isTailCall && !isEligibleTailCallOptimization(callee, cc, varArg,
                ins, dag))
            isTailCall = false;

        ArrayList<SDValue> inVals = new ArrayList<>();
        chain = lowerCall(chain, callee, cc, varArg, isTailCall, outs, ins, dag,
                inVals);

        if (isTailCall)
        {
            dag.setRoot(chain);
            return Pair.get(new SDValue(), new SDValue());
        }

        int assertOp = ISD.DELETED_NODE;
        if (retSExt)
            assertOp = ISD.AssertSext;
        else if (retZExt)
            assertOp = ISD.AssertZext;

        ArrayList<SDValue> returnValues = new ArrayList<>();
        int curReg = 0;
        for (EVT vt : retTys)
        {
            EVT registerVT = getRegisterType(vt);
            int numRegs = getNumRegisters(vt);
            SDValue[] temp = new SDValue[numRegs];
            for (int i = 0; i < numRegs; i++)
                temp[i] = inVals.get(curReg + i);

            SDValue returnValue = getCopyFromParts(dag, temp, registerVT, vt,
                    assertOp);
            for (int i = 0; i < numRegs; i++)
                inVals.set(i + curReg, temp[i]);
            returnValues.add(returnValue);
            curReg += numRegs;
        }

        if (returnValues.isEmpty())
            return Pair.get(new SDValue(), new SDValue());

        SDValue res = dag
                .getNode(ISD.MERGE_VALUES, dag.getVTList(retTys), returnValues);
        return Pair.get(res, chain);
    }

    private int getByValTypeAlignment(Type ty)
    {
        return td.getCallFrameTypeAlignment(ty);
    }

    public boolean isEligibleTailCallOptimization(SDValue calle,
            CallingConv calleeCC, boolean isVarArg, ArrayList<InputArg> ins,
            SelectionDAG dag)
    {
        return false;
    }

    public abstract SDValue lowerCall(SDValue chain, SDValue callee,
            CallingConv cc, boolean isVarArg, boolean isTailCall,
            ArrayList<OutputArg> outs, ArrayList<InputArg> ins,
            SelectionDAG dag, ArrayList<SDValue> inVals);

    public boolean isTruncateFree(Type ty1, Type ty2)
    {
        return false;
    }

    public MachineBasicBlock emitInstrWithCustomInserter(MachineInstr mi,
            MachineBasicBlock mbb)
    {
        Util.shouldNotReachHere("Target must implements this method!");
        return null;
    }

    public boolean isOperationLegal(int opc, EVT vt)
    {
        return (vt.equals(new EVT(MVT.Other)) || isTypeLegal(vt))
                && getOperationAction(opc, vt) == Legal;
    }

    public LegalizeAction getOperationAction(int opc, EVT vt)
    {
        if (vt.isExtended()) return Expand;
        assert opc < opActions[0].length && vt.getSimpleVT().simpleVT <
                opActions[0][0]*8:"Table isn't big enough!";
        int i = vt.getSimpleVT().simpleVT;
        int j = i & 31;
        i = i >> 5;
        return LegalizeAction.values()[(int) ((opActions[i][opc] >> (j*2))&3)];
    }

    public boolean isOperationLegalOrCustom(int opc, EVT vt)
    {
        return (vt.getSimpleVT().simpleVT == MVT.Other || isTypeLegal(vt))
                && (getOperationAction(opc, vt) == Legal ||
                getOperationAction(opc, vt) == Custom);
    }

    public LegalizeAction getLoadExtAction(int lType, EVT vt)
    {
        assert lType < loadExtActions.length &&
                vt.getSimpleVT().simpleVT < 32:"Table isn't big enough!";
        return LegalizeAction.values()[(int) (loadExtActions[lType] >>
                ((2*vt.getSimpleVT().simpleVT)&3))];
    }

    public boolean isLoadExtLegal(int lType, EVT vt)
    {
        return vt.isSimple() && (getLoadExtAction(lType, vt) == Legal ||
            getLoadExtAction(lType, vt) == Custom);
    }

    public LegalizeAction getTruncStoreAction(EVT valVT, EVT memVT)
    {
        assert valVT.getSimpleVT().simpleVT < truncStoreActions.length &&
                memVT.getSimpleVT().simpleVT < 32:"Table isn't big enough!";
        return LegalizeAction.values()[(int) (truncStoreActions[valVT.getSimpleVT().simpleVT] >>
                        ((2*memVT.getSimpleVT().simpleVT)&3))];
    }

    public boolean isTruncStoreLegal(EVT valVT, EVT memVT)
    {
        return isTypeLegal(valVT) && memVT.isSimple() &&
                (getTruncStoreAction(valVT, memVT) == Legal ||
                getTruncStoreAction(valVT, memVT) == Custom);
    }

    public LegalizeAction getIndexedLoadAction(int idxMode, EVT vt)
    {
        assert idxMode < indexedModeActions[0][0].length &&
                vt.getSimpleVT().simpleVT < MVT.LAST_VALUETYPE :
                "Table isn't big enough!";
        return LegalizeAction.values()[(int) indexedModeActions[vt.getSimpleVT().simpleVT][0][idxMode]];
    }

    public boolean isIndexedLoadLegal(int idxMode, EVT vt)
    {
        return vt.isSimple() && (getIndexedLoadAction(idxMode, vt) == Legal ||
                getIndexedLoadAction(idxMode, vt) == Custom);
    }

    public LegalizeAction getIndexedStoreAction(int idxMode, EVT vt)
    {
        assert idxMode < indexedModeActions[0][1].length &&
                vt.getSimpleVT().simpleVT < MVT.LAST_VALUETYPE :
                "Table is't big enough!";
        return LegalizeAction.values()[(int) indexedModeActions[vt.getSimpleVT().simpleVT][1][idxMode]];
    }

    public boolean isIndexedStoreLegal(int idxMode, EVT vt)
    {
        return vt.isSimple() && (getIndexedStoreAction(idxMode, vt) == Legal
            || getIndexedStoreAction(idxMode, vt) == Custom);
    }

    public LegalizeAction getConvertAction(EVT fromVT, EVT toVT)
    {
        assert fromVT.getSimpleVT().simpleVT < condCodeActions.length &&
                toVT.getSimpleVT().simpleVT < 32:"Table isn't big enough!";
        return LegalizeAction.values()[(int) condCodeActions[fromVT.getSimpleVT().simpleVT >>
                (2*toVT.getSimpleVT().simpleVT) & 3]];
    }

    public LegalizeAction getCondCodeAction(CondCode cc, EVT vt)
    {
        assert cc.ordinal() < condCodeActions.length &&
                vt.getSimpleVT().simpleVT < 32:"Table isn't big enough!";
        LegalizeAction action = LegalizeAction.values()[(int) (condCodeActions[cc.ordinal()] >>
                        (2*vt.getSimpleVT().simpleVT)&3)];
        assert action != Promote:"Can't promote condition code!";
        return action;
    }

    public boolean isCondCodeLegal(CondCode cc, EVT vt)
    {
        return getCondCodeAction(cc, vt) == Legal ||
                getCondCodeAction(cc, vt) == Custom;
    }

    public EVT getTypeToPromoteType(int opc, EVT vt)
    {
        assert getOperationAction(opc, vt) == Promote:
                "This operation isn't promoted!";
        Pair<Integer, Integer> key = Pair.get(opc, vt.getSimpleVT().simpleVT);
        if (promoteType.containsKey(key))
            return new EVT(promoteType.get(key));

        assert vt.isInteger() || vt.isFloatingPoint();
        EVT nvt = vt;
        do
        {
            nvt = new EVT(nvt.getSimpleVT().simpleVT+1);
            assert nvt.isInteger() == vt.isInteger() && !nvt.equals(new EVT(MVT.isVoid))
                    : "Didn't find type to promote to!";
        }while (!isTypeLegal(nvt) || getOperationAction(opc, nvt) == Promote);
        return nvt;
    }

    public int getSetCCResultType(EVT vt)
    {
        return pointerTy.simpleVT;
    }
}

