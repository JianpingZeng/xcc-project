package backend.asm;

import backend.lir.backend.ByteOrder;
import backend.lir.backend.MachineInfo;

/**
 * The platform-independent abstract base class for assembler.
 *
 * @author Xlous.zeng
 */
public abstract class AbstractAssembler
{
	/**
	 * The target machine this assembler used for.
	 */
	public final MachineInfo target;
	/**
	 * Code buffer.
	 */
	public final Buffer codeBuffer;

	public AbstractAssembler(MachineInfo target)
	{
		this.target = target;

		if (target.arch.byteOrder == ByteOrder.BigEndian)
		{
			this.codeBuffer = new Buffer.BigEndian();
		}
		else
		{
			this.codeBuffer = new Buffer.LittleEndian();
		}
	}

	public final void bind(Label l)
	{
		assert !l.isBound() : "can bind label only once";
		l.bind(codeBuffer.position());
		l.patchInstructions(this);
	}

	protected abstract void patchJumpTarget(int branch, int target);

	protected final void emitByte(int x)
	{
		codeBuffer.emitByte(x);
	}

	protected final void emitShort(int x)
	{
		codeBuffer.emitShort(x);
	}

	protected final void emitInt(int x)
	{
		codeBuffer.emitInt(x);
	}

	protected final void emitLong(long x)
	{
		codeBuffer.emitLong(x);
	}
}
