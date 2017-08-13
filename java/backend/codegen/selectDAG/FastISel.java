package backend.codegen.selectDAG;
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
import backend.target.*;
import backend.type.SequentialType;
import backend.type.StructType;
import backend.type.Type;
import backend.value.*;
import backend.value.Instruction.AllocaInst;
import backend.value.Instruction.BranchInst;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.APFloat;
import tools.APInt;
import tools.OutParamWrapper;

import java.util.ArrayList;
import java.util.HashMap;

import static backend.codegen.MachineInstrBuilder.buildMI;
import static tools.APFloat.RoundingMode.rmTowardZero;

/**
 * This is a fast-path instruction selection class that
 * generates poor code and doesn't support illegal types or non-trivial
 * lowering, but runs quickly.
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class FastISel
{
    protected MachineBasicBlock mbb;
    protected TObjectIntHashMap<Value> localValueMap;
    protected TObjectIntHashMap<Value> valueMap;
    protected HashMap<BasicBlock, MachineBasicBlock> mbbMap;
    protected TObjectIntHashMap<AllocaInst> staticAllocMap;

    protected MachineFunction mf;
    protected MachineModuleInfo mmi;
    protected MachineRegisterInfo mri;
    protected MachineFrameInfo mfi;
    protected MachineConstantPool mcp;
    protected TargetMachine tm;
    protected TargetData td;
    protected TargetInstrInfo tii;
    protected TargetLowering tli;

    public void startNewBlock(MachineBasicBlock mbb)
    {
        setCUrrentBlock(mbb);
        localValueMap.clear();
    }

    public void setCUrrentBlock(MachineBasicBlock mbb)
    {
        this.mbb = mbb;
    }

    /**
     * Do "fast" instruction selection for the given
     * LLVM IR instruction, and append generated machine instructions to
     * the current block. Return true if selection was successful.
     * @param inst
     * @return
     */
    public boolean selectInstruction(Instruction inst)
    {
        return selectOperator(inst, inst.getOpcode());
    }

    /**
     * Do "fast" instruction selection for the given
     * LLVM IR operator (Instruction or ConstantExpr), and append
     * generated machine instructions to the current block. Return true
     * if selection was successful.
     * @param u
     * @param opcode
     * @return
     */
    public boolean selectOperator(User u, Operator opcode)
    {
        switch (opcode)
        {
            case Add:
                return selectBinaryOp(u, ISD.ADD);
            case FAdd:
                return selectBinaryOp(u, ISD.FADD);
            case Sub:
                return selectBinaryOp(u, ISD.SUB);
            case FSub:
                return selectBinaryOp(u, ISD.FSUB);
            case Mul:
                return selectBinaryOp(u, ISD.MUL);
            case FMul:
                return selectBinaryOp(u, ISD.FMUL);
            case SDiv:
                return selectBinaryOp(u, ISD.SDIV);
            case UDiv:
                return selectBinaryOp(u, ISD.UDIV);
            case FDiv:
                return selectBinaryOp(u, ISD.FDIV);
            case SRem:
                return selectBinaryOp(u, ISD.SREM);
            case URem:
                return selectBinaryOp(u, ISD.UREM);
            case FRem:
                return selectBinaryOp(u, ISD.FREM);
            case Shl:
                return selectBinaryOp(u, ISD.SHL);
            case LShr:
                return selectBinaryOp(u, ISD.SRL);
            case AShr:
                return selectBinaryOp(u, ISD.SRA);
            case And:
                return selectBinaryOp(u, ISD.AND);
            case Or:
                return selectBinaryOp(u, ISD.OR);
            case Xor:
                return selectBinaryOp(u, ISD.XOR);

            case GetElementPtr:
                return selectGetElementPtr(u);

            case Br:
            {
                BranchInst bi = (BranchInst)u;
                if (bi.isUnconditional())
                {
                    BasicBlock llvmSuccBB = bi.getSuccessor(0);
                    MachineBasicBlock mSuccBB = mbbMap.get(llvmSuccBB);
                    fastEmitBranch(mSuccBB);
                    return true;
                }

                // Conditional branches are not handed yet.
                // Halt "fast" selection and bail.
                return false;
            }

            case Phi:
                // PHI nodes are already emitted.
                return true;

            case Alloca:
                // FunctionLowering has the static-sized case covered.
                if (staticAllocMap.containsKey(u))
                    return true;

                // Dynamic-sized alloca is not handled yet.
                return false;

            case Call:
                return selectCall(u);

            case BitCast:
                return selectBitCast(u);

            case FPToSI:
                return selectCast(u, ISD.FP_TO_SINT);
            case ZExt:
                return selectCast(u, ISD.ZERO_EXTEND);
            case SExt:
                return selectCast(u, ISD.SIGN_EXTEND);
            case Trunc:
                return selectCast(u, ISD.TRUNCATE);
            case SIToFP:
                return selectCast(u, ISD.SINT_TO_FP);

            case IntToPtr: // Deliberate fall-through.
            case PtrToInt:
            {
                EVT SrcVT = tli.getValueType(u.operand(0).getType());
                EVT DstVT = tli.getValueType(u.getType());
                if (DstVT.bitsGT(SrcVT))
                    return selectCast(u, ISD.ZERO_EXTEND);
                if (DstVT.bitsLT(SrcVT))
                    return selectCast(u, ISD.TRUNCATE);
                int Reg = getRegForValue(u.operand(0));
                if (Reg == 0) return false;
                updateValueMap(u, Reg);
                return true;
            }

            default:
                // Unhandled instruction. Halt "fast" selection and bail.
                return false;
        }
    }

    public abstract boolean targetSelectInstruction(Instruction inst);

    public int getRegForValue(Value v)
    {
        // TODO: 17-7-17
        return 0;
    }

    public int lookupRegForValue(Value v)
    {
        if (valueMap.containsKey(v))
            return valueMap.get(v);
        
        return localValueMap.get(v);
    }

    public int getRegForGEPIndex(Value idx)
    {
        int idxReg = getRegForValue(idx);
        if (idxReg == 0)
            return 0;

        MVT ptrVT = tli.getPointerTy();
        EVT idxVT = EVT.getEVT(idx.getType(), false);
        if (idxVT.bitsLT(new EVT(ptrVT)))
            idxReg = fastEmit_r(idxVT.getSimpleVT(), ptrVT, ISD.SIGN_EXTEND, idxReg);
        else if (idxVT.bitsGT(new EVT(ptrVT)))
            idxReg = fastEmit_r(idxVT.getSimpleVT(), ptrVT, ISD.TRUNCATE, idxReg);
        return idxReg;
    }

    protected FastISel(MachineFunction mf, MachineModuleInfo mmi,
            TObjectIntHashMap<Value> vm,
            HashMap<BasicBlock, MachineBasicBlock> bm,
            TObjectIntHashMap<AllocaInst> am)
    {
        mbb = null;
        valueMap = vm;
        mbbMap = bm;
        staticAllocMap = am;
        this.mf = mf;
        this.mmi = mmi;
        mri = mf.getMachineRegisterInfo();
        mfi = mf.getFrameInfo();
        mcp = mf.getConstantPool();
        tm = mf.getTarget();
        td = tm.getTargetData();
        tii = tm.getInstrInfo();
        tli = tm.getTargetLowering();
    }

    public int createResultReg(TargetRegisterClass rc)
    {
        return mri.createVirtualRegister(rc);
    }

    /**
     * This method is called by target-independent code
     * to request that an instruction with the given type and opcode
     * be emitted.
     * @param vt
     * @param retVT
     * @param opcode
     * @return
     */
    public int fastEmit_(MVT vt, MVT retVT, int opcode)
    {
        return 0;
    }

    public int fastEmit_r(MVT vt, MVT retVT, int opcode, int op0)
    {
        return 0;
    }

    public int fastEmit_rr(MVT vt, MVT retVT, int opcdoe, int op0, int op1)
    {
        return 0;
    }

    public int fastEmit_ri(MVT vt, MVT retVT, int opcdoe, int op0, long imm)
    {
        return 0;
    }

    public int fastEmit_rf(MVT vt, MVT retVT, int opcode, int op0, ConstantFP fpImm)
    {
        return 0;
    }

    public int fastEmit_rri(MVT vt, MVT retVT, int opcode, int op0, int op1, long imm)
    {
        return 0;
    }

    /**
     * This method is a wrapper of FastEmit_ri. It first tries
     * to emit an instruction with an immediate operand using FastEmit_ri.
     * If that fails, it materializes the immediate into a register and try
     * FastEmit_rr instead.
     * @param vt
     * @param opcode
     * @param op0
     * @param imm
     * @param immType
     * @return
     */
    public int fastEmit_ri_(MVT vt, int opcode, int op0, long imm, MVT immType)
    {
        int resultReg = fastEmit_ri(vt, vt, opcode, op0, imm);
        if (resultReg != 0)
            return resultReg;
        int materialReg = fastEmit_i(immType, immType, ISD.Constant, imm);
        if (materialReg == 0)
            return 0;

        return fastEmit_rr(vt, vt, opcode, op0, resultReg);
    }

    /**
     * This method is a wrapper of FastEmit_rf. It first tries
     * to emit an instruction with an immediate operand using FastEmit_rf.
     * If that fails, it materializes the immediate into a register and try
     * FastEmit_rr instead.
     * @param vt
     * @param opcode
     * @param op0
     * @param fpImm
     * @param immType
     * @return
     */
    public int fastEmit_rf_(MVT vt, int opcode, int op0, ConstantFP fpImm, MVT immType)
    {
        int resultReg = fastEmit_rf(vt, vt, opcode, op0, fpImm);
        if (resultReg != 0)
            return resultReg;

        int materialReg = fastEmit_f(immType, immType, ISD.ConstantFP, fpImm);
        if (materialReg == 0)
        {
            APFloat flt = fpImm.getValueAPF();
            EVT intVT = new EVT(tli.getPointerTy());

            long[] x = new long[2];
            long intBitWidth = intVT.getSizeInBits();

            OutParamWrapper<Boolean> isExact = new OutParamWrapper<>();
            flt.convertToInteger(x, (int) intBitWidth, true, rmTowardZero, isExact);
            if (!isExact.get())
                return 0;

            APInt intVal = new APInt((int) intBitWidth, 2, x);

            int integerReg = fastEmit_i(intVT.getSimpleVT(), intVT.getSimpleVT(),
                    ISD.Constant, intVal.getZExtValue());
            if (integerReg == 0)
                return 0;

            materialReg = fastEmit_r(intVT.getSimpleVT(), vt, ISD.SINT_TO_FP,
                    integerReg);

            if (materialReg == 0)
                return 0;
        }
        return fastEmit_rr(vt, vt, opcode, op0, materialReg);
    }

    public int fastEmit_i(MVT vt, MVT retVT, int opcode, long imm)
    {
        return 0;
    }

    public int fastEmit_f(MVT vt, MVT retVT, int opcode, ConstantFP fpImm)
    {
        return 0;
    }

    /**
     * Emit a MachineInstr with no operands and a
     * result register in the given register class.
     * @param machineInstOpcode
     * @param rc
     * @return
     */
    public int fastEmitInst_(int machineInstOpcode, TargetRegisterClass rc)
    {
        int resultReg = createResultReg(rc);
        TargetInstrDesc ii = tii.get(machineInstOpcode);
        buildMI(mbb, ii, resultReg);
        return resultReg;
    }

    public int fastEmitInst_r(int machineInstOpcode,
            TargetRegisterClass rc,
            int op0)
    {
        int resultReg = createResultReg(rc);
        TargetInstrDesc ii = tii.get(machineInstOpcode);

        if (ii.getNumDefs() >= 1)
            buildMI(mbb, ii, resultReg).addReg(op0);
        else
        {
            buildMI(mbb, ii).addReg(op0);
            boolean insertedCopy = tii.copyRegToReg(mbb, mbb.size(),
                    resultReg, ii.implicitDefs[0],
                    rc, rc);
            if (!insertedCopy)
                resultReg = 0;
        }

        return resultReg;
    }

    public int fastEmitInst_rr(int machineInstOpcode,
            TargetRegisterClass rc,
            int op0,
            int op1)
    {
        int resultReg = createResultReg(rc);
        TargetInstrDesc ii = tii.get(machineInstOpcode);

        if (ii.getNumDefs() >= 1)
            buildMI(mbb, ii, resultReg).addReg(op0).addReg(op1);
        else
        {
            buildMI(mbb, ii).addReg(op0).addReg(op1);
            boolean insertedCopy = tii.copyRegToReg(mbb, mbb.size(),
                    resultReg, ii.implicitDefs[0],
                    rc, rc);
            if (!insertedCopy)
                resultReg = 0;
        }

        return resultReg;
    }

    public int fastEmitInst_ri(int machineInstOpcode,
            TargetRegisterClass rc,
            int op0,
            long imm)
    {
        int resultReg = createResultReg(rc);
        TargetInstrDesc ii = tii.get(machineInstOpcode);

        if (ii.getNumDefs() >= 1)
            buildMI(mbb, ii, resultReg).addReg(op0).addImm(imm);
        else
        {
            buildMI(mbb, ii).addReg(op0).addImm(imm);
            boolean insertedCopy = tii.copyRegToReg(mbb, mbb.size(),
                    resultReg, ii.implicitDefs[0],
                    rc, rc);
            if (!insertedCopy)
                resultReg = 0;
        }

        return resultReg;
    }

    public int fastEmitInst_rf(int machineInstOpcode,
            TargetRegisterClass rc,
            int op0,
            ConstantFP fp)
    {
        int resultReg = createResultReg(rc);
        TargetInstrDesc ii = tii.get(machineInstOpcode);

        if (ii.getNumDefs() >= 1)
            buildMI(mbb, ii, resultReg).addReg(op0).addFPImm(fp);
        else
        {
            buildMI(mbb, ii).addReg(op0).addFPImm(fp);
            boolean insertedCopy = tii.copyRegToReg(mbb, mbb.size(),
                    resultReg, ii.implicitDefs[0],
                    rc, rc);
            if (!insertedCopy)
                resultReg = 0;
        }

        return resultReg;
    }

    public int fastEmitInst_rri(int machineInstOpcode,
            TargetRegisterClass rc,
            int op0,
            int op1,
            long imm)
    {
        int resultReg = createResultReg(rc);
        TargetInstrDesc ii = tii.get(machineInstOpcode);

        if (ii.getNumDefs() >= 1)
            buildMI(mbb, ii, resultReg).addReg(op0).addReg(op1).addImm(imm);
        else
        {
            buildMI(mbb, ii).addReg(op0).addReg(op0).addReg(op1).addImm(imm);
            boolean insertedCopy = tii.copyRegToReg(mbb, mbb.size(),
                    resultReg, ii.implicitDefs[0],
                    rc, rc);
            if (!insertedCopy)
                resultReg = 0;
        }

        return resultReg;
    }

    public int fastEmitInst_i(int machineInstOpcode,
            TargetRegisterClass rc,
            long imm)
    {
        int resultReg = createResultReg(rc);
        TargetInstrDesc ii = tii.get(machineInstOpcode);

        if (ii.getNumDefs() >= 1)
            buildMI(mbb, ii, resultReg).addImm(imm);
        else
        {
            buildMI(mbb, ii).addImm(imm);
            boolean insertedCopy = tii.copyRegToReg(mbb, mbb.size(),
                    resultReg, ii.implicitDefs[0],
                    rc, rc);
            if (!insertedCopy)
                resultReg = 0;
        }

        return resultReg;
    }

    public int fastEmitInst_extractsubreg(int retVT,
            int op0,
            int idx)
    {
        return fastEmitInst_extractsubreg(new MVT(retVT), op0, idx);
    }

    public int fastEmitInst_extractsubreg(MVT retVT,
            int op0,
            int idx)
    {
        TargetRegisterClass rc = mri.getRegClass(op0);

        int resultReg = createResultReg(tli.getRegClassFor(new EVT(retVT)));
        TargetInstrDesc ii = tii.get(TargetInstrInfo.EXTRACT_SUBREG);

        if (ii.getNumDefs() >= 1)
            buildMI(mbb, ii, resultReg).addReg(op0).addImm(idx);
        else
        {
            buildMI(mbb, ii).addReg(op0).addImm(idx);
            boolean insertedCopy = tii.copyRegToReg(mbb, mbb.size(),
                    resultReg, ii.implicitDefs[0],
                    rc, rc);
            if (!insertedCopy)
                resultReg = 0;
        }

        return resultReg;
    }

    /**
     * Emit MachineInstrs to compute the value of Op
     * with all but the least significant bit set to zero.
     * @param vt
     * @param op
     * @return
     */
    public int fastEmitZExtFromI1(MVT vt, int op)
    {
        return fastEmit_ri(vt, vt, ISD.AND, op, 1);
    }

    public void fastEmitBranch(MachineBasicBlock msucc)
    {
        if (mbb.isLayoutSuccessor(msucc))
        {
            // The unconditional fall-through case, which needs no instructions.
        }
        else
        {
            tii.InsertBranch(mbb, msucc, null, new ArrayList<>());
        }
        mbb.addSuccessor(msucc);
    }

    public int updateValueMap(Value val, int reg)
    {
        if (!(val instanceof Instruction))
        {
            localValueMap.put(val, reg);
            return reg;
        }

        if (!valueMap.containsKey(val))
        {
            valueMap.put(val, reg);
            return reg;
        }
        else if (valueMap.get(val) != reg)
        {
            int assignedReg = valueMap.get(val);
            TargetRegisterClass rc = mri.getRegClass(reg);

            tii.copyRegToReg(mbb, mbb.size(), assignedReg, reg, rc, rc);
            return assignedReg;
        }
        return reg;
    }

    public int targetMaterializeConstant(Constant c)
    {
        return 0;
    }

    public int targetMaterializeConstant(AllocaInst ai)
    {
        return 0;
    }

    /**
     * Select and emit code for a binary operator instruction which has an opcode
     * which directly corresponds to the given ISD opcode.
     * @param u
     * @param opcode
     * @return
     */
    private boolean selectBinaryOp(User u, int opcode)
    {
        EVT vt = EVT.getEVT(u.getType(), true);
        if (vt.equals(new EVT(new MVT(MVT.Other))) || !vt.isSimple())
            // Unhandled type. halt "fast" selection and bail.
            return false;

        // We only handle legal types. For example, on x86-32 the instruction
        // selector contains all of the 64-bit instructions from x86-64,
        // under the assumption that i64 won't be used if the target doesn't
        // support it.
        if (!tli.isTypeLegal(vt))
        {
            if (vt.equals(new EVT(new MVT(MVT.i1))) &&
                    (opcode == ISD.AND ||
                    opcode == ISD.OR ||
                    opcode == ISD.XOR))
            {
                vt = tli.getTypeForTransformTo(vt);
            }
            else
                return false;
        }

        // obtain the assigned register of the first operand.
        int op0 = getRegForValue(u.operand(0));
        if (op0 == 0)
            // Unhandled operand, halt "fast" instruction selection.
            return false;

        // Check if the second operand is a constant integer or not.
        // attempt to constant folding.
        if (u.operand(1) instanceof ConstantInt)
        {
            ConstantInt ci = (ConstantInt)u.operand(1);
            int resultReg = fastEmit_ri(vt.getSimpleVT(), vt.getSimpleVT(),
                    opcode, op0, ci.getZExtValue());

            if (resultReg != 0)
            {
                // We successfully emitted code for the gien LLVM instruction.
                updateValueMap(u, resultReg);
                return true;
            }
        }

        // Check if the second operand is a constant float.
        if (u.operand(1) instanceof ConstantFP)
        {
            ConstantFP cf = (ConstantFP)u.operand(1);
            int resultReg = fastEmit_rf(vt.getSimpleVT(), vt.getSimpleVT(),
                    opcode, op0, cf);

            if (resultReg != 0)
            {
                // We successfully emitted code for the gien LLVM instruction.
                updateValueMap(u, resultReg);
                return true;
            }
        }

        int op1 = getRegForValue(u.operand(1));
        if (op1 == 0)
        {
            // Unhandled operand. Halt "fast" selection and bail.
            return false;
        }

        int resultReg = fastEmit_rr(vt.getSimpleVT(), vt.getSimpleVT(),
                opcode, op0, op1);
        if (resultReg == 0)
        {
            // Target-specific code wasn't able to find a machine opcode for
            // the given ISD opcode and type. Halt "fast" selection and bail.
            return false;
        }

        updateValueMap(u, resultReg);
        return true;
    }

    private boolean selectGetElementPtr(User u)
    {
        int baseAddr = getRegForValue(u.operand(0));
        if (baseAddr == 0)
            // Unhandled operand. Halt "Fast" instruction selection.
            return false;

        Type ty = u.operand(0).getType();
        MVT vt = tli.getPointerTy();
        for (int i = 1; i < u.getNumOfOperands(); i++)
        {
            Value idx = u.operand(i);
            if (ty instanceof StructType)
            {
                StructType sty = (StructType)ty;
                long field = ((ConstantInt)idx).getZExtValue();
                if (field != 0)
                {
                    // baseAddr = baseAddr + offset.
                    long offset = td.getStructLayout(sty).getElementOffset(field);

                    baseAddr = fastEmit_ri_(vt, ISD.ADD, baseAddr, offset, vt);
                    if (baseAddr == 0)
                        return false;
                }

                ty = sty.getElementType((int) field);
            }
            else
            {
                assert ty instanceof SequentialType;
                ty = ((SequentialType)ty).getElementType();

                // If this is a constant subscript, handle it quickly.
                if (idx instanceof ConstantInt)
                {
                    ConstantInt ci = (ConstantInt)idx;
                    if (ci.getZExtValue() == 0)
                        continue;
                    long off = td.getTypeAllocSize(ty) * ((ConstantInt)ci).getSExtValue();

                    // baseAddr = baseAddr + sizeOfElt * idx.
                    baseAddr = fastEmit_ri_(vt, ISD.ADD, baseAddr, off, vt);
                    if (baseAddr == 0)
                        // Failure.
                        return false;
                    continue;
                }

                // baseAddr = baseAddr + sizeOfElt * idx.
                long eltSize = td.getTypeAllocSize(ty);
                int idxN = getRegForGEPIndex(idx);
                if (idxN == 0)
                    return false;

                if (eltSize != 1)
                {
                    // If eltSize != 0, emit multiple instructon.
                    idxN = fastEmit_ri_(vt, ISD.MUL, idxN, eltSize, vt);
                    if (idxN == 0)
                        return false;
                }
                baseAddr = fastEmit_rr(vt, vt, ISD.ADD, baseAddr, idxN);
                if (baseAddr == 0)
                    return false;
            }
        }

        // We successfully emitted code for the given LLVM instruction.
        updateValueMap(u, baseAddr);
        return true;
    }

    private boolean selectCall(User u)
    {
        Function f = ((Instruction.CallInst)u).getCalledFunction();
        if (f == null)
            return false;

        int iid = f.getIntrinsicID();
        switch (iid)
        {
            default: break;
            // TODO: 17-7-17 Handle calling to intrinsic function.
        }
        return false;
    }

    private boolean selectBitCast(User u)
    {
        // If the bitcast doesn't change the type, just use the operand value.
        if (u.getType().equals(u.operand(0).getType()))
        {
            int reg = getRegForValue(u.operand(0));
            if (reg == 0)
                return false;

            updateValueMap(u, reg);
        }

        EVT srcVT = tli.getValueType(u.operand(0).getType());
        EVT dstVT = tli.getValueType(u.getType());

        if (srcVT.equals(new EVT(new MVT(MVT.Other))) || !srcVT.isSimple() ||
                dstVT.equals(new EVT(new MVT(MVT.Other))) || !dstVT.isSimple())
            return false;

        int op0 = getRegForValue(u.operand(0));
        if (op0 == 0)
            return false;

        // First, try to perform the bitcast by inserting a reg-reg copy.
        int resultReg = 0;
        if (srcVT.getSimpleVT().equals(dstVT.getSimpleVT()))
        {
            TargetRegisterClass srcClass = tli.getRegClassFor(srcVT);
            TargetRegisterClass dstClass = tli.getRegClassFor(dstVT);
            resultReg = createResultReg(dstClass);

            boolean insertCopy = tii.copyRegToReg(mbb, mbb.size(), resultReg,
                    op0, dstClass, srcClass);
            if (!insertCopy)
                resultReg = 0;
        }

        // If the reg-reg copy failed, select a BIT_CONVERT opcode.
        if (resultReg == 0)
        {
            resultReg = fastEmit_r(srcVT.getSimpleVT(), dstVT.getSimpleVT(),
                    ISD.BIT_CONVERT, op0);
        }

        if (resultReg == 0)
            return false;

        updateValueMap(u, resultReg);
        return false;
    }

    private boolean selectCast(User u, int opcode)
    {
        EVT srcVT = tli.getValueType(u.operand(0).getType());
        EVT dstVT = tli.getValueType(u.getType());

        if (srcVT.equals(new EVT(new MVT(MVT.Other))) || !srcVT.isSimple() ||
                dstVT.equals(new EVT(new MVT(MVT.Other))) || !dstVT.isSimple())
            return false;

        if (!tli.isTypeLegal(dstVT))
        {
            if (!dstVT.equals(new EVT(new MVT(MVT.i1))) || opcode != ISD.TRUNCATE)
                return false;
        }

        if (!tli.isTypeLegal(srcVT))
        {
            if (!srcVT.equals(new EVT(new MVT(MVT.i1))) || opcode != ISD.ZERO_EXTEND)
                return false;
        }

        int inputReg = getRegForValue(u.operand(0));
        if (inputReg == 0)
            return false;

        // If the operand is i1, arrange for the high bits in the register to be zero.
        if (srcVT.equals(new EVT(new MVT(MVT.i1))))
        {
            srcVT = tli.getTypeForTransformTo(srcVT);
            inputReg = fastEmitZExtFromI1(srcVT.getSimpleVT(), inputReg);
            if (inputReg == 0)
                return false;
        }

        if (dstVT.equals(new EVT(new MVT(MVT.i1))))
        {
            dstVT = tli.getTypeForTransformTo(dstVT);
        }

        int resultReg = fastEmit_r(srcVT.getSimpleVT(),
                dstVT.getSimpleVT(),
                opcode, inputReg);
        if (resultReg == 0)
            return false;

        updateValueMap(u, resultReg);
        return true;
    }
}
