package backend.passManaging;

import backend.pass.AnalysisResolver;
import backend.pass.Pass;
import backend.value.Function;
import backend.value.Module;

/**
 * FunctionPassManager manages FunctionPasses and BasicBlockPassManagers.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class FunctionPassManager implements PassManagerBase {
  private FunctionPassManagerImpl fpm;
  private Module m;

  public FunctionPassManager(Module m) {
    fpm = new FunctionPassManagerImpl(0);
    fpm.setTopLevelManager(fpm);
    this.m = m;

    AnalysisResolver resolver = new AnalysisResolver(fpm);
    fpm.setAnalysisResolver(resolver);
  }

  /**
   * Execute all of the passes scheduled for execution.  Keep
   * track of whether any of the passes modifies the function, and if
   * so, return true.
   *
   * @param f
   * @return
   */
  public boolean run(Function f) {
    switch (f.getName()) {
        // bit width must be same in handleJTSwitchCase
      case "sqlite3ExprAffinity":
      case "sqlite3ExprCollSeq":

        // simplifyDemandedBits
//      case "getOverflowPage":
//      case "sqlite3VXPrintf":
//      case "sqlite3_backup_step":

        // Exception in thread "main" java.lang.RuntimeException: Unsupported extended type!
      case "destroyRootPage":
      case "sqlite3NestedParse":
      case "sqlite3Reindex":
      case "sqlite3Pragma":
      case "sqlite3GenerateRowDelete":
      case "sqlite3CodeRowTriggerDirect":
      case "getRowTrigger":
      case "sqlite3DeleteFrom":
      case "sqlite3Insert":
      case "codeTriggerProgram":
      case "sqlite3FkActions":
      case "sqlite3GenerateConstraintChecks":
      case "sqlite3Update":
      case "sqlite3FkCheck":
      case "sqlite3LocateTable":
        return false;
    }
    return fpm.run(f);
  }

  /**
   * Add a pass to the queue of passes to run.
   *
   * @param p
   */
  @Override
  public void add(Pass p) {
    fpm.add(p);
  }

  /**
   * Run all the initialization works for those passes.
   *
   * @return
   */
  public boolean doInitialization() {
    return fpm.doInitialization(m);
  }

  /**
   * * Run all the finalization works for those passes.
   *
   * @return
   */
  public boolean doFinalization() {
    return fpm.doFinalization(m);
  }
}
