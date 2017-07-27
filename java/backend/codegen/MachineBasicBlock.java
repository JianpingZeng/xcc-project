package backend.codegen;

import backend.value.BasicBlock;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Iterator;
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
	private TIntArrayList liveIns;

	/**
	 * Indicates the number of this machine block in the machine function.
	 */
	private int number;

	private ArrayList<MachineBasicBlock> predecessors;
	private ArrayList<MachineBasicBlock> successors;

	private MachineFunction parent;

	public MachineBasicBlock(final BasicBlock bb)
	{
		insts = new LinkedList<>();
		this.bb = bb;
		number = -1;
		predecessors = new ArrayList<>();
		successors = new ArrayList<>();
		liveIns = new TIntArrayList();
	}

	public BasicBlock getBasicBlock() { return bb; }

	public int size() {return insts.size();}

	public boolean isEmpty() {return insts.isEmpty();}

	public LinkedList<MachineInstr> getInsts() {return insts;}

	public void addLast(MachineInstr instr) {insts.addLast(instr);}

	public MachineInstr getInstAt(int index) {return insts.get(index);}

	public void insert(int itr, MachineInstr instr)
	{
		insts.add(itr, instr);
		instr.setParent(this);
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

	public ArrayList<MachineBasicBlock> getPredecessors()
	{
		return predecessors;
	}

	public ArrayList<MachineBasicBlock> getSuccessors()
	{
		return successors;
	}

	public void addSuccessor(MachineBasicBlock succ)
	{
		assert succ != null :"Can not add a null succ into succ list";
		successors.add(succ);
	}

	public void removeSuccessor(MachineBasicBlock succ)
	{
		assert predecessors.contains(succ)
				: "The pred to be removed not contained in succ list";
		successors.remove(succ);
	}

	public void removeSuccessor(int idx)
	{
		assert idx>= 0 && idx < getNumSuccessors();
		successors.remove(idx);
	}

	private void addPredecessor(MachineBasicBlock pred)
	{
		assert pred!= null :"Can not add a null pred";
		predecessors.add(pred);
	}

	private void removePredecessor(MachineBasicBlock pred)
	{
		assert predecessors.contains(pred)
				: "The pred to be removed not contained in pred list";
		predecessors.remove(pred);
	}

	public void removePredecessor(int idx)
	{
		assert idx>= 0 && idx < getNumPredecessors();
		predecessors.remove(idx);
	}

	public boolean isSuccessor(MachineBasicBlock mbb)
	{
		return successors.contains(mbb);
	}

	public void replaceSuccessor(MachineBasicBlock oldOne, MachineBasicBlock newOne)
	{
		if (!successors.contains(oldOne))
			return;
		int idx = successors.indexOf(oldOne);
		successors.set(idx, newOne);
	}

	public boolean predIsEmpty() {return predecessors.isEmpty();}

	public boolean succIsEmpty() {return successors.isEmpty();}

	public int getNumPredecessors() {return predecessors.size(); }

	public int getNumSuccessors() {return successors.size(); }

	public MachineBasicBlock getPred(int idx)
	{
		assert idx >= 0 && idx < getNumPredecessors();
		return predecessors.get(idx);
	}

	public MachineBasicBlock getSucc(int idx)
	{
		assert idx>= 0 && idx < getNumSuccessors();
		return successors.get(idx);
	}

	/**
	 * Obtains the number of this machine block.
	 * @return
	 */
	public int getNumber()
	{
		return number;
	}

	public void setNumber(int number)
	{
		this.number = number;
	}

	public void eraseFromParent()
	{
		assert getParent() != null;
		getParent().erase(this);
	}

	public MachineFunction getParent()
	{
		return parent;
	}

	public Iterator<MachineBasicBlock> predIterator()
	{
		return predecessors.iterator();
	}

	public Iterator<MachineBasicBlock> succIterator()
	{
		return successors.iterator();
	}

	public int getFirstTerminator()
	{
		int i = size() - 1;
		for (; i >=0 && getInstAt(i).getDesc().isTerminator(); i--);
		return i;
	}

	public void remove(MachineInstr miToDelete)
	{
		insts.remove(miToDelete);
	}

	public boolean isLayoutSuccessor(MachineBasicBlock mbb)
	{
		return mbb.next == mbb;
	}

	public void removeInstrAt(int indexToDel)
	{
		assert indexToDel >= 0 && indexToDel < size();
		insts.remove(indexToDel);
	}

	public void addLiveIn(int reg)
	{
		liveIns.add(reg);
	}

    public MachineInstr getLastInst()
    {
        return insts.getLast();
    }

    public MachineInstr getFirstInst()
    {
        return insts.getFirst();
    }
}
