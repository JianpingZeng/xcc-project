package backend.transform.scalars.instructionCombine;

/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Jianping Zeng.
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

import backend.ir.AllocationInst;
import backend.ir.MallocInst;
import backend.ir.SelectInst;
import backend.support.LLVMContext;
import backend.target.TargetData;
import backend.transform.scalars.InstructionCombine;
import backend.type.*;
import backend.utils.InstVisitor;
import backend.value.*;
import backend.value.Instruction.*;
import backend.value.Value.UndefValue;
import tools.APInt;
import tools.Util;

import java.util.ArrayList;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class Combiner implements InstVisitor<Instruction> {
  private InstructionCombine com;
  private TargetData td;

  public Combiner(InstructionCombine com) {
    this.com = com;
    td = com.getTargetData();
  }

  /**
   * Transform the case that bitcast from integral pointer to
   * aggregate pointer, get rid of redundant bitcast instruction.
   *
   * @param inst
   * @return
   */
  @Override
  public Instruction visitBitCast(User inst) {
    Util.assertion(inst != null);
    BitCastInst bc = (BitCastInst) inst;
    // optimize the case like this:
    // %t = alloca %struct.Test
    // %1 = bitcast %struct.Test* %t to i64*
    // store i64 %0, i64* %1, align 1
    Value srcOp = bc.operand(0);
    if (!(srcOp instanceof AllocationInst))
      return null;
    // if no td available, we can't go on.
    if (td == null)
      return null;

    AllocationInst ai = (AllocationInst) srcOp;

    Type srcTy = ai.getType();
    Type destTy = bc.getType();

    if (srcTy.isPointerType() && destTy.isPointerType()) {
      Type srcEleTy = ((PointerType) srcTy).getElementType();
      Type destEleTy = ((PointerType) destTy).getElementType();
      if (!srcEleTy.isSized() || !destEleTy.isSized())
        return null;
      int srcEleTyAlign = td.getABITypeAlignment(srcEleTy);
      int destEleTyAlign = td.getABITypeAlignment(destEleTy);
      if (destEleTyAlign < srcEleTyAlign)
        return null;

      // If the allocation has multiple uses, only promote it if we are strictly
      // increasing the alignment of the resultant allocation.  If we keep it the
      // same, we open the door to infinite loops of various kinds.
      if (!ai.hasOneUses() && destEleTyAlign == srcEleTyAlign)
        return null;

      long srcEleTySize = td.getTypeSize(srcEleTy);
      long destEleTySize = td.getTypeSize(destEleTy);
      if (srcEleTySize == 0 || destEleTySize == 0)
        return null;
      if ((destEleTySize % srcEleTySize) != 0)
        return null;

      if (srcEleTy.isAggregateType() && destEleTy.isIntegerTy()) {
        int scale = (int) (srcEleTySize / destEleTySize);
        Value numElements = ai.operand(0);
        Value amt;
        if (scale == 1)
          amt = numElements;
        else {
          // If the allocation size is constant, form a constant mul expression
          amt = ConstantInt.get(LLVMContext.Int32Ty, scale);
          if (numElements instanceof ConstantInt) {
            amt = ConstantExpr.getMul((ConstantInt) amt, (ConstantInt) numElements);
          } else {
            Instruction tmp = BinaryOps.createMul(amt, numElements, "tmp");
            tmp.insertBefore(ai);
            amt = tmp;
          }
        }
        AllocationInst newAI;
        if (ai instanceof MallocInst)
          newAI = new MallocInst(destEleTy, amt, "tmp", ai.getAlignment(), ai);
        else
          newAI = new AllocaInst(destEleTy, amt, ai.getAlignment(), "tmp", ai);
        newAI.setName(ai.getName());

        // If the allocation has multiple real uses, insert a cast and change all
        // things that used it to use the new cast.  This will also hack on CI, but it
        // will die soon.
        if (!bc.isUseEmpty()) {
          com.addUserToWorklist(ai);
          CastInst cast = new BitCastInst(newAI, ai.getType(), "tmpcast");
          cast.insertBefore(ai);
          ai.replaceAllUsesWith(cast);
          ai.eraseFromParent();
        }
        return com.replaceInstUsesWith(bc, newAI);
      }
    }
    return null;
  }

  @Override
  public Instruction visitRet(User inst) {
    return null;
  }

  @Override
  public Instruction visitBr(User inst) {
    return null;
  }

  @Override
  public Instruction visitSwitch(User inst) {
    return null;
  }

  @Override
  public Instruction visitICmp(User inst) {
    return null;
  }

  @Override
  public Instruction visitFCmp(User inst) {
    return null;
  }

  @Override
  public Instruction visitLoad(User inst) {
    return null;
  }

  @Override
  public Instruction visitStore(User inst) {
    return null;
  }

  @Override
  public Instruction visitCall(User inst) {
    return null;
  }

  @Override
  public Instruction visitGetElementPtr(User inst) {
    Util.assertion(inst instanceof GetElementPtrInst);
    GetElementPtrInst gep = (GetElementPtrInst) inst;
    Value ptrOp = gep.operand(0);
    if (gep.getNumOfOperands() == 1)
      return com.replaceInstUsesWith(gep, ptrOp);
    boolean firstIndexIsZero = false;
    if (gep.operand(1) instanceof ConstantInt) {
      firstIndexIsZero = ((ConstantInt) gep.operand(1)).isNullValue();
    }
    if (gep.getNumOfOperands() == 2 && firstIndexIsZero)
      return com.replaceInstUsesWith(gep, ptrOp);

    // Checks to see if we can simplify:
    // X = bitcast A to struct T*
    // Y = gep X, <... constant index...>
    if (ptrOp instanceof BitCastInst) {
      BitCastInst bci = (BitCastInst) ptrOp;
      if (td != null && !(bci.operand(0) instanceof BitCastInst) &&
          gep.hasAllConstantIndices()) {
        Value gepRes = emitGEPOffset(gep, gep);
        if (!(gepRes instanceof ConstantInt))
          return null;

        ConstantInt offsetVal = (ConstantInt) gepRes;
        long offset = offsetVal.getSExtValue();
        if (offset == 0) {
          // If this GEP instruction doesn't move the pointer, just replace the GEP
          // with a bitcast of the real input to the dest type.
          if (bci.operand(0) instanceof AllocationInst) {
            Instruction i = visitBitCast(bci);
            if (i != null) {
              if (!i.equals(bci)) {
                i.setName(bci.getName());
                bci.eraseFromParent();
                com.replaceInstUsesWith(bci, i);
              }
              return gep;
            }
          }
          Instruction res = new BitCastInst(bci.operand(0), gep.getType(), "bitcast", gep);
          return com.replaceInstUsesWith(gep, res);
        }

        // Otherwise, if the offset is non-zero, we need to find out if there is a
        // field at Offset in 'A's type.  If so, we can pull the cast through the
        // GEP.
        ArrayList<Value> newIndices = new ArrayList<>();
        Type intTy = ((PointerType) bci.operand(0).getType()).getElementType();
        if (findElementAtOffset(intTy, offset, newIndices, td) != null) {
          Instruction newGEP = new GetElementPtrInst(bci.operand(0), newIndices, "gep.new");
          if (newGEP.getType().equals(gep.getType()))
            return gep;
          if (gep.getInbounds())
            ((GetElementPtrInst) newGEP).setInbounds(true);

          newGEP.insertBefore(gep);
          Instruction res = new BitCastInst(newGEP, gep.getType(), "bitcast", gep);
          return com.replaceInstUsesWith(gep, res);
        }
      }
    }
    return null;
  }

  private Type findElementAtOffset(Type ty,
                                   long offset,
                                   ArrayList<Value> newIndices,
                                   TargetData td) {
    if (td == null || !ty.isSized()) return null;

    Type intPtrTy = td.getIntPtrType();
    long firstIdx = 0;
    long tySize = td.getTypeAllocSize(ty);
    if (tySize != 0) {
      firstIdx = offset / tySize;
      offset -= firstIdx * tySize;
      if (offset < 0) {
        --firstIdx;
        offset += tySize;
        Util.assertion(offset == 0);
      }
      Util.assertion(offset < tySize, "out of range");
    }

    newIndices.add(ConstantInt.get(intPtrTy, firstIdx));
    while (offset != 0) {
      if (offset * 8 >= td.getTypeSizeInBits(ty))
        return null;
      if (ty instanceof StructType) {
        StructType sty = (StructType) ty;
        TargetData.StructLayout layout = td.getStructLayout(sty);
        Util.assertion(offset < layout.getSizeInBits(),
            "Offset must stay within the indexed type");
        int elt = layout.getElementContainingOffset(offset);
        newIndices.add(ConstantInt.get(LLVMContext.Int32Ty, elt));
        offset -= layout.getElementOffset(elt);
        ty = sty.getElementType(elt);
      } else if (ty instanceof ArrayType) {
        ArrayType aty = (ArrayType) ty;
        long eleSize = td.getTypeAllocSize(aty.getElementType());
        Util.assertion(eleSize != 0, "can't index into zero-sized array");
        newIndices.add(ConstantInt.get(intPtrTy, offset / eleSize));
        offset %= eleSize;
        ty = aty.getElementType();
      } else
        // otherwise, we can't index into the middle of this typeo.
        return null;
    }
    return ty;
  }

  private Value emitGEPOffset(GetElementPtrInst gep,
                              Instruction inst) {
    Type intPtrTy = td.getIntPtrType();
    GEPTypeIterator gepItr = new GEPTypeIterator(gep);

    Value result = ConstantInt.getNullValue(intPtrTy);
    int intPtrWidth = td.getPointerSizeInBits();
    long ptrSizeMask = ~0L >>> (64 - intPtrWidth);
    CompositeType ct = (CompositeType) gep.getPointerOperandType().getElementType();

    for (int i = 1, e = gep.getNumOfOperands(); i < e && gepItr.hasNext(); i++) {
      Value op = gep.operand(i);
      long size = td.getTypeAllocSize(gepItr.getIndexedType()) & ptrSizeMask;
      if (op instanceof ConstantInt) {
        ConstantInt ci = (ConstantInt) op;
        if (ci.isZero()) continue;

        if (gepItr.getCurType() instanceof StructType) {
          StructType sty = (StructType) gepItr.getCurType();
          size = td.getStructLayout(sty).getElementOffset(ci.getZExtValue());

          if (result instanceof ConstantInt) {
            ConstantInt rc = (ConstantInt) result;
            result = ConstantInt.get(rc.getValue().add(new APInt(intPtrWidth, size)));
          } else {
            result = BinaryOps.createAdd(result, ConstantInt.get(intPtrTy, size),
                gep.getName() + ".offs", gep);
          }
          continue;
        }

        Constant scale = ConstantInt.get(intPtrTy, size);
        Constant oc = ConstantExpr.getIntegerCast(ci, intPtrTy, true);
        scale = ConstantExpr.getMul(oc, scale);
        if (result instanceof Constant)
          result = ConstantExpr.getAdd((Constant) result, scale);
        else {
          // emit an add instruction.
          result = BinaryOps.createAdd(result, scale, gep.getName() + ".offs", gep);
        }
        continue;
      }
      if (!op.getType().equals(intPtrTy)) {
        if (op instanceof Constant) {
          Constant opC = (Constant) op;
          op = ConstantExpr.getIntegerCast(opC, intPtrTy, true);
        } else
          op = CastInst.createIntegerCast(op, intPtrTy, true,
              op.getName() + ".c", gep);
      }

      if (size != 1) {
        Constant scale = ConstantInt.get(intPtrTy, size);
        if (op instanceof Constant) {
          Constant opC = (Constant) op;
          op = ConstantExpr.getMul(scale, opC);
        } else
          op = BinaryOps.createMul(scale, op, gep.getName() + ".idx", gep);
      }

      // emit an add instruction.
      if (op instanceof Constant && result instanceof Constant) {
        result = ConstantExpr.getAdd((Constant) result, (Constant) op);
      } else
        result = BinaryOps.createAdd(result, op, op.getName() + ".add", gep);
    }
    return result;
  }

  @Override
  public Instruction visitPhiNode(User inst) {
    return null;
  }

  @Override
  public Instruction visitSelect(User inst) {
    return null;
  }

  /***
   * <pre>
   * Rank the specified value, order the value ascend from undef, const, unary, other inst.
   * 0 -> undef, 1 -> Const, 2 -> Other, 3 -> Arg, 3 -> Unary, 4 -> OtherInst
   * </pre>
   * @param val
   * @return
   */
  private int getComplexity(Value val) {
    if (val instanceof UndefValue) return 0;
    else if (val instanceof Constant) return 1;
    else if (val instanceof Instruction) {
      if (BinaryOps.isNot(val) || BinaryOps.isNeg(val) || BinaryOps.isFNeg(val))
        return 3;
      return 4;
    } else if (val instanceof Argument)
      return 3;
    else
      return 2;

  }

  /**
   * Reorder the expression list as the order from most complexity to less complexity.
   * 1. Transform: (op (op V, C1), C2) ==> (op V, (op C1, C2)).
   * 2. Transform: (op (op V1, C1), (op V2, C2)) ==> (op (op V1, V2), (op C1,C2)).
   *
   * @param inst
   * @return
   */
  private boolean simplifyCommutative(BinaryOps inst) {
    boolean changed = false;
    if (getComplexity(inst.operand(0)) < getComplexity(inst.operand(1)))
      changed = !inst.swapOperands();

    if (!inst.isAssociative()) return changed;
    Value op0 = inst.operand(0), op1 = inst.operand(1);
    if (op0 instanceof BinaryOps) {
      BinaryOps binOp0 = (BinaryOps) op0;
      if (binOp0.getOpcode() == inst.getOpcode()) {
        if (binOp0.operand(1) instanceof ConstantInt) {
          ConstantInt ci1 = (ConstantInt) binOp0.operand(1);
          if (op1 instanceof ConstantInt) {
            Constant foldedCE = ConstantExpr.get(inst.getOpcode(),
                ci1, (Constant) op1);
            inst.setOperand(0, binOp0.operand(0));
            inst.setOperand(1, foldedCE);
            changed = true;
          } else if (op1 instanceof BinaryOps &&
              ((BinaryOps) op1).getOpcode() == inst.getOpcode()) {
            BinaryOps binaryOp1 = (BinaryOps) op1;
            if (binaryOp1.operand(1) instanceof ConstantInt) {
              ConstantInt ci2 = (ConstantInt) binaryOp1.operand(1);
              inst.setOperand(0, BinaryOps.create(inst.getOpcode(),
                  binOp0.operand(0), binaryOp1.operand(0), "tmp", inst));
              inst.setOperand(1, ConstantExpr.get(inst.getOpcode(),
                  ci1, ci2));
              changed = true;
            }
          }
        }
      }
    }
    return changed;
  }

  @Override
  public Instruction visitAdd(User inst) {
    BinaryOps binOps = (BinaryOps) inst;
    boolean changed = simplifyCommutative(binOps);
    Value lhs = binOps.operand(0), rhs = binOps.operand(1);
    // any + C
    if (rhs instanceof Constant) {
      Constant rhsC = (Constant) rhs;
      // any + undef --> undef.
      if (rhs instanceof UndefValue)
        return com.replaceInstUsesWith(binOps, lhs);
      // X + 0 --> X
      if (rhs.isNullConstant())
        return com.replaceInstUsesWith(binOps, rhs);
      // zext(bool) + C --> bool ? C+1 : C.
      if (rhs instanceof ConstantInt) {
        ConstantInt ci = (ConstantInt) rhs;
        if (lhs instanceof ZExtInst &&
            ((ZExtInst) lhs).operand(0).hasOneUses() &&
            ((ZExtInst) lhs).operand(0).getType() == LLVMContext.Int1Ty) {
          Value cond = ((ZExtInst) lhs).operand(0);
          return com.replaceInstUsesWith(binOps, new SelectInst(cond,
              ConstantExpr.getAdd(ci, ConstantInt.getTrue()), ci, "", binOps));
        }
      }
      if (lhs instanceof Constant) {
        Constant lhsC = (Constant) lhs;
        return com.replaceInstUsesWith(binOps, ConstantExpr.getAdd(lhsC, rhsC));
      }
      // simplify it with pattern matching technology.
    }
    return null;
  }

  @Override
  public Instruction visitFAdd(User inst) {
    return null;
  }

  @Override
  public Instruction visitSub(User inst) {
    return null;
  }

  @Override
  public Instruction visitFSub(User inst) {
    return null;
  }

  @Override
  public Instruction visitMul(User inst) {
    return null;
  }

  @Override
  public Instruction visitFMul(User inst) {
    return null;
  }

  @Override
  public Instruction visitFDiv(User inst) {
    return null;
  }
}
