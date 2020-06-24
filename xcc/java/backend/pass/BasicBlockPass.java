package backend.pass;

import backend.passManaging.*;
import backend.support.PrintBasicBlockPass;
import backend.value.BasicBlock;
import backend.value.Function;
import backend.value.Module;
import tools.Util;

import java.io.PrintStream;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public interface BasicBlockPass extends Pass {
  /**
   * To run this pass on a function, we simply call runOnBasicBlock once for
   * each function.
   *
   * @return
   */
  default boolean runOnFunction(Function f) {
    return false;
  }

  boolean runOnBasicBlock(BasicBlock block);

  default boolean doInitialization(Module m) {
    return false;
  }

  default boolean doFinalization(Module m) {
    return false;
  }

  default boolean doInitialization(Function f) {
    return false;
  }

  default boolean doFinalization(Function f) {
    return false;
  }

  @Override
  default PassManagerType getPotentialPassManagerType() {
    return PassManagerType.PMT_BasicBlockPassManager;
  }

  @Override
  default void assignPassManager(PMStack pms,
                                 PassManagerType pmt) {
    while (!pms.isEmpty()) {
      if (pms.peek().getPassManagerType().compareTo(PassManagerType.PMT_BasicBlockPassManager) > 0) {
        pms.pop();
      } else
        break;
    }
    Util.assertion(!pms.isEmpty(), "Errorous status");
    BBPassManager bpm;
    if (!(pms.peek() instanceof FPPassManager)) {
      PMDataManager pmd = pms.peek();
      // Step#1 create new Function Pass Manager
      bpm = new BBPassManager(pmd.getDepth() + 1);
      bpm.populateInheritedAnalysis(pms);


      // Step#2 Assign manager to manage this new manager.
      bpm.assignPassManager(pms, pmd.getPassManagerType());
      // Step#3 Push new manager into stack.
      pms.push(bpm);
    }
    bpm = (BBPassManager) pms.peek();
    bpm.add(this);
  }

  @Override
  default void assignPassManager(PMStack pms) {
    assignPassManager(pms, PassManagerType.PMT_BasicBlockPassManager);
  }

  @Override
  default Pass createPrinterPass(PrintStream os, String banner) {
    return PrintBasicBlockPass.createPrintBasicBlockPass(os, banner);
  }
}
