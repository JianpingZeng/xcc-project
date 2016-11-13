package backend.codegen;

import backend.target.TargetMachine;
import backend.value.Function;

import java.util.LinkedList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class MachineFunction
{
	private Function fn;
	private TargetMachine td;
	/**
	 * A list containing all machine basic block.
	 */
	private LinkedList<MachineBasicBlock> basicBlocks;
	/**
	 * Used to keep track of stack frame information about target.
	 */
	private MachineFrameInfo mfInfo;
	/**
	 * Keep track of constants to be spilled into stack slot.
	 */
	private MachineConstantPool constantPool;

	public MachineFunction(Function fn, TargetMachine tm)
	{}

	public Function getFunction() {return fn;}

	public TargetMachine getTargetMachine() {return td;}


}
