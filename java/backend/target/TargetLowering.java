package backend.target;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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
import backend.codegen.selectDAG.FastISel;
import backend.type.Type;
import backend.value.BasicBlock;
import backend.value.Instruction;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.HashMap;

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

    private MVT pointerTy;

    /**
     * This indicates the default register class to use for
     * each ValueType the target supports natively.
     */
    private TargetRegisterClass[] regClassForVT = new TargetRegisterClass[MVT.LAST_VALUETYPE];

    private EVT[] transformToType = new EVT[MVT.LAST_VALUETYPE];

    private ValueTypeAction valueTypeAction = new ValueTypeAction();

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
        return vt == new EVT(new MVT(MVT.iPTR)) ? new EVT(pointerTy) : vt;
    }

    public abstract FastISel createFastISel(MachineFunction mf,
            MachineModuleInfo mmi,
            TObjectIntHashMap vm,
            HashMap<BasicBlock, MachineBasicBlock> bm,
            TObjectIntHashMap<Instruction.AllocaInst> am);

    /**
     * Return true if the target has native support for the
     * specified value type.  This means that it has a register that directly
     * holds it without promotions or expansions.
     * @param vt
     * @return
     */
    public boolean isTypeLegal(EVT vt)
    {
        assert !vt.isSimple() || vt.getSimpleVT().simpleVT < regClassForVT.length;
        return vt.isSimple() && regClassForVT[vt.getSimpleVT().simpleVT] != null;
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
        TargetRegisterClass rc = regClassForVT[vt.getSimpleVT().simpleVT];
        assert rc != null:"This value type is not natively supported!";
        return rc;
    }

    public MVT getPointerTy()
    {
        return pointerTy;
    }
}
