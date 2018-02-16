package backend.utils;

import backend.ir.AllocationInst;
import backend.ir.MallocInst;
import backend.ir.SelectInst;
import backend.value.BasicBlock;
import backend.value.Function;
import backend.value.Instruction;
import backend.value.Instruction.*;

import java.util.List;

/**
 * A Value Visitor for Instruction or Value using Visitor pattern.
 *
 * @author Xlous.zeng
 */
public interface InstVisitor<RetTy>
{
	default void visit(List<BasicBlock> blocks)
	{
		for (BasicBlock bb : blocks)
			visit(bb);
	}

	default void visit(BasicBlock bb)
	{
		for (Instruction inst : bb.getInstList())
			visit(inst);
	}

	default void visit(Function f)
	{
		visit(f.getBasicBlockList());
	}

	default RetTy visit(Instruction inst)
	{
		switch (inst.getOpcode())
		{
			default:
				assert false : "Undefined instruction type encountered!";
				System.exit(-1);
			case Add:
				return visitAdd((BinaryOps) inst);
			case FAdd:
				return visitFAdd((BinaryOps) inst);
			case Sub:
				return visitSub((BinaryOps) inst);
			case FSub:
				return visitFSub((BinaryOps) inst);
			case Mul:
				return visitMul((BinaryOps) inst);
			case FMul:
				return visitFMul((BinaryOps) inst);
			case UDiv:
				return visitUDiv((BinaryOps) inst);
			case SDiv:
				return visitSDiv((BinaryOps) inst);
			case FDiv:
				return visitFDiv((BinaryOps) inst);
			case URem:
				return visitURem((BinaryOps) inst);
			case SRem:
				return visitSRem((BinaryOps) inst);
			case FRem:
				return visitFRem((BinaryOps) inst);
			case And:
				return visitAnd((BinaryOps) inst);
			case Or:
				return visitOr((BinaryOps) inst);
			case Xor:
				return visitXor((BinaryOps) inst);
			case Shl:
				return visitShl((BinaryOps) inst);
			case AShr:
				return visitAShr((BinaryOps) inst);
			case LShr:
				return visitLShr((BinaryOps) inst);
			case ICmp:
				return visitICmp((ICmpInst) inst);
			case FCmp:
				return visitFCmp((FCmpInst) inst);
			case Trunc:
				return visitTrunc((CastInst) inst);
			case ZExt:
				return visitZExt((CastInst) inst);
			case SExt:
				return visitSExt((CastInst) inst);
			case FPToUI:
				return visitFPToUI((CastInst) inst);
			case FPToSI:
				return visitFPToSI((CastInst) inst);
			case UIToFP:
				return visitUIToFP((CastInst) inst);
			case SIToFP:
				return visitSIToFP((CastInst) inst);
			case FPTrunc:
				return visitFPTrunc((CastInst) inst);
			case FPExt:
				return visistFPExt((CastInst) inst);
			case PtrToInt:
				return visitPtrToInt((CastInst) inst);
			case IntToPtr:
				return visitIntToPtr((CastInst) inst);
			case BitCast:
				return visitBitCast((CastInst) inst);
		}
	}

	//==============================================//
	// branch instr.
	//==============================================//
	RetTy visitRet(ReturnInst inst);

	RetTy visitBr(BranchInst inst);

	RetTy visitSwitch(SwitchInst inst);

	//===============================================//
	// arithmetic instr.
	//===============================================//
	default RetTy visitAdd(BinaryOps inst)
	{
		return visitBinaryOp(inst);
	}

	default RetTy visitFAdd(BinaryOps inst)
	{
		return visitBinaryOp(inst);
	}

	default RetTy visitSub(BinaryOps inst)
	{
		return visitBinaryOp(inst);
	}

	default RetTy visitFSub(BinaryOps inst)
	{
		return visitBinaryOp(inst);
	}

	default RetTy visitMul(BinaryOps inst)
	{
		return visitBinaryOp(inst);
	}

