package backend.codegen;

import backend.target.TargetMachine;
import backend.value.Function;

import java.util.ArrayList;

import static backend.target.TargetRegisterInfo.FirstVirtualRegister;

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
	 * Keeping track of mapping from SSA values to registers.
	 */
	private SSARegMap ssaRegMap;
	/**
	 * Keep track of constants to be spilled into stack slot.
	 */
	private MachineConstantPool constantPool;

	/**
	 * This array keeps track of the defined machine operand with the spceified
	 * machine register.
	 */
	private MachineOperand[] phyRegDefUseList;

	public MachineFunction(Function fn, TargetMachine tm)
	{
		this.fn = fn;
		target = tm;
		mbbNumber = new ArrayList<>();
		frameInfo = new MachineFrameInfo();
		ssaRegMap = new SSARegMap();
		constantPool = new MachineConstantPool(tm.getTargetData());
		phyRegDefUseList = new MachineOperand[tm.getRegInfo().getNumRegs()];

		// associate this machine function with HIR function.
		fn.setMachineFunc(this);
	}

	public Function getFunction() {return fn;}

	public TargetMachine getTargetMachine() {return target;}

	public MachineBasicBlock getEntryBlock() {return mbbNumber.get(0);}

	public MachineFrameInfo getFrameInfo() {return frameInfo;}

	public SSARegMap getSsaRegMap(){return ssaRegMap;}

	public void clearSSARegMap() {ssaRegMap.clear();}

	public MachineConstantPool getConstantPool(){return constantPool;}

	public ArrayList<MachineBasicBlock> getBasicBlocks() {return mbbNumber;}

	public void erase(MachineBasicBlock mbb)
	{
		mbbNumber.remove(mbb);
	}

	public int getNumMBB()
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

	/**
	 * Replaces all of usage of oldReg with newReg in this machine function.
	 * @param oldReg
	 * @param newReg
	 */
	public void replaceRegWith(int oldReg, int newReg)
	{
		assert oldReg != newReg :"It is not needed to replace the same reg";
		MachineOperand defined = null;
		if (oldReg < FirstVirtualRegister)
			defined = phyRegDefUseList[oldReg];
		else
			defined = ssaRegMap.getDefinedMO(oldReg);
		for (MachineOperand user : defined.getDefUseList())
		{
			user.setRegNum(newReg);
		}
	}
}
