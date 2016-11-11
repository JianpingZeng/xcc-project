package driver;

/**
 * Keeps track of various options which control how the code is optimized and
 * passed to the backend.
 * @author Xlous.zeng
 * @version 0.1
 */
public class CompileOptions
{
	enum InliningMethod
	{
		/**
		 * Performs no inlining.
		 */
		NoInlining,

		/**
		 * Use the standard function inline pass.
		 */
		NormalInlining,

		/**
		 *  Only run the always inlining pass.
		 */
		OnlyAlwaysInlining
	}

	/**
	 * The -O[0-4] option specified.
	 */
	byte optimizationLevel = 0;
	/**
	 * If -Os specified.
	 */
	boolean optimizeSize = false;
	/**
	 * If -g specified.
	 */
	boolean debugInfo = false;
	/**
	 * Control whether the loop unrolling pass is taken.
	 */
	boolean unrollLoops = false;
	/**
	 * What kind of inlining would be taken.
	 */
	InliningMethod inlining = InliningMethod.NoInlining;
	/**
	 * An optional CPU to target.
	 */
	String CPU;
}
