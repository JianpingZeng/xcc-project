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

import backend.codegen.*;
import backend.codegen.fastISel.ISD;
import backend.value.*;
import tools.*;

import java.io.PrintStream;
import java.util.ArrayList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class SDNode implements Comparable<SDNode>, FoldingSetNode
{
    protected int opcode;
    protected int sublassData;
    protected int nodeID;
    protected SDUse[] operandList;
    protected EVT[] valueList;
    protected ArrayList<SDUse> useList;

    private static EVT[] getValueTypeList(EVT vt)
    {
    }

    public int getOpcode()
    {
        assert opcode >= 0 : "Is a machine operator?";
        return opcode;
    }

    public boolean isTargetOpcode()
    {
        return opcode >= NodeType.BUILTIN_OP_END.ordinal();
    }

    public boolean isMachineOperand()
    {
        return opcode < 0;
    }

    public int getMachineOpcode()
    {
        assert isMachineOperand();
        return ~opcode;
    }

    public boolean isUseEmpty()
    {
        return useList == null;
    }

    public boolean hasOneUse()
    {
        return !isUseEmpty() && useList.size() == 1;
    }

    public int getUseSize()
    {
        return useList.size();
    }

    public int getNodeID()
    {
        return nodeID;
    }

    public boolean hasNumUsesOfValue(int numOfUses, int value)
    {
    }

    public boolean hasAnyUseOfValue(int value)
    {
    }

    public boolean isOnlyUserOf(SDNode node)
    {
    }

    public boolean isOperandOf(SDNode node)
    {
    }

    public boolean isPredecessor(SDNode node)
    {
    }

    public static FltSemantics EVTToAPFloatSemantics(EVT vt)
    {
        switch (vt.getSimpleVT().simpleVT)
        {
            case MVT.f32: return APFloat.IEEEsingle;
            case MVT.f64: return APFloat.IEEEdouble;
            case MVT.f80: return APFloat.x87DoubleExtended;
            case MVT.f128: return APFloat.IEEEquad;
            default:
                Util.shouldNotReachHere("Unknown FP format!");
                return null;
        }
    }

    public boolean isNormalLoad(SDNode node)
    {

    }

    public boolean isNONExtLoad(SDNode node)
    {}

    public boolean isExtLoad(SDNode node)
    {}

    public boolean isSEXTLoad(SDNode node)
    {}

    public boolean isZEXTLoad(SDNode node)
    {}

    public boolean isUNINDEXEDLoad(SDNode node)
    {}

    public boolean isNormalStore(SDNode node)
    {}

    public boolean isNONTRUNCStore(SDNode node)
    {}

    public boolean isTRUNCStore(SDNode node)
    {}

    public boolean isUNINDEXEDStore(SDNode node)
    {}

    public int getNumOperands()
    {
        return operandList.length;
    }

    public long getConstantOperandVal(int num)
    {
    }

    public SDValue getOperand(int num)
    {
        assert num <= getNumOperands() && num >= 0;
        return operandList[num].val;
    }

    public SDUse[] getOperandList()
    {
        return operandList;
    }

    public SDNode getFlaggedNode()
    {
        if (getNumOperands() != 0 && getOperand(getNumOperands() - 1).getValueType().getSimpleVT().
                equals(new EVT(MVT.Flag)))
        {
            return getOperand(getNumOperands() - 1).getNode();
        }
        return null;
    }

    public SDNode getFlaggedMachineNode()
    {
        SDNode res = this;
        while (!res.isMachineOperand())
        {
            SDNode n = res.getFlaggedNode();
            if (n == null)
                break;
            res = n;
        }
        return res;
    }

    public int getNumValues()
    {
        return valueList.length;
    }

    public EVT getValueType(int resNo)
    {
        assert resNo <= getNumValues() && resNo >= 0;
        return valueList[resNo];
    }

    public int getValueSizeInBits(int resNo)
    {
        return getValueType(resNo).getSizeInBits();
    }

    public SDVTList getValueList()
    {
        return new SDVTList(valueList);
    }

    public String getOperationName()
    {
        return getOperationName(null);
    }

    public String getOperationName(SelectionDAG dag)
    {
    }

    public static String getIndexedModeName(MemIndexedMode am)
    {
    }

    public void printTypes(PrintStream os)
    {
        printTypes(os, null);
    }

    public void printTypes(PrintStream os, SelectionDAG dag)
    {
    }

    public void printDetails(PrintStream os, SelectionDAG dag)
    {
    }

    public void print(PrintStream os)
    {
        print(os, null);
    }

    public void print(PrintStream os, SelectionDAG dag)
    {
    }

    public void printr(PrintStream os, SelectionDAG dag)
    {
    }

    public void printr(PrintStream os)
    {
        printr(os, null);
    }

    public void dump()
    {
    }

    public void dumpr()
    {
    }

    public void dump(SelectionDAG dag)
    {

    }

    public void addUse(SDUse use)
    {
        assert use != null;
        useList.add(use);
    }

    public int compareTo(SDNode node)
    {
        return 0;
    }

    @Override
    public void profile(FoldingSetNodeID id)
    {
        SelectionDAG.addNodeToID(id, this);
    }

    static class SDVTList
    {
        EVT[] vts;
        int numVTs;

        public SDVTList()
        {}

        public SDVTList(EVT[] vts)
        {
            this.vts = vts;
            this.numVTs = vts.length;
        }
    }

    protected static SDVTList getSDVTList(EVT vt)
    {
        SDVTList list = new SDVTList();
        list.vts = getValueTypeList(vt);
        list.numVTs = 1;
        return list;
    }

    protected SDNode(int opc, SDVTList vts, ArrayList<SDValue> ops)
    {
        this.opcode = opc;
        sublassData = 0;
        nodeID = -1;
        operandList = ops.size() != 0 ? new SDUse[ops.size()]: null;
        valueList = vts.vts;
        for (int i = 0; i < ops.size(); i++)
        {
            operandList[i].setUser(this);
            operandList[i].setInitial(ops.get(i));
        }
    }

    protected SDNode(int opc, SDVTList vts, SDValue[] ops)
    {
        this.opcode = opc;
        sublassData = 0;
        nodeID = -1;
        operandList = ops.length != 0 ? new SDUse[ops.length]: null;
        valueList = vts.vts;
        for (int i = 0; i < ops.length; i++)
        {
            operandList[i].setUser(this);
            operandList[i].setInitial(ops[i]);
        }
    }

    protected SDNode(int opc, SDVTList list)
    {
        opcode = opc;
        sublassData = 0;
        nodeID = -1;
        operandList = null;
        valueList = list.vts;
    }

    protected void initOperands(SDValue op0)
    {
        operandList = new SDUse[1];
        operandList[0] = new SDUse();
        operandList[0].setUser(this);
        operandList[0].setInitial(op0);
    }

    protected void initOperands(SDValue op0, SDValue op1)
    {
        operandList = new SDUse[2];
        operandList[0] = new SDUse();
        operandList[1] = new SDUse();
        operandList[0].setUser(this);
        operandList[0].setInitial(op0);
        operandList[1].setUser(this);
        operandList[2].setInitial(op1);
    }

    protected void initOperands(SDValue op0, SDValue op1, SDValue op2)
    {
        operandList = new SDUse[3];
        operandList[0] = new SDUse();
        operandList[1] = new SDUse();
        operandList[2] = new SDUse();

        operandList[0].setUser(this);
        operandList[0].setInitial(op0);
        operandList[1].setUser(this);
        operandList[1].setInitial(op1);
        operandList[2].setUser(this);
        operandList[2].setInitial(op2);
    }

    protected void initOperands(SDValue op0, SDValue op1, SDValue op2, SDValue op3)
    {
        operandList = new SDUse[4];
        operandList[0] = new SDUse();
        operandList[1] = new SDUse();
        operandList[2] = new SDUse();
        operandList[3] = new SDUse();

        operandList[0].setUser(this);
        operandList[0].setInitial(op0);
        operandList[1].setUser(this);
        operandList[1].setInitial(op1);
        operandList[2].setUser(this);
        operandList[2].setInitial(op2);
        operandList[3].setUser(this);
        operandList[3].setInitial(op3);
    }

    protected void initOperands(ArrayList<SDValue> vals)
    {
        assert vals != null && vals.size() > 0:"Illegal values for initialization!";
        operandList = new SDUse[vals.size()];
        for (int i = 0; i < operandList.length; i++)
        {
            operandList[i] = new SDUse();
            operandList[i].setUser(this);
            operandList[i].setInitial(vals.get(i));
        }
    }

    protected void initOperands(SDValue... vals)
    {
        assert vals != null && vals.length > 0:"Illegal values for initialization!";
        operandList = new SDUse[vals.length];
        for (int i = 0; i < operandList.length; i++)
        {
            operandList[i] = new SDUse();
            operandList[i].setUser(this);
            operandList[i].setInitial(vals[i]);
        }
    }

    protected void dropOperands()
    {
        for (SDUse anOperandList : operandList)
        {
            anOperandList.set(new SDValue());
        }
    }

    public static class UnarySDNode extends SDNode
    {
        public UnarySDNode(int opc, SDVTList vts, SDValue op)
        {
            super(opc, vts);
            initOperands(op);
        }
    }

    public static class BinarySDNode extends SDNode
    {
        public BinarySDNode(int opc, SDVTList vts, SDValue op0, SDValue op1)
        {
            super(opc, vts);
            initOperands(op0, op1);
        }
    }

    public static class TernarySDNode extends SDNode
    {
        public TernarySDNode(int opc, SDVTList vts, SDValue op0, SDValue op1, SDValue op2)
        {
            super(opc, vts);
            initOperands(op0, op1, op2);
        }
    }

    /**
     * SDNode for memory operation.
     */
    public static class MemSDNode extends SDNode
    {
        private EVT memoryVT;
        private Value srcValue;
        private int svOffset;

        public MemSDNode(int opc, SDVTList vts, EVT memVT, Value srcVal, int svOff,
                int alignment, boolean isVolatile)
        {
            super(opc, vts);
        }

        public MemSDNode(int opc, SDVTList vts, SDValue[] ops, EVT memVT,
                Value srcVal, int svOff, int alignment, boolean isVotatile)
        {
            super(opc, vts);
        }

        public int getAlignment()
        {
            return (1 << (sublassData >> 6)) >> 1;
        }

        public boolean isVolatile()
        {
            return ((sublassData >> 5) & 0x1) != 0;
        }

        public int getRawSubclassData()
        {
            return sublassData;
        }

        public Value getSrcValue()
        {
            return srcValue;
        }

        public int getSrcValueOffset()
        {
            return svOffset;
        }

        public EVT getMemoryVT()
        {
            return memoryVT;
        }

        public MachineMemOperand getMemOperand()
        {
            int flags = 0;
            if (this instanceof LoadSDNode)
                flags = MachineMemOperand.MOLoad;
            else if (this instanceof StoreSDNode)
                flags = MachineMemOperand.MOStore;
            else if (this instanceof AtomicSDNode)
                flags = MachineMemOperand.MOLoad | MachineMemOperand.MOStore;
            else
            {
                assert false:"MemIntrinsic not supported!";
            }
            // alignment it in 1 byte.
            int size = (getMemoryVT().getSizeInBits() + 7) >> 3;
            if (isVolatile()) flags |= MachineMemOperand.MOVolatile;

            FrameIndexSDNode fi = getBasePtr().getNode() instanceof FrameIndexSDNode? ((FrameIndexSDNode) getBasePtr().getNode()): null;
            if (getSrcValue() == null && fi != null)
            {
                return new MachineMemOperand(PseudoSourceValue.getFixedStack(fi.getFrameIndex()),
                        flags, 0, size, getAlignment());
            }
            else
                return new MachineMemOperand(getSrcValue(),
                        flags, getSrcValueOffset(), size, getAlignment());
        }

        public SDValue getChain()
        {
            return getOperand(0);
        }

        public SDValue getBasePtr()
        {
            return getOperand(getOpcode() == ISD.STORE ? 2 : 1);
        }
    }

    public static class AtomicSDNode extends MemSDNode
    {
        public AtomicSDNode(int opc, SDVTList vts,
                EVT memVT,
                SDValue chain,
                SDValue ptr,
                SDValue cmp,
                SDValue swp,
                Value srcVal)
        {
            this(opc, vts, memVT, chain, ptr, cmp, swp, srcVal, 0);
        }
        public AtomicSDNode(int opc, SDVTList vts,
                EVT memVT,
                SDValue chain,
                SDValue ptr,
                SDValue cmp,
                SDValue swp,
                Value srcVal,
                int align)
        {
            super(opc, vts, memVT, srcVal, 0, align, true);
            initOperands(chain, ptr, cmp, swp);
        }

        public AtomicSDNode(int opc, SDVTList vts,
                EVT memVT,
                SDValue chain,
                SDValue ptr,
                SDValue val,
                Value srcVal,
                int align)
        {
            super(opc, vts, memVT, srcVal, 0, align, true);
            initOperands(chain, ptr, val);
        }

        @Override
        public SDValue getBasePtr()
        {
            return getOperand(1);
        }

        public SDValue getVal()
        {
            return getOperand(2);
        }

        public boolean isCompareAndSwap()
        {
            return getOpcode() == ISD.ATOMIC_CMP_SWAP;
        }
    }

    public static class MemIntrinsicSDNode extends MemSDNode
    {
        private boolean raedMem;
        private boolean writeMem;

        public MemIntrinsicSDNode(int opc, SDVTList vts, SDValue[] ops,
                EVT memVT, Value srcValue, int svOff,
                int align, boolean isVotatile,
                boolean readMem,
                boolean writeMem)
        {
            super(opc, vts, ops, memVT, srcValue, svOff, align, isVotatile);
            this.raedMem = readMem;
            this.writeMem = writeMem;
        }

        public boolean isRaedMem()
        {
            return raedMem;
        }

        public boolean isWriteMem()
        {
            return writeMem;
        }
    }

    public static class ConstantSDNode extends SDNode
    {
        private ConstantInt value;
        public ConstantSDNode(boolean isTarget, ConstantInt val, EVT vt)
        {
            super(isTarget?ISD.TargetConstant : ISD.Constant, getSDVTList(vt));
            value = val;
        }

        public ConstantInt getConstantIntValue()
        {
            return value;
        }

        public APInt getAPIntValue()
        {
            return value.getValue();
        }

        public long getZExtValue()
        {
            return value.getZExtValue();
        }

        public long getSExtValue()
        {
            return value.getSExtValue();
        }

        public boolean isNullValue()
        {
            return value.isNullValue();
        }

        public boolean isAllOnesValue()
        {
            return value.isAllOnesValue();
        }
    }

    public static class ConstantFPSDNode extends SDNode
    {
        private ConstantFP value;
        public ConstantFPSDNode(boolean isTarget, ConstantFP val, EVT vt)
        {
            super(isTarget?ISD.TargetConstantFP:ISD.ConstantFP, getSDVTList(vt));;
            value = val;
        }

        public APFloat getValueAPF()
        {
            return value.getValueAPF();
        }

        public ConstantFP getConstantFPValue()
        {
            return value;
        }

        public boolean isExactlyValue(double v)
        {
            APFloat tmp = new APFloat(v);
            OutParamWrapper<Boolean> ignored = new OutParamWrapper<>(false);
            tmp.convert(value.getValueAPF().getSemantics(),
                    APFloat.RoundingMode.rmNearestTiesToEven, ignored);
            return isExactlyValue(tmp);
        }

        public boolean isExactlyValue(APFloat v)
        {
            return getValueAPF().bitwiseIsEqual(v);
        }

        public boolean isValueValidForType(EVT vt, APFloat val)
        {
            assert vt.isFloatingPoint():"Can only convert between FP types!";
            APFloat val2 = new APFloat(val);
            OutParamWrapper<Boolean> ignored = new OutParamWrapper<>(false);
            val2.convert(EVTToAPFloatSemantics(vt), APFloat.RoundingMode.rmNearestTiesToEven,
                    ignored);
            return !ignored.get();
        }
    }

    public static class GlobalAddressSDNode extends SDNode
    {
        private GlobalValue gv;
        private long offset;
        private int tsFlags;

        public GlobalAddressSDNode(int opc, EVT vt, GlobalValue gv, long off, int targetFlags)
        {
            super(opc, getSDVTList(vt));
            this.gv = gv;
            this.offset = off;
            this.tsFlags = targetFlags;
        }

        public GlobalValue getGlobalValue()
        {
            return gv;
        }

        public long getOffset()
        {
            return offset;
        }
        public int getTargetFlags()
        {
            return tsFlags;
        }
    }

    public static class FrameIndexSDNode extends SDNode
    {
        private int frameIndex;
        public FrameIndexSDNode(int fi, EVT vt, boolean isTarget)
        {
            super(isTarget?ISD.TargetFrameIndex:ISD.FrameIndex, getSDVTList(vt));
            this.frameIndex = fi;
        }

        public int getFrameIndex()
        {
            return frameIndex;
        }

        public void setFrameIndex(int frameIndex)
        {
            this.frameIndex = frameIndex;
        }
    }

    public static class JumpTableSDNode extends SDNode
    {
        private int jumpTableIndex;
        public JumpTableSDNode(int jit, EVT vt, boolean isTarget)
        {
            super(isTarget?ISD.TargetJumpTable:ISD.JumpTable, getSDVTList(vt));
            jumpTableIndex = jit;
        }

        public int getJumpTableIndex()
        {
            return jumpTableIndex;
        }

        public void setJumpTableIndex(int jumpTableIndex)
        {
            this.jumpTableIndex = jumpTableIndex;
        }
    }

    public static class ConstantPoolSDNode extends SDNode
    {
        private Object val;
        private int offset;
        private int align;
        private int targetFlags;
        public ConstantPoolSDNode(boolean isTarget, Constant cnt, EVT vt, int off, int align, int targetFlags)
        {
            super(isTarget?ISD.TargetConstantPool:ISD.ConstantPool, getSDVTList(vt));
            val = cnt;
            offset = off;
            this.align = align;
            this.targetFlags = targetFlags;
        }

        public ConstantPoolSDNode(boolean isTarget, MachineConstantPoolValue machPoolVal, EVT vt, int off, int align, int targetFlags)
        {
            super(isTarget?ISD.TargetConstantPool:ISD.ConstantPool, getSDVTList(vt));
            val = machPoolVal;
            offset = off;
            this.align = align;
            this.targetFlags = targetFlags;
        }

        public boolean isMachineConstantPoolValue()
        {
            return val instanceof MachineConstantPoolValue;
        }

        public Constant getConstantValue()
        {
            assert !isMachineConstantPoolValue();
            return (Constant)val;
        }

        public MachineConstantPoolValue getMachineConstantPoolValue()
        {
            assert isMachineConstantPoolValue();
            return (MachineConstantPoolValue)val;
        }

        public int getOffset()
        {
            return offset;
        }

        public int getAlign()
        {
            return align;
        }

        public int getTargetFlags()
        {
            return targetFlags;
        }
    }

    public static class BasicBlockSDNode extends SDNode
    {
        private BasicBlock bb;
        public BasicBlockSDNode(BasicBlock bb)
        {
            super(ISD.BasicBlock, getSDVTList(new EVT(MVT.Other)));
            this.bb = bb;
        }

        public BasicBlock getBasicBlock()
        {
            return bb;
        }
    }

    public static class MemOperandSDNode extends SDNode
    {
        private MachineMemOperand mmo;
        public MemOperandSDNode(MachineMemOperand mmo)
        {
            super(ISD.MEMOPERAND, getSDVTList(new EVT(MVT.Other)));
            this.mmo = mmo;
        }

        public MachineMemOperand getMachineMemOperand()
        {
            return mmo;
        }
    }

    public static class RegisterSDNode extends SDNode
    {
        private int reg;
        public RegisterSDNode(EVT vt, int reg)
        {
            super(ISD.Register, getSDVTList(vt));
            this.reg = reg;
        }

        public int getReg()
        {
            return reg;
        }
    }

    public static class LabelSDNode extends SDNode
    {
        private int labelID;
        public LabelSDNode(int nodeTy, EVT vt, SDValue ch, int labelID)
        {
            super(nodeTy, getSDVTList(vt));
            initOperands(ch);
            this.labelID = labelID;
        }

        public int getLabelID()
        {
            return labelID;
        }
    }

    public static class ExternalSymbolSDNode extends SDNode
    {
        private String extSymol;
        private int targetFlags;
        public ExternalSymbolSDNode(boolean isTarget, EVT vt, String sym, int flags)
        {
            super(isTarget? ISD.TargetExternalSymbol:ISD.ExternalSymbol, getSDVTList(vt));
            this.extSymol = sym;
            this.targetFlags = flags;
        }

        public String getExtSymol()
        {
            return extSymol;
        }

        public int getTargetFlags()
        {
            return targetFlags;
        }
    }

    public static class CondCodeSDNode extends SDNode
    {
        private CondCode condition;
        public CondCodeSDNode(CondCode cc)
        {
            super(ISD.CONDCODE, getSDVTList(new EVT(MVT.Other)));
            condition = cc;
        }

        public CondCode getCondition()
        {
            return condition;
        }
    }

    public static class VTSDNode extends SDNode
    {
        private EVT vt;
        public VTSDNode(EVT vt)
        {
            super(ISD.VALUETYPE, getSDVTList(new EVT(MVT.Other)));
            this.vt = vt;
        }

        public EVT getVT()
        {
            return vt;
        }
    }

    public static class LSBaseSDNode extends MemSDNode
    {
        public LSBaseSDNode(int nodeTy, SDValue[] operands, SDVTList vts,
                            MemIndexedMode mode, EVT vt, Value srcVal,
                            int srcOff,
                            int alig,
                            boolean isVolatile)
        {
            super(nodeTy, vts, vt, srcVal ,srcOff, alig, isVolatile);
            assert alig != 0 :"Loads and Stores should have non-zero alignment!";
            sublassData |= mode.ordinal() << 2;
            assert getAddressingMode() == mode;
            initOperands(operands);
            assert getOffset().getOpcode() == ISD.UNDEF || isIndexed() :
                    "Only indexed loads and stores have a non-undef offset operand!";
        }

        public SDValue getOffset()
        {
            return getOperand(getOpcode() == ISD.LOAD ? 2 : 3);
        }

        public MemIndexedMode getAddressingMode()
        {
            return MemIndexedMode.values()[(sublassData >> 2)&7];
        }

        public boolean isIndexed()
        {
            return getAddressingMode() != MemIndexedMode.UNINDEXED;
        }

        public boolean isUnindexed()
        {
            return !isIndexed();
        }
    }

    public static class LoadSDNode extends LSBaseSDNode
    {
        public LoadSDNode(SDValue[] chainPtrOff, SDVTList vts, MemIndexedMode mode,
                          LoadExtType ety, EVT vt, Value sv, int offset, int align,
                          boolean isVolatile)
        {
            super(ISD.LOAD, chainPtrOff, vts, mode, vt, sv, offset, align, isVolatile);
            sublassData |= ety.ordinal();
            assert getExtensionType() == ety :"LoadExtType encoding error!";
        }

        public LoadExtType getExtensionType()
        {
            return LoadExtType.values()[sublassData & 3];
        }

        @Override
        public SDValue getBasePtr()
        {
            return getOperand(1);
        }

        @Override
        public SDValue getOffset()
        {
            return getOperand(2);
        }
    }

    public static class StoreSDNode extends LSBaseSDNode
    {
        public StoreSDNode(SDValue[] chainPtrOff, SDVTList vts, MemIndexedMode mode,
                           boolean isTrunc, EVT vt, Value sv, int offset, int align,
                           boolean isVolatile)
        {
            super(ISD.STORE, chainPtrOff, vts, mode, vt, sv, offset, align, isVolatile);
            sublassData |= isTrunc?1:0;
            assert isTruncatingStore() == isTrunc;
        }

        public boolean isTruncatingStore()
        {
            return (sublassData&1) != 0;
        }

        public SDValue getValue()
        {
            return getOperand(1);
        }

        @Override
        public SDValue getBasePtr()
        {
            return getOperand(2);
        }

        @Override
        public SDValue getOffset()
        {
            return getOperand(3);
        }
    }
}
