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
	private TargetMachine target;
	/**
	 * A list containing all machine basic block.
	 */
	private LinkedList<MachineBasicBlock> basicBlocks;
	/**
	 * Used to keep track of stack frame information about target.
	 */
	private MachineFrameInfo frameInfo;
	/**
	 * Keeping track of mapping from SSA values to registers.
	 */
	private SSARegMap ssaRegMap;
	/**
	 * Keep track of constants to be spilled into stack slot.
	 */
	private MachineConstantPool constantPool;

	public MachineFunction(Function fn, TargetMachine tm)
	{
		this.fn = fn;
		target = tm;
		basicBlocks = new LinkedList<>();
		frameInfo = new MachineFrameInfo();
		ssaRegMap = new SSARegMap();
		constantPool = new MachineConstantPool();

		// associate this machine function with HIR function.
		fn.setMachineFunc(this);
	}

	public Function getFunction() {return fn;}

	public TargetMachine getTargetMachine() {return target;}

	public MachineBasicBlock getFirst() {return basicBlocks.getFirst();}

	public MachineFrameInfo getFrameInfo() {return frameInfo;}

	public SSARegMap getSsaRegMap(){return ssaRegMap;}

	public void clearSSARegMap() {ssaRegMap.clear();}

	public MachineConstantPool getConstantPool(){return constantPool;}

	public LinkedList<MachineBasicBlock> getBasicBlocks() {return basicBlocks;}
}