	default RetTy visitFMul(BinaryOps inst)
	{
		return visitBinaryOp(inst);
	}

	default RetTy visitUDiv(BinaryOps inst)
	{
		return visitBinaryOp(inst);
	}

	default RetTy visitSDiv(BinaryOps inst)
	{
		return visitBinaryOp(inst);
	}

	default RetTy visitFDiv(BinaryOps inst)
	{
		return visitBinaryOp(inst);
	}

	default RetTy visitURem(BinaryOps inst)
	{
		return visitBinaryOp(inst);
	}

	default RetTy visitSRem(BinaryOps inst)
	{
		return visitBinaryOp(inst);
	}

	default RetTy visitFRem(BinaryOps inst)
	{
		return visitBinaryOp(inst);
	}

	default RetTy visitBinaryOp(BinaryOps inst)
	{
		return visitBinaryOp(inst);
	}

	//===============================================//
	// bitwise operator.
	//===============================================//
	default RetTy visitAnd(BinaryOps inst)
	{
		return visitBinaryOp(inst);
	}

	default RetTy visitOr(BinaryOps inst)
	{
		return visitBinaryOp(inst);
	}

	default RetTy visitXor(BinaryOps inst)
	{
		return visitBinaryOp(inst);
	}

	//=============================================//
	// shift operators.
	//============================================//
	default RetTy visitShl(BinaryOps inst)
	{
		return visitBinaryOp(inst);
	}

	default RetTy visitLShr(BinaryOps inst)
	{
		return visitBinaryOp(inst);
	}

	default RetTy visitAShr(BinaryOps inst)
	{
		return visitBinaryOp(inst);
	}

	//===============================================//
	// comparison instr.
	//===============================================//
	RetTy visitICmp(ICmpInst inst);

	RetTy visitFCmp(FCmpInst inst);

	//==============================================//
	// cast operators.
	//=============================================//
	default RetTy visitTrunc(CastInst inst)
	{
		return visitCastInst(inst);
	}

	default RetTy visitZExt(CastInst inst)
	{
		return visitCastInst(inst);
	}

	default RetTy visitSExt(CastInst inst)
	{
		return visitCastInst(inst);
	}

	default RetTy visitFPToUI(CastInst inst)
	{
		return visitCastInst(inst);
	}

	default RetTy visitFPToSI(CastInst inst)
	{
		return visitCastInst(inst);
	}

	default RetTy visitUIToFP(CastInst inst)
	{
		return visitCastInst(inst);
	}

	default RetTy visitSIToFP(CastInst inst)
	{
		return visitCastInst(inst);
	}

	default RetTy visitFPTrunc(CastInst inst)
	{
		return visitCastInst(inst);
	}

	default RetTy visistFPExt(CastInst inst)
	{
		return visitCastInst(inst);
	}

	default RetTy visitPtrToInt(CastInst inst)
	{
		return visitCastInst(inst);
	}

	default RetTy visitIntToPtr(CastInst inst)
	{
		return visitCastInst(inst);
	}

	default RetTy visitBitCast(CastInst inst)
	{
		return visitCastInst(inst);
	}

	RetTy visitCastInst(CastInst inst);

	//===============================================//
	// memory operator.
	//===============================================//
	default RetTy visitAlloca(AllocaInst inst)
	{
		return visitAllocationInst(inst);
	}

	default RetTy visitMalloc(MallocInst inst)
	{
		return visitAllocationInst(inst);
	}

	default RetTy visitAllocationInst(AllocationInst inst)
	{
		return null;
	}

	RetTy visitLoad(LoadInst inst);

	RetTy visitStore(StoreInst inst);

	//==============================================//
	// other operators.
	//==============================================//
	RetTy visitCall(CallInst inst);

	RetTy visitGetElementPtr(GetElementPtrInst inst);

	RetTy visitPhiNode(PhiNode inst);

	/**
	 * Visit Select instruciton in LLVM IR programming reference.
	 * @param inst
	 * @return
	 */
	RetTy visitSelect(SelectInst inst);
}
