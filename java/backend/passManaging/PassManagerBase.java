package backend.passManaging;

import backend.pass.Pass;

/**
 * An abstract interface to allow code to add
 * passes to a pass manager without having to hard-code what
 * kind of pass manager it is.
 *
 * @author Jianping Zeng
 * @version 0.1
 * @T This generic type parameter represents the type of entity on which different
 * Pass will operates.
 * @PassType Represents the type of pass.
 */
public interface PassManagerBase {
  /**
   * Add a pass to the queue of passes to run.
   *
   * @param p
   */
  void add(Pass p);
}
