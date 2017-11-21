package backend.codegen;

import backend.target.TargetMachine;
import backend.target.TargetRegisterClass;
import backend.target.TargetRegisterInfo;
import backend.value.BasicBlock;
import backend.value.Function;
import tools.Pair;

import java.io.PrintStream;
import java.util.ArrayList;

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
	private ArrayList<MachineBasicBlock> mbbNumber;
	/**
	 * Used to keep track of stack frame information about target.
	 */
	private MachineFrameInfo frameInfo;
	/**
	 * Keeping track of diagMapping from SSA values to registers.
	 */
	private MachineRegisterInfo regInfo;
	/**
	 * Keep track of constants to be spilled into stack slot.
	 */
	private MachineConstantPool constantPool;

	/**
	 * This array keeps track of the defined machine operand with the spceified
	 * machine register.
	 */
	private MachineOperand[] phyRegDefUseList;

	/**
	 * Used to keep track of target-specific per-machine function information for
	 * the target implementation.
	 */
	MachineFunctionInfo mfInfo;
	private int alignment;

	public MachineFunction(Function fn, TargetMachine tm)
	{
		this.fn = fn;
		target = tm;
		mbbNumber = new ArrayList<>();
		frameInfo = new MachineFrameInfo(tm.getFrameInfo());
		regInfo = new MachineRegisterInfo(tm.getRegisterInfo());
		constantPool = new MachineConstantPool(tm.getTargetData());
		phyRegDefUseList = new MachineOperand[tm.getRegisterInfo().getNumRegs()];

		// associate this machine function with HIR function.
		fn.setMachineFunc(this);
	}

	public Function getFunction() {return fn;}

	public TargetMachine getTarget() {return target;}

	public MachineBasicBlock getEntryBlock() {return mbbNumber.get(0);}

	public MachineFrameInfo getFrameInfo() {return frameInfo;}

	public MachineRegisterInfo getMachineRegisterInfo(){return regInfo;}

	public void clearSSARegMap() {
		regInfo.clear();}

	public MachineConstantPool getConstantPool(){return constantPool;}

	public ArrayList<MachineBasicBlock> getBasicBlocks() {return mbbNumber;}

	public void erase(MachineBasicBlock mbb)
	{
		mbbNumber.remove(mbb);
	}

	/**
	 * returns the number of allocated blocks ID.
	 * @return
	 */
	public int getNumBlockIDs()
	{
		return mbbNumber.size();
	}

	public MachineBasicBlock getMBBAt(int blockNo)
	{
		assert blockNo >= 0 && blockNo < mbbNumber.size();
		return mbbNumber.get(blockNo);
	}

	public boolean isEmpty() {return mbbNumber.isEmpty();}

	public void addMBBNumbering(MachineBasicBlock mbb)
	{
		mbbNumber.add(mbb);
		mbb.setNumber(mbbNumber.size()-1);
	}

	/**
	 * Performs a phase for re-numbering all of blocks in this function.
	 */
	public void renumberBlocks()
	{
		if (isEmpty())
		{
			mbbNumber.clear();
			return;
		}

		renumberBlocks(getEntryBlock());
	}

	private void renumberBlocks(MachineBasicBlock start)
	{
		int blockNo = 0;
		int i = mbbNumber.indexOf(start);
		if (i != 0)
			blockNo = mbbNumber.get(i-1).getNumber() + 1;

		for (int e = mbbNumber.size(); i < e; i++, blockNo++)
		{
			MachineBasicBlock mbb = mbbNumber.get(i);

			if (mbb.getNumber() != blockNo)
			{
				// remove the old number and let a new number to it.
				if (mbb.getNumber() != -1)
				{
					mbbNumber.set(mbb.getNumber(), null);
				}
				if (mbbNumber.get(blockNo) != null)
					mbbNumber.get(blockNo).setNumber(-1);

				mbbNumber.set(blockNo, mbb);
				mbb.setNumber(blockNo);
			}
		}

		assert blockNo <= mbbNumber.size():"Mismatch!";
		mbbNumber.ensureCapacity(blockNo);
	}

	public MachineFunctionInfo getInfo()
    {
        return mfInfo;
    }

	public int getAlignment()
	{
		return alignment;
	}

	public void setAlignment(int align)
	{
		alignment = align;
	}

	public int size()
	{
		return getBasicBlocks().size();
	}

	public MachineBasicBlock createMachineBasicBlock(BasicBlock bb)
	{
		MachineBasicBlock mbb = new MachineBasicBlock(bb);
		mbb.setParent(this);
		return mbb;
	}

	public void print(PrintStream os)
    {
        print(os, new PrefixPrinter());
    }

	public void print(PrintStream os, PrefixPrinter prefix)
	{
		os.printf("# Machine code for %s():%n", fn.getName());

		//Print frame information.
		frameInfo.print(this, os);

		// Print Jump table information.
		// Print constant pool.
		constantPool.print(os);

		TargetRegisterInfo tri = target.getRegisterInfo();
		if (regInfo != null && !regInfo.isLiveInEmpty())
		{
			os.printf("Live Ins:");
			for (Pair<Integer, Integer> entry : regInfo.getLiveIns())
			{
				if (tri != null)
					os.printf(" %s", tri.getName(entry.first));
				else
					os.printf(" Reg #%d", entry.first);

				if (entry.second != 0)
					os.printf(" in VR#%d ", entry.second);
			}
			os.println();
		}

		if (regInfo != null && !regInfo.isLiveOutEmpty())
		{
			os.printf("Live Outs:");
			for (Pair<Integer, Integer> entry : regInfo.getLiveIns())
			{
				if (tri != null)
					os.printf(" %s", tri.getName(entry.first));
				else
					os.printf(" Reg #%d", entry.first);
			}
			os.println();
		}

		//
		for (MachineBasicBlock mbb:mbbNumber)
		{
			prefix.print(os, mbb);
			mbb.print(os, prefix);
		}
		os.printf("%n# End machine code for %s().%n%n", fn.getName());
	}

	/**
	 * Add the specified physical register as a live-in values. Also create a
	 * virtual register associated with this register.
	 * @param locReg
	 * @param rc
	 * @return
	 */
	public int addLiveIn(int locReg, TargetRegisterClass rc)
	{
		assert rc.contains(locReg):"Not the current regclass!";
		int virReg = getMachineRegisterInfo().createVirtualRegister(rc);
		getMachineRegisterInfo().addLiveIn(locReg, virReg);
		return virReg;
	}
}
