package backend.utils;

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
	RetTy visitAdd(BinaryOps inst);

	RetTy visitFAdd(BinaryOps inst);

	RetTy visitSub(BinaryOps inst);

	RetTy visitFSub(BinaryOps inst);

	RetTy visitMul(BinaryOps inst);

	RetTy visitFMul(BinaryOps inst);

	RetTy visitUDiv(BinaryOps inst);

	RetTy visitSDiv(BinaryOps inst);

	RetTy visitFDiv(BinaryOps inst);

	RetTy visitURem(BinaryOps inst);

	RetTy visitSRem(BinaryOps inst);

	RetTy visitFRem(BinaryOps inst);

	//===============================================//
	// bitwise operator.
	//===============================================//
	RetTy visitAnd(BinaryOps inst);

	RetTy visitOr(BinaryOps inst);

	RetTy visitXor(BinaryOps inst);

	//=============================================//
	// shift operators.
	//============================================//
	RetTy visitShl(BinaryOps inst);

	RetTy visitLShr(BinaryOps inst);

	RetTy visitAShr(BinaryOps inst);

	//===============================================//
	// comparison instr.
	//===============================================//
	RetTy visitICmp(ICmpInst inst);

	RetTy visitFCmp(FCmpInst inst);

	//==============================================//
	// cast operators.
	//=============================================//
	RetTy visitTrunc(CastInst inst);

	RetTy visitZExt(CastInst inst);

	RetTy visitSExt(CastInst inst);

	RetTy visitFPToUI(CastInst inst);

	RetTy visitFPToSI(CastInst inst);

	RetTy visitUIToFP(CastInst inst);

	RetTy visitSIToFP(CastInst inst);

	RetTy visitFPTrunc(CastInst inst);

	RetTy visistFPExt(CastInst inst);

	RetTy visitPtrToInt(CastInst inst);

	RetTy visitIntToPtr(CastInst inst);

	RetTy visitBitCast(CastInst inst);

	//===============================================//
	// memory operator.
	//===============================================//
	RetTy visitAlloca(AllocaInst inst);

	RetTy visitLoad(LoadInst inst);

	RetTy visitStore(StoreInst inst);

	//==============================================//
	// other operators.
	//==============================================//
	RetTy visitCall(CallInst inst);

	RetTy visitGetElementPtr(GetElementPtrInst inst);

	RetTy visitPhiNode(PhiNode inst);
}
