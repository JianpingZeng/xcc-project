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
import backend.codegen.dagisel.SDValue;
import backend.codegen.dagisel.SelectionDAG;
import backend.codegen.fastISel.ISD;
import backend.support.CallingConv;
import backend.type.Type;
import backend.value.Function;
import tools.APInt;
import tools.OutParamWrapper;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;

import static backend.target.TargetLowering.LegalizeAction.Expand;
import static backend.target.TargetLowering.LegalizeAction.Promote;

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
     * @param mf
     * @return
     */
    public abstract MachineFunctionInfo createMachineFunctionInfo(MachineFunction mf);

    public EVT getValueType(Type type)
    {
        return getValueType(type, false);
    }

    /**
     * eturn the EVT corresponding to this LLVM type.
     * This is fixed by the LLVM operations except for the pointer size.  If
     * AllowUnknown is true, this will return MVT::Other for types with no EVT
     * counterpart (e.g. structs), otherwise it will assert.
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
     * @param vt
     * @return
     */
    public boolean isTypeLegal(EVT vt)
    {
        assert !vt.isSimple() || vt.getSimpleVT().simpleVT < registerClassForVT.length;
        return vt.isSimple() && registerClassForVT[vt.getSimpleVT().simpleVT] != null;
    }

    public EVT getTypeForTransformTo(EVT vt)
    {
        if (vt.isSimple())
        {
            assert vt.getSimpleVT().simpleVT < transformToType.length;
            EVT nvt = transformToType[vt.getSimpleVT().simpleVT];
            assert getTypeAction(nvt) != Promote :"Promote may not follow expand or promote";
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
            return getTypeAction(nvt) == Promote ? getTypeForTransformTo(nvt): nvt;
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
                return getTypeAction(nvt) == Promote ? getTypeForTransformTo(nvt) : nvt;
            }
        }
        assert false:"Unsupported extended type!";
        return new EVT(new MVT(MVT.Other));
    }

    /**
     * Return how we should legalize values of this type, either
     * it is already legal (return 'Legal') or we need to promote it to a larger
     × type (return 'Promote'), or we need to expand it into multiple registers
     × of smaller integer type (return 'Expand').  'Custom' is not an option.
     * @param nvt
     * @return
     */
    private LegalizeAction getTypeAction(EVT nvt)
    {
        return valueTypeAction.getTypeAction(nvt);
    }

    public TargetRegisterClass getRegClassFor(EVT vt)
    {
        assert vt.isSimple():"getRegClassFor called on illegal type!";
        TargetRegisterClass rc = registerClassForVT[vt.getSimpleVT().simpleVT];
        assert rc != null:"This value type is not natively supported!";
        return rc;
    }

    public MVT getPointerTy()
    {
        return pointerTy;
    }

    /**
     * Add the specified register class as an available
     /// regclass for the specified value type.  This indicates the selector can
     /// handle values of that class natively.
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

        while (numElts >1 && !isTypeLegal(new EVT(MVT.getVectorVT(eltTy.getSimpleVT(), numElts))))
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
            return numVectorRegs * (newVT.getSizeInBits()/destVT.getSizeInBits());
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
            return getRegisterType(getTypeForTransformTo(valueVT));
        }
        assert false:"Unsupported extended type!";
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

        while (numElts >1 && !tli.isTypeLegal(new EVT(MVT.getVectorVT(eltTy, numElts))))
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
            return numVectorRegs * (newVT.getSizeInBits()/destVT.getSizeInBits());
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
            assert largetestIntReg != MVT.i1:"No integer register defined";

        // Every integer value type larger than this largest register takes twice as
        // many registers to represent as the previous ValueType.
        for (int expandedReg = largetestIntReg+1; ; ++expandedReg)
        {
            EVT vt = new EVT(new MVT(expandedReg));
            if (!vt.isInteger())
                break;

            numRegistersForVT[expandedReg] = 2 * numRegistersForVT[expandedReg-1];
            registerTypeForVT[expandedReg] = new EVT(new MVT(largetestIntReg));
            transformToType[expandedReg] = new EVT(new MVT(expandedReg-1));
        }

        // Inspect all of the ValueType's smaller than the largest integer
        // register to see which ones need promotion.
        int legalIntReg = largetestIntReg;
        for (int intReg = largetestIntReg-1; intReg >= MVT.i1; --intReg)
        {
            EVT ivt = new EVT(new MVT(intReg));
            if (isTypeLegal(ivt))
            {
                legalIntReg = intReg;
            }
            else
            {
                registerTypeForVT[intReg] = transformToType[intReg] =
                        new EVT(new MVT(legalIntReg));
            }
        }

        // ppcf128 type is really two f64's.
        if(!isTypeLegal(new EVT(new MVT(MVT.ppcf128))))
        {
            numRegistersForVT[MVT.ppcf128] = 2*numRegistersForVT[MVT.f64];
            registerTypeForVT[MVT.ppcf128] = transformToType[MVT.ppcf128]
                    = new EVT(new MVT(MVT.f64));
        }

        // Decide how to handle f64. If the target does not have native f64 support,
        // expand it to i64 and we will be generating soft float library calls.
        if (!isTypeLegal(new EVT(new MVT(MVT.f64))))
        {
            numRegistersForVT[MVT.i64] = numRegistersForVT[MVT.i64];
            registerTypeForVT[MVT.i64] = transformToType[MVT.i64]
                    = new EVT(new MVT(MVT.i64));
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
                OutParamWrapper<Integer> numIntermediates = new OutParamWrapper<>(0);

                numRegistersForVT[i] = getVectorTypeBreakdownMVT(vt, intermediateVT,
                        numIntermediates, registerVT, this);
                registerTypeForVT[i] = registerVT.get();

                boolean isLegalWiderType = false;
                EVT elTvt = new EVT(vt.getVectorElementType());
                int numElts = vt.getVectorNumElements();
                for (int nvt = i+1; nvt <= MVT.LAST_VECTOR_VALUETYPE; ++nvt)
                {
                    EVT svt = new EVT(new MVT(nvt));
                    if (isTypeLegal(svt) && svt.getVectorElementType().equals(elTvt) &&
                            svt.getVectorNumElements() > numElts)
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
                                                 boolean varArg, ArrayList<InputArg> ins,
                                                 SelectionDAG dag, ArrayList<SDValue> inVals);

    public abstract SDValue lowerMemArgument(SDValue chain, CallingConv cc,
            ArrayList<InputArg> argInfo, SelectionDAG dag, CCValAssign va,
            MachineFrameInfo mfi, int i);

    public void computeMaskedBitsForTargetNode(SDValue op,
            APInt mask,
            APInt[] knownVals,
            SelectionDAG selectionDAG,
            int depth)
    {
        int opc = op.getOpcode();
        assert opc >= ISD.BUILTIN_OP_END || opc == ISD.INTRINSIC_VOID
                || opc == ISD.INTRINSIC_W_CHAIN || opc == ISD.INTRINSIC_WO_CHAIN;
        knownVals[0] = knownVals[1] = new APInt(mask.getBitWidth(), 0);
    }
}

