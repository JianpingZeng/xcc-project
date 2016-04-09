package lir;

import asm.Label;

/**
 * @author Jianping Zeng
 */
public class LIRList
{
	public void branchDesination(Label label)
	{
		append(new LIRLabel(label));
	}

	private void append(LIRInstruction inst)
	{

	}
}
