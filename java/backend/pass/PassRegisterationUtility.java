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
        new RegisterPass("Live Interval Analysis", "liveintervals", LiveIntervalAnalysis.class);
        new RegisterPass( "Dominance Frontier Construction", "domfrontier", DominanceFrontier.class, true, true);
        new RegisterPass("Dominator Tree Construction", "domtree", DomTree.class, true, true);
        new RegisterPass( "Induction Variable Users", "iv-users",IVUsers.class,false, true);
        new RegisterPass( "Live Variable Analysis", "livevars", LiveVariables.class);
        new RegisterPass( "Natural Loop Information", "loops", LoopInfo.class, true, true);
        new RegisterPass( "Machine Dominator Tree Construction", "machinedomtree", MachineDomTreeInfo.class, true);
        new RegisterPass( "Machine Natural Loop Construction", "machine-loops", MachineLoopInfo.class, true);
        new RegisterPass( "Scalar Evolution Analysis", "scalar-evolution", ScalarEvolution.class, false, true);

        // IPO transformation
        new RegisterPass( "Raise allocations from calls to instructions", "raiseallocs", RaiseAllocations.class);

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
        new RegisterPass("Loop Simplification Pass","loopsimplify", LoopSimplify.class);
        new RegisterPass("Lower switch with branch", "lowerswitch", LowerSwitch.class);
        new RegisterPass("Promote memory to Register", "mem2reg",  Mem2Reg.class);
        new RegisterPass("Sparse Conditional Constant Propagation", "sccp", SCCP.class);
        new RegisterPass("Scalar Replacement of Aggregates", "scalarrepl", SROA.class);
        new RegisterPass("Tail Call Elimination", "tailcallelim", TailCallElim.class);
        new RegisterPass("Unify function exit nodes", "mergereturn", UnifyFunctionExitNodes.class);
        new RegisterPass("Remove unreachable basic block from CFG", "unreachableblockelim", UnreachableBlockElim.class);

        // Machine Function Passes
        new RegisterPass("Eliminate PHI nodes for register allocation", "phi-node-elimination",  PhiElimination.class);
        new RegisterPass("Prologue/Epilogue Insertion", "prologepilog",  PrologEpilogInserter.class);
        new RegisterPass("Linear Scan Register Allocation", "lsra", RegAllocLinearScan.class);
        new RegisterPass("Local register allocator", "regalloc-local",RegAllocLocal.class);
        new RegisterPass("Two-Address instruction pass", "twoaddressinstruction", TwoAddrInstructionPass.class);
        new RegisterPass("Remove unreachable machine blocks from the machine CFG", "unreachable-machineblockelim", UnreachableMachineBlockElim.class);
        new RegisterPass("Machine Module Information", "machine-module=info", MachineModuleInfo.class);
        // Register X86 fast isel by reflect mechanism.
        try
        {
            new RegisterPass("X86 Fast Instruction Selector", "fast-isel",
                    Class.forName("backend.target.x86.X86GenFastISel").asSubclass(Pass.class));
        }
        catch (ClassNotFoundException e)
        {
            ErrorHandling.llvmReportError("Fail to register X86GenFastISel pass");
        }
        new RegisterPass("Print out Function", "print-function", PrintMachineFunctionPass.class);
        new RegisterPass("Print out Module", "print-module", PrintModulePass.class);
        new RegisterPass("Verify generated machine code","machine-verifier", MachineCodeVerifier.class);

        // Immutable Passes.
        new RegisterPass("Target Data Layout", "targetdata", TargetData.class, false, true);
    }
}
