package lir;

import lir.ci.CiKind;
import lir.ci.StackSlot;
import static lir.ci.CiKind.*;

/**
 * A compiler stub is a shared routine that performs an operation on behalf of compiled code.
 * Typically the routine is too large to inline, is infrequent, or requires runtime support.
 * Compiler stubs are called with a callee-save convention; the compiler stub must save any
 * registers it may destroy and then restore them upon return. This allows the register
 * allocator to ignore calls to compiler stubs. Parameters to compiler stubs are
 * passed on the stack in order to preserve registers for the rest of the code.
 */
public class CompilerStub
{

	public enum Id
	{

		fneg(Float, Float),
		dneg(Double, Double),
		f2i(Int, Float),
		f2l(Long, Float),
		d2i(Int, Double),
		d2l(Long, Double);

		public final CiKind resultKind;
		public final CiKind[] arguments;

		private Id(CiKind resultKind, CiKind... args)
		{
			this.resultKind = resultKind;
			this.arguments = args;
		}
	}

	public final Id id;
	public final CiKind resultKind;
	public final Object stubObject;

	/**
	 * The slots in which the stub finds its incoming arguments.
	 * To get the arguments from the perspective of the stub's caller,
	 * use {@link StackSlot#asOutArg()}.
	 */
	public final StackSlot[] inArgs;

	/**
	 * The slot in which the stub places its return value (if any).
	 * To get the value from the perspective of the stub's caller,
	 * use {@link StackSlot#asOutArg()}.
	 */
	public final StackSlot outResult;

	public CompilerStub(Id id, CiKind resultKind, Object stubObject,
			StackSlot[] argSlots, StackSlot resultSlot)
	{
		this.id = id;
		this.resultKind = resultKind;
		this.stubObject = stubObject;
		this.inArgs = argSlots;
		this.outResult = resultSlot;
	}

}
