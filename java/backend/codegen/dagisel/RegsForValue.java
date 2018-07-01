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
import backend.target.TargetLowering;
import backend.target.TargetRegisterInfo;
import backend.type.Type;
import gnu.trove.list.array.TIntArrayList;
import tools.OutParamWrapper;
import tools.Util;

import java.util.ArrayList;

import static backend.codegen.dagisel.FunctionLoweringInfo.computeValueVTs;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class RegsForValue
{
    TargetLowering tli;
    ArrayList<EVT> valueVTs;
    ArrayList<EVT> regVTs;
    TIntArrayList regs;

    public RegsForValue()
    {
        valueVTs = new ArrayList<>();
        regVTs = new ArrayList<>();
        regs = new TIntArrayList();
    }

    public RegsForValue(TargetLowering tli,
            TIntArrayList regs,
            EVT regvt, EVT valuevt)
    {
        this();
        this.tli = tli;
        valueVTs.add(valuevt);
        regVTs.add(regvt);
        this.regs = regs;
    }

    public RegsForValue(TargetLowering tli,
            int reg, Type ty)
    {
        this();
        this.tli = tli;
        computeValueVTs(tli, ty, valueVTs);
        for (int i = 0, e = valueVTs.size(); i < e; i++)
        {
            EVT valueVT = valueVTs.get(i);
            int numRegs = tli.getNumRegisters(valueVT);
            EVT registerVT = tli.getRegisterType(valueVT);
            for (int j = 0; j < numRegs; j++)
                regs.add(reg+j);
            regVTs.add(registerVT);
            reg += numRegs;
        }
    }

    public void append(RegsForValue rhs)
    {
        tli = rhs.tli;
        valueVTs.addAll(rhs.valueVTs);
        regVTs.addAll(rhs.regVTs);
        regs.addAll(rhs.regs);
    }

    public SDValue getCopyFromRegs(SelectionDAG dag,
            OutParamWrapper<SDValue> chain,
            OutParamWrapper<SDValue> flag)
    {
        SDValue[] values = new SDValue[valueVTs.size()];

        for (int i = 0, part = 0, e = valueVTs.size(); i < e; i++)
        {
            EVT valueVT = valueVTs.get(i);
            int numRegs = tli.getNumRegisters(valueVT);
            EVT registerVT = regVTs.get(i);

            SDValue[] parts = new SDValue[numRegs];
            for (int j = 0; j < numRegs; j++)
            {
                SDValue p;
                if (flag == null)
                    p = dag.getCopyFromReg(chain.get(), regs.get(part + i), registerVT);
                else
                {
                    p = dag.getCopyFromReg(chain.get(), regs.get(part + i), registerVT, flag.get());
                    flag.set(p.getValue(2));
                }
                chain.set(p.getValue(1));

                if (TargetRegisterInfo.isVirtualRegister(regs.get(part+i)) &&
                        registerVT.isInteger() && !registerVT.isVector())
                {
                    int slotNo = regs.get(part+i) - TargetRegisterInfo.FirstVirtualRegister;
                    FunctionLoweringInfo fli = dag.getFunctionLoweringInfo();
                    if (fli.liveOutRegInfo.size() > slotNo)
                    {
                        FunctionLoweringInfo.LiveOutInfo loi = fli.liveOutRegInfo.get(slotNo);
                        int regSize = registerVT.getSizeInBits();
                        int numSignBits = loi.numSignBits;
                        int numZeroBits = loi.knownZero.countLeadingOnes();

                        boolean isSExt = true;
                        EVT fromVT = new EVT(MVT.Other);
                        if (numSignBits == regSize)
                        {
                            isSExt = true;
                            fromVT = new EVT(MVT.i1);
                        }
                        else if (numZeroBits >= regSize-1)
                        {
                            isSExt = false;
                            fromVT = new EVT(MVT.i1);
                        }
                        else if (numSignBits > regSize - 8)
                        {
                            isSExt = true;
                            fromVT = new EVT(MVT.i8);
                        }
                        else if (numZeroBits >= regSize - 8)
                        {
                            isSExt = false;
                            fromVT = new EVT(MVT.i8);
                        }
                        else if (numSignBits > regSize - 16)
                        {
                            isSExt = false;
                            fromVT = new EVT(MVT.i16);
                        }
                        else if (numZeroBits >= regSize - 16)
                        {
                            isSExt = false;
                            fromVT = new EVT(MVT.i16);
                        }
                        else if (numSignBits > regSize - 32)
                        {
                            isSExt = true;
                            fromVT = new EVT(MVT.i32);
                        }
                        else if (numZeroBits >= regSize - 32)
                        {
                            isSExt = false;
                            fromVT = new EVT(MVT.i32);
                        }
                        if (!fromVT.equals(new EVT(MVT.Other)))
                        {
                            p = dag.getNode(isSExt? ISD.AssertSext: ISD.AssertZext,
                                    registerVT, p, dag.getValueType(fromVT));
                        }
                    }
                }
                parts[j] = p;
            }

            values[i] = getCopyFromParts(dag, parts, registerVT, valueVT);
            part += numRegs;
        }
        EVT[] vts = new EVT[valueVTs.size()];
        valueVTs.toArray(vts);
        return dag.getNode(ISD.MERGE_VALUES, dag.getVTList(vts), values);
    }

    public void getCopyToRegs(SDValue val,
            SelectionDAG dag,
            OutParamWrapper<SDValue> chain,
            OutParamWrapper<SDValue> flag)
    {
        int numRegs = regs.size();
        ArrayList<SDValue> parts = new ArrayList<>(numRegs);
        for (int i = 0; i < numRegs; i++) parts.add(new SDValue());

        for (int value = 0, part = 0, e = valueVTs.size(); value < e; value++)
        {
            EVT valueVT = valueVTs.get(value);
            int numParts = tli.getNumRegisters(valueVT);
            EVT registerVT = regVTs.get(value);

            SDValue[] temp = new SDValue[numParts];
            for (int i = part; i < part+numParts; i++)
                temp[i-part] = parts.get(part);

            getCopyToParts(dag, val.getValue(val.getResNo() + value), temp, registerVT);

            for (int i = part; i < part+numParts; i++)
                parts.set(part, temp[i-part]);

            part += numParts;
        }

        ArrayList<SDValue> chains = new ArrayList<>();
        for (int i = 0; i < numRegs; i++) chains.add(new SDValue());
        for (int i = 0; i < numRegs; i++)
        {
            SDValue part;
            if (flag == null)
                part = dag.getCopyToReg(chain.get(), regs.get(i), parts.get(i));
            else
            {
                part = dag.getCopyToReg(chain.get(), regs.get(i), parts.get(i), flag.get());
                flag.set(part.getValue(1));
            }
            chains.set(i, part.getValue(0));
        }

        if (numRegs == 1 || flag != null)
            chain.set(chains.get(numRegs-1));
        else
        {
            SDValue[] temp = new SDValue[numRegs];
            for (int i = 0; i < numRegs; i++)
                temp[i] = chains.get(i);
            chain.set(dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), temp));
        }
    }

    public static SDValue getCopyFromParts(SelectionDAG dag, SDValue[] parts,
            EVT partVT, EVT valueVT)
    {
        return getCopyFromParts(dag, parts, partVT, valueVT, ISD.DELETED_NODE);
    }

    public static SDValue getCopyFromParts(SelectionDAG dag, SDValue[] parts,
            EVT partVT, EVT valueVT, int assertOp)
    {
        int numParts = parts.length;
        Util.assertion(numParts > 0, "No parts to assemble!");
        TargetLowering tli = dag.getTargetLoweringInfo();
        SDValue val = parts[0];
        if (numParts > 1)
        {
            if (!valueVT.isVector() && valueVT.isInteger())
            {
                int partBits = partVT.getSizeInBits();
                int valueBits = valueVT.getSizeInBits();

                int roundParts = (numParts & (numParts-1)) != 0 ?
                    1 << Util.log2(numParts) : numParts;
                int roundBits = partBits * roundParts;
                EVT roundVT = roundBits == valueBits ? valueVT : EVT.getIntegerVT(roundBits);
                SDValue lo, hi;

                EVT halfVT = EVT.getIntegerVT(roundBits/2);
                if (roundParts > 2)
                {
                    SDValue[] temps = new SDValue[roundParts/2];
                    System.arraycopy(parts, 0, temps, 0, roundParts/2);
                    lo = getCopyFromParts(dag, temps, partVT, halfVT);
                    System.arraycopy(parts, roundParts/2, temps, 0, roundParts/2);
                    hi = getCopyFromParts(dag, temps, partVT, halfVT);
                }
                else
                {
                    lo = dag.getNode(ISD.BIT_CONVERT, halfVT, parts[0]);
                    hi = dag.getNode(ISD.BIT_CONVERT, halfVT, parts[1]);
                }
                if (tli.isBigEndian())
                {
                    SDValue temp = lo;
                    lo = hi;
                    hi = temp;
                }
                val = dag.getNode(ISD.BUILD_PAIR, roundVT, lo, hi);
                if (roundParts < numParts)
                {
                    int oddParts = numParts - roundParts;
                    EVT oddVT = EVT.getIntegerVT(oddParts*partBits);
                    SDValue[] temps = new SDValue[oddParts];
                    System.arraycopy(parts, roundParts, temps, 0, oddParts);
                    hi = getCopyFromParts(dag, temps, partVT, oddVT);

                    lo = val;
                    if (tli.isBigEndian())
                    {
                        SDValue temp = lo;
                        lo = hi;
                        hi = temp;
                    }
                    EVT totalVT = EVT.getIntegerVT(numParts*partBits);
                    hi = dag.getNode(ISD.ANY_EXTEND, totalVT, hi);
                    hi = dag.getNode(ISD.SHL, totalVT, hi,
                            dag.getConstant(lo.getValueType().getSizeInBits(),
                            new EVT(tli.getPointerTy()), false));
                    lo = dag.getNode(ISD.ZERO_EXTEND, totalVT, lo);
                    val = dag.getNode(ISD.OR, totalVT, lo, hi);
                }
            }
            else if (valueVT.isVector())
            {
                OutParamWrapper<EVT> intermediateVT = new OutParamWrapper<>(),
                        registerVT = new OutParamWrapper<>();
                OutParamWrapper<Integer> numIntermidates = new OutParamWrapper<>(0);

                int numRegs = tli.getVectorTypeBreakdown(valueVT, intermediateVT,
                        numIntermidates, registerVT);
                Util.assertion( numRegs ==numParts);
                numParts = numRegs;
                Util.assertion( registerVT.get().equals(partVT));
                Util.assertion( registerVT.get().equals(parts[0].getValueType()));

                ArrayList<SDValue> ops = new ArrayList<>();
                if (numIntermidates.get() == numParts)
                {
                    for (int i = 0; i < numParts; i++)
                    {
                        SDValue[] temp = new SDValue[1];
                        temp[0] = parts[i];
                        ops.add(getCopyFromParts(dag, temp, partVT, intermediateVT.get()));
                    }
                }
                else if (numParts > 0)
                {
                    Util.assertion( numParts % numIntermidates.get() == 0);
                    int factor = numParts / numIntermidates.get();
                    for (int i = 0; i < numIntermidates.get(); i++)
                    {
                        SDValue[] temp = new SDValue[factor];
                        System.arraycopy(parts, i*factor, temp, 0, factor);
                        ops.add(getCopyFromParts(dag, temp, partVT, intermediateVT.get()));
                    }
                }
                SDValue[] temp = new SDValue[numIntermidates.get()];
                System.arraycopy(ops.toArray(),0, temp, 0, temp.length);
                val = dag.getNode(intermediateVT.get().isVector()?
                ISD.CONCAT_VECTORS: ISD.BUILD_VECTOR, valueVT, temp);
            }
            else if (partVT.isFloatingPoint())
            {
                Util.assertion( valueVT.equals(new EVT(MVT.ppcf128)) || partVT.equals(new EVT(MVT.f64)));
                SDValue lo, hi;
                lo = dag.getNode(ISD.BIT_CONVERT, new EVT(MVT.f64), parts[0]);
                hi = dag.getNode(ISD.BIT_CONVERT, new EVT(MVT.f64), parts[1]);
                if (tli.isBigEndian())
                {
                    SDValue temp = lo;
                    lo = hi;
                    hi = temp;
                }
                val = dag.getNode(ISD.BUILD_VECTOR, valueVT, lo, hi);
            }
            else
            {
                Util.assertion( valueVT.isFloatingPoint() && partVT.isInteger() &&!partVT.isVector());
                EVT intVT = EVT.getIntegerVT(valueVT.getSizeInBits());
                SDValue[] temp = new SDValue[numParts];
                System.arraycopy(parts, 0, temp, 0, numParts);
                val = getCopyFromParts(dag, temp, partVT, intVT);
            }
        }

        partVT = val.getValueType();
        if (partVT.equals(valueVT))
            return val;

        if (partVT.isVector())
        {
            Util.assertion( valueVT.isVector());
            return dag.getNode(ISD.BIT_CONVERT, valueVT, val);
        }

        if (valueVT.isVector())
        {
            Util.assertion( valueVT.getVectorElementType().equals(partVT) &&                    valueVT.getVectorNumElements() == 1);

            return dag.getNode(ISD.BUILD_VECTOR, valueVT, val);
        }

        if (partVT.isInteger() && valueVT.isInteger())
        {
            if (valueVT.bitsLT(partVT))
            {
                if (assertOp != ISD.DELETED_NODE)
                    val = dag.getNode(assertOp, partVT, val, dag.getValueType(valueVT));
                return dag.getNode(ISD.TRUNCATE, valueVT, val);
            }
            else
                return dag.getNode(ISD.ANY_EXTEND, valueVT, val);
        }

        if (partVT.isFloatingPoint() && valueVT.isFloatingPoint())
        {
            if (valueVT.bitsLT(val.getValueType()))
                return dag.getNode(ISD.FP_ROUND, valueVT, val, dag.getIntPtrConstant(1));
            return dag.getNode(ISD.FP_EXTEND, valueVT, val);
        }

        if (partVT.getSizeInBits() == valueVT.getSizeInBits())
            return dag.getNode(ISD.BIT_CONVERT, valueVT, val);

        Util.shouldNotReachHere("Unknown mismatch!");
        return new SDValue();
    }
    public static void getCopyToParts(SelectionDAG dag, SDValue val,
            SDValue[] parts, EVT partVT)
    {
        getCopyToParts(dag, val, parts, partVT, ISD.ANY_EXTEND);
    }

    public static void getCopyToParts(SelectionDAG dag, SDValue val,
            SDValue[] parts, EVT partVT,
            int extendKind)
    {
        int numParts = parts.length;
        TargetLowering tli = dag.getTargetLoweringInfo();
        EVT ptrVT = new EVT(tli.getPointerTy());
        EVT valueVT = val.getValueType();
        int partBits = partVT.getSizeInBits();
        int originNumParts = numParts;
        Util.assertion(tli.isTypeLegal(partVT), "Copying to an illegal type!");

        if (numParts <= 0) return;

        if (!valueVT.isVector())
        {
            if (partVT.equals(valueVT))
            {
                Util.assertion(numParts == 1, "No-op copy with multiple parts!");
                parts[0] = val;
                return;
            }

            if (numParts * partBits > valueVT.getSizeInBits())
            {
                if (partVT.isFloatingPoint() && valueVT.isFloatingPoint())
                {
                    Util.assertion( numParts == 1);
                    val = dag.getNode(ISD.FP_EXTEND, partVT, val);
                }
                else if (partVT.isInteger() && valueVT.isInteger())
                {
                    valueVT = EVT.getIntegerVT(numParts*partBits);
                    val = dag.getNode(extendKind, valueVT, val);
                }
                else
                {
                    Util.shouldNotReachHere("Unknown mismatch!");
                }
            }
            else if (partBits == valueVT.getSizeInBits())
            {
                Util.assertion( numParts == 1 && !partVT.equals(valueVT));
                val = dag.getNode(ISD.BIT_CONVERT, partVT, val);
            }
            else if (numParts * partBits < valueVT.getSizeInBits())
            {
                if (partVT.isInteger() && valueVT.isInteger())
                {
                    valueVT = EVT.getIntegerVT(numParts*partBits);
                    val = dag.getNode(ISD.TRUNCATE, valueVT, val);
                }
                else
                    Util.shouldNotReachHere("Unknown mismatch!");
            }

            valueVT = val.getValueType();
            Util.assertion( numParts*partBits == valueVT.getSizeInBits());

            if (numParts == 1)
            {
                Util.assertion( partVT.equals(valueVT));
                parts[0] = val;
                return;
            }

            if ((numParts & (numParts - 1)) != 0)
            {
                Util.assertion( partVT.isInteger() && valueVT.isInteger());
                int roundParts = 1 << Util.log2(numParts);
                int roundBits = roundParts * partBits;
                int oddParts = numParts - roundParts;
                SDValue oddVal = dag.getNode(ISD.SRL, valueVT, val,
                        dag.getConstant(roundBits, new EVT(tli.getPointerTy()),
                                false));
                SDValue[] temp = new SDValue[oddParts];
                System.arraycopy(parts, roundParts, temp, 0, oddParts);
                getCopyToParts(dag, oddVal, temp, partVT);
                if (tli.isBigEndian())
                {
                    for (int i = roundParts; i < (roundParts + numParts) / 2; i++)
                    {
                        SDValue t = parts[i];
                        parts[i] = parts[numParts - 1 - i];
                        parts[numParts - 1 - i] = t;
                    }

                    numParts = roundParts;
                    valueVT = EVT.getIntegerVT(numParts * numParts);
                    val = dag.getNode(ISD.TRUNCATE, valueVT, val);
                }

                parts[0] = dag.getNode(ISD.BIT_CONVERT, EVT.getIntegerVT(valueVT.getSizeInBits()),
                        val);
                for (int stepSize = numParts; stepSize > 1; stepSize /= 2)
                {
                    for (int i = 0; i < numParts; i += stepSize)
                    {
                        int thisBits = stepSize * partBits / 2;
                        EVT thisVT = EVT.getIntegerVT(thisBits);
                        SDValue part0 = parts[i];
                        SDValue part1 = parts[i + stepSize / 2];

                        part1 = dag.getNode(ISD.EXTRACT_ELEMENT, thisVT, part0,
                                dag.getConstant(1, ptrVT, false));
                        part0 = dag.getNode(ISD.EXTRACT_ELEMENT, thisVT, part0,
                                dag.getConstant(0, ptrVT, false));

                        if (thisBits == partBits && !thisVT.equals(partVT))
                        {
                            part0 = dag.getNode(ISD.BIT_CONVERT, partVT, part0);
                            part1 = dag.getNode(ISD.BIT_CONVERT, partVT, part1);
                        }
                        parts[i] = part0;
                        parts[i + stepSize / 2] = part1;
                    }
                }

                if (tli.isBigEndian())
                {
                    for (int i = 0; i < originNumParts / 2; i++)
                    {
                        SDValue t = parts[i];
                        parts[i] = parts[originNumParts - 1 - i];
                        parts[originNumParts - 1 - i] = t;
                    }
                }
                return;
            }
        }

        if (numParts == 1)
        {
            if (!partVT.equals(valueVT))
            {
                if (partVT.isVector())
                {
                    val = dag.getNode(ISD.BIT_CONVERT, partVT, val);
                }
                else
                {
                    Util.assertion( valueVT.getVectorElementType().equals(partVT)
                            && valueVT.getVectorNumElements() == 1);

                    val = dag.getNode(ISD.EXTRACT_VECTOR_ELT, partVT, val,
                            dag.getConstant(0, ptrVT, false));
                }
            }
            parts[0] = val;
            return;
        }

        OutParamWrapper<EVT> intermidiateVT = new OutParamWrapper<>(),
                registerVT = new OutParamWrapper<>();
        OutParamWrapper<Integer> numIntermediates = new OutParamWrapper<>(0);
        int numRegs = tli.getVectorTypeBreakdown(valueVT, intermidiateVT, numIntermediates,
                registerVT);

        int numElts = valueVT.getVectorNumElements();
        Util.assertion( numRegs == numParts);
        numParts = numRegs;
        Util.assertion( registerVT.get().equals(partVT));

        ArrayList<SDValue> ops = new ArrayList<>();
        for (int i = 0; i < numIntermediates.get(); i++)
        {
            if (intermidiateVT.get().isVector())
                ops.add(dag.getNode(ISD.EXTRACT_SUBVECTOR, intermidiateVT.get(),
                        dag.getConstant(i*(numElts/numIntermediates.get()),
                                ptrVT, false)));
            else
                ops.add(dag.getNode(ISD.EXTRACT_VECTOR_ELT,
                        intermidiateVT.get(), val,
                        dag.getConstant(i, ptrVT, false)));
        }

        if (numParts == numIntermediates.get())
        {
            for (int i = 0; i < numParts; i++)
            {
                SDValue[] temp = new SDValue[1];
                temp[0] = parts[i];
                getCopyToParts(dag, ops.get(i), temp, partVT);
            }
        }
        else if (numParts > 0)
        {
            Util.assertion( numParts % numIntermediates.get() == 0);
            int factor = numParts / numIntermediates.get();
            for (int i = 0; i < numIntermediates.get();i++)
            {
                SDValue[] temp = new SDValue[factor];
                System.arraycopy(parts, i*factor, temp, 0, factor);
                getCopyToParts(dag, ops.get(i), temp, partVT);
            }
        }
    }
}
