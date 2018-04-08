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

import backend.analysis.aa.AliasAnalysis;
import backend.codegen.EVT;
import backend.codegen.MVT;
import backend.codegen.MachineBasicBlock;
import backend.codegen.MachineInstr;
import backend.codegen.dagisel.SDNode.RegisterSDNode;
import backend.codegen.fastISel.ISD;
import backend.ir.AllocationInst;
import backend.ir.MallocInst;
import backend.ir.SelectInst;
import backend.support.BackendCmdOptions;
import backend.support.CallSite;
import backend.target.TargetData;
import backend.target.TargetLowering;
import backend.target.TargetMachine;
import backend.target.TargetRegisterInfo;
import backend.type.SequentialType;
import backend.type.StructType;
import backend.type.Type;
import backend.utils.InstVisitor;
import backend.value.*;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class SelectionDAGLowering implements InstVisitor<Void>
{
    MachineBasicBlock curMBB;
    HashMap<Value, SDValue> nodeMap;
    ArrayList<SDValue> pendingLoads;
    ArrayList<SDValue> pendingExports;

    static class Case implements Comparable<Case>
    {
        ConstantInt low;
        ConstantInt high;
        MachineBasicBlock mbb;
        Case()
        {}

        Case(ConstantInt lowVal, ConstantInt highVal, MachineBasicBlock mbb)
        {
            this.low = lowVal;
            this.high = highVal;
            this.mbb = mbb;
        }

        public long size()
        {
            return high.getSExtValue() - low.getSExtValue() + 1;
        }

        @Override
        public int compareTo(Case o)
        {
            return high.getValue().slt(o.low.getValue())?1:-1;
        }
    }

    static class CaseBits implements Comparable<CaseBits>
    {
        long mask;
        MachineBasicBlock mbb;
        int bits;
        CaseBits(long mask, MachineBasicBlock mbb, int bits)
        {
            this.mask = mask;
            this.mbb = mbb;
            this.bits = bits;
        }

        @Override
        public int compareTo(CaseBits o)
        {
            return Integer.compare(bits, o.bits);
        }
    }

    static class CaseRec
    {
        CaseRec(MachineBasicBlock mbb, ConstantInt lt, ConstantInt ge,
                ArrayList<Case> caseRanges)
        {
            this.mbb = mbb;
            this.low = lt;
            this.high = ge;
            this.caseRanges = caseRanges;
        }

        MachineBasicBlock mbb;
        ConstantInt low;
        ConstantInt high;
        ArrayList<Case> caseRanges;
    }

    public TargetLowering tli;
    public SelectionDAG dag;
    public TargetData td;
    public AliasAnalysis aa;

    public ArrayList<Pair<MachineInstr, Integer>> phiNodesToUpdate;

    TObjectIntHashMap<Constant> constantsOut;

    FunctionLoweringInfo funcInfo;

    TargetMachine.CodeGenOpt optLevel;
    boolean hasTailCall;

    public SelectionDAGLowering(SelectionDAG dag,
            TargetLowering tli, FunctionLoweringInfo funcInfo,
            TargetMachine.CodeGenOpt level)
    {
        this.dag = dag;
        this.tli = tli;
        this.funcInfo = funcInfo;
        this.optLevel = level;
    }

    public void init(AliasAnalysis aa)
    {
        this.aa = aa;
        td = dag.getTarget().getTargetData();
    }

    public void clear()
    {
        nodeMap.clear();
        pendingExports.clear();
        pendingLoads.clear();
        dag.clear();
        hasTailCall = false;
    }

    public SDValue getRoot()
    {
        if (pendingLoads.isEmpty())
            return dag.getRoot();

        if (pendingLoads.size() == 1)
        {
            SDValue root = pendingLoads.get(0);
            dag.setRoot(root);
            pendingLoads.clear();
            return root;
        }
        SDValue[] vals = new SDValue[pendingLoads.size()];
        pendingLoads.toArray(vals);
        SDValue root = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other),
                vals);
        pendingLoads.clear();
        dag.setRoot(root);
        return root;
    }

    public SDValue getControlRoot()
    {
        SDValue root = dag.getRoot();
        if (pendingExports.isEmpty())
            return root;

        if (root.getOpcode() != ISD.EntryToken)
        {
            int i = 0, e = pendingExports.size();
            while (i < e)
            {
                assert pendingExports.get(i).getNode().getNumOperands() > 1;
                if (pendingExports.get(i).getNode().getOperand(0).equals(root))
                    break;

                i++;
            }
            if (i == e)
                pendingExports.add(root);
        }

        SDValue[] vals = new SDValue[pendingExports.size()];
        pendingExports.toArray(vals);
        root = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other),
                vals);
        pendingExports.clear();
        dag.setRoot(root);
        return root;
    }

    public void copyValueToVirtualRegister(Value val, int reg)
    {
        SDValue op = getValue(val);
        assert op.getOpcode() != ISD.CopyFromReg ||
                ((RegisterSDNode)op.getOperand(1).getNode()).getReg() != reg
            :"Copy from a arg to the same reg";
        assert !TargetRegisterInfo.isPhysicalRegister(reg):"Is a phyisical reg?";

        // TODO: 18-4-8
    }

    public SDValue getValue(Value val)
    {
        // TODO: 18-4-8
        return null;
    }

    public void setValue(Value val, SDValue sdVal)
    {
        assert !nodeMap.containsKey(val):"Already set a value for this node!";
        nodeMap.put(val, sdVal);
    }

    public void lowerCallTo(CallSite cs, SDValue callee, boolean isTailCall)
    {}


    @Override
    public Void visitRet(Instruction.ReturnInst inst)
    {
        return null;
    }

    @Override
    public Void visitBr(Instruction.BranchInst inst)
    {
        return null;
    }

    @Override
    public Void visitSwitch(Instruction.SwitchInst inst)
    {
        return null;
    }

    private int getSDOpc(Operator opc)
    {
        switch (opc)
        {
            case Add: return ISD.ADD;
            case FAdd: return ISD.FADD;
            case Sub: return ISD.SUB;
            case FSub: return ISD.FSUB;
            case Mul: return ISD.MUL;
            case FMul: return ISD.FMUL;
            case URem: return ISD.UREM;
            case SRem: return ISD.SREM;
            case FRem: return ISD.FREM;
            case SDiv: return ISD.SDIV;
            case UDiv: return ISD.UDIV;
            case FDiv: return ISD.FDIV;
            case And: return ISD.AND;
            case Or: return ISD.OR;
            case Xor: return ISD.XOR;
            case Shl: return ISD.SHL;
            case AShr: return ISD.SRL;
            case LShr: return ISD.SRA;
            default:
                assert false:"Unknown binary operator!";
                return -1;
        }
    }

    @Override
    public Void visitBinaryOp(Instruction.BinaryOps inst)
    {
        SDValue op1 = getValue(inst.operand(0));
        SDValue op2 = getValue(inst.operand(1));
        int opc = getSDOpc(inst.getOpcode());
        assert opc >= 0;
        if (inst.getOpcode().isShift())
        {
            if (!op2.getValueType().equals(tli.getShiftAmountTy()))
            {
                EVT pty = new EVT(tli.getPointerTy());
                EVT sty = new EVT(tli.getShiftAmountTy());
                if (sty.bitsGT(op2.getValueType()))
                    op2 = dag.getNode(ISD.ANY_EXTEND, sty, op2);
                else if (sty.getSizeInBits() >= Util.log2Ceil(op2.getValueType().getSizeInBits()))
                {
                    op2 = dag.getNode(ISD.TRUNCATE, sty, op2);
                }
                else if (pty.bitsLT(op2.getValueType()))
                    op2 = dag.getNode(ISD.TRUNCATE, pty, op2);
                else if (pty.bitsGT(op2.getValueType()))
                    op2 = dag.getNode(ISD.ANY_EXTEND, pty, op2);
            }
            setValue(inst, dag.getNode(opc, op1.getValueType(), op1, op2));
        }
        else
        {
            setValue(inst, dag.getNode(getSDOpc(inst.getOpcode()),
                    op1.getValueType(), op1, op2));
        }
        return null;
    }

    private CondCode getICmpCondCode(Instruction.CmpInst.Predicate pred)
    {
        switch (pred)
        {
            case ICMP_EQ: return CondCode.SETEQ;
            case ICMP_NE: return CondCode.SETNE;
            case ICMP_SLE: return CondCode.SETLE;
            case ICMP_ULE: return CondCode.SETULE;
            case ICMP_SGE: return CondCode.SETGE;
            case ICMP_UGE: return CondCode.SETUGE;
            case ICMP_SLT: return CondCode.SETLT;
            case ICMP_ULT: return CondCode.SETULT;
            case ICMP_SGT: return CondCode.SETGT;
            case ICMP_UGT: return CondCode.SETUGT;
            default:
                Util.shouldNotReachHere("Invalid ICmp predicate opcode!");;
                return CondCode.SETNE;
        }
    }

    private CondCode getFCmpCondCode(Instruction.FCmpInst.Predicate pred)
    {
        CondCode fpc, foc;
        switch (pred)
        {
            case FCMP_FALSE:
                fpc = foc = CondCode.SETFALSE;
                break;
            case FCMP_OEQ:
                foc = CondCode.SETEQ;
                fpc = CondCode.SETOEQ;
                break;
            case FCMP_OGT:
                foc = CondCode.SETGT;
                fpc = CondCode.SETOGT;
                break;
            case FCMP_OGE:
                foc = CondCode.SETGE;
                fpc = CondCode.SETOGE;
                break;
            case FCMP_OLT:
                foc = CondCode.SETLT;
                fpc = CondCode.SETOLT;
                break;
            case FCMP_OLE:
                foc = CondCode.SETLE;
                fpc = CondCode.SETOLE;
                break;
            case FCMP_ONE:
                foc = CondCode.SETNE;
                fpc = CondCode.SETONE;
                break;
            case FCMP_ORD:
                foc = fpc = CondCode.SETO;
                break;
            case FCMP_UNO:
                foc = fpc = CondCode.SETUO;
                break;
            case FCMP_UEQ:
                foc = CondCode.SETEQ;
                fpc = CondCode.SETUEQ;
                break;
            case FCMP_UGT:
                foc = CondCode.SETGT;
                fpc = CondCode.SETUGT;
                break;
            case FCMP_UGE:
                foc = CondCode.SETGE;
                fpc = CondCode.SETUGE;
                break;
            case FCMP_ULT:
                foc = CondCode.SETLT;
                fpc = CondCode.SETULT;
                break;
            case FCMP_ULE:
                foc = CondCode.SETLE;
                fpc = CondCode.SETULE;
                break;
            case FCMP_UNE:
                foc = CondCode.SETNE;
                fpc = CondCode.SETUNE;
                break;
            case FCMP_TRUE:
                foc = fpc = CondCode.SETTRUE;
                break;
            default:
                Util.shouldNotReachHere("Invalid predicate for FCmp instruction!");
                foc = fpc = CondCode.SETFALSE;
                break;
        }
        return BackendCmdOptions.finiteOnlyFPMath()? foc : fpc;
    }

    @Override
    public Void visitICmp(Instruction.ICmpInst inst)
    {
        Instruction.CmpInst.Predicate pred = inst.getPredicate();
        SDValue op1 = getValue(inst.operand(0));
        SDValue op2 = getValue(inst.operand(1));
        CondCode opc = getICmpCondCode(pred);
        EVT destVT = tli.getValueType(inst.getType());
        setValue(inst, dag.getSetCC(destVT, op1, op2, opc));
        return null;
    }

    @Override
    public Void visitFCmp(Instruction.FCmpInst inst)
    {
        Instruction.CmpInst.Predicate pred = inst.getPredicate();
        SDValue op1 = getValue(inst.operand(0));
        SDValue op2 = getValue(inst.operand(1));
        CondCode opc = getFCmpCondCode(pred);
        EVT destVT = tli.getValueType(inst.getType());
        setValue(inst, dag.getSetCC(destVT, op1, op2, opc));
        return null;
    }

    @Override
    public Void visitTrunc(Instruction.CastInst inst)
    {
        EVT destVT = tli.getValueType(inst.getType());
        SDValue op1 = getValue(inst.operand(0));
        setValue(inst, dag.getNode(ISD.TRUNCATE, destVT, op1));
        return null;
    }

    @Override
    public Void visitZExt(Instruction.CastInst inst)
    {
        EVT destVT = tli.getValueType(inst.getType());
        SDValue op1 = getValue(inst.operand(0));
        setValue(inst, dag.getNode(ISD.ZERO_EXTEND, destVT, op1));
        return null;
    }

    @Override
    public Void visitSExt(Instruction.CastInst inst)
    {
        EVT destVT = tli.getValueType(inst.getType());
        SDValue op1 = getValue(inst.operand(0));
        setValue(inst, dag.getNode(ISD.SIGN_EXTEND, destVT, op1));
        return null;
    }

    @Override
    public Void visitFPToUI(Instruction.CastInst inst)
    {
        EVT destVT = tli.getValueType(inst.getType());
        SDValue op1 = getValue(inst.operand(0));
        setValue(inst, dag.getNode(ISD.FP_TO_UINT, destVT, op1));
        return null;
    }

    @Override
    public Void visitFPToSI(Instruction.CastInst inst)
    {
        EVT destVT = tli.getValueType(inst.getType());
        SDValue op1 = getValue(inst.operand(0));
        setValue(inst, dag.getNode(ISD.FP_TO_SINT, destVT, op1));
        return null;
    }

    @Override
    public Void visitUIToFP(Instruction.CastInst inst)
    {
        EVT destVT = tli.getValueType(inst.getType());
        SDValue op1 = getValue(inst.operand(0));
        setValue(inst, dag.getNode(ISD.UINT_TO_FP, destVT, op1));
        return null;
    }

    @Override
    public Void visitSIToFP(Instruction.CastInst inst)
    {
        EVT destVT = tli.getValueType(inst.getType());
        SDValue op1 = getValue(inst.operand(0));
        setValue(inst, dag.getNode(ISD.SINT_TO_FP, destVT, op1));
        return null;
    }

    @Override
    public Void visitFPTrunc(Instruction.CastInst inst)
    {
        EVT destVT = tli.getValueType(inst.getType());
        SDValue op1 = getValue(inst.operand(0));
        setValue(inst, dag.getNode(ISD.FTRUNC, destVT, op1));
        return null;
    }

    @Override
    public Void visistFPExt(Instruction.CastInst inst)
    {
        EVT destVT = tli.getValueType(inst.getType());
        SDValue op1 = getValue(inst.operand(0));
        setValue(inst, dag.getNode(ISD.FP_EXTEND, destVT, op1));
        return null;
    }

    @Override
    public Void visitPtrToInt(Instruction.CastInst inst)
    {
        EVT destVT = tli.getValueType(inst.getType());
        EVT srcVT = tli.getValueType(inst.operand(0).getType());
        int opc;
        if (destVT.getSizeInBits() < srcVT.getSizeInBits())
            opc = ISD.TRUNCATE;
        else
            opc = ISD.ZERO_EXTEND;
        SDValue op1 = getValue(inst.operand(0));
        setValue(inst, dag.getNode(opc, destVT, op1));
        return null;
    }

    @Override
    public Void visitIntToPtr(Instruction.CastInst inst)
    {
        EVT destVT = tli.getValueType(inst.getType());
        EVT srcVT = tli.getValueType(inst.operand(0).getType());
        int opc;
        if (destVT.getSizeInBits() < srcVT.getSizeInBits())
            opc = ISD.TRUNCATE;
        else
            opc = ISD.ZERO_EXTEND;
        SDValue op1 = getValue(inst.operand(0));
        setValue(inst, dag.getNode(opc, destVT, op1));
        return null;
    }

    @Override
    public Void visitBitCast(Instruction.CastInst inst)
    {
        EVT destVT = tli.getValueType(inst.getType());
        SDValue op1 = getValue(inst.operand(0));
        EVT srcVT = tli.getValueType(inst.operand(0).getType());
        if (!destVT.equals(srcVT))
            setValue(inst, dag.getNode(ISD.BIT_CONVERT, destVT, op1));
        else
            setValue(inst, op1);
        return null;
    }

    @Override
    public Void visitCastInst(Instruction.CastInst inst)
    {
        // TODO: 18-3-20
        assert false:"TODO";
        return null;
    }

    @Override
    public Void visitAlloca(Instruction.AllocaInst inst)
    {
        return null;
    }

    @Override
    public Void visitMalloc(MallocInst inst)
    {
        return null;
    }

    @Override
    public Void visitAllocationInst(AllocationInst inst)
    {
        return null;
    }

    @Override
    public Void visitLoad(Instruction.LoadInst inst)
    {
        return null;
    }

    @Override
    public Void visitStore(Instruction.StoreInst inst)
    {
        return null;
    }

    @Override
    public Void visitCall(Instruction.CallInst inst)
    {
        return null;
    }

    @Override
    public Void visitGetElementPtr(Instruction.GetElementPtrInst inst)
    {
        SDValue node = getValue(inst.operand(0));
        Type ty = inst.operand(0).getType();
        for (int i = 1, e = inst.getNumOfOperands(); i < e; i++)
        {
            Value idx = inst.operand(i);
            if (ty instanceof StructType)
            {
                long field = ((ConstantInt)idx).getZExtValue();
                StructType sty = (StructType)ty;
                if (field != 0)
                {
                    // N = N + offset;
                    long offset = td.getStructLayout(sty).getElementOffset(field);
                    node = dag.getNode(ISD.ADD, node.getValueType(), node,
                            dag.getIntPtrConstant(offset));
                }
                ty = sty.getElementType((int) field);
            }
            else
            {
                ty = ((SequentialType)ty).getElementType();
                if (idx instanceof ConstantInt)
                {
                    ConstantInt ci = (ConstantInt)idx;
                    long val = ci.getZExtValue();
                    if (val == 0)
                        continue;
                    long offset = td.getTypeAllocSize(ty) * ci.getSExtValue();
                    SDValue offsVal;
                    EVT pty = new EVT(tli.getPointerTy());
                    int ptrBits = pty.getSizeInBits();
                    if (ptrBits < 64)
                    {
                        offsVal = dag.getNode(ISD.TRUNCATE, pty, dag.getConstant(offset,
                                new EVT(MVT.i64), false));
                    }
                    else
                    {
                        offsVal = dag.getIntPtrConstant(offset);
                    }
                    node = dag.getNode(ISD.ADD, node.getValueType(), node, offsVal);
                    continue;
                }

                // node = node + idx * eltSize;
                long eltSize = td.getTypeAllocSize(ty);
                SDValue idxN = getValue(idx);

                if (idxN.getValueType().bitsLT(node.getValueType()))
                {
                    idxN = dag.getNode(ISD.SIGN_EXTEND, node.getValueType(), idxN);
                }
                else if (idxN.getValueType().bitsGT(node.getValueType()))
                {
                    idxN = dag.getNode(ISD.TRUNCATE, node.getValueType(), idxN);
                }

                if (eltSize != 1)
                {
                    if (Util.isPowerOf2(eltSize))
                    {
                        int amt = Util.log2(eltSize);
                        idxN = dag.getNode(ISD.SHL, node.getValueType(),
                                idxN, dag.getConstant(amt, new EVT(tli.getPointerTy()), false));
                    }
                    else
                    {
                        SDValue scale = dag.getIntPtrConstant(eltSize);
                        idxN = dag.getNode(ISD.MUL, node.getValueType(), idxN, scale);
                    }
                }
                node = dag.getNode(ISD.ADD, node.getValueType(), node, idxN);
            }
        }
        setValue(inst, node);
        return null;
    }

    @Override
    public Void visitPhiNode(Instruction.PhiNode inst)
    {
        return null;
    }

    @Override
    public Void visitSelect(SelectInst inst)
    {
        return null;
    }
}
