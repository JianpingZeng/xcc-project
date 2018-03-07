package backend.ir;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous zeng.
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

import backend.support.LLVMContext;
import backend.type.Type;
import backend.value.*;
import backend.value.Instruction.*;
import backend.value.Instruction.CmpInst.Predicate;

import java.util.ArrayList;
import java.util.LinkedList;

import static backend.value.Instruction.CmpInst.Predicate.*;

/**
 * This file defines a class "HIRBuilder" that responsible for converting each
 * kind of AST node (Expression or Statement) into LLVM IR code. For instance,
 * converting all of statement (except for expression statement) into control
 * flow IR, e.g. br, icmp, fcmp, goto etc. In the another greatly important aspect,
 * the handling of Expression is the focus. We uses some arithmetic, logical,
 * comparison, shift-wise operation to accompolish this purpose.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class HIRBuilder
{
	/**
	 * The basic block where all instruction will be inserted.
	 */
	private BasicBlock curBB;

	private Instruction insertPtr;

	public HIRBuilder(){super();}

	public HIRBuilder(BasicBlock bb)
	{
		setInsertPoint(bb);
	}

	public void setInsertPoint(BasicBlock insertPoint)
	{
		curBB = insertPoint;
		insertPtr = insertPoint.getLastInst();
		//assert insertPtr != null:"Can not set an empty BasicBlock as insert point";
	}

	public void setInsertPoint(BasicBlock theBB, Instruction pos)
	{
		curBB = theBB;
		assert pos.getParent().equals(theBB);
		insertPtr = pos;
	}

	private <InstTy extends Instruction> InstTy insert(InstTy inst)
	{
		insertHelper(inst, curBB, insertPtr);
		return inst;
	}

	private <InstTy extends Instruction> InstTy insert(InstTy inst, String name)
	{
		insertHelper(inst, curBB, insertPtr);
		inst.setName(name);
		return inst;
	}

	private <InstTy extends Instruction> void insertHelper(
			InstTy inst,
			BasicBlock bb,
			Instruction insertPtr)
	{
		bb.insertBefore(inst, insertPtr);
	}

	public BasicBlock getInsertBlock()
	{
		return curBB;
	}

	public Instruction getInsertPoint()
	{
		return insertPtr;
	}

	/**
	 * Clear the current insertion point to let the newest created instruction
	 * would be inserted into a block.
	 */
	public void clearInsertPoint()
	{
		curBB = null;
	}

	//============================================================//
	// Cast instruction.                                          //
	//============================================================//

	public Value createTrunc(Value val, Type destType, String name)
	{
		return createCast(Operator.Trunc, val, destType, name);
	}

	public Value createZExt(Value val, Type destType, String name)
	{
		return createCast(Operator.ZExt, val, destType, name);
	}

	public Value createSExt(Value val, Type destType, String name)
	{
		return createCast(Operator.SExt, val, destType, name);
	}

	public Value createFPToUI(Value val, Type destType, String name)
	{
		return createCast(Operator.FPToUI, val, destType, name);
	}

	public Value createFPToSI(Value val, Type destType, String name)
	{
		return createCast(Operator.FPToSI, val, destType, name);
	}

	public Value createUIToFP(Value val, Type destType, String name)
	{
		return createCast(Operator.UIToFP, val, destType, name);
	}

	public Value createSIToFP(Value val, Type destType, String name)
	{
		return createCast(Operator.SIToFP, val, destType, name);
	}

	public Value createFPTrunc(Value val, Type destType, String name)
	{
		return createCast(Operator.FPTrunc, val, destType, name);
	}

	public Value createFPExt(Value val, Type destType, String name)
	{
		return createCast(Operator.FPExt, val, destType, name);
	}

	public Value createPtrToInt(Value val, Type destType, String name)
	{
		return createCast(Operator.PtrToInt, val, destType, name);
	}

	public Value createIntToPtr(Value val, Type destType, String name)
	{
		return createCast(Operator.IntToPtr, val, destType, name);
	}

	public Value createBitCast(Value val, Type destType, String name)
	{
		return createCast(Operator.BitCast, val, destType, name);
	}

	public Value createIntCast(Value value, backend.type.Type destTy,
			boolean isSigned)
	{
		return createIntCast(value, destTy, isSigned, "");
	}

	public Value createIntCast(Value value, backend.type.Type destTy,
			boolean isSigned, String name)
	{
		// if the type of source is equal to destination type
		// just return original value.
		if (value.getType() == destTy)
			return value;

		if (value instanceof Constant)
		{
			// TODO make constant folding.
		}
		return insert(CastInst.createIntegerCast(value, destTy, isSigned),
				name);
	}

	public Value createCast(Operator op, Value val, Type destType, String name)
	{
		if (val.getType() == destType)
			return val;

		if (val instanceof Constant)
		{
			// TODO make constant folding.
		}
		return insert(CastInst.create(op, val, destType, name, null));
	}

    public Value createBitCast(Value value, Type destTy)
    {
        return createBitCast(value, destTy, "");
    }

	/**
	 * create an unconditional branch instruction-'br label X'.
	 *
	 * @param targetBB
	 */
	public BranchInst createBr(BasicBlock targetBB)
	{
		return insert(new BranchInst(targetBB));
	}

	/**
	 * creates a branch instruction, like 'br cond trueBB, falseBB' on the
	 * specified condition.
	 *
	 * @param condVal
	 * @param trueBB
	 * @param falseBB
	 * @return
	 */
	public Value createCondBr(Value condVal, BasicBlock trueBB,
			BasicBlock falseBB)
	{
		return insert(new BranchInst(trueBB, falseBB, condVal));
	}

	/**
	 * creates a switch instruction with the specified value.
	 * default dest, and with a hint for the number of cases that will
	 * be added (for efficient allocation).
	 *
	 * @param condV
	 * @param defaultBB
	 * @return
	 */
	public SwitchInst createSwitch(Value condV, BasicBlock defaultBB)
	{
		return createSwitch(condV, defaultBB, 10);
	}

	public SwitchInst createSwitch(Value condV, BasicBlock defaultBB,
			int numCases)
	{
		return insert(new SwitchInst(condV, defaultBB, numCases, ""));
	}

	//===--------------------------------------------------------------------===//
	// Instruction creation methods: Compare Instructions
	//===--------------------------------------------------------------------===//

	public Value createICmpEQ(Value lhs, Value rhs, final String name)
	{
		return createICmp(ICMP_EQ, lhs, rhs, name);
	}

	public Value createICmpNE(Value lhs, Value rhs, final String name)
	{
		return createICmp(ICMP_NE, lhs, rhs, name);
	}

	public Value createICmpUGT(Value lhs, Value rhs, final String name)
	{
		return createICmp(ICMP_UGT, lhs, rhs, name);
	}

	public Value createICmpUGE(Value lhs, Value rhs, final String name)
	{
		return createICmp(ICMP_UGE, lhs, rhs, name);
	}

	public Value createICmpULT(Value lhs, Value rhs, final String name)
	{
		return createICmp(ICMP_ULT, lhs, rhs, name);
	}

	public Value createICmpULE(Value lhs, Value rhs, final String name)
	{
		return createICmp(ICMP_ULE, lhs, rhs, name);
	}

	public Value createICmpSGT(Value lhs, Value rhs, final String name)
	{
		return createICmp(ICMP_SGT, lhs, rhs, name);
	}

	public Value createICmpSGE(Value lhs, Value rhs, final String name)
	{
		return createICmp(ICMP_SGE, lhs, rhs, name);
	}

	public Value createICmpSLT(Value lhs, Value rhs, final String name)
	{
		return createICmp(ICMP_SLT, lhs, rhs, name);
	}

	public Value createICmpSLE(Value lhs, Value rhs, final String name)
	{
		return createICmp(ICMP_SLE, lhs, rhs, name);
	}

	public Value createFCmpOEQ(Value lhs, Value rhs, final String name)
	{
		return createFCmp(FCMP_OEQ, lhs, rhs, name);
	}

	public Value createFCmpOGT(Value lhs, Value rhs, final String name)
	{
		return createFCmp(FCMP_OGT, lhs, rhs, name);
	}

	public Value createFCmpOGE(Value lhs, Value rhs, final String name)
	{
		return createFCmp(FCMP_OGE, lhs, rhs, name);
	}

	public Value createFCmpOLT(Value lhs, Value rhs, final String name)
	{
		return createFCmp(FCMP_OLT, lhs, rhs, name);
	}

	public Value createFCmpOLE(Value lhs, Value rhs, final String name)
	{
		return createFCmp(FCMP_OLE, lhs, rhs, name);
	}

	public Value createFCmpONE(Value lhs, Value rhs, final String name)
	{
		return createFCmp(FCMP_ONE, lhs, rhs, name);
	}

	public Value createFCmpORD(Value lhs, Value rhs, final String name)
	{
		return createFCmp(FCMP_ORD, lhs, rhs, name);
	}

	public Value createFCmpUNO(Value lhs, Value rhs, final String name)
	{
		return createFCmp(FCMP_UNO, lhs, rhs, name);
	}

	public Value createFCmpUEQ(Value lhs, Value rhs, final String name)
	{
		return createFCmp(FCMP_UEQ, lhs, rhs, name);
	}

	public Value createFCmpUGT(Value lhs, Value rhs, final String name)
	{
		return createFCmp(FCMP_UGT, lhs, rhs, name);
	}

	public Value createFCmpUGE(Value lhs, Value rhs, final String name)
	{
		return createFCmp(FCMP_UGE, lhs, rhs, name);
	}

	public Value createFCmpULT(Value lhs, Value rhs, final String name)
	{
		return createFCmp(FCMP_ULT, lhs, rhs, name);
	}

	public Value createFCmpULE(Value lhs, Value rhs, final String name)
	{
		return createFCmp(FCMP_ULE, lhs, rhs, name);
	}

	public Value createFCmpUNE(Value lhs, Value rhs, final String name)
	{
		return createFCmp(FCMP_UNE, lhs, rhs, name);
	}

	public Value createICmp(Predicate pred, Value lhs, Value rhs, final String name)
	{
		if (lhs instanceof Constant)
		{
			Constant LC = (Constant) lhs;
			if (rhs instanceof Constant)
			{
				Constant RC = (Constant) rhs;
				return null; // TODO folder.createICmp(P, LC, RC);
			}
		}
		return insert(new Instruction.ICmpInst(pred, lhs, rhs, name), name);
	}

	public Value createFCmp(Predicate P, Value lhs, Value rhs, final String name)
	{
		if (lhs instanceof Constant)
		{
			Constant LC = (Constant) lhs;
			if (rhs instanceof Constant)
			{
				Constant RC = (Constant) rhs;
				return null; // TODO folder.createFCmp(P, LC, RC);
			}
		}
		return insert(new Instruction.FCmpInst(P, lhs, rhs, name), name);
	}

	//===--------------------------------------------------------------------===//
	// Instruction creation methods: Binary Operators
	//===--------------------------------------------------------------------===//

	public Value createAdd(Value lhs, Value rhs, final String name)
	{
		if (lhs instanceof Constant)
		{
			Constant LC = (Constant) lhs;
			if (rhs instanceof Constant)
			{
				Constant RC = (Constant) rhs;
				return null; // TODO folder.createAdd(LC, RC);
			}
		}
		return insert(BinaryOps.createAdd(lhs, rhs, name),name);
	}

	public Value createFAdd(Value lhs, Value rhs, final String name)
	{
		if (lhs instanceof Constant)
		{
			Constant LC = (Constant) lhs;
			if (rhs instanceof Constant)
			{
				Constant RC = (Constant) rhs;
				return null; // TODO folder.createFAdd(LC, RC);
			}
		}

		return insert(BinaryOps.createFAdd(lhs, rhs, name),name);
	}

	public Value createSub(Value lhs, Value rhs, final String name)
	{
		if (lhs instanceof Constant)
		{
			Constant LC = (Constant) lhs;
			if (rhs instanceof Constant)
			{
				Constant RC = (Constant) rhs;
				return null; // TODO folder.createSub(LC, RC);
			}
		}

		return insert(BinaryOps.createSub(lhs, rhs, name),name);
	}

	public Value createFSub(Value lhs, Value rhs, final String name)
	{
		if (lhs instanceof Constant)
		{
			Constant LC = (Constant) lhs;
			if (rhs instanceof Constant)
			{
				Constant RC = (Constant) rhs;
				return null; // TODO folder.createFSub(LC, RC);
			}
		}

		return insert(BinaryOps.createFSub(lhs, rhs, name),name);
	}

	public Value createMul(Value lhs, Value rhs, final String name)
	{
		if (lhs instanceof Constant)
		{
			Constant LC = (Constant) lhs;
			if (rhs instanceof Constant)
			{
				Constant RC = (Constant) rhs;
				return null; // TODO folder.createMul(LC, RC);
			}
		}
		return insert(BinaryOps.createMul(lhs, rhs, name),name);
	}

	public Value createFMul(Value lhs, Value rhs, final String name)
	{
		if (lhs instanceof Constant)
		{
			Constant LC = (Constant) lhs;
			if (rhs instanceof Constant)
			{
				Constant RC = (Constant) rhs;
				return null; // TODO folder.createFMul(LC, RC);
			}
		}

		return insert(BinaryOps.createFMul(lhs, rhs, name),name);
	}

	public Value createUDiv(Value lhs, Value rhs, final String name)
	{
		if (lhs instanceof Constant)
		{
			Constant LC = (Constant) lhs;
			if (rhs instanceof Constant)
			{
				Constant RC = (Constant) rhs;
				return null; // TODO folder.createUDiv(LC, RC);
			}
		}
		return insert(BinaryOps.createUDiv(lhs, rhs, name),name);
	}

	public Value createSDiv(Value lhs, Value rhs, final String name)
	{
		if (lhs instanceof Constant)
		{
			Constant LC = (Constant) lhs;
			if (rhs instanceof Constant)
			{
				Constant RC = (Constant) rhs;
				return null; // TODO folder.createSDiv(LC, RC);
			}
		}
		return insert(BinaryOps.createSDiv(lhs, rhs, name),name);
	}

	public Value createFDiv(Value lhs, Value rhs, final String name)
	{
		if (lhs instanceof Constant)
		{
			Constant LC = (Constant) lhs;
			if (rhs instanceof Constant)
			{
				Constant RC = (Constant) rhs;
				return null; // TODO folder.createFDiv(LC, RC);
			}
		}
		return insert(BinaryOps.createFDiv(lhs, rhs, name),name);
	}

	public Value createURem(Value lhs, Value rhs, final String name)
	{
		if (lhs instanceof Constant)
		{
			Constant LC = (Constant) lhs;
			if (rhs instanceof Constant)
			{
				Constant RC = (Constant) rhs;
				return null; // TODO folder.createURem(LC, RC);
			}
		}
		return insert(BinaryOps.createURem(lhs, rhs, name),name);
	}

	public Value createSRem(Value lhs, Value rhs, final String name)
	{
		if (lhs instanceof Constant)
		{
			Constant LC = (Constant) lhs;
			if (rhs instanceof Constant)
			{
				Constant RC = (Constant) rhs;
				return null; // TODO folder.createSRem(LC, RC);
			}
		}
		return insert(BinaryOps.createSRem(lhs, rhs, name),name);
	}

	public Value createFRem(Value lhs, Value rhs, final String name)
	{
		if (lhs instanceof Constant)
		{
			Constant LC = (Constant) lhs;
			if (rhs instanceof Constant)
			{
				Constant RC = (Constant) rhs;
				return null; // TODO folder.createFRem(LC, RC);
			}
		}
		return insert(BinaryOps.createFRem(lhs, rhs, name),name);
	}

	public Value createShl(Value lhs, Value rhs, final String name)
	{
		if (lhs instanceof Constant)
		{
			Constant LC = (Constant) lhs;
			if (rhs instanceof Constant)
			{
				Constant RC = (Constant) rhs;
				return null; // TODO folder.createShl(LC, RC);
			}
		}
		return insert(BinaryOps.createShl(lhs, rhs, name),name);
	}

	public Value createLShr(Value lhs, Value rhs, final String name)
	{
		if (lhs instanceof Constant)
		{
			Constant LC = (Constant) lhs;
			if (rhs instanceof Constant)
			{
				Constant RC = (Constant) rhs;
				return null; // TODO folder.createLShr(LC, RC);
			}
		}
		return insert(BinaryOps.createLShr(lhs, rhs, name),name);
	}

	public Value createAShr(Value lhs, Value rhs, final String name)
	{
		if (lhs instanceof Constant)
		{
			Constant LC = (Constant) lhs;
			if (rhs instanceof Constant)
			{
				Constant RC = (Constant) rhs;
				return null; // TODO folder.createAShr(LC, RC);
			}
		}
		return insert(BinaryOps.createAShr(lhs, rhs, name),name);
	}

	public Value createAnd(Value lhs, Value rhs, final String name)
	{
		if (lhs instanceof Constant)
		{
			Constant LC = (Constant) lhs;
			if (rhs instanceof Constant)
			{
				Constant RC = (Constant) rhs;
				return null; // TODO folder.createAnd(LC, RC);
			}
		}
		return insert(BinaryOps.createAnd(lhs, rhs, name),name);
	}

	public Value createOr(Value lhs, Value rhs, final String name)
	{
		if (lhs instanceof Constant)
		{
			Constant LC = (Constant) lhs;
			if (rhs instanceof Constant)
			{
				Constant RC = (Constant) rhs;
				return null; // TODO folder.createOr(LC, RC);
			}
		}
		return insert(BinaryOps.createOr(lhs, rhs, name),name);
	}

	public Value createXor(Value lhs, Value rhs, final String name)
	{
		if (lhs instanceof Constant)
		{
			Constant LC = (Constant) lhs;
			if (rhs instanceof Constant)
			{
				Constant RC = (Constant) rhs;
				return null; // TODO folder.createXor(LC, RC);
			}
		}
		return insert(BinaryOps.createXor(lhs, rhs, name),name);
	}

	public Value createBinOp(Operator opc, Value lhs,
			Value rhs, final String name)
	{
		if (lhs instanceof Constant)
		{
			Constant LC = (Constant) lhs;
			if (rhs instanceof Constant)
			{
				Constant RC = (Constant) rhs;
				return null; // TODO folder.createBinOp(opc, LC, RC);
			}
		}
		return insert(BinaryOps.create(opc, lhs, rhs, name),name);
	}

	public Value createNeg(Value v, final String name)
	{
		if (v instanceof Constant)
		{
			Constant vc = (Constant)v;
			return null; // TODO folder.createNeg(vc);
		}
		return insert(BinaryOps.createNeg(v), name);
	}

	public Value createFNeg(Value v, final String name)
	{
		if (v instanceof Constant)
		{
			Constant vc = (Constant)v;
			return null; // TODO folder.createFNeg(vc);
		}
		return insert(BinaryOps.createFNeg(v), name);
	}

	public Value createNot(Value v, final String name)
	{
		if (v instanceof Constant)
		{
			Constant vc = (Constant)v;
			return null; // TODO folder.createNot(vc);
		}
		return insert(BinaryOps.createNot(v), name);
	}

	public LoadInst createLoad(Value addr)
	{
		return createLoad(addr, false, "");
	}

	public LoadInst createLoad(Value addr, boolean isVolatile, String name)
	{
		return insert(new LoadInst(addr, name, false, 0), name);
	}

	public Instruction.StoreInst createStore(Value val, Value ptr)
	{
		return insert(new Instruction.StoreInst(val, ptr, ""), "");
	}

	public AllocaInst createAlloca(final Type ty, Value arraySize,
			final String name)
	{
		return insert(new AllocaInst(ty, arraySize, name), name);
	}

	public Value createGEP(Value ptr, Value idx, String name)
	{
		return insert(new GetElementPtrInst(ptr, idx, name));
	}

	public Value createInBoundsGEP(Value ptr, Value idx, String name)
	{
		GetElementPtrInst gep = new GetElementPtrInst(ptr, idx, name);
		gep.setInbounds(true);
		return insert(gep);
	}

    /**
     * This method is a variant of {@linkplain #createCall4(Value, Value, Value, Value, Value)}
     * with empty asmName by default.
     * @param callee
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @return
     */
    public CallInst createCall4(
            Value callee,
            Value arg1,
            Value arg2,
            Value arg3,
            Value arg4)
    {
        return createCall4(callee, arg1, arg2, arg3, arg4);
    }

    /**
     * Call the function specified by {@code callee} with some arguments, eg.
     * arg1, arg2, arg3, arg4.
     * @param callee
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param name
     * @return
     */
	public CallInst createCall4(
	        Value callee,
            Value arg1,
            Value arg2,
			Value arg3,
            Value arg4,
			String name)
	{
		Value[] args = {arg1, arg2, arg3, arg4};
		return insert(new CallInst(args, callee), name);
	}

	public CallInst createCall(Value callee, LinkedList<Value> args)
	{
		Value[] temp = new Value[args.size()];
		args.toArray(temp);
		return insert(new CallInst(temp, callee));
	}

	public Value createStructGEPInbounds(Value base, int idx, String name)
	{
		return createStructGEP32Inbounds(base, 0, idx, name);
	}

	public Value createStructGEP32Inbounds(Value base, int idx1, int idx2, String name)
	{
		ArrayList<Value> indices = new ArrayList<>();
		indices.add(ConstantInt.get(LLVMContext.Int32Ty,idx1));
		indices.add(ConstantInt.get(LLVMContext.Int32Ty,idx2));
		GetElementPtrInst gep = new GetElementPtrInst(base, indices, name);
		gep.setInbounds(true);
		return insert(gep);
	}

	public Value createStructGEPInbounds(Value base, long idx, String name)
	{
		return createStructGEP64Inbounds(base, 0, idx, name);
	}

	public Value createStructGEP64Inbounds(Value base, long idx1, long idx2, String name)
	{
		ArrayList<Value> indices = new ArrayList<>();
		indices.add(ConstantInt.get(LLVMContext.Int64Ty,idx1));
		indices.add(ConstantInt.get(LLVMContext.Int64Ty,idx2));
		GetElementPtrInst gep = new GetElementPtrInst(base, indices, name);
		gep.setInbounds(true);
		return insert(gep);
	}

	/**
	 * Creates a return instruction {@code 'ret <value>'} which returns
	 * specified value.
	 * @param value
	 * @return
	 */
	public ReturnInst createRet(Value value)
	{
		return insert(new ReturnInst(value));
	}

	/**
	 * Creates a ReturnInst {@code 'ret <void>'} which no return value.
	 * @return
	 */
	public ReturnInst createRetVoid()
	{
		return insert(new ReturnInst());
	}

	/**
	 * Create a select LLVM instruction.
	 * @param cond  The condition of this instruction.
	 * @param lhs   The left hand expression.
	 * @param rhs   The right hand expression.
	 * @param name  The name of this instruction would be printed out into assembly.
	 * @return
	 */
	public Value createSelect(Value cond, Value lhs, Value rhs, String name)
	{
		return insert(new SelectInst(cond, lhs, rhs, name));
	}
}
