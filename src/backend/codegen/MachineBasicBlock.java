package backend.codegen;

import backend.hir.BasicBlock;

import java.util.LinkedList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class MachineBasicBlock
{
	private LinkedList<MachineInstr> insts;
	private MachineBasicBlock prev, next;
	private final BasicBlock bb;

	public MachineBasicBlock(final BasicBlock bb)
	{
		this.bb = bb;
	}

	public BasicBlock getBasicBlock() {return bb;}

	public int size() {return insts.size();}

	public boolean isEmpty() {return insts.isEmpty();}

	public LinkedList<MachineInstr> getInsts() {return insts;}

	public void addLast(MachineInstr instr) {insts.addLast(instr);}

	public MachineInstr getInstAt(int index) {return insts.get(index);}

	public void insert(int itr, MachineInstr instr)
	{
		insts.add(itr, instr);
	}

	public void erase(int idx) {insts.remove(idx);}

	public void erase(int start, int end)
	{
		for (int i = start; i < end; i++)
			insts.remove(i);
	}

	public void replace(int idx, MachineInstr newInstr)
	{
		assert  idx>= 0 && idx < size():"idx out of range!";
		insts.remove(idx);
		insts.add(idx, newInstr);
	}

	public MachineInstr front() {return insts.getFirst();}

	public MachineInstr back() {return insts.getLast();}

}
