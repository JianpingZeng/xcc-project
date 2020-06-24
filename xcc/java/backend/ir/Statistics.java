package backend.ir;

/**
 * Contains statistics gathered during the compilation of a method and reported back
 * from the jlang.driver as the getReturnValue of compilation.
 *
 * @author Jianping Zeng
 */
public class Statistics {
  /**
   * The number of internal graph nodes created during this compilation.
   */
  public int nodeCount;

  /**
   * The number of basic blocks created during this compilation.
   */
  public int blockCount;

  /**
   * The number of loops in the compiled method.
   */
  public int loopCount;

  /**
   * The number of functionList inlined.
   */
  public int inlineCount;

  /**
   * The number of functionList folded (i.e. evaluated).
   */
  public int foldCount;

  /**
   * The number of intrinsics inlined in this compilation.
   */
  public int intrinsicCount;

}
