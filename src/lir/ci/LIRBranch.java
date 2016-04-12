package lir.ci;

/**
 * @author Jianping Zeng
 */

import asm.Label;
import hir.BasicBlock;
import hir.Condition;
import lir.LIRAssembler;
import lir.LIRInstruction;
import lir.LIRLabel;
import lir.LIROpcode;

/**
 *
 */
public class LIRBranch extends LIRInstruction
{

	private Condition cond;
	private CiKind kind;
	private Label label;

	/**
	 * The targetAbstractLayer block of this branch.
	 */
	private BasicBlock block;

	/**
	 * This is the unordered block for a float branch.
	 */
	private BasicBlock unorderedBlock;

	/**
	 * Creates a new LIRBranch instruction.
	 *
	 * @param cond  the branch condition
	 * @param label targetAbstractLayer label
	 */
	public LIRBranch(Condition cond, Label label)
	{
		super(LIROpcode.Branch, CiValue.IllegalValue, false);
		this.cond = cond;
		this.label = label;
	}

	/**
	 * Creates a new LIRBranch instruction.
	 *
	 * @param cond
	 * @param kind
	 * @param block
	 */
	public LIRBranch(Condition cond, CiKind kind, BasicBlock block)
	{
		super(LIROpcode.Branch, CiValue.IllegalValue, false);
		this.cond = cond;
		this.kind = kind;
		this.label = block.label();
		this.block = block;
		this.unorderedBlock = null;
	}

	public LIRBranch(Condition cond, CiKind kind, BasicBlock block,
			BasicBlock ublock)
	{
		super(LIROpcode.CondFloatBranch, CiValue.IllegalValue, false);
		this.cond = cond;
		this.kind = kind;
		this.label = block.label();
		this.block = block;
		this.unorderedBlock = ublock;
	}

	/**
	 * @return the condition
	 */
	public Condition cond()
	{
		return cond;
	}

	public Label label()
	{
		return label;
	}

	public BasicBlock block()
	{
		return block;
	}

	public BasicBlock unorderedBlock()
	{
		return unorderedBlock;
	}

	public void changeBlock(BasicBlock b)
	{
		assert block != null : "must have old block";
		assert block.label() == label() : "must be equal";

		this.block = b;
		this.label = b.label();
	}

	public void changeUblock(BasicBlock b)
	{
		assert unorderedBlock != null : "must have old block";
		this.unorderedBlock = b;
	}

	public void negateCondition()
	{
		cond = cond.negate();
	}

	@Override public void emitCode(LIRAssembler masm)
	{
		//masm.emitBranch(this);
	}

	@Override public String operationString(CiValue.Formatter operandFmt)
	{
		StringBuilder buf = new StringBuilder(cond().operator).append(' ');
		if (block() != null)
		{
			buf.append("[B").append(block.getID()).append(']');
		}
		else if (label().isBound())
		{
			buf.append("[label:0x")
					.append(Integer.toHexString(label().position()))
					.append(']');
		}
		else
		{
			buf.append("[label:??]");
		}
		if (unorderedBlock() != null)
		{
			buf.append("unordered: [B").append(unorderedBlock().getID())
					.append(']');
		}
		return buf.toString();
	}

	public void substitute(BasicBlock oldBlock, BasicBlock newBlock)
	{
		if (block == oldBlock)
		{
			block = newBlock;
			LIRInstruction instr = newBlock.getMachineBlock().lir()
					.instructionsList().get(0);
			assert instr instanceof LIRLabel : "first instruction of block must be label";
			label = ((LIRLabel) instr).label();
		}
		if (unorderedBlock == oldBlock)
		{
			unorderedBlock = newBlock;
		}
	}
}

