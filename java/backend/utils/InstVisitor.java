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
				assert false : "Unknown instruction type encountered!";
				System.exit(-1);
			case Add:
				return visitAdd((Op2) inst);
			case FAdd:
				return visitFAdd((Op2) inst);
			case Sub:
				return visitSub((Op2) inst);
			case FSub:
				return visitFSub((Op2) inst);
			case Mul:
				return visitMul((Op2) inst);
			case FMul:
				return visitFMul((Op2) inst);
			case UDiv:
				return visitUDiv((Op2) inst);
			case SDiv:
				return visitSDiv((Op2) inst);
			case FDiv:
				return visitFDiv((Op2) inst);
			case URem:
				return visitURem((Op2) inst);
			case SRem:
				return visitSRem((Op2) inst);
			case FRem:
				return visitFRem((Op2) inst);
			case And:
				return visitAnd((Op2) inst);
			case Or:
				return visitOr((Op2) inst);
			case Xor:
				return visitXor((Op2) inst);
			case Shl:
				return visitShl((Op2) inst);
			case AShr:
				return visitAShr((Op2) inst);
			case LShr:
				return visitLShr((Op2) inst);
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
	RetTy visitAdd(Op2 inst);

	RetTy visitFAdd(Op2 inst);

	RetTy visitSub(Op2 inst);

	RetTy visitFSub(Op2 inst);

	RetTy visitMul(Op2 inst);

	RetTy visitFMul(Op2 inst);

	RetTy visitUDiv(Op2 inst);

	RetTy visitSDiv(Op2 inst);

	RetTy visitFDiv(Op2 inst);

	RetTy visitURem(Op2 inst);

	RetTy visitSRem(Op2 inst);

	RetTy visitFRem(Op2 inst);

	//===============================================//
	// bitwise operator.
	//===============================================//
	RetTy visitAnd(Op2 inst);

	RetTy visitOr(Op2 inst);

	RetTy visitXor(Op2 inst);

	//=============================================//
	// shift operators.
	//============================================//
	RetTy visitShl(Op2 inst);

	RetTy visitLShr(Op2 inst);

	RetTy visitAShr(Op2 inst);

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
