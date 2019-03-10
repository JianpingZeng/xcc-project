package backend.pass;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import backend.passManaging.FunctionPassManager;
import backend.passManaging.PassManager;
import backend.transform.ipo.AlwaysInliner;
import backend.transform.ipo.BasicInliner;
import backend.transform.scalars.*;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class PassCreator {
  public static void createStandardFunctionPasses(
      FunctionPassManager fpm, int optimizationLevel) {
    if (optimizationLevel > 0) {
      fpm.add(CFGSimplifyPass.createCFGSimplificationPass());

      if (optimizationLevel == 1)
        fpm.add(Mem2Reg.createPromoteMemoryToRegisterPass());
      else
        fpm.add(SROA.createScalarRreplacementOfAggregatePass());
      fpm.add(InstructionCombine.createInstructionCombinePass());
    }
  }

  public static Pass createFunctionInliningPass(int threshold) {
    return BasicInliner.createFunctionInlinePass(threshold);
  }

  /**
   * Creates an always inliner pass.
   *
   * @return
   */
  public static Pass createAlwaysInliningPass() {
    return AlwaysInliner.createAlwaysInlinerPass();
  }

  public static void createStandardModulePasses(PassManager pm,
                                                int optimizationLevel,
                                                boolean optimizeSize,
                                                boolean unrollLoops,
                                                Pass inliningPass) {
    if (optimizationLevel == 0) {
      if (inliningPass != null)
        pm.add(inliningPass);
    } else {
      // remove useless Basic block.
      pm.add(CFGSimplifyPass.createCFGSimplificationPass());
      // remove redundant alloca.
      pm.add(Mem2Reg.createPromoteMemoryToRegisterPass());

      pm.add(InstructionCombine.createInstructionCombinePass());
      pm.add(CFGSimplifyPass.createCFGSimplificationPass());

      if (inliningPass != null)
        pm.add(inliningPass);
      pm.add(SROA.createScalarRreplacementOfAggregatePass());
      pm.add(InstructionCombine.createInstructionCombinePass());
      pm.add(ConditionalPropagate.createCondPropagatePass());
      pm.add(TailCallElim.createTailCallElimination());
      pm.add(CFGSimplifyPass.createCFGSimplificationPass());
      pm.add(DCE.createDeadCodeEliminationPass());
      //pm.add(GVNPRE.createGVNPREPass());
      pm.add(LoopSimplify.createLoopSimplifyPass());
      //pm.add(LCSSA.createLCSSAPass());
      //pm.add(IndVarSimplify.createIndVarSimplifyPass());
      //pm.add(LICM.createLICMPass());
      //pm.add(LoopDeletion.createLoopDeletionPass());
      //pm.add(GVNPRE.createGVNPREPass());
      pm.add(SCCP.createSparseConditionalConstantPropagatePass());
      pm.add(DCE.createDeadCodeEliminationPass());
      pm.add(CFGSimplifyPass.createCFGSimplificationPass());
    }
  }
}
