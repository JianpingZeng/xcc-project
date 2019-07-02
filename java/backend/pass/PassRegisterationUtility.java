/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.pass;

import backend.analysis.*;
import backend.analysis.aa.AliasAnalysis;
import backend.codegen.*;
import backend.codegen.dagisel.RegisterScheduler;
import backend.codegen.dagisel.ScheduleDAGFast;
import backend.codegen.linearscan.SimpleRegisterCoalescer;
import backend.codegen.linearscan.WimmerLinearScanRegAllocator;
import backend.support.*;
import backend.target.TargetData;
import backend.transform.ipo.AlwaysInliner;
import backend.transform.ipo.BasicInliner;
import backend.transform.ipo.RaiseAllocations;
import backend.transform.scalars.*;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public final class PassRegisterationUtility {
  /**
   * This method is helpful for registering all of passes into Pass DataBase.
   */
  public static void registerPasses() {
    // Analysis passes.
    new RegisterPass("Live Interval Analysis", "liveintervals", LiveIntervalAnalysis.class);
    new RegisterPass("Dominance Frontier Construction", "domfrontier", DominanceFrontier.class, true, true);
    new RegisterPass("Dominator Tree Construction", "domtree", DomTree.class, true, true);
    new RegisterPass("Induction Variable Users", "iv-users", IVUsers.class, false, true);
    new RegisterPass("Live Variable Analysis", "livevars", LiveVariables.class);
    new RegisterPass("Natural Loop Information", "loops", LoopInfo.class, true, true);
    new RegisterPass("Machine Dominator Tree Construction", "machinedomtree", MachineDomTree.class, true, true);
    new RegisterPass("Machine Natural Loop Construction", "machine-loops", MachineLoopInfo.class, true, true);
    new RegisterPass("Scalar Evolution Analysis", "scalar-evolution", ScalarEvolution.class, false, true);
    new RegisterPass("Print out Dom tree into dot file", "dot-domtree", DomTreePrinter.class, false, true);
    new RegisterPass("Print out cfg into dot file", "dot-cfg", CFGPrinter.class, false, true);
    new RegisterPass("Alias Analysis Pass", "alias-analysis", AliasAnalysis.class, false, true);
    new RegisterPass("Live interval Analysis for wimmer style ra", "wimmer-li",
        backend.codegen.linearscan.LiveIntervalAnalysis.class);
    new RegisterPass("Basic Call Graph Analysis", "call-graph", CallGraph.class, false, true);
    new RegisterPass("Basic Function Inlining/Integration", "inline", BasicInliner.class, false, false);
    new RegisterPass("Inliner for always_inline functions", "always-inline", AlwaysInliner.class, false, false);

    // IPO transformation
    new RegisterPass("Raise allocations from calls to instructions", "raiseallocs", RaiseAllocations.class);

    // Scalar transformation
    new RegisterPass("Break critical edges in CFG", "break-crit-edges", BreakCriticalEdge.class);
    new RegisterPass("Simplify the CFG", "simplifycfg", CFGSimplifyPass.class);
    new RegisterPass("Conditional Propagation", "condprop", ConditionalPropagate.class);
    new RegisterPass("Simple constant propagation", "constprop", ConstantPropagation.class);
    new RegisterPass("Dead Instruction Elimination", "die", DCE.class);
    new RegisterPass("Global Value Numbering/Partial Redundancy Elimination", "gvnpre", GVNPRE.class);
    new RegisterPass("Canonicalize Induction Variables[Morgen textbook]", "indvarsv1", InductionVarSimplify.class);
    new RegisterPass("Canonicalize Induction Variables", "indvars", IndVarSimplify.class);
    new RegisterPass("Loop closed SSA", "lcssa", LCSSA.class);
    new RegisterPass("Loop Invariant Code Motion", "licm", LICM.class);
    new RegisterPass("Delete dead loops", "loop-deletion", LoopDeletion.class);
    new RegisterPass("Loop Simplification Pass", "loopsimplify", LoopSimplify.class);
    new RegisterPass("Lower switch with branch", "lowerswitch", LowerSwitch.class);
    new RegisterPass("Promote memory to Register", "mem2reg", Mem2Reg.class);
    new RegisterPass("Sparse Conditional Constant Propagation", "sccp", SCCP.class);
    new RegisterPass("Scalar Replacement of Aggregates", "scalarrepl", SROA.class);
    new RegisterPass("Tail Call Elimination", "tailcallelim", TailCallElim.class);
    new RegisterPass("Unify function exit nodes", "mergereturn", UnifyFunctionExitNodes.class);
    new RegisterPass("Remove unreachable basic block from CFG", "unreachableblockelim", UnreachableBlockElim.class);
    new RegisterPass("Combine redundant instruction", "instcombine", InstructionCombine.class, false, false);

    // Machine Function Passes
    new RegisterPass("Eliminate PHI nodes for register allocation", "phi-node-elimination", PhiElimination.class);
    new RegisterPass("Prologue/Epilogue Insertion", "prologepilog", PrologEpilogInserter.class);
    new RegisterPass("Two-Address instruction pass", "twoaddressinstruction", TwoAddrInstructionPass.class);
    new RegisterPass("Remove unreachable machine blocks from the machine CFG", "unreachable-machineblockelim", UnreachableMachineBlockElim.class);
    new RegisterPass("Machine Module Information", "machine-module=info", MachineModuleInfo.class);
    new RegisterPass("Rearragement machine basic blocks in Function to reduce useless branch", "rearragementmbbs", RearrangementMBB.class);

    // Register X86 dag isel by reflect mechanism.
    try {
      new RegisterPass("Instruction Selector based on DAG covering", "dag-isel",
          Class.forName("backend.target.x86.X86GenDAGISel").asSubclass(Pass.class));
    } catch (ClassNotFoundException e) {
      ErrorHandling.llvmReportError("Fail to register X86GenDAGISel pass");
    }

    new RegisterRegAlloc("linearscan", "Linear Scan Register Allocation", RegAllocLinearScan::createLinearScanRegAllocator);
    new RegisterRegAlloc("local", "Local register allocator", RegAllocLocal::createLocalRegAllocator);
    new RegisterRegAlloc("pbqp", "PBQP Register Allocator", RegAllocPBQP::createPBQPRegisterAllocator);
    new RegisterRegAlloc("wimmer", "Wimmer-Style Linear scan register allocator", WimmerLinearScanRegAllocator::createWimmerLinearScanRegAlloc);

    // Register scheduler.
    new RegisterScheduler("fast", "Fast Instruction Scheduler", ScheduleDAGFast::createFastDAGScheduler);


    new RegisterPass("register-coalescing", "Simple register coalescer", LiveIntervalCoalescing.class);
    new RegisterPass("simple-register-coalescer", "Simple Register Coalescer", SimpleRegisterCoalescer.class);

    new RegisterPass("live-stack-slot", "Live Analysis of Stack Slot", LiveStackSlot.class);
    new RegisterPass("edge-bundles", "Bundle Machine CFG Edges", EdgeBundles.class);
    new RegisterPass("dead-mi-elimination", "Remove dead machine instruction", DeadMachineInstructionElim.class);

    new RegisterPass("Verify generated machine code", "machine-verifier", MachineCodeVerifier.class);

    // Immutable Passes.
    new RegisterPass("Target Data Layout", "targetdata", TargetData.class, false, true);

    new RegisterPass("Print Module to stderr", "print-module", PrintModulePass.class, false, true);
    new RegisterPass("Print Function to stderr", "print-function", PrintFunctionPass.class, true, true);
    new RegisterPass("Print Basic Block to stderr", "print-bb", PrintBasicBlockPass.class, true, true);
    new RegisterPass("Print out Machine Function to stderr", "print-machine-function", PrintMachineFunctionPass.class, true, true);
    new RegisterPass("Print Loop to stderr", "print-loop", PrintLoopPass.class, true, true);
  }
}
