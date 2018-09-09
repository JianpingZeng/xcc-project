package jlang.support;

import java.util.ArrayList;

/**
 * Keeps track of various options which control how the code is optimized and
 * passed to the backend.
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public class CompileOptions {
  public enum InliningMethod {
    /**
     * Performs no inlining.
     */
    NoInlining,

    /**
     * Use the standard function inline pass.
     */
    NormalInlining,

    /**
     * Only run the always inlining pass.
     */
    OnlyAlwaysInlining
  }

  /**
   * The -O[0-4] option specified.
   */
  public byte optimizationLevel = 0;
  /**
   * If -Os specified.
   */
  public boolean optimizeSize = false;
  /**
   * If -g specified.
   */
  public boolean debugInfo = false;
  /**
   * Control whether the loop unrolling pass is taken.
   */
  public boolean unrollLoops = false;
  /**
   * What kind of inlining would be taken.
   */
  public InliningMethod inlining = InliningMethod.NoInlining;
  /**
   * An optional CPU to backend.target.
   */
  public String CPU;

  public ArrayList<String> features = new ArrayList<>();
}
