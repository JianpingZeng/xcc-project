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
				return visitAdd((BinaryInstruction) inst);
			case FAdd:
				return visitFAdd((BinaryInstruction) inst);
			case Sub:
				return visitSub((BinaryInstruction) inst);
			case FSub:
				return visitFSub((BinaryInstruction) inst);
			case Mul:
				return visitMul((BinaryInstruction) inst);
			case FMul:
				return visitFMul((BinaryInstruction) inst);
			case UDiv:
				return visitUDiv((BinaryInstruction) inst);
			case SDiv:
				return visitSDiv((BinaryInstruction) inst);
			case FDiv:
				return visitFDiv((BinaryInstruction) inst);
			case URem:
				return visitURem((BinaryInstruction) inst);
			case SRem:
				return visitSRem((BinaryInstruction) inst);
			case FRem:
				return visitFRem((BinaryInstruction) inst);
			case And:
				return visitAnd((BinaryInstruction) inst);
			case Or:
				return visitOr((BinaryInstruction) inst);
			case Xor:
				return visitXor((BinaryInstruction) inst);
			case Shl:
				return visitShl((BinaryInstruction) inst);
			case AShr:
				return visitAShr((BinaryInstruction) inst);
			case LShr:
				return visitLShr((BinaryInstruction) inst);
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
	RetTy visitAdd(BinaryInstruction inst);

	RetTy visitFAdd(BinaryInstruction inst);

	RetTy visitSub(BinaryInstruction inst);

	RetTy visitFSub(BinaryInstruction inst);

	RetTy visitMul(BinaryInstruction inst);

	RetTy visitFMul(BinaryInstruction inst);

	RetTy visitUDiv(BinaryInstruction inst);

	RetTy visitSDiv(BinaryInstruction inst);

	RetTy visitFDiv(BinaryInstruction inst);

	RetTy visitURem(BinaryInstruction inst);

	RetTy visitSRem(BinaryInstruction inst);

	RetTy visitFRem(BinaryInstruction inst);

	//===============================================//
	// bitwise operator.
	//===============================================//
	RetTy visitAnd(BinaryInstruction inst);

	RetTy visitOr(BinaryInstruction inst);

	RetTy visitXor(BinaryInstruction inst);

	//=============================================//
	// shift operators.
	//============================================//
	RetTy visitShl(BinaryInstruction inst);

	RetTy visitLShr(BinaryInstruction inst);

	RetTy visitAShr(BinaryInstruction inst);

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
