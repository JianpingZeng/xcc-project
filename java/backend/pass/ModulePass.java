package backend.pass;

import backend.passManaging.PMStack;
import backend.passManaging.PassManagerType;
import backend.support.PrintModulePass;
import backend.value.Module;
import tools.Util;

import java.io.PrintStream;

import static backend.passManaging.PassManagerType.PMT_ModulePassManager;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public interface ModulePass extends Pass {
  /**
   * This method should be overridden by concrete subclasses to perform
   * user-defined operation.
   *
   * @param m
   * @return
   */
  boolean runOnModule(Module m);

  @Override
  default void assignPassManager(PMStack pms) {
    assignPassManager(pms, PMT_ModulePassManager);
  }

  @Override
  default void assignPassManager(PMStack pms,
                                 PassManagerType preferredType) {
    while (!pms.isEmpty()) {
      PassManagerType ty = pms.peek().getPassManagerType();
      if (ty.compareTo(PMT_ModulePassManager) > 0)
        pms.pop();
      else
        break;
    }
    Util.assertion(!pms.isEmpty(), "Unable to find appropriate pass manager!");
    pms.peek().add(this);
  }

  default PassManagerType getPotentialPassManagerType() {
    return PMT_ModulePassManager;
  }

  @Override
  default Pass createPrinterPass(PrintStream os, String banner) {
    return PrintModulePass.createPrintModulePass(os);
  }
}
