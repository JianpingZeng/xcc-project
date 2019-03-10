package backend.utils;

import backend.value.BasicBlock;
import backend.value.Function;
import backend.value.Instruction;
import backend.value.User;
import tools.Util;

import java.util.List;

/**
 * A Value Visitor for Instruction or Value using Visitor pattern.
 *
 * @author Jianping Zeng
 */
public interface InstVisitor<RetTy> {
  default void visit(List<BasicBlock> blocks) {
    for (BasicBlock bb : blocks)
      visit(bb);
  }

  default void visit(BasicBlock bb) {
    for (Instruction inst : bb.getInstList())
      visit(inst);
  }

  default void visit(Function f) {
    visit(f.getBasicBlockList());
  }

  default RetTy visit(Instruction inst) {
    switch (inst.getOpcode()) {
      default:
        Util.assertion(false, "Undefined instruction type encountered!");
        return null;
      case None:
        break;
      case Ret:
        break;
      case Br:
        break;
      case Switch:
        break;
      case Unreachable:
        break;
      case Alloca:
        break;
      case Free:
        break;
      case Malloc:
        break;
      case Store:
        break;
      case Load:
        break;
      case Phi:
        break;
      case Call:
        break;
      case GetElementPtr:
        return visitGetElementPtr(inst);
      case Select:
        break;
      case Add:
        return visitAdd(inst);
      case FAdd:
        return visitFAdd(inst);
      case Sub:
        return visitSub(inst);
      case FSub:
        return visitFSub(inst);
      case Mul:
        return visitMul(inst);
      case FMul:
        return visitFMul(inst);
      case UDiv:
        return visitUDiv(inst);
      case SDiv:
        return visitSDiv(inst);
      case FDiv:
        return visitFDiv(inst);
      case URem:
        return visitURem(inst);
      case SRem:
        return visitSRem(inst);
      case FRem:
        return visitFRem(inst);
      case And:
        return visitAnd(inst);
      case Or:
        return visitOr(inst);
      case Xor:
        return visitXor(inst);
      case Shl:
        return visitShl(inst);
      case AShr:
        return visitAShr(inst);
      case LShr:
        return visitLShr(inst);
      case ICmp:
        return visitICmp(inst);
      case FCmp:
        return visitFCmp(inst);
      case Trunc:
        return visitTrunc(inst);
      case ZExt:
        return visitZExt(inst);
      case SExt:
        return visitSExt(inst);
      case FPToUI:
        return visitFPToUI(inst);
      case FPToSI:
        return visitFPToSI(inst);
      case UIToFP:
        return visitUIToFP(inst);
      case SIToFP:
        return visitSIToFP(inst);
      case FPTrunc:
        return visitFPTrunc(inst);
      case FPExt:
        return visitFPExt(inst);
      case PtrToInt:
        return visitPtrToInt(inst);
      case IntToPtr:
        return visitIntToPtr(inst);
      case BitCast:
        return visitBitCast(inst);
    }
    return null;
  }

  //==============================================//
  // branch instr.
  //==============================================//
  RetTy visitRet(User inst);

  RetTy visitBr(User inst);

  RetTy visitSwitch(User inst);

  //===============================================//
  // arithmetic instr.
  //===============================================//
  default RetTy visitAdd(User inst) {
    return visitBinaryOp(inst);
  }

  default RetTy visitFAdd(User inst) {
    return visitBinaryOp(inst);
  }

  default RetTy visitSub(User inst) {
    return visitBinaryOp(inst);
  }

  default RetTy visitFSub(User inst) {
    return visitBinaryOp(inst);
  }

  default RetTy visitMul(User inst) {
    return visitBinaryOp(inst);
  }

  default RetTy visitFMul(User inst) {
    return visitBinaryOp(inst);
  }

  default RetTy visitUDiv(User inst) {
    return visitBinaryOp(inst);
  }

  default RetTy visitSDiv(User inst) {
    return visitBinaryOp(inst);
  }

  default RetTy visitFDiv(User inst) {
    return visitBinaryOp(inst);
  }

  default RetTy visitURem(User inst) {
    return visitBinaryOp(inst);
  }

  default RetTy visitSRem(User inst) {
    return visitBinaryOp(inst);
  }

  default RetTy visitFRem(User inst) {
    return visitBinaryOp(inst);
  }

  default RetTy visitBinaryOp(User inst) {
    return null;
  }

  //===============================================//
  // bitwise operator.
  //===============================================//
  default RetTy visitAnd(User inst) {
    return visitBinaryOp(inst);
  }

  default RetTy visitOr(User inst) {
    return visitBinaryOp(inst);
  }

  default RetTy visitXor(User inst) {
    return visitBinaryOp(inst);
  }

  //=============================================//
  // shift operators.
  //============================================//
  default RetTy visitShl(User inst) {
    return visitBinaryOp(inst);
  }

  default RetTy visitLShr(User inst) {
    return visitBinaryOp(inst);
  }

  default RetTy visitAShr(User inst) {
    return visitBinaryOp(inst);
  }

  //===============================================//
  // comparison instr.
  //===============================================//
  RetTy visitICmp(User inst);

  RetTy visitFCmp(User inst);

  //==============================================//
  // cast operators.
  //=============================================//
  default RetTy visitTrunc(User inst) {
    return visitCastInst(inst);
  }

  default RetTy visitZExt(User inst) {
    return visitCastInst(inst);
  }

  default RetTy visitSExt(User inst) {
    return visitCastInst(inst);
  }

  default RetTy visitFPToUI(User inst) {
    return visitCastInst(inst);
  }

  default RetTy visitFPToSI(User inst) {
    return visitCastInst(inst);
  }

  default RetTy visitUIToFP(User inst) {
    return visitCastInst(inst);
  }

  default RetTy visitSIToFP(User inst) {
    return visitCastInst(inst);
  }

  default RetTy visitFPTrunc(User inst) {
    return visitCastInst(inst);
  }

  default RetTy visitFPExt(User inst) {
    return visitCastInst(inst);
  }

  default RetTy visitPtrToInt(User inst) {
    return visitCastInst(inst);
  }

  default RetTy visitIntToPtr(User inst) {
    return visitCastInst(inst);
  }

  default RetTy visitBitCast(User inst) {
    return visitCastInst(inst);
  }

  default RetTy visitCastInst(User inst) {
    return null;
  }

  //===============================================//
  // memory operator.
  //===============================================//
  default RetTy visitAlloca(User inst) {
    return visitAllocationInst(inst);
  }

  default RetTy visitMalloc(User inst) {
    return visitAllocationInst(inst);
  }

  default RetTy visitAllocationInst(User inst) {
    return null;
  }

  default RetTy visitFree(User inst) {
    return null;
  }

  RetTy visitLoad(User inst);

  RetTy visitStore(User inst);

  //==============================================//
  // other operators.
  //==============================================//
  RetTy visitCall(User inst);

  RetTy visitGetElementPtr(User inst);

  RetTy visitPhiNode(User inst);

  /**
   * Visit Select instruciton in LLVM IR programming reference.
   *
   * @param inst
   * @return
   */
  RetTy visitSelect(User inst);

  default RetTy visitExtractElementInst(User inst) { return null; }

  default RetTy visitInsertElementInst(User inst) { return null; }

  default RetTy visitShuffleVectorInst(User inst) { return null; }

  default RetTy visitExtractVectorInst(User inst) { return null; }

  default RetTy visitInsertValueInst(User inst) { return null; }
}
