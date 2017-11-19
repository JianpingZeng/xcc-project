/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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
import backend.codegen.*;
import backend.support.ErrorHandling;
import backend.target.TargetData;
import backend.transform.ipo.RaiseAllocations;
import backend.transform.scalars.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class PassRegisterationUtility
{
    /**
     * This method is helpful for registering all of passes into Pass DataBase.
     */
    public static void registerPasses()
    {
        // Analysis passes.
        new RegisterPass("liveintervals", "Live Interval Analysis", LiveIntervalAnalysis.class);
        new RegisterPass("domfrontier", "Dominance Frontier Construction", DominanceFrontier.class, true, true);
        new RegisterPass("domtree", "Dominator Tree Construction", DomTreeInfo.class, true, true);
        new RegisterPass("iv-users", "Induction Variable Users", IVUsers.class,false, true);
        new RegisterPass("livevars", "Live Variable Analysis", LiveVariables.class);
        new RegisterPass("loops", "Natural Loop Information", LoopInfo.class, true, true);
        new RegisterPass("machinedomtree", "Machine Dominator Tree Construction", MachineDomTreeInfo.class, true);
        new RegisterPass("machine-loops", "Machine Natural Loop Construction", MachineLoopInfo.class, true);
        new RegisterPass("scalar-evolution", "Scalar Evolution Analysis", ScalarEvolution.class, false, true);

        // IPO transformation
        new RegisterPass("raiseallocs", "Raise allocations from calls to instructions", RaiseAllocations.class);

        // Scalar transformation
        new RegisterPass("break-crit-edges", "Break critical edges in CFG", BreakCriticalEdge.class);
        new RegisterPass("simplifycfg", "Simplify the CFG", CFGSimplifyPass.class);
        new RegisterPass("condprop", "Conditional Propagation", ConditionalPropagate.class);
        new RegisterPass("constprop", "Simple constant propagation", ConstantPropagation.class);
        new RegisterPass("die", "Dead Instruction Elimination", DCE.class);
        new RegisterPass("gvnpre", "Global Value Numbering/Partial Redundancy Elimination", GVNPRE.class);
        new RegisterPass("indvarsv1", "Canonicalize Induction Variables[Morgen textbook]", InductionVarSimplify.class);
        new RegisterPass("indvars", "Canonicalize Induction Variables", IndVarSimplify.class);
        new RegisterPass("lcssa", "Loop closed SSA", LCSSA.class);
        new RegisterPass("licm", "Loop Invariant Code Motion", LICM.class);
        new RegisterPass("loop-deletion", "Delete dead loops", LoopDeletion.class);
        new RegisterPass("loopsimply", "Loop Simplification Pass", LoopSimplify.class);
        new RegisterPass("lowerswitch", "X86 lower switch", LowerSwitch.class);
        new RegisterPass("mem2reg", "Promote memory to Register", Mem2Reg.class);
        new RegisterPass("sccp", "Sparse Conditional Constant Propagation", SCCP.class);
        new RegisterPass("scalarrepl", "Scalar Replacement of Aggregates", SROA.class);
        new RegisterPass("tailcallelim", "Tail Call Elimination", TailCallElim.class);
        new RegisterPass("mergereturn", "Unify function exit nodes", UnifyFunctionExitNodes.class);
        new RegisterPass("unreachableblockelim", "Remove unreachable basic block from CFG", UnreachableBlockElim.class);

        // Machine Function Passes
        new RegisterPass("phi-node-elimination", "Eliminate PHI nodes for register allocation", PhiElimination.class);
        new RegisterPass("prologepilog", "Prologue/Epilogue Insertion", PrologEpilogInserter.class);
        new RegisterPass("lsra", "Linear Scan Register Allocation", RegAllocLinearScan.class);
        new RegisterPass("regalloc-local", "Local register allocator", RegAllocLocal.class);
        new RegisterPass("regalloc-simple", "Simple register allocator", RegAllocSimple.class);
        new RegisterPass("twoaddressinstruction", "Two-Address instruction pass", TwoAddrInstructionPass.class);
        new RegisterPass("unreachable-machineblockelim", "Remove unreachable machine blocks from the machine CFG", UnreachableMachineBlockElim.class);
        new RegisterPass("machine-module=info", "Machine Module Information", MachineModuleInfo.class);
        // Register X86 fast isel by reflect mechanism.
        try
        {
            new RegisterPass("fast-isel", "X86 Fast Instruction Selector",
                    Class.forName("backend.target.x86.X86GenFastISel").asSubclass(Pass.class));
        }
        catch (ClassNotFoundException e)
        {
            ErrorHandling.llvmReportError("Fail to register X86GenFastISel pass");
        }
        new RegisterPass("print-function", "Print out Function", PrintMachineFunctionPass.class);
        new RegisterPass("print-module", "Print out Module", PrintModulePass.class);
        new RegisterPass("machine-verifier", "Verify generated machine code", MachineCodeVerifier.class);

        // Immutable Passes.
        new RegisterPass("targetdata", "Target Data Layout", TargetData.class, false, true);
    }
}
