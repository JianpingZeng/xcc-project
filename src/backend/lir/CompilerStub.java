package backend.lir;

import static backend.lir.ci.LIRKind.Double;
import static backend.lir.ci.LIRKind.Float;
import static backend.lir.ci.LIRKind.Int;
import static backend.lir.ci.LIRKind.Long;
import backend.lir.ci.LIRKind;
import backend.lir.ci.StackSlot;

/**
 * A driver stub is a shared routine that performs an operation on behalf of
 * compiled code. Typically the routine is too large to inline, is infrequent,
 * or requires runtime support. CompilerInstance stubs are called with a callee-save
 * convention; the driver stub must save any LIRRegisters it may destroy and
 * then restore them upon return. This allows the register allocator to ignore
 * calls to driver stubs. Parameters to driver stubs are passed on the stack
 * in order to preserve LIRRegisters for the rest of the code.
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

		public final LIRKind resultKind;
		public final LIRKind[] arguments;

		Id(LIRKind resultKind, LIRKind... args)
		{
			this.resultKind = resultKind;
			this.arguments = args;
		}
	}

	public final Id id;
	public final LIRKind resultKind;
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

	public CompilerStub(Id id, LIRKind resultKind, Object stubObject,
			StackSlot[] argSlots, StackSlot resultSlot)
	{
		this.id = id;
		this.resultKind = resultKind;
		this.stubObject = stubObject;
		this.inArgs = argSlots;
		this.outResult = resultSlot;
	}

}
