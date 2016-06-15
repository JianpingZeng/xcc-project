package hir;

/**
 * Contains statistics gathered during the compilation of a method and reported back
 * from the compiler as the result of compilation.
 *
 * @author Xlous.zeng
 */
public class Statistics
{
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
	 * The number of methods inlined.
	 */
	public int inlineCount;

	/**
	 * The number of methods folded (i.e. evaluated).
	 */
	public int foldCount;

	/**
	 * The number of intrinsics inlined in this compilation.
	 */
	public int intrinsicCount;

}
