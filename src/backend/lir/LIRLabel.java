package backend.lir;

import backend.lir.ci.LIRValue;
import backend.asm.Label;

/**
 * The {@code LIRLabel} class definition.
 */
public class LIRLabel extends LIROp0
{

	private Label label;

	/**
	 * Constructs a LIRLabel instruction.
	 *
	 * @param label the label
	 */
	public LIRLabel(Label label)
	{
		super(LIROpcode.Label, LIRValue.IllegalValue);
		assert label != null;
		this.label = label;
	}

	/**
	 * Gets the label associated to this instruction.
	 *
	 * @return the label
	 */
	public Label label()
	{
		return label;
	}

	/**
	 * Emits targetAbstractLayer assembly code for this LIRLabel instruction.
	 *
	 * @param masm the LIRAssembler
	 */
	@Override public void emitCode(LIRAssembler masm)
	{
		//masm.emitOpLabel(this);
	}

	/**
	 * Prints this instruction to a LogStream.
	 */
	@Override public String operationString(LIRValue.Formatter operandFmt)
	{
		return label.isBound() ? String.valueOf(label.position()) : "?";
	}
}

