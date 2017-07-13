package backend.target.x86;

import backend.codegen.*;
import backend.codegen.MachineOperand.UseType;
import backend.value.BasicBlock;
import backend.utils.InstVisitor;
import backend.value.Operator;
import backend.pass.FunctionPass;
import backend.target.TargetData;
import backend.target.TargetInstrInfo;
import backend.target.TargetMachine;
import backend.target.TargetRegisterClass;
import backend.type.SequentialType;
import backend.type.StructType;
import backend.type.Type;
import backend.value.*;
import backend.value.Instruction.*;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static backend.codegen.MachineInstrBuilder.*;
import static backend.target.x86.X86SimpleInstSel.TypeClass.*;
import static backend.value.Instruction.CmpInst.Predicate.FIRST_ICMP_PREDICATE;
import static backend.value.Instruction.CmpInst.Predicate.LAST_ICMP_PREDICATE;

/**
 * This is a simple implementation of instruction selector with peephole optimization
 * in X86 machine.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86SimpleInstSel implements InstVisitor<Void>,FunctionPass
{
    interface TypeClass
    {
        int i8 = 0;
        int i16 = 1;
        int i32 = 2;
        int i64 = 3;
        int f32 = 4;
        int f64 = 5;
        int f80 = 6;
    }

    static class ValueRecord
    {
        Value val;
        int reg;
        Type ty;

        ValueRecord(int r, Type t)
        {
            reg = r;
            ty = t;
        }

        ValueRecord(Value v)
        {
            val = v;
            reg = 0;
            ty = v.getType();
        }
    }

    private TargetMachine tm;
    private MachineFunction mf;

    /**
     * The current MachineBasicBlock where all generated machineInstr would be filled.
     */
    private MachineBasicBlock mbb;

    /**
     * Mapping between SSA value and virtual register
     * (it just is a non-negative integer).
     */
    private TObjectIntHashMap<Value> regMap;

    /**
     * Mapping between HIR basic block and machine basic block.
     */
    private HashMap<BasicBlock, MachineBasicBlock> mbbMap;
    /**
     * The index into the first vararg value of variable argument list.
     */
    private int varArgsFrameIndex;

    private X86SimpleInstSel(TargetMachine machine)
    {
        tm = machine;
        regMap = new TObjectIntHashMap<Value>();
        mbbMap = new HashMap<>();
    }

    /**
     * Top level implementation of instruction selector for x86 target on entire
     * function.
     *
     * @param f The FunctionProto object to be converted to MachineFunction.
     * @return Return {@code true} if the CFG is not changed.
     */
    @Override
    public boolean runOnFunction(Function f)
    {
        mf = new MachineFunction(f, tm);

        // fills mbbMap, create a map from BasicBlock to MachineBasicBlock.
        f.getBasicBlockList().forEach(bb -> mbbMap.put(bb, new MachineBasicBlock(bb)));

        mbb = mf.getEntryBlock();

        // assigns incoming arguments with virtual register.
        assignArgumentsWithVirtualReg(f);

        // First pass: performs inst selecting expect for Phi node.
        visit(f);

        // Second pass: lowering PHI nodes.
        selectPhiNodes();

        regMap.clear();
        mbbMap.clear();
        return true;
    }

    private static MachineInstrBuilder bmi(MachineBasicBlock mbb, int idx, int opcode,
            int numOperands, int destReg)
    {
        assert idx >= 0 && idx < mbb.size();
        MachineInstr instr = new MachineInstr(opcode, numOperands + 1);
        mbb.insert(idx, instr);
        return new MachineInstrBuilder(instr);
    }

    private static MachineInstrBuilder bmi(MachineBasicBlock mbb, int idx, int opcode,
            int numOperands)
    {
        assert idx >= 0 && idx < mbb.size();
        MachineInstr instr = new MachineInstr(opcode, numOperands);
        mbb.insert(idx, instr);
        return new MachineInstrBuilder(instr);
    }

    /**
     * This method returns the next virtual register number we have not yet used.
     * <p>
     * Long values are handled somewhat specially, they are always allocated as '
     * pairs of 32 bit integer values. The register number returned
     * is the upper 32 bit of the long value.
     *
     * @param ty
     * @return
     */
    private int makeAnotherReg(Type ty)
    {
        assert tm
                .getRegInfo() instanceof X86RegisterInfo : "Current target doesn't have x86 reg info!";
        X86RegisterInfo mri = (X86RegisterInfo) tm.getRegInfo();
        if (ty.getTypeID() == Type.IntegerTyID)
        {
            TargetRegisterClass rc = mri.getRegClassForType(Type.Int32Ty);
            mf.getMachineRegisterInfo().createVirtualRegister(rc);
            return mf.getMachineRegisterInfo().createVirtualRegister(rc) - 1;
        }

        TargetRegisterClass rc = mri.getRegClassForType(ty);
        return mf.getMachineRegisterInfo().createVirtualRegister(rc);
    }

    private void emitGEPOperation(int insertPos, Value base, List<Use> uselists,
            int destReg)
    {
        int baseReg = getReg(base, mbb, insertPos);
        TargetData td = tm.getTargetData();
        Type ty = base.getType();

        // GEPs have zero or more indices; we must perform a struct access
        // or array access for each one.
        for (Use u : uselists)
        {
            Value idx = u.getValue();
            int nextReg = baseReg;

            if (ty instanceof StructType)
            {
                StructType sty = (StructType) ty;
                // It is a struct access. idx is the index into structure member
                //
                ConstantInt ci = (ConstantInt) idx;
                assert ci.getType() == Type.Int8Ty;

                // use the TargetData to pick up what the layout of the structure
                // is in memory.  Since the structure index must
                // be constant, we can get its value and use it to find the
                // right byte offset from the StructLayout class's list of
                // structure member offsets.
                int idxValue = (int) ci.getZExtValue();
                int fieldOff = td.getStructLayout(sty).
                        memberOffsets.get(idxValue).intValue();

                if (fieldOff != 0)
                {
                    nextReg = makeAnotherReg(Type.Int32Ty);
                    // emit an ADD to add fieldOff to the basePtr.
                    bmi(mbb, insertPos++, X86InstrNames.ADDri32, 2, nextReg).
                            addReg(baseReg, UseType.Use).addZImm(fieldOff);
                }
                // The next type is the member of the structure selected by the
                // index.
                ty = sty.getElementType(idxValue);
            }
            else if (ty instanceof SequentialType)
            {
                SequentialType sty = (SequentialType) ty;
                // It's an array or pointer access: [ArraySize x ElementType].
                // idx is the index into the array.
                // Unlike with structure
                // indices, we may not know its actual value at code-generation
                // time.

                // Most GEP instructions use a [cast (int/uint) to LongTy] as their
                // operand on X86.  Handle this case directly now...
                if (idx instanceof CastInst)
                {
                    CastInst ci = (CastInst) idx;
                    if (ci.operand(0).getType() == Type.Int32Ty)
                        idx = ci.operand(0);
                }

                ty = sty.getElemType();
                int eltSize = (int) td.getTypeSize(ty);

                // If idxReg is a constant, we don't need to generate mul instr.
                if (idx instanceof ConstantInt)
                {
                    ConstantInt ci = (ConstantInt) idx;
                    if (!ci.isNullValue())
                    {
                        long offset = eltSize * ci.getZExtValue();
                        nextReg = makeAnotherReg(Type.Int32Ty);
                        bmi(mbb, insertPos++, X86InstrNames.ADDri32, 2, nextReg)
                                .addReg(baseReg).addZImm(offset);
                    }
                }
                else if (eltSize == 1)
                {
                    // If the element getNumOfSubLoop is 1, we don't have to multiply, just add
                    int idxReg = getReg(idx, mbb, insertPos++);
                    nextReg = makeAnotherReg(Type.Int32Ty);
                    bmi(mbb, insertPos++, X86InstrNames.ADDrr32, 2, nextReg).
                            addReg(baseReg).addReg(idxReg);
                }
                else
                {
                    // normal slow case, the idx is not a constant and eltSize
                    // is not one.
                    int idxReg = getReg(idx, mbb, insertPos++);
                    int offsetReg = makeAnotherReg(Type.Int32Ty);
                    doMultiplyConst(insertPos++, offsetReg, Type.Int32Ty, idxReg, eltSize);

                    // emit a ADD to add offsetReg to baseReg.
                    nextReg = makeAnotherReg(Type.Int32Ty);
                    bmi(mbb, insertPos++, X86InstrNames.ADDrr32, 2, nextReg).
                            addReg(baseReg).addReg(offsetReg);
                }
            }

            baseReg = nextReg;
        }

        // After we processed all indices, the result is left in baseReg.
        // Move it to destReg where we were expected to put the result.
        bmi(mbb, insertPos++, X86InstrNames.MOVrr32, 1, destReg).addReg(baseReg);
    }

    private void handleTrunc(int srcVT, int destVT, int srcReg, int destReg)
    {
        assert srcVT > destVT && srcVT < f32;

        if (srcVT == i64 && destVT == i32)
        {
            buildMI(mbb, X86InstrNames.MOVrr32, 1, destReg).addReg(srcReg);
            return;
        }
        int regRegMove[] = { X86InstrNames.MOVrr8, X86InstrNames.MOVrr16,
                X86InstrNames.MOVrr32, X86InstrNames.FpMOV,
                X86InstrNames.MOVrr32 };

        // Handle cast of LARGER int to SMALLER int using a move to EAX followed by a
        // move out of AX or AL.
        int areg[] = { X86RegNames.AL, X86RegNames.AX, X86RegNames.EAX, 0, X86RegNames.EAX };
        buildMI(mbb, regRegMove[srcVT], 1, areg[srcVT]).addReg(srcReg);
        buildMI(mbb, regRegMove[destVT], 1, areg[destVT]).addReg(areg[destReg]);
    }

    private void handleZExt(int srcVT, int destVT, int srcReg, int destReg)
    {
        assert  srcVT < destVT && destVT < f32;

        boolean isLong = destVT == i64;
        if (isLong) destVT = i32;

        int[] opcodes = { X86InstrNames.MOVZXr16r8,
                X86InstrNames.MOVZXr32r8,
                X86InstrNames.MOVZXr32r16,
                X86InstrNames.MOVrr32};

        buildMI(mbb, opcodes[srcVT + destVT - 1], 1, destReg).addReg(srcReg);
        if (isLong)
            buildMI(mbb, X86InstrNames.SHRir32, 1, destReg+1).
                    addReg(destReg).addZImm(31);
    }

    private void handleSExt(int srcVT, int destVT, int srcReg, int destReg)
    {
        assert srcVT < destVT && destVT < f32;

        boolean isLong = destVT == i64;
        if (isLong)
            destVT = i32;

        int[] opcodes = { X86InstrNames.MOVSXr16r8,
                X86InstrNames.MOVSXr32r8, X86InstrNames.MOVSXr32r16,
                X86InstrNames.MOVrr32 };

        buildMI(mbb, opcodes[srcVT + destVT - 1], 1, destReg).addReg(srcReg);
        if (isLong)
            buildMI(mbb, X86InstrNames.SARir32, 1, destReg + 1).
                    addReg(destReg).addZImm(31);
    }

    private void emitCastOperation(int idx,
            Value op0,
            Operator opcode,
            Type destTy,
            int destReg)
    {
        int srcVT = getClass(op0.getType());
        int destVT = getClass(destTy);

        int srcReg = getReg(op0);
        if (srcReg == 0)
            return;

       switch (opcode)
       {
           default:
               assert false : "Unknown cast operator!";
               break;
           case Trunc:
                handleTrunc(srcVT, destVT, srcReg, destReg);
                break;
           case SExt:
               handleSExt(srcVT, destVT, srcReg, destReg);
               break;
           case ZExt:
                handleZExt(srcVT, destVT, srcReg, destReg);
                break;
           case SIToFP:
           case UIToFP:
               handleIntToFP(srcReg, destReg, srcVT, destVT, op0.getType());
                break;
           case FPExt:
               handleFPExt(srcVT, destVT, srcReg, destReg);
               break;
           case FPTrunc:
               handleFPTrunc(srcVT, destVT, srcReg, destReg);
               break;
           case FPToSI:
           case FPToUI:
               handleFPToInt(srcReg, destReg, srcVT, destVT, destTy);
               break;
           case IntToPtr:
           case PtrToInt:
               if (srcVT < destVT)
                   handleZExt(srcVT, destVT, srcReg, destReg);
               if (srcVT > destVT)
                   handleTrunc(srcVT, destVT, srcReg, destReg);
               break;
           case BitCast:
               handleBitCast(srcVT, destVT, srcReg, destReg);
               break;
       }
    }

    /**
     * Implement simple binary operators for integral types.
     * {@code operatorClass} is one of: 0 for Add, 1 for Sub, 2 for And, 3 for
     * Or, 4 for Xor.
     *
     * @param insertPos
     * @param op0
     * @param op1
     * @param operatorClass
     * @param destReg
     */
    private void emitSimpleBinaryOperation(int insertPos, Value op0, Value op1,
            int operatorClass, int destReg)
    {
        int klass = getClass(op0.getType());
        // Before handling common cass, there are many special cases we should
        // handled for improving performance.

        // sub 0, X ---> neg X.
        if (operatorClass == 1 && klass != i64)
        {
            if (op0 instanceof ConstantInt)
            {
                ConstantInt ci = (ConstantInt) op0;
                if (ci.isNullValue())
                {
                    int op1Reg = getReg(op1, mbb, insertPos++);
                    assert klass >= i8 && klass
                            <= i32 : "Unknown type class for this function";
                    int[] opcode = { X86InstrNames.NEGr8, X86InstrNames.NEGr16,
                            X86InstrNames.NEGr32 };
                    bmi(mbb, insertPos, opcode[klass], 1, destReg).addReg(op1Reg);
                    return;
                }
            }
        }

        if (!(op1 instanceof ConstantInt) || klass == i64)
        {
            int[][] opcodeTab = {
                    // Arithmetic operators
                    { X86InstrNames.ADDrr8, X86InstrNames.ADDrr16, X86InstrNames.ADDrr32, X86InstrNames.FpADD },
                    // ADD
                    { X86InstrNames.SUBrr8, X86InstrNames.SUBrr16, X86InstrNames.SUBrr32, X86InstrNames.FpSUB },
                    // SUB

                    // Bitwise operators
                    { X86InstrNames.ANDrr8, X86InstrNames.ANDrr16, X86InstrNames.ANDrr32, 0 },  // AND
                    { X86InstrNames.ORrr8, X86InstrNames.ORrr16, X86InstrNames.ORrr32, 0 },  // OR
                    { X86InstrNames.XORrr8, X86InstrNames.XORrr16, X86InstrNames.XORrr32, 0 },  // XOR
            };

            boolean isLong = false;
            if (klass == i64)
            {
                isLong = true;
                klass = i32;
            }

            int opcode = opcodeTab[operatorClass][klass];
            assert opcode != 0 : "Floating point argument to logical inst?";
            int op1Reg = getReg(op0, mbb, insertPos++);
            int op2Reg = getReg(op1, mbb, insertPos++);
            bmi(mbb, insertPos++, opcode, 2, destReg).addReg(op1Reg).addReg(op2Reg);

            if (isLong)
            {
                // Handle the upper 32 bits of long values...
                int[] topTab = { X86InstrNames.ADCrr32, X86InstrNames.SBBrr32,
                        X86InstrNames.ANDrr32, X86InstrNames.ORrr32,
                        X86InstrNames.XORrr32 };
                bmi(mbb, insertPos++, topTab[operatorClass], 2, destReg + 1).
                        addReg(op1Reg + 1).addReg(op2Reg + 1);
            }
            return;
        }

        // Special case: op Reg <const> and type is <=i32.
        ConstantInt ci1 = (ConstantInt) op1;
        int op0Reg = getReg(op0, mbb, insertPos++);

        // xor X, -1 --> not X.
        if (operatorClass == 4 && ci1.isAllOnesValue())
        {
            int[] notTab = { X86InstrNames.NOTr8, X86InstrNames.NOTr16,
                    X86InstrNames.NOTr32 };
            bmi(mbb, insertPos, notTab[klass], 1, destReg).addReg(op0Reg);
            return;
        }

        // add X, -1 --> dec X.
        if (operatorClass == 0 && ci1.isAllOnesValue())
        {
            int[] decTab = { X86InstrNames.DECr8, X86InstrNames.DECr16,
                    X86InstrNames.DECr32 };
            bmi(mbb, insertPos, decTab[klass], 1, destReg).addReg(op0Reg);
            return;
        }

        // add X, 1 --> inc X.
        if (operatorClass == 0 && ci1.equalsInt(1))
        {
            int[] incTab = { X86InstrNames.INCr8, X86InstrNames.INCr16,
                    X86InstrNames.INCr32 };
            bmi(mbb, insertPos, incTab[klass], 1, destReg).addReg(op0Reg);
            return;
        }

        int[][] opcodeTab = {
                // Arithmetic operators
                { X86InstrNames.ADDri8, X86InstrNames.ADDri16, X86InstrNames.ADDri32 },  // ADD
                { X86InstrNames.SUBri8, X86InstrNames.SUBri16, X86InstrNames.SUBri32 },  // SUB

                // Bitwise operators
                { X86InstrNames.ANDri8, X86InstrNames.ANDri16, X86InstrNames.ANDri32 },  // AND
                { X86InstrNames.ORri8, X86InstrNames.ORri16, X86InstrNames.ORri32 },  // OR
                { X86InstrNames.XORri8, X86InstrNames.XORri16, X86InstrNames.XORri32 },  // XOR
        };

        assert klass < 3 : "Invalid TypeClass in emitSimpleBinaryOperation";
        assert operatorClass < 5;
        int opcode = opcodeTab[operatorClass][klass];
        long op1Const = ci1.getZExtValue();
        bmi(mbb, insertPos, opcode, 2, destReg).addReg(op0Reg).addZImm(op1Const);
    }

    /**
     * Emits instruction used for turn a narrow operand into 32-bit wide operand
     * , and move it into destination register.
     *
     * @param destReg
     * @param arg
     */
    private void promote32(int destReg, ValueRecord arg)
    {
        int[][] extendOp = {
                // int extended mov.
                { X86InstrNames.MOVZXr32r8, X86InstrNames.MOVZXr32r16,
                        X86InstrNames.MOVrr32 },
                // signed extended mov (there is no difference regardless of int).
                { X86InstrNames.MOVSXr32r8, X86InstrNames.MOVSXr32r16,
                        X86InstrNames.MOVrr32 } };

        int isUnsigned = arg.ty.isUnsigned() ? 0 : 1;
        // make sure we have a virtual register number for this arg.
        int reg = arg.val != null ? getReg(arg.val) : arg.reg;

        int klass = getClass(arg.ty);
        assert klass >= i8 && klass <= i32 : "Unpromotable operand class";

        buildMI(mbb, extendOp[isUnsigned][klass], 1, destReg).addReg(reg);
    }

    /**
     * This emits an abstract call instruction, setting up the arguments and
     * the return value as appropriate.
     *
     * @param res
     * @param called
     * @param args
     */
    private void doCall(ValueRecord res, MachineInstr called, ArrayList<ValueRecord> args)
    {
        // count how many number of bytes will be pushed onto stack.
        int numBytes = 0;

        // Allocate the frame stack space for incoming arguments.
        if (!args.isEmpty())
        {
            for (ValueRecord arg : args)
            {
                switch (getClass(arg.ty))
                {
                    case i8:
                    case i16:
                    case i32:
                    case f32:
                        numBytes += 4;
                        break;
                    case i64:
                    case f64:
                        numBytes += 8;
                        break;
                    default:
                        assert false : "Unknown type class!";
                        return;
                }
            }

            // adjust stack pointer for incoming arguments.
            buildMI(mbb, X86InstrNames.ADJCALLSTACKDOWN, 1).addZImm(numBytes);

            // Arguments go on the stack in reverse order, as specified by the ABI.
            int argOffset = 0;
            for (int i = args.size() - 1; i >= 0; i--)
            {
                ValueRecord arg = args.get(i);
                switch (getClass(arg.ty))
                {
                    case i8:
                        if (arg.val != null && arg.val instanceof ConstantInt)
                        {
                            long val = ((ConstantInt) arg.val).getZExtValue();
                            addRegOffset(buildMI(mbb, X86InstrNames.MOVim32, 5),
                                    X86RegNames.ESP, argOffset).
                                    addZImm(val & 0xFF);
                            break;
                        }
                        // Fall through.
                    case i16:
                    {
                        if (arg.val != null && arg.val instanceof ConstantInt)
                        {
                            long val = ((ConstantInt) arg.val).getZExtValue();
                            addRegOffset(buildMI(mbb, X86InstrNames.MOVim32, 5),
                                    X86RegNames.ESP, argOffset).
                                    addZImm(val & 0xFFFF);
                        }
                        else
                        {
                            // Promote arg to 32 bits wide into a temporary register...
                            int r = makeAnotherReg(Type.Int32Ty);
                            promote32(r, arg);
                            addRegOffset(buildMI(mbb, X86InstrNames.MOVrm32, 5),
                                    X86RegNames.ESP, argOffset).addReg(r);
                        }
                        break;
                    }
                    case i32:
                        if (arg.val != null && arg.val instanceof ConstantInt)
                        {
                            long val = ((ConstantInt) arg.val).getZExtValue();
                            addRegOffset(buildMI(mbb, X86InstrNames.MOVim32, 5),
                                    X86RegNames.ESP, argOffset).
                                    addZImm(val);
                        }
                        else if (arg.val != null && arg.val instanceof ConstantPointerNull)
                        {
                            addRegOffset(buildMI(mbb, X86InstrNames.MOVim32, 5),
                                    X86RegNames.ESP, argOffset).
                                    addZImm(0);
                        }
                        else
                        {
                            int argReg = arg.val != null ? getReg(arg.val) : arg.reg;
                            addRegOffset(buildMI(mbb, X86InstrNames.MOVrm32, 5),
                                    X86RegNames.ESP, argOffset).addReg(argReg);
                        }
                        break;
                    case i64:
                        if (arg.val != null && arg.val instanceof ConstantInt)
                        {
                            long val = ((ConstantInt) arg.val).getZExtValue();
                            addRegOffset(buildMI(mbb, X86InstrNames.MOVim32, 5),
                                    X86RegNames.ESP, argOffset).
                                    addZImm((int) val);
                            addRegOffset(buildMI(mbb, X86InstrNames.MOVim32, 5),
                                    X86RegNames.ESP, argOffset + 4).
                                    addZImm(val >> 32);
                        }
                        else
                        {
                            int argReg = arg.val != null ? getReg(arg.val) : arg.reg;
                            addRegOffset(buildMI(mbb, X86InstrNames.MOVrm32, 5),
                                    X86RegNames.ESP, argOffset).addReg(argReg);
                            addRegOffset(buildMI(mbb, X86InstrNames.MOVrm32, 5),
                                    X86RegNames.ESP, argOffset + 4)
                                    .addReg(argReg + 1);
                        }
                        argOffset += 4;
                        break;
                    case f32:
                    {
                        int argReg = arg.val != null ? getReg(arg.val) : arg.reg;
                        addRegOffset(buildMI(mbb, X86InstrNames.FST32m, 5),
                                X86RegNames.ESP, argOffset).addReg(argReg);
                        break;
                    }
                    case f64:
                    {
                        int argReg = arg.val != null ? getReg(arg.val) : arg.reg;
                        addRegOffset(buildMI(mbb, X86InstrNames.FST64m, 5),
                                X86RegNames.ESP, argOffset).addReg(argReg);
                        argOffset += 4;
                        break;
                    }
                    default:
                        assert false : "Unknown class!";
                }
                argOffset += 4;
            }
        }
        else
        {
            buildMI(mbb, X86InstrNames.ADJCALLSTACKDOWN, 1).addZImm(0);
        }

        mbb.addLast(called);
        // after calling to called. restore stack frame.
        buildMI(mbb, X86InstrNames.ADJCALLSTACKUP, 1).addZImm(numBytes);

        // If there is a return value, obtain the returned value from %al/%ax/%eax
        // register.
        if (res.ty != Type.VoidTy)
        {
            int destClass = getClass(res.ty);
            switch (destClass)
            {
                case i8:
                case i16:
                case i32:
                {
                    // integer results are in %eax.
                    int[] retRegMove = { X86InstrNames.MOVrr8, X86InstrNames.MOVrr16,
                            X86InstrNames.MOVrr32 };
                    int[] retRegs = { X86RegNames.AL, X86RegNames.AX,
                            X86RegNames.EAX };
                    buildMI(mbb, retRegMove[destClass], 1, res.reg).addReg(retRegs[destClass]);
                    break;
                }
                case f32:
                case f64:
                {
                    // floating-point return instruction.
                    buildMI(mbb, X86InstrNames.FpGETRESULT, 1, res.reg);
                    break;
                }
                case i64:
                {
                    buildMI(mbb, X86InstrNames.MOVrr32, 1, res.reg).addReg(X86RegNames.EAX);
                    buildMI(mbb, X86InstrNames.MOVrr32, 1, res.reg + 1).addReg(X86RegNames.EDX);
                    break;
                }
                default:
                    assert false : "Unknown type class";
            }
        }
    }

    private void emitDivRemOperation(int idx, int op1Reg, int op2Reg, Type ty,
            boolean isDiv, int destReg)
    {
        int klass = getClass(ty);
        // First, handle float-pointing and long integral number.
        switch (klass)
        {
            case f32:
            case f64:
            case f80:
            {
                if (isDiv)
                {
                    buildMI(mbb, X86InstrNames.FpDIV, 2, destReg).addReg(op1Reg)
                            .addReg(op2Reg);
                }
                else
                {
                    MachineInstr call = buildMI(X86InstrNames.CALLpcrel32, 1)
                            .addExternalSymbol("fmod", true).getMInstr();
                    ArrayList<ValueRecord> args = new ArrayList<>();
                    args.add(new ValueRecord(op1Reg, Type.DoubleTy));
                    args.add(new ValueRecord(op2Reg, Type.DoubleTy));
                    doCall(new ValueRecord(destReg, Type.DoubleTy), call, args);
                }
                return;
            }
            case i64:
            {
                String[] fnName = { "__moddi3", "__divdi3", "__umoddi3",
                        "__udivdi3" };
                int nameIdx = (ty.isUnsigned() ? 1 : 0) * 2 + (isDiv ? 1 : 0);
                MachineInstr call = buildMI(X86InstrNames.CALLpcrel32, 1)
                        .addExternalSymbol(fnName[nameIdx], true).getMInstr();
                ArrayList<ValueRecord> args = new ArrayList<>();
                args.add(new ValueRecord(op1Reg, Type.Int64Ty));
                args.add(new ValueRecord(op2Reg, Type.Int64Ty));
                doCall(new ValueRecord(destReg, Type.Int64Ty), call, args);
                return;
            }
            case i8:
            case i16:
            case i32:
                break; // small integer, handled below.
            default:
                assert false : "Unkown type class";
        }

        // Second, handle i8, i16, i32 integer.
        int regs[] = { X86RegNames.AL, X86RegNames.AX, X86RegNames.EAX };
        int movOpcode[] = { X86InstrNames.MOVrr8, X86InstrNames.MOVrr16,
                X86InstrNames.MOVrr32 };
        int sarOpcode[] = { X86InstrNames.SARir8, X86InstrNames.SARir16,
                X86InstrNames.SARir32 };
        int clrOpcode[] = { X86InstrNames.XORrr8, X86InstrNames.XORrr16,
                X86InstrNames.XORrr32 };
        int extRegs[] = { X86RegNames.AH, X86RegNames.DX, X86RegNames.EDX };

        int[][] divOpecode = {
                // int division.
                { X86InstrInfo.DIVr8, X86InstrInfo.DIVr16, X86InstrInfo.DIVr32 },
                // signed disision.
                { X86InstrInfo.IDIVr8, X86InstrInfo.IDIVr16,
                        X86InstrInfo.IDIVr32 } };

        boolean isSigned = ty.isSigned();
        int reg = regs[klass];
        int extReg = extRegs[klass];

        // Put the first operand into AL/AX/EAX.
        buildMI(mbb, movOpcode[klass], 1, reg).addReg(op1Reg);

        if (isSigned)
        {
            // emit a sign extension instruction.
            int shiftResult = makeAnotherReg(ty);
            buildMI(mbb, sarOpcode[klass], 2, shiftResult).addReg(op1Reg).addZImm(31);
            buildMI(mbb, movOpcode[klass], 1, extReg).addReg(shiftResult);
        }
        else
        {
            // If int, emit a zeroing instruction.
            buildMI(mbb, clrOpcode[klass], 2, extReg).addReg(extReg).addReg(extReg);
        }

        // Here, the first operand is preparing.
        buildMI(mbb, divOpecode[isSigned ? 1 : 0][klass], 1).addReg(op2Reg);

        // for holding the result depending on it is division or remainder.
        int tmpReg = isDiv ? reg : extReg;

        buildMI(mbb, movOpcode[klass], 1, destReg).addReg(tmpReg);
    }

    private void doMultiplyConst(int insertPos, int destReg, Type ty, int op1Reg, int consOp2)
    {
        int klass = getClass(ty);
        if (Util.isPowerOf2(consOp2))
        {
            int shift = Util.log2(consOp2);
            switch (klass)
            {
                default:
                    assert false : "Unknown class for this function!";
                case TypeClass.i8:
                case TypeClass.i16:
                case TypeClass.i32:
                    bmi(mbb, insertPos, X86InstrNames.SHLir32, 2, destReg).
                            addReg(op1Reg).addZImm(consOp2);
                    return;
            }
        }

        if (klass == TypeClass.i16)
        {
            bmi(mbb, insertPos, X86InstrNames.IMULri16, 2, destReg).
                    addReg(op1Reg).addZImm(consOp2);
            return;
        }
        else if (klass == TypeClass.i32)
        {
            bmi(mbb, insertPos, X86InstrNames.IMULri32, 2, destReg).
                    addReg(op1Reg).addZImm(consOp2);
            return;
        }

        // Most general case, emit a normal multiply.
        int[] tab = { X86InstrNames.MOVir8, X86InstrNames.MOVir16,
                X86InstrNames.MOVir32 };

        int tempReg = makeAnotherReg(ty);
        bmi(mbb, insertPos, tab[klass], 1, tempReg).addZImm(consOp2);

        doMultiply(insertPos, destReg, ty, op1Reg, tempReg);
    }

    private void doMultiply(int insertPos, int destReg, Type destTy, int op1Reg,
            int op2Reg)
    {
        int klass = getClass(destTy);
        switch (klass)
        {
            case f32:
            case f64:
            case f80:
                bmi(mbb, insertPos, X86InstrNames.FpMUL, 2, destReg).
                        addReg(op1Reg).addReg(op2Reg);
                return;
            case i32:
                bmi(mbb, insertPos, X86InstrNames.IMULrr32, 2, destReg).
                        addReg(op1Reg).addReg(op2Reg);
                return;
            case i16:
                bmi(mbb, insertPos, X86InstrNames.IMULrr16, 2, destReg).
                        addReg(op1Reg).addReg(op2Reg);
                return;
            case i8:
                // Must use the MUL instruction, which forces use of AL...
                bmi(mbb, insertPos++, X86InstrNames.MOVrr8, 1, X86RegNames.AL).
                        addReg(op1Reg);
                bmi(mbb, insertPos++, X86InstrNames.MULr8, 1).addReg(op2Reg);
                bmi(mbb, insertPos++, X86InstrNames.MOVrr8, 1, destReg).
                        addReg(X86RegNames.AL);
                return;
            default:
            case i64:
                assert false : "doMultiply can not operates by long";
        }
    }

    /**
     * Generates the instruction required for moving constant into specified
     * virtual register.
     *
     * @param c
     * @param reg
     */
    private void copyConstToReg(int idx, Constant c, int reg)
    {
        if (c instanceof ConstantExpr)
        {
            int category = 0;
            ConstantExpr ce = (ConstantExpr) c;
            switch (ce.getOpCode())
            {
                case GetElementPtr:
                    emitGEPOperation(idx, ce, ce.getUseList(), reg);
                    return;
                case Trunc:
                case SExt:
                case ZExt:
                case SIToFP:
                case UIToFP:
                case FPExt:
                case FPTrunc:
                case FPToSI:
                case FPToUI:
                case IntToPtr:
                case PtrToInt:
                case BitCast:
                    emitCastOperation(idx, ce.operand(0), ce.getOpCode(), ce.getType(), reg);
                    return;
                case Xor:
                    category++;
                case Or:
                    category++;
                case And:
                    category++;
                case Sub:
                case FSub:
                    category++;
                case Add:
                case FAdd:
                    emitSimpleBinaryOperation(idx, ce.operand(0), ce.operand(1),
                            category, reg);
                    return;
                case Mul:
                case FMul:
                {
                    int op1Reg = getReg(ce.operand(0), mbb, idx);
                    int op2Reg = getReg(ce.operand(1), mbb, idx);
                    doMultiply(idx, reg, ce.getType(), op1Reg, op2Reg);
                    return;
                }
                case UDiv:
                case SDiv:
                case URem:
                case SRem:
                case FDiv:
                {
                    int op1Reg = getReg(ce.operand(0), mbb, idx);
                    int op2Reg = getReg(ce.operand(1), mbb, idx);
                    boolean isDiv = ce.getOpCode() == Operator.UDiv
                            || ce.getOpCode() == Operator.SDiv
                            || ce.getOpCode() == Operator.FDiv;
                    emitDivRemOperation(idx, op1Reg, op2Reg, ce.getType(), isDiv, reg);
                    return;
                }
                default:
                    assert false : "Unknown constant expr!";
            }
        }

        if (c.getType().isIntegerType())
        {
            int klass = getClass(c.getType());
            if (klass == i64)
            {
                // Copy the value into the register pair.
                long val = ((ConstantInt) (c)).getZExtValue();
                bmi(mbb, idx++, X86InstrNames.MOVir32, 1, reg).addZImm(val & 0xFFFFFFF);
                bmi(mbb, idx++, X86InstrNames.MOVir32, 1, reg + 1)
                        .addZImm(val >> 32);
                return;
            }

            assert klass <= TypeClass.i32 : "Invalid type class!";

            int integralOpcodeTab[] = { X86InstrNames.MOVir8, X86InstrNames.MOVir16, X86InstrNames.MOVir32 };

            ConstantInt ci = (ConstantInt) c;
            bmi(mbb, idx++, integralOpcodeTab[klass], 1, reg).addZImm(ci.getZExtValue());
        }
        else if (c instanceof ConstantFP)
        {
            ConstantFP cfp = (ConstantFP) c;
            double val = cfp.getValue();
            if (val == +0.0)
                bmi(mbb, idx++, X86InstrNames.FLD0, 0, reg);
            else if (val == +1.0)
                bmi(mbb, idx++, X86InstrNames.FLD1, 0, reg);
            else
            {
                // Otherwise, we need to spill it into constant pool.
                MachineConstantPool pool = mf.getConstantPool();
                int cpi = pool.getConstantPoolIndex(cfp);
                Type ty = cfp.getType();
                assert ty == Type.FloatTy || ty == Type.DoubleTy;
                int i = ty == Type.FloatTy ? 0 : 1;
                int[] ops = { X86InstrNames.FLDr32, X86InstrNames.FLDr64 };
                addConstantPoolReference(bmi(mbb, idx++, ops[i], 4, reg), cpi, 0);
            }
        }
        else if (c instanceof ConstantPointerNull)
        {
            // Copy zero (null pointer) to the register.
            bmi(mbb, idx++, X86InstrNames.MOVir32, 1, reg).addZImm(0);
        }
        else
        {
            System.err.println("Illegal constant:" + c);
            assert false : "Type not handled yet!";
        }
    }

    /**
     * This method is similar to getReg(Value val), but the only one
     * difference is that there are additional instructions maybe
     * appended into the end of mbb.
     *
     * @param val
     * @param mbb
     * @param idx
     * @return
     */
    private int getReg(Value val, MachineBasicBlock mbb, int idx)
    {
        int reg;
        if (!regMap.containsKey(val))
        {
            reg = makeAnotherReg(val.getType());
            regMap.put(val, reg);
        }
        else
            reg = regMap.get(val);

        // if this value is a constant, emit the code to copy the constant
        // into register.
        if (val instanceof GlobalValue)
        {
            // move the address of global value into the register.
            bmi(mbb, idx, X86InstrNames.MOVir32, 1, reg).
                    addGlobalAddress((GlobalValue) val, false).getMInstr();
        }
        else if (val instanceof Constant)
        {
            copyConstToReg(idx, (Constant) val, reg);
            // remove this constant since it is moved to register.
            regMap.remove(val);
        }
        return reg;
    }

    /**
     * This method used for turning HIR value to virtual register number.
     *
     * @param val
     * @return
     */
    private int getReg(Value val)
    {
        return getReg(val, mbb, mbb.size() - 1);
    }

    private static int getClass(Type ty)
    {
        switch (ty.getTypeID())
        {
            case Type.IntegerTyID:
            case Type.IntegerTyID:
                return TypeClass.i8;
            case Type.IntegerTyID:
                return TypeClass.i16;
            case Type.IntegerTyID:
                return TypeClass.i32;
            case Type.IntegerTyID:
                return i64;
            case Type.FloatTyID:
                return TypeClass.f32;
            case Type.DoubleTyID:
                return TypeClass.f64;
            default:
                assert false : "Invalid type to getClass()!";
                return TypeClass.i32;  // default to i32.
        }
    }

    private void assignArgumentsWithVirtualReg(Function f)
    {
        // emit instructions to load the arguments...  On entry to a function on the
        // X86, the stack frame looks like this:
        //
        // [ESP] -- return address
        // [ESP + 4] -- first argument (leftmost lexically)
        // [ESP + 8] -- second argument, if first argument is four bytes in getNumOfSubLoop

        int argOffset = 0;
        MachineFrameInfo mfi = mf.getFrameInfo();

        for (Argument arg : f.getArgumentList())
        {
            int reg = getReg(arg);
            int fi;
            int typeSize = (int) tm.getTargetData().getTypeSize(arg.getType());
            fi = mfi.createFixedObject(typeSize, argOffset);

            switch (getClass(arg.getType()))
            {
                case TypeClass.i8:
                    addFrameReference(buildMI(mbb, X86InstrNames.MOVmr8, 4, reg), fi);
                    break;
                case TypeClass.i16:
                    addFrameReference(buildMI(mbb, X86InstrNames.MOVmr16, 4, reg), fi);
                    break;
                case TypeClass.i32:
                    addFrameReference(buildMI(mbb, X86InstrNames.MOVmr32, 4, reg), fi);
                    break;
                case TypeClass.i64:
                    addFrameReference(buildMI(mbb, X86InstrNames.MOVmr32, 4, reg), fi);
                    addFrameReference(
                            buildMI(mbb, X86InstrNames.MOVmr32, 4, reg + 1), fi,
                            4);
                    break;
                case TypeClass.f32:
                    addFrameReference(buildMI(mbb, X86InstrNames.FLDr32, 4, reg), fi);
                    break;
                case TypeClass.f64:
                    addFrameReference(buildMI(mbb, X86InstrNames.FLDr64, 4, reg), fi);
                    argOffset += 4;
                    break;
                default:
                    assert false : "Unknown type class in assignArgumentsWithVirtualReg()";
                    break;
            }
            // advance argument offset to the next slot position.
            // Each argument takes at least 4 bytes on the stack.
            argOffset += 4;
        }
        if (f.getFunctionType().isVarArgs())
            varArgsFrameIndex = mfi.createFixedObject(1, argOffset);
    }

    /**
     * Insert machine code to generate phis.  This is tricky
     * because we have to generate our sources into the source basic blocks, not
     * the current one.
     */
    private void selectPhiNodes()
    {
        TargetInstrInfo targetInstrInfo = tm.getInstrInfo();
        Function f = mf.getFunction(); // obtains the HIR function.
        for (BasicBlock bb : f.getBasicBlockList())
        {
            MachineBasicBlock mbb = mbbMap.get(bb);

            // a index into a position offset head of mbb where all phi node
            // will be inserts.
            // We must ensure that all Phi nodes will be inserted prior to
            // other non-Phi instr.
            int numPhis = 0;
            for (int i = 0, e = bb.size();
                 i < e && (bb.getInstAt(i) instanceof PhiNode); i++)
            {
                PhiNode phiNode = (PhiNode) bb.getInstAt(i);
                // creates a machine phi node and insert it into mbb.
                int phiReg = getReg(phiNode);
                MachineInstr phiMI = buildMI(X86InstrNames.PHI, phiNode.getNumOfOperands(),
                        phiReg).getMInstr();
                mbb.insert(numPhis++, phiMI);

                // special handling for typed of i64.
                MachineInstr longPhiMI = null;
                if (phiNode.getType() == Type.Int64Ty)
                {
                    longPhiMI = buildMI(X86InstrNames.PHI, phiNode.getNumOfOperands(),
                            phiReg + 1).getMInstr();
                    mbb.insert(numPhis++, longPhiMI);
                }

                // with a hashmap for mapping BasicBlock into virtual register
                // number, so that we are only initialize one incoming value
                // for a particular block even if the block has multiply entries
                // int Phi node.
                HashMap<MachineBasicBlock, Integer> phiValues = new HashMap<>();
                for (int j = 0, size = phiNode.getNumberIncomingValues();
                     j < size; j++)
                {
                    BasicBlock incomingBB = phiNode.getIncomingBlock(i);
                    MachineBasicBlock predMBB = mbbMap.get(incomingBB);

                    int valReg = 0;
                    if (phiValues.containsKey(predMBB))
                    {
                        // we already inserted an initialization of the register
                        // for this predecessor.
                        valReg = phiValues.get(predMBB);
                    }
                    else
                    {
                        // compute a virtual register for incoming value,
                        // and insert it into phiValues.
                        Value incomingVal = phiNode.getIncomingValue(i);
                        if (incomingVal instanceof Constant)
                        {
                            // If the incomingVal is a constant, we should insert
                            // code in the predecessor to compute a virtual register.

                            // skip all machine phi nodes.
                            int k = 0;
                            for (int sz = predMBB.size(); k < sz
                                    && predMBB.getInstAt(k).getOpCode()
                                    == X86InstrNames.PHI; k++)
                                ;

                            valReg = getReg(incomingVal, predMBB, k);
                        }
                        else
                            valReg = getReg(incomingVal);

                        phiValues.put(predMBB, valReg);
                    }

                    phiMI.addRegOperand(valReg, UseType.Use);
                    phiMI.addMachineBasicBlockOperand(predMBB);
                    if (longPhiMI != null)
                    {
                        phiMI.addRegOperand(valReg + 1, UseType.Use);
                        phiMI.addMachineBasicBlockOperand(predMBB);
                    }
                }
            }
        }
    }

    private void visitSimpleBinary(Op2 inst, int operatorClass)
    {
        int destReg = getReg(inst);
        emitSimpleBinaryOperation(mbb.size() - 1, inst.operand(0), inst.operand(1), operatorClass, destReg);
    }

    private void visitDivRem(Op2 inst, boolean isDiv)
    {
        int op1Reg = getReg(inst.operand(0));
        int op2Reg = getReg(inst.operand(1));
        int resultReg = getReg(inst);

        emitDivRemOperation(mbb.size() - 1, op1Reg, op2Reg, inst.getType(),
                isDiv, resultReg);
    }

    /**
     * Here we are interested in meeting the x86 ABI.  As such,
     * we have the following possibilities:
     * <p>
     * ret void: No return value, simply emit a 'ret' instruction
     * ret sbyte, ubyte : Extend value into EAX and return
     * ret short, ushort: Extend value into EAX and return
     * ret int, uint    : Move value into EAX and return
     * ret pointer      : Move value into EAX and return
     * ret long, ulong  : Move value into EAX/EDX and return
     * ret float/double : Top of FP stack
     *
     * @param inst
     * @return
     */
    @Override public Void visitRet(Instruction.ReturnInst inst)
    {
        if (inst.getNumOfOperands() == 0)
        {
            // emit 'ret'.
            buildMI(mbb, X86InstrNames.RET, 0);
            return null;
        }

        Value retValue = inst.operand(0);
        int retReg = getReg(retValue);
        switch (getClass(retValue.getType()))
        {
            case i8:
            case i16:
            case i32:
                promote32(X86RegNames.EAX, new ValueRecord(retReg, retValue.getType()));
                // Declare that EAX are live on exit
                buildMI(mbb, X86InstrNames.IMPLICIT_USE, 2).addReg(X86RegNames.EAX).addReg(X86RegNames.ESP);
                break;
            case f32:
            case f64:
                buildMI(mbb, X86InstrNames.FpGETRESULT, 1).addReg(retReg);
                // Declare that ST0 are live on exit
                buildMI(mbb, X86InstrNames.IMPLICIT_USE, 2).
                        addReg(X86RegNames.ST0).addReg(X86RegNames.ESP);
                break;
            case i64:
                buildMI(mbb, X86InstrNames.MOVrr32, 1, X86RegNames.EAX).
                        addReg(retReg);
                buildMI(mbb, X86InstrNames.MOVrr32, 1, X86RegNames.EDX).
                        addReg(retReg + 1);
                // Declare that EAX & EDX are live on exit
                buildMI(mbb, X86InstrNames.IMPLICIT_USE, 3).
                        addReg(X86RegNames.EAX).
                        addReg(X86RegNames.EDX).
                        addReg(X86RegNames.ESP);
                break;
            default:
                assert false : "Invalid type class in ret";
        }
        // emit a 'ret' instruction.
        buildMI(mbb, X86InstrNames.RET, 0);
        return null;
    }

    /**
     * Return the basic block which occurs lexically after the
     * specified one.
     *
     * @param bb
     * @return
     */
    private static BasicBlock getBlockAfter(BasicBlock bb)
    {
        Iterator<BasicBlock> itr = bb.getParent().getBasicBlockList().iterator();
        while (itr.hasNext())
        {
            BasicBlock next = itr.next();
            if (next == bb)
            {
                return itr.hasNext() ? itr.next() : null;
            }
        }
        return null;
    }

    /**
     * Handle conditional and unconditional branches here.  Note that since code
     * layout is frozen at this point, that if we are trying to jump to a block
     * that is the immediate successor of the current block, we can just make a
     * fall-through (but we don't currently).
     *
     * @param bi
     * @return
     */
    @Override public Void visitBr(BranchInst bi)
    {
        // Obtains the basic where the branch instr resides.
        BasicBlock nextBB = getBlockAfter(bi.getParent());

        // Add the mbb successor.
        mbb.addSuccessor(mbbMap.get(bi.suxAt(0)));
        if (bi.isConditional())
            mbb.addSuccessor(mbbMap.get(bi.suxAt(1)));

        if (bi.isUnconditional())
        {
            if (bi.suxAt(0) != nextBB)
                buildMI(mbb, X86InstrNames.JMP, 1).
                        addMBB(mbbMap.get(bi.suxAt(0)));
            return null;
        }

        int condReg = getReg(bi.getCondition());
        buildMI(mbb, X86InstrNames.TESTrr8, 2).addReg(condReg).addReg(condReg);
        if (bi.suxAt(1) == nextBB)
        {
            if (bi.suxAt(0) != nextBB)
                buildMI(mbb, X86InstrNames.JNE, 1).addMBB(mbbMap.get(bi.suxAt(0)));
        }
        else
        {
            buildMI(mbb, X86InstrNames.JE, 1).addMBB(mbbMap.get(bi.suxAt(1)));

            if (bi.suxAt(0) != nextBB)
                buildMI(mbb, X86InstrNames.JMP, 1).addMBB(mbbMap.get(bi.suxAt(0)));
        }
        return null;
    }

    @Override public Void visitSwitch(SwitchInst inst)
    {
        assert false : "Switch inst have not been lowered into chained br as yet?";
        return null;
    }

    @Override public Void visitAdd(Op2 inst)
    {
        visitSimpleBinary(inst, 0);
        return null;
    }

    @Override public Void visitFAdd(Op2 inst)
    {
        // TODO
        int op0Reg = getReg(inst.operand(0));
        int op1Reg = getReg(inst.operand(1));
        int destReg = getReg(inst);

        int typeClass = getClass(inst.getType());
        assert typeClass >= f32
                && typeClass <= f64 : "The type class is not a fp?";
        buildMI(mbb, X86InstrNames.FpADD, 2, destReg).addReg(op0Reg).addReg(op1Reg);
        return null;
    }

    @Override public Void visitSub(Op2 inst)
    {
        visitSimpleBinary(inst, 1);
        return null;
    }

    @Override public Void visitFSub(Op2 inst)
    {
        // TODO
        int op0Reg = getReg(inst.operand(0));
        int op1Reg = getReg(inst.operand(1));
        int destReg = getReg(inst);

        int typeClass = getClass(inst.getType());
        assert typeClass >= f32
                && typeClass <= f80 : "The type class is not a fp?";
        buildMI(mbb, X86InstrNames.FpSUB, 2, destReg).addReg(op0Reg).addReg(op1Reg);
        return null;
    }

    @Override public Void visitMul(Op2 inst)
    {
        int op0Reg = getReg(inst.operand(0));
        int destReg = getReg(inst);

        if (inst.getType() != Type.Int64Ty)
        {
            // mul op0Reg, const.
            if (inst.operand(1) instanceof ConstantInt)
            {
                ConstantInt ci = (ConstantInt) inst.operand(1);
                // it must be a 32-bit integer.
                int val = (int) ci.getZExtValue();
                doMultiplyConst(mbb.size() - 1, destReg, inst.getType(), op0Reg,
                        val);
            }
            else
            {
                // mul op0Reg, op1Reg.
                int op1Reg = getReg(inst.operand(1));
                doMultiply(mbb.size() - 1, destReg, inst.getType(), op0Reg,
                        op1Reg);
                ;
            }
        }
        else
        {
            int op1Reg = getReg(inst.operand(1));

            // lower multiply.
            buildMI(mbb, X86InstrNames.MOVrr32, 1, X86RegNames.EAX).addReg(op0Reg);
            buildMI(mbb, X86InstrNames.MULr32, 1).addReg(op1Reg);

            int overflowReg = makeAnotherReg(Type.Int32Ty);
            buildMI(mbb, X86InstrNames.MOVrr32, 1, destReg).addReg(X86RegNames.EAX);
            buildMI(mbb, X86InstrNames.MOVrr32, 1, overflowReg).addReg(X86RegNames.EDX);

            int ahblReg = makeAnotherReg(Type.Int32Ty);
            bmi(mbb, mbb.size() - 1, X86InstrNames.IMULrr32, 2, ahblReg).
                    addReg(op0Reg + 1).addReg(op1Reg);

            int ahbPlusOverlowReg = makeAnotherReg(Type.Int32Ty);
            buildMI(mbb, X86InstrNames.ADDrr32, 2, ahbPlusOverlowReg).
                    addReg(ahblReg).addReg(overflowReg);

            int albHReg = makeAnotherReg(Type.Int32Ty);
            bmi(mbb, mbb.size() - 1, X86InstrNames.IMULrr32, 2, albHReg).
                    addReg(op0Reg).addReg(op1Reg + 1);

            buildMI(mbb, X86InstrNames.ADDrr32, 2, destReg + 1).
                    addReg(ahbPlusOverlowReg).addReg(albHReg);
        }
        return null;
    }

    @Override public Void visitFMul(Op2 inst)
    {
        int op0Reg = getReg(inst.operand(0));
        int op1Reg = getReg(inst.operand(1));
        int destReg = getReg(inst);
        doMultiply(mbb.size() - 1, destReg, inst.getType(), op0Reg, op1Reg);
        return null;
    }

    @Override public Void visitUDiv(Op2 inst)
    {
        visitDivRem(inst, true);
        return null;
    }

    @Override public Void visitSDiv(Op2 inst)
    {
        visitDivRem(inst, true);
        return null;
    }

    @Override public Void visitFDiv(Op2 inst)
    {
        visitDivRem(inst, true);
        return null;
    }

    @Override public Void visitURem(Op2 inst)
    {
        visitDivRem(inst, false);
        return null;
    }

    @Override public Void visitSRem(Op2 inst)
    {
        visitDivRem(inst, false);
        return null;
    }

    @Override public Void visitFRem(Op2 inst)
    {
        visitDivRem(inst, false);
        return null;
    }

    @Override public Void visitAnd(Op2 inst)
    {
        visitSimpleBinary(inst, 2);
        return null;
    }

    @Override public Void visitOr(Op2 inst)
    {
        visitSimpleBinary(inst, 3);
        return null;
    }

    @Override public Void visitXor(Op2 inst)
    {
        visitSimpleBinary(inst, 4);
        return null;
    }

    private void emitShfitOperator(Op2 inst, boolean isLeftShift)
    {
        int srcReg = getReg(inst.operand(0));
        int destReg = getReg(inst);
        boolean isSigned = inst.getType().isSigned();
        int klass = getClass(inst.getType());

        int[][] ConstantOperand = {
                { X86InstrNames.SHRir8, X86InstrNames.SHRir16, X86InstrNames.SHRir32, X86InstrNames.SHRDir32 },  // SHR
                { X86InstrNames.SARir8, X86InstrNames.SARir16, X86InstrNames.SARir32, X86InstrNames.SHRDir32 },  // SAR
                { X86InstrNames.SHLir8, X86InstrNames.SHLir16, X86InstrNames.SHLir32, X86InstrNames.SHLDir32 },  // SHL
                { X86InstrNames.SHLir8, X86InstrNames.SHLir16, X86InstrNames.SHLir32, X86InstrNames.SHLDir32 },
                // SAL = SHL
        };

        int[][] NonConstantOperand = { { X86InstrNames.SHRrCL8, X86InstrNames.SHRrCL16,
                X86InstrNames.SHRrCL32 },  // SHR
                { X86InstrNames.SARrCL8, X86InstrNames.SARrCL16,
                        X86InstrNames.SARrCL32 },  // SAR
                { X86InstrNames.SHLrCL8, X86InstrNames.SHLrCL16,
                        X86InstrNames.SHLrCL32 },  // SHL
                { X86InstrNames.SHLrCL8, X86InstrNames.SHLrCL16,
                        X86InstrNames.SHLrCL32 },  // SAL = SHL
        };

        // Longs, as usual, are handled specially...
        if (klass == i64)
        {
            // If we have a constant shift, we can generate much more efficient code
            // than otherwise...
            //
            if (inst.operand(1) instanceof ConstantInt)
            {
                ConstantInt cui = (ConstantInt) inst.operand(0);
                int Amount = (int) cui.getZExtValue();
                if (Amount < 32)
                {
                    int[] Opc = ConstantOperand[(isLeftShift ? 1 : 0) * 2 + (
                            isSigned ?
                                    1 :
                                    0)];
                    if (isLeftShift)
                    {
                        buildMI(mbb, Opc[3], 3, destReg + 1).addReg(srcReg + 1).addReg(srcReg).addZImm(Amount);
                        buildMI(mbb, Opc[2], 2, destReg).addReg(srcReg).addZImm(Amount);
                    }
                    else
                    {
                        buildMI(mbb, Opc[3], 3, destReg).addReg(srcReg)
                                .addReg(srcReg + 1).addZImm(Amount);
                        buildMI(mbb, Opc[2], 2, destReg + 1).addReg(srcReg + 1).addZImm(Amount);
                    }
                }
                else
                {                 // Shifting more than 32 bits
                    Amount -= 32;
                    if (isLeftShift)
                    {
                        buildMI(mbb, X86InstrNames.SHLir32, 2, destReg + 1).addReg(srcReg).addZImm(Amount);
                        buildMI(mbb, X86InstrNames.MOVir32, 1, destReg).addZImm(0);
                    }
                    else
                    {
                        int Opcode = isSigned ?
                                X86InstrNames.SARir32 :
                                X86InstrNames.SHRir32;
                        buildMI(mbb, Opcode, 2, destReg).addReg(srcReg + 1).addZImm(Amount);
                        buildMI(mbb, X86InstrNames.MOVir32, 1, destReg + 1).addZImm(0);
                    }
                }
            }
            else
            {
                int TmpReg = makeAnotherReg(Type.Int32Ty);

                if (!isLeftShift && isSigned)
                {
                    // If this is a SHR of a Long, then we need to do funny sign extension
                    // stuff.  TmpReg gets the value to use as the high-part if we are
                    // shifting more than 32 bits.
                    buildMI(mbb, X86InstrNames.SARir32, 2, TmpReg).addReg(srcReg).addZImm(31);
                }
                else
                {
                    // Other shifts use a fixed zero value if the shift is more than 32
                    // bits.
                    buildMI(mbb, X86InstrNames.MOVir32, 1, TmpReg).addZImm(0);
                }

                // Initialize CL with the shift amount...
                int ShiftAmount = getReg(inst.operand(1));
                buildMI(mbb, X86InstrNames.MOVrr8, 1, X86RegNames.CL).addReg(ShiftAmount);

                int TmpReg2 = makeAnotherReg(Type.Int32Ty);
                int TmpReg3 = makeAnotherReg(Type.Int32Ty);
                if (isLeftShift)
                {
                    // TmpReg2 = shld inHi, inLo
                    buildMI(mbb, X86InstrNames.SHLDrCL32, 2, TmpReg2)
                            .addReg(srcReg + 1).addReg(srcReg);
                    // TmpReg3 = shl  inLo, CL
                    buildMI(mbb, X86InstrNames.SHLrCL32, 1, TmpReg3).addReg(srcReg);

                    // Set the flags to indicate whether the shift was by more than 32 bits.
                    buildMI(mbb, X86InstrNames.TESTri8, 2).addReg(X86RegNames.CL).addZImm(32);

                    // DestHi = (>32) ? TmpReg3 : TmpReg2;
                    buildMI(mbb, X86InstrNames.CMOVNErr32, 2, destReg + 1).addReg(TmpReg2).addReg(TmpReg3);
                    // DestLo = (>32) ? TmpReg : TmpReg3;
                    buildMI(mbb, X86InstrNames.CMOVNErr32, 2, destReg).addReg(TmpReg3).addReg(TmpReg);
                }
                else
                {
                    // TmpReg2 = shrd inLo, inHi
                    buildMI(mbb, X86InstrNames.SHRDrCL32, 2, TmpReg2).addReg(srcReg).addReg(srcReg
                            + 1);
                    // TmpReg3 = s[ah]r  inHi, CL
                    buildMI(mbb, isSigned ?
                            X86InstrNames.SARrCL32 :
                            X86InstrNames.SHRrCL32, 1, TmpReg3)
                            .addReg(srcReg + 1);

                    // Set the flags to indicate whether the shift was by more than 32 bits.
                    buildMI(mbb, X86InstrNames.TESTri8, 2).addReg(X86RegNames.CL).addZImm(32);

                    // DestLo = (>32) ? TmpReg3 : TmpReg2;
                    buildMI(mbb, X86InstrNames.CMOVNErr32, 2, destReg).addReg(TmpReg2).addReg(TmpReg3);

                    // DestHi = (>32) ? TmpReg : TmpReg3;
                    buildMI(mbb, X86InstrNames.CMOVNErr32, 2, destReg + 1).addReg(TmpReg3).addReg(TmpReg);
                }
            }
            return;
        }

        if (inst.operand(1) instanceof ConstantInt)
        {
            ConstantInt cui = (ConstantInt) inst.operand(1);
            // The shift amount is constant, guaranteed to be a ubyte. Get its value.
            assert cui.getType() == Type.Int8Ty : "Shift amount not a ubyte?";

            int[] opc = ConstantOperand[(isLeftShift ? 1 : 0) * 2 + (isSigned ?
                    1 :
                    0)];
            buildMI(mbb, opc[klass], 2, destReg).addReg(srcReg).addZImm(cui.getZExtValue());
        }
        else
        {                  // The shift amount is non-constant.
            buildMI(mbb, X86InstrNames.MOVrr8, 1, X86RegNames.CL).addReg(getReg(inst.operand(1)));

            int[] opc = ConstantOperand[(isLeftShift ? 1 : 0) * 2 + (isSigned ?
                    1 :
                    0)];
            buildMI(mbb, opc[klass], 1, destReg).addReg(srcReg);
        }
    }

    @Override public Void visitShl(Op2 inst)
    {
        emitShfitOperator(inst, true);
        return null;
    }

    @Override public Void visitLShr(Op2 inst)
    {
        emitShfitOperator(inst, false);
        return null;
    }

    @Override public Void visitAShr(Op2 inst)
    {
        emitShfitOperator(inst, false);
        return null;
    }

    private int getSetCCNumber(CmpInst.Predicate pred)
    {
        assert pred.ordinal() >= FIRST_ICMP_PREDICATE.ordinal()
                && pred.ordinal() <= LAST_ICMP_PREDICATE
                .ordinal() : "Invalid predicate for ICmp instr";
        switch (pred)
        {
            case ICMP_EQ:
                return X86InstrNames.SETEr;
            case ICMP_NE:
                return X86InstrNames.SETNEr;
            case ICMP_UGT:
                return X86InstrNames.SETAr;
            case ICMP_UGE:
                return X86InstrNames.SETAEr;
            case ICMP_ULT:
                return X86InstrNames.SETBr;
            case ICMP_ULE:
                return X86InstrNames.SETBEr;
            case ICMP_SGT:
                return X86InstrNames.SETGr;
            case ICMP_SGE:
                return X86InstrNames.SETGEr;
            case ICMP_SLT:
                return X86InstrNames.SETLr;
            case ICMP_SLE:
                return X86InstrNames.SETLEr;
            default:
                return -1;
        }
    }

    private int chooseCmpImmediateOpcode(int vt, ConstantInt rhsc)
    {
        switch (vt)
        {
            default:
                return 0;
            case i8:
                return X86InstrNames.CMPri8;
            case i16:
                return X86InstrNames.CMPri16;
            case i32:
                return X86InstrNames.CMPri32;
            case i64:
                // 64-bit comparisons are only valid if the immediate fits in a 32-bit sext
                // field.
                if ((int) rhsc.getSExtValue() == rhsc.getSExtValue())
                    return X86InstrNames.CMP64ri32;
                return 0;
        }
    }

    private int chooseCmpOpcode(int vt)
    {
        switch (vt)
        {
            default:
                return 0;
            case i8:
                return X86InstrNames.CMPrr8;
            case i16:
                return X86InstrNames.CMPrr16;
            case i32:
                return X86InstrNames.CMPrr32;
            case i64:
                return X86InstrNames.CMP64rr;
            case f32:
                return X86InstrNames.UCOMPISSrr;
            case f64:
                return X86InstrNames.UCOMISDrr;
        }
    }

    private boolean emitCompare(Value op0, Value op1, int vt)
    {
        int op0Reg = getReg(op0);
        if (op0Reg == 0)
            return false;

        // Handle 'null' like ptr == NULL.
        if (op1 instanceof ConstantPointerNull)
        {
            op1 = Constant.getNullValue(tm.getTargetData().getIntPtrType());
        }

        if (op1 instanceof ConstantInt)
        {
            ConstantInt op1C = (ConstantInt) op1;
            int compareImmOpc = chooseCmpImmediateOpcode(vt, op1C);
            if (compareImmOpc != 0)
            {
                buildMI(mbb, compareImmOpc, 2).addReg(op0Reg).
                        addSImm(op1C.getSExtValue());
                return true;
            }
        }

        int compareOpc = chooseCmpOpcode(vt);
        if (compareOpc == 0)
            return false;

        int op1Reg = getReg(op1);
        if (op1Reg == 0)
            return false;
        buildMI(mbb, compareOpc, 2).addReg(op0Reg).addReg(op1Reg);
        return true;
    }

    private int createResultReg(TargetRegisterClass regClass)
    {
        return mf.getMachineRegisterInfo().createVirtualRegister(regClass);
    }

    private void emitCompare(CmpInst ci)
    {
        Type ty = ci.getType();
        int vt = getClass(ty);

        assert vt>=i8 && vt<=i64;

        int resultReg = getReg(ci);

        boolean swapArgs = false;
        int setCCOpc;
        switch (ci.getPredicate())
        {
            case FCMP_OEQ:
            {
                if (!emitCompare(ci.operand(0), ci.operand(1), vt))
                    return;

                int ereg = createResultReg(X86RegisterInfo.x86R8RegClass);
                int npreg = createResultReg(X86RegisterInfo.x86R8RegClass);
                buildMI(mbb, X86InstrNames.SETEr, ereg);
                buildMI(mbb, X86InstrNames.SETNPr, npreg);
                buildMI(mbb, X86InstrNames.ANDrr8, resultReg, 2).addReg(ereg).addReg(npreg);
                return;
            }
            case FCMP_UNE:
            {
                if (!emitCompare(ci.operand(0), ci.operand(1), vt))
                    return;

                int nereg = createResultReg(X86RegisterInfo.x86R8RegClass);
                int npreg = createResultReg(X86RegisterInfo.x86R8RegClass);
                buildMI(mbb, X86InstrNames.SETNEr, nereg);
                buildMI(mbb, X86InstrNames.SETPr, npreg);
                buildMI(mbb, X86InstrNames.ORrr8, resultReg, 2).addReg(nereg).addReg(npreg);
                return;
            }
            case FCMP_OGT: swapArgs = false; setCCOpc = X86InstrNames.SETAr; break;
            case FCMP_OGE: swapArgs = false; setCCOpc = X86InstrNames.SETAEr; break;
            case FCMP_OLT: swapArgs = true;  setCCOpc = X86InstrNames.SETAr;  break;
            case FCMP_OLE: swapArgs = true;  setCCOpc = X86InstrNames.SETAEr; break;
            case FCMP_ONE: swapArgs = false; setCCOpc = X86InstrNames.SETNEr; break;
            case FCMP_ORD: swapArgs = false; setCCOpc = X86InstrNames.SETNPr; break;
            case FCMP_UNO: swapArgs = false; setCCOpc = X86InstrNames.SETPr;  break;
            case FCMP_UEQ: swapArgs = false; setCCOpc = X86InstrNames.SETEr;  break;
            case FCMP_UGT: swapArgs = true;  setCCOpc = X86InstrNames.SETBr;  break;
            case FCMP_UGE: swapArgs = true;  setCCOpc = X86InstrNames.SETBEr; break;
            case FCMP_ULT: swapArgs = false; setCCOpc = X86InstrNames.SETBr;  break;
            case FCMP_ULE: swapArgs = false; setCCOpc = X86InstrNames.SETBEr; break;

            case ICMP_EQ: setCCOpc = X86InstrNames.SETEr; break;
            case ICMP_NE: setCCOpc = X86InstrNames.SETNEr; break;
            case ICMP_UGT: setCCOpc = X86InstrNames.SETAr; break;
            case ICMP_UGE: setCCOpc = X86InstrNames.SETAEr; break;
            case ICMP_ULT: setCCOpc = X86InstrNames.SETBr; break;
            case ICMP_ULE: setCCOpc = X86InstrNames.SETBEr; break;
            case ICMP_SGT: setCCOpc = X86InstrNames.SETGr; break;
            case ICMP_SGE: setCCOpc = X86InstrNames.SETGEr; break;
            case ICMP_SLT: setCCOpc = X86InstrNames.SETLr; break;
            case ICMP_SLE: setCCOpc = X86InstrNames.SETLEr; break;
            default:
                return;
        }

        Value op0 = ci.operand(0), op1 = ci.operand(1);
        if (swapArgs)
        {
            Value temp = op0;
            op0 = op1;
            op1 = temp;
        }

        // emit a compare of op0 - op1.
        if (!emitCompare(op0, op1, vt))
            return;

        buildMI(mbb, setCCOpc, resultReg);
    }

	@Override
	public Void visitICmp(ICmpInst ci)
	{
		emitCompare(ci);
        return null;
	}

	@Override
	public Void visitFCmp(FCmpInst ci)
	{
	    emitCompare(ci);
        return null;
	}

	@Override
	public Void visitTrunc(CastInst inst)
	{
		int srcReg = getReg(inst.operand(0));
		int destReg = getReg(inst);
        if (srcReg == 0 || destReg == 0)
            return null;

		int srcClass = getClass(inst.operand(0).getType());
		int destClass = getClass(inst.getType());

		assert  srcClass > destClass && srcClass < f32;

		if (srcClass == i64 && destClass == i32)
		{
			buildMI(mbb, X86InstrNames.MOVrr32, 1, destReg).addReg(srcReg);
			return null;
		}
		int regRegMove[] =
		{
			X86InstrNames.MOVrr8,
			X86InstrNames.MOVrr16, X86InstrNames.MOVrr32,
				X86InstrNames.FpMOV, X86InstrNames.MOVrr32
		};

		// Handle cast of LARGER int to SMALLER int using a move to EAX followed by a
		// move out of AX or AL.
		int areg[] = { X86RegNames.AL, X86RegNames.AX, X86RegNames.EAX, 0, X86RegNames.EAX};
		buildMI(mbb, regRegMove[srcClass], 1, areg[srcClass]).addReg(srcReg);
		buildMI(mbb, regRegMove[destClass], 1, areg[destClass]).addReg(areg[destReg]);
		return null;
	}

	@Override
	public Void visitZExt(CastInst inst)
	{
		int srcReg = getReg(inst.operand(0));
		int destReg = getReg(inst);

		int srcClass = getClass(inst.operand(0).getType());
		int destClass = getClass(inst.getType());

		assert  srcClass < destClass && destClass < f32;

		boolean isLong = destClass == i64;
		if (isLong) destClass = i32;

		int[] opcodes = { X86InstrNames.MOVZXr16r8,
				X86InstrNames.MOVZXr32r8,
				X86InstrNames.MOVZXr32r16,
				X86InstrNames.MOVrr32};

		buildMI(mbb, opcodes[srcClass + destClass - 1], 1, destReg).addReg(srcReg);
		if (isLong)
			buildMI(mbb, X86InstrNames.MOVir32, 1, destReg+1).addZImm(0);
		return null;
	}

	@Override
	public Void visitSExt(CastInst inst)
	{
		int srcReg = getReg(inst.operand(0));
		int destReg = getReg(inst);

		int srcClass = getClass(inst.operand(0).getType());
		int destClass = getClass(inst.getType());

		assert  srcClass < destClass && destClass < f32;

		boolean isLong = destClass == i64;
		if (isLong) destClass = i32;

		int[] opcodes = { X86InstrNames.MOVSXr16r8,
				X86InstrNames.MOVSXr32r8,
				X86InstrNames.MOVSXr32r16,
				X86InstrNames.MOVrr32};

		buildMI(mbb, opcodes[srcClass + destClass - 1], 1, destReg).addReg(srcReg);
		if (isLong)
			buildMI(mbb, X86InstrNames.SARir32, 1, destReg+1).
					addReg(destReg).addZImm(31);
		return null;
	}

	private void handleFPToInt(int srcReg, int destReg,
            int srcClass, int destClass,
            Type destTy)
    {
        // Change the floating point control register to use "round towards zero"
        // mode when truncating to an integer value.
        //
        int CWFrameIdx = mf.getFrameInfo().createStackObject(2, 2);
        addFrameReference(buildMI(mbb, X86InstrNames.FNSTCWm16, 4), CWFrameIdx);

        // Load the old value of the high byte of the control word...
        int HighPartOfCW = makeAnotherReg(Type.Int8Ty);
        addFrameReference(buildMI(mbb, X86InstrNames.MOVmr8, 4, HighPartOfCW),
                CWFrameIdx, 1);

        // Set the high part to be round to zero...
        addFrameReference(buildMI(mbb, X86InstrNames.MOVim8, 5), CWFrameIdx, 1)
                .addZImm(12);

        // Reload the modified control word now...
        addFrameReference(buildMI(mbb, X86InstrNames.FLDCWm16, 4), CWFrameIdx);

        // Restore the memory image of control word to original value
        addFrameReference(buildMI(mbb, X86InstrNames.MOVrm8, 5), CWFrameIdx, 1)
                .addReg(HighPartOfCW);

        // We don't have the facilities for directly storing byte sized data to
        // memory.  Promote it to 16 bits.  We also must promote int values to
        // larger classes because we only have signed FP stores.
        int StoreClass = destClass;
        Type storeTy = destTy;
        if (StoreClass == i8 || destTy.isUnsigned())
            switch (StoreClass)
            {
                case i8:
                    storeTy = Type.Int1Ty;
                    StoreClass = i16;
                    break;
                case i16:
                    storeTy = Type.Int32Ty;
                    StoreClass = i32;
                    break;
                case i32:
                    storeTy = Type.Int64Ty;
                    StoreClass = i64;
                    break;
                // The following treatment of i64 may not be perfectly right,
                // but it survives chains of casts of the form
                // double.ulong.double.
                case i64:
                    storeTy = Type.Int64Ty;
                    StoreClass = i64;
                    break;
                default:
                    assert false : "Unknown store class!";
            }

        // Spill the integer to memory and reload it from there...
        int FrameIdx = mf.getFrameInfo()
                .createStackObject(storeTy, tm.getTargetData());

        int Op1[] = { 0, X86InstrNames.FISTr16, X86InstrNames.FISTr32, 0,
                X86InstrNames.FISTPr64 };
        addFrameReference(buildMI(mbb, Op1[StoreClass], 5), FrameIdx)
                .addReg(srcReg);

        if (destClass == i64)
        {
            addFrameReference(buildMI(mbb, X86InstrNames.MOVmr32, 4, destReg),
                    FrameIdx);
            addFrameReference(
                    buildMI(mbb, X86InstrNames.MOVmr32, 4, destReg + 1),
                    FrameIdx, 4);
        }
        else
        {
            int Op2[] = { X86InstrNames.MOVmr8, X86InstrNames.MOVmr16,
                    X86InstrNames.MOVmr32 };
            addFrameReference(buildMI(mbb, Op2[destClass], 4, destReg),
                    FrameIdx);
        }

        // Reload the original control word now...
        addFrameReference(buildMI(mbb, X86InstrNames.FLDCWm16, 4), CWFrameIdx);
    }

	private Void emitFPToInt(CastInst inst)
	{
		int srcReg = getReg(inst.operand(0));
		int destReg = getReg(inst);

		int srcClass = getClass(inst.operand(0).getType());
		int destClass = getClass(inst.getType());
        handleFPToInt(srcReg, destReg, srcClass, destClass, inst.getType());
		return null;
	}

	@Override
	public Void visitFPToUI(CastInst inst)
	{
		return emitFPToInt(inst);
	}

	@Override
	public Void visitFPToSI(CastInst inst)
	{
		return emitFPToInt(inst);
	}


	private void handleIntToFP(int srcReg, int destReg,
            int srcClass, int destClass, Type srcTy)
    {

        Type promoteType = null;
        int promoteOpcode = 0;
        switch (srcTy.getTypeID())
        {
            case Type.IntegerTyID:
            case Type.IntegerTyID:
                promoteType = Type.Int16Ty;
                promoteOpcode = srcTy.isSigned()?
                        X86InstrNames.MOVSXr16r8: X86InstrNames.MOVZXr16r8;
                break;
            case Type.IntegerTyID:
                promoteType = Type.Int32Ty;
                promoteOpcode = srcTy.isSigned()?
                        X86InstrNames.MOVSXr32r16: X86InstrNames.MOVZXr32r16;
                break;
            case Type.IntegerTyID:
                int tempReg = makeAnotherReg(Type.Int64Ty);
                buildMI(mbb, X86InstrNames.MOVrr32, 1, tempReg).addReg(srcReg);
                buildMI(mbb, X86InstrNames.MOVir32, 1, tempReg+1).addZImm(0);
                srcTy = Type.Int64Ty;
                srcClass = i64;
                srcReg = tempReg;
                break;
            default:
                assert false:"Not implemented: cast long to float point type.";
        }
        if (promoteType != null)
        {
            int tempReg = makeAnotherReg(promoteType);
            buildMI(mbb, srcTy.isSigned()?
                            X86InstrNames.MOVSXr16r8:
                            X86InstrNames.MOVZXr16r8
                    , 1, tempReg).addReg(srcReg);
            srcTy = promoteType;
            srcClass = getClass(promoteType);
            srcReg = tempReg;
        }

        int frameIdx = mf.getFrameInfo().createStackObject(srcTy, tm.getTargetData());

        if (srcClass == i64)
        {
            addFrameReference(buildMI(mbb, X86InstrNames.MOVrm32, 5), frameIdx).addReg(srcReg);
            addFrameReference(buildMI(mbb, X86InstrNames.MOVrm32, 5), frameIdx, 4).addReg(srcReg+1);
        }
        else
        {
            int[] op1s = { X86InstrNames.MOVrm8, X86InstrNames.MOVrm16, X86InstrNames.MOVrm32};
            addFrameReference(buildMI(mbb, op1s[srcClass], 5), frameIdx).addReg(srcReg);
        }

        int[] op2s = {0, X86InstrNames.FLDr16, X86InstrNames.FLDr32, 0, X86InstrNames.FLDr64};
        addFrameReference(buildMI(mbb, op2s[srcClass], 5, destReg), frameIdx);
    }

	private Void emitIntToFP(CastInst inst)
	{
		int srcReg = getReg(inst.operand(0));
		int destReg = getReg(inst);

		int srcClass = getClass(inst.operand(0).getType());
		int destClass = getClass(inst.getType());

		// Promote the integer to a type supported by FLD.  We do this because there
		// are no int FLD instructions, so we must promote an int value to
		// a larger signed value, then use FLD on the larger value.
		Type srcTy = inst.operand(0).getType();

		handleIntToFP(srcReg, destReg, srcClass, destClass, srcTy);
		return null;
	}

	@Override
	public Void visitUIToFP(CastInst inst)
	{
		return emitIntToFP(inst);
	}

	@Override
	public Void visitSIToFP(CastInst inst)
	{
		return emitIntToFP(inst);
	}

	private void handleFPTrunc(int srcVT, int destVT, int opReg, int resultReg)
    {
        if (srcVT == f32)
        {
            if (destVT == f64)
            {
                if (opReg == 0 || resultReg == 0)
                    return;

                buildMI(mbb, X86InstrNames.CVTSD2SSrr, 1, resultReg).addReg(opReg);
            }
        }
    }

	@Override
	public Void visitFPTrunc(CastInst inst)
	{
		// This method uses the SSE2 instruction that is not supported in such
        // machine before Intel Pentium.
        int srcVT = getClass(inst.getType());
        int destVT = getClass(inst.operand(0).getType());
        Value val = inst.operand(0);
        int opReg = getReg(val);
        int resultReg = getReg(inst);
        handleFPTrunc(srcVT, destVT, opReg, resultReg);
		return null;
	}

	private void handleFPExt(int srcVT, int destVT, int opReg, int resultReg)
    {
        if (srcVT == f64)
        {
            if (destVT == f32)
            {
                if (opReg == 0 || resultReg == 0)
                    return;

                buildMI(mbb, X86InstrNames.CVTSS2SDrr, 1, resultReg).addReg(opReg);
            }
        }
    }

	@Override
	public Void visistFPExt(CastInst inst)
	{
        // This method uses the SSE2 instruction that is not supported in such
        // machine before Intel Pentium.
        int srcVT = getClass(inst.getType());
        int destVT = getClass(inst.operand(0).getType());
        Value val = inst.operand(0);
        int opReg = getReg(val);
        int resultReg = getReg(inst);
        handleFPExt(srcVT, destVT, opReg, resultReg);
        return null;
	}

	private void handlePtrConversion(CastInst inst)
    {
        int srcVT = getClass(inst.operand(0).getType());
        int destVT = getClass(inst.getType());
        if (destVT > srcVT)
        {
            visitZExt(inst);
            return;
        }
        if (destVT < srcVT)
        {
            visitTrunc(inst);
        }
    }

	@Override
	public Void visitPtrToInt(CastInst inst)
	{
		handlePtrConversion(inst);
		return null;
	}

	@Override
	public Void visitIntToPtr(CastInst inst)
	{
        handlePtrConversion(inst);
        return null;
	}

	private void handleBitCast(int srcVT, int destVT, int srcReg, int destReg)
    {
        assert srcVT== destVT && srcVT < f32;
        int[] opcodes = { X86InstrNames.MOVrr8, X86InstrNames.MOVrr16, X86InstrNames.MOVrr32,
                X86InstrNames.MOVrr32};
        buildMI(mbb, opcodes[srcVT], 1, destReg).addReg(srcReg);
        if (srcVT == i64)
        {
            buildMI(mbb, opcodes[srcVT], 1, destReg+1).addReg(srcReg+1);
        }
    }

	@Override
	public Void visitBitCast(CastInst inst)
	{
		int srcClass = getClass(inst.operand(0).getType());
		int destClass = getClass(inst.getType());
		int srcReg = getReg(inst.operand(0));
		int destReg = getReg(inst);

		handleBitCast(srcClass, destClass, srcReg, destReg);
		return null;
	}

	@Override
	public Void visitAlloca(Instruction.AllocaInst inst)
	{
		Type ty = inst.getAllocatedType();
		int tySize = (int) tm.getTargetData().getTypeSize(ty);

		if (inst.getArraySize() instanceof ConstantInt)
		{
			if (inst.getParent() == inst.getParent().getParent().getEntryBlock())
			{
				ConstantInt ci = (ConstantInt) inst.getArraySize();
				tySize *= ci.getZExtValue();
				int align = tm.getTargetData().getTypeAlign(ty);

				int frameIdx = mf.getFrameInfo().createStackObject(tySize, align);
				addFrameReference(buildMI(mbb, X86InstrNames.LEAr32, 5, getReg(inst)), frameIdx);
			}
		}

		// create a register to hold the temporary result of multipling the type getNumOfSubLoop.
		int totalSizeReg = makeAnotherReg(Type.Int32Ty);
		int srcReg = getReg(inst.getArraySize());

		// totalSize = tySize * array length.
		doMultiplyConst(mbb.size()-1, totalSizeReg, Type.Int32Ty, srcReg, tySize);

		// following instruction for alignment in 16Byte.
		// addedSizeReg = add totalSizeReg, 15.
		int addedSizeReg = makeAnotherReg(Type.Int32Ty);
		buildMI(mbb, X86InstrNames.ADDri32, 2, addedSizeReg).
				addReg(totalSizeReg).addZImm(15);
		// alignSizeReg = and addedSizeReg, ~15.
		int alignSizeReg = makeAnotherReg(Type.Int32Ty);
		buildMI(mbb, X86InstrNames.ANDri32, 2, alignSizeReg).
				addReg(addedSizeReg).addZImm(~15);

		// subtract getNumOfSubLoop from stack pointer, thereby allocating some space.
		buildMI(mbb, X86InstrNames.SUBrr32, 2, X86RegNames.ESP).
				addReg(X86RegNames.ESP).addReg(alignSizeReg);

		// Put a pointer to the space into the result register, by copying
		// the stack pointer.
		buildMI(mbb, X86InstrNames.MOVrr32, 1, getReg(inst)).addReg(X86RegNames.ESP);

		// Inform the frame information that we have just allocated a
		// variable sized object.
		mf.getFrameInfo().createVariableSizedObject();
		return null;
	}

	@Override
	public Void visitLoad(Instruction.LoadInst inst)
	{
		int srcAddReg = getReg(inst.operand(0));
		int destReg = getReg(inst);

		int klass = getClass(inst.getType());
		if (klass == i64)
		{
			addDirectMem(buildMI(mbb, X86InstrNames.MOVmr32, 4, destReg), srcAddReg);
			addRegOffset(buildMI(mbb, X86InstrNames.MOVmr32, 4, destReg+1), srcAddReg, 4);
		}

		int[] opcodes = { X86InstrNames.MOVmr8, X86InstrNames.MOVmr16,
				X86InstrNames.MOVmr32,
				X86InstrNames.FLDr32,
				X86InstrNames.FLDr64};
		int opcode = opcodes[klass];
		addDirectMem(buildMI(mbb, opcode, 4, destReg), srcAddReg);
		return null;
	}

	@Override
	public Void visitStore(StoreInst inst)
	{
		int srcReg = getReg(inst.operand(0));
		int destAddrReg = getReg(inst.operand(1));

		int klass = getClass(inst.operand(0).getType());
		if (klass == i64)
		{
			addDirectMem(buildMI(mbb, X86InstrNames.MOVrm32, 1+4), destAddrReg).
					addReg(srcReg);
			addRegOffset(buildMI(mbb, X86InstrNames.MOVrm32, 1+4), destAddrReg, 4).
					addReg(srcReg+1);
		}

		int[] opcodes = { X86InstrNames.MOVrm8, X86InstrNames.MOVrm16,
				X86InstrNames.MOVrm32,
				X86InstrNames.FSTr32,
				X86InstrNames.FSTr64};
		int opcode = opcodes[klass];
		addDirectMem(buildMI(mbb, opcode, 1+4), destAddrReg).addReg(srcReg);
		return null;
	}

	/**
	 * Push args on stack and do a procedure call instruction.
	 * @param ci
	 * @return
	 */
	@Override
	public Void visitCall(CallInst ci)
	{
		MachineInstr call = null;
		Function f = null;
		if ((f = ci.getCalledFunction()) != null)
		{
			// direct call to function.
			// handle intrinsic call.

			// emit a CALL instruction with PC-relative displacement.
			call = buildMI(X86InstrNames.CALLpcrel32, 1).
					addGlobalAddress(f, true).getMInstr();
		}
		else
		{
			// indirect call.
			int reg = getReg(ci.getCalledValue());
			call = buildMI(X86InstrNames.CALLr32, 1).addReg(reg).getMInstr();
		}

		ArrayList<ValueRecord> args = new ArrayList<>();
		for (int i = 0, e = ci.getNumsOfArgs(); i < e; i++)
		{
			args.add(new ValueRecord(ci.argumentAt(i)));
		}
		int destReg = ci.getType() != Type.VoidTy?getReg(ci) : 0;
		doCall(new ValueRecord(destReg, ci.getType()), call, args);
		return null;
	}

	@Override
	public Void visitGetElementPtr(GetElementPtrInst inst)
	{
		int outputReg = getReg(inst);

		// Can not handled, fast halted.
		if (outputReg == 0) return null;

		emitGEPOperation(mbb.size()-1, inst.operand(0), inst.getUseList(), outputReg);
		return null;
	}

	/**
	 * This method is noop operation. All PHI Node will be processed in a
	 * single special pass.
	 * @param inst
	 * @return
	 */
	@Override
	public Void visitPhiNode(PhiNode inst)
	{
		Util.shouldNotReachHere("Should not reach here!");
		return null;
	}

	@Override
	public String getPassName()
	{
		return "X86 simple instruction selector.";
	}

	/**
	 * Creates a simple instruction selector for x86 target.
	 * @param tm
	 * @return
	 */
	public static X86SimpleInstSel
		createX86SimpleInstructionSelector(TargetMachine tm)
	{
		return new X86SimpleInstSel(tm);
	}
}
