package backend.transform.utils;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
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

import backend.target.TargetData;
import backend.value.*;
import backend.value.Instruction.*;
import backend.value.Value.UndefValue;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import static backend.transform.utils.ConstantFolder.*;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public final class PruningFunctionCloner {
  private Function caller;
  private final Function callee;
  private HashMap<Value, Value> valueMap;
  private ArrayList<ReturnInst> returns;
  private String nameSuffix;
  private ClonedCodeInfo codeInfo;
  private TargetData td;
  private Value dbgFnStart;

  private PruningFunctionCloner(Function caller,
                                Function callee,
                                HashMap<Value, Value> valueMap,
                                ArrayList<ReturnInst> returns,
                                String suffix,
                                ClonedCodeInfo codeInfo,
                                TargetData td) {
    this.caller = caller;
    this.callee = callee;
    this.valueMap = valueMap;
    this.returns = returns;
    this.nameSuffix = suffix;
    this.codeInfo = codeInfo;
    this.td = td;
    dbgFnStart = null;
  }

  private void cloneBlock(BasicBlock bb, LinkedList<BasicBlock> toClone) {
    if (valueMap.containsKey(bb))
      return;

    BasicBlock newBB = BasicBlock.createBasicBlock();
    newBB.setName(bb.getName() + nameSuffix);
    valueMap.put(bb, newBB);
    boolean hasCalls = false, hasDynamicAllocas = false,
        hasStaticAllocas = false;

    for (Instruction inst : bb) {
      // skips terminator inst.
      if (inst instanceof TerminatorInst)
        break;
      Constant c = constantFoldMappedInstruction(inst, td);
      if (c != null) {
        valueMap.put(inst, c);
        continue;
      }

      // TODO 8/15/2018, Handling llvm.dbg.region.end and llvm.dbg.region.start.
      Instruction newInst = inst.clone();
      if (inst.hasName())
        newInst.setName(inst.getName() + nameSuffix);

      newBB.appendInst(newInst);
      valueMap.put(inst, newInst);

      hasCalls |= (inst instanceof CallInst) && !(inst instanceof IntrinsicInst.DbgInfoIntrinsic);
      if (inst instanceof AllocaInst) {
        AllocaInst ai = (AllocaInst) inst;
        if (ai.getArraySize() instanceof ConstantInt)
          hasStaticAllocas = true;
        else
          hasDynamicAllocas = true;
      }
    }
    // Finally, clone over the terminator.
    TerminatorInst ti = bb.getTerminator();
    boolean terminatorDone = false;
    if (ti instanceof BranchInst) {
      BranchInst br = (BranchInst) ti;
      if (br.isConditional()) {
        ConstantInt cond = br.getCondition() instanceof ConstantInt ?
            (ConstantInt) br.getCondition() : null;
        if (cond == null) {
          Value res = valueMap.get(br.getCondition());
          cond = res instanceof ConstantInt ?
              (ConstantInt) res : null;
        }

        if (cond != null) {
          BasicBlock target = br.getSuccessor(cond.getZExtValue() != 0 ? 0 : 1);
          valueMap.put(br, new BranchInst(target, newBB));
          toClone.add(target);
          terminatorDone = true;
        }
      }
    } else if (ti instanceof SwitchInst) {
      SwitchInst si = (SwitchInst) ti;
      ConstantInt cond = si.getCondition() instanceof ConstantInt ?
          (ConstantInt) si.getCondition() : null;
      if (cond == null) {
        Value res = valueMap.get(si.getCondition());
        cond = res instanceof ConstantInt ?
            (ConstantInt) res : null;
      }
      if (cond != null) {
        BasicBlock target = si.getSuccessor(si.findCaseValue(cond));
        valueMap.put(si, new BranchInst(target, newBB));
        toClone.add(target);
        terminatorDone = true;
      }
    }

    if (!terminatorDone) {
      Instruction newTI = ti.clone();
      if (ti.hasName())
        newTI.setName(ti.getName() + nameSuffix);
      newBB.appendInst(newTI);
      valueMap.put(ti, newTI);

      // Recursively clone any reachable successor blocks.
      for (int i = 0, e = ti.getNumOfSuccessors(); i < e; i++)
        toClone.add(ti.getSuccessor(i));
    }

    if (codeInfo != null) {
      codeInfo.containsCalls |= hasCalls;
      codeInfo.containsDynamicAllocas |= hasDynamicAllocas;
      codeInfo.containsDynamicAllocas |= hasStaticAllocas &&
          bb != bb.getParent().getEntryBlock();
    }

    if (ti instanceof ReturnInst)
      returns.add((ReturnInst) ti);
  }

  private Constant constantFoldMappedInstruction(Instruction inst, TargetData td) {
    ArrayList<Constant> ops = new ArrayList<>();
    for (int i = 0, e = inst.getNumOfOperands(); i < e; i++) {
      Value res = valueMap.get(inst.operand(i));
      if (res instanceof Constant)
        ops.add((Constant) res);
      else
        return null;
    }
    if (inst instanceof CmpInst) {
      CmpInst ci = (CmpInst) inst;
      return constantFoldCompareInstOperands(ci.getPredicate(), ops, td);
    }
    if (inst instanceof LoadInst) {
      LoadInst ld = (LoadInst) inst;
      if (ops.get(0) instanceof ConstantExpr) {
        ConstantExpr ce = (ConstantExpr) ops.get(0);
        if (!ld.isVolatile() && ce.getOpcode() == Operator.GetElementPtr) {
          if (ce.operand(0) instanceof GlobalVariable) {
            GlobalVariable gv = (GlobalVariable) ce.operand(0);
            if (gv.isConstant() && gv.hasDefinitiveInitializer())
              return constantFoldLoadThroughGEPConstantExpr(gv.getInitializer(),
                  ce);
          }
        }
      }
    }
    return constantFoldInstOperands(inst.getOpcode(), inst.getType(), ops, td);
  }

  public static ClonedCodeInfo cloneAndPruneFunctionInfo(
      Function caller,
      Function callee,
      HashMap<Value, Value> valueMap,
      ArrayList<ReturnInst> returns,
      String nameSuffix,
      TargetData td) {
    Util.assertion(nameSuffix != null, "nameSuffix can not be null!");
    ClonedCodeInfo codeInfo = new ClonedCodeInfo();
    PruningFunctionCloner cloner = new PruningFunctionCloner(caller, callee, valueMap,
        returns, nameSuffix, codeInfo, td);
    LinkedList<BasicBlock> cloneWorklist = new LinkedList<>();
    cloneWorklist.addLast(callee.getEntryBlock());
    while (!cloneWorklist.isEmpty()) {
      BasicBlock bb = cloneWorklist.removeLast();
      cloner.cloneBlock(bb, cloneWorklist);
    }

    // Loop over all of the basic blocks in the old function.  If the block was
    // reachable, we have cloned it and the old block is now in the value map:
    // insert it into the new function in the right order.  If not, ignore it.
    //
    // Defer PHI resolution until rest of function is resolved.
    ArrayList<PhiNode> phinodes = new ArrayList<>();
    for (BasicBlock oldBB : callee) {
      if (!valueMap.containsKey(oldBB))
        continue;

      BasicBlock newBB = (BasicBlock) valueMap.get(oldBB);
      caller.addBasicBlock(newBB);

      // Specially handling to phinodes in old basic block.
      if (newBB.getFirstInst() instanceof PhiNode) {
        int i = 0;
        for (Instruction inst : newBB) {
          if (!(inst instanceof PhiNode))
            break;
          phinodes.add((PhiNode) oldBB.getInstAt(i));
          ++i;
        }
      }
      // handle normal instruction.
      for (Instruction inst : newBB)
        remapInstruction(inst, valueMap);
    }

    // Handle specially phi node, because it's incoming value would be changed
    // after cloning callee into caller function. So we need to carefully keep
    // it update
    if (!phinodes.isEmpty()) {
      for (int i = 0, size = phinodes.size(); i < size; ) {
        PhiNode oldPN = phinodes.get(i);
        BasicBlock oldBB = oldPN.getParent();
        BasicBlock newBB = (BasicBlock) valueMap.get(oldBB);
        Util.assertion(newBB != null, "no value in valueMap");

        int numPreds = oldBB.getNumPredecessors();
        // update any incoming value from predecessor caused by updating CFG.
        for (; i < size && phinodes.get(i).getParent() == oldBB; i++) {
          oldPN = phinodes.get(i);
          PhiNode newPN = (PhiNode) valueMap.get(oldPN);
          Util.assertion(newPN != null, "illegal null reference to newPN");
          for (int pred = 0; pred < numPreds; ++pred) {
            Value temp = valueMap.get(oldPN.getIncomingBlock(pred));
            if (temp != null) {
              BasicBlock newPred = (BasicBlock) temp;
              newPN.setIncomingBlock(pred, newPred);
              newPN.setIncomingValue(pred, valueMap.get(oldPN.getIncomingValue(pred)));
            } else {
              newPN.removeIncomingValue(pred, false);
              --pred;
              --numPreds;
            }
          }
        }

        // clear the phinode in new basic block if there are duplicate entry in PHINode
        // from same predecessor block.
        if (newBB.getFirstInst() instanceof PhiNode) {
          PhiNode pn = (PhiNode) newBB.getFirstInst();
          if (pn.getNumberIncomingValues() != numPreds) {
            TObjectIntHashMap<BasicBlock> predCounts = new TObjectIntHashMap<BasicBlock>();
            for (int j = 0, e = newBB.getNumPredecessors(); j < e; j++) {
              BasicBlock pred = newBB.predAt(j);
              predCounts.put(pred, predCounts.get(pred) - 1);
            }
            for (int j = 0, e = pn.getNumberIncomingValues(); j < e; j++) {
              BasicBlock pred = pn.getIncomingBlock(j);
              predCounts.put(pred, predCounts.get(pred) + 1);
            }

            for (Instruction inst : newBB) {
              if (!(inst instanceof PhiNode)) break;
              pn = (PhiNode) inst;
              for (int j = 0, e = pn.getNumberIncomingValues(); j < e; j++) {
                BasicBlock pred = pn.getIncomingBlock(j);
                int numToBeReduced = predCounts.get(pred);
                Util.assertion(numToBeReduced >= 0, "Shouldn't be negative");
                while (numToBeReduced != 0) {
                  pn.removeIncomingValue(pred, false);
                  --numToBeReduced;
                }
              }
            }
          }
        }

        // substitute UndefValue for those phi nodes without incoming value
        // from predecessors.
        for (int j = 0, e = newBB.size(); j < e && newBB.getInstAt(j) instanceof PhiNode; ++j) {
          PhiNode pn = (PhiNode) newBB.getInstAt(j);
          oldPN = (PhiNode) oldBB.getInstAt(j);
          if (pn.getNumberIncomingValues() == 0) {
            Value res = UndefValue.get(pn.getType());
            pn.replaceAllUsesWith(res);
            pn.eraseFromParent();
            Util.assertion(valueMap.get(oldPN).equals(pn), "valueMap mismatch!");
            valueMap.put(oldPN, res);
          }
        }
      }
    }

    // OK, if we get here, merge a basic block into it's predecessor if
    // predecessor directly unconditional branch to current block and
    // block has only one predecessor.
    for (int i = 0, e = caller.size(); i < e; i++) {
      BasicBlock bb = callee.getBlockAt(i);
      TerminatorInst ti = bb.getTerminator();
      // Ignores those conditional branch.
      if (ti == null || !(ti instanceof BranchInst) ||
          ((BranchInst) ti).isConditional())
        continue;
      BranchInst br = (BranchInst) ti;
      BasicBlock singlePred = bb.getSinglePredecessor();
      if (singlePred == null || singlePred.getNumSuccessors() != 1
          || !bb.hasNoPhiNode())
        continue;

      // delete this branch instruction.
      br.eraseFromParent();
      singlePred.appendInst(bb.getInstList());
      bb.replaceAllUsesWith(singlePred);
      bb.eraseFromParent();
    }
    return codeInfo;
  }

  private static void remapInstruction(Instruction inst, HashMap<Value, Value> valueMap) {
    if (inst == null || valueMap == null)
      return;
    for (int i = 0, e = inst.getNumOfOperands(); i < e; i++) {
      Value op = inst.operand(i);
      Value val = mapValue(op, valueMap);
      Util.assertion(val != null, "Referenced value is not in valueMap");
      inst.setOperand(i, val);
    }
  }

  private static Value mapValue(Value val, HashMap<Value, Value> valueMap) {
    if (valueMap.containsKey(val))
      return valueMap.get(val);

    if (val instanceof GlobalValue || val instanceof MDString ||
        val instanceof MDNode || val instanceof NamedMDNode ||
        val instanceof ConstantInt || val instanceof ConstantFP ||
        val instanceof ConstantPointerNull || val instanceof ConstantAggregateZero ||
        val instanceof UndefValue) {
      valueMap.put(val, val);
      return val;
    }
    if (val instanceof Constant) {
      if (val instanceof ConstantArray) {
        ConstantArray ca = (ConstantArray) val;
        int e = ca.getNumOfOperands();
        Constant[] c = new Constant[e];
        for (int i = 0; i < e; i++) {
          Value oldVal = ca.operand(i);
          Value newVal = mapValue(oldVal, valueMap);
          if (newVal != null && !newVal.equals(oldVal)) {
            for (int j = 0; j < i; j++)
              c[j] = ca.operand(j);
            c[i] = (Constant) newVal;
            for (int j = i + 1; j < e; j++)
              c[j] = ca.operand(j);
            Constant res = ConstantArray.get(ca.getType(), c);
            valueMap.put(val, res);
            return res;
          }
        }
      } else if (val instanceof ConstantStruct) {
        ConstantStruct cs = (ConstantStruct) val;
        int e = cs.getNumOfOperands();
        Constant[] c = new Constant[e];
        for (int i = 0; i < e; i++) {
          Value oldVal = cs.operand(i);
          Value newVal = mapValue(oldVal, valueMap);
          if (newVal != null && !newVal.equals(oldVal)) {
            for (int j = 0; j < i; j++)
              c[j] = cs.operand(j);
            c[i] = (Constant) newVal;
            for (int j = i + 1; j < e; j++)
              c[j] = cs.operand(j);
            Constant res = ConstantStruct.get(cs.getType(), c);
            valueMap.put(val, res);
            return res;
          }
        }
      } else if (val instanceof ConstantVector) {
        ConstantVector cv = (ConstantVector) val;
        int e = cv.getNumOfOperands();
        Constant[] c = new Constant[e];
        for (int i = 0; i < e; i++) {
          Value oldVal = cv.operand(i);
          Value newVal = mapValue(oldVal, valueMap);
          if (newVal != null && !newVal.equals(oldVal)) {
            for (int j = 0; j < i; j++)
              c[j] = cv.operand(j);
            c[i] = (Constant) newVal;
            for (int j = i + 1; j < e; j++)
              c[j] = cv.operand(j);
            Constant res = ConstantVector.get(cv.getType(), c);
            valueMap.put(val, res);
            return res;
          }
        }
      } else
        Util.assertion(false, "Unknown constant!");
    }
    // Otherwise we return a null pointer.
    return null;
  }
}
