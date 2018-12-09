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
