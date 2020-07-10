package backend.codegen;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng
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

import backend.analysis.MachineDomTree;
import backend.analysis.MachineLoopInfo;
import backend.pass.AnalysisUsage;
import backend.support.DepthFirstOrder;
import backend.support.MachineFunctionPass;
import backend.target.TargetOpcode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * This pass used for eliminating the unreachable machine basic block from
 * machine function.
 * It must be performed after LoopInfo and DomTree.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public final class UnreachableMachineBlockElim extends MachineFunctionPass {
  @Override
  public String getPassName() {
    return "Removing the unreachable machine block";
  }

  /**
   * This method must be overridded by concrete subclass for performing
   * desired machine code transformation or analysis.
   *
   * @param mf
   * @return
   */
  @Override
  public boolean runOnMachineFunction(MachineFunction mf) {
    ArrayList<MachineBasicBlock> reachable;

    MachineDomTree mdt = (MachineDomTree) getAnalysisToUpDate(MachineDomTree.class);
    MachineLoopInfo mli = (MachineLoopInfo) getAnalysisToUpDate(MachineLoopInfo.class);

    // mark all reachable machine block.
    reachable = DepthFirstOrder.reversePostOrder(mf.getEntryBlock());

    // loop all dead blocks, remembering them and then deleting all instr
    // in them.
    LinkedList<MachineBasicBlock> deaded = new LinkedList<>();
    for (MachineBasicBlock mbb : mf.getBasicBlocks()) {
      if (!reachable.contains(mbb)) {
        deaded.addLast(mbb);

        // update dominator and loop info.
        if (mli != null)
          mli.removeBlock(mbb);
        if (mdt != null && mdt.getNode(mbb) != null)
          mdt.eraseNode(mbb);

        for (int i = 0; i < mbb.getNumSuccessors(); ) {
          MachineBasicBlock succ = mbb.suxAt(i);

          for (ListIterator<MachineInstr> itr = succ.getInsts().listIterator();
               itr.hasNext(); ) {
            MachineInstr mi = itr.next();
            if (mi.getOpcode() != TargetOpcode.PHI)
              break;

            for (int j = mi.getNumOperands() - 1; j >= 2; j -= 2) {
              if (mi.getOperand(j).isMBB()
                  && mi.getOperand(j).getMBB() == mbb) {
                mi.removeOperand(j);
                mi.removeOperand(j - 1);
              }
            }
          }
          mbb.removeSuccessor(0);
        }
      }
    }

    for (MachineBasicBlock dead : deaded) {
      // TODO add MachineModuleInfo pass 2016.12.01.
      dead.eraseFromParent();
    }

    // cleanup phi node.
    for (int i = 0, e = mf.getNumBlocks(); i < e; i++) {
      // Prune the unneeded PHI nodes.
      MachineBasicBlock mbb = mf.getMBBAt(i);
      if (mbb.isEmpty() || mbb.getInstAt(0).getOpcode() != TargetOpcode.PHI)
        continue;

      HashSet<MachineBasicBlock> pred = new HashSet<>();
      pred.addAll(mbb.getPredecessors());

      MachineInstr phi;
      for (int j = 0; j < mbb.size() && (phi = mbb.getInstAt(j))
          .getOpcode() == TargetOpcode.PHI; ) {
        for (int k = phi.getNumOperands() - 1; k >= 1; k -= 2) {
          // Removes some phi entries that is not from predecessor.
          if (!pred.contains(phi.getOperand(k).getMBB())) {
            phi.removeOperand(k);
            phi.removeOperand(k - 1);
          }
        }

        // If this phi have only one input argument, remove it from MBB.
        if (phi.getNumOperands() == 3) {
          int input = phi.getOperand(1).getReg();
          int output = phi.getOperand(0).getReg();
          int phiPos = j;

          // advance to next inst.
          // Note that, when removing some element in ArrayList, all
          // of elements after the deleted one shifted left.
          // So, the next one is located at phiPos.
          j = phiPos;

          // remove this phi inst.
          mbb.erase(phiPos);
          if (input != output)
            mf.getMachineRegisterInfo().replaceRegWith(output, input);
          continue;
        }
        j++;
      }
    }

    // re-number all blocks in machine function.
    mf.renumberBlocks();

    return !deaded.isEmpty();
  }

  /**
   * Add all required MachineFunctionPasses for UnreachableMachineBlockElim
   * to the PassManager, like MachineLoopInfo and MachineDomTree.
   *
   * @param au
   */
  @Override
  public void getAnalysisUsage(AnalysisUsage au) {
    au.addPreserved(MachineLoopInfo.class);
    au.addPreserved(MachineDomTree.class);
    super.getAnalysisUsage(au);
  }
}
