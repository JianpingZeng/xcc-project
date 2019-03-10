/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

package backend.codegen;

import backend.analysis.MachineDomTree;
import backend.analysis.MachineLoop;
import backend.analysis.MachineLoopInfo;
import backend.mc.MCRegisterClass;
import backend.pass.AnalysisUsage;
import backend.pass.FunctionPass;
import backend.support.*;
import backend.target.*;
import backend.value.Function;
import tools.BitMap;
import tools.OutRef;
import tools.Pair;
import tools.Util;

import java.util.*;

import static backend.codegen.PrologEpilogInserter.ShrinkWrapDebugLevel.*;
import static backend.target.TargetFrameLowering.StackDirection.StackGrowDown;
import static backend.target.TargetRegisterInfo.isPhysicalRegister;

/**
 * This class responsible for finalizing the stack frame layout and emits
 * code for inserting prologue and epilogue code.
 * <p>
 * Another important subtask is eliminating abstract frame index reference.
 * <p>
 * <emp>Note that</emp> this pass must be run after executing machine
 * instruction selector.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class PrologEpilogInserter extends MachineFunctionPass {
  public static final IntStatistic numSRRecord = new IntStatistic(
      "numSRRecord", "Number of CSR spills+restores reduced");

  // Debugging level for shrink wrapping.
  public enum ShrinkWrapDebugLevel {
    None, BasicInfo, Iterations, Details
  }

  private RegScavenger rs;
  /**
   * Keeps the range of callee saved stack frame indexes.
   */
  private int minCSFrameIndex;
  private int maxCSFrameIndex;

  // Flag to control shrink wrapping per-function:
  // may choose to skip shrink wrapping for certain
  // functions.
  private boolean shrinkWrapThisFunction;

  private BitMap usedCSRegs;
  private TreeMap<MachineBasicBlock, BitMap> csrUsed;
  private TreeMap<MachineBasicBlock, BitMap> anticIn, anticOut;
  private TreeMap<MachineBasicBlock, BitMap> availIn, availOut;
  private TreeMap<MachineBasicBlock, BitMap> csrSave;
  private TreeMap<MachineBasicBlock, BitMap> csrRestore;

  private MachineBasicBlock entryBlock;
  private ArrayList<MachineBasicBlock> returnBlocks;
  private TreeMap<MachineBasicBlock, MachineLoop> tlLoops;

  private boolean hasFastExitPath;
  private MachineFunction mf;
  private TargetRegisterInfo tri;

  public PrologEpilogInserter() {
    minCSFrameIndex = 0;
    maxCSFrameIndex = 0;
    shrinkWrapThisFunction = false;
    usedCSRegs = new BitMap();
    csrUsed = new TreeMap<>();
    anticIn = new TreeMap<>();
    availIn = new TreeMap<>();
    availOut = new TreeMap<>();
    anticOut = new TreeMap<>();
    csrSave = new TreeMap<>();
    csrRestore = new TreeMap<>();
    entryBlock = null;
    returnBlocks = new ArrayList<>();
    tlLoops = new TreeMap<>();
  }

  @Override
  public String getPassName() {
    return "Prolog/Epilog Insertion & Frame Finalization";
  }

  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    au.addPreserved(MachineLoopInfo.class);
    au.addPreserved(MachineDomTree.class);
    super.getAnalysisUsage(au);
  }

  /**
   * Insert prolog/epilog code and replace abstract frame indexes with appropriate
   * references.
   *
   * @param mf
   * @return
   */
  @Override
  public boolean runOnMachineFunction(MachineFunction mf) {
    this.mf = mf;
    tri = mf.getTarget().getRegisterInfo();

    Function f = mf.getFunction();
    TargetRegisterInfo tri = mf.getTarget().getRegisterInfo();
    rs = tri.requireRegisterScavenging(mf) ? new RegScavenger() : null;

    // Calculate the MaxCallFrameSize and HasCalls variables for the
    // function's frame information. Also elimination call frame psedo
    // instructions.
    calculateCallsInformation(mf);

    tri.processFunctionBeforeCalleeSavedScan(mf, rs);

    // Scan the function for modified callee saved registers and
    // inserts spill code for any callee saved registers that are modified.
    calculateCalleeSavedRegisters(mf);

    // Determine placement of CSR spill/restore code:
    //  - with shrink wrapping, place spills and restores to tightly
    //    enclose regions in the Machine CFG of the function where
    //    they are used. Without shrink wrapping
    //  - default (no shrink wrapping), place all spills in the
    //    entry block, all restores in return blocks.
    placeCSRSpillsAndRestores(mf);

    // Add the code to save and restore the callee saved registers
    if (!f.hasFnAttr(Attribute.Naked))
      insertCSRSpillsAndRestores(mf);

    // ALlow target machine to make final modification to the function
    // before the frame layout is finalized.
    tri.processFunctionBeforeFrameFinalized(mf);

    // Calculate actual frame offsets for all of the stack object.
    calculateFrameObjectOffsets(mf);

    // add prolog and epilog code to the function.
    if (!f.hasFnAttr(Attribute.Naked))
      insertPrologEpilogCode(mf);

    // Replace all MO_FrameIndex operands with physical register
    // references and actual offsets.
    replaceFrameIndices(mf);
    return true;
  }

  /**
   * Calculate the maxCallFrameSize and hasCalls variables for the
   * function's frame information. Also elimination call frame psedo
   * instructions.
   *
   * @param mf
   */
  private void calculateCallsInformation(MachineFunction mf) {
    TargetRegisterInfo tri = mf.getTarget().getRegisterInfo();
    TargetInstrInfo tii = mf.getTarget().getInstrInfo();
    long maxCallFrameSize = 0;
    boolean hasCalls = false;

    int frameSetupOpcode = tii.getCallFrameSetupOpcode();
    int frameDestroyOpcode = tii.getCallFrameDestroyOpcode();

    if (frameSetupOpcode == -1 && frameDestroyOpcode == -1)
      return;

    // A list for keeping track of frame setup/destroy operation.
    ArrayList<MachineInstr> frameSDOps = new ArrayList<>();
    for (MachineBasicBlock mbb : mf.getBasicBlocks()) {
      for (int i = 0, e = mbb.size(); i != e; i++) {
        MachineInstr mi = mbb.getInstAt(i);
        if (mi.getOpcode() == frameSetupOpcode || mi.getOpcode() == frameDestroyOpcode) {
          Util.assertion(mi.getNumOperands() >= 1, "Call frame Setup/Destroy" + " Psedo instruction should have a single immediate argument!");

          long size = mi.getOperand(0).getImm();
          if (size > maxCallFrameSize)
            maxCallFrameSize = size;
          hasCalls = true;
          frameSDOps.add(mi);
        }
      }
    }

    MachineFrameInfo mfi = mf.getFrameInfo();
    mfi.setHasCalls(hasCalls);
    mfi.setMaxCallFrameSize(maxCallFrameSize);

    for (MachineInstr mi : frameSDOps) {
      // If call frames are not being included as part of the stack frame,
      // and there is no dynamic allocation(therefore referencing frame
      // slots off sp), leave the psedo ops alone. we'll eliminate them
      // later.
      TargetFrameLowering tfl = mf.getTarget().getFrameLowering();
      if (tfl.hasReservedCallFrame(mf) || tfl.hasFP(mf))
        tii.eliminateCallFramePseudoInstr(mf, mi);
    }
  }

  private void placeCSRSpillsAndRestores(MachineFunction mf) {
    initShrinkWrappingInfo();

    if (calculateSets(mf))
      placeSpillsAndRestores(mf);
  }

  /**
   * Initialize all shrink wrapping data.
   */
  private void initShrinkWrappingInfo() {
    clearAllSets();
    entryBlock = null;
    shrinkWrapThisFunction = BackendCmdOptions.ShrinkWrapping.value;
  }

  private void clearAllSets() {
    returnBlocks.clear();
    clearAnticAvailSets();
    usedCSRegs.clear();
    csrUsed.clear();
    tlLoops.clear();
    csrSave.clear();
    csrRestore.clear();
  }

  private void clearAnticAvailSets() {
    anticIn.clear();
    anticOut.clear();
    availIn.clear();
    availOut.clear();
  }

  /**
   * collect the CSRs used in this function, compute
   * the DF sets that describe the initial minimal regions in the
   * Machine CFG around which CSR spills and restores must be placed.
   * <p>
   * Additionally, this function decides if shrink wrapping should
   * be disabled for the current function, checking the following:
   * <ol>
   * <li>
   * the current function has more than 500 MBBs: heuristic limit
   * on function size to reduce compile time impact of the current
   * iterative algorithm.
   * </li>
   * <li>
   * all CSRs are used in the entry block.
   * </li>
   * <li>
   * all CSRs are used in all immediate successors of the entry block.
   * </li>
   * <li>
   * all CSRs are used in a subset of blocks, each of which dominates
   * all return blocks. These blocks, taken as a subgraph of the MCFG,
   * are equivalent to the entry block since all execution paths pass
   * through them.
   * </li>
   * </ol>
   *
   * @param mf
   * @return
   */
  private boolean calculateSets(MachineFunction mf) {
    ArrayList<CalleeSavedInfo> csi = mf.getFrameInfo().getCalleeSavedInfo();

    // If no csrs. we done.
    if (csi.isEmpty()) {
      if (shrinkWrapThisFunction)
        System.err.printf("Disabled: %s: uses no called-saved registers\n",
            mf.getFunction().getName());
      return false;
    }

    entryBlock = mf.getEntryBlock();

    for (MachineBasicBlock mbb : mf.getBasicBlocks()) {
      if (isReturnBlock(mbb))
        returnBlocks.add(mbb);
    }

    if (shrinkWrapThisFunction)
      findFastExitPath();

    if (mf.size() > 500) {
      if (shrinkWrapThisFunction) {
        System.err.printf("Disabled: %s: too large (%d MBBs)\n",
            mf.getFunction().getName(), mf.size());
        shrinkWrapThisFunction = false;
      }
    }

    // Return now if not shrink wrapping.
    if (!shrinkWrapThisFunction)
      return false;

    // Collect set of used CSRs.
    for (int i = 0, e = csi.size(); i != e; i++)
      usedCSRegs.set(i);

    // Walk instructions in all MBBs, create CSRUsed[] sets, choose
    // whether or not to shrink wrap this function.
    MachineLoopInfo li = (MachineLoopInfo) getAnalysisToUpDate(MachineLoopInfo.class);
    MachineDomTree dt = (MachineDomTree) getAnalysisToUpDate(MachineDomTree.class);
    TargetRegisterInfo tri = mf.getTarget().getRegisterInfo();

    boolean allCSRUsesInEntryBlock = true;
    for (MachineBasicBlock mbb : mf.getBasicBlocks()) {
      for (MachineInstr mi : mbb.getInsts()) {
        for (int idx = 0, e = csi.size(); idx != e; ++idx) {
          int reg = csi.get(idx).getReg();
          // If instruction I reads or modifies Reg, add it to UsedCSRegs,
          // CSRUsed map for the current block.
          for (int opIdx = 0, opEnd = mi.getNumOperands(); opIdx != opEnd; ++opIdx) {
            MachineOperand mo = mi.getOperand(opIdx);
            if (!(mo.isRegister() && (mo.isDef() || mo.isUse())))
              continue;

            int moReg = mo.getReg();
            if (moReg == 0)
              continue;

            if (moReg == reg || (isPhysicalRegister(moReg) &&
                isPhysicalRegister(reg) && tri.isSubRegister(reg, moReg))) {
              // if the this machine operand is register and this register
              // is the callee-saved register, add it to used set of mbb.
              if (csrUsed.containsKey(mbb))
                csrUsed.put(mbb, new BitMap());
              csrUsed.get(mbb).set(idx);

              if (!mbb.equals(entryBlock))
                allCSRUsesInEntryBlock = false;
            }
          }
        }
      }

      if (csrUsed.get(mbb).isEmpty())
        continue;

      // Propagate csrUsed.get(mbb) in loops
      MachineLoop lp = li.getLoopFor(mbb);
      if (lp != null) {
        // Add top level loop to work list.
        MachineBasicBlock hdr = getTopLevelLoopPreHeader(lp);
        MachineLoop plp = getTopLevelParentLoop(lp);

        if (hdr == null) {
          hdr = plp.getHeaderBlock();
          Util.assertion(!hdr.isPredEmpty(), "Loop header has no predecessor");
          hdr = hdr.predAt(0);
        }
        tlLoops.put(hdr, plp);

        if (lp.getLoopDepth() > 1) {
          for (plp = lp.getParentLoop(); plp != null; plp = plp.getParentLoop())
            propagateUsesAroundLoop(mbb, plp);
        } else {
          propagateUsesAroundLoop(mbb, lp);
        }
      }
    }

    if (allCSRUsesInEntryBlock) {
      System.err.printf("Disabled: %s: all CSRs used in EntryBlock\n",
          mf.getFunction().getName());
      shrinkWrapThisFunction = false;
    } else {
      boolean allCSRsUsedInEntryFanout = true;
      for (MachineBasicBlock succ : entryBlock.getSuccessors()) {
        if (!csrUsed.get(succ).equals(usedCSRegs))
          allCSRsUsedInEntryFanout = false;
      }
      if (allCSRsUsedInEntryFanout) {
        System.err.printf("Disabled: %s:all CSRs used in imm successors\n",
            mf.getFunction().getName());
        shrinkWrapThisFunction = false;
      }
    }

    if (shrinkWrapThisFunction) {
      // Check if MBB uses CSRs and dominates all exit nodes.
      // Such nodes are equiv. to the entry node w.r.t.
      // CSR uses: every path through the function must
      // pass through this node. If each CSR is used at least
      // once by these nodes, shrink wrapping is disabled.
      BitMap csrUsedInChokePoints = new BitMap();
      for (MachineBasicBlock mbb : mf.getBasicBlocks()) {
        if (mbb.equals(entryBlock) || csrUsed.get(mbb).isEmpty())
          continue;

        boolean dominatesExitBlock = true;
        for (MachineBasicBlock returnBB : returnBlocks) {
          if (!dt.dominates(mbb, returnBB)) {
            dominatesExitBlock = false;
            break;
          }
        }
        if (dominatesExitBlock) {
          csrUsedInChokePoints.or(csrUsed.get(mbb));
          if (csrUsedInChokePoints.equals(usedCSRegs)) {
            System.err.printf("Disabled: %s:all CSRs used in choke points at%s\n",
                mf.getFunction().getName(), getBasicBlockName(mbb));
            shrinkWrapThisFunction = false;
            break;
          }
        }
      }
    }

    if (!shrinkWrapThisFunction) {
      return false;
    }

    System.err.printf("Enabled: %s", mf.getFunction().getName());
    if (hasFastExitPath)
      System.err.print(" (fast exit path)");
    System.err.println();
    if (BackendCmdOptions.ShrinkWrapDebugging.value.ordinal() >= BasicInfo.ordinal()) {
      System.err.print("------------------------------"
          + "-----------------------------\n");
      System.err.printf("UsedCSRegs = %s\n", stringifyCSRegSet(usedCSRegs));
      if (BackendCmdOptions.ShrinkWrapDebugging.value.ordinal() >= Details.ordinal()) {
        System.err.print("------------------------------"
            + "-----------------------------\n");
        dumpAllUsed();
      }
    }

    // Build initial DF sets to determine minimal regions in the
    // Machine CFG around which CSRs must be spilled and restored.
    calculateAnticAvail(mf);
    return true;
  }

  /**
   * build the sets anticipated and available registers in the MCFG of
   * the current function iteratively, doing a combined forward and
   * backward analysis.
   *
   * @param mf
   */
  private void calculateAnticAvail(MachineFunction mf) {
    // Initialize data flow sets.
    clearAnticAvailSets();
    // Calulate Antic{In,Out} and Avail{In,Out} iteratively on the MCFG.
    boolean changed = true;
    int iterations = 0;
    while (changed) {
      changed = false;
      ++iterations;
      for (MachineBasicBlock mbb : mf.getBasicBlocks()) {
        // Calculate anticipate in, out regs at the mbb from
        // anticipated at successors of mbb.
        changed |= calcuateAnticInOut(mbb);
        // Calculate available in, out regs at the mbb from available
        // at the predecessor mbb.
        changed |= calculateAvailInOut(mbb);
      }
    }

    if (BackendCmdOptions.ShrinkWrapDebugging.value.ordinal() >= Details.ordinal()) {
      System.err.print("-----------------------------------------------------------\n");
      System.err.print(" Antic/Avail Sets:\n");
      System.err.print("-----------------------------------------------------------\n");
      System.err.printf("iterations = %d\n", iterations);
      System.err.printf("-----------------------------------------------------------\n");
      System.err.printf("MBB | USED | ANTIC_IN | ANTIC_OUT | AVAIL_IN | AVAIL_OUT\n");
      System.err.printf("-----------------------------------------------------------\n");
    }
  }

  private boolean calcuateAnticInOut(MachineBasicBlock mbb) {
    boolean changed = false;
    BitMap preAnticOut = anticOut.get(mbb).clone();

    // A set for ensuring the only once computing for each successor
    // if there are multiple edges from the this mbb to the successor.
    TreeSet<MachineBasicBlock> succs = new TreeSet<>();
    for (MachineBasicBlock succ : mbb.getSuccessors()) {
      if (succs.add(succ)) {
        anticOut.get(mbb).and(anticIn.get(mbb));
      }
    }
    if (!preAnticOut.equals(anticOut.get(mbb)))
      changed = true;

    // AnticIn[MBB] = UNION(CSRUsed[MBB], AnticOut[MBB]);
    BitMap preAnticIn = anticIn.get(mbb).clone();
    BitMap temp = anticOut.get(mbb).clone();
    temp.or(csrUsed.get(mbb));
    anticIn.put(mbb, temp);

    if (!preAnticIn.equals(anticIn.get(mbb)))
      changed = true;

    return changed;
  }

  /**
   * Performs the backward dataflow analysis to computing AvailabltIn[mbb] and
   * AvailableOut[mbb] down from top.
   *
   * @param mbb
   * @return
   */
  private boolean calculateAvailInOut(MachineBasicBlock mbb) {
    boolean changed = false;
    BitMap preAvailIn = availIn.get(mbb).clone();

    // A set for ensuring the only once computing for each predecessor
    // if there are multiple edges from the predecessor to this mbb.
    TreeSet<MachineBasicBlock> preds = new TreeSet<>();
    for (MachineBasicBlock pred : mbb.getPredecessors()) {
      if (preds.add(pred)) {
        availIn.get(mbb).and(availOut.get(pred));
      }
    }

    if (!preAvailIn.equals(availIn.get(mbb)))
      changed = true;

    // AvailOut[mbb] = UNION(csrUses[mbb], AvailIn[mbb])
    BitMap preAvailOut = availOut.get(mbb).clone();
    BitMap temp = availIn.get(mbb).clone();
    temp.or(csrUsed.get(mbb));

    availOut.put(mbb, temp);
    if (!preAvailOut.equals(availOut.get(mbb)))
      changed = true;

    return changed;
  }

  /**
   * copy used register info from MBB to all blocks of the loop given
   * by {@code loop} and its parent loops. This prevents spills/restores
   * from being placed in the bodies of loops.
   *
   * @param mbb
   * @param loop
   */
  private void propagateUsesAroundLoop(MachineBasicBlock mbb, MachineLoop loop) {
    if (mbb == null || loop == null)
      return;
    for (MachineBasicBlock lbb : loop.getBlocks()) {
      if (lbb.equals(mbb))
        continue;
      csrUsed.get(lbb).or(csrUsed.get(mbb));
    }
  }

  private MachineBasicBlock getTopLevelLoopPreHeader(MachineLoop lp) {
    Util.assertion(lp != null, "Machine loop is null!");
    MachineLoop parent = getTopLevelParentLoop(lp);
    return parent.getLoopPreheader();
  }

  private MachineLoop getTopLevelParentLoop(MachineLoop loop) {
    if (loop == null)
      return null;

    MachineLoop parent = loop.getParentLoop();
    while (parent != null) {
      loop = parent;
      parent = loop.getParentLoop();
    }
    return loop;
  }

  /**
   * debugging method used to detect functions
   * with at least one path from the entry block to a return block
   * directly or which has a very small number of edges.
   */
  private void findFastExitPath() {
    if (entryBlock == null)
      return;

    // Find a path from EntryBlock to any return block that does not branch:
    //        Entry
    //          |     ...
    //          v      |
    //         B1<-----+
    //          |
    //          v
    //       Return
    for (MachineBasicBlock succ : entryBlock.getSuccessors()) {
      // Assume positive, disprove existence of fast path.
      hasFastExitPath = true;

      if (isReturnBlock(succ)) {
        if (BackendCmdOptions.ShrinkWrapDebugging.value.ordinal() >= BasicInfo.ordinal()) {
          System.err.printf("Fast exit path: %s->%s",
              getBasicBlockName(entryBlock), getBasicBlockName(succ));
          break;
        }

        // Traverse df from SUCC, look for a branch block.
        String exitPath = getBasicBlockName(succ);
        List<MachineBasicBlock> dfs = DepthFirstOrder.dfTraversal(succ);
        for (MachineBasicBlock mbb : dfs) {
          // Rejet paths with branch nodes.
          if (mbb.getNumSuccessors() > 1) {
            hasFastExitPath = false;
            break;
          }
          exitPath += "->" + getBasicBlockName(mbb);
        }

        if (hasFastExitPath) {
          if (BackendCmdOptions.ShrinkWrapDebugging.value.ordinal() >= BasicInfo.ordinal()) {
            System.err.printf("Fast exit path: %s->%s\n",
                getBasicBlockName(entryBlock),
                exitPath);
          }
        }
      }
    }
  }

  private String getBasicBlockName(MachineBasicBlock mbb) {
    if (mbb == null)
      return "";

    if (mbb.getBasicBlock() != null)
      return mbb.getBasicBlock().getName();

    return "_MBB_" + mbb.getNumber();
  }

  private boolean isReturnBlock(MachineBasicBlock mbb) {
    return mbb != null && !mbb.isEmpty() && mbb.getLastInst().getDesc().isReturn();
  }

  /**
   * place spills and restores of CSRs used in MBBs in minimal regions
   * that contain the uses.
   *
   * @param mf
   */
  private void placeSpillsAndRestores(MachineFunction mf) {
    TreeMap<MachineBasicBlock, BitMap> prevCSRSave = new TreeMap<>();
    TreeMap<MachineBasicBlock, BitMap> prevCSRRestore = new TreeMap<>();

    Stack<MachineBasicBlock> cvBlocks = new Stack<>();
    Stack<MachineBasicBlock> ncvBlocks = new Stack<>();
    boolean changed = true;
    int iterations = 0;

    while (changed) {
      changed = false;
      ++iterations;

      if (BackendCmdOptions.ShrinkWrapDebugging.value.ordinal() >= Iterations.ordinal()) {
        System.err.printf("iter %d%s\n", iterations,
            " --------------------------------------------------");

        boolean srChanged = false;
        // Calculate CSR{Save,Restore} sets using Antic, Avail on the MCFG,
        // which determines the placements of spills and restores.
        // Keep track of changes to spills, restores in each iteration to
        // minimize the total iterations.
        for (MachineBasicBlock mbb : mf.getBasicBlocks()) {
          // Place spills for CSRs in MBB.
          srChanged |= calcSpillPlacement(mbb, cvBlocks, prevCSRSave);

          // Place restores for CSRs in MBB.
          srChanged |= calcRestorePlacement(mbb, cvBlocks, prevCSRRestore);
        }

        // Add uses of CSRs used inside loops where needed.
        changed |= addUsesForTopLevelLoops(cvBlocks);

        if (changed || srChanged) {
          while (!cvBlocks.isEmpty()) {
            MachineBasicBlock mbb = cvBlocks.pop();
            changed |= addUsesForMEMRegion(mbb, ncvBlocks);
          }

          if (!ncvBlocks.isEmpty()) {
            cvBlocks.clear();
            cvBlocks.addAll(ncvBlocks);
            ncvBlocks.clear();
          }
        }

        if (changed) {
          calculateAnticAvail(mf);
          csrSave.clear();
          csrRestore.clear();
        }
      }
    }

    // Check for effectiveness:
    //  SR0 = {r | r in CSRSave[EntryBlock], CSRRestore[RB], RB in ReturnBlocks}
    //  numSRReduced = |(UsedCSRegs - SR0)|, approx. SR0 by CSRSave[EntryBlock]
    // Gives a measure of how many CSR spills have been moved from EntryBlock
    // to minimal regions enclosing their uses.
    BitMap notSpilledInEntryBlock = usedCSRegs.clone();
    notSpilledInEntryBlock.diff(csrSave.get(entryBlock));
    int numSRReducedThisFunction = notSpilledInEntryBlock.size();
    numSRRecord.add(numSRReducedThisFunction);
    if (BackendCmdOptions.ShrinkWrapDebugging.value.ordinal() >= BasicInfo.ordinal()) {
      System.err.print("-----------------------------------------------------------\n");
      System.err.printf("total iterations = %s (%s %d %d )\n",
          iterations,
          mf.getFunction().getName(),
          numSRReducedThisFunction,
          mf.size());
      System.err.print("-----------------------------------------------------------\n");
      dumpSRSets();
      System.err.print("-----------------------------------------------------------\n");
      if (numSRReducedThisFunction != 0) {
        verifySpillRestorePlacement();
      }
    }
  }

  private boolean calcSpillPlacement(MachineBasicBlock mbb,
                                     Stack<MachineBasicBlock> blocks,
                                     TreeMap<MachineBasicBlock, BitMap> prevSpills) {
    boolean placedSpills = false;
    BitMap anticInPreds = new BitMap();
    TreeSet<MachineBasicBlock> preds = new TreeSet<>();
    for (MachineBasicBlock pred : mbb.getPredecessors()) {
      if (pred != mbb)
        preds.add(pred);
    }

    if (!preds.isEmpty()) {
      Iterator<MachineBasicBlock> itr = preds.iterator();
      anticInPreds.or(usedCSRegs);
      anticInPreds.diff(anticIn.get(itr.next()));
      while (itr.hasNext()) {
        BitMap temp = new BitMap();
        temp.or(usedCSRegs);
        temp.diff(anticIn.get(itr.next()));
        anticInPreds.and(temp);
      }
    } else {
      anticInPreds.or(usedCSRegs);
    }

    BitMap temp = new BitMap();
    temp.or(anticIn.get(mbb));
    temp.diff(availIn.get(mbb));
    temp.and(anticInPreds);
    temp.or(csrSave.get(mbb));
    csrSave.put(mbb, temp);

    if (!csrSave.get(mbb).isEmpty()) {
      if (mbb.equals(entryBlock)) {
        returnBlocks.forEach(retBB ->
        {
          csrRestore.get(retBB).or(csrSave.get(mbb));
        });
      } else {
        if (csrSave.get(entryBlock).intersects(csrSave.get(mbb))) {
          csrSave.get(mbb).diff(csrSave.get(entryBlock));
        }
      }
    }

    placedSpills = !csrSave.get(mbb).equals(prevSpills.get(mbb));
    prevSpills.put(mbb, csrSave.get(mbb));

    if (placedSpills)
      blocks.add(mbb);

    return placedSpills;
  }

  private boolean calcRestorePlacement(MachineBasicBlock mbb,
                                       Stack<MachineBasicBlock> blocks,
                                       TreeMap<MachineBasicBlock, BitMap> prevRestores) {
    boolean placedRestores = false;
    BitMap availOutSucc = new BitMap();
    TreeSet<MachineBasicBlock> successors = new TreeSet<>();
    for (MachineBasicBlock succ : mbb.getSuccessors()) {
      if (!succ.equals(mbb))
        successors.add(succ);
    }

    if (!successors.isEmpty()) {
      availOutSucc.or(usedCSRegs);
      Iterator<MachineBasicBlock> itr = successors.iterator();
      availOutSucc.diff(availOut.get(itr.next()));

      while (itr.hasNext()) {
        BitMap temp = new BitMap();
        temp.or(usedCSRegs);
        temp.diff(availOut.get(itr.next()));
        availOutSucc.and(temp);
      }
    } else {
      if (!csrUsed.get(mbb).isEmpty() || !availOut.get(mbb).isEmpty())
        availOutSucc.or(usedCSRegs);
    }

    BitMap temp = new BitMap();
    temp.or(availOut.get(mbb));
    temp.diff(anticOut.get(mbb));
    temp.and(availOutSucc);

    csrRestore.get(mbb).or(temp);

    if (!mbb.succIsEmpty() && !csrRestore.get(mbb).isEmpty()) {
      if (!csrSave.get(entryBlock).isEmpty()) {
        csrRestore.get(mbb).diff(csrSave.get(entryBlock));
      }
    }

    placedRestores = !csrRestore.get(mbb).equals(prevRestores.get(mbb));
    prevRestores.put(mbb, csrRestore.get(mbb));

    if (placedRestores)
      blocks.add(mbb);

    return placedRestores;
  }

  private boolean addUsesForTopLevelLoops(Stack<MachineBasicBlock> blocks) {
    boolean addedUses = false;

    for (Map.Entry<MachineBasicBlock, MachineLoop> pair : tlLoops.entrySet()) {
      MachineBasicBlock mbb = pair.getKey();
      MachineLoop loop = pair.getValue();

      MachineBasicBlock hdr = loop.getHeaderBlock();
      ArrayList<MachineBasicBlock> exitBlocks = new ArrayList<>();
      BitMap loopSpils = new BitMap();

      loopSpils.or(csrSave.get(mbb));
      if (csrSave.get(mbb).isEmpty()) {
        loopSpils.clear();
        loopSpils.or(csrUsed.get(hdr));
        Util.assertion(!loopSpils.isEmpty(), "Not CSRs used in loop?");
      } else if (csrRestore.get(mbb).contains(csrSave.get(mbb)))
        continue;

      exitBlocks = loop.getExitingBlocks();
      Util.assertion(!exitBlocks.isEmpty(), "Loop has no top level exit blocks");
      for (MachineBasicBlock exitBB : exitBlocks) {
        if (!csrUsed.get(exitBB).contains(loopSpils)) {
          csrUsed.get(exitBB).or(loopSpils);
          addedUses = true;
          if (exitBB.getNumSuccessors() > 1 || exitBB.getNumPredecessors() > 1)
            blocks.add(exitBB);
        }
      }
    }
    return addedUses;
  }

  private boolean addUsesForMEMRegion(
      MachineBasicBlock mbb,
      Stack<MachineBasicBlock> blocks) {
    if (mbb.getNumSuccessors() < 2 && mbb.getNumPredecessors() < 2) {
      boolean processThisBlock = false;
      for (MachineBasicBlock succ : mbb.getSuccessors()) {
        if (succ.getNumPredecessors() > 1) {
          processThisBlock = true;
          break;
        }
      }

      if (!csrRestore.get(mbb).isEmpty() && !mbb.succIsEmpty()) {
        for (MachineBasicBlock pred : mbb.getPredecessors()) {
          if (pred.getNumSuccessors() > 1) {
            processThisBlock = true;
            break;
          }
        }
      }
      if (!processThisBlock)
        return false;
    }

    BitMap prop = new BitMap();
    if (!csrSave.get(mbb).isEmpty()) {
      prop.or(csrSave.get(mbb));
    } else if (!csrRestore.get(mbb).isEmpty())
      prop.or(csrRestore.get(mbb));
    else
      prop.or(csrUsed.get(mbb));

    if (prop.isEmpty())
      return false;

    boolean addedUses = false;
    for (MachineBasicBlock succ : mbb.getSuccessors()) {
      if (succ.equals(mbb))
        continue;
      if (!csrUsed.get(succ).contains(prop)) {
        csrUsed.get(succ).or(prop);
        addedUses = true;
        blocks.add(succ);
      }
    }
    for (MachineBasicBlock pred : mbb.getPredecessors()) {
      if (pred.equals(mbb))
        continue;
      if (!csrUsed.get(pred).isEmpty()) {
        csrUsed.get(pred).or(prop);
        addedUses = true;
        blocks.add(pred);
      }
    }
    return addedUses;
  }

  private void verifySpillRestorePlacement() {
    int numReturnBlocks = 0;
    for (MachineBasicBlock mbb : mf.getBasicBlocks()) {
      if (isReturnBlock(mbb) || mbb.succIsEmpty())
        ++numReturnBlocks;
    }

    for (Map.Entry<MachineBasicBlock, BitMap> pair : csrSave.entrySet()) {
      MachineBasicBlock mbb = pair.getKey();
      BitMap spilled = pair.getValue();
      BitMap restored = new BitMap();

      if (spilled.isEmpty())
        continue;

      if (csrRestore.get(mbb).intersects(spilled)) {
        BitMap temp = new BitMap();
        temp.or(csrRestore.get(mbb));
        temp.and(spilled);
        restored.or(temp);
      }

      List<MachineBasicBlock> visited = DepthFirstOrder.dfTraversal(mbb);
      for (MachineBasicBlock sbb : visited) {
        if (sbb.equals(mbb))
          continue;

        BitMap temp = new BitMap();
        temp.or(csrSave.get(sbb));
        temp.and(spilled);
        if (csrSave.get(sbb).contains(spilled) && !restored.contains(temp)) {
          break;
        }

        if (csrRestore.get(sbb).contains(spilled)) {
          temp = new BitMap();
          temp.or(csrRestore.get(sbb));
          temp.and(spilled);
          restored.or(temp);
        }

        if (isReturnBlock(sbb) || sbb.succIsEmpty()) {
          if (!restored.equals(spilled)) {
            BitMap notRestored = new BitMap();
            notRestored.or(spilled);
            notRestored.diff(restored);
            System.err.printf("%s: %s spilled at %s are never "
                    + "restored on path to return %s\n",
                mf.getFunction().getName(),
                stringifyCSRegSet(notRestored),
                getBasicBlockName(mbb),
                getBasicBlockName(sbb));
          }
          restored.clear();
        }
      }
    }

    // Check restore placements.
    for (Map.Entry<MachineBasicBlock, BitMap> pair : csrRestore.entrySet()) {
      MachineBasicBlock mbb = pair.getKey();
      BitMap restored = pair.getValue();
      BitMap spilled = new BitMap();

      if (restored.isEmpty())
        continue;

      if (csrSave.get(mbb).intersects(restored)) {
        spilled.or(csrSave.get(mbb));
        spilled.and(restored);
      }
      List<MachineBasicBlock> visited = DepthFirstOrder.dfTraversal(mbb);
      for (MachineBasicBlock sbb : visited) {
        if (sbb.equals(mbb))
          continue;

        BitMap temp = new BitMap();
        temp.or(csrRestore.get(sbb));
        temp.and(restored);
        if (csrRestore.get(sbb).intersects(restored) && !spilled.contains(temp))
          break;

        if (csrSave.get(sbb).intersects(restored)) {
          temp = new BitMap();
          temp.or(csrSave.get(sbb));
          temp.and(restored);
          spilled.or(temp);
        }
      }

      if (!spilled.equals(restored)) {
        BitMap notSpilled = new BitMap();
        notSpilled.or(restored);
        notSpilled.diff(spilled);
        System.err.printf("%s: %s restored at %s are never spilled\n",
            mf.getFunction().getName(),
            stringifyCSRegSet(notSpilled),
            getBasicBlockName(mbb));
      }
    }
  }

  /**
   * Inserts spill and restores code for callee saved registers used in the
   * function.
   *
   * @param mf
   */
  private void insertCSRSpillsAndRestores(MachineFunction mf) {
    MachineFrameInfo mfi = mf.getFrameInfo();
    ArrayList<CalleeSavedInfo> csi = mfi.getCalleeSavedInfo();

    mfi.setCalleeSavedInfoValid(true);

    if (csi.isEmpty())
      return;

    TargetInstrInfo tii = mf.getTarget().getInstrInfo();
    int idx;
    MachineBasicBlock mbb;
    if (!shrinkWrapThisFunction) {
      idx = 0;
      if (!tii.spillCalleeSavedRegisters(entryBlock, idx, csi)) {
        for (CalleeSavedInfo info : csi) {
          // Add the callee-saved register as live-in.
          // It's killed at the spill.
          entryBlock.addLiveIn(info.getReg());

          // Insert the spill to the stack frame.
          tii.storeRegToStackSlot(entryBlock, idx, info.getReg(), true,
              info.getFrameIdx(), info.getRegisterClass());
        }
      }

      // Restore using target interface.
      for (MachineBasicBlock retBB : returnBlocks) {
        idx = retBB.size();
        mbb = retBB;
        idx--;

        // Skip over all terminator instruction.
        int i2 = idx;
        while (i2 != 0 && (mbb.getInstAt(--i2).getDesc().isTerminator()))
          idx = i2;

        boolean atStart = idx == 0;
        int beforeIdx = idx;
        if (!atStart)
          --beforeIdx;

        if (!tii.restoreCalleeSavedRegisters(mbb, idx, csi)) {
          for (CalleeSavedInfo info : csi) {
            tii.loadRegFromStackSlot(mbb, idx, info.getReg(),
                info.getFrameIdx(),
                info.getRegisterClass());
            Util.assertion(idx != 0, "loadRegFromStackSlot didn't insert any code!");
            if (atStart)
              idx = 0;
            else {
              idx = beforeIdx;
              ++idx;
            }
          }
        }
      }
      return;
    }

    // Insert spills.
    ArrayList<CalleeSavedInfo> blockCSI = new ArrayList<>();
    for (Map.Entry<MachineBasicBlock, BitMap> pair : csrSave.entrySet()) {
      mbb = pair.getKey();
      BitMap save = pair.getValue();

      if (save.isEmpty())
        continue;

      blockCSI.clear();
      for (int i = 0, e = save.size(); i != e; i++) {
        if (save.get(i))
          blockCSI.add(csi.get(i));
      }

      Util.assertion(!blockCSI.isEmpty(), "Could not collect callee saved register info");

      idx = 0;
      // When shrink wrapping, use stack slot stores/loads.
      for (CalleeSavedInfo info : blockCSI) {
        mbb.addLiveIn(info.getReg());
        tii.storeRegToStackSlot(mbb, idx, info.getReg(),
            true,
            info.getFrameIdx(),
            info.getRegisterClass());
      }
    }

    for (Map.Entry<MachineBasicBlock, BitMap> pair : csrRestore.entrySet()) {
      mbb = pair.getKey();
      BitMap restore = pair.getValue();

      if (restore.isEmpty())
        continue;

      blockCSI.clear();
      for (int i = 0, e = restore.size(); i != e; i++) {
        if (restore.get(i))
          blockCSI.add(csi.get(i));
      }

      Util.assertion(!blockCSI.isEmpty(), "Could not find callee saved regsiter info");

      if (mbb.isEmpty())
        idx = 0;
      else {
        idx = mbb.size();
        --idx;

        if (!mbb.getInstAt(idx).getDesc().isTerminator()) {
          ++idx;
        } else {
          int i2 = idx;
          while (i2 != 0 && mbb.getInstAt(--i2).getDesc().isTerminator())
            idx = i2;
        }
      }

      boolean atStart = idx == 0;
      int beforeIdx = idx;
      if (!atStart)
        --beforeIdx;

      for (CalleeSavedInfo info : blockCSI) {
        tii.loadRegFromStackSlot(mbb, idx, info.getReg(),
            info.getFrameIdx(),
            info.getRegisterClass());
        Util.assertion(idx != 0, "loadRegFromStackSlot didn't insert any code!");
        if (atStart)
          idx = 0;
        else {
          idx = beforeIdx;
          ++idx;
        }
      }
    }
  }

  /**
   * Scans the entirely function for finding modified callee-saved registers,
   * and inserts spill and restore code for them when desired. Also calculate
   * the MaxFrameSize and hasCall variable for function's frame information
   * and eliminating call frame pseudo instructions.
   *
   * @param mf
   */
  private void calculateCalleeSavedRegisters(MachineFunction mf) {
    TargetRegisterInfo regInfo = mf.getTarget().getRegisterInfo();
    TargetInstrInfo tii = mf.getTarget().getInstrInfo();
    TargetFrameLowering tfi = mf.getTarget().getFrameLowering();
    MachineRegisterInfo mri = mf.getMachineRegisterInfo();
    MachineFrameInfo mfi = mf.getFrameInfo();

    int[] calleeSavedRegs = regInfo.getCalleeSavedRegs(mf);
    int frameSetupOpcode = tii.getCallFrameSetupOpcode();
    int frameDestroyOpcode = tii.getCallFrameDestroyOpcode();

    minCSFrameIndex = Integer.MAX_VALUE;
    maxCSFrameIndex = 0;

    // If the target machine don't support frame setup/destroy pseudo instr,
    // or there is no callee-saved registers.
    // return early.
    if (calleeSavedRegs == null || calleeSavedRegs.length == 0)
      return;

    MCRegisterClass[] csRegClasses = regInfo.getCalleeSavedRegClasses(mf);
    ArrayList<CalleeSavedInfo> csInfo = new ArrayList<>();
    for (int i = 0, sz = calleeSavedRegs.length; i != sz; i++) {
      int reg = calleeSavedRegs[i];
      if (mri.isPhysRegUsed(reg)) {
        // if the reg is used in this function (in other world, modified)
        // , save it.
        csInfo.add(new CalleeSavedInfo(reg, csRegClasses[i]));
      } else {
        // Check aliases register.
        int[] aliases = regInfo.getAliasSet(reg);
        if (aliases != null) {
          for (int alias : aliases) {
            if (mri.isPhysRegUsed(alias)) {
              // needs check only once!.
              csInfo.add(new CalleeSavedInfo(reg, csRegClasses[i]));
              break;
            }
          }
        }
      }
    }

    // If no callee saved registers are used, early exit.
    if (csInfo.isEmpty())
      return;

    Pair<Integer, Integer>[] fixedSpillSlots = tfi.getCalleeSavedSpillSlots();
    for (CalleeSavedInfo info : csInfo) {
      int reg = info.getReg();
      MCRegisterClass rc = info.getRegisterClass();

      OutRef<Integer> temp = new OutRef<>(0);
      if (regInfo.hasReservedSpillSlot(mf, reg, temp)) {
        info.setFrameIdx(temp.get());
        continue;
      }
      int frameIdx = temp.get();

      int fixedSlot = 0;
      if (fixedSpillSlots != null) {
        while (fixedSlot < fixedSpillSlots.length &&
            fixedSpillSlots[fixedSlot].first != reg)
          ++fixedSlot;
      }

      if (fixedSpillSlots == null || fixedSlot == fixedSpillSlots.length) {
        int align = tri.getSpillAlignment(rc);
        int stackAlign = tfi.getStackAlignment();

        align = Math.min(align, stackAlign);
        frameIdx = mfi.createStackObject(tri.getRegSize(rc), align);
        if (frameIdx < minCSFrameIndex)
          minCSFrameIndex = frameIdx;
        if (frameIdx > maxCSFrameIndex)
          maxCSFrameIndex = frameIdx;
      } else {
        // Spill it to the stack where we must.
        frameIdx = mfi.createFixedObject(tri.getRegSize(rc), fixedSpillSlots[fixedSlot].second);
      }
      info.setFrameIdx(frameIdx);
    }

    mfi.setCalleeSavedInfo(csInfo);
  }

  /**
   * Calculate actual frame offsets for all of the stack object.
   *
   * @param mf
   */
  private void calculateFrameObjectOffsets(MachineFunction mf) {
    TargetFrameLowering tfl = mf.getTarget().getFrameLowering();
    boolean stackGrowDown = tfl.getStackGrowDirection() == StackGrowDown;

    MachineFrameInfo mfi = mf.getFrameInfo();
    int maxAlign = mfi.getMaxAlignment();

    long offset = tfl.getLocalAreaOffset();
    if (stackGrowDown)
      offset = -offset;
    Util.assertion(offset >= 0, "Local area offset should be in direction of stack growth");

    for (int i = mfi.getObjectIndexBegin(); i != 0; ++i) {
      long fixedOffset;
      if (stackGrowDown) {
        fixedOffset = -mfi.getObjectOffset(i);
      } else {
        fixedOffset = mfi.getObjectOffset(i) + mfi.getObjectSize(i);
      }
      if (fixedOffset > offset)
        offset = fixedOffset;
    }

    if (stackGrowDown) {
      for (int i = minCSFrameIndex; i <= maxCSFrameIndex; i++) {
        offset += mfi.getObjectSize(i);

        int align = mfi.getObjectAlignment(i);
        maxAlign = Math.max(maxAlign, align);
        offset = (offset + align - 1) / align * align;

        mfi.setObjectOffset(i, -offset);
      }
    } else {
      int maxCSFI = maxCSFrameIndex, minCSFI = minCSFrameIndex;
      for (int i = maxCSFI; i >= minCSFI; i--) {
        int align = mfi.getObjectAlignment(i);

        maxAlign = Math.max(maxAlign, align);
        offset = (offset + align - 1) / align * align;
        mfi.setObjectOffset(i, offset);
        offset += mfi.getObjectSize(i);
      }
    }

    TargetRegisterInfo regInfo = mf.getTarget().getRegisterInfo();
    if (rs != null && tfl.hasFP(mf)) {
      int sfi = rs.getScavengingFrameIndex();
      if (sfi >= 0) {
        OutRef<Long> t1 = new OutRef<>(offset);
        OutRef<Integer> t2 = new OutRef<>(maxAlign);
        adjustStackOffset(mfi, sfi, stackGrowDown, t1, t2);
        offset = t1.get();
        maxAlign = t2.get();
      }
    }

    //todo if (mfi.getStackProtectorIndex()>0) for StackProtector.
    OutRef<Long> t1 = new OutRef<>();
    OutRef<Integer> t2 = new OutRef<>();
    for (int i = 0, e = mfi.getObjectIndexEnd(); i != e; i++) {
      if (i >= minCSFrameIndex && i <= maxCSFrameIndex)
        continue;

      if (rs != null && i == rs.getScavengingFrameIndex())
        continue;
      if (mfi.isDeadObjectIndex(i))
        continue;
      //todo if (mfi.getStackProtectorIndex() == i) continue;

      t1.set(offset);
      t2.set(maxAlign);
      adjustStackOffset(mfi, i, stackGrowDown, t1, t2);
      offset = t1.get();
      maxAlign = t2.get();
    }

    if (rs != null && !tfl.hasFP(mf)) {
      int sfi = rs.getScavengingFrameIndex();
      if (sfi >= 0) {
        t1.set(offset);
        t2.set(maxAlign);
        adjustStackOffset(mfi, sfi, stackGrowDown, t1, t2);
        offset = t1.get();
        maxAlign = t2.get();
      }
    }

    if (!regInfo.targetHandlessStackFrameRounding() &&
        (mfi.hasCalls() || mfi.hasVarSizedObjects() ||
            (regInfo.needsStackRealignment(mf) &&
                mfi.getObjectIndexEnd() != 0))) {
      if (tfl.hasReservedCallFrame(mf))
        offset += mfi.getMaxCallFrameSize();

      int alignMask = Math.max(tfl.getStackAlignment(), maxAlign) - 1;
      offset = (offset + alignMask) & ~alignMask;
    }

    mfi.setStackSize((int) (offset + tfl.getLocalAreaOffset()));

    mfi.setMaxAlignment(maxAlign);
  }

  /**
   * Helpful function used to adjust the stack frame offset.
   *
   * @param mfi
   * @param frameIdx
   * @param stackGrowDown
   * @param offset
   * @param maxAlign
   */
  private static void adjustStackOffset(
      MachineFrameInfo mfi,
      int frameIdx,
      boolean stackGrowDown,
      OutRef<Long> offset,
      OutRef<Integer> maxAlign) {
    if (stackGrowDown)
      offset.set(offset.get() + mfi.getObjectSize(frameIdx));

    int align = mfi.getObjectAlignment(frameIdx);

    // If the alignment of this object is greater than that of the stack, then
    // increase the stack alignment to match.
    maxAlign.set(Math.max(maxAlign.get(), align));

    // Adjust to alignment boundary.
    long t = (offset.get() + align - 1) / align * align;
    offset.set(t);
    if (stackGrowDown)
      mfi.setObjectOffset(frameIdx, -offset.get());
    else {
      mfi.setObjectOffset(frameIdx, offset.get());
      offset.set(offset.get() + mfi.getObjectSize(frameIdx));
    }
  }

  /**
   * Emits code for inserting prologue and epilogue code to mf.
   *
   * @param mf
   */
  private void insertPrologEpilogCode(MachineFunction mf) {
    TargetFrameLowering tfl = mf.getTarget().getFrameLowering();
    tfl.emitPrologue(mf);

    for (MachineBasicBlock mbb : mf.getBasicBlocks()) {
      if (!mbb.isEmpty() && mbb.getLastInst().getDesc().isReturn())
        tfl.emitEpilogue(mf, mbb);
    }
  }

  /**
   * Replace all MO_FrameIndex operands with physical register
   * references and actual offsets.
   *
   * @param mf
   */
  private void replaceFrameIndices(MachineFunction mf) {
    if (!mf.getFrameInfo().hasStackObjects())
      return;

    TargetMachine tm = mf.getTarget();
    Util.assertion(tm.getRegisterInfo() != null);
    TargetRegisterInfo regInfo = tm.getRegisterInfo();
    for (MachineBasicBlock mbb : mf.getBasicBlocks()) {
      for (int i = 0; i < mbb.size(); i++) {
        MachineInstr mi = mbb.getInstAt(i);
        for (int j = 0; j < mi.getNumOperands(); j++)
          if (mi.getOperand(j).isFrameIndex())
            regInfo.eliminateFrameIndex(mf, mi);
      }
    }
  }

  /**
   * Creates a pass used for emitting prologue and epilogue code to function.
   * Also eliminating abstract frame index with actual stack slot reference.
   *
   * @return
   */
  public static FunctionPass createPrologEpilogEmitter() {
    return new PrologEpilogInserter();
  }

  private String stringifyCSRegSet(BitMap usedCSRegs) {
    TargetRegisterInfo tri = mf.getTarget().getRegisterInfo();
    ArrayList<CalleeSavedInfo> csInfo = mf.getFrameInfo().getCalleeSavedInfo();

    StringBuilder buf = new StringBuilder();
    if (csInfo.isEmpty()) {
      return "[]";
    }
    buf.append("[");
    for (int idx = 0, e = usedCSRegs.size(); idx != e; ++idx) {
      if (!usedCSRegs.get(idx))
        continue;

      int reg = csInfo.get(idx).getReg();
      buf.append(tri.getName(reg));
      if (idx < e - 1)
        buf.append(",");
    }
    buf.append("]");
    return buf.toString();
  }

  public void dumpAllUsed() {
    mf.getBasicBlocks().forEach(this::dumpUsed);
  }

  public void dumpSet(BitMap s) {
    System.err.println(stringifyCSRegSet(s));
  }

  public void dumpSets(MachineBasicBlock mbb) {
    if (mbb != null) {
      System.err.printf("%s | %s | %s | %s | %s | %s\n",
          getBasicBlockName(mbb),
          stringifyCSRegSet(csrUsed.get(mbb)),
          stringifyCSRegSet(anticIn.get(mbb)),
          stringifyCSRegSet(anticOut.get(mbb)),
          stringifyCSRegSet(availIn.get(mbb)),
          stringifyCSRegSet(availOut.get(mbb)));
    }
  }

  public void dumpUsed(MachineBasicBlock mbb) {
    if (mbb != null) {
      System.err.printf("CSRUsed[%s] = %s\n",
          getBasicBlockName(mbb),
          stringifyCSRegSet(csrUsed.get(mbb)));
    }
  }

  public void dumpSets1(MachineBasicBlock mbb) {
    if (mbb != null) {
      System.err.printf("%s | %s | %s | %s | %s | %s | %s | %s\n",
          getBasicBlockName(mbb),
          stringifyCSRegSet(csrUsed.get(mbb)),
          stringifyCSRegSet(anticIn.get(mbb)),
          stringifyCSRegSet(anticOut.get(mbb)),
          stringifyCSRegSet(availIn.get(mbb)),
          stringifyCSRegSet(availOut.get(mbb)),
          stringifyCSRegSet(csrSave.get(mbb)),
          stringifyCSRegSet(csrRestore.get(mbb)));
    }
  }

  public void dumpAllSets() {
    mf.getBasicBlocks().forEach(this::dumpSets1);
  }

  public void dumpSRSets() {
    mf.getBasicBlocks().forEach(mbb ->
    {
      if (!csrSave.get(mbb).isEmpty()) {
        System.err.printf("Save[%s] = %s",
            getBasicBlockName(mbb),
            stringifyCSRegSet(csrSave.get(mbb)));
        if (csrRestore.get(mbb).isEmpty())
          System.err.println();
      }
      if (!csrRestore.get(mbb).isEmpty()) {
        if (!csrSave.get(mbb).isEmpty())
          System.err.printf("    ");
        System.err.printf("Restore[%s] = %s\n",
            getBasicBlockName(mbb),
            stringifyCSRegSet(csrRestore.get(mbb)));
      }
    });
  }
}
