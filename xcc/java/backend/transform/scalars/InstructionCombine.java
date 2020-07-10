package backend.transform.scalars;
/*
 * Extremely Compiler Collection
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

import backend.pass.AnalysisResolver;
import backend.pass.AnalysisUsage;
import backend.pass.FunctionPass;
import backend.support.DepthFirstOrder;
import backend.target.TargetData;
import backend.transform.scalars.instructionCombine.Combiner;
import backend.value.*;
import tools.Util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class InstructionCombine implements FunctionPass {
  private AnalysisResolver resolver;
  private Combiner combiner;
  private LinkedList<Instruction> worklist;
  private HashSet<Instruction> existed;
  private TargetData td;

  public InstructionCombine() {
    worklist = new LinkedList<>();
    existed = new HashSet<>();
  }

  private boolean collectReachableInsts(Function f) {
    BasicBlock entryBB = f.getEntryBlock();
    HashSet<BasicBlock> reachable = new HashSet<>(DepthFirstOrder.dfTraversal(entryBB));

    boolean everChanged = false;
    for (Iterator<BasicBlock> itr = f.getBasicBlockList().iterator(); itr.hasNext(); ) {
      BasicBlock bb = itr.next();
      if (!reachable.contains(bb)) {
        // delete the dead block.
        Util.assertion(bb.isUseEmpty(), "Unreachable block must no use");
        itr.remove();
        bb.eraseFromParent();
        everChanged = true;
        continue;
      }
      for (int i = 0, e = bb.size(); i < e; i++) {
        Instruction inst = bb.getInstAt(i);
        addToWorklist(inst);
      }
    }
    return everChanged;
  }

  public void addToWorklist(User u) {
    if (u instanceof Instruction) {
      Instruction inst = (Instruction) u;
      if (existed.add(inst))
        worklist.addLast(inst);
    }
  }

  public void addUserToWorklist(Instruction inst) {
    Util.assertion(inst != null, "inst shouldn't be null");
    for (Use u : inst.getUseList()) {
      addToWorklist(u.getUser());
    }
  }

  public Instruction replaceInstUsesWith(Instruction inst, Value newVal) {
    addUserToWorklist(inst);
    if (inst.equals(newVal)) {
      inst.replaceAllUsesWith(Value.UndefValue.get(inst.getType()));
      return inst;
    } else {
      inst.replaceAllUsesWith(newVal);
      return (Instruction) newVal;
    }
  }

  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    au.addRequired(TargetData.class);
  }

  public TargetData getTargetData() {
    return td;
  }

  private boolean doIterate(Function f) {
    boolean everChanged = collectReachableInsts(f);
    while (!worklist.isEmpty()) {
      Instruction inst = worklist.removeFirst();
      // There are three status returned,
      // (1). null indicates the specified instruction is dead
      // and should be removed from enclosing block.
      // (2). as same as inst indicates that no transformation was performed.
      // (3). Otherwise calling to replaceAllUsesWith() to update it.
      Value res = combiner.visit(inst);
      if (res == null) {
        //empty block.
      } else if (!res.equals(inst)) {
        inst.eraseFromParent();
        everChanged |= true;
      }
    }
    worklist.clear();
    existed.clear();
    return everChanged;
  }

  @Override
  public boolean runOnFunction(Function f) {
    if (f == null || f.empty()) return false;
    this.td = (TargetData) getAnalysisToUpDate(TargetData.class);
    combiner = new Combiner(this);

    boolean everChanged = false;
    while (true) {
      boolean localChanged = doIterate(f);
      if (!localChanged)
        break;
      everChanged = true;
    }
    return everChanged;
  }

  @Override
  public String getPassName() {
    return "Instruction Combiner";
  }

  @Override
  public AnalysisResolver getAnalysisResolver() {
    return resolver;
  }

  @Override
  public void setAnalysisResolver(AnalysisResolver resolver) {
    this.resolver = resolver;
  }

  public static FunctionPass createInstructionCombinePass() {
    return new InstructionCombine();
  }
}
