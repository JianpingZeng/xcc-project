package backend.codegen.fastISel;
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
import backend.codegen.dagisel.FunctionLoweringInfo;
import backend.intrinsic.Intrinsic;
import backend.support.ErrorHandling;
import backend.target.*;
import backend.value.*;
import backend.value.Instruction.*;
import gnu.trove.list.array.TIntArrayList;
import tools.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.codegen.dagisel.FunctionLoweringInfo.computeValueVTs;
import static tools.APFloat.RoundingMode.rmTowardZero;

/**
 * This is a fast-path instruction selection class that
 * generates poor code and doesn't support illegal types or non-trivial
 * lowering, but runs quickly.
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class FastISel extends MachineFunctionPass
{
    protected MachineBasicBlock mbb;

    // FIXME, 2017/11/19 because of TObjectIntHashMap compute the
    // same value for different key. Like, the computed value of
    // "load i32* %x.addr" is same with "load i32* %retval"
    protected HashMap<Value, Integer> localValueMap;
    protected HashMap<Value, Integer> valueMap;
    protected HashMap<BasicBlock, MachineBasicBlock> mbbMap;
    protected HashMap<AllocaInst, Integer> staticAllocMap;

    protected MachineFunction mf;
    protected Function fn;
    protected MachineRegisterInfo mri;
    protected MachineFrameInfo mfi;
    protected MachineConstantPool mcp;
    protected TargetMachine tm;
    protected TargetMachine.CodeGenOpt optLvel;
    protected TargetData td;
    protected TargetInstrInfo instrInfo;
    protected TargetLowering tli;
    /**
     * Preserves all information about the whole function when codegin on a single
     * basic block.
     */
    protected FunctionLoweringInfo funcInfo;

    private ArrayList<Pair<MachineInstr, Integer>> phiNodeToUpdate;

    private CallLowering cli;

    public void startNewBlock(MachineBasicBlock mbb)
    {
        setCurrentBlock(mbb);
        localValueMap.clear();
    }

    public void setCurrentBlock(MachineBasicBlock mbb)
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
                // Phi instruction already emit.
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

    public boolean targetSelectInstruction(Instruction inst)
    {
        return targetSelectOperation(inst, inst.getOpcode());
    }

    public abstract boolean targetSelectOperation(User inst, Operator opc);

    public int getRegForValue(Value v)
    {
        EVT realVT = tli.getValueType(v.getType(), true);
        // FIXME Can not handle non-simple value currently
        if (!realVT.isSimple())
            return 0;
        MVT vt = realVT.getSimpleVT();
        if (!tli.isTypeLegal(realVT))
        {
            // Promote i1 to a legal type, because it is common and easy.
            if (vt.simpleVT == MVT.i1)
                vt = tli.getTypeToTransformTo(realVT).getSimpleVT();
            else
                return 0;
        }
        realVT = new EVT(vt);

        // Lookup for the instructions accros basic block.
        if (valueMap.containsKey(v))
            return valueMap.get(v);
        // Checks this value whether dead out Basic Block.
        if (localValueMap.containsKey(v) && localValueMap.get(v) != 0)
            return localValueMap.get(v);

        // the result register holds the value.
        int reg = 0;
        // Here, the specified Value is newly reached
        if (v instanceof ConstantInt)
        {
            ConstantInt ci = (ConstantInt)v;
            reg = fastEmit_i(vt, vt, ISD.Constant, ci.getZExtValue());
        }
        else if (v instanceof AllocaInst)
        {
            reg = targetMaterializeConstant((AllocaInst)v);
        }
        else if (v instanceof ConstantPointerNull)
        {
            reg = getRegForValue(ConstantInt.getNullValue(td.getIntPtrType()));
        }
        else if (v instanceof ConstantFP)
        {
            ConstantFP fp = (ConstantFP)v;
            reg = fastEmit_f(vt, vt, ISD.ConstantFP, fp);
            MVT pointerTy = tli.getPointerTy();
            if (reg == 0)
            {
                // Try to convert float number into a integer to check whether it
                // fits.
                int intWidth = pointerTy.getSizeInBits();
                APFloat f = fp.getValueAPF();
                long[] ints = new long[2];
                OutParamWrapper<Boolean> isExact = new OutParamWrapper<>(false);
                f.convertToInteger(ints, intWidth, true/*isSign*/, rmTowardZero, isExact);
                if (isExact.get())
                {
                    APInt intVal = new APInt(ints, intWidth);
                    int intReg = getRegForValue(ConstantInt.get(intVal));
                    if (intReg != 0)
                        reg = fastEmit_r(pointerTy, vt, ISD.SINT_TO_FP, intReg);
                }
            }
        }
        else if (v instanceof ConstantExpr)
        {
            ConstantExpr ce = (ConstantExpr)v;
            if (!selectOperator(ce, ce.getOpcode()) &&
                    !targetSelectOperation(ce, ce.getOpcode()))
                return 0;

            reg = localValueMap.get(ce);
        }
        else if (v instanceof Value.UndefValue)
        {
            reg = createResultReg(tli.getRegClassFor(realVT));
            buildMI(mbb, instrInfo.get(TargetInstrInfo.IMPLICIT_DEF), reg);
        }
        else if (v instanceof GlobalVariable)
        {
            // handle global value.
            reg = loadFromValue((GlobalVariable)v);
        }

        // If target-independent code couldn't handle the value, give target-specific
        // code a try.
        if (reg == 0 && v instanceof Constant)
            reg = targetMaterializeConstant((Constant)v);

        // Because this value is local to current basic block, so we cached it into
        // localValueMap but valueMap.
        if (reg != 0)
            localValueMap.put(v, reg);

        return reg;
    }

    public int loadFromValue(GlobalVariable addr)
    {
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

    public FastISel(TargetMachine tm, TargetMachine.CodeGenOpt optLevel)
    {
        this.tm = tm;
        this.optLvel = optLevel;
        phiNodeToUpdate = new ArrayList<>();
    }

    @Override
    public boolean runOnMachineFunction(MachineFunction mf)
    {
        this.mf = mf;
        this.fn = mf.getFunction();
        tli = tm.getTargetLowering();

        // Collects information about global to whole Function.
        funcInfo = new FunctionLoweringInfo(tli);
        funcInfo.set(fn, mf);

        valueMap = funcInfo.valueMap;
        mbbMap = funcInfo.mbbmap;
        staticAllocMap = funcInfo.staticAllocaMap;
        localValueMap = new HashMap<>();

        mri = mf.getMachineRegisterInfo();
        mfi = mf.getFrameInfo();
        mcp = mf.getConstantPool();
        tm = mf.getTarget();
        td = tm.getTargetData();
        instrInfo = tm.getInstrInfo();
        cli = mf.getTarget().getCallLowering();
        cli.setIsel(this);

        // for debug.
        if (Util.DEBUG)
        {
            System.err.printf("%n%n%n==============%s===============%n", fn.getName());
        }
        selectAllBasicBlocks(fn);

        // Add live-in register into function live in set.
        MachineRegisterInfo regInfo = mf.getMachineRegisterInfo();
        for (Pair<Integer, Integer> entry : regInfo.getLiveIns())
        {
            mf.getEntryBlock().addLiveIn(entry.first);
        }
        phiNodeToUpdate.clear();
        return false;
    }

    public void selectAllBasicBlocks(Function fn)
    {
        // Iterate over all basic blocks in the function.
        for (BasicBlock llvmBB : fn.getBasicBlockList())
        {
            // First, clear the locaValueMap.
            mbb = funcInfo.mbbmap.get(llvmBB);
            startNewBlock(mbb);

            int bi = 0, end = llvmBB.size();

            // Lower any arguments needed in this block if this is entry block.
            if (llvmBB.equals(fn.getEntryBlock()))
            {
                if (!lowerArguments(llvmBB))
                    assert false:"Failed to lower argument!";
            }

            // Do FastISel on as many instructions as possible.
            if (Util.DEBUG)
            {
                System.err.println("========Instructions============");
            }
            for (; bi != end; ++bi)
            {
                Instruction inst = llvmBB.getInstAt(bi);
                if (Util.DEBUG)
                {
                    Util.Debug(inst.toString());
                }

                if (inst instanceof TerminatorInst)
                {
                    // create machine phi node for phinode in successor
                    if(!handlePhiNodeInSuccessorBlocks(llvmBB))
                    {
                        System.err.println("FastISel miss: ");
                        inst.dump();
                        break;
                    }
                }

                // First try normal tablegen-generated "fast" selection.
                if (selectInstruction(inst))
                    continue;

                // Next, try calling the target to attempt to handle the instruction.
                if (targetSelectInstruction(inst))
                    continue;

                // Otherwise, give up on FastISel for the rest of the block.
                // For now, be a little lenient about non-branch terminators.
                if (inst instanceof TerminatorInst)
                {
                    // TODO: 2017/11/17
                    assert false:"TerminatorInst waited to be finished!";
                }
                break;
            }

            if (bi != end)
            {
                assert false:"FastIsel can not gen code for those instruction "
                        + llvmBB.getInstAt(bi).getName();
            }

            // add operand for machine phinode
            finishBasicBlock();
        }
    }

    /**
     * Prints out dump information when finishing codegen on specified BasicBlock.
     */
    private void finishBasicBlock()
    {
        if (Util.DEBUG)
        {
            System.err.println("Target-post-processed machine code:\n");
            mbb.dump();
            System.err.printf("Total amount of phi nodes to update: %d%n",
                    phiNodeToUpdate.size());
            int i = 0;
            for (Pair<MachineInstr, Integer> pair : phiNodeToUpdate)
            {
                System.err.printf("Node %d : (0x%x, %d)%n", i++, pair.first.hashCode(),
                        pair.second);
            }
        }

        // Update PHI Nodes
        for (int i = 0, e = phiNodeToUpdate.size(); i < e; i++)
        {
            MachineInstr mi = phiNodeToUpdate.get(i).first;
            assert mi.getOpcode() == TargetInstrInfo.PHI:
                    "This is not a machine phi node that we are updating!";
            mi.addOperand(MachineOperand.createReg(phiNodeToUpdate.get(i).second, false, false));
            mi.addOperand(MachineOperand.createMBB(mbb, 0));
        }

        phiNodeToUpdate.clear();
    }

    /**
     * A map from LLVM IR PhiNode to Machine PHI instruction.
     */
    private HashMap<PhiNode, MachineInstr> phinode2MI = new HashMap<>();

    private boolean handlePhiNodeInSuccessorBlocks(BasicBlock bb)
    {
        HashSet<MachineBasicBlock> succHandled = new HashSet<>();
        // Check successor node's PhiNode that expect a constant to be available
        // from this block.
        for (int succ = 0, e = bb.getNumSuccessors(); succ < e; succ++)
        {
            BasicBlock succBB = bb.suxAt(succ);
            if (succBB == null || succBB.isEmpty()) continue;
            if (!(succBB.getFirstInst() instanceof PhiNode))
                continue;

            MachineBasicBlock succMBB = funcInfo.mbbmap.get(succBB);

            // If the terminator has multiple identical successors (common for
            // switch), only handle each successor once.
            if (!succHandled.add(succMBB))
                continue;

            for (int i = 0, sz = succBB.size(); i < sz &&
                    succBB.getInstAt(i) instanceof PhiNode; i++)
            {
                PhiNode pn = (PhiNode)succBB.getInstAt(i);
                // don't handle phinode has no use
                if (pn.isUseEmpty()) continue;

                MachineInstr pnMI = null;
                if (phinode2MI.containsKey(pn))
                {
                    pnMI = phinode2MI.get(pn);
                }
                else
                {
                    EVT resVT = tli.getValueType(pn.getType());
                    if (!resVT.isSimple()) return false;

                    int phiReg = mri.createVirtualRegister(tli.getRegClassFor(resVT));
                    assert phiReg > 0 :"failed to create vreg for phinode";

                    valueMap.put(pn, phiReg);
                    ArrayList<EVT> valueVTs = new ArrayList<>();
                    computeValueVTs(tli, pn.getType(), valueVTs);
                    for (EVT ty : valueVTs)
                    {
                        int numRegisters = tli.getNumRegisters(ty);
                        TargetInstrInfo tii = mf.getTarget().getInstrInfo();
                        for (int j = 0; j < numRegisters; j++)
                        {
                            pnMI = buildMI(succMBB, tii.get(TargetInstrInfo.PHI), phiReg + j)
                                    .getMInstr();
                        }
                        phiReg += numRegisters;
                    }
                    phinode2MI.put(pn, pnMI);
                }
                assert pnMI != null;

                EVT vt = tli.getValueType(pn.getType(), true);
                if (vt.equals(new EVT(MVT.Other)) || !tli.isTypeLegal(vt))
                {
                    // promote MVT.i1
                    if (vt.getSimpleVT().simpleVT == MVT.i1)
                        vt = tli.getTypeToTransformTo(vt);
                    else
                    {
                        // erroreous type
                        return false;
                    }
                }

                Value phiOp = pn.getIncomingValueForBlock(bb);
                int reg = getRegForValue(phiOp);
                if (reg <= 0)
                {
                    // errorous type
                    return false;
                }
                phiNodeToUpdate.add(Pair.get(pnMI, reg));
            }
        }
        return true;
    }

    /**
     * This method takes responsibility for lowering the function formal arguments
     * into the specified Memory Location or register.
     * @param llvmBB
     */
    protected abstract boolean lowerArguments(BasicBlock llvmBB);

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
        TargetInstrDesc ii = instrInfo.get(machineInstOpcode);
        buildMI(mbb, ii, resultReg);
        return resultReg;
    }

    public int fastEmitInst_r(int machineInstOpcode,
            TargetRegisterClass rc,
            int op0)
    {
        int resultReg = createResultReg(rc);
        TargetInstrDesc ii = instrInfo.get(machineInstOpcode);

        if (ii.getNumDefs() >= 1)
            buildMI(mbb, ii, resultReg).addReg(op0);
        else
        {
            buildMI(mbb, ii).addReg(op0);
            boolean insertedCopy = instrInfo.copyRegToReg(mbb, mbb.size(),
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
        TargetInstrDesc ii = instrInfo.get(machineInstOpcode);

        if (ii.getNumDefs() >= 1)
            buildMI(mbb, ii, resultReg).addReg(op0).addReg(op1);
        else
        {
            buildMI(mbb, ii).addReg(op0).addReg(op1);
            boolean insertedCopy = instrInfo.copyRegToReg(mbb, mbb.size(),
                    resultReg, ii.implicitDefs[0],
                    rc, rc);
            if (!insertedCopy)
                resultReg = 0;
        }

        return resultReg;
    }

    public int fastEmitInst_ri(
            int machineInstOpcode,
            TargetRegisterClass rc,
            int op0,
            long imm)
    {
        int resultReg = createResultReg(rc);
        TargetInstrDesc ii = instrInfo.get(machineInstOpcode);

        if (ii.getNumDefs() >= 1)
            buildMI(mbb, ii, resultReg).addReg(op0).addImm(imm);
        else
        {
            buildMI(mbb, ii).addReg(op0).addImm(imm);
            boolean insertedCopy = instrInfo.copyRegToReg(mbb, mbb.size(),
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
        TargetInstrDesc ii = instrInfo.get(machineInstOpcode);

        if (ii.getNumDefs() >= 1)
            buildMI(mbb, ii, resultReg).addReg(op0).addFPImm(fp);
        else
        {
            buildMI(mbb, ii).addReg(op0).addFPImm(fp);
            boolean insertedCopy = instrInfo.copyRegToReg(mbb, mbb.size(),
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
        TargetInstrDesc ii = instrInfo.get(machineInstOpcode);

        if (ii.getNumDefs() >= 1)
            buildMI(mbb, ii, resultReg).addReg(op0).addReg(op1).addImm(imm);
        else
        {
            buildMI(mbb, ii).addReg(op0).addReg(op0).addReg(op1).addImm(imm);
            boolean insertedCopy = instrInfo.copyRegToReg(mbb, mbb.size(),
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
        TargetInstrDesc ii = instrInfo.get(machineInstOpcode);

        if (ii.getNumDefs() >= 1)
            buildMI(mbb, ii, resultReg).addImm(imm);
        else
        {
            buildMI(mbb, ii).addImm(imm);
            boolean insertedCopy = instrInfo.copyRegToReg(mbb, mbb.size(),
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
        TargetInstrDesc ii = instrInfo.get(TargetInstrInfo.EXTRACT_SUBREG);

        if (ii.getNumDefs() >= 1)
            buildMI(mbb, ii, resultReg).addReg(op0).addImm(idx);
        else
        {
            buildMI(mbb, ii).addReg(op0).addImm(idx);
            boolean insertedCopy = instrInfo.copyRegToReg(mbb, mbb.size(),
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
            instrInfo.insertBranch(mbb, msucc, null, new ArrayList<>());
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

            instrInfo.copyRegToReg(mbb, mbb.size(), assignedReg, reg, rc, rc);
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
                vt = tli.getTypeToTransformTo(vt);
            }
            else
                return false;
        }

        // transform the constant as rhs operand.
        if (u.operand(0) instanceof Constant &&
                !(u.operand(1) instanceof Constant))
        {
            // swap lhs and rhs.
            Value temp = u.operand(0);
            u.setOperand(0, u.operand(1));
            u.setOperand(1, temp);
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
            if(cf.isNullValue())
            {
                u.replaceAllUsesWith(cf);
                return true;
            }

            int resultReg = fastEmit_rf(vt.getSimpleVT(), vt.getSimpleVT(),
                    opcode, op0, cf);

            if (resultReg != 0)
            {
                // We successfully emitted code for the given LLVM instruction.
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

    private boolean selectCall(User u)
    {
        CallInst ci = (CallInst)u;
        Function f = ci.getCalledFunction();
        assert f!= null:"The function to be called must be not null!";

        Intrinsic.ID iid = f.getIntrinsicID();
        if (iid != Intrinsic.ID.not_intrinsic)
        {
            ErrorHandling.llvmReportError("Calling to intrinsic is not supported!");
            return false;
        }

        int res = ci.getType().isVoidType() ? 0 : createResultReg(tli.getRegClassFor(tli.getValueType(ci.getType())));
        TIntArrayList args = new TIntArrayList();
        for (int i = 0, e = ci.getNumsOfArgs(); i < e; i++)
        {
            // ConstantInt argument is not needed to reside in virt register.
            if (ci.argumentAt(i) instanceof ConstantInt)
                args.add(0);
            else
                args.add(getRegForValue(ci.argumentAt(i)));
        }

        mf.getFrameInfo().setHasCalls(true);
        return cli.lowerCall(mbb, ci, res, args, getRegForValue(ci.getCalledValue()));
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

            boolean insertCopy = instrInfo
                    .copyRegToReg(mbb, mbb.size(), resultReg,
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
            srcVT = tli.getTypeToTransformTo(srcVT);
            inputReg = fastEmitZExtFromI1(srcVT.getSimpleVT(), inputReg);
            if (inputReg == 0)
                return false;
        }

        if (dstVT.equals(new EVT(new MVT(MVT.i1))))
        {
            dstVT = tli.getTypeToTransformTo(dstVT);
        }

        int resultReg = fastEmit_r(srcVT.getSimpleVT(),
                dstVT.getSimpleVT(),
                opcode, inputReg);
        if (resultReg == 0)
        {
            resultReg = selectIntToFP((Instruction) u);
            if (resultReg == 0) return false;
        }

        updateValueMap(u, resultReg);
        return true;
    }

    /**
     * This method is called when {@linkplain #selectCast(User, int)} fail to
     * generate code for Int2FP conversion operation.
     * @param inst  Int to FP conversion.
     * @return  The result register.
     */
    public abstract int selectIntToFP(Instruction inst);
}
