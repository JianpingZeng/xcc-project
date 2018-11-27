package backend.codegen;
/*
 * Extremely C language Compiler
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

import backend.support.IntStatistic;
import backend.target.TargetMachine;
import tools.Util;
import tools.commandline.*;

import java.util.Arrays;

import static backend.codegen.VirtRegRewriter.RewriterName.Local;
import static backend.codegen.VirtRegRewriter.RewriterName.Simple;
import static backend.target.TargetRegisterInfo.FirstVirtualRegister;
import static backend.target.TargetRegisterInfo.isVirtualRegister;
import static tools.commandline.FormattingFlags.Prefix;
import static tools.commandline.OptionNameApplicator.optionName;

/**
 * This interface implements an interface that rewriting the reference to virtual
 * registers to physical register or stack slot.
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public abstract class VirtRegRewriter {
  public static IntStatistic numSpills =
      new IntStatistic("VirtPhyWriter", "Number of register spills");
  public static IntStatistic numStores =
      new IntStatistic("VirtPhyWriter", "Number of stores added");
  public static IntStatistic numLoads =
      new IntStatistic("VirtPhyWriter", "Number of loads added");

  public enum RewriterName {
    Simple,
    Local
  }

  public static Opt<RewriterName> RewriterOpt = new Opt<>(
      new Parser<>(),
      optionName("virtRegRewriter"),
      new FormattingFlagsApplicator(Prefix),
      Desc.desc("Choose a rewriter(currently only simple supported)"),
      new ValueClass<>(
          new ValueClass.Entry<>(Simple, "simple", "simple virtual register rewriter"),
          new ValueClass.Entry<>(Local, "local", "local virtual register rewriter")),
      Initializer.init(Simple));


  public abstract boolean runOnMachineFunction(MachineFunction mf, VirtRegMap vrm);

  /**
   * create an return a spiller object, as specified on the command line.
   *
   * @return
   */
  public static VirtRegRewriter createVirtRegRewriter() {
    return new SimpleVirtRegRewriter();
        /*
        if (RewriterOpt.value == Simple)

        else
            return new LocalVirtRegRewriter();
        */
  }

  public static class SimpleVirtRegRewriter extends VirtRegRewriter {
    @Override
    public boolean runOnMachineFunction(MachineFunction mf, VirtRegMap vrm) {
      if (Util.DEBUG) {
        System.err.printf("********* Rewrite machine code *********\n");
        System.err.printf("********* Function: %s\n", mf.getFunction().getName());
      }

      TargetMachine tm = mf.getTarget();
      MachineRegisterInfo mri = mf.getMachineRegisterInfo();
      if (mri.getLastVirReg() < FirstVirtualRegister)
        return false;

      // Map the virtual register to it's used or not.
      boolean[] loaded = new boolean[mri.getLastVirReg() - FirstVirtualRegister];

      for (MachineBasicBlock mbb : mf.getBasicBlocks()) {
        if (Util.DEBUG)
          System.err.printf("%s:\n", mbb.getBasicBlock().getName());
        for (int i = 0; i < mbb.size(); i++) {
          MachineInstr mi = mbb.getInstAt(i);
          for (int j = 0, e = mi.getNumOperands(); j < e; j++) {
            MachineOperand mo = mi.getOperand(j);
            if (mo.isRegister() && mo.getReg() != 0 &&
                isVirtualRegister(mo.getReg())) {
              int virtReg = mo.getReg();
              int physReg = vrm.getPhys(virtReg);
              if (mo.isUse() && vrm.hasStackSlot(virtReg)
                  && !loaded[virtReg - FirstVirtualRegister]) {
                tm.getInstrInfo().loadRegFromStackSlot(mbb, i,
                    physReg, vrm.getStackSlot(virtReg),
                    mri.getRegClass(virtReg));
                loaded[virtReg - FirstVirtualRegister] = true;
                if (Util.DEBUG) {
                  System.err.printf("\t");
                  mbb.getInstAt(i - 1).print(System.err, tm);
                }
                numLoads.inc();
              }
              if (mo.isDef() && vrm.hasStackSlot(virtReg)) {
                tm.getInstrInfo().storeRegToStackSlot(
                    mbb, i + 1,
                    physReg,
                    false,
                    vrm.getStackSlot(virtReg),
                    mri.getRegClass(virtReg));
                numStores.inc();
              }
              mi.setMachineOperandReg(j, physReg);
              mri.setPhysRegUsed(physReg);
            }
          }
          if (Util.DEBUG) {
            System.err.print("\t");
            mi.print(System.err, tm);
          }
          Arrays.fill(loaded, false);
        }
      }
      return true;
    }
  }

  public static class LocalVirtRegRewriter extends VirtRegRewriter {
    @Override
    public boolean runOnMachineFunction(MachineFunction mf, VirtRegMap vrm) {
      Util.assertion(false, "Current local spiller is not supported!");
      return false;
    }
  }
}
