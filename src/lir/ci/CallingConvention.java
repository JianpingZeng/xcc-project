package lir.ci;

import lir.backend.RegisterConfig;

/**
 * A calling convention describes the locations where the arguments for a call
 * are placed.
 */
public class CallingConvention
{
	/**
	 * Constants denoting the type of a call for which a calling convention is
	 * {@linkplain RegisterConfig#getCallingConvention(Type, LIRKind[],
	 * lir.backend.TargetMachine, boolean) requested}.
	 */
	public enum Type
	{
		/**
		 * A request for the outgoing argument locations at a call site to Java code.
		 */
		JavaCall(true),

		/**
		 * A request for the incoming argument locations.
		 */
		JavaCallee(false),

		/**
		 * A request for the outgoing argument locations at a call site to the
		 * runtime (which may be Java or native code).
		 */
		RuntimeCall(true),

		/**
		 * A request for the outgoing argument locations at a call site to
		 * external native code that complies with the platform ABI.
		 */
		NativeCall(true);

		/**
		 * Determines if this is a request for the outgoing argument locations at a call site.
		 */
		public final boolean out;

		public static final Type[] VALUES = values();

		private Type(boolean out)
		{
			this.out = out;
		}
	}

	/**
	 * The amount of stack space (in bytes) required for the stack-based arguments
	 * of the call.
	 */
	public final int stackSize;

	/**
	 * The locations in which the arguments are placed. This array ordered by
	 * argument index, which is organized as follows:
	 * <pre>
	 *  |--leftmost param|-----|--right most param--|
	 *  |--locations[0]--|-----|--locations[n-1]--|
	 * </pre>
	 *  where, n is the numbers of real parameters being passed into callee function.
	 * according to <a href="http://www.intel.com/content/dam/www/public/us/en/documents/manuals/64-ia-32-architectures-software-developer-manual-325462.pdf">AMD64 calling convention</a>.
	 */
	public final LIRValue[] locations;

	public CallingConvention(LIRValue[] locations, int stackSize)
	{
		this.locations = locations;
		this.stackSize = stackSize;
		assert verify();
	}

	@Override public String toString()
	{
		StringBuilder result = new StringBuilder();
		result.append("CallingConvention[");
		for (LIRValue op : locations)
		{
			result.append(op.toString()).append(" ");
		}
		result.append("]");
		return result.toString();
	}

	private boolean verify()
	{
		for (int i = 0; i < locations.length; i++)
		{
			LIRValue location = locations[i];
			assert location.isStackSlot() || location.isRegister();
		}
		return true;
	}
}

