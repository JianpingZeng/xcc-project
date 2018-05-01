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
import backend.codegen.*;
import backend.codegen.dagisel.SDNode.RegisterSDNode;
import backend.codegen.fastISel.ISD;
import backend.support.Attribute;
import backend.support.BackendCmdOptions;
import backend.support.CallSite;
import backend.support.CallingConv;
import backend.target.TargetData;
import backend.target.TargetLowering;
import backend.target.TargetMachine;
import backend.target.TargetRegisterInfo;
import backend.type.*;
import backend.utils.InstVisitor;
import backend.value.*;
import backend.value.Instruction.*;
import backend.value.Instruction.CmpInst.Predicate;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.OutParamWrapper;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;

import static backend.codegen.dagisel.FunctionLoweringInfo.computeValueVTs;
import static backend.codegen.dagisel.RegsForValue.getCopyToParts;
import static backend.target.TargetOptions.EnablePerformTailCallOpt;

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

    public boolean hasTailCall()
    {
        return hasTailCall;
    }

    public void copyToExpendRegsIfNeeds(Value val)
    {
        if (!val.isUseEmpty())
        {
            if (funcInfo.valueMap.containsKey(val))
            {
                int reg = funcInfo.valueMap.get(val);
                copyValueToVirtualRegister(val, reg);
            }
        }
    }

    public void setCurrentBasicBlock(MachineBasicBlock mbb)
    {
        curMBB = mbb;
    }

    public MachineBasicBlock getCurrentBasicBlock()
    {
        return curMBB;
    }

    static class Case implements Comparable<Case>
    {
        ConstantInt low;
        ConstantInt high;
        MachineBasicBlock mbb;

        Case()
        {
        }

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

        @Override public int compareTo(Case o)
        {
            return high.getValue().slt(o.low.getValue()) ? 1 : -1;
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

        @Override public int compareTo(CaseBits o)
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

    public SelectionDAGLowering(SelectionDAG dag, TargetLowering tli,
            FunctionLoweringInfo funcInfo, TargetMachine.CodeGenOpt level)
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
        SDValue root = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), vals);
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
        root = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), vals);
        pendingExports.clear();
        dag.setRoot(root);
        return root;
    }

    public void copyValueToVirtualRegister(Value val, int reg)
    {
        SDValue op = getValue(val);
        assert op.getOpcode() != ISD.CopyFromReg
                || ((RegisterSDNode) op.getOperand(1).getNode()).getReg()
                != reg : "Copy from a arg to the same reg";
        assert !TargetRegisterInfo
                .isPhysicalRegister(reg) : "Is a physical reg?";

        RegsForValue rfv = new RegsForValue(tli, reg, val.getType());
        SDValue chain = dag.getEntryNode();
        OutParamWrapper<SDValue> x = new OutParamWrapper<>(chain);
        rfv.getCopyToRegs(op, dag, x, null);
        chain = x.get();
        pendingExports.add(chain);
    }

    public SDValue getValue(Value val)
    {
        if (nodeMap.containsKey(val))
            return nodeMap.get(val);

        if (val instanceof Constant)
        {
            Constant cnt = (Constant) val;
            EVT vt = tli.getValueType(cnt.getType(), true);
            if (cnt instanceof ConstantInt)
            {
                SDValue n = dag.getConstant((ConstantInt) cnt, vt, false);
                nodeMap.put(val, n);
                return n;
            }
            if (cnt instanceof GlobalValue)
            {
                SDValue n = dag
                        .getGlobalAddress((GlobalValue) cnt, vt, 0, false, 0);
                nodeMap.put(val, n);
                return n;
            }
            if (cnt instanceof ConstantPointerNull)
            {
                SDValue n = dag.getConstant(0, vt, false);
                nodeMap.put(val, n);
                return n;
            }
            if (cnt instanceof ConstantFP)
            {
                SDValue n = dag.getConstantFP((ConstantFP) cnt, vt, false);
                nodeMap.put(val, n);
                return n;
            }
            if (cnt instanceof Value.UndefValue)
            {
                SDValue n = dag.getUNDEF(vt);
                nodeMap.put(val, n);
                return n;
            }
            if (cnt instanceof ConstantExpr)
            {
                ConstantExpr ce = (ConstantExpr) cnt;
                visit(ce.getOpcode(), ce);
                SDValue n1 = nodeMap.get(val);
                assert n1.getNode() != null;
                return n1;
            }

            if (cnt instanceof ConstantStruct || cnt instanceof ConstantArray)
            {
                ArrayList<SDValue> constants = new ArrayList<>();
                for (int i = 0, e = cnt.getNumOfOperands(); i < e; i++)
                {
                    SDNode elt = getValue(cnt.operand(i)).getNode();
                    for (int j = 0, ee = elt.getNumValues(); j < ee; j++)
                        constants.add(new SDValue(elt, j));
                }
                return dag.getMergeValues(constants);
            }

            if (cnt.getType() instanceof StructType || cnt.getType() instanceof ArrayType)
            {
                assert cnt instanceof ConstantAggregateZero
                        || cnt instanceof Value.UndefValue : "Unknown struct or array constant!";

                ArrayList<EVT> valueVTs = new ArrayList<>();
                computeValueVTs(tli, cnt.getType(), valueVTs);
                int numElts = valueVTs.size();
                if (numElts == 0)
                    return new SDValue();

                ArrayList<SDValue> constants = new ArrayList<>();
                for (int i = 0; i < numElts; i++)
                {
                    EVT eltVT = valueVTs.get(i);
                    if (cnt instanceof Value.UndefValue)
                        constants.add(dag.getUNDEF(eltVT));
                    else if (eltVT.isFloatingPoint())
                        constants.add(dag.getConstantFP(0, eltVT, false));
                    else
                        constants.add(dag.getConstant(0, eltVT, false));
                }
                return dag.getMergeValues(constants);
            }

            Util.shouldNotReachHere("Vector type not supported!");
            return null;
        }
        if (val instanceof AllocaInst)
        {
            if (funcInfo.staticAllocaMap.containsKey(val))
                return dag.getFrameIndex(funcInfo.staticAllocaMap.get(val),
                        new EVT(tli.getPointerTy()), false);
        }
        int inReg = funcInfo.valueMap.get(val);
        assert inReg != 0 : "Value not in map!";
        RegsForValue rfv = new RegsForValue(tli, inReg, val.getType());
        SDValue chain = dag.getEntryNode();
        OutParamWrapper<SDValue> x = new OutParamWrapper<>(chain);
        SDValue res = rfv.getCopyFromRegs(dag, x, null);
        chain = x.get();
        return res;
    }

    public void setValue(Value val, SDValue sdVal)
    {
        assert !nodeMap.containsKey(val) : "Already set a value for this node!";
        nodeMap.put(val, sdVal);
    }

    public void lowerCallTo(CallSite cs, SDValue callee, boolean isTailCall)
    {
        lowerCallTo(cs, callee, isTailCall, null);
    }

    public void lowerCallTo(CallSite cs, SDValue callee, boolean isTailCall, MachineBasicBlock landingPad)
    {
        PointerType pt = (PointerType)(cs.getCalledValue().getType());
        FunctionType fty = (FunctionType) pt.getElementType();
        MachineModuleInfo mmi = dag.getMachineModuleInfo();
        int beginLabel = 0, endLabel = 0;

        ArrayList<ArgListEntry> args = new ArrayList<>(cs.getNumOfArguments());
        ArgListEntry entry = new ArgListEntry();

        for (int i = 0, e = cs.getNumOfArguments(); i < e; i++)
        {
            SDValue arg = getValue(cs.getArgument(i));
            entry.node = arg;
            entry.ty = cs.getArgument(i).getType();

            int attrInd = i + 1;
            entry.isSExt = cs.paramHasAttr(attrInd, Attribute.SExt);
            entry.isZExt = cs.paramHasAttr(attrInd, Attribute.ZExt);
            entry.isInReg = cs.paramHasAttr(attrInd, Attribute.InReg);
            entry.isSRet = cs.paramHasAttr(attrInd, Attribute.StructRet);
            entry.isNest = cs.paramHasAttr(attrInd, Attribute.Nest);
            entry.isByVal = cs.paramHasAttr(attrInd, Attribute.ByVal);
            entry.alignment = cs.paramAlignment(attrInd);
            args.add(entry);
        }

        if (landingPad != null && mmi != null)
        {
            beginLabel = mmi.nextLabelID();
            getRoot();
            dag.setRoot(dag.getLabel(ISD.EH_LABEL, getControlRoot(), beginLabel));
        }

        if (isTailCall && !isInTailCallPosition(cs.getInstruction(),
                cs.getAttributes().getRetAttribute(), tli))
            isTailCall = false;

        Pair<SDValue, SDValue> result = tli.lowerCallTo(getRoot(),
                cs.getType(), cs.paramHasAttr(0, Attribute.SExt),
                cs.paramHasAttr(0, Attribute.ZExt),
                fty.isVarArg(),
                cs.paramHasAttr(0, Attribute.InReg),
                fty.getNumParams(),
                cs.getCallingConv(),
                isTailCall,
                !cs.getInstruction().isUseEmpty(),
                callee, args, dag);
        assert !isTailCall || result.second.getNode() != null;
        assert result.second.getNode() != null || result.first.getNode() == null;
        if (result.first.getNode() != null)
            setValue(cs.getInstruction(), result.first);

        if (result.second.getNode() != null)
            dag.setRoot(result.second);
        else
            hasTailCall = true;

        if (landingPad != null && mmi != null)
        {
            endLabel = mmi.getNextLabelID();
            dag.setRoot(dag.getLabel(ISD.EH_LABEL, getRoot(), endLabel));
            mmi.addInvoke(landingPad, beginLabel, endLabel);
        }
    }

    static boolean isInTailCallPosition(Instruction inst, int retAttr,
            TargetLowering tli)
    {
        BasicBlock exitBB = inst.getParent();
        TerminatorInst ti = exitBB.getTerminator();
        ReturnInst ret = ti instanceof ReturnInst ? (ReturnInst) ti:null;
        Function f = exitBB.getParent();

        if (ret == null && !(ti instanceof UnreachableInst)) return false;

        if (inst.mayHasSideEffects() || inst.mayReadMemory() ||
                !inst.isSafeToSpecutativelyExecute())
        {
            for (int i = exitBB.size()-2; ; --i)
            {
                if (exitBB.getInstAt(i).equals(inst))
                    break;
                if (exitBB.getInstAt(i).mayHasSideEffects() || exitBB.getInstAt(i).mayReadMemory()
                        || !exitBB.getInstAt(i).isSafeToSpecutativelyExecute())
                    return false;
            }
        }

        if (ret == null || ret.getNumOfOperands() == 0 ) return true;

        if (f.getAttributes().getRetAttribute() != retAttr)
            return false;

        Instruction u = ret.operand(0) instanceof Instruction ?
                (Instruction)ret.operand(0):null;
        while (true)
        {
            if (u == null) return false;

            if (!u.hasOneUses())
                return false;

            if (u.equals(inst))
                break;
            if (u instanceof TruncInst && tli.isTruncateFree(u.operand(0).getType(),
                    u.getType()))
            {
                u = u.operand(0) instanceof Instruction ?
                        (Instruction)u.operand(0):null;
                continue;
            }
            return false;
        }
        return true;
    }

    public void visit(Operator opc, User u)
    {
        switch (opc)
        {
            case Ret:
                visitRet(u);
                break;
            case Br:
                visitBr(u);
                break;
            case Switch:
                visitSwitch(u);
                break;
            case Unreachable:
                // binary operator
                Util.shouldNotReachHere();
                break;
            // add
            case Add:
                visitAdd(u);
                break;
            case FAdd:
                visitFAdd(u);
                break;
            // subtractive
            case Sub:
                visitSub(u);
                break;
            case FSub:
                visitFSub(u);
                break;
            // multiple
            case Mul:
                visitMul(u);
                break;
            case FMul:
                visitFMul(u);
                break;
            // division
            case UDiv:
                visitUDiv(u);
                break;
            case SDiv:
                visitSDiv(u);
                break;
            case FDiv:
                visitFDiv(u);
                break;
            // mod operation
            case URem:
                visitURem(u);
                break;
            case SRem:
                visitSRem(u);
                break;
            case FRem:
                visitFRem(u);
                break;
            // bit-operation
            case And:
                visitAnd(u);
                break;
            case Or:
                visitOr(u);
                break;
            case Xor:
                visitXor(u);
                break;
            // comparison operation
            case ICmp:
                visitICmp(u);
                break;
            case FCmp:
                visitFCmp(u);
                break;
            // shift operation
            case Shl:
                visitShl(u);
                break;
            case LShr:
                visitLShr(u);
                break;
            case AShr:
                visitAShr(u);
                break;
            // converts operation
            //truncate integers.
            case Trunc:
                visitTrunc(u);
                break;
            // zero extend integers.
            case ZExt:
                visitZExt(u);
                break;
            // Sign extend integers.
            case SExt:
                visitSExt(u);
                break;
            // floatint-pint to unsigned integer.
            case FPToUI:
                visitFPToUI(u);
                break;
            // floating point to signed integer.
            case FPToSI:
                visitFPToSI(u);
                break;
            // unsigned integer to floating-point.
            case UIToFP:
                visitUIToFP(u);
                break;
            // signed integer to floating-point.
            case SIToFP:
                visitSIToFP(u);
                break;
            // floating point truncate.
            case FPTrunc:
                visitFPTrunc(u);
                break;
            // float point extend.
            case FPExt:
                visitFPExt(u);
                break;
            // pointer to integer.
            case PtrToInt:
                visitPtrToInt(u);
                break;
            // Integer to pointer.
            case IntToPtr:
                visitIntToPtr(u);
                break;
            // type cast.
            case BitCast:
                visitBitCast(u);
                break;
            // memory operation
            case Alloca:
                visitAlloca(u);
                break;
            case Free:
                visitFree(u);
                break;
            case Malloc:
                visitMalloc(u);
                break;
            case Store:
                visitStore(u);
                break;
            case Load:
                visitLoad(u);
                break;
            // other operation
            case Phi:
                visitPhiNode(u);
                break;
            case Call:
                visitCall(u);
                break;
            case GetElementPtr:
                visitGetElementPtr(u);
                break;
            // Select instruction acts as ?: operator in C language.
            case Select:
                visitSelect(u);
                break;
            default:
                Util.shouldNotReachHere("Unknown operator!");
        }
    }

    @Override
    public Void visitRet(User inst)
    {
        SDValue chain = getControlRoot();
        ReturnInst ret = (ReturnInst)inst;
        ArrayList<OutputArg> outs = new ArrayList<>(8);
        for (int i = 0, e = inst.getNumOfOperands(); i < e; i++)
        {
            ArrayList<EVT> valueVTs = new ArrayList<>();
            computeValueVTs(tli, ret.operand(i).getType(), valueVTs);
            int numValues = valueVTs.size();
            if (numValues <= 0) continue;
            SDValue retOp = getValue(ret.operand(i));
            for (int j = 0; j < numValues; j++)
            {
                EVT vt = valueVTs.get(j);

                int extendKind = ISD.ANY_EXTEND;
                Function f = ret.getParent().getParent();
                if (f.paramHasAttr(0, Attribute.SExt))
                    extendKind = ISD.SIGN_EXTEND;
                else if (f.paramHasAttr(0, Attribute.ZExt))
                    extendKind = ISD.ZERO_EXTEND;

                if (extendKind != ISD.ANY_EXTEND && vt.isInteger())
                {
                    EVT minVT = tli.getRegisterType(new EVT(MVT.i32));
                    if (vt.bitsLT(minVT))
                        vt = minVT;
                }

                int numParts = tli.getNumRegisters(vt);
                EVT partVT = tli.getRegisterType(vt);
                SDValue[] parts = new SDValue[numParts];
                getCopyToParts(dag, new SDValue(retOp.getNode(), retOp.getResNo()+j),
                        parts, partVT, extendKind);

                ArgFlagsTy flags = new ArgFlagsTy();
                if (f.paramHasAttr(0, Attribute.InReg))
                    flags.setInReg();

                if (f.paramHasAttr(0, Attribute.SExt))
                    flags.setSExt();
                else if (f.paramHasAttr(0, Attribute.ZExt))
                    flags.setZExt();
                for (int k = 0; k < numParts; k++)
                    outs.add(new OutputArg(flags, parts[i], true));
            }
        }

        boolean isVarArg = dag.getMachineFunction().getFunction().isVarArg();
        CallingConv cc = dag.getMachineFunction().getFunction().getCallingConv();
        chain = tli.lowerReturn(chain, cc, isVarArg, outs, dag);
        assert chain.getNode() != null && chain.getValueType().getSimpleVT().simpleVT == MVT.Other;
        dag.setRoot(chain);
        return null;
    }

    @Override
    public Void visitBr(User inst)
    {
        // TODO: 18-5-1
        Util.shouldNotReachHere("Not implemented currently!");
        return null;
    }

    @Override
    public Void visitSwitch(User inst)
    {
        // TODO: 18-5-1
        Util.shouldNotReachHere("Not implemented currently!");
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
    public Void visitBinaryOp(User inst)
    {
        Operator op = inst instanceof Instruction ?
                ((Instruction)inst).getOpcode() : ((ConstantExpr)inst).getOpcode();
        SDValue op1 = getValue(inst.operand(0));
        SDValue op2 = getValue(inst.operand(1));
        int opc = getSDOpc(op);
        assert opc >= 0;
        if (op.isShift())
        {
            if (!op2.getValueType().getSimpleVT().equals(tli.getShiftAmountTy()))
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
            setValue(inst, dag.getNode(opc, op1.getValueType(), op1, op2));
        }
        return null;
    }

    private CondCode getICmpCondCode(Predicate pred)
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

    private CondCode getFCmpCondCode(Predicate pred)
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
    public Void visitICmp(User inst)
    {
        Predicate pred = inst instanceof CmpInst ?((CmpInst)inst).getPredicate() :
                ((ConstantExpr)inst).getPredicate();
        SDValue op1 = getValue(inst.operand(0));
        SDValue op2 = getValue(inst.operand(1));
        CondCode opc = getICmpCondCode(pred);
        EVT destVT = tli.getValueType(inst.getType());
        setValue(inst, dag.getSetCC(destVT, op1, op2, opc));
        return null;
    }

    @Override
    public Void visitFCmp(User inst)
    {
        Predicate pred = inst instanceof CmpInst ?
                ((CmpInst)inst).getPredicate() :
                ((ConstantExpr)inst).getPredicate();
        SDValue op1 = getValue(inst.operand(0));
        SDValue op2 = getValue(inst.operand(1));
        CondCode opc = getFCmpCondCode(pred);
        EVT destVT = tli.getValueType(inst.getType());
        setValue(inst, dag.getSetCC(destVT, op1, op2, opc));
        return null;
    }

    @Override
    public Void visitTrunc(User inst)
    {
        EVT destVT = tli.getValueType(inst.getType());
        SDValue op1 = getValue(inst.operand(0));
        setValue(inst, dag.getNode(ISD.TRUNCATE, destVT, op1));
        return null;
    }

    @Override
    public Void visitZExt(User inst)
    {
        EVT destVT = tli.getValueType(inst.getType());
        SDValue op1 = getValue(inst.operand(0));
        setValue(inst, dag.getNode(ISD.ZERO_EXTEND, destVT, op1));
        return null;
    }

    @Override
    public Void visitSExt(User inst)
    {
        EVT destVT = tli.getValueType(inst.getType());
        SDValue op1 = getValue(inst.operand(0));
        setValue(inst, dag.getNode(ISD.SIGN_EXTEND, destVT, op1));
        return null;
    }

    @Override
    public Void visitFPToUI(User inst)
    {
        EVT destVT = tli.getValueType(inst.getType());
        SDValue op1 = getValue(inst.operand(0));
        setValue(inst, dag.getNode(ISD.FP_TO_UINT, destVT, op1));
        return null;
    }

    @Override
    public Void visitFPToSI(User inst)
    {
        EVT destVT = tli.getValueType(inst.getType());
        SDValue op1 = getValue(inst.operand(0));
        setValue(inst, dag.getNode(ISD.FP_TO_SINT, destVT, op1));
        return null;
    }

    @Override
    public Void visitUIToFP(User inst)
    {
        EVT destVT = tli.getValueType(inst.getType());
        SDValue op1 = getValue(inst.operand(0));
        setValue(inst, dag.getNode(ISD.UINT_TO_FP, destVT, op1));
        return null;
    }

    @Override
    public Void visitSIToFP(User inst)
    {
        EVT destVT = tli.getValueType(inst.getType());
        SDValue op1 = getValue(inst.operand(0));
        setValue(inst, dag.getNode(ISD.SINT_TO_FP, destVT, op1));
        return null;
    }

    @Override
    public Void visitFPTrunc(User inst)
    {
        EVT destVT = tli.getValueType(inst.getType());
        SDValue op1 = getValue(inst.operand(0));
        setValue(inst, dag.getNode(ISD.FTRUNC, destVT, op1));
        return null;
    }

    @Override
    public Void visitFPExt(User inst)
    {
        EVT destVT = tli.getValueType(inst.getType());
        SDValue op1 = getValue(inst.operand(0));
        setValue(inst, dag.getNode(ISD.FP_EXTEND, destVT, op1));
        return null;
    }

    @Override
    public Void visitPtrToInt(User inst)
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
    public Void visitIntToPtr(User inst)
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
    public Void visitBitCast(User inst)
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
    public Void visitAlloca(User inst)
    {
        AllocaInst ai = (AllocaInst)inst;

        if (funcInfo.staticAllocaMap.containsKey(ai))
            return null;
        Type ty = ai.getAllocatedType();
        long size = tli.getTargetData().getTypeAllocSize(ty);
        int align = Math.max(tli.getTargetData().getPrefTypeAlignment(ty), ai.getAlignment());
        SDValue allocaSize = getValue(ai.getArraySize());
        allocaSize = dag.getNode(ISD.MUL, allocaSize.getValueType(), allocaSize,
                dag.getConstant(size, allocaSize.getValueType(), false));

        EVT intPtr = new EVT(tli.getPointerTy());
        if (intPtr.bitsLT(allocaSize.getValueType()))
            allocaSize = dag.getNode(ISD.TRUNCATE, intPtr, allocaSize);
        else if (intPtr.bitsGT(allocaSize.getValueType()))
            allocaSize = dag.getNode(ISD.ZERO_EXTEND, intPtr, allocaSize);

        int stackAlign = tli.getTargetMachine().getFrameInfo().getStackAlignment();
        if (align <= stackAlign)
            align = 0;

        allocaSize = dag.getNode(ISD.ADD, allocaSize.getValueType(), allocaSize,
                dag.getIntPtrConstant(stackAlign-1));
        allocaSize = dag.getNode(ISD.AND, allocaSize.getValueType(), allocaSize,
                dag.getIntPtrConstant(~(stackAlign-1)));
        SDValue[] ops = {getRoot(), allocaSize, dag.getIntPtrConstant(align)};
        SDNode.SDVTList vts = dag.getVTList(allocaSize.getValueType(), new EVT(MVT.Other));
        SDValue dsa = dag.getNode(ISD.DYNAMIC_STACKALLOC, vts, ops);
        setValue(inst, dsa);
        dag.setRoot(dsa.getValue(1));

        // Inform the Frame Information that we have just allocated a variable-sized
        // object.
        funcInfo.mf.getFrameInfo().createVariableSizedObject();
        return null;
    }

    @Override
    public Void visitMalloc(User inst)
    {
        Util.shouldNotReachHere("Not implemented currently!");
        return null;
    }

    @Override
    public Void visitLoad(User inst)
    {
        LoadInst li = (LoadInst)inst;
        Value sv = li.operand(0);
        SDValue ptr = getValue(sv);

        Type ty = li.getType();
        boolean isVolatile = li.isVolatile();
        int alignment = li.getAlignment();

        ArrayList<EVT> valueVTs = new ArrayList<>();
        TLongArrayList offsets = new TLongArrayList();
        computeValueVTs(tli, ty, valueVTs, offsets);

        if (valueVTs.isEmpty())
            return null;

        SDValue root = new SDValue();
        boolean constantMemory = false;
        if (li.isVolatile())
        {
            root = getRoot();
        }
        else if (aa.pointsToConstantMemory(sv))
        {
            root = dag.getEntryNode();
            constantMemory = true;
        }
        else
        {
            root = dag.getRoot();
        }

        ArrayList<SDValue> values = new ArrayList<>(valueVTs.size());
        ArrayList<SDValue> chains = new ArrayList<>(valueVTs.size());
        EVT ptrVT = ptr.getValueType();
        for (int i = 0; i < valueVTs.size(); i++)
        {
            SDValue l = dag.getLoad(valueVTs.get(i), root,
                    dag.getNode(ISD.ADD, ptrVT, ptr, dag.getConstant(offsets.get(i), ptrVT, false)),
                    sv, (int) offsets.get(i), isVolatile, alignment);
            values.set(i, l);
            chains.set(i, l.getValue(1));
        }
        if (!constantMemory)
        {
            SDValue chain = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), chains);
            if (isVolatile)
                dag.setRoot(chain);
            else
                pendingLoads.add(chain);
        }
        setValue(li, dag.getNode(ISD.MERGE_VALUES, dag.getVTList(valueVTs), values));
        return null;
    }

    @Override
    public Void visitStore(User inst)
    {
        StoreInst si = (StoreInst)inst;
        Value srcVal = si.operand(0);
        Value ptrVal = si.operand(1);

        ArrayList<EVT> valueVTs = new ArrayList<>();
        TLongArrayList offsets = new TLongArrayList();
        computeValueVTs(tli, srcVal.getType(), valueVTs, offsets);
        int numValues = valueVTs.size();
        if (numValues == 0)
            return null;

        SDValue src = getValue(srcVal);
        SDValue ptr = getValue(ptrVal);

        SDValue root = getRoot();
        ArrayList<SDValue> chains = new ArrayList<>(numValues);
        EVT ptrVT = ptr.getValueType();
        boolean isVolatile = si.isVolatile();
        int align = si.getAlignment();
        for (int i = 0; i < numValues; i++)
        {
            chains.set(i, dag.getStore(root, new SDValue(src.getNode(), src.getResNo()+i),
                    dag.getNode(ISD.ADD, ptrVT, ptr, dag.getConstant(offsets.get(i), ptrVT, false)),
                    ptrVal, (int) offsets.get(i), isVolatile, align));
        }
        dag.setRoot(dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), chains));
        return null;
    }

    @Override
    public Void visitCall(User inst)
    {
        String renameFn = null;
        CallInst ci = (CallInst)inst;
        SDValue callee = getValue(inst.operand(0));

        boolean isTailCall = EnablePerformTailCallOpt.value && ci.isTailCall();
        lowerCallTo(new CallSite(ci), callee, isTailCall);
        return null;
    }

    @Override
    public Void visitGetElementPtr(User inst)
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
    public Void visitPhiNode(User inst)
    {
        Util.shouldNotReachHere("PHI handled specially!");
        return null;
    }

    public Void visitSelect(User u)
    {
        ArrayList<EVT> valueVTs = new ArrayList<>();
        computeValueVTs(tli, u.getType(), valueVTs);
        if (!valueVTs.isEmpty())
        {
            ArrayList<SDValue> values = new ArrayList<>(valueVTs.size());
            SDValue cond = getValue(u.operand(0));
            SDValue trueVal = getValue(u.operand(1));
            SDValue falseVal = getValue(u.operand(2));
            for (int i = 0; i < valueVTs.size(); i++)
            {
                values.set(i, dag.getNode(ISD.SELECT,
                        trueVal.getValueType(), cond,
                        new SDValue(trueVal.getNode(), trueVal.getResNo()+i),
                        new SDValue(falseVal.getNode(), falseVal.getResNo()+i)));
            }
            setValue(u, dag.getNode(ISD.MERGE_VALUES, dag.getVTList(valueVTs),
                    values));
        }
        return null;
    }

    @Override
    public Void visitFree(User inst)
    {
        Util.shouldNotReachHere("Not implemented currently!");
        return null;
    }
}
